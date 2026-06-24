# DECISIONS — 설계 결정 기록 (ADR 라이트)

> 되돌리기 어렵거나 둘 다 영향받는 결정을 **왜** 그렇게 했는지 남깁니다.
> 최신 항목을 **아래에** 추가(번호 증가). 형식은 가볍게 유지.

---

## DEC-0001 · 백엔드 스택: Spring Boot 4 + MyBatis + MySQL
- **날짜**: 2026-06
- **결정**: Java 21 / Spring Boot 4.0.6 / MyBatis / MySQL(`jaringochi` 스키마) 사용.
- **이유**: 학습 범위에 맞춘 표준 조합. JPA 대신 MyBatis로 SQL 직접 제어.
- **영향**: 매퍼 XML은 `resources/mapper/` 하위, 응답은 camelCase 매핑.

## DEC-0002 · 프론트 스택: Vue 3 + Vite + axios
- **날짜**: 2026-06
- **결정**: Vue 3.5 SFC(`<script setup>`) + Vite 8, HTTP는 axios.
- **이유**: 가벼운 SPA, 빠른 개발 서버.

## DEC-0003 · API 단일 기준 문서화
- **날짜**: 2026-06
- **결정**: 모든 엔드포인트는 [API.md](API.md) 를 단일 기준으로 삼고, 변경 시 문서 우선 수정.
- **이유**: 프론트/백을 다른 사람·다른 AI 도구가 맡으므로 계약(contract)을 고정해야 충돌 방지.

## DEC-0004 · 디자인 토큰 확정
- **날짜**: 2026-06
- **결정**: 골드 `#F2A33C` / 크림 `#FBF5EA` / 수입 `#2FA98C` / 지출 `#E8623D`, Pretendard,
  그래프 인라인 SVG, 굴비 마스코트 강조. (사용자 승인)
- **이유**: 친근한 톤 + 절약 컨셉(굴비) 일관성. 목업 15화면 기준.

## DEC-0005 · 분담: 기능 수직 분할(각자 풀스택)
- **날짜**: 2026-06-04
- **결정**: 도메인을 둘로 나눠 각자 백+프론트 풀스택 담당 (A: auth/user·transaction, B: category·budget·statistics·notification). 화면도 도메인 묶음으로 분배(목업 난이도 점수로 업무량 균형 ≈ A 29 / B 31).
- **이유**: 7~8일·2인·고정된 API 계약에서 충돌 최소화 + 처리량 확보. 같은 파일 동시수정 방지.
- **영향**: 학습 범위가 다소 갈리므로 **PR 교차 리뷰 + 하루 5분 공유**로 보완하고, 버퍼 주간(6/15~)에 특화 영역(A: 차트, B: JWT)을 교차 실습.

## DEC-0006 · 인증은 학습 직후(6/9) 구현 + 임시인증 1곳 추상화
- **날짜**: 2026-06-04
- **결정**: 커리큘럼상 Security(6/8)·JWT(6/9)를 늦게 배우므로, 그 전에는 **임시 인증**으로 도메인부터 개발. 현재 유저 취득을 **백 `getCurrentUserId()` / 프 axios 인터셉터 각 1곳**으로 추상화 후, 6/9에 그 한 곳만 실 JWT로 교체.
- **이유**: "인증 먼저"가 불가능한 일정. 임시→실 인증 전환 비용을 단일 지점으로 국소화.
- **영향**: Day1 토대에서 SecurityConfig는 `permitAll`(임시)로 시작. API.md의 `Bearer` 계약은 그대로 유지.

## DEC-0007 · MVP 범위와 보너스/발표 분리
- **날짜**: 2026-06-04
- **결정**: 알림(지출 임계치 트리거)은 **MVP 포함**. 굴비 챗봇(Spring AI)은 **보너스**(6/15~). 발표 준비는 6/22~24.
- **이유**: 알림은 핵심 컨셉(절약 경고)이라 포함. 챗봇은 Spring AI 학습(6/10) 이후라 후순위.
- **영향**: `notification` 테이블에 `weekly_budget_id`+`threshold` 포함(임계치 중복 알림 방지용).

