package com.medicine.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "wish")
@Data
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Wish {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"dailies", "wishes", "schedules", "medicineRecords", "mealChecks", "comments", "dailyComments", "dailyLikes", "activities", "password"})
    private User user;

    @Column(nullable = false, length = 200)
    private String title;  // 제목

    @Column(columnDefinition = "TEXT")
    private String description;  // 설명

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WishCategory category;  // 카테고리

    @Column(precision = 10, scale = 7)
    private Double latitude;  // 위도

    @Column(precision = 10, scale = 7)
    private Double longitude;  // 경도

    @Column(length = 500)
    private String address;  // 주소

    @Column(columnDefinition = "TEXT")
    private String imageUrl;  // 이미지 URL

    @Column(nullable = false)
    private Boolean completed = false;  // 완료 여부

    @Column(name = "daily_id")
    private Long dailyId;  // 연관된 일상 게시물 ID

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "wish", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("wish-schedules")
    private List<WishSchedule> schedules = new ArrayList<>();

    public enum WishCategory {
        RESTAURANT("맛집"),
        TRAVEL("여행지"),
        SHOPPING("쇼핑"),
        CAFE("카페"),
        ACTIVITY("액티비티"),
        OTHER("기타");

        private final String displayName;

        WishCategory(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (completed == null) {
            completed = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
