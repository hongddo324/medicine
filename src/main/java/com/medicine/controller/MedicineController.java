package com.medicine.controller;

import com.medicine.model.Activity;
import com.medicine.model.Comment;
import com.medicine.model.MedicineRecord;
import com.medicine.model.Role;
import com.medicine.model.User;
import com.medicine.service.ActivityService;
import com.medicine.service.CommentService;
import com.medicine.service.FileStorageService;
import com.medicine.service.MedicineService;
import com.medicine.service.PointService;
import com.medicine.service.PushNotificationService;
import com.medicine.service.UserService;
import com.medicine.model.PointHistory;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MedicineController {

    @Value("${app.version:1.0.0}")
    private String appVersion;

    private final MedicineService medicineService;
    private final CommentService commentService;
    private final FileStorageService fileStorageService;
    private final PushNotificationService pushNotificationService;
    private final PointService pointService;
    private final ActivityService activityService;
    private final UserService userService;

    @GetMapping("/")
    public String home(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        MedicineRecord morningRecord = medicineService.getTodayRecord(MedicineRecord.MedicineType.MORNING);
        MedicineRecord eveningRecord = medicineService.getTodayRecord(MedicineRecord.MedicineType.EVENING);
        List<Comment> comments = commentService.getAllComments();

        // user ID 2의 포인트를 모든 사용자에게 표시
        int displayPoints = 0;
        try {
            User targetUser = userService.findById(2L).orElse(null);
            if (targetUser != null) {
                displayPoints = targetUser.getPoints() != null ? targetUser.getPoints() : 0;
            }
        } catch (Exception e) {
            log.warn("Failed to get user ID 2 points: {}", e.getMessage());
        }

        model.addAttribute("user", user);
        model.addAttribute("displayPoints", displayPoints);
        model.addAttribute("morningRecord", morningRecord);
        model.addAttribute("eveningRecord", eveningRecord);
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("canTakeMedicine", user.getRole() == Role.FATHER);
        model.addAttribute("comments", comments);
        model.addAttribute("appVersion", appVersion);

        log.debug("Home page accessed by user: {}, morning: {}, evening: {}, displayPoints: {}",
            user.getUsername(), morningRecord.isTaken() ? "taken" : "not taken",
            eveningRecord.isTaken() ? "taken" : "not taken", displayPoints);

        return "medicine";
    }

    @PostMapping("/api/medicine/take")
    @ResponseBody
    public ResponseEntity<?> takeMedicine(@RequestParam String medicineType, HttpSession session) {
        User user = (User) session.getAttribute("user");

        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        if (user.getRole() != Role.FATHER) {
            log.warn("Unauthorized medicine take attempt by user: {} with role: {}",
                user.getUsername(), user.getRole());
            return ResponseEntity.status(403).body(Map.of("error", "약 복용 기록 권한이 없습니다."));
        }

        try {
            MedicineRecord.MedicineType type = MedicineRecord.MedicineType.valueOf(medicineType.toUpperCase());
            MedicineRecord record = medicineService.markAsTaken(user, type);

            // 포인트 적립 (FATHER 권한만)
            if (user.getRole() == Role.FATHER) {
                pointService.addPoints(user, 10, PointHistory.PointType.MEDICINE,
                    type.getDisplayName() + " 약 복용");
                log.info("Points added - User: {}, Points: +10, Type: MEDICINE", user.getUsername());
            }

            // 활동 기록 생성
            try {
                String message = user.getDisplayName() + "님이 " + type.getDisplayName() + " 약을 복용했습니다";
                activityService.createActivity(user, Activity.ActivityType.MEDICINE_TAKEN, message, record.getId());
                log.info("Activity created for medicine taken - User: {}, Type: {}", user.getUsername(), type);
            } catch (Exception e) {
                log.error("Failed to create activity for medicine taken", e);
            }

            log.info("Medicine taken - User: {}, Type: {}, Date: {}, Time: {}",
                user.getUsername(), type, record.getDate(), record.getTakenTime());

            // 약복용 알림 전송 (본인 제외)
            String notificationTitle = "💊 약 복용 알림";
            String notificationBody = user.getDisplayName() + "님이 " + type.getDisplayName() + " 약을 복용했습니다";
            Map<String, String> notificationData = Map.of(
                    "type", "medicine",
                    "medicineType", type.name(),
                    "userId", user.getUsername()
            );
            pushNotificationService.sendNotificationToAllUsersExcept(
                    user.getUsername(),
                    notificationTitle,
                    notificationBody,
                    "/medicine",
                    notificationData
            );
            log.info("FCM notification sent for medicine - User: {}, Type: {}", user.getUsername(), type);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("taken", true);
            response.put("takenTime", record.getTakenTime().toString());
            response.put("takenBy", record.getTakenBy() != null ? record.getTakenBy().getUsername() : null);
            response.put("medicineType", type.name());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "잘못된 약 복용 타입입니다."));
        }
    }

    @PostMapping("/api/medicine/cancel")
    @ResponseBody
    public ResponseEntity<?> cancelMedicine(@RequestParam String medicineType, HttpSession session) {
        User user = (User) session.getAttribute("user");

        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        if (user.getRole() != Role.FATHER) {
            log.warn("Unauthorized medicine cancel attempt by user: {} with role: {}",
                user.getUsername(), user.getRole());
            return ResponseEntity.status(403).body(Map.of("error", "약 복용 취소 권한이 없습니다."));
        }

        try {
            MedicineRecord.MedicineType type = MedicineRecord.MedicineType.valueOf(medicineType.toUpperCase());
            MedicineRecord record = medicineService.cancelTaken(user, type);

            log.info("Medicine cancelled - User: {}, Type: {}, Date: {}",
                user.getUsername(), type, record.getDate());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("taken", false);
            response.put("medicineType", type.name());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "잘못된 약 복용 타입입니다."));
        }
    }

    @GetMapping("/api/medicine/calendar/{year}/{month}")
    @ResponseBody
    public ResponseEntity<?> getCalendarData(@PathVariable int year,
                                            @PathVariable int month,
                                            HttpSession session) {
        User user = (User) session.getAttribute("user");

        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        Map<String, Object> calendarData = medicineService.getMonthCalendarData(year, month);

        log.debug("Calendar data requested by user: {} for {}-{}", user.getUsername(), year, month);

        return ResponseEntity.ok(calendarData);
    }

    // 댓글 목록 조회
    @GetMapping("/api/comments")
    @ResponseBody
    public ResponseEntity<?> getComments(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        List<Comment> comments = commentService.getAllComments();
        return ResponseEntity.ok(comments);
    }

    // 댓글 작성
    @PostMapping("/api/comments")
    @ResponseBody
    public ResponseEntity<?> createComment(@RequestBody Map<String, Object> requestBody, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        try {
            String content = (String) requestBody.get("content");
            String imageData = (String) requestBody.get("imageData");
            Object parentCommentIdObj = requestBody.get("parentCommentId");
            Long parentCommentId = null;
            if (parentCommentIdObj != null) {
                if (parentCommentIdObj instanceof Integer) {
                    parentCommentId = ((Integer) parentCommentIdObj).longValue();
                } else if (parentCommentIdObj instanceof Long) {
                    parentCommentId = (Long) parentCommentIdObj;
                }
            }

            Comment comment = commentService.createComment(content, imageData, user, parentCommentId);
            log.info("Comment created - User: {}, ID: {}, HasImage: {}, ParentId: {}",
                    user.getUsername(), comment.getId(), imageData != null, parentCommentId);

            // 댓글 작성 알림 전송 (작성자 본인 제외)
            String notificationTitle = "💬 새 댓글";
            String notificationBody = user.getDisplayName() + "님이 댓글을 남겼습니다: " +
                    (content.length() > 30 ? content.substring(0, 30) + "..." : content);
            Map<String, String> notificationData = Map.of(
                    "type", "comment",
                    "commentId", String.valueOf(comment.getId()),
                    "userId", user.getUsername()
            );
            pushNotificationService.sendNotificationToAllUsersExcept(
                    user.getUsername(),
                    notificationTitle,
                    notificationBody,
                    "/medicine",
                    notificationData
            );

            log.info("FCM notification sent for new comment - User: {}", user.getUsername());

            return ResponseEntity.ok(Map.of("success", true, "comment", comment));
        } catch (Exception e) {
            log.error("Failed to create comment for user: {}", user.getUsername(), e);
            return ResponseEntity.status(500).body(Map.of("error", "댓글 작성 중 오류가 발생했습니다."));
        }
    }

    // 댓글 좋아요 토글
    @PostMapping("/api/comments/{commentId}/like")
    @ResponseBody
    public ResponseEntity<?> toggleLike(@PathVariable Long commentId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        Comment comment = commentService.toggleLike(commentId, user.getId());
        if (comment == null) {
            return ResponseEntity.status(404).body(Map.of("error", "댓글을 찾을 수 없습니다."));
        }

        log.debug("Like toggled by user: {} on comment: {}", user.getUsername(), commentId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "likesCount", comment.getLikesCount(),
                "isLiked", comment.isLikedBy(user.getId())
        ));
    }

    // 댓글 삭제
    @DeleteMapping("/api/comments/{commentId}")
    @ResponseBody
    public ResponseEntity<?> deleteComment(@PathVariable Long commentId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        Comment comment = commentService.findById(commentId).orElse(null);
        if (comment == null) {
            return ResponseEntity.status(404).body(Map.of("error", "댓글을 찾을 수 없습니다."));
        }

        // 본인의 댓글이거나 관리자만 삭제 가능
        if (!comment.getUser().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("error", "삭제 권한이 없습니다."));
        }

        // Delete comment image if exists
        if (comment.getImageUrl() != null && !comment.getImageUrl().isEmpty()
                && comment.getImageUrl().startsWith("data:image")) {
            // Base64 이미지는 DB에만 저장되므로 삭제할 파일 없음
            log.debug("Comment has Base64 image - CommentId: {}", commentId);
        }

        commentService.deleteComment(commentId);
        log.info("Comment deleted - User: {}, CommentId: {}", user.getUsername(), commentId);

        return ResponseEntity.ok(Map.of("success", true));
    }

    // 오늘의 약 복용 상태 조회
    @GetMapping("/api/medicine/today")
    @ResponseBody
    public ResponseEntity<?> getTodayMedicineStatus(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        MedicineRecord morningRecord = medicineService.getTodayRecord(MedicineRecord.MedicineType.MORNING);
        MedicineRecord eveningRecord = medicineService.getTodayRecord(MedicineRecord.MedicineType.EVENING);

        Map<String, Object> response = new HashMap<>();
        response.put("morning", Map.of("taken", morningRecord.isTaken()));
        response.put("evening", Map.of("taken", eveningRecord.isTaken()));

        return ResponseEntity.ok(response);
    }
}
