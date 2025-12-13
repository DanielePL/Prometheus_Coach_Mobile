package com.prometheuscoach.mobile.data.repository

import android.util.Log
import com.prometheuscoach.mobile.data.model.ConversationParticipant
import com.prometheuscoach.mobile.data.model.ConversationWithDetails
import com.prometheuscoach.mobile.data.model.Message
import com.prometheuscoach.mobile.data.model.MessageWithSender
import com.prometheuscoach.mobile.data.model.OtherParticipant
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for chat operations.
 * Uses conversations-based model with RPCs for cross-user queries.
 *
 * @see Prometheus Developer Guidelines v1.0.0
 */
@Singleton
class ChatRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository
) {
    companion object {
        private const val TAG = "ChatRepository"
    }

    /**
     * Find or create a 1:1 conversation with another user.
     * Uses find_or_create_conversation RPC.
     */
    suspend fun findOrCreateConversation(targetUserId: String): Result<String> {
        return try {
            val conversationId = supabaseClient.postgrest
                .rpc(
                    "find_or_create_conversation",
                    buildJsonObject { put("target_user_id", targetUserId) }
                )
                .decodeAs<String>()

            Log.d(TAG, "Found/created conversation: $conversationId with user: $targetUserId")
            Result.success(conversationId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to find/create conversation", e)
            Result.failure(e)
        }
    }

    /**
     * Get the other participant in a conversation.
     * Uses get_other_participant RPC.
     */
    suspend fun getOtherParticipant(conversationId: String): Result<OtherParticipant> {
        return try {
            val participant = supabaseClient.postgrest
                .rpc(
                    "get_other_participant",
                    buildJsonObject { put("conv_id", conversationId) }
                )
                .decodeSingle<OtherParticipant>()

            Result.success(participant)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get other participant", e)
            Result.failure(e)
        }
    }

    /**
     * Get all conversations for the current user with details.
     */
    suspend fun getConversations(): Result<List<ConversationWithDetails>> {
        return try {
            val userId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            // Get conversations where user is participant
            val participantRecords = try {
                supabaseClient.postgrest
                    .from("conversation_participants")
                    .select {
                        filter { eq("user_id", userId) }
                    }
                    .decodeList<ConversationParticipant>()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to get participant records, returning empty list", e)
                return Result.success(emptyList())
            }

            val conversationIds = participantRecords.map { it.conversationId }

            if (conversationIds.isEmpty()) {
                return Result.success(emptyList())
            }

            // Get details for each conversation
            val conversations = conversationIds.mapNotNull { convId ->
                try {
                    val otherParticipant = getOtherParticipant(convId).getOrNull()

                    // Get last message
                    val lastMessage = try {
                        supabaseClient.postgrest
                            .from("messages")
                            .select {
                                filter { eq("conversation_id", convId) }
                                order("created_at", Order.DESCENDING)
                                limit(1)
                            }
                            .decodeList<Message>()
                            .firstOrNull()
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to get last message for $convId", e)
                        null
                    }

                    ConversationWithDetails(
                        conversationId = convId,
                        participantId = otherParticipant?.userId ?: "",
                        participantName = otherParticipant?.fullName ?: "Unknown",
                        participantAvatar = otherParticipant?.avatarUrl,
                        lastMessage = lastMessage?.content,
                        lastMessageAt = lastMessage?.createdAt,
                        unreadCount = 0 // TODO: Calculate unread count
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to get conversation details for $convId", e)
                    null
                }
            }.sortedByDescending { it.lastMessageAt }

            Result.success(conversations)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get conversations", e)
            Result.failure(e)
        }
    }

    /**
     * Get messages for a conversation.
     */
    suspend fun getMessages(conversationId: String): Result<List<MessageWithSender>> {
        return try {
            val userId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            val messages = supabaseClient.postgrest
                .from("messages")
                .select {
                    filter { eq("conversation_id", conversationId) }
                    order("created_at", Order.ASCENDING)
                }
                .decodeList<Message>()

            // Get other participant info for sender names
            val otherParticipant = getOtherParticipant(conversationId).getOrNull()

            // Get current user profile
            val currentUserProfile = supabaseClient.postgrest
                .from("profiles")
                .select {
                    filter { eq("id", userId) }
                }
                .decodeSingleOrNull<Map<String, String?>>()

            val currentUserName = currentUserProfile?.get("full_name") ?: "You"

            val messagesWithSender = messages.map { message ->
                val isFromCurrentUser = message.senderId == userId
                MessageWithSender(
                    id = message.id,
                    conversationId = message.conversationId,
                    senderId = message.senderId,
                    senderName = if (isFromCurrentUser) currentUserName else (otherParticipant?.fullName ?: "Unknown"),
                    senderAvatar = if (isFromCurrentUser) null else otherParticipant?.avatarUrl,
                    content = message.content,
                    createdAt = message.createdAt,
                    isFromCurrentUser = isFromCurrentUser,
                    isRead = message.readAt != null
                )
            }

            Result.success(messagesWithSender)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get messages", e)
            Result.failure(e)
        }
    }

    /**
     * Send a message to a conversation.
     */
    suspend fun sendMessage(conversationId: String, content: String): Result<Message> {
        return try {
            val userId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            val message = supabaseClient.postgrest
                .from("messages")
                .insert(
                    mapOf(
                        "conversation_id" to conversationId,
                        "sender_id" to userId,
                        "content" to content
                    )
                ) {
                    select()
                }
                .decodeSingle<Message>()

            Log.d(TAG, "Sent message: ${message.id}")
            Result.success(message)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message", e)
            Result.failure(e)
        }
    }

    /**
     * Mark messages as read.
     */
    suspend fun markMessagesAsRead(conversationId: String): Result<Unit> {
        return try {
            val userId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            // Update last_read_at in conversation_participants
            supabaseClient.postgrest
                .from("conversation_participants")
                .update(
                    mapOf("last_read_at" to java.time.Instant.now().toString())
                ) {
                    filter {
                        eq("conversation_id", conversationId)
                        eq("user_id", userId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark messages as read", e)
            Result.failure(e)
        }
    }

    // Note: Realtime subscription will be implemented when needed.
    // For now, use polling or manual refresh for message updates.
}
