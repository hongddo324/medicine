package com.medicine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockSummaryDto {
    private String stockCode;    // 종목코드
    private String stockName;    // 종목명
    private String marketCode;   // 시장구분
    private Long currentPrice;   // 현재가
    private Long priceChange;    // 전일 대비
    private Double changeRate;   // 등락률
}
