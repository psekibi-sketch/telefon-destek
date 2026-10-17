package com.example.data

import com.example.model.SupportRecord
import com.example.model.SupportTemplate
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SupportRepository(
    private val dao: SupportDao,
    private val templateDao: TemplateDao
) {

    val allRecords: Flow<List<SupportRecord>> = dao.getAllRecords()
    val unsyncedCount: Flow<Int> = dao.getUnsyncedCount()

    suspend fun getAllRecordsList(): List<SupportRecord> = dao.getAllRecordsList()

    suspend fun getUnsyncedRecords(): List<SupportRecord> = dao.getUnsyncedRecords()

    suspend fun markAsSynced(ids: List<Long>, syncedAt: Long = System.currentTimeMillis()) {
        dao.markAsSynced(ids, syncedAt)
    }

    suspend fun getLatestBySicilNo(sicilNo: String): SupportRecord? = dao.getLatestBySicilNo(sicilNo)

    suspend fun searchRecords(sicilNo: String, telefon: String, gorusulen: String): List<SupportRecord> {
        return dao.searchRecords(sicilNo.trim(), telefon.trim(), gorusulen.trim())
    }

    suspend fun searchBySicilNo(query: String): List<SupportRecord> = dao.searchBySicilNo(query.trim())

    suspend fun searchByPhone(query: String): List<SupportRecord> = dao.searchByPhone(query.trim())

    suspend fun searchByGorusulen(query: String): List<SupportRecord> = dao.searchByGorusulen(query.trim())

    suspend fun searchByMusteriBayi(query: String): List<SupportRecord> = dao.searchByMusteriBayi(query.trim())

    suspend fun searchAllFields(query: String): List<SupportRecord> = dao.searchAllFields(query.trim())

    suspend fun getRecordsBetweenDates(startDateTime: String, endDateTime: String): List<SupportRecord> {
        return dao.getRecordsBetweenDates(startDateTime, endDateTime)
    }

    suspend fun insert(record: SupportRecord): Long {
        return dao.insert(record)
    }

    suspend fun update(record: SupportRecord) {
        dao.update(record)
    }

    suspend fun delete(record: SupportRecord) {
        dao.delete(record)
    }

    suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }

    suspend fun deleteRecordsBetweenDates(startDate: String, endDate: String): Int {
        val startDateTime = "$startDate 00:00:00"
        val endDateTime = "$endDate 23:59:59"
        return dao.deleteRecordsBetweenDates(startDateTime, endDateTime)
    }

    suspend fun countRecordsBetweenDates(startDate: String, endDate: String): Int {
        val startDateTime = "$startDate 00:00:00"
        val endDateTime = "$endDate 23:59:59"
        return dao.countRecordsBetweenDates(startDateTime, endDateTime)
    }

    suspend fun deleteAll() {
        dao.deleteAll()
    }

    // --- TEMPLATES ---

    fun getTemplatesByType(type: String): Flow<List<SupportTemplate>> {
        return templateDao.getTemplatesByType(type)
    }

    suspend fun insertTemplate(type: String, text: String): Long {
        if (text.isBlank()) return -1
        return templateDao.insert(SupportTemplate(type = type, text = text.trim()))
    }

    suspend fun deleteTemplate(template: SupportTemplate) {
        templateDao.delete(template)
    }

    suspend fun deleteTemplateById(id: Long) {
        templateDao.deleteById(id)
    }

    suspend fun seedDefaultTemplatesIfEmpty() {
        if (templateDao.getCount() == 0) {
            val defaultTespitler = listOf(
                "GİB portal şifre süresi dolmuş, servis 401 Unauthorized dönüyor.",
                "Kullanıcı yetki seviyesi yetersiz olduğundan modül açılmıyor.",
                "Veritabanı transaction kilitlenmesi (Deadlock) nedeniyle oturum askıda kaldı.",
                "COM port çakışması ve güncel olmayan sürücü tespit edildi.",
                "Lisans anahtarı süresi dolmuş veya geçersiz.",
                "Yazıcı kuyruğunda biriken bozuk belgeler nedeniyle çıktı alınamıyor.",
                "E-İrsaliye şema doğrulama hatası (XML schema validation error).",
                "Ağ bağlantısı kopması sonucu SQL sunucuya erişim sağlanamadı.",
                "Dönem sonu kur farkı tablosunda katsayı uyuşmazlığı tespit edildi."
            )

            val defaultIslemler = listOf(
                "Müşteri ile portal şifresi yenilendi, API parametreleri güncellendi ve test başarılı.",
                "Kullanıcı rol ve yetki tanımları düzenlendi, ekran yetkisi verildi.",
                "Kilitli SP_ID'ler sonlandırıldı, servis yeniden başlatıldı ve mutabakat sağlandı.",
                "Sürücü güncellendi, USB port adresi sabitlendi, test okumaları yapıldı.",
                "Yeni lisans anahtarı girildi ve sunucu aktivasyonu onaylandı.",
                "Yazdırma biriktiricisi (Spooler) servisi durdurulup kuyruk temizlendi, test sayfası basıldı.",
                "Firma e-irsaliye şablonu güncellendi ve yeniden onay gönderildi.",
                "DNS ve IP yapılandırması kontrol edildi, güvenlik duvarı kuralı eklendi.",
                "Rapor hesaplama tablosu yeniden derlendi ve test dökümü alındı."
            )

            val defaultKonular = listOf(
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

            for (t in defaultTespitler) {
                templateDao.insert(SupportTemplate(type = "TESPIT", text = t))
            }
            for (i in defaultIslemler) {
                templateDao.insert(SupportTemplate(type = "ISLEM", text = i))
            }
            for (k in defaultKonular) {
                templateDao.insert(SupportTemplate(type = "KONU", text = k))
            }
        }
    }

    // --- SAMPLE DATA INITIALIZATION (REMOVED) ---

    suspend fun seedSampleDataIfEmpty() {
        seedDefaultTemplatesIfEmpty()
        // Otomatik demo çağrı kayıtları oluşturma mekanizması tamamen kaldırılmıştır.
        // Kullanıcının onayı olmadan hiçbir demo/sahte kayıt eklenmez.
    }
}
