package com.prometheuscoach.mobile.data.repository

import android.util.Log
import com.prometheuscoach.mobile.data.model.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for workout/routine operations.
 * Uses coach_routines_v view for combined routine + exercise data.
 *
 * @see Prometheus Developer Guidelines v1.0.0
 */
@Singleton
class WorkoutRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository
) {
    companion object {
        private const val TAG = "WorkoutRepository"
    }

    // ==================== READ OPERATIONS ====================

    /**
     * Get all routines for the current coach (summary only, no exercises).
     * Uses routines table directly for list view.
     */
    suspend fun getRoutines(): Result<List<RoutineSummary>> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            // Get routines
            val routines = supabaseClient.postgrest
                .from("routines")
                .select {
                    filter { eq("coach_id", coachId) }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<Routine>()

            // Get exercise counts per routine via the view
            val viewRows = supabaseClient.postgrest
                .from("coach_routines_v")
                .select {
                    filter { eq("coach_id", coachId) }
                }
                .decodeList<CoachRoutineViewRow>()

            // Count exercises per routine
            val exerciseCounts = viewRows
                .filter { it.routineExerciseId != null }
                .groupBy { it.routineId }
                .mapValues { it.value.size }

            val summaries = routines.map { routine ->
                RoutineSummary(
                    id = routine.id,
                    name = routine.name,
                    description = routine.description,
                    exerciseCount = exerciseCounts[routine.id] ?: 0,
                    createdAt = routine.createdAt,
                    updatedAt = routine.updatedAt
                )
            }

            Log.d(TAG, "Loaded ${summaries.size} routines")
            Result.success(summaries)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get routines", e)
            Result.failure(e)
        }
    }

    /**
     * Get routine details with all exercises.
     * Uses coach_routines_v view for combined data.
     */
    suspend fun getRoutineWithExercises(routineId: String): Result<RoutineWithExercises> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            val viewRows = supabaseClient.postgrest
                .from("coach_routines_v")
                .select {
                    filter {
                        eq("routine_id", routineId)
                        eq("coach_id", coachId)
                    }
                    order("order_index", Order.ASCENDING)
                }
                .decodeList<CoachRoutineViewRow>()

            if (viewRows.isEmpty()) {
                return Result.failure(Exception("Routine not found"))
            }

            // First row contains routine header info
            val firstRow = viewRows.first()

            // Map exercises (filter out null exercise entries for empty routines)
            val exercises = viewRows
                .filter { it.routineExerciseId != null && it.exerciseId != null }
                .map { row ->
                    RoutineExerciseDetail(
                        routineExerciseId = row.routineExerciseId!!,
                        exerciseId = row.exerciseId!!,
                        orderIndex = row.orderIndex ?: 0,
                        sets = row.sets ?: 3,
                        repsMin = row.repsMin,
                        repsMax = row.repsMax,
                        restSeconds = row.restSeconds ?: 90,
                        notes = row.routineExerciseNotes,
                        exerciseTitle = row.exerciseTitle ?: "Unknown",
                        exerciseCategory = row.exerciseCategory,
                        exerciseThumbnailUrl = row.exerciseThumbnailUrl,
                        exerciseVideoUrl = row.exerciseVideoUrl,
                        primaryMuscles = row.primaryMuscles,
                        secondaryMuscles = row.secondaryMuscles
                    )
                }

            val routine = RoutineWithExercises(
                id = firstRow.routineId,
                coachId = firstRow.coachId,
                name = firstRow.routineName,
                description = firstRow.routineDescription,
                createdAt = firstRow.routineCreatedAt,
                updatedAt = firstRow.routineUpdatedAt,
                exercises = exercises
            )

            Log.d(TAG, "Loaded routine ${routine.name} with ${exercises.size} exercises")
            Result.success(routine)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get routine details", e)
            Result.failure(e)
        }
    }

    /**
     * Search exercises for picker.
     * Uses exercises_new table (same as client mobile app).
     */
    suspend fun searchExercises(
        query: String = "",
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<ExerciseListItem>> {
        return try {
            val exercises = supabaseClient.postgrest
                .from("exercises_new")
                .select {
                    if (query.isNotBlank()) {
                        filter { ilike("name", "%$query%") }
                    }
                    order("name", Order.ASCENDING)
                    limit(limit.toLong())
                    range(offset.toLong(), (offset + limit - 1).toLong())
                }
                .decodeList<ExerciseListItem>()

            Log.d(TAG, "Found ${exercises.size} exercises for query: $query")
            Result.success(exercises)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search exercises", e)
            Result.failure(e)
        }
    }

    /**
     * Get single exercise details.
     * Uses exercises_new table.
     */
    suspend fun getExercise(exerciseId: String): Result<Exercise> {
        return try {
            val exercise = supabaseClient.postgrest
                .from("exercises_new")
                .select {
                    filter { eq("id", exerciseId) }
                }
                .decodeSingle<Exercise>()

            Result.success(exercise)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get exercise", e)
            Result.failure(e)
        }
    }

    /**
     * Alias for getExercise - used by ExerciseDetailViewModel.
     */
    suspend fun getExerciseById(exerciseId: String): Result<Exercise> = getExercise(exerciseId)

    // ==================== WRITE OPERATIONS ====================

    /**
     * Create a new routine.
     */
    suspend fun createRoutine(name: String, description: String?): Result<Routine> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            val request = CreateRoutineRequest(
                coachId = coachId,
                name = name,
                description = description
            )

            val routine = supabaseClient.postgrest
                .from("routines")
                .insert(request) {
                    select()
                }
                .decodeSingle<Routine>()

            Log.d(TAG, "Created routine: ${routine.id}")
            Result.success(routine)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create routine", e)
            Result.failure(e)
        }
    }

    /**
     * Update routine name/description.
     */
    suspend fun updateRoutine(routineId: String, name: String, description: String?): Result<Unit> {
        return try {
            supabaseClient.postgrest
                .from("routines")
                .update(
                    mapOf(
                        "name" to name,
                        "description" to description,
                        "updated_at" to java.time.Instant.now().toString()
                    )
                ) {
                    filter { eq("id", routineId) }
                }

            Log.d(TAG, "Updated routine: $routineId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update routine", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a routine.
     * Note: This will cascade delete routine_exercises due to FK.
     */
    suspend fun deleteRoutine(routineId: String): Result<Unit> {
        return try {
            supabaseClient.postgrest
                .from("routines")
                .delete {
                    filter { eq("id", routineId) }
                }

            Log.d(TAG, "Deleted routine: $routineId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete routine", e)
            Result.failure(e)
        }
    }

    /**
     * Add an exercise to a routine.
     */
    suspend fun addExerciseToRoutine(
        routineId: String,
        exerciseId: String,
        orderIndex: Int,
        sets: Int = 3,
        repsMin: Int? = null,
        repsMax: Int? = null,
        restSeconds: Int = 90,
        notes: String? = null
    ): Result<RoutineExercise> {
        return try {
            // Validate
            require(sets > 0) { "Sets must be greater than 0" }
            require(restSeconds >= 0) { "Rest seconds must be >= 0" }
            if (repsMin != null && repsMax != null) {
                require(repsMin <= repsMax) { "reps_min must be <= reps_max" }
            }

            val request = AddRoutineExerciseRequest(
                routineId = routineId,
                exerciseId = exerciseId,
                orderIndex = orderIndex,
                sets = sets,
                repsMin = repsMin,
                repsMax = repsMax,
                restSeconds = restSeconds,
                notes = notes
            )

            val routineExercise = supabaseClient.postgrest
                .from("routine_exercises")
                .insert(request) {
                    select()
                }
                .decodeSingle<RoutineExercise>()

            // Update routine's updated_at timestamp
            updateRoutineTimestamp(routineId)

            Log.d(TAG, "Added exercise to routine: ${routineExercise.id}")
            Result.success(routineExercise)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add exercise to routine", e)
            Result.failure(e)
        }
    }

    /**
     * Update exercise parameters in a routine.
     */
    suspend fun updateRoutineExercise(
        routineExerciseId: String,
        sets: Int? = null,
        repsMin: Int? = null,
        repsMax: Int? = null,
        restSeconds: Int? = null,
        notes: String? = null
    ): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any?>()
            sets?.let {
                require(it > 0) { "Sets must be greater than 0" }
                updates["sets"] = it
            }
            repsMin?.let { updates["reps_min"] = it }
            repsMax?.let { updates["reps_max"] = it }
            restSeconds?.let {
                require(it >= 0) { "Rest seconds must be >= 0" }
                updates["rest_seconds"] = it
            }
            notes?.let { updates["notes"] = it }

            if (updates.isEmpty()) {
                return Result.success(Unit)
            }

            supabaseClient.postgrest
                .from("routine_exercises")
                .update(updates) {
                    filter { eq("id", routineExerciseId) }
                }

            Log.d(TAG, "Updated routine exercise: $routineExerciseId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update routine exercise", e)
            Result.failure(e)
        }
    }

    /**
     * Remove an exercise from a routine.
     */
    suspend fun removeExerciseFromRoutine(routineExerciseId: String, routineId: String): Result<Unit> {
        return try {
            supabaseClient.postgrest
                .from("routine_exercises")
                .delete {
                    filter { eq("id", routineExerciseId) }
                }

            // Reindex remaining exercises
            reindexRoutineExercises(routineId)

            // Update routine's updated_at timestamp
            updateRoutineTimestamp(routineId)

            Log.d(TAG, "Removed exercise from routine: $routineExerciseId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove exercise from routine", e)
            Result.failure(e)
        }
    }

    /**
     * Reorder exercises in a routine.
     * Takes a list of routine_exercise_ids in the new order.
     */
    suspend fun reorderExercises(routineId: String, orderedIds: List<String>): Result<Unit> {
        return try {
            orderedIds.forEachIndexed { index, routineExerciseId ->
                supabaseClient.postgrest
                    .from("routine_exercises")
                    .update(mapOf("order_index" to index)) {
                        filter { eq("id", routineExerciseId) }
                    }
            }

            updateRoutineTimestamp(routineId)

            Log.d(TAG, "Reordered ${orderedIds.size} exercises in routine: $routineId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reorder exercises", e)
            Result.failure(e)
        }
    }

    // ==================== HELPER METHODS ====================

    /**
     * Reindex all exercises in a routine to be sequential (0-based).
     */
    private suspend fun reindexRoutineExercises(routineId: String) {
        try {
            val exercises = supabaseClient.postgrest
                .from("routine_exercises")
                .select {
                    filter { eq("routine_id", routineId) }
                    order("order_index", Order.ASCENDING)
                }
                .decodeList<RoutineExercise>()

            exercises.forEachIndexed { index, exercise ->
                if (exercise.orderIndex != index) {
                    supabaseClient.postgrest
                        .from("routine_exercises")
                        .update(mapOf("order_index" to index)) {
                            filter { eq("id", exercise.id) }
                        }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to reindex exercises", e)
        }
    }

    /**
     * Update the routine's updated_at timestamp.
     */
    private suspend fun updateRoutineTimestamp(routineId: String) {
        try {
            supabaseClient.postgrest
                .from("routines")
                .update(mapOf("updated_at" to java.time.Instant.now().toString())) {
                    filter { eq("id", routineId) }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update routine timestamp", e)
        }
    }

    // ==================== ASSIGNMENT OPERATIONS ====================

    /**
     * Assign a workout/routine to a client.
     */
    suspend fun assignWorkoutToClient(
        routineId: String,
        clientId: String,
        scheduledDate: String? = null,
        notes: String? = null
    ): Result<RoutineAssignment> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            val request = AssignRoutineRequest(
                routineId = routineId,
                coachId = coachId,
                clientId = clientId,
                scheduledDate = scheduledDate,
                notes = notes
            )

            val assignment = supabaseClient.postgrest
                .from("routine_assignments")
                .insert(request) {
                    select()
                }
                .decodeSingle<RoutineAssignment>()

            Log.d(TAG, "Assigned workout $routineId to client $clientId")
            Result.success(assignment)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to assign workout to client", e)
            Result.failure(e)
        }
    }

    /**
     * Get all workouts assigned to a specific client.
     */
    suspend fun getClientAssignments(clientId: String): Result<List<AssignedWorkout>> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            // Get assignments for the client
            val assignments = supabaseClient.postgrest
                .from("routine_assignments")
                .select {
                    filter {
                        eq("client_id", clientId)
                        eq("coach_id", coachId)
                    }
                    order("assigned_at", Order.DESCENDING)
                }
                .decodeList<RoutineAssignment>()

            if (assignments.isEmpty()) {
                return Result.success(emptyList())
            }

            // Get routine details for these assignments
            val routineIds = assignments.map { it.routineId }.distinct()
            val routines = supabaseClient.postgrest
                .from("routines")
                .select {
                    filter { isIn("id", routineIds) }
                }
                .decodeList<Routine>()

            // Get exercise counts
            val viewRows = supabaseClient.postgrest
                .from("coach_routines_v")
                .select {
                    filter {
                        eq("coach_id", coachId)
                        isIn("routine_id", routineIds)
                    }
                }
                .decodeList<CoachRoutineViewRow>()

            val exerciseCounts = viewRows
                .filter { it.routineExerciseId != null }
                .groupBy { it.routineId }
                .mapValues { it.value.size }

            val routineMap = routines.associateBy { it.id }

            val assignedWorkouts = assignments.mapNotNull { assignment ->
                val routine = routineMap[assignment.routineId] ?: return@mapNotNull null
                AssignedWorkout(
                    assignmentId = assignment.id,
                    routineId = routine.id,
                    routineName = routine.name,
                    routineDescription = routine.description,
                    exerciseCount = exerciseCounts[routine.id] ?: 0,
                    assignedAt = assignment.assignedAt,
                    scheduledDate = assignment.scheduledDate,
                    notes = assignment.notes,
                    status = assignment.status
                )
            }

            Log.d(TAG, "Loaded ${assignedWorkouts.size} assignments for client $clientId")
            Result.success(assignedWorkouts)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get client assignments", e)
            Result.failure(e)
        }
    }

    /**
     * Remove a workout assignment.
     */
    suspend fun removeAssignment(assignmentId: String): Result<Unit> {
        return try {
            supabaseClient.postgrest
                .from("routine_assignments")
                .delete {
                    filter { eq("id", assignmentId) }
                }

            Log.d(TAG, "Removed assignment: $assignmentId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove assignment", e)
            Result.failure(e)
        }
    }

    /**
     * Update assignment status (e.g., 'active', 'completed', 'cancelled').
     */
    suspend fun updateAssignmentStatus(assignmentId: String, status: String): Result<Unit> {
        return try {
            supabaseClient.postgrest
                .from("routine_assignments")
                .update(mapOf("status" to status)) {
                    filter { eq("id", assignmentId) }
                }

            Log.d(TAG, "Updated assignment status: $assignmentId -> $status")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update assignment status", e)
            Result.failure(e)
        }
    }
}
