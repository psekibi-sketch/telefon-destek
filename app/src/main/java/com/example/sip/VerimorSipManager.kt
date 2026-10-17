package com.example.sip

import android.content.Context
import android.util.Log
import com.example.model.MissedCallRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.NetworkInterface
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/**
 * Verimor Bulut Santral SIP Konfigürasyonu
 */
data class VerimorSipConfig(
    val domain: String = "sirketiniz.bulutsantralim.com",
    val extension: String = "101",
    val password: String = "",
    val port: Int = 5060,
    val autoStartTimer: Boolean = true,
    val autoSearchCustomer: Boolean = true
)

/**
 * SIP Bağlantı Durumu
 */
sealed class VerimorSipState {
    object Disconnected : VerimorSipState()
    object Connecting : VerimorSipState()
    data class Registered(val extension: String, val domain: String) : VerimorSipState()
    data class Ringing(val callerNumber: String, val callerName: String) : VerimorSipState()
    data class InCall(val callerNumber: String, val durationSeconds: Long = 0) : VerimorSipState()
    data class Error(val message: String) : VerimorSipState()
}

/**
 * Gelen Çağrı Olayı
 */
data class VerimorCallEvent(
    val callerNumber: String,
    val callerName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSimulated: Boolean = false
)

/**
 * Aktif SIP Çağrı Oturumu ve SDP Medya Bilgileri
 */
data class ActiveCallDialog(
    val callerNumber: String,
    val callerName: String,
    val callId: String,
    val from: String,
    val to: String,
    val via: String,
    val viaHeaders: List<String> = emptyList(),
    val recordRouteHeaders: List<String> = emptyList(),
    val cseq: String,
    val contact: String?,
    val serverAddress: InetAddress,
    val serverPort: Int,
    val remoteRtpAddress: InetAddress,
    val remoteRtpPort: Int,
    val preferredCodec: Int = 8, // PCMA A-law
    val localToTag: String = "vm" + System.currentTimeMillis(),
    val isSimulated: Boolean = false
)

/**
 * Verimor Bulut Santral SIP İletişim Motoru
 * RFC 3261 SIP Kayıt, RFC 2617 MD5 Digest Authentication (qop destekli),
 * UDP Soket Dinleme, Gelen INVITE Yakalama, SDP Müzakeresi ve RTP Çift Yönlü Ses Akışı.
 */
class VerimorSipManager private constructor(private val appContext: Context) {

