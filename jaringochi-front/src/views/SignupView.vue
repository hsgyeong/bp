<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { signupApi } from '@/api/auth'

const router = useRouter()

const email = ref('')
const nickname = ref('')
const password = ref('')
const passwordConfirm = ref('')
const showPassword = ref(false)
const showPasswordConfirm = ref(false)

const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
let redirectTimer = null

const trimmedEmail = computed(() => email.value.trim())
const trimmedNickname = computed(() => nickname.value.trim())

const isEmailValid = computed(() => {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmedEmail.value)
})

const isPasswordValid = computed(() => {
  return /^(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,}$/.test(password.value)
})
const isPasswordMatched = computed(() => {
  return Boolean(passwordConfirm.value && password.value === passwordConfirm.value)
})

const passwordConfirmMessage = computed(() => {
  if (!passwordConfirm.value) return ''
  if (!password.value) return ''
  if (!isPasswordMatched.value) return '비밀번호가 서로 일치하지 않습니다.'
  return ''
})

const canSubmit = computed(() => {
  return Boolean(
    trimmedEmail.value &&
    trimmedNickname.value &&
    password.value &&
    passwordConfirm.value &&
    isPasswordMatched.value &&
    !loading.value &&
    !successMessage.value
  )
})

function validateForm() {
  if (!trimmedEmail.value) return '이메일을 입력해주세요.'
  if (!isEmailValid.value) return '이메일 형식이 올바르지 않습니다.'
  if (!trimmedNickname.value) return '닉네임을 입력해주세요.'
  if (!isPasswordValid.value) return '비밀번호는 8자 이상이며 영문, 숫자, 특수문자를 모두 포함해야 합니다.'
  if (!isPasswordMatched.value) return '비밀번호가 서로 일치하지 않습니다.'
  return ''
}

async function submitSignup() {
  if (!canSubmit.value) return

  const validationMessage = validateForm()
  if (validationMessage) {
    errorMessage.value = validationMessage
    return
  }

  loading.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    await signupApi({
      email: trimmedEmail.value,
      nickname: trimmedNickname.value,
      password: password.value,
    })

    successMessage.value = '가입이 완료되었습니다. 로그인 화면으로 이동합니다.'

    redirectTimer = window.setTimeout(() => {
      router.replace({ name: 'login' })
    }, 700)
  } catch (err) {
    const status = err.response?.status
    const serverMessage = err.response?.data?.message

    if (status === 409) {
      errorMessage.value = '이미 사용 중인 이메일입니다.'
    } else {
      errorMessage.value = serverMessage || '회원가입 중 문제가 발생했습니다.'
    }
  } finally {
    loading.value = false
  }
}

onBeforeUnmount(() => {
  if (redirectTimer) window.clearTimeout(redirectTimer)
})

</script>

