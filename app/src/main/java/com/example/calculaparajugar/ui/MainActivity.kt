package com.example.calculaparajugar.ui

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.calculaparajugar.R
import com.example.calculaparajugar.databinding.ActivityMainBinding
import com.example.calculaparajugar.service.MonitorService
import com.example.calculaparajugar.util.AppConfig
import com.example.calculaparajugar.util.MascotManager
import com.example.calculaparajugar.util.ProgressManager
import com.example.calculaparajugar.util.QuestionGenerator
import com.example.calculaparajugar.util.PinManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val appSelectorLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { updateBlockedCount() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()

        // El fondo ya aporta profundidad con degradados. Evitamos aplicar RenderEffect
        // a toda la vista para mantener el arranque estable en Android 8–11.

        when {
            PinManager.hasPin(this) -> showUnlockDialog()
            PinManager.shouldShowInitialPrompt(this) -> showSetPinDialog()
        }

        setupUI()
        setupMicroInteractions()
        loadConfig()
    }

    private fun setupUI() {
        binding.btnSelectApps.setOnClickListener {
            appSelectorLauncher.launch(Intent(this, AppSelectorActivity::class.java))
        }

        binding.btnPeriodicTable.setOnClickListener {
            startActivity(Intent(this, PeriodicTableActivity::class.java))
        }

        binding.sliderInterval.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val min = value.toInt()
                AppConfig.setIntervalMinutes(this, min)
                binding.tvIntervalValue.text = "Cada $min minuto${if (min > 1) "s" else ""}"
            }
        }

        // Categorías (multi-select)
        binding.chipGroupCategory.setOnCheckedStateChangeListener { _, checkedIds ->
            val cats = mutableSetOf<QuestionGenerator.Category>()
            if (checkedIds.contains(R.id.chipMath)) cats.add(QuestionGenerator.Category.MATH)
            if (checkedIds.contains(R.id.chipEnglish)) cats.add(QuestionGenerator.Category.ENGLISH)
            if (checkedIds.contains(R.id.chipPython)) cats.add(QuestionGenerator.Category.PYTHON)
            if (checkedIds.contains(R.id.chipCuriosidades)) cats.add(QuestionGenerator.Category.CURIOSIDADES)
            if (checkedIds.contains(R.id.chipPeriodicTable)) cats.add(QuestionGenerator.Category.PERIODIC_TABLE)
            if (cats.isEmpty()) {
                binding.chipMath.isChecked = true
                cats.add(QuestionGenerator.Category.MATH)
                Toast.makeText(this, "Mínimo una categoría", Toast.LENGTH_SHORT).show()
            }
            AppConfig.setEnabledCategories(this, cats)
        }

        // Permisos (ahora en Settings, pero necesitamos verificar antes de activar)
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Control
        binding.btnStartService.setOnClickListener {
            if (checkAllPermissions()) {
                val monitorAllApps = AppConfig.isMonitorAllAppsEnabled(this)
                if (!monitorAllApps && AppConfig.getBlockedApps(this).isEmpty()) {
                    Toast.makeText(this, "Selecciona al menos una app o activa todas", Toast.LENGTH_LONG).show()
                } else {
                    startForegroundService(Intent(this, MonitorService::class.java))
                    Toast.makeText(this, "BrainWall activado", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnStopService.setOnClickListener {
            val stopProtection = {
                stopService(Intent(this, MonitorService::class.java))
                Toast.makeText(this, "Protección desactivada", Toast.LENGTH_SHORT).show()
            }
            if (PinManager.hasPin(this)) {
                val dialog = PinDialogFragment.newInstanceWithMode(PinDialogFragment.MODE_UNLOCK_ACTION)
                dialog.onUnlockSuccess = stopProtection
                dialog.show(supportFragmentManager, "pin_stop")
            } else {
                stopProtection()
            }
        }

        binding.btnPauseService.setOnClickListener {
            AppConfig.pauseFor(this, 30 * 60_000L)
            Toast.makeText(this, "Preguntas pausadas durante 30 minutos", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadConfig() {
        val interval = AppConfig.getIntervalMinutes(this)
        binding.sliderInterval.value = interval.toFloat()
        binding.tvIntervalValue.text = "Cada $interval minuto${if (interval > 1) "s" else ""}"

        val cats = AppConfig.getEnabledCategories(this)
        binding.chipMath.isChecked = QuestionGenerator.Category.MATH in cats
        binding.chipEnglish.isChecked = QuestionGenerator.Category.ENGLISH in cats
        binding.chipPython.isChecked = QuestionGenerator.Category.PYTHON in cats
        binding.chipCuriosidades.isChecked = QuestionGenerator.Category.CURIOSIDADES in cats
        binding.chipPeriodicTable.isChecked = QuestionGenerator.Category.PERIODIC_TABLE in cats

        updateBlockedCount()
        updateProgress()
    }

    private fun updateProgress() {
        val p = ProgressManager.getProgress(this)
        binding.tvLevel.text = "${p.level}"
        binding.tvXp.text = "${p.xpInLevel}/${p.xpToNextLevel} XP"
        binding.tvStreak.text = "${p.streakDays}"
        binding.tvTodayCorrect.text = "${p.todayCorrect}"
        binding.tvTodayIncorrect.text = "${p.todayIncorrect}"
        binding.progressLevel.progress = (p.levelProgress * 100).toInt()

        // Actualizar mascota header
        val mascot = MascotManager.getStageForLevel(p.level)
        binding.imgMascotHeader.setImageResource(mascot.drawableRes)
        binding.tvMascotStatus.text = mascot.description

        // Mascota grande en el card
        binding.imgMascotBig.setImageResource(mascot.drawableRes)
        binding.tvMascotTitle.text = "${mascot.name} · ${mascot.title}"

        val next = MascotManager.getNextStage(p.level)
        if (next != null) {
            val levelsLeft = MascotManager.getLevelsToNextEvolution(p.level)
            binding.tvMascotEvolution.text = "Evoluciona en $levelsLeft nivel${if (levelsLeft > 1) "es" else ""}"
        } else {
            binding.tvMascotEvolution.text = "¡Forma final alcanzada!"
        }
    }

    private fun setupMicroInteractions() {
        listOf(
            binding.btnSettings,
            binding.btnSelectApps,
            binding.btnPeriodicTable,
            binding.btnStartService,
            binding.btnStopService,
            binding.btnPauseService,
            binding.chipMath,
            binding.chipEnglish,
            binding.chipPython,
            binding.chipCuriosidades,
            binding.chipPeriodicTable
        ).forEach { it.applyPressFeedback() }
    }

    private fun startMascotAnimations() {
        startHeaderMascotMotion()
        startHeroMascotMotion()
    }

    private fun startHeaderMascotMotion() {
        binding.imgMascotHeader.animate()
            .translationY(-3f)
            .rotation(2f)
            .setDuration(1800)
            .setInterpolator(UiMotion.easeInOut)
            .withEndAction {
                binding.imgMascotHeader.animate()
                    .translationY(0f)
                    .rotation(-1.5f)
                    .setDuration(1800)
                    .setInterpolator(UiMotion.easeInOut)
                    .withEndAction { startHeaderMascotMotion() }
                    .start()
            }
            .start()
    }

    private fun startHeroMascotMotion() {
        binding.imgMascotBig.animate()
            .translationY(-6f)
            .scaleX(1.025f)
            .scaleY(1.025f)
            .rotation(1.5f)
            .setDuration(2200)
            .setInterpolator(UiMotion.easeInOut)
            .withEndAction {
                binding.imgMascotBig.animate()
                    .translationY(0f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .rotation(-1.5f)
                    .setDuration(2200)
                    .setInterpolator(UiMotion.easeInOut)
                    .withEndAction { startHeroMascotMotion() }
                    .start()
            }
            .start()
    }

    private fun updateBlockedCount() {
        val count = AppConfig.getBlockedApps(this).size
        binding.tvBlockedCount.text = if (AppConfig.isMonitorAllAppsEnabled(this)) {
            "Todas las apps monitoreadas"
        } else if (count == 0)
            "Ninguna app seleccionada"
        else {
            "$count app${if (count > 1) "s" else ""} monitoreada${if (count > 1) "s" else ""}"
        }
    }

    private fun checkAllPermissions(): Boolean {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Concede permisos en Ajustes", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, SettingsActivity::class.java))
            return false
        }
        if (!hasUsageStatsPermission()) {
            Toast.makeText(this, "Concede permisos en Ajustes", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, SettingsActivity::class.java))
            return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Activa las notificaciones del servicio", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, SettingsActivity::class.java))
            return false
        }
        return true
    }

    private fun hasUsageStatsPermission(): Boolean {
        val aom = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        return aom.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName) == AppOpsManager.MODE_ALLOWED
    }

    private fun showSetPinDialog() {
        val dialog = PinDialogFragment.newInitialSetupInstance()
        dialog.onUnlockSuccess = { showGuideIfNeeded() }
        dialog.show(supportFragmentManager, "pin")
    }

    private fun showUnlockDialog() {
        val dialog = PinDialogFragment.newInstance(isSetup = false)
        dialog.onUnlockSuccess = { showGuideIfNeeded() }
        dialog.show(supportFragmentManager, "pin")
    }

    private fun showGuideIfNeeded() {
        if (AppConfig.hasSeenGuide(this)) return
        AppConfig.setGuideSeen(this, true)
        GuideDialogFragment().show(supportFragmentManager, "guide")
    }

    override fun onResume() {
        super.onResume()
        updateBlockedCount()
        updateProgress()
        startMascotAnimations()
    }

    override fun onPause() {
        super.onPause()
        binding.imgMascotHeader.animate().cancel()
        binding.imgMascotBig.animate().cancel()
    }
}
