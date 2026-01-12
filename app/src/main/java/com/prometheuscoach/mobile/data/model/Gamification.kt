package com.prometheuscoach.mobile.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ═══════════════════════════════════════════════════════════════════════════
// GAMIFICATION MODELS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Client gamification stats from Supabase.
 */
@Serializable
data class ClientGamificationStats(
    val id: String,
    @SerialName("user_id") val clientId: String,
    @SerialName("user_name") val clientName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val level: Int = 1,
    @SerialName("current_xp") val currentXp: Int = 0,
    @SerialName("total_xp") val totalXp: Int = 0,
    @SerialName("streak_days") val streakDays: Int = 0,
    @SerialName("longest_streak") val longestStreak: Int = 0,
    @SerialName("workouts_completed") val workoutsCompleted: Int = 0,
    @SerialName("total_volume_kg") val totalVolumeKg: Double = 0.0,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    val xpToNextLevel: Int
        get() = calculateXpForLevel(level + 1) - calculateXpForLevel(level)

    val xpProgress: Float
        get() {
            val levelStartXp = calculateXpForLevel(level)
            val levelEndXp = calculateXpForLevel(level + 1)
            val xpInLevel = totalXp - levelStartXp
            val xpNeeded = levelEndXp - levelStartXp
            return if (xpNeeded > 0) (xpInLevel.toFloat() / xpNeeded).coerceIn(0f, 1f) else 0f
        }

    val displayName: String
        get() = clientName ?: "Unknown Client"

    companion object {
        fun calculateXpForLevel(level: Int): Int {
            // XP curve: each level requires progressively more XP
            return when {
                level <= 1 -> 0
                else -> (100 * level * (level - 1) / 2)
            }
        }

        fun calculateLevelFromXp(xp: Int): Int {
            var level = 1
            while (calculateXpForLevel(level + 1) <= xp) {
                level++
            }
            return level
        }
    }
}

/**
 * Badge earned by a client.
 */
@Serializable
data class Badge(
    val id: String,
    val name: String,
    val description: String,
    @SerialName("icon_name") val iconName: String? = null,
    @SerialName("icon_url") val iconUrl: String? = null,
    val category: BadgeCategory = BadgeCategory.ACHIEVEMENT,
    val rarity: BadgeRarity = BadgeRarity.COMMON,
    @SerialName("xp_reward") val xpReward: Int = 0
)

@Serializable
enum class BadgeCategory {
    @SerialName("achievement") ACHIEVEMENT,
    @SerialName("milestone") MILESTONE,
    @SerialName("streak") STREAK,
    @SerialName("challenge") CHALLENGE,
    @SerialName("social") SOCIAL,
    @SerialName("special") SPECIAL
}

@Serializable
enum class BadgeRarity {
    @SerialName("common") COMMON,
    @SerialName("uncommon") UNCOMMON,
    @SerialName("rare") RARE,
    @SerialName("epic") EPIC,
    @SerialName("legendary") LEGENDARY
}

/**
 * User's earned badge.
 */
@Serializable
data class UserBadge(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("badge_id") val badgeId: String,
    @SerialName("earned_at") val earnedAt: String,
    val badge: Badge? = null
)

/**
 * Community challenge for gamification.
 */
