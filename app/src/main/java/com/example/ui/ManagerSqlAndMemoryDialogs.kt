package com.example.ui

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * SQL Server / Arka Ofis'ten Tarih Bazlı Veri Çekme Seçenekleri Penceresi
 */
@Composable
fun SqlFetchOptionsDialog(
    viewModel: SupportViewModel,
    defaultStartDate: String,
    defaultEndDate: String,
    onDismiss: () -> Unit
) {
    var selectedPreset by remember { mutableStateOf("TODAY") } // TODAY, YESTERDAY_TODAY, THIS_WEEK, THIS_MONTH, PANEL, CUSTOM, ALL
    var customStartDate by remember { mutableStateOf(defaultStartDate) }
    var customEndDate by remember { mutableStateOf(defaultEndDate) }
    var activeDatePicker by remember { mutableStateOf<String?>(null) } // "start" or "end"
    var userCodeFilter by remember { mutableStateOf("") }
    var selectedLimit by remember { mutableStateOf(1000) }
    var showServerCodeHelp by remember { mutableStateOf(false) }
    val isFetchingFromSql by viewModel.isFetchingFromSql.collectAsState()

    val sdfYmd = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    // Tarih aralığını belirle
    val calculatedRange = remember(selectedPreset, customStartDate, customEndDate, defaultStartDate, defaultEndDate) {
        val cal = Calendar.getInstance()
        val todayStr = sdfYmd.format(cal.time)

        when (selectedPreset) {
            "TODAY" -> Pair(todayStr, todayStr)
            "YESTERDAY_TODAY" -> {
                cal.add(Calendar.DAY_OF_YEAR, -1)
                Pair(sdfYmd.format(cal.time), todayStr)
            }
            "THIS_WEEK" -> {
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                Pair(sdfYmd.format(cal.time), todayStr)
            }
            "THIS_MONTH" -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                Pair(sdfYmd.format(cal.time), todayStr)
            }
            "PANEL" -> Pair(defaultStartDate, defaultEndDate)
            "CUSTOM" -> Pair(customStartDate, customEndDate)
            "ALL" -> Pair("", "") // Boş = tümü
            else -> Pair(todayStr, todayStr)
        }
    }

    if (activeDatePicker != null) {
        AppComposeDatePickerDialog(
            initialDateYmd = if (activeDatePicker == "start") customStartDate else customEndDate,
            title = if (activeDatePicker == "start") "Başlangıç Tarihi Seç" else "Bitiş Tarihi Seç",
            onDateSelected = { selected ->
                if (activeDatePicker == "start") customStartDate = selected else customEndDate = selected
                selectedPreset = "CUSTOM"
                activeDatePicker = null
            },
            onDismiss = { activeDatePicker = null }
        )
    }

    if (showServerCodeHelp) {
        ServerApiCodeHelpDialog(onDismiss = { showServerCodeHelp = false })
    }

    Dialog(
        onDismissRequest = { if (!isFetchingFromSql) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(0.96f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(18.dp))
                .testTag("sql_fetch_options_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1E3A8A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "SQL'den Veri Çek (Tarih Bazlı)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Hafıza şişmesini önlemek için periyot seçin",
                                fontSize = 11.sp,
                                color = iOSTextSecondary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, enabled = !isFetchingFromSql) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat", tint = iOSTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bilgilendirme Kutusu
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF1D4ED8), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tüm veritabanını indirmek yerine sadece incelemek istediğiniz tarih aralığını çekmeniz internet trafiğini ve telefon hafızasını korur.",
                            fontSize = 11.sp,
                            color = Color(0xFF1E40AF),
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "1. ÇEKİLECEK TARİH ARALIĞI",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = iOSBlue,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Hızlı Seçim Butonları
                val presets = listOf(
                    "TODAY" to "🟢 Sadece Bugün",
                    "YESTERDAY_TODAY" to "🔵 Dün ve Bugün",
                    "THIS_WEEK" to "🟡 Bu Hafta",
                    "THIS_MONTH" to "🟣 Bu Ay",
                    "PANEL" to "📅 Panel Periyodu",
                    "CUSTOM" to "🗓️ Özel Tarih",
                    "ALL" to "⚠️ Tümü (Tüm Kayıtlar)"
                )

                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    presets.chunked(2).forEach { rowPresets ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            rowPresets.forEach { (key, label) ->
                                val isSelected = selectedPreset == key
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFF1E3A8A) else iOSCardSecondary,
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF60A5FA) else iOSBorder),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            selectedPreset = key
                                            if (key == "CUSTOM") activeDatePicker = "start"
                                        }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)
                                    )
                                }
                            }
                            if (rowPresets.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // Özel Tarih Seçim Kartları
                if (selectedPreset == "CUSTOM" || selectedPreset == "PANEL") {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Başlangıç
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = iOSCardSecondary,
                            border = BorderStroke(0.8.dp, iOSBlue),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { activeDatePicker = "start" }
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("BAŞLANGIÇ", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = iOSBlue)
                                Text(calculatedRange.first.ifBlank { "Seçiniz" }, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        // Bitiş
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = iOSCardSecondary,
                            border = BorderStroke(0.8.dp, iOSBlue),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { activeDatePicker = "end" }
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("BİTİŞ", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = iOSBlue)
                                Text(calculatedRange.second.ifBlank { "Seçiniz" }, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Temsilci / Limit Seçenekleri
                Text(
                    text = "2. ÇEKİM LİMİTİ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = iOSBlue,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(500, 1000, 2000, 5000).forEach { limit ->
                        val isSelected = selectedLimit == limit
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) iOSBlue else iOSCardSecondary,
                            border = BorderStroke(0.8.dp, if (isSelected) Color(0xFF60A5FA) else iOSBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedLimit = limit }
                        ) {
                            Text(
                                text = "$limit Kayıt",
                                fontSize = 10.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Sunucu API Kodu Kılavuzu Butonu
                OutlinedButton(
                    onClick = { showServerCodeHelp = true },
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(0.8.dp, Color(0xFF60A5FA))
                ) {
                    Icon(Icons.Default.Code, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("💡 Sunucu API Okuma Endpoint Kodu (Python / Node / PHP / C#)", fontSize = 11.sp, color = Color(0xFF93C5FD), fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Aksiyon Butonları
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isFetchingFromSql,
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("İptal", fontSize = 12.5.sp)
                    }

                    Button(
                        onClick = {
                            viewModel.fetchCallsFromSql(
                                startDate = calculatedRange.first,
                                endDate = calculatedRange.second,
                                userCode = userCodeFilter,
                                limit = selectedLimit
                            )
                            onDismiss()
                        },
                        enabled = !isFetchingFromSql,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                        modifier = Modifier.weight(1.5f).height(42.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isFetchingFromSql) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("İndiriliyor...", fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SQL'den İndir", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Telefon Yerel Hafızasını Temizleme Penceresi (Arka Ofis / SQL Server'a Asla Dokunmaz)
 */
@Composable
fun LocalMemoryCleanDialog(
    viewModel: SupportViewModel,
    allRecordsCount: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val storageBreakdown by viewModel.storageBreakdown.collectAsState()
    var confirmActionType by remember { mutableStateOf<String?>(null) } // "SYNCED", "OLDER_30", "OLDER_60", "CACHE", "ALL_LOCAL"

    LaunchedEffect(Unit) {
        viewModel.refreshStorageBreakdown(context)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(0.96f)
                .wrapContentHeight()
                .clip(RoundedCornerShape(18.dp))
                .testTag("local_memory_clean_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF047857)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CleaningServices, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Telefon Hafızasını Temizle",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Mevcut Yerel Kayıt: $allRecordsCount adet",
                                fontSize = 11.sp,
                                color = iOSTextSecondary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat", tint = iOSTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Güvenlik Rozeti ve Garantisi
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFECFDF5),
                    border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "🛡️ ARKA OFİS & SQL SERVER KORUMASI",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF065F46)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Bu işlemler SADECE telefonun hafızasını rahatlatır. SQL Server ve Arka Ofis veritabanınızdaki kayıtlarınıza KESİNLİKLE DOKUNULMAZ ve hiçbir şey silinmez.",
                                fontSize = 11.sp,
                                color = Color(0xFF047857),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Depolama Durum Kartları
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Toplam Uygulama Verisi
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0F172A),
                        border = BorderStroke(0.8.dp, Color(0xFF334155)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("MEVCUT DOLULUK", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                            Text(
                                text = viewModel.formatBytes(storageBreakdown.totalAppSizeBytes),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF60A5FA)
                            )
                            Text(
                                text = "${storageBreakdown.dbRecordCount} Yerel Kayıt",
                                fontSize = 8.5.sp,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }

                    // Temizlenebilir Tahmini Alan
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF064E3B),
                        border = BorderStroke(0.8.dp, Color(0xFF059669)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("AÇILABİLİR ALAN", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA7F3D0))
                            Text(
                                text = viewModel.formatBytes(storageBreakdown.estimatedTotalCleanableBytes),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF34D399)
                            )
                            Text(
                                text = "${storageBreakdown.syncedCount} SQL Aktarılmış",
                                fontSize = 8.5.sp,
                                color = Color(0xFFA7F3D0)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Temizleme Seçenekleri Listesi
                Text(
                    text = "TEMİZLEME SEÇENEKLERİ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = iOSBlue,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Seçenek 1: Sadece SQL ile Eşitlenmiş Kayıtları Sil (En Güvenli)
                MemoryCleanOptionCard(
                    title = "SQL'den Çekilen / Senkronize Kayıtları Sil",
                    description = "Sunucuda zaten yedekli olan kayıtları telefondan siler. Bekleyen yerel kayıtlar korunur.",
                    badge = "Önerilen",
                    badgeColor = Color(0xFF059669),
                    icon = Icons.Default.CloudDone,
                    onClick = { confirmActionType = "SYNCED" }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Seçenek 2: 30 Günden Eski Kayıtları Temizle
                MemoryCleanOptionCard(
                    title = "30 Günden Eski Kayıtları Temizle",
                    description = "Bir aydan eski eşitlenmiş yerel kayıtları temizleyerek yer açar.",
                    badge = "Hızlı Temizlik",
                    badgeColor = iOSBlue,
                    icon = Icons.Default.History,
                    onClick = { confirmActionType = "OLDER_30" }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Seçenek 3: Geçici Dosya ve Önbellek Temizliği
                MemoryCleanOptionCard(
                    title = "Geçici Dosyaları ve Önbelleği (Cache) Temizle",
                    description = "Oluşturulan geçici Excel raporları ve önbellek dosyalarını siler.",
                    badge = "Önbellek",
                    badgeColor = iOSOrange,
                    icon = Icons.Default.FolderDelete,
                    onClick = { confirmActionType = "CACHE" }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Seçenek 4: Tüm Yerel Kayıtları Sıfırla (Telefon Hafızası)
                MemoryCleanOptionCard(
                    title = "Tüm Yerel Çağrıları Sıfırla (Sadece Telefon)",
                    description = "Telefon hafızasını tamamen boşaltır. (Arka Ofis / SQL Server veritabanınız korunur).",
                    badge = "Tüm Yerel",
                    badgeColor = iOSRed,
                    icon = Icons.Default.DeleteSweep,
                    onClick = { confirmActionType = "ALL_LOCAL" }
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = iOSCardSecondary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Kapat", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.5.sp)
                }
            }
        }
    }

    // Onay Penceresi (Confirmation Dialog)
    if (confirmActionType != null) {
        AlertDialog(
            onDismissRequest = { confirmActionType = null },
            icon = {
                Icon(
                    imageVector = if (confirmActionType == "ALL_LOCAL") Icons.Default.Warning else Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = if (confirmActionType == "ALL_LOCAL") iOSRed else iOSBlue
                )
            },
            title = {
                Text(
                    text = when (confirmActionType) {
                        "SYNCED" -> "SQL Kayıtları Yerelden Silinsin mi?"
                        "OLDER_30" -> "30 Günden Eski Kayıtlar Silinsin mi?"
                        "CACHE" -> "Önbellek Temizlensin mi?"
                        "ALL_LOCAL" -> "Tüm Yerel Kayıtlar Sıfırlansın mı?"
                        else -> "Onay"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            },
            text = {
                Text(
                    text = when (confirmActionType) {
                        "SYNCED" -> "SQL Server ile eşitlenmiş olan tüm yerel kayıtlar telefon hafızasından silinecektir.\n\n🛡️ SQL Server / Arka Ofis veritabanındaki ana kayıtlarınıza hiçbir zarar gelmez."
                        "OLDER_30" -> "30 günden daha eski eşitlenmiş kayıtlar yerel hafızadan temizlenecektir.\n\n🛡️ Arka Ofis kayıtlarınız korunur."
                        "CACHE" -> "Geçici rapor ve önbellek dosyaları temizlenecektir."
                        "ALL_LOCAL" -> "Telefon hafızasındaki tüm çağrı kayıtları sıfırlanacaktır.\n\n🛡️ DİKKAT: SQL Server / Arka Ofis veritabanındaki kayıtlarınız KESİNLİKLE SİLİNMEZ. İhtiyaç duyduğunuzda 'SQL Server'dan Çek' butonuyla dilediğiniz tarih aralığını tekrar indirebilirsiniz."
                        else -> ""
                    },
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        when (confirmActionType) {
                            "SYNCED" -> viewModel.clearSyncedLocalRecords()
                            "OLDER_30" -> viewModel.clearSyncedLocalRecordsOlderThan(30)
                            "CACHE" -> viewModel.clearAppTempCache(context)
                            "ALL_LOCAL" -> viewModel.clearAllLocalRecordsOnly()
                        }
                        confirmActionType = null
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (confirmActionType == "ALL_LOCAL") iOSRed else Color(0xFF059669))
                ) {
                    Text("Evet, Temizle", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { confirmActionType = null }) {
                    Text("Vazgeç")
                }
            }
        )
    }
}

@Composable
private fun MemoryCleanOptionCard(
    title: String,
    description: String,
    badge: String,
    badgeColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = iOSCardSecondary,
        border = BorderStroke(0.8.dp, iOSBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = badgeColor.copy(alpha = 0.2f),
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text(
                            text = badge,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 10.5.sp,
                    color = iOSTextSecondary,
                    lineHeight = 14.sp
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = iOSTextSecondary, modifier = Modifier.size(16.dp))
        }
    }
}

/**
 * Sunucu API'sine Eklenecek Okuma (Fetch / Get Calls) Kod Kılavuzu Dialogu
 */
@Composable
fun ServerApiCodeHelpDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedLanguageTab by remember { mutableStateOf(0) } // 0: Python FastAPI/Flask, 1: Node.js Express, 2: PHP, 3: SQL Sorgusu

    val pythonCode = """
# Python FastAPI / Flask Örnek Endpoint Kodu:
# Endpoint: POST /api/fetch-calls veya GET /api/fetch-calls

from fastapi import FastAPI, Request
import pyodbc # veya pymssql / pymysql

@app.post("/api/fetch-calls")
async def fetch_calls(request: Request):
    data = await request.json()
    start_date = data.get("start_date", "")
    end_date = data.get("end_date", "")
    user_code = data.get("user_code", "")
    limit = int(data.get("limit", 2000))
    
    conn = pyodbc.connect('DRIVER={ODBC Driver 17 for SQL Server};SERVER=localhost;DATABASE=destek_db;UID=sa;PWD=parola')
    cursor = conn.cursor()
    
    query = "SELECT TOP (?) * FROM destek_kayitlari WHERE 1=1"
    params = [limit]
    
    if start_date:
        query += " AND BASLANGICTARIHI >= ?"
        params.append(start_date + " 00:00:00")
    if end_date:
        query += " AND BASLANGICTARIHI <= ?"
        params.append(end_date + " 23:59:59")
    if user_code:
        query += " AND (KULLANICIKODU = ? OR KULLANICIADI = ?)"
        params.extend([user_code, user_code])
        
    query += " ORDER BY BASLANGICTARIHI DESC"
    
    cursor.execute(query, params)
    columns = [column[0] for column in cursor.description]
    records = [dict(zip(columns, row)) for row in cursor.fetchall()]
    
    return {
        "success": True,
        "records": records,
        "total": len(records)
    }
""".trimIndent()

    val nodeJsCode = """
// Node.js Express (mssql) Örnek Endpoint Kodu:
// Endpoint: POST /api/fetch-calls

const express = require('express');
const sql = require('mssql');
const app = express();
app.use(express.json());

app.post('/api/fetch-calls', async (req, res) => {
    try {
        const { start_date, end_date, user_code, limit = 2000 } = req.body;
        
        let query = "SELECT TOP (@limit) * FROM destek_kayitlari WHERE 1=1";
        const request = new sql.Request();
        request.input('limit', sql.Int, parseInt(limit));
        
        if (start_date) {
            query += " AND BASLANGICTARIHI >= @startDate";
            request.input('startDate', sql.VarChar, start_date + ' 00:00:00');
        }
        if (end_date) {
            query += " AND BASLANGICTARIHI <= @endDate";
            request.input('endDate', sql.VarChar, end_date + ' 23:59:59');
        }
        if (user_code) {
            query += " AND (KULLANICIKODU = @userCode OR KULLANICIADI = @userCode)";
            request.input('userCode', sql.VarChar, user_code);
        }
        
        query += " ORDER BY BASLANGICTARIHI DESC";
        
        const result = await request.query(query);
        return res.json({
            success: true,
            records: result.recordset,
            total: result.recordset.length
        });
    } catch (err) {
        return res.status(500).json({ success: false, message: err.message });
    }
});
""".trimIndent()

    val phpCode = """
<?php
// PHP Örnek Endpoint: api/fetch-calls.php
header('Content-Type: application/json; charset=utf-8');

${'$'}input = json_decode(file_get_contents('php://input'), true) ?? ${'$'}_GET;
${'$'}startDate = ${'$'}input['start_date'] ?? '';
${'$'}endDate = ${'$'}input['end_date'] ?? '';
${'$'}userCode = ${'$'}input['user_code'] ?? '';
${'$'}limit = intval(${'$'}input['limit'] ?? 2000);

try {
    ${'$'}pdo = new PDO("sqlsrv:Server=localhost;Database=destek_db", "sa", "parola");
    ${'$'}pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    
    ${'$'}sql = "SELECT TOP " . intval(${'$'}limit) . " * FROM destek_kayitlari WHERE 1=1";
    ${'$'}params = [];
    
    if (!empty(${'$'}startDate)) {
        ${'$'}sql .= " AND BASLANGICTARIHI >= ?";
        ${'$'}params[] = ${'$'}startDate . " 00:00:00";
    }
    if (!empty(${'$'}endDate)) {
        ${'$'}sql .= " AND BASLANGICTARIHI <= ?";
        ${'$'}params[] = ${'$'}endDate . " 23:59:59";
    }
    if (!empty(${'$'}userCode)) {
        ${'$'}sql .= " AND (KULLANICIKODU = ? OR KULLANICIADI = ?)";
        ${'$'}params[] = ${'$'}userCode;
        ${'$'}params[] = ${'$'}userCode;
    }
    ${'$'}sql .= " ORDER BY BASLANGICTARIHI DESC";
    
    ${'$'}stmt = ${'$'}pdo->prepare(${'$'}sql);
    ${'$'}stmt->execute(${'$'}params);
    ${'$'}records = ${'$'}stmt->fetchAll(PDO::FETCH_ASSOC);
    
    echo json_encode([
        'success' => true,
        'records' => ${'$'}records,
        'total' => count(${'$'}records)
    ], JSON_UNESCAPED_UNICODE);
} catch (Exception ${'$'}e) {
    http_response_code(500);
    echo json_encode(['success' => false, 'message' => ${'$'}e->getMessage()]);
}
?>
""".trimIndent()

    val sqlQuery = """
-- SQL Server Doğrudan Test Sorgusu:
SELECT TOP 2000 
    KULLANICIKODU,
    KULLANICIADI,
    BASLANGICTARIHI,
    ARAYANTURU,
    MUSTERIBAYIADI,
    GORUSULENKISI,
    TELEFON,
    TELEFON2,
    MAIL,
    ILETISIMTURU,
    DESTEKKONUSU1,
    SICILNO1,
    TESPIT1,
    ISLEM1,
    BAGLANTIID1,
    BITISTARIHI,
    TOPLAMSURE,
    KASAVERSIYON1,
    ARKAOFISVERSIYON1,
    REDMINEID1,
    AKTIF,
    B2BKAYIT,
    BEKLEYENIS,
    ARGEGIDIS,
    ARGEDONUS,
    ARGEGIDISNOTU,
    ARGEDONUSNOTU,
    RESIM,
    TCKN,
    VERGINO,
    TESPITKODU1,
    ISLEMKODU1,
    YN500SICILNO1,
    DYNCASHVERSIYON1,
    ARKAOFISID1,
    YN500KASAVERSIYON1,
    NOT1
FROM destek_kayitlari
WHERE BASLANGICTARIHI >= '2026-10-01 00:00:00'
ORDER BY BASLANGICTARIHI DESC;
""".trimIndent()

    val activeCode = when (selectedLanguageTab) {
        0 -> pythonCode
        1 -> nodeJsCode
        2 -> phpCode
        else -> sqlQuery
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(18.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1E3A8A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Terminal, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Sunucu API Okuma Kılavuzu",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "SQL Server'dan telefona veri okuma endpointi",
                                fontSize = 11.sp,
                                color = iOSTextSecondary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat", tint = iOSTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bilgilendirme Notu
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFEF3C7),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Telefondan sunucuya kayıt yazma (sync-calls) çalışıyor fakat sunucunuzdaki API scriptine henüz veri okuma (fetch-calls) fonksiyonu eklenmemiş olabilir. Aşağıdaki hazır kodu sunucu scriptinize eklemeniz yeterlidir.",
                            fontSize = 11.sp,
                            color = Color(0xFF92400E),
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tab Selector (Python, Node.js, PHP, SQL)
                TabRow(
                    selectedTabIndex = selectedLanguageTab,
                    containerColor = Color(0xFF0F172A),
                    contentColor = iOSBlue
                ) {
                    Tab(
                        selected = selectedLanguageTab == 0,
                        onClick = { selectedLanguageTab = 0 },
                        text = { Text("Python", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedLanguageTab == 1,
                        onClick = { selectedLanguageTab = 1 },
                        text = { Text("Node.js", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedLanguageTab == 2,
                        onClick = { selectedLanguageTab = 2 },
                        text = { Text("PHP", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedLanguageTab == 3,
                        onClick = { selectedLanguageTab = 3 },
                        text = { Text("SQL Sorgusu", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Code Box with Scroll
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0A0F1D),
                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = activeCode,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFF93C5FD),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState())
                                .horizontalScroll(rememberScrollState())
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Copy Code and Close Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("API Endpoint Kodu", activeCode)
                            clipboard?.setPrimaryClip(clip)
                            android.widget.Toast.makeText(context, "API Kodu Panoya Kopyalandı!", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.5f).height(42.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Kodu Kopyala", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(42.dp)
                    ) {
                        Text("Kapat", fontSize = 12.5.sp)
                    }
                }
            }
        }
    }
}

