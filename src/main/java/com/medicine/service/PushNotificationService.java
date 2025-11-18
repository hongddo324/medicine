package com.medicine.service;

import com.google.firebase.messaging.*;
import com.medicine.model.FcmToken;
import com.medicine.repository.FcmTokenRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Firebase Cloud Messaging 푸시 알림 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationService {

    private final FcmTokenRepository fcmTokenRepository;

    @Value("${firebase.enabled:false}")
    private boolean firebaseEnabled;

    @PostConstruct
    public void init() {
        log.info("=== Firebase Cloud Messaging Service Initialized ===");
        log.info("Firebase Enabled: {}", firebaseEnabled);
    }

    /**
     * FCM 토큰 등록
     * 사용자당 하나의 토큰만 유지 (중복 알림 방지)
     */
    public void registerToken(String userId, String fcmToken) {
        if (!firebaseEnabled) {
            log.warn("Firebase is disabled. Token not registered.");
            return;
        }

        // 기존 토큰 모두 삭제 (같은 사용자의 모든 이전 토큰 제거)
        List<FcmToken> existingTokens = fcmTokenRepository.findByUserId(userId);
        if (!existingTokens.isEmpty()) {
            log.info("🔍 Found {} existing token(s) for user: {}", existingTokens.size(), userId);
            for (FcmToken existingToken : existingTokens) {
                fcmTokenRepository.delete(existingToken.getToken());
                log.info("🗑️ Removed old FCM token for user: {} (token: {}...)",
                    userId, existingToken.getToken().substring(0, Math.min(20, existingToken.getToken().length())));
            }
        }

        // 새 토큰 저장 (사용자당 정확히 하나의 토큰만 유지)
        FcmToken token = new FcmToken(userId, fcmToken);
        fcmTokenRepository.save(token);

        // 등록 확인
        int currentTokenCount = fcmTokenRepository.findByUserId(userId).size();
        log.info("✅ FCM token registered for user: {} (Current count: {})", userId, currentTokenCount);

        if (currentTokenCount > 1) {
            log.error("⚠️ WARNING: User {} still has {} tokens after cleanup! This should not happen!",
                userId, currentTokenCount);
        }
    }

    /**
     * FCM 토큰 삭제
     */
    public void unregisterToken(String fcmToken) {
        fcmTokenRepository.delete(fcmToken);
        log.info("🗑️ FCM token unregistered: {}", fcmToken);
    }

    /**
     * 사용자의 등록된 토큰 개수 조회 (디버깅용)
     */
    public int getTokenCountForUser(String userId) {
        List<FcmToken> tokens = fcmTokenRepository.findByUserId(userId);
        int count = tokens.size();
        log.info("📊 User {} has {} FCM token(s) registered", userId, count);
        return count;
    }

    /**
     * 특정 사용자에게 알림 전송
     */
    public void sendNotification(String userId, String title, String body) {
        sendNotification(userId, title, body, "/medicine", null);
    }

    /**
     * 특정 사용자에게 알림 전송 (URL, 데이터 포함)
     */
    public void sendNotification(String userId, String title, String body, String url, Map<String, String> data) {
        if (!firebaseEnabled) {
            log.debug("Firebase disabled - skipping notification for user: {}", userId);
            return;
        }

        List<FcmToken> tokens = fcmTokenRepository.findByUserId(userId);

        if (tokens.isEmpty()) {
            log.debug("No FCM tokens found for user: {}", userId);
            return;
        }

        log.info("📤 Sending notification to user: {} (Found {} token(s))", userId, tokens.size());
        if (tokens.size() > 1) {
            log.warn("⚠️ WARNING: User {} has {} tokens! This will cause duplicate notifications!", userId, tokens.size());
        }

        int successCount = 0;
        for (FcmToken fcmToken : tokens) {
            try {
                sendToToken(fcmToken.getToken(), title, body, url, data);
                successCount++;

                // 마지막 사용 시간 업데이트
                fcmToken.setLastUsedAt(System.currentTimeMillis());
                fcmTokenRepository.save(fcmToken);
            } catch (Exception e) {
                log.error("Failed to send notification to user: {}", userId, e);
            }
        }

        log.info("📊 Notification sent to user: {} ({}/{} tokens succeeded)", userId, successCount, tokens.size());
    }

    /**
     * 특정 사용자를 제외한 모든 사용자에게 알림 전송
     */
    public void sendNotificationToAllUsersExcept(String excludedUserId, String title, String body, String url, Map<String, String> data) {
        if (!firebaseEnabled) {
            log.debug("Firebase disabled - skipping broadcast notification");
            return;
        }

        List<FcmToken> allTokens = fcmTokenRepository.findAll();
        int sent = 0;
        int failed = 0;

        for (FcmToken fcmToken : allTokens) {
            if (fcmToken.getUserId().equals(excludedUserId)) {
                continue;
            }

            try {
                sendToToken(fcmToken.getToken(), title, body, url, data);
                fcmToken.setLastUsedAt(System.currentTimeMillis());
                fcmTokenRepository.save(fcmToken);
                sent++;
            } catch (Exception e) {
                failed++;
                log.error("Failed to send to user: {}", fcmToken.getUserId(), e);
            }
        }

        log.info("📢 Broadcast complete - Sent: {}, Failed: {}", sent, failed);
    }

    /**
     * 모든 사용자에게 알림 전송
     */
    public void sendNotificationToAllUsers(String title, String body) {
        sendNotificationToAllUsers(title, body, "/medicine", null);
    }

    /**
     * 모든 사용자에게 알림 전송 (URL, 데이터 포함)
     */
    public void sendNotificationToAllUsers(String title, String body, String url, Map<String, String> data) {
        if (!firebaseEnabled) {
            log.debug("Firebase disabled - skipping broadcast notification");
            return;
        }

        List<FcmToken> allTokens = fcmTokenRepository.findAll();
        int sent = 0;
        int failed = 0;

        for (FcmToken fcmToken : allTokens) {
            try {
                sendToToken(fcmToken.getToken(), title, body, url, data);
                fcmToken.setLastUsedAt(System.currentTimeMillis());
                fcmTokenRepository.save(fcmToken);
                sent++;
            } catch (Exception e) {
                failed++;
                log.error("Failed to send to user: {}", fcmToken.getUserId(), e);
            }
        }

        log.info("📢 Broadcast to all - Sent: {}, Failed: {}", sent, failed);
    }

    /**
     * FCM 토큰으로 직접 알림 전송
     */
    private void sendToToken(String fcmToken, String title, String body, String url, Map<String, String> customData) {
        try {
            // 알림 페이로드 구성 (Android용 기본 Notification)
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            // 웹푸시 설정 (클릭 시 이동할 URL 포함)
            WebpushNotification webpushNotification = WebpushNotification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .setIcon("/icons/icon-192x192.png")
                    .setBadge("/icons/badge-72x72.png")
                    .build();

            WebpushConfig.Builder webpushConfigBuilder = WebpushConfig.builder()
                    .setNotification(webpushNotification);

            if (url != null && !url.isEmpty()) {
                webpushConfigBuilder.setFcmOptions(WebpushFcmOptions.builder()
                        .setLink(url)
                        .build());
            }

            // iOS용 APNS 설정 (iOS 푸시 알림 지원)
            Aps aps = Aps.builder()
                    .setAlert(ApsAlert.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .setSound("default")
                    .setBadge(1)
                    .setContentAvailable(true)
                    .build();

            ApnsConfig apnsConfig = ApnsConfig.builder()
                    .setAps(aps)
                    .putHeader("apns-priority", "10")
                    .build();

            // 데이터 페이로드 구성
            Map<String, String> data = new HashMap<>();
            data.put("title", title);
            data.put("body", body);
            data.put("url", url != null ? url : "/medicine");
            data.put("timestamp", String.valueOf(System.currentTimeMillis()));

            if (customData != null) {
                data.putAll(customData);
            }

            // FCM 메시지 구성 (모든 플랫폼 지원)
            Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(notification)  // Android/Web용 기본 알림
                    .setWebpushConfig(webpushConfigBuilder.build())  // Web용
                    .setApnsConfig(apnsConfig)  // iOS용
                    .putAllData(data)
                    .build();

            // FCM 전송
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("✅ FCM notification sent successfully - Message ID: {}", response);

        } catch (FirebaseMessagingException fme) {
            log.error("❌ FCM error: {} - {}", fme.getMessagingErrorCode(), fme.getMessage());

            // 잘못된 토큰 제거
            if (fme.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED ||
                fme.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT) {
                fcmTokenRepository.delete(fcmToken);
                log.info("🗑️ Removed invalid FCM token");
            }
            throw new RuntimeException("Failed to send FCM notification", fme);

        } catch (Exception e) {
            log.error("❌ Unexpected error sending FCM notification", e);
            throw new RuntimeException("Failed to send FCM notification", e);
        }
    }
}
