<!--
  굴비 마스코트 — 상태(mood)에 따라 표정이 바뀌는 손그림 굴비.
  흑백 라인 일러스트. 화면에서는 happy/smirk/angry/sad 4종만 사용
  (hello는 로그인/회원가입 전용이라 여기 없음).

  사용 예) <GulbiMascot mood="happy" :size="120" />
  mood: happy | smirk | angry | sad
-->
<script setup>
import happy from '@/assets/gulbi/gulbi-happy.png'
import sad   from '@/assets/gulbi/gulbi-sad.png'
import smirk from '@/assets/gulbi/gulbi-smirk.png'
import angry from '@/assets/gulbi/gulbi-angry.png'

const MOODS = { happy, smirk, angry, sad }
const LABEL = {
  happy: '굴비가 흐뭇해해요',
  smirk: '굴비가 비웃어요',
  angry: '굴비가 화났어요',
  sad:   '굴비가 슬퍼해요',
}

const props = defineProps({
  mood: { type: String, default: 'happy' },
  size: { type: [Number, String], default: 120 },
  images: { type: Object, default: null }, 
})

function src() {
   // 옷 입은 이미지가 있으면 그걸, 없으면 기본 굴비 PNG
  if (props.images && props.images[props.mood]) return props.images[props.mood]
  return MOODS[props.mood] || MOODS.happy
}
function label() {
  return LABEL[props.mood] || LABEL.happy
}
</script>

<template>
  <img
    class="gulbi-mascot"
    :src="src()"
    :alt="label()"
    :style="{ width: typeof size === 'number' ? size + 'px' : size }"
  />
</template>

<style scoped>
.gulbi-mascot {
  display: block;
  height: auto;
  user-select: none;
  -webkit-user-drag: none;
}
</style>
