package com.example.ui

import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DailyShiftReportDialog(
    viewModel: SupportViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val allRecords by viewModel.allRecords.collectAsState()
    val globalUser by viewModel.globalKullanici.collectAsState()
    val globalUserCode by viewModel.globalKullaniciKodu.collectAsState()

    val todayYmd = remember { viewModel.todayDateString() }
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    val yesterdayYmd = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        sdf.format(cal.time)
    }

    val twoDaysAgoYmd = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -2)
        sdf.format(cal.time)
    }

    val threeDaysAgoYmd = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -3)
        sdf.format(cal.time)
    }

    var selectedDate by remember { mutableStateOf(todayYmd) }
    var showCalendarModal by remember { mutableStateOf(false) }
    var includeExcelWithReport by remember { mutableStateOf(true) }

    val displayDate = remember(selectedDate) {
        viewModel.formatYmdToDisplay(selectedDate)
    }

    val selectedRecords = remember(allRecords, selectedDate) {
        allRecords.filter { viewModel.isRecordOnDate(it.baslangicTarihi, selectedDate) }
    }

    val reportText = remember(allRecords, globalUser, globalUserCode, selectedDate) {
        viewModel.generateDailyShiftReport(selectedDate)
    }

    val totalCalls = selectedRecords.size
    val resolvedCount = remember(selectedRecords) {
        selectedRecords.count { it.bekleyenIs.equals("Hayır", ignoreCase = true) || it.bekleyenIs.isBlank() }
    }
    val pendingCount = totalCalls - resolvedCount

    // Pure Jetpack Compose Calendar Picker Modal (No keyboard text input, full interactive calendar)
    if (showCalendarModal) {
        AppComposeDatePickerDialog(
            initialDateYmd = selectedDate,
            title = "Gün Sonu Rapor Tarihi Seçiniz",
            onDateSelected = { newDate ->
                selectedDate = newDate
                showCalendarModal = false
            },
            onDismiss = { showCalendarModal = false }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(20.dp))
                .testTag("daily_shift_report_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
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
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E2818)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = null,
                                tint = Color(0xFF4ADE80),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "🌙 Gün Sonu / Vardiya Raporu",
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = iOSTextPrimary
                            )
                            Text(
                                text = "Rapor Tarihi: $displayDate • Temsilci: ${globalUser.ifBlank { "Temsilci" }}",
                                fontSize = 11.5.sp,
                                color = iOSTextSecondary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat", tint = iOSTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Date Selector Header & Presets (Menüden ve Takvimden Seçim - Elle Yazma Yok)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = iOSCardSecondary,
                    border = BorderStroke(1.dp, if (selectedDate == todayYmd) Color(0xFF15803D).copy(alpha = 0.5f) else Color(0xFFD97706).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = if (selectedDate == todayYmd) Color(0xFF22C55E) else Color(0xFFF59E0B),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Vardiya / Rapor Tarihi:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = iOSTextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (selectedDate == todayYmd) Color(0xFF14532D) else Color(0xFF78350F)
                                    ) {
                                        Text(
                                            text = if (selectedDate == todayYmd) "BUGÜN" else "GEÇMİŞ TARİH (SONRADAN GÖNDERİM)",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedDate == todayYmd) Color(0xFF86EFAC) else Color(0xFFFDE68A),
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = displayDate,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (selectedDate == todayYmd) Color(0xFF4ADE80) else Color(0xFFFBBF24)
                                )
                            }

                            // Calendar Picker Trigger Button
                            Button(
                                onClick = { showCalendarModal = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedDate == todayYmd) Color(0xFF15803D) else Color(0xFFB45309)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("btn_open_report_calendar")
                            ) {
                                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Takvimden Seç", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick Presets Row (Menüden Tek Dokunuşla Seçim)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isToday = selectedDate == todayYmd
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isToday) Color(0xFF15803D) else Color(0xFF0F172A),
                                border = BorderStroke(0.8.dp, if (isToday) Color(0xFF22C55E) else iOSBorder),
                                modifier = Modifier.clickable { selectedDate = todayYmd }
                            ) {
                                Text(
                                    text = "Bugün",
                                    fontSize = 11.sp,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }

                            val isYesterday = selectedDate == yesterdayYmd
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isYesterday) Color(0xFF15803D) else Color(0xFF0F172A),
                                border = BorderStroke(0.8.dp, if (isYesterday) Color(0xFF22C55E) else iOSBorder),
                                modifier = Modifier.clickable { selectedDate = yesterdayYmd }
                            ) {
                                Text(
                                    text = "Dün",
                                    fontSize = 11.sp,
                                    fontWeight = if (isYesterday) FontWeight.Bold else FontWeight.Normal,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }

                            val isTwoDaysAgo = selectedDate == twoDaysAgoYmd
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isTwoDaysAgo) Color(0xFF15803D) else Color(0xFF0F172A),
                                border = BorderStroke(0.8.dp, if (isTwoDaysAgo) Color(0xFF22C55E) else iOSBorder),
                                modifier = Modifier.clickable { selectedDate = twoDaysAgoYmd }
                            ) {
                                Text(
                                    text = "2 Gün Önce",
                                    fontSize = 11.sp,
                                    fontWeight = if (isTwoDaysAgo) FontWeight.Bold else FontWeight.Normal,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }

                            val isThreeDaysAgo = selectedDate == threeDaysAgoYmd
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isThreeDaysAgo) Color(0xFF15803D) else Color(0xFF0F172A),
                                border = BorderStroke(0.8.dp, if (isThreeDaysAgo) Color(0xFF22C55E) else iOSBorder),
                                modifier = Modifier.clickable { selectedDate = threeDaysAgoYmd }
                            ) {
                                Text(
                                    text = "3 Gün Önce",
                                    fontSize = 11.sp,
                                    fontWeight = if (isThreeDaysAgo) FontWeight.Bold else FontWeight.Normal,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF1E293B),
                                border = BorderStroke(0.8.dp, iOSBlue),
                                modifier = Modifier.clickable { showCalendarModal = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = iOSBlue, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Başka Gün Seç (Takvim)...",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = iOSBlue
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Quick Stats Metric Cards Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = iOSCardSecondary,
                            border = BorderStroke(0.6.dp, iOSBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Toplam Çağrı", fontSize = 10.sp, color = iOSTextSecondary)
                                Text(
                                    text = "$totalCalls",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = iOSBlue
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = iOSCardSecondary,
                            border = BorderStroke(0.6.dp, iOSBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Çözülen", fontSize = 10.sp, color = iOSTextSecondary)
                                Text(
                                    text = "$resolvedCount",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ExcelGreen
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = iOSCardSecondary,
                            border = BorderStroke(0.6.dp, iOSBorder),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Bekleyen", fontSize = 10.sp, color = iOSTextSecondary)
                                Text(
                                    text = "$pendingCount",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (pendingCount > 0) iOSRed else iOSTextSecondary
                                )
                            }
                        }
                    }

                    // Formatted Report Preview Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0F172A),
                        border = BorderStroke(0.8.dp, Color(0xFF334155)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Chat,
                                        contentDescription = null,
                                        tint = Color(0xFF4ADE80),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "GÖNDERİLECEK RAPOR METNİ ($displayDate)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF94A3B8)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF1E293B),
                                    modifier = Modifier.clickable {
                                        viewModel.copyDailyShiftReportToClipboard(context, selectedDate)
                                        Toast.makeText(context, "📋 Rapor panoya kopyalandı!", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = iOSBlue, modifier = Modifier.size(11.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Kopyala", fontSize = 10.sp, color = iOSBlue, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = Color(0xFF1E293B), thickness = 0.8.dp)
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = reportText,
                                fontSize = 11.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFE2E8F0),
                                lineHeight = 17.sp
                            )
                        }
                    }

                    // Optional Daily Excel Export Card (İsteğe Bağlı Günlük Excel Dosyası)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (includeExcelWithReport) ExcelGreenLight.copy(alpha = 0.5f) else iOSCardSecondary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (includeExcelWithReport) ExcelGreen else iOSBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (includeExcelWithReport) ExcelGreen else Color(0xFF475569)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.TableChart,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Excel Rapor Dosyasını (.xlsx) Ekle",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.5.sp,
                                                color = if (includeExcelWithReport) ExcelGreenDark else iOSTextPrimary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (includeExcelWithReport) Color(0xFFDCFCE7) else Color(0xFF334155)
                                            ) {
                                                Text(
                                                    text = "İsteğe Bağlı",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (includeExcelWithReport) ExcelGreenDark else Color(0xFF94A3B8),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = if (totalCalls > 0) {
                                                "GunSonu_${selectedDate.replace("-", "")}.xlsx • $totalCalls çağrı, 21 sütunlu tam veri tablosu"
                                            } else {
                                                "Bu tarihe ait kayıt bulunmamaktadır"
                                            },
                                            fontSize = 10.sp,
                                            color = if (includeExcelWithReport) Slate800 else iOSTextSecondary
                                        )
                                    }
                                }

                                Switch(
                                    checked = includeExcelWithReport,
                                    onCheckedChange = { includeExcelWithReport = it },
                                    modifier = Modifier.testTag("switch_include_excel_daily_report")
                                )
                            }

                            if (includeExcelWithReport && totalCalls > 0) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "📎 WhatsApp veya E-Posta gönderiminde özet metnin altına bu tarihin 21 sütunlu tam Excel tablosu eklenerek gönderilir.",
                                    fontSize = 10.sp,
                                    color = ExcelGreenDark,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.exportAndShareDailyExcel(context, selectedDate)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ExcelGreen),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1.2f).height(34.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Sadece Excel'i Aç / İndir", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            viewModel.exportAndShareDailyExcel(context, selectedDate)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).height(34.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp),
                                        border = BorderStroke(0.8.dp, ExcelGreen)
                                    ) {
                                        Icon(Icons.Default.Email, contentDescription = null, tint = ExcelGreenDark, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Outlook / Mail", fontSize = 10.5.sp, color = ExcelGreenDark, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Share Buttons
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // WhatsApp Button
                        Button(
                            onClick = {
                                viewModel.shareDailyShiftReportViaWhatsApp(
                                    context = context,
                                    targetDateStr = selectedDate,
                                    includeExcel = includeExcelWithReport && totalCalls > 0
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(44.dp)
                                .testTag("btn_share_report_whatsapp"),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (includeExcelWithReport && totalCalls > 0) "WhatsApp (Rapor + Excel)" else "WhatsApp ile Gönder",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // Copy Button
                        Button(
                            onClick = {
                                viewModel.copyDailyShiftReportToClipboard(context, selectedDate)
                                Toast.makeText(context, "📋 Rapor panoya kopyalandı!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F2942)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(0.9f)
                                .height(44.dp)
                                .testTag("btn_copy_report"),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = iOSBlue, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Kopyala", fontSize = 12.sp, color = iOSBlue, fontWeight = FontWeight.Bold)
                        }
                    }

                    // System Share / Email
                    OutlinedButton(
                        onClick = {
                            viewModel.shareDailyShiftReportGeneral(
                                context = context,
                                targetDateStr = selectedDate,
                                includeExcel = includeExcelWithReport && totalCalls > 0
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .testTag("btn_share_report_email"),
                        border = BorderStroke(0.8.dp, iOSBorder)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = iOSTextPrimary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (includeExcelWithReport && totalCalls > 0) {
                                "E-Posta / Outlook veya Diğer Uygulamalarda Paylaş (Excel Ekli)"
                            } else {
                                "E-Posta / Outlook veya Diğer Uygulamalarda Paylaş"
                            },
                            fontSize = 11.sp,
                            color = iOSTextPrimary
                        )
                    }
                }
            }
        }
    }
}
