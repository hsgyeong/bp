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

// 회원정보 수정 API
export function updateMeApi(body) {
  return http.put('/users/me', body)
}

// 회원탈퇴 API
export function deleteMeApi() {
  return http.delete('/users/me')
}

// 닉네임 중복확인 API
export function checkNicknameApi(nickname) {
  return http.get('/auth/check-nickname', {
    params: { nickname },
  })
}