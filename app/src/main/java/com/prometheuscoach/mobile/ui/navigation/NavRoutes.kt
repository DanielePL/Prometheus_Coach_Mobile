package com.prometheuscoach.mobile.ui.navigation

import kotlinx.serialization.Serializable

sealed interface NavRoute {
    @Serializable
    data object Auth : NavRoute

    @Serializable
    data object EmailVerified : NavRoute

    @Serializable
    data object Dashboard : NavRoute

    @Serializable
    data object Community : NavRoute

    @Serializable
    data object CreatePost : NavRoute

    @Serializable
    data class PostDetail(val postId: String) : NavRoute

    @Serializable
    data class UserProfile(val userId: String) : NavRoute

    @Serializable
    data class Clients(val openAddSheet: Boolean = false) : NavRoute

    @Serializable
    data class ClientDetail(val clientId: String) : NavRoute

    @Serializable
    data class ClientProgress(val clientId: String) : NavRoute

    @Serializable
    data class ClientNutrition(val clientId: String, val clientName: String) : NavRoute

    @Serializable
    data object Calendar : NavRoute

    @Serializable
    data object Conversations : NavRoute

    @Serializable
    data class Chat(val conversationId: String) : NavRoute

    @Serializable
    data object Library : NavRoute

    @Serializable
    data object Workouts : NavRoute

    @Serializable
    data class WorkoutDetail(val workoutId: String) : NavRoute

    @Serializable
    data class ExerciseDetail(val exerciseId: String) : NavRoute

    @Serializable
    data object Settings : NavRoute

    @Serializable
    data object Trends : NavRoute

    @Serializable
    data object CreateProgram : NavRoute

    @Serializable
    data class ProgramDetail(val programId: String) : NavRoute
}
