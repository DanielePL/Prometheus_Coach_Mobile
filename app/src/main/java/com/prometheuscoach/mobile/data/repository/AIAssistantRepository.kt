package com.prometheuscoach.mobile.data.repository

import android.util.Log
import com.prometheuscoach.mobile.BuildConfig
import com.prometheuscoach.mobile.data.model.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AIAssistantRepository"
private const val EDGE_FUNCTION_URL = "${BuildConfig.SUPABASE_URL}/functions/v1/ai-chat"

/**
 * Repository for AI Coach Assistant functionality.
 * Uses Supabase Edge Functions for AI processing.
 */
@Singleton
class AIAssistantRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository,
    private val clientRepository: ClientRepository
) {
    // HTTP client for Edge Function calls
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }
    // ═══════════════════════════════════════════════════════════════════════
    // CONVERSATION MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Get recent AI conversations for the current coach.
     */
    suspend fun getRecentConversations(limit: Int = 20): Result<List<AIConversation>> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            val conversations = supabaseClient.postgrest
                .from("ai_conversations")
                .select {
                    filter { eq("coach_id", coachId) }
                    order("updated_at", Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<AIConversationRecord>()
                .map { it.toConversation() }

            Log.d(TAG, "Loaded ${conversations.size} AI conversations")
            Result.success(conversations)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get conversations", e)
            Result.failure(e)
        }
    }

    /**
     * Get a specific conversation with all messages.
     */
    suspend fun getConversation(conversationId: String): Result<AIConversation> {
        return try {
            val conversation = supabaseClient.postgrest
                .from("ai_conversations")
                .select {
                    filter { eq("id", conversationId) }
                }
                .decodeSingle<AIConversationRecord>()

            val messages = supabaseClient.postgrest
                .from("ai_messages")
                .select {
                    filter { eq("conversation_id", conversationId) }
                    order("timestamp", Order.ASCENDING)
                }
                .decodeList<AIMessage>()

            Result.success(conversation.toConversation().copy(messages = messages))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get conversation", e)
            Result.failure(e)
        }
    }

    /**
     * Create a new conversation.
     */
    suspend fun createConversation(
        contextType: AIContextType,
        contextId: String? = null,
        contextName: String? = null,
        title: String? = null
    ): Result<AIConversation> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            val conversationId = UUID.randomUUID().toString()
            val now = Instant.now().toString()

            val record = AIConversationInsert(
                id = conversationId,
                coachId = coachId,
                contextType = contextType,
                contextId = contextId,
                contextName = contextName,
                title = title,
                createdAt = now,
                updatedAt = now
            )

            supabaseClient.postgrest
                .from("ai_conversations")
                .insert(record)

            val conversation = AIConversation(
                id = conversationId,
                coachId = coachId,
                contextType = contextType,
                contextId = contextId,
                contextName = contextName,
                title = title,
                createdAt = now,
                updatedAt = now
            )

            Log.d(TAG, "Created AI conversation: $conversationId")
            Result.success(conversation)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create conversation", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a conversation and its messages.
     */
    suspend fun deleteConversation(conversationId: String): Result<Unit> {
        return try {
            // Delete messages first
            supabaseClient.postgrest
                .from("ai_messages")
                .delete {
                    filter { eq("conversation_id", conversationId) }
                }

            // Delete conversation
            supabaseClient.postgrest
                .from("ai_conversations")
                .delete {
                    filter { eq("id", conversationId) }
                }

            Log.d(TAG, "Deleted conversation: $conversationId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete conversation", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // AI MESSAGE HANDLING
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Send a message to the AI and get a response.
     * Uses Supabase Edge Function for AI processing.
     */
    suspend fun sendMessage(
        conversationId: String?,
        message: String,
        contextType: AIContextType,
        contextId: String? = null
    ): Result<AIResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val coachId = authRepository.getCurrentUserId()
                    ?: return@withContext Result.failure(Exception("Not authenticated"))

                // Create conversation if needed
                val activeConversationId = conversationId ?: run {
                    val newConvo = createConversation(contextType, contextId).getOrThrow()
                    newConvo.id
                }

                // Save user message
                val userMessageId = UUID.randomUUID().toString()
                val now = Instant.now().toString()

                val userMessage = AIMessageInsert(
                    id = userMessageId,
                    conversationId = activeConversationId,
                    role = AIRole.USER,
                    content = message,
                    timestamp = now
                )

                supabaseClient.postgrest
                    .from("ai_messages")
                    .insert(userMessage)

                // Get conversation history for context
                val conversationHistory = getConversationHistory(activeConversationId)

                // Get client context if available
                val clientContext = contextId?.let { getClientContext(it) }

                // Call Edge Function for AI response
                val aiResponse = callEdgeFunction(
                    message = message,
                    contextType = contextType,
                    conversationHistory = conversationHistory,
                    clientContext = clientContext,
                    conversationId = activeConversationId
                )

                // Save assistant message
                val assistantMessageId = UUID.randomUUID().toString()
                val assistantMessage = AIMessageInsert(
                    id = assistantMessageId,
                    conversationId = activeConversationId,
                    role = AIRole.ASSISTANT,
                    content = aiResponse.message,
                    timestamp = Instant.now().toString()
                )

                supabaseClient.postgrest
                    .from("ai_messages")
                    .insert(assistantMessage)

                // Update conversation timestamp
                supabaseClient.postgrest
                    .from("ai_conversations")
                    .update({
                        set("updated_at", Instant.now().toString())
                    }) {
                        filter { eq("id", activeConversationId) }
                    }

                Log.d(TAG, "AI response received for conversation: $activeConversationId")

                Result.success(aiResponse)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send message to AI", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Get conversation history for context.
     */
    private suspend fun getConversationHistory(conversationId: String): List<EdgeFunctionMessage> {
        return try {
            val messages = supabaseClient.postgrest
                .from("ai_messages")
                .select {
                    filter { eq("conversation_id", conversationId) }
                    order("timestamp", Order.ASCENDING)
                }
                .decodeList<AIMessage>()
                .takeLast(10) // Keep last 10 messages
                .map { msg ->
                    EdgeFunctionMessage(
                        role = if (msg.role == AIRole.USER) "user" else "assistant",
                        content = msg.content
                    )
                }
            messages
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get conversation history", e)
            emptyList()
        }
    }

    /**
     * Get client context for AI enrichment.
     */
    private suspend fun getClientContext(clientId: String): EdgeFunctionClientContext? {
        return try {
            val client = clientRepository.getClientById(clientId).getOrNull()
            client?.let {
                EdgeFunctionClientContext(
                    clientName = it.fullName,
                    goals = null, // TODO: Add when goals are implemented in Client model
                    progressSummary = null // Could be enriched with actual progress data
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get client context", e)
            null
        }
    }

    /**
     * Call the Supabase Edge Function for AI processing.
     */
    private suspend fun callEdgeFunction(
        message: String,
        contextType: AIContextType,
        conversationHistory: List<EdgeFunctionMessage>,
        clientContext: EdgeFunctionClientContext?,
        conversationId: String
    ): AIResponse {
        return try {
            // Get auth token for Edge Function authorization
            val accessToken = supabaseClient.auth.currentAccessTokenOrNull()

            val request = EdgeFunctionRequest(
                message = message,
                contextType = contextType.toEdgeFunctionType(),
                conversationHistory = conversationHistory.takeIf { it.isNotEmpty() },
                clientContext = clientContext
            )

            Log.d(TAG, "Calling Edge Function with context: ${contextType.name}")

            val response = httpClient.post(EDGE_FUNCTION_URL) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $accessToken")
                header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                setBody(request)
            }

            if (response.status.isSuccess()) {
                val edgeResponse = response.body<EdgeFunctionResponse>()
                Log.d(TAG, "Edge Function response received, tokens: ${edgeResponse.usage?.inputTokens}/${edgeResponse.usage?.outputTokens}")

                AIResponse(
                    message = edgeResponse.message,
                    conversationId = conversationId,
                    suggestions = edgeResponse.suggestions ?: emptyList()
                )
            } else {
                Log.e(TAG, "Edge Function error: ${response.status}")
                // Fall back to static response on error
                createFallbackResponse(conversationId, message, contextType)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Edge Function call failed", e)
            // Fall back to static response on network/parsing errors
            createFallbackResponse(conversationId, message, contextType)
        }
    }

    /**
     * Convert AIContextType to Edge Function string format.
     */
    private fun AIContextType.toEdgeFunctionType(): String = when (this) {
        AIContextType.GENERAL -> "general"
        AIContextType.CLIENT_ANALYSIS -> "client_analysis"
        AIContextType.PROGRAM_DESIGN -> "program_design"
        AIContextType.MESSAGE_DRAFT -> "message_draft"
        AIContextType.WORKOUT_REVIEW -> "workout_review"
    }

    /**
     * Create a fallback response when AI service is unavailable.
     */
    private fun createFallbackResponse(
        conversationId: String,
        userMessage: String,
        contextType: AIContextType
    ): AIResponse {
        val fallbackMessage = when (contextType) {
            AIContextType.CLIENT_ANALYSIS ->
                "I'd be happy to help analyze your client's progress. To provide accurate insights, I'll need access to their workout history and goals. In the meantime, consider reviewing their recent workout completion rate and any feedback they've provided."

            AIContextType.PROGRAM_DESIGN ->
                "I can help you design an effective training program. Consider these key factors: the client's experience level, available training days, equipment access, and primary goals. A well-structured program typically includes progressive overload, adequate recovery, and exercise variety."

            AIContextType.MESSAGE_DRAFT ->
                "Here's a template you can customize:\n\n\"Hi [Name],\n\nI wanted to check in on your progress this week. How are you feeling about your workouts? Remember, consistency is key to reaching your goals.\n\nLet me know if you have any questions!\n\nBest,\n[Your Name]\""

            AIContextType.WORKOUT_REVIEW ->
                "When reviewing a workout, consider: exercise selection and order, volume (sets × reps), intensity (% of 1RM or RPE), rest periods, and how it fits into the weekly training structure. Make sure compound movements come before isolation work."

            AIContextType.GENERAL ->
                "I'm here to help with your coaching questions. While my full capabilities are currently limited, I can still provide general guidance on training principles, client communication, and program design. What specific aspect would you like to explore?"
        }

        return AIResponse(
            message = fallbackMessage,
            conversationId = conversationId,
            suggestions = listOf(
                "Tell me more about the client's goals",
                "What's their training experience?",
                "Any specific challenges to address?"
            )
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // QUICK ACTIONS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Generate client insights.
     */
    suspend fun generateClientInsights(clientId: String): Result<AIResponse> {
        val client = clientRepository.getClientById(clientId).getOrNull()
        val clientName = client?.fullName ?: "this client"

        return sendMessage(
            conversationId = null,
            message = "Provide a comprehensive analysis of $clientName's training progress, including strengths, areas for improvement, and specific recommendations.",
            contextType = AIContextType.CLIENT_ANALYSIS,
            contextId = clientId
        )
    }

    /**
     * Generate program suggestion.
     */
    suspend fun generateProgramSuggestion(
        clientId: String?,
        goals: String,
        constraints: String?
    ): Result<AIResponse> {
        val message = buildString {
            append("Design a training program with these parameters:\n")
            append("Goals: $goals\n")
            constraints?.let { append("Constraints: $it\n") }
            append("\nProvide a structured weekly plan with exercise selection, sets, reps, and progression scheme.")
        }

        return sendMessage(
            conversationId = null,
            message = message,
            contextType = AIContextType.PROGRAM_DESIGN,
            contextId = clientId
        )
    }

    /**
     * Draft a message for a client.
     */
    suspend fun draftClientMessage(
        clientId: String,
        topic: String
    ): Result<AIResponse> {
        val client = clientRepository.getClientById(clientId).getOrNull()
        val clientName = client?.fullName ?: "the client"

        return sendMessage(
            conversationId = null,
            message = "Draft a professional and encouraging message to $clientName about: $topic",
            contextType = AIContextType.MESSAGE_DRAFT,
            contextId = clientId
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// DATA TRANSFER OBJECTS
// ═══════════════════════════════════════════════════════════════════════════

@Serializable
private data class AIConversationRecord(
    val id: String,
    @SerialName("coach_id") val coachId: String,
    @SerialName("context_type") val contextType: AIContextType,
    @SerialName("context_id") val contextId: String? = null,
    @SerialName("context_name") val contextName: String? = null,
    val title: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    fun toConversation() = AIConversation(
        id = id,
        coachId = coachId,
        contextType = contextType,
        contextId = contextId,
        contextName = contextName,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

@Serializable
private data class AIConversationInsert(
    val id: String,
    @SerialName("coach_id") val coachId: String,
    @SerialName("context_type") val contextType: AIContextType,
    @SerialName("context_id") val contextId: String? = null,
    @SerialName("context_name") val contextName: String? = null,
    val title: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
private data class AIMessageInsert(
    val id: String,
    @SerialName("conversation_id") val conversationId: String,
    val role: AIRole,
    val content: String,
    val timestamp: String
)

// ═══════════════════════════════════════════════════════════════════════════
// EDGE FUNCTION DTOs
// ═══════════════════════════════════════════════════════════════════════════

@Serializable
private data class EdgeFunctionMessage(
    val role: String,
    val content: String
)

@Serializable
private data class EdgeFunctionClientContext(
    @SerialName("client_name") val clientName: String? = null,
    val goals: List<String>? = null,
    @SerialName("recent_workouts") val recentWorkouts: Int? = null,
    @SerialName("progress_summary") val progressSummary: String? = null
)

@Serializable
private data class EdgeFunctionRequest(
    val message: String,
    @SerialName("context_type") val contextType: String,
    @SerialName("conversation_history") val conversationHistory: List<EdgeFunctionMessage>? = null,
    @SerialName("client_context") val clientContext: EdgeFunctionClientContext? = null
)

@Serializable
private data class EdgeFunctionResponse(
    val message: String,
    val suggestions: List<String>? = null,
    val usage: EdgeFunctionUsage? = null,
    val error: String? = null
)

@Serializable
private data class EdgeFunctionUsage(
    @SerialName("input_tokens") val inputTokens: Int? = null,
    @SerialName("output_tokens") val outputTokens: Int? = null
)
