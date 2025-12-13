package com.prometheuscoach.mobile.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prometheuscoach.mobile.data.model.ConversationWithDetails
import com.prometheuscoach.mobile.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConversationsState(
    val conversations: List<ConversationWithDetails> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ConversationsViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ConversationsState())
    val state: StateFlow<ConversationsState> = _state.asStateFlow()

    init {
        loadConversations()
    }

    fun loadConversations() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            chatRepository.getConversations()
                .onSuccess { conversations ->
                    _state.value = _state.value.copy(
                        conversations = conversations,
                        isLoading = false
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load conversations"
                    )
                }
        }
    }

    fun refresh() {
        loadConversations()
    }

    /**
     * Start a new conversation with a user.
     * Returns the conversation ID.
     */
    suspend fun startConversation(userId: String): Result<String> {
        return chatRepository.findOrCreateConversation(userId)
    }
}
