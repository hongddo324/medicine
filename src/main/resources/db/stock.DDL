-- ==========================================
-- 한국 주식 종목 정보 테이블
-- ==========================================
-- Purpose: 한국거래소(KRX) 상장 종목 정보 저장
-- Usage: 사용자 키워드 검색 시 종목코드 매핑용
-- ==========================================

CREATE TABLE IF NOT EXISTS stock_info (
    id BIGSERIAL PRIMARY KEY,
    stock_code VARCHAR(20) NOT NULL UNIQUE,     -- 종목코드 (예: 005930)
    stock_name VARCHAR(200) NOT NULL,            -- 종목명 (예: 삼성전자)
    market_code VARCHAR(20) NOT NULL,            -- 시장구분 (KOSPI, KOSDAQ, KONEX)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 종목명 검색 성능 향상을 위한 인덱스
CREATE INDEX IF NOT EXISTS idx_stock_info_name ON stock_info(stock_name);

-- 종목코드 검색 최적화 (UNIQUE 제약조건으로 자동 생성되지만 명시)
CREATE INDEX IF NOT EXISTS idx_stock_info_code ON stock_info(stock_code);

-- 시장구분별 조회 최적화
CREATE INDEX IF NOT EXISTS idx_stock_info_market ON stock_info(market_code);

-- LIKE 검색 최적화를 위한 GIN 인덱스 (PostgreSQL 전문 검색)
-- 한글 검색 성능 향상
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX IF NOT EXISTS idx_stock_info_name_trgm ON stock_info USING gin (stock_name gin_trgm_ops);

-- 코멘트 추가
COMMENT ON TABLE stock_info IS '한국거래소 상장 종목 정보';
COMMENT ON COLUMN stock_info.stock_code IS '종목코드 (6자리)';
COMMENT ON COLUMN stock_info.stock_name IS '종목명 (한글)';
COMMENT ON COLUMN stock_info.market_code IS '시장구분 (KOSPI/KOSDAQ/KONEX)';
