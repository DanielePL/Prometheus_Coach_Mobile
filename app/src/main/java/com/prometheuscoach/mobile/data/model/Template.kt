package com.prometheuscoach.mobile.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ==================== TEMPLATE ENUMS ====================

enum class TemplateType(val value: String) {
    SYSTEM("system"),
    COACH("coach"),
    AI("ai");

    companion object {
        fun fromValue(value: String?): TemplateType =
            entries.find { it.value == value } ?: COACH
    }
}

enum class FitnessLevel(val value: String, val displayName: String) {
    BEGINNER("beginner", "Anfänger"),
    INTERMEDIATE("intermediate", "Fortgeschritten"),
    ADVANCED("advanced", "Profi");

    companion object {
        fun fromValue(value: String?): FitnessLevel =
            entries.find { it.value == value } ?: INTERMEDIATE
    }
}

// ==================== DATABASE MODELS ====================

/**
 * Template category from public.template_categories table.
 */
@Serializable
data class TemplateCategory(
    val id: String,
    val name: String,
    val icon: String? = null,
    @SerialName("display_order")
    val displayOrder: Int = 0
)

/**
 * Workout template from public.workout_templates table.
 */
@Serializable
data class WorkoutTemplate(
    val id: String,
    @SerialName("coach_id")
    val coachId: String? = null,
    val name: String,
    val description: String? = null,
    @SerialName("category_id")
    val categoryId: String? = null,
    @SerialName("template_type")
    val templateType: String = "coach",
    @SerialName("default_level")
    val defaultLevel: String = "intermediate",
    @SerialName("scaling_config")
    val scalingConfig: ScalingConfig? = null,
    @SerialName("target_muscles")
    val targetMuscles: List<String>? = null,
    val equipment: List<String>? = null,
    @SerialName("use_count")
    val useCount: Int = 0,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
) {
    val type: TemplateType get() = TemplateType.fromValue(templateType)
    val level: FitnessLevel get() = FitnessLevel.fromValue(defaultLevel)
}

/**
 * Scaling configuration for different fitness levels.
 */
@Serializable
data class ScalingConfig(
    val beginner: LevelScaling? = null,
    val intermediate: LevelScaling? = null,
    val advanced: LevelScaling? = null
)

@Serializable
data class LevelScaling(
    @SerialName("sets_multiplier")
    val setsMultiplier: Float = 1.0f,
    @SerialName("reps_multiplier")
    val repsMultiplier: Float = 1.0f
)

/**
 * Template exercise from public.template_exercises table.
 */
@Serializable
data class TemplateExercise(
    val id: String,
    @SerialName("template_id")
    val templateId: String,
    @SerialName("exercise_id")
    val exerciseId: String,
    @SerialName("order_index")
    val orderIndex: Int = 0,
    @SerialName("default_sets")
    val defaultSets: Int = 3,
    @SerialName("default_reps_min")
    val defaultRepsMin: Int? = null,
    @SerialName("default_reps_max")
    val defaultRepsMax: Int? = null,
    @SerialName("default_rest_seconds")
    val defaultRestSeconds: Int = 90,
    @SerialName("level_overrides")
    val levelOverrides: LevelOverrides? = null,
    val notes: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)

/**
 * Level-specific overrides for exercise parameters.
 */
@Serializable
data class LevelOverrides(
    val beginner: ExerciseOverride? = null,
    val intermediate: ExerciseOverride? = null,
    val advanced: ExerciseOverride? = null
)

@Serializable
data class ExerciseOverride(
    val sets: Int? = null,
    @SerialName("reps_min")
    val repsMin: Int? = null,
    @SerialName("reps_max")
    val repsMax: Int? = null,
    @SerialName("rest_seconds")
    val restSeconds: Int? = null
)

/**
 * Coach template favorite from public.coach_template_favorites table.
 */
@Serializable
data class CoachTemplateFavorite(
    val id: String,
    @SerialName("coach_id")
    val coachId: String,
    @SerialName("template_id")
    val templateId: String,
    @SerialName("created_at")
    val createdAt: String? = null
)

// ==================== UI MODELS ====================

/**
 * Summary model for template list.
 */
