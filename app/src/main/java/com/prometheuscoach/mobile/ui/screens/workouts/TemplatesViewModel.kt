package com.prometheuscoach.mobile.ui.screens.workouts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prometheuscoach.mobile.data.model.*
import com.prometheuscoach.mobile.data.repository.TemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TemplatesState(
    // Categories & Templates
    val categories: List<TemplateCategory> = emptyList(),
    val systemTemplates: Map<TemplateCategory, List<TemplateSummary>> = emptyMap(),
    val coachTemplates: List<TemplateSummary> = emptyList(),
    val favoriteTemplates: List<TemplateSummary> = emptyList(),

    // Filters
    val selectedCategory: TemplateCategory? = null,
    val selectedLevel: FitnessLevel? = null,
    val searchQuery: String = "",

    // UI State
    val selectedSubTab: TemplateSubTab = TemplateSubTab.SYSTEM,
    val isLoading: Boolean = false,
    val error: String? = null,

    // Clone dialog
    val showCloneDialog: Boolean = false,
    val cloneTemplateId: String? = null,
    val cloneTemplateName: String? = null,
    val isCloning: Boolean = false,
    val cloneError: String? = null
)

enum class TemplateSubTab {
    SYSTEM,
    MY_TEMPLATES,
    FAVORITES
}

