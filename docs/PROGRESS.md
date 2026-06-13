# PROGRESS — 작업 진행 로그

> 누가 · 무엇을 · 어디까지 했는지 짧게 기록. 최신 항목을 **위에** 추가.
> 도구(Claude/Codex)와 담당자를 함께 적으면 상대가 맥락을 파악하기 쉽습니다.

## 진행 중 (In Progress)

| 담당 | 도구 | 작업 | 브랜치 | 비고 |
|------|------|------|--------|------|
| 팀원 A | Codex | user/auth + transaction 구현·검토 및 잔여 작업 추적 | feat/transaction-front | user/auth: JWT 기반 로그인·회원가입·내정보 API와 로그인/회원가입 화면 연동 진행. transaction: 백엔드 CRUD/필터/소유자 검증 + 카테고리 포함 응답 매핑 구현, 프론트 LedgerView(목록/달력)·TransactionFormView(등록/수정/삭제)·거래 API 모듈 연동 완료. 남은 핵심은 예산/알림 재계산 연동 및 실제 로그인 후 CRUD 실행 검증 |
| 팀원 B | Claude | 주간예산 백엔드(4종: current/list/post/put) + 프론트 토대(router·axios·레이아웃·토큰) | feat/budget | ⚠️ 계약 변경: 예산 **월간 제거**(DEC-0009) → API.md 4-5 삭제. 예산은 '주' 단위만. A는 홈/거래가 주간예산만 써서 영향 거의 없음. 프론트 토대는 A·B 공용이라 라우트·axios 컨벤션 확정되면 공유 |

## 완료 (Done)

| 날짜 | 담당 | 도구 | 작업 |
|------|------|------|------|
| 2026-06-13 | 팀원 A | Codex | user/auth API 명세 정합성 반영: 실제 백엔드 기준으로 `POST /api/auth/signup`은 공통 `Response<User>`, `POST /api/auth/login`은 raw `LoginResponse(accessToken, tokenType, email, role)`, `POST /api/auth/logout`은 공통 `Response<Void>` 응답으로 API.md 수정 |
| 2026-06-13 | 팀원 A | Codex | transaction/ledger 프론트 구현 및 검토: `LedgerView.vue` 거래 목록·월 선택·일자별/달력 탭·월 수입/지출/합계·등록/수정 이동 구현, `TransactionFormView.vue` 거래 등록·수정·삭제 및 카테고리 조회 연동, `transaction.js` API 모듈 추가. 백엔드 `TransactionMapper.xml`은 category 조인 결과를 nested `category`로 응답하도록 정리. 백엔드 compile 및 프론트 build 통과 확인 |
| 2026-06-13 | 팀원 A | Codex | 현재 코드 기준 user/transaction 진행 상태 확인: user/auth 백엔드 JWT 흐름(SecurityConfig·Swagger bearer·Auth/User Controller·Service·DAO·Mapper) 및 프론트 로그인/회원가입 API 연결 확인, transaction 백엔드 Controller·Service·DAO·Mapper CRUD/조회필터 구현 확인 |
| 2026-06-08 | 팀원 B | Claude | 카테고리 API 완성(DTO·DAO·Mapper XML·Service·Controller) + ErrorCode 카테고리/예산 코드, MyBatisConfig. feat/category PR 머지 완료 |
| 2026-06-04 | | Claude | 개발 계획 수립(기능 수직 분할·7일 일정·브랜치 전략), 목업 15화면 난이도 평가 후 화면/도메인 A·B 분담 확정, 노션 작업보드 2개(팀원 A/B DB, 각 14행) 생성 + 간트·보드 뷰 구성. 상세는 AGENTS §8 / DECISIONS DEC-0005~0007 / 노션 참고 |
| 2026-06-03 | | Claude | AGENTS.md / CLAUDE.md 작성, PROGRESS·DECISIONS 템플릿 추가 |
| 2026-06-?? | | | 백엔드 초기 골격 (Spring Boot 4 + MyBatis), TestController |
| 2026-06-?? | | | 프론트 Vite 스캐폴드 |
| 2026-06-?? | | | 목업 15화면 HTML + png, docs/API.md 명세 |

## 알려진 이슈 (Known Issues)

- **MyBatis 매퍼 경로**: 2026-06-13 현재 `application.properties`의
  `mybatis.mapper-locations=classpath:mappers/**/*.xml`와 실제 XML 경로
  `src/main/resources/mappers/`가 일치함. 기존 `mapper/`(단수) 불일치 메모는
  오래된 이슈로 보이며, 실행 테스트 때 매퍼 로딩 여부만 재확인하면 됨.

## 다음 할 일 (Backlog)

### 2026-06-13 · user/transaction 확인 필요 (팀원 A)

- [x] **user/auth 응답 계약 정합성 확인** — 백엔드 실제 응답 기준으로 API.md 수정 완료.
  회원가입·로그아웃은 공통 `Response<T>`, 로그인은 raw `LoginResponse(accessToken, tokenType, email, role)` 기준.
- [ ] **user 프론트 잔여 화면 연동** — 로그인/회원가입은 API 호출이 연결되어 있으나,
  내 정보 조회·수정·탈퇴 화면/흐름은 아직 확인 및 연동 필요.
- [x] **transaction 프론트 연동** — 백엔드 CRUD API 기준으로
  `LedgerView.vue` 거래 목록/달력 화면, `TransactionFormView.vue` 등록·수정·삭제 화면,
  `src/api/transaction.js` API 모듈 연결 완료. 백엔드 compile 및 프론트 build 통과 확인.
