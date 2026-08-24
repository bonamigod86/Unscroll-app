package com.unscroll.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject

class BlockedAppsStore private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private val _blockedFlow = MutableStateFlow(readBlocked())
    val blockedFlow: StateFlow<Set<String>> = _blockedFlow

    fun blocked(): Set<String> = readBlocked()

    fun isBlocked(packageName: String): Boolean = readBlocked().contains(packageName)

    fun setBlocked(packageName: String, blocked: Boolean) {
        val current = readBlocked().toMutableSet()
        if (blocked) current.add(packageName) else current.remove(packageName)
        prefs.edit().putStringSet(KEY_BLOCKED, current).apply()
        _blockedFlow.value = current
    }

    fun isTemporarilyUnlocked(packageName: String, nowMs: Long = System.currentTimeMillis()): Boolean =
        readUnlocks().optLong(packageName, 0L) > nowMs

    fun grantTemporaryUnlock(packageName: String, minutes: Int, nowMs: Long = System.currentTimeMillis()) {
        val unlocks = readUnlocks()
        unlocks.put(packageName, nowMs + minutes * 60_000L)
        prefs.edit().putString(KEY_UNLOCKS, unlocks.toString()).apply()
    }

    private fun readBlocked(): Set<String> =
        prefs.getStringSet(KEY_BLOCKED, emptySet()) ?: emptySet()

    private fun readUnlocks(): JSONObject {
        val raw = prefs.getString(KEY_UNLOCKS, null) ?: return JSONObject()
        return try {
            JSONObject(raw)
        } catch (_: Exception) {
            JSONObject()
        }
    }

    companion object {
        private const val FILE = "unscroll_blocklist"
        private const val KEY_BLOCKED = "blocked_packages"
        private const val KEY_UNLOCKS = "temp_unlocks"

        @Volatile
        private var instance: BlockedAppsStore? = null

        fun get(context: Context): BlockedAppsStore =
            instance ?: synchronized(this) {
                instance ?: BlockedAppsStore(context).also { instance = it }
            }
    }
}
