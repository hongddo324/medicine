package com.medicine.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 일상 게시물의 다중 이미지를 관리하는 엔티티
 * 인스타그램 스타일의 여러 이미지를 하나의 게시물에 첨부할 수 있습니다.
 */
@Entity
@Table(name = "daily_images")
@Data
public class DailyImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_id", nullable = false)
    private Daily daily;

    /**
     * 이미지 URL
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String imageUrl;

    /**
     * 이미지 순서 (슬라이드 순서)
     * 0부터 시작
     */
    @Column(nullable = false)
    private Integer imageOrder = 0;

    /**
     * 이미지 타입
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private MediaType mediaType = MediaType.IMAGE;

    /**
     * 생성 시간
     */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum MediaType {
        IMAGE,
        VIDEO
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (imageOrder == null) {
            imageOrder = 0;
        }
        if (mediaType == null) {
            mediaType = MediaType.IMAGE;
        }
    }
}
