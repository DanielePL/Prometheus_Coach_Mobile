package com.prometheuscoach.mobile.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ═══════════════════════════════════════════════════════════════════════════
// VELOCITY BASED TRAINING (VBT) MODELS
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Single velocity measurement from MOSSE Barbell Tracker.
 * Represents one rep with all velocity metrics.
 */
@Serializable
data class VelocityEntry(
    val id: String,
    @SerialName("user_id") val clientId: String,
    @SerialName("exercise_id") val exerciseId: String,
    @SerialName("exercise_name") val exerciseName: String? = null,
    @SerialName("load_kg") val loadKg: Double,
    @SerialName("peak_velocity") val peakVelocity: Double? = null,     // m/s
    @SerialName("mean_velocity") val meanVelocity: Double? = null,     // m/s
    @SerialName("mpv") val mpv: Double? = null,                        // Mean Propulsive Velocity
    @SerialName("power_watts") val powerWatts: Double? = null,         // Power in Watts
    @SerialName("rom_cm") val romCm: Double? = null,                   // Range of Motion in cm
    @SerialName("set_number") val setNumber: Int? = null,
    @SerialName("rep_number") val repNumber: Int? = null,
    @SerialName("set_type") val setType: String? = null,               // warmup, working, max
    @SerialName("workout_session_id") val workoutSessionId: String? = null,
    @SerialName("recorded_at") val recordedAt: String,
    @SerialName("device_id") val deviceId: String? = null              // MOSSE tracker ID
) {
    /** Primary velocity metric for display (prefer MPV > Mean > Peak) */
    val displayVelocity: Double?
        get() = mpv ?: meanVelocity ?: peakVelocity

    /** Formatted velocity string */
    val velocityFormatted: String
        get() = displayVelocity?.let { String.format("%.2f m/s", it) } ?: "-"
}

// ═══════════════════════════════════════════════════════════════════════════
// LOAD-VELOCITY PROFILE
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Load-Velocity Profile for an exercise.
 * Used for 1RM predictions and readiness assessment.
 *
 * The relationship: Velocity = intercept + (slope * Load)
 * Or: Load = (Velocity - intercept) / slope
 */
@Serializable
data class LoadVelocityProfile(
    val id: String,
    @SerialName("user_id") val clientId: String,
    @SerialName("exercise_id") val exerciseId: String,
    @SerialName("exercise_name") val exerciseName: String? = null,
    @SerialName("mvt") val mvt: Double,                                // Minimum Velocity Threshold
    @SerialName("v1rm") val v1rm: Double,                              // Velocity at 1RM
    @SerialName("load_at_1ms") val loadAt1Ms: Double? = null,          // Load at 1.0 m/s
    @SerialName("slope") val slope: Double,                            // L-V slope (negative)
    @SerialName("intercept") val intercept: Double,                    // L-V y-intercept
    @SerialName("r_squared") val rSquared: Double,                     // Correlation coefficient
    @SerialName("data_points") val dataPoints: Int,                    // Number of data points
    @SerialName("calculated_at") val calculatedAt: String,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    /** Predict 1RM based on the L-V profile */
    fun predict1RM(): Double? {
        if (slope == 0.0) return null
        // 1RM is where velocity = v1rm (usually ~0.17-0.30 m/s depending on exercise)
        return (v1rm - intercept) / slope
    }

    /** Predict velocity at a given load */
    fun predictVelocity(loadKg: Double): Double {
        return intercept + (slope * loadKg)
    }

    /** Predict load at a given velocity */
    fun predictLoad(velocity: Double): Double? {
        if (slope == 0.0) return null
        return (velocity - intercept) / slope
    }

    /** R² quality description */
    val profileQuality: String
        get() = when {
            rSquared >= 0.95 -> "Excellent"
            rSquared >= 0.90 -> "Good"
            rSquared >= 0.80 -> "Acceptable"
            else -> "Poor"
        }
}

// ═══════════════════════════════════════════════════════════════════════════
// 1RM PREDICTION
// ═══════════════════════════════════════════════════════════════════════════

/**
 * 1RM prediction for an exercise.
 */
