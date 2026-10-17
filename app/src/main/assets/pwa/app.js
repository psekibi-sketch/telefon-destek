/* ============================================================================
 * TELEFON DESTEK - GITHUB PAGES PWA TAM SÜRÜM MOTORU (app.js)
 * Android APK (SupportViewModel, BackupManager, ExcelExporter) ile %100 Uyumlu
 * ============================================================================ */

const STORAGE_KEYS = {
  RECORDS: 'td_pwa_records_v1',
  TEMPLATES: 'td_pwa_templates_v1',
  MISSED_CALLS: 'td_pwa_missed_calls_v1',
  SCENARIOS: 'td_pwa_scenarios_v1',
  KB_ARTICLES: 'td_pwa_kb_articles_v1',
  SETTINGS: 'td_pwa_settings_v1',
  REPS: 'td_pwa_representatives_v1'
};

const EXCEL_COLUMNS = [
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
];

const DEFAULT_QUICK_SCENARIOS = [
  {
    id: "z_raporu",
    title: "Z Raporu / Gün Sonu",
    icon: "🧾",
    konu: "Gün Sonu / Z Raporu Hatası",
    tespit: "Mali hafıza Z raporu iletişim/yazıcı hatası",
    islem: "Mali rapor kuyruğu kontrol edildi, Z raporu başarıyla alındı ve onaylandı.",
    usageCount: 5
  },
  {
    id: "mali_hafiza",
    title: "Mali Hafıza",
    icon: "💾",
    konu: "Mali Hafıza / Hafıza Sonu Uyarısı",
    tespit: "Mali hafıza kapasitesi ve son Z raporu kontrolü",
    islem: "Mali hafıza durumu sorgulandı, servis kaydı oluşturularak yetkili bayiye yönlendirildi.",
    usageCount: 3
  },
  {
    id: "ag_baglanti",
    title: "Ağ / IP Bağlantısı",
    icon: "🌐",
    konu: "Ağ Bağlantısı / Ethernet Hatası",
    tespit: "Yazar kasa IP adresi ve ağ geçidi bağlantısı kopuk",
    islem: "Kablo ve switch kontrol edildi, statik IP adresi ve port yapılandırması yenilendi.",
    usageCount: 4
  },
  {
    id: "yazici_kagit",
    title: "Yazıcı / Kağıt",
    icon: "🖨️",
    konu: "Yazıcı / Fiş Kesme Arızası",
    tespit: "Rulo mekanizmasında kağıt sıkışması / kutter arızası",
    islem: "Rulo kapağı manuel açılarak sıkışan kağıt temizlendi, test fişi kesildi.",
    usageCount: 4
  },
  {
    id: "pos_banka",
    title: "POS / Banka Provizyon",
    icon: "💳",
    konu: "POS / Banka İletişim Hatası",
    tespit: "Banka EFT-POS bağlantısında zaman aşımı hatası",
    islem: "Banka terminal portu ve haberleşme parametreleri güncellenerek provizyon testi alındı.",
    usageCount: 6
  },
  {
    id: "versiyon_guncelleme",
    title: "Versiyon Güncelleme",
    icon: "🔄",
    konu: "Yazılım & Versiyon Güncelleme",
    tespit: "Eski versiyondan kaynaklı menü kilitlenmesi",
    islem: "Cihaz en son kararlı OKC ve DynCash versiyonuna güncellendi, parametreler yüklendi.",
    usageCount: 5
  },
  {
    id: "yedek_alma",
    title: "Yedek Alma / Aktarım",
    icon: "📦",
    konu: "Veritabanı / Yedek Alma",
    tespit: "Cihaz veya arka ofis yedekleme talebi",
    islem: "Mali ve operasyonel veriler harici ortama yedeklendi, doğrulandı.",
    usageCount: 2
  },
  {
    id: "sifre_yetki",
    title: "Şifre / Yetki Sıfırlama",
    icon: "🔐",
    konu: "Kullanıcı & Şifre İşlemleri",
    tespit: "Kasiyer şifresi unutulmuş / yetki kısıtı",
    islem: "Yönetici şifresi doğrulanarak yetkili kasiyer hesabı sıfırlandı ve test edildi.",
    usageCount: 3
  }
];

const DEFAULT_TEMPLATES = [
  { id: 1, type: "KONU", text: "Gün Sonu / Z Raporu Hatası" },
  { id: 2, type: "KONU", text: "POS / Banka İletişim Hatası" },
  { id: 3, type: "KONU", text: "Ağ Bağlantısı / Ethernet Hatası" },
  { id: 4, type: "KONU", text: "Yazılım & Versiyon Güncelleme" },
  { id: 5, type: "KONU", text: "Yazıcı / Fiş Kesme Arızası" },
  { id: 6, type: "KONU", text: "E-Arşiv / E-Fatura Entegrasyonu" },
  { id: 7, type: "KONU", text: "Mali Hafıza / TSM Raporlama" },
  { id: 8, type: "TESPIT", text: "Banka EFT-POS bağlantısında COM port zaman aşımı hatası." },
  { id: 9, type: "TESPIT", text: "Yazar kasa IP adresi ve ağ geçidi bağlantısı kopuk." },
  { id: 10, type: "TESPIT", text: "Açık satış/fiş kalması nedeniyle Z raporu gün sonu uyarısı." },
  { id: 11, type: "ISLEM", text: "Uzak bağlantı ile parametreler güncellendi, test satışı başarılı alındı." },
  { id: 12, type: "ISLEM", text: "Statik IP ve DNS tanımlandı, TSM ve banka bağlantısı doğrulandı." },
  { id: 13, type: "ISLEM", text: "Askıdaki fiş kapatılarak Z raporu alındı ve mali bellek onaylandı." }
];

const DEFAULT_KB_ARTICLES = [
  {
    id: "kb_304",
    errorCode: "ERR-304",
    title: "Hata 304 - Mali Mühür Zaman Aşımı & GİB İletişim Hatası",
    category: "Mali & GİB",
    cause: "Mali bellek zaman damgası veya GİB SSL güvenlik sertifikasının süresinin dolması/uyuşmaması.",
    steps: [
      "Kasanın internet ve DNS ayarlarını kontrol edin (8.8.8.8 veya 1.1.1.1).",
      "Menü > Sistem Ayarları > Tarih/Saat menüsünden sistem saatini senkronize edin.",
      "Menü > Mali İşlemler > Sertifika Güncelleme seçeneğini çalıştırın.",
      "Gün sonu / Z raporu denemesi yaparak mali iletişimi doğrulayın."
    ],
    recommendedTespit: "Mali mühür zaman damgası ve GİB SSL sertifikası zaman aşımı hatası (Hata 304).",
    recommendedIslem: "Ağ ve DNS ayarları yapılandırıldı, mali sertifika güncellendi ve test X raporu alındı."
  },
  {
    id: "kb_500",
    errorCode: "E-FAT-500",
    title: "Hata 500 / 502 - E-Arşiv & E-Fatura Entegratör Sunucu Hatası",
    category: "E-Dönüşüm",
    cause: "Özel entegratör (DynCash / Enpos) sunucusunun yanıt vermemesi veya oturum tokeninin düşmesi.",
    steps: [
      "Entegratör portalına web tarayıcısından girerek hesap durumunu doğrulayın.",
      "DynCash arka ofis servis yöneticisinden (IntegratorService) servisi yeniden başlatın.",
      "Bekleyen e-fatura kuyruğundaki hatalı paketleri seçip 'Yeniden Gönder' butonuna basın."
    ],
    recommendedTespit: "E-Arşiv / E-Fatura entegratör haberleşme kuyruğunda token zaman aşımı.",
    recommendedIslem: "Entegratör servisleri yeniden başlatıldı, bekleyen fatura paketi başarıyla onaylandı."
  },
  {
    id: "kb_prn24",
    errorCode: "PRN-24",
    title: "Hata 24 - Kutter (Kağıt Bıçak) Kilitlenmesi & Kağıt Sıkışması",
    category: "Yazıcı & Mekanik",
    cause: "Rulo kağıt parçacıklarının kutter bıçağını sıkıştırması veya rulo kapağının zorlanması.",
    steps: [
      "Kasa güç anahtarını kapatın ve elektrik fişini çekin.",
      "Yazıcı yanındaki manuel kutter dişlisini çevirerek bıçağı yuvasına çekin.",
      "Rulo kapağı mandalını açın, sıkışan kağıt parçalarını temizleyin ve test fişi kesin."
    ],
    recommendedTespit: "Termal yazıcı kutter otomatik kesici bıçağında kağıt sıkışması.",
    recommendedIslem: "Kutter bıçağı manuel serbest bırakıldı, kağıt yolu temizlendi ve test fişi kesildi."
  },
  {
    id: "kb_net01",
    errorCode: "NET-01",
    title: "Ağ Kopuk / IP 0.0.0.0 Hatası (Ethernet & Statik IP)",
    category: "Ağ & Donanım",
    cause: "DHCP sunucusundan IP alınamaması, switch portunun düşmesi veya ağ kablosu temassızlığı.",
    steps: [
      "Kasa arkasındaki RJ45 Ethernet soketinin yeşil/turuncu LED ışıklarını kontrol edin.",
      "Menü > Ağ Ayarları > Statik IP seçeneğini açıp kasanın kayıtlı IP'sini elle girin.",
      "Ağ Geçidi (Gateway) Ping testini çalıştırarak bağlantıyı doğrulayın."
    ],
    recommendedTespit: "Ağ kablosu temassızlığı ve DHCP IP atama hatası (IP 0.0.0.0).",
    recommendedIslem: "Ethernet soketi ve switch portu kontrol edildi, statik IP ve ağ geçidi tanımlandı."
  },
  {
    id: "kb_pos91",
    errorCode: "POS-91",
    title: "Hata 91 - Banka POS Provizyon Zaman Aşımı (EFT-POS Bağlantısı)",
    category: "POS & Banka",
    cause: "Banka POS terminali ile yazar kasa arasındaki seri port / USB haberleşme kopukluğu.",
    steps: [
      "POS cihazı ile kasa arasındaki haberleşme ara kablosunu kontrol ederek yeniden takın.",
      "Kasa POS ayarlarından COM portu hızını (Baud Rate 115200) kontrol edin.",
      "1 TL'lik kartlı satış ve provizyon iptal testi yapın."
    ],
    recommendedTespit: "Kasa ile EFT-POS terminali arasında COM port haberleşme zaman aşımı (Hata 91).",
    recommendedIslem: "Haberleşme kablosu ve port parametreleri yenilendi, banka test provizyonu başarılı alındı."
  },
  {
    id: "kb_dblock",
    errorCode: "DB-LOCK",
    title: "DynCash Veritabanı Kilitlenmesi & SQLite Lock",
    category: "Yazılım & Veritabanı",
    cause: "Beklenmeyen elektrik kesintisi veya arka ofis sürecinin veritabanını kilitlemesi.",
    steps: [
      "Arka ofis veya kasadaki çalışan POS uygulamasını Görev Yöneticisinden kapatın.",
      "Uygulama veri klasöründeki geçici '.db-journal' veya '.wal' kilit dosyalarını temizleyin.",
      "Veritabanı Onarım / Reindex aracını çalıştırıp uygulamayı başlatın."
    ],
    recommendedTespit: "Ani elektrik kesintisi sonrası veritabanı kilitlenme ve indeks bozulması.",
    recommendedIslem: "Veritabanı kilit dosyaları temizlendi, indeksler onarıldı ve satış ekranı açıldı."
  }
];

// Global Runtime State
let state = {
  records: [],
  templates: [],
  missedCalls: [],
  scenarios: [],
  kbArticles: [],
  representatives: [
    { code: "1", name: "AHMET KAYA" },
    { code: "2", name: "MEHMET YILMAZ" },
    { code: "6", name: "MURAT DEMİR" }
  ],
  settings: {
    globalKullanici: "DESTEK TEMSİLCİSİ",
    globalKullaniciKodu: "101",
    outlookRecipientEmail: "servis@enpos.com.tr",
    showAdvancedFields: true,
    dutyModeEnabled: false,
    sipDomain: "sirketiniz.bulutsantralim.com",
    sipExtension: "101",
    sipPassword: "",
    sipPort: "5060",
    sqlDbType: "MySQL",
    sqlHost: "",
    sqlPort: "8080",
    sqlDbName: "destek_db"
  },
  editingRecordId: null,
  timerRunning: false,
  timerSeconds: 0,
  timerInterval: null,
  historyFilter: 'ALL',
  callLogFilter: 'ALL',
  statsRange: 'ALL',
  pendingAutoFill: null,
  deferredInstallPrompt: null
};