<template>
  <section class="signup-page">
    <div class="signup-body">
      <header class="topbar">
        <RouterLink to="/login" class="back-link" aria-label="로그인 화면으로 이동">‹</RouterLink>
        <h1>회원가입</h1>
        <span class="topbar-spacer" aria-hidden="true"></span>
      </header>

      <div class="intro" aria-hidden="true">
        <svg class="gulbi" viewBox="0 0 120 120">
          <defs>
            <linearGradient id="signupGold" x1="0" y1="0" x2="1" y2="1">
              <stop offset="0" stop-color="#FFD37A" />
              <stop offset="1" stop-color="#E89020" />
            </linearGradient>
          </defs>
          <path d="M14 60 L2 42 Q-2 60 2 78 Z" fill="#E89020" />
          <ellipse cx="62" cy="60" rx="48" ry="30" fill="url(#signupGold)" stroke="#D77E15" stroke-width="2.5" />
          <path d="M40 36 Q44 60 40 84" stroke="#D77E15" stroke-width="2" fill="none" opacity=".5" />
          <path d="M58 33 Q62 60 58 87" stroke="#D77E15" stroke-width="2" fill="none" opacity=".4" />
          <path d="M62 30 Q72 18 84 30 Q72 34 62 36 Z" fill="#F2A33C" stroke="#D77E15" stroke-width="1.5" />
          <ellipse cx="78" cy="68" rx="7" ry="4.5" fill="#FF9D7A" opacity=".55" />
          <circle cx="86" cy="52" r="6.5" fill="#fff" stroke="#7A5418" stroke-width="1.5" />
          <circle cx="87.5" cy="53" r="3" fill="#3A2A10" />
          <circle cx="89" cy="51.5" r="1" fill="#fff" />
          <path d="M96 62 Q102 66 96 70" stroke="#7A5418" stroke-width="2" fill="none" stroke-linecap="round" />
        </svg>
        <p>굴비와 함께 절약을 시작해요</p>
      </div>

      <form class="signup-form" @submit.prevent="submitSignup">
        <label class="field-group">
          <span>이메일</span>
          <input v-model="email" type="email" autocomplete="email" placeholder="example@email.com" />
        </label>

        <label class="field-group">
          <span>닉네임</span>
          <input v-model="nickname" type="text" autocomplete="nickname" placeholder="알뜰이" />
        </label>

        <label class="field-group">
          <span>비밀번호</span>
          <div class="password-input-wrap">
            <input
              v-model="password"
              :type="showPassword ? 'text' : 'password'"
              autocomplete="new-password"
              placeholder="영문+숫자+특수문자 8자 이상"
            />
            <button
              class="password-toggle"
              type="button"
              :aria-label="showPassword ? '비밀번호 숨기기' : '비밀번호 보기'"
              :aria-pressed="showPassword"
              @click="showPassword = !showPassword"
            >
              <svg v-if="showPassword" viewBox="0 0 24 24" aria-hidden="true">
                <path d="M3 3l18 18" />
                <path d="M10.6 10.6a2 2 0 0 0 2.8 2.8" />
                <path d="M9.9 4.2A9.4 9.4 0 0 1 12 4c5 0 8.4 4.4 9.5 6a2.3 2.3 0 0 1 0 4 17.2 17.2 0 0 1-2.4 2.8" />
                <path d="M6.5 6.5A17.2 17.2 0 0 0 2.5 10a2.3 2.3 0 0 0 0 4c1.1 1.6 4.5 6 9.5 6a9.3 9.3 0 0 0 4.4-1.1" />
              </svg>
              <svg v-else viewBox="0 0 24 24" aria-hidden="true">
                <path d="M2.5 10a2.3 2.3 0 0 0 0 4c1.1 1.6 4.5 6 9.5 6s8.4-4.4 9.5-6a2.3 2.3 0 0 0 0-4c-1.1-1.6-4.5-6-9.5-6s-8.4 4.4-9.5 6Z" />
                <circle cx="12" cy="12" r="3" />
              </svg>
            </button>
          </div>
        </label>

        <label class="field-group">
          <span>비밀번호 확인</span>
          <div class="password-input-wrap">
            <input
              v-model="passwordConfirm"
              :type="showPasswordConfirm ? 'text' : 'password'"
              autocomplete="new-password"
              placeholder="비밀번호 재입력"
            />
            <button
              class="password-toggle"
              type="button"
              :aria-label="showPasswordConfirm ? '비밀번호 확인 숨기기' : '비밀번호 확인 보기'"
              :aria-pressed="showPasswordConfirm"
              @click="showPasswordConfirm = !showPasswordConfirm"
            >
              <svg v-if="showPasswordConfirm" viewBox="0 0 24 24" aria-hidden="true">
                <path d="M3 3l18 18" />
                <path d="M10.6 10.6a2 2 0 0 0 2.8 2.8" />
                <path d="M9.9 4.2A9.4 9.4 0 0 1 12 4c5 0 8.4 4.4 9.5 6a2.3 2.3 0 0 1 0 4 17.2 17.2 0 0 1-2.4 2.8" />
                <path d="M6.5 6.5A17.2 17.2 0 0 0 2.5 10a2.3 2.3 0 0 0 0 4c1.1 1.6 4.5 6 9.5 6a9.3 9.3 0 0 0 4.4-1.1" />
              </svg>
              <svg v-else viewBox="0 0 24 24" aria-hidden="true">
                <path d="M2.5 10a2.3 2.3 0 0 0 0 4c1.1 1.6 4.5 6 9.5 6s8.4-4.4 9.5-6a2.3 2.3 0 0 0 0-4c-1.1-1.6-4.5-6-9.5-6s-8.4 4.4-9.5 6Z" />
                <circle cx="12" cy="12" r="3" />
              </svg>
            </button>
          </div>
          <p v-if="passwordConfirmMessage" class="field-error">
            {{ passwordConfirmMessage }}
          </p>
        </label>

        <p v-if="errorMessage" class="message error-message">{{ errorMessage }}</p>
        <p v-if="successMessage" class="message success-message">{{ successMessage }}</p>

        <button class="signup-button" type="submit" :disabled="!canSubmit">
          {{ loading ? '가입 중...' : '가입하기' }}
        </button>
      </form>

      <p class="login-guide">
        이미 회원이신가요?
        <RouterLink to="/login">로그인</RouterLink>
      </p>
    </div>
  </section>
