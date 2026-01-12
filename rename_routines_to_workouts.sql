-- ============================================================================
-- RENAME ROUTINES TO WORKOUTS - DATABASE MIGRATION
-- Run this in the Supabase SQL Editor
-- ============================================================================

-- ============================================================================
-- STEP 1: RENAME TABLES
-- ============================================================================

-- Rename routines -> workouts
ALTER TABLE IF EXISTS routines RENAME TO workouts;

-- Rename routine_exercises -> workout_exercises
ALTER TABLE IF EXISTS routine_exercises RENAME TO workout_exercises;

-- Rename routine_assignments -> workout_assignments
ALTER TABLE IF EXISTS routine_assignments RENAME TO workout_assignments;

-- ============================================================================
-- STEP 2: RENAME FOREIGN KEY COLUMNS
-- ============================================================================

-- In workout_exercises: routine_id -> workout_id
ALTER TABLE workout_exercises RENAME COLUMN routine_id TO workout_id;

-- In workout_assignments: routine_id -> workout_id
ALTER TABLE workout_assignments RENAME COLUMN routine_id TO workout_id;

-- ============================================================================
-- STEP 3: DROP OLD VIEW AND CREATE NEW VIEW
-- ============================================================================

-- Drop the old view
DROP VIEW IF EXISTS coach_routines_v;

-- Create new view with 'workout' naming
CREATE VIEW coach_workouts_v AS
SELECT
    w.id AS workout_id,
    w.coach_id,
    w.name AS workout_name,
    w.description AS workout_description,
    w.created_at AS workout_created_at,
    w.updated_at AS workout_updated_at,
    we.id AS workout_exercise_id,
    we.order_index,
    we.sets,
    we.reps_min,
    we.reps_max,
    we.rest_seconds,
    we.notes AS workout_exercise_notes,
    e.id AS exercise_id,
    e.name AS exercise_title,
    e.category AS exercise_category,
    NULL::text AS exercise_thumbnail_url,
    e.video_url AS exercise_video_url,
    e.main_muscle_group AS primary_muscles,
    ARRAY_TO_STRING(e.secondary_muscle_groups, ', ') AS secondary_muscles
FROM workouts w
LEFT JOIN workout_exercises we ON w.id = we.workout_id
LEFT JOIN exercises_new e ON we.exercise_id = e.id;

-- Grant access to the view
GRANT SELECT ON coach_workouts_v TO authenticated;

-- ============================================================================
-- STEP 4: DROP OLD RLS POLICIES AND CREATE NEW ONES
-- ============================================================================

-- Drop old policies (they reference old table name)
DROP POLICY IF EXISTS "Coaches can view their own routines" ON workouts;
DROP POLICY IF EXISTS "Coaches can insert their own routines" ON workouts;
DROP POLICY IF EXISTS "Coaches can update their own routines" ON workouts;
DROP POLICY IF EXISTS "Coaches can delete their own routines" ON workouts;
DROP POLICY IF EXISTS "routines_select_policy" ON workouts;
DROP POLICY IF EXISTS "routines_insert_policy" ON workouts;
DROP POLICY IF EXISTS "routines_update_policy" ON workouts;
DROP POLICY IF EXISTS "routines_delete_policy" ON workouts;

-- Enable RLS
ALTER TABLE workouts ENABLE ROW LEVEL SECURITY;

-- Create new policies for workouts table
CREATE POLICY "workouts_select_policy" ON workouts
FOR SELECT TO authenticated
USING (coach_id = auth.uid());

CREATE POLICY "workouts_insert_policy" ON workouts
FOR INSERT TO authenticated
WITH CHECK (coach_id = auth.uid());

CREATE POLICY "workouts_update_policy" ON workouts
FOR UPDATE TO authenticated
USING (coach_id = auth.uid())
WITH CHECK (coach_id = auth.uid());

