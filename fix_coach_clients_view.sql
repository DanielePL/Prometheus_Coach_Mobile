-- ============================================================================
-- FIX COACH_CLIENTS_V VIEW TO USE user_id NAMING CONVENTION
-- The table has client_id, but we expose it as user_id in the view
-- Run this in the Supabase SQL Editor
-- ============================================================================

DROP VIEW IF EXISTS coach_clients_v;

CREATE VIEW coach_clients_v AS
SELECT
    ccc.id AS connection_id,
    ccc.coach_id,
    ccc.client_id AS user_id,  -- Expose client_id as user_id for mobile app compatibility
    ccc.status,
    ccc.requested_at,
    ccc.responded_at,
    ccc.created_at,
    ccc.updated_at,
    COALESCE(p.full_name, 'Unknown') AS user_name,
    p.avatar_url AS user_avatar
FROM coach_client_connections ccc
LEFT JOIN profiles p ON ccc.client_id = p.id;

-- Grant permissions
GRANT SELECT ON coach_clients_v TO authenticated;

-- ============================================================================
-- VERIFY: Check if avatars are now visible
-- ============================================================================
-- SELECT user_id, user_name, user_avatar FROM coach_clients_v LIMIT 10;

SELECT 'coach_clients_v view updated successfully!' as status;
?/'dapa'