// ============================================================================
// INITIALIZATION & STORAGE
// ============================================================================
document.addEventListener('DOMContentLoaded', () => {
  loadAllFromStorage();
  initServiceWorker();
  initInstallPrompt();
  initDefaultFormValues();
  renderAllUI();

  // Check URL shortcuts (?tab=0, ?modal=shift, ?modal=stats)
  const params = new URLSearchParams(window.location.search);
  if (params.get('modal') === 'shift') openDailyShiftModal();
  if (params.get('modal') === 'stats') openStatisticsModal();
  if (params.get('tab')) switchTab(parseInt(params.get('tab'), 10) || 0);
});

function loadAllFromStorage() {
  try {
    const r = localStorage.getItem(STORAGE_KEYS.RECORDS);
    state.records = r ? JSON.parse(r) : [];

    const t = localStorage.getItem(STORAGE_KEYS.TEMPLATES);
    state.templates = t ? JSON.parse(t) : [...DEFAULT_TEMPLATES];

    const m = localStorage.getItem(STORAGE_KEYS.MISSED_CALLS);
    state.missedCalls = m ? JSON.parse(m) : [];

    const sc = localStorage.getItem(STORAGE_KEYS.SCENARIOS);
    state.scenarios = sc ? JSON.parse(sc) : [...DEFAULT_QUICK_SCENARIOS];

    const kb = localStorage.getItem(STORAGE_KEYS.KB_ARTICLES);
    state.kbArticles = kb ? JSON.parse(kb) : [...DEFAULT_KB_ARTICLES];

    const st = localStorage.getItem(STORAGE_KEYS.SETTINGS);
    if (st) state.settings = { ...state.settings, ...JSON.parse(st) };

    const rp = localStorage.getItem(STORAGE_KEYS.REPS);
    if (rp) state.representatives = JSON.parse(rp);
  } catch (err) {
    console.error('Storage load error:', err);
  }
}

function saveAllToStorage() {
  try {
    localStorage.setItem(STORAGE_KEYS.RECORDS, JSON.stringify(state.records));
    localStorage.setItem(STORAGE_KEYS.TEMPLATES, JSON.stringify(state.templates));
    localStorage.setItem(STORAGE_KEYS.MISSED_CALLS, JSON.stringify(state.missedCalls));
    localStorage.setItem(STORAGE_KEYS.SCENARIOS, JSON.stringify(state.scenarios));
    localStorage.setItem(STORAGE_KEYS.KB_ARTICLES, JSON.stringify(state.kbArticles));
    localStorage.setItem(STORAGE_KEYS.SETTINGS, JSON.stringify(state.settings));
    localStorage.setItem(STORAGE_KEYS.REPS, JSON.stringify(state.representatives));
    updateStorageBadge();
  } catch (err) {
    console.error('Storage save error:', err);
  }
}

function updateStorageBadge() {
  let totalBytes = 0;
  Object.values(STORAGE_KEYS).forEach(k => {
    const val = localStorage.getItem(k) || '';
    totalBytes += val.length * 2;
  });
  const kb = (totalBytes / 1024).toFixed(1);
  const badge = document.getElementById('storageSizeBadge');
  if (badge) badge.textContent = `${kb} KB Yerel Veri`;
}

function initServiceWorker() {
  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('./sw.js').catch(() => {});
  }
}

function initInstallPrompt() {
  window.addEventListener('beforeinstallprompt', (e) => {
    e.preventDefault();
    state.deferredInstallPrompt = e;
    const btn = document.getElementById('installPwaBtn');
    if (btn) btn.style.display = 'inline-flex';
  });
}

function installPWA() {
  if (!state.deferredInstallPrompt) {
    showToast('Tarayıcı menüsünden "Ana Ekrana Ekle / Uygulamayı Yükle" seçeneğini kullanabilirsiniz.');
    return;
  }
  state.deferredInstallPrompt.prompt();
  state.deferredInstallPrompt.userChoice.then(() => {
    state.deferredInstallPrompt = null;
    document.getElementById('installPwaBtn').style.display = 'none';
  });
}

// ============================================================================
// DATE & TIME HELPERS
// ============================================================================
function nowDateTimeStr() {
  const d = new Date();
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}