- [ ] **transaction 등록·수정 후 예산/알림 연동** — API.md 3-3/3-4의
  "지출 비율 재계산 및 임계치 알림 생성/재평가"는 거래 서비스에서 아직 별도 연동 확인 필요.
- [ ] **Swagger 또는 로컬 실행 검증** — 회원가입 → 로그인 → Swagger Authorize →
  `/api/users/me` → `/api/transactions` CRUD 순서로 실제 동작 확인 필요. 코드 기준 빌드는 통과했지만
  브라우저/Swagger에서 토큰 포함 실제 등록·수정·삭제까지는 별도 확인 필요.

### 2026-06-09 · 예산 작업 중 확인 필요 (팀원 B)

- [ ] **ErrorCode에 예산 소유권 403 코드 없음** — 현재 예산용은
  `BUDGET_NOT_FOUND / BUDGET_ALREADY_EXISTS / BUDGET_UPDATE_LIMIT_EXCEEDED / BUDGET_INVALID_INPUT`
  4개뿐. 카테고리의 `CATEGORY_FORBIDDEN` 같은 **소유권 위반(403)** 코드가 예산엔 없음.
  예산 수정(PUT 4-4)에서 남의 예산 접근을 막는 `findOwned`에 403이 필요함. 선택지:
  - **(a) `BUDGET_FORBIDDEN` 추가** — ⚠️ 코드 문자열 `B403`은 이미
    `BUDGET_UPDATE_LIMIT_EXCEEDED`가 사용 중 → `B4031` 등 **다른 문자열**로 잡아야 함.
    `ErrorCode.java`는 공통 파일(친구 작성) → 수정 전 공유.
  - **(b) 기존 `USER_FORBIDDEN`(U403) 재사용** — 추가 없이 바로. 도메인 특정성은 떨어짐.
  - 현재 `BudgetServiceImpl.findOwned`는 **(a) `BUDGET_FORBIDDEN` 기준으로 작성**됨 →
    코드 추가 전엔 컴파일 안 됨. (a)/(b) 결정 필요.

### 2026-06-08 · 카테고리 작업 중 확인 필요 (팀원 B)

- [ ] **ErrorCode에 카테고리 코드 추가** — 현재 `ErrorCode` enum에 category 코드가 없음
  (user/transaction/budget/notification만 있음). 카테고리 Service에서 403/404를
  던지려면 아래 2개 추가 필요:
  - `CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "C404", "해당 카테고리를 찾을 수 없습니다.")`
  - `CATEGORY_FORBIDDEN(HttpStatus.FORBIDDEN, "C403", "해당 카테고리에 접근할 권한이 없습니다.")`
  - ⚠️ `ErrorCode.java`는 공통 파일(친구 작성) → 수정 전 공유해서 충돌 방지.
- [ ] **응답의 `isDefault` 처리 방식 결정** — API.md 카테고리 응답엔 `isDefault`(기본제공 여부)가
  있는데 `Category` DTO엔 없음. `isDefault`는 `userId == null` 이면 true인 파생값.
  - (a) DTO에 `isDefault` 필드 추가 + Service에서 계산 / (b) `userId` 그대로 노출 후 프론트가 판단(간단).
  - MVP는 (b)도 가능. 추후 결정.

- [ ] **카테고리 display_order(표시 순서) 처리 방침 결정**
  - 순서 정책: **새 카테고리는 맨 뒤**로. (대부분 앱 관례 + 기본 카테고리 순서 안 깨짐)
    - 맨 앞(display_order=0)은 기본 제공 카테고리(1,2,3…) 위로 끼어들어 어색 → 비채택.
    - 진짜 맨 앞/맨 뒤 둘 다 기존 값 조회 쿼리(min-1 / max+1)가 필요함.
  - 구현 선택지:
    - **옵션 X (MVP 단순)**: display_order 자동설정 안 함 → `ORDER BY type, id`(생성순)으로 조회.
      `findMaxDisplayOrder` 불필요. 새 카테고리는 id가 커서 자연히 뒤에 생김.
    - **옵션 Y (미래 대비)**: `findMaxDisplayOrder`로 max+1 부여. 추후 드래그 정렬 기능에 바로 활용.
  - 현재 판단: 드래그 정렬은 **보너스**라 MVP는 옵션 X로 시작 가능. 드래그 정렬 도입 시 옵션 Y로 전환.
  - (관련: `CategoryServiceImpl.addCategory`, `CategoryDao.findMaxDisplayOrder`, `CategoryMapper.xml`) 

- [ ] **카테고리 조회 `type` 파라미터 필수 처리** — 수입/지출을 섞어 조회할 일이 없어
  Mapper의 `selectCategories`에서 `<if>`를 빼고 `AND type = #{type}` 고정함. 따라서 type 생략 시
  `type=NULL` 비교로 **빈 목록**이 반환됨 → 아래 2가지로 일관성 맞춰야 함:
  - **CategoryController(미작성)**: `@RequestParam(required = true) Integer type` 으로 type 필수화
    (생략 시 400으로 명확히 거부 → 빈 목록과 혼동 방지).
  - API.md 2-1: "생략 시 전체" → **"type 필수"** 로 수정 완료 (2026-06-08).
  - (선택) `ORDER BY type, id` 에서 `type` 은 한 type만 조회하므로 `ORDER BY id` 로 줄여도 됨.

### 일반

- [ ] DB 스키마 DDL 확정 (5개 테이블)
- [ ] 인증(JWT) 백엔드 구현
- [ ] 목업 → Vue 컴포넌트 이식 (4탭 라우팅)
- [ ] 프론트-백 API 연동 (axios)