## DEC-0008 · db 이름 ssafy 에서 jaringochi로 수정
- **날짜**: 2026-06-05
- **결정**: db 이름 ssafy 에서 jaringochi로 수정
- **이유**: 이전에 db 설계할 때 정한 사항
- **영향**: application.properties와 agents.md, decisions.md 수정

## DEC-0009 · 예산은 '주' 단위만 — 월간 예산 미채택
- **날짜**: 2026-06-09
- **결정**: 예산 기능은 **주 단위(weekly_budget)** 만 제공. 월별 예산/요약 엔드포인트(구 API.md 4-5 `GET /api/budgets/monthly`, `confirmedBudget`)는 **삭제**.
- **이유**: 본 프로젝트는 '주' 단위 지출 관리에 집중하는 컨셉. 예산을 월 단위로 합산·요약해 보여주는 흐름은 제품 방향과 맞지 않음. 월 단위로 보고 싶은 건 *예산*이 아니라 *지출 통계*임.
- **영향**: API.md 4-5 섹션 및 요약표의 `/budgets/monthly` 제거. 예산 백엔드는 4개 엔드포인트(4-1~4-4)로 축소, `MonthlySummary` DTO 불필요(`WeeklyBudget` 단일 DTO). 월 단위 지출 집계는 통계 도메인(`/statistics/*`)이 담당(예산과 무관). 알림의 `currentBudget`은 주간예산 기준이라 영향 없음.

## DEC-0010 · 주간예산 목록 4-2 = '월별'에서 '최근 5주'로 변경
- **날짜**: 2026-06-09
- **결정**: 4-2를 `GET /api/budgets/weekly?year=&month=`(해당 월 주들) → **`GET /api/budgets/weekly/recent`**(파라미터 없음)으로 변경. 오늘이 속한 주를 포함해 `start_date` 기준 **최근 5개** 주간예산 반환(미래 주 제외, 과거→현재 순).
- **이유**: 제품이 '월' 단위가 아닌 '주' 단위 흐름에 집중(→ DEC-0009와 같은 맥락). 화면에서 필요한 건 "이번 주 포함 최근 몇 주 추이"이지 특정 달의 주 목록이 아님. 경로도 4-1 `/weekly/current`과 짝이 맞게 `/weekly/recent`로.
- **영향**: DAO `selectWeeklyByMonth(userId,year,month)` → `selectRecentWeeks(userId)`. Mapper는 `WHERE start_date <= CURDATE() ORDER BY start_date DESC LIMIT 5`. Controller 시그니처에서 year/month 파라미터 제거. API.md 4-2 및 요약표 갱신 완료.
- **⚠️ 갱신(2026-06-16, DEC-0014)**: 통계 '주' 화면과 데이터를 공유하기 위해 **최근 5주 → 4주**로 변경(`LIMIT 5` → `LIMIT 4`). 예산 화면 "최근 주간 예산" 목록도 4줄로 줄어듦.


## DEC-0011 · 알림 - 임계치 넘었을 때 가장 가까운 임계값에 대해서만 알림 발송
- **날짜**:2026-06-12
- **결정**:임계치 넘었을 때 가장 가까운 임계값에 대해서만 알림 발송
- **이유**:예컨대, 50%, 75%를 동시에 넘었을 때 둘 다 알림 발송되는 건 이상함. 75%만 넘도록 하는 것이 사용자 경험상 더 자연스러움
- **영향**:
  - 트리거 로직(③, A의 `TransactionService` 지출 등록부에 연결)이 **차집합 방식 → 최댓값 비교 방식**으로 단순화됨:
    `maxSent = SELECT MAX(threshold) WHERE weekly_budget_id=?` / `crossed = ratio 이하 최대 단계` / `crossed > maxSent`일 때만 `crossed` 1건 INSERT.
  - 건너뛴 중간 단계(위 예의 50%)는 알림 행이 **남지 않음**. 단계별 행을 요구하는 소비처가 없어 실질 영향 없음(사용률은 예산 화면이 실시간 표시).
  - 주가 바뀌면 `weekly_budget_id`가 달라져 단계 카운트가 자연히 0부터 리셋(새 주엔 다시 25%부터).
  - 동시성 대비 `UNIQUE (weekly_budget_id, threshold)` 안전망은 유지(중복 INSERT를 DB가 차단).
  - §5 조회 API(GET 알림 목록/개수, PATCH 읽음)는 읽기 전용이라 **무영향**.

