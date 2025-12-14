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
    val isLiked: Boolean = false
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
    val isFollowedBy: Boolean = false
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
    val visibility: PostVisibility = PostVisibility.PUBLIC,
    val isPosting: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)
