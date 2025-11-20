package com.medicine.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "daily")
@Data
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Daily {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"dailies", "wishes", "schedules", "medicineRecords", "mealChecks", "comments", "dailyComments", "dailyLikes", "activities", "password"})
    private User user;

    @Column(columnDefinition = "TEXT")
    private String content;  // 글 내용

    @Column(columnDefinition = "TEXT")
    private String mediaUrl;  // 이미지/영상 URL

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private MediaType mediaType;  // IMAGE, VIDEO

    @Column(nullable = false)
    private Integer likesCount = 0;  // 좋아요 수

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "daily", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("daily-comments")
    private List<DailyComment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "daily", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("daily-likes")
    private List<DailyLike> likes = new ArrayList<>();

    /**
     * 다중 이미지 목록 (인스타그램 스타일)
     * imageOrder로 정렬되어 슬라이드 순서 유지
     */
    @OneToMany(mappedBy = "daily", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("imageOrder ASC")
    @JsonManagedReference("daily-images")
    private List<DailyImage> images = new ArrayList<>();

    public enum MediaType {
        IMAGE,
        VIDEO
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (likesCount == null) {
            likesCount = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
