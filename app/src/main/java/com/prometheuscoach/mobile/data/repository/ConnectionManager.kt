package com.prometheuscoach.mobile.data.repository

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ConnectionManager"
private const val REALTIME_CHANNEL = "coach-client-connections"
private const val POLL_INTERVAL_MS = 30_000L
private const val RECONNECT_DELAY_MS = 3_000L
private const val MAX_RECONNECT_ATTEMPTS = 2

/**
 * Unified Connection Manager for Coach-Client relationships.
 *
 * Features:
 * - Uses PostgreSQL RPC functions for all operations (unified API)
 * - Realtime subscription for instant updates
 * - Automatic fallback to polling if Realtime fails
 * - Reconnection logic with exponential backoff
 *
 * This pattern should be replicated across all 5 apps (2 Android, 2 iOS, 1 React)
 */
@Singleton
class ConnectionManager @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _connections = MutableStateFlow<List<Connection>>(emptyList())
    val connections: StateFlow<List<Connection>> = _connections.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _userRole = MutableStateFlow<String?>(null)
    val userRole: StateFlow<String?> = _userRole.asStateFlow()

    private var realtimeChannel: RealtimeChannel? = null
    private var pollingJob: Job? = null
    private var isRealtimeActive = false
    private var reconnectAttempts = 0

    // =========================================================================
    // PUBLIC API - Use these methods from ViewModels
    // =========================================================================

    /**
     * Connect to a coach using their invite code.
     * Used by CLIENT apps only.
     */
    suspend fun connectByInviteCode(code: String): ConnectionResult {
        return try {
            Log.d(TAG, "Connecting with invite code: $code")

            val response = supabaseClient.postgrest.rpc(
                function = "connect_by_invite_code",
                parameters = buildJsonObject {
                    put("p_invite_code", code.trim().uppercase())
                }
            ).decodeAs<JsonObject>()

            if (response["success"]?.jsonPrimitive?.boolean == true) {
                // Refresh connections after successful request
                refreshConnections()

                ConnectionResult.Success(
                    connectionId = response["connection_id"]?.jsonPrimitive?.contentOrNull,
                    coachName = response["coach_name"]?.jsonPrimitive?.contentOrNull,
                    message = response["message"]?.jsonPrimitive?.contentOrNull
                )
            } else {
                ConnectionResult.Error(
                    code = response["error"]?.jsonPrimitive?.contentOrNull ?: "UNKNOWN",
                    message = response["message"]?.jsonPrimitive?.contentOrNull ?: "Unknown error"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect by invite code", e)
            ConnectionResult.Error("NETWORK_ERROR", e.message ?: "Network error occurred")
        }
    }

    /**
     * Preview coach info before connecting.
     * Used by CLIENT apps only.
     */
    suspend fun getCoachByInviteCode(code: String): CoachPreviewResult {
        return try {
            val response = supabaseClient.postgrest.rpc(
                function = "get_coach_by_invite_code",
                parameters = buildJsonObject {
                    put("p_invite_code", code.trim().uppercase())
                }
            ).decodeAs<JsonObject>()

            if (response["success"]?.jsonPrimitive?.boolean == true) {
                val coach = response["coach"]?.jsonObject
                CoachPreviewResult.Success(
                    CoachPreview(
                        id = coach?.get("id")?.jsonPrimitive?.contentOrNull ?: "",
                        name = coach?.get("name")?.jsonPrimitive?.contentOrNull ?: "",
                        avatarUrl = coach?.get("avatar_url")?.jsonPrimitive?.contentOrNull,
                        bio = coach?.get("bio")?.jsonPrimitive?.contentOrNull
                    )
                )
            } else {
                CoachPreviewResult.NotFound
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get coach by invite code", e)
            CoachPreviewResult.Error(e.message ?: "Network error")
        }
    }

    /**
     * Accept or decline a connection request.
     * Used by COACH apps only.
     */
    suspend fun respondToConnection(connectionId: String, accept: Boolean): Result<String> {
        return try {
            val currentUserId = supabaseClient.auth.currentUserOrNull()?.id
            Log.d(TAG, "🔄 Responding to connection:")
            Log.d(TAG, "   - Connection ID: $connectionId")
            Log.d(TAG, "   - Accept: $accept")
            Log.d(TAG, "   - Current User: $currentUserId")

            val response = supabaseClient.postgrest.rpc(
                function = "respond_to_connection",
                parameters = buildJsonObject {
                    put("p_connection_id", connectionId)
                    put("p_accept", accept)
                }
            ).decodeAs<JsonObject>()

            Log.d(TAG, "📥 RPC Response: $response")

            if (response["success"]?.jsonPrimitive?.boolean == true) {
                // Refresh connections after response
                refreshConnections()

                val status = response["status"]?.jsonPrimitive?.contentOrNull ?: "unknown"
                Log.d(TAG, "✅ Success! New status: $status")
                Result.success(status)
            } else {
                val error = response["error"]?.jsonPrimitive?.contentOrNull ?: "UNKNOWN"
                val message = response["message"]?.jsonPrimitive?.contentOrNull ?: "Failed"
                Log.e(TAG, "❌ RPC Failed: $error - $message")
                Result.failure(Exception(message))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception in respondToConnection", e)
            Result.failure(e)
        }
    }

    /**
     * Disconnect from a coach or client.
     * Used by ALL apps.
     */
    suspend fun disconnect(connectionId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Disconnecting: $connectionId")

            val response = supabaseClient.postgrest.rpc(
                function = "disconnect_connection",
                parameters = buildJsonObject {
                    put("p_connection_id", connectionId)
                }
            ).decodeAs<JsonObject>()

            if (response["success"]?.jsonPrimitive?.boolean == true) {
                refreshConnections()
                Result.success(Unit)
            } else {
                Result.failure(Exception(response["message"]?.jsonPrimitive?.contentOrNull ?: "Failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to disconnect", e)
            Result.failure(e)
        }
    }

    /**
     * Get current connections using RPC function.
     */
    suspend fun refreshConnections(): Result<List<Connection>> {
        return try {
            _connectionState.value = ConnectionState.Loading

            val response = supabaseClient.postgrest.rpc("get_my_connections")
                .decodeAs<JsonObject>()

            if (response["success"]?.jsonPrimitive?.boolean == true) {
                _userRole.value = response["role"]?.jsonPrimitive?.contentOrNull

                val connectionsList = response["connections"]?.jsonArray?.mapNotNull { element ->
                    try {
                        val obj = element.jsonObject
                        Connection(
                            connectionId = obj["connection_id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null,
                            userId = obj["user_id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null,
                            userName = obj["user_name"]?.jsonPrimitive?.contentOrNull ?: "Unknown",
                            userAvatar = obj["user_avatar"]?.jsonPrimitive?.contentOrNull,
                            status = obj["status"]?.jsonPrimitive?.contentOrNull ?: "unknown",
                            role = obj["role"]?.jsonPrimitive?.contentOrNull ?: "unknown",
                            requestedAt = obj["requested_at"]?.jsonPrimitive?.contentOrNull,
                            respondedAt = obj["responded_at"]?.jsonPrimitive?.contentOrNull
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse connection", e)
                        null
                    }
                } ?: emptyList()

                _connections.value = connectionsList
                _connectionState.value = ConnectionState.Connected

                Log.d(TAG, "Loaded ${connectionsList.size} connections")
                Result.success(connectionsList)
            } else {
                _connectionState.value = ConnectionState.Error("Failed to load connections")
                Result.failure(Exception("Failed to load connections"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh connections", e)
            _connectionState.value = ConnectionState.Error(e.message ?: "Network error")
            Result.failure(e)
        }
    }

    // =========================================================================
    // REALTIME SUBSCRIPTION WITH FALLBACK
    // =========================================================================

    /**
     * Start listening for connection changes.
     * Uses Realtime if available, falls back to polling.
     */
    fun startListening() {
        val userId = supabaseClient.auth.currentUserOrNull()?.id ?: return

        scope.launch {
            // Initial fetch
            refreshConnections()

            // Try to establish Realtime connection
            tryStartRealtime(userId)
        }
    }

    private suspend fun tryStartRealtime(userId: String) {
        try {
            Log.d(TAG, "Starting Realtime subscription for user: $userId")
            _connectionState.value = ConnectionState.Connecting

            val channel = supabaseClient.realtime.channel(REALTIME_CHANNEL)
            realtimeChannel = channel

            // Listen for changes to coach_client_connections
            val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "coach_client_connections"
            }

            changeFlow.onEach { action ->
                Log.d(TAG, "Realtime change received: ${action::class.simpleName}")

                // Check if this change affects the current user
                val recordUserId = when (action) {
                    is PostgresAction.Insert -> action.record["coach_id"]?.jsonPrimitive?.contentOrNull
                        ?: action.record["client_id"]?.jsonPrimitive?.contentOrNull
                    is PostgresAction.Update -> action.record["coach_id"]?.jsonPrimitive?.contentOrNull
                        ?: action.record["client_id"]?.jsonPrimitive?.contentOrNull
                    is PostgresAction.Delete -> action.oldRecord["coach_id"]?.jsonPrimitive?.contentOrNull
                        ?: action.oldRecord["client_id"]?.jsonPrimitive?.contentOrNull
                    else -> null
                }

                if (recordUserId == userId) {
                    // Refresh connections when our data changes
                    refreshConnections()
                }
            }.launchIn(scope)

            channel.subscribe()
            isRealtimeActive = true
            reconnectAttempts = 0
            _connectionState.value = ConnectionState.Connected

            Log.d(TAG, "Realtime subscription started successfully")

            // Stop polling if it was running
            stopPolling()

        } catch (e: Exception) {
            Log.e(TAG, "Realtime subscription failed, starting polling fallback", e)
            isRealtimeActive = false

            // Start polling as fallback
            startPolling()

            // Schedule reconnect attempt
            scheduleReconnect(userId)
        }
    }

    private fun startPolling() {
        if (pollingJob?.isActive == true) return

        Log.d(TAG, "Starting polling fallback (interval: ${POLL_INTERVAL_MS}ms)")
        _connectionState.value = ConnectionState.Polling

        pollingJob = scope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                refreshConnections()
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        Log.d(TAG, "Polling stopped")
    }

    private fun scheduleReconnect(userId: String) {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            Log.w(TAG, "Max reconnect attempts reached, staying on polling")
            return
        }

        reconnectAttempts++
        val delay = RECONNECT_DELAY_MS * reconnectAttempts

        scope.launch {
            Log.d(TAG, "Scheduling Realtime reconnect in ${delay}ms (attempt $reconnectAttempts)")
            delay(delay)

            if (!isRealtimeActive) {
                tryStartRealtime(userId)
            }
        }
    }

    /**
     * Stop all listeners and clean up.
     */
    fun stopListening() {
        scope.launch {
            try {
                realtimeChannel?.let { channel ->
                    supabaseClient.realtime.removeChannel(channel)
                    realtimeChannel = null
                }
                isRealtimeActive = false
                stopPolling()
                _connectionState.value = ConnectionState.Idle
                Log.d(TAG, "All listeners stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping listeners", e)
            }
        }
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    /**
     * Get pending connection requests (for Coach).
     */
    fun getPendingConnections(): List<Connection> {
        return _connections.value.filter { it.status == "pending" }
    }

    /**
     * Get accepted connections.
     */
    fun getAcceptedConnections(): List<Connection> {
        return _connections.value.filter { it.status == "accepted" }
    }
}

// =========================================================================
// DATA CLASSES
// =========================================================================

@Serializable
data class Connection(
    val connectionId: String,
    val userId: String,
    val userName: String,
    val userAvatar: String?,
    val status: String,  // "pending", "accepted", "declined"
    val role: String,    // "coach" or "client"
    val requestedAt: String?,
    val respondedAt: String?
)

data class CoachPreview(
    val id: String,
    val name: String,
    val avatarUrl: String?,
    val bio: String?
)

sealed class ConnectionResult {
    data class Success(
        val connectionId: String?,
        val coachName: String?,
        val message: String? = null
    ) : ConnectionResult()

    data class Error(
        val code: String,
        val message: String
    ) : ConnectionResult()
}

sealed class CoachPreviewResult {
    data class Success(val coach: CoachPreview) : CoachPreviewResult()
    object NotFound : CoachPreviewResult()
    data class Error(val message: String) : CoachPreviewResult()
}

sealed class ConnectionState {
    object Idle : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    object Polling : ConnectionState()
    object Loading : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}
