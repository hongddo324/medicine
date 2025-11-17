package com.medicine.controller;

import com.medicine.model.Comment;
import com.medicine.model.MedicineRecord;
import com.medicine.model.Role;
import com.medicine.model.User;
import com.medicine.service.CommentService;
import com.medicine.service.FileStorageService;
import com.medicine.service.MedicineService;
import com.medicine.service.PushNotificationService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final MedicineService medicineService;
    private final CommentService commentService;
    private final FileStorageService fileStorageService;
    private final PushNotificationService pushNotificationService;

    @GetMapping("/")
    public String home(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        MedicineRecord morningRecord = medicineService.getTodayRecord(MedicineRecord.MedicineType.MORNING);
        MedicineRecord eveningRecord = medicineService.getTodayRecord(MedicineRecord.MedicineType.EVENING);
        List<Comment> comments = commentService.getAllComments();

        model.addAttribute("user", user);
        model.addAttribute("morningRecord", morningRecord);
        model.addAttribute("eveningRecord", eveningRecord);
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("canTakeMedicine", user.getRole() == Role.FATHER);
        model.addAttribute("comments", comments);

        log.debug("Home page accessed by user: {}, morning: {}, evening: {}",
            user.getUsername(), morningRecord.isTaken() ? "taken" : "not taken",
            eveningRecord.isTaken() ? "taken" : "not taken");

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
            MedicineRecord record = medicineService.markAsTaken(user.getUsername(), type);

            log.info("Medicine taken - User: {}, Type: {}, Date: {}, Time: {}",
                user.getUsername(), type, record.getDate(), record.getTakenTime());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("taken", true);
            response.put("takenTime", record.getTakenTime().toString());
            response.put("takenBy", record.getTakenBy());
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
            MedicineRecord record = medicineService.cancelTaken(user.getUsername(), type);

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
        return ResponseEntity.ok(Map.of("comments", comments, "currentUserId", user.getId()));
    }

    // 댓글 작성
    @PostMapping("/api/comments")
    @ResponseBody
    public ResponseEntity<?> createComment(
            @RequestParam String content,
            @RequestParam(required = false) MultipartFile image,
            @RequestParam(required = false) String parentCommentId,
            HttpSession session) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        try {
            String imageUrl = null;
            if (image != null && !image.isEmpty()) {
                // Generate a temporary comment ID for storing the image
                String tempCommentId = "comment_" + System.currentTimeMillis();
                imageUrl = fileStorageService.storeCommentImage(image, tempCommentId);
                log.info("Comment image stored - User: {}, Path: {}, Size: {} bytes",
                        user.getUsername(), imageUrl, image.getSize());
            }

            Comment comment = commentService.createComment(content, imageUrl, user, parentCommentId);
            log.info("Comment created - User: {}, ID: {}, HasImage: {}",
                    user.getUsername(), comment.getId(), imageUrl != null);

            // 댓글 작성 알림 전송 (작성자 본인 제외)
            String notificationTitle = "💬 새 댓글";
            String notificationBody = user.getDisplayName() + "님이 댓글을 남겼습니다: " +
                    (content.length() > 30 ? content.substring(0, 30) + "..." : content);
            Map<String, String> notificationData = Map.of(
                    "type", "comment",
                    "commentId", comment.getId(),
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
        } catch (IOException e) {
            log.error("Failed to process comment image for user: {}", user.getUsername(), e);
            return ResponseEntity.status(500).body(Map.of("error", "이미지 처리 중 오류가 발생했습니다."));
        }
    }

    // 댓글 좋아요 토글
    @PostMapping("/api/comments/{commentId}/like")
    @ResponseBody
    public ResponseEntity<?> toggleLike(@PathVariable String commentId, HttpSession session) {
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
    public ResponseEntity<?> deleteComment(@PathVariable String commentId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        Comment comment = commentService.findById(commentId).orElse(null);
        if (comment == null) {
            return ResponseEntity.status(404).body(Map.of("error", "댓글을 찾을 수 없습니다."));
        }

        // 본인의 댓글이거나 관리자만 삭제 가능
        if (!comment.getUserId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("error", "삭제 권한이 없습니다."));
        }

        // Delete comment image if exists
        if (comment.getImageUrl() != null && !comment.getImageUrl().isEmpty()
                && comment.getImageUrl().startsWith("/files/")) {
            fileStorageService.deleteFile(comment.getImageUrl());
            log.info("Comment image deleted - CommentId: {}, Path: {}", commentId, comment.getImageUrl());
        }

        commentService.deleteComment(commentId);
        log.info("Comment deleted - User: {}, CommentId: {}", user.getUsername(), commentId);

        return ResponseEntity.ok(Map.of("success", true));
    }
}