    companion object {
        private const val TAG = "VerimorSipManager"
        private const val REGISTRATION_TIMEOUT_MS = 15000L // 15 saniye zaman aşımı
        private const val AUTH_TIMEOUT_MS = 15000L         // 15 saniye kimlik doğrulama zaman aşımı
        private const val KEEP_ALIVE_INTERVAL_MS = 30000L  // 30 saniyede bir can suyu (keep-alive)

        @Volatile
        private var INSTANCE: VerimorSipManager? = null

        fun getInstance(context: Context): VerimorSipManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: VerimorSipManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Çift Yönlü RTP Ses Motoru (AudioTrack & AudioRecord & G.711)
    val rtpAudioEngine = RtpAudioEngine(appContext)

    // Gelen Arama Sürekli Zil Sesi ve Titreşim Yöneticisi
    val ringtonePlayer = SipRingtonePlayer(appContext)

    private val _connectionState = MutableStateFlow<VerimorSipState>(VerimorSipState.Disconnected)
    val connectionState: StateFlow<VerimorSipState> = _connectionState.asStateFlow()

    private val _callEvents = MutableSharedFlow<VerimorCallEvent>(
        replay = 0,
        extraBufferCapacity = 20,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val callEvents: SharedFlow<VerimorCallEvent> = _callEvents.asSharedFlow()

    private val _missedCallEvents = MutableSharedFlow<MissedCallRecord>(
        replay = 0,
        extraBufferCapacity = 20,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val missedCallEvents: SharedFlow<MissedCallRecord> = _missedCallEvents.asSharedFlow()

    private val socketLock = Any()
    private var socket: DatagramSocket? = null

    private var regTimeoutJob: Job? = null
    private var authTimeoutJob: Job? = null
    private var listenJob: Job? = null
    private var keepAliveJob: Job? = null
    private var autoReconnectJob: Job? = null
    private var callDurationJob: Job? = null
    private var okRetransmitJob: Job? = null
    private var rtpFallbackJob: Job? = null

    private var activeCallId: String = ""
    private var activeFromTag: String = ""
    private val cseqCounter = AtomicInteger(1)
    private var authAttempts = 0

    private var currentConfig: VerimorSipConfig? = null
    private var isUserExplicitlyDisconnected = false
    private val processedCallEndIds = java.util.Collections.synchronizedSet(LinkedHashSet<String>())

    @Volatile
    var isMasterPowerEnabled: Boolean = true
        private set

    fun setMasterPower(enabled: Boolean) {
        isMasterPowerEnabled = enabled
        if (!enabled) {
            disconnect()
        }
    }

    // Mevcut veya gelen aktif çağrı diyaloğu
    @Volatile
    private var activeDialog: ActiveCallDialog? = null

    val isMuted: StateFlow<Boolean> = rtpAudioEngine.isMuted
    val isSpeaker: StateFlow<Boolean> = rtpAudioEngine.isSpeaker
    val isAudioStreaming: StateFlow<Boolean> = rtpAudioEngine.isStreaming

    fun getActiveDialog(): ActiveCallDialog? = activeDialog

    /**
     * Verimor Santraline SIP Kaydı (REGISTER) Başlatır.
     */
    fun register(config: VerimorSipConfig) {
        if (!isMasterPowerEnabled) {
            Log.d(TAG, "Registration skipped because Master Power is OFF")
            return
        }
        val cleanDomain = config.domain.trim()
        val cleanExt = config.extension.trim()
        val cleanPass = config.password.trim()

        if (cleanDomain.isBlank() || cleanExt.isBlank()) {
            _connectionState.value = VerimorSipState.Error("Dahili numarası ve Domain adresi boş bırakılamaz.")
            return
        }

        isUserExplicitlyDisconnected = false
        currentConfig = config

        cleanupNetworkJobs()

        _connectionState.value = VerimorSipState.Connecting
        authAttempts = 0

        try {
            VerimorSipService.startService(appContext)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to start foreground service: ${e.message}")
        }

        // 15 saniyelik kayıt zaman aşımı kontrolü
        regTimeoutJob = managerScope.launch {
            delay(REGISTRATION_TIMEOUT_MS)
            if (_connectionState.value is VerimorSipState.Connecting) {
                Log.w(TAG, "SIP Registration timed out after ${REGISTRATION_TIMEOUT_MS}ms")
                _connectionState.value = VerimorSipState.Error(
                    "Zaman aşımı: Santralden ($cleanDomain) yanıt alınamadı. Host ve port (5060) ayarlarınızı kontrol ediniz."
                )
                cleanupNetworkJobs()
                scheduleAutoReconnectIfAllowed()
            }
        }

        managerScope.launch {
            try {
                // DNS Çözümleme (5 saniye tavan süre)
                val serverAddress = withTimeoutOrNull(5000L) {
                    try {
                        InetAddress.getByName(cleanDomain)
                    } catch (e: Exception) {
                        null
                    }
                }

                if (serverAddress == null) {
                    regTimeoutJob?.cancel()
                    _connectionState.value = VerimorSipState.Error("DNS Çözümlenemedi: '$cleanDomain' ana makinesi bulunamadı.")
                    cleanupNetworkJobs()
                    return@launch
                }

                // UDP Soketi oluştur
                val localSocket = try {
                    DatagramSocket().apply {
                        reuseAddress = true
                    }
                } catch (e: Exception) {
                    regTimeoutJob?.cancel()
                    _connectionState.value = VerimorSipState.Error("Soket Açılamadı: ${e.localizedMessage}")
                    cleanupNetworkJobs()
                    return@launch
                }

                synchronized(socketLock) {
                    socket = localSocket
                }

                val localPort = localSocket.localPort
                Log.d(TAG, "SIP UDP Socket opened on port $localPort -> Server: ${serverAddress.hostAddress}:${config.port}")

                activeCallId = UUID.randomUUID().toString() + "@" + (localSocket.localAddress?.hostAddress ?: "127.0.0.1")
                activeFromTag = UUID.randomUUID().toString().replace("-", "").take(8)
                cseqCounter.set(1)

                startListening(serverAddress, config.port, cleanExt, cleanDomain, cleanPass)

                val regMessage = buildRegisterMessage(
                    domain = cleanDomain,
                    extension = cleanExt,
                    localPort = localPort,
                    callId = activeCallId,
                    fromTag = activeFromTag,
                    cseq = cseqCounter.get(),
                    authHeader = null
                )

                sendSipPacket(regMessage, serverAddress, config.port)

            } catch (e: Exception) {
                Log.e(TAG, "SIP Registration exception", e)
                regTimeoutJob?.cancel()
                _connectionState.value = VerimorSipState.Error("Bağlantı Hatası: ${e.localizedMessage}")
                cleanupNetworkJobs()
                scheduleAutoReconnectIfAllowed()
            }
        }
    }

    /**
     * Kullanıcı tarafından manuel bağlantı kesme
     */
    fun disconnect() {
        isUserExplicitlyDisconnected = true
        autoReconnectJob?.cancel()
        autoReconnectJob = null

        hangupCall()
        cleanupNetworkJobs()
        _connectionState.value = VerimorSipState.Disconnected

        try {
            VerimorSipService.stopService(appContext)
        } catch (_: Exception) {}

        Log.d(TAG, "Verimor SIP Disconnected by user")
    }

    /**
     * Test amaçlı simüle çağrı tetikler
     */
    fun simulateIncomingCall(phone: String = "1006", name: String = "Telefon Destek") {
        val cleanPhone = phone.replace(Regex("\\s+"), "").trim()
        val loopback = InetAddress.getLoopbackAddress()

        activeDialog = ActiveCallDialog(
            callerNumber = cleanPhone,
            callerName = name,
            callId = "sim-${System.currentTimeMillis()}",
            from = "\"$name\" <sip:$cleanPhone@127.0.0.1>",
            to = "<sip:${currentConfig?.extension ?: "101"}@127.0.0.1>",
            via = "SIP/2.0/UDP 127.0.0.1:5060;branch=z9hG4bKsim",
            cseq = "1 INVITE",
            contact = "<sip:$cleanPhone@127.0.0.1:5060>",
            serverAddress = loopback,
            serverPort = 5060,
            remoteRtpAddress = loopback,
            remoteRtpPort = 10002,
            preferredCodec = 8,
            isSimulated = true
        )

        val event = VerimorCallEvent(
            callerNumber = cleanPhone,
            callerName = name,
            isSimulated = true
        )
        _connectionState.value = VerimorSipState.Ringing(cleanPhone, name)
        ringtonePlayer.start()
        managerScope.launch {
            _callEvents.emit(event)
        }
    }

    /**
     * Dış Arama Başlat (Verimor SIP INVITE ile doğrudan santral üzerinden arama)
     */
    fun makeCall(targetNumber: String, callerDisplayName: String = "") {
        if (!isMasterPowerEnabled) {
            _connectionState.value = VerimorSipState.Error("SIP kapalı. Ana güç anahtarını (ON) açınız.")
            return
        }
        val cfg = currentConfig
        val cleanTarget = targetNumber.replace(Regex("[^0-9+]"), "").trim()
        if (cleanTarget.isBlank()) return

        managerScope.launch(Dispatchers.IO) {
            try {
                val serverAddress = if (cfg != null && cfg.domain.isNotBlank()) {
                    withTimeoutOrNull(4000L) {
                        try { InetAddress.getByName(cfg.domain) } catch (_: Exception) { null }
                    } ?: InetAddress.getLoopbackAddress()
                } else InetAddress.getLoopbackAddress()

                val localSocket = synchronized(socketLock) { socket }
                val localPort = localSocket?.localPort ?: 5060
                val localIp = getLocalIpAddress(serverAddress)
                val callId = UUID.randomUUID().toString() + "@" + localIp
                val fromTag = UUID.randomUUID().toString().replace("-", "").take(8)
                val branch = "z9hG4bK" + UUID.randomUUID().toString().replace("-", "").take(12)
                val cseq = cseqCounter.incrementAndGet()
                val ext = cfg?.extension ?: "101"
                val srvPort = cfg?.port ?: 5060
                val domain = cfg?.domain ?: "verimor.com.tr"

                val localRtpPort = rtpAudioEngine.prepare()
                val sdpOffer = buildSdpAnswer(localIp, localRtpPort, 8)
                val sdpBytes = sdpOffer.toByteArray(Charsets.UTF_8)

                val inviteMsg = StringBuilder().apply {
                    append("INVITE sip:$cleanTarget@$domain:$srvPort SIP/2.0\r\n")
                    append("Via: SIP/2.0/UDP $localIp:$localPort;branch=$branch;rport\r\n")
                    append("Max-Forwards: 70\r\n")
                    append("From: <sip:$ext@$domain>;tag=$fromTag\r\n")
                    append("To: <sip:$cleanTarget@$domain>\r\n")
                    append("Call-ID: $callId\r\n")
                    append("CSeq: $cseq INVITE\r\n")
                    append("Contact: <sip:$ext@$localIp:$localPort>\r\n")
                    append("Allow: INVITE, ACK, CANCEL, OPTIONS, BYE, REFER, NOTIFY, MESSAGE, INFO\r\n")
                    append("User-Agent: VerimorBulutSantralMobile/2.0\r\n")
                    append("Content-Type: application/sdp\r\n")
                    append("Content-Length: ${sdpBytes.size}\r\n\r\n")
                    append(sdpOffer)
                }.toString()

                activeDialog = ActiveCallDialog(
                    callerNumber = cleanTarget,
                    callerName = if (callerDisplayName.isNotBlank() && callerDisplayName.any { it.isLetter() }) callerDisplayName else "",
                    callId = callId,
                    from = "<sip:$ext@$domain>;tag=$fromTag",
                    to = "<sip:$cleanTarget@$domain>",
                    via = "SIP/2.0/UDP $localIp:$localPort;branch=$branch",
                    cseq = "$cseq INVITE",
                    contact = "sip:$cleanTarget@$domain:$srvPort",
                    serverAddress = serverAddress,
                    serverPort = srvPort,
                    remoteRtpAddress = serverAddress,
                    remoteRtpPort = 10000,
                    preferredCodec = 8,
                    localToTag = fromTag,
                    isSimulated = (localSocket == null)
                )

                _connectionState.value = VerimorSipState.InCall(cleanTarget, 0)
                startCallDurationTimer(cleanTarget)

                if (localSocket != null) {
                    sendSipPacket(inviteMsg, serverAddress, srvPort)
                    Log.i(TAG, "Sent outbound INVITE to $cleanTarget at $domain:$srvPort")
                }

                // Start RTP audio stream
                rtpAudioEngine.start(
                    remoteAddress = serverAddress,
                    remotePort = 10000,
                    payloadType = 8
                )
            } catch (e: Exception) {
                Log.e(TAG, "makeCall failed", e)
                _connectionState.value = VerimorSipState.Error("Arama hatası: ${e.message}")
            }
        }
    }

    /**
     * Gelen çağrıyı yanıtla (200 OK + SDP gönderir, PBX ACK geldiğinde RTP ses akışını başlatır)
     */
    fun answerCall() {
        ringtonePlayer.stop()
        val current = _connectionState.value
        if (current !is VerimorSipState.Ringing && activeDialog == null) {
            Log.w(TAG, "Cannot answer call when state is not Ringing and no active dialog: $current")
            return
        }

        val callerNum = when (current) {
            is VerimorSipState.Ringing -> current.callerNumber
            is VerimorSipState.InCall -> current.callerNumber
            else -> activeDialog?.callerNumber ?: "1006"
        }
        val callerNm = when (current) {
            is VerimorSipState.Ringing -> current.callerName
            else -> activeDialog?.callerName ?: "Santral Çağrısı"
        }

        val dialog = activeDialog ?: ActiveCallDialog(
            callerNumber = callerNum,
            callerName = callerNm,
            callId = "fallback-${System.currentTimeMillis()}",
            from = callerNum,
            to = currentConfig?.extension ?: "101",
            via = "",
            cseq = "1 INVITE",
            contact = null,
            serverAddress = InetAddress.getLoopbackAddress(),
            serverPort = 5060,
            remoteRtpAddress = InetAddress.getLoopbackAddress(),
            remoteRtpPort = 10002,
            isSimulated = true
        ).also { activeDialog = it }

        Log.i(TAG, "Answering call from ${dialog.callerNumber} (Simulated: ${dialog.isSimulated})")

        // 1. RTP Portunu ve Yerel IP'yi hazırla
        val localRtpPort = rtpAudioEngine.prepare()
        val localIp = getLocalIpAddress(dialog.serverAddress)
        val localSipPort = synchronized(socketLock) { socket?.localPort } ?: 5060
        val ext = currentConfig?.extension ?: "101"

        // 2. Durumu Görüşmede olarak güncelle ve kronometreyi başlat
        _connectionState.value = VerimorSipState.InCall(dialog.callerNumber, 0)
        startCallDurationTimer(dialog.callerNumber)

        if (dialog.isSimulated) {
            // Test amaçlı simüle çağrıda doğrudan sesi başlat
            rtpAudioEngine.start(
                remoteAddress = dialog.remoteRtpAddress,
                remotePort = dialog.remoteRtpPort,
                payloadType = dialog.preferredCodec
            )
        } else {
            // 3. RFC 3261 Uyumlu SIP/2.0 200 OK + SDP Yanıtı Gönder
            val sdpAnswer = buildSdpAnswer(localIp, localRtpPort, dialog.preferredCodec)
            val sdpBytes = sdpAnswer.toByteArray(Charsets.UTF_8)
            val toWithTag = if (dialog.to.contains(";tag=")) dialog.to else "${dialog.to};tag=${dialog.localToTag}"
            val cseqNumber = dialog.cseq.substringBefore(" ").trim()
            val cseqFull = if (dialog.cseq.contains("INVITE", ignoreCase = true)) dialog.cseq else "$cseqNumber INVITE"

            val okResponse = StringBuilder()
            okResponse.append("SIP/2.0 200 OK\r\n")
            if (dialog.viaHeaders.isNotEmpty()) {
                for (vh in dialog.viaHeaders) {
                    okResponse.append("Via: $vh\r\n")
                }
            } else if (dialog.via.isNotBlank()) {
                okResponse.append("Via: ${dialog.via}\r\n")
            }
            for (rr in dialog.recordRouteHeaders) {
                okResponse.append("Record-Route: $rr\r\n")
            }
            okResponse.append("From: ${dialog.from}\r\n")
            okResponse.append("To: $toWithTag\r\n")
            okResponse.append("Call-ID: ${dialog.callId}\r\n")
            okResponse.append("CSeq: $cseqFull\r\n")
            okResponse.append("Contact: <sip:$ext@$localIp:$localSipPort>\r\n")
            okResponse.append("Allow: INVITE, ACK, CANCEL, OPTIONS, BYE, REFER, NOTIFY, MESSAGE, INFO\r\n")
            okResponse.append("User-Agent: VerimorBulutSantralMobile/2.0\r\n")
            okResponse.append("Content-Type: application/sdp\r\n")
            okResponse.append("Content-Length: ${sdpBytes.size}\r\n\r\n")
            okResponse.append(sdpAnswer)

            val okStr = okResponse.toString()
            send200OkToAllTargets(okStr, dialog)
            Log.i(TAG, "Sent 200 OK with SDP Answer to PBX -> ${dialog.serverAddress.hostAddress}:${dialog.serverPort}")

            // 200 OK paketini santral ACK yanıtını dönene kadar 350ms aralıklarla tekrar gönder (RFC 3261 retransmission)
            start200OkRetransmissions(okStr, dialog)

            // Güvenlik Gecikmesi: PBX ACK paketi UDP'de düşerse veya gecikirse en geç 1500ms sonra RTP'yi otomatik başlat
            rtpFallbackJob?.cancel()
            rtpFallbackJob = managerScope.launch {
                delay(1500L)
                if (isActive && !rtpAudioEngine.isStreaming.value && _connectionState.value is VerimorSipState.InCall) {
                    Log.i(TAG, "Fallback: Starting RTP audio path (PBX ACK not received within 1500ms)")
                    rtpAudioEngine.start(
                        remoteAddress = dialog.remoteRtpAddress,
                        remotePort = dialog.remoteRtpPort,
                        payloadType = dialog.preferredCodec
                    )
                }
            }
        }
    }

    private fun send200OkToAllTargets(okMsg: String, dialog: ActiveCallDialog) {
        val targets = mutableListOf<Pair<InetAddress, Int>>()
        // 1. Gelen paketin fiziksel kaynak IP ve Portu
        targets.add(Pair(dialog.serverAddress, dialog.serverPort))

        // 2. Port farklıysa santral standart SIP portu (5060)
        if (dialog.serverPort != 5060) {
            targets.add(Pair(dialog.serverAddress, 5060))
        }

        // 3. RFC 3261 En üst Via başlığındaki sent-by / received / rport
        try {
            val topVia = dialog.viaHeaders.firstOrNull() ?: dialog.via
            if (topVia.isNotBlank()) {
                val receivedParam = extractAuthParam(topVia, "received")
                val rportParam = extractAuthParam(topVia, "rport")
                val hostPort = topVia.substringBefore(";").substringAfter("SIP/2.0/UDP").trim()
                val viaHost = receivedParam?.ifBlank { null } ?: hostPort.substringBefore(":")
                val viaPort = rportParam?.toIntOrNull() ?: hostPort.substringAfter(":", "5060").toIntOrNull() ?: 5060
                if (viaHost.isNotBlank() && viaHost != "0.0.0.0") {
                    val viaAddr = InetAddress.getByName(viaHost)
                    targets.add(Pair(viaAddr, viaPort))
                }
            }
        } catch (_: Exception) {}

        // 4. Konfigürasyondaki santral sunucu host ve portu
        val cfg = currentConfig
        if (cfg != null) {
            try {
                val cfgAddr = InetAddress.getByName(cfg.domain)
                targets.add(Pair(cfgAddr, cfg.port))
            } catch (_: Exception) {}
        }

        val sentSet = mutableSetOf<String>()
        for ((addr, port) in targets) {
            val key = "${addr.hostAddress}:$port"
            if (sentSet.add(key)) {
                sendSipPacket(okMsg, addr, port)
            }
        }
    }

    private fun start200OkRetransmissions(okMsg: String, dialog: ActiveCallDialog) {
        okRetransmitJob?.cancel()
        okRetransmitJob = managerScope.launch {
            var count = 0
            while (isActive && count < 8 && _connectionState.value is VerimorSipState.InCall) {
                delay(350L)
                count++
                Log.d(TAG, "Retransmitting 200 OK #$count to PBX")
                send200OkToAllTargets(okMsg, dialog)
            }
        }
    }

    private fun stop200OkRetransmissions() {
        okRetransmitJob?.cancel()
        okRetransmitJob = null
        rtpFallbackJob?.cancel()
        rtpFallbackJob = null
    }

    private fun sendToTopViaIfDifferent(okMsg: String, dialog: ActiveCallDialog) {
        send200OkToAllTargets(okMsg, dialog)
    }

    /**
     * Gelen çağrıyı reddet (Santrale 486 Busy Here iletir)
     */
    fun declineCall() {
        ringtonePlayer.stop()
        val dialog = activeDialog
        if (dialog != null && !dialog.isSimulated) {
            val localIp = getLocalIpAddress(dialog.serverAddress)
            val localPort = synchronized(socketLock) { socket?.localPort } ?: 5060
            val branch = "z9hG4bK" + UUID.randomUUID().toString().replace("-", "").take(12)
            val toWithTag = if (dialog.to.contains(";tag=")) dialog.to else "${dialog.to};tag=${dialog.localToTag}"

            val busyMsg = StringBuilder()
            busyMsg.append("SIP/2.0 486 Busy Here\r\n")
            if (dialog.viaHeaders.isNotEmpty()) {
                for (vh in dialog.viaHeaders) {
                    busyMsg.append("Via: $vh\r\n")
                }
            } else if (dialog.via.isNotBlank()) {
                busyMsg.append("Via: ${dialog.via}\r\n")
            } else {
                busyMsg.append("Via: SIP/2.0/UDP $localIp:$localPort;branch=$branch;rport\r\n")
            }
            busyMsg.append("From: ${dialog.from}\r\n")
            busyMsg.append("To: $toWithTag\r\n")
            busyMsg.append("Call-ID: ${dialog.callId}\r\n")
            busyMsg.append("CSeq: ${dialog.cseq}\r\n")
            busyMsg.append("User-Agent: VerimorBulutSantralMobile/2.0\r\n")
            busyMsg.append("Content-Length: 0\r\n\r\n")

            val msgStr = busyMsg.toString()
            sendSipPacket(msgStr, dialog.serverAddress, dialog.serverPort)
            if (dialog.serverPort != 5060) {
                sendSipPacket(msgStr, dialog.serverAddress, 5060)
            }
            Log.i(TAG, "Sent 486 Busy Here to PBX for ${dialog.callerNumber}")
        }

        cleanupCallState()
    }

    /**
     * DTMF Tuş Sinyali Gönderimi (Hem RFC 2833 RTP hem de SIP INFO RFC 2976 üzerinden)
     */
    fun sendDtmf(digit: Char) {
        // 1. RFC 2833 RTP üzerinden ses paketi ve yerel ses çalımı
        rtpAudioEngine.sendDtmf(digit)

        // 2. SIP INFO sinyali (Verimor Santral Transfer & Menü Yönlendirme için)
        val dialog = activeDialog
        val currentState = _connectionState.value
        if (dialog != null && currentState is VerimorSipState.InCall && !dialog.isSimulated) {
            managerScope.launch(Dispatchers.IO) {
                try {
                    val nextCSeq = cseqCounter.incrementAndGet()
                    val branch = "z9hG4bK" + UUID.randomUUID().toString().replace("-", "").take(12)
                    val localPort = synchronized(socketLock) { socket?.localPort } ?: 5060
                    val toWithTag = if (dialog.to.contains(";tag=")) dialog.to else "${dialog.to};tag=${dialog.localToTag}"
                    val dtmfBody = "Signal=$digit\r\nDuration=160\r\n"
                    val targetUri = dialog.contact ?: "sip:${dialog.callerNumber}@${dialog.serverAddress.hostAddress}:${dialog.serverPort}"

                    val infoMsg = StringBuilder()
                    infoMsg.append("INFO $targetUri SIP/2.0\r\n")
                    infoMsg.append("Via: SIP/2.0/UDP 0.0.0.0:$localPort;branch=$branch\r\n")
                    infoMsg.append("Max-Forwards: 70\r\n")
                    infoMsg.append("From: $toWithTag\r\n")
                    infoMsg.append("To: ${dialog.from}\r\n")
                    infoMsg.append("Call-ID: ${dialog.callId}\r\n")
                    infoMsg.append("CSeq: $nextCSeq INFO\r\n")
                    infoMsg.append("User-Agent: VerimorBulutSantralMobile/2.0\r\n")
                    infoMsg.append("Content-Type: application/dtmf-relay\r\n")
                    infoMsg.append("Content-Length: ${dtmfBody.toByteArray(Charsets.UTF_8).size}\r\n\r\n")
                    infoMsg.append(dtmfBody)

                    sendSipPacket(infoMsg.toString(), dialog.serverAddress, dialog.serverPort)
                    Log.i(TAG, "Sent SIP INFO DTMF '$digit' to ${dialog.serverAddress.hostAddress}:${dialog.serverPort}")
                } catch (e: Exception) {
                    Log.w(TAG, "Error sending SIP INFO DTMF: ${e.message}")
                }
            }
        }
    }

    /**
     * Çoklu DTMF Sırası Gönderimi (Örn: *2202 aktarma dizisi)
     */
    fun sendDtmfSequence(sequence: String) {
        managerScope.launch(Dispatchers.IO) {
            for (ch in sequence) {
                sendDtmf(ch)
                delay(120L)
            }
        }
    }

    /**
     * Görüşmeyi sonlandır (Santrale BYE iletir, karşı tarafın hattını hemen düşürür)
     */
    fun hangupCall() {
        ringtonePlayer.stop()
        rtpAudioEngine.stop()
        val dialog = activeDialog
        val currentState = _connectionState.value

        if (currentState is VerimorSipState.Ringing) {
            declineCall()
            return
        }

        if (dialog != null && !dialog.isSimulated) {
            val nextCSeq = cseqCounter.incrementAndGet()
            val branch = "z9hG4bK" + UUID.randomUUID().toString().replace("-", "").take(12)
            val localPort = synchronized(socketLock) { socket?.localPort } ?: 5060
            val localIp = getLocalIpAddress(dialog.serverAddress)
            val toWithTag = if (dialog.to.contains(";tag=")) dialog.to else "${dialog.to};tag=${dialog.localToTag}"

            // Request URI: Contact başlığı varsa doğrudan hedef URI, yoksa PBX adresi
            val reqUri = if (!dialog.contact.isNullOrBlank()) {
                val clean = dialog.contact.substringAfter("<").substringBefore(">").trim()
                if (clean.startsWith("sip:")) clean else "sip:$clean"
            } else {
                "sip:${dialog.callerNumber}@${dialog.serverAddress.hostAddress}:${dialog.serverPort}"
            }

            val byeMsg = StringBuilder()
            byeMsg.append("BYE $reqUri SIP/2.0\r\n")
            byeMsg.append("Via: SIP/2.0/UDP $localIp:$localPort;branch=$branch;rport\r\n")
            if (dialog.recordRouteHeaders.isNotEmpty()) {
                for (rr in dialog.recordRouteHeaders) {
                    byeMsg.append("Route: $rr\r\n")
                }
            }
            byeMsg.append("Max-Forwards: 70\r\n")
            byeMsg.append("From: $toWithTag\r\n")
            byeMsg.append("To: ${dialog.from}\r\n")
            byeMsg.append("Call-ID: ${dialog.callId}\r\n")
            byeMsg.append("CSeq: $nextCSeq BYE\r\n")
            byeMsg.append("User-Agent: VerimorBulutSantralMobile/2.0\r\n")
            byeMsg.append("Content-Length: 0\r\n\r\n")

            val byePacketStr = byeMsg.toString()

            // 1. İlk BYE paketini hemen gönder
            sendSipPacket(byePacketStr, dialog.serverAddress, dialog.serverPort)
            if (dialog.serverPort != 5060) {
                sendSipPacket(byePacketStr, dialog.serverAddress, 5060)
            }
            Log.i(TAG, "Sent BYE to PBX for ${dialog.callerNumber} -> ${dialog.serverAddress.hostAddress}:${dialog.serverPort}")

            // 2. UDP paket kaybına karşı 80ms ve 200ms sonra BYE tekrarı gönder (RFC 3261 güvenli sonlandırma)
            managerScope.launch(Dispatchers.IO) {
                delay(80L)
                sendSipPacket(byePacketStr, dialog.serverAddress, dialog.serverPort)
                if (dialog.serverPort != 5060) {
                    sendSipPacket(byePacketStr, dialog.serverAddress, 5060)
                }
                delay(120L)
                sendSipPacket(byePacketStr, dialog.serverAddress, dialog.serverPort)
            }
        }

        cleanupCallState()
    }

    private fun cleanupCallState() {
        stop200OkRetransmissions()
        ringtonePlayer.stop()
        rtpAudioEngine.stop()
        callDurationJob?.cancel()
        callDurationJob = null
        activeDialog = null

        val cfg = currentConfig
        if (cfg != null && _connectionState.value !is VerimorSipState.Error) {
            _connectionState.value = VerimorSipState.Registered(cfg.extension, cfg.domain)
        } else {
            _connectionState.value = VerimorSipState.Disconnected
        }
    }

    private fun startCallDurationTimer(callerNumber: String) {
        callDurationJob?.cancel()
        callDurationJob = managerScope.launch {
            var seconds = 0L
            while (isActive && _connectionState.value is VerimorSipState.InCall) {
                delay(1000L)
                seconds++
                _connectionState.value = VerimorSipState.InCall(callerNumber, seconds)
            }
        }
    }

    fun toggleSpeaker(): Boolean = rtpAudioEngine.toggleSpeaker()
    fun setSpeaker(enable: Boolean) = rtpAudioEngine.setSpeaker(enable)
    fun toggleMute(): Boolean = rtpAudioEngine.toggleMute()
    fun setMute(mute: Boolean) = rtpAudioEngine.setMute(mute)

    private fun cleanupNetworkJobs() {
        stop200OkRetransmissions()
        regTimeoutJob?.cancel()
        regTimeoutJob = null
        authTimeoutJob?.cancel()
        authTimeoutJob = null
        listenJob?.cancel()
        listenJob = null
        keepAliveJob?.cancel()
        keepAliveJob = null

        cleanupCallState()

        synchronized(socketLock) {
            try {
                socket?.close()
            } catch (_: Exception) {}
            socket = null
        }
    }

    private var reconnectAttempt = 0

    private fun scheduleAutoReconnectIfAllowed() {
        if (isUserExplicitlyDisconnected || !isMasterPowerEnabled) return
        val cfg = currentConfig ?: return

        // Batarya ve işlem yükü optimizasyonu: Akıllı kademeli bekleme (Exponential backoff)
        // 5-25 saniyelik agresif döngü kaldırıldı; 30s -> 60s -> 120s -> 300s aralığına çekildi.
        val delayMs = when (reconnectAttempt) {
            0 -> 30_000L
            1 -> 60_000L
            2 -> 120_000L
            else -> 300_000L
        }
        reconnectAttempt++

        autoReconnectJob?.cancel()
        autoReconnectJob = managerScope.launch {
            delay(delayMs)
            if (!isUserExplicitlyDisconnected && isMasterPowerEnabled && (_connectionState.value is VerimorSipState.Error || _connectionState.value is VerimorSipState.Disconnected)) {
                Log.i(TAG, "Attempting auto-reconnect to Verimor SIP after ${delayMs / 1000}s...")
                register(cfg)
            }
        }
    }

    // --- UDP PAKET DİNLEME VE AYIKLAMA ---

    private fun startListening(
        serverAddress: InetAddress,
        serverPort: Int,
        extension: String,
        domain: String,
        password: String
    ) {
        listenJob?.cancel()
        listenJob = managerScope.launch(Dispatchers.IO) {
            val buffer = ByteArray(4096)
            while (isActive) {
                val currentSocket = synchronized(socketLock) { socket } ?: break
                if (currentSocket.isClosed) break

                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    currentSocket.receive(packet)

                    val receivedText = String(packet.data, 0, packet.length, Charsets.UTF_8)
                    Log.d(TAG, "SIP RX (${packet.length} bytes) <- ${packet.address.hostAddress}:${packet.port}")

                    handleIncomingSipPacket(
                        sipText = receivedText,
                        serverAddress = serverAddress,
                        serverPort = serverPort,
                        packetAddress = packet.address,
                        packetPort = packet.port,
                        extension = extension,
                        domain = domain,
                        password = password
                    )

                } catch (e: Exception) {
                    if (isActive && !currentSocket.isClosed) {
                        Log.e(TAG, "SIP Socket receive error: ${e.message}")
                    }
                    break
                }
            }
        }
    }

    private suspend fun handleIncomingSipPacket(
        sipText: String,
        serverAddress: InetAddress,
        serverPort: Int,
        packetAddress: InetAddress,
        packetPort: Int,
        extension: String,
        domain: String,
        password: String
    ) {
        val firstLine = sipText.lines().firstOrNull()?.trim() ?: return

        when {
            // 200 OK (Kayıt başarılı veya Görüşme Onayı)
            firstLine.startsWith("SIP/2.0 200 OK") -> {
                regTimeoutJob?.cancel()
                authTimeoutJob?.cancel()
                authAttempts = 0

                val cseqHeader = extractHeader(sipText, "CSeq") ?: ""
                if (cseqHeader.contains("REGISTER", ignoreCase = true)) {
                    Log.i(TAG, "✓ Verimor SIP 200 OK Kayıt Başarılı! Dahili: $extension@$domain")
                    reconnectAttempt = 0
                    _connectionState.value = VerimorSipState.Registered(extension, domain)
                    startKeepAlive(serverAddress, serverPort, extension, domain)
                }
            }

            // 401 Unauthorized veya 407 Proxy Authentication Required
            firstLine.startsWith("SIP/2.0 401") || firstLine.startsWith("SIP/2.0 407") -> {
                regTimeoutJob?.cancel()
                authAttempts++

                if (authAttempts > 2) {
                    Log.w(TAG, "SIP Authentication failed repeatedly. Credentials incorrect.")
                    authTimeoutJob?.cancel()
                    _connectionState.value = VerimorSipState.Error("Kimlik doğrulama başarısız: Dahili şifresi veya kullanıcı adı hatalı.")
                    cleanupNetworkJobs()
                    return
                }

                Log.d(TAG, "Received SIP Auth Challenge ($firstLine). Computing Digest response...")

                val isProxy = firstLine.startsWith("SIP/2.0 407") || extractHeader(sipText, "Proxy-Authenticate") != null
                val challengeHeader = if (isProxy) {
                    extractHeader(sipText, "Proxy-Authenticate") ?: extractHeader(sipText, "WWW-Authenticate")
                } else {
                    extractHeader(sipText, "WWW-Authenticate") ?: extractHeader(sipText, "Proxy-Authenticate")
                }

                if (challengeHeader != null && password.isNotBlank()) {
                    val authResponseHeader = buildDigestAuthorizationHeader(
                        challengeHeader = challengeHeader,
                        isProxy = isProxy,
                        username = extension,
                        password = password,
                        domain = domain,
                        method = "REGISTER",
                        uri = "sip:$domain"
                    )

                    val localPort = synchronized(socketLock) { socket?.localPort } ?: 5060
                    val nextCSeq = cseqCounter.incrementAndGet()

                    val authRegMsg = buildRegisterMessage(
                        domain = domain,
                        extension = extension,
                        localPort = localPort,
                        callId = activeCallId,
                        fromTag = activeFromTag,
                        cseq = nextCSeq,
                        authHeader = authResponseHeader
                    )

                    authTimeoutJob?.cancel()
                    authTimeoutJob = managerScope.launch {
                        delay(AUTH_TIMEOUT_MS)
                        if (_connectionState.value is VerimorSipState.Connecting) {
                            Log.w(TAG, "SIP Auth timed out after ${AUTH_TIMEOUT_MS}ms")
                            _connectionState.value = VerimorSipState.Error("Kimlik doğrulama zaman aşımı: Santral yanıt vermedi.")
                            cleanupNetworkJobs()
                            scheduleAutoReconnectIfAllowed()
                        }
                    }

                    sendSipPacket(authRegMsg, serverAddress, serverPort)

                } else {
                    _connectionState.value = VerimorSipState.Error("Santral Dahili Şifresi İstedi. Lütfen Dahili Şifrenizi giriniz.")
                    cleanupNetworkJobs()
                }
            }

            // 403 Forbidden veya 404 Not Found
            firstLine.startsWith("SIP/2.0 403") || firstLine.startsWith("SIP/2.0 404") -> {
                regTimeoutJob?.cancel()
                authTimeoutJob?.cancel()
                val reason = if (firstLine.contains("403")) "403 Yasaklandı (Dahili veya Şifre Hatalı)" else "404 Dahili Bulunamadı"
                _connectionState.value = VerimorSipState.Error("Santral Reddi: $reason")
                cleanupNetworkJobs()
            }

            // Gelen Çağrı (INVITE)
            firstLine.startsWith("INVITE") -> {
                Log.i(TAG, "📞 Incoming SIP INVITE received from PBX")
                val callId = extractHeader(sipText, "Call-ID") ?: ""
                val fromHeader = extractHeader(sipText, "From") ?: ""
                val toHeader = extractHeader(sipText, "To") ?: ""
                val viaHeader = extractHeader(sipText, "Via") ?: ""
                val cseq = extractHeader(sipText, "CSeq") ?: ""
                val contact = extractHeader(sipText, "Contact")

                val viaHeaders = extractAllHeaders(sipText, "Via")
                val recordRouteHeaders = extractAllHeaders(sipText, "Record-Route")
                val preferredCodec = extractPreferredCodec(sipText)

                // Çağrı halihazırda yanıtlanmışsa ve santral UDP paketi tekrar ettiyse (Retransmission) 200 OK yanıtını tekrar ilet
                val current = _connectionState.value
                val existingDialog = activeDialog
                if (current is VerimorSipState.InCall && existingDialog != null && existingDialog.callId == callId) {
                    Log.i(TAG, "Retransmitting 200 OK for INVITE Call-ID: $callId")
                    val localRtpPort = rtpAudioEngine.getLocalPort()
                    val localIp = getLocalIpAddress(existingDialog.serverAddress)
                    val localSipPort = synchronized(socketLock) { socket?.localPort } ?: 5060
                    val sdpAnswer = buildSdpAnswer(localIp, localRtpPort, existingDialog.preferredCodec)
                    val sdpBytes = sdpAnswer.toByteArray(Charsets.UTF_8)
                    val toWithTag = if (existingDialog.to.contains(";tag=")) existingDialog.to else "${existingDialog.to};tag=${existingDialog.localToTag}"

                    val okResponse = StringBuilder()
                    okResponse.append("SIP/2.0 200 OK\r\n")
                    if (existingDialog.viaHeaders.isNotEmpty()) {
                        for (vh in existingDialog.viaHeaders) {
                            okResponse.append("Via: $vh\r\n")
                        }
                    } else if (existingDialog.via.isNotBlank()) {
                        okResponse.append("Via: ${existingDialog.via}\r\n")
                    }
                    for (rr in existingDialog.recordRouteHeaders) {
                        okResponse.append("Record-Route: $rr\r\n")
                    }
                    okResponse.append("From: ${existingDialog.from}\r\n")
                    okResponse.append("To: $toWithTag\r\n")
                    okResponse.append("Call-ID: ${existingDialog.callId}\r\n")
                    val cseqNumber = cseq.substringBefore(" ").trim()
                    val cseqFull = if (cseq.contains("INVITE", ignoreCase = true)) cseq else "$cseqNumber INVITE"
                    okResponse.append("CSeq: $cseqFull\r\n")
                    okResponse.append("Contact: <sip:${currentConfig?.extension ?: "101"}@$localIp:$localSipPort>\r\n")
                    okResponse.append("Allow: INVITE, ACK, CANCEL, OPTIONS, BYE, REFER, NOTIFY, MESSAGE, INFO\r\n")
                    okResponse.append("User-Agent: VerimorBulutSantralMobile/2.0\r\n")
                    okResponse.append("Content-Type: application/sdp\r\n")
                    okResponse.append("Content-Length: ${sdpBytes.size}\r\n\r\n")
                    okResponse.append(sdpAnswer)
                    sendSipPacket(okResponse.toString(), packetAddress, packetPort)
                    return
                }

                val callerNumber = extractCallerNumber(fromHeader, sipText)
                val callerName = extractCallerName(fromHeader, sipText)

                // SDP Medya Bilgilerini Çözümle (RTP IP ve Port)
                val sdpConnectionIp = extractSdpConnectionIp(sipText)
                val remoteRtpAddress = if (!sdpConnectionIp.isNullOrBlank() && sdpConnectionIp != "0.0.0.0") {
                    try {
                        InetAddress.getByName(sdpConnectionIp)
                    } catch (_: Exception) {
                        packetAddress
                    }
                } else {
                    packetAddress
                }
                val remoteRtpPort = extractSdpAudioPort(sipText) ?: 10000

                Log.i(TAG, "Call from: $callerNumber ($callerName) -> Remote RTP: ${remoteRtpAddress.hostAddress}:$remoteRtpPort (Codec: $preferredCodec)")

                val dialog = ActiveCallDialog(
                    callerNumber = callerNumber,
                    callerName = callerName,
                    callId = callId,
                    from = fromHeader,
                    to = toHeader,
                    via = viaHeader,
                    viaHeaders = viaHeaders,
                    recordRouteHeaders = recordRouteHeaders,
                    cseq = cseq,
                    contact = contact,
                    serverAddress = packetAddress,
                    serverPort = packetPort,
                    remoteRtpAddress = remoteRtpAddress,
                    remoteRtpPort = remoteRtpPort,
                    preferredCodec = preferredCodec
                )
                activeDialog = dialog

                // Santrale derhal 180 Ringing cevabı ver
                sendRingingResponse(sipText, packetAddress, packetPort, dialog.localToTag, viaHeaders)

                _connectionState.value = VerimorSipState.Ringing(callerNumber, callerName)
                ringtonePlayer.start()
                _callEvents.emit(VerimorCallEvent(callerNumber, callerName))
            }

            // Çağrı Sonlandı (BYE veya CANCEL)
            firstLine.startsWith("BYE") || firstLine.startsWith("CANCEL") -> {
                val wasRinging = _connectionState.value is VerimorSipState.Ringing
                val ringingState = _connectionState.value as? VerimorSipState.Ringing
                val wasInCall = _connectionState.value is VerimorSipState.InCall
                val inCallState = _connectionState.value as? VerimorSipState.InCall
                val currentDialog = activeDialog
                val callId = (extractHeader(sipText, "Call-ID") ?: currentDialog?.callId ?: "").trim()

                ringtonePlayer.stop()
                sendOkResponse(sipText, packetAddress, packetPort)
                cleanupCallState()

                if (callId.isNotBlank() && !processedCallEndIds.add(callId)) {
                    // Bu çağrı için zaten kapanış kaydı oluşturuldu (retransmisyon önlendi)
                } else {
                    val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR")).format(Date())
                    if (wasRinging && ringingState != null) {
                        managerScope.launch {
                            _missedCallEvents.emit(
                                MissedCallRecord(
                                    callerNumber = ringingState.callerNumber,
                                    callerName = ringingState.callerName,
                                    timestamp = System.currentTimeMillis(),
                                    dateFormatted = dateStr,
                                    reason = "Cevapsız",
                                    isHandled = false,
                                    callType = "MISSED",
                                    duration = ""
                                )
                            )
                        }
                    } else if (wasInCall && inCallState != null) {
                        val m = inCallState.durationSeconds / 60
                        val s = inCallState.durationSeconds % 60
                        val durStr = "%02d:%02d".format(m, s)
                        managerScope.launch {
                            _missedCallEvents.emit(
                                MissedCallRecord(
                                    callerNumber = inCallState.callerNumber,
                                    callerName = currentDialog?.callerName ?: "",
                                    timestamp = System.currentTimeMillis(),
                                    dateFormatted = dateStr,
                                    reason = "Gelen Çağrı (Görüşüldü)",
                                    isHandled = true,
                                    callType = "INCOMING",
                                    duration = durStr
                                )
                            )
                        }
                    }
                }
            }

            // Santral ACK
            firstLine.startsWith("ACK") -> {
                Log.i(TAG, "✓ SIP ACK received from PBX for Call-ID: ${activeDialog?.callId}")
                stop200OkRetransmissions()
                val dialog = activeDialog
                if (dialog != null) {
                    rtpAudioEngine.start(
                        remoteAddress = dialog.remoteRtpAddress,
                        remotePort = dialog.remoteRtpPort,
                        payloadType = dialog.preferredCodec
                    )
                    Log.i(TAG, "RTP Audio Engine activated on PBX ACK -> ${dialog.remoteRtpAddress.hostAddress}:${dialog.remoteRtpPort}")
                }
            }

            // Santral OPTIONS ping kontrolü
            firstLine.startsWith("OPTIONS") -> {
                sendOkResponse(sipText, packetAddress, packetPort)
            }
        }
    }

    private fun buildDigestAuthorizationHeader(
        challengeHeader: String,
        isProxy: Boolean,
        username: String,
        password: String,
        domain: String,
        method: String,
        uri: String
    ): String {
        val realm = extractAuthParam(challengeHeader, "realm") ?: domain
        val nonce = extractAuthParam(challengeHeader, "nonce") ?: ""
        val opaque = extractAuthParam(challengeHeader, "opaque")
        val qop = extractAuthParam(challengeHeader, "qop")

        val headerName = if (isProxy) "Proxy-Authorization" else "Authorization"

        val ha1 = md5("$username:$realm:$password")
        val ha2 = md5("$method:$uri")

        val response: String
        val sb = StringBuilder()
        sb.append("$headerName: Digest username=\"$username\", realm=\"$realm\", nonce=\"$nonce\", uri=\"$uri\"")

        if (qop != null && qop.contains("auth", ignoreCase = true)) {
            val nc = "00000001"
            val cnonce = UUID.randomUUID().toString().replace("-", "").take(8)
            response = md5("$ha1:$nonce:$nc:$cnonce:auth:$ha2")
            sb.append(", qop=auth, nc=$nc, cnonce=\"$cnonce\"")
        } else {
            response = md5("$ha1:$nonce:$ha2")
        }

        sb.append(", response=\"$response\", algorithm=MD5")
        if (!opaque.isNullOrBlank()) {
            sb.append(", opaque=\"$opaque\"")
        }

        return sb.toString()
    }

    private fun startKeepAlive(
        serverAddress: InetAddress,
        serverPort: Int,
        extension: String,
        domain: String
    ) {
        keepAliveJob?.cancel()
        keepAliveJob = managerScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(90000L) // 90 saniyede bir akıllı can suyu (Pil tasarrufu optimizasyonu)
                try {
                    val localPort = synchronized(socketLock) { socket?.localPort } ?: 5060
                    val ping = buildRegisterMessage(
                        domain = domain,
                        extension = extension,
                        localPort = localPort,
                        callId = activeCallId,
                        fromTag = activeFromTag,
                        cseq = cseqCounter.incrementAndGet(),
                        authHeader = null
                    )
                    sendSipPacket(ping, serverAddress, serverPort)
                } catch (e: Exception) {
                    Log.w(TAG, "Keep-alive ping error: ${e.message}")
                    scheduleAutoReconnectIfAllowed()
                }
            }
        }
    }

