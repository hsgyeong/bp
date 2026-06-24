<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { drawGulbiReward, decideGulbiReward, getGulbiReward } from '@/api/gulbiReward'

// 4종 무드 PNG (기본 굴비). hello는 로그인/회원가입 전용이라 제외.
import happy from '@/assets/gulbi/gulbi-happy.png'
import sad   from '@/assets/gulbi/gulbi-sad.png'
import smirk from '@/assets/gulbi/gulbi-smirk.png'
import angry from '@/assets/gulbi/gulbi-angry.png'


const baseAssets = { happy, smirk, angry, sad }
const MOOD_LABEL = {
  happy: '흐뭇', smirk: '씨익', angry: '화남', sad: '슬픔',
}

// router/index.js 에서 props:true 로 넘겨받음
const props = defineProps({
  weeklyBudgetId: { type: [String, Number], required: true },
})

const router = useRouter()

const phase = ref('idle')        // idle | preview
const loading = ref(false)
const errorMessage = ref('')

const outfitKey = ref('')
const images = ref({})           // { mood: dataURL }
const previewMood = ref('happy') // 미리보기 메인 표정

// 에셋 URL → { mimeType, base64 }
async function toBase64(url) {
  const blob = await (await fetch(url)).blob()
  return await new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onloadend = () => {
      const [meta, data] = String(reader.result).split(',')
      const mimeType = meta.match(/data:(.*);base64/)[1]
      resolve({ mimeType, base64: data })
    }
    reader.onerror = reject
    reader.readAsDataURL(blob)
  })
}

async function buildBaseImages() {
  const entries = await Promise.all(
    Object.entries(baseAssets).map(async ([mood, url]) => [mood, await toBase64(url)]),
  )
  return Object.fromEntries(entries)
}

async function onDraw() {
  loading.value = true
  errorMessage.value = ''
  try {
    const baseImages = await buildBaseImages()
    const res = await drawGulbiReward(props.weeklyBudgetId, { baseImages })
    const payload = res.data.data || res.data
    outfitKey.value = payload.outfitKey
    images.value = payload.images || {}
    previewMood.value = images.value.happy ? 'happy' : Object.keys(images.value)[0]
    phase.value = 'preview'
  } catch (e) {
    errorMessage.value =
      e.response?.data?.message || '뽑기에 실패했어요. 잠시 후 다시 시도해주세요.'
  } finally {
    loading.value = false
  }
}

async function onDecide(decision) {
  loading.value = true
  errorMessage.value = ''
  try {
    await decideGulbiReward(props.weeklyBudgetId, decision)
    router.replace({ name: 'home' })
  } catch (e) {
    errorMessage.value = e.response?.data?.message || '처리에 실패했어요.'
  } finally {
    loading.value = false
  }
}

function goHome() {
  router.replace({ name: 'home' })
}

// 이미 뽑아둔(PENDING) 보상이 있으면 재생성 없이 미리보기로 진입
onMounted(async () => {
  try {
    const res = await getGulbiReward(props.weeklyBudgetId)
    const payload = res.data.data || res.data
    if (payload && payload.rewardStatus === 'PENDING') {
      outfitKey.value = payload.outfitKey
      images.value = payload.images || {}
      previewMood.value = images.value.happy ? 'happy' : Object.keys(images.value)[0]
      phase.value = 'preview'
    }
  } catch (_) {
    /* 없으면 idle 유지 */
  }
})
</script>

<template>
  <div class="reward-view">
    <h1 class="title">굴비 옷 뽑기</h1>

    <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>

    <!-- 로딩 -->
    <div v-if="loading" class="card center">
      <div class="spinner" aria-hidden="true"></div>
      <p class="muted">굴비에게 옷을 입히는 중이에요…</p>
    </div>

    <!-- 1) 뽑기 전 -->
    <template v-else-if="phase === 'idle'">
      <div class="card center">
        <p class="lead">지난주 예산을 지켜낸 보상이에요. 🎉</p>
        <p class="muted">굴비가 어떤 옷을 받을지 뽑아볼까요?</p>
        <button class="btn primary" type="button" @click="onDraw">굴비 옷 뽑기</button>
        <button class="btn ghost" type="button" @click="goHome">다음에 할래요</button>
      </div>
    </template>

    <!-- 2) 미리보기 (PENDING) -->
    <template v-else-if="phase === 'preview'">
      <div class="card center">
        <p class="lead">굴비가 <b>{{ outfitKey }}</b>을(를) 입었어요!</p>

        <img class="hero" :src="images[previewMood]" :alt="`${outfitKey} 굴비`" />

        <div class="thumbs">
          <button
            v-for="(url, mood) in images"
            :key="mood"
            class="thumb"
            :class="{ active: mood === previewMood }"
            type="button"
            @click="previewMood = mood"
          >
            <img :src="url" :alt="MOOD_LABEL[mood] || mood" />
            <span>{{ MOOD_LABEL[mood] || mood }}</span>
          </button>
        </div>

        <div class="actions">
          <button class="btn primary" type="button" @click="onDecide('ACCEPT')">받기</button>
          <button class="btn line" type="button" @click="onDraw">다시 뽑기</button>
          <button class="btn ghost" type="button" @click="onDecide('DECLINE')">안 받을래요</button>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.reward-view { padding: 16px; }
.title { font-size: 20px; font-weight: 800; margin-bottom: 16px; }

.card {
  background: #fff;
  border-radius: 18px;
  padding: 24px 18px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.05);
}
.center { display: flex; flex-direction: column; align-items: center; gap: 12px; }

.lead { font-size: 16px; font-weight: 700; }
.muted { color: #9a8f7a; font-size: 13px; }

.hero {
  width: 200px; height: auto;
  background: #FBF5EA; border-radius: 16px; padding: 12px;
}

.thumbs { display: flex; flex-wrap: wrap; justify-content: center; gap: 8px; margin-top: 4px; }
.thumb {
  display: flex; flex-direction: column; align-items: center; gap: 2px;
  border: 2px solid transparent; border-radius: 12px; padding: 4px;
  background: #FBF5EA; cursor: pointer; font-size: 11px; color: #6B5320;
}
.thumb.active { border-color: #F2A33C; }
.thumb img { width: 48px; height: auto; }

.actions { display: flex; flex-direction: column; gap: 8px; width: 100%; margin-top: 8px; }

.btn {
  width: 100%; padding: 13px; border-radius: 12px;
  font-weight: 800; font-size: 15px; cursor: pointer; border: none;
}
.btn.primary { background: #F2A33C; color: #fff; }
.btn.line   { background: #fff; color: #F2A33C; border: 1.5px solid #F2A33C; }
.btn.ghost  { background: transparent; color: #9a8f7a; }

.spinner {
  width: 34px; height: 34px; border-radius: 50%;
  border: 4px solid #F2E2C4; border-top-color: #F2A33C;
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.error-message { color: #E8623D; font-weight: 700; margin-bottom: 10px; }
</style>