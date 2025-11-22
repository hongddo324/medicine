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
public class StockPriceOutputDto {
    @JsonProperty("stck_shrn_iscd")
    private String stockCode;  // 종목코드

    @JsonProperty("hts_kor_isnm")
    private String stockName;  // 한글명

    @JsonProperty("stck_prpr")
    private String currentPrice;  // 현재가

    @JsonProperty("prdy_vrss")
    private String priceChange;  // 전일 대비

    @JsonProperty("prdy_vrss_sign")
    private String changeSign;  // 등락 기호

    @JsonProperty("prdy_ctrt")
    private String changeRate;  // 등락률

    @JsonProperty("acml_vol")
    private String volume;  // 거래량
}
