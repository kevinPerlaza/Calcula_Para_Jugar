package com.example.calculaparajugar.ui

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.calculaparajugar.R
import com.example.calculaparajugar.databinding.ActivitySettingsBinding
import com.example.calculaparajugar.util.BackupManager
import com.example.calculaparajugar.util.PinManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    private val exportBackup = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        runCatching {
            contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                it.write(BackupManager.export(this))
            } ?: error("No se pudo abrir el archivo")
        }.onSuccess {
            Toast.makeText(this, "Respaldo guardado", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, "No se pudo guardar el respaldo", Toast.LENGTH_LONG).show()
        }
    }

    private val importBackup = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        runCatching {
            val json = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: error("No se pudo abrir el archivo")
            BackupManager.restore(this, json)
        }.onSuccess {
            updatePermissions()
            Toast.makeText(this, "Progreso restaurado", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, "El respaldo no es válido", Toast.LENGTH_LONG).show()
        }
    }

    private val requestNotifications = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { updatePermissions() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()

        setupMicroInteractions()

        binding.btnBack.setOnClickListener { finish() }

        binding.btnLevelTest.setOnClickListener {
            startActivity(Intent(this, LevelTestActivity::class.java))
        }

        binding.btnGuide.setOnClickListener {
            GuideDialogFragment().show(supportFragmentManager, "guide")
        }

        binding.btnGrantOverlay.setOnClickListener {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")))
        }

        binding.btnGrantUsageStats.setOnClickListener {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        binding.btnGrantNotifications.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        binding.btnExportBackup.setOnClickListener {
            exportBackup.launch("brainwall-respaldo.json")
        }

        binding.btnImportBackup.setOnClickListener {
            importBackup.launch(arrayOf("application/json", "text/plain"))
        }

        binding.btnChangePin.setOnClickListener {
            if (PinManager.hasPin(this)) {
                // Ya tiene PIN: pedir el actual primero
                PinDialogFragment.newInstanceWithMode(PinDialogFragment.MODE_CHANGE)
                    .show(supportFragmentManager, "pin_change")
            } else {
                // No tiene PIN: crear directamente
                PinDialogFragment.newInstanceWithMode(PinDialogFragment.MODE_SETUP)
                    .show(supportFragmentManager, "pin_setup")
            }
        }

        updatePermissions()
    }

    private fun setupMicroInteractions() {
        listOf(
            binding.btnBack,
            binding.btnLevelTest,
            binding.btnGuide,
            binding.btnGrantOverlay,
            binding.btnGrantUsageStats,
            binding.btnGrantNotifications,
            binding.btnChangePin,
            binding.btnExportBackup,
            binding.btnImportBackup
        ).forEach { it.applyPressFeedback() }
    }

    override fun onResume() {
        super.onResume()
        updatePermissions()
    }

    private fun updatePermissions() {
        binding.btnChangePin.text = if (PinManager.hasPin(this)) "Cambiar PIN" else "Agregar PIN"

        val overlayGranted = Settings.canDrawOverlays(this)
        binding.tvOverlayStatus.text = if (overlayGranted)
            "Superposición activa" else "Superposición pendiente"
        binding.tvOverlayStatus.setTextColor(statusColor(overlayGranted))
        binding.btnGrantOverlay.isEnabled = !overlayGranted
        binding.btnGrantOverlay.text = if (overlayGranted) "Configurado" else "Conceder superposición"

        val usageGranted = hasUsageStatsPermission()
        binding.tvUsageStatsStatus.text = if (usageGranted)
            "Estadísticas de uso activas" else "Estadísticas de uso pendientes"
        binding.tvUsageStatsStatus.setTextColor(statusColor(usageGranted))
        binding.btnGrantUsageStats.isEnabled = !usageGranted
        binding.btnGrantUsageStats.text = if (usageGranted) "Configurado" else "Conceder acceso de uso"

        val notificationsGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        binding.tvNotificationsStatus.text = if (notificationsGranted)
            "Notificaciones activas" else "Notificaciones pendientes"
        binding.tvNotificationsStatus.setTextColor(statusColor(notificationsGranted))
        binding.btnGrantNotifications.isEnabled = !notificationsGranted
        binding.btnGrantNotifications.text = if (notificationsGranted) "Configurado" else "Conceder notificaciones"
    }

    private fun statusColor(isReady: Boolean): Int {
        return getColor(if (isReady) R.color.correct_green else R.color.warning_amber)
    }

    private fun hasUsageStatsPermission(): Boolean {
        val aom = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        return aom.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(), packageName) == AppOpsManager.MODE_ALLOWED
    }

}