## DEC-0012 · 알림 트리거 연결 = 직접 호출 (ApplicationEvent 미채택)
- **날짜**: 2026-06-13
- **결정**: 지출 등록 시 알림 평가를 `TransactionServiceImpl.addTransaction`에서 `NotificationService.evaluateExpense(userId, date)` **직접 호출**로 연결. ApplicationEvent 방식은 미채택.
- **이유**: 2인 MVP에선 직접 호출이 더 명시적이고 디버깅·추적이 쉬움. 이벤트의 느슨한 결합 이점은 현 규모에서 과함.
- **영향**:
  - transaction 도메인이 notification 서비스에 의존(import + `final` 필드 + 호출 1줄). `type==2`(지출)일 때만 호출.
  - `evaluateExpense`는 **`@Transactional`을 의도적으로 안 붙임** -> 거래의 트랜잭션에 그대로 참여해 방금 등록한 지출이 지출합 SUM에 포함됨. 동시에 내부 `try-catch`로 알림 실패(제약 위반 등)가 거래를 롤백시키지 않음(거래는 항상 201).
  - 거래 수정(`updateTransaction`) 시 재평가는 추후(동일 호출 한 줄, PROGRESS 백로그).
  - `notification.threshold`는 단계값이 150까지라 `TINYINT`(127 이하)로는 초과 -> **`SMALLINT`** 필요(검증 중 발견·수정).

## DEC-0013 · 컨트롤러 임시인증(1L) -> 실 JWT 전환 (DEC-0006 이행)
- **날짜**: 2026-06-13
- **결정**: 컨트롤러 `getCurrentUserId()` `1L` 하드코딩을 토큰 `jwt.getSubject()` 기반으로 전환. 거래(기존 적용)에 더해 **카테고리·예산·알림** 컨트롤러를 동일 패턴(`Authentication` 파라미터)으로 통일.
- **이유**: 알림 트리거 검증 중 거래(실인증, user4)와 예산·알림(1L=user1)이 엇갈려 트리거가 동작하지 않는 문제 발견. 실제 서비스에서도 사용자별로 깨지므로 통일 필요.
- **영향**:
  - 각 핸들러에 `Authentication authentication` 파라미터 추가, `getCurrentUserId(authentication)`로 userId 추출. 미인증 시 `USER_UNAUTHORIZED`(401).
  - 프론트는 이미 axios 토큰 인터셉터라 영향 적음. `UserController`(내 정보)는 추후 점검(백로그).
  - DEC-0006의 "임시->실 인증 단일 지점 전환"을 실제로 이행한 항목.


## DEC-0014 · Refresh Token DB 저장/폐기로 로그아웃 보완
- **날짜**: 2026-06-14
- **결정**: 로그인 응답에 `refreshToken`을 추가하고, 서버 DB `refresh_token`에는 원문이 아닌 SHA-256 해시값만 저장한다. 로그아웃 시 클라이언트가 보낸 `refreshToken`을 해시해 해당 행의 `revoked_at`을 기록하고, 회원탈퇴 시 해당 사용자의 모든 Refresh Token을 폐기한다.
- **이유**: JWT Access Token은 서버가 상태를 저장하지 않아 클라이언트 토큰 삭제만으로는 서버 측 폐기 기록이 남지 않음. MVP에서는 Access Token 블랙리스트보다 Refresh Token 재사용 권한을 DB에서 끊는 방식이 단순하고 설명하기 좋음. 토큰 원문은 DB 유출 시 바로 악용될 수 있어 저장하지 않음.
- **영향**:
  - `schema.sql` 테이블이 5개 -> 6개로 증가(`refresh_token` 추가). `application.properties`에 `jwt.refresh-expiration` 추가.
  - `LoginResponse`에 `refreshToken` 필드 추가. 프론트 auth store와 API.md도 함께 갱신.
  - `/api/auth/logout`은 Access Token 대신 요청 body의 `refreshToken`으로 폐기 대상을 찾음.
  - `/api/users/me` 회원탈퇴 시 해당 userId의 모든 Refresh Token을 함께 폐기함.
  - 클라이언트는 서버 로그아웃 성공 여부와 관계없이 로컬 `accessToken` / `refreshToken` / `user` 삭제.
  - Access Token 자체는 만료 전까지 즉시 차단하지 않음. 즉시 차단이 필요하면 추후 `jti` 블랙리스트 또는 `/api/auth/refresh` 재발급 흐름 추가.

