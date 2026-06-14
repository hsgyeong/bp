<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchMeApi, updateMeApi, checkNicknameApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

// 화면 이동용 라우터
const router = useRouter()

// 로그인한 사용자 정보를 저장하는 Pinia store
const authStore = useAuthStore()

// 입력값
const nickname = ref('')
const password = ref('')
const passwordConfirm = ref('')

const originalNickname = ref('')
const nicknameChecked = ref(false)
const nicknameAvailable = ref(false)
const nicknameMessage = ref('')
const checkedNickname = ref('')

// 요청/메시지 상태
const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')


// 이메일은 수정하지 않고 보여주기만 한다.
const email = computed(() => {
  return authStore.user?.email || '-'
})

// 닉네임 변경 여부
const isNicknameChanged = computed(() => {
  return nickname.value.trim() !== originalNickname.value
})

// 닉네임 또는 비밀번호 중 하나라도 입력되어 있고,
// 요청 중이 아닐 때만 수정 버튼을 활성화한다.
const canSubmit = computed(() => {
  const hasNickname = nickname.value.trim()
  const hasPassword = password.value.trim() || passwordConfirm.value.trim()

  return !loading.value && (hasNickname || hasPassword)
})

// 화면이 처음 열릴 때 현재 사용자 정보를 다시 가져온다.
// 새로고침 후 store.user가 비어 있을 수 있기 때문이다.
async function loadMe() {
  const token = localStorage.getItem('token')

  if (!token) return

  nickname.value = me.nickname || ''
  originalNickname.value = me.nickname || ''

  loading.value = true
  errorMessage.value = ''

  try {
    const res = await fetchMeApi()
    const me = res.data.data || res.data

    // 최신 사용자 정보를 store와 localStorage에 다시 저장
    authStore.login(token, me)

    nickname.value = me.nickname || ''
  } catch (err) {
    errorMessage.value =
      err.response?.data?.message || '사용자 정보를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

// 닉네임 확인 상태 초기화
function resetNicknameCheck() {
  nicknameChecked.value = false
  nicknameAvailable.value = false
  nicknameMessage.value = ''
  checkedNickname.value = ''
}

// 닉네임 중복확인
async function checkNickname() {
  const nextNickname = nickname.value.trim()

  if (!nextNickname) {
    nicknameMessage.value = '닉네임을 입력해주세요.'
    nicknameChecked.value = false
    nicknameAvailable.value = false
    return
  }

  if (nextNickname === originalNickname.value) {
    nicknameChecked.value = true
    nicknameAvailable.value = true
    checkedNickname.value = nextNickname
    nicknameMessage.value = '현재 사용 중인 닉네임입니다.'
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    const res = await checkNicknameApi(nextNickname)
    const available = res.data.data ?? res.data

    nicknameChecked.value = true
    nicknameAvailable.value = Boolean(available)
    checkedNickname.value = nextNickname

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

// 회원정보 수정 제출
async function submitProfile() {
  if (!canSubmit.value) return

  errorMessage.value = ''
  successMessage.value = ''

  // 백엔드로 보낼 수정 데이터
  const body = {}

  // 닉네임이 입력되어 있으면 수정 요청에 포함
  if (isNicknameChanged.value) {
    if (!nicknameChecked.value || checkedNickname.value !== nickname.value.trim()) {
        errorMessage.value = '닉네임 중복확인을 해주세요.'
        return
    }

    if (!nicknameAvailable.value) {
        errorMessage.value = '이미 사용 중인 닉네임입니다.'
        return
    }

    body.nickname = nickname.value.trim()
    }

  // 비밀번호는 두 칸 중 하나라도 입력되었을 때만 변경 시도
  if (password.value || passwordConfirm.value) {
    if (password.value !== passwordConfirm.value) {
      errorMessage.value = '새 비밀번호가 일치하지 않습니다.'
      return
    }

    body.password = password.value
  }

  loading.value = true

  try {
    const res = await updateMeApi(body)
    const updatedUser = res.data.data || res.data

    const token = localStorage.getItem('token')

    // 수정된 사용자 정보를 store/localStorage에 반영
    authStore.login(token, updatedUser)

    // 비밀번호 입력값은 저장하지 않고 비운다.
    password.value = ''
    passwordConfirm.value = ''

    successMessage.value = '회원정보가 수정되었습니다.'
  } catch (err) {
    errorMessage.value =
      err.response?.data?.message || '회원정보 수정 중 문제가 발생했습니다.'
  } finally {
    loading.value = false
  }
}

// 이전 화면으로 돌아가기
function goBack() {
  router.back()
}

onMounted(loadMe)
</script>

<template>
  <section class="profile-edit-view">
    <header class="page-head">
      <button class="back-button" type="button" aria-label="뒤로가기" @click="goBack">
        ‹
      </button>
      <h1>회원정보 수정</h1>
    </header>

    <form class="edit-form" @submit.prevent="submitProfile">      
      <label class="field-group">
        <span>이메일</span>
        <input :value="email" type="email" disabled />
      </label>

      <label class="field-group">
        <span>닉네임</span>
        <div class="nickname-check-row">
            <input
                v-model="nickname"
                type="text"
                autocomplete="nickname"
                placeholder="닉네임을 입력하세요."
                @input="resetNicknameCheck"
            />

            <button
                class="check-button"
                type="button"
                :disabled="loading || !nickname.trim() || !isNicknameChanged"
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
        <span>새 비밀번호</span>
        <input
          v-model="password"
          type="password"
          autocomplete="new-password"
          placeholder="변경할 때만 입력하세요."
        />
      </label>

      <label class="field-group">
        <span>새 비밀번호 확인</span>
        <input
          v-model="passwordConfirm"
          type="password"
          autocomplete="new-password"
          placeholder="새 비밀번호를 다시 입력하세요."
        />
      </label>

      <p v-if="successMessage" class="success-message">
        {{ successMessage }}
      </p>

      <p v-if="errorMessage" class="error-message">
        {{ errorMessage }}
      </p>

      <button class="save-button" type="submit" :disabled="!canSubmit">
        {{ loading ? '수정 중...' : '수정 완료' }}
      </button>
    </form>
  </section>
</template>

<style scoped>
.profile-edit-view {
  min-height: calc(100vh - 76px);
  padding: 28px 20px 112px;
  background: var(--cream);
}

.page-head {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.back-button {
  width: 38px;
  height: 38px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: var(--card);
  color: var(--ink);
  font-size: 28px;
  line-height: 1;
  cursor: pointer;
}

.page-head h1 {
  color: var(--ink);
  font-size: 26px;
  font-weight: 900;
}

.edit-form {
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
  color: var(--ink-2);
  font-size: 13px;
  font-weight: 800;
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

.field-group input:disabled {
  background: var(--cream-2);
  color: var(--mute);
}

.field-group input:focus {
  border-color: var(--gold);
  box-shadow: 0 0 0 3px rgba(242, 163, 60, 0.18);
}

.success-message {
  padding: 12px 14px;
  border-radius: 14px;
  background: var(--income-soft);
  color: var(--income);
  font-size: 13px;
  font-weight: 700;
}

.error-message {
  padding: 12px 14px;
  border-radius: 14px;
  background: var(--expense-soft);
  color: var(--expense);
  font-size: 13px;
  font-weight: 700;
}

.save-button {
  height: 54px;
  margin-top: 8px;
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

.save-button:disabled {
  cursor: default;
  opacity: 0.55;
  box-shadow: none;
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
</style>