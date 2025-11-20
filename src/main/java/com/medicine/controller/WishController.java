package com.medicine.controller;

import com.medicine.model.Daily;
import com.medicine.model.User;
import com.medicine.model.Wish;
import com.medicine.model.WishSchedule;
import com.medicine.service.DailyService;
import com.medicine.service.WishService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/wish")
@RequiredArgsConstructor
public class WishController {

    private final WishService wishService;
    private final DailyService dailyService;

    /**
     * 모든 위시리스트 조회 (모든 유저 공유)
     */
    @GetMapping
    public ResponseEntity<?> getAllWishes(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        try {
            // 모든 유저의 위시리스트 조회 (일상탭처럼 공유)
            List<Wish> wishes = wishService.getAllWishes();

            List<Map<String, Object>> wishList = wishes.stream().map(wish -> {
                Map<String, Object> wishMap = new HashMap<>();
                wishMap.put("id", wish.getId());
                wishMap.put("title", wish.getTitle());
                wishMap.put("description", wish.getDescription());
                wishMap.put("category", wish.getCategory());
                wishMap.put("latitude", wish.getLatitude());
                wishMap.put("longitude", wish.getLongitude());
                wishMap.put("address", wish.getAddress());
                wishMap.put("imageUrl", wish.getImageUrl());
                wishMap.put("completed", wish.getCompleted());
                wishMap.put("dailyId", wish.getDailyId());

                // 연결된 일상 게시물 제목 추가
                if (wish.getDailyId() != null) {
                    try {
                        dailyService.getDailyById(wish.getDailyId()).ifPresent(daily -> {
                            if (daily.getContent() != null) {
                                wishMap.put("dailyTitle", daily.getContent());
                            }
                        });
                    } catch (Exception e) {
                        log.warn("Failed to get daily title for wish {}: {}", wish.getId(), e.getMessage());
                    }
                }

                wishMap.put("createdAt", wish.getCreatedAt());
                wishMap.put("user", Map.of(
                    "id", wish.getUser().getId(),
                    "username", wish.getUser().getUsername(),
                    "displayName", wish.getUser().getDisplayName() != null ? wish.getUser().getDisplayName() : wish.getUser().getUsername()
                ));
                return wishMap;
            }).toList();

            return ResponseEntity.ok(wishList);
        } catch (Exception e) {
            log.error("Failed to get wishes", e);
            return ResponseEntity.status(500).body(Map.of("error", "위시리스트 조회에 실패했습니다."));
        }
    }

