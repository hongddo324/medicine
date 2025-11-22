package com.medicine.service;

import com.medicine.config.KisApiProperties;
import com.medicine.dto.kis.*;
import com.medicine.exception.StockSearchException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 한국투자증권 Open API 클라이언트
 *
 * 주식 현재가 조회 전용
 * (종목 검색은 DB에서 수행)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KisStockClient {

    private final WebClient kisWebClient;
    private final TokenService tokenService;
    private final KisApiProperties kisApiProperties;

    /**
     * 주식 현재가 조회
     *
     * 한국투자증권 API: /uapi/domestic-stock/v1/quotations/inquire-price
     * TR_ID: FHKST01010100
     *
     * @param stockCode 종목코드 (예: "005930")
     * @return 현재가 정보 (가격, 등락률 등)
     * @throws StockSearchException API 호출 실패 시
     */
    public StockPriceDto getCurrentPrice(String stockCode) {
        String accessToken = tokenService.getAccessToken();
        if (accessToken == null) {
            throw new StockSearchException("API 인증 토큰을 가져올 수 없습니다");
        }

        String priceTrId = kisApiProperties.getTrId().getPriceQuote();
        log.info("현재가 조회 API 호출 - stockCode: {}, tr_id: {}", stockCode, priceTrId);

        try {
            StockPriceResponseDto response = kisWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/uapi/domestic-stock/v1/quotations/inquire-price")
                            .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                            .queryParam("FID_INPUT_ISCD", stockCode)
                            .build())
                    .header("Authorization", "Bearer " + accessToken)
                    .header("appkey", kisApiProperties.getAppKey())
                    .header("appsecret", kisApiProperties.getAppSecret())
                    .header("tr_id", priceTrId)
                    .header("custtype", "P")
                    .retrieve()
                    .bodyToMono(StockPriceResponseDto.class)
                    .block();

            if (response == null) {
                log.error("현재가 조회 API 응답이 null - stockCode: {}", stockCode);
                throw new StockSearchException("현재가 조회 API 응답을 받지 못했습니다");
            }

            log.debug("현재가 조회 API 응답 - rt_cd: {}, msg_cd: {}, msg: {}",
                    response.getRtCd(), response.getMsgCd(), response.getMsg());

            if (!"0".equals(response.getRtCd())) {
                String errorMsg = String.format("현재가 조회 실패 [%s] %s", response.getMsgCd(), response.getMsg());
                log.error(errorMsg);
                throw new StockSearchException(errorMsg);
            }

            StockPriceOutputDto output = response.getOutput();
            if (output == null) {
                log.error("현재가 데이터 없음 - stockCode: {}", stockCode);
                throw new StockSearchException("현재가 데이터가 없습니다");
            }

            // String → Long/Double 변환
            StockPriceDto priceDto = StockPriceDto.builder()
                    .stockCode(output.getStockCode())
                    .stockName(output.getStockName())
                    .currentPrice(parseLong(output.getCurrentPrice()))
                    .priceChange(parseLong(output.getPriceChange()))
                    .changeRate(parseDouble(output.getChangeRate()))
                    .volume(parseLong(output.getVolume()))
                    .build();

            log.info("현재가 조회 성공 - {}: {}원", priceDto.getStockName(), priceDto.getCurrentPrice());
            return priceDto;

        } catch (StockSearchException e) {
            throw e;
        } catch (Exception e) {
            log.error("현재가 조회 API 호출 중 예외 발생 - stockCode: {}", stockCode, e);
            throw new StockSearchException("현재가 조회 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    // 문자열 → Long 파싱 (예외 안전)
    private Long parseLong(String value) {
        try {
            return value != null ? Long.parseLong(value.replaceAll("[^0-9-]", "")) : 0L;
        } catch (Exception e) {
            log.warn("Long 파싱 실패 - value: {}", value);
            return 0L;
        }
    }

    // 문자열 → Double 파싱 (예외 안전)
    private Double parseDouble(String value) {
        try {
            return value != null ? Double.parseDouble(value.replaceAll("[^0-9.-]", "")) : 0.0;
        } catch (Exception e) {
            log.warn("Double 파싱 실패 - value: {}", value);
            return 0.0;
        }
    }
}
