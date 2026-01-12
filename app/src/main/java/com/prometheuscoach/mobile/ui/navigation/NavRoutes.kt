package com.prometheuscoach.mobile.ui.navigation

import kotlinx.serialization.Serializable

sealed interface NavRoute {
    @Serializable
    data object Auth : NavRoute

    @Serializable
    data object EmailVerified : NavRoute

    @Serializable
    data object SubscriptionGate : NavRoute

    @Serializable
    data object Dashboard : NavRoute

    @Serializable
    data object CoachDashboard : NavRoute

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
    data class ClientVBT(val clientId: String, val clientName: String) : NavRoute

    @Serializable
    data class ClientFormAnalysis(val clientId: String, val clientName: String) : NavRoute

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
    data class TemplateDetail(val templateId: String) : NavRoute

    @Serializable
    data class ExerciseDetail(val exerciseId: String) : NavRoute

    @Serializable
    data object Settings : NavRoute

    @Serializable
    data object Account : NavRoute

    @Serializable
    data object EditCoachProfile : NavRoute

    @Serializable
    data class CoachSetCard(val coachId: String) : NavRoute

    @Serializable
    data object Trends : NavRoute

    @Serializable
    data object CreateProgram : NavRoute

    @Serializable
    data class ProgramDetail(val programId: String) : NavRoute

    @Serializable
    data object AIAssistant : NavRoute

    @Serializable
    data class AIAssistantWithContext(
        val contextType: String,
        val contextId: String? = null,
        val contextName: String? = null
    ) : NavRoute

    @Serializable
    data object GamificationDashboard : NavRoute

    @Serializable
    data class ClientGamification(val clientId: String, val clientName: String) : NavRoute

    @Serializable
    data object Challenges : NavRoute

    @Serializable
    data class ChallengeDetail(val challengeId: String) : NavRoute
}