@HiltViewModel
class TemplatesViewModel @Inject constructor(
    private val templateRepository: TemplateRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TemplatesState())
    val state: StateFlow<TemplatesState> = _state.asStateFlow()

    init {
        loadTemplates()
    }

    fun loadTemplates() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            try {
                // Load categories
                val categories = templateRepository.getCategories().getOrElse { emptyList() }

                // Load system templates grouped by category
                val systemTemplates = templateRepository.getSystemTemplates().getOrElse { emptyMap() }

                // Load coach templates
                val coachTemplates = templateRepository.getCoachTemplates().getOrElse { emptyList() }

                // Load favorites
                val favoriteTemplates = templateRepository.getFavoriteTemplates().getOrElse { emptyList() }

                _state.value = _state.value.copy(
                    categories = categories,
                    systemTemplates = systemTemplates,
                    coachTemplates = coachTemplates,
                    favoriteTemplates = favoriteTemplates,
                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load templates"
                )
            }
        }
    }

    fun refresh() {
        loadTemplates()
    }

    fun selectSubTab(tab: TemplateSubTab) {
        _state.value = _state.value.copy(selectedSubTab = tab)
    }

    fun selectCategory(category: TemplateCategory?) {
        _state.value = _state.value.copy(selectedCategory = category)
    }

    fun selectLevel(level: FitnessLevel?) {
        _state.value = _state.value.copy(selectedLevel = level)
    }

    fun setSearchQuery(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
    }

    fun toggleFavorite(templateId: String) {
        viewModelScope.launch {
            templateRepository.toggleFavorite(templateId)
                .onSuccess { isFavorite ->
                    // Refresh favorites
                    val favorites = templateRepository.getFavoriteTemplates().getOrElse { emptyList() }
                    _state.value = _state.value.copy(favoriteTemplates = favorites)

                    // Update system templates if needed
                    val updatedSystem = _state.value.systemTemplates.mapValues { (_, templates) ->
                        templates.map {
                            if (it.id == templateId) it.copy(isFavorite = isFavorite) else it
                        }
                    }

                    // Update coach templates if needed
                    val updatedCoach = _state.value.coachTemplates.map {
                        if (it.id == templateId) it.copy(isFavorite = isFavorite) else it
                    }

                    _state.value = _state.value.copy(
                        systemTemplates = updatedSystem,
                        coachTemplates = updatedCoach
                    )
                }
        }
    }

    // ==================== CLONE OPERATIONS ====================

    fun showCloneDialog(templateId: String, templateName: String) {
        _state.value = _state.value.copy(
            showCloneDialog = true,
            cloneTemplateId = templateId,
            cloneTemplateName = templateName,
            cloneError = null
        )
    }

    fun dismissCloneDialog() {
        _state.value = _state.value.copy(
            showCloneDialog = false,
            cloneTemplateId = null,
            cloneTemplateName = null,
            isCloning = false,
            cloneError = null
        )
    }

    suspend fun cloneTemplate(
        level: FitnessLevel,
        scalingPercentage: Int,
        customName: String?
    ): Result<String> {
        val templateId = _state.value.cloneTemplateId
            ?: return Result.failure(Exception("No template selected"))

        _state.value = _state.value.copy(isCloning = true, cloneError = null)

        val config = TemplateCloneConfig(
            templateId = templateId,
            targetLevel = level,
            scalingPercentage = scalingPercentage,
            customName = customName
        )

        return templateRepository.cloneTemplateToWorkout(config)
            .onSuccess { workoutId ->
                _state.value = _state.value.copy(
                    isCloning = false,
                    showCloneDialog = false,
                    cloneTemplateId = null,
                    cloneTemplateName = null
                )
            }
            .onFailure { error ->
                _state.value = _state.value.copy(
                    isCloning = false,
                    cloneError = error.message ?: "Failed to clone template"
                )
            }
    }

    // ==================== CREATE/DELETE ====================

    suspend fun createTemplate(
        name: String,
        description: String?,
        categoryId: String?
    ): Result<WorkoutTemplate> {
        return templateRepository.createTemplate(
            name = name,
            description = description,
            categoryId = categoryId
        ).onSuccess {
            loadTemplates()
        }
    }

    suspend fun deleteTemplate(templateId: String): Result<Unit> {
        return templateRepository.deleteTemplate(templateId)
            .onSuccess {
                loadTemplates()
            }
    }

    // ==================== SAVE WORKOUT AS TEMPLATE ====================

    suspend fun saveWorkoutAsTemplate(
        workoutId: String,
        name: String,
        categoryId: String?
    ): Result<WorkoutTemplate> {
        return templateRepository.saveWorkoutAsTemplate(
            workoutId = workoutId,
            name = name,
            categoryId = categoryId
        ).onSuccess {
            loadTemplates()
        }
    }

    // ==================== FILTERED DATA ====================

    fun getFilteredSystemTemplates(): Map<TemplateCategory, List<TemplateSummary>> {
        val state = _state.value
        var filtered = state.systemTemplates

        // Filter by selected category
        state.selectedCategory?.let { category ->
            filtered = filtered.filterKeys { it.id == category.id }
        }

        // Filter by level
        state.selectedLevel?.let { level ->
            filtered = filtered.mapValues { (_, templates) ->
                templates.filter { it.level == level }
            }.filterValues { it.isNotEmpty() }
        }

        // Filter by search query
        if (state.searchQuery.isNotBlank()) {
            val query = state.searchQuery.lowercase()
            filtered = filtered.mapValues { (_, templates) ->
                templates.filter {
                    it.name.lowercase().contains(query) ||
                    it.description?.lowercase()?.contains(query) == true ||
                    it.targetMuscles.any { muscle -> muscle.lowercase().contains(query) }
                }
            }.filterValues { it.isNotEmpty() }
        }

        return filtered
    }

    fun getFilteredCoachTemplates(): List<TemplateSummary> {
        val state = _state.value
        var filtered = state.coachTemplates

        // Filter by level
        state.selectedLevel?.let { level ->
            filtered = filtered.filter { it.level == level }
        }

        // Filter by search query
        if (state.searchQuery.isNotBlank()) {
            val query = state.searchQuery.lowercase()
            filtered = filtered.filter {
                it.name.lowercase().contains(query) ||
                it.description?.lowercase()?.contains(query) == true
            }
        }

        return filtered
    }

    fun getFilteredFavorites(): List<TemplateSummary> {
        val state = _state.value
        var filtered = state.favoriteTemplates

        // Filter by search query
        if (state.searchQuery.isNotBlank()) {
            val query = state.searchQuery.lowercase()
            filtered = filtered.filter {
                it.name.lowercase().contains(query) ||
                it.description?.lowercase()?.contains(query) == true
            }
        }

        return filtered
    }
}
