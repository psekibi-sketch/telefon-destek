/* ============================================================================
 * TELEFON DESTEK — AKILLI SÜRÜM KONTROLLÜ & VERİ KORUMALI SERVICE WORKER (sw.js)
 * ============================================================================
 * - GitHub Pages üzerinden yapılan güncellemelerde yalnızca uygulama kabuğu
 *   (HTML/CSS/JS) önbelleği yenilenir.
 * - Kullanıcının localStorage, IndexedDB ve Korunan Kullanıcı Kasası
 *   (telefon-destek-user-vault-permanent) içindeki ayarları (SIP, kullanıcı adı,
 *   SQL kullanıcı/şifre) ve offline çağrı kayıtları ASLA silinmez ve sıfırlanmaz.
 * ============================================================================ */

const APP_VERSION = '5.2.0';
const SHELL_CACHE_PREFIX = 'telefon-destek-shell-';
const SHELL_CACHE_NAME = `${SHELL_CACHE_PREFIX}v${APP_VERSION}`;
const PROTECTED_USER_VAULT_CACHE = 'telefon-destek-user-vault-permanent';

const OFFLINE_ASSETS = [
  './',
  './index.html',
  './styles.css',
  './app.js',
  './manifest.json',
  './icon.svg'
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(SHELL_CACHE_NAME).then(async (cache) => {
      // Yeni sürüm dosyalarını sunucudan taze olarak çekip yeni sürüm önbelleğine yaz
      for (const assetUrl of OFFLINE_ASSETS) {
        try {
          const freshResponse = await fetch(assetUrl, { cache: 'reload' });
          if (freshResponse && freshResponse.ok) {
            await cache.put(assetUrl, freshResponse);
          } else {
            await cache.add(assetUrl);
          }
        } catch (_) {
          try {
            await cache.add(assetUrl);
          } catch (__) {}
        }
      }
    })
  );
  // Yeni Service Worker sürümünü beklemeden devreye al
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    (async () => {
      const keys = await caches.keys();
      await Promise.all(
        keys.map((key) => {
          // KORUMA KURALI: Kullanıcının ayar ve veri kasasına (PROTECTED_USER_VAULT_CACHE) ASLA DOKUNMA!
          if (key === PROTECTED_USER_VAULT_CACHE) {
            return Promise.resolve(false);
          }
          // Sadece eski uygulama kabuğu (shell) önbelleklerini temizle
          const isOldShellCache =
            (key.startsWith(SHELL_CACHE_PREFIX) || key.startsWith('telefon-destek-offline-pwa-')) &&
            key !== SHELL_CACHE_NAME;

          if (isOldShellCache) {
            return caches.delete(key);
          }
          return Promise.resolve(false);
        })
      );

      await self.clients.claim();

      // Açık olan tüm sekmelere / PWA pencerelerine yeni sürüm bilgisini gönder
      // (İstemci tarafı localStorage verilerini koruyarak sürüm geçişini tamamlar)
      const clientList = await self.clients.matchAll({ type: 'window', includeUncontrolled: true });
      for (const client of clientList) {
        client.postMessage({
          type: 'SW_VERSION_UPDATED',
          version: APP_VERSION,
          preserveLocalStorage: true
        });
      }
    })()
  );
});

self.addEventListener('message', (event) => {
  const data = event.data || {};
  if (data.type === 'SKIP_WAITING') {
    self.skipWaiting();
  } else if (data.type === 'GET_VERSION') {
    if (event.source) {
      event.source.postMessage({
        type: 'SW_CURRENT_VERSION',
        version: APP_VERSION
      });
    }
  }
});

self.addEventListener('fetch', (event) => {
  const req = event.request;
  const url = new URL(req.url);

  // Ofis İçi Wi-Fi Sync API istekleri (/api/sync-calls, /api/fetch-calls, /api/test-db-connection, /api/search-customers)
  // her zaman doğrudan yerel ağa (network) gitmeli, asla önbellekten dönmemelidir.
  if (url.pathname.includes('/api/') || req.method !== 'GET') {
    return;
  }

  // HTML / Navigasyon isteklerinde Network-First (güncellemeleri anında yakalamak için),
  // internet yoksa anında Offline Cache'ten yanıt dön.
  if (req.mode === 'navigate' || url.pathname.endsWith('/index.html') || url.pathname.endsWith('/')) {
    event.respondWith(
      fetch(req)
        .then((networkResponse) => {
          if (networkResponse && networkResponse.status === 200 && networkResponse.type === 'basic') {
            const clone = networkResponse.clone();
            caches.open(SHELL_CACHE_NAME).then((cache) => cache.put(req, clone));
          }
          return networkResponse;
        })
        .catch(async () => {
          const cached = await caches.match(req, { ignoreSearch: true });
          return cached || caches.match('./index.html');
        })
    );
    return;
  }

  // Diğer tüm statik dosyalar (app.js, styles.css, icon.svg, manifest.json) için
  // Stale-While-Revalidate (Anında açılış + arka planda sessiz güncelleme)
  event.respondWith(
    caches.match(req, { ignoreSearch: true }).then((cachedResponse) => {
      const fetchPromise = fetch(req)
        .then((networkResponse) => {
          if (networkResponse && networkResponse.status === 200 && networkResponse.type === 'basic') {
            const clone = networkResponse.clone();
            caches.open(SHELL_CACHE_NAME).then((cache) => cache.put(req, clone));
          }
          return networkResponse;
        })
        .catch(() => null);

      return cachedResponse || fetchPromise || caches.match('./index.html');
    })
  );
});
