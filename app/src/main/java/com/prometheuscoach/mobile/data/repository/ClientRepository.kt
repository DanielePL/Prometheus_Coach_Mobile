package com.prometheuscoach.mobile.data.repository

import android.util.Log
import com.prometheuscoach.mobile.data.cache.CacheKeys
import com.prometheuscoach.mobile.data.cache.SessionCache
import com.prometheuscoach.mobile.data.model.Client
import com.prometheuscoach.mobile.data.model.CoachClientConnection
import com.prometheuscoach.mobile.data.model.CoachClientView
import com.prometheuscoach.mobile.data.model.CoachProfile
import com.prometheuscoach.mobile.data.model.ConnectionStatus
import com.prometheuscoach.mobile.data.model.UpdateClientRequest
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
    private val authRepository: AuthRepository,
    private val cache: SessionCache
) {
    /**
     * Get all clients for the current coach.
     * Uses coach_clients_v view with status='accepted'.
     * Results are cached for 5 minutes.
     *
     * @see Prometheus Developer Guidelines v1.0.0
     */
    suspend fun getClients(forceRefresh: Boolean = false): Result<List<Client>> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            // Check cache first (unless forced refresh)
            if (!forceRefresh) {
                cache.get<List<Client>>(CacheKeys.CLIENTS)?.let {
                    return Result.success(it)
                }
            }

            // Use coach_clients_v view - the single source of truth for coach-client relationships
            val clientViews = supabaseClient.postgrest
                .from("coach_clients_v")
                .select {
                    filter {
                        eq("coach_id", coachId)
                        eq("status", ConnectionStatus.ACCEPTED.value)
                    }
                    order("user_name", Order.ASCENDING)
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

            // Cache the result
            cache.put(CacheKeys.CLIENTS, clients)

            Result.success(clients)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Invalidate clients cache. Call after adding/removing clients.
     */
    fun invalidateClientsCache() {
        cache.invalidate(CacheKeys.CLIENTS)
    }

    /**
     * Get a single client by ID.
     * Uses coach_clients_v view which includes user_avatar from user_profiles.
     * Falls back to profiles table for additional data if needed.
     */
    suspend fun getClientById(clientId: String): Result<Client> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            // Get client data from view (includes proper avatar from user_profiles)
            val clientViews = supabaseClient.postgrest
                .from("coach_clients_v")
                .select {
                    filter {
                        eq("coach_id", coachId)
                        eq("user_id", clientId)
                        eq("status", ConnectionStatus.ACCEPTED.value)
                    }
                }
                .decodeList<CoachClientView>()

            if (clientViews.isEmpty()) {
                return Result.failure(Exception("Client not found or access denied"))
            }

            val clientView = clientViews.first()
            Log.d("ClientRepository", "View data - clientId: ${clientView.clientId}, name: ${clientView.clientName}, avatar: ${clientView.clientAvatar}")

            // Fetch additional profile data (timezone, etc.) from profiles
            val profileData = try {
                supabaseClient.postgrest
                    .from("profiles")
                    .select {
                        filter {
                            eq("id", clientId)
                        }
                    }
                    .decodeSingleOrNull<Client>()
            } catch (e: Exception) {
                Log.w("ClientRepository", "Could not fetch additional profile data", e)
                null
            }

            // Combine view data (with correct avatar) with profile data
            val client = Client(
                id = clientView.clientId,
                fullName = clientView.clientName,
                avatarUrl = clientView.clientAvatar,  // Use avatar from view (includes user_profiles)
                role = profileData?.role ?: "client",
                createdAt = clientView.respondedAt ?: clientView.createdAt,
                updatedAt = clientView.updatedAt,
                isAdmin = profileData?.isAdmin ?: false,
                preferredTimezone = profileData?.preferredTimezone
            )

            Log.d("ClientRepository", "Loaded client ${client.fullName} with avatar: ${client.avatarUrl?.take(50)}...")
            Result.success(client)
        } catch (e: Exception) {
            Log.e("ClientRepository", "Failed to load client $clientId", e)
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
                        eq("user_id", client.id)
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
                        "user_id" to client.id,
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
     * Retrieves existing code from profiles.invite_code, or generates and saves a new one.
     *
     * Code format: 6 uppercase alphanumeric characters (e.g., "A3B7X9")
     * Excludes confusing characters: 0, O, I, 1
     */
    suspend fun getCoachInviteCode(): Result<String> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            // Try to get existing invite code from profile
            val profile = supabaseClient.postgrest
                .from("profiles")
                .select {
                    filter { eq("id", coachId) }
                }
                .decodeSingleOrNull<CoachProfile>()

            // If profile has an invite code, return it
            if (!profile?.inviteCode.isNullOrBlank()) {
                Log.d("ClientRepository", "Using existing invite code: ${profile?.inviteCode}")
                return Result.success(profile!!.inviteCode!!)
            }

            // Generate a new unique invite code
            val newCode = generateUniqueInviteCode()
            Log.d("ClientRepository", "Generated new invite code: $newCode")

            // Save the new invite code to the profile
            supabaseClient.postgrest
                .from("profiles")
                .update(mapOf("invite_code" to newCode)) {
                    filter { eq("id", coachId) }
                }

            Result.success(newCode)
        } catch (e: Exception) {
            Log.e("ClientRepository", "Failed to get/create invite code", e)
            Result.failure(e)
        }
    }

    /**
     * Generate a unique 6-character alphanumeric invite code.
     * Excludes confusing characters: 0, O, I, 1
     */
    private suspend fun generateUniqueInviteCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        var attempts = 0
        val maxAttempts = 10

        while (attempts < maxAttempts) {
            val code = (1..6)
                .map { chars.random() }
                .joinToString("")

            // Check if code already exists
            val existing = supabaseClient.postgrest
                .from("profiles")
                .select {
                    filter { eq("invite_code", code) }
                }
                .decodeList<CoachProfile>()

            if (existing.isEmpty()) {
                return code
            }
            attempts++
        }

        // Fallback: use timestamp-based code
        return System.currentTimeMillis().toString(36).takeLast(6).uppercase()
    }

    /**
     * Update a client's profile data.
     * Only updates fields that are non-null in the request.
     * First verifies the coach has access to this client.
     *
     * @param clientId The ID of the client to update
     * @param request The update request containing fields to change
     * @return Result indicating success or failure
     */
    suspend fun updateClient(clientId: String, request: UpdateClientRequest): Result<Unit> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            // Verify coach has access to this client
            val hasAccess = supabaseClient.postgrest
                .from("coach_clients_v")
                .select {
                    filter {
                        eq("coach_id", coachId)
                        eq("user_id", clientId)
                        eq("status", ConnectionStatus.ACCEPTED.value)
                    }
                }
                .decodeList<CoachClientView>()
                .isNotEmpty()

            if (!hasAccess) {
                return Result.failure(Exception("Client not found or access denied"))
            }

            // Build update map with only non-null fields
            val updateMap = mutableMapOf<String, Any>()
            request.fullName?.let { updateMap["full_name"] = it }
            request.preferredTimezone?.let { updateMap["preferred_timezone"] = it }

            if (updateMap.isEmpty()) {
                return Result.success(Unit) // Nothing to update
            }

            // Update the client's profile
            supabaseClient.postgrest
                .from("profiles")
                .update(updateMap) {
                    filter { eq("id", clientId) }
                }

            Log.d("ClientRepository", "Successfully updated client $clientId: $updateMap")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ClientRepository", "Failed to update client $clientId", e)
            Result.failure(e)
        }
    }
}
