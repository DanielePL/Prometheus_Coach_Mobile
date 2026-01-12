package com.prometheuscoach.mobile.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.prometheuscoach.mobile.data.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Profile info for community posts (local to this repository)
 * Maps to profiles table (coaches)
 */
@Serializable
private data class ProfileInfo(
    val id: String,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null
)

/**
 * User profile info from user_profiles table (clients from mobile app)
 * The client mobile app stores profile images in user_profiles.profile_image_url
 */
@Serializable
private data class UserProfileInfo(
    val id: String,
    val name: String? = null,
    @SerialName("profile_image_url") val profileImageUrl: String? = null
)

/**
 * Repository for community feed operations.
 * Handles posts, likes, comments, and feed retrieval for coaches.
 */
@Singleton
class CommunityRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "CommunityRepository"
        private const val POSTS_TABLE = "community_posts"
        private const val LIKES_TABLE = "community_likes"
        private const val COMMENTS_TABLE = "community_comments"
        private const val FOLLOWS_TABLE = "community_follows"
        private const val PAGE_SIZE = 20
        private const val COMMUNITY_VIDEOS_BUCKET = "community-videos"
        private const val COMMUNITY_IMAGES_BUCKET = "community-images"
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FEED RETRIEVAL
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Get feed for coach - posts from connected clients AND the coach's own posts
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

            // Include coach's own posts + client posts
            val allUserIds = (clientIds + coachId).distinct()
            Log.d(TAG, "Fetching posts for user IDs (including coach): $allUserIds")

            // Fetch posts from connected clients AND the coach
            val posts = supabaseClient.postgrest
                .from(POSTS_TABLE)
                .select {
                    filter {
                        isIn("user_id", allUserIds)
                    }
                    order("created_at", Order.DESCENDING)
                    limit(limit.toLong())
                    if (offset > 0) {
                        range(offset.toLong(), (offset + limit - 1).toLong())
                    }
                }
                .decodeList<CommunityPost>()
            Log.d(TAG, "Found ${posts.size} posts from clients and coach")

            // Fetch user profiles for the posts
            val userIds = posts.map { it.userId }.distinct()
            val profiles = fetchUserProfiles(userIds)

            // Fetch preview comments and actual counts for all posts
            val postIds = posts.map { it.id }
            val previewCommentsMap = getPreviewCommentsForPosts(postIds)
            val likesCountMap = countLikesForPosts(postIds)
            val commentsCountMap = countCommentsForPosts(postIds)

            // Map to FeedPost with user data, preview comments, and actual counts
            val feedPosts = posts.map { post ->
                val profile = profiles[post.userId]
                FeedPost(
                    id = post.id,
                    userId = post.userId,
                    userName = profile?.fullName ?: "Unknown",
                    userAvatar = profile?.avatarUrl,
                    isCoach = post.userId == coachId, // Mark coach's own posts
                    workoutName = post.workoutName,
                    totalVolumeKg = post.totalVolumeKg,
                    totalSets = post.totalSets,
                    totalReps = post.totalReps,
                    durationMinutes = post.durationMinutes,
                    prsAchieved = post.prsAchieved,
                    caption = post.caption,
                    imageUrls = post.imageUrls,
                    videoUrls = post.videoUrls,
                    likesCount = likesCountMap[post.id] ?: 0,
                    commentsCount = commentsCountMap[post.id] ?: 0,
                    createdAt = post.createdAt,
                    isLiked = checkIfLiked(post.id, coachId),
                    previewComments = previewCommentsMap[post.id] ?: emptyList()
                )
            }

            Log.d(TAG, "Loaded ${feedPosts.size} posts (coach + clients)")
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

            // Fetch preview comments and actual counts for all posts
            val postIds = posts.map { it.id }
            val previewCommentsMap = getPreviewCommentsForPosts(postIds)
            val likesCountMap = countLikesForPosts(postIds)
            val commentsCountMap = countCommentsForPosts(postIds)

            // Map to FeedPost with user data, preview comments, and actual counts
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
                    likesCount = likesCountMap[post.id] ?: 0,
                    commentsCount = commentsCountMap[post.id] ?: 0,
                    createdAt = post.createdAt,
                    isLiked = userId?.let { checkIfLiked(post.id, it) } ?: false,
                    previewComments = previewCommentsMap[post.id] ?: emptyList()
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
     * Fetch user profiles for a list of user IDs.
     *
     * IMPORTANT: This checks BOTH tables:
     * - profiles: Coach app users (avatar_url)
     * - user_profiles: Client mobile app users (profile_image_url)
     *
     * Priority: user_profiles.profile_image_url > profiles.avatar_url
     * (because clients upload to user_profiles, not profiles)
     */
    private suspend fun fetchUserProfiles(userIds: List<String>): Map<String, ProfileInfo> {
        if (userIds.isEmpty()) return emptyMap()

        return try {
            // First, get from profiles table (coaches)
            val coachProfiles = supabaseClient.postgrest
                .from("profiles")
                .select {
                    filter {
                        isIn("id", userIds)
                    }
                }
                .decodeList<ProfileInfo>()

            Log.d(TAG, "Fetched ${coachProfiles.size} profiles from 'profiles' table")
            coachProfiles.forEach { p ->
                Log.d(TAG, "  Profile: id=${p.id}, name=${p.fullName}, avatar=${p.avatarUrl?.take(50)}...")
            }

            // Then, get from user_profiles table (clients from mobile app)
            val clientProfiles = try {
                supabaseClient.postgrest
                    .from("user_profiles")
                    .select {
                        filter {
                            isIn("id", userIds)
                        }
                    }
                    .decodeList<UserProfileInfo>()
            } catch (e: Exception) {
                Log.w(TAG, "Could not fetch from user_profiles: ${e.message}")
                emptyList()
            }

            Log.d(TAG, "Fetched ${clientProfiles.size} profiles from 'user_profiles' table")
            clientProfiles.forEach { p ->
                Log.d(TAG, "  UserProfile: id=${p.id}, name=${p.name}, profileImageUrl=${p.profileImageUrl?.take(50)}...")
            }

            // Create base map from coach profiles
            val profileMap = coachProfiles.associateBy { it.id }.toMutableMap()

            // Merge with client profiles - prioritize client's profile_image_url
            clientProfiles.forEach { clientProfile ->
                val existing = profileMap[clientProfile.id]
                if (existing != null) {
                    // User exists in profiles table - update avatar if client has one
                    if (!clientProfile.profileImageUrl.isNullOrBlank()) {
                        profileMap[clientProfile.id] = existing.copy(
                            avatarUrl = clientProfile.profileImageUrl
                        )
                        Log.d(TAG, "Updated avatar for ${clientProfile.id} from user_profiles")
                    }
                    // Also update name if client has one and profiles doesn't
                    if (existing.fullName.isNullOrBlank() && !clientProfile.name.isNullOrBlank()) {
                        profileMap[clientProfile.id] = profileMap[clientProfile.id]!!.copy(
                            fullName = clientProfile.name
                        )
                    }
                } else {
                    // User only exists in user_profiles (client without coach profile entry)
                    profileMap[clientProfile.id] = ProfileInfo(
                        id = clientProfile.id,
                        fullName = clientProfile.name,
                        avatarUrl = clientProfile.profileImageUrl
                    )
                    Log.d(TAG, "Added profile for ${clientProfile.id} from user_profiles only")
                }
            }

            Log.d(TAG, "Final merged profiles: ${profileMap.size}")
            profileMap
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
     * Count actual likes for multiple posts from the likes table.
     * More reliable than cached likes_count in posts table.
     */
    private suspend fun countLikesForPosts(postIds: List<String>): Map<String, Int> {
        if (postIds.isEmpty()) return emptyMap()
        return try {
            val likes = supabaseClient.postgrest
                .from(LIKES_TABLE)
                .select {
                    filter { isIn("post_id", postIds) }
                }
                .decodeList<CommunityLike>()
            likes.groupBy { it.postId }.mapValues { it.value.size }
        } catch (e: Exception) {
            Log.e(TAG, "Error counting likes: ${e.message}")
            emptyMap()
        }
    }

    /**
     * Count actual comments for multiple posts from the comments table.
     * More reliable than cached comments_count in posts table.
     */
    private suspend fun countCommentsForPosts(postIds: List<String>): Map<String, Int> {
        if (postIds.isEmpty()) return emptyMap()
        return try {
            val comments = supabaseClient.postgrest
                .from(COMMENTS_TABLE)
                .select {
                    filter { isIn("post_id", postIds) }
                }
                .decodeList<CommunityComment>()
            comments.groupBy { it.postId }.mapValues { it.value.size }
        } catch (e: Exception) {
            Log.e(TAG, "Error counting comments: ${e.message}")
            emptyMap()
        }
    }

    /**
     * Get posts for a specific user
     */
    suspend fun getPostsForUser(userId: String, limit: Int = PAGE_SIZE): Result<List<FeedPost>> {
        return try {
            val currentUserId = authRepository.getCurrentUserId()

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

            // Fetch user profile for the posts
            val profiles = fetchUserProfiles(listOf(userId))
            val profile = profiles[userId]

            val feedPosts = posts.map { post ->
                FeedPost(
                    id = post.id,
                    userId = post.userId,
                    userName = profile?.fullName ?: "Unknown",
                    userAvatar = profile?.avatarUrl,
                    isCoach = false,
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
                    isLiked = currentUserId?.let { checkIfLiked(post.id, it) } ?: false
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
            val currentUserId = authRepository.getCurrentUserId()

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
                // Fetch user profile for the post author
                val profiles = fetchUserProfiles(listOf(post.userId))
                val profile = profiles[post.userId]

                Result.success(
                    FeedPost(
                        id = post.id,
                        userId = post.userId,
                        userName = profile?.fullName ?: "Unknown",
                        userAvatar = profile?.avatarUrl,
                        isCoach = false, // Will be determined by UI if needed
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
                        isLiked = currentUserId?.let { checkIfLiked(post.id, it) } ?: false
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

            // Check if already liked to avoid duplicates
            if (checkIfLiked(postId, userId)) {
                Log.d(TAG, "Post already liked: $postId")
                return Result.success(Unit)
            }

            val like = CommunityLike(
                postId = postId,
                userId = userId
            )

            supabaseClient.postgrest
                .from(LIKES_TABLE)
                .insert(like)

            // Increment likes_count in posts table
            try {
                val currentPost = supabaseClient.postgrest
                    .from(POSTS_TABLE)
                    .select { filter { eq("id", postId) } }
                    .decodeSingleOrNull<CommunityPost>()

                if (currentPost != null) {
                    supabaseClient.postgrest
                        .from(POSTS_TABLE)
                        .update({ set("likes_count", currentPost.likesCount + 1) }) {
                            filter { eq("id", postId) }
                        }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update likes_count: ${e.message}")
            }

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

            // Check if actually liked before unliking
            if (!checkIfLiked(postId, userId)) {
                Log.d(TAG, "Post not liked, nothing to unlike: $postId")
                return Result.success(Unit)
            }

            supabaseClient.postgrest
                .from(LIKES_TABLE)
                .delete {
                    filter {
                        eq("post_id", postId)
                        eq("user_id", userId)
                    }
                }

            // Decrement likes_count in posts table
            try {
                val currentPost = supabaseClient.postgrest
                    .from(POSTS_TABLE)
                    .select { filter { eq("id", postId) } }
                    .decodeSingleOrNull<CommunityPost>()

                if (currentPost != null && currentPost.likesCount > 0) {
                    supabaseClient.postgrest
                        .from(POSTS_TABLE)
                        .update({ set("likes_count", currentPost.likesCount - 1) }) {
                            filter { eq("id", postId) }
                        }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update likes_count: ${e.message}")
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
     * Get comments for a post with user profile data
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

            // Fetch user profiles for comment authors
            val userIds = comments.map { it.userId }.distinct()
            val profiles = fetchUserProfiles(userIds)

            // Enrich comments with user profile data
            val enrichedComments = comments.map { comment ->
                val profile = profiles[comment.userId]
                comment.copy(
                    userName = profile?.fullName ?: "Unknown",
                    userAvatar = profile?.avatarUrl
                )
            }

            Log.d(TAG, "Loaded ${enrichedComments.size} comments for post $postId")
            Result.success(enrichedComments)
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
     * Get preview comments for multiple posts (last 2 comments per post, newest first)
     * Returns a map of postId to list of comments
     */
    suspend fun getPreviewCommentsForPosts(postIds: List<String>, limit: Int = 2): Map<String, List<CommunityComment>> {
        if (postIds.isEmpty()) {
            Log.d(TAG, "getPreviewCommentsForPosts: No post IDs provided")
            return emptyMap()
        }

        Log.d(TAG, "getPreviewCommentsForPosts: Fetching comments for ${postIds.size} posts")

        return try {
            // Fetch comments for all posts
            val allComments = supabaseClient.postgrest
                .from(COMMENTS_TABLE)
                .select {
                    filter {
                        isIn("post_id", postIds)
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<CommunityComment>()

            Log.d(TAG, "getPreviewCommentsForPosts: Found ${allComments.size} total comments")

            if (allComments.isEmpty()) {
                return emptyMap()
            }

            // Fetch user profiles for comment authors
            val userIds = allComments.map { it.userId }.distinct()
            val profiles = fetchUserProfiles(userIds)

            // Enrich comments with user profile data
            val enrichedComments = allComments.map { comment ->
                val profile = profiles[comment.userId]
                comment.copy(
                    userName = profile?.fullName ?: "Unknown",
                    userAvatar = profile?.avatarUrl
                )
            }

            // Group by post and take last N comments per post
            val result = enrichedComments
                .groupBy { it.postId }
                .mapValues { (_, comments) ->
                    comments.take(limit).reversed() // Take newest N, then reverse so oldest is first
                }

            Log.d(TAG, "getPreviewCommentsForPosts: Returning comments for ${result.size} posts")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error loading preview comments: ${e.message}", e)
            emptyMap()
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

    // ═══════════════════════════════════════════════════════════════════════
    // MEDIA UPLOAD
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Upload a video to Supabase Storage.
     * Path format: posts/{user_id}/{uuid}.{extension}
     * This matches the RLS policy which checks (storage.foldername(name))[2] = auth.uid()
     */
    suspend fun uploadVideo(videoUri: Uri): Result<String> {
        return try {
            val userId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(videoUri) ?: "video/mp4"
            val extension = when {
                mimeType.contains("mp4") -> "mp4"
                mimeType.contains("mov") -> "mov"
                mimeType.contains("webm") -> "webm"
                else -> "mp4"
            }

            val fileName = "${UUID.randomUUID()}.$extension"
            // Path: posts/{user_id}/{filename} to match RLS policy
            val filePath = "posts/$userId/$fileName"

            val inputStream = contentResolver.openInputStream(videoUri)
                ?: return Result.failure(Exception("Could not read video file"))

            val bytes = inputStream.use { it.readBytes() }

            Log.d(TAG, "Uploading video to: $filePath (${bytes.size} bytes)")

            supabaseClient.storage
                .from(COMMUNITY_VIDEOS_BUCKET)
                .upload(filePath, bytes) {
                    contentType = ContentType.parse(mimeType)
                    upsert = false
                }

            // Get the public URL
            val publicUrl = supabaseClient.storage
                .from(COMMUNITY_VIDEOS_BUCKET)
                .publicUrl(filePath)

            Log.d(TAG, "Video uploaded successfully: $publicUrl")
            Result.success(publicUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading video: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Upload an image to Supabase Storage.
     * Path format: posts/{user_id}/{uuid}.{extension}
     */
    suspend fun uploadImage(imageUri: Uri): Result<String> {
        return try {
            val userId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(imageUri) ?: "image/jpeg"
            val extension = when {
                mimeType.contains("png") -> "png"
                mimeType.contains("webp") -> "webp"
                mimeType.contains("gif") -> "gif"
                else -> "jpg"
            }

            val fileName = "${UUID.randomUUID()}.$extension"
            val filePath = "posts/$userId/$fileName"

            val inputStream = contentResolver.openInputStream(imageUri)
                ?: return Result.failure(Exception("Could not read image file"))

            val bytes = inputStream.use { it.readBytes() }

            Log.d(TAG, "Uploading image to: $filePath (${bytes.size} bytes)")

            supabaseClient.storage
                .from(COMMUNITY_IMAGES_BUCKET)
                .upload(filePath, bytes) {
                    contentType = ContentType.parse(mimeType)
                    upsert = false
                }

            val publicUrl = supabaseClient.storage
                .from(COMMUNITY_IMAGES_BUCKET)
                .publicUrl(filePath)

            Log.d(TAG, "Image uploaded successfully: $publicUrl")
            Result.success(publicUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading image: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // POST CREATION
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Create a new community post with optional media.
     */
    suspend fun createPost(
        caption: String,
        imageUrls: List<String> = emptyList(),
        videoUrls: List<String> = emptyList(),
        visibility: String = "public"
    ): Result<CommunityPost> {
        return try {
            val userId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            // Use CreateCommunityPostRequest to avoid sending id="" which violates NOT NULL
            val postRequest = CreateCommunityPostRequest(
                userId = userId,
                caption = caption,
                imageUrls = imageUrls.ifEmpty { null },
                videoUrls = videoUrls.ifEmpty { null },
                visibility = visibility
            )

            Log.d(TAG, "Creating post with request: userId=$userId, caption=$caption, visibility=$visibility")

            val insertedPosts = supabaseClient.postgrest
                .from(POSTS_TABLE)
                .insert(postRequest) {
                    select()
                }
                .decodeList<CommunityPost>()

            val insertedPost = insertedPosts.firstOrNull()
                ?: return Result.failure(Exception("Failed to create post"))

            Log.d(TAG, "Created post: ${insertedPost.id}")
            Result.success(insertedPost)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating post: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CHALLENGES
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Get active community challenges.
     */
    suspend fun getActiveChallenges(): Result<List<Challenge>> {
        return try {
            val challenges = try {
                supabaseClient.postgrest
                    .from("challenges")
                    .select {
                        filter { eq("status", "active") }
                        order("end_date", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                    }
                    .decodeList<Challenge>()
            } catch (e: Exception) {
                Log.w(TAG, "challenges table not available, using mock data", e)
                getMockChallenges()
            }

            Log.d(TAG, "Loaded ${challenges.size} active challenges")
            Result.success(challenges)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading active challenges", e)
            Result.failure(e)
        }
    }

    /**
     * Get all challenges (active, upcoming, completed).
     */
    suspend fun getAllChallenges(): Result<ChallengesState> {
        return try {
            val allChallenges = try {
                supabaseClient.postgrest
                    .from("challenges")
                    .select {
                        order("created_at", Order.DESCENDING)
                    }
                    .decodeList<Challenge>()
            } catch (e: Exception) {
                Log.w(TAG, "challenges table not available", e)
                getMockChallenges()
            }

            val active = allChallenges.filter { it.status == ChallengeStatus.ACTIVE }
            val upcoming = allChallenges.filter { it.status == ChallengeStatus.UPCOMING }
            val completed = allChallenges.filter { it.status == ChallengeStatus.COMPLETED }

            Result.success(
                ChallengesState(
                    activeChallenges = active,
                    upcomingChallenges = upcoming,
                    completedChallenges = completed
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error loading all challenges", e)
            Result.failure(e)
        }
    }

    /**
     * Get leaderboard for a specific challenge.
     */
    suspend fun getChallengeLeaderboard(challengeId: String, limit: Int = 50): Result<List<ChallengeEntry>> {
        return try {
            val entries = try {
                supabaseClient.postgrest
                    .from("challenge_entries")
                    .select {
                        filter { eq("challenge_id", challengeId) }
                        order("value_kg", Order.DESCENDING)
                        limit(limit.toLong())
                    }
                    .decodeList<ChallengeEntry>()
            } catch (e: Exception) {
                Log.w(TAG, "Could not get challenge leaderboard", e)
                emptyList()
            }

            // Fetch user profiles for entries
            val userIds = entries.map { it.userId }.distinct()
            val profiles = fetchUserProfiles(userIds)

            // Enrich entries with user profiles and calculate ranks
            val enrichedEntries = entries.mapIndexed { index, entry ->
                val profile = profiles[entry.userId]
                entry.copy(
                    rank = index + 1,
                    userName = profile?.fullName ?: entry.userName,
                    userAvatar = profile?.avatarUrl ?: entry.userAvatar
                )
            }

            Log.d(TAG, "Loaded ${enrichedEntries.size} entries for challenge $challengeId")
            Result.success(enrichedEntries)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading challenge leaderboard", e)
            Result.failure(e)
        }
    }

    /**
     * Get challenge participation history for a specific client.
     */
    suspend fun getClientChallengeHistory(clientId: String): Result<List<ChallengeEntry>> {
        return try {
            val entries = try {
                supabaseClient.postgrest
                    .from("challenge_entries")
                    .select {
                        filter { eq("user_id", clientId) }
                        order("submitted_at", Order.DESCENDING)
                    }
                    .decodeList<ChallengeEntry>()
            } catch (e: Exception) {
                Log.w(TAG, "Could not get client challenge history", e)
                emptyList()
            }

            Log.d(TAG, "Loaded ${entries.size} challenge entries for client $clientId")
            Result.success(entries)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading client challenge history", e)
            Result.failure(e)
        }
    }

    /**
     * Get clients participating in a specific challenge (coach view).
     */
    suspend fun getClientsInChallenge(challengeId: String): Result<List<ChallengeEntry>> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            // Get connected client IDs
            val clientIds = getConnectedClientIds(coachId)
            if (clientIds.isEmpty()) {
                return Result.success(emptyList())
            }

            // Get entries for those clients in this challenge
            val entries = try {
                supabaseClient.postgrest
                    .from("challenge_entries")
                    .select {
                        filter {
                            eq("challenge_id", challengeId)
                            isIn("user_id", clientIds)
                        }
                        order("value_kg", Order.DESCENDING)
                    }
                    .decodeList<ChallengeEntry>()
            } catch (e: Exception) {
                Log.w(TAG, "Could not get client challenge entries", e)
                emptyList()
            }

            // Fetch user profiles for entries
            val userIds = entries.map { it.userId }.distinct()
            val profiles = fetchUserProfiles(userIds)

            val enrichedEntries = entries.mapIndexed { index, entry ->
                val profile = profiles[entry.userId]
                entry.copy(
                    rank = index + 1,
                    userName = profile?.fullName ?: entry.userName,
                    userAvatar = profile?.avatarUrl ?: entry.userAvatar
                )
            }

            Log.d(TAG, "Found ${enrichedEntries.size} clients in challenge $challengeId")
            Result.success(enrichedEntries)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting clients in challenge", e)
            Result.failure(e)
        }
    }

    private fun getMockChallenges(): List<Challenge> {
        val now = java.time.LocalDate.now()
        return listOf(
            Challenge(
                id = "mof_weekly",
                title = "Max Out Friday - Bench Press",
                description = "Weekly max lift competition",
                challengeType = ChallengeType.MAX_OUT_FRIDAY,
                exerciseId = "bench_press",
                exerciseName = "Bench Press",
                startDate = now.minusDays(2).toString(),
                endDate = now.plusDays(5).toString(),
                status = ChallengeStatus.ACTIVE,
                participantsCount = 24
            ),
            Challenge(
                id = "volume_week",
                title = "Volume Challenge",
                description = "Most volume lifted this week",
                challengeType = ChallengeType.VOLUME,
                targetVolumeKg = 50000.0,
                startDate = now.minusDays(3).toString(),
                endDate = now.plusDays(4).toString(),
                status = ChallengeStatus.ACTIVE,
                participantsCount = 18
            ),
            Challenge(
                id = "streak_challenge",
                title = "7-Day Streak",
                description = "Complete 7 workouts in a row",
                challengeType = ChallengeType.STREAK,
                targetStreakDays = 7,
                startDate = now.minusDays(5).toString(),
                endDate = now.plusDays(9).toString(),
                status = ChallengeStatus.ACTIVE,
                participantsCount = 32
            )
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MAX OUT FRIDAY
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Get current Max Out Friday challenge.
     */
    suspend fun getCurrentMaxOutFriday(): Result<MaxOutFridayInfo?> {
        return try {
            val maxOut = try {
                supabaseClient.postgrest
                    .from("challenges")
                    .select {
                        filter {
                            eq("challenge_type", "max_out_friday")
                            eq("status", "active")
                        }
                        limit(1)
                    }
                    .decodeList<Challenge>()
                    .firstOrNull()?.let { challenge ->
                        MaxOutFridayInfo(
                            id = challenge.id,
                            title = challenge.title,
                            exerciseId = challenge.exerciseId ?: "",
                            exerciseName = challenge.exerciseName ?: "Unknown",
                            startDate = challenge.startDate,
                            endDate = challenge.endDate,
                            participantsCount = challenge.participantsCount,
                            userEntryKg = null,
                            userRank = null
                        )
                    }
            } catch (e: Exception) {
                Log.w(TAG, "Could not get max out friday, using mock", e)
                getMockMaxOutFriday()
            }

            Result.success(maxOut)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current max out friday", e)
            Result.failure(e)
        }
    }

    /**
     * Get Max Out Friday leaderboard.
     */
    suspend fun getMaxOutFridayLeaderboard(challengeId: String, limit: Int = 20): Result<List<ChallengeEntry>> {
        return getChallengeLeaderboard(challengeId, limit)
    }

    /**
     * Get Max Out Friday history for clients.
     */
    suspend fun getMaxOutFridayHistory(limit: Int = 10): Result<List<MaxOutFridayHistory>> {
        return try {
            val history = try {
                supabaseClient.postgrest
                    .from("max_out_friday_history")
                    .select {
                        order("week_date", Order.DESCENDING)
                        limit(limit.toLong())
                    }
                    .decodeList<MaxOutFridayHistory>()
            } catch (e: Exception) {
                Log.w(TAG, "max_out_friday_history table not available", e)
                emptyList()
            }

            Result.success(history)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting max out friday history", e)
            Result.failure(e)
        }
    }

    /**
     * Get previous Max Out Friday winners.
     */
    suspend fun getPreviousWinners(limit: Int = 6): Result<List<PreviousWinner>> {
        return try {
            val winners = try {
                supabaseClient.postgrest
                    .from("previous_winners")
                    .select {
                        order("week_date", Order.DESCENDING)
                        limit(limit.toLong())
                    }
                    .decodeList<PreviousWinner>()
            } catch (e: Exception) {
                Log.w(TAG, "previous_winners table not available, using mock", e)
                getMockPreviousWinners()
            }

            Result.success(winners)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting previous winners", e)
            Result.failure(e)
        }
    }

    /**
     * Get client's Max Out Friday participation history.
     */
    suspend fun getClientMaxOutHistory(clientId: String): Result<List<MaxOutFridayHistory>> {
        return try {
            val history = try {
                supabaseClient.postgrest
                    .from("max_out_friday_history")
                    .select {
                        filter { eq("user_id", clientId) }
                        order("week_date", Order.DESCENDING)
                    }
                    .decodeList<MaxOutFridayHistory>()
            } catch (e: Exception) {
                Log.w(TAG, "Could not get client max out history", e)
                emptyList()
            }

            Result.success(history)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting client max out history", e)
            Result.failure(e)
        }
    }

    private fun getMockMaxOutFriday(): MaxOutFridayInfo {
        val now = java.time.LocalDate.now()
        val (exerciseId, exerciseName) = MaxOutFridayRotation.getExerciseForWeek(
            now.get(java.time.temporal.WeekFields.ISO.weekOfYear())
        )
        return MaxOutFridayInfo(
            id = "mof_${now.get(java.time.temporal.WeekFields.ISO.weekOfYear())}",
            title = "Max Out Friday - $exerciseName",
            exerciseId = exerciseId,
            exerciseName = exerciseName,
            startDate = now.with(java.time.DayOfWeek.MONDAY).toString(),
            endDate = now.with(java.time.DayOfWeek.FRIDAY).plusDays(1).toString(),
            participantsCount = 24,
            userEntryKg = null,
            userRank = null
        )
    }

    private fun getMockPreviousWinners(): List<PreviousWinner> {
        return listOf(
            PreviousWinner(
                challengeId = "mof_51",
                exerciseName = "Squat",
                winnerId = "user1",
                winnerName = "Alex T.",
                winnerAvatar = null,
                winningWeightKg = 180.0,
                weekDate = "2024-12-20"
            ),
            PreviousWinner(
                challengeId = "mof_50",
                exerciseName = "Deadlift",
                winnerId = "user2",
                winnerName = "Jordan M.",
                winnerAvatar = null,
                winningWeightKg = 220.0,
                weekDate = "2024-12-13"
            ),
            PreviousWinner(
                challengeId = "mof_49",
                exerciseName = "Bench Press",
                winnerId = "user3",
                winnerName = "Sam K.",
                winnerAvatar = null,
                winningWeightKg = 140.0,
                weekDate = "2024-12-06"
            )
        )
    }
}
