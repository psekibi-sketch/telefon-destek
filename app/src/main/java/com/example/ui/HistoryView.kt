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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SupportRecord
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HistoryView(
    viewModel: SupportViewModel,
    onNavigateToHome: () -> Unit = {}
) {
    val context = LocalContext.current
    val records by viewModel.allRecords.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val dailyStats by viewModel.supportDailyStats.collectAsState()
    val deviceReportData by viewModel.deviceReportData.collectAsState()
    var isStatsExpanded by remember { mutableStateOf(false) }
    var searchInput by remember { mutableStateOf(searchQuery) }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Filter period: TODAY (default Bugün / Son 1 Gün), WEEK, MONTH, THREE_MONTHS, ALL, CUSTOM
    var selectedPeriod by remember { mutableStateOf("TODAY") }
    var customStartDate by remember { mutableStateOf(todayDateString()) }
    var customEndDate by remember { mutableStateOf(todayDateString()) }
    var showCustomDatePickers by remember { mutableStateOf(false) }
    var recordToDelete by remember { mutableStateOf<SupportRecord?>(null) }

    // Date filtering helper
    val periodFilteredRecords = remember(records, selectedPeriod, customStartDate, customEndDate) {
        val todayStartMillis = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        when (selectedPeriod) {
            "TODAY" -> {
                val todayStr = todayDateString() // dd.MM.yyyy
                val todayYmd = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                records.filter { r ->
                    val raw = r.baslangicTarihi.ifBlank { r.basTarihi }.trim()
                    raw.startsWith(todayStr) || raw.startsWith(todayYmd) || parseDateTimeToMillis(raw) >= todayStartMillis
                }
            }
            "WEEK" -> {
                val weekStartMillis = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -7)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                records.filter { r ->
                    val raw = r.baslangicTarihi.ifBlank { r.basTarihi }.trim()
                    val m = parseDateTimeToMillis(raw)
                    m >= weekStartMillis
                }
            }
            "MONTH" -> {
                val monthStartMillis = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -30)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                records.filter { r ->
                    val raw = r.baslangicTarihi.ifBlank { r.basTarihi }.trim()
                    val m = parseDateTimeToMillis(raw)
                    m >= monthStartMillis
                }
            }
            "THREE_MONTHS" -> {
                val threeMonthsStartMillis = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -90)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                records.filter { r ->
                    val raw = r.baslangicTarihi.ifBlank { r.basTarihi }.trim()
                    val m = parseDateTimeToMillis(raw)
                    m >= threeMonthsStartMillis
                }
            }
            "CUSTOM" -> {
                val startMillis = parseDateTimeToMillis(customStartDate)
                val endMillis = parseDateTimeToMillis(customEndDate).let {
                    if (it > 0) it + 86400000L - 1L else Long.MAX_VALUE
                }
                records.filter { r ->
                    val raw = r.baslangicTarihi.ifBlank { r.basTarihi }.trim()
                    val m = parseDateTimeToMillis(raw)
                    if (m == 0L) true else m in startMillis..endMillis
                }
            }
            else -> records
        }
    }

    // Text Search filter: When searching by serial/sicil/phone/text, search across ALL records in entire DB!
    val filteredList = remember(records, periodFilteredRecords, searchQuery) {
        val base = if (searchQuery.isBlank()) {
            periodFilteredRecords
        } else {
            val q = searchQuery.trim().lowercase(Locale("tr", "TR"))
            records.filter { r ->
                r.musteriBayiAdi.lowercase(Locale("tr", "TR")).contains(q) ||
                r.gorusulenKisi.lowercase(Locale("tr", "TR")).contains(q) ||
                r.telefon.contains(q) ||
                r.telefon2.contains(q) ||
                r.sicilNo1.lowercase(Locale("tr", "TR")).contains(q) ||
                r.yn500SicilNo1.lowercase(Locale("tr", "TR")).contains(q) ||
                r.destekKonusu1.lowercase(Locale("tr", "TR")).contains(q) ||
                r.tespit1.lowercase(Locale("tr", "TR")).contains(q) ||
                r.islem1.lowercase(Locale("tr", "TR")).contains(q) ||
                r.redmineId1.contains(q) ||
                r.baglantiId1.contains(q)
            }
        }
        base.distinctBy { it.id }.sortedWith(
            compareByDescending<SupportRecord> { parseDateTimeToMillis(it.baslangicTarihi) }
                .thenByDescending { it.id }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(iOSBackground)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        // Günlük Destek Performans Göstergesi (Support KPI Dashboard)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .clickable { isStatsExpanded = !isStatsExpanded },
            colors = CardDefaults.cardColors(containerColor = iOSCardSecondary),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(0.6.dp, iOSBlue.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(iOSBlue.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Analytics, contentDescription = null, tint = iOSBlue, modifier = Modifier.size(14.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Bugünkü Destek: ${dailyStats.todayCallsCount} Çağrı",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = iOSTextPrimary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFF0F2942),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Süre: ${formatSecondsToDisplay(dailyStats.todayTotalDurationSeconds)}",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = iOSBlue,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (isStatsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = iOSTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (isStatsExpanded) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = iOSBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Ortalama Çağrı Süresi
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = iOSCardTertiary,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Ort. Görüşme", fontSize = 9.sp, color = iOSTextSecondary)
                                Text(formatSecondsToDisplay(dailyStats.todayAvgDurationSeconds), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = iOSOrange)
                            }
                        }

                        // Çözülen / Bekleyen
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = iOSCardTertiary,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Çözüm Oranı", fontSize = 9.sp, color = iOSTextSecondary)
                                val rate = if (dailyStats.todayCallsCount > 0) (dailyStats.todayResolvedCount * 100 / dailyStats.todayCallsCount) else 100
                                Text("%$rate", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = ExcelGreen)
                            }
                        }

                        // En Sık Konu
                        Surface(
                            modifier = Modifier.weight(1.2f),
                            color = iOSCardTertiary,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("En Çok Gelen", fontSize = 9.sp, color = iOSTextSecondary)
                                Text(dailyStats.topTopic.take(14), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = iOSTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = { viewModel.openStatisticsDialog() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F2942)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(32.dp),
                            contentPadding = PaddingValues(horizontal = 3.dp)
                        ) {
                            Icon(Icons.Default.BarChart, contentDescription = null, tint = iOSBlue, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("İstatistik", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = iOSBlue)
                        }

                        Button(
                            onClick = { viewModel.openDailyReportDialog() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2818)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.1f).height(32.dp),
                            contentPadding = PaddingValues(horizontal = 3.dp)
                        ) {
                            Icon(Icons.Default.Assessment, contentDescription = null, tint = Color(0xFF4ADE80), modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Gün Sonu", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4ADE80))
                        }

                        Button(
                            onClick = { viewModel.openServiceManagerDialog() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.2f).height(32.dp),
                            contentPadding = PaddingValues(horizontal = 3.dp)
                        ) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Color(0xFF93C5FD), modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("👔 Müdür Paneli", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF93C5FD))
                        }
                    }
                }
            }
        }

        // Top Filter Chips Row (Tek satır yatay kaydırılabilir)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val periods = listOf(
                "TODAY" to "Bugün (Varsayılan)",
                "WEEK" to "Son 1 Hafta",
                "MONTH" to "Son 1 Ay",
                "THREE_MONTHS" to "Son 3 Ay",
                "ALL" to "Tümü",
                "CUSTOM" to "Özel Tarih"
            )

            periods.forEach { (key, label) ->
                val isSelected = selectedPeriod == key
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) iOSBlue else iOSCardSecondary,
                    border = BorderStroke(0.5.dp, if (isSelected) iOSBlue else iOSBorder),
                    modifier = Modifier.clickable {
                        selectedPeriod = key
                        showCustomDatePickers = (key == "CUSTOM")
                    }
                ) {
                    Text(
                        text = label,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else iOSTextPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Custom Date Range Inputs
        if (showCustomDatePickers) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(containerColor = iOSCardSecondary),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(0.5.dp, iOSBorder)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customStartDate,
                        onValueChange = { customStartDate = it },
                        label = { Text("Başlangıç (GG.AA.YYYY)", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = formTextFieldColors()
                    )
                    OutlinedTextField(
                        value = customEndDate,
                        onValueChange = { customEndDate = it },
                        label = { Text("Bitiş (GG.AA.YYYY)", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = formTextFieldColors()
                    )
                }
            }
        }

        // Search Box & Actions Row (Kelimenin tamamı yazılınca Ara butonuna basarak arama)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchInput,
                onValueChange = { searchInput = it },
                placeholder = { Text("Firma, kişi, seri/sicil no, tel...", fontSize = 11.5.sp, color = iOSTextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = iOSTextSecondary, modifier = Modifier.size(16.dp)) },
                trailingIcon = {
                    if (searchInput.isNotBlank()) {
                        IconButton(onClick = {
                            searchInput = ""
                            viewModel.setSearchQuery("")
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Temizle", modifier = Modifier.size(14.dp))
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                        viewModel.setSearchQuery(searchInput)
                    }
                ),
                modifier = Modifier.weight(1f),
                colors = formTextFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )

            // Ara Butonu
            Button(
                onClick = {
                    keyboardController?.hide()
                    viewModel.setSearchQuery(searchInput)
                },
                colors = ButtonDefaults.buttonColors(containerColor = iOSBlue),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                modifier = Modifier.testTag("button_search_submit")
            ) {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Ara", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            // Excel Dışa Aktar Butonu
            Button(
                onClick = { viewModel.openExportDialog() },
                colors = ButtonDefaults.buttonColors(containerColor = iOSCardSecondary),
                border = BorderStroke(1.dp, iOSBlue.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
            ) {
                Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(14.dp), tint = iOSBlue)
                Spacer(modifier = Modifier.width(3.dp))
                Text("Excel", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = iOSBlue)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Record List & Scrollable Summary/Report (Aşağı kaydırıldığında özet ve servis raporu da yukarı kayar)
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = iOSTextSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (records.isEmpty()) "Henüz kayıt bulunmuyor." else "Filtreye uygun kayıt bulunamadı.",
                        color = iOSTextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            val isSearchActive = searchQuery.isNotBlank()
            val totalFilteredDuration = remember(filteredList) {
                filteredList.sumOf { parseDurationToSecondsHelper(it.toplamSure) }
            }
            val resolvedCount = remember(filteredList) {
                filteredList.count { it.bekleyenIs.equals("Hayır", ignoreCase = true) || it.bekleyenIs.isBlank() }
            }
            val pendingCount = filteredList.size - resolvedCount
            val topIssue = remember(filteredList) {
                filteredList.map { it.tespit1.trim().ifBlank { it.destekKonusu1.trim() } }
                    .filter { it.isNotBlank() }
                    .groupingBy { it }
                    .eachCount()
                    .maxByOrNull { it.value }?.key ?: "Belirtilmemiş"
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // 1. Cihazın Geçmişini ve Tüm Geçmişi Anlatan Başta Kısa & Kompakt Özet Kartı (Listeyle Birlikte Kayar)
                item(key = "header_summary_card") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isSearchActive) Color(0xFF0C2440) else iOSCardSecondary),
                        border = BorderStroke(0.7.dp, if (isSearchActive) Color(0xFF38BDF8) else iOSBorder)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = if (isSearchActive) Icons.Default.DevicesOther else Icons.Default.Summarize,
                                        contentDescription = null,
                                        tint = if (isSearchActive) Color(0xFF38BDF8) else iOSBlue,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (isSearchActive) "🔍 Arama & Cihaz Özeti: \"$searchQuery\"" else "📋 Liste & Genel Özet",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSearchActive) Color(0xFFBAE6FD) else iOSTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (isSearchActive) {
                                    TextButton(
                                        onClick = {
                                            searchInput = ""
                                            viewModel.setSearchQuery("")
                                        },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                        modifier = Modifier.height(20.dp)
                                    ) {
                                        Text("Temizle", fontSize = 10.sp, color = iOSOrange)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Kompakt 3 İstatistik Kutucuğu
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(6.dp),
                                    color = iOSCardTertiary
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Kayıt:", fontSize = 9.sp, color = iOSTextSecondary)
                                        Text("${filteredList.size}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = iOSTextPrimary)
                                    }
                                }
                                Surface(
                                    modifier = Modifier.weight(1.3f),
                                    shape = RoundedCornerShape(6.dp),
                                    color = iOSCardTertiary
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Durum:", fontSize = 9.sp, color = iOSTextSecondary)
                                        Text(
                                            "$resolvedCount Çözüldü" + if (pendingCount > 0) " / $pendingCount B." else "",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (pendingCount > 0) iOSOrange else ExcelGreen
                                        )
                                    }
                                }
                                Surface(
                                    modifier = Modifier.weight(1.2f),
                                    shape = RoundedCornerShape(6.dp),
                                    color = iOSCardTertiary
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Süre:", fontSize = 9.sp, color = iOSTextSecondary)
                                        Text(formatSecondsToDisplay(totalFilteredDuration), fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = ExcelGreen)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(3.dp))

                            Text(
                                text = "⚡ En Sık Tespit: ${topIssue.take(35)}",
                                fontSize = 9.5.sp,
                                color = iOSTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // 2. Cihaz Seçili Servis Raporu Kartı (Listeyle Birlikte Yukarı Kayar)
                if (searchQuery.isNotBlank()) {
                    item(key = "header_device_report_banner") {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E3A8A),
                            border = BorderStroke(1.dp, Color(0xFF60A5FA)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.openDeviceReport(searchQuery, filteredList)
                                }
                                .testTag("button_open_device_report_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF3B82F6)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Assessment, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "📊 Cihaz Servis Raporunu Aç (${filteredList.size} Kayıt)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Arıza dökümü, analiz ve paylaşım",
                                            fontSize = 9.sp,
                                            color = Color(0xFFBFDBFE)
                                        )
                                    }
                                }
                                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                }

                // 3. Kayıt Sayısı Başlığı (Listeyle Birlikte Kayar)
                item(key = "header_count_banner") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${filteredList.size} Kayıt Listeleniyor",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = iOSTextSecondary
                        )
                        Text(
                            text = "Toplam: ${records.size}",
                            fontSize = 10.5.sp,
                            color = iOSTextSecondary.copy(alpha = 0.7f)
                        )
                    }
                }

                items(filteredList, key = { it.id }) { record ->
                    HistoryRecordCard(
                        record = record,
                        viewModel = viewModel,
                        onEdit = {
                            viewModel.loadRecordForEditing(record)
                            viewModel.setBottomNavTab(0) // Switch to Ana Ekran
                            onNavigateToHome()
                        },
                        onDelete = { recordToDelete = record },
                        onDirectCall = { phone ->
                            viewModel.startVerimorSipCall(phone, record.gorusulenKisi)
                        },
                        onWhatsAppShare = {
                            viewModel.shareRecordViaWhatsApp(context, record)
                        },
                        onOpenDeviceReport = {
                            val target = record.sicilNo1.ifBlank { record.musteriBayiAdi }
                            viewModel.openDeviceReport(target)
                        }
                    )
                }
            }
        }
    }

    // Cihaz & Sicil Servis Raporu Dialogu
    if (deviceReportData != null) {
        DeviceServiceReportDialog(
            data = deviceReportData!!,
            viewModel = viewModel,
            onDismiss = { viewModel.closeDeviceReport() }
        )
    }

    // Delete Confirmation Dialog
    if (recordToDelete != null) {
        val target = recordToDelete!!
        AlertDialog(
            onDismissRequest = { recordToDelete = null },
            title = { Text("Kaydı Sil", fontWeight = FontWeight.Bold) },
            text = { Text("#${target.id} numaralı '${target.musteriBayiAdi}' destek kaydı silinsin mi?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteRecord(target)
                        recordToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = iOSRed)
                ) {
                    Text("Sil", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { recordToDelete = null }) {
                    Text("İptal")
                }
            }
        )
    }
}