@Serializable
data class GamificationChallenge(
    val id: String,
    val title: String,
    val description: String? = null,
    @SerialName("challenge_type") val challengeType: GamificationChallengeType = GamificationChallengeType.WORKOUT_COUNT,
    @SerialName("target_value") val targetValue: Int,
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String,
    @SerialName("xp_reward") val xpReward: Int = 0,
    @SerialName("badge_reward_id") val badgeRewardId: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
enum class GamificationChallengeType {
    @SerialName("workout_count") WORKOUT_COUNT,
    @SerialName("streak_days") STREAK_DAYS,
    @SerialName("volume_kg") VOLUME_KG,
    @SerialName("exercise_mastery") EXERCISE_MASTERY,
    @SerialName("max_weight") MAX_WEIGHT
}

/**
 * User's participation in a gamification challenge.
 */
@Serializable
data class GamificationChallengeEntry(
    val id: String,
    @SerialName("challenge_id") val challengeId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("user_name") val userName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("current_value") val currentValue: Double = 0.0,
    @SerialName("is_completed") val isCompleted: Boolean = false,
    @SerialName("completed_at") val completedAt: String? = null,
    val rank: Int? = null,
    @SerialName("joined_at") val joinedAt: String? = null
) {
    val progress: Float
        get() = 0f // Calculated with challenge target
}

// ═══════════════════════════════════════════════════════════════════════════
// PERSONAL CHALLENGES (COACH -> CLIENT)
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Personal challenge created by coach for a specific client.
 */
@Serializable
data class PersonalChallenge(
    val id: String,
    @SerialName("coach_id") val coachId: String,
    @SerialName("client_id") val clientId: String,
    @SerialName("client_name") val clientName: String? = null,
    val title: String,
    val description: String? = null,
    @SerialName("target_type") val targetType: PersonalChallengeType,
    @SerialName("target_value") val targetValue: Int,
    @SerialName("current_value") val currentValue: Int = 0,
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String,
    val status: PersonalChallengeStatus = PersonalChallengeStatus.ACTIVE,
    @SerialName("reward_xp") val rewardXp: Int = 0,
    @SerialName("reward_message") val rewardMessage: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("completed_at") val completedAt: String? = null
) {
    val progress: Float
        get() = if (targetValue > 0) (currentValue.toFloat() / targetValue).coerceIn(0f, 1f) else 0f

    val isCompleted: Boolean
        get() = currentValue >= targetValue || status == PersonalChallengeStatus.COMPLETED

    val daysRemaining: Int
        get() {
            // Simple calculation - in production use proper date parsing
            return 7 // Placeholder
        }
}

@Serializable
enum class PersonalChallengeType {
    @SerialName("workouts_count") WORKOUTS_COUNT,     // X Workouts in timeframe
    @SerialName("streak_days") STREAK_DAYS,           // Maintain X day streak
    @SerialName("volume_kg") VOLUME_KG,               // Lift X kg total volume
    @SerialName("exercise_sessions") EXERCISE_SESSIONS // X sessions of specific exercise
}

@Serializable
enum class PersonalChallengeStatus {
    @SerialName("active") ACTIVE,
    @SerialName("completed") COMPLETED,
    @SerialName("failed") FAILED,
    @SerialName("cancelled") CANCELLED
}

// ═══════════════════════════════════════════════════════════════════════════
// LEADERBOARD
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Leaderboard entry for gamification ranking display.
 */
@Serializable
data class GamificationLeaderboardEntry(
    val rank: Int,
    @SerialName("user_id") val userId: String,
    @SerialName("user_name") val userName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val value: Double,
    val level: Int? = null,
    @SerialName("is_client") val isClient: Boolean = false
) {
    val displayName: String
        get() = userName ?: "Unknown"
}

/**
 * Leaderboard time period.
 */
enum class LeaderboardPeriod {
    WEEKLY,
    MONTHLY,
    ALL_TIME
}

/**
 * Leaderboard type.
 */
enum class LeaderboardType {
    XP,
    STREAK,
    VOLUME,
    WORKOUTS
}

// ═══════════════════════════════════════════════════════════════════════════
// UI STATE
// ═══════════════════════════════════════════════════════════════════════════

/**
 * UI state for Gamification Dashboard.
 */
data class GamificationDashboardState(
    val isLoading: Boolean = false,
    val error: String? = null,
    // Clients overview
    val clientsStats: List<ClientGamificationStats> = emptyList(),
    val selectedClientId: String? = null,
    val selectedClientStats: ClientGamificationStats? = null,
    // Badges
    val clientBadges: List<UserBadge> = emptyList(),
    val allBadges: List<Badge> = emptyList(),
    // Challenges
    val activeChallenges: List<GamificationChallenge> = emptyList(),
    val clientChallengeEntries: List<GamificationChallengeEntry> = emptyList(),
    // Personal challenges
    val personalChallenges: List<PersonalChallenge> = emptyList(),
    val showCreateChallengeDialog: Boolean = false,
    // Leaderboard
    val leaderboard: List<GamificationLeaderboardEntry> = emptyList(),
    val leaderboardType: LeaderboardType = LeaderboardType.XP,
    val leaderboardPeriod: LeaderboardPeriod = LeaderboardPeriod.WEEKLY,
    // Tabs
    val selectedTab: GamificationTab = GamificationTab.OVERVIEW
) {
    val topClients: List<ClientGamificationStats>
        get() = clientsStats.sortedByDescending { it.totalXp }.take(5)

    val activeStreaks: List<ClientGamificationStats>
        get() = clientsStats.filter { it.streakDays > 0 }.sortedByDescending { it.streakDays }
}

enum class GamificationTab {
    OVERVIEW,
    LEADERBOARD,
    CHALLENGES,
    BADGES
}

/**
 * State for client-specific gamification view.
 */
data class ClientGamificationState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val clientId: String = "",
    val clientName: String = "",
    val stats: ClientGamificationStats? = null,
    val badges: List<UserBadge> = emptyList(),
    val challengeEntries: List<GamificationChallengeEntry> = emptyList(),
    val personalChallenges: List<PersonalChallenge> = emptyList(),
    val showCreateChallengeSheet: Boolean = false
)

/**
 * Input for creating a personal challenge.
 */
data class CreatePersonalChallengeInput(
    val clientId: String = "",
    val title: String = "",
    val description: String = "",
    val targetType: PersonalChallengeType = PersonalChallengeType.WORKOUTS_COUNT,
    val targetValue: Int = 5,
    val durationDays: Int = 7,
    val rewardXp: Int = 100,
    val rewardMessage: String = ""
) {
    val isValid: Boolean
        get() = title.isNotBlank() && targetValue > 0 && durationDays > 0
}
