# 자린고비 가계부 — Figma 목업 생성기 (플러그인)

HTML을 Figma로 "변환"한 게 아니라, **Figma Plugin API로 같은 화면을 네이티브 레이어(오토레이아웃 프레임·텍스트·도형·차트)로 직접 생성**하는 코드입니다. 실행하면 캔버스에 15개 화면이 한 번에 그려지고, 모든 요소를 Figma에서 자유롭게 편집할 수 있습니다.

## 실행 방법 (개발용 플러그인 등록)
1. **Figma 데스크톱 앱**을 연다 (웹 버전은 로컬 플러그인 등록 불가).
2. 아무 디자인 파일이나 새로 만든다.
3. 메뉴: **Plugins → Development → Import plugin from manifest…**
4. 이 폴더의 `manifest.json` 을 선택한다.
5. 메뉴: **Plugins → Development → 자린고비 가계부 목업 생성기** 실행.
6. 캔버스에 15개 화면이 5개씩 3줄로 생성됨 → 끝.

## 폰트
한글 폰트를 설치된 것 중에서 자동 선택합니다 (우선순위):
`Pretendard → Noto Sans KR → Apple SD Gothic Neo → Malgun Gothic → Spoqa Han Sans Neo → Inter`.
- 가장 권장: **Pretendard** 설치 (웹 시안과 동일). https://github.com/orioncactus/pretendard
- 아무것도 없으면 Inter로 fallback (한글이 □로 보일 수 있으니 한글 폰트 1개는 설치 권장).
- 굵기는 Regular/Bold 두 가지만 사용 → Figma에서 원하는 weight로 바꾸면 됨.

## 생성되는 화면 (한 줄에 5개)
1줄: 로그인 · 홈 · 가계부(일자별) · 통계(월·단순금액) · 회원가입
2줄: 가계부(달력) · 통계(월·카테고리) · 통계(주·단순금액) · 더보기 · 거래등록
3줄: 카테고리설정 · 예산설정 · 내프로필 · 탈퇴(바텀시트) · 비밀번호수정

## 구조
- `code.js` — 컬러 토큰 + 재사용 컴포넌트 헬퍼(카드/탭바/세그먼트/차트/굴비 등) + 화면 15개 빌더
- 차트: 꺾은선=Vector, 도넛=Ellipse arcData, 막대=Rectangle 로 네이티브 생성
- 굴비 마스코트는 도형 조합(타원+삼각형)으로 간단 버전 — 정교한 일러스트는 Figma에서 교체 권장

## 수정 팁
- 색은 `code.js` 상단 `C` 객체(컬러 토큰)만 바꾸면 전체 일괄 반영.
- 특정 화면만 빼고 싶으면 `main()` 의 `builders` 배열에서 해당 함수 제거.
- 각 화면 빌더는 try/catch로 감싸져 있어, 하나가 실패해도 나머지는 생성됨 (콘솔에 실패 화면명 로그).
