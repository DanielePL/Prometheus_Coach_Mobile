-- ============================================================================
-- FIX ROUTINES TABLE RLS POLICY
-- Error: "new row violates row-level security policy for table routines"
--
-- Run this in the Supabase SQL Editor
-- ============================================================================

-- First, check current RLS policies on routines table
SELECT
    schemaname,
    tablename,
    policyname,
    permissive,
    roles,
    cmd,
    qual,
    with_check
FROM pg_policies
WHERE tablename = 'routines';

-- ============================================================================
-- OPTION 1: If no INSERT policy exists, create one
-- ============================================================================

-- Drop existing policies if needed (uncomment if you want to recreate)
-- DROP POLICY IF EXISTS "Coaches can view their own routines" ON routines;
-- DROP POLICY IF EXISTS "Coaches can insert their own routines" ON routines;
-- DROP POLICY IF EXISTS "Coaches can update their own routines" ON routines;
-- DROP POLICY IF EXISTS "Coaches can delete their own routines" ON routines;

-- Make sure RLS is enabled
ALTER TABLE routines ENABLE ROW LEVEL SECURITY;

-- Create comprehensive policies for coaches
-- SELECT policy
CREATE POLICY "Coaches can view their own routines"
ON routines FOR SELECT
TO authenticated
USING (coach_id = auth.uid());

-- INSERT policy - THIS IS LIKELY THE MISSING ONE
CREATE POLICY "Coaches can insert their own routines"
ON routines FOR INSERT
TO authenticated
WITH CHECK (coach_id = auth.uid());

-- UPDATE policy
CREATE POLICY "Coaches can update their own routines"
ON routines FOR UPDATE
TO authenticated
USING (coach_id = auth.uid())
WITH CHECK (coach_id = auth.uid());

-- DELETE policy
CREATE POLICY "Coaches can delete their own routines"
ON routines FOR DELETE
TO authenticated
USING (coach_id = auth.uid());

-- ============================================================================
-- VERIFY: Check that policies were created
-- ============================================================================
SELECT
    policyname,
    cmd,
    qual,
    with_check
FROM pg_policies
WHERE tablename = 'routines'
ORDER BY policyname;

SELECT 'RLS policies for routines table have been created/updated!' as status;

-- ============================================================================
-- ALSO CHECK routine_exercises table
-- ============================================================================

-- Make sure RLS is enabled
ALTER TABLE routine_exercises ENABLE ROW LEVEL SECURITY;

-- Check if coach owns the routine before allowing operations
CREATE POLICY "Coaches can view exercises in their routines"
ON routine_exercises FOR SELECT
TO authenticated
USING (
    routine_id IN (
        SELECT id FROM routines WHERE coach_id = auth.uid()
    )
);

CREATE POLICY "Coaches can insert exercises to their routines"
ON routine_exercises FOR INSERT
TO authenticated
WITH CHECK (
    routine_id IN (
        SELECT id FROM routines WHERE coach_id = auth.uid()
    )
);

CREATE POLICY "Coaches can update exercises in their routines"
ON routine_exercises FOR UPDATE
TO authenticated
USING (
    routine_id IN (
        SELECT id FROM routines WHERE coach_id = auth.uid()
    )
)
WITH CHECK (
    routine_id IN (
        SELECT id FROM routines WHERE coach_id = auth.uid()
    )
);

CREATE POLICY "Coaches can delete exercises from their routines"
ON routine_exercises FOR DELETE
TO authenticated
USING (
    routine_id IN (
        SELECT id FROM routines WHERE coach_id = auth.uid()
    )
);

SELECT 'All RLS policies created successfully!' as status;