@Serializable
data class OneRMPrediction(
    val id: String,
    @SerialName("user_id") val clientId: String,
    @SerialName("exercise_id") val exerciseId: String,
    @SerialName("exercise_name") val exerciseName: String? = null,
    @SerialName("predicted_1rm_kg") val predicted1RmKg: Double,
    @SerialName("confidence_level") val confidenceLevel: Double? = null,  // 0-1
    @SerialName("calculation_method") val calculationMethod: String,      // lv_profile, brzycki, epley
    @SerialName("based_on_load_kg") val basedOnLoadKg: Double? = null,
    @SerialName("based_on_velocity") val basedOnVelocity: Double? = null,
    @SerialName("calculated_at") val calculatedAt: String,
    @SerialName("previous_1rm_kg") val previous1RmKg: Double? = null
) {
    /** Change from previous 1RM */
    val change: Double?
        get() = previous1RmKg?.let { predicted1RmKg - it }

    /** Confidence description */
    val confidenceDescription: String
        get() = when {
            confidenceLevel == null -> "Unknown"
            confidenceLevel >= 0.9 -> "High"
            confidenceLevel >= 0.7 -> "Medium"
            else -> "Low"
        }

    /** Formatted 1RM */
    val formattedValue: String
        get() = String.format("%.1f kg", predicted1RmKg)
}

// ═══════════════════════════════════════════════════════════════════════════
// FATIGUE INDEX
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Fatigue level based on velocity loss during a set.
 */
@Serializable
enum class FatigueLevel {
    @SerialName("fresh") FRESH,           // <10% velocity loss
    @SerialName("optimal") OPTIMAL,       // 10-20% - optimal training zone
    @SerialName("moderate") MODERATE,     // 20-30%
    @SerialName("high") HIGH,             // 30-40%
    @SerialName("excessive") EXCESSIVE    // >40% - stop set
}

/**
 * Current fatigue state based on velocity loss during a set or session.
 */
