package com.prometheuscoach.mobile.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Feedback types that can be submitted during beta testing
 */
enum class FeedbackType(val value: String) {
    BUG("bug"),
    FEEDBACK("feedback"),
    IDEA("idea")
}

/**
 * Data class for inserting feedback into Supabase
 */
@Serializable
data class BetaFeedbackInsert(
    @SerialName("user_id")
    val userId: String?,
    @SerialName("username")
    val username: String?,
    @SerialName("screen_name")
    val screenName: String,
    @SerialName("feedback_type")
    val feedbackType: String,
    @SerialName("message")
    val message: String,
    @SerialName("app_version")
    val appVersion: String,
    @SerialName("device_info")
    val deviceInfo: String
)