</template>

<style scoped>
.signup-page {
  min-height: calc(100vh - 76px);
  padding: 18px 20px 28px;
  display: flex;
}

.signup-body {
  width: 100%;
  display: flex;
  flex-direction: column;
}

.topbar {
  height: 42px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.topbar h1 {
  font-size: 20px;
  font-weight: 900;
  color: var(--ink);
}

.back-link,
.topbar-spacer {
  width: 34px;
  height: 34px;
}

.back-link {
  display: grid;
  place-items: center;
  border-radius: 50%;
  color: var(--ink-2);
  text-decoration: none;
  font-size: 30px;
  font-weight: 500;
  line-height: 1;
}

.intro {
  text-align: center;
  margin-bottom: 22px;
}

.gulbi {
  width: 78px;
  height: 78px;
  display: block;
  margin: 0 auto;
}

.intro p {
  margin-top: 6px;
  color: var(--mute);
  font-size: 14px;
  font-weight: 700;
}

.signup-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
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

.password-input-wrap {
  position: relative;
}

.password-input-wrap input {
  padding-right: 52px;
}

.password-toggle {
  position: absolute;
  top: 50%;
  right: 8px;
  transform: translateY(-50%);
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  border: 0;
  border-radius: 50%;
  background: transparent;
  color: var(--mute);
  cursor: pointer;
}

.password-toggle:focus-visible {
  outline: 3px solid rgba(242, 163, 60, 0.28);
  outline-offset: 2px;
}

.password-toggle svg {
  width: 21px;
  height: 21px;
  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.message {
  padding: 12px 14px;
  border-radius: 14px;
  font-size: 13px;
  font-weight: 800;
}

.error-message {
  background: var(--expense-soft);
  color: var(--expense);
}

.success-message {
  background: var(--income-soft);
  color: var(--income);
}

.signup-button {
  height: 54px;
  margin-top: 10px;
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

.signup-button:disabled {
  cursor: default;
  opacity: 0.55;
  box-shadow: none;
}

.login-guide {
  margin-top: 14px;
  text-align: center;
  color: var(--ink-2);
  font-size: 13px;
  font-weight: 700;
}

.login-guide a {
  color: var(--gold-deep);
  font-weight: 900;
  text-decoration: none;
}

.field-error {
  margin-top: -2px;
  color: var(--expense);
  font-size: 12px;
  font-weight: 700;
}

</style>

