<script setup>
import { computed, onMounted, ref } from 'vue'  // computed: 값이 바뀌면 자동으로 다시 계산되는 값 / onMounted: 화면이 처음 열렸을 때 실행되는 함수 등록 / ref: 화면에 반영되는 반응형 변수
import { useRouter } from 'vue-router'          // 코드로 화면 이동할 때 사용
import { fetchTransactions } from '@/api/transaction'
import { getCurrentWeek, getRecentWeeks } from '@/api/budget'
import { useAuthStore } from '@/stores/auth'    // 로그인한 사용자 정보를 꺼내기 위한 Pinia store
import { useTheme } from '@/composables/useTheme'
import GulbiMascot from '@/components/GulbiMascot.vue'
import { categoryTablerIcon } from '@/utils/categoryIcon'
import { fetchMeApi } from '@/api/auth'

const router = useRouter()
const authStore = useAuthStore()
const { theme } = useTheme()                    // paint 테마일 때만 굴비 사진 마스코트 사용

const loading = ref(false)
const errorMessage = ref('')
const transactions = ref([])
const weeklyBudget = ref(null)
const recentWeeks = ref([])
const gulbiImages = ref(null)   // 현재 착용 옷 이미지 맵

// 끝난 주 중 "절약 성공 & 아직 안 뽑은" 가장 최근 주 1개
const pendingReward = computed(() => {
  const now = new Date()
  const todayStr =
    `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`

  return [...recentWeeks.value]
    .filter((w) => {
      const ended   = String(w.endDate) < todayStr
      const success = Number(w.spentMoney || 0) <= Number(w.amount || 0)
      const notDecided = w.rewardStatus == null || w.rewardStatus === 'PENDING' // ← null + PENDING
      return ended && success && notDecided
    })
    .sort((a, b) => String(b.endDate).localeCompare(String(a.endDate)))[0] || null
})

const hasWeeklyBudget = computed(() => {
  return weeklyBudget.value != null
})

// 표시할 닉네임 (기본값: '알뜰')
const nickname = computed(() => {
  return authStore.user?.nickname || ''
})

// 인사 문구
const greetingText = computed(() => {
  if (nickname.value) {
    return `안녕하세요, ${nickname.value}님`
  }

  return '안녕하세요'
})

// 현재 월을 "2026년 6월" 형식으로 표시
const monthLabel = computed(() => {           
  const [year, month] = getThisMonth().split('-')
  return `${year}년 ${Number(month)}월`
})

// 이번 달 수입 합계
const totalIncome = computed(() => {          
  return transactions.value
    .filter((item) => item.type === 1)
    .reduce((sum, item) => sum + Number(item.amount || 0), 0)
})

// 이번 달 지출 합계
const totalExpense = computed(() => {         
  return transactions.value
    .filter((item) => item.type === 2)
    .reduce((sum, item) => sum + Number(item.amount || 0), 0)
})

// 이번 주 예산 금액
const budgetAmount = computed(() => {         
  return Number(weeklyBudget.value?.amount || weeklyBudget.value?.currentBudget || 0)
})

// 이번 주 누적 지출 금액
const spentMoney = computed(() => {           
  return Number(weeklyBudget.value?.spentMoney || 0)
})

// 이번 주 예산 사용률
const budgetRate = computed(() => {

  if (weeklyBudget.value?.ratio != null) {              // 서버에서 ratio를 내려주면 그 값을 사용하고,
    return Math.round(Number(weeklyBudget.value.ratio))
  }

  if (!budgetAmount.value) return 0                     // 없으면 spentMoney / budgetAmount로 직접 계산한다.
  return Math.round((spentMoney.value / budgetAmount.value) * 100)
})

// progress bar의 채워진 너비
const progressWidth = computed(() => {
  return `${Math.min(budgetRate.value, 100)}%`
})

// 이번 주 남은 예산 (서버 remaining 우선, 없으면 예산-사용 직접 계산)
const remaining = computed(() => {
  const r = weeklyBudget.value?.remaining
  return r != null ? Number(r) : budgetAmount.value - spentMoney.value
})

