package com.prometheuscoach.mobile.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Base Routine entity from public.routines table.
 */
@Serializable
data class Routine(
    val id: String,
    @SerialName("coach_id")
    val coachId: String,
    val name: String,
    val description: String? = null,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String
)

/**
 * Exercise entity from public.exercises_new table.
 * Matches the schema used by the client mobile app.
 */
@Serializable
data class Exercise(
    val id: String,
    val name: String,
    val category: String? = null,
    @SerialName("main_muscle_group")
    val mainMuscleGroup: String? = null,
    @SerialName("secondary_muscle_groups")
    val secondaryMuscleGroups: List<String>? = null,
    val equipment: List<String>? = null,
    val level: String? = null,
    val visibility: String? = null,
    @SerialName("owner_id")
    val ownerId: String? = null,
    val tempo: String? = null,
    @SerialName("rest_time_seconds")
    val restTimeSeconds: Int? = null,
    @SerialName("track_reps")
    val trackReps: Boolean = true,
    @SerialName("track_sets")
    val trackSets: Boolean = true,
    @SerialName("track_weight")
    val trackWeight: Boolean = true,
    @SerialName("track_rpe")
    val trackRpe: Boolean = false,
    @SerialName("video_url")
    val videoUrl: String? = null,
    val tutorial: String? = null,
    val notes: String? = null,
    @SerialName("vbt_enabled")
    val vbtEnabled: Boolean = false,
    val sports: List<String>? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)

/**
 * Simplified exercise for picker/list views.
 * Uses exercises_new table with 'name' field.
 */
@Serializable
data class ExerciseListItem(
    val id: String,
    val name: String,
    val category: String? = null,
    @SerialName("main_muscle_group")
    val mainMuscleGroup: String? = null,
    val equipment: List<String>? = null,
    val sports: List<String>? = null
) {
    /** Display name for UI */
    val title: String get() = name

    /** Thumbnail URL - exercises_new doesn't have thumbnails */
    val thumbnailUrl: String? get() = null
}

/**
 * Routine exercise from public.routine_exercises table.
 */
@Serializable
data class RoutineExercise(
    val id: String,
    @SerialName("routine_id")
    val routineId: String,
    @SerialName("exercise_id")
    val exerciseId: String,
    @SerialName("order_index")
    val orderIndex: Int,
    val sets: Int = 3,
    @SerialName("reps_min")
    val repsMin: Int? = null,
    @SerialName("reps_max")
    val repsMax: Int? = null,
    @SerialName("rest_seconds")
    val restSeconds: Int = 90,
    val notes: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)

/**
 * Combined view from public.coach_routines_v.
 * Contains routine + exercise details in one row per exercise.
 */
@Serializable
data class CoachRoutineViewRow(
    @SerialName("routine_id")
    val routineId: String,
    @SerialName("coach_id")
    val coachId: String,
    @SerialName("routine_name")
    val routineName: String,
    @SerialName("routine_description")
    val routineDescription: String? = null,
    @SerialName("routine_created_at")
    val routineCreatedAt: String,
    @SerialName("routine_updated_at")
    val routineUpdatedAt: String,
    @SerialName("routine_exercise_id")
    val routineExerciseId: String? = null,
    @SerialName("order_index")
    val orderIndex: Int? = null,
    val sets: Int? = null,
    @SerialName("reps_min")
    val repsMin: Int? = null,
    @SerialName("reps_max")
    val repsMax: Int? = null,
    @SerialName("rest_seconds")
    val restSeconds: Int? = null,
    @SerialName("routine_exercise_notes")
    val routineExerciseNotes: String? = null,
    @SerialName("exercise_id")
    val exerciseId: String? = null,
    @SerialName("exercise_title")
    val exerciseTitle: String? = null,
    @SerialName("exercise_category")
    val exerciseCategory: String? = null,
    @SerialName("exercise_thumbnail_url")
    val exerciseThumbnailUrl: String? = null,
    @SerialName("exercise_video_url")
    val exerciseVideoUrl: String? = null,
    @SerialName("primary_muscles")
    val primaryMuscles: String? = null,
    @SerialName("secondary_muscles")
    val secondaryMuscles: String? = null
)

