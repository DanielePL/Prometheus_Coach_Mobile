package com.prometheuscoach.mobile.ui.screens.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prometheuscoach.mobile.data.model.CalendarEvent
import com.prometheuscoach.mobile.data.model.CalendarEventType
import com.prometheuscoach.mobile.data.model.Client
import com.prometheuscoach.mobile.ui.components.GlowAvatarSmall
import com.prometheuscoach.mobile.ui.components.GradientBackground
import com.prometheuscoach.mobile.ui.theme.PrometheusOrange
import com.prometheuscoach.mobile.ui.theme.RadiusExtraSmall
import com.prometheuscoach.mobile.ui.theme.RadiusLarge
import com.prometheuscoach.mobile.ui.theme.RadiusSmall
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onNavigateBack: () -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val state by viewModel.calendarState.collectAsState()
    var showAddEventSheet by remember { mutableStateOf(false) }

    GradientBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Schedule", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        TextButton(onClick = { viewModel.goToToday() }) {
                            Text("Today", color = PrometheusOrange)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        viewModel.loadClients()
                        showAddEventSheet = true
                    },
                    containerColor = PrometheusOrange
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Event", tint = Color.White)
                }
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Month Navigation
            MonthNavigationHeader(
                currentMonth = state.currentMonth,
                onPreviousMonth = { viewModel.goToPreviousMonth() },
                onNextMonth = { viewModel.goToNextMonth() }
            )

            // Calendar Grid
            CalendarGrid(
                currentMonth = state.currentMonth,
                selectedDate = state.selectedDate,
                events = state.events,
                onDateSelected = { viewModel.selectDate(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Selected Date Events
            SelectedDateSection(
                selectedDate = state.selectedDate,
                events = state.selectedDateEvents,
                isLoading = state.isLoading,
                onEventClick = { /* Navigate to event detail */ },
                onDeleteEvent = { viewModel.deleteEvent(it) },
                onMarkComplete = { viewModel.updateEventStatus(it, "completed") }
            )
        }
        }
    }

    // Add Event Bottom Sheet
    if (showAddEventSheet) {
        AddEventBottomSheet(
            selectedDate = state.selectedDate,
            clients = state.clients,
            isLoading = state.isCreatingEvent,
            isLoadingClients = state.isLoadingClients,
            onDismiss = { showAddEventSheet = false },
            onCreateEvent = { title, eventType, clientId, startTime, endTime, description ->
                viewModel.createEvent(
                    title = title,
                    eventType = eventType,
                    clientId = clientId,
                    startTime = startTime,
                    endTime = endTime,
                    description = description
                )
                showAddEventSheet = false
            }
        )
    }
}

