package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SupportDatabase
import com.example.data.SupportRepository
import com.example.export.ExcelExporter
import com.example.model.SupportRecord
import com.example.network.SyncApiClient
import com.example.network.SearchCustomerRequest
import com.example.network.toSyncCallItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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

data class SupportFormState(
    val editingId: Long? = null,
    val redmine: String = "",
    val basTarihi: String = "",
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

    private val _settingsInitialTab = MutableStateFlow(0)
    val settingsInitialTab: StateFlow<Int> = _settingsInitialTab.asStateFlow()

    // Global Kullanıcı (Temsilci) ve Kullanıcı Kodu (ID) stored in SharedPreferences
    private val sharedPrefs = application.getSharedPreferences("support_app_prefs", Context.MODE_PRIVATE)
    private val _globalKullanici = MutableStateFlow(sharedPrefs.getString("global_kullanici", "") ?: "")
    val globalKullanici: StateFlow<String> = _globalKullanici.asStateFlow()

    private val _globalKullaniciKodu = MutableStateFlow(sharedPrefs.getString("global_kullanici_kodu", "") ?: "")
    val globalKullaniciKodu: StateFlow<String> = _globalKullaniciKodu.asStateFlow()

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

    // Templates for quick completion / suggestion
    val tespitTemplates: StateFlow<List<com.example.model.SupportTemplate>>
    val islemTemplates: StateFlow<List<com.example.model.SupportTemplate>>
    val konuTemplates: StateFlow<List<com.example.model.SupportTemplate>>

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
        repository = SupportRepository(database.supportDao(), database.templateDao())

        allRecords = repository.allRecords.stateIn(
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

        viewModelScope.launch {
            repository.seedDefaultTemplatesIfEmpty()
            // Automatic demo records creation is completely removed as requested
        }

        // Persistent SIP Connection: Automatically reconnect on app startup if previously enabled and configured
        val shouldAutoConnectSip = sharedPrefs.getBoolean("verimor_auto_connect", true)
        val domain = _verimorDomain.value.trim()
        val ext = _verimorExtension.value.trim()
        if (shouldAutoConnectSip && domain.isNotBlank() && ext.isNotBlank()) {
            connectVerimorSip(isAuto = true)
        }

        // Listen for incoming Verimor SIP call events
        viewModelScope.launch {
            verimorSipManager.callEvents.collect { callEvent ->
                handleIncomingVerimorCall(callEvent)
            }
        }

        // Listen for SIP state errors to notify user immediately via snackbar
        viewModelScope.launch {
            verimorSipManager.connectionState.collect { state ->
                if (state is com.example.sip.VerimorSipState.Error) {
                    _uiEvent.emit("Verimor SIP: ${state.message}")
                }
            }
        }
    }

    // --- FORM FIELD UPDATES ---

    fun updateRedmine(value: String) = _formState.update { it.copy(redmine = value) }
    fun updateBasTarihi(value: String) = _formState.update { it.copy(basTarihi = value) }

    fun updateSicilNo(value: String) {
        val upper = value.uppercase(Locale("tr", "TR"))
        _formState.update { it.copy(sicilNo = upper) }
        triggerSmartLookup("SICIL", upper)
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
    fun updateDestekKonu(value: String) = _formState.update { it.copy(destekKonu = value) }
    fun updateTespit(value: String) = _formState.update { it.copy(tespit = value) }
    fun updateIslem(value: String) = _formState.update { it.copy(islem = value) }
    fun updateBagId(value: String) = _formState.update { it.copy(bagId = value) }
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

    // --- AKILLI ÇOKLU KRİTER ARAMA & KONTROL MEKANİZMASI (SICIL, TELEFON, GÖRÜŞÜLEN) ---

    fun triggerSmartLookup(criterion: String, query: String, isExplicit: Boolean = false) {
        lookupJob?.cancel()
        lookupJob = viewModelScope.launch {
            if (!isExplicit) {
                delay(300L) // Debounce typing only when continuously typing
            }

            val cleanQuery = query.trim()
            val checkVal = if (criterion == "TELEFON") {
                cleanQuery.replace(Regex("\\D"), "")
            } else {
                cleanQuery
            }

            val minLength = when {
                isExplicit -> 1
                criterion == "TELEFON" -> 4
                criterion == "SICIL" -> 1
                else -> 2
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

            current.copy(
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
        if (!isAuto) {
            viewModelScope.launch {
                _uiEvent.emit("Verimor Dahili $ext kaydı başlatılıyor...")
            }
        }
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

    fun simulateVerimorIncomingCall(testPhone: String = "05321002030") {
        verimorSipManager.simulateIncomingCall(testPhone, "Santral Test Çağrısı")
    }

    fun dismissIncomingCallBanner() {
        _activeIncomingCall.value = null
        verimorSipManager.hangupCall()
    }

    fun handleIncomingVerimorCall(callEvent: com.example.sip.VerimorCallEvent) {
        val cleanPhone = callEvent.callerNumber.replace(Regex("\\s+"), "").trim()
        val callerName = callEvent.callerName.trim()

        _activeIncomingCall.value = callEvent

        if (_verimorAutoTimer.value) {
            // Automatically triggers "Başlat (Yeni)" stopwatch & resets call duration
            startCallAndClear(initialPhone = cleanPhone, initialGorusulen = callerName)
        } else {
            updateTelefon(cleanPhone)
            if (callerName.isNotBlank() && _formState.value.gorusulen.isBlank()) {
                updateGorusulen(callerName)
            }
        }

        if (_verimorAutoSearch.value && cleanPhone.isNotBlank()) {
            // Automatically triggers local offline database search
            triggerSmartLookup("TELEFON", cleanPhone, isExplicit = true)
        }

        viewModelScope.launch {
            _uiEvent.emit("📞 Verimor Santral: $cleanPhone çağrısı yakalandı. Form ve kronometre başlatıldı.")
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
        _formState.value = SupportFormState(
            editingId = record.id,
            redmine = record.redmineId1,
            basTarihi = record.baslangicTarihi,
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

            val record = SupportRecord(
                id = currentForm.editingId ?: 0,
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
                destekKonusu1 = currentForm.destekKonu.trim(),
                sicilNo1 = currentForm.sicilNo.trim().uppercase(trLocale),
                tespit1 = currentForm.tespit.trim(),
                islem1 = currentForm.islem.trim(),
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
                yn500SicilNo1 = currentForm.sicilNo.trim().uppercase(trLocale),
                dynCashVersiyon1 = currentForm.dynCashVersiyon.trim(),
                arkaOfisId1 = "",
                yn500KasaVersiyon1 = currentForm.yn500Versiyon.trim(),
                servisKodu = currentForm.servisKodu.trim(),
                not1 = currentForm.servisKodu.trim(),
                isSynced = false
            )

            if (currentForm.editingId != null && currentForm.editingId > 0) {
                repository.update(record)
                _uiEvent.emit("Destek çağrı kaydı başarıyla güncellendi!")
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

    fun openSettings(initialTab: Int = 0) {
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

    fun setGlobalUser(name: String, code: String) {
        _globalKullanici.value = name
        _globalKullaniciKodu.value = code
        sharedPrefs.edit()
            .putString("global_kullanici", name)
            .putString("global_kullanici_kodu", code)
            .apply()
        _formState.update { it.copy(kullanici = name, kullaniciKodu = code) }
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

    private fun currentDateTimeString(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    }

    private fun todayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
}
