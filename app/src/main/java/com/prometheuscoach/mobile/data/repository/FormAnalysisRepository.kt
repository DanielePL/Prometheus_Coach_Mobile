package com.prometheuscoach.mobile.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.prometheuscoach.mobile.data.model.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.upload
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "FormAnalysisRepository"
private const val STORAGE_BUCKET = "form-analysis-videos"
private const val THUMBNAIL_BUCKET = "form-analysis-thumbnails"

/**
 * Repository for Form Analysis video management.
 * Handles video upload to Supabase Storage and metadata in Postgres.
 */
@Singleton
class FormAnalysisRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) {
    // Upload progress state
    private val _uploadProgress = MutableStateFlow(0f)
    val uploadProgress: StateFlow<Float> = _uploadProgress.asStateFlow()

    // ═══════════════════════════════════════════════════════════════════════
    // VIDEO CRUD OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Get all form analysis videos for a client.
     */
    suspend fun getClientVideos(clientId: String): Result<List<FormAnalysisVideo>> {
        return try {
            val videos = supabaseClient.postgrest
                .from("form_analysis_videos")
                .select {
                    filter { eq("client_id", clientId) }
                    order("uploaded_at", Order.DESCENDING)
                }
                .decodeList<FormAnalysisVideo>()

            Log.d(TAG, "Loaded ${videos.size} videos for client $clientId")
            Result.success(videos)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get client videos", e)
            Result.failure(e)
        }
    }

    /**
     * Get a single video by ID.
     */
    suspend fun getVideo(videoId: String): Result<FormAnalysisVideo> {
        return try {
            val video = supabaseClient.postgrest
                .from("form_analysis_videos")
                .select {
                    filter { eq("id", videoId) }
                }
                .decodeSingle<FormAnalysisVideo>()

            Result.success(video)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get video $videoId", e)
            Result.failure(e)
        }
    }

    /**
     * Upload a video for a client.
     * Returns the created FormAnalysisVideo on success.
     */
    suspend fun uploadVideo(
        clientId: String,
        videoUri: Uri,
        request: UploadVideoRequest
    ): Result<FormAnalysisVideo> {
        return withContext(Dispatchers.IO) {
            try {
                _uploadProgress.value = 0f

                val coachId = authRepository.getCurrentUserId()
                    ?: return@withContext Result.failure(Exception("Not authenticated"))

                // Generate unique filename
                val videoId = UUID.randomUUID().toString()
                val timestamp = System.currentTimeMillis()
                val fileName = "videos/$coachId/$clientId/${videoId}_$timestamp.mp4"

                Log.d(TAG, "Uploading video: $fileName")
                _uploadProgress.value = 0.1f

                // Read video file
                val inputStream = context.contentResolver.openInputStream(videoUri)
                    ?: return@withContext Result.failure(Exception("Cannot read video file"))

                val videoBytes = inputStream.use { it.readBytes() }
                val fileSize = videoBytes.size.toLong()

                _uploadProgress.value = 0.3f

                // Upload to Supabase Storage
                val bucket = supabaseClient.storage.from(STORAGE_BUCKET)
                bucket.upload(fileName, videoBytes) {
                    upsert = false
                }

                _uploadProgress.value = 0.7f

                // Get public URL
                val videoUrl = bucket.publicUrl(fileName)

                _uploadProgress.value = 0.8f

                // Create database record
                val videoRecord = FormAnalysisVideoInsert(
                    id = videoId,
                    coachId = coachId,
                    clientId = clientId,
                    exerciseId = request.exerciseId,
                    exerciseName = request.exerciseName,
                    videoUrl = videoUrl,
                    fileSizeBytes = fileSize,
                    title = request.title,
                    notes = request.notes,
                    status = AnalysisStatus.PENDING,
                    uploadedAt = Instant.now().toString()
                )

                supabaseClient.postgrest
                    .from("form_analysis_videos")
                    .insert(videoRecord)

                _uploadProgress.value = 1f

                // Fetch the created video
                val result = getVideo(videoId)
                Log.d(TAG, "Video uploaded successfully: $videoId")

                result
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload video", e)
                _uploadProgress.value = 0f
                Result.failure(e)
            }
        }
    }

