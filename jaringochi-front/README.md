# 자린고비 가계부 — 프론트엔드 (jaringochi-front)

Vue 3 + Vite 기반. 모바일 가계부 앱 (max-width 480px, 모바일 퍼스트).

## 실행 방법

```bash
npm install      # 의존성 설치 (최초 1회, node_modules 생성)
npm run dev      # 개발 서버 실행 → http://localhost:5173
```

> 백엔드(:8080)도 함께 떠 있어야 API가 동작.
> `vite.config.js`의 프록시가 `/api` 요청을 `localhost:8080`으로 전달.

## 앱 실행(부팅) 순서

전체
```
[index.html]
   │  main.js가 시동
   ▼
createApp(App).use(Pinia).use(router).mount('#app')
   │  #app 자리에 App.vue를 띄움
   ▼
[App.vue: 탭바 + <router-view>]
   │  주소에 맞는 화면을 <router-view>에 끼움
   ▼
[views/*.vue: HomeView, StatsView ...]
```

큰 흐름
```
index.html → main.js → App.vue → <router-view> → views/*.vue
 (껍데기)     (시동)    (레이아웃)   (화면 자리)    (실제 화면)
```

main.js 부분
```
main.js:  createApp(App) → .use(Pinia) → .use(router) → .mount('#app')
            앱 생성          창고 장착        라우터 장착      #app에 띄움
```

## 화면 전환 흐름

```
사용자가 하단 탭(<router-link>) 클릭
  → 주소(URL) 변경  (예: / → /stats)
  → router/index.js 의 routes 표에서 매칭되는 컴포넌트 결정
  → App.vue 의 <router-view> 자리에 해당 화면 교체
  (탭바 등 공통 레이아웃은 그대로 유지)
```

## 폴더 구조

관심사 분리 - 역할별로 파일을 나누는 것
views/(화면)·stores/(데이터창고)·router/(길)·api/(통신)

```
src/
├─ main.js          앱 시동 + 라우터·Pinia 장착
├─ App.vue          최상위 레이아웃 (하단 4탭 + <router-view>)
├─ style.css        전역 스타일 (디자인 토큰 + 리셋 + 공통 .card)
│
├─ router/
│   └─ index.js     주소↔화면 매핑(routes) + 라우트 가드(beforeEach)
│
├─ stores/          Pinia — 화면들이 공유하는 데이터 창고
│   └─ auth.js      로그인 토큰·유저 (현재 빈 껍데기, JWT 후 채움)
│
├─ api/             서버 통신 계층 (axios)
│   └─ http.js      공통 axios 인스턴스 (baseURL '/api' + 인터셉터 자리)
│
├─ views/           화면 단위 컴포넌트 (라우터의 목적지)
│   ├─ HomeView.vue / LedgerView.vue / StatsView.vue / MoreView.vue
│   ├─ LoginView.vue / SignupView.vue
│   └─ CategoryView.vue
│
└─ components/      여러 화면이 재사용하는 부품 (예정)
```

## 역할 한 줄 요약

| 영역 | 역할 |
|------|------|
| `router/` | **어떤** 화면을 보여줄지 (주소 → 화면, 이동 규칙) |
| `views/`  | 각 화면의 실제 내용 |
| `stores/` | 화면들이 공유하는 데이터 (Pinia) |
| `api/`    | 서버와 통신 (axios) |
| `style.css` | 전역 디자인 토큰·기본 스타일 |

## 설정 메모

- **`@` 별칭** = `src/` (`vite.config.js`). 예: `@/views/HomeView.vue`
- **프록시**: `/api/*` → `http://localhost:8080` (CORS 회피)
- **디자인 토큰**: 골드 `#F2A33C` / 크림 `#FBF5EA` / 수입 `#2FA98C` / 지출 `#E8623D`, 폰트 Pretendard
- **인증**: 미구현. axios 인터셉터(`api/http.js`)와 라우트 가드(`router/index.js`)에 자리만 둠 → JWT 학습 후 그 자리만 교체