## DEC-0015 · 거래 목록 검색/정렬은 서버 쿼리 파라미터로 처리
- **날짜**: 2026-06-14
- **결정**: 거래 목록 조회 `GET /api/transactions`에 `keyword`와 `sort` 쿼리 파라미터를 추가한다. `keyword`는 메모와 카테고리명을 부분 검색하고, `sort`는 `date_desc`, `date_asc`, `amount_desc`, `amount_asc` 네 값만 허용한다.
- **이유**: 거래 목록은 월 범위, 카테고리, 수입/지출 조건과 함께 조회되므로 프론트에서 전체 데이터를 받은 뒤 필터링하기보다 백엔드 조회 조건으로 통일하는 편이 API 계약이 명확함. 정렬값은 SQL `ORDER BY`에 영향을 주므로 허용 목록으로 제한해야 안전하다.
- **영향**:
  - API.md 거래 목록 조회 명세에 `keyword`, `sort` 추가.
  - 프론트 거래 목록 화면은 검색어와 정렬값을 `fetchTransactions` 파라미터로 전달하면 됨.
  - 백엔드는 MyBatis 동적 SQL에서 `keyword` 조건과 `sort`별 `ORDER BY` 분기를 추가해야 함.

## DEC-0016 · 통계 도메인 목업 기준 재설계 (summary 제거 · 추이 추가)
- **날짜**: 2026-06-16
- **결정**: 1차 통계(`by-category` + `summary`)를 목업(03 월·단순금액, 06 월·카테고리별, 07 주) + 원본 기획 기준으로 재설계.
  - `summary`(income/expense/balance) **제거** - 어느 통계 화면에도 안 쓰임(balance 3종은 통계가 아니라 홈 성격).
  - `by-category`는 유지하되 **상위 4개 + 나머지 합산 '기타' 1건**(categoryId=null)으로 묶어 반환(목업 06). 월/주 카테고리별 공용(프론트가 기간 전달), `type`으로 수입/지출.
  - **`monthly-trend` 신규** - 월별 합계 추이(꺾은선) + **전월대비 `diffRatio`**(목업 03). 빈 달은 0으로 채워 연속.
  - 통계 '주·단순금액'(예산 vs 지출 막대 + 달성률)은 **별도 엔드포인트 없이 §4-2 `/budgets/weekly/recent` 재사용**. 주는 **지출 전용**(수입 그래프 없음).
- **이유**: 1차는 목업을 안 보고 API 계약만 보고 만들어 화면 요구(시계열·전월대비·예산비교)와 어긋남. UI 우선으로 역산해 계약을 다시 맞춤. 통계·예산 모두 pearlseo73 단독 소유라 함께 조정 가능.
- **영향**:
  - API.md §6 개편(summary 삭제, by-category 기타 묶기 명시, monthly-trend 추가), §4-2 5주→4주(DEC-0010 갱신).
  - 코드: `StatisticsSummary` DTO·selectSummary·getSummary·`/summary` 제거. `getByCategory`에 기타 묶기. `monthly-trend` 일습(DTO/DAO/Mapper/Service/Controller) 신규. `BudgetMapper.selectRecentWeeks` `LIMIT 4`.
  - ErrorCode `STATISTICS_INVALID_INPUT(S400)`은 유지(by-category·monthly-trend 입력검증에 계속 사용).
  - 통계 프론트(`StatsView.vue` 차트)는 별도 후속 작업.

