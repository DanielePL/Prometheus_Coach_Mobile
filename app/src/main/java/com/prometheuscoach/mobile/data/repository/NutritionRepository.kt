package com.prometheuscoach.mobile.data.repository

import com.prometheuscoach.mobile.data.model.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NutritionRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository
) {

    /**
     * Get nutrition data for a client (last N days).
     * Includes logs, meals, meal items, and active goal.
     */
    suspend fun getClientNutrition(clientId: String, days: Int = 30): Result<ClientNutritionData> {
        return try {
            val startDate = LocalDate.now().minusDays(days.toLong())
            val startDateStr = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

            // Get nutrition logs
            val logs = supabaseClient.postgrest
                .from("nutrition_logs")
                .select {
                    filter {
                        eq("user_id", clientId)
                        gte("date", startDateStr)
                    }
                    order("date", Order.DESCENDING)
                }
                .decodeList<NutritionLog>()

            if (logs.isEmpty()) {
                // Still try to get the goal even if no logs
                val goal = getActiveGoal(clientId)
                return Result.success(ClientNutritionData(
                    logs = emptyList(),
                    summaries = emptyList(),
                    goal = goal
                ))
            }

            // Get meals for these logs
            val logIds = logs.map { it.id }
            val meals = supabaseClient.postgrest
                .from("meals")
                .select {
                    filter {
                        isIn("nutrition_log_id", logIds)
                    }
                    order("time", Order.ASCENDING)
                }
                .decodeList<Meal>()

            // Get meal items
            val mealIds = meals.map { it.id }
            val items = if (mealIds.isNotEmpty()) {
                supabaseClient.postgrest
                    .from("meal_items")
                    .select {
                        filter {
                            isIn("meal_id", mealIds)
                        }
                    }
                    .decodeList<MealItem>()
            } else {
                emptyList()
            }

            // Get active goal
            val goal = getActiveGoal(clientId)

            // Attach items to meals
            val mealsWithItems = meals.map { meal ->
                meal.copy(mealItems = items.filter { it.mealId == meal.id })
            }

            // Attach meals to logs
            val logsWithMeals = logs.map { log ->
                log.copy(meals = mealsWithItems.filter { it.nutritionLogId == log.id })
            }

            // Calculate daily summaries
            val summaries = logsWithMeals.map { log ->
                val allItems = log.meals?.flatMap { it.mealItems ?: emptyList() } ?: emptyList()
                val totalCalories = allItems.sumOf { it.calories.toDouble() }.toFloat()
                val totalProtein = allItems.sumOf { it.protein.toDouble() }.toFloat()
                val totalCarbs = allItems.sumOf { it.carbs.toDouble() }.toFloat()
                val totalFat = allItems.sumOf { it.fat.toDouble() }.toFloat()

                val targetCalories = log.targetCalories ?: goal?.targetCalories
                val targetProtein = log.targetProtein ?: goal?.targetProtein
                val targetCarbs = log.targetCarbs ?: goal?.targetCarbs
                val targetFat = log.targetFat ?: goal?.targetFat

                DailyNutritionSummary(
                    date = log.date,
                    totalCalories = totalCalories,
                    totalProtein = totalProtein,
                    totalCarbs = totalCarbs,
                    totalFat = totalFat,
                    targetCalories = targetCalories,
                    targetProtein = targetProtein,
                    targetCarbs = targetCarbs,
                    targetFat = targetFat,
                    mealsCount = log.meals?.size ?: 0,
                    calorieProgress = if (targetCalories != null && targetCalories > 0) (totalCalories / targetCalories) * 100 else 0f,
                    proteinProgress = if (targetProtein != null && targetProtein > 0) (totalProtein / targetProtein) * 100 else 0f,
                    carbsProgress = if (targetCarbs != null && targetCarbs > 0) (totalCarbs / targetCarbs) * 100 else 0f,
                    fatProgress = if (targetFat != null && targetFat > 0) (totalFat / targetFat) * 100 else 0f
                )
            }

            Result.success(ClientNutritionData(
                logs = logsWithMeals,
                summaries = summaries,
                goal = goal
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get weekly nutrition averages for a client.
     */
    suspend fun getClientNutritionWeekly(clientId: String): Result<WeeklyNutritionSummary> {
        return try {
            val endDate = LocalDate.now()
            val startDate = endDate.minusDays(7)

            val logs = supabaseClient.postgrest
                .from("nutrition_logs")
                .select {
                    filter {
                        eq("user_id", clientId)
                        gte("date", startDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                        lte("date", endDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    }
                }
                .decodeList<NutritionLog>()

            if (logs.isEmpty()) {
                return Result.success(WeeklyNutritionSummary(
                    avgCalories = 0,
                    avgProtein = 0,
                    avgCarbs = 0,
                    avgFat = 0,
                    daysLogged = 0
                ))
            }

            // Get meals for these logs
            val logIds = logs.map { it.id }
            val meals = supabaseClient.postgrest
                .from("meals")
                .select {
                    filter {
                        isIn("nutrition_log_id", logIds)
                    }
                }
                .decodeList<Meal>()

            // Get meal items
            val mealIds = meals.map { it.id }
            val items = if (mealIds.isNotEmpty()) {
                supabaseClient.postgrest
                    .from("meal_items")
                    .select {
                        filter {
                            isIn("meal_id", mealIds)
                        }
                    }
                    .decodeList<MealItem>()
            } else {
                emptyList()
            }

            // Calculate totals
            var totalCalories = 0f
            var totalProtein = 0f
            var totalCarbs = 0f
            var totalFat = 0f

            items.forEach { item ->
                totalCalories += item.calories
                totalProtein += item.protein
                totalCarbs += item.carbs
                totalFat += item.fat
            }

            val daysLogged = logs.size

            Result.success(WeeklyNutritionSummary(
                avgCalories = if (daysLogged > 0) (totalCalories / daysLogged).toInt() else 0,
                avgProtein = if (daysLogged > 0) (totalProtein / daysLogged).toInt() else 0,
                avgCarbs = if (daysLogged > 0) (totalCarbs / daysLogged).toInt() else 0,
                avgFat = if (daysLogged > 0) (totalFat / daysLogged).toInt() else 0,
                daysLogged = daysLogged
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get active nutrition goal for a client.
     */
    private suspend fun getActiveGoal(clientId: String): NutritionGoal? {
        return try {
            val goals = supabaseClient.postgrest
                .from("nutrition_goals")
                .select {
                    filter {
                        eq("user_id", clientId)
                        eq("is_active", true)
                    }
                    limit(1)
                }
                .decodeList<NutritionGoal>()

            goals.firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Set nutrition goals for a client (coach action).
     * Deactivates existing goals and creates a new one.
     */
    suspend fun setClientNutritionGoal(
        clientId: String,
        goalType: String,
        targetCalories: Float,
        targetProtein: Float,
        targetCarbs: Float,
        targetFat: Float
    ): Result<NutritionGoal> {
        return try {
            // Deactivate existing goals
            supabaseClient.postgrest
                .from("nutrition_goals")
                .update(mapOf("is_active" to false)) {
                    filter {
                        eq("user_id", clientId)
                        eq("is_active", true)
                    }
                }

            // Create new goal
            val newGoal = supabaseClient.postgrest
                .from("nutrition_goals")
                .insert(mapOf(
                    "user_id" to clientId,
                    "goal_type" to goalType,
                    "target_calories" to targetCalories,
                    "target_protein" to targetProtein,
                    "target_carbs" to targetCarbs,
                    "target_fat" to targetFat,
                    "is_active" to true
                )) {
                    select()
                }
                .decodeSingle<NutritionGoal>()

            Result.success(newGoal)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
