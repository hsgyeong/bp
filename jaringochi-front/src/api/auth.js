import http from './http'

// 로그인 API
export function loginApi(body) {
    return http.post('/auth/login', body)
}

// 회원가입 API
export function signupApi(body) {
    return http.post('/auth/signup', body)
}

// 로그아웃 API
export function logoutApi() {
    return http.post('/auth/logout')
}

// 내 정보 조회 API
export function fetchMeApi() {
  return http.get('/users/me')
}