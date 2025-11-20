/**
 * Firebase Initialization & FCM Token Management
 *
 * 이 파일은 Firebase를 초기화하고 FCM 토큰을 발급/등록/갱신합니다.
 */

// Firebase SDK 동적 로드
let firebaseApp = null;
let messaging = null;
let currentToken = null;

/**
 * Firebase 초기화
 */
async function initializeFirebase() {
    try {
        console.log('[FCM] Firebase 초기화 시작...');

        // Firebase SDK 로드 확인
        if (typeof firebase === 'undefined') {
            console.error('[FCM] Firebase SDK가 로드되지 않았습니다.');
            return false;
        }

        // Firebase 앱 초기화
        if (!firebase.apps.length) {
            firebaseApp = firebase.initializeApp(window.FIREBASE_CONFIG);
            console.log('[FCM] Firebase 앱 초기화 완료');
        } else {
            firebaseApp = firebase.apps[0];
            console.log('[FCM] 기존 Firebase 앱 사용');
        }

        // Messaging 인스턴스 생성
        if (firebase.messaging.isSupported()) {
            messaging = firebase.messaging();
            console.log('[FCM] Firebase Messaging 지원됨');

            // 포그라운드 메시지 수신 처리
            messaging.onMessage((payload) => {
                console.log('[FCM] 포그라운드 메시지 수신:', payload);
                handleForegroundMessage(payload);
            });

            return true;
        } else {
            console.warn('[FCM] 이 브라우저는 Firebase Messaging을 지원하지 않습니다.');
            return false;
        }
    } catch (error) {
        console.error('[FCM] Firebase 초기화 실패:', error);
        return false;
    }
}

/**
 * 포그라운드 메시지 처리
 */
function handleForegroundMessage(payload) {
    const { title, body, icon } = payload.notification || {};
    const data = payload.data || {};

    // 브라우저 알림 표시
    if (Notification.permission === 'granted') {
        const notification = new Notification(title || '새 알림', {
            body: body || '',
            icon: icon || '/icons/icon-192x192.png',
            badge: '/icons/icon-72x72.png',
            tag: data.type || 'default',
            data: data,
            requireInteraction: false
        });

        notification.onclick = function(event) {
            event.preventDefault();
            window.focus();

            // Activity 타입에 따라 탭 전환
            if (data.activityType && data.referenceId) {
                handleNotificationClick(data.activityType, data.referenceId);
            }

            notification.close();
        };
    }

    // 활동알림 UI 업데이트 (WebSocket과 동일한 효과)
    if (typeof updateActivityNotifications === 'function') {
        updateActivityNotifications();
    }
}

/**
 * 알림 클릭 처리 (탭 전환)
 */
function handleNotificationClick(activityType, referenceId) {
    switch (activityType) {
        case 'WISH_ADDED':
        case 'SCHEDULE_ADDED':
            switchTab('wishTab');
            // TODO: 위시 상세 페이지 열기
            break;
        case 'DAILY_POST':
        case 'DAILY_COMMENT':
        case 'DAILY_LIKE':
            switchTab('dailyTab');
            // TODO: 일상 상세 페이지 열기
            break;
        case 'MEDICINE_TAKEN':
        case 'MEAL_UPLOADED':
            switchTab('healthTab');
            break;
        case 'COMMENT':
        case 'COMMENT_REPLY':
            switchTab('homeTab');
            break;
        case 'PROFILE_UPDATED':
            switchTab('profileTab');
            break;
        default:
            switchTab('activityTab');
    }
}

/**
 * FCM 토큰 요청 및 등록
 */
async function requestNotificationPermissionAndGetToken() {
    try {
        console.log('[FCM] 알림 권한 요청 시작...');

        // Firebase 초기화 확인
        if (!messaging) {
            const initialized = await initializeFirebase();
            if (!initialized) {
                console.error('[FCM] Firebase 초기화 실패');
                return null;
            }
        }

        // 브라우저 알림 권한 요청
        const permission = await Notification.requestPermission();
        console.log('[FCM] 알림 권한:', permission);

        if (permission !== 'granted') {
            console.warn('[FCM] 알림 권한이 거부되었습니다.');
            return null;
        }

        // Service Worker 등록 확인
        if (!('serviceWorker' in navigator)) {
            console.error('[FCM] Service Worker를 지원하지 않는 브라우저입니다.');
            return null;
        }

        // Service Worker 등록 대기
        const registration = await navigator.serviceWorker.ready;
        console.log('[FCM] Service Worker 준비 완료');

        // FCM 토큰 발급
        const token = await messaging.getToken({
            vapidKey: window.VAPID_KEY,
            serviceWorkerRegistration: registration
        });

        if (token) {
            console.log('[FCM] 토큰 발급 성공:', token.substring(0, 20) + '...');
            currentToken = token;

            // 서버에 토큰 등록
            await registerTokenToServer(token);

            return token;
        } else {
            console.warn('[FCM] 토큰 발급 실패');
            return null;
        }
    } catch (error) {
        console.error('[FCM] 토큰 발급 오류:', error);

        // 권한 거부 시 사용자에게 안내
        if (error.code === 'messaging/permission-blocked') {
            showToast('알림 권한이 차단되었습니다. 브라우저 설정에서 알림을 허용해주세요.', 'warning');
        } else if (error.code === 'messaging/token-subscribe-failed') {
            showToast('알림 구독에 실패했습니다. VAPID 키를 확인해주세요.', 'error');
        }

        return null;
    }
}

