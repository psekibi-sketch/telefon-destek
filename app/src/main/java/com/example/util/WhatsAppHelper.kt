package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.model.SupportRecord

object WhatsAppHelper {

    fun formatPhoneNumber(rawPhone: String): String {
        var clean = rawPhone.replace(Regex("[^0-9]"), "").trim()
        if (clean.startsWith("0")) {
            clean = "9$clean"
        } else if (clean.length == 10 && !clean.startsWith("90")) {
            clean = "90$clean"
        }
        return clean
    }

    fun generateServiceSlipText(
        firma: String,
        sicilNo: String,
        gorusulen: String,
        destekKonusu: String,
        tespit: String,
        islem: String,
        temsilci: String,
        tarih: String,
        sure: String
    ): String = buildString {
        append("Sayın Müşterimiz / Bayimiz,\n\n")
        append("📋 *TELEFON DESTEK SERVİS BİLGİLENDİRMESİ*\n")
        append("━━━━━━━━━━━━━━━━━━━━\n")
        if (firma.isNotBlank()) append("🏢 *Firma:* $firma\n")
        if (sicilNo.isNotBlank()) append("📟 *Sicil No:* $sicilNo\n")
        if (gorusulen.isNotBlank()) append("👤 *Görüşülen:* $gorusulen\n")
        if (destekKonusu.isNotBlank()) append("📌 *Destek Konusu:* $destekKonusu\n")
        if (tespit.isNotBlank()) append("🔍 *Tespit:* $tespit\n")
        if (islem.isNotBlank()) append("✅ *Yapılan İşlem:* $islem\n")
        if (temsilci.isNotBlank()) append("👨‍💼 *Destek Temsilcisi:* $temsilci\n")
        val cleanTarih = tarih.take(16).ifBlank { "Bugün" }
        val cleanSure = if (sure.isNotBlank()) " ($sure)" else ""
        append("🕒 *Tarih:* $cleanTarih$cleanSure\n")
        append("━━━━━━━━━━━━━━━━━━━━\n")
        append("Çağrınız başarıyla tamamlanmıştır. İyi çalışmalar dileriz.")
    }

    fun shareRecord(context: Context, record: SupportRecord, representativeName: String) {
        val phone = formatPhoneNumber(record.telefon.ifBlank { record.telefon2 })
        val text = generateServiceSlipText(
            firma = record.musteriBayiAdi,
            sicilNo = record.sicilNo1,
            gorusulen = record.gorusulenKisi,
            destekKonusu = record.destekKonusu1,
            tespit = record.tespit1,
            islem = record.islem1,
            temsilci = representativeName.ifBlank { record.kullaniciAdi },
            tarih = record.baslangicTarihi,
            sure = record.toplamSure
        )
        launchWhatsAppOrChooser(context, phone, text)
    }

    fun shareForm(
        context: Context,
        phoneRaw: String,
        firma: String,
        sicilNo: String,
        gorusulen: String,
        destekKonusu: String,
        tespit: String,
        islem: String,
        temsilci: String,
        tarih: String,
        sure: String
    ) {
        val phone = formatPhoneNumber(phoneRaw)
        val text = generateServiceSlipText(
            firma = firma,
            sicilNo = sicilNo,
            gorusulen = gorusulen,
            destekKonusu = destekKonusu,
            tespit = tespit,
            islem = islem,
            temsilci = temsilci,
            tarih = tarih,
            sure = sure
        )
        launchWhatsAppOrChooser(context, phone, text)
    }

    fun generateKnowledgeArticleInstructionText(
        article: com.example.model.KnowledgeArticle,
        customerName: String = ""
    ): String = buildString {
        if (customerName.isNotBlank()) append("Sayın $customerName,\n\n") else append("Sayın Müşterimiz,\n\n")
        append("🛠️ *TEKNİK DESTEK ÇÖZÜM ADIMLARI*\n")
        append("━━━━━━━━━━━━━━━━━━━━\n")
        append("📌 *Konu:* ${article.title}\n")
        if (article.errorCode.isNotBlank()) append("⚠️ *Hata Kodu:* ${article.errorCode}\n")
        if (article.cause.isNotBlank()) append("🔍 *Olası Sebep:* ${article.cause}\n\n")
        append("📝 *Lütfen Aşağıdaki Adımları Sırasıyla Uygulayınız:*\n")
        article.steps.forEachIndexed { idx, step ->
            append("${idx + 1}. $step\n")
        }
        append("━━━━━━━━━━━━━━━━━━━━\n")
        append("Sorun devam ederse lütfen çağrı merkezimize bilgi veriniz.\nİyi çalışmalar dileriz.")
    }

    fun shareKnowledgeArticle(
        context: Context,
        article: com.example.model.KnowledgeArticle,
        phoneRaw: String,
        customerName: String = ""
    ) {
        val phone = formatPhoneNumber(phoneRaw)
        val text = generateKnowledgeArticleInstructionText(article, customerName)
        launchWhatsAppOrChooser(context, phone, text)
    }

    fun shareShiftReport(context: Context, reportText: String) {
        launchWhatsAppOrChooser(context, "", reportText)
    }

    fun shareShiftReportWithOptionalExcel(
        context: Context,
        reportText: String,
        excelFile: java.io.File? = null
    ) {
        if (excelFile != null && excelFile.exists()) {
            try {
                val fileUri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    excelFile
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    putExtra(Intent.EXTRA_TEXT, reportText)
                    putExtra(Intent.EXTRA_SUBJECT, "Gün Sonu / Vardiya Raporu & Excel Tablosu")
                    setPackage("com.whatsapp")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(shareIntent)
            } catch (_: Exception) {
                // Fallback to system chooser with both file and text
                try {
                    val fileUri = androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        excelFile
                    )
                    val chooserIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        putExtra(Intent.EXTRA_STREAM, fileUri)
                        putExtra(Intent.EXTRA_TEXT, reportText)
                        putExtra(Intent.EXTRA_SUBJECT, "Gün Sonu / Vardiya Raporu & Excel Tablosu")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(Intent.createChooser(chooserIntent, "Rapor ve Excel Dosyasını Paylaş"))
                } catch (_: Exception) {
                    shareShiftReport(context, reportText)
                }
            }
        } else {
            shareShiftReport(context, reportText)
        }
    }

    private fun launchWhatsAppOrChooser(context: Context, formattedPhone: String, message: String) {
        val uri = if (formattedPhone.isNotBlank()) {
            Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(message)}")
        } else {
            Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(message)}")
        }

        val directIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            context.startActivity(directIntent)
        } catch (_: Exception) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(shareIntent, "Servis Bilgisini Paylaş"))
        }
    }
}