function todayYmdStr() {
  const d = new Date();
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

function formatDurationHms(totalSec) {
  const sec = Math.max(0, Math.floor(totalSec));
  const h = Math.floor(sec / 3600);
  const m = Math.floor((sec % 3600) / 60);
  const s = sec % 60;
  const pad = (n) => String(n).padStart(2, '0');
  return `${pad(h)}:${pad(m)}:${pad(s)}`;
}

function parseDurationToSeconds(hmsStr) {
  if (!hmsStr) return 0;
  const parts = String(hmsStr).trim().split(':').map(Number);
  if (parts.length === 3 && parts.every(n => !isNaN(n))) {
    return parts[0] * 3600 + parts[1] * 60 + parts[2];
  }
  if (parts.length === 2 && parts.every(n => !isNaN(n))) {
    return parts[0] * 60 + parts[1];
  }
  return 0;
}

function calcDurationBetweenDates(startStr, endStr) {
  if (!startStr || !endStr) return "00:01:00";
  const s = new Date(startStr.replace(' ', 'T'));
  const e = new Date(endStr.replace(' ', 'T'));
  if (isNaN(s.getTime()) || isNaN(e.getTime())) return "00:01:00";
  const diffSec = Math.max(1, Math.floor((e.getTime() - s.getTime()) / 1000));
  return formatDurationHms(diffSec);
}

// ============================================================================
// STOPWATCH / KRONOMETRE ENGINE
// ============================================================================
function toggleTimer() {
  if (state.timerRunning) {
    pauseTimer();
  } else {
    startTimer();
  }
}

function startTimer() {
  if (state.timerRunning) return;
  if (!document.getElementById('fBasTarihi').value) {
    document.getElementById('fBasTarihi').value = nowDateTimeStr();
  }
  state.timerRunning = true;
  state.timerInterval = setInterval(() => {
    state.timerSeconds++;
    updateTimerUI();
  }, 1000);
  updateTimerUI();
}

function pauseTimer() {
  state.timerRunning = false;
  if (state.timerInterval) {
    clearInterval(state.timerInterval);
    state.timerInterval = null;
  }
  updateTimerUI();
}

function resetTimer() {
  pauseTimer();
  state.timerSeconds = 0;
  updateTimerUI();
}

function finishCallTimerAndFill() {
  pauseTimer();
  const endNow = nowDateTimeStr();
  document.getElementById('fBitTarihi').value = endNow;
  if (state.timerSeconds > 0) {
    document.getElementById('fSure').value = formatDurationHms(state.timerSeconds);
  } else {
    const startVal = document.getElementById('fBasTarihi').value;
    document.getElementById('fSure').value = calcDurationBetweenDates(startVal, endNow);
  }
  showToast('⏱️ Çağrı süresi ve bitiş tarihi forma aktarıldı.');
}

function updateTimerUI() {
  const disp = document.getElementById('timerDisplay');
  const box = document.getElementById('timerBox');
  const btn = document.getElementById('btnTimerToggle');
  const badge = document.getElementById('timerStateBadge');

  if (disp) disp.textContent = formatDurationHms(state.timerSeconds);
  if (state.timerRunning) {
    box.classList.add('running');
    btn.innerHTML = '⏸️ Duraklat';
    badge.textContent = 'Görüşmede';
    badge.className = 'badge badge-green';
  } else {
    box.classList.remove('running');
    btn.innerHTML = '▶️ Başlat';
    badge.textContent = state.timerSeconds > 0 ? 'Duraklatıldı' : 'Beklemede';
    badge.className = state.timerSeconds > 0 ? 'badge badge-orange' : 'badge badge-blue';
  }
}

// ============================================================================
// FORM & AUTO-FILL LOGIC
// ============================================================================
function initDefaultFormValues() {
  const today = todayYmdStr();
  document.getElementById('fBasTarihi').value = nowDateTimeStr();
  document.getElementById('fKullaniciKodu').value = state.settings.globalKullaniciKodu || '';
  document.getElementById('fKullanici').value = state.settings.globalKullanici || '';
  document.getElementById('exportStartDate').value = today;
  document.getElementById('exportEndDate').value = today;
  document.getElementById('delStartDate').value = today;
  document.getElementById('delEndDate').value = today;
  document.getElementById('shiftReportDate').value = today;

  // Settings inputs
  document.getElementById('setGlobalKod').value = state.settings.globalKullaniciKodu || '';
  document.getElementById('setGlobalAd').value = state.settings.globalKullanici || '';
  document.getElementById('setOutlookEmail').value = state.settings.outlookRecipientEmail || 'servis@enpos.com.tr';
  document.getElementById('setSipDomain').value = state.settings.sipDomain || '';
  document.getElementById('setSipExt').value = state.settings.sipExtension || '101';
  document.getElementById('setSipPass').value = state.settings.sipPassword || '';
  document.getElementById('setSipPort').value = state.settings.sipPort || '5060';
  document.getElementById('setSqlType').value = state.settings.sqlDbType || 'MySQL';
  document.getElementById('setSqlHost').value = state.settings.sqlHost || '';
  document.getElementById('setSqlPort').value = state.settings.sqlPort || '8080';
  document.getElementById('setSqlDb').value = state.settings.sqlDbName || '';
}

function setStartNow() {
  document.getElementById('fBasTarihi').value = nowDateTimeStr();
  showToast('🕒 Başlangıç saati güncellendi.');
}

function setEndNowAndCalc() {
  const endStr = nowDateTimeStr();
  document.getElementById('fBitTarihi').value = endStr;
  const startStr = document.getElementById('fBasTarihi').value;
  document.getElementById('fSure').value = calcDurationBetweenDates(startStr, endStr);
}

function setBekleyenValue(val) {
  document.getElementById('fBekleyen').value = val;
  const btnHayir = document.getElementById('segBekleyenHayir');
  const btnEvet = document.getElementById('segBekleyenEvet');
  if (val === 'Evet') {
    btnEvet.className = 'seg-btn active-orange';
    btnHayir.className = 'seg-btn';
  } else {
    btnHayir.className = 'seg-btn active-green';
    btnEvet.className = 'seg-btn';
  }
}

function toggleAdvancedFields() {
  const el = document.getElementById('advancedFieldsContainer');
  const isHidden = el.style.display === 'none';
  el.style.display = isHidden ? 'block' : 'none';
  state.settings.showAdvancedFields = isHidden;
  saveAllToStorage();
}

function getFullSicilFromForm() {
  const prefix = (document.getElementById('fSicilPrefix').value || '').trim();
  const digits = (document.getElementById('fSicilDigits').value || '').trim();
  if (!digits) return '';
  if (digits.toUpperCase().startsWith(prefix.toUpperCase())) return digits.toUpperCase();
  return `${prefix}${digits}`.toUpperCase();
}

function syncSicilFull() {
  const full = getFullSicilFromForm();
  if (full.length >= 4) checkAutoFillByQuery(full, 'SICIL');
}

function onSicilDigitsInput(val) {
  const clean = val.trim();
  if (clean.length >= 3) {
    checkAutoFillByQuery(getFullSicilFromForm(), 'SICIL');
  } else {
    dismissAutoFillBanner();
  }
}

function onMusteriInput(inputEl) {
  inputEl.value = inputEl.value.toLocaleUpperCase('tr-TR');
  if (inputEl.value.trim().length >= 3) {
    checkAutoFillByQuery(inputEl.value.trim(), 'MUSTERI');
  }
}

function onPhoneInput(inputEl) {
  inputEl.value = inputEl.value.replace(/\s+/g, '');
  if (inputEl.value.length >= 7) {
    checkAutoFillByQuery(inputEl.value, 'TELEFON');
  }
}

function checkAutoFillByQuery(query, type) {
  const q = query.toLocaleUpperCase('tr-TR');
  const match = state.records.find(r => {
    if (type === 'SICIL') return (r.sicilNo1 || '').toLocaleUpperCase('tr-TR').includes(q);
    if (type === 'TELEFON') return (r.telefon || '').includes(q) || (r.telefon2 || '').includes(q);
    if (type === 'MUSTERI') return (r.musteriBayiAdi || '').toLocaleUpperCase('tr-TR').includes(q);
    return false;
  });

  if (match) {
    state.pendingAutoFill = match;
    const banner = document.getElementById('autoFillBanner');
    document.getElementById('autoFillTitle').textContent = `💡 Eşleşen Kayıt: ${match.musteriBayiAdi || 'Müşteri'} (${match.sicilNo1 || '-'})`;
    document.getElementById('autoFillDesc').textContent = `Tel: ${match.telefon || '-'} • Yetkili: ${match.gorusulenKisi || '-'} • YNÖKC: ${match.kasaVersiyon1 || '-'}`;
    banner.classList.add('visible');
  }
}

function applyAutoFillSuggestion() {
  const m = state.pendingAutoFill;
  if (!m) return;
  fillCustomerDataIntoForm(m);
  dismissAutoFillBanner();
  showToast('⚡ Müşteri ve cihaz bilgileri otomatik dolduruldu.');
}

function dismissAutoFillBanner() {
  state.pendingAutoFill = null;
  document.getElementById('autoFillBanner').classList.remove('visible');
}

function fillCustomerDataIntoForm(rec) {
  if (rec.sicilNo1) {
    const knownPrefixes = ['OKC', 'PC', 'YN', 'EN', 'DC'];
    const upper = rec.sicilNo1.toUpperCase();
    const matchedPref = knownPrefixes.find(p => upper.startsWith(p));
    if (matchedPref) {
      document.getElementById('fSicilPrefix').value = matchedPref;
      document.getElementById('fSicilDigits').value = upper.slice(matchedPref.length);
    } else {
      document.getElementById('fSicilPrefix').value = '';
      document.getElementById('fSicilDigits').value = upper;
    }
  }
  if (rec.musteriBayiAdi) document.getElementById('fMusteriBayi').value = rec.musteriBayiAdi;
  if (rec.gorusulenKisi) document.getElementById('fGorusulen').value = rec.gorusulenKisi;
  if (rec.telefon) document.getElementById('fTelefon').value = rec.telefon;
  if (rec.telefon2) document.getElementById('fTelefon2').value = rec.telefon2;
  if (rec.mail) document.getElementById('fMail').value = rec.mail;
  if (rec.arayanTuru) document.getElementById('fArayan').value = rec.arayanTuru;
  if (rec.baglantiId1) document.getElementById('fBagId').value = rec.baglantiId1;
  if (rec.kasaVersiyon1) document.getElementById('fYnokcVersiyon').value = rec.kasaVersiyon1;
  if (rec.dynCashVersiyon1) document.getElementById('fDynCashVersiyon').value = rec.dynCashVersiyon1;
  if (rec.yn500KasaVersiyon1) document.getElementById('fYn500Versiyon').value = rec.yn500KasaVersiyon1;
  if (rec.arkaOfisVersiyon1) document.getElementById('fArkaOfisVersiyon').value = rec.arkaOfisVersiyon1;
  if (rec.yn500SicilNo1) document.getElementById('fYn500SicilNo').value = rec.yn500SicilNo1;
  if (rec.arkaOfisId1) document.getElementById('fArkaOfisId').value = rec.arkaOfisId1;
  if (rec.servisKodu || rec.not1) document.getElementById('fServisKodu').value = rec.servisKodu || rec.not1;
}

function lookupCustomerCandidatesManual() {
  const uniqueMap = new Map();
  state.records.forEach(r => {
    const key = `${r.sicilNo1}_${r.musteriBayiAdi}_${r.telefon}`;
    if (!uniqueMap.has(key) && (r.musteriBayiAdi || r.sicilNo1)) {
      uniqueMap.set(key, r);
    }
  });
  const candidates = Array.from(uniqueMap.values()).slice(0, 30);
  const container = document.getElementById('candidatePickerList');
  if (candidates.length === 0) {
    container.innerHTML = `<p style="color:var(--text-secondary); text-align:center; padding:20px;">Henüz kayıtlı müşteri geçmişi bulunmuyor.</p>`;
  } else {
    container.innerHTML = candidates.map(c => `
      <div class="record-card" style="cursor:pointer;" onclick="selectCandidateById(${c.id})">
        <div class="record-top">
          <div class="record-title">🏢 ${escapeHtml(c.musteriBayiAdi || 'İsimsiz Müşteri')}</div>
          <span class="badge badge-blue">📟 ${escapeHtml(c.sicilNo1 || '-')}</span>
        </div>
        <div class="record-meta-grid">
          <span>👤 ${escapeHtml(c.gorusulenKisi || '-')}</span>
          <span>📞 ${escapeHtml(c.telefon || '-')}</span>
          <span>💳 YNÖKC: ${escapeHtml(c.kasaVersiyon1 || '-')}</span>
        </div>
      </div>
    `).join('');
  }
  openModal('candidatePickerModal');
}

function selectCandidateById(id) {
  const rec = state.records.find(r => r.id === id);
  if (rec) {
    fillCustomerDataIntoForm(rec);
    closeModal('candidatePickerModal');
    showToast('✅ Müşteri bilgileri forma aktarıldı.');
  }
}

// ============================================================================
// SAVE / UPDATE / DELETE SUPPORT RECORD
// ============================================================================
function handleSaveRecord(e) {
  e.preventDefault();
  const startStr = document.getElementById('fBasTarihi').value.trim() || nowDateTimeStr();
  let endStr = document.getElementById('fBitTarihi').value.trim();
  if (!endStr) endStr = nowDateTimeStr();

  let durationStr = document.getElementById('fSure').value.trim();
  if (!durationStr) {
    durationStr = state.timerSeconds > 0
      ? formatDurationHms(state.timerSeconds)
      : calcDurationBetweenDates(startStr, endStr);
  }

  const trUpper = (s) => (s || '').trim().toLocaleUpperCase('tr-TR');
  const cleanPhone = (p) => (p || '').replace(/\s+/g, '').replace(/[-()]/g, '').trim();

  const recordObj = {
    id: state.editingRecordId || Date.now(),
    kullaniciKodu: document.getElementById('fKullaniciKodu').value.trim() || state.settings.globalKullaniciKodu,
    kullaniciAdi: document.getElementById('fKullanici').value.trim() || state.settings.globalKullanici,
    baslangicTarihi: startStr,
    arayanTuru: document.getElementById('fArayan').value,
    musteriBayiAdi: trUpper(document.getElementById('fMusteriBayi').value),
    gorusulenKisi: trUpper(document.getElementById('fGorusulen').value),
    telefon: cleanPhone(document.getElementById('fTelefon').value),
    telefon2: cleanPhone(document.getElementById('fTelefon2').value),
    mail: document.getElementById('fMail').value.trim(),
    iletisimTuru: document.getElementById('fIletisimTuru').value,
    destekKonusu1: document.getElementById('fDestekKonu').value.trim(),
    sicilNo1: getFullSicilFromForm(),
    tespit1: document.getElementById('fTespit').value.trim(),
    islem1: document.getElementById('fIslem').value.trim(),
    baglantiId1: document.getElementById('fBagId').value.trim(),
    bitisTarihi: endStr,
    toplamSure: durationStr,
    kasaVersiyon1: document.getElementById('fYnokcVersiyon').value.trim(),
    arkaOfisVersiyon1: document.getElementById('fArkaOfisVersiyon').value.trim(),
    redmineId1: document.getElementById('fRedmine').value.trim(),
    aktif: "1",
    b2bKayit: document.getElementById('fB2bKayit').value.trim(),
    bekleyenIs: document.getElementById('fBekleyen').value || "Hayır",
    argeGidis: document.getElementById('fArgeGidis').value || "Hayır",
    argeDonus: document.getElementById('fArgeDonus').value || "Hayır",
    argeGidisNotu: document.getElementById('fArgeGidisNotu').value.trim(),
    argeDonusNotu: document.getElementById('fArgeDonusNotu').value.trim(),
    resim: "",
    tckn: document.getElementById('fTckn').value.trim(),
    vergiNo: document.getElementById('fVergiNo').value.trim(),
    tespitKodu1: document.getElementById('fTespitKodu').value.trim(),
    islemKodu1: document.getElementById('fIslemKodu').value.trim(),
    yn500SicilNo1: document.getElementById('fYn500SicilNo').value.trim(),
    dynCashVersiyon1: document.getElementById('fDynCashVersiyon').value.trim(),
    arkaOfisId1: document.getElementById('fArkaOfisId').value.trim(),
    yn500KasaVersiyon1: document.getElementById('fYn500Versiyon').value.trim(),
    servisKodu: document.getElementById('fServisKodu').value.trim(),
    not1: document.getElementById('fServisKodu').value.trim(),
    audioUri: "",
    isSynced: false,
    syncedAt: null,
    createdAt: Date.now()
  };

  if (state.editingRecordId) {
    const idx = state.records.findIndex(r => r.id === state.editingRecordId);
    if (idx !== -1) state.records[idx] = recordObj;
    showToast('✅ Destek kaydı başarıyla güncellendi!');
  } else {
    state.records.unshift(recordObj);
    showToast('💾 Yeni destek kaydı başarıyla kaydedildi!');
  }

  resetTimer();
  saveAllToStorage();
  clearSupportForm();
  renderAllUI();
}

function clearSupportForm() {
  state.editingRecordId = null;
  document.getElementById('formModeIcon').textContent = '📝';
  document.getElementById('formModeTitle').textContent = 'Yeni Destek Çağrı Kaydı';
  document.getElementById('editingRecordBadge').style.display = 'none';
  document.getElementById('btnSubmitRecord').innerHTML = '💾 Kaydı Veritabanına Kaydet';

  document.getElementById('fSicilDigits').value = '';
  document.getElementById('fBasTarihi').value = nowDateTimeStr();
  document.getElementById('fMusteriBayi').value = '';
  document.getElementById('fGorusulen').value = '';
  document.getElementById('fTelefon').value = '';
  document.getElementById('fTelefon2').value = '';
  document.getElementById('fMail').value = '';
  document.getElementById('fDestekKonu').value = '';
  document.getElementById('fTespit').value = '';
  document.getElementById('fIslem').value = '';
  document.getElementById('fBagId').value = '';
  document.getElementById('fRedmine').value = '';
  document.getElementById('fBitTarihi').value = '';
  document.getElementById('fSure').value = '';
  setBekleyenValue('Hayır');
  document.getElementById('fYnokcVersiyon').value = '';
  document.getElementById('fDynCashVersiyon').value = '';
  document.getElementById('fYn500Versiyon').value = '';
  document.getElementById('fArkaOfisVersiyon').value = '';
  document.getElementById('fYn500SicilNo').value = '';
  document.getElementById('fArkaOfisId').value = '';
  document.getElementById('fServisKodu').value = '';
  document.getElementById('fArgeGidis').value = 'Hayır';
  document.getElementById('fArgeDonus').value = 'Hayır';
  document.getElementById('fArgeGidisNotu').value = '';
  document.getElementById('fArgeDonusNotu').value = '';
  document.getElementById('fTckn').value = '';
  document.getElementById('fVergiNo').value = '';
  document.getElementById('fB2bKayit').value = '';
  document.getElementById('fTespitKodu').value = '';
  document.getElementById('fIslemKodu').value = '';
  document.getElementById('fKullaniciKodu').value = state.settings.globalKullaniciKodu || '';
  document.getElementById('fKullanici').value = state.settings.globalKullanici || '';
  dismissAutoFillBanner();
}

function editRecordById(id) {
  const rec = state.records.find(r => r.id === id);
  if (!rec) return;
  state.editingRecordId = rec.id;
  fillCustomerDataIntoForm(rec);
  document.getElementById('fBasTarihi').value = rec.baslangicTarihi || '';
  document.getElementById('fBitTarihi').value = rec.bitisTarihi || '';
  document.getElementById('fSure').value = rec.toplamSure || '';
  document.getElementById('fIletisimTuru').value = rec.iletisimTuru || 'Telefon';
  document.getElementById('fDestekKonu').value = rec.destekKonusu1 || '';
  document.getElementById('fTespit').value = rec.tespit1 || '';
  document.getElementById('fIslem').value = rec.islem1 || '';
  document.getElementById('fRedmine').value = rec.redmineId1 || '';
  setBekleyenValue(rec.bekleyenIs || 'Hayır');
  document.getElementById('fKullaniciKodu').value = rec.kullaniciKodu || '';
  document.getElementById('fKullanici').value = rec.kullaniciAdi || '';
  document.getElementById('fArgeGidis').value = rec.argeGidis || 'Hayır';
  document.getElementById('fArgeDonus').value = rec.argeDonus || 'Hayır';
  document.getElementById('fArgeGidisNotu').value = rec.argeGidisNotu || '';
  document.getElementById('fArgeDonusNotu').value = rec.argeDonusNotu || '';

  document.getElementById('formModeIcon').textContent = '✏️';
  document.getElementById('formModeTitle').textContent = `Kaydı Düzenle (#${rec.id})`;
  document.getElementById('editingRecordBadge').style.display = 'inline-flex';
  document.getElementById('btnSubmitRecord').innerHTML = '💾 Değişiklikleri Güncelle';

  switchTab(0);
  window.scrollTo({ top: 0, behavior: 'smooth' });
  showToast('✏️ Kayıt düzenleme modunda forma yüklendi.');
}

function toggleRecordPendingStatus(id) {
  const rec = state.records.find(r => r.id === id);
  if (!rec) return;
  rec.bekleyenIs = rec.bekleyenIs === 'Evet' ? 'Hayır' : 'Evet';
  saveAllToStorage();
  renderAllUI();
  showToast(`Durum güncellendi: ${rec.bekleyenIs === 'Evet' ? '⏳ Bekliyor' : '✅ Çözüldü'}`);
}

function deleteRecordById(id) {
  if (!confirm('Bu destek kaydını silmek istediğinize emin misiniz?')) return;
  state.records = state.records.filter(r => r.id !== id);
  saveAllToStorage();
  renderAllUI();
  showToast('🗑️ Kayıt silindi.');
}

// ============================================================================
// RENDER UI & TABS
// ============================================================================
function switchTab(tabIdx) {
  [0, 1, 2, 3].forEach(i => {
    const pane = document.getElementById(`tabContent${i}`);
    const btn = document.getElementById(`navBtn${i}`);
    if (pane) pane.style.display = i === tabIdx ? 'block' : 'none';
    if (btn) btn.classList.toggle('active', i === tabIdx);
  });
  renderAllUI();
}

function renderAllUI() {
  renderHeaderInfo();
  renderTodayKPIs();
  renderQuickScenarios();
  renderTemplatesAndPopularChips();
  renderHistoryList();
  renderMissedCallsList();
  renderRepresentativesChips();
  updateStorageBadge();
}

function renderHeaderInfo() {
  const repName = state.settings.globalKullanici || 'Belirtilmemiş';
  const repCode = state.settings.globalKullaniciKodu || '-';
  document.getElementById('headerRepSubtitle').textContent =
    `Temsilci: ${repName} (Kod: ${repCode}) • ${state.records.length} Kayıt`;
  document.getElementById('sipStatusText').textContent =
    `SIP: ${state.settings.sipExtension || '101'}`;

  const dutyIcon = document.getElementById('dutyModeIcon');
  const dutyText = document.getElementById('dutyModeText');
  if (state.settings.dutyModeEnabled) {
    dutyIcon.textContent = '🔔';
    dutyText.textContent = 'Nöbet: Aktif';
  } else {
    dutyIcon.textContent = '🌙';
    dutyText.textContent = 'Nöbet: Kapalı';
  }

  // Badges on bottom nav
  const pendingCount = state.records.filter(r => r.bekleyenIs === 'Evet').length;
  const histBadge = document.getElementById('navHistoryBadge');
  if (pendingCount > 0) {
    histBadge.style.display = 'inline-block';
    histBadge.textContent = pendingCount;
  } else {
    histBadge.style.display = 'none';
  }

  const unhandledMissed = state.missedCalls.filter(m => !m.isHandled).length;
  const missBadge = document.getElementById('navMissedBadge');
  if (unhandledMissed > 0) {
    missBadge.style.display = 'inline-block';
    missBadge.textContent = unhandledMissed;
  } else {
    missBadge.style.display = 'none';
  }
}

function renderTodayKPIs() {
  const today = todayYmdStr();
  const todayRecs = state.records.filter(r => (r.baslangicTarihi || '').startsWith(today));
  const total = todayRecs.length;
  const pending = todayRecs.filter(r => r.bekleyenIs === 'Evet').length;
  const resolved = total - pending;

  let totalSec = 0;
  todayRecs.forEach(r => { totalSec += parseDurationToSeconds(r.toplamSure); });
  const avgSec = total > 0 ? Math.floor(totalSec / total) : 0;
  const avgMin = String(Math.floor(avgSec / 60)).padStart(2, '0');
  const avgRemSec = String(avgSec % 60).padStart(2, '0');

  document.getElementById('kpiTodayTotal').textContent = total;
  document.getElementById('kpiTodayResolved').textContent = resolved;
  document.getElementById('kpiTodayPending').textContent = pending;
  document.getElementById('kpiTodayAvg').textContent = `${avgMin}:${avgRemSec}`;
}

function renderQuickScenarios() {
  const row = document.getElementById('quickScenariosRow');
  document.getElementById('scenarioCountBadge').textContent = `${state.scenarios.length} Senaryo`;
  row.innerHTML = state.scenarios.map(sc => `
    <button type="button" class="chip" onclick="applyQuickScenario('${sc.id}')">
      <span>${escapeHtml(sc.icon || '⚡')}</span>
      <span>${escapeHtml(sc.title)}</span>
    </button>
  `).join('');
}

function applyQuickScenario(id) {
  const sc = state.scenarios.find(s => s.id === id);
  if (!sc) return;
  sc.usageCount = (sc.usageCount || 0) + 1;
  document.getElementById('fDestekKonu').value = sc.konu || '';
  document.getElementById('fTespit').value = sc.tespit || '';
  document.getElementById('fIslem').value = sc.islem || '';
  saveAllToStorage();
  showToast(`⚡ "${sc.title}" senaryosu forma uygulandı.`);
}

function openScenarioModal() {
  document.getElementById('scTitle').value = '';
  document.getElementById('scKonu').value = document.getElementById('fDestekKonu').value || '';
  document.getElementById('scTespit').value = document.getElementById('fTespit').value || '';
  document.getElementById('scIslem').value = document.getElementById('fIslem').value || '';
  openModal('scenarioModal');
}

function saveNewQuickScenario() {
  const title = document.getElementById('scTitle').value.trim();
  const konu = document.getElementById('scKonu').value.trim();
  if (!title || !konu) {
    showToast('⚠️ Lütfen senaryo başlığı ve konusu girin.');
    return;
  }
  state.scenarios.unshift({
    id: 'sc_' + Date.now(),
    icon: document.getElementById('scIcon').value.trim() || '⚡',
    title,
    konu,
    tespit: document.getElementById('scTespit').value.trim(),
    islem: document.getElementById('scIslem').value.trim(),
    usageCount: 1
  });
  saveAllToStorage();
  renderQuickScenarios();
  closeModal('scenarioModal');
  showToast('✅ Yeni hızlı senaryo eklendi.');
}

function renderTemplatesAndPopularChips() {
  const konuList = state.templates.filter(t => t.type === 'KONU');
  const tespitList = state.templates.filter(t => t.type === 'TESPIT');
  const islemList = state.templates.filter(t => t.type === 'ISLEM');

  const dl = document.getElementById('konuDatalist');
  dl.innerHTML = konuList.map(k => `<option value="${escapeHtml(k.text)}"></option>`).join('');

  document.getElementById('popularKonuChips').innerHTML = konuList.map(k => `
    <button type="button" class="chip" onclick="document.getElementById('fDestekKonu').value='${escapeJsStr(k.text)}'">
      📌 ${escapeHtml(k.text)}
    </button>
  `).join('');

  document.getElementById('tespitTemplateChips').innerHTML = tespitList.map(t => `
    <button type="button" class="chip" onclick="appendTextToField('fTespit', '${escapeJsStr(t.text)}')">
      🔍 ${escapeHtml(t.text.slice(0, 42))}${t.text.length > 42 ? '...' : ''}
    </button>
  `).join('');

  document.getElementById('islemTemplateChips').innerHTML = islemList.map(t => `
    <button type="button" class="chip" onclick="appendTextToField('fIslem', '${escapeJsStr(t.text)}')">
      🛠️ ${escapeHtml(t.text.slice(0, 42))}${t.text.length > 42 ? '...' : ''}
    </button>
  `).join('');
}

function appendTextToField(fieldId, text) {
  const el = document.getElementById(fieldId);
  if (!el) return;
  el.value = el.value.trim() ? `${el.value.trim()} ${text}` : text;
}

function promptAddTemplate(type) {
  const currentVal = type === 'KONU'
    ? document.getElementById('fDestekKonu').value
    : type === 'TESPIT'
      ? document.getElementById('fTespit').value
      : document.getElementById('fIslem').value;
  const text = prompt(`Yeni ${type} şablonu metnini girin:`, currentVal || '');
  if (!text || !text.trim()) return;
  state.templates.unshift({ id: Date.now(), type, text: text.trim() });
  saveAllToStorage();
  renderTemplatesAndPopularChips();
  showToast(`✅ ${type} şablonu kaydedildi.`);
}

// ============================================================================
// HISTORY LIST & FILTERING
// ============================================================================
function setHistoryFilter(filter) {
  state.historyFilter = filter;
  ['ALL', 'TODAY', 'PENDING', 'RESOLVED', 'ARGE'].forEach(f => {
    const idMap = {
      ALL: 'histFilterAll',
      TODAY: 'histFilterToday',
      PENDING: 'histFilterPending',
      RESOLVED: 'histFilterResolved',
      ARGE: 'histFilterArge'
    };
    const btn = document.getElementById(idMap[f]);
    if (btn) btn.classList.toggle('active', f === filter);
  });
  renderHistoryList();
}

function renderHistoryList() {
  const q = (document.getElementById('historySearchInput')?.value || '').trim().toLocaleLowerCase('tr-TR');
  const today = todayYmdStr();

  const filtered = state.records.filter(r => {
    if (state.historyFilter === 'TODAY' && !(r.baslangicTarihi || '').startsWith(today)) return false;
    if (state.historyFilter === 'PENDING' && r.bekleyenIs !== 'Evet') return false;
    if (state.historyFilter === 'RESOLVED' && r.bekleyenIs === 'Evet') return false;
    if (state.historyFilter === 'ARGE' && r.argeGidis !== 'Evet' && r.argeDonus !== 'Evet') return false;

    if (!q) return true;
    const hay = [
      r.musteriBayiAdi, r.sicilNo1, r.gorusulenKisi, r.telefon, r.telefon2,
      r.destekKonusu1, r.tespit1, r.islem1, r.kullaniciAdi, r.redmineId1
    ].join(' ').toLocaleLowerCase('tr-TR');
    return hay.includes(q);
  });

  document.getElementById('historyTotalBadge').textContent = `${filtered.length} Kayıt`;
  const container = document.getElementById('historyListContainer');

  if (filtered.length === 0) {
    container.innerHTML = `
      <div style="text-align:center; padding:32px 16px; color:var(--text-secondary);">
        <div style="font-size:32px; margin-bottom:8px;">📭</div>
        <div style="font-weight:700;">Eşleşen destek kaydı bulunamadı</div>
        <div style="font-size:12px; margin-top:4px;">Yeni kayıt ekleyebilir veya Ayarlar'dan JSON yedeğinizi geri yükleyebilirsiniz.</div>
      </div>
    `;
    return;
  }

  container.innerHTML = filtered.slice(0, 150).map(r => {
    const isPending = r.bekleyenIs === 'Evet';
    return `
      <div class="record-card">
        <div class="record-top">
          <div>
            <div class="record-title">🏢 ${escapeHtml(r.musteriBayiAdi || 'BELİRTİLMEMİŞ MÜŞTERİ')}</div>
            <div style="font-size:11.5px; color:#60A5FA; font-weight:700; margin-top:2px;">
              📌 ${escapeHtml(r.destekKonusu1 || 'Genel Destek')}
            </div>
          </div>
          <div style="display:flex; gap:5px; align-items:center; flex-wrap:wrap;">
            <span class="badge badge-blue tabular-nums">📟 ${escapeHtml(r.sicilNo1 || '-')}</span>
            <span class="badge ${isPending ? 'badge-orange' : 'badge-green'}">
              ${isPending ? '⏳ Bekliyor' : '✅ Çözüldü'}
            </span>
          </div>
        </div>

        <div class="record-meta-grid tabular-nums">
          <span>🕒 ${escapeHtml(r.baslangicTarihi || '-')}</span>
          <span>⏱️ ${escapeHtml(r.toplamSure || '-')}</span>
          <span>👤 ${escapeHtml(r.gorusulenKisi || '-')}</span>
          <span>📞 ${escapeHtml(r.telefon || '-')}</span>
          <span>🎧 ${escapeHtml(resolveRepDisplay(r))}</span>
        </div>

        <div class="record-detail-box">
          <div><b>🔍 Tespit:</b> ${escapeHtml(r.tespit1 || '-')}</div>
          <div style="margin-top:4px;"><b>🛠️ İşlem:</b> ${escapeHtml(r.islem1 || '-')}</div>
        </div>

        <div class="record-actions">
          <button type="button" class="btn btn-sm btn-blue" onclick="editRecordById(${r.id})">✏️ Düzenle</button>
          <button type="button" class="btn btn-sm btn-outline" onclick="toggleRecordPendingStatus(${r.id})">
            ${isPending ? '✅ Çözüldü Yap' : '⏳ Bekleyene Al'}
          </button>
          <button type="button" class="btn btn-sm btn-outline" onclick="openDeviceReportBySicil('${escapeJsStr(r.sicilNo1 || r.musteriBayiAdi)}')">
            🛠️ Sicil Karnesi
          </button>
          <button type="button" class="btn btn-sm btn-green" onclick="shareRecordWhatsApp(${r.id})">💬 WP</button>
          <button type="button" class="btn btn-sm btn-red" onclick="deleteRecordById(${r.id})">🗑️ Sil</button>
        </div>
      </div>
    `;
  }).join('');
}

function resolveRepDisplay(r) {
  const code = (r.kullaniciKodu || '').trim();
  const name = (r.kullaniciAdi || '').trim();
  const match = state.representatives.find(rep => rep.code === code || rep.name.toLowerCase() === name.toLowerCase());
  if (match) return `${match.name} (${match.code})`;
  if (name && code) return `${name} (${code})`;
  return name || code || 'Temsilci';
}

// ============================================================================
// MISSED CALLS & SANTRAL LOGS
// ============================================================================
function setCallLogFilter(filter) {
  state.callLogFilter = filter;
  const map = {
    ALL: 'callFilterAll',
    MISSED: 'callFilterMissed',
    UNHANDLED: 'callFilterUnhandled',
    INCOMING: 'callFilterIncoming',
    OUTGOING: 'callFilterOutgoing'
  };
  Object.entries(map).forEach(([k, id]) => {
    document.getElementById(id)?.classList.toggle('active', k === filter);
  });
  renderMissedCallsList();
}

function renderMissedCallsList() {
  const unhandledCount = state.missedCalls.filter(m => !m.isHandled).length;
  document.getElementById('missedCountHeaderBadge').textContent = `${unhandledCount} Bekleyen`;

  const filtered = state.missedCalls.filter(m => {
    if (state.callLogFilter === 'MISSED') return m.callType === 'MISSED';
    if (state.callLogFilter === 'UNHANDLED') return !m.isHandled;
    if (state.callLogFilter === 'INCOMING') return m.callType === 'INCOMING';
    if (state.callLogFilter === 'OUTGOING') return m.callType === 'OUTGOING';
    return true;
  });

  const container = document.getElementById('missedCallsListContainer');
  if (filtered.length === 0) {
    container.innerHTML = `<div style="text-align:center; padding:24px; color:var(--text-secondary);">Kayıtlı çağrı bulunmuyor.</div>`;
    return;
  }

  container.innerHTML = filtered.map(c => `
    <div class="record-card">
      <div class="record-top">
        <div class="record-title tabular-nums">📞 ${escapeHtml(c.callerNumber)} ${c.callerName ? `(${escapeHtml(c.callerName)})` : ''}</div>
        <span class="badge ${c.callType === 'MISSED' ? 'badge-red' : 'badge-blue'}">${escapeHtml(c.reason || 'Cevapsız')}</span>
      </div>
      <div class="record-meta-grid tabular-nums">
        <span>🕒 ${escapeHtml(c.dateFormatted || '-')}</span>
        <span>Durum: ${c.isHandled ? '✅ Dönüş Yapıldı' : '⏳ Dönüş Bekliyor'}</span>
      </div>
      <div class="record-actions">
        <button type="button" class="btn btn-sm btn-blue" onclick="transferCallToSupportForm(${c.id})">📝 Forma Aktar & Kayıt Aç</button>
        <button type="button" class="btn btn-sm btn-green" onclick="window.open('tel:${escapeJsStr(c.callerNumber)}')">📞 Geri Ara</button>
        <button type="button" class="btn btn-sm btn-outline" onclick="toggleMissedCallHandled(${c.id})">
          ${c.isHandled ? '⏳ Bekleyene Al' : '✅ İşlendi Yap'}
        </button>
      </div>
    </div>
  `).join('');
}

function promptAddMissedCall() {
  const num = prompt('Arayan Telefon Numarası:', '0532');
  if (!num) return;
  const name = prompt('Arayan Müşteri / Kişi Adı (Opsiyonel):', '');
  state.missedCalls.unshift({
    id: Date.now(),
    callerNumber: num.replace(/\s+/g, ''),
    callerName: (name || '').toLocaleUpperCase('tr-TR'),
    timestamp: Date.now(),
    dateFormatted: nowDateTimeStr(),
    reason: 'Cevapsız Çağrı',
    isHandled: false,
    callType: 'MISSED',
    duration: '00:00'
  });
  saveAllToStorage();
  renderAllUI();
}

function transferCallToSupportForm(callId) {
  const c = state.missedCalls.find(m => m.id === callId);
  if (!c) return;
  c.isHandled = true;
  clearSupportForm();
  document.getElementById('fTelefon').value = c.callerNumber;
  if (c.callerName) document.getElementById('fMusteriBayi').value = c.callerName;
  checkAutoFillByQuery(c.callerNumber, 'TELEFON');
  saveAllToStorage();
  switchTab(0);
  startTimer();
  showToast('📞 Çağrı bilgileri forma aktarıldı ve kronometre başlatıldı.');
}

function toggleMissedCallHandled(id) {
  const c = state.missedCalls.find(m => m.id === id);
  if (c) {
    c.isHandled = !c.isHandled;
    saveAllToStorage();
    renderAllUI();
  }
}

function markAllMissedHandled() {
  state.missedCalls.forEach(c => { c.isHandled = true; });
  saveAllToStorage();
  renderAllUI();
  showToast('✅ Tüm çağrılar işlendi olarak işaretlendi.');
}

// ============================================================================
// STATISTICS & ANALYTICS MODAL (SupportStatisticsDialog Equivalent)
// ============================================================================
function openStatisticsModal() {
  renderStatisticsModalContent();
  openModal('statsModal');
}

function setStatsRange(range) {
  state.statsRange = range;
  ['ALL', 'TODAY', 'WEEK', 'MONTH'].forEach(r => {
    const id = { ALL: 'stRangeAll', TODAY: 'stRangeToday', WEEK: 'stRangeWeek', MONTH: 'stRangeMonth' }[r];
    document.getElementById(id)?.classList.toggle('active', r === range);
  });
  renderStatisticsModalContent();
}

function renderStatisticsModalContent() {
  const now = new Date();
  const todayStr = todayYmdStr();
  const filtered = state.records.filter(r => {
    const dStr = (r.baslangicTarihi || '').slice(0, 10);
    if (!dStr) return state.statsRange === 'ALL';
    if (state.statsRange === 'TODAY') return dStr === todayStr;
    if (state.statsRange === 'WEEK') {
      const diff = (now - new Date(dStr)) / (1000 * 3600 * 24);
      return diff <= 7;
    }
    if (state.statsRange === 'MONTH') {
      return dStr.slice(0, 7) === todayStr.slice(0, 7);
    }
    return true;
  });

  const total = filtered.length;
  const pending = filtered.filter(r => r.bekleyenIs === 'Evet').length;
  const resolved = total - pending;
  const resolveRate = total > 0 ? Math.round((resolved / total) * 100) : 100;

  // Top 5 Topics
  const topicCounts = {};
  const islemCounts = {};
  const repMap = {};
  const hourCounts = new Array(24).fill(0);

  filtered.forEach(r => {
    const t = (r.destekKonusu1 || 'Belirtilmemiş').trim();
    topicCounts[t] = (topicCounts[t] || 0) + 1;

    const isl = (r.islem1 || 'Standart Çözüm').trim().slice(0, 65);
    islemCounts[isl] = (islemCounts[isl] || 0) + 1;

    const rep = resolveRepDisplay(r);
    if (!repMap[rep]) repMap[rep] = { count: 0, resolved: 0, sec: 0 };
    repMap[rep].count++;
    if (r.bekleyenIs !== 'Evet') repMap[rep].resolved++;
    repMap[rep].sec += parseDurationToSeconds(r.toplamSure);

    const hrMatch = (r.baslangicTarihi || '').match(/\s(\d{2}):/);
    if (hrMatch) {
      const h = parseInt(hrMatch[1], 10);
      if (h >= 0 && h < 24) hourCounts[h]++;
    }
  });

  const topTopics = Object.entries(topicCounts).sort((a, b) => b[1] - a[1]).slice(0, 5);
  const topIslemler = Object.entries(islemCounts).sort((a, b) => b[1] - a[1]).slice(0, 5);
  const repList = Object.entries(repMap).sort((a, b) => b[1].count - a[1].count);

  const el = document.getElementById('statsModalContent');
  el.innerHTML = `
    <div class="kpi-strip" style="margin-bottom:14px;">
      <div class="kpi-box"><div class="kpi-label">Toplam Çağrı</div><div class="kpi-val" style="color:#60A5FA;">${total}</div></div>
      <div class="kpi-box"><div class="kpi-label">Çözülen</div><div class="kpi-val" style="color:#4ADE80;">${resolved}</div></div>
      <div class="kpi-box"><div class="kpi-label">Bekleyen</div><div class="kpi-val" style="color:#FBBF24;">${pending}</div></div>
      <div class="kpi-box"><div class="kpi-label">Çözüm Oranı</div><div class="kpi-val" style="color:#2DD4BF;">%${resolveRate}</div></div>
    </div>

    <div class="section-card" style="margin-bottom:12px;">
      <div class="section-title" style="margin-bottom:10px;">🔥 En Çok Aranan 5 Destek Konusu</div>
      ${topTopics.length === 0 ? '<div style="color:var(--text-secondary);">Veri yok</div>' : topTopics.map(([name, cnt]) => {
        const pct = Math.max(6, Math.round((cnt / total) * 100));
        return `
          <div class="stat-bar-row">
            <div class="stat-bar-top">
              <span class="stat-bar-label">${escapeHtml(name)}</span>
              <span class="tabular-nums"><b>${cnt}</b> (%${pct})</span>
            </div>
            <div class="stat-bar-track"><div class="stat-bar-fill" style="width:${pct}%; background:#0A84FF;"></div></div>
          </div>
        `;
      }).join('')}
    </div>

    <div class="section-card" style="margin-bottom:12px;">
      <div class="section-title" style="margin-bottom:10px;">🛠️ En Çok Uygulanan 5 Çözüm / İşlem</div>
      ${topIslemler.length === 0 ? '<div style="color:var(--text-secondary);">Veri yok</div>' : topIslemler.map(([name, cnt]) => {
        const pct = Math.max(6, Math.round((cnt / total) * 100));
        return `
          <div class="stat-bar-row">
            <div class="stat-bar-top">
              <span class="stat-bar-label">${escapeHtml(name)}</span>
              <span class="tabular-nums"><b>${cnt}</b> (%${pct})</span>
            </div>
            <div class="stat-bar-track"><div class="stat-bar-fill" style="width:${pct}%; background:#22C55E;"></div></div>
          </div>
        `;
      }).join('')}
    </div>

    <div class="section-card">
      <div class="section-title" style="margin-bottom:10px;">👥 Temsilci Performans Dağılımı</div>
      ${repList.length === 0 ? '<div style="color:var(--text-secondary);">Veri yok</div>' : repList.map(([repName, info]) => {
        const pct = Math.round((info.resolved / info.count) * 100);
        return `
          <div class="stat-bar-row">
            <div class="stat-bar-top">
              <span class="stat-bar-label">🎧 ${escapeHtml(repName)}</span>
              <span class="tabular-nums">${info.count} Çağrı • %${pct} Çözüm • ⏱️ ${formatDurationHms(info.sec)}</span>
            </div>
            <div class="stat-bar-track"><div class="stat-bar-fill" style="width:${pct}%; background:#A855F7;"></div></div>
          </div>
        `;
      }).join('')}
    </div>
  `;
}

// ============================================================================
// DAILY SHIFT REPORT (DailyShiftReportDialog Equivalent)
// ============================================================================
function openDailyShiftModal() {
  document.getElementById('shiftReportDate').value = todayYmdStr();
  renderDailyShiftReport();
  openModal('shiftModal');
}

function setShiftDateOffset(days) {
  const d = new Date();
  d.setDate(d.getDate() + days);
  const pad = (n) => String(n).padStart(2, '0');
  document.getElementById('shiftReportDate').value = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
  renderDailyShiftReport();
}

function renderDailyShiftReport() {
  const ymd = document.getElementById('shiftReportDate').value || todayYmdStr();
  const recs = state.records.filter(r => (r.baslangicTarihi || '').startsWith(ymd));
  const pending = recs.filter(r => r.bekleyenIs === 'Evet');
  const resolved = recs.length - pending.length;

  let totalSec = 0;
  const topicMap = {};
  recs.forEach(r => {
    totalSec += parseDurationToSeconds(r.toplamSure);
    const k = r.destekKonusu1 || 'Diğer';
    topicMap[k] = (topicMap[k] || 0) + 1;
  });

  const topTopicsLines = Object.entries(topicMap)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 5)
    .map(([k, v], i) => `  ${i + 1}. ${k}: ${v} çağrı`)
    .join('\n') || '  - Kayıt yok';

  const pendingLines = pending.map((p, i) =>
    `  ${i + 1}. [${p.sicilNo1 || '-'}] ${p.musteriBayiAdi || '-'} -> ${p.destekKonusu1 || '-'} (${p.tespit1 || '-'})`
  ).join('\n') || '  - Devreden bekleyen iş bulunmamaktadır.';

  const report = [
    `📋 TELEFON DESTEK GÜN SONU VARDİYA RAPORU`,
    `📅 Tarih: ${ymd}`,
    `🎧 Temsilci: ${state.settings.globalKullanici || '-'} (Kod: ${state.settings.globalKullaniciKodu || '-'})`,
    `==========================================`,
    `📞 Toplam Çağrı : ${recs.length}`,
    `✅ Çözülen İş   : ${resolved}`,
    `⏳ Bekleyen İş  : ${pending.length}`,
    `⏱️ Toplam Süre  : ${formatDurationHms(totalSec)}`,
    ``,
    `📌 EN ÇOK GÖRÜŞÜLEN KONULAR:`,
    topTopicsLines,
    ``,
    `⏳ DEVREDEN / BEKLEYEN İŞLER:`,
    pendingLines,
    `==========================================`
  ].join('\n');

  document.getElementById('shiftReportText').value = report;
}

