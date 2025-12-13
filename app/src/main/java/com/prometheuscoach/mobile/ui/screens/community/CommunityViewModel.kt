package com.prometheuscoach.mobile.ui.screens.community

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prometheuscoach.mobile.data.model.*
import com.prometheuscoach.mobile.data.repository.AuthRepository
import com.prometheuscoach.mobile.data.repository.CommunityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Community Feed functionality.
 * Handles feed loading, pagination, likes, comments, and post creation.
 */
@HiltViewModel
class CommunityViewModel @Inject constructor(
    private val communityRepository: CommunityRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    companion object {
        private const val TAG = "CommunityViewModel"
        private const val PAGE_SIZE = 20
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STATE
    // ═══════════════════════════════════════════════════════════════════════

    private val _feedState = MutableStateFlow(FeedState())
    val feedState: StateFlow<FeedState> = _feedState.asStateFlow()

    private val _postDetailState = MutableStateFlow(PostDetailState())
    val postDetailState: StateFlow<PostDetailState> = _postDetailState.asStateFlow()

    private val _createPostState = MutableStateFlow(CreatePostState())
    val createPostState: StateFlow<CreatePostState> = _createPostState.asStateFlow()

    private var currentOffset = 0

    // ═══════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════

    init {
        loadFeed(refresh = true)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FEED LOADING
    // ═══════════════════════════════════════════════════════════════════════

    fun loadFeed(refresh: Boolean = false) {
        if (refresh) {
            currentOffset = 0
            _feedState.update { it.copy(isLoading = true, error = null) }
        } else {
            if (_feedState.value.isLoadingMore || !_feedState.value.hasMore) return
            _feedState.update { it.copy(isLoadingMore = true) }
        }

        viewModelScope.launch {
            try {
                val result = when (_feedState.value.currentTab) {
                    FeedTab.FOLLOWING -> communityRepository.getFeed(PAGE_SIZE, currentOffset)
                    FeedTab.DISCOVER -> communityRepository.getDiscoverFeed(PAGE_SIZE, currentOffset)
                }

                result.onSuccess { posts ->
                    val allPosts = if (refresh) {
                        posts
                    } else {
                        _feedState.value.posts + posts
                    }

                    currentOffset = allPosts.size

                    _feedState.update {
                        it.copy(
                            posts = allPosts,
                            isLoading = false,
                            isLoadingMore = false,
                            error = null,
                            hasMore = posts.size >= PAGE_SIZE
                        )
                    }

                    Log.d(TAG, "Loaded ${posts.size} posts, total: ${allPosts.size}")
                }.onFailure { error ->
                    Log.e(TAG, "Error loading feed: ${error.message}")
                    _feedState.update {
                        it.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            error = error.message
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading feed: ${e.message}")
                _feedState.update {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        error = e.message
                    )
                }
            }
        }
    }

    fun loadMore() {
        loadFeed(refresh = false)
    }

    fun refresh() {
        loadFeed(refresh = true)
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TAB SWITCHING
    // ═══════════════════════════════════════════════════════════════════════

    fun switchTab(tab: FeedTab) {
        if (_feedState.value.currentTab != tab) {
            _feedState.update {
                it.copy(
                    currentTab = tab,
                    posts = emptyList(),
                    hasMore = true
                )
            }
            currentOffset = 0
            loadFeed(refresh = true)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LIKES
    // ═══════════════════════════════════════════════════════════════════════

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            val post = _feedState.value.posts.find { it.id == postId } ?: return@launch
            val isCurrentlyLiked = post.isLiked

            // Optimistic update
            updatePostInFeed(postId) { currentPost ->
                currentPost.copy(
                    isLiked = !isCurrentlyLiked,
                    likesCount = if (isCurrentlyLiked) currentPost.likesCount - 1 else currentPost.likesCount + 1
                )
            }

            // Make API call
            val result = if (isCurrentlyLiked) {
                communityRepository.unlikePost(postId)
            } else {
                communityRepository.likePost(postId)
            }

            // Revert on failure
            result.onFailure {
                Log.e(TAG, "Error toggling like: ${it.message}")
                updatePostInFeed(postId) { currentPost ->
                    currentPost.copy(
                        isLiked = isCurrentlyLiked,
                        likesCount = if (isCurrentlyLiked) currentPost.likesCount + 1 else currentPost.likesCount - 1
                    )
                }
            }
        }
    }

    private fun updatePostInFeed(postId: String, transform: (FeedPost) -> FeedPost) {
        _feedState.update { state ->
            state.copy(
                posts = state.posts.map { post ->
                    if (post.id == postId) transform(post) else post
                }
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // POST DETAIL
    // ═══════════════════════════════════════════════════════════════════════

    fun loadPostDetail(postId: String) {
        _postDetailState.update { PostDetailState(isLoading = true) }

        viewModelScope.launch {
            try {
                communityRepository.getPost(postId).onSuccess { post ->
                    _postDetailState.update {
                        it.copy(post = post, isLoading = false)
                    }
                    loadComments(postId)
                }.onFailure { error ->
                    _postDetailState.update {
                        it.copy(isLoading = false, error = error.message)
                    }
                }
            } catch (e: Exception) {
                _postDetailState.update {
                    it.copy(isLoading = false, error = e.message)
                }
            }
        }
    }

    private fun loadComments(postId: String) {
        _postDetailState.update { it.copy(isLoadingComments = true) }

        viewModelScope.launch {
            communityRepository.getComments(postId).onSuccess { comments ->
                _postDetailState.update {
                    it.copy(comments = comments, isLoadingComments = false)
                }
            }.onFailure { error ->
                Log.e(TAG, "Error loading comments: ${error.message}")
                _postDetailState.update { it.copy(isLoadingComments = false) }
            }
        }
    }

    fun addComment(postId: String, content: String) {
        viewModelScope.launch {
            communityRepository.addComment(postId, content).onSuccess { comment ->
                _postDetailState.update {
                    it.copy(comments = it.comments + comment)
                }
                updatePostInFeed(postId) { post ->
                    post.copy(commentsCount = post.commentsCount + 1)
                }
                Log.d(TAG, "Comment added")
            }.onFailure { error ->
                Log.e(TAG, "Error adding comment: ${error.message}")
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // POST CREATION
    // ═══════════════════════════════════════════════════════════════════════

    fun updateCreatePostType(postType: PostType) {
        _createPostState.update { it.copy(postType = postType) }
    }

    fun updateCreatePostTitle(title: String) {
        _createPostState.update { it.copy(title = title) }
    }

    fun updateCreatePostContent(content: String) {
        _createPostState.update { it.copy(content = content) }
    }

    fun updateCreatePostVisibility(visibility: PostVisibility) {
        _createPostState.update { it.copy(visibility = visibility) }
    }

    fun createPost() {
        // Posts are workout-based and created by clients, not coaches
        _createPostState.update {
            it.copy(error = "Posts are created automatically when clients share their workouts")
        }
    }

    fun resetCreatePostState() {
        _createPostState.update { CreatePostState() }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            communityRepository.deletePost(postId).onSuccess {
                _feedState.update { state ->
                    state.copy(posts = state.posts.filter { it.id != postId })
                }
                Log.d(TAG, "Post deleted")
            }.onFailure { error ->
                Log.e(TAG, "Error deleting post: ${error.message}")
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FOLLOW
    // ═══════════════════════════════════════════════════════════════════════

    fun followUser(userId: String) {
        viewModelScope.launch {
            communityRepository.followUser(userId).onSuccess {
                Log.d(TAG, "Followed user: $userId")
            }.onFailure { error ->
                Log.e(TAG, "Error following user: ${error.message}")
            }
        }
    }

    fun unfollowUser(userId: String) {
        viewModelScope.launch {
            communityRepository.unfollowUser(userId).onSuccess {
                Log.d(TAG, "Unfollowed user: $userId")
            }.onFailure { error ->
                Log.e(TAG, "Error unfollowing user: ${error.message}")
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    fun clearError() {
        _feedState.update { it.copy(error = null) }
    }

    fun clearPostDetail() {
        _postDetailState.update { PostDetailState() }
    }

    fun isCurrentUser(userId: String): Boolean {
        return authRepository.getCurrentUserId() == userId
    }
}