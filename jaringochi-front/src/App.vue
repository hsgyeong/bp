<!--

참고: <router-link> vs <a>

<router-link to="/stats">는 누르면 새로고침 없이 주소를 /stats로 바꿈.(아래 코드에서는 반복문(v-for)으로 구현) 
-> <router-view> 자리가 StatsView로 교체. 

일반 <a href>처럼 페이지를 통째로 다시 안 불러와서 빠르고 부드러움.

src/router/index.js 참고

-->

<script setup>
// 하단 탭 정의 — 반복 마크업을 줄이려고 배열로 관리 (아래에서 v-for 사용)
const tabs = [
  { to: '/',       label: '홈',     icon: '🏠' },
  { to: '/ledger', label: '가계부', icon: '📒' },
  { to: '/stats',  label: '통계',   icon: '📊' },
  { to: '/more',   label: '더보기', icon: '⋯' },
]
</script>

<template>
  <main class="content">
    <router-view />    <!-- 현재 주소에 맞는 화면이 여기 끼워짐 -->
  </main>

  <!-- 하단 4탭 바 -->
  <nav class="tabbar">
    <router-link
      v-for="tab in tabs"
      :key="tab.to"
      :to="tab.to"
      class="tab"
    >
      <span class="tab-icon">{{ tab.icon }}</span>
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
</style>