package com.medicine.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("MealCheck")
public class MealCheck implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    @Indexed
    private LocalDate date;

    @Indexed
    private MealType mealType;  // 아침, 점심, 저녁

    private String imageUrl;  // 식단 이미지 URL

    private LocalDateTime uploadedAt;  // 업로드 시간

    private String uploadedBy;  // 업로드한 사용자 ID

    private String aiEvaluation;  // AI 평가 결과

    private Integer score;  // AI가 매긴 점수 (0-100)

    public enum MealType {
        BREAKFAST("아침"),
        LUNCH("점심"),
        DINNER("저녁");

        private final String displayName;

        MealType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
