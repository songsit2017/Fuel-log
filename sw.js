const CACHE_NAME = "fuellog-pro-v4.3.2-google-login-ui-fix-20260726";
const ASSETS = [
  "./",
  "./index.html",
  "./manifest.json",
  "./icon-192.png",
  "./icon-512.png",
  "./oil-prices.json",
  "./app.css",
  "./app.js",
  "./addons/minimal-pro.css",
  "./addons/true-minimal-ui.css",
  "./addons/true-minimal-ui.js",
  "./addons/firebase-config.js",
  "./addons/family-pro.js",
  "./diagnostics.html",
  "./original-v2.html"
];
self.addEventListener("install", event => {
  event.waitUntil(caches.open(CACHE_NAME).then(cache => cache.addAll(ASSETS)).catch(() => {}));
  self.skipWaiting();
});
self.addEventListener("activate", event => {
  event.waitUntil(caches.keys().then(keys => Promise.all(keys.filter(key => key !== CACHE_NAME).map(key => caches.delete(key)))));
  self.clients.claim();
});
self.addEventListener("fetch", event => {
  if (event.request.method !== "GET") return;
  const url = new URL(event.request.url);
  const externalHosts = ["anthropic.com","overpass-api.de","overpass.kumi.systems","cdnjs.cloudflare.com","bangchak.co.th","accounts.google.com","googleapis.com","gstatic.com","cdn.jsdelivr.net"];
  if (externalHosts.some(host => url.hostname.includes(host))) return;
  if (event.request.mode === "navigate") {
    event.respondWith(fetch(event.request).then(response => {
      const clone = response.clone();
      caches.open(CACHE_NAME).then(cache => cache.put("./index.html", clone)).catch(() => {});
      return response;
    }).catch(() => caches.match("./index.html")));
    return;
  }
  event.respondWith(caches.match(event.request).then(cached => cached || fetch(event.request).then(response => {
    if (response && response.ok && !response.redirected) {
      const clone = response.clone();
      caches.open(CACHE_NAME).then(cache => cache.put(event.request, clone)).catch(() => {});
    }
    return response;
  })));
});
