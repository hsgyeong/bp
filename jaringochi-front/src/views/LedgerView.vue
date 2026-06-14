<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { fetchTransactions } from '@/api/transaction'

const router = useRouter()

const selectedMonth = ref(getThisMonth()) // 현재 선택된 월
const viewMode = ref('list')              // 일자별/달력 탭 상태
const transactions = ref([])              // 거래 목록 데이터
const loading = ref(false)                // 목록 로딩/에러 상태
const errorMessage = ref('')              

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

// 합계: 수입 - 지출
const totalBalance = computed(() => totalIncome.value - totalExpense.value)

// 거래를 날짜별로 묶는다
const groupedTransactions = computed(() => {
  const groups = new Map()

  transactions.value.forEach((item) => {
    if (!groups.has(item.date)) {
      groups.set(item.date, {
        date: item.date,
        dayTotal: 0,
        items: [],
      })
    }

    const group = groups.get(item.date)
    const amount = Number(item.amount || 0)

    // 수입은 +, 지출은 -로 그날 합계에 반영
    group.dayTotal += item.type === 1 ? amount : -amount
    group.items.push(item)
  })

  return Array.from(groups.values()).sort((a, b) => {
    return b.date.localeCompare(a.date)
  })
})

const monthLabel = computed(() => selectedMonth.value.replace('-', '.'))

const calendarDays = computed(() => {
  const [year, month] = selectedMonth.value.split('-').map(Number)
  const firstDate = new Date(year, month - 1, 1)
  const lastDate = new Date(year, month, 0)

  const days = []

  for (let i = 0; i < firstDate.getDay(); i++) {
    days.push({ key: `empty-${i}`, empty: true })
  }

  for (let day = 1; day <= lastDate.getDate(); day++) {
    const date = `${selectedMonth.value}-${String(day).padStart(2, '0')}`
    const dayItems = transactions.value.filter((item) => item.date === date)

    const income = dayItems
      .filter((item) => item.type === 1)
      .reduce((sum, item) => sum + Number(item.amount || 0), 0)

    const expense = dayItems
      .filter((item) => item.type === 2)
      .reduce((sum, item) => sum + Number(item.amount || 0), 0)

    days.push({
      key: date,
      date,
      day,
      income,
      expense,
      items: dayItems,
    })
  }

  return days
})

function getThisMonth() {
  const now = new Date()
  const year = now.getFullYear()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  return `${year}-${month}`
}

function getMonthRange() {
  const [year, month] = selectedMonth.value.split('-').map(Number)
  const lastDay = new Date(year, month, 0).getDate()

  return {
    startDate: `${selectedMonth.value}-01`,
    endDate: `${selectedMonth.value}-${String(lastDay).padStart(2, '0')}`,
  }
}

// 숫자를 1,000,000 형식으로 표시
function won(value) {
  return Number(value || 0).toLocaleString('ko-KR')
}

// 금액 앞에 + 또는 - 를 붙여 표시
function signedWon(value) {
  const number = Number(value || 0)
  const sign = number > 0 ? '+' : number < 0 ? '-' : ''
  return `${sign}${won(Math.abs(number))}`
}

function getCategory(transaction) {
  return transaction.category || {
    name: transaction.type === 1 ? '수입' : '지출',
    icon: transaction.type === 1 ? '💰' : '🧾',
  }
}

// YYYY-MM-DD 문자열을 year/month/day로 나눈다.
function parseDate(dateText) {
  const [year, month, day] = dateText.split('-').map(Number)
  return { year, month, day }
}

function getDateLabel(dateText) {
  const { month, day } = parseDate(dateText)
  return `${month}월 ${day}일`
}

function getDayName(dateText) {
  const { year, month, day } = parseDate(dateText)
  const dayNames = ['일', '월', '화', '수', '목', '금', '토']
  return dayNames[new Date(year, month - 1, day).getDay()]
}

