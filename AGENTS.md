# AGENTS.md — 자린고비 가계부 (공동작업 단일 진실 문서)

> **이 파일은 Claude Code와 Codex가 함께 읽는 공유 컨텍스트입니다.**
> 두 명(Claude 사용자 / Codex 사용자)이 협업하므로, 상대 도구가 알아야 할 중요한
> 결정·구조·변경은 **여기에 적어** 주세요. 길게 늘리지 말고 핵심만, 바뀌면 갱신.
>
> - 진행 상황(누가 무엇을 어디까지) → [docs/PROGRESS.md](docs/PROGRESS.md)
> - 설계 결정 근거(왜 이렇게 했나) → [docs/DECISIONS.md](docs/DECISIONS.md)
> - API 명세(단일 기준) → [docs/API.md](docs/API.md)

---

## 1. 프로젝트 개요

- **자린고비(jaringochi) 가계부** 앱. 레퍼런스는 "편한가계부".
- 마스코트 **굴비** (자린고비 설화 — 굴비를 천장에 매달아 두고 보며 절약).
- 탭 4개: **홈 / 가계부 / 통계 / 더보기**.
- 통계는 `월·주 × 수입·지출 × 단순금액·카테고리별` 조합.

## 2. 저장소 구조

```
bp/
├─ jaringochi/          # 백엔드 (Spring Boot)
├─ jaringochi-front/    # 프론트엔드 (Vue 3 + Vite)
├─ mockups/             # HTML 화면 시안 15장 + png 내보내기 (실제 코드 아님)
├─ docs/                # API.md, PROGRESS.md, DECISIONS.md
├─ AGENTS.md            # ← 이 파일 (공유 컨텍스트)
└─ CLAUDE.md            # Claude Code용; 이 파일을 참조함
```

- 원격: `https://github.com/hsgyeong/bp.git` (브랜치: `master`)

## 3. 기술 스택

### 백엔드 (`jaringochi/`)
- **Java 21**, **Spring Boot 4.0.6**, **MyBatis** (`mybatis-spring-boot-starter 4.0.1`)
- DB: **MySQL** — 스키마 `ssafy`, 계정 `ssafy / ssafy`, `jdbc:mysql://127.0.0.1:3306/ssafy`
- 서버 포트 **8080**, springdoc-openapi(Swagger UI) 포함, Lombok 사용
- 패키지 베이스: `com.bp.jaringochi`, 도메인형 구조 `domain/<도메인>/{controller,service,...}`
- 빌드: `./mvnw spring-boot:run` (Windows: `mvnw.cmd`)

### 프론트엔드 (`jaringochi-front/`)
- **Vue 3.5** + **Vite 8** + **axios**, `<script setup>` SFC
- 실행: `npm install` → `npm run dev`

## 4. 도메인 / DB (5개 테이블)

`user`, `category`, `transaction`, `weekly_budget`, `notification`
→ 컬럼·관계·엔드포인트 상세는 **[docs/API.md](docs/API.md)** 가 단일 기준.

REST 규약: Base URL `/api`, 인증은 `Authorization: Bearer <token>` (JWT).
주요 그룹: 인증/사용자, 카테고리, 거래내역, 주간예산, 알림, 통계.

## 5. 디자인 토큰 (사용자 승인 확정)

| 항목 | 값 |
|------|-----|
| 메인(골드) | `#F2A33C` |
| 배경(크림) | `#FBF5EA` |
| 수입(초록) | `#2FA98C` |
| 지출(코랄) | `#E8623D` |
| 폰트 | Pretendard |

둥근 카드 + 하단 4탭 + 플로팅 + 버튼, 그래프는 전부 인라인 SVG, 굴비 캐릭터 강조.
화면 시안 원본: `mockups/` (HTML), 내보낸 이미지: `mockups/png/`, 상세: `mockups/README.md`.

## 6. 협업 규칙

1. **master 직접 커밋 금지** — 브랜치 따서 작업 후 PR로 머지.
2. 작업 시작/완료 시 **[docs/PROGRESS.md](docs/PROGRESS.md)** 갱신 (누가·무엇을).
3. 설계를 바꾸는 결정은 **[docs/DECISIONS.md](docs/DECISIONS.md)** 에 근거와 함께 기록.
4. **API는 docs/API.md 가 진실의 원천** — 프론트/백 모두 여기에 맞춤. 바꾸면 먼저 문서 수정.
5. 같은 파일 동시 수정 최소화를 위해 **화면/도메인 단위로 분담**.
6. 상대 도구(Claude↔Codex)가 알아야 할 변경은 이 파일 또는 PROGRESS에 남길 것.

## 7. 현재 상태 (2026-06 기준)

- 백엔드: **초기 골격만**. 동작 컨트롤러는 `TestController` (`GET /api/test`) 하나.
  매퍼 XML 4개(budget/report/statistics/user)는 자리만 잡힌 상태.
- 프론트: **Vite 기본 스캐폴드** (`App.vue` + `HelloWorld.vue`), 실제 화면 미구현.
- 목업: 15화면 완성(이미지 추출용 HTML). 실제 Vue 컴포넌트로는 아직 미이식.
- ⚠️ **알려진 이슈**: `application.properties`의
  `mybatis.mapper-locations=classpath:mappers/**`(복수) vs 실제 경로
  `resources/mapper/`(단수) 불일치 → 매퍼 로딩 안 됨. 자세한 건 PROGRESS 참고.
