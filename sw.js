const CACHE_NAME = 'fuel-log-v2';
const ASSETS = [
  './',
  './index.html',
  './manifest.json',
  './icon-192.png',
  './icon-512.png',
];

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => cache.addAll(ASSETS)).catch(() => {})
  );
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(keys.filter((k) => k !== CACHE_NAME).map((k) => caches.delete(k)))
    )
  );
  self.clients.claim();
});

self.addEventListener('fetch', (event) => {
  const url = new URL(event.request.url);
  // Never cache API calls (Anthropic / Overpass / CDN libs) - always go straight to network
  if (url.hostname.includes('anthropic.com') || url.hostname.includes('overpass-api.de') || url.hostname.includes('cdnjs.cloudflare.com') || url.hostname.includes('chnwt.dev')) {
    return;
  }
  if (event.request.method !== 'GET') return;

  // Navigations (opening the app / home-screen shortcut): network first, fall back to cached shell.
  if (event.request.mode === 'navigate') {
    event.respondWith(
      fetch(event.request)
        .then((res) => {
          try {
            const resClone = res.clone();
            caches.open(CACHE_NAME).then((cache) => cache.put('./index.html', resClone)).catch(() => {});
          } catch (e) {}
          return res;
        })
        .catch(() => caches.match('./index.html').then((cached) => cached || caches.match('./')))
    );
    return;
  }

  event.respondWith(
    caches.match(event.request).then((cached) => {
      return (
        cached ||
        fetch(event.request)
          .then((res) => {
            if (res && res.ok && !res.redirected) {
              const resClone = res.clone();
              caches.open(CACHE_NAME).then((cache) => cache.put(event.request, resClone)).catch(() => {});
            }
            return res;
          })
          .catch(() => cached)
      );
    })
  );
});
