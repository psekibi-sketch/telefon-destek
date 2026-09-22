package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.SupportDatabase
import com.example.data.SupportRepository
import com.example.export.ExcelExporter
import com.example.model.SupportRecord
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class AutoFillSuggestion(
    val title: String,
    val musteriBayi: String,
    val gorusulen: String,
    val telefon: String,
    val matchedField: String // "SICIL" or "TELEFON"
)

data class SupportFormState(
    val editingId: Long? = null,
    val redmine: String = "",
    val basTarihi: String = "",
    val sicilNo: String = "",
    val kullanici: String = "",
    val arayan: String = "",
    val musteriBayi: String = "",
    val gorusulen: String = "",
    val telefon: String = "",
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
    val argeDonusNotu: String = ""
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

    // Global Kullanıcı (Temsilci) setting stored in SharedPreferences
    private val sharedPrefs = application.getSharedPreferences("support_app_prefs", Context.MODE_PRIVATE)
    private val _globalKullanici = MutableStateFlow(sharedPrefs.getString("global_kullanici", "") ?: "")
    val globalKullanici: StateFlow<String> = _globalKullanici.asStateFlow()

    // Auto-fill suggestion state
    private val _autoFillSuggestion = MutableStateFlow<AutoFillSuggestion?>(null)
    val autoFillSuggestion: StateFlow<AutoFillSuggestion?> = _autoFillSuggestion.asStateFlow()

    // Show advanced fields (Redmine, Ar-Ge, Bag ID) toggle - default false as requested
    private val _showAdvancedFields = MutableStateFlow(false)
    val showAdvancedFields: StateFlow<Boolean> = _showAdvancedFields.asStateFlow()

    // Date range delete state in Settings
    private val _deleteStartDate = MutableStateFlow(todayDateString())
    val deleteStartDate: StateFlow<String> = _deleteStartDate.asStateFlow()

    private val _deleteEndDate = MutableStateFlow(todayDateString())
    val deleteEndDate: StateFlow<String> = _deleteEndDate.asStateFlow()

    // Templates for quick completion / suggestion
    val tespitTemplates: StateFlow<List<com.example.model.SupportTemplate>>
    val islemTemplates: StateFlow<List<com.example.model.SupportTemplate>>
    val konuTemplates: StateFlow<List<com.example.model.SupportTemplate>>

    // Snackbar notifications
    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent: SharedFlow<String> = _uiEvent.asSharedFlow()

    init {
        val database = SupportDatabase.getInstance(application)
        repository = SupportRepository(database.supportDao(), database.templateDao())

        allRecords = repository.allRecords.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
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
        }
    }

    // --- FORM FIELD UPDATES ---

    fun updateRedmine(value: String) = _formState.update { it.copy(redmine = value) }
    fun updateBasTarihi(value: String) = _formState.update { it.copy(basTarihi = value) }

    fun updateSicilNo(value: String) {
        val upper = value.uppercase(Locale("tr", "TR"))
        _formState.update { it.copy(sicilNo = upper) }
        checkAutoFillBySicilNo(upper)
    }

    fun updateKullanici(value: String) = _formState.update { it.copy(kullanici = value) }
    fun updateArayan(value: String) = _formState.update { it.copy(arayan = value) }
    fun updateMusteriBayi(value: String) = _formState.update { it.copy(musteriBayi = value.uppercase(Locale("tr", "TR"))) }
    fun updateGorusulen(value: String) = _formState.update { it.copy(gorusulen = value.uppercase(Locale("tr", "TR"))) }

    fun updateTelefon(value: String) {
        _formState.update { it.copy(telefon = value) }
        checkAutoFillByTelefon(value)
    }

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

    // --- AUTO FILL LOGIC (SICIL NO & TELEFON) ---

    private fun checkAutoFillBySicilNo(sicil: String) {
        val trimmed = sicil.trim()
        if (trimmed.length < 2) {
            _autoFillSuggestion.value = null
            return
        }

        // Search past records with this sicilNo
        val match = allRecords.value.firstOrNull { it.sicilNo.equals(trimmed, ignoreCase = true) }
        if (match != null && (match.musteriBayi.isNotBlank() || match.gorusulen.isNotBlank() || match.telefon.isNotBlank())) {
            _autoFillSuggestion.value = AutoFillSuggestion(
                title = "Sicil No ($trimmed) bulundu! Bilgiler otomatik doldurulsun mu?",
                musteriBayi = match.musteriBayi,
                gorusulen = match.gorusulen,
                telefon = match.telefon,
                matchedField = "SICIL"
            )
        } else {
            if (_autoFillSuggestion.value?.matchedField == "SICIL") {
                _autoFillSuggestion.value = null
            }
        }
    }

    private fun checkAutoFillByTelefon(phone: String) {
        val cleanPhone = phone.replace(" ", "").replace("-", "").replace("(", "").replace(")", "").trim()
        if (cleanPhone.length < 5) {
            if (_autoFillSuggestion.value?.matchedField == "TELEFON") {
                _autoFillSuggestion.value = null
            }
            return
        }

        val match = allRecords.value.firstOrNull {
            val recPhone = it.telefon.replace(" ", "").replace("-", "").replace("(", "").replace(")", "").trim()
            recPhone.isNotBlank() && recPhone == cleanPhone
        }

        if (match != null && (match.musteriBayi.isNotBlank() || match.gorusulen.isNotBlank())) {
            _autoFillSuggestion.value = AutoFillSuggestion(
                title = "Bu telefon numarası (${match.telefon}) daha önce kullanılmış. Müşteri ve görüşülen bilgileri doldurulsun mu?",
                musteriBayi = match.musteriBayi,
                gorusulen = match.gorusulen,
                telefon = match.telefon,
                matchedField = "TELEFON"
            )
        } else {
            if (_autoFillSuggestion.value?.matchedField == "TELEFON") {
                _autoFillSuggestion.value = null
            }
        }
    }

    fun applyAutoFillSuggestion() {
        val suggestion = _autoFillSuggestion.value ?: return
        _formState.update { current ->
            if (suggestion.matchedField == "TELEFON") {
                // Keep sicilNo empty or untouched as requested: "aynı sekıl doldur ama sicil no kısmını boş getir"
                current.copy(
                    musteriBayi = suggestion.musteriBayi.uppercase(),
                    gorusulen = suggestion.gorusulen.uppercase(),
                    telefon = suggestion.telefon,
                    sicilNo = ""
                )
            } else {
                current.copy(
                    musteriBayi = suggestion.musteriBayi.uppercase(),
                    gorusulen = suggestion.gorusulen.uppercase(),
                    telefon = suggestion.telefon
                )
            }
        }
        _autoFillSuggestion.value = null
        viewModelScope.launch {
            _uiEvent.emit("Bilgiler otomatik dolduruldu.")
        }
    }

    fun dismissAutoFillSuggestion() {
        _autoFillSuggestion.value = null
    }

    // --- KRONOMETRE & BAŞLAT (START CALL) ---

    /**
     * Requirement 4:
     * "birinci ekrandaki ve ikinci ekrandaki tum kayıtlar baslat a bos gelsin baslat a basınca ekran da verıler temizlensin"
     * Clears all fields in Screen 1 & Screen 2, sets start time, resets stopwatch to 0 and starts timer.
     */
    fun startCallAndClear() {
        val nowFormatted = currentDateTimeString()
        // Clear all fields and set new start time
        _formState.value = SupportFormState(
            basTarihi = nowFormatted,
            sure = "00:00"
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
            redmine = record.redmine,
            basTarihi = record.basTarihi,
            sicilNo = record.sicilNo,
            kullanici = record.kullanici,
            arayan = record.arayan,
            musteriBayi = record.musteriBayi,
            gorusulen = record.gorusulen,
            telefon = record.telefon,
            iletisimTuru = record.iletisimTuru,
            destekKonu = record.destekKonu,
            tespit = record.tespit,
            islem = record.islem,
            bagId = record.bagId,
            bitTarihi = record.bitTarihi,
            sure = record.sure,
            bekleyen = record.bekleyen,
            b2bKayit = record.b2bKayit,
            argeGidis = record.argeGidis,
            argeGidisNotu = record.argeGidisNotu,
            argeDonus = record.argeDonus,
            argeDonusNotu = record.argeDonusNotu
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

            // Determine user: Use global setting if specified, otherwise form field
            val userToSave = _globalKullanici.value.trim().ifBlank { currentForm.kullanici.trim() }

            val record = SupportRecord(
                id = currentForm.editingId ?: 0,
                redmine = currentForm.redmine.trim(),
                basTarihi = bas.trim(),
                sicilNo = currentForm.sicilNo.trim().uppercase(),
                kullanici = userToSave,
                arayan = currentForm.arayan.trim(),
                musteriBayi = currentForm.musteriBayi.trim().uppercase(),
                gorusulen = currentForm.gorusulen.trim().uppercase(),
                telefon = currentForm.telefon.trim(),
                iletisimTuru = currentForm.iletisimTuru.trim(),
                destekKonu = currentForm.destekKonu.trim(),
                tespit = currentForm.tespit.trim(),
                islem = currentForm.islem.trim(),
                bagId = currentForm.bagId.trim(),
                bitTarihi = bit.trim(),
                sure = duration.trim(),
                bekleyen = currentForm.bekleyen.trim(),
                b2bKayit = currentForm.b2bKayit.trim(),
                argeGidis = currentForm.argeGidis.trim(),
                argeGidisNotu = currentForm.argeGidisNotu.trim(),
                argeDonus = currentForm.argeDonus.trim(),
                argeDonusNotu = currentForm.argeDonusNotu.trim()
            )

            if (currentForm.editingId != null && currentForm.editingId > 0) {
                repository.update(record)
                _uiEvent.emit("Destek çağrı kaydı başarıyla güncellendi!")
            } else {
                repository.insert(record)
                _uiEvent.emit("Destek çağrı kaydı başarıyla kaydedildi!")
            }

            // Clear form completely for next call
            _formState.value = SupportFormState()
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
        _formState.value = SupportFormState()
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

    fun openSettings() {
        _isSettingsOpen.value = true
    }

    fun closeSettings() {
        _isSettingsOpen.value = false
    }

    fun updateGlobalKullanici(name: String) {
        _globalKullanici.value = name
        sharedPrefs.edit().putString("global_kullanici", name).apply()
    }

    fun toggleShowAdvancedFields() {
        _showAdvancedFields.value = !_showAdvancedFields.value
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
