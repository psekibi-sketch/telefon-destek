package com.example.network

import com.example.model.SupportRecord
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class SyncCallItem(
    @Json(name = "id") val id: Long,
    @Json(name = "KULLANICIKODU") val kullaniciKodu: String,
    @Json(name = "KULLANICIADI") val kullaniciAdi: String = "",
    @Json(name = "kullanici_kodu") val userCode: String = "",
    @Json(name = "kullanici_id") val userId: String = "",
    @Json(name = "kullanici_adi") val userName: String = "",
    @Json(name = "BASLANGICTARIHI") val baslangicTarihi: String,
    @Json(name = "ARAYANTURU") val arayanTuru: String,
    @Json(name = "MUSTERIBAYIADI") val musteriBayiAdi: String,
    @Json(name = "GORUSULENKISI") val gorusulenKisi: String,
    @Json(name = "TELEFON") val telefon: String,
    @Json(name = "TELEFON2") val telefon2: String,
    @Json(name = "MAIL") val mail: String,
    @Json(name = "ILETISIMTURU") val iletisimTuru: String,
    @Json(name = "DESTEKKONUSU1") val destekKonusu1: String,
    @Json(name = "SICILNO1") val sicilNo1: String,
    @Json(name = "TESPIT1") val tespit1: String,
    @Json(name = "ISLEM1") val islem1: String,
    @Json(name = "BAGLANTIID1") val baglantiId1: String,
    @Json(name = "BITISTARIHI") val bitisTarihi: String,
    @Json(name = "TOPLAMSURE") val toplamSure: String,
    @Json(name = "KASAVERSIYON1") val kasaVersiyon1: String,
    @Json(name = "ARKAOFISVERSIYON1") val arkaOfisVersiyon1: String,
    @Json(name = "REDMINEID1") val redmineId1: String,
    @Json(name = "AKTIF") val aktif: String,
    @Json(name = "B2BKAYIT") val b2bKayit: String,
    @Json(name = "BEKLEYENIS") val bekleyenIs: String,
    @Json(name = "ARGEGIDIS") val argeGidis: String,
    @Json(name = "ARGEDONUS") val argeDonus: String,
    @Json(name = "ARGEGIDISNOTU") val argeGidisNotu: String,
    @Json(name = "ARGEDONUSNOTU") val argeDonusNotu: String,
    @Json(name = "RESIM") val resim: String,
    @Json(name = "TCKN") val tckn: String,
    @Json(name = "VERGINO") val vergiNo: String,
    @Json(name = "TESPITKODU1") val tespitKodu1: String,
    @Json(name = "ISLEMKODU1") val islemKodu1: String,
    @Json(name = "YN500SICILNO1") val yn500SicilNo1: String,
    @Json(name = "DYNCASHVERSIYON1") val dynCashVersiyon1: String,
    @Json(name = "ARKAOFISID1") val arkaOfisId1: String,
    @Json(name = "YN500KASAVERSIYON1") val yn500KasaVersiyon1: String,
    @Json(name = "NOT1") val not1: String,
    @Json(name = "created_at") val createdAt: Long
)

@JsonClass(generateAdapter = true)
data class SyncCallsRequest(
    @Json(name = "records") val records: List<SyncCallItem>
)

@JsonClass(generateAdapter = true)
data class SyncCallsResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String?,
    @Json(name = "synced_ids") val syncedIds: List<Long>?
)

@JsonClass(generateAdapter = true)
data class TestDbConnectionRequest(
    @Json(name = "db_type") val dbType: String,
    @Json(name = "host") val host: String,
    @Json(name = "port") val port: String,
    @Json(name = "db_name") val dbName: String,
    @Json(name = "user") val user: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class TestDbConnectionResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String?
)

@JsonClass(generateAdapter = true)
data class SearchCustomerRequest(
    @Json(name = "sicil_no") val sicilNo: String = "",
    @Json(name = "telefon") val telefon: String = "",
    @Json(name = "gorusulen") val gorusulen: String = "",
    @Json(name = "query") val query: String = ""
)

