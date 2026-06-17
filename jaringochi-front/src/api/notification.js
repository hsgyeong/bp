import http from './http'

// 5-1. 알림 목록 (isRead 생략 시 전체, 0=안읽음 / 1=읽음)
export function listNotifications(isRead) {
  return http.get('/notifications', { params: { isRead } })
}

// 5-2. 안읽은 개수 -> res.data.data.count
export function getUnreadCount() {
  return http.get('/notifications/unread-count')
}

// 5-3. 단건 읽음
export function markRead(id) {
  return http.patch(`/notifications/${id}/read`)
}

// 5-4. 전체 읽음
export function markAllRead() {
  return http.patch('/notifications/read-all')
}
