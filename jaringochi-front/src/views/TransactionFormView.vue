<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listCategories } from '@/api/category'
import { createTransaction, fetchTransaction, updateTransaction, deleteTransaction } from '@/api/transaction';

const route = useRoute()
const router = useRouter()

// /ledger/:id/edit 로 들어오면 수정, /ledger/new 로 들어오면 등록
const transactionId = computed(() => route.params.id)
const isEditMode = computed(() => Boolean(transactionId.value))

// type: 1 = 수입, 2 = 지출
const type = ref(2)

const amount = ref('')
const categoryId = ref(null)
const date = ref(getToday())
const memo = ref('')

const loading = ref(false)
const saving = ref(false)
const deleting = ref(false)
const categories = ref([])
const errorMessage = ref('')

// 저장 버튼 활성화 조건 
const canSave = computed(() => {
    return (
        Number(amountValue.value) > 0 &&
        categoryId.value &&
        date.value &&
        !saving.value &&
        !deleting.value
    )
})

// 콤마가 들어간 금액 문자열도 숫자로 바꿀 수 있게 처리
const amountValue = computed(() => {
    return Number(String(amount.value).replaceAll(',', ''))
})

const amountPreview = computed(() => {
    const value = amountValue.value
    if (!value) return '0'
    return value.toLocaleString('ko-KR')
})

