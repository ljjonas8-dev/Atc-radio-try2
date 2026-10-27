const CACHE='atc-live-v17';
const ASSETS=['./','./index.html','./manifest.webmanifest','./impressum.html','./datenschutz.html'];
self.addEventListener('install',event=>{
 event.waitUntil(caches.open(CACHE).then(cache=>cache.addAll(ASSETS)).then(()=>self.skipWaiting()));
});
self.addEventListener('activate',event=>{
 event.waitUntil(caches.keys().then(keys=>Promise.all(keys.filter(key=>key.startsWith('atc-live-')&&key!==CACHE).map(key=>caches.delete(key)))).then(()=>self.clients.claim()));
});
self.addEventListener('fetch',event=>{
 if(event.request.method!=='GET') return;
 const url=new URL(event.request.url);
 if(url.origin!==self.location.origin) return;
 if(event.request.mode==='navigate' || url.pathname.endsWith('/index.html')){
  event.respondWith(fetch(event.request).then(response=>{
   if(response.ok) caches.open(CACHE).then(cache=>cache.put(event.request,response.clone()));
   return response;
  }).catch(()=>caches.open(CACHE).then(cache=>cache.match(event.request)).then(cached=>cached||caches.match('./index.html'))));
  return;
 }
 event.respondWith(caches.open(CACHE).then(async cache=>{
  const cached=await cache.match(event.request);
  if(cached) return cached;
  const response=await fetch(event.request);
  if(response.ok) cache.put(event.request,response.clone());
  return response;
 }));
});
