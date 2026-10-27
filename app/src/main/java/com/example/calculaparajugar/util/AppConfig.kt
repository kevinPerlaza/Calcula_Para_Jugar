package com.example.calculaparajugar.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Configuración de BrainWall:
 * - Apps bloqueadas
 * - Intervalo de preguntas
 * - Categorías habilitadas
 */
object AppConfig {

    private const val PREFS_NAME = "brainwall_prefs"
    private const val KEY_BLOCKED_APPS = "blocked_apps"
    private const val KEY_MONITOR_ALL_APPS = "monitor_all_apps"
    private const val KEY_INTERVAL_MINUTES = "interval_minutes"
    private const val KEY_CATEGORIES = "enabled_categories"
    private const val KEY_GUIDE_SEEN = "guide_seen"
    private const val KEY_PAUSED_UNTIL = "paused_until"
    private const val KEY_DAILY_GOAL = "daily_goal"
    private const val KEY_PROTECTION_ENABLED = "protection_enabled"

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // ═══ Apps Bloqueadas ═══

    fun getBlockedApps(context: Context): Set<String> {
        return prefs(context).getStringSet(KEY_BLOCKED_APPS, emptySet()) ?: emptySet()
    }

    fun setBlockedApps(context: Context, apps: Set<String>) {
        prefs(context).edit().putStringSet(KEY_BLOCKED_APPS, apps).apply()
    }

    fun isMonitorAllAppsEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_MONITOR_ALL_APPS, false)
    }

    fun setMonitorAllAppsEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_MONITOR_ALL_APPS, enabled).apply()
    }

    // ═══ Intervalo ═══

    fun getIntervalMinutes(context: Context): Int {
        return prefs(context).getInt(KEY_INTERVAL_MINUTES, 5)
    }

    fun setIntervalMinutes(context: Context, minutes: Int) {
        prefs(context).edit().putInt(KEY_INTERVAL_MINUTES, minutes).apply()
    }

    // ═══ Categorías ═══

    fun getEnabledCategories(context: Context): List<QuestionGenerator.Category> {
        val saved = prefs(context).getStringSet(KEY_CATEGORIES, null)
        if (saved.isNullOrEmpty()) return listOf(QuestionGenerator.Category.MATH)

        return saved.mapNotNull { name ->
            try { QuestionGenerator.Category.valueOf(name) }
            catch (e: Exception) { null }
        }.ifEmpty { listOf(QuestionGenerator.Category.MATH) }
    }

    fun setEnabledCategories(context: Context, categories: Set<QuestionGenerator.Category>) {
        val names = categories.map { it.name }.toSet()
        prefs(context).edit().putStringSet(KEY_CATEGORIES, names).apply()
    }

    fun hasSeenGuide(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_GUIDE_SEEN, false)
    }

    fun setGuideSeen(context: Context, seen: Boolean) {
        prefs(context).edit().putBoolean(KEY_GUIDE_SEEN, seen).apply()
    }

    fun pauseFor(context: Context, durationMillis: Long) {
        prefs(context).edit()
            .putLong(KEY_PAUSED_UNTIL, System.currentTimeMillis() + durationMillis)
            .apply()
    }

    fun isPaused(context: Context): Boolean {
        return prefs(context).getLong(KEY_PAUSED_UNTIL, 0L) > System.currentTimeMillis()
    }

    fun getDailyGoal(context: Context): Int = prefs(context).getInt(KEY_DAILY_GOAL, 10)

    fun cycleDailyGoal(context: Context): Int {
        val next = when (getDailyGoal(context)) {
            5 -> 10
            10 -> 20
            else -> 5
        }
        prefs(context).edit().putInt(KEY_DAILY_GOAL, next).apply()
        return next
    }

    fun setProtectionEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_PROTECTION_ENABLED, enabled).apply()
    }

    fun isProtectionEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PROTECTION_ENABLED, false)
}
