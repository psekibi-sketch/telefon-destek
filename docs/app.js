/* ============================================================================
 * TELEFON DESTEK — TAMAMEN OFFLINE ÇALIŞABİLEN & YEREL DEPOLAMALI PWA MOTORU
 * Android APK ile %100 Birebir Özellik, Ekran, Form ve Veri Uyumlu
 * ============================================================================
 * 1. Dual-Layer Kalıcı Depolama (IndexedDB + localStorage + StorageManager.persist)
 *    Normal sekme ile PWA Ana Ekran kısayolu arasında scope uyuşmazlığını önler.
 * 2. Ofis İçi Wi-Fi Sync Modülü (Varsayılan: http://172.16.160.21:8080)
 *    Offline biriken tüm çağrıları tek tuşla POST /api/sync-calls adresine aktarır,
 *    POST /api/fetch-calls ile SQL'den tarih bazlı veri çeker,
 *    POST /api/test-connection ile bağlantıyı test eder.
 * 3. Ana Ekran 2 Alt Sekme:
 *    - 1. Çağrı Girişi (7 Temel Alan + Sicil Ön Ek & 8 Hane Otomatik Tamamlama +
 *      Çoklu Seçimli Tespit/İşlem Çipleri + Sesli Dikte + Karalama Defteri)
 *    - 2. Gelişmiş Alanlar (36 Sütun Kurumsal Şema: İletişim Türü, Tel 2, Mail,
 *      Redmine, AnyDesk, B2B, AR-GE Gidiş/Dönüş, Cihaz Versiyonları, TCKN/VN)
 * 4. Form Koruma & Kuyrukta Bekleyen Çağrılar Havuzu
 * 5. 4 Sekmeli Servis Müdürü & Yönetici Denetim Paneli (Temsilciler, Grafikler,
 *    Çağrı Listesi, Rapor Önizleme)
 * 6. Gerçek 36 Sütunlu Microsoft Excel (.xlsx) Üretici (100% Offline ZIP/OpenXML)
 * ============================================================================ */

const STORAGE_KEYS = {
  RECORDS: 'telefon_destek_records_v3',
  MISSED: 'telefon_destek_missed_v3',
  WAITING_QUEUE: 'telefon_destek_waiting_queue_v3',
  SETTINGS: 'telefon_destek_settings_v3',
  TEMPLATES: 'telefon_destek_templates_v3',
  SCENARIOS: 'telefon_destek_scenarios_v3',
  KB: 'telefon_destek_kb_v3',
  REPS: 'telefon_destek_reps_v3',
  PREFIXES: 'telefon_destek_prefixes_v3',
  SCRATCHPAD: 'telefon_destek_scratchpad_v3'
};

const IDB_NAME = 'TelefonDestekOfflinePWA_DB';
const IDB_VERSION = 2;
const IDB_STORE = 'kv_store';

const DEFAULT_PREFIXES = ['PC', 'Beko', 'Profilo', 'Vestel', 'Arçelik', 'Hugin', 'Ingenico', 'Vera', 'Pavo', 'Inpos', 'Diğer'];

const DEFAULT_REPS = [
  { code: '1', name: 'AHMET YILMAZ' },
  { code: '2', name: 'MEHMET KAYA' },
  { code: '3', name: 'AYŞE DEMİR' },
  { code: '4', name: 'FATMA ÇELİK' },
  { code: '5', name: 'ALİ ŞAHİN' },
  { code: '6', name: 'MURAT DEMİR' },
  { code: '7', name: 'ZEYNEP YILDIZ' },
  { code: '8', name: 'EMRE KOÇ' }
];

const DEFAULT_TEMPLATES = {
  KONU: [
    'YAZARKASA POS BAĞLANTI HATASI',
    'GÜN SONU / Z RAPORU ALINAMIYOR',
    'E-FATURA / E-ARŞİV ENTEGRASYON',
    'BANKA POS EŞLEŞTİRME (BKM TECHPOS)',
    'FİŞ / KAĞIT SIKIŞMASI & YAZICI HATASI',
    'ARKA OFİS / DYNCASH FİYAT AKTARIMI',
    'VERSİYON GÜNCELLEME TALEBİ',
    'TERAZİ / BARKOD OKUYUCU HABERLEŞME',
    'MALİ HAFIZA / EKÜ DOLU UYARISI',
    'AĞ / ETHERNET / İNTERNET KOPMASI'
  ],
  TESPIT: [
    'GİB SUNUCU BAĞLANTISI ZAMAN AŞIMINA UĞRUYOR',
    'CİHAZ IP ADRESİ ALAMIYOR (DHCP KAPALI)',
    'AÇIK KALMIŞ SATIŞ FİŞİ NEDENİYLE Z RAPORU VERMİYOR',
    'TERMAL YAZICI KAPAĞI VEYA SENSÖRÜ OKUMUYOR',
    'DYNCASH İLE ARKA OFİS SQL SERVİSİ DURMUŞ',
    'BANKA UYGULAMASI GÜNCEL DEĞİL (PARAMETRE EKSİK)',
    'SERİ PORT / COM PORT EŞLEŞMESİ BOZULMUŞ',
    'TARİH VE SAAT UYUŞMAZLIĞI MEVCUT',
    'KULLANICI YETKİ / ŞİFRE HATASI ALIYOR'
  ],
  ISLEM: [
    'STATİK IP VE DNS (8.8.8.8) TANIMLANDI, BAĞLANTI SAĞLANDI',
    'AÇIK FİŞ İPTAL EDİLİP Z RAPORU ALDIRILDI',
    'ANYDESK İLE BAĞLANILARAK SERVİSLER YENİDEN BAŞLATILDI',
    'BKM TECHPOS PARAMETRE YÜKLEMESİ VE EŞLEŞTİRME YAPILDI',
    'VERSİYON GÜNCELLEMESİ TAMAMLANDI, TEST SATIŞI YAPILDI',
    'YAZICI MEKANİZMASI TEMİZLENDİ, RULO DÜZELTİLDİ',
    'ARKA OFİS VERİTABANI AKTARIMI MANUEL TETİKLENDİ',
    'CİHAZ DONANIM ARIZASI NEDENİYLE SERVİSE YÖNLENDİRİLDİ',
    'MÜŞTERİYE KULLANIM BİLGİSİ VERİLDİ, SORUN GİDERİLDİ'
  ]
};

const DEFAULT_SCENARIOS = [
  {
    id: 'sc_1',
    icon: '🧾',
    title: 'Z Raporu & Açık Fiş',
    konu: 'GÜN SONU / Z RAPORU ALINAMIYOR',
    tespit: 'AÇIK KALMIŞ SATIŞ FİŞİ NEDENİYLE Z RAPORU VERMİYOR',
    islem: 'AÇIK FİŞ İPTAL EDİLİP Z RAPORU ALDIRILDI'
  },
  {
    id: 'sc_2',
    icon: '🌐',
    title: 'İnternet / DNS',
    konu: 'AĞ / ETHERNET / İNTERNET KOPMASI',
    tespit: 'CİHAZ IP ADRESİ ALAMIYOR (DHCP KAPALI)',
    islem: 'STATİK IP VE DNS (8.8.8.8) TANIMLANDI, BAĞLANTI SAĞLANDI'
  },
  {
    id: 'sc_3',
    icon: '💳',
    title: 'Banka Eşleşme',
    konu: 'BANKA POS EŞLEŞTİRME (BKM TECHPOS)',
    tespit: 'BANKA UYGULAMASI GÜNCEL DEĞİL (PARAMETRE EKSİK)',
    islem: 'BKM TECHPOS PARAMETRE YÜKLEMESİ VE EŞLEŞTİRME YAPILDI'
  },
  {
    id: 'sc_4',
    icon: '💻',
    title: 'Arka Ofis Aktarım',
    konu: 'ARKA OFİS / DYNCASH FİYAT AKTARIMI',
    tespit: 'DYNCASH İLE ARKA OFİS SQL SERVİSİ DURMUŞ',
    islem: 'ANYDESK İLE BAĞLANILARAK SERVİSLER YENİDEN BAŞLATILDI'
  },
  {
    id: 'sc_5',
    icon: '🖨️',
    title: 'Yazıcı / Kağıt',
    konu: 'FİŞ / KAĞIT SIKIŞMASI & YAZICI HATASI',
    tespit: 'TERMAL YAZICI KAPAĞI VEYA SENSÖRÜ OKUMUYOR',
    islem: 'YAZICI MEKANİZMASI TEMİZLENDİ, RULO DÜZELTİLDİ'
  }
];

const DEFAULT_KB = [
  {
    id: 'kb_1',
    code: 'ERR-101 / GİB-408',
    category: 'GİB & AĞ',
    title: 'GİB Sunucu Bağlantı Hatası / Paket Gönderilemedi',
    symptoms: 'Cihaz Z raporu gönderemiyor, GİB bağlantı testi başarısız oluyor.',
    solution: '1. Ayarlar > Ağ Ayarları > Ethernet/Wi-Fi kontrol edin.\n2. Sabit IP verip DNS1: 8.8.8.8, DNS2: 1.1.1.1 tanımlayın.\n3. GİB Test menüsünden "Bekleyen Paketleri Gönder" işlemini başlatın.',
    konu: 'AĞ / ETHERNET / İNTERNET KOPMASI',
    tespit: 'GİB SUNUCU BAĞLANTISI ZAMAN AŞIMINA UĞRUYOR',
    islem: 'STATİK IP VE DNS (8.8.8.8) TANIMLANDI, BAĞLANTI SAĞLANDI'
  },
  {
    id: 'kb_2',
    code: 'ERR-204 / Z-ACIKFIS',
    category: 'MALİ İŞLEMLER',
    title: 'Z Raporu Alınamıyor (Açık Fiş / Belge Uyarısı)',
    symptoms: 'Gün sonu Z raporu alırken "Açık Fiş Var" veya "Belge Tamamlanmadı" uyarısı veriyor.',
    solution: '1. Kasiyer menüsünden Satış ekranına girin.\n2. Ara Toplam > Belge İptal tuşuna basın.\n3. Raporlar > Z Raporu menüsünden gün sonunu alın.',
    konu: 'GÜN SONU / Z RAPORU ALINAMIYOR',
    tespit: 'AÇIK KALMIŞ SATIŞ FİŞİ NEDENİYLE Z RAPORU VERMİYOR',
    islem: 'AÇIK FİŞ İPTAL EDİLİP Z RAPORU ALDIRILDI'
  },
  {
    id: 'kb_3',
    code: 'POS-91 / BKM-TECHPOS',
    category: 'BANKA & ÖDEME',
    title: 'Banka POS Cevap Vermiyor / Eşleşme Hatası',
    symptoms: 'Kredi kartı çekiminde "Banka Uygulaması Yok" veya "Eşleşme Hatası 91" veriyor.',
    solution: '1. BKM TechPOS menüsüne girin.\n2. Parametre Yükleme / Banka Güncelleme seçin.\n3. Cihazı yeniden başlatıp 1 TL test çekimi yapın.',
    konu: 'BANKA POS EŞLEŞTİRME (BKM TECHPOS)',
    tespit: 'BANKA UYGULAMASI GÜNCEL DEĞİL (PARAMETRE EKSİK)',
    islem: 'BKM TECHPOS PARAMETRE YÜKLEMESİ VE EŞLEŞTİRME YAPILDI'
  },
  {
    id: 'kb_4',
    code: 'SQL-500 / DYNCASH',
    category: 'ARKA OFİS',
    title: 'DynCash & Arka Ofis Fiyat / PLU Güncellenmiyor',
    symptoms: 'Arka ofiste değişen fiyatlar kasaya düşmüyor, haberleşme servisi durmuş.',
    solution: '1. Ana bilgisayara AnyDesk ile bağlanın.\n2. Windows Hizmetler (services.msc) üzerinden SQL Server ve Enpos Aktarım servisini yeniden başlatın.\n3. Kasadan "Veri Al" butonuna basın.',
    konu: 'ARKA OFİS / DYNCASH FİYAT AKTARIMI',
    tespit: 'DYNCASH İLE ARKA OFİS SQL SERVİSİ DURMUŞ',
    islem: 'ANYDESK İLE BAĞLANILARAK SERVİSLER YENİDEN BAŞLATILDI'
  }
];

/* --- UYGULAMA DURUMU (STATE) --- */
const state = {
  records: [],
  missedCalls: [],
  waitingQueue: [],
  prefixes: [...DEFAULT_PREFIXES],
  reps: [...DEFAULT_REPS],
  templates: JSON.parse(JSON.stringify(DEFAULT_TEMPLATES)),
  scenarios: JSON.parse(JSON.stringify(DEFAULT_SCENARIOS)),
  kb: JSON.parse(JSON.stringify(DEFAULT_KB)),
  scratchpad: '',
  settings: {
    globalKod: '6',
    globalAd: 'MURAT DEMİR',
    outlookEmail: 'servis@enpos.com.tr',
    masterPower: true,
    dutyMode: true,
    selectedPrefix: 'PC',
    sipDomain: 'santral.verimor.com.tr',
    sipExt: '101',
    sipPass: '',
    sipPort: '5060',
    sipAutoTimer: true,
    sipAutoSearch: true,
    sqlHost: '172.16.160.21',
    sqlPort: '8080',
    sqlType: 'MSSQL',
    sqlDb: 'destek_db',
    sqlUser: 'sa',
    sqlPass: '',
    _updatedAt: 0
  },
  // Bağlantı ve SIP durumu varsayılan olarak ASLA yeşil (CONNECTED) olmaz!
  // Sadece /api/test-db-connection başarılı yanıt döndüğünde 'CONNECTED' olur.
  connectionStatus: 'DISCONNECTED', // 'DISCONNECTED' (Gri/Bekliyor) | 'TESTING' (Turuncu) | 'CONNECTED' (Yeşil/Bağlı) | 'ERROR' (Kırmızı/Çevrimdışı)
  lastConnectionMessage: '',
  activeTab: 0,
  activeFormPage: 0,
  editingRecordId: null,
  historyFilter: 'ALL',
  callLogFilter: 'ALL',
  statsRange: 'ALL',
  managerPeriod: 'TODAY',
  managerTab: 0,
  managerRepFilter: null,
  timerRunning: false,
  timerSeconds: 0,
  timerInterval: null,
  activeSipCall: null, // { number, state: 'RINGING'|'INCALL', muted: false, speaker: false }
  candidateList: []
};

/* ============================================================================
 * 1. ÇİFT KATMANLI KALICI DEPOLAMA (INDEXEDDB + LOCALSTORAGE + PERSISTENCE)
 * ============================================================================ */
let idbInstance = null;

function openIndexedDB() {
  return new Promise((resolve) => {
    if (!window.indexedDB) {
      resolve(null);
      return;
    }
    if (idbInstance) {
      resolve(idbInstance);
      return;
    }
    try {
      const req = window.indexedDB.open(IDB_NAME, IDB_VERSION);
      req.onupgradeneeded = (e) => {
        const db = e.target.result;
        if (!db.objectStoreNames.contains(IDB_STORE)) {
          db.createObjectStore(IDB_STORE);
        }
      };
      req.onsuccess = (e) => {
        idbInstance = e.target.result;
        resolve(idbInstance);
      };
      req.onerror = () => resolve(null);
    } catch (_) {
      resolve(null);
    }
  });
}

async function idbGet(key) {
  const db = await openIndexedDB();
  if (!db) return null;
  return new Promise((resolve) => {
    try {
      const tx = db.transaction(IDB_STORE, 'readonly');
      const store = tx.objectStore(IDB_STORE);
      const req = store.get(key);
      req.onsuccess = () => resolve(req.result !== undefined ? req.result : null);
      req.onerror = () => resolve(null);
    } catch (_) {
      resolve(null);
    }
  });
}

async function idbSet(key, val) {
  const db = await openIndexedDB();
  if (!db) return;
  return new Promise((resolve) => {
    try {
      const tx = db.transaction(IDB_STORE, 'readwrite');
      const store = tx.objectStore(IDB_STORE);
      store.put(val, key);
      tx.oncomplete = () => resolve(true);
      tx.onerror = () => resolve(false);
    } catch (_) {
      resolve(false);
    }
  });
}

const PROTECTED_USER_VAULT_CACHE = 'telefon-destek-user-vault-permanent';
const PERMANENT_SETTING_ATOMS = [
  'globalKod',
  'globalAd',
  'outlookEmail',
  'masterPower',
  'dutyMode',
  'selectedPrefix',
  'sipDomain',
  'sipExt',
  'sipPass',
  'sipPort',
  'sipAutoTimer',
  'sipAutoSearch',
  'sqlHost',
  'sqlPort',
  'sqlType',
  'sqlDb',
  'sqlUser',
  'sqlPass'
];

async function vaultPut(key, val) {
  if (!('caches' in window)) return;
  try {
    const cache = await caches.open(PROTECTED_USER_VAULT_CACHE);
    const resp = new Response(JSON.stringify(val), {
      headers: { 'Content-Type': 'application/json' }
    });
    await cache.put(`/__user_vault__/${encodeURIComponent(key)}`, resp);
  } catch (_) {}
}

async function vaultGet(key) {
  if (!('caches' in window)) return null;
  try {
    const cache = await caches.open(PROTECTED_USER_VAULT_CACHE);
    const resp = await cache.match(`/__user_vault__/${encodeURIComponent(key)}`);
    if (!resp) return null;
    return await resp.json();
  } catch (_) {
    return null;
  }
}

function writeAtomicSettingsBackup(settingsObj) {
  if (!settingsObj || typeof settingsObj !== 'object') return;
  try {
    PERMANENT_SETTING_ATOMS.forEach((field) => {
      if (settingsObj[field] !== undefined && settingsObj[field] !== null) {
        localStorage.setItem(`td_perm_${field}`, JSON.stringify(settingsObj[field]));
      }
    });
    localStorage.setItem('td_protected_settings_backup', JSON.stringify(settingsObj));
  } catch (_) {}
}

function readAtomicSettingsBackup() {
  const recovered = {};
  let hasAny = false;
  try {
    const backupRaw = localStorage.getItem('td_protected_settings_backup');
    if (backupRaw) {
      const parsed = JSON.parse(backupRaw);
      if (parsed && typeof parsed === 'object') {
        Object.assign(recovered, parsed);
        hasAny = true;
      }
    }
    // Eski sürüm anahtarlarından da otomatik kurtarma (v1 / v2 / düz anahtarlar)
    ['telefon_destek_settings_v2', 'telefon_destek_settings_v1', 'telefon_destek_settings'].forEach((legacyKey) => {
      const raw = localStorage.getItem(legacyKey);
      if (raw) {
        try {
          const parsed = JSON.parse(raw);
          if (parsed && typeof parsed === 'object') {
            Object.assign(recovered, parsed);
            hasAny = true;
          }
        } catch (_) {}
      }
    });
    PERMANENT_SETTING_ATOMS.forEach((field) => {
      const atomRaw = localStorage.getItem(`td_perm_${field}`);
      if (atomRaw !== null) {
        try {
          recovered[field] = JSON.parse(atomRaw);
          hasAny = true;
        } catch (_) {
          recovered[field] = atomRaw;
          hasAny = true;
        }
      }
    });
  } catch (_) {}
  return hasAny ? recovered : null;
}

function saveToStorage(key, value) {
  let valToSave = value;
  if (key === STORAGE_KEYS.SETTINGS && valToSave && typeof valToSave === 'object') {
    valToSave._updatedAt = Date.now();
    writeAtomicSettingsBackup(valToSave);
  }
  try {
    const serialized = JSON.stringify(valToSave);
    localStorage.setItem(key, serialized);
  } catch (e) {
    console.warn('localStorage yazma hatası:', e);
  }
  idbSet(key, valToSave);
  vaultPut(key, valToSave);
  updateStorageMetricsUI();
}

async function loadFromStorageDual(key, fallback) {
  let localVal = null;
  try {
    const raw = localStorage.getItem(key);
    if (raw) localVal = JSON.parse(raw);
  } catch (_) {}

  const idbVal = await idbGet(key);
  const vaultVal = await vaultGet(key);

  // 1. DİZİLER (Çağrı Kayıtları, Kaçan Çağrılar, Şablonlar vb.):
  // En çok kayda sahip olan veya boş olmayan katmanı seç, ayrıca eski sürüm kayıt anahtarlarını da birleştir
  if (Array.isArray(fallback)) {
    const a = Array.isArray(localVal) ? localVal : [];
    const b = Array.isArray(idbVal) ? idbVal : [];
    const c = Array.isArray(vaultVal) ? vaultVal : [];
    let best = a;
    if (b.length > best.length) best = b;
    if (c.length > best.length) best = c;

    if (key === STORAGE_KEYS.RECORDS && best.length === 0) {
      // Eski versiyon kayıt anahtarlarını kontrol et
      for (const oldKey of ['telefon_destek_records_v2', 'telefon_destek_records_v1', 'telefon_destek_records']) {
        try {
          const oldRaw = localStorage.getItem(oldKey);
          if (oldRaw) {
            const oldArr = JSON.parse(oldRaw);
            if (Array.isArray(oldArr) && oldArr.length > best.length) {
              best = oldArr;
            }
          }
        } catch (_) {}
      }
    }

    const chosen = best.length > 0 ? best : fallback;
    try {
      localStorage.setItem(key, JSON.stringify(chosen));
    } catch (_) {}
    idbSet(key, chosen);
    vaultPut(key, chosen);
    return chosen;
  }

  // 2. KULLANICI AYARLARI OBJESİ (SIP, Kullanıcı Adı, SQL Sunucu ve Şifreler):
  // KESİN KORUMA: localStorage ve atomik yedekler her zaman varsayılan (fallback) değerlerin üzerindedir!
  if (key === STORAGE_KEYS.SETTINGS || (fallback && typeof fallback === 'object' && !Array.isArray(fallback))) {
    const atomicRecovered = key === STORAGE_KEYS.SETTINGS ? readAtomicSettingsBackup() : null;
    const vObj = vaultVal && typeof vaultVal === 'object' && !Array.isArray(vaultVal) ? vaultVal : {};
    const iObj = idbVal && typeof idbVal === 'object' && !Array.isArray(idbVal) ? idbVal : {};
    const lObj = localVal && typeof localVal === 'object' && !Array.isArray(localVal) ? localVal : {};
    const aObj = atomicRecovered && typeof atomicRecovered === 'object' ? atomicRecovered : {};

    // Öncelik sırası: fallback (en düşük) -> vault -> idb -> atomic backup -> localStorage (en yüksek/güncel)
    // Eğer idb veya vault'taki _updatedAt localStorage'dan daha yeniyse (örn. localStorage temizlenmişse) onu koru
    const localTime = Number(lObj._updatedAt || aObj._updatedAt || 0);
    const idbTime = Number(iObj._updatedAt || 0);
    const vaultTime = Number(vObj._updatedAt || 0);

    let merged;
    if (Object.keys(lObj).length > 0 || Object.keys(aObj).length > 0) {
      merged = Object.assign({}, fallback, vObj, iObj, aObj, lObj);
      if (idbTime > localTime && localTime > 0) {
        merged = Object.assign({}, fallback, vObj, aObj, lObj, iObj);
      }
      if (vaultTime > Math.max(localTime, idbTime) && localTime > 0) {
        merged = Object.assign({}, fallback, iObj, aObj, lObj, vObj);
      }
    } else {
      // localStorage boşsa (ör. tarayıcı önbelleği silindiyse) IndexedDB ve Vault'tan otomatik kurtar!
      merged = Object.assign({}, fallback, vObj, iObj, aObj);
    }

    try {
      localStorage.setItem(key, JSON.stringify(merged));
    } catch (_) {}
    if (key === STORAGE_KEYS.SETTINGS) {
      writeAtomicSettingsBackup(merged);
    }
    idbSet(key, merged);
    vaultPut(key, merged);
    return merged;
  }

  const finalVal = localVal !== null ? localVal : idbVal !== null ? idbVal : vaultVal !== null ? vaultVal : fallback;
  saveToStorage(key, finalVal);
  return finalVal;
}

