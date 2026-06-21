<script setup>
import { useTheme } from '@/composables/useTheme'

const { theme } = useTheme()   // paint 테마: 메뉴 이모지 → Tabler 라인 아이콘

// 더보기 메뉴 — 반복 마크업을 줄이려고 배열로 관리 (ti: paint 라인 아이콘)
const menus = [
  { to: '/report',     label: '굴비 레포트', emoji: '🐟',  ti: 'ti-report' },
  { to: '/me',         label: '마이페이지', emoji: '👤',  ti: 'ti-user' },
  { to: '/categories', label: '분류 관리',  emoji: '🏷️', ti: 'ti-tag' },
  { to: '/budget',     label: '예산 설정',  emoji: '🎯',  ti: 'ti-target' },
]
</script>

<template>
  <div class="more-view">
    <h1 class="title">더보기</h1>

    <div class="card list">
      <router-link v-for="(m, i) in menus" :key="m.to" :to="m.to" class="menu"
        :class="{ 'paint-hline-b': i < menus.length - 1 }">
        <span class="mic">
          <i v-if="theme === 'paint'" class="ti" :class="m.ti" aria-hidden="true"></i>
          <template v-else>{{ m.emoji }}</template>
        </span>
        {{ m.label }}
        <span class="arr">›</span>
      </router-link>
    </div>
  </div>
</template>

<style scoped>
.more-view { padding: 16px 20px 24px; }
.title { font-size: 18px; margin-bottom: 14px; }

.list { padding: 4px 4px; }
.menu {
  display: flex; align-items: center; gap: 13px;
  padding: 15px 12px; font-size: 15px; font-weight: 700;
  color: var(--ink); text-decoration: none;
}
.mic { font-size: 19px; }
.arr { margin-left: auto; color: var(--mute); font-size: 20px; font-weight: 600; }
</style>