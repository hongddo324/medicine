package com.medicine.controller;

import com.medicine.dto.StockDTO;
import com.medicine.model.Stock;
import com.medicine.model.User;
import com.medicine.service.StockService;
import com.medicine.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
@Slf4j
public class StockController {

    private final StockService stockService;
    private final UserService userService;

    // 주식 검색 (한글 키워드로 종목 검색 + 현재가 조회)
    @GetMapping("/search")
    public ResponseEntity<?> searchStocks(@RequestParam String keyword) {
        try {
            log.info("주식 검색 요청 - keyword: {}", keyword);
            StockDTO result = stockService.searchDomesticStockByKoreanName(keyword);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            // 종목 없음 (400 Bad Request)
            log.warn("종목 검색 실패 - keyword: {}, reason: {}", keyword, e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (IllegalStateException e) {
            // API 응답 오류 (500 Internal Server Error)
            log.error("종목 검색 중 API 오류 - keyword: {}, reason: {}", keyword, e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        } catch (Exception e) {
            // 기타 예외 (500 Internal Server Error)
            log.error("주식 검색 중 예외 발생 - keyword: {}", keyword, e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "주식 검색 중 오류가 발생했습니다");
            return ResponseEntity.internalServerError().body(error);
        }
    }

    // 국내 주식 시세 조회
    @GetMapping("/domestic/{stockCode}")
    public ResponseEntity<StockDTO> getDomesticStock(@PathVariable String stockCode) {
        try {
            StockDTO stock = stockService.getDomesticStockPrice(stockCode);
            return ResponseEntity.ok(stock);
        } catch (Exception e) {
            log.error("국내 주식 조회 실패: {}", e.getMessage());
            return ResponseEntity.ok(new StockDTO());
        }
    }

    // 해외 주식 시세 조회
    @GetMapping("/overseas/{market}/{stockCode}")
    public ResponseEntity<StockDTO> getOverseasStock(
            @PathVariable String market,
            @PathVariable String stockCode) {
        try {
            StockDTO stock = stockService.getOverseasStockPrice(stockCode, market);
            return ResponseEntity.ok(stock);
        } catch (Exception e) {
            log.error("해외 주식 조회 실패: {}", e.getMessage());
            return ResponseEntity.ok(new StockDTO());
        }
    }

    // 주식 매수
    @PostMapping("/buy")
    public ResponseEntity<Map<String, Object>> buyStock(
            @RequestBody Map<String, Object> request,
            HttpSession session) {
        try {
            String username = (String) session.getAttribute("username");
            if (username == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "로그인이 필요합니다");
                return ResponseEntity.status(401).body(response);
            }

            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

            String stockCode = (String) request.get("stockCode");
            String market = (String) request.get("market");
            Long buyPrice = ((Number) request.get("buyPrice")).longValue();
            Integer quantity = ((Number) request.get("quantity")).intValue();

            Stock stock = stockService.buyStock(user, stockCode, market, buyPrice, quantity);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("remainingPoints", user.getPoints());
            response.put("stock", stock);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("주식 매수 실패: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "주식 매수에 실패했습니다");
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // 내 보유 주식 조회
    @GetMapping("/my-stocks")
    public ResponseEntity<List<Map<String, Object>>> getMyStocks(HttpSession session) {
        try {
            String username = (String) session.getAttribute("username");
            if (username == null) {
                return ResponseEntity.status(401).body(List.of());
            }

            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

            List<Stock> stocks = stockService.getUserStocks(user);

            // 현재가 정보 포함
            List<Map<String, Object>> result = stocks.stream().map(stock -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", stock.getId());
                map.put("code", stock.getStockCode());
                map.put("name", stock.getStockName());
                map.put("market", stock.getMarket());
                map.put("quantity", stock.getQuantity());
                map.put("buyPrice", stock.getBuyPrice());
                map.put("pointsUsed", stock.getPointsUsed());
                map.put("purchaseDate", stock.getPurchaseDate());

                // 현재가 조회
                try {
                    StockDTO currentInfo = stock.getMarket().equals("DOMESTIC") ?
                        stockService.getDomesticStockPrice(stock.getStockCode()) :
                        stockService.getOverseasStockPrice(stock.getStockCode(), stock.getMarket());
                    map.put("currentPrice", currentInfo.getCurrentPrice());
                } catch (Exception e) {
                    map.put("currentPrice", stock.getBuyPrice());
                }

                return map;
            }).toList();

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("보유 주식 조회 실패: {}", e.getMessage());
            return ResponseEntity.ok(List.of());
        }
    }
}
