package com.prometheuscoach.mobile.ui.screens.clients

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.focus.onFocusEvent
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.prometheuscoach.mobile.data.model.Client
import com.prometheuscoach.mobile.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToClientDetail: (String) -> Unit,
    openAddClientSheet: Boolean = false,
    viewModel: ClientsViewModel = hiltViewModel()
) {
    val clientsState by viewModel.clientsState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showAddClientSheet by remember { mutableStateOf(openAddClientSheet) }

    // Handle openAddClientSheet flag - load invite code when opening
    LaunchedEffect(openAddClientSheet) {
        if (openAddClientSheet) {
            viewModel.loadCoachInviteCode()
        }
    }
    val snackbarHostState = remember { SnackbarHostState() }

    val filteredClients = remember(clientsState.clients, searchQuery) {
        if (searchQuery.isBlank()) {
            clientsState.clients
        } else {
            clientsState.clients.filter { client ->
                client.fullName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Handle invite success
    LaunchedEffect(clientsState.inviteSuccess) {
        if (clientsState.inviteSuccess) {
            snackbarHostState.showSnackbar("Invitation sent successfully!")
            viewModel.clearInviteState()
            showAddClientSheet = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clients", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.loadCoachInviteCode()
                        showAddClientSheet = true
                    }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Add Client", tint = PrometheusOrange)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(DarkBackground, DarkBackgroundSecondary)
                    )
                )
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search clients...", color = Gray500) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = PrometheusOrange)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Gray400)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrometheusOrange,
                        unfocusedBorderColor = Gray700,
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                when {
                    clientsState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = PrometheusOrange)
                        }
                    }

                    clientsState.error != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = ErrorRed
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(clientsState.error!!, color = Gray400)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.loadClients() },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrometheusOrange)
                                ) {
                                    Text("Retry")
                                }
                            }
                        }
                    }

                    filteredClients.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.People,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = Gray600
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    if (searchQuery.isNotEmpty()) "No clients found" else "No clients yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    if (searchQuery.isNotEmpty()) "Try a different search" else "Invite your first client to start coaching",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Gray500,
                                    textAlign = TextAlign.Center
                                )
                                if (searchQuery.isEmpty()) {
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Button(
                                        onClick = {
                                            viewModel.loadCoachInviteCode()
                                            showAddClientSheet = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrometheusOrange)
                                    ) {
                                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Invite Client")
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                Text(
                                    "${filteredClients.size} client${if (filteredClients.size != 1) "s" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Gray500,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }

                            items(filteredClients) { client ->
                                ClientCard(
                                    client = client,
                                    onClick = { onNavigateToClientDetail(client.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Client Bottom Sheet
    if (showAddClientSheet) {
        AddClientBottomSheet(
            inviteCode = clientsState.coachInviteCode,
            isInviting = clientsState.isInviting,
            inviteError = clientsState.inviteError,
            onDismiss = {
                showAddClientSheet = false
                viewModel.clearInviteState()
            },
            onInviteByEmail = { email ->
                viewModel.inviteClientByEmail(email)
            }
        )
    }
}

@Composable
private fun ClientCard(
    client: Client,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black
        ),
        border = BorderStroke(1.dp, PrometheusOrange)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            if (client.avatarUrl != null) {
                AsyncImage(
                    model = client.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(PrometheusOrange.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = client.fullName.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrometheusOrange
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    client.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Client",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = PrometheusOrange
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// ADD CLIENT BOTTOM SHEET
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun AddClientBottomSheet(
    inviteCode: String?,
    isInviting: Boolean,
    inviteError: String?,
    onDismiss: () -> Unit,
    onInviteByEmail: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = PrometheusOrange,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Add Client",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "Invite athletes to your coaching roster",
                        color = Gray400,
                        fontSize = 14.sp
                    )
                }
            }

            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = PrometheusOrange,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = PrometheusOrange
                        )
                    }
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("By Email") },
                    selectedContentColor = PrometheusOrange,
                    unselectedContentColor = Gray400
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Share Code") },
                    selectedContentColor = PrometheusOrange,
                    unselectedContentColor = Gray400
                )
            }

            Spacer(Modifier.height(20.dp))

            when (selectedTab) {
                0 -> {
                    // Email Invitation
                    Text(
                        "INVITE BY EMAIL",
                        color = PrometheusOrange,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Enter the email of a registered Prometheus user to send them a coaching invitation.",
                        color = Gray400,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(16.dp))

                    // Error message
                    if (inviteError != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = ErrorRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(inviteError, color = ErrorRed, fontSize = 13.sp)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewRequester(bringIntoViewRequester)
                            .onFocusEvent { focusState ->
                                if (focusState.isFocused) {
                                    coroutineScope.launch {
                                        bringIntoViewRequester.bringIntoView()
                                    }
                                }
                            },
                        label = { Text("Email Address", color = Gray400) },
                        placeholder = { Text("athlete@example.com", color = Gray600) },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = PrometheusOrange)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrometheusOrange,
                            unfocusedBorderColor = Gray700,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = PrometheusOrange
                        )
                    )

                    Spacer(Modifier.height(20.dp))

                    Button(
                        onClick = { onInviteByEmail(email) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = email.isNotBlank() && email.contains("@") && !isInviting,
                        colors = ButtonDefaults.buttonColors(containerColor = PrometheusOrange),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isInviting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Icon(Icons.Default.Send, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isInviting) "Sending..." else "Send Invitation",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                1 -> {
                    // Share Invite Code
                    Text(
                        "YOUR COACH CODE",
                        color = PrometheusOrange,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Share this code with your athletes. They can enter it in the Prometheus app to request a coaching connection.",
                        color = Gray400,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(20.dp))

                    // Code Display Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = PrometheusOrange.copy(alpha = 0.1f)
                        ),
                        border = BorderStroke(2.dp, PrometheusOrange),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                inviteCode ?: "Loading...",
                                color = PrometheusOrange,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 4.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Copy & Share Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                inviteCode?.let { code ->
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Coach Code", code)
                                    clipboard.setPrimaryClip(clip)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, PrometheusOrange),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, null, tint = PrometheusOrange)
                            Spacer(Modifier.width(8.dp))
                            Text("Copy", color = PrometheusOrange)
                        }

                        Button(
                            onClick = {
                                inviteCode?.let { code ->
                                    val sendIntent = android.content.Intent().apply {
                                        action = android.content.Intent.ACTION_SEND
                                        putExtra(android.content.Intent.EXTRA_TEXT,
                                            "Join me on Prometheus Coach! Use code: $code")
                                        type = "text/plain"
                                    }
                                    context.startActivity(android.content.Intent.createChooser(sendIntent, "Share Code"))
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = PrometheusOrange),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Share, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Share")
                        }
                    }
                }
            }
        }
    }
}

