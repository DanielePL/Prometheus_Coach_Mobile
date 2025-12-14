package com.prometheuscoach.mobile.data.repository

import android.util.Log
import com.prometheuscoach.mobile.data.model.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for program operations.
 * Programs are multi-week training plans that contain workouts.
 */
@Singleton
class ProgramRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository
) {
    companion object {
        private const val TAG = "ProgramRepository"
    }

    // ==================== READ OPERATIONS ====================

    /**
     * Get all programs for the current coach.
     */
    suspend fun getPrograms(): Result<List<ProgramSummary>> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            val programs = supabaseClient.postgrest
                .from("programs")
                .select {
                    filter { eq("coach_id", coachId) }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<Program>()

            val summaries = programs.map { program ->
                ProgramSummary(
                    id = program.id,
                    name = program.name,
                    description = program.description,
                    durationWeeks = program.durationWeeks,
                    workoutsPerWeek = program.workoutsPerWeek,
                    difficulty = program.difficulty,
                    status = program.status,
                    createdAt = program.createdAt
                )
            }

            Log.d(TAG, "Loaded ${summaries.size} programs")
            Result.success(summaries)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get programs", e)
            Result.failure(e)
        }
    }

    /**
     * Get a single program with all its weeks and workouts.
     */
    suspend fun getProgramWithWeeks(programId: String): Result<ProgramWithWeeks> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            // Get the program
            val program = supabaseClient.postgrest
                .from("programs")
                .select {
                    filter {
                        eq("id", programId)
                        eq("coach_id", coachId)
                    }
                }
                .decodeSingle<Program>()

            // Get weeks
            val weeks = supabaseClient.postgrest
                .from("program_weeks")
                .select {
                    filter { eq("program_id", programId) }
                    order("week_number", Order.ASCENDING)
                }
                .decodeList<ProgramWeek>()

            // Get workouts for all weeks
            val weekIds = weeks.map { it.id }
            val workouts = if (weekIds.isNotEmpty()) {
                supabaseClient.postgrest
                    .from("program_workouts")
                    .select {
                        filter { isIn("program_week_id", weekIds) }
                        order("day_number", Order.ASCENDING)
                    }
                    .decodeList<ProgramWorkout>()
            } else {
                emptyList()
            }

            // Get routine details for workouts
            val routineIds = workouts.map { it.routineId }.distinct()
            val routines = if (routineIds.isNotEmpty()) {
                supabaseClient.postgrest
                    .from("routines")
                    .select {
                        filter { isIn("id", routineIds) }
                    }
                    .decodeList<Routine>()
            } else {
                emptyList()
            }
            val routineMap = routines.associateBy { it.id }

            // Build the full structure
            val weeksWithWorkouts = weeks.map { week ->
                val weekWorkouts = workouts
                    .filter { it.programWeekId == week.id }
                    .map { workout ->
                        val routine = routineMap[workout.routineId]
                        ProgramWorkoutDetail(
                            id = workout.id,
                            dayNumber = workout.dayNumber,
                            routineId = workout.routineId,
                            routineName = routine?.name ?: "Unknown",
                            exerciseCount = 0, // TODO: Get exercise count
                            notes = workout.notes
                        )
                    }

                ProgramWeekWithWorkouts(
                    id = week.id,
                    weekNumber = week.weekNumber,
                    name = week.name,
                    description = week.description,
                    workouts = weekWorkouts
                )
            }

            val result = ProgramWithWeeks(
                id = program.id,
                coachId = program.coachId,
                name = program.name,
                description = program.description,
                durationWeeks = program.durationWeeks,
                workoutsPerWeek = program.workoutsPerWeek,
                difficulty = program.difficulty,
                status = program.status,
                weeks = weeksWithWorkouts
            )

            Log.d(TAG, "Loaded program ${program.name} with ${weeks.size} weeks")
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get program with weeks", e)
            Result.failure(e)
        }
    }

    // ==================== WRITE OPERATIONS ====================

    /**
     * Create a new program with empty weeks.
     */
    suspend fun createProgram(
        name: String,
        description: String?,
        durationWeeks: Int,
        workoutsPerWeek: Int,
        difficulty: String?
    ): Result<Program> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            require(durationWeeks in 1..52) { "Duration must be between 1 and 52 weeks" }
            require(workoutsPerWeek in 1..7) { "Workouts per week must be between 1 and 7" }

            val request = CreateProgramRequest(
                coachId = coachId,
                name = name,
                description = description,
                durationWeeks = durationWeeks,
                workoutsPerWeek = workoutsPerWeek,
                difficulty = difficulty
            )

            val program = supabaseClient.postgrest
                .from("programs")
                .insert(request) {
                    select()
                }
                .decodeSingle<Program>()

            // Create empty weeks
            for (weekNum in 1..durationWeeks) {
                val weekRequest = CreateProgramWeekRequest(
                    programId = program.id,
                    weekNumber = weekNum,
                    name = "Week $weekNum"
                )
                supabaseClient.postgrest
                    .from("program_weeks")
                    .insert(weekRequest)
            }

            Log.d(TAG, "Created program: ${program.id} with $durationWeeks weeks")
            Result.success(program)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create program", e)
            Result.failure(e)
        }
    }

    /**
     * Update program details.
     */
    suspend fun updateProgram(
        programId: String,
        name: String,
        description: String?,
        difficulty: String?
    ): Result<Unit> {
        return try {
            supabaseClient.postgrest
                .from("programs")
                .update(
                    mapOf(
                        "name" to name,
                        "description" to description,
                        "difficulty" to difficulty,
                        "updated_at" to java.time.Instant.now().toString()
                    )
                ) {
                    filter { eq("id", programId) }
                }

            Log.d(TAG, "Updated program: $programId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update program", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a program (cascades to weeks and workouts).
     */
    suspend fun deleteProgram(programId: String): Result<Unit> {
        return try {
            supabaseClient.postgrest
                .from("programs")
                .delete {
                    filter { eq("id", programId) }
                }

            Log.d(TAG, "Deleted program: $programId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete program", e)
            Result.failure(e)
        }
    }

    /**
     * Add a workout to a program week.
     */
    suspend fun addWorkoutToWeek(
        programWeekId: String,
        routineId: String,
        dayNumber: Int,
        notes: String? = null
    ): Result<ProgramWorkout> {
        return try {
            require(dayNumber in 1..7) { "Day number must be between 1 and 7" }

            val request = AddProgramWorkoutRequest(
                programWeekId = programWeekId,
                routineId = routineId,
                dayNumber = dayNumber,
                notes = notes
            )

            val workout = supabaseClient.postgrest
                .from("program_workouts")
                .insert(request) {
                    select()
                }
                .decodeSingle<ProgramWorkout>()

            Log.d(TAG, "Added workout to week: ${workout.id}")
            Result.success(workout)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add workout to week", e)
            Result.failure(e)
        }
    }

    /**
     * Remove a workout from a program week.
     */
    suspend fun removeWorkoutFromWeek(programWorkoutId: String): Result<Unit> {
        return try {
            supabaseClient.postgrest
                .from("program_workouts")
                .delete {
                    filter { eq("id", programWorkoutId) }
                }

            Log.d(TAG, "Removed workout from week: $programWorkoutId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove workout from week", e)
            Result.failure(e)
        }
    }

    // ==================== ASSIGNMENT OPERATIONS ====================

    /**
     * Assign a program to a client.
     */
    suspend fun assignProgramToClient(
        programId: String,
        clientId: String,
        startDate: String
    ): Result<ProgramAssignment> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            val assignment = supabaseClient.postgrest
                .from("program_assignments")
                .insert(
                    mapOf(
                        "program_id" to programId,
                        "coach_id" to coachId,
                        "user_id" to clientId,
                        "start_date" to startDate
                    )
                ) {
                    select()
                }
                .decodeSingle<ProgramAssignment>()

            Log.d(TAG, "Assigned program $programId to client $clientId")
            Result.success(assignment)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to assign program", e)
            Result.failure(e)
        }
    }
}