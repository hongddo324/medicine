package com.medicine.controller;

import com.medicine.model.Activity;
import com.medicine.model.User;
import com.medicine.service.ActivityService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@RestController
@RequestMapping("/api/activity")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    // 사용자별 SSE Emitter 관리 (userId -> List<SseEmitter>)
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> userEmitters = new HashMap<>();

    /**
     * 최근 활동 조회 (사용자별 읽음 상태 포함)
     */
    @GetMapping
    public ResponseEntity<?> getRecentActivities(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        try {
            // 사용자별 읽음 상태를 포함한 활동 조회
            List<Map<String, Object>> activities = activityService.getRecentActivitiesForUser(user);
            long unreadCount = activityService.getUnreadCount(user);

            return ResponseEntity.ok(Map.of(
                "activities", activities,
                "unreadCount", unreadCount
            ));
        } catch (Exception e) {
            log.error("Failed to get activities for user {}", user.getUsername(), e);
            return ResponseEntity.status(500).body(Map.of("error", "활동 조회에 실패했습니다."));
        }
    }

    /**
     * 사용자별 활동 읽음 처리
     */
    @PostMapping("/{activityId}/read")
    public ResponseEntity<?> markAsRead(
            @PathVariable Long activityId,
            HttpSession session) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        try {
            activityService.markAsRead(user, activityId);
            long unreadCount = activityService.getUnreadCount(user);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "unreadCount", unreadCount
            ));
        } catch (Exception e) {
            log.error("Failed to mark activity {} as read for user {}", activityId, user.getUsername(), e);
            return ResponseEntity.status(500).body(Map.of("error", "활동 읽음 처리에 실패했습니다."));
        }
    }

    /**
     * 사용자별 모든 활동 읽음 처리
     */
    @PostMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        try {
            activityService.markAllAsRead(user);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "unreadCount", 0
            ));
        } catch (Exception e) {
            log.error("Failed to mark all activities as read for user {}", user.getUsername(), e);
            return ResponseEntity.status(500).body(Map.of("error", "모든 활동 읽음 처리에 실패했습니다."));
        }
    }

    /**
     * 사용자별 읽지 않은 활동 개수 조회
     */
    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        try {
            long unreadCount = activityService.getUnreadCount(user);
            return ResponseEntity.ok(Map.of("unreadCount", unreadCount));
        } catch (Exception e) {
            log.error("Failed to get unread count for user {}", user.getUsername(), e);
            return ResponseEntity.status(500).body(Map.of("error", "읽지 않은 활동 개수 조회에 실패했습니다."));
        }
    }

    /**
     * 활동 삭제
     */
    @DeleteMapping("/{activityId}")
    public ResponseEntity<?> deleteActivity(
            @PathVariable Long activityId,
            HttpSession session) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        try {
            activityService.deleteActivity(activityId, user);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.error("Failed to delete activity {} for user {}", activityId, user.getUsername(), e);
            return ResponseEntity.status(500).body(Map.of("error", "활동 삭제에 실패했습니다."));
        }
    }

    /**
     * 모든 활동 삭제
     */
    @DeleteMapping("/delete-all")
    public ResponseEntity<?> deleteAllActivities(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        try {
            activityService.deleteAllActivities(user);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.error("Failed to delete all activities for user {}", user.getUsername(), e);
            return ResponseEntity.status(500).body(Map.of("error", "모든 활동 삭제에 실패했습니다."));
        }
    }

    /**
     * SSE 연결 엔드포인트 (실시간 알림 스트림)
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamActivities(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            log.warn("Unauthorized SSE connection attempt");
            return null;
        }

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // 무제한 타임아웃
        Long userId = user.getId();

        // 사용자별 emitter 리스트 생성 또는 가져오기
        userEmitters.putIfAbsent(userId, new CopyOnWriteArrayList<>());
        userEmitters.get(userId).add(emitter);

        log.info("SSE connection established for user: {}", user.getUsername());

        // 연결 완료 이벤트 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(Map.of("message", "SSE connected", "userId", userId)));
        } catch (IOException e) {
            log.error("Failed to send connection event", e);
        }

        // 연결 종료 처리
        emitter.onCompletion(() -> {
            log.info("SSE connection completed for user: {}", user.getUsername());
            removeEmitter(userId, emitter);
        });

        emitter.onTimeout(() -> {
            log.info("SSE connection timeout for user: {}", user.getUsername());
            removeEmitter(userId, emitter);
        });

        emitter.onError((e) -> {
            log.error("SSE connection error for user: {}", user.getUsername(), e);
            removeEmitter(userId, emitter);
        });

        return emitter;
    }

    /**
     * Emitter 제거 헬퍼 메서드
     */
    private void removeEmitter(Long userId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                userEmitters.remove(userId);
            }
        }
    }

    /**
     * 특정 사용자에게 SSE 이벤트 전송
     */
    public void sendActivityToUser(Long userId, Map<String, Object> activityData) {
        CopyOnWriteArrayList<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            log.debug("No active SSE connections for user: {}", userId);
            return;
        }

        List<SseEmitter> deadEmitters = new java.util.ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("activity")
                        .data(activityData));
                log.debug("Sent activity event to user: {}", userId);
            } catch (IOException e) {
                log.warn("Failed to send SSE event to user: {}", userId, e);
                deadEmitters.add(emitter);
            }
        }

        // 전송 실패한 emitter 제거
        deadEmitters.forEach(emitter -> removeEmitter(userId, emitter));
    }
}
