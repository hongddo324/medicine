package com.medicine.service;

import com.medicine.model.MealCheck;
import com.medicine.model.User;
import com.medicine.repository.MealCheckRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MealCheckService {

    private final MealCheckRepository mealCheckRepository;
    private final FileStorageService fileStorageService;
    private final OpenAIService openAIService;

    /**
     * 식단 이미지 업로드 및 AI 분석
     */
    public MealCheck uploadMealImage(LocalDate date, MealCheck.MealType mealType,
                                     MultipartFile image, User user) throws IOException {

        // 해당 날짜/식사타입에 기존 기록이 있는지 확인
        Optional<MealCheck> existingMealOpt = mealCheckRepository.findByDateAndMealType(date, mealType);

        // 기존 이미지가 있다면 삭제
        if (existingMealOpt.isPresent() && existingMealOpt.get().getImageUrl() != null) {
            fileStorageService.deleteFile(existingMealOpt.get().getImageUrl());
        }

        // 새 이미지 저장
        String mealId = "meal_" + date + "_" + mealType.name().toLowerCase();
        String imageUrl = fileStorageService.storeMealImage(image, mealId);

        log.info("Meal image stored - Date: {}, Type: {}, Path: {}", date, mealType, imageUrl);

        // 이미지를 Base64로 인코딩하여 OpenAI에 전송
        String imageBase64 = encodeImageToBase64(imageUrl);

        Map<String, Object> aiResponse = openAIService.analyzeMealImage(imageBase64);

        boolean success = (boolean) aiResponse.get("success");
        int score = (int) aiResponse.get("score");
        String evaluation = (String) aiResponse.get("evaluation");
        String fullResponse = (String) aiResponse.getOrDefault("fullResponse", evaluation);

        log.info("AI analysis completed - Success: {}, Score: {}", success, score);

        // AI가 음식을 찾지 못한 경우 예외 발생
        if (!success) {
            // 업로드된 이미지 삭제
            fileStorageService.deleteFile(imageUrl);
            throw new IllegalArgumentException(fullResponse);
        }

        // MealCheck 객체 생성 또는 업데이트
        MealCheck mealCheck;
        if (existingMealOpt.isPresent()) {
            mealCheck = existingMealOpt.get();
            mealCheck.setImageUrl(imageUrl);
            mealCheck.setUploadedAt(LocalDateTime.now());
            mealCheck.setAiEvaluation(fullResponse);
            mealCheck.setScore(score);
        } else {
            mealCheck = new MealCheck();
            mealCheck.setDate(date);
            mealCheck.setMealType(mealType);
            mealCheck.setImageUrl(imageUrl);
            mealCheck.setUploadedAt(LocalDateTime.now());
            mealCheck.setUploadedBy(user);
            mealCheck.setAiEvaluation(fullResponse);
            mealCheck.setScore(score);
        }

        return mealCheckRepository.save(mealCheck);
    }

    /**
     * 이미지 파일을 Base64로 인코딩
     */
    private String encodeImageToBase64(String imageUrl) throws IOException {
        // imageUrl 형식: /files/meal/meal_2025-01-13_breakfast_abc.jpg
        String[] parts = imageUrl.split("/");
        if (parts.length < 4) {
            throw new IOException("Invalid image URL format");
        }

        String type = parts[2]; // "meal"
        String filename = parts[3];

        Path imagePath = fileStorageService.getFilePath(type, filename);
        byte[] imageBytes = Files.readAllBytes(imagePath);

        return Base64.getEncoder().encodeToString(imageBytes);
    }

    /**
     * 특정 날짜의 모든 식단 조회
     */
    public List<MealCheck> getMealsByDate(LocalDate date) {
        return mealCheckRepository.findByDateOrderByMealTypeAsc(date);
    }

    /**
     * 특정 날짜의 평균 점수 계산
     */
    public Map<String, Object> getDailyStats(LocalDate date) {
        List<MealCheck> meals = mealCheckRepository.findByDateOrderByMealTypeAsc(date);

        Map<String, Object> stats = new HashMap<>();

        if (meals.isEmpty()) {
            stats.put("averageScore", 0);
            stats.put("mealCount", 0);
            stats.put("emoji", "");
            stats.put("meals", new ArrayList<>());
            return stats;
        }

        // 평균 점수 계산 (업로드된 식단만 포함)
        double averageScore = meals.stream()
                .filter(m -> m.getScore() != null)
                .mapToInt(MealCheck::getScore)
                .average()
                .orElse(0.0);

        String emoji = getEmojiForScore((int) averageScore);

        stats.put("averageScore", (int) averageScore);
        stats.put("mealCount", meals.size());
        stats.put("emoji", emoji);
        stats.put("meals", meals);

        return stats;
    }

    /**
     * 점수에 따른 이모티콘 반환
     */
    public String getEmojiForScore(int score) {
        if (score >= 80) {
            return "😊";  // 80-100
        } else if (score >= 60) {
            return "🙂";  // 60-79
        } else if (score >= 40) {
            return "😐";  // 40-59
        } else if (score >= 20) {
            return "😟";  // 20-39
        } else {
            return "😢";  // 0-19
        }
    }

    /**
     * 월별 식단 데이터 조회 (달력 표시용)
     */
    public Map<String, Object> getMonthlyMealData(int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        // JPA를 사용하여 날짜 범위로 조회
        List<MealCheck> meals = mealCheckRepository.findByDateBetweenOrderByDateDescMealTypeAsc(startDate, endDate);

        // 날짜별로 그룹화
        Map<LocalDate, List<MealCheck>> mealsByDate = meals.stream()
                .collect(Collectors.groupingBy(MealCheck::getDate));

        // 각 날짜별 평균 점수와 이모티콘 계산
        List<Map<String, Object>> dailyStats = new ArrayList<>();

        for (Map.Entry<LocalDate, List<MealCheck>> entry : mealsByDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<MealCheck> dailyMeals = entry.getValue();

            double averageScore = dailyMeals.stream()
                    .filter(m -> m.getScore() != null)
                    .mapToInt(MealCheck::getScore)
                    .average()
                    .orElse(0.0);

            String emoji = getEmojiForScore((int) averageScore);

            Map<String, Object> stat = new HashMap<>();
            stat.put("date", date.toString());
            stat.put("averageScore", (int) averageScore);
            stat.put("emoji", emoji);
            stat.put("mealCount", dailyMeals.size());

            dailyStats.add(stat);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("dailyStats", dailyStats);

        log.debug("Retrieved daily stats for {} days in {}-{}", dailyStats.size(), year, month);

        return result;
    }

    /**
     * 식단 삭제
     */
    public void deleteMeal(Long mealId) {
        Optional<MealCheck> mealOpt = mealCheckRepository.findById(mealId);

        if (mealOpt.isPresent()) {
            MealCheck meal = mealOpt.get();

            // 이미지 파일 삭제
            if (meal.getImageUrl() != null && !meal.getImageUrl().isEmpty()) {
                fileStorageService.deleteFile(meal.getImageUrl());
            }

            // DB에서 삭제
            mealCheckRepository.deleteById(mealId);

            log.info("Meal deleted - ID: {}, Date: {}, Type: {}",
                    mealId, meal.getDate(), meal.getMealType());
        }
    }
}
