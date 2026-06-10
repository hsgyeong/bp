# 자린고비 가계부 (Jaringochi)

> 주 단위 지출 관리에 집중한 모바일 가계부 앱. 마스코트는 절약의 상징 굴비.

---

## 핵심 기능

| 탭 | 기능 |
|----|------|
| 홈 | 굴비 메시지, 이번 주 예산 사용률, 월 수입/지출 요약, 최근 거래 |
| 가계부 | 일자별 · 달력 보기, 거래 등록(수입/지출) |
| 통계 | `월·주 x 수입·지출 x 단순금액·카테고리별` 조합 - 꺾은선/도넛/막대 (인라인 SVG) |
| 더보기 | 카테고리 설정, 주간 예산 설정, 프로필/계정 관리 |

- 예산 초과 알림: 지출이 주간 예산의 임계치(25·50·75·100·125·150%)를 넘으면 자동 알림
- 주(週) 단위 예산: 한 주의 예산 대비 사용률을 직관적으로 (월 단위 X - 월 집계는 통계가 담당)
- 굴비 챗봇 (보너스, Spring AI): 절약 코칭

---

## 기술 스택

- 백엔드 - Java 21 · Spring Boot 4.0.6 · MyBatis · MySQL · springdoc-openapi(Swagger)
- 프론트엔드 - Vue 3.5 (`<script setup>` SFC) · Vite 8 · Vue Router · Pinia · axios
- 인증 - JWT (`Authorization: Bearer <token>`)

---

## 저장소 구조

```
bp/
├─ jaringochi/          # 백엔드 (Spring Boot, domain/<도메인>/{controller,service,dao,dto})
├─ jaringochi-front/    # 프론트엔드 (Vue 3 + Vite)
├─ mockups/             # HTML 화면 시안 15장 + 디자인 시스템
├─ docs/                # API.md · PROGRESS.md · DECISIONS.md
└─ AGENTS.md            # 공유 컨텍스트(단일 진실 문서)
```

## 도메인 (5개 테이블)

`user` · `category` · `transaction` · `weekly_budget` · `notification`
-> 컬럼·관계·엔드포인트 상세는 [docs/API.md](docs/API.md) 가 단일 기준.

REST 규약: Base URL `/api`, 인증 `Bearer` 토큰. 주요 그룹 - 인증/사용자 · 카테고리 · 거래 · 주간예산 · 알림 · 통계.

---

## 디자인 토큰

| 항목 | 값 |
|------|-----|
| 메인(골드) | `#F2A33C` |
| 배경(크림) | `#FBF5EA` |
| 수입(초록) | `#2FA98C` |
| 지출(코랄) | `#E8623D` |
| 폰트 | Pretendard |

둥근 카드 + 하단 4탭 + 플로팅 버튼, 그래프는 전부 인라인 SVG. 화면 시안: [`mockups/`](mockups/).

---

## 실행

백엔드 (MySQL 스키마 `jaringochi` 필요)
```bash
cd jaringochi
./mvnw spring-boot:run        # Windows: mvnw.cmd  -> http://localhost:8080
```

프론트엔드
```bash
cd jaringochi-front
npm install
npm run dev                   # http://localhost:5173 (/api -> :8080 프록시)
```

---

## 팀 & 분담

기능 수직 분할 - 두 명이 각자 백+프론트 풀스택으로 도메인을 담당.

- hsgyeong - 기록+계정: `auth/user` · `transaction`
- pearlseo73 - 설정+분석: `category` · `budget` · `statistics` · `notification`

협업 규칙: master 직접 커밋 금지 -> 브랜치 + PR 교차 리뷰. 

