package com.medicine.controller;

import com.medicine.model.Activity;
import com.medicine.model.MealCheck;
import com.medicine.model.User;
import com.medicine.service.ActivityService;
import com.medicine.service.MealCheckService;
import com.medicine.service.PushNotificationService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/meal")
@RequiredArgsConstructor
public class MealCheckController {

    private final MealCheckService mealCheckService;
    private final PushNotificationService pushNotificationService;
    private final ActivityService activityService;

    /**
     * 식단 이미지 업로드
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadMeal(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String mealType,
            @RequestParam MultipartFile image,
            HttpSession session) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        // Check if user is FATHER (only FATHER can upload meals)
        if (user.getRole() != com.medicine.model.Role.FATHER) {
            return ResponseEntity.status(403).body(Map.of("error", "식단 업로드는 아버님만 가능합니다."));
        }

        try {
            MealCheck.MealType type = MealCheck.MealType.valueOf(mealType.toUpperCase());

            log.info("Meal upload request - User: {}, Date: {}, Type: {}",
                    user.getUsername(), date, type);

            MealCheck mealCheck = mealCheckService.uploadMealImage(date, type, image, user);

            log.info("Meal uploaded successfully - ID: {}, Score: {}",
                    mealCheck.getId(), mealCheck.getScore());

            // 활동 기록 생성
            try {
                String mealTypeName = type.getDisplayName();
                String message = user.getDisplayName() + "님이 " + mealTypeName + " 식단을 업로드했습니다";
                activityService.createActivity(user, Activity.ActivityType.MEAL_UPLOADED, message, mealCheck.getId());
                log.info("Activity created for meal upload - User: {}, Type: {}", user.getUsername(), mealTypeName);
            } catch (Exception e) {
                log.error("Failed to create activity for meal upload", e);
            }

            // 식사 업로드 알림 전송
            String mealTypeName = type.getDisplayName();
            String notificationMessage = mealTypeName + " 식사가 업로드되었습니다";
            Map<String, String> notificationData = Map.of(
                    "type", "meal-upload",
                    "mealType", type.name().toLowerCase(),
                    "date", date.toString()
            );
            pushNotificationService.sendNotificationToAllUsersExcept(
                    user.getUsername(),
                    "🍽️ 식사 업로드 알림",
                    notificationMessage,
                    "/medicine",
                    notificationData
            );

            log.info("FCM notification sent for meal upload - Type: {}", mealTypeName);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("meal", mealCheck);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            // AI가 음식을 찾지 못한 경우 또는 잘못된 식사 타입
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("음식을 찾을 수 없습니다")) {
                log.warn("AI could not detect food in image: {}", errorMessage);
                return ResponseEntity.badRequest().body(Map.of("error", errorMessage));
            } else {
                log.error("Invalid meal type: {}", mealType, e);
                return ResponseEntity.badRequest().body(Map.of("error", "잘못된 식사 타입입니다."));
            }
        } catch (IOException e) {
            log.error("Failed to upload meal image", e);
            return ResponseEntity.status(500).body(Map.of("error", "이미지 업로드 중 오류가 발생했습니다."));
        } catch (Exception e) {
            log.error("Unexpected error during meal upload", e);
            return ResponseEntity.status(500).body(Map.of("error", "서버 오류가 발생했습니다."));
        }
    }

    /**
     * 특정 날짜의 식단 조회
     */
    @GetMapping("/date/{date}")
    public ResponseEntity<?> getMealsByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpSession session) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        List<MealCheck> meals = mealCheckService.getMealsByDate(date);
        Map<String, Object> stats = mealCheckService.getDailyStats(date);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("meals", meals);
        response.put("stats", stats);

        return ResponseEntity.ok(response);
    }

    /**
     * 월별 식단 데이터 조회 (달력용)
     */
    @GetMapping("/calendar/{year}/{month}")
    public ResponseEntity<?> getMonthlyMealData(
            @PathVariable int year,
            @PathVariable int month,
            HttpSession session) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        Map<String, Object> data = mealCheckService.getMonthlyMealData(year, month);

        log.debug("Monthly meal data requested - User: {}, Year: {}, Month: {}",
                user.getUsername(), year, month);

        return ResponseEntity.ok(data);
    }

    /**
     * 식단 삭제
     */
    @DeleteMapping("/{mealId}")
    public ResponseEntity<?> deleteMeal(
            @PathVariable Long mealId,
            HttpSession session) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
        }

        try {
            mealCheckService.deleteMeal(mealId);

            log.info("Meal deleted - User: {}, MealId: {}", user.getUsername(), mealId);

            return ResponseEntity.ok(Map.of("success", true));

        } catch (Exception e) {
            log.error("Failed to delete meal", e);
            return ResponseEntity.status(500).body(Map.of("error", "삭제 중 오류가 발생했습니다."));
        }
    }
}
