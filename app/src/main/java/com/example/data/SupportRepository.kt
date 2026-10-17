package com.example.data

import com.example.model.MissedCallRecord
import com.example.model.SupportRecord
import com.example.model.SupportTemplate
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SupportRepository(
    private val dao: SupportDao,
    private val templateDao: TemplateDao,
    private val missedCallDao: MissedCallDao
) {

    val allRecords: Flow<List<SupportRecord>> = dao.getAllRecords()
    val unsyncedCount: Flow<Int> = dao.getUnsyncedCount()
    val allMissedCalls: Flow<List<MissedCallRecord>> = missedCallDao.getAllMissedCalls()

    suspend fun getAllRecordsList(): List<SupportRecord> {
        val list = mutableListOf<SupportRecord>()
        var offset = 0
        val pageSize = 250
        while (true) {
            val chunk = dao.getRecordsPaged(limit = pageSize, offset = offset)
            if (chunk.isEmpty()) break
            list.addAll(chunk)
            if (chunk.size < pageSize) break
            offset += pageSize
        }
        return list
    }

    suspend fun getTotalCount(): Int = dao.getTotalCount()

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

    suspend fun insertAll(records: List<SupportRecord>): List<Long> {
        if (records.isEmpty()) return emptyList()
        // Deduplicate incoming list first
        val uniqueIncoming = records.distinctBy { generateRecordSignature(it) }

        // Fetch existing records safely using chunked list
        val existing = getAllRecordsList()
        val existingMap = existing.associateBy { generateRecordSignature(it) }

        val toInsert = mutableListOf<SupportRecord>()
        val insertedIds = mutableListOf<Long>()

        for (rec in uniqueIncoming) {
            val sig = generateRecordSignature(rec)
            val match = existingMap[sig]
            if (match != null) {
                // Already in local DB: update existing row and preserve local flags
                dao.update(rec.copy(id = match.id, isSynced = true))
                insertedIds.add(match.id)
            } else {
                toInsert.add(rec)
            }
        }

        if (toInsert.isNotEmpty()) {
            // Insert in chunks of 200 to avoid any SQLite parameter or transaction limits
            for (chunk in toInsert.chunked(200)) {
                val newIds = dao.insertAll(chunk)
                insertedIds.addAll(newIds)
            }
        }
        return insertedIds
    }

    fun generateRecordSignature(r: SupportRecord): String {
        val redmine = r.redmineId1.trim()
        val bagId = r.baglantiId1.trim()
        if (redmine.isNotBlank() && redmine != "0" && redmine != "-") {
            return "redmine_$redmine"
        }
        if (bagId.isNotBlank() && bagId != "0" && bagId != "-") {
            val sic = r.sicilNo1.trim().ifBlank { r.yn500SicilNo1.trim() }.uppercase(Locale.ROOT)
            return "bag_${bagId}_$sic"
        }
        val dateNorm = normalizeDateForSignature(r.baslangicTarihi.ifBlank { r.basTarihi })
        val sicil = r.sicilNo1.trim().uppercase(Locale.ROOT)
        val yn500 = r.yn500SicilNo1.trim().uppercase(Locale.ROOT)
        val tel = r.telefon.filter { it.isDigit() }.takeLast(10)
        val musteri = r.musteriBayiAdi.trim().lowercase(Locale("tr", "TR"))
        val gorusulen = r.gorusulenKisi.trim().lowercase(Locale("tr", "TR"))
        val konu = r.destekKonusu1.trim().lowercase(Locale("tr", "TR"))
        val tespit = r.tespit1.trim().lowercase(Locale("tr", "TR"))
        return "call_${dateNorm}_${sicil}_${yn500}_${tel}_${musteri}_${gorusulen}_${konu}_${tespit}"
    }

    private fun normalizeDateForSignature(raw: String): String {
        val clean = raw.trim()
        if (clean.isBlank()) return ""
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
                if (d != null) {
                    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT).format(d)
                }
            } catch (_: Exception) {}
        }
        val dotMatch = Regex("""^(\d{2})\.(\d{2})\.(\d{4})(.*)""").find(clean)
        if (dotMatch != null) {
            val (d, m, y, rest) = dotMatch.destructured
            return "$y-$m-$d${rest.take(6).trim()}"
        }
        return clean.take(16)
    }

    suspend fun removeDuplicateRecords(): Int {
        return try {
            val all = getAllRecordsList()
            val seen = mutableSetOf<String>()
            val duplicatesToDelete = mutableListOf<Long>()

            // Keep the latest ID for each unique record, mark older duplicates for deletion
            for (r in all.sortedByDescending { it.id }) {
                val sig = generateRecordSignature(r)
                if (seen.contains(sig)) {
                    duplicatesToDelete.add(r.id)
                } else {
                    seen.add(sig)
                }
            }

            for (id in duplicatesToDelete) {
                dao.deleteById(id)
            }
            duplicatesToDelete.size
        } catch (_: Exception) {
            0
        }
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

    suspend fun deleteSyncedRecords(): Int {
        return dao.deleteSyncedRecords()
    }

    suspend fun deleteSyncedRecordsOlderThan(daysAgo: Int): Int {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -daysAgo)
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        val cutoff = sdf.format(cal.time)
        return dao.deleteSyncedRecordsOlderThan(cutoff)
    }

    suspend fun deleteSyncedRecordsBetweenDates(startDate: String, endDate: String): Int {
        val startDateTime = "$startDate 00:00:00"
        val endDateTime = "$endDate 23:59:59"
        return dao.deleteSyncedRecordsBetweenDates(startDateTime, endDateTime)
    }

    suspend fun countSyncedRecords(): Int {
        return dao.countSyncedRecords()
    }

    suspend fun countSyncedRecordsOlderThan(daysAgo: Int): Int {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DAY_OF_YEAR, -daysAgo)
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        val cutoff = sdf.format(cal.time)
        return dao.countSyncedRecordsOlderThan(cutoff)
    }

    suspend fun deleteAll() {
        dao.deleteAll()
    }

    suspend fun insertAllRecords(records: List<SupportRecord>): List<Long> {
        return dao.insertAll(records)
    }

    // --- MISSED CALLS ---

    suspend fun insertMissedCall(record: MissedCallRecord): Long {
        val tenSecondsAgo = record.timestamp - 10000L
        val recent = missedCallDao.findRecentCall(record.callerNumber, tenSecondsAgo)
        if (recent != null && (recent.callType == record.callType || recent.timestamp > tenSecondsAgo)) {
            // Son 10 saniye içinde aynı numaradan çağrı zaten kaydedilmişse mükerrer oluşturma
            return recent.id
        }
        return missedCallDao.insert(record)
    }

    suspend fun cleanDuplicateCalls() {
        try {
            val all = missedCallDao.getAllMissedCallsList()
            val seen = mutableSetOf<String>()
            for (item in all) {
                // Numara + Çağrı Türü + 10 saniyelik zaman dilimine göre mükerrer kopyaları temizle
                val timeBucket = item.timestamp / 10000L
                val key = "${item.callerNumber}_${item.callType}_$timeBucket"
                if (seen.contains(key)) {
                    missedCallDao.deleteById(item.id)
                } else {
                    seen.add(key)
                }
            }
        } catch (_: Exception) {}
    }

    suspend fun deleteMissedCall(id: Long) {
        missedCallDao.deleteById(id)
    }

    suspend fun deleteAllMissedCalls() {
        missedCallDao.deleteAll()
    }

    suspend fun markMissedCallHandled(id: Long) {
        missedCallDao.markHandled(id)
    }

    suspend fun markMissedCallHandledByNumber(number: String) {
        val clean = number.replace(Regex("\\D"), "").trim()
        if (clean.isNotBlank()) {
            missedCallDao.markHandledByNumber(clean)
        }
    }

    suspend fun deleteMissedCallByNumber(number: String) {
        val clean = number.replace(Regex("\\D"), "").trim()
        if (clean.isNotBlank()) {
            missedCallDao.deleteByNumber(clean)
        }
    }

    // --- TEMPLATES ---

    fun getTemplatesByType(type: String): Flow<List<SupportTemplate>> {
        return templateDao.getTemplatesByType(type)
    }

    suspend fun getAllTemplatesList(): List<SupportTemplate> {
        return templateDao.getAllTemplatesList()
    }

    suspend fun insertAllTemplates(templates: List<SupportTemplate>): List<Long> {
        return templateDao.insertAll(templates)
    }

    suspend fun deleteAllTemplates() {
        templateDao.deleteAll()
    }

    suspend fun insertTemplate(type: String, text: String): Long {
        if (text.isBlank()) return -1
        return templateDao.insert(SupportTemplate(type = type, text = text.trim()))
    }

    suspend fun addTemplate(type: String, text: String): Long = insertTemplate(type, text)

    suspend fun deleteTemplate(template: SupportTemplate) {
        templateDao.delete(template)
    }

    suspend fun deleteTemplate(id: Long) {
        templateDao.deleteById(id)
    }

    suspend fun deleteTemplateById(id: Long) {
        templateDao.deleteById(id)
    }

    suspend fun seedDefaultTemplatesIfEmpty() {
        // Requirement 10: "Dahili varsayılan başlıklar ve ön ekler yüklenmesin; liste tamamen boş başlasın."
        // Kullanıcı kendi şablon ve ön eklerini Ayarlar'dan manuel yönetir. Otomatik hiçbir demo başlık eklenmez.
    }

    suspend fun seedSampleDataIfEmpty() {
        // No sample data
    }
}
