<!--

참고: <router-link> vs <a>

<router-link to="/stats">는 누르면 새로고침 없이 주소를 /stats로 바꿈.(아래 코드에서는 반복문(v-for)으로 구현) 
-> <router-view> 자리가 StatsView로 교체. 

일반 <a href>처럼 페이지를 통째로 다시 안 불러와서 빠르고 부드러움.

src/router/index.js 참고

-->

<script setup>
import NotificationBell from '@/components/NotificationBell.vue'
import { useTheme } from '@/composables/useTheme'

// 하단 탭 정의 — 반복 마크업을 줄이려고 배열로 관리 (아래에서 v-for 사용)
// ti: paint 테마에서 쓰는 Tabler 라인 아이콘 클래스 (classic은 icon 이모지)
const tabs = [
  { to: '/',       label: '홈',     icon: '🏠', ti: 'ti-home' },
  { to: '/ledger', label: '가계부', icon: '📒', ti: 'ti-notebook' },
  { to: '/stats',  label: '통계',   icon: '📊', ti: 'ti-chart-bar' },
  { to: '/more',   label: '더보기', icon: '⋯',  ti: 'ti-dots' },
]

// 테마 전환 (classic ↔ paint) — 상태는 useTheme composable이 전역 관리
const THEME_LABEL = { classic: '🎨 기본', paint: '✏️ 그림판' }
const { theme, cycle: cycleTheme } = useTheme()
</script>

<template>
  <!-- 손그림 테두리용 SVG 필터 (paint 테마에서 filter:url(#paintWobble)로 참조).
       화면엔 안 보이지만 문서 어디서나 쓸 수 있게 App 최상단에 1회 삽입. -->
  <svg width="0" height="0" style="position:absolute" aria-hidden="true">
    <defs>
      <filter id="paintWobble" x="-15%" y="-15%" width="130%" height="130%">
        <feTurbulence type="fractalNoise" baseFrequency="0.018" numOctaves="2" seed="7" result="n" />
        <feDisplacementMap in="SourceGraphic" in2="n" scale="3.5" xChannelSelector="R" yChannelSelector="G" />
      </filter>
      <!-- 작은 아이콘용: 더 잘게 떨리되, 잘리지 않게 영역을 넉넉히 -->
      <filter id="paintWobbleSmall" x="-50%" y="-50%" width="200%" height="200%">
        <feTurbulence type="fractalNoise" baseFrequency="0.045" numOctaves="2" seed="4" result="n" />
        <feDisplacementMap in="SourceGraphic" in2="n" scale="2.6" xChannelSelector="R" yChannelSelector="G" />
      </filter>
      <!-- 긴 가로/세로 선용: 얇은 선도 손그림처럼 또렷이 울렁이게 (잦은 진동 + 큰 진폭) -->
      <filter id="paintWobbleLine" x="-15%" y="-60%" width="130%" height="220%">
        <feTurbulence type="fractalNoise" baseFrequency="0.028" numOctaves="2" seed="9" result="n" />
        <feDisplacementMap in="SourceGraphic" in2="n" scale="5" xChannelSelector="R" yChannelSelector="G" />
      </filter>
    </defs>
  </svg>

  <!-- 테마 전환 토글 (디자인 비교용). classic ↔ paint 즉시 전환 -->
  <button class="theme-toggle" type="button" @click="cycleTheme">
    {{ THEME_LABEL[theme] }}
  </button>

  <!-- 전역 알림 종 + 드롭다운 (우상단 고정, 로그인 시에만 표시) -->
  <NotificationBell />

  <main class="content">
    <router-view />    <!-- 현재 주소에 맞는 화면이 여기 끼워짐 -->
  </main>

  <!-- 하단 4탭 바 (paint: paint-hline 유틸로 윗선을 손그림 가로선으로) -->
  <nav class="tabbar paint-hline">
    <router-link
      v-for="tab in tabs"
      :key="tab.to"
      :to="tab.to"
      class="tab"
    >
      <span class="tab-icon">
        <i v-if="theme === 'paint'" class="ti" :class="tab.ti" aria-hidden="true"></i>
        <template v-else>{{ tab.icon }}</template>
      </span>
      <span class="tab-label">{{ tab.label }}</span>
    </router-link>
  </nav>
</template>

<style scoped>
.content {
  padding-bottom: 76px;   /* 탭바에 가려지지 않게 아래 여백 */
}
.tabbar {
  position: fixed;        /* 화면 하단에 고정 */
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);  /* 가운데 정렬 */
  width: 100%;
  max-width: 480px;       /* 앱 폭(#app)과 맞춤 */
  height: 64px;
  display: flex;
  background: var(--card);
  border-top: 1px solid var(--line);
}
.tab {
  flex: 1;                /* 4칸 똑같이 나눔 */
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  text-decoration: none;
  color: var(--mute);
  font-size: 11px;
  font-weight: 600;
}
.tab-icon { font-size: 20px; }

/* 현재 선택한 탭만 강조('exact') */
.tab.router-link-exact-active {
  color: var(--gold-deep);
}

/* paint 테마: 탭바 윗선은 .paint-hline 유틸(style.css)이 손그림 가로선으로 그림.
   여기선 기존 직선 border만 숨기고 선 두께만 지정. */
:root[data-theme="paint"] .tabbar {
  border-top-color: transparent;
  --hand-line-w: 2.5px;
}

/* 테마 전환 토글 — 화면 우하단 고정 (탭바 위) */
.theme-toggle {
  position: fixed;
  bottom: 78px;
  right: 16px;
  z-index: 100;
  padding: 6px 12px;
  font-size: 13px;
  font-weight: 600;
  background: var(--card);
  color: var(--ink);
  border: 1.5px solid var(--line);
  border-radius: 999px;
  box-shadow: var(--shadow);
  cursor: pointer;
}
</style>