package com.example.calculaparajugar.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.calculaparajugar.databinding.ActivityLearningBinding
import com.example.calculaparajugar.util.AppConfig
import com.example.calculaparajugar.util.ProgressManager

class LearningActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLearningBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLearningBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        binding.btnBack.setOnClickListener { finish() }
        binding.btnPractice.setOnClickListener {
            startActivity(Intent(this, QuestionActivity::class.java).apply {
                putExtra(QuestionActivity.EXTRA_PRACTICE_MODE, true)
            })
        }
        binding.btnPeriodicTable.setOnClickListener {
            startActivity(Intent(this, PeriodicTableActivity::class.java))
        }
        binding.btnLevelTest.setOnClickListener {
            startActivity(Intent(this, LevelTestActivity::class.java))
        }
        binding.btnDailyGoal.setOnClickListener {
            AppConfig.cycleDailyGoal(this)
            updateProgress()
        }
    }

    override fun onResume() {
        super.onResume()
        updateProgress()
    }

    private fun updateProgress() {
        val progress = ProgressManager.getProgress(this)
        val goal = AppConfig.getDailyGoal(this)
        val completed = progress.todayCorrect.coerceAtMost(goal)
        val attempts = progress.todayCorrect + progress.todayIncorrect
        binding.tvDailyGoal.text = "$completed de $goal preguntas correctas"
        binding.progressDailyGoal.max = goal
        binding.progressDailyGoal.progress = completed
        binding.btnDailyGoal.text = "Meta: $goal"
        binding.tvAccuracy.text = if (attempts == 0) "Aún no has practicado hoy"
        else "Precisión de hoy: ${progress.todayCorrect * 100 / attempts}%"
    }
}
