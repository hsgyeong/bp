# 자린고치 가계부 API 명세서

> 기준 DB: `user`, `category`, `transaction`, `weekly_budget`, `notification`, `refresh_token` (6개 테이블)
> Base URL: `/api`
> 인증: 로그인 후 발급된 토큰(JWT 등)을 `Authorization: Bearer <token>` 헤더로 전달
> 공통 응답 형식 / 상태 코드는 문서 하단 참고

---

## 1. 인증 / 사용자 (Auth & User)

### 1-1. 회원가입
- **POST** `/api/auth/signup`
- 인증: 불필요

| 요청 (Body) | 타입 | 필수 | 설명 |
|------|------|------|------|
| email | string | ✅ | 로그인 이메일 (중복 불가) |
| password | string | ✅ | 비밀번호 |
| nickname | string | ✅ | 표시 이름 |

**Response 201**
```json
{
  "code": "SUCCESS",
  "message": "회원가입이 완료되었습니다.",
  "data": {
    "id": 1,
    "email": "alice@test.com",
    "nickname": "앨리스",
    "createdAt": "2026-05-01T10:00:00",
    "deletedAt": null
  }
}
```
- 409: 이메일 중복

---

### 1-2. 로그인
- **POST** `/api/auth/login`
- 인증: 불필요

| 요청 | 타입 | 필수 |
|------|------|------|
| email | string | ✅ |
| password | string | ✅ |

**Response 200**
```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "tokenType": "Bearer",
  "email": "alice@test.com",
  "role": "ROLE_USER"
}
```
- 비고: 로그인 응답은 공통 `Response<T>`로 감싸지 않고 `LoginResponse`를 직접 반환한다.
- `accessToken`: API 요청 시 `Authorization: Bearer <accessToken>` 헤더에 사용한다.
- `refreshToken`: 로그아웃 시 서버 DB의 Refresh Token을 폐기하기 위해 요청 body로 보낸다.
- 401: 이메일/비밀번호 불일치

---

### 1-3. 로그아웃
- **POST** `/api/auth/logout`
- 인증: Access Token 불필요
- 비고: 클라이언트가 저장한 `refreshToken`을 서버로 보내면, 서버는 DB의 Refresh Token을 폐기한다. 클라이언트는 응답 성공 여부와 관계없이 로컬의 `accessToken`, `refreshToken`, `user` 정보를 삭제한다.

| 요청 (Body) | 타입 | 필수 | 설명 |
|------|------|------|------|
| refreshToken | string | ✅ | 로그인 응답으로 받은 Refresh Token |