@Composable
private fun MonthNavigationHeader(
    currentMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Previous Month",
                tint = PrometheusOrange
            )
        }

        Text(
            text = currentMonth.format(monthFormatter),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        IconButton(onClick = onNextMonth) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next Month",
                tint = PrometheusOrange
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    events: Map<LocalDate, List<CalendarEvent>>,
    onDateSelected: (LocalDate) -> Unit
) {
    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val today = LocalDate.now()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        // Day headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Calendar days
        val firstDayOfMonth = currentMonth.atDay(1)
        val lastDayOfMonth = currentMonth.atEndOfMonth()

        // Adjust for Monday start (ISO week)
        val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value - 1 // 0 = Monday
        val daysInMonth = currentMonth.lengthOfMonth()

        val totalCells = firstDayOfWeek + daysInMonth
        val rows = (totalCells + 6) / 7

        for (week in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (dayOfWeek in 0..6) {
                    val dayIndex = week * 7 + dayOfWeek - firstDayOfWeek
                    val date = if (dayIndex in 0 until daysInMonth) {
                        currentMonth.atDay(dayIndex + 1)
                    } else null

                    CalendarDay(
                        date = date,
                        isToday = date == today,
                        isSelected = date == selectedDate,
                        hasEvents = date?.let { events[it]?.isNotEmpty() == true } ?: false,
                        eventCount = date?.let { events[it]?.size } ?: 0,
                        onSelect = { date?.let { onDateSelected(it) } },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarDay(
    date: LocalDate?,
    isToday: Boolean,
    isSelected: Boolean,
    hasEvents: Boolean,
    eventCount: Int,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .then(
                when {
                    isSelected -> Modifier.background(PrometheusOrange)
                    isToday -> Modifier.border(2.dp, PrometheusOrange, CircleShape)
                    else -> Modifier
                }
            )
            .clickable(enabled = date != null, onClick = onSelect),
        contentAlignment = Alignment.Center
    ) {
        if (date != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    color = when {
                        isSelected -> Color.White
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
                )

                // Event indicator dots
                if (hasEvents && !isSelected) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        repeat(minOf(eventCount, 3)) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .background(PrometheusOrange, CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedDateSection(
    selectedDate: LocalDate,
    events: List<CalendarEvent>,
    isLoading: Boolean,
    onEventClick: (CalendarEvent) -> Unit,
    onDeleteEvent: (String) -> Unit,
    onMarkComplete: (String) -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                RoundedCornerShape(topStart = RadiusLarge, topEnd = RadiusLarge)
            )
    ) {
        // Date header
        Text(
            text = selectedDate.format(dateFormatter),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrometheusOrange)
            }
        } else if (events.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.EventAvailable,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No events scheduled",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Tap + to add a training session",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(events) { event ->
                    EventCard(
                        event = event,
                        onClick = { onEventClick(event) },
                        onDelete = { onDeleteEvent(event.id) },
                        onMarkComplete = { onMarkComplete(event.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventCard(
    event: CalendarEvent,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onMarkComplete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val eventColor = when (event.eventType) {
        "workout" -> PrometheusOrange
        "session" -> Color(0xFF4CAF50)
        "reminder" -> Color(0xFF2196F3)
        else -> MaterialTheme.colorScheme.primary
    }

    val eventIcon = when (event.eventType) {
        "workout" -> Icons.Default.FitnessCenter
        "session" -> Icons.Default.Person
        "reminder" -> Icons.Default.Notifications
        else -> Icons.Default.Event
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Event type indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(eventColor.copy(alpha = 0.15f), RoundedCornerShape(RadiusExtraSmall)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    eventIcon,
                    contentDescription = null,
                    tint = eventColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Event details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (event.clientName != null) {
                    Text(
                        text = event.clientName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (event.startTime != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = buildString {
                                append(event.startTime)
                                if (event.endTime != null) {
                                    append(" - ${event.endTime}")
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Status indicator
            if (event.status == "completed") {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Completed",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(20.dp)
                )
            }

            // More options
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (event.status != "completed") {
                        DropdownMenuItem(
                            text = { Text("Mark Complete") },
                            leadingIcon = {
                                Icon(Icons.Default.Check, contentDescription = null)
                            },
                            onClick = {
                                onMarkComplete()
                                showMenu = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            onDelete()
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEventBottomSheet(
    selectedDate: LocalDate,
    clients: List<Client>,
    isLoading: Boolean,
    isLoadingClients: Boolean,
    onDismiss: () -> Unit,
    onCreateEvent: (
        title: String,
        eventType: CalendarEventType,
        clientId: String?,
        startTime: String?,
        endTime: String?,
        description: String?
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedEventType by remember { mutableStateOf(CalendarEventType.SESSION) }
    var title by remember { mutableStateOf("") }
    var selectedClient by remember { mutableStateOf<Client?>(null) }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var showClientPicker by remember { mutableStateOf(false) }

    val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Schedule Event",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = selectedDate.format(dateFormatter),
                style = MaterialTheme.typography.bodyMedium,
                color = PrometheusOrange
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Event Type Selector
            Text(
                text = "Event Type",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    EventTypeChip(
                        label = "Session",
                        icon = Icons.Default.Person,
                        selected = selectedEventType == CalendarEventType.SESSION,
                        onClick = { selectedEventType = CalendarEventType.SESSION }
                    )
                }
                item {
                    EventTypeChip(
                        label = "Workout",
                        icon = Icons.Default.FitnessCenter,
                        selected = selectedEventType == CalendarEventType.WORKOUT,
                        onClick = { selectedEventType = CalendarEventType.WORKOUT }
                    )
                }
                item {
                    EventTypeChip(
                        label = "Reminder",
                        icon = Icons.Default.Notifications,
                        selected = selectedEventType == CalendarEventType.REMINDER,
                        onClick = { selectedEventType = CalendarEventType.REMINDER }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                placeholder = { Text("e.g., Strength Training Session") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrometheusOrange,
                    focusedLabelColor = PrometheusOrange
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Client Selection
            if (selectedEventType != CalendarEventType.REMINDER) {
                Text(
                    text = "Client",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedCard(
                    onClick = { showClientPicker = !showClientPicker },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedClient?.fullName ?: "Select a client",
                            color = if (selectedClient != null)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            if (showClientPicker) Icons.Default.KeyboardArrowUp
                            else Icons.Default.KeyboardArrowDown,
                            contentDescription = null
                        )
                    }
                }

                AnimatedVisibility(
                    visible = showClientPicker,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        if (isLoadingClients) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = PrometheusOrange
                                )
                            }
                        } else if (clients.isEmpty()) {
                            Text(
                                text = "No clients found",
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column {
                                clients.forEach { client ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedClient = client
                                                showClientPicker = false
                                            }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        GlowAvatarSmall(
                                            avatarUrl = client.avatarUrl,
                                            name = client.fullName
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(client.fullName)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Time inputs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = startTime,
                    onValueChange = { startTime = it },
                    label = { Text("Start Time") },
                    placeholder = { Text("09:00") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrometheusOrange,
                        focusedLabelColor = PrometheusOrange
                    )
                )

                OutlinedTextField(
                    value = endTime,
                    onValueChange = { endTime = it },
                    label = { Text("End Time") },
                    placeholder = { Text("10:00") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrometheusOrange,
                        focusedLabelColor = PrometheusOrange
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Notes (optional)") },
                placeholder = { Text("Add any additional details...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrometheusOrange,
                    focusedLabelColor = PrometheusOrange
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Create Button
            Button(
                onClick = {
                    onCreateEvent(
                        title,
                        selectedEventType,
                        selectedClient?.id,
                        startTime.takeIf { it.isNotBlank() },
                        endTime.takeIf { it.isNotBlank() },
                        description.takeIf { it.isNotBlank() }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = title.isNotBlank() && !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrometheusOrange
                ),
                shape = RoundedCornerShape(RadiusSmall)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    Text(
                        "Schedule Event",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun EventTypeChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = PrometheusOrange.copy(alpha = 0.2f),
            selectedLabelColor = PrometheusOrange,
            selectedLeadingIconColor = PrometheusOrange
        )
    )
}