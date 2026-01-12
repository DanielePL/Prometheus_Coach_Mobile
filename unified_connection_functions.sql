-- ============================================================================
-- UNIFIED COACH-CLIENT CONNECTION FUNCTIONS
--
-- Diese Funktionen bieten eine einheitliche API für alle 5 Apps:
-- - 2 Android Apps (Coach + Client)
-- - 2 iOS/Swift Apps (Coach + Client)
-- - 1 React Web App
--
-- VORTEILE:
-- 1. Alle Business-Logik serverseitig → keine Duplikation
-- 2. Atomare Transaktionen → Daten-Konsistenz
-- 3. Validierung serverseitig → Sicherheit
-- 4. Einheitliche Fehlerbehandlung
--
-- Run this in Supabase SQL Editor
-- ============================================================================

-- ============================================================================
-- 1. CONNECT BY INVITE CODE
-- Client nutzt den Invite-Code vom Coach um eine Verbindung anzufordern
-- ============================================================================
CREATE OR REPLACE FUNCTION connect_by_invite_code(p_invite_code TEXT)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_client_id UUID;
    v_coach_id UUID;
    v_coach_name TEXT;
    v_existing_connection RECORD;
    v_new_connection_id UUID;
BEGIN
    -- Get the authenticated user (client)
    v_client_id := auth.uid();

    IF v_client_id IS NULL THEN
        RETURN jsonb_build_object(
            'success', false,
            'error', 'NOT_AUTHENTICATED',
            'message', 'You must be logged in to connect with a coach'
        );
    END IF;

    -- Find the coach by invite code (case-insensitive)
    SELECT id, full_name INTO v_coach_id, v_coach_name
    FROM profiles
    WHERE UPPER(invite_code) = UPPER(TRIM(p_invite_code))
    AND role = 'coach';

    IF v_coach_id IS NULL THEN
        RETURN jsonb_build_object(
            'success', false,
            'error', 'INVALID_CODE',
            'message', 'Invalid invite code. Please check and try again.'
        );
    END IF;

    -- Check if client is trying to connect to themselves
    IF v_client_id = v_coach_id THEN
        RETURN jsonb_build_object(
            'success', false,
            'error', 'SELF_CONNECTION',
            'message', 'You cannot connect to yourself'
        );
    END IF;

    -- Check for existing connection
    SELECT * INTO v_existing_connection
    FROM coach_client_connections
    WHERE coach_id = v_coach_id AND client_id = v_client_id;

    IF v_existing_connection IS NOT NULL THEN
        -- Return appropriate message based on status
        IF v_existing_connection.status = 'accepted' THEN
            RETURN jsonb_build_object(
                'success', false,
                'error', 'ALREADY_CONNECTED',
                'message', 'You are already connected with this coach'
            );
        ELSIF v_existing_connection.status = 'pending' THEN
            RETURN jsonb_build_object(
                'success', false,
                'error', 'REQUEST_PENDING',
                'message', 'Your connection request is already pending'
            );
        ELSIF v_existing_connection.status = 'declined' THEN
            -- Allow re-request after decline - update existing row
            UPDATE coach_client_connections
            SET status = 'pending',
                accepted_at = NULL,
                updated_at = NOW()
            WHERE id = v_existing_connection.id
            RETURNING id INTO v_new_connection_id;

            RETURN jsonb_build_object(
                'success', true,
                'connection_id', v_new_connection_id,
                'coach_name', v_coach_name,
                'message', 'Connection request sent to coach'
            );
        END IF;
    END IF;

    -- Create new connection request
    INSERT INTO coach_client_connections (
        coach_id,
        client_id,
        status,
        created_at,
        updated_at
    ) VALUES (
        v_coach_id,
        v_client_id,
        'pending',
        NOW(),
        NOW()
    )
    RETURNING id INTO v_new_connection_id;

    RETURN jsonb_build_object(
        'success', true,
        'connection_id', v_new_connection_id,
        'coach_name', v_coach_name,
        'message', 'Connection request sent to coach'
    );

EXCEPTION WHEN OTHERS THEN
    RETURN jsonb_build_object(
        'success', false,
        'error', 'INTERNAL_ERROR',
        'message', 'An unexpected error occurred. Please try again.'
    );
END;
$$;

