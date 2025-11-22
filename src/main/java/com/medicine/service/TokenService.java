package com.medicine.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medicine.config.KisApiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final StringRedisTemplate redisTemplate;
    private final KisApiProperties kisApiProperties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String STOCK_TOKEN_KEY = "stock:access_token";
    private static final long TOKEN_EXPIRE_HOURS = 23;

    // 토큰 발급 동기화를 위한 락 객체
    private final Object tokenLock = new Object();

    /**
     * KIS OAuth Access Token 조회
     * - Redis 캐시 우선 조회
     * - 없으면 API 호출하여 발급
     * - 23시간 TTL로 Redis에 저장
     */
    public String getAccessToken() {
        String appKey = kisApiProperties.getAppKey();
        String appSecret = kisApiProperties.getAppSecret();
        String baseUrl = kisApiProperties.getBaseUrl();

        // API 키 확인
        if (appKey == null || appKey.isEmpty() || appSecret == null || appSecret.isEmpty()) {
            log.debug("API 키 미설정 - null 반환");
            return null;
        }

        // 1차 Redis 조회 (Lock 없이 빠르게)
        try {
            String cachedToken = redisTemplate.opsForValue().get(STOCK_TOKEN_KEY);
            if (cachedToken != null && !cachedToken.isEmpty()) {
                log.debug("Redis에서 캐시된 토큰 사용");
                return cachedToken;
            }
        } catch (Exception e) {
            log.warn("Redis 토큰 조회 실패: {}", e.getMessage());
        }

        // Redis에 토큰이 없을 때만 동기화 블록 진입
        synchronized (tokenLock) {
            // 2차 Redis 조회 (다른 스레드가 이미 발급했을 수 있음)
            try {
                String cachedToken = redisTemplate.opsForValue().get(STOCK_TOKEN_KEY);
                if (cachedToken != null && !cachedToken.isEmpty()) {
                    log.debug("Redis에서 캐시된 토큰 사용 (동기화 블록 내)");
                    return cachedToken;
                }
            } catch (Exception e) {
                log.warn("Redis 토큰 재조회 실패: {}", e.getMessage());
            }

            // 토큰 발급
            log.info("OAuth 토큰 발급 시작 (Redis에 토큰 없음)");
            try {
                String url = baseUrl + "/oauth2/tokenP";

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                Map<String, String> body = new HashMap<>();
                body.put("grant_type", "client_credentials");
                body.put("appkey", appKey);
                body.put("appsecret", appSecret);

                HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

                if (response.getStatusCode() == HttpStatus.OK) {
                    JsonNode jsonNode = objectMapper.readTree(response.getBody());
                    String accessToken = jsonNode.get("access_token").asText();

                    // Redis에 토큰 저장 (23시간 TTL)
                    try {
                        redisTemplate.opsForValue().set(
                                STOCK_TOKEN_KEY,
                                accessToken,
                                Duration.ofHours(TOKEN_EXPIRE_HOURS)
                        );
                        log.info("✅ OAuth 토큰 발급 및 Redis 저장 완료 (만료: {}시간)", TOKEN_EXPIRE_HOURS);
                    } catch (Exception e) {
                        log.warn("Redis 토큰 저장 실패: {}", e.getMessage());
                    }

                    return accessToken;
                } else {
                    log.error("OAuth 토큰 발급 실패 - 응답 코드: {}", response.getStatusCode());
                }
            } catch (Exception e) {
                log.error("❌ OAuth 토큰 발급 실패: {}", e.getMessage());
            }

            return null;
        }
    }
}
