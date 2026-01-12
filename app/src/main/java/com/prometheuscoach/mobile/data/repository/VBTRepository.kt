package com.prometheuscoach.mobile.data.repository

import android.util.Log
import com.prometheuscoach.mobile.data.model.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "VBTRepository"
private const val REALTIME_CHANNEL = "vbt-live-session"

/**
 * Repository for Velocity Based Training data.
 * Supports both historical data retrieval and live session streaming via Supabase Realtime.
 */
@Singleton
class VBTRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Live session state
    private val _liveVelocityEntries = MutableStateFlow<List<VelocityEntry>>(emptyList())
    val liveVelocityEntries: StateFlow<List<VelocityEntry>> = _liveVelocityEntries.asStateFlow()

    private val _liveSession = MutableStateFlow<LiveVBTSession?>(null)
    val liveSession: StateFlow<LiveVBTSession?> = _liveSession.asStateFlow()

    private var realtimeChannel: RealtimeChannel? = null
    private var isLiveSessionActive = false
    private var currentLiveClientId: String? = null

    // ═══════════════════════════════════════════════════════════════════════
    // LIVE COACHING SESSION
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Start listening for live velocity data from a client's workout session.
     * Uses Supabase Realtime to stream data as it's recorded.
     */
    fun startLiveSession(clientId: String, clientName: String) {
        if (isLiveSessionActive && currentLiveClientId == clientId) {
            Log.d(TAG, "Live session already active for client: $clientId")
            return
        }

        // Stop any existing session
        stopLiveSession()

        currentLiveClientId = clientId
        _liveSession.value = LiveVBTSession(
            clientId = clientId,
            clientName = clientName,
            isActive = true,
            startedAt = Instant.now().toString()
        )
        _liveVelocityEntries.value = emptyList()

        scope.launch {
            try {
                Log.d(TAG, "Starting live VBT session for client: $clientId")

                val channel = supabaseClient.realtime.channel(REALTIME_CHANNEL)
                realtimeChannel = channel

                // Listen for new velocity entries for this client
                val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "velocity_history"
                }

                changeFlow.onEach { action ->
                    when (action) {
                        is PostgresAction.Insert -> {
                            val recordClientId = action.record["user_id"]?.jsonPrimitive?.content
                            if (recordClientId == clientId) {
                                Log.d(TAG, "Received live velocity entry")
                                handleNewVelocityEntry(action.record)
                            }
                        }
                        else -> { /* Ignore updates and deletes for live view */ }
                    }
                }.launchIn(scope)

                channel.subscribe()
                isLiveSessionActive = true

                Log.d(TAG, "Live VBT session started successfully")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to start live VBT session", e)
                isLiveSessionActive = false
                _liveSession.value = _liveSession.value?.copy(isActive = false)
            }
        }
    }

    /**
     * Stop the live coaching session.
     */
    fun stopLiveSession() {
        scope.launch {
            try {
                realtimeChannel?.let { channel ->
                    supabaseClient.realtime.removeChannel(channel)
                    realtimeChannel = null
                }
                isLiveSessionActive = false
                currentLiveClientId = null
                _liveSession.value = null
                _liveVelocityEntries.value = emptyList()
                Log.d(TAG, "Live VBT session stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop live VBT session", e)
            }
        }
    }

    /**
     * Handle a new velocity entry from Realtime.
     */
    private fun handleNewVelocityEntry(record: kotlinx.serialization.json.JsonObject) {
        try {
            val entry = VelocityEntry(
                id = record["id"]?.jsonPrimitive?.content ?: return,
                clientId = record["user_id"]?.jsonPrimitive?.content ?: return,
                exerciseId = record["exercise_id"]?.jsonPrimitive?.content ?: "",
                exerciseName = record["exercise_name"]?.jsonPrimitive?.content,
                loadKg = record["load_kg"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                peakVelocity = record["peak_velocity"]?.jsonPrimitive?.content?.toDoubleOrNull(),
                meanVelocity = record["mean_velocity"]?.jsonPrimitive?.content?.toDoubleOrNull(),
                mpv = record["mpv"]?.jsonPrimitive?.content?.toDoubleOrNull(),
                powerWatts = record["power_watts"]?.jsonPrimitive?.content?.toDoubleOrNull(),
                romCm = record["rom_cm"]?.jsonPrimitive?.content?.toDoubleOrNull(),
                setNumber = record["set_number"]?.jsonPrimitive?.content?.toIntOrNull(),
                repNumber = record["rep_number"]?.jsonPrimitive?.content?.toIntOrNull(),
                setType = record["set_type"]?.jsonPrimitive?.content,
                workoutSessionId = record["workout_session_id"]?.jsonPrimitive?.content,
                recordedAt = record["recorded_at"]?.jsonPrimitive?.content ?: Instant.now().toString(),
                deviceId = record["device_id"]?.jsonPrimitive?.content
            )

            // Update live entries
            val currentEntries = _liveVelocityEntries.value.toMutableList()
            currentEntries.add(entry)
            _liveVelocityEntries.value = currentEntries

            // Update session state
            updateLiveSessionState(entry)

            Log.d(TAG, "Added live velocity entry: ${entry.velocityFormatted}")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse velocity entry from realtime", e)
        }
    }

    /**
     * Update the live session state with the new entry.
     */
    private fun updateLiveSessionState(entry: VelocityEntry) {
        val current = _liveSession.value ?: return
        val currentEntries = _liveVelocityEntries.value

        // Calculate best velocity in current set
        val setEntries = currentEntries.filter { it.setNumber == entry.setNumber }
        val bestVelocity = setEntries.mapNotNull { it.displayVelocity }.maxOrNull()

        // Calculate velocity loss
        val firstVelocity = setEntries.firstOrNull()?.displayVelocity
        val currentVelocity = entry.displayVelocity
        val velocityLoss = if (firstVelocity != null && currentVelocity != null) {
            FatigueIndex.calculateVelocityLoss(firstVelocity, currentVelocity)
        } else null

        _liveSession.value = current.copy(
            exerciseId = entry.exerciseId,
            exerciseName = entry.exerciseName,
            currentSetNumber = entry.setNumber ?: current.currentSetNumber,
            currentRepNumber = entry.repNumber ?: (current.currentRepNumber + 1),
            lastVelocity = entry.displayVelocity,
            bestVelocity = bestVelocity,
            velocityLossPercent = velocityLoss,
            reps = currentEntries
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HISTORICAL DATA
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Get velocity history for a client.
     */
    suspend fun getVelocityHistory(
        clientId: String,
        exerciseId: String? = null,
        dateFrom: String? = null,
        dateTo: String? = null,
        limit: Int = 100
    ): Result<List<VelocityEntry>> {
        return try {
            val entries = supabaseClient.postgrest
                .from("velocity_history")
                .select {
                    filter {
                        eq("user_id", clientId)
                        exerciseId?.let { eq("exercise_id", it) }
                        dateFrom?.let { gte("recorded_at", it) }
                        dateTo?.let { lte("recorded_at", it) }
                    }
                    order("recorded_at", Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<VelocityEntry>()

            Log.d(TAG, "Loaded ${entries.size} velocity entries for client $clientId")
            Result.success(entries)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get velocity history", e)
            Result.failure(e)
        }
    }

    /**
     * Get load-velocity profiles for a client.
     */
    suspend fun getLoadVelocityProfiles(clientId: String): Result<List<LoadVelocityProfile>> {
        return try {
            val profiles = supabaseClient.postgrest
                .from("load_velocity_profiles")
                .select {
                    filter { eq("user_id", clientId) }
                    order("calculated_at", Order.DESCENDING)
                }
                .decodeList<LoadVelocityProfile>()

            Log.d(TAG, "Loaded ${profiles.size} L-V profiles for client $clientId")
            Result.success(profiles)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get L-V profiles", e)
            Result.failure(e)
        }
    }

    /**
     * Get 1RM predictions for a client.
     */
    suspend fun get1RMPredictions(clientId: String): Result<List<OneRMPrediction>> {
        return try {
            val predictions = supabaseClient.postgrest
                .from("one_rm_predictions")
                .select {
                    filter { eq("user_id", clientId) }
                    order("calculated_at", Order.DESCENDING)
                }
                .decodeList<OneRMPrediction>()

            Log.d(TAG, "Loaded ${predictions.size} 1RM predictions for client $clientId")
            Result.success(predictions)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get 1RM predictions", e)
            Result.failure(e)
        }
    }

    /**
     * Get exercises that have VBT data for this client.
     */
    suspend fun getVBTExercises(clientId: String): Result<List<ExerciseVBTSummary>> {
        return try {
            // Get unique exercises from velocity history
            val entries = supabaseClient.postgrest
                .from("velocity_history")
                .select {
                    filter { eq("user_id", clientId) }
                }
                .decodeList<VelocityEntry>()

            // Get profiles
            val profiles = getLoadVelocityProfiles(clientId).getOrNull() ?: emptyList()
            val profileMap = profiles.associateBy { it.exerciseId }

            // Get predictions
            val predictions = get1RMPredictions(clientId).getOrNull() ?: emptyList()
            val predictionMap = predictions.groupBy { it.exerciseId }

            // Group and summarize
            val summaries = entries
                .groupBy { it.exerciseId }
                .map { (exerciseId, exerciseEntries) ->
                    val latestEntry = exerciseEntries.maxByOrNull { it.recordedAt }
                    val profile = profileMap[exerciseId]
                    val latestPrediction = predictionMap[exerciseId]?.maxByOrNull { it.calculatedAt }

                    ExerciseVBTSummary(
                        exerciseId = exerciseId,
                        exerciseName = latestEntry?.exerciseName ?: exerciseId,
                        dataPointCount = exerciseEntries.size,
                        hasProfile = profile != null,
                        last1RmPrediction = latestPrediction?.predicted1RmKg,
                        lastRecordedAt = latestEntry?.recordedAt
                    )
                }
                .sortedByDescending { it.lastRecordedAt }

            Log.d(TAG, "Found ${summaries.size} exercises with VBT data for client $clientId")
            Result.success(summaries)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get VBT exercises", e)
            Result.failure(e)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CALCULATIONS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Calculate current fatigue index from a set of velocity entries.
     */
    fun calculateFatigueIndex(
        entries: List<VelocityEntry>,
        clientId: String,
        exerciseId: String? = null
    ): FatigueIndex? {
        if (entries.isEmpty()) return null

        val velocities = entries.mapNotNull { it.displayVelocity }
        if (velocities.isEmpty()) return null

        val firstVelocity = velocities.first()
        val lastVelocity = velocities.last()
        val velocityLoss = FatigueIndex.calculateVelocityLoss(firstVelocity, lastVelocity)
        val fatigueLevel = FatigueIndex.fromVelocityLoss(velocityLoss)

        return FatigueIndex(
            clientId = clientId,
            exerciseId = exerciseId,
            sessionId = entries.firstOrNull()?.workoutSessionId,
            velocityLossPercent = velocityLoss,
            fatigueLevel = fatigueLevel,
            firstRepVelocity = firstVelocity,
            lastRepVelocity = lastVelocity,
            calculatedAt = Instant.now().toString()
        )
    }

    /**
     * Calculate readiness based on warmup velocity vs historical baseline.
     */
    suspend fun calculateReadiness(
        clientId: String,
        exerciseId: String,
        warmupVelocity: Double
    ): ClientReadiness {
        // Get baseline from load-velocity profile
        val profiles = getLoadVelocityProfiles(clientId).getOrNull() ?: emptyList()
        val profile = profiles.find { it.exerciseId == exerciseId }

        // If no profile, try to calculate baseline from recent history
        val baselineVelocity = if (profile != null) {
            // Use velocity at a moderate load (e.g., 60% of 1RM)
            val estimated1RM = profile.predict1RM() ?: return unknownReadiness(clientId, warmupVelocity)
            val moderateLoad = estimated1RM * 0.6
            profile.predictVelocity(moderateLoad)
        } else {
            // Calculate from recent history for same exercise
            val recentHistory = getVelocityHistory(clientId, exerciseId, limit = 20).getOrNull()
            recentHistory?.mapNotNull { it.displayVelocity }?.average() ?: return unknownReadiness(clientId, warmupVelocity)
        }

        // Calculate deviation percentage
        val deviation = ((warmupVelocity - baselineVelocity) / baselineVelocity) * 100
        val readinessLevel = ClientReadiness.fromDeviationPercent(deviation)

        return ClientReadiness(
            clientId = clientId,
            readinessLevel = readinessLevel,
            velocityDeviationPercent = deviation,
            baselineVelocity = baselineVelocity,
            currentVelocity = warmupVelocity,
            calculatedAt = Instant.now().toString()
        )
    }

    private fun unknownReadiness(clientId: String, currentVelocity: Double): ClientReadiness {
        return ClientReadiness(
            clientId = clientId,
            readinessLevel = ReadinessLevel.UNKNOWN,
            velocityDeviationPercent = 0.0,
            baselineVelocity = null,
            currentVelocity = currentVelocity,
            calculatedAt = Instant.now().toString()
        )
    }

    /**
     * Predict 1RM from current velocity data using L-V profile.
     */
    fun predict1RM(
        loadKg: Double,
        velocity: Double,
        profile: LoadVelocityProfile?
    ): Double? {
        if (profile == null || profile.slope == 0.0) return null

        // Using the L-V relationship: 1RM = Load / (1 - (V1RM / MV))
        // Or from profile: predict load where velocity = MVT
        return profile.predictLoad(profile.mvt)
    }

    /**
     * Quick 1RM estimate using Brzycki formula (for reps-based estimation).
     */
    fun estimateBrzycki1RM(loadKg: Double, reps: Int): Double {
        if (reps <= 0) return loadKg
        if (reps == 1) return loadKg
        return loadKg * (36.0 / (37.0 - reps))
    }
}
