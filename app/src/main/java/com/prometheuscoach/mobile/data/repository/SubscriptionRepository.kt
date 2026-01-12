package com.prometheuscoach.mobile.data.repository

import android.util.Log
import com.prometheuscoach.mobile.BuildConfig
import com.prometheuscoach.mobile.data.model.PlanTier
import com.prometheuscoach.mobile.data.model.Subscription
import com.prometheuscoach.mobile.data.model.SubscriptionInfo
import com.prometheuscoach.mobile.data.model.SubscriptionStatus
import com.prometheuscoach.mobile.data.notification.TrialNotificationManager
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SubscriptionRepository"
private const val REALTIME_CHANNEL = "subscription-changes"

@Singleton
class SubscriptionRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val trialNotificationManager: TrialNotificationManager
) {
    // If bypass is enabled, start with tester access; otherwise start with no subscription
    private val _subscriptionInfo = MutableStateFlow(
        if (BuildConfig.BYPASS_SUBSCRIPTION) SubscriptionInfo.testerAccess()
        else SubscriptionInfo.noSubscription()
    )
    val subscriptionInfo: Flow<SubscriptionInfo> = _subscriptionInfo.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var realtimeSubscriptionActive = false

    /**
     * Fetch the current user's subscription from Supabase.
     * This should be called after login to check if the coach has access.
     * In debug builds with BYPASS_SUBSCRIPTION=true, grants full access for testing.
     */
    suspend fun fetchSubscription(): Result<SubscriptionInfo> {
        // Bypass subscription check for testers in debug builds
        if (BuildConfig.BYPASS_SUBSCRIPTION) {
            Log.d(TAG, "BYPASS_SUBSCRIPTION enabled - granting full tester access")
            val testerInfo = SubscriptionInfo.testerAccess()
            _subscriptionInfo.value = testerInfo
            return Result.success(testerInfo)
        }

        return try {
            val userId = supabaseClient.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Not authenticated"))

            Log.d(TAG, "Fetching subscription for user: $userId")

            val subscription = supabaseClient.postgrest
                .from("subscriptions")
                .select {
                    filter { eq("user_id", userId) }
                }
                .decodeSingleOrNull<Subscription>()

            val info = buildSubscriptionInfo(subscription)
            _subscriptionInfo.value = info

            Log.d(TAG, "Subscription status: ${info.status}, canAccessApp: ${info.canAccessApp}")

            // Schedule trial reminder notifications if user is in trial
            if (info.isTrialing) {
                trialNotificationManager.scheduleTrialReminders(info)
            }

            Result.success(info)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch subscription", e)
            val noSub = SubscriptionInfo.noSubscription()
            _subscriptionInfo.value = noSub
            Result.failure(e)
        }
    }

    /**
     * Check if the current user can access the app.
     * Returns true if subscription is active or in trial.
     */
    suspend fun canAccessApp(): Boolean {
        val result = fetchSubscription()
        return result.getOrNull()?.canAccessApp ?: false
    }

    /**
     * Get the current subscription info without fetching.
     * Use fetchSubscription() to refresh the data first.
     */
    fun getCurrentSubscriptionInfo(): SubscriptionInfo {
        return _subscriptionInfo.value
    }

    /**
     * Check if user has reached their client limit.
     */
    suspend fun checkClientLimit(): Result<ClientLimitInfo> {
        return try {
            val userId = supabaseClient.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Not authenticated"))

            val subscriptionInfo = _subscriptionInfo.value

            // Count current connected clients (accepted status only)
            val result = supabaseClient.postgrest
                .from("coach_client_connections")
                .select {
                    filter {
                        eq("coach_id", userId)
                        eq("status", "accepted")
                    }
                }
                .decodeList<Map<String, String>>()

            val currentCount = result.size
            val limit = subscriptionInfo.clientLimit
            val remaining = (limit - currentCount).coerceAtLeast(0)

            val info = ClientLimitInfo(
                canAddClient = currentCount < limit,
                currentCount = currentCount,
                limit = limit,
                remaining = remaining
            )

            Log.d(TAG, "Client limit check: $currentCount / $limit")

            Result.success(info)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check client limit", e)
            Result.failure(e)
        }
    }

    /**
     * Build SubscriptionInfo from the raw Subscription data.
     */
    private fun buildSubscriptionInfo(subscription: Subscription?): SubscriptionInfo {
        if (subscription == null) {
            return SubscriptionInfo.noSubscription()
        }

        val status = subscription.subscriptionStatus
        val now = Instant.now()

        // Check trial status
        var isTrialing = false
        var trialDaysRemaining: Int? = null

        if (status == SubscriptionStatus.TRIALING && subscription.trialEnd != null) {
            try {
                val trialEnd = Instant.parse(subscription.trialEnd)
                if (trialEnd.isAfter(now)) {
                    isTrialing = true
                    trialDaysRemaining = ChronoUnit.DAYS.between(now, trialEnd).toInt()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse trial end date: ${subscription.trialEnd}")
            }
        }

        // Subscription is active if status is ACTIVE or TRIALING
        val isActive = status == SubscriptionStatus.ACTIVE ||
                       (status == SubscriptionStatus.TRIALING && isTrialing)

        // Build plan display name
        val planDisplayName = buildPlanDisplayName(subscription)

        return SubscriptionInfo(
            subscription = subscription,
            status = status,
            isActive = isActive,
            isTrialing = isTrialing,
            trialDaysRemaining = trialDaysRemaining,
            clientLimit = subscription.clientLimit,
            hasAI = subscription.hasAI,
            canAccessApp = isActive,
            needsSubscription = !isActive,
            planDisplayName = planDisplayName
        )
    }

    /**
     * Build a human-readable plan name.
     */
    private fun buildPlanDisplayName(subscription: Subscription): String {
        val tier = if (subscription.planTier == PlanTier.PRO) "Pro" else "Basic"
        val clients = subscription.clientLimit
        return "Coach $tier ($clients clients)"
    }

    /**
     * Clear subscription info (e.g., on logout).
     */
    fun clearSubscription() {
        _subscriptionInfo.value = SubscriptionInfo.noSubscription()
        // Cancel any scheduled trial reminders
        trialNotificationManager.cancelAllReminders()
        // Stop realtime subscription
        stopRealtimeSubscription()
    }

    private var realtimeChannel: io.github.jan.supabase.realtime.RealtimeChannel? = null

    /**
     * Start listening for realtime subscription changes.
     * This should be called after successful login/subscription fetch.
     */
    fun startRealtimeSubscription() {
        val userId = supabaseClient.auth.currentUserOrNull()?.id ?: return

        if (realtimeSubscriptionActive) {
            Log.d(TAG, "Realtime subscription already active")
            return
        }

        scope.launch {
            try {
                Log.d(TAG, "Starting realtime subscription for user: $userId")

                val channel = supabaseClient.realtime.channel(REALTIME_CHANNEL)
                realtimeChannel = channel

                // Listen for changes to the user's subscription
                val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "subscriptions"
                }

                changeFlow.onEach { action ->
                    Log.d(TAG, "Received realtime subscription change: ${action::class.simpleName}")

                    when (action) {
                        is PostgresAction.Insert -> {
                            // Check if this is for our user
                            val recordUserId = action.record["user_id"]?.jsonPrimitive?.content
                            if (recordUserId == userId) {
                                Log.d(TAG, "Subscription inserted for current user")
                                handleSubscriptionChange(action.record)
                            }
                        }
                        is PostgresAction.Update -> {
                            val recordUserId = action.record["user_id"]?.jsonPrimitive?.content
                            if (recordUserId == userId) {
                                Log.d(TAG, "Subscription updated for current user")
                                handleSubscriptionChange(action.record)
                            }
                        }
                        is PostgresAction.Delete -> {
                            Log.d(TAG, "Subscription deleted")
                            // For deletes, we need to refetch since the old record is gone
                            fetchSubscription()
                        }
                        else -> {
                            Log.d(TAG, "Unknown action type")
                        }
                    }
                }.launchIn(scope)

                channel.subscribe()
                realtimeSubscriptionActive = true

                Log.d(TAG, "Realtime subscription started successfully")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to start realtime subscription", e)
                realtimeSubscriptionActive = false
            }
        }
    }

    /**
     * Stop the realtime subscription.
     */
    private fun stopRealtimeSubscription() {
        scope.launch {
            try {
                realtimeChannel?.let { channel ->
                    supabaseClient.realtime.removeChannel(channel)
                    realtimeChannel = null
                }
                realtimeSubscriptionActive = false
                Log.d(TAG, "Realtime subscription stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop realtime subscription", e)
            }
        }
    }

    /**
     * Handle a subscription change from realtime.
     */
    private suspend fun handleSubscriptionChange(record: kotlinx.serialization.json.JsonObject) {
        try {
            // Parse the subscription from the record
            val subscription = Subscription(
                id = record["id"]?.jsonPrimitive?.content ?: "",
                userId = record["user_id"]?.jsonPrimitive?.content ?: "",
                stripeCustomerId = record["stripe_customer_id"]?.jsonPrimitive?.content,
                stripeSubscriptionId = record["stripe_subscription_id"]?.jsonPrimitive?.content,
                planId = record["plan_id"]?.jsonPrimitive?.content ?: "basic_10",
                status = record["status"]?.jsonPrimitive?.content ?: "none",
                currentPeriodStart = record["current_period_start"]?.jsonPrimitive?.content,
                currentPeriodEnd = record["current_period_end"]?.jsonPrimitive?.content,
                cancelAtPeriodEnd = record["cancel_at_period_end"]?.jsonPrimitive?.content?.toBoolean() ?: false,
                trialStart = record["trial_start"]?.jsonPrimitive?.content,
                trialEnd = record["trial_end"]?.jsonPrimitive?.content,
                createdAt = record["created_at"]?.jsonPrimitive?.content
            )

            val info = buildSubscriptionInfo(subscription)
            _subscriptionInfo.value = info

            Log.d(TAG, "Subscription updated via realtime: ${info.status}, canAccessApp: ${info.canAccessApp}")

            // Update trial reminders if needed
            if (info.isTrialing) {
                trialNotificationManager.scheduleTrialReminders(info)
            } else {
                trialNotificationManager.cancelAllReminders()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse subscription from realtime record", e)
            // Fallback: fetch subscription from database
            fetchSubscription()
        }
    }
}

/**
 * Information about client limit status
 */
data class ClientLimitInfo(
    val canAddClient: Boolean,
    val currentCount: Int,
    val limit: Int,
    val remaining: Int
)