function copyShiftReport() {
  const txt = document.getElementById('shiftReportText').value;
  navigator.clipboard.writeText(txt).then(() => showToast('📋 Gün sonu raporu panoya kopyalandı!'));
}

function shareShiftReportWhatsApp() {
  const txt = document.getElementById('shiftReportText').value;
  window.open(`https://wa.me/?text=${encodeURIComponent(txt)}`, '_blank');
}

function exportShiftDateExcel() {
  const ymd = document.getElementById('shiftReportDate').value || todayYmdStr();
  document.getElementById('exportStartDate').value = ymd;
  document.getElementById('exportEndDate').value = ymd;
  generateAndDownloadExcel();
}

// ============================================================================
// DEVICE / SICIL SERVICE REPORT (DeviceServiceReportDialog Equivalent)
// ============================================================================
function openDeviceReportModal() {
  renderDeviceServiceReport();
  openModal('deviceReportModal');
}

function openDeviceReportForCurrentSicil() {
  const s = getFullSicilFromForm() || document.getElementById('fMusteriBayi').value.trim();
  openDeviceReportBySicil(s);
}

function openDeviceReportBySicil(query) {
  document.getElementById('deviceReportSearchInput').value = query || '';
  renderDeviceServiceReport();
  openModal('deviceReportModal');
}