// 선택된 월의 거래 목록을 서버에서 가져온다.
async function loadTransactions() {
  loading.value = true
  errorMessage.value = ''

  try {
    const res = await fetchTransactions(getMonthRange())
    transactions.value = res.data.data || []
  } catch (err) {
    errorMessage.value =
      err.response?.data?.message || '거래 내역을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

// 캘린더 금액 표시: 2만/3천처럼 줄이지 않고 실제 금액을 그대로 보여준다.
function shortWon(value) {
  const number = Number(value || 0)
  return won(number)
}

function goCreate() {
  router.push({ name: 'transaction-new' })
}

function goEdit(id) {
  router.push({ name: 'transaction-edit', params: { id } })
}

// 화면이 처음 열릴 때 거래 목록을 불러온다.
onMounted(loadTransactions)

// 월이 바뀌면 해당 월 거래 목록을 다시 불러온다.
watch(selectedMonth, loadTransactions)
</script>

<template>
  <section class="ledger-view">
    <header class="topbar">
      <h1>가계부</h1>

      <label class="month-picker">
        <span>{{ monthLabel }}</span>
        <small>▾</small>
        <input v-model="selectedMonth" type="month" />
      </label>
    </header>

    <!-- 일자별/달력 전환 탭 -->
    <div class="mode-wrap">
      <div class="seg">
        <button type="button" :class="{ on: viewMode === 'list' }" @click="viewMode = 'list'">
          일자별
        </button>
        <button type="button" :class="{ on: viewMode === 'calendar' }" @click="viewMode = 'calendar'">
          달력
        </button>
      </div>
    </div>

    <!-- 월 요약 -->
    <section class="summary-card">
      <div>
        <span>수입</span>
        <strong class="income">{{ won(totalIncome) }}</strong>
      </div>
      <div>
        <span>지출</span>
        <strong class="expense">{{ won(totalExpense) }}</strong>
      </div>
      <div>
        <span>합계</span>
        <strong>{{ signedWon(totalBalance) }}</strong>
      </div>
    </section>

    <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>

    <div v-if="loading" class="empty">거래 내역을 불러오는 중입니다.</div>

    <div v-else-if="viewMode === 'calendar'" class="calendar-wrap">
      <div class="calendar-card">
        <div class="calendar-head">
          <span>일</span>
          <span>월</span>
          <span>화</span>
          <span>수</span>
          <span>목</span>
          <span>금</span>
          <span>토</span>
        </div>

        <div class="calendar-grid">
          <button v-for="day in calendarDays" :key="day.key" class="calendar-cell" :class="{ empty: day.empty }" type="button">
            <template v-if="!day.empty">
              <strong>{{ day.day }}</strong>
              <small v-if="day.income" class="income">+{{ shortWon(day.income) }}</small>
              <small v-if="day.expense" class="expense">-{{ shortWon(day.expense) }}</small>
            </template>
          </button>
        </div>
      </div>
    </div>

    <div v-else-if="groupedTransactions.length === 0" class="empty">
      <strong>거래 내역이 없어요</strong>
      <span>오른쪽 아래 + 버튼으로 거래를 등록해보세요.</span>
    </div>

       <!-- 날짜별 거래 목록 -->
    <div v-else class="day-list">
      <section v-for="group in groupedTransactions" :key="group.date" class="day-group">
        <div class="day-head">
          <span>
            {{ getDateLabel(group.date) }}
            <small>{{ getDayName(group.date) }}</small>
          </span>

          <strong :class="group.dayTotal >= 0 ? 'income' : 'expense'">
            {{ signedWon(group.dayTotal) }}
          </strong>
        </div>

        <button
          v-for="item in group.items"
          :key="item.id"
          class="txn"
          type="button"
          @click="goEdit(item.id)"
        >
          <span class="cat-ic" :class="item.type === 1 ? 'income-bg' : 'expense-bg'">
            {{ getCategory(item).icon }}
          </span>

          <span class="txn-text">
            <strong>{{ getCategory(item).name }}</strong>
            <small>{{ item.memo || '메모 없음' }}</small>
          </span>

          <strong class="amount" :class="item.type === 1 ? 'income' : 'expense'">
            {{ item.type === 1 ? '+' : '-' }}{{ won(item.amount) }}
          </strong>
        </button>
      </section>
    </div>

    <button class="fab" type="button" @click="goCreate">+</button>
  </section>
</template>

<style scoped>
.ledger-view {
  min-height: calc(100vh - 76px);
  padding: 34px 20px 112px;
  background: var(--cream);
  position: relative;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 28px;
}

.topbar h1 {
  font-size: 32px;
  font-weight: 900;
  color: var(--ink);
}

.month-picker {
  position: relative;
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--mute);
  font-size: 22px;
  font-weight: 900;
  cursor: pointer;
}

.month-picker small {
  font-size: 13px;
}

.month-picker input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

.mode-wrap {
  margin-bottom: 18px;
}

.seg {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px;
  padding: 5px;
  border-radius: 22px;
  background: var(--cream-2);
}

.seg button {
  height: 54px;
  border: 0;
  border-radius: 18px;
  background: transparent;
  color: var(--ink-2);
  font: inherit;
  font-size: 18px;
  font-weight: 900;
  cursor: pointer;
}

.seg button.on {
  background: #fff;
  color: var(--gold-deep);
  box-shadow: 0 4px 12px rgba(120, 90, 30, 0.08);
}

.summary-card {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  margin-bottom: 24px;
  padding: 20px 16px;
  border: 1px solid var(--line);
  border-radius: 28px;
  background: #fff;
  box-shadow: 0 4px 12px rgba(120, 90, 30, 0.04);
}

.summary-card div {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.summary-card span {
  color: var(--mute);
  font-size: 16px;
  font-weight: 900;
}

.summary-card strong {
  max-width: 100%;
  overflow-wrap: anywhere;
  color: var(--ink);
  font-size: 24px;
  font-weight: 900;
  line-height: 1;
}

.income {
  color: var(--income) !important;
}

.expense {
  color: var(--expense) !important;
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

.empty {
  min-height: 180px;
  display: grid;
  place-items: center;
  gap: 8px;
  padding: 24px;
  color: var(--mute);
  text-align: center;
  font-size: 14px;
  font-weight: 800;
}

.empty strong {
  color: var(--ink);
  font-size: 16px;
}

.day-list {
  display: flex;
  flex-direction: column;
}

.day-group + .day-group {
  margin-top: 14px;
}

.day-head {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 0 13px;
  border-bottom: 1px solid var(--line);
}

.day-head span {
  color: var(--ink);
  font-size: 22px;
  font-weight: 900;
}

.day-head small {
  margin-left: 4px;
  color: var(--mute);
  font-size: 17px;
  font-weight: 800;
}

.day-head strong {
  font-size: 21px;
  font-weight: 900;
}

.txn {
  width: 100%;
  display: grid;
  grid-template-columns: 54px minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  padding: 17px 0 22px;
  border: 0;
  background: transparent;
  color: inherit;
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.cat-ic {
  width: 54px;
  height: 54px;
  display: grid;
  place-items: center;
  border-radius: 18px;
  font-size: 24px;
}

.income-bg {
  background: var(--income-soft);
}

.expense-bg {
  background: var(--expense-soft);
}

.txn-text {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.txn-text strong,
.txn-text small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.txn-text strong {
  color: var(--ink);
  font-size: 23px;
  font-weight: 900;
  line-height: 1;
}

.txn-text small {
  color: var(--mute);
  font-size: 18px;
  font-weight: 700;
}

.amount {
  justify-self: end;
  white-space: nowrap;
  font-size: 24px;
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

@media (max-width: 380px) {
  .ledger-view {
    padding-left: 18px;
    padding-right: 18px;
  }

  .topbar h1 {
    font-size: 30px;
  }

  .month-picker {
    font-size: 20px;
  }

  .summary-card strong {
    font-size: 21px;
  }

  .txn {
    grid-template-columns: 50px minmax(0, 1fr) auto;
    gap: 12px;
  }

  .cat-ic {
    width: 50px;
    height: 50px;
  }

  .txn-text strong {
    font-size: 21px;
  }

  .txn-text small {
    font-size: 16px;
  }

  .amount {
    font-size: 21px;
  }
}

.calendar-wrap {
  padding-bottom: 70px;
}

.calendar-card {
  padding: 14px 12px;
  border: 1px solid var(--line);
  border-radius: 22px;
  background: #fff;
  box-shadow: 0 4px 12px rgba(120, 90, 30, 0.04);
}

.calendar-head,
.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
}

.calendar-head {
  padding-bottom: 8px;
  color: var(--ink-2);
  text-align: center;
  font-size: 12px;
  font-weight: 900;
}

.calendar-head span:first-child {
  color: var(--expense);
}

.calendar-head span:last-child {
  color: #5b8def;
}

.calendar-cell {
  min-height: 54px;
  border: 0;
  border-radius: 12px;
  background: transparent;
  color: var(--ink);
  font: inherit;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 5px 2px;
}

.calendar-cell.empty {
  pointer-events: none;
}

.calendar-cell strong {
  font-size: 12px;
  font-weight: 800;
}

.calendar-cell small {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 9px;
  font-weight: 900;
  line-height: 1;
}
</style>
