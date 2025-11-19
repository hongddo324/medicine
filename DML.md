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