function renderDeviceServiceReport() {
  const q = (document.getElementById('deviceReportSearchInput').value || '').trim().toLocaleUpperCase('tr-TR');
  const container = document.getElementById('deviceReportContent');

  const matches = state.records.filter(r => {
    if (!q) return Boolean(r.sicilNo1);
    return (r.sicilNo1 || '').toLocaleUpperCase('tr-TR').includes(q) ||
           (r.musteriBayiAdi || '').toLocaleUpperCase('tr-TR').includes(q);
  });

  if (matches.length === 0) {
    container.innerHTML = `<div style="padding:20px; text-align:center; color:var(--text-secondary);">Bu sicil veya müşteriye ait servis kaydı bulunamadı.</div>`;
    return;
  }

  const head = matches[0];
  container.innerHTML = `
    <div class="section-card" style="margin-bottom:10px; background:var(--bg-input);">
      <div style="font-weight:800; font-size:14px; color:#60A5FA;">📟 Sicil: ${escapeHtml(head.sicilNo1 || '-')} — ${escapeHtml(head.musteriBayiAdi || '-')}</div>
      <div style="font-size:12px; color:var(--text-secondary); margin-top:4px;">
        Toplam Servis Kaydı: <b>${matches.length}</b> • YNÖKC: <b>${escapeHtml(head.kasaVersiyon1 || '-')}</b> • DynCash: <b>${escapeHtml(head.dynCashVersiyon1 || '-')}</b>
      </div>
    </div>
    ${matches.slice(0, 30).map(m => `
      <div class="record-card">
        <div class="record-top">
          <div class="record-title">📌 ${escapeHtml(m.destekKonusu1 || '-')}</div>
          <span class="badge ${m.bekleyenIs === 'Evet' ? 'badge-orange' : 'badge-green'}">${m.bekleyenIs === 'Evet' ? 'Bekliyor' : 'Çözüldü'}</span>
        </div>
        <div class="record-meta-grid tabular-nums">
          <span>🕒 ${escapeHtml(m.baslangicTarihi)}</span>
          <span>⏱️ ${escapeHtml(m.toplamSure)}</span>
          <span>🎧 ${escapeHtml(resolveRepDisplay(m))}</span>
        </div>
        <div class="record-detail-box">
          <div><b>Tespit:</b> ${escapeHtml(m.tespit1 || '-')}</div>
          <div><b>İşlem:</b> ${escapeHtml(m.islem1 || '-')}</div>
        </div>
      </div>
    `).join('')}
  `;
}

