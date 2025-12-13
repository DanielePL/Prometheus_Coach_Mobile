package com.prometheuscoach.mobile.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Nutrition goal types
 */
enum class NutritionGoalType {
    @SerialName("cutting") CUTTING,
    @SerialName("bulking") BULKING,
    @SerialName("maintenance") MAINTENANCE,
    @SerialName("performance") PERFORMANCE
}

/**
 * Meal types
 */
enum class MealType {
    @SerialName("breakfast") BREAKFAST,
    @SerialName("lunch") LUNCH,
    @SerialName("dinner") DINNER,
    @SerialName("snack") SNACK,
    @SerialName("shake") SHAKE
}

/**
 * Individual food item in a meal
 */
@Serializable
data class MealItem(
    val id: String,
    @SerialName("meal_id") val mealId: String,
    @SerialName("food_id") val foodId: String? = null,
    @SerialName("item_name") val itemName: String,
    val quantity: Float,
    @SerialName("quantity_unit") val quantityUnit: String = "g",
    val calories: Float,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val fiber: Float? = null,
    val sugar: Float? = null,
    @SerialName("saturated_fat") val saturatedFat: Float? = null,
    val sodium: Float? = null,
    val potassium: Float? = null,
    val calcium: Float? = null,
    val iron: Float? = null,
    @SerialName("vitamin_a") val vitaminA: Float? = null,
    @SerialName("vitamin_c") val vitaminC: Float? = null,
    @SerialName("vitamin_d") val vitaminD: Float? = null,
    @SerialName("created_at") val createdAt: String? = null
)

/**
 * A meal (breakfast, lunch, dinner, snack, shake)
 */
@Serializable
data class Meal(
    val id: String,
    @SerialName("nutrition_log_id") val nutritionLogId: String,
    @SerialName("meal_type") val mealType: String,
    @SerialName("meal_name") val mealName: String? = null,
    val time: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("ai_analysis_id") val aiAnalysisId: String? = null,
    @SerialName("ai_confidence") val aiConfidence: Float? = null,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("meal_items") val mealItems: List<MealItem>? = null
)

/**
 * Daily nutrition log
 */
@Serializable
data class NutritionLog(
    val id: String,
    @SerialName("user_id") val userId: String,
    val date: String,
    @SerialName("target_calories") val targetCalories: Float? = null,
    @SerialName("target_protein") val targetProtein: Float? = null,
    @SerialName("target_carbs") val targetCarbs: Float? = null,
    @SerialName("target_fat") val targetFat: Float? = null,
    val notes: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val meals: List<Meal>? = null
)

/**
 * Nutrition goals set by coach
 */
@Serializable
data class NutritionGoal(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("goal_type") val goalType: String,
    @SerialName("target_calories") val targetCalories: Float,
    @SerialName("target_protein") val targetProtein: Float,
    @SerialName("target_carbs") val targetCarbs: Float,
    @SerialName("target_fat") val targetFat: Float,
    @SerialName("meals_per_day") val mealsPerDay: Int = 3,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null
)

/**
 * Daily nutrition summary (calculated)
 */
data class DailyNutritionSummary(
    val date: String,
    val totalCalories: Float,
    val totalProtein: Float,
    val totalCarbs: Float,
    val totalFat: Float,
    val targetCalories: Float?,
    val targetProtein: Float?,
    val targetCarbs: Float?,
    val targetFat: Float?,
    val mealsCount: Int,
    val calorieProgress: Float, // percentage 0-100+
    val proteinProgress: Float,
    val carbsProgress: Float,
    val fatProgress: Float
)

/**
 * Weekly nutrition averages
 */
data class WeeklyNutritionSummary(
    val avgCalories: Int,
    val avgProtein: Int,
    val avgCarbs: Int,
    val avgFat: Int,
    val daysLogged: Int
)

/**
 * Complete nutrition data for a client
 */
data class ClientNutritionData(
    val logs: List<NutritionLog>,
    val summaries: List<DailyNutritionSummary>,
    val goal: NutritionGoal?
)
