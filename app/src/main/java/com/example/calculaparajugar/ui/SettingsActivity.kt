package com.example.calculaparajugar.ui

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.example.calculaparajugar.R
import com.example.calculaparajugar.databinding.ActivitySettingsBinding
import com.example.calculaparajugar.util.PinManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

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
            binding.btnChangePin
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

        val usageGranted = hasUsageStatsPermission()
        binding.tvUsageStatsStatus.text = if (usageGranted)
            "Estadísticas de uso activas" else "Estadísticas de uso pendientes"
        binding.tvUsageStatsStatus.setTextColor(statusColor(usageGranted))

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
