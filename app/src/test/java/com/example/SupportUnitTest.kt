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
        assertEquals(36, headers.size)
        assertEquals("KULLANICIKODU", headers[0])
        assertEquals("BASLANGICTARIHI", headers[1])
        assertEquals("ARAYANTURU", headers[2])
        assertEquals("MUSTERIBAYIADI", headers[3])
        assertEquals("GORUSULENKISI", headers[4])
        assertEquals("TELEFON", headers[5])
    }

    @Test
    fun testExcelPhoneNumberCleaned() {
        val record = SupportRecord(
            telefon = "0532 123 45 67"
        )
        val rowValues = record.toExcelRowValues()
        assertEquals("05321234567", rowValues[5])
    }

    @Test
    fun testDateFiltering() {
        val record1 = SupportRecord(
            id = 1,
            baslangicTarihi = "2026-09-10 10:00:00",
            musteriBayiAdi = "Test AŞ",
            destekKonusu1 = "E-Fatura"
        )
        val record2 = SupportRecord(
            id = 2,
            baslangicTarihi = "2026-09-15 14:00:00",
            musteriBayiAdi = "Örnek Ltd",
            destekKonusu1 = "Yazıcı"
        )
        val record3 = SupportRecord(
            id = 3,
            baslangicTarihi = "2026-09-20 09:30:00",
            musteriBayiAdi = "Gelişim AŞ",
            destekKonusu1 = "Lisans"
        )

        val list = listOf(record1, record2, record3)

        val startDate = "2026-09-12"
        val endDate = "2026-09-18"

        val filtered = list.filter { record ->
            val recordDate = record.basTarihi.take(10)
            recordDate in startDate..endDate
        }

        assertEquals(1, filtered.size)
        assertEquals("Örnek Ltd", filtered[0].musteriBayiAdi)
    }
}
