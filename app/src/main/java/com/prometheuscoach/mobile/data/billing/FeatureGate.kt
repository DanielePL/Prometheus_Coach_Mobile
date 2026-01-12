package com.prometheuscoach.mobile.data.billing

import com.prometheuscoach.mobile.data.model.PlanTier
import com.prometheuscoach.mobile.data.model.SubscriptionInfo
import com.prometheuscoach.mobile.data.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Available features that can be gated by subscription plan
 */
enum class Feature {
    // AI-powered features (Pro only)
    AI_WORKOUT_GENERATOR,
    AI_MEAL_PLANNER,
    AI_EXERCISE_SUGGESTIONS,
    AI_COACHING_INSIGHTS,
    AI_CHAT_ASSISTANT,

    // Other Pro features
    ADVANCED_ANALYTICS,
    CUSTOM_BRANDING,
    PRIORITY_SUPPORT;

    /**
     * Check if this feature requires Pro plan
     */
    val requiresPro: Boolean
        get() = when (this) {
            AI_WORKOUT_GENERATOR,
            AI_MEAL_PLANNER,
            AI_EXERCISE_SUGGESTIONS,
            AI_COACHING_INSIGHTS,
            AI_CHAT_ASSISTANT,
            ADVANCED_ANALYTICS,
            CUSTOM_BRANDING,
            PRIORITY_SUPPORT -> true
        }

    /**
     * Human-readable feature name for UI display
     */
    val displayName: String
        get() = when (this) {
            AI_WORKOUT_GENERATOR -> "AI Workout Generator"
            AI_MEAL_PLANNER -> "AI Meal Planner"
            AI_EXERCISE_SUGGESTIONS -> "AI Exercise Suggestions"
            AI_COACHING_INSIGHTS -> "AI Coaching Insights"
            AI_CHAT_ASSISTANT -> "AI Chat Assistant"
            ADVANCED_ANALYTICS -> "Advanced Analytics"
            CUSTOM_BRANDING -> "Custom Branding"
            PRIORITY_SUPPORT -> "Priority Support"
        }

    /**
     * Description for upgrade prompts
     */
    val description: String
        get() = when (this) {
            AI_WORKOUT_GENERATOR -> "Generate personalized workout plans with AI"
            AI_MEAL_PLANNER -> "Create custom meal plans powered by AI"
            AI_EXERCISE_SUGGESTIONS -> "Get AI-powered exercise recommendations"
            AI_COACHING_INSIGHTS -> "Receive AI-driven coaching insights"
            AI_CHAT_ASSISTANT -> "Chat with an AI assistant for coaching help"
            ADVANCED_ANALYTICS -> "Access detailed performance analytics"
            CUSTOM_BRANDING -> "Customize your coaching brand"
            PRIORITY_SUPPORT -> "Get priority customer support"
        }
}

/**
 * FeatureGate manages access to subscription-gated features.
 *
 * Use this to check if a user has access to specific features
 * based on their subscription plan.
 */
@Singleton
class FeatureGate @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository
) {
    /**
     * Flow of current subscription info for reactive UI updates
     */
    val subscriptionInfo: Flow<SubscriptionInfo> = subscriptionRepository.subscriptionInfo

    /**
     * Flow that emits true if user has Pro plan
     */
    val hasProAccess: Flow<Boolean> = subscriptionInfo.map { it.hasAI }

    /**
     * Check if a specific feature is available synchronously.
     * Use this for one-time checks, not for UI state.
     */
    fun isFeatureAvailable(feature: Feature): Boolean {
        val info = subscriptionRepository.getCurrentSubscriptionInfo()
        return isFeatureAvailableFor(feature, info)
    }

    /**
     * Flow that emits whether a specific feature is available.
     * Use this for reactive UI that should update when subscription changes.
     */
    fun observeFeatureAvailability(feature: Feature): Flow<Boolean> {
        return subscriptionInfo.map { info ->
            isFeatureAvailableFor(feature, info)
        }
    }

    /**
     * Check if any AI feature is available
     */
    fun hasAnyAIFeature(): Boolean {
        return subscriptionRepository.getCurrentSubscriptionInfo().hasAI
    }

    /**
     * Flow for observing AI feature access
     */
    fun observeAIAccess(): Flow<Boolean> {
        return subscriptionInfo.map { it.hasAI }
    }

    /**
     * Get the current plan tier
     */
    fun getCurrentPlanTier(): PlanTier {
        val info = subscriptionRepository.getCurrentSubscriptionInfo()
        return info.subscription?.planTier ?: PlanTier.BASIC
    }

    /**
     * Get all unavailable features for the current plan
     */
    fun getUnavailableFeatures(): List<Feature> {
        val info = subscriptionRepository.getCurrentSubscriptionInfo()
        return Feature.entries.filter { !isFeatureAvailableFor(it, info) }
    }

    /**
     * Get all available features for the current plan
     */
    fun getAvailableFeatures(): List<Feature> {
        val info = subscriptionRepository.getCurrentSubscriptionInfo()
        return Feature.entries.filter { isFeatureAvailableFor(it, info) }
    }

    /**
     * Internal helper to check feature availability
     */
    private fun isFeatureAvailableFor(feature: Feature, info: SubscriptionInfo): Boolean {
        // If feature requires Pro, check if user has AI access (Pro plan)
        return if (feature.requiresPro) {
            info.hasAI
        } else {
            // Non-Pro features are available if user has any active subscription
            info.canAccessApp
        }
    }
}
