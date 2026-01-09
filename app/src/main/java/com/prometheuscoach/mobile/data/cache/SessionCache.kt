package com.prometheuscoach.mobile.data.cache

import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory session cache for faster data access.
 * Data persists during app session, cleared on app restart.
 *
 * Usage:
 * - cache.put("clients", clientsList)
 * - val clients = cache.get<List<Client>>("clients")
 * - cache.invalidate("clients") // force refresh
 */
@Singleton
class SessionCache @Inject constructor() {

    companion object {
        private const val TAG = "SessionCache"
        private const val DEFAULT_TTL_MS = 5 * 60 * 1000L // 5 minutes
    }

    private data class CacheEntry(
        val data: Any,
        val timestamp: Long,
        val ttlMs: Long
    ) {
        fun isExpired(): Boolean = System.currentTimeMillis() - timestamp > ttlMs
    }

    private val cache = ConcurrentHashMap<String, CacheEntry>()

    /**
     * Store data in cache with optional TTL.
     */
    fun <T : Any> put(key: String, data: T, ttlMs: Long = DEFAULT_TTL_MS) {
        cache[key] = CacheEntry(data, System.currentTimeMillis(), ttlMs)
        Log.d(TAG, "Cached: $key (TTL: ${ttlMs/1000}s)")
    }

    /**
     * Get data from cache. Returns null if not found or expired.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String): T? {
        val entry = cache[key] ?: return null

        if (entry.isExpired()) {
            cache.remove(key)
            Log.d(TAG, "Cache expired: $key")
            return null
        }

        Log.d(TAG, "Cache hit: $key")
        return entry.data as? T
    }

    /**
     * Get data from cache, or fetch from source if not cached/expired.
     */
    suspend fun <T : Any> getOrFetch(
        key: String,
        ttlMs: Long = DEFAULT_TTL_MS,
        fetch: suspend () -> T
    ): T {
        get<T>(key)?.let { return it }

        Log.d(TAG, "Cache miss: $key - fetching...")
        val data = fetch()
        put(key, data, ttlMs)
        return data
    }

    /**
     * Invalidate a specific cache entry.
     */
    fun invalidate(key: String) {
        cache.remove(key)
        Log.d(TAG, "Invalidated: $key")
    }

    /**
     * Invalidate all entries matching a prefix.
     */
    fun invalidatePrefix(prefix: String) {
        cache.keys.filter { it.startsWith(prefix) }.forEach {
            cache.remove(it)
            Log.d(TAG, "Invalidated: $it")
        }
    }

    /**
     * Clear entire cache.
     */
    fun clearAll() {
        cache.clear()
        Log.d(TAG, "Cache cleared")
    }

    /**
     * Get cache stats for debugging.
     */
    fun stats(): String {
        val total = cache.size
        val expired = cache.values.count { it.isExpired() }
        return "Cache: $total entries ($expired expired)"
    }
}

/**
 * Cache keys constants.
 */
object CacheKeys {
    const val CLIENTS = "clients"
    const val WORKOUTS = "workouts"
    const val PROGRAMS = "programs"
    const val CONVERSATIONS = "conversations"
    const val EXERCISES = "exercises"
    const val CURRENT_USER = "current_user"
    const val COACH_PROFILE = "coach_profile"

    fun messages(conversationId: String) = "messages_$conversationId"
    fun client(clientId: String) = "client_$clientId"
    fun workout(workoutId: String) = "workout_$workoutId"
    fun program(programId: String) = "program_$programId"
}