/**
 * UI model for a routine with its exercises.
 */
data class RoutineWithExercises(
    val id: String,
    val coachId: String,
    val name: String,
    val description: String?,
    val createdAt: String,
    val updatedAt: String,
    val exercises: List<RoutineExerciseDetail>
)

/**
 * UI model for an exercise within a routine.
 */
data class RoutineExerciseDetail(
    val routineExerciseId: String,
    val exerciseId: String,
    val orderIndex: Int,
    val sets: Int,
    val repsMin: Int?,
    val repsMax: Int?,
    val restSeconds: Int,
    val notes: String?,
    val exerciseTitle: String,
    val exerciseCategory: String?,
    val exerciseThumbnailUrl: String?,
    val exerciseVideoUrl: String?,
    val primaryMuscles: String?,
    val secondaryMuscles: String?
) {
    /**
     * Format reps as "min-max" or just "min" if equal/null.
     */
    val repsDisplay: String
        get() = when {
            repsMin == null && repsMax == null -> "-"
            repsMin == repsMax || repsMax == null -> "$repsMin"
            repsMin == null -> "$repsMax"
            else -> "$repsMin-$repsMax"
        }

    /**
     * Format rest as "Xs" or "Xm Ys".
     */
    val restDisplay: String
        get() = when {
            restSeconds < 60 -> "${restSeconds}s"
            restSeconds % 60 == 0 -> "${restSeconds / 60}m"
            else -> "${restSeconds / 60}m ${restSeconds % 60}s"
        }
}

/**
 * Summary model for routine list (header only).
 */
data class RoutineSummary(
    val id: String,
    val name: String,
    val description: String?,
    val exerciseCount: Int,
    val createdAt: String,
    val updatedAt: String
)

/**
 * Request model for creating a routine.
 */
@Serializable
data class CreateRoutineRequest(
    @SerialName("coach_id")
    val coachId: String,
    val name: String,
    val description: String? = null
)

/**
 * Request model for updating a routine.
 */
@Serializable
data class UpdateRoutineRequest(
    val name: String,
    val description: String? = null
)

/**
 * Request model for adding an exercise to a routine.
 */
@Serializable
data class AddRoutineExerciseRequest(
    @SerialName("routine_id")
    val routineId: String,
    @SerialName("exercise_id")
    val exerciseId: String,
    @SerialName("order_index")
    val orderIndex: Int,
    val sets: Int = 3,
    @SerialName("reps_min")
    val repsMin: Int? = null,
    @SerialName("reps_max")
    val repsMax: Int? = null,
    @SerialName("rest_seconds")
    val restSeconds: Int = 90,
    val notes: String? = null
)

/**
 * Request model for updating exercise order.
 */
@Serializable
data class UpdateOrderRequest(
    @SerialName("order_index")
    val orderIndex: Int
)

// ==================== ROUTINE ASSIGNMENT MODELS ====================

/**
 * Routine assignment entity from public.routine_assignments table.
 * Represents a workout assigned to a client by a coach.
 */
@Serializable
data class RoutineAssignment(
    val id: String,
    @SerialName("routine_id")
    val routineId: String,
    @SerialName("coach_id")
    val coachId: String,
    @SerialName("user_id")
    val clientId: String,
    @SerialName("assigned_at")
    val assignedAt: String,
    @SerialName("scheduled_date")
    val scheduledDate: String? = null,
    val notes: String? = null,
    val status: String = "active"
)

/**
 * Request model for assigning a routine to a client.
 */
@Serializable
data class AssignRoutineRequest(
    @SerialName("routine_id")
    val routineId: String,
    @SerialName("coach_id")
    val coachId: String,
    @SerialName("user_id")
    val clientId: String,
    @SerialName("scheduled_date")
    val scheduledDate: String? = null,
    val notes: String? = null
)

/**
 * UI model for displaying an assigned workout with routine details.
 */
data class AssignedWorkout(
    val assignmentId: String,
    val routineId: String,
    val routineName: String,
    val routineDescription: String?,
    val exerciseCount: Int,
    val assignedAt: String,
    val scheduledDate: String?,
    val notes: String?,
    val status: String
)
