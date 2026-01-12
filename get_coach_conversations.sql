-- Single RPC to get all conversations with details for a coach
-- This replaces N+1 queries with a single optimized query

CREATE OR REPLACE FUNCTION get_coach_conversations()
RETURNS TABLE (
    conversation_id UUID,
    participant_id UUID,
    participant_name TEXT,
    participant_avatar TEXT,
    last_message TEXT,
    last_message_at TIMESTAMPTZ,
    unread_count BIGINT
)
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    current_user_id UUID := auth.uid();
BEGIN
    RETURN QUERY
    WITH my_conversations AS (
        -- Get all conversations where I'm a participant
        SELECT
            cp.conversation_id,
            cp.last_read_at
        FROM conversation_participants cp
        WHERE cp.user_id = current_user_id
    ),
    other_participants AS (
        -- Get the other participant for each conversation
        SELECT DISTINCT ON (cp.conversation_id)
            cp.conversation_id,
            cp.user_id AS other_user_id,
            p.full_name,
            p.avatar_url
        FROM conversation_participants cp
        JOIN profiles p ON p.id = cp.user_id
        WHERE cp.conversation_id IN (SELECT mc.conversation_id FROM my_conversations mc)
          AND cp.user_id != current_user_id
    ),
    last_messages AS (
        -- Get the last message for each conversation
        SELECT DISTINCT ON (m.conversation_id)
            m.conversation_id,
            m.content,
            m.created_at
        FROM messages m
        WHERE m.conversation_id IN (SELECT mc.conversation_id FROM my_conversations mc)
        ORDER BY m.conversation_id, m.created_at DESC
    ),
    unread_counts AS (
        -- Count unread messages (from others, after my last_read_at)
        SELECT
            mc.conversation_id,
            COUNT(m.id) AS cnt
        FROM my_conversations mc
        LEFT JOIN messages m ON m.conversation_id = mc.conversation_id
            AND m.sender_id != current_user_id
            AND (mc.last_read_at IS NULL OR m.created_at > mc.last_read_at)
        GROUP BY mc.conversation_id
    )
    SELECT
        mc.conversation_id,
        op.other_user_id AS participant_id,
        COALESCE(op.full_name, 'Unknown') AS participant_name,
        op.avatar_url AS participant_avatar,
        lm.content AS last_message,
        lm.created_at AS last_message_at,
        COALESCE(uc.cnt, 0) AS unread_count
    FROM my_conversations mc
    LEFT JOIN other_participants op ON op.conversation_id = mc.conversation_id
    LEFT JOIN last_messages lm ON lm.conversation_id = mc.conversation_id
    LEFT JOIN unread_counts uc ON uc.conversation_id = mc.conversation_id
    ORDER BY lm.created_at DESC NULLS LAST;
END;
$$;

-- Grant execute permission
GRANT EXECUTE ON FUNCTION get_coach_conversations() TO authenticated;
