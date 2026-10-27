package com.example.calculaparajugar.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.calculaparajugar.databinding.ActivityStatsBinding
import com.example.calculaparajugar.util.AdaptiveQuestionManager
import com.example.calculaparajugar.util.ProgressManager

class StatsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityStatsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        binding.btnBack.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        val progress = ProgressManager.getProgress(this)
        val attempts = progress.todayCorrect + progress.todayIncorrect
        binding.tvLevel.text = "Nivel ${progress.level} · ${progress.xpTotal} XP totales"
        binding.tvToday.text = "Hoy: ${progress.todayCorrect} correctas · ${progress.todayIncorrect} por repasar"
        binding.tvAccuracy.text = if (attempts == 0) "Precisión: sin respuestas hoy"
        else "Precisión: ${progress.todayCorrect * 100 / attempts}%"
        binding.tvStreak.text = "Racha: ${progress.streakDays} día${if (progress.streakDays == 1) "" else "s"}"
        val weak = AdaptiveQuestionManager.getWeakTopics(this)
        binding.tvWeakTopics.text = if (weak.isEmpty()) {
            "Responde algunas preguntas para detectar los temas que necesitan refuerzo."
        } else weak.joinToString("\n") { (topic, error) -> "• $topic · $error% de errores" }
    }
}
