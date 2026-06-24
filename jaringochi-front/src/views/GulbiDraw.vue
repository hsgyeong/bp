<script setup>
import { ref } from 'vue'
import { drawGulbiReward, decideGulbiReward } from '../api/gulbiReward';

import hello from '@/assets/gulbi/gulbi-hello.png'
import happy from '@/assets/gulbi/gulbi-happy.png'
import sad   from '@/assets/gulbi/gulbi-sad.png'
import smirk from '@/assets/gulbi/gulbi-smirk.png'
import angry from '@/assets/gulbi/gulbi-angry.png'
import { P } from 'vue-router/dist/index-BQLwgiyK.js';

const props = defineProps({
    weeklyBudgetId: { type: Number, required: true },
})

const baseAssets = { hello, happy, sad, smirk, angry }

const loading = ref(false)
const reward = ref(null)

async function imageUrlToBase64(url) {
    const blob = await fetch(url).then((r) => r.blob())
    return await new Promise((resolve) => {
        const reader = new FileReader()
        reader.onloadend = () => resolve(reader.result.split(',')[1])
        reader.readAsDataURL(blob)
    })
}

async function buildBaseImages(){
    const result = {}
    for (const [mood, url] of Object.entries(baseAssets)) {
        result[mood] = {
            mimeType: 'image/png',
            base64: await imageUrlToBase64(url),
        }
    }
    return result
}

async function draw() {
    loading.value = true
    try {
        const baseImages = await buildBaseImages()
        const res = await drawGulbiReward(props.weeklyBudgetId, { baseImages })
        reward.value = res.data.data
    } finally {
        loading.value = false
    }
}

async function accept() {
  await decideGulbiReward(props.weeklyBudgetId, 'ACCEPT')
  alert('새 옷으로 갈아입었어요!')
}

async function decline() {
  await decideGulbiReward(props.weeklyBudgetId, 'DECLINE')
  alert('기존 상태를 유지할게요.')
}
</script>

<template>
  <section class="gulbi-reward">
    <template v-if="!reward && !loading">
      <p class="title">이번 주 절약에 성공했어요!</p>
      <button class="draw-btn" @click="draw">굴비 옷 뽑기</button>
    </template>

    <template v-if="loading">
      <div class="magic-loading">
        <div class="spark"></div>
        <p>마법의 굴비 옷장을 여는 중...</p>
      </div>
    </template>

    <template v-if="reward && !loading">
      <p class="title">새 옷을 뽑았어요!</p>
      <img class="gulbi-preview" :src="reward.images.happy" alt="새 옷을 입은 굴비" />

      <div class="actions">
        <button @click="accept">갈아입기</button>
        <button class="ghost" @click="decline">기존 상태 유지</button>
      </div>
    </template>
  </section>
</template>

<style scoped>
.gulbi-reward {
  padding: 20px;
  text-align: center;
}

.title {
  font-weight: 800;
  margin-bottom: 16px;
}

.draw-btn,
.actions button {
  border: 0;
  padding: 12px 18px;
  border-radius: 12px;
  background: #f2a33c;
  font-weight: 800;
  cursor: pointer;
}

.actions {
  display: flex;
  justify-content: center;
  gap: 10px;
}

.actions .ghost {
  background: #f4eadb;
}

.gulbi-preview {
  width: 180px;
  margin: 18px auto;
  display: block;
}

.magic-loading {
  min-height: 220px;
  display: grid;
  place-items: center;
  gap: 14px;
}

.spark {
  width: 130px;
  height: 130px;
  border-radius: 50%;
  background:
    radial-gradient(circle, #fff 0 18%, transparent 20%),
    conic-gradient(#f2a33c, #e8623d, #2fa98c, #5b8def, #f2a33c);
  animation: spin 1s linear infinite, pulse .8s ease-in-out infinite alternate;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@keyframes pulse {
  to { filter: brightness(1.25); transform: scale(1.08); }
}
</style>