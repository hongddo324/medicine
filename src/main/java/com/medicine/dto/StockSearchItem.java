package com.medicine.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockSearchItem {
    @JsonProperty("hname")
    private String hname;  // 종목명 (한글)

    @JsonProperty("srtn_cd")
    private String srtnCd;  // 단축코드 (종목코드)

    @JsonProperty("stock_code")
    private String stockCode;  // 종목코드 (대체 필드)
}
