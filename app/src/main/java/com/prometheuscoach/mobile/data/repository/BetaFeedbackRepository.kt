package com.prometheuscoach.mobile.data.repository

import android.os.Build
import android.util.Log
import com.prometheuscoach.mobile.BuildConfig
import com.prometheuscoach.mobile.data.model.BetaFeedbackInsert
import com.prometheuscoach.mobile.data.model.FeedbackType
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BetaFeedbackRepository - Handles beta tester feedback submission
 *
 * Responsibilities:
 * - Submit bug reports, feedback, and ideas from beta testers
 * - Track which screen the feedback was submitted from
 * - Include device info for debugging
 */
@Singleton
class BetaFeedbackRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    companion object {
        private const val TAG = "BetaFeedbackRepository"
        private const val TABLE_BETA_FEEDBACK = "beta_feedback_coach_mobile"
    }

    /**
     * Submit feedback to Supabase
     *
     * @param feedbackType One of: BUG, FEEDBACK, IDEA
     * @param message The feedback message from the user
     * @param screenName The screen where feedback was submitted from
     * @return Result indicating success or failure
     */
    suspend fun submitFeedback(
        feedbackType: FeedbackType,
        message: String,
        screenName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = getCurrentUserId()
            val username = getCurrentUsername()

            if (userId == null) {
                Log.w(TAG, "User not logged in, submitting anonymous feedback")
            }

            val feedback = BetaFeedbackInsert(
                userId = userId,
                username = username,
                screenName = screenName,
                feedbackType = feedbackType.value,
                message = message,
                appVersion = getAppVersion(),
                deviceInfo = getDeviceInfo()
            )

            Log.d(TAG, "Submitting ${feedbackType.value} from $screenName")

            supabaseClient.from(TABLE_BETA_FEEDBACK).insert(feedback)

            Log.d(TAG, "Feedback submitted successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error submitting feedback: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun getCurrentUserId(): String? {
        return try {
            supabaseClient.auth.currentUserOrNull()?.id
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current user: ${e.message}")
            null
        }
    }

    private fun getCurrentUsername(): String? {
        return try {
            supabaseClient.auth.currentUserOrNull()?.email?.substringBefore("@")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting username: ${e.message}")
            null
        }
    }

    private fun getAppVersion(): String {
        return try {
            BuildConfig.VERSION_NAME
        } catch (e: Exception) {
            "unknown"
        }
    }

    private fun getDeviceInfo(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"
    }
}