## DEC-0017 · 예산 수정 시 그 주 알림 리셋 후 재평가
- **날짜**: 2026-06-16
- **결정**: 주간 예산 수정(PUT `/api/budgets/weekly/{id}`) 시, 그 `weekly_budget_id`의 알림을 **전부 삭제(기준 리셋)** 한 뒤 새 금액 기준으로 **재평가**한다. 재평가는 기존 트리거(`evaluateExpense`)를 그대로 써서 **새 비율의 최고 단계 1건만** 생성(DEC-0011 유지).
- **이유**: 알림 중복 차단이 `weekly_budget_id` 기준(`selectMaxThreshold`)이라, 예산 금액만 바꾸면 같은 id가 유지돼 이미 보낸 단계가 "보냄"으로 남아 새 기준에서 재발송이 막혔다. 예산을 낮춰 이미 초과 상태가 돼도 경고가 안 뜨는 문제. "예산 기준이 바뀌면 그 기준으로 다시 평가"가 자연스러운 UX라 판단.
- **영향**:
  - `NotificationDao.deleteByWeeklyBudgetId` + Mapper `<delete>` 신규. `NotificationService.reevaluateOnBudgetChange(userId, weeklyBudgetId, weekDate)` 신규(삭제 -> `evaluateExpense` 호출, try-catch로 알림 실패가 예산 수정을 롤백 안 시킴).
  - `BudgetServiceImpl.updateWeeklyBudget`이 `NotificationService`에 의존(주입 + 호출 1줄). 순환 의존 없음(알림은 BudgetDao만 의존).
  - 단계별 여러 건이 아니라 **최고 1건**만 재생성(DEC-0011 철학 유지). 거래 수정 시 재평가(`updateTransaction`)는 여전히 백로그.
  - API.md §4-4에 재평가 동작 명시.

## DEC-0018 · 굴비 보상(절약 성공 리워드) 도메인 신설
- **날짜**: 2026-06-22
- **결정**: 한 주 예산을 지킨(절약 성공) 사용자에게 보상으로 **굴비에게 옷을 입히는 AI 이미지**를 생성해 주는 `gulbi` 도메인을 추가한다.
  - 엔드포인트: `GET/POST /api/budgets/weekly/{weeklyBudgetId}/gulbi-reward`(상태조회 / `/draw` 뽑기 / `/decision` 받기·거절). 예산 하위 리소스로 배치.
  - 자격: **그 주 종료**(endDate < 오늘) + **지출 ≤ 예산** + 미결정(rewardStatus가 ACCEPTED/DECLINED 아님).
  - 옷은 6종(hanbok·hoodie·pajama·school·santa·raincoat) 중 **현재 옷 제외 랜덤 1개**, 무드 7종이 같은 옷 착용.
  - DB: `weekly_budget`에 `reward_status`·`reward_outfit_key`·`reward_images_json`·`reward_decided_at`, `user`에 `current_outfit_key`·`current_gulbi_images_json` 추가.
- **이유**: "절약하면 굴비가 새 옷을 받는다"는 보상 루프로 절약 동기를 강화. 예산 달성 여부가 판정 기준이라 주간예산 하위에 둠.
- **영향**:
  - 이미지 생성은 느리고 외부 호출이라 **뽑기=생성(트랜잭션 밖) → 저장만 짧은 트랜잭션**으로 분리(`GulbiRewardService.draw` + `GulbiRewardTx.persistPending`).
  - PENDING 상태로 저장해 두고 `GET`으로 재생성 없이 이어보기 가능. ACCEPT 시에만 유저 현재 외형 반영.
  - ErrorCode `REWARD_*`(G400·G401·G403·G404·G409) 추가. API.md §7 신설.
  - 컨트롤러 `@RequestBody`는 반드시 `org.springframework.web.bind.annotation` 것을 사용(초기 버그: Swagger 애너테이션을 import해 본문 바인딩이 안 돼 `baseImages`가 null → NPE).

