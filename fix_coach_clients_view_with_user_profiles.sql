-- ============================================================================
-- FIX COACH_CLIENTS_V VIEW TO USE user_profiles FOR CLIENT AVATARS
--
-- PROBLEM: The coach app uses `profiles.avatar_url` but client mobile app
-- stores profile images in `user_profiles.profile_image_url`
--
-- SOLUTION: Join with user_profiles table to get the correct avatar
--
-- Run this in the Supabase SQL Editor
-- ============================================================================

DROP VIEW IF EXISTS coach_clients_v;

CREATE VIEW coach_clients_v AS
SELECT
    ccc.id AS connection_id,
    ccc.coach_id,
    ccc.client_id AS user_id,
    ccc.status,
    ccc.requested_at,
    ccc.responded_at,
    ccc.created_at,
    ccc.updated_at,
    -- Get name from profiles table (fallback to user_profiles.name)
    COALESCE(p.full_name, up.name, 'Unknown') AS user_name,
    -- Get avatar from user_profiles.profile_image_url (where client mobile app stores it)
    -- Fallback to profiles.avatar_url if user_profiles doesn't have one
    COALESCE(up.profile_image_url, p.avatar_url) AS user_avatar
FROM coach_client_connections ccc
LEFT JOIN profiles p ON ccc.client_id = p.id
LEFT JOIN user_profiles up ON ccc.client_id = up.id;

-- Grant permissions
GRANT SELECT ON coach_clients_v TO authenticated;

-- ============================================================================
-- VERIFY: Check if avatars are now visible
-- ============================================================================
SELECT
    user_id,
    user_name,
    user_avatar,
    CASE
        WHEN user_avatar IS NOT NULL THEN 'Has avatar'
        ELSE 'No avatar'
    END as avatar_status
FROM coach_clients_v
LIMIT 10;

-- ============================================================================
-- ALSO CHECK: What's in user_profiles for these clients?
-- ============================================================================
SELECT
    up.id,
    up.name,
    up.profile_image_url,
    CASE
        WHEN up.profile_image_url IS NOT NULL THEN 'Has profile image'
        ELSE 'No profile image'
    END as image_status
FROM user_profiles up
WHERE up.id IN (SELECT client_id FROM coach_client_connections)
LIMIT 10;

SELECT 'coach_clients_v view updated to use user_profiles.profile_image_url!' as status;
