package com.prometheuscoach.mobile.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ==================== TECHNIQUE GUIDE MODELS ====================

/**
 * Technique guide section from exercise_technique_guides table.
 * Matches the JSONB structure: {"title": "...", "bullets": ["...", "..."]}
 */
@Serializable
data class TechniqueSection(
    @SerialName("title")
    val title: String = "",
    @SerialName("bullets")
    val bullets: List<String> = emptyList()
)

// ==================== WORKOUT MODELS ====================

/**
 * Base Workout entity from public.workouts table.
 */
@Serializable
data class Workout(
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
 * Exercise entity from public.exercise_view (joins exercises_new with technique_guides).
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
    @SerialName("track_duration")
    val trackDuration: Boolean = false,
    @SerialName("track_distance")
    val trackDistance: Boolean = false,
    @SerialName("video_url")
    val videoUrl: String? = null,
    val tutorial: String? = null,
    val notes: String? = null,
    @SerialName("vbt_enabled")
    val vbtEnabled: Boolean = false,
    @SerialName("supports_power_score")
    val supportsPowerScore: Boolean = false,
    @SerialName("supports_technique_score")
    val supportsTechniqueScore: Boolean = false,
    val sports: List<String>? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    // Technique guide sections from exercise_view join
    @SerialName("technique_sections")
    val techniqueSections: List<TechniqueSection>? = null
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
    @SerialName("secondary_muscle_groups")
    val secondaryMuscleGroups: List<String>? = null,
    val equipment: List<String>? = null,
    val sports: List<String>? = null,
    @SerialName("track_reps")
    val trackReps: Boolean = true,
    @SerialName("track_weight")
    val trackWeight: Boolean = true,
    @SerialName("track_duration")
    val trackDuration: Boolean = false,
    @SerialName("track_distance")
    val trackDistance: Boolean = false,
    @SerialName("track_rpe")
    val trackRpe: Boolean = false,
    @SerialName("vbt_enabled")
    val vbtEnabled: Boolean = false,
    @SerialName("supports_power_score")
    val supportsPowerScore: Boolean = false,
    @SerialName("supports_technique_score")
    val supportsTechniqueScore: Boolean = false
) {
    /** Display name for UI */
    val title: String get() = name

    /** Thumbnail URL - exercises_new doesn't have thumbnails */
    val thumbnailUrl: String? get() = null
}

/**
 * Workout exercise from public.workout_exercises table.
 */
@Serializable
data class WorkoutExercise(
    val id: String,
    @SerialName("workout_id")
    val workoutId: String,
    @SerialName("exercise_id")
    val exerciseId: String,
    @SerialName("order_index")
    val orderIndex: Int,
    val sets: Int = 3,
    @SerialName("target_reps")
    val targetReps: Int? = null,
    val notes: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)

/**
 * Combined view from public.coach_workouts_v.
 * Contains workout + exercise details in one row per exercise.
 */