async function requestPersistentStorage() {
  if (navigator.storage && navigator.storage.persist) {
    try {
      await navigator.storage.persist();
    } catch (_) {}
  }
}

/* ============================================================================
 * 2. UYGULAMA BAŞLATMA (INIT) & AKILLI SERVICE WORKER SÜRÜM KORUMASI
 * ============================================================================ */
window.addEventListener('DOMContentLoaded', async () => {
  await requestPersistentStorage();
  await loadAllApplicationData();
  registerServiceWorker();
  setupNetworkStatusListeners();
  initDefaultFormTimestamps();
  renderAllUI();
});

function registerServiceWorker() {
  if (!('serviceWorker' in navigator)) return;

  // Service Worker'dan gelen yeni sürüm mesajlarını dinle:
  // Eski önbellek temizlenirken localStorage ve kullanıcı ayarlarını yeniden mühürle!
  navigator.serviceWorker.addEventListener('message', (event) => {
    const data = event.data || {};
    if (data.type === 'SW_VERSION_UPDATED') {
      // Tüm mevcut kullanıcı ayarlarını (SIP, Temsilci, SQL) ve kayıtları koruma altına al
      writeAtomicSettingsBackup(state.settings);
      saveAllState();
      const prevVer = localStorage.getItem('td_installed_sw_version');
      localStorage.setItem('td_installed_sw_version', data.version || '5.2.0');
      if (prevVer && prevVer !== data.version) {
        showToast(
          `✨ Uygulama v${data.version} sürümüne güncellendi. SIP, kullanıcı ve offline kayıtlarınız korundu.`,
          'info'
        );
      }
    }
  });

  navigator.serviceWorker
    .register('./sw.js', { scope: './', updateViaCache: 'none' })
    .then((reg) => {
      // GitHub Pages'te yeni sürüm olup olmadığını kontrol et
      reg.update().catch(() => {});
    })
    .catch(() => {});
}

function setupNetworkStatusListeners() {
  const onNetworkChange = () => {
    if (!navigator.onLine) {
      // İnternet/Wi-Fi tamamen koptuğunda bağlantı durumunu तुरंत Çevrimdışı (DISCONNECTED) yap
      state.connectionStatus = 'DISCONNECTED';
    }
    updateConnectionStatusUI();
  };
  window.addEventListener('online', onNetworkChange);
  window.addEventListener('offline', onNetworkChange);
  updateConnectionStatusUI();
}

function updateConnectionStatusUI() {
  const topBadge = document.getElementById('offlineStatusBadge');
  const quickSipBtn = document.getElementById('quickSipBtn');
  const sipDot = document.getElementById('sipStatusDot');
  const sipTxt = document.getElementById('sipStatusText');
  const sqlBadge = document.getElementById('sqlConnectionStateBadge');
  const sipCardBadge = document.getElementById('sipConnectionStateBadge');
  const modalBadge = document.getElementById('modalSqlConnectionBadge');

  const ext = (state.settings.sipExt || '').trim();
  const hostPort = `${state.settings.sqlHost || '172.16.160.21'}:${state.settings.sqlPort || '8080'}`;
  const status = state.connectionStatus; // 'DISCONNECTED' | 'TESTING' | 'CONNECTED' | 'ERROR'

  // KURAL 3: Asla varsayılan olarak yeşil yanmaz! Sadece /api/test-db-connection başarılı olduğunda ('CONNECTED') yeşil yanar.
  if (status === 'CONNECTED') {
    if (topBadge) {
      topBadge.textContent = `🟢 Bağlı (${hostPort})`;
      topBadge.className = 'badge badge-green';
    }
    if (quickSipBtn) quickSipBtn.className = 'qt-btn qt-sip-connected';
    if (sipDot) sipDot.textContent = '🟢';
    if (sipTxt) sipTxt.textContent = ext ? `SIP: ${ext} (Bağlı)` : 'SIP: Bağlı';
    if (sqlBadge) {
      sqlBadge.textContent = '🟢 Bağlı (Doğrulandı)';
      sqlBadge.className = 'badge badge-green';
    }
    if (sipCardBadge) {
      sipCardBadge.textContent = ext ? `🟢 Bağlı (Dahili ${ext})` : '🟢 Bağlı';
      sipCardBadge.className = 'badge badge-green';
    }
    if (modalBadge) {
      modalBadge.textContent = '🟢 Bağlı (/api/test-db-connection OK)';
      modalBadge.className = 'badge badge-green';
    }
  } else if (status === 'TESTING') {
    if (topBadge) {
      topBadge.textContent = '⏳ Bağlantı Test Ediliyor...';
      topBadge.className = 'badge badge-orange';
    }
    if (quickSipBtn) quickSipBtn.className = 'qt-btn qt-sip-testing';
    if (sipDot) sipDot.textContent = '⏳';
    if (sipTxt) sipTxt.textContent = 'Test Ediliyor...';
    if (sqlBadge) {
      sqlBadge.textContent = '⏳ Test Ediliyor...';
      sqlBadge.className = 'badge badge-orange';
    }
    if (sipCardBadge) {
      sipCardBadge.textContent = '⏳ Test Ediliyor...';
      sipCardBadge.className = 'badge badge-orange';
    }
    if (modalBadge) {
      modalBadge.textContent = '⏳ Test Ediliyor...';
      modalBadge.className = 'badge badge-orange';
    }
  } else if (status === 'ERROR') {
    if (topBadge) {
      topBadge.textContent = '🔴 Çevrimdışı / Bağlantı Yok';
      topBadge.className = 'badge badge-red';
    }
    if (quickSipBtn) quickSipBtn.className = 'qt-btn qt-sip-error';
    if (sipDot) sipDot.textContent = '🔴';
    if (sipTxt) sipTxt.textContent = ext ? `SIP: ${ext} (Yok)` : 'SIP: Bağlı Değil';
    if (sqlBadge) {
      sqlBadge.textContent = '🔴 Bağlantı Yok / Çevrimdışı';
      sqlBadge.className = 'badge badge-red';
    }
    if (sipCardBadge) {
      sipCardBadge.textContent = '🔴 Çevrimdışı / Bağlı Değil';
      sipCardBadge.className = 'badge badge-red';
    }
    if (modalBadge) {
      modalBadge.textContent = '🔴 Bağlantı Başarısız';
      modalBadge.className = 'badge badge-red';
    }
  } else {
    // Varsayılan durum ('DISCONNECTED'): Gri (Çevrimdışı / Bekliyor)
    if (topBadge) {
      topBadge.textContent = '⚪ Çevrimdışı / Bekliyor';
      topBadge.className = 'badge badge-gray';
    }
    if (quickSipBtn) quickSipBtn.className = 'qt-btn qt-sip-disconnected';
    if (sipDot) sipDot.textContent = '⚪';
    if (sipTxt) sipTxt.textContent = ext ? `SIP: ${ext} (Bekliyor)` : 'SIP: Bekliyor';
    if (sqlBadge) {
      sqlBadge.textContent = '⚪ Çevrimdışı / Bekliyor';
      sqlBadge.className = 'badge badge-gray';
    }
    if (sipCardBadge) {
      sipCardBadge.textContent = '⚪ Çevrimdışı / Bekliyor';
      sipCardBadge.className = 'badge badge-gray';
    }
    if (modalBadge) {
      modalBadge.textContent = '⚪ Çevrimdışı / Bekliyor';
      modalBadge.className = 'badge badge-gray';
    }
  }
}

async function loadAllApplicationData() {
  state.settings = await loadFromStorageDual(STORAGE_KEYS.SETTINGS, state.settings);
  state.records = await loadFromStorageDual(STORAGE_KEYS.RECORDS, []);
  state.missedCalls = await loadFromStorageDual(STORAGE_KEYS.MISSED, []);
  state.waitingQueue = await loadFromStorageDual(STORAGE_KEYS.WAITING_QUEUE, []);
  state.prefixes = await loadFromStorageDual(STORAGE_KEYS.PREFIXES, DEFAULT_PREFIXES);
  state.reps = await loadFromStorageDual(STORAGE_KEYS.REPS, DEFAULT_REPS);
  state.templates = await loadFromStorageDual(STORAGE_KEYS.TEMPLATES, DEFAULT_TEMPLATES);
  state.scenarios = await loadFromStorageDual(STORAGE_KEYS.SCENARIOS, DEFAULT_SCENARIOS);
  state.kb = await loadFromStorageDual(STORAGE_KEYS.KB, DEFAULT_KB);
  state.scratchpad = await loadFromStorageDual(STORAGE_KEYS.SCRATCHPAD, '');

  // Eğer hiç kayıt yoksa ilk kurulum için zengin örnek kayıtlar yükle
  if (!Array.isArray(state.records) || state.records.length === 0) {
    seedInitialSampleRecords(false);
  }
}

function saveAllState() {
  saveToStorage(STORAGE_KEYS.SETTINGS, state.settings);
  saveToStorage(STORAGE_KEYS.RECORDS, state.records);
  saveToStorage(STORAGE_KEYS.MISSED, state.missedCalls);
  saveToStorage(STORAGE_KEYS.WAITING_QUEUE, state.waitingQueue);
  saveToStorage(STORAGE_KEYS.PREFIXES, state.prefixes);
  saveToStorage(STORAGE_KEYS.REPS, state.reps);
  saveToStorage(STORAGE_KEYS.TEMPLATES, state.templates);
  saveToStorage(STORAGE_KEYS.SCENARIOS, state.scenarios);
  saveToStorage(STORAGE_KEYS.KB, state.kb);
  saveToStorage(STORAGE_KEYS.SCRATCHPAD, state.scratchpad);
}

/* ============================================================================
 * 3. TARİH / SAAT YARDIMCILARI (DD.MM.YYYY HH:mm)
 * ============================================================================ */
function formatDateTimeTR(dateObj = new Date()) {
  const pad = (n) => String(n).padStart(2, '0');
  return `${pad(dateObj.getDate())}.${pad(dateObj.getMonth() + 1)}.${dateObj.getFullYear()} ${pad(dateObj.getHours())}:${pad(dateObj.getMinutes())}`;
}

function formatDateInputISO(dateObj = new Date()) {
  const pad = (n) => String(n).padStart(2, '0');
  return `${dateObj.getFullYear()}-${pad(dateObj.getMonth() + 1)}-${pad(dateObj.getDate())}`;
}

function parseRecordDateMs(str) {
  if (!str) return 0;
  const clean = String(str).trim();
  const m = clean.match(/^(\d{1,2})\.(\d{1,2})\.(\d{4})(?:\s+(\d{1,2}):(\d{1,2}))?/);
  if (m) {
    return new Date(Number(m[3]), Number(m[2]) - 1, Number(m[1]), Number(m[4] || 0), Number(m[5] || 0)).getTime();
  }
  const iso = Date.parse(clean);
  return isNaN(iso) ? 0 : iso;
}

function parseDurationToSeconds(durStr) {
  if (!durStr) return 0;
  const parts = String(durStr).trim().split(':').map(Number);
  if (parts.length === 2 && !isNaN(parts[0]) && !isNaN(parts[1])) {
    return parts[0] * 60 + parts[1];
  }
  if (parts.length === 3 && !isNaN(parts[0]) && !isNaN(parts[1]) && !isNaN(parts[2])) {
    return parts[0] * 3600 + parts[1] * 60 + parts[2];
  }
  return 0;
}

function formatSecondsMMSS(totalSec) {
  const sec = Math.max(0, Math.floor(totalSec || 0));
  const m = Math.floor(sec / 60);
  const s = sec % 60;
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
}

/* ============================================================================
 * 4. ANA EKRAN & SEKME GEÇİŞLERİ
 * ============================================================================ */
function switchTab(tabIdx) {
  state.activeTab = tabIdx;
  for (let i = 0; i < 4; i++) {
    const pane = document.getElementById(`tabContent${i}`);
    const nav = document.getElementById(`navBtn${i}`);
    if (pane) pane.style.display = i === tabIdx ? 'block' : 'none';
    if (nav) nav.classList.toggle('active', i === tabIdx);
  }
  if (tabIdx === 1) renderHistoryList();
  if (tabIdx === 2) renderMissedCallsList();
  if (tabIdx === 3) syncSettingsInputsFromState();
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function switchFormPage(pageIdx) {
  state.activeFormPage = pageIdx;
  const p0 = document.getElementById('formPage0');
  const p1 = document.getElementById('formPage1');
  const b0 = document.getElementById('subTabBtn0');
  const b1 = document.getElementById('subTabBtn1');
  if (p0) p0.style.display = pageIdx === 0 ? 'block' : 'none';
  if (p1) p1.style.display = pageIdx === 1 ? 'block' : 'none';
  if (b0) b0.classList.toggle('active', pageIdx === 0);
  if (b1) b1.classList.toggle('active', pageIdx === 1);
}

function renderAllUI() {
  renderTopBarAndQuickToolbar();
  renderSicilPrefixSelect();
  renderQuickScenariosRow();
  renderTemplatesAndPopularChips();
  renderWaitingCallsBanner();
  renderScratchpadUI();
  renderHistoryList();
  renderMissedCallsList();
  syncSettingsInputsFromState();
  renderRepresentativePresets();
  updateStorageMetricsUI();
}

function renderTopBarAndQuickToolbar() {
  const repSub = document.getElementById('headerRepSubtitle');
  const totalCount = state.records.length;
  const unsyncedCount = state.records.filter((r) => !r.isSyncedToSql).length;

  if (repSub) {
    const repLabel = state.settings.globalAd
      ? `👤 ${state.settings.globalAd} (${state.settings.globalKod || '-'})`
      : '👤 Temsilci Seçiniz';
    repSub.textContent = `${repLabel} • ${totalCount} Kayıt`;
  }

  // Master Power
  const pBtn = document.getElementById('masterPowerBtn');
  const pTxt = document.getElementById('masterPowerText');
  if (pBtn && pTxt) {
    pBtn.className = `top-pill-btn ${state.settings.masterPower ? 'top-pill-power-on' : 'top-pill-power-off'}`;
    pTxt.textContent = state.settings.masterPower ? 'ON' : 'OFF';
  }

  // Quick SQL Sync Button
  const sqlBtn = document.getElementById('quickSqlSyncBtn');
  const sqlTxt = document.getElementById('quickSqlSyncText');
  if (sqlBtn && sqlTxt) {
    sqlBtn.className = `qt-btn ${unsyncedCount > 0 ? 'qt-sql-unsynced' : 'qt-sql-synced'}`;
    sqlTxt.textContent = `SQL (${unsyncedCount})`;
  }

  // Gerçekçi SIP / API Bağlantı Durumu Göstergesini Güncelle
  updateConnectionStatusUI();

  // Settings Unsynced Badge
  const setBadge = document.getElementById('settingsUnsyncedBadge');
  if (setBadge) {
    setBadge.textContent = `${unsyncedCount} Bekleyen Kayıt`;
    setBadge.className = `badge ${unsyncedCount > 0 ? 'badge-orange' : 'badge-green'}`;
  }

  // Bottom Nav Badges
  const navHist = document.getElementById('navHistoryBadge');
  if (navHist) {
    navHist.style.display = totalCount > 0 ? 'inline-block' : 'none';
    navHist.textContent = totalCount > 999 ? '999+' : String(totalCount);
  }

  const unhandledMissed = state.missedCalls.filter((m) => !m.isHandled).length;
  const navMissed = document.getElementById('navMissedBadge');
  if (navMissed) {
    navMissed.style.display = unhandledMissed > 0 ? 'inline-block' : 'none';
    navMissed.textContent = String(unhandledMissed);
  }
}

function toggleMasterPower() {
  state.settings.masterPower = !state.settings.masterPower;
  saveToStorage(STORAGE_KEYS.SETTINGS, state.settings);
  renderTopBarAndQuickToolbar();
  showToast(
    state.settings.masterPower
      ? '⚡ Sistem Aktif: Otomatik sorgulama ve SIP dinleme açık.'
      : '⏸️ Sistem Pasif: Otomatik tetikleyiciler duraklatıldı.',
    state.settings.masterPower ? 'success' : 'info'
  );
}

/* ============================================================================
 * 5. SİCİL NO ÖN EK + 8 HANE OTOMATİK SIFIR TAMAMLAMA (ZERO-PADDING)
 * ============================================================================ */
function renderSicilPrefixSelect() {
  const sel = document.getElementById('fSicilPrefix');
  const inline = document.getElementById('sicilInlinePrefix');
  if (!sel) return;

  sel.innerHTML = state.prefixes
    .map((p) => `<option value="${escapeHtml(p)}" ${p === state.settings.selectedPrefix ? 'selected' : ''}>${escapeHtml(p)} ▾</option>`)
    .join('');

  if (inline) {
    inline.textContent = state.settings.selectedPrefix === 'Diğer' ? '' : state.settings.selectedPrefix;
  }
  updateCombinedSicilPreview();
}

function onSicilPrefixChange() {
  const sel = document.getElementById('fSicilPrefix');
  if (!sel) return;
  state.settings.selectedPrefix = sel.value;
  saveToStorage(STORAGE_KEYS.SETTINGS, state.settings);
  const inline = document.getElementById('sicilInlinePrefix');
  if (inline) {
    inline.textContent = sel.value === 'Diğer' ? '' : sel.value;
  }
  updateCombinedSicilPreview();
}

function onSicilDigitsInput(val) {
  const input = document.getElementById('fSicilDigits');
  if (!input) return;

  // Eğer kullanıcı PC000123 gibi ön ekli yapıştırdıysa otomatik ayır
  const matchedPrefix = state.prefixes.find(
    (p) => p !== 'Diğer' && val.toUpperCase().startsWith(p.toUpperCase())
  );
  if (matchedPrefix) {
    state.settings.selectedPrefix = matchedPrefix;
    const rawNum = val.slice(matchedPrefix.length).replace(/\D/g, '').slice(0, 8);
    input.value = rawNum;
    renderSicilPrefixSelect();
  } else if (state.settings.selectedPrefix !== 'Diğer') {
    input.value = val.replace(/\D/g, '').slice(0, 8);
  }
  updateCombinedSicilPreview();

  const fullSicil = getFullSicilFromForm();
  if (state.settings.masterPower && fullSicil.length >= 6) {
    triggerSmartLookup('SICIL', fullSicil, false);
  }
  updateCustomerPreviousCallsCard();
}

function padSicilDigitsOnBlur() {
  const input = document.getElementById('fSicilDigits');
  if (!input) return;
  const raw = input.value.trim();
  if (!raw) return;
  if (state.settings.selectedPrefix !== 'Diğer') {
    const digits = raw.replace(/\D/g, '');
    if (digits.length > 0 && digits.length < 8) {
      input.value = digits.padStart(8, '0');
    }
  }
  updateCombinedSicilPreview();
  updateCustomerPreviousCallsCard();
}

function getFullSicilFromForm() {
  const prefix = state.settings.selectedPrefix || 'PC';
  const rawDigits = (document.getElementById('fSicilDigits')?.value || '').trim();
  if (!rawDigits) return '';
  if (prefix === 'Diğer') return rawDigits.toUpperCase();
  const padded = rawDigits.replace(/\D/g, '').padStart(8, '0');
  return `${prefix}${padded}`;
}

function setSicilIntoForm(fullSicil) {
  const digitsInput = document.getElementById('fSicilDigits');
  if (!digitsInput) return;
  if (!fullSicil) {
    digitsInput.value = '';
    updateCombinedSicilPreview();
    return;
  }
  const clean = String(fullSicil).trim();
  const matched = state.prefixes.find(
    (p) => p !== 'Diğer' && clean.toUpperCase().startsWith(p.toUpperCase())
  );
  if (matched) {
    state.settings.selectedPrefix = matched;
    digitsInput.value = clean.slice(matched.length);
  } else {
    digitsInput.value = clean;
  }
  renderSicilPrefixSelect();
}

function updateCombinedSicilPreview() {
  const badge = document.getElementById('combinedSicilPreview');
  if (!badge) return;
  const full = getFullSicilFromForm();
  if (full) {
    badge.style.display = 'inline-flex';
    badge.textContent = full;
  } else {
    badge.style.display = 'none';
  }
}

function promptAddPrefix() {
  const p = prompt('Yeni Cihaz / Sicil Ön Eki girin (Örn: Pax, Verifone):');
  if (!p || !p.trim()) return;
  const clean = p.trim();
  if (!state.prefixes.includes(clean)) {
    state.prefixes.splice(state.prefixes.length - 1, 0, clean);
    state.settings.selectedPrefix = clean;
    saveToStorage(STORAGE_KEYS.PREFIXES, state.prefixes);
    saveToStorage(STORAGE_KEYS.SETTINGS, state.settings);
    renderSicilPrefixSelect();
    showToast(`✅ "${clean}" ön eki eklendi.`, 'success');
  }
}

/* ============================================================================
 * 6. AKILLI SORGULAMA (SMART AUTO-FILL) & MÜŞTERİ GEÇMİŞ ÇAĞRILARI KARTI
 * ============================================================================ */
function onMusteriInput(el) {
  const pos = el.selectionStart;
  el.value = el.value.toLocaleUpperCase('tr-TR');
  try { el.setSelectionRange(pos, pos); } catch (_) {}
  updateCustomerPreviousCallsCard();
}

function onGorusulenInput(el) {
  const pos = el.selectionStart;
  el.value = el.value.toLocaleUpperCase('tr-TR');
  try { el.setSelectionRange(pos, pos); } catch (_) {}
}

function onPhoneInput(el) {
  updateCustomerPreviousCallsCard();
  const digits = el.value.replace(/\D/g, '');
  if (state.settings.masterPower && digits.length >= 10) {
    triggerSmartLookup('TELEFON', digits, false);
  }
}

function normalizePhoneLast10(phone) {
  const digits = String(phone || '').replace(/\D/g, '');
  return digits.length >= 10 ? digits.slice(-10) : digits;
}

async function triggerSmartLookup(fieldType, rawQuery, showEmptyAlert = false) {
  const q = String(rawQuery || '').trim();
  if (!q || q.length < 2) {
    if (showEmptyAlert) showToast('Sorgulamak için en az 2 karakter girin.', 'info');
    return;
  }

  // 1. Önce yerel (Offline) veritabanında ara
  const matches = [];
  const seenKeys = new Set();

  for (const r of state.records) {
    let isMatch = false;
    if (fieldType === 'SICIL') {
      isMatch = (r.sicilNo || '').toUpperCase().includes(q.toUpperCase());
    } else if (fieldType === 'MUSTERI') {
      isMatch = (r.musteriBayi || '').toLocaleUpperCase('tr-TR').includes(q.toLocaleUpperCase('tr-TR'));
    } else if (fieldType === 'GORUSULEN') {
      isMatch = (r.gorusulen || '').toLocaleUpperCase('tr-TR').includes(q.toLocaleUpperCase('tr-TR'));
    } else if (fieldType === 'TELEFON') {
      const q10 = normalizePhoneLast10(q);
      const r10 = normalizePhoneLast10(r.telefon);
      isMatch = q10.length >= 7 && r10.includes(q10);
    }

    if (isMatch) {
      const uniqueKey = `${r.sicilNo || ''}|${r.musteriBayi || ''}|${normalizePhoneLast10(r.telefon)}`;
      if (!seenKeys.has(uniqueKey)) {
        seenKeys.add(uniqueKey);
        matches.push(r);
      }
    }
  }

  // 2. Eğer online ve yerel ağdaysak /api/search-customer üzerinden de dene
  if (matches.length === 0 && showEmptyAlert) {
    const remoteMatches = await searchCustomerOnOfficeApi(fieldType, q);
    remoteMatches.forEach((rm) => matches.push(rm));
  }

  if (matches.length === 0) {
    if (showEmptyAlert) {
      showToast(`"${q}" için eşleşen geçmiş müşteri/cihaz kaydı bulunamadı.`, 'info');
    }
    return;
  }

  // Kullanıcı Onaylı Seçim Modalını Aç
  state.candidateList = matches.slice(0, 20);
  renderCandidatePickerModal(q);
}

async function searchCustomerOnOfficeApi(fieldType, query) {
  const baseUrl = getOfficeApiBaseUrl();
  if (!baseUrl) return [];
  try {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 2800);
    const bodyObj = {
      host: state.settings.sqlHost,
      port: state.settings.sqlPort,
      dbName: state.settings.sqlDb,
      db_name: state.settings.sqlDb,
      dbType: state.settings.sqlType,
      db_type: state.settings.sqlType,
      user: state.settings.sqlUser || '',
      password: state.settings.sqlPass || '',
      fieldType,
      sicil_no: fieldType === 'SICIL' ? query : '',
      telefon: fieldType === 'TELEFON' ? query : '',
      gorusulen: fieldType === 'GORUSULEN' ? query : '',
      query
    };
    let resp = await fetch(`${baseUrl}/api/search-customers`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(bodyObj),
      signal: controller.signal
    }).catch(() => null);
    if (!resp || !resp.ok) {
      resp = await fetch(`${baseUrl}/api/search-customer`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(bodyObj),
        signal: controller.signal
      });
    }
    clearTimeout(timer);
    if (!resp || !resp.ok) return [];
    const data = await resp.json();
    const rawList = Array.isArray(data?.results)
      ? data.results
      : Array.isArray(data?.records)
      ? data.records
      : [];
    return rawList.map(normalizeIncomingRecord);
  } catch (_) {}
  return [];
}

