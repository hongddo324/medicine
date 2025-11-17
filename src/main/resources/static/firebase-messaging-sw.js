// Firebase Cloud Messaging Service Worker
// 이 파일은 백그라운드에서 푸시 알림을 수신하기 위해 필요합니다

importScripts('https://www.gstatic.com/firebasejs/10.7.1/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/10.7.1/firebase-messaging-compat.js');

// Firebase 설정
const firebaseConfig = {
    apiKey: "AIzaSyC4oP4zGjT1W5gVsWu0kyOoHVpbieOtVpM",
    authDomain: "mdedicine.firebaseapp.com",
    projectId: "mdedicine",
    storageBucket: "mdedicine.firebasestorage.app",
    messagingSenderId: "659585584078",
    appId: "1:659585584078:web:bf9f90d5a2fdcd42f5b99f",
    measurementId: "G-GETB4S7XM4"
};

// Firebase 초기화
firebase.initializeApp(firebaseConfig);

// Messaging 인스턴스 가져오기
const messaging = firebase.messaging();

// 백그라운드 메시지 처리
messaging.onBackgroundMessage((payload) => {
    console.log('[firebase-messaging-sw.js] Received background message:', payload);

    // 알림 데이터 추출
    const notificationTitle = payload.data.title || payload.notification?.title || '약복용 알림';
    const notificationOptions = {
        body: payload.data.body || payload.notification?.body || '새로운 알림이 있습니다',
        icon: payload.data.icon || '/icons/icon-192x192.png',
        badge: payload.data.badge || '/icons/badge-72x72.png',
        tag: payload.data.type || 'default',
        data: {
            url: payload.data.url || '/medicine',
            ...payload.data
        },
        requireInteraction: false,
        vibrate: [200, 100, 200]
    };

    // 알림 표시
    return self.registration.showNotification(notificationTitle, notificationOptions);
});

// 알림 클릭 처리
self.addEventListener('notificationclick', (event) => {
    console.log('[firebase-messaging-sw.js] Notification clicked:', event);

    event.notification.close();

    // 알림 클릭 시 이동할 URL (절대 경로로 변경하여 404 방지)
    const targetUrl = event.notification.data?.url || '/medicine';
    const urlToOpen = targetUrl.startsWith('http') ? targetUrl : 'https://www.hongddo.top' + targetUrl;

    // 클라이언트 창 열기 또는 포커스
    event.waitUntil(
        clients.matchAll({ type: 'window', includeUncontrolled: true })
            .then((clientList) => {
                // 이미 열려있는 창이 있으면 해당 창으로 이동
                for (const client of clientList) {
                    if (client.url.includes('hongddo.top') && 'focus' in client) {
                        return client.focus().then(focusedClient => {
                            if ('navigate' in focusedClient) {
                                return focusedClient.navigate(urlToOpen);
                            }
                        });
                    }
                }
                // 열려있는 창이 없으면 새 창 열기
                if (clients.openWindow) {
                    return clients.openWindow(urlToOpen);
                }
            })
    );
});

console.log('[firebase-messaging-sw.js] Service Worker loaded successfully');
