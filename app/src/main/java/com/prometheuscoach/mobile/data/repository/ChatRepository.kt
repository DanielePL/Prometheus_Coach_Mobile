package com.prometheuscoach.mobile.data.repository

import android.util.Log
import com.prometheuscoach.mobile.data.cache.CacheKeys
import com.prometheuscoach.mobile.data.cache.SessionCache
import com.prometheuscoach.mobile.data.model.AttachmentType
import com.prometheuscoach.mobile.data.model.ConversationWithDetails
import com.prometheuscoach.mobile.data.model.Message
import com.prometheuscoach.mobile.data.model.MessageWithSender
import com.prometheuscoach.mobile.data.model.OtherParticipant
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.hours

/**
 * Repository for chat operations.
 * Uses conversations-based model with RPCs for cross-user queries.
 *
 * @see Prometheus Developer Guidelines v1.0.0
 */
@Singleton
class ChatRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository,
    private val cache: SessionCache
) {
    companion object {
        private const val TAG = "ChatRepository"
        private const val ATTACHMENTS_BUCKET = "chat-files"  // Same bucket as Prometheus_V1
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
            // Use decodeList + firstOrNull to handle empty results gracefully
            val participants = supabaseClient.postgrest
                .rpc(
                    "get_other_participant",
                    buildJsonObject { put("conv_id", conversationId) }
                )
                .decodeList<OtherParticipant>()

            val participant = participants.firstOrNull()
                ?: return Result.failure(Exception("No participant found for conversation"))

            Result.success(participant)
        } catch (e: CancellationException) {
            Log.w(TAG, "getOtherParticipant cancelled for $conversationId")
            Result.failure(e) // Let cancellation propagate but return failure gracefully
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get other participant", e)
            Result.failure(e)
        }
    }

    /**
     * Get all conversations for the current user with details.
     * Uses SINGLE RPC call for maximum performance - all data in one query.
     * Results are cached for 2 minutes (shorter TTL for real-time feel).
     */
    suspend fun getConversations(forceRefresh: Boolean = false): Result<List<ConversationWithDetails>> {
        return try {
            // Check cache first (shorter TTL for conversations)
            if (!forceRefresh) {
                cache.get<List<ConversationWithDetails>>(CacheKeys.CONVERSATIONS)?.let {
                    Log.d(TAG, "Returning ${it.size} cached conversations")
                    return Result.success(it)
                }
            }

            Log.d(TAG, "Loading conversations via RPC...")
            val startTime = System.currentTimeMillis()

            val rpcResult = supabaseClient.postgrest
                .rpc("get_coach_conversations")
                .decodeList<ConversationRpcResponse>()

            val conversations = rpcResult.map { row ->
                ConversationWithDetails(
                    conversationId = row.conversationId,
                    participantId = row.participantId ?: "",
                    participantName = row.participantName ?: "Unknown",
                    participantAvatar = row.participantAvatar,
                    lastMessage = row.lastMessage,
                    lastMessageAt = row.lastMessageAt,
                    unreadCount = row.unreadCount?.toInt() ?: 0
                )
            }

            // Cache with shorter TTL (2 minutes) for conversations
            cache.put(CacheKeys.CONVERSATIONS, conversations, 2 * 60 * 1000L)

            val elapsed = System.currentTimeMillis() - startTime
            Log.d(TAG, "Loaded ${conversations.size} conversations in ${elapsed}ms")

            Result.success(conversations)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get conversations via RPC", e)
            Result.success(emptyList())
        }
    }

    /**
     * Invalidate conversations cache. Call after sending a message.
     */
    fun invalidateConversationsCache() {
        cache.invalidate(CacheKeys.CONVERSATIONS)
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

            // Get current user profile - only select full_name to avoid type issues with non-string fields
            val currentUserProfile = supabaseClient.postgrest
                .from("profiles")
                .select(columns = Columns.raw("full_name")) {
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
                    isRead = message.readAt != null,
                    // Attachment fields (V1 compatible)
                    fileUrl = message.fileUrl,
                    fileType = message.fileType?.let { type ->
                        AttachmentType.entries.find { it.value == type }
                    },
                    fileName = message.fileName
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

    // ═══════════════════════════════════════════════════════════════════════════
    // ATTACHMENT FUNCTIONS
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Upload a file attachment and send a message with it.
     * Uses V1-compatible column names (file_url, file_type, file_name).
     *
     * @param conversationId The conversation to send the message to
     * @param content Optional text content (can be empty for attachment-only messages)
     * @param fileBytes The file data as ByteArray
     * @param fileName Original file name
     * @param mimeType The MIME type of the file
     * @param fileSize File size in bytes (not stored in V1, kept for future use)
     */
    suspend fun sendMessageWithAttachment(
        conversationId: String,
        content: String,
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String,
        fileSize: Long
    ): Result<Message> {
        return try {
            val userId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            // 1. Upload file to storage (V1 path: {senderId}/{timestamp}_{filename})
            val fileUrl = uploadAttachment(userId, fileBytes, fileName, mimeType)
                .getOrElse { return Result.failure(it) }

            // 2. Determine file type
            val fileType = AttachmentType.fromMimeType(mimeType)

            // 3. Create message with file (V1 column names)
            val messageData = MessageInsertRequest(
                conversationId = conversationId,
                senderId = userId,
                content = content.ifEmpty { getDefaultAttachmentMessage(fileType) },
                fileUrl = fileUrl,
                fileType = fileType.value,
                fileName = fileName
            )

            val message = supabaseClient.postgrest
                .from("messages")
                .insert(messageData) {
                    select()
                }
                .decodeSingle<Message>()

            Log.d(TAG, "Sent message with file: ${message.id}, type: ${fileType.value}")
            Result.success(message)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message with attachment", e)
            Result.failure(e)
        }
    }

    /**
     * Upload a file to Supabase Storage.
     * V1 compatible path: {senderId}/{timestamp}_{filename}
     */
    private suspend fun uploadAttachment(
        senderId: String,
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String
    ): Result<String> {
        return try {
            // V1 path format: {senderId}/{timestamp}_{originalName}
            val timestamp = System.currentTimeMillis()
            val uniqueFileName = "${timestamp}_$fileName"
            val filePath = "$senderId/$uniqueFileName"

            Log.d(TAG, "Uploading file to: $filePath")

            supabaseClient.storage
                .from(ATTACHMENTS_BUCKET)
                .upload(filePath, fileBytes) {
                    contentType = io.ktor.http.ContentType.parse(mimeType)
                    upsert = false
                }

            Log.d(TAG, "File uploaded successfully: $filePath")
            Result.success(filePath)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload file", e)
            Result.failure(e)
        }
    }

    /**
     * Get a signed URL for downloading/viewing an attachment.
     * URLs are valid for 1 hour.
     */
    suspend fun getAttachmentUrl(filePath: String): Result<String> {
        return try {
            val url = supabaseClient.storage
                .from(ATTACHMENTS_BUCKET)
                .createSignedUrl(filePath, 1.hours)

            Result.success(url)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get attachment URL", e)
            Result.failure(e)
        }
    }

    /**
     * Get default message text for attachment-only messages.
     */
    private fun getDefaultAttachmentMessage(type: AttachmentType): String {
        return when (type) {
            AttachmentType.IMAGE -> "Sent an image"
            AttachmentType.DOCUMENT -> "Sent a document"
            AttachmentType.VOICE -> "Sent a voice message"
        }
    }
}

/**
 * Response from get_coach_conversations RPC.
 */
@Serializable
private data class ConversationRpcResponse(
    @SerialName("conversation_id")
    val conversationId: String,
    @SerialName("participant_id")
    val participantId: String?,
    @SerialName("participant_name")
    val participantName: String?,
    @SerialName("participant_avatar")
    val participantAvatar: String?,
    @SerialName("last_message")
    val lastMessage: String?,
    @SerialName("last_message_at")
    val lastMessageAt: String?,
    @SerialName("unread_count")
    val unreadCount: Long?
)

/**
 * Request body for inserting a message with file attachment.
 * V1 compatible column names.
 */
@Serializable
private data class MessageInsertRequest(
    @SerialName("conversation_id")
    val conversationId: String,
    @SerialName("sender_id")
    val senderId: String,
    val content: String,
    @SerialName("file_url")
    val fileUrl: String? = null,
    @SerialName("file_type")
    val fileType: String? = null,
    @SerialName("file_name")
    val fileName: String? = null
)
