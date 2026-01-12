package com.prometheuscoach.mobile.ui.screens.chat

import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.Color
import com.prometheuscoach.mobile.data.model.AttachmentType
import com.prometheuscoach.mobile.data.model.MessageWithSender
import com.prometheuscoach.mobile.ui.components.GlowAvatar
import com.prometheuscoach.mobile.ui.components.GlowAvatarSmall
import com.prometheuscoach.mobile.ui.components.GradientBackground
import com.prometheuscoach.mobile.ui.theme.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String,
    onNavigateBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val messageInput by viewModel.messageInput.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // File picker launcher for images and documents
    // Using GetContent for simpler file selection (more stable with activity lifecycle)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        android.util.Log.d("ChatScreen", "File picker returned, uri: $uri")

        if (uri == null) {
            android.util.Log.d("ChatScreen", "File picker cancelled or no file selected")
            return@rememberLauncherForActivityResult
        }

        scope.launch {
            try {
                android.util.Log.d("ChatScreen", "Processing file: $uri")
                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
                android.util.Log.d("ChatScreen", "File mimeType: $mimeType")

                // Get actual filename from cursor
                val fileName = contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    cursor.moveToFirst()
                    if (nameIndex >= 0) cursor.getString(nameIndex) else null
                } ?: "attachment_${System.currentTimeMillis()}"
                android.util.Log.d("ChatScreen", "File name: $fileName")

                // Read file bytes with size check
                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    android.util.Log.e("ChatScreen", "Could not open input stream for file")
                    return@launch
                }

                val fileBytes = inputStream.use { it.readBytes() }
                val fileSize = fileBytes.size.toLong()
                android.util.Log.d("ChatScreen", "File size: $fileSize bytes")

                // Check file size (max 20MB)
                if (fileSize > 20 * 1024 * 1024) {
                    android.util.Log.e("ChatScreen", "File too large: $fileSize bytes")
                    // TODO: Show error to user
                    return@launch
                }

                // Send the attachment
                android.util.Log.d("ChatScreen", "Sending attachment...")
                viewModel.sendMessageWithAttachment(
                    fileBytes = fileBytes,
                    fileName = fileName,
                    mimeType = mimeType,
                    fileSize = fileSize
                )
                android.util.Log.d("ChatScreen", "Attachment sent successfully")
            } catch (e: OutOfMemoryError) {
                android.util.Log.e("ChatScreen", "File too large - out of memory", e)
            } catch (e: Exception) {
                android.util.Log.e("ChatScreen", "Failed to process attachment", e)
            }
        }
    }

    // Load chat when conversationId changes
    LaunchedEffect(conversationId) {
        viewModel.loadChat(conversationId)
    }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    // Show error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    GradientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            state.otherParticipant?.let { participant ->
                                GlowAvatar(
                                    avatarUrl = participant.avatarUrl,
                                    name = participant.fullName,
                                    size = 40.dp,
                                    borderWidth = 1.5.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    participant.fullName,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            } ?: Text("Chat", color = Color.White)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            MessageInputBar(
                value = messageInput,
                onValueChange = viewModel::updateMessageInput,
                onSend = viewModel::sendMessage,
                onAttachmentClick = { filePickerLauncher.launch("*/*") },
                isSending = state.isSending,
                isUploading = state.isUploadingAttachment
            )
        }
    ) { paddingValues ->
        when {
            state.isLoading && state.messages.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrometheusOrange)
                }
            }

            state.messages.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No messages yet",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Send a message to start the conversation",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.messages) { message ->
                        MessageBubble(
                            message = message,
                            onAttachmentClick = { attachmentUrl ->
                                // Get signed URL and open attachment
                                scope.launch {
                                    viewModel.getAttachmentUrl(attachmentUrl)?.let { signedUrl ->
                                        // Open URL in browser or image viewer
                                        val intent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            Uri.parse(signedUrl)
                                        )
                                        context.startActivity(intent)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun MessageBubble(
    message: MessageWithSender,
    onAttachmentClick: (String) -> Unit = {}
) {
    val isFromCurrentUser = message.isFromCurrentUser

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isFromCurrentUser) {
            // Show avatar for other user with glow ring
            GlowAvatarSmall(
                avatarUrl = message.senderAvatar,
                name = message.senderName
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isFromCurrentUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isFromCurrentUser) 16.dp else 4.dp,
                    bottomEnd = if (isFromCurrentUser) 4.dp else 16.dp
                ),
                color = if (isFromCurrentUser) {
                    PrometheusOrange
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    // Show attachment if present
                    if (message.hasAttachment && message.fileUrl != null) {
                        AttachmentPreview(
                            fileType = message.fileType,
                            fileName = message.fileName,
                            isFromCurrentUser = isFromCurrentUser,
                            onClick = { onAttachmentClick(message.fileUrl) }
                        )
                        if (message.content.isNotEmpty() &&
                            message.content != "Sent an image" &&
                            message.content != "Sent a document" &&
                            message.content != "Sent a voice message") {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    // Show text content (skip default attachment messages)
                    val shouldShowText = message.content.isNotEmpty() &&
                        (!message.hasAttachment ||
                            (message.content != "Sent an image" &&
                             message.content != "Sent a document" &&
                             message.content != "Sent a voice message"))

                    if (shouldShowText) {
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isFromCurrentUser) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = formatMessageTime(message.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AttachmentPreview(
    fileType: AttachmentType?,
    fileName: String?,
    isFromCurrentUser: Boolean,
    onClick: () -> Unit
) {
    val textColor = if (isFromCurrentUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    when (fileType) {
        AttachmentType.IMAGE -> {
            // Show image placeholder with click to view
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.2f))
                    .clickable { onClick() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = "Image",
                        modifier = Modifier.size(48.dp),
                        tint = textColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Tap to view image",
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor
                    )
                }
            }
        }

        AttachmentType.DOCUMENT -> {
            // Show document with icon and name
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.1f))
                    .clickable { onClick() }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.InsertDriveFile,
                    contentDescription = "Document",
                    modifier = Modifier.size(32.dp),
                    tint = textColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = fileName ?: "Document",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.Download,
                    contentDescription = "Download",
                    modifier = Modifier.size(20.dp),
                    tint = textColor
                )
            }
        }

        AttachmentType.VOICE -> {
            // Show voice message placeholder
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.1f))
                    .clickable { onClick() }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.PlayCircle,
                    contentDescription = "Voice message",
                    modifier = Modifier.size(32.dp),
                    tint = textColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Voice message",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
            }
        }

        null -> {}
    }
}

@Composable
private fun MessageInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachmentClick: () -> Unit,
    isSending: Boolean,
    isUploading: Boolean = false
) {
    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Attachment button
            IconButton(
                onClick = onAttachmentClick,
                enabled = !isSending && !isUploading
            ) {
                Icon(
                    Icons.Default.AttachFile,
                    contentDescription = "Attach file",
                    tint = if (!isSending && !isUploading) PrometheusOrange else Color.Gray
                )
            }

            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text("Type a message...") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                enabled = !isSending && !isUploading
            )

            Spacer(modifier = Modifier.width(8.dp))

            FilledIconButton(
                onClick = onSend,
                enabled = value.isNotBlank() && !isSending && !isUploading,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = PrometheusOrange
                )
            ) {
                if (isSending || isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send"
                    )
                }
            }
        }
    }
}

private fun formatMessageTime(isoTimestamp: String): String {
    return try {
        val instant = Instant.parse(isoTimestamp)
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
            .withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } catch (e: Exception) {
        ""
    }
}
