<script setup>
import { computed, ref } from 'vue'                           // computed: 어떤 값이 바뀌면 자동으로 다시 계산되는 값 / ref: Vue가 화면 변화를 감지할 수 있는 반응형 함수
import { RouterLink, useRoute, useRouter } from 'vue-router'  // RouterLink: 새로고침 없이 다른 화면으로 이동하는 링크 컴포넌트 / useRoute: 현재 주소 정보 확인 / useRouter: 코드로 화면 이동할 때 사용
import { loginApi, fetchMeApi } from '@/api/auth'  // fetchMeApi: 로그인 성공 후 사용자 정보 조회
import { useAuthStore } from '@/stores/auth'      // 로그인 토큰과 유저 정보를 저장하는 Pinia store

const router = useRouter()          // 페이지 이동용
const route = useRoute()            // 현재 페이지 정보
const authStore = useAuthStore()    // 로그인 상태 저장소

const email = ref('')
const password = ref('')
const loading = ref(false)
const errorMessage = ref('')

const successMessage = computed(() => {
  return route.query.message || ''
})

const canSubmit = computed(() => {
  return email.value.trim() && password.value.trim() && !loading.value
})

async function submitLogin() {
  if (!canSubmit.value) return

  loading.value = true      // 요청 시작
  errorMessage.value = ''

  try {
    const res = await loginApi({    // 백엔드 로그인 API 호출 / vite.config.js의 proxy 덕분에 /api/auth/login은 localhost:8080으로 전달
      email: email.value.trim(),
      password: password.value,
    })

    const body = res.data       // axios 응답의 실제 JSON 본문은 res.data에 들어있음
    const token = body.accessToken || body.token    // 현재 백엔드는 accessToken이라는 이름으로 토큰을 준다.
    const refreshToken = body.refreshToken

    if (!token || !refreshToken) {
      throw new Error('로그인 응답에 토큰이 없습니다.')
    }

    localStorage.setItem('token', token)              // localStorage -> 브라우저 저장소. 새로고침 해도 token이 남아있게 저장
    localStorage.setItem('refreshToken', refreshToken) // 요청에 토큰이 붙도록 먼저 저장   
                                                                                                        
    const meRes = await fetchMeApi()                  // 홈 화면에서 사용자 정보를 사용하므로, 
    const user = meRes.data.data || meRes.data        // 내 정보 조회까지 성공해야 로그인 완료로 처리한다.

    authStore.login(token, refreshToken, user)        // Pinia store에서도 로그인 상태 저장

    const redirectPath = route.query.redirect || '/'  // 로그인 전에 가려던 페이지가 있으면 이동하고, 없으면 홈('/')으로 이동
    router.push(String(redirectPath))
  } catch (err) {
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('user')

    const status = err.response?.status
    const serverMessage = err.response?.data?.message   // ?. : optional chaining(옵셔널 체이닝): 있으면 들어가고, 없으면 멈추게 하는 안전장치 
                                                        //      서버가 꺼져 있거나 네트워크 문제면 response 자체가 없을 수 있기 때문
    if (status === 401) {
      errorMessage.value = '이메일 또는 비밀번호가 올바르지 않습니다.'
    } else {
      errorMessage.value = serverMessage || '로그인 중 문제가 발생했습니다.'
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="login-page">                        <!-- section: 화면의 한 구역-->
    <div class="login-body">
      <div class="mascot-wrap" aria-hidden="true">    <!-- aria-hidden: 화면의 읽기 도구가 읽지 않게 함 -->
        <div class="string"></div>

        <svg class="gulbi" viewBox="0 0 120 120">     <!-- svg: 벡터 그림을 그리는 태그 -->
          <defs>
            <linearGradient id="loginGold" x1="0" y1="0" x2="1" y2="1"> <!-- linearGradient: 그라데이션 색상 정의 -->
              <stop offset="0" stop-color="#FFD37A" />
              <stop offset="1" stop-color="#E89020" />
            </linearGradient>
          </defs>
          <!-- path, ellipse, circle: SVG 도형 -->
          <path d="M14 60 L2 42 Q-2 60 2 78 Z" fill="#E89020" />
          <ellipse
              cx="62"
              cy="60"
              rx="48"
              ry="30"
              fill="url(#loginGold)"
              stroke="#D77E15"
              stroke-width="2.5"
          />
          <path d="M40 36 Q44 60 40 84" stroke="#D77E15" stroke-width="2" fill="none" opacity=".5"/>
          <path d="M58 33 Q62 60 58 87" stroke="#D77E15" stroke-width="2" fill="none" opacity=".4" />
          <path d="M62 30 Q72 18 84 30 Q72 34 62 36 Z" fill="#F2A33C" stroke="#D77E15" stroke-width="1.5" />
          <ellipse cx="78" cy="68" rx="7" ry="4.5" fill="#FF9D7A" opacity=".55" />
          <circle cx="86" cy="52" r="6.5" fill="#fff" stroke="#7A5418" stroke-width="1.5" />
          <circle cx="87.5" cy="53" r="3" fill="#3A2A10" />
          <circle cx="89" cy="51.5" r="1" fill="#fff" />
          <path d="M96 62 Q102 66 96 70" stroke="#7A5418" stroke-width="2" fill="none" stroke-linecap="round" />
        </svg>
      </div>

      <div class="mascot-speech">
        <strong>주 단위로 지출을 관리하실 수 있게 도와드려요!</strong>
        <span>로그인하면 이번 주 예산과 지출 흐름을 바로 확인할 수 있어요.</span>
      </div>

      <header class="login-header">
        <h1>자린고비 가계부</h1>
      </header>

      <form class="login-form" @submit.prevent="submitLogin">  <!-- @submit.prevent: form 제출 시 새로고침을 막고 submitLogin 함수를 실행 -->
        <label class="field-group">
          <span>이메일</span>
          <input v-model="email" type="email" autocomplete="email" placeholder="이메일을 입력하세요." />    <!-- v-model: input 값과 email 변수를 양방향으로 연결 -->
        </label>
        <label class="field-group">
          <span>비밀번호</span>
          <input v-model="password" type="password" autocomplete="current-password" placeholder="비밀번호를 입력하세요." />    
        </label>

        <p v-if="successMessage" class="success-message">
          {{ successMessage }}
        </p>

        <p v-if="errorMessage" class="error-message">         <!-- v-if: 조건이 true일 때만 태그를 화면에 만든다. -->
          {{ errorMessage }}                                  <!-- {{ }}: Vue 변수를 화면에 출력하는 문법 -->
        </p>

        <button class="login-button" type="submit" :disabled="!canSubmit">   <!-- :disabled : disabled 속성에 JS 값을 연결 -->
          {{ loading ? '로그인 중...' : '로그인' }}   
        </button>
      </form>

      <p class="signup-guide">
        아직 회원이 아니신가요?
        <RouterLink to="/signup">회원가입</RouterLink>         <!-- RouterLink: Vue Router 전용 링크. 페이지 새로고침 없이 이동함 -->
      </p>
    </div>  
  </section>
</template>

<style scoped>
/* scoped: 이 스타일이 이 컴포넌트 안에서만 적용되게 함 */
.login-page {
  min-height: calc(100vh - 76px);
  padding: 28px 20px 96px;
  display: flex;
  align-items: flex-start;
}

.login-body {
  width: 100%;
  padding-top: 12px;
}

.mascot-wrap {
  position: relative;
  width: 104px;
  height: 104px;
  margin: 0 auto 10px;
}

.string {
  position: absolute;
  left: 50%;
  top: -34px;
  width: 2px;
  height: 44px;
  background: #cbb38a;
  transform: translateX(-50%);
}

.gulbi {
  width: 104px;
  height: 104px;
  display: block;
}

.mascot-speech {
  position: relative;
  margin: 0 auto 20px;
  padding: 15px 16px;
  border: 1px solid #f6dfb5;
  border-radius: 20px;
  background: #fff7e8;
  color: #6f4c12;
  text-align: center;
  box-shadow: 0 6px 16px rgba(120, 90, 30, 0.06);
}

.mascot-speech::before {
  content: "";
  position: absolute;
  top: -8px;
  left: 50%;
  width: 14px;
  height: 14px;
  border-left: 1px solid #f6dfb5;
  border-top: 1px solid #f6dfb5;
  background: #fff7e8;
  transform: translateX(-50%) rotate(45deg);
}

.mascot-speech strong {
  display: block;
  margin-bottom: 6px;
  font-size: 16px;
  font-weight: 900;
  line-height: 1.35;
  word-break: keep-all;
}

.mascot-speech span {
  display: block;
  color: #8a681f;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.45;
  word-break: keep-all;
}

.login-header {
  text-align: center;
  margin-bottom: 22px;
}

.login-header h1 {
  font-size: 26px;
  font-weight: 900;
  color: var(--ink);
  line-height: 1.15;
}

.login-header p {
  margin-top: 8px;
  color: var(--mute);
  font-size: 14px;
  font-weight: 700;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 13px;
}

.field-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-group span {
  font-size: 13px;
  font-weight: 800;
  color: var(--ink-2);
}

.field-group input {
  width: 100%;
  height: 52px;
  border: 1px solid var(--line);
  border-radius: 16px;
  padding: 0 16px;
  background: var(--card);
  color: var(--ink);
  font: inherit;
  font-size: 15px;
  outline: none;
}

.field-group input:focus {
  border-color: var(--gold);
  box-shadow: 0 0 0 3px rgba(242, 163, 60, 0.18);
}

.field-group input::placeholder {
  color: var(--mute);
}

.error-message {
  padding: 12px 14px;
  border-radius: 14px;
  background: var(--expense-soft);
  color: var(--expense);
  font-size: 13px;
  font-weight: 700;
}

.login-button {
  height: 54px;
  margin-top: 6px;
  border: 0;
  border-radius: 18px;
  background: var(--gold);
  color: #fff;
  font: inherit;
  font-size: 16px;
  font-weight: 900;
  cursor: pointer;
  box-shadow: var(--shadow);
}

.login-button:disabled {
  cursor: default;
  opacity: 0.55;
  box-shadow: none;
}

.signup-guide {
  margin-top: 16px;
  text-align: center;
  color: var(--ink-2);
  font-size: 13px;
  font-weight: 700;
}

.signup-guide a {
  color: var(--gold-deep);
  font-weight: 900;
  text-decoration: none;
}

@media (max-width: 380px) {
  .login-page {
    padding-left: 18px;
    padding-right: 18px;
  }

  .mascot-speech strong {
    font-size: 15px;
  }

  .mascot-speech span {
    font-size: 12px;
  }
}

.success-message {
  padding: 12px 14px;
  border-radius: 14px;
  background: var(--income-soft);
  color: var(--income);
  font-size: 13px;
  font-weight: 700;
}
</style>

