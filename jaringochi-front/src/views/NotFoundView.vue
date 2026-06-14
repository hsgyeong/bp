<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <section class="not-found-view">
    <strong>페이지를 찾을 수 없습니다.</strong>
    <span>주소가 잘못되었거나 더 이상 사용할 수 없는 화면입니다.</span>

    <button type="button" @click="router.push({ name: 'home' })">
      홈으로 돌아가기
    </button>
  </section>
</template>