@JsonClass(generateAdapter = true)
data class CustomerSearchResult(
    @Json(name = "id") val id: Long = 0,
    @Json(name = "MUSTERIBAYIADI") val musteriBayiAdi: String = "",
    @Json(name = "SICILNO1") val sicilNo1: String = "",
    @Json(name = "GORUSULENKISI") val gorusulenKisi: String = "",
    @Json(name = "TELEFON") val telefon: String = "",
    @Json(name = "TELEFON2") val telefon2: String = "",
    @Json(name = "MAIL") val mail: String = "",
    @Json(name = "KASAVERSIYON1") val kasaVersiyon1: String = "",
    @Json(name = "DYNCASHVERSIYON1") val dynCashVersiyon1: String = "",
    @Json(name = "YN500KASAVERSIYON1") val yn500KasaVersiyon1: String = "",
    @Json(name = "ARKAOFISVERSIYON1") val arkaOfisVersiyon1: String = "",
    @Json(name = "SERVISKODU") val servisKodu: String = "",
    @Json(name = "BASLANGICTARIHI") val baslangicTarihi: String = "",
    @Json(name = "DESTEKKONUSU1") val destekKonusu1: String = ""
)

@JsonClass(generateAdapter = true)
data class SearchCustomerResponse(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "message") val message: String? = null,
    @Json(name = "results") val results: List<CustomerSearchResult>? = null
)

fun SupportRecord.toSyncCallItem(): SyncCallItem {
    val cleanPhone = telefon.replace(Regex("\\s+"), "").replace("-", "").replace("(", "").replace(")", "").trim()
    val cleanPhone2 = telefon2.replace(Regex("\\s+"), "").replace("-", "").replace("(", "").replace(")", "").trim()
    val trLocale = java.util.Locale("tr", "TR")
    val finalCode = kullaniciKodu.trim().ifBlank { kullaniciAdi.trim() }
    val finalName = kullaniciAdi.trim().ifBlank { kullaniciKodu.trim() }
    return SyncCallItem(
        id = id,
        kullaniciKodu = finalCode,
        kullaniciAdi = finalName,
        userCode = finalCode,
        userId = finalCode,
        userName = finalName,
        baslangicTarihi = baslangicTarihi,
        arayanTuru = arayanTuru,
        musteriBayiAdi = musteriBayiAdi.uppercase(trLocale),
        gorusulenKisi = gorusulenKisi.uppercase(trLocale),
        telefon = cleanPhone,
        telefon2 = cleanPhone2,
        mail = mail,
        iletisimTuru = iletisimTuru,
        destekKonusu1 = destekKonusu1,
        sicilNo1 = sicilNo1.uppercase(trLocale),
        tespit1 = tespit1,
        islem1 = islem1,
        baglantiId1 = baglantiId1,
        bitisTarihi = bitisTarihi,
        toplamSure = toplamSure,
        kasaVersiyon1 = kasaVersiyon1,
        arkaOfisVersiyon1 = arkaOfisVersiyon1,
        redmineId1 = redmineId1,
        aktif = aktif,
        b2bKayit = b2bKayit,
        bekleyenIs = bekleyenIs,
        argeGidis = argeGidis,
        argeDonus = argeDonus,
        argeGidisNotu = argeGidisNotu,
        argeDonusNotu = argeDonusNotu,
        resim = resim,
        tckn = tckn,
        vergiNo = vergiNo,
        tespitKodu1 = tespitKodu1,
        islemKodu1 = islemKodu1,
        yn500SicilNo1 = yn500SicilNo1,
        dynCashVersiyon1 = dynCashVersiyon1,
        arkaOfisId1 = arkaOfisId1,
        yn500KasaVersiyon1 = yn500KasaVersiyon1,
        not1 = not1.ifBlank { servisKodu },
        createdAt = createdAt
    )
}

interface SyncApiService {
    @POST("api/sync-calls")
    suspend fun syncCalls(@Body request: SyncCallsRequest): Response<SyncCallsResponse>

    @POST("api/test-db-connection")
    suspend fun testDbConnection(@Body request: TestDbConnectionRequest): Response<TestDbConnectionResponse>

    @POST("api/search-customers")
    suspend fun searchCustomers(@Body request: SearchCustomerRequest): Response<SearchCustomerResponse>
}

object SyncApiClient {
    private var currentBaseUrl: String = ""
    private var apiService: SyncApiService? = null

    fun getService(baseUrl: String): SyncApiService {
        val trimmed = baseUrl.trim()
        require(trimmed.isNotBlank()) { "API sunucu adresi (Host / IP) boş bırakılamaz." }
        val normalizedUrl = if (trimmed.endsWith("/")) trimmed else "$trimmed/"
        if (apiService == null || currentBaseUrl != normalizedUrl) {
            currentBaseUrl = normalizedUrl
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(normalizedUrl)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()

            apiService = retrofit.create(SyncApiService::class.java)
        }
        return apiService!!
    }
}
