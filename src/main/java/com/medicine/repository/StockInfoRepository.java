package com.medicine.repository;

import com.medicine.model.StockInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 주식 종목 정보 Repository
 *
 * 한국거래소 상장 종목 데이터 조회
 */
@Repository
public interface StockInfoRepository extends JpaRepository<StockInfo, Long> {

    /**
     * 종목코드로 검색
     *
     * @param stockCode 종목코드 (예: 005930)
     * @return 종목 정보
     */
    Optional<StockInfo> findByStockCode(String stockCode);

    /**
     * 종목명 완전 일치 검색
     *
     * @param stockName 종목명 (예: 삼성전자)
     * @return 종목 정보
     */
    Optional<StockInfo> findByStockName(String stockName);

    /**
     * 종목명 LIKE 검색 (부분 일치)
     * - 한글 키워드로 검색 시 사용
     * - 대소문자 구분 없음
     *
     * @param keyword 검색 키워드 (예: 삼성, 전자)
     * @return 검색 결과 리스트 (최대 10개)
     */
    @Query("SELECT s FROM StockInfo s WHERE LOWER(s.stockName) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY s.stockName")
    List<StockInfo> findByStockNameContaining(@Param("keyword") String keyword);

    /**
     * 종목명 LIKE 검색 with 결과 개수 제한
     * - 검색 성능 최적화
     *
     * @param keyword 검색 키워드
     * @param limit 최대 결과 개수
     * @return 검색 결과 리스트
     */
    @Query(value = "SELECT * FROM stock_info WHERE stock_name ILIKE CONCAT('%', :keyword, '%') ORDER BY stock_name LIMIT :limit", nativeQuery = true)
    List<StockInfo> findByStockNameContainingWithLimit(@Param("keyword") String keyword, @Param("limit") int limit);

    /**
     * 시장별 종목 조회
     *
     * @param marketCode 시장코드 (KOSPI, KOSDAQ, KONEX)
     * @return 종목 리스트
     */
    List<StockInfo> findByMarketCode(String marketCode);

    /**
     * 시장별 + 종목명 검색
     *
     * @param marketCode 시장코드
     * @param keyword 검색 키워드
     * @param limit 최대 결과 개수
     * @return 검색 결과 리스트
     */
    @Query(value = "SELECT * FROM stock_info WHERE market_code = :marketCode AND stock_name ILIKE CONCAT('%', :keyword, '%') ORDER BY stock_name LIMIT :limit", nativeQuery = true)
    List<StockInfo> findByMarketCodeAndStockNameContaining(
            @Param("marketCode") String marketCode,
            @Param("keyword") String keyword,
            @Param("limit") int limit
    );

    /**
     * 전체 종목 수 조회
     *
     * @return 종목 개수
     */
    @Query("SELECT COUNT(s) FROM StockInfo s")
    long countAllStocks();

    /**
     * 시장별 종목 수 조회
     *
     * @param marketCode 시장코드
     * @return 종목 개수
     */
    long countByMarketCode(String marketCode);
}
