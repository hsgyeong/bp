import http from './http'

// 7-1. 월간 레포트 조회 (없으면 백엔드가 생성·저장 후 반환). year/month 필수
export function getMonthlyReport(year, month) {
  return http.get('/reports/monthly', { params: { year, month } })
}

// 7-2. 굴비에게 한 마디 (월 1회). 성공 시 user_message/gulbi_reply 채워진 레포트 반환
export function talkToGulbi(year, month, message) {
  return http.post('/reports/monthly/talk', { year, month, message })
}