@Serializable
data class FatigueIndex(
    @SerialName("user_id") val clientId: String,
    @SerialName("exercise_id") val exerciseId: String? = null,
    @SerialName("session_id") val sessionId: String? = null,
    @SerialName("velocity_loss_percent") val velocityLossPercent: Double,
    @SerialName("fatigue_level") val fatigueLevel: FatigueLevel,
    @SerialName("first_rep_velocity") val firstRepVelocity: Double? = null,
    @SerialName("last_rep_velocity") val lastRepVelocity: Double? = null,
    @SerialName("calculated_at") val calculatedAt: String
) {
    /** Color for the fatigue indicator */
    val indicatorColor: String
        get() = when (fatigueLevel) {
            FatigueLevel.FRESH -> "#4CAF50"      // Green
            FatigueLevel.OPTIMAL -> "#8BC34A"   // Light Green
            FatigueLevel.MODERATE -> "#FFC107"  // Yellow/Amber
            FatigueLevel.HIGH -> "#FF9800"      // Orange
            FatigueLevel.EXCESSIVE -> "#F44336" // Red
        }

    /** Should the set be stopped? */
    val shouldStopSet: Boolean
        get() = fatigueLevel == FatigueLevel.EXCESSIVE

    /** Formatted velocity loss */
    val formattedLoss: String
        get() = String.format("%.1f%%", velocityLossPercent)

    companion object {
        /** Calculate fatigue level from velocity loss percentage */
        fun fromVelocityLoss(lossPercent: Double): FatigueLevel = when {
            lossPercent < 10 -> FatigueLevel.FRESH
            lossPercent < 20 -> FatigueLevel.OPTIMAL
            lossPercent < 30 -> FatigueLevel.MODERATE
            lossPercent < 40 -> FatigueLevel.HIGH
            else -> FatigueLevel.EXCESSIVE
        }

        /** Calculate velocity loss percentage from first and last rep velocities */
        fun calculateVelocityLoss(firstVelocity: Double, lastVelocity: Double): Double {
            if (firstVelocity == 0.0) return 0.0
            return ((firstVelocity - lastVelocity) / firstVelocity) * 100
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// READINESS ASSESSMENT
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Readiness level based on velocity deviation from baseline.
 */
@Serializable
enum class ReadinessLevel {
    @SerialName("peaked") PEAKED,         // >5% above baseline
    @SerialName("fresh") FRESH,           // 0-5% above baseline
    @SerialName("normal") NORMAL,         // ±2% of baseline
    @SerialName("fatigued") FATIGUED,     // 2-8% below baseline
    @SerialName("tired") TIRED,           // >8% below baseline
    @SerialName("unknown") UNKNOWN
}

/**
 * Client readiness assessment based on warmup velocity vs historical baseline.
 */
@Serializable
data class ClientReadiness(
    @SerialName("user_id") val clientId: String,
    @SerialName("readiness_level") val readinessLevel: ReadinessLevel,
    @SerialName("velocity_deviation_percent") val velocityDeviationPercent: Double,
    @SerialName("baseline_velocity") val baselineVelocity: Double? = null,
    @SerialName("current_velocity") val currentVelocity: Double? = null,
    @SerialName("calculated_at") val calculatedAt: String
) {
    /** Color for the readiness indicator */
    val indicatorColor: String
        get() = when (readinessLevel) {
            ReadinessLevel.PEAKED -> "#2196F3"   // Blue - peak performance
            ReadinessLevel.FRESH -> "#4CAF50"    // Green
            ReadinessLevel.NORMAL -> "#8BC34A"   // Light Green
            ReadinessLevel.FATIGUED -> "#FFC107" // Yellow/Amber
            ReadinessLevel.TIRED -> "#F44336"    // Red
            ReadinessLevel.UNKNOWN -> "#9E9E9E"  // Grey
        }

    /** Training recommendation based on readiness */
    val recommendation: String
        get() = when (readinessLevel) {
            ReadinessLevel.PEAKED -> "Peak day - consider max attempt"
            ReadinessLevel.FRESH -> "Good to train heavy"
            ReadinessLevel.NORMAL -> "Normal training"
            ReadinessLevel.FATIGUED -> "Consider lighter loads or deload"
            ReadinessLevel.TIRED -> "Recovery recommended"
            ReadinessLevel.UNKNOWN -> "Insufficient data"
        }

    /** Formatted deviation */
    val formattedDeviation: String
        get() {
            val sign = if (velocityDeviationPercent >= 0) "+" else ""
            return String.format("%s%.1f%%", sign, velocityDeviationPercent)
        }

    companion object {
        /** Calculate readiness level from velocity deviation percentage */
        fun fromDeviationPercent(deviationPercent: Double): ReadinessLevel = when {
            deviationPercent > 5 -> ReadinessLevel.PEAKED
            deviationPercent > 0 -> ReadinessLevel.FRESH
            deviationPercent >= -2 -> ReadinessLevel.NORMAL
            deviationPercent >= -8 -> ReadinessLevel.FATIGUED
            else -> ReadinessLevel.TIRED
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// LIVE VBT SESSION
// ═══════════════════════════════════════════════════════════════════════════

/**
 * State for a live VBT coaching session.
 * Used when coach is viewing client's workout in real-time.
 */
data class LiveVBTSession(
    val clientId: String,
    val clientName: String,
    val exerciseId: String? = null,
    val exerciseName: String? = null,
    val isActive: Boolean = false,
    val currentSetNumber: Int = 0,
    val currentRepNumber: Int = 0,
    val lastVelocity: Double? = null,
    val bestVelocity: Double? = null,
    val velocityLossPercent: Double? = null,
    val reps: List<VelocityEntry> = emptyList(),
    val startedAt: String? = null
) {
    /** Is this the best rep in the current set? */
    fun isCurrentRepBest(velocity: Double): Boolean {
        return velocity >= (bestVelocity ?: 0.0)
    }

    /** Current fatigue level for the set */
    val currentFatigueLevel: FatigueLevel?
        get() = velocityLossPercent?.let { FatigueIndex.fromVelocityLoss(it) }

    /** Number of reps in current set */
    val repsInSet: Int
        get() = reps.count { it.setNumber == currentSetNumber }
}

// ═══════════════════════════════════════════════════════════════════════════
// VBT DASHBOARD STATE
// ═══════════════════════════════════════════════════════════════════════════

/**
 * UI state for the VBT Dashboard screen.
 */
data class VBTDashboardState(
    val clientId: String = "",
    val clientName: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    // Mode
    val isLiveMode: Boolean = false,
    // Live session data
    val liveSession: LiveVBTSession? = null,
    val liveVelocityEntries: List<VelocityEntry> = emptyList(),
    // Historical data
    val velocityHistory: List<VelocityEntry> = emptyList(),
    val loadVelocityProfiles: List<LoadVelocityProfile> = emptyList(),
    val oneRmPredictions: List<OneRMPrediction> = emptyList(),
    // Current metrics
    val currentFatigue: FatigueIndex? = null,
    val readiness: ClientReadiness? = null,
    // Selection
    val selectedExerciseId: String? = null,
    val availableExercises: List<ExerciseVBTSummary> = emptyList()
)

/**
 * Summary of VBT data available for an exercise.
 */
data class ExerciseVBTSummary(
    val exerciseId: String,
    val exerciseName: String,
    val dataPointCount: Int,
    val hasProfile: Boolean,
    val last1RmPrediction: Double?,
    val lastRecordedAt: String?
)
