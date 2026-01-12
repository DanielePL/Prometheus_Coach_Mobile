package com.prometheuscoach.mobile.ui.screens.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prometheuscoach.mobile.data.model.*
import com.prometheuscoach.mobile.data.repository.AIAssistantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AIAssistantViewModel @Inject constructor(
    private val aiRepository: AIAssistantRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AIAssistantState())
    val state: StateFlow<AIAssistantState> = _state.asStateFlow()

    init {
        loadRecentConversations()
    }

    /**
     * Load recent conversations.
     */
    private fun loadRecentConversations() {
        viewModelScope.launch {
            aiRepository.getRecentConversations()
                .onSuccess { conversations ->
                    _state.update { it.copy(recentConversations = conversations) }
                }
        }
    }

    /**
     * Initialize with a specific context (e.g., from client detail screen).
     */
    fun initWithContext(
        contextType: AIContextType,
        contextId: String? = null,
        contextName: String? = null
    ) {
        _state.update {
            it.copy(
                contextType = contextType,
                contextId = contextId,
                contextName = contextName,
                messages = emptyList(),
                currentConversation = null,
                suggestions = AIPrompts.getStartersForContext(contextType)
            )
        }
    }

    /**
     * Load an existing conversation.
     */
    fun loadConversation(conversationId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            aiRepository.getConversation(conversationId)
                .onSuccess { conversation ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            currentConversation = conversation,
                            messages = conversation.messages,
                            contextType = conversation.contextType,
                            contextId = conversation.contextId,
                            contextName = conversation.contextName,
                            suggestions = emptyList()
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = e.message
                        )
                    }
                }
        }
    }

    /**
     * Start a new conversation.
     */
    fun startNewConversation(contextType: AIContextType = AIContextType.GENERAL) {
        _state.update {
            it.copy(
                currentConversation = null,
                messages = emptyList(),
                contextType = contextType,
                contextId = null,
                contextName = null,
                suggestions = AIPrompts.getStartersForContext(contextType),
                actionItems = emptyList()
            )
        }
    }

    /**
     * Update input text.
     */
    fun updateInputText(text: String) {
        _state.update { it.copy(inputText = text) }
    }

    /**
     * Send a message.
     */
    fun sendMessage(message: String? = null) {
        val messageToSend = message ?: _state.value.inputText
        if (messageToSend.isBlank()) return

        viewModelScope.launch {
            val currentState = _state.value

            // Add user message to UI immediately
            val userMessage = AIMessage(
                id = UUID.randomUUID().toString(),
                role = AIRole.USER,
                content = messageToSend,
                timestamp = Instant.now().toString()
            )

            _state.update {
                it.copy(
                    messages = it.messages + userMessage,
                    inputText = "",
                    isSending = true,
                    suggestions = emptyList(),
                    error = null
                )
            }

            // Send to AI
            aiRepository.sendMessage(
                conversationId = currentState.currentConversation?.id,
                message = messageToSend,
                contextType = currentState.contextType,
                contextId = currentState.contextId
            )
                .onSuccess { response ->
                    // Add assistant message
                    val assistantMessage = AIMessage(
                        id = UUID.randomUUID().toString(),
                        role = AIRole.ASSISTANT,
                        content = response.message,
                        timestamp = Instant.now().toString()
                    )

                    _state.update {
                        it.copy(
                            messages = it.messages + assistantMessage,
                            isSending = false,
                            currentConversation = it.currentConversation?.copy(
                                id = response.conversationId
                            ) ?: AIConversation(
                                id = response.conversationId,
                                coachId = "",
                                contextType = currentState.contextType,
                                contextId = currentState.contextId,
                                contextName = currentState.contextName,
                                createdAt = Instant.now().toString()
                            ),
                            suggestions = response.suggestions ?: emptyList(),
                            actionItems = response.actionItems ?: emptyList()
                        )
                    }

                    // Refresh recent conversations
                    loadRecentConversations()
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isSending = false,
                            error = e.message ?: "Failed to get AI response"
                        )
                    }
                }
        }
    }

    /**
     * Use a suggestion as input.
     */
    fun useSuggestion(suggestion: String) {
        sendMessage(suggestion)
    }

    /**
     * Generate client insights.
     */
    fun generateClientInsights(clientId: String, clientName: String) {
        _state.update {
            it.copy(
                contextType = AIContextType.CLIENT_ANALYSIS,
                contextId = clientId,
                contextName = clientName,
                messages = emptyList(),
                currentConversation = null
            )
        }

        viewModelScope.launch {
            _state.update { it.copy(isSending = true) }

            aiRepository.generateClientInsights(clientId)
                .onSuccess { response ->
                    val assistantMessage = AIMessage(
                        id = UUID.randomUUID().toString(),
                        role = AIRole.ASSISTANT,
                        content = response.message,
                        timestamp = Instant.now().toString()
                    )

                    _state.update {
                        it.copy(
                            messages = listOf(assistantMessage),
                            isSending = false,
                            currentConversation = AIConversation(
                                id = response.conversationId,
                                coachId = "",
                                contextType = AIContextType.CLIENT_ANALYSIS,
                                contextId = clientId,
                                contextName = clientName,
                                createdAt = Instant.now().toString()
                            ),
                            suggestions = response.suggestions ?: emptyList()
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isSending = false,
                            error = e.message
                        )
                    }
                }
        }
    }

    /**
     * Draft a message for a client.
     */
    fun draftClientMessage(clientId: String, clientName: String, topic: String) {
        _state.update {
            it.copy(
                contextType = AIContextType.MESSAGE_DRAFT,
                contextId = clientId,
                contextName = clientName,
                messages = emptyList(),
                currentConversation = null
            )
        }

        viewModelScope.launch {
            _state.update { it.copy(isSending = true) }

            aiRepository.draftClientMessage(clientId, topic)
                .onSuccess { response ->
                    val assistantMessage = AIMessage(
                        id = UUID.randomUUID().toString(),
                        role = AIRole.ASSISTANT,
                        content = response.message,
                        timestamp = Instant.now().toString()
                    )

                    _state.update {
                        it.copy(
                            messages = listOf(assistantMessage),
                            isSending = false,
                            suggestions = response.suggestions ?: emptyList()
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isSending = false,
                            error = e.message
                        )
                    }
                }
        }
    }

    /**
     * Delete a conversation.
     */
    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            aiRepository.deleteConversation(conversationId)
                .onSuccess {
                    _state.update { state ->
                        state.copy(
                            recentConversations = state.recentConversations.filter { it.id != conversationId },
                            currentConversation = if (state.currentConversation?.id == conversationId) null else state.currentConversation,
                            messages = if (state.currentConversation?.id == conversationId) emptyList() else state.messages
                        )
                    }
                }
        }
    }

    /**
     * Clear error.
     */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    /**
     * Set context type.
     */
    fun setContextType(contextType: AIContextType) {
        _state.update {
            it.copy(
                contextType = contextType,
                suggestions = AIPrompts.getStartersForContext(contextType)
            )
        }
    }
}
