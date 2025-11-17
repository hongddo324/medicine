# Database Schema (PostgreSQL)

이 문서는 Medicine Tracker 애플리케이션의 PostgreSQL 데이터베이스 스키마를 정의합니다.

## 개요

- **데이터베이스**: PostgreSQL 15+
- **Character Set**: UTF-8
- **Timezone**: Asia/Seoul

## 테이블 구조

### 1. users (사용자)

사용자 정보를 저장하는 테이블

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    display_name VARCHAR(100),
    profile_image TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_username ON users(username);

-- Comments
COMMENT ON TABLE users IS '사용자 정보';
COMMENT ON COLUMN users.id IS '사용자 ID (Primary Key)';
COMMENT ON COLUMN users.username IS '로그인 아이디';
COMMENT ON COLUMN users.password IS '암호화된 비밀번호';
COMMENT ON COLUMN users.role IS '사용자 역할 (ADMIN, FATHER, FAMILY, OTHER)';
COMMENT ON COLUMN users.display_name IS '표시 이름';
COMMENT ON COLUMN users.profile_image IS '프로필 사진 (URL 또는 Base64)';
COMMENT ON COLUMN users.created_at IS '생성 일시';
COMMENT ON COLUMN users.updated_at IS '수정 일시';
```

### 2. comments (댓글)

댓글 및 응원 메시지를 저장하는 테이블

```sql
CREATE TABLE comments (
    id BIGSERIAL PRIMARY KEY,
    content TEXT NOT NULL,
    image_url TEXT,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    parent_comment_id BIGINT,
    CONSTRAINT fk_comment_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_parent FOREIGN KEY (parent_comment_id) REFERENCES comments(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_user_id ON comments(user_id);
CREATE INDEX idx_created_at ON comments(created_at);

-- Comments
COMMENT ON TABLE comments IS '댓글/응원 메시지';
COMMENT ON COLUMN comments.id IS '댓글 ID (Primary Key)';
COMMENT ON COLUMN comments.content IS '댓글 내용';
COMMENT ON COLUMN comments.image_url IS '첨부 이미지 (Base64 또는 URL)';
COMMENT ON COLUMN comments.user_id IS '작성자 ID (FK)';
COMMENT ON COLUMN comments.created_at IS '작성 일시';
COMMENT ON COLUMN comments.parent_comment_id IS '부모 댓글 ID (대댓글인 경우)';
```

### 3. comment_likes (댓글 좋아요)

댓글에 대한 좋아요 정보를 저장하는 테이블

```sql
CREATE TABLE comment_likes (
    comment_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (comment_id, user_id),
    CONSTRAINT fk_like_comment FOREIGN KEY (comment_id) REFERENCES comments(id) ON DELETE CASCADE,
    CONSTRAINT fk_like_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Comments
COMMENT ON TABLE comment_likes IS '댓글 좋아요';
COMMENT ON COLUMN comment_likes.comment_id IS '댓글 ID (FK)';
COMMENT ON COLUMN comment_likes.user_id IS '좋아요를 누른 사용자 ID (FK)';
```

### 4. meal_checks (식단 관리)

식단 업로드 및 관리 정보를 저장하는 테이블

```sql
CREATE TABLE meal_checks (
    id BIGSERIAL PRIMARY KEY,
    date DATE NOT NULL,
    meal_type VARCHAR(20) NOT NULL,
    image_url TEXT,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    uploaded_by BIGINT NOT NULL,
    ai_evaluation TEXT,
    score INTEGER,
    CONSTRAINT fk_meal_user FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_meal_type CHECK (meal_type IN ('BREAKFAST', 'LUNCH', 'DINNER')),
    CONSTRAINT chk_score CHECK (score IS NULL OR (score >= 0 AND score <= 100))
);

-- Indexes
CREATE INDEX idx_date ON meal_checks(date);
CREATE INDEX idx_meal_type ON meal_checks(meal_type);
CREATE INDEX idx_uploaded_by ON meal_checks(uploaded_by);

-- Comments
COMMENT ON TABLE meal_checks IS '식단 관리';
COMMENT ON COLUMN meal_checks.id IS '식단 ID (Primary Key)';
COMMENT ON COLUMN meal_checks.date IS '식사 날짜';
COMMENT ON COLUMN meal_checks.meal_type IS '식사 유형 (BREAKFAST, LUNCH, DINNER)';
COMMENT ON COLUMN meal_checks.image_url IS '식단 이미지 (Base64 또는 URL)';
COMMENT ON COLUMN meal_checks.uploaded_at IS '업로드 일시';
COMMENT ON COLUMN meal_checks.uploaded_by IS '업로드한 사용자 ID (FK)';
COMMENT ON COLUMN meal_checks.ai_evaluation IS 'AI 평가 결과';
COMMENT ON COLUMN meal_checks.score IS 'AI 점수 (0-100)';
```

### 5. medicine_records (약 복용 기록)

약 복용 여부를 저장하는 테이블

```sql
CREATE TABLE medicine_records (
    id BIGSERIAL PRIMARY KEY,
    date DATE NOT NULL,
    medicine_type VARCHAR(20) NOT NULL,
    taken_time TIMESTAMP,
    taken_by BIGINT,
    taken BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_medicine_user FOREIGN KEY (taken_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_medicine_type CHECK (medicine_type IN ('MORNING', 'LUNCH', 'EVENING'))
);

-- Indexes
CREATE INDEX idx_date ON medicine_records(date);
CREATE INDEX idx_medicine_type ON medicine_records(medicine_type);
CREATE INDEX idx_taken_by ON medicine_records(taken_by);
CREATE UNIQUE INDEX idx_unique_medicine ON medicine_records(date, medicine_type);

-- Comments
COMMENT ON TABLE medicine_records IS '약 복용 기록';
COMMENT ON COLUMN medicine_records.id IS '기록 ID (Primary Key)';
COMMENT ON COLUMN medicine_records.date IS '날짜';
COMMENT ON COLUMN medicine_records.medicine_type IS '복용 시간 (MORNING, LUNCH, EVENING)';
COMMENT ON COLUMN medicine_records.taken_time IS '복용한 시간';
COMMENT ON COLUMN medicine_records.taken_by IS '복용한 사용자 ID (FK)';
COMMENT ON COLUMN medicine_records.taken IS '복용 여부';
COMMENT ON COLUMN medicine_records.created_at IS '생성 일시';
```

## 초기 데이터

### 기본 관리자 계정 생성

```sql
-- 비밀번호는 실제 환경에서 암호화되어야 합니다
INSERT INTO users (username, password, role, display_name, created_at, updated_at)
VALUES ('admin', '$2a$10$example_hashed_password', 'ADMIN', '관리자', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
```

## 마이그레이션 참고사항

### Redis에서 PostgreSQL로 마이그레이션

기존 Redis 데이터를 PostgreSQL로 마이그레이션할 때 주의사항:

1. **ID 타입 변경**: Redis의 String ID → PostgreSQL의 BIGSERIAL
2. **관계 설정**: User ID를 FK로 연결
3. **타임존**: LocalDateTime은 Asia/Seoul 기준
4. **인덱스**: 자주 조회되는 컬럼에 인덱스 추가

### 데이터 마이그레이션 순서

1. users 테이블 먼저 마이그레이션
2. comments 테이블 마이그레이션 (user_id FK 참조)
3. comment_likes 테이블 마이그레이션
4. meal_checks 테이블 마이그레이션 (uploaded_by FK 참조)
5. medicine_records 테이블 마이그레이션 (taken_by FK 참조)

## 성능 최적화

### 권장 설정

```sql
-- 통계 업데이트
ANALYZE users;
ANALYZE comments;
ANALYZE comment_likes;
ANALYZE meal_checks;
ANALYZE medicine_records;

-- Vacuum
VACUUM ANALYZE;
```

### 파티셔닝 고려사항

데이터가 많아지면 날짜 기반 파티셔닝을 고려:
- `meal_checks`: date 컬럼으로 월별 파티셔닝
- `medicine_records`: date 컬럼으로 월별 파티셔닝

## 백업 및 복구

### 백업

```bash
pg_dump -U postgres -d medicine > backup_$(date +%Y%m%d).sql
```

### 복구

```bash
psql -U postgres -d medicine < backup_20250117.sql
```

## 보안 고려사항

1. **비밀번호**: BCrypt 등으로 암호화 저장
2. **이미지**: Base64 대신 별도 스토리지 사용 권장 (S3 등)
3. **SQL Injection**: PreparedStatement 사용
4. **접근 권한**: 최소 권한 원칙 적용

## 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|-----------|
| 1.0 | 2025-01-17 | 초기 스키마 생성 |