    private fun sendSipPacket(message: String, address: InetAddress, port: Int) {
        try {
            val bytes = message.toByteArray(Charsets.UTF_8)
            val packet = DatagramPacket(bytes, bytes.size, address, port)
            synchronized(socketLock) {
                socket?.send(packet)
            }
            Log.d(TAG, "SIP TX (${bytes.size} bytes) -> ${address.hostAddress}:$port")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending SIP packet: ${e.message}")
        }
    }

    private fun sendRingingResponse(
        inviteText: String,
        address: InetAddress,
        port: Int,
        toTag: String,
        viaHeaders: List<String> = emptyList()
    ) {
        val from = extractHeader(inviteText, "From") ?: return
        val to = extractHeader(inviteText, "To") ?: return
        val callId = extractHeader(inviteText, "Call-ID") ?: return
        val cseq = extractHeader(inviteText, "CSeq") ?: return

        val sb = StringBuilder()
        sb.append("SIP/2.0 180 Ringing\r\n")
        if (viaHeaders.isNotEmpty()) {
            for (vh in viaHeaders) {
                sb.append("Via: $vh\r\n")
            }
        } else {
            val via = extractHeader(inviteText, "Via") ?: return
            sb.append("Via: $via\r\n")
        }
        sb.append("From: $from\r\n")
        sb.append("To: $to;tag=$toTag\r\n")
        sb.append("Call-ID: $callId\r\n")
        sb.append("CSeq: $cseq\r\n")
        sb.append("User-Agent: VerimorBulutSantralMobile/2.0\r\n")
        sb.append("Content-Length: 0\r\n\r\n")

        sendSipPacket(sb.toString(), address, port)
    }

