package com.example.calculaparajugar.util

import android.content.Context
import com.example.calculaparajugar.data.QuestionBank
import kotlin.math.max
import kotlin.random.Random

/** Prioriza preguntas falladas sin dejar de introducir contenido nuevo. */
object AdaptiveQuestionManager {
    private const val PREFS = "brainwall_adaptive"
    private const val RECENT_WINDOW_MS = 24 * 60 * 60_000L
    private const val NEW_QUESTION_WEIGHT = 9

    fun recordAnswer(
        context: Context,
        questionId: String,
        correct: Boolean,
        category: QuestionGenerator.Category
    ) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val categoryKey = category.name
        editor.putInt("category_total_$categoryKey", prefs.getInt("category_total_$categoryKey", 0) + 1)
        if (!correct) {
            editor.putInt("category_wrong_$categoryKey", prefs.getInt("category_wrong_$categoryKey", 0) + 1)
        }
        if (questionId.isBlank()) {
            editor.apply()
            return
        }
        val prefix = if (correct) "correct_" else "wrong_"
        val key = prefix + questionId
        val correctCount = prefs.getInt("correct_$questionId", 0) + if (correct) 1 else 0
        val wrongCount = prefs.getInt("wrong_$questionId", 0) + if (correct) 0 else 1
        val delay = reviewDelayMillis(correctCount, wrongCount, correct)
        editor
            .putInt(key, prefs.getInt(key, 0) + 1)
            .putLong("seen_$questionId", System.currentTimeMillis())
            .putLong("due_$questionId", System.currentTimeMillis() + delay)
            .apply()
    }

    fun select(context: Context, questions: List<QuestionBank.Question>): QuestionBank.Question? {
        if (questions.isEmpty()) return null
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val unseen = questions.filter { prefs.getLong("seen_${it.id}", 0L) == 0L }
        val due = questions.filter {
            val dueAt = prefs.getLong("due_${it.id}", 0L)
            dueAt in 1..now
        }
        val notRecent = questions.filter {
            now - prefs.getLong("seen_${it.id}", 0L) >= RECENT_WINDOW_MS
        }
        val pool = when {
            due.isNotEmpty() -> due
            unseen.isNotEmpty() -> unseen
            notRecent.isNotEmpty() -> notRecent
            else -> questions
        }
        val weighted = pool.map { question ->
            val correct = prefs.getInt("correct_${question.id}", 0)
            val wrong = prefs.getInt("wrong_${question.id}", 0)
            val attempts = correct + wrong
            val weight = if (attempts == 0) NEW_QUESTION_WEIGHT else 3 + wrong * 5 - correct * 2
            question to max(1, weight)
        }
        var pick = Random.nextInt(weighted.sumOf { it.second })
        for ((question, weight) in weighted) {
            pick -= weight
            if (pick < 0) return question
        }
        return weighted.last().first
    }

    /** Mezcla nivel y rendimiento real para evitar saltos bruscos de dificultad. */
    fun difficultyFor(
        context: Context,
        category: QuestionGenerator.Category,
        userLevel: Int
    ): Int {
        val base = QuestionBank.difficultyForUserLevel(userLevel)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val total = prefs.getInt("category_total_${category.name}", 0)
        if (total < 5) return base
        val wrong = prefs.getInt("category_wrong_${category.name}", 0)
        val accuracy = (total - wrong).toFloat() / total
        return when {
            accuracy < 0.55f -> (base - 1).coerceAtLeast(1)
            accuracy > 0.85f && total >= 10 -> (base + 1).coerceAtMost(5)
            else -> base
        }
    }

    /** Rota materias y refuerza las que presentan más errores. */
    fun selectCategory(
        context: Context,
        categories: List<QuestionGenerator.Category>
    ): QuestionGenerator.Category {
        if (categories.size == 1) return categories.first()
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val last = prefs.getString("last_category", null)
        val candidates = categories.filter { it.name != last }.ifEmpty { categories }
        val leastShown = candidates.minOf { prefs.getInt("category_shown_${it.name}", 0) }
        val balanced = candidates.filter {
            prefs.getInt("category_shown_${it.name}", 0) <= leastShown + 1
        }
        val selected = balanced.maxByOrNull { category ->
            val total = prefs.getInt("category_total_${category.name}", 0)
            val wrong = prefs.getInt("category_wrong_${category.name}", 0)
            if (total == 0) 1.0 else wrong.toDouble() / total
        } ?: candidates.random()
        prefs.edit()
            .putString("last_category", selected.name)
            .putInt("category_shown_${selected.name}", prefs.getInt("category_shown_${selected.name}", 0) + 1)
            .apply()
        return selected
    }

    private fun reviewDelayMillis(correct: Int, wrong: Int, wasCorrect: Boolean): Long {
        if (!wasCorrect) return 5 * 60_000L
        val mastery = (correct - wrong).coerceIn(0, 5)
        val hours = when (mastery) {
            0 -> 1L
            1 -> 6L
            2 -> 24L
            3 -> 72L
            else -> 168L
        }
        return hours * 60 * 60_000L
    }
}
