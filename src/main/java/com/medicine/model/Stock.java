package com.medicine.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "stocks")
public class Stock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String stockCode;  // 종목코드

    @Column(nullable = false)
    private String stockName;  // 종목명

    @Column(nullable = false)
    private String market;  // DOMESTIC, NASDAQ, NYSE, etc.

    @Column(nullable = false)
    private Integer quantity;  // 수량

    @Column(nullable = false)
    private Long buyPrice;  // 매수가 (원)

    @Column(nullable = false)
    private Integer pointsUsed;  // 사용한 포인트

    @Column(nullable = false)
    private LocalDateTime purchaseDate;  // 매수일시

    @PrePersist
    protected void onCreate() {
        purchaseDate = LocalDateTime.now();
    }
}
