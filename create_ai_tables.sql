-- ============================================================================
-- PROMETHEUS COACH - AI ASSISTANT TABLES SETUP
-- ============================================================================
-- Run this script in Supabase SQL Editor to create AI assistant tables
-- ============================================================================

-- ============================================================================
-- 1. AI CONVERSATIONS TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS ai_conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    coach_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    context_type TEXT NOT NULL CHECK (context_type IN ('general', 'client_analysis', 'program_design', 'message_draft', 'workout_review')),
    context_id UUID,  -- References client_id, program_id, etc.
    context_name TEXT,
    title TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Index for coach conversations
CREATE INDEX IF NOT EXISTS idx_ai_conversations_coach
ON ai_conversations(coach_id, updated_at DESC);

-- ============================================================================
-- 2. AI MESSAGES TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS ai_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES ai_conversations(id) ON DELETE CASCADE,
    role TEXT NOT NULL CHECK (role IN ('user', 'assistant', 'system')),
    content TEXT NOT NULL,
    timestamp TIMESTAMPTZ DEFAULT NOW(),
    context_type TEXT,
    context_data JSONB,
    -- Token usage tracking (optional)
    input_tokens INTEGER,
    output_tokens INTEGER
);

-- Index for conversation messages
CREATE INDEX IF NOT EXISTS idx_ai_messages_conversation
ON ai_messages(conversation_id, timestamp ASC);

-- ============================================================================
-- 3. ROW LEVEL SECURITY
-- ============================================================================

ALTER TABLE ai_conversations ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_messages ENABLE ROW LEVEL SECURITY;

-- Coaches can manage their own conversations
CREATE POLICY "Coaches can view own AI conversations"
ON ai_conversations FOR SELECT
TO authenticated
USING (coach_id = auth.uid());

CREATE POLICY "Coaches can create own AI conversations"
ON ai_conversations FOR INSERT
TO authenticated
WITH CHECK (coach_id = auth.uid());

CREATE POLICY "Coaches can update own AI conversations"
ON ai_conversations FOR UPDATE
TO authenticated
USING (coach_id = auth.uid())
WITH CHECK (coach_id = auth.uid());

CREATE POLICY "Coaches can delete own AI conversations"
ON ai_conversations FOR DELETE
TO authenticated
USING (coach_id = auth.uid());

-- Messages policies (based on conversation ownership)
CREATE POLICY "Coaches can view messages in own conversations"
ON ai_messages FOR SELECT
TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM ai_conversations
        WHERE ai_conversations.id = ai_messages.conversation_id
        AND ai_conversations.coach_id = auth.uid()
    )
);

CREATE POLICY "Coaches can create messages in own conversations"
ON ai_messages FOR INSERT
TO authenticated
WITH CHECK (
    EXISTS (
        SELECT 1 FROM ai_conversations
        WHERE ai_conversations.id = ai_messages.conversation_id
        AND ai_conversations.coach_id = auth.uid()
    )
);

CREATE POLICY "Coaches can delete messages in own conversations"
ON ai_messages FOR DELETE
TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM ai_conversations
        WHERE ai_conversations.id = ai_messages.conversation_id
        AND ai_conversations.coach_id = auth.uid()
    )
);

-- ============================================================================
-- 4. UPDATED_AT TRIGGER
-- ============================================================================

DROP TRIGGER IF EXISTS update_ai_conversations_updated_at ON ai_conversations;
CREATE TRIGGER update_ai_conversations_updated_at
    BEFORE UPDATE ON ai_conversations
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- DONE!
-- ============================================================================
