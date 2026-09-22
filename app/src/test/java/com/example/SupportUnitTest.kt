package com.example

import com.example.export.ExcelExporter
import com.example.model.SupportRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportUnitTest {

    @Test
    fun testExcelColumnsCountAndHeaders() {
        val headers = SupportRecord.EXCEL_COLUMNS
        assertEquals(21, headers.size)
        assertEquals("REDMINE", headers[0])
        assertEquals("BASLAMATARIHI", headers[1])
        assertEquals("SICILNO1", headers[2])
        assertEquals("KULLANICI", headers[3])
        assertEquals("ARAYAN", headers[4])
        assertEquals("MUSTERIBAYI", headers[5])
        assertEquals("GORUSULEN", headers[6])
        assertEquals("TELEFON", headers[7])
        assertEquals("ILETISIMTURU", headers[8])
        assertEquals("DESTEKKONU", headers[9])
        assertEquals("TESPIT-1", headers[10])
        assertEquals("ISLEM-1", headers[11])
        assertEquals("BAG.ID-1", headers[12])
        assertEquals("BITISTARIHI", headers[13])
        assertEquals("SURE", headers[14])
        assertEquals("BEKLEYEN", headers[15])
        assertEquals("B2BKAYIT", headers[16])
        assertEquals("ARGEGIDIS", headers[17])
        assertEquals("ARGEGIDISNOTU", headers[18])
        assertEquals("ARGEDONUS", headers[19])
        assertEquals("ARGEDONUSNOTU", headers[20])
    }

    @Test
    fun testExcelPhoneNumberCleaned() {
        val record = SupportRecord(
            telefon = "0532 123 45 67"
        )
        val rowValues = record.toExcelRowValues()
        assertEquals("05321234567", rowValues[7])
    }

    @Test
    fun testDateFiltering() {
        val record1 = SupportRecord(
            id = 1,
            basTarihi = "2026-09-10 10:00:00",
            musteriBayi = "Test AŞ",
            destekKonu = "E-Fatura"
        )
        val record2 = SupportRecord(
            id = 2,
            basTarihi = "2026-09-15 14:00:00",
            musteriBayi = "Örnek Ltd",
            destekKonu = "Yazıcı"
        )
        val record3 = SupportRecord(
            id = 3,
            basTarihi = "2026-09-20 09:30:00",
            musteriBayi = "Gelişim AŞ",
            destekKonu = "Lisans"
        )

        val list = listOf(record1, record2, record3)

        val startDate = "2026-09-12"
        val endDate = "2026-09-18"

        val filtered = list.filter { record ->
            val recordDate = record.basTarihi.take(10)
            recordDate in startDate..endDate
        }

        assertEquals(1, filtered.size)
        assertEquals("Örnek Ltd", filtered[0].musteriBayi)
    }
}
