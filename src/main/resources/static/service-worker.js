const CACHE_NAME = 'medicine-app-v3'; // 버전 업데이트로 강제 재설치

// Install Service Worker
self.addEventListener('install', (event) => {
  console.log('Service Worker v3 installing...');
  // 즉시 활성화
  self.skipWaiting();
});

// Activate Service Worker
self.addEventListener('activate', (event) => {
  event.waitUntil(
    (async () => {
      // 이전 캐시 삭제
      const cacheNames = await caches.keys();
      await Promise.all(
        cacheNames.map((cacheName) => {
          if (cacheName !== CACHE_NAME) {
            console.log('Deleting old cache:', cacheName);
            return caches.delete(cacheName);
          }
        })
      );

      // 모든 클라이언트에 새 버전 알림
      const allClients = await self.clients.matchAll({ includeUncontrolled: true });
      for (const client of allClients) {
        client.postMessage({ type: 'NEW_VERSION_READY' });
      }

      console.log('Service Worker v3 activated, notified clients');
    })()
  );
  self.clients.claim();
});

// Fetch event - Network First, then Cache
self.addEventListener('fetch', (event) => {
  const requestUrl = event.request.url;

  // chrome-extension, data:, blob: 등 지원하지 않는 스킴 무시
  if (!requestUrl.startsWith('http://') && !requestUrl.startsWith('https://')) {
    console.log('Ignoring non-http request:', requestUrl);
    return;
  }

  // Chrome 확장 프로그램 리소스 무시
  if (requestUrl.includes('chrome-extension://')) {
    return;
  }

  event.respondWith(
    fetch(event.request)
      .then((response) => {
        // 유효한 응답만 캐시
        if (response && response.ok && response.type === 'basic') {
          try {
            const responseToCache = response.clone();
            caches.open(CACHE_NAME)
              .then((cache) => {
                cache.put(event.request, responseToCache)
                  .catch((err) => {
                    console.log('Cache put failed:', err.message);
                  });
              });
          } catch (err) {
            console.log('Cache error:', err.message);
          }
        }

        return response;
      })
      .catch(() => {
        return caches.match(event.request);
      })
  );
});

// 클라이언트로부터 메시지 수신
self.addEventListener('message', (event) => {
  if (event.data && event.data.type === 'SKIP_WAITING') {
    self.skipWaiting();
  }
});

// Push notification은 firebase-messaging-sw.js에서 처리합니다.
// 중복 알림을 방지하기 위해 여기서는 처리하지 않습니다.
