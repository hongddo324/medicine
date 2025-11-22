package com.medicine.controller;

import com.medicine.dto.StockSummaryDto;
import com.medicine.service.StockSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
public class StockSearchController {

    private final StockSearchService stockSearchService;

    /**
     * 주식 검색 (한글 키워드)
     *
     * GET /api/stocks/search?keyword=삼성전자
     *
     * @param keyword 검색 키워드
     * @return 검색 결과 (종목명, 종목코드, 현재가 등)
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchStocks(@RequestParam String keyword) {
        // 키워드 검증
        if (keyword == null || keyword.trim().isEmpty()) {
            log.warn("주식 검색 실패 - 키워드 없음");
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("reason", "INVALID_KEYWORD");
            error.put("message", "검색 키워드를 입력해주세요");
            return ResponseEntity.badRequest().body(error);
        }

        log.info("주식 검색 요청 - keyword: {}", keyword);

        // 검색 수행
        List<StockSummaryDto> items = stockSearchService.searchWithPrice(keyword.trim());

        // 응답 구성
        Map<String, Object> response = new HashMap<>();
        response.put("keyword", keyword.trim());
        response.put("items", items);

        log.info("주식 검색 완료 - keyword: {}, 결과 수: {}", keyword, items.size());
        return ResponseEntity.ok(response);
    }
}
