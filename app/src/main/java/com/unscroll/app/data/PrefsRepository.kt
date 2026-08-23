package com.unscroll.app.data

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.unscroll.app.widget.ReclaimedWidgetReceiver

class PrefsRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private val _onboardingDone = MutableStateFlow(prefs.getBoolean(KEY_ONBOARDING_DONE, false))
    val onboardingDone: StateFlow<Boolean> = _onboardingDone

    private val _subscribed = MutableStateFlow(prefs.getBoolean(KEY_SUBSCRIBED, false))
    val subscribed: StateFlow<Boolean> = _subscribed

    private val _streak = MutableStateFlow(prefs.getInt(KEY_STREAK, 0))
    val streak: StateFlow<Int> = _streak

    private val _interventions = MutableStateFlow(prefs.getInt(KEY_INTERVENTIONS, 0))
    val interventions: StateFlow<Int> = _interventions

    private val _goalConfigured = MutableStateFlow(prefs.getBoolean(KEY_GOAL_CONFIGURED, false))
    val goalConfigured: StateFlow<Boolean> = _goalConfigured

    private val _dailyGoalMinutes = MutableStateFlow(prefs.getInt(KEY_DAILY_GOAL_MINUTES, DEFAULT_GOAL_MINUTES))
    val dailyGoalMinutes: StateFlow<Int> = _dailyGoalMinutes

    private val _frictionChallenge =
        MutableStateFlow(FrictionChallenge.fromId(prefs.getInt(KEY_FRICTION_CHALLENGE, 0)))
    val frictionChallenge: StateFlow<FrictionChallenge> = _frictionChallenge

    private var statDay: Long = prefs.getLong(KEY_STAT_DAY, -1L)
    private val _todayInterventions = MutableStateFlow(todayValue(prefs.getInt(KEY_TODAY_INTERVENTIONS, 0)))
    val todayInterventions: StateFlow<Int> = _todayInterventions

    private val _todayUnlocks = MutableStateFlow(todayValue(prefs.getInt(KEY_TODAY_UNLOCKS, 0)))
    val todayUnlocks: StateFlow<Int> = _todayUnlocks

    fun setOnboardingDone(done: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, done).apply()
        _onboardingDone.value = done
    }

    fun setSubscribed(value: Boolean) {
        prefs.edit().putBoolean(KEY_SUBSCRIBED, value).apply()
        _subscribed.value = value
    }

    fun setGoalConfigured(value: Boolean) {
        prefs.edit().putBoolean(KEY_GOAL_CONFIGURED, value).apply()
        _goalConfigured.value = value
    }

    fun setDailyGoalMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_DAILY_GOAL_MINUTES, minutes).apply()
        _dailyGoalMinutes.value = minutes
    }

    fun setFrictionChallenge(challenge: FrictionChallenge) {
        prefs.edit().putInt(KEY_FRICTION_CHALLENGE, challenge.id).apply()
        _frictionChallenge.value = challenge
    }

    fun touchStreak() {
        rollDayIfNeeded()
        val today = currentDay()
        val last = prefs.getLong(KEY_LAST_ACTIVE_DAY, -1L)
        if (last == today) return
        val next = when (last) {
            today - 1 -> _streak.value + 1
            else -> 1
        }
        prefs.edit()
            .putLong(KEY_LAST_ACTIVE_DAY, today)
            .putInt(KEY_STREAK, next)
            .apply()
        _streak.value = next
    }

    fun incrementInterventions() {
        rollDayIfNeeded()
        val totalNext = _interventions.value + 1
        val todayNext = _todayInterventions.value + 1
        prefs.edit()
            .putInt(KEY_INTERVENTIONS, totalNext)
            .putInt(KEY_TODAY_INTERVENTIONS, todayNext)
            .apply()
        _interventions.value = totalNext
        _todayInterventions.value = todayNext
        pushWidgetUpdate()
    }

    fun incrementUnlocks() {
        rollDayIfNeeded()
        val todayNext = _todayUnlocks.value + 1
        prefs.edit().putInt(KEY_TODAY_UNLOCKS, todayNext).apply()
        _todayUnlocks.value = todayNext
        pushWidgetUpdate()
    }

    private fun rollDayIfNeeded() {
        val today = currentDay()
        if (statDay == today) return
        statDay = today
        prefs.edit()
            .putLong(KEY_STAT_DAY, today)
            .putInt(KEY_TODAY_INTERVENTIONS, 0)
            .putInt(KEY_TODAY_UNLOCKS, 0)
            .apply()
        _todayInterventions.value = 0
        _todayUnlocks.value = 0
        pushWidgetUpdate()
    }

    private fun pushWidgetUpdate() {
        runCatching {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).setComponent(
                ComponentName(appContext, ReclaimedWidgetReceiver::class.java)
            )
            appContext.sendBroadcast(intent)
        }
    }

    private fun todayValue(stored: Int): Int {
        if (statDay != currentDay()) return 0
        return stored
    }

    val estimatedHoursReclaimed: Float
        get() = _interventions.value * ESTIMATED_MINUTES_SAVED_PER_INTERVENTION / 60f

    val todayReclaimedMinutes: Float
        get() = _todayInterventions.value * ESTIMATED_MINUTES_SAVED_PER_INTERVENTION

    companion object {
        private const val FILE = "unscroll_prefs"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_SUBSCRIBED = "subscribed"
        private const val KEY_STREAK = "streak"
        private const val KEY_INTERVENTIONS = "interventions"
        private const val KEY_LAST_ACTIVE_DAY = "last_active_day"
        private const val KEY_GOAL_CONFIGURED = "goal_configured"
        private const val KEY_DAILY_GOAL_MINUTES = "daily_goal_minutes"
        private const val KEY_FRICTION_CHALLENGE = "friction_challenge"
        private const val KEY_STAT_DAY = "stat_day"
        private const val KEY_TODAY_INTERVENTIONS = "today_interventions"
        private const val KEY_TODAY_UNLOCKS = "today_unlocks"

        const val MILLIS_PER_DAY = 86_400_000L
        const val ESTIMATED_MINUTES_SAVED_PER_INTERVENTION = 17f
        const val DEFAULT_GOAL_MINUTES = 60

        private fun currentDay(): Long = System.currentTimeMillis() / MILLIS_PER_DAY

        @Volatile
        private var instance: PrefsRepository? = null

        fun get(context: Context): PrefsRepository =
            instance ?: synchronized(this) {
                instance ?: PrefsRepository(context).also { instance = it }
            }
    }
}