## DEC-0019 · 이미지 생성 제공자: SSAFY GMS(Gemini) 채택
- **날짜**: 2026-06-22
- **결정**: 굴비 옷 이미지 생성은 **SSAFY GMS 게이트웨이 경유 Gemini 이미지 모델**(`gemini-2.5-flash-image`, JSON `generateContent`)을 사용한다. 호출 URL은 `{gms.base-url}/generativelanguage.googleapis.com/v1beta/models/{model}:generateContent`, 인증 헤더 `x-goog-api-key: <GMS_KEY>`. 키는 `.env`(`GMS_KEY`)로 주입, `.gitignore` 대상.
- **이유**: Claude는 이미지 생성 불가, OpenAI `gpt-image-1`은 유료, Google 직접 호출은 이미지 모델 무료 티어 0(429). GMS는 SSAFY 제공으로 학생 무료 + Gemini 이미지 편집 지원.
- **영향**:
  - `GmsImageClient`는 멀티파트가 아닌 **JSON(contents/parts/inline_data + responseModalities)** 으로 호출. 설정은 `gms.base-url`·`gms.api-key`·`gms.model`.
  - 사용 안 하게 된 `GmsConfig`(빈 RestClient·`${gms.key}` 주입) 삭제 — 미삭제 시 기동 실패.
  - 모델명은 GMS가 실제로 여는 카탈로그와 일치해야 함(목록 확인). 인증이 `x-goog-api-key`로 안 되면 `Authorization: Bearer`로 대체.
  - 뽑기 1회 = 무드 7종 = **이미지 7회 호출**이라 비용·지연·쿼터 부담. 테스트 시 무드 축소 권장.

## DEC-0020 · 7무드 동일 옷: 앵커 생성 후 레퍼런스 통일
- **날짜**: 2026-06-22
- **결정**: 한 번의 뽑기에서 옷은 `draw()`가 **한 번만** 랜덤 선택해 7무드가 같은 `outfitKey`를 공유한다. 시각적 일관성을 위해 **첫 무드(앵커)로 옷의 정본을 생성**한 뒤, 나머지 무드는 그 정본 이미지를 **레퍼런스(2번째 입력 이미지)** 로 함께 보내 같은 옷을 입히도록 한다.
- **이유**: 무드별로 독립 생성하면 같은 "후드티"라도 색·무늬가 제각각 나온다. 정본을 레퍼런스로 강제하면 종류·색·디테일이 강하게 일치.
- **영향**:
  - `GmsImageClient.dressGulbi(..., referenceB64)` 5번째 인자 추가(앵커는 `null`). `GulbiRewardService.generate`가 앵커→레퍼런스 순으로 호출.
  - 생성 모델 특성상 **픽셀 단위 동일은 보장 못 함**. 더 엄격히 맞추려면 프롬프트에 색까지 고정. 호출 수는 7회 유지.

## DEC-0021 · 굴비 옷: 고정 목록 제거 → Gemini 랜덤 생성 + 한글 이름
- **날짜**: 2026-06-23
- **결정**: 옷을 6종 고정 목록에서 뽑던 방식을 버리고, **Gemini가 매번 새 옷을 직접 디자인**하게 한다. 앵커 호출 시 모델이 옷 이미지와 함께 **한국어 옷 이름(1~4단어)** 을 텍스트로 반환하고, 이 이름을 `reward_outfit_key`/표시 라벨로 쓴다.
- **이유**: 옷 다양성을 무한대로 늘리고 키워드 관리를 없앰. 7무드 동일 옷은 키워드가 아니라 레퍼런스 이미지로 보장되므로(DEC-0020) 랜덤화해도 일관성은 유지됨.
- **영향**:
  - `OUTFITS` 목록·`pickOutfitExcept`·`Outfit` 레코드 제거. `dressGulbi`는 `outfitName` 인자를 빼고 `DressResult(dataUrl, outfitName)` 반환. `generate`는 `GenerateResult(images, outfitName)` 반환.
  - `outfitKey`가 고정값이 아니라 자유 텍스트(한글)라, 프론트 `GulbiRewardView`의 `OUTFIT_LABEL` 맵 제거하고 `outfitKey`를 그대로 표시. API.md §7 갱신.
  - DEC-0018의 "6종 중 현재 옷 제외 랜덤" 부분을 대체. **현재 옷과 중복 회피 로직은 사라짐**(가끔 비슷한 옷이 또 나올 수 있음).
  - 이름이 없거나 비면 "랜덤 옷"으로 대체, 50자 컷(컬럼 보호).

