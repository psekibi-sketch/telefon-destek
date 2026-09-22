package com.example.ui

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.SupportRecord
import com.example.model.SupportTemplate
import com.example.ui.theme.*
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(
    viewModel: SupportViewModel
) {
    val context = LocalContext.current
    val formState by viewModel.formState.collectAsState()
    val isTimerRunning by viewModel.isTimerRunning.collectAsState()
    val timerSeconds by viewModel.timerSeconds.collectAsState()
    val allRecords by viewModel.allRecords.collectAsState()
    val isExportDialogOpen by viewModel.isExportDialogOpen.collectAsState()
    val isHistoryModalOpen by viewModel.isHistoryModalOpen.collectAsState()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsState()
    val showAdvancedFields by viewModel.showAdvancedFields.collectAsState()

    val tespitTemplates by viewModel.tespitTemplates.collectAsState()
    val islemTemplates by viewModel.islemTemplates.collectAsState()
    val konuTemplates by viewModel.konuTemplates.collectAsState()
    val autoFillSuggestion by viewModel.autoFillSuggestion.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ExcelGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.HeadsetMic,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Destek Çağrı Kayıt",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Excel (.xlsx) Doğrudan Raporlama",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Excel Export Action Button
                    Button(
                        onClick = { viewModel.openExportDialog() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ExcelGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .testTag("export_excel_topbar_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = "Excel",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Excel'e Aktar",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Record History Button with Badge
                    IconButton(
                        onClick = { viewModel.openHistoryModal() },
                        modifier = Modifier.testTag("history_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (allRecords.isNotEmpty()) {
                                    Badge(containerColor = ExcelGreen) {
                                        Text(text = allRecords.size.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Kayıt Geçmişi"
                            )
                        }
                    }

                    // Settings Button (Templates, Date-based deletion, Advanced fields)
                    IconButton(
                        onClick = { viewModel.openSettings() },
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Ayarlar",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val isWideScreen = maxWidth >= 840.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                // Top Stopwatch Bar (Live Call Timer & Clean Start) - COMPACT
                StopwatchWidget(
                    isRunning = isTimerRunning,
                    seconds = timerSeconds,
                    basTarihi = formState.basTarihi,
                    sure = formState.sure,
                    onStartCallAndClear = { viewModel.startCallAndClear() },
                    onStop = { viewModel.stopTimer() },
                    onReset = { viewModel.resetTimer() }
                )

                Spacer(modifier = Modifier.height(6.dp))

                var selectedTab by remember { mutableIntStateOf(0) }

                // Requirement 3: When hidden fields are enabled from settings, present them as 2nd page/tab together!
                if (showAdvancedFields) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = ExcelGreen,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.HeadsetMic, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("1. Çağrı Girişi (Tek Ekran)", fontSize = 12.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("2. Gelişmiş Alanlar (Gizlenenler)", fontSize = 12.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Content Card (Clean Single-Screen Form or 2nd Advanced Page)
                ElevatedCard(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Edit Mode Indicator Banner
                        if (formState.editingId != null) {
                            Surface(
                                color = AccentOrange.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, AccentOrange.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Default.Edit, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Kayıt Düzenleme Modu (#${formState.editingId})",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AccentOrange
                                        )
                                    }
                                    TextButton(
                                        onClick = { viewModel.cancelEditing() },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                    ) {
                                        Text("Vazgeç", fontSize = 11.sp, color = AccentRed)
                                    }
                                }
                            }
                        }

                        if (showAdvancedFields && selectedTab == 1) {
                            // 2nd Page: All Hidden & Advanced Fields Gathered Together (Requirement 3)
                            AdvancedFieldsSection(
                                formState = formState,
                                viewModel = viewModel
                            )
                        } else {
                            // 1st Page: Pure, Compact Single-Screen Call Form (Requirement 1 & 2)
                            SinglePageCallForm(
                                formState = formState,
                                viewModel = viewModel,
                                tespitTemplates = tespitTemplates,
                                islemTemplates = islemTemplates,
                                konuTemplates = konuTemplates
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Bottom Action Bar: Compact Clear & Save Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { viewModel.clearForm() },
                            modifier = Modifier.testTag("clear_form_button"),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Temizle", fontSize = 11.sp)
                        }

                        if (formState.editingId != null) {
                            OutlinedButton(
                                onClick = { viewModel.cancelEditing() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = AccentRed, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("İptal", fontSize = 11.sp, color = AccentRed)
                            }
                        }
                    }

                    Button(
                        onClick = { viewModel.saveRecord() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (formState.editingId != null) AccentOrange else ExcelGreen,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.testTag("save_record_button")
                    ) {
                        Icon(
                            imageVector = if (formState.editingId != null) Icons.Default.Check else Icons.Default.Save,
                            contentDescription = "Kaydet",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (formState.editingId != null) "GÜNCELLE (#${formState.editingId})" else "KAYDET & BİTİR",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Modal: Date Range Filtered Excel Export
    if (isExportDialogOpen) {
        ExcelExportDateFilterDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.closeExportDialog() },
            onExport = { viewModel.exportAndShareExcel(context) }
        )
    }

    // Modal: Record History Viewer
    if (isHistoryModalOpen) {
        RecordHistoryDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.closeHistoryModal() }
        )
    }

    // Modal: Settings (Templates, Date-Range Deletion, Options)
    if (isSettingsOpen) {
        SettingsDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.closeSettings() }
        )
    }

    // Auto-fill Suggestion Alert Dialog
    autoFillSuggestion?.let { suggestion ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissAutoFillSuggestion() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = ExcelGreen,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (suggestion.matchedField == "SICIL") "Sicil No Eşleşmesi Bulundu" else "Telefon Eşleşmesi Bulundu",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = suggestion.title,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (suggestion.musteriBayi.isNotBlank()) {
                                Row {
                                    Text("Müşteri / Bayi: ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(suggestion.musteriBayi, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            if (suggestion.gorusulen.isNotBlank()) {
                                Row {
                                    Text("Görüşülen: ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(suggestion.gorusulen, fontSize = 12.sp)
                                }
                            }
                            if (suggestion.telefon.isNotBlank()) {
                                Row {
                                    Text("Telefon: ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(suggestion.telefon, fontSize = 12.sp)
                                }
                            }
                            if (suggestion.matchedField == "TELEFON") {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "ℹ Sicil No alanı kural gereği boş bırakılacaktır.",
                                    fontSize = 11.sp,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                    Text(
                        text = "Bu bilgileri forma aktarmak istiyor musunuz?",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.applyAutoFillSuggestion() },
                    colors = ButtonDefaults.buttonColors(containerColor = ExcelGreen)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Otomatik Doldur", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.dismissAutoFillSuggestion() }) {
                    Text("Vazgeç", fontSize = 12.sp)
                }
            }
        )
    }
}

// -------------------------------------------------------------
// STOPWATCH WIDGET (COMPACT & SLIM WITH START & CLEAN FUNCTION)
// -------------------------------------------------------------
@Composable
fun StopwatchWidget(
    isRunning: Boolean,
    seconds: Long,
    basTarihi: String,
    sure: String,
    onStartCallAndClear: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit
) {
    val timerText = remember(seconds) {
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        if (hrs > 0) {
            String.format("%02d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format("%02d:%02d", mins, secs)
        }
    }

    val cardBg by animateColorAsState(
        targetValue = if (isRunning) Slate900 else Slate800,
        label = "stopwatchBg"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("stopwatch_widget"),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Status & Timer
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isRunning) AccentRed else Color.Gray)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = timerText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isRunning) "ÇAĞRI AKTİF" else "KRONOMETRE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isRunning) AccentOrange else Color.LightGray
                        )
                    }

                    if (basTarihi.isNotBlank()) {
                        Text(
                            text = "Başlangıç: $basTarihi" + if (sure.isNotBlank()) " | Süre: $sure" else "",
                            fontSize = 10.sp,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }
            }

            // Right: Compact Controls
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!isRunning) {
                    Button(
                        onClick = onStartCallAndClear,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .heightIn(min = 32.dp)
                            .testTag("timer_start_button")
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Başlat", modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Başlat (Yeni)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onStop,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .heightIn(min = 32.dp)
                            .testTag("timer_stop_button")
                    ) {
                        Icon(imageVector = Icons.Default.Pause, contentDescription = "Durdur", modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Durdur", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                IconButton(
                    onClick = onReset,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sıfırla",
                        tint = Color.LightGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 1. PAGE: PURE SINGLE-SCREEN CALL FORM (SADECE GEREKLİ ALANLAR)
// -------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SinglePageCallForm(
    formState: SupportFormState,
    viewModel: SupportViewModel,
    tespitTemplates: List<SupportTemplate>,
    islemTemplates: List<SupportTemplate>,
    konuTemplates: List<SupportTemplate>
) {
    // Sicil No (Tek satır, kompakt)
    OutlinedTextField(
        value = formState.sicilNo,
        onValueChange = { viewModel.updateSicilNo(it) },
        label = { Text("SİCİL NO", fontSize = 11.sp) },
        placeholder = { Text("TECH-104", fontSize = 11.sp) },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("input_sicil_no"),
        colors = formTextFieldColors()
    )

    Spacer(modifier = Modifier.height(5.dp))

    // Müşteri / Bayi (Otomatik BÜYÜK HARF)
    OutlinedTextField(
        value = formState.musteriBayi,
        onValueChange = { viewModel.updateMusteriBayi(it) },
        label = { Text("MÜŞTERİ / BAYİ (BÜYÜK HARF)", fontSize = 11.sp) },
        placeholder = { Text("FİRMA ÜNVANI", fontSize = 11.sp) },
        leadingIcon = { Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(16.dp)) },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("input_musteri_bayi"),
        colors = formTextFieldColors()
    )

    Spacer(modifier = Modifier.height(5.dp))

    // Görüşülen (Otomatik BÜYÜK HARF) & Telefon
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = formState.gorusulen,
            onValueChange = { viewModel.updateGorusulen(it) },
            label = { Text("GÖRÜŞÜLEN (BÜYÜK HARF)", fontSize = 11.sp) },
            placeholder = { Text("İLGİLİ KİŞİ", fontSize = 11.sp) },
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .testTag("input_gorusulen"),
            colors = formTextFieldColors()
        )

        OutlinedTextField(
            value = formState.telefon,
            onValueChange = { viewModel.updateTelefon(it) },
            label = { Text("TELEFON", fontSize = 11.sp) },
            placeholder = { Text("05XXXXXXXXX", fontSize = 11.sp) },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .testTag("input_telefon"),
            colors = formTextFieldColors()
        )
    }

    Spacer(modifier = Modifier.height(5.dp))

    // Destek Konusu - Dropdown / Select (Requirement 3: Sabit Seçenekler)
    val defaultTopics = remember {
        listOf(
            "Donanım Arızası",
            "Kurulum",
            "Yazılım Hatası",
            "E-Fatura",
            "E-İrsaliye",
            "Lisans & Aktivasyon",
            "Veritabanı / SQL",
            "Raporlama & Dizayn",
            "Yazıcı & Sürücü",
            "Program Kurulum",
            "Kullanıcı Yetkilendirme",
            "Dönem Sonu / Devir",
            "Yedekleme & Geri Yükleme"
        )
    }
    val allTopics = remember(konuTemplates) {
        val fromDb = konuTemplates.map { it.text }
        (fromDb + defaultTopics).distinct()
    }
    var topicDropdownExpanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = topicDropdownExpanded,
        onExpandedChange = { topicDropdownExpanded = !topicDropdownExpanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = formState.destekKonu,
            onValueChange = {},
            readOnly = true,
            label = { Text("DESTEK KONU (Seçiniz)", fontSize = 11.sp) },
            placeholder = { Text("Listeden konu seçiniz...", fontSize = 11.sp) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = topicDropdownExpanded) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
                .testTag("input_destek_konu"),
            colors = formTextFieldColors()
        )

        ExposedDropdownMenu(
            expanded = topicDropdownExpanded,
            onDismissRequest = { topicDropdownExpanded = false },
            modifier = Modifier.heightIn(max = 260.dp)
        ) {
            allTopics.forEach { topic ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = topic,
                            fontSize = 12.5.sp,
                            fontWeight = if (formState.destekKonu == topic) FontWeight.Bold else FontWeight.Normal,
                            color = if (formState.destekKonu == topic) ExcelGreen else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        viewModel.updateDestekKonu(topic)
                        topicDropdownExpanded = false
                    },
                    leadingIcon = if (formState.destekKonu == topic) {
                        { Icon(Icons.Default.Check, contentDescription = null, tint = ExcelGreen, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
        }
    }

    // Hızlı Konu Seçim Etiketleri
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 2.dp)) {
        allTopics.take(6).forEach { topic ->
            Text(
                text = topic,
                fontSize = 9.5.sp,
                color = if (formState.destekKonu == topic) Color.White else MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (formState.destekKonu == topic) ExcelGreen
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    )
                    .clickable { viewModel.updateDestekKonu(topic) }
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(5.dp))

    // TESPİT-1 (Sorun / Arıza - Kompakt Mobil Boyut)
    AutoCompleteTemplateField(
        value = formState.tespit,
        onValueChange = { viewModel.updateTespit(it) },
        label = "TESPİT-1 (Sorun / Arıza)",
        placeholder = "Yazın veya şablon seçin...",
        templates = tespitTemplates,
        testTag = "input_tespit"
    )

    Spacer(modifier = Modifier.height(5.dp))

    // İŞLEM-1 (Yapılan Çözüm Müdahalesi - Kompakt Mobil Boyut)
    AutoCompleteTemplateField(
        value = formState.islem,
        onValueChange = { viewModel.updateIslem(it) },
        label = "İŞLEM-1 (Yapılan Müdahale)",
        placeholder = "Yazın veya şablon seçin...",
        templates = islemTemplates,
        testTag = "input_islem"
    )
}

// -------------------------------------------------------------
// 2. PAGE: ADVANCED & HIDDEN FIELDS GATHERED TOGETHER (TÜM GİZLENENLER)
// -------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdvancedFieldsSection(
    formState: SupportFormState,
    viewModel: SupportViewModel
) {
    SectionHeader(
        title = "Gelişmiş ve Ekstra Alanlar (Tüm Gizlenenler)",
        subtitle = "Bu alanlar ana ekranı sade tutmak amacıyla burada toplanmıştır. Doldurulmasa dahi Excel çıktısına standart değerlerle aktarılır.",
        icon = Icons.Default.Tune
    )

    Spacer(modifier = Modifier.height(10.dp))

    // 1. İletişim Türü & Bekleyen Çağrı
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = "İLETİŞİM TÜRÜ",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            val communicationTypes = listOf("Telefon", "Uzak Bağlantı", "E-Posta", "B2B", "Yerinde Destek")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                communicationTypes.forEach { type ->
                    FilterChip(
                        selected = formState.iletisimTuru == type,
                        onClick = { viewModel.updateIletisimTuru(type) },
                        label = { Text(type, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ExcelGreenLight,
                            selectedLabelColor = ExcelGreenDark
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "BEKLEYEN ÇAĞRI MI?",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = formState.bekleyen == "Hayır",
                        onClick = { viewModel.updateBekleyen("Hayır") },
                        label = { Text("Hayır", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = formState.bekleyen == "Evet",
                        onClick = { viewModel.updateBekleyen("Evet") },
                        label = { Text("Evet", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFFE4E6),
                            selectedLabelColor = AccentRed
                        )
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 2. Harici Sistem Bağlantıları (Redmine, Bağ. ID, B2B)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = "HARİCİ SİSTEM BAĞLANTILARI",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = formState.redmine,
                    onValueChange = { viewModel.updateRedmine(it) },
                    label = { Text("REDMINE No", fontSize = 11.sp) },
                    placeholder = { Text("RM-XXXXX", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_redmine"),
                    colors = formTextFieldColors()
                )
                OutlinedTextField(
                    value = formState.bagId,
                    onValueChange = { viewModel.updateBagId(it) },
                    label = { Text("BAĞ.ID-1", fontSize = 11.sp) },
                    placeholder = { Text("AnyDesk ID", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_bag_id"),
                    colors = formTextFieldColors()
                )
                OutlinedTextField(
                    value = formState.b2bKayit,
                    onValueChange = { viewModel.updateB2bKayit(it) },
                    label = { Text("B2B KAYIT", fontSize = 11.sp) },
                    placeholder = { Text("B2B-1234", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_b2b_kayit"),
                    colors = formTextFieldColors()
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 3. Zaman Damgaları & Süre
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = "ZAMAN DAMGALARI & SÜRE (Otomatik)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = formState.basTarihi,
                    onValueChange = { viewModel.updateBasTarihi(it) },
                    label = { Text("BAŞ.TARİHİ", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = formTextFieldColors()
                )
                OutlinedTextField(
                    value = formState.bitTarihi,
                    onValueChange = { viewModel.updateBitTarihi(it) },
                    label = { Text("BİT.TARİHİ", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = formTextFieldColors()
                )
                OutlinedTextField(
                    value = formState.sure,
                    onValueChange = { viewModel.updateSure(it) },
                    label = { Text("SÜRE", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(0.8f),
                    colors = formTextFieldColors()
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 4. Ar-Ge Sevk & Takip
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = "AR-GE SEVK VE GERİ DÖNÜŞ",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("ARGE GİDİŞ:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = formState.argeGidis == "Hayır",
                        onClick = { viewModel.updateArgeGidis("Hayır") },
                        label = { Text("Hayır", fontSize = 10.sp) }
                    )
                    FilterChip(
                        selected = formState.argeGidis == "Evet",
                        onClick = { viewModel.updateArgeGidis("Evet") },
                        label = { Text("Evet", fontSize = 10.sp) }
                    )
                }
            }
            if (formState.argeGidis == "Evet") {
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = formState.argeGidisNotu,
                    onValueChange = { viewModel.updateArgeGidisNotu(it) },
                    label = { Text("ARGE GİDİŞ NOTU", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = formTextFieldColors()
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("ARGE DÖNÜŞ:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = formState.argeDonus == "Hayır",
                        onClick = { viewModel.updateArgeDonus("Hayır") },
                        label = { Text("Hayır", fontSize = 10.sp) }
                    )
                    FilterChip(
                        selected = formState.argeDonus == "Evet",
                        onClick = { viewModel.updateArgeDonus("Evet") },
                        label = { Text("Evet", fontSize = 10.sp) }
                    )
                }
            }
            if (formState.argeDonus == "Evet") {
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = formState.argeDonusNotu,
                    onValueChange = { viewModel.updateArgeDonusNotu(it) },
                    label = { Text("ARGE DÖNÜŞ NOTU", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = formTextFieldColors()
                )
            }
        }
    }
}

// -------------------------------------------------------------
// AUTO-COMPLETE TEMPLATE FIELD (Cumle Tamamlama Bileşeni)
// -------------------------------------------------------------
@Composable
fun AutoCompleteTemplateField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    templates: List<SupportTemplate>,
    testTag: String
) {
    // Matching suggestions as user types the initial letters
    val matchingSuggestions = remember(value, templates) {
        if (value.length < 2) emptyList() else {
            templates.filter {
                it.text.contains(value, ignoreCase = true) && !it.text.equals(value, ignoreCase = true)
            }.take(3)
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, fontSize = 11.sp) },
            placeholder = { Text(placeholder, fontSize = 11.sp) },
            minLines = 1,
            maxLines = 2,
            trailingIcon = {
                if (value.isNotBlank()) {
                    IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(22.dp)) {
                        Icon(Icons.Default.Clear, contentDescription = "Temizle", modifier = Modifier.size(15.dp))
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag),
            colors = formTextFieldColors()
        )

        // Dropdown Auto-Complete Suggestions (As user types, matching sentences appear)
        AnimatedVisibility(
            visible = matchingSuggestions.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                shape = RoundedCornerShape(6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(4.dp)) {
                    matchingSuggestions.forEach { template ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onValueChange(template.text) }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = ExcelGreen,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = template.text,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Quick Pick Chips from top templates
        if (templates.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(templates.take(5)) { template ->
                    val shortText = if (template.text.length > 22) template.text.take(20) + "..." else template.text
                    Text(
                        text = shortText,
                        fontSize = 9.5.sp,
                        color = ExcelGreenDark,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ExcelGreenLight.copy(alpha = 0.6f))
                            .clickable { onValueChange(template.text) }
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SECTION HEADER HELPER
// -------------------------------------------------------------
@Composable
fun SectionHeader(
    title: String,
    subtitle: String,
    icon: ImageVector
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun formTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = ExcelGreen,
    unfocusedBorderColor = Slate200,
    focusedLabelColor = ExcelGreen
)

// -------------------------------------------------------------
// MODAL 1: ENHANCED DATE-FILTERED EXCEL EXPORT DIALOG
// -------------------------------------------------------------
@Composable
fun ExcelExportDateFilterDialog(
    viewModel: SupportViewModel,
    onDismiss: () -> Unit,
    onExport: () -> Unit
) {
    val startDate by viewModel.exportStartDate.collectAsState()
    val endDate by viewModel.exportEndDate.collectAsState()
    val filteredRecords = remember(startDate, endDate) {
        viewModel.getFilteredRecordsForExport()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
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
                                .clip(RoundedCornerShape(8.dp))
                                .background(ExcelGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Excel'e Aktar (Tarih Seçimi)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "21 Sütunlu Tam Tablo .xlsx Formatı",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Presets
                Text(
                    text = "Hızlı Tarih Filtresi:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Bugün", "Dün", "Son 7 Gün", "Bu Ay", "Tümü").forEach { preset ->
                        OutlinedButton(
                            onClick = { viewModel.setDatePreset(preset) },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(preset, fontSize = 10.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Date Inputs
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = { viewModel.setExportStartDate(it) },
                        label = { Text("Başlangıç Tarihi") },
                        placeholder = { Text("YYYY-MM-DD") },
                        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_start_date"),
                        colors = formTextFieldColors()
                    )

                    OutlinedTextField(
                        value = endDate,
                        onValueChange = { viewModel.setExportEndDate(it) },
                        label = { Text("Bitiş Tarihi") },
                        placeholder = { Text("YYYY-MM-DD") },
                        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_end_date"),
                        colors = formTextFieldColors()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Status banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (filteredRecords.isNotEmpty()) ExcelGreenLight else Color(0xFFFEE2E2)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (filteredRecords.isNotEmpty()) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = null,
                            tint = if (filteredRecords.isNotEmpty()) ExcelGreenDark else AccentRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (filteredRecords.isNotEmpty()) "${filteredRecords.size} adet kayıt aktarılmak üzere seçildi" else "Seçilen tarih aralığında kayıt bulunamadı",
                                fontWeight = FontWeight.Bold,
                                color = if (filteredRecords.isNotEmpty()) ExcelGreenDark else AccentRed,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Excel dosyasında hiçbir sütun silinmez, 21 sütun eksiksiz yazılır.",
                                fontSize = 10.sp,
                                color = Slate700
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable list preview of matching records
                Text(
                    text = "Aktarılacak Kayıtların Önizlemesi (${filteredRecords.size}):",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(4.dp)
                ) {
                    if (filteredRecords.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Seçilen tarih aralığında kayıt bulunmuyor.", color = Color.Gray, fontSize = 12.sp)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(filteredRecords, key = { it.id }) { record ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = record.musteriBayi.ifBlank { "İsimsiz Firma" },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = "${record.basTarihi} | ${record.destekKonu.ifBlank { "Konu belirtilmemiş" }}",
                                                fontSize = 10.sp,
                                                color = Color.Gray
                                            )
                                        }
                                        Text(
                                            text = record.sure.ifBlank { "-" },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = ExcelGreenDark
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Kapat")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onExport,
                        enabled = filteredRecords.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ExcelGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("confirm_export_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Excel (.xlsx) Oluştur ve Paylaş", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// MODAL 2: SETTINGS (SABİT ŞABLONLAR & TARİHE GÖRE SİLME)
// -------------------------------------------------------------
@Composable
fun SettingsDialog(
    viewModel: SupportViewModel,
    onDismiss: () -> Unit
) {
    var selectedSettingsTab by remember { mutableIntStateOf(0) }
    val tespitTemplates by viewModel.tespitTemplates.collectAsState()
    val islemTemplates by viewModel.islemTemplates.collectAsState()
    val konuTemplates by viewModel.konuTemplates.collectAsState()
    val showAdvanced by viewModel.showAdvancedFields.collectAsState()

    val deleteStart by viewModel.deleteStartDate.collectAsState()
    val deleteEnd by viewModel.deleteEndDate.collectAsState()
    val globalKullanici by viewModel.globalKullanici.collectAsState()

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    var newTemplateText by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = ExcelGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Genel Ayarlar ve Şablon Yönetimi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tabs: 1. Sabit Tespitler, 2. Sabit İşlemler, 3. Sabit Konular, 4. Genel & Silme Ayarları
                TabRow(
                    selectedTabIndex = selectedSettingsTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = ExcelGreen
                ) {
                    Tab(
                        selected = selectedSettingsTab == 0,
                        onClick = { selectedSettingsTab = 0; newTemplateText = "" },
                        text = { Text("Tespitler (${tespitTemplates.size})", fontSize = 10.5.sp) }
                    )
                    Tab(
                        selected = selectedSettingsTab == 1,
                        onClick = { selectedSettingsTab = 1; newTemplateText = "" },
                        text = { Text("İşlemler (${islemTemplates.size})", fontSize = 10.5.sp) }
                    )
                    Tab(
                        selected = selectedSettingsTab == 2,
                        onClick = { selectedSettingsTab = 2; newTemplateText = "" },
                        text = { Text("Konular (${konuTemplates.size})", fontSize = 10.5.sp) }
                    )
                    Tab(
                        selected = selectedSettingsTab == 3,
                        onClick = { selectedSettingsTab = 3 },
                        text = { Text("Kullanıcı & Silme", fontSize = 10.5.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (selectedSettingsTab) {
                    0 -> {
                        // Sabit Tespit Şablonları Listesi & Ekleme
                        Text(
                            text = "Programda TESPİT-1 yazarken baş harflerle otomatik eşleşecek sabit cümleler:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Add new input
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newTemplateText,
                                onValueChange = { newTemplateText = it },
                                placeholder = { Text("Yeni tespit cümlesi yazın...") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = formTextFieldColors()
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    viewModel.addTemplate("TESPIT", newTemplateText)
                                    newTemplateText = ""
                                },
                                enabled = newTemplateText.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = ExcelGreen)
                            ) {
                                Text("Ekle")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(tespitTemplates, key = { it.id }) { template ->
                                TemplateRowItem(
                                    template = template,
                                    onDelete = { viewModel.deleteTemplate(template) }
                                )
                            }
                        }
                    }

                    1 -> {
                        // Sabit İşlem Şablonları Listesi & Ekleme
                        Text(
                            text = "Programda İŞLEM-1 yazarken baş harflerle otomatik eşleşecek sabit cümleler:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newTemplateText,
                                onValueChange = { newTemplateText = it },
                                placeholder = { Text("Yeni işlem / çözüm cümlesi yazın...") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = formTextFieldColors()
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    viewModel.addTemplate("ISLEM", newTemplateText)
                                    newTemplateText = ""
                                },
                                enabled = newTemplateText.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = ExcelGreen)
                            ) {
                                Text("Ekle")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(islemTemplates, key = { it.id }) { template ->
                                TemplateRowItem(
                                    template = template,
                                    onDelete = { viewModel.deleteTemplate(template) }
                                )
                            }
                        }
                    }

                    2 -> {
                        // Sabit Destek Konuları Listesi & Ekleme (Requirement 3)
                        Text(
                            text = "Açılır menüde (Dropdown) çıkacak sabit destek konuları:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newTemplateText,
                                onValueChange = { newTemplateText = it },
                                placeholder = { Text("Yeni destek konusu ekle...") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = formTextFieldColors()
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    viewModel.addTemplate("KONU", newTemplateText)
                                    newTemplateText = ""
                                },
                                enabled = newTemplateText.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = ExcelGreen)
                            ) {
                                Text("Ekle")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(konuTemplates, key = { it.id }) { template ->
                                TemplateRowItem(
                                    template = template,
                                    onDelete = { viewModel.deleteTemplate(template) }
                                )
                            }
                        }
                    }

                    3 -> {
                        // Genel Kullanıcı Ayarı, Geçmiş Verileri Tarihe Göre Silme & Ekran Görünüm Ayarı
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Section: Sabit Kullanıcı (Temsilci) Ayarı
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = ExcelGreenLight.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = ExcelGreen, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Sabit Kullanıcı (Temsilci) Ayarı",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = ExcelGreenDark
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Buraya girilen kullanıcı adı tüm yeni çağrı kayıtlarında ve Excel aktarımlarında otomatik olarak kullanılır.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedTextField(
                                        value = globalKullanici,
                                        onValueChange = { viewModel.updateGlobalKullanici(it) },
                                        label = { Text("Kullanıcı Ad Soyad") },
                                        placeholder = { Text("Örn: Ahmet Yılmaz") },
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("input_global_kullanici"),
                                        colors = formTextFieldColors()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Section: Tarihe Göre Kayıt Silme
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F2)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = AccentRed, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Tarih Aralığı Vererek Kayıtları Sil",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = AccentRed
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Eski veya arşivlenmiş çağrıları belirtilen tarihler arasında kalıcı olarak temizler.",
                                        fontSize = 11.sp,
                                        color = Slate700
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Presets
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf("Bugün", "1 Ay Önce ve Öncesi", "3 Ay Önce ve Öncesi", "Tümü").forEach { preset ->
                                            OutlinedButton(
                                                onClick = { viewModel.setDeletePreset(preset) },
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(preset, fontSize = 9.sp)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = deleteStart,
                                            onValueChange = { viewModel.setDeleteStartDate(it) },
                                            label = { Text("Silme Başlangıç") },
                                            placeholder = { Text("YYYY-MM-DD") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            colors = formTextFieldColors()
                                        )
                                        OutlinedTextField(
                                            value = deleteEnd,
                                            onValueChange = { viewModel.setDeleteEndDate(it) },
                                            label = { Text("Silme Bitiş") },
                                            placeholder = { Text("YYYY-MM-DD") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            colors = formTextFieldColors()
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = { showDeleteConfirmDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Seçilen Tarih Aralığındaki Kayıtları Sil", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Section: Ekran Görünüm Ayarları (Gizlenen Sütunlar)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Gizlenen Alanları 2. Sayfa Olarak Aç",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = "İletişim türü, Bekleyen çağrı, Redmine No, Bağlantı ID, B2B Kayıt ve Ar-Ge alanlarını ana ekrana koymadan 2. sekmede toplu olarak açar. (Excel çıktısında her zaman tüm 21 sütun yer alır).",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Switch(
                                            checked = showAdvanced,
                                            onCheckedChange = { viewModel.toggleShowAdvancedFields() }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Tamam")
                    }
                }
            }
        }
    }

    // Confirmation Alert for Date Range Deletion
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Kayıtları Silmek İstiyor musunuz?") },
            text = { Text("$deleteStart ile $deleteEnd arasındaki tüm çağrı kayıtları kalıcı olarak silinecektir. Bu işlem geri alınamaz!") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteRecordsByDateRange()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Text("Evet, Sil")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Vazgeç")
                }
            }
        )
    }
}

@Composable
fun TemplateRowItem(
    template: SupportTemplate,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = template.text,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Sil",
                    tint = AccentRed.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// MODAL 3: RECORD HISTORY VIEWER
// -------------------------------------------------------------
@Composable
fun RecordHistoryDialog(
    viewModel: SupportViewModel,
    onDismiss: () -> Unit
) {
    val records by viewModel.allRecords.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val filteredList = remember(records, searchQuery) {
        if (searchQuery.isBlank()) records else {
            records.filter {
                it.musteriBayi.contains(searchQuery, ignoreCase = true) ||
                it.arayan.contains(searchQuery, ignoreCase = true) ||
                it.telefon.contains(searchQuery, ignoreCase = true) ||
                it.destekKonu.contains(searchQuery, ignoreCase = true) ||
                it.redmine.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = ExcelGreen
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Kayıtlı Destek Çağrıları (${records.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Müşteri, telefon, konu ile ara...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = formTextFieldColors()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Record List
                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (records.isEmpty()) "Henüz kayıt bulunmuyor." else "Arama kriterine uygun kayıt bulunamadı.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredList, key = { it.id }) { record ->
                            RecordItemCard(
                                record = record,
                                onEdit = {
                                    viewModel.loadRecordForEditing(record)
                                    onDismiss()
                                },
                                onDelete = { viewModel.deleteRecord(record) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        onDismiss()
                        viewModel.openExportDialog()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ExcelGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tüm Kayıtları Excel'e Aktar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun RecordItemCard(
    record: SupportRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = record.musteriBayi.ifBlank { "İsimsiz Firma" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Düzenlemek için dokunun ✎",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Düzenle",
                            tint = AccentOrange,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Sil",
                            tint = AccentRed.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (record.destekKonu.isNotBlank()) {
                Text(
                    text = "Konu: ${record.destekKonu}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Arayan: ${record.arayan.ifBlank { "-" }} (${record.telefon.ifBlank { "-" }})",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Süre: ${record.sure.ifBlank { "-" }}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = "Başlangıç: ${record.basTarihi}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}
