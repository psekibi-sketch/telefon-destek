package com.example.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.RepresentativePreset
import com.example.model.SupportTemplate
import com.example.ui.theme.*

@Composable
fun SettingsDialog(
    viewModel: SupportViewModel,
    initialTab: Int = -1,
    onDismiss: () -> Unit
) {
    var selectedSettingsTab by remember(initialTab) { mutableIntStateOf(initialTab) }

    // iOS BackHandler: If inside a subpage, back button returns to the modular cards menu
    BackHandler(enabled = selectedSettingsTab != -1) {
        selectedSettingsTab = -1
    }
    val tespitTemplates by viewModel.tespitTemplates.collectAsState()
    val islemTemplates by viewModel.islemTemplates.collectAsState()
    val konuTemplates by viewModel.konuTemplates.collectAsState()
    val prefixTemplates by viewModel.prefixTemplates.collectAsState()
    val showAdvanced by viewModel.showAdvancedFields.collectAsState()

    var newPrefixText by remember { mutableStateOf("") }

    val deleteStart by viewModel.deleteStartDate.collectAsState()
    val deleteEnd by viewModel.deleteEndDate.collectAsState()
    val globalKullanici by viewModel.globalKullanici.collectAsState()
    val globalKullaniciKodu by viewModel.globalKullaniciKodu.collectAsState()
    val outlookRecipientEmail by viewModel.outlookRecipientEmail.collectAsState()
    val representatives by viewModel.representatives.collectAsState()
    val dynamicBaseUrl by viewModel.dynamicBaseUrl.collectAsState()
    val unsyncedCount by viewModel.unsyncedCount.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    var newRepName by remember { mutableStateOf("") }
    var newRepCode by remember { mutableStateOf("") }
    var editingRep by remember { mutableStateOf<RepresentativePreset?>(null) }
    var editRepName by remember { mutableStateOf("") }
    var editRepCode by remember { mutableStateOf("") }
    var repToDelete by remember { mutableStateOf<RepresentativePreset?>(null) }

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

    val allRecords by viewModel.allRecords.collectAsState()
    val isBackingUp by viewModel.isBackingUp.collectAsState()
    val isRestoring by viewModel.isRestoring.collectAsState()
    val backupPreview by viewModel.backupPreview.collectAsState()
    val lastBackupInfo by viewModel.lastBackupInfo.collectAsState()
    val storageBreakdown by viewModel.storageBreakdown.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.refreshStorageBreakdown(context)
    }

    val backupPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.loadBackupForPreview(context, it) }
    }

    var restoreClearExisting by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showLocalMemoryCleanDialog by remember { mutableStateOf(false) }
    var newTemplateText by remember { mutableStateOf("") }

    val quickScenarios by viewModel.quickScenarios.collectAsState()
    var isScenarioDialogOpen by remember { mutableStateOf(false) }
    var editingScenario by remember { mutableStateOf<QuickScenario?>(null) }
    var scenarioTitle by remember { mutableStateOf("") }
    var scenarioIcon by remember { mutableStateOf("⚡") }
    var scenarioKonu by remember { mutableStateOf("") }
    var scenarioTespit by remember { mutableStateOf("") }
    var scenarioIslem by remember { mutableStateOf("") }
    var scenarioToDelete by remember { mutableStateOf<QuickScenario?>(null) }
    var showResetScenariosConfirm by remember { mutableStateOf(false) }
    var showPwaPreviewDialog by remember { mutableStateOf(false) }

    if (showPwaPreviewDialog) {
        com.example.export.PwaPreviewWebViewDialog(
            records = allRecords,
            templates = tespitTemplates + islemTemplates + konuTemplates,
            globalKullanici = globalKullanici,
            globalKullaniciKodu = globalKullaniciKodu,
            onDismiss = { showPwaPreviewDialog = false }
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
                .fillMaxHeight(0.90f)
                .imePadding()
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (selectedSettingsTab == -1) {
                    // MAIN MODULAR CARD SETTINGS HEADER (Requirement 11)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF0F2942),
                                border = BorderStroke(1.dp, iOSBlue.copy(alpha = 0.4f)),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Settings, contentDescription = null, tint = iOSBlue, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Ayarlar ve Yapılandırma",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Modüler Ayar Kutuları ve Yönetim",
                                    fontSize = 11.sp,
                                    color = iOSTextSecondary
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Kapat", tint = iOSTextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // KUTUCUKLU AYARLAR MENÜSÜ (Requirement 11)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val sipMiniStatus = when (verimorState) {
                            is com.example.sip.VerimorSipState.Registered -> "Kayıtlı ($verimorExtension)"
                            is com.example.sip.VerimorSipState.Connecting -> "Bağlanıyor..."
                            is com.example.sip.VerimorSipState.Error -> "Hata"
                            else -> if (verimorExtension.isNotBlank()) "Pasif ($verimorExtension)" else "Yapılandırılmadı"
                        }
                        val sipBadgeColor = when (verimorState) {
                            is com.example.sip.VerimorSipState.Registered -> iOSBlue
                            is com.example.sip.VerimorSipState.Connecting -> iOSOrange
                            is com.example.sip.VerimorSipState.Error -> iOSRed
                            else -> iOSTextSecondary
                        }
                        SettingsMenuBox(
                            title = "1. Verimor SIP Bulut Santral",
                            subtitle = "Domain: ${verimorDomain.ifBlank { "verimor.com.tr" }} • Dahili: ${verimorExtension.ifBlank { "-" }}",
                            icon = Icons.Default.PhoneInTalk,
                            iconTint = iOSBlue,
                            iconBg = Color(0xFF0F2942),
                            badgeText = sipMiniStatus,
                            badgeColor = sipBadgeColor,
                            onClick = { selectedSettingsTab = 0 }
                        )

                        SettingsMenuBox(
                            title = "2. Veri Yedekleme & Geri Yükleme",
                            subtitle = "Export / Import (.json), Bulut ve Dosya Yedeği",
                            icon = Icons.Default.Backup,
                            iconTint = iOSBlue,
                            iconBg = Color(0xFF0F2942),
                            badgeText = "${allRecords.size} Kayıt",
                            badgeColor = iOSBlue,
                            onClick = { selectedSettingsTab = 1 }
                        )

                        SettingsMenuBox(
                            title = "3. Hızlı Çözüm Şablonları (Senaryolar)",
                            subtitle = "Tek dokunuşla Konu, Tespit ve İşlem dolduran ${quickScenarios.size} akıllı senaryo",
                            icon = Icons.Default.Bolt,
                            iconTint = Color(0xFFF59E0B),
                            iconBg = Color(0xFF33200A),
                            badgeText = "${quickScenarios.size} Şablon",
                            badgeColor = Color(0xFFF59E0B),
                            onClick = { selectedSettingsTab = 9 }
                        )

                        SettingsMenuBox(
                            title = "4. Tespit Şablonları Yönetimi",
                            subtitle = "Hızlı seçim ve otomatik tamamlama listesi",
                            icon = Icons.Default.FindInPage,
                            iconTint = iOSOrange,
                            iconBg = Color(0xFF33200A),
                            badgeText = "${tespitTemplates.size} Şablon",
                            badgeColor = iOSOrange,
                            onClick = { selectedSettingsTab = 2; newTemplateText = "" }
                        )

                        SettingsMenuBox(
                            title = "5. İşlem Şablonları Yönetimi",
                            subtitle = "Destek işlemi ve çözüm şablonları listesi",
                            icon = Icons.Default.Build,
                            iconTint = iOSPurple,
                            iconBg = Color(0xFF2E103E),
                            badgeText = "${islemTemplates.size} Şablon",
                            badgeColor = iOSPurple,
                            onClick = { selectedSettingsTab = 3; newTemplateText = "" }
                        )

                        SettingsMenuBox(
                            title = "6. Destek Konuları Yönetimi",
                            subtitle = "Hızlı konu çipleri ve şablon listesi",
                            icon = Icons.Default.Topic,
                            iconTint = iOSTeal,
                            iconBg = Color(0xFF0A2B33),
                            badgeText = "${konuTemplates.size} Konu",
                            badgeColor = iOSTeal,
                            onClick = { selectedSettingsTab = 4; newTemplateText = "" }
                        )

                        SettingsMenuBox(
                            title = "7. SQL Sync & Veritabanı",
                            subtitle = if (sqlHost.isNotBlank()) "$sqlHost / $sqlDbName ($sqlDbType)" else "SQL sunucusu henüz ayarlanmadı",
                            icon = Icons.Default.Storage,
                            iconTint = iOSBlue,
                            iconBg = Color(0xFF0F2942),
                            badgeText = if (unsyncedCount > 0) "$unsyncedCount Bekleyen" else "Güncel",
                            badgeColor = if (unsyncedCount > 0) iOSOrange else iOSBlue,
                            onClick = { selectedSettingsTab = 6 }
                        )

                        SettingsMenuBox(
                            title = "8. Sicil No Ön Ekleri",
                            subtitle = "PC, YNOKC vb. cihaz ön ek kodları",
                            icon = Icons.Default.Badge,
                            iconTint = iOSOrange,
                            iconBg = Color(0xFF33200A),
                            badgeText = "${prefixTemplates.size} Ön Ek",
                            badgeColor = iOSOrange,
                            onClick = { selectedSettingsTab = 7 }
                        )

                        SettingsMenuBox(
                            title = "9. Kullanıcı & Temsilci Yönetimi",
                            subtitle = "Aktif: ${globalKullanici.ifBlank { "Belirtilmedi" }} • Toplu Veri Silme",
                            icon = Icons.Default.People,
                            iconTint = iOSBlue,
                            iconBg = Color(0xFF0F2942),
                            badgeText = "${representatives.size} Temsilci",
                            badgeColor = iOSBlue,
                            onClick = { selectedSettingsTab = 5 }
                        )

                        SettingsMenuBox(
                            title = "10. Sürüm Geçmişi (Changelog)",
                            subtitle = "v2.6.5 • Sesli Dikte, Gün Sonu Raporu & Arıza Rehberi",
                            icon = Icons.Default.Info,
                            iconTint = iOSTextSecondary,
                            iconBg = iOSCardTertiary,
                            badgeText = "v2.6.5",
                            badgeColor = iOSTextSecondary,
                            onClick = { selectedSettingsTab = 8 }
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // 11. GITHUB PAGES PWA SÜRÜMÜ KARTI (ZIP İNDİR & CANLI ÖNİZLE)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = iOSCardSecondary),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, iOSBlue.copy(alpha = 0.45f))
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
                                        Icon(
                                            imageVector = Icons.Default.Language,
                                            contentDescription = null,
                                            tint = iOSBlue,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "11. GitHub Pages PWA Sürümü",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = iOSTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Surface(
                                        color = Color(0xFF0F2942),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = "PWA HAZIR",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = iOSBlue,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tüm özellikleriyle çalışan GitHub Pages PWA sürümü (/docs klasöründe hazır). İsterseniz mevcut verilerinizle birlikte ZIP olarak indirip yeni bir GitHub deposuna yükleyebilir veya canlı önizleyebilirsiniz.",
                                    fontSize = 11.sp,
                                    color = iOSTextSecondary
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            com.example.export.PwaPackageExporter.exportAndSharePwaZip(
                                                context = context,
                                                includeCurrentDatabaseInPwa = true,
                                                records = allRecords,
                                                templates = tespitTemplates + islemTemplates + konuTemplates,
                                                globalKullanici = globalKullanici,
                                                globalKullaniciKodu = globalKullaniciKodu
                                            )
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = ExcelGreen),
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Icon(Icons.Default.FolderZip, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "PWA (.zip) İndir",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Button(
                                        onClick = { showPwaPreviewDialog = true },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = iOSBlue),
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "PWA Canlı Önizle",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) {
                            Text("Kapat", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // SUBPAGE NAVIGATION HEADER
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            color = iOSCardSecondary,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(0.5.dp, iOSBorder),
                            modifier = Modifier.clickable { selectedSettingsTab = -1 }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Geri",
                                    tint = iOSBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Tüm Ayarlar",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = iOSBlue
                                )
                            }
                        }

                        Text(
                            text = when (selectedSettingsTab) {
                                0 -> "Verimor SIP Santral"
                                1 -> "Veri Yedekleme"
                                2 -> "Tespit Şablonları"
                                3 -> "İşlem Şablonları"
                                4 -> "Destek Konuları"
                                5 -> "Kullanıcı & Temsilciler"
                                6 -> "SQL Sync Ayarları"
                                7 -> "Sicil Ön Ekleri"
                                8 -> "Sürüm Geçmişi"
                                9 -> "Hızlı Çözüm Şablonları (Senaryolar)"
                                else -> "Ayar Detayı"
                            },
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = iOSTextPrimary
                        )

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Kapat", tint = iOSTextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Compact Scrollable Tabs for switching directly inside subpages
                    ScrollableTabRow(
                        selectedTabIndex = selectedSettingsTab.coerceIn(0, 9),
                        edgePadding = 0.dp,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        contentColor = iOSBlue,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    ) {
                        Tab(selected = selectedSettingsTab == 0, onClick = { selectedSettingsTab = 0 }, text = { Text("SIP", fontSize = 10.sp) })
                        Tab(selected = selectedSettingsTab == 1, onClick = { selectedSettingsTab = 1 }, text = { Text("Yedek", fontSize = 10.sp) })
                        Tab(selected = selectedSettingsTab == 9, onClick = { selectedSettingsTab = 9 }, text = { Text("Hızlı Şablon (${quickScenarios.size})", fontSize = 10.sp, fontWeight = if (selectedSettingsTab == 9) FontWeight.Bold else FontWeight.Normal) })
                        Tab(selected = selectedSettingsTab == 2, onClick = { selectedSettingsTab = 2; newTemplateText = "" }, text = { Text("Tespitler (${tespitTemplates.size})", fontSize = 10.sp) })
                        Tab(selected = selectedSettingsTab == 3, onClick = { selectedSettingsTab = 3; newTemplateText = "" }, text = { Text("İşlemler (${islemTemplates.size})", fontSize = 10.sp) })
                        Tab(selected = selectedSettingsTab == 4, onClick = { selectedSettingsTab = 4; newTemplateText = "" }, text = { Text("Konular (${konuTemplates.size})", fontSize = 10.sp) })
                        Tab(selected = selectedSettingsTab == 5, onClick = { selectedSettingsTab = 5 }, text = { Text("Kullanıcı", fontSize = 10.sp) })
                        Tab(selected = selectedSettingsTab == 6, onClick = { selectedSettingsTab = 6 }, text = { Text("SQL", fontSize = 10.sp) })
                        Tab(selected = selectedSettingsTab == 7, onClick = { selectedSettingsTab = 7 }, text = { Text("Ön Ek (${prefixTemplates.size})", fontSize = 10.sp) })
                        Tab(selected = selectedSettingsTab == 8, onClick = { selectedSettingsTab = 8 }, text = { Text("Sürüm", fontSize = 10.sp, fontWeight = FontWeight.Bold) })
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                // Tab Content Switch
                when (selectedSettingsTab) {
                    0 -> {
                        // TAB 5: VERİMOR BULUT SANTRAL / SIP AYARLARI
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = iOSCardSecondary),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(0.5.dp, iOSBorder)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.PhoneInTalk, contentDescription = null, tint = iOSBlue, modifier = Modifier.size(22.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Verimor Bulut Santral / SIP",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp,
                                                color = iOSTextPrimary
                                            )
                                        }

                                        // Canlı Durum Rozeti
                                        val statusText = when (verimorState) {
                                            is com.example.sip.VerimorSipState.Registered -> "✓ Kayıtlı ($verimorExtension)"
                                            is com.example.sip.VerimorSipState.Connecting -> "⏳ Bağlanıyor..."
                                            is com.example.sip.VerimorSipState.Ringing -> "🔔 Çalıyor"
                                            is com.example.sip.VerimorSipState.InCall -> "📞 Görüşmede"
                                            is com.example.sip.VerimorSipState.Error -> "Hata"
                                            else -> "Bağlı Değil"
                                        }
                                        val statusBg = when (verimorState) {
                                            is com.example.sip.VerimorSipState.Registered -> Color(0xFF0F2942)
                                            is com.example.sip.VerimorSipState.Connecting -> Color(0xFF33200A)
                                            is com.example.sip.VerimorSipState.Ringing, is com.example.sip.VerimorSipState.InCall -> Color(0xFF0F2942)
                                            is com.example.sip.VerimorSipState.Error -> Color(0xFF3B1214)
                                            else -> iOSCardTertiary
                                        }
                                        val statusFg = when (verimorState) {
                                            is com.example.sip.VerimorSipState.Registered, is com.example.sip.VerimorSipState.Ringing, is com.example.sip.VerimorSipState.InCall -> iOSBlue
                                            is com.example.sip.VerimorSipState.Connecting -> iOSOrange
                                            is com.example.sip.VerimorSipState.Error -> iOSRed
                                            else -> iOSTextSecondary
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
                                        text = "Verimor Bulut Santral dahili bilgilerinizi giriniz. Cihaz doğrudan dahili SIP motoruyla santrale register olur; çağrı çaldığı an arayan numara yakalanır ve kronometre otomatik başlatılır.",
                                        fontSize = 11.sp,
                                        color = iOSTextSecondary
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Santral Host
                                    OutlinedTextField(
                                        value = verimorDomain,
                                        onValueChange = { viewModel.updateVerimorDomain(it) },
                                        label = { Text("Santral Domain / Host Adı", fontSize = 11.sp) },
                                        placeholder = { Text("sirketiniz.bulutsantralim.com", fontSize = 11.sp) },
                                        leadingIcon = { Icon(Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = formTextFieldColors()
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Dahili & Port
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = verimorExtension,
                                            onValueChange = { viewModel.updateVerimorExtension(it) },
                                            label = { Text("Dahili (Extension)", fontSize = 11.sp) },
                                            placeholder = { Text("101", fontSize = 11.sp) },
                                            leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.weight(1.3f),
                                            colors = formTextFieldColors()
                                        )

                                        OutlinedTextField(
                                            value = verimorPort,
                                            onValueChange = { viewModel.updateVerimorPort(it) },
                                            label = { Text("Port", fontSize = 11.sp) },
                                            placeholder = { Text("5060", fontSize = 11.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.weight(0.9f),
                                            colors = formTextFieldColors()
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Şifre
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
                                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = formTextFieldColors()
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Otomasyon Anahtarları
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
                                                    Text("Çağrı çaldığı an kronometre (00:00) otomatik başlatılır.", fontSize = 10.sp, color = Color.Gray)
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
                                                    Text("Arayan numara SQLite'ta aranır; ASLA otomatik doldurmaz, onay penceresi açar.", fontSize = 10.sp, color = Color.Gray)
                                                }
                                                Switch(
                                                    checked = verimorAutoSearch,
                                                    onCheckedChange = { viewModel.toggleVerimorAutoSearch() }
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Bağlan & Bağlantıyı Kes Butonları
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

                                    // Kalıcı Bağlantı Durumu Bilgisi
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
                                                    "Kalıcı Bağlantı Pasif: Bağlantı kesildi. Otomatik bağlanmak için 'Santrale Kayıt Ol' butonuna basınız.",
                                                fontSize = 10.sp,
                                                color = if (verimorAutoConnect) ExcelGreenDark else Slate700,
                                                lineHeight = 13.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        color = Color(0xFFEFF6FF),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Column {
                                                    Text("Arka Plan Bağlantı Koruması", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF1E40AF))
                                                    Text("Uygulama arka plandayken çağrıların düşmemesi için pil kısıtlamasını kaldırın.", fontSize = 9.5.sp, color = Slate700)
                                                }
                                            }
                                            TextButton(
                                                onClick = { com.example.sip.VerimorSipService.requestIgnoreBatteryOptimizations(context) },
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                            ) {
                                                Text("İzin Ver", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                                            }
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

                                    // CANLI TEST SİMÜLATÖRÜ
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
                                                text = "Santral bağlantısı olmadan da çağrı yakalama, kronometre başlatma ve onay diyaloglarını test edebilirsiniz.",
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
                                                    modifier = Modifier.weight(1f),
                                                    colors = formTextFieldColors()
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Button(
                                                    onClick = { viewModel.simulateVerimorIncomingCall(testPhoneNumber) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Text("Çağrıyı Tetikle", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "İpucu: '05446007080' çoklu şube seçim listesi açar. '05422003040' tek kayıt onay penceresi açar. Kural gereği kullanıcı onayı olmadan ASLA otomatik doldurma yapılmaz.",
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

                    1 -> {
                        // TAB 6: VERİ YEDEKLEME VE GERİ YÜKLEME (DATA BACKUP & RESTORE)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF2563EB).copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Backup, contentDescription = null, tint = Color(0xFF1D4ED8), modifier = Modifier.size(22.dp))
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "Veri Yedekleme ve Geri Yükleme",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Slate900
                                            )
                                            Text(
                                                text = "Tüm SQLite Tabloları & SIP Ayarları JSON Transferi",
                                                fontSize = 11.sp,
                                                color = Slate700
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Telefon sıfırlandığında veya yeni bir telefona geçtiğinizde verilerinizin kaybolmaması için tüm veritabanını ve ayarlarınızı tek tıkla yedekleyin veya geri yükleyin.",
                                        fontSize = 11.5.sp,
                                        color = Slate700,
                                        lineHeight = 15.sp
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Durum Özeti
                                    Surface(
                                        color = Color.White,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Mevcut Çağrı Kaydı:", fontSize = 11.sp, color = Slate700)
                                                Text("${allRecords.size} Adet", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ExcelGreenDark)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Sabit Şablonlar (Tespit/İşlem/Konu):", fontSize = 11.sp, color = Slate700)
                                                Text("${tespitTemplates.size + islemTemplates.size + konuTemplates.size} Adet", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ExcelGreenDark)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Kayıtlı SIP Dahili:", fontSize = 11.sp, color = Slate700)
                                                Text(
                                                    if (verimorExtension.isNotBlank()) "$verimorExtension @ $verimorDomain" else "(Ayarlanmamış)",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (verimorExtension.isNotBlank()) Color(0xFF2563EB) else Slate700
                                                )
                                            }
                                            if (lastBackupInfo != null) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("Son Alınan Yedek:", fontSize = 11.sp, color = Slate700)
                                                    Text(lastBackupInfo ?: "", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F766E))
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // 0. DEPOLAMA & HAFIZA ANALİZİ (TOPLAM BOYUT & TEMİZLİK RAPORU)
                            Card(
                                modifier = Modifier.fillMaxWidth().testTag("storage_analysis_card"),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF3B82F6))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFF2563EB)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Storage, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "Depolama & Hafıza Analizi",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.5.sp,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = "Uygulamanın tuttuğu toplam veri ve temizlik potansiyeli",
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF94A3B8)
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = { viewModel.refreshStorageBreakdown(context) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Yenile", tint = Color(0xFF60A5FA), modifier = Modifier.size(18.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Total vs Cleanable Summary Row
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        // Toplam Boyut
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF0F172A),
                                            border = BorderStroke(0.8.dp, Color(0xFF334155)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text("TOPLAM UYGULAMA VERİSİ", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                                                Spacer(modifier = Modifier.height(3.dp))
                                                Text(
                                                    text = viewModel.formatBytes(storageBreakdown.totalAppSizeBytes),
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFF60A5FA)
                                                )
                                                Text(
                                                    text = "${storageBreakdown.dbRecordCount} Yerel Destek Kaydı",
                                                    fontSize = 9.5.sp,
                                                    color = Color(0xFFCBD5E1)
                                                )
                                            }
                                        }

                                        // Temizlik Yapılırsa Açılacak Alan
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF064E3B),
                                            border = BorderStroke(0.8.dp, Color(0xFF059669)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text("KAZANILABİLİR ALAN", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA7F3D0))
                                                Spacer(modifier = Modifier.height(3.dp))
                                                Text(
                                                    text = viewModel.formatBytes(storageBreakdown.estimatedTotalCleanableBytes),
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFF34D399)
                                                )
                                                Text(
                                                    text = "Arka ofise dokunulmadan",
                                                    fontSize = 9.5.sp,
                                                    color = Color(0xFFA7F3D0)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Detaylı Döküm Tablosu
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF0F172A),
                                        border = BorderStroke(0.6.dp, Color(0xFF334155)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            StorageBreakdownRow(
                                                label = "Destek Kayıtları (SQLite DB)",
                                                detail = "${storageBreakdown.dbRecordCount} kayıt (${storageBreakdown.syncedCount} SQL aktarıldı, ${storageBreakdown.unsyncedCount} beklemede)",
                                                sizeStr = viewModel.formatBytes(storageBreakdown.dbSizeBytes),
                                                badge = if (storageBreakdown.syncedCount > 0) "Yer Açılabilir" else null,
                                                badgeColor = Color(0xFF10B981)
                                            )
                                            Divider(color = Color(0xFF1E293B), thickness = 0.5.dp)
                                            StorageBreakdownRow(
                                                label = "Güvenlik Yedekleri (JSON)",
                                                detail = "${storageBreakdown.autoBackupCount} adet yerel otomatik yedek kopyası",
                                                sizeStr = viewModel.formatBytes(storageBreakdown.autoBackupSizeBytes),
                                                badge = null,
                                                badgeColor = Color.Transparent
                                            )
                                            Divider(color = Color(0xFF1E293B), thickness = 0.5.dp)
                                            StorageBreakdownRow(
                                                label = "Önbellek & Geçici Raporlar",
                                                detail = "Geçici Excel paylaşım ve çıktı dosyaları",
                                                sizeStr = viewModel.formatBytes(storageBreakdown.cacheSizeBytes),
                                                badge = "Hemen Silinebilir",
                                                badgeColor = Color(0xFF3B82F6)
                                            )
                                            Divider(color = Color(0xFF1E293B), thickness = 0.5.dp)
                                            StorageBreakdownRow(
                                                label = "Ayarlar & Şablonlar",
                                                detail = "SIP ve tespit şablon konfigürasyonu",
                                                sizeStr = viewModel.formatBytes(storageBreakdown.preferencesSizeBytes),
                                                badge = null,
                                                badgeColor = Color.Transparent
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Bilgilendirme Notu
                                    Text(
                                        text = "💡 Bilgi: Temizlik yapıldığında sadece telefonunuzdaki önbellek ve SQL Server'a önceden başarıyla aktarılmış kopyalar temizlenir. Arka Ofis / SQL Server'daki ana kayıtlarınıza KESİNLİKLE dokunulmaz.",
                                        fontSize = 10.sp,
                                        color = Color(0xFF94A3B8),
                                        lineHeight = 14.sp
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Buton: Hafızayı Temizle Menüsünü Aç
                                    Button(
                                        onClick = { showLocalMemoryCleanDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("button_open_clean_storage")
                                    ) {
                                        Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Hafızayı Temizle (Yer Aç)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // 1. EXPORT (YEDEK AL) - İSTENEN BUTON 1
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = ExcelGreenDark, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "1. Export (Yedek Al)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = ExcelGreenDark
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "SQLite veritabanındaki tüm çağrı kayıtları, sabit tespitler, sabit işlemler, konular ve Verimor SIP bağlantı ayarları tek bir JSON dosyasında toplanır.",
                                        fontSize = 11.sp,
                                        color = Slate700
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Buton: "Yedek Al & Paylaş (.json)"
                                    Button(
                                        onClick = { viewModel.exportDataBackup(context, shareImmediately = true) },
                                        enabled = !isBackingUp,
                                        colors = ButtonDefaults.buttonColors(containerColor = ExcelGreen),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("button_export_backup_share")
                                    ) {
                                        if (isBackingUp) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Yedek Hazırlanıyor...", fontSize = 12.sp)
                                        } else {
                                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Yedek Al & Paylaş (.json)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Buton: "Cihazın İndirilenler (Downloads) Klasörüne Kaydet"
                                    OutlinedButton(
                                        onClick = { viewModel.exportDataBackup(context, shareImmediately = false) },
                                        enabled = !isBackingUp,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("button_export_backup_download")
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp), tint = ExcelGreenDark)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Cihazın İndirilenler (Downloads) Klasörüne Kaydet", fontSize = 11.5.sp, color = ExcelGreenDark)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // 2. IMPORT (GERİ YÜKLE) - İSTENEN BUTON 2
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color(0xFF1E40AF), modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "2. Import (Geri Yükle)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFF1E40AF)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Yeni telefonda uygulamayı kurduğunuzda, daha önce aldığınız .json yedek dosyasını seçerek tüm veritabanınızı ve ayarlarınızı tek hamlede geri yükleyin.",
                                        fontSize = 11.sp,
                                        color = Slate700
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Buton: "Yedek Dosyası Seç ve Geri Yükle (Import)"
                                    Button(
                                        onClick = {
                                            try {
                                                backupPickerLauncher.launch("*/*")
                                            } catch (e: Exception) {
                                                try {
                                                    backupPickerLauncher.launch("application/json")
                                                } catch (_: Exception) {}
                                            }
                                        },
                                        enabled = !isRestoring,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("button_import_backup_select")
                                    ) {
                                        if (isRestoring) {
                                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Geri Yükleniyor...", fontSize = 12.sp)
                                        } else {
                                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Yedek Dosyası Seç ve Geri Yükle (Import)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Güvenlik Kuralı: Dosya seçildiğinde öncelikle içerik doğrulanır, kayıt sayıları gösterilir ve onayınız olmadan yükleme yapılmaz.",
                                        fontSize = 10.sp,
                                        color = Color(0xFF1D4ED8),
                                        fontStyle = FontStyle.Italic
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // 3. YEREL HAFIZAYI TEMİZLE (ARKA OFİSE DOKUNULMAZ)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CleaningServices, contentDescription = null, tint = Color(0xFF047857), modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "3. Telefon Hafızasını Temizle (Arka Ofis Korunur)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFF065F46)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "SQL Server'dan veya harici cihazlardan çekilen çağrıları yerel telefon hafızasından temizleyerek hafıza şişmesini önleyin. Arka Ofis / SQL Server ana veritabanındaki kayıtlarınıza KESİNLİKLE DOKUNULMAZ.",
                                        fontSize = 11.sp,
                                        color = Color(0xFF047857),
                                        lineHeight = 15.sp
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = { showLocalMemoryCleanDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("button_clean_local_memory")
                                    ) {
                                        Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Hafıza & Önbellek Temizleme Menüsünü Aç", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(280.dp))
                        }
                    }

                    2 -> {
                        // Sabit Tespit Şablonları Listesi & Ekleme
                        Text(
                            text = "Programda TESPİT-1 yazarken baş harflerle otomatik eşleşecek sabit cümleler:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newTemplateText,
                                onValueChange = { newTemplateText = it },
                                placeholder = { Text("Yeni tespit cümlesi yazın...", fontSize = 11.sp) },
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

                    3 -> {
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
                                placeholder = { Text("Yeni işlem / çözüm cümlesi yazın...", fontSize = 11.sp) },
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

                    4 -> {
                        // Sabit Destek Konuları Şablonları Listesi & Ekleme
                        Text(
                            text = "Programda DESTEK KONUSU yazarken baş harflerle otomatik eşleşecek konular:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newTemplateText,
                                onValueChange = { newTemplateText = it },
                                placeholder = { Text("Yeni destek konusu yazın...", fontSize = 11.sp) },
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

                    5 -> {
                        // Kullanıcı (Temsilci) Ayarları, Temsilci Yönetimi & Tarihe Göre Silme
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            // 1. AKTİF TEMSİLCİ BİLGİSİ
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = ExcelGreenLight.copy(alpha = 0.45f)),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, ExcelGreen.copy(alpha = 0.35f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Badge, contentDescription = null, tint = ExcelGreen, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Aktif Kullanıcı (Temsilci) & ID Kodu",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = ExcelGreenDark
                                            )
                                        }

                                        if (globalKullanici.isNotBlank() || globalKullaniciKodu.isNotBlank()) {
                                            Surface(
                                                color = ExcelGreen,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "✓ Aktif",
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Seçilen aktif temsilci form kayıtlarında, yeni çağrılarda ve veritabanı senkronizasyonunda otomatik kullanılır.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = globalKullanici,
                                            onValueChange = { viewModel.updateGlobalKullanici(it) },
                                            label = { Text("Kullanıcı Adı Soyadı") },
                                            placeholder = { Text("Örn: Ali Veli") },
                                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                            singleLine = true,
                                            modifier = Modifier.weight(1.3f).testTag("input_global_kullanici"),
                                            colors = formTextFieldColors()
                                        )

                                        OutlinedTextField(
                                            value = globalKullaniciKodu,
                                            onValueChange = { viewModel.updateGlobalKullaniciKodu(it) },
                                            label = { Text("ID / Kodu") },
                                            placeholder = { Text("101") },
                                            leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                            singleLine = true,
                                            modifier = Modifier.weight(0.9f).testTag("input_global_kullanici_kodu"),
                                            colors = formTextFieldColors()
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // 2. OUTLOOK BİLDİRİM E-POSTA ALICISI (SERVİS ADRESİ)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Email, contentDescription = null, tint = iOSBlue, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Outlook E-Posta Alıcısı (Servis Bildirimi)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Aramalar sekmesinde Outlook taslağı oluşturulduğunda alıcı (Kime) alanına otomatik eklenecek e-posta adresidir.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = outlookRecipientEmail,
                                            onValueChange = { viewModel.updateOutlookRecipientEmail(it) },
                                            label = { Text("Outlook Alıcı E-Postası") },
                                            placeholder = { Text("servis@enpos.com.tr") },
                                            leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f).testTag("input_outlook_recipient_email"),
                                            colors = formTextFieldColors()
                                        )

                                        if (outlookRecipientEmail != "servis@enpos.com.tr") {
                                            TextButton(
                                                onClick = { viewModel.updateOutlookRecipientEmail("servis@enpos.com.tr") },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                                modifier = Modifier.height(40.dp)
                                            ) {
                                                Text("Varsayılan", fontSize = 11.sp, color = iOSBlue, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // 3. TEMSİLCİ YÖNETİMİ (YENİ EKLE, LİSTEDEN SEÇ, DÜZENLE, SİL)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Group, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Temsilci Listesi & Yönetimi",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Temsilcilerinizi ekleyip düzenleyebilir ve aralarından dilediğinizi varsayılan aktif temsilci seçebilirsiniz.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Yeni Temsilci Ekleme Formu
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = "Yeni Temsilci Ekle:",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OutlinedTextField(
                                                    value = newRepName,
                                                    onValueChange = { newRepName = it },
                                                    placeholder = { Text("Temsilci Adı Soyadı", fontSize = 11.sp) },
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1.3f),
                                                    colors = formTextFieldColors()
                                                )
                                                OutlinedTextField(
                                                    value = newRepCode,
                                                    onValueChange = { newRepCode = it },
                                                    placeholder = { Text("ID / Kodu", fontSize = 11.sp) },
                                                    singleLine = true,
                                                    modifier = Modifier.weight(0.9f),
                                                    colors = formTextFieldColors()
                                                )
                                                Button(
                                                    onClick = {
                                                        viewModel.addRepresentative(newRepName, newRepCode)
                                                        newRepName = ""
                                                        newRepCode = ""
                                                    },
                                                    enabled = newRepName.isNotBlank() || newRepCode.isNotBlank(),
                                                    colors = ButtonDefaults.buttonColors(containerColor = ExcelGreen),
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text("Ekle", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Kayıtlı Temsilciler Listesi
                                    if (representatives.isEmpty()) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.surface,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "Kayıtlı temsilci bulunmuyor. Yukarıdaki formdan yeni temsilci ekleyebilirsiniz.",
                                                fontSize = 11.sp,
                                                color = Color.Gray,
                                                modifier = Modifier.padding(12.dp)
                                            )
                                        }
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            representatives.forEach { preset ->
                                                val isSelected = (globalKullaniciKodu == preset.code && preset.code.isNotBlank()) ||
                                                        (globalKullanici == preset.name && globalKullaniciKodu.isBlank())

                                                Surface(
                                                    color = if (isSelected) ExcelGreenLight.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
                                                    shape = RoundedCornerShape(8.dp),
                                                    border = BorderStroke(
                                                        1.dp,
                                                        if (isSelected) ExcelGreenDark else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                                    ),
                                                    modifier = Modifier.fillMaxWidth()
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
                                                                color = if (isSelected) ExcelGreen else MaterialTheme.colorScheme.surfaceVariant,
                                                                shape = RoundedCornerShape(6.dp)
                                                            ) {
                                                                Text(
                                                                    text = preset.code.ifBlank { "-" },
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 11.sp,
                                                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text(
                                                                text = preset.name.ifBlank { "İsimsiz Temsilci" },
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                                fontSize = 12.5.sp,
                                                                color = if (isSelected) ExcelGreenDark else MaterialTheme.colorScheme.onSurface
                                                            )
                                                            if (isSelected) {
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                Surface(
                                                                    color = ExcelGreen,
                                                                    shape = RoundedCornerShape(4.dp)
                                                                ) {
                                                                    Text(
                                                                        text = "Varsayılan",
                                                                        fontSize = 9.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = Color.White,
                                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                                    )
                                                                }
                                                            }
                                                        }

                                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                            if (!isSelected) {
                                                                TextButton(
                                                                    onClick = { viewModel.selectActiveRepresentative(preset) },
                                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                                                    modifier = Modifier.height(28.dp)
                                                                ) {
                                                                    Text("Aktif Seç", fontSize = 11.sp, color = ExcelGreenDark, fontWeight = FontWeight.Bold)
                                                                }
                                                            }

                                                            IconButton(
                                                                onClick = {
                                                                    editingRep = preset
                                                                    editRepName = preset.name
                                                                    editRepCode = preset.code
                                                                },
                                                                modifier = Modifier.size(28.dp)
                                                            ) {
                                                                Icon(Icons.Default.Edit, contentDescription = "Düzenle", modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                                                            }

                                                            IconButton(
                                                                onClick = { repToDelete = preset },
                                                                modifier = Modifier.size(28.dp)
                                                            ) {
                                                                Icon(Icons.Default.Delete, contentDescription = "Sil", modifier = Modifier.size(15.dp), tint = AccentRed)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Tarihe Göre Silme
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

                            Spacer(modifier = Modifier.height(14.dp))

                            // Ekran Görünüm Ayarları
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
                                            Text("Gizlenen Alanları 2. Sayfada Göster", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("Ana ekranda 2. sekme olarak sunulur (İletişim türü, Bekleyen çağrı, Redmine No, Bağlantı ID vb.).", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

                    6 -> {
                        // SQL Sync Ayarları
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
                                        Text("Merkezi SQL Senkronizasyonu", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1E40AF))
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Veriler cihazda saklanır; istendiğinde merkezi MySQL / PostgreSQL / MSSQL REST API servisine toplu olarak aktarılır.", fontSize = 11.sp, color = Slate700)

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(8.dp)).padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Senkronizasyon Bekleyen Kayıt:", fontSize = 11.sp, color = Color.Gray)
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
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
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

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // IP & Port
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = sqlHost,
                                            onValueChange = { viewModel.updateSqlHost(it) },
                                            label = { Text("Sunucu IP / Host") },
                                            placeholder = { Text("192.168.1.100") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1.5f),
                                            colors = formTextFieldColors()
                                        )
                                        OutlinedTextField(
                                            value = sqlPort,
                                            onValueChange = { viewModel.updateSqlPort(it) },
                                            label = { Text("Port") },
                                            placeholder = { Text("3306") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.weight(0.9f),
                                            colors = formTextFieldColors()
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    OutlinedTextField(
                                        value = sqlDbName,
                                        onValueChange = { viewModel.updateSqlDbName(it) },
                                        label = { Text("Veritabanı Adı") },
                                        placeholder = { Text("destek_db") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = formTextFieldColors()
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = sqlUser,
                                            onValueChange = { viewModel.updateSqlUser(it) },
                                            label = { Text("Kullanıcı") },
                                            placeholder = { Text("root") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            colors = formTextFieldColors()
                                        )
                                        OutlinedTextField(
                                            value = sqlPassword,
                                            onValueChange = { viewModel.updateSqlPassword(it) },
                                            label = { Text("Şifre") },
                                            placeholder = { Text("••••••") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            colors = formTextFieldColors()
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = { viewModel.saveDbSettings() },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Ayarları Kaydet", fontSize = 11.sp)
                                        }
                                        Button(
                                            onClick = { viewModel.testDbConnection() },
                                            enabled = !isTestingConnection,
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            if (isTestingConnection) {
                                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Test...", fontSize = 11.sp)
                                            } else {
                                                Text("Bağlantıyı Test Et", fontSize = 11.sp)
                                            }
                                        }
                                    }

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
                        }
                    }
                    7 -> {
                        // TAB 7: SİCİL NO ÖN EK (PREFIX) YÖNETİMİ
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Tag, contentDescription = null, tint = ExcelGreen, modifier = Modifier.size(22.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Sicil No Başlık / Ön Ek (Prefix) Yönetimi",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Burada tanımlanan ön ekler (Örn: PC, PE, SRV, TECH) destek formu ana ekranındaki açılır menüde listelenir. Kullanıcı sadece rakamları girer, ön ek otomatik eklenir.",
                                        fontSize = 11.sp,
                                        color = Slate700
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // YENİ ÖN EK EKLEME
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = newPrefixText,
                                            onValueChange = { newPrefixText = it.uppercase() },
                                            label = { Text("Yeni Ön Ek (Örn: PC, PE)", fontSize = 11.sp) },
                                            placeholder = { Text("Örn: KASA", fontSize = 11.sp) },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            colors = formTextFieldColors()
                                        )

                                        Button(
                                            onClick = {
                                                if (newPrefixText.isNotBlank()) {
                                                    viewModel.addPrefix(newPrefixText)
                                                    newPrefixText = ""
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = ExcelGreen),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Ekle", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Text(
                                        text = "Kayıtlı Sicil Ön Ekleri (${prefixTemplates.size}):",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = ExcelGreenDark
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    if (prefixTemplates.isEmpty()) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFFEF3C7),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "Henüz ön ek tanımlanmamış. Varsayılan PC, PE, SRV sistemce sunulur.",
                                                fontSize = 11.sp,
                                                color = Color(0xFF92400E),
                                                modifier = Modifier.padding(10.dp)
                                            )
                                        }
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            prefixTemplates.forEach { pfx ->
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color.White,
                                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = ExcelGreenLight,
                                                                modifier = Modifier.padding(end = 8.dp)
                                                            ) {
                                                                Text(
                                                                    text = pfx.text,
                                                                    fontWeight = FontWeight.ExtraBold,
                                                                    fontSize = 13.sp,
                                                                    color = ExcelGreenDark,
                                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                            Text(
                                                                text = "Örnek: ${pfx.text}-00123",
                                                                fontSize = 11.sp,
                                                                color = Color.Gray
                                                            )
                                                        }

                                                        IconButton(
                                                            onClick = { viewModel.deletePrefix(pfx.id) },
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Delete,
                                                                contentDescription = "Sil",
                                                                tint = AccentRed,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    8 -> {
                        // TAB 8: SÜRÜM GEÇMİŞİ VE YENİLİKLER (CHANGELOG)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Güncel Sürüm Başlık Kartı
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Telefon Destek",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "Verimor SIP, SQL Sync, Raporlama ve Akıllı Çağrı Yönetimi",
                                                fontSize = 11.sp,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFF10B981)
                                        ) {
                                            Text(
                                                text = "v2.6.5 (En Yeni)",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // SÜRÜM 2.6.5 (YENİ EKLENEN ÖZELLİKLER)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.2.dp, Color(0xFF22C55E))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = "🔥 Sürüm 2.6.5 (En Yeni Özellikler)", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = ExcelGreenDark)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(color = Color(0xFF16A34A), shape = RoundedCornerShape(4.dp)) {
                                            Text("YENİ", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val v265Items = listOf(
                                        "🎙️ Sesle Form Doldurma (Sesli Dikte): Tespit ve İşlem kutularının yanında küçük mikrofon simgeleri. Temsilci basıp konuştuğunda sesi anında temiz Türkçe metin olarak kutuya yazar; yoğun günlerde form doldurma süresini 10 saniyenin altına indirir.",
                                        "🌙 Gün Sonu / Vardiya Özeti (WhatsApp & Mail Raporu): Üst menüden, istatistik panelinden veya geçmişten tek tıkla açılan 'Gün Sonu' ekranı. Günün toplam çağrı sayısı, toplam görüşme süresi, ortalama süre, çözüm oranı ve en sık karşılaşılan kronik arızaları şık bir metin olarak WhatsApp veya E-Posta ile amirinize/grubunuza tek tıkla iletir.",
                                        "📚 Adım Adım Arıza / Hata Kodu Rehberi (Bilgi Bankası): Üst menüdeki 'Rehber' butonuyla açılan hızlı arama ekranı. Hata kodu (Hata 304, E-Arşiv 500, Kutter 24 vb.) yazıldığında adım adım çözüm maddelerini gösterir, tek dokunuşla hem forma aktarabilir hem de müşteriye WhatsApp üzerinden talimat olarak atabilirsiniz."
                                    )
                                    v265Items.forEach { item ->
                                        Text(text = item, fontSize = 11.sp, color = Slate900, lineHeight = 16.sp, modifier = Modifier.padding(vertical = 3.dp))
                                    }
                                }
                            }

                            // SÜRÜM 2.6.4
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(text = "📊 Sürüm 2.6.4: İstatistik Paneli & Hızlı Çözüm Şablonları", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate800)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val v26Items = listOf(
                                        "• 📊 Günlük / Haftalık Performans & İstatistik Paneli: Toplam çağrı, ortalama süre, % çözüm ve en sık karşılaşılan sorun grafikleri.",
                                        "• ⚡ Kişiselleştirilebilir Hızlı Şablonlar: Ayarlar > 3. Hızlı Çözüm Şablonları sekmesinden yeni senaryo tanımlama ve özelleştirme.",
                                        "• 💬 WhatsApp Servis Bilgi Fişi: Form üzerinden tek tıkla müşteriye servis bilgi mesajı gönderme.",
                                        "• 📝 Canlı Görüşme Karalama Notu (Scratchpad): Hızlı notları tek tıkla tespite/işleme aktarma."
                                    )
                                    v26Items.forEach { item ->
                                        Text(text = item, fontSize = 10.5.sp, color = Slate800, lineHeight = 15.sp, modifier = Modifier.padding(vertical = 1.5.dp))
                                    }
                                }
                            }

                            // SÜRÜM 2.0 ÖZETİ
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(text = "🌐 Sürüm 2.0: SQL Sync & Canlı Çağrı Takibi", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate800)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val v20Items = listOf(
                                        "• Kurumsal MSSQL ve PostgreSQL veritabanı iki yönlü senkronizasyonu.",
                                        "• Canlı çağrı kontrolü ve santral aktarma (*2202 DTMF) desteği.",
                                        "• Gelişmiş veri yedekleme, dışa/içe aktarma (.json) motoru."
                                    )
                                    v20Items.forEach { item ->
                                        Text(text = item, fontSize = 10.5.sp, color = Slate700, modifier = Modifier.padding(vertical = 1.dp))
                                    }
                                }
                            }

                            // SÜRÜM 1.2 YENİLİKLERİ
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(text = "🚀 Sürüm 1.2: Arama Geliştirmeleri & Şablonlar", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate800)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val v12Items = listOf(
                                        "• Hazır Demo DB Entegrasyonu ve ilk kurulum şablonları.",
                                        "• Sicil No Ön Ek (Prefix) & Sayısal klavye optimizasyonu.",
                                        "• Akıllı arama debounce gecikmesi ve SQL UPSERT mükerrer kayıt önleme.",
                                        "• Harf bazlı süzülen açılır tespit ve işlem menüleri."
                                    )
                                    v12Items.forEach { item ->
                                        Text(text = item, fontSize = 10.5.sp, color = Slate700, modifier = Modifier.padding(vertical = 1.dp))
                                    }
                                }
                            }

                            // SÜRÜM 1.1 GEÇMİŞİ
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(text = "📋 Sürüm 1.1: SIP 200 OK & Güvenlik", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate800)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val v11Items = listOf(
                                        "• Verimor SIP 200 OK ses motoru ve arka plan Doze/Keep-Alive koruması.",
                                        "• Asla otomatik doldurma güvenlik kuralı ve müşteri onay dialoğu.",
                                        "• Tarih aralıklı Excel aktarımı ve JSON yedekleme/geri yükleme."
                                    )
                                    v11Items.forEach { item ->
                                        Text(text = item, fontSize = 10.5.sp, color = Slate700, modifier = Modifier.padding(vertical = 1.dp))
                                    }
                                }
                            }

                            // SÜRÜM 1.0 GEÇMİŞİ
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(text = "📦 Sürüm 1.0: Temel Çıkış", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Slate800)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "• İlk sürüm: Çağrı form kayıtları, Room SQLite yerel veritabanı, kronometre ve raporlama.", fontSize = 10.5.sp, color = Slate700)
                                }
                            }
                        }
                    }

                    9 -> {
                        // TAB 9: HIZLI ÇÖZÜM ŞABLONLARI & SENARYOLAR (KİŞİSELLEŞTİRME)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Bilgilendirme Kartı
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = iOSCardSecondary),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(0.6.dp, iOSBlue.copy(alpha = 0.6f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(iOSBlue.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Bolt, contentDescription = null, tint = iOSBlue, modifier = Modifier.size(16.dp))
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Hızlı Çözüm Şablonları (Senaryolar)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                            color = iOSTextPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Çağrı formunda 'Hızlı Şablonlar' satırından tek dokunuşla Destek Konusu, Tespit ve İşlem alanlarını otomatik dolduran hazır senaryolardır. Sık karşılaştığınız teknik konuları buradan kendinize özel olarak ekleyebilir, değiştirebilir veya silebilirsiniz.",
                                        fontSize = 11.sp,
                                        color = iOSTextSecondary,
                                        lineHeight = 15.sp
                                    )
                                }
                            }

                            // Aksiyon Butonları (Yeni Ekle & Sıfırla)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        editingScenario = null
                                        scenarioTitle = ""
                                        scenarioIcon = "⚡"
                                        scenarioKonu = ""
                                        scenarioTespit = ""
                                        scenarioIslem = ""
                                        isScenarioDialogOpen = true
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = ExcelGreen),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Yeni Şablon Ekle", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = { showResetScenariosConfirm = true },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = iOSOrange),
                                    border = BorderStroke(0.8.dp, iOSOrange.copy(alpha = 0.6f))
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Varsayılana Dön", fontSize = 11.sp)
                                }
                            }

                            Text(
                                text = "Kayıtlı Şablonlar (${quickScenarios.size} Adet):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = iOSTextPrimary
                            )

                            // Şablon Kartları Listesi
                            quickScenarios.forEach { scenario ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = iOSCardSecondary),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(0.5.dp, iOSBorder)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Text(text = scenario.icon, fontSize = 20.sp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = scenario.title,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = iOSTextPrimary
                                                )
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = {
                                                        editingScenario = scenario
                                                        scenarioTitle = scenario.title
                                                        scenarioIcon = scenario.icon
                                                        scenarioKonu = scenario.konu
                                                        scenarioTespit = scenario.tespit
                                                        scenarioIslem = scenario.islem
                                                        isScenarioDialogOpen = true
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Düzenle", tint = iOSBlue, modifier = Modifier.size(16.dp))
                                                }
                                                IconButton(
                                                    onClick = { scenarioToDelete = scenario },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Sil", tint = AccentRed, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        if (scenario.konu.isNotBlank()) {
                                            Surface(
                                                color = iOSTeal.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(4.dp),
                                                border = BorderStroke(0.4.dp, iOSTeal.copy(alpha = 0.5f))
                                            ) {
                                                Text(
                                                    text = "Konu: ${scenario.konu}",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = iOSTeal,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                        }

                                        if (scenario.tespit.isNotBlank()) {
                                            Text(
                                                text = "Tespit: ${scenario.tespit}",
                                                fontSize = 11.sp,
                                                color = iOSTextSecondary,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                        }

                                        if (scenario.islem.isNotBlank()) {
                                            Text(
                                                text = "İşlem: ${scenario.islem}",
                                                fontSize = 11.sp,
                                                color = ExcelGreen,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { selectedSettingsTab = -1 }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(14.dp), tint = iOSBlue)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tüm Ayarlar", color = iOSBlue, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Kapat", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

    // Geri Yükleme Öncesi Onay ve Önizleme Dialogu
    val preview = backupPreview
    if (preview != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissBackupPreview() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ExcelGreen, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Yedek Dosyası Doğrulandı", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Seçilen yedek paketinin detayları aşağıdadır. Geri yüklemek istiyor musunuz?",
                        fontSize = 12.sp
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("• Yedek Tarihi: ${preview.backupDate.take(19)}", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                            Text("• Çağrı Kayıtları: ${preview.totalRecords} adet", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = ExcelGreenDark)
                            Text("• Sabit Şablonlar: ${preview.totalTemplates} adet", fontSize = 11.5.sp)
                            val sipSummary = if (preview.sipExtension.isNotBlank()) "${preview.sipExtension} @ ${preview.sipDomain}" else "(Yok)"
                            Text("• SIP Yapılandırması: $sipSummary", fontSize = 11.5.sp, color = Color(0xFF2563EB))
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { restoreClearExisting = !restoreClearExisting }
                    ) {
                        Checkbox(
                            checked = restoreClearExisting,
                            onCheckedChange = { restoreClearExisting = it }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Mevcut yerel verileri tamamen temizleyip yerine bu yedeği kur (Temiz Kurulum)",
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmAndRestoreBackup(context, restoreClearExisting) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("Evet, Geri Yükle", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.dismissBackupPreview() }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    // Silme Onay Dialogu
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = AccentRed, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Kayıtları Silmeyi Onaylayın", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "$deleteStart ile $deleteEnd arasındaki tüm kayıtlar kalıcı olarak silinecektir. Bu işlem geri alınamaz!",
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteRecordsByDateRange()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Text("Kalıcı Olarak Sil", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    // Telefon Yerel Hafıza & Önbellek Temizleme Penceresi
    if (showLocalMemoryCleanDialog) {
        LocalMemoryCleanDialog(
            viewModel = viewModel,
            allRecordsCount = allRecords.size,
            onDismiss = { showLocalMemoryCleanDialog = false }
        )
    }

    // Temsilci Düzenleme Dialogu
    editingRep?.let { targetRep ->
        AlertDialog(
            onDismissRequest = { editingRep = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = iOSBlue, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Temsilciyi Düzenle", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editRepName,
                        onValueChange = { editRepName = it },
                        label = { Text("Temsilci Adı Soyadı") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = formTextFieldColors()
                    )
                    OutlinedTextField(
                        value = editRepCode,
                        onValueChange = { editRepCode = it },
                        label = { Text("Temsilci ID / Kodu") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = formTextFieldColors()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateRepresentative(targetRep.code, editRepName, editRepCode)
                        editingRep = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExcelGreen)
                ) {
                    Text("Kaydet", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { editingRep = null }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    // Temsilci Silme Onay Dialogu
    repToDelete?.let { targetRep ->
        AlertDialog(
            onDismissRequest = { repToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = AccentRed, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Temsilciyi Sil", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "'${targetRep.code} - ${targetRep.name}' temsilcisi listeden silinsin mi?",
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteRepresentative(targetRep.code)
                        repToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Text("Sil", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { repToDelete = null }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    // Hızlı Çözüm Şablonu (Senaryo) Ekleme / Düzenleme Dialogu
    if (isScenarioDialogOpen) {
        val isEditing = editingScenario != null
        val presetEmojis = listOf("⚡", "🧾", "💾", "🌐", "💳", "🖨️", "🔄", "📦", "🔐", "🔧", "📞", "📄", "⚠️", "🛠️")

        AlertDialog(
            onDismissRequest = { isScenarioDialogOpen = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isEditing) Icons.Default.Edit else Icons.Default.AddCircle,
                        contentDescription = null,
                        tint = iOSBlue,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEditing) "Şablonu Düzenle" else "Yeni Hızlı Şablon Tanımla",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Çip üzerinde görünecek emoji ve başlığı seçin, ardından form alanlarına otomatik aktarılacak metinleri girin:",
                        fontSize = 11.sp,
                        color = iOSTextSecondary
                    )

                    // Emoji Seçici
                    Text("İkon / Emoji Seçin:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) {
                        items(presetEmojis) { em ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (scenarioIcon == em) iOSBlue.copy(alpha = 0.25f) else iOSCardTertiary,
                                border = BorderStroke(1.dp, if (scenarioIcon == em) iOSBlue else iOSBorder),
                                modifier = Modifier.clickable { scenarioIcon = em }
                            ) {
                                Text(
                                    text = em,
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = scenarioTitle,
                        onValueChange = { scenarioTitle = it },
                        label = { Text("Şablon Başlığı (Örn: Z Raporu / Gün Sonu)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = formTextFieldColors()
                    )

                    OutlinedTextField(
                        value = scenarioKonu,
                        onValueChange = { scenarioKonu = it },
                        label = { Text("Destek Konusu (Forma yazılacak)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = formTextFieldColors()
                    )

                    OutlinedTextField(
                        value = scenarioTespit,
                        onValueChange = { scenarioTespit = it },
                        label = { Text("Tespit Metni (Forma yazılacak)") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        colors = formTextFieldColors()
                    )

                    OutlinedTextField(
                        value = scenarioIslem,
                        onValueChange = { scenarioIslem = it },
                        label = { Text("Yapılan İşlem / Çözüm Metni (Forma yazılacak)") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        colors = formTextFieldColors()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val title = scenarioTitle.trim()
                        if (title.isNotBlank()) {
                            if (isEditing) {
                                viewModel.updateQuickScenario(
                                    id = editingScenario!!.id,
                                    title = title,
                                    icon = scenarioIcon,
                                    konu = scenarioKonu,
                                    tespit = scenarioTespit,
                                    islem = scenarioIslem
                                )
                            } else {
                                viewModel.addQuickScenario(
                                    title = title,
                                    icon = scenarioIcon,
                                    konu = scenarioKonu,
                                    tespit = scenarioTespit,
                                    islem = scenarioIslem
                                )
                            }
                            isScenarioDialogOpen = false
                        }
                    },
                    enabled = scenarioTitle.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = ExcelGreen)
                ) {
                    Text(if (isEditing) "Güncelle" else "Kaydet", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { isScenarioDialogOpen = false }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    // Hızlı Çözüm Şablonu Silme Onayı Dialogu
    scenarioToDelete?.let { targetScenario ->
        AlertDialog(
            onDismissRequest = { scenarioToDelete = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = AccentRed, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Şablonu Sil", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "'${targetScenario.icon} ${targetScenario.title}' hızlı şablonu silinsin mi?",
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteQuickScenario(targetScenario.id)
                        scenarioToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Text("Sil", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { scenarioToDelete = null }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    // Hızlı Şablonları Varsayılana Sıfırlama Onay Dialogu
    if (showResetScenariosConfirm) {
        AlertDialog(
            onDismissRequest = { showResetScenariosConfirm = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = iOSOrange, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Varsayılan Şablonlara Dön", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "Tüm özel eklediğiniz hızlı şablonlar sıfırlanıp fabrika çıkış varsayılan senaryolar (Z Raporu, Mali Hafıza, Ağ Bağlantısı vb.) geri yüklenecektir. Onaylıyor musunuz?",
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetDefaultQuickScenarios()
                        showResetScenariosConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = iOSOrange)
                ) {
                    Text("Varsayılana Sıfırla", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetScenariosConfirm = false }) {
                    Text("Vazgeç")
                }
            }
        )
    }
}

/**
 * Modern iOS Koyu Kart / Kutucuklu Ayarlar Menüsü Elemanı (Requirement 11)
 */
@Composable
fun SettingsMenuBox(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    iconBg: Color,
    badgeText: String? = null,
    badgeColor: Color = iOSTextSecondary,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = iOSCardSecondary),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(0.5.dp, iOSBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = iOSTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    color = iOSTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (badgeText != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor.copy(alpha = 0.15f),
                    border = BorderStroke(0.5.dp, badgeColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = iOSTextTertiary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun StorageBreakdownRow(
    label: String,
    detail: String,
    sizeStr: String,
    badge: String?,
    badgeColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (badge != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = badgeColor.copy(alpha = 0.2f),
                        border = BorderStroke(0.5.dp, badgeColor)
                    ) {
                        Text(
                            text = badge,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = detail,
                fontSize = 9.5.sp,
                color = Color(0xFF94A3B8)
            )
        }
        Text(
            text = sizeStr,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF60A5FA)
        )
    }
}


