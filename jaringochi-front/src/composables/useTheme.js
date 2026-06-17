// 테마 전환 상태를 앱 전역에서 공유하는 composable.
// classic(기존 디자인) ↔ paint(그림판). 선택은 localStorage에 기억.
// <html data-theme="paint">만 토글하면 style.css의 paint 규칙이 통째로 적용된다.
import { ref, watch } from 'vue'

const THEMES = ['classic', 'paint']
const theme = ref(localStorage.getItem('jr-theme') || 'classic')

function apply(value) {
  const el = document.documentElement
  if (value === 'classic') el.removeAttribute('data-theme')
  else el.setAttribute('data-theme', value)
  localStorage.setItem('jr-theme', value)
}

apply(theme.value) // 새로고침 직후에도 즉시 반영
watch(theme, apply)

export function useTheme() {
  function cycle() {
    const i = THEMES.indexOf(theme.value)
    theme.value = THEMES[(i + 1) % THEMES.length]
  }
  return { theme, cycle, THEMES }
}
