package com.prometheuscoach.mobile.data.repository

import android.util.Log
import com.prometheuscoach.mobile.data.model.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User profile from profiles table (main user table)
 */
@Serializable
data class UserProfile(
    val id: String,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val email: String? = null
) {
    val userId: String get() = id
}

/**
 * Repository for community feed operations.
 * Handles posts, likes, comments, and feed retrieval for coaches.
 */
@Singleton
class CommunityRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository
) {
    companion object {
        private const val TAG = "CommunityRepository"
        private const val POSTS_TABLE = "community_posts"
        private const val LIKES_TABLE = "community_likes"
        private const val COMMENTS_TABLE = "community_comments"
        private const val FOLLOWS_TABLE = "community_follows"
        private const val PAGE_SIZE = 20
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FEED RETRIEVAL
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Get feed for coach - posts from connected clients
     * Uses coach_clients_v to find client IDs, then fetches their posts
     */
    suspend fun getFeed(limit: Int = PAGE_SIZE, offset: Int = 0): Result<List<FeedPost>> {
        return try {
            val coachId = authRepository.getCurrentUserId()
            Log.d(TAG, "getFeed called, coachId: $coachId")
            if (coachId == null) {
                Log.e(TAG, "Not authenticated - no user ID")
                return Result.failure(Exception("Not authenticated"))
            }

            // Get connected client IDs from coach_clients_v view
            val clientIds = getConnectedClientIds(coachId)
            Log.d(TAG, "Found ${clientIds.size} connected clients: $clientIds")

            if (clientIds.isEmpty()) {
                Log.d(TAG, "No connected clients, returning empty feed")
                return Result.success(emptyList())
            }

            // Fetch posts from connected clients
            Log.d(TAG, "Fetching posts for client IDs: $clientIds")
            val posts = supabaseClient.postgrest
                .from(POSTS_TABLE)
                .select {
                    filter {
                        isIn("user_id", clientIds)
                    }
                    order("created_at", Order.DESCENDING)
                    limit(limit.toLong())
                    if (offset > 0) {
                        range(offset.toLong(), (offset + limit - 1).toLong())
                    }
                }
                .decodeList<CommunityPost>()
            Log.d(TAG, "Found ${posts.size} posts from clients")

            // Fetch user profiles for the posts
            val userIds = posts.map { it.userId }.distinct()
            val profiles = fetchUserProfiles(userIds)

            // Map to FeedPost with user data
            val feedPosts = posts.map { post ->
                val profile = profiles[post.userId]
                FeedPost(
                    id = post.id,
                    userId = post.userId,
                    userName = profile?.fullName ?: "Unknown",
                    userAvatar = profile?.avatarUrl,
                    isCoach = false, // These are client posts
                    workoutName = post.workoutName,
                    totalVolumeKg = post.totalVolumeKg,
                    totalSets = post.totalSets,
                    totalReps = post.totalReps,
                    durationMinutes = post.durationMinutes,
                    prsAchieved = post.prsAchieved,
                    caption = post.caption,
                    imageUrls = post.imageUrls,
                    videoUrls = post.videoUrls,
                    likesCount = post.likesCount,
                    commentsCount = post.commentsCount,
                    createdAt = post.createdAt,
                    isLiked = checkIfLiked(post.id, coachId)
                )
            }

            Log.d(TAG, "Loaded ${feedPosts.size} posts from clients")
            Result.success(feedPosts)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading feed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get discover feed - all community posts (workout shares from clients)
     */
    suspend fun getDiscoverFeed(limit: Int = PAGE_SIZE, offset: Int = 0): Result<List<FeedPost>> {
        return try {
            val userId = authRepository.getCurrentUserId()
            Log.d(TAG, "getDiscoverFeed called")

            // Fetch all community posts (no visibility filter - RLS handles access)
            val posts = supabaseClient.postgrest
                .from(POSTS_TABLE)
                .select {
                    order("created_at", Order.DESCENDING)
                    limit(limit.toLong())
                    if (offset > 0) {
                        range(offset.toLong(), (offset + limit - 1).toLong())
                    }
                }
                .decodeList<CommunityPost>()
            Log.d(TAG, "Discover: Found ${posts.size} total posts in community_posts")
            posts.forEach { post ->
                Log.d(TAG, "Post: id=${post.id}, userId=${post.userId}, workout=${post.workoutName}")
            }

            // Fetch user profiles for the posts
            val userIds = posts.map { it.userId }.distinct()
            val profiles = fetchUserProfiles(userIds)

            // Map to FeedPost with user data
            val feedPosts = posts.map { post ->
                val profile = profiles[post.userId]
                FeedPost(
                    id = post.id,
                    userId = post.userId,
                    userName = profile?.fullName ?: "Unknown",
                    userAvatar = profile?.avatarUrl,
                    isCoach = false, // Community posts are from clients
                    workoutName = post.workoutName,
                    totalVolumeKg = post.totalVolumeKg,
                    totalSets = post.totalSets,
                    totalReps = post.totalReps,
                    durationMinutes = post.durationMinutes,
                    prsAchieved = post.prsAchieved,
                    caption = post.caption,
                    imageUrls = post.imageUrls,
                    videoUrls = post.videoUrls,
                    likesCount = post.likesCount,
                    commentsCount = post.commentsCount,
                    createdAt = post.createdAt,
                    isLiked = userId?.let { checkIfLiked(post.id, it) } ?: false
                )
            }

            Log.d(TAG, "Loaded ${feedPosts.size} posts for discover")
            Result.success(feedPosts)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading discover feed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get client IDs connected to the coach
     */
    private suspend fun getConnectedClientIds(coachId: String): List<String> {
        return try {
            Log.d(TAG, "Fetching connected clients for coach: $coachId")
            val connections = supabaseClient.postgrest
                .from("coach_clients_v")
                .select {
                    filter {
                        eq("coach_id", coachId)
                        eq("status", "accepted")
                    }
                }
                .decodeList<CoachClientView>()

            Log.d(TAG, "Found ${connections.size} connections from coach_clients_v")
            connections.forEach { conn ->
                Log.d(TAG, "Connection: clientId=${conn.clientId}, status=${conn.status}")
            }
            connections.map { it.clientId }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching connected clients: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Fetch user profiles for a list of user IDs from the profiles table
     */
    private suspend fun fetchUserProfiles(userIds: List<String>): Map<String, UserProfile> {
        if (userIds.isEmpty()) return emptyMap()

        return try {
            val profiles = supabaseClient.postgrest
                .from("profiles")
                .select {
                    filter {
                        isIn("id", userIds)
                    }
                }
                .decodeList<UserProfile>()

            profiles.associateBy { it.id }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching profiles: ${e.message}", e)
            emptyMap()
        }
    }

    /**
     * Check if current user has liked a post
     */
    private suspend fun checkIfLiked(postId: String, userId: String): Boolean {
        return try {
            val likes = supabaseClient.postgrest
                .from(LIKES_TABLE)
                .select {
                    filter {
                        eq("post_id", postId)
                        eq("user_id", userId)
                    }
                }
                .decodeList<CommunityLike>()
            likes.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get posts for a specific user
     */
    suspend fun getPostsForUser(userId: String, limit: Int = PAGE_SIZE): Result<List<FeedPost>> {
        return try {
            val posts = supabaseClient.postgrest
                .from(POSTS_TABLE)
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                    order("created_at", Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<CommunityPost>()

            val feedPosts = posts.map { post ->
                FeedPost(
                    id = post.id,
                    userId = post.userId,
                    workoutName = post.workoutName,
                    totalVolumeKg = post.totalVolumeKg,
                    totalSets = post.totalSets,
                    totalReps = post.totalReps,
                    durationMinutes = post.durationMinutes,
                    prsAchieved = post.prsAchieved,
                    caption = post.caption,
                    imageUrls = post.imageUrls,
                    videoUrls = post.videoUrls,
                    likesCount = post.likesCount,
                    commentsCount = post.commentsCount,
                    createdAt = post.createdAt,
                    isLiked = false
                )
            }

            Result.success(feedPosts)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading user posts: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get a single post by ID
     */
    suspend fun getPost(postId: String): Result<FeedPost?> {
        return try {
            val posts = supabaseClient.postgrest
                .from(POSTS_TABLE)
                .select {
                    filter {
                        eq("id", postId)
                    }
                }
                .decodeList<CommunityPost>()

            val post = posts.firstOrNull()
            if (post != null) {
                Result.success(
                    FeedPost(
                        id = post.id,
                        userId = post.userId,
                        workoutName = post.workoutName,
                        totalVolumeKg = post.totalVolumeKg,
                        totalSets = post.totalSets,
                        totalReps = post.totalReps,
                        durationMinutes = post.durationMinutes,
                        prsAchieved = post.prsAchieved,
                        caption = post.caption,
                        imageUrls = post.imageUrls,
                        videoUrls = post.videoUrls,
                        likesCount = post.likesCount,
                        commentsCount = post.commentsCount,
                        createdAt = post.createdAt
                    )
                )
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading post: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a post
     */
    suspend fun deletePost(postId: String): Result<Unit> {
        return try {
            val userId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            supabaseClient.postgrest
                .from(POSTS_TABLE)
                .delete {
                    filter {
                        eq("id", postId)
                        eq("user_id", userId)
                    }
                }

            Log.d(TAG, "Deleted post: $postId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting post: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LIKES
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Like a post
     */
    suspend fun likePost(postId: String): Result<Unit> {
        return try {
            val userId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            val like = CommunityLike(
                postId = postId,
                userId = userId
            )

            supabaseClient.postgrest
                .from(LIKES_TABLE)
                .insert(like)

            Log.d(TAG, "Liked post: $postId")
            Result.success(Unit)
        } catch (e: Exception) {
            // May fail if already liked (unique constraint)
            if (e.message?.contains("duplicate") == true ||
                e.message?.contains("unique") == true) {
                Log.d(TAG, "Post already liked: $postId")
                Result.success(Unit)
            } else {
                Log.e(TAG, "Error liking post: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Unlike a post
     */
    suspend fun unlikePost(postId: String): Result<Unit> {
        return try {
            val userId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            supabaseClient.postgrest
                .from(LIKES_TABLE)
                .delete {
                    filter {
                        eq("post_id", postId)
                        eq("user_id", userId)
                    }
                }

            Log.d(TAG, "Unliked post: $postId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error unliking post: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // COMMENTS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Get comments for a post
     */
    suspend fun getComments(postId: String): Result<List<CommunityComment>> {
        return try {
            val comments = supabaseClient.postgrest
                .from(COMMENTS_TABLE)
                .select {
                    filter {
                        eq("post_id", postId)
                    }
                    order("created_at", Order.ASCENDING)
                }
                .decodeList<CommunityComment>()

            Log.d(TAG, "Loaded ${comments.size} comments for post $postId")
            Result.success(comments)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading comments: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Add a comment to a post
     */
    suspend fun addComment(postId: String, content: String): Result<CommunityComment> {
        return try {
            val userId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            val comment = CommunityComment(
                postId = postId,
                userId = userId,
                content = content
            )

            val insertedComments = supabaseClient.postgrest
                .from(COMMENTS_TABLE)
                .insert(comment) {
                    select()
                }
                .decodeList<CommunityComment>()

            val insertedComment = insertedComments.firstOrNull()
                ?: return Result.failure(Exception("Failed to create comment"))

            Log.d(TAG, "Added comment to post $postId")
            Result.success(insertedComment)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding comment: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a comment
     */
    suspend fun deleteComment(commentId: String): Result<Unit> {
        return try {
            val userId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            supabaseClient.postgrest
                .from(COMMENTS_TABLE)
                .delete {
                    filter {
                        eq("id", commentId)
                        eq("user_id", userId)
                    }
                }

            Log.d(TAG, "Deleted comment: $commentId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting comment: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FOLLOW SYSTEM
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Follow a user
     */
    suspend fun followUser(targetUserId: String): Result<Unit> {
        return try {
            val userId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            val follow = Follow(
                followerId = userId,
                followingId = targetUserId,
                status = FollowStatus.ACTIVE
            )

            supabaseClient.postgrest
                .from(FOLLOWS_TABLE)
                .insert(follow)

            Log.d(TAG, "Followed user: $targetUserId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error following user: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Unfollow a user
     */
    suspend fun unfollowUser(targetUserId: String): Result<Unit> {
        return try {
            val userId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            supabaseClient.postgrest
                .from(FOLLOWS_TABLE)
                .delete {
                    filter {
                        eq("follower_id", userId)
                        eq("following_id", targetUserId)
                    }
                }

            Log.d(TAG, "Unfollowed user: $targetUserId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error unfollowing user: ${e.message}", e)
            Result.failure(e)
        }
    }
}
