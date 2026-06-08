# PROGRESS — 작업 진행 로그

> 누가 · 무엇을 · 어디까지 했는지 짧게 기록. 최신 항목을 **위에** 추가.
> 도구(Claude/Codex)와 담당자를 함께 적으면 상대가 맥락을 파악하기 쉽습니다.

## 진행 중 (In Progress)

| 담당 | 도구 | 작업 | 브랜치 | 비고 |
|------|------|------|--------|------|
| 팀원 B | Claude | 카테고리 API (DTO·DAO 완료 → Mapper XML·Service·Controller 진행) | feat/category | 끝나면 즉시 PR 머지 (A 거래등록 선행) |

## 완료 (Done)

| 날짜 | 담당 | 도구 | 작업 |
|------|------|------|------|
| 2026-06-04 | | Claude | 개발 계획 수립(기능 수직 분할·7일 일정·브랜치 전략), 목업 15화면 난이도 평가 후 화면/도메인 A·B 분담 확정, 노션 작업보드 2개(팀원 A/B DB, 각 14행) 생성 + 간트·보드 뷰 구성. 상세는 AGENTS §8 / DECISIONS DEC-0005~0007 / 노션 참고 |
| 2026-06-03 | | Claude | AGENTS.md / CLAUDE.md 작성, PROGRESS·DECISIONS 템플릿 추가 |
| 2026-06-?? | | | 백엔드 초기 골격 (Spring Boot 4 + MyBatis), TestController |
| 2026-06-?? | | | 프론트 Vite 스캐폴드 |
| 2026-06-?? | | | 목업 15화면 HTML + png, docs/API.md 명세 |

## 알려진 이슈 (Known Issues)

- **MyBatis 매퍼 경로 불일치**: `application.properties` 의
  `mybatis.mapper-locations=classpath:mappers/**/*.xml`(복수 `mappers`)인데
  실제 XML은 `src/main/resources/mapper/`(단수 `mapper`)에 있음 → 런타임에
  매퍼 미로딩. 설정값 또는 폴더명을 한쪽으로 통일 필요.

## 다음 할 일 (Backlog)

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

### 일반

- [ ] DB 스키마 DDL 확정 (5개 테이블)
- [ ] 인증(JWT) 백엔드 구현
- [ ] 목업 → Vue 컴포넌트 이식 (4탭 라우팅)
- [ ] 프론트-백 API 연동 (axios)
