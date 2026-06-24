# PROGRESS - 작업 진행 로그

> 누가 · 무엇을 · 어디까지 했는지 짧게 기록. 최신 항목을 **위에** 추가.

## 진행 중 (In Progress)

| 담당 | 작업 | 브랜치 | 비고 |
|------|------|--------|------|
| pearlseo73 | **AI 월간 레포트 + 굴비 한마디** 백엔드 | feat/report2 | 백엔드 완성(컴파일 통과). `domain/report`(controller/service/dao/dto) + `monthly_report` 테이블 + `REPORT_*` ErrorCode(R400/404/409/503). **Spring AI `ChatClient` + GMS 경유**(`gpt-5.4-mini`), AI 2회 호출(레포트 생성 `.entity()`·굴비 한마디 `.content()`), 통계/예산 서비스 재사용, 지난달 연속성(DB조회), 실패 시 폴백. ⚠️ 실행엔 `jaringochi/.env`에 `GMS_KEY` 필요(gitignore됨). **남음**: 프론트(`api/report.js`·`ReportView.vue`·라우트), GMS키 미설정 시 일반 OpenAI키 폴백(보류), `GmsConfig.java`(이미지용·미사용) 정리 |
| pearlseo73 | 알림 트리거(생성) 백엔드 + 컨트롤러 실인증 통일 + threshold 버그픽스 | feat/notification-trigger | **PR 대기**. 지출 등록 시 임계치 알림 자동 생성(DEC-0011·0012). Swagger 검증 완료: 25~150 단계 생성·같은 단계 중복차단·다단계 점프 시 최고 1건. hsgyeong 공유 필요: `schema.sql` 2곳(UNIQUE 안전망 + threshold `TINYINT`->`SMALLINT`), `TransactionServiceImpl.addTransaction` 연결 1줄 |

## 완료 (Done)

