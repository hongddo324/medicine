package com.medicine.controller;

import com.medicine.model.User;
import com.medicine.service.PushNotificationService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * FCM 푸시 알림 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final PushNotificationService pushNotificationService;

    /**
     * FCM 토큰 등록
     */
    @PostMapping("/register-token")
    public ResponseEntity<?> registerToken(@RequestBody Map<String, String> request, HttpSession session) {
        try {
            User user = (User) session.getAttribute("user");

            if (user == null) {
                return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "인증되지 않은 사용자입니다."
                ));
            }

            String token = request.get("token");
            String platform = request.get("platform");
            String deviceInfo = request.get("deviceInfo");

            if (token == null || token.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "토큰이 비어있습니다."
                ));
            }

            // FCM 토큰 등록 (userId = username)
            pushNotificationService.registerToken(user.getUsername(), token);

            log.info("[FCM] 토큰 등록 완료 - User: {}, Platform: {}, Device: {}",
                    user.getUsername(), platform, deviceInfo);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "FCM 토큰이 성공적으로 등록되었습니다."
            ));

        } catch (Exception e) {
            log.error("[FCM] 토큰 등록 실패", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "토큰 등록 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * FCM 토큰 삭제
     */
    @DeleteMapping("/unregister-token")
    public ResponseEntity<?> unregisterToken(@RequestBody Map<String, String> request, HttpSession session) {
        try {
            User user = (User) session.getAttribute("user");

            if (user == null) {
                return ResponseEntity.status(401).body(Map.of(
                        "success", false,
                        "message", "인증되지 않은 사용자입니다."
                ));
            }

            String token = request.get("token");

            if (token == null || token.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "토큰이 비어있습니다."
                ));
            }

            pushNotificationService.unregisterToken(token);

            log.info("[FCM] 토큰 삭제 완료 - User: {}", user.getUsername());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "FCM 토큰이 성공적으로 삭제되었습니다."
            ));

        } catch (Exception e) {
            log.error("[FCM] 토큰 삭제 실패", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "토큰 삭제 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 테스트용 알림 전송
     */
    @PostMapping("/test")
    public ResponseEntity<?> sendTestNotification(HttpSession session) {
        try {
            User user = (User) session.getAttribute("user");

            if (user == null) {
                return ResponseEntity.status(401).body(Map.of(
                        "success", false,
                        "message", "인증되지 않은 사용자입니다."
                ));
            }

            pushNotificationService.sendNotification(
                    user.getUsername(),
                    "테스트 알림",
                    "FCM 푸시 알림이 정상적으로 작동합니다!"
            );

            log.info("[FCM] 테스트 알림 전송 완료 - User: {}", user.getUsername());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "테스트 알림이 전송되었습니다."
            ));

        } catch (Exception e) {
            log.error("[FCM] 테스트 알림 전송 실패", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "테스트 알림 전송 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 사용자의 등록된 토큰 개수 조회 (디버깅용)
     */
    @GetMapping("/token-count")
    public ResponseEntity<?> getTokenCount(HttpSession session) {
        try {
            User user = (User) session.getAttribute("user");

            if (user == null) {
                return ResponseEntity.status(401).body(Map.of(
                        "success", false,
                        "message", "인증되지 않은 사용자입니다."
                ));
            }

            int count = pushNotificationService.getTokenCountForUser(user.getUsername());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "count", count
            ));

        } catch (Exception e) {
            log.error("[FCM] 토큰 개수 조회 실패", e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "토큰 개수 조회 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }
}
