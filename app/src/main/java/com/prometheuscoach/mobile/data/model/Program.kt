package com.prometheuscoach.mobile.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Program models for multi-week training programs.
 * Programs contain weeks, and weeks contain workouts.
 */

@Serializable
data class Program(
    val id: String,
    @SerialName("coach_id")
    val coachId: String,
    val name: String,
    val description: String? = null,
    @SerialName("duration_weeks")
    val durationWeeks: Int,
    @SerialName("workouts_per_week")
    val workoutsPerWeek: Int,
    val difficulty: String? = null,
    val status: String = "draft", // draft, active, archived
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String
)

/**
 * Summary model for program list view.
 */
data class ProgramSummary(
    val id: String,
    val name: String,
    val description: String?,
    val durationWeeks: Int,
    val workoutsPerWeek: Int,
    val difficulty: String?,
    val status: String,
    val createdAt: String
)

/**
 * Program week - represents one week in a program.
 */
@Serializable
data class ProgramWeek(
    val id: String,
    @SerialName("program_id")
    val programId: String,
    @SerialName("week_number")
    val weekNumber: Int,
    val name: String? = null,
    val description: String? = null,
    @SerialName("created_at")
    val createdAt: String
)

/**
 * Program workout - a workout assigned to a specific day in a week.
 */
@Serializable
data class ProgramWorkout(
    val id: String,
    @SerialName("program_week_id")
    val programWeekId: String,
    @SerialName("routine_id")
    val routineId: String,
    @SerialName("day_number")
    val dayNumber: Int, // 1-7 for Monday-Sunday
    val notes: String? = null,
    @SerialName("created_at")
    val createdAt: String
)

/**
 * Request model for creating a program.
 */
@Serializable
data class CreateProgramRequest(
    @SerialName("coach_id")
    val coachId: String,
    val name: String,
    val description: String? = null,
    @SerialName("duration_weeks")
    val durationWeeks: Int,
    @SerialName("workouts_per_week")
    val workoutsPerWeek: Int,
    val difficulty: String? = null
)

/**
 * Request model for creating a program week.
 */
@Serializable
data class CreateProgramWeekRequest(
    @SerialName("program_id")
    val programId: String,
    @SerialName("week_number")
    val weekNumber: Int,
    val name: String? = null,
    val description: String? = null
)

/**
 * Request model for adding a workout to a program week.
 */
@Serializable
data class AddProgramWorkoutRequest(
    @SerialName("program_week_id")
    val programWeekId: String,
    @SerialName("routine_id")
    val routineId: String,
    @SerialName("day_number")
    val dayNumber: Int,
    val notes: String? = null
)

/**
 * UI model for a week with its workouts.
 */
data class ProgramWeekWithWorkouts(
    val id: String,
    val weekNumber: Int,
    val name: String?,
    val description: String?,
    val workouts: List<ProgramWorkoutDetail>
)

/**
 * UI model for a workout in a program week.
 */
data class ProgramWorkoutDetail(
    val id: String,
    val dayNumber: Int,
    val routineId: String,
    val routineName: String,
    val exerciseCount: Int,
    val notes: String?
)

/**
 * Full program with all weeks and workouts.
 */
data class ProgramWithWeeks(
    val id: String,
    val coachId: String,
    val name: String,
    val description: String?,
    val durationWeeks: Int,
    val workoutsPerWeek: Int,
    val difficulty: String?,
    val status: String,
    val weeks: List<ProgramWeekWithWorkouts>
)

/**
 * Program assignment - assigns a program to a client.
 */
@Serializable
data class ProgramAssignment(
    val id: String,
    @SerialName("program_id")
    val programId: String,
    @SerialName("coach_id")
    val coachId: String,
    @SerialName("user_id")
    val clientId: String,
    @SerialName("start_date")
    val startDate: String,
    @SerialName("current_week")
    val currentWeek: Int = 1,
    val status: String = "active", // active, paused, completed
    @SerialName("assigned_at")
    val assignedAt: String
)