| 날짜 | 담당 | 작업 |
|------|------|------|
| 2026-06-24 | pearlseo73 | **레포트 "굴비의 총평" 카드 추가 + "굴비가 기억하는 너" 제거** — memory 기능 전면 삭제(`ReportMemory` DTO·`attachMemory`·`MonthlyReport.memory`·프론트 카드). "굴비의 한 수"(advice) 바로 위에 **300자 총평** `story` 카드 신설. story는 **추가 AI 호출 없이** `applyAiNarrative` 단일 구조화 호출에 필드 1개 추가로 생성, DB 캐싱 위해 `monthly_report.story`(VARCHAR(1000)) 컬럼·매퍼 반영. 실패 시 숫자 기반 폴백. API.md §8-1 갱신 |
| 2026-06-24 | hsgyeong | **가계부 달력 날짜 상세 모달** - 달력 뷰에서 거래 있는 날짜 클릭 시 **화면 중앙 모달**로 그날 수입/지출/합계 + 거래 목록 표시(항목 클릭→수정 이동, Esc·배경 클릭으로 닫힘, 월 변경 시 자동 닫힘). 모달 및 수입/지출/합계 박스 테두리를 그림판 테마 손그림 선(`.paint-box`)으로 통일. 거래 수정 화면 다녀와도 보던 탭(달력)이 유지되도록 `viewMode`를 `sessionStorage`에 저장/복원(DEC-0025). `LedgerView.vue` 단일 파일. ※ 작업 중 디스크 풀(C: 98%)로 파일이 0바이트 손상되어 HEAD에서 복구 후 재작성 |
| 2026-06-23 | hsgyeong | **굴비 보상 고도화** - 옷을 고정 6종 대신 **Gemini 랜덤 생성**으로 전환(`OUTFITS`/`pickOutfitExcept` 제거, 앵커가 옷+한국어 이름 반환 · DEC-0021). 레퍼런스 프롬프트 강화(옷만 변경·외형/표정 불변·옷 없는 결과 금지). 결과 이미지 **서버측 다운스케일**(`shrinkToDataUrl`, MAX_EDGE=256 · DEC-0022). 프론트 `GulbiRewardView`의 `OUTFIT_LABEL` 맵 제거 → `outfitKey`(모델 한글 이름) 직접 표시. API.md §7 갱신 |
| 2026-06-22 | hsgyeong | **굴비 보상(절약 성공 리워드) 기능** - `gulbi` 도메인 신설(draw/decision/조회) + `weekly_budget`·`user` 보상 컬럼 + 자격검사(주 종료·지출≤예산·미결정) + 7무드 동일 옷(앵커→레퍼런스, DEC-0020). 이미지 생성 제공자: Claude(불가)·OpenAI(유료)·Google직접(무료티어 0) 검토 후 **SSAFY GMS 경유 Gemini** 채택(DEC-0019). 버그픽스: 컨트롤러 `@RequestBody`를 Swagger→Spring으로 교체(본문 null NPE), 불필요한 `GmsConfig` 삭제. 프론트 `GulbiRewardView`·`api/gulbiReward.js`·홈 보상카드·그림판 테마 마스코트 연동. API.md §7 추가 |
| 2026-06-14 | hsgyeong | **거래 목록 검색·정렬 API 명세 추가** - `GET /api/transactions`에 `keyword`(메모/카테고리명 검색), `sort`(`date_desc`, `date_asc`, `amount_desc`, `amount_asc`) 쿼리 파라미터 문서화 |
| 2026-06-14 | hsgyeong | **Refresh Token 기반 로그아웃 보완** - 로그인 응답에 `refreshToken` 추가, `refresh_token` 테이블/DAO/Mapper/Service 추가, 로그아웃·회원탈퇴 시 Refresh Token 폐기, 프론트 auth store·로그인·마이페이지·프로필 수정 화면의 토큰 상태 동기화, 로그아웃·회원탈퇴 완료 안내 alert 처리, API.md 인증 명세 갱신 |
| 2026-06-12 | pearlseo73 | **알림 조회 백엔드 4종**(목록/안읽음수/단건읽음/전체읽음) - DTO·DAO·Mapper·Service·Controller + 소유권 검증(404/403). feat/notification PR#19 머지 |
| 2026-06-12 | pearlseo73 | **주간예산 화면**(사용률 게이지·최근 5주·설정/수정 폼) + `api/budget.js` + `/budget` 라우트 + 더보기 링크. feat/budget-front PR#16 머지 |
| 2026-06-12 | hsgyeong | 회원가입 화면 구현. feat/signup PR#18 머지 |
| 2026-06-11 | hsgyeong | 로그인 화면 + 인증 API 연동. feat/login-front PR#15 머지 |
| 2026-06-11 | pearlseo73 | **카테고리 화면**(목록·수입/지출 탭·추가·삭제) + 토큰 인터셉터(http.js). feat/category-front PR#14 머지 |
| 2026-06-11 | hsgyeong | **JWT 인증** - oauth2 resource server 방식으로 인증 흐름 통일. PR#13 머지 |
| 2026-06-11 | hsgyeong | **거래(transaction) 백엔드** CRUD(목록/단건/등록/수정/삭제) + 소유권·카테고리 검증. PR#12 머지 |
| 2026-06-11 | pearlseo73 | **주간예산 백엔드 4종**(current/recent/post/put, 월 2회 수정 제한) - 월간 예산 제거(DEC-0009)·최근 5주로 변경(DEC-0010) |
| 2026-06-08 | pearlseo73 | **카테고리 API 백엔드** 완성(DTO·DAO·Mapper·Service·Controller) + ErrorCode 카테고리/예산 코드 + MyBatisConfig |
| 2026-06-04 | pearlseo73 | 개발 계획 수립(기능 수직 분할·7일 일정·브랜치 전략), 목업 15화면 난이도 평가 후 분담 확정, 노션 작업보드 2개 생성. 상세는 AGENTS §8 / DECISIONS DEC-0005~0007 |
| 2026-06-03 | pearlseo73 | AGENTS.md / CLAUDE.md 작성, PROGRESS·DECISIONS 템플릿 추가 |
| 2026-06-?? | 공통 | 백엔드 초기 골격(Spring Boot 4 + MyBatis), 프론트 Vite 스캐폴드, 목업 15화면 HTML+png, docs/API.md 명세 |

## 알려진 이슈 (Known Issues)

