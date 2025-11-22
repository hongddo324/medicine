package com.medicine.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockPriceResponse {
    @JsonProperty("rt_cd")
    private String rtCd;  // 응답 코드 ("0": 성공, "1": 실패)

    @JsonProperty("msg_cd")
    private String msgCd;  // 메시지 코드

    @JsonProperty("msg1")
    private String msg1;  // 메시지

    @JsonProperty("output")
    private StockPriceOutput output;  // 현재가 정보
}
