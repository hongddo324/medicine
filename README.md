# 약 복용 관리 시스템

아버지의 약 복용 여부를 확인하고 관리하기 위한 웹 애플리케이션입니다.

## 기술 스택

- **Backend**: Spring Boot 3.0.13
- **Database**: Redis (Session & Data Storage)
- **Frontend**: Thymeleaf + Bootstrap 5
- **Calendar**: FullCalendar
- **Build Tool**: Gradle 8.5
- **Java Version**: 17

## 주요 기능

### 1. 로그인 시스템
- Redis 기반 사용자 인증
- 4가지 권한 레벨: `admin`, `father`, `family`, `other`
- 세션 100일 자동 유지 (쿠키)
- 모든 로그인/접근 로그 기록

### 2. 약 복용 확인 페이지
- **상단**: 오늘 날짜 표시
- **중앙**: 약 복용 확인 버튼
  - 기본 상태: 빨간색 - "약 아직 복용전"
  - 복용 후: 초록색 - "금일 약 복용함"
  - `father` 권한만 클릭 가능
  - 클릭 시 확인 팝업 표시
- **하단**: 캘린더
  - 약 복용한 날짜 표시
  - 복용 시간 정보 포함
  - 월별 조회 가능

### 3. 로깅
- 접근 로그: `logs/access-{날짜}.log`
- 애플리케이션 로그: `logs/medicine-tracker-{날짜}.log`
- 날짜별 자동 로테이션
- 파일당 최대 10MB, 최대 30일 보관

## 설정

### Redis 연결 정보 (환경변수 사용)
```yaml
Host: ${REDIS_HOST}
Port: ${REDIS_PORT}
Password: ${REDIS_PASSWORD}
```

환경변수 예시:
- Windows (PowerShell)
  - `setx REDIS_HOST 127.0.0.1`
  - `setx REDIS_PORT 6379`
  - `setx REDIS_PASSWORD <비밀번호>`
- macOS/Linux (bash/zsh)
  - `export REDIS_HOST=127.0.0.1`
  - `export REDIS_PORT=6379`
  - `export REDIS_PASSWORD=<비밀번호>`

애플리케이션은 `application.yml`에서 환경변수를 참조하도록 설정되어 있습니다.

### 기본 사용자 계정

| 계정명 | 비밀번호 | 권한 | 설명 |
|--------|---------|------|------|
| admin | admin123 | ADMIN | 관리자 |
| father | father123 | FATHER | 아버지 (약 복용 기록 권한) |
| family | family123 | FAMILY | 가족 |
| guest | guest123 | OTHER | 게스트 |

## 빌드 및 실행

### 필수 요구사항
- JDK 17 이상
- Gradle 8.5 이상 (또는 포함된 Gradle Wrapper 사용)
- Redis 서버 (125.129.57.235:6379 접근 가능)

### 빌드
```bash
./gradlew clean build
```

Windows:
```bash
gradlew.bat clean build
```

### 실행
```bash
java -jar build/libs/medicine-tracker-1.0.0.jar
```

또는 개발 모드:
```bash
./gradlew bootRun
```

Windows:
```bash
gradlew.bat bootRun
```

### 접속
```
http://localhost:8080
```

## 프로젝트 구조

```
medicine/
├── src/
│   ├── main/
│   │   ├── java/com/medicine/
│   │   │   ├── MedicineTrackerApplication.java
│   │   │   ├── config/
│   │   │   │   ├── RedisConfig.java
│   │   │   │   └── WebConfig.java
│   │   │   ├── model/
│   │   │   │   ├── Role.java
│   │   │   │   ├── User.java
│   │   │   │   └── MedicineRecord.java
│   │   │   ├── repository/
│   │   │   │   ├── UserRepository.java
│   │   │   │   └── MedicineRecordRepository.java
│   │   │   ├── service/
│   │   │   │   ├── UserService.java
│   │   │   │   └── MedicineService.java
│   │   │   ├── controller/
│   │   │   │   ├── LoginController.java
│   │   │   │   └── MedicineController.java
│   │   │   └── interceptor/
│   │   │       └── AuthInterceptor.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── logback-spring.xml
│   │       └── templates/
│   │           ├── login.html
│   │           └── medicine.html
│   └── test/
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties
├── logs/ (자동 생성)
├── build.gradle
├── settings.gradle
├── gradlew
├── gradlew.bat
└── README.md
```

## 추가 구현 사항

### 보안
1. **세션 보안**: HTTP-Only 쿠키 사용으로 XSS 공격 방지
2. **접근 제어**: Interceptor를 통한 페이지별 권한 검증
3. **비밀번호**: 현재는 평문 저장 (실제 운영 시 BCrypt 암호화 권장)

### 사용자 경험
1. **반응형 디자인**: Bootstrap 5를 활용한 모바일 친화적 UI
2. **직관적인 색상**: 빨간색(미복용), 초록색(복용) 명확한 시각적 피드백
3. **확인 팝업**: 실수로 인한 복용 기록 방지
4. **실시간 업데이트**: AJAX를 통한 페이지 새로고침 없는 상태 변경

### 데이터 관리
1. **자동 초기화**: 매일 자정에 자동으로 복용 상태 초기화 (날짜 기반)
2. **기록 보존**: 과거 복용 기록은 Redis에 영구 보관
3. **월별 조회**: 캘린더를 통한 월별 복용 기록 확인

### 로깅
1. **상세 로그**: 모든 로그인, 로그아웃, 약 복용 기록 이벤트 로깅
2. **구조화**: 접근 로그와 애플리케이션 로그 분리
3. **자동 관리**: 날짜별 로테이션 및 오래된 로그 자동 삭제

## 라이선스

이 프로젝트는 개인 사용 목적으로 제작되었습니다.