/**
 * 서버에 토큰 등록
 */
async function registerTokenToServer(token) {
    try {
        console.log('[FCM] 서버에 토큰 등록 중...');

        const response = await fetch('/api/notifications/register-token', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                token: token,
                platform: 'WEB',
                deviceInfo: navigator.userAgent
            })
        });

        if (response.ok) {
            console.log('[FCM] 서버에 토큰 등록 완료');
            return true;
        } else {
            const error = await response.text();
            console.error('[FCM] 서버 토큰 등록 실패:', error);
            return false;
        }
    } catch (error) {
        console.error('[FCM] 서버 토큰 등록 오류:', error);
        return false;
    }
}

/**
 * 토큰 갱신 처리
 */
function setupTokenRefreshListener() {
    if (!messaging) {
        return;
    }

    messaging.onTokenRefresh(async () => {
        try {
            console.log('[FCM] 토큰 갱신 이벤트 발생');
            const registration = await navigator.serviceWorker.ready;
            const newToken = await messaging.getToken({
                vapidKey: window.VAPID_KEY,
                serviceWorkerRegistration: registration
            });

            if (newToken) {
                console.log('[FCM] 새 토큰 발급:', newToken.substring(0, 20) + '...');
                currentToken = newToken;
                await registerTokenToServer(newToken);
            }
        } catch (error) {
            console.error('[FCM] 토큰 갱신 실패:', error);
        }
    });
}

/**
 * 알림 권한 상태 확인
 */
function checkNotificationPermission() {
    if (!('Notification' in window)) {
        console.warn('[FCM] 이 브라우저는 알림을 지원하지 않습니다.');
        return 'unsupported';
    }

    return Notification.permission; // 'default', 'granted', 'denied'
}

/**
 * 알림 허용 버튼 UI 표시/숨김
 */
function updateNotificationButtonUI() {
    const permission = checkNotificationPermission();
    const notificationButton = document.getElementById('enableNotificationsBtn');

    if (!notificationButton) {
        return;
    }

    if (permission === 'granted') {
        notificationButton.style.display = 'none';
    } else if (permission === 'denied') {
        notificationButton.textContent = '알림이 차단되었습니다';
        notificationButton.disabled = true;
    } else {
        notificationButton.style.display = 'block';
    }
}

/**
 * 페이지 로드 시 자동 초기화
 */
window.addEventListener('load', async () => {
    console.log('[FCM] 페이지 로드 - Firebase 초기화 시작');

    // Firebase 초기화
    const initialized = await initializeFirebase();
    if (!initialized) {
        console.warn('[FCM] Firebase 초기화 실패 - FCM 비활성화');
        return;
    }

    // 알림 권한 확인
    const permission = checkNotificationPermission();
    console.log('[FCM] 현재 알림 권한:', permission);

    // UI 업데이트
    updateNotificationButtonUI();

    // 이미 권한이 있으면 자동으로 토큰 발급
    if (permission === 'granted') {
        console.log('[FCM] 알림 권한 있음 - 토큰 자동 발급');
        await requestNotificationPermissionAndGetToken();
    }

    // 토큰 갱신 리스너 설정
    setupTokenRefreshListener();

    // Service Worker 메시지 수신 (알림 클릭 처리)
    if ('serviceWorker' in navigator) {
        navigator.serviceWorker.addEventListener('message', (event) => {
            if (event.data && event.data.type === 'NOTIFICATION_CLICK') {
                console.log('[FCM] 알림 클릭 메시지 수신:', event.data);
                handleNotificationClick(event.data.activityType, event.data.referenceId);
            }
        });
    }
});

// 전역 함수로 노출
window.initializeFirebase = initializeFirebase;
window.requestNotificationPermissionAndGetToken = requestNotificationPermissionAndGetToken;
window.checkNotificationPermission = checkNotificationPermission;
