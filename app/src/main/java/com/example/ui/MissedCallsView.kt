package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MissedCallRecord
import com.example.ui.theme.*

@Composable
fun MissedCallsView(
    viewModel: SupportViewModel
) {
    val context = LocalContext.current
    val dutyModeEnabled by viewModel.dutyModeEnabled.collectAsState()
    val allMissedCalls by viewModel.allMissedCalls.collectAsState()
    val globalKullanici by viewModel.globalKullanici.collectAsState()
    val outlookRecipientEmail by viewModel.outlookRecipientEmail.collectAsState()

    // 1. Arama Türü Filtresi: ALL (Tümü), MISSED (Kaçanlar), ANSWERED (Görüşülenler)
    var selectedTypeFilter by remember { mutableStateOf("ALL") }

    // 2. Zaman Periyodu Filtresi: WEEK (Son 1 Hafta), MONTH (Son 1 Ay), ALL (Tümü)
    var selectedPeriod by remember { mutableStateOf("WEEK") }

    var callToDelete by remember { mutableStateOf<MissedCallRecord?>(null) }
    var showClearAllConfirm by remember { mutableStateOf(false) }

    val missedCount = remember(allMissedCalls) {
        allMissedCalls.count { it.callType == "MISSED" && !it.isHandled }
    }
    val answeredCount = remember(allMissedCalls) {
        allMissedCalls.count { it.callType != "MISSED" || it.isHandled }
    }

    // Filtrelenmiş çağrılar listesi
    val filteredCalls = remember(allMissedCalls, selectedTypeFilter, selectedPeriod) {
        val now = System.currentTimeMillis()
        val weekMillis = 7L * 24 * 3600 * 1000
        val monthMillis = 30L * 24 * 3600 * 1000

        // Tür filtresi
        val typeFiltered = when (selectedTypeFilter) {
            "MISSED" -> allMissedCalls.filter { it.callType == "MISSED" && !it.isHandled }
            "ANSWERED" -> allMissedCalls.filter { it.callType != "MISSED" || it.isHandled }
            else -> allMissedCalls
        }

        // Periyot filtresi
        when (selectedPeriod) {
            "WEEK" -> typeFiltered.filter { (now - it.timestamp) <= weekMillis }
            "MONTH" -> typeFiltered.filter { (now - it.timestamp) <= monthMillis }
            else -> typeFiltered
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(iOSBackground)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        // Nöbet / Mesai Dışı Modu Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (dutyModeEnabled) Color(0xFF0F2942) else iOSCardSecondary
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, if (dutyModeEnabled) iOSBlue else iOSBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (dutyModeEnabled) iOSBlue.copy(alpha = 0.2f) else iOSCardTertiary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (dutyModeEnabled) Icons.Default.Bedtime else Icons.Default.BedtimeOff,
                        contentDescription = null,
                        tint = if (dutyModeEnabled) iOSBlue else iOSTextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Nöbet / Mesai Dışı Modu",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = if (dutyModeEnabled) Color.White else iOSTextPrimary
                    )
                    Text(
                        text = if (dutyModeEnabled) {
                            "Aktif: Kaçan çağrılar izleniyor ve anlık bildirim veriliyor."
                        } else {
                            "Kapalı: Gündüz mesai modu (Pil ve veri tasarrufu aktif)."
                        },
                        fontSize = 11.5.sp,
                        color = if (dutyModeEnabled) iOSBlue else iOSTextSecondary
                    )
                }

                Switch(
                    checked = dutyModeEnabled,
                    onCheckedChange = { viewModel.setDutyMode(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = iOSBlue,
                        uncheckedThumbColor = iOSTextSecondary,
                        uncheckedTrackColor = iOSCardTertiary
                    )
                )
            }
        }

        // 1. Ana Çağrı Türü Filtre Butonları (Tümü, Kaçanlar, Görüşülenler)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Tümü Butonu
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (selectedTypeFilter == "ALL") iOSBlue else iOSCardSecondary,
                border = BorderStroke(0.5.dp, if (selectedTypeFilter == "ALL") iOSBlue else iOSBorder),
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedTypeFilter = "ALL" }
            ) {
                Text(
                    text = "Tümü (${allMissedCalls.size})",
                    fontSize = 12.sp,
                    fontWeight = if (selectedTypeFilter == "ALL") FontWeight.Bold else FontWeight.Medium,
                    color = if (selectedTypeFilter == "ALL") Color.White else iOSTextPrimary,
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 1
                )
            }

            // Kırmızı Kaçanlar Butonu
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (selectedTypeFilter == "MISSED") iOSRed else iOSCardSecondary,
                border = BorderStroke(0.5.dp, if (selectedTypeFilter == "MISSED") iOSRed else Color(0xFF7F1D1D).copy(alpha = 0.6f)),
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedTypeFilter = "MISSED" }
            ) {
                Text(
                    text = "🔴 Kaçan ($missedCount)",
                    fontSize = 12.sp,
                    fontWeight = if (selectedTypeFilter == "MISSED") FontWeight.Bold else FontWeight.Medium,
                    color = if (selectedTypeFilter == "MISSED") Color.White else iOSRed,
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 1
                )
            }

            // Beyaz Görüşülenler Butonu
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (selectedTypeFilter == "ANSWERED") Color(0xFF334155) else iOSCardSecondary,
                border = BorderStroke(0.5.dp, if (selectedTypeFilter == "ANSWERED") Color(0xFF64748B) else iOSBorder),
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedTypeFilter = "ANSWERED" }
            ) {
                Text(
                    text = "⚪ Görüşülen ($answeredCount)",
                    fontSize = 12.sp,
                    fontWeight = if (selectedTypeFilter == "ANSWERED") FontWeight.Bold else FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 1
                )
            }
        }

        // 2. Zaman Periyodu Filtreleme Çipleri
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val periods = listOf(
                "WEEK" to "Son 1 Hafta",
                "MONTH" to "Son 1 Ay",
                "ALL" to "Tüm Zamanlar"
            )

            periods.forEach { (key, label) ->
                val isSelected = selectedPeriod == key
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) Color(0xFF0F2942) else iOSCardTertiary,
                    border = BorderStroke(0.5.dp, if (isSelected) iOSBlue else iOSBorder),
                    modifier = Modifier.clickable { selectedPeriod = key }
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) iOSBlue else iOSTextSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (filteredCalls.isNotEmpty()) {
                TextButton(
                    onClick = { showClearAllConfirm = true },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("Tümünü Temizle", color = iOSRed, fontSize = 11.5.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Çağrı Listesi
        if (filteredCalls.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (selectedTypeFilter == "MISSED") Icons.Default.PhoneMissed else Icons.Default.PhoneCallback,
                        contentDescription = null,
                        tint = iOSTextSecondary.copy(alpha = 0.35f),
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = when (selectedTypeFilter) {
                            "MISSED" -> "Filtre dönemine ait kaçan çağrı bulunmuyor."
                            "ANSWERED" -> "Filtre dönemine ait görüşülen arama bulunmuyor."
                            else -> if (allMissedCalls.isEmpty()) "Henüz kayıtlı arama bulunmuyor." else "Filtreye uygun arama kaydı yok."
                        },
                        color = iOSTextSecondary,
                        fontSize = 13.5.sp
                    )
                    if (!dutyModeEnabled && selectedTypeFilter == "MISSED") {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Nöbet modunu açarak aramaları arka planda takip edebilirsiniz.",
                            color = iOSTextTertiary,
                            fontSize = 11.5.sp
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredCalls, key = { it.id }) { call ->
                    CallHistoryItemCard(
                        call = call,
                        onOutlookDraft = {
                            viewModel.markMissedCallHandled(call.id)
                            launchOutlookDraft(context, call, globalKullanici, outlookRecipientEmail)
                        },
                        onDirectCall = {
                            viewModel.markMissedCallHandled(call.id)
                            viewModel.startVerimorSipCall(call.callerNumber, call.callerName)
                        },
                        onLoadToForm = {
                            viewModel.loadPhoneToForm(call.callerNumber, call.callerName)
                        },
                        onDelete = { callToDelete = call }
                    )
                }
            }
        }
    }

    // Tekli Kayıt Silme Onayı
    if (callToDelete != null) {
        val target = callToDelete!!
        AlertDialog(
            onDismissRequest = { callToDelete = null },
            title = { Text("Arama Kaydını Sil", fontWeight = FontWeight.Bold) },
            text = { Text("${target.callerNumber} numaralı arama kaydı silinsin mi?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMissedCall(target.id)
                        callToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = iOSRed)
                ) {
                    Text("Sil", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { callToDelete = null }) {
                    Text("İptal")
                }
            }
        )
    }

    // Tümünü Temizle Onayı
    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            title = { Text("Tümünü Temizle", fontWeight = FontWeight.Bold) },
            text = { Text("Listelenen tüm arama geçmişi kayıtları silinsin mi?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllMissedCalls()
                        showClearAllConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = iOSRed)
                ) {
                    Text("Hepsini Sil", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirm = false }) {
                    Text("İptal")
                }
            }
        )
    }
}

