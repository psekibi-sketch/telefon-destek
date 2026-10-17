# 🌐 Telefon Destek — GitHub Pages PWA Sürümü

Bu klasör (`/docs`), **Telefon Destek** Android uygulamasının tüm özelliklerini barındıran, internet olmadan da (Offline Service Worker ile) çalışabilen **Progressive Web App (PWA)** sürümüdür.

## 🚀 Özellikler
- **Tam Destek Kayıt Formu (36 Sütunlu SQL/Excel Şeması):** Sicil No, Müşteri/Bayi, Görüşülen Kişi, Telefon 1/2, Destek Konusu, Tespit, Yapılan İşlem, YNÖKC / DynCash / YN500 / Arka Ofis Versiyonları, AR-GE ve Bekleyen İş takibi.
- **Canlı Çağrı Kronometresi & Verimor SIP Tuş Takımı:** Süre hesaplama, DTMF tuş sesi ve hızlı arama.
- **Hızlı Çözüm Senaryoları & Bilgi Bankası:** Tek tıkla arıza tespit/çözüm doldurma ve sesle yazma (Web Speech API).
- **Gelişmiş Raporlar:**
  - 📊 **İstatistik & Analiz Raporu** (En Çok Aranan 5 Konu, En Çok Yapılan 5 İşlem, Temsilci Performansı)
  - 📋 **Gün Sonu Vardiya Raporu** (WhatsApp & Panoya Kopyalama)
  - 🛠️ **Cihaz / Sicil Servis Karnesi Raporu**
  - 👔 **Servis Müdürü Yönetici Paneli**
  - 📥 **36 Sütunlu Gerçek Microsoft Excel (`.xlsx`) Çıktısı** (Tamamen çevrimdışı çalışır)
- **Android APK ile %100 Uyumlu JSON Yedekleme & Geri Yükleme:** Android telefondan alınan `Destek_Yedek_*.json` yedeği doğrudan PWA'ya yüklenebilir veya PWA'dan alınan yedek Android uygulamasına aktarılabilir.

---

## 🛠️ GitHub Pages Üzerinde Yayınlama (2 Kolay Yöntem)

### Yöntem 1: Mevcut Proje Üzerinden (Orijinal APK + PWA Birlikte)
1. Projeyi GitHub hesabınıza aktarın (Push to GitHub).
2. GitHub deponuzda **Settings > Pages** menüsüne girin.
3. **Build and deployment** altında:
   - **Source:** `Deploy from a branch` seçin, **Branch:** `main` ve klasör olarak **`/docs`** seçip **Save** butonuna basın *(veya `GitHub Actions` seçerek otomatik workflow'u kullanın)*.
4. Birkaç saniye içinde `https://kullaniciadiniz.github.io/depo-adi/` adresinde PWA uygulamanız yayına girecektir!

### Yöntem 2: Ayrı / Yeni Bir GitHub Projesi Olarak Yayınlama
1. GitHub'da yeni bir repository açın (Örn: `telefon-destek-pwa`).
2. Bu `/docs` klasörünün içindeki 6 dosyayı (`index.html`, `styles.css`, `app.js`, `manifest.json`, `sw.js`, `icon.svg`, `.nojekyll`) doğrudan yeni deponun ana dizinine yükleyin *(veya Android uygulaması içindeki **Ayarlar > GitHub PWA Paketi İndir (.zip)** butonundan ZIP olarak indirip çıkartın)*.
3. **Settings > Pages** altından `main` / `/ (root)` seçip kaydedin.
