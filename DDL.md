# DDL (Data Definition Language)

## 1. Daily 테이블 생성

일상 게시물을 저장하는 메인 테이블입니다.

```sql
CREATE TABLE daily (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    content TEXT,
    media_url TEXT,
    media_type VARCHAR(10),
    likes_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_created_at (created_at DESC),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

## 2. DailyComment 테이블 생성

일상 게시물의 댓글 및 대댓글을 저장하는 테이블입니다.

```sql
CREATE TABLE daily_comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    daily_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    parent_comment_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (daily_id) REFERENCES daily(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_comment_id) REFERENCES daily_comment(id) ON DELETE CASCADE,
    INDEX idx_daily_id (daily_id),
    INDEX idx_parent_comment_id (parent_comment_id),
    INDEX idx_created_at (created_at ASC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

## 3. DailyLike 테이블 생성

일상 게시물의 좋아요를 저장하는 테이블입니다.

```sql
CREATE TABLE daily_like (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    daily_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (daily_id) REFERENCES daily(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    UNIQUE KEY uk_daily_user (daily_id, user_id),
    INDEX idx_daily_id (daily_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

## 설명

### Daily 테이블
- **id**: 게시물 고유 ID (자동 증가)
- **user_id**: 작성자 ID (User 테이블 참조)
- **content**: 게시물 내용 (텍스트)
- **media_url**: 이미지/영상 파일 경로
- **media_type**: 미디어 타입 (IMAGE, VIDEO)
- **likes_count**: 좋아요 개수
- **created_at**: 생성 시간
- **updated_at**: 수정 시간

### DailyComment 테이블
- **id**: 댓글 고유 ID (자동 증가)
- **daily_id**: 게시물 ID (Daily 테이블 참조)
- **user_id**: 댓글 작성자 ID (User 테이블 참조)
- **content**: 댓글 내용
- **parent_comment_id**: 부모 댓글 ID (대댓글인 경우, DailyComment 테이블 자기 참조)
- **created_at**: 생성 시간

### DailyLike 테이블
- **id**: 좋아요 고유 ID (자동 증가)
- **daily_id**: 게시물 ID (Daily 테이블 참조)
- **user_id**: 좋아요 누른 사용자 ID (User 테이블 참조)
- **created_at**: 생성 시간
- **UNIQUE KEY**: 한 사용자가 같은 게시물에 중복 좋아요 방지

## 인덱스

- **Daily**: created_at (내림차순) - 최신 게시물 조회 최적화
- **Daily**: user_id - 특정 사용자 게시물 조회 최적화
- **DailyComment**: daily_id - 특정 게시물의 댓글 조회 최적화
- **DailyComment**: parent_comment_id - 대댓글 조회 최적화
- **DailyComment**: created_at (오름차순) - 댓글 순서대로 조회 최적화
- **DailyLike**: daily_id, user_id - 좋아요 조회 및 중복 방지 최적화

## 4. Wish 테이블 생성

위시리스트 아이템을 저장하는 테이블입니다.

```sql
CREATE TABLE wish (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    category VARCHAR(20) NOT NULL,
    latitude DECIMAL(10, 7),
    longitude DECIMAL(10, 7),
    address VARCHAR(500),
    image_url TEXT,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at DESC),
    INDEX idx_category (category),
    INDEX idx_completed (completed)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

## 5. WishSchedule 테이블 생성

위시리스트와 연결된 일정을 저장하는 테이블입니다.

```sql
CREATE TABLE wish_schedule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    wish_id BIGINT NOT NULL,
    scheduled_date TIMESTAMP NOT NULL,
    title VARCHAR(200),
    description TEXT,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (wish_id) REFERENCES wish(id) ON DELETE CASCADE,
    INDEX idx_wish_id (wish_id),
    INDEX idx_scheduled_date (scheduled_date ASC),
    INDEX idx_completed (completed)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### Wish 테이블
- **id**: 위시 고유 ID (자동 증가)
- **user_id**: 작성자 ID (User 테이블 참조)
- **title**: 위시 제목
- **description**: 위시 설명
- **category**: 카테고리 (RESTAURANT, TRAVEL, SHOPPING, CAFE, ACTIVITY, OTHER)
- **latitude**: 위도 (Naver Map API)
- **longitude**: 경도 (Naver Map API)
- **address**: 주소
- **image_url**: 이미지 파일 경로
- **completed**: 완료 여부
- **created_at**: 생성 시간
- **updated_at**: 수정 시간

### WishSchedule 테이블
- **id**: 일정 고유 ID (자동 증가)
- **wish_id**: 위시 ID (Wish 테이블 참조)
- **scheduled_date**: 일정 날짜 및 시간
- **title**: 일정 제목
- **description**: 메모
- **completed**: 완료 여부
- **created_at**: 생성 시간

### 인덱스
- **Wish**: user_id - 특정 사용자 위시리스트 조회 최적화
- **Wish**: created_at (내림차순) - 최신 위시 조회 최적화
- **Wish**: category - 카테고리별 조회 최적화
- **Wish**: completed - 완료/미완료 필터링 최적화
- **WishSchedule**: wish_id - 특정 위시의 일정 조회 최적화
- **WishSchedule**: scheduled_date (오름차순) - 일정 순서대로 조회 최적화
- **WishSchedule**: completed - 완료/미완료 필터링 최적화

## 6. Activity 테이블 생성

사용자 활동 알림을 저장하는 테이블입니다.

```sql
CREATE TABLE activity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    activity_type VARCHAR(20) NOT NULL,
    user_id BIGINT NOT NULL,
    message TEXT,
    reference_id BIGINT,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_created_at (created_at DESC),
    INDEX idx_is_read (is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### Activity 테이블
- **id**: 활동 고유 ID (자동 증가)
- **activity_type**: 활동 유형 (COMMENT, COMMENT_REPLY, DAILY_POST, DAILY_COMMENT, DAILY_LIKE, WISH_ADDED, SCHEDULE_ADDED)
- **user_id**: 활동 수행자 ID (User 테이블 참조)
- **message**: 활동 메시지
- **reference_id**: 참조 ID (댓글, 일상, 위시 등의 ID)
- **is_read**: 읽음 여부
- **created_at**: 생성 시간

### 인덱스
- **Activity**: created_at (내림차순) - 최신 활동 조회 최적화
- **Activity**: is_read - 읽지 않은 활동 조회 최적화

## 7. 기존 데이터 업데이트

### User 테이블의 points가 null인 데이터를 0으로 업데이트

기존에 생성된 사용자 중 points가 null인 경우 0으로 초기화합니다.

```sql
UPDATE users SET points = 0 WHERE points IS NULL;
```
