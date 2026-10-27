package com.example.calculaparajugar.util

import android.content.Context
import com.example.calculaparajugar.data.QuestionBank
import kotlin.random.Random

/** Prioriza preguntas falladas sin dejar de introducir contenido nuevo. */
object AdaptiveQuestionManager {
    private const val PREFS = "brainwall_adaptive"
    private const val RECENT_WINDOW_MS = 24 * 60 * 60_000L
    fun recordAnswer(
        context: Context,
        question: QuestionBank.Question?,
        correct: Boolean,
        category: QuestionGenerator.Category,
        responseMs: Long
    ) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val categoryKey = category.name
        editor.putInt("category_total_$categoryKey", prefs.getInt("category_total_$categoryKey", 0) + 1)
        if (!correct) {
            editor.putInt("category_wrong_$categoryKey", prefs.getInt("category_wrong_$categoryKey", 0) + 1)
        }
        val responseTotal = prefs.getLong("category_response_$categoryKey", 0L) + responseMs
        editor.putLong("category_response_$categoryKey", responseTotal)
        val questionId = question?.id.orEmpty()
        if (questionId.isBlank()) {
            editor.apply()
            return
        }
        val prefix = if (correct) "correct_" else "wrong_"
        val key = prefix + questionId
        val correctCount = prefs.getInt("correct_$questionId", 0) + if (correct) 1 else 0
        val wrongCount = prefs.getInt("wrong_$questionId", 0) + if (correct) 0 else 1
        val topicKey = topicKey(question?.topic.orEmpty())
        val knownTopics = prefs.getStringSet("known_topics", emptySet()).orEmpty() + topicKey
        val topicTotal = prefs.getInt("topic_total_$topicKey", 0) + 1
        val topicWrong = prefs.getInt("topic_wrong_$topicKey", 0) + if (correct) 0 else 1
        val delay = LearningPolicy.reviewDelayMillis(correctCount, wrongCount, correct, responseMs)
        editor
            .putInt(key, prefs.getInt(key, 0) + 1)
            .putLong("seen_$questionId", System.currentTimeMillis())
            .putLong("due_$questionId", System.currentTimeMillis() + delay)
            .putString("topic_name_$topicKey", question?.topic.orEmpty())
            .putStringSet("known_topics", knownTopics)
            .putInt("topic_total_$topicKey", topicTotal)
            .putInt("topic_wrong_$topicKey", topicWrong)
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
            val key = topicKey(question.topic)
            val topicTotal = prefs.getInt("topic_total_$key", 0)
            val topicWrong = prefs.getInt("topic_wrong_$key", 0)
            val topicAccuracy = if (topicTotal == 0) 1.0 else (topicTotal - topicWrong).toDouble() / topicTotal
            question to LearningPolicy.questionWeight(correct, wrong, topicAccuracy)
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
        val wrong = prefs.getInt("category_wrong_${category.name}", 0)
        val averageResponse = if (total == 0) 0L else
            prefs.getLong("category_response_${category.name}", 0L) / total
        return LearningPolicy.adaptiveDifficulty(base, total, wrong, averageResponse)
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

    fun getWeakTopics(context: Context, limit: Int = 5): List<Pair<String, Int>> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getStringSet("known_topics", emptySet()).orEmpty().mapNotNull { key ->
            val total = prefs.getInt("topic_total_$key", 0)
            if (total < 2) return@mapNotNull null
            val wrong = prefs.getInt("topic_wrong_$key", 0)
            val errorRate = wrong * 100 / total
            (prefs.getString("topic_name_$key", key) ?: key) to errorRate
        }.sortedByDescending { it.second }.take(limit)
    }

    private fun topicKey(topic: String): String = topic.lowercase()
        .replace(Regex("[^a-záéíóúüñ0-9]+"), "_")
        .trim('_')
}