    private fun sendOkResponse(requestText: String, address: InetAddress, port: Int) {
        val via = extractHeader(requestText, "Via") ?: return
        val from = extractHeader(requestText, "From") ?: return
        val to = extractHeader(requestText, "To") ?: return
        val callId = extractHeader(requestText, "Call-ID") ?: return
        val cseq = extractHeader(requestText, "CSeq") ?: return

        val response = "SIP/2.0 200 OK\r\n" +
                "Via: $via\r\n" +
                "From: $from\r\n" +
                "To: $to\r\n" +
                "Call-ID: $callId\r\n" +
                "CSeq: $cseq\r\n" +
                "User-Agent: VerimorBulutSantralMobile/2.0\r\n" +
                "Content-Length: 0\r\n\r\n"

        sendSipPacket(response, address, port)
    }

    private fun buildSdpAnswer(localIp: String, localRtpPort: Int, preferredCodec: Int = 8): String {
        return "v=0\r\n" +
                "o=- ${System.currentTimeMillis()} 1 IN IP4 $localIp\r\n" +
                "s=VerimorCall\r\n" +
                "c=IN IP4 $localIp\r\n" +
                "t=0 0\r\n" +
                "m=audio $localRtpPort RTP/AVP 0 8 101\r\n" +
                "a=rtpmap:0 PCMU/8000\r\n" +
                "a=rtpmap:8 PCMA/8000\r\n" +
                "a=rtpmap:101 telephone-event/8000\r\n" +
                "a=fmtp:101 0-16\r\n" +
                "a=sendrecv\r\n" +
                "a=ptime:20\r\n"
    }

