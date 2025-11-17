# 변경사항 요약

## 완료된 작업

### 1. Firebase Push 알림 문제 수정 ✅

#### 중복 알림 문제
- **원인**: `service-worker.js`와 `firebase-messaging-sw.js` 둘 다 푸시 메시지를 처리
- **해결**: `service-worker.js`에서 푸시 핸들러 제거, `firebase-messaging-sw.js`에서만 처리

#### URL 경로 문제
- **원인**: 상대 경로 사용으로 인한 404 오류
- **해결**: 절대 경로 사용 (`https://www.hongddo.top`)
- **파일**: `src/main/resources/static/firebase-messaging-sw.js`

### 2. PostgreSQL 데이터베이스 마이그레이션 ✅

#### 의존성 추가
- `build.gradle`에 PostgreSQL 및 JPA 의존성 추가
  - `spring-boot-starter-data-jpa`
  - `postgresql:42.7.1`

#### 데이터베이스 설정
- `application.yml`에 PostgreSQL 설정 추가
  - Connection Pool (HikariCP)
  - JPA/Hibernate 설정

#### 엔티티 변환
기존 Redis 기반 모델을 JPA 엔티티로 변환:

1. **User 엔티티**
   - ID: `String` → `Long` (BIGSERIAL)
   - 테이블: `users`
   - 추가 필드: `createdAt`, `updatedAt`

2. **Comment 엔티티**
   - ID: `String` → `Long`
   - 테이블: `comments`, `comment_likes`
   - User와 ManyToOne 관계 설정
   - 대댓글 지원 (self-referencing)

3. **MealCheck 엔티티**
   - ID: `String` → `Long`
   - 테이블: `meal_checks`
   - uploadedBy: User와 ManyToOne 관계

4. **MedicineRecord 엔티티**
   - ID: `String` → `Long`
   - 테이블: `medicine_records`
   - takenBy: User와 ManyToOne 관계

#### Repository 변환
- `CrudRepository` → `JpaRepository`
- ID 타입: `String` → `Long`
- 페이징, 정렬, 검색 기능 추가

#### DDL 문서
- `DATABASE_SCHEMA.md` 생성
- 모든 테이블의 DDL 포함
- 인덱스, 제약조건, 코멘트 포함

### 3. Redis 용도 변경 ✅
- **기존**: 모든 데이터 저장 (User, Comment, MealCheck, MedicineRecord 등)
- **변경 후**: 세션 관리 및 알림 설정만 사용
- FcmToken, PushSubscription은 Redis에 유지

## 진행 중인 작업

### 4. 모바일 친화적 UI 개편 🚧

#### 계획된 변경사항
1. **하단 네비게이션 바** (5개 탭)
   - 홈: 로그인 정보, 댓글/응원 기능 (페이징, NEW 뱃지)
   - 약복용: 기존 기능 + 달력 뷰
   - 식단관리: 업로드 기능
   - 포인트: 추후 기능 추가 예정
   - 설정: 프로필 변경, 비밀번호 변경, Push 알림 설정

2. **댓글 기능 개선**
   - 프로필 썸네일 클릭 시 모달로 원본 사진 표시
   - 10개 이상 페이징 처리
   - 새 댓글 NEW 뱃지

#### 파일
- `src/main/resources/templates/medicine.html`

## 다음 단계

### Service 레이어 수정 필요
다음 Service 파일들을 JPA 엔티티와 호환되도록 수정 필요:
- `UserService.java`
- `CommentService.java`
- `MealCheckService.java`
- `MedicineService.java`

주요 변경사항:
- ID 타입: `String` → `Long`
- 엔티티 관계 처리 (User 객체 참조)
- 트랜잭션 관리

### 데이터 마이그레이션
Redis에 저장된 기존 데이터를 PostgreSQL로 마이그레이션하는 스크립트 필요

### 환경 변수 설정
다음 환경 변수 설정 필요:
```bash
DB_HOST=localhost
DB_PORT=5432
DB_NAME=medicine
DB_USERNAME=postgres
DB_PASSWORD=your_password
```

## 주의사항

1. **데이터 백업**: 마이그레이션 전 Redis 데이터 백업 필수
2. **타입 변경**: ID가 String에서 Long으로 변경되어 기존 데이터 변환 필요
3. **테스트**: 변경 후 충분한 테스트 필요

## 파일 변경 내역

### 수정된 파일
- `build.gradle`
- `src/main/resources/application.yml`
- `src/main/java/com/medicine/model/*.java` (4개 파일)
- `src/main/java/com/medicine/repository/*.java` (4개 파일)
- `src/main/resources/static/firebase-messaging-sw.js`
- `src/main/resources/static/service-worker.js`

### 새로 추가된 파일
- `DATABASE_SCHEMA.md`
- `src/main/resources/templates/medicine.html.backup`

## 참고사항

### PostgreSQL 테이블 생성
애플리케이션 실행 시 JPA가 자동으로 테이블 생성 (`ddl-auto: update`)

Production 환경에서는 `ddl-auto: validate`로 변경하고 직접 DDL 실행 권장

### Redis vs PostgreSQL
- **Redis**: 세션, FcmToken, PushSubscription (빠른 액세스 필요)
- **PostgreSQL**: User, Comment, MealCheck, MedicineRecord (영구 저장 필요)
