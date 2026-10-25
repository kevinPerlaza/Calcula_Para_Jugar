package com.example.calculaparajugar.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.calculaparajugar.databinding.ActivityLevelUpBinding
import com.example.calculaparajugar.util.MascotManager

/**
 * Pantalla de celebración al subir de nivel.
 * Muestra la mascota con animaciones y confetti.
 */
class LevelUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLevelUpBinding
    private var particleAnimator: AnimatorSet? = null

    companion object {
        const val EXTRA_NEW_LEVEL = "new_level"
        const val EXTRA_EVOLVED = "mascot_evolved"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLevelUpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()

        val newLevel = intent.getIntExtra(EXTRA_NEW_LEVEL, 1)
        val evolved = intent.getBooleanExtra(EXTRA_EVOLVED, false)

        val mascot = MascotManager.getStageForLevel(newLevel)

        binding.imgMascot.setImageResource(mascot.drawableRes)
        binding.tvLevelNumber.text = "Nivel $newLevel"
        binding.tvMascotName.text = "${mascot.name} · ${mascot.title}"
        binding.tvMascotDesc.text = mascot.description

        if (evolved) {
            binding.tvEvolved.visibility = View.VISIBLE
            binding.tvEvolved.text = "${mascot.name} evolucionó"
        }

        // Animaciones
        animateEntry()

        binding.btnContinue.applyPressFeedback()
        binding.btnContinue.setOnClickListener { finish() }
    }

    private fun animateEntry() {
        binding.imgMascot.alpha = 0f
        binding.imgMascot.scaleX = 0.96f
        binding.imgMascot.scaleY = 0.96f
        val scaleX = ObjectAnimator.ofFloat(binding.imgMascot, "scaleX", 0.96f, 1.06f, 1f)
        val scaleY = ObjectAnimator.ofFloat(binding.imgMascot, "scaleY", 0.96f, 1.06f, 1f)
        val mascotAlpha = ObjectAnimator.ofFloat(binding.imgMascot, "alpha", 0f, 1f)
        scaleX.duration = 520
        scaleY.duration = 520
        mascotAlpha.duration = 260
        scaleX.interpolator = UiMotion.easeOut
        scaleY.interpolator = UiMotion.easeOut
        mascotAlpha.interpolator = UiMotion.easeOut

        binding.tvLevelNumber.alpha = 0f
        binding.tvLevelNumber.translationY = 24f
        val fadeLevel = ObjectAnimator.ofFloat(binding.tvLevelNumber, "alpha", 0f, 1f)
        val slideLevel = ObjectAnimator.ofFloat(binding.tvLevelNumber, "translationY", 24f, 0f)
        fadeLevel.startDelay = 220
        slideLevel.startDelay = 220
        fadeLevel.duration = 260
        slideLevel.duration = 320
        fadeLevel.interpolator = UiMotion.easeOut
        slideLevel.interpolator = UiMotion.easeOut

        binding.tvMascotName.alpha = 0f
        val fadeName = ObjectAnimator.ofFloat(binding.tvMascotName, "alpha", 0f, 1f)
        fadeName.startDelay = 360
        fadeName.duration = 240
        fadeName.interpolator = UiMotion.easeOut

        binding.tvMascotDesc.alpha = 0f
        val fadeDesc = ObjectAnimator.ofFloat(binding.tvMascotDesc, "alpha", 0f, 1f)
        fadeDesc.startDelay = 460
        fadeDesc.duration = 240
        fadeDesc.interpolator = UiMotion.easeOut

        binding.btnContinue.alpha = 0f
        val fadeBtn = ObjectAnimator.ofFloat(binding.btnContinue, "alpha", 0f, 1f)
        fadeBtn.startDelay = 560
        fadeBtn.duration = 260
        fadeBtn.interpolator = UiMotion.easeOut

        AnimatorSet().apply {
            playTogether(scaleX, scaleY, mascotAlpha, fadeLevel, slideLevel, fadeName, fadeDesc, fadeBtn)
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    startParticleMotion()
                    startIdleMascotMotion()
                }
            })
            start()
        }
    }

    private fun startParticleMotion() {
        val pulseX = ObjectAnimator.ofFloat(binding.tvParticles, "scaleX", 0.96f, 1.08f, 0.96f)
        val pulseY = ObjectAnimator.ofFloat(binding.tvParticles, "scaleY", 0.96f, 1.08f, 0.96f)
        pulseX.repeatCount = ObjectAnimator.INFINITE
        pulseY.repeatCount = ObjectAnimator.INFINITE
        pulseX.duration = 2400
        pulseY.duration = 2400
        pulseX.interpolator = UiMotion.easeInOut
        pulseY.interpolator = UiMotion.easeInOut
        particleAnimator = AnimatorSet().apply {
            playTogether(pulseX, pulseY)
            start()
        }
    }

    private fun startIdleMascotMotion() {
        binding.imgMascot.animate()
            .translationY(-6f)
            .rotation(2f)
            .setDuration(1800)
            .setInterpolator(UiMotion.easeInOut)
            .withEndAction {
                binding.imgMascot.animate()
                    .translationY(0f)
                    .rotation(-2f)
                    .setDuration(1800)
                    .setInterpolator(UiMotion.easeInOut)
                    .withEndAction { startIdleMascotMotion() }
                    .start()
            }
            .start()
    }

    override fun onDestroy() {
        super.onDestroy()
        particleAnimator?.cancel()
        binding.imgMascot.animate().cancel()
    }
}
