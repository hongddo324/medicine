package com.medicine.service;

import com.medicine.config.KisApiProperties;
import com.medicine.dto.kis.*;
import com.medicine.exception.StockSearchException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KisStockClient {

    private final WebClient kisWebClient;
    private final TokenService tokenService;
    private final KisApiProperties kisApiProperties;

    /**
     * 종목 검색 (한글 키워드)
     *
     * @param keyword 검색 키워드 (예: "삼성전자")
     * @return 종목 검색 결과 리스트
     */
    public List<StockSearchItemDto> searchStocks(String keyword) {
        String accessToken = tokenService.getAccessToken();
        if (accessToken == null) {
            throw new StockSearchException("API 인증 토큰을 가져올 수 없습니다");
        }

        String searchTrId = kisApiProperties.getTrId().getSearchStock();
        log.info("종목 검색 API 호출 - keyword: {}, tr_id: {}", keyword, searchTrId);

        try {
            StockSearchResponseDto response = kisWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/uapi/domestic-stock/v1/quotations/search-stock-info")
                            .queryParam("KEYWORD", keyword)
                            .build())
                    .header("Authorization", "Bearer " + accessToken)
                    .header("appkey", kisApiProperties.getAppKey())
                    .header("appsecret", kisApiProperties.getAppSecret())
                    .header("tr_id", searchTrId)
                    .header("custtype", "P")
                    .retrieve()
                    .bodyToMono(StockSearchResponseDto.class)
                    .block();

            if (response == null) {
                log.error("종목 검색 API 응답이 null");
                throw new StockSearchException("종목 검색 API 응답을 받지 못했습니다");
            }

            log.debug("종목 검색 API 응답 - rt_cd: {}, msg_cd: {}, msg: {}",
                    response.getRtCd(), response.getMsgCd(), response.getMsg());

            if (!"0".equals(response.getRtCd())) {
                String errorMsg = String.format("종목 검색 실패 [%s] %s", response.getMsgCd(), response.getMsg());
                log.error(errorMsg);
                throw new StockSearchException(errorMsg);
            }

            List<StockSearchItemDto> items = response.getItems();
            if (items == null || items.isEmpty()) {
                log.warn("검색 결과 없음 - keyword: {}", keyword);
                throw new StockSearchException("해당 키워드로 검색된 종목이 없습니다: " + keyword);
            }

            log.info("종목 검색 성공 - 검색된 종목 수: {}", items.size());
            return items;

        } catch (StockSearchException e) {
            throw e;
        } catch (Exception e) {
            log.error("종목 검색 API 호출 중 예외 발생 - keyword: {}", keyword, e);
            throw new StockSearchException("종목 검색 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 주식 현재가 조회
     *
     * @param stockCode 종목코드 (예: "005930")
     * @return 현재가 정보
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
