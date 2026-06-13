import http from './http'

// 거래 목록 조회 API
export function fetchTransactions(params) {
    return http.get('/transactions', { params })
}

// 거래 단건 조회 API
export function fetchTransaction(id) {
    return http.get(`/transactions/${id}`)
}

// 거래 등록 API
export function createTransaction(body) {
    return http.post('/transactions', body)
}

// 거래 수정 API
export function updateTransaction(id, body) {
    return http.put(`/transactions/${id}`, body)
}

// 거래 삭제 API
export function deleteTransaction(id) {
    return http.delete(`/transactions/${id}`)
}
