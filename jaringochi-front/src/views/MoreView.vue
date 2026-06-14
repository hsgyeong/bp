<script setup>
</script>

<template>
  <div class="more-view">
    <h1 class="title">더보기</h1>

    <div class="card list">
      <!-- 카테고리(분류) 관리로 이동 -->
      <router-link to="/categories" class="menu">
        <span class="mic">🏷️</span>
        분류 관리
        <span class="arr">›</span>
      </router-link>
      <!-- 예산 설정으로 이동 -->
      <router-link to="/budget" class="menu">
        <span class="mic">🎯</span>
        예산 설정
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