CREATE POLICY "workouts_delete_policy" ON workouts
FOR DELETE TO authenticated
USING (coach_id = auth.uid());

-- ============================================================================
-- STEP 5: RLS POLICIES FOR WORKOUT_EXERCISES
-- ============================================================================

-- Drop old policies
DROP POLICY IF EXISTS "Coaches can view exercises in their routines" ON workout_exercises;
DROP POLICY IF EXISTS "Coaches can insert exercises to their routines" ON workout_exercises;
DROP POLICY IF EXISTS "Coaches can update exercises in their routines" ON workout_exercises;
DROP POLICY IF EXISTS "Coaches can delete exercises from their routines" ON workout_exercises;

-- Enable RLS
ALTER TABLE workout_exercises ENABLE ROW LEVEL SECURITY;

-- Create new policies
CREATE POLICY "workout_exercises_select_policy" ON workout_exercises
FOR SELECT TO authenticated
USING (
    workout_id IN (
        SELECT id FROM workouts WHERE coach_id = auth.uid()
    )
);

CREATE POLICY "workout_exercises_insert_policy" ON workout_exercises
FOR INSERT TO authenticated
WITH CHECK (
    workout_id IN (
        SELECT id FROM workouts WHERE coach_id = auth.uid()
    )
);

CREATE POLICY "workout_exercises_update_policy" ON workout_exercises
FOR UPDATE TO authenticated
USING (
    workout_id IN (
        SELECT id FROM workouts WHERE coach_id = auth.uid()
    )
)
WITH CHECK (
    workout_id IN (
        SELECT id FROM workouts WHERE coach_id = auth.uid()
    )
);

CREATE POLICY "workout_exercises_delete_policy" ON workout_exercises
FOR DELETE TO authenticated
USING (
    workout_id IN (
        SELECT id FROM workouts WHERE coach_id = auth.uid()
    )
);

-- ============================================================================
-- STEP 6: RLS POLICIES FOR WORKOUT_ASSIGNMENTS
-- ============================================================================

-- Drop old policies if they exist
DROP POLICY IF EXISTS "routine_assignments_select_policy" ON workout_assignments;
DROP POLICY IF EXISTS "routine_assignments_insert_policy" ON workout_assignments;
DROP POLICY IF EXISTS "routine_assignments_update_policy" ON workout_assignments;
DROP POLICY IF EXISTS "routine_assignments_delete_policy" ON workout_assignments;

-- Enable RLS
ALTER TABLE workout_assignments ENABLE ROW LEVEL SECURITY;

-- Create new policies
CREATE POLICY "workout_assignments_select_policy" ON workout_assignments
FOR SELECT TO authenticated
USING (coach_id = auth.uid() OR user_id = auth.uid());

CREATE POLICY "workout_assignments_insert_policy" ON workout_assignments
FOR INSERT TO authenticated
WITH CHECK (coach_id = auth.uid());

CREATE POLICY "workout_assignments_update_policy" ON workout_assignments
FOR UPDATE TO authenticated
USING (coach_id = auth.uid())
WITH CHECK (coach_id = auth.uid());

CREATE POLICY "workout_assignments_delete_policy" ON workout_assignments
FOR DELETE TO authenticated
USING (coach_id = auth.uid());

-- ============================================================================
-- VERIFY: Check that everything was renamed correctly
-- ============================================================================

SELECT 'Tables renamed:' as info;
SELECT tablename FROM pg_tables
WHERE schemaname = 'public'
AND tablename IN ('workouts', 'workout_exercises', 'workout_assignments');

SELECT 'Views created:' as info;
SELECT viewname FROM pg_views
WHERE schemaname = 'public'
AND viewname = 'coach_workouts_v';

SELECT 'Policies on workouts:' as info;
SELECT policyname, cmd FROM pg_policies WHERE tablename = 'workouts';

SELECT 'Migration complete! Tables renamed from routines to workouts.' as status;
