package com.prometheuscoach.mobile.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ═══════════════════════════════════════════════════════════════════════════
// AI COACH ASSISTANT MODELS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Type of AI assistance context.
 */
@Serializable
enum class AIContextType {
    @SerialName("general") GENERAL,                 // General coaching questions
    @SerialName("client_analysis") CLIENT_ANALYSIS, // Analysis of a specific client
    @SerialName("program_design") PROGRAM_DESIGN,   // Program planning assistance
    @SerialName("message_draft") MESSAGE_DRAFT,     // Draft message for client
    @SerialName("workout_review") WORKOUT_REVIEW    // Review/improve a workout
}

/**
 * Role in the AI conversation.
 */
@Serializable
enum class AIRole {
    @SerialName("user") USER,
    @SerialName("assistant") ASSISTANT,
    @SerialName("system") SYSTEM
}

/**
 * A single message in an AI conversation.
 */
@Serializable
data class AIMessage(
    val id: String,
    val role: AIRole,
    val content: String,
    val timestamp: String,
    @SerialName("context_type") val contextType: AIContextType? = null,
    @SerialName("context_data") val contextData: String? = null  // JSON string of context
) {
    val isUser: Boolean get() = role == AIRole.USER
    val isAssistant: Boolean get() = role == AIRole.ASSISTANT
}

/**
 * An AI conversation session.
 */
@Serializable
data class AIConversation(
    val id: String,
    @SerialName("coach_id") val coachId: String,
    @SerialName("context_type") val contextType: AIContextType,
    @SerialName("context_id") val contextId: String? = null,  // clientId or programId
    @SerialName("context_name") val contextName: String? = null,
    val title: String? = null,
    val messages: List<AIMessage> = emptyList(),
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    val displayTitle: String
        get() = title ?: when (contextType) {
            AIContextType.GENERAL -> "General Coaching"
            AIContextType.CLIENT_ANALYSIS -> "Client Analysis: ${contextName ?: "Unknown"}"
            AIContextType.PROGRAM_DESIGN -> "Program Design"
            AIContextType.MESSAGE_DRAFT -> "Message Draft"
            AIContextType.WORKOUT_REVIEW -> "Workout Review"
        }

    val lastMessage: AIMessage?
        get() = messages.lastOrNull()

    val messageCount: Int
        get() = messages.size
}

/**
 * Quick action types for the AI Assistant.
 */
enum class AIQuickAction(
    val title: String,
    val description: String,
    val icon: String,
    val contextType: AIContextType
) {
    CLIENT_INSIGHTS(
        title = "Client Insights",
        description = "Analyze client progress and get recommendations",
        icon = "analytics",
        contextType = AIContextType.CLIENT_ANALYSIS
    ),
    PROGRAM_SUGGESTION(
        title = "Program Design",
        description = "Get AI help designing a training program",
        icon = "fitness_center",
        contextType = AIContextType.PROGRAM_DESIGN
    ),
    DRAFT_MESSAGE(
        title = "Draft Message",
        description = "AI-assisted message to a client",
        icon = "edit_note",
        contextType = AIContextType.MESSAGE_DRAFT
    ),
    WORKOUT_FEEDBACK(
        title = "Workout Review",
        description = "Get feedback on a workout design",
        icon = "rate_review",
        contextType = AIContextType.WORKOUT_REVIEW
    ),
    GENERAL_HELP(
        title = "Coaching Help",
        description = "Ask any coaching-related question",
        icon = "help",
        contextType = AIContextType.GENERAL
    )
}

/**
 * Request to send a message to the AI.
 */
data class AISendMessageRequest(
    val conversationId: String?,
    val message: String,
    val contextType: AIContextType,
    val contextId: String? = null,
    val contextData: Map<String, Any>? = null
)

/**
 * Response from the AI service.
 */
@Serializable
data class AIResponse(
    val message: String,
    @SerialName("conversation_id") val conversationId: String,
    val suggestions: List<String>? = null,
    @SerialName("action_items") val actionItems: List<AIActionItem>? = null
)

/**
 * Action item suggested by AI.
 */
@Serializable
data class AIActionItem(
    val type: String,  // "create_workout", "send_message", "schedule_session", etc.
    val title: String,
    val description: String? = null,
    val data: String? = null  // JSON data for the action
)

/**
 * Client summary for AI context.
 */
@Serializable
data class AIClientContext(
    @SerialName("client_id") val clientId: String,
    @SerialName("client_name") val clientName: String,
    val goals: List<String>? = null,
    @SerialName("recent_workouts") val recentWorkouts: Int? = null,
    @SerialName("current_program") val currentProgram: String? = null,
    @SerialName("notes") val notes: String? = null,
    @SerialName("progress_summary") val progressSummary: String? = null
)

// ═══════════════════════════════════════════════════════════════════════════
// UI STATE
// ═══════════════════════════════════════════════════════════════════════════

/**
 * UI state for the AI Assistant screen.
 */
data class AIAssistantState(
    val isLoading: Boolean = false,
    val error: String? = null,
    // Current conversation
    val currentConversation: AIConversation? = null,
    val messages: List<AIMessage> = emptyList(),
    // Input
    val inputText: String = "",
    val isSending: Boolean = false,
    // Context
    val contextType: AIContextType = AIContextType.GENERAL,
    val contextId: String? = null,
    val contextName: String? = null,
    val clientContext: AIClientContext? = null,
    // History
    val recentConversations: List<AIConversation> = emptyList(),
    // Suggestions
    val suggestions: List<String> = emptyList(),
    val actionItems: List<AIActionItem> = emptyList()
) {
    val hasActiveConversation: Boolean
        get() = currentConversation != null

    val canSend: Boolean
        get() = inputText.isNotBlank() && !isSending
}

/**
 * Predefined prompts for different contexts.
 */
object AIPrompts {
    val clientAnalysisStarters = listOf(
        "How is this client progressing?",
        "What should I focus on with this client?",
        "Suggest improvements for their program",
        "Are there any red flags I should address?"
    )

    val programDesignStarters = listOf(
        "Design a strength program for a beginner",
        "Create a hypertrophy mesocycle",
        "Suggest a deload week structure",
        "How should I periodize for powerlifting?"
    )

    val messageDraftStarters = listOf(
        "Write a motivational check-in message",
        "Draft feedback on their recent progress",
        "Compose a program update announcement",
        "Write a reminder about nutrition goals"
    )

    val generalStarters = listOf(
        "How do I handle a plateau?",
        "Best practices for client retention",
        "Tips for remote coaching",
        "How to structure initial consultations"
    )

    fun getStartersForContext(contextType: AIContextType): List<String> = when (contextType) {
        AIContextType.CLIENT_ANALYSIS -> clientAnalysisStarters
        AIContextType.PROGRAM_DESIGN -> programDesignStarters
        AIContextType.MESSAGE_DRAFT -> messageDraftStarters
        AIContextType.WORKOUT_REVIEW -> programDesignStarters
        AIContextType.GENERAL -> generalStarters
    }
}
