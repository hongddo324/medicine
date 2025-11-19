package com.medicine.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "wish_schedule")
@Data
public class WishSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wish_id", nullable = false)
    private Wish wish;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDateTime scheduledDate;  // 일정 날짜 및 시간

    @Column(length = 200)
    private String title;  // 일정 제목 (wish의 title을 기본으로 사용)

    @Column(columnDefinition = "TEXT")
    private String description;  // 메모

    @Column(nullable = false)
    private Boolean completed = false;  // 완료 여부

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (completed == null) {
            completed = false;
        }
    }
}