function getToday() {
    const now = new Date()
    const year = now.getFullYear()
    const month = String(now.getMonth() + 1).padStart(2, '0')
    const day = String(now.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
}

function formatDateLabel(value) {
    if (!value) return '날짜를 선택하세요'

    const [year, month, day] = value.split('-').map(Number)
    const dayNames = ['일', '월', '화', '수', '목', '금', '토']
    const dayName = dayNames[new Date(year, month - 1, day).getDay()]

    return `${year}년 ${month}월 ${day}일 (${dayName})`
}

// 금액 입력 시 숫자와 콤마만 남긴다.
function onAmountInput(event) {
    const onlyNumber = event.target.value.replace(/[^0-9]/g, '')
    amount.value = onlyNumber ? Number(onlyNumber).toLocaleString('ko-KR') : ''
}

// 수입/지출이 바뀔 때 해당 타입의 카테고리 목록을 다시 불러온다.
async function loadCategories() {
  try {
    const res = await listCategories(type.value)
    categories.value = res.data.data || []

    errorMessage.value = ''

    const exists = categories.value.some((category) => category.id === categoryId.value)
    if (!exists) {
      categoryId.value = categories.value[0]?.id || null
    }
  } catch (err) {
    categories.value = []
    categoryId.value = null
    errorMessage.value = err.response?.data?.message || '분류 목록을 불러오지 못했습니다.'
  }
}

// 수정 모드일 때 기존 거래 정보를 폼에 채운다.
async function loadTransaction() {
  if (!isEditMode.value) return

  loading.value = true
  errorMessage.value = ''

  try {
    const res = await fetchTransaction(transactionId.value)
    const transaction = res.data.data

    type.value = transaction.type
    amount.value = Number(transaction.amount || 0).toLocaleString('ko-KR')
    categoryId.value = transaction.categoryId
    date.value = transaction.date
    memo.value = transaction.memo || ''

    await loadCategories()
  } catch (err) {
    errorMessage.value = err.response?.data?.message || '거래 정보를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

function selectType(nextType) {
  if (type.value === nextType) return
  type.value = nextType
}

function buildRequestBody() {
  return {
    categoryId: categoryId.value,
    amount: amountValue.value,
    type: type.value,
    memo: memo.value.trim() || null,
    date: date.value,
  }
}

// 거래내역 등록 / 수정
async function saveTransaction() {
  if (!canSave.value) return

  saving.value = true
  errorMessage.value = ''

  try {
    const body = buildRequestBody()

    if (isEditMode.value) {
      await updateTransaction(transactionId.value, body)
    } else {
      await createTransaction(body)
    }

    router.push({ name: 'ledger' })
  } catch (err) {
    errorMessage.value = err.response?.data?.message || '거래 내역을 저장하지 못했습니다.'
  } finally {
    saving.value = false
  }
}

// 거래내역 삭제
async function removeTransaction() {
  if (!isEditMode.value || deleting.value || saving.value) return

  const confirmed = window.confirm('이 거래를 삭제할까요?')
  if (!confirmed) return

  deleting.value = true
  errorMessage.value = ''

  try {
    await deleteTransaction(transactionId.value)
    router.push({ name: 'ledger' })
  } catch (err) {
    errorMessage.value = err.response?.data?.message || '거래 내역을 삭제하지 못했습니다.'
  } finally {
    deleting.value = false
  }
}

function goBack() {
  router.push({ name: 'ledger' })
}

onMounted(async () => {
  if (isEditMode.value) {
    await loadTransaction()
  } else {
    await loadCategories()
  }
})

watch(type, loadCategories)
</script>

<template>
  <section class="transaction-form-view">
    <header class="topbar">
      <button class="top-action" type="button" aria-label="닫기" @click="goBack">×</button>
      <h1>{{ isEditMode ? '거래 수정' : '거래 등록' }}</h1>
      <button class="save-link" type="button" :disabled="!canSave" @click="saveTransaction">
        저장
      </button>
    </header>

    <div class="seg">
      <button type="button" :class="{ on: type === 1, income: type === 1 }" @click="selectType(1)">
        수입
      </button>
      <button type="button" :class="{ on: type === 2, expense: type === 2 }" @click="selectType(2)">
        지출
      </button>
    </div>

    <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>

    <div v-if="loading" class="state-box">거래 정보를 불러오는 중입니다.</div>

    <form v-else class="form-body" @submit.prevent="saveTransaction">
      <section class="amount-card">
        <label for="amount">금액</label>
        <div class="amount-preview" :class="type === 1 ? 'income-text' : 'expense-text'">
          {{ type === 1 ? '+' : '-' }} {{ amountPreview }}
          <span>원</span>
        </div>

        <input
          id="amount"
          :value="amount"
          type="text"
          inputmode="numeric"
          placeholder="0"
          @input="onAmountInput"
        />
      </section>

      <div class="field-block">
        <div class="label">분류</div>

        <div v-if="categories.length === 0" class="empty-categories">
          선택할 분류가 없습니다.
        </div>

        <div v-else class="chips">
          <button
            v-for="category in categories"
            :key="category.id"
            type="button"
            class="chip"
            :class="{ on: categoryId === category.id }"
            @click="categoryId = category.id"
          >
            <span>{{ category.icon || '📦' }}</span>
            {{ category.name }}
          </button>
        </div>
      </div>

      <div class="field-block">
        <label class="label" for="date">날짜</label>
        <label class="date-field">
          <span>{{ formatDateLabel(date) }}</span>
          <small>📅</small>
          <input id="date" v-model="date" type="date" />
        </label>
      </div>

      <div class="field-block">
        <label class="label" for="memo">메모</label>
        <input id="memo" v-model="memo" class="field" type="text" placeholder="메모를 입력하세요." />
      </div>

      <button class="submit-button" type="submit" :disabled="!canSave">
        {{ saving ? '저장 중...' : '저장하기' }}
      </button>

      <button v-if="isEditMode" class="delete-button" type="button" :disabled="deleting || saving" @click="removeTransaction">
        {{ deleting ? '삭제 중...' : '삭제하기' }}
      </button>
    </form>
  </section>
</template>

<style scoped>
.transaction-form-view {
  min-height: calc(100vh - 76px);
  padding: 18px 20px 28px;
  background: var(--cream);
}

.topbar {
  height: 42px;
  display: grid;
  grid-template-columns: 42px 1fr 42px;
  align-items: center;
  margin-bottom: 18px;
}

.topbar h1 {
  text-align: center;
  color: var(--ink);
  font-size: 18px;
  font-weight: 900;
}

.top-action,
.save-link {
  border: 0;
  background: transparent;
  font: inherit;
  cursor: pointer;
}

.top-action {
  color: var(--ink-2);
  font-size: 28px;
  line-height: 1;
}

.save-link {
  color: var(--gold-deep);
  font-size: 14px;
  font-weight: 900;
}

.save-link:disabled {
  color: var(--mute);
  cursor: default;
}

.seg {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px;
  padding: 5px;
  margin-bottom: 14px;
  border-radius: 18px;
  background: var(--cream-2);
}

.seg button {
  height: 44px;
  border: 0;
  border-radius: 14px;
  background: transparent;
  color: var(--ink-2);
  font: inherit;
  font-size: 15px;
  font-weight: 900;
  cursor: pointer;
}

.seg button.on {
  background: #fff;
  box-shadow: 0 3px 10px rgba(120, 90, 30, 0.08);
}

.seg button.on.income {
  color: var(--income);
}

.seg button.on.expense {
  color: var(--expense);
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

.state-box {
  min-height: 180px;
  display: grid;
  place-items: center;
  color: var(--mute);
  font-size: 14px;
  font-weight: 800;
}

.form-body {
  display: flex;
  flex-direction: column;
}

.amount-card {
  text-align: center;
  padding: 22px 18px;
  margin-bottom: 18px;
  border: 1px solid var(--line);
  border-radius: 22px;
  background: #fff;
  box-shadow: 0 4px 12px rgba(120, 90, 30, 0.04);
}

.amount-card label {
  display: block;
  margin-bottom: 6px;
  color: var(--mute);
  font-size: 12px;
  font-weight: 900;
}

.amount-preview {
  margin-bottom: 12px;
  font-size: 34px;
  font-weight: 900;
  line-height: 1.1;
}

.amount-preview span {
  color: var(--mute);
  font-size: 18px;
}

.income-text {
  color: var(--income);
}

.expense-text {
  color: var(--expense);
}

.amount-card input {
  width: 100%;
  height: 44px;
  border: 1px solid var(--line);
  border-radius: 14px;
  padding: 0 14px;
  background: var(--cream);
  color: var(--ink);
  font: inherit;
  font-size: 18px;
  font-weight: 900;
  text-align: center;
  outline: none;
}

.field-block {
  margin-bottom: 18px;
}

.label {
  display: block;
  margin: 0 0 8px 2px;
  color: var(--ink-2);
  font-size: 13px;
  font-weight: 900;
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 9px;
}

.chip {
  border: 1px solid var(--line);
  border-radius: 999px;
  padding: 10px 13px;
  background: #fff;
  color: var(--ink-2);
  font: inherit;
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
}

.chip.on {
  border-color: var(--gold);
  background: var(--gold-soft);
  color: var(--gold-deep);
}

.chip span {
  margin-right: 4px;
}

.empty-categories {
  padding: 16px;
  border-radius: 16px;
  background: #fff;
  color: var(--mute);
  text-align: center;
  font-size: 13px;
  font-weight: 800;
}

.date-field {
  position: relative;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border: 1px solid var(--line);
  border-radius: 14px;
  padding: 0 16px;
  background: #fff;
  color: var(--ink);
  font-size: 15px;
  font-weight: 800;
  cursor: pointer;
}

.date-field small {
  color: var(--mute);
}

.date-field input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

.field {
  width: 100%;
  height: 52px;
  border: 1px solid var(--line);
  border-radius: 14px;
  padding: 0 16px;
  background: #fff;
  color: var(--ink);
  font: inherit;
  font-size: 15px;
  font-weight: 700;
  outline: none;
}

.field::placeholder {
  color: var(--mute);
}

.submit-button {
  height: 54px;
  margin-top: 4px;
  border: 0;
  border-radius: 16px;
  background: linear-gradient(135deg, var(--gold), var(--gold-deep));
  color: #fff;
  font: inherit;
  font-size: 16px;
  font-weight: 900;
  box-shadow: 0 8px 18px rgba(224, 135, 26, 0.35);
  cursor: pointer;
}

.submit-button:disabled {
  opacity: 0.5;
  box-shadow: none;
  cursor: default;
}

.delete-button {
  height: 52px;
  margin-top: 10px;
  border: 1px solid var(--expense);
  border-radius: 16px;
  background: #fff;
  color: var(--expense);
  font: inherit;
  font-size: 15px;
  font-weight: 900;
  cursor: pointer;
}

.delete-button:disabled {
  opacity: 0.5;
  cursor: default;
}
</style>