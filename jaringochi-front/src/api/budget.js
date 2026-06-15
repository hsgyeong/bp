import http from './http'

// 4-1. 이번 주 예산 + 지출현황 조회
export function getCurrentWeek()          { return http.get('/budgets/weekly/current') }
// 4-2. 최근 4주 목록 (통계 주 화면 공용)
export function getRecentWeeks()          { return http.get('/budgets/weekly/recent') }
// 4-3. 주간 예산 등록
export function createWeeklyBudget(body)  { return http.post('/budgets/weekly', body) }
// 4-4. 주간 예산 수정 (월 2회 제한)
export function updateWeeklyBudget(id, body) { return http.put(`/budgets/weekly/${id}`, body) }