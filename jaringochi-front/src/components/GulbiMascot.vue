<!--
  굴비 마스코트 — 상태(mood)에 따라 표정이 바뀌는 손그림 굴비.
  paint(그림판) 테마용 흑백 라인 일러스트. 이미지는 src/assets/gulbi/ 의 5종.
  hello 는 로그인/회원가입 전용, 나머지 happy/smirk/angry/sad 4종을 화면에서 사용.

  사용 예) <GulbiMascot mood="happy" :size="120" />
  mood: hello | happy | smirk | angry | sad
-->
<script setup>
import hello from '@/assets/gulbi/gulbi-hello.png'
import happy from '@/assets/gulbi/gulbi-happy.png'
import sad   from '@/assets/gulbi/gulbi-sad.png'
import smirk from '@/assets/gulbi/gulbi-smirk.png'
import angry from '@/assets/gulbi/gulbi-angry.png'

const MOODS = { hello, happy, sad, smirk, angry }
const LABEL = {
  hello: '굴비가 인사해요',
  happy: '굴비가 흐뭇해해요',
  sad:   '굴비가 슬퍼해요',
  smirk: '굴비가 비웃어요',
  angry: '굴비가 화났어요',
}

const props = defineProps({
  mood: { type: String, default: 'hello' },
  size: { type: [Number, String], default: 120 },
  images: { type: Object, default: null }, 
})

function src() {
   // 옷 입은 이미지가 있으면 그걸, 없으면 기본 굴비 PNG
  if (props.images && props.images[props.mood]) return props.images[props.mood]
  return MOODS[props.mood] || MOODS.hello
}
function label() {
  return LABEL[props.mood] || LABEL.hello
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
