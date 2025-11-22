package com.medicine.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medicine.dto.StockDTO;
import com.medicine.dto.StockPriceOutput;
import com.medicine.dto.StockPriceResponse;
import com.medicine.dto.StockSearchItem;
import com.medicine.dto.StockSearchResponse;
import com.medicine.model.Stock;
import com.medicine.model.User;
import com.medicine.repository.StockRepository;
import com.medicine.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockService {

    private final StockRepository stockRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${stock.api.app-key:}")
    private String appKey;

    @Value("${stock.api.app-secret:}")
    private String appSecret;

    @Value("${stock.api.base-url:https://openapi.koreainvestment.com:9443}")
    private String baseUrl;

    private static final int POINT_TO_WON = 50;
    private static final String STOCK_TOKEN_KEY = "stock:access_token";
    private static final long TOKEN_EXPIRE_HOURS = 23;

    // 토큰 발급 동기화를 위한 락 객체
    private final Object tokenLock = new Object();

    // 국내 주식 검색 (한글 키워드)
    public StockDTO searchDomesticStockByKoreanName(String keyword) {
        try {
            log.info("한글 종목 검색 시작: {}", keyword);

            // 1. 종목 검색 API 호출
            StockSearchResponse searchResponse = searchStockInfo(keyword);

            // 2. 응답 검증
            if (!"0".equals(searchResponse.getRtCd())) {
                log.error("종목 검색 실패 - rt_cd: {}, msg: {}", searchResponse.getRtCd(), searchResponse.getMsg1());
                throw new IllegalStateException("종목 검색 실패: " + searchResponse.getMsg1());
            }

            if (searchResponse.getOutput() == null || searchResponse.getOutput().isEmpty()) {
                log.warn("검색 결과 없음 - keyword: {}", keyword);
                throw new IllegalArgumentException("해당 키워드에 대한 종목이 없습니다: " + keyword);
            }

            // 3. 첫 번째 종목 선택
            StockSearchItem firstItem = searchResponse.getOutput().get(0);
            String stockCode = firstItem.getSrtnCd() != null ? firstItem.getSrtnCd() : firstItem.getStockCode();
            String stockName = firstItem.getHname();

            log.info("종목 검색 결과 - 종목명: {}, 종목코드: {}", stockName, stockCode);

            // 4. 현재가 조회 API 호출
            StockPriceResponse priceResponse = getDomesticPrice(stockCode);

            // 5. 응답 검증
            if (!"0".equals(priceResponse.getRtCd())) {
                log.error("현재가 조회 실패 - rt_cd: {}, msg: {}", priceResponse.getRtCd(), priceResponse.getMsg1());
                throw new IllegalStateException("현재가 조회 실패: " + priceResponse.getMsg1());
            }

            StockPriceOutput priceOutput = priceResponse.getOutput();
            if (priceOutput == null) {
                log.error("현재가 데이터 없음 - stockCode: {}", stockCode);
                throw new IllegalStateException("현재가 데이터가 없습니다");
            }

            // 6. StockDTO로 변환
            StockDTO dto = new StockDTO();
            dto.setCode(stockCode);
            dto.setName(stockName);
            dto.setMarket("DOMESTIC");
            dto.setCurrentPrice(parseLong(priceOutput.getStckPrpr()));
            dto.setChange(parseLong(priceOutput.getPrdyVrss()));
            dto.setChangeRate(parseDouble(priceOutput.getPrdyCtrt()));
            dto.setVolume(parseLong(priceOutput.getAcmlVol()));

            log.info("종목 정보 조회 완료 - {}: {}원", stockName, dto.getCurrentPrice());
            return dto;

        } catch (IllegalArgumentException | IllegalStateException e) {
            // 비즈니스 예외는 그대로 던짐
            throw e;
        } catch (Exception e) {
            log.error("한글 종목 검색 중 오류 발생 - keyword: {}, error: {}", keyword, e.getMessage(), e);
            throw new RuntimeException("종목 검색 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    // 종목 검색 API 호출
    private StockSearchResponse searchStockInfo(String keyword) {
        try {
            String url = baseUrl + "/uapi/domestic-stock/v1/quotations/search-stock-info";
            HttpHeaders headers = createHeaders("FHPP0C01010000");

            String requestUrl = url + "?KEYWORD=" + java.net.URLEncoder.encode(keyword, "UTF-8");
            log.debug("종목 검색 API 호출 - URL: {}", requestUrl);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(requestUrl, HttpMethod.GET, entity, String.class);

            log.debug("종목 검색 API 응답 - status: {}, body: {}", response.getStatusCode(), response.getBody());

            return objectMapper.readValue(response.getBody(), StockSearchResponse.class);
        } catch (Exception e) {
            log.error("종목 검색 API 호출 실패 - keyword: {}, error: {}", keyword, e.getMessage(), e);
            throw new RuntimeException("종목 검색 API 호출 실패", e);
        }
    }

    // 현재가 조회 API 호출
    private StockPriceResponse getDomesticPrice(String stockCode) {
        try {
            String url = baseUrl + "/uapi/domestic-stock/v1/quotations/inquire-price";
            HttpHeaders headers = createHeaders("FHKST01010100");

            String requestUrl = url + "?FID_COND_MRKT_DIV_CODE=J&FID_INPUT_ISCD=" + stockCode;
            log.debug("현재가 조회 API 호출 - URL: {}", requestUrl);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(requestUrl, HttpMethod.GET, entity, String.class);

            log.debug("현재가 조회 API 응답 - status: {}, body: {}", response.getStatusCode(), response.getBody());

            return objectMapper.readValue(response.getBody(), StockPriceResponse.class);
        } catch (Exception e) {
            log.error("현재가 조회 API 호출 실패 - stockCode: {}, error: {}", stockCode, e.getMessage(), e);
            throw new RuntimeException("현재가 조회 API 호출 실패", e);
        }
    }

    // 문자열 → Long 파싱 (예외 안전)
    private Long parseLong(String value) {
        try {
            return value != null ? Long.parseLong(value.replaceAll("[^0-9-]", "")) : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    // 문자열 → Double 파싱 (예외 안전)
    private Double parseDouble(String value) {
        try {
            return value != null ? Double.parseDouble(value.replaceAll("[^0-9.-]", "")) : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    // 국내 주식 검색 (레거시 - 사용 안 함)
    @Deprecated
    public List<StockDTO> searchDomesticStocks(String keyword) {
        try {
            // 한국투자증권 API - 국내주식 종목검색
            String url = baseUrl + "/uapi/domestic-stock/v1/quotations/search-stock-info";

            HttpHeaders headers = createHeaders("CTPF1002R");

            String requestUrl = url + "?PRDT_TYPE_CD=300&PDNO=" + keyword;

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(requestUrl, HttpMethod.GET, entity, String.class);

            return parseDomesticSearchResponse(response.getBody());
        } catch (Exception e) {
            log.error("국내 주식 검색 실패: {}", e.getMessage());
            return getFallbackDomesticStocks(keyword);
        }
    }

    // 국내 주식 시세 조회
    public StockDTO getDomesticStockPrice(String stockCode) {
        try {
            String url = baseUrl + "/uapi/domestic-stock/v1/quotations/inquire-price";

            HttpHeaders headers = createHeaders("FHKST01010100");

            String requestUrl = url + "?FID_COND_MRKT_DIV_CODE=J&FID_INPUT_ISCD=" + stockCode;

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(requestUrl, HttpMethod.GET, entity, String.class);

            return parseDomesticPriceResponse(response.getBody(), stockCode);
        } catch (Exception e) {
            log.error("국내 주식 시세 조회 실패: {}", e.getMessage());
            return getFallbackDomesticStock(stockCode);
        }
    }

    // 해외 주식 검색
    public List<StockDTO> searchOverseasStocks(String keyword, String market) {
        try {
            // 한국투자증권 API - 해외주식 종목검색
            String url = baseUrl + "/uapi/overseas-price/v1/quotations/search";

            HttpHeaders headers = createHeaders("HHDFS76240000");

            String requestUrl = url + "?AUTH=&EXCD=" + getExchangeCode(market) + "&CO_YN_PRIC=0&CO_ST_PRIC=0&PRDT_NAME=" + keyword;

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(requestUrl, HttpMethod.GET, entity, String.class);

            return parseOverseasSearchResponse(response.getBody(), market);
        } catch (Exception e) {
            log.error("해외 주식 검색 실패: {}", e.getMessage());
            return getFallbackOverseasStocks(keyword, market);
        }
    }

    // 해외 주식 시세 조회
    public StockDTO getOverseasStockPrice(String stockCode, String market) {
        try {
            String url = baseUrl + "/uapi/overseas-price/v1/quotations/price";

            HttpHeaders headers = createHeaders("HHDFS00000300");

            String exchangeCode = getExchangeCode(market);
            String requestUrl = url + "?AUTH=&EXCD=" + exchangeCode + "&SYMB=" + stockCode;

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(requestUrl, HttpMethod.GET, entity, String.class);

            return parseOverseasPriceResponse(response.getBody(), stockCode, market);
        } catch (Exception e) {
            log.error("해외 주식 시세 조회 실패: {}", e.getMessage());
            return getFallbackOverseasStock(stockCode, market);
        }
    }

    // 통합 주식 검색 (레거시 - 사용 안 함)
    @Deprecated
    public List<StockDTO> searchStocks(String keyword) {
        List<StockDTO> results = new ArrayList<>();

        // 국내 주식 검색
        results.addAll(searchDomesticStocks(keyword));

        // 해외 주식은 Fallback만 사용 (API 엔드포인트 미확인)
        // TODO: 해외 주식 API 확인 후 활성화
        // results.addAll(searchOverseasStocks(keyword, "NASDAQ"));
        // results.addAll(searchOverseasStocks(keyword, "NYSE"));

        return results.stream().limit(10).toList();
    }

    // 주식 매수
    public Stock buyStock(User user, String stockCode, String market, Long buyPrice, Integer quantity) {
        int pointsPerShare = (int) Math.ceil((double) buyPrice / POINT_TO_WON);
        int totalPoints = pointsPerShare * quantity;

        if (user.getPoints() < totalPoints) {
            throw new IllegalArgumentException("포인트가 부족합니다");
        }

        // 포인트 차감
        user.setPoints(user.getPoints() - totalPoints);
        userRepository.save(user);

        // 주식 정보 조회
        StockDTO stockInfo = market.equals("DOMESTIC") ?
            getDomesticStockPrice(stockCode) :
            getOverseasStockPrice(stockCode, market);

        // 주식 저장
        Stock stock = new Stock();
        stock.setUser(user);
        stock.setStockCode(stockCode);
        stock.setStockName(stockInfo.getName());
        stock.setMarket(market);
        stock.setQuantity(quantity);
        stock.setBuyPrice(buyPrice);
        stock.setPointsUsed(totalPoints);

        return stockRepository.save(stock);
    }

    // 사용자의 보유 주식 조회
    public List<Stock> getUserStocks(User user) {
        return stockRepository.findByUserOrderByPurchaseDateDesc(user);
    }

    // 헤더 생성
    private HttpHeaders createHeaders(String trId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // API 키가 설정되어 있을 때만 인증 헤더 추가
        if (appKey != null && !appKey.isEmpty()) {
            String token = getAccessToken();
            if (token != null && !token.isEmpty()) {
                headers.set("Authorization", "Bearer " + token);  // 대문자 A
            }
            headers.set("appkey", appKey);
            headers.set("appsecret", appSecret);
            headers.set("custtype", "P");  // 개인고객
        }

        headers.set("tr_id", trId);
        return headers;
    }

    // Access Token 발급 (Redis 캐싱 + 동기화)
    private String getAccessToken() {
        // API 키가 없으면 null 반환 (fallback 데이터 사용)
        if (appKey == null || appKey.isEmpty() || appSecret == null || appSecret.isEmpty()) {
            log.debug("API 키 미설정 - fallback 데이터 사용");
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

            // 토큰 발급 (이제 한 번에 하나의 스레드만 실행)
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
                        log.warn("Redis 토큰 저장 실패: {} (메모리에서만 사용)", e.getMessage());
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

    // 거래소 코드 변환
    private String getExchangeCode(String market) {
        return switch (market) {
            case "NASDAQ" -> "NAS";
            case "NYSE" -> "NYS";
            case "AMEX" -> "AMS";
            case "HONGKONG" -> "HKS";
            case "SHANGHAI" -> "SHS";
            case "SHENZHEN" -> "SZS";
            case "TOKYO" -> "TSE";
            default -> "NAS";
        };
    }

    // 응답 파싱 메서드들
    private List<StockDTO> parseDomesticSearchResponse(String responseBody) {
        // 파싱 로직 구현
        return new ArrayList<>();
    }

    private StockDTO parseDomesticPriceResponse(String responseBody, String stockCode) {
        // 파싱 로직 구현
        return new StockDTO();
    }

    private List<StockDTO> parseOverseasSearchResponse(String responseBody, String market) {
        // 파싱 로직 구현
        return new ArrayList<>();
    }

    private StockDTO parseOverseasPriceResponse(String responseBody, String stockCode, String market) {
        // 파싱 로직 구현
        return new StockDTO();
    }

    // Fallback 데이터 (API 실패시)
    private List<StockDTO> getFallbackDomesticStocks(String keyword) {
        Map<String, StockDTO> stocks = Map.of(
            "005930", new StockDTO("005930", "삼성전자", "DOMESTIC", 75000L, -1000L, -1.32, 15000000L),
            "000660", new StockDTO("000660", "SK하이닉스", "DOMESTIC", 145000L, 2000L, 1.40, 3000000L),
            "035420", new StockDTO("035420", "NAVER", "DOMESTIC", 210000L, 5000L, 2.44, 500000L)
        );

        return stocks.entrySet().stream()
            .filter(e -> e.getValue().getName().contains(keyword) || e.getKey().contains(keyword))
            .map(Map.Entry::getValue)
            .toList();
    }

    private StockDTO getFallbackDomesticStock(String stockCode) {
        return getFallbackDomesticStocks("").stream()
            .filter(s -> s.getCode().equals(stockCode))
            .findFirst()
            .orElse(new StockDTO(stockCode, "Unknown", "DOMESTIC", 0L, 0L, 0.0, 0L));
    }

    private List<StockDTO> getFallbackOverseasStocks(String keyword, String market) {
        Map<String, StockDTO> stocks = Map.of(
            "AAPL", new StockDTO("AAPL", "Apple Inc", "NASDAQ", 175L * 1300, 2L * 1300, 1.15, 50000000L),
            "TSLA", new StockDTO("TSLA", "Tesla Inc", "NASDAQ", 245L * 1300, -5L * 1300, -2.00, 120000000L),
            "MSFT", new StockDTO("MSFT", "Microsoft", "NASDAQ", 380L * 1300, 3L * 1300, 0.80, 25000000L)
        );

        return stocks.entrySet().stream()
            .filter(e -> e.getValue().getName().toLowerCase().contains(keyword.toLowerCase()) ||
                        e.getKey().toLowerCase().contains(keyword.toLowerCase()))
            .filter(e -> e.getValue().getMarket().equals(market))
            .map(Map.Entry::getValue)
            .toList();
    }

    private StockDTO getFallbackOverseasStock(String stockCode, String market) {
        return getFallbackOverseasStocks("", market).stream()
            .filter(s -> s.getCode().equals(stockCode))
            .findFirst()
            .orElse(new StockDTO(stockCode, "Unknown", market, 0L, 0L, 0.0, 0L));
    }
}
