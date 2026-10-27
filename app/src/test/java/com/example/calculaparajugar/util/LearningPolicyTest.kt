package com.example.calculaparajugar.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningPolicyTest {
    @Test
    fun failedAnswerReturnsSoon() {
        assertEquals(5 * 60_000L, LearningPolicy.reviewDelayMillis(0, 1, false, 12_000L))
    }

    @Test
    fun fastCorrectAnswerExtendsReview() {
        val fast = LearningPolicy.reviewDelayMillis(2, 0, true, 5_000L)
        val slow = LearningPolicy.reviewDelayMillis(2, 0, true, 25_000L)
        assertTrue(fast > slow)
    }

    @Test
    fun weakPerformanceLowersDifficulty() {
        assertEquals(2, LearningPolicy.adaptiveDifficulty(3, 10, 6, 12_000L))
    }

    @Test
    fun strongFastPerformanceRaisesDifficulty() {
        assertEquals(4, LearningPolicy.adaptiveDifficulty(3, 20, 1, 9_000L))
    }

    @Test
    fun difficultyAlwaysStaysInRange() {
        assertEquals(1, LearningPolicy.adaptiveDifficulty(1, 10, 9, 30_000L))
        assertEquals(5, LearningPolicy.adaptiveDifficulty(5, 20, 0, 5_000L))
    }

    @Test
    fun weakConceptReceivesMoreWeight() {
        val weak = LearningPolicy.questionWeight(correct = 1, wrong = 2, conceptAccuracy = 0.4)
        val strong = LearningPolicy.questionWeight(correct = 1, wrong = 2, conceptAccuracy = 0.9)
        assertTrue(weak > strong)
    }
}
