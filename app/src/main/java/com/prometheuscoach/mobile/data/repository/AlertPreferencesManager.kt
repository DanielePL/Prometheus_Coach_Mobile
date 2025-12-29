package com.prometheuscoach.mobile.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

private val Context.alertDataStore: DataStore<Preferences> by preferencesDataStore(name = "alert_preferences")

/**
 * Manages alert preferences using DataStore.
 * Handles dismissed alerts and celebrated wins persistence.
 */
@Singleton
class AlertPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val DISMISSED_ALERTS_KEY = stringSetPreferencesKey("dismissed_alerts")
        private val CELEBRATED_WINS_KEY = stringSetPreferencesKey("celebrated_wins")

        // Alerts older than this will be auto-cleaned from storage
        private const val ALERT_EXPIRY_DAYS = 14L
    }

    /**
     * Get all dismissed alert IDs.
     */
    suspend fun getDismissedAlertIds(): Set<String> {
        return context.alertDataStore.data
            .map { preferences ->
                val stored = preferences[DISMISSED_ALERTS_KEY] ?: emptySet()
                // Parse and filter expired entries
                stored.mapNotNull { entry ->
                    parseEntry(entry)?.takeIf { !isExpired(it.second) }?.first
                }.toSet()
            }
            .first()
    }

    /**
     * Mark an alert as dismissed.
     */
    suspend fun dismissAlert(alertId: String) {
        context.alertDataStore.edit { preferences ->
            val current = preferences[DISMISSED_ALERTS_KEY] ?: emptySet()
            // Store as "alertId|timestamp" for expiry tracking
            val entry = "$alertId|${Instant.now()}"
            preferences[DISMISSED_ALERTS_KEY] = current + entry
        }
    }

    /**
     * Restore a dismissed alert (undo dismiss).
     */
    suspend fun restoreAlert(alertId: String) {
        context.alertDataStore.edit { preferences ->
            val current = preferences[DISMISSED_ALERTS_KEY] ?: emptySet()
            // Remove any entry matching this alertId
            preferences[DISMISSED_ALERTS_KEY] = current.filter { entry ->
                parseEntry(entry)?.first != alertId
            }.toSet()
        }
    }

    /**
     * Get all celebrated win IDs.
     */
    suspend fun getCelebratedWinIds(): Set<String> {
        return context.alertDataStore.data
            .map { preferences ->
                val stored = preferences[CELEBRATED_WINS_KEY] ?: emptySet()
                stored.mapNotNull { entry ->
                    parseEntry(entry)?.takeIf { !isExpired(it.second) }?.first
                }.toSet()
            }
            .first()
    }

    /**
     * Mark a win as celebrated.
     */
    suspend fun celebrateWin(winId: String) {
        context.alertDataStore.edit { preferences ->
            val current = preferences[CELEBRATED_WINS_KEY] ?: emptySet()
            val entry = "$winId|${Instant.now()}"
            preferences[CELEBRATED_WINS_KEY] = current + entry
        }
    }

    /**
     * Clean up expired entries from storage.
     */
    suspend fun cleanupExpired() {
        context.alertDataStore.edit { preferences ->
            // Clean dismissed alerts
            val dismissedAlerts = preferences[DISMISSED_ALERTS_KEY] ?: emptySet()
            preferences[DISMISSED_ALERTS_KEY] = dismissedAlerts.filter { entry ->
                parseEntry(entry)?.let { !isExpired(it.second) } ?: false
            }.toSet()

            // Clean celebrated wins
            val celebratedWins = preferences[CELEBRATED_WINS_KEY] ?: emptySet()
            preferences[CELEBRATED_WINS_KEY] = celebratedWins.filter { entry ->
                parseEntry(entry)?.let { !isExpired(it.second) } ?: false
            }.toSet()
        }
    }

    /**
     * Clear all preferences (for testing or reset).
     */
    suspend fun clearAll() {
        context.alertDataStore.edit { preferences ->
            preferences.clear()
        }
    }

    /**
     * Parse an entry in format "id|timestamp".
     */
    private fun parseEntry(entry: String): Pair<String, Instant>? {
        return try {
            val parts = entry.split("|")
            if (parts.size == 2) {
                Pair(parts[0], Instant.parse(parts[1]))
            } else {
                // Legacy format without timestamp - treat as still valid
                Pair(entry, Instant.now())
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if a timestamp is expired.
     */
    private fun isExpired(timestamp: Instant): Boolean {
        val daysSince = ChronoUnit.DAYS.between(timestamp, Instant.now())
        return daysSince > ALERT_EXPIRY_DAYS
    }
}
