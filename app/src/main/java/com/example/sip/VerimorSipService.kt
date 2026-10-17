package com.example.sip

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Android Foreground Service to maintain persistent background SIP listening
 * for Verimor Bulut Santral, keeping UDP sockets alive and handling incoming calls.
 */
class VerimorSipService : Service() {

    companion object {
        private const val TAG = "VerimorSipService"
        const val CHANNEL_ID_SERVICE = "verimor_sip_service_channel"
        const val CHANNEL_ID_CALLS = "verimor_sip_calls_channel"
        const val NOTIFICATION_ID_SERVICE = 9001
        const val NOTIFICATION_ID_INCOMING_CALL = 9002

        const val ACTION_START = "com.example.sip.ACTION_START"
        const val ACTION_STOP = "com.example.sip.ACTION_STOP"

        fun startService(context: Context) {
            val intent = Intent(context, VerimorSipService::class.java).apply {
                action = ACTION_START
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start VerimorSipService: ${e.message}")
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, VerimorSipService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop VerimorSipService: ${e.message}")
            }
        }

        fun requestIgnoreBatteryOptimizations(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                    val pkg = context.packageName
                    if (pm != null && !pm.isIgnoringBatteryOptimizations(pkg)) {
                        val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = android.net.Uri.parse("package:$pkg")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                        Log.i(TAG, "Requested ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS")
                    }
                } catch (e: Exception) {
                    try {
                        val fallbackIntent = Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(fallbackIntent)
                    } catch (err: Exception) {
                        Log.w(TAG, "Battery optimization request failed: ${err.message}")
                    }
                }
            }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        observeSipManager()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        val sipManager = VerimorSipManager.getInstance(applicationContext)
        if (action == ACTION_STOP || !sipManager.isMasterPowerEnabled) {
            Log.d(TAG, "Stopping VerimorSipService as requested or Master Power is OFF")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        // Start as Foreground Service with a completely silent, non-intrusive status notification
        val initialNotification = buildServiceNotification("Arka plan servisi hazır")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID_SERVICE,
                    initialNotification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                )
            } else {
                startForeground(NOTIFICATION_ID_SERVICE, initialNotification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "startForeground error", e)
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        releaseWakeLock()
        Log.d(TAG, "VerimorSipService destroyed")
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (wakeLock == null) {
                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "VerimorSip::BackgroundWakeLock").apply {
                    setReferenceCounted(false)
                }
            }
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire()
                Log.d(TAG, "Acquired continuous PARTIAL_WAKE_LOCK for Verimor SIP")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to acquire wake lock: ${e.message}")
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
        wakeLock = null
    }

    private fun observeSipManager() {
        val sipManager = VerimorSipManager.getInstance(applicationContext)

        // Observe connection state to update ongoing service notification silently
        serviceScope.launch {
            sipManager.connectionState.collectLatest { state ->
                val statusText = when (state) {
                    is VerimorSipState.Registered -> "Dahili ${state.extension} hazır"
                    is VerimorSipState.Connecting -> "Bağlantı kuruluyor"
                    is VerimorSipState.Ringing -> "Gelen Çağrı: ${state.callerNumber}"
                    is VerimorSipState.InCall -> "Görüşmede: ${state.callerNumber}"
                    is VerimorSipState.Error -> "⚠ Bağlantı Hatası: ${state.message}"
                    is VerimorSipState.Disconnected -> "Pasif"
                }

                // Dynamic WakeLock: Sadece çağrı çalarken veya görüşmedeyken işlemciyi uyanık tut,
                // boştayken cihazın derin uykuya (Doze mode) geçmesine izin vererek pili koru
                if (state is VerimorSipState.Ringing || state is VerimorSipState.InCall) {
                    acquireWakeLock()
                } else {
                    releaseWakeLock()
                }

                val notification = buildServiceNotification(statusText)
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager?.notify(NOTIFICATION_ID_SERVICE, notification)

                // Çağrı sonlandıysa gelen çağrı bildirimini kaldır
                if (state !is VerimorSipState.Ringing) {
                    notificationManager?.cancel(NOTIFICATION_ID_INCOMING_CALL)
                }
            }
        }

        // Observe incoming call events to launch full-screen call screen and issue high-priority heads-up notification
        serviceScope.launch {
            sipManager.callEvents.collectLatest { event ->
                try {
                    val fullScreenIntent = Intent(applicationContext, IncomingCallActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    startActivity(fullScreenIntent)
                } catch (e: Exception) {
                    Log.w(TAG, "Could not start IncomingCallActivity directly: ${e.message}")
                }

                showIncomingCallNotification(event)
            }
        }
    }

    private fun showIncomingCallNotification(event: VerimorCallEvent) {
        val callIntent = Intent(this, IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            callIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val callerTitle = if (event.callerName.isNotBlank()) {
            "${event.callerName} (${event.callerNumber})"
        } else {
            event.callerNumber
        }

        val callNotification = NotificationCompat.Builder(this, CHANNEL_ID_CALLS)
            .setSmallIcon(android.R.drawable.sym_call_incoming)
            .setContentTitle("📞 Gelen Çağrı (Verimor Santral)")
            .setContentText(callerTitle)
            .setSubText("Destek Çağrısı")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            .setDefaults(Notification.DEFAULT_ALL)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.notify(NOTIFICATION_ID_INCOMING_CALL, callNotification)
    }

    private fun buildServiceNotification(contentText: String): Notification {
        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID_SERVICE)
            .setSmallIcon(android.R.drawable.stat_sys_phone_call)
            .setContentTitle("Verimor Bulut Santral")
            .setContentText(contentText)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(pendingIntent)
            .setShowWhen(false)
            .build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java) ?: return

            // Service status channel (completely silent, no vibration, no sound)
            val serviceChannel = NotificationChannel(
                CHANNEL_ID_SERVICE,
                "Verimor Santral Servisi",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Verimor SIP arka plan servis bildirimleri"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(serviceChannel)

            // Incoming calls channel (loud, heads-up)
            val callsChannel = NotificationChannel(
                CHANNEL_ID_CALLS,
                "Verimor Gelen Çağrılar",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Verimor santralinden gelen arama bildirimleri"
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(callsChannel)
        }
    }
}
