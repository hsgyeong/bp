import axios from 'axios'

// '나만의 axios' 인스턴스 - baseURL을 '/api'로 고정
const http = axios.create({
  baseURL: '/api',   // 모든 요청 앞에 자동으로 붙음. vite 프록시가 이걸 :8080으로 넘김
})

// -- 요청 인터셉터: 요청이 서버로 나가기 "직전"에 가로채는 자리 --
// http.interceptors.request.use((config) => {
//   const token = localStorage.getItem('token')
//   if (token) config.headers.Authorization = `Bearer ${token}`
//   return config   // 가로챈 설정을 그대로 통과시킴
// })

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  const isAuthApi = config.url?.startsWith('/auth/')

  if (token && !isAuthApi) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

 // 응답 인터셉터: 만료/가짜 토큰이면 로그인 정보 삭제 후 로그인 화면으로 이동
http.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status
    const isAuthApi = error.config?.url?.startsWith('/auth/')

    if (status === 401 && !isAuthApi) {
      localStorage.removeItem('token')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('user')

      const currentPath = window.location.pathname + window.location.search

      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = `/login?redirect=${encodeURIComponent(currentPath)}`
      }
    }
    return Promise.reject(error)
  }
)

export default http   // 다른 파일에서 import 해서 쓰도록 내보냄