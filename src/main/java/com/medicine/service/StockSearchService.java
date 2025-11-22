package com.medicine.service;

import com.medicine.dto.StockSummaryDto;
import com.medicine.dto.kis.StockPriceDto;
import com.medicine.dto.kis.StockSearchItemDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockSearchService {

    private final KisStockClient kisStockClient;

    /**
     * 한글 키워드로 종목 검색 + 현재가 조회
     *
     * @param keyword 검색 키워드 (예: "삼성전자")
     * @return 종목 요약 정보 리스트 (최대 5개)
     */
    public List<StockSummaryDto> searchWithPrice(String keyword) {
        log.info("주식 검색 요청 - keyword: {}", keyword);

        // 1. 종목 검색
        List<StockSearchItemDto> searchResults = kisStockClient.searchStocks(keyword);

        // 2. 상위 5개만 선택
        List<StockSearchItemDto> topResults = searchResults.stream()
                .limit(5)
                .collect(Collectors.toList());

        log.info("검색된 종목 중 상위 {}개에 대해 현재가 조회", topResults.size());

        // 3. 각 종목의 현재가 조회 및 StockSummaryDto로 변환
        return topResults.stream()
                .map(this::fetchPriceAndCreateSummary)
                .filter(summary -> summary != null)  // 실패한 것은 제외
                .collect(Collectors.toList());
    }

    /**
     * 개별 종목의 현재가 조회 및 요약 DTO 생성
     *
     * @param searchItem 종목 검색 결과 아이템
     * @return 종목 요약 정보
     */
    private StockSummaryDto fetchPriceAndCreateSummary(StockSearchItemDto searchItem) {
        try {
            StockPriceDto priceDto = kisStockClient.getCurrentPrice(searchItem.getStockCode());

            return StockSummaryDto.builder()
                    .stockCode(searchItem.getStockCode())
                    .stockName(searchItem.getStockName())
                    .marketCode(searchItem.getMarketCode())
                    .currentPrice(priceDto.getCurrentPrice())
                    .priceChange(priceDto.getPriceChange())
                    .changeRate(priceDto.getChangeRate())
                    .build();
        } catch (Exception e) {
            log.error("현재가 조회 실패 - stockCode: {}, stockName: {}, error: {}",
                    searchItem.getStockCode(), searchItem.getStockName(), e.getMessage());
            // 실패한 종목은 null 반환 (필터링됨)
            return null;
        }
    }
}
