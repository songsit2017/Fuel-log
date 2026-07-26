const VERSION = '7.8.3';
const CACHE = `fuellog-main-v${VERSION}`;
const CORE = [
  './', './index.html', `./styles.css?v=${VERSION}`, `./app.js?v=${VERSION}`,
  './modules/settings.js', './modules/weather.js', './modules/ocr-client.js', './modules/fuel-metrics.js',
  `./firebase-config.js?v=${VERSION}`, './manifest.json', './icon-192.png', './icon-512.png'
];

self.addEventListener('install', event => {
  event.waitUntil((async()=>{
    const cache=await caches.open(CACHE);
    await Promise.all(CORE.map(async url=>{
      const response=await fetch(new Request(url,{cache:'reload'}));
      if(!response.ok)throw new Error(`Precache failed: ${url}`);
      await cache.put(url,response);
    }));
    await self.skipWaiting();
  })());
});

self.addEventListener('activate', event => {
  event.waitUntil((async()=>{
    const keys=await caches.keys();
    await Promise.all(keys.filter(key=>key!==CACHE).map(key=>caches.delete(key)));
    await self.clients.claim();
    const clients=await self.clients.matchAll({type:'window',includeUncontrolled:true});
    clients.forEach(client=>client.postMessage({type:'APP_UPDATED',version:VERSION}));
  })());
});

self.addEventListener('message', event => {
  if(event.data?.type==='SKIP_WAITING')self.skipWaiting();
  if(event.data?.type==='CLEAR_APP_CACHE'){
    event.waitUntil((async()=>{
      const keys=await caches.keys();
      await Promise.all(keys.map(key=>caches.delete(key)));
      event.source?.postMessage({type:'APP_CACHE_CLEARED'});
    })());
  }
});

self.addEventListener('fetch', event => {
  if(event.request.method!=='GET')return;
  const url=new URL(event.request.url);
  if(url.origin!==self.location.origin||
     url.hostname.includes('googleapis.com')||url.hostname.includes('gstatic.com')||
     url.hostname.includes('overpass-api.de')||url.hostname.includes('chnwt.dev')||
     url.hostname.includes('open-meteo.com')||url.hostname.includes('allorigins.win')){
    event.respondWith(fetch(event.request));
    return;
  }

  const isAppCode=event.request.mode==='navigate'||
    /\/(?:index\.html|app\.js|styles\.css|sw\.js|firebase-config\.js|manifest\.json|modules\/.+\.js)$/.test(url.pathname);

  if(isAppCode){
    event.respondWith((async()=>{
      try{
        const response=await fetch(new Request(event.request,{cache:'no-store'}));
        if(response.ok){
          const cache=await caches.open(CACHE);
          await cache.put(event.request,response.clone());
        }
        return response;
      }catch{
        return (await caches.match(event.request,{ignoreSearch:true}))||
          (await caches.match('./index.html',{ignoreSearch:true}))||
          Response.error();
      }
    })());
    return;
  }

  event.respondWith(caches.match(event.request).then(cached=>cached||fetch(event.request).then(response=>{
    if(response.ok)caches.open(CACHE).then(cache=>cache.put(event.request,response.clone()));
    return response;
  })));
});
