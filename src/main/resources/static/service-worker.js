const CACHE_NAME = 'medicine-app-v2'; // 버전 업데이트로 강제 재설치

// Install Service Worker
self.addEventListener('install', (event) => {
  console.log('Service Worker v2 installing...');
  // 즉시 활성화
  self.skipWaiting();
});

// Activate Service Worker
self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((cacheNames) => {
      return Promise.all(
        cacheNames.map((cacheName) => {
          if (cacheName !== CACHE_NAME) {
            console.log('Deleting old cache:', cacheName);
            return caches.delete(cacheName);
          }
        })
      );
    })
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

// Push notification event
self.addEventListener('push', (event) => {
  const data = event.data ? event.data.json() : {};
  const title = data.title || '약복용 알림';
  const options = {
    body: data.body || '알림이 도착했습니다.',
    icon: '/icons/icon-192x192.png',
    badge: '/icons/icon-72x72.png',
    vibrate: [200, 100, 200],
    tag: data.tag || 'medicine-notification',
    requireInteraction: true,
    data: {
      url: data.url || '/medicine'
    }
  };

  event.waitUntil(
    self.registration.showNotification(title, options)
  );
});

// Notification click event
self.addEventListener('notificationclick', (event) => {
  event.notification.close();

  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true })
      .then((clientList) => {
        // If a window is already open, focus it
        for (let i = 0; i < clientList.length; i++) {
          const client = clientList[i];
          if (client.url === event.notification.data.url && 'focus' in client) {
            return client.focus();
          }
        }
        // Otherwise, open a new window
        if (clients.openWindow) {
          return clients.openWindow(event.notification.data.url);
        }
      })
  );
});