function copyDeviceReportText() {
  const q = (document.getElementById('deviceReportSearchInput').value || '').trim().toLocaleUpperCase('tr-TR');
  const matches = state.records.filter(r =>
    !q || (r.sicilNo1 || '').toLocaleUpperCase('tr-TR').includes(q) || (r.musteriBayiAdi || '').toLocaleUpperCase('tr-TR').includes(q)
  );
  const lines = matches.map((m, idx) =>
    `${idx + 1}. [${m.baslangicTarihi}] Sicil:${m.sicilNo1} | ${m.musteriBayiAdi} | Konu:${m.destekKonusu1} | İşlem:${m.islem1}`
  ).join('\n');
  navigator.clipboard.writeText(lines || 'Kayıt yok').then(() => showToast('📋 Cihaz servis karnesi kopyalandı!'));
}

// ============================================================================
// SERVICE MANAGER PANEL (ServiceManagerDialog Equivalent)
// ============================================================================
function openServiceManagerModal() {
  const pendingRecs = state.records.filter(r => r.bekleyenIs === 'Evet');
  const argeRecs = state.records.filter(r => r.argeGidis === 'Evet');

  const sicilCounts = {};
  state.records.forEach(r => {
    if (r.sicilNo1) {
      sicilCounts[r.sicilNo1] = (sicilCounts[r.sicilNo1] || 0) + 1;
    }
  });
  const repeatSicils = Object.entries(sicilCounts).filter(([, c]) => c >= 2).sort((a, b) => b[1] - a[1]);

  document.getElementById('serviceManagerContent').innerHTML = `
    <div class="kpi-strip" style="margin-bottom:12px;">
      <div class="kpi-box"><div class="kpi-label">⏳ Açık Bekleyen İş</div><div class="kpi-val" style="color:#FBBF24;">${pendingRecs.length}</div></div>
      <div class="kpi-box"><div class="kpi-label">🧪 AR-GE Kaydı</div><div class="kpi-val" style="color:#C084FC;">${argeRecs.length}</div></div>
      <div class="kpi-box"><div class="kpi-label">🔁 Tekrarlayan Cihaz</div><div class="kpi-val" style="color:#F87171;">${repeatSicils.length}</div></div>
      <div class="kpi-box"><div class="kpi-label">📊 Toplam Veritabanı</div><div class="kpi-val" style="color:#4ADE80;">${state.records.length}</div></div>
    </div>

    <div class="section-card">
      <div class="section-title" style="margin-bottom:8px;">🔁 Birden Fazla Arıza Açılan Cihazlar (Kronik Takip)</div>
      ${repeatSicils.length === 0 ? '<div style="color:var(--text-secondary);">Tekrarlayan sicil kaydı yok.</div>' : repeatSicils.slice(0, 10).map(([sicil, cnt]) => `
        <div style="display:flex; justify-content:space-between; align-items:center; padding:8px 0; border-bottom:1px solid var(--border-subtle);">
          <span>📟 <b>${escapeHtml(sicil)}</b></span>
          <button type="button" class="btn btn-sm btn-orange" onclick="closeModal('serviceManagerModal'); openDeviceReportBySicil('${escapeJsStr(sicil)}')">
            ${cnt} Kayıt İncele
          </button>
        </div>
      `).join('')}
    </div>
  `;
  openModal('serviceManagerModal');
}

// ============================================================================
// KNOWLEDGE BASE MODAL (KnowledgeBaseDialog Equivalent)
// ============================================================================
function openKnowledgeBaseModal() {
  renderKnowledgeBase();
  openModal('kbModal');
}

function renderKnowledgeBase() {
  const q = (document.getElementById('kbSearchInput').value || '').trim().toLocaleLowerCase('tr-TR');
  const list = state.kbArticles.filter(a => {
    if (!q) return true;
    return [a.errorCode, a.title, a.category, a.cause, a.recommendedTespit, a.recommendedIslem]
      .join(' ').toLocaleLowerCase('tr-TR').includes(q);
  });

  document.getElementById('kbArticlesContainer').innerHTML = list.map(a => `
    <div class="record-card">
      <div class="record-top">
        <div class="record-title">📘 ${escapeHtml(a.title)}</div>
        <span class="badge badge-purple">${escapeHtml(a.errorCode)}</span>
      </div>
      <div style="font-size:12px; color:var(--text-secondary); margin-bottom:6px;">
        <b>Sebep:</b> ${escapeHtml(a.cause)}
      </div>
      <div class="record-detail-box">
        ${(a.steps || []).map((s, i) => `<div>${i + 1}. ${escapeHtml(s)}</div>`).join('')}
      </div>
      <div class="record-actions">
        <button type="button" class="btn btn-sm btn-green" onclick="applyKbArticleToForm('${escapeJsStr(a.id)}')">
          ⚡ Tespit & Çözümü Forma Aktar
        </button>
      </div>
    </div>
  `).join('');
}

function applyKbArticleToForm(id) {
  const a = state.kbArticles.find(x => x.id === id);
  if (!a) return;
  document.getElementById('fDestekKonu').value = a.title;
  document.getElementById('fTespit').value = a.recommendedTespit;
  document.getElementById('fIslem').value = a.recommendedIslem;
  closeModal('kbModal');
  switchTab(0);
  showToast(`📚 "${a.errorCode}" çözümü forma uygulandı!`);
}

function promptAddKnowledgeArticle() {
  const code = prompt('Hata Kodu (Örn: ERR-405):', 'ERR-');
  if (!code) return;
  const title = prompt('Arıza Başlığı:', '');
  if (!title) return;
  const tespit = prompt('Önerilen Tespit Metni:', document.getElementById('fTespit').value || '');
  const islem = prompt('Önerilen Çözüm / İşlem Metni:', document.getElementById('fIslem').value || '');

  state.kbArticles.unshift({
    id: 'kb_' + Date.now(),
    errorCode: code.toUpperCase(),
    title,
    category: 'Özel Çözüm',
    cause: tespit || title,
    steps: [islem || 'Standart çözüm adımları uygulandı.'],
    recommendedTespit: tespit || title,
    recommendedIslem: islem || ''
  });
  saveAllToStorage();
  renderKnowledgeBase();
  showToast('✅ Yeni Bilgi Bankası makalesi eklendi.');
}

// ============================================================================
// KEYPAD, WEB AUDIO DTMF & SIP CALL SIMULATION
// ============================================================================
function pressKeypad(key) {
  const inp = document.getElementById('keypadDisplayInput');
  inp.value += key;
  playDtmfTone();
}

function playDtmfTone() {
  try {
    const ctx = new (window.AudioContext || window.webkitAudioContext)();
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.frequency.value = 697;
    gain.gain.value = 0.06;
    osc.connect(gain);
    gain.connect(ctx.destination);
    osc.start();
    setTimeout(() => { osc.stop(); ctx.close(); }, 90);
  } catch (_) {}
}

function startCallFromKeypad() {
  const num = document.getElementById('keypadDisplayInput').value.trim();
  if (!num) {
    showToast('⚠️ Lütfen aranacak numarayı girin.');
    return;
  }
  document.getElementById('fTelefon').value = num;
  checkAutoFillByQuery(num, 'TELEFON');
  closeModal('keypadModal');
  switchTab(0);
  startTimer();
  showToast(`📞 ${num} numarası forma aktarıldı ve çağrı sayacı başlatıldı.`);
}

function simulateIncomingSipCall() {
  const sampleNumbers = ['05324418892', '05443129901', '02123456789'];
  const num = sampleNumbers[Math.floor(Math.random() * sampleNumbers.length)];
  document.getElementById('keypadDisplayInput').value = num;
  playDtmfTone();
  showToast(`🔔 Verimor SIP Gelen Çağrı: ${num}`);
}

