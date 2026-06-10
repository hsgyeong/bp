import axios from 'axios'

// '나만의 axios' 인스턴스 - baseURL을 '/api'로 고정
const http = axios.create({
  baseURL: '/api',   // 모든 요청 앞에 자동으로 붙음. vite 프록시가 이걸 :8080으로 넘김
})

// -- 요청 인터셉터: 요청이 서버로 나가기 "직전"에 가로채는 자리 --
http.interceptors.request.use((config) => {
  // * (JWT 강의 후) 여기서 토큰을 붙일 예정. 지금은 인증 전이라 비워둠.
  // const token = localStorage.getItem('token')
  // if (token) config.headers.Authorization = `Bearer ${token}`
  return config   // 가로챈 설정을 그대로 통과시킴
})

export default http   // 다른 파일에서 import 해서 쓰도록 내보냄