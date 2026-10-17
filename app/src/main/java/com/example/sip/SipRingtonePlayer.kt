package com.example.sip

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Gelen SIP Çağrılarında Telefonun Kendi Zil Sesini Sürekli (Looping) Çalma Motoru.
 * Çağrı geldiğinde başlar; Cevapla, Reddet veya Sonlandır dendiğinde anında ve tamamen susar.
 * RingtoneManager, MediaPlayer ve emülatörler için ToneGenerator çoklu katmanlı yedekleme içerir.
 */
class SipRingtonePlayer(private val context: Context) {

    companion object {
        private const val TAG = "SipRingtonePlayer"
    }

    private val playerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var mediaPlayer: MediaPlayer? = null
    private var ringtone: Ringtone? = null
    private var toneGeneratorJob: Job? = null
    private var toneGen: ToneGenerator? = null
    private var vibrator: Vibrator? = null
    private val playerLock = Any()
    @Volatile private var isPlaying = false

    init {
        try {
            vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } catch (_: Exception) {}
    }

    /**
     * Sürekli zil sesi ve titreşimi başlatır.
     */
    fun start() {
        synchronized(playerLock) {
            if (isPlaying) {
                Log.d(TAG, "Ringtone is already playing.")
                return
            }
            isPlaying = true
            Log.i(TAG, "Starting ringtone playback...")

            var startedSuccessfully = false

            // 1. Android RingtoneManager ile Sistem Zil Sesini Çalmayı Dene
            try {
                val ringtoneUri: Uri = RingtoneManager.getActualDefaultRingtoneUri(
                    context,
                    RingtoneManager.TYPE_RINGTONE
                ) ?: Settings.System.DEFAULT_RINGTONE_URI
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

                val r = RingtoneManager.getRingtone(context, ringtoneUri)
                if (r != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        r.isLooping = true
                    }
                    r.audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    r.play()
                    ringtone = r
                    startedSuccessfully = true
                    Log.i(TAG, "✓ System Ringtone started via RingtoneManager.")
                }
            } catch (e: Exception) {
                Log.w(TAG, "RingtoneManager play failed: ${e.message}")
            }

            // 2. Ringtone başarısız olduysa MediaPlayer ile dene
            if (!startedSuccessfully) {
                try {
                    val ringtoneUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                        ?: Settings.System.DEFAULT_RINGTONE_URI

                    val player = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                        )
                        setDataSource(context, ringtoneUri)
                        isLooping = true
                        prepare()
                        start()
                    }
                    mediaPlayer = player
                    startedSuccessfully = true
                    Log.i(TAG, "✓ Ringtone started via MediaPlayer.")
                } catch (e: Exception) {
                    Log.w(TAG, "MediaPlayer play failed: ${e.message}")
                }
            }

            // 3. Emülatör veya ses dosyası bulunamayan cihazlar için ToneGenerator Çalma Yedeklemesi
            if (!startedSuccessfully) {
                startToneGeneratorRing()
            }

            // Titreşimi başlat
            startVibration()
        }
    }

    private fun startToneGeneratorRing() {
        toneGeneratorJob?.cancel()
        toneGeneratorJob = playerScope.launch {
            try {
                val tg = ToneGenerator(AudioManager.STREAM_RING, 100)
                toneGen = tg
                Log.i(TAG, "✓ Ringing tone fallback active via ToneGenerator.")
                while (isActive && isPlaying) {
                    tg.startTone(ToneGenerator.TONE_SUP_RINGTONE, 1200)
                    delay(2500L)
                }
            } catch (e: Exception) {
                Log.w(TAG, "ToneGenerator error: ${e.message}")
            }
        }
    }

    /**
     * Zil sesini, ton jeneratörünü ve titreşimi anında ve tamamen durdurur.
     */
    fun stop() {
        synchronized(playerLock) {
            isPlaying = false
            Log.i(TAG, "Stopping all ringtone playback immediately...")

            // 1. Ringtone durdur
            try {
                ringtone?.let {
                    if (it.isPlaying) {
                        it.stop()
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping Ringtone: ${e.message}")
            } finally {
                ringtone = null
            }

            // 2. MediaPlayer durdur
            try {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        it.stop()
                    }
                    it.reset()
                    it.release()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping MediaPlayer: ${e.message}")
            } finally {
                mediaPlayer = null
            }

            // 3. Tone Generator durdur
            toneGeneratorJob?.cancel()
            toneGeneratorJob = null
            try {
                toneGen?.stopTone()
                toneGen?.release()
            } catch (_: Exception) {}
            toneGen = null

            // 4. Titreşimi durdur
            stopVibration()

            Log.i(TAG, "✓ Sip Ringtone stopped completely.")
        }
    }

    private fun startVibration() {
        try {
            val v = vibrator ?: return
            if (!v.hasVibrator()) return

            val pattern = longArrayOf(0, 1000, 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.d(TAG, "Vibration start error: ${e.message}")
        }
    }

    private fun stopVibration() {
        try {
            vibrator?.cancel()
        } catch (_: Exception) {}
    }
}
