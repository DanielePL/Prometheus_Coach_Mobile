package com.prometheuscoach.mobile.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Chat system models based on conversations architecture.
 * @see Prometheus Developer Guidelines v1.0.0
 */

@Serializable
data class Conversation(
    val id: String,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String
)

@Serializable
data class ConversationParticipant(
    val id: String,
    @SerialName("conversation_id")
    val conversationId: String,
    @SerialName("user_id")
    val userId: String,
    val role: String = "member",
    @SerialName("joined_at")
    val joinedAt: String,
    @SerialName("last_read_at")
    val lastReadAt: String? = null
)

@Serializable
data class Message(
    val id: String,
    @SerialName("conversation_id")
    val conversationId: String,
    @SerialName("sender_id")
    val senderId: String,
    val content: String,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("edited_at")
    val editedAt: String? = null,
    @SerialName("read_at")
    val readAt: String? = null
)

/**
 * Response from get_other_participant RPC.
 */
@Serializable
data class OtherParticipant(
    @SerialName("user_id")
    val userId: String,
    @SerialName("full_name")
    val fullName: String,
    @SerialName("avatar_url")
    val avatarUrl: String? = null
)

/**
 * UI model for conversation list item.
 */
data class ConversationWithDetails(
    val conversationId: String,
    val participantId: String,
    val participantName: String,
    val participantAvatar: String?,
    val lastMessage: String?,
    val lastMessageAt: String?,
    val unreadCount: Int = 0
)

/**
 * UI model for message display.
 */
data class MessageWithSender(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val senderAvatar: String?,
    val content: String,
    val createdAt: String,
    val isFromCurrentUser: Boolean,
    val isRead: Boolean
)
