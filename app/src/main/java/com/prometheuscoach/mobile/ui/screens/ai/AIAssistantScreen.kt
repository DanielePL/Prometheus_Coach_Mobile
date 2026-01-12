package com.prometheuscoach.mobile.ui.screens.ai

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.prometheuscoach.mobile.data.model.*
import com.prometheuscoach.mobile.ui.components.GradientBackground
import com.prometheuscoach.mobile.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

// ═══════════════════════════════════════════════════════════════════════════
// AI COACH ASSISTANT SCREEN - Prometheus V1 Design
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIAssistantScreen(
    contextType: AIContextType? = null,
    contextId: String? = null,
    contextName: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: AIAssistantViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Drawer state for conversation history
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    // Attachment state
    var attachments by remember { mutableStateOf<List<MessageAttachment>>(emptyList()) }
    var showAttachmentPicker by remember { mutableStateOf(false) }

    // Initialize with context if provided
    LaunchedEffect(contextType, contextId, contextName) {
        if (contextType != null) {
            viewModel.initWithContext(contextType, contextId, contextName)
        }
    }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(state.messages.size - 1)
            }
        }
    }

    // File Pickers
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val attachment = MessageAttachment(
                id = UUID.randomUUID().toString(),
                type = AttachmentType.IMAGE,
                fileName = "image_${System.currentTimeMillis()}.jpg",
                fileUri = it.toString()
            )
            attachments = attachments + attachment
        }
    }

    val documentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val attachment = MessageAttachment(
                id = UUID.randomUUID().toString(),
                type = AttachmentType.PDF,
                fileName = "document_${System.currentTimeMillis()}.pdf",
                fileUri = it.toString()
            )
            attachments = attachments + attachment
        }
    }

    // Camera capture
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && tempCameraUri != null) {
            try {
                val correctedUri = processCapturedImage(context, tempCameraUri!!)
                val attachment = MessageAttachment(
                    id = UUID.randomUUID().toString(),
                    type = AttachmentType.IMAGE,
                    fileName = "photo_${System.currentTimeMillis()}.jpg",
                    fileUri = correctedUri.toString()
                )
                attachments = attachments + attachment
            } catch (e: Exception) {
                Log.e("AIAssistantScreen", "Failed to process camera image", e)
                val attachment = MessageAttachment(
                    id = UUID.randomUUID().toString(),
                    type = AttachmentType.IMAGE,
                    fileName = "photo_${System.currentTimeMillis()}.jpg",
                    fileUri = tempCameraUri.toString()
                )
                attachments = attachments + attachment
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val photoFile = createImageFile(context)
            tempCameraUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            cameraLauncher.launch(tempCameraUri!!)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = DarkSurface,
                modifier = Modifier.width(300.dp)
            ) {
                ConversationsDrawerContent(
                    conversations = state.recentConversations,
                    currentConversationId = state.currentConversation?.id,
                    onConversationClick = { conversationId ->
                        viewModel.loadConversation(conversationId)
                        coroutineScope.launch { drawerState.close() }
                    },
                    onNewChatClick = {
                        viewModel.startNewConversation()
                        coroutineScope.launch { drawerState.close() }
                    },
                    onDeleteConversation = { conversationId ->
                        viewModel.deleteConversation(conversationId)
                    },
                    onClose = {
                        coroutineScope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        GradientBackground {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        title = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // AI Avatar with gradient
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(PrometheusOrangeGlow, PrometheusOrange)
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Psychology,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = "Prometheus Coach",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (state.contextName != null) {
                                            Text(
                                                text = state.contextName!!,
                                                color = PrometheusOrange,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = "•",
                                                color = PrometheusOrange.copy(alpha = 0.5f),
                                                fontSize = 12.sp
                                            )
                                        }
                                        Text(
                                            text = if (state.isSending) "typing..." else "online",
                                            color = if (state.isSending) PrometheusOrangeGlow else Color.Green,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                coroutineScope.launch { drawerState.open() }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Conversations",
                                    tint = Color.White
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { viewModel.startNewConversation() }) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "New chat",
                                    tint = PrometheusOrange
                                )
                            }
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color.White
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Error message
                    AnimatedVisibility(
                        visible = state.error != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        state.error?.let { error ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = error,
                                        modifier = Modifier.weight(1f),
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    IconButton(onClick = { viewModel.clearError() }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss"
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Main content
                    if (state.messages.isEmpty() && !state.isSending) {
                        // Empty state with quick actions
                        EmptyConversationContent(
                            contextType = state.contextType,
                            suggestions = state.suggestions,
                            onQuickAction = { action ->
                                viewModel.setContextType(action.contextType)
                            },
                            onSuggestionClick = { suggestion ->
                                viewModel.useSuggestion(suggestion)
                            }
                        )
                    } else {
                        // Messages list
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(vertical = 16.dp)
                        ) {
                            items(state.messages, key = { it.id }) { message ->
                                MessageBubble(message = message)
                            }

                            // Typing indicator
                            if (state.isSending) {
                                item {
                                    LoadingBubble()
                                }
                            }

                            // Suggestions after AI response
                            if (state.suggestions.isNotEmpty() && !state.isSending) {
                                item {
                                    SuggestionChips(
                                        suggestions = state.suggestions,
                                        onSuggestionClick = { viewModel.useSuggestion(it) }
                                    )
                                }
                            }

                            // Action items
                            if (state.actionItems.isNotEmpty()) {
                                item {
                                    ActionItemsCard(
                                        actionItems = state.actionItems,
                                        onActionClick = { /* TODO: Handle action */ }
                                    )
                                }
                            }
                        }
                    }

                    // Input Area
                    MessageInputArea(
                        text = state.inputText,
                        onTextChange = { viewModel.updateInputText(it) },
                        onSend = {
                            if (state.inputText.isNotBlank() || attachments.isNotEmpty()) {
                                viewModel.sendMessage()
                                attachments = emptyList()
                            }
                        },
                        isSending = state.isSending,
                        attachments = attachments,
                        onRemoveAttachment = { attachment ->
                            attachments = attachments.filter { it.id != attachment.id }
                        },
                        onAttachClick = { showAttachmentPicker = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Attachment Picker Dialog
            if (showAttachmentPicker) {
                AttachmentPickerDialog(
                    onDismiss = { showAttachmentPicker = false },
                    onTakePhoto = {
                        showAttachmentPicker = false
                        when {
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED -> {
                                val photoFile = createImageFile(context)
                                tempCameraUri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    photoFile
                                )
                                cameraLauncher.launch(tempCameraUri!!)
                            }
                            else -> {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    },
                    onChooseGallery = {
                        imagePicker.launch("image/*")
                        showAttachmentPicker = false
                    },
                    onChoosePdf = {
                        documentPicker.launch("application/pdf")
                        showAttachmentPicker = false
                    }
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// EMPTY CONVERSATION CONTENT
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun EmptyConversationContent(
    contextType: AIContextType,
    suggestions: List<String>,
    onQuickAction: (AIQuickAction) -> Unit,
    onSuggestionClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // AI Icon with glow
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            PrometheusOrange.copy(alpha = 0.4f),
                            PrometheusOrange.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(PrometheusOrangeGlow, PrometheusOrange)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "How can I help you today?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "I'm your AI coaching assistant. Ask me about client management, program design, or workout planning.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Quick actions
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(AIQuickAction.entries) { action ->
                QuickActionCard(
                    action = action,
                    isSelected = action.contextType == contextType,
                    onClick = { onQuickAction(action) }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Suggestions
        if (suggestions.isNotEmpty()) {
            Text(
                text = "Suggested prompts",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                suggestions.forEach { suggestion ->
                    SuggestionCard(
                        text = suggestion,
                        onClick = { onSuggestionClick(suggestion) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    action: AIQuickAction,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val icon = when (action) {
        AIQuickAction.CLIENT_INSIGHTS -> Icons.Default.Analytics
        AIQuickAction.PROGRAM_SUGGESTION -> Icons.Default.FitnessCenter
        AIQuickAction.DRAFT_MESSAGE -> Icons.Default.Edit
        AIQuickAction.WORKOUT_FEEDBACK -> Icons.Default.RateReview
        AIQuickAction.GENERAL_HELP -> Icons.Default.Help
    }

    Box(
        modifier = Modifier
            .width(140.dp)
            .then(
                if (isSelected) {
                    Modifier
                        .glassPremium(cornerRadius = RadiusMedium)
                        .border(1.dp, PrometheusOrange, RoundedCornerShape(RadiusMedium))
                } else {
                    Modifier.glassPremium(cornerRadius = RadiusMedium)
                }
            )
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) PrometheusOrange else Color.White,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = action.title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) PrometheusOrange else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SuggestionCard(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassPremium(cornerRadius = RadiusSmall)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                tint = PrometheusOrange,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MESSAGE BUBBLES
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun MessageBubble(message: AIMessage) {
    val isUser = message.isUser

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            // AI Avatar
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(PrometheusOrangeGlow, PrometheusOrange)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        val userShape = RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp)
        val aiShape = RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)

        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            // Message Content with glassmorphism
            Box(
                modifier = Modifier.then(
                    if (isUser) {
                        Modifier.glassUserMessage(userShape)
                    } else {
                        Modifier.glassAiMessage(aiShape)
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = message.content,
                        color = Color.White,
                        fontSize = 15.sp,
                        lineHeight = 20.sp
                    )

                    // Timestamp
                    Text(
                        text = formatTimestamp(message.timestamp),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        modifier = Modifier.align(if (isUser) Alignment.End else Alignment.Start)
                    )
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            // User Avatar
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(PrometheusOrange),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun LoadingBubble() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        // AI Avatar
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(PrometheusOrangeGlow, PrometheusOrange)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        val aiShape = RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)
        Box(
            modifier = Modifier.glassAiMessage(aiShape)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(PrometheusOrange.copy(alpha = 0.6f))
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// SUGGESTIONS & ACTIONS
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun SuggestionChips(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(suggestions) { suggestion ->
            SuggestionChip(
                onClick = { onSuggestionClick(suggestion) },
                label = {
                    Text(
                        text = suggestion,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = PrometheusOrange
                    )
                },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = PrometheusOrange.copy(alpha = 0.15f)
                ),
                border = SuggestionChipDefaults.suggestionChipBorder(
                    enabled = true,
                    borderColor = PrometheusOrange.copy(alpha = 0.3f)
                )
            )
        }
    }
}

@Composable
private fun ActionItemsCard(
    actionItems: List<AIActionItem>,
    onActionClick: (AIActionItem) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassPremium(cornerRadius = RadiusMedium)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.TaskAlt,
                    contentDescription = null,
                    tint = PrometheusOrange
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Suggested Actions",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            actionItems.forEach { item ->
                ActionItemRow(
                    item = item,
                    onClick = { onActionClick(item) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ActionItemRow(
    item: AIActionItem,
    onClick: () -> Unit
) {
    val icon = when (item.type) {
        "create_workout" -> Icons.Default.FitnessCenter
        "send_message" -> Icons.Default.Email
        "schedule_session" -> Icons.Default.CalendarMonth
        else -> Icons.Default.PlayArrow
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusSmall))
            .background(PrometheusOrange.copy(alpha = 0.15f))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrometheusOrange
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                item.description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MESSAGE INPUT
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun MessageInputArea(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isSending: Boolean,
    attachments: List<MessageAttachment>,
    onRemoveAttachment: (MessageAttachment) -> Unit,
    onAttachClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = DarkSurface,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Attachment Preview
            if (attachments.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(attachments) { attachment ->
                        AttachmentPreviewChip(
                            attachment = attachment,
                            onRemove = { onRemoveAttachment(attachment) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Attachment button
                IconButton(
                    onClick = onAttachClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = "Attach file",
                        tint = PrometheusOrangeGlow
                    )
                }

                // Text Input
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            "Ask your coach...",
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = PrometheusOrangeGlow,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        cursorColor = PrometheusOrangeGlow,
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant
                    ),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4
                )

                // Send Button
                IconButton(
                    onClick = onSend,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (text.isNotBlank() || attachments.isNotEmpty()) {
                                Brush.linearGradient(
                                    colors = listOf(PrometheusOrangeGlow, PrometheusOrange)
                                )
                            } else {
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.1f),
                                        Color.White.copy(alpha = 0.1f)
                                    )
                                )
                            }
                        ),
                    enabled = (text.isNotBlank() || attachments.isNotEmpty()) && !isSending
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (text.isNotBlank() || attachments.isNotEmpty())
                                Color.White else Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachmentPreviewChip(
    attachment: MessageAttachment,
    onRemove: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = PrometheusOrange.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (attachment.type) {
                    AttachmentType.IMAGE -> Icons.Default.Image
                    AttachmentType.PDF -> Icons.Default.PictureAsPdf
                },
                contentDescription = null,
                tint = PrometheusOrangeGlow,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = attachment.fileName.take(15) + if (attachment.fileName.length > 15) "..." else "",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// CONVERSATIONS DRAWER
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun ConversationsDrawerContent(
    conversations: List<AIConversation>,
    currentConversationId: String?,
    onConversationClick: (String) -> Unit,
    onNewChatClick: () -> Unit,
    onDeleteConversation: (String) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSurface)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Conversations",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }

        HorizontalDivider(color = DarkBorder)

        // New Chat Button
        Surface(
            onClick = onNewChatClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            color = PrometheusOrange.copy(alpha = 0.2f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = PrometheusOrange,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "New Chat",
                    color = PrometheusOrange,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        HorizontalDivider(
            color = DarkBorder,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // Conversations List
        if (conversations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No conversations yet",
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(conversations, key = { it.id }) { conversation ->
                    ConversationItem(
                        conversation = conversation,
                        isSelected = conversation.id == currentConversationId,
                        onClick = { onConversationClick(conversation.id) },
                        onDelete = { onDeleteConversation(conversation.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationItem(
    conversation: AIConversation,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = if (isSelected) PrometheusOrange.copy(alpha = 0.15f) else Color.Transparent,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = conversation.displayTitle,
                    color = if (isSelected) PrometheusOrange else Color.White,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                conversation.lastMessage?.content?.let { lastMsg ->
                    Text(
                        text = lastMsg,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = formatTimestamp(conversation.createdAt),
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp
                )
            }

            IconButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Conversation") },
            text = { Text("Are you sure you want to delete this conversation?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = DarkSurface
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// ATTACHMENT PICKER DIALOG
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun AttachmentPickerDialog(
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onChooseGallery: () -> Unit,
    onChoosePdf: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Attachment", color = Color.White) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Camera Button
                Button(
                    onClick = onTakePhoto,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrometheusOrange
                    )
                ) {
                    Icon(Icons.Default.CameraAlt, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Take Photo")
                }

                // Gallery Button
                Button(
                    onClick = onChooseGallery,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkSurfaceVariant
                    )
                ) {
                    Icon(Icons.Default.Image, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Choose from Gallery")
                }

                // PDF Button
                Button(
                    onClick = onChoosePdf,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkSurfaceVariant
                    )
                ) {
                    Icon(Icons.Default.PictureAsPdf, null)
                    Spacer(Modifier.width(8.dp))
                    Text("PDF Document")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White)
            }
        },
        containerColor = DarkSurface
    )
}

// ═══════════════════════════════════════════════════════════════════════════
// HELPER FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════════

private fun formatTimestamp(timestamp: String): String {
    return try {
        val instant = Instant.parse(timestamp)
        val now = Instant.now()
        val diff = java.time.Duration.between(instant, now)

        when {
            diff.toMinutes() < 1 -> "Just now"
            diff.toMinutes() < 60 -> "${diff.toMinutes()}m ago"
            diff.toHours() < 24 -> "${diff.toHours()}h ago"
            diff.toDays() < 7 -> "${diff.toDays()}d ago"
            else -> {
                val formatter = DateTimeFormatter.ofPattern("MMM d")
                    .withZone(ZoneId.systemDefault())
                formatter.format(instant)
            }
        }
    } catch (e: Exception) {
        timestamp
    }
}

private fun createImageFile(context: Context): File {
    val timestamp = System.currentTimeMillis()
    val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    return File.createTempFile(
        "COACH_${timestamp}_",
        ".jpg",
        storageDir
    )
}

private fun processCapturedImage(context: Context, uri: Uri): Uri {
    val inputStream = context.contentResolver.openInputStream(uri)
        ?: return uri

    val originalBitmap = BitmapFactory.decodeStream(inputStream)
    inputStream.close()

    val exifInputStream = context.contentResolver.openInputStream(uri)
    val exif = exifInputStream?.let { ExifInterface(it) }
    exifInputStream?.close()

    val rotation = when (exif?.getAttributeInt(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_NORMAL
    )) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }

    if (rotation == 0f) {
        return uri
    }

    val matrix = Matrix().apply { postRotate(rotation) }
    val rotatedBitmap = Bitmap.createBitmap(
        originalBitmap,
        0, 0,
        originalBitmap.width,
        originalBitmap.height,
        matrix,
        true
    )

    val outputFile = createImageFile(context)
    FileOutputStream(outputFile).use { out ->
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
    }

    if (rotatedBitmap != originalBitmap) {
        originalBitmap.recycle()
    }

    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        outputFile
    )
}

// ═══════════════════════════════════════════════════════════════════════════
// DATA MODELS FOR ATTACHMENTS
// ═══════════════════════════════════════════════════════════════════════════

data class MessageAttachment(
    val id: String,
    val type: AttachmentType,
    val fileName: String,
    val fileUri: String
)

enum class AttachmentType {
    IMAGE,
    PDF
}
