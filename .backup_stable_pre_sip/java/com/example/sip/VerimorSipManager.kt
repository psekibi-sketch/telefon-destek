package com.example.sip

import android.content.Context
import android.util.Log
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
import java.security.MessageDigest
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
 * Verimor Bulut Santral SIP İletişim Motoru
 * RFC 3261 SIP Kayıt, RFC 2617 MD5 Digest Authentication (qop destekli),
 * UDP Soket Dinleme, Gelen INVITE Yakalama ve Çağrı Otomasyonu.
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

    private val _connectionState = MutableStateFlow<VerimorSipState>(VerimorSipState.Disconnected)
    val connectionState: StateFlow<VerimorSipState> = _connectionState.asStateFlow()

    private val _callEvents = MutableSharedFlow<VerimorCallEvent>(
        replay = 0,
        extraBufferCapacity = 20,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val callEvents: SharedFlow<VerimorCallEvent> = _callEvents.asSharedFlow()

    private val socketLock = Any()
    private var socket: DatagramSocket? = null

    private var regTimeoutJob: Job? = null
    private var authTimeoutJob: Job? = null
    private var listenJob: Job? = null
    private var keepAliveJob: Job? = null
    private var autoReconnectJob: Job? = null

    private val cseqCounter = AtomicInteger(1)
    private var currentConfig: VerimorSipConfig? = null
    private var isUserExplicitlyDisconnected = false

    // Dialog kimlikleri (RFC 3261 uyarınca challenge boyunca aynı kalmalıdır)
    private var activeCallId: String = ""
    private var activeFromTag: String = ""
    private var authAttempts = 0

    /**
     * Santrale SIP REGISTER ile kayıt ol
     */
    fun register(config: VerimorSipConfig) {
        isUserExplicitlyDisconnected = false
        currentConfig = config
        authAttempts = 0

        autoReconnectJob?.cancel()
        autoReconnectJob = null

        val cleanDomain = config.domain.trim()
            .removePrefix("http://")
            .removePrefix("https://")
            .removePrefix("sip:")
            .removeSuffix("/")
        val cleanExt = config.extension.trim()
        val cleanPass = config.password.trim()

        if (cleanDomain.isBlank() || cleanExt.isBlank()) {
            _connectionState.value = VerimorSipState.Error("Santral Domain veya Dahili Numarası boş olamaz.")
            return
        }

        cleanupNetworkJobs()

        _connectionState.value = VerimorSipState.Connecting
        Log.d(TAG, "Starting SIP register for $cleanExt@$cleanDomain:${config.port}")

        // Arka plan servisinin çalıştırılması
        try {
            VerimorSipService.startService(appContext)
        } catch (e: Exception) {
            Log.w(TAG, "Service start: ${e.message}")
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

                // Dialog için sabit Call-ID ve From Tag üret (Challenge süresince korunur)
                activeCallId = UUID.randomUUID().toString() + "@" + (localSocket.localAddress?.hostAddress ?: "127.0.0.1")
                activeFromTag = UUID.randomUUID().toString().replace("-", "").take(8)
                cseqCounter.set(1)

                // Gelen paket dinleyicisini başlat
                startListening(serverAddress, config.port, cleanExt, cleanDomain, cleanPass)

                // İlk REGISTER mesajını gönder (challenge isteği başlatır)
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
    fun simulateIncomingCall(phone: String, name: String = "Test Müşterisi") {
        val cleanPhone = phone.replace(Regex("\\s+"), "").trim()
        val event = VerimorCallEvent(
            callerNumber = cleanPhone,
            callerName = name,
            isSimulated = true
        )
        _connectionState.value = VerimorSipState.Ringing(cleanPhone, name)
        managerScope.launch {
            _callEvents.emit(event)
        }
    }

    /**
     * Gelen çağrıyı yanıtla
     */
    fun answerCall() {
        val current = _connectionState.value
        if (current is VerimorSipState.Ringing) {
            _connectionState.value = VerimorSipState.InCall(current.callerNumber, 0)
        }
    }

    /**
     * Çağrıyı sonlandır
     */
    fun hangupCall() {
        val cfg = currentConfig
        if (cfg != null && _connectionState.value !is VerimorSipState.Error) {
            _connectionState.value = VerimorSipState.Registered(cfg.extension, cfg.domain)
        } else {
            _connectionState.value = VerimorSipState.Disconnected
        }
    }

    private fun cleanupNetworkJobs() {
        regTimeoutJob?.cancel()
        regTimeoutJob = null
        authTimeoutJob?.cancel()
        authTimeoutJob = null
        listenJob?.cancel()
        listenJob = null
        keepAliveJob?.cancel()
        keepAliveJob = null

        synchronized(socketLock) {
            try {
                socket?.close()
            } catch (_: Exception) {}
            socket = null
        }
    }

    private fun scheduleAutoReconnectIfAllowed() {
        if (isUserExplicitlyDisconnected) return
        val cfg = currentConfig ?: return

        autoReconnectJob?.cancel()
        autoReconnectJob = managerScope.launch {
            delay(12000L) // 12 saniye sonra otomatik tekrar dene
            if (!isUserExplicitlyDisconnected && _connectionState.value is VerimorSipState.Error) {
                Log.i(TAG, "Attempting auto-reconnect to Verimor SIP...")
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
        listenJob = managerScope.launch(Dispatchers.IO) {
            val buffer = ByteArray(4096)
            while (isActive) {
                val currentSocket = synchronized(socketLock) { socket } ?: break
                if (currentSocket.isClosed) break

                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    currentSocket.receive(packet)

                    val response = String(packet.data, 0, packet.length, Charsets.UTF_8)
                    Log.d(TAG, "SIP RX (${packet.length} bytes):\n$response")

                    handleIncomingSipPacket(response, serverAddress, serverPort, extension, domain, password)

                } catch (e: Exception) {
                    if (isActive && !currentSocket.isClosed) {
                        Log.e(TAG, "Socket receive error: ${e.message}")
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
        extension: String,
        domain: String,
        password: String
    ) {
        val firstLine = sipText.lines().firstOrNull()?.trim() ?: return

        when {
            // 200 OK (Kayıt başarılı)
            firstLine.startsWith("SIP/2.0 200 OK") -> {
                regTimeoutJob?.cancel()
                authTimeoutJob?.cancel()
                authAttempts = 0

                val cseqHeader = extractHeader(sipText, "CSeq") ?: ""
                if (cseqHeader.contains("REGISTER", ignoreCase = true)) {
                    Log.i(TAG, "✓ Verimor SIP 200 OK Kayıt Başarılı! Dahili: $extension@$domain")
                    _connectionState.value = VerimorSipState.Registered(extension, domain)
                    startKeepAlive(serverAddress, serverPort, extension, domain)
                }
            }

            // 401 Unauthorized veya 407 Proxy Authentication Required
            firstLine.startsWith("SIP/2.0 401") || firstLine.startsWith("SIP/2.0 407") -> {
                regTimeoutJob?.cancel()
                authAttempts++

                if (authAttempts > 2) {
                    // İkinci denemede de 401/407 geliyorsa şifre veya kullanıcı adı kesinlikle yanlıştır
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

                    // 15 saniyelik kimlik doğrulama zaman aşımı
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
                val fromHeader = extractHeader(sipText, "From") ?: ""
                val callerNumber = extractCallerNumber(fromHeader, sipText)
                val callerName = extractCallerName(fromHeader, sipText)

                Log.i(TAG, "Identified Caller: $callerNumber ($callerName)")

                // Santrale derhal 180 Ringing cevabı ver
                sendRingingResponse(sipText, serverAddress, serverPort)

                _connectionState.value = VerimorSipState.Ringing(callerNumber, callerName)
                _callEvents.emit(VerimorCallEvent(callerNumber, callerName))
            }

            // Çağrı Sonlandı (BYE veya CANCEL)
            firstLine.startsWith("BYE") || firstLine.startsWith("CANCEL") -> {
                sendOkResponse(sipText, serverAddress, serverPort)
                hangupCall()
            }

            // Santral OPTIONS ping kontrolü
            firstLine.startsWith("OPTIONS") -> {
                sendOkResponse(sipText, serverAddress, serverPort)
            }
        }
    }

    /**
     * RFC 2617 / RFC 3261 uyumlu Digest Authentication Header oluşturucu
     * qop="auth", MD5, realm ve nonce parametrilerini tam destekler.
     */
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
                delay(KEEP_ALIVE_INTERVAL_MS)
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

    private fun sendRingingResponse(inviteText: String, address: InetAddress, port: Int) {
        val via = extractHeader(inviteText, "Via") ?: return
        val from = extractHeader(inviteText, "From") ?: return
        val to = extractHeader(inviteText, "To") ?: return
        val callId = extractHeader(inviteText, "Call-ID") ?: return
        val cseq = extractHeader(inviteText, "CSeq") ?: return

        val response = "SIP/2.0 180 Ringing\r\n" +
                "Via: $via\r\n" +
                "From: $from\r\n" +
                "To: $to;tag=vm${System.currentTimeMillis()}\r\n" +
                "Call-ID: $callId\r\n" +
                "CSeq: $cseq\r\n" +
                "User-Agent: VerimorBulutSantralMobile/2.0\r\n" +
                "Content-Length: 0\r\n\r\n"

        sendSipPacket(response, address, port)
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
        return sipText.lines()
            .firstOrNull { it.trim().startsWith(prefix, ignoreCase = true) }
            ?.substringAfter(":")
            ?.trim()
    }

    private fun extractAuthParam(headerValue: String, paramName: String): String? {
        // Parametre tırnaklı: param="value"
        val regexQuoted = Regex("""(?i)\b$paramName\s*=\s*"([^"]*)"""")
        regexQuoted.find(headerValue)?.groupValues?.getOrNull(1)?.let { return it }

        // Parametre tırnaksız: param=value
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
            return if (digits.length >= 7) digits else userPart
        }
        return "05XXXXXXXXX"
    }

    private fun extractCallerName(fromHeader: String, fullSipText: String): String {
        val paidHeader = extractHeader(fullSipText, "P-Asserted-Identity")
            ?: extractHeader(fullSipText, "Remote-Party-ID")
        val target = paidHeader ?: fromHeader

        val nameRegex = Regex("\"([^\"]+)\"")
        val match = nameRegex.find(target)
        return match?.groupValues?.getOrNull(1)?.trim() ?: ""
    }
}