data class TemplateSummary(
    val id: String,
    val name: String,
    val description: String?,
    val templateType: TemplateType,
    val level: FitnessLevel,
    val categoryName: String?,
    val categoryIcon: String?,
    val exerciseCount: Int,
    val targetMuscles: List<String>,
    val isFavorite: Boolean = false,
    val useCount: Int = 0
) {
    val isSystem: Boolean get() = templateType == TemplateType.SYSTEM
    val isCoach: Boolean get() = templateType == TemplateType.COACH
    val isAI: Boolean get() = templateType == TemplateType.AI
}

/**
 * Full template with exercises for detail view.
 */
data class TemplateWithExercises(
    val template: WorkoutTemplate,
    val category: TemplateCategory?,
    val exercises: List<TemplateExerciseDetail>,
    val isFavorite: Boolean = false
) {
    val exerciseCount: Int get() = exercises.size
}

/**
 * Exercise detail within a template.
 */
data class TemplateExerciseDetail(
    val templateExerciseId: String,
    val exerciseId: String,
    val orderIndex: Int,
    val defaultSets: Int,
    val defaultRepsMin: Int?,
    val defaultRepsMax: Int?,
    val defaultRestSeconds: Int,
    val levelOverrides: LevelOverrides?,
    val notes: String?,
    // Exercise info from exercises_new
    val exerciseName: String,
    val exerciseCategory: String?,
    val exerciseVideoUrl: String?,
    val primaryMuscle: String?,
    val secondaryMuscles: String?
) {
    /**
     * Get sets for a specific level (with overrides).
     */
    fun getSetsForLevel(level: FitnessLevel): Int {
        return when (level) {
            FitnessLevel.BEGINNER -> levelOverrides?.beginner?.sets ?: defaultSets
            FitnessLevel.INTERMEDIATE -> levelOverrides?.intermediate?.sets ?: defaultSets
            FitnessLevel.ADVANCED -> levelOverrides?.advanced?.sets ?: defaultSets
        }
    }

    /**
     * Get reps range for a specific level.
     */
    fun getRepsForLevel(level: FitnessLevel): Pair<Int?, Int?> {
        val override = when (level) {
            FitnessLevel.BEGINNER -> levelOverrides?.beginner
            FitnessLevel.INTERMEDIATE -> levelOverrides?.intermediate
            FitnessLevel.ADVANCED -> levelOverrides?.advanced
        }
        return Pair(
            override?.repsMin ?: defaultRepsMin,
            override?.repsMax ?: defaultRepsMax
        )
    }

    /**
     * Format reps display (e.g., "8-12" or "10").
     */
    fun getRepsDisplay(level: FitnessLevel): String {
        val (min, max) = getRepsForLevel(level)
        return when {
            min != null && max != null && min != max -> "$min-$max"
            min != null -> min.toString()
            max != null -> max.toString()
            else -> "-"
        }
    }
}

// ==================== REQUEST MODELS ====================

/**
 * Request model for creating a template.
 */
@Serializable
data class CreateTemplateRequest(
    @SerialName("coach_id")
    val coachId: String,
    val name: String,
    val description: String? = null,
    @SerialName("category_id")
    val categoryId: String? = null,
    @SerialName("template_type")
    val templateType: String = "coach",
    @SerialName("default_level")
    val defaultLevel: String = "intermediate",
    @SerialName("target_muscles")
    val targetMuscles: List<String>? = null,
    val equipment: List<String>? = null
)

/**
 * Request model for adding an exercise to a template.
 */
@Serializable
data class AddTemplateExerciseRequest(
    @SerialName("template_id")
    val templateId: String,
    @SerialName("exercise_id")
    val exerciseId: String,
    @SerialName("order_index")
    val orderIndex: Int,
    @SerialName("default_sets")
    val defaultSets: Int = 3,
    @SerialName("default_reps_min")
    val defaultRepsMin: Int? = null,
    @SerialName("default_reps_max")
    val defaultRepsMax: Int? = null,
    @SerialName("default_rest_seconds")
    val defaultRestSeconds: Int = 90,
    val notes: String? = null
)

/**
 * Configuration for cloning a template to a workout.
 */
data class TemplateCloneConfig(
    val templateId: String,
    val targetLevel: FitnessLevel = FitnessLevel.INTERMEDIATE,
    val scalingPercentage: Int = 100,
    val clientId: String? = null,
    val customName: String? = null
)
