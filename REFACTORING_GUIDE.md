# 🎯 프로젝트 리팩토링 가이드

> 📅 작성일: 2025-01-22
> 📝 작성자: Senior Fullstack Developer (Claude)

## 📌 목차

1. [기능 1: 최신글 클릭 시 일상 탭 전환 + 모달 오픈](#기능-1-최신글-클릭-시-일상-탭-전환--모달-오픈)
2. [기능 2: 게시물 자동 갱신 알고리즘 개선](#기능-2-게시물-자동-갱신-알고리즘-개선)
3. [기능 3: 위시 탭 이미지 선택 UI 축소](#기능-3-위시-탭-이미지-선택-ui-축소)
4. [기능 4: 갤럭시 폴드/테블릿 반응형 이미지](#기능-4-갤럭시-폴드테블릿-반응형-이미지)
5. [테스트 가이드](#테스트-가이드)

---

## 기능 1: 최신글 클릭 시 일상 탭 전환 + 모달 오픈

### 📖 개요
메인 화면의 "최근 일상" 섹션에서 게시글을 클릭하면, 자동으로 일상 탭으로 전환되고 해당 게시물의 상세 모달이 바로 표시되도록 개선

### ✅ 구현 내용

#### 1. HTML 구조 변경
```html
<!-- 기존 -->
<div class="comment-item" onclick="switchTab('dailyTab')">...</div>

<!-- 개선 후 -->
<div class="comment-item" data-post-id="${daily.id}" onclick="openDailyFromHome(${daily.id})">...</div>
```

#### 2. JavaScript 함수 추가
```javascript
/**
 * 홈 탭에서 최신 일상 게시글 클릭 시 호출
 * @param {number} postId - 게시물 ID
 */
function openDailyFromHome(postId) {
    // 1. 일상 탭으로 전환
    switchTab('dailyTab');

    // 2. 게시글이 로드되지 않았으면 먼저 로드
    if (!allDailies || allDailies.length === 0) {
        loadDailies().then(() => {
            setTimeout(() => {
                showDailyDetailModal(postId);
            }, 300);
        });
    } else {
        // 3. 이미 로드된 경우 바로 모달 오픈
        setTimeout(() => {
            showDailyDetailModal(postId);
        }, 300);
    }
}
```

#### 3. URL 파라미터 지원
```javascript
// URL: ?tab=daily&postId=123
function handleUrlParameters() {
    const urlParams = new URLSearchParams(window.location.search);
    const tabParam = urlParams.get('tab');
    const postIdParam = urlParams.get('postId');

    if (tabParam === 'daily' && postIdParam) {
        switchTab('dailyTab');
        loadDailies().then(() => {
            setTimeout(() => {
                const postId = parseInt(postIdParam);
                showDailyDetailModal(postId);
            }, 500);
        });

        // URL에서 파라미터 제거
        window.history.replaceState({}, '', window.location.pathname);
    }
}
```

### 🎯 사용 예시
```javascript
// 1. 최신글 클릭
// 사용자가 홈 화면에서 게시물 클릭 → 자동으로 일상 탭 + 모달 오픈

// 2. 공유 링크로 접근
// https://yourapp.com/?tab=daily&postId=123 → 자동으로 해당 게시물 모달 표시
```

---

## 기능 2: 게시물 자동 갱신 알고리즘 개선

### 📖 개요
기존의 전체 페이지 새로고침 방식을 증분 갱신으로 변경하여, 사용자의 스크롤 위치, 검색 상태, 열린 모달을 유지하면서 새 게시물만 추가

### 🔴 기존 문제점
```javascript
// ❌ 기존 방식: 주기적으로 전체 리로드
setInterval(() => {
    loadDailies();  // 전체 리스트 재로딩 → 스크롤/검색 상태 초기화
}, 10000);
```

- ❌ 사용자가 "더보기"로 과거 글을 보고 있어도 초기화
- ❌ 검색 중이어도 검색 결과 사라짐
- ❌ 모달이 열려있어도 닫힘
- ❌ 스크롤 위치 맨 위로 이동

### ✅ 개선 방안

#### 1. 증분 갱신 (Incremental Update)
```javascript
let latestDailyId = 0;       // 마지막으로 로드한 게시물 ID
let newDailiesCount = 0;     // 새로 도착한 게시물 수
let pendingDailies = [];     // 대기 중인 새 게시물들

/**
 * 주기적으로 새 게시물만 확인 (10초마다)
 */
async function checkNewDailies() {
    try {
        // 현재 최신 게시물 ID 가져오기
        if (allDailies && allDailies.length > 0) {
            latestDailyId = Math.max(...allDailies.map(d => d.id));
        }

        // 서버에서 해당 ID 이후의 게시물만 조회
        const response = await fetch(`/api/daily?since=${latestDailyId}`);
        const newDailies = await response.json();

        if (newDailies.length > 0) {
            pendingDailies = newDailies;
            newDailiesCount = newDailies.length;
            showNewPostsBanner();  // 배너 표시
        }
    } catch (error) {
        console.error('새 게시물 확인 실패:', error);
    }
}
```

#### 2. 배너 방식 알림
```javascript
/**
 * 새 게시물 알림 배너 표시 (자동 반영 X)
 */
function showNewPostsBanner() {
    const banner = document.createElement('div');
    banner.id = 'newPostsBanner';
    banner.style.cssText = `
        position: sticky;
        top: 0;
        background: linear-gradient(135deg, #4A90E2 0%, #357abd 100%);
        color: white;
        padding: 12px 16px;
        text-align: center;
        cursor: pointer;
        z-index: 100;
        border-radius: 12px;
        margin-bottom: 12px;
    `;
    banner.innerHTML = `
        <i class="bi bi-arrow-up-circle-fill"></i>
        새 글 ${newDailiesCount}개가 있습니다. 클릭하여 새로고침
    `;
    banner.onclick = applyNewDailies;  // 클릭 시에만 반영

    const grid = document.getElementById('dailyPostsGrid');
    grid.insertBefore(banner, grid.firstChild);
}
```

#### 3. 사용자 클릭 시 반영
```javascript
/**
 * 사용자가 배너 클릭 시 새 게시물 반영
 */
function applyNewDailies() {
    // 새 게시물을 기존 리스트 앞에 추가
    allDailies = [...pendingDailies, ...allDailies];

    // 배너 제거
    document.getElementById('newPostsBanner')?.remove();

    // 리스트 재렌더링 (스크롤 위치 자동 유지됨)
    renderDailies();

    // 초기화
    pendingDailies = [];
    newDailiesCount = 0;

    showToast('새 글이 추가되었습니다.', 'success');
}
```

#### 4. 백엔드 API 수정
```java
// DailyController.java

/**
 * 모든 일상 게시물 조회 (증분 갱신 지원)
 * @param since 선택적 파라미터: 이 ID 이후의 게시물만 조회
 */
@GetMapping
public ResponseEntity<?> getAllDailies(
        @RequestParam(required = false) Long since,
        HttpSession session) {

    User user = (User) session.getAttribute("user");
    if (user == null) {
        return ResponseEntity.status(401).body(Map.of("error", "인증되지 않은 사용자입니다."));
    }

    List<Daily> dailies;

    // since 파라미터가 있으면 해당 ID 이후의 게시물만 조회
    if (since != null && since > 0) {
        dailies = dailyService.getAllDailies().stream()
                .filter(daily -> daily.getId() > since)
                .toList();
    } else {
        dailies = dailyService.getAllDailies();
    }

    // ... 나머지 로직
}
```

### 📊 비교표

| 구분 | 기존 방식 | 개선 방식 |
|------|----------|----------|
| **갱신 방법** | 전체 리로드 | 증분 갱신 (새 글만) |
| **스크롤 위치** | ❌ 초기화됨 | ✅ 유지됨 |
| **검색 상태** | ❌ 사라짐 | ✅ 유지됨 |
| **열린 모달** | ❌ 닫힘 | ✅ 유지됨 |
| **사용자 경험** | ❌ 불편함 | ✅ 자연스러움 |
| **네트워크** | 전체 데이터 전송 | 새 데이터만 전송 |

---

## 기능 3: 위시 탭 이미지 선택 UI 축소

### 📖 개요
위시 탭의 큰 이미지 선택 버튼을 작은 카메라 아이콘으로 변경하여 공간 효율성 개선

### 🔴 기존 UI
```html
<div style="margin-bottom:16px">
    <label style="display:block;margin-bottom:8px;font-weight:600">이미지 (선택)</label>
    <input type="file" id="wishImage" accept="image/*" style="display:none">
    <button type="button" class="comment-submit-btn" onclick="..." style="width:100%">
        <i class="bi bi-image-fill"></i> 이미지 선택
    </button>
</div>
```
- ❌ 버튼이 전체 너비를 차지
- ❌ 수직 공간 많이 차지
- ❌ 디자인적으로 부담스러움

### ✅ 개선 UI
```html
<div style="margin-bottom:16px">
    <label style="display:flex;align-items:center;gap:8px">
        <span style="font-weight:600">카메라 및 사진</span>
        <button type="button" class="image-select-icon-btn" onclick="..." title="이미지 선택">
            <i class="bi bi-camera-fill"></i>
        </button>
    </label>
    <input type="file" id="wishImage" accept="image/*" style="display:none">
    <div id="wishImagePreview" style="margin-top:8px;display:none"></div>
    <div id="wishImageFileName" style="font-size:12px;color:var(--gray-600);margin-top:4px;display:none"></div>
</div>
```

### 🎨 CSS 스타일
```css
.image-select-icon-btn {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    background: var(--white);
    color: var(--primary);
    border: 2px solid var(--gray-300);
    display: inline-flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: all 0.3s;
    font-size: 16px;
    padding: 0;
}

.image-select-icon-btn:hover {
    background: var(--primary);
    color: white;
    border-color: var(--primary);
    transform: scale(1.1);
}

.image-select-icon-btn:active {
    transform: scale(0.95);
}

/* 다크모드 지원 */
body.dark-mode .image-select-icon-btn {
    background: var(--gray-100);
    border-color: var(--gray-600);
}

body.dark-mode .image-select-icon-btn:hover {
    background: var(--primary);
    color: white;
}
```

### 📱 사용 예시
```javascript
function handleWishImageSelect(event) {
    const file = event.target.files[0];
    const fileNameDiv = document.getElementById('wishImageFileName');
    const previewDiv = document.getElementById('wishImagePreview');

    if (file) {
        // 파일명 표시
        fileNameDiv.textContent = `선택됨: ${file.name}`;
        fileNameDiv.style.display = 'block';

        // 미리보기 이미지
        const reader = new FileReader();
        reader.onload = function(e) {
            previewDiv.innerHTML = `
                <img src="${e.target.result}" style="max-width:100%;max-height:200px;border-radius:8px">
            `;
            previewDiv.style.display = 'block';
        };
        reader.readAsDataURL(file);
    }
}
```

---

## 기능 4: 갤럭시 폴드/테블릿 반응형 이미지

### 📖 개요
다양한 화면 비율(갤럭시 폴드, 테블릿 등)에서 이미지가 자연스럽게 표시되도록 반응형 스타일 적용

### 🔴 기존 문제점
- ❌ 고정 비율로 인해 특이한 화면에서 이미지가 심하게 잘림
- ❌ 갤럭시 Z Fold 펼침 모드에서 UI 깨짐
- ❌ 테블릿 가로 모드에서 비율 어색함

### ✅ 개선 전략

#### 1. aspect-ratio 사용
```css
/* 기본: 일반 모바일 (16:9) */
.daily-thumbnail-container {
    width: 100%;
    aspect-ratio: 16 / 9;  /* 비율 고정 */
    overflow: hidden;
    border-radius: 12px;
    background: var(--gray-100);
}

.daily-thumbnail-image {
    width: 100%;
    height: 100%;
    object-fit: cover;  /* 비율 유지하며 컨테이너 채우기 */
}
```

#### 2. 미디어 쿼리로 기기별 최적화
```css
/* 작은 화면 (갤럭시 Z Fold 접힘 모드) */
@media (max-width: 374px) {
    .daily-thumbnail-container {
        aspect-ratio: 4 / 3;
    }

    .recent-daily-thumbnail {
        width: 50px;
        height: 50px;
    }
}

/* 테블릿 */
@media (min-width: 768px) {
    .daily-thumbnail-container {
        aspect-ratio: 3 / 2;
    }

    .recent-daily-thumbnail {
        width: 80px;
        height: 80px;
    }
}

/* 갤럭시 Z Fold 펼침 모드 */
@media (min-width: 700px) and (max-width: 900px) {
    .daily-thumbnail-container {
        aspect-ratio: 16 / 10;
    }
}
```

#### 3. object-fit 전략

| 속성 | 설명 | 장점 | 단점 | 사용 케이스 |
|------|------|------|------|------------|
| **cover** | 비율 유지하며 컨테이너 채움 | 여백 없음 | 일부 잘림 | 썸네일, 프로필 |
| **contain** | 전체 이미지 표시 | 잘림 없음 | 여백 생김 | 모달, 상세보기 |

```css
/* 썸네일: cover 사용 */
.thumbnail-image {
    object-fit: cover;
}

/* 상세보기 모달: contain 사용 */
.modal-image {
    object-fit: contain;
}
```

### 📊 반응형 전략 정리

| 디바이스 | 화면 크기 | 썸네일 비율 | object-fit |
|---------|----------|------------|------------|
| 일반 모바일 (세로) | ~374px | 4:3 | cover |
| 일반 모바일 | 375px~767px | 16:9 | cover |
| 갤럭시 Z Fold (접힘) | ~374px | 4:3 | cover |
| 갤럭시 Z Fold (펼침) | 700px~900px | 16:10 | cover |
| 테블릿 | 768px~1024px | 3:2 | cover |
| 데스크탑 | 1024px~ | 16:9 | cover |

### 🎯 적용 예시

#### HTML 구조
```html
<!-- 일상 게시물 썸네일 -->
<div class="daily-thumbnail-container">
    <img src="image.jpg" class="daily-thumbnail-image" alt="게시물 이미지">
</div>

<!-- 홈 최신글 썸네일 -->
<div class="recent-daily-thumbnail">
    <img src="image.jpg" alt="썸네일">
</div>
```

#### CSS 전체
```css
/* ========== 일상 게시물 썸네일 ========== */
.daily-thumbnail-container {
    width: 100%;
    aspect-ratio: 16 / 9;
    overflow: hidden;
    border-radius: 12px;
    background: var(--gray-100);
}

.daily-thumbnail-image {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.3s;
}

.daily-thumbnail-image:hover {
    transform: scale(1.05);
}

/* ========== 위시리스트 썸네일 ========== */
.wish-thumbnail-container {
    width: 100%;
    aspect-ratio: 4 / 3;
    overflow: hidden;
    border-radius: 12px;
}

.wish-thumbnail-image {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

/* ========== 홈 최신글 썸네일 ========== */
.recent-daily-thumbnail {
    width: 60px;
    height: 60px;
    border-radius: 8px;
    overflow: hidden;
    flex-shrink: 0;
}

.recent-daily-thumbnail img {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

/* ========== 미디어 쿼리 ========== */
@media (min-width: 768px) {
    .daily-thumbnail-container {
        aspect-ratio: 3 / 2;
    }

    .wish-thumbnail-container {
        aspect-ratio: 1 / 1;
    }

    .recent-daily-thumbnail {
        width: 80px;
        height: 80px;
    }
}

@media (max-width: 374px) {
    .daily-thumbnail-container {
        aspect-ratio: 4 / 3;
    }

    .wish-thumbnail-container {
        aspect-ratio: 3 / 2;
    }

    .recent-daily-thumbnail {
        width: 50px;
        height: 50px;
    }
}

@media (min-width: 700px) and (max-width: 900px) {
    .daily-thumbnail-container {
        aspect-ratio: 16 / 10;
    }

    .wish-thumbnail-container {
        aspect-ratio: 4 / 3;
    }
}
```

---

## 테스트 가이드

### ✅ 기능 1 테스트

1. **홈 화면에서 최신글 클릭**
   ```
   1. 홈 탭으로 이동
   2. "최근 일상" 섹션에서 게시물 클릭
   3. ✅ 일상 탭으로 자동 전환 확인
   4. ✅ 해당 게시물의 모달이 바로 열리는지 확인
   ```

2. **URL 파라미터로 직접 접근**
   ```
   1. 브라우저 주소창에 ?tab=daily&postId=123 입력
   2. ✅ 일상 탭 + 123번 게시물 모달이 열리는지 확인
   3. ✅ URL 파라미터가 자동으로 제거되는지 확인
   ```

### ✅ 기능 2 테스트

1. **배너 표시 확인**
   ```
   1. 일상 탭에서 대기
   2. 다른 사용자가 새 글 작성
   3. ✅ 10초 후 상단에 "새 글 N개" 배너가 나타나는지 확인
   ```

2. **스크롤 유지 확인**
   ```
   1. "더보기" 버튼을 여러 번 눌러 과거 글 표시
   2. 중간 위치로 스크롤
   3. 새 글 배너 클릭
   4. ✅ 스크롤 위치가 유지되는지 확인
   ```

3. **모달 유지 확인**
   ```
   1. 게시물 모달 열기
   2. 모달이 열려있는 상태로 10초 대기
   3. ✅ 자동 갱신이 일어나지 않는지 확인 (모달이 닫히지 않음)
   ```

### ✅ 기능 3 테스트

1. **아이콘 버튼 동작**
   ```
   1. 위시 탭 → "위시 추가" 버튼 클릭
   2. "카메라 및 사진" 옆의 카메라 아이콘 클릭
   3. ✅ 파일 선택 창이 열리는지 확인
   ```

2. **파일 선택 및 미리보기**
   ```
   1. 이미지 파일 선택
   2. ✅ 파일명이 "선택됨: filename.jpg" 형태로 표시되는지 확인
   3. ✅ 이미지 미리보기가 나타나는지 확인
   ```

### ✅ 기능 4 테스트

1. **갤럭시 Z Fold 테스트**
   ```
   1. Chrome DevTools → Responsive Design Mode
   2. 화면 크기를 280px (접힘) 으로 설정
   3. ✅ 썸네일이 4:3 비율로 표시되는지 확인
   4. 화면 크기를  768px (펼침) 으로 변경
   5. ✅ 썸네일이 16:10 비율로 표시되는지 확인
   ```

2. **테블릿 테스트**
   ```
   1. Chrome DevTools → iPad Pro 선택
   2. ✅ 썸네일이 3:2 비율로 표시되는지 확인
   3. ✅ 이미지가 잘리지 않고 자연스럽게 보이는지 확인
   ```

---

## 🎉 결론

### 주요 개선 사항 요약

| 기능 | 개선 전 | 개선 후 | 효과 |
|------|---------|---------|------|
| **최신글 클릭** | 탭만 전환 | 탭 + 모달 자동 오픈 | UX 개선 |
| **자동 갱신** | 전체 리로드 | 증분 갱신 | 상태 유지 |
| **이미지 선택 UI** | 큰 버튼 | 작은 아이콘 | 공간 효율 |
| **반응형 이미지** | 고정 비율 | 기기별 최적화 | 다양한 기기 지원 |

### 기술 스택
- **Frontend**: HTML5, ES6 JavaScript, Bootstrap 5
- **Backend**: Spring Boot, Java 17
- **Architecture**: PWA (Progressive Web App)

### 향후 개선 사항
- [ ] WebSocket 기반 실시간 갱신 (폴링 → 푸시)
- [ ] 이미지 레이지 로딩 (Intersection Observer)
- [ ] 가상 스크롤링 (무한 스크롤 성능 개선)

---

📌 **문의사항**: 추가 개선이 필요하거나 버그 발견 시 이슈 등록 부탁드립니다.
