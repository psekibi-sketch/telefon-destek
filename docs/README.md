# 🎧 Telefon Destek — Tamamen Offline Çalışabilen & Yerel Depolamalı PWA

Android APK olarak kullanılan **Telefon Destek ve Servis Takip Uygulamasının** tüm özelliklerini, ekran tasarımlarını, 2 sekmeli form mimarisini (`1. Çağrı Girişi` / `2. Gelişmiş Alanlar`), 36 sütunlu Excel çıktısını, 4 sekmeli Servis Müdürü Denetim Panelini ve **Ofis İçi Wi-Fi Sync (`http://172.16.160.21:8080/api/sync-calls`)** modülünü birebir barındıran **Progressive Web App (PWA)** projesidir.

---

## 🚀 Öne Çıkan Özellikler

1. **⚡ %100 Offline-First (İnternetsiz Çalışma) & Çift Katmanlı Kalıcı Depolama:**
   - Şirket dışında tamamen internetsiz çalışır (`Service Worker Cache-First` mimarisi).
   - Girilen tüm çağrılar, kullanıcı/temsilci seçimi, SIP ayarları ve şablonlar **hem `IndexedDB` hem de `localStorage`** üzerinde senkronize (`Dual-Layer Persistence`) tutulur ve `navigator.storage.persist()` ile işletim sistemi temizliklerine karşı korunur.
   - Normal tarayıcı sekmesi ile **Ana Ekrana Eklenen PWA Kısayolu** arasında veri kaybı veya scope uyuşmazlığı yaşanmaz (`manifest.json` -> `scope: "./"`, `start_url: "./index.html"`).

2. **📡 Ofis İçi Wi-Fi Sync Modülü (`http://172.16.160.21:8080`):**
   - Şirket dışında offline kaydedilen çağrılar yerel hafızada `Sync Bekliyor` olarak işaretlenir.
   - Ofise gelip yerel Wi-Fi ağına bağlandığınızda **SQL (Bekleyen)** butonuna veya **Ayarlar > Ofis İçi Wi-Fi Sync** ekranındaki **"🚀 Bekleyen Kayıtları Şimdi Gönder (`/api/sync-calls`)"** butonuna basarak tüm offline kayıtları tek tuşla merkezi veritabanına aktarabilirsiniz.
   - Ayrıca **`/api/fetch-calls`** ile SQL Server'dan tarih bazlı (Bugün, Dün & Bugün, Bu Hafta, Bu Ay, Tümü) kayıt çekebilir ve **`/api/test-connection`** ile sunucu bağlantısını test edebilirsiniz.

3. **🎧 Birebir Android APK Ekran ve Form Akışı:**
   - **Üst Bar & Hızlı Araç Çubuğu:** `Rehber`, `Gün Sonu`, `İstatistik`, `Müdür`, `ON/OFF` güç şalteri ve `Excel`, `SQL (Bekleyen)`, `Yedek`, `SIP` kısayolları.
   - **1. Çağrı Girişi:** Sicil No Ön Ek seçici (`PC`, `Beko`, `Profilo`, `Vestel` vb.) + **8 haneli otomatik sıfır tamamlama (`00000005`)**, Müşteri/Bayi, Görüşülen Kişi, Telefon, Müşterinin Önceki Çağrıları Kartı, Hızlı Çözüm Şablonları, Destek Konusu, **Çoklu Seçimli Tespit & İşlem Çipleri**, **Sesli Dikte (🎙️)** ve **Görüşme İçi Hızlı Not (Karalama Defteri)**.
   - **2. Gelişmiş Alanlar (36 Sütun):** İletişim Türü çipleri, Telefon 2, E-Posta, Arayan Türü, Bekleyen Çağrı, Redmine No, AnyDesk Bağlantı ID, B2B Kayıt No, Başlangıç/Bitiş/Süre, AR-GE Gidiş/Dönüş ve Cihaz Donanım/Versiyon (`YNOKC`, `DynCash`, `YN500`, `Arka Ofis`, `TCKN`, `Vergi No`) alanları.
   - **🛡️ Form Koruma & Bekleyen Çağrı Kuyruğu:** Siz mevcut çağrı formunu doldururken yeni bir çağrı gelirse yazılarınız asla silinmez; yeni çağrı üstteki **Bekleyen Çağrı Kuyruğu** bandına alınır.

4. **👔 4 Sekmeli Servis Müdürü & Yönetici Denetim Paneli:**
   - **Temsilciler:** Temsilci bazlı çağrı, çözüm oranı ve ortalama süre sıralaması.
   - **Grafikler:** Konu bazlı yoğunluk barları.
   - **Çağrı Listesi:** Kayıtları filtreleyip tek tıkla inceleme/düzenleme.
   - **Rapor Önizleme:** Tek tuşla kopyalanabilir veya WhatsApp'ta paylaşılabilir yönetici raporu.

---

## 🌐 GitHub Pages Üzerinde Yayınlama

1. Bu depoyu GitHub hesabınıza yükleyin (veya Android APK içindeki **Ayarlar > 11. GitHub Pages PWA Sürümü > PWA (.zip) İndir** butonuyla aldığınız dosyaları yeni bir GitHub deposuna atın).
2. GitHub deponuzda **Settings > Pages** menüsüne girin.
3. **Branch** olarak `main` ve klasör olarak `/docs` (veya dosyaları ana dizine attıysanız `/ (root)`) seçip **Save** butonuna basın.
4. Telefonunuzdan oluşan bağlantıyı açıp **"Ana Ekrana Ekle (Add to Home Screen)"** diyerek tamamen offline çalışan uygulama olarak kullanabilirsiniz.
