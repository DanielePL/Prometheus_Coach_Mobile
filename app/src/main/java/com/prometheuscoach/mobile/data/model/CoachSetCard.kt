package com.prometheuscoach.mobile.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Public coach profile data for the SetCard.
 * This is the information that other users can see about a coach.
 */
@Serializable
data class CoachSetCard(
    val id: String,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val bio: String? = null,
    val specialization: String? = null,

    // Social Media Handles
    @SerialName("instagram_handle") val instagramHandle: String? = null,
    @SerialName("tiktok_handle") val tiktokHandle: String? = null,
    @SerialName("youtube_handle") val youtubeHandle: String? = null,
    @SerialName("twitter_handle") val twitterHandle: String? = null,
    @SerialName("website_url") val websiteUrl: String? = null,

    // Public stats (optional - coach can choose to show)
    @SerialName("years_experience") val yearsExperience: Int? = null,
    @SerialName("certifications") val certifications: List<String>? = null,

    // Metadata
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null
) {
    val displayName: String get() = fullName ?: "Coach"

    val hasSocialMedia: Boolean get() =
        !instagramHandle.isNullOrBlank() ||
        !tiktokHandle.isNullOrBlank() ||
        !youtubeHandle.isNullOrBlank() ||
        !twitterHandle.isNullOrBlank() ||
        !websiteUrl.isNullOrBlank()

    val instagramUrl: String? get() = instagramHandle?.let { handle ->
        if (handle.startsWith("http")) handle else "https://instagram.com/${handle.removePrefix("@")}"
    }

    val tiktokUrl: String? get() = tiktokHandle?.let { handle ->
        if (handle.startsWith("http")) handle else "https://tiktok.com/@${handle.removePrefix("@")}"
    }

    val youtubeUrl: String? get() = youtubeHandle?.let { handle ->
        if (handle.startsWith("http")) handle else "https://youtube.com/@${handle.removePrefix("@")}"
    }

    val twitterUrl: String? get() = twitterHandle?.let { handle ->
        if (handle.startsWith("http")) handle else "https://twitter.com/${handle.removePrefix("@")}"
    }
}

/**
 * Request model for updating coach profile.
 */
@Serializable
data class UpdateCoachProfileRequest(
    val bio: String? = null,
    val specialization: String? = null,
    @SerialName("instagram_handle") val instagramHandle: String? = null,
    @SerialName("tiktok_handle") val tiktokHandle: String? = null,
    @SerialName("youtube_handle") val youtubeHandle: String? = null,
    @SerialName("twitter_handle") val twitterHandle: String? = null,
    @SerialName("website_url") val websiteUrl: String? = null,
    @SerialName("years_experience") val yearsExperience: Int? = null,
    @SerialName("certifications") val certifications: List<String>? = null
)

/**
 * Social media platform enum for UI.
 */
enum class SocialMediaPlatform(
    val displayName: String,
    val placeholder: String,
    val urlPrefix: String
) {
    INSTAGRAM("Instagram", "@username", "https://instagram.com/"),
    TIKTOK("TikTok", "@username", "https://tiktok.com/@"),
    YOUTUBE("YouTube", "@channel", "https://youtube.com/@"),
    TWITTER("X / Twitter", "@username", "https://twitter.com/"),
    WEBSITE("Website", "https://...", "")
}
