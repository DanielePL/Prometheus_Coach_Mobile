package com.prometheuscoach.mobile.ui.screens.formanalysis

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prometheuscoach.mobile.data.model.*
import com.prometheuscoach.mobile.data.repository.FormAnalysisRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FormAnalysisViewModel @Inject constructor(
    private val formAnalysisRepository: FormAnalysisRepository
) : ViewModel() {

    private val _state = MutableStateFlow(FormAnalysisState())
    val state: StateFlow<FormAnalysisState> = _state.asStateFlow()

    init {
        // Observe upload progress
        viewModelScope.launch {
            formAnalysisRepository.uploadProgress.collect { progress ->
                _state.update { it.copy(uploadProgress = progress) }
            }
        }
    }

    /**
     * Initialize the screen for a client.
     */
    fun loadClient(clientId: String, clientName: String) {
        _state.update {
            it.copy(
                clientId = clientId,
                clientName = clientName,
                isLoading = true,
                error = null
            )
        }

        viewModelScope.launch {
            loadVideos()
        }
    }

    /**
     * Load videos for the current client.
     */
    private suspend fun loadVideos() {
        formAnalysisRepository.getClientVideos(_state.value.clientId)
            .onSuccess { videos ->
                _state.update {
                    it.copy(
                        videos = videos,
                        isLoading = false,
                        error = null
                    )
                }
            }
            .onFailure { e ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load videos"
                    )
                }
            }
    }

    /**
     * Refresh the videos list.
     */
    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            loadVideos()
        }
    }

    /**
     * Select a video for viewing/playback.
     */
    fun selectVideo(video: FormAnalysisVideo?) {
        _state.update { it.copy(selectedVideo = video) }

        // Load timestamps if video selected
        video?.let { v ->
            viewModelScope.launch {
                formAnalysisRepository.getVideoTimestamps(v.id)
                    .onSuccess { timestamps ->
                        _state.update { it.copy(timestamps = timestamps) }
                    }
            }
        }
    }

    /**
     * Upload a new video.
     */
    fun uploadVideo(
        videoUri: Uri,
        exerciseName: String?,
        title: String?,
        notes: String?
    ) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isUploading = true,
                    uploadProgress = 0f,
                    uploadError = null
                )
            }

            val request = UploadVideoRequest(
                clientId = _state.value.clientId,
                exerciseName = exerciseName,
                title = title,
                notes = notes
            )

            formAnalysisRepository.uploadVideo(
                clientId = _state.value.clientId,
                videoUri = videoUri,
                request = request
            )
                .onSuccess { video ->
                    _state.update {
                        it.copy(
                            videos = listOf(video) + it.videos,
                            isUploading = false,
                            uploadProgress = 0f
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isUploading = false,
                            uploadProgress = 0f,
                            uploadError = e.message ?: "Upload failed"
                        )
                    }
                }
        }
    }

    /**
     * Add feedback to a video.
     */
    fun addFeedback(videoId: String, feedback: String, rating: Int?) {
        viewModelScope.launch {
            formAnalysisRepository.addFeedback(videoId, feedback, rating)
                .onSuccess { updatedVideo ->
                    _state.update { state ->
                        state.copy(
                            videos = state.videos.map {
                                if (it.id == videoId) updatedVideo else it
                            },
                            selectedVideo = if (state.selectedVideo?.id == videoId) updatedVideo else state.selectedVideo
                        )
                    }
                }
        }
    }

    /**
     * Archive a video.
     */
    fun archiveVideo(videoId: String) {
        viewModelScope.launch {
            formAnalysisRepository.updateStatus(videoId, AnalysisStatus.ARCHIVED)
                .onSuccess {
                    _state.update { state ->
                        state.copy(
                            videos = state.videos.map {
                                if (it.id == videoId) it.copy(status = AnalysisStatus.ARCHIVED) else it
                            }
                        )
                    }
                }
        }
    }

    /**
     * Delete a video.
     */
    fun deleteVideo(videoId: String) {
        viewModelScope.launch {
            formAnalysisRepository.deleteVideo(videoId)
                .onSuccess {
                    _state.update { state ->
                        state.copy(
                            videos = state.videos.filter { it.id != videoId },
                            selectedVideo = if (state.selectedVideo?.id == videoId) null else state.selectedVideo
                        )
                    }
                }
        }
    }

    /**
     * Add a timestamp marker.
     */
    fun addTimestamp(
        timestampSeconds: Double,
        label: String,
        note: String?,
        markerType: MarkerType
    ) {
        val videoId = _state.value.selectedVideo?.id ?: return

        viewModelScope.launch {
            formAnalysisRepository.addTimestamp(
                videoId = videoId,
                timestampSeconds = timestampSeconds,
                label = label,
                note = note,
                markerType = markerType
            )
                .onSuccess { timestamp ->
                    _state.update { state ->
                        state.copy(
                            timestamps = (state.timestamps + timestamp).sortedBy { it.timestampSeconds }
                        )
                    }
                }
        }
    }

    /**
     * Delete a timestamp marker.
     */
    fun deleteTimestamp(timestampId: String) {
        viewModelScope.launch {
            formAnalysisRepository.deleteTimestamp(timestampId)
                .onSuccess {
                    _state.update { state ->
                        state.copy(
                            timestamps = state.timestamps.filter { it.id != timestampId }
                        )
                    }
                }
        }
    }

    /**
     * Set filter by status.
     */
    fun setStatusFilter(status: AnalysisStatus?) {
        _state.update { it.copy(statusFilter = status) }
    }

    /**
     * Set filter by exercise.
     */
    fun setExerciseFilter(exercise: String?) {
        _state.update { it.copy(exerciseFilter = exercise) }
    }

    /**
     * Clear upload error.
     */
    fun clearUploadError() {
        _state.update { it.copy(uploadError = null) }
    }

    /**
     * Update playback position.
     */
    fun updatePlaybackPosition(position: Long, duration: Long) {
        _state.update {
            it.copy(
                currentPosition = position,
                duration = duration
            )
        }
    }

    /**
     * Set playing state.
     */
    fun setPlaying(isPlaying: Boolean) {
        _state.update { it.copy(isPlaying = isPlaying) }
    }
}
