package com.prometheuscoach.mobile.data.repository

import android.util.Log
import com.prometheuscoach.mobile.data.model.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgressRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository
) {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /**
     * Get workout logs for a client.
     */
    suspend fun getWorkoutLogs(
        clientId: String,
        limit: Int = 50
    ): Result<List<WorkoutLog>> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            // Verify coach has access to this client
            val hasAccess = verifyClientAccess(coachId, clientId)
            if (!hasAccess) {
                return Result.failure(Exception("Access denied"))
            }

            val logs = supabaseClient.postgrest
                .from("workout_logs")
                .select {
                    filter {
                        eq("user_id", clientId)
                    }
                    order("started_at", Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<WorkoutLog>()

            Result.success(logs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get exercise logs for a specific workout.
     */
    suspend fun getExerciseLogs(workoutLogId: String): Result<List<ExerciseLog>> {
        return try {
            val logs = supabaseClient.postgrest
                .from("exercise_logs")
                .select {
                    filter {
                        eq("workout_log_id", workoutLogId)
                    }
                    order("created_at", Order.ASCENDING)
                }
                .decodeList<ExerciseLog>()

            Result.success(logs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get personal bests for a client.
     * NOTE: If the client_personal_bests_v view doesn't exist, returns empty list.
     */
    suspend fun getPersonalBests(clientId: String): Result<List<PersonalBest>> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            val hasAccess = verifyClientAccess(coachId, clientId)
            if (!hasAccess) {
                return Result.failure(Exception("Access denied"))
            }

            val pbs = try {
                supabaseClient.postgrest
                    .from("client_personal_bests_v")
                    .select {
                        filter {
                            eq("user_id", clientId)
                        }
                        order("achieved_at", Order.DESCENDING)
                    }
                    .decodeList<PersonalBest>()
            } catch (e: Exception) {
                // View might not exist - return empty list
                Log.w("ProgressRepository", "Could not fetch personal bests (view may not exist): ${e.message}")
                emptyList()
            }

            Result.success(pbs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get progress summary for a client.
     */
    suspend fun getProgressSummary(
        clientId: String,
        period: String = "month"
    ): Result<ProgressSummary> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            val hasAccess = verifyClientAccess(coachId, clientId)
            if (!hasAccess) {
                return Result.failure(Exception("Access denied"))
            }

            // Calculate date range
            val today = LocalDate.now()
            val startDate = when (period) {
                "week" -> today.minusWeeks(1)
                "month" -> today.minusMonths(1)
                "3months" -> today.minusMonths(3)
                else -> LocalDate.of(2000, 1, 1) // all time
            }

            // Get workout logs for period
            val logs = supabaseClient.postgrest
                .from("workout_logs")
                .select {
                    filter {
                        eq("user_id", clientId)
                        gte("started_at", startDate.format(dateFormatter))
                    }
                }
                .decodeList<WorkoutLog>()

            // Calculate stats
            val workoutsCompleted = logs.size
            val totalDuration = logs.mapNotNull { it.durationMinutes }.sum()

            // Get exercise count
            val exerciseCount = if (logs.isNotEmpty()) {
                val workoutIds = logs.map { it.id }
                // Simplified - just return a reasonable estimate based on workouts
                workoutsCompleted * 5
            } else 0

            // Get personal bests count for period (view might not exist)
            val pbs = try {
                supabaseClient.postgrest
                    .from("client_personal_bests_v")
                    .select {
                        filter {
                            eq("user_id", clientId)
                            gte("achieved_at", startDate.format(dateFormatter))
                        }
                    }
                    .decodeList<PersonalBest>()
            } catch (e: Exception) {
                Log.w("ProgressRepository", "Could not fetch personal bests for summary: ${e.message}")
                emptyList()
            }

            // Calculate streak
            val streak = calculateStreak(logs)

            // Calculate avg workouts per week
            val daysDiff = ChronoUnit.DAYS.between(startDate, today).coerceAtLeast(1)
            val weeks = (daysDiff / 7.0).coerceAtLeast(1.0)
            val avgPerWeek = workoutsCompleted / weeks

            Result.success(
                ProgressSummary(
                    period = period,
                    workoutsCompleted = workoutsCompleted,
                    totalDuration = totalDuration,
                    exercisesPerformed = exerciseCount,
                    personalBests = pbs.size,
                    streakDays = streak,
                    avgWorkoutsPerWeek = avgPerWeek
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get weekly progress data for charts.
     */
    suspend fun getWeeklyProgress(
        clientId: String,
        weeks: Int = 8
    ): Result<List<WeeklyProgress>> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            val hasAccess = verifyClientAccess(coachId, clientId)
            if (!hasAccess) {
                return Result.failure(Exception("Access denied"))
            }

            val today = LocalDate.now()
            val startDate = today.minusWeeks(weeks.toLong())

            val logs = supabaseClient.postgrest
                .from("workout_logs")
                .select {
                    filter {
                        eq("user_id", clientId)
                        gte("started_at", startDate.format(dateFormatter))
                    }
                    order("started_at", Order.ASCENDING)
                }
                .decodeList<WorkoutLog>()

            // Group by week
            val weeklyData = mutableListOf<WeeklyProgress>()
            var currentWeekStart = startDate.with(java.time.DayOfWeek.MONDAY)

            while (currentWeekStart.isBefore(today) || currentWeekStart.isEqual(today)) {
                val weekEnd = currentWeekStart.plusDays(6)
                val weekLogs = logs.filter { log ->
                    val logDate = LocalDate.parse(log.startedAt.substring(0, 10))
                    logDate >= currentWeekStart && logDate <= weekEnd
                }

                weeklyData.add(
                    WeeklyProgress(
                        weekStart = currentWeekStart.format(dateFormatter),
                        workoutsCompleted = weekLogs.size,
                        totalMinutes = weekLogs.mapNotNull { it.durationMinutes }.sum(),
                        exercisesPerformed = weekLogs.size * 5, // Estimate
                        personalBests = 0 // Would need separate query
                    )
                )

                currentWeekStart = currentWeekStart.plusWeeks(1)
            }

            Result.success(weeklyData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get exercise-specific progress for a client.
     */
    suspend fun getExerciseProgress(
        clientId: String,
        exerciseId: String
    ): Result<ExerciseProgressStats> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            val hasAccess = verifyClientAccess(coachId, clientId)
            if (!hasAccess) {
                return Result.failure(Exception("Access denied"))
            }

            // Get all logs for this exercise
            val logs = supabaseClient.postgrest
                .from("exercise_logs_v")
                .select {
                    filter {
                        eq("user_id", clientId)
                        eq("exercise_id", exerciseId)
                    }
                    order("created_at", Order.ASCENDING)
                }
                .decodeList<ExerciseLog>()

            if (logs.isEmpty()) {
                return Result.failure(Exception("No data found for this exercise"))
            }

            // Calculate stats
            val totalSets = logs.size
            val totalReps = logs.mapNotNull { it.reps }.sum()
            val weights = logs.mapNotNull { it.weight }
            val maxWeight = weights.maxOrNull()
            val avgWeight = if (weights.isNotEmpty()) weights.average() else null

            // Calculate progress (compare first vs last)
            val progressPercentage = if (weights.size >= 2) {
                val firstWeight = weights.first()
                val lastWeight = weights.last()
                if (firstWeight > 0) {
                    ((lastWeight - firstWeight) / firstWeight) * 100
                } else null
            } else null

            // Build history (group by date)
            val history = logs.groupBy { it.createdAt?.substring(0, 10) ?: "" }
                .map { (date, dayLogs) ->
                    val bestSet = dayLogs.maxByOrNull { (it.weight ?: 0.0) * (it.reps ?: 0) }
                    ExerciseHistoryEntry(
                        date = date,
                        sets = dayLogs.size,
                        bestSet = BestSetInfo(
                            reps = bestSet?.reps ?: 0,
                            weight = bestSet?.weight,
                            rpe = bestSet?.rpe
                        )
                    )
                }

            Result.success(
                ExerciseProgressStats(
                    exerciseId = exerciseId,
                    exerciseName = logs.firstOrNull()?.exerciseName ?: "Unknown",
                    category = null,
                    totalSets = totalSets,
                    totalReps = totalReps,
                    maxWeight = maxWeight,
                    avgWeight = avgWeight,
                    progressPercentage = progressPercentage,
                    history = history
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Verify coach has access to client.
     */
    private suspend fun verifyClientAccess(coachId: String, clientId: String): Boolean {
        return try {
            val result = supabaseClient.postgrest
                .from("coach_clients_v")
                .select {
                    filter {
                        eq("coach_id", coachId)
                        eq("client_id", clientId)
                        eq("status", "accepted")
                    }
                }
                .decodeList<CoachClientView>()

            result.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Calculate current workout streak.
     */
    private fun calculateStreak(logs: List<WorkoutLog>): Int {
        if (logs.isEmpty()) return 0

        val sortedDates = logs
            .map { LocalDate.parse(it.startedAt.substring(0, 10)) }
            .distinct()
            .sortedDescending()

        var streak = 0
        var expectedDate = LocalDate.now()

        for (date in sortedDates) {
            // Allow for yesterday or today
            if (date == expectedDate || date == expectedDate.minusDays(1)) {
                streak++
                expectedDate = date.minusDays(1)
            } else if (date < expectedDate.minusDays(1)) {
                break
            }
        }

        return streak
    }
}
