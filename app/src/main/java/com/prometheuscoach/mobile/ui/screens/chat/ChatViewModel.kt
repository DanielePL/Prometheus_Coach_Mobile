package com.prometheuscoach.mobile.ui.screens.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prometheuscoach.mobile.data.model.MessageWithSender
import com.prometheuscoach.mobile.data.model.OtherParticipant
import com.prometheuscoach.mobile.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatState(
    val conversationId: String = "",
    val otherParticipant: OtherParticipant? = null,
    val messages: List<MessageWithSender> = emptyList(),
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val isUploadingAttachment: Boolean = false,
    val uploadProgress: Float = 0f,
    val error: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    private val _messageInput = MutableStateFlow("")
    val messageInput: StateFlow<String> = _messageInput.asStateFlow()

    init {
        // Get conversationId from navigation arguments
        savedStateHandle.get<String>("conversationId")?.let { convId ->
            loadChat(convId)
        }
    }

    fun loadChat(conversationId: String) {
        _state.value = _state.value.copy(
            conversationId = conversationId,
            isLoading = true,
            error = null
        )

        viewModelScope.launch {
            // Load other participant info
            chatRepository.getOtherParticipant(conversationId)
                .onSuccess { participant ->
                    _state.value = _state.value.copy(otherParticipant = participant)
                }

            // Load messages
            loadMessages(conversationId)

            // Mark as read
            chatRepository.markMessagesAsRead(conversationId)
        }
    }

    private suspend fun loadMessages(conversationId: String) {
        chatRepository.getMessages(conversationId)
            .onSuccess { messages ->
                _state.value = _state.value.copy(
                    messages = messages,
                    isLoading = false
                )
            }
            .onFailure { error ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = error.message ?: "Failed to load messages"
                )
            }
    }

    /**
     * Refresh messages manually. Call this to poll for new messages.
     */
    fun refreshMessages() {
        val conversationId = _state.value.conversationId
        if (conversationId.isEmpty()) return

        viewModelScope.launch {
            loadMessages(conversationId)
            chatRepository.markMessagesAsRead(conversationId)
        }
    }

    fun updateMessageInput(text: String) {
        _messageInput.value = text
    }

    fun sendMessage() {
        val content = _messageInput.value.trim()
        if (content.isEmpty()) return

        val conversationId = _state.value.conversationId
        if (conversationId.isEmpty()) return

        viewModelScope.launch {
            _state.value = _state.value.copy(isSending = true)
            _messageInput.value = "" // Clear input immediately for better UX

            chatRepository.sendMessage(conversationId, content)
                .onSuccess {
                    _state.value = _state.value.copy(isSending = false)
                    // Messages will be updated via realtime subscription
                    // But also reload to ensure consistency
                    loadMessages(conversationId)
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isSending = false,
                        error = error.message ?: "Failed to send message"
                    )
                    // Restore message input on failure
                    _messageInput.value = content
                }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ATTACHMENT FUNCTIONS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Send a message with an attachment (image or document).
     */
    fun sendMessageWithAttachment(
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String,
        fileSize: Long,
        caption: String = ""
    ) {
        val conversationId = _state.value.conversationId
        if (conversationId.isEmpty()) return

        viewModelScope.launch {
            _state.value = _state.value.copy(
                isUploadingAttachment = true,
                isSending = true
            )

            chatRepository.sendMessageWithAttachment(
                conversationId = conversationId,
                content = caption,
                fileBytes = fileBytes,
                fileName = fileName,
                mimeType = mimeType,
                fileSize = fileSize
            )
                .onSuccess {
                    _state.value = _state.value.copy(
                        isUploadingAttachment = false,
                        isSending = false
                    )
                    loadMessages(conversationId)
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isUploadingAttachment = false,
                        isSending = false,
                        error = error.message ?: "Failed to send attachment"
                    )
                }
        }
    }

    /**
     * Get a signed URL for viewing/downloading an attachment.
     */
    suspend fun getAttachmentUrl(filePath: String): String? {
        return chatRepository.getAttachmentUrl(filePath).getOrNull()
    }
}
