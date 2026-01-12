package com.prometheuscoach.mobile.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ═══════════════════════════════════════════════════════════════════════════
// POST TYPES FOR COACHES
// ═══════════════════════════════════════════════════════════════════════════

@Serializable
enum class PostType {
    @SerialName("tip") TIP,                     // Training/nutrition tip
    @SerialName("transformation") TRANSFORMATION, // Client success story
    @SerialName("motivation") MOTIVATION,       // Motivational content
    @SerialName("workout") WORKOUT,             // Workout share
    @SerialName("general") GENERAL              // General post
}

@Serializable
enum class PostVisibility {
    @SerialName("public") PUBLIC,
    @SerialName("followers") FOLLOWERS,
    @SerialName("private") PRIVATE
}

// ═══════════════════════════════════════════════════════════════════════════
// COMMUNITY POST
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Community post from community_posts table.
 * These are workout-based posts shared by clients.
 */
@Serializable
data class CommunityPost(
    val id: String = "",
    @SerialName("user_id") val userId: String,
    @SerialName("workout_history_id") val workoutHistoryId: String? = null,
    @SerialName("workout_name") val workoutName: String? = null,
    @SerialName("total_volume_kg") val totalVolumeKg: Int? = null,
    @SerialName("total_sets") val totalSets: Int? = null,
    @SerialName("total_reps") val totalReps: Int? = null,
    @SerialName("duration_minutes") val durationMinutes: Int? = null,
    @SerialName("prs_achieved") val prsAchieved: Int? = null,
    @SerialName("pr_exercises") val prExercises: String? = null,
    val caption: String? = null,
    val visibility: String = "followers",
    @SerialName("likes_count") val likesCount: Int = 0,
    @SerialName("comments_count") val commentsCount: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("image_urls") val imageUrls: List<String>? = null,
    @SerialName("video_urls") val videoUrls: List<String>? = null
)

/**
 * Request model for creating a new community post.
 * Does NOT include id, likes_count, comments_count, or timestamps - those are set by the database.
 */
@Serializable
data class CreateCommunityPostRequest(
    @SerialName("user_id") val userId: String,
    val caption: String? = null,
    val visibility: String = "public",
    @SerialName("image_urls") val imageUrls: List<String>? = null,
    @SerialName("video_urls") val videoUrls: List<String>? = null
)

/**
 * Post with joined user data for feed display.
 * Represents a workout post from a client.
 */
@Serializable
data class FeedPost(
    val id: String,
    @SerialName("user_id") val userId: String,
    // User info (joined from profiles)
    val userName: String? = null,
    val userAvatar: String? = null,
    val isCoach: Boolean = false,
    // Workout info
    val workoutName: String? = null,
    val totalVolumeKg: Int? = null,
    val totalSets: Int? = null,
    val totalReps: Int? = null,
    val durationMinutes: Int? = null,
    val prsAchieved: Int? = null,
    val caption: String? = null,
    // Media
    @SerialName("image_urls") val imageUrls: List<String>? = null,
    @SerialName("video_urls") val videoUrls: List<String>? = null,
    // Engagement
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val createdAt: String? = null,
    val isLiked: Boolean = false,
    // Preview comments for feed display (last 2-3 comments)
    val previewComments: List<CommunityComment> = emptyList()
) {
    val userDisplayName: String? get() = userName
    val userAvatarUrl: String? get() = userAvatar
    val hasMedia: Boolean get() = !imageUrls.isNullOrEmpty() || !videoUrls.isNullOrEmpty()
    // For backwards compatibility with UI
    val content: String get() = caption ?: ""
    val title: String? get() = workoutName
    val postType: PostType get() = PostType.WORKOUT
}

// ═══════════════════════════════════════════════════════════════════════════
// LIKES & COMMENTS
// ═══════════════════════════════════════════════════════════════════════════

