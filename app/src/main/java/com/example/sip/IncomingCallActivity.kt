package com.example.sip

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainActivity
import com.example.ui.theme.MyApplicationTheme

/**
 * Full-Screen Gelen Çağrı ve Ses Kontrol Ekranı (IncomingCallActivity).
 * Santralden gelen INVITE çağrılarında ekranı açar, kilit ekranının üzerine çıkar,
 * arayan numarayı (örn: 1006) gösterir, Cevapla (Yeşil), Reddet (Kırmızı),
 * Mute ve Hoparlör kontrollerini sunar.
 */
class IncomingCallActivity : ComponentActivity() {

    private lateinit var sipManager: VerimorSipManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sipManager = VerimorSipManager.getInstance(applicationContext)

        configureLockScreenWindow()

        setContent {
            MyApplicationTheme {
                val connectionState by sipManager.connectionState.collectAsState()
                val isMuted by sipManager.isMuted.collectAsState()
                val isSpeaker by sipManager.isSpeaker.collectAsState()
                val isStreaming by sipManager.isAudioStreaming.collectAsState()

                // Çağrı cevaplandığında (InCall) veya kapandığında aktiviteyi kapat ve doğrudan ana forma dön
                LaunchedEffect(connectionState) {
                    if (connectionState is VerimorSipState.InCall || connectionState is VerimorSipState.Registered || connectionState is VerimorSipState.Disconnected || connectionState is VerimorSipState.Error) {
                        finish()
                    }
                }

                IncomingCallScreenContent(
                    state = connectionState,
                    isMuted = isMuted,
                    isSpeaker = isSpeaker,
                    isStreaming = isStreaming,
                    onAnswer = {
                        sipManager.answerCall()
                        finish()
                    },
                    onDecline = {
                        sipManager.declineCall()
                        finish()
                    },
                    onHangup = {
                        sipManager.hangupCall()
                        finish()
                    },
                    onToggleMute = { sipManager.toggleMute() },
                    onToggleSpeaker = { sipManager.toggleSpeaker() },
                    onSendDtmf = { sipManager.sendDtmf(it) },
                    onSendDtmfSequence = { sipManager.sendDtmfSequence(it) },
                    onOpenForm = {
                        val mainIntent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                        startActivity(mainIntent)
                    }
                )
            }
        }
    }

    private fun configureLockScreenWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }
}

