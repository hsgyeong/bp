<!--
  굴비 마스코트 — 상태(mood)에 따라 표정이 바뀌는 손그림 굴비.
  paint(그림판) 테마용 흑백 라인 일러스트. 이미지는 src/assets/gulbi/ 에서 잘라둔 7종.

  사용 예) <GulbiMascot mood="happy" :size="120" />
  mood: hello | warn | happy | sad | hungry | sulk | angry
-->
<script setup>
import hello from '@/assets/gulbi/gulbi-hello.png'
import warn from '@/assets/gulbi/gulbi-warn.png'
import happy from '@/assets/gulbi/gulbi-happy.png'
import sad from '@/assets/gulbi/gulbi-sad.png'
import hungry from '@/assets/gulbi/gulbi-hungry.png'
import sulk from '@/assets/gulbi/gulbi-sulk.png'
import angry from '@/assets/gulbi/gulbi-angry.png'

const MOODS = { hello, warn, happy, sad, hungry, sulk, angry }
const LABEL = {
  hello: '굴비가 인사해요',
  warn: '굴비가 걱정해요',
  happy: '굴비가 흐뭇해해요',
  sad: '굴비가 슬퍼해요',
  hungry: '굴비가 배고파해요',
  sulk: '굴비가 삐졌어요',
  angry: '굴비가 화났어요',
}

const props = defineProps({
  mood: { type: String, default: 'hello' },
  size: { type: [Number, String], default: 120 },
})

function src() {
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
