package com.prometheuscoach.mobile.data.repository

import com.prometheuscoach.mobile.data.model.Client
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClientRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository
) {
    suspend fun getClients(): Result<List<Client>> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            val clients = supabaseClient.postgrest
                .from("profiles")
                .select {
                    filter {
                        eq("coach_id", coachId)
                        eq("role", "client")
                    }
                }
                .decodeList<Client>()

            Result.success(clients)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getClientById(clientId: String): Result<Client> {
        return try {
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

    suspend fun getClientCount(): Result<Int> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            val count = supabaseClient.postgrest
                .from("profiles")
                .select(columns = Columns.raw("count")) {
                    filter {
                        eq("coach_id", coachId)
                        eq("role", "client")
                    }
                    count(io.github.jan.supabase.postgrest.query.Count.EXACT)
                }

            Result.success(count.countOrNull()?.toInt() ?: 0)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
