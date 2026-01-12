-- ============================================================
-- MESSAGE ATTACHMENTS SETUP
-- Run this in Supabase SQL Editor
-- ============================================================

-- 1. Add attachment columns to messages table
ALTER TABLE messages
ADD COLUMN IF NOT EXISTS attachment_url TEXT,
ADD COLUMN IF NOT EXISTS attachment_type TEXT,  -- 'image', 'document', 'voice'
ADD COLUMN IF NOT EXISTS attachment_name TEXT,  -- Original filename
ADD COLUMN IF NOT EXISTS attachment_size BIGINT; -- File size in bytes

-- 2. Create storage bucket for message attachments
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
    'message-attachments',
    'message-attachments',
    false,  -- Private bucket, requires auth
    20971520,  -- 20MB max file size
    ARRAY[
        'image/jpeg',
        'image/png',
        'image/webp',
        'image/gif',
        'image/heic',
        'application/pdf',
        'application/msword',
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        'application/vnd.ms-excel',
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        'text/plain',
        'text/csv',
        'audio/mpeg',
        'audio/mp4',
        'audio/ogg',
        'audio/wav',
        'video/mp4',
        'video/quicktime'
    ]
)
ON CONFLICT (id) DO UPDATE SET
    file_size_limit = EXCLUDED.file_size_limit,
    allowed_mime_types = EXCLUDED.allowed_mime_types;

-- 3. RLS Policies for message-attachments bucket

-- Policy: Users can upload attachments to conversations they're part of
CREATE POLICY "Users can upload message attachments"
ON storage.objects FOR INSERT
TO authenticated
WITH CHECK (
    bucket_id = 'message-attachments'
    AND (storage.foldername(name))[1] IN (
        SELECT cp.conversation_id::text
        FROM conversation_participants cp
        WHERE cp.user_id = auth.uid()
    )
);

-- Policy: Users can view attachments from conversations they're part of
CREATE POLICY "Users can view message attachments"
ON storage.objects FOR SELECT
TO authenticated
USING (
    bucket_id = 'message-attachments'
    AND (storage.foldername(name))[1] IN (
        SELECT cp.conversation_id::text
        FROM conversation_participants cp
        WHERE cp.user_id = auth.uid()
    )
);

-- Policy: Users can delete their own attachments
CREATE POLICY "Users can delete own attachments"
ON storage.objects FOR DELETE
TO authenticated
USING (
    bucket_id = 'message-attachments'
    AND owner = auth.uid()
);

-- 4. Index for faster attachment queries
CREATE INDEX IF NOT EXISTS idx_messages_attachment_url
ON messages(attachment_url)
WHERE attachment_url IS NOT NULL;

-- 5. Helper function to get signed URL for attachment (optional, for private access)
CREATE OR REPLACE FUNCTION get_attachment_url(file_path TEXT)
RETURNS TEXT
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    -- Returns the storage path, client will use Supabase SDK to get signed URL
    RETURN file_path;
END;
$$;

GRANT EXECUTE ON FUNCTION get_attachment_url(TEXT) TO authenticated;
