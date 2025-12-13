package com.prometheuscoach.mobile.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prometheuscoach.mobile.data.model.CalendarEvent
import com.prometheuscoach.mobile.data.model.CalendarEventType
import com.prometheuscoach.mobile.data.model.Client
import com.prometheuscoach.mobile.data.repository.CalendarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class CalendarState(
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val events: Map<LocalDate, List<CalendarEvent>> = emptyMap(),
    val selectedDateEvents: List<CalendarEvent> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,

    // Event creation state
    val isCreatingEvent: Boolean = false,
    val createEventError: String? = null,
    val clients: List<Client> = emptyList(),
    val isLoadingClients: Boolean = false
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val calendarRepository: CalendarRepository
) : ViewModel() {

    private val _calendarState = MutableStateFlow(CalendarState())
    val calendarState: StateFlow<CalendarState> = _calendarState.asStateFlow()

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    init {
        loadEventsForMonth(_calendarState.value.currentMonth)
        loadEventsForDate(_calendarState.value.selectedDate)
    }

    fun loadEventsForMonth(yearMonth: YearMonth) {
        viewModelScope.launch {
            _calendarState.update { it.copy(isLoading = true, error = null) }

            calendarRepository.getEventsForMonth(yearMonth)
                .onSuccess { eventList ->
                    // Group events by date
                    val eventsByDate = eventList.groupBy { event ->
                        LocalDate.parse(event.eventDate)
                    }

                    _calendarState.update {
                        it.copy(
                            isLoading = false,
                            events = eventsByDate,
                            currentMonth = yearMonth
                        )
                    }
                }
                .onFailure { exception ->
                    _calendarState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message
                        )
                    }
                }
        }
    }

    fun loadEventsForDate(date: LocalDate) {
        viewModelScope.launch {
            calendarRepository.getEventsForDate(date)
                .onSuccess { events ->
                    _calendarState.update {
                        it.copy(
                            selectedDate = date,
                            selectedDateEvents = events
                        )
                    }
                }
                .onFailure { exception ->
                    _calendarState.update {
                        it.copy(
                            error = exception.message
                        )
                    }
                }
        }
    }

    fun selectDate(date: LocalDate) {
        _calendarState.update { it.copy(selectedDate = date) }
        loadEventsForDate(date)
    }

    fun goToPreviousMonth() {
        val newMonth = _calendarState.value.currentMonth.minusMonths(1)
        loadEventsForMonth(newMonth)
    }

    fun goToNextMonth() {
        val newMonth = _calendarState.value.currentMonth.plusMonths(1)
        loadEventsForMonth(newMonth)
    }

    fun goToToday() {
        val today = LocalDate.now()
        val currentMonth = YearMonth.from(today)

        if (_calendarState.value.currentMonth != currentMonth) {
            loadEventsForMonth(currentMonth)
        }
        selectDate(today)
    }

    fun loadClients() {
        viewModelScope.launch {
            _calendarState.update { it.copy(isLoadingClients = true) }

            calendarRepository.getClientsForScheduling()
                .onSuccess { clients ->
                    _calendarState.update {
                        it.copy(
                            clients = clients,
                            isLoadingClients = false
                        )
                    }
                }
                .onFailure {
                    _calendarState.update {
                        it.copy(isLoadingClients = false)
                    }
                }
        }
    }

    fun createEvent(
        title: String,
        eventType: CalendarEventType,
        clientId: String? = null,
        description: String? = null,
        startTime: String? = null,
        endTime: String? = null
    ) {
        viewModelScope.launch {
            _calendarState.update { it.copy(isCreatingEvent = true, createEventError = null) }

            calendarRepository.createEvent(
                title = title,
                eventType = eventType,
                eventDate = _calendarState.value.selectedDate,
                clientId = clientId,
                description = description,
                startTime = startTime,
                endTime = endTime
            )
                .onSuccess {
                    _calendarState.update { it.copy(isCreatingEvent = false) }
                    // Refresh events
                    loadEventsForMonth(_calendarState.value.currentMonth)
                    loadEventsForDate(_calendarState.value.selectedDate)
                }
                .onFailure { exception ->
                    _calendarState.update {
                        it.copy(
                            isCreatingEvent = false,
                            createEventError = exception.message
                        )
                    }
                }
        }
    }

    fun scheduleSession(
        clientId: String,
        title: String,
        startTime: String,
        endTime: String? = null,
        description: String? = null
    ) {
        viewModelScope.launch {
            _calendarState.update { it.copy(isCreatingEvent = true, createEventError = null) }

            calendarRepository.scheduleSession(
                clientId = clientId,
                title = title,
                date = _calendarState.value.selectedDate,
                startTime = startTime,
                endTime = endTime,
                description = description
            )
                .onSuccess {
                    _calendarState.update { it.copy(isCreatingEvent = false) }
                    loadEventsForMonth(_calendarState.value.currentMonth)
                    loadEventsForDate(_calendarState.value.selectedDate)
                }
                .onFailure { exception ->
                    _calendarState.update {
                        it.copy(
                            isCreatingEvent = false,
                            createEventError = exception.message
                        )
                    }
                }
        }
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            calendarRepository.deleteEvent(eventId)
                .onSuccess {
                    loadEventsForMonth(_calendarState.value.currentMonth)
                    loadEventsForDate(_calendarState.value.selectedDate)
                }
        }
    }

    fun updateEventStatus(eventId: String, status: String) {
        viewModelScope.launch {
            calendarRepository.updateEvent(eventId = eventId, status = status)
                .onSuccess {
                    loadEventsForMonth(_calendarState.value.currentMonth)
                    loadEventsForDate(_calendarState.value.selectedDate)
                }
        }
    }

    fun clearError() {
        _calendarState.update { it.copy(error = null, createEventError = null) }
    }
}