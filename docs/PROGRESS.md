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