## DEC-0022 · 생성 이미지 서버측 다운스케일(용량 절감)
- **날짜**: 2026-06-23
- **결정**: 모델이 돌려준 이미지를 그대로 저장하지 않고, 서버에서 **긴 변 기준 축소 후 PNG 재인코딩**(`shrinkToDataUrl`, 현재 `MAX_EDGE=256`)해서 반환·저장한다.
- **이유**: 단순 라인아트(원본 ~100KB)가 생성형 편집 후 색·그라데이션·노이즈가 붙어 1MB+로 커짐. 무드 7장을 base64로 DB(JSON 컬럼)·응답에 싣기 때문에 용량 부담이 큼.
- **영향**:
  - `GmsImageClient`에 AWT/ImageIO 기반 `shrinkToDataUrl` 추가, 반환 직전 적용. 투명도 유지 위해 ARGB PNG로 재인코딩. 실패 시 원본 유지(최적화 실패가 기능을 막지 않게).
  - 표시 크기(홈 92~120px·미리보기 200px)에 비해 256px면 충분. 더 줄이려면 `MAX_EDGE` 하향.

<!--
## DEC-000N · 제목
- **날짜**:
- **결정**:
- **이유**:
- **영향**:
-->

## DEC-0023 · 레포트 mood: AI 선택 → 코드 결정(4종) + AI 주입
- **날짜**: 2026-06-24
- **결정**: 굴비 표정 mood를 **AI가 고르지 않고** 코드가 데이터로 결정한다(`ReportServiceImpl.computeMood`).
  그 달 예산 초과 주 수(`failedWeeks=totalWeeks-successWeeks`)로 `≤1=happy / 2=smirk / 3=angry / ≥4=sad`.
  예산을 한 주도 안 짠 달(`totalWeeks=0`)은 전월대비(`diffRatio`)로 폴백(≤0% happy, ≤10 smirk, ≤25 angry, 그 외 sad).
  결정된 mood는 **AI 프롬프트에 주입**해 AI가 그 기분 톤으로 텍스트(oneLiner/categoryComment/advice)만 생성.
- **이유**: 굴비 이미지가 5종(hello+4)으로 축소됨 → AI가 7종 중 표시 불가 표정(warn/hungry/sulk)을 고를 위험 제거.
  기존엔 mood·텍스트를 AI가 한 번에 생성해 둘이 일관됐으므로, mood를 코드로 정하되 AI가 그 mood를 "사용"하게 해 일관성 유지.
- **영향**: `ReportNarrative.mood` 제거, `normalizeMood`/7종 분기 삭제. `schema.sql`·API.md mood 4종 갱신.
  프론트 `GulbiMascot` 5종 정리, 레포트는 미지원 값→happy 흡수. mood는 이미지 선택 전용(말투엔 직접 영향 없었음).

## DEC-0024 · 레포트 부가지표 extra_json 스냅샷 + 메모리 최대 12개월
- **날짜**: 2026-06-24
- **결정**: 하루평균·무지출일·가장 큰 하루·가장 아낀/늘어난 항목·주차별 달성을 `monthly_report.extra_json` **1컬럼 JSON 스냅샷**(`category_json` 패턴, `ReportExtra` DTO).
  전월 비교 도넛 2개 위해 `CategoryDiffItem`에 `prevAmount`/`prevRatio` 추가, 전월·당월 **합집합** 구성.
  메모리 1개월 → **최대 12개월**(`selectRecentReports`), 프롬프트엔 단계적 압축. 과거 다짐은 `ReportMemory`로 응답에 실어 "굴비가 기억하는 너" 카드 노출.
- **이유**: 레포트 보강을 데이터 재사용으로. 월 1회 캐싱이라 스냅샷 저장이 일관적. 컬럼 다수 대신 JSON 1컬럼으로 스키마 변경 최소화.
- **영향**: 실제 거래액은 **원본 그대로**, 하루평균만 원 단위 반올림. 신규 쿼리 `selectWeeksByMonth`·`selectDailyExpense`.
  프론트 그림판 테마 기준(StatsView 색연필 도넛 미러링) + 월 선택 MonthPicker 모달.
- **참고**: data.sql에 과거 6개월 주간 예산이 없어 과거 레포트가 전부 예산 미입력으로 떴음 → 2025-12~2026-05 주별 예산 시드 추가(mood 4종 다양화).
