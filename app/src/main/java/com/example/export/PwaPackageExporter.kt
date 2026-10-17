package com.example.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.model.SupportRecord
import com.example.model.SupportTemplate
import com.example.ui.theme.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object PwaPackageExporter {

    private val PWA_ASSET_FILES = listOf(
        "index.html",
        "styles.css",
        "app.js",
        "manifest.json",
        "sw.js",
        "icon.svg",
        ".nojekyll",
        "README.md"
    )

    /**
     * Paketlenen GitHub Pages PWA dosyalarını (.zip) olarak hem İndirilenler klasörüne kaydeder
     * hem de paylaşım menüsü ile dışa aktarır.
     */
    fun exportAndSharePwaZip(
        context: Context,
        includeCurrentDatabaseInPwa: Boolean,
        records: List<SupportRecord>,
        templates: List<SupportTemplate>,
        globalKullanici: String,
        globalKullaniciKodu: String
    ) {
        try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val zipFile = File(exportDir, "Telefon_Destek_GitHub_PWA.zip")

            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                for (fileName in PWA_ASSET_FILES) {
                    val bytes = try {
                        context.assets.open("pwa/$fileName").use { it.readBytes() }
                    } catch (_: Exception) {
                        null
                    } ?: continue

                    zos.putNextEntry(ZipEntry(fileName))
                    if (fileName == "app.js" && includeCurrentDatabaseInPwa && records.isNotEmpty()) {
                        val originalJs = String(bytes, Charsets.UTF_8)
                        val seedScript = buildSeedScript(records, templates, globalKullanici, globalKullaniciKodu)
                        val combinedJs = "$originalJs\n\n$seedScript"
                        zos.write(combinedJs.toByteArray(Charsets.UTF_8))
                    } else {
                        zos.write(bytes)
                    }
                    zos.closeEntry()
                }
            }

            // İndirilenler (Downloads) klasörüne kopyala
            saveZipToDownloads(context, zipFile)

            // Sistem Paylaşım Menüsünü Aç
            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, zipFile)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Telefon_Destek_GitHub_PWA.zip")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Telefon Destek - GitHub Pages PWA Kurulum Paketi (.zip). GitHub deponuza yükleyip Pages özelliğini aktif ederek hemen kullanabilirsiniz."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "GitHub PWA Paketini (.zip) İndir / Paylaş")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

            Toast.makeText(
                context,
                "✅ Telefon_Destek_GitHub_PWA.zip İndirilenler klasörüne kaydedildi!",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Toast.makeText(context, "PWA ZIP oluşturma hatası: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun buildSeedScript(
        records: List<SupportRecord>,
        templates: List<SupportTemplate>,
        globalKullanici: String,
        globalKullaniciKodu: String
    ): String {
        val recArr = JSONArray()
        records.forEach { r ->
            val obj = JSONObject().apply {
                put("id", r.id)
                put("kullaniciKodu", r.kullaniciKodu)
                put("kullaniciAdi", r.kullaniciAdi)
                put("baslangicTarihi", r.baslangicTarihi)
                put("arayanTuru", r.arayanTuru)
                put("musteriBayiAdi", r.musteriBayiAdi)
                put("gorusulenKisi", r.gorusulenKisi)
                put("telefon", r.telefon)
                put("telefon2", r.telefon2)
                put("mail", r.mail)
                put("iletisimTuru", r.iletisimTuru)
                put("destekKonusu1", r.destekKonusu1)
                put("sicilNo1", r.sicilNo1)
                put("tespit1", r.tespit1)
                put("islem1", r.islem1)
                put("baglantiId1", r.baglantiId1)
                put("bitisTarihi", r.bitisTarihi)
                put("toplamSure", r.toplamSure)
                put("kasaVersiyon1", r.kasaVersiyon1)
                put("arkaOfisVersiyon1", r.arkaOfisVersiyon1)
                put("redmineId1", r.redmineId1)
                put("aktif", r.aktif)
                put("b2bKayit", r.b2bKayit)
                put("bekleyenIs", r.bekleyenIs)
                put("argeGidis", r.argeGidis)
                put("argeDonus", r.argeDonus)
                put("argeGidisNotu", r.argeGidisNotu)
                put("argeDonusNotu", r.argeDonusNotu)
                put("tckn", r.tckn)
                put("vergiNo", r.vergiNo)
                put("tespitKodu1", r.tespitKodu1)
                put("islemKodu1", r.islemKodu1)
                put("yn500SicilNo1", r.yn500SicilNo1)
                put("dynCashVersiyon1", r.dynCashVersiyon1)
                put("arkaOfisId1", r.arkaOfisId1)
                put("yn500KasaVersiyon1", r.yn500KasaVersiyon1)
                put("servisKodu", r.servisKodu)
                put("not1", r.not1)
            }
            recArr.put(obj)
        }

        val tmpArr = JSONArray()
        templates.forEach { t ->
            tmpArr.put(JSONObject().apply {
                put("id", t.id)
                put("type", t.type)
                put("text", t.text)
            })
        }

        return """
            (function() {
              try {
                if (!localStorage.getItem('td_pwa_records_v1') || JSON.parse(localStorage.getItem('td_pwa_records_v1') || '[]').length === 0) {
                  localStorage.setItem('td_pwa_records_v1', ${JSONObject.quote(recArr.toString())});
                }
                if (!localStorage.getItem('td_pwa_templates_v1')) {
                  localStorage.setItem('td_pwa_templates_v1', ${JSONObject.quote(tmpArr.toString())});
                }
                var st = JSON.parse(localStorage.getItem('td_pwa_settings_v1') || '{}');
                if (!st.globalKullanici) st.globalKullanici = ${JSONObject.quote(globalKullanici)};
                if (!st.globalKullaniciKodu) st.globalKullaniciKodu = ${JSONObject.quote(globalKullaniciKodu)};
                localStorage.setItem('td_pwa_settings_v1', JSON.stringify(st));
              } catch (e) {}
            })();
        """.trimIndent()
    }

    private fun saveZipToDownloads(context: Context, zipFile: File) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, zipFile.name)
                    put(MediaStore.Downloads.MIME_TYPE, "application/zip")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        zipFile.inputStream().use { inp -> inp.copyTo(out) }
                    }
                }
            }
        } catch (_: Exception) {}
    }
}

@Composable
fun PwaPreviewWebViewDialog(
    records: List<SupportRecord>,
    templates: List<SupportTemplate>,
    globalKullanici: String,
    globalKullaniciKodu: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF090D16),
            border = BorderStroke(1.dp, iOSBorder)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = iOSBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Canlı GitHub PWA Önizleme (Tüm Özellikler)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = Color.White
                        )
                    }
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = iOSCardSecondary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Kapat", fontSize = 11.5.sp, color = Color.White)
                    }
                }

                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.allowFileAccess = true
                            settings.allowContentAccess = true
                            webChromeClient = WebChromeClient()
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    val seedJs = PwaPackageExporter.buildSeedScript(
                                        records = records,
                                        templates = templates,
                                        globalKullanici = globalKullanici,
                                        globalKullaniciKodu = globalKullaniciKodu
                                    ) + "\nif (typeof loadAllFromStorage === 'function') { loadAllFromStorage(); renderAllUI(); }"
                                    view?.evaluateJavascript(seedJs, null)
                                }
                            }
                            loadUrl("file:///android_asset/pwa/index.html")
                        }
                    }
                )
            }
        }
    }
}
