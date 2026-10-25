package com.example.calculaparajugar.util

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Sistema de progresión tipo Duolingo.
 *
 * - XP: experiencia acumulativa (nunca se pierde)
 * - Nivel: determinado por XP total. Sube la dificultad gradualmente.
 * - Racha: días consecutivos respondiendo al menos 1 pregunta
 * - Hoy: estadísticas del día (correctas, errores)
 *
 * Niveles:
 *   1-5   → Fácil (preguntas básicas)
 *   6-12  → Medio (mezcla gradual)
 *   13-20 → Difícil (preguntas avanzadas)
 *   20+   → Experto (todo difícil + speed bonus)
 *
 * XP por nivel: nivel * 50 XP para subir
 *   Nivel 1→2: 50 XP
 *   Nivel 2→3: 100 XP
 *   etc.
 */
object ProgressManager {

    private const val PREFS = "brainwall_progress"
    private const val KEY_XP_TOTAL = "xp_total"
    private const val KEY_LEVEL = "level"
    private const val KEY_XP_IN_LEVEL = "xp_in_level"
    private const val KEY_STREAK_DAYS = "streak_days"
    private const val KEY_LAST_ACTIVE_DATE = "last_active_date"
    private const val KEY_TODAY_CORRECT = "today_correct"
    private const val KEY_TODAY_INCORRECT = "today_incorrect"
    private const val KEY_TODAY_DATE = "today_date"
    private const val KEY_TOTAL_CORRECT = "all_time_correct"
    private const val KEY_TOTAL_INCORRECT = "all_time_incorrect"
    private const val KEY_CONSECUTIVE_CORRECT = "consecutive_correct"

    // XP rewards
    private const val XP_CORRECT = 10
    private const val XP_CORRECT_STREAK_BONUS = 2 // +2 XP por cada respuesta correcta consecutiva
    private const val XP_PENALTY = 0 // No se pierde XP, solo se rompe la racha de consecutivas

