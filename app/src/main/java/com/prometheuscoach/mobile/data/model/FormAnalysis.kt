package com.prometheuscoach.mobile.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ═══════════════════════════════════════════════════════════════════════════
// FORM ANALYSIS VIDEO MODELS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Status of a form analysis video.
 */
@Serializable
enum class AnalysisStatus {
    @SerialName("pending") PENDING,       // Uploaded, awaiting review
    @SerialName("reviewed") REVIEWED,     // Coach has reviewed and provided feedback
    @SerialName("archived") ARCHIVED      // Archived/hidden from active view
}

/**
 * A video uploaded for form analysis.
 * Coach uploads videos of client exercises to review and provide feedback.
 */
@Serializable
data class FormAnalysisVideo(
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
    @SerialName("notes") val notes: String? = null,           // Coach notes when uploading
    @SerialName("feedback") val feedback: String? = null,     // Coach feedback after review
    @SerialName("rating") val rating: Int? = null,            // 1-5 form rating
    @SerialName("status") val status: AnalysisStatus = AnalysisStatus.PENDING,
    @SerialName("uploaded_at") val uploadedAt: String,
    @SerialName("reviewed_at") val reviewedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null
) {
    /** Display title (fallback to exercise name or generic) */
    val displayTitle: String
        get() = title ?: exerciseName ?: "Form Video"

    /** Whether this video has been reviewed */
    val isReviewed: Boolean
        get() = status == AnalysisStatus.REVIEWED

    /** Whether this video is pending review */
    val isPending: Boolean
        get() = status == AnalysisStatus.PENDING

    /** Formatted duration */
    val formattedDuration: String
        get() {
            val seconds = durationSeconds ?: return ""
            val mins = seconds / 60
            val secs = seconds % 60
            return String.format("%d:%02d", mins, secs)
        }

    /** Formatted file size */
    val formattedFileSize: String
        get() {
            val bytes = fileSizeBytes ?: return ""
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> "${bytes / 1024} KB"
                else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            }
        }

    /** Rating description */
    val ratingDescription: String
        get() = when (rating) {
            5 -> "Excellent"
            4 -> "Good"
            3 -> "Needs Work"
            2 -> "Poor"
            1 -> "Major Issues"
            else -> "Not Rated"
        }
}

/**
 * Timestamp marker for specific moments in a video.
 * Used to mark specific points where feedback applies.
 */
@Serializable
data class VideoTimestamp(
    val id: String,
    @SerialName("video_id") val videoId: String,
    @SerialName("timestamp_seconds") val timestampSeconds: Double,
    @SerialName("label") val label: String,
    @SerialName("note") val note: String? = null,
    @SerialName("marker_type") val markerType: MarkerType = MarkerType.NOTE,
    @SerialName("created_at") val createdAt: String? = null
) {
    /** Formatted timestamp */
    val formattedTimestamp: String
        get() {
            val totalSeconds = timestampSeconds.toInt()
            val mins = totalSeconds / 60
            val secs = totalSeconds % 60
            return String.format("%d:%02d", mins, secs)
        }
}

/**
 * Type of timestamp marker.
 */
@Serializable
enum class MarkerType {
    @SerialName("note") NOTE,           // General note
    @SerialName("good") GOOD,           // Good form example
    @SerialName("issue") ISSUE,         // Form issue to fix
    @SerialName("cue") CUE              // Coaching cue
}

/**
 * Request to upload a new form analysis video.
 */
data class UploadVideoRequest(
    val clientId: String,
    val exerciseId: String? = null,
    val exerciseName: String? = null,
    val title: String? = null,
    val notes: String? = null
)

/**
 * Request to add feedback to a video.
 */
data class VideoFeedbackRequest(
    val videoId: String,
    val feedback: String,
    val rating: Int? = null
)

// ═══════════════════════════════════════════════════════════════════════════
// UI STATE
// ═══════════════════════════════════════════════════════════════════════════

/**
 * UI state for the Form Analysis screen.
 */
data class FormAnalysisState(
    val clientId: String = "",
    val clientName: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    // Videos
    val videos: List<FormAnalysisVideo> = emptyList(),
    val selectedVideo: FormAnalysisVideo? = null,
    val timestamps: List<VideoTimestamp> = emptyList(),
    // Filters
    val statusFilter: AnalysisStatus? = null,
    val exerciseFilter: String? = null,
    // Upload state
    val isUploading: Boolean = false,
    val uploadProgress: Float = 0f,
    val uploadError: String? = null,
    // Playback
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L
) {
    /** Filtered videos based on current filters */
    val filteredVideos: List<FormAnalysisVideo>
        get() {
            var result = videos
            statusFilter?.let { filter ->
                result = result.filter { it.status == filter }
            }
            exerciseFilter?.let { filter ->
                result = result.filter { it.exerciseId == filter || it.exerciseName == filter }
            }
            return result
        }

    /** Count of pending reviews */
    val pendingCount: Int
        get() = videos.count { it.isPending }

    /** Count of reviewed videos */
    val reviewedCount: Int
        get() = videos.count { it.isReviewed }

    /** Available exercises from videos */
    val availableExercises: List<String>
        get() = videos.mapNotNull { it.exerciseName }.distinct().sorted()
}
