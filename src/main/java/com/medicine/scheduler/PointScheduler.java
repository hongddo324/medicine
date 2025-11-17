package com.medicine.scheduler;

import com.medicine.model.MealCheck;
import com.medicine.model.PointHistory;
import com.medicine.model.Role;
import com.medicine.model.User;
import com.medicine.repository.MealCheckRepository;
import com.medicine.repository.UserRepository;
import com.medicine.service.PointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PointScheduler {

    private final UserRepository userRepository;
    private final MealCheckRepository mealCheckRepository;
    private final PointService pointService;

    /**
     * 매일 자정 1분에 실행
     * 전날 식단 점수를 계산하여 FATHER 권한 사용자에게 포인트 적립
     */
    @Scheduled(cron = "0 1 0 * * *")  // 매일 00:01:00에 실행
    public void calculateDailyMealPoints() {
        log.info("=== 일일 식단 포인트 계산 시작 ===");

        try {
            // FATHER 권한 사용자만 조회
            List<User> fatherUsers = userRepository.findAll().stream()
                    .filter(user -> user.getRole() == Role.FATHER)
                    .toList();

            if (fatherUsers.isEmpty()) {
                log.info("FATHER 권한 사용자가 없습니다.");
                return;
            }

            LocalDate yesterday = LocalDate.now().minusDays(1);

            for (User user : fatherUsers) {
                try {
                    // 전날 식단 조회
                    List<MealCheck> meals = mealCheckRepository.findByDateOrderByMealTypeAsc(yesterday);

                    if (meals.isEmpty()) {
                        log.info("식단 없음 - User: {}, Date: {}", user.getUsername(), yesterday);
                        continue;
                    }

                    // 평균 점수 계산
                    double avgScore = meals.stream()
                            .filter(m -> m.getScore() != null)
                            .mapToInt(MealCheck::getScore)
                            .average()
                            .orElse(0.0);

                    // 점수에 따른 포인트 계산
                    int points;
                    if (avgScore >= 80) {
                        points = 70;
                    } else if (avgScore >= 60) {
                        points = 50;
                    } else if (avgScore >= 40) {
                        points = 30;
                    } else {
                        points = 20;
                    }

                    // 포인트 적립
                    pointService.addPoints(
                            user,
                            points,
                            PointHistory.PointType.MEAL,
                            String.format("%s 식단 관리 (평균 %.0f점)", yesterday, avgScore)
                    );

                    log.info("식단 포인트 적립 - User: {}, Date: {}, AvgScore: {}, Points: +{}",
                            user.getUsername(), yesterday, avgScore, points);

                } catch (Exception e) {
                    log.error("식단 포인트 계산 실패 - User: {}", user.getUsername(), e);
                }
            }

            log.info("=== 일일 식단 포인트 계산 완료 ===");

        } catch (Exception e) {
            log.error("일일 식단 포인트 계산 중 오류 발생", e);
        }
    }
}
