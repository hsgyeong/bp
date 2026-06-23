<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { signupApi, checkNicknameApi } from '@/api/auth'
import { useTheme } from '@/composables/useTheme'  // paint 테마: 컬러 SVG 굴비 → 흑백 GulbiMascot
import GulbiMascot from '@/components/GulbiMascot.vue'

const { theme } = useTheme()
const router = useRouter()

const email = ref('')
const nickname = ref('')
const password = ref('')
const passwordConfirm = ref('')

// 닉네임 확인
const nicknameChecked = ref(false)
const nicknameAvailable = ref(false)
const nicknameMessage = ref('')
const checkedNickname = ref('')

// 비밀번호 보기/숨기기 상태
const showPassword = ref(false)
const showPasswordConfirm = ref(false)

const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

// 회원가입 성공 후 로그인 페이지로 이동하는 타이머 id
let redirectTimer = null

const trimmedEmail = computed(() => email.value.trim())
const trimmedNickname = computed(() => nickname.value.trim())

// 이메일 형식 검사
const isEmailValid = computed(() => {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmedEmail.value)
})

function resetNicknameCheck() {
  nicknameChecked.value = false
  nicknameAvailable.value = false
  nicknameMessage.value = ''
  checkedNickname.value = ''
}

// 닉네임 중복확인 함수
async function checkNickname() {
  if (!trimmedNickname.value) {
    nicknameMessage.value = '닉네임을 입력해주세요.'
    nicknameChecked.value = false
    nicknameAvailable.value = false
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    const res = await checkNicknameApi(trimmedNickname.value)
    const available = res.data.data ?? res.data

    nicknameChecked.value = true
    nicknameAvailable.value = Boolean(available)
    checkedNickname.value = trimmedNickname.value

    nicknameMessage.value = available
      ? '사용 가능한 닉네임입니다.'
      : '이미 사용 중인 닉네임입니다.'
  } catch (err) {
    nicknameChecked.value = false
    nicknameAvailable.value = false
    nicknameMessage.value =
      err.response?.data?.message || '닉네임 중복확인 중 문제가 발생했습니다.'
  } finally {
    loading.value = false
  }
}

// 비밀번호 조건 검사: 8자 이상 + 영문 + 숫자 + 특수문자
const isPasswordValid = computed(() => {
  return /^(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,}$/.test(password.value)
})

// 비밀번호와 비밀번호 확인 값이 일치하는지 확인
const isPasswordMatched = computed(() => {
  return Boolean(passwordConfirm.value && password.value === passwordConfirm.value)
})

// 비밀번호 확인 입력 중 불일치 메시지 표시
const passwordConfirmMessage = computed(() => {
  if (!passwordConfirm.value) return ''
  if (!password.value) return ''
  if (!isPasswordMatched.value) return '비밀번호가 서로 일치하지 않습니다.'
  return ''
})

// 가입 버튼 활성화 조건
const canSubmit = computed(() => {
  return Boolean(
    trimmedEmail.value &&
    trimmedNickname.value &&
    nicknameChecked.value &&
    nicknameAvailable.value &&
    checkedNickname.value === trimmedNickname.value &&
    password.value &&
    passwordConfirm.value &&
    isPasswordMatched.value &&
    !loading.value &&
    !successMessage.value
  )
})

// 최종 제출 전 입력값 검증
function validateForm() {
  if (!trimmedEmail.value) return '이메일을 입력해주세요.'
  if (!isEmailValid.value) return '이메일 형식이 올바르지 않습니다.'
  if (!trimmedNickname.value) return '닉네임을 입력해주세요.'
  if (!isPasswordValid.value) return '비밀번호는 8자 이상이며 영문, 숫자, 특수문자를 모두 포함해야 합니다.'
  if (!isPasswordMatched.value) return '비밀번호가 서로 일치하지 않습니다.'
  
  if (!nicknameChecked.value || checkedNickname.value !== trimmedNickname.value) {
    return '닉네임 중복확인을 해주세요.'
  }
  if (!nicknameAvailable.value) {
    return '이미 사용 중인 닉네임입니다.'
  }
  return ''
}