function renderCandidatePickerModal(queryText) {
  const titleEl = document.getElementById('candidateModalTitle');
  const listEl = document.getElementById('candidatePickerList');
  if (!listEl) return;
  if (titleEl) {
    titleEl.textContent = `🔍 "${queryText}" İçin Eşleşen Kayıtlar (${state.candidateList.length})`;
  }

  listEl.innerHTML = state.candidateList
    .map(
      (c, idx) => `
      <div class="history-item" style="cursor:pointer;" onclick="applyCandidateToForm(${idx})">
        <div class="history-top">
          <div>
            <div class="history-customer">🏢 ${escapeHtml(c.musteriBayi || 'İsimsiz Müşteri')}</div>
            <div class="history-meta">
              🆔 <b>${escapeHtml(c.sicilNo || '-')}</b> • 👤 ${escapeHtml(c.gorusulen || '-')} • 📞 ${escapeHtml(c.telefon || '-')}
            </div>
          </div>
          <button type="button" class="btn btn-sm btn-blue">✅ Forma Doldur</button>
        </div>
        ${
          c.destekKonu || c.tespit
            ? `<div style="font-size:11px; color:var(--text-secondary); margin-top:4px;">
                📌 Son Konu: ${escapeHtml(c.destekKonu || '-')} | 🔍 ${escapeHtml(c.tespit || '-')}
              </div>`
            : ''
        }
      </div>
    `
    )
    .join('');

  openModal('candidatePickerModal');
}

function applyCandidateToForm(idx) {
  const c = state.candidateList[idx];
  if (!c) return;

  if (c.sicilNo) setSicilIntoForm(c.sicilNo);
  if (c.musteriBayi) document.getElementById('fMusteriBayi').value = c.musteriBayi;
  if (c.gorusulen) document.getElementById('fGorusulen').value = c.gorusulen;
  if (c.telefon) document.getElementById('fTelefon').value = c.telefon;
  if (c.telefon2) document.getElementById('fTelefon2').value = c.telefon2;
  if (c.mail) document.getElementById('fMail').value = c.mail;
  if (c.bagId) document.getElementById('fBagId').value = c.bagId;
  if (c.ynokcVersiyon) document.getElementById('fYnokcVersiyon').value = c.ynokcVersiyon;
  if (c.dynCashVersiyon) document.getElementById('fDynCashVersiyon').value = c.dynCashVersiyon;
  if (c.arkaOfisVersiyon) document.getElementById('fArkaOfisVersiyon').value = c.arkaOfisVersiyon;
  if (c.tckn) document.getElementById('fTckn').value = c.tckn;
  if (c.vergiNo) document.getElementById('fVergiNo').value = c.vergiNo;

  closeModal('candidatePickerModal');
  updateCustomerPreviousCallsCard();
  showToast(`✅ "${c.musteriBayi || c.sicilNo}" bilgileri forma aktarıldı.`, 'success');
}

function updateCustomerPreviousCallsCard() {
  const card = document.getElementById('customerPrevCallsCard');
  const title = document.getElementById('customerPrevCallsTitle');
  const body = document.getElementById('customerPrevCallsBody');
  if (!card || !title || !body) return;

  const sicil = getFullSicilFromForm();
  const phone10 = normalizePhoneLast10(document.getElementById('fTelefon')?.value || '');
  const musteri = (document.getElementById('fMusteriBayi')?.value || '').trim().toLocaleUpperCase('tr-TR');

  if (!sicil && phone10.length < 10 && musteri.length < 3) {
    card.style.display = 'none';
    return;
  }

  const prev = state.records.filter((r) => {
    if (state.editingRecordId && r.id === state.editingRecordId) return false;
    if (sicil && r.sicilNo && r.sicilNo.toUpperCase() === sicil.toUpperCase()) return true;
    if (phone10.length >= 10 && normalizePhoneLast10(r.telefon) === phone10) return true;
    if (musteri.length >= 4 && (r.musteriBayi || '').toLocaleUpperCase('tr-TR') === musteri) return true;
    return false;
  });

  if (prev.length === 0) {
    card.style.display = 'none';
    return;
  }

  card.style.display = 'block';
  title.textContent = `🕒 Bu Müşterinin Önceki Çağrıları (${prev.length} Kayıt)`;
  body.innerHTML = prev
    .slice(0, 5)
    .map(
      (r) => `
      <div style="padding:6px 8px; border-radius:8px; background:rgba(255,255,255,0.03); border:1px solid var(--border-subtle); margin-bottom:5px; font-size:11px;">
        <div style="display:flex; justify-content:space-between; font-weight:700; color:#93C5FD;">
          <span>📌 ${escapeHtml(r.destekKonu || 'Genel Destek')}</span>
          <span>🕒 ${escapeHtml(r.basTarihi || '-')}</span>
        </div>
        <div style="color:var(--text-secondary); margin-top:2px;">
          🔍 <b>Tespit:</b> ${escapeHtml(r.tespit || '-')} | 🛠️ <b>İşlem:</b> ${escapeHtml(r.islem || '-')}
        </div>
      </div>
    `
    )
    .join('');
}

function toggleCustomerPrevCalls() {
  const body = document.getElementById('customerPrevCallsBody');
  const arrow = document.getElementById('customerPrevCallsArrow');
  if (!body) return;
  const open = body.style.display === 'none';
  body.style.display = open ? 'block' : 'none';
  if (arrow) arrow.textContent = open ? '▲' : '▼';
}

/* ============================================================================
 * 7. HIZLI ÇÖZÜM ŞABLONLARI & ÇOKLU SEÇİMLİ TESPİT / İŞLEM ÇİPLERİ
 * ============================================================================ */
function renderQuickScenariosRow() {
  const row = document.getElementById('quickScenariosRow');
  if (!row) return;
  row.innerHTML = state.scenarios
    .map(
      (sc) => `
      <button type="button" class="chip" onclick="applyQuickScenario('${escapeHtml(sc.id)}')" title="${escapeHtml(sc.konu)}">
        <span>${escapeHtml(sc.icon || '⚡')}</span>
        <span>${escapeHtml(sc.title)}</span>
      </button>
    `
    )
    .join('');
}

function applyQuickScenario(scId) {
  const sc = state.scenarios.find((s) => s.id === scId);
  if (!sc) return;
  if (sc.konu) document.getElementById('fDestekKonu').value = sc.konu.toLocaleUpperCase('tr-TR');
  if (sc.tespit) document.getElementById('fTespit').value = sc.tespit.toLocaleUpperCase('tr-TR');
  if (sc.islem) document.getElementById('fIslem').value = sc.islem.toLocaleUpperCase('tr-TR');
  renderTemplatesAndPopularChips();
  showToast(`⚡ "${sc.title}" şablonu uygulandı.`, 'success');
}

function openScenarioModal() {
  document.getElementById('scIcon').value = '⚡';
  document.getElementById('scTitle').value = '';
  document.getElementById('scKonu').value = document.getElementById('fDestekKonu')?.value || '';
  document.getElementById('scTespit').value = document.getElementById('fTespit')?.value || '';
  document.getElementById('scIslem').value = document.getElementById('fIslem')?.value || '';
  openModal('scenarioModal');
}

function saveNewQuickScenario() {
  const icon = document.getElementById('scIcon').value.trim() || '⚡';
  const title = document.getElementById('scTitle').value.trim();
  const konu = document.getElementById('scKonu').value.trim().toLocaleUpperCase('tr-TR');
  const tespit = document.getElementById('scTespit').value.trim().toLocaleUpperCase('tr-TR');
  const islem = document.getElementById('scIslem').value.trim().toLocaleUpperCase('tr-TR');

  if (!title) {
    showToast('Lütfen şablon için kısa bir başlık girin.', 'error');
    return;
  }

  state.scenarios.unshift({
    id: 'sc_' + Date.now(),
    icon,
    title,
    konu,
    tespit,
    islem
  });
  saveToStorage(STORAGE_KEYS.SCENARIOS, state.scenarios);
  renderQuickScenariosRow();
  closeModal('scenarioModal');
  showToast(`✅ "${title}" hızlı şablonlara eklendi.`, 'success');
}

function renderTemplatesAndPopularChips() {
  // 5. Destek Konusu Datalist + Yatay Çipler
  const datalist = document.getElementById('konuDatalist');
  const konuChips = document.getElementById('popularKonuChips');
  const currentKonu = (document.getElementById('fDestekKonu')?.value || '').trim().toLocaleUpperCase('tr-TR');

  if (datalist) {
    datalist.innerHTML = state.templates.KONU.map((k) => `<option value="${escapeHtml(k)}"></option>`).join('');
  }
  if (konuChips) {
    konuChips.innerHTML = state.templates.KONU.map((k) => {
      const active = currentKonu === k.toLocaleUpperCase('tr-TR');
      return `<button type="button" class="chip ${active ? 'active' : ''}" onclick="selectKonuChip('${escapeJs(k)}')">${active ? '✓ ' : ''}${escapeHtml(k)}</button>`;
    }).join('');
  }

  // 6. Tespit Çoklu Seçim Çipleri (virgülle ayrılmış)
  const tespitEl = document.getElementById('tespitTemplateChips');
  const currentTespitParts = (document.getElementById('fTespit')?.value || '')
    .split(',')
    .map((s) => s.trim().toLocaleUpperCase('tr-TR'))
    .filter(Boolean);

  if (tespitEl) {
    tespitEl.innerHTML = state.templates.TESPIT.map((t) => {
      const upper = t.toLocaleUpperCase('tr-TR');
      const selected = currentTespitParts.includes(upper);
      return `<button type="button" class="chip ${selected ? 'selected-orange' : ''}" onclick="toggleMultiChipField('fTespit', '${escapeJs(upper)}')">${selected ? '✓ ' : '+ '}${escapeHtml(t)}</button>`;
    }).join('');
  }

  // 7. İşlem Çoklu Seçim Çipleri
  const islemEl = document.getElementById('islemTemplateChips');
  const currentIslemParts = (document.getElementById('fIslem')?.value || '')
    .split(',')
    .map((s) => s.trim().toLocaleUpperCase('tr-TR'))
    .filter(Boolean);

  if (islemEl) {
    islemEl.innerHTML = state.templates.ISLEM.map((t) => {
      const upper = t.toLocaleUpperCase('tr-TR');
      const selected = currentIslemParts.includes(upper);
      return `<button type="button" class="chip ${selected ? 'selected-green' : ''}" onclick="toggleMultiChipField('fIslem', '${escapeJs(upper)}')">${selected ? '✓ ' : '+ '}${escapeHtml(t)}</button>`;
    }).join('');
  }
}

function selectKonuChip(konuText) {
  const input = document.getElementById('fDestekKonu');
  if (!input) return;
  const upper = konuText.toLocaleUpperCase('tr-TR');
  input.value = input.value.trim().toLocaleUpperCase('tr-TR') === upper ? '' : upper;
  renderTemplatesAndPopularChips();
}

function toggleMultiChipField(fieldId, itemText) {
  const input = document.getElementById(fieldId);
  if (!input) return;
  const target = itemText.trim().toLocaleUpperCase('tr-TR');
  let parts = input.value
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean);

  const existsIdx = parts.findIndex((p) => p.toLocaleUpperCase('tr-TR') === target);
  if (existsIdx >= 0) {
    parts.splice(existsIdx, 1);
  } else {
    parts.push(target);
  }
  input.value = parts.join(', ');
  renderTemplatesAndPopularChips();
}

function promptAddTemplate(type) {
  const labelMap = { KONU: 'Destek Konusu', TESPIT: 'Tespit Şablonu', ISLEM: 'İşlem Şablonu' };
  const val = prompt(`Yeni ${labelMap[type] || 'Şablon'} metnini girin:`);
  if (!val || !val.trim()) return;
  const upper = val.trim().toLocaleUpperCase('tr-TR');
  if (!state.templates[type].includes(upper)) {
    state.templates[type].unshift(upper);
    saveToStorage(STORAGE_KEYS.TEMPLATES, state.templates);
    renderTemplatesAndPopularChips();
    showToast(`✅ Yeni ${labelMap[type]} eklendi.`, 'success');
  }
}

/* ============================================================================
 * 8. SESLİ DİKTE (WEB SPEECH API) & KARALAMA DEFTERİ (SCRATCHPAD)
 * ============================================================================ */
function startVoiceInput(targetInputId) {
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
  if (!SpeechRecognition) {
    showToast('Cihaz tarayıcınız sesli dikte (Web Speech API) desteklemiyor.', 'error');
    return;
  }
  try {
    const rec = new SpeechRecognition();
    rec.lang = 'tr-TR';
    rec.interimResults = false;
    rec.maxAlternatives = 1;
    showToast('🎙️ Dinleniyor... Lütfen konuşun.', 'info');
    rec.onresult = (ev) => {
      const spoken = ev.results?.[0]?.[0]?.transcript || '';
      if (spoken) {
        const input = document.getElementById(targetInputId);
        if (input) {
          const upper = spoken.trim().toLocaleUpperCase('tr-TR');
          input.value = input.value.trim() ? `${input.value.trim()}, ${upper}` : upper;
          renderTemplatesAndPopularChips();
          showToast('🎙️ Sesli metin eklendi.', 'success');
        }
      }
    };
    rec.onerror = () => showToast('Ses algılanamadı veya mikrofon izni verilmedi.', 'error');
    rec.start();
  } catch (_) {
    showToast('Sesli dikte başlatılamadı.', 'error');
  }
}

function renderScratchpadUI() {
  const input = document.getElementById('scratchpadInput');
  const title = document.getElementById('scratchpadHeaderTitle');
  if (input && input.value !== state.scratchpad) {
    input.value = state.scratchpad || '';
  }
  if (title) {
    title.textContent = state.scratchpad
      ? `📝 Görüşme İçi Hızlı Not (Dolu • ${state.scratchpad.length} krk)`
      : '📝 Görüşme İçi Hızlı Not (Karalama Defteri)';
  }
}

function toggleScratchpad() {
  const body = document.getElementById('scratchpadBody');
  const arrow = document.getElementById('scratchpadArrow');
  if (!body) return;
  const isHidden = body.style.display === 'none';
  body.style.display = isHidden ? 'block' : 'none';
  if (arrow) arrow.textContent = isHidden ? '▲' : '▼';
}

function onScratchpadInput(val) {
  state.scratchpad = val;
  saveToStorage(STORAGE_KEYS.SCRATCHPAD, val);
  renderScratchpadUI();
}

function appendScratchpadToField(fieldId) {
  if (!state.scratchpad.trim()) {
    showToast('Karalama defterinde aktarılacak not yok.', 'info');
    return;
  }
  const input = document.getElementById(fieldId);
  if (!input) return;
  const upper = state.scratchpad.trim().toLocaleUpperCase('tr-TR');
  input.value = input.value.trim() ? `${input.value.trim()} - ${upper}` : upper;
  renderTemplatesAndPopularChips();
  showToast('✅ Not forma aktarıldı.', 'success');
}

function clearScratchpad() {
  state.scratchpad = '';
  saveToStorage(STORAGE_KEYS.SCRATCHPAD, '');
  renderScratchpadUI();
}

/* ============================================================================
 * 9. KRONOMETRE (STOPWATCH) & GELİŞMİŞ ALAN YARDIMCILARI
 * ============================================================================ */
function startCallTimer() {
  if (state.timerInterval) clearInterval(state.timerInterval);
  state.timerRunning = true;
  if (!document.getElementById('fBasTarihi')?.value) {
    document.getElementById('fBasTarihi').value = formatDateTimeTR(new Date());
  }
  state.timerInterval = setInterval(() => {
    state.timerSeconds++;
    updateTimerUI();
  }, 1000);
  updateTimerUI();
}

function stopCallTimer() {
  if (state.timerInterval) {
    clearInterval(state.timerInterval);
    state.timerInterval = null;
  }
  state.timerRunning = false;
  const formatted = formatSecondsMMSS(state.timerSeconds);
  const sureEl = document.getElementById('fSure');
  if (sureEl) sureEl.value = formatted;
  const bitEl = document.getElementById('fBitTarihi');
  if (bitEl) bitEl.value = formatDateTimeTR(new Date());
  updateTimerUI();
}

function resetTimer() {
  if (state.timerInterval) {
    clearInterval(state.timerInterval);
    state.timerInterval = null;
  }
  state.timerRunning = false;
  state.timerSeconds = 0;
  const sureEl = document.getElementById('fSure');
  if (sureEl) sureEl.value = '00:00';
  updateTimerUI();
}

function startCallAndClearForm() {
  clearSupportForm(false);
  resetTimer();
  startCallTimer();
  showToast('▶️ Yeni çağrı başlatıldı, kronometre çalışıyor.', 'success');
}

function updateTimerUI() {
  const box = document.getElementById('timerBox');
  const disp = document.getElementById('timerDisplay');
  const badge = document.getElementById('timerStateBadge');
  const btnStart = document.getElementById('btnTimerStartNew');
  const btnStop = document.getElementById('btnTimerStop');
  const topLive = document.getElementById('liveCallTopBadge');

  const formatted = formatSecondsMMSS(state.timerSeconds);
  if (disp) disp.textContent = formatted;
  if (box) box.classList.toggle('running', state.timerRunning);
  if (badge) badge.textContent = state.timerRunning ? 'GÖRÜŞMEDE' : 'HAZIR';
  if (btnStart) btnStart.style.display = state.timerRunning ? 'none' : 'inline-flex';
  if (btnStop) btnStop.style.display = state.timerRunning ? 'inline-flex' : 'none';
  if (topLive) topLive.style.display = state.timerRunning ? 'inline-flex' : 'none';
}

function setCommType(commType) {
  document.getElementById('fIletisimTuru').value = commType;
  const row = document.getElementById('commTypeChipsRow');
  if (!row) return;
  row.querySelectorAll('.chip').forEach((btn) => {
    const isMatch = btn.getAttribute('data-comm') === commType;
    btn.classList.toggle('selected-green', isMatch);
  });
}