    data class Progress(
        val level: Int,
        val xpInLevel: Int,
        val xpToNextLevel: Int,
        val xpTotal: Int,
        val streakDays: Int,
        val todayCorrect: Int,
        val todayIncorrect: Int,
        val consecutiveCorrect: Int,
        val levelProgress: Float // 0.0 - 1.0
    )

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    private fun todayStr(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun xpNeededForLevel(level: Int): Int {
        // Cada nivel necesita más XP: nivel * 50
        // Nivel 1→2: 50, Nivel 5→6: 300, Nivel 10→11: 550
        return level * 50
    }

    /**
     * Chequea si es un nuevo día y actualiza la racha.
     */
    private fun checkDay(context: Context) {
        val p = prefs(context)
        val today = todayStr()
        val savedDay = p.getString(KEY_TODAY_DATE, "")

        if (savedDay != today) {
            // Nuevo día
            val lastActive = p.getString(KEY_LAST_ACTIVE_DATE, "") ?: ""

            // Calcular si es día consecutivo
            val streak = if (isConsecutiveDay(lastActive, today)) {
                p.getInt(KEY_STREAK_DAYS, 0) // mantener racha
            } else if (lastActive.isEmpty()) {
                0 // primera vez
            } else {
                0 // se rompió la racha
            }

            p.edit()
                .putString(KEY_TODAY_DATE, today)
                .putInt(KEY_TODAY_CORRECT, 0)
                .putInt(KEY_TODAY_INCORRECT, 0)
                .putInt(KEY_STREAK_DAYS, streak)
                .apply()
        }
    }

    private fun isConsecutiveDay(last: String, today: String): Boolean {
        if (last.isEmpty()) return false
        return try {
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val lastDate = fmt.parse(last) ?: return false
            val todayDate = fmt.parse(today) ?: return false
            val diff = (todayDate.time - lastDate.time) / (24 * 60 * 60 * 1000)
            diff == 1L
        } catch (e: Exception) { false }
    }

    data class AnswerResult(
        val xpGained: Int,
        val leveledUp: Boolean,
        val newLevel: Int,
        val mascotEvolved: Boolean
    )

    /**
     * Registra una respuesta correcta. Devuelve resultado con info de level up.
     */
    fun onCorrect(context: Context): AnswerResult {
        checkDay(context)
        val p = prefs(context)
        val today = todayStr()

        val consecutive = p.getInt(KEY_CONSECUTIVE_CORRECT, 0) + 1
        val todayCorrect = p.getInt(KEY_TODAY_CORRECT, 0) + 1
        val totalCorrect = p.getInt(KEY_TOTAL_CORRECT, 0) + 1

        // XP: base + bonus por consecutivas
        val xpGained = XP_CORRECT + (consecutive * XP_CORRECT_STREAK_BONUS)

        var xpTotal = p.getInt(KEY_XP_TOTAL, 0) + xpGained
        val oldLevel = p.getInt(KEY_LEVEL, 1)
        var level = oldLevel
        var xpInLevel = p.getInt(KEY_XP_IN_LEVEL, 0) + xpGained

        // Check level up
        while (xpInLevel >= xpNeededForLevel(level)) {
            xpInLevel -= xpNeededForLevel(level)
            level++
        }

        val leveledUp = level > oldLevel

        // Check if mascot evolved
        val oldMascot = MascotManager.getStageForLevel(oldLevel)
        val newMascot = MascotManager.getStageForLevel(level)
        val mascotEvolved = newMascot.drawableRes != oldMascot.drawableRes

        // Actualizar racha de días
        val streak = p.getInt(KEY_STREAK_DAYS, 0) + if (todayCorrect == 1) 1 else 0

        p.edit()
            .putInt(KEY_CONSECUTIVE_CORRECT, consecutive)
            .putInt(KEY_TODAY_CORRECT, todayCorrect)
            .putInt(KEY_TOTAL_CORRECT, totalCorrect)
            .putInt(KEY_XP_TOTAL, xpTotal)
            .putInt(KEY_LEVEL, level)
            .putInt(KEY_XP_IN_LEVEL, xpInLevel)
            .putInt(KEY_STREAK_DAYS, streak)
            .putString(KEY_LAST_ACTIVE_DATE, today)
            .apply()

        return AnswerResult(xpGained, leveledUp, level, mascotEvolved)
    }

    /**
     * Registra una respuesta incorrecta.
     */
    fun onIncorrect(context: Context) {
        checkDay(context)
        val p = prefs(context)
        val today = todayStr()

        val todayIncorrect = p.getInt(KEY_TODAY_INCORRECT, 0) + 1
        val totalIncorrect = p.getInt(KEY_TOTAL_INCORRECT, 0) + 1

        p.edit()
            .putInt(KEY_CONSECUTIVE_CORRECT, 0) // rompe racha de consecutivas
            .putInt(KEY_TODAY_INCORRECT, todayIncorrect)
            .putInt(KEY_TOTAL_INCORRECT, totalIncorrect)
            .putString(KEY_LAST_ACTIVE_DATE, today)
            .apply()
    }

    /**
     * Obtiene el progreso completo.
     */
    fun getProgress(context: Context): Progress {
        checkDay(context)
        val p = prefs(context)
        val level = p.getInt(KEY_LEVEL, 1)
        val xpInLevel = p.getInt(KEY_XP_IN_LEVEL, 0)
        val xpNeeded = xpNeededForLevel(level)

        return Progress(
            level = level,
            xpInLevel = xpInLevel,
            xpToNextLevel = xpNeeded,
            xpTotal = p.getInt(KEY_XP_TOTAL, 0),
            streakDays = p.getInt(KEY_STREAK_DAYS, 0),
            todayCorrect = p.getInt(KEY_TODAY_CORRECT, 0),
            todayIncorrect = p.getInt(KEY_TODAY_INCORRECT, 0),
            consecutiveCorrect = p.getInt(KEY_CONSECUTIVE_CORRECT, 0),
            levelProgress = xpInLevel.toFloat() / xpNeeded.toFloat()
        )
    }

    /**
     * Establece manualmente el nivel (usado por la evaluación).
     * Resetea XP del nivel actual a 0.
     */
    fun setLevel(context: Context, newLevel: Int) {
        val p = prefs(context)
        val xpForLevel = (1 until newLevel).sumOf { xpNeededForLevel(it) }
        p.edit()
            .putInt(KEY_LEVEL, newLevel)
            .putInt(KEY_XP_IN_LEVEL, 0)
            .putInt(KEY_XP_TOTAL, xpForLevel)
            .apply()
    }

    /**
     * Determina la dificultad de preguntas basándose en el nivel.
     */
    fun getDifficultyForLevel(context: Context): QuestionGenerator.Difficulty {
        val level = prefs(context).getInt(KEY_LEVEL, 1)
        val rand = kotlin.random.Random.nextFloat()

        return when {
            level <= 3 -> QuestionGenerator.Difficulty.EASY

            level <= 5 -> {
                // 80% fácil, 20% medio
                if (rand < 0.80f) QuestionGenerator.Difficulty.EASY
                else QuestionGenerator.Difficulty.MEDIUM
            }

            level <= 8 -> {
                // 50% fácil, 50% medio
                if (rand < 0.50f) QuestionGenerator.Difficulty.EASY
                else QuestionGenerator.Difficulty.MEDIUM
            }

            level <= 12 -> {
                // 20% fácil, 60% medio, 20% difícil
                when {
                    rand < 0.20f -> QuestionGenerator.Difficulty.EASY
                    rand < 0.80f -> QuestionGenerator.Difficulty.MEDIUM
                    else -> QuestionGenerator.Difficulty.HARD
                }
            }

            level <= 16 -> {
                // 40% medio, 60% difícil
                if (rand < 0.40f) QuestionGenerator.Difficulty.MEDIUM
                else QuestionGenerator.Difficulty.HARD
            }

            else -> {
                // 10% medio, 90% difícil
                if (rand < 0.10f) QuestionGenerator.Difficulty.MEDIUM
                else QuestionGenerator.Difficulty.HARD
            }
        }
    }
}
