package com.prometheuscoach.mobile.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Workout completion record from workout_logs table.
 */
@Serializable
data class WorkoutLog(
    val id: String,
    @SerialName("user_id")
    val clientId: String,
    @SerialName("routine_id")
    val routineId: String? = null,
    @SerialName("routine_name")
    val routineName: String? = null,
    @SerialName("started_at")
    val startedAt: String,
    @SerialName("completed_at")
    val completedAt: String? = null,
    @SerialName("duration_minutes")
    val durationMinutes: Int? = null,
    val notes: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)

/**
 * Exercise performance record from exercise_logs table.
 */
@Serializable
data class ExerciseLog(
    val id: String,
    @SerialName("workout_log_id")
    val workoutLogId: String,
    @SerialName("exercise_id")
    val exerciseId: String,
    @SerialName("exercise_name")
    val exerciseName: String? = null,
    @SerialName("set_number")
    val setNumber: Int,
    val reps: Int? = null,
    val weight: Double? = null,
    @SerialName("weight_unit")
    val weightUnit: String = "kg",
    val rpe: Int? = null,
    val notes: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)

/**
 * Personal record/best for an exercise.
 */
@Serializable
data class PersonalBest(
    @SerialName("exercise_id")
    val exerciseId: String,
    @SerialName("exercise_name")
    val exerciseName: String,
    @SerialName("best_weight")
    val bestWeight: Double? = null,
    @SerialName("best_reps")
    val bestReps: Int? = null,
    @SerialName("achieved_at")
    val achievedAt: String,
    @SerialName("previous_best_weight")
    val previousBestWeight: Double? = null
)

/**
 * Weekly progress summary.
 */
data class WeeklyProgress(
    val weekStart: String,
    val workoutsCompleted: Int,
    val totalMinutes: Int,
    val exercisesPerformed: Int,
    val personalBests: Int
)

/**
 * Client progress overview for coach dashboard.
 */
data class ClientProgressOverview(
    val clientId: String,
    val clientName: String,
    val clientAvatar: String?,
    val totalWorkouts: Int,
    val workoutsThisWeek: Int,
    val currentStreak: Int,
    val lastWorkoutDate: String?,
    val recentPersonalBests: List<PersonalBest>
)

/**
 * Detailed progress stats for a specific exercise.
 */
data class ExerciseProgressStats(
    val exerciseId: String,
    val exerciseName: String,
    val category: String?,
    val totalSets: Int,
    val totalReps: Int,
    val maxWeight: Double?,
    val avgWeight: Double?,
    val progressPercentage: Double?,
    val history: List<ExerciseHistoryEntry>
)

/**
 * Single entry in exercise history.
 */
data class ExerciseHistoryEntry(
    val date: String,
    val sets: Int,
    val bestSet: BestSetInfo
)

/**
 * Best set info for display.
 */
data class BestSetInfo(
    val reps: Int,
    val weight: Double?,
    val rpe: Int?
)

/**
 * Progress summary for a time period.
 */
data class ProgressSummary(
    val period: String, // "week", "month", "all_time"
    val workoutsCompleted: Int,
    val totalDuration: Int,
    val exercisesPerformed: Int,
    val personalBests: Int,
    val streakDays: Int,
    val avgWorkoutsPerWeek: Double
)
