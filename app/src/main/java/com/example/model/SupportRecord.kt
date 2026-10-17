package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "support_records")
data class SupportRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Core SQL Columns matching exact database schema:
    val kullaniciKodu: String = "",        // KULLANICIKODU (ID / Kod)
    val kullaniciAdi: String = "",         // KULLANICIADI (Temsilci Adı Soyadı)
    val baslangicTarihi: String = "",      // BASLANGICTARIHI (YYYY-MM-DD HH:mm:ss)
    val arayanTuru: String = "",           // ARAYANTURU (Müşteri, Bayi, vb.)
    val musteriBayiAdi: String = "",       // MUSTERIBAYIADI (UPPERCASE)
    val gorusulenKisi: String = "",        // GORUSULENKISI (UPPERCASE)
    val telefon: String = "",              // TELEFON (No spaces)
    val telefon2: String = "",             // TELEFON2 (No spaces)
    val mail: String = "",                 // MAIL
    val iletisimTuru: String = "Telefon",  // ILETISIMTURU
    val destekKonusu1: String = "",        // DESTEKKONUSU1 (Dropdown)
    val sicilNo1: String = "",             // SICILNO1 (UPPERCASE)
    val tespit1: String = "",              // TESPIT1
    val islem1: String = "",               // ISLEM1
    val baglantiId1: String = "",          // BAGLANTIID1 (AnyDesk vb.)
    val bitisTarihi: String = "",          // BITISTARIHI (YYYY-MM-DD HH:mm:ss)
    val toplamSure: String = "",           // TOPLAMSURE (Örn: 00:04:35)
    val kasaVersiyon1: String = "",        // KASAVERSIYON1 (YNOKC Versiyon)
    val arkaOfisVersiyon1: String = "",    // ARKAOFISVERSIYON1
    val redmineId1: String = "",           // REDMINEID1
    val aktif: String = "1",               // AKTIF
    val b2bKayit: String = "",             // B2BKAYIT
    val bekleyenIs: String = "Hayır",      // BEKLEYENIS
    val argeGidis: String = "Hayır",       // ARGEGIDIS
    val argeDonus: String = "Hayır",       // ARGEDONUS
    val argeGidisNotu: String = "",        // ARGEGIDISNOTU
    val argeDonusNotu: String = "",        // ARGEDONUSNOTU
    val resim: String = "",                // RESIM
    val tckn: String = "",                 // TCKN
    val vergiNo: String = "",              // VERGINO
    val tespitKodu1: String = "",          // TESPITKODU1
    val islemKodu1: String = "",           // ISLEMKODU1
    val yn500SicilNo1: String = "",        // YN500SICILNO1
    val dynCashVersiyon1: String = "",     // DYNCASHVERSIYON1
    val arkaOfisId1: String = "",          // ARKAOFISID1
    val yn500KasaVersiyon1: String = "",   // YN500KASAVERSIYON1
    val servisKodu: String = "",           // SERVISKODU / NOT1
    val not1: String = "",                 // NOT1
    val audioUri: String = "",             // AUDIOURI (Çağrı Ses Kaydı Dosya Yolu)

    // Offline-First & SQL Synchronization Fields
    val isSynced: Boolean = false,         // is_synced: false/true
    val syncedAt: Long? = null,            // Senkronizasyon zaman damgası
    val createdAt: Long = System.currentTimeMillis()
) {
    // Legacy getters/compatibility mapping if needed in UI or exports:
    val sicilNo: String get() = sicilNo1
    val basTarihi: String get() = baslangicTarihi
    val bitTarihi: String get() = bitisTarihi
    val sure: String get() = toplamSure
    val kullanici: String get() = kullaniciAdi.ifBlank { kullaniciKodu }
    val arayan: String get() = arayanTuru
    val musteriBayi: String get() = musteriBayiAdi
    val gorusulen: String get() = gorusulenKisi
    val destekKonu: String get() = destekKonusu1
    val tespit: String get() = tespit1
    val islem: String get() = islem1
    val bagId: String get() = baglantiId1
    val redmine: String get() = redmineId1
    val bekleyen: String get() = bekleyenIs
    val ynokcVersiyon: String get() = kasaVersiyon1
    val dynCashVersiyon: String get() = dynCashVersiyon1
    val yn500Versiyon: String get() = yn500KasaVersiyon1
    val arkaOfisVersiyon: String get() = arkaOfisVersiyon1

    companion object {
        val EXCEL_COLUMNS = listOf(
            "KULLANICIKODU",
            "BASLANGICTARIHI",
            "ARAYANTURU",
            "MUSTERIBAYIADI",
            "GORUSULENKISI",
            "TELEFON",
            "TELEFON2",
            "MAIL",
            "ILETISIMTURU",
            "DESTEKKONUSU1",
            "SICILNO1",
            "TESPIT1",
            "ISLEM1",
            "BAGLANTIID1",
            "BITISTARIHI",
            "TOPLAMSURE",
            "KASAVERSIYON1",
            "ARKAOFISVERSIYON1",
            "REDMINEID1",
            "AKTIF",
            "B2BKAYIT",
            "BEKLEYENIS",
            "ARGEGIDIS",
            "ARGEDONUS",
            "ARGEGIDISNOTU",
            "ARGEDONUSNOTU",
            "RESIM",
            "TCKN",
            "VERGINO",
            "TESPITKODU1",
            "ISLEMKODU1",
            "YN500SICILNO1",
            "DYNCASHVERSIYON1",
            "ARKAOFISID1",
            "YN500KASAVERSIYON1",
            "NOT1"
        )
    }

    fun toExcelRowValues(): List<String> {
        val cleanTelefon = telefon.replace(Regex("\\s+"), "").replace("-", "").replace("(", "").replace(")", "").trim()
        val cleanTelefon2 = telefon2.replace(Regex("\\s+"), "").replace("-", "").replace("(", "").replace(")", "").trim()
        val trLocale = java.util.Locale("tr", "TR")
        val displayUser = when {
            kullaniciKodu.isNotBlank() && kullaniciAdi.isNotBlank() -> "$kullaniciKodu - $kullaniciAdi"
            kullaniciKodu.isNotBlank() -> kullaniciKodu
            else -> kullaniciAdi
        }
        return listOf(
            displayUser,
            baslangicTarihi,
            arayanTuru,
            musteriBayiAdi.uppercase(trLocale),
            gorusulenKisi.uppercase(trLocale),
            cleanTelefon,
            cleanTelefon2,
            mail,
            iletisimTuru,
            destekKonusu1,
            sicilNo1.uppercase(trLocale),
            tespit1,
            islem1,
            baglantiId1,
            bitisTarihi,
            toplamSure,
            kasaVersiyon1,
            arkaOfisVersiyon1,
            redmineId1,
            aktif,
            b2bKayit,
            bekleyenIs,
            argeGidis,
            argeDonus,
            argeGidisNotu,
            argeDonusNotu,
            resim,
            tckn,
            vergiNo,
            tespitKodu1,
            islemKodu1,
            yn500SicilNo1,
            dynCashVersiyon1,
            arkaOfisId1,
            yn500KasaVersiyon1,
            not1.ifBlank { servisKodu }
        )
    }
}
