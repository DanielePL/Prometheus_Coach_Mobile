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
    @SerialName("client_id")
    val clientId: String,
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
 * connection_id, coach_id, client_id, status, requested_at, responded_at,
 * created_at, updated_at, client_name, client_avatar, client_email
 *
 * @see Prometheus Developer Guidelines v1.0.0
 */
@Serializable
data class CoachClientView(
    @SerialName("connection_id")
    val connectionId: String,
    @SerialName("coach_id")
    val coachId: String,
    @SerialName("client_id")
    val clientId: String,
    val status: String,  // 'pending', 'accepted', 'declined'
    @SerialName("requested_at")
    val requestedAt: String? = null,
    @SerialName("responded_at")
    val respondedAt: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("client_name")
    val clientName: String,
    @SerialName("client_avatar")
    val clientAvatar: String? = null,
    @SerialName("client_email")
    val clientEmail: String? = null
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