**Request**
```json
{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

- **Response 200**
```json
{
  "code": "SUCCESS",
  "message": "로그아웃 되었습니다.",
  "data": null
}
```

---

### 1-4. 내 정보 조회
- **GET** `/api/users/me`
- 인증: 필요

**Response 200**
```json
{ "id": 1, "email": "alice@test.com", "nickname": "앨리스", "createdAt": "2026-05-01T10:00:00" }
```

---

### 1-5. 내 정보 수정 (닉네임/비밀번호)
- **PUT** `/api/users/me`
- 인증: 필요

| 요청 | 타입 | 필수 | 설명 |
|------|------|------|------|
| nickname | string | ❌ | 변경할 닉네임 |
| password | string | ❌ | 변경할 비밀번호 |

**Response 200** 수정된 사용자 정보

---

### 1-6. 회원 탈퇴
- **DELETE** `/api/users/me`
- 인증: 필요
- 처리: 실제 삭제 X → `deleted_at`에 현재 시각 기록 (soft delete)
- 비고: 회원 탈퇴 시 해당 사용자의 모든 Refresh Token도 서버 DB에서 폐기한다.
- **Response 200** `{ "message": "회원 탈퇴가 완료되었습니다." }`

---

## 2. 카테고리 (Category)

### 2-1. 카테고리 목록 조회
- **GET** `/api/categories`
- 인증: 필요
- 반환: 기본 제공(user_id=NULL) + 내가 추가한 것, `is_active=1`만

| 쿼리 파라미터 | 타입 | 필수 | 설명 |
|------|------|------|------|
| type | int | ✅ | 1=수입 / 2=지출 (가계부 화면이 수입/지출 탭으로 분리되어 항상 type별 조회) |

**Response 200**
```json
[
  { "id": 1, "name": "월급", "type": 1, "displayOrder": 1, "icon": "💰", "color": "#4CAF50", "isDefault": true },
  { "id": 8, "name": "의료비", "type": 2, "displayOrder": 5, "icon": "💊", "color": "#F44336", "isDefault": false }
]
```

---

### 2-2. 카테고리 추가
- **POST** `/api/categories`
- 인증: 필요 (user_id = 로그인 사용자)

| 요청 | 타입 | 필수 | 설명 |
|------|------|------|------|
| name | string | ✅ | 카테고리 이름 |
| type | int | ✅ | 1=수입 / 2=지출 |
| icon | string | ❌ | 아이콘 |
| color | string | ❌ | 색상 |
| displayOrder | int | ❌ | 표시 순서 |

**Response 201** 생성된 카테고리

---

### 2-3. 카테고리 수정
- **PUT** `/api/categories/{id}`
- 인증: 필요 (본인 카테고리만, 기본 제공 카테고리는 수정 불가)
- Body: name / type / icon / color / displayOrder
- **Response 200** 수정된 카테고리
- 403: 기본 제공 또는 타인 카테고리

---

### 2-4. 카테고리 삭제
- **DELETE** `/api/categories/{id}`
- 인증: 필요
- 처리: 실제 삭제 X → `is_active=0` (과거 거래 보존)
- **Response 200** `{ "message": "삭제되었습니다." }`

---

## 3. 거래 내역 (Transaction)

### 3-1. 거래 목록 조회
- **GET** `/api/transactions`
- 인증: 필요

| 쿼리 파라미터 | 타입 | 설명 |
|------|------|------|
| startDate | date | 조회 시작일 (YYYY-MM-DD) |
| endDate | date | 조회 종료일 |
| type | int | 1=수입 / 2=지출 |
| categoryId | int | 특정 카테고리 |
| keyword | string | 메모 또는 카테고리명 검색어 |
| sort | string | 정렬 기준: `date_desc`(최신순, 기본값), `date_asc`(오래된순), `amount_desc`(금액 높은순), `amount_asc`(금액 낮은순) |

**Request 예시**
```http
GET /api/transactions?startDate=2026-06-01&endDate=2026-06-30&keyword=식비&sort=amount_desc
```

**Response 200**
```json
[
  {
    "id": 4, "amount": 45000.00, "type": 2,
    "category": { "id": 4, "name": "식비", "icon": "🍚" },
    "memo": "저녁 회식", "date": "2026-06-03"
  }
]
```

---

### 3-2. 거래 단건 조회
- **GET** `/api/transactions/{id}`
- 인증: 필요
- **Response 200** 거래 상세
- 404: 없음 / 403: 타인 거래

---

### 3-3. 거래 등록
- **POST** `/api/transactions`
- 인증: 필요

| 요청 | 타입 | 필수 | 설명 |
|------|------|------|------|
| categoryId | int | ✅ | 카테고리 |
| amount | decimal | ✅ | 금액 |
| type | int | ✅ | 1=수입 / 2=지출 |
| memo | string | ❌ | 메모 |
| date | date | ✅ | 거래 일자 |

**Response 201** 생성된 거래
> ⚠️ 지출 등록 시 → 해당 주 예산 대비 비율 재계산 후, 임계치(25/50/75/100/125/150) 도달 시 알림 생성 (3-7 참고)

---

### 3-4. 거래 수정
- **PUT** `/api/transactions/{id}`
- 인증: 필요
- Body: categoryId / amount / type / memo / date
- **Response 200** 수정된 거래
> 수정으로 지출 비율이 바뀌면 알림 재평가

---

### 3-5. 거래 삭제
- **DELETE** `/api/transactions/{id}`
- 인증: 필요
- **Response 200** `{ "message": "삭제되었습니다." }`

---

## 4. 주간 예산 (Weekly Budget)

### 4-1. 현재 주 예산 조회 (지출 현황 포함)
- **GET** `/api/budgets/weekly/current`
- 인증: 필요
- 반환: 오늘이 속한 주의 예산 + 누적 지출 + 비율

**Response 200**
```json
{
  "id": 1,
  "amount": 300000.00,
  "startDate": "2026-06-01",
  "endDate": "2026-06-07",
  "spentMoney": 182250.00,
  "ratio": 60.75,
  "remaining": 117750.00,
  "updateCount": 0,
  "updatable": true
}
```

---

### 4-2. 최근 주간 예산 목록 (이번 주 포함 최근 4주)
- **GET** `/api/budgets/weekly/recent`
- 인증: 필요
- 파라미터 없음. **월과 무관하게** 오늘이 속한 주를 포함해 `start_date` 기준 **최근 4개** 주간예산을 반환 (미래 주 제외).
- 비고: 통계 '주·단순금액'(예산 vs 지출) 화면(§6)도 이 엔드포인트를 그대로 재사용한다. 각 항목에 `startDate`/`endDate`가 있어 차트 라벨을 실제 날짜범위로 찍을 수 있다.

**Response 200** 주간 예산 배열 (각 항목에 지출/비율 포함, 과거→현재 순)

---

### 4-3. 주간 예산 등록
- **POST** `/api/budgets/weekly`
- 인증: 필요

| 요청 | 타입 | 필수 | 설명 |
|------|------|------|------|
| amount | decimal | ✅ | 예산 한도 |
| startDate | date | ✅ | 주 시작일 |
| endDate | date | ✅ | 주 종료일 |

**Response 201** 생성된 예산
- 409: 해당 주 예산이 이미 존재

---

### 4-4. 주간 예산 수정
- **PUT** `/api/budgets/weekly/{id}`
- 인증: 필요
- 제약: **한 달에 2회까지만 수정 가능** (`update_count` 확인)
- 처리: 성공 시 `update_count += 1`, `updated_at` 갱신
- 비고: 예산 기준이 바뀌므로 **그 주 알림을 리셋 후 새 금액 기준으로 재평가**한다. 새 비율이 임계치(25~150)를 넘으면 최고 단계 1건이 새로 생성됨 (DEC-0017).
- **Response 200** 수정된 예산
- 403: 월 수정 횟수 초과 (`{ "error": "이번 달 예산 수정 횟수를 초과했습니다." }`)

> ℹ️ 예산은 **주 단위만** 제공한다. 월 단위 예산/요약 엔드포인트는 두지 않음(DEC-0009).
> 월 단위 *지출* 집계가 필요하면 통계 도메인(`/statistics/*`)을 사용한다.

---

## 5. 알림 (Notification)

> 알림 유형 `type` 3종 (DEC-0026):
> - `BUDGET` — 주간 예산 임계치 도달 (지출 등록 트리거, `threshold`/`ratio` 등 포함)
> - `DRAW` — 옷 뽑기 기회 (최근 4주 이내 끝난 주 중 절약 성공·미결정·미알림). `weeklyBudgetId`로 뽑기 화면 이동
> - `REPORT` — 새 달 시작 → 지난달 레포트 생성 가능. `reportYear`/`reportMonth` 포함
>
> `DRAW`/`REPORT`는 **스케줄러 없이** 알림 조회(5-1, 5-2) 시점에 자격을 검사해 없으면 생성한다(지연 생성). 종 배지가 5-2를 폴링하므로 사용자가 화면을 이동하면 배지가 자동 갱신된다. 중복은 `DRAW`=주별 1건(존재검사), `REPORT`=월별 1건(`UNIQUE(user_id,type,report_year,report_month)`)으로 차단.

### 5-1. 알림 목록 조회
- **GET** `/api/notifications`
- 인증: 필요

| 쿼리 | 타입 | 설명 |
|------|------|------|
| isRead | int | 0=안읽음 / 1=읽음 (생략 시 전체) |

**Response 200** (유형별 필드 — 해당 없는 필드는 null)
```json
[
  {
    "id": 3, "type": "BUDGET", "weeklyBudgetId": 1, "threshold": 75,
    "currentBudget": 300000.00, "spentMoney": 235000.00, "ratio": 78.33,
    "reportYear": null, "reportMonth": null,
    "isRead": 0, "createdAt": "2026-06-07T19:00:00"
  },
  {
    "id": 5, "type": "DRAW", "weeklyBudgetId": 2, "threshold": null,
    "reportYear": null, "reportMonth": null,
    "isRead": 0, "createdAt": "2026-06-25T11:38:00"
  },
  {
    "id": 6, "type": "REPORT", "weeklyBudgetId": null, "threshold": null,
    "reportYear": 2026, "reportMonth": 5,
    "isRead": 0, "createdAt": "2026-06-25T11:37:00"
  }
]
```
> 프론트는 `type`으로 문구·아이콘을 조립하고, 클릭 시 `DRAW`→뽑기 화면(`weeklyBudgetId`), `REPORT`→레포트 화면으로 이동한다.

---

### 5-2. 안읽은 알림 개수
- **GET** `/api/notifications/unread-count`
- 인증: 필요
- **Response 200** `{ "count": 2 }`
- 비고: 호출 시 `DRAW`/`REPORT` 이벤트 알림을 지연 생성하므로, 새 자격이 생기면 이 개수가 자동으로 증가한다.

---

### 5-3. 알림 읽음 처리
- **PATCH** `/api/notifications/{id}/read`
- 인증: 필요
- **Response 200** `{ "message": "읽음 처리되었습니다." }`

---

### 5-4. 알림 전체 읽음 처리
- **PATCH** `/api/notifications/read-all`
- 인증: 필요
- **Response 200** `{ "message": "모든 알림을 읽음 처리했습니다." }`

---

## 6. 통계 (Statistics)

> 별도 테이블 없이 transaction 집계로 생성. 통계 탭 = **월**(수입/지출 × 단순금액/카테고리별) + **주**(지출 전용).
> - 월·단순금액 = `monthly-trend`(6-2) / 월·카테고리별 = `by-category`(6-1)
> - 주·단순금액(예산 vs 지출 + 달성률) = **§4-2 `/budgets/weekly/recent` 재사용**(통계 전용 엔드포인트 없음)
> - 주·카테고리별 = `by-category`(6-1, 프론트가 그 주 범위 전달)

### 6-1. 카테고리별 지출/수입 통계
- **GET** `/api/statistics/by-category`
- 인증: 필요
- 월/주 카테고리별 화면 공용 - 프론트가 해당 **월 또는 주의 startDate~endDate**를 전달. `type`으로 수입/지출 구분.

| 쿼리 | 타입 | 필수 | 설명 |
|------|------|------|------|
| startDate | date | ✅ | 시작일 |
| endDate | date | ✅ | 종료일 |
| type | int | ❌ | 1=수입 / 2=지출 (생략 시 전체) |

- **items 구성**: 금액 내림차순 **상위 4개 + 나머지를 합산한 `기타` 1건**(`categoryId: null`). 카테고리가 4개 이하면 기타 없음.
- **`기타` 항목에는 `members`**(합쳐진 세부 카테고리 배열: `categoryId`/`categoryName`/`amount`/`ratio`)가 포함된다. 기타가 아닌 항목엔 `members` 필드 없음.
- 날짜 역전(end < start) → 400 (`S400`). 거래 없는 기간 → `total: 0`, `items: []`.

**Response 200**
```json
{
  "total": 600000.00,
  "items": [
    { "categoryId": 4, "categoryName": "식비", "amount": 228000.00, "ratio": 38.0 },
    { "categoryId": 6, "categoryName": "교통비", "amount": 132000.00, "ratio": 22.0 },
    { "categoryId": 5, "categoryName": "쇼핑", "amount": 108000.00, "ratio": 18.0 },
    { "categoryId": 7, "categoryName": "문화생활", "amount": 72000.00, "ratio": 12.0 },
    { "categoryId": null, "categoryName": "기타", "amount": 60000.00, "ratio": 10.0,
      "members": [
        { "categoryId": 8, "categoryName": "통신비", "amount": 35000.00, "ratio": 5.83 },
        { "categoryId": 9, "categoryName": "의료비", "amount": 25000.00, "ratio": 4.17 }
      ] }
  ]
}
```

---

### 6-2. 월별 추이 (단순금액)
- **GET** `/api/statistics/monthly-trend`
- 인증: 필요
- 최근 N개월의 월별 합계(꺾은선) + 전월대비. 월·단순금액 화면 전용. (주 단위 추이는 §4-2 재사용이라 여기 없음)

| 쿼리 | 타입 | 필수 | 설명 |
|------|------|------|------|
| type | int | ✅ | 1=수입 / 2=지출 |
| months | int | ❌ | 조회 개월 수 (기본 6) |

- `items`는 과거→현재 순. **거래 없는 달도 `amount: 0`으로 채워** 연속 반환.
- `diffRatio`: 마지막 달 vs 직전 달 증감률(%). **음수 = 절약(▼)**, 양수 = 증가(▲). 직전 달이 0이면 `null`.
- `months < 1` → 400 (`S400`).

**Response 200**
```json
{
  "items": [
    { "month": "2025-12", "amount": 520000.00 },
    { "month": "2026-01", "amount": 610000.00 },
    { "month": "2026-02", "amount": 480000.00 },
    { "month": "2026-03", "amount": 700000.00 },
    { "month": "2026-04", "amount": 680000.00 },
    { "month": "2026-05", "amount": 600000.00 }
  ],
  "diffRatio": -11.76
}
```

---

## 7. 굴비 보상 (Gulbi Reward)

> 한 주의 예산을 지켜내면(절약 성공) 보상으로 굴비에게 입힐 옷을 AI 이미지로 생성한다.
> 마스코트 7무드(hello·warn·happy·sad·hungry·sulk·angry)가 **동일한 한 벌**을 입는다.
> 옷은 고정 목록 없이 **Gemini가 매번 랜덤으로 디자인**한다. `outfitKey`는 모델이 함께 반환하는 **한국어 옷 이름**(1~4단어, 자유 텍스트)이다 (DEC-0021).
> 이미지 생성은 SSAFY GMS 게이트웨이 경유 Gemini 이미지 모델 사용 (DEC-0019). 결과 이미지는 서버에서 축소 후 저장 (DEC-0022).
> 자격: **그 주가 종료**(endDate < 오늘) + **지출 ≤ 예산** + 아직 ACCEPT/DECLINE 안 한 상태.

### 7-1. 보상 상태 조회 (PENDING 이어보기)
- **GET** `/api/budgets/weekly/{weeklyBudgetId}/gulbi-reward`
- 인증: 필요
- 용도: 이미 뽑아둔(PENDING) 보상이 있으면 **재생성 없이** 저장된 결과를 반환. 없으면 `data: null`.

**Response 200** (PENDING 있음)
```json
{
  "weeklyBudgetId": 1,
  "outfitKey": "우주복",
  "rewardStatus": "PENDING",
  "images": { "happy": "data:image/png;base64,...", "sad": "data:image/png;base64,..." }
}
```

---

### 7-2. 굴비 옷 뽑기
- **POST** `/api/budgets/weekly/{weeklyBudgetId}/gulbi-reward/draw`
- 인증: 필요
- 처리: 자격 검사 → **Gemini 랜덤 옷 생성**(앵커가 옷+한국어 이름 생성) → 7무드 **동일 옷** 이미지 생성(트랜잭션 밖, 앵커 1장 생성 후 나머지는 레퍼런스로 통일 · DEC-0020) → PENDING 저장(짧은 트랜잭션).

| 요청(body) | 타입 | 필수 | 설명 |
|------|------|------|------|
| baseImages | object | ✅ | `{ "<mood>": { "mimeType": "image/png", "base64": "..." } }` 무드별 원본 굴비 이미지 |

**Response 200** 생성 결과 (7-1과 동일 형식, `rewardStatus: "PENDING"`)
- 400: `baseImages` 누락
- 403: 절약 성공 주가 아님 (REWARD_NOT_ELIGIBLE, G403)
- 404: 해당 주 예산 없음 (BUDGET_NOT_FOUND, B404)
- 409: 이미 받기/거절 처리된 보상 (REWARD_ALREADY_DECIDED, G409)

---

### 7-3. 보상 받기 / 거절
- **POST** `/api/budgets/weekly/{weeklyBudgetId}/gulbi-reward/decision`
- 인증: 필요

| 요청(body) | 타입 | 필수 | 설명 |
|------|------|------|------|
| decision | string | ✅ | `ACCEPT` 또는 `DECLINE` |

- 처리: `ACCEPT` 시 `user.current_outfit_key`·`current_gulbi_images_json`을 뽑은 결과로 갱신 후 보상 `ACCEPTED`. `DECLINE` 시 보상만 `DECLINED`(굴비 외형 유지).
- **Response 200** `{ "message": "굴비 옷 선택이 반영되었습니다." }`
- 400: decision 값 오류 (REWARD_INVALID_DECISION, G400) / 먼저 뽑기 필요 (REWARD_NO_DRAW, G401)
- 404: 보상 정보 없음 (REWARD_NOT_FOUND, G404)
- 409: 이미 처리된 보상 (REWARD_ALREADY_DECIDED, G409)


---


## 8. 월간 레포트 (Monthly Report)

> AI(굴비) 월간 레포트. 통계/예산 지표는 기존 도메인을 재사용해 정확히 계산하고, 텍스트
> (한줄평·코멘트·조언·한마디 응답)는 **Spring AI `ChatClient`**로 생성한다. AI 호출은
> **GMS(SSAFY AI 게이트웨이) 경유**(`spring.ai.openai.*` 설정, 키는 `.env`의 `GMS_KEY`).
> 레포트는 **월 1회 생성 후 DB 캐싱**(`monthly_report`), 굴비 한마디도 **월 1회**.
> 생성 대상은 **완료된 달(지난달까지)만** — 진행 중인 이번 달/미래 달은 400(R400).
> 주간 예산 성공 주는 **완료된 최근 3주**(진행 중 주 제외) 중 `ratio ≤ 100` 카운트(월 무관).

### 8-1. 월간 레포트 조회 (없으면 생성)
- **GET** `/api/reports/monthly?year=&month=`
- 인증: 필요
- DB에 있으면 그대로 반환, 없으면 통계 수집 + AI 생성 후 저장하고 반환. **재요청 시 재생성 안 함.**

| 쿼리 | 타입 | 필수 | 설명 |
|------|------|------|------|
| year | int | ✅ | 대상 연도 |
| month | int | ✅ | 대상 월(1~12) |

**Response 200**
```json
{
  "id": 1,
  "reportYear": 2026, "reportMonth": 5,
  "totalExpense": 320000.00, "prevExpense": 280000.00, "diffRatio": 14.29,
  "successWeeks": 2, "totalWeeks": 3,
  "topCategory": "식비",
  "oneLiner": "지난달보다 조금 더 썼네, 다음 달엔 같이 줄여보자.",
  "mood": "smirk",
  "categoryComment": "식비가 전월보다 늘었어. 외식 한 번만 줄여도 큰 도움이 돼.",
  "advice": "주간 예산을 미리 정하고 큰 지출은 하루 미뤄 생각해보자.",
  "story": "이번 달은 총 32만 원을 썼네. 지난달보다 조금 늘었지만 식비 빼면 흐름은 안정적이야 ... (300자 내외 총평)",
  "userMessage": null, "gulbiReply": null, "repliedAt": null,
  "generatedAt": "2026-06-01T09:00:00",
  "categories": [
    { "categoryId": 5, "categoryName": "식비", "amount": 120000.00, "ratio": 37.50,
      "prevAmount": 100000.00, "prevRatio": 35.71, "diffAmount": 20000.00 },
    { "categoryId": null, "categoryName": "기타", "amount": 60000.00, "ratio": 18.75,
      "prevAmount": 40000.00, "prevRatio": 14.29, "diffAmount": 20000.00,
      "members": [
        { "categoryId": 8, "categoryName": "통신비", "amount": 35000.00, "ratio": 10.94 },
        { "categoryId": 9, "categoryName": "의료비", "amount": 25000.00, "ratio": 7.81 }
      ] }
  ],
  "extra": {
    "dailyAvg": 45806,
    "noSpendDays": 8,
    "biggestDay": { "date": "2026-05-17", "amount": 120000.00 },
    "savedMost": { "name": "쇼핑", "diff": -130000.00 },
    "spentMost": { "name": "외식·카페", "diff": 45000.00 },
    "weeks": [ { "label": "1주차", "ratio": 78.00, "pass": true } ]
  }
}
```
- `diffRatio`: 전월 대비 %, **전월 지출이 0이면 `null`**. `mood`: **`happy|smirk|angry|sad` 4종**(코드가 그 달 예산 초과 주 수로 결정, AI는 그 mood 톤으로 텍스트만 생성).
- `story`("굴비의 총평"): 이번 달 흐름을 굴비가 풀어내는 **300자 내외 긴 총평**. `oneLiner`/`categoryComment`/`advice`와 **같은 AI 호출 1회**(`applyAiNarrative`)에서 함께 생성되어 DB에 캐싱됨. 옛 캐시 레포트엔 `null`일 수 있음(프론트는 `v-if`로 숨김).
- `categories[]`: 전월/당월 **합집합**(전월에만 있던 카테고리는 `amount=0`). `prevAmount`/`prevRatio`=전월 값(전월 도넛용), `diffAmount`=`amount − prevAmount`.
- `extra`: 부가 지표 스냅샷. **실제 거래액은 원본 그대로**, `dailyAvg`만 원 단위 반올림(총지출÷그 달 전체 일수). `weeks`=그 달 완료된 주의 달성 리스트. 옛 캐시 레포트엔 `null`일 수 있음.
- 400(R400): 이번 달/미래 달 요청 또는 월 범위 오류
- AI 호출 실패 시에도 레포트는 **숫자 기반 폴백 문구로 정상 반환**(앱 안 죽음). 굴비 텍스트만 기본값으로 채움.

---

### 8-2. 굴비에게 한 마디 (월 1회)
- **POST** `/api/reports/monthly/talk`
- 인증: 필요
- 해당 월 레포트가 있고 **아직 답하지 않았을 때만**. 메시지 1개 → 굴비 응답 저장 후 갱신 레포트 반환.

| 요청 | 타입 | 필수 | 설명 |
|------|------|------|------|
| year | int | ✅ | 대상 연도 |
| month | int | ✅ | 대상 월 |
| message | string | ✅ | 굴비에게 건넬 한 마디(최대 200자) |

**Response 200** 갱신된 레포트(`userMessage`/`gulbiReply`/`repliedAt` 채워짐)
- 404(R404): 해당 월 레포트 없음
- 409(R409): 이미 이번 달 한 마디 사용함
- 503(R503): AI 실패 — **저장하지 않음(1회 기회 미소모, 재시도 가능)**

---

## 공통 사항

### 응답 상태 코드
| 코드 | 의미 |
|------|------|
| 200 | 성공 |
| 201 | 생성 성공 |
| 400 | 잘못된 요청 (필수값 누락 등) |
| 401 | 인증 실패 (토큰 없음/만료) |
| 403 | 권한 없음 (타인 리소스 등) |
| 404 | 리소스 없음 |
| 409 | 충돌 (중복 등) |
| 503 | AI(굴비) 호출 실패 (R503) |

### 공통 에러 응답 형식
```json
{ "status": 400, "error": "Bad Request", "message": "amount는 필수입니다." }
```

### 엔드포인트 요약
| 도메인 | 메서드 & 경로 |
|--------|---------------|
| 인증 | POST /auth/signup, /auth/login, /auth/logout |
| 사용자 | GET·PUT·DELETE /users/me |
| 카테고리 | GET·POST /categories, PUT·DELETE /categories/{id} |
| 거래 | GET·POST /transactions, GET·PUT·DELETE /transactions/{id} |
| 주간예산 | GET /budgets/weekly/current, GET /budgets/weekly/recent, POST /budgets/weekly, PUT /budgets/weekly/{id} |
| 알림 | GET /notifications, GET /notifications/unread-count, PATCH /notifications/{id}/read, /notifications/read-all |
| 통계 | GET /statistics/by-category, /statistics/monthly-trend |
| 레포트 | GET /reports/monthly, POST /reports/monthly/talk |
| 굴비보상 | GET /budgets/weekly/{id}/gulbi-reward, POST .../gulbi-reward/draw, POST .../gulbi-reward/decision |