function setBekleyenValue(val) {
  document.getElementById('fBekleyen').value = val;
  const btnHayir = document.getElementById('segBekleyenHayir');
  const btnEvet = document.getElementById('segBekleyenEvet');
  if (btnHayir && btnEvet) {
    btnHayir.className = `btn btn-sm ${val === 'Hayır' ? 'btn-green' : 'btn-outline'}`;
    btnEvet.className = `btn btn-sm ${val === 'Evet' ? 'btn-orange' : 'btn-outline'}`;
  }
}

function setStartNow() {
  document.getElementById('fBasTarihi').value = formatDateTimeTR(new Date());
}

function setEndNowAndCalc() {
  const nowStr = formatDateTimeTR(new Date());
  document.getElementById('fBitTarihi').value = nowStr;
  const startMs = parseRecordDateMs(document.getElementById('fBasTarihi').value);
  const endMs = parseRecordDateMs(nowStr);
  if (startMs > 0 && endMs >= startMs) {
    const diffSec = Math.round((endMs - startMs) / 1000);
    document.getElementById('fSure').value = formatSecondsMMSS(diffSec);
  }
}

function initDefaultFormTimestamps() {
  const nowStr = formatDateTimeTR(new Date());
  if (document.getElementById('fBasTarihi')) document.getElementById('fBasTarihi').value = nowStr;
  if (document.getElementById('fBitTarihi')) document.getElementById('fBitTarihi').value = nowStr;
  if (document.getElementById('fSure')) document.getElementById('fSure').value = '00:00';
  if (document.getElementById('fKullaniciKodu')) document.getElementById('fKullaniciKodu').value = state.settings.globalKod || '';
  if (document.getElementById('fKullanici')) document.getElementById('fKullanici').value = state.settings.globalAd || '';
}

/* ============================================================================
 * 10. FORM KAYDETME, DÜZENLEME VE TEMİZLEME (36 SÜTUN TAM UYUMLU)
 * ============================================================================ */
function isFormCurrentlyDirty() {
  const digits = (document.getElementById('fSicilDigits')?.value || '').trim();
  const musteri = (document.getElementById('fMusteriBayi')?.value || '').trim();
  const gorusulen = (document.getElementById('fGorusulen')?.value || '').trim();
  const tel = (document.getElementById('fTelefon')?.value || '').trim();
  const tespit = (document.getElementById('fTespit')?.value || '').trim();
  const islem = (document.getElementById('fIslem')?.value || '').trim();
  return Boolean(digits || musteri || gorusulen || tel || tespit || islem || state.editingRecordId);
}

function collectRecordFromForm() {
  padSicilDigitsOnBlur();
  const nowStr = formatDateTimeTR(new Date());
  const basTarihi = (document.getElementById('fBasTarihi')?.value || '').trim() || nowStr;
  const bitTarihi = (document.getElementById('fBitTarihi')?.value || '').trim() || nowStr;
  let sure = (document.getElementById('fSure')?.value || '').trim();
  if ((!sure || sure === '00:00') && state.timerSeconds > 0) {
    sure = formatSecondsMMSS(state.timerSeconds);
  }
  if (!sure) sure = '01:00';

  return {
    id: state.editingRecordId || Date.now(),
    iletisimTuru: document.getElementById('fIletisimTuru')?.value || 'Telefon',
    sicilNo: getFullSicilFromForm(),
    musteriBayi: (document.getElementById('fMusteriBayi')?.value || '').trim().toLocaleUpperCase('tr-TR'),
    gorusulen: (document.getElementById('fGorusulen')?.value || '').trim().toLocaleUpperCase('tr-TR'),
    basTarihi,
    bitTarihi,
    sure,
    kullaniciKodu: (document.getElementById('fKullaniciKodu')?.value || state.settings.globalKod || '').trim(),
    kullanici: (document.getElementById('fKullanici')?.value || state.settings.globalAd || '').trim().toLocaleUpperCase('tr-TR'),
    destekKonu: (document.getElementById('fDestekKonu')?.value || '').trim().toLocaleUpperCase('tr-TR'),
    tespit: (document.getElementById('fTespit')?.value || '').trim().toLocaleUpperCase('tr-TR'),
    islem: (document.getElementById('fIslem')?.value || '').trim().toLocaleUpperCase('tr-TR'),
    redmine: (document.getElementById('fRedmine')?.value || '').trim(),
    bagId: (document.getElementById('fBagId')?.value || '').trim(),
    telefon: (document.getElementById('fTelefon')?.value || '').trim(),
    telefon2: (document.getElementById('fTelefon2')?.value || '').trim(),
    mail: (document.getElementById('fMail')?.value || '').trim(),
    bekleyen: document.getElementById('fBekleyen')?.value || 'Hayır',
    argeGidis: document.getElementById('fArgeGidis')?.value || 'Hayır',
    argeGidisNotu: (document.getElementById('fArgeGidisNotu')?.value || '').trim(),
    argeDonus: document.getElementById('fArgeDonus')?.value || 'Hayır',
    argeDonusNotu: (document.getElementById('fArgeDonusNotu')?.value || '').trim(),
    b2bKayit: (document.getElementById('fB2bKayit')?.value || '').trim(),
    arayan: document.getElementById('fArayan')?.value || 'Müşteri',
    ynokcVersiyon: (document.getElementById('fYnokcVersiyon')?.value || '').trim(),
    dynCashVersiyon: (document.getElementById('fDynCashVersiyon')?.value || '').trim(),
    yn500Versiyon: (document.getElementById('fYn500Versiyon')?.value || '').trim(),
    arkaOfisVersiyon: (document.getElementById('fArkaOfisVersiyon')?.value || '').trim(),
    servisKodu: (document.getElementById('fServisKodu')?.value || '').trim(),
    yn500SicilNo: (document.getElementById('fYn500SicilNo')?.value || '').trim(),
    arkaOfisId: (document.getElementById('fArkaOfisId')?.value || '').trim(),
    tckn: (document.getElementById('fTckn')?.value || '').trim(),
    vergiNo: (document.getElementById('fVergiNo')?.value || '').trim(),
    tespitKodu: (document.getElementById('fTespitKodu')?.value || '').trim(),
    islemKodu: (document.getElementById('fIslemKodu')?.value || '').trim(),
    isSyncedToSql: false
  };
}

function handleSaveRecord(ev) {
  if (ev) ev.preventDefault();
  if (state.timerRunning) stopCallTimer();

  const rec = collectRecordFromForm();
  if (!rec.sicilNo && !rec.musteriBayi && !rec.telefon && !rec.tespit && !rec.islem) {
    showToast('⚠️ Lütfen kaydetmeden önce Sicil No, Müşteri, Telefon veya Tespit/İşlem alanlarından en az birini doldurun.', 'error');
    return;
  }

  // Yeni girilen Konu/Tespit/İşlem şablonlarda yoksa otomatik öğren
  if (rec.destekKonu && !state.templates.KONU.includes(rec.destekKonu)) {
    state.templates.KONU.unshift(rec.destekKonu);
  }
  saveToStorage(STORAGE_KEYS.TEMPLATES, state.templates);

  if (state.editingRecordId) {
    const idx = state.records.findIndex((r) => r.id === state.editingRecordId);
    if (idx >= 0) {
      state.records[idx] = rec;
    } else {
      state.records.unshift(rec);
    }
    showToast(`✅ Kayıt (#${rec.id}) güncellendi ve yerel hafızaya kaydedildi.`, 'success');
  } else {
    state.records.unshift(rec);
    showToast('✅ Destek kaydı yerel hafızaya (Offline) güvenle kaydedildi!', 'success');
  }

  // Eğer bu telefon numarası kaçan çağrılarda varsa otomatik "İşlendi" yap
  if (rec.telefon) {
    const p10 = normalizePhoneLast10(rec.telefon);
    if (p10.length >= 7) {
      state.missedCalls.forEach((m) => {
        if (normalizePhoneLast10(m.number) === p10) m.isHandled = true;
      });
      saveToStorage(STORAGE_KEYS.MISSED, state.missedCalls);
    }
  }

  saveToStorage(STORAGE_KEYS.RECORDS, state.records);
  clearSupportForm(false);
  resetTimer();
  renderAllUI();

  // Eğer kuyrukta bekleyen çağrı varsa kullanıcıya bildir
  if (state.waitingQueue.length > 0) {
    showToast(`⏳ Kuyrukta bekleyen ${state.waitingQueue.length} çağrı var. "▶️ Al" butonuyla sıradakine geçebilirsiniz.`, 'info');
  }
}

function clearSupportForm(showNotification = true) {
  state.editingRecordId = null;
  const editBanner = document.getElementById('editingRecordBanner');
  if (editBanner) editBanner.classList.remove('visible');

  const btnSubmit = document.getElementById('btnSubmitRecord');
  if (btnSubmit) btnSubmit.innerHTML = '💾 KAYDET & BİTİR';

  const idsToClear = [
    'fSicilDigits', 'fMusteriBayi', 'fGorusulen', 'fTelefon', 'fDestekKonu',
    'fTespit', 'fIslem', 'fTelefon2', 'fMail', 'fRedmine', 'fBagId',
    'fB2bKayit', 'fArgeGidisNotu', 'fArgeDonusNotu', 'fYnokcVersiyon',
    'fDynCashVersiyon', 'fYn500Versiyon', 'fArkaOfisVersiyon', 'fServisKodu',
    'fYn500SicilNo', 'fArkaOfisId', 'fTckn', 'fVergiNo', 'fTespitKodu', 'fIslemKodu'
  ];
  idsToClear.forEach((id) => {
    const el = document.getElementById(id);
    if (el) el.value = '';
  });

  setCommType('Telefon');
  setBekleyenValue('Hayır');
  if (document.getElementById('fArayan')) document.getElementById('fArayan').value = 'Müşteri';
  if (document.getElementById('fArgeGidis')) document.getElementById('fArgeGidis').value = 'Hayır';
  if (document.getElementById('fArgeDonus')) document.getElementById('fArgeDonus').value = 'Hayır';

  initDefaultFormTimestamps();
  updateCombinedSicilPreview();
  updateCustomerPreviousCallsCard();
  renderTemplatesAndPopularChips();
  switchFormPage(0);

  if (showNotification) {
    showToast('🧹 Form temizlendi, yeni çağrıya hazır.', 'info');
  }
}

function loadRecordIntoFormForEdit(recordId) {
  const r = state.records.find((item) => item.id === recordId);
  if (!r) return;

  state.editingRecordId = r.id;
  setSicilIntoForm(r.sicilNo || '');
  document.getElementById('fMusteriBayi').value = r.musteriBayi || '';
  document.getElementById('fGorusulen').value = r.gorusulen || '';
  document.getElementById('fTelefon').value = r.telefon || '';
  document.getElementById('fDestekKonu').value = r.destekKonu || '';
  document.getElementById('fTespit').value = r.tespit || '';
  document.getElementById('fIslem').value = r.islem || '';
  document.getElementById('fTelefon2').value = r.telefon2 || '';
  document.getElementById('fMail').value = r.mail || '';
  document.getElementById('fRedmine').value = r.redmine || '';
  document.getElementById('fBagId').value = r.bagId || '';
  document.getElementById('fB2bKayit').value = r.b2bKayit || '';
  document.getElementById('fBasTarihi').value = r.basTarihi || '';
  document.getElementById('fBitTarihi').value = r.bitTarihi || '';
  document.getElementById('fSure').value = r.sure || '00:00';
  document.getElementById('fArgeGidis').value = r.argeGidis || 'Hayır';
  document.getElementById('fArgeGidisNotu').value = r.argeGidisNotu || '';
  document.getElementById('fArgeDonus').value = r.argeDonus || 'Hayır';
  document.getElementById('fArgeDonusNotu').value = r.argeDonusNotu || '';
  document.getElementById('fArayan').value = r.arayan || 'Müşteri';
  document.getElementById('fYnokcVersiyon').value = r.ynokcVersiyon || '';
  document.getElementById('fDynCashVersiyon').value = r.dynCashVersiyon || '';
  document.getElementById('fYn500Versiyon').value = r.yn500Versiyon || '';
  document.getElementById('fArkaOfisVersiyon').value = r.arkaOfisVersiyon || '';
  document.getElementById('fServisKodu').value = r.servisKodu || '';
  document.getElementById('fYn500SicilNo').value = r.yn500SicilNo || '';
  document.getElementById('fArkaOfisId').value = r.arkaOfisId || '';
  document.getElementById('fTckn').value = r.tckn || '';
  document.getElementById('fVergiNo').value = r.vergiNo || '';
  document.getElementById('fTespitKodu').value = r.tespitKodu || '';
  document.getElementById('fIslemKodu').value = r.islemKodu || '';
  document.getElementById('fKullaniciKodu').value = r.kullaniciKodu || state.settings.globalKod || '';
  document.getElementById('fKullanici').value = r.kullanici || state.settings.globalAd || '';

  setCommType(r.iletisimTuru || 'Telefon');
  setBekleyenValue(r.bekleyen || 'Hayır');

  const editBanner = document.getElementById('editingRecordBanner');
  const editText = document.getElementById('editingRecordBannerText');
  if (editBanner && editText) {
    editBanner.classList.add('visible');
    editText.textContent = `✏️ Kayıt Düzenleme Modu: ${r.musteriBayi || r.sicilNo || '#' + r.id}`;
  }
  const btnSubmit = document.getElementById('btnSubmitRecord');
  if (btnSubmit) btnSubmit.innerHTML = '💾 GÜNCELLEMEYİ KAYDET';

  renderTemplatesAndPopularChips();
  updateCustomerPreviousCallsCard();
  switchTab(0);
  switchFormPage(0);
  showToast('✏️ Kayıt düzenleme modunda forma yüklendi.', 'info');
}

/* ============================================================================
 * 11. OFİS İÇİ Wİ-Fİ SYNC MODÜLÜ (http://172.16.160.21:8080/api/sync-calls)
 * ============================================================================ */
function getOfficeApiBaseUrl() {
  const host = (state.settings.sqlHost || '172.16.160.21').trim();
  const port = (state.settings.sqlPort || '8080').trim();
  if (!host) return '';
  if (host.startsWith('http://') || host.startsWith('https://')) {
    return port && !host.includes(':' + port) ? `${host}:${port}` : host;
  }
  return port ? `http://${host}:${port}` : `http://${host}`;
}

function openWifiSyncModal() {
  updateWifiSyncModalStats();
  const todayIso = formatDateInputISO(new Date());
  if (document.getElementById('sqlFetchStart') && !document.getElementById('sqlFetchStart').value) {
    document.getElementById('sqlFetchStart').value = todayIso;
  }
  if (document.getElementById('sqlFetchEnd') && !document.getElementById('sqlFetchEnd').value) {
    document.getElementById('sqlFetchEnd').value = todayIso;
  }
  const s = state.settings;
  if (document.getElementById('modalSqlHost')) document.getElementById('modalSqlHost').value = s.sqlHost || '172.16.160.21';
  if (document.getElementById('modalSqlPort')) document.getElementById('modalSqlPort').value = s.sqlPort || '8080';
  if (document.getElementById('modalSqlType')) document.getElementById('modalSqlType').value = s.sqlType || 'MSSQL';
  if (document.getElementById('modalSqlDb')) document.getElementById('modalSqlDb').value = s.sqlDb || 'destek_db';
  if (document.getElementById('modalSqlUser')) document.getElementById('modalSqlUser').value = s.sqlUser || '';
  if (document.getElementById('modalSqlPass')) document.getElementById('modalSqlPass').value = s.sqlPass || '';
  updateConnectionStatusUI();
  openModal('wifiSyncModal');
}

function updateWifiSyncModalStats() {
  const unsynced = state.records.filter((r) => !r.isSyncedToSql).length;
  const synced = state.records.filter((r) => r.isSyncedToSql).length;
  const total = state.records.length;
  const hostPort = `${state.settings.sqlHost || '172.16.160.21'}:${state.settings.sqlPort || '8080'}`;

  const elUnsynced = document.getElementById('syncModalUnsyncedCount');
  const elSynced = document.getElementById('syncModalSyncedCount');
  const elTotal = document.getElementById('syncModalTotalCount');
  const elTarget = document.getElementById('syncModalTargetUrl');
  const elPreview = document.getElementById('syncEndpointPreview');

  if (elUnsynced) elUnsynced.textContent = String(unsynced);
  if (elSynced) elSynced.textContent = String(synced);
  if (elTotal) elTotal.textContent = String(total);
  if (elTarget) elTarget.textContent = hostPort;
  if (elPreview) elPreview.textContent = hostPort;
  updateConnectionStatusUI();
}

function saveModalSqlHostPort(notify = true) {
  const host = (document.getElementById('modalSqlHost')?.value || '172.16.160.21').trim();
  const port = (document.getElementById('modalSqlPort')?.value || '8080').trim();
  const type = document.getElementById('modalSqlType')?.value || state.settings.sqlType || 'MSSQL';
  const db = (document.getElementById('modalSqlDb')?.value || state.settings.sqlDb || 'destek_db').trim();
  const user = (document.getElementById('modalSqlUser')?.value ?? state.settings.sqlUser ?? '').trim();
  const pass = document.getElementById('modalSqlPass')?.value ?? state.settings.sqlPass ?? '';

  state.settings.sqlHost = host;
  state.settings.sqlPort = port;
  state.settings.sqlType = type;
  state.settings.sqlDb = db;
  state.settings.sqlUser = user;
  state.settings.sqlPass = pass;

  saveToStorage(STORAGE_KEYS.SETTINGS, state.settings);
  syncSettingsInputsFromState();
  updateWifiSyncModalStats();
  if (notify) {
    showToast(`💾 API Sunucu (${host}:${port}) ve SQL Kullanıcı/Şifre ayarları kalıcı kaydedildi.`, 'success');
  }
}

function mapRecordForSqlApi(r) {
  const kod = r.kullaniciKodu || '';
  const ad = r.kullanici || r.kullaniciAdi || '';
  const bas = r.basTarihi || r.baslangicTarihi || '';
  const bit = r.bitTarihi || r.bitisTarihi || '';
  const arayan = r.arayan || r.arayanTuru || 'Müşteri';
  const musteri = r.musteriBayi || r.musteriBayiAdi || '';
  const gorusulen = r.gorusulen || r.gorusulenKisi || '';
  const tel = r.telefon || '';
  const tel2 = r.telefon2 || '';
  const mail = r.mail || '';
  const iletisim = r.iletisimTuru || 'Telefon';
  const konu = r.destekKonu || r.destekKonusu1 || '';
  const sicil = r.sicilNo || r.sicilNo1 || '';
  const tespit = r.tespit || r.tespit1 || '';
  const islem = r.islem || r.islem1 || '';
  const bagId = r.bagId || r.baglantiId1 || '';
  const sure = r.sure || r.toplamSure || '00:00';
  const kasaVer = r.ynokcVersiyon || r.kasaVersiyon1 || '';
  const arkaVer = r.arkaOfisVersiyon || r.arkaOfisVersiyon1 || '';
  const redmine = r.redmine || r.redmineId1 || '';
  const b2b = r.b2bKayit || '';
  const bekleyen = r.bekleyen || r.bekleyenIs || 'Hayır';
  const argeG = r.argeGidis || 'Hayır';
  const argeD = r.argeDonus || 'Hayır';
  const argeGN = r.argeGidisNotu || '';
  const argeDN = r.argeDonusNotu || '';
  const tckn = r.tckn || '';
  const vergiNo = r.vergiNo || '';
  const tespitKodu = r.tespitKodu || r.tespitKodu1 || '';
  const islemKodu = r.islemKodu || r.islemKodu1 || '';
  const yn500Sicil = r.yn500SicilNo || r.yn500SicilNo1 || sicil;
  const dynVer = r.dynCashVersiyon || r.dynCashVersiyon1 || '';
  const arkaId = r.arkaOfisId || r.arkaOfisId1 || '';
  const yn500Ver = r.yn500Versiyon || r.yn500KasaVersiyon1 || '';
  const servisKodu = r.servisKodu || r.not1 || '';

  return {
    ...r,
    id: r.id || Date.now(),
    // SyncApiService.kt @Json UPPERCASE & snake_case alanları (100% birebir uyumlu)
    KULLANICIKODU: kod,
    KULLANICIADI: ad,
    kullanici_kodu: kod,
    kullanici_id: kod,
    kullanici_adi: ad,
    BASLANGICTARIHI: bas,
    ARAYANTURU: arayan,
    MUSTERIBAYIADI: musteri,
    GORUSULENKISI: gorusulen,
    TELEFON: tel,
    TELEFON2: tel2,
    MAIL: mail,
    ILETISIMTURU: iletisim,
    DESTEKKONUSU1: konu,
    SICILNO1: sicil,
    TESPIT1: tespit,
    ISLEM1: islem,
    BAGLANTIID1: bagId,
    BITISTARIHI: bit,
    TOPLAMSURE: sure,
    KASAVERSIYON1: kasaVer,
    ARKAOFISVERSIYON1: arkaVer,
    REDMINEID1: redmine,
    AKTIF: '1',
    B2BKAYIT: b2b,
    BEKLEYENIS: bekleyen,
    ARGEGIDIS: argeG,
    ARGEDONUS: argeD,
    ARGEGIDISNOTU: argeGN,
    ARGEDONUSNOTU: argeDN,
    RESIM: '',
    TCKN: tckn,
    VERGINO: vergiNo,
    TESPITKODU1: tespitKodu,
    ISLEMKODU1: islemKodu,
    YN500SICILNO1: yn500Sicil,
    DYNCASHVERSIYON1: dynVer,
    ARKAOFISID1: arkaId,
    YN500KASAVERSIYON1: yn500Ver,
    NOT1: servisKodu,
    created_at: r.id || Date.now(),
    // CamelCase karşılıkları
    kullaniciKodu: kod,
    kullaniciAdi: ad,
    baslangicTarihi: bas,
    arayanTuru: arayan,
    musteriBayiAdi: musteri,
    gorusulenKisi: gorusulen,
    telefon: tel,
    telefon2: tel2,
    mail: mail,
    iletisimTuru: iletisim,
    destekKonusu1: konu,
    sicilNo1: sicil,
    tespit1: tespit,
    islem1: islem,
    baglantiId1: bagId,
    bitisTarihi: bit,
    toplamSure: sure,
    kasaVersiyon1: kasaVer,
    arkaOfisVersiyon1: arkaVer,
    redmineId1: redmine,
    aktif: '1',
    b2bKayit: b2b,
    bekleyenIs: bekleyen,
    argeGidis: argeG,
    argeDonus: argeD,
    argeGidisNotu: argeGN,
    argeDonusNotu: argeDN,
    tckn,
    vergiNo,
    tespitKodu1: tespitKodu,
    islemKodu1: islemKodu,
    yn500SicilNo1: yn500Sicil,
    dynCashVersiyon1: dynVer,
    arkaOfisId1: arkaId,
    yn500KasaVersiyon1: yn500Ver,
    servisKodu,
    not1: servisKodu
  };
}

