package com.example.util

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AudioRecordHelper {
    private const val TAG = "AudioRecordHelper"
    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var mediaPlayer: MediaPlayer? = null

    var isRecording: Boolean = false
        private set

    var isPlaying: Boolean = false
        private set

    /**
     * Yeni bir çağrı ses kaydı başlatır
     */
    fun startRecording(context: Context): File? {
        if (isRecording) return currentOutputFile
        return try {
            val audioDir = File(context.filesDir, "call_recordings").apply { mkdirs() }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(audioDir, "SES_$timeStamp.m4a")

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            currentOutputFile = file
            isRecording = true
            file
        } catch (e: Exception) {
            Log.e(TAG, "Ses kaydı başlatılamadı: ${e.message}")
            isRecording = false
            currentOutputFile = null
            null
        }
    }

    /**
     * Devam eden ses kaydını durdurur ve kaydedilen dosyayı döner
     */
    fun stopRecording(): File? {
        if (!isRecording) return null
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            currentOutputFile
        } catch (e: Exception) {
            Log.e(TAG, "Ses kaydı durdurulamadı: ${e.message}")
            isRecording = false
            mediaRecorder = null
            null
        }
    }

    /**
     * Belirtilen ses dosyasını oynatır
     */
    fun playAudio(filePath: String, onCompletion: () -> Unit, onError: () -> Unit = {}) {
        stopAudio()
        try {
            val file = File(filePath)
            if (!file.exists()) {
                onError()
                return
            }
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener {
                    this@AudioRecordHelper.isPlaying = false
                    onCompletion()
                }
                setOnErrorListener { _, _, _ ->
                    this@AudioRecordHelper.isPlaying = false
                    onError()
                    true
                }
                start()
            }
            this@AudioRecordHelper.isPlaying = true
        } catch (e: Exception) {
            Log.e(TAG, "Ses çalınamadı: ${e.message}")
            this@AudioRecordHelper.isPlaying = false
            onError()
        }
    }

    /**
     * Oynatılan sesi durdurur
     */
    fun stopAudio() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            mediaPlayer = null
            isPlaying = false
        } catch (e: Exception) {
            Log.e(TAG, "Ses durdurulamadı: ${e.message}")
        }
    }
}
