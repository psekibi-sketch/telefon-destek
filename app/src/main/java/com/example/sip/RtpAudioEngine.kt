package com.example.sip

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.ToneGenerator
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.Random

/**
 * RTP (Real-Time Transport Protocol, RFC 3550) Çift Yönlü Ses Motoru.
 * Santral ile UDP üzerinden PCMA (G.711 A-law) ve PCMU (µ-law) 8000 Hz 16-bit
 * ses paketlerini alır (AudioTrack) ve mikrofondan gönderir (AudioRecord).
 */
class RtpAudioEngine(private val context: Context) {

    companion object {
        private const val TAG = "RtpAudioEngine"
        private const val SAMPLE_RATE = 8000
        private const val FRAME_SAMPLES = 160 // 20ms @ 8kHz
        private const val RTP_HEADER_SIZE = 12
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var rxJob: Job? = null
    private var txJob: Job? = null

    private var rtpSocket: DatagramSocket? = null
    private var audioTrack: AudioTrack? = null
    private var audioRecord: AudioRecord? = null

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isSpeaker = MutableStateFlow(false)
    val isSpeaker: StateFlow<Boolean> = _isSpeaker.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private var ssrc: Int = Random().nextInt()
    private var sequenceNumber: Int = Random().nextInt(32767)
    private var timestamp: Long = Random().nextInt(65535).toLong()
    private var currentRemoteAddress: InetAddress? = null
    private var currentRemotePort: Int = 0

    /**
     * RTP için yerel UDP soketini açar ve port numarasını döner.
     */
    fun prepare(): Int {
        closeSocket()
        val socket = DatagramSocket()
        socket.reuseAddress = true
        socket.receiveBufferSize = 64 * 1024
        socket.sendBufferSize = 64 * 1024
        rtpSocket = socket
        Log.i(TAG, "Local RTP Socket prepared on port: ${socket.localPort}")
        return socket.localPort
    }

    fun getLocalPort(): Int {
        return rtpSocket?.localPort ?: 10002
    }

    /**
     * RTP Çift Yönlü Ses Akışını Başlatır.
     */
    @SuppressLint("MissingPermission")
    fun start(remoteAddress: InetAddress, remotePort: Int, payloadType: Int = 8) {
        if (_isStreaming.value) {
            Log.w(TAG, "RTP Audio Engine is already streaming.")
            return
        }

        Log.i(TAG, "Starting RTP Audio Stream -> ${remoteAddress.hostAddress}:$remotePort (PT: $payloadType)")
        currentRemoteAddress = remoteAddress
        currentRemotePort = remotePort

        setupAudioHardware()

        // 1. AudioTrack (Gelen Sesi Oynatma) Başlatma
        try {
            val minTrackBufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val trackBufferSize = maxOf(minTrackBufferSize, FRAME_SAMPLES * 2 * 8)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(trackBufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            track.play()
            audioTrack = track
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing AudioTrack: ${e.message}", e)
        }

        // 2. AudioRecord (Mikrofon Kaydı) Başlatma
        try {
            val minRecBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val recBufferSize = maxOf(minRecBufferSize, FRAME_SAMPLES * 2 * 8)

            val record = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                recBufferSize
            )

            if (record.state == AudioRecord.STATE_INITIALIZED) {
                record.startRecording()
                audioRecord = record
            } else {
                Log.w(TAG, "AudioRecord state is not initialized. Check RECORD_AUDIO permission.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing AudioRecord: ${e.message}", e)
        }

        _isStreaming.value = true

        // 3. NAT Delme ve Ön El Sıkışma (Hole Punching / Keep-Alive)
        // Santrale derhal 5 adet sessizlik RTP paketi göndererek mobil/NAT arkasındaki UDP portunu açar
        engineScope.launch(Dispatchers.IO) {
            val socket = rtpSocket ?: return@launch
            val silencePacket = ByteArray(RTP_HEADER_SIZE + FRAME_SAMPLES)
            silencePacket[0] = 0x80.toByte()
            silencePacket[1] = (payloadType and 0x7F).toByte()
            val fillByte = if (payloadType == 0) 0xFF.toByte() else 0xD5.toByte()
            for (i in RTP_HEADER_SIZE until silencePacket.size) {
                silencePacket[i] = fillByte
            }

            repeat(5) {
                try {
                    sequenceNumber = (sequenceNumber + 1) and 0xFFFF
                    silencePacket[2] = ((sequenceNumber shr 8) and 0xFF).toByte()
                    silencePacket[3] = (sequenceNumber and 0xFF).toByte()
                    timestamp = (timestamp + FRAME_SAMPLES) and 0xFFFFFFFFL
                    silencePacket[4] = ((timestamp shr 24) and 0xFF).toByte()
                    silencePacket[5] = ((timestamp shr 16) and 0xFF).toByte()
                    silencePacket[6] = ((timestamp shr 8) and 0xFF).toByte()
                    silencePacket[7] = (timestamp and 0xFF).toByte()
                    silencePacket[8] = ((ssrc shr 24) and 0xFF).toByte()
                    silencePacket[9] = ((ssrc shr 16) and 0xFF).toByte()
                    silencePacket[10] = ((ssrc shr 8) and 0xFF).toByte()
                    silencePacket[11] = (ssrc and 0xFF).toByte()

                    val packet = DatagramPacket(silencePacket, silencePacket.size, remoteAddress, remotePort)
                    socket.send(packet)
                    Log.d(TAG, "Sent RTP NAT Hole-Punch packet #${it + 1} to ${remoteAddress.hostAddress}:$remotePort")
                    kotlinx.coroutines.delay(15L)
                } catch (e: Exception) {
                    Log.w(TAG, "Hole punching packet error: ${e.message}")
                }
            }
        }

        // 4. RX Coroutine (Gelen RTP Paketlerini Dinleme ve Oynatma)
        rxJob = engineScope.launch(Dispatchers.IO) {
            val socket = rtpSocket ?: return@launch
            val rxBuffer = ByteArray(1500)
            val pcmBuffer = ShortArray(FRAME_SAMPLES * 4)

            while (isActive && _isStreaming.value) {
                try {
                    val packet = DatagramPacket(rxBuffer, rxBuffer.size)
                    socket.receive(packet)

                    if (packet.length < RTP_HEADER_SIZE) continue

                    // RTP Header analizi
                    val b0 = rxBuffer[0].toInt() and 0xFF
                    val b1 = rxBuffer[1].toInt() and 0xFF
                    val pt = b1 and 0x7F
                    val cc = b0 and 0x0F
                    val hasExtension = (b0 and 0x10) != 0

                    var headerOffset = RTP_HEADER_SIZE + (cc * 4)
                    if (hasExtension && packet.length >= headerOffset + 4) {
                        val extLength = ((rxBuffer[headerOffset + 2].toInt() and 0xFF) shl 8) or
                                (rxBuffer[headerOffset + 3].toInt() and 0xFF)
                        headerOffset += 4 + (extLength * 4)
                    }

                    val payloadLength = packet.length - headerOffset
                    if (payloadLength > 0 && headerOffset < packet.length) {
                        val samplesDecoded = G711Codec.decodeRtpPayload(
                            payload = rxBuffer,
                            offset = headerOffset,
                            length = payloadLength,
                            payloadType = pt,
                            outputPcm = pcmBuffer
                        )

                        audioTrack?.write(pcmBuffer, 0, samplesDecoded)
                    }
                } catch (e: Exception) {
                    if (isActive && _isStreaming.value) {
                        Log.d(TAG, "RTP RX loop interrupted: ${e.message}")
                    }
                }
            }
        }

        // 5. TX Coroutine (Mikrofondan RTP Paketi Gönderme & Kesintisiz Canlı Tutma)
        txJob = engineScope.launch(Dispatchers.IO) {
            val socket = rtpSocket ?: return@launch
            val micPcmBuffer = ShortArray(FRAME_SAMPLES)
            val txPacketData = ByteArray(RTP_HEADER_SIZE + FRAME_SAMPLES)
            val silenceByte = if (payloadType == 0) 0xFF.toByte() else 0xD5.toByte()

            while (isActive && _isStreaming.value) {
                try {
                    val record = audioRecord
                    var pcmRead = 0
                    if (record != null && record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                        pcmRead = record.read(micPcmBuffer, 0, FRAME_SAMPLES)
                    }

                    if (pcmRead > 0) {
                        if (_isMuted.value) {
                            // Sessize alındıysa sıfır gönder
                            micPcmBuffer.fill(0)
                        }

                        // 12-byte RTP Header'ı oluştur
                        txPacketData[0] = 0x80.toByte() // V=2, P=0, X=0, CC=0
                        txPacketData[1] = (payloadType and 0x7F).toByte()

                        sequenceNumber = (sequenceNumber + 1) and 0xFFFF
                        txPacketData[2] = ((sequenceNumber shr 8) and 0xFF).toByte()
                        txPacketData[3] = (sequenceNumber and 0xFF).toByte()

                        timestamp = (timestamp + FRAME_SAMPLES) and 0xFFFFFFFFL
                        txPacketData[4] = ((timestamp shr 24) and 0xFF).toByte()
                        txPacketData[5] = ((timestamp shr 16) and 0xFF).toByte()
                        txPacketData[6] = ((timestamp shr 8) and 0xFF).toByte()
                        txPacketData[7] = (timestamp and 0xFF).toByte()

                        txPacketData[8] = ((ssrc shr 24) and 0xFF).toByte()
                        txPacketData[9] = ((ssrc shr 16) and 0xFF).toByte()
                        txPacketData[10] = ((ssrc shr 8) and 0xFF).toByte()
                        txPacketData[11] = (ssrc and 0xFF).toByte()

                        // G.711 Payload Kodlama (160 bayt)
                        G711Codec.encodeRtpPayload(
                            inputPcm = micPcmBuffer,
                            pcmOffset = 0,
                            pcmCount = pcmRead,
                            payloadType = payloadType,
                            outputPayload = txPacketData,
                            outputOffset = RTP_HEADER_SIZE
                        )

                        val sendPacket = DatagramPacket(
                            txPacketData,
                            RTP_HEADER_SIZE + pcmRead,
                            remoteAddress,
                            remotePort
                        )
                        socket.send(sendPacket)
                    } else {
                        // Mikrofon henüz hazır değilse veya izin bekleniyorsa santralin ses akışını koparmamak için sessizlik paketi gönder
                        txPacketData[0] = 0x80.toByte()
                        txPacketData[1] = (payloadType and 0x7F).toByte()
                        sequenceNumber = (sequenceNumber + 1) and 0xFFFF
                        txPacketData[2] = ((sequenceNumber shr 8) and 0xFF).toByte()
                        txPacketData[3] = (sequenceNumber and 0xFF).toByte()
                        timestamp = (timestamp + FRAME_SAMPLES) and 0xFFFFFFFFL
                        txPacketData[4] = ((timestamp shr 24) and 0xFF).toByte()
                        txPacketData[5] = ((timestamp shr 16) and 0xFF).toByte()
                        txPacketData[6] = ((timestamp shr 8) and 0xFF).toByte()
                        txPacketData[7] = (timestamp and 0xFF).toByte()
                        txPacketData[8] = ((ssrc shr 24) and 0xFF).toByte()
                        txPacketData[9] = ((ssrc shr 16) and 0xFF).toByte()
                        txPacketData[10] = ((ssrc shr 8) and 0xFF).toByte()
                        txPacketData[11] = (ssrc and 0xFF).toByte()

                        for (i in RTP_HEADER_SIZE until txPacketData.size) {
                            txPacketData[i] = silenceByte
                        }

                        val sendPacket = DatagramPacket(
                            txPacketData,
                            txPacketData.size,
                            remoteAddress,
                            remotePort
                        )
                        socket.send(sendPacket)
                        kotlinx.coroutines.delay(20L)
                    }
                } catch (e: Exception) {
                    if (isActive && _isStreaming.value) {
                        Log.d(TAG, "RTP TX loop exception: ${e.message}")
                    }
                    kotlinx.coroutines.delay(20L)
                }
            }
        }
    }

    /**
     * RTP Ses Akışını ve Donanımı Sonlandırır.
     */
    fun stop() {
        Log.i(TAG, "Stopping RTP Audio Engine...")
        _isStreaming.value = false

        rxJob?.cancel()
        txJob?.cancel()
        rxJob = null
        txJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null

        try {
            audioTrack?.stop()
            audioTrack?.flush()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null

        closeSocket()
        currentRemoteAddress = null
        currentRemotePort = 0
        restoreAudioHardware()
    }

    /**
     * RFC 2833 / RFC 4733 Telephony Event DTMF Tonu Gönderimi (PT: 101)
     */
    fun sendDtmf(digit: Char) {
        val eventCode = when (digit) {
            in '0'..'9' -> digit - '0'
            '*' -> 10
            '#' -> 11
            'A', 'a' -> 12
            'B', 'b' -> 13
            'C', 'c' -> 14
            'D', 'd' -> 15
            else -> return
        }

        // Yerel ses geri bildirimi (Kullanıcı arayüzünde tuş sesi)
        try {
            val toneType = when (digit) {
                '0' -> ToneGenerator.TONE_DTMF_0
                '1' -> ToneGenerator.TONE_DTMF_1
                '2' -> ToneGenerator.TONE_DTMF_2
                '3' -> ToneGenerator.TONE_DTMF_3
                '4' -> ToneGenerator.TONE_DTMF_4
                '5' -> ToneGenerator.TONE_DTMF_5
                '6' -> ToneGenerator.TONE_DTMF_6
                '7' -> ToneGenerator.TONE_DTMF_7
                '8' -> ToneGenerator.TONE_DTMF_8
                '9' -> ToneGenerator.TONE_DTMF_9
                '*' -> ToneGenerator.TONE_DTMF_S
                '#' -> ToneGenerator.TONE_DTMF_P
                'A', 'a' -> ToneGenerator.TONE_DTMF_A
                'B', 'b' -> ToneGenerator.TONE_DTMF_B
                'C', 'c' -> ToneGenerator.TONE_DTMF_C
                'D', 'd' -> ToneGenerator.TONE_DTMF_D
                else -> ToneGenerator.TONE_PROP_BEEP
            }
            val tg = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 80)
            tg.startTone(toneType, 160)
        } catch (_: Exception) {}

        val socket = rtpSocket ?: return
        val addr = currentRemoteAddress ?: return
        val port = currentRemotePort
        if (port <= 0) return

        engineScope.launch(Dispatchers.IO) {
            try {
                val startTs = timestamp
                val dtmfPacket = ByteArray(RTP_HEADER_SIZE + 4)

                // Packet 1-3 (Tone started / in progress)
                for (step in 1..3) {
                    val duration = step * 160
                    dtmfPacket[0] = 0x80.toByte() // V=2
                    dtmfPacket[1] = if (step == 1) (0x80 or 101).toByte() else 101.toByte() // Marker on first packet, PT 101
                    sequenceNumber = (sequenceNumber + 1) and 0xFFFF
                    dtmfPacket[2] = ((sequenceNumber shr 8) and 0xFF).toByte()
                    dtmfPacket[3] = (sequenceNumber and 0xFF).toByte()

                    dtmfPacket[4] = ((startTs shr 24) and 0xFF).toByte()
                    dtmfPacket[5] = ((startTs shr 16) and 0xFF).toByte()
                    dtmfPacket[6] = ((startTs shr 8) and 0xFF).toByte()
                    dtmfPacket[7] = (startTs and 0xFF).toByte()

                    dtmfPacket[8] = ((ssrc shr 24) and 0xFF).toByte()
                    dtmfPacket[9] = ((ssrc shr 16) and 0xFF).toByte()
                    dtmfPacket[10] = ((ssrc shr 8) and 0xFF).toByte()
                    dtmfPacket[11] = (ssrc and 0xFF).toByte()

                    // RFC 2833 Payload: Event, E/R/Volume, Duration
                    dtmfPacket[12] = eventCode.toByte()
                    dtmfPacket[13] = 10.toByte() // E=0, Volume=10
                    dtmfPacket[14] = ((duration shr 8) and 0xFF).toByte()
                    dtmfPacket[15] = (duration and 0xFF).toByte()

                    socket.send(DatagramPacket(dtmfPacket, dtmfPacket.size, addr, port))
                    kotlinx.coroutines.delay(20L)
                }

                // Packet 4-6 (Tone End packets)
                for (rep in 1..3) {
                    val duration = 640
                    dtmfPacket[0] = 0x80.toByte()
                    dtmfPacket[1] = 101.toByte()
                    sequenceNumber = (sequenceNumber + 1) and 0xFFFF
                    dtmfPacket[2] = ((sequenceNumber shr 8) and 0xFF).toByte()
                    dtmfPacket[3] = (sequenceNumber and 0xFF).toByte()

                    dtmfPacket[4] = ((startTs shr 24) and 0xFF).toByte()
                    dtmfPacket[5] = ((startTs shr 16) and 0xFF).toByte()
                    dtmfPacket[6] = ((startTs shr 8) and 0xFF).toByte()
                    dtmfPacket[7] = (startTs and 0xFF).toByte()

                    dtmfPacket[8] = ((ssrc shr 24) and 0xFF).toByte()
                    dtmfPacket[9] = ((ssrc shr 16) and 0xFF).toByte()
                    dtmfPacket[10] = ((ssrc shr 8) and 0xFF).toByte()
                    dtmfPacket[11] = (ssrc and 0xFF).toByte()

                    // RFC 2833 End Payload: E=1, Volume=10
                    dtmfPacket[12] = eventCode.toByte()
                    dtmfPacket[13] = (0x80 or 10).toByte() // E=1
                    dtmfPacket[14] = ((duration shr 8) and 0xFF).toByte()
                    dtmfPacket[15] = (duration and 0xFF).toByte()

                    socket.send(DatagramPacket(dtmfPacket, dtmfPacket.size, addr, port))
                    kotlinx.coroutines.delay(20L)
                }
                Log.i(TAG, "RFC 2833 DTMF sent: '$digit' (event $eventCode) to ${addr.hostAddress}:$port")
            } catch (e: Exception) {
                Log.w(TAG, "Error sending RFC 2833 DTMF: ${e.message}")
            }
        }
    }

    fun toggleMute(): Boolean {
        _isMuted.value = !_isMuted.value
        Log.i(TAG, "Microphone Muted: ${_isMuted.value}")
        return _isMuted.value
    }

    fun setMute(mute: Boolean) {
        _isMuted.value = mute
    }

    fun toggleSpeaker(): Boolean {
        val newState = !_isSpeaker.value
        setSpeaker(newState)
        return newState
    }

    fun setSpeaker(enable: Boolean) {
        _isSpeaker.value = enable
        try {
            audioManager?.isSpeakerphoneOn = enable
            Log.i(TAG, "Speakerphone set to: $enable")
        } catch (e: Exception) {
            Log.w(TAG, "Could not set speakerphone: ${e.message}")
        }
    }

    private fun setupAudioHardware() {
        try {
            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager?.isSpeakerphoneOn = _isSpeaker.value
        } catch (e: Exception) {
            Log.w(TAG, "Audio hardware mode setup error: ${e.message}")
        }
    }

    private fun restoreAudioHardware() {
        try {
            audioManager?.isSpeakerphoneOn = false
            audioManager?.mode = AudioManager.MODE_NORMAL
        } catch (_: Exception) {}
    }

    private fun closeSocket() {
        try {
            rtpSocket?.close()
        } catch (_: Exception) {}
        rtpSocket = null
    }

    fun release() {
        stop()
        engineScope.cancel()
    }
}
