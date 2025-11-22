package com.medicine.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "kis")
public class KisApiProperties {
    private String baseUrl;
    private String appKey;
    private String appSecret;
    private TrId trId;

    @Data
    public static class TrId {
        private String searchStock;  // 종목 검색용 TR_ID
        private String priceQuote;   // 주식현재가 시세 TR_ID (FHKST01010100)
    }
}
