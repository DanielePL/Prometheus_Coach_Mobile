package com.prometheuscoach.mobile.data.repository

import android.util.Log
import com.prometheuscoach.mobile.data.model.CoachSetCard
import com.prometheuscoach.mobile.data.model.UpdateCoachProfileRequest
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class UserProfile(
    val id: String,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val email: String? = null,
    val role: String? = null,
    // Coach profile fields
    val bio: String? = null,
    val specialization: String? = null,
    @SerialName("instagram_handle") val instagramHandle: String? = null,
    @SerialName("tiktok_handle") val tiktokHandle: String? = null,
    @SerialName("youtube_handle") val youtubeHandle: String? = null,
    @SerialName("twitter_handle") val twitterHandle: String? = null,
    @SerialName("website_url") val websiteUrl: String? = null,
    @SerialName("years_experience") val yearsExperience: Int? = null,
    @SerialName("is_verified") val isVerified: Boolean = false
) {
    val userId: String get() = id

    fun toCoachSetCard(): CoachSetCard = CoachSetCard(
        id = id,
        fullName = fullName,
        avatarUrl = avatarUrl,
        bio = bio,
        specialization = specialization,
        instagramHandle = instagramHandle,
        tiktokHandle = tiktokHandle,
        youtubeHandle = youtubeHandle,
        twitterHandle = twitterHandle,
        websiteUrl = websiteUrl,
        yearsExperience = yearsExperience,
        isVerified = isVerified
    )
}

@Singleton
class AuthRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    val sessionStatus: Flow<SessionStatus> = supabaseClient.auth.sessionStatus

    val isAuthenticated: Flow<Boolean> = sessionStatus.map { status ->
        status is SessionStatus.Authenticated
    }

    suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            supabaseClient.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUp(email: String, password: String): Result<Unit> {
        return try {
            supabaseClient.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut(): Result<Unit> {
        return try {
            supabaseClient.auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            supabaseClient.auth.resetPasswordForEmail(email)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCurrentUserId(): String? {
        return supabaseClient.auth.currentUserOrNull()?.id
    }

    suspend fun handleAuthCallback(uri: String): Result<Unit> {
        return try {
            // Parse the URI and extract the token/code if present
            // Supabase handles the session automatically when we parse the URL
            val url = android.net.Uri.parse(uri)

            // Check for access_token in fragment (implicit flow)
            val fragment = url.fragment
            if (fragment != null && fragment.contains("access_token")) {
                // Parse fragment parameters
                val params = fragment.split("&").associate {
                    val parts = it.split("=")
                    if (parts.size == 2) parts[0] to parts[1] else "" to ""
                }
                val accessToken = params["access_token"]
                val refreshToken = params["refresh_token"]

                if (accessToken != null && refreshToken != null) {
                    supabaseClient.auth.importAuthToken(accessToken)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentUserProfile(): Result<UserProfile> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            val profile = supabaseClient.postgrest
                .from("profiles")
                .select {
                    filter { eq("id", userId) }
                }
                .decodeSingle<UserProfile>()

            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upload a profile avatar image to Supabase Storage.
     * @param imageBytes The image data as ByteArray
     * @param contentType The MIME type (e.g., "image/jpeg")
     * @return The public URL of the uploaded image
     */
    suspend fun uploadProfileAvatar(imageBytes: ByteArray, contentType: String): Result<String> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            val extension = when (contentType) {
                "image/jpeg" -> "jpg"
                "image/png" -> "png"
                "image/webp" -> "webp"
                else -> "jpg"
            }

            val fileName = "avatar_${userId}_${System.currentTimeMillis()}.$extension"
            val path = "avatars/$fileName"

            Log.d("AuthRepository", "Uploading avatar: $path")

            // Upload to storage
            supabaseClient.storage
                .from("avatars")
                .upload(path, imageBytes) {
                    upsert = true
                }

            // Get public URL
            val publicUrl = supabaseClient.storage
                .from("avatars")
                .publicUrl(path)

            Log.d("AuthRepository", "Avatar uploaded: $publicUrl")

            // Update profile with new avatar URL
            updateProfileAvatar(publicUrl)

            Result.success(publicUrl)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to upload avatar", e)
            Result.failure(e)
        }
    }

    /**
     * Update the avatar_url in the user's profile.
     */
    suspend fun updateProfileAvatar(avatarUrl: String): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            supabaseClient.postgrest
                .from("profiles")
                .update(mapOf("avatar_url" to avatarUrl)) {
                    filter { eq("id", userId) }
                }

            Log.d("AuthRepository", "Profile avatar updated: $avatarUrl")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to update profile avatar", e)
            Result.failure(e)
        }
    }

    /**
     * Update the user's profile name.
     */
    suspend fun updateProfileName(name: String): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            supabaseClient.postgrest
                .from("profiles")
                .update(mapOf("full_name" to name)) {
                    filter { eq("id", userId) }
                }

            Log.d("AuthRepository", "Profile name updated: $name")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to update profile name", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // COACH PROFILE (PUBLIC SETCARD)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Get a coach's public profile by user ID.
     * This is used to display the coach's SetCard to other users.
     */
    suspend fun getCoachSetCard(coachId: String): Result<CoachSetCard> {
        return try {
            val profile = supabaseClient.postgrest
                .from("profiles")
                .select {
                    filter { eq("id", coachId) }
                }
                .decodeSingle<UserProfile>()

            Result.success(profile.toCoachSetCard())
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to get coach profile: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get the current user's coach profile.
     */
    suspend fun getCurrentCoachSetCard(): Result<CoachSetCard> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            getCoachSetCard(userId)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to get current coach profile", e)
            Result.failure(e)
        }
    }

    /**
     * Update the current user's coach profile (SetCard data).
     */
    suspend fun updateCoachProfile(request: UpdateCoachProfileRequest): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            // Use the serializable request object directly
            supabaseClient.postgrest
                .from("profiles")
                .update(request) {
                    filter { eq("id", userId) }
                }

            Log.d("AuthRepository", "Coach profile updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to update coach profile: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Update a single string field in the coach profile.
     */
    suspend fun updateCoachProfileField(fieldName: String, value: String?): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            supabaseClient.postgrest
                .from("profiles")
                .update(mapOf(fieldName to value)) {
                    filter { eq("id", userId) }
                }

            Log.d("AuthRepository", "Coach profile field '$fieldName' updated")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to update coach profile field: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Update a single integer field in the coach profile.
     */
    suspend fun updateCoachProfileIntField(fieldName: String, value: Int?): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            supabaseClient.postgrest
                .from("profiles")
                .update(mapOf(fieldName to value)) {
                    filter { eq("id", userId) }
                }

            Log.d("AuthRepository", "Coach profile field '$fieldName' updated")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to update coach profile field: ${e.message}", e)
            Result.failure(e)
        }
    }
}
