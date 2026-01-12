-- ============================================================================
-- DEBUG AND FIX ROUTINES RLS POLICY
-- Run this in the Supabase SQL Editor step by step
-- ============================================================================

-- ============================================================================
-- STEP 1: Check your user's profile
-- Replace the UUID with your actual coach ID from the logs
-- ============================================================================
SELECT
    id,
    full_name,
    role,
    email
FROM profiles
WHERE id = 'faba7636-66b9-43cd-8570-37cdc32ffff0';

-- ============================================================================
-- STEP 2: Check ALL policies on routines (including hidden ones)
-- ============================================================================
SELECT
    schemaname,
    tablename,
    policyname,
    permissive,
    roles,
    cmd,
    qual::text as using_clause,
    with_check::text as with_check_clause
FROM pg_policies
WHERE tablename = 'routines'
ORDER BY policyname;

-- ============================================================================
-- STEP 3: Check if RLS is enabled
-- ============================================================================
SELECT
    relname as table_name,
    relrowsecurity as rls_enabled,
    relforcerowsecurity as force_rls
FROM pg_class
WHERE relname = 'routines';

-- ============================================================================
-- STEP 4: Check for any triggers on routines table
-- ============================================================================
SELECT
    tgname as trigger_name,
    tgtype,
    proname as function_name
FROM pg_trigger t
JOIN pg_proc p ON t.tgfoid = p.oid
JOIN pg_class c ON t.tgrelid = c.oid
WHERE c.relname = 'routines';

-- ============================================================================
-- STEP 5: Try inserting as the service_role (bypasses RLS) to verify table works
-- ============================================================================
-- This should work if the table structure is correct
INSERT INTO routines (coach_id, name, description)
VALUES ('faba7636-66b9-43cd-8570-37cdc32ffff0', 'Test Routine Direct', 'Test')
RETURNING *;

-- ============================================================================
-- STEP 6: Delete the test routine
-- ============================================================================
DELETE FROM routines WHERE name = 'Test Routine Direct';

-- ============================================================================
-- FIX OPTION 1: If role is not 'coach', update it
-- ============================================================================
-- UPDATE profiles
-- SET role = 'coach'
-- WHERE id = 'faba7636-66b9-43cd-8570-37cdc32ffff0';

-- ============================================================================
-- FIX OPTION 2: Drop and recreate ALL RLS policies for routines
-- This ensures no conflicting policies exist
-- ============================================================================

-- First, drop ALL existing policies
DROP POLICY IF EXISTS "Coaches can view their own routines" ON routines;
DROP POLICY IF EXISTS "Coaches can insert their own routines" ON routines;
DROP POLICY IF EXISTS "Coaches can update their own routines" ON routines;
DROP POLICY IF EXISTS "Coaches can delete their own routines" ON routines;
DROP POLICY IF EXISTS "Enable read access for all users" ON routines;
DROP POLICY IF EXISTS "Enable insert for authenticated users only" ON routines;
DROP POLICY IF EXISTS "Enable update for users based on coach_id" ON routines;
DROP POLICY IF EXISTS "Enable delete for users based on coach_id" ON routines;
DROP POLICY IF EXISTS "routines_select_policy" ON routines;
DROP POLICY IF EXISTS "routines_insert_policy" ON routines;
DROP POLICY IF EXISTS "routines_update_policy" ON routines;
DROP POLICY IF EXISTS "routines_delete_policy" ON routines;

-- Make sure RLS is enabled
ALTER TABLE routines ENABLE ROW LEVEL SECURITY;

-- Create fresh policies with simple conditions
CREATE POLICY "routines_select_policy" ON routines
FOR SELECT TO authenticated
USING (coach_id = auth.uid());

CREATE POLICY "routines_insert_policy" ON routines
FOR INSERT TO authenticated
WITH CHECK (coach_id = auth.uid());

CREATE POLICY "routines_update_policy" ON routines
FOR UPDATE TO authenticated
USING (coach_id = auth.uid())
WITH CHECK (coach_id = auth.uid());

CREATE POLICY "routines_delete_policy" ON routines
FOR DELETE TO authenticated
USING (coach_id = auth.uid());

-- ============================================================================
-- VERIFY: Check policies after fix
-- ============================================================================
SELECT
    policyname,
    cmd,
    qual::text as using_clause,
    with_check::text as with_check_clause
FROM pg_policies
WHERE tablename = 'routines'
ORDER BY policyname;

-- ============================================================================
-- FIX OPTION 3: If there's a trigger checking role, we might need to
-- create a simpler policy that doesn't rely on role checking
-- ============================================================================

-- Alternative: Create policies that check role in profiles table
-- (Only use this if the simple policies above don't work)

/*
DROP POLICY IF EXISTS "routines_select_policy" ON routines;
DROP POLICY IF EXISTS "routines_insert_policy" ON routines;
DROP POLICY IF EXISTS "routines_update_policy" ON routines;
DROP POLICY IF EXISTS "routines_delete_policy" ON routines;

CREATE POLICY "routines_select_policy" ON routines
FOR SELECT TO authenticated
USING (
    coach_id = auth.uid()
    AND EXISTS (
        SELECT 1 FROM profiles
        WHERE id = auth.uid()
        AND role = 'coach'
    )
);

CREATE POLICY "routines_insert_policy" ON routines
FOR INSERT TO authenticated
WITH CHECK (
    coach_id = auth.uid()
    AND EXISTS (
        SELECT 1 FROM profiles
        WHERE id = auth.uid()
        AND role = 'coach'
    )
);

CREATE POLICY "routines_update_policy" ON routines
FOR UPDATE TO authenticated
USING (coach_id = auth.uid())
WITH CHECK (coach_id = auth.uid());

CREATE POLICY "routines_delete_policy" ON routines
FOR DELETE TO authenticated
USING (coach_id = auth.uid());
*/

SELECT 'Debug and fix complete - try creating a workout now!' as status;