// 이번 주 기간 (예: 2026-06-22 ~ 2026-06-28)
const weekRangeText = computed(() => {
  const b = weeklyBudget.value
  return b?.startDate && b?.endDate ? `${b.startDate} ~ ${b.endDate}` : ''
})

// 굴비 표정 단계에 맞춘 안내 문구 (요일 + 주간예산 1/7 로직 기준)
const mascotMessage = computed(() => {
  switch (mascotMood.value) {
    case 'happy': return '아주 좋아요. 이번 주 지출 흐름이 안정적이에요.'
    case 'smirk': return '살짝 빨라요. 오늘은 지갑을 조금만 닫아볼까요?'
    case 'angry': return '어쩌려고 그래요? 남은 날은 아껴봐요.'
    case 'sad':   return '제발 아껴요 이러다 텅장이 돼요...'
    default:      return ''
  }
})

// 요일 + 주간예산 대비 사용률 → 굴비 표정 (happy/smirk/angry/sad)
// 하루치 예산 = 주간예산/7. 요일 D(월=1..일=7)까지 D/7 사용은 happy,
// 거기서 1/7씩 초과할 때마다 smirk → angry → sad. (정확히 D/7이면 happy)
const mascotMood = computed(() => {
  const b = weeklyBudget.value
  if (!b || !Number(b.amount)) return 'happy'              // 예산 없으면 기본 happy
  const f = Number(b.spentMoney || 0) / Number(b.amount)   // 주간예산 대비 사용 비율(0~)
  const jsDay = new Date().getDay()                         // 일=0, 월=1 … 토=6
  const d = jsDay === 0 ? 7 : jsDay                         // 월=1 … 일=7

  if (f <= d / 7)       return 'happy'
  if (f <= (d + 1) / 7) return 'smirk'
  if (f <= (d + 2) / 7) return 'angry'
  return 'sad'
})

// 최근 거래 2개
// 거래 목록을 날짜 내림차순, id 내림차순으로 정렬한 뒤 앞에서 2개만 보여준다.
const recentTransactions = computed(() => {
  return [...transactions.value]
    .sort((a, b) => {
      const dateCompare = String(b.date).localeCompare(String(a.date))
      if (dateCompare !== 0) return dateCompare
      return Number(b.id || 0) - Number(a.id || 0)
    })
    .slice(0, 2)
})

// 현재 월 YYYY-MM 형식
function getThisMonth() {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  return `${year}-${month}`
}

// 이번 달 거래 조회에 사용할 startDate, endDate
function getMonthRange() {
  const selectedMonth = getThisMonth()
  const [year, month] = selectedMonth.split('-').map(Number)
  const lastDay = new Date(year, month, 0).getDate()

  return {
    startDate: `${selectedMonth}-01`,
    endDate: `${selectedMonth}-${String(lastDay).padStart(2, '0')}`,
  }
}

// 숫자를 1,000,000 형태로 표시
function won(value) {
  return Number(value || 0).toLocaleString('ko-KR')
}

function signedWon(transaction) {
  const sign = transaction.type === 1 ? '+' : '-'
  return `${sign}${won(transaction.amount)}`
}

function formatDate(dateText) {
  if (!dateText) return ''
  const [, month, day] = dateText.split('-').map(Number)
  return `${month}월 ${day}일`
}

function getCategory(transaction) {
  return transaction.category || {
    name: transaction.type === 1 ? '수입' : '지출',
    icon: transaction.type === 1 ? '💰' : '🧾',
  }
}

function goLedger() {
  router.push({ name: 'ledger' })
}

function goCreate() {
  router.push({ name: 'transaction-new' })
}

function goBudget() {
  router.push({ name: 'budget' })
}

function goReward() {
  if (!pendingReward.value) return
  router.push({ name: 'gulbi-reward', params: { weeklyBudgetId: pendingReward.value.id } })
}