function normalizeIncomingRecord(rem) {
  return {
    id: rem.id || Date.now() + Math.floor(Math.random() * 100000),
    iletisimTuru: rem.iletisimTuru || rem.ILETISIMTURU || 'Telefon',
    sicilNo: rem.sicilNo || rem.sicilNo1 || rem.SICILNO1 || '',
    musteriBayi: rem.musteriBayi || rem.musteriBayiAdi || rem.MUSTERIBAYIADI || '',
    gorusulen: rem.gorusulen || rem.gorusulenKisi || rem.GORUSULENKISI || '',
    basTarihi: rem.basTarihi || rem.baslangicTarihi || rem.BASLANGICTARIHI || '',
    bitTarihi: rem.bitTarihi || rem.bitisTarihi || rem.BITISTARIHI || '',
    sure: rem.sure || rem.toplamSure || rem.TOPLAMSURE || '00:00',
    kullaniciKodu: rem.kullaniciKodu || rem.KULLANICIKODU || rem.kullanici_kodu || rem.kullanici_id || '',
    kullanici: rem.kullanici || rem.kullaniciAdi || rem.KULLANICIADI || rem.kullanici_adi || '',
    destekKonu: rem.destekKonu || rem.destekKonusu1 || rem.DESTEKKONUSU1 || '',
    tespit: rem.tespit || rem.tespit1 || rem.TESPIT1 || '',
    islem: rem.islem || rem.islem1 || rem.ISLEM1 || '',
    redmine: rem.redmine || rem.redmineId1 || rem.REDMINEID1 || '',
    bagId: rem.bagId || rem.baglantiId1 || rem.BAGLANTIID1 || '',
    telefon: rem.telefon || rem.TELEFON || '',
    telefon2: rem.telefon2 || rem.TELEFON2 || '',
    mail: rem.mail || rem.MAIL || '',
    bekleyen: rem.bekleyen || rem.bekleyenIs || rem.BEKLEYENIS || 'Hayır',
    argeGidis: rem.argeGidis || rem.ARGEGIDIS || 'Hayır',
    argeGidisNotu: rem.argeGidisNotu || rem.ARGEGIDISNOTU || '',
    argeDonus: rem.argeDonus || rem.ARGEDONUS || 'Hayır',
    argeDonusNotu: rem.argeDonusNotu || rem.ARGEDONUSNOTU || '',
    b2bKayit: rem.b2bKayit || rem.B2BKAYIT || '',
    arayan: rem.arayan || rem.arayanTuru || rem.ARAYANTURU || 'Müşteri',
    ynokcVersiyon: rem.ynokcVersiyon || rem.kasaVersiyon1 || rem.KASAVERSIYON1 || '',
    dynCashVersiyon: rem.dynCashVersiyon || rem.dynCashVersiyon1 || rem.DYNCASHVERSIYON1 || '',
    yn500Versiyon: rem.yn500Versiyon || rem.yn500KasaVersiyon1 || rem.YN500KASAVERSIYON1 || '',
    arkaOfisVersiyon: rem.arkaOfisVersiyon || rem.arkaOfisVersiyon1 || rem.ARKAOFISVERSIYON1 || '',
    servisKodu: rem.servisKodu || rem.SERVISKODU || rem.not1 || rem.NOT1 || '',
    yn500SicilNo: rem.yn500SicilNo || rem.yn500SicilNo1 || rem.YN500SICILNO1 || '',
    arkaOfisId: rem.arkaOfisId || rem.arkaOfisId1 || rem.ARKAOFISID1 || '',
    tckn: rem.tckn || rem.TCKN || '',
    vergiNo: rem.vergiNo || rem.VERGINO || '',
    tespitKodu: rem.tespitKodu || rem.tespitKodu1 || rem.TESPITKODU1 || '',
    islemKodu: rem.islemKodu || rem.islemKodu1 || rem.ISLEMKODU1 || '',
    isSyncedToSql: rem.isSyncedToSql !== undefined ? Boolean(rem.isSyncedToSql) : Boolean(rem.isSynced)
  };
}

async function syncOfflineRecordsToOfficeApi() {
  const unsyncedList = state.records.filter((r) => !r.isSyncedToSql);
  if (unsyncedList.length === 0) {
    showToast('✅ Gönderilmeyi bekleyen offline kayıt bulunmuyor. Tüm kayıtlar eşitlenmiş!', 'info');
    updateWifiSyncModalStats();
    return;
  }

  const baseUrl = getOfficeApiBaseUrl();
  const endpoint = `${baseUrl}/api/sync-calls`;
  const statusBox = document.getElementById('wifiSyncStatusBox');
  const btn = document.getElementById('btnRunWifiSyncNow');

  if (statusBox) {
    statusBox.style.display = 'block';
    statusBox.textContent = `⏳ ${unsyncedList.length} adet offline kayıt ${endpoint} adresine gönderiliyor...`;
  }
  if (btn) btn.disabled = true;

  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 12000);

    const payload = {
      host: state.settings.sqlHost || '172.16.160.21',
      port: state.settings.sqlPort || '8080',
      dbName: state.settings.sqlDb || 'destek_db',
      db_name: state.settings.sqlDb || 'destek_db',
      dbType: state.settings.sqlType || 'MSSQL',
      db_type: state.settings.sqlType || 'MSSQL',
      user: state.settings.sqlUser || '',
      password: state.settings.sqlPass || '',
      records: unsyncedList.map(mapRecordForSqlApi)
    };

    const resp = await fetch(endpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
      signal: controller.signal
    });
    clearTimeout(timeoutId);

    if (!resp.ok) {
      throw new Error(`HTTP ${resp.status}`);
    }

    let resultData = {};
    try {
      resultData = await resp.json();
    } catch (_) {
      resultData = { success: true, syncedCount: unsyncedList.length };
    }

    if (resultData.success === false) {
      throw new Error(resultData.message || 'Sunucu kayıtları reddetti.');
    }

    // Başarılı aktarım sonrası bağlantı durumunu da bağlı yap
    state.connectionStatus = 'CONNECTED';

    // Başarıyla gönderilen kayıtları isSyncedToSql = true olarak işaretle
    const unsyncedIds = new Set(unsyncedList.map((r) => r.id));
    state.records.forEach((r) => {
      if (unsyncedIds.has(r.id)) r.isSyncedToSql = true;
    });
    saveToStorage(STORAGE_KEYS.RECORDS, state.records);
    renderAllUI();
    updateWifiSyncModalStats();

    const msg = `✅ ${resultData.syncedCount || unsyncedList.length} adet offline kayıt başarıyla ${endpoint} adresine aktarıldı!`;
    if (statusBox) statusBox.textContent = msg;
    showToast(msg, 'success');
  } catch (err) {
    state.connectionStatus = 'ERROR';
    updateConnectionStatusUI();
    const errMsg = `⚠️ Ofis sunucusuna (${endpoint}) ulaşılamadı. Kayıtlarınız yerel hafızada güvende, ofis Wi-Fi ağına bağlandığınızda tekrar deneyin.`;
    if (statusBox) statusBox.textContent = errMsg;
    showToast(errMsg, 'error');
  } finally {
    if (btn) btn.disabled = false;
  }
}

function setSqlFetchPreset(preset, btnEl) {
  const parent = btnEl?.parentElement;
  if (parent) {
    parent.querySelectorAll('.chip').forEach((c) => c.classList.remove('active'));
    btnEl.classList.add('active');
  }

  const now = new Date();
  let start = new Date();
  if (preset === 'TODAY') {
    start = new Date();
  } else if (preset === 'YESTERDAY_TODAY') {
    start.setDate(now.getDate() - 1);
  } else if (preset === 'THIS_WEEK') {
    start.setDate(now.getDate() - 7);
  } else if (preset === 'THIS_MONTH') {
    start = new Date(now.getFullYear(), now.getMonth(), 1);
  } else if (preset === 'ALL') {
    start = new Date(2020, 0, 1);
  }

  const sInput = document.getElementById('sqlFetchStart');
  const eInput = document.getElementById('sqlFetchEnd');
  if (sInput) sInput.value = formatDateInputISO(start);
  if (eInput) eInput.value = formatDateInputISO(now);
}

async function fetchCallsFromOfficeSql() {
  const baseUrl = getOfficeApiBaseUrl();
  const endpoint = `${baseUrl}/api/fetch-calls`;
  const startIso = document.getElementById('sqlFetchStart')?.value || '';
  const endIso = document.getElementById('sqlFetchEnd')?.value || '';
  const limit = parseInt(document.getElementById('sqlFetchLimit')?.value || '1000', 10);

  const fmtTR = (iso) => {
    if (!iso) return '';
    const p = iso.split('-');
    return p.length === 3 ? `${p[2]}.${p[1]}.${p[0]}` : iso;
  };

  showToast(`📥 ${endpoint} adresinden kayıtlar çekiliyor...`, 'info');
  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 12000);
    const resp = await fetch(endpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        host: state.settings.sqlHost,
        port: state.settings.sqlPort,
        dbName: state.settings.sqlDb,
        db_name: state.settings.sqlDb,
        dbType: state.settings.sqlType,
        db_type: state.settings.sqlType,
        user: state.settings.sqlUser || '',
        password: state.settings.sqlPass || '',
        limit,
        startDate: fmtTR(startIso),
        endDate: fmtTR(endIso),
        start_date: startIso,
        end_date: endIso
      }),
      signal: controller.signal
    });
    clearTimeout(timeoutId);
    if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
    const data = await resp.json();
    const incomingList = Array.isArray(data?.records)
      ? data.records
      : Array.isArray(data?.data)
      ? data.data
      : Array.isArray(data)
      ? data
      : null;
    if (!incomingList) {
      throw new Error(data?.message || 'Geçersiz veri formatı');
    }

    state.connectionStatus = 'CONNECTED';
    let added = 0;
    const existingKeys = new Set(
      state.records.map((r) => `${r.sicilNo}|${r.basTarihi}|${r.musteriBayi}`)
    );

    incomingList.forEach((rawRem) => {
      const rem = normalizeIncomingRecord(rawRem);
      rem.isSyncedToSql = true;
      const key = `${rem.sicilNo || ''}|${rem.basTarihi || ''}|${rem.musteriBayi || ''}`;
      if (!existingKeys.has(key)) {
        existingKeys.add(key);
        state.records.push(rem);
        added++;
      }
    });

    state.records.sort((a, b) => parseRecordDateMs(b.basTarihi) - parseRecordDateMs(a.basTarihi));
    saveToStorage(STORAGE_KEYS.RECORDS, state.records);
    renderAllUI();
    updateWifiSyncModalStats();
    showToast(`✅ SQL Sunucudan ${incomingList.length} kayıt okundu, ${added} yeni kayıt eklendi.`, 'success');
  } catch (_) {
    state.connectionStatus = 'ERROR';
    updateConnectionStatusUI();
    showToast(`⚠️ SQL sunucusuna (${endpoint}) ulaşılamadı. Ofis Wi-Fi bağlantınızı kontrol edin.`, 'error');
  }
}

function renderSqlConnectionResultBoxes(isSuccess, message) {
  ['sqlConnectionResultBox', 'modalSqlConnResultBox'].forEach((boxId) => {
    const box = document.getElementById(boxId);
    if (!box) return;
    if (!message) {
      box.style.display = 'none';
      return;
    }
    box.style.display = 'block';
    box.style.background = isSuccess ? 'rgba(34, 197, 94, 0.16)' : 'rgba(239, 68, 68, 0.16)';
    box.style.border = isSuccess ? '1px solid rgba(34, 197, 94, 0.45)' : '1px solid rgba(239, 68, 68, 0.45)';
    box.style.color = isSuccess ? '#4ADE80' : '#F87171';
    box.textContent = message;
  });
}

async function testSqlConnection() {
  // Eğer ayarlar sekmesindeki inputlar doluysa önce onları kaydet
  if (document.getElementById('setSqlHost')) {
    saveSqlSettings(false);
  }
  const baseUrl = getOfficeApiBaseUrl();
  const endpoint = `${baseUrl}/api/test-db-connection`;

  state.connectionStatus = 'TESTING';
  updateConnectionStatusUI();
  renderSqlConnectionResultBoxes(false, `⏳ ${endpoint} adresine bağlantı testi yapılıyor...`);
  showToast(`🔍 ${endpoint} bağlantısı test ediliyor...`, 'info');

  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 6000);
    const requestPayload = {
      db_type: state.settings.sqlType || 'MSSQL',
      dbType: state.settings.sqlType || 'MSSQL',
      host: state.settings.sqlHost || '172.16.160.21',
      port: state.settings.sqlPort || '8080',
      db_name: state.settings.sqlDb || 'destek_db',
      dbName: state.settings.sqlDb || 'destek_db',
      user: state.settings.sqlUser || '',
      username: state.settings.sqlUser || '',
      password: state.settings.sqlPass || ''
    };

    const resp = await fetch(endpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(requestPayload),
      signal: controller.signal
    });
    clearTimeout(timeoutId);

    let data = null;
    try {
      data = await resp.json();
    } catch (_) {}

    if (resp.ok && (!data || data.success !== false)) {
      state.connectionStatus = 'CONNECTED';
      updateConnectionStatusUI();
      const okMsg = data?.message
        ? `✓ Bağlantı Başarılı: ${data.message}`
        : `✓ Ofis API & SQL Sunucusu (${baseUrl}) bağlantısı doğrulandı!`;
      renderSqlConnectionResultBoxes(true, okMsg);
      showToast(okMsg, 'success');
      return true;
    } else {
      state.connectionStatus = 'ERROR';
      updateConnectionStatusUI();
      const failMsg = data?.message
        ? `✗ Bağlantı Hatası: ${data.message}`
        : `✗ Sunucu yanıt verdi ancak bağlantı doğrulanamadı (HTTP ${resp.status}).`;
      renderSqlConnectionResultBoxes(false, failMsg);
      showToast(failMsg, 'error');
      return false;
    }
  } catch (_) {
    state.connectionStatus = 'ERROR';
    updateConnectionStatusUI();
    const offlineMsg = `✗ Sunucuya (${endpoint}) ulaşılamadı. Şu an Çevrimdışı (Offline) moddasınız; tüm kayıtlar yerel hafızada güvenle saklanmaktadır.`;
    renderSqlConnectionResultBoxes(false, offlineMsg);
    showToast(offlineMsg, 'error');
    return false;
  }
}

function markAllLocalAsSynced() {
  state.records.forEach((r) => (r.isSyncedToSql = true));
  saveToStorage(STORAGE_KEYS.RECORDS, state.records);
  renderAllUI();
  updateWifiSyncModalStats();
  showToast('✅ Tüm yerel kayıtlar "SQL Eşitlendi" olarak işaretlendi.', 'success');
}

function markAllLocalAsUnsynced() {
  state.records.forEach((r) => (r.isSyncedToSql = false));
  saveToStorage(STORAGE_KEYS.RECORDS, state.records);
  renderAllUI();
  updateWifiSyncModalStats();
  showToast('🔄 Tüm yerel kayıtlar tekrar gönderime (/api/sync-calls) açıldı.', 'info');
}

/* ============================================================================
 * 12. KUYRUKTA BEKLEYEN ÇAĞRILAR HAVUZU & VERIMOR SIP SANTRAL
 * ============================================================================ */
function renderWaitingCallsBanner() {
  const banner = document.getElementById('waitingCallsBanner');
  const txt = document.getElementById('waitingBannerText');
  if (!banner || !txt) return;

  if (state.waitingQueue.length === 0) {
    banner.classList.remove('visible');
    return;
  }

  banner.classList.add('visible');
  const first = state.waitingQueue[0];
  txt.textContent = `⚠️ Bekleyen: ${state.waitingQueue.length} | Sıradaki: ${first.customerName || first.number} (${first.time})`;
}

function openWaitingQueueModal() {
  renderWaitingQueueModalList();
  openModal('waitingQueueModal');
}

function renderWaitingQueueModalList() {
  const cont = document.getElementById('waitingQueueListContainer');
  if (!cont) return;
  if (state.waitingQueue.length === 0) {
    cont.innerHTML = `<div style="text-align:center; padding:20px; color:var(--text-secondary);">Kuyrukta bekleyen çağrı bulunmuyor.</div>`;
    return;
  }

  cont.innerHTML = state.waitingQueue
    .map(
      (w, idx) => `
      <div class="history-item">
        <div class="history-top">
          <div>
            <div class="history-customer">📞 ${escapeHtml(w.number)} ${w.customerName ? `• ${escapeHtml(w.customerName)}` : ''}</div>
            <div class="history-meta">🕒 Geliş Saati: ${escapeHtml(w.time)} ${w.sicilNo ? `• 🆔 ${escapeHtml(w.sicilNo)}` : ''}</div>
          </div>
          <div style="display:flex; gap:5px;">
            <button type="button" class="btn btn-sm btn-green" onclick="takeWaitingCallByIndex(${idx})">▶️ Forma Al</button>
            <button type="button" class="btn btn-sm btn-red" onclick="removeWaitingCallByIndex(${idx})">✖</button>
          </div>
        </div>
      </div>
    `
    )
    .join('');
}

function takeNextWaitingCall() {
  if (state.waitingQueue.length === 0) return;
  takeWaitingCallByIndex(0);
}

function takeWaitingCallByIndex(idx) {
  const item = state.waitingQueue[idx];
  if (!item) return;
  state.waitingQueue.splice(idx, 1);
  saveToStorage(STORAGE_KEYS.WAITING_QUEUE, state.waitingQueue);

  clearSupportForm(false);
  document.getElementById('fTelefon').value = item.number || '';
  if (item.sicilNo) setSicilIntoForm(item.sicilNo);
  if (item.customerName) document.getElementById('fMusteriBayi').value = item.customerName;
  if (item.contactPerson) document.getElementById('fGorusulen').value = item.contactPerson;

  resetTimer();
  startCallTimer();
  updateCustomerPreviousCallsCard();
  renderWaitingCallsBanner();
  renderWaitingQueueModalList();
  closeModal('waitingQueueModal');
  switchTab(0);
  showToast(`📞 ${item.customerName || item.number} çağrısı forma alındı ve süre başlatıldı.`, 'success');
}

function removeWaitingCallByIndex(idx) {
  state.waitingQueue.splice(idx, 1);
  saveToStorage(STORAGE_KEYS.WAITING_QUEUE, state.waitingQueue);
  renderWaitingCallsBanner();
  renderWaitingQueueModalList();
}

function clearWaitingQueue() {
  state.waitingQueue = [];
  saveToStorage(STORAGE_KEYS.WAITING_QUEUE, state.waitingQueue);
  renderWaitingCallsBanner();
  renderWaitingQueueModalList();
  closeModal('waitingQueueModal');
  showToast('🗑️ Bekleyen çağrı kuyruğu temizlendi.', 'info');
}

function simulateIncomingSipCall(customNumber) {
  const sampleNumbers = ['05324108899', '05442201530', '02123456789', '05339876543'];
  const incomingNum = customNumber || sampleNumbers[Math.floor(Math.random() * sampleNumbers.length)];
  const p10 = normalizePhoneLast10(incomingNum);
  const matchedRec = state.records.find((r) => normalizePhoneLast10(r.telefon) === p10);

  const nowTime = new Date().toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit' });

  // Arama günlüğüne ekle
  state.missedCalls.unshift({
    id: Date.now(),
    number: incomingNum,
    customerName: matchedRec?.musteriBayi || '',
    sicilNo: matchedRec?.sicilNo || '',
    timestamp: formatDateTimeTR(new Date()),
    type: 'INCOMING',
    isHandled: false
  });
  saveToStorage(STORAGE_KEYS.MISSED, state.missedCalls);

  // Eğer form doluysa mevcut yazıları koru ve Kuyruğa ekle!
  if (isFormCurrentlyDirty()) {
    state.waitingQueue.push({
      id: Date.now(),
      number: incomingNum,
      customerName: matchedRec?.musteriBayi || '',
      contactPerson: matchedRec?.gorusulen || '',
      sicilNo: matchedRec?.sicilNo || '',
      time: nowTime
    });
    saveToStorage(STORAGE_KEYS.WAITING_QUEUE, state.waitingQueue);
    renderWaitingCallsBanner();
    renderTopBarAndQuickToolbar();
    closeModal('keypadModal');
    showToast(`🛡️ Form Dolu Koruma: Yeni gelen çağrı (${incomingNum}) yazılarınız silinmesin diye Bekleyen Kuyruğuna eklendi!`, 'info');
    return;
  }

  // Form boşsa doğrudan Canlı Çağrı Bandını göster
  state.activeSipCall = {
    number: incomingNum,
    customerName: matchedRec?.musteriBayi || '',
    sicilNo: matchedRec?.sicilNo || '',
    contactPerson: matchedRec?.gorusulen || '',
    state: 'RINGING',
    muted: false,
    speaker: false
  };
  renderActiveSipCallBanner();
  closeModal('keypadModal');
}

function renderActiveSipCallBanner() {
  const banner = document.getElementById('activeCallBanner');
  const icon = document.getElementById('activeCallIcon');
  const title = document.getElementById('activeCallTitle');
  const btnAns = document.getElementById('btnAnswerSip');
  const btnDec = document.getElementById('btnDeclineSip');
  const btnKey = document.getElementById('btnKeypadInCall');
  const btnMute = document.getElementById('btnMuteInCall');
  const btnSpk = document.getElementById('btnSpeakerInCall');
  const btnHang = document.getElementById('btnHangupSip');

  if (!banner) return;
  if (!state.activeSipCall) {
    banner.classList.remove('visible', 'ringing');
    return;
  }

  const c = state.activeSipCall;
  banner.classList.add('visible');
  banner.classList.toggle('ringing', c.state === 'RINGING');

  if (c.state === 'RINGING') {
    icon.textContent = '📲';
    title.textContent = `Gelen Çağrı: ${c.number} ${c.customerName ? `(${c.customerName})` : ''}`;
    btnAns.style.display = 'inline-flex';
    btnDec.style.display = 'inline-flex';
    btnKey.style.display = 'none';
    btnMute.style.display = 'none';
    btnSpk.style.display = 'none';
    btnHang.style.display = 'none';
  } else {
    icon.textContent = '📞';
    title.textContent = `Görüşmede: ${c.number} ${c.customerName ? `• ${c.customerName}` : ''}`;
    btnAns.style.display = 'none';
    btnDec.style.display = 'none';
    btnKey.style.display = 'inline-flex';
    btnMute.style.display = 'inline-flex';
    btnSpk.style.display = 'inline-flex';
    btnHang.style.display = 'inline-flex';
    btnMute.textContent = c.muted ? '🔇' : '🎤';
    btnSpk.textContent = c.speaker ? '📢' : '🔊';
  }
}

