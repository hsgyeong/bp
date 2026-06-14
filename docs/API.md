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

### 4-2. 최근 주간 예산 목록 (이번 주 포함 최근 5주)
- **GET** `/api/budgets/weekly/recent`
- 인증: 필요
- 파라미터 없음. **월과 무관하게** 오늘이 속한 주를 포함해 `start_date` 기준 **최근 5개** 주간예산을 반환 (미래 주 제외).

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
- **Response 200** 수정된 예산
- 403: 월 수정 횟수 초과 (`{ "error": "이번 달 예산 수정 횟수를 초과했습니다." }`)

> ℹ️ 예산은 **주 단위만** 제공한다. 월 단위 예산/요약 엔드포인트는 두지 않음(DEC-0009).
> 월 단위 *지출* 집계가 필요하면 통계 도메인(`/statistics/*`)을 사용한다.

---

## 5. 알림 (Notification)

### 5-1. 알림 목록 조회
- **GET** `/api/notifications`
- 인증: 필요

| 쿼리 | 타입 | 설명 |
|------|------|------|
| isRead | int | 0=안읽음 / 1=읽음 (생략 시 전체) |

**Response 200**
```json
[
  {
    "id": 3, "threshold": 75,
    "currentBudget": 300000.00, "spentMoney": 235000.00, "ratio": 78.33,
    "isRead": 0, "createdAt": "2026-06-07T19:00:00"
  }
]
```

---

### 5-2. 안읽은 알림 개수
- **GET** `/api/notifications/unread-count`
- 인증: 필요
- **Response 200** `{ "count": 2 }`

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

> 별도 테이블 없이 transaction 집계로 생성

### 6-1. 카테고리별 지출/수입 통계
- **GET** `/api/statistics/by-category`
- 인증: 필요

| 쿼리 | 타입 | 필수 | 설명 |
|------|------|------|------|
| startDate | date | ✅ | 시작일 |
| endDate | date | ✅ | 종료일 |
| type | int | ❌ | 1=수입 / 2=지출 |

**Response 200**
```json
{
  "total": 250000.00,
  "items": [
    { "categoryId": 4, "categoryName": "식비", "amount": 80000.00, "ratio": 32.0 },
    { "categoryId": 6, "categoryName": "교통비", "amount": 56250.00, "ratio": 22.5 }
  ]
}
```

---

### 6-2. 기간별 수입/지출 합계
- **GET** `/api/statistics/summary`
- 인증: 필요

| 쿼리 | 타입 | 필수 |
|------|------|------|
| startDate | date | ✅ |
| endDate | date | ✅ |

**Response 200**
```json
{ "income": 3100000.00, "expense": 250000.00, "balance": 2850000.00 }
```

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
| 통계 | GET /statistics/by-category, /statistics/summary |
