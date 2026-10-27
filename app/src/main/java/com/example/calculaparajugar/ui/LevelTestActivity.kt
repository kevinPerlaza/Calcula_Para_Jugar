package com.example.calculaparajugar.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.calculaparajugar.R
import com.example.calculaparajugar.data.QuestionBank
import com.example.calculaparajugar.databinding.ActivityLevelTestBinding
import com.example.calculaparajugar.util.AppConfig
import com.example.calculaparajugar.util.LessonManager
import com.example.calculaparajugar.util.ProgressManager
import com.example.calculaparajugar.util.QuestionGenerator
import com.google.android.material.button.MaterialButton

/**
 * Prueba de nivel con 15 preguntas (5 fáciles + 5 medias + 5 difíciles).
 *
 * Resultados:
 * - 0%-59%: No puede saltar. Debe repasar.
 * - 60%-79%: Puede repetir o practicar.
 * - 80%-94%: Salta 1 nivel.
 * - 95%-100%: Salta 2 niveles.
 *
 * Reglas:
 * - No se puede saltar a ecuaciones sin dominar operaciones básicas.
 * - Curiosidades es más flexible (no requiere orden).
 */
class LevelTestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLevelTestBinding

    private var currentQuestionIndex = 0
    private var correctCount = 0
    private var totalQuestions = 10
    private var currentAnswer = ""
    private var currentExplanation = ""
    private var currentCorrectionFeedback = ""
    private var currentDifficulty = 1
    private var answerLocked = false
    private var correctButtonIndex = -1
    private lateinit var jumpRule: JumpRule
    private val askedIds = mutableSetOf<String>()
    private val correctByDifficulty = mutableMapOf<Int, Int>()
    private val totalByDifficulty = mutableMapOf<Int, Int>()
    private val optionLetters = listOf("A", "B", "C", "D")

    private data class JumpRule(
        val stage: Int,
        val targetLabel: String,
        val difficultySequence: List<Int>,
        val passPercent: Int,
        val gateDifficulty: Int,
        val gateCorrect: Int
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLevelTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupMicroInteractions()

        binding.btnBack.setOnClickListener { finish() }
        binding.btnSubmitAnswer.setOnClickListener { checkFreeAnswer() }
        binding.btnOption1.setOnClickListener { checkOption(0) }
        binding.btnOption2.setOnClickListener { checkOption(1) }
        binding.btnOption3.setOnClickListener { checkOption(2) }
        binding.btnOption4.setOnClickListener { checkOption(3) }
        binding.btnFinish.setOnClickListener { finish() }
        binding.btnRetry.setOnClickListener { retry() }

        jumpRule = buildJumpRuleForCurrentLevel()
        totalQuestions = jumpRule.difficultySequence.size
        showNextQuestion()
    }

    private fun setupMicroInteractions() {
        listOf(
            binding.btnBack,
            binding.btnSubmitAnswer,
            binding.btnOption1,
            binding.btnOption2,
            binding.btnOption3,
            binding.btnOption4,
            binding.btnFinish,
            binding.btnRetry
        ).forEach { it.applyPressFeedback() }
    }

    private fun showNextQuestion() {
        if (currentQuestionIndex >= totalQuestions) {
            showResult()
            return
        }

        binding.layoutQuestion.visibility = View.VISIBLE
        binding.layoutResult.visibility = View.GONE
        binding.btnSubmitAnswer.visibility = View.GONE

        val difficulty = jumpRule.difficultySequence[currentQuestionIndex]
        currentDifficulty = difficulty
        val categories = AppConfig.getEnabledCategories(this)
        val category = categories.random()
        val subject = categoryToSubject(category)

        val q = QuestionBank.getJumpQuestions(subject, jumpRule.stage, difficulty, askedIds).randomOrNull()
            ?: QuestionBank.getRandomQuestion(subject, difficulty = difficulty)

        if (q != null) {
            askedIds.add(q.id)
            binding.tvQuestion.text = q.question
            currentAnswer = q.answer
            currentExplanation = q.explanation

            binding.layoutFreeAnswer.visibility = View.GONE
            binding.layoutOptions.visibility = View.VISIBLE
            val shuffled = q.options.shuffled()
            setOptionButtons(shuffled)
            currentCorrectionFeedback = buildBankCorrection(q)
        } else {
            // Fallback: usar generador viejo
            QuestionGenerator.setDifficulty(
                when (difficulty) { 1, 2 -> QuestionGenerator.Difficulty.EASY
                    3 -> QuestionGenerator.Difficulty.MEDIUM
                    else -> QuestionGenerator.Difficulty.HARD }
            )
            val gen = QuestionGenerator.generate(category)
            binding.tvQuestion.text = gen.text
            currentAnswer = gen.correctAnswer
            currentExplanation = ""
            currentCorrectionFeedback = "Respuesta correcta: ${gen.correctAnswer}."

            if (gen.options.isEmpty()) {
                binding.layoutFreeAnswer.visibility = View.VISIBLE
                binding.layoutOptions.visibility = View.GONE
                binding.etAnswer.text?.clear()
            } else {
                binding.layoutFreeAnswer.visibility = View.GONE
                binding.layoutOptions.visibility = View.VISIBLE
                val shuffled = gen.options.shuffled()
                setOptionButtons(shuffled)
                currentCorrectionFeedback = "La respuesta correcta era ${correctAnswerWithLetter()}."
            }
        }

        binding.tvProgress.text = "${currentQuestionIndex + 1} / $totalQuestions"
        binding.progressTest.max = totalQuestions
        binding.progressTest.progress = currentQuestionIndex
        binding.tvFeedback.visibility = View.GONE
        binding.tvFeedback.text = ""
        answerLocked = false
        setInputsEnabled(true)
        optionButtons().forEach { resetOptionButton(it) }

        binding.tvDiffLabel.text = when (difficulty) {
            1 -> "Básico"
            2 -> "Elemental"
            3 -> "Intermedio"
            4 -> "Avanzado"
            else -> "Experto"
        }
    }

    private fun checkFreeAnswer() {
        val answer = binding.etAnswer.text.toString().trim()
        if (answer.isEmpty()) return
        processAnswer(answer.equals(currentAnswer, ignoreCase = true), null)
    }

    private fun checkOption(index: Int) {
        val buttons = listOf(binding.btnOption1, binding.btnOption2, binding.btnOption3, binding.btnOption4)
        processAnswer(buttons[index].tag?.toString() == currentAnswer, index)
    }

    private fun processAnswer(correct: Boolean, selectedIndex: Int?) {
        if (answerLocked) return
        answerLocked = true
        setInputsEnabled(false)
        selectedIndex?.let { colorOption(it, correct) }
        if (!correct && correctButtonIndex >= 0) colorOption(correctButtonIndex, true)

        totalByDifficulty[currentDifficulty] = (totalByDifficulty[currentDifficulty] ?: 0) + 1
        if (correct) {
            correctCount++
            correctByDifficulty[currentDifficulty] = (correctByDifficulty[currentDifficulty] ?: 0) + 1
        }

        // Siempre mostrar feedback
        binding.tvFeedback.visibility = View.VISIBLE
        if (correct) {
            binding.tvFeedback.setTextColor(getColor(R.color.correct_green))
            binding.tvFeedback.text = "✅ ¡Correcto! ${currentExplanation}"
        } else {
            binding.tvFeedback.setTextColor(getColor(R.color.incorrect_red))
            binding.tvFeedback.text = "❌ $currentCorrectionFeedback"
        }

        currentQuestionIndex++

        if (correct) {
            // Correcta: pausa breve para leer, luego avanza
            Handler(Looper.getMainLooper()).postDelayed({ showNextQuestion() }, 1800)
        } else {
            // Incorrecta: NO auto-avanza. Muestra botón "Continuar" abajo
            binding.layoutFreeAnswer.visibility = View.VISIBLE
            binding.btnSubmitAnswer.visibility = View.VISIBLE
            binding.btnSubmitAnswer.text = "Continuar →"
            binding.btnSubmitAnswer.isEnabled = true
            binding.etAnswer.visibility = View.GONE
            binding.btnSubmitAnswer.setOnClickListener {
                binding.layoutFreeAnswer.visibility = View.GONE
                binding.etAnswer.visibility = View.VISIBLE
                binding.btnSubmitAnswer.text = "Siguiente"
                binding.btnSubmitAnswer.setOnClickListener { checkFreeAnswer() }
                showNextQuestion()
            }
        }
    }

    private fun showResult() {
        binding.layoutQuestion.visibility = View.GONE
        binding.layoutResult.visibility = View.VISIBLE

        val percentage = (correctCount * 100) / totalQuestions

        binding.tvResultScore.text = "$correctCount/$totalQuestions"
        binding.tvResultPercentage.text = "$percentage%"

        val gateCorrect = correctByDifficulty[jumpRule.gateDifficulty] ?: 0
        val passed = percentage >= jumpRule.passPercent && gateCorrect >= jumpRule.gateCorrect

        if (passed) {
            val current = ProgressManager.getProgress(this).level
            val newLevel = current + 1
            ProgressManager.setLevel(this, newLevel)
            LessonManager.setLessonIndexForLevel(this, newLevel)

            binding.tvResultLevel.text = "Nivel superado: $current a $newLevel"
            binding.tvResultDesc.text = "Aprobaste ${jumpRule.targetLabel}. Las siguientes preguntas suben de dificultad."
            binding.tvResultLevel.setTextColor(getColor(R.color.primary))
            binding.btnRetry.visibility = View.GONE
        } else if (percentage >= 60) {
            binding.tvResultLevel.text = "Casi. Puedes repetir la prueba."
            binding.tvResultDesc.text = "Te faltan bases en dificultad ${jumpRule.gateDifficulty}. Practica ese bloque antes de saltar."
            binding.tvResultLevel.setTextColor(getColor(R.color.text_secondary))
            binding.btnRetry.visibility = View.VISIBLE
        } else {
            binding.tvResultLevel.text = "Aun no puedes saltar"
            binding.tvResultDesc.text = "No esta listo el salto. Refuerza las preguntas de menor dificultad y vuelve a intentarlo."
            binding.tvResultLevel.setTextColor(getColor(R.color.incorrect_red))
            binding.btnRetry.visibility = View.VISIBLE
        }
    }

    private fun retry() {
        currentQuestionIndex = 0
        correctCount = 0
        askedIds.clear()
        correctByDifficulty.clear()
        totalByDifficulty.clear()
        answerLocked = false
        showNextQuestion()
    }

    private fun setInputsEnabled(enabled: Boolean) {
        binding.etAnswer.isEnabled = enabled
        binding.btnSubmitAnswer.isEnabled = enabled
        binding.btnOption1.isEnabled = enabled
        binding.btnOption2.isEnabled = enabled
        binding.btnOption3.isEnabled = enabled
        binding.btnOption4.isEnabled = enabled
    }

    private fun setOptionButtons(options: List<String>) {
        correctButtonIndex = options.indexOf(currentAnswer)
        optionButtons().forEachIndexed { index, button ->
            val value = options.getOrNull(index).orEmpty()
            button.text = "${optionLetters[index]}. $value"
            button.tag = value
            button.visibility = if (value.isBlank()) View.GONE else View.VISIBLE
            resetOptionButton(button)
            if (value.isNotBlank()) button.playSoftPopEnter()
        }
    }

    private fun correctAnswerWithLetter(): String {
        val letter = optionLetters.getOrNull(correctButtonIndex)
        return if (letter != null) "$letter: $currentAnswer" else currentAnswer
    }

    private fun buildBankCorrection(q: QuestionBank.Question): String {
        val why = q.whyCorrect.ifBlank { q.explanation }
        val commonError = q.commonError.ifBlank {
            q.feedbackWrong.substringAfter("Error común:", "")
                .substringBefore("Acción:")
                .trim()
                .trimEnd('.', ' ')
        }
        return buildString {
            append("La respuesta correcta era ${correctAnswerWithLetter()}.")
            if (why.isNotBlank()) append("\n$why")
            if (commonError.isNotBlank()) append("\nError común: $commonError")
        }
    }

    private fun optionButtons(): List<MaterialButton> {
        return listOf(binding.btnOption1, binding.btnOption2, binding.btnOption3, binding.btnOption4)
    }

    private fun resetOptionButton(button: MaterialButton) {
        button.backgroundTintList = null
        button.strokeColor = ColorStateList.valueOf(getColor(R.color.primary))
        button.setTextColor(getColor(R.color.text_primary))
    }

    private fun colorOption(index: Int, isCorrect: Boolean) {
        val button = optionButtons().getOrNull(index) ?: return
        val color = if (isCorrect) R.color.correct_green else R.color.incorrect_red
        button.backgroundTintList = ColorStateList.valueOf(getColor(color))
        button.strokeColor = ColorStateList.valueOf(getColor(color))
        button.setTextColor(getColor(R.color.black))
        button.animate()
            .scaleX(0.98f)
            .scaleY(0.98f)
            .setDuration(90)
            .setInterpolator(UiMotion.easeOut)
            .withEndAction {
                button.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(140)
                    .setInterpolator(UiMotion.easeOut)
                    .start()
            }
            .start()
    }

    private fun buildJumpRuleForCurrentLevel(): JumpRule {
        val currentLevel = ProgressManager.getProgress(this).level
        val stage = QuestionBank.difficultyForUserLevel(currentLevel).coerceIn(1, 5)
        return when (stage) {
            1 -> JumpRule(1, "salto a dificultad 2", listOf(1, 1, 1, 1, 2, 2, 2, 2, 2, 2), 80, 2, 4)
            2 -> JumpRule(2, "salto a dificultad 3", listOf(2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 1, 1), 80, 3, 5)
            3 -> JumpRule(3, "salto a dificultad 4", listOf(3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 2, 2), 85, 4, 5)
            4 -> JumpRule(4, "salto a dificultad 5", listOf(4, 4, 4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 5, 3, 3), 85, 5, 7)
            else -> JumpRule(5, "modo reto", listOf(5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 4, 4, 3, 2, 1), 90, 5, 9)
        }
    }

    private fun categoryToSubject(cat: QuestionGenerator.Category): String = when (cat) {
        QuestionGenerator.Category.MATH -> "Matemáticas"
        QuestionGenerator.Category.ENGLISH -> "Inglés"
        QuestionGenerator.Category.PYTHON -> "Python"
        QuestionGenerator.Category.CURIOSIDADES -> "Curiosidades"
        QuestionGenerator.Category.PERIODIC_TABLE -> "Tabla periódica"
    }
}
