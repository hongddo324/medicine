package com.medicine.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "daily")
@Data
public class Daily {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
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
    private List<DailyComment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "daily", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DailyLike> likes = new ArrayList<>();

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
