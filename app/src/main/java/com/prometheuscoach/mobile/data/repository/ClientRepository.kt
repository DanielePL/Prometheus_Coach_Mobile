package com.prometheuscoach.mobile.data.repository

import com.prometheuscoach.mobile.data.model.Client
import com.prometheuscoach.mobile.data.model.CoachClientConnection
import com.prometheuscoach.mobile.data.model.CoachClientView
import com.prometheuscoach.mobile.data.model.ConnectionStatus
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for coach-client operations.
 *
 * IMPORTANT: Uses coach_clients_v view as the single source of truth for coach-client relationships.
 * Never query profiles with coach_id - that column doesn't exist.
 *
 * @see <a href="https://github.com/...">Dev Guidelines</a>
 */
@Singleton
class ClientRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository
) {
    /**
     * Get all clients for the current coach.
     * Uses coach_clients_v view with status='accepted'.
     *
     * @see Prometheus Developer Guidelines v1.0.0
     */
    suspend fun getClients(): Result<List<Client>> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            // Use coach_clients_v view - the single source of truth for coach-client relationships
            val clientViews = supabaseClient.postgrest
                .from("coach_clients_v")
                .select {
                    filter {
                        eq("coach_id", coachId)
                        eq("status", ConnectionStatus.ACCEPTED.value)
                    }
                    order("client_name", Order.ASCENDING)
                }
                .decodeList<CoachClientView>()

            // Map view data to Client model
            val clients = clientViews.map { view ->
                Client(
                    id = view.clientId,
                    fullName = view.clientName,
                    avatarUrl = view.clientAvatar,
                    role = "client",
                    createdAt = view.respondedAt  // Use responded_at as connection date
                )
            }

            Result.success(clients)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get a single client by ID.
     * First verifies the coach has access via coach_clients_v, then fetches full profile.
     */
    suspend fun getClientById(clientId: String): Result<Client> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            // Verify coach has access to this client via the view
            val hasAccess = supabaseClient.postgrest
                .from("coach_clients_v")
                .select {
                    filter {
                        eq("coach_id", coachId)
                        eq("client_id", clientId)
                        eq("status", ConnectionStatus.ACCEPTED.value)
                    }
                }
                .decodeList<CoachClientView>()
                .isNotEmpty()

            if (!hasAccess) {
                return Result.failure(Exception("Client not found or access denied"))
            }

            // Fetch full client profile
            val client = supabaseClient.postgrest
                .from("profiles")
                .select {
                    filter {
                        eq("id", clientId)
                    }
                }
                .decodeSingle<Client>()

            Result.success(client)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get count of accepted clients for the current coach.
     */
    suspend fun getClientCount(): Result<Int> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            val count = supabaseClient.postgrest
                .from("coach_clients_v")
                .select(columns = Columns.raw("count")) {
                    filter {
                        eq("coach_id", coachId)
                        eq("status", ConnectionStatus.ACCEPTED.value)
                    }
                    count(io.github.jan.supabase.postgrest.query.Count.EXACT)
                }

            Result.success(count.countOrNull()?.toInt() ?: 0)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get pending client requests (status='pending').
     */
    suspend fun getPendingRequests(): Result<List<CoachClientView>> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            val pendingClients = supabaseClient.postgrest
                .from("coach_clients_v")
                .select {
                    filter {
                        eq("coach_id", coachId)
                        eq("status", ConnectionStatus.PENDING.value)
                    }
                    order("requested_at", Order.DESCENDING)
                }
                .decodeList<CoachClientView>()

            Result.success(pendingClients)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Invite a client by email.
     * Creates a pending connection in coach_client_connections table.
     */
    suspend fun inviteClientByEmail(email: String): Result<Unit> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            // First, find the user by email in profiles
            val clientProfiles = supabaseClient.postgrest
                .from("profiles")
                .select {
                    filter {
                        eq("email", email.lowercase().trim())
                    }
                }
                .decodeList<Client>()

            if (clientProfiles.isEmpty()) {
                return Result.failure(Exception("No user found with this email. They need to create an account first."))
            }

            val client = clientProfiles.first()

            // Check if connection already exists
            val existingConnections = supabaseClient.postgrest
                .from("coach_client_connections")
                .select {
                    filter {
                        eq("coach_id", coachId)
                        eq("client_id", client.id)
                    }
                }
                .decodeList<CoachClientConnection>()

            if (existingConnections.isNotEmpty()) {
                val existing = existingConnections.first()
                return when (existing.status) {
                    ConnectionStatus.ACCEPTED.value -> Result.failure(Exception("This client is already connected to you."))
                    ConnectionStatus.PENDING.value -> Result.failure(Exception("An invitation is already pending for this client."))
                    else -> Result.failure(Exception("A connection already exists with this client."))
                }
            }

            // Create new pending connection
            supabaseClient.postgrest
                .from("coach_client_connections")
                .insert(
                    mapOf(
                        "coach_id" to coachId,
                        "client_id" to client.id,
                        "status" to ConnectionStatus.PENDING.value,
                        "requested_at" to java.time.Instant.now().toString()
                    )
                )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get coach's invite code for sharing.
     * Clients can use this code to request connection.
     */
    suspend fun getCoachInviteCode(): Result<String> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            // For now, use the coach ID as invite code
            // In production, you'd want a separate shorter code
            Result.success(coachId.take(8).uppercase())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