    /**
     * Add feedback to a video (marks it as reviewed).
     */
    suspend fun addFeedback(
        videoId: String,
        feedback: String,
        rating: Int? = null
    ): Result<FormAnalysisVideo> {
        return try {
            supabaseClient.postgrest
                .from("form_analysis_videos")
                .update({
                    set("feedback", feedback)
                    rating?.let { set("rating", it) }
                    set("status", AnalysisStatus.REVIEWED.name.lowercase())
                    set("reviewed_at", Instant.now().toString())
                }) {
                    filter { eq("id", videoId) }
                }

            Log.d(TAG, "Feedback added to video $videoId")
            getVideo(videoId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add feedback", e)
            Result.failure(e)
        }
    }

    /**
     * Update video status (e.g., archive).
     */
    suspend fun updateStatus(videoId: String, status: AnalysisStatus): Result<Unit> {
        return try {
            supabaseClient.postgrest
                .from("form_analysis_videos")
                .update({
                    set("status", status.name.lowercase())
                }) {
                    filter { eq("id", videoId) }
                }

            Log.d(TAG, "Updated video $videoId status to $status")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update status", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a video (removes from storage and database).
     */
    suspend fun deleteVideo(videoId: String): Result<Unit> {
        return try {
            // Get video to find the storage path
            val video = getVideo(videoId).getOrNull()
                ?: return Result.failure(Exception("Video not found"))

            // Delete from storage
            try {
                val path = extractStoragePath(video.videoUrl)
                if (path != null) {
                    supabaseClient.storage.from(STORAGE_BUCKET).delete(path)
                }
                // Delete thumbnail if exists
                video.thumbnailUrl?.let { thumbUrl ->
                    extractStoragePath(thumbUrl)?.let { thumbPath ->
                        supabaseClient.storage.from(THUMBNAIL_BUCKET).delete(thumbPath)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete storage files", e)
                // Continue with database deletion even if storage fails
            }

            // Delete from database
            supabaseClient.postgrest
                .from("form_analysis_videos")
                .delete {
                    filter { eq("id", videoId) }
                }

            Log.d(TAG, "Deleted video $videoId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete video", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TIMESTAMP MARKERS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Get timestamps for a video.
     */
    suspend fun getVideoTimestamps(videoId: String): Result<List<VideoTimestamp>> {
        return try {
            val timestamps = supabaseClient.postgrest
                .from("video_timestamps")
                .select {
                    filter { eq("video_id", videoId) }
                    order("timestamp_seconds", Order.ASCENDING)
                }
                .decodeList<VideoTimestamp>()

            Result.success(timestamps)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get timestamps", e)
            Result.failure(e)
        }
    }

    /**
     * Add a timestamp marker to a video.
     */
    suspend fun addTimestamp(
        videoId: String,
        timestampSeconds: Double,
        label: String,
        note: String? = null,
        markerType: MarkerType = MarkerType.NOTE
    ): Result<VideoTimestamp> {
        return try {
            val timestamp = VideoTimestampInsert(
                id = UUID.randomUUID().toString(),
                videoId = videoId,
                timestampSeconds = timestampSeconds,
                label = label,
                note = note,
                markerType = markerType
            )

            supabaseClient.postgrest
                .from("video_timestamps")
                .insert(timestamp)

            val result = supabaseClient.postgrest
                .from("video_timestamps")
                .select {
                    filter { eq("id", timestamp.id) }
                }
                .decodeSingle<VideoTimestamp>()

            Log.d(TAG, "Added timestamp at ${timestampSeconds}s")
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add timestamp", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a timestamp marker.
     */
    suspend fun deleteTimestamp(timestampId: String): Result<Unit> {
        return try {
            supabaseClient.postgrest
                .from("video_timestamps")
                .delete {
                    filter { eq("id", timestampId) }
                }

            Log.d(TAG, "Deleted timestamp $timestampId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete timestamp", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Extract storage path from public URL.
     */
    private fun extractStoragePath(url: String): String? {
        return try {
            // URL format: https://xxx.supabase.co/storage/v1/object/public/bucket/path
            val parts = url.split("/object/public/$STORAGE_BUCKET/")
            if (parts.size == 2) parts[1] else null
        } catch (e: Exception) {
            null
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// INSERT DTOs
// ═══════════════════════════════════════════════════════════════════════════

@Serializable
private data class FormAnalysisVideoInsert(
    val id: String,
    @SerialName("coach_id") val coachId: String,
    @SerialName("client_id") val clientId: String,
    @SerialName("exercise_id") val exerciseId: String? = null,
    @SerialName("exercise_name") val exerciseName: String? = null,
    @SerialName("video_url") val videoUrl: String,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerialName("duration_seconds") val durationSeconds: Int? = null,
    @SerialName("file_size_bytes") val fileSizeBytes: Long? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("notes") val notes: String? = null,
    @SerialName("status") val status: AnalysisStatus,
    @SerialName("uploaded_at") val uploadedAt: String
)

@Serializable
private data class VideoTimestampInsert(
    val id: String,
    @SerialName("video_id") val videoId: String,
    @SerialName("timestamp_seconds") val timestampSeconds: Double,
    @SerialName("label") val label: String,
    @SerialName("note") val note: String? = null,
    @SerialName("marker_type") val markerType: MarkerType
)
