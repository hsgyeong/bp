import http from './http'

// 6-1. 카테고리별 통계 (상위 4 + 기타). type: 1=수입 / 2=지출 (옵션)
// 월/주 공용 - 호출부에서 그 달/주의 startDate~endDate 전달
export function getByCategory(startDate, endDate, type) {
  return http.get('/statistics/by-category', { params: { startDate, endDate, type } })
}

// 6-2. 월별 추이 (최근 months개월) + 전월대비. type 필수
export function getMonthlyTrend(type, months = 6) {
  return http.get('/statistics/monthly-trend', { params: { type, months } })
}
