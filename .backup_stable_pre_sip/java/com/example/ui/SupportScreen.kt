package com.example.ui

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import com.example.R
import com.example.model.SupportRecord
import com.example.model.SupportTemplate
import com.example.ui.theme.*
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class RepresentativePreset(
    val code: String,
    val name: String
)

val DEFAULT_REPRESENTATIVES = listOf(
    RepresentativePreset("101", "Ahmet Yılmaz"),
    RepresentativePreset("102", "Mehmet Demir"),
    RepresentativePreset("103", "Ayşe Kaya"),
    RepresentativePreset("104", "Fatma Şahin"),
    RepresentativePreset("105", "Ali Çelik"),
    RepresentativePreset("106", "Canan Öztürk")
)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun Modifier.autoBringIntoViewOnFocus(): Modifier {
    val requester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    return this
        .bringIntoViewRequester(requester)
        .onFocusChanged { state ->
            if (state.isFocused) {
                scope.launch {
                    delay(220)
                    requester.bringIntoView()
                }
            }
        }
}

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
    val settingsInitialTab by viewModel.settingsInitialTab.collectAsState()
    val showAdvancedFields by viewModel.showAdvancedFields.collectAsState()

    val unsyncedCount by viewModel.unsyncedCount.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    val tespitTemplates by viewModel.tespitTemplates.collectAsState()
    val islemTemplates by viewModel.islemTemplates.collectAsState()
    val konuTemplates by viewModel.konuTemplates.collectAsState()
    val autoFillSuggestion by viewModel.autoFillSuggestion.collectAsState()
    val matchPicker by viewModel.matchPickerState.collectAsState()

    val verimorState by viewModel.verimorState.collectAsState()
    val verimorExtension by viewModel.verimorExtension.collectAsState()
    val activeIncomingCall by viewModel.activeIncomingCall.collectAsState()

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
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF0F172A), Color(0xFF1E3A8A), Color(0xFF0284C7))
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    color = Color(0x3338BDF8),
                                    shape = RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_arif_bayram_logo),
                                contentDescription = "Arif Bayram Telefon Destek Logo",
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Arif Bayram",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "Telefon Destek",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Excel & Verimor Destek Otomasyonu",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // SQL Sync Action Button with Pending Unsynced Badge
                    BadgedBox(
                        badge = {
                            if (unsyncedCount > 0) {
                                Badge(
                                    containerColor = AccentOrange,
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = unsyncedCount.toString(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        },
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Button(
                            onClick = { viewModel.syncToSql() },
                            enabled = !isSyncing,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (unsyncedCount > 0) AccentOrange else Color(0xFF3B82F6),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("sync_sql_topbar_button")
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(14.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = "SQL Senkronizasyon",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isSyncing) "Aktarılıyor..." else "SQL'e Aktar",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

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

                    // Verimor SIP Quick Status Pill
                    val sipBg = when (verimorState) {
                        is com.example.sip.VerimorSipState.Registered -> ExcelGreenLight
                        is com.example.sip.VerimorSipState.Connecting -> Color(0xFFFEF3C7)
                        is com.example.sip.VerimorSipState.Ringing, is com.example.sip.VerimorSipState.InCall -> Color(0xFFDCFCE7)
                        is com.example.sip.VerimorSipState.Error -> Color(0xFFFFE4E6)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    val sipFg = when (verimorState) {
                        is com.example.sip.VerimorSipState.Registered, is com.example.sip.VerimorSipState.Ringing, is com.example.sip.VerimorSipState.InCall -> ExcelGreenDark
                        is com.example.sip.VerimorSipState.Connecting -> Color(0xFFB45309)
                        is com.example.sip.VerimorSipState.Error -> AccentRed
                        else -> Slate700
                    }
                    Surface(
                        color = sipBg,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .clickable { viewModel.openSettings(initialTab = 5) }
                            .padding(end = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (verimorState is com.example.sip.VerimorSipState.Connecting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(11.dp),
                                    strokeWidth = 1.5.dp,
                                    color = sipFg
                                )
                            } else {
                                Icon(
                                    imageVector = if (verimorState is com.example.sip.VerimorSipState.Error) Icons.Default.PhoneMissed else Icons.Default.PhoneInTalk,
                                    contentDescription = "SIP",
                                    tint = sipFg,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(3.dp))
                            val sipLabel = when (verimorState) {
                                is com.example.sip.VerimorSipState.Registered -> "SIP: $verimorExtension"
                                is com.example.sip.VerimorSipState.Connecting -> "Bağlanıyor..."
                                is com.example.sip.VerimorSipState.Ringing -> "Çalıyor"
                                is com.example.sip.VerimorSipState.InCall -> "Görüşmede"
                                is com.example.sip.VerimorSipState.Error -> "SIP Hata"
                                else -> "SIP Pasif"
                            }
                            Text(
                                text = sipLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = sipFg
                            )
                        }
                    }

                    // Settings Button (Templates, Date-based deletion, Advanced fields)
                    IconButton(
                        onClick = { viewModel.openSettings(initialTab = 0) },
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
                    .imePadding()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                // VERİMOR INCOMING CALL NOTIFICATION BANNER
                activeIncomingCall?.let { call ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.5.dp, ExcelGreen)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = CircleShape,
                                    color = ExcelGreen,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhoneInTalk,
                                        contentDescription = "Gelen Çağrı",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .padding(6.dp)
                                            .size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "GELEN SANTRAL ÇAĞRISI: ${call.callerNumber}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = ExcelGreenDark
                                        )
                                        if (call.isSimulated) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Surface(
                                                color = Color(0xFFDBEAFE),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "Test",
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF1E40AF),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "⏱ Kronometre otomatik başlatıldı | Form dolduruldu",
                                        fontSize = 10.sp,
                                        color = Slate700
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = {
                                        viewModel.stopTimer()
                                        viewModel.dismissIncomingCallBanner()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.CallEnd, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Bitir", fontSize = 10.5.sp)
                                }

                                IconButton(
                                    onClick = { viewModel.dismissIncomingCallBanner() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Kapat", modifier = Modifier.size(16.dp), tint = Slate700)
                                }
                            }
                        }
                    }
                }

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

                // Requirement 2: "Gizlenen alanları 2. sayfada göster" varsayılan olarak AÇIK gelir ve hatırlanır
                if (showAdvancedFields) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = ExcelGreen,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.HeadsetMic, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text("1. Çağrı Girişi", fontSize = 11.5.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text("2. Gelişmiş Alanlar", fontSize = 11.5.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        FilledTonalIconButton(
                            onClick = {
                                selectedTab = 0
                                viewModel.toggleShowAdvancedFields()
                            },
                            modifier = Modifier.size(38.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = ExcelGreen.copy(alpha = 0.12f),
                                contentColor = ExcelGreenDark
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "2. Sayfayı Gizle",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                } else {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleShowAdvancedFields() },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Tune,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = ExcelGreenDark
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "2. Sayfayı Göster (Detay ve Gelişmiş Alanlar)",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Aç",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ExcelGreenDark
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    Icons.Default.VisibilityOff,
                                    contentDescription = "Aç",
                                    modifier = Modifier.size(15.dp),
                                    tint = ExcelGreenDark
                                )
                            }
                        }
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

                        // En altta kalan veri giriş alanlarının rahatça ekranın üst tarafına kayabilmesi için esnek alt boşluk (300.dp)
                        Spacer(modifier = Modifier.height(300.dp))
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
            initialTab = settingsInitialTab,
            onDismiss = { viewModel.closeSettings() }
        )
    }

    // Intelligent Multi-Candidate Match Picker Dialog
    matchPicker?.let { pickerState ->
        CustomerMatchPickerDialog(
            matchState = pickerState,
            viewModel = viewModel
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
    val focusManager = LocalFocusManager.current
    val focusKullanici = remember { FocusRequester() }
    val focusKullaniciKodu = remember { FocusRequester() }
    val focusSicil = remember { FocusRequester() }
    val focusMusteri = remember { FocusRequester() }
    val focusGorusulen = remember { FocusRequester() }
    val focusTelefon = remember { FocusRequester() }
    val focusMail = remember { FocusRequester() }
    val focusTespit = remember { FocusRequester() }
    val focusIslem = remember { FocusRequester() }

    // 1. Kullanıcı Seçimi / Kodu Alanı (Temsilci Adı ve Sistem ID/Kodu)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Badge,
                        contentDescription = null,
                        tint = ExcelGreen,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "KULLANICI (TEMSİLCİ) & ID KODU",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                var showUserDropdown by remember { mutableStateOf(false) }
                Box {
                    TextButton(
                        onClick = { showUserDropdown = true },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text("Listeden Seç ▾", fontSize = 10.5.sp, color = ExcelGreenDark, fontWeight = FontWeight.Bold)
                    }

                    DropdownMenu(
                        expanded = showUserDropdown,
                        onDismissRequest = { showUserDropdown = false }
                    ) {
                        DEFAULT_REPRESENTATIVES.forEach { preset ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = ExcelGreenLight,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = preset.code,
                                                fontWeight = FontWeight.Bold,
                                                color = ExcelGreenDark,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(preset.name, fontSize = 12.sp)
                                    }
                                },
                                onClick = {
                                    viewModel.setFormUser(preset.name, preset.code)
                                    showUserDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = formState.kullanici,
                    onValueChange = { viewModel.updateKullanici(it) },
                    label = { Text("Kullanıcı Adı", fontSize = 10.5.sp) },
                    placeholder = { Text("Örn: Ahmet Yılmaz", fontSize = 10.5.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(15.dp))
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusKullaniciKodu.requestFocus() }),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1.3f)
                        .focusRequester(focusKullanici)
                        .autoBringIntoViewOnFocus()
                        .testTag("input_form_kullanici"),
                    colors = formTextFieldColors()
                )

                OutlinedTextField(
                    value = formState.kullaniciKodu,
                    onValueChange = { viewModel.updateKullaniciKodu(it) },
                    label = { Text("ID / Kodu", fontSize = 10.5.sp) },
                    placeholder = { Text("101", fontSize = 10.5.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Pin, contentDescription = null, modifier = Modifier.size(15.dp))
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusSicil.requestFocus() }),
                    singleLine = true,
                    modifier = Modifier
                        .weight(0.9f)
                        .focusRequester(focusKullaniciKodu)
                        .autoBringIntoViewOnFocus()
                        .testTag("input_form_kullanici_kodu"),
                    colors = formTextFieldColors()
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // 1. Sicil No (Tek satır, tam genişlik, akıllı kontrol & enter ile sonraki alana geçiş)
    OutlinedTextField(
        value = formState.sicilNo,
        onValueChange = { viewModel.updateSicilNo(it) },
        label = { Text("SİCİL NO (LOKAL ARAMA & KONTROL)", fontSize = 11.sp) },
        placeholder = { Text("Örn: TECH-101", fontSize = 11.sp) },
        leadingIcon = { Icon(Icons.Default.Tag, contentDescription = null, modifier = Modifier.size(16.dp)) },
        trailingIcon = {
            if (formState.sicilNo.isNotBlank()) {
                IconButton(
                    onClick = { viewModel.triggerSmartLookup("SICIL", formState.sicilNo, isExplicit = true) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Sicil No Ara",
                        tint = ExcelGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
            onNext = {
                if (formState.sicilNo.isNotBlank()) {
                    viewModel.triggerSmartLookup("SICIL", formState.sicilNo, isExplicit = true)
                }
                focusMusteri.requestFocus()
            }
        ),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusSicil)
            .autoBringIntoViewOnFocus()
            .testTag("input_sicil_no"),
        colors = formTextFieldColors()
    )

    Spacer(modifier = Modifier.height(6.dp))

    // 2. Müşteri / Bayi (Tek satır, tam genişlik, otomatik BÜYÜK HARF)
    OutlinedTextField(
        value = formState.musteriBayi,
        onValueChange = { viewModel.updateMusteriBayi(it) },
        label = { Text("MÜŞTERİ / BAYİ (BÜYÜK HARF)", fontSize = 11.sp) },
        placeholder = { Text("FİRMA ÜNVANI", fontSize = 11.sp) },
        leadingIcon = { Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(16.dp)) },
        trailingIcon = {
            if (formState.musteriBayi.isNotBlank()) {
                IconButton(
                    onClick = { viewModel.triggerSmartLookup("MUSTERI", formState.musteriBayi, isExplicit = true) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Firma Ara",
                        tint = ExcelGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
            onNext = {
                if (formState.musteriBayi.isNotBlank()) {
                    viewModel.triggerSmartLookup("MUSTERI", formState.musteriBayi, isExplicit = true)
                }
                focusGorusulen.requestFocus()
            }
        ),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusMusteri)
            .autoBringIntoViewOnFocus()
            .testTag("input_musteri_bayi"),
        colors = formTextFieldColors()
    )

    Spacer(modifier = Modifier.height(6.dp))

    // 3. Görüşülen Kişi (Tek satır, tam genişlik - İKİ SÜTUN YAN YANA KALDIRILDI)
    OutlinedTextField(
        value = formState.gorusulen,
        onValueChange = { viewModel.updateGorusulen(it) },
        label = { Text("GÖRÜŞÜLEN KİŞİ (BÜYÜK HARF)", fontSize = 11.sp) },
        placeholder = { Text("İLGİLİ KİŞİ", fontSize = 11.sp) },
        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp)) },
        trailingIcon = {
            if (formState.gorusulen.isNotBlank()) {
                IconButton(
                    onClick = { viewModel.triggerSmartLookup("GORUSULEN", formState.gorusulen, isExplicit = true) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Kişi Ara",
                        tint = ExcelGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
            onNext = {
                if (formState.gorusulen.isNotBlank()) {
                    viewModel.triggerSmartLookup("GORUSULEN", formState.gorusulen, isExplicit = true)
                }
                focusTelefon.requestFocus()
            }
        ),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusGorusulen)
            .autoBringIntoViewOnFocus()
            .testTag("input_gorusulen"),
        colors = formTextFieldColors()
    )

    Spacer(modifier = Modifier.height(6.dp))

    // 4. Telefon (Tek satır, tam genişlik - İKİ SÜTUN YAN YANA KALDIRILDI)
    OutlinedTextField(
        value = formState.telefon,
        onValueChange = { viewModel.updateTelefon(it) },
        label = { Text("TELEFON", fontSize = 11.sp) },
        placeholder = { Text("05XXXXXXXXX", fontSize = 11.sp) },
        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp)) },
        trailingIcon = {
            if (formState.telefon.isNotBlank()) {
                IconButton(
                    onClick = { viewModel.triggerSmartLookup("TELEFON", formState.telefon, isExplicit = true) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Telefon Ara",
                        tint = ExcelGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
            onNext = {
                if (formState.telefon.isNotBlank()) {
                    viewModel.triggerSmartLookup("TELEFON", formState.telefon, isExplicit = true)
                }
                focusMail.requestFocus()
            }
        ),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusTelefon)
            .autoBringIntoViewOnFocus()
            .testTag("input_telefon"),
        colors = formTextFieldColors()
    )

    Spacer(modifier = Modifier.height(6.dp))

    // 5. E-Posta (Tek satır, tam genişlik - İKİ SÜTUN YAN YANA KALDIRILDI, Telefon 2 Gelişmiş Alanlara Taşındı)
    OutlinedTextField(
        value = formState.mail,
        onValueChange = { viewModel.updateMail(it) },
        label = { Text("E-POSTA", fontSize = 11.sp) },
        placeholder = { Text("ornek@firma.com", fontSize = 11.sp) },
        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp)) },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next
        ),
        keyboardActions = KeyboardActions(
            onNext = { focusTespit.requestFocus() }
        ),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusMail)
            .autoBringIntoViewOnFocus()
            .testTag("input_mail"),
        colors = formTextFieldColors()
    )

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
        testTag = "input_tespit",
        modifier = Modifier.focusRequester(focusTespit),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focusIslem.requestFocus() })
    )

    Spacer(modifier = Modifier.height(5.dp))

    // İŞLEM-1 (Yapılan Çözüm Müdahalesi - Kompakt Mobil Boyut)
    AutoCompleteTemplateField(
        value = formState.islem,
        onValueChange = { viewModel.updateIslem(it) },
        label = "İŞLEM-1 (Yapılan Müdahale)",
        placeholder = "Yazın veya şablon seçin...",
        templates = islemTemplates,
        testTag = "input_islem",
        modifier = Modifier.focusRequester(focusIslem),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
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

    // 1. İletişim Türü & Bekleyen Çağrı & İkincil Telefon
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = "İLETİŞİM TÜRÜ & BİLGİLERİ",
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

            // Telefon 2 (Ana ekrandan 2. Gelişmiş Alanlar sekmesine taşınan alan)
            Text(
                text = "İKİNCİL İLETİŞİM (TELEFON 2):",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = formState.telefon2,
                onValueChange = { viewModel.updateTelefon2(it) },
                label = { Text("TELEFON 2 (ALTERNATİF / SABİT HAT)", fontSize = 11.sp) },
                placeholder = { Text("05XXXXXXXXX veya Sabit Hat", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.PhoneCallback, contentDescription = null, modifier = Modifier.size(16.dp)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .autoBringIntoViewOnFocus()
                    .testTag("input_telefon2"),
                colors = formTextFieldColors()
            )

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
                        .autoBringIntoViewOnFocus()
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
                        .autoBringIntoViewOnFocus()
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
                        .autoBringIntoViewOnFocus()
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
                    modifier = Modifier.weight(1f).autoBringIntoViewOnFocus(),
                    colors = formTextFieldColors()
                )
                OutlinedTextField(
                    value = formState.bitTarihi,
                    onValueChange = { viewModel.updateBitTarihi(it) },
                    label = { Text("BİT.TARİHİ", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f).autoBringIntoViewOnFocus(),
                    colors = formTextFieldColors()
                )
                OutlinedTextField(
                    value = formState.sure,
                    onValueChange = { viewModel.updateSure(it) },
                    label = { Text("SÜRE", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(0.8f).autoBringIntoViewOnFocus(),
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
                    modifier = Modifier.fillMaxWidth().autoBringIntoViewOnFocus(),
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
                    modifier = Modifier.fillMaxWidth().autoBringIntoViewOnFocus(),
                    colors = formTextFieldColors()
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 4. Cihaz Donanım ve Versiyon Bilgileri (Yazar Kasa / POS / Arka Ofis)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Devices, contentDescription = null, tint = ExcelGreen, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "CİHAZ DONANIM VE VERSİYON BİLGİLERİ",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Sicil No girildiğinde cihaz geçmişinden otomatik dolar.",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = formState.ynokcVersiyon,
                    onValueChange = { viewModel.updateYnokcVersiyon(it) },
                    label = { Text("YNOKC VERSİYON", fontSize = 10.5.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f).autoBringIntoViewOnFocus(),
                    colors = formTextFieldColors()
                )
                OutlinedTextField(
                    value = formState.dynCashVersiyon,
                    onValueChange = { viewModel.updateDynCashVersiyon(it) },
                    label = { Text("DYNCASH VERSİYON", fontSize = 10.5.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f).autoBringIntoViewOnFocus(),
                    colors = formTextFieldColors()
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = formState.yn500Versiyon,
                    onValueChange = { viewModel.updateYn500Versiyon(it) },
                    label = { Text("YN500 VERSİYON", fontSize = 10.5.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f).autoBringIntoViewOnFocus(),
                    colors = formTextFieldColors()
                )
                OutlinedTextField(
                    value = formState.arkaOfisVersiyon,
                    onValueChange = { viewModel.updateArkaOfisVersiyon(it) },
                    label = { Text("ARKA OFİS VERSİYON", fontSize = 10.5.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f).autoBringIntoViewOnFocus(),
                    colors = formTextFieldColors()
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = formState.servisKodu,
                onValueChange = { viewModel.updateServisKodu(it) },
                label = { Text("SERVİS KODU", fontSize = 10.5.sp) },
                placeholder = { Text("Örn: SRV-01") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().autoBringIntoViewOnFocus(),
                colors = formTextFieldColors()
            )
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
    testTag: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
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
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            modifier = modifier
                .fillMaxWidth()
                .autoBringIntoViewOnFocus()
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
                .imePadding()
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
                            .autoBringIntoViewOnFocus()
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
                            .autoBringIntoViewOnFocus()
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
    initialTab: Int = 0,
    onDismiss: () -> Unit
) {
    var selectedSettingsTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    val tespitTemplates by viewModel.tespitTemplates.collectAsState()
    val islemTemplates by viewModel.islemTemplates.collectAsState()
    val konuTemplates by viewModel.konuTemplates.collectAsState()
    val showAdvanced by viewModel.showAdvancedFields.collectAsState()

    val deleteStart by viewModel.deleteStartDate.collectAsState()
    val deleteEnd by viewModel.deleteEndDate.collectAsState()
    val globalKullanici by viewModel.globalKullanici.collectAsState()
    val globalKullaniciKodu by viewModel.globalKullaniciKodu.collectAsState()
    val dynamicBaseUrl by viewModel.dynamicBaseUrl.collectAsState()
    val unsyncedCount by viewModel.unsyncedCount.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    val sqlDbType by viewModel.sqlDbType.collectAsState()
    val sqlHost by viewModel.sqlHost.collectAsState()
    val sqlPort by viewModel.sqlPort.collectAsState()
    val sqlDbName by viewModel.sqlDbName.collectAsState()
    val sqlUser by viewModel.sqlUser.collectAsState()
    val sqlPassword by viewModel.sqlPassword.collectAsState()
    val isTestingConnection by viewModel.isTestingConnection.collectAsState()
    val connectionTestResult by viewModel.connectionTestResult.collectAsState()

    val verimorDomain by viewModel.verimorDomain.collectAsState()
    val verimorExtension by viewModel.verimorExtension.collectAsState()
    val verimorPassword by viewModel.verimorPassword.collectAsState()
    val verimorPort by viewModel.verimorPort.collectAsState()
    val verimorAutoTimer by viewModel.verimorAutoTimer.collectAsState()
    val verimorAutoSearch by viewModel.verimorAutoSearch.collectAsState()
    val verimorAutoConnect by viewModel.verimorAutoConnect.collectAsState()
    val verimorState by viewModel.verimorState.collectAsState()

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
                .imePadding()
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

                // Tabs: 1. Sabit Tespitler, 2. Sabit İşlemler, 3. Sabit Konular, 4. Kullanıcı & Silme, 5. SQL Senkronizasyon, 6. Verimor SIP
                ScrollableTabRow(
                    selectedTabIndex = selectedSettingsTab,
                    edgePadding = 0.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = ExcelGreen
                ) {
                    Tab(
                        selected = selectedSettingsTab == 0,
                        onClick = { selectedSettingsTab = 0; newTemplateText = "" },
                        text = { Text("Tespitler (${tespitTemplates.size})", fontSize = 10.sp) }
                    )
                    Tab(
                        selected = selectedSettingsTab == 1,
                        onClick = { selectedSettingsTab = 1; newTemplateText = "" },
                        text = { Text("İşlemler (${islemTemplates.size})", fontSize = 10.sp) }
                    )
                    Tab(
                        selected = selectedSettingsTab == 2,
                        onClick = { selectedSettingsTab = 2; newTemplateText = "" },
                        text = { Text("Konular (${konuTemplates.size})", fontSize = 10.sp) }
                    )
                    Tab(
                        selected = selectedSettingsTab == 3,
                        onClick = { selectedSettingsTab = 3 },
                        text = { Text("Kullanıcı", fontSize = 10.sp) }
                    )
                    Tab(
                        selected = selectedSettingsTab == 4,
                        onClick = { selectedSettingsTab = 4 },
                        text = { Text("SQL Sync", fontSize = 10.sp) }
                    )
                    Tab(
                        selected = selectedSettingsTab == 5,
                        onClick = { selectedSettingsTab = 5 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PhoneInTalk, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Verimor SIP", fontSize = 10.sp)
                            }
                        }
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
                                modifier = Modifier.weight(1f).autoBringIntoViewOnFocus(),
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
                                modifier = Modifier.weight(1f).autoBringIntoViewOnFocus(),
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
                                modifier = Modifier.weight(1f).autoBringIntoViewOnFocus(),
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
                            // Section: Sabit Kullanıcı (Temsilci) ve ID Kodu Ayarı
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = ExcelGreenLight.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Badge, contentDescription = null, tint = ExcelGreen, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Sabit Kullanıcı (Temsilci) & ID Kodu Ayarı",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = ExcelGreenDark
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Kullanıcı Adı ve Kullanıcı Kodu (ID) yeni çağrılarda otomatik doldurulur ve SQL senkronizasyonunda eksiksiz iletilir. Böylece ID eşleşmeme sorunu tamamen çözülür.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = "Kayıtlı Temsilcilerden Hızlı Seçim:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Slate700
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))

                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        contentPadding = PaddingValues(vertical = 2.dp)
                                    ) {
                                        items(DEFAULT_REPRESENTATIVES) { preset ->
                                            val isSelected = globalKullaniciKodu == preset.code || (globalKullanici == preset.name && globalKullaniciKodu.isBlank())
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = {
                                                    viewModel.setGlobalUser(preset.name, preset.code)
                                                },
                                                label = { Text("${preset.code} - ${preset.name}", fontSize = 10.5.sp) },
                                                leadingIcon = if (isSelected) {
                                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp)) }
                                                } else null,
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = ExcelGreenLight,
                                                    selectedLabelColor = ExcelGreenDark
                                                )
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = globalKullanici,
                                            onValueChange = { viewModel.updateGlobalKullanici(it) },
                                            label = { Text("Kullanıcı Adı Soyadı") },
                                            placeholder = { Text("Örn: Ahmet Yılmaz") },
                                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                            singleLine = true,
                                            modifier = Modifier
                                                .weight(1.3f)
                                                .autoBringIntoViewOnFocus()
                                                .testTag("input_global_kullanici"),
                                            colors = formTextFieldColors()
                                        )

                                        OutlinedTextField(
                                            value = globalKullaniciKodu,
                                            onValueChange = { viewModel.updateGlobalKullaniciKodu(it) },
                                            label = { Text("ID / Kodu") },
                                            placeholder = { Text("101") },
                                            leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                            singleLine = true,
                                            modifier = Modifier
                                                .weight(0.9f)
                                                .autoBringIntoViewOnFocus()
                                                .testTag("input_global_kullanici_kodu"),
                                            colors = formTextFieldColors()
                                        )
                                    }

                                    if (globalKullanici.isNotBlank() || globalKullaniciKodu.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Surface(
                                            color = ExcelGreen.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = ExcelGreenDark, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Aktif Varsayılan Temsilci: ${globalKullanici.ifBlank { "(İsimsiz)" }}  [ID: ${globalKullaniciKodu.ifBlank { "Belirtilmedi" }}]",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ExcelGreenDark
                                                )
                                            }
                                        }
                                    }
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
                                            modifier = Modifier.weight(1f).autoBringIntoViewOnFocus(),
                                            colors = formTextFieldColors()
                                        )
                                        OutlinedTextField(
                                            value = deleteEnd,
                                            onValueChange = { viewModel.setDeleteEndDate(it) },
                                            label = { Text("Silme Bitiş") },
                                            placeholder = { Text("YYYY-MM-DD") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f).autoBringIntoViewOnFocus(),
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
                                                text = "Gizlenen Alanları 2. Sayfada Göster",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = "Uygulama açılışında varsayılan olarak AÇIK gelir. Yaptığınız seçim cihaz hafızasında hatırlanır. Açıkken ana ekranda 2. sekme olarak sunulur (İletişim türü, Bekleyen çağrı, Redmine No, Bağlantı ID, B2B Kayıt ve Ar-Ge alanları).",
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

                    4 -> {
                        // SQL Senkronizasyon Ayarları ve Durumu (Endpoint, Bekleyen Kayıtlar, Manuel Tetikleme)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(22.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Merkezi SQL Senkronizasyonu",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color(0xFF1E40AF)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Uygulama offline-first mimaridedir. Veriler öncelikle cihazda saklanır. İstendiğinde veya internet olduğunda merkezi MySQL / PostgreSQL / MSSQL REST API servisine toplu olarak aktarılır.",
                                        fontSize = 11.sp,
                                        color = Slate700
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Status info
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.White, RoundedCornerShape(8.dp))
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Senkronize Edilmeyi Bekleyen Kayıt:", fontSize = 11.sp, color = Color.Gray)
                                            Text(
                                                text = "$unsyncedCount adet kayıt bekliyor",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (unsyncedCount > 0) AccentOrange else ExcelGreen
                                            )
                                        }

                                        Button(
                                            onClick = { viewModel.syncToSql() },
                                            enabled = !isSyncing,
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            if (isSyncing) {
                                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                                            } else {
                                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Şimdi Aktar", fontSize = 11.5.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Dinamik API Base URL Bilgi Kartı
                                    Surface(
                                        color = Color.White,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Link, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Dinamik API Base URL (http://<IP>:<Port>):",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.5.sp,
                                                    color = Color(0xFF1E40AF)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = if (dynamicBaseUrl.isNotBlank()) dynamicBaseUrl else "http://[IP]:[PORT] (Lütfen aşağıdaki kutulardan IP ve Port giriniz)",
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (dynamicBaseUrl.isNotBlank()) Color(0xFF1D4ED8) else Color.Gray
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "• Test: POST /api/test-db-connection\n• Senkronizasyon: POST /api/sync-calls",
                                                fontSize = 10.sp,
                                                color = Slate700
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Direct SQL Database Connection Settings (MySQL / PostgreSQL / MSSQL)
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFF93C5FD))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Storage, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "SQL Veritabanı Bağlantı Parametreleri",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.5.sp,
                                                    color = Color(0xFF1E40AF)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))

                                            // Database Type Selector Chips
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                listOf("MySQL", "PostgreSQL", "MSSQL").forEach { type ->
                                                    FilterChip(
                                                        selected = sqlDbType == type,
                                                        onClick = { viewModel.updateSqlDbType(type) },
                                                        label = { Text(type, fontSize = 11.sp) },
                                                        colors = FilterChipDefaults.filterChipColors(
                                                            selectedContainerColor = Color(0xFFDBEAFE),
                                                            selectedLabelColor = Color(0xFF1E40AF)
                                                        )
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            // Host and Port Row
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = sqlHost,
                                                    onValueChange = { viewModel.updateSqlHost(it) },
                                                    label = { Text("Sunucu IP / Host", fontSize = 11.sp) },
                                                    placeholder = { Text("192.168.1.100", fontSize = 11.sp) },
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1.5f).autoBringIntoViewOnFocus(),
                                                    colors = formTextFieldColors()
                                                )
                                                OutlinedTextField(
                                                    value = sqlPort,
                                                    onValueChange = { viewModel.updateSqlPort(it) },
                                                    label = { Text("Port", fontSize = 11.sp) },
                                                    placeholder = { Text("3306", fontSize = 11.sp) },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    singleLine = true,
                                                    modifier = Modifier.weight(0.9f).autoBringIntoViewOnFocus(),
                                                    colors = formTextFieldColors()
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))

                                            // Database Name
                                            OutlinedTextField(
                                                value = sqlDbName,
                                                onValueChange = { viewModel.updateSqlDbName(it) },
                                                label = { Text("Veritabanı Adı (Database Name)", fontSize = 11.sp) },
                                                placeholder = { Text("destek_db", fontSize = 11.sp) },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth().autoBringIntoViewOnFocus(),
                                                colors = formTextFieldColors()
                                            )

                                            Spacer(modifier = Modifier.height(6.dp))

                                            // User & Password Row
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = sqlUser,
                                                    onValueChange = { viewModel.updateSqlUser(it) },
                                                    label = { Text("Kullanıcı (User)", fontSize = 11.sp) },
                                                    placeholder = { Text("root", fontSize = 11.sp) },
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1f).autoBringIntoViewOnFocus(),
                                                    colors = formTextFieldColors()
                                                )
                                                OutlinedTextField(
                                                    value = sqlPassword,
                                                    onValueChange = { viewModel.updateSqlPassword(it) },
                                                    label = { Text("Şifre (Password)", fontSize = 11.sp) },
                                                    placeholder = { Text("••••••", fontSize = 11.sp) },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1f).autoBringIntoViewOnFocus(),
                                                    colors = formTextFieldColors()
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            // Action Buttons: Save Settings & Test Connection
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedButton(
                                                    onClick = { viewModel.saveDbSettings() },
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(15.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Ayarları Kaydet", fontSize = 11.sp)
                                                }

                                                Button(
                                                    onClick = { viewModel.testDbConnection() },
                                                    enabled = !isTestingConnection,
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    if (isTestingConnection) {
                                                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Test Ediliyor...", fontSize = 11.sp)
                                                    } else {
                                                        Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(15.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Bağlantıyı Test Et", fontSize = 11.sp)
                                                    }
                                                }
                                            }

                                            // Test result feedback banner
                                            connectionTestResult?.let { result ->
                                                Spacer(modifier = Modifier.height(6.dp))
                                                val isSuccess = result.startsWith("✓")
                                                Surface(
                                                    color = if (isSuccess) ExcelGreenLight else Color(0xFFFFE4E6),
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = result,
                                                        fontSize = 11.sp,
                                                        color = if (isSuccess) ExcelGreenDark else AccentRed,
                                                        fontWeight = FontWeight.SemiBold,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Demo / Test option
                                    OutlinedButton(
                                        onClick = { viewModel.markAllAsSyncedLocally() },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Bekleyenleri 'Aktarıldı' Olarak İşaretle (Lokal)", fontSize = 11.sp)
                                    }

                                    Spacer(modifier = Modifier.height(280.dp))
                                }
                            }
                        }
                    }

                    5 -> {
                        // Verimor Bulut Santral / SIP Ayarları ve Canlı Test Paneli
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.PhoneInTalk, contentDescription = null, tint = ExcelGreen, modifier = Modifier.size(22.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Verimor Bulut Santral / SIP",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp,
                                                color = ExcelGreenDark
                                            )
                                        }

                                        // Connection Status Badge
                                        val statusText = when (verimorState) {
                                            is com.example.sip.VerimorSipState.Registered -> "✓ Kayıtlı ($verimorExtension)"
                                            is com.example.sip.VerimorSipState.Connecting -> "⏳ Bağlanıyor..."
                                            is com.example.sip.VerimorSipState.Ringing -> "🔔 Çalıyor"
                                            is com.example.sip.VerimorSipState.InCall -> "📞 Görüşmede"
                                            is com.example.sip.VerimorSipState.Error -> "Hata"
                                            else -> "Bağlı Değil"
                                        }
                                        val statusBg = when (verimorState) {
                                            is com.example.sip.VerimorSipState.Registered -> ExcelGreenLight
                                            is com.example.sip.VerimorSipState.Connecting -> Color(0xFFFEF3C7)
                                            is com.example.sip.VerimorSipState.Ringing, is com.example.sip.VerimorSipState.InCall -> Color(0xFFDCFCE7)
                                            is com.example.sip.VerimorSipState.Error -> Color(0xFFFFE4E6)
                                            else -> Color(0xFFF3F4F6)
                                        }
                                        val statusFg = when (verimorState) {
                                            is com.example.sip.VerimorSipState.Registered, is com.example.sip.VerimorSipState.Ringing, is com.example.sip.VerimorSipState.InCall -> ExcelGreenDark
                                            is com.example.sip.VerimorSipState.Connecting -> Color(0xFFB45309)
                                            is com.example.sip.VerimorSipState.Error -> AccentRed
                                            else -> Slate700
                                        }

                                        Surface(
                                            color = statusBg,
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(
                                                text = statusText,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = statusFg,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Verimor Bulut Santral dahili bilgilerinizi giriniz. Cihaz arka planda santrale register olur; çağrı düştüğü anda arayan numara forma yazılır, kronometre başlatılır ve lokal müşteri araması tetiklenir.",
                                        fontSize = 11.sp,
                                        color = Slate700
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Santral Host / Domain
                                    OutlinedTextField(
                                        value = verimorDomain,
                                        onValueChange = { viewModel.updateVerimorDomain(it) },
                                        label = { Text("Santral Domain / Host Adı", fontSize = 11.sp) },
                                        placeholder = { Text("sirketiniz.bulutsantralim.com", fontSize = 11.sp) },
                                        leadingIcon = { Icon(Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().autoBringIntoViewOnFocus(),
                                        colors = formTextFieldColors()
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Dahili (Extension) ve Port
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = verimorExtension,
                                            onValueChange = { viewModel.updateVerimorExtension(it) },
                                            label = { Text("Dahili (Extension)", fontSize = 11.sp) },
                                            placeholder = { Text("101", fontSize = 11.sp) },
                                            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.weight(1.3f).autoBringIntoViewOnFocus(),
                                            colors = formTextFieldColors()
                                        )

                                        OutlinedTextField(
                                            value = verimorPort,
                                            onValueChange = { viewModel.updateVerimorPort(it) },
                                            label = { Text("Port", fontSize = 11.sp) },
                                            placeholder = { Text("5060", fontSize = 11.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.weight(0.9f).autoBringIntoViewOnFocus(),
                                            colors = formTextFieldColors()
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Dahili Şifresi (Password)
                                    var passwordVisible by remember { mutableStateOf(false) }
                                    OutlinedTextField(
                                        value = verimorPassword,
                                        onValueChange = { viewModel.updateVerimorPassword(it) },
                                        label = { Text("Dahili Şifresi (Secret)", fontSize = 11.sp) },
                                        placeholder = { Text("••••••••", fontSize = 11.sp) },
                                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        trailingIcon = {
                                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                                Icon(
                                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                    contentDescription = if (passwordVisible) "Gizle" else "Göster",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        },
                                        visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().autoBringIntoViewOnFocus(),
                                        colors = formTextFieldColors()
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Otomasyon Ayarları Switches
                                    Surface(
                                        color = Color.White,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("Kronometreyi Otomatik Başlat", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                                                    Text("Çağrı düştüğü an 'Başlat (Yeni)' butonu otomatik tetiklenir.", fontSize = 10.sp, color = Color.Gray)
                                                }
                                                Switch(
                                                    checked = verimorAutoTimer,
                                                    onCheckedChange = { viewModel.toggleVerimorAutoTimer() }
                                                )
                                            }

                                            Divider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFFF1F5F9))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("Lokal Müşteri Aramasını Tetikle", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                                                    Text("Arayan numara SQLite veritabanında taranır; tek kayıt ise doldurur, çoklu ise seçim listesi açar.", fontSize = 10.sp, color = Color.Gray)
                                                }
                                                Switch(
                                                    checked = verimorAutoSearch,
                                                    onCheckedChange = { viewModel.toggleVerimorAutoSearch() }
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Bağlan / Bağlantıyı Kes Butonları
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { viewModel.connectVerimorSip() },
                                            enabled = verimorState !is com.example.sip.VerimorSipState.Connecting,
                                            colors = ButtonDefaults.buttonColors(containerColor = ExcelGreen),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            if (verimorState is com.example.sip.VerimorSipState.Connecting) {
                                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Bağlanıyor...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            } else {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Santrale Kayıt Ol (Register)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        OutlinedButton(
                                            onClick = { viewModel.disconnectVerimorSip() },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(0.7f)
                                        ) {
                                            Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(14.dp), tint = AccentRed)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Bağlantıyı Kes", fontSize = 10.5.sp, color = AccentRed)
                                        }
                                    }

                                    // Kalıcı Bağlantı Durumu Bilgi Çubuğu
                                    Surface(
                                        color = if (verimorAutoConnect) ExcelGreenLight.copy(alpha = 0.5f) else Color(0xFFF3F4F6),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (verimorAutoConnect) Icons.Default.CheckCircle else Icons.Default.Info,
                                                contentDescription = null,
                                                tint = if (verimorAutoConnect) ExcelGreenDark else Slate700,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (verimorAutoConnect)
                                                    "Kalıcı Bağlantı Aktif: 'Bağlantıyı Kes' butonuna basılana kadar uygulama her açıldığında otomatik bağlanır."
                                                else
                                                    "Kalıcı Bağlantı Pasif: Bağlantı manuel olarak kapatıldı. Otomatik bağlanmak için 'Santrale Kayıt Ol' butonuna basınız.",
                                                fontSize = 10.sp,
                                                color = if (verimorAutoConnect) ExcelGreenDark else Slate700,
                                                lineHeight = 13.sp
                                            )
                                        }
                                    }

                                    if (verimorState is com.example.sip.VerimorSipState.Error) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Surface(
                                            color = Color(0xFFFFE4E6),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "⚠ ${(verimorState as com.example.sip.VerimorSipState.Error).message}",
                                                fontSize = 11.sp,
                                                color = AccentRed,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // CANLI TEST SİMÜLATÖRÜ (Hemen test edebilmek için)
                                    Surface(
                                        color = Color(0xFFEFF6FF),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.PhoneCallback, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Gelen Çağrı Simülatörü (Canlı Test)",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF1E40AF)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Santral bağlantısı olmadan da akışı test edebilirsiniz. Bir telefon numarası girip butona bastığınızda Verimor çağrısı gibi algılanır, kronometre başlar ve lokal veritabanı taranır.",
                                                fontSize = 10.5.sp,
                                                color = Slate700
                                            )

                                            Spacer(modifier = Modifier.height(8.dp))

                                            var testPhoneNumber by remember { mutableStateOf("05446007080") }

                                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                                OutlinedTextField(
                                                    value = testPhoneNumber,
                                                    onValueChange = { testPhoneNumber = it },
                                                    label = { Text("Arayan Test No", fontSize = 10.5.sp) },
                                                    placeholder = { Text("05446007080", fontSize = 10.5.sp) },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1f).autoBringIntoViewOnFocus(),
                                                    colors = formTextFieldColors()
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Button(
                                                    onClick = {
                                                        viewModel.simulateVerimorIncomingCall(testPhoneNumber)
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Text("Çağrıyı Tetikle", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "İpucu: '05446007080' yazarsanız 2 farklı şube eşleştiği için doğrudan Çoklu Kayıt Dialogu açılır! '05422003040' yazarsanız tek kayıt doğrudan doldurulur.",
                                                fontSize = 9.5.sp,
                                                color = Color(0xFF1D4ED8),
                                                fontStyle = FontStyle.Italic
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(280.dp))
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
                .imePadding()
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
                    modifier = Modifier.fillMaxWidth().autoBringIntoViewOnFocus(),
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
                    // Sync Status Badge
                    Surface(
                        color = if (record.isSynced) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (record.isSynced) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = if (record.isSynced) ExcelGreen else AccentOrange,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (record.isSynced) "SQL Aktarıldı" else "Bekliyor",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (record.isSynced) ExcelGreenDark else Color(0xFFB45309)
                            )
                        }
                    }

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

            val userLabel = when {
                record.kullaniciKodu.isNotBlank() && record.kullaniciAdi.isNotBlank() ->
                    "${record.kullaniciAdi} [ID: ${record.kullaniciKodu}]"
                record.kullaniciKodu.isNotBlank() -> "ID: ${record.kullaniciKodu}"
                else -> record.kullaniciAdi
            }
            if (userLabel.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Temsilci: $userLabel",
                    fontSize = 11.sp,
                    color = ExcelGreenDark,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (record.sicilNo.isNotBlank() || record.ynokcVersiyon.isNotBlank() || record.servisKodu.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Sicil: ${record.sicilNo.ifBlank { "-" }} | YNOKC: ${record.ynokcVersiyon.ifBlank { "-" }} | SRV: ${record.servisKodu.ifBlank { "-" }}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Arama yapıldığında bulunan kayıtları gösteren onay ve seçim diyaloğu.
 * 1 kayıt da bulunsa asla otomatik doldurulmaz; kullanıcı onayı alınır.
 * Birden fazla kayıt varsa hangisinin getirileceği kullanıcıya seçtirilir.
 */
@Composable
fun CustomerMatchPickerDialog(
    matchState: CustomerSearchResultState,
    viewModel: SupportViewModel
) {
    val trLocale = Locale("tr", "TR")
    val isSingle = matchState.candidates.size == 1
    val criterionLabel = when (matchState.searchCriterion) {
        "SICIL" -> "Sicil No"
        "TELEFON" -> "Telefon Numarası"
        "GORUSULEN" -> "Görüşülen Kişi"
        else -> "Arama"
    }

    var filterText by remember { mutableStateOf("") }
    val filteredCandidates = remember(filterText, matchState.candidates) {
        if (filterText.isBlank()) matchState.candidates
        else {
            val q = filterText.uppercase(trLocale)
            matchState.candidates.filter {
                it.musteriBayi.uppercase(trLocale).contains(q) ||
                it.sicilNo.uppercase(trLocale).contains(q) ||
                it.gorusulen.uppercase(trLocale).contains(q) ||
                it.telefon.contains(filterText)
            }
        }
    }

    Dialog(
        onDismissRequest = { viewModel.dismissMatchPicker() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(0.96f)
                .then(
                    if (isSingle) Modifier.wrapContentHeight().heightIn(max = 580.dp)
                    else Modifier.fillMaxHeight(0.85f)
                ),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                // Başlık Alanı
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSingle) Color(0xFFE0F2FE) else ExcelGreenLight,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isSingle) Icons.Default.FactCheck else Icons.Default.ManageSearch,
                                    contentDescription = null,
                                    tint = if (isSingle) Color(0xFF0369A1) else ExcelGreenDark,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isSingle) "Geçmiş Kayıt Bulundu" else "Eşleşen Kayıtlar (${matchState.candidates.size} Kayıt)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$criterionLabel: \"${matchState.queryValue}\"",
                                fontSize = 11.5.sp,
                                color = ExcelGreenDark,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.dismissMatchPicker() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bilgilendirme Kutusu
                Surface(
                    color = if (isSingle) Color(0xFFF0FDF4) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (isSingle) ExcelGreen.copy(alpha = 0.3f) else Color.Transparent)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isSingle) Icons.Default.HelpOutline else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isSingle) ExcelGreenDark else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSingle) {
                                "Geçmiş kayıt bulundu, bu müşteri ve cihaz verileri forma getirilsin mi?"
                            } else {
                                "Birden fazla geçmiş kayıt bulundu. Hangisi getirilsin? Lütfen seçiniz:"
                            },
                            fontSize = 11.5.sp,
                            fontWeight = if (isSingle) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSingle) ExcelGreenDark else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Filtreleme Arama Çubuğu (Birden fazla kayıt varsa)
                if (matchState.candidates.size > 3) {
                    OutlinedTextField(
                        value = filterText,
                        onValueChange = { filterText = it },
                        placeholder = { Text("Eşleşmeler içinde ara...", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = formTextFieldColors()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Eşleşen Kayıtlar Listesi
                LazyColumn(
                    modifier = if (isSingle) Modifier.weight(1f, fill = false) else Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredCandidates) { candidate ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.applyMatchCandidate(candidate, matchState.searchCriterion, isAuto = false)
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSingle) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (isSingle) ExcelGreen.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = candidate.musteriBayi.ifBlank { "İsimsiz Firma" },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Surface(
                                        color = ExcelGreenLight,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "Lokal Kayıt",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ExcelGreenDark,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(5.dp))

                                // Sicil & Görüşülen
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (candidate.sicilNo.isNotBlank()) {
                                        Surface(
                                            color = ExcelGreen.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Tag, contentDescription = null, tint = ExcelGreenDark, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = candidate.sicilNo,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ExcelGreenDark
                                                )
                                            }
                                        }
                                    }

                                    if (candidate.gorusulen.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = candidate.gorusulen,
                                                fontSize = 11.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                if (candidate.telefon.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Phone, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = candidate.telefon,
                                            fontSize = 11.sp,
                                            color = Slate700
                                        )
                                    }
                                }

                                // Cihaz / Versiyon Bilgileri
                                val versions = listOfNotNull(
                                    if (candidate.ynokcVersiyon.isNotBlank()) "YNOKC: ${candidate.ynokcVersiyon}" else null,
                                    if (candidate.dynCashVersiyon.isNotBlank()) "DynCash: ${candidate.dynCashVersiyon}" else null,
                                    if (candidate.servisKodu.isNotBlank()) "SRV: ${candidate.servisKodu}" else null
                                )
                                if (versions.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = versions.joinToString(" • "),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                if (candidate.sonTarih.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Son Çağrı: ${candidate.sonTarih}${if (candidate.sonKonu.isNotBlank()) " (${candidate.sonKonu})" else ""}",
                                        fontSize = 9.5.sp,
                                        color = Color.Gray
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.applyMatchCandidate(candidate, matchState.searchCriterion, isAuto = false)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ExcelGreen),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isSingle) "Evet, Bilgileri Getir" else "Bu Kaydı Aktar",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Alt Kapat / Vazgeç Butonları
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isSingle) {
                            "Verileri getirmek istemiyorsanız 'Vazgeç'e dokununuz."
                        } else {
                            "Aradığınız kayıt yoksa kapatıp yeni kayıt girebilirsiniz."
                        },
                        fontSize = 10.sp,
                        color = Color.Gray,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.dismissMatchPicker() },
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(if (isSingle) "Vazgeç" else "Kapat", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
