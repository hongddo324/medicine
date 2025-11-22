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
public class StockPriceResponseDto {
    @JsonProperty("rt_cd")
    private String rtCd;  // 응답 코드 ("0": 성공)

    @JsonProperty("msg_cd")
    private String msgCd;  // 메시지 코드

    @JsonProperty("msg1")
    private String msg;  // 메시지

    @JsonProperty("output")
    private StockPriceOutputDto output;  // 현재가 정보
}
