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
@Table(name = "point_history", indexes = {
    @Index(name = "idx_user_history", columnList = "user_id"),
    @Index(name = "idx_created_at_history", columnList = "created_at")
})
public class PointHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;  // 사용자

    @Column(nullable = false)
    private Integer points;  // 포인트 변동 (양수: 적립, 음수: 차감)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PointType type;  // 포인트 타입

    @Column(columnDefinition = "TEXT")
    private String description;  // 설명

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "point_item_id")
    private PointItem pointItem;  // 구매한 아이템 (구매 시에만)

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public enum PointType {
        MEDICINE("약 복용"),
        MEAL("식단 관리"),
        PURCHASE("상품 구매"),
        MANUAL("수동 지급");

        private final String displayName;

        PointType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