async function loadHome() {
  loading.value = true
  errorMessage.value = ''

  // 이번 주 예산과 이번 달 거래 목록 동시에 요청
  const [budgetResult, transactionResult, recentResult, meResult] = await Promise.allSettled([
    getCurrentWeek(),
    fetchTransactions(getMonthRange()),
    getRecentWeeks(),
    fetchMeApi(),                                  
  ])

  if (budgetResult.status === 'fulfilled') {
    weeklyBudget.value = budgetResult.value.data.data || budgetResult.value.data
  } else {
    weeklyBudget.value = null
  }

  if (transactionResult.status === 'fulfilled') {
    transactions.value = transactionResult.value.data.data || []
  } else {
    transactions.value = []
    errorMessage.value =
      transactionResult.reason?.response?.data?.message || '홈 정보를 불러오지 못했습니다.'
  }

  if (recentResult.status === 'fulfilled') {
    recentWeeks.value = recentResult.value.data.data || recentResult.value.data || []
  } else {
    recentWeeks.value = []
  }

  if (meResult.status === 'fulfilled') {
    const me = meResult.value.data?.data ?? null
    gulbiImages.value = me?.currentGulbiImages || null
  } else {
    gulbiImages.value = null
  }

  loading.value = false
}

// onMounted(loadHome)
onMounted(() => {
  loadHome()
  fetchMeApi().then((res) => {
    const me = res.data?.data ?? null
    gulbiImages.value = me?.currentGulbiImages || null
    console.log(me)
  })
})
</script>

