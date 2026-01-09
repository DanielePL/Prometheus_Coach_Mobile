package com.prometheuscoach.mobile.data.repository

import android.util.Log
import com.prometheuscoach.mobile.data.cache.CacheKeys
import com.prometheuscoach.mobile.data.cache.SessionCache
import com.prometheuscoach.mobile.data.model.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for workout operations.
 * Uses coach_workouts_v view for combined workout + exercise data.
 *
 * @see Prometheus Developer Guidelines v1.0.0
 */
@Singleton
class WorkoutRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository,
    private val cache: SessionCache
) {
    companion object {
        private const val TAG = "WorkoutRepository"
    }

    // ==================== READ OPERATIONS ====================

    /**
     * Get all workouts for the current coach (summary only, no exercises).
     * Uses workouts table directly for list view.
     * Results are cached for 5 minutes.
     */
    suspend fun getWorkouts(forceRefresh: Boolean = false): Result<List<WorkoutSummary>> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            // Check cache first
            if (!forceRefresh) {
                cache.get<List<WorkoutSummary>>(CacheKeys.WORKOUTS)?.let {
                    Log.d(TAG, "Returning ${it.size} cached workouts")
                    return Result.success(it)
                }
            }

            // Get workouts
            val workouts = supabaseClient.postgrest
                .from("workouts")
                .select {
                    filter { eq("coach_id", coachId) }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<Workout>()

            // Get exercise counts per workout via the view
            val viewRows = supabaseClient.postgrest
                .from("coach_workouts_v")
                .select {
                    filter { eq("coach_id", coachId) }
                }
                .decodeList<CoachWorkoutViewRow>()

            // Count exercises per workout
            val exerciseCounts = viewRows
                .filter { it.workoutExerciseId != null }
                .groupBy { it.workoutId }
                .mapValues { it.value.size }

            val summaries = workouts.map { workout ->
                WorkoutSummary(
                    id = workout.id,
                    name = workout.name,
                    description = workout.description,
                    exerciseCount = exerciseCounts[workout.id] ?: 0,
                    createdAt = workout.createdAt,
                    updatedAt = workout.updatedAt
                )
            }

            // Cache the result
            cache.put(CacheKeys.WORKOUTS, summaries)

            Log.d(TAG, "Loaded ${summaries.size} workouts")
            Result.success(summaries)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get workouts", e)
            Result.failure(e)
        }
    }

    /**
     * Invalidate workouts cache. Call after creating/updating/deleting workouts.
     */
    fun invalidateWorkoutsCache() {
        cache.invalidate(CacheKeys.WORKOUTS)
    }

    /**
     * Get workout details with all exercises.
     * Uses coach_workouts_v view for combined data.
     */
    suspend fun getWorkoutWithExercises(workoutId: String): Result<WorkoutWithExercises> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            val viewRows = supabaseClient.postgrest
                .from("coach_workouts_v")
                .select {
                    filter {
                        eq("workout_id", workoutId)
                        eq("coach_id", coachId)
                    }
                    order("order_index", Order.ASCENDING)
                }
                .decodeList<CoachWorkoutViewRow>()

            if (viewRows.isEmpty()) {
                return Result.failure(Exception("Workout not found"))
            }

            // First row contains workout header info
            val firstRow = viewRows.first()

            // Map exercises (filter out null exercise entries for empty workouts)
            val exercises = viewRows
                .filter { it.workoutExerciseId != null && it.exerciseId != null }
                .map { row ->
                    WorkoutExerciseDetail(
                        workoutExerciseId = row.workoutExerciseId!!,
                        exerciseId = row.exerciseId!!,
                        orderIndex = row.orderIndex ?: 0,
                        sets = row.sets ?: 3,
                        targetReps = row.targetReps,
                        notes = row.workoutExerciseNotes,
                        exerciseTitle = row.exerciseTitle ?: "Unknown",
                        exerciseCategory = row.exerciseCategory,
                        exerciseThumbnailUrl = row.exerciseThumbnailUrl,
                        exerciseVideoUrl = row.exerciseVideoUrl,
                        primaryMuscles = row.primaryMuscles,
                        secondaryMuscles = row.secondaryMuscles
                    )
                }

            val workout = WorkoutWithExercises(
                id = firstRow.workoutId,
                coachId = firstRow.coachId,
                name = firstRow.workoutName,
                description = firstRow.workoutDescription,
                createdAt = firstRow.workoutCreatedAt,
                updatedAt = firstRow.workoutUpdatedAt,
                exercises = exercises
            )

            Log.d(TAG, "Loaded workout ${workout.name} with ${exercises.size} exercises")
            Result.success(workout)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get workout details", e)
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
     * Uses exercise_view which joins exercises_new with technique_guides.
     */
    suspend fun getExercise(exerciseId: String): Result<Exercise> {
        return try {
            val exercise = supabaseClient.postgrest
                .from("exercise_view")
                .select {
                    filter { eq("id", exerciseId) }
                }
                .decodeSingle<Exercise>()

            Log.d(TAG, "Loaded exercise: ${exercise.name}, technique sections: ${exercise.techniqueSections?.size ?: 0}")
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
     * Create a new workout.
     */
    suspend fun createWorkout(name: String, description: String?): Result<Workout> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            val request = CreateWorkoutRequest(
                coachId = coachId,
                name = name,
                description = description
            )

            val workout = supabaseClient.postgrest
                .from("workouts")
                .insert(request) {
                    select()
                }
                .decodeSingle<Workout>()

            Log.d(TAG, "Created workout: ${workout.id}")
            Result.success(workout)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create workout", e)
            Result.failure(e)
        }
    }

    /**
     * Update workout name/description.
     */
    suspend fun updateWorkout(workoutId: String, name: String, description: String?): Result<Unit> {
        return try {
            supabaseClient.postgrest
                .from("workouts")
                .update(
                    mapOf(
                        "name" to name,
                        "description" to description,
                        "updated_at" to java.time.Instant.now().toString()
                    )
                ) {
                    filter { eq("id", workoutId) }
                }

            Log.d(TAG, "Updated workout: $workoutId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update workout", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a workout.
     * Note: This will cascade delete workout_exercises due to FK.
     */
    suspend fun deleteWorkout(workoutId: String): Result<Unit> {
        return try {
            supabaseClient.postgrest
                .from("workouts")
                .delete {
                    filter { eq("id", workoutId) }
                }

            Log.d(TAG, "Deleted workout: $workoutId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete workout", e)
            Result.failure(e)
        }
    }

    /**
     * Add an exercise to a workout.
     */
    suspend fun addExerciseToWorkout(
        workoutId: String,
        exerciseId: String,
        orderIndex: Int,
        sets: Int = 3,
        targetReps: Int? = null,
        notes: String? = null
    ): Result<WorkoutExercise> {
        return try {
            // Validate
            require(sets > 0) { "Sets must be greater than 0" }

            val request = AddWorkoutExerciseRequest(
                workoutId = workoutId,
                exerciseId = exerciseId,
                orderIndex = orderIndex,
                sets = sets,
                targetReps = targetReps,
                notes = notes
            )

            val workoutExercise = supabaseClient.postgrest
                .from("workout_exercises")
                .insert(request) {
                    select()
                }
                .decodeSingle<WorkoutExercise>()

            // Update workout's updated_at timestamp
            updateWorkoutTimestamp(workoutId)

            Log.d(TAG, "Added exercise to workout: ${workoutExercise.id}")
            Result.success(workoutExercise)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add exercise to workout", e)
            Result.failure(e)
        }
    }

    /**
     * Update exercise parameters in a workout.
     */
    suspend fun updateWorkoutExercise(
        workoutExerciseId: String,
        sets: Int? = null,
        targetReps: Int? = null,
        notes: String? = null
    ): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any?>()
            sets?.let {
                require(it > 0) { "Sets must be greater than 0" }
                updates["sets"] = it
            }
            targetReps?.let { updates["target_reps"] = it }
            notes?.let { updates["notes"] = it }

            if (updates.isEmpty()) {
                return Result.success(Unit)
            }

            supabaseClient.postgrest
                .from("workout_exercises")
                .update(updates) {
                    filter { eq("id", workoutExerciseId) }
                }

            Log.d(TAG, "Updated workout exercise: $workoutExerciseId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update workout exercise", e)
            Result.failure(e)
        }
    }

    /**
     * Remove an exercise from a workout.
     */
    suspend fun removeExerciseFromWorkout(workoutExerciseId: String, workoutId: String): Result<Unit> {
        return try {
            supabaseClient.postgrest
                .from("workout_exercises")
                .delete {
                    filter { eq("id", workoutExerciseId) }
                }

            // Reindex remaining exercises
            reindexWorkoutExercises(workoutId)

            // Update workout's updated_at timestamp
            updateWorkoutTimestamp(workoutId)

            Log.d(TAG, "Removed exercise from workout: $workoutExerciseId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove exercise from workout", e)
            Result.failure(e)
        }
    }

    /**
     * Reorder exercises in a workout.
     * Takes a list of workout_exercise_ids in the new order.
     */
    suspend fun reorderExercises(workoutId: String, orderedIds: List<String>): Result<Unit> {
        return try {
            orderedIds.forEachIndexed { index, workoutExerciseId ->
                supabaseClient.postgrest
                    .from("workout_exercises")
                    .update(mapOf("order_index" to index)) {
                        filter { eq("id", workoutExerciseId) }
                    }
            }

            updateWorkoutTimestamp(workoutId)

            Log.d(TAG, "Reordered ${orderedIds.size} exercises in workout: $workoutId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reorder exercises", e)
            Result.failure(e)
        }
    }

    // ==================== COUNT OPERATIONS ====================

    /**
     * Get the total count of workouts for the current coach.
     */
    suspend fun getWorkoutCount(): Result<Int> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            val workouts = supabaseClient.postgrest
                .from("workouts")
                .select {
                    filter { eq("coach_id", coachId) }
                }
                .decodeList<Workout>()

            Log.d(TAG, "Workout count for coach: ${workouts.size}")
            Result.success(workouts.size)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get workout count", e)
            Result.failure(e)
        }
    }

    // ==================== HELPER METHODS ====================

    /**
     * Reindex all exercises in a workout to be sequential (0-based).
     */
    private suspend fun reindexWorkoutExercises(workoutId: String) {
        try {
            val exercises = supabaseClient.postgrest
                .from("workout_exercises")
                .select {
                    filter { eq("workout_id", workoutId) }
                    order("order_index", Order.ASCENDING)
                }
                .decodeList<WorkoutExercise>()

            exercises.forEachIndexed { index, exercise ->
                if (exercise.orderIndex != index) {
                    supabaseClient.postgrest
                        .from("workout_exercises")
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
     * Update the workout's updated_at timestamp.
     */
    private suspend fun updateWorkoutTimestamp(workoutId: String) {
        try {
            supabaseClient.postgrest
                .from("workouts")
                .update(mapOf("updated_at" to java.time.Instant.now().toString())) {
                    filter { eq("id", workoutId) }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update workout timestamp", e)
        }
    }

    // ==================== ASSIGNMENT OPERATIONS ====================

    /**
     * Assign a workout to a client.
     */
    suspend fun assignWorkoutToClient(
        workoutId: String,
        clientId: String,
        scheduledDate: String? = null,
        notes: String? = null
    ): Result<WorkoutAssignment> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            val request = AssignWorkoutRequest(
                workoutId = workoutId,
                coachId = coachId,
                clientId = clientId,
                scheduledDate = scheduledDate,
                notes = notes
            )

            val assignment = supabaseClient.postgrest
                .from("workout_assignments")
                .insert(request) {
                    select()
                }
                .decodeSingle<WorkoutAssignment>()

            Log.d(TAG, "Assigned workout $workoutId to client $clientId")
            Result.success(assignment)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to assign workout to client", e)
            Result.failure(e)
        }
    }

    /**
     * Get all workouts assigned to a specific client.
     * Note: DB uses 'client_id' and 'workout_template_id' column names.
     */
    suspend fun getClientAssignments(clientId: String): Result<List<AssignedWorkout>> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            // Get assignments for the client (DB uses client_id, not user_id)
            val assignments = supabaseClient.postgrest
                .from("workout_assignments")
                .select {
                    filter {
                        eq("client_id", clientId)
                        eq("coach_id", coachId)
                    }
                    order("assigned_at", Order.DESCENDING)
                }
                .decodeList<WorkoutAssignment>()

            if (assignments.isEmpty()) {
                return Result.success(emptyList())
            }

            // Get workout details from workout_templates table (web app uses this)
            val workoutIds = assignments.map { it.workoutId }.distinct()
            val workoutTemplates = supabaseClient.postgrest
                .from("workout_templates")
                .select {
                    filter { isIn("id", workoutIds) }
                }
                .decodeList<WorkoutTemplate>()

            val workoutMap = workoutTemplates.associateBy { it.id }

            // Get template exercises with exercise details and sets (V1 compatible)
            val templateExercisesMap = mutableMapOf<String, List<AssignedExerciseInfo>>()
            Log.d(TAG, "Fetching exercises for ${workoutIds.size} templates: $workoutIds")

            for (templateId in workoutIds) {
                try {
                    Log.d(TAG, "Querying workout_template_exercises for workout_template_id=$templateId")
                    val exerciseRows = supabaseClient.postgrest
                        .from("workout_template_exercises")
                        .select {
                            filter { eq("workout_template_id", templateId) }
                            order("order_index", Order.ASCENDING)
                        }
                        .decodeList<WorkoutTemplateExerciseRow>()

                    Log.d(TAG, "Found ${exerciseRows.size} exercises for template $templateId")

                    if (exerciseRows.isNotEmpty()) {
                        // Get exercise details from exercises_new
                        val exerciseIds = exerciseRows.map { it.exerciseId }
                        val exerciseDetails = supabaseClient.postgrest
                            .from("exercises_new")
                            .select {
                                filter { isIn("id", exerciseIds) }
                            }
                            .decodeList<ExerciseDetailRow>()
                        val detailMap = exerciseDetails.associateBy { it.id }

                        // Get sets from exercise_sets for all workout_template_exercises
                        val workoutExerciseIds = exerciseRows.map { it.id }
                        val allSets = supabaseClient.postgrest
                            .from("exercise_sets")
                            .select {
                                filter { isIn("workout_exercise_id", workoutExerciseIds) }
                                order("set_number", Order.ASCENDING)
                            }
                            .decodeList<ExerciseSetRow>()

                        Log.d(TAG, "Found ${allSets.size} sets for ${workoutExerciseIds.size} exercises")
                        val setsMap = allSets.groupBy { it.workoutExerciseId }

                        templateExercisesMap[templateId] = exerciseRows.map { row ->
                            val detail = detailMap[row.exerciseId]
                            val sets = setsMap[row.id] ?: emptyList()
                            AssignedExerciseInfo(
                                workoutExerciseId = row.id,
                                exerciseId = row.exerciseId,
                                name = detail?.name ?: "Unknown",
                                muscleGroup = detail?.mainMuscle,
                                equipment = detail?.equipmentCategory,
                                orderIndex = row.orderIndex,
                                sets = sets.map { set ->
                                    ExerciseSetInfo(
                                        id = set.id,
                                        setNumber = set.setNumber,
                                        targetReps = set.targetReps,
                                        targetWeight = set.targetWeight,
                                        restSeconds = set.restSeconds
                                    )
                                }
                            )
                        }
                    } else {
                        Log.w(TAG, "No exercises found in workout_template_exercises for template $templateId")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to fetch exercises for template $templateId: ${e.message}", e)
                }
            }

            Log.d(TAG, "Template exercises map has ${templateExercisesMap.size} entries")

            val assignedWorkouts = assignments.mapNotNull { assignment ->
                val template = workoutMap[assignment.workoutId] ?: return@mapNotNull null
                val exercises = templateExercisesMap[template.id] ?: emptyList()
                AssignedWorkout(
                    assignmentId = assignment.id,
                    workoutId = template.id,
                    workoutName = template.name,
                    workoutDescription = template.description,
                    exerciseCount = exercises.size,
                    assignedAt = assignment.assignedAt,
                    scheduledDate = assignment.scheduledDate,
                    notes = assignment.notes,
                    status = assignment.status,
                    exercises = exercises
                )
            }

            Log.d(TAG, "Loaded ${assignedWorkouts.size} assignments for client $clientId")
            Result.success(assignedWorkouts)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get client assignments", e)
            Result.failure(e)
        }
    }

    // DTO for workout_template_exercises table
    @Serializable
    private data class WorkoutTemplateExerciseRow(
        val id: String,
        @SerialName("workout_template_id")
        val workoutTemplateId: String,
        @SerialName("exercise_id")
        val exerciseId: String,
        @SerialName("order_index")
        val orderIndex: Int = 0,
        val notes: String? = null
    )

    // DTO for exercise details from exercises_new
    @Serializable
    private data class ExerciseDetailRow(
        val id: String,
        val name: String,
        @SerialName("main_muscle")
        val mainMuscle: String? = null,
        @SerialName("equipment_category")
        val equipmentCategory: String? = null
    )

    // DTO for exercise_sets table
    @Serializable
    private data class ExerciseSetRow(
        val id: String,
        @SerialName("workout_exercise_id")
        val workoutExerciseId: String,
        @SerialName("set_number")
        val setNumber: Int,
        @SerialName("target_reps")
        val targetReps: Int,
        @SerialName("target_weight")
        val targetWeight: Double,
        @SerialName("rest_seconds")
        val restSeconds: Int
    )

    /**
     * Remove a workout assignment.
     */
    suspend fun removeAssignment(assignmentId: String): Result<Unit> {
        return try {
            supabaseClient.postgrest
                .from("workout_assignments")
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
                .from("workout_assignments")
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

    /**
     * Update an assignment (notes, scheduled date, status).
     */
    suspend fun updateAssignment(
        assignmentId: String,
        notes: String?,
        scheduledDate: String?,
        status: String?
    ): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any?>()
            notes?.let { updates["notes"] = it }
            scheduledDate?.let { updates["scheduled_date"] = it }
            status?.let { updates["status"] = it }

            if (updates.isEmpty()) {
                return Result.success(Unit)
            }

            supabaseClient.postgrest
                .from("workout_assignments")
                .update(updates) {
                    filter { eq("id", assignmentId) }
                }

            Log.d(TAG, "Updated assignment: $assignmentId with ${updates.keys}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update assignment", e)
            Result.failure(e)
        }
    }

    // ==================== EXERCISE SET OPERATIONS ====================

    /**
     * Update exercise sets for a workout exercise.
     * Handles insert, update, and delete operations.
     *
     * @param workoutExerciseId The workout_template_exercise ID
     * @param sets List of sets to save (new sets have IDs starting with "new_")
     */
    suspend fun updateExerciseSets(
        workoutExerciseId: String,
        sets: List<ExerciseSetUpdate>
    ): Result<Unit> {
        return try {
            Log.d(TAG, "Updating ${sets.size} sets for workout exercise: $workoutExerciseId")

            // Get existing sets
            val existingSets = supabaseClient.postgrest
                .from("exercise_sets")
                .select {
                    filter { eq("workout_exercise_id", workoutExerciseId) }
                }
                .decodeList<ExerciseSetRow>()

            val existingIds = existingSets.map { it.id }.toSet()
            val newSetIds = sets.filter { it.id.startsWith("new_") }.map { it.id }.toSet()
            val updatedIds = sets.filter { !it.id.startsWith("new_") }.map { it.id }.toSet()

            // Delete sets that were removed
            val toDelete = existingIds - updatedIds
            if (toDelete.isNotEmpty()) {
                Log.d(TAG, "Deleting ${toDelete.size} sets")
                supabaseClient.postgrest
                    .from("exercise_sets")
                    .delete {
                        filter { isIn("id", toDelete.toList()) }
                    }
            }

            // Update existing sets
            sets.filter { !it.id.startsWith("new_") }.forEach { set ->
                supabaseClient.postgrest
                    .from("exercise_sets")
                    .update(
                        mapOf(
                            "set_number" to set.setNumber,
                            "target_reps" to set.targetReps,
                            "target_weight" to set.targetWeight,
                            "rest_seconds" to set.restSeconds
                        )
                    ) {
                        filter { eq("id", set.id) }
                    }
            }

            // Insert new sets
            val newSets = sets.filter { it.id.startsWith("new_") }
            if (newSets.isNotEmpty()) {
                Log.d(TAG, "Inserting ${newSets.size} new sets")
                newSets.forEach { set ->
                    supabaseClient.postgrest
                        .from("exercise_sets")
                        .insert(
                            mapOf(
                                "workout_exercise_id" to workoutExerciseId,
                                "set_number" to set.setNumber,
                                "target_reps" to set.targetReps,
                                "target_weight" to set.targetWeight,
                                "rest_seconds" to set.restSeconds
                            )
                        )
                }
            }

            Log.d(TAG, "Successfully updated sets for workout exercise: $workoutExerciseId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update exercise sets", e)
            Result.failure(e)
        }
    }

    /**
     * Data class for set updates.
     */
    data class ExerciseSetUpdate(
        val id: String,
        val setNumber: Int,
        val targetReps: Int,
        val targetWeight: Double,
        val restSeconds: Int
    )
}
