package com.medicine.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 한국 주식 종목 정보 엔티티
 *
 * 한국거래소(KRX) 상장 종목 정보를 저장
 * 사용자 키워드 검색 시 종목코드 매핑용
 */
@Entity
@Table(name = "stock_info", indexes = {
    @Index(name = "idx_stock_info_name", columnList = "stock_name"),
    @Index(name = "idx_stock_info_code", columnList = "stock_code"),
    @Index(name = "idx_stock_info_market", columnList = "market_code")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 종목코드 (6자리)
     * 예: 005930 (삼성전자)
     */
    @Column(name = "stock_code", length = 20, nullable = false, unique = true)
    private String stockCode;

    /**
     * 종목명 (한글)
     * 예: 삼성전자
     */
    @Column(name = "stock_name", length = 200, nullable = false)
    private String stockName;

    /**
     * 시장구분
     * KOSPI, KOSDAQ, KONEX
     */
    @Column(name = "market_code", length = 20, nullable = false)
    private String marketCode;

    /**
     * 생성일시
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 수정일시
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