// 회원가입 제출 처리
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
    // 백엔드 회원가입 API 호출
    await signupApi({
      email: trimmedEmail.value,
      nickname: trimmedNickname.value,
      password: password.value,
    })

    successMessage.value = '가입이 완료되었습니다. 로그인 화면으로 이동합니다.'

    // 가입 성공 후 로그인 화면으로 이동
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

// 컴포넌트가 사라질 때 예약된 타이머 정리
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
        <GulbiMascot v-if="theme === 'paint'" mood="hello" :size="104" class="gulbi" />
        <svg v-else class="gulbi" viewBox="0 0 120 120">
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

      <!-- 회원가입 입력 폼 -->
      <form class="signup-form" @submit.prevent="submitSignup">
        <label class="field-group">
          <span>이메일</span>
          <span class="paint-field">
            <input v-model="email" type="email" autocomplete="email" placeholder="이메일을 입력해주세요." />
          </span>
        </label>

        <label class="field-group">
          <span>닉네임</span>
          <div class="nickname-check-row">
            <span class="paint-field">
              <input
                  v-model="nickname"
                  type="text"
                  autocomplete="nickname"
                  placeholder="닉네임을 입력해주세요."
                  @input="resetNicknameCheck"
              />
            </span>

            <button
                class="check-button"
                type="button"
                :disabled="loading || !trimmedNickname"
                @click="checkNickname"
            >
            중복확인
            </button>
          </div>

          <p
            v-if="nicknameMessage"
            class="field-message"
            :class="nicknameAvailable ? 'available' : 'unavailable'"
          >
          {{ nicknameMessage }}
          </p>
        </label>

        <label class="field-group">
          <span>비밀번호</span>
          <div class="password-input-wrap">
            <span class="paint-field">
              <input
                v-model="password"
                :type="showPassword ? 'text' : 'password'"
                autocomplete="new-password"
                placeholder="영문+숫자+특수문자 8자 이상"
              />
            </span>

            <!-- 비밀번호 보기/숨기기 토글 -->
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
            <span class="paint-field">
              <input
                v-model="passwordConfirm"
                :type="showPasswordConfirm ? 'text' : 'password'"
                autocomplete="new-password"
                placeholder="비밀번호를 다시 입력해주세요."
              />
            </span>

            <!-- 비밀번호 확인 보기/숨기기 토글 -->
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

        <!-- API 실패 또는 검증 실패 메시지 -->
        <p v-if="errorMessage" class="message error-message">{{ errorMessage }}</p>

        <!-- 회원가입 성공 메시지 -->
        <p v-if="successMessage" class="message success-message">{{ successMessage }}</p>

        <!-- 회원가입 제출 버튼 -->
        <button class="signup-button" type="submit" :disabled="!canSubmit">
          {{ loading ? '가입 중...' : '가입하기' }}
        </button>
      </form>

      <!-- 로그인 화면 이동 링크 -->
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
  width: 104px;
  height: 51.45px;
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

.nickname-check-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 92px;
  gap: 8px;
}

.check-button {
  height: 52px;
  border: 0;
  border-radius: 16px;
  background: var(--gold-soft);
  color: var(--gold-deep);
  font: inherit;
  font-size: 13px;
  font-weight: 900;
  cursor: pointer;
}

.check-button:disabled {
  cursor: default;
  opacity: 0.55;
}

.field-message {
  margin-top: -2px;
  font-size: 12px;
  font-weight: 700;
}

.field-message.available {
  color: var(--income);
}

.field-message.unavailable {
  color: var(--expense);
}

/* ── paint(그림판) 테마 보정 ── */
/* 비밀번호 보기 토글은 아이콘 버튼이라 전역 button wobble 박스 제외 */
:root[data-theme="paint"] .password-toggle::before { display: none; }
</style>

