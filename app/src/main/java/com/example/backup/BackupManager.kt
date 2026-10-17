package com.example.backup

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.SupportRepository
import com.example.model.SupportRecord
import com.example.model.SupportTemplate
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * SIP Bağlantı Bilgileri Yedek Modeli
 */
data class SipBackupConfig(
    val domain: String = "",
    val extension: String = "",
    val password: String = "",
    val port: String = "5060",
    val autoTimer: Boolean = true,
    val autoSearch: Boolean = true,
    val autoConnect: Boolean = true
)

/**
 * Uygulama & SQL Bağlantı Ayarları Yedek Modeli
 */
data class AppBackupSettings(
    val globalKullanici: String = "",
    val globalKullaniciKodu: String = "",
    val showAdvancedFields: Boolean = true,
    val sqlDbType: String = "MySQL",
    val sqlHost: String = "",
    val sqlPort: String = "",
    val sqlDbName: String = "",
    val sqlUser: String = "",
    val sqlPassword: String = ""
)

/**
 * Tam Sistem Yedek Paketi
 */
data class BackupPackage(
    val version: Int = 1,
    val appName: String = "Telefon Destek",
    val backupDate: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sipConfig: SipBackupConfig,
    val appSettings: AppBackupSettings,
    val templates: List<SupportTemplate>,
    val records: List<SupportRecord>
)

/**
 * Geri Yükleme Öncesi Doğrulama ve Önizleme Bilgisi
 */
data class BackupPreviewData(
    val backupDate: String,
    val totalRecords: Int,
    val totalTemplates: Int,
    val sipExtension: String,
    val sipDomain: String,
    val representativeName: String,
    val packageData: BackupPackage
)

/**
 * Geri Yükleme İşlem Sonucu
 */
data class BackupRestoreResult(
    val success: Boolean,
    val recordsCount: Int,
    val templatesCount: Int,
    val sipRestored: Boolean,
    val message: String
)

/**
 * SQLite Veritabanı ve SIP Ayarları için Tam Yedekleme & Geri Yükleme Yöneticisi
 */
object BackupManager {

    private const val TAG = "BackupManager"
    private const val PREFS_NAME = "support_app_prefs"

    /**
     * Güncel veritabanı kayıtlarını ve ayarları JSON formatında yedekler.
     * Hem yerel önbellek dosyası oluşturur hem de Cihazın İndirilenler klasörüne kaydeder.
     */
    suspend fun createBackup(
        context: Context,
        repository: SupportRepository
    ): Pair<File, BackupPackage> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // 1. Ayarları oku
        val sipConfig = SipBackupConfig(
            domain = prefs.getString("verimor_domain", "sirketiniz.bulutsantralim.com") ?: "",
            extension = prefs.getString("verimor_extension", "101") ?: "",
            password = prefs.getString("verimor_password", "") ?: "",
            port = prefs.getString("verimor_port", "5060") ?: "5060",
            autoTimer = prefs.getBoolean("verimor_auto_timer", true),
            autoSearch = prefs.getBoolean("verimor_auto_search", true),
            autoConnect = prefs.getBoolean("verimor_auto_connect", true)
        )

        val appSettings = AppBackupSettings(
            globalKullanici = prefs.getString("global_kullanici", "") ?: "",
            globalKullaniciKodu = prefs.getString("global_kullanici_kodu", "") ?: "",
            showAdvancedFields = prefs.getBoolean("show_advanced_fields", true),
            sqlDbType = prefs.getString("sql_db_type", "MySQL") ?: "MySQL",
            sqlHost = prefs.getString("sql_host", "") ?: "",
            sqlPort = prefs.getString("sql_port", "") ?: "",
            sqlDbName = prefs.getString("sql_db_name", "") ?: "",
            sqlUser = prefs.getString("sql_user", "") ?: "",
            sqlPassword = prefs.getString("sql_password", "") ?: ""
        )

        // 2. Veritabanındaki tüm tabloları oku
        val records = repository.getAllRecordsList()
        val templates = repository.getAllTemplatesList()

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val backupDate = dateFormat.format(Date())

