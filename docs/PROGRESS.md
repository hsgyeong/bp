# PROGRESS — 작업 진행 로그

> 누가 · 무엇을 · 어디까지 했는지 짧게 기록. 최신 항목을 **위에** 추가.
> 도구(Claude/Codex)와 담당자를 함께 적으면 상대가 맥락을 파악하기 쉽습니다.

## 진행 중 (In Progress)

| 담당 | 도구 | 작업 | 브랜치 | 비고 |
|------|------|------|--------|------|
| | | | | |

## 완료 (Done)

| 날짜 | 담당 | 도구 | 작업 |
|------|------|------|------|
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

- [ ] DB 스키마 DDL 확정 (5개 테이블)
- [ ] 인증(JWT) 백엔드 구현
- [ ] 목업 → Vue 컴포넌트 이식 (4탭 라우팅)
- [ ] 프론트-백 API 연동 (axios)