    /**
     * 위시 생성
     */
    @PostMapping
    public ResponseEntity<?> createWish(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam String category,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) MultipartFile image,
            @RequestParam(required = false) Long dailyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            HttpSession session) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        try {
            Wish.WishCategory wishCategory = Wish.WishCategory.valueOf(category);
            Wish wish = wishService.createWish(user, title, description, wishCategory, latitude, longitude, address, image, dailyId);

            // 날짜 범위로 일정 추가
            if (startDate != null && endDate != null) {
                LocalDateTime current = startDate;
                while (!current.isAfter(endDate)) {
                    wishService.createSchedule(wish.getId(), current, title, description);
                    current = current.plusDays(1);
                }
            } else if (startDate != null) {
                wishService.createSchedule(wish.getId(), startDate, title, description);
            }

            log.info("Wish created - User: {}, ID: {}", user.getUsername(), wish.getId());
            return ResponseEntity.ok(Map.of("success", true, "wish", wish));
        } catch (Exception e) {
            log.error("Failed to create wish", e);
            return ResponseEntity.status(500).body(Map.of("error", "위시 추가에 실패했습니다."));
        }
    }

    /**
     * 위시 수정
     */
    @PutMapping("/{wishId}")
    public ResponseEntity<?> updateWish(
            @PathVariable Long wishId,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam String category,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) MultipartFile image,
            @RequestParam(required = false) Long dailyId,
            HttpSession session) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        try {
            Wish.WishCategory wishCategory = Wish.WishCategory.valueOf(category);
            Wish wish = wishService.updateWish(wishId, user, title, description, wishCategory, latitude, longitude, address, image, dailyId);

            return ResponseEntity.ok(Map.of("success", true, "wish", wish));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update wish", e);
            return ResponseEntity.status(500).body(Map.of("error", "위시 수정에 실패했습니다."));
        }
    }

    /**
     * 위시 완료 토글
     */
    @PostMapping("/{wishId}/toggle-complete")
    public ResponseEntity<?> toggleWishCompletion(
            @PathVariable Long wishId,
            HttpSession session) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        try {
            Wish wish = wishService.toggleWishCompletion(wishId, user);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "completed", wish.getCompleted()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to toggle wish completion", e);
            return ResponseEntity.status(500).body(Map.of("error", "위시 완료 처리에 실패했습니다."));
        }
    }

    /**
     * 위시 삭제
     */
    @DeleteMapping("/{wishId}")
    public ResponseEntity<?> deleteWish(
            @PathVariable Long wishId,
            HttpSession session) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        try {
            wishService.deleteWish(wishId, user);
            return ResponseEntity.ok(Map.of("success", true, "message", "위시가 삭제되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to delete wish", e);
            return ResponseEntity.status(500).body(Map.of("error", "위시 삭제에 실패했습니다."));
        }
    }

    /**
     * 특정 사용자의 모든 일정 조회
     */
    @GetMapping("/schedules")
    public ResponseEntity<?> getUserSchedules(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        try {
            List<WishSchedule> schedules = wishService.getUserSchedules(user.getId());

            List<Map<String, Object>> scheduleList = schedules.stream().map(schedule -> {
                Map<String, Object> scheduleMap = new HashMap<>();
                scheduleMap.put("id", schedule.getId());
                scheduleMap.put("wishId", schedule.getWish().getId());
                scheduleMap.put("title", schedule.getTitle());
                scheduleMap.put("description", schedule.getDescription());
                scheduleMap.put("scheduledDate", schedule.getScheduledDate());
                scheduleMap.put("completed", schedule.getCompleted());

                Map<String, Object> wishMap = new HashMap<>();
                wishMap.put("id", schedule.getWish().getId());
                wishMap.put("title", schedule.getWish().getTitle());
                wishMap.put("category", schedule.getWish().getCategory());
                wishMap.put("address", schedule.getWish().getAddress() != null ? schedule.getWish().getAddress() : "");
                wishMap.put("latitude", schedule.getWish().getLatitude());
                wishMap.put("longitude", schedule.getWish().getLongitude());
                wishMap.put("description", schedule.getWish().getDescription());
                wishMap.put("dailyId", schedule.getWish().getDailyId());
                wishMap.put("user", Map.of(
                    "id", schedule.getWish().getUser().getId(),
                    "username", schedule.getWish().getUser().getUsername(),
                    "displayName", schedule.getWish().getUser().getDisplayName() != null ? schedule.getWish().getUser().getDisplayName() : schedule.getWish().getUser().getUsername()
                ));

                scheduleMap.put("wish", wishMap);
                return scheduleMap;
            }).toList();

            return ResponseEntity.ok(scheduleList);
        } catch (Exception e) {
            log.error("Failed to get schedules", e);
            return ResponseEntity.status(500).body(Map.of("error", "일정 조회에 실패했습니다."));
        }
    }

    /**
     * 날짜 범위로 일정 조회
     */
    @GetMapping("/schedules/range")
    public ResponseEntity<?> getSchedulesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            HttpSession session) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        try {
            List<WishSchedule> schedules = wishService.getSchedulesByDateRange(user.getId(), startDate, endDate);

            List<Map<String, Object>> scheduleList = schedules.stream().map(schedule -> {
                Map<String, Object> scheduleMap = new HashMap<>();
                scheduleMap.put("id", schedule.getId());
                scheduleMap.put("wishId", schedule.getWish().getId());
                scheduleMap.put("title", schedule.getTitle());
                scheduleMap.put("description", schedule.getDescription());
                scheduleMap.put("scheduledDate", schedule.getScheduledDate());
                scheduleMap.put("completed", schedule.getCompleted());

                Map<String, Object> wishMap = new HashMap<>();
                wishMap.put("id", schedule.getWish().getId());
                wishMap.put("title", schedule.getWish().getTitle());
                wishMap.put("category", schedule.getWish().getCategory());
                wishMap.put("address", schedule.getWish().getAddress() != null ? schedule.getWish().getAddress() : "");
                wishMap.put("latitude", schedule.getWish().getLatitude());
                wishMap.put("longitude", schedule.getWish().getLongitude());
                wishMap.put("description", schedule.getWish().getDescription());
                wishMap.put("dailyId", schedule.getWish().getDailyId());
                wishMap.put("user", Map.of(
                    "id", schedule.getWish().getUser().getId(),
                    "username", schedule.getWish().getUser().getUsername(),
                    "displayName", schedule.getWish().getUser().getDisplayName() != null ? schedule.getWish().getUser().getDisplayName() : schedule.getWish().getUser().getUsername()
                ));

                scheduleMap.put("wish", wishMap);
                return scheduleMap;
            }).toList();

            return ResponseEntity.ok(scheduleList);
        } catch (Exception e) {
            log.error("Failed to get schedules by date range", e);
            return ResponseEntity.status(500).body(Map.of("error", "일정 조회에 실패했습니다."));
        }
    }

    /**
     * 일정 생성
     */
    @PostMapping("/{wishId}/schedules")
    public ResponseEntity<?> createSchedule(
            @PathVariable Long wishId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledDate,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            HttpSession session) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        try {
            WishSchedule schedule = wishService.createSchedule(wishId, scheduledDate, title, description);
            return ResponseEntity.ok(Map.of("success", true, "schedule", schedule));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to create schedule", e);
            return ResponseEntity.status(500).body(Map.of("error", "일정 추가에 실패했습니다."));
        }
    }

    /**
     * 일정 완료 토글
     */
    @PostMapping("/schedules/{scheduleId}/toggle-complete")
    public ResponseEntity<?> toggleScheduleCompletion(
            @PathVariable Long scheduleId,
            HttpSession session) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        try {
            WishSchedule schedule = wishService.toggleScheduleCompletion(scheduleId, user);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "completed", schedule.getCompleted()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to toggle schedule completion", e);
            return ResponseEntity.status(500).body(Map.of("error", "일정 완료 처리에 실패했습니다."));
        }
    }

    /**
     * 일정 삭제
     */
    @DeleteMapping("/schedules/{scheduleId}")
    public ResponseEntity<?> deleteSchedule(
            @PathVariable Long scheduleId,
            HttpSession session) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        try {
            wishService.deleteSchedule(scheduleId, user);
            return ResponseEntity.ok(Map.of("success", true, "message", "일정이 삭제되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to delete schedule", e);
            return ResponseEntity.status(500).body(Map.of("error", "일정 삭제에 실패했습니다."));
        }
    }
}
