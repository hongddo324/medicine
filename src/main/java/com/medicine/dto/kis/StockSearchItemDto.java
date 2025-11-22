package com.medicine.dto.kis;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockSearchItemDto {
    @JsonProperty("stck_shrn_iscd")
    private String stockCode;  // 종목코드

    @JsonProperty("hts_kor_isnm")
    private String stockName;  // 한글명

    @JsonProperty("mrkt_cls_code")
    private String marketCode;  // 시장구분코드 (KOSPI, KOSDAQ 등)
}
