package com.example.sip

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PhoneForwarded
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Santral Tuş Takımı (DTMF Dialpad / Keypad).
 * RFC 2833 ve SIP INFO destekli DTMF sinyalleri gönderir.
 * *2202 gibi dahili aktarma dizilerini hızlıca göndermeyi sağlar.
 */
@Composable
fun CallDialpadView(
    onSendDtmf: (Char) -> Unit,
    onSendDtmfSequence: (String) -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = true
) {
    var dialedText by remember { mutableStateOf("") }

    val bgColor = if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cardBg = if (isDarkTheme) Color(0xFF1E293B) else Color.White
    val textColor = if (isDarkTheme) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
    val keyBg = if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("call_dialpad_view"),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF334155) else Color(0xFFCBD5E1))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // BAŞLIK VE HIZLI AKTARMA ÇİPİ (*2202)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PhoneInTalk,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TUŞ TAKIMI (DTMF)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) Color(0xFF10B981) else Color(0xFF047857)
                    )
                }

                // HIZLI AKTARMA KISAYOLU: *2202
                Button(
                    onClick = {
                        dialedText = "*2202"
                        onSendDtmfSequence("*2202")
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF059669),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp).testTag("dtmf_btn_quick_transfer_2202")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneForwarded,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Aktar: *2202", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // TUŞLANAN NUMARA EKRANI
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = if (isDarkTheme) Color(0xFF090D16) else Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (dialedText.isEmpty()) "Tuşlanan: —" else dialedText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (dialedText.isEmpty()) subTextColor else textColor,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )

                    if (dialedText.isNotEmpty()) {
                        IconButton(
                            onClick = { dialedText = dialedText.dropLast(1) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Sil",
                                tint = subTextColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = { dialedText = "" },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Temizle",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3x4 TELEFON TUŞLARI
            val keypadRows = listOf(
                listOf(KeypadButton("1", ""), KeypadButton("2", "ABC"), KeypadButton("3", "DEF")),
                listOf(KeypadButton("4", "GHI"), KeypadButton("5", "JKL"), KeypadButton("6", "MNO")),
                listOf(KeypadButton("7", "PQRS"), KeypadButton("8", "TUV"), KeypadButton("9", "WXYZ")),
                listOf(KeypadButton("*", ""), KeypadButton("0", "+"), KeypadButton("#", ""))
            )

            keypadRows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { btn ->
                        DialpadKey(
                            digit = btn.digit,
                            subtext = btn.letters,
                            bgColor = keyBg,
                            textColor = textColor,
                            subTextColor = subTextColor,
                            onClick = {
                                dialedText += btn.digit
                                onSendDtmf(btn.digit[0])
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // GÖNDER / AKTAR BUTONU
            if (dialedText.isNotEmpty()) {
                Button(
                    onClick = {
                        onSendDtmfSequence(dialedText)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .testTag("dtmf_btn_send_sequence"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneForwarded,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Santrale Gönder: $dialedText",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private data class KeypadButton(val digit: String, val letters: String)

@Composable
private fun DialpadKey(
    digit: String,
    subtext: String,
    bgColor: Color,
    textColor: Color,
    subTextColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = bgColor,
        modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag("dialpad_key_$digit")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = digit,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                lineHeight = 22.sp
            )
            if (subtext.isNotBlank()) {
                Text(
                    text = subtext,
                    fontSize = 7.5.sp,
                    color = subTextColor,
                    lineHeight = 9.sp
                )
            }
        }
    }
}

/**
 * Pop-up Çağrı Kontrol ve Tuş Takımı Ekranı (In-App Call Control & DTMF Keypad).
 * Aktif çağrı sırasında "Görüşmede" durumuna veya arama çubuğundaki tuş takımı ikonuna
 * tıklandığında açılır.
 */
@Composable
fun CallControlKeypadDialog(
    state: VerimorSipState,
    callDuration: Long,
    isMuted: Boolean,
    isSpeaker: Boolean,
    isStreaming: Boolean,
    onDismiss: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onHangup: () -> Unit,
    onSendDtmf: (Char) -> Unit,
    onSendDtmfSequence: (String) -> Unit,
    onOpenFullScreen: () -> Unit
) {
    val callerNum = when (state) {
        is VerimorSipState.Ringing -> state.callerNumber
        is VerimorSipState.InCall -> state.callerNumber
        else -> "Santral Çağrısı"
    }
    val m = callDuration / 60
    val s = callDuration % 60
    val durationText = "%02d:%02d".format(m, s)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Başlık Satırı (Arayan & Tam Ekran & Kapat Butonları)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFDCFCE7),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.PhoneInTalk,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Görüşmede: $callerNum",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Süre: $durationText" + if (isStreaming) " • RTP Ses Aktif" else "",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = onOpenFullScreen, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.OpenInFull,
                                contentDescription = "Tam Ekran",
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Kapat",
                                tint = Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Ses ve Çağrı Butonları (Mute, Speaker, Bitir) - Rahat Tıklanabilir Geniş Satır
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onToggleMute,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isMuted) Color(0xFFFFE4E6) else Color.Transparent
                        ),
                        border = BorderStroke(1.dp, if (isMuted) Color(0xFFDC2626) else Color(0xFFCBD5E1)),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = null,
                                tint = if (isMuted) Color(0xFFDC2626) else Color(0xFF334155),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isMuted) "Sessiz" else "Mikrofon",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onToggleSpeaker,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSpeaker) Color(0xFFDBEAFE) else Color.Transparent
                        ),
                        border = BorderStroke(1.dp, if (isSpeaker) Color(0xFF2563EB) else Color(0xFFCBD5E1)),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(
                                imageVector = if (isSpeaker) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                contentDescription = null,
                                tint = if (isSpeaker) Color(0xFF2563EB) else Color(0xFF334155),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isSpeaker) "Hoparlör" else "Ahize",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }

                    Button(
                        onClick = onHangup,
                        modifier = Modifier.weight(1.15f).height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(Icons.Default.CallEnd, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Bitir", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // DTMF Tuş Takımı
                CallDialpadView(
                    onSendDtmf = onSendDtmf,
                    onSendDtmfSequence = onSendDtmfSequence,
                    isDarkTheme = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

