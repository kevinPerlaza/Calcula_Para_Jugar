package com.example.calculaparajugar.util

import android.content.Context
import com.example.calculaparajugar.data.QuestionBank
import kotlin.math.max
import kotlin.random.Random

/** Prioriza preguntas falladas sin dejar de introducir contenido nuevo. */
object AdaptiveQuestionManager {
    private const val PREFS = "brainwall_adaptive"

    fun recordAnswer(context: Context, questionId: String, correct: Boolean) {
        if (questionId.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val prefix = if (correct) "correct_" else "wrong_"
        val key = prefix + questionId
        prefs.edit().putInt(key, prefs.getInt(key, 0) + 1).apply()
    }

    fun select(context: Context, questions: List<QuestionBank.Question>): QuestionBank.Question? {
        if (questions.isEmpty()) return null
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val weighted = questions.map { question ->
            val correct = prefs.getInt("correct_${question.id}", 0)
            val wrong = prefs.getInt("wrong_${question.id}", 0)
            question to max(1, 3 + wrong * 4 - correct)
        }
        var pick = Random.nextInt(weighted.sumOf { it.second })
        for ((question, weight) in weighted) {
            pick -= weight
            if (pick < 0) return question
        }
        return weighted.last().first
    }
}
