<script setup>
import { ref, onMounted } from 'vue'
import { getCurrentWeek, getRecentWeeks, createWeeklyBudget, updateWeeklyBudget } from '@/api/budget'

const current = ref(null)   // 이번 주 예산 (없으면 null)
const weeks   = ref([])     // 최근 5주 목록
const amount  = ref('')     // 폼 입력 금액

// 두 API를 같이 불러 화면 상태를 채운다
async function load() {
  const cur = await getCurrentWeek()
  current.value = cur.data.data          // 봉투 두 겹: axios .data + 우리 봉투 .data
  const rec = await getRecentWeeks()
  weeks.value = rec.data.data
}

onMounted(load)   // 화면 처음 뜰 때 1회

// 천 단위 콤마 (45000 → "45,000")
function won(n) {
  return Number(n).toLocaleString()
}

// 이번 주 월요일~일요일 날짜 계산 (오늘 기준)
function thisWeekRange() {
  const t = new Date()
  const day = (t.getDay() + 6) % 7       // 월=0 … 일=6
  const mon = new Date(t); mon.setDate(t.getDate() - day)
  const sun = new Date(mon); sun.setDate(mon.getDate() + 6)
  const fmt = (d) =>
    `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  return { startDate: fmt(mon), endDate: fmt(sun) }
}

// 저장: 이번 주 예산이 있으면 수정(PUT), 없으면 등록(POST)
async function onSave() {
  const value = Number(amount.value)
  if (!value || value <= 0) return alert('금액을 입력하세요')
  const { startDate, endDate } = thisWeekRange()
  if (current.value) {
    await updateWeeklyBudget(current.value.id, { amount: value, startDate, endDate })
  } else {
    await createWeeklyBudget({ amount: value, startDate, endDate })
  }
  amount.value = ''
  await load()   // 성공 후 다시 불러 새로고침
}
</script>

<template>
  <div class="budget-view">
    <h1 class="title">예산</h1>

    <!-- 굴비 한마디 -->
    <div class="gulbi-msg" v-if="current">
      <div class="bubble-text">이번 주 예산의 <b>{{ Math.round(current.ratio) }}%</b> 썼어요. 🐟</div>
    </div>

    <!-- 이번 주 사용률 -->
    <div class="card" v-if="current">
      <div class="row between" style="margin-bottom:12px">
        <b>이번 주 예산 사용률</b>
        <b style="color:var(--gold-deep)">{{ Math.round(current.ratio) }}%</b>
      </div>
      <div class="bar-track"><div class="bar-fill" :style="{ width: Math.min(current.ratio, 100) + '%' }"></div></div>
      <div class="row between" style="margin-top:9px;font-size:12px;font-weight:700;color:var(--mute)">
        <span>사용 {{ won(current.spentMoney) }}원</span>
        <span>예산 {{ won(current.amount) }}원</span>
      </div>
      <div class="row between" style="margin-top:6px;font-size:12px;font-weight:700;color:var(--mute)">
        <span>{{ current.startDate }} ~ {{ current.endDate }}</span>
        <span>남은 {{ won(current.remaining) }}원</span>
      </div>
    </div>
    <div class="card" v-else>
      <div class="muted" style="text-align:center;font-weight:700">이번 주 예산이 아직 없어요. 아래에서 설정하세요.</div>
    </div>

    <!-- 예산 설정/수정 폼 -->
    <div class="label" style="margin-top:20px">이번 주 예산 {{ current ? '수정' : '설정' }}</div>
    <div class="card">
      <input class="field" type="number" v-model="amount" placeholder="금액 입력 (원)" />
      <p v-if="current && !current.updatable" class="muted" style="font-size:12px;font-weight:700;margin:10px 2px 0">
        이번 달 수정 횟수(2회)를 모두 사용했어요.
      </p>
    </div>
    <button class="btn-primary" style="margin-top:14px" :disabled="current && !current.updatable" @click="onSave">
      {{ current ? '예산 수정' : '예산 저장' }}
    </button>

    <!-- 최근 5주 -->
    <div class="label" style="margin-top:24px">최근 주간 예산</div>
    <div class="card list">
      <div class="menu" v-for="w in weeks" :key="w.id">
        <span class="period">{{ w.startDate }} ~ {{ w.endDate }}</span>
        <span class="ratio">{{ Math.round(w.ratio) }}%</span>
        <b class="amt">{{ won(w.amount) }}원</b>
      </div>
      <div v-if="weeks.length === 0" class="empty">예산 기록이 없어요</div>
    </div>
  </div>
</template>

<style scoped>
.budget-view { padding: 16px 20px 24px; }
.title { font-size: 18px; margin-bottom: 14px; }
.card { background: var(--card); border-radius: 22px; padding: 18px; box-shadow: 0 6px 18px rgba(120,90,30,.06); border: 1px solid var(--line); }
.gulbi-msg { background: linear-gradient(135deg,#FFF7E8,#FCEBCB); border: 1px solid var(--gold-soft); border-radius: 22px; padding: 14px 16px; margin-bottom: 14px; }
.bubble-text { font-size: 14px; font-weight: 600; color: #6b5320; }
.row { display: flex; align-items: center; }
.between { justify-content: space-between; }
.muted { color: var(--mute); }
.bar-track { height: 14px; background: var(--cream-2); border-radius: 10px; overflow: hidden; }
.bar-fill { height: 100%; border-radius: 10px; background: linear-gradient(90deg, var(--gold), var(--gold-deep)); }
.field { width: 100%; border: 1.5px solid var(--line); background: #fff; border-radius: 14px; padding: 15px 16px; font-size: 15px; font-family: inherit; font-weight: 600; color: var(--ink); box-sizing: border-box; }
.label { font-size: 13px; font-weight: 700; color: var(--ink-2); margin: 0 0 8px 2px; }
.btn-primary { width: 100%; border: none; border-radius: 16px; padding: 16px; font-size: 16px; font-weight: 800; font-family: inherit; color: #fff; background: linear-gradient(135deg, var(--gold), var(--gold-deep)); box-shadow: 0 8px 18px rgba(224,135,26,.35); cursor: pointer; }
.btn-primary:disabled { opacity: .5; box-shadow: none; cursor: not-allowed; }
.list { padding: 4px 6px; }
.menu { display: flex; align-items: center; gap: 10px; padding: 14px 8px; border-bottom: 1px solid var(--line); font-size: 13px; font-weight: 700; }
.menu:last-child { border-bottom: none; }
.period { color: var(--ink-2); }
.ratio { color: var(--gold-deep); }
.amt { margin-left: auto; font-size: 14px; }
.empty { padding: 24px; text-align: center; color: var(--mute); font-weight: 600; }
</style>