-- ============================================================================
-- 2. RESPOND TO CONNECTION REQUEST
-- Coach akzeptiert oder lehnt eine Verbindungsanfrage ab
-- ============================================================================
CREATE OR REPLACE FUNCTION respond_to_connection(
    p_connection_id UUID,
    p_accept BOOLEAN
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_coach_id UUID;
    v_connection RECORD;
    v_new_status TEXT;
BEGIN
    -- Get the authenticated user (coach)
    v_coach_id := auth.uid();

    IF v_coach_id IS NULL THEN
        RETURN jsonb_build_object(
            'success', false,
            'error', 'NOT_AUTHENTICATED',
            'message', 'You must be logged in to respond to requests'
        );
    END IF;

    -- Find the connection and verify ownership
    SELECT * INTO v_connection
    FROM coach_client_connections
    WHERE id = p_connection_id AND coach_id = v_coach_id;

    IF v_connection IS NULL THEN
        RETURN jsonb_build_object(
            'success', false,
            'error', 'NOT_FOUND',
            'message', 'Connection request not found'
        );
    END IF;

    -- Check if already responded
    IF v_connection.status != 'pending' THEN
        RETURN jsonb_build_object(
            'success', false,
            'error', 'ALREADY_RESPONDED',
            'message', 'This request has already been ' || v_connection.status
        );
    END IF;

    -- Update the connection status
    v_new_status := CASE WHEN p_accept THEN 'accepted' ELSE 'declined' END;

    UPDATE coach_client_connections
    SET status = v_new_status,
        accepted_at = CASE WHEN p_accept THEN NOW() ELSE NULL END,
        updated_at = NOW()
    WHERE id = p_connection_id;

    RETURN jsonb_build_object(
        'success', true,
        'status', v_new_status,
        'message', CASE WHEN p_accept
            THEN 'Client connected successfully'
            ELSE 'Request declined'
        END
    );

EXCEPTION WHEN OTHERS THEN
    RETURN jsonb_build_object(
        'success', false,
        'error', 'INTERNAL_ERROR',
        'message', 'An unexpected error occurred. Please try again.'
    );
END;
$$;

-- ============================================================================
-- 3. GET MY CONNECTIONS
-- Gibt alle Verbindungen für den aktuellen User zurück (Coach ODER Client)
-- Funktioniert für beide Perspektiven!
-- ============================================================================
CREATE OR REPLACE FUNCTION get_my_connections()
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_user_id UUID;
    v_user_role TEXT;
    v_connections JSONB;
BEGIN
    -- Get the authenticated user
    v_user_id := auth.uid();

    IF v_user_id IS NULL THEN
        RETURN jsonb_build_object(
            'success', false,
            'error', 'NOT_AUTHENTICATED',
            'connections', '[]'::jsonb
        );
    END IF;

    -- Get user role
    SELECT role INTO v_user_role FROM profiles WHERE id = v_user_id;

    IF v_user_role = 'coach' THEN
        -- Coach perspective: get all clients
        SELECT COALESCE(jsonb_agg(
            jsonb_build_object(
                'connection_id', ccc.id,
                'user_id', ccc.client_id,
                'user_name', COALESCE(up.name, p.full_name, 'Unknown'),
                'user_avatar', COALESCE(up.profile_image_url, p.avatar_url),
                'status', ccc.status,
                'requested_at', ccc.created_at,
                'responded_at', ccc.accepted_at,
                'role', 'client'
            ) ORDER BY
                CASE ccc.status WHEN 'pending' THEN 0 ELSE 1 END,
                ccc.created_at DESC
        ), '[]'::jsonb) INTO v_connections
        FROM coach_client_connections ccc
        LEFT JOIN profiles p ON ccc.client_id = p.id
        LEFT JOIN user_profiles up ON ccc.client_id = up.id
        WHERE ccc.coach_id = v_user_id;

    ELSE
        -- Client perspective: get all coaches
        SELECT COALESCE(jsonb_agg(
            jsonb_build_object(
                'connection_id', ccc.id,
                'user_id', ccc.coach_id,
                'user_name', COALESCE(p.full_name, 'Unknown'),
                'user_avatar', p.avatar_url,
                'status', ccc.status,
                'requested_at', ccc.created_at,
                'responded_at', ccc.accepted_at,
                'role', 'coach'
            ) ORDER BY ccc.accepted_at DESC NULLS LAST
        ), '[]'::jsonb) INTO v_connections
        FROM coach_client_connections ccc
        LEFT JOIN profiles p ON ccc.coach_id = p.id
        WHERE ccc.client_id = v_user_id;

    END IF;

    RETURN jsonb_build_object(
        'success', true,
        'role', v_user_role,
        'connections', v_connections
    );

EXCEPTION WHEN OTHERS THEN
    RETURN jsonb_build_object(
        'success', false,
        'error', 'INTERNAL_ERROR',
        'connections', '[]'::jsonb
    );
END;
$$;

-- ============================================================================
-- 4. DISCONNECT
-- Coach oder Client beendet die Verbindung
-- ============================================================================
CREATE OR REPLACE FUNCTION disconnect_connection(p_connection_id UUID)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_user_id UUID;
    v_connection RECORD;
BEGIN
    -- Get the authenticated user
    v_user_id := auth.uid();

    IF v_user_id IS NULL THEN
        RETURN jsonb_build_object(
            'success', false,
            'error', 'NOT_AUTHENTICATED',
            'message', 'You must be logged in'
        );
    END IF;

    -- Find the connection (user must be either coach or client)
    SELECT * INTO v_connection
    FROM coach_client_connections
    WHERE id = p_connection_id
    AND (coach_id = v_user_id OR client_id = v_user_id);

    IF v_connection IS NULL THEN
        RETURN jsonb_build_object(
            'success', false,
            'error', 'NOT_FOUND',
            'message', 'Connection not found'
        );
    END IF;

    -- Delete the connection
    DELETE FROM coach_client_connections WHERE id = p_connection_id;

    RETURN jsonb_build_object(
        'success', true,
        'message', 'Disconnected successfully'
    );

EXCEPTION WHEN OTHERS THEN
    RETURN jsonb_build_object(
        'success', false,
        'error', 'INTERNAL_ERROR',
        'message', 'An unexpected error occurred'
    );
END;
$$;

-- ============================================================================
-- 5. GET COACH BY INVITE CODE (Preview before connecting)
-- Client kann Coach-Info sehen bevor er sich verbindet
-- ============================================================================
CREATE OR REPLACE FUNCTION get_coach_by_invite_code(p_invite_code TEXT)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_coach RECORD;
BEGIN
    -- Find the coach by invite code
    SELECT
        id,
        full_name,
        avatar_url,
        bio
    INTO v_coach
    FROM profiles
    WHERE UPPER(invite_code) = UPPER(TRIM(p_invite_code))
    AND role = 'coach';

    IF v_coach.id IS NULL THEN
        RETURN jsonb_build_object(
            'success', false,
            'error', 'INVALID_CODE',
            'message', 'No coach found with this invite code'
        );
    END IF;

    RETURN jsonb_build_object(
        'success', true,
        'coach', jsonb_build_object(
            'id', v_coach.id,
            'name', v_coach.full_name,
            'avatar_url', v_coach.avatar_url,
            'bio', v_coach.bio
        )
    );

EXCEPTION WHEN OTHERS THEN
    RETURN jsonb_build_object(
        'success', false,
        'error', 'INTERNAL_ERROR',
        'message', 'An unexpected error occurred'
    );
END;
$$;

-- ============================================================================
-- GRANT PERMISSIONS
-- ============================================================================
GRANT EXECUTE ON FUNCTION connect_by_invite_code(TEXT) TO authenticated;
GRANT EXECUTE ON FUNCTION respond_to_connection(UUID, BOOLEAN) TO authenticated;
GRANT EXECUTE ON FUNCTION get_my_connections() TO authenticated;
GRANT EXECUTE ON FUNCTION disconnect_connection(UUID) TO authenticated;
GRANT EXECUTE ON FUNCTION get_coach_by_invite_code(TEXT) TO authenticated;

-- ============================================================================
-- USAGE EXAMPLES FOR ALL PLATFORMS
-- ============================================================================
/*

=== KOTLIN (Android) ===
val result = supabaseClient.postgrest.rpc("connect_by_invite_code", mapOf("p_invite_code" to "ABC123"))
val json = result.decodeAs<JsonObject>()
if (json["success"].jsonPrimitive.boolean) {
    // Connected!
}

=== SWIFT (iOS) ===
let result = try await supabase.rpc("connect_by_invite_code", params: ["p_invite_code": "ABC123"])
let json = try result.decodeAs(ConnectionResult.self)
if json.success {
    // Connected!
}

=== TYPESCRIPT (React) ===
const { data, error } = await supabase.rpc('connect_by_invite_code', { p_invite_code: 'ABC123' })
if (data.success) {
    // Connected!
}``

*/

SELECT 'Unified connection functions created successfully!' as status;
