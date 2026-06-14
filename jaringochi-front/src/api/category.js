import http from './http'

export function listCategories(type) { return http.get('/categories', { params: { type } }) }
export function createCategory(body)  { return http.post('/categories', body) }
export function updateCategory(id, body) { return http.put(`/categories/${id}`, body) }
export function deleteCategory(id)    { return http.delete(`/categories/${id}`) }