- ~~MyBatis 매퍼 경로 불일치~~ -> **해결됨**: `application.properties`(`classpath:mappers/**/*.xml`)와 실제 폴더(`resources/mappers/`)가 모두 복수 `mappers`로 통일됨.
- `ratio` 컬럼이 `DECIMAL(5,2)`(최대 999.99) - 예산을 아주 적게 잡고 지출이 1000%를 넘으면 저장 실패 가능. 드문 케이스라 보류, 필요 시 자릿수 확대.
- **굴비 보상 운영 주의** - (1) `gms.model`이 GMS 실제 카탈로그 이름과 일치해야 함(목록 확인), 인증 안 되면 `Authorization: Bearer`로 대체. (2) 뽑기 1회=이미지 7회 호출이라 GMS 쿼터/지연 부담 → 테스트 시 프론트 무드 1~2개로 축소. (3) `httpClient` 타임아웃 미설정 → 멈추면 길게 블로킹(개선 권장). (4) `gemini-2.5-flash-image`를 Google **직접** 호출하면 무료 티어 한도 0(429)이라 GMS 경유 필수.

## 다음 할 일 (Backlog)

### 2026-06-13 · 다음 큰 덩어리 (pearlseo73 우선순위)

- [ ] **통계 백엔드** (`feat/statistics`) - `GET /statistics/by-category`·`/summary`(API §6). 별도 테이블 없이 transaction GROUP BY/SUM 집계. `StatisticsMapper.xml` stub·MyBatisConfig 등록은 이미 준비됨. pearlseo73 단독.
- [ ] **알림 프론트** - 종 아이콘 뱃지(`unread-count`)·목록(기본 `isRead=0`)·지출 저장 직후 토스트(`GET /notifications?isRead=0`). 트리거 머지 후. pearlseo73 단독.
- [ ] **UserController 실인증 전환** - 내 정보(GET/PUT/DELETE `/users/me`)의 `getCurrentUserId`가 아직 1L인지 점검(DEC-0013 잔여).
- [ ] 홈 화면 조립·프로필 화면 (hsgyeong)

### 2026-06-13 · 알림 트리거 후속 (pearlseo73)

- [ ] **거래 수정 시 알림 재평가** - 지금은 `addTransaction`(등록)에만 `evaluateExpense` 호출.
  `updateTransaction`에도 `if (transaction.getType() == 2) notificationService.evaluateExpense(userId, transaction.getDate());`
  한 줄을 넣으면 API 3-4 "수정으로 지출 비율이 바뀌면 알림 재평가"가 충족됨.
  `crossed > already` 로직이 중복/하향 발송을 자동 차단하므로 안전. MVP는 등록만 먼저, 여유 생기면 추가.
  - (관련: `TransactionServiceImpl.updateTransaction` - hsgyeong 소유 파일, 협의 필요)

### 카테고리/예산 미해결 결정 (pearlseo73)

- [ ] **응답 `isDefault` 처리** - API.md 카테고리 응답엔 `isDefault`가 있는데 `Category` DTO엔 없음(`userId == null`이면 true인 파생값). (a) DTO 필드 추가+Service 계산 / (b) `userId` 노출 후 프론트 판단. MVP는 (b) 가능, 추후 결정.
- [ ] **카테고리 display_order 정책** - MVP는 옵션 X(`ORDER BY type, id` 생성순, 자동설정 없음). 드래그 정렬(보너스) 도입 시 옵션 Y(`max+1` 부여)로 전환.

### 일반

- [x] DB 스키마 DDL 확정 (6개 테이블) - schema.sql (`refresh_token` 포함)
- [x] 인증(JWT) 백엔드 구현 - oauth2 resource server
- [x] Refresh Token DB 저장/로그아웃·회원탈퇴 폐기 흐름 구현 - Access Token 재발급 API는 아직 미구현
- [x] 카테고리 ErrorCode 추가 / 예산 소유권 403 - `USER_FORBIDDEN`(U403) 재사용으로 정리됨
- [ ] 목업 -> Vue 이식 - 카테고리·예산·로그인·회원가입 완료 / 홈·통계·프로필·거래 화면 남음
- [ ] 프론트-백 API 연동 - 진행 중(완료된 화면 단위로 axios 연동)
- [ ] Access Token 재발급 API(`/api/auth/refresh`) 검토 - Refresh Token 유효성 검증 로직은 준비됐지만 엔드포인트/프론트 재발급 흐름은 미구현

