package com.medicine.service;

import com.medicine.dto.StockSummaryDto;
import com.medicine.dto.kis.StockPriceDto;
import com.medicine.exception.StockSearchException;
import com.medicine.model.StockInfo;
import com.medicine.repository.StockInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 주식 검색 서비스
 *
 * 플로우:
 * 1. DB에서 키워드로 종목 검색 (LIKE 검색)
 * 2. 검색된 종목들의 종목코드로 KIS API 현재가 조회
 * 3. StockSummaryDto 리스트 반환
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockSearchService {

    private final StockInfoRepository stockInfoRepository;
    private final KisStockClient kisStockClient;

    private static final int MAX_SEARCH_RESULTS = 10;  // 최대 검색 결과 개수

    /**
     * 한글 키워드로 종목 검색 + 현재가 조회
     *
     * 플로우:
     * 1. DB에서 종목명 LIKE 검색 (최대 10개)
     * 2. 각 종목의 종목코드로 KIS API 현재가 조회
     * 3. 결과 반환
     *
     * @param keyword 검색 키워드 (예: "삼성", "삼성전자")
     * @return 종목 요약 정보 리스트
     * @throws StockSearchException 검색 결과가 없을 때
     */
    public List<StockSummaryDto> searchWithPrice(String keyword) {
        log.info("주식 검색 시작 - keyword: {}", keyword);

        // 1. DB에서 종목 검색 (LIKE 검색)
        List<StockInfo> stockInfoList = stockInfoRepository.findByStockNameContainingWithLimit(
                keyword,
                MAX_SEARCH_RESULTS
        );

        if (stockInfoList.isEmpty()) {
            log.warn("검색 결과 없음 - keyword: {}", keyword);
            throw new StockSearchException("검색 결과가 없습니다: " + keyword);
        }

        log.info("DB 검색 완료 - keyword: {}, 결과 수: {}", keyword, stockInfoList.size());

        // 2. 각 종목의 현재가 조회
        List<StockSummaryDto> results = stockInfoList.stream()
                .map(this::fetchPriceAndCreateSummary)
                .filter(summary -> summary != null)  // API 조회 실패한 것 제외
                .collect(Collectors.toList());

        log.info("주식 검색 완료 - keyword: {}, 최종 결과 수: {}", keyword, results.size());

        return results;
    }

    /**
     * 개별 종목의 현재가 조회 및 요약 DTO 생성
     *
     * @param stockInfo DB에서 조회한 종목 정보
     * @return 종목 요약 정보 (현재가 포함)
     */
    private StockSummaryDto fetchPriceAndCreateSummary(StockInfo stockInfo) {
        try {
            log.debug("현재가 조회 - stockCode: {}, stockName: {}",
                    stockInfo.getStockCode(), stockInfo.getStockName());

            // KIS API 현재가 조회
            StockPriceDto priceDto = kisStockClient.getCurrentPrice(stockInfo.getStockCode());

            return StockSummaryDto.builder()
                    .stockCode(stockInfo.getStockCode())
                    .stockName(stockInfo.getStockName())
                    .marketCode(stockInfo.getMarketCode())
                    .currentPrice(priceDto.getCurrentPrice())
                    .priceChange(priceDto.getPriceChange())
                    .changeRate(priceDto.getChangeRate())
                    .build();

        } catch (Exception e) {
            log.error("현재가 조회 실패 - stockCode: {}, stockName: {}, error: {}",
                    stockInfo.getStockCode(), stockInfo.getStockName(), e.getMessage());
            // 현재가 조회 실패 시 null 반환 (필터링됨)
            return null;
        }
    }
}
