package com.example.calculaparajugar.service

import android.app.AlarmManager
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.calculaparajugar.R
import com.example.calculaparajugar.ui.LessonActivity
import com.example.calculaparajugar.ui.MainActivity
import com.example.calculaparajugar.ui.QuestionActivity
import com.example.calculaparajugar.util.AppConfig
import com.example.calculaparajugar.util.LessonManager

/**
 * Servicio de monitoreo BrainWall.
 *
 * Usa AlarmManager para disparar preguntas periódicas de forma confiable.
 * El alarm envía un intent al propio servicio via onStartCommand.
 */
class MonitorService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var pollRunnable: Runnable
    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    hasObservedDeviceState = true
                    deviceWasReady = false
                    onDeviceLocked()
                }
                Intent.ACTION_USER_PRESENT -> {
                    hasObservedDeviceState = true
                    deviceWasReady = true
                    onDeviceUnlocked()
                }
            }
        }
    }

    companion object {
        private const val TAG = "BrainWall"
        private const val CHANNEL_ID = "brainwall_monitor"
        private const val NOTIFICATION_ID = 1
        private const val POLL_INTERVAL_MS = 2000L
        private const val ALARM_REQUEST_CODE = 9999
        private const val ENTRY_GRACE_MS = 15_000L

        const val ACTION_PERIODIC_TRIGGER = "ACTION_PERIODIC_TRIGGER"

        @Volatile
        var isQuestionVisible = false

        @Volatile
        private var hasPendingChallenge = false

        @Volatile
        private var pendingBlockedApp: String? = null

        fun markChallengeResolved() {
            isQuestionVisible = false
            hasPendingChallenge = false
            pendingBlockedApp = null
        }
    }

    private var trackedBlockedApp: String? = null
    private var hasObservedDeviceState = false
    private var deviceWasReady = false
    private var deferChallengesUntil = 0L
    private var entryGraceUntil = 0L
    private var challengeShownForCurrentApp = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        ContextCompat.registerReceiver(
            this,
            screenStateReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        startPolling()
        Log.d(TAG, "=== Servicio CREADO ===")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Verificar si es un trigger periódico del AlarmManager
        if (intent?.action == ACTION_PERIODIC_TRIGGER) {
            Log.d(TAG, "⏰ ALARM TRIGGER recibido en onStartCommand")
            handlePeriodicTrigger()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(pollRunnable)
        runCatching { unregisterReceiver(screenStateReceiver) }
        cancelAlarm()
        Log.d(TAG, "=== Servicio DESTRUIDO ===")
    }

    // ═══════════════════════════════════════════
    // POLLING — detecta cuándo entra a app bloqueada
    // ═══════════════════════════════════════════

    private fun startPolling() {
        pollRunnable = object : Runnable {
            override fun run() {
                try {
                    pollCheck()
                } catch (e: Exception) {
                    Log.e(TAG, "Poll error: ${e.message}")
                }
                handler.postDelayed(this, POLL_INTERVAL_MS)
            }
        }
        handler.post(pollRunnable)
    }

    private fun pollCheck() {
        if (AppConfig.isPaused(this)) {
            trackedBlockedApp = null
            challengeShownForCurrentApp = false
            cancelAlarm()
            return
        }
        val deviceReady = canShowChallengeNow()
        if (!hasObservedDeviceState) {
            hasObservedDeviceState = true
            deviceWasReady = deviceReady
        }

        if (!deviceReady) {
            if (deviceWasReady) onDeviceLocked()
            deviceWasReady = false
            return
        }

        if (!deviceWasReady) {
            deviceWasReady = true
            onDeviceUnlocked()
            return
        }

        if (isQuestionVisible) return

        val foreground = getLastNonSelfApp() ?: return
        val blockedApps = AppConfig.getBlockedApps(this)
        val monitorAllApps = AppConfig.isMonitorAllAppsEnabled(this)
        val shouldMonitor = monitorAllApps || foreground in blockedApps

        if (shouldMonitor) {
            if (hasPendingChallenge && !isQuestionVisible) {
                Log.d(TAG, "Reabriendo desafio pendiente")
                showQuestion(pendingBlockedApp ?: foreground)
                return
            }

            if (foreground != trackedBlockedApp) {
                // Entró a una nueva app bloqueada
                trackedBlockedApp = foreground
                challengeShownForCurrentApp = false
                entryGraceUntil = SystemClock.elapsedRealtime() + ENTRY_GRACE_MS
                if (SystemClock.elapsedRealtime() < deferChallengesUntil) {
                    Log.d(TAG, "App monitoreada restaurada tras desbloqueo; reiniciando intervalo")
                    scheduleNextAlarm()
                    return
                }

                Log.d(TAG, "Nueva app monitoreada: $foreground — aplicando período de gracia")
                return
            }

            if (!challengeShownForCurrentApp && SystemClock.elapsedRealtime() >= entryGraceUntil) {
                Log.d(TAG, "Período de gracia finalizado: mostrando pregunta")
                showQuestion(foreground)
                scheduleNextAlarm()
                challengeShownForCurrentApp = true
            }
            // Si ya es la misma app tracked, la alarma se encarga del periódico
        } else {
            if (trackedBlockedApp != null) {
                Log.d(TAG, "◀◀◀ Salió de app bloqueada. Ahora: $foreground")
                trackedBlockedApp = null
                challengeShownForCurrentApp = false
                cancelAlarm()
            }
        }
    }

    // ═══════════════════════════════════════════
    // ALARMA — pregunta periódica garantizada
    // ═══════════════════════════════════════════

    private fun scheduleNextAlarm() {
        val intervalMs = AppConfig.getIntervalMinutes(this) * 60_000L
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(this, MonitorService::class.java).apply {
            action = ACTION_PERIODIC_TRIGGER
        }
        val pendingIntent = PendingIntent.getService(
            this, ALARM_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAt = SystemClock.elapsedRealtime() + intervalMs

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            triggerAt,
            pendingIntent
        )
        Log.d(TAG, "⏰ Próxima pregunta programada en ${intervalMs / 1000} segundos")
    }

    private fun cancelAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, MonitorService::class.java).apply {
            action = ACTION_PERIODIC_TRIGGER
        }
        val pendingIntent = PendingIntent.getService(
            this, ALARM_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "⏰ Alarma cancelada")
    }

    private fun onDeviceLocked() {
        cancelAlarm()
        hasPendingChallenge = false
        pendingBlockedApp = null
        Log.d(TAG, "Pantalla bloqueada: se descartan desafios pendientes")
    }

    private fun onDeviceUnlocked() {
        val intervalMs = AppConfig.getIntervalMinutes(this) * 60_000L
        deferChallengesUntil = SystemClock.elapsedRealtime() + intervalMs
        cancelAlarm()
        if (trackedBlockedApp != null) scheduleNextAlarm()
        Log.d(TAG, "Dispositivo desbloqueado: intervalo reiniciado desde cero")
    }

    private fun handlePeriodicTrigger() {
        if (AppConfig.isPaused(this)) {
            cancelAlarm()
            return
        }
        if (!canShowChallengeNow()) {
            Log.d(TAG, "Alarma pospuesta: pantalla apagada o dispositivo bloqueado")
            scheduleNextAlarm()
            return
        }

        if (isQuestionVisible) {
            Log.d(TAG, "Pregunta ya visible, reprogramando...")
            scheduleNextAlarm()
            return
        }

        // Verificar que sigue en app bloqueada
        val foreground = getLastNonSelfApp()
        val blockedApps = AppConfig.getBlockedApps(this)
        val monitorAllApps = AppConfig.isMonitorAllAppsEnabled(this)

        if (foreground != null && (monitorAllApps || foreground in blockedApps)) {
            Log.d(TAG, ">>> PREGUNTA PERIÓDICA para: $foreground")
            showQuestion(foreground)
            // Programar la siguiente
            scheduleNextAlarm()
        } else {
            Log.d(TAG, "Alarma disparó pero ya no está en app bloqueada (foreground: $foreground)")
            trackedBlockedApp = null
        }
    }

    // ═══════════════════════════════════════════
    // DETECCIÓN
    // ═══════════════════════════════════════════

    private fun getLastNonSelfApp(): String? {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()

        // Buscar en último minuto
        val usageEvents = usageStatsManager.queryEvents(now - 60_000, now)

        var lastPackage: String? = null
        var lastTimestamp: Long = 0

        val event = UsageEvents.Event()
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                if (event.packageName != packageName) {
                    if (event.timeStamp >= lastTimestamp) {
                        lastPackage = event.packageName
                        lastTimestamp = event.timeStamp
                    }
                }
            }
        }

        return lastPackage
    }

    private fun canShowChallengeNow(): Boolean {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return powerManager.isInteractive && !keyguardManager.isDeviceLocked
    }

    // ═══════════════════════════════════════════
    // MOSTRAR PREGUNTA
    // ═══════════════════════════════════════════

    private fun showQuestion(blockedPackage: String) {
        if (!canShowChallengeNow()) {
            Log.d(TAG, "Desafio omitido: el dispositivo no esta desbloqueado")
            return
        }

        isQuestionVisible = true
        hasPendingChallenge = true
        pendingBlockedApp = blockedPackage

        // Decidir si mostrar LECCIÓN o PREGUNTA basado en el estado del LessonManager
        val categories = AppConfig.getEnabledCategories(this)
        val category = categories.random()
        val mode = LessonManager.getNextShowMode(this, category)

        val intent = if (mode == LessonManager.ShowMode.LESSON) {
            Log.d(TAG, "Mostrando LECCIÓN de ${category.name}")
            Intent(this, LessonActivity::class.java)
        } else {
            Log.d(TAG, "Mostrando PREGUNTA de ${category.name}")
            Intent(this, QuestionActivity::class.java)
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.putExtra("blocked_package", blockedPackage)
        intent.putExtra("category", category.name)
        startActivity(intent)
    }

    // ═══════════════════════════════════════════
    // NOTIFICACIÓN
    // ═══════════════════════════════════════════

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "BrainWall Monitor",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Monitorea el uso de aplicaciones"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BrainWall")
            .setContentText("Protección activa")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
