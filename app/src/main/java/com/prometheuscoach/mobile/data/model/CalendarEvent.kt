package com.prometheuscoach.mobile.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Calendar event entity from coach_calendar_v view.
 * Aggregates scheduled workouts, sessions, and custom events.
 */
@Serializable
data class CalendarEvent(
    val id: String,
    @SerialName("coach_id")
    val coachId: String,
    @SerialName("user_id")
    val clientId: String? = null,
    @SerialName("client_name")
    val clientName: String? = null,
    @SerialName("client_avatar")
    val clientAvatar: String? = null,
    val title: String,
    val description: String? = null,
    @SerialName("event_type")
    val eventType: String,  // 'workout', 'session', 'reminder', 'custom'
    @SerialName("event_date")
    val eventDate: String,
    @SerialName("start_time")
    val startTime: String? = null,
    @SerialName("end_time")
    val endTime: String? = null,
    @SerialName("routine_id")
    val routineId: String? = null,
    @SerialName("routine_name")
    val routineName: String? = null,
    val status: String = "scheduled",  // 'scheduled', 'completed', 'cancelled'
    @SerialName("created_at")
    val createdAt: String? = null
)

/**
 * Request model for creating a calendar event.
 */
@Serializable
data class CreateCalendarEventRequest(
    @SerialName("coach_id")
    val coachId: String,
    @SerialName("user_id")
    val clientId: String? = null,
    val title: String,
    val description: String? = null,
    @SerialName("event_type")
    val eventType: String,
    @SerialName("event_date")
    val eventDate: String,
    @SerialName("start_time")
    val startTime: String? = null,
    @SerialName("end_time")
    val endTime: String? = null,
    @SerialName("routine_id")
    val routineId: String? = null
)

/**
 * Enum for calendar event types.
 */
enum class CalendarEventType(val value: String) {
    WORKOUT("workout"),
    SESSION("session"),
    REMINDER("reminder"),
    CUSTOM("custom")
}

/**
 * Enum for event status.
 */
enum class EventStatus(val value: String) {
    SCHEDULED("scheduled"),
    COMPLETED("completed"),
    CANCELLED("cancelled")
}

/**
 * UI model for displaying events grouped by date.
 */
data class DayWithEvents(
    val date: String,
    val events: List<CalendarEvent>,
    val isToday: Boolean = false,
    val isSelected: Boolean = false
)