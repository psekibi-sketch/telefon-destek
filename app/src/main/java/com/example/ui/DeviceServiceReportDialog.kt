package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.SupportRecord
import com.example.ui.theme.*

data class DeviceReportData(
    val query: String,
    val title: String,
    val totalCalls: Int,
    val totalSeconds: Long,
    val resolvedCount: Int,
    val pendingCount: Int,
    val firstDate: String,
    val lastDate: String,
    val topIssueBreakdown: List<Pair<String, Int>>,
    val userBreakdown: List<Pair<String, Pair<Int, Long>>>,
    val actionsSummary: List<String>,
    val records: List<SupportRecord>
)

/**
 * Cihaz / Sicil No / Müşteri Servis Geçmişi ve Kapsamlı Analiz Raporu Dialogu
 */
@Composable
fun DeviceServiceReportDialog(
    data: DeviceReportData,
    viewModel: SupportViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: Özet & Dağılım, 1: Çağrı Dökümü, 2: Metin Önizleme
    var includeExcelInShare by remember { mutableStateOf(true) }

    val reportText = remember(data) {
        viewModel.generateDeviceReportText(data)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 14.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
                .clip(RoundedCornerShape(18.dp))
                .testTag("device_service_report_dialog"),
            color = iOSBackground,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Header (Başlık & Kapat)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF2563EB)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Assessment, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Cihaz & Sicil Servis Raporu",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = iOSTextPrimary
                            )
                            Text(
                                text = "${data.query.uppercase()} • ${data.title}",
                                fontSize = 11.5.sp,
                                color = iOSBlue,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat", tint = iOSTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // KPI Metrik Kartları
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DeviceMetricCard(
                        title = "TOPLAM ÇAĞRI",
                        value = "${data.totalCalls}",
                        sub = "Servis Kaydı",
                        color = iOSBlue,
                        modifier = Modifier.weight(1f)
                    )
                    DeviceMetricCard(
                        title = "TOPLAM SÜRE",
                        value = viewModel.formatSecondsToReadable(data.totalSeconds),
                        sub = "Harcanan Süre",
                        color = iOSOrange,
                        modifier = Modifier.weight(1.2f)
                    )
                    DeviceMetricCard(
                        title = "ÇÖZÜM ORANI",
                        value = "%${if (data.totalCalls > 0) (data.resolvedCount * 100 / data.totalCalls) else 100}",
                        sub = "${data.resolvedCount}/${data.totalCalls} Çözüldü",
                        color = ExcelGreen,
                        modifier = Modifier.weight(1.1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tab Switcher (Özet & Analiz / Çağrı Dökümü / Metin Rapor)
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = iOSCardSecondary,
                    contentColor = iOSBlue,
                    divider = {},
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("📊 Özet & Dağılım", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("📑 Çağrı Dökümü (${data.records.size})", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("📝 Rapor Metni", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Main Tab Content
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> DeviceReportSummaryTab(data = data, viewModel = viewModel)
                        1 -> DeviceReportCallsListTab(records = data.records, viewModel = viewModel)
                        2 -> DeviceReportTextPreviewTab(
                            reportText = reportText,
                            includeExcel = includeExcelInShare,
                            onToggleExcel = { includeExcelInShare = it },
                            recordCount = data.records.size
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Action Bar: WhatsApp + E-Posta + Excel + Kopyala
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // 1. WhatsApp ile Paylaş (Özet Metin + 21 Sütunlu Excel)
                        Button(
                            onClick = {
                                viewModel.shareDeviceReport(
                                    context = context,
                                    data = data,
                                    includeExcel = includeExcelInShare,
                                    viaWhatsApp = true
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.3f).height(42.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (includeExcelInShare) "WhatsApp (Rapor + Excel)" else "WhatsApp ile Gönder",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // 2. E-Posta ile Gönder
                        Button(
                            onClick = {
                                viewModel.shareDeviceReport(
                                    context = context,
                                    data = data,
                                    includeExcel = includeExcelInShare,
                                    viaMail = true
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(42.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("E-Posta (Mail)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // 3. Excel (.xlsx) İndir / Dışa Aktar
                        Button(
                            onClick = {
                                val safeQuery = data.query.replace(Regex("[^a-zA-Z0-9_]"), "_")
                                viewModel.exportServiceManagerExcel(
                                    context = context,
                                    records = data.records,
                                    filenameSuffix = "Cihaz_Servis_Gecmisi_${safeQuery}"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ExcelGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.1f).height(38.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Excel (.xlsx)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        // 4. Metni Kopyala
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                val clip = ClipData.newPlainText("Cihaz Servis Raporu", reportText)
                                clipboard?.setPrimaryClip(clip)
                                Toast.makeText(context, "📋 Cihaz servis raporu panoya kopyalandı!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F2942)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(0.9f).height(38.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = iOSBlue, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Metni Kopyala", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = iOSBlue)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceMetricCard(
    title: String,
    value: String,
    sub: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = iOSCardSecondary,
        border = BorderStroke(0.8.dp, color.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = iOSTextSecondary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(sub, fontSize = 8.sp, color = iOSTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * Tab 0: Özet ve Dağılım Paneli (Arıza & Temsilci Analizi)
 */
@Composable
private fun DeviceReportSummaryTab(
    data: DeviceReportData,
    viewModel: SupportViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Tarih ve Cihaz Künyesi Kartı
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = iOSCardSecondary),
            border = BorderStroke(0.8.dp, iOSBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = iOSBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cihaz & Müşteri Künyesi", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = iOSTextPrimary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("İlk Servis Kaydı:", fontSize = 10.5.sp, color = iOSTextSecondary)
                    Text(data.firstDate, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = iOSTextPrimary)
                }
                Spacer(modifier = Modifier.height(3.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Son Servis Kaydı:", fontSize = 10.5.sp, color = iOSTextSecondary)
                    Text(data.lastDate, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = iOSOrange)
                }
                Spacer(modifier = Modifier.height(3.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Kayıtlı Telefonlar:", fontSize = 10.5.sp, color = iOSTextSecondary)
                    val phones = data.records.map { it.telefon }.filter { it.isNotBlank() }.distinct().take(2).joinToString(", ")
                    Text(phones.ifBlank { "-" }, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = iOSBlue)
                }
            }
        }

        // 1. Arıza ve Destek Konusu Dağılımı ("Şu arızadan şu kadar...")
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = iOSCardSecondary),
            border = BorderStroke(0.8.dp, iOSBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PieChart, contentDescription = null, tint = iOSOrange, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Arıza & Destek Konusu Dağılımı", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = iOSTextPrimary)
                }
                Spacer(modifier = Modifier.height(8.dp))

                val colors = listOf(Color(0xFFEF4444), Color(0xFFF59E0B), Color(0xFF10B981), Color(0xFF3B82F6), Color(0xFF8B5CF6), Color(0xFFEC4899))

                data.topIssueBreakdown.forEachIndexed { idx, (issue, count) ->
                    val ratio = if (data.totalCalls > 0) count.toFloat() / data.totalCalls.toFloat() else 0f
                    val color = colors.getOrElse(idx) { iOSBlue }

                    Column(modifier = Modifier.padding(vertical = 3.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = issue,
                                fontSize = 11.sp,
                                color = iOSTextPrimary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$count Çağrı (%${(ratio * 100).toInt()})",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = color
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        LinearProgressIndicator(
                            progress = { ratio.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = color,
                            trackColor = Color(0xFF1E293B)
                        )
                    }
                }
            }
        }

        // 2. Bakan Kullanıcı / Temsilci Dağılımı ("Şu kullanıcı şu kadar bakmış...")
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = iOSCardSecondary),
            border = BorderStroke(0.8.dp, iOSBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.People, contentDescription = null, tint = iOSBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Bakan Temsilci & Personel Dağılımı", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = iOSTextPrimary)
                }
                Spacer(modifier = Modifier.height(8.dp))

                data.userBreakdown.forEach { (user, pair) ->
                    val count = pair.first
                    val secs = pair.second
                    val ratio = if (data.totalCalls > 0) count.toFloat() / data.totalCalls.toFloat() else 0f

                    Column(modifier = Modifier.padding(vertical = 3.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = user,
                                fontSize = 11.sp,
                                color = iOSTextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "$count Çağrı • ${viewModel.formatSecondsToReadable(secs)}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = iOSBlue
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        LinearProgressIndicator(
                            progress = { ratio.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = iOSBlue,
                            trackColor = Color(0xFF1E293B)
                        )
                    }
                }
            }
        }

        // 3. Yapılan Başlıca İşlemler
        if (data.actionsSummary.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = iOSCardSecondary),
                border = BorderStroke(0.8.dp, iOSBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Build, contentDescription = null, tint = ExcelGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Yapılan Başlıca İşlemler / Müdahaleler", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = iOSTextPrimary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    data.actionsSummary.forEach { action ->
                        Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
                            Text("✓ ", color = ExcelGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text(action, fontSize = 10.5.sp, color = iOSTextSecondary)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tab 1: Çağrı Dökümü (Kronolojik Liste)
 */
@Composable
private fun DeviceReportCallsListTab(records: List<SupportRecord>, viewModel: SupportViewModel) {
    if (records.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Çağrı kaydı bulunamadı.", color = iOSTextSecondary, fontSize = 12.sp)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(records, key = { it.id }) { record ->
            val isResolved = !record.bekleyenIs.equals("Evet", ignoreCase = true)
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = iOSCardSecondary),
                border = BorderStroke(0.6.dp, iOSBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(9.dp)) {
                    // Top row: Tarih, Temsilci, Durum rozeti
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = record.baslangicTarihi.take(16),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = iOSBlue
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "👤 ${viewModel.resolveRepresentativeName(record.kullaniciAdi.ifBlank { record.kullaniciKodu })}",
                                fontSize = 10.sp,
                                color = iOSTextSecondary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isResolved) Color(0xFF14532D) else Color(0xFF78350F)
                        ) {
                            Text(
                                text = if (isResolved) "Çözüldü" else "Bekliyor",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isResolved) Color(0xFF86EFAC) else Color(0xFFFDE68A),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Konu & Tespit
                    Text(
                        text = "📌 Konu: ${record.destekKonusu1.ifBlank { record.tespit1 }}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = iOSTextPrimary
                    )

                    if (record.islem1.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "🛠️ İşlem: ${record.islem1}",
                            fontSize = 10.sp,
                            color = iOSTextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Alt Satır: Telefon, Süre, Redmine
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📞 ${record.telefon.ifBlank { "-" }} • ${record.gorusulenKisi.ifBlank { "-" }}",
                            fontSize = 9.5.sp,
                            color = iOSTextSecondary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (record.redmineId1.isNotBlank() && record.redmineId1 != "-") {
                                Text(
                                    text = "🎫 #${record.redmineId1}",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = iOSOrange
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = "⏱️ ${record.toplamSure}",
                                fontSize = 9.5.sp,
                                color = iOSTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tab 2: Formatlı Metin Önizleme ve Excel Ekleme Ayarı
 */
@Composable
private fun DeviceReportTextPreviewTab(
    reportText: String,
    includeExcel: Boolean,
    onToggleExcel: (Boolean) -> Unit,
    recordCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Excel toggle card
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = if (includeExcel) ExcelGreenLight.copy(alpha = 0.4f) else iOSCardSecondary),
            border = BorderStroke(0.8.dp, if (includeExcel) ExcelGreen else iOSBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TableChart, contentDescription = null, tint = ExcelGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("21 Sütunlu Excel Tablosunu Ek Olarak Ekle", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = iOSTextPrimary)
                        Text("$recordCount adet filtrelenmiş çağrı kaydı", fontSize = 9.5.sp, color = iOSTextSecondary)
                    }
                }
                Switch(checked = includeExcel, onCheckedChange = onToggleExcel)
            }
        }

        // Preview Box
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(0.8.dp, iOSBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "📋 Rapor Metni Önizlemesi (WhatsApp / E-Posta):",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = iOSBlue
                )
                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = Color(0xFF1E293B), thickness = 0.6.dp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = reportText,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFE2E8F0),
                    lineHeight = 16.sp
                )
            }
        }
    }
}
