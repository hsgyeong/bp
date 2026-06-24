// 디자인은 그림판(paint) 하나로 고정한다. (classic 테마 제거 — paint 영구 적용)
// 기존 컴포넌트의 `theme === 'paint'` 분기와 style.css의 [data-theme="paint"] 규칙이
// 항상 적용되도록, theme는 늘 'paint'이고 <html data-theme="paint">를 고정한다.
import { ref } from 'vue'

const theme = ref('paint')

// 항상 paint 규칙이 적용되도록 data-theme 고정 (새로고침 직후에도 즉시 반영)
document.documentElement.setAttribute('data-theme', 'paint')

export function useTheme() {
  // 테마 토글 제거됨. cycle은 기존 호출 호환용 no-op.
  function cycle() {}
  return { theme, cycle, THEMES: ['paint'] }
}
