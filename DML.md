# DML (Data Manipulation Language)

## 초기 데이터

일상(Daily) 기능은 사용자가 직접 생성하는 데이터이므로 별도의 초기 DML이 필요하지 않습니다.

## 참고용 쿼리 예시

### 1. 일상 게시물 조회 (최신순)

```sql
SELECT
    d.id,
    d.content,
    d.media_url,
    d.media_type,
    d.likes_count,
    d.created_at,
    u.id AS user_id,
    u.username,
    u.display_name,
    u.profile_image
FROM daily d
INNER JOIN user u ON d.user_id = u.id
ORDER BY d.created_at DESC;
```

### 2. 특정 게시물의 댓글 조회 (시간순)

```sql
SELECT
    dc.id,
    dc.content,
    dc.parent_comment_id,
    dc.created_at,
    u.id AS user_id,
    u.username,
    u.display_name,
    u.profile_image
FROM daily_comment dc
INNER JOIN user u ON dc.user_id = u.id
WHERE dc.daily_id = ?
ORDER BY dc.created_at ASC;
```

### 3. 특정 사용자가 좋아요한 게시물 조회

```sql
SELECT
    d.id,
    d.content,
    d.media_url,
    d.media_type,
    d.likes_count,
    d.created_at,
    u.id AS user_id,
    u.username,
    u.display_name,
    u.profile_image
FROM daily d
INNER JOIN user u ON d.user_id = u.id
INNER JOIN daily_like dl ON d.id = dl.daily_id
WHERE dl.user_id = ?
ORDER BY dl.created_at DESC;
```

### 4. 좋아요 수가 많은 게시물 TOP 10

```sql
SELECT
    d.id,
    d.content,
    d.media_url,
    d.likes_count,
    d.created_at,
    u.username,
    u.display_name
FROM daily d
INNER JOIN user u ON d.user_id = u.id
ORDER BY d.likes_count DESC, d.created_at DESC
LIMIT 10;
```

### 5. 댓글이 많은 게시물 조회

```sql
SELECT
    d.id,
    d.content,
    d.likes_count,
    d.created_at,
    u.username,
    COUNT(dc.id) AS comment_count
FROM daily d
INNER JOIN user u ON d.user_id = u.id
LEFT JOIN daily_comment dc ON d.id = dc.daily_id
GROUP BY d.id
ORDER BY comment_count DESC, d.created_at DESC
LIMIT 10;
```

## 데이터 정리

### 오래된 미디어 파일 없는 게시물 삭제 (30일 이상)

```sql
DELETE FROM daily
WHERE media_url IS NULL
  AND content IS NULL
  AND created_at < DATE_SUB(NOW(), INTERVAL 30 DAY);
```

### 미아 댓글 정리 (부모 댓글이 삭제된 대댓글)

```sql
DELETE FROM daily_comment
WHERE parent_comment_id IS NOT NULL
  AND parent_comment_id NOT IN (SELECT id FROM daily_comment);
```

## 통계 쿼리

### 사용자별 게시물 통계

```sql
SELECT
    u.id,
    u.username,
    u.display_name,
    COUNT(d.id) AS post_count,
    SUM(d.likes_count) AS total_likes,
    MAX(d.created_at) AS last_post_date
FROM user u
LEFT JOIN daily d ON u.id = d.user_id
GROUP BY u.id
ORDER BY post_count DESC;
```

### 일별 게시물 및 좋아요 통계

```sql
SELECT
    DATE(created_at) AS post_date,
    COUNT(*) AS post_count,
    SUM(likes_count) AS total_likes
FROM daily
GROUP BY DATE(created_at)
ORDER BY post_date DESC;
```

## 위시리스트 쿼리

### 1. 특정 사용자의 위시리스트 조회 (최신순)

```sql
SELECT
    w.id,
    w.title,
    w.description,
    w.category,
    w.latitude,
    w.longitude,
    w.address,
    w.image_url,
    w.completed,
    w.created_at,
    u.id AS user_id,
    u.username,
    u.display_name
FROM wish w
INNER JOIN user u ON w.user_id = u.id
WHERE w.user_id = ?
ORDER BY w.created_at DESC;
```

### 2. 카테고리별 위시리스트 조회

```sql
SELECT
    w.id,
    w.title,
    w.description,
    w.category,
    w.address,
    w.completed,
    w.created_at,
    u.username,
    u.display_name
FROM wish w
INNER JOIN user u ON w.user_id = u.id
WHERE w.category = ?
ORDER BY w.created_at DESC;
```

### 3. 완료되지 않은 위시리스트 조회

```sql
SELECT
    w.id,
    w.title,
    w.description,
    w.category,
    w.address,
    w.created_at,
    u.username
FROM wish w
INNER JOIN user u ON w.user_id = u.id
WHERE w.completed = FALSE
ORDER BY w.created_at DESC;
```

### 4. 특정 기간의 일정 조회

```sql
SELECT
    ws.id,
    ws.scheduled_date,
    ws.title,
    ws.description,
    ws.completed,
    w.id AS wish_id,
    w.title AS wish_title,
    w.category,
    w.address,
    u.username
FROM wish_schedule ws
INNER JOIN wish w ON ws.wish_id = w.id
INNER JOIN user u ON w.user_id = u.id
WHERE ws.scheduled_date BETWEEN ? AND ?
ORDER BY ws.scheduled_date ASC;
```

### 5. 특정 사용자의 다가오는 일정 조회

```sql
SELECT
    ws.id,
    ws.scheduled_date,
    ws.title,
    ws.description,
    ws.completed,
    w.title AS wish_title,
    w.category,
    w.address
FROM wish_schedule ws
INNER JOIN wish w ON ws.wish_id = w.id
WHERE w.user_id = ?
  AND ws.scheduled_date >= NOW()
  AND ws.completed = FALSE
ORDER BY ws.scheduled_date ASC
LIMIT 10;
```

### 6. 카테고리별 위시 통계

```sql
SELECT
    category,
    COUNT(*) AS wish_count,
    SUM(CASE WHEN completed = TRUE THEN 1 ELSE 0 END) AS completed_count,
    SUM(CASE WHEN completed = FALSE THEN 1 ELSE 0 END) AS pending_count
FROM wish
GROUP BY category
ORDER BY wish_count DESC;
```

### 7. 사용자별 위시리스트 통계

```sql
SELECT
    u.id,
    u.username,
    u.display_name,
    COUNT(w.id) AS total_wishes,
    SUM(CASE WHEN w.completed = TRUE THEN 1 ELSE 0 END) AS completed_wishes,
    COUNT(ws.id) AS total_schedules,
    MAX(w.created_at) AS last_wish_date
FROM user u
LEFT JOIN wish w ON u.id = w.user_id
LEFT JOIN wish_schedule ws ON w.id = ws.wish_id
GROUP BY u.id
ORDER BY total_wishes DESC;
```

## 위시리스트 데이터 정리

### 완료된 오래된 위시 삭제 (1년 이상)

```sql
DELETE FROM wish
WHERE completed = TRUE
  AND updated_at < DATE_SUB(NOW(), INTERVAL 1 YEAR);
```

### 지난 일정 삭제 (완료된 일정 중 6개월 이상 경과)

```sql
DELETE FROM wish_schedule
WHERE completed = TRUE
  AND scheduled_date < DATE_SUB(NOW(), INTERVAL 6 MONTH);
```