    private fun buildRegisterMessage(
        domain: String,
        extension: String,
        localPort: Int,
        callId: String,
        fromTag: String,
        cseq: Int,
        authHeader: String?
    ): String {
        val branch = "z9hG4bK" + UUID.randomUUID().toString().replace("-", "").take(12)

        val sb = StringBuilder()
        sb.append("REGISTER sip:$domain SIP/2.0\r\n")
        sb.append("Via: SIP/2.0/UDP 0.0.0.0:$localPort;branch=$branch;rport\r\n")
        sb.append("Max-Forwards: 70\r\n")
        sb.append("From: <sip:$extension@$domain>;tag=$fromTag\r\n")
        sb.append("To: <sip:$extension@$domain>\r\n")
        sb.append("Call-ID: $callId\r\n")
        sb.append("CSeq: $cseq REGISTER\r\n")
        sb.append("Contact: <sip:$extension@0.0.0.0:$localPort>\r\n")
        sb.append("Expires: 300\r\n")
        sb.append("Allow: INVITE, ACK, CANCEL, OPTIONS, BYE, REFER, NOTIFY, MESSAGE, SUBSCRIBE, INFO\r\n")
        sb.append("User-Agent: VerimorBulutSantralMobile/2.0\r\n")
        if (authHeader != null) {
            sb.append(authHeader).append("\r\n")
        }
        sb.append("Content-Length: 0\r\n\r\n")

        return sb.toString()
    }

