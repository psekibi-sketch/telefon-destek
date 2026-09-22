package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "support_records")
data class SupportRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Exact 21 Excel Columns:
    val redmine: String = "",          // 1. REDMINE
    val basTarihi: String = "",        // 2. BAŞ.TARİHİ (YYYY-MM-DD HH:mm:ss)
    val sicilNo: String = "",          // 3. SİCİL NO
    val kullanici: String = "",        // 4. KULLANICI
    val arayan: String = "",           // 5. ARAYAN
    val musteriBayi: String = "",      // 6. MÜŞTERİ/BAYİ
    val gorusulen: String = "",        // 7. GÖRÜŞÜLEN
    val telefon: String = "",          // 8. TELEFON
    val iletisimTuru: String = "",     // 9. İLETİŞİM TÜRÜ
    val destekKonu: String = "",       // 10. DESTEK KONU
    val tespit: String = "",           // 11. TESPİT-1
    val islem: String = "",            // 12. İŞLEM-1
    val bagId: String = "",            // 13. BAĞ.ID-1
    val bitTarihi: String = "",        // 14. BİT.TARİHİ (YYYY-MM-DD HH:mm:ss)
    val sure: String = "",             // 15. SÜRE (Örn: 00:04:35)
    val bekleyen: String = "",         // 16. BEKLEYEN
    val b2bKayit: String = "",         // 17. B2B KAYIT
    val argeGidis: String = "",        // 18. ARGE GİDİŞ
    val argeGidisNotu: String = "",    // 19. ARGE GİDİŞ NOTU
    val argeDonus: String = "",        // 20. ARGE DÖNÜŞ
    val argeDonusNotu: String = "",    // 21. ARGE DÖNÜŞ NOTU
    
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        val EXCEL_COLUMNS = listOf(
            "REDMINE",
            "BASLAMATARIHI",
            "SICILNO1",
            "KULLANICI",
            "ARAYAN",
            "MUSTERIBAYI",
            "GORUSULEN",
            "TELEFON",
            "ILETISIMTURU",
            "DESTEKKONU1",
            "TESPIT1",
            "ISLEM1",
            "BAGID1",
            "BITISTARIHI",
            "SURE",
            "BEKLEYEN",
            "B2BKAYIT",
            "ARGEGIDIS",
            "ARGEGIDISNOTU",
            "ARGEDONUS",
            "ARGEDONUSNOTU"
        )
    }

    fun toExcelRowValues(): List<String> {
        val cleanTelefon = telefon.replace(Regex("\\s+"), "").replace("-", "").replace("(", "").replace(")", "").trim()
        val trLocale = java.util.Locale("tr", "TR")
        return listOf(
            redmine,
            basTarihi,
            sicilNo.uppercase(trLocale),
            kullanici,
            arayan,
            musteriBayi.uppercase(trLocale),
            gorusulen.uppercase(trLocale),
            cleanTelefon,
            iletisimTuru,
            destekKonu,
            tespit,
            islem,
            bagId,
            bitTarihi,
            sure,
            bekleyen,
            b2bKayit,
            argeGidis,
            argeGidisNotu,
            argeDonus,
            argeDonusNotu
        )
    }
}