/**
 * 2. Renk Kodlaması:
 * - Cevapsız (kaçan) aramalar: KIRMIZI
 * - Aranan veya görüşmesi yapılmış tüm aramalar: BEYAZ (Normal)
 * 3. Karta tıklandığında zorla form AÇILMAZ; sadece detayları genişletip daraltır.
 */
@Composable
fun CallHistoryItemCard(
    call: MissedCallRecord,
    onOutlookDraft: () -> Unit,
    onDirectCall: () -> Unit,
    onLoadToForm: () -> Unit,
    onDelete: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val isMissed = call.callType == "MISSED" && !call.isHandled

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }, // Tıklama zorla form açmaz, detay açıp kapatır
        colors = CardDefaults.cardColors(containerColor = iOSCardSecondary),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = if (isMissed) 1.dp else 0.5.dp,
            color = if (isMissed) Color(0xFF7F1D1D).copy(alpha = 0.7f) else iOSBorder
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    // İkon Yuvası (Kırmızı veya Beyaz/Normal)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isMissed) Color(0xFF450A0A) else iOSCardTertiary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isMissed -> Icons.Default.PhoneMissed
                                call.callType == "OUTGOING" -> Icons.Default.CallMade
                                else -> Icons.Default.CallReceived
                            },
                            contentDescription = if (isMissed) "Cevapsız Çağrı" else "Görüşülen Arama",
                            tint = if (isMissed) iOSRed else Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        // Numara (Her iki durumda da okunaklı beyaz)
                        Text(
                            text = call.callerNumber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = Color.White
                        )
                        if (call.callerName.isNotBlank()) {
                            Text(
                                text = call.callerName,
                                fontSize = 12.sp,
                                color = iOSTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Durum Etiketi: Kırmızı (Cevapsız) veya Beyaz/Normal (Görüşüldü / Giden)
                Surface(
                    color = if (isMissed) Color(0xFF2A1215) else iOSCardTertiary,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(
                        0.5.dp,
                        if (isMissed) iOSRed.copy(alpha = 0.6f) else iOSBorder
                    )
                ) {
                    val statusText = when {
                        isMissed -> call.reason.ifBlank { "Cevapsız" }
                        call.callType == "OUTGOING" -> "Giden Arama"
                        call.duration.isNotBlank() -> "${call.reason.ifBlank { "Gelen Çağrı" }} (${call.duration})"
                        else -> call.reason.ifBlank { "Görüşüldü" }
                    }
                    Text(
                        text = statusText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isMissed) iOSRed else iOSTextPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tarih & Aksiyon Butonları
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = call.dateFormatted,
                    fontSize = 11.sp,
                    color = if (isMissed) iOSRed.copy(alpha = 0.8f) else iOSTextTertiary
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Outlook Mail Taslağı Butonu
                    Button(
                        onClick = onOutlookDraft,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0078D4)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Outlook", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Doğrudan SIP ile Ara
                    IconButton(onClick = onDirectCall, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Call, contentDescription = "SIP ile Ara", tint = iOSBlue, modifier = Modifier.size(16.dp))
                    }

                    // Forma Aktar (Yalnızca kullanıcı açıkça tıklarsa ana form ekranına aktarır)
                    IconButton(onClick = onLoadToForm, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Assignment, contentDescription = "Forma Aktar", tint = iOSOrange, modifier = Modifier.size(16.dp))
                    }

                    // Sil Butonu
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Sil", tint = iOSRed.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Genişletilmiş Bilgiler (Detay İnceleme)
            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = iOSBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Tür: ${if (isMissed) "Cevapsız Arama" else if (call.callType == "OUTGOING") "Giden Çağrı" else "Yanıtlanan Çağrı"}",
                        fontSize = 11.sp,
                        color = iOSTextSecondary
                    )
                    if (call.duration.isNotBlank()) {
                        Text(
                            text = "Süre: ${call.duration}",
                            fontSize = 11.sp,
                            color = iOSTextSecondary
                        )
                    }
                }
            }
        }
    }
}

