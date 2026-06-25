<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { fetchTransactions } from '@/api/transaction'
import { listCategories } from '@/api/category'
import { useTheme } from '@/composables/useTheme'
import { categoryTablerIcon } from '@/utils/categoryIcon'
import MonthPicker from '@/components/MonthPicker.vue'

const router = useRouter()
const { theme } = useTheme()   // paint 테마: 카테고리 이모지 → Tabler 라인 아이콘

const selectedMonth = ref(getThisMonth()) // 현재 선택된 월
// 일자별/달력 탭 상태. 수정 화면 등으로 라우트 이동 후 돌아와도(재마운트)
// 보던 탭이 유지되도록 sessionStorage에 기억해 둔다.
const viewMode = ref(sessionStorage.getItem('ledgerViewMode') || 'list')
const transactions = ref([])              // 거래 목록 데이터
const loading = ref(false)                // 목록 로딩/에러 상태
const errorMessage = ref('')              
const keyword = ref('')              // 검색어: 메모/카테고리명 검색에 사용
const transactionType = ref('all')    // 거래 유형 필터: 전체/수입/지출
const sort = ref('date_desc')        // 정렬 기준: 기본은 최신순

const categories = ref([])
const selectedCategory = ref('all')

// 유형(전체/수입/지출)에 맞는 카테고리만 드롭다운에 보여준다.
const visibleCategories = computed(() => {
  if (transactionType.value === 'all') return categories.value
  const t = Number(transactionType.value)
  return categories.value.filter((c) => c.type === t)
})

const selectedDay = ref(null)  // 달력에서 클릭한 날짜의 상세(모달). null이면 닫힘.

// 선택한 날짜의 순합계(수입 - 지출)
const selectedDayBalance = computed(() => {
  if (!selectedDay.value) return 0
  return selectedDay.value.income - selectedDay.value.expense
})

let searchTimer = null               // 검색어 입력 debounce용 타이머
let latestRequestId = 0              // 늦게 도착한 이전 응답을 무시하기 위한 요청 번호

