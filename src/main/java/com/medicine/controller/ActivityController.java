package com.medicine.controller;

import com.medicine.model.Activity;
import com.medicine.model.User;
import com.medicine.service.ActivityService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/activity")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

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
            activityService.deleteActivity(activityId);
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
            activityService.deleteAllActivities();
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.error("Failed to delete all activities for user {}", user.getUsername(), e);
            return ResponseEntity.status(500).body(Map.of("error", "모든 활동 삭제에 실패했습니다."));
        }
    }
}