@Serializable
data class CommunityLike(
    val id: String = "",
    @SerialName("post_id") val postId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class CommunityComment(
    val id: String = "",
    @SerialName("post_id") val postId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("parent_comment_id") val parentCommentId: String? = null,
    val content: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("user_name") val userName: String? = null,
    @SerialName("user_avatar") val userAvatar: String? = null,
    @SerialName("is_coach") val isCoach: Boolean = false
) {
    val userDisplayName: String? get() = userName
    val userAvatarUrl: String? get() = userAvatar
}

// ═══════════════════════════════════════════════════════════════════════════
// FOLLOW SYSTEM
// ═══════════════════════════════════════════════════════════════════════════

@Serializable
enum class FollowStatus {
    @SerialName("active") ACTIVE,
    @SerialName("pending") PENDING,
    @SerialName("blocked") BLOCKED
}

@Serializable
data class Follow(
    val id: String = "",
    @SerialName("follower_id") val followerId: String,
    @SerialName("following_id") val followingId: String,
    val status: FollowStatus = FollowStatus.ACTIVE,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class FollowWithUser(
    val id: String,
    @SerialName("follower_id") val followerId: String,
    @SerialName("following_id") val followingId: String,
    val status: FollowStatus = FollowStatus.ACTIVE,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("user_name") val userName: String? = null,
    @SerialName("user_avatar") val userAvatar: String? = null,
    @SerialName("is_coach") val isCoach: Boolean = false
) {
    val userDisplayName: String? get() = userName
    val userAvatarUrl: String? get() = userAvatar
}

// ═══════════════════════════════════════════════════════════════════════════
// USER COMMUNITY PROFILE
// ═══════════════════════════════════════════════════════════════════════════

data class UserCommunityStats(
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val postsCount: Int = 0,
    val isFollowing: Boolean = false,
    val isFollowedBy: Boolean = false,
    val isPendingFollow: Boolean = false
)

// ═══════════════════════════════════════════════════════════════════════════
// COMMUNITY PROFILE
// ═══════════════════════════════════════════════════════════════════════════

@Serializable
data class CommunityProfile(
    @SerialName("user_id") val userId: String,
    @SerialName("is_public") val isPublic: Boolean = true,
    @SerialName("display_name") val displayName: String? = null,
    val bio: String? = null,
    @SerialName("show_in_leaderboard") val showInLeaderboard: Boolean = true,
    @SerialName("allow_follow_requests") val allowFollowRequests: Boolean = true,
    @SerialName("auto_share_workouts") val autoShareWorkouts: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

// ═══════════════════════════════════════════════════════════════════════════
// LEADERBOARD
// ═══════════════════════════════════════════════════════════════════════════

@Serializable
data class LeaderboardEntry(
    val id: String = "",
    @SerialName("user_id") val userId: String,
    @SerialName("exercise_id") val exerciseId: String,
    @SerialName("exercise_name") val exerciseName: String,
    @SerialName("pr_weight_kg") val prWeightKg: Double? = null,
    @SerialName("pr_reps") val prReps: Int? = null,
    @SerialName("estimated_1rm_kg") val estimated1rmKg: Double? = null,
    @SerialName("user_bodyweight_kg") val userBodyweightKg: Double? = null,
    @SerialName("wilks_score") val wilksScore: Double? = null,
    @SerialName("dots_score") val dotsScore: Double? = null,
    @SerialName("achieved_at") val achievedAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("user_name") val userName: String? = null,
    @SerialName("user_avatar") val userAvatar: String? = null,
    var rank: Int = 0,
    @SerialName("is_current_user") val isCurrentUser: Boolean = false,
    @SerialName("is_pr") val isPr: Boolean = false
) {
    val userDisplayName: String? get() = userName
    val userAvatarUrl: String? get() = userAvatar
}

object LeaderboardExercises {
    val EXERCISES = listOf(
        LeaderboardExercise("bench_press", "Bench Press"),
        LeaderboardExercise("squat", "Squat"),
        LeaderboardExercise("deadlift", "Deadlift"),
        LeaderboardExercise("overhead_press", "Overhead Press"),
        LeaderboardExercise("barbell_row", "Barbell Row"),
        LeaderboardExercise("weighted_pullup", "Weighted Pull-Up")
    )

    fun getExerciseName(id: String): String {
        return EXERCISES.find { it.id == id }?.name ?: id
    }
}

data class LeaderboardExercise(
    val id: String,
    val name: String
)

data class LeaderboardState(
    val entries: List<LeaderboardEntry> = emptyList(),
    val selectedExercise: String = "bench_press",
    val friendsOnly: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentUserRank: Int? = null
)

data class UserProfileState(
    val profile: CommunityUserProfile? = null,
    val posts: List<FeedPost> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingPosts: Boolean = false,
    val isCurrentUser: Boolean = false,
    val error: String? = null
)

data class CommunityUserProfile(
    val userId: String,
    val name: String,
    val avatarUrl: String? = null,
    val bio: String? = null,
    val isCoach: Boolean = false,
    val stats: UserCommunityStats = UserCommunityStats()
) {
    val displayName: String get() = name
    val followersCount: Int get() = stats.followersCount
    val followingCount: Int get() = stats.followingCount
    val postsCount: Int get() = stats.postsCount
    val isFollowing: Boolean get() = stats.isFollowing
}

// ═══════════════════════════════════════════════════════════════════════════
// UI STATE
// ═══════════════════════════════════════════════════════════════════════════

enum class FeedTab {
    FOLLOWING,
    DISCOVER
}

data class FeedState(
    val posts: List<FeedPost> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val hasMore: Boolean = true,
    val currentTab: FeedTab = FeedTab.FOLLOWING  // Default to My Clients tab for coaches
)

data class PostDetailState(
    val post: FeedPost? = null,
    val comments: List<CommunityComment> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingComments: Boolean = false,
    val error: String? = null
)

// ═══════════════════════════════════════════════════════════════════════════
// CREATE POST STATE
// ═══════════════════════════════════════════════════════════════════════════

data class CreatePostState(
    val postType: PostType = PostType.GENERAL,
    val title: String = "",
    val content: String = "",
    val imageUrls: List<String> = emptyList(),
    val videoUrls: List<String> = emptyList(),
    val selectedImageUris: List<String> = emptyList(),
    val selectedVideoUris: List<String> = emptyList(),
    val visibility: PostVisibility = PostVisibility.PUBLIC,
    val isPosting: Boolean = false,
    val isUploadingMedia: Boolean = false,
    val uploadProgress: Float = 0f,
    val error: String? = null,
    val isSuccess: Boolean = false
)