<template>
  <section class="home-view">
    <header class="home-head">
      <div>
        <p class="month">{{ monthLabel }}</p>
        <h1>{{ greetingText }} <span v-if="theme !== 'paint'">👋</span></h1>
      </div>
    </header>

    <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>

    <section class="mascot-card" :class="{ 'no-budget': !hasWeeklyBudget }">
      <!-- paint 테마: 표정 굴비 사진 / classic 테마: 기존 골드 SVG 굴비 -->
      <GulbiMascot v-if="theme === 'paint'" :mood="mascotMood" :size="120"  :images="gulbiImages"/>
      <svg v-else class="gulbi" viewBox="0 0 120 80" aria-hidden="true">
        <path d="M14 40 L2 24 Q-2 40 2 56 Z" fill="#E89020" />
        <ellipse cx="58" cy="40" rx="45" ry="25" fill="#F8B64C" stroke="#D98217" stroke-width="2.5" />
        <path d="M36 18 Q40 40 36 62" stroke="#D98217" stroke-width="2" fill="none" opacity=".45" />
        <path d="M55 16 Q59 40 55 64" stroke="#D98217" stroke-width="2" fill="none" opacity=".35" />
        <path d="M59 16 Q69 6 82 16 Q70 21 59 22 Z" fill="#F2A33C" stroke="#D98217" stroke-width="1.5" />
        <circle cx="79" cy="32" r="6" fill="#fff" stroke="#7A5418" stroke-width="1.5" />
        <circle cx="80.5" cy="33" r="2.7" fill="#3A2A10" />
        <path d="M90 43 Q96 46 90 50" stroke="#7A5418" stroke-width="2" fill="none" stroke-linecap="round" />
      </svg>

      <div v-if="hasWeeklyBudget">
        <strong>이번 주 예산의 {{ budgetRate }}% 썼어요.</strong>
        <p>{{ mascotMessage }}</p>
      </div>

      <div v-else class="budget-empty">
        <!-- 지난주 절약 성공 & 미뽑기 → 굴비 옷 뽑기 우선 노출 -->
        <template v-if="pendingReward">
          <strong class="budget-empty-title">지난주 예산을 지켜냈어요! 🎉</strong>
          <div class="budget-empty-action">
            <p>절약에 성공한 보상으로 굴비에게 새 옷을 선물할 수 있어요.</p>
            <button class="budget-link" type="button" @click="goReward">굴비 옷 뽑기</button>
          </div>
        </template>

        <!-- 보상이 없으면 기존 예산 설정 안내 -->
        <template v-else>
          <strong class="budget-empty-title">아직 이번주 예산을 설정하지 않았어요.</strong>
          <div class="budget-empty-action">
            <p>지금 바로 예산을 설정해볼까요?</p>
            <button class="budget-link" type="button" @click="goBudget">예산 설정하기</button>
          </div>
        </template>
      </div>
    </section>

    <section class="budget-card">
      <div class="card-head">
        <h2>이번 주 예산 사용률</h2>
        <strong>{{ budgetRate }}%</strong>
      </div>

      <!-- 예산 사용률 막대 -->
      <div class="progress">
        <span :style="{ width: progressWidth }"></span>
      </div>

      <div class="budget-row">
        <span>사용 <b style="color:var(--expense)">{{ won(spentMoney) }}원</b></span>
        <span>예산 <b style="color:var(--budget)">{{ won(budgetAmount) }}원</b></span>
      </div>
      <div class="budget-row budget-sub">
        <span>{{ weekRangeText }}</span>
        <span>남은 <b style="color:var(--ink)">{{ won(remaining) }}원</b></span>
      </div>
    </section>

    <section class="summary-grid">
      <article class="summary-card">
        <span>월 수입</span>
        <strong class="income">+{{ won(totalIncome) }}</strong>
      </article>

      <article class="summary-card">
        <span>월 지출</span>
        <strong class="expense">-{{ won(totalExpense) }}</strong>
      </article>
    </section>

    <section class="recent-card">
      <div class="recent-head">
        <h2>최근 거래</h2>
        <button type="button" @click="goLedger">더보기 ›</button>
      </div>

      <div v-if="loading" class="empty-state">홈 정보를 불러오는 중입니다.</div>

      <div v-else-if="recentTransactions.length === 0" class="empty-state">
        최근 거래가 없어요.
      </div>

      <button
        v-for="transaction in recentTransactions"
        v-else
        :key="transaction.id"
        class="recent-item"
        type="button"
        @click="router.push({ name: 'transaction-edit', params: { id: transaction.id } })"
      >
        <span class="category-icon" :class="transaction.type === 1 ? 'income-bg' : 'expense-bg'">
          <i
            v-if="theme === 'paint'"
            class="ti"
            :class="categoryTablerIcon(getCategory(transaction).name, transaction.type)"
            aria-hidden="true"
          ></i>
          <template v-else>{{ getCategory(transaction).icon }}</template>
        </span>

        <span class="recent-text">
          <span class="recent-title">
            <strong>{{ getCategory(transaction).name }}</strong>
            <span>· {{ transaction.memo || '메모 없음' }}</span>
          </span>
          <small>{{ formatDate(transaction.date) }}</small>
        </span>

        <strong class="recent-amount" :class="transaction.type === 1 ? 'income' : 'expense'">
          {{ signedWon(transaction) }}
        </strong>
      </button>
    </section>

    <button class="fab" type="button" aria-label="거래 등록" @click="goCreate">+</button>
  </section>
</template>

<style scoped>
.home-view {
  min-height: calc(100vh - 76px);
  padding: 34px 20px 112px;
  background: var(--cream);
  position: relative;
}

.home-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 26px;
}

.month {
  margin-bottom: 8px;
  color: var(--mute);
  font-size: 20px;
  font-weight: 900;
}

