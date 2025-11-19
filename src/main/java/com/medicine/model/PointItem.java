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
@Table(name = "point_items")
public class PointItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;  // 상품명

    @Column(columnDefinition = "TEXT")
    private String description;  // 상품 설명

    @Column(nullable = false)
    private Integer points;  // 필요한 포인트

    @Column(length = 50)
    private String icon;  // 아이콘 (Bootstrap icon 클래스명)

    @Column(length = 20)
    private String color;  // 색상 (CSS color)

    @Column(columnDefinition = "TEXT")
    private String imageUrl;  // 상품 이미지 URL

    @Column(nullable = false)
    private Boolean available = true;  // 구매 가능 여부

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (available == null) {
            available = true;
        }
    }
}