function answerActiveSipCall() {
  if (!state.activeSipCall) return;
  state.activeSipCall.state = 'INCALL';
  const c = state.activeSipCall;

  document.getElementById('fTelefon').value = c.number || '';
  if (c.sicilNo) setSicilIntoForm(c.sicilNo);
  if (c.customerName) document.getElementById('fMusteriBayi').value = c.customerName;
  if (c.contactPerson) document.getElementById('fGorusulen').value = c.contactPerson;

  if (state.settings.sipAutoTimer) {
    resetTimer();
    startCallTimer();
  }
  updateCustomerPreviousCallsCard();
  renderActiveSipCallBanner();
}

function hangupActiveSipCall() {
  state.activeSipCall = null;
  if (state.timerRunning) stopCallTimer();
  renderActiveSipCallBanner();
  showToast('📴 Çağrı sonlandırıldı.', 'info');
}

function toggleCallMute() {
  if (!state.activeSipCall) return;
  state.activeSipCall.muted = !state.activeSipCall.muted;
  renderActiveSipCallBanner();
}

function toggleCallSpeaker() {
  if (!state.activeSipCall) return;
  state.activeSipCall.speaker = !state.activeSipCall.speaker;
  renderActiveSipCallBanner();
}

function pressKeypad(digit) {
  const inp = document.getElementById('keypadDisplayInput');
  if (inp) inp.value += digit;
}

function startCallFromKeypad() {
  const num = (document.getElementById('keypadDisplayInput')?.value || '').trim();
  if (!num) {
    showToast('Lütfen aranacak numarayı veya dahiliyi girin.', 'error');
    return;
  }
  state.activeSipCall = {
    number: num,
    customerName: '',
    sicilNo: '',
    contactPerson: '',
    state: 'INCALL',
    muted: false,
    speaker: false
  };
  document.getElementById('fTelefon').value = num;
  if (state.settings.sipAutoTimer) {
    resetTimer();
    startCallTimer();
  }
  if (state.settings.sipAutoSearch) {
    triggerSmartLookup('TELEFON', num, false);
  }
  renderActiveSipCallBanner();
  closeModal('keypadModal');
}

/* ============================================================================
 * 13. SEKME 1: GEÇMİŞ KAYITLAR LİSTESİ & FİLTRELEME
 * ============================================================================ */
function setHistoryFilter(filterKey) {
  state.historyFilter = filterKey;
  const map = {
    ALL: 'histFilterAll',
    TODAY: 'histFilterToday',
    UNSYNCED: 'histFilterUnsynced',
    PENDING: 'histFilterPending',
    RESOLVED: 'histFilterResolved',
    ARGE: 'histFilterArge'
  };
  Object.entries(map).forEach(([k, id]) => {
    const el = document.getElementById(id);
    if (el) el.classList.toggle('active', k === filterKey);
  });
  renderHistoryList();
}

function renderHistoryList() {
  const cont = document.getElementById('historyListContainer');
  const badge = document.getElementById('historyTotalBadge');
  if (!cont) return;

  const q = (document.getElementById('historySearchInput')?.value || '').trim().toLocaleUpperCase('tr-TR');
  const todayPrefix = formatDateTimeTR(new Date()).slice(0, 10);

  const filtered = state.records.filter((r) => {
    if (state.historyFilter === 'TODAY' && !(r.basTarihi || '').startsWith(todayPrefix)) return false;
    if (state.historyFilter === 'UNSYNCED' && r.isSyncedToSql) return false;
    if (state.historyFilter === 'PENDING' && r.bekleyen !== 'Evet') return false;
    if (state.historyFilter === 'RESOLVED' && r.bekleyen === 'Evet') return false;
    if (state.historyFilter === 'ARGE' && r.argeGidis !== 'Evet' && r.argeDonus !== 'Evet') return false;

    if (!q) return true;
    const haystack = [
      r.sicilNo, r.musteriBayi, r.gorusulen, r.telefon, r.destekKonu,
      r.tespit, r.islem, r.kullanici, r.redmine, r.bagId
    ]
      .join(' ')
      .toLocaleUpperCase('tr-TR');
    return haystack.includes(q);
  });

  if (badge) badge.textContent = `${filtered.length} / ${state.records.length} Kayıt`;

  if (filtered.length === 0) {
    cont.innerHTML = `<div style="text-align:center; padding:28px; color:var(--text-secondary);">Filtreye uygun destek kaydı bulunamadı.</div>`;
    return;
  }

  cont.innerHTML = filtered
    .slice(0, 150)
    .map((r) => {
      const syncBadge = r.isSyncedToSql
        ? `<span class="badge badge-green">☁️ SQL Eşitlendi</span>`
        : `<span class="badge badge-orange">⏳ Offline (Sync Bekliyor)</span>`;
      const pendingBadge =
        r.bekleyen === 'Evet'
          ? `<span class="badge badge-red">⏳ Bekleyen İş</span>`
          : `<span class="badge badge-blue">✅ Çözüldü</span>`;

      return `
        <div class="history-item">
          <div class="history-top">
            <div>
              <div class="history-customer">
                🏢 ${escapeHtml(r.musteriBayi || 'İsimsiz Müşteri')}
                ${r.sicilNo ? `<span class="badge badge-blue tabular-nums">${escapeHtml(r.sicilNo)}</span>` : ''}
              </div>
              <div class="history-meta">
                👤 ${escapeHtml(r.gorusulen || '-')} • 📞 ${escapeHtml(r.telefon || '-')} • 🕒 ${escapeHtml(r.basTarihi || '-')} (${escapeHtml(r.sure || '00:00')})
              </div>
            </div>
            <div style="display:flex; gap:4px; flex-wrap:wrap; justify-content:flex-end;">
              ${syncBadge}
              ${pendingBadge}
            </div>
          </div>

          <div style="font-size:11.5px; margin:5px 0; color:var(--text-primary);">
            <div>📌 <b>Konu:</b> ${escapeHtml(r.destekKonu || '-')}</div>
            <div>🔍 <b>Tespit:</b> ${escapeHtml(r.tespit || '-')}</div>
            <div>🛠️ <b>İşlem:</b> ${escapeHtml(r.islem || '-')}</div>
          </div>

          <div class="history-actions">
            <button type="button" class="btn btn-sm btn-blue" onclick="loadRecordIntoFormForEdit(${r.id})">✏️ Düzenle</button>
            <button type="button" class="btn btn-sm btn-outline" onclick="toggleRecordPendingStatus(${r.id})">
              ${r.bekleyen === 'Evet' ? '✅ Çözüldü Yap' : '⏳ Bekleyene Al'}
            </button>
            <button type="button" class="btn btn-sm btn-whatsapp" onclick="shareRecordWhatsApp(${r.id})">💬 WhatsApp</button>
            <button type="button" class="btn btn-sm btn-outline" onclick="openDeviceReportBySicil('${escapeJs(r.sicilNo || r.musteriBayi || '')}')">🛠️ Karne</button>
            <button type="button" class="btn btn-sm btn-red" onclick="deleteSingleRecord(${r.id})">🗑️ Sil</button>
          </div>
        </div>
      `;
    })
    .join('');
}

function toggleRecordPendingStatus(id) {
  const r = state.records.find((item) => item.id === id);
  if (!r) return;
  r.bekleyen = r.bekleyen === 'Evet' ? 'Hayır' : 'Evet';
  r.isSyncedToSql = false;
  saveToStorage(STORAGE_KEYS.RECORDS, state.records);
  renderAllUI();
}

function deleteSingleRecord(id) {
  if (!confirm('Bu destek kaydını silmek istediğinize emin misiniz?')) return;
  state.records = state.records.filter((r) => r.id !== id);
  saveToStorage(STORAGE_KEYS.RECORDS, state.records);
  renderAllUI();
  showToast('🗑️ Kayıt silindi.', 'info');
}

