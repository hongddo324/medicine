package com.medicine.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "meal_checks", indexes = {
    @Index(name = "idx_date", columnList = "date"),
    @Index(name = "idx_meal_type", columnList = "meal_type"),
    @Index(name = "idx_uploaded_by", columnList = "uploaded_by")
})
public class MealCheck implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", nullable = false, length = 20)
    private MealType mealType;  // 아침, 점심, 저녁

    @Column(columnDefinition = "TEXT")
    private String imageUrl;  // 식단 이미지 URL

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;  // 업로드 시간

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;  // 업로드한 사용자

    @Column(columnDefinition = "TEXT")
    private String aiEvaluation;  // AI 평가 결과

    private Integer score;  // AI가 매긴 점수 (0-100)

    @PrePersist
    protected void onCreate() {
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
    }

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
