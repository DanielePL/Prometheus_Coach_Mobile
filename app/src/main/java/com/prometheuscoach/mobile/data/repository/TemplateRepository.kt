package com.prometheuscoach.mobile.data.repository

import android.util.Log
import com.prometheuscoach.mobile.data.model.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for workout template operations.
 * Supports system templates, coach templates, and AI-generated templates.
 */
@Singleton
class TemplateRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository
) {
    companion object {
        private const val TAG = "TemplateRepository"
    }

    // ==================== CATEGORY OPERATIONS ====================

    /**
     * Get all template categories.
     */
    suspend fun getCategories(): Result<List<TemplateCategory>> {
        return try {
            val categories = supabaseClient.postgrest
                .from("template_categories")
                .select {
                    order("display_order", Order.ASCENDING)
                }
                .decodeList<TemplateCategory>()

            Log.d(TAG, "Loaded ${categories.size} categories")
            Result.success(categories)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get categories", e)
            Result.failure(e)
        }
    }

    // ==================== TEMPLATE READ OPERATIONS ====================

    /**
     * Get all templates (system + coach's own).
     */
    suspend fun getTemplates(
        categoryId: String? = null,
        templateType: TemplateType? = null,
        level: FitnessLevel? = null
    ): Result<List<TemplateSummary>> {
        return try {
            val coachId = authRepository.getCurrentUserId()

            // Get templates (system + own)
            val templates = supabaseClient.postgrest
                .from("workout_templates")
                .select {
                    filter {
                        or {
                            eq("template_type", "system")
                            if (coachId != null) {
                                eq("coach_id", coachId)
                            }
                        }
                        categoryId?.let { eq("category_id", it) }
                        templateType?.let { eq("template_type", it.value) }
                        level?.let { eq("default_level", it.value) }
                    }
                    order("use_count", Order.DESCENDING)
                }
                .decodeList<WorkoutTemplate>()

            // Get categories for names
            val categories = getCategories().getOrElse { emptyList() }
            val categoryMap = categories.associateBy { it.id }

            // Get exercise counts
            val templateIds = templates.map { it.id }
            val exerciseCounts = getExerciseCountsForTemplates(templateIds)

            // Get favorites for current coach
            val favoriteIds = if (coachId != null) {
                getFavoriteTemplateIds(coachId)
            } else emptySet()

            val summaries = templates.map { template ->
                TemplateSummary(
                    id = template.id,
                    name = template.name,
                    description = template.description,
                    templateType = template.type,
                    level = template.level,
                    categoryName = template.categoryId?.let { categoryMap[it]?.name },
                    categoryIcon = template.categoryId?.let { categoryMap[it]?.icon },
                    exerciseCount = exerciseCounts[template.id] ?: 0,
                    targetMuscles = template.targetMuscles ?: emptyList(),
                    isFavorite = template.id in favoriteIds,
                    useCount = template.useCount
                )
            }

            Log.d(TAG, "Loaded ${summaries.size} templates")
            Result.success(summaries)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get templates", e)
            Result.failure(e)
        }
    }

    /**
     * Get system templates grouped by category.
     */
    suspend fun getSystemTemplates(): Result<Map<TemplateCategory, List<TemplateSummary>>> {
        return try {
            val categories = getCategories().getOrElse { emptyList() }
            val templates = getTemplates(templateType = TemplateType.SYSTEM).getOrElse { emptyList() }

            val categoryMap = categories.associateBy { it.id }
            val grouped = templates.groupBy { summary ->
                summary.categoryName?.let { name ->
                    categories.find { it.name == name }
                } ?: TemplateCategory(id = "uncategorized", name = "Sonstige")
            }

            Log.d(TAG, "Loaded system templates in ${grouped.size} categories")
            Result.success(grouped)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get system templates", e)
            Result.failure(e)
        }
    }

    /**
     * Get coach's own templates.
     */
    suspend fun getCoachTemplates(): Result<List<TemplateSummary>> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            val templates = supabaseClient.postgrest
                .from("workout_templates")
                .select {
                    filter {
                        eq("coach_id", coachId)
                        eq("template_type", "coach")
                    }
                    order("updated_at", Order.DESCENDING)
                }
                .decodeList<WorkoutTemplate>()

            val categories = getCategories().getOrElse { emptyList() }
            val categoryMap = categories.associateBy { it.id }
            val templateIds = templates.map { it.id }
            val exerciseCounts = getExerciseCountsForTemplates(templateIds)
            val favoriteIds = getFavoriteTemplateIds(coachId)

            val summaries = templates.map { template ->
                TemplateSummary(
                    id = template.id,
                    name = template.name,
                    description = template.description,
                    templateType = template.type,
                    level = template.level,
                    categoryName = template.categoryId?.let { categoryMap[it]?.name },
                    categoryIcon = template.categoryId?.let { categoryMap[it]?.icon },
                    exerciseCount = exerciseCounts[template.id] ?: 0,
                    targetMuscles = template.targetMuscles ?: emptyList(),
                    isFavorite = template.id in favoriteIds,
                    useCount = template.useCount
                )
            }

            Log.d(TAG, "Loaded ${summaries.size} coach templates")
            Result.success(summaries)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get coach templates", e)
            Result.failure(e)
        }
    }

    /**
     * Get favorite templates for current coach.
     */
    suspend fun getFavoriteTemplates(): Result<List<TemplateSummary>> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            val favoriteIds = getFavoriteTemplateIds(coachId)
            if (favoriteIds.isEmpty()) {
                return Result.success(emptyList())
            }

            val templates = getTemplates().getOrElse { emptyList() }
            val favorites = templates.filter { it.id in favoriteIds }

            Log.d(TAG, "Loaded ${favorites.size} favorite templates")
            Result.success(favorites)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get favorite templates", e)
            Result.failure(e)
        }
    }

    /**
     * Get template with all exercises.
     */
    suspend fun getTemplateWithExercises(templateId: String): Result<TemplateWithExercises> {
        return try {
            val coachId = authRepository.getCurrentUserId()

            // Get template
            val template = supabaseClient.postgrest
                .from("workout_templates")
                .select {
                    filter { eq("id", templateId) }
                }
                .decodeSingle<WorkoutTemplate>()

            // Check access (system templates are public, coach templates need ownership)
            if (template.templateType != "system" && template.coachId != coachId) {
                return Result.failure(Exception("Access denied"))
            }

            // Get category
            val category = template.categoryId?.let { catId ->
                supabaseClient.postgrest
                    .from("template_categories")
                    .select {
                        filter { eq("id", catId) }
                    }
                    .decodeSingleOrNull<TemplateCategory>()
            }

            // Get exercises with exercise details
            val templateExercises = supabaseClient.postgrest
                .from("template_exercises")
                .select {
                    filter { eq("template_id", templateId) }
                    order("order_index", Order.ASCENDING)
                }
                .decodeList<TemplateExercise>()

            // Get exercise details
            val exerciseIds = templateExercises.map { it.exerciseId }
            val exercises = if (exerciseIds.isNotEmpty()) {
                supabaseClient.postgrest
                    .from("exercises_new")
                    .select {
                        filter { isIn("id", exerciseIds) }
                    }
                    .decodeList<ExerciseListItem>()
            } else emptyList()

            val exerciseMap = exercises.associateBy { it.id }

            val exerciseDetails = templateExercises.mapNotNull { te ->
                val exercise = exerciseMap[te.exerciseId] ?: return@mapNotNull null
                TemplateExerciseDetail(
                    templateExerciseId = te.id,
                    exerciseId = te.exerciseId,
                    orderIndex = te.orderIndex,
                    defaultSets = te.defaultSets,
                    defaultRepsMin = te.defaultRepsMin,
                    defaultRepsMax = te.defaultRepsMax,
                    defaultRestSeconds = te.defaultRestSeconds,
                    levelOverrides = te.levelOverrides,
                    notes = te.notes,
                    exerciseName = exercise.name,
                    exerciseCategory = exercise.category,
                    exerciseVideoUrl = null,
                    primaryMuscle = exercise.mainMuscleGroup,
                    secondaryMuscles = exercise.secondaryMuscleGroups?.joinToString(", ")
                )
            }

            // Check if favorite
            val isFavorite = coachId?.let { getFavoriteTemplateIds(it).contains(templateId) } ?: false

            val result = TemplateWithExercises(
                template = template,
                category = category,
                exercises = exerciseDetails,
                isFavorite = isFavorite
            )

            Log.d(TAG, "Loaded template ${template.name} with ${exerciseDetails.size} exercises")
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get template details", e)
            Result.failure(e)
        }
    }

    // ==================== TEMPLATE WRITE OPERATIONS ====================

    /**
     * Create a new template.
     */
    suspend fun createTemplate(
        name: String,
        description: String? = null,
        categoryId: String? = null,
        defaultLevel: FitnessLevel = FitnessLevel.INTERMEDIATE,
        targetMuscles: List<String>? = null,
        equipment: List<String>? = null
    ): Result<WorkoutTemplate> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            val request = CreateTemplateRequest(
                coachId = coachId,
                name = name,
                description = description,
                categoryId = categoryId,
                templateType = "coach",
                defaultLevel = defaultLevel.value,
                targetMuscles = targetMuscles,
                equipment = equipment
            )

            val template = supabaseClient.postgrest
                .from("workout_templates")
                .insert(request) {
                    select()
                }
                .decodeSingle<WorkoutTemplate>()

            Log.d(TAG, "Created template: ${template.id}")
            Result.success(template)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create template", e)
            Result.failure(e)
        }
    }

    /**
     * Update a template.
     */
    suspend fun updateTemplate(
        templateId: String,
        name: String? = null,
        description: String? = null,
        categoryId: String? = null,
        defaultLevel: FitnessLevel? = null
    ): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any?>()
            name?.let { updates["name"] = it }
            description?.let { updates["description"] = it }
            categoryId?.let { updates["category_id"] = it }
            defaultLevel?.let { updates["default_level"] = it.value }
            updates["updated_at"] = java.time.Instant.now().toString()

            supabaseClient.postgrest
                .from("workout_templates")
                .update(updates) {
                    filter { eq("id", templateId) }
                }

            Log.d(TAG, "Updated template: $templateId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update template", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a template.
     */
    suspend fun deleteTemplate(templateId: String): Result<Unit> {
        return try {
            supabaseClient.postgrest
                .from("workout_templates")
                .delete {
                    filter { eq("id", templateId) }
                }

            Log.d(TAG, "Deleted template: $templateId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete template", e)
            Result.failure(e)
        }
    }

    /**
     * Add an exercise to a template.
     */
    suspend fun addExerciseToTemplate(
        templateId: String,
        exerciseId: String,
        orderIndex: Int,
        defaultSets: Int = 3,
        defaultRepsMin: Int? = null,
        defaultRepsMax: Int? = null,
        defaultRestSeconds: Int = 90,
        notes: String? = null
    ): Result<TemplateExercise> {
        return try {
            val request = AddTemplateExerciseRequest(
                templateId = templateId,
                exerciseId = exerciseId,
                orderIndex = orderIndex,
                defaultSets = defaultSets,
                defaultRepsMin = defaultRepsMin,
                defaultRepsMax = defaultRepsMax,
                defaultRestSeconds = defaultRestSeconds,
                notes = notes
            )

            val exercise = supabaseClient.postgrest
                .from("template_exercises")
                .insert(request) {
                    select()
                }
                .decodeSingle<TemplateExercise>()

            updateTemplateTimestamp(templateId)

            Log.d(TAG, "Added exercise to template: ${exercise.id}")
            Result.success(exercise)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add exercise to template", e)
            Result.failure(e)
        }
    }

    /**
     * Remove an exercise from a template.
     */
    suspend fun removeExerciseFromTemplate(templateExerciseId: String, templateId: String): Result<Unit> {
        return try {
            supabaseClient.postgrest
                .from("template_exercises")
                .delete {
                    filter { eq("id", templateExerciseId) }
                }

            reindexTemplateExercises(templateId)
            updateTemplateTimestamp(templateId)

            Log.d(TAG, "Removed exercise from template: $templateExerciseId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove exercise from template", e)
            Result.failure(e)
        }
    }

    // ==================== FAVORITE OPERATIONS ====================

    /**
     * Toggle favorite status for a template.
     */
    suspend fun toggleFavorite(templateId: String): Result<Boolean> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            val existingFavorite = supabaseClient.postgrest
                .from("coach_template_favorites")
                .select {
                    filter {
                        eq("coach_id", coachId)
                        eq("template_id", templateId)
                    }
                }
                .decodeList<CoachTemplateFavorite>()
                .firstOrNull()

            if (existingFavorite != null) {
                // Remove favorite
                supabaseClient.postgrest
                    .from("coach_template_favorites")
                    .delete {
                        filter { eq("id", existingFavorite.id) }
                    }
                Log.d(TAG, "Removed favorite: $templateId")
                Result.success(false)
            } else {
                // Add favorite
                supabaseClient.postgrest
                    .from("coach_template_favorites")
                    .insert(
                        mapOf(
                            "coach_id" to coachId,
                            "template_id" to templateId
                        )
                    )
                Log.d(TAG, "Added favorite: $templateId")
                Result.success(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle favorite", e)
            Result.failure(e)
        }
    }

    // ==================== CLONE OPERATIONS ====================

    /**
     * Clone a template to a new workout.
     * Applies level-based scaling and optional percentage scaling.
     */
    suspend fun cloneTemplateToWorkout(config: TemplateCloneConfig): Result<String> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            // Get template with exercises
            val templateData = getTemplateWithExercises(config.templateId).getOrThrow()
            val template = templateData.template

            // Create workout
            val workoutName = config.customName ?: "${template.name} (${config.targetLevel.displayName})"

            @Serializable
            data class WorkoutInsert(
                @SerialName("coach_id") val coachId: String,
                val name: String,
                val description: String?
            )

            val workout = supabaseClient.postgrest
                .from("workouts")
                .insert(
                    WorkoutInsert(
                        coachId = coachId,
                        name = workoutName,
                        description = template.description
                    )
                ) {
                    select()
                }
                .decodeSingle<Workout>()

            // Add exercises with scaling
            val scalingFactor = config.scalingPercentage / 100.0

            templateData.exercises.forEachIndexed { index, exerciseDetail ->
                val sets = (exerciseDetail.getSetsForLevel(config.targetLevel) * scalingFactor).toInt().coerceAtLeast(1)
                val (repsMin, repsMax) = exerciseDetail.getRepsForLevel(config.targetLevel)
                val targetReps = repsMax ?: repsMin

                @Serializable
                data class WorkoutExerciseInsert(
                    @SerialName("workout_id") val workoutId: String,
                    @SerialName("exercise_id") val exerciseId: String,
                    @SerialName("order_index") val orderIndex: Int,
                    val sets: Int,
                    @SerialName("target_reps") val targetReps: Int?,
                    val notes: String?
                )

                supabaseClient.postgrest
                    .from("workout_exercises")
                    .insert(
                        WorkoutExerciseInsert(
                            workoutId = workout.id,
                            exerciseId = exerciseDetail.exerciseId,
                            orderIndex = index,
                            sets = sets,
                            targetReps = targetReps,
                            notes = exerciseDetail.notes
                        )
                    )
            }

            // Increment use count
            incrementTemplateUseCount(config.templateId)

            Log.d(TAG, "Cloned template ${template.name} to workout ${workout.id}")
            Result.success(workout.id)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clone template to workout", e)
            Result.failure(e)
        }
    }

    /**
     * Save an existing workout as a new template.
     */
    suspend fun saveWorkoutAsTemplate(
        workoutId: String,
        name: String,
        categoryId: String? = null
    ): Result<WorkoutTemplate> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            // Get workout with exercises using the view
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

            val firstRow = viewRows.first()

            // Create template
            val template = createTemplate(
                name = name,
                description = firstRow.workoutDescription,
                categoryId = categoryId
            ).getOrThrow()

            // Add exercises
            viewRows
                .filter { it.workoutExerciseId != null && it.exerciseId != null }
                .forEachIndexed { index, row ->
                    addExerciseToTemplate(
                        templateId = template.id,
                        exerciseId = row.exerciseId!!,
                        orderIndex = index,
                        defaultSets = row.sets ?: 3,
                        defaultRepsMin = row.targetReps,
                        defaultRepsMax = row.targetReps,
                        notes = row.workoutExerciseNotes
                    )
                }

            Log.d(TAG, "Saved workout $workoutId as template ${template.id}")
            Result.success(template)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save workout as template", e)
            Result.failure(e)
        }
    }

    // ==================== HELPER METHODS ====================

    private suspend fun getExerciseCountsForTemplates(templateIds: List<String>): Map<String, Int> {
        if (templateIds.isEmpty()) return emptyMap()

        return try {
            val exercises = supabaseClient.postgrest
                .from("template_exercises")
                .select {
                    filter { isIn("template_id", templateIds) }
                }
                .decodeList<TemplateExercise>()

            exercises.groupBy { it.templateId }.mapValues { it.value.size }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get exercise counts", e)
            emptyMap()
        }
    }

    private suspend fun getFavoriteTemplateIds(coachId: String): Set<String> {
        return try {
            val favorites = supabaseClient.postgrest
                .from("coach_template_favorites")
                .select {
                    filter { eq("coach_id", coachId) }
                }
                .decodeList<CoachTemplateFavorite>()

            favorites.map { it.templateId }.toSet()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get favorites", e)
            emptySet()
        }
    }

    private suspend fun reindexTemplateExercises(templateId: String) {
        try {
            val exercises = supabaseClient.postgrest
                .from("template_exercises")
                .select {
                    filter { eq("template_id", templateId) }
                    order("order_index", Order.ASCENDING)
                }
                .decodeList<TemplateExercise>()

            exercises.forEachIndexed { index, exercise ->
                if (exercise.orderIndex != index) {
                    supabaseClient.postgrest
                        .from("template_exercises")
                        .update(mapOf("order_index" to index)) {
                            filter { eq("id", exercise.id) }
                        }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to reindex exercises", e)
        }
    }

    private suspend fun updateTemplateTimestamp(templateId: String) {
        try {
            supabaseClient.postgrest
                .from("workout_templates")
                .update(mapOf("updated_at" to java.time.Instant.now().toString())) {
                    filter { eq("id", templateId) }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update template timestamp", e)
        }
    }

    private suspend fun incrementTemplateUseCount(templateId: String) {
        try {
            // Get current count
            val template = supabaseClient.postgrest
                .from("workout_templates")
                .select {
                    filter { eq("id", templateId) }
                }
                .decodeSingle<WorkoutTemplate>()

            supabaseClient.postgrest
                .from("workout_templates")
                .update(mapOf("use_count" to (template.useCount + 1))) {
                    filter { eq("id", templateId) }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to increment use count", e)
        }
    }
}