    private fun md5(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun extractHeader(sipText: String, headerName: String): String? {
        val prefix = "$headerName:"
        val shortPrefix = when (headerName.lowercase()) {
            "via" -> "v:"
            "from" -> "f:"
            "to" -> "t:"
            "call-id" -> "i:"
            "contact" -> "m:"
            "content-type" -> "c:"
            "content-length" -> "l:"
            else -> null
        }
        return sipText.lines()
            .firstOrNull { line ->
                val trimmed = line.trim()
                trimmed.startsWith(prefix, ignoreCase = true) ||
                        (shortPrefix != null && trimmed.startsWith(shortPrefix, ignoreCase = true))
            }
            ?.substringAfter(":")
            ?.trim()
    }

    private fun extractAuthParam(headerValue: String, paramName: String): String? {
        val regexQuoted = Regex("""(?i)\b$paramName\s*=\s*"([^"]*)"""")
        regexQuoted.find(headerValue)?.groupValues?.getOrNull(1)?.let { return it }

        val regexUnquoted = Regex("""(?i)\b$paramName\s*=\s*([^,\s]+)""")
        return regexUnquoted.find(headerValue)?.groupValues?.getOrNull(1)?.trim()
    }

    private fun extractCallerNumber(fromHeader: String, fullSipText: String): String {
        val paidHeader = extractHeader(fullSipText, "P-Asserted-Identity")
            ?: extractHeader(fullSipText, "Remote-Party-ID")
        val target = paidHeader ?: fromHeader

        val uriRegex = Regex("sip:([^@>]+)@")
        val match = uriRegex.find(target)
        if (match != null) {
            val userPart = match.groupValues[1]
            val digits = userPart.replace(Regex("\\D"), "")
            return if (digits.length >= 3) digits else userPart
        }
        return "1006"
    }

    private fun extractCallerName(fromHeader: String, fullSipText: String): String {
        val paidHeader = extractHeader(fullSipText, "P-Asserted-Identity")
            ?: extractHeader(fullSipText, "Remote-Party-ID")
        val target = paidHeader ?: fromHeader

        val nameRegex = Regex("\"([^\"]+)\"")
        val match = nameRegex.find(target)
        return match?.groupValues?.getOrNull(1)?.trim() ?: ""
    }

    private fun extractSdpConnectionIp(sipText: String): String? {
        val regex = Regex("""(?m)^c=IN\s+IP4\s+(\S+)""")
        return regex.find(sipText)?.groupValues?.getOrNull(1)?.trim()
    }

    private fun extractSdpAudioPort(sipText: String): Int? {
        val regex = Regex("""(?m)^m=audio\s+(\d+)""")
        return regex.find(sipText)?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    private fun extractAllHeaders(sipText: String, headerName: String): List<String> {
        val prefix = "$headerName:"
        val shortPrefix = when (headerName.lowercase()) {
            "via" -> "v:"
            "from" -> "f:"
            "to" -> "t:"
            "call-id" -> "i:"
            "contact" -> "m:"
            "content-type" -> "c:"
            "content-length" -> "l:"
            else -> null
        }
        return sipText.lines()
            .filter { line ->
                val trimmed = line.trim()
                trimmed.startsWith(prefix, ignoreCase = true) ||
                        (shortPrefix != null && trimmed.startsWith(shortPrefix, ignoreCase = true))
            }
            .map { it.substringAfter(":").trim() }
    }

    private fun extractPreferredCodec(sipText: String): Int {
        val audioLine = Regex("""(?m)^m=audio\s+\d+\s+RTP/AVP\s+(.*)""").find(sipText)?.groupValues?.getOrNull(1)
        if (audioLine != null) {
            val pts = audioLine.split("\\s+".toRegex()).mapNotNull { it.toIntOrNull() }
            if (8 in pts) return 8 // PCMA A-law
            if (0 in pts) return 0 // PCMU µ-law
        }
        return 8
    }

    private fun getLocalIpAddress(targetAddress: InetAddress? = null): String {
        try {
            if (targetAddress != null && !targetAddress.isLoopbackAddress) {
                val tempSocket = DatagramSocket()
                tempSocket.connect(targetAddress, 5060)
                val routeAddr = tempSocket.localAddress?.hostAddress
                tempSocket.close()
                if (!routeAddr.isNullOrBlank() && routeAddr != "0.0.0.0" && routeAddr != "127.0.0.1") {
                    return routeAddr
                }
            }
        } catch (_: Exception) {}

        try {
            val bound = synchronized(socketLock) { socket?.localAddress }
            if (bound != null && !bound.isLoopbackAddress && !bound.isAnyLocalAddress && bound is java.net.Inet4Address) {
                return bound.hostAddress ?: "127.0.0.1"
            }
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (addr is java.net.Inet4Address && !addr.isLoopbackAddress) {
                        return addr.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (_: Exception) {}
        return "127.0.0.1"
    }
}
