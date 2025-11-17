package com.medicine.controller;

import com.medicine.model.User;
import com.medicine.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * FCM 푸시 알림 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
public class PushController {

    private final PushNotificationService pushNotificationService;

    /**
     * FCM 토큰 등록
     * 프론트엔드에서 Firebase Messaging으로 생성한 토큰을 받아 저장
     */
    @PostMapping("/token")
    public ResponseEntity<Map<String, Object>> registerToken(
            @RequestBody Map<String, String> request,
            HttpSession session) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "Unauthorized"));
        }

        String fcmToken = request.get("token");
        if (fcmToken == null || fcmToken.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Token is required"));
        }

        log.info("=== FCM Token Registration ===");
        log.info("User: {}", user.getUsername());
        log.info("Token: {}...", fcmToken.substring(0, Math.min(50, fcmToken.length())));

        try {
            pushNotificationService.registerToken(user.getUsername(), fcmToken);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "FCM token registered successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to register FCM token", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "error", "Failed to register token"));
        }
    }

    /**
     * FCM 토큰 삭제
     */
    @DeleteMapping("/token")
    public ResponseEntity<Map<String, Object>> unregisterToken(
            @RequestBody Map<String, String> request,
            HttpSession session) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "Unauthorized"));
        }

        String fcmToken = request.get("token");
        if (fcmToken == null || fcmToken.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Token is required"));
        }

        log.info("=== FCM Token Unregistration ===");
        log.info("User: {}", user.getUsername());
        log.info("Token: {}...", fcmToken.substring(0, Math.min(50, fcmToken.length())));

        try {
            pushNotificationService.unregisterToken(fcmToken);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "FCM token unregistered successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to unregister FCM token", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "error", "Failed to unregister token"));
        }
    }

    /**
     * 테스트용 알림 전송
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> sendTestNotification(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "Unauthorized"));
        }

        try {
            pushNotificationService.sendNotification(
                    user.getUsername(),
                    "테스트 알림",
                    "FCM 푸시 알림이 정상적으로 작동합니다!",
                    "/medicine",
                    Map.of("type", "test")
            );

            return ResponseEntity.ok(Map.of("success", true, "message", "Test notification sent"));

        } catch (Exception e) {
            log.error("Failed to send test notification", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "error", "Failed to send test notification"));
        }
    }

    /**
     * 디버그: 현재 등록된 토큰 수 확인
     */
    @GetMapping("/debug/tokens")
    public ResponseEntity<Map<String, Object>> getTokenInfo(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "Unauthorized"));
        }

        try {
            int tokenCount = pushNotificationService.getTokenCountForUser(user.getUsername());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("username", user.getUsername());
            response.put("tokenCount", tokenCount);
            response.put("message", tokenCount == 1 ? "정상 (1개)" : "경고! " + tokenCount + "개의 토큰이 등록되어 있습니다");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get token info", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "error", "Failed to get token info"));
        }
    }
}