### 레포트 도메인 보강 (pearlseo73, 2026-06-24)

- [x] **굴비 표정 7종→4종 + 코드 결정식** - `mood`(happy/smirk/angry/sad)를 `ReportServiceImpl.computeMood`가
  그 달 예산 초과 주 수(≤1/2/3/≥4)로 결정, 예산 없으면 전월대비 폴백. AI는 mood를 고르지 않고
  주입된 mood 톤으로 텍스트만 생성(`ReportNarrative.mood` 제거).
- [x] **전월/당월 카테고리 도넛 2개** - `CategoryDiffItem`에 `prevAmount`/`prevRatio` 추가, `buildDiffs` 합집합.
  프론트 `ReportView.vue` 도넛 2개(색연필/wobble, StatsView 미러링), 색은 categories 위치 기준 일관.
- [x] **부가 지표(extra_json)** - 하루평균(원 단위 반올림)·무지출일·가장 큰 하루·가장 아낀/늘어난 항목·주차별 달성.
  `monthly_report.extra_json` 컬럼 추가, `ReportExtra` DTO. 실제 거래액은 원본 그대로.
- [x] **메모리 1개월→최대 12개월** - `ReportDao.selectRecentReports`. 내러티브/굴비한마디 프롬프트에 단계적 압축 주입.
- [x] 신규 쿼리: `BudgetDao.selectWeeksByMonth`, `StatisticsDao.selectDailyExpense`.
- [x] `ReportView.vue` 그림판 테마 기준 재작성, `GulbiMascot.vue` 주석 5종 정리. API.md/schema.sql 갱신.
- [x] **"굴비가 기억하는 너" 카드** - `ReportMemory` DTO로 과거 가장 최근 다짐을 응답에 주입(`attachMemory`,
  getMonthly/talk 양쪽). 프론트 카드 추가.
- [x] **월 선택 모달** - 레포트 월 라벨에 `MonthPicker`(통계와 동일) 연결 → 과거 달 바로 이동(엘리스 메모리 테스트 가능).
- [x] **파이차트 총액** - 전월/당월 도넛 위에 각 달 총 지출(원본 금액) 캡션.
- [x] **data.sql 과거 예산 시드** - 2025-12~2026-05 주별 예산 추가(이전엔 6월만 있어 과거 레포트가 전부 예산
  미입력 → mood 폴백만 → 표정 2종). 이제 mood 4종 다양화.
- [x] **pom 인코딩 수정** - `project.build.sourceEncoding=UTF-8` 추가 → `mvnw compile` CLI 빌드 정상화.
- [ ] **(확인 필요) 파이 시작각** - 첫 카테고리는 12시(offset 0)에서 시작하게 되어 있음. 재시드 후 실행해
  4월/5월 도넛 top 확인 권장(이전 어긋남은 데이터 부족/관찰 가능성).

### 레포트/홈 UI 보완 (pearlseo73, 2026-06-24)

- [x] **전월/당월 도넛 크기 통일** - `ReportView.vue` `.donut.sub` 104px→132px(당월과 동일).
- [x] **도넛 색 중복 제거** - 팔레트 5색→8색(classic/paint 각각). 카테고리 6개 이상일 때 `기타` 다음(전월 전용)
  항목이 첫 항목과 같은 색이 되어 전월 도넛 시작/끝이 붙어 보이던 문제 해소.
- [x] **`기타` 펼쳐보기** - `기타`에 합쳐진 세부 카테고리를 응답에 포함하고 범례에서 탭하면 펼침.
  백엔드: `CategoryStatItem.members` + `StatisticsServiceImpl.collapseTail`에서 수집, `CategoryDiffItem.members` +
  `ReportServiceImpl.buildDiffs`에서 전달. 프론트: `ReportView.vue` 범례 행 토글 + 세부 목록. **API.md 갱신**.
  ⚠️ 레포트는 월 1회 생성 후 캐싱(`categoryJson`)이라 **기존 생성월은 재생성해야 members 표시**(미생성 달 조회 또는 row 삭제).
- [x] **홈 굴비 확대** - `HomeView.vue` paint 마스코트 92→120px, classic SVG/그리드 칼럼·모바일 분기 동반 확대.