.home-head h1 {
  color: var(--ink);
  font-size: 30px;
  font-weight: 900;
  line-height: 1.2;
  word-break: keep-all;
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

.mascot-card {
  min-height: 112px;
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr);
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
  padding: 20px 22px;
  border: 1px solid #f6dfb5;
  border-radius: 28px;
  background: linear-gradient(180deg, #fff7e8, #feefcd);
}

.gulbi {
  width: 118px;
  height: 90px;
}

.mascot-card.no-budget {
  grid-template-columns: 110px minmax(0, 1fr);
}

.mascot-card strong {
  display: block;
  margin-bottom: 8px;
  color: #6f4c12;
  font-size: 19px;
  font-weight: 900;
  line-height: 1.35;
  word-break: keep-all;
}

.mascot-card p {
  color: #6f4c12;
  font-size: 17px;
  font-weight: 800;
  line-height: 1.35;
  word-break: keep-all;
}

.mascot-card .budget-empty-title {
  white-space: nowrap;
  font-size: 17px;
}

.budget-empty-action {
  display: inline-block;
}

.budget-empty-action p {
  white-space: normal;
  word-break: keep-all;
  line-height: 1.5;
}

.budget-link {
  display: block;
  width: 90%;
  margin-top: 14px;
  border: 0;
  border-radius: 14px;
  padding: 12px 16px;
  background: var(--gold-deep);
  color: #fff;
  font: inherit;
  font-size: 15px;
  font-weight: 900;
  cursor: pointer;
}

.budget-card,
.summary-card,
.recent-card {
  border: 1px solid var(--line);
  background: #fff;
  box-shadow: 0 4px 12px rgba(120, 90, 30, 0.04);
}

.budget-card {
  margin-bottom: 20px;
  padding: 24px 22px;
  border-radius: 28px;
}

.card-head,
.budget-row,
.recent-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.card-head {
  margin-bottom: 22px;
}

.card-head h2,
.recent-head h2 {
  color: var(--ink);
  font-size: 22px;
  font-weight: 900;
}

.card-head strong {
  color: var(--expense);
  font-size: 24px;
  font-weight: 900;
}

.progress {
  height: 20px;
  overflow: hidden;
  border-radius: 999px;
  background: var(--cream-2);
}

.progress span {
  height: 100%;
  display: block;
  border-radius: inherit;
  background: var(--expense);
}

.budget-row {
  margin-top: 16px;
  color: var(--mute);
  font-size: 16px;
  font-weight: 900;
}

/* 날짜·남은 줄: 한 단계 작게 */
.budget-row.budget-sub {
  margin-top: 8px;
  font-size: 13px;
  font-weight: 700;
}

.summary-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.summary-card {
  min-width: 0;
  min-height: 112px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 12px;
  padding: 22px;
  border-radius: 26px;
}

.summary-card span {
  color: var(--mute);
  font-size: 18px;
  font-weight: 900;
}

.summary-card strong {
  font-size: 28px;
  font-weight: 900;
  line-height: 1;
  overflow-wrap: anywhere;
}

.income {
  color: var(--income) !important;
}

.expense {
  color: var(--expense) !important;
}

.income-bg {
  background: var(--income-soft);
}

.expense-bg {
  background: var(--expense-soft);
}

.recent-card {
  margin-top: 20px;
  padding: 24px 22px;
  border-radius: 28px;
}

.recent-head {
  margin-bottom: 18px;
}

.recent-head button {
  border: 0;
  background: transparent;
  color: var(--mute);
  font: inherit;
  font-size: 16px;
  font-weight: 900;
  cursor: pointer;
}
/* paint 테마의 전역 버튼 손그림 테두리(::before) 제거 — '더보기'는 텍스트만 */
:root[data-theme="paint"] .recent-head button::before { content: none; }

.empty-state {
  min-height: 82px;
  display: grid;
  place-items: center;
  color: var(--mute);
  font-size: 14px;
  font-weight: 800;
}

.recent-item {
  width: 100%;
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  padding: 16px 0;
  border: 0;
  border-bottom: 1px solid var(--line);
  background: transparent;
  color: inherit;
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.recent-item:last-child {
  border-bottom: 0;
  padding-bottom: 4px;
}

.category-icon {
  width: 56px;
  height: 56px;
  display: grid;
  place-items: center;
  border-radius: 18px;
  font-size: 26px;
}

.recent-text {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.recent-title,
.recent-text small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recent-title {
  color: var(--ink);
  font-size: 18px;
  font-weight: 500;
}

.recent-title strong {
  font-weight: 900;
}

.recent-text small {
  color: var(--mute);
  font-size: 16px;
  font-weight: 800;
}

.recent-amount {
  justify-self: end;
  white-space: nowrap;
  font-size: 22px;
  font-weight: 900;
}

.fab {
  position: fixed;
  right: max(20px, calc((100vw - 480px) / 2 + 20px));
  bottom: 86px;
  width: 78px;
  height: 78px;
  border: 0;
  border-radius: 24px;
  background: linear-gradient(135deg, var(--gold), var(--gold-deep));
  color: #fff;
  font-size: 42px;
  font-weight: 300;
  line-height: 1;
  box-shadow: 0 10px 22px rgba(224, 135, 26, 0.45);
  cursor: pointer;
}

/* ── paint(그림판) 테마 보정 — 하드코딩된 골드/그라데이션을 흑백으로 ── */
:root[data-theme="paint"] .mascot-card {
  background: var(--card) !important;
  border-radius: 5px !important;
  /* 굴비가 왼쪽을 보고 있으니 텍스트 오른쪽(=카드 오른쪽)에 배치해 글을 바라보게 */
  grid-template-columns: minmax(0, 1fr) 120px;
}
:root[data-theme="paint"] .mascot-card > .gulbi-mascot { order: 2; justify-self: center; }
:root[data-theme="paint"] .mascot-card > div { order: 1; }
:root[data-theme="paint"] .mascot-card strong,
:root[data-theme="paint"] .mascot-card p {
  color: var(--ink);
}
/* 사용률 바: 각진 손그림 트랙(wobble) + 코랄 색연필 빗금 채움 (예산 페이지와 통일) */
:root[data-theme="paint"] .progress {
  background: #fff;
  border: 1.5px solid var(--ink);
  border-radius: 0;
  filter: url(#paintWobbleSmall);
}
:root[data-theme="paint"] .progress span {
  border-radius: 0;
  background-color: var(--expense);
  background-image: repeating-linear-gradient(45deg,
    rgba(0,0,0,.16) 0, rgba(0,0,0,.16) 1.4px, transparent 1.4px, transparent 6px);
}
/* 최근 거래 카테고리 아이콘: 회색 배경 박스 제거, 라인 아이콘만 */
:root[data-theme="paint"] .category-icon {
  background: transparent !important;
  border-radius: 0;
}
:root[data-theme="paint"] .category-icon .ti {
  font-size: 30px;
}
/* + 버튼: 흑백반전(흰 배경·검은 테두리·검은 +) + 크기 축소. 테두리는 전역 wobble */
:root[data-theme="paint"] .fab {
  width: 58px;
  height: 58px;
  background: var(--card) !important;
  color: var(--ink);
  font-size: 32px;
}
/* 최근 거래 항목 박스가 서로 겹치지 않게 간격 */
:root[data-theme="paint"] .recent-item {
  margin-bottom: 12px;
  padding: 12px 14px;
  border-radius: 6px;
}
:root[data-theme="paint"] .recent-item:last-child {
  margin-bottom: 0;
  padding-bottom: 12px;
}

@media (max-width: 380px) {
  .home-view {
    padding-left: 18px;
    padding-right: 18px;
  }

  .home-head h1 {
    font-size: 27px;
  }

  .mascot-card {
    grid-template-columns: 104px minmax(0, 1fr);
    padding: 18px;
  }

  .mascot-card.no-budget {
    display: block;
  }

  .gulbi {
    width: 100px;
    height: auto;
  }

  .mascot-card.no-budget .gulbi {
    margin-bottom: 12px;
  }

  .mascot-card strong {
    font-size: 17px;
  }

  .mascot-card p {
    font-size: 15px;
  }

  .summary-card {
    padding: 18px;
  }

  .summary-card strong {
    font-size: 24px;
  }

  .recent-item {
    grid-template-columns: 50px minmax(0, 1fr) auto;
    gap: 12px;
  }

  .category-icon {
    width: 50px;
    height: 50px;
  }

  .recent-amount {
    font-size: 19px;
  }
}
</style>
