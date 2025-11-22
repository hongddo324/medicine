package com.medicine.dto.kis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockPriceDto {
    private String stockCode;    // 종목코드
    private String stockName;    // 종목명
    private Long currentPrice;   // 현재가
    private Long priceChange;    // 전일 대비
    private Double changeRate;   // 등락률
    private Long volume;         // 거래량
}