@Composable
fun IncomingCallScreenContent(
    state: VerimorSipState,
    isMuted: Boolean,
    isSpeaker: Boolean,
    isStreaming: Boolean,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
    onHangup: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onSendDtmf: (Char) -> Unit,
    onSendDtmfSequence: (String) -> Unit,
    onOpenForm: () -> Unit
) {
    val callerNumber = when (state) {
        is VerimorSipState.Ringing -> state.callerNumber
        is VerimorSipState.InCall -> state.callerNumber
        else -> "1006"
    }

    val callerName = when (state) {
        is VerimorSipState.Ringing -> state.callerName.ifBlank { "Verimor Santral Arayanı" }
        is VerimorSipState.InCall -> "Görüşme Sürüyor"
        else -> "Bilinmeyen Numara"
    }

    val isInCall = state is VerimorSipState.InCall
    val durationSeconds = if (state is VerimorSipState.InCall) state.durationSeconds else 0L
    var showKeypad by remember { mutableStateOf(false) }

    // Çağrı çalma animasyonu (Pulsing ring)
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF090D16),
                        Color(0xFF0F172A),
                        Color(0xFF1E293B)
                    )
                )
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ÜST BÖLÜM: Santral Bilgisi ve Form Kısayolu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFF0F2942),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0A84FF).copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneInTalk,
                            contentDescription = null,
                            tint = Color(0xFF0A84FF),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "VERİMOR SIP",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0A84FF)
                        )
                    }
                }

                if (isInCall) {
                    Button(
                        onClick = onOpenForm,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("call_btn_open_form")
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Destek Formuna Dön", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ORTA BÖLÜM: Arayan Bilgisi veya Tuş Takımı (DTMF)
            if (isInCall && showKeypad) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp)
                ) {
                    val minutes = durationSeconds / 60
                    val secs = durationSeconds % 60
                    Text(
                        text = "Aktif Görüşme: $callerNumber • %02d:%02d".format(minutes, secs),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CallDialpadView(
                        onSendDtmf = onSendDtmf,
                        onSendDtmfSequence = onSendDtmfSequence,
                        isDarkTheme = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 24.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(140.dp)
                    ) {
                        // Dış titreşim halkası (Gelen arama esnasında)
                        if (!isInCall) {
                            Box(
                                modifier = Modifier
                                    .size(130.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(Color(0x3310B981))
                            )
                        }

                        // Ana Avatar Dairesi
                        Surface(
                            shape = CircleShape,
                            color = if (isInCall) Color(0xFF1E3A8A) else Color(0xFF0F766E),
                            modifier = Modifier
                                .size(100.dp)
                                .border(3.dp, if (isInCall) Color(0xFF38BDF8) else Color(0xFF2DD4BF), CircleShape),
                            shadowElevation = 8.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isInCall) Icons.Default.PhoneInTalk else Icons.Default.Call,
                                    contentDescription = "Arayan",
                                    tint = Color.White,
                                    modifier = Modifier.size(46.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = callerNumber,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = callerName,
                        fontSize = 16.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (isInCall) {
                        val minutes = durationSeconds / 60
                        val secs = durationSeconds % 60
                        val durationStr = "%02d:%02d".format(minutes, secs)

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0x3338BDF8),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x6638BDF8))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Görüşme Sürüyor • $durationStr",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8)
                                )
                                if (isStreaming) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "(RTP Ses Aktif)",
                                        fontSize = 11.sp,
                                        color = Color(0xFF4ADE80)
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Gelen Santral Çağrısı Çalıyor...",
                            fontSize = 13.sp,
                            color = Color(0xFF2DD4BF),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // ALT BÖLÜM: Butonlar ve Kontroller
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Görüşme sırasındaki Hızlı Ses Kontrolleri (Hoparlör & Mute & Tuş Takımı)
                if (isInCall) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .padding(bottom = 20.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // SESSİZE AL (MUTE) BUTONU
                        CallActionButton(
                            icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            label = if (isMuted) "Mikrofon Kapalı" else "Sessize Al",
                            isActive = isMuted,
                            activeColor = Color(0xFFEF4444),
                            inactiveColor = Color(0xFF334155),
                            onClick = onToggleMute,
                            testTag = "call_btn_mute"
                        )

                        // TUŞ TAKIMI (KEYPAD / DTMF) BUTONU
                        CallActionButton(
                            icon = Icons.Default.Dialpad,
                            label = if (showKeypad) "Klavyeyi Kapat" else "Tuş Takımı",
                            isActive = showKeypad,
                            activeColor = Color(0xFF0A84FF),
                            inactiveColor = Color(0xFF334155),
                            onClick = { showKeypad = !showKeypad },
                            testTag = "call_btn_keypad"
                        )

                        // HOPARLÖR BUTONU
                        CallActionButton(
                            icon = if (isSpeaker) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            label = if (isSpeaker) "Hoparlör Açık" else "Hoparlör",
                            isActive = isSpeaker,
                            activeColor = Color(0xFF0A84FF),
                            inactiveColor = Color(0xFF334155),
                            onClick = onToggleSpeaker,
                            testTag = "call_btn_speaker"
                        )
                    }
                }

                // ANA ÇAĞRI BUTONLARI (CEVAPLA / REDDET / SONLANDIR)
                if (!isInCall) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // REDDET (Kırmızı)
                        CallCircularButton(
                            icon = Icons.Default.CallEnd,
                            label = "Reddet",
                            bgColor = Color(0xFFDC2626),
                            onClick = onDecline,
                            testTag = "call_btn_decline"
                        )

                        // CEVAPLA (iOS Mavi)
                        CallCircularButton(
                            icon = Icons.Default.Call,
                            label = "Cevapla",
                            bgColor = Color(0xFF0A84FF),
                            onClick = onAnswer,
                            testTag = "call_btn_answer"
                        )
                    }
                } else {
                    // ARAMAYI SONLANDIR (Büyük Kırmızı Buton)
                    CallCircularButton(
                        icon = Icons.Default.CallEnd,
                        label = "Aramayı Sonlandır",
                        bgColor = Color(0xFFDC2626),
                        onClick = onHangup,
                        testTag = "call_btn_hangup"
                    )
                }
            }
        }
    }
}

@Composable
fun CallCircularButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    bgColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.testTag(testTag)
    ) {
        Surface(
            shape = CircleShape,
            color = bgColor,
            shadowElevation = 8.dp,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape),
            onClick = onClick
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun CallActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.testTag(testTag)
    ) {
        Surface(
            shape = CircleShape,
            color = if (isActive) activeColor else inactiveColor,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape),
            onClick = onClick
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.5.sp,
            color = if (isActive) Color.White else Color(0xFF94A3B8)
        )
    }
}