        val backupPackage = BackupPackage(
            version = 1,
            appName = "Telefon Destek",
            backupDate = backupDate,
            timestamp = System.currentTimeMillis(),
            sipConfig = sipConfig,
            appSettings = appSettings,
            templates = templates,
            records = records
        )

        // 3. JSON Dizgisini oluştur
        val jsonString = serializeBackupToJson(backupPackage)

        // 4. Dosyaya yaz (Uygulama Önbelleği & Paylaşım Alanı)
        val fileTimestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Destek_Yedek_$fileTimestamp.json"

        val backupDir = File(context.cacheDir, "backups").apply { mkdirs() }
        val backupFile = File(backupDir, fileName)
        FileOutputStream(backupFile).use { fos ->
            fos.write(jsonString.toByteArray(Charsets.UTF_8))
        }

        // 5. Depolama / İndirilenler klasörüne kaydetmeyi dene
        saveToDownloadsFolder(context, fileName, jsonString)

        return Pair(backupFile, backupPackage)
    }

    /**
     * Oluşturulan yedek dosyasını sistem paylaşım menüsü (Drive, WhatsApp, Mail vb.) ile paylaşır.
     */
    fun shareBackupFile(context: Context, file: File) {
        val authority = "${context.packageName}.fileprovider"
        val contentUri: Uri = FileProvider.getUriForFile(context, authority, file)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            putExtra(Intent.EXTRA_TEXT, "Telefon Destek Veri Yedeği (${file.name})")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Yedek Dosyasını Kaydet / Paylaş")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /**
     * Seçilen yedek dosyasını (Uri) okuyarak önizleme bilgilerini üretir.
     */
    fun parseBackupFromUri(context: Context, uri: Uri): BackupPreviewData {
        val inputStream: InputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Yedek dosyası açılamadı.")

        val jsonString = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { it.readText() }
        val jsonRoot = JSONObject(jsonString)

        val version = jsonRoot.optInt("version", 1)
        val backupDate = jsonRoot.optString("backupDate", "Bilinmeyen Tarih")

        // SIP Ayarları
        val sipObj = jsonRoot.optJSONObject("sipSettings") ?: JSONObject()
        val sipConfig = SipBackupConfig(
            domain = sipObj.optString("domain", ""),
            extension = sipObj.optString("extension", ""),
            password = sipObj.optString("password", ""),
            port = sipObj.optString("port", "5060"),
            autoTimer = sipObj.optBoolean("autoTimer", true),
            autoSearch = sipObj.optBoolean("autoSearch", true),
            autoConnect = sipObj.optBoolean("autoConnect", true)
        )

        // Uygulama Ayarları
        val appObj = jsonRoot.optJSONObject("appSettings") ?: JSONObject()
        val appSettings = AppBackupSettings(
            globalKullanici = appObj.optString("globalKullanici", ""),
            globalKullaniciKodu = appObj.optString("globalKullaniciKodu", ""),
            showAdvancedFields = appObj.optBoolean("showAdvancedFields", true),
            sqlDbType = appObj.optString("sqlDbType", "MySQL"),
            sqlHost = appObj.optString("sqlHost", ""),
            sqlPort = appObj.optString("sqlPort", ""),
            sqlDbName = appObj.optString("sqlDbName", ""),
            sqlUser = appObj.optString("sqlUser", ""),
            sqlPassword = appObj.optString("sqlPassword", "")
        )

        // Sabit Şablonlar
        val templatesList = mutableListOf<SupportTemplate>()
        val templatesArray = jsonRoot.optJSONArray("templates") ?: JSONArray()
        for (i in 0 until templatesArray.length()) {
            val item = templatesArray.getJSONObject(i)
            templatesList.add(
                SupportTemplate(
                    id = 0, // Geri yüklenirken SQLite otomatik id üretecektir
                    type = item.optString("type", "TESPIT"),
                    text = item.optString("text", "")
                )
            )
        }

        // Destek Çağrı Kayıtları
        val recordsList = mutableListOf<SupportRecord>()
        val recordsArray = jsonRoot.optJSONArray("records") ?: JSONArray()
        for (i in 0 until recordsArray.length()) {
            val r = recordsArray.getJSONObject(i)
            recordsList.add(
                SupportRecord(
                    id = 0, // ID çakışmalarını önlemek için otomatik ID
                    kullaniciKodu = r.optString("kullaniciKodu", ""),
                    kullaniciAdi = r.optString("kullaniciAdi", ""),
                    baslangicTarihi = r.optString("baslangicTarihi", ""),
                    arayanTuru = r.optString("arayanTuru", ""),
                    musteriBayiAdi = r.optString("musteriBayiAdi", ""),
                    gorusulenKisi = r.optString("gorusulenKisi", ""),
                    telefon = r.optString("telefon", ""),
                    telefon2 = r.optString("telefon2", ""),
                    mail = r.optString("mail", ""),
                    iletisimTuru = r.optString("iletisimTuru", "Telefon"),
                    destekKonusu1 = r.optString("destekKonusu1", ""),
                    sicilNo1 = r.optString("sicilNo1", ""),
                    tespit1 = r.optString("tespit1", ""),
                    islem1 = r.optString("islem1", ""),
                    baglantiId1 = r.optString("baglantiId1", ""),
                    bitisTarihi = r.optString("bitisTarihi", ""),
                    toplamSure = r.optString("toplamSure", ""),
                    kasaVersiyon1 = r.optString("kasaVersiyon1", ""),
                    arkaOfisVersiyon1 = r.optString("arkaOfisVersiyon1", ""),
                    redmineId1 = r.optString("redmineId1", ""),
                    aktif = r.optString("aktif", "1"),
                    b2bKayit = r.optString("b2bKayit", ""),
                    bekleyenIs = r.optString("bekleyenIs", "Hayır"),
                    argeGidis = r.optString("argeGidis", "Hayır"),
                    argeDonus = r.optString("argeDonus", "Hayır"),
                    argeGidisNotu = r.optString("argeGidisNotu", ""),
                    argeDonusNotu = r.optString("argeDonusNotu", ""),
                    resim = r.optString("resim", ""),
                    tckn = r.optString("tckn", ""),
                    vergiNo = r.optString("vergiNo", ""),
                    tespitKodu1 = r.optString("tespitKodu1", ""),
                    islemKodu1 = r.optString("islemKodu1", ""),
                    yn500SicilNo1 = r.optString("yn500SicilNo1", ""),
                    dynCashVersiyon1 = r.optString("dynCashVersiyon1", ""),
                    arkaOfisId1 = r.optString("arkaOfisId1", ""),
                    yn500KasaVersiyon1 = r.optString("yn500KasaVersiyon1", ""),
                    servisKodu = r.optString("servisKodu", ""),
                    not1 = r.optString("not1", ""),
                    isSynced = r.optBoolean("isSynced", false),
                    syncedAt = if (r.has("syncedAt") && !r.isNull("syncedAt")) r.optLong("syncedAt") else null,
                    createdAt = r.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        val pkg = BackupPackage(
            version = version,
            appName = jsonRoot.optString("appName", "Telefon Destek"),
            backupDate = backupDate,
            timestamp = jsonRoot.optLong("timestamp", System.currentTimeMillis()),
            sipConfig = sipConfig,
            appSettings = appSettings,
            templates = templatesList,
            records = recordsList
        )

        return BackupPreviewData(
            backupDate = backupDate,
            totalRecords = recordsList.size,
            totalTemplates = templatesList.size,
            sipExtension = sipConfig.extension,
            sipDomain = sipConfig.domain,
            representativeName = appSettings.globalKullanici,
            packageData = pkg
        )
    }

    /**
     * Yedek paketini SQLite veritabanına ve SharedPreferences'a geri yükler.
     * @param clearExisting Eğer true ise mevcut tablolar tamamen temizlenir ve yedek tek kaynak yapılır.
     */
    suspend fun restoreBackup(
        context: Context,
        repository: SupportRepository,
        previewData: BackupPreviewData,
        clearExisting: Boolean
    ): BackupRestoreResult {
        val pkg = previewData.packageData

        if (clearExisting) {
            repository.deleteAll()
            repository.deleteAllTemplates()
        }

        // 1. Şablonları ekle
        if (pkg.templates.isNotEmpty()) {
            repository.insertAllTemplates(pkg.templates)
        }

        // 2. Çağrı kayıtlarını ekle
        if (pkg.records.isNotEmpty()) {
            repository.insertAllRecords(pkg.records)
        }

        // 3. SharedPreferences Ayarlarını Geri Yükle
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            if (pkg.sipConfig.domain.isNotBlank()) {
                putString("verimor_domain", pkg.sipConfig.domain)
                putString("verimor_extension", pkg.sipConfig.extension)
                putString("verimor_password", pkg.sipConfig.password)
                putString("verimor_port", pkg.sipConfig.port)
                putBoolean("verimor_auto_timer", pkg.sipConfig.autoTimer)
                putBoolean("verimor_auto_search", pkg.sipConfig.autoSearch)
                putBoolean("verimor_auto_connect", pkg.sipConfig.autoConnect)
            }

            if (pkg.appSettings.globalKullanici.isNotBlank()) {
                putString("global_kullanici", pkg.appSettings.globalKullanici)
            }
            if (pkg.appSettings.globalKullaniciKodu.isNotBlank()) {
                putString("global_kullanici_kodu", pkg.appSettings.globalKullaniciKodu)
            }
            putBoolean("show_advanced_fields", pkg.appSettings.showAdvancedFields)

            if (pkg.appSettings.sqlHost.isNotBlank()) {
                putString("sql_db_type", pkg.appSettings.sqlDbType)
                putString("sql_host", pkg.appSettings.sqlHost)
                putString("sql_port", pkg.appSettings.sqlPort)
                putString("sql_db_name", pkg.appSettings.sqlDbName)
                putString("sql_user", pkg.appSettings.sqlUser)
                putString("sql_password", pkg.appSettings.sqlPassword)
            }
            apply()
        }

        val sipRestored = pkg.sipConfig.domain.isNotBlank() && pkg.sipConfig.extension.isNotBlank()

        return BackupRestoreResult(
            success = true,
            recordsCount = pkg.records.size,
            templatesCount = pkg.templates.size,
            sipRestored = sipRestored,
            message = "Tüm veriler başarıyla geri yüklendi! (${pkg.records.size} kayıt, ${pkg.templates.size} şablon)"
        )
    }

    private fun serializeBackupToJson(pkg: BackupPackage): String {
        val root = JSONObject()
        root.put("version", pkg.version)
        root.put("appName", pkg.appName)
        root.put("backupDate", pkg.backupDate)
        root.put("timestamp", pkg.timestamp)

        // SIP Ayarları
        val sipObj = JSONObject()
        sipObj.put("domain", pkg.sipConfig.domain)
        sipObj.put("extension", pkg.sipConfig.extension)
        sipObj.put("password", pkg.sipConfig.password)
        sipObj.put("port", pkg.sipConfig.port)
        sipObj.put("autoTimer", pkg.sipConfig.autoTimer)
        sipObj.put("autoSearch", pkg.sipConfig.autoSearch)
        sipObj.put("autoConnect", pkg.sipConfig.autoConnect)
        root.put("sipSettings", sipObj)

        // Uygulama Ayarları
        val appObj = JSONObject()
        appObj.put("globalKullanici", pkg.appSettings.globalKullanici)
        appObj.put("globalKullaniciKodu", pkg.appSettings.globalKullaniciKodu)
        appObj.put("showAdvancedFields", pkg.appSettings.showAdvancedFields)
        appObj.put("sqlDbType", pkg.appSettings.sqlDbType)
        appObj.put("sqlHost", pkg.appSettings.sqlHost)
        appObj.put("sqlPort", pkg.appSettings.sqlPort)
        appObj.put("sqlDbName", pkg.appSettings.sqlDbName)
        appObj.put("sqlUser", pkg.appSettings.sqlUser)
        appObj.put("sqlPassword", pkg.appSettings.sqlPassword)
        root.put("appSettings", appObj)

        // Sabit Şablonlar
        val templatesArray = JSONArray()
        for (t in pkg.templates) {
            val tObj = JSONObject()
            tObj.put("id", t.id)
            tObj.put("type", t.type)
            tObj.put("text", t.text)
            templatesArray.put(tObj)
        }
        root.put("templates", templatesArray)

        // Çağrı Kayıtları
        val recordsArray = JSONArray()
        for (r in pkg.records) {
            val rObj = JSONObject()
            rObj.put("id", r.id)
            rObj.put("kullaniciKodu", r.kullaniciKodu)
            rObj.put("kullaniciAdi", r.kullaniciAdi)
            rObj.put("baslangicTarihi", r.baslangicTarihi)
            rObj.put("arayanTuru", r.arayanTuru)
            rObj.put("musteriBayiAdi", r.musteriBayiAdi)
            rObj.put("gorusulenKisi", r.gorusulenKisi)
            rObj.put("telefon", r.telefon)
            rObj.put("telefon2", r.telefon2)
            rObj.put("mail", r.mail)
            rObj.put("iletisimTuru", r.iletisimTuru)
            rObj.put("destekKonusu1", r.destekKonusu1)
            rObj.put("sicilNo1", r.sicilNo1)
            rObj.put("tespit1", r.tespit1)
            rObj.put("islem1", r.islem1)
            rObj.put("baglantiId1", r.baglantiId1)
            rObj.put("bitisTarihi", r.bitisTarihi)
            rObj.put("toplamSure", r.toplamSure)
            rObj.put("kasaVersiyon1", r.kasaVersiyon1)
            rObj.put("arkaOfisVersiyon1", r.arkaOfisVersiyon1)
            rObj.put("redmineId1", r.redmineId1)
            rObj.put("aktif", r.aktif)
            rObj.put("b2bKayit", r.b2bKayit)
            rObj.put("bekleyenIs", r.bekleyenIs)
            rObj.put("argeGidis", r.argeGidis)
            rObj.put("argeDonus", r.argeDonus)
            rObj.put("argeGidisNotu", r.argeGidisNotu)
            rObj.put("argeDonusNotu", r.argeDonusNotu)
            rObj.put("resim", r.resim)
            rObj.put("tckn", r.tckn)
            rObj.put("vergiNo", r.vergiNo)
            rObj.put("tespitKodu1", r.tespitKodu1)
            rObj.put("islemKodu1", r.islemKodu1)
            rObj.put("yn500SicilNo1", r.yn500SicilNo1)
            rObj.put("dynCashVersiyon1", r.dynCashVersiyon1)
            rObj.put("arkaOfisId1", r.arkaOfisId1)
            rObj.put("yn500KasaVersiyon1", r.yn500KasaVersiyon1)
            rObj.put("servisKodu", r.servisKodu)
            rObj.put("not1", r.not1)
            rObj.put("isSynced", r.isSynced)
            rObj.put("syncedAt", r.syncedAt)
            rObj.put("createdAt", r.createdAt)
            recordsArray.put(rObj)
        }
        root.put("records", recordsArray)

        return root.toString(2)
    }

    private fun saveToDownloadsFolder(context: Context, fileName: String, content: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { os ->
                        os.write(content.toByteArray(Charsets.UTF_8))
                    }
                    Log.i(TAG, "✓ Backup file saved to Downloads: $fileName")
                }
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (downloadsDir.exists() || downloadsDir.mkdirs()) {
                    val file = File(downloadsDir, fileName)
                    FileOutputStream(file).use { fos ->
                        fos.write(content.toByteArray(Charsets.UTF_8))
                    }
                    Log.i(TAG, "✓ Backup file saved to Downloads (legacy): $fileName")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not save copy directly to Downloads: ${e.message}")
        }
    }

    /**
     * Günlük otomatik yedekleme (Uygulama açılışında veya güncelleme sonrası yerel güvenlik kopyası)
     * En güncel 7 günlük yedeği `context.filesDir/auto_backups/` altında tutar.
     */
    suspend fun performDailyAutoBackup(context: Context, repository: SupportRepository): File? {
        return try {
            val autoBackupDir = File(context.filesDir, "auto_backups").apply { mkdirs() }
            val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val todayFile = File(autoBackupDir, "auto_backup_$todayDateStr.json")

            if (todayFile.exists() && todayFile.length() > 0) {
                // Bugünün otomatik yedeği zaten alınmış
                return todayFile
            }

            val records = repository.getAllRecordsList()
            val templates = repository.getAllTemplatesList()
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            val sipConfig = SipBackupConfig(
                domain = prefs.getString("sip_domain", "") ?: "",
                extension = prefs.getString("sip_extension", "") ?: "",
                password = prefs.getString("sip_password", "") ?: "",
                port = prefs.getString("sip_port", "5060") ?: "5060",
                autoTimer = prefs.getBoolean("sip_auto_timer", true),
                autoSearch = prefs.getBoolean("sip_auto_search", true),
                autoConnect = prefs.getBoolean("sip_auto_connect", true)
            )

            val appSettings = AppBackupSettings(
                globalKullanici = prefs.getString("global_kullanici", "") ?: "",
                globalKullaniciKodu = prefs.getString("global_kullanici_kodu", "") ?: "",
                showAdvancedFields = prefs.getBoolean("show_advanced_fields", true),
                sqlDbType = prefs.getString("sql_db_type", "MySQL") ?: "MySQL",
                sqlHost = prefs.getString("sql_host", "") ?: "",
                sqlPort = prefs.getString("sql_port", "") ?: "",
                sqlDbName = prefs.getString("sql_db_name", "") ?: "",
                sqlUser = prefs.getString("sql_user", "") ?: "",
                sqlPassword = prefs.getString("sql_password", "") ?: ""
            )

            val backupDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val backupPackage = BackupPackage(
                version = 1,
                appName = "Telefon Destek",
                backupDate = backupDate,
                timestamp = System.currentTimeMillis(),
                sipConfig = sipConfig,
                appSettings = appSettings,
                templates = templates,
                records = records
            )

            val jsonString = serializeBackupToJson(backupPackage)
            FileOutputStream(todayFile).use { fos ->
                fos.write(jsonString.toByteArray(Charsets.UTF_8))
            }

            // 7 günden eski otomatik yedekleri temizle (Hafıza şişmesini önler)
            val files = autoBackupDir.listFiles { file -> file.isFile && file.name.startsWith("auto_backup_") }
            if (files != null && files.size > 7) {
                files.sortedBy { it.lastModified() }
                    .take(files.size - 7)
                    .forEach { it.delete() }
            }

            Log.i(TAG, "✓ Otomatik Günlük Yedek Alındı: ${todayFile.name} (${records.size} kayıt)")
            todayFile
        } catch (e: Exception) {
            Log.e(TAG, "Otomatik günlük yedekleme hatası: ${e.message}", e)
            null
        }
    }

    /**
     * Geçici Excel dosyalarını, paylaşım ve önbellek dosyalarını temizler
     */
    fun clearTempCache(context: Context): Long {
        var freedBytes = 0L
        try {
            // cacheDir
            context.cacheDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    freedBytes += file.length()
                    file.delete()
                } else if (file.isDirectory && file.name != "auto_backups") {
                    freedBytes += deleteDirectoryRecursive(file)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cache temizleme uyarısı: ${e.message}")
        }
        return freedBytes
    }

    private fun deleteDirectoryRecursive(dir: File): Long {
        var bytes = 0L
        dir.listFiles()?.forEach { child ->
            if (child.isDirectory) {
                bytes += deleteDirectoryRecursive(child)
            } else {
                bytes += child.length()
                child.delete()
            }
        }
        dir.delete()
        return bytes
    }

    /**
     * Uygulamanın depolama ve hafıza boyut analizini hesaplar
     */
    suspend fun calculateStorageBreakdown(context: Context, repository: SupportRepository): AppStorageBreakdown {
        return try {
            // 1. Veritabanı Dosyaları Boyutu
            var dbBytes = 0L
            val dbFile = context.getDatabasePath("support_database.db")
            if (dbFile.exists()) dbBytes += dbFile.length()
            val walFile = File(dbFile.parentFile, "support_database.db-wal")
            if (walFile.exists()) dbBytes += walFile.length()
            val shmFile = File(dbFile.parentFile, "support_database.db-shm")
            if (shmFile.exists()) dbBytes += shmFile.length()

            // 2. Kayıt Sayıları (Doğrudan SQL COUNT ile hızlı ve CursorWindow güvenli)
            val totalRecords = repository.getTotalCount()
            val syncedCount = repository.countSyncedRecords()
            val unsyncedCount = (totalRecords - syncedCount).coerceAtLeast(0)

            // 3. Otomatik Yedekleme Klasörü
            var autoBackupBytes = 0L
            var autoBackupCount = 0
            val autoBackupDir = File(context.filesDir, "auto_backups")
            if (autoBackupDir.exists() && autoBackupDir.isDirectory) {
                val files = autoBackupDir.listFiles { f -> f.isFile }
                if (files != null) {
                    autoBackupCount = files.size
                    autoBackupBytes = files.sumOf { it.length() }
                }
            }

            // 4. Manuel Yedekleme Klasörü / Dosyaları
            val backupsDir = File(context.filesDir, "backups")
            if (backupsDir.exists() && backupsDir.isDirectory) {
                val files = backupsDir.listFiles { f -> f.isFile }
                if (files != null) {
                    autoBackupBytes += files.sumOf { it.length() }
                    autoBackupCount += files.size
                }
            }

            // 5. Önbellek ve Geçici Dışa Aktarılan Raporlar (cacheDir)
            var cacheBytes = 0L
            context.cacheDir.listFiles()?.forEach { f ->
                if (f.isFile) cacheBytes += f.length()
                else if (f.isDirectory) cacheBytes += getDirectorySize(f)
            }

            // 6. SharedPreferences & Ek Veriler
            var prefsBytes = 0L
            val sharedPrefsDir = File(context.filesDir.parentFile, "shared_prefs")
            if (sharedPrefsDir.exists() && sharedPrefsDir.isDirectory) {
                sharedPrefsDir.listFiles()?.forEach { if (it.isFile) prefsBytes += it.length() }
            }

            // 7. Temizlenebilir Alan Hesaplaması
            val cleanableCache = cacheBytes
            val cleanableSynced = if (totalRecords > 0) {
                ((syncedCount.toDouble() / totalRecords.toDouble()) * dbBytes).toLong()
            } else 0L
            val cleanableBackups = if (autoBackupCount > 3) (autoBackupBytes * (autoBackupCount - 3) / autoBackupCount) else 0L
            val estimatedTotalCleanable = cleanableCache + cleanableSynced + cleanableBackups
            val totalAppBytes = dbBytes + autoBackupBytes + cacheBytes + prefsBytes

            AppStorageBreakdown(
                dbSizeBytes = dbBytes,
                dbRecordCount = totalRecords,
                unsyncedCount = unsyncedCount,
                syncedCount = syncedCount,
                autoBackupSizeBytes = autoBackupBytes,
                autoBackupCount = autoBackupCount,
                cacheSizeBytes = cacheBytes,
                preferencesSizeBytes = prefsBytes,
                totalAppSizeBytes = totalAppBytes,
                cleanableCacheBytes = cleanableCache,
                cleanableSyncedBytes = cleanableSynced,
                cleanableBackupsBytes = cleanableBackups,
                estimatedTotalCleanableBytes = estimatedTotalCleanable
            )
        } catch (e: Exception) {
            Log.e(TAG, "Depolama analizi hatası: ${e.message}", e)
            AppStorageBreakdown()
        }
    }

    private fun getDirectorySize(dir: File): Long {
        var size = 0L
        dir.listFiles()?.forEach { child ->
            size += if (child.isDirectory) getDirectorySize(child) else child.length()
        }
        return size
    }

    fun formatStorageBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
            mb >= 1.0 -> String.format(Locale.US, "%.2f MB", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
            else -> "$bytes B"
        }
    }
}

/**
 * Uygulama depolama alanı detay raporu
 */
data class AppStorageBreakdown(
    val dbSizeBytes: Long = 0L,
    val dbRecordCount: Int = 0,
    val unsyncedCount: Int = 0,
    val syncedCount: Int = 0,
    val autoBackupSizeBytes: Long = 0L,
    val autoBackupCount: Int = 0,
    val cacheSizeBytes: Long = 0L,
    val preferencesSizeBytes: Long = 0L,
    val totalAppSizeBytes: Long = 0L,
    val cleanableCacheBytes: Long = 0L,
    val cleanableSyncedBytes: Long = 0L,
    val cleanableBackupsBytes: Long = 0L,
    val estimatedTotalCleanableBytes: Long = 0L
)
