const CACHE = 'fuellog-main-v7-7610';
const CORE = [
  './', './index.html', './styles.css?v=7.8.0', './app.js?v=7.8.0',
  './modules/settings.js', './modules/weather.js', './modules/ocr-client.js', './modules/fuel-metrics.js',
  './firebase-config.js?v=7.8.0', './manifest.json', './icon-192.png', './icon-512.png'
];

self.addEventListener('install', event => {
  event.waitUntil(caches.open(CACHE).then(cache => cache.addAll(CORE)).then(()=>self.skipWaiting()));
});

self.addEventListener('activate', event => {
  event.waitUntil(caches.keys()
    .then(keys => Promise.all(keys.filter(key => key !== CACHE).map(key => caches.delete(key))))
    .then(()=>self.clients.claim()));
});

self.addEventListener('fetch', event => {
  if(event.request.method !== 'GET') return;
  const url = new URL(event.request.url);

  // Never cache Firebase/Google responses, or third-party live-data APIs.
  if(url.hostname.includes('googleapis.com') || url.hostname.includes('gstatic.com') ||
     url.hostname.includes('overpass-api.de') || url.hostname.includes('chnwt.dev') ||
     url.hostname.includes('open-meteo.com') || url.hostname.includes('allorigins.win')){
    event.respondWith(fetch(event.request));
    return;
  }

  const isAppCode = event.request.mode === 'navigate' ||
    /\/(?:index\.html|app\.js|styles\.css|firebase-config\.js|modules\/.+\.js)$/.test(url.pathname);

  if(isAppCode){
    event.respondWith(
      fetch(event.request)
        .then(response => {
          const copy = response.clone();
          caches.open(CACHE).then(cache => cache.put(event.request, copy));
          return response;
        })
        .catch(()=>caches.match(event.request).then(r=>r||caches.match('./index.html')))
    );
    return;
  }

  event.respondWith(caches.match(event.request).then(cached => cached || fetch(event.request)));
});
