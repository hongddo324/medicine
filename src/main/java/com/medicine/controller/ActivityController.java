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
     * 최근 활동 조회
     */
    @GetMapping
    public ResponseEntity<?> getRecentActivities(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        try {
            List<Activity> activities = activityService.getRecentActivities();

            List<Map<String, Object>> activityList = activities.stream().map(activity -> {
                Map<String, Object> activityMap = new HashMap<>();
                activityMap.put("id", activity.getId());
                activityMap.put("activityType", activity.getActivityType().name());
                activityMap.put("message", activity.getMessage());
                activityMap.put("referenceId", activity.getReferenceId());
                activityMap.put("isRead", activity.getIsRead());
                activityMap.put("createdAt", activity.getCreatedAt());
                activityMap.put("user", Map.of(
                    "id", activity.getUser().getId(),
                    "username", activity.getUser().getUsername(),
                    "displayName", activity.getUser().getDisplayName() != null ? activity.getUser().getDisplayName() : activity.getUser().getUsername(),
                    "profileImage", activity.getUser().getProfileImage() != null ? activity.getUser().getProfileImage() : ""
                ));
                return activityMap;
            }).toList();

            long unreadCount = activityService.getUnreadCount();

            return ResponseEntity.ok(Map.of(
                "activities", activityList,
                "unreadCount", unreadCount
            ));
        } catch (Exception e) {
            log.error("Failed to get activities", e);
            return ResponseEntity.status(500).body(Map.of("error", "활동 조회에 실패했습니다."));
        }
    }

    /**
     * 활동 읽음 처리
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
            activityService.markAsRead(activityId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.error("Failed to mark activity as read", e);
            return ResponseEntity.status(500).body(Map.of("error", "활동 읽음 처리에 실패했습니다."));
        }
    }

    /**
     * 모든 활동 읽음 처리
     */
    @PostMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        try {
            activityService.markAllAsRead();
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.error("Failed to mark all activities as read", e);
            return ResponseEntity.status(500).body(Map.of("error", "모든 활동 읽음 처리에 실패했습니다."));
        }
    }

    /**
     * 읽지 않은 활동 개수 조회
     */
    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        try {
            long unreadCount = activityService.getUnreadCount();
            return ResponseEntity.ok(Map.of("unreadCount", unreadCount));
        } catch (Exception e) {
            log.error("Failed to get unread count", e);
            return ResponseEntity.status(500).body(Map.of("error", "읽지 않은 활동 개수 조회에 실패했습니다."));
        }
    }
}
