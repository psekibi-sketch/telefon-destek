package com.example.ui

import android.app.Application
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.MainActivity
import com.example.data.SupportDatabase
import com.example.data.SupportRepository
import com.example.export.ExcelExporter
import com.example.model.KnowledgeArticle
import com.example.model.MissedCallRecord
import com.example.model.RepresentativePreset
import com.example.model.SupportRecord
import com.example.network.SyncApiClient
import com.example.network.SearchCustomerRequest
import com.example.network.toSyncCallItem
import com.example.network.toSupportRecord
import com.example.sip.VerimorSipService
import com.example.util.WhatsAppHelper
import android.content.ClipboardManager
import android.content.ClipData
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CustomerMatchCandidate(
    val id: Long = 0,
    val source: String = "LOCAL", // "LOCAL" or "SERVER"
    val musteriBayi: String = "",
    val sicilNo: String = "",
    val gorusulen: String = "",
    val telefon: String = "",
    val telefon2: String = "",
    val mail: String = "",
    val ynokcVersiyon: String = "",
    val dynCashVersiyon: String = "",
    val yn500Versiyon: String = "",
    val arkaOfisVersiyon: String = "",
    val servisKodu: String = "",
    val sonTarih: String = "",
    val sonKonu: String = ""
)

data class CustomerSearchResultState(
    val searchCriterion: String, // "SICIL", "TELEFON", "GORUSULEN", "MANUAL"
    val queryValue: String,
    val candidates: List<CustomerMatchCandidate> = emptyList(),
    val isSearching: Boolean = false
)

data class AutoFillSuggestion(
    val title: String,
    val musteriBayi: String,
    val gorusulen: String,
    val telefon: String,
    val telefon2: String = "",
    val mail: String = "",
    val ynokcVersiyon: String = "",
    val dynCashVersiyon: String = "",
    val yn500Versiyon: String = "",
    val arkaOfisVersiyon: String = "",
    val servisKodu: String = "",
    val matchedField: String // "SICIL" or "TELEFON"
)

data class QuickScenario(
    val id: String,
    val title: String,
    val icon: String,
    val konu: String,
    val tespit: String,
    val islem: String,
    val usageCount: Int = 0
)

data class SupportDailyStats(
    val todayCallsCount: Int = 0,
    val todayTotalDurationSeconds: Long = 0,
    val todayAvgDurationSeconds: Long = 0,
    val todayResolvedCount: Int = 0,
    val todayPendingCount: Int = 0,
    val topTopic: String = "-"
)

data class SupportFormState(
    val editingId: Long? = null,
    val redmine: String = "",
    val basTarihi: String = "",
    val sicilPrefix: String = "PC",
    val sicilDigits: String = "",
    val sicilNo: String = "",
    val kullanici: String = "",
    val kullaniciKodu: String = "",
    val arayan: String = "",
    val musteriBayi: String = "",
    val gorusulen: String = "",
    val telefon: String = "",
    val telefon2: String = "",
    val mail: String = "",
    val iletisimTuru: String = "Telefon",
    val destekKonu: String = "",
    val tespit: String = "",
    val islem: String = "",
    val bagId: String = "",
    val bitTarihi: String = "",
    val sure: String = "",
    val bekleyen: String = "Hayır",
    val b2bKayit: String = "",
    val argeGidis: String = "Hayır",
    val argeGidisNotu: String = "",
    val argeDonus: String = "Hayır",
    val argeDonusNotu: String = "",
    // Device & Hardware Info
    val ynokcVersiyon: String = "",
    val dynCashVersiyon: String = "",
    val yn500Versiyon: String = "",
    val arkaOfisVersiyon: String = "",
    val servisKodu: String = ""
)

class SupportViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SupportRepository

    val allRecords: StateFlow<List<SupportRecord>>

    private val _formState = MutableStateFlow(SupportFormState())
    val formState: StateFlow<SupportFormState> = _formState.asStateFlow()

    // Stopwatch state
    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private val _timerSeconds = MutableStateFlow(0L)
    val timerSeconds: StateFlow<Long> = _timerSeconds.asStateFlow()

    private var timerJob: Job? = null

    // Excel Export Dialog state
    private val _isExportDialogOpen = MutableStateFlow(false)
    val isExportDialogOpen: StateFlow<Boolean> = _isExportDialogOpen.asStateFlow()

    private val _exportStartDate = MutableStateFlow(todayDateString())
    val exportStartDate: StateFlow<String> = _exportStartDate.asStateFlow()

    private val _exportEndDate = MutableStateFlow(todayDateString())
    val exportEndDate: StateFlow<String> = _exportEndDate.asStateFlow()

    // History Modal state
    private val _isHistoryModalOpen = MutableStateFlow(false)
    val isHistoryModalOpen: StateFlow<Boolean> = _isHistoryModalOpen.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Settings Modal state
    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private val _settingsInitialTab = MutableStateFlow(-1)
    val settingsInitialTab: StateFlow<Int> = _settingsInitialTab.asStateFlow()

    // Global Kullanıcı (Temsilci) ve Kullanıcı Kodu (ID) stored in SharedPreferences
    private val sharedPrefs = application.getSharedPreferences("support_app_prefs", Context.MODE_PRIVATE)
    private val _globalKullanici = MutableStateFlow(sharedPrefs.getString("global_kullanici", "") ?: "")
    val globalKullanici: StateFlow<String> = _globalKullanici.asStateFlow()

    private val _globalKullaniciKodu = MutableStateFlow(sharedPrefs.getString("global_kullanici_kodu", "") ?: "")
    val globalKullaniciKodu: StateFlow<String> = _globalKullaniciKodu.asStateFlow()

    // Outlook E-posta Alıcı Adresi (Varsayılan: servis@enpos.com.tr)
    private val _outlookRecipientEmail = MutableStateFlow(
        sharedPrefs.getString("outlook_recipient_email", "servis@enpos.com.tr") ?: "servis@enpos.com.tr"
    )
    val outlookRecipientEmail: StateFlow<String> = _outlookRecipientEmail.asStateFlow()

    // Call Control & Keypad Dialog (Tam Ekran / Pop-up Çağrı Kontrol ve Tuş Takımı Ekranı)
    private val _isCallControlDialogOpen = MutableStateFlow(false)
    val isCallControlDialogOpen: StateFlow<Boolean> = _isCallControlDialogOpen.asStateFlow()

    fun openCallControlDialog() {
        _isCallControlDialogOpen.value = true
    }

    fun closeCallControlDialog() {
        _isCallControlDialogOpen.value = false
    }

    // Representative (Temsilci) List Management
    private val _representatives = MutableStateFlow<List<RepresentativePreset>>(loadSavedRepresentatives())
    val representatives: StateFlow<List<RepresentativePreset>> = _representatives.asStateFlow()

    // Session Protection & Waiting Call Queue (Kuyrukta Bekleyen Çağrılar Havuzu)
    private val _waitingCallsQueue = MutableStateFlow<List<com.example.sip.VerimorCallEvent>>(emptyList())
    val waitingCallsQueue: StateFlow<List<com.example.sip.VerimorCallEvent>> = _waitingCallsQueue.asStateFlow()

    private fun loadSavedRepresentatives(): List<RepresentativePreset> {
        val raw = sharedPrefs.getString("saved_representatives_data", null) ?: ""
        if (raw.isBlank()) return emptyList()
        return try {
            raw.split(";;;").mapNotNull { item ->
                val parts = item.split(":::")
                if (parts.size >= 2) {
                    RepresentativePreset(code = parts[0].trim(), name = parts[1].trim())
                } else null
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun persistRepresentatives(list: List<RepresentativePreset>) {
        _representatives.value = list
        val serialized = list.joinToString(";;;") { "${it.code}:::${it.name}" }
        sharedPrefs.edit().putString("saved_representatives_data", serialized).apply()
    }

    /**
     * Temsilci kodunu (örn. "6" veya "3") ayarlardaki temsilci adına çevirir.
     * Önce Ana Kullanıcı (globalKullanici & globalKullaniciKodu) kontrol edilir.
     */
    fun resolveRepresentativeName(raw: String): String {
        val clean = raw.trim()
        if (clean.isBlank()) return "Belirtilmemiş"

        // 1. Ana Kullanıcı (Varsayılan Kullanıcı Kodu ve Adı)
        val mainName = _globalKullanici.value.trim()
        val mainCode = _globalKullaniciKodu.value.trim()
        if (mainCode.isNotBlank() && clean.equals(mainCode, ignoreCase = true)) {
            if (mainName.isNotBlank() && !mainName.equals(mainCode, ignoreCase = true)) {
                return "$mainName (No: $mainCode)"
            }
            return mainName.ifBlank { mainCode }
        }
        if (mainName.isNotBlank() && clean.equals(mainName, ignoreCase = true)) {
            if (mainCode.isNotBlank() && !mainName.equals(mainCode, ignoreCase = true)) {
                return "$mainName (No: $mainCode)"
            }
            return mainName
        }

        // 2. Ek Temsilciler Listesi
        val list = _representatives.value
        val match = list.firstOrNull {
            it.code.equals(clean, ignoreCase = true) || it.name.equals(clean, ignoreCase = true)
        }
        if (match != null) {
            if (match.name.isNotBlank() && match.code.isNotBlank() && !match.name.equals(match.code, ignoreCase = true)) {
                return "${match.name} (No: ${match.code})"
            }
            return match.name.ifBlank { match.code }
        }
        if (clean.all { it.isDigit() }) {
            return "Temsilci $clean"
        }
        return clean
    }

    // SQL Database Connection Parameters (Saved in SharedPreferences and loaded automatically on app start)
    private val _sqlDbType = MutableStateFlow(sharedPrefs.getString("sql_db_type", "MySQL") ?: "MySQL")
    val sqlDbType: StateFlow<String> = _sqlDbType.asStateFlow()

    private val _sqlHost = MutableStateFlow(sharedPrefs.getString("sql_host", "") ?: "")
    val sqlHost: StateFlow<String> = _sqlHost.asStateFlow()

    private val _sqlPort = MutableStateFlow(sharedPrefs.getString("sql_port", "") ?: "")
    val sqlPort: StateFlow<String> = _sqlPort.asStateFlow()

    private val _sqlDbName = MutableStateFlow(sharedPrefs.getString("sql_db_name", "") ?: "")
    val sqlDbName: StateFlow<String> = _sqlDbName.asStateFlow()

    private val _sqlUser = MutableStateFlow(sharedPrefs.getString("sql_user", "") ?: "")
    val sqlUser: StateFlow<String> = _sqlUser.asStateFlow()

    private val _sqlPassword = MutableStateFlow(sharedPrefs.getString("sql_password", "") ?: "")
    val sqlPassword: StateFlow<String> = _sqlPassword.asStateFlow()

    // Helper: Formats dynamic Base URL as http://<IP>:<Port>
    private fun formatDynamicBaseUrl(host: String, port: String): String {
        val trimmedHost = host.trim()
        if (trimmedHost.isBlank()) return ""
        var cleanHost = trimmedHost
        if (cleanHost.startsWith("http://", ignoreCase = true)) {
            cleanHost = cleanHost.substring(7)
        } else if (cleanHost.startsWith("https://", ignoreCase = true)) {
            cleanHost = cleanHost.substring(8)
        }
        cleanHost = cleanHost.trimEnd('/')
        val trimmedPort = port.trim()
        return if (trimmedPort.isNotBlank()) {
            "http://$cleanHost:$trimmedPort"
        } else {
            "http://$cleanHost"
        }
    }

    // Dynamic API Base URL exposed to UI
    val dynamicBaseUrl: StateFlow<String> = combine(_sqlHost, _sqlPort) { host, port ->
        formatDynamicBaseUrl(host, port)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        formatDynamicBaseUrl(_sqlHost.value, _sqlPort.value)
    )

    fun getDynamicBaseUrl(): String {
        val base = formatDynamicBaseUrl(_sqlHost.value, _sqlPort.value)
        return if (base.isNotBlank()) "$base/" else ""
    }

    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection: StateFlow<Boolean> = _isTestingConnection.asStateFlow()

    private val _connectionTestResult = MutableStateFlow<String?>(null)
    val connectionTestResult: StateFlow<String?> = _connectionTestResult.asStateFlow()

    // Syncing state
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // Live Unsynced Record Count
    val unsyncedCount: StateFlow<Int>

    // Auto-fill suggestion state
    private val _autoFillSuggestion = MutableStateFlow<AutoFillSuggestion?>(null)
    val autoFillSuggestion: StateFlow<AutoFillSuggestion?> = _autoFillSuggestion.asStateFlow()

    // Smart Multi-Candidate Match Picker Dialog State
    private val _matchPickerState = MutableStateFlow<CustomerSearchResultState?>(null)
    val matchPickerState: StateFlow<CustomerSearchResultState?> = _matchPickerState.asStateFlow()

    private var lookupJob: Job? = null

    // Show advanced fields (Gizlenen Alanlar / 2. Sayfa) toggle - default TRUE (AÇIK) and saved in SharedPreferences
    private val _showAdvancedFields = MutableStateFlow(sharedPrefs.getBoolean("show_advanced_fields", true))
    val showAdvancedFields: StateFlow<Boolean> = _showAdvancedFields.asStateFlow()

    // Depolama & Hafıza Boyut Analizi (Uygulama ne kadar yer kaplıyor, ne kadar yer açılabilir)
    private val _storageBreakdown = MutableStateFlow(com.example.backup.AppStorageBreakdown())
    val storageBreakdown: StateFlow<com.example.backup.AppStorageBreakdown> = _storageBreakdown.asStateFlow()

    // Cihaz / Sicil Servis Raporu Dialog State
    private val _deviceReportData = MutableStateFlow<DeviceReportData?>(null)
    val deviceReportData: StateFlow<DeviceReportData?> = _deviceReportData.asStateFlow()

    fun refreshStorageBreakdown(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val breakdown = com.example.backup.BackupManager.calculateStorageBreakdown(context, repository)
            _storageBreakdown.value = breakdown
        }
    }

    fun formatBytes(bytes: Long): String = com.example.backup.BackupManager.formatStorageBytes(bytes)

    // Date range delete state in Settings
    private val _deleteStartDate = MutableStateFlow(todayDateString())
    val deleteStartDate: StateFlow<String> = _deleteStartDate.asStateFlow()

    private val _deleteEndDate = MutableStateFlow(todayDateString())
    val deleteEndDate: StateFlow<String> = _deleteEndDate.asStateFlow()

    // Verimor Bulut Santral SIP Configuration & State
    private val _verimorDomain = MutableStateFlow(sharedPrefs.getString("verimor_domain", "sirketiniz.bulutsantralim.com") ?: "sirketiniz.bulutsantralim.com")
    val verimorDomain: StateFlow<String> = _verimorDomain.asStateFlow()

    private val _verimorExtension = MutableStateFlow(sharedPrefs.getString("verimor_extension", "101") ?: "101")
    val verimorExtension: StateFlow<String> = _verimorExtension.asStateFlow()

    private val _verimorPassword = MutableStateFlow(sharedPrefs.getString("verimor_password", "") ?: "")
    val verimorPassword: StateFlow<String> = _verimorPassword.asStateFlow()

    private val _verimorPort = MutableStateFlow(sharedPrefs.getString("verimor_port", "5060") ?: "5060")
    val verimorPort: StateFlow<String> = _verimorPort.asStateFlow()

    private val _verimorAutoTimer = MutableStateFlow(sharedPrefs.getBoolean("verimor_auto_timer", true))
    val verimorAutoTimer: StateFlow<Boolean> = _verimorAutoTimer.asStateFlow()

    private val _verimorAutoSearch = MutableStateFlow(sharedPrefs.getBoolean("verimor_auto_search", true))
    val verimorAutoSearch: StateFlow<Boolean> = _verimorAutoSearch.asStateFlow()

    // Persistent SIP connection state: user preference to stay connected until explicitly disconnected
    private val _verimorAutoConnect = MutableStateFlow(sharedPrefs.getBoolean("verimor_auto_connect", true))
    val verimorAutoConnect: StateFlow<Boolean> = _verimorAutoConnect.asStateFlow()

    val verimorSipManager = com.example.sip.VerimorSipManager.getInstance(application)
    val verimorState: StateFlow<com.example.sip.VerimorSipState> = verimorSipManager.connectionState

    private val _activeIncomingCall = MutableStateFlow<com.example.sip.VerimorCallEvent?>(null)
    val activeIncomingCall: StateFlow<com.example.sip.VerimorCallEvent?> = _activeIncomingCall.asStateFlow()

    // Fixed Bottom Navigation Tab (0: Ana Ekran, 1: Geçmiş, 2: Kaçan Çağrılar, 3: Ayarlar)
    private val _currentBottomNavTab = MutableStateFlow(0)
    val currentBottomNavTab: StateFlow<Int> = _currentBottomNavTab.asStateFlow()

    fun setBottomNavTab(tab: Int) {
        _currentBottomNavTab.value = tab
        _matchPickerState.value = null
        _autoFillSuggestion.value = null
        lookupJob?.cancel()
    }

    /**
     * Requirement 1: Telefon çaldığı veya çağrıya geçilmek istendiği anda uygulama
     * otomatik olarak gecikmeksizin istisnasız Ana Destek Ekranına (1. Çağrı Girişi) kilitlenir
     * ve tüm diyaloglar/pencereler derhal kapatılır.
     */
    fun navigateToMainCallScreen() {
        _currentBottomNavTab.value = 0
        closeSettings()
        closeHistoryModal()
        closeExportDialog()
        dismissAutoFillSuggestion()
        dismissMatchPicker()
        _isCallControlDialogOpen.value = false
    }

    // Master On/Off Switch: Stop all background SIP and network activities
    private val _masterPowerEnabled = MutableStateFlow(sharedPrefs.getBoolean("master_power_enabled", true))
    val masterPowerEnabled: StateFlow<Boolean> = _masterPowerEnabled.asStateFlow()

    // Duty / Off-Hours Mode (Nöbet / Mesai Dışı Modu): Only track and notify missed calls when enabled
    private val _dutyModeEnabled = MutableStateFlow(sharedPrefs.getBoolean("duty_mode_enabled", false))
    val dutyModeEnabled: StateFlow<Boolean> = _dutyModeEnabled.asStateFlow()

    // All Missed Calls from Database
    val allMissedCalls: StateFlow<List<MissedCallRecord>>

    // Templates for quick completion / suggestion
    val tespitTemplates: StateFlow<List<com.example.model.SupportTemplate>>
    val islemTemplates: StateFlow<List<com.example.model.SupportTemplate>>
    val konuTemplates: StateFlow<List<com.example.model.SupportTemplate>>
    val prefixTemplates: StateFlow<List<com.example.model.SupportTemplate>>

    // Dynamic Popular Choices Ranked by Frequency in Records (Tek Satır Yatay Çipler)
    val popularTopics: StateFlow<List<String>>
    val popularTespit: StateFlow<List<String>>
    val popularIslem: StateFlow<List<String>>

    // Daily Support Analytics
    val supportDailyStats: StateFlow<SupportDailyStats>

    // Statistics Dialog State
    private val _isStatisticsDialogOpen = MutableStateFlow(false)
    val isStatisticsDialogOpen: StateFlow<Boolean> = _isStatisticsDialogOpen.asStateFlow()

    fun openStatisticsDialog() {
        _isStatisticsDialogOpen.value = true
    }

    fun closeStatisticsDialog() {
        _isStatisticsDialogOpen.value = false
    }

    // Daily Shift Report Dialog State
    private val _isDailyReportDialogOpen = MutableStateFlow(false)
    val isDailyReportDialogOpen: StateFlow<Boolean> = _isDailyReportDialogOpen.asStateFlow()

    fun openDailyReportDialog() {
        _isDailyReportDialogOpen.value = true
    }

    fun closeDailyReportDialog() {
        _isDailyReportDialogOpen.value = false
    }

    // Service Manager Executive Dialog State
    private val _isServiceManagerDialogOpen = MutableStateFlow(false)
    val isServiceManagerDialogOpen: StateFlow<Boolean> = _isServiceManagerDialogOpen.asStateFlow()

    fun openServiceManagerDialog() {
        _isServiceManagerDialogOpen.value = true
    }

    fun closeServiceManagerDialog() {
        _isServiceManagerDialogOpen.value = false
    }

    private val _isFetchingFromSql = MutableStateFlow(false)
    val isFetchingFromSql: StateFlow<Boolean> = _isFetchingFromSql.asStateFlow()

    // Quick Solution Scenarios (Kişiselleştirilebilir Sık Karşılaşılan Senaryolar)
    companion object {
        val DEFAULT_QUICK_SCENARIOS = listOf(
            QuickScenario(
                id = "z_raporu",
                title = "Z Raporu / Gün Sonu",
                icon = "🧾",
                konu = "Gün Sonu / Z Raporu Hatası",
                tespit = "Mali hafıza Z raporu iletişim/yazıcı hatası",
                islem = "Mali rapor kuyruğu kontrol edildi, Z raporu başarıyla alındı ve onaylandı."
            ),
            QuickScenario(
                id = "mali_hafiza",
                title = "Mali Hafıza",
                icon = "💾",
                konu = "Mali Hafıza / Hafıza Sonu Uyarısı",
                tespit = "Mali hafıza kapasitesi ve son Z raporu kontrolü",
                islem = "Mali hafıza durumu sorgulandı, servis kaydı oluşturularak yetkili bayiye yönlendirildi."
            ),
            QuickScenario(
                id = "ag_baglanti",
                title = "Ağ / IP Bağlantısı",
                icon = "🌐",
                konu = "Ağ Bağlantısı / Ethernet Hatası",
                tespit = "Yazar kasa IP adresi ve ağ geçidi bağlantısı kopuk",
                islem = "Kablo ve switch kontrol edildi, statik IP adresi ve port yapılandırması yenilendi."
            ),
            QuickScenario(
                id = "yazici_kagit",
                title = "Yazıcı / Kağıt",
                icon = "🖨️",
                konu = "Yazıcı / Fiş Kesme Arızası",
                tespit = "Rulo mekanizmasında kağıt sıkışması / kutter arızası",
                islem = "Rulo kapağı manuel açılarak sıkışan kağıt temizlendi, test fişi kesildi."
            ),
            QuickScenario(
                id = "pos_banka",
                title = "POS / Banka Provizyon",
                icon = "💳",
                konu = "POS / Banka İletişim Hatası",
                tespit = "Banka EFT-POS bağlantısında zaman aşımı hatası",
                islem = "Banka terminal portu ve haberleşme parametreleri güncellenerek provizyon testi alındı."
            ),
            QuickScenario(
                id = "versiyon_guncelleme",
                title = "Versiyon Güncelleme",
                icon = "🔄",
                konu = "Yazılım & Versiyon Güncelleme",
                tespit = "Eski versiyondan kaynaklı menü kilitlenmesi",
                islem = "Cihaz en son kararlı OKC ve DynCash versiyonuna güncellendi, parametreler yüklendi."
            ),
            QuickScenario(
                id = "yedek_alma",
                title = "Yedek Alma / Aktarım",
                icon = "📦",
                konu = "Veritabanı / Yedek Alma",
                tespit = "Cihaz veya arka ofis yedekleme talebi",
                islem = "Mali ve operasyonel veriler harici ortama yedeklendi, doğrulandı."
            ),
            QuickScenario(
                id = "sifre_yetki",
                title = "Şifre / Yetki Sıfırlama",
                icon = "🔐",
                konu = "Kullanıcı & Şifre İşlemleri",
                tespit = "Kasiyer şifresi unutulmuş / yetki kısıtı",
                islem = "Yönetici şifresi doğrulanarak yetkili kasiyer hesabı sıfırlandı ve test edildi."
            )
        )

        // Knowledge Base Default Articles (Zengin POS & ÖKC Arıza / Hata Kodu Bilgi Bankası)
        val DEFAULT_KNOWLEDGE_ARTICLES = listOf(
            KnowledgeArticle(
                id = "kb_304",
                errorCode = "ERR-304",
                title = "Hata 304 - Mali Mühür Zaman Aşımı & GİB İletişim Hatası",
                category = "Mali & GİB",
                cause = "Mali bellek zaman damgası veya GİB SSL güvenlik sertifikasının süresinin dolması/uyuşmaması.",
                steps = listOf(
                    "Kasanın internet ve DNS ayarlarını kontrol edin (8.8.8.8 veya 1.1.1.1).",
                    "Menü > Sistem Ayarları > Tarih/Saat menüsünden sistem saatini senkronize edin.",
                    "Menü > Mali İşlemler > Sertifika Güncelleme seçeneğini çalıştırın.",
                    "Gün sonu / Z raporu denemesi yaparak mali iletişimi doğrulayın."
                ),
                recommendedTespit = "Mali mühür zaman damgası ve GİB SSL sertifikası zaman aşımı hatası (Hata 304).",
                recommendedIslem = "Ağ ve DNS ayarları yapılandırıldı, mali sertifika güncellendi ve test X raporu alındı."
            ),
            KnowledgeArticle(
                id = "kb_500",
                errorCode = "E-FAT-500",
                title = "Hata 500 / 502 - E-Arşiv & E-Fatura Entegratör Sunucu Hatası",
                category = "E-Dönüşüm",
                cause = "Özel entegratör (DynCash / Enpos) sunucusunun yanıt vermemesi veya oturum tokeninin düşmesi.",
                steps = listOf(
                    "Entegratör portalına web tarayıcısından girerek hesap ve kontör durumunu doğrulayın.",
                    "DynCash arka ofis servis yöneticisinden (IntegratorService) servisi yeniden başlatın.",
                    "Bekleyen e-fatura kuyruğundaki hatalı paketleri seçip 'Yeniden Gönder' butonuna basın.",
                    "Kasa ekranında satış yapılarak e-arşiv fiş çıktısının basıldığını teyit edin."
                ),
                recommendedTespit = "E-Arşiv / E-Fatura entegratör haberleşme kuyruğunda token zaman aşımı.",
                recommendedIslem = "Entegratör servisleri yeniden başlatıldı, bekleyen fatura paketi başarıyla onaylandı."
            ),
            KnowledgeArticle(
                id = "kb_prn24",
                errorCode = "PRN-24",
                title = "Hata 24 - Kutter (Kağıt Bıçak) Kilitlenmesi & Kağıt Sıkışması",
                category = "Yazıcı & Mekanik",
                cause = "Rulo kağıt parçacıklarının kutter bıçağını sıkıştırması veya rulo kapağının zorlanması.",
                steps = listOf(
                    "Kasa güç anahtarını kapatın ve elektrik fişini çekin.",
                    "Yazıcı yanındaki manuel kutter dişlisini saat yönünde çevirerek bıçağı geri yuvasına çekin.",
                    "Rulo kapağı mandalını açın, sıkışan kağıt parçalarını cımbız veya elle temizleyin.",
                    "Yeni termal ruloyu takıp kapağı sıkıca kapatın, FEED tuşuna basarak kağıt ilerletmeyi test edin."
                ),
                recommendedTespit = "Termal yazıcı kutter otomatik kesici bıçağında kağıt sıkışması ve motor kilitlenmesi.",
                recommendedIslem = "Kutter bıçağı manuel serbest bırakıldı, kağıt yolu temizlendi ve test fişi kesildi."
            ),
            KnowledgeArticle(
                id = "kb_net01",
                errorCode = "NET-01",
                title = "Ağ Kopuk / IP 0.0.0.0 Hatası (Ethernet & Statik IP)",
                category = "Ağ & Donanım",
                cause = "DHCP sunucusundan IP alınamaması, switch portunun düşmesi veya ağ kablosu temassızlığı.",
                steps = listOf(
                    "Kasa arkasındaki RJ45 Ethernet soketinin yeşil/turuncu LED ışıklarını kontrol edin.",
                    "Kabloyu çıkarıp switch üzerindeki çalışan başka bir porta takın.",
                    "Menü > Ağ Ayarları > Statik IP seçeneğini açıp kasanın kayıtlı IP'sini (örn: 192.168.1.150) elle girin.",
                    "Menü > Ağ Testi > Ağ Geçidi (Gateway) Ping testini çalıştırarak bağlantıyı doğrulayın."
                ),
                recommendedTespit = "Ağ kablosu temassızlığı ve DHCP IP atama hatası (IP 0.0.0.0).",
                recommendedIslem = "Ethernet soketi ve switch portu kontrol edildi, statik IP ve ağ geçidi tanımlandı."
            ),
            KnowledgeArticle(
                id = "kb_pos91",
                errorCode = "POS-91",
                title = "Hata 91 - Banka POS Provizyon Zaman Aşımı (EFT-POS Bağlantısı)",
                category = "POS & Banka",
                cause = "Banka POS terminali ile yazar kasa arasındaki seri port / USB haberleşme kopukluğu.",
                steps = listOf(
                    "POS cihazı ile kasa arasındaki haberleşme ara kablosunu söküp pinleri kontrol ederek yeniden takın.",
                    "Banka POS cihazının güç adaptörünü söküp açın; POS ekranından 'Gün Sonu' veya 'Parametre Yükle' yapın.",
                    "Kasa POS ayarlarından COM portu hızını (Baud Rate 115200) kontrol edin.",
                    "Kasa üzerinden 1 TL'lik kartlı satış ve hemen ardından provizyon iptal testi yapın."
                ),
                recommendedTespit = "Kasa ile EFT-POS terminali arasında COM port haberleşme zaman aşımı (Hata 91).",
                recommendedIslem = "Haberleşme kablosu ve port parametreleri yenilendi, banka test provizyonu başarılı alındı."
            ),
            KnowledgeArticle(
                id = "kb_zrapor",
                errorCode = "Z-RAPOR",
                title = "Z Raporu Kilitlendi / 24 Saatlik Mali Gün Sonu Hatası",
                category = "Mali & GİB",
                cause = "Kasada açık satış/fatura kalması veya 24 saat kuralı gereği kasanın satışa kapanması.",
                steps = listOf(
                    "Kasa ekranında açık/askıda fiş veya yarım kalmış işlem olup olmadığını kontrol edin ve iptal edin.",
                    "Menü > Raporlar > Mali Raporlar > Z Raporu seçeneğini açın.",
                    "Zorunlu Gün Sonu onayına 'Evet' diyerek mali hafızaya yazılmasını sağlayın.",
                    "Z Raporunun basıldığını ve mali sayaç numarasının ilerlediğini kontrol edin."
                ),
                recommendedTespit = "Açık işlem nedeniyle 24 saatlik kanuni Z raporu gün sonunun kilitlenmesi.",
                recommendedIslem = "Askıdaki işlem temizlendi, mali Z raporu başarıyla yazdırılarak gün devri tamamlandı."
            ),
            KnowledgeArticle(
                id = "kb_malihf",
                errorCode = "MALİ-HAFIZA",
                title = "Mali Hafıza Sonu (Bellek Dolu Uyarısı)",
                category = "Mali & GİB",
                cause = "Kasanın 2500 adetlik kanuni mali Z raporu kapasitesinin dolması.",
                steps = listOf(
                    "Kasa ekranındaki 'Kalan Z Sayısı' uyarısını kontrol edin.",
                    "Kasa mali hafıza durum raporunu (Mali Kümülatif Rapor) yazdırıp arşivleyin.",
                    "Yetkili servis ve ilgili Vergi Dairesi nezdinde mali hafıza değişim tutanağı hazırlayın.",
                    "Müşteriye yeni mali hafıza değişimi için teknik servis randevusu ve yönlendirmesi yapın."
                ),
                recommendedTespit = "Kasa mali hafıza Z raporu kayıt kapasitesinin dolması.",
                recommendedIslem = "Mali durum raporu alındı, servis değişim tutanağı oluşturularak yetkili bayiye iletildi."
            ),
            KnowledgeArticle(
                id = "kb_sifre",
                errorCode = "ŞİFRE-RESET",
                title = "Kasiyer / Yönetici Şifre Sıfırlama & Hesap Kilidi",
                category = "Kullanıcı & Güvenlik",
                cause = "Kasiyer veya mağaza yetkilisinin giriş şifresini unutması veya hatalı girişle kilitlenmesi.",
                steps = listOf(
                    "Süpervizör / Master Admin hesabı ile kasanın güvenlik menüsüne giriş yapın.",
                    "Menü > Ayarlar > Kullanıcı Yönetimi ekranını açın.",
                    "İlgili kasiyer numarasını seçip 'Şifre Sıfırla' butonuna basın.",
                    "Geçici şifre tanımlayıp kasiyerin ilk oturum açışında şifresini değiştirmesini sağlayın."
                ),
                recommendedTespit = "Kasiyer hesabının hatalı şifre denemeleri sebebiyle kilitlenmesi.",
                recommendedIslem = "Yönetici yetkisiyle kullanıcı hesabı serbest bırakıldı, yeni şifre atanarak test edildi."
            ),
            KnowledgeArticle(
                id = "kb_tsm",
                errorCode = "GİB-TSM",
                title = "GİB TSM Raporlama & Zaman Aşımı Uyarısı",
                category = "Mali & GİB",
                cause = "Gelir İdaresi Başkanlığı TSM merkezine günlük mali rapor paketinin gönderilememesi.",
                steps = listOf(
                    "Kasanın internet bağlantısını ve TSM sunucu durumunu kontrol edin.",
                    "Menü > TSM İşlemleri > TSM Parametre Kontrolü yapın.",
                    "'TSM Manuel Rapor Gönder' fonksiyonunu çalıştırın.",
                    "Ekranda 'GİB TSM Paketi Başarıyla Onaylandı' bilgi fişini alın."
                ),
                recommendedTespit = "GİB ÖKC TSM merkezine günlük mali rapor aktarımının beklemeye düşmesi.",
                recommendedIslem = "TSM sunucu bağlantısı kontrol edildi, bekleyen mali paketler manuel tetiklenerek onaylandı."
            ),
            KnowledgeArticle(
                id = "kb_dblock",
                errorCode = "DB-LOCK",
                title = "DynCash Veritabanı Kilitlenmesi & SQLite Lock",
                category = "Yazılım & Veritabanı",
                cause = "Beklenmeyen elektrik kesintisi veya arka ofis sürecinin veritabanını kilitlemesi.",
                steps = listOf(
                    "Arka ofis veya kasadaki çalışan POS uygulamasını Görev Yöneticisinden kapatın.",
                    "Uygulama veri klasöründeki geçici '.db-journal' veya '.wal' geçici kilit dosyalarını temizleyin.",
                    "Veritabanı Onarım / Reindex aracını çalıştırın.",
                    "Uygulamayı yeniden başlatıp cari ve ürün aramasının hızlı çalıştığını doğrulayın."
                ),
                recommendedTespit = "Ani elektrik kesintisi sonrası veritabanı kilitlenme ve indeks bozulması.",
                recommendedIslem = "Veritabanı kilit dosyaları temizlendi, indeksler onarıldı ve satış ekranı açıldı."
            ),
            KnowledgeArticle(
                id = "kb_barkod",
                errorCode = "BARKOD-01",
                title = "Barkod Okuyucu / Tarayıcı Okumuyor veya Hatalı Yazıyor",
                category = "Donanım & Çevre Birimleri",
                cause = "USB barkod okuyucu optik camının kirlenmesi, kablo gevşemesi veya Türkçe klavye modunun bozulması.",
                steps = listOf(
                    "Barkod okuyucu USB soketini kasanın arkasındaki başka bir USB portuna takın.",
                    "Barkod okuyucunun konfigürasyon kitapçığından 'USB HID Klavye' ve 'Türkçe Q Klavye' barkodunu okutun.",
                    "Okuyucunun 'Enter / CR Suffix' ekleme barkodunu okutarak otomatik satır sonunu aktif edin.",
                    "Not Defteri veya satış ekranında örnek bir ürün barkodunu okutarak test edin."
                ),
                recommendedTespit = "Barkod okuyucu klavye dil modu ve CR suffix ayarının bozulması.",
                recommendedIslem = "Barkod okuyucu fabrika ayarları ve Türkçe Q klavye parametreleri okutularak test edildi."
            ),
            KnowledgeArticle(
                id = "kb_verupg",
                errorCode = "VER-UPG",
                title = "Yazılım Versiyon Güncelleme Hatası (CRC / İndirme Hatası)",
                category = "Yazılım & Lisans",
                cause = "Güncelleme dosyasının eksik inmesi veya indirme sunucusu bağlantı kopukluğu.",
                steps = listOf(
                    "Kasa Güncelleme ekranında 'Önbelleği Temizle' seçeneğine tıklayın.",
                    "Kasanın DNS ve internet hızını test edin.",
                    "FTP / Güncelleme sunucusundan paket indirmeyi manuel başlatın.",
                    "Güncelleme tamamlandıktan sonra cihazı yeniden başlatıp versiyon kodunu kontrol edin."
                ),
                recommendedTespit = "Güncelleme paketi indirme esnasında CRC dosya bozulması.",
                recommendedIslem = "İndirme önbelleği temizlendi, güncel sürüm paketi yüklenerek kasa yeniden başlatıldı."
            )
        )
    }

    private val _quickScenarios = MutableStateFlow<List<QuickScenario>>(loadSavedQuickScenarios())
    val quickScenarios: StateFlow<List<QuickScenario>>

    private fun loadSavedQuickScenarios(): List<QuickScenario> {
        val raw = sharedPrefs.getString("saved_quick_scenarios_json", null) ?: return DEFAULT_QUICK_SCENARIOS
        return try {
            val jsonArray = org.json.JSONArray(raw)
            val list = mutableListOf<QuickScenario>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    QuickScenario(
                        id = obj.optString("id", System.currentTimeMillis().toString()),
                        title = obj.optString("title", ""),
                        icon = obj.optString("icon", "⚡"),
                        konu = obj.optString("konu", ""),
                        tespit = obj.optString("tespit", ""),
                        islem = obj.optString("islem", ""),
                        usageCount = obj.optInt("usageCount", 0)
                    )
                )
            }
            if (list.isEmpty()) DEFAULT_QUICK_SCENARIOS else list
        } catch (_: Exception) {
            DEFAULT_QUICK_SCENARIOS
        }
    }

    private fun persistQuickScenarios(list: List<QuickScenario>) {
        _quickScenarios.value = list
        try {
            val jsonArray = org.json.JSONArray()
            list.forEach { sc ->
                val obj = org.json.JSONObject().apply {
                    put("id", sc.id)
                    put("title", sc.title)
                    put("icon", sc.icon)
                    put("konu", sc.konu)
                    put("tespit", sc.tespit)
                    put("islem", sc.islem)
                    put("usageCount", sc.usageCount)
                }
                jsonArray.put(obj)
            }
            sharedPrefs.edit().putString("saved_quick_scenarios_json", jsonArray.toString()).apply()
        } catch (_: Exception) {}
    }

    fun addQuickScenario(title: String, icon: String, konu: String, tespit: String, islem: String) {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return
        val newScenario = QuickScenario(
            id = "scenario_${System.currentTimeMillis()}",
            title = cleanTitle,
            icon = icon.trim().ifBlank { "⚡" },
            konu = konu.trim(),
            tespit = tespit.trim(),
            islem = islem.trim()
        )
        persistQuickScenarios(_quickScenarios.value + newScenario)
        viewModelScope.launch { _uiEvent.emit("Yeni şablon eklendi: $cleanTitle") }
    }

    fun updateQuickScenario(id: String, title: String, icon: String, konu: String, tespit: String, islem: String) {
        val updated = _quickScenarios.value.map { sc ->
            if (sc.id == id) {
                sc.copy(
                    title = title.trim().ifBlank { sc.title },
                    icon = icon.trim().ifBlank { sc.icon },
                    konu = konu.trim(),
                    tespit = tespit.trim(),
                    islem = islem.trim()
                )
            } else sc
        }
        persistQuickScenarios(updated)
        viewModelScope.launch { _uiEvent.emit("Şablon güncellendi: $title") }
    }

    fun deleteQuickScenario(id: String) {
        val filtered = _quickScenarios.value.filter { it.id != id }
        persistQuickScenarios(filtered)
        viewModelScope.launch { _uiEvent.emit("Şablon silindi") }
    }

    fun resetDefaultQuickScenarios() {
        persistQuickScenarios(DEFAULT_QUICK_SCENARIOS)
        viewModelScope.launch { _uiEvent.emit("Şablonlar varsayılana sıfırlandı") }
    }

    // --- KNOWLEDGE BASE / ARIZA REHBERİ (BİLGİ BANKASI) ---
    private val _knowledgeArticles = MutableStateFlow<List<KnowledgeArticle>>(loadSavedKnowledgeArticles())
    val knowledgeArticles: StateFlow<List<KnowledgeArticle>> = _knowledgeArticles.asStateFlow()

    private val _isKnowledgeBaseDialogOpen = MutableStateFlow(false)
    val isKnowledgeBaseDialogOpen: StateFlow<Boolean> = _isKnowledgeBaseDialogOpen.asStateFlow()

    fun openKnowledgeBaseDialog() {
        _isKnowledgeBaseDialogOpen.value = true
    }

    fun closeKnowledgeBaseDialog() {
        _isKnowledgeBaseDialogOpen.value = false
    }

    private fun loadSavedKnowledgeArticles(): List<KnowledgeArticle> {
        val raw = sharedPrefs.getString("saved_knowledge_articles_json", null) ?: return DEFAULT_KNOWLEDGE_ARTICLES
        return try {
            val jsonArray = org.json.JSONArray(raw)
            val list = mutableListOf<KnowledgeArticle>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val stepsArray = obj.optJSONArray("steps")
                val stepsList = mutableListOf<String>()
                if (stepsArray != null) {
                    for (j in 0 until stepsArray.length()) {
                        stepsList.add(stepsArray.getString(j))
                    }
                }
                list.add(
                    KnowledgeArticle(
                        id = obj.optString("id", System.currentTimeMillis().toString()),
                        errorCode = obj.optString("errorCode", ""),
                        title = obj.optString("title", ""),
                        category = obj.optString("category", "Genel"),
                        cause = obj.optString("cause", ""),
                        steps = stepsList,
                        recommendedTespit = obj.optString("recommendedTespit", ""),
                        recommendedIslem = obj.optString("recommendedIslem", "")
                    )
                )
            }
            if (list.isEmpty()) DEFAULT_KNOWLEDGE_ARTICLES else list
        } catch (_: Exception) {
            DEFAULT_KNOWLEDGE_ARTICLES
        }
    }

    private fun persistKnowledgeArticles(list: List<KnowledgeArticle>) {
        _knowledgeArticles.value = list
        try {
            val jsonArray = org.json.JSONArray()
            list.forEach { art ->
                val obj = org.json.JSONObject().apply {
                    put("id", art.id)
                    put("errorCode", art.errorCode)
                    put("title", art.title)
                    put("category", art.category)
                    put("cause", art.cause)
                    val sArr = org.json.JSONArray()
                    art.steps.forEach { sArr.put(it) }
                    put("steps", sArr)
                    put("recommendedTespit", art.recommendedTespit)
                    put("recommendedIslem", art.recommendedIslem)
                }
                jsonArray.put(obj)
            }
            sharedPrefs.edit().putString("saved_knowledge_articles_json", jsonArray.toString()).apply()
        } catch (_: Exception) {}
    }

    fun addKnowledgeArticle(
        errorCode: String,
        title: String,
        category: String,
        cause: String,
        steps: List<String>,
        recommendedTespit: String,
        recommendedIslem: String
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return
        val newArticle = KnowledgeArticle(
            id = "art_${System.currentTimeMillis()}",
            errorCode = errorCode.trim().uppercase(Locale("tr", "TR")),
            title = cleanTitle,
            category = category.trim().ifBlank { "Genel" },
            cause = cause.trim(),
            steps = steps.map { it.trim() }.filter { it.isNotBlank() },
            recommendedTespit = recommendedTespit.trim(),
            recommendedIslem = recommendedIslem.trim()
        )
        persistKnowledgeArticles(_knowledgeArticles.value + newArticle)
        viewModelScope.launch { _uiEvent.emit("Rehbere yeni madde eklendi: $cleanTitle") }
    }

    fun updateKnowledgeArticle(
        id: String,
        errorCode: String,
        title: String,
        category: String,
        cause: String,
        steps: List<String>,
        recommendedTespit: String,
        recommendedIslem: String
    ) {
        val updated = _knowledgeArticles.value.map { art ->
            if (art.id == id) {
                art.copy(
                    errorCode = errorCode.trim().uppercase(Locale("tr", "TR")),
                    title = title.trim().ifBlank { art.title },
                    category = category.trim().ifBlank { art.category },
                    cause = cause.trim(),
                    steps = steps.map { it.trim() }.filter { it.isNotBlank() },
                    recommendedTespit = recommendedTespit.trim(),
                    recommendedIslem = recommendedIslem.trim()
                )
            } else art
        }
        persistKnowledgeArticles(updated)
        viewModelScope.launch { _uiEvent.emit("Rehber maddesi güncellendi: $title") }
    }

    fun deleteKnowledgeArticle(id: String) {
        val filtered = _knowledgeArticles.value.filter { it.id != id }
        persistKnowledgeArticles(filtered)
        viewModelScope.launch { _uiEvent.emit("Rehber maddesi silindi") }
    }

    fun resetDefaultKnowledgeArticles() {
        persistKnowledgeArticles(DEFAULT_KNOWLEDGE_ARTICLES)
        viewModelScope.launch { _uiEvent.emit("Rehber varsayılan maddelere sıfırlandı") }
    }

    fun applyKnowledgeArticleToForm(article: KnowledgeArticle) {
        _formState.update { current ->
            current.copy(
                destekKonu = article.title,
                tespit = article.recommendedTespit.ifBlank { article.cause },
                islem = article.recommendedIslem.ifBlank { article.steps.joinToString(" • ") }
            )
        }
        viewModelScope.launch {
            _uiEvent.emit("📚 Rehber Çözümü Forma Aktarıldı: ${article.title}")
        }
    }

    fun shareKnowledgeArticleViaWhatsApp(context: Context, article: KnowledgeArticle) {
        val targetPhone = _formState.value.telefon.ifBlank { _formState.value.telefon2 }
        val custName = _formState.value.musteriBayi.ifBlank { _formState.value.gorusulen }
        WhatsAppHelper.shareKnowledgeArticle(
            context = context,
            article = article,
            phoneRaw = targetPhone,
            customerName = custName
        )
    }

    // --- GÜN SONU / VARDİYA ÖZETİ RAPORU (DAILY SHIFT REPORT) ---
    fun formatYmdToDisplay(ymd: String): String {
        val clean = ymd.trim()
        val parts = clean.split("-")
        return if (parts.size == 3) "${parts[2]}.${parts[1]}.${parts[0]}" else clean
    }

    fun isRecordOnDate(recordDate: String, targetDateYmd: String): Boolean {
        val clean = recordDate.trim().take(10)
        if (clean == targetDateYmd) return true
        val parts = targetDateYmd.split("-")
        if (parts.size == 3) {
            val dmy = "${parts[2]}.${parts[1]}.${parts[0]}"
            if (clean == dmy) return true
        }
        return false
    }

    fun generateDailyShiftReport(targetDateStr: String = todayDateString()): String {
        val dateYmd = targetDateStr.trim().ifBlank { todayDateString() }
        val displayDate = formatYmdToDisplay(dateYmd)
        val targetRecords = allRecords.value.filter { isRecordOnDate(it.baslangicTarihi, dateYmd) }

        val totalCalls = targetRecords.size
        var totalSecs = 0L

        targetRecords.forEach { r ->
            val s = r.toplamSure.trim()
            val parts = s.split(":")
            if (parts.size == 2) {
                val m = parts[0].toLongOrNull() ?: 0L
                val sec = parts[1].toLongOrNull() ?: 0L
                totalSecs += (m * 60 + sec)
            } else if (parts.size == 3) {
                val h = parts[0].toLongOrNull() ?: 0L
                val m = parts[1].toLongOrNull() ?: 0L
                val sec = parts[2].toLongOrNull() ?: 0L
                totalSecs += (h * 3600 + m * 60 + sec)
            }
        }

        val resolvedCount = targetRecords.count { it.bekleyenIs.equals("Hayır", ignoreCase = true) || it.bekleyenIs.isBlank() }
        val pendingCount = targetRecords.count { it.bekleyenIs.equals("Evet", ignoreCase = true) }
        val avgSecs = if (totalCalls > 0) totalSecs / totalCalls else 0L

        // Top topics
        val topicCounts = targetRecords.map { it.destekKonusu1.trim().ifBlank { "Diğer" } }
            .groupingBy { it }.eachCount()
        val topTopics = topicCounts.entries.sortedByDescending { it.value }.take(3)

        // Top customers
        val customerCounts = targetRecords.map { it.musteriBayiAdi.trim().ifBlank { "İsimsiz Müşteri" } }
            .groupingBy { it }.eachCount()
        val topCustomers = customerCounts.entries.sortedByDescending { it.value }.take(3)

        val repName = _globalKullanici.value.ifBlank { "Servis Temsilcisi" }
        val repCode = _globalKullaniciKodu.value.ifBlank { "" }
        val repSuffix = if (repCode.isNotBlank()) " (Kod: $repCode)" else ""

        val totalDurationText = if (totalSecs >= 3600) {
            val h = totalSecs / 3600
            val m = (totalSecs % 3600) / 60
            "${h} sa ${m} dk"
        } else {
            val m = totalSecs / 60
            val s = totalSecs % 60
            "${m} dk ${s} sn"
        }

        val avgDurationText = "${avgSecs / 60} dk ${avgSecs % 60} sn"

        return buildString {
            append("📋 *GÜNLÜK SERVİS RAPORU ($displayDate)*\n")
            append("👤 *Temsilci:* $repName$repSuffix\n")
            append("📞 *Toplam Çağrı:* $totalCalls Adet\n")
            append("⏱️ *Toplam Süre:* $totalDurationText (Ort. $avgDurationText)\n")
            append("✅ *Çözülen:* $resolvedCount | ⏳ *Bekleyen:* $pendingCount\n")
            if (topTopics.isNotEmpty()) {
                val topicsSummary = topTopics.joinToString(", ") { (topic, count) ->
                    val pct = if (totalCalls > 0) (count * 100 / totalCalls) else 0
                    "%$pct $topic"
                }
                append("📌 *En Sık Karşılaşılan:* $topicsSummary\n")
            }
            if (topCustomers.isNotEmpty()) {
                append("━━━━━━━━━━━━━━━━━━━━\n")
                append("🏢 *En Çok Destek Alan Müşteriler:*\n")
                topCustomers.forEach { (cust, count) ->
                    append("• $cust ($count çağrı)\n")
                }
            }
            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("_Telefon Destek Sistemi üzerinden otomatik üretilmiştir._")
        }
    }

    fun shareDailyShiftReportViaWhatsApp(
        context: Context,
        targetDateStr: String = todayDateString(),
        includeExcel: Boolean = false
    ) {
        val report = generateDailyShiftReport(targetDateStr)
        if (includeExcel) {
            viewModelScope.launch {
                val dateYmd = targetDateStr.trim().ifBlank { todayDateString() }
                val records = allRecords.value.filter { isRecordOnDate(it.baslangicTarihi, dateYmd) }
                val file = try {
                    ExcelExporter.exportToCacheFile(
                        context = context,
                        records = records,
                        filenamePrefix = "GunSonu_${dateYmd.replace("-", "").replace(".", "_")}"
                    )
                } catch (e: Exception) {
                    null
                }
                WhatsAppHelper.shareShiftReportWithOptionalExcel(context, report, file)
            }
        } else {
            WhatsAppHelper.shareShiftReport(context, report)
        }
    }

    fun copyDailyShiftReportToClipboard(context: Context, targetDateStr: String = todayDateString()) {
        val report = generateDailyShiftReport(targetDateStr)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("Günlük Servis Raporu", report)
        clipboard?.setPrimaryClip(clip)
        viewModelScope.launch { _uiEvent.emit("📋 Gün sonu raporu panoya kopyalandı!") }
    }

    fun shareDailyShiftReportGeneral(
        context: Context,
        targetDateStr: String = todayDateString(),
        includeExcel: Boolean = false
    ) {
        val report = generateDailyShiftReport(targetDateStr)
        val displayDate = formatYmdToDisplay(targetDateStr)
        if (includeExcel) {
            viewModelScope.launch {
                val dateYmd = targetDateStr.trim().ifBlank { todayDateString() }
                val records = allRecords.value.filter { isRecordOnDate(it.baslangicTarihi, dateYmd) }
                val file = try {
                    ExcelExporter.exportToCacheFile(
                        context = context,
                        records = records,
                        filenamePrefix = "GunSonu_${dateYmd.replace("-", "").replace(".", "_")}"
                    )
                } catch (e: Exception) {
                    null
                }
                if (file != null && file.exists()) {
                    val fileUri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        putExtra(Intent.EXTRA_STREAM, fileUri)
                        putExtra(Intent.EXTRA_SUBJECT, "Günlük Servis & Vardiya Raporu ($displayDate)")
                        putExtra(Intent.EXTRA_TEXT, report)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Gün Sonu Raporu ve Excel Dosyasını Paylaş"))
                } else {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Günlük Servis & Vardiya Raporu ($displayDate)")
                        putExtra(Intent.EXTRA_TEXT, report)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Gün Sonu Raporunu Paylaş"))
                }
            }
        } else {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Günlük Servis & Vardiya Raporu ($displayDate)")
                putExtra(Intent.EXTRA_TEXT, report)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(sendIntent, "Gün Sonu Raporunu Paylaş"))
        }
    }

    fun exportAndShareDailyExcel(context: Context, targetDateStr: String = todayDateString()) {
        viewModelScope.launch {
            val dateYmd = targetDateStr.trim().ifBlank { todayDateString() }
            val displayDate = formatYmdToDisplay(dateYmd)
            val records = allRecords.value.filter { isRecordOnDate(it.baslangicTarihi, dateYmd) }
            if (records.isEmpty()) {
                _uiEvent.emit("$displayDate tarihine ait aktarılacak çağrı kaydı bulunamadı!")
                return@launch
            }
            try {
                val file = ExcelExporter.exportToCacheFile(
                    context = context,
                    records = records,
                    filenamePrefix = "GunSonu_${dateYmd.replace("-", "").replace(".", "_")}"
                )
                _uiEvent.emit("$displayDate tarihine ait ${records.size} adet kayıt Excel (.xlsx) olarak hazırlandı!")
                ExcelExporter.shareExcelFile(context, file)
            } catch (e: Exception) {
                _uiEvent.emit("Excel oluşturma hatası: ${e.message}")
            }
        }
    }

    // Live In-Call Scratchpad (Hızlı Karalama Notu)
    private val _scratchpadNote = MutableStateFlow("")
    val scratchpadNote: StateFlow<String> = _scratchpadNote.asStateFlow()

    // Customer Previous Support Records (Bu müşterinin önceki çağrıları)
    private val _customerPreviousRecords = MutableStateFlow<List<SupportRecord>>(emptyList())
    val customerPreviousRecords: StateFlow<List<SupportRecord>> = _customerPreviousRecords.asStateFlow()

    // Snackbar notifications
    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent: SharedFlow<String> = _uiEvent.asSharedFlow()

    init {
        // Initialize form with active representative name and code
        _formState.value = SupportFormState(
            kullanici = _globalKullanici.value,
            kullaniciKodu = _globalKullaniciKodu.value
        )

        val database = SupportDatabase.getInstance(application)
        repository = SupportRepository(database.supportDao(), database.templateDao(), database.missedCallDao())

        allRecords = repository.allRecords.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        supportDailyStats = allRecords.map { list ->
            calculateDailyStats(list)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SupportDailyStats())

        allMissedCalls = repository.allMissedCalls.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        unsyncedCount = repository.unsyncedCount.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

        tespitTemplates = repository.getTemplatesByType("TESPIT").stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        islemTemplates = repository.getTemplatesByType("ISLEM").stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        konuTemplates = repository.getTemplatesByType("KONU").stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        prefixTemplates = repository.getTemplatesByType("PREFIX").stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Seed default prefixes if empty (PC, POS, NB, SRV, TRM)
        viewModelScope.launch {
            kotlinx.coroutines.delay(200)
            val currentPrefixes = repository.getTemplatesByType("PREFIX").firstOrNull() ?: emptyList()
            if (currentPrefixes.isEmpty()) {
                listOf("PC", "POS", "NB", "SRV", "TRM").forEach { pfx ->
                    repository.addTemplate("PREFIX", pfx)
                }
            }
        }

        // Dynamic popular topics from historical usage
        popularTopics = combine(allRecords, konuTemplates) { records, templates ->
            val counts = records.map { it.destekKonusu1.trim() }.filter { it.isNotBlank() }
                .groupingBy { it }.eachCount()
            val fromRecords = counts.entries.sortedByDescending { it.value }.map { it.key }
            val fromTemplates = templates.map { it.text.trim() }.filter { it.isNotBlank() }
            (fromRecords + fromTemplates).distinct().take(15)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Dynamic popular tespit from historical usage
        popularTespit = combine(allRecords, tespitTemplates) { records, templates ->
            val counts = records.flatMap { it.tespit1.split(", ").map { p -> p.trim() } }.filter { it.isNotBlank() }
                .groupingBy { it }.eachCount()
            val fromRecords = counts.entries.sortedByDescending { it.value }.map { it.key }
            val fromTemplates = templates.map { it.text.trim() }.filter { it.isNotBlank() }
            (fromRecords + fromTemplates).distinct().take(15)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Dynamic popular islem from historical usage
        popularIslem = combine(allRecords, islemTemplates) { records, templates ->
            val counts = records.flatMap { it.islem1.split(", ").map { p -> p.trim() } }.filter { it.isNotBlank() }
                .groupingBy { it }.eachCount()
            val fromRecords = counts.entries.sortedByDescending { it.value }.map { it.key }
            val fromTemplates = templates.map { it.text.trim() }.filter { it.isNotBlank() }
            (fromRecords + fromTemplates).distinct().take(15)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Ana ekran hazır şablonları: En çok kullanılanlar en başta olacak şekilde dinamik sıralama
        quickScenarios = combine(_quickScenarios, allRecords) { scenarios, records ->
            scenarios.sortedByDescending { sc ->
                val recordMatches = records.count { rec ->
                    (sc.konu.isNotBlank() && rec.destekKonusu1.contains(sc.konu, ignoreCase = true)) ||
                    (sc.tespit.isNotBlank() && rec.tespit1.contains(sc.tespit, ignoreCase = true)) ||
                    (sc.islem.isNotBlank() && rec.islem1.contains(sc.islem, ignoreCase = true))
                }
                (sc.usageCount * 10) + recordMatches
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), loadSavedQuickScenarios())

        viewModelScope.launch {
            repository.seedDefaultTemplatesIfEmpty()
        }

        // Sync master power state
        verimorSipManager.setMasterPower(_masterPowerEnabled.value)

        // Persistent SIP Connection: Reconnect on app startup ONLY IF master power is ON
        val shouldAutoConnectSip = sharedPrefs.getBoolean("verimor_auto_connect", true)
        val domain = _verimorDomain.value.trim()
        val ext = _verimorExtension.value.trim()
        if (_masterPowerEnabled.value && shouldAutoConnectSip && domain.isNotBlank() && ext.isNotBlank()) {
            connectVerimorSip(isAuto = true)
        }

        // Listen for incoming Verimor SIP call events
        viewModelScope.launch {
            verimorSipManager.callEvents.collect { callEvent ->
                handleIncomingVerimorCall(callEvent)
            }
        }

        // Listen for call events (missed and answered) from VerimorSipManager - SINGLE SOURCE OF TRUTH
        viewModelScope.launch {
            verimorSipManager.missedCallEvents.collect { callRecord ->
                val id = repository.insertMissedCall(callRecord)
                if (callRecord.callType == "MISSED" && !callRecord.isHandled && _dutyModeEnabled.value) {
                    sendMissedCallNotification(callRecord.copy(id = id))
                }
            }
        }

        // Açılışta varsa geçmiş mükerrer çağrı kopyalarını temizle
        viewModelScope.launch {
            repository.cleanDuplicateCalls()
        }

        // Listen for SIP state errors to notify user immediately via snackbar
        viewModelScope.launch {
            verimorSipManager.connectionState.collect { state ->
                if (state is com.example.sip.VerimorSipState.Error) {
                    _uiEvent.emit("Verimor SIP: ${state.message}")
                }
            }
        }

        // Listen for call termination (Arayan kişi açılsa da açılmasa da kapattığında):
        // Formu ASLA sıfırlama ve otomatik kaydetme YAPMA! Sadece kronometreyi durdur, arama panelini kapat ve form verilerini koruyarak bekle.
        viewModelScope.launch {
            verimorSipManager.connectionState.collect { state ->
                when (state) {
                    is com.example.sip.VerimorSipState.Registered,
                    is com.example.sip.VerimorSipState.Disconnected,
                    is com.example.sip.VerimorSipState.Error -> {
                        _activeIncomingCall.value = null
                        if (_isTimerRunning.value) {
                            stopTimer()
                        }
                    }
                    else -> {}
                }
            }
        }

        // Müşterinin Önceki Çağrıları Dinleyicisi
        viewModelScope.launch {
            combine(_formState, allRecords) { form, _ ->
                val digits = form.sicilDigits.trim()
                val phone = form.telefon.trim()
                val editId = form.editingId
                if (digits.length >= 3 || phone.length >= 4) {
                    val fullSicil = if (digits.isNotBlank()) "${form.sicilPrefix}$digits" else ""
                    repository.searchRecords(sicilNo = fullSicil, telefon = phone, gorusulen = "")
                        .filter { r -> editId == null || r.id != editId }
                        .take(5)
                } else {
                    emptyList()
                }
            }.collect { list ->
                _customerPreviousRecords.value = list
            }
        }

        // Güvenlik: Uygulama açılışında otomatik tekilleştirme, günlük yerel yedekleme & depolama analizi
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                repository.removeDuplicateRecords()
                com.example.backup.BackupManager.performDailyAutoBackup(application, repository)
                refreshStorageBreakdown(application)
            } catch (_: Exception) {}
        }
    }

    private fun calculateDailyStats(records: List<SupportRecord>): SupportDailyStats {
        val todayStr = todayDateString()
        val todayRecords = records.filter { it.baslangicTarihi.take(10) == todayStr }
        if (todayRecords.isEmpty()) {
            return SupportDailyStats()
        }
        val todayCalls = todayRecords.size
        var totalSeconds = 0L
        for (r in todayRecords) {
            val s = r.toplamSure.trim()
            val parts = s.split(":")
            if (parts.size == 2) {
                val m = parts[0].toLongOrNull() ?: 0L
                val sec = parts[1].toLongOrNull() ?: 0L
                totalSeconds += (m * 60 + sec)
            } else if (parts.size == 3) {
                val h = parts[0].toLongOrNull() ?: 0L
                val m = parts[1].toLongOrNull() ?: 0L
                val sec = parts[2].toLongOrNull() ?: 0L
                totalSeconds += (h * 3600 + m * 60 + sec)
            }
        }
        val avgSeconds = if (todayCalls > 0) totalSeconds / todayCalls else 0L
        val resolved = todayRecords.count { it.bekleyenIs.equals("Hayır", ignoreCase = true) || it.bekleyenIs.isBlank() }
        val pending = todayRecords.count { it.bekleyenIs.equals("Evet", ignoreCase = true) }
        val topTopic = todayRecords.map { it.destekKonusu1.trim() }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }?.key ?: "-"

        return SupportDailyStats(
            todayCallsCount = todayCalls,
            todayTotalDurationSeconds = totalSeconds,
            todayAvgDurationSeconds = avgSeconds,
            todayResolvedCount = resolved,
            todayPendingCount = pending,
            topTopic = topTopic
        )
    }

    // --- HIZLI ÇÖZÜM SENARYOLARI (QUICK SOLUTION SCENARIOS) ---
    fun applyQuickScenario(scenario: QuickScenario) {
        _formState.update { current ->
            current.copy(
                destekKonu = scenario.konu,
                tespit = scenario.tespit,
                islem = scenario.islem
            )
        }
        val updated = _quickScenarios.value.map {
            if (it.id == scenario.id) it.copy(usageCount = it.usageCount + 1) else it
        }
        persistQuickScenarios(updated)
        viewModelScope.launch {
            _uiEvent.emit("⚡ Şablon Uygulandı: ${scenario.title}")
        }
    }

    // --- GÖRÜŞME İÇİ KARALAMA NOTU (LIVE SCRATCHPAD) ---
    fun updateScratchpadNote(text: String) {
        _scratchpadNote.value = text
    }

    fun appendScratchpadToTespit() {
        val note = _scratchpadNote.value.trim()
        if (note.isNotBlank()) {
            val cur = _formState.value.tespit
            val updated = if (cur.isBlank()) note else "$cur, $note"
            _formState.update { it.copy(tespit = updated) }
            viewModelScope.launch { _uiEvent.emit("Not Tespit Alanına Eklendi") }
        }
    }

    fun appendScratchpadToIslem() {
        val note = _scratchpadNote.value.trim()
        if (note.isNotBlank()) {
            val cur = _formState.value.islem
            val updated = if (cur.isBlank()) note else "$cur, $note"
            _formState.update { it.copy(islem = updated) }
            viewModelScope.launch { _uiEvent.emit("Not İşlem Alanına Eklendi") }
        }
    }

    fun clearScratchpad() {
        _scratchpadNote.value = ""
    }

    // --- MÜŞTERİNİN ÖNCEKİ ÇÖZÜMÜNÜ AKTARMA ---
    fun applyPreviousRecordSolution(record: SupportRecord) {
        _formState.update { current ->
            current.copy(
                destekKonu = record.destekKonusu1.ifBlank { current.destekKonu },
                tespit = record.tespit1.ifBlank { current.tespit },
                islem = record.islem1.ifBlank { current.islem }
            )
        }
        viewModelScope.launch {
            _uiEvent.emit("Önceki çağrı çözümü forma aktarıldı!")
        }
    }

    // --- WHATSAPP İLE SERVİS FİŞİ PAYLAŞIMI ---
    fun shareCurrentFormViaWhatsApp(context: Context) {
        val f = _formState.value
        val phone = f.telefon.ifBlank { f.telefon2 }
        val fullSicil = if (f.sicilDigits.isNotBlank()) "${f.sicilPrefix}${f.sicilDigits}" else f.sicilNo
        com.example.util.WhatsAppHelper.shareForm(
            context = context,
            phoneRaw = phone,
            firma = f.musteriBayi,
            sicilNo = fullSicil,
            gorusulen = f.gorusulen,
            destekKonusu = f.destekKonu,
            tespit = f.tespit,
            islem = f.islem,
            temsilci = _globalKullanici.value,
            tarih = f.basTarihi.ifBlank { todayDateString() },
            sure = f.sure
        )
    }

    fun shareRecordViaWhatsApp(context: Context, record: SupportRecord) {
        com.example.util.WhatsAppHelper.shareRecord(
            context = context,
            record = record,
            representativeName = _globalKullanici.value
        )
    }

    // --- EXCEL RAPORUNU OUTLOOK / E-POSTA İLE PAYLAŞMA ---
    fun exportAndShareExcelViaOutlook(context: Context) {
        viewModelScope.launch {
            val filtered = getFilteredRecordsForExport()
            if (filtered.isEmpty()) {
                _uiEvent.emit("Seçilen tarih aralığında aktarılacak kayıt bulunamadı!")
                return@launch
            }
            try {
                val file = ExcelExporter.exportToCacheFile(
                    context = context,
                    records = filtered,
                    filenamePrefix = "Destek_${_exportStartDate.value}_${_exportEndDate.value}"
                )
                closeExportDialog()
                _uiEvent.emit("${filtered.size} adet kayıt Excel (.xlsx) olarak oluşturuldu!")
                ExcelExporter.shareExcelViaOutlookOrEmail(context, file, _outlookRecipientEmail.value)
            } catch (e: Exception) {
                _uiEvent.emit("Excel e-posta hatası: ${e.message}")
            }
        }
    }

    fun setMasterPower(enabled: Boolean) {
        _masterPowerEnabled.value = enabled
        sharedPrefs.edit().putBoolean("master_power_enabled", enabled).apply()
        verimorSipManager.setMasterPower(enabled)
        if (!enabled) {
            VerimorSipService.stopService(getApplication())
            viewModelScope.launch {
                _uiEvent.emit("🛑 Sistem ve arka plan dinlemeleri durduruldu (OFF)")
            }
        } else {
            try {
                VerimorSipService.startService(getApplication())
            } catch (_: Exception) {}
            if (_verimorAutoConnect.value && _verimorDomain.value.isNotBlank() && _verimorExtension.value.isNotBlank()) {
                connectVerimorSip(isAuto = false)
            }
            viewModelScope.launch {
                _uiEvent.emit("⚡ Sistem ve arka plan dinlemeleri aktif edildi (ON)")
            }
        }
    }

    fun setDutyMode(enabled: Boolean) {
        _dutyModeEnabled.value = enabled
        sharedPrefs.edit().putBoolean("duty_mode_enabled", enabled).apply()
        viewModelScope.launch {
            if (enabled) {
                _uiEvent.emit("🌙 Nöbet / Mesai Dışı Modu Aktif (Kaçan çağrılar takip ediliyor)")
            } else {
                _uiEvent.emit("☀️ Gündüz Mesai Modu (Pil ve veri tasarrufu aktif)")
            }
        }
    }

    fun markMissedCallHandled(id: Long) {
        viewModelScope.launch {
            repository.markMissedCallHandled(id)
        }
    }

    fun loadPhoneToForm(phoneNumber: String, contactName: String = "") {
        updateTelefon(phoneNumber)
        if (contactName.isNotBlank()) {
            updateGorusulen(contactName)
        }
        setBottomNavTab(0)
        viewModelScope.launch {
            _uiEvent.emit("📞 $phoneNumber numarası forma aktarıldı.")
        }
    }

    fun startVerimorSipCall(phoneNumber: String, contactName: String = "") {
        val clean = phoneNumber.replace(Regex("[^0-9+]"), "").trim()
        if (clean.isBlank()) return

        if (!_masterPowerEnabled.value) {
            viewModelScope.launch {
                _uiEvent.emit("SIP kapalı. Lütfen üst bardaki Ana Güç Anahtarını (ON) açınız.")
            }
            return
        }

        val validName = if (contactName.any { it.isLetter() }) contactName.trim() else ""

        // Aranan numara arama geçmişine "Giden Arama" (Beyaz) olarak kaydedilir ve varsa kaçan çağrı durumu güncellenir
        val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR")).format(Date())
        val outRecord = MissedCallRecord(
            callerNumber = clean,
            callerName = validName,
            timestamp = System.currentTimeMillis(),
            dateFormatted = dateStr,
            reason = "Giden Arama",
            isHandled = true,
            callType = "OUTGOING",
            duration = ""
        )
        viewModelScope.launch {
            repository.markMissedCallHandledByNumber(clean)
            repository.insertMissedCall(outRecord)
        }

        // Requirement 1: Çağrıya geçilmek istendiği anda tek dokunuşla Ana Destek Ekranına kilitlen
        navigateToMainCallScreen()
        verimorSipManager.makeCall(clean, validName)
        openCallControlDialog()
    }

    fun deleteMissedCall(id: Long) {
        viewModelScope.launch {
            repository.deleteMissedCall(id)
        }
    }

    fun deleteAllMissedCalls() {
        viewModelScope.launch {
            repository.deleteAllMissedCalls()
            _uiEvent.emit("Tüm kaçan çağrılar temizlendi.")
        }
    }

    private fun sendMissedCallNotification(call: MissedCallRecord) {
        try {
            val notificationManager = getApplication<Application>().getSystemService(NotificationManager::class.java)
            val intent = Intent(getApplication(), MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                getApplication(),
                call.id.toInt().coerceAtLeast(100),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )
            val notif = NotificationCompat.Builder(getApplication(), VerimorSipService.CHANNEL_ID_CALLS)
                .setSmallIcon(android.R.drawable.stat_notify_missed_call)
                .setContentTitle("Kaçan Çağrı: ${call.callerNumber}")
                .setContentText("${if (call.callerName.isNotBlank()) "${call.callerName} " else ""}(${call.callerNumber}) numarası tarafından arandınız.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()
            notificationManager?.notify(call.id.toInt().coerceAtLeast(8000), notif)
        } catch (_: Exception) {}
    }

    // --- FORM FIELD UPDATES ---

    fun updateRedmine(value: String) = _formState.update { it.copy(redmine = value) }
    fun updateBasTarihi(value: String) = _formState.update { it.copy(basTarihi = value) }

    fun updateSicilPrefix(prefix: String) {
        val cleanPrefix = prefix.replace("-", "").replace("_", "").replace(" ", "").replace("\n", "").replace("\r", "").trim().uppercase(Locale("tr", "TR"))
        _formState.update { current ->
            val cleanDigits = current.sicilDigits.filter { it.isDigit() }
            val combined = if (cleanDigits.isBlank()) "" else "$cleanPrefix$cleanDigits"
            current.copy(sicilPrefix = cleanPrefix, sicilDigits = cleanDigits, sicilNo = combined)
        }
    }

    fun updateSicilDigits(digits: String) {
        val onlyDigits = digits.filter { it.isDigit() }
        _formState.update { current ->
            val pfx = current.sicilPrefix.ifBlank { "PC" }
            val cleanPrefix = pfx.replace("-", "").replace("_", "").replace(" ", "").replace("\n", "").replace("\r", "").trim().uppercase(Locale("tr", "TR"))
            val combined = if (onlyDigits.isBlank()) "" else "$cleanPrefix$onlyDigits"
            current.copy(sicilPrefix = cleanPrefix, sicilDigits = onlyDigits, sicilNo = combined)
        }
        val full = _formState.value.sicilNo
        if (full.isNotBlank()) {
            triggerSmartLookup("SICIL", full)
        }
    }

    /**
     * Kullanıcı inputtan çıktığında (Focus Lost) veya Enter tuşuna bastığında
     * girilen seri numarasını otomatik olarak başına 0 ekleyerek 8 basamağa tamamlar.
     * Örnek: '5' -> '00000005', '123' -> '00000123'.
     * Birleştirilmiş hali: 'PC00000005'
     */
    fun padSicilDigits() {
        _formState.update { current ->
            val rawDigits = current.sicilDigits.filter { it.isDigit() }
            if (rawDigits.isBlank()) {
                current.copy(sicilDigits = "", sicilNo = "")
            } else {
                val padded = rawDigits.padStart(8, '0')
                val pfx = current.sicilPrefix.ifBlank { "PC" }
                val cleanPrefix = pfx.replace("-", "").replace("_", "").replace(" ", "").replace("\n", "").replace("\r", "").trim().uppercase(Locale("tr", "TR"))
                val combined = "$cleanPrefix$padded"
                current.copy(sicilPrefix = cleanPrefix, sicilDigits = padded, sicilNo = combined)
            }
        }
        val full = _formState.value.sicilNo
        if (full.isNotBlank()) {
            triggerSmartLookup("SICIL", full)
        }
    }

    fun splitSicilPrefixAndDigits(input: String): Pair<String, String> {
        val trimmed = input.replace("\n", "").replace("\r", "").replace(" ", "").trim()
        val letters = trimmed.takeWhile { !it.isDigit() }.replace("-", "").replace("_", "").trim()
        val numbers = trimmed.dropWhile { !it.isDigit() }.filter { it.isDigit() }
        return Pair(letters, numbers)
    }

    fun updateSicilNo(value: String) {
        val upper = value.uppercase(Locale("tr", "TR"))
        val (p, d) = splitSicilPrefixAndDigits(upper)
        _formState.update { it.copy(sicilPrefix = p, sicilDigits = d, sicilNo = upper) }
        triggerSmartLookup("SICIL", upper)
    }

    fun addPrefix(prefix: String) {
        val clean = prefix.trim().uppercase(Locale("tr", "TR"))
        if (clean.isNotBlank()) {
            viewModelScope.launch {
                repository.addTemplate("PREFIX", clean)
                _uiEvent.emit("'$clean' sicil ön eki eklendi.")
            }
        }
    }

    fun deletePrefix(id: Long) {
        viewModelScope.launch {
            repository.deleteTemplate(id)
            _uiEvent.emit("Sicil ön eki silindi.")
        }
    }

    fun updateKullanici(value: String) = _formState.update { it.copy(kullanici = value) }
    fun updateKullaniciKodu(value: String) = _formState.update { it.copy(kullaniciKodu = value) }
    fun setFormUser(name: String, code: String) = _formState.update { it.copy(kullanici = name, kullaniciKodu = code) }
    fun updateArayan(value: String) = _formState.update { it.copy(arayan = value) }
    fun updateMusteriBayi(value: String) = _formState.update { it.copy(musteriBayi = value.uppercase(Locale("tr", "TR"))) }

    fun updateGorusulen(value: String) {
        val upper = value.uppercase(Locale("tr", "TR"))
        _formState.update { it.copy(gorusulen = upper) }
        triggerSmartLookup("GORUSULEN", upper)
    }

    fun updateTelefon(value: String) {
        _formState.update { it.copy(telefon = value) }
        triggerSmartLookup("TELEFON", value)
    }

    fun updateTelefon2(value: String) = _formState.update { it.copy(telefon2 = value) }
    fun updateMail(value: String) = _formState.update { it.copy(mail = value) }

    fun updateIletisimTuru(value: String) = _formState.update { it.copy(iletisimTuru = value) }
    fun updateDestekKonu(value: String) = _formState.update { it.copy(destekKonu = value.uppercase(Locale("tr", "TR"))) }
    fun updateTespit(value: String) = _formState.update { it.copy(tespit = value.uppercase(Locale("tr", "TR"))) }
    fun updateIslem(value: String) = _formState.update { it.copy(islem = value.uppercase(Locale("tr", "TR"))) }
    fun updateBagId(value: String) = _formState.update { it.copy(bagId = value.uppercase(Locale("tr", "TR"))) }
    fun updateBitTarihi(value: String) = _formState.update { it.copy(bitTarihi = value) }
    fun updateSure(value: String) = _formState.update { it.copy(sure = value) }
    fun updateBekleyen(value: String) = _formState.update { it.copy(bekleyen = value) }
    fun updateB2bKayit(value: String) = _formState.update { it.copy(b2bKayit = value) }
    fun updateArgeGidis(value: String) = _formState.update { it.copy(argeGidis = value) }
    fun updateArgeGidisNotu(value: String) = _formState.update { it.copy(argeGidisNotu = value) }
    fun updateArgeDonus(value: String) = _formState.update { it.copy(argeDonus = value) }
    fun updateArgeDonusNotu(value: String) = _formState.update { it.copy(argeDonusNotu = value) }

    // Device / Hardware version setters
    fun updateYnokcVersiyon(value: String) = _formState.update { it.copy(ynokcVersiyon = value) }
    fun updateDynCashVersiyon(value: String) = _formState.update { it.copy(dynCashVersiyon = value) }
    fun updateYn500Versiyon(value: String) = _formState.update { it.copy(yn500Versiyon = value) }
    fun updateArkaOfisVersiyon(value: String) = _formState.update { it.copy(arkaOfisVersiyon = value) }
    fun updateServisKodu(value: String) = _formState.update { it.copy(servisKodu = value) }

    // DTMF Tuş Takımı Yönlendirmeleri (*2202 aktarma vb.)
    fun sendDtmf(digit: Char) {
        verimorSipManager.sendDtmf(digit)
    }

    fun sendDtmfSequence(sequence: String) {
        verimorSipManager.sendDtmfSequence(sequence)
    }

    // --- AKILLI ÇOKLU KRİTER ARAMA & KONTROL MEKANİZMASI (SICIL, TELEFON, GÖRÜŞÜLEN) ---

    fun triggerSmartLookup(criterion: String, query: String, isExplicit: Boolean = false) {
        lookupJob?.cancel()
        lookupJob = viewModelScope.launch {
            if (!isExplicit) {
                delay(900L) // 900ms debounce: kullanıcının yazmayı bitirip duraksamasını bekle
            }

            val cleanQuery = query.trim()
            val checkVal = if (criterion == "TELEFON") {
                cleanQuery.replace(Regex("\\D"), "")
            } else {
                cleanQuery
            }

            // Requirement 3: "Seri No" veya form alanlarına yazı yazarken kullanıcı henüz yazısını bitirmeden (örn: sadece "pc" veya "00102" yazmışken) veritabanı anımsatması veya arama tetiklenmesin.
            if (!isExplicit && criterion == "SICIL") {
                val digits = checkVal.filter { it.isDigit() }
                if (digits.length < 3) {
                    // En az 3 hane rakam yazılmadan yazım esnasında arama tetiklenmesin
                    return@launch
                }
            }

            val minLength = when {
                isExplicit -> 1
                criterion == "TELEFON" -> 6
                criterion == "SICIL" -> 4
                else -> 3
            }

            if (checkVal.length < minLength) {
                if (_matchPickerState.value?.searchCriterion == criterion) {
                    _matchPickerState.value = null
                }
                return@launch
            }

            // 1. TAMAMEN OFFLINE (LOKAL) VERİTABANI ARAMASI
            // Online/SQL sorgulama pasife alınmıştır; tüm eşleşmeler yerel Room SQLite veritabanından çekilir.
            val localRecords: List<SupportRecord> = when (criterion) {
                "SICIL" -> repository.searchBySicilNo(checkVal)
                "TELEFON" -> repository.searchByPhone(checkVal)
                "GORUSULEN" -> repository.searchByGorusulen(checkVal)
                "MUSTERI" -> repository.searchByMusteriBayi(checkVal)
                else -> repository.searchAllFields(checkVal)
            }

            val localCandidates = localRecords.map { r ->
                CustomerMatchCandidate(
                    id = r.id,
                    source = "LOCAL",
                    musteriBayi = r.musteriBayiAdi.trim(),
                    sicilNo = r.sicilNo1.trim().ifBlank { r.yn500SicilNo1.trim() },
                    gorusulen = r.gorusulenKisi.trim(),
                    telefon = r.telefon.trim(),
                    telefon2 = r.telefon2.trim(),
                    mail = r.mail.trim(),
                    ynokcVersiyon = r.kasaVersiyon1.trim(),
                    dynCashVersiyon = r.dynCashVersiyon1.trim(),
                    yn500Versiyon = r.yn500KasaVersiyon1.trim(),
                    arkaOfisVersiyon = r.arkaOfisVersiyon1.trim(),
                    servisKodu = r.servisKodu.trim(),
                    sonTarih = r.baslangicTarihi.trim(),
                    sonKonu = r.destekKonusu1.trim()
                )
            }

            // Benzersiz Müşteri/Cihaz/Sicil Gruplaması
            val distinctCandidates = localCandidates
                .filter { it.musteriBayi.isNotBlank() || it.sicilNo.isNotBlank() || it.telefon.isNotBlank() }
                .groupBy {
                    val keySicil = it.sicilNo.uppercase(Locale("tr", "TR"))
                    val keyMusteri = it.musteriBayi.uppercase(Locale("tr", "TR"))
                    val keyTel = it.telefon.replace(Regex("\\D"), "")
                    "$keyMusteri|$keySicil|$keyTel"
                }
                .map { entry -> entry.value.first() }

            // Sonuç Yönetimi: ASLA OTOMATİK DOLDURMA YAPILMAZ (KULLANICI ONAYI ŞART)
            // 1 kayıt da bulunsa, birden fazla kayıt da bulunsa form alanları asla doğrudan dolmaz.
            // Her zaman kullanıcı onay / seçim dialogu açılarak kullanıcının kararına bırakılır.
            when {
                distinctCandidates.isEmpty() -> {
                    if (_matchPickerState.value?.searchCriterion == criterion) {
                        _matchPickerState.value = null
                    }
                    if (isExplicit) {
                        _uiEvent.emit("Lokal veritabanında '$cleanQuery' ile eşleşen kayıt bulunamadı.")
                    }
                }
                else -> {
                    // Kullanıcı onayı şart: 1 kayıt veya birden fazla kayıt için diyalog açılır
                    _matchPickerState.value = CustomerSearchResultState(
                        searchCriterion = criterion,
                        queryValue = query,
                        candidates = distinctCandidates,
                        isSearching = false
                    )
                }
            }
        }
    }

    fun triggerManualSearch(query: String) {
        val clean = query.trim()
        if (clean.isBlank()) return
        triggerSmartLookup("MANUAL", clean, isExplicit = true)
    }

    fun applyMatchCandidate(candidate: CustomerMatchCandidate, criterion: String = "MANUAL", isAuto: Boolean = false) {
        val trLocale = Locale("tr", "TR")
        lookupJob?.cancel()
        _formState.update { current ->
            val updatedMusteri = candidate.musteriBayi.uppercase(trLocale).ifBlank { current.musteriBayi }
            val updatedGorusulen = candidate.gorusulen.uppercase(trLocale).ifBlank { current.gorusulen }
            val updatedTelefon = candidate.telefon.ifBlank { current.telefon }
            val updatedTelefon2 = candidate.telefon2.ifBlank { current.telefon2 }
            val updatedMail = candidate.mail.ifBlank { current.mail }
            val updatedSicil = if (candidate.sicilNo.isNotBlank()) candidate.sicilNo.uppercase(trLocale) else current.sicilNo
            val updatedYnokc = candidate.ynokcVersiyon.ifBlank { current.ynokcVersiyon }
            val updatedDynCash = candidate.dynCashVersiyon.ifBlank { current.dynCashVersiyon }
            val updatedYn500 = candidate.yn500Versiyon.ifBlank { current.yn500Versiyon }
            val updatedArkaOfis = candidate.arkaOfisVersiyon.ifBlank { current.arkaOfisVersiyon }
            val updatedServisKodu = candidate.servisKodu.ifBlank { current.servisKodu }

            val (pfx, dgt) = splitSicilPrefixAndDigits(updatedSicil)
            val preservedEditingId = current.editingId
            current.copy(
                editingId = preservedEditingId,
                sicilPrefix = pfx,
                sicilDigits = dgt,
                musteriBayi = updatedMusteri,
                gorusulen = updatedGorusulen,
                telefon = updatedTelefon,
                telefon2 = updatedTelefon2,
                mail = updatedMail,
                sicilNo = updatedSicil,
                ynokcVersiyon = updatedYnokc,
                dynCashVersiyon = updatedDynCash,
                yn500Versiyon = updatedYn500,
                arkaOfisVersiyon = updatedArkaOfis,
                servisKodu = updatedServisKodu
            )
        }
        _matchPickerState.value = null
        _autoFillSuggestion.value = null
        viewModelScope.launch {
            val label = if (candidate.sicilNo.isNotBlank()) {
                "${candidate.musteriBayi} [Sicil: ${candidate.sicilNo}]"
            } else {
                "${candidate.musteriBayi} (${candidate.telefon})"
            }
            _uiEvent.emit("✓ $label onaylandı ve forma aktarıldı.")
        }
    }

    fun dismissMatchPicker() {
        _matchPickerState.value = null
    }

    fun applyAutoFillSuggestion() {
        val suggestion = _autoFillSuggestion.value ?: return
        applyMatchCandidate(
            CustomerMatchCandidate(
                musteriBayi = suggestion.musteriBayi,
                sicilNo = if (suggestion.matchedField == "TELEFON") "" else _formState.value.sicilNo,
                gorusulen = suggestion.gorusulen,
                telefon = suggestion.telefon,
                telefon2 = suggestion.telefon2,
                mail = suggestion.mail,
                ynokcVersiyon = suggestion.ynokcVersiyon,
                dynCashVersiyon = suggestion.dynCashVersiyon,
                yn500Versiyon = suggestion.yn500Versiyon,
                arkaOfisVersiyon = suggestion.arkaOfisVersiyon,
                servisKodu = suggestion.servisKodu
            ),
            criterion = suggestion.matchedField,
            isAuto = false
        )
    }

    fun dismissAutoFillSuggestion() {
        _autoFillSuggestion.value = null
    }

    // --- KRONOMETRE & BAŞLAT (START CALL) ---

    /**
     * Requirement 4:
     * "birinci ekrandaki ve ikinci ekrandaki tum kayıtlar baslat a bos gelsin baslat a basınca ekran da verıler temizlensin"
     * Clears all fields in Screen 1 & Screen 2, sets start time, resets stopwatch to 0 and starts timer.
     * Optionally sets initialPhone and initialGorusulen when triggered by an incoming Verimor SIP call.
     */
    fun startCallAndClear(initialPhone: String = "", initialGorusulen: String = "") {
        navigateToMainCallScreen()
        val nowFormatted = currentDateTimeString()
        // Clear all fields and set new start time, preserving active user and code
        _formState.value = SupportFormState(
            basTarihi = nowFormatted,
            sure = "00:00",
            kullanici = _formState.value.kullanici.ifBlank { _globalKullanici.value },
            kullaniciKodu = _formState.value.kullaniciKodu.ifBlank { _globalKullaniciKodu.value },
            telefon = initialPhone,
            gorusulen = initialGorusulen
        )
        _timerSeconds.value = 0L
        _isTimerRunning.value = true

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_isTimerRunning.value) {
                delay(1000)
                _timerSeconds.update { it + 1 }
                _formState.update { it.copy(sure = formatDuration(_timerSeconds.value)) }
            }
        }
    }

    // --- VERİMOR BULUT SANTRAL SIP KONTROL VE ÇAĞRI AKIŞI ---

    fun updateVerimorDomain(value: String) {
        val clean = value.trim()
        _verimorDomain.value = clean
        sharedPrefs.edit().putString("verimor_domain", clean).apply()
    }

    fun updateVerimorExtension(value: String) {
        val clean = value.trim()
        _verimorExtension.value = clean
        sharedPrefs.edit().putString("verimor_extension", clean).apply()
    }

    fun updateVerimorPassword(value: String) {
        val clean = value.trim()
        _verimorPassword.value = clean
        sharedPrefs.edit().putString("verimor_password", clean).apply()
    }

    fun updateVerimorPort(value: String) {
        val clean = value.trim()
        _verimorPort.value = clean
        sharedPrefs.edit().putString("verimor_port", clean).apply()
    }

    fun toggleVerimorAutoTimer() {
        val newVal = !_verimorAutoTimer.value
        _verimorAutoTimer.value = newVal
        sharedPrefs.edit().putBoolean("verimor_auto_timer", newVal).apply()
    }

    fun toggleVerimorAutoSearch() {
        val newVal = !_verimorAutoSearch.value
        _verimorAutoSearch.value = newVal
        sharedPrefs.edit().putBoolean("verimor_auto_search", newVal).apply()
    }

    fun connectVerimorSip(isAuto: Boolean = false) {
        val portInt = _verimorPort.value.trim().toIntOrNull() ?: 5060
        val domain = _verimorDomain.value.trim()
            .removePrefix("http://")
            .removePrefix("https://")
            .removePrefix("sip:")
            .removeSuffix("/")
        val ext = _verimorExtension.value.trim()
        val pass = _verimorPassword.value.trim()

        // User requested connection (or persistent auto-connect active)
        _verimorAutoConnect.value = true
        sharedPrefs.edit().putBoolean("verimor_auto_connect", true).apply()

        if (domain.isBlank() || ext.isBlank()) {
            if (!isAuto) {
                viewModelScope.launch {
                    _uiEvent.emit("Lütfen Santral Host ve Dahili numarasını giriniz.")
                }
            }
            return
        }

        val config = com.example.sip.VerimorSipConfig(
            domain = domain,
            extension = ext,
            password = pass,
            port = portInt,
            autoStartTimer = _verimorAutoTimer.value,
            autoSearchCustomer = _verimorAutoSearch.value
        )
        verimorSipManager.register(config)
        // Silent startup: Açılışta veya arka planda kullanıcıyı rahatsız edecek bildirim/toast atılmaz
    }

    fun disconnectVerimorSip() {
        // User explicitly disconnected: remember this preference so we don't auto-connect next time
        _verimorAutoConnect.value = false
        sharedPrefs.edit().putBoolean("verimor_auto_connect", false).apply()

        verimorSipManager.disconnect()
        viewModelScope.launch {
            _uiEvent.emit("Verimor SIP bağlantısı kesildi. (Otomatik bağlantı pasif)")
        }
    }

    fun simulateVerimorIncomingCall(testPhone: String = "1006") {
        verimorSipManager.simulateIncomingCall(testPhone, "Santral Test Çağrısı")
    }

    val isMuted = verimorSipManager.isMuted
    val isSpeaker = verimorSipManager.isSpeaker
    val isAudioStreaming = verimorSipManager.isAudioStreaming

    fun answerVerimorCall() {
        verimorSipManager.answerCall()
    }

    fun declineVerimorCall() {
        _activeIncomingCall.value = null
        verimorSipManager.declineCall()
    }

    fun hangupVerimorCall() {
        _activeIncomingCall.value = null
        verimorSipManager.hangupCall()
    }

    fun toggleSpeaker() {
        verimorSipManager.toggleSpeaker()
    }

    fun toggleMute() {
        verimorSipManager.toggleMute()
    }

    fun dismissIncomingCallBanner() {
        _activeIncomingCall.value = null
        verimorSipManager.hangupCall()
    }

    fun isFormActive(): Boolean {
        val f = _formState.value
        val hasData = f.musteriBayi.isNotBlank() ||
                f.sicilNo.isNotBlank() ||
                f.gorusulen.isNotBlank() ||
                f.telefon.isNotBlank() ||
                f.telefon2.isNotBlank() ||
                f.mail.isNotBlank() ||
                f.destekKonu.isNotBlank() ||
                f.tespit.isNotBlank() ||
                f.islem.isNotBlank() ||
                f.bagId.isNotBlank() ||
                f.redmine.isNotBlank() ||
                _timerSeconds.value > 0 ||
                f.editingId != null
        return hasData || _isTimerRunning.value || verimorSipManager.connectionState.value is com.example.sip.VerimorSipState.InCall
    }

    fun handleIncomingVerimorCall(callEvent: com.example.sip.VerimorCallEvent) {
        // Requirement 1: Telefon çaldığı anda kullanıcı hangi ekranda olursa olsun anında Ana Destek Ekranına kilitlen
        navigateToMainCallScreen()
        val cleanPhone = callEvent.callerNumber.replace(Regex("\\s+"), "").trim()
        val callerName = callEvent.callerName.trim()

        // 1. Session Protection (Form Veri Koruma Kuralı):
        // Kullanıcı önceki çağrının formunu doldururken/yazarken arka planda yeni bir arama gelirse,
        // form üzerindeki mevcut veriler ASLA silinmez veya üzerine yazılmaz.
        // Gelen yeni arama bekleyen çağrı havuzuna eklenir ve ekranda rozet olarak gösterilir.
        if (isFormActive()) {
            _waitingCallsQueue.update { current ->
                current + callEvent
            }
            viewModelScope.launch {
                _uiEvent.emit("⏳ Mevcut form korunuyor! Yeni arama ($cleanPhone) kuyruğa alındı (Bekleyen: ${_waitingCallsQueue.value.size})")
            }
            return
        }

        _activeIncomingCall.value = callEvent

        viewModelScope.launch {
            val matches = repository.searchByPhone(cleanPhone)
            val matchedRecord = matches.firstOrNull { 
                val k = it.gorusulenKisi.trim()
                k.isNotBlank() && k.any { ch -> ch.isLetter() } && !k.all { ch -> ch.isDigit() || ch == '+' || ch == ' ' || ch == '-' }
            }

            val isCallerNameRealPerson = callerName.isNotBlank() &&
                callerName != cleanPhone &&
                !callerName.equals("unknown", ignoreCase = true) &&
                !callerName.equals("anonymous", ignoreCase = true) &&
                callerName.any { it.isLetter() } &&
                !callerName.all { it.isDigit() || it == '+' || it == ' ' || it == '-' }

            // Rule 1: Santralden gelen numara sistemde kayıtlı bir kişi değilse "Görüşülen Kişi" alanı ASLA numara ile doldurulmaz, tamamen BOŞ başlar
            val resolvedName = when {
                matchedRecord != null -> matchedRecord.gorusulenKisi.trim()
                isCallerNameRealPerson -> callerName.trim()
                else -> ""
            }

            // 2. Form boşsa normal akışta başlat
            if (_verimorAutoTimer.value) {
                // Automatically triggers "Başlat (Yeni)" stopwatch & resets call duration, sets incoming phone number & contact name
                startCallAndClear(initialPhone = cleanPhone, initialGorusulen = resolvedName)
            } else {
                updateTelefon(cleanPhone)
                if (resolvedName.isNotBlank()) {
                    updateGorusulen(resolvedName)
                }
            }

            if (_verimorAutoSearch.value && cleanPhone.isNotBlank()) {
                // Automatically triggers local offline database search - strictly requires user confirmation dialog
                triggerSmartLookup("TELEFON", cleanPhone, isExplicit = true)
            }

            _uiEvent.emit("📞 Verimor Santral: $cleanPhone çağrısı yakalandı. Kronometre başlatıldı.")
        }
    }

    fun takeNextWaitingCall(force: Boolean = false) {
        val nextCall = _waitingCallsQueue.value.firstOrNull() ?: return
        _waitingCallsQueue.update { it.drop(1) }
        processNextWaitingCall(nextCall)
    }

    fun takeSpecificWaitingCall(callEvent: com.example.sip.VerimorCallEvent) {
        _waitingCallsQueue.update { list -> list.filter { it != callEvent } }
        processNextWaitingCall(callEvent)
    }

    fun removeWaitingCall(callEvent: com.example.sip.VerimorCallEvent) {
        _waitingCallsQueue.update { list -> list.filter { it != callEvent } }
        viewModelScope.launch {
            _uiEvent.emit("Bekleyen çağrı kuyruktan silindi: ${callEvent.callerNumber}")
        }
    }

    fun clearWaitingQueue() {
        val count = _waitingCallsQueue.value.size
        _waitingCallsQueue.value = emptyList()
        viewModelScope.launch {
            _uiEvent.emit("$count adet bekleyen çağrı kuyruktan temizlendi.")
        }
    }

    fun processNextWaitingCall(callEvent: com.example.sip.VerimorCallEvent) {
        navigateToMainCallScreen()
        val cleanPhone = callEvent.callerNumber.replace(Regex("\\s+"), "").trim()
        val callerName = callEvent.callerName.trim()
        _activeIncomingCall.value = callEvent

        viewModelScope.launch {
            val matches = repository.searchByPhone(cleanPhone)
            val matchedRecord = matches.firstOrNull { 
                val k = it.gorusulenKisi.trim()
                k.isNotBlank() && k.any { ch -> ch.isLetter() } && !k.all { ch -> ch.isDigit() || ch == '+' || ch == ' ' || ch == '-' }
            }

            val isCallerNameRealPerson = callerName.isNotBlank() &&
                callerName != cleanPhone &&
                !callerName.equals("unknown", ignoreCase = true) &&
                !callerName.equals("anonymous", ignoreCase = true) &&
                callerName.any { it.isLetter() } &&
                !callerName.all { it.isDigit() || it == '+' || it == ' ' || it == '-' }

            // Rule 1: Sistemde eşleşen kişi yoksa tamamen BOŞ başlar
            val resolvedName = when {
                matchedRecord != null -> matchedRecord.gorusulenKisi.trim()
                isCallerNameRealPerson -> callerName.trim()
                else -> ""
            }

            startCallAndClear(initialPhone = cleanPhone, initialGorusulen = resolvedName)
            if (_verimorAutoSearch.value && cleanPhone.isNotBlank()) {
                triggerSmartLookup("TELEFON", cleanPhone, isExplicit = true)
            }

            // CRITICAL: Bekleyen çağrıyı derhal sesli canlı görüşmeye bağla!
            val sipState = verimorSipManager.connectionState.value
            val isCallRinging = sipState is com.example.sip.VerimorSipState.Ringing
            val currentInCallNum = (sipState as? com.example.sip.VerimorSipState.InCall)?.callerNumber ?: ""

            if (isCallRinging) {
                // Santralde hat çalıyor -> Derhal yanıtla ve canlı ses akışını bağla
                answerVerimorCall()
                _uiEvent.emit("📞 Bekleyen çağrı ($cleanPhone) canlı görüşmeye bağlandı!")
            } else if (sipState is com.example.sip.VerimorSipState.InCall && currentInCallNum == cleanPhone) {
                _uiEvent.emit("📞 Bekleyen çağrı ($cleanPhone) canlı görüşmede aktif!")
            } else {
                if (sipState is com.example.sip.VerimorSipState.InCall) {
                    verimorSipManager.hangupCall()
                    delay(300)
                }
                if (callEvent.isSimulated) {
                    verimorSipManager.simulateIncomingCall(cleanPhone, resolvedName.ifBlank { callerName })
                    answerVerimorCall()
                    _uiEvent.emit("📞 Bekleyen simülasyon çağrısı ($cleanPhone) canlı görüşmeye alındı!")
                } else if (cleanPhone.isNotBlank()) {
                    startVerimorSipCall(cleanPhone, resolvedName.ifBlank { callerName })
                    _uiEvent.emit("📞 Bekleyen numara ($cleanPhone) aranarak canlı görüşmeye bağlandı!")
                }
            }
        }
    }

    fun startTimer() {
        if (_isTimerRunning.value) return
        _isTimerRunning.value = true

        if (_formState.value.basTarihi.isBlank()) {
            val nowFormatted = currentDateTimeString()
            _formState.update { it.copy(basTarihi = nowFormatted) }
        }

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_isTimerRunning.value) {
                delay(1000)
                _timerSeconds.update { it + 1 }
                _formState.update { it.copy(sure = formatDuration(_timerSeconds.value)) }
            }
        }
    }

    fun stopTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
        timerJob = null

        val endTimeFormatted = currentDateTimeString()
        val durationFormatted = formatDuration(_timerSeconds.value)
        _formState.update {
            it.copy(
                bitTarihi = endTimeFormatted,
                sure = durationFormatted
            )
        }
    }

    fun resetTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
        timerJob = null
        _timerSeconds.value = 0L
    }

    fun formatDuration(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }

    // --- RECORD SAVING & EDITING ---

    fun loadRecordForEditing(record: SupportRecord) {
        navigateToMainCallScreen()
        val (pfx, dgt) = splitSicilPrefixAndDigits(record.sicilNo1)
        _formState.value = SupportFormState(
            editingId = record.id,
            redmine = record.redmineId1,
            basTarihi = record.baslangicTarihi,
            sicilPrefix = pfx,
            sicilDigits = dgt,
            sicilNo = record.sicilNo1,
            kullanici = record.kullaniciAdi.ifBlank { record.kullaniciKodu },
            kullaniciKodu = record.kullaniciKodu,
            arayan = record.arayanTuru,
            musteriBayi = record.musteriBayiAdi,
            gorusulen = record.gorusulenKisi,
            telefon = record.telefon,
            telefon2 = record.telefon2,
            mail = record.mail,
            iletisimTuru = record.iletisimTuru,
            destekKonu = record.destekKonusu1,
            tespit = record.tespit1,
            islem = record.islem1,
            bagId = record.baglantiId1,
            bitTarihi = record.bitisTarihi,
            sure = record.toplamSure,
            bekleyen = record.bekleyenIs,
            b2bKayit = record.b2bKayit,
            argeGidis = record.argeGidis,
            argeGidisNotu = record.argeGidisNotu,
            argeDonus = record.argeDonus,
            argeDonusNotu = record.argeDonusNotu,
            ynokcVersiyon = record.kasaVersiyon1,
            dynCashVersiyon = record.dynCashVersiyon1,
            yn500Versiyon = record.yn500KasaVersiyon1,
            arkaOfisVersiyon = record.arkaOfisVersiyon1,
            servisKodu = record.servisKodu
        )
        // Close history modal if open
        _isHistoryModalOpen.value = false
        viewModelScope.launch {
            _uiEvent.emit("Kayıt düzenleme moduna alındı (#${record.id})")
        }
    }

    fun cancelEditing() {
        _formState.value = SupportFormState()
        resetTimer()
    }

    fun saveRecord() {
        viewModelScope.launch {
            var currentForm = _formState.value

            // If timer was running, stop it and calculate end time & duration
            if (_isTimerRunning.value) {
                stopTimer()
                currentForm = _formState.value
            }

            // Fallbacks for timestamps if left blank
            val bas = if (currentForm.basTarihi.isNotBlank()) currentForm.basTarihi else currentDateTimeString()
            val bit = if (currentForm.bitTarihi.isNotBlank()) currentForm.bitTarihi else currentDateTimeString()
            val duration = if (currentForm.sure.isNotBlank()) currentForm.sure else {
                if (_timerSeconds.value > 0) formatDuration(_timerSeconds.value) else "00:01"
            }

            // Determine user: Use form values if specified, fallback to global settings
            val codeToSave = currentForm.kullaniciKodu.trim().ifBlank { _globalKullaniciKodu.value.trim() }
            val nameToSave = currentForm.kullanici.trim().ifBlank { _globalKullanici.value.trim() }
            val finalCode = codeToSave.ifBlank { nameToSave }
            val finalName = nameToSave.ifBlank { codeToSave }
            val cleanPhone = currentForm.telefon.replace(Regex("\\s+"), "").replace("-", "").replace("(", "").replace(")", "").trim()
            val cleanPhone2 = currentForm.telefon2.replace(Regex("\\s+"), "").replace("-", "").replace("(", "").replace(")", "").trim()
            val trLocale = Locale("tr", "TR")

            val isLiveCall = _activeIncomingCall.value != null || verimorState.value is com.example.sip.VerimorSipState.InCall
            
            // Ön Ek ile Seri Numarasını Tek Parça Halinde Birleştir & 8 Haneye Sıfırla Tamamla (Örn: "PC00000005" / "PC00100313"):
            // Asla alt alta bölünmesin, aralarında boşluk, tire veya satır kırılması olmasın.
            val rawDigits = currentForm.sicilDigits.filter { it.isDigit() }
            val pfx = currentForm.sicilPrefix.ifBlank { "PC" }
            val rawPrefix = pfx.replace("-", "").replace("_", "").replace(" ", "").replace("\n", "").replace("\r", "").trim().uppercase(trLocale)
            val cleanSicil = if (rawDigits.isNotBlank()) {
                val padded = rawDigits.padStart(8, '0')
                "$rawPrefix$padded" // 8 haneli tek parça bitişik metin (Örnek: "PC00000005")
            } else {
                val rawFull = currentForm.sicilNo.replace("\n", "").replace("\r", "").replace(" ", "").replace("-", "").replace("_", "").trim().uppercase(trLocale)
                val (extractedPfx, extractedDigits) = splitSicilPrefixAndDigits(rawFull)
                if (extractedDigits.isNotBlank()) {
                    val padded = extractedDigits.padStart(8, '0')
                    val finalPfx = extractedPfx.ifBlank { rawPrefix }
                    "$finalPfx$padded"
                } else {
                    "" // Tamamen boş! Ön ek tek başına asla kaydedilmez/gönderilmez.
                }
            }

            // Requirement 3: Mevcut Kaydı Güncelleme (Yeni Kayıt Açmayı Engelle - UPSERT)
            // Var olan bir kayıt düzenlenip tekrar kaydedildiğinde / aktarıldığında yeni kayıt olarak
            // KESİNLİKLE EKLENMEZ. Mevcut ID üzerinden güncellenir.
            var targetId: Long? = null
            if (currentForm.editingId != null && currentForm.editingId > 0) {
                targetId = currentForm.editingId
            } else if (cleanSicil.isNotBlank() && !isLiveCall) {
                // SQL Upsert: Aynı sicil numarası zaten varsa mükerrer açma, eski kaydı güncelle
                val matchedBySicil = repository.searchBySicilNo(cleanSicil).firstOrNull {
                    it.sicilNo1.replace("-", "").replace(" ", "").equals(cleanSicil, ignoreCase = true)
                }
                if (matchedBySicil != null) {
                    targetId = matchedBySicil.id
                }
            }

            // Requirement 4: Tespit ve İşlem alanları için otomatik büyük harf dönüşümü
            val cleanTespit = currentForm.tespit.trim().uppercase(trLocale)
            val cleanIslem = currentForm.islem.trim().uppercase(trLocale)
            val cleanDestekKonu = currentForm.destekKonu.trim().uppercase(trLocale)

            val record = SupportRecord(
                id = targetId ?: 0,
                kullaniciKodu = finalCode,
                kullaniciAdi = finalName,
                baslangicTarihi = bas.trim(),
                arayanTuru = currentForm.arayan.trim(),
                musteriBayiAdi = currentForm.musteriBayi.trim().uppercase(trLocale),
                gorusulenKisi = currentForm.gorusulen.trim().uppercase(trLocale),
                telefon = cleanPhone,
                telefon2 = cleanPhone2,
                mail = currentForm.mail.trim(),
                iletisimTuru = currentForm.iletisimTuru.trim(),
                destekKonusu1 = cleanDestekKonu,
                sicilNo1 = cleanSicil,
                tespit1 = cleanTespit,
                islem1 = cleanIslem,
                baglantiId1 = currentForm.bagId.trim(),
                bitisTarihi = bit.trim(),
                toplamSure = duration.trim(),
                kasaVersiyon1 = currentForm.ynokcVersiyon.trim(),
                arkaOfisVersiyon1 = currentForm.arkaOfisVersiyon.trim(),
                redmineId1 = currentForm.redmine.trim(),
                aktif = "1",
                b2bKayit = currentForm.b2bKayit.trim(),
                bekleyenIs = currentForm.bekleyen.trim(),
                argeGidis = currentForm.argeGidis.trim(),
                argeDonus = currentForm.argeDonus.trim(),
                argeGidisNotu = currentForm.argeGidisNotu.trim(),
                argeDonusNotu = currentForm.argeDonusNotu.trim(),
                resim = "",
                tckn = "",
                vergiNo = "",
                tespitKodu1 = "",
                islemKodu1 = "",
                yn500SicilNo1 = cleanSicil,
                dynCashVersiyon1 = currentForm.dynCashVersiyon.trim(),
                arkaOfisId1 = "",
                yn500KasaVersiyon1 = currentForm.yn500Versiyon.trim(),
                servisKodu = currentForm.servisKodu.trim(),
                not1 = currentForm.servisKodu.trim(),
                isSynced = false
            )

            if (targetId != null && targetId > 0) {
                repository.update(record)
                _uiEvent.emit("Destek çağrı kaydı (#$targetId) güncellendi (Mükerrer önlendi)!")
            } else {
                repository.insert(record)
                _uiEvent.emit("Destek çağrı kaydı başarıyla kaydedildi!")
            }

            // Clear form for next call, preserving active user and code
            _formState.value = SupportFormState(
                kullanici = _globalKullanici.value,
                kullaniciKodu = _globalKullaniciKodu.value
            )
            resetTimer()

            // Requirement 1: "Kullanıcı 'Kaydet & Bitir' ile işini bitirdikten sonra sistem sıradaki bekleyen çağrıyı işleme alsın."
            if (_waitingCallsQueue.value.isNotEmpty()) {
                val nextCall = _waitingCallsQueue.value.first()
                _waitingCallsQueue.update { it.drop(1) }
                processNextWaitingCall(nextCall)
            }
        }
    }

    fun deleteRecord(record: SupportRecord) {
        viewModelScope.launch {
            repository.delete(record)
            _uiEvent.emit("Kayıt silindi.")
        }
    }

    fun clearForm() {
        _formState.value = SupportFormState(
            kullanici = _globalKullanici.value,
            kullaniciKodu = _globalKullaniciKodu.value
        )
        resetTimer()
    }

    // --- TEMPLATES MANAGEMENT (SETTINGS) ---

    fun addTemplate(type: String, text: String) {
        viewModelScope.launch {
            if (text.isBlank()) {
                _uiEvent.emit("Şablon metni boş olamaz!")
                return@launch
            }
            repository.insertTemplate(type, text)
            _uiEvent.emit("Şablon başarıyla eklendi.")
        }
    }

    fun deleteTemplate(template: com.example.model.SupportTemplate) {
        viewModelScope.launch {
            repository.deleteTemplate(template)
            _uiEvent.emit("Şablon silindi.")
        }
    }

    // --- SETTINGS MODAL & DATE-RANGE DELETION ---

    fun openSettings(initialTab: Int = -1) {
        _settingsInitialTab.value = initialTab
        _isSettingsOpen.value = true
    }

    fun closeSettings() {
        _isSettingsOpen.value = false
    }

    fun updateGlobalKullanici(name: String) {
        _globalKullanici.value = name
        sharedPrefs.edit().putString("global_kullanici", name).apply()
        _formState.update { current ->
            if (current.kullanici.isBlank()) current.copy(kullanici = name) else current
        }
    }

    fun updateGlobalKullaniciKodu(code: String) {
        _globalKullaniciKodu.value = code
        sharedPrefs.edit().putString("global_kullanici_kodu", code).apply()
        _formState.update { current ->
            if (current.kullaniciKodu.isBlank()) current.copy(kullaniciKodu = code) else current
        }
    }

    fun updateOutlookRecipientEmail(email: String) {
        val trimmed = email.trim()
        _outlookRecipientEmail.value = trimmed
        sharedPrefs.edit().putString("outlook_recipient_email", trimmed).apply()
    }

    fun setGlobalUser(name: String, code: String) {
        _globalKullanici.value = name
        _globalKullaniciKodu.value = code
        sharedPrefs.edit()
            .putString("global_kullanici", name)
            .putString("global_kullanici_kodu", code)
            .apply()
        _formState.update { it.copy(kullanici = name, kullaniciKodu = code) }
    }

    fun addRepresentative(name: String, code: String) {
        val cleanName = name.trim()
        val cleanCode = code.trim()
        if (cleanName.isBlank() && cleanCode.isBlank()) return
        val current = _representatives.value.toMutableList()
        current.removeAll { it.code.equals(cleanCode, ignoreCase = true) }
        current.add(RepresentativePreset(code = cleanCode, name = cleanName))
        persistRepresentatives(current)
        if (_globalKullanici.value.isBlank() && _globalKullaniciKodu.value.isBlank()) {
            setGlobalUser(cleanName, cleanCode)
        }
    }

    fun updateRepresentative(oldCode: String, newName: String, newCode: String) {
        val cleanName = newName.trim()
        val cleanCode = newCode.trim()
        val current = _representatives.value.toMutableList()
        val index = current.indexOfFirst { it.code == oldCode }
        if (index != -1) {
            current[index] = RepresentativePreset(code = cleanCode, name = cleanName)
            persistRepresentatives(current)
            if (_globalKullaniciKodu.value == oldCode) {
                setGlobalUser(cleanName, cleanCode)
            }
        }
    }

    fun deleteRepresentative(code: String) {
        val current = _representatives.value.toMutableList()
        current.removeAll { it.code == code }
        persistRepresentatives(current)
        if (_globalKullaniciKodu.value == code) {
            val next = current.firstOrNull()
            if (next != null) {
                setGlobalUser(next.name, next.code)
            } else {
                setGlobalUser("", "")
            }
        }
    }

    fun selectActiveRepresentative(preset: RepresentativePreset) {
        setGlobalUser(preset.name, preset.code)
        viewModelScope.launch {
            _uiEvent.emit("✓ Aktif Temsilci: ${preset.code} - ${preset.name} seçildi.")
        }
    }

    fun updateSqlDbType(type: String) {
        _sqlDbType.value = type
        sharedPrefs.edit().putString("sql_db_type", type).apply()
    }

    fun updateSqlHost(host: String) {
        _sqlHost.value = host
        sharedPrefs.edit().putString("sql_host", host.trim()).apply()
    }

    fun updateSqlPort(port: String) {
        _sqlPort.value = port
        sharedPrefs.edit().putString("sql_port", port.trim()).apply()
    }

    fun updateSqlDbName(name: String) {
        _sqlDbName.value = name
        sharedPrefs.edit().putString("sql_db_name", name.trim()).apply()
    }

    fun updateSqlUser(user: String) {
        _sqlUser.value = user
        sharedPrefs.edit().putString("sql_user", user.trim()).apply()
    }

    fun updateSqlPassword(pass: String) {
        _sqlPassword.value = pass
        sharedPrefs.edit().putString("sql_password", pass).apply()
    }

    fun saveDbSettings() {
        val trimmedHost = _sqlHost.value.trim()
        val trimmedPort = _sqlPort.value.trim()
        val trimmedDb = _sqlDbName.value.trim()
        val trimmedUser = _sqlUser.value.trim()

        sharedPrefs.edit()
            .putString("sql_db_type", _sqlDbType.value)
            .putString("sql_host", trimmedHost)
            .putString("sql_port", trimmedPort)
            .putString("sql_db_name", trimmedDb)
            .putString("sql_user", trimmedUser)
            .putString("sql_password", _sqlPassword.value)
            .apply()

        val displayUrl = formatDynamicBaseUrl(trimmedHost, trimmedPort)
        viewModelScope.launch {
            val msg = if (displayUrl.isNotBlank()) {
                "Bağlantı ayarları kaydedildi. Hedef: $displayUrl"
            } else {
                "Bağlantı ayarları yerel hafızaya kaydedildi."
            }
            _uiEvent.emit(msg)
        }
    }

    fun testDbConnection() {
        if (_isTestingConnection.value) return
        val baseUrl = getDynamicBaseUrl()
        val displayUrl = formatDynamicBaseUrl(_sqlHost.value, _sqlPort.value)
        if (baseUrl.isBlank()) {
            _connectionTestResult.value = "✗ Hata: Lütfen Sunucu IP / Host adresini girin."
            viewModelScope.launch { _uiEvent.emit("Lütfen Sunucu IP / Host adresini girin.") }
            return
        }

        viewModelScope.launch {
            _isTestingConnection.value = true
            _connectionTestResult.value = null
            try {
                val apiService = com.example.network.SyncApiClient.getService(baseUrl)
                val testReq = com.example.network.TestDbConnectionRequest(
                    dbType = _sqlDbType.value,
                    host = _sqlHost.value.trim(),
                    port = _sqlPort.value.trim(),
                    dbName = _sqlDbName.value.trim(),
                    user = _sqlUser.value.trim(),
                    password = _sqlPassword.value
                )
                var caughtError: Throwable? = null
                val response = try {
                    apiService.testDbConnection(testReq)
                } catch (t: Throwable) {
                    caughtError = t
                    null
                }

                if (response != null && response.isSuccessful && response.body()?.success == true) {
                    val msg = response.body()?.message ?: "Veritabanı bağlantısı başarılı! (${_sqlDbType.value} / ${_sqlDbName.value})"
                    _connectionTestResult.value = "✓ $msg"
                    _uiEvent.emit("Bağlantı Başarılı: $msg")
                } else if (response != null) {
                    val errBody = try { response.errorBody()?.string() } catch (e: Exception) { null }
                    val err = response.body()?.message ?: errBody ?: "HTTP ${response.code()}: ${response.message()}"
                    _connectionTestResult.value = "✗ Hata: $err"
                    _uiEvent.emit("Bağlantı Hatası: $err")
                } else {
                    val realError = caughtError?.localizedMessage 
                        ?: caughtError?.message 
                        ?: caughtError?.cause?.message 
                        ?: caughtError?.toString() 
                        ?: "Bilinmeyen ağ bağlantı hatası"
                    _connectionTestResult.value = "✗ Hata: $realError"
                    _uiEvent.emit("Bağlantı Hatası: $realError")
                }
            } catch (e: Throwable) {
                val realMsg = e.localizedMessage ?: e.message ?: e.toString()
                _connectionTestResult.value = "✗ Hata: $realMsg"
                _uiEvent.emit("Hata: $realMsg")
            } finally {
                _isTestingConnection.value = false
            }
        }
    }

    // --- SQL SYNC LOGIC (POST /api/sync-calls) ---

    fun syncToSql() {
        if (_isSyncing.value) return
        val baseUrl = getDynamicBaseUrl()
        val displayUrl = formatDynamicBaseUrl(_sqlHost.value, _sqlPort.value)
        if (baseUrl.isBlank()) {
            viewModelScope.launch { _uiEvent.emit("Lütfen Ayarlar'dan Sunucu IP ve Port bilgilerini girin.") }
            return
        }

        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val unsyncedList = repository.getUnsyncedRecords()
                if (unsyncedList.isEmpty()) {
                    _uiEvent.emit("Senkronize edilecek bekleyen yeni kayıt bulunamadı.")
                    _isSyncing.value = false
                    return@launch
                }

                val syncItems = unsyncedList.map { it.toSyncCallItem() }
                val request = com.example.network.SyncCallsRequest(records = syncItems)

                val apiService = com.example.network.SyncApiClient.getService(baseUrl)
                var caughtError: Throwable? = null
                val response = try {
                    apiService.syncCalls(request)
                } catch (t: Throwable) {
                    caughtError = t
                    null
                }

                if (response != null && response.isSuccessful && response.body()?.success == true) {
                    val body = response.body()
                    val returnedSyncedIds = body?.syncedIds ?: unsyncedList.map { it.id }
                    repository.markAsSynced(returnedSyncedIds)
                    _uiEvent.emit("${returnedSyncedIds.size} adet kayıt merkezi SQL veritabanına aktarıldı!")
                } else {
                    val errorMsg = if (response != null) {
                        val errBody = try { response.errorBody()?.string() } catch (e: Exception) { null }
                        "Sunucu Hatası: ${response.code()} ${errBody ?: response.message()}"
                    } else {
                        val realError = caughtError?.localizedMessage 
                            ?: caughtError?.message 
                            ?: caughtError?.cause?.message 
                            ?: caughtError?.toString() 
                            ?: "Bilinmeyen ağ hatası"
                        "SQL Senkronizasyon Hatası ($displayUrl): $realError"
                    }
                    _uiEvent.emit(errorMsg)
                }
            } catch (e: Throwable) {
                val realMsg = e.localizedMessage ?: e.message ?: e.toString()
                _uiEvent.emit("SQL Senkronizasyon Hatası: $realMsg")
            } finally {
                _isSyncing.value = false
            }
        }
    }

    // --- SERVICE MANAGER / EXECUTIVE LOGIC & OTOMATİK HAFTALIK WİFİ SENKRONİZASYONU ---

    private val _lastSuccessfulSqlSyncTime = MutableStateFlow(sharedPrefs.getLong("last_successful_sql_sync_time", 0L))
    val lastSuccessfulSqlSyncTime: StateFlow<Long> = _lastSuccessfulSqlSyncTime.asStateFlow()

    private val _isAutoWeeklySyncEnabled = MutableStateFlow(sharedPrefs.getBoolean("auto_weekly_sql_sync", true))
    val isAutoWeeklySyncEnabled: StateFlow<Boolean> = _isAutoWeeklySyncEnabled.asStateFlow()

    fun updateAutoWeeklySyncEnabled(enabled: Boolean) {
        _isAutoWeeklySyncEnabled.value = enabled
        sharedPrefs.edit().putBoolean("auto_weekly_sql_sync", enabled).apply()
    }

    /**
     * Pazartesi günleri WiFi bağlı iken son 1 haftanın (veya 2 haftadır alınamadıysa son başarılı tarihten itibaren)
     * verisini SQL Server'dan otomatik çeker. Alınamazsa kullanıcıya uyarı bildirimi verir.
     */
    fun checkAndExecuteAutomaticWeeklySqlSync(context: Context, force: Boolean = false) {
        if (!_isAutoWeeklySyncEnabled.value && !force) return
        val baseUrl = getDynamicBaseUrl()
        if (baseUrl.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val isWifi = cm?.activeNetwork?.let { net ->
                cm.getNetworkCapabilities(net)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            } ?: false

            if (!isWifi && !force) {
                // WiFi bağlı değilken otomatik çekme yapıp mobil kotayı harcama
                return@launch
            }

            val cal = Calendar.getInstance()
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            val now = System.currentTimeMillis()
            val lastSync = _lastSuccessfulSqlSyncTime.value
            val daysSinceLastSync = if (lastSync > 0) (now - lastSync) / (1000 * 60 * 60 * 24) else 14

            val isMonday = dayOfWeek == Calendar.MONDAY
            val isOverdue = daysSinceLastSync >= 7

            if (!force && !isMonday && !isOverdue) {
                return@launch
            }

            // Son 12 saat içinde zaten başarılı bir senkronizasyon yapıldıysa tekrar etme
            if (!force && lastSync > 0 && (now - lastSync) < 12 * 60 * 60 * 1000L) {
                return@launch
            }

            // Başlangıç tarihi hesabı: En son başarılı alınan tarihten itibaren (örn. 2 hafta alınamadıysa 2 haftayı dener)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayStr = sdf.format(Date(now))
            val startDateStr = if (lastSync > 0) {
                val startCal = Calendar.getInstance().apply { timeInMillis = lastSync }
                sdf.format(startCal.time)
            } else {
                // Hiç senkronize edilmediyse son 14 gün
                val startCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -14) }
                sdf.format(startCal.time)
            }

            fetchCallsFromSql(
                startDate = startDateStr,
                endDate = todayStr,
                isAutoSync = true
            )
        }
    }

    fun fetchCallsFromSql(
        startDate: String = "",
        endDate: String = "",
        userCode: String = "",
        limit: Int = 2000,
        isAutoSync: Boolean = false
    ) {
        if (_isFetchingFromSql.value) return
        val baseUrl = getDynamicBaseUrl()
        val displayUrl = formatDynamicBaseUrl(_sqlHost.value, _sqlPort.value)
        if (baseUrl.isBlank()) {
            if (!isAutoSync) {
                viewModelScope.launch { _uiEvent.emit("Lütfen Ayarlar'dan Sunucu IP ve Port bilgilerini girin.") }
            }
            return
        }

        viewModelScope.launch {
            _isFetchingFromSql.value = true
            try {
                val apiService = com.example.network.SyncApiClient.getService(baseUrl)
                val request = com.example.network.FetchCallsRequest(
                    startDate = startDate.trim(),
                    endDate = endDate.trim(),
                    userCode = userCode.trim(),
                    limit = limit
                )

                var rawRecords = mutableListOf<SupportRecord>()
                var lastHttpCode = 0
                var lastErrorMessage = ""

                // 1. Deneme: POST /api/fetch-calls (Moshi Adapter)
                try {
                    val resp = apiService.fetchCalls(request)
                    lastHttpCode = resp.code()
                    if (resp.isSuccessful && resp.body()?.success == true) {
                        val items = resp.body()?.records.orEmpty()
                        rawRecords.addAll(items.map { it.toSupportRecord() })
                    }
                } catch (e: Exception) {
                    lastErrorMessage = e.localizedMessage ?: e.message ?: ""
                }

                // 2. Deneme: POST /api/fetch-calls (Raw Body / JSON Array / Flexible parser)
                if (rawRecords.isEmpty() && (lastHttpCode == 200 || lastHttpCode == 0)) {
                    try {
                        val rawResp = apiService.fetchCallsRaw(request)
                        if (rawResp.isSuccessful) {
                            val bodyStr = rawResp.body()?.string().orEmpty()
                            if (bodyStr.isNotBlank()) {
                                val parsed = parseRecordsFromRawJsonString(bodyStr)
                                rawRecords.addAll(parsed)
                            }
                        }
                    } catch (_: Exception) {}
                }

                // 3. Deneme: POST /api/get-calls (Alternatif Endpoint)
                if (rawRecords.isEmpty() && lastHttpCode != 200) {
                    try {
                        val rawResp = apiService.getCallsRaw(request)
                        lastHttpCode = rawResp.code()
                        if (rawResp.isSuccessful) {
                            val bodyStr = rawResp.body()?.string().orEmpty()
                            if (bodyStr.isNotBlank()) {
                                val parsed = parseRecordsFromRawJsonString(bodyStr)
                                rawRecords.addAll(parsed)
                            }
                        }
                    } catch (_: Exception) {}
                }

                // 4. Deneme: GET /api/fetch-calls (Query Parametreleri ile)
                if (rawRecords.isEmpty() && lastHttpCode != 200) {
                    try {
                        val getResp = apiService.fetchCallsGet(
                            startDate = startDate.trim(),
                            endDate = endDate.trim(),
                            userCode = userCode.trim(),
                            limit = limit
                        )
                        lastHttpCode = getResp.code()
                        if (getResp.isSuccessful) {
                            val bodyStr = getResp.body()?.string().orEmpty()
                            if (bodyStr.isNotBlank()) {
                                val parsed = parseRecordsFromRawJsonString(bodyStr)
                                rawRecords.addAll(parsed)
                            }
                        }
                    } catch (_: Exception) {}
                }

                // 5. Deneme: GET /api/calls
                if (rawRecords.isEmpty() && lastHttpCode != 200) {
                    try {
                        val getResp = apiService.getCallsList(
                            startDate = startDate.trim(),
                            endDate = endDate.trim(),
                            userCode = userCode.trim(),
                            limit = limit
                        )
                        lastHttpCode = getResp.code()
                        if (getResp.isSuccessful) {
                            val bodyStr = getResp.body()?.string().orEmpty()
                            if (bodyStr.isNotBlank()) {
                                val parsed = parseRecordsFromRawJsonString(bodyStr)
                                rawRecords.addAll(parsed)
                            }
                        }
                    } catch (_: Exception) {}
                }

                // Sonuç Değerlendirmesi
                val filterDesc = if (startDate.isNotBlank() && endDate.isNotBlank()) " ($startDate - $endDate)" else ""

                if (rawRecords.isNotEmpty()) {
                    val insertedIds = repository.insertAll(rawRecords)
                    val nowMillis = System.currentTimeMillis()
                    _lastSuccessfulSqlSyncTime.value = nowMillis
                    sharedPrefs.edit().putLong("last_successful_sql_sync_time", nowMillis).apply()

                    if (isAutoSync) {
                        _uiEvent.emit("✓ Otomatik SQL Senkronizasyonu: WiFi üzerinden$filterDesc ${insertedIds.size} adet çağrı başarıyla çekildi!")
                    } else {
                        _uiEvent.emit("✓ SQL Server'dan$filterDesc ${insertedIds.size} adet çağrı başarıyla çekildi ve panele aktarıldı!")
                    }
                } else if (lastHttpCode == 200) {
                    val nowMillis = System.currentTimeMillis()
                    _lastSuccessfulSqlSyncTime.value = nowMillis
                    sharedPrefs.edit().putLong("last_successful_sql_sync_time", nowMillis).apply()

                    if (isAutoSync) {
                        _uiEvent.emit("✓ Otomatik SQL Kontrolü: Veritabanı güncel, yeni kayıt bulunmuyor.")
                    } else {
                        _uiEvent.emit("Merkezi SQL sunucuda belirtilen kriterde$filterDesc kayıt bulunamadı (Veritabanı güncel).")
                    }
                } else if (lastHttpCode == 404) {
                    val msg = "⚠️ Sunucuda /api/fetch-calls servisi bulunamadı (HTTP 404). Lütfen sunucu API kodunu güncelleyin."
                    _uiEvent.emit(if (isAutoSync) "⚠️ SQL Otomatik Senkronizasyon: Deneme yapıldı fakat veri alınamadı! (HTTP 404)" else msg)
                } else if (lastHttpCode > 0) {
                    val msg = "Sunucu Hatası (HTTP $lastHttpCode): $displayUrl adresinden veri çekilemedi."
                    _uiEvent.emit(if (isAutoSync) "⚠️ SQL Otomatik Senkronizasyon: Deneme yapıldı fakat veri alınamadı! (HTTP $lastHttpCode)" else msg)
                } else {
                    val realMsg = if (lastErrorMessage.isNotBlank()) lastErrorMessage else "Sunucuya bağlanılamadı"
                    if (isAutoSync) {
                        _uiEvent.emit("⚠️ SQL Otomatik Senkronizasyon: Deneme yapıldı fakat veri alınamadı! ($realMsg)")
                    } else {
                        _uiEvent.emit("SQL Veri Çekme Hatası ($displayUrl): $realMsg")
                    }
                }
            } catch (e: Throwable) {
                val realMsg = e.localizedMessage ?: e.message ?: e.toString()
                if (isAutoSync) {
                    _uiEvent.emit("⚠️ SQL Otomatik Senkronizasyon: Deneme yapıldı fakat veri alınamadı! ($realMsg)")
                } else {
                    _uiEvent.emit("SQL Veri Çekme Hatası: $realMsg")
                }
            } finally {
                _isFetchingFromSql.value = false
            }
        }
    }

    private fun parseRecordsFromRawJsonString(jsonStr: String): List<SupportRecord> {
        val list = mutableListOf<SupportRecord>()
        try {
            val trimmed = jsonStr.trim()
            if (trimmed.startsWith("{")) {
                val root = org.json.JSONObject(trimmed)
                val arr = when {
                    root.has("records") -> root.optJSONArray("records")
                    root.has("data") -> root.optJSONArray("data")
                    root.has("results") -> root.optJSONArray("results")
                    root.has("calls") -> root.optJSONArray("calls")
                    else -> null
                }
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        list.add(parseSingleRecordFromJson(obj))
                    }
                } else {
                    list.add(parseSingleRecordFromJson(root))
                }
            } else if (trimmed.startsWith("[")) {
                val arr = org.json.JSONArray(trimmed)
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    list.add(parseSingleRecordFromJson(obj))
                }
            }
        } catch (_: Exception) {}
        return list
    }

    // --- YEREL HAFIZA & ÖNBELLEK TEMİZLEME (ARKA OFİS / SQL SERVER ASLA ETKİLENMEZ) ---

    /**
     * Sadece SQL Server'dan çekilmiş / eşitlenmiş kayıtları telefon hafızasından siler.
     * Arka ofis / SQL Server veritabanından ASLA hiçbir şey silmez.
     */
    fun clearSyncedLocalRecords() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val count = repository.deleteSyncedRecords()
                _uiEvent.emit("✓ Telefon hafızası temizlendi: SQL ile eşitlenmiş $count adet yerel kayıt silindi. (Arka Ofis veritabanına dokunulmadı)")
            } catch (e: Exception) {
                _uiEvent.emit("Hafıza temizleme hatası: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    /**
     * Belirli günden eski eşitlenmiş yerel kayıtları temizler.
     */
    fun clearSyncedLocalRecordsOlderThan(daysAgo: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val count = repository.deleteSyncedRecordsOlderThan(daysAgo)
                _uiEvent.emit("✓ $daysAgo günden eski $count adet eşitlenmiş yerel kayıt telefon hafızasından temizlendi. (Arka Ofis korundu)")
            } catch (e: Exception) {
                _uiEvent.emit("Hafıza temizleme hatası: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    /**
     * Belirli tarih aralığındaki eşitlenmiş yerel kayıtları temizler.
     */
    fun clearSyncedLocalRecordsBetweenDates(startDate: String, endDate: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val count = repository.deleteSyncedRecordsBetweenDates(startDate, endDate)
                _uiEvent.emit("✓ $startDate - $endDate arasındaki $count adet eşitlenmiş yerel kayıt temizlendi. (Arka Ofis korundu)")
            } catch (e: Exception) {
                _uiEvent.emit("Hafıza temizleme hatası: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    /**
     * Geçici dosya ve önbellekleri (Cache, geçici Excel'ler) temizler.
     */
    fun clearAppTempCache(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val freedBytes = com.example.backup.BackupManager.clearTempCache(context)
            val mbStr = String.format(Locale.US, "%.2f MB", freedBytes / (1024.0 * 1024.0))
            _uiEvent.emit("✓ Geçici dosyalar ve önbellek temizlendi. Kazanılan alan: $mbStr")
        }
    }

    /**
     * Tüm yerel kayıtları telefon hafızasından sıfırlar. (Arka Ofis / SQL Server'a kesinlikle dokunulmaz).
     */
    fun clearAllLocalRecordsOnly() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val count = repository.getTotalCount()
                repository.deleteAll()
                _uiEvent.emit("✓ Telefon hafızasındaki $count yerel kayıt sıfırlandı. (Arka Ofis / SQL Server ana verileriniz KORUNDU)")
            } catch (e: Exception) {
                _uiEvent.emit("Hafıza temizleme hatası: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    fun importJsonRecords(jsonString: String) {
        viewModelScope.launch {
            try {
                val trimmed = jsonString.trim()
                if (trimmed.isBlank()) {
                    _uiEvent.emit("Hata: Yapıştırılan metin boş.")
                    return@launch
                }

                val recordsList = mutableListOf<SupportRecord>()

                if (trimmed.startsWith("{")) {
                    // Full backup package or single object
                    val jsonRoot = org.json.JSONObject(trimmed)
                    if (jsonRoot.has("records")) {
                        val arr = jsonRoot.getJSONArray("records")
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            recordsList.add(parseSingleRecordFromJson(obj))
                        }
                    } else {
                        recordsList.add(parseSingleRecordFromJson(jsonRoot))
                    }
                } else if (trimmed.startsWith("[")) {
                    val arr = org.json.JSONArray(trimmed)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        recordsList.add(parseSingleRecordFromJson(obj))
                    }
                } else {
                    _uiEvent.emit("Hata: Geçerli bir JSON formatı bulunamadı.")
                    return@launch
                }

                if (recordsList.isNotEmpty()) {
                    val insertedIds = repository.insertAll(recordsList)
                    _uiEvent.emit("✓ Dışarıdan ${insertedIds.size} adet çağrı kaydı başarıyla içeri aktarıldı!")
                } else {
                    _uiEvent.emit("JSON içinde aktarılacak çağrı kaydı bulunamadı.")
                }
            } catch (e: Exception) {
                _uiEvent.emit("İçeri aktarma hatası: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    private fun parseSingleRecordFromJson(r: org.json.JSONObject): SupportRecord {
        return SupportRecord(
            id = 0,
            kullaniciKodu = r.optString("kullaniciKodu", r.optString("KULLANICIKODU", "")),
            kullaniciAdi = r.optString("kullaniciAdi", r.optString("KULLANICIADI", "")),
            baslangicTarihi = r.optString("baslangicTarihi", r.optString("BASLANGICTARIHI", "")),
            arayanTuru = r.optString("arayanTuru", r.optString("ARAYANTURU", "Müşteri")),
            musteriBayiAdi = r.optString("musteriBayiAdi", r.optString("MUSTERIBAYIADI", "")),
            gorusulenKisi = r.optString("gorusulenKisi", r.optString("GORUSULENKISI", "")),
            telefon = r.optString("telefon", r.optString("TELEFON", "")),
            telefon2 = r.optString("telefon2", r.optString("TELEFON2", "")),
            mail = r.optString("mail", r.optString("MAIL", "")),
            iletisimTuru = r.optString("iletisimTuru", r.optString("ILETISIMTURU", "Telefon")),
            destekKonusu1 = r.optString("destekKonusu1", r.optString("DESTEKKONUSU1", "")),
            sicilNo1 = r.optString("sicilNo1", r.optString("SICILNO1", "")),
            tespit1 = r.optString("tespit1", r.optString("TESPIT1", "")),
            islem1 = r.optString("islem1", r.optString("ISLEM1", "")),
            baglantiId1 = r.optString("baglantiId1", r.optString("BAGLANTIID1", "")),
            bitisTarihi = r.optString("bitisTarihi", r.optString("BITISTARIHI", "")),
            toplamSure = r.optString("toplamSure", r.optString("TOPLAMSURE", "")),
            kasaVersiyon1 = r.optString("kasaVersiyon1", r.optString("KASAVERSIYON1", "")),
            arkaOfisVersiyon1 = r.optString("arkaOfisVersiyon1", r.optString("ARKAOFISVERSIYON1", "")),
            redmineId1 = r.optString("redmineId1", r.optString("REDMINEID1", "")),
            aktif = r.optString("aktif", r.optString("AKTIF", "1")),
            b2bKayit = r.optString("b2bKayit", r.optString("B2BKAYIT", "")),
            bekleyenIs = r.optString("bekleyenIs", r.optString("BEKLEYENIS", "Hayır")),
            argeGidis = r.optString("argeGidis", r.optString("ARGEGIDIS", "Hayır")),
            argeDonus = r.optString("argeDonus", r.optString("ARGEDONUS", "Hayır")),
            argeGidisNotu = r.optString("argeGidisNotu", r.optString("ARGEGIDISNOTU", "")),
            argeDonusNotu = r.optString("argeDonusNotu", r.optString("ARGEDONUSNOTU", "")),
            resim = r.optString("resim", r.optString("RESIM", "")),
            tckn = r.optString("tckn", r.optString("TCKN", "")),
            vergiNo = r.optString("vergiNo", r.optString("VERGINO", "")),
            tespitKodu1 = r.optString("tespitKodu1", r.optString("TESPITKODU1", "")),
            islemKodu1 = r.optString("islemKodu1", r.optString("ISLEMKODU1", "")),
            yn500SicilNo1 = r.optString("yn500SicilNo1", r.optString("YN500SICILNO1", "")),
            dynCashVersiyon1 = r.optString("dynCashVersiyon1", r.optString("DYNCASHVERSIYON1", "")),
            arkaOfisId1 = r.optString("arkaOfisId1", r.optString("ARKAOFISID1", "")),
            yn500KasaVersiyon1 = r.optString("yn500KasaVersiyon1", r.optString("YN500KASAVERSIYON1", "")),
            servisKodu = r.optString("servisKodu", r.optString("SERVISKODU", "")),
            not1 = r.optString("not1", r.optString("NOT1", "")),
            isSynced = true,
            syncedAt = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis()
        )
    }

    fun parseDurationToSeconds(durationStr: String): Long {
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

    fun formatSecondsToReadable(sec: Long): String {
        val h = sec / 3600
        val m = (sec % 3600) / 60
        val s = sec % 60
        return when {
            h > 0 -> "${h} sa ${m} dk ${s} sn"
            m > 0 -> "${m} dk ${s} sn"
            else -> "${s} sn"
        }
    }

    fun generateServiceManagerReport(records: List<SupportRecord>, periodTitle: String): String {
        val totalCalls = records.size
        val totalSeconds = records.sumOf { parseDurationToSeconds(it.toplamSure) }
        val avgSeconds = if (totalCalls > 0) totalSeconds / totalCalls else 0
        val resolvedCount = records.count { it.bekleyenIs.equals("Hayır", ignoreCase = true) || it.bekleyenIs.isBlank() }
        val pendingCount = totalCalls - resolvedCount
        val resolutionRate = if (totalCalls > 0) (resolvedCount * 100 / totalCalls) else 100

        // User breakdown with resolved representative names
        val userGroups = records.groupBy { resolveRepresentativeName(it.kullaniciAdi.ifBlank { it.kullaniciKodu }) }
            .toList()
            .sortedByDescending { it.second.size }

        // Top topics: Sorunlar için tespit1 alanı baz alınır (boşsa destekKonusu1)
        val topTopics = records.map { it.tespit1.trim().ifBlank { it.destekKonusu1.trim() }.ifBlank { "Belirtilmemiş" } }
            .filter { it.isNotBlank() && it != "Belirtilmemiş" }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(5)

        // En çok düzeltilen 3 konu / yapılan işlemler
        val topFixedActions = records.filter { it.islem1.isNotBlank() && !it.bekleyenIs.equals("Evet", ignoreCase = true) }
            .map { it.islem1.trim() }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(3)

        // En çok görüşen 3 personel
        val top3Staff = userGroups.take(3)

        // Top customers
        val topCustomers = records.map { it.musteriBayiAdi.ifBlank { "İsimsiz Firma" } }
            .filter { it.isNotBlank() && it != "İsimsiz Firma" }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(5)

        return buildString {
            append("👔 *SERVİS MÜDÜRÜ YÖNETİCİ RAPORU*\n")
            append("📅 *Dönem:* $periodTitle\n")
            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("📊 *GENEL SERVİS PERFORMANSI:*\n")
            append("• Toplam Çağrı: $totalCalls Adet\n")
            append("• Aktif Temsilci: ${userGroups.size} Kişi\n")
            append("• Toplam Süre: ${formatSecondsToReadable(totalSeconds)}\n")
            append("• Ortalama Çağrı Süresi: ${formatSecondsToReadable(avgSeconds)}\n")
            append("• Çözülen Çağrılar: $resolvedCount (%$resolutionRate)\n")
            append("• Bekleyen / Takipteki İşler: $pendingCount\n")
            append("━━━━━━━━━━━━━━━━━━━━\n")

            if (top3Staff.isNotEmpty()) {
                append("🏆 *EN ÇOK GÖRÜŞEN / DESTEK VEREN 3 PERSONEL:*\n")
                top3Staff.forEachIndexed { index, (user, userRecords) ->
                    val uTotal = userRecords.size
                    val uSecs = userRecords.sumOf { parseDurationToSeconds(it.toplamSure) }
                    val medal = when (index) {
                        0 -> "🥇"
                        1 -> "🥈"
                        2 -> "🥉"
                        else -> "${index + 1}."
                    }
                    append("$medal *$user:* $uTotal Kayıt | Toplam Süre: ${formatSecondsToReadable(uSecs)}\n")
                }
                append("━━━━━━━━━━━━━━━━━━━━\n")
            }

            if (userGroups.isNotEmpty()) {
                append("👥 *TEMSİLCİ BAZLI ÇAĞRI DAĞILIMI:*\n")
                userGroups.forEachIndexed { index, (user, userRecords) ->
                    val uTotal = userRecords.size
                    val uPct = if (totalCalls > 0) (uTotal * 100 / totalCalls) else 0
                    val uSecs = userRecords.sumOf { parseDurationToSeconds(it.toplamSure) }
                    val uAvg = if (uTotal > 0) uSecs / uTotal else 0
                    val uResolved = userRecords.count { it.bekleyenIs.equals("Hayır", ignoreCase = true) || it.bekleyenIs.isBlank() }
                    val medal = when (index) {
                        0 -> "🥇"
                        1 -> "🥈"
                        2 -> "🥉"
                        else -> "${index + 1}."
                    }
                    append("$medal *$user:* $uTotal Çağrı (%$uPct) | Ort: ${formatSecondsToReadable(uAvg)} | Çözüm: $uResolved/$uTotal\n")
                }
                append("━━━━━━━━━━━━━━━━━━━━\n")
            }

            if (topTopics.isNotEmpty()) {
                append("📌 *EN ÇOK GELEN ARIZA & TESPİT KONULARI (TESPİTLER):*\n")
                topTopics.forEachIndexed { i, (topic, count) ->
                    val pct = if (totalCalls > 0) (count * 100 / totalCalls) else 0
                    append("${i + 1}. $topic — $count Adet (%$pct)\n")
                }
                append("━━━━━━━━━━━━━━━━━━━━\n")
            }

            if (topFixedActions.isNotEmpty()) {
                append("🔧 *EN ÇOK DÜZELTİLEN 3 KONU (YAPILAN İŞLEMLER):*\n")
                topFixedActions.forEachIndexed { i, (action, count) ->
                    val pct = if (resolvedCount > 0) (count * 100 / resolvedCount) else 0
                    append("${i + 1}. $action — $count Çözüm (%$pct)\n")
                }
                append("━━━━━━━━━━━━━━━━━━━━\n")
            }

            if (topCustomers.isNotEmpty()) {
                append("🏢 *EN ÇOK ARAYAN MÜŞTERİ / BAYİLER:*\n")
                topCustomers.forEachIndexed { i, (cust, count) ->
                    append("${i + 1}. $cust ($count Çağrı)\n")
                }
                append("━━━━━━━━━━━━━━━━━━━━\n")
            }

            append("_Telefon Destek Yönetici Modülü üzerinden üretilmiştir._")
        }
    }

    fun shareServiceManagerReportViaWhatsApp(
        context: Context,
        records: List<SupportRecord>,
        periodTitle: String,
        includeExcel: Boolean = false
    ) {
        val report = generateServiceManagerReport(records, periodTitle)
        if (includeExcel && records.isNotEmpty()) {
            viewModelScope.launch {
                val file = try {
                    ExcelExporter.exportToCacheFile(
                        context = context,
                        records = records,
                        filenamePrefix = "Mudur_Raporu_${System.currentTimeMillis()}"
                    )
                } catch (e: Exception) {
                    null
                }
                WhatsAppHelper.shareShiftReportWithOptionalExcel(context, report, file)
            }
        } else {
            WhatsAppHelper.shareShiftReport(context, report)
        }
    }

    fun shareServiceManagerReportGeneral(
        context: Context,
        records: List<SupportRecord>,
        periodTitle: String,
        includeExcel: Boolean = false
    ) {
        val report = generateServiceManagerReport(records, periodTitle)
        if (includeExcel && records.isNotEmpty()) {
            viewModelScope.launch {
                val file = try {
                    ExcelExporter.exportToCacheFile(
                        context = context,
                        records = records,
                        filenamePrefix = "Mudur_Raporu_${System.currentTimeMillis()}"
                    )
                } catch (e: Exception) {
                    null
                }
                if (file != null && file.exists()) {
                    val fileUri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        putExtra(Intent.EXTRA_STREAM, fileUri)
                        putExtra(Intent.EXTRA_SUBJECT, "Servis Müdürü Yönetici Raporu ($periodTitle)")
                        putExtra(Intent.EXTRA_TEXT, report)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Müdür Raporu ve Excel Paylaş"))
                } else {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Servis Müdürü Yönetici Raporu ($periodTitle)")
                        putExtra(Intent.EXTRA_TEXT, report)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Müdür Raporu Paylaş"))
                }
            }
        } else {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Servis Müdürü Yönetici Raporu ($periodTitle)")
                putExtra(Intent.EXTRA_TEXT, report)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(sendIntent, "Müdür Raporu Paylaş"))
        }
    }

    fun exportServiceManagerExcel(context: Context, records: List<SupportRecord>, filenameSuffix: String = "Mudur_Raporu") {
        viewModelScope.launch {
            if (records.isEmpty()) {
                _uiEvent.emit("Seçilen filtrede aktarılacak kayıt bulunamadı!")
                return@launch
            }
            try {
                val file = ExcelExporter.exportToCacheFile(
                    context = context,
                    records = records,
                    filenamePrefix = filenameSuffix
                )
                _uiEvent.emit("✓ ${records.size} adet kayıt Excel (.xlsx) olarak oluşturuldu!")
                ExcelExporter.shareExcelFile(context, file)
            } catch (e: Exception) {
                _uiEvent.emit("Excel oluşturma hatası: ${e.message}")
            }
        }
    }

    // --- CİHAZ / SERİ NO / SİCİL SERVİS GEÇMİŞİ VE ANALİZ RAPORU ---

    fun openDeviceReport(query: String, explicitRecords: List<SupportRecord>? = null) {
        viewModelScope.launch {
            val q = query.trim()
            val targetRecords = if (explicitRecords != null) {
                explicitRecords
            } else if (q.isNotBlank()) {
                repository.searchAllFields(q)
            } else {
                emptyList()
            }.distinctBy { it.id }.sortedWith(
                compareByDescending<SupportRecord> { parseDateTimeFlexibleMillis(it.baslangicTarihi) }
                    .thenByDescending { it.id }
            )

            if (targetRecords.isEmpty()) {
                _uiEvent.emit("'$q' için servis geçmişi kaydı bulunamadı!")
                return@launch
            }

            val total = targetRecords.size
            val totalSecs = targetRecords.sumOf { parseDurationToSeconds(it.toplamSure) }
            val resolved = targetRecords.count { it.bekleyenIs.equals("Hayır", ignoreCase = true) || it.bekleyenIs.isBlank() }
            val pending = total - resolved

            val sortedAsc = targetRecords.sortedWith(
                compareBy<SupportRecord> { parseDateTimeFlexibleMillis(it.baslangicTarihi) }
                    .thenBy { it.id }
            )
            val firstDate = sortedAsc.firstOrNull()?.baslangicTarihi?.take(16) ?: "-"
            val lastDate = sortedAsc.lastOrNull()?.baslangicTarihi?.take(16) ?: "-"

            // Top issues breakdown (Arıza / Destek Konusu Dağılımı)
            val issueCounts = targetRecords.map {
                it.destekKonusu1.trim().ifBlank { it.tespit1.trim() }.ifBlank { "Diğer / Belirtilmemiş" }
            }.groupingBy { it }.eachCount().toList().sortedByDescending { it.second }

            // Users breakdown (Hangi Kullanıcı / Temsilci Kaç Kez Bakmış - Ayarlardaki isimle eşleştirilmiş)
            val userMap = targetRecords.groupBy {
                resolveRepresentativeName(it.kullaniciAdi.trim().ifBlank { it.kullaniciKodu.trim() })
            }.map { (uName, uRecs) ->
                val uCount = uRecs.size
                val uSecs = uRecs.sumOf { parseDurationToSeconds(it.toplamSure) }
                uName to (uCount to uSecs)
            }.sortedByDescending { it.second.first }

            // Actions summary (Yapılan İşlemler)
            val actions = targetRecords.map { it.islem1.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .take(10)

            val primaryTitle = targetRecords.firstOrNull()?.musteriBayiAdi?.ifBlank { q } ?: q

            _deviceReportData.value = DeviceReportData(
                query = q,
                title = primaryTitle,
                totalCalls = total,
                totalSeconds = totalSecs,
                resolvedCount = resolved,
                pendingCount = pending,
                firstDate = firstDate,
                lastDate = lastDate,
                topIssueBreakdown = issueCounts,
                userBreakdown = userMap,
                actionsSummary = actions,
                records = targetRecords
            )
        }
    }

    fun closeDeviceReport() {
        _deviceReportData.value = null
    }

    fun generateDeviceReportText(data: DeviceReportData): String {
        val sb = StringBuilder()
        sb.append("📋 *CİHAZ / SİCİL SERVİS GEÇMİŞİ RAPORU*\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("🔍 *Arama / Sicil:* ${data.query.uppercase(Locale.ROOT)}\n")
        sb.append("🏢 *Müşteri / Bayi:* ${data.title}\n")
        sb.append("📞 *Toplam Servis / Çağrı:* ${data.totalCalls} adet\n")
        sb.append("⏱️ *Toplam Harcanan Süre:* ${formatSecondsToReadable(data.totalSeconds)}\n")
        sb.append("✅ *Çözülen:* ${data.resolvedCount} | ⏳ *Bekleyen:* ${data.pendingCount}\n")
        sb.append("📅 *Tarih Aralığı:* ${data.firstDate} — ${data.lastDate}\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━\n\n")

        sb.append("📊 *ARIZA / DESTEK KONUSU DAĞILIMI:*\n")
        data.topIssueBreakdown.forEach { (issue, count) ->
            val pct = if (data.totalCalls > 0) (count * 100 / data.totalCalls) else 0
            sb.append("• $issue: *$count Çağrı* (%$pct)\n")
        }
        sb.append("\n")

        sb.append("👤 *BAKAN TEMSİLCİ / KULLANICILAR:*\n")
        data.userBreakdown.forEach { (user, pair) ->
            val count = pair.first
            val secs = pair.second
            sb.append("• $user: *$count Çağrı* (Toplam: ${formatSecondsToReadable(secs)})\n")
        }
        sb.append("\n")

        if (data.actionsSummary.isNotEmpty()) {
            sb.append("🛠️ *YAPILAN BAŞLICA İŞLEMLER:*\n")
            data.actionsSummary.take(5).forEach { act ->
                sb.append("✓ $act\n")
            }
            sb.append("\n")
        }

        sb.append("📑 *KRONOLOJİK SERVİS DÖKÜMÜ (${data.records.size} Kayıt):*\n")
        sb.append("────────────────────\n")
        data.records.take(15).forEachIndexed { idx, r ->
            val status = if (r.bekleyenIs.equals("Evet", ignoreCase = true)) "⏳ Bekliyor" else "✅ Çözüldü"
            val user = r.kullaniciAdi.ifBlank { r.kullaniciKodu }
            sb.append("${idx + 1}. [${r.baslangicTarihi.take(16)}] $status\n")
            sb.append("   👤 Temsilci: $user | Tel: ${r.telefon}\n")
            sb.append("   📌 Konu: ${r.destekKonusu1.ifBlank { r.tespit1 }}\n")
            if (r.islem1.isNotBlank()) {
                sb.append("   🛠️ İşlem: ${r.islem1}\n")
            }
            if (r.redmineId1.isNotBlank() && r.redmineId1 != "-") {
                sb.append("   🎫 Redmine: #${r.redmineId1}\n")
            }
            sb.append("   ⏱️ Süre: ${r.toplamSure}\n")
            sb.append("────────────────────\n")
        }
        if (data.records.size > 15) {
            sb.append("...ve ${data.records.size - 15} adet daha kayıt (Ekli Excel tablosunda tam 21 sütunlu döküm yer almaktadır).\n")
        }
        return sb.toString()
    }

    fun parseDateTimeFlexibleMillis(str: String): Long {
        val clean = str.trim()
        if (clean.isBlank()) return 0L
        val patterns = listOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "dd.MM.yyyy HH:mm:ss",
            "dd.MM.yyyy HH:mm",
            "dd/MM/yyyy HH:mm:ss",
            "dd/MM/yyyy HH:mm",
            "yyyy-MM-dd",
            "dd.MM.yyyy",
            "dd/MM/yyyy"
        )
        for (p in patterns) {
            try {
                val sdf = SimpleDateFormat(p, Locale.ROOT)
                sdf.isLenient = true
                val d = sdf.parse(clean)
                if (d != null) return d.time
            } catch (_: Exception) {}
        }
        return 0L
    }

    fun shareDeviceReport(
        context: Context,
        data: DeviceReportData,
        includeExcel: Boolean = true,
        viaWhatsApp: Boolean = false,
        viaMail: Boolean = false
    ) {
        viewModelScope.launch {
            try {
                val reportText = generateDeviceReportText(data)
                var excelUri: Uri? = null

                if (includeExcel && data.records.isNotEmpty()) {
                    val safeQuery = data.query.replace(Regex("[^a-zA-Z0-9_]"), "_")
                    val file = ExcelExporter.exportToCacheFile(
                        context = context,
                        records = data.records,
                        filenamePrefix = "Cihaz_Servis_Gecmisi_${safeQuery}"
                    )
                    excelUri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                }

                val safeTitle = "Cihaz Servis Raporu - ${data.query}"

                if (viaWhatsApp) {
                    val waIntent = Intent(Intent.ACTION_SEND).apply {
                        setPackage("com.whatsapp")
                        if (excelUri != null) {
                            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                            putExtra(Intent.EXTRA_STREAM, excelUri as Parcelable)
                            putExtra(Intent.EXTRA_TEXT, reportText)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        } else {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, reportText)
                        }
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    try {
                        context.startActivity(waIntent)
                    } catch (_: Exception) {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            if (excelUri != null) {
                                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                putExtra(Intent.EXTRA_STREAM, excelUri as Parcelable)
                                putExtra(Intent.EXTRA_TEXT, reportText)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            } else {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, reportText)
                            }
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Cihaz Raporunu Paylaş"))
                    }
                } else if (viaMail) {
                    val mailIntent = Intent(Intent.ACTION_SEND).apply {
                        type = if (excelUri != null) "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" else "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Cihaz / Sicil Servis Raporu: ${data.query} - ${data.title}")
                        putExtra(Intent.EXTRA_TEXT, reportText)
                        if (excelUri != null) {
                            putExtra(Intent.EXTRA_STREAM, excelUri as Parcelable)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(Intent.createChooser(mailIntent, "E-Posta ile Gönder"))
                } else {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        if (excelUri != null) {
                            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                            putExtra(Intent.EXTRA_STREAM, excelUri as Parcelable)
                            putExtra(Intent.EXTRA_TEXT, reportText)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        } else {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, reportText)
                        }
                        putExtra(Intent.EXTRA_SUBJECT, safeTitle)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Cihaz Raporunu Paylaş"))
                }
            } catch (e: Exception) {
                _uiEvent.emit("Rapor paylaşım hatası: ${e.message}")
            }
        }
    }

    fun removeDuplicateRecordsManually(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val count = repository.removeDuplicateRecords()
            refreshStorageBreakdown(context)
            _uiEvent.emit("✓ Veritabanı temizlendi: $count adet mükerrer (çift) kayıt başarıyla silindi.")
        }
    }

    // Helper for manual demo or force sync
    fun markAllAsSyncedLocally() {
        viewModelScope.launch {
            val unsynced = repository.getUnsyncedRecords()
            if (unsynced.isNotEmpty()) {
                repository.markAsSynced(unsynced.map { it.id })
                _uiEvent.emit("${unsynced.size} adet kayıt senkronize edildi olarak işaretlendi.")
            }
        }
    }

    fun toggleShowAdvancedFields() {
        val newValue = !_showAdvancedFields.value
        _showAdvancedFields.value = newValue
        sharedPrefs.edit().putBoolean("show_advanced_fields", newValue).apply()
    }

    fun setShowAdvancedFields(show: Boolean) {
        _showAdvancedFields.value = show
        sharedPrefs.edit().putBoolean("show_advanced_fields", show).apply()
    }

    fun setDeleteStartDate(date: String) {
        _deleteStartDate.value = date
    }

    fun setDeleteEndDate(date: String) {
        _deleteEndDate.value = date
    }

    fun setDeletePreset(preset: String) {
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = sdf.format(calendar.time)

        when (preset) {
            "Bugün" -> {
                _deleteStartDate.value = today
                _deleteEndDate.value = today
            }
            "1 Ay Önce ve Öncesi" -> {
                calendar.add(Calendar.MONTH, -1)
                _deleteStartDate.value = "2020-01-01"
                _deleteEndDate.value = sdf.format(calendar.time)
            }
            "3 Ay Önce ve Öncesi" -> {
                calendar.add(Calendar.MONTH, -3)
                _deleteStartDate.value = "2020-01-01"
                _deleteEndDate.value = sdf.format(calendar.time)
            }
            "Tümü" -> {
                _deleteStartDate.value = "2020-01-01"
                _deleteEndDate.value = "2030-12-31"
            }
        }
    }

    fun deleteRecordsByDateRange() {
        viewModelScope.launch {
            val start = _deleteStartDate.value.trim()
            val end = _deleteEndDate.value.trim()
            if (start.isBlank() || end.isBlank()) {
                _uiEvent.emit("Lütfen geçerli başlangıç ve bitiş tarihi girin!")
                return@launch
            }
            val deletedCount = repository.deleteRecordsBetweenDates(start, end)
            _uiEvent.emit("$deletedCount adet çağrı kaydı silindi.")
        }
    }

    // --- EXCEL EXPORT MODAL & FILTERING ---

    fun openExportDialog() {
        _exportStartDate.value = todayDateString()
        _exportEndDate.value = todayDateString()
        _isExportDialogOpen.value = true
    }

    fun closeExportDialog() {
        _isExportDialogOpen.value = false
    }

    fun setExportStartDate(date: String) {
        _exportStartDate.value = date
    }

    fun setExportEndDate(date: String) {
        _exportEndDate.value = date
    }

    fun setDatePreset(preset: String) {
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = sdf.format(calendar.time)

        when (preset) {
            "Bugün" -> {
                _exportStartDate.value = today
                _exportEndDate.value = today
            }
            "Dün" -> {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                val yesterday = sdf.format(calendar.time)
                _exportStartDate.value = yesterday
                _exportEndDate.value = yesterday
            }
            "Son 7 Gün" -> {
                _exportEndDate.value = today
                calendar.add(Calendar.DAY_OF_YEAR, -6)
                _exportStartDate.value = sdf.format(calendar.time)
            }
            "Bu Ay" -> {
                _exportEndDate.value = today
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                _exportStartDate.value = sdf.format(calendar.time)
            }
            "Tümü" -> {
                _exportStartDate.value = "2020-01-01"
                _exportEndDate.value = "2030-12-31"
            }
        }
    }

    fun getFilteredRecordsForExport(): List<SupportRecord> {
        val start = "${_exportStartDate.value} 00:00:00"
        val end = "${_exportEndDate.value} 23:59:59"
        val all = allRecords.value

        return all.filter { record ->
            val recordDate = record.basTarihi.trim()
            if (recordDate.length >= 10) {
                val recordDay = recordDate.substring(0, 10)
                recordDay >= _exportStartDate.value && recordDay <= _exportEndDate.value
            } else {
                recordDate >= start && recordDate <= end
            }
        }.sortedBy { it.basTarihi }
    }

    fun exportAndShareExcel(context: Context) {
        viewModelScope.launch {
            val filtered = getFilteredRecordsForExport()
            if (filtered.isEmpty()) {
                _uiEvent.emit("Seçilen tarih aralığında aktarılacak kayıt bulunamadı!")
                return@launch
            }

            try {
                val file = ExcelExporter.exportToCacheFile(
                    context = context,
                    records = filtered,
                    filenamePrefix = "Destek_${_exportStartDate.value}_${_exportEndDate.value}"
                )
                closeExportDialog()
                _uiEvent.emit("${filtered.size} adet kayıt Excel (.xlsx) olarak oluşturuldu!")
                ExcelExporter.shareExcelFile(context, file)
            } catch (e: Exception) {
                _uiEvent.emit("Excel oluşturma hatası: ${e.message}")
            }
        }
    }

    // --- HISTORY MODAL ---

    fun openHistoryModal() {
        _isHistoryModalOpen.value = true
    }

    fun closeHistoryModal() {
        _isHistoryModalOpen.value = false
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // --- VERİ YEDEKLEME VE GERİ YÜKLEME (DATA BACKUP & RESTORE) ---

    private val _isBackingUp = MutableStateFlow(false)
    val isBackingUp: StateFlow<Boolean> = _isBackingUp.asStateFlow()

    private val _isRestoring = MutableStateFlow(false)
    val isRestoring: StateFlow<Boolean> = _isRestoring.asStateFlow()

    private val _backupPreview = MutableStateFlow<com.example.backup.BackupPreviewData?>(null)
    val backupPreview: StateFlow<com.example.backup.BackupPreviewData?> = _backupPreview.asStateFlow()

    private val _lastBackupInfo = MutableStateFlow<String?>(sharedPrefs.getString("last_backup_info", null))
    val lastBackupInfo: StateFlow<String?> = _lastBackupInfo.asStateFlow()

    fun exportDataBackup(context: Context, shareImmediately: Boolean = true) {
        if (_isBackingUp.value) return
        viewModelScope.launch {
            _isBackingUp.value = true
            try {
                val (file, pkg) = com.example.backup.BackupManager.createBackup(context, repository)
                val info = "Son Yedek: ${pkg.backupDate} (${pkg.records.size} Kayıt, ${pkg.templates.size} Şablon)"
                _lastBackupInfo.value = info
                sharedPrefs.edit().putString("last_backup_info", info).apply()

                _uiEvent.emit("✓ Yedekleme başarılı! ${pkg.records.size} çağrı kaydı ve tüm ayarlar hazırlandı.")

                if (shareImmediately) {
                    com.example.backup.BackupManager.shareBackupFile(context, file)
                }
            } catch (e: Exception) {
                _uiEvent.emit("Yedekleme hatası: ${e.localizedMessage ?: e.message}")
            } finally {
                _isBackingUp.value = false
            }
        }
    }

    fun loadBackupForPreview(context: Context, uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                val preview = com.example.backup.BackupManager.parseBackupFromUri(context, uri)
                _backupPreview.value = preview
            } catch (e: Exception) {
                _uiEvent.emit("Geçersiz yedek dosyası: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    fun dismissBackupPreview() {
        _backupPreview.value = null
    }

    fun confirmAndRestoreBackup(context: Context, clearExisting: Boolean) {
        val preview = _backupPreview.value ?: return
        if (_isRestoring.value) return

        viewModelScope.launch {
            _isRestoring.value = true
            try {
                val result = com.example.backup.BackupManager.restoreBackup(
                    context = context,
                    repository = repository,
                    previewData = preview,
                    clearExisting = clearExisting
                )

                // SharedPreferences ve StateFlow'ları tazeleyelim
                _globalKullanici.value = sharedPrefs.getString("global_kullanici", "") ?: ""
                _globalKullaniciKodu.value = sharedPrefs.getString("global_kullanici_kodu", "") ?: ""
                _showAdvancedFields.value = sharedPrefs.getBoolean("show_advanced_fields", true)
                _sqlDbType.value = sharedPrefs.getString("sql_db_type", "MySQL") ?: "MySQL"
                _sqlHost.value = sharedPrefs.getString("sql_host", "") ?: ""
                _sqlPort.value = sharedPrefs.getString("sql_port", "") ?: ""
                _sqlDbName.value = sharedPrefs.getString("sql_db_name", "") ?: ""
                _sqlUser.value = sharedPrefs.getString("sql_user", "") ?: ""
                _sqlPassword.value = sharedPrefs.getString("sql_password", "") ?: ""

                _verimorDomain.value = sharedPrefs.getString("verimor_domain", "sirketiniz.bulutsantralim.com") ?: ""
                _verimorExtension.value = sharedPrefs.getString("verimor_extension", "101") ?: ""
                _verimorPassword.value = sharedPrefs.getString("verimor_password", "") ?: ""
                _verimorPort.value = sharedPrefs.getString("verimor_port", "5060") ?: "5060"
                _verimorAutoTimer.value = sharedPrefs.getBoolean("verimor_auto_timer", true)
                _verimorAutoSearch.value = sharedPrefs.getBoolean("verimor_auto_search", true)
                _verimorAutoConnect.value = sharedPrefs.getBoolean("verimor_auto_connect", true)

                // Aktif formu da güncel temsilciye eşitleyelim
                _formState.update {
                    it.copy(
                        kullanici = _globalKullanici.value,
                        kullaniciKodu = _globalKullaniciKodu.value
                    )
                }

                _backupPreview.value = null

                // SIP yeniden bağlantı: Yedekten gelen bilgilerle kayıt ol
                if (_verimorAutoConnect.value && _verimorDomain.value.isNotBlank() && _verimorExtension.value.isNotBlank()) {
                    connectVerimorSip(isAuto = true)
                }

                _uiEvent.emit("✓ ${result.message}")
            } catch (e: Exception) {
                _uiEvent.emit("Geri yükleme hatası: ${e.localizedMessage ?: e.message}")
            } finally {
                _isRestoring.value = false
            }
        }
    }

    private fun currentDateTimeString(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    }

    fun todayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
}