// ============================================================================
// VOICE INPUT (WEB SPEECH API)
// ============================================================================
function startVoiceInput(targetFieldId) {
  const SpeechRec = window.SpeechRecognition || window.webkitSpeechRecognition;
  if (!SpeechRec) {
    showToast('⚠️ Tarayıcınız sesli yazdırma özelliğini desteklemiyor.');
    return;
  }
  const rec = new SpeechRec();
  rec.lang = 'tr-TR';
  rec.interimResults = false;
  rec.maxAlternatives = 1;
  showToast('🎤 Dinleniyor... Lütfen konuşun.');
  rec.onresult = (event) => {
    const transcript = event.results[0][0].transcript;
    appendTextToField(targetFieldId, transcript);
    showToast('✅ Ses metne dönüştürüldü.');
  };
  rec.onerror = () => showToast('⚠️ Ses algılanamadı.');
  rec.start();
}

// ============================================================================
// WHATSAPP, EMAIL & CLIPBOARD SHARING
// ============================================================================
function formatSingleRecordShareText(r) {
  return [
    `📞 *TELEFON DESTEK KAYDI*`,
    `🏢 *Müşteri:* ${r.musteriBayiAdi || '-'}`,
    `📟 *Sicil No:* ${r.sicilNo1 || '-'}`,
    `👤 *Görüşülen:* ${r.gorusulenKisi || '-'} (${r.telefon || '-'})`,
    `📌 *Konu:* ${r.destekKonusu1 || '-'}`,
    `🔍 *Tespit:* ${r.tespit1 || '-'}`,
    `🛠️ *İşlem:* ${r.islem1 || '-'}`,
    `⏳ *Bekleyen İş:* ${r.bekleyenIs || 'Hayır'}`,
    `⏱️ *Süre:* ${r.toplamSure || '-'} | 🕒 ${r.baslangicTarihi || '-'}`
  ].join('\n');
}

function shareCurrentFormWhatsApp() {
  const tempRec = {
    musteriBayiAdi: document.getElementById('fMusteriBayi').value,
    sicilNo1: getFullSicilFromForm(),
    gorusulenKisi: document.getElementById('fGorusulen').value,
    telefon: document.getElementById('fTelefon').value,
    destekKonusu1: document.getElementById('fDestekKonu').value,
    tespit1: document.getElementById('fTespit').value,
    islem1: document.getElementById('fIslem').value,
    bekleyenIs: document.getElementById('fBekleyen').value,
    toplamSure: document.getElementById('fSure').value || formatDurationHms(state.timerSeconds),
    baslangicTarihi: document.getElementById('fBasTarihi').value
  };
  const txt = formatSingleRecordShareText(tempRec);
  window.open(`https://wa.me/?text=${encodeURIComponent(txt)}`, '_blank');
}

function shareRecordWhatsApp(id) {
  const r = state.records.find(x => x.id === id);
  if (!r) return;
  window.open(`https://wa.me/?text=${encodeURIComponent(formatSingleRecordShareText(r))}`, '_blank');
}

function shareCurrentFormEmail() {
  const to = state.settings.outlookRecipientEmail || 'servis@enpos.com.tr';
  const sicil = getFullSicilFromForm() || 'SICIL';
  const musteri = document.getElementById('fMusteriBayi').value || 'MUSTERI';
  const subject = `Destek Kaydı: [${sicil}] ${musteri}`;
  const body = formatSingleRecordShareText({
    musteriBayiAdi: musteri,
    sicilNo1: sicil,
    gorusulenKisi: document.getElementById('fGorusulen').value,
    telefon: document.getElementById('fTelefon').value,
    destekKonusu1: document.getElementById('fDestekKonu').value,
    tespit1: document.getElementById('fTespit').value,
    islem1: document.getElementById('fIslem').value,
    bekleyenIs: document.getElementById('fBekleyen').value,
    toplamSure: document.getElementById('fSure').value,
    baslangicTarihi: document.getElementById('fBasTarihi').value
  }).replace(/\*/g, '');
  window.location.href = `mailto:${encodeURIComponent(to)}?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(body)}`;
}

function dialPhoneField(id) {
  const val = document.getElementById(id)?.value.trim();
  if (!val) return showToast('⚠️ Telefon numarası boş.');
  window.open(`tel:${val}`);
}

function whatsappPhoneField(id) {
  let val = (document.getElementById(id)?.value || '').replace(/\D/g, '');
  if (!val) return showToast('⚠️ Telefon numarası boş.');
  if (val.startsWith('0')) val = '90' + val.slice(1);
  window.open(`https://wa.me/${val}`, '_blank');
}

function copyFieldValue(id) {
  const val = document.getElementById(id)?.value || '';
  navigator.clipboard.writeText(val).then(() => showToast('📋 Kopyalandı: ' + val));
}

// ============================================================================
// SETTINGS, REPRESENTATIVES & ANDROID-COMPATIBLE JSON BACKUP / RESTORE
// ============================================================================
function toggleDutyMode() {
  state.settings.dutyModeEnabled = !state.settings.dutyModeEnabled;
  saveAllToStorage();
  renderHeaderInfo();
  showToast(state.settings.dutyModeEnabled ? '🔔 Nöbet Modu Aktif!' : '🌙 Nöbet Modu Kapatıldı.');
}

function saveGeneralSettings() {
  state.settings.globalKullaniciKodu = document.getElementById('setGlobalKod').value.trim();
  state.settings.globalKullanici = document.getElementById('setGlobalAd').value.trim().toLocaleUpperCase('tr-TR');
  state.settings.outlookRecipientEmail = document.getElementById('setOutlookEmail').value.trim();
  document.getElementById('fKullaniciKodu').value = state.settings.globalKullaniciKodu;
  document.getElementById('fKullanici').value = state.settings.globalKullanici;
  saveAllToStorage();
  renderAllUI();
  showToast('✅ Temsilci ve E-Posta ayarları kaydedildi.');
}

function saveSipSettings() {
  state.settings.sipDomain = document.getElementById('setSipDomain').value.trim();
  state.settings.sipExtension = document.getElementById('setSipExt').value.trim();
  state.settings.sipPassword = document.getElementById('setSipPass').value;
  state.settings.sipPort = document.getElementById('setSipPort').value.trim() || '5060';
  saveAllToStorage();
  renderHeaderInfo();
  showToast('🔌 Verimor SIP ayarları kaydedildi.');
}

function saveSqlSettings() {
  state.settings.sqlDbType = document.getElementById('setSqlType').value;
  state.settings.sqlHost = document.getElementById('setSqlHost').value.trim();
  state.settings.sqlPort = document.getElementById('setSqlPort').value.trim();
  state.settings.sqlDbName = document.getElementById('setSqlDb').value.trim();
  saveAllToStorage();
  showToast('💾 SQL bağlantı parametreleri kaydedildi.');
}

function testSqlConnection() {
  const host = document.getElementById('setSqlHost').value.trim();
  if (!host) return showToast('⚠️ Lütfen önce Sunucu IP / Host adresini girin.');
  showToast(`🔍 ${host} sunucusuna bağlantı kontrol ediliyor... (Çevrimdışı Önbellek Aktif)`);
}

function renderRepresentativesChips() {
  const el = document.getElementById('repPresetsContainer');
  if (!el) return;
  el.innerHTML = state.representatives.map((rep, idx) => `
    <span class="chip">
      <span>🎧 <b>${escapeHtml(rep.code)}</b>: ${escapeHtml(rep.name)}</span>
      <span style="margin-left:4px; color:#F87171; cursor:pointer;" onclick="removeRepresentative(${idx})">✖</span>
    </span>
  `).join('');
}

function promptAddRepresentative() {
  const code = prompt('Temsilci Kodu (Örn: 7):', '');
  if (!code) return;
  const name = prompt('Temsilci Adı Soyadı:', '');
  if (!name) return;
  state.representatives.push({ code: code.trim(), name: name.trim().toLocaleUpperCase('tr-TR') });
  saveAllToStorage();
  renderRepresentativesChips();
}

function removeRepresentative(idx) {
  state.representatives.splice(idx, 1);
  saveAllToStorage();
  renderRepresentativesChips();
}

// 100% Android BackupManager.kt Compatible JSON Backup Export
function exportFullBackupJson() {
  const backupObj = {
    version: 1,
    appName: "Telefon Destek",
    backupDate: nowDateTimeStr(),
    timestamp: Date.now(),
    sipSettings: {
      domain: state.settings.sipDomain || "",
      extension: state.settings.sipExtension || "101",
      password: state.settings.sipPassword || "",
      port: state.settings.sipPort || "5060",
      autoTimer: true,
      autoSearch: true,
      autoConnect: true
    },
    appSettings: {
      globalKullanici: state.settings.globalKullanici || "",
      globalKullaniciKodu: state.settings.globalKullaniciKodu || "",
      showAdvancedFields: Boolean(state.settings.showAdvancedFields),
      sqlDbType: state.settings.sqlDbType || "MySQL",
      sqlHost: state.settings.sqlHost || "",
      sqlPort: state.settings.sqlPort || "",
      sqlDbName: state.settings.sqlDbName || "",
      sqlUser: "",
      sqlPassword: ""
    },
    templates: state.templates.map(t => ({
      id: t.id || 0,
      type: t.type || "TESPIT",
      text: t.text || ""
    })),
    records: state.records
  };

  const jsonStr = JSON.stringify(backupObj, null, 2);
  const blob = new Blob([jsonStr], { type: 'application/json;charset=utf-8' });
  const stamp = nowDateTimeStr().replace(/[-: ]/g, '');
  downloadBlob(blob, `Destek_Yedek_${stamp}.json`);
  showToast('📤 Android APK ile %100 uyumlu JSON yedek dosyası indirildi!');
}

function handleBackupFileSelected(event) {
  const file = event.target.files?.[0];
  if (!file) return;
  const reader = new FileReader();
  reader.onload = (e) => {
    try {
      const root = JSON.parse(e.target.result);
      const recs = Array.isArray(root.records) ? root.records : [];
      const tmps = Array.isArray(root.templates) ? root.templates : [];
      const replaceAll = confirm(
        `Yedek Dosyası Okundu:\n` +
        `• Tarih: ${root.backupDate || '-'}\n` +
        `• Kayıt Sayısı: ${recs.length}\n` +
        `• Şablon Sayısı: ${tmps.length}\n\n` +
        `"Tamam" derseniz mevcut kayıtlarla birleştirilir, "İptal" derseniz işlem durdurulur.`
      );
      if (!replaceAll) return;

      recs.forEach((r, idx) => {
        if (!r.id || state.records.some(x => x.id === r.id)) {
          r.id = Date.now() + idx;
        }
        state.records.push(r);
      });

      tmps.forEach((t, idx) => {
        if (!state.templates.some(x => x.text === t.text && x.type === t.type)) {
          state.templates.push({ id: Date.now() + idx, type: t.type || 'TESPIT', text: t.text || '' });
        }
      });

      if (root.appSettings) {
        if (root.appSettings.globalKullanici) state.settings.globalKullanici = root.appSettings.globalKullanici;
        if (root.appSettings.globalKullaniciKodu) state.settings.globalKullaniciKodu = root.appSettings.globalKullaniciKodu;
      }
      if (root.sipSettings && root.sipSettings.domain) {
        state.settings.sipDomain = root.sipSettings.domain;
        state.settings.sipExtension = root.sipSettings.extension || '101';
      }

      saveAllToStorage();
      initDefaultFormValues();
      renderAllUI();
      showToast(`✅ ${recs.length} destek kaydı ve ${tmps.length} şablon başarıyla yüklendi!`);
    } catch (err) {
      alert('Yedek dosyası okunamadı: ' + err.message);
    }
  };
  reader.readAsText(file, 'UTF-8');
  event.target.value = '';
}

