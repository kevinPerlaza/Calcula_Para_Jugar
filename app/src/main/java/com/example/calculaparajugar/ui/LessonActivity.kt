package com.example.calculaparajugar.ui

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.calculaparajugar.databinding.ActivityLessonBinding
import com.example.calculaparajugar.service.MonitorService
import com.example.calculaparajugar.util.AppConfig
import com.example.calculaparajugar.util.LessonManager
import com.example.calculaparajugar.util.QuestionGenerator

/**
 * Muestra contenido educativo (la lección) sin pregunta.
 * El usuario lee y toca "Entendido" para continuar.
 */
class LessonActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLessonBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        binding = ActivityLessonBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = Unit
        })

        MonitorService.isQuestionVisible = true
        binding.lessonSheet.playSoftPopEnter()
        binding.btnUnderstood.applyPressFeedback()

        // Obtener categoría desde el intent del servicio, o aleatoria como fallback
        val categoryName = intent.getStringExtra("category")
        val category = if (categoryName != null) {
            try { QuestionGenerator.Category.valueOf(categoryName) }
            catch (e: IllegalArgumentException) { AppConfig.getEnabledCategories(this).random() }
        } else {
            AppConfig.getEnabledCategories(this).random()
        }
        val lesson = LessonManager.getCurrentLesson(this, category)

        binding.tvLessonTitle.text = lesson.title
        binding.tvLessonExplanation.text = lesson.explanation
        binding.tvLessonExample.text = "Ejemplo:\n${lesson.example}"

        binding.btnUnderstood.setOnClickListener {
            LessonManager.onLessonShown(this, category)
            MonitorService.markChallengeResolved()
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        if (MonitorService.isQuestionVisible && canStayInFront()) {
            val relaunch = Intent(this, LessonActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(relaunch)
        }
    }

    override fun onResume() {
        super.onResume()
        MonitorService.isQuestionVisible = true
    }

    private fun canStayInFront(): Boolean {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return powerManager.isInteractive && !keyguardManager.isDeviceLocked
    }

    override fun onStop() {
        super.onStop()
        if (!isFinishing) {
            MonitorService.isQuestionVisible = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        MonitorService.isQuestionVisible = false
    }
}
