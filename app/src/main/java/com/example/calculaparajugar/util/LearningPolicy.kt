package com.example.calculaparajugar.util

import kotlin.math.max

/** Reglas puras y comprobables del motor de aprendizaje. */
object LearningPolicy {
    const val FAST_ANSWER_MS = 8_000L
    const val SLOW_ANSWER_MS = 20_000L

    fun reviewDelayMillis(correct: Int, wrong: Int, wasCorrect: Boolean, responseMs: Long): Long {
        if (!wasCorrect) return 5 * 60_000L
        val speedAdjustment = when {
            responseMs <= FAST_ANSWER_MS -> 1
            responseMs >= SLOW_ANSWER_MS -> -1
            else -> 0
        }
        val mastery = (correct - wrong + speedAdjustment).coerceIn(0, 5)
        val hours = when (mastery) {
            0 -> 1L
            1 -> 6L
            2 -> 24L
            3 -> 72L
            else -> 168L
        }
        return hours * 60 * 60_000L
    }

    fun adaptiveDifficulty(base: Int, total: Int, wrong: Int, averageResponseMs: Long): Int {
        if (total < 5) return base.coerceIn(1, 5)
        val accuracy = (total - wrong).toFloat() / total
        return when {
            accuracy < 0.55f || averageResponseMs > SLOW_ANSWER_MS -> base - 1
            accuracy > 0.85f && total >= 10 && averageResponseMs < 15_000L -> base + 1
            else -> base
        }.coerceIn(1, 5)
    }

    fun questionWeight(correct: Int, wrong: Int, conceptAccuracy: Double): Int {
        val attempts = correct + wrong
        if (attempts == 0) return 9
        val conceptBoost = when {
            conceptAccuracy < 0.55 -> 5
            conceptAccuracy < 0.75 -> 2
            else -> 0
        }
        return max(1, 3 + wrong * 5 - correct * 2 + conceptBoost)
    }
}
