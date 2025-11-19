package com.medicine.controller;

import com.medicine.model.Daily;
import com.medicine.model.DailyComment;
import com.medicine.model.User;
import com.medicine.service.DailyService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/daily")
@RequiredArgsConstructor
public class DailyController {

    private final DailyService dailyService;

    /**
     * 모든 일상 게시물 조회
     */
    @GetMapping
    public ResponseEntity<?> getAllDailies(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        List<Daily> dailies = dailyService.getAllDailies();

        // 각 게시물에 현재 사용자의 좋아요 여부 추가
        List<Map<String, Object>> dailyList = dailies.stream().map(daily -> {
            Map<String, Object> dailyMap = new HashMap<>();
            dailyMap.put("id", daily.getId());
            dailyMap.put("content", daily.getContent());
            dailyMap.put("mediaUrl", daily.getMediaUrl());
            dailyMap.put("mediaType", daily.getMediaType());
            dailyMap.put("likesCount", daily.getLikesCount());
            dailyMap.put("createdAt", daily.getCreatedAt());
            dailyMap.put("user", Map.of(
                "id", daily.getUser().getId(),
                "username", daily.getUser().getUsername(),
                "displayName", daily.getUser().getDisplayName() != null ? daily.getUser().getDisplayName() : daily.getUser().getUsername(),
                "profileImage", daily.getUser().getProfileImage() != null ? daily.getUser().getProfileImage() : ""
            ));
            dailyMap.put("isLiked", dailyService.isLikedByUser(daily.getId(), user.getId()));
            return dailyMap;
        }).toList();

        return ResponseEntity.ok(dailyList);
    }

    /**
     * 일상 게시물 작성
     */
    @PostMapping
    public ResponseEntity<?> createDaily(
            @RequestParam String content,
            @RequestParam(required = false) MultipartFile media,
            HttpSession session) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        try {
            Daily daily = dailyService.createDaily(user, content, media);
            log.info("Daily post created - User: {}, ID: {}", user.getUsername(), daily.getId());

            return ResponseEntity.ok(Map.of("success", true, "daily", daily));
        } catch (Exception e) {
            log.error("Failed to create daily post", e);
            return ResponseEntity.status(500).body(Map.of("error", "게시물 작성에 실패했습니다."));
        }
    }

    /**
     * 일상 게시물 삭제
     */
    @DeleteMapping("/{dailyId}")
    public ResponseEntity<?> deleteDaily(
            @PathVariable Long dailyId,
            HttpSession session) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        try {
            dailyService.deleteDaily(dailyId, user);
            return ResponseEntity.ok(Map.of("success", true, "message", "게시물이 삭제되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to delete daily post", e);
            return ResponseEntity.status(500).body(Map.of("error", "게시물 삭제에 실패했습니다."));
        }
    }

    /**
     * 좋아요 토글
     */
    @PostMapping("/{dailyId}/like")
    public ResponseEntity<?> toggleLike(
            @PathVariable Long dailyId,
            HttpSession session) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        try {
            boolean liked = dailyService.toggleLike(dailyId, user);
            Daily daily = dailyService.getDailyById(dailyId)
                    .orElseThrow(() -> new IllegalArgumentException("게시물을 찾을 수 없습니다."));

            return ResponseEntity.ok(Map.of(
                "success", true,
                "liked", liked,
                "likesCount", daily.getLikesCount()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to toggle like", e);
            return ResponseEntity.status(500).body(Map.of("error", "좋아요 처리에 실패했습니다."));
        }
    }

    /**
     * 댓글 조회
     */
    @GetMapping("/{dailyId}/comments")
    public ResponseEntity<?> getComments(
            @PathVariable Long dailyId,
            HttpSession session) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        try {
            List<DailyComment> comments = dailyService.getComments(dailyId);

            List<Map<String, Object>> commentList = comments.stream().map(comment -> {
                Map<String, Object> commentMap = new HashMap<>();
                commentMap.put("id", comment.getId());
                commentMap.put("content", comment.getContent());
                commentMap.put("createdAt", comment.getCreatedAt());
                commentMap.put("user", Map.of(
                    "id", comment.getUser().getId(),
                    "username", comment.getUser().getUsername(),
                    "displayName", comment.getUser().getDisplayName() != null ? comment.getUser().getDisplayName() : comment.getUser().getUsername(),
                    "profileImage", comment.getUser().getProfileImage() != null ? comment.getUser().getProfileImage() : ""
                ));
                commentMap.put("parentCommentId", comment.getParentComment() != null ? comment.getParentComment().getId() : null);
                return commentMap;
            }).toList();

            return ResponseEntity.ok(commentList);
        } catch (Exception e) {
            log.error("Failed to get comments", e);
            return ResponseEntity.status(500).body(Map.of("error", "댓글 조회에 실패했습니다."));
        }
    }

    /**
     * 댓글 추가
     */
    @PostMapping("/{dailyId}/comments")
    public ResponseEntity<?> addComment(
            @PathVariable Long dailyId,
            @RequestParam String content,
            @RequestParam(required = false) Long parentCommentId,
            HttpSession session) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        try {
            DailyComment comment = dailyService.addComment(dailyId, user, content, parentCommentId);

            Map<String, Object> commentMap = new HashMap<>();
            commentMap.put("id", comment.getId());
            commentMap.put("content", comment.getContent());
            commentMap.put("createdAt", comment.getCreatedAt());
            commentMap.put("user", Map.of(
                "id", comment.getUser().getId(),
                "username", comment.getUser().getUsername(),
                "displayName", comment.getUser().getDisplayName() != null ? comment.getUser().getDisplayName() : comment.getUser().getUsername(),
                "profileImage", comment.getUser().getProfileImage() != null ? comment.getUser().getProfileImage() : ""
            ));
            commentMap.put("parentCommentId", comment.getParentComment() != null ? comment.getParentComment().getId() : null);

            return ResponseEntity.ok(Map.of("success", true, "comment", commentMap));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to add comment", e);
            return ResponseEntity.status(500).body(Map.of("error", "댓글 추가에 실패했습니다."));
        }
    }

    /**
     * 댓글 삭제
     */
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<?> deleteComment(
            @PathVariable Long commentId,
            HttpSession session) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        try {
            dailyService.deleteComment(commentId, user);
            return ResponseEntity.ok(Map.of("success", true, "message", "댓글이 삭제되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to delete comment", e);
            return ResponseEntity.status(500).body(Map.of("error", "댓글 삭제에 실패했습니다."));
        }
    }
}
