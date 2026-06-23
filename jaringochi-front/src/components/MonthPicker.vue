<script setup>
// 'YYYY-MM' 값을 고르는 커스텀 월 선택기.
// 트리거(라벨)는 부모가 slot 으로 넘기고, 누르면 연도 헤더 + 1~12월 그리드 팝업이 뜬다.
// paint 테마에선 .paint-box 가 손그림(wobble) 테두리를 그린다.
import { ref, computed, watch } from 'vue'

const props = defineProps({
  modelValue: { type: String, required: true }, // 'YYYY-MM'
  max: { type: String, default: '' },            // 'YYYY-MM' 이후(미래)는 선택 불가
  align: { type: String, default: 'left' },      // 팝업 정렬 'left' | 'right'
})
const emit = defineEmits(['update:modelValue'])

const open = ref(false)
const shownYear = ref(Number(props.modelValue.split('-')[0]))

// 열 때마다 선택값의 연도로 맞춘다.
watch(open, (o) => { if (o) shownYear.value = Number(props.modelValue.split('-')[0]) })

const selYear  = computed(() => Number(props.modelValue.split('-')[0]))
const selMonth = computed(() => Number(props.modelValue.split('-')[1]))

const ym = (y, m) => `${y}-${String(m).padStart(2, '0')}`
const isFuture = (m) => !!props.max && ym(shownYear.value, m) > props.max

function pick(m) {
  if (isFuture(m)) return
  emit('update:modelValue', ym(shownYear.value, m))
  open.value = false
}
</script>

<template>
  <span class="mp">
    <span class="mp-trigger" @click="open = !open"><slot /></span>

    <template v-if="open">
      <div class="mp-backdrop" @click="open = false"></div>
      <div class="mp-pop paint-box" :class="align === 'right' ? 'mp-right' : 'mp-left'">
        <div class="mp-head">
          <button type="button" class="mp-nav" @click="shownYear--">‹</button>
          <span>{{ shownYear }}년</span>
          <button type="button" class="mp-nav" @click="shownYear++">›</button>
        </div>
        <div class="mp-grid">
          <span v-for="m in 12" :key="m" class="mp-cell"
                :class="{ sel: shownYear === selYear && m === selMonth, future: isFuture(m) }"
                @click="pick(m)">{{ m }}월</span>
        </div>
      </div>
    </template>
  </span>
</template>

<style scoped>
.mp { position: relative; display: inline-flex; }
.mp-trigger { display: inline-flex; align-items: center; gap: 5px; cursor: pointer; }

.mp-backdrop { position: fixed; inset: 0; z-index: 15; }
.mp-pop {
  position: absolute; z-index: 20; top: calc(100% + 6px); width: 232px;
  background: var(--card); border: 1.5px solid var(--ink); border-radius: 14px; padding: 10px 12px;
}
.mp-pop.mp-left  { left: 0; }
.mp-pop.mp-right { right: 0; }
.mp-head { display: flex; align-items: center; justify-content: space-between; font-weight: 800; font-size: 14px; margin-bottom: 10px; }
.mp-nav { background: none; border: none; font-size: 18px; line-height: 1; cursor: pointer; padding: 2px 10px; color: var(--ink); font-family: inherit; }
.mp-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 6px; text-align: center; }
.mp-cell { padding: 9px 0; border-radius: 10px; font-size: 13px; font-weight: 700; color: var(--ink); cursor: pointer; }
.mp-cell.sel { background: var(--expense-soft); color: var(--expense); }
.mp-cell.future { opacity: .3; cursor: not-allowed; }
.mp-cell:not(.sel):not(.future):hover { background: var(--cream-2); }
</style>
