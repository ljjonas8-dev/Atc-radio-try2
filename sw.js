const CACHE='atc-live-v2';
const ASSETS=['./','./index.html','./manifest.webmanifest'];
self.addEventListener('install',event=>{
 event.waitUntil(caches.open(CACHE).then(cache=>cache.addAll(ASSETS)).then(()=>self.skipWaiting()));
});
self.addEventListener('activate',event=>{
 event.waitUntil(caches.keys().then(keys=>Promise.all(keys.filter(key=>key.startsWith('atc-live-')&&key!==CACHE).map(key=>caches.delete(key)))).then(()=>self.clients.claim()));
});
self.addEventListener('fetch',event=>{
 if(event.request.method!=='GET') return;
 event.respondWith(caches.open(CACHE).then(async cache=>{
  const cached=await cache.match(event.request);
  if(cached) return cached;
  try {
   const response=await fetch(event.request);
   if(response.ok && new URL(event.request.url).origin===self.location.origin) cache.put(event.request,response.clone());
   return response;
  } catch(error) {
   if(event.request.mode==='navigate') return cache.match('./index.html');
   throw error;
  }
 }));
});
