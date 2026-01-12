package com.prometheuscoach.mobile.ui.screens.subscription

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prometheuscoach.mobile.BuildConfig
import com.prometheuscoach.mobile.data.model.SubscriptionInfo
import com.prometheuscoach.mobile.data.model.SubscriptionStatus
import com.prometheuscoach.mobile.data.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "SubscriptionViewModel"

data class SubscriptionState(
    val isLoading: Boolean = true,
    val subscriptionInfo: SubscriptionInfo = SubscriptionInfo.noSubscription(),
    val error: String? = null
)

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SubscriptionState())
    val state: StateFlow<SubscriptionState> = _state.asStateFlow()

    init {
        checkSubscription()
        observeSubscriptionChanges()
    }

    /**
     * Observe realtime subscription changes
     */
    private fun observeSubscriptionChanges() {
        subscriptionRepository.subscriptionInfo
            .onEach { info ->
                // Only update if we're not in loading state (to avoid overwriting fetch results)
                if (!_state.value.isLoading) {
                    _state.update {
                        it.copy(subscriptionInfo = info)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Check the user's subscription status.
     * In debug builds with BYPASS_SUBSCRIPTION=true, grants full access without checking.
     */
    fun checkSubscription() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            // Bypass subscription check for testers in debug builds
            if (BuildConfig.BYPASS_SUBSCRIPTION) {
                Log.d(TAG, "BYPASS_SUBSCRIPTION enabled - granting full access for testing")
                val testerInfo = SubscriptionInfo.testerAccess()
                _state.update {
                    it.copy(
                        isLoading = false,
                        subscriptionInfo = testerInfo,
                        error = null
                    )
                }
                return@launch
            }

            subscriptionRepository.fetchSubscription()
                .onSuccess { info ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            subscriptionInfo = info,
                            error = null
                        )
                    }
                    // Start listening for realtime updates
                    subscriptionRepository.startRealtimeSubscription()
                }
                .onFailure { exception ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to check subscription"
                        )
                    }
                }
        }
    }

    /**
     * Get the appropriate message for the current subscription status
     */
    fun getStatusMessage(): String {
        val info = _state.value.subscriptionInfo
        return when (info.status) {
            SubscriptionStatus.NONE ->
                "You need an active subscription to use the Coach app. Please subscribe via the web tool."

            SubscriptionStatus.TRIALING ->
                if (info.trialDaysRemaining != null && info.trialDaysRemaining > 0) {
                    "Your trial has ${info.trialDaysRemaining} days remaining."
                } else {
                    "Your trial has expired. Please subscribe to continue."
                }

            SubscriptionStatus.PAST_DUE ->
                "Your payment is overdue. Please update your payment method in the web tool."

            SubscriptionStatus.CANCELED ->
                "Your subscription has been canceled. Please resubscribe via the web tool."

            SubscriptionStatus.UNPAID ->
                "Your subscription payment failed. Please update your payment method."

            SubscriptionStatus.ACTIVE ->
                "Your subscription is active."
        }
    }

    /**
     * Get the appropriate title for the current subscription status
     */
    fun getStatusTitle(): String {
        return when (_state.value.subscriptionInfo.status) {
            SubscriptionStatus.NONE -> "Subscription Required"
            SubscriptionStatus.TRIALING -> "Trial Period"
            SubscriptionStatus.PAST_DUE -> "Payment Overdue"
            SubscriptionStatus.CANCELED -> "Subscription Ended"
            SubscriptionStatus.UNPAID -> "Payment Failed"
            SubscriptionStatus.ACTIVE -> "Subscription Active"
        }
    }

    /**
     * Check if the user can access the app
     */
    fun canAccessApp(): Boolean {
        return _state.value.subscriptionInfo.canAccessApp
    }

    /**
     * Clear subscription state (e.g., on logout)
     */
    fun clearState() {
        subscriptionRepository.clearSubscription()
        _state.update { SubscriptionState() }
    }
}