// 1. Outlook Intent ile tam konu ve içerik ayrımı:
// Alıcı (To): servis@enpos.com.tr (Kullanıcı ayarlarından değiştirilebilir)
// Konu (Subject): "[Arayan Numara] numarası tarafından arandınız" -> Parantezsiz: "05321234567 numarası tarafından arandınız"
// Gövde (Body): "05321234567 numarası tarafından gelen çağrıya geri dönüş sağlanmıştır.\n\n[Aktif Kullanıcı]"
private fun launchOutlookDraft(
    context: Context,
    call: MissedCallRecord,
    currentUserName: String,
    recipientEmail: String
) {
    val senderName = currentUserName.trim().ifBlank { "Destek Temsilcisi" }
    val toEmail = recipientEmail.trim().ifBlank { "servis@enpos.com.tr" }
    val cleanCaller = call.callerNumber.trim()
    val subject = "$cleanCaller numarası tarafından arandınız"
    val body = "$cleanCaller numarası tarafından gelen çağrıya geri dönüş sağlanmıştır.\n\n$senderName"

    val outlookIntent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:${Uri.encode(toEmail)}")
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
        setPackage("com.microsoft.office.outlook")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    try {
        context.startActivity(outlookIntent)
    } catch (_: Exception) {
        // Fallback: standard mail chooser
        try {
            val fallbackIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:${Uri.encode(toEmail)}")
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(fallbackIntent, "E-Posta Taslağı Aç"))
        } catch (_: Exception) {}
    }
}
