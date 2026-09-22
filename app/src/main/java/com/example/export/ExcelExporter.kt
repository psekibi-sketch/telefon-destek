package com.example.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.model.SupportRecord
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ExcelExporter {

    private const val CONTENT_TYPES_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""

    private const val ROOT_RELS_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private const val WORKBOOK_RELS_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

    private const val WORKBOOK_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <bookViews>
    <workbookView xWindow="0" yWindow="0" windowWidth="20480" windowHeight="10240"/>
  </bookViews>
  <sheets>
    <sheet name="Destek Kayıtları" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>"""

    private const val STYLES_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="2">
    <font>
      <sz val="10"/>
      <color theme="1"/>
      <name val="Segoe UI"/>
      <family val="2"/>
    </font>
    <font>
      <b/>
      <sz val="11"/>
      <color rgb="FFFFFFFF"/>
      <name val="Segoe UI"/>
      <family val="2"/>
    </font>
  </fonts>
  <fills count="3">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill>
      <patternFill patternType="solid">
        <fgColor rgb="FF107C41"/>
      </patternFill>
    </fill>
  </fills>
  <borders count="2">
    <border>
      <left/><right/><top/><bottom/><diagonal/>
    </border>
    <border>
      <left style="thin"><color rgb="FFD1D5DB"/></left>
      <right style="thin"><color rgb="FFD1D5DB"/></right>
      <top style="thin"><color rgb="FFD1D5DB"/></top>
      <bottom style="thin"><color rgb="FFD1D5DB"/></bottom>
    </border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="3">
    <!-- 0: Normal Data -->
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyFont="1" applyBorder="1" applyAlignment="1">
      <alignment vertical="center"/>
    </xf>
    <!-- 1: Header Row -->
    <xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center" wrapText="1"/>
    </xf>
    <!-- 2: Alternating Row -->
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyFont="1" applyBorder="1" applyAlignment="1">
      <alignment vertical="center"/>
    </xf>
  </cellXfs>
</styleSheet>"""

    /**
     * Converts column index (1-based: 1 = A, 2 = B, 26 = Z, 27 = AA) to Excel column letter
     */
    private fun getColumnLetter(colIndex: Int): String {
        var temp = colIndex
        val sb = StringBuilder()
        while (temp > 0) {
            temp--
            sb.insert(0, ('A'.code + (temp % 26)).toChar())
            temp /= 26
        }
        return sb.toString()
    }

    private fun escapeXml(text: String): String {
        val sb = StringBuilder()
        for (c in text) {
            when (c) {
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '"' -> sb.append("&quot;")
                '\'' -> sb.append("&apos;")
                else -> {
                    // Filter invalid XML control characters
                    if (c.code >= 32 || c == '\n' || c == '\r' || c == '\t') {
                        sb.append(c)
                    }
                }
            }
        }
        return sb.toString()
    }

    fun generateSheetXml(records: List<SupportRecord>): String {
        val columns = SupportRecord.EXCEL_COLUMNS
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")

        // Column widths for 21 columns
        sb.append("<cols>")
        val colWidths = listOf(
            12, // 1 REDMINE
            20, // 2 BAŞ.TARİHİ
            14, // 3 SİCİL NO
            16, // 4 KULLANICI
            18, // 5 ARAYAN
            22, // 6 MÜŞTERİ/BAYİ
            18, // 7 GÖRÜŞÜLEN
            16, // 8 TELEFON
            15, // 9 İLETİŞİM TÜRÜ
            24, // 10 DESTEK KONU
            28, // 11 TESPİT-1
            28, // 12 İŞLEM-1
            16, // 13 BAĞ.ID-1
            20, // 14 BİT.TARİHİ
            12, // 15 SÜRE
            14, // 16 BEKLEYEN
            14, // 17 B2B KAYIT
            14, // 18 ARGE GİDİŞ
            24, // 19 ARGE GİDİŞ NOTU
            14, // 20 ARGE DÖNÜŞ
            24  // 21 ARGE DÖNÜŞ NOTU
        )

        for (i in columns.indices) {
            val colNum = i + 1
            val width = if (i < colWidths.size) colWidths[i] else 16
            sb.append("""<col min="$colNum" max="$colNum" width="$width" customWidth="1"/>""")
        }
        sb.append("</cols>")

        sb.append("<sheetData>")

        // Header Row (r=1)
        sb.append("""<row r="1" ht="28" customHeight="1">""")
        for (i in columns.indices) {
            val cellRef = "${getColumnLetter(i + 1)}1"
            val text = escapeXml(columns[i])
            sb.append("""<c r="$cellRef" t="inlineStr" s="1"><is><t>$text</t></is></c>""")
        }
        sb.append("</row>")

        // Data Rows
        var rowNum = 2
        for (record in records) {
            val values = record.toExcelRowValues()
            sb.append("""<row r="$rowNum" ht="22" customHeight="1">""")
            for (i in values.indices) {
                val cellRef = "${getColumnLetter(i + 1)}$rowNum"
                val text = escapeXml(values[i])
                sb.append("""<c r="$cellRef" t="inlineStr" s="0"><is><t>$text</t></is></c>""")
            }
            sb.append("</row>")
            rowNum++
        }

        sb.append("</sheetData>")
        sb.append("</worksheet>")
        return sb.toString()
    }

    /**
     * Writes the complete .xlsx file into the provided OutputStream
     */
    fun writeXlsx(outputStream: OutputStream, records: List<SupportRecord>) {
        ZipOutputStream(outputStream).use { zip ->
            // 1. [Content_Types].xml
            zip.putNextEntry(ZipEntry("[Content_Types].xml"))
            zip.write(CONTENT_TYPES_XML.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // 2. _rels/.rels
            zip.putNextEntry(ZipEntry("_rels/.rels"))
            zip.write(ROOT_RELS_XML.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // 3. xl/_rels/workbook.xml.rels
            zip.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
            zip.write(WORKBOOK_RELS_XML.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // 4. xl/workbook.xml
            zip.putNextEntry(ZipEntry("xl/workbook.xml"))
            zip.write(WORKBOOK_XML.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // 5. xl/styles.xml
            zip.putNextEntry(ZipEntry("xl/styles.xml"))
            zip.write(STYLES_XML.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // 6. xl/worksheets/sheet1.xml
            val sheetXml = generateSheetXml(records)
            zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            zip.write(sheetXml.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
    }

    /**
     * Exports records to a cached file and returns its FileProvider Uri
     */
    fun exportToCacheFile(
        context: Context,
        records: List<SupportRecord>,
        filenamePrefix: String = "Destek_Raporu"
    ): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "${filenamePrefix}_$timeStamp.xlsx"
        val exportDir = File(context.cacheDir, "excel_exports").apply { mkdirs() }
        val file = File(exportDir, fileName)

        FileOutputStream(file).use { fos ->
            writeXlsx(fos, records)
        }
        return file
    }

    /**
     * Launches the system share intent for the generated Excel file
     */
    fun shareExcelFile(context: Context, file: File) {
        val authority = "${context.packageName}.fileprovider"
        val contentUri: Uri = FileProvider.getUriForFile(context, authority, file)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            putExtra(Intent.EXTRA_TEXT, "Destek Çağrı Kayıtları Excel Raporu (${file.name})")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Excel Raporunu Paylaş / Aç")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    /**
     * Opens the Excel file in a viewer or spreadsheet app
     */
    fun openExcelFile(context: Context, file: File) {
        val authority = "${context.packageName}.fileprovider"
        val contentUri: Uri = FileProvider.getUriForFile(context, authority, file)

        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(viewIntent)
        } catch (e: Exception) {
            shareExcelFile(context, file)
        }
    }
}