@Serializable
data class CoachWorkoutViewRow(
    @SerialName("workout_id")
    val workoutId: String,
    @SerialName("coach_id")
    val coachId: String,
    @SerialName("workout_name")
    val workoutName: String,
    @SerialName("workout_description")
    val workoutDescription: String? = null,
    @SerialName("workout_created_at")
    val workoutCreatedAt: String,
    @SerialName("workout_updated_at")
    val workoutUpdatedAt: String,
    @SerialName("workout_exercise_id")
    val workoutExerciseId: String? = null,
    @SerialName("order_index")
    val orderIndex: Int? = null,
    val sets: Int? = null,
    @SerialName("target_reps")
    val targetReps: Int? = null,
    @SerialName("workout_exercise_notes")
    val workoutExerciseNotes: String? = null,
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
 * UI model for a workout with its exercises.
 */
data class WorkoutWithExercises(
    val id: String,
    val coachId: String,
    val name: String,
    val description: String?,
    val createdAt: String,
    val updatedAt: String,
    val exercises: List<WorkoutExerciseDetail>
)

/**
 * UI model for an exercise within a workout.
 */
data class WorkoutExerciseDetail(
    val workoutExerciseId: String,
    val exerciseId: String,
    val orderIndex: Int,
    val sets: Int,
    val targetReps: Int?,
    val notes: String?,
    val exerciseTitle: String,
    val exerciseCategory: String?,
    val exerciseThumbnailUrl: String?,
    val exerciseVideoUrl: String?,
    val primaryMuscles: String?,
    val secondaryMuscles: String?
) {
    /**
     * Format reps display.
     */
    val repsDisplay: String
        get() = targetReps?.toString() ?: "-"
}

/**
 * Summary model for workout list (header only).
 */
data class WorkoutSummary(
    val id: String,
    val name: String,
    val description: String?,
    val exerciseCount: Int,
    val createdAt: String,
    val updatedAt: String
)

/**
 * Request model for creating a workout.
 */
@Serializable
data class CreateWorkoutRequest(
    @SerialName("coach_id")
    val coachId: String,
    val name: String,
    val description: String? = null
)

/**
 * Request model for updating a workout.
 */
@Serializable
data class UpdateWorkoutRequest(
    val name: String,
    val description: String? = null
)

/**
 * Request model for adding an exercise to a workout.
 */
@Serializable
data class AddWorkoutExerciseRequest(
    @SerialName("workout_id")
    val workoutId: String,
    @SerialName("exercise_id")
    val exerciseId: String,
    @SerialName("order_index")
    val orderIndex: Int,
    val sets: Int = 3,
    @SerialName("target_reps")
    val targetReps: Int? = null,
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

// ==================== WORKOUT ASSIGNMENT MODELS ====================
// Note: WorkoutTemplate is defined in Template.kt

/**
 * Workout assignment entity from public.workout_assignments table.
 * Represents a workout assigned to a client by a coach.
 *
 * Note: DB uses 'workout_template_id' and 'client_id' column names.
 */
@Serializable
data class WorkoutAssignment(
    val id: String,
    @SerialName("workout_template_id")
    val workoutId: String,
    @SerialName("coach_id")
    val coachId: String,
    @SerialName("client_id")
    val clientId: String,
    @SerialName("assigned_at")
    val assignedAt: String,
    @SerialName("scheduled_date")
    val scheduledDate: String? = null,
    val notes: String? = null,
    val status: String = "active"
)

/**
 * Request model for assigning a workout to a client.
 */
@Serializable
data class AssignWorkoutRequest(
    @SerialName("workout_template_id")
    val workoutId: String,
    @SerialName("coach_id")
    val coachId: String,
    @SerialName("client_id")
    val clientId: String,
    @SerialName("scheduled_date")
    val scheduledDate: String? = null,
    val notes: String? = null
)

/**
 * Request model for updating an assignment.
 */
@Serializable
data class UpdateAssignmentRequest(
    val notes: String? = null,
    @SerialName("scheduled_date")
    val scheduledDate: String? = null,
    val status: String? = null
)

/**
 * UI model for displaying an assigned workout with workout details.
 */
data class AssignedWorkout(
    val assignmentId: String,
    val workoutId: String,
    val workoutName: String,
    val workoutDescription: String?,
    val exerciseCount: Int,
    val assignedAt: String,
    val scheduledDate: String?,
    val notes: String?,
    val status: String,
    val exercises: List<AssignedExerciseInfo> = emptyList()
)

/**
 * Exercise info for assigned workout display (V1 compatible).
 * Contains full set details from exercise_sets table.
 */
data class AssignedExerciseInfo(
    val workoutExerciseId: String,
    val exerciseId: String,
    val name: String,
    val muscleGroup: String?,
    val equipment: String?,
    val orderIndex: Int,
    val sets: List<ExerciseSetInfo> = emptyList()
) {
    val setCount: Int get() = sets.size
    val totalReps: Int get() = sets.sumOf { it.targetReps }
}

/**
 * Set details from exercise_sets table (V1 compatible).
 */
data class ExerciseSetInfo(
    val id: String,
    val setNumber: Int,
    val targetReps: Int,
    val targetWeight: Double,
    val restSeconds: Int
)
