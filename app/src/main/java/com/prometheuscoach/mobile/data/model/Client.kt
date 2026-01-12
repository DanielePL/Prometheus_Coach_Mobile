package com.prometheuscoach.mobile.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Client(
    val id: String,
    @SerialName("full_name")
    val fullName: String,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    val role: String = "client",
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("is_admin")
    val isAdmin: Boolean = false,
    @SerialName("preferred_timezone")
    val preferredTimezone: String? = null
)

@Serializable
data class CoachClientConnection(
    val id: String,
    @SerialName("coach_id")
    val coachId: String,
    @SerialName("user_id")
    val userId: String,
    val status: String,
    @SerialName("chat_enabled")
    val chatEnabled: Boolean? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("requested_at")
    val requestedAt: String? = null,
    @SerialName("responded_at")
    val respondedAt: String? = null
)

@Serializable
data class ClientWithConnection(
    val id: String,
    @SerialName("full_name")
    val fullName: String,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    val role: String = "client",
    @SerialName("created_at")
    val createdAt: String? = null
)

/**
 * Model for coach_clients_v view.
 * This is the single source of truth for coach-client relationships.
 *
 * View columns:
 * connection_id, coach_id, user_id, status, requested_at, responded_at,
 * created_at, updated_at, user_name, user_avatar
 *
 * @see Prometheus Developer Guidelines v1.0.0
 */
@Serializable
data class CoachClientView(
    @SerialName("connection_id")
    val connectionId: String,
    @SerialName("coach_id")
    val coachId: String,
    @SerialName("user_id")
    val clientId: String,  // Keep property name for backwards compat, but maps to user_id
    val status: String,  // 'pending', 'accepted', 'declined'
    @SerialName("requested_at")
    val requestedAt: String? = null,
    @SerialName("responded_at")
    val respondedAt: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("user_name")
    val clientName: String,  // Keep property name for backwards compat, but maps to user_name
    @SerialName("user_avatar")
    val clientAvatar: String? = null  // Keep property name for backwards compat, but maps to user_avatar
)

/**
 * Connection status enum matching database values.
 * @see Prometheus Developer Guidelines v1.0.0
 */
enum class ConnectionStatus(val value: String) {
    PENDING("pending"),
    ACCEPTED("accepted"),
    DECLINED("declined")
}

/**
 * Coach profile model for invite code functionality.
 * Maps to the profiles table with coach-specific fields.
 */
@Serializable
data class CoachProfile(
    val id: String,
    @SerialName("full_name")
    val fullName: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
    @SerialName("invite_code")
    val inviteCode: String? = null,
    val role: String? = null
)

/**
 * Request model for updating client profile data.
 * Only non-null fields will be updated.
 */
@Serializable
data class UpdateClientRequest(
    @SerialName("full_name")
    val fullName: String? = null,
    @SerialName("preferred_timezone")
    val preferredTimezone: String? = null
)
