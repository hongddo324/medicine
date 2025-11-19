package com.medicine.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "activity", indexes = {
    @Index(name = "idx_created_at", columnList = "created_at"),
    @Index(name = "idx_is_read", columnList = "is_read")
})
public class Activity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActivityType activityType;  // 활동 유형

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;  // 활동 수행자

    @Column(columnDefinition = "TEXT")
    private String message;  // 활동 메시지

    @Column(name = "reference_id")
    private Long referenceId;  // 참조 ID (댓글, 일상, 위시 등의 ID)

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;  // 읽음 여부

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;  // 생성 시간

    public enum ActivityType {
        COMMENT,           // 응원메시지 작성
        COMMENT_REPLY,     // 응원메시지 답글
        DAILY_POST,        // 일상 게시
        DAILY_COMMENT,     // 일상 댓글
        DAILY_LIKE,        // 일상 좋아요
        WISH_ADDED,        // 위시 추가
        SCHEDULE_ADDED,    // 일정 추가
        PROFILE_UPDATED    // 프로필 업데이트
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (isRead == null) {
            isRead = false;
        }
    }
}
