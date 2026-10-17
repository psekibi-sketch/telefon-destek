package com.example.ui

import com.example.sip.CallControlKeypadDialog
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
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
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import com.example.R
import com.example.model.SupportRecord
import com.example.model.SupportTemplate
import com.example.ui.theme.*
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.example.model.RepresentativePreset
import android.speech.RecognizerIntent
import android.app.Activity
import android.app.DatePickerDialog
import android.widget.Toast
import java.util.Calendar

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
                    delay(60)
                    requester.bringIntoView(
                        Rect(
                            left = 0f,
                            top = 0f,
                            right = 1000f,
                            bottom = 2500f
                        )
                    )
                }
            }
        }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    val prefixTemplates by viewModel.prefixTemplates.collectAsState()
    val autoFillSuggestion by viewModel.autoFillSuggestion.collectAsState()
    val matchPicker by viewModel.matchPickerState.collectAsState()

    val verimorState by viewModel.verimorState.collectAsState()
    val verimorExtension by viewModel.verimorExtension.collectAsState()
    val activeIncomingCall by viewModel.activeIncomingCall.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val isSpeaker by viewModel.isSpeaker.collectAsState()
    val isAudioStreaming by viewModel.isAudioStreaming.collectAsState()
    val waitingCallsQueue by viewModel.waitingCallsQueue.collectAsState()
    val isCallControlDialogOpen by viewModel.isCallControlDialogOpen.collectAsState()
    val isStatisticsDialogOpen by viewModel.isStatisticsDialogOpen.collectAsState()
    val isDailyReportDialogOpen by viewModel.isDailyReportDialogOpen.collectAsState()
    val isKnowledgeBaseDialogOpen by viewModel.isKnowledgeBaseDialogOpen.collectAsState()
    val isServiceManagerDialogOpen by viewModel.isServiceManagerDialogOpen.collectAsState()

    val masterPowerEnabled by viewModel.masterPowerEnabled.collectAsState()
    val allMissedCalls by viewModel.allMissedCalls.collectAsState()
    val popularTopics by viewModel.popularTopics.collectAsState()
    val popularTespit by viewModel.popularTespit.collectAsState()
    val popularIslem by viewModel.popularIslem.collectAsState()

    val currentBottomTab by viewModel.currentBottomNavTab.collectAsState()

    BackHandler(enabled = currentBottomTab != 0) {
        viewModel.setBottomNavTab(0)
    }

    val isKeyboardOpen = WindowInsets.isImeVisible
    val isCallActive = verimorState is com.example.sip.VerimorSipState.InCall ||
            verimorState is com.example.sip.VerimorSipState.Ringing ||
            activeIncomingCall != null
    val isFocusMode = isKeyboardOpen || isCallActive

    var showWaitingQueueDialog by remember { mutableStateOf(false) }
    var showFullScreenCallModal by remember { mutableStateOf(false) }
    var showPermissionRationaleDialog by remember { mutableStateOf(false) }

    val requiredPermissions = remember {
        buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val audioGranted = result[Manifest.permission.RECORD_AUDIO] == true
        if (!audioGranted) {
            showPermissionRationaleDialog = true
        }
    }

    LaunchedEffect(Unit) {
        val audioCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val notifCheck = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true

        if (!audioCheck || !notifCheck) {
            permissionLauncher.launch(requiredPermissions.toTypedArray())
        }
    }

    // Requirement 1: Gelen santral çağrısında Ana Destek Ekranına yönlendir; çağrı cevaplandığında tam ekran modali hemen kapatarak formda yazmaya izin ver
    LaunchedEffect(verimorState) {
        if (verimorState is com.example.sip.VerimorSipState.Ringing) {
            viewModel.navigateToMainCallScreen()
            showWaitingQueueDialog = false
            showFullScreenCallModal = true
            try {
                val intent = Intent(context, com.example.sip.IncomingCallActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        } else if (verimorState is com.example.sip.VerimorSipState.InCall) {
            // Canlı görüşme başladığı anda tam ekran modali kapat; kullanıcı formda veri girişi yapabilsin
            showFullScreenCallModal = false
        } else {
            showFullScreenCallModal = false
        }
    }

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
                                .size(22.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF0F172A), Color(0xFF1E3A8A), Color(0xFF0284C7))
                                    )
                                )
                                .border(
                                    width = 0.6.dp,
                                    color = Color(0x3338BDF8),
                                    shape = RoundedCornerShape(5.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_telefon_destek_logo),
                                contentDescription = "Logo",
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.width(5.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Telefon Destek",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            if (isCallActive) {
                                Surface(
                                    color = Color(0xFFDCFCE7),
                                    shape = RoundedCornerShape(3.dp),
                                    border = BorderStroke(0.5.dp, ExcelGreen)
                                ) {
                                    Text(
                                        text = "Canlı Çağrı",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ExcelGreenDark,
                                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                actions = {
                    // Arıza & Hata Kodu Rehberi (Bilgi Bankası)
                    Surface(
                        color = Color(0xFF2E103E),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(0.8.dp, iOSPurple),
                        modifier = Modifier
                            .clickable { viewModel.openKnowledgeBaseDialog() }
                            .padding(end = 4.dp)
                            .testTag("topbar_knowledge_base_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = "Arıza & Hata Rehberi",
                                tint = iOSPurple,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(2.5.dp))
                            Text(
                                text = "Rehber",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = iOSPurple
                            )
                        }
                    }

                    // Gün Sonu / Vardiya Raporu Butonu
                    Surface(
                        color = Color(0xFF1E2818),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(0.8.dp, Color(0xFF22C55E)),
                        modifier = Modifier
                            .clickable { viewModel.openDailyReportDialog() }
                            .padding(end = 4.dp)
                            .testTag("topbar_daily_report_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = "Gün Sonu Raporu",
                                tint = Color(0xFF4ADE80),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(2.5.dp))
                            Text(
                                text = "Gün Sonu",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4ADE80)
                            )
                        }
                    }

                    // İstatistik & Performans Paneli Butonu (Dashboard)
                    Surface(
                        color = Color(0xFF0F2942),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(0.8.dp, iOSBlue),
                        modifier = Modifier
                            .clickable { viewModel.openStatisticsDialog() }
                            .padding(end = 3.dp)
                            .testTag("topbar_statistics_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 4.5.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = "İstatistikler",
                                tint = iOSBlue,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "İstatistik",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = iOSBlue
                            )
                        }
                    }

                    // Servis Müdürü Yönetici Paneli Butonu (Executive Dashboard)
                    Surface(
                        color = Color(0xFF1E3A8A),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(0.8.dp, Color(0xFF60A5FA)),
                        modifier = Modifier
                            .clickable { viewModel.openServiceManagerDialog() }
                            .padding(end = 3.dp)
                            .testTag("topbar_service_manager_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 4.5.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Servis Müdürü Paneli",
                                tint = Color(0xFF93C5FD),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "Müdür",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF93C5FD)
                            )
                        }
                    }

                    // Master Power On/Off Switch Button (Sistem & Arka Plan Güç Anahtarı)
                    Surface(
                        color = if (masterPowerEnabled) Color(0xFF0F2942) else Color(0xFF2C1E20),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(0.8.dp, if (masterPowerEnabled) iOSBlue else iOSRed),
                        modifier = Modifier
                            .clickable { viewModel.setMasterPower(!masterPowerEnabled) }
                            .padding(end = 4.dp)
                            .testTag("topbar_master_power_switch")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = "Sistem Güç Anahtarı",
                                tint = if (masterPowerEnabled) iOSBlue else iOSRed,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(2.5.dp))
                            Text(
                                text = if (masterPowerEnabled) "ON" else "OFF",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (masterPowerEnabled) iOSBlue else iOSRed
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = iOSCardSecondary,
                tonalElevation = 6.dp,
                windowInsets = NavigationBarDefaults.windowInsets,
                modifier = Modifier.border(BorderStroke(0.5.dp, iOSBorder))
            ) {
                NavigationBarItem(
                    selected = currentBottomTab == 0,
                    onClick = { viewModel.setBottomNavTab(0) },
                    icon = { Icon(Icons.Default.SupportAgent, contentDescription = "Ana Ekran", modifier = Modifier.size(20.dp)) },
                    label = { Text("Ana Ekran", fontSize = 10.sp, fontWeight = if (currentBottomTab == 0) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = iOSBlue,
                        selectedTextColor = iOSBlue,
                        indicatorColor = iOSBlue.copy(alpha = 0.15f),
                        unselectedIconColor = iOSTextSecondary,
                        unselectedTextColor = iOSTextSecondary
                    )
                )
                NavigationBarItem(
                    selected = currentBottomTab == 1,
                    onClick = { viewModel.setBottomNavTab(1) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (allRecords.isNotEmpty()) {
                                    Badge(containerColor = iOSBlue) {
                                        Text(allRecords.size.toString(), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.History, contentDescription = "Geçmiş", modifier = Modifier.size(20.dp))
                        }
                    },
                    label = { Text("Geçmiş", fontSize = 10.sp, fontWeight = if (currentBottomTab == 1) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = iOSBlue,
                        selectedTextColor = iOSBlue,
                        indicatorColor = iOSBlue.copy(alpha = 0.15f),
                        unselectedIconColor = iOSTextSecondary,
                        unselectedTextColor = iOSTextSecondary
                    )
                )
                NavigationBarItem(
                    selected = currentBottomTab == 2,
                    onClick = { viewModel.setBottomNavTab(2) },
                    icon = {
                        BadgedBox(
                            badge = {
                                val unhandledCount = allMissedCalls.count { it.callType == "MISSED" && !it.isHandled }
                                if (unhandledCount > 0) {
                                    Badge(containerColor = iOSRed) {
                                        Text(unhandledCount.toString(), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.PhoneCallback, contentDescription = "Aramalar", modifier = Modifier.size(20.dp))
                        }
                    },
                    label = { Text("Aramalar", fontSize = 10.sp, fontWeight = if (currentBottomTab == 2) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = iOSBlue,
                        selectedTextColor = iOSBlue,
                        indicatorColor = iOSBlue.copy(alpha = 0.15f),
                        unselectedIconColor = iOSTextSecondary,
                        unselectedTextColor = iOSTextSecondary
                    )
                )
                NavigationBarItem(
                    selected = currentBottomTab == 3,
                    onClick = {
                        viewModel.setBottomNavTab(3)
                        viewModel.openSettings(initialTab = -1)
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Ayarlar", modifier = Modifier.size(20.dp)) },
                    label = { Text("Ayarlar", fontSize = 10.sp, fontWeight = FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = iOSBlue,
                        selectedTextColor = iOSBlue,
                        indicatorColor = iOSBlue.copy(alpha = 0.15f),
                        unselectedIconColor = iOSTextSecondary,
                        unselectedTextColor = iOSTextSecondary
                    )
                )
            }
        }
    ) { innerPadding ->
        when (currentBottomTab) {
            1 -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(iOSBackground)
                ) {
                    HistoryView(
                        viewModel = viewModel,
                        onNavigateToHome = { viewModel.setBottomNavTab(0) }
                    )
                }
            }
            2 -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(iOSBackground)
                ) {
                    MissedCallsView(
                        viewModel = viewModel
                    )
                }
            }
            else -> {
                val isKeyboardOpen = WindowInsets.isImeVisible
                val currentBottomPadding = if (isKeyboardOpen) {
                    WindowInsets.ime.asPaddingValues().calculateBottomPadding()
                } else {
                    innerPadding.calculateBottomPadding()
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = innerPadding.calculateTopPadding(),
                            bottom = currentBottomPadding,
                            start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                            end = innerPadding.calculateEndPadding(LocalLayoutDirection.current)
                        )
                        .background(MaterialTheme.colorScheme.background)
                ) {
            val isWideScreen = maxWidth >= 840.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                // HIZLI İŞLEM ÇUBUĞU (TOOLBAR QUICK ACTIONS: EXCEL, SQL, YEDEKLEME, SIP) - ULTRA KOMPAKT & SABİT
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Excel Export Action Button
                    Button(
                        onClick = { viewModel.openExportDialog() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ExcelGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(5.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(26.dp)
                            .testTag("export_excel_quick_button")
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(11.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Excel", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                    }

                    // SQL Sync Action Button
                    Button(
                        onClick = { viewModel.syncToSql() },
                        enabled = !isSyncing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (unsyncedCount > 0) AccentOrange else Color(0xFF3B82F6),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(5.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(26.dp)
                            .testTag("sync_sql_quick_button")
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 1.2.dp, modifier = Modifier.size(10.dp))
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(11.dp))
                        }
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(if (isSyncing) "..." else "SQL ($unsyncedCount)", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                    }

                    // Veri Yedekleme Quick Button
                    Button(
                        onClick = { viewModel.openSettings(initialTab = 1) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2563EB),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(5.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        modifier = Modifier
                            .weight(1.1f)
                            .height(26.dp)
                            .testTag("backup_quick_action_button")
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(11.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Yedek", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                    }

                    // Verimor SIP Quick Button
                    Surface(
                        modifier = Modifier
                            .weight(1.1f)
                            .height(26.dp)
                            .clickable { viewModel.openSettings(initialTab = 0) }
                            .testTag("sip_quick_action_button"),
                        shape = RoundedCornerShape(5.dp),
                        color = sipBg,
                        border = BorderStroke(0.8.dp, sipFg.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.PhoneInTalk, contentDescription = null, tint = sipFg, modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = if (verimorExtension.isNotBlank()) "SIP: $verimorExtension" else "SIP Ayar",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = sipFg,
                                maxLines = 1
                            )
                        }
                    }
                }

                // KUYRUKTA BEKLEYEN ÇAĞRI ZARİF VE İNCE UYARI BANDI (SLIM BANNER)
                if (waitingCallsQueue.isNotEmpty()) {
                    val nextNum = waitingCallsQueue.firstOrNull()?.callerNumber ?: ""
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 2.dp)
                            .testTag("waiting_calls_banner_card"),
                        color = Color(0xFFFFFBEB),
                        shape = RoundedCornerShape(5.dp),
                        border = BorderStroke(0.8.dp, Color(0xFFF59E0B))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "⚠️",
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(end = 3.dp)
                                )
                                Text(
                                    text = "Bekleyen: ${waitingCallsQueue.size}" + (if (nextNum.isNotBlank()) " | Sıradaki: $nextNum" else ""),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFB45309),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { showWaitingQueueDialog = true },
                                    contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(0.6.dp, Color(0xFFD97706)),
                                    modifier = Modifier.height(20.dp)
                                ) {
                                    Text("Kuyruk", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                                }
                                Button(
                                    onClick = { viewModel.takeNextWaitingCall() },
                                    contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                    modifier = Modifier.height(20.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(9.dp))
                                    Spacer(modifier = Modifier.width(1.dp))
                                    Text("Al", fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // VERİMOR GELEN ÇAĞRI VEYA AKTİF GÖRÜŞME PANELİ (SES & ÇAĞRI KONTROLÜ)
                if (verimorState is com.example.sip.VerimorSipState.Ringing || verimorState is com.example.sip.VerimorSipState.InCall) {
                    val isRinging = verimorState is com.example.sip.VerimorSipState.Ringing
                    val isInCallState = verimorState is com.example.sip.VerimorSipState.InCall
                    val callNum = when (verimorState) {
                        is com.example.sip.VerimorSipState.Ringing -> (verimorState as com.example.sip.VerimorSipState.Ringing).callerNumber
                        is com.example.sip.VerimorSipState.InCall -> (verimorState as com.example.sip.VerimorSipState.InCall).callerNumber
                        else -> activeIncomingCall?.callerNumber ?: "1006"
                    }
                    val callDuration = if (isInCallState) (verimorState as com.example.sip.VerimorSipState.InCall).durationSeconds else 0L

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 2.5.dp)
                            .testTag("active_call_card"),
                        color = if (isInCallState) Color(0xFFEFF6FF) else Color(0xFFDCFCE7),
                        shape = RoundedCornerShape(5.dp),
                        border = BorderStroke(0.8.dp, if (isInCallState) Color(0xFF3B82F6) else ExcelGreen)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).clickable { viewModel.openCallControlDialog() }) {
                                Icon(
                                    imageVector = if (isInCallState) Icons.Default.PhoneInTalk else Icons.Default.Call,
                                    contentDescription = null,
                                    tint = if (isInCallState) Color(0xFF2563EB) else ExcelGreenDark,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                val m = callDuration / 60
                                val s = callDuration % 60
                                val durStr = if (isInCallState) " (%02d:%02d)".format(m, s) else ""
                                Text(
                                    text = "${if (isInCallState) "Görüşmede" else "Çağrı"}: $callNum$durStr",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = if (isInCallState) Color(0xFF1E40AF) else ExcelGreenDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (isRinging) {
                                    Button(
                                        onClick = { viewModel.answerVerimorCall() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                        shape = RoundedCornerShape(4.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                        modifier = Modifier.height(26.dp).testTag("inapp_btn_answer")
                                    ) {
                                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("Cevapla", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { viewModel.declineVerimorCall() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                        shape = RoundedCornerShape(4.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                        modifier = Modifier.height(26.dp).testTag("inapp_btn_decline")
                                    ) {
                                        Icon(Icons.Default.CallEnd, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("Reddet", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFECFDF5),
                                        border = BorderStroke(0.6.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                                        modifier = Modifier.size(28.dp).clickable { viewModel.openCallControlDialog() }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Dialpad,
                                                contentDescription = "Tuş Takımı",
                                                tint = Color(0xFF059669),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isMuted) Color(0xFFFEE2E2) else Color(0xFFF1F5F9),
                                        border = BorderStroke(0.6.dp, if (isMuted) Color(0xFFEF4444) else Color(0xFFCBD5E1)),
                                        modifier = Modifier.size(28.dp).clickable { viewModel.toggleMute() }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                                contentDescription = "Mute",
                                                tint = if (isMuted) Color(0xFFDC2626) else Slate700,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isSpeaker) Color(0xFFDBEAFE) else Color(0xFFF1F5F9),
                                        border = BorderStroke(0.6.dp, if (isSpeaker) Color(0xFF2563EB) else Color(0xFFCBD5E1)),
                                        modifier = Modifier.size(28.dp).clickable { viewModel.toggleSpeaker() }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                if (isSpeaker) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                                contentDescription = "Hoparlör",
                                                tint = if (isSpeaker) Color(0xFF2563EB) else Slate700,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.stopTimer()
                                            viewModel.hangupVerimorCall()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                        shape = RoundedCornerShape(4.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                        modifier = Modifier.height(28.dp).testTag("inapp_btn_hangup")
                                    ) {
                                        Icon(Icons.Default.CallEnd, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("Bitir", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                                    }
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
                    isCompact = true,
                    onStartCallAndClear = { viewModel.startCallAndClear() },
                    onStop = { viewModel.stopTimer() },
                    onReset = { viewModel.resetTimer() }
                )

                Spacer(modifier = Modifier.height(2.5.dp))

                var selectedTab by remember { mutableIntStateOf(0) }

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
                                .height(26.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(0.6.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.HeadsetMic, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("1. Çağrı Girişi", fontSize = 9.5.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("2. Gelişmiş Alanlar", fontSize = 9.5.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        FilledTonalIconButton(
                            onClick = {
                                selectedTab = 0
                                viewModel.toggleShowAdvancedFields()
                            },
                            modifier = Modifier.size(26.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = ExcelGreen.copy(alpha = 0.12f),
                                contentColor = ExcelGreenDark
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "2. Sayfayı Gizle",
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.5.dp))
                } else {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleShowAdvancedFields() },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(5.dp),
                        border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 2.5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Tune,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = ExcelGreenDark
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "2. Sayfayı Göster (Detay ve Gelişmiş Alanlar)",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Aç",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ExcelGreenDark
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    Icons.Default.VisibilityOff,
                                    contentDescription = "Aç",
                                    modifier = Modifier.size(12.dp),
                                    tint = ExcelGreenDark
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.5.dp))
                }

                // Content Card (Clean Single-Screen Form or 2nd Advanced Page)
                ElevatedCard(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Edit Mode Indicator Banner
                        if (formState.editingId != null) {
                            Surface(
                                color = AccentOrange.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(5.dp),
                                border = BorderStroke(0.8.dp, AccentOrange.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 3.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Default.Edit, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Kayıt Düzenleme Modu (#${formState.editingId})",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AccentOrange
                                        )
                                    }
                                    TextButton(
                                        onClick = { viewModel.cancelEditing() },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                    ) {
                                        Text("Vazgeç", fontSize = 9.sp, color = AccentRed)
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
                                prefixTemplates = prefixTemplates,
                                tespitTemplates = tespitTemplates,
                                islemTemplates = islemTemplates,
                                konuTemplates = konuTemplates
                            )
                        }

                        // Alt boşluk
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(2.5.dp))

                // Bottom Action Bar: Compact Clear & Save Buttons - 30% Reduced
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { viewModel.clearForm() },
                            modifier = Modifier.height(28.dp).testTag("clear_form_button"),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Temizle", fontSize = 9.5.sp)
                        }

                        // WhatsApp Servis Fişi Paylaş Butonu
                        val canShareWhatsApp = formState.musteriBayi.isNotBlank() || formState.telefon.isNotBlank() || formState.sicilDigits.isNotBlank()
                        if (canShareWhatsApp) {
                            Button(
                                onClick = { viewModel.shareCurrentFormViaWhatsApp(context) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                contentPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.height(28.dp).testTag("whatsapp_form_share_button")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "WhatsApp", tint = Color.White, modifier = Modifier.size(11.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("WhatsApp", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        if (formState.editingId != null) {
                            OutlinedButton(
                                onClick = { viewModel.cancelEditing() },
                                modifier = Modifier.height(28.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = AccentRed, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("İptal", fontSize = 9.5.sp, color = AccentRed)
                            }
                        }
                    }

                    val currentEditingId = formState.editingId
                    val isManualEdit = currentEditingId != null && currentEditingId > 0
                    Button(
                        onClick = { viewModel.saveRecord() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isManualEdit) AccentOrange else ExcelGreen,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.height(28.dp).testTag("save_record_button")
                    ) {
                        Icon(
                            imageVector = if (isManualEdit) Icons.Default.Check else Icons.Default.Save,
                            contentDescription = "Kaydet",
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isManualEdit) "GÜNCELLE (#$currentEditingId)" else "KAYDET & BİTİR",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
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

    // Modal: Support Statistics & Performance Dashboard
    if (isStatisticsDialogOpen) {
        SupportStatisticsDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.closeStatisticsDialog() }
        )
    }

    // Modal: Daily Shift Report Dialog (Gün Sonu / Vardiya Özeti)
    if (isDailyReportDialogOpen) {
        DailyShiftReportDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.closeDailyReportDialog() }
        )
    }

    // Modal: Knowledge Base & Error Guide Dialog (Arıza & Hata Kodu Rehberi)
    if (isKnowledgeBaseDialogOpen) {
        KnowledgeBaseDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.closeKnowledgeBaseDialog() }
        )
    }

    // Modal: Service Manager Executive Dashboard (Servis Müdürü Paneli)
    if (isServiceManagerDialogOpen) {
        ServiceManagerDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.closeServiceManagerDialog() }
        )
    }

    // Modal: Call Control & DTMF Keypad Pop-Up
    if (isCallControlDialogOpen) {
        val currentCallDuration = if (verimorState is com.example.sip.VerimorSipState.InCall) {
            (verimorState as com.example.sip.VerimorSipState.InCall).durationSeconds
        } else 0L
        CallControlKeypadDialog(
            state = verimorState,
            callDuration = currentCallDuration,
            isMuted = isMuted,
            isSpeaker = isSpeaker,
            isStreaming = isAudioStreaming,
            onDismiss = { viewModel.closeCallControlDialog() },
            onToggleMute = { viewModel.toggleMute() },
            onToggleSpeaker = { viewModel.toggleSpeaker() },
            onHangup = {
                viewModel.stopTimer()
                viewModel.hangupVerimorCall()
                viewModel.closeCallControlDialog()
            },
            onSendDtmf = { viewModel.sendDtmf(it) },
            onSendDtmfSequence = { viewModel.sendDtmfSequence(it) },
            onOpenFullScreen = {
                viewModel.closeCallControlDialog()
                try {
                    val intent = Intent(context, com.example.sip.IncomingCallActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {}
            }
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

    // Intelligent Multi-Candidate Match Picker Dialog - Sadece Ana Form sekmesinde göster
    if (currentBottomTab == 0) {
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

    // 1. Android Standart İzin Uyarı Pop-Up Penceresi
    if (showPermissionRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionRationaleDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Ses ve Arama İzinleri Gerekli",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "Verimor bulut santralinden gelen ve giden çağrılarda ses iletimi (RTP) ve tam ekran çağrı pencerelerinin gösterilebilmesi için Mikrofon (RECORD_AUDIO) ve Bildirim (POST_NOTIFICATIONS) izinleri gereklidir.",
                    fontSize = 13.sp,
                    color = Slate700
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionRationaleDialog = false
                        permissionLauncher.launch(requiredPermissions.toTypedArray())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExcelGreen)
                ) {
                    Text("İzinleri Ver", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showPermissionRationaleDialog = false }
                ) {
                    Text("Daha Sonra")
                }
            }
        )
    }

    // 2. Tam Ekran Çağrı / Arama Ekranı Overlay Modali (Sadece Çalarken / Ringing durumunda gösterilir; cevaplandığı an kullanıcı doğrudan forma geçer)
    if (showFullScreenCallModal && verimorState is com.example.sip.VerimorSipState.Ringing) {
        val isInCall = false
        val callerNum = when (val state = verimorState) {
            is com.example.sip.VerimorSipState.Ringing -> state.callerNumber
            is com.example.sip.VerimorSipState.InCall -> state.callerNumber
            else -> "1006"
        }
        val callerName = when (val state = verimorState) {
            is com.example.sip.VerimorSipState.Ringing -> state.callerName.ifBlank { "Verimor Santral Arayanı" }
            is com.example.sip.VerimorSipState.InCall -> "Görüşme Sürüyor"
            else -> "Bilinmeyen Numara"
        }
        val durationSeconds = 0L

        Dialog(
            onDismissRequest = {
                // Görüşme sürerken forma dönmek için küçültmeye izin verilir
                if (isInCall) {
                    showFullScreenCallModal = false
                }
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = isInCall,
                dismissOnClickOutside = false
            )
        ) {
            InCallOverlayDialogContent(
                callerNumber = callerNum,
                callerName = callerName,
                isInCall = isInCall,
                durationSeconds = durationSeconds,
                isMuted = isMuted,
                isSpeaker = isSpeaker,
                isAudioStreaming = isAudioStreaming,
                onAnswer = { viewModel.answerVerimorCall() },
                onDecline = {
                    viewModel.declineVerimorCall()
                    showFullScreenCallModal = false
                },
                onHangup = {
                    viewModel.stopTimer()
                    viewModel.hangupVerimorCall()
                    showFullScreenCallModal = false
                },
                onToggleMute = { viewModel.toggleMute() },
                onToggleSpeaker = { viewModel.toggleSpeaker() },
                onDismissToForm = { showFullScreenCallModal = false }
            )
        }
    }

    // 3. Kuyrukta Bekleyen Çağrılar Dialogu
    if (showWaitingQueueDialog) {
        WaitingCallsQueueDialog(
            queue = waitingCallsQueue,
            onTakeCall = { callEvent ->
                viewModel.takeSpecificWaitingCall(callEvent)
                showWaitingQueueDialog = false
            },
            onRemoveCall = { callEvent ->
                viewModel.removeWaitingCall(callEvent)
            },
            onClearQueue = {
                viewModel.clearWaitingQueue()
                showWaitingQueueDialog = false
            },
            onDismiss = { showWaitingQueueDialog = false }
        )
    }
}

@Composable
fun WaitingCallsQueueDialog(
    queue: List<com.example.sip.VerimorCallEvent>,
    onTakeCall: (com.example.sip.VerimorCallEvent) -> Unit,
    onRemoveCall: (com.example.sip.VerimorCallEvent) -> Unit,
    onClearQueue: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFEF3C7),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PhoneCallback,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Kuyrukta Bekleyen Çağrılar",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "${queue.size} adet çağrı sırada bekliyor",
                        fontSize = 11.5.sp,
                        color = Color.Gray
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    color = Color(0xFFEFF6FF),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Form Koruma: Siz mevcut form kaydını 'Kaydet & Bitir' ile tamamlayana kadar yeni çağrılar bu havuzda güvenle saklanır.",
                            fontSize = 11.sp,
                            color = Color(0xFF1E40AF)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (queue.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Kuyrukta bekleyen çağrı bulunmuyor.", color = Color.Gray, fontSize = 12.5.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(queue) { callItem ->
                            Surface(
                                color = Color(0xFFFFFBEB),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFFD97706),
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.CallReceived,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = callItem.callerNumber,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp,
                                                color = Slate900
                                            )
                                            if (callItem.callerName.isNotBlank()) {
                                                Text(
                                                    text = callItem.callerName,
                                                    fontSize = 10.5.sp,
                                                    color = Slate700
                                                )
                                            }
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { onRemoveCall(callItem) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Sil",
                                                tint = Color(0xFFDC2626),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Button(
                                            onClick = { onTakeCall(callItem) },
                                            colors = ButtonDefaults.buttonColors(containerColor = ExcelGreen),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("Forma Al", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (queue.isNotEmpty()) {
                TextButton(onClick = onClearQueue) {
                    Text("Tüm Kuyruğu Temizle", color = Color(0xFFDC2626), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    )
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
    isCompact: Boolean = false,
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
        shape = RoundedCornerShape(if (isCompact) 6.dp else 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (isCompact) 8.dp else 10.dp,
                    vertical = if (isCompact) 3.dp else 6.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Status & Timer
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(if (isCompact) 8.dp else 10.dp)
                        .clip(CircleShape)
                        .background(if (isRunning) AccentRed else Color.Gray)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = timerText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = if (isCompact) 14.sp else 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isRunning) (if (isCompact) "CANLI" else "ÇAĞRI AKTİF") else (if (isCompact) "SÜRE" else "KRONOMETRE"),
                            fontSize = if (isCompact) 9.sp else 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isRunning) AccentOrange else Color.LightGray
                        )
                    }

                    if (!isCompact && basTarihi.isNotBlank()) {
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
                        contentPadding = PaddingValues(
                            horizontal = if (isCompact) 6.dp else 8.dp,
                            vertical = if (isCompact) 2.dp else 4.dp
                        ),
                        modifier = Modifier
                            .heightIn(min = if (isCompact) 26.dp else 32.dp)
                            .testTag("timer_start_button")
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Başlat", modifier = Modifier.size(if (isCompact) 13.dp else 15.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(if (isCompact) "Yeni" else "Başlat (Yeni)", fontSize = if (isCompact) 10.sp else 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onStop,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(
                            horizontal = if (isCompact) 6.dp else 8.dp,
                            vertical = if (isCompact) 2.dp else 4.dp
                        ),
                        modifier = Modifier
                            .heightIn(min = if (isCompact) 26.dp else 32.dp)
                            .testTag("timer_stop_button")
                    ) {
                        Icon(imageVector = Icons.Default.Pause, contentDescription = "Durdur", modifier = Modifier.size(if (isCompact) 13.dp else 15.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Durdur", fontSize = if (isCompact) 10.sp else 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                IconButton(
                    onClick = onReset,
                    modifier = Modifier.size(if (isCompact) 24.dp else 28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sıfırla",
                        tint = Color.LightGray,
                        modifier = Modifier.size(if (isCompact) 14.dp else 16.dp)
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
    prefixTemplates: List<SupportTemplate> = emptyList(),
    tespitTemplates: List<SupportTemplate>,
    islemTemplates: List<SupportTemplate>,
    konuTemplates: List<SupportTemplate>
) {
    val focusManager = LocalFocusManager.current
    val focusSicil = remember { FocusRequester() }
    val focusMusteri = remember { FocusRequester() }
    val focusGorusulen = remember { FocusRequester() }
    val focusTelefon = remember { FocusRequester() }
    val focusMail = remember { FocusRequester() }
    val focusTespit = remember { FocusRequester() }
    val focusIslem = remember { FocusRequester() }

    val customerPreviousRecords by viewModel.customerPreviousRecords.collectAsState()
    val scratchpadNote by viewModel.scratchpadNote.collectAsState()
    val quickScenarios by viewModel.quickScenarios.collectAsState()
    var isPreviousRecordsExpanded by remember { mutableStateOf(false) }
    var isScratchpadExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var showVoiceFallbackDialog by remember { mutableStateOf(false) }
    var speechTarget by remember { mutableStateOf<String?>(null) } // "TESPIT" or "ISLEM"

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = matches?.firstOrNull()?.trim()
            if (!spoken.isNullOrBlank()) {
                val formatted = spoken.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale("tr", "TR")) else it.toString()
                }
                if (speechTarget == "TESPIT") {
                    val current = formState.tespit.trim()
                    val updated = if (current.isBlank()) formatted else "$current, $formatted"
                    viewModel.updateTespit(updated)
                    Toast.makeText(context, "🎙️ Tespit sesle dolduruldu", Toast.LENGTH_SHORT).show()
                } else if (speechTarget == "ISLEM") {
                    val current = formState.islem.trim()
                    val updated = if (current.isBlank()) formatted else "$current, $formatted"
                    viewModel.updateIslem(updated)
                    Toast.makeText(context, "🎙️ İşlem sesle dolduruldu", Toast.LENGTH_SHORT).show()
                }
            }
        }
        speechTarget = null
    }

    val startVoiceInput: (String, String) -> Unit = { target, prompt ->
        speechTarget = target
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "tr-TR")
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "tr-TR")
                putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
            }
            speechLauncher.launch(intent)
        } catch (_: Exception) {
            showVoiceFallbackDialog = true
        }
    }

    // 1. Sicil No: Ön Ek (Prefix) Seçici + Doğrudan Sayısal (Numeric) Klavye
    val allPrefixes = remember(prefixTemplates) {
        prefixTemplates.map { it.text.replace("-", "").replace("_", "").replace(" ", "").replace("\n", "").replace("\r", "").uppercase() }.filter { it.isNotBlank() }.distinct()
    }
    var prefixDropdownExpanded by remember { mutableStateOf(false) }
    val currentPrefix = formState.sicilPrefix.replace("-", "").replace("_", "").replace(" ", "").replace("\n", "").replace("\r", "").ifBlank { allPrefixes.firstOrNull() ?: "" }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Ön Ek (Prefix) Açılır Menü Butonu (Kompakt ve Şık)
        Box(modifier = Modifier.wrapContentSize()) {
            OutlinedButton(
                onClick = { prefixDropdownExpanded = true },
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, iOSBlue),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFF0F2942)),
                contentPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp),
                modifier = Modifier
                    .height(54.dp)
                    .testTag("btn_prefix_selector")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = currentPrefix,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Ön Ek Seç",
                        tint = iOSBlue,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = prefixDropdownExpanded,
                onDismissRequest = { prefixDropdownExpanded = false }
            ) {
                allPrefixes.forEach { pfx ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = pfx,
                                fontSize = 11.5.sp,
                                fontWeight = if (currentPrefix == pfx) FontWeight.Bold else FontWeight.Normal,
                                color = if (currentPrefix == pfx) iOSBlue else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            viewModel.updateSicilPrefix(pfx)
                            prefixDropdownExpanded = false
                        },
                        leadingIcon = if (currentPrefix == pfx) {
                            { Icon(Icons.Default.Check, contentDescription = null, tint = iOSBlue, modifier = Modifier.size(14.dp)) }
                        } else null
                    )
                }
            }
        }

        // Sayısal (Numeric) Klavye ile Rakam Giriş Alanı
        OutlinedTextField(
            value = formState.sicilDigits,
            onValueChange = { digits ->
                viewModel.updateSicilDigits(digits)
            },
            textStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = iOSTextPrimary),
            label = { Text("SİCİL NO", fontSize = 9.5.sp, fontWeight = FontWeight.Bold) },
            leadingIcon = {
                Text(
                    text = currentPrefix,
                    fontWeight = FontWeight.ExtraBold,
                    color = iOSBlue,
                    fontSize = 11.5.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
                    if (formState.sicilDigits.isNotBlank()) {
                        IconButton(
                            onClick = { viewModel.updateSicilDigits("") },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Temizle",
                                tint = iOSTextSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                    if (formState.sicilNo.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = iOSCardSecondary,
                            border = BorderStroke(0.6.dp, iOSBorder)
                        ) {
                            Text(
                                text = formState.sicilNo,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.triggerSmartLookup("SICIL", formState.sicilNo, isExplicit = true) },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Sicil No Ara",
                                tint = iOSBlue,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = {
                    viewModel.padSicilDigits()
                    if (formState.sicilNo.isNotBlank()) {
                        viewModel.triggerSmartLookup("SICIL", formState.sicilNo, isExplicit = true)
                    }
                    focusMusteri.requestFocus()
                }
            ),
            singleLine = true,
            maxLines = 1,
            modifier = Modifier
                .weight(1f)
                .height(54.dp)
                .focusRequester(focusSicil)
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused) {
                        viewModel.padSicilDigits()
                    }
                }
                .autoBringIntoViewOnFocus()
                .testTag("input_sicil_no"),
            colors = formTextFieldColors()
        )
    }

    Spacer(modifier = Modifier.height(3.dp))

    // 2. Müşteri / Bayi (Tek satır, tam genişlik)
    OutlinedTextField(
        value = formState.musteriBayi,
        onValueChange = { viewModel.updateMusteriBayi(it) },
        textStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = iOSTextPrimary),
        label = { Text("MÜŞTERİ / BAYİ", fontSize = 9.5.sp, fontWeight = FontWeight.Bold) },
        leadingIcon = { Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(14.dp)) },
        trailingIcon = {
            if (formState.musteriBayi.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.updateMusteriBayi("") },
                        modifier = Modifier.size(20.dp).testTag("clear_musteri_bayi_button")
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Temizle",
                            tint = iOSTextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.triggerSmartLookup("MUSTERI", formState.musteriBayi, isExplicit = true) },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Firma Ara",
                            tint = iOSBlue,
                            modifier = Modifier.size(13.dp)
                        )
                    }
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
        maxLines = 1,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .focusRequester(focusMusteri)
            .autoBringIntoViewOnFocus()
            .testTag("input_musteri_bayi"),
        colors = formTextFieldColors()
    )

    Spacer(modifier = Modifier.height(3.dp))

    // 3. Görüşülen Kişi (Tek satır, tam genişlik)
    OutlinedTextField(
        value = formState.gorusulen,
        onValueChange = { viewModel.updateGorusulen(it) },
        textStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = iOSTextPrimary),
        label = { Text("GÖRÜŞÜLEN KİŞİ", fontSize = 9.5.sp, fontWeight = FontWeight.Bold) },
        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp)) },
        trailingIcon = {
            if (formState.gorusulen.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.updateGorusulen("") },
                        modifier = Modifier.size(20.dp).testTag("clear_gorusulen_button")
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Temizle",
                            tint = iOSTextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.triggerSmartLookup("GORUSULEN", formState.gorusulen, isExplicit = true) },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Kişi Ara",
                            tint = iOSBlue,
                            modifier = Modifier.size(13.dp)
                        )
                    }
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
        maxLines = 1,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .focusRequester(focusGorusulen)
            .autoBringIntoViewOnFocus()
            .testTag("input_gorusulen"),
        colors = formTextFieldColors()
    )

    Spacer(modifier = Modifier.height(3.dp))

    // 4. Telefon (Tek satır, tam genişlik)
    OutlinedTextField(
        value = formState.telefon,
        onValueChange = { viewModel.updateTelefon(it) },
        textStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = iOSTextPrimary),
        label = { Text("TELEFON", fontSize = 9.5.sp, fontWeight = FontWeight.Bold) },
        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp)) },
        trailingIcon = {
            if (formState.telefon.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.updateTelefon("") },
                        modifier = Modifier.size(20.dp).testTag("clear_telefon_button")
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Temizle",
                            tint = iOSTextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.triggerSmartLookup("TELEFON", formState.telefon, isExplicit = true) },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Telefon Ara",
                            tint = iOSBlue,
                            modifier = Modifier.size(13.dp)
                        )
                    }
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
                focusTespit.requestFocus()
            }
        ),
        singleLine = true,
        maxLines = 1,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .focusRequester(focusTelefon)
            .autoBringIntoViewOnFocus()
            .testTag("input_telefon"),
        colors = formTextFieldColors()
    )

    // Müşterinin Önceki Çağrıları Kartı (Varsa Açılır/Kapanır Gösterim)
    if (customerPreviousRecords.isNotEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 3.dp),
            colors = CardDefaults.cardColors(containerColor = iOSCardSecondary),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(0.6.dp, iOSBlue.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isPreviousRecordsExpanded = !isPreviousRecordsExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            tint = iOSBlue,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Bu Müşterinin Önceki Çağrıları (${customerPreviousRecords.size} Kayıt)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = iOSBlue
                        )
                    }
                    Icon(
                        imageVector = if (isPreviousRecordsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = iOSTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (isPreviousRecordsExpanded) {
                    Spacer(modifier = Modifier.height(6.dp))
                    customerPreviousRecords.take(3).forEach { pastRecord ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            color = iOSCardTertiary,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = pastRecord.baslangicTarihi.take(16),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = iOSTextTertiary
                                    )
                                    TextButton(
                                        onClick = { viewModel.applyPreviousRecordSolution(pastRecord) },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                        modifier = Modifier.height(22.dp)
                                    ) {
                                        Text("Çözümü Forma Al", fontSize = 10.sp, color = iOSOrange, fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (pastRecord.destekKonusu1.isNotBlank()) {
                                    Text(
                                        text = "Konu: ${pastRecord.destekKonusu1}",
                                        fontSize = 10.5.sp,
                                        color = iOSTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (pastRecord.islem1.isNotBlank()) {
                                    Text(
                                        text = "İşlem: ${pastRecord.islem1}",
                                        fontSize = 10.sp,
                                        color = ExcelGreen,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(3.dp))

    // Hızlı Çözüm Senaryoları Çipleri (Tek Dokunuşla Konu + Tespit + İşlem Doldurur)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = Color(0xFF0F2942),
            border = BorderStroke(0.6.dp, iOSBlue),
            modifier = Modifier.clickable { viewModel.openSettings(9) }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚡ HIZLI ŞABLONLAR",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = iOSBlue
                )
                Spacer(modifier = Modifier.width(3.dp))
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Şablonları Düzenle",
                    tint = iOSBlue,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
        quickScenarios.forEach { sc ->
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = iOSCardSecondary,
                border = BorderStroke(0.6.dp, iOSBorder),
                modifier = Modifier.clickable { viewModel.applyQuickScenario(sc) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(sc.icon, fontSize = 9.5.sp)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = sc.title,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = iOSTextPrimary
                    )
                }
            }
        }
        // Şablonları Özelleştir / Ekle butonu
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = Color(0xFF0F2942),
            border = BorderStroke(0.6.dp, iOSBlue.copy(alpha = 0.5f)),
            modifier = Modifier.clickable { viewModel.openSettings(9) }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = iOSBlue, modifier = Modifier.size(10.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text("Özelleştir", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = iOSBlue)
            }
        }
    }

    Spacer(modifier = Modifier.height(3.dp))

    // Destek Konusu - Dropdown / Select (Dinamik ve Temiz Liste)
    val popularTopics by viewModel.popularTopics.collectAsState()
    val popularTespit by viewModel.popularTespit.collectAsState()
    val popularIslem by viewModel.popularIslem.collectAsState()

    val allTopics = remember(konuTemplates, popularTopics) {
        val fromDb = konuTemplates.map { it.text.trim() }.filter { it.isNotBlank() }
        (popularTopics + fromDb).distinct()
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
            textStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = iOSTextPrimary),
            label = { Text("DESTEK KONUSU", fontSize = 9.5.sp, fontWeight = FontWeight.Bold) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = topicDropdownExpanded) },
            singleLine = true,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .menuAnchor()
                .autoBringIntoViewOnFocus()
                .testTag("input_destek_konu"),
            colors = formTextFieldColors()
        )

        if (allTopics.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = topicDropdownExpanded,
                onDismissRequest = { topicDropdownExpanded = false },
                modifier = Modifier.heightIn(max = 200.dp)
            ) {
                allTopics.forEach { topic ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = topic,
                                fontSize = 11.5.sp,
                                fontWeight = if (formState.destekKonu == topic) FontWeight.Bold else FontWeight.Normal,
                                color = if (formState.destekKonu == topic) iOSBlue else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            viewModel.updateDestekKonu(topic)
                            topicDropdownExpanded = false
                        },
                        leadingIcon = if (formState.destekKonu == topic) {
                            { Icon(Icons.Default.Check, contentDescription = null, tint = iOSBlue, modifier = Modifier.size(13.dp)) }
                        } else null
                    )
                }
            }
        }
    }

    // Hızlı Konu Seçim Çipleri (Tek Satır, Yatay Kaydırılabilir)
    if (allTopics.isNotEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(top = 2.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            allTopics.forEach { topic ->
                val isSelected = formState.destekKonu == topic
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isSelected) iOSBlue else iOSCardSecondary,
                    border = BorderStroke(0.6.dp, if (isSelected) iOSBlue else iOSBorder),
                    modifier = Modifier.clickable { viewModel.updateDestekKonu(topic) }
                ) {
                    Text(
                        text = topic,
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else iOSTextPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        maxLines = 1
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(3.dp))

    // TESPİT (Sorun / Arıza - Tek Satır, Sağa Kaydırılabilir, İkinci Satıra Geçmez)
    AutoCompleteTemplateField(
        value = formState.tespit,
        onValueChange = { viewModel.updateTespit(it) },
        label = "TESPİT",
        placeholder = "",
        templates = tespitTemplates,
        popularChoices = popularTespit,
        testTag = "input_tespit",
        onVoiceInputClick = {
            startVoiceInput("TESPIT", "Lütfen tespit / arıza konusunu konuşarak söyleyin...")
        },
        modifier = Modifier.focusRequester(focusTespit),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focusIslem.requestFocus() })
    )

    // Dynamic Popular Tespit Chips (Tek Satır, Yatay Kaydırılabilir)
    val tespitChips = remember(popularTespit, tespitTemplates) {
        val fromDb = tespitTemplates.map { it.text.trim() }.filter { it.isNotBlank() }
        (popularTespit + fromDb).distinct()
    }
    if (tespitChips.isNotEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(top = 2.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tespitChips.forEach { chipText ->
                val currentList = formState.tespit.split(",").map { it.trim() }.filter { it.isNotBlank() }
                val isSelected = currentList.any { it.equals(chipText, ignoreCase = true) }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isSelected) iOSBlue else iOSCardSecondary,
                    border = BorderStroke(0.6.dp, if (isSelected) iOSBlue else iOSBorder),
                    modifier = Modifier.clickable {
                        val updated = if (isSelected) {
                            currentList.filter { !it.equals(chipText, ignoreCase = true) }
                        } else {
                            currentList + chipText
                        }
                        viewModel.updateTespit(updated.joinToString(", "))
                    }
                ) {
                    Text(
                        text = chipText,
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else iOSTextPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        maxLines = 1
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(3.dp))

    // İŞLEM (Yapılan Müdahale - Tek Satır, Sağa Kaydırılabilir, İkinci Satıra Geçmez)
    AutoCompleteTemplateField(
        value = formState.islem,
        onValueChange = { viewModel.updateIslem(it) },
        label = "İŞLEM",
        placeholder = "",
        templates = islemTemplates,
        popularChoices = popularIslem,
        testTag = "input_islem",
        onVoiceInputClick = {
            startVoiceInput("ISLEM", "Lütfen yapılan müdahale / işlemi konuşarak söyleyin...")
        },
        modifier = Modifier.focusRequester(focusIslem),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
    )

    // Dynamic Popular Islem Chips (Tek Satır, Yatay Kaydırılabilir)
    val islemChips = remember(popularIslem, islemTemplates) {
        val fromDb = islemTemplates.map { it.text.trim() }.filter { it.isNotBlank() }
        (popularIslem + fromDb).distinct()
    }
    if (islemChips.isNotEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(top = 2.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            islemChips.forEach { chipText ->
                val currentList = formState.islem.split(",").map { it.trim() }.filter { it.isNotBlank() }
                val isSelected = currentList.any { it.equals(chipText, ignoreCase = true) }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isSelected) iOSPurple else iOSCardSecondary,
                    border = BorderStroke(0.6.dp, if (isSelected) iOSPurple else iOSBorder),
                    modifier = Modifier.clickable {
                        val updated = if (isSelected) {
                            currentList.filter { !it.equals(chipText, ignoreCase = true) }
                        } else {
                            currentList + chipText
                        }
                        viewModel.updateIslem(updated.joinToString(", "))
                    }
                ) {
                    Text(
                        text = chipText,
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else iOSTextPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        maxLines = 1
                    )
                }
            }
        }
    }

    // Sesle Dikte Bilgilendirme İpucu
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 1.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = null,
            tint = iOSBlue,
            modifier = Modifier.size(11.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = "Sesle Dikte: Tespit ve İşlem kutularındaki mikrofon simgesine dokunup konuşarak otomatik doldurabilirsiniz.",
            fontSize = 8.5.sp,
            color = iOSTextSecondary
        )
    }

    Spacer(modifier = Modifier.height(3.dp))

    // Görüşme İçi Karalama Defteri / Hızlı Not Kutusu (Live Scratchpad)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        colors = CardDefaults.cardColors(containerColor = iOSCardSecondary),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.5.dp, if (scratchpadNote.isNotBlank()) iOSOrange.copy(alpha = 0.6f) else iOSBorder)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isScratchpadExpanded = !isScratchpadExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EditNote, contentDescription = null, tint = iOSOrange, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (scratchpadNote.isNotBlank()) "📝 Hızlı Not (Dolu: ${scratchpadNote.length} harf)" else "📝 Görüşme İçi Hızlı Not (Karalama)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (scratchpadNote.isNotBlank()) iOSOrange else iOSTextPrimary
                    )
                }
                Icon(
                    imageVector = if (isScratchpadExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = iOSTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }

            if (isScratchpadExpanded) {
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = scratchpadNote,
                    onValueChange = { viewModel.updateScratchpadNote(it) },
                    placeholder = { Text("Görüşme sırasında hızlıca not alın, hata kodu veya numara yazın...", fontSize = 10.5.sp, color = iOSTextTertiary) },
                    textStyle = TextStyle(fontSize = 11.sp, color = iOSTextPrimary),
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    colors = formTextFieldColors()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { viewModel.appendScratchpadToTespit() },
                        enabled = scratchpadNote.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F2942)),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text("Tespite Aktar", fontSize = 9.5.sp, color = iOSBlue, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.appendScratchpadToIslem() },
                        enabled = scratchpadNote.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF064E3B)),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text("İşleme Aktar", fontSize = 9.5.sp, color = ExcelGreen, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    if (scratchpadNote.isNotBlank()) {
                        TextButton(
                            onClick = { viewModel.clearScratchpad() },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text("Temizle", fontSize = 9.5.sp, color = iOSRed)
                        }
                    }
                }
            }
        }
    }

    if (showVoiceFallbackDialog) {
        AlertDialog(
            onDismissRequest = { showVoiceFallbackDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = iOSBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("🎙️ Sesle Form Dikte", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Cihazınızda ses tanıma servisi (Google Sesli Arama / Konuşarak Yazma) başlatılamadı.",
                        fontSize = 12.sp,
                        color = iOSTextPrimary
                    )
                    Text(
                        "💡 Pratik Çözüm: Android klavyeniz açıldığında boşluk tuşunun yanındaki mikrofon simgesine basarak da Türkçe metin dikte edebilirsiniz.",
                        fontSize = 11.5.sp,
                        color = iOSTextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showVoiceFallbackDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = iOSBlue)
                ) {
                    Text("Tamam")
                }
            }
        )
    }
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
                label = { Text("TELEFON 2", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.PhoneCallback, contentDescription = null, modifier = Modifier.size(16.dp)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .autoBringIntoViewOnFocus()
                    .testTag("input_telefon2"),
                colors = formTextFieldColors()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // E-POSTA ALANI (Ana ekrandan 2. Gelişmiş Alanlar sekmesine taşınan alan)
            Text(
                text = "E-POSTA ADRESİ:",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = formState.mail,
                onValueChange = { viewModel.updateMail(it) },
                label = { Text("E-POSTA", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                singleLine = true,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .autoBringIntoViewOnFocus()
                    .testTag("input_mail"),
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
                    label = { Text("REDMINE NO", fontSize = 11.sp) },
                    singleLine = true,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .autoBringIntoViewOnFocus()
                        .testTag("input_redmine"),
                    colors = formTextFieldColors()
                )
                OutlinedTextField(
                    value = formState.bagId,
                    onValueChange = { viewModel.updateBagId(it) },
                    label = { Text("BAĞLANTI ID", fontSize = 11.sp) },
                    singleLine = true,
                    maxLines = 1,
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
                    singleLine = true,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .autoBringIntoViewOnFocus()
                        .testTag("input_b2b_kayit"),
                    colors = formTextFieldColors()
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // 3. Zaman Damgaları & Süre
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = "ZAMAN DAMGALARI & SÜRE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = formState.basTarihi,
                    onValueChange = { viewModel.updateBasTarihi(it) },
                    label = { Text("BAŞLANGIÇ", fontSize = 11.sp) },
                    singleLine = true,
                    maxLines = 1,
                    modifier = Modifier.weight(1f).autoBringIntoViewOnFocus(),
                    colors = formTextFieldColors()
                )
                OutlinedTextField(
                    value = formState.bitTarihi,
                    onValueChange = { viewModel.updateBitTarihi(it) },
                    label = { Text("BİTİŞ", fontSize = 11.sp) },
                    singleLine = true,
                    maxLines = 1,
                    modifier = Modifier.weight(1f).autoBringIntoViewOnFocus(),
                    colors = formTextFieldColors()
                )
                OutlinedTextField(
                    value = formState.sure,
                    onValueChange = { viewModel.updateSure(it) },
                    label = { Text("SÜRE", fontSize = 11.sp) },
                    singleLine = true,
                    maxLines = 1,
                    modifier = Modifier.weight(0.8f).autoBringIntoViewOnFocus(),
                    colors = formTextFieldColors()
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

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
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("AR-GE GİDİŞ:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
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
                    label = { Text("AR-GE GİDİŞ NOTU", fontSize = 11.sp) },
                    singleLine = true,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth().autoBringIntoViewOnFocus(),
                    colors = formTextFieldColors()
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("AR-GE DÖNÜŞ:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
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
                    label = { Text("AR-GE DÖNÜŞ NOTU", fontSize = 11.sp) },
                    singleLine = true,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth().autoBringIntoViewOnFocus(),
                    colors = formTextFieldColors()
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

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

            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = formState.ynokcVersiyon,
                    onValueChange = { viewModel.updateYnokcVersiyon(it) },
                    label = { Text("YNOKC VERSİYON", fontSize = 10.5.sp) },
                    singleLine = true,
                    maxLines = 1,
                    modifier = Modifier.weight(1f).autoBringIntoViewOnFocus(),
                    colors = formTextFieldColors()
                )
                OutlinedTextField(
                    value = formState.dynCashVersiyon,
                    onValueChange = { viewModel.updateDynCashVersiyon(it) },
                    label = { Text("DYNCASH VERSİYON", fontSize = 10.5.sp) },
                    singleLine = true,
                    maxLines = 1,
                    modifier = Modifier.weight(1f).autoBringIntoViewOnFocus(),
                    colors = formTextFieldColors()
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = formState.yn500Versiyon,
                    onValueChange = { viewModel.updateYn500Versiyon(it) },
                    label = { Text("YN500 VERSİYON", fontSize = 10.5.sp) },
                    singleLine = true,
                    maxLines = 1,
                    modifier = Modifier.weight(1f).autoBringIntoViewOnFocus(),
                    colors = formTextFieldColors()
                )
                OutlinedTextField(
                    value = formState.arkaOfisVersiyon,
                    onValueChange = { viewModel.updateArkaOfisVersiyon(it) },
                    label = { Text("ARKA OFİS VERSİYON", fontSize = 10.5.sp) },
                    singleLine = true,
                    maxLines = 1,
                    modifier = Modifier.weight(1f).autoBringIntoViewOnFocus(),
                    colors = formTextFieldColors()
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = formState.servisKodu,
                onValueChange = { viewModel.updateServisKodu(it) },
                label = { Text("SERVİS KODU", fontSize = 10.5.sp) },
                singleLine = true,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth().autoBringIntoViewOnFocus(),
                colors = formTextFieldColors()
            )
        }
    }
}

// -------------------------------------------------------------
// AUTO-COMPLETE TEMPLATE FIELD (Çoklu Seçim & Popüler Sıralamalı)
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoCompleteTemplateField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    templates: List<SupportTemplate>,
    popularChoices: List<String> = emptyList(),
    testTag: String,
    onVoiceInputClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    var expanded by remember { mutableStateOf(false) }

    val currentItems = remember(value) {
        value.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }

    // Determine query for typing filter (if user types after comma, take that last piece)
    val activeQuery = remember(value) {
        val lastPart = value.substringAfterLast(",", "").trim()
        if (lastPart.isNotBlank()) lastPart else value.trim()
    }

    val allCandidates = remember(templates, popularChoices) {
        val fromTemplates = templates.map { it.text.trim() }.filter { it.isNotBlank() }
        (popularChoices + fromTemplates).distinct()
    }

    val filteredList = remember(allCandidates, activeQuery) {
        if (activeQuery.isBlank()) allCandidates else {
            allCandidates.filter { it.contains(activeQuery, ignoreCase = true) }
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded && filteredList.isNotEmpty(),
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it.uppercase(Locale("tr", "TR")))
                expanded = true
            },
            textStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = iOSTextPrimary),
            label = { Text(label, fontSize = 9.5.sp, fontWeight = FontWeight.Bold) },
            placeholder = if (placeholder.isNotBlank()) { { Text(placeholder, fontSize = 9.5.sp) } } else null,
            singleLine = true,
            maxLines = 1,
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
                    if (onVoiceInputClick != null) {
                        IconButton(
                            onClick = onVoiceInputClick,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(iOSBlue.copy(alpha = 0.18f))
                                .testTag("${testTag}_mic_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Sesle Yazdır (Dikte)",
                                tint = iOSBlue,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(3.dp))
                    }
                    if (value.isNotBlank()) {
                        IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Clear, contentDescription = "Temizle", modifier = Modifier.size(13.dp))
                        }
                    }
                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(20.dp).menuAnchor()
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = "Hazır Şablonlar Menüsü",
                            tint = iOSBlue,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            },
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            modifier = modifier
                .fillMaxWidth()
                .height(54.dp)
                .menuAnchor()
                .autoBringIntoViewOnFocus()
                .testTag(testTag),
            colors = formTextFieldColors()
        )

        if (filteredList.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp)
                    .background(iOSCardSecondary)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (activeQuery.isBlank()) "Şablon Seçimi (${filteredList.size}):" else "Filtrelenenler ('$activeQuery'):",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = iOSBlue
                    )
                    TextButton(
                        onClick = { expanded = false },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text("Tamam", fontSize = 9.5.sp, color = iOSBlue, fontWeight = FontWeight.Bold)
                    }
                }
                HorizontalDivider(color = iOSBorder)

                filteredList.forEach { candidateText ->
                    val isChecked = currentItems.any { it.equals(candidateText, ignoreCase = true) }
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = iOSBlue,
                                        uncheckedColor = iOSTextSecondary
                                    ),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = candidateText,
                                    fontSize = 10.sp,
                                    color = if (isChecked) iOSBlue else iOSTextPrimary,
                                    fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        onClick = {
                            val newSelected = if (isChecked) {
                                currentItems.filter { !it.equals(candidateText, ignoreCase = true) }
                            } else {
                                val base = currentItems.filter { !it.equals(activeQuery, ignoreCase = true) }
                                base + candidateText
                            }
                            onValueChange(newSelected.joinToString(", "))
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
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
    focusedTextColor = iOSTextPrimary,
    unfocusedTextColor = iOSTextPrimary,
    focusedBorderColor = iOSBlue,
    unfocusedBorderColor = Color(0xFF475569),
    focusedLabelColor = iOSBlue,
    unfocusedLabelColor = Color(0xFFCBD5E1),
    focusedContainerColor = iOSCard,
    unfocusedContainerColor = iOSCard,
    focusedLeadingIconColor = iOSBlue,
    unfocusedLeadingIconColor = Color(0xFF94A3B8),
    focusedTrailingIconColor = iOSBlue,
    unfocusedTrailingIconColor = Color(0xFF94A3B8),
    cursorColor = iOSBlue
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
    val context = LocalContext.current
    val filteredRecords = remember(startDate, endDate) {
        viewModel.getFilteredRecordsForExport()
    }

    var activePickerTarget by remember { mutableStateOf<String?>(null) } // "start" or "end"

    // Pure Jetpack Compose Calendar Picker Modal (No keyboard text input, full interactive calendar)
    if (activePickerTarget == "start") {
        AppComposeDatePickerDialog(
            initialDateYmd = startDate,
            title = "Başlangıç Tarihi Seçiniz",
            onDateSelected = {
                viewModel.setExportStartDate(it)
                activePickerTarget = null
            },
            onDismiss = { activePickerTarget = null }
        )
    } else if (activePickerTarget == "end") {
        AppComposeDatePickerDialog(
            initialDateYmd = endDate,
            title = "Bitiş Tarihi Seçiniz",
            onDateSelected = {
                viewModel.setExportEndDate(it)
                activePickerTarget = null
            },
            onDismiss = { activePickerTarget = null }
        )
    }

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

                // Date Selectors (Takvimden doğrudan seçim - Elle yazma yok, takvim açılır)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Başlangıç Tarihi Kartı
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { activePickerTarget = "start" }
                            .testTag("export_start_date"),
                        colors = CardDefaults.cardColors(containerColor = ExcelGreenLight.copy(alpha = 0.4f)),
                        border = BorderStroke(1.2.dp, ExcelGreen)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        tint = ExcelGreenDark,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "BAŞLANGIÇ",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ExcelGreenDark
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "Takvim",
                                    tint = ExcelGreenDark,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = viewModel.formatYmdToDisplay(startDate),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "📅 Takvimden Seç",
                                fontSize = 9.5.sp,
                                color = ExcelGreenDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Bitiş Tarihi Kartı
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { activePickerTarget = "end" }
                            .testTag("export_end_date"),
                        colors = CardDefaults.cardColors(containerColor = ExcelGreenLight.copy(alpha = 0.4f)),
                        border = BorderStroke(1.2.dp, ExcelGreen)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = null,
                                        tint = ExcelGreenDark,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "BİTİŞ",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ExcelGreenDark
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "Takvim",
                                    tint = ExcelGreenDark,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = viewModel.formatYmdToDisplay(endDate),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "📅 Takvimden Seç",
                                fontSize = 9.5.sp,
                                color = ExcelGreenDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = ExcelGreenDark,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Tarih kutularına dokunarak takvimden gün, ay ve yılı kolayca seçebilirsiniz.",
                        fontSize = 9.5.sp,
                        color = Slate700
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

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

                // Outlook / E-Posta ile Gönder Butonu (Doğrudan servis@enpos.com.tr)
                Button(
                    onClick = { viewModel.exportAndShareExcelViaOutlook(context) },
                    enabled = filteredRecords.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0078D4),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Outlook / E-Posta ile Gönder", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

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
// -------------------------------------------------------------
// MODAL 2: SETTINGS (EXTRACTED TO com.example.ui.SettingsDialog.kt)
// -------------------------------------------------------------

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

@Composable
fun InCallOverlayDialogContent(
    callerNumber: String,
    callerName: String,
    isInCall: Boolean,
    durationSeconds: Long,
    isMuted: Boolean,
    isSpeaker: Boolean,
    isAudioStreaming: Boolean,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
    onHangup: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onDismissToForm: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
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
            // ÜST BAŞLIK & FORM KISAYOLU
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
                    border = BorderStroke(1.dp, iOSBlue.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneInTalk,
                            contentDescription = null,
                            tint = iOSBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "VERİMOR SIP",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = iOSBlue
                        )
                    }
                }

                if (isInCall) {
                    Button(
                        onClick = onDismissToForm,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("overlay_btn_return_form")
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Destek Formuna Dön", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ORTA ALAN: Arayan Bilgisi ve Canlı Durum
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 24.dp)
            ) {
                // Titreşen Çağrı Avatarı
                Box(contentAlignment = Alignment.Center) {
                    if (!isInCall) {
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(Color(0x3310B981))
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(if (isInCall) Color(0xFF2563EB) else Color(0xFF16A34A))
                            .border(3.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isInCall) Icons.Default.PhoneInTalk else Icons.Default.Call,
                            contentDescription = "Arama",
                            tint = Color.White,
                            modifier = Modifier.size(54.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = callerNumber,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = callerName,
                    fontSize = 16.sp,
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (isInCall) {
                    val m = durationSeconds / 60
                    val s = durationSeconds % 60
                    Surface(
                        color = Color(0x333B82F6),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0x663B82F6))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Görüşme Süresi: %02d:%02d".format(m, s),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF60A5FA)
                            )
                            if (isAudioStreaming) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "• RTP Ses Aktif",
                                    fontSize = 12.sp,
                                    color = Color(0xFF4ADE80),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Santralden Gelen Çağrı Yanıt Bekliyor...",
                        fontSize = 14.sp,
                        color = Color(0xFF34D399),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // ALT ALAN: KONTROL BUTONLARI (CEVAPLA / REDDET VEYA MUTE / SPEAKER / BİTİR)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isInCall) {
                    // MUTE VE SPEAKER KONTROLLERİ
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // MUTE BUTONU
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = onToggleMute,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(if (isMuted) Color(0xFFEF4444) else Color(0x33FFFFFF))
                                    .testTag("overlay_btn_mute")
                            ) {
                                Icon(
                                    imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = "Mute",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isMuted) "Sessiz" else "Mikrofon",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        // HOPARLÖR BUTONU
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = onToggleSpeaker,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(if (isSpeaker) Color(0xFF3B82F6) else Color(0x33FFFFFF))
                                    .testTag("overlay_btn_speaker")
                            ) {
                                Icon(
                                    imageVector = if (isSpeaker) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                    contentDescription = "Hoparlör",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isSpeaker) "Hoparlör" else "Ahize",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // ARAMAYI SONLANDIR (BİTİR) BUTONU
                    Button(
                        onClick = onHangup,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(72.dp)
                            .testTag("overlay_btn_hangup"),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "Sonlandır",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                } else {
                    // CEVAPLA (YEŞİL) VE REDDET (KIRMIZI) BUTONLARI
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // REDDET (Kırmızı)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Button(
                                onClick = onDecline,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(74.dp)
                                    .testTag("overlay_btn_decline"),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CallEnd,
                                    contentDescription = "Reddet",
                                    tint = Color.White,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Reddet", color = Color(0xFFEF4444), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }

                        // CEVAPLA (iOS Mavi)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Button(
                                onClick = onAnswer,
                                colors = ButtonDefaults.buttonColors(containerColor = iOSBlue),
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(74.dp)
                                    .testTag("overlay_btn_answer"),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "Cevapla",
                                    tint = Color.White,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Cevapla", color = iOSBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
