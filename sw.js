const CACHE = 'fuellog-v5-clean-4';
const CORE = [
  './', './index.html', './styles.css?v=5.0.3', './app.js?v=5.0.3',
  './firebase-config.js?v=5.0.3', './manifest.json', './icon-192.png', './icon-512.png'
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

  // Never cache Firebase/Google responses.
  if(url.hostname.includes('googleapis.com') || url.hostname.includes('gstatic.com')){
    event.respondWith(fetch(event.request));
    return;
  }

  const isAppCode = event.request.mode === 'navigate' ||
    /\/(?:index\.html|app\.js|styles\.css|firebase-config\.js)$/.test(url.pathname);

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
