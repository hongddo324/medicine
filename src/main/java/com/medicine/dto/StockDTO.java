package com.medicine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockDTO {
    private String code;
    private String name;
    private String market;  // DOMESTIC, NASDAQ, NYSE, etc.
    private Long currentPrice;  // 현재가 (원)
    private Long change;  // 전일대비
    private Double changeRate;  // 등락률
    private Long volume;  // 거래량
}