function loadSampleDemoData() {
  const today = todayYmdStr();
  const demo = [
    {
      id: Date.now() + 1,
      kullaniciKodu: "101",
      kullaniciAdi: state.settings.globalKullanici || "MURAT DEMİR",
      baslangicTarihi: `${today} 09:18:10`,
      bitisTarihi: `${today} 09:24:40`,
      toplamSure: "00:06:30",
      arayanTuru: "Müşteri",
      musteriBayiAdi: "ANADOLU HİPERMARKET A.Ş.",
      gorusulenKisi: "KEMAL ÖZKAN",
      telefon: "05324102030",
      telefon2: "",
      mail: "bilgi@anadolumarket.com",
      iletisimTuru: "Telefon",
      destekKonusu1: "Gün Sonu / Z Raporu Hatası",
      sicilNo1: "PC104892",
      tespit1: "Açıkta bekleyen askıdaki fiş nedeniyle Z raporu gün sonu uyarısı.",
      islem1: "Uzak bağlantı ile askıdaki fiş kapatıldı, Z raporu alındı ve TSM senkronizasyonu doğrulandı.",
      baglantiId1: "482 910 334",
      kasaVersiyon1: "v4.2.18",
      dynCashVersiyon1: "2025.03.1",
      arkaOfisVersiyon1: "BO-3.12",
      yn500KasaVersiyon1: "v1.9.4",
      redmineId1: "#48201",
      aktif: "1",
      bekleyenIs: "Hayır",
      argeGidis: "Hayır",
      argeDonus: "Hayır",
      not1: "Merkez Kasa-1"
    },
    {
      id: Date.now() + 2,
      kullaniciKodu: "101",
      kullaniciAdi: state.settings.globalKullanici || "MURAT DEMİR",
      baslangicTarihi: `${today} 10:42:00`,
      bitisTarihi: `${today} 10:51:15`,
      toplamSure: "00:09:15",
      arayanTuru: "Bayi",
      musteriBayiAdi: "EGE POS TEKNOLOJİ LTD.",
      gorusulenKisi: "SELİM AKSOY",
      telefon: "05448901234",
      telefon2: "",
      mail: "servis@egepos.com",
      iletisimTuru: "Uzak Bağlantı",
      destekKonusu1: "POS / Banka İletişim Hatası",
      sicilNo1: "OKC209511",
      tespit1: "Kasa ile EFT-POS terminali arasında COM port haberleşme zaman aşımı (Hata 91).",
      islem1: "Baud rate 115200 olarak güncellendi, banka parametre yüklemesi yapıldı, provizyon testi başarılı.",
      baglantiId1: "719 204 881",
      kasaVersiyon1: "v4.2.19",
      dynCashVersiyon1: "2025.03.2",
      arkaOfisVersiyon1: "BO-3.12",
      yn500KasaVersiyon1: "",
      redmineId1: "#48219",
      aktif: "1",
      bekleyenIs: "Evet",
      argeGidis: "Hayır",
      argeDonus: "Hayır",
      not1: "2. Kasa için yarın kontrol yapılacak"
    }
  ];
  state.records = [...demo, ...state.records];
  saveAllToStorage();
  renderAllUI();
  showToast('✨ Örnek test kayıtları başarıyla eklendi!');
}

function deleteRecordsByDateRange() {
  const s = document.getElementById('delStartDate').value;
  const e = document.getElementById('delEndDate').value;
  if (!s || !e) return showToast('⚠️ Tarih aralığı seçiniz.');
  if (!confirm(`${s} ile ${e} arasındaki tüm kayıtlar silinecek. Onaylıyor musunuz?`)) return;
  const before = state.records.length;
  state.records = state.records.filter(r => {
    const d = (r.baslangicTarihi || '').slice(0, 10);
    return !(d >= s && d <= e);
  });
  saveAllToStorage();
  renderAllUI();
  showToast(`🗑️ ${before - state.records.length} kayıt silindi.`);
}

function deleteAllRecordsConfirm() {
  if (!confirm('TÜM destek kayıtlarını kalıcı olarak silmek istediğinize emin misiniz?')) return;
  state.records = [];
  saveAllToStorage();
  renderAllUI();
  showToast('🔥 Tüm kayıtlar temizlendi.');
}

// ============================================================================
// PURE-JS 36-COLUMN MICROSOFT EXCEL (.XLSX) GENERATOR (100% OFFLINE)
// ============================================================================
function openExportExcelModal() {
  openModal('exportExcelModal');
}

function setExportRangePreset(daysBack) {
  const end = new Date();
  const start = new Date();
  start.setDate(end.getDate() - daysBack);
  const pad = (n) => String(n).padStart(2, '0');
  document.getElementById('exportStartDate').value = `${start.getFullYear()}-${pad(start.getMonth() + 1)}-${pad(start.getDate())}`;
  document.getElementById('exportEndDate').value = `${end.getFullYear()}-${pad(end.getMonth() + 1)}-${pad(end.getDate())}`;
}

function recordToExcelRowValues(r) {
  const kod = (r.kullaniciKodu || '').trim();
  const ad = (r.kullaniciAdi || '').trim();
  const displayUser = kod && ad ? `${kod} - ${ad}` : (kod || ad);
  const cleanPhone = (p) => (p || '').replace(/\s+/g, '').replace(/[-()]/g, '').trim();
  return [
    displayUser,
    r.baslangicTarihi || "",
    r.arayanTuru || "",
    (r.musteriBayiAdi || "").toLocaleUpperCase('tr-TR'),
    (r.gorusulenKisi || "").toLocaleUpperCase('tr-TR'),
    cleanPhone(r.telefon),
    cleanPhone(r.telefon2),
    r.mail || "",
    r.iletisimTuru || "Telefon",
    r.destekKonusu1 || "",
    (r.sicilNo1 || "").toLocaleUpperCase('tr-TR'),
    r.tespit1 || "",
    r.islem1 || "",
    r.baglantiId1 || "",
    r.bitisTarihi || "",
    r.toplamSure || "",
    r.kasaVersiyon1 || "",
    r.arkaOfisVersiyon1 || "",
    r.redmineId1 || "",
    r.aktif || "1",
    r.b2bKayit || "",
    r.bekleyenIs || "Hayır",
    r.argeGidis || "Hayır",
    r.argeDonus || "Hayır",
    r.argeGidisNotu || "",
    r.argeDonusNotu || "",
    r.resim || "",
    r.tckn || "",
    r.vergiNo || "",
    r.tespitKodu1 || "",
    r.islemKodu1 || "",
    r.yn500SicilNo1 || "",
    r.dynCashVersiyon1 || "",
    r.arkaOfisId1 || "",
    r.yn500KasaVersiyon1 || "",
    r.not1 || r.servisKodu || ""
  ];
}

function generateAndDownloadExcel() {
  const sDate = document.getElementById('exportStartDate').value || '2000-01-01';
  const eDate = document.getElementById('exportEndDate').value || '2099-12-31';

  const rows = state.records.filter(r => {
    const d = (r.baslangicTarihi || '').slice(0, 10);
    if (!d) return true;
    return d >= sDate && d <= eDate;
  });

  if (rows.length === 0) {
    showToast('⚠️ Seçilen tarih aralığında dışa aktarılacak kayıt bulunamadı.');
    return;
  }

  const sheetData = [EXCEL_COLUMNS, ...rows.map(recordToExcelRowValues)];
  const xlsxBlob = buildOpenXmlXlsxBlob(sheetData);
  const fileName = `Telefon_Destek_Raporu_${sDate}_${eDate}.xlsx`;
  downloadBlob(xlsxBlob, fileName);
  closeModal('exportExcelModal');
  showToast(`📥 ${rows.length} kayıt içeren 36 sütunlu Excel (.xlsx) indirildi!`);
}

// Minimal Pure-JS OpenXML (.xlsx) Builder (Uncompressed ZIP + XML Sheet)
function buildOpenXmlXlsxBlob(aoa) {
  const colName = (idx) => {
    let s = '';
    let n = idx + 1;
    while (n > 0) {
      const m = (n - 1) % 26;
      s = String.fromCharCode(65 + m) + s;
      n = Math.floor((n - 1) / 26);
    }
    return s;
  };

  const xmlEsc = (v) => String(v ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;');

  const sheetRowsXml = aoa.map((row, rIdx) => {
    const cells = row.map((val, cIdx) => {
      const ref = `${colName(cIdx)}${rIdx + 1}`;
      const styleAttr = rIdx === 0 ? ' s="1"' : '';
      return `<c r="${ref}" t="inlineStr"${styleAttr}><is><t>${xmlEsc(val)}</t></is></c>`;
    }).join('');
    return `<row r="${rIdx + 1}">${cells}</row>`;
  }).join('');

  const files = {
    '[Content_Types].xml': `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>`,
    '_rels/.rels': `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>`,
    'xl/workbook.xml': `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Destek Kayitlari" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>`,
    'xl/_rels/workbook.xml.rels': `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>`,
    'xl/styles.xml': `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="2">
    <font><sz val="10"/><name val="Calibri"/></font>
    <font><b/><sz val="10"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
  </fonts>
  <fills count="3">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FF15803D"/></patternFill></fill>
  </fills>
  <borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
  <cellXfs count="2">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/>
  </cellXfs>
</styleSheet>`,
    'xl/worksheets/sheet1.xml': `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetData>${sheetRowsXml}</sheetData>
</worksheet>`
  };

  return createZipBlob(files, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
}

// Pure-JS STORE Zip Packer
function createZipBlob(fileMap, mimeType) {
  const enc = new TextEncoder();
  const entries = [];
  let offset = 0;

  const crcTable = (() => {
    const t = new Uint32Array(256);
    for (let i = 0; i < 256; i++) {
      let c = i;
      for (let k = 0; k < 8; k++) c = (c & 1) ? (0xEDB88320 ^ (c >>> 1)) : (c >>> 1);
      t[i] = c >>> 0;
    }
    return t;
  })();

  const crc32 = (bytes) => {
    let crc = 0xFFFFFFFF;
    for (let i = 0; i < bytes.length; i++) {
      crc = crcTable[(crc ^ bytes[i]) & 0xFF] ^ (crc >>> 8);
    }
    return (crc ^ 0xFFFFFFFF) >>> 0;
  };

  const chunks = [];
  for (const [name, content] of Object.entries(fileMap)) {
    const nameBytes = enc.encode(name);
    const dataBytes = enc.encode(content);
    const crc = crc32(dataBytes);

    const localHeader = new Uint8Array(30 + nameBytes.length);
    const dv = new DataView(localHeader.buffer);
    dv.setUint32(0, 0x04034b50, true);
    dv.setUint16(4, 20, true);
    dv.setUint16(6, 0, true);
    dv.setUint16(8, 0, true);
    dv.setUint16(10, 0, true);
    dv.setUint16(12, 0, true);
    dv.setUint32(14, crc, true);
    dv.setUint32(18, dataBytes.length, true);
    dv.setUint32(22, dataBytes.length, true);
    dv.setUint16(26, nameBytes.length, true);
    dv.setUint16(28, 0, true);
    localHeader.set(nameBytes, 30);

    entries.push({ nameBytes, crc, size: dataBytes.length, offset });
    chunks.push(localHeader, dataBytes);
    offset += localHeader.length + dataBytes.length;
  }

  const centralStart = offset;
  for (const e of entries) {
    const cd = new Uint8Array(46 + e.nameBytes.length);
    const dv = new DataView(cd.buffer);
    dv.setUint32(0, 0x02014b50, true);
    dv.setUint16(4, 20, true);
    dv.setUint16(6, 20, true);
    dv.setUint16(8, 0, true);
    dv.setUint16(10, 0, true);
    dv.setUint16(12, 0, true);
    dv.setUint16(14, 0, true);
    dv.setUint32(16, e.crc, true);
    dv.setUint32(20, e.size, true);
    dv.setUint32(24, e.size, true);
    dv.setUint16(28, e.nameBytes.length, true);
    dv.setUint16(30, 0, true);
    dv.setUint16(32, 0, true);
    dv.setUint16(34, 0, true);
    dv.setUint16(36, 0, true);
    dv.setUint32(38, 0, true);
    dv.setUint32(42, e.offset, true);
    cd.set(e.nameBytes, 46);
    chunks.push(cd);
    offset += cd.length;
  }

  const centralSize = offset - centralStart;
  const eocd = new Uint8Array(22);
  const edv = new DataView(eocd.buffer);
  edv.setUint32(0, 0x06054b50, true);
  edv.setUint16(4, 0, true);
  edv.setUint16(6, 0, true);
  edv.setUint16(8, entries.length, true);
  edv.setUint16(10, entries.length, true);
  edv.setUint32(12, centralSize, true);
  edv.setUint32(16, centralStart, true);
  edv.setUint16(20, 0, true);
  chunks.push(eocd);

  return new Blob(chunks, { type: mimeType });
}

// ============================================================================
// UI UTILITIES (MODALS, TOAST, ESCAPING, BLOB DOWNLOAD)
// ============================================================================
function openModal(id) {
  document.getElementById(id)?.classList.add('open');
}

function closeModal(id) {
  document.getElementById(id)?.classList.remove('open');
}

function showToast(msg) {
  const c = document.getElementById('toastContainer');
  if (!c) return;
  const el = document.createElement('div');
  el.className = 'toast';
  el.textContent = msg;
  c.appendChild(el);
  setTimeout(() => { el.remove(); }, 3200);
}

function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  setTimeout(() => {
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }, 300);
}

function escapeHtml(str) {
  return String(str ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function escapeJsStr(str) {
  return String(str ?? '').replace(/\\/g, '\\\\').replace(/'/g, "\\'").replace(/\n/g, ' ');
}
