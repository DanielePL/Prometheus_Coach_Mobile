package com.prometheuscoach.mobile.data.repository

import android.util.Log
import com.prometheuscoach.mobile.data.model.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing client alerts and wins.
 * Generates alerts based on client activity and fetches stored alerts/wins.
 */
@Singleton
class AlertRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository,
    private val alertPreferencesManager: AlertPreferencesManager
) {
    companion object {
        private const val TAG = "AlertRepository"

        // Alert thresholds
        const val NO_WORKOUT_WARNING_DAYS = 3
        const val NO_WORKOUT_CRITICAL_DAYS = 5
        const val INACTIVE_CRITICAL_DAYS = 7
        const val NUTRITION_WARNING_DAYS = 3

        // Streak milestones
        val STREAK_MILESTONES = listOf(7, 14, 30, 60, 90, 180, 365)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FETCH ALERTS (from generated data)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Generate and fetch client alerts for the coach.
     * This combines real-time calculation with any stored alerts.
     * Dismissed alerts are filtered out.
     */
    suspend fun getClientAlerts(): Result<List<ClientAlert>> = withContext(Dispatchers.IO) {
        try {
            val coachId = authRepository.getCurrentUserId()
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            Log.d(TAG, "Generating alerts for coach: $coachId")

            // Get dismissed alert IDs
            val dismissedIds = alertPreferencesManager.getDismissedAlertIds()
            Log.d(TAG, "Found ${dismissedIds.size} dismissed alerts")

            // Get all clients for this coach
            val clients = supabaseClient.postgrest
                .from("coach_clients_v")
                .select {
                    filter {
                        eq("coach_id", coachId)
                        eq("status", "accepted")
                    }
                }
                .decodeList<CoachClientView>()

            Log.d(TAG, "Found ${clients.size} clients")

            // Generate alerts for each client IN PARALLEL for better performance
            val alertLists = clients.map { client ->
                async {
                    try {
                        generateAlertsForClient(client)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error generating alerts for ${client.clientName}", e)
                        emptyList()
                    }
                }
            }.awaitAll()

            val alerts = alertLists.flatten()

            // Filter out dismissed alerts and sort by priority (CRITICAL first), then by daysSince (longest first)
            val sortedAlerts = alerts
                .filter { it.id !in dismissedIds }
                .sortedWith(
                    compareBy<ClientAlert> { it.priority.ordinal }
                        .thenByDescending { it.daysSince }
                )

            Log.d(TAG, "Generated ${sortedAlerts.size} alerts total (after filtering dismissed)")
            Result.success(sortedAlerts)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get client alerts", e)
            Result.failure(e)
        }
    }

    /**
     * Generate alerts for a single client.
     */
    private suspend fun generateAlertsForClient(client: CoachClientView): List<ClientAlert> {
        val alerts = mutableListOf<ClientAlert>()

        try {
            // Get last workout for this client
            val lastWorkout = getLastWorkoutForClient(client.clientId)
            val daysSinceWorkout = lastWorkout?.let { calculateDaysSince(it.completedAt) } ?: Int.MAX_VALUE

            Log.d(TAG, "Client ${client.clientName}: days since workout = $daysSinceWorkout")

            // 1. NO WORKOUT ALERTS
            when {
                daysSinceWorkout >= NO_WORKOUT_CRITICAL_DAYS -> {
                    alerts.add(
                        ClientAlert(
                            id = "${client.clientId}_no_workout",
                            clientId = client.clientId,
                            clientName = client.clientName,
                            clientAvatar = client.clientAvatar,
                            type = AlertType.NO_WORKOUT,
                            priority = AlertPriority.CRITICAL,
                            title = "No workout for $daysSinceWorkout days",
                            subtitle = lastWorkout?.let {
                                "Last: ${it.workoutName ?: "Workout"}"
                            } ?: "No workouts logged yet",
                            daysSince = daysSinceWorkout,
                            actionLabel = "Message"
                        )
                    )
                }
                daysSinceWorkout in NO_WORKOUT_WARNING_DAYS until NO_WORKOUT_CRITICAL_DAYS -> {
                    alerts.add(
                        ClientAlert(
                            id = "${client.clientId}_no_workout",
                            clientId = client.clientId,
                            clientName = client.clientName,
                            clientAvatar = client.clientAvatar,
                            type = AlertType.NO_WORKOUT,
                            priority = AlertPriority.WARNING,
                            title = "No workout for $daysSinceWorkout days",
                            subtitle = lastWorkout?.let {
                                "Last: ${it.workoutName ?: "Workout"}"
                            } ?: "No workouts logged yet",
                            daysSince = daysSinceWorkout,
                            actionLabel = "Message"
                        )
                    )
                }
            }

            // 2. MISSED SCHEDULED WORKOUT ALERTS
            val missedWorkouts = getMissedScheduledWorkouts(client.clientId)
            for (missed in missedWorkouts) {
                val daysSinceMissed = calculateDaysSince(missed.scheduledDate)
                val priority = when {
                    daysSinceMissed >= 3 -> AlertPriority.CRITICAL
                    daysSinceMissed >= 1 -> AlertPriority.WARNING
                    else -> AlertPriority.NOTICE
                }

                alerts.add(
                    ClientAlert(
                        id = "${client.clientId}_missed_${missed.assignmentId}",
                        clientId = client.clientId,
                        clientName = client.clientName,
                        clientAvatar = client.clientAvatar,
                        type = AlertType.MISSED_SCHEDULED,
                        priority = priority,
                        title = "Missed: ${missed.workoutName}",
                        subtitle = "Scheduled for ${formatDateForDisplay(missed.scheduledDate)}",
                        daysSince = daysSinceMissed,
                        actionLabel = "Message"
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating alerts for client ${client.clientId}", e)
        }

        return alerts
    }

    /**
     * Get scheduled workouts that were missed (scheduled date passed, not completed).
     * NOTE: Currently disabled - workout_assignments table uses different column names.
     * TODO: Fix when database schema is clarified.
     */
    private suspend fun getMissedScheduledWorkouts(clientId: String): List<MissedWorkout> {
        // Skip this query for now - the workout_assignments table doesn't have user_id column
        // This was causing slow loading due to repeated failing network calls
        return emptyList()

        /* Original code - disabled until schema is fixed:
        return try {
            val today = LocalDate.now()

            // Get all scheduled assignments for this client
            val assignments = supabaseClient.postgrest
                .from("workout_assignments")
                .select {
                    filter {
                        eq("user_id", clientId)
                        eq("status", "active")
                    }
                }
                .decodeList<ScheduledAssignment>()
                .filter { !it.scheduledDate.isNullOrBlank() }

            // Filter to only past scheduled dates
            val pastAssignments = assignments.filter { assignment ->
                assignment.scheduledDate?.let { dateStr ->
                    val scheduledDate = parseDate(dateStr)
                    scheduledDate != null && scheduledDate.isBefore(today)
                } ?: false
            }

            if (pastAssignments.isEmpty()) {
                return emptyList()
            }

            // Get completed workouts for this client to check which were actually done
            val completedWorkoutIds = getCompletedWorkoutIds(clientId)

            // Find assignments that weren't completed
            val missedWorkouts = mutableListOf<MissedWorkout>()
            for (assignment in pastAssignments) {
                // Check if this specific assignment date has a completion
                val wasCompleted = checkIfWorkoutCompletedOnDate(
                    clientId = clientId,
                    workoutId = assignment.workoutId,
                    scheduledDate = assignment.scheduledDate!!
                )

                if (!wasCompleted) {
                    // Get workout name
                    val workoutName = getWorkoutName(assignment.workoutId)
                    missedWorkouts.add(
                        MissedWorkout(
                            assignmentId = assignment.id,
                            workoutId = assignment.workoutId,
                            workoutName = workoutName ?: "Workout",
                            scheduledDate = assignment.scheduledDate
                        )
                    )
                }
            }

            // Limit to most recent 3 missed workouts to avoid alert overload
            missedWorkouts.sortedByDescending { it.scheduledDate }.take(3)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting missed scheduled workouts for client $clientId", e)
            emptyList()
        }
        */
    }

    /**
     * Check if a workout was completed on or after the scheduled date.
     */
    private suspend fun checkIfWorkoutCompletedOnDate(
        clientId: String,
        workoutId: String,
        scheduledDate: String
    ): Boolean {
        return try {
            val completions = supabaseClient.postgrest
                .from("workout_history")
                .select {
                    filter {
                        eq("user_id", clientId)
                        eq("workout_id", workoutId)
                        gte("completed_at", scheduledDate)
                    }
                    limit(1)
                }
                .decodeList<WorkoutHistoryRow>()

            completions.isNotEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking workout completion", e)
            false
        }
    }

    /**
     * Get set of completed workout IDs for a client.
     */
    private suspend fun getCompletedWorkoutIds(clientId: String): Set<String> {
        return try {
            supabaseClient.postgrest
                .from("workout_history")
                .select {
                    filter { eq("user_id", clientId) }
                }
                .decodeList<WorkoutHistoryRow>()
                .mapNotNull { it.workoutId }
                .toSet()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting completed workout IDs", e)
            emptySet()
        }
    }

    /**
     * Get workout name by ID.
     */
    private suspend fun getWorkoutName(workoutId: String): String? {
        return try {
            val workouts = supabaseClient.postgrest
                .from("workouts")
                .select {
                    filter { eq("id", workoutId) }
                    limit(1)
                }
                .decodeList<WorkoutNameRow>()

            workouts.firstOrNull()?.name
        } catch (e: Exception) {
            Log.e(TAG, "Error getting workout name", e)
            null
        }
    }

    /**
     * Format a date string for display (e.g., "Monday, Dec 25").
     */
    private fun formatDateForDisplay(dateString: String): String {
        return try {
            val date = parseDate(dateString) ?: return dateString
            val formatter = DateTimeFormatter.ofPattern("EEEE, MMM d")
            date.format(formatter)
        } catch (e: Exception) {
            dateString
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // FETCH WINS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Generate and fetch client wins for the coach.
     * Already celebrated wins are marked as such.
     */
    suspend fun getClientWins(): Result<List<ClientWin>> = withContext(Dispatchers.IO) {
        try {
            val coachId = authRepository.getCurrentUserId()
                ?: return@withContext Result.failure(Exception("Not authenticated"))

            Log.d(TAG, "Generating wins for coach: $coachId")

            // Get celebrated win IDs
            val celebratedIds = alertPreferencesManager.getCelebratedWinIds()
            Log.d(TAG, "Found ${celebratedIds.size} celebrated wins")

            // Get all clients for this coach
            val clients = supabaseClient.postgrest
                .from("coach_clients_v")
                .select {
                    filter {
                        eq("coach_id", coachId)
                        eq("status", "accepted")
                    }
                }
                .decodeList<CoachClientView>()

            // Generate wins for each client IN PARALLEL for better performance
            val winLists = clients.map { client ->
                async {
                    try {
                        generateWinsForClient(client)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error generating wins for ${client.clientName}", e)
                        emptyList()
                    }
                }
            }.awaitAll()

            val wins = winLists.flatten()

            // Mark celebrated wins and sort by createdAt (newest first)
            val sortedWins = wins
                .map { win ->
                    if (win.id in celebratedIds) win.copy(celebrated = true) else win
                }
                .sortedByDescending { it.createdAt }

            Log.d(TAG, "Generated ${sortedWins.size} wins total")
            Result.success(sortedWins)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get client wins", e)
            Result.failure(e)
        }
    }

    /**
     * Generate wins for a single client.
     */
    private suspend fun generateWinsForClient(client: CoachClientView): List<ClientWin> {
        val wins = mutableListOf<ClientWin>()

        try {
            // Check for workout streak
            val streak = calculateWorkoutStreak(client.clientId)

            if (streak in STREAK_MILESTONES) {
                wins.add(
                    ClientWin(
                        id = "${client.clientId}_streak_$streak",
                        clientId = client.clientId,
                        clientName = client.clientName,
                        clientAvatar = client.clientAvatar,
                        type = WinType.STREAK_MILESTONE,
                        title = "$streak Day Streak!",
                        subtitle = "Consistency is paying off",
                        celebratable = true,
                        createdAt = Instant.now().toString()
                    )
                )
            }

            // Check for recent PRs
            val recentPRs = getRecentPRsForClient(client.clientId)
            for (pr in recentPRs) {
                wins.add(
                    ClientWin(
                        id = "${client.clientId}_pr_${pr.exerciseId}",
                        clientId = client.clientId,
                        clientName = client.clientName,
                        clientAvatar = client.clientAvatar,
                        type = WinType.PERSONAL_RECORD,
                        title = "New PR: ${pr.exerciseName}",
                        subtitle = "${pr.oldValue}kg → ${pr.newValue}kg (+${pr.newValue - pr.oldValue}kg)",
                        celebratable = true,
                        createdAt = pr.achievedAt
                    )
                )
            }

            // Check weekly consistency (4+ workouts this week)
            val workoutsThisWeek = getWorkoutsThisWeek(client.clientId)
            if (workoutsThisWeek >= 4) {
                wins.add(
                    ClientWin(
                        id = "${client.clientId}_consistency_${LocalDate.now().format(DateTimeFormatter.ISO_DATE)}",
                        clientId = client.clientId,
                        clientName = client.clientName,
                        clientAvatar = client.clientAvatar,
                        type = WinType.CONSISTENCY,
                        title = "Crushed this week!",
                        subtitle = "$workoutsThisWeek workouts completed",
                        celebratable = true,
                        createdAt = Instant.now().toString()
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating wins for client ${client.clientId}", e)
        }

        return wins
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPER QUERIES
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get the last workout for a client.
     */
    private suspend fun getLastWorkoutForClient(clientId: String): LastWorkout? {
        return try {
            val workouts = supabaseClient.postgrest
                .from("workout_history")
                .select {
                    filter {
                        eq("user_id", clientId)
                    }
                    order("completed_at", Order.DESCENDING)
                    limit(1)
                }
                .decodeList<WorkoutHistoryRow>()

            workouts.firstOrNull()?.let {
                LastWorkout(
                    workoutName = it.workoutName,
                    completedAt = it.completedAt ?: it.createdAt ?: ""
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching last workout for client $clientId", e)
            null
        }
    }

    /**
     * Calculate workout streak for a client.
     */
    private suspend fun calculateWorkoutStreak(clientId: String): Int {
        return try {
            // Get all workout dates for this client, ordered by date desc
            val workouts = supabaseClient.postgrest
                .from("workout_history")
                .select {
                    filter {
                        eq("user_id", clientId)
                    }
                    order("completed_at", Order.DESCENDING)
                }
                .decodeList<WorkoutHistoryRow>()

            if (workouts.isEmpty()) return 0

            // Calculate streak from consecutive days
            var streak = 0
            var currentDate = LocalDate.now()
            val workoutDates = workouts.mapNotNull { workout ->
                workout.completedAt?.let { parseDate(it) }
            }.distinct().sorted().reversed()

            for (date in workoutDates) {
                if (date == currentDate || date == currentDate.minusDays(1)) {
                    streak++
                    currentDate = date
                } else if (date.isBefore(currentDate.minusDays(1))) {
                    break
                }
            }

            streak
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating streak for client $clientId", e)
            0
        }
    }

    /**
     * Get recent PRs for a client (last 48 hours).
     * Uses the client_personal_bests_v view which tracks PRs with previous best weight.
     * NOTE: If the view doesn't exist, returns empty list gracefully.
     */
    private suspend fun getRecentPRsForClient(clientId: String): List<PersonalRecord> {
        return try {
            // Calculate timestamp for 48 hours ago
            val cutoffTime = Instant.now().minus(48, ChronoUnit.HOURS).toString()

            val pbs = supabaseClient.postgrest
                .from("client_personal_bests_v")
                .select {
                    filter {
                        eq("user_id", clientId)
                        gte("achieved_at", cutoffTime)
                    }
                }
                .decodeList<PersonalBestRow>()

            Log.d(TAG, "Found ${pbs.size} recent PRs for client $clientId")

            // Convert to PersonalRecord, only include actual improvements
            pbs.mapNotNull { pb ->
                // Only count as PR if there's a weight and it's an actual improvement
                val currentWeight = pb.bestWeight ?: return@mapNotNull null
                val previousWeight = pb.previousBestWeight ?: 0.0

                // Must be an actual improvement (not first time)
                if (previousWeight > 0 && currentWeight > previousWeight) {
                    PersonalRecord(
                        exerciseId = pb.exerciseId,
                        exerciseName = pb.exerciseName,
                        oldValue = previousWeight.toInt(),
                        newValue = currentWeight.toInt(),
                        achievedAt = pb.achievedAt
                    )
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            // The client_personal_bests_v view might not exist - this is OK, just return empty
            Log.w(TAG, "Could not fetch PRs for client $clientId (view may not exist): ${e.message}")
            emptyList()
        }
    }

    /**
     * Get number of workouts this week for a client.
     */
    private suspend fun getWorkoutsThisWeek(clientId: String): Int {
        return try {
            val startOfWeek = LocalDate.now()
                .minusDays(LocalDate.now().dayOfWeek.value.toLong() - 1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toString()

            val workouts = supabaseClient.postgrest
                .from("workout_history")
                .select {
                    filter {
                        eq("user_id", clientId)
                        gte("completed_at", startOfWeek)
                    }
                }
                .decodeList<WorkoutHistoryRow>()

            workouts.size
        } catch (e: Exception) {
            Log.e(TAG, "Error getting workouts this week for client $clientId", e)
            0
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ACTIONS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Mark an alert as dismissed.
     * The dismissal is persisted in DataStore for 14 days.
     */
    suspend fun dismissAlert(alertId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Dismissing alert: $alertId")
            alertPreferencesManager.dismissAlert(alertId)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dismiss alert", e)
            Result.failure(e)
        }
    }

    /**
     * Restore a dismissed alert (undo dismiss).
     */
    suspend fun restoreAlert(alertId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Restoring alert: $alertId")
            alertPreferencesManager.restoreAlert(alertId)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore alert", e)
            Result.failure(e)
        }
    }

    /**
     * Mark a win as celebrated.
     * The celebration is persisted in DataStore for 14 days.
     */
    suspend fun markWinCelebrated(winId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Celebrating win: $winId")
            alertPreferencesManager.celebrateWin(winId)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to celebrate win", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // UTILITY FUNCTIONS
    // ═══════════════════════════════════════════════════════════════════════════

    private fun calculateDaysSince(dateString: String?): Int {
        if (dateString.isNullOrBlank()) return Int.MAX_VALUE

        return try {
            val date = parseDate(dateString) ?: return Int.MAX_VALUE
            ChronoUnit.DAYS.between(date, LocalDate.now()).toInt()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing date: $dateString", e)
            Int.MAX_VALUE
        }
    }

    private fun parseDate(dateString: String): LocalDate? {
        return try {
            // Handle ISO instant format
            if (dateString.contains("T")) {
                Instant.parse(dateString).atZone(ZoneId.systemDefault()).toLocalDate()
            } else {
                LocalDate.parse(dateString)
            }
        } catch (e: Exception) {
            try {
                // Try parsing just the date part
                LocalDate.parse(dateString.substring(0, 10))
            } catch (e2: Exception) {
                null
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// HELPER DATA CLASSES
// ═══════════════════════════════════════════════════════════════════════════

private data class LastWorkout(
    val workoutName: String?,
    val completedAt: String
)

private data class PersonalRecord(
    val exerciseId: String,
    val exerciseName: String,
    val oldValue: Int,
    val newValue: Int,
    val achievedAt: String
)

@Serializable
private data class WorkoutHistoryRow(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("workout_id") val workoutId: String? = null,
    @SerialName("workout_name") val workoutName: String? = null,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
private data class ScheduledAssignment(
    val id: String,
    @SerialName("workout_id") val workoutId: String,
    @SerialName("user_id") val clientId: String,
    @SerialName("scheduled_date") val scheduledDate: String? = null,
    val status: String = "active"
)

private data class MissedWorkout(
    val assignmentId: String,
    val workoutId: String,
    val workoutName: String,
    val scheduledDate: String
)

@Serializable
private data class WorkoutNameRow(
    val id: String,
    val name: String
)

@Serializable
private data class PersonalBestRow(
    @SerialName("user_id") val userId: String,
    @SerialName("exercise_id") val exerciseId: String,
    @SerialName("exercise_name") val exerciseName: String,
    @SerialName("best_weight") val bestWeight: Double? = null,
    @SerialName("best_reps") val bestReps: Int? = null,
    @SerialName("achieved_at") val achievedAt: String,
    @SerialName("previous_best_weight") val previousBestWeight: Double? = null
)