// 금액순 정렬인지 확인한다.
// 금액순일 때는 날짜별 그룹으로 묶으면 서버 정렬 순서가 깨지므로 flat list로 보여준다.
const isAmountSort = computed(() => {
  return sort.value === 'amount_desc' || sort.value === 'amount_asc'
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

// 합계: 수입 - 지출
const totalBalance = computed(() => totalIncome.value - totalExpense.value)

// 거래를 날짜별로 묶는다
// 날짜 기준 정렬일 때만 이 값을 화면에 사용
// 금액순 정렬은 날짜 그룹으로 묶으면 전체 금액순이 깨지므로 별도 flat list로 보여준다.
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

  const grouped = Array.from(groups.values())

  // 날짜 오름차순 선택 시 오래된 날짜부터 보여준다.
  if (sort.value === 'date_asc') {
    return grouped.sort((a, b) => a.date.localeCompare(b.date))
  }

  // 기본은 최신 날짜부터 보여준다.
  return grouped.sort((a, b) => b.date.localeCompare(a.date))
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

// 선택된 월, 검색어, 정렬 조건을 서버로 보내 거래 목록을 가져온다.
// requestId를 사용해 이전 요청이 늦게 도착해도 최신 검색 결과를 덮어쓰지 못하게 막는다.
async function loadTransactions() {
  const requestId = ++latestRequestId

  loading.value = true
  errorMessage.value = ''

  try {
    const range = getMonthRange()

    const res = await fetchTransactions({
      ...range,
      keyword: keyword.value.trim() || undefined,
      type: transactionType.value === 'all' ? undefined : Number(transactionType.value),
      categoryId: selectedCategory.value === 'all' ? undefined : Number(selectedCategory.value),
      sort: sort.value,
    })

    // 이 응답보다 더 최신 요청이 이미 시작되었다면 이 응답은 버린다.
    if (requestId !== latestRequestId) return

    transactions.value = res.data.data || []
  } catch (err) {

     // 오래된 요청의 에러가 최신 화면 상태를 덮지 않게 막는다.
    if (requestId !== latestRequestId) return

    errorMessage.value =
      err.response?.data?.message || '거래 내역을 불러오지 못했습니다.'
  } finally {
    // 최신 요청일 때만 로딩 상태를 끈다.
    if (requestId === latestRequestId) {
      loading.value = false
    }
  }
}

async function loadCategories() {
  try {
    const [income, expense] = await Promise.all([
      listCategories(1),
      listCategories(2),
    ])
    categories.value = [
      ...(income.data.data || []),
      ...(expense.data.data || []),
    ]
  } catch (e) {
    categories.value = []
  }
}

// 캘린더 금액 표시: 2만/3천처럼 줄이지 않고 실제 금액을 그대로 보여준다.
function shortWon(value) {
  const number = Number(value || 0)
  return won(number)
}

// 달력 칸 클릭 → 그날 상세 모달 열기. 거래가 없는 날은 열지 않는다.
function openDay(day) {
  if (day.empty || day.items.length === 0) return
  selectedDay.value = day
}

function closeDay() {
  selectedDay.value = null
}

function goCreate() {
  router.push({ name: 'transaction-new' })
}

function goEdit(id) {
  router.push({ name: 'transaction-edit', params: { id } })
}

// Esc 키로 날짜 상세 모달 닫기
function onKeydown(e) {
  if (e.key === 'Escape' && selectedDay.value) closeDay()
}

// 화면이 처음 열릴 때 거래 목록을 불러온다.
onMounted(() => {
  loadCategories()
  loadTransactions()
  window.addEventListener('keydown', onKeydown)
})

// 월이 바뀌면 열려 있던 날짜 상세 모달을 닫는다.
watch(selectedMonth, closeDay)

// 보던 탭(일자별/달력)을 기억해 둔다 → 수정 후 돌아와도 같은 탭 유지.
watch(viewMode, (mode) => sessionStorage.setItem('ledgerViewMode', mode))

// 검색어는 글자마다 바로 요청하지 않고 300ms 쉬었다가 조회한다.
// 빠르게 입력할 때 불필요한 API 호출과 응답 순서 꼬임을 줄인다.
function scheduleSearch() {
  if (searchTimer) {
    clearTimeout(searchTimer)
  }

  searchTimer = setTimeout(() => {
    loadTransactions()
  }, 300)
}

// 유형을 바꾸면 카테고리 선택을 '전체'로 초기화 (다른 유형 항목이 남지 않도록).
// 아래 조회 watch보다 먼저 등록돼 유형 변경 시 조회는 한 번만 실행된다.
watch(transactionType, () => {
  selectedCategory.value = 'all'
})

// 월, 거래 유형, 정렬 기준은 선택 즉시 다시 조회한다.
watch([selectedMonth, transactionType, sort, selectedCategory], loadTransactions)

// 검색어는 debounce를 거쳐 조회한다.
watch(keyword, scheduleSearch)

// 화면을 떠날 때 남아 있는 검색 타이머를 정리한다.
onBeforeUnmount(() => {
  if (searchTimer) {
    clearTimeout(searchTimer)
  }
  window.removeEventListener('keydown', onKeydown)
})
</script>

<template>
  <section class="ledger-view">
    <header class="topbar">
      <MonthPicker v-model="selectedMonth" class="month-picker">
        <span>{{ monthLabel }}</span>
        <small>▾</small>
      </MonthPicker>

      <h1>가계부</h1>
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

    <!-- 검색/정렬 영역 -->
    <section class="filter-card" :class="{ 'calendar-filter': viewMode === 'calendar' }">
      <label class="search-field">
        <span>검색</span>
        <span class="paint-field">
          <input
            v-model="keyword"
            type="search"
            placeholder="메모 검색"
          />
        </span>
      </label>

      <label class="type-field">
        <span>유형</span>
        <span class="paint-field">
          <select v-model="transactionType">
            <option value="all">전체</option>
            <option value="1">수입</option>
            <option value="2">지출</option>
          </select>
        </span>
      </label>

      <label class="category-field">
        <span>카테고리</span>        
        <span class="paint-field">
          <select v-model="selectedCategory">
            <option value="all">전체</option>
            <option v-for="c in visibleCategories" :key="c.id" :value="c.id">{{ c.name }}</option>
          </select>
        </span>
      </label>

      <label v-if="viewMode !== 'calendar'" class="sort-field">
        <span>정렬</span>
        <span class="paint-field">
          <select v-model="sort">
            <option value="date_desc">최신순</option>
            <option value="date_asc">오래된순</option>
            <option value="amount_desc">금액 높은순</option>
            <option value="amount_asc">금액 낮은순</option>
          </select>
        </span>
      </label>
    </section>

    <div class="ledger-scroll">
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
            <button
              v-for="day in calendarDays"
              :key="day.key"
              class="calendar-cell"
              :class="{ empty: day.empty, 'has-items': !day.empty && day.items.length > 0 }"
              type="button"
              @click="openDay(day)"
            >
              <template v-if="!day.empty">
                <strong>{{ day.day }}</strong>
                <small v-if="day.income" class="income">+{{ shortWon(day.income) }}</small>
                <small v-if="day.expense" class="expense">-{{ shortWon(day.expense) }}</small>
              </template>
            </button>
          </div>
        </div>
      </div>

      <div v-else-if="transactions.length === 0" class="empty">
        <strong>거래 내역이 없어요</strong>
        <span>오른쪽 아래 + 버튼으로 거래를 등록해보세요.</span>
      </div>

      <!-- 금액순 정렬 목록 -->
      <div v-else-if="isAmountSort" class="flat-list">
        <button
          v-for="item in transactions"
          :key="item.id"
          class="txn"
          type="button"
          @click="goEdit(item.id)"
        >
          <span class="cat-ic" :class="item.type === 1 ? 'income-bg' : 'expense-bg'">
            <i
              v-if="theme === 'paint'"
              class="ti"
              :class="categoryTablerIcon(getCategory(item).name, item.type)"
              aria-hidden="true"
            ></i>
            <template v-else>{{ getCategory(item).icon }}</template>
          </span>

          <span class="txn-text">
            <strong>{{ getCategory(item).name }}</strong>
            <small>
              {{ item.memo || '메모 없음' }}
              ·
              {{ getDateLabel(item.date) }}
            </small>
          </span>

          <strong class="amount" :class="item.type === 1 ? 'income' : 'expense'">
            {{ item.type === 1 ? '+' : '-' }}{{ won(item.amount) }}
          </strong>
        </button>
      </div>

      <!-- 날짜별 거래 목록 -->
      <div v-else class="day-list">
        <section v-for="group in groupedTransactions" :key="group.date" class="day-group">
          <div class="day-head paint-hline-b">
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
              <i
                v-if="theme === 'paint'"
                class="ti"
                :class="categoryTablerIcon(getCategory(item).name, item.type)"
                aria-hidden="true"
              ></i>
              <template v-else>{{ getCategory(item).icon }}</template>
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
    </div>

    <button class="fab" type="button" @click="goCreate">+</button>

    <!-- 달력에서 날짜 클릭 시 그날 거래 상세 -->
    <div v-if="selectedDay" class="day-modal-backdrop" @click.self="closeDay">
      <div class="day-modal paint-box" role="dialog" aria-modal="true">
        <div class="day-modal-head">
          <span class="day-modal-date">
            {{ getDateLabel(selectedDay.date) }}
            <small>{{ getDayName(selectedDay.date) }}</small>
          </span>
          <button class="day-modal-close" type="button" @click="closeDay" aria-label="닫기">✕</button>
        </div>

        <div class="day-modal-sum paint-box">
          <div>
            <span>수입</span>
            <strong class="income">{{ won(selectedDay.income) }}</strong>
          </div>
          <div>
            <span>지출</span>
            <strong class="expense">{{ won(selectedDay.expense) }}</strong>
          </div>
          <div>
            <span>합계</span>
            <strong :class="selectedDayBalance >= 0 ? 'income' : 'expense'">
              {{ signedWon(selectedDayBalance) }}
            </strong>
          </div>
        </div>

        <div class="day-modal-list">
          <button
            v-for="item in selectedDay.items"
            :key="item.id"
            class="txn"
            type="button"
            @click="goEdit(item.id)"
          >
            <span class="cat-ic" :class="item.type === 1 ? 'income-bg' : 'expense-bg'">
              <i
                v-if="theme === 'paint'"
                class="ti"
                :class="categoryTablerIcon(getCategory(item).name, item.type)"
                aria-hidden="true"
              ></i>
              <template v-else>{{ getCategory(item).icon }}</template>
            </span>

            <span class="txn-text">
              <strong>{{ getCategory(item).name }}</strong>
              <small>{{ item.memo || '메모 없음' }}</small>
            </span>

            <strong class="amount" :class="item.type === 1 ? 'income' : 'expense'">
              {{ item.type === 1 ? '+' : '-' }}{{ won(item.amount) }}
            </strong>
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.ledger-view {
  height: calc(100vh - 76px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 34px 20px 0;
  background: var(--cream);
  position: relative;
}

.topbar {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 26px;
}

.topbar h1 {
  font-size: 30px;
  font-weight: 900;
  line-height: 1.2;
  color: var(--ink);
}

/* 월 선택: 홈 화면의 '2026년 6월'(작은 회색)과 동일한 톤·위치 */
.month-picker {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--mute);
  font-size: 20px;
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

.ledger-scroll {
  min-height: 0;
  flex: 1;
  overflow-y: auto;
  padding-bottom: 112px;
  scrollbar-width: none;
  -webkit-overflow-scrolling: touch;
}

.ledger-scroll::-webkit-scrollbar {
  display: none;
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

.calendar-cell {
  cursor: default;
}

.calendar-cell.has-items {
  cursor: pointer;
}

.calendar-cell.has-items:hover {
  background: var(--cream-2);
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

.filter-card {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 18px;
}

.filter-card.calendar-filter {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.search-field {
  grid-column: 1 / -1;
}

.type-field,
.sort-field {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.search-field span,
.type-field span,
.sort-field span {
  color: var(--ink-2);
  font-size: 12px;
  font-weight: 900;
}

.search-field input,
.type-field select,
.category-field select,
.sort-field select {
  width: 100%;
  height: 46px;
  border: 1px solid var(--line);
  border-radius: 14px;
  padding: 0 12px;
  background: #fff;
  color: var(--ink);
  font: inherit;
  font-size: 14px;
  font-weight: 800;
  outline: none;
}

.search-field input:focus,
.type-field select:focus,
.category-field select:focus,
.sort-field select:focus {
  border-color: var(--gold);
  box-shadow: 0 0 0 3px rgba(242, 163, 60, 0.18);
}

@media (max-width: 380px) {
  .filter-card {
    grid-template-columns: 1fr;
  }
}

/* ── paint(그림판) 테마 보정 ───────────────────────────────────────────── */
/* 카테고리 아이콘: 회색 배경 박스 제거하고 라인 아이콘만 (HomeView와 동일 컨벤션) */
:root[data-theme="paint"] .cat-ic {
  background: transparent !important;
  border-radius: 0;
  font-size: 30px;
}
/* 날짜 헤더 구분선은 .paint-hline-b 유틸이 손그림 선으로 그림 → 직선 border만 숨김 */
:root[data-theme="paint"] .day-head {
  border-bottom-color: transparent;
}
/* 거래 행(버튼)은 전역 규칙으로 손그림 박스가 됨 → 서로 안 겹치게 간격/패딩 부여 */
:root[data-theme="paint"] .txn {
  padding: 12px 14px;
  margin: 8px 0 12px;
  border-radius: 6px;
}
/* 검색/정렬 묶음은 '-card' 라 전역 손그림 테두리가 붙지만 카드가 아니므로 테두리 제거 */
:root[data-theme="paint"] .filter-card::before {
  display: none;
}
/* + 버튼: 흑백 반전(흰 배경·검은 +) + 크기 축소, 테두리는 전역 wobble */
:root[data-theme="paint"] .fab {
  width: 58px;
  height: 58px;
  background: var(--card) !important;
  color: var(--ink);
  font-size: 32px;
}
/* 캘린더 토요일 색(하드코딩 파랑) → 흑백 톤으로 */
:root[data-theme="paint"] .calendar-head span:last-child {
  color: var(--ink-2);
}
/* 일자별/달력 토글: 회색 프레임 제거(흰색), 선택 칸만 #e9e9e9+검정 글씨, 비선택은 흰색 */
:root[data-theme="paint"] .mode-wrap .seg {
  background: #fff;
}
:root[data-theme="paint"] .mode-wrap .seg button {
  background: #fff;
}
:root[data-theme="paint"] .mode-wrap .seg button.on {
  background: #e9e9e9;
  color: #000;
}

/* ── 날짜 상세 모달 ──────────────────────────────────────────────────── */
.day-modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: rgba(0, 0, 0, 0.42);
}

.day-modal {
  width: 100%;
  max-width: 440px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  background: var(--card);
  border-radius: 26px;
  padding: 20px 20px 28px;
  box-shadow: 0 18px 48px rgba(60, 45, 15, 0.28);
}

.day-modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.day-modal-date {
  color: var(--ink);
  font-size: 22px;
  font-weight: 900;
}

.day-modal-date small {
  margin-left: 6px;
  color: var(--mute);
  font-size: 16px;
  font-weight: 800;
}

.day-modal-close {
  width: 36px;
  height: 36px;
  border: 0;
  border-radius: 12px;
  background: var(--cream-2);
  color: var(--ink-2);
  font-size: 16px;
  font-weight: 900;
  cursor: pointer;
}

.day-modal-sum {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  padding: 16px 12px;
  margin-bottom: 8px;
  border: 1px solid var(--line);
  border-radius: 20px;
  background: var(--cream);
}

.day-modal-sum div {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.day-modal-sum span {
  color: var(--mute);
  font-size: 13px;
  font-weight: 900;
}

.day-modal-sum strong {
  max-width: 100%;
  overflow-wrap: anywhere;
  color: var(--ink);
  font-size: 18px;
  font-weight: 900;
  line-height: 1;
}

.day-modal-list {
  min-height: 0;
  flex: 1;
  overflow-y: auto;
  scrollbar-width: none;
}

.day-modal-list::-webkit-scrollbar {
  display: none;
}
</style>
