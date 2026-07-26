const CACHE_NAME = "fuellog-pro-v5.1-full-restore-20260726";
const APP_SHELL = [
  "./", "./index.html", "./manifest.json", "./icon-192.png", "./icon-512.png",
  "./oil-prices.json", "./firebase-config.js", "./addons/minimal-pro.css",
  "./addons/true-minimal-ui.css", "./addons/true-minimal-ui.js",
  "./addons/family-pro.js", "./vendor/jszip.min.js", "./diagnostics.html"
];
self.addEventListener("install", event => {
  event.waitUntil(caches.open(CACHE_NAME).then(c=>c.addAll(APP_SHELL)).catch(()=>{}));
  self.skipWaiting();
});
self.addEventListener("activate", event => {
  event.waitUntil(caches.keys().then(keys=>Promise.all(keys.filter(k=>k!==CACHE_NAME).map(k=>caches.delete(k)))));
  self.clients.claim();
});
self.addEventListener("fetch", event => {
  if(event.request.method!=="GET") return;
  const url=new URL(event.request.url);
  if(url.origin!==self.location.origin) return;
  const isCode=/\.(?:html|js|css)$/i.test(url.pathname)||url.pathname.endsWith('/Fuel-log/')||url.pathname.endsWith('/Fuel-log');
  if(event.request.mode==="navigate"||isCode){
    event.respondWith(fetch(event.request,{cache:'no-store'}).then(res=>{
      if(res.ok){const copy=res.clone();caches.open(CACHE_NAME).then(c=>c.put(event.request,copy)).catch(()=>{});}
      return res;
    }).catch(()=>caches.match(event.request).then(x=>x||caches.match('./index.html'))));
    return;
  }
  event.respondWith(caches.match(event.request).then(cached=>cached||fetch(event.request).then(res=>{
    if(res.ok){const copy=res.clone();caches.open(CACHE_NAME).then(c=>c.put(event.request,copy)).catch(()=>{});}
    return res;
  })));
});
