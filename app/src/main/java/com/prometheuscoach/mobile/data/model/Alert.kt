package com.prometheuscoach.mobile.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ═══════════════════════════════════════════════════════════════════════════
// ALERT TYPES & PRIORITIES
// ═══════════════════════════════════════════════════════════════════════════

enum class AlertType {
    NO_WORKOUT,           // Client hasn't trained for X days
    MISSED_SCHEDULED,     // Scheduled workout missed
    NUTRITION_SLIPPING,   // Nutrition goals not met
    INACTIVE              // Completely inactive (no app opens)
}

enum class AlertPriority {
    CRITICAL,   // Red - act immediately
    WARNING,    // Orange - act soon
    NOTICE      // Yellow - keep an eye on
}

// ═══════════════════════════════════════════════════════════════════════════
// CLIENT ALERT MODEL
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Client alert for the coach dashboard.
 * Represents a client that needs attention.
 */
data class ClientAlert(
    val id: String = "",
    val clientId: String,
    val clientName: String,
    val clientAvatar: String? = null,
    val type: AlertType,
    val priority: AlertPriority,
    val title: String,           // "No workout for 3 days"
    val subtitle: String,        // "Last: Push Day (Thursday)"
    val daysSince: Int,
    val actionLabel: String,     // "Message" or "View"
    val createdAt: String? = null
)

/**
 * Database model for client_alerts table.
 */
@Serializable
data class ClientAlertEntity(
    val id: String = "",
    @SerialName("coach_id") val coachId: String,
    @SerialName("client_id") val clientId: String,
    @SerialName("alert_type") val alertType: String,
    val priority: String,
    val title: String,
    val subtitle: String? = null,
    @SerialName("days_since") val daysSince: Int = 0,
    @SerialName("is_resolved") val isResolved: Boolean = false,
    @SerialName("resolved_at") val resolvedAt: String? = null,
    @SerialName("dismissed_at") val dismissedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

// ═══════════════════════════════════════════════════════════════════════════
// WIN TYPES
// ═══════════════════════════════════════════════════════════════════════════

enum class WinType {
    STREAK_MILESTONE,     // 7, 14, 30, 60, 90 day streak
    PERSONAL_RECORD,      // New PR on an exercise
    VOLUME_RECORD,        // Highest volume
    NUTRITION_STREAK,     // X days protein goal reached
    CONSISTENCY           // 4 workouts this week
}

// ═══════════════════════════════════════════════════════════════════════════
// CLIENT WIN MODEL
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Client win for celebration on dashboard.
 */
data class ClientWin(
    val id: String = "",
    val clientId: String,
    val clientName: String,
    val clientAvatar: String? = null,
    val type: WinType,
    val title: String,           // "7 Day Streak!"
    val subtitle: String,        // "+10kg since last month"
    val value: String? = null,   // Additional data as JSON string
    val celebratable: Boolean = true,
    val celebrated: Boolean = false,
    val createdAt: String? = null
)

/**
 * Database model for client_wins table.
 */
@Serializable
data class ClientWinEntity(
    val id: String = "",
    @SerialName("coach_id") val coachId: String,
    @SerialName("client_id") val clientId: String,
    @SerialName("win_type") val winType: String,
    val title: String,
    val subtitle: String? = null,
    val value: String? = null,  // JSON string
    val celebrated: Boolean = false,
    @SerialName("celebrated_at") val celebratedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

// ═══════════════════════════════════════════════════════════════════════════
// COACH DASHBOARD STATE
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Combined view data for dashboard client alert with client info.
 */
@Serializable
data class DashboardAlertView(
    val id: String = "",
    @SerialName("coach_id") val coachId: String,
    @SerialName("client_id") val clientId: String,
    @SerialName("alert_type") val alertType: String,
    val priority: String,
    val title: String,
    val subtitle: String? = null,
    @SerialName("days_since") val daysSince: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
    // Client info (joined from profiles)
    @SerialName("client_name") val clientName: String? = null,
    @SerialName("client_avatar") val clientAvatar: String? = null
)

/**
 * Combined view data for dashboard client win with client info.
 */
@Serializable
data class DashboardWinView(
    val id: String = "",
    @SerialName("coach_id") val coachId: String,
    @SerialName("client_id") val clientId: String,
    @SerialName("win_type") val winType: String,
    val title: String,
    val subtitle: String? = null,
    val celebrated: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    // Client info (joined from profiles)
    @SerialName("client_name") val clientName: String? = null,
    @SerialName("client_avatar") val clientAvatar: String? = null
)

// ═══════════════════════════════════════════════════════════════════════════
// EXTENSION FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Convert database entity to UI model.
 */
fun DashboardAlertView.toClientAlert(): ClientAlert {
    return ClientAlert(
        id = id,
        clientId = clientId,
        clientName = clientName ?: "Unknown",
        clientAvatar = clientAvatar,
        type = try {
            AlertType.valueOf(alertType)
        } catch (e: Exception) {
            AlertType.NO_WORKOUT
        },
        priority = try {
            AlertPriority.valueOf(priority)
        } catch (e: Exception) {
            AlertPriority.NOTICE
        },
        title = title,
        subtitle = subtitle ?: "",
        daysSince = daysSince,
        actionLabel = "Message",
        createdAt = createdAt
    )
}

/**
 * Convert database entity to UI model.
 */
fun DashboardWinView.toClientWin(): ClientWin {
    return ClientWin(
        id = id,
        clientId = clientId,
        clientName = clientName ?: "Unknown",
        clientAvatar = clientAvatar,
        type = try {
            WinType.valueOf(winType)
        } catch (e: Exception) {
            WinType.STREAK_MILESTONE
        },
        title = title,
        subtitle = subtitle ?: "",
        celebratable = true,
        celebrated = celebrated,
        createdAt = createdAt
    )
}

// ═══════════════════════════════════════════════════════════════════════════
// CELEBRATION TEMPLATES
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Get a celebration message template for a win.
 */
fun getCelebrationTemplate(win: ClientWin): String {
    val firstName = win.clientName.split(" ").firstOrNull() ?: win.clientName
    return when (win.type) {
        WinType.STREAK_MILESTONE -> "Great job $firstName! ${win.title} Keep it up, you're on fire!"
        WinType.PERSONAL_RECORD -> "Congrats on the PR! ${win.subtitle} - that's what consistent work looks like!"
        WinType.VOLUME_RECORD -> "Incredible volume $firstName! ${win.subtitle} - your dedication is paying off!"
        WinType.NUTRITION_STREAK -> "Amazing nutrition discipline! ${win.subtitle} - keep fueling your gains!"
        WinType.CONSISTENCY -> "What a week $firstName! ${win.subtitle} - proud of your dedication!"
    }
}

/**
 * Get a suggested message for an alert.
 */
fun getSuggestedMessage(alert: ClientAlert): String {
    val firstName = alert.clientName.split(" ").firstOrNull() ?: alert.clientName
    return when (alert.type) {
        AlertType.NO_WORKOUT -> "Hey $firstName, how's it going? I noticed you've been away for a few days. Everything okay? Let me know if you need anything!"
        AlertType.MISSED_SCHEDULED -> "Hey, I saw today's workout didn't happen - is everything alright? Let me know if we need to reschedule."
        AlertType.NUTRITION_SLIPPING -> "Hi $firstName, I noticed nutrition has been a bit off lately. Want to chat about it? Sometimes life gets busy - let's figure out a sustainable approach."
        AlertType.INACTIVE -> "Hey $firstName, it's been a while! How are you doing? Would love to hear from you when you get a chance."
    }
}