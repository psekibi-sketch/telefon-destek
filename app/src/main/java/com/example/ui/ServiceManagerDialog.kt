package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.SupportRecord
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ManagerPeriod(val label: String) {
    TODAY("Bugün"),
    YESTERDAY("Dün"),
    THIS_WEEK("Bu Hafta"),
    LAST_7_DAYS("Son 7 Gün"),
    THIS_MONTH("Bu Ay"),
    LAST_30_DAYS("Son 30 Gün"),
    ALL("Tümü"),
    CUSTOM("Özel Tarih...")
}

@Composable
fun ServiceManagerDialog(
    viewModel: SupportViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val allRecords by viewModel.allRecords.collectAsState()
    val isFetchingFromSql by viewModel.isFetchingFromSql.collectAsState()
    val representatives by viewModel.representatives.collectAsState()

    var selectedPeriod by remember { mutableStateOf(ManagerPeriod.TODAY) }
    var customStartDate by remember { mutableStateOf(viewModel.todayDateString()) }
    var customEndDate by remember { mutableStateOf(viewModel.todayDateString()) }
    var activeDatePicker by remember { mutableStateOf<String?>(null) } // "start", "end", or null

    // Search and secondary filters
    var selectedUserFilter by remember { mutableStateOf("TÜMÜ") }
    var searchCompanyQuery by remember { mutableStateOf("") }
    var searchSicilQuery by remember { mutableStateOf("") }
    var searchTopicQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("TÜMÜ") } // TÜMÜ, ÇÖZÜLEN, BEKLEYEN
    var showFiltersPanel by remember { mutableStateOf(false) }

    var activeTab by remember { mutableStateOf(0) } // 0: Temsilciler, 1: Grafikler, 2: Çağrı Listesi, 3: Rapor Önizleme
    var includeExcelInShare by remember { mutableStateOf(true) }

    var showSqlFetchOptionsDialog by remember { mutableStateOf(false) }
    var showMemoryCleanDialog by remember { mutableStateOf(false) }
    var showServerCodeHelpDialog by remember { mutableStateOf(false) }

    // File launcher for JSON backup import
    val jsonFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.loadBackupForPreview(context, it)
        }
    }

    // Date calculations for filters
    val sdfYmd = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    val periodDateRange = remember(selectedPeriod, customStartDate, customEndDate) {
        val cal = Calendar.getInstance()
        val todayStr = sdfYmd.format(cal.time)

        when (selectedPeriod) {
            ManagerPeriod.TODAY -> Pair(todayStr, todayStr)
            ManagerPeriod.YESTERDAY -> {
                cal.add(Calendar.DAY_OF_YEAR, -1)
                val yStr = sdfYmd.format(cal.time)
                Pair(yStr, yStr)
            }
            ManagerPeriod.THIS_WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                val mondayStr = sdfYmd.format(cal.time)
                Pair(mondayStr, todayStr)
            }
            ManagerPeriod.LAST_7_DAYS -> {
                cal.add(Calendar.DAY_OF_YEAR, -6)
                val past7Str = sdfYmd.format(cal.time)
                Pair(past7Str, todayStr)
            }
            ManagerPeriod.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val monthStart = sdfYmd.format(cal.time)
                Pair(monthStart, todayStr)
            }
            ManagerPeriod.LAST_30_DAYS -> {
                cal.add(Calendar.DAY_OF_YEAR, -29)
                val past30Str = sdfYmd.format(cal.time)
                Pair(past30Str, todayStr)
            }
            ManagerPeriod.ALL -> Pair("2020-01-01", "2035-12-31")
            ManagerPeriod.CUSTOM -> Pair(customStartDate, customEndDate)
        }
    }

    val periodTitle = remember(selectedPeriod, periodDateRange) {
        if (selectedPeriod == ManagerPeriod.CUSTOM) {
            "${viewModel.formatYmdToDisplay(periodDateRange.first)} - ${viewModel.formatYmdToDisplay(periodDateRange.second)}"
        } else {
            "${selectedPeriod.label} (${viewModel.formatYmdToDisplay(periodDateRange.first)} - ${viewModel.formatYmdToDisplay(periodDateRange.second)})"
        }
    }

    // Base records for the selected period and text search (BEFORE representative filtering)
    val baseRecordsForPeriod = remember(
        allRecords,
        periodDateRange,
        searchCompanyQuery,
        searchSicilQuery,
        searchTopicQuery,
        statusFilter
    ) {
        val (start, end) = periodDateRange
        val startYmd = normalizeToYmd(start)
        val endYmd = normalizeToYmd(end)
        val isSearchingText = searchCompanyQuery.isNotBlank() || searchSicilQuery.isNotBlank() || searchTopicQuery.isNotBlank()

        allRecords.filter { record ->
            // Date filter (Flexible format support: yyyy-MM-dd or dd.MM.yyyy) - Bypassed if searching text!
            if (!isSearchingText) {
                val rawDate = record.baslangicTarihi.ifBlank { record.basTarihi }
                val recordYmd = normalizeToYmd(rawDate)
                val dateMatches = if (recordYmd.isNotBlank() && startYmd.isNotBlank() && endYmd.isNotBlank()) {
                    recordYmd in startYmd..endYmd
                } else {
                    true
                }
                if (!dateMatches) return@filter false
            }

            // Company filter
            if (searchCompanyQuery.isNotBlank()) {
                if (!record.musteriBayiAdi.contains(searchCompanyQuery, ignoreCase = true)) return@filter false
            }

            // Sicil / Seri No filter
            if (searchSicilQuery.isNotBlank()) {
                val s1 = record.sicilNo1
                val s2 = record.yn500SicilNo1
                if (!s1.contains(searchSicilQuery, ignoreCase = true) && !s2.contains(searchSicilQuery, ignoreCase = true)) return@filter false
            }

            // Topic / issue filter
            if (searchTopicQuery.isNotBlank()) {
                val t1 = record.destekKonusu1
                val t2 = record.tespit1
                val t3 = record.islem1
                if (!t1.contains(searchTopicQuery, ignoreCase = true) &&
                    !t2.contains(searchTopicQuery, ignoreCase = true) &&
                    !t3.contains(searchTopicQuery, ignoreCase = true)
                ) return@filter false
            }

            // Status filter
            if (statusFilter == "ÇÖZÜLEN") {
                if (record.bekleyenIs.equals("Evet", ignoreCase = true)) return@filter false
            } else if (statusFilter == "BEKLEYEN") {
                if (!record.bekleyenIs.equals("Evet", ignoreCase = true)) return@filter false
            }

            true
        }.distinctBy { it.id }.sortedWith(
            compareByDescending<SupportRecord> { parseFlexibleDateToMillis(it.baslangicTarihi.ifBlank { it.basTarihi }) }
                .thenByDescending { it.id }
        )
    }

    // Filter records: When representative filter is active
    val filteredRecords = remember(baseRecordsForPeriod, selectedUserFilter, representatives) {
        if (selectedUserFilter == "TÜMÜ") {
            baseRecordsForPeriod
        } else {
            baseRecordsForPeriod.filter { record ->
                val resolved = viewModel.resolveRepresentativeName(record.kullaniciAdi.ifBlank { record.kullaniciKodu })
                resolved.equals(selectedUserFilter, ignoreCase = true) ||
                record.kullaniciAdi.equals(selectedUserFilter, ignoreCase = true) ||
                record.kullaniciKodu.equals(selectedUserFilter, ignoreCase = true)
            }
        }
    }

    // List of unique users in all records (mapped with settings names)
    val allUniqueUsers = remember(allRecords, representatives) {
        allRecords.map { viewModel.resolveRepresentativeName(it.kullaniciAdi.ifBlank { it.kullaniciKodu }) }
            .filter { it.isNotBlank() && it != "Belirtilmemiş" }
            .distinct()
            .sorted()
    }

    // Metrics based on filtered records
    val totalCalls = filteredRecords.size
    val totalSeconds: Long = remember(filteredRecords) {
        filteredRecords.sumOf { viewModel.parseDurationToSeconds(it.toplamSure) }
    }
    val avgSeconds: Long = remember(totalCalls, totalSeconds) {
        if (totalCalls > 0) totalSeconds / totalCalls else 0L
    }
    val resolvedCount = remember(filteredRecords) {
        filteredRecords.count { it.bekleyenIs.equals("Hayır", ignoreCase = true) || it.bekleyenIs.isBlank() }
    }
    val pendingCount = totalCalls - resolvedCount
    val resolutionRate = remember(totalCalls, resolvedCount) {
        if (totalCalls > 0) (resolvedCount * 100 / totalCalls) else 100
    }

    // User breakdown list (Computed from all records of the period, using settings representative names)
    val userPerformanceList = remember(baseRecordsForPeriod, representatives) {
        baseRecordsForPeriod.groupBy { viewModel.resolveRepresentativeName(it.kullaniciAdi.ifBlank { it.kullaniciKodu }) }
            .map { (userName, uRecords) ->
                val uTotal = uRecords.size
                val uSecs: Long = uRecords.sumOf { viewModel.parseDurationToSeconds(it.toplamSure) }
                val uAvg: Long = if (uTotal > 0) uSecs / uTotal else 0L
                val uResolved = uRecords.count { it.bekleyenIs.equals("Hayır", ignoreCase = true) || it.bekleyenIs.isBlank() }
                val uPending = uTotal - uResolved
                val uRate = if (uTotal > 0) (uResolved * 100 / uTotal) else 100
                val topUserTopic = uRecords.map { it.destekKonusu1.ifBlank { it.tespit1 } }
                    .filter { it.isNotBlank() }
                    .groupingBy { it }
                    .eachCount()
                    .maxByOrNull { it.value }?.key ?: "-"

                UserPerfData(
                    name = userName,
                    totalCalls = uTotal,
                    totalSeconds = uSecs,
                    avgSeconds = uAvg,
                    resolvedCount = uResolved,
                    pendingCount = uPending,
                    resolutionRate = uRate,
                    topTopic = topUserTopic
                )
            }.sortedByDescending { it.totalCalls }
    }

    // Date picker dialog
    if (activeDatePicker != null) {
        AppComposeDatePickerDialog(
            initialDateYmd = if (activeDatePicker == "start") customStartDate else customEndDate,
            title = if (activeDatePicker == "start") "Başlangıç Tarihi Seç" else "Bitiş Tarihi Seç",
            onDateSelected = { selected ->
                if (activeDatePicker == "start") customStartDate = selected else customEndDate = selected
                selectedPeriod = ManagerPeriod.CUSTOM
                activeDatePicker = null
            },
            onDismiss = { activeDatePicker = null }
        )
    }

    // Tarih Bazlı SQL Veri Çekme Seçenekleri Penceresi
    if (showSqlFetchOptionsDialog) {
        SqlFetchOptionsDialog(
            viewModel = viewModel,
            defaultStartDate = periodDateRange.first,
            defaultEndDate = periodDateRange.second,
            onDismiss = { showSqlFetchOptionsDialog = false }
        )
    }

    // Telefon Yerel Hafıza & Önbellek Temizleme Penceresi (Arka Ofise Asla Dokunulmaz)
    if (showMemoryCleanDialog) {
        LocalMemoryCleanDialog(
            viewModel = viewModel,
            allRecordsCount = allRecords.size,
            onDismiss = { showMemoryCleanDialog = false }
        )
    }

    // Sunucu API Okuma Kodu Kılavuzu Penceresi
    if (showServerCodeHelpDialog) {
        ServerApiCodeHelpDialog(onDismiss = { showServerCodeHelpDialog = false })
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(0.98f)
                .fillMaxHeight(0.97f)
                .clip(RoundedCornerShape(18.dp))
                .testTag("service_manager_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // Header Row (Düzgün Hizalanmış, Asla Kırılmayan Başlık)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F2942)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "👔 Servis Müdürü Paneli",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = iOSTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF1E3A8A)
                            ) {
                                Text(
                                    text = "YÖNETİCİ",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF93C5FD),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "SQL Server Senkronizasyon & Analiz",
                            fontSize = 10.5.sp,
                            color = iOSTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // API Kodu Yardım Butonu
                    IconButton(
                        onClick = { showServerCodeHelpDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Code, contentDescription = "API Kodu", tint = Color(0xFF60A5FA), modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat", tint = iOSTextSecondary, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Bar: SQL'den Çek + Hafıza Temizle + Dışarıdan Yükle + Panodan Yapıştır
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = iOSCardSecondary,
                    border = BorderStroke(0.6.dp, iOSBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // SQL Server'dan Tarih Seçerek Çek Butonu
                            Button(
                                onClick = { showSqlFetchOptionsDialog = true },
                                enabled = !isFetchingFromSql,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1.3f).height(34.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp)
                            ) {
                                if (isFetchingFromSql) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Çekiliyor...", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("SQL'den Çek...", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            // Hafıza Temizleme Butonu (Arka Ofise Asla Dokunmaz)
                            OutlinedButton(
                                onClick = { showMemoryCleanDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1.2f).height(34.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp),
                                border = BorderStroke(0.8.dp, Color(0xFF059669))
                            ) {
                                Icon(Icons.Default.CleaningServices, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Hafızayı Temizle", fontSize = 10.sp, color = Color(0xFF059669), fontWeight = FontWeight.SemiBold)
                            }

                            // Dosyadan JSON İçe Aktar (WhatsApp/Mail ile gelen veriler)
                            OutlinedButton(
                                onClick = { jsonFileLauncher.launch("application/json") },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(0.9f).height(34.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                border = BorderStroke(0.8.dp, iOSBlue)
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null, tint = iOSBlue, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Dosya", fontSize = 10.sp, color = iOSBlue, fontWeight = FontWeight.SemiBold)
                            }

                            // Panodan JSON Yapıştır
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val clipText = clipboard?.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
                                    if (clipText.isNotBlank()) {
                                        viewModel.importJsonRecords(clipText)
                                    } else {
                                        Toast.makeText(context, "Pano boş! Önce kopyalanan JSON metnini hazırlayın.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(0.9f).height(34.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp),
                                border = BorderStroke(0.8.dp, iOSOrange)
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = null, tint = iOSOrange, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Pano", fontSize = 10.sp, color = iOSOrange, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Period Selector Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ManagerPeriod.values().forEach { period ->
                        val isSelected = selectedPeriod == period
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) iOSBlue else Color(0xFF0F172A),
                            border = BorderStroke(0.8.dp, if (isSelected) Color(0xFF60A5FA) else iOSBorder),
                            modifier = Modifier.clickable {
                                selectedPeriod = period
                                if (period == ManagerPeriod.CUSTOM) {
                                    activeDatePicker = "start"
                                }
                            }
                        ) {
                            Text(
                                text = period.label,
                                fontSize = 10.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else iOSTextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Filter Toggle Button
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (showFiltersPanel) iOSOrange.copy(alpha = 0.2f) else Color(0xFF1E293B),
                        border = BorderStroke(0.8.dp, if (showFiltersPanel) iOSOrange else iOSBorder),
                        modifier = Modifier.clickable { showFiltersPanel = !showFiltersPanel }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                tint = if (showFiltersPanel) iOSOrange else iOSTextPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (showFiltersPanel) "Filtreleri Gizle" else "Detaylı Filtreler",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (showFiltersPanel) iOSOrange else iOSTextPrimary
                            )
                        }
                    }
                }

                // Expandable Filters Panel
                AnimatedVisibility(visible = showFiltersPanel) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0F172A),
                        border = BorderStroke(0.8.dp, iOSBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            // User Selector & Status Row
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                // Temsilci Dropdown / Chips
                                Column(modifier = Modifier.weight(1.3f)) {
                                    Text("Temsilci Filtresi:", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = iOSBlue)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        val usersWithAll = listOf("TÜMÜ") + allUniqueUsers
                                        usersWithAll.forEach { user ->
                                            val isSel = selectedUserFilter == user
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (isSel) iOSBlue else Color(0xFF1E293B),
                                                modifier = Modifier.clickable { selectedUserFilter = user }
                                            ) {
                                                Text(
                                                    text = user,
                                                    fontSize = 9.5.sp,
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Çözüm Durumu
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("İş Durumu:", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = ExcelGreen)
                                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                        listOf("TÜMÜ", "ÇÖZÜLEN", "BEKLEYEN").forEach { st ->
                                            val isSel = statusFilter == st
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (isSel) ExcelGreen else Color(0xFF1E293B),
                                                modifier = Modifier.clickable { statusFilter = st }
                                            ) {
                                                Text(
                                                    text = st,
                                                    fontSize = 9.sp,
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Search Fields Row
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = searchCompanyQuery,
                                    onValueChange = { searchCompanyQuery = it },
                                    placeholder = { Text("Firma / Bayi Ara...", fontSize = 9.5.sp) },
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    singleLine = true,
                                    colors = formTextFieldColors()
                                )
                                OutlinedTextField(
                                    value = searchSicilQuery,
                                    onValueChange = { searchSicilQuery = it },
                                    placeholder = { Text("Sicil No...", fontSize = 9.5.sp) },
                                    modifier = Modifier.weight(0.8f).height(40.dp),
                                    singleLine = true,
                                    colors = formTextFieldColors()
                                )
                                OutlinedTextField(
                                    value = searchTopicQuery,
                                    onValueChange = { searchTopicQuery = it },
                                    placeholder = { Text("Arıza / Tespit...", fontSize = 9.5.sp) },
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    singleLine = true,
                                    colors = formTextFieldColors()
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // KPI Metric Summary Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MetricChip(
                        title = "Toplam Çağrı",
                        value = "$totalCalls",
                        sub = periodTitle,
                        accentColor = iOSBlue,
                        modifier = Modifier.weight(1f)
                    )
                    MetricChip(
                        title = "Aktif Temsilci",
                        value = "${userPerformanceList.size}",
                        sub = "Kişi",
                        accentColor = Color(0xFFA855F7),
                        modifier = Modifier.weight(0.8f)
                    )
                    MetricChip(
                        title = "Toplam Süre",
                        value = viewModel.formatSecondsToReadable(totalSeconds),
                        sub = "Ort: ${viewModel.formatSecondsToReadable(avgSeconds)}",
                        accentColor = ExcelGreen,
                        modifier = Modifier.weight(1.1f)
                    )
                    MetricChip(
                        title = "Çözüm Oranı",
                        value = "%$resolutionRate",
                        sub = "$resolvedCount Çöz / $pendingCount Bek",
                        accentColor = if (resolutionRate >= 80) Color(0xFF22C55E) else iOSOrange,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Aktif Temsilci Filtresi Varsa "Geri Topla / Tümünü Göster" Uyarısı ve Butonu
                if (selectedUserFilter != "TÜMÜ") {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, Color(0xFF3B82F6)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.FilterAlt, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "Filtrelenen Temsilci: $selectedUserFilter",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${filteredRecords.size} çağrı listeleniyor",
                                        fontSize = 9.5.sp,
                                        color = Color(0xFF93C5FD)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = { selectedUserFilter = "TÜMÜ" },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Geri Topla / Tümünü Göster", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                // Tabs: 0: Temsilciler, 1: Grafikler, 2: Çağrılar, 3: Yönetici Raporu
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = Color(0xFF0F172A),
                    contentColor = iOSBlue,
                    divider = {}
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("👥 Temsilciler (${userPerformanceList.size})", fontSize = 10.5.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("📊 Grafikler", fontSize = 10.5.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        text = { Text("📋 Kayıtlar ($totalCalls)", fontSize = 10.5.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = activeTab == 3,
                        onClick = { activeTab = 3 },
                        text = { Text("📝 Rapor Önizle", fontSize = 10.5.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Main Content Area based on Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (activeTab) {
                        0 -> UserPerformanceView(
                            users = userPerformanceList,
                            allPeriodRecords = baseRecordsForPeriod,
                            selectedUserFilter = selectedUserFilter,
                            totalCalls = baseRecordsForPeriod.size,
                            viewModel = viewModel,
                            onSelectUser = { uName ->
                                selectedUserFilter = uName
                                activeTab = 2
                            },
                            onClearUserFilter = { selectedUserFilter = "TÜMÜ" }
                        )
                        1 -> ManagerChartsView(
                            records = filteredRecords,
                            users = userPerformanceList,
                            totalCalls = totalCalls,
                            viewModel = viewModel
                        )
                        2 -> ManagerCallsListView(
                            records = filteredRecords,
                            selectedUserFilter = selectedUserFilter,
                            viewModel = viewModel,
                            onClearUserFilter = { selectedUserFilter = "TÜMÜ" }
                        )
                        3 -> ManagerReportPreviewView(
                            records = filteredRecords,
                            periodTitle = periodTitle,
                            viewModel = viewModel,
                            includeExcel = includeExcelInShare,
                            onToggleExcel = { includeExcelInShare = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Sharing & Export Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // WhatsApp Executive Report Button
                    Button(
                        onClick = {
                            viewModel.shareServiceManagerReportViaWhatsApp(
                                context = context,
                                records = filteredRecords,
                                periodTitle = periodTitle,
                                includeExcel = includeExcelInShare
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.3f).height(42.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (includeExcelInShare) "WhatsApp (Müdür Raporu + Excel)" else "WhatsApp ile Gönder",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Excel Export Button (.xlsx)
                    Button(
                        onClick = {
                            viewModel.exportServiceManagerExcel(
                                context = context,
                                records = filteredRecords,
                                filenameSuffix = "Mudur_Raporu_${periodDateRange.first}_${periodDateRange.second}"
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ExcelGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(42.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Excel (.xlsx)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    // Copy Summary Button
                    Button(
                        onClick = {
                            val text = viewModel.generateServiceManagerReport(filteredRecords, periodTitle)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            val clip = ClipData.newPlainText("Müdür Raporu", text)
                            clipboard?.setPrimaryClip(clip)
                            Toast.makeText(context, "📋 Müdür raporu panoya kopyalandı!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F2942)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(0.7f).height(42.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = iOSBlue, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Kopyala", fontSize = 11.sp, color = iOSBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

data class UserPerfData(
    val name: String,
    val totalCalls: Int,
    val totalSeconds: Long,
    val avgSeconds: Long,
    val resolvedCount: Int,
    val pendingCount: Int,
    val resolutionRate: Int,
    val topTopic: String
)

@Composable
fun MetricChip(
    title: String,
    value: String,
    sub: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = iOSCardSecondary,
        border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 9.sp, color = iOSTextSecondary, maxLines = 1)
            Text(value, fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold, color = accentColor)
            Text(sub, fontSize = 8.5.sp, color = iOSTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun UserPerformanceView(
    users: List<UserPerfData>,
    allPeriodRecords: List<SupportRecord>,
    selectedUserFilter: String,
    totalCalls: Int,
    viewModel: SupportViewModel,
    onSelectUser: (String) -> Unit,
    onClearUserFilter: () -> Unit
) {
    if (users.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Seçilen dönemde temsilci çağrı kaydı bulunamadı.", color = iOSTextSecondary, fontSize = 12.sp)
        }
        return
    }

    var expandedUsers by remember { mutableStateOf(setOf<String>()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Aktif Filtre Bilgisi ve Geri Toplama Butonu (En Üstte)
        if (selectedUserFilter != "TÜMÜ") {
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.dp, Color(0xFF3B82F6)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.FilterAlt, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Şu an '$selectedUserFilter' filtrelenmiş",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = onClearUserFilter,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Geri Topla / Tümünü Göster", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        items(users) { user ->
            val isExpanded = expandedUsers.contains(user.name)
            val isCurrentFiltered = selectedUserFilter == user.name
            val sharePct = if (totalCalls > 0) (user.totalCalls * 100f / totalCalls) else 0f
            val medal = when (users.indexOf(user)) {
                0 -> "🥇"
                1 -> "🥈"
                2 -> "🥉"
                else -> "${users.indexOf(user) + 1}."
            }

            // Temsilcinin bu dönemdeki tüm kayıtları
            val userRecords = remember(allPeriodRecords, user.name) {
                allPeriodRecords.filter { r ->
                    val resolved = viewModel.resolveRepresentativeName(r.kullaniciAdi.ifBlank { r.kullaniciKodu })
                    resolved.equals(user.name, ignoreCase = true) ||
                    r.kullaniciAdi.equals(user.name, ignoreCase = true) ||
                    r.kullaniciKodu.equals(user.name, ignoreCase = true)
                }
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrentFiltered) Color(0xFF0F2942) else iOSCardSecondary
                ),
                border = BorderStroke(
                    if (isCurrentFiltered) 1.5.dp else if (isExpanded) 1.dp else 0.8.dp,
                    if (isCurrentFiltered) Color(0xFF60A5FA) else if (isExpanded) iOSBlue.copy(alpha = 0.7f) else iOSBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    // Üst Başlık Satırı (Temsilci Adı, Çağrı Sayısı, Aç/Kapat)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedUsers = if (isExpanded) {
                                    expandedUsers - user.name
                                } else {
                                    expandedUsers + user.name
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(medal, fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = user.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = iOSTextPrimary
                                    )
                                    if (isCurrentFiltered) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFF1E3A8A)
                                        ) {
                                            Text(
                                                text = "FİLTRELİ",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF93C5FD),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = if (isExpanded) "Dokun: Destekleri Geri Topla (Kapat)" else "Dokun: Destekleri Aşağı Aç (${user.totalCalls} Kayıt)",
                                    fontSize = 9.sp,
                                    color = if (isExpanded) iOSOrange else iOSBlue
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${user.totalCalls} Çağrı",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = iOSBlue
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF1E293B)
                            ) {
                                Text(
                                    text = "%${sharePct.toInt()}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF60A5FA),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "Geri Topla" else "Aşağı Aç",
                                tint = if (isExpanded) iOSOrange else iOSTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Progress bar for call share
                    LinearProgressIndicator(
                        progress = { (sharePct / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = iOSBlue,
                        trackColor = Color(0xFF1E293B)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⏱ Toplam: ${viewModel.formatSecondsToReadable(user.totalSeconds)} • Ort: ${viewModel.formatSecondsToReadable(user.avgSeconds)}",
                            fontSize = 10.sp,
                            color = iOSTextSecondary
                        )
                        Text(
                            text = "✅ Çözüm: ${user.resolvedCount}/${user.totalCalls} (%${user.resolutionRate})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (user.resolutionRate >= 80) ExcelGreen else iOSOrange
                        )
                    }

                    if (user.topTopic.isNotBlank() && user.topTopic != "-") {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "📌 En Sık Baktığı: ${user.topTopic}",
                            fontSize = 9.5.sp,
                            color = Slate700,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Hızlı Aksiyon Butonları (Aşağı Aç & Filtrele)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Aç / Geri Topla Butonu
                        OutlinedButton(
                            onClick = {
                                expandedUsers = if (isExpanded) {
                                    expandedUsers - user.name
                                } else {
                                    expandedUsers + user.name
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(0.8.dp, if (isExpanded) iOSOrange else iOSBlue),
                            modifier = Modifier.weight(1.2f).height(30.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = if (isExpanded) iOSOrange else iOSBlue,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isExpanded) "Destekleri Geri Topla ⬆" else "Destekleri Listele (${user.totalCalls}) ⬇",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isExpanded) iOSOrange else iOSBlue
                            )
                        }

                        // Bu Temsilciyi Filtrele ve Kayıtlar Sekmesine Geç Butonu
                        Button(
                            onClick = {
                                onSelectUser(user.name)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(30.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Filtrele & Git", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    // AÇILIR LİSTE (ACCORDION) - Temsilciye ait destek kayıtları
                    if (isExpanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Color(0xFF334155), thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "📋 ${user.name} Tarafından Bakılan Çağrılar (${userRecords.size} Kayıt):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF93C5FD)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            userRecords.forEach { rec ->
                                val isRecResolved = !rec.bekleyenIs.equals("Evet", ignoreCase = true)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF0F172A),
                                    border = BorderStroke(0.6.dp, Color(0xFF334155)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = rec.musteriBayiAdi.ifBlank { "İsimsiz Firma" },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.5.sp,
                                                color = iOSTextPrimary,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (isRecResolved) Color(0xFF14532D) else Color(0xFF78350F)
                                            ) {
                                                Text(
                                                    text = if (isRecResolved) "Çözüldü" else "Bekliyor",
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isRecResolved) Color(0xFF86EFAC) else Color(0xFFFDE68A),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(3.dp))

                                        Text(
                                            text = "📌 Konu: ${rec.destekKonusu1.ifBlank { rec.tespit1 }}",
                                            fontSize = 10.sp,
                                            color = iOSTextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        if (rec.islem1.isNotBlank()) {
                                            Text(
                                                text = "✅ İşlem: ${rec.islem1}",
                                                fontSize = 9.5.sp,
                                                color = Color(0xFF64748B),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(3.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${rec.baslangicTarihi.take(16)} (${rec.toplamSure})",
                                                fontSize = 9.sp,
                                                color = Slate700
                                            )

                                            if (rec.sicilNo1.isNotBlank()) {
                                                Surface(
                                                    color = Color(0xFF1E3A8A),
                                                    shape = RoundedCornerShape(4.dp),
                                                    modifier = Modifier.clickable {
                                                        viewModel.openDeviceReport(rec.sicilNo1)
                                                    }
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    ) {
                                                        Text("Sicil: ${rec.sicilNo1}", fontSize = 9.sp, color = Color(0xFF93C5FD), fontWeight = FontWeight.Bold)
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Icon(Icons.Default.Assessment, contentDescription = null, tint = Color(0xFF93C5FD), modifier = Modifier.size(10.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Listenin Altındaki Geri Topla Butonu
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = {
                                expandedUsers = expandedUsers - user.name
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(28.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.ExpandLess, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("⬆ Destekleri Geri Topla (Listeyi Kapat)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ManagerChartsView(
    records: List<SupportRecord>,
    users: List<UserPerfData>,
    totalCalls: Int,
    viewModel: SupportViewModel
) {
    if (records.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Grafik oluşturulacak veri bulunamadı.", color = iOSTextSecondary, fontSize = 12.sp)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Chart 1: Representative Call Volume Distribution
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = iOSCardSecondary),
            border = BorderStroke(0.8.dp, iOSBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BarChart, contentDescription = null, tint = iOSBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Temsilci Çağrı Dağılımı", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = iOSTextPrimary)
                }
                Spacer(modifier = Modifier.height(8.dp))

                users.take(8).forEach { user ->
                    val ratio = if (totalCalls > 0) (user.totalCalls.toFloat() / totalCalls.toFloat()) else 0f
                    Column(modifier = Modifier.padding(vertical = 3.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(user.name, fontSize = 10.5.sp, color = iOSTextPrimary, fontWeight = FontWeight.Medium)
                            Text("${user.totalCalls} Adet (%${(ratio * 100).toInt()})", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = iOSBlue)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        LinearProgressIndicator(
                            progress = { ratio.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = iOSBlue,
                            trackColor = Color(0xFF1E293B)
                        )
                    }
                }
            }
        }

        // Chart 2: Top Issues / Arıza Dağılımı
        val topTopics = remember(records) {
            records.map { it.destekKonusu1.trim().ifBlank { it.tespit1.trim() }.ifBlank { "Diğer / Belirtilmemiş" } }
                .groupingBy { it }
                .eachCount()
                .toList()
                .sortedByDescending { it.second }
                .take(5)
        }

        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = iOSCardSecondary),
            border = BorderStroke(0.8.dp, iOSBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PieChart, contentDescription = null, tint = iOSOrange, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("En Çok Karşılaşılan Arıza & Tespit Konuları", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = iOSTextPrimary)
                }
                Spacer(modifier = Modifier.height(8.dp))

                val colors = listOf(Color(0xFFEF4444), Color(0xFFF59E0B), Color(0xFF10B981), Color(0xFF3B82F6), Color(0xFF8B5CF6))

                topTopics.forEachIndexed { idx, (topic, count) ->
                    val ratio = if (totalCalls > 0) (count.toFloat() / totalCalls.toFloat()) else 0f
                    val color = colors.getOrElse(idx) { iOSBlue }
                    Column(modifier = Modifier.padding(vertical = 3.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(topic, fontSize = 10.5.sp, color = iOSTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Text("$count Adet (%${(ratio * 100).toInt()})", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        LinearProgressIndicator(
                            progress = { ratio.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = color,
                            trackColor = Color(0xFF1E293B)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ManagerCallsListView(
    records: List<SupportRecord>,
    selectedUserFilter: String,
    viewModel: SupportViewModel,
    onClearUserFilter: () -> Unit
) {
    if (records.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Filtreye uygun çağrı kaydı bulunamadı.", color = iOSTextSecondary, fontSize = 12.sp)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Temsilci filtresi etkinse en üstte geri toplama bandı göster
        if (selectedUserFilter != "TÜMÜ") {
            item {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.dp, Color(0xFF3B82F6)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "👤 Filtrelenen: $selectedUserFilter (${records.size} Kayıt)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = onClearUserFilter,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Geri Topla / Tümünü Göster", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        items(records, key = { it.id }) { record ->
            val isResolved = !record.bekleyenIs.equals("Evet", ignoreCase = true)
            val resolvedRepName = viewModel.resolveRepresentativeName(record.kullaniciAdi.ifBlank { record.kullaniciKodu })

            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = iOSCardSecondary),
                border = BorderStroke(0.6.dp, iOSBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = record.musteriBayiAdi.ifBlank { "İsimsiz Firma" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = iOSTextPrimary
                            )
                            Text(
                                text = "👤 Temsilci: $resolvedRepName • Sicil: ${record.sicilNo1.ifBlank { "-" }}",
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
                    Text(
                        text = "📌 Konu: ${record.destekKonusu1.ifBlank { record.tespit1 }}",
                        fontSize = 10.5.sp,
                        color = iOSTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (record.islem1.isNotBlank()) {
                        Text(
                            text = "✅ İşlem: ${record.islem1}",
                            fontSize = 10.sp,
                            color = iOSTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = record.baslangicTarihi.take(16),
                            fontSize = 9.sp,
                            color = Slate700
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (record.sicilNo1.isNotBlank()) {
                                Surface(
                                    color = Color(0xFF1E3A8A),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.clickable {
                                        viewModel.openDeviceReport(record.sicilNo1)
                                    }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text("Rapor", fontSize = 8.5.sp, color = Color(0xFF93C5FD), fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Icon(Icons.Default.Assessment, contentDescription = null, tint = Color(0xFF93C5FD), modifier = Modifier.size(10.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = "Süre: ${record.toplamSure.ifBlank { "-" }}",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ExcelGreen
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ManagerReportPreviewView(
    records: List<SupportRecord>,
    periodTitle: String,
    viewModel: SupportViewModel,
    includeExcel: Boolean,
    onToggleExcel: (Boolean) -> Unit
) {
    val reportText = remember(records, periodTitle) {
        viewModel.generateServiceManagerReport(records, periodTitle)
    }

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
                        Text("${records.size} adet filtrelenmiş çağrı kaydı", fontSize = 9.5.sp, color = iOSTextSecondary)
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
                    text = "📋 Yönetici Raporu Önizlemesi (WhatsApp / E-Posta):",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = iOSBlue
                )
                Spacer(modifier = Modifier.height(6.dp))
                Divider(color = Color(0xFF1E293B), thickness = 0.6.dp)
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

private fun parseFlexibleDateToMillis(str: String): Long {
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

private fun normalizeToYmd(str: String): String {
    val clean = str.trim()
    if (clean.isBlank()) return ""
    val millis = parseFlexibleDateToMillis(clean)
    if (millis > 0) {
        return SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date(millis))
    }
    // If yyyy-MM-dd
    if (clean.length >= 10 && clean[4] == '-' && clean[7] == '-') {
        return clean.substring(0, 10)
    }
    // If dd.MM.yyyy
    if (clean.length >= 10 && clean[2] == '.' && clean[5] == '.') {
        val d = clean.substring(0, 2)
        val m = clean.substring(3, 5)
        val y = clean.substring(6, 10)
        return "$y-$m-$d"
    }
    // If dd/MM/yyyy
    if (clean.length >= 10 && clean[2] == '/' && clean[5] == '/') {
        val d = clean.substring(0, 2)
        val m = clean.substring(3, 5)
        val y = clean.substring(6, 10)
        return "$y-$m-$d"
    }
    return clean.take(10)
}
