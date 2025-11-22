package com.medicine.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockPriceOutput {
    @JsonProperty("stck_prpr")
    private String stckPrpr;  // 현재가

    @JsonProperty("prdy_vrss")
    private String prdyVrss;  // 전일 대비

    @JsonProperty("prdy_ctrt")
    private String prdyCtrt;  // 등락률

    @JsonProperty("acml_vol")
    private String acmlVol;  // 거래량
}
