package com.prometheuscoach.mobile.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Subscription status matching the web tool's Stripe integration
 */
enum class SubscriptionStatus {
    @SerialName("active") ACTIVE,
    @SerialName("trialing") TRIALING,
    @SerialName("past_due") PAST_DUE,
    @SerialName("canceled") CANCELED,
    @SerialName("unpaid") UNPAID,
    @SerialName("none") NONE;

    companion object {
        fun fromString(value: String?): SubscriptionStatus {
            return when (value?.lowercase()) {
                "active" -> ACTIVE
                "trialing" -> TRIALING
                "past_due" -> PAST_DUE
                "canceled" -> CANCELED
                "unpaid" -> UNPAID
                else -> NONE
            }
        }
    }
}

/**
 * Plan tier - Basic (without AI) or Pro (with AI features)
 */
enum class PlanTier {
    BASIC,
    PRO;

    companion object {
        fun fromPlanId(planId: String?): PlanTier {
            return if (planId?.startsWith("pro") == true) PRO else BASIC
        }
    }
}

/**
 * Subscription data from Supabase subscriptions table
 * This is synced from the web tool's Stripe integration
 */
@Serializable
data class Subscription(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("stripe_customer_id") val stripeCustomerId: String? = null,
    @SerialName("stripe_subscription_id") val stripeSubscriptionId: String? = null,
    @SerialName("plan_id") val planId: String,
    val status: String,
    @SerialName("current_period_start") val currentPeriodStart: String? = null,
    @SerialName("current_period_end") val currentPeriodEnd: String? = null,
    @SerialName("cancel_at_period_end") val cancelAtPeriodEnd: Boolean = false,
    @SerialName("trial_start") val trialStart: String? = null,
    @SerialName("trial_end") val trialEnd: String? = null,
    @SerialName("created_at") val createdAt: String? = null
) {
    /**
     * Get the subscription status as enum
     */
    val subscriptionStatus: SubscriptionStatus
        get() = SubscriptionStatus.fromString(status)

    /**
     * Get the plan tier (Basic or Pro)
     */
    val planTier: PlanTier
        get() = PlanTier.fromPlanId(planId)

    /**
     * Check if subscription includes AI features
     */
    val hasAI: Boolean
        get() = planTier == PlanTier.PRO

    /**
     * Get client limit based on plan ID
     * Plans: basic_10, pro_10, basic_25, pro_25, basic_50, pro_50, basic_100, pro_100
     */
    val clientLimit: Int
        get() {
            return when {
                planId.endsWith("_10") -> 10
                planId.endsWith("_25") -> 25
                planId.endsWith("_50") -> 50
                planId.endsWith("_100") -> 100
                else -> 10 // Default to smallest limit
            }
        }
}

/**
 * Comprehensive subscription info for UI consumption
 */
data class SubscriptionInfo(
    val subscription: Subscription?,
    val status: SubscriptionStatus,
    val isActive: Boolean,
    val isTrialing: Boolean,
    val trialDaysRemaining: Int?,
    val clientLimit: Int,
    val hasAI: Boolean,
    val canAccessApp: Boolean,
    val needsSubscription: Boolean,
    val planDisplayName: String
) {
    companion object {
        /**
         * Create a default "no subscription" info
         */
        fun noSubscription() = SubscriptionInfo(
            subscription = null,
            status = SubscriptionStatus.NONE,
            isActive = false,
            isTrialing = false,
            trialDaysRemaining = null,
            clientLimit = 0,
            hasAI = false,
            canAccessApp = false,
            needsSubscription = true,
            planDisplayName = "No Plan"
        )

        /**
         * Create full access info for testers (debug builds only).
         * Grants Pro-level access with 100 client limit.
         */
        fun testerAccess() = SubscriptionInfo(
            subscription = null,
            status = SubscriptionStatus.ACTIVE,
            isActive = true,
            isTrialing = false,
            trialDaysRemaining = null,
            clientLimit = 100,
            hasAI = true,  // Full access to AI features for testing
            canAccessApp = true,
            needsSubscription = false,
            planDisplayName = "Tester Access"
        )
    }
}
