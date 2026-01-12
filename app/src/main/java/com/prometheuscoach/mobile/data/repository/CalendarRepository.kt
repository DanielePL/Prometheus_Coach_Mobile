package com.prometheuscoach.mobile.data.repository

import com.prometheuscoach.mobile.data.model.CalendarEvent
import com.prometheuscoach.mobile.data.model.CalendarEventType
import com.prometheuscoach.mobile.data.model.Client
import com.prometheuscoach.mobile.data.model.CreateCalendarEventRequest
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository
) {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /**
     * Get all events for a specific month.
     */
    suspend fun getEventsForMonth(yearMonth: YearMonth): Result<List<CalendarEvent>> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            val startDate = yearMonth.atDay(1).format(dateFormatter)
            val endDate = yearMonth.atEndOfMonth().format(dateFormatter)

            val events = supabaseClient.postgrest
                .from("coach_calendar_events")
                .select {
                    filter {
                        eq("coach_id", coachId)
                        gte("event_date", startDate)
                        lte("event_date", endDate)
                    }
                    order("event_date", Order.ASCENDING)
                    order("start_time", Order.ASCENDING)
                }
                .decodeList<CalendarEvent>()

            Result.success(events)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get events for a specific date.
     */
    suspend fun getEventsForDate(date: LocalDate): Result<List<CalendarEvent>> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            val dateStr = date.format(dateFormatter)

            val events = supabaseClient.postgrest
                .from("coach_calendar_events")
                .select {
                    filter {
                        eq("coach_id", coachId)
                        eq("event_date", dateStr)
                    }
                    order("start_time", Order.ASCENDING)
                }
                .decodeList<CalendarEvent>()

            Result.success(events)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create a new calendar event.
     */
    suspend fun createEvent(
        title: String,
        eventType: CalendarEventType,
        eventDate: LocalDate,
        clientId: String? = null,
        description: String? = null,
        startTime: String? = null,
        endTime: String? = null,
        workoutId: String? = null
    ): Result<CalendarEvent> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            val request = CreateCalendarEventRequest(
                coachId = coachId,
                clientId = clientId,
                title = title,
                description = description,
                eventType = eventType.value,
                eventDate = eventDate.format(dateFormatter),
                startTime = startTime,
                endTime = endTime,
                workoutId = workoutId
            )

            val event = supabaseClient.postgrest
                .from("coach_calendar_events")
                .insert(request) {
                    select()
                }
                .decodeSingle<CalendarEvent>()

            Result.success(event)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update an existing event.
     */
    suspend fun updateEvent(
        eventId: String,
        title: String? = null,
        description: String? = null,
        eventDate: LocalDate? = null,
        startTime: String? = null,
        endTime: String? = null,
        status: String? = null
    ): Result<Unit> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            val updates = buildMap<String, Any?> {
                title?.let { put("title", it) }
                description?.let { put("description", it) }
                eventDate?.let { put("event_date", it.format(dateFormatter)) }
                startTime?.let { put("start_time", it) }
                endTime?.let { put("end_time", it) }
                status?.let { put("status", it) }
            }

            if (updates.isNotEmpty()) {
                supabaseClient.postgrest
                    .from("coach_calendar_events")
                    .update(updates) {
                        filter {
                            eq("id", eventId)
                            eq("coach_id", coachId)
                        }
                    }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete an event.
     */
    suspend fun deleteEvent(eventId: String): Result<Unit> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            supabaseClient.postgrest
                .from("coach_calendar_events")
                .delete {
                    filter {
                        eq("id", eventId)
                        eq("coach_id", coachId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Schedule a workout for a client.
     */
    suspend fun scheduleWorkout(
        clientId: String,
        workoutId: String,
        workoutName: String,
        date: LocalDate,
        startTime: String? = null,
        notes: String? = null
    ): Result<CalendarEvent> {
        return createEvent(
            title = workoutName,
            eventType = CalendarEventType.WORKOUT,
            eventDate = date,
            clientId = clientId,
            description = notes,
            startTime = startTime,
            workoutId = workoutId
        )
    }

    /**
     * Schedule a coaching session with a client.
     */
    suspend fun scheduleSession(
        clientId: String,
        title: String,
        date: LocalDate,
        startTime: String,
        endTime: String? = null,
        description: String? = null
    ): Result<CalendarEvent> {
        return createEvent(
            title = title,
            eventType = CalendarEventType.SESSION,
            eventDate = date,
            clientId = clientId,
            description = description,
            startTime = startTime,
            endTime = endTime
        )
    }

    /**
     * Get upcoming events (next 7 days).
     */
    suspend fun getUpcomingEvents(days: Int = 7): Result<List<CalendarEvent>> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            val today = LocalDate.now()
            val endDate = today.plusDays(days.toLong())

            val events = supabaseClient.postgrest
                .from("coach_calendar_events")
                .select {
                    filter {
                        eq("coach_id", coachId)
                        gte("event_date", today.format(dateFormatter))
                        lte("event_date", endDate.format(dateFormatter))
                    }
                    order("event_date", Order.ASCENDING)
                    order("start_time", Order.ASCENDING)
                }
                .decodeList<CalendarEvent>()

            Result.success(events)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get clients for scheduling (for the event creation UI).
     */
    suspend fun getClientsForScheduling(): Result<List<Client>> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            // Use coach_clients_v view
            val clientViews = supabaseClient.postgrest
                .from("coach_clients_v")
                .select {
                    filter {
                        eq("coach_id", coachId)
                        eq("status", "accepted")
                    }
                    order("client_name", Order.ASCENDING)
                }
                .decodeList<com.prometheuscoach.mobile.data.model.CoachClientView>()

            val clients = clientViews.map { view ->
                Client(
                    id = view.clientId,
                    fullName = view.clientName,
                    avatarUrl = view.clientAvatar
                )
            }

            Result.success(clients)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}