function removeDuplicateRecords() {
  const before = state.records.length;
  const seen = new Set();
  state.records = state.records.filter((r) => {
    const key = `${(r.sicilNo || '').toUpperCase()}|${r.basTarihi || ''}|${(r.musteriBayi || '').toUpperCase()}|${(r.tespit || '').toUpperCase()}`;
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
  const removed = before - state.records.length;
  saveToStorage(STORAGE_KEYS.RECORDS, state.records);
  renderAllUI();
  showToast(`🧹 ${removed} adet mükerrer (çift) kayıt temizlendi.`, 'success');
}

/* ============================================================================
 * 14. SEKME 2: ARAMALAR & KAÇAN ÇAĞRILAR GÜNLÜĞÜ
 * ============================================================================ */
function setCallLogFilter(filterKey) {
  state.callLogFilter = filterKey;
  const map = {
    ALL: 'callFilterAll',
    MISSED: 'callFilterMissed',
    UNHANDLED: 'callFilterUnhandled',
    INCOMING: 'callFilterIncoming',
    OUTGOING: 'callFilterOutgoing'
  };
  Object.entries(map).forEach(([k, id]) => {
    const el = document.getElementById(id);
    if (el) el.classList.toggle('active', k === filterKey);
  });
  renderMissedCallsList();
}

function renderMissedCallsList() {
  const cont = document.getElementById('missedCallsListContainer');
  const badge = document.getElementById('missedCountHeaderBadge');
  if (!cont) return;

  const unhandledCount = state.missedCalls.filter((m) => !m.isHandled).length;
  if (badge) badge.textContent = `${unhandledCount} Bekleyen`;

  const filtered = state.missedCalls.filter((m) => {
    if (state.callLogFilter === 'MISSED' && m.type !== 'MISSED') return false;
    if (state.callLogFilter === 'UNHANDLED' && m.isHandled) return false;
    if (state.callLogFilter === 'INCOMING' && m.type !== 'INCOMING') return false;
    if (state.callLogFilter === 'OUTGOING' && m.type !== 'OUTGOING') return false;
    return true;
  });

  if (filtered.length === 0) {
    cont.innerHTML = `<div style="text-align:center; padding:24px; color:var(--text-secondary);">Kayıtlı çağrı bulunmuyor.</div>`;
    return;
  }

  cont.innerHTML = filtered
    .map(
      (m) => `
      <div class="history-item">
        <div class="history-top">
          <div>
            <div class="history-customer">
              📞 ${escapeHtml(m.number)}
              ${m.customerName ? `<span class="badge badge-blue">${escapeHtml(m.customerName)}</span>` : ''}
            </div>
            <div class="history-meta">
              🕒 ${escapeHtml(m.timestamp)} • ${m.type === 'MISSED' ? '🔴 Cevapsız' : m.type === 'OUTGOING' ? '↗️ Giden' : '↙️ Gelen'}
            </div>
          </div>
          <span class="badge ${m.isHandled ? 'badge-green' : 'badge-red'}">
            ${m.isHandled ? '✅ Dönüş Yapıldı' : '⏳ Dönüş Bekliyor'}
          </span>
        </div>
        <div class="history-actions">
          <button type="button" class="btn btn-sm btn-green" onclick="loadMissedCallIntoForm(${m.id})">▶️ Forma Al & Başlat</button>
          <button type="button" class="btn btn-sm btn-outline" onclick="toggleMissedCallHandled(${m.id})">
            ${m.isHandled ? '⏳ Bekleyene Al' : '✅ İşlendi Yap'}
          </button>
        </div>
      </div>
    `
    )
    .join('');
}

function loadMissedCallIntoForm(id) {
  const m = state.missedCalls.find((x) => x.id === id);
  if (!m) return;
  m.isHandled = true;
  saveToStorage(STORAGE_KEYS.MISSED, state.missedCalls);

  clearSupportForm(false);
  document.getElementById('fTelefon').value = m.number || '';
  if (m.sicilNo) setSicilIntoForm(m.sicilNo);
  if (m.customerName) document.getElementById('fMusteriBayi').value = m.customerName;

  resetTimer();
  startCallTimer();
  triggerSmartLookup('TELEFON', m.number, false);
  renderAllUI();
  switchTab(0);
}

function toggleMissedCallHandled(id) {
  const m = state.missedCalls.find((x) => x.id === id);
  if (!m) return;
  m.isHandled = !m.isHandled;
  saveToStorage(STORAGE_KEYS.MISSED, state.missedCalls);
  renderAllUI();
}

function promptAddMissedCall() {
  const num = prompt('Geri dönüş yapılacak telefon numarasını girin:');
  if (!num || !num.trim()) return;
  const p10 = normalizePhoneLast10(num);
  const matched = state.records.find((r) => normalizePhoneLast10(r.telefon) === p10);
  state.missedCalls.unshift({
    id: Date.now(),
    number: num.trim(),
    customerName: matched?.musteriBayi || '',
    sicilNo: matched?.sicilNo || '',
    timestamp: formatDateTimeTR(new Date()),
    type: 'MISSED',
    isHandled: false
  });
  saveToStorage(STORAGE_KEYS.MISSED, state.missedCalls);
  renderAllUI();
}

function toggleDutyMode() {
  state.settings.dutyMode = !state.settings.dutyMode;
  saveToStorage(STORAGE_KEYS.SETTINGS, state.settings);
  const icon = document.getElementById('dutyModeIcon');
  const txt = document.getElementById('dutyModeText');
  if (icon) icon.textContent = state.settings.dutyMode ? '🔔' : '🔕';
  if (txt) txt.textContent = state.settings.dutyMode ? 'Nöbet: Aktif' : 'Nöbet: Kapalı';
}

function markAllMissedHandled() {
  state.missedCalls.forEach((m) => (m.isHandled = true));
  saveToStorage(STORAGE_KEYS.MISSED, state.missedCalls);
  renderAllUI();
}

function clearAllMissedCalls() {
  if (!confirm('Tüm arama günlüğünü temizlemek istediğinize emin misiniz?')) return;
  state.missedCalls = [];
  saveToStorage(STORAGE_KEYS.MISSED, state.missedCalls);
  renderAllUI();
}

/* ============================================================================
 * 15. SEKME 3: KULLANICI TANIMLAMA, AYARLAR, YEDEKLEME & BELLEK YÖNETİMİ
 * ============================================================================ */
function togglePasswordVisibility(inputId, btnEl) {
  const inp = document.getElementById(inputId);
  if (!inp) return;
  const isPass = inp.type === 'password';
  inp.type = isPass ? 'text' : 'password';
  if (btnEl) btnEl.textContent = isPass ? '🙈' : '👁️';
}

function syncSettingsInputsFromState() {
  const s = state.settings;
  const setVal = (id, v) => {
    const el = document.getElementById(id);
    if (el) el.value = v !== undefined && v !== null ? v : '';
  };
  setVal('setSqlHost', s.sqlHost || '172.16.160.21');
  setVal('setSqlPort', s.sqlPort || '8080');
  setVal('setSqlType', s.sqlType || 'MSSQL');
  setVal('setSqlDb', s.sqlDb || 'destek_db');
  setVal('setSqlUser', s.sqlUser || '');
  setVal('setSqlPass', s.sqlPass || '');

  setVal('modalSqlHost', s.sqlHost || '172.16.160.21');
  setVal('modalSqlPort', s.sqlPort || '8080');
  setVal('modalSqlType', s.sqlType || 'MSSQL');
  setVal('modalSqlDb', s.sqlDb || 'destek_db');
  setVal('modalSqlUser', s.sqlUser || '');
  setVal('modalSqlPass', s.sqlPass || '');

  setVal('setGlobalKod', s.globalKod || '');
  setVal('setGlobalAd', s.globalAd || '');
  setVal('setOutlookEmail', s.outlookEmail || '');

  setVal('setSipDomain', s.sipDomain || '');
  setVal('setSipExt', s.sipExt || '');
  setVal('setSipPass', s.sipPass || '');
  setVal('setSipPort', s.sipPort || '5060');

  if (document.getElementById('setSipAutoTimer')) {
    document.getElementById('setSipAutoTimer').checked = Boolean(s.sipAutoTimer);
  }
  if (document.getElementById('setSipAutoSearch')) {
    document.getElementById('setSipAutoSearch').checked = Boolean(s.sipAutoSearch);
  }
}

function saveSqlSettings(notify = true) {
  state.settings.sqlHost = (document.getElementById('setSqlHost')?.value || '172.16.160.21').trim();
  state.settings.sqlPort = (document.getElementById('setSqlPort')?.value || '8080').trim();
  state.settings.sqlType = document.getElementById('setSqlType')?.value || 'MSSQL';
  state.settings.sqlDb = (document.getElementById('setSqlDb')?.value || 'destek_db').trim();
  state.settings.sqlUser = (document.getElementById('setSqlUser')?.value || '').trim();
  state.settings.sqlPass = document.getElementById('setSqlPass')?.value || '';
  saveToStorage(STORAGE_KEYS.SETTINGS, state.settings);
  syncSettingsInputsFromState();
  updateWifiSyncModalStats();
  if (notify) showToast('💾 Ofis Wi-Fi Sync & SQL (Kullanıcı/Şifre) ayarları kalıcı olarak kaydedildi.', 'success');
}

function saveGeneralSettings() {
  state.settings.globalKod = (document.getElementById('setGlobalKod')?.value || '').trim();
  state.settings.globalAd = (document.getElementById('setGlobalAd')?.value || '').trim().toLocaleUpperCase('tr-TR');
  state.settings.outlookEmail = (document.getElementById('setOutlookEmail')?.value || '').trim();
  saveToStorage(STORAGE_KEYS.SETTINGS, state.settings);

  if (document.getElementById('fKullaniciKodu')) document.getElementById('fKullaniciKodu').value = state.settings.globalKod;
  if (document.getElementById('fKullanici')) document.getElementById('fKullanici').value = state.settings.globalAd;

  renderAllUI();
  showToast(`✅ Aktif temsilci "${state.settings.globalAd}" olarak kalıcı kaydedildi.`, 'success');
}

async function saveSipSettings() {
  state.settings.sipDomain = (document.getElementById('setSipDomain')?.value || '').trim();
  state.settings.sipExt = (document.getElementById('setSipExt')?.value || '').trim();
  state.settings.sipPass = document.getElementById('setSipPass')?.value || '';
  state.settings.sipPort = (document.getElementById('setSipPort')?.value || '5060').trim();
  state.settings.sipAutoTimer = Boolean(document.getElementById('setSipAutoTimer')?.checked);
  state.settings.sipAutoSearch = Boolean(document.getElementById('setSipAutoSearch')?.checked);
  saveToStorage(STORAGE_KEYS.SETTINGS, state.settings);
  showToast(`💾 SIP Dahili (${state.settings.sipExt || '101'}) ayarları kalıcı kaydedildi. Sunucu bağlantısı kontrol ediliyor...`, 'info');
  // Gerçekçi bağlantı kontrolü: Sadece /api/test-db-connection yanıt verirse yeşil olur!
  await testSqlConnection();
}

function disconnectSipAndApiStatus() {
  state.connectionStatus = 'DISCONNECTED';
  updateConnectionStatusUI();
  renderSqlConnectionResultBoxes(false, '');
  showToast('📴 Bağlantı durumu Çevrimdışı / Bekliyor moduna alındı.', 'info');
}

function renderRepresentativePresets() {
  const cont = document.getElementById('repPresetsContainer');
  const modalCont = document.getElementById('repSwitchListContainer');

  const html = state.reps
    .map((rep) => {
      const isSelected = state.settings.globalAd === rep.name;
      return `
        <button type="button" class="chip ${isSelected ? 'selected-green' : ''}" onclick="selectActiveRep('${escapeJs(rep.code)}', '${escapeJs(rep.name)}')">
          👤 [${escapeHtml(rep.code)}] ${escapeHtml(rep.name)} ${isSelected ? '✓' : ''}
        </button>
      `;
    })
    .join('');

  if (cont) cont.innerHTML = html;

  if (modalCont) {
    modalCont.innerHTML = state.reps
      .map((rep) => {
        const isSelected = state.settings.globalAd === rep.name;
        return `
          <div class="history-item" style="display:flex; justify-content:space-between; align-items:center; cursor:pointer; margin-bottom:0;" onclick="selectActiveRep('${escapeJs(rep.code)}', '${escapeJs(rep.name)}'); closeModal('repSwitchModal');">
            <div>
              <div style="font-weight:800; font-size:13px;">👤 ${escapeHtml(rep.name)}</div>
              <div style="font-size:11px; color:var(--text-secondary);">Temsilci Kodu: #${escapeHtml(rep.code)}</div>
            </div>
            <span class="badge ${isSelected ? 'badge-green' : 'badge-blue'}">${isSelected ? '✓ Aktif' : 'Seç'}</span>
          </div>
        `;
      })
      .join('');
  }
}

function openRepSwitchModal() {
  renderRepresentativePresets();
  openModal('repSwitchModal');
}

function selectActiveRep(code, name) {
  state.settings.globalKod = code;
  state.settings.globalAd = name;
  saveToStorage(STORAGE_KEYS.SETTINGS, state.settings);
  if (document.getElementById('fKullaniciKodu')) document.getElementById('fKullaniciKodu').value = code;
  if (document.getElementById('fKullanici')) document.getElementById('fKullanici').value = name;
  renderAllUI();
  showToast(`👤 Aktif temsilci: ${name} (${code})`, 'success');
}

function promptAddRepresentative() {
  const code = prompt('Yeni Temsilci Kodu (Örn: 9):');
  if (!code) return;
  const name = prompt('Yeni Temsilci Adı Soyadı:');
  if (!name) return;
  const cleanName = name.trim().toLocaleUpperCase('tr-TR');
  state.reps.push({ code: code.trim(), name: cleanName });
  saveToStorage(STORAGE_KEYS.REPS, state.reps);
  selectActiveRep(code.trim(), cleanName);
}

/* --- YEDEKLEME VE GERİ YÜKLEME --- */
function exportFullBackupJson() {
  const payload = {
    version: 3,
    exportedAt: formatDateTimeTR(new Date()),
    settings: state.settings,
    records: state.records,
    missedCalls: state.missedCalls,
    prefixes: state.prefixes,
    reps: state.reps,
    templates: state.templates,
    scenarios: state.scenarios,
    kb: state.kb
  };
  const blob = new Blob([JSON.stringify(payload, null, 2)], { type: 'application/json;charset=utf-8' });
  const filename = `Destek_Yedek_${formatDateInputISO(new Date())}.json`;
  triggerBlobDownload(blob, filename);
  showToast(`📤 Tam sistem yedeği (${state.records.length} kayıt) indirildi.`, 'success');
}

function handleBackupFileSelected(ev) {
  const file = ev.target?.files?.[0];
  if (!file) return;
  const reader = new FileReader();
  reader.onload = (e) => {
    importJsonBackupString(String(e.target?.result || ''));
    ev.target.value = '';
  };
  reader.readAsText(file, 'utf-8');
}

async function pasteJsonRecordsFromClipboard() {
  try {
    const text = await navigator.clipboard.readText();
    if (!text) {
      showToast('Panoda metin bulunamadı.', 'error');
      return;
    }
    importJsonBackupString(text);
  } catch (_) {
    const manual = prompt('Yedek JSON metnini buraya yapıştırın:');
    if (manual) importJsonBackupString(manual);
  }
}

function importJsonBackupString(rawJson) {
  try {
    const parsed = JSON.parse(rawJson);
    const incomingRecords = Array.isArray(parsed)
      ? parsed
      : Array.isArray(parsed.records)
      ? parsed.records
      : [];

    if (parsed.settings && typeof parsed.settings === 'object') {
      state.settings = Object.assign({}, state.settings, parsed.settings);
    }
    if (Array.isArray(parsed.reps) && parsed.reps.length > 0) state.reps = parsed.reps;
    if (Array.isArray(parsed.prefixes) && parsed.prefixes.length > 0) state.prefixes = parsed.prefixes;
    if (parsed.templates && typeof parsed.templates === 'object') state.templates = parsed.templates;
    if (Array.isArray(parsed.scenarios) && parsed.scenarios.length > 0) state.scenarios = parsed.scenarios;

    let added = 0;
    const existingIds = new Set(state.records.map((r) => r.id));
    incomingRecords.forEach((r) => {
      const id = r.id || Date.now() + Math.floor(Math.random() * 100000);
      if (!existingIds.has(id)) {
        existingIds.add(id);
        state.records.push({ ...r, id });
        added++;
      }
    });

    state.records.sort((a, b) => parseRecordDateMs(b.basTarihi) - parseRecordDateMs(a.basTarihi));
    saveAllState();
    renderAllUI();
    showToast(`✅ Yedek başarıyla yüklendi (${added} yeni kayıt eklendi).`, 'success');
  } catch (e) {
    showToast('Geçersiz JSON yedek dosyası formatı.', 'error');
  }
}

/* --- YEREL HAFIZA TEMİZLEME (ARKA OFİS KORUNUR) --- */
function clearSyncedLocalRecordsOnly() {
  const syncedCount = state.records.filter((r) => r.isSyncedToSql).length;
  if (syncedCount === 0) {
    showToast('Silinecek SQL ile eşitlenmiş yerel kayıt bulunamadı.', 'info');
    return;
  }
  if (!confirm(`SQL ile eşitlenmiş ${syncedCount} adet yerel kaydı telefondan silmek istiyor musunuz? (Arka ofis veritabanındaki kayıtlarınız silinmez)`)) return;
  state.records = state.records.filter((r) => !r.isSyncedToSql);
  saveToStorage(STORAGE_KEYS.RECORDS, state.records);
  renderAllUI();
  showToast(`🧹 ${syncedCount} eşitlenmiş kayıt yerel hafızadan temizlendi.`, 'success');
}

function clearOldSyncedRecords(days) {
  const cutoff = Date.now() - days * 86400000;
  const before = state.records.length;
  state.records = state.records.filter((r) => {
    if (!r.isSyncedToSql) return true;
    const ms = parseRecordDateMs(r.basTarihi);
    return ms === 0 || ms >= cutoff;
  });
  const removed = before - state.records.length;
  saveToStorage(STORAGE_KEYS.RECORDS, state.records);
  renderAllUI();
  showToast(`🗓️ ${days} günden eski ${removed} eşitlenmiş kayıt temizlendi.`, 'success');
}

function deleteRecordsByDateRange() {
  const sIso = document.getElementById('delStartDate')?.value;
  const eIso = document.getElementById('delEndDate')?.value;
  if (!sIso || !eIso) {
    showToast('Lütfen başlangıç ve bitiş tarihi seçin.', 'error');
    return;
  }
  const sMs = new Date(sIso + 'T00:00:00').getTime();
  const eMs = new Date(eIso + 'T23:59:59').getTime();
  const before = state.records.length;
  state.records = state.records.filter((r) => {
    const ms = parseRecordDateMs(r.basTarihi);
    return ms < sMs || ms > eMs;
  });
  const removed = before - state.records.length;
  saveToStorage(STORAGE_KEYS.RECORDS, state.records);
  renderAllUI();
  showToast(`🗑️ Seçili tarih aralığındaki ${removed} kayıt silindi.`, 'info');
}

function deleteAllRecordsConfirm() {
  if (!confirm('DİKKAT: Telefondaki tüm yerel kayıtlar silinecek! Devam etmek istiyor musunuz?')) return;
  state.records = [];
  saveToStorage(STORAGE_KEYS.RECORDS, state.records);
  renderAllUI();
  showToast('🔥 Tüm yerel kayıtlar sıfırlandı.', 'info');
}

function updateStorageMetricsUI() {
  const badge = document.getElementById('storageSizeBadge');
  if (!badge) return;
  try {
    let bytes = 0;
    Object.values(STORAGE_KEYS).forEach((k) => {
      const v = localStorage.getItem(k);
      if (v) bytes += v.length * 2;
    });
    badge.textContent = `${(bytes / 1024).toFixed(1)} KB`;
  } catch (_) {}
}

/* ============================================================================
 * 16. MODAL PENCERELER, İSTATİSTİK, GÜN SONU, CİHAZ KARNESİ & MÜDÜR PANELİ
 * ============================================================================ */
function openModal(id) {
  const el = document.getElementById(id);
  if (el) el.classList.add('open');
}

function closeModal(id) {
  const el = document.getElementById(id);
  if (el) el.classList.remove('open');
}

/* --- İSTATİSTİK MODALI --- */
function openStatisticsModal() {
  renderStatisticsModalContent();
  openModal('statsModal');
}

function setStatsRange(range) {
  state.statsRange = range;
  ['ALL', 'TODAY', 'WEEK', 'MONTH'].forEach((r) => {
    const btn = document.getElementById(`stRange${r === 'ALL' ? 'All' : r === 'TODAY' ? 'Today' : r === 'WEEK' ? 'Week' : 'Month'}`);
    if (btn) btn.classList.toggle('active', r === range);
  });
  renderStatisticsModalContent();
}

function filterRecordsByPeriodKey(records, periodKey) {
  const now = new Date();
  const todayPrefix = formatDateTimeTR(now).slice(0, 10);
  if (periodKey === 'TODAY') {
    return records.filter((r) => (r.basTarihi || '').startsWith(todayPrefix));
  }
  if (periodKey === 'YESTERDAY') {
    const y = new Date(now);
    y.setDate(now.getDate() - 1);
    const yPrefix = formatDateTimeTR(y).slice(0, 10);
    return records.filter((r) => (r.basTarihi || '').startsWith(yPrefix));
  }
  if (periodKey === 'WEEK') {
    const cutoff = Date.now() - 7 * 86400000;
    return records.filter((r) => parseRecordDateMs(r.basTarihi) >= cutoff);
  }
  if (periodKey === 'MONTH') {
    const mPrefix = `${String(now.getMonth() + 1).padStart(2, '0')}.${now.getFullYear()}`;
    return records.filter((r) => (r.basTarihi || '').slice(3, 10) === mPrefix);
  }
  return records;
}

function renderStatisticsModalContent() {
  const cont = document.getElementById('statsModalContent');
  if (!cont) return;
  const list = filterRecordsByPeriodKey(state.records, state.statsRange);
  const total = list.length;
  const resolved = list.filter((r) => r.bekleyen !== 'Evet').length;
  const pending = list.filter((r) => r.bekleyen === 'Evet').length;
  const fcr = total > 0 ? Math.round((resolved / total) * 100) : 0;

  const totalSec = list.reduce((acc, r) => acc + parseDurationToSeconds(r.sure), 0);
  const avgSec = total > 0 ? Math.round(totalSec / total) : 0;

  // Konu dağılımı
  const topicCounts = {};
  list.forEach((r) => {
    const k = r.destekKonu || 'GENEL DESTEK';
    topicCounts[k] = (topicCounts[k] || 0) + 1;
  });
  const topTopics = Object.entries(topicCounts)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 6);

  cont.innerHTML = `
    <div class="kpi-strip">
      <div class="kpi-box">
        <div class="kpi-label">Toplam Çağrı</div>
        <div class="kpi-val" style="color:#60A5FA;">${total}</div>
      </div>
      <div class="kpi-box">
        <div class="kpi-label">İlk Çağrıda Çözüm</div>
        <div class="kpi-val" style="color:#4ADE80;">%${fcr}</div>
      </div>
      <div class="kpi-box">
        <div class="kpi-label">Bekleyen İş</div>
        <div class="kpi-val" style="color:#FBBF24;">${pending}</div>
      </div>
      <div class="kpi-box">
        <div class="kpi-label">Ort. Süre</div>
        <div class="kpi-val tabular-nums" style="color:#C084FC;">${formatSecondsMMSS(avgSec)}</div>
      </div>
    </div>

    <div class="section-card" style="margin-bottom:0;">
      <div class="section-title" style="margin-bottom:8px;">📌 En Çok Gelen Destek Konuları</div>
      ${
        topTopics.length === 0
          ? `<div style="color:var(--text-secondary); font-size:12px;">Kayıt bulunamadı.</div>`
          : topTopics
              .map(([topic, cnt]) => {
                const pct = Math.round((cnt / Math.max(1, total)) * 100);
                return `
                  <div style="margin-bottom:8px;">
                    <div style="display:flex; justify-content:space-between; font-size:11.5px; margin-bottom:3px;">
                      <span>${escapeHtml(topic)}</span>
                      <b>${cnt} Çağrı (%${pct})</b>
                    </div>
                    <div style="height:7px; background:rgba(255,255,255,0.07); border-radius:99px; overflow:hidden;">
                      <div style="height:100%; width:${pct}%; background:var(--ios-blue);"></div>
                    </div>
                  </div>
                `;
              })
              .join('')
      }
    </div>
  `;
}

/* --- GÜN SONU VARDİYA RAPORU --- */
function openDailyShiftModal() {
  const dateInput = document.getElementById('shiftReportDate');
  if (dateInput && !dateInput.value) {
    dateInput.value = formatDateInputISO(new Date());
  }
  renderDailyShiftReport();
  openModal('shiftModal');
}

function setShiftDateOffset(daysOffset) {
  const d = new Date();
  d.setDate(d.getDate() + daysOffset);
  const dateInput = document.getElementById('shiftReportDate');
  if (dateInput) dateInput.value = formatDateInputISO(d);
  renderDailyShiftReport();
}

function renderDailyShiftReport() {
  const iso = document.getElementById('shiftReportDate')?.value || formatDateInputISO(new Date());
  const parts = iso.split('-');
  const trDate = parts.length === 3 ? `${parts[2]}.${parts[1]}.${parts[0]}` : iso;

  const list = state.records.filter((r) => (r.basTarihi || '').startsWith(trDate));
  const resolved = list.filter((r) => r.bekleyen !== 'Evet').length;
  const pending = list.filter((r) => r.bekleyen === 'Evet').length;
  const arge = list.filter((r) => r.argeGidis === 'Evet').length;

  const lines = [
    `📋 TELEFON DESTEK GÜN SONU VARDİYA RAPORU`,
    `📅 Tarih: ${trDate}`,
    `👤 Temsilci: ${state.settings.globalAd || '-'}`,
    `----------------------------------------`,
    `📞 Toplam Çağrı : ${list.length}`,
    `✅ Çözülen      : ${resolved}`,
    `⏳ Bekleyen İş  : ${pending}`,
    `🧪 AR-GE Sevk   : ${arge}`,
    `----------------------------------------`,
    `📌 ÇAĞRI DETAY ÖZETİ:`
  ];

  list.forEach((r, i) => {
    lines.push(
      `${i + 1}. [${r.sicilNo || '-'}] ${r.musteriBayi || '-'} | ${r.destekKonu || '-'} -> ${r.islem || '-'} (${r.bekleyen === 'Evet' ? 'BEKLİYOR' : 'ÇÖZÜLDÜ'})`
    );
  });

  const area = document.getElementById('shiftReportText');
  if (area) area.value = lines.join('\n');
}

function copyShiftReport() {
  const txt = document.getElementById('shiftReportText')?.value || '';
  navigator.clipboard?.writeText(txt);
  showToast('📋 Gün sonu raporu panoya kopyalandı.', 'success');
}

function shareShiftReportWhatsApp() {
  const txt = document.getElementById('shiftReportText')?.value || '';
  window.open(`https://wa.me/?text=${encodeURIComponent(txt)}`, '_blank');
}

function exportShiftDateExcel() {
  const iso = document.getElementById('shiftReportDate')?.value || formatDateInputISO(new Date());
  const parts = iso.split('-');
  const trDate = parts.length === 3 ? `${parts[2]}.${parts[1]}.${parts[0]}` : iso;
  const list = state.records.filter((r) => (r.basTarihi || '').startsWith(trDate));
  exportRecordsToXlsx(list, `Gun_Sonu_${iso}.xlsx`);
}

/* --- CİHAZ / SİCİL SERVİS KARNESİ --- */
function openDeviceReportForCurrentSicil() {
  const query = getFullSicilFromForm() || document.getElementById('fMusteriBayi')?.value || '';
  openDeviceReportBySicil(query);
}

function openDeviceReportBySicil(query) {
  const inp = document.getElementById('deviceReportSearchInput');
  if (inp) inp.value = query || '';
  renderDeviceServiceReport();
  openModal('deviceReportModal');
}

function renderDeviceServiceReport() {
  const q = (document.getElementById('deviceReportSearchInput')?.value || '').trim().toLocaleUpperCase('tr-TR');
  const cont = document.getElementById('deviceReportContent');
  if (!cont) return;

  if (!q) {
    cont.innerHTML = `<div style="padding:18px; text-align:center; color:var(--text-secondary);">Sorgulamak için Sicil No veya Müşteri adı girin.</div>`;
    return;
  }

  const matches = state.records.filter(
    (r) =>
      (r.sicilNo || '').toLocaleUpperCase('tr-TR').includes(q) ||
      (r.musteriBayi || '').toLocaleUpperCase('tr-TR').includes(q)
  );

  if (matches.length === 0) {
    cont.innerHTML = `<div style="padding:18px; text-align:center; color:var(--text-secondary);">"${escapeHtml(q)}" için geçmiş servis kaydı bulunamadı.</div>`;
    return;
  }

  const head = matches[0];
  cont.innerHTML = `
    <div class="section-card" style="margin-bottom:8px;">
      <div style="font-weight:800; font-size:14px; color:#60A5FA;">🆔 ${escapeHtml(head.sicilNo || '-')} — ${escapeHtml(head.musteriBayi || '-')}</div>
      <div style="font-size:11.5px; color:var(--text-secondary); margin-top:4px;">
        👤 Yetkili: ${escapeHtml(head.gorusulen || '-')} • 📞 Tel: ${escapeHtml(head.telefon || '-')} • Toplam Çağrı: <b>${matches.length}</b>
      </div>
    </div>
    ${matches
      .map(
        (m) => `
        <div class="history-item">
          <div class="history-top">
            <b>📌 ${escapeHtml(m.destekKonu || '-')}</b>
            <span class="badge badge-blue">${escapeHtml(m.basTarihi || '-')}</span>
          </div>
          <div style="font-size:11.5px; color:var(--text-secondary);">
            🔍 <b>Tespit:</b> ${escapeHtml(m.tespit || '-')}<br/>
            🛠️ <b>İşlem:</b> ${escapeHtml(m.islem || '-')}<br/>
            👤 <b>Temsilci:</b> ${escapeHtml(m.kullanici || '-')}
          </div>
        </div>
      `
      )
      .join('')}
  `;
}

function copyDeviceReportText() {
  const q = (document.getElementById('deviceReportSearchInput')?.value || '').trim().toLocaleUpperCase('tr-TR');
  const matches = state.records.filter(
    (r) =>
      (r.sicilNo || '').toLocaleUpperCase('tr-TR').includes(q) ||
      (r.musteriBayi || '').toLocaleUpperCase('tr-TR').includes(q)
  );
  const txt = matches
    .map((m, i) => `${i + 1}. [${m.basTarihi}] ${m.sicilNo} - ${m.musteriBayi} | ${m.destekKonu} | ${m.islem}`)
    .join('\n');
  navigator.clipboard?.writeText(txt || 'Kayıt yok');
  showToast('📋 Cihaz servis karnesi panoya kopyalandı.', 'success');
}

function shareDeviceReportWhatsApp() {
  const q = (document.getElementById('deviceReportSearchInput')?.value || '').trim().toLocaleUpperCase('tr-TR');
  const matches = state.records.filter(
    (r) =>
      (r.sicilNo || '').toLocaleUpperCase('tr-TR').includes(q) ||
      (r.musteriBayi || '').toLocaleUpperCase('tr-TR').includes(q)
  );
  const txt = `🛠️ *CİHAZ SERVİS KARNESİ (${q})*\n` + matches.map((m, i) => `${i + 1}. [${m.basTarihi}] ${m.destekKonu} -> ${m.islem}`).join('\n');
  window.open(`https://wa.me/?text=${encodeURIComponent(txt)}`, '_blank');
}

function exportDeviceReportExcel() {
  const q = (document.getElementById('deviceReportSearchInput')?.value || '').trim().toLocaleUpperCase('tr-TR');
  const matches = state.records.filter(
    (r) =>
      (r.sicilNo || '').toLocaleUpperCase('tr-TR').includes(q) ||
      (r.musteriBayi || '').toLocaleUpperCase('tr-TR').includes(q)
  );
  exportRecordsToXlsx(matches, `Cihaz_Karne_${q || 'Rapor'}.xlsx`);
}

/* ============================================================================
 * 17. SERVİS MÜDÜRÜ YÖNETİCİ PANELİ (4 SEKMELİ TAM DASHBOARD)
 * ============================================================================ */
function openServiceManagerModal() {
  renderServiceManagerDashboard();
  openModal('serviceManagerModal');
}

function setManagerPeriod(period) {
  state.managerPeriod = period;
  ['TODAY', 'YESTERDAY', 'WEEK', 'MONTH', 'ALL'].forEach((p) => {
    const el = document.getElementById(`mgrPer${p}`);
    if (el) el.classList.toggle('active', p === period);
  });
  renderServiceManagerDashboard();
}

function setManagerTab(tabIdx) {
  state.managerTab = tabIdx;
  for (let i = 0; i < 4; i++) {
    const el = document.getElementById(`mgrTab${i}`);
    if (el) el.classList.toggle('active', i === tabIdx);
  }
  renderServiceManagerDashboard();
}

function renderServiceManagerDashboard() {
  const cont = document.getElementById('serviceManagerContent');
  if (!cont) return;

  const periodList = filterRecordsByPeriodKey(state.records, state.managerPeriod);
  const filteredList = state.managerRepFilter
    ? periodList.filter((r) => (r.kullanici || 'ATANMAMIŞ') === state.managerRepFilter)
    : periodList;

  const total = filteredList.length;
  const resolved = filteredList.filter((r) => r.bekleyen !== 'Evet').length;
  const pending = filteredList.filter((r) => r.bekleyen === 'Evet').length;
  const fcr = total > 0 ? Math.round((resolved / total) * 100) : 0;

  // Temsilci bazlı gruplama
  const byRep = {};
  periodList.forEach((r) => {
    const rep = (r.kullanici || 'ATANMAMIŞ').trim();
    if (!byRep[rep]) byRep[rep] = { name: rep, count: 0, resolved: 0, pending: 0, totalSec: 0 };
    byRep[rep].count++;
    if (r.bekleyen === 'Evet') byRep[rep].pending++;
    else byRep[rep].resolved++;
    byRep[rep].totalSec += parseDurationToSeconds(r.sure);
  });
  const repRows = Object.values(byRep).sort((a, b) => b.count - a.count);

  const kpiHtml = `
    <div class="kpi-strip">
      <div class="kpi-box">
        <div class="kpi-label">Dönem Çağrı</div>
        <div class="kpi-val" style="color:#60A5FA;">${total}</div>
      </div>
      <div class="kpi-box">
        <div class="kpi-label">Çözüm Oranı</div>
        <div class="kpi-val" style="color:#4ADE80;">%${fcr}</div>
      </div>
      <div class="kpi-box">
        <div class="kpi-label">Bekleyen İş</div>
        <div class="kpi-val" style="color:#FBBF24;">${pending}</div>
      </div>
      <div class="kpi-box">
        <div class="kpi-label">Aktif Temsilci</div>
        <div class="kpi-val" style="color:#C084FC;">${repRows.length}</div>
      </div>
    </div>
  `;

  if (state.managerTab === 0) {
    // SEKME 0: TEMSİLCİLER PERFORMANS LİSTESİ
    cont.innerHTML =
      kpiHtml +
      (repRows.length === 0
        ? `<div style="padding:20px; text-align:center; color:var(--text-secondary);">Seçili dönemde temsilci kaydı bulunamadı.</div>`
        : repRows
            .map((rp, i) => {
              const rate = rp.count > 0 ? Math.round((rp.resolved / rp.count) * 100) : 0;
              const avgSec = rp.count > 0 ? Math.round(rp.totalSec / rp.count) : 0;
              return `
                <div class="history-item">
                  <div class="history-top">
                    <div>
                      <div class="history-customer">#${i + 1} 👤 ${escapeHtml(rp.name)}</div>
                      <div class="history-meta">
                        📞 Toplam: <b>${rp.count}</b> • ✅ Çözülen: <b>${rp.resolved}</b> • ⏳ Bekleyen: <b>${rp.pending}</b> • ⏱️ Ort: <b>${formatSecondsMMSS(avgSec)}</b>
                      </div>
                    </div>
                    <span class="badge ${rate >= 85 ? 'badge-green' : 'badge-orange'}">Çözüm %${rate}</span>
                  </div>
                </div>
              `;
            })
            .join(''));
  } else if (state.managerTab === 1) {
    // SEKME 1: GRAFİKLER & KONU / SAAT DAĞILIMI
    const topicMap = {};
    filteredList.forEach((r) => {
      const k = r.destekKonu || 'DİĞER';
      topicMap[k] = (topicMap[k] || 0) + 1;
    });
    const topics = Object.entries(topicMap).sort((a, b) => b[1] - a[1]);

    cont.innerHTML =
      kpiHtml +
      `<div class="section-card" style="margin-bottom:0;">
        <div class="section-title" style="margin-bottom:8px;">📊 Konu Bazlı Çağrı Yoğunluğu</div>
        ${topics
          .map(([t, c]) => {
            const pct = Math.round((c / Math.max(1, total)) * 100);
            return `
              <div style="margin-bottom:8px;">
                <div style="display:flex; justify-content:space-between; font-size:11.5px; margin-bottom:2px;">
                  <span>${escapeHtml(t)}</span>
                  <b>${c} (%${pct})</b>
                </div>
                <div style="height:8px; background:rgba(255,255,255,0.06); border-radius:99px; overflow:hidden;">
                  <div style="height:100%; width:${pct}%; background:linear-gradient(90deg,#3B82F6,#10B981);"></div>
                </div>
              </div>
            `;
          })
          .join('')}
      </div>`;
  } else if (state.managerTab === 2) {
    // SEKME 2: ÇAĞRI LİSTESİ DENETİMİ
    cont.innerHTML =
      kpiHtml +
      filteredList
        .slice(0, 100)
        .map(
          (r) => `
          <div class="history-item">
            <div class="history-top">
              <div>
                <div class="history-customer">🏢 ${escapeHtml(r.musteriBayi || '-')} (${escapeHtml(r.sicilNo || '-')})</div>
                <div class="history-meta">👤 Temsilci: <b>${escapeHtml(r.kullanici || '-')}</b> • 🕒 ${escapeHtml(r.basTarihi || '-')}</div>
              </div>
              <button type="button" class="btn btn-sm btn-blue" onclick="closeModal('serviceManagerModal'); loadRecordIntoFormForEdit(${r.id});">✏️ İncele</button>
            </div>
            <div style="font-size:11px; color:var(--text-secondary);">
              📌 ${escapeHtml(r.destekKonu || '-')} | 🛠️ ${escapeHtml(r.islem || '-')}
            </div>
          </div>
        `
        )
        .join('');
  } else {
    // SEKME 3: YÖNETİCİ RAPOR ÖNİZLEME
    const reportTxt = buildManagerReportPlainText();
    cont.innerHTML =
      kpiHtml +
      `<textarea class="form-textarea" style="min-height:220px; font-family:monospace; font-size:12px;" readonly>${escapeHtml(reportTxt)}</textarea>`;
  }
}

function buildManagerReportPlainText() {
  const list = filterRecordsByPeriodKey(state.records, state.managerPeriod);
  const resolved = list.filter((r) => r.bekleyen !== 'Evet').length;
  const pending = list.filter((r) => r.bekleyen === 'Evet').length;

  const byRep = {};
  list.forEach((r) => {
    const rep = r.kullanici || 'ATANMAMIŞ';
    byRep[rep] = (byRep[rep] || 0) + 1;
  });

  const repSummary = Object.entries(byRep)
    .sort((a, b) => b[1] - a[1])
    .map(([name, cnt]) => `- ${name}: ${cnt} Çağrı`)
    .join('\n');

  return [
    `👔 SERVİS MÜDÜRÜ YÖNETİCİ DENETİM RAPORU`,
    `📅 Dönem: ${state.managerPeriod}`,
    `📞 Toplam Çağrı: ${list.length} | ✅ Çözülen: ${resolved} | ⏳ Bekleyen: ${pending}`,
    `----------------------------------------`,
    `👥 TEMSİLCİ PERFORMANS DAĞILIMI:`,
    repSummary || '- Kayıt yok'
  ].join('\n');
}

function copyManagerReportText() {
  navigator.clipboard?.writeText(buildManagerReportPlainText());
  showToast('📋 Yönetici raporu panoya kopyalandı.', 'success');
}

function shareManagerReportWhatsApp() {
  window.open(`https://wa.me/?text=${encodeURIComponent(buildManagerReportPlainText())}`, '_blank');
}

function exportManagerFilteredExcel() {
  const list = filterRecordsByPeriodKey(state.records, state.managerPeriod);
  exportRecordsToXlsx(list, `Mudur_Raporu_${state.managerPeriod}.xlsx`);
}

/* ============================================================================
 * 18. ARIZA & HATA KODU BİLGİ BANKASI (KNOWLEDGE BASE)
 * ============================================================================ */
function openKnowledgeBaseModal() {
  renderKnowledgeBase();
  openModal('kbModal');
}

function renderKnowledgeBase() {
  const cont = document.getElementById('kbArticlesContainer');
  if (!cont) return;
  const q = (document.getElementById('kbSearchInput')?.value || '').trim().toLocaleUpperCase('tr-TR');

  const filtered = state.kb.filter((item) => {
    if (!q) return true;
    return [item.code, item.category, item.title, item.symptoms, item.solution]
      .join(' ')
      .toLocaleUpperCase('tr-TR')
      .includes(q);
  });

  cont.innerHTML = filtered
    .map(
      (item) => `
      <div class="history-item">
        <div class="history-top">
          <div>
            <span class="badge badge-purple">${escapeHtml(item.code)}</span>
            <span class="badge badge-blue">${escapeHtml(item.category)}</span>
            <div style="font-weight:800; font-size:13px; margin-top:4px;">${escapeHtml(item.title)}</div>
          </div>
          <button type="button" class="btn btn-sm btn-green" onclick="applyKbArticleToForm('${escapeJs(item.id)}')">⚡ Forma Aktar</button>
        </div>
        <div style="font-size:11.5px; color:var(--text-secondary); margin-top:4px; white-space:pre-line;">
          <b>Belirti:</b> ${escapeHtml(item.symptoms)}
          <b>Çözüm:</b> ${escapeHtml(item.solution)}
        </div>
      </div>
    `
    )
    .join('');
}

function applyKbArticleToForm(kbId) {
  const item = state.kb.find((x) => x.id === kbId);
  if (!item) return;
  if (item.konu) document.getElementById('fDestekKonu').value = item.konu;
  if (item.tespit) document.getElementById('fTespit').value = item.tespit;
  if (item.islem) document.getElementById('fIslem').value = item.islem;
  renderTemplatesAndPopularChips();
  closeModal('kbModal');
  switchTab(0);
  showToast(`📚 "${item.code}" çözüm adımları forma aktarıldı.`, 'success');
}

function promptAddKnowledgeArticle() {
  const code = prompt('Hata Kodu (Örn: ERR-505):');
  if (!code) return;
  const title = prompt('Arıza Başlığı:');
  if (!title) return;
  const solution = prompt('Çözüm Adımları:');
  if (!solution) return;

  state.kb.unshift({
    id: 'kb_' + Date.now(),
    code: code.trim().toUpperCase(),
    category: 'GENEL',
    title: title.trim(),
    symptoms: title.trim(),
    solution: solution.trim(),
    konu: title.trim().toLocaleUpperCase('tr-TR'),
    tespit: title.trim().toLocaleUpperCase('tr-TR'),
    islem: solution.trim().toLocaleUpperCase('tr-TR')
  });
  saveToStorage(STORAGE_KEYS.KB, state.kb);
  renderKnowledgeBase();
}

/* ============================================================================
 * 19. WHATSAPP, E-POSTA VE TELEFON PAYLAŞIM YARDIMCILARI
 * ============================================================================ */
function dialPhoneField(inputId) {
  const val = (document.getElementById(inputId)?.value || '').trim();
  if (!val) {
    openModal('keypadModal');
    return;
  }
  window.location.href = `tel:${val}`;
}

function whatsappPhoneField(inputId) {
  const raw = (document.getElementById(inputId)?.value || '').replace(/\D/g, '');
  const p10 = raw.length >= 10 ? '90' + raw.slice(-10) : raw;
  const url = p10 ? `https://wa.me/${p10}` : 'https://wa.me/';
  window.open(url, '_blank');
}

function shareCurrentFormWhatsApp() {
  const r = collectRecordFromForm();
  const text = formatRecordShareText(r);
  window.open(`https://wa.me/?text=${encodeURIComponent(text)}`, '_blank');
}

function shareRecordWhatsApp(id) {
  const r = state.records.find((x) => x.id === id);
  if (!r) return;
  window.open(`https://wa.me/?text=${encodeURIComponent(formatRecordShareText(r))}`, '_blank');
}

function shareCurrentFormEmail() {
  const r = collectRecordFromForm();
  const to = state.settings.outlookEmail || '';
  const subj = `Teknik Destek Kaydı: ${r.sicilNo || ''} - ${r.musteriBayi || ''}`;
  const body = formatRecordShareText(r);
  window.location.href = `mailto:${encodeURIComponent(to)}?subject=${encodeURIComponent(subj)}&body=${encodeURIComponent(body)}`;
}

function formatRecordShareText(r) {
  return [
    `🎧 *TELEFON DESTEK SERVİS KAYDI*`,
    `🆔 *Sicil No:* ${r.sicilNo || '-'}`,
    `🏢 *Müşteri/Bayi:* ${r.musteriBayi || '-'}`,
    `👤 *Görüşülen:* ${r.gorusulen || '-'}`,
    `📞 *Telefon:* ${r.telefon || '-'}`,
    `📌 *Konu:* ${r.destekKonu || '-'}`,
    `🔍 *Tespit:* ${r.tespit || '-'}`,
    `🛠️ *İşlem:* ${r.islem || '-'}`,
    `⏱️ *Tarih/Süre:* ${r.basTarihi || '-'} (${r.sure || '00:00'})`,
    `👤 *Temsilci:* ${r.kullanici || '-'}`
  ].join('\n');
}

/* ============================================================================
 * 20. 36 SÜTUNLU GERÇEK MICROSOFT EXCEL (.XLSX) ÜRETİCİ (100% OFFLINE)
 * ============================================================================ */
function openExportExcelModal() {
  setExportRangePreset(30);
  openModal('exportExcelModal');
}

function setExportRangePreset(daysBack) {
  const end = new Date();
  const start = new Date();
  start.setDate(end.getDate() - daysBack);
  if (document.getElementById('exportStartDate')) {
    document.getElementById('exportStartDate').value = formatDateInputISO(start);
  }
  if (document.getElementById('exportEndDate')) {
    document.getElementById('exportEndDate').value = formatDateInputISO(end);
  }
}

function generateAndDownloadExcel() {
  const sIso = document.getElementById('exportStartDate')?.value;
  const eIso = document.getElementById('exportEndDate')?.value;
  let list = state.records;
  if (sIso && eIso) {
    const sMs = new Date(sIso + 'T00:00:00').getTime();
    const eMs = new Date(eIso + 'T23:59:59').getTime();
    list = state.records.filter((r) => {
      const ms = parseRecordDateMs(r.basTarihi);
      return ms === 0 || (ms >= sMs && ms <= eMs);
    });
  }
  exportRecordsToXlsx(list, `Telefon_Destek_36Sutun_${formatDateInputISO(new Date())}.xlsx`);
  closeModal('exportExcelModal');
}

function exportRecordsToXlsx(recordsList, filename) {
  const headers = [
    'ID', 'İletişim Türü', 'Sicil No', 'Müşteri / Bayi', 'Görüşülen Kişi',
    'Başlangıç Tarihi', 'Bitiş Tarihi', 'Süre', 'Temsilci Kodu', 'Temsilci Adı',
    'Destek Konusu', 'Tespit', 'Yapılan İşlem', 'Redmine No', 'Bağlantı ID',
    'Telefon 1', 'Telefon 2', 'E-Posta', 'Bekleyen Çağrı', 'AR-GE Gidiş',
    'AR-GE Gidiş Notu', 'AR-GE Dönüş', 'AR-GE Dönüş Notu', 'B2B Kayıt No', 'Arayan Türü',
    'YNOKC Versiyon', 'DynCash Versiyon', 'YN500 Versiyon', 'Arka Ofis Versiyon', 'Servis Kodu',
    'YN500 Sicil No', 'Arka Ofis ID', 'TCKN', 'Vergi No', 'Tespit Kodu', 'İşlem Kodu'
  ];

  const rows = [headers];
  recordsList.forEach((r, idx) => {
    rows.push([
      String(r.id || idx + 1),
      r.iletisimTuru || 'Telefon',
      r.sicilNo || '',
      r.musteriBayi || '',
      r.gorusulen || '',
      r.basTarihi || '',
      r.bitTarihi || '',
      r.sure || '',
      r.kullaniciKodu || '',
      r.kullanici || '',
      r.destekKonu || '',
      r.tespit || '',
      r.islem || '',
      r.redmine || '',
      r.bagId || '',
      r.telefon || '',
      r.telefon2 || '',
      r.mail || '',
      r.bekleyen || 'Hayır',
      r.argeGidis || 'Hayır',
      r.argeGidisNotu || '',
      r.argeDonus || 'Hayır',
      r.argeDonusNotu || '',
      r.b2bKayit || '',
      r.arayan || 'Müşteri',
      r.ynokcVersiyon || '',
      r.dynCashVersiyon || '',
      r.yn500Versiyon || '',
      r.arkaOfisVersiyon || '',
      r.servisKodu || '',
      r.yn500SicilNo || '',
      r.arkaOfisId || '',
      r.tckn || '',
      r.vergiNo || '',
      r.tespitKodu || '',
      r.islemKodu || ''
    ]);
  });

  const xlsxBlob = buildMinimalXlsxBlob(rows);
  triggerBlobDownload(xlsxBlob, filename || 'Destek_Raporu.xlsx');
  showToast(`📥 36 sütunlu Excel (.xlsx) dosyası (${recordsList.length} kayıt) indirildi.`, 'success');
}

function buildMinimalXlsxBlob(aoa) {
  const xmlEscape = (s) =>
    String(s ?? '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');

  const colName = (n) => {
    let s = '';
    while (n >= 0) {
      s = String.fromCharCode((n % 26) + 65) + s;
      n = Math.floor(n / 26) - 1;
    }
    return s;
  };

  let sheetRowsXml = '';
  for (let r = 0; r < aoa.length; r++) {
    const row = aoa[r];
    let cellsXml = '';
    for (let c = 0; c < row.length; c++) {
      const ref = `${colName(c)}${r + 1}`;
      cellsXml += `<c r="${ref}" t="inlineStr"><is><t>${xmlEscape(row[c])}</t></is></c>`;
    }
    sheetRowsXml += `<row r="${r + 1}">${cellsXml}</row>`;
  }

  const files = {
    '[Content_Types].xml': `<?xml version="1.0" encoding="UTF-8"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>`,
    '_rels/.rels': `<?xml version="1.0" encoding="UTF-8"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>`,
    'xl/workbook.xml': `<?xml version="1.0" encoding="UTF-8"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets><sheet name="Destek_Kayitlari" sheetId="1" r:id="rId1"/></sheets>
</workbook>`,
    'xl/_rels/workbook.xml.rels': `<?xml version="1.0" encoding="UTF-8"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
</Relationships>`,
    'xl/worksheets/sheet1.xml': `<?xml version="1.0" encoding="UTF-8"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetData>${sheetRowsXml}</sheetData>
</worksheet>`
  };

  return createUncompressedZipBlob(files);
}

function createUncompressedZipBlob(fileMap) {
  const enc = new TextEncoder();
  const entries = [];
  let offset = 0;

  const crcTable = new Uint32Array(256);
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    crcTable[n] = c >>> 0;
  }
  const crc32 = (bytes) => {
    let c = 0xffffffff;
    for (let i = 0; i < bytes.length; i++) c = crcTable[(c ^ bytes[i]) & 0xff] ^ (c >>> 8);
    return (c ^ 0xffffffff) >>> 0;
  };

  const localParts = [];
  for (const [name, content] of Object.entries(fileMap)) {
    const nameBytes = enc.encode(name);
    const dataBytes = enc.encode(content);
    const crc = crc32(dataBytes);

    const localHeader = new Uint8Array(30 + nameBytes.length);
    const dv = new DataView(localHeader.buffer);
    dv.setUint32(0, 0x04034b50, true);
    dv.setUint16(4, 20, true);
    dv.setUint32(14, crc, true);
    dv.setUint32(18, dataBytes.length, true);
    dv.setUint32(22, dataBytes.length, true);
    dv.setUint16(26, nameBytes.length, true);
    localHeader.set(nameBytes, 30);

    entries.push({ nameBytes, crc, size: dataBytes.length, offset });
    localParts.push(localHeader, dataBytes);
    offset += localHeader.length + dataBytes.length;
  }

  const centralParts = [];
  let centralSize = 0;
  for (const e of entries) {
    const cd = new Uint8Array(46 + e.nameBytes.length);
    const dv = new DataView(cd.buffer);
    dv.setUint32(0, 0x02014b50, true);
    dv.setUint16(4, 20, true);
    dv.setUint16(6, 20, true);
    dv.setUint32(16, e.crc, true);
    dv.setUint32(20, e.size, true);
    dv.setUint32(24, e.size, true);
    dv.setUint16(28, e.nameBytes.length, true);
    dv.setUint32(42, e.offset, true);
    cd.set(e.nameBytes, 46);
    centralParts.push(cd);
    centralSize += cd.length;
  }

  const eocd = new Uint8Array(22);
  const edv = new DataView(eocd.buffer);
  edv.setUint32(0, 0x06054b50, true);
  edv.setUint16(8, entries.length, true);
  edv.setUint16(10, entries.length, true);
  edv.setUint32(12, centralSize, true);
  edv.setUint32(16, offset, true);

  return new Blob([...localParts, ...centralParts, eocd], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  });
}

