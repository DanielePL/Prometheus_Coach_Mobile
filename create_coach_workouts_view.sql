-- ============================================================================
-- CREATE COACH_WORKOUTS_V VIEW
-- This view combines workouts, workout_exercises, and exercises_new tables
-- for the Coach Mobile App to load workout details
-- Run this in the Supabase SQL Editor
-- ============================================================================

-- Drop existing view if any
DROP VIEW IF EXISTS coach_workouts_v;

-- Create the combined view
-- workout_exercises has: id, workout_id, exercise_id, order_index, sets, notes, created_at, target_reps, target_weight
-- exercises_new has: id, name, category, main_muscle_group, secondary_muscle_groups, video_url, etc.
CREATE VIEW coach_workouts_v AS
SELECT
    -- Workout columns
    w.id AS workout_id,
    w.coach_id,
    w.name AS workout_name,
    w.description AS workout_description,
    w.created_at AS workout_created_at,
    w.updated_at AS workout_updated_at,

    -- Workout Exercise columns (from junction table)
    we.id AS workout_exercise_id,
    we.order_index,
    COALESCE(we.sets, 3) AS sets,
    we.target_reps AS reps_min,
    we.target_reps AS reps_max,
    90 AS rest_seconds,  -- Default value, column doesn't exist in workout_exercises
    we.notes AS workout_exercise_notes,

    -- Exercise columns from exercises_new table
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

-- Grant permissions to authenticated users
GRANT SELECT ON coach_workouts_v TO authenticated;

-- ============================================================================
-- VERIFY: Test the view
-- ============================================================================
-- SELECT * FROM coach_workouts_v LIMIT 10;

SELECT 'coach_workouts_v view created successfully!' as status;
