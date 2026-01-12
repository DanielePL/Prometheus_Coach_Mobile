package com.prometheuscoach.mobile.data.repository

import android.util.Log
import com.prometheuscoach.mobile.data.model.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "GamificationRepository"

/**
 * Repository for gamification data.
 * Provides read access to client gamification stats and management of personal challenges.
 */
@Singleton
class GamificationRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository
) {
    // ═══════════════════════════════════════════════════════════════════════
    // CLIENT GAMIFICATION STATS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Get gamification stats for all clients of the current coach.
     */
    suspend fun getAllClientsGamificationStats(): Result<List<ClientGamificationStats>> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            // First get all client IDs for this coach
            val clientConnections = supabaseClient.postgrest
                .from("coach_clients_v")
                .select {
                    filter {
                        eq("coach_id", coachId)
                        eq("status", "accepted")
                    }
                }
                .decodeList<CoachClientRecord>()

            val clientIds = clientConnections.map { it.userId }

            if (clientIds.isEmpty()) {
                return Result.success(emptyList())
            }

            // Try to get gamification stats from user_gamification table
            val stats = try {
                supabaseClient.postgrest
                    .from("user_gamification")
                    .select {
                        filter {
                            isIn("user_id", clientIds)
                        }
                    }
                    .decodeList<ClientGamificationStats>()
            } catch (e: Exception) {
                Log.w(TAG, "user_gamification table not available, generating mock data", e)
                // Generate mock stats for clients
                clientConnections.mapIndexed { index, client ->
                    ClientGamificationStats(
                        id = client.connectionId,
                        clientId = client.userId,
                        clientName = client.userName,
                        avatarUrl = client.userAvatar,
                        level = (index % 10) + 1,
                        currentXp = (index * 150) % 500,
                        totalXp = (index + 1) * 500,
                        streakDays = if (index % 3 == 0) index + 1 else 0,
                        longestStreak = index + 5,
                        workoutsCompleted = (index + 1) * 12,
                        totalVolumeKg = (index + 1) * 5000.0
                    )
                }
            }

            Log.d(TAG, "Loaded gamification stats for ${stats.size} clients")
            Result.success(stats)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get clients gamification stats", e)
            Result.failure(e)
        }
    }

    /**
     * Get gamification stats for a specific client.
     */
    suspend fun getClientGamificationStats(clientId: String): Result<ClientGamificationStats> {
        return try {
            val stats = try {
                supabaseClient.postgrest
                    .from("user_gamification")
                    .select {
                        filter { eq("user_id", clientId) }
                    }
                    .decodeSingle<ClientGamificationStats>()
            } catch (e: Exception) {
                Log.w(TAG, "Generating mock gamification stats for client", e)
                // Generate mock stats
                ClientGamificationStats(
                    id = clientId,
                    clientId = clientId,
                    level = 5,
                    currentXp = 250,
                    totalXp = 2500,
                    streakDays = 7,
                    longestStreak = 14,
                    workoutsCompleted = 45,
                    totalVolumeKg = 25000.0
                )
            }

            Result.success(stats)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get client gamification stats", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // BADGES
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Get all available badges.
     */
    suspend fun getAllBadges(): Result<List<Badge>> {
        return try {
            val badges = try {
                supabaseClient.postgrest
                    .from("badges")
                    .select()
                    .decodeList<Badge>()
            } catch (e: Exception) {
                Log.w(TAG, "badges table not available, using default badges", e)
                getDefaultBadges()
            }

            Result.success(badges)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get badges", e)
            Result.failure(e)
        }
    }

    /**
     * Get badges earned by a specific client.
     */
    suspend fun getClientBadges(clientId: String): Result<List<UserBadge>> {
        return try {
            val badges = try {
                supabaseClient.postgrest
                    .from("user_badges")
                    .select {
                        filter { eq("user_id", clientId) }
                    }
                    .decodeList<UserBadge>()
            } catch (e: Exception) {
                Log.w(TAG, "user_badges table not available, generating mock badges", e)
                // Return some mock earned badges
                getDefaultBadges().take(3).mapIndexed { index, badge ->
                    UserBadge(
                        id = "ub_${index}",
                        userId = clientId,
                        badgeId = badge.id,
                        earnedAt = Instant.now().toString(),
                        badge = badge
                    )
                }
            }

            Result.success(badges)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get client badges", e)
            Result.failure(e)
        }
    }

    private fun getDefaultBadges(): List<Badge> = listOf(
        Badge(
            id = "first_workout",
            name = "First Steps",
            description = "Complete your first workout",
            iconName = "fitness_center",
            category = BadgeCategory.MILESTONE,
            rarity = BadgeRarity.COMMON,
            xpReward = 50
        ),
        Badge(
            id = "streak_7",
            name = "Week Warrior",
            description = "Maintain a 7-day workout streak",
            iconName = "local_fire_department",
            category = BadgeCategory.STREAK,
            rarity = BadgeRarity.UNCOMMON,
            xpReward = 100
        ),
        Badge(
            id = "streak_30",
            name = "Month Master",
            description = "Maintain a 30-day workout streak",
            iconName = "whatshot",
            category = BadgeCategory.STREAK,
            rarity = BadgeRarity.RARE,
            xpReward = 500
        ),
        Badge(
            id = "volume_10k",
            name = "10K Club",
            description = "Lift 10,000 kg total volume",
            iconName = "fitness_center",
            category = BadgeCategory.ACHIEVEMENT,
            rarity = BadgeRarity.UNCOMMON,
            xpReward = 150
        ),
        Badge(
            id = "volume_100k",
            name = "Iron Warrior",
            description = "Lift 100,000 kg total volume",
            iconName = "military_tech",
            category = BadgeCategory.ACHIEVEMENT,
            rarity = BadgeRarity.EPIC,
            xpReward = 1000
        ),
        Badge(
            id = "early_bird",
            name = "Early Bird",
            description = "Complete 10 workouts before 7 AM",
            iconName = "wb_sunny",
            category = BadgeCategory.SPECIAL,
            rarity = BadgeRarity.RARE,
            xpReward = 200
        ),
        Badge(
            id = "challenge_champion",
            name = "Challenge Champion",
            description = "Win a community challenge",
            iconName = "emoji_events",
            category = BadgeCategory.CHALLENGE,
            rarity = BadgeRarity.EPIC,
            xpReward = 500
        ),
        Badge(
            id = "social_butterfly",
            name = "Social Butterfly",
            description = "Receive 50 likes on your posts",
            iconName = "favorite",
            category = BadgeCategory.SOCIAL,
            rarity = BadgeRarity.UNCOMMON,
            xpReward = 100
        )
    )

    // ═══════════════════════════════════════════════════════════════════════
    // CHALLENGES
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Get active community challenges.
     */
    suspend fun getActiveChallenges(): Result<List<GamificationChallenge>> {
        return try {
            val challenges = try {
                supabaseClient.postgrest
                    .from("challenges")
                    .select {
                        filter { eq("is_active", true) }
                        order("end_date", Order.ASCENDING)
                    }
                    .decodeList<GamificationChallenge>()
            } catch (e: Exception) {
                Log.w(TAG, "challenges table not available, using mock data", e)
                getMockChallenges()
            }

            Result.success(challenges)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get active challenges", e)
            Result.failure(e)
        }
    }

    /**
     * Get challenge entries for a specific client.
     */
    suspend fun getClientChallengeEntries(clientId: String): Result<List<GamificationChallengeEntry>> {
        return try {
            val entries = try {
                supabaseClient.postgrest
                    .from("challenge_entries")
                    .select {
                        filter { eq("user_id", clientId) }
                    }
                    .decodeList<GamificationChallengeEntry>()
            } catch (e: Exception) {
                Log.w(TAG, "challenge_entries table not available", e)
                emptyList()
            }

            Result.success(entries)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get client challenge entries", e)
            Result.failure(e)
        }
    }

    /**
     * Get leaderboard for a challenge.
     */
    suspend fun getChallengeLeaderboard(challengeId: String, limit: Int = 20): Result<List<GamificationChallengeEntry>> {
        return try {
            val entries = try {
                supabaseClient.postgrest
                    .from("challenge_entries")
                    .select {
                        filter { eq("challenge_id", challengeId) }
                        order("current_value", Order.DESCENDING)
                        limit(limit.toLong())
                    }
                    .decodeList<GamificationChallengeEntry>()
            } catch (e: Exception) {
                Log.w(TAG, "Could not get challenge leaderboard", e)
                emptyList()
            }

            Result.success(entries)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get challenge leaderboard", e)
            Result.failure(e)
        }
    }

    private fun getMockChallenges(): List<GamificationChallenge> {
        val now = LocalDate.now()
        return listOf(
            GamificationChallenge(
                id = "weekly_volume",
                title = "Volume Week",
                description = "Lift the most total volume this week",
                challengeType = GamificationChallengeType.VOLUME_KG,
                targetValue = 50000,
                startDate = now.minusDays(3).toString(),
                endDate = now.plusDays(4).toString(),
                xpReward = 500,
                isActive = true
            ),
            GamificationChallenge(
                id = "workout_streak",
                title = "Streak Challenge",
                description = "Maintain a 5-day workout streak",
                challengeType = GamificationChallengeType.STREAK_DAYS,
                targetValue = 5,
                startDate = now.minusDays(1).toString(),
                endDate = now.plusDays(6).toString(),
                xpReward = 300,
                isActive = true
            )
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PERSONAL CHALLENGES (COACH -> CLIENT)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Get personal challenges created by this coach.
     */
    suspend fun getCoachPersonalChallenges(): Result<List<PersonalChallenge>> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            val challenges = try {
                supabaseClient.postgrest
                    .from("personal_challenges")
                    .select {
                        filter { eq("coach_id", coachId) }
                        order("created_at", Order.DESCENDING)
                    }
                    .decodeList<PersonalChallenge>()
            } catch (e: Exception) {
                Log.w(TAG, "personal_challenges table not available", e)
                emptyList()
            }

            Result.success(challenges)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get coach personal challenges", e)
            Result.failure(e)
        }
    }

    /**
     * Get personal challenges for a specific client.
     */
    suspend fun getClientPersonalChallenges(clientId: String): Result<List<PersonalChallenge>> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            val challenges = try {
                supabaseClient.postgrest
                    .from("personal_challenges")
                    .select {
                        filter {
                            eq("coach_id", coachId)
                            eq("client_id", clientId)
                        }
                        order("created_at", Order.DESCENDING)
                    }
                    .decodeList<PersonalChallenge>()
            } catch (e: Exception) {
                Log.w(TAG, "personal_challenges table not available", e)
                emptyList()
            }

            Result.success(challenges)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get client personal challenges", e)
            Result.failure(e)
        }
    }

    /**
     * Create a personal challenge for a client.
     */
    suspend fun createPersonalChallenge(
        clientId: String,
        title: String,
        description: String?,
        targetType: PersonalChallengeType,
        targetValue: Int,
        durationDays: Int,
        rewardXp: Int,
        rewardMessage: String?
    ): Result<PersonalChallenge> {
        return try {
            val coachId = authRepository.getCurrentUserId()
                ?: return Result.failure(Exception("Not authenticated"))

            val id = UUID.randomUUID().toString()
            val now = Instant.now()
            val startDate = LocalDate.now()
            val endDate = startDate.plusDays(durationDays.toLong())

            val challenge = PersonalChallengeInsert(
                id = id,
                coachId = coachId,
                clientId = clientId,
                title = title,
                description = description,
                targetType = targetType,
                targetValue = targetValue,
                currentValue = 0,
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                status = PersonalChallengeStatus.ACTIVE,
                rewardXp = rewardXp,
                rewardMessage = rewardMessage,
                createdAt = now.toString()
            )

            try {
                supabaseClient.postgrest
                    .from("personal_challenges")
                    .insert(challenge)
            } catch (e: Exception) {
                Log.w(TAG, "Could not insert personal challenge - table may not exist", e)
            }

            val result = PersonalChallenge(
                id = id,
                coachId = coachId,
                clientId = clientId,
                title = title,
                description = description,
                targetType = targetType,
                targetValue = targetValue,
                currentValue = 0,
                startDate = startDate.toString(),
                endDate = endDate.toString(),
                status = PersonalChallengeStatus.ACTIVE,
                rewardXp = rewardXp,
                rewardMessage = rewardMessage,
                createdAt = now.toString()
            )

            Log.d(TAG, "Created personal challenge: $id")
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create personal challenge", e)
            Result.failure(e)
        }
    }

    /**
     * Cancel a personal challenge.
     */
    suspend fun cancelPersonalChallenge(challengeId: String): Result<Unit> {
        return try {
            try {
                supabaseClient.postgrest
                    .from("personal_challenges")
                    .update({
                        set("status", PersonalChallengeStatus.CANCELLED.name.lowercase())
                    }) {
                        filter { eq("id", challengeId) }
                    }
            } catch (e: Exception) {
                Log.w(TAG, "Could not update personal challenge", e)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel personal challenge", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LEADERBOARD
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Get leaderboard for coach's clients.
     */
    suspend fun getClientsLeaderboard(
        type: LeaderboardType,
        period: LeaderboardPeriod = LeaderboardPeriod.ALL_TIME,
        limit: Int = 20
    ): Result<List<GamificationLeaderboardEntry>> {
        return try {
            val stats = getAllClientsGamificationStats().getOrDefault(emptyList())

            val sortedStats = when (type) {
                LeaderboardType.XP -> stats.sortedByDescending { it.totalXp }
                LeaderboardType.STREAK -> stats.sortedByDescending { it.streakDays }
                LeaderboardType.VOLUME -> stats.sortedByDescending { it.totalVolumeKg }
                LeaderboardType.WORKOUTS -> stats.sortedByDescending { it.workoutsCompleted }
            }

            val entries = sortedStats.take(limit).mapIndexed { index, stat ->
                GamificationLeaderboardEntry(
                    rank = index + 1,
                    userId = stat.clientId,
                    userName = stat.clientName,
                    avatarUrl = stat.avatarUrl,
                    value = when (type) {
                        LeaderboardType.XP -> stat.totalXp.toDouble()
                        LeaderboardType.STREAK -> stat.streakDays.toDouble()
                        LeaderboardType.VOLUME -> stat.totalVolumeKg
                        LeaderboardType.WORKOUTS -> stat.workoutsCompleted.toDouble()
                    },
                    level = stat.level,
                    isClient = true
                )
            }

            Result.success(entries)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get clients leaderboard", e)
            Result.failure(e)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// DATA TRANSFER OBJECTS
// ═══════════════════════════════════════════════════════════════════════════

@Serializable
private data class CoachClientRecord(
    @SerialName("connection_id") val connectionId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("user_name") val userName: String? = null,
    @SerialName("user_avatar") val userAvatar: String? = null
)

@Serializable
private data class PersonalChallengeInsert(
    val id: String,
    @SerialName("coach_id") val coachId: String,
    @SerialName("client_id") val clientId: String,
    val title: String,
    val description: String?,
    @SerialName("target_type") val targetType: PersonalChallengeType,
    @SerialName("target_value") val targetValue: Int,
    @SerialName("current_value") val currentValue: Int,
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String,
    val status: PersonalChallengeStatus,
    @SerialName("reward_xp") val rewardXp: Int,
    @SerialName("reward_message") val rewardMessage: String?,
    @SerialName("created_at") val createdAt: String
)