function triggerBlobDownload(blob, filename) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  setTimeout(() => {
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }, 500);
}

/* ============================================================================
 * 21. ÖRNEK VERİ YÜKLEME & GENEL YARDIMCILAR
 * ============================================================================ */
function loadSampleDemoData() {
  seedInitialSampleRecords(true);
  renderAllUI();
  showToast('✨ Örnek destek ve servis kayıtları yüklendi.', 'success');
}

function seedInitialSampleRecords(forceAppend = false) {
  if (!forceAppend && state.records.length > 0) return;
  const now = new Date();
  const d0 = formatDateTimeTR(now);
  const sample = [
    {
      id: Date.now() - 30000,
      iletisimTuru: 'Telefon',
      sicilNo: 'PC00102458',
      musteriBayi: 'MİGROS TİCARET A.Ş. - KADIKÖY ŞB.',
      gorusulen: 'SERKAN YILDIRIM',
      basTarihi: d0,
      bitTarihi: d0,
      sure: '04:15',
      kullaniciKodu: '6',
      kullanici: 'MURAT DEMİR',
      destekKonu: 'GÜN SONU / Z RAPORU ALINAMIYOR',
      tespit: 'AÇIK KALMIŞ SATIŞ FİŞİ NEDENİYLE Z RAPORU VERMİYOR',
      islem: 'AÇIK FİŞ İPTAL EDİLİP Z RAPORU ALDIRILDI',
      redmine: '#48219',
      bagId: '842 190 311',
      telefon: '05324108899',
      telefon2: '02163451122',
      mail: 'kadikoy@migros.com.tr',
      bekleyen: 'Hayır',
      argeGidis: 'Hayır',
      argeGidisNotu: '',
      argeDonus: 'Hayır',
      argeDonusNotu: '',
      b2bKayit: 'B2B-9041',
      arayan: 'Müşteri',
      ynokcVersiyon: 'v4.2.19',
      dynCashVersiyon: '2025.03',
      yn500Versiyon: 'v2.1.0',
      arkaOfisVersiyon: 'BO-3.12',
      servisKodu: 'SRV-34',
      yn500SicilNo: 'YN500192',
      arkaOfisId: 'BO-104',
      tckn: '',
      vergiNo: '6210049281',
      tespitKodu: 'T-02',
      islemKodu: 'I-05',
      isSyncedToSql: false
    },
    {
      id: Date.now() - 15000,
      iletisimTuru: 'Uzak Bağlantı',
      sicilNo: 'Beko00048912',
      musteriBayi: 'ÖZLEM MARKET GIDA LTD. ŞTİ.',
      gorusulen: 'HASAN KORKMAZ',
      basTarihi: d0,
      bitTarihi: d0,
      sure: '06:40',
      kullaniciKodu: '3',
      kullanici: 'AYŞE DEMİR',
      destekKonu: 'BANKA POS EŞLEŞTİRME (BKM TECHPOS)',
      tespit: 'BANKA UYGULAMASI GÜNCEL DEĞİL (PARAMETRE EKSİK)',
      islem: 'BKM TECHPOS PARAMETRE YÜKLEMESİ VE EŞLEŞTİRME YAPILDI',
      redmine: '',
      bagId: '519 402 881',
      telefon: '05442201530',
      telefon2: '',
      mail: 'info@ozlemmarket.com',
      bekleyen: 'Evet',
      argeGidis: 'Hayır',
      argeGidisNotu: '',
      argeDonus: 'Hayır',
      argeDonusNotu: '',
      b2bKayit: '',
      arayan: 'Bayi',
      ynokcVersiyon: 'v4.2.19',
      dynCashVersiyon: '2025.03',
      yn500Versiyon: '',
      arkaOfisVersiyon: 'BO-3.12',
      servisKodu: 'SRV-06',
      yn500SicilNo: '',
      arkaOfisId: '',
      tckn: '',
      vergiNo: '7190441102',
      tespitKodu: 'T-08',
      islemKodu: 'I-09',
      isSyncedToSql: false
    }
  ];

  state.records = [...sample, ...state.records];
  saveToStorage(STORAGE_KEYS.RECORDS, state.records);
}

function showToast(message, type = 'info') {
  const cont = document.getElementById('toastContainer');
  if (!cont) return;
  const item = document.createElement('div');
  item.className = `toast-item ${type}`;
  item.textContent = message;
  cont.appendChild(item);
  setTimeout(() => {
    item.style.opacity = '0';
    setTimeout(() => item.remove(), 250);
  }, 3600);
}

function escapeHtml(str) {
  return String(str ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function escapeJs(str) {
  return String(str ?? '')
    .replace(/\\/g, '\\\\')
    .replace(/'/g, "\\'");
}
