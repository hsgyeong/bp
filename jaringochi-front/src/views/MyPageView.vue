<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchMeApi, logoutApi, deleteMeApi } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)
const errorMessage = ref('')

const user = computed(() => authStore.user)

const nickname = computed(() => {
  return user.value?.nickname || '사용자'
})

const email = computed(() => {
  return user.value?.email || '-'
})

async function loadMe() {
  const token = localStorage.getItem('token')
  const refreshToken = localStorage.getItem('refreshToken')

  if (!token) return

  loading.value = true
  errorMessage.value = ''

  try {
    const res = await fetchMeApi()
    const me = res.data.data || res.data

    // 새로고침 후 user 정보가 없을 수 있으므로 다시 저장
    authStore.login(token, refreshToken, me)
  } catch (err) {
    errorMessage.value =
      err.response?.data?.message || '사용자 정보를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

// 로그아웃
async function logout() {

  // 로그인할 때 localStorage에 저장해둔 refreshToken을 꺼낸다.
  const refreshToken = localStorage.getItem('refreshToken')

  try {
    // refreshToken이 있으면 서버에 보내서 DB에서 폐기 처리
    if (refreshToken) {
      await logoutApi(refreshToken)
    }
  } catch {
    // 서버 로그아웃 실패와 관계없이 프론트 로그아웃은 진행
  } finally {
    // 프론트에 저장된 accessToken, refreshToken, user 정보 삭제
    authStore.logout()
    window.alert('로그아웃 되었습니다.')
    router.replace('/login')
  }
}

// 회원 탈퇴
async function withdraw() {
  const ok = window.confirm('정말 회원탈퇴 하시겠어요? 탈퇴 후에는 계정 정보를 되돌릴 수 없습니다.')

  if (!ok) return

  loading.value = true
  errorMessage.value = ''

  try {
    await deleteMeApi()

    authStore.logout()
    window.alert('회원탈퇴가 완료되었습니다.')
    router.replace('/login')
  } catch (err) {
    errorMessage.value =
      err.response?.data?.message || '회원탈퇴 중 문제가 발생했습니다.'
  } finally {
    loading.value = false
  }
}

function goEdit() {
  router.push({ name: 'profile-edit' })
}

onMounted(loadMe)
</script>

<template>
  <section class="mypage-view">
    <header class="page-head">
      <h1>마이페이지</h1>
    </header>

    <p v-if="errorMessage" class="error-message">
      {{ errorMessage }}
    </p>

    <section class="profile-card">        
      <div class="profile-icon" aria-hidden="true">
        🐟
      </div>

      <div class="profile-info">
        <strong>{{ nickname }}님</strong>
        <span>{{ email }}</span>
      </div>
    </section>

    <section class="card menu-card">
      <button class="menu-button action-menu" type="button" @click="goEdit">
        <span>회원정보 수정</span>
        <strong>›</strong>
      </button>

      <button class="menu-button" type="button" disabled>
        <span>닉네임</span>
        <strong>{{ nickname }}</strong>
      </button>

      <button class="menu-button" type="button" disabled>
        <span>이메일</span>
        <strong>{{ email }}</strong>
      </button>
    </section>

    <button class="logout-button" type="button" :disabled="loading" @click="logout">
      로그아웃
    </button>

    <div class="withdraw-row">
        <button class="withdraw-button" type="button" :disabled="loading" @click="withdraw">
        회원탈퇴
        </button>
    </div>
  </section>
</template>

<style scoped>
.mypage-view {
  min-height: calc(100vh - 76px);
  padding: 28px 20px 112px;
  background: var(--cream);
}

.page-head {
  margin-bottom: 22px;
}

.page-head h1 {
  color: var(--ink);
  font-size: 28px;
  font-weight: 900;
}

.error-message {
  margin-bottom: 14px;
  padding: 12px 14px;
  border-radius: 14px;
  background: var(--expense-soft);
  color: var(--expense);
  font-size: 13px;
  font-weight: 800;
}

.profile-card {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 18px;
  padding: 22px;
  border: 1px solid #f6dfb5;
  border-radius: 24px;
  background: #fff7e8;
}

.profile-icon {
  width: 62px;
  height: 62px;
  display: grid;
  place-items: center;
  border-radius: 20px;
  background: var(--gold-soft);
  font-size: 32px;
}

.profile-info {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.profile-info strong {
  color: var(--ink);
  font-size: 22px;
  font-weight: 900;
}

.profile-info span {
  color: var(--ink-2);
  font-size: 14px;
  font-weight: 700;
  overflow-wrap: anywhere;
}

.menu-card {
  display: flex;
  flex-direction: column;
  padding: 4px;
}

.menu-button {
  min-height: 58px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 0 14px;
  border: 0;
  border-bottom: 1px solid var(--line);
  background: transparent;
  color: inherit;
  font: inherit;
  text-align: left;
}

.menu-button:last-child {
  border-bottom: 0;
}

.menu-button span {
  color: var(--mute);
  font-size: 14px;
  font-weight: 800;
}

.menu-button strong {
  color: var(--ink);
  font-size: 15px;
  font-weight: 900;
  overflow-wrap: anywhere;
}

.logout-button {
  width: 100%;
  height: 54px;
  margin-top: 18px;
  border: 0;
  border-radius: 18px;
  background: var(--expense);
  color: #fff;
  font: inherit;
  font-size: 16px;
  font-weight: 900;
  cursor: pointer;
}

.logout-button:disabled {
  cursor: default;
  opacity: 0.55;
}

.withdraw-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}

.withdraw-button {
  border: 0;
  background: transparent;
  color: var(--mute);
  font: inherit;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.withdraw-button:disabled {
  cursor: default;
  opacity: 0.55;
}

.action-menu {
  cursor: pointer;
}

.action-menu strong {
  color: var(--gold-deep);
  font-size: 22px;
}
</style>
