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
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        acquireWakeLock()
        observeSipManager()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        if (action == ACTION_STOP) {
            Log.d(TAG, "Stopping VerimorSipService as requested")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        // Start as Foreground Service
        val initialNotification = buildServiceNotification("Verimor SIP: Başlatılıyor...")
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
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "VerimorSip::BackgroundWakeLock").apply {
                setReferenceCounted(false)
                acquire(10 * 60 * 1000L) // Safe 10 min chunks or continuously refreshed
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

        // Observe connection state to update ongoing service notification
        serviceScope.launch {
            sipManager.connectionState.collectLatest { state ->
                val statusText = when (state) {
                    is VerimorSipState.Registered -> "✓ Dahili ${state.extension} Dinleniyor (${state.domain})"
                    is VerimorSipState.Connecting -> "⏳ Santrale bağlanıyor..."
                    is VerimorSipState.Ringing -> "🔔 Çağrı Geliyor: ${state.callerNumber}"
                    is VerimorSipState.InCall -> "📞 Görüşmede: ${state.callerNumber}"
                    is VerimorSipState.Error -> "⚠ Bağlantı Hatası: ${state.message}"
                    is VerimorSipState.Disconnected -> "SIP Bağlantısı Pasif"
                }

                val notification = buildServiceNotification(statusText)
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager?.notify(NOTIFICATION_ID_SERVICE, notification)
            }
        }

        // Observe incoming call events to issue high-priority heads-up notification
        serviceScope.launch {
            sipManager.callEvents.collectLatest { event ->
                showIncomingCallNotification(event)
            }
        }
    }

    private fun showIncomingCallNotification(event: VerimorCallEvent) {
        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            appIntent,
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
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setShowWhen(false)
            .build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java) ?: return

            // Service status channel (silent)
            val serviceChannel = NotificationChannel(
                CHANNEL_ID_SERVICE,
                "Verimor Santral Servisi",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Verimor SIP arka plan servis bildirimleri"
                setShowBadge(false)
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
