package com.example.calculaparajugar.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Banco de preguntas cargado desde assets/questions/.
 * Preguntas organizadas por materia, lección y dificultad.
 *
 * Estructura JSON por pregunta:
 * {
 *   "id": "MAT-01-001",
 *   "subject": "Matemáticas",
 *   "lesson": 1,
 *   "difficulty": 1,
 *   "question": "8 + 5 = ?",
 *   "options": ["12", "14", "15", "13"],
 *   "answer": "13",
 *   "explanation": "Sumas 8 y 5: 8+5=13."
 * }
 */
object QuestionBank {

    data class Question(
        val id: String,
        val subject: String,
        val lesson: Int,
        val difficulty: Int,
        val question: String,
        val options: List<String>,
        val answer: String,
        val explanation: String,
        val feedbackWrong: String,
        val whyCorrect: String,
        val commonError: String,
        val jumpEligible: Boolean,
        val poolJumpL1L2: Boolean,
        val poolJumpL2L3: Boolean,
        val poolJumpL3L4: Boolean,
        val poolJumpL4L5: Boolean
    )

    private var questions: List<Question> = emptyList()
    private var loaded = false

    /**
     * Carga todas las preguntas desde assets/questions/
     */
    fun load(context: Context) {
        if (loaded) return
        val allQuestions = mutableListOf<Question>()

        val files = listOf(
            "questions_math.json",
            "questions_english.json",
            "questions_python.json",
            "questions_curiosidades.json",
            "questions_periodic_table.json"
        )

        for (file in files) {
            try {
                val json = context.assets.open("questions/$file")
                    .bufferedReader().use { it.readText() }
                val array = JSONArray(json)
                for (i in 0 until array.length()) {
                    allQuestions.add(parseQuestion(array.getJSONObject(i)))
                }
            } catch (e: Exception) {
                // Archivo no encontrado aún, skip
            }
        }

        questions = allQuestions
        loaded = true
    }

    /**
     * Filtra preguntas por materia, lección y dificultad.
     */
    fun getQuestions(
        subject: String,
        lesson: Int? = null,
        difficulty: Int? = null
    ): List<Question> {
        return questions.filter { q ->
            q.subject == subject &&
            (lesson == null || q.lesson == lesson) &&
            (difficulty == null || q.difficulty == difficulty)
        }
    }

    /**
     * Obtiene una pregunta aleatoria según filtros.
     */
    fun getRandomQuestion(
        subject: String,
        lesson: Int? = null,
        difficulty: Int? = null
    ): Question? {
        val filtered = getQuestions(subject, lesson, difficulty)
        return if (filtered.isNotEmpty()) filtered.random() else null
    }

    fun difficultyForUserLevel(userLevel: Int): Int = when {
        userLevel <= 3 -> 1
        userLevel <= 6 -> 2
        userLevel <= 10 -> 3
        userLevel <= 15 -> 4
        else -> 5
    }

    fun getJumpQuestions(
        subject: String,
        jumpStage: Int,
        difficulty: Int,
        excludeIds: Set<String>
    ): List<Question> {
        return questions.filter { q ->
            q.subject == subject &&
                q.difficulty == difficulty &&
                q.id !in excludeIds &&
                when (jumpStage) {
                    1 -> q.poolJumpL1L2
                    2 -> q.poolJumpL2L3
                    3 -> q.poolJumpL3L4
                    4 -> q.poolJumpL4L5
                    else -> q.jumpEligible
                }
        }
    }

    private fun parseQuestion(obj: JSONObject): Question {
        val options = mutableListOf<String>()
        val optArray = obj.getJSONArray("options")
        for (i in 0 until optArray.length()) {
            options.add(optArray.getString(i))
        }

        return Question(
            id = obj.getString("id"),
            subject = obj.getString("subject"),
            lesson = obj.getInt("lesson"),
            difficulty = obj.getInt("difficulty"),
            question = obj.getString("question"),
            options = options,
            answer = obj.getString("answer"),
            explanation = obj.optString("explanation", ""),
            feedbackWrong = obj.optString("feedbackWrong", ""),
            whyCorrect = obj.optString("whyCorrect", ""),
            commonError = obj.optString("commonError", ""),
            jumpEligible = obj.optString("jumpEligible", "No").equals("Sí", ignoreCase = true),
            poolJumpL1L2 = obj.optString("poolJumpL1L2", "No").equals("Sí", ignoreCase = true),
            poolJumpL2L3 = obj.optString("poolJumpL2L3", "No").equals("Sí", ignoreCase = true),
            poolJumpL3L4 = obj.optString("poolJumpL3L4", "No").equals("Sí", ignoreCase = true),
            poolJumpL4L5 = obj.optString("poolJumpL4L5", "No").equals("Sí", ignoreCase = true)
        )
    }
}
