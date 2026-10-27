package com.example.calculaparajugar.ui

import android.content.Intent
import android.app.KeyguardManager
import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.calculaparajugar.R
import com.example.calculaparajugar.data.QuestionBank
import com.example.calculaparajugar.databinding.ActivityQuestionBinding
import com.example.calculaparajugar.service.MonitorService
import com.example.calculaparajugar.util.AppConfig
import com.example.calculaparajugar.util.AdaptiveQuestionManager
import com.example.calculaparajugar.util.LessonManager
import com.example.calculaparajugar.util.ProgressManager
import com.example.calculaparajugar.util.QuestionGenerator
import com.google.android.material.button.MaterialButton

/**
 * Pantalla de pregunta.
 * Usa QuestionBank (JSON) si hay preguntas disponibles,
 * si no, cae al LessonManager hardcodeado como fallback.
 */
class QuestionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PRACTICE_MODE = "practice_mode"
    }

    private lateinit var binding: ActivityQuestionBinding
    private var correctAnswer: String = ""
    private var explanation: String = ""
    private var correctionFeedback: String = ""
    private var currentCategory: QuestionGenerator.Category = QuestionGenerator.Category.MATH
    private var answerLocked: Boolean = false
    private var correctButtonIndex: Int = -1
    private var currentBankQuestion: QuestionBank.Question? = null
    private var questionStartedAt: Long = 0L
    private val optionLetters = listOf("A", "B", "C", "D")
    private val recentQuestionIds = mutableListOf<String>()
    private val uiHandler = Handler(Looper.getMainLooper())
    private var isPracticeMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        binding = ActivityQuestionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        isPracticeMode = intent.getBooleanExtra(EXTRA_PRACTICE_MODE, false)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isPracticeMode) finish()
            }
        })

        if (!isPracticeMode) MonitorService.isQuestionVisible = true
        binding.btnClosePractice.visibility = if (isPracticeMode) View.VISIBLE else View.GONE
        binding.btnClosePractice.setOnClickListener { finish() }
        setupMicroInteractions()
        binding.questionSheet.playSheetEnter()
        binding.imgCategoryIcon.playSoftPopEnter()
        loadQuestion()

        binding.btnSubmitAnswer.setOnClickListener { checkFreeAnswer() }
        binding.btnOption1.setOnClickListener { checkOptionAnswer(0) }
        binding.btnOption2.setOnClickListener { checkOptionAnswer(1) }
        binding.btnOption3.setOnClickListener { checkOptionAnswer(2) }
        binding.btnOption4.setOnClickListener { checkOptionAnswer(3) }
    }

    private fun setupMicroInteractions() {
        listOf(
            binding.btnSubmitAnswer,
            binding.btnOption1,
            binding.btnOption2,
            binding.btnOption3,
            binding.btnOption4
        ).forEach { it.applyPressFeedback() }
    }

    private fun loadQuestion() {
        answerLocked = false
        currentBankQuestion = null
        resetQuestionUi()

        // Leer categoría del intent (pasada por MonitorService), o aleatoria como fallback
        val categoryName = intent.getStringExtra("category")
        currentCategory = if (isPracticeMode) {
            AdaptiveQuestionManager.selectCategory(this, AppConfig.getEnabledCategories(this))
        } else if (categoryName != null) {
            try { QuestionGenerator.Category.valueOf(categoryName) }
            catch (e: IllegalArgumentException) { AppConfig.getEnabledCategories(this).random() }
        } else {
            AppConfig.getEnabledCategories(this).random()
        }

        val progress = ProgressManager.getProgress(this)
        val subject = categoryToSubject(currentCategory)
        val lesson = getLessonForCategory(currentCategory)

        // Intentar cargar del banco JSON
        val bankQuestion = getBankQuestionAvoidingRecent(subject, lesson, progress.level)

        if (bankQuestion != null) {
            showBankQuestion(bankQuestion)
        } else {
            // Fallback al LessonManager hardcodeado
            showLessonQuestion()
        }
    }

    private fun showBankQuestion(q: QuestionBank.Question) {
        binding.imgCategoryIcon.setImageResource(iconForCategory(currentCategory))
        binding.tvCategoryName.text = "${if (isPracticeMode) "PRÁCTICA" else "PREGUNTA"} · ${currentCategory.displayName}"
        binding.tvQuestion.text = q.question
        correctAnswer = q.answer
        explanation = q.explanation
        currentBankQuestion = q
        questionStartedAt = System.currentTimeMillis()
        rememberQuestion(q.id)

        // Siempre opciones múltiples desde el banco
        binding.layoutFreeAnswer.visibility = View.GONE
        binding.layoutOptions.visibility = View.VISIBLE
        val shuffled = q.options.shuffled()
        setOptionButtons(shuffled)
        correctionFeedback = buildBankCorrection(q)
    }

    private fun showLessonQuestion() {
        val lesson = LessonManager.getCurrentLesson(this, currentCategory)
        binding.imgCategoryIcon.setImageResource(iconForCategory(lesson.category))
        binding.tvCategoryName.text = "${if (isPracticeMode) "PRÁCTICA" else "PREGUNTA"} · ${lesson.category.displayName}"
        binding.tvQuestion.text = lesson.questionText
        correctAnswer = lesson.correctAnswer
        explanation = ""
        questionStartedAt = System.currentTimeMillis()

        if (lesson.options.isEmpty()) {
            binding.layoutFreeAnswer.visibility = View.VISIBLE
            binding.layoutOptions.visibility = View.GONE
            binding.etAnswer.text?.clear()
            binding.etAnswer.requestFocus()
            correctionFeedback = "La respuesta correcta era ${lesson.correctAnswer}."
        } else {
            binding.layoutFreeAnswer.visibility = View.GONE
            binding.layoutOptions.visibility = View.VISIBLE
            val shuffled = lesson.options.shuffled()
            setOptionButtons(shuffled)
            correctionFeedback = "La respuesta correcta era ${correctAnswerWithLetter()}."
        }
    }

    private fun checkFreeAnswer() {
        if (answerLocked) return
        val userAnswer = binding.etAnswer.text.toString().trim()
        if (userAnswer.isEmpty()) {
            binding.tvFeedback.text = "Ingresa una respuesta"
            binding.tvFeedback.visibility = View.VISIBLE
            shakeView(binding.etAnswer)
            return
        }
        if (userAnswer.equals(correctAnswer, ignoreCase = true)) onCorrect()
        else onIncorrect(null)
    }

    private fun checkOptionAnswer(index: Int) {
        if (answerLocked) return
        val buttons = listOf(binding.btnOption1, binding.btnOption2, binding.btnOption3, binding.btnOption4)
        val selected = buttons[index].tag?.toString().orEmpty()
        if (selected == correctAnswer) onCorrect(index) else onIncorrect(index)
    }

    private fun onCorrect(selectedIndex: Int? = null) {
        answerLocked = true
        setInputsEnabled(false)
        selectedIndex?.let { colorOption(it, true) }

        LessonManager.onCorrectAnswer(this, currentCategory)
        AdaptiveQuestionManager.recordAnswer(
            this, currentBankQuestion, true, currentCategory,
            System.currentTimeMillis() - questionStartedAt
        )
        val result = ProgressManager.onCorrect(this)
        val progress = ProgressManager.getProgress(this)

        val feedback = if (explanation.isNotEmpty())
            "Correcto · +${result.xpGained} XP · ${explanation}"
        else
            "Correcto · +${result.xpGained} XP · Nivel ${progress.level}"

        binding.tvFeedback.text = feedback
        binding.tvFeedback.setTextColor(getColor(R.color.correct_green))
        binding.tvFeedback.visibility = View.VISIBLE

        if (!isPracticeMode) MonitorService.markChallengeResolved()

        uiHandler.postDelayed({
            if (result.leveledUp) {
                startActivity(Intent(this, LevelUpActivity::class.java).apply {
                    putExtra(LevelUpActivity.EXTRA_NEW_LEVEL, result.newLevel)
                    putExtra(LevelUpActivity.EXTRA_EVOLVED, result.mascotEvolved)
                })
            }
            if (isPracticeMode) loadQuestion() else finish()
        }, 650)
    }

    private fun onIncorrect(selectedIndex: Int?) {
        answerLocked = true
        setInputsEnabled(false)
        selectedIndex?.let { colorOption(it, false) }
        if (correctButtonIndex >= 0) colorOption(correctButtonIndex, true)

        LessonManager.onIncorrectAnswer(this, currentCategory)
        AdaptiveQuestionManager.recordAnswer(
            this, currentBankQuestion, false, currentCategory,
            System.currentTimeMillis() - questionStartedAt
        )
        ProgressManager.onIncorrect(this)
        binding.tvFeedback.text = "Revisa la explicación"
        binding.tvFeedback.setTextColor(getColor(R.color.incorrect_red))
        binding.tvFeedback.visibility = View.VISIBLE

        shakeView(binding.tvQuestion)

        uiHandler.postDelayed({
            val feedback = correctionFeedback.ifBlank { "La respuesta correcta era $correctAnswer." }
            binding.tvQuestion.visibility = View.GONE
            binding.layoutOptions.visibility = View.GONE
            binding.layoutFreeAnswer.visibility = View.GONE
            binding.tvFeedback.text = feedback
        }, 650)

        uiHandler.postDelayed({
            loadQuestion()
        }, 3500)
    }

    private fun shakeView(view: View) {
        val shake = AnimationUtils.loadAnimation(this, R.anim.shake)
        view.startAnimation(shake)
    }

    private fun getBankQuestionAvoidingRecent(
        subject: String,
        lesson: Int,
        userLevel: Int
    ): QuestionBank.Question? {
        val diff = AdaptiveQuestionManager.difficultyFor(this, currentCategory, userLevel)
        val pools = listOf(
            QuestionBank.getQuestions(subject, lesson, diff),
            QuestionBank.getQuestions(subject, lesson, diff - 1),
            QuestionBank.getQuestions(subject, lesson, diff + 1),
            QuestionBank.getQuestions(subject, lesson)
        )

        for (pool in pools) {
            if (pool.isEmpty()) continue
            val fresh = pool.filter { it.id !in recentQuestionIds }
            return AdaptiveQuestionManager.select(this, fresh.ifEmpty { pool })
        }
        return null
    }

    private fun rememberQuestion(id: String) {
        recentQuestionIds.remove(id)
        recentQuestionIds.add(id)
        while (recentQuestionIds.size > 8) recentQuestionIds.removeAt(0)
    }

    private fun setOptionButtons(options: List<String>) {
        val buttons = optionButtons()
        correctButtonIndex = options.indexOf(correctAnswer)
        buttons.forEachIndexed { index, button ->
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
        return if (letter != null) "$letter: $correctAnswer" else correctAnswer
    }

    private fun buildBankCorrection(q: QuestionBank.Question): String {
        val why = q.whyCorrect.ifBlank { q.explanation }
        val commonError = q.commonError.ifBlank { extractCommonError(q.feedbackWrong) }
        return buildString {
            append("La respuesta correcta era ${correctAnswerWithLetter()}.")
            if (why.isNotBlank()) append("\n$why")
            if (commonError.isNotBlank()) append("\nError común: $commonError")
            if (q.remedialAction.isNotBlank()) append("\nSiguiente paso: ${q.remedialAction}")
        }
    }

    private fun extractCommonError(feedback: String): String {
        return feedback.substringAfter("Error común:", "")
            .substringBefore("Acción:")
            .trim()
            .trimEnd('.', ' ')
    }

    private fun resetQuestionUi() {
        binding.tvQuestion.visibility = View.VISIBLE
        binding.layoutFreeAnswer.visibility = View.GONE
        binding.layoutOptions.visibility = View.GONE
        binding.tvFeedback.text = ""
        binding.tvFeedback.visibility = View.GONE
        binding.etAnswer.text?.clear()
        setInputsEnabled(true)
        optionButtons().forEach { resetOptionButton(it) }
    }

    private fun setInputsEnabled(enabled: Boolean) {
        binding.etAnswer.isEnabled = enabled
        binding.btnSubmitAnswer.isEnabled = enabled
        optionButtons().forEach { it.isEnabled = enabled }
    }

    private fun optionButtons(): List<MaterialButton> {
        return listOf(binding.btnOption1, binding.btnOption2, binding.btnOption3, binding.btnOption4)
    }

    private fun resetOptionButton(button: MaterialButton) {
        button.backgroundTintList = null
        button.strokeColor = ColorStateList.valueOf(getColor(R.color.card_stroke_dark))
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

    private fun categoryToSubject(cat: QuestionGenerator.Category): String = when (cat) {
        QuestionGenerator.Category.MATH -> "Matemáticas"
        QuestionGenerator.Category.ENGLISH -> "Inglés"
        QuestionGenerator.Category.PYTHON -> "Python"
        QuestionGenerator.Category.CURIOSIDADES -> "Curiosidades"
        QuestionGenerator.Category.PERIODIC_TABLE -> "Tabla periódica"
    }

    private fun getLessonForCategory(cat: QuestionGenerator.Category): Int {
        val info = LessonManager.getProgressInfo(this, cat)
        return (info.first + 1).coerceAtLeast(1) // Lección actual (1-indexed)
    }

    private fun iconForCategory(cat: QuestionGenerator.Category): Int = when (cat) {
        QuestionGenerator.Category.MATH -> R.drawable.ic_cat_math
        QuestionGenerator.Category.ENGLISH -> R.drawable.ic_cat_english
        QuestionGenerator.Category.PYTHON -> R.drawable.ic_cat_python
        QuestionGenerator.Category.CURIOSIDADES -> R.drawable.ic_cat_curiosidades
        QuestionGenerator.Category.PERIODIC_TABLE -> R.drawable.ic_ui_book
    }

    override fun onPause() {
        super.onPause()
        // Si el usuario intenta escapar (recientes, home, etc.), volver al frente
        if (!isPracticeMode && MonitorService.isQuestionVisible && canStayInFront()) {
            val relaunch = Intent(this, QuestionActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(relaunch)
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isPracticeMode) MonitorService.isQuestionVisible = true
    }

    private fun canStayInFront(): Boolean {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return powerManager.isInteractive && !keyguardManager.isDeviceLocked
    }

    override fun onStop() {
        super.onStop()
        if (!isPracticeMode && !isFinishing) {
            MonitorService.isQuestionVisible = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        uiHandler.removeCallbacksAndMessages(null)
        if (!isPracticeMode) MonitorService.isQuestionVisible = false
    }
}