@Composable
fun HistoryRecordCard(
    record: SupportRecord,
    viewModel: SupportViewModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDirectCall: (String) -> Unit,
    onWhatsAppShare: () -> Unit = {},
    onOpenDeviceReport: () -> Unit = {}
) {
    var isExpanded by remember { mutableStateOf(false) }

    // 5. Destek liste kartlarında firma adının yanına "Görüşülen Kişi" (Firma - Kişi) eklensin
    val titleText = if (record.gorusulenKisi.isNotBlank()) {
        "${record.musteriBayiAdi.ifBlank { "İsimsiz Firma" }} - ${record.gorusulenKisi}"
    } else {
        record.musteriBayiAdi.ifBlank { "İsimsiz Firma" }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(containerColor = iOSCardSecondary),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(0.5.dp, if (isExpanded) iOSBlue.copy(alpha = 0.6f) else iOSBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header Row: Title (Firma - Kişi) and Sync Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = titleText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    color = iOSTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))

                // Sync badge
                Surface(
                    color = if (record.isSynced) Color(0xFF0F2942) else Color(0xFF451A03),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(0.5.dp, if (record.isSynced) iOSBlue else iOSOrange)
                ) {
                    Text(
                        text = if (record.isSynced) "SQL Aktarıldı" else "Bekliyor",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (record.isSynced) iOSBlue else Color(0xFFFDBA74),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle: Sicil No, Konu, Tarih, Süre
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (record.sicilNo1.isNotBlank()) {
                    Surface(
                        color = Color(0xFF1E3A8A),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(0.5.dp, Color(0xFF60A5FA)),
                        modifier = Modifier.clickable { onOpenDeviceReport() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = record.sicilNo1,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF93C5FD)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(Icons.Default.Assessment, contentDescription = "Rapor", tint = Color(0xFF93C5FD), modifier = Modifier.size(10.dp))
                        }
                    }
                }
                if (record.destekKonusu1.isNotBlank()) {
                    Text(
                        text = record.destekKonusu1,
                        fontSize = 11.5.sp,
                        color = iOSTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                Text(
                    text = "${record.baslangicTarihi.take(16)} (${record.toplamSure})",
                    fontSize = 10.5.sp,
                    color = iOSTextTertiary
                )
            }

            // Expanded Details Section
            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = iOSBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val repName = viewModel.resolveRepresentativeName(record.kullaniciAdi.ifBlank { record.kullaniciKodu })
                    if (repName.isNotBlank() && repName != "Belirtilmemiş") {
                        Row {
                            Text("👨‍💼 Temsilci: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA855F7))
                            Text(repName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = iOSTextPrimary)
                        }
                    }
                    if (record.tespit1.isNotBlank()) {
                        Row {
                            Text("Tespit: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = iOSOrange)
                            Text(record.tespit1, fontSize = 11.sp, color = iOSTextPrimary)
                        }
                    }
                    if (record.islem1.isNotBlank()) {
                        Row {
                            Text("İşlem: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ExcelGreen)
                            Text(record.islem1, fontSize = 11.sp, color = iOSTextPrimary)
                        }
                    }
                    if (record.telefon2.isNotBlank() || record.mail.isNotBlank()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (record.telefon2.isNotBlank()) {
                                Text("Tel 2: ${record.telefon2}", fontSize = 10.5.sp, color = iOSTextSecondary)
                            }
                            if (record.mail.isNotBlank()) {
                                Text("Mail: ${record.mail}", fontSize = 10.5.sp, color = iOSTextSecondary)
                            }
                        }
                    }
                    if (record.baglantiId1.isNotBlank() || record.redmineId1.isNotBlank()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (record.baglantiId1.isNotBlank()) {
                                Text("Bağlantı ID: ${record.baglantiId1}", fontSize = 10.5.sp, color = iOSBlue)
                            }
                            if (record.redmineId1.isNotBlank()) {
                                Text("Redmine: ${record.redmineId1}", fontSize = 10.5.sp, color = iOSRed)
                            }
                        }
                    }
                    if (record.kasaVersiyon1.isNotBlank() || record.arkaOfisVersiyon1.isNotBlank()) {
                        Text(
                            "Versiyonlar: Kasa: ${record.kasaVersiyon1} | Arka Ofis: ${record.arkaOfisVersiyon1}",
                            fontSize = 10.sp,
                            color = iOSTextTertiary
                        )
                    }

                    // Cihaz Servis Raporu Butonu (Expanded içinde)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = onOpenDeviceReport,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = iOSBlue),
                        border = BorderStroke(0.8.dp, iOSBlue.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(32.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Assessment, contentDescription = null, tint = iOSBlue, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Bu Cihazın / Sicilin Tüm Servis Geçmişi Raporunu Aç",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = iOSBlue
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Action Row: Direct Phone Call, Edit Button, Delete Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Numaraya tıklandığında doğrudan SIP araması başlatılabilsin
                if (record.telefon.isNotBlank()) {
                    Surface(
                        color = Color(0xFF0F2942),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(0.5.dp, iOSBlue),
                        modifier = Modifier.clickable { onDirectCall(record.telefon) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = "Ara", tint = iOSBlue, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = record.telefon,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = iOSBlue
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    // WhatsApp Servis Bilgisi Paylaş Butonu
                    Button(
                        onClick = onWhatsAppShare,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 7.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "WhatsApp", tint = Color.White, modifier = Modifier.size(11.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("WhatsApp", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    // Dedicated Edit Button: Only edits when explicitly tapped
                    OutlinedButton(
                        onClick = onEdit,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = iOSOrange),
                        border = BorderStroke(1.dp, iOSOrange.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Düzenle", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Sil", tint = iOSRed.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

private fun dialNumberDirect(context: Context, rawPhone: String) {
    val clean = rawPhone.replace(Regex("[^0-9+]"), "").trim()
    if (clean.isBlank()) return
    try {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}

private fun todayDateString(): String = SimpleDateFormat("dd.MM.yyyy", Locale("tr", "TR")).format(Date())

private fun parseDateTimeToMillis(str: String): Long {
    val clean = str.trim()
    if (clean.isBlank()) return 0L
    val fullPatterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm",
        "yyyy-MM-dd HH:mm:ss",
        "dd.MM.yyyy HH:mm:ss",
        "dd/MM/yyyy HH:mm:ss",
        "yyyy-MM-dd HH:mm",
        "dd.MM.yyyy HH:mm",
        "dd/MM/yyyy HH:mm",
        "yyyy-MM-dd",
        "dd.MM.yyyy",
        "dd/MM/yyyy"
    )
    for (p in fullPatterns) {
        try {
            val sdf = SimpleDateFormat(p, Locale.ROOT).apply { isLenient = true }
            val date = sdf.parse(clean)
            if (date != null) return date.time
        } catch (_: Exception) {}
    }
    val dmyMatch = Regex("""(\d{1,2})[./-](\d{1,2})[./-](\d{4})""").find(clean)
    if (dmyMatch != null) {
        val (d, m, y) = dmyMatch.destructured
        try {
            val cal = Calendar.getInstance().apply {
                set(y.toInt(), m.toInt() - 1, d.toInt(), 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis
        } catch (_: Exception) {}
    }
    val ymdMatch = Regex("""(\d{4})[./-](\d{1,2})[./-](\d{1,2})""").find(clean)
    if (ymdMatch != null) {
        val (y, m, d) = ymdMatch.destructured
        try {
            val cal = Calendar.getInstance().apply {
                set(y.toInt(), m.toInt() - 1, d.toInt(), 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis
        } catch (_: Exception) {}
    }
    return 0L
}

private fun parseDateFlexible(str: String): Date? {
    val m = parseDateTimeToMillis(str)
    return if (m > 0) Date(m) else null
}

private fun formatSecondsToDisplay(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0) "${m} dk ${s} sn" else "${s} sn"
}

private fun parseDurationToSecondsHelper(durationStr: String): Long {
    val clean = durationStr.trim()
    val parts = clean.split(":")
    return when (parts.size) {
        2 -> {
            val m = parts[0].toLongOrNull() ?: 0L
            val s = parts[1].toLongOrNull() ?: 0L
            m * 60 + s
        }
        3 -> {
            val h = parts[0].toLongOrNull() ?: 0L
            val m = parts[1].toLongOrNull() ?: 0L
            val s = parts[2].toLongOrNull() ?: 0L
            h * 3600 + m * 60 + s
        }
        else -> 0L
    }
}
