<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { getByCategory, getMonthlyTrend } from '@/api/statistics'
import { getRecentWeeks } from '@/api/budget'

// 3단 토글
const period = ref('month')  // 'month' | 'week'
const type   = ref(2)        // 1=수입 / 2=지출 (월에서만 노출)
const view   = ref('amount') // 'amount'(단순금액) | 'category'(카테고리별)

// 데이터
const trend = ref(null)      // monthly-trend: { items:[{month,amount}], diffRatio }
const cat   = ref(null)      // by-category: { total, items:[{categoryId,categoryName,amount,ratio}] }
const weeks = ref([])        // budgets/weekly/recent (과거->현재, 최대 4주)

const DONUT_COLORS = ['#E8623D', '#F2A33C', '#5B8DEF', '#2FA98C', '#B0A595']

const won = (n) => Number(n || 0).toLocaleString()

// ===== 날짜 범위 =====
const pad = (n) => String(n).padStart(2, '0')
const fmt = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`

function monthRange() {
  const t = new Date()
  const first = new Date(t.getFullYear(), t.getMonth(), 1)
  const last  = new Date(t.getFullYear(), t.getMonth() + 1, 0)
  return { startDate: fmt(first), endDate: fmt(last) }
}
function weekRange() {
  const t = new Date()
  const day = (t.getDay() + 6) % 7         // 월=0 … 일=6
  const mon = new Date(t); mon.setDate(t.getDate() - day)
  const sun = new Date(mon); sun.setDate(mon.getDate() + 6)
  return { startDate: fmt(mon), endDate: fmt(sun) }
}

// "2026-05" -> "5월"
const monthLabel = (m) => parseInt(m.split('-')[1], 10) + '월'
// "2026-05-25" -> "5/25"
const md = (s) => { const [, m, d] = s.split('-'); return `${+m}/${+d}` }
const weekLabel = (w) => `${md(w.startDate)}~${md(w.endDate)}`

// ===== 로드 (토글 조합별로 필요한 것만 호출) =====
async function load() {
  if (period.value === 'month') {
    if (view.value === 'amount') {
      const res = await getMonthlyTrend(type.value, 6)
      trend.value = res.data.data
    } else {
      const { startDate, endDate } = monthRange()
      const res = await getByCategory(startDate, endDate, type.value)
      cat.value = res.data.data
    }
  } else {
    // 주 = 지출 전용
    if (view.value === 'amount') {
      const res = await getRecentWeeks()
      weeks.value = res.data.data
    } else {
      const { startDate, endDate } = weekRange()
      const res = await getByCategory(startDate, endDate, 2)
      cat.value = res.data.data
    }
  }
}
onMounted(load)
watch([period, type, view], load)

// ===== 월 단순금액: 꺾은선 =====
const lineColor = computed(() => (type.value === 1 ? 'var(--income)' : 'var(--expense)'))

const trendChart = computed(() => {
  if (!trend.value || !trend.value.items.length) return null
  const items = trend.value.items
  const amts = items.map((i) => Number(i.amount))
  const max = Math.max(...amts, 1)
  const n = items.length
  const x = (i) => (n === 1 ? 165 : 45 + i * (240 / (n - 1)))
  const y = (v) => 130 - (v / max) * 110
  const pts = items.map((it, i) => `${x(i).toFixed(1)},${y(amts[i]).toFixed(1)}`).join(' ')
  const area = `${pts} ${x(n - 1).toFixed(1)},130 ${x(0).toFixed(1)},130`
  const dots = items.map((it, i) => ({ cx: x(i).toFixed(1), cy: y(amts[i]).toFixed(1) }))
  const labels = items.map((it, i) => ({ x: x(i).toFixed(1), t: monthLabel(it.month) }))
  return { pts, area, dots, labels }
})

const latest = computed(() => {
  const its = trend.value?.items
  return its && its.length ? its[its.length - 1] : null
})
const diff = computed(() => {
  const d = trend.value?.diffRatio
  if (d === null || d === undefined) return null
  const v = Number(d)
  return { abs: Math.abs(v).toFixed(0), down: v < 0, flat: v === 0 }
})

// ===== 카테고리별: 도넛 =====
const catTitle = computed(() => {
  if (period.value === 'week') return '이번 주 지출 구성'
  const m = new Date().getMonth() + 1
  return `${m}월 ${type.value === 1 ? '수입' : '지출'} 구성`
})
const donut = computed(() => {
  if (!cat.value || !cat.value.items.length) return null
  const r = 54
  const circ = 2 * Math.PI * r
  let acc = 0
  const segs = cat.value.items.map((it, i) => {
    const seg = (Number(it.ratio) / 100) * circ
    const s = {
      color: DONUT_COLORS[i % DONUT_COLORS.length],
      dash: `${seg.toFixed(1)} ${(circ - seg).toFixed(1)}`,
      offset: (-acc).toFixed(1),
    }
    acc += seg
    return s
  })
  return { r, segs }
})
const legend = computed(() =>
  (cat.value?.items || []).map((it, i) => ({
    ...it,
    color: DONUT_COLORS[i % DONUT_COLORS.length],
  }))
)

// ===== 주 단순금액: 막대(예산 vs 지출) + 달성률 꺾은선 =====
const barChart = computed(() => {
  if (!weeks.value.length) return null
  const vals = weeks.value.flatMap((w) => [Number(w.amount), Number(w.spentMoney)])
  const max = Math.max(...vals, 1)
  const n = weeks.value.length
  const groupW = 270 / n
  const h = (v) => (v / max) * 80
  const bars = weeks.value.map((w, i) => {
    const gx = 20 + i * groupW + (groupW - 40) / 2
    const bH = h(Number(w.amount))
    const sH = h(Number(w.spentMoney))
    return {
      budget: { x: gx.toFixed(1), y: (120 - bH).toFixed(1), h: bH.toFixed(1) },
      spent:  { x: (gx + 20).toFixed(1), y: (120 - sH).toFixed(1), h: sH.toFixed(1) },
      labelX: (gx + 19).toFixed(1),
      label: weekLabel(w),
    }
  })
  return { bars }
})
const rateChart = computed(() => {
  if (!weeks.value.length) return null
  const rates = weeks.value.map((w) => Number(w.ratio))
  const scaleMax = Math.max(100, ...rates, 1)
  const n = weeks.value.length
  const x = (i) => (n === 1 ? 165 : 55 + i * (225 / (n - 1)))
  const y = (v) => 95 - (v / scaleMax) * 60
  const pts = rates.map((v, i) => `${x(i).toFixed(1)},${y(v).toFixed(1)}`).join(' ')
  const dots = rates.map((v, i) => ({ cx: x(i).toFixed(1), cy: y(v).toFixed(1) }))
  const labels = weeks.value.map((w, i) => ({ x: x(i).toFixed(1), t: weekLabel(w) }))
  return { pts, dots, y100: y(100).toFixed(1), labels }
})
</script>

<template>
  <div class="stats-view">
    <h1 class="title">통계</h1>

    <!-- 기간 -->
    <div class="seg">
      <div class="s" :class="{ on: period === 'month' }" @click="period = 'month'">월</div>
      <div class="s" :class="{ on: period === 'week' }" @click="period = 'week'">주</div>
    </div>

    <!-- 타입 (월만) -->
    <div class="seg" v-if="period === 'month'">
      <div class="s" :class="{ on: type === 2 }" @click="type = 2">지출</div>
      <div class="s" :class="{ on: type === 1 }" @click="type = 1">수입</div>
    </div>

    <!-- 보기 -->
    <div class="seg">
      <div class="s" :class="{ on: view === 'amount' }" @click="view = 'amount'">단순 금액</div>
      <div class="s" :class="{ on: view === 'category' }" @click="view = 'category'">카테고리별</div>
    </div>

    <!-- ===== 월 · 단순금액: 꺾은선 + 요약 ===== -->
    <template v-if="period === 'month' && view === 'amount'">
      <div class="card">
        <b style="font-size:15px">월별 {{ type === 1 ? '수입' : '지출' }} 추이</b>
        <div class="muted sub">최근 6개월</div>
        <svg v-if="trendChart" viewBox="0 0 300 160" class="chart">
          <line x1="30" y1="20" x2="30" y2="130" stroke="var(--line)" />
          <line x1="30" y1="130" x2="290" y2="130" stroke="var(--line)" />
          <polygon :fill="lineColor" opacity="0.08" :points="trendChart.area" />
          <polyline fill="none" :stroke="lineColor" stroke-width="3" stroke-linejoin="round"
                    stroke-linecap="round" :points="trendChart.pts" />
          <g :fill="lineColor">
            <circle v-for="(d, i) in trendChart.dots" :key="i" :cx="d.cx" :cy="d.cy" r="4" />
          </g>
          <g fill="var(--mute)" font-size="11" font-weight="700" text-anchor="middle">
            <text v-for="(l, i) in trendChart.labels" :key="i" :x="l.x" y="148">{{ l.t }}</text>
          </g>
        </svg>
        <div v-else class="empty">데이터가 없어요</div>
      </div>

      <div class="card" v-if="latest">
        <div class="row between">
          <span class="muted lbl">{{ monthLabel(latest.month) }} {{ type === 1 ? '수입' : '지출' }}</span>
          <b class="amt-big" :style="{ color: lineColor }">{{ won(latest.amount) }}원</b>
        </div>
        <div class="row between" style="margin-top:10px">
          <span class="muted lbl">전월 대비</span>
          <b v-if="diff" :style="{ color: diff.down ? 'var(--income)' : 'var(--expense)', fontSize: '15px' }">
            <template v-if="diff.flat">변동 없음</template>
            <template v-else>{{ diff.down ? '▼' : '▲' }} {{ diff.abs }}%</template>
          </b>
          <b v-else class="muted">-</b>
        </div>
      </div>
    </template>

    <!-- ===== 카테고리별: 도넛 (월/주 공용) ===== -->
    <template v-if="view === 'category'">
      <div class="card">
        <div class="row between" style="margin-bottom:6px">
          <b style="font-size:15px">{{ catTitle }}</b>
          <b style="color:var(--expense)" v-if="cat">{{ won(cat.total) }}원</b>
        </div>
        <div class="donut-wrap" v-if="donut">
          <svg viewBox="0 0 160 160" class="donut">
            <circle v-for="(s, i) in donut.segs" :key="i" cx="80" cy="80" :r="donut.r"
                    fill="none" :stroke="s.color" stroke-width="26"
                    :stroke-dasharray="s.dash" :stroke-dashoffset="s.offset" />
          </svg>
        </div>
        <div class="legend" v-for="it in legend" :key="it.categoryId ?? 'etc'">
          <span class="lg"><i :style="{ background: it.color }"></i>{{ it.categoryName }}</span>
          <b>{{ won(it.amount) }}</b>
          <span class="lp">{{ Number(it.ratio).toFixed(0) }}%</span>
        </div>
        <div v-if="!donut" class="empty">데이터가 없어요</div>
      </div>
    </template>

    <!-- ===== 주 · 단순금액: 막대 + 달성률 ===== -->
    <template v-if="period === 'week' && view === 'amount'">
      <div class="card">
        <b style="font-size:15px">주별 예산 vs 지출</b>
        <div class="bar-legend">
          <span><i style="background:var(--gold-soft)"></i>예산</span>
          <span><i style="background:var(--gold-deep)"></i>지출</span>
        </div>
        <svg v-if="barChart" viewBox="0 0 300 150" class="chart">
          <line x1="20" y1="120" x2="290" y2="120" stroke="var(--line)" />
          <template v-for="(b, i) in barChart.bars" :key="i">
            <rect :x="b.budget.x" :y="b.budget.y" width="18" :height="b.budget.h" rx="4" fill="var(--gold-soft)" />
            <rect :x="b.spent.x" :y="b.spent.y" width="18" :height="b.spent.h" rx="4" fill="var(--gold-deep)" />
          </template>
          <g fill="var(--mute)" font-size="9" font-weight="700" text-anchor="middle">
            <text v-for="(b, i) in barChart.bars" :key="i" :x="b.labelX" y="138">{{ b.label }}</text>
          </g>
        </svg>
        <div v-else class="empty">예산 기록이 없어요</div>
      </div>

      <div class="card" v-if="rateChart">
        <b style="font-size:15px">예산 대비 지출 달성률</b>
        <svg viewBox="0 0 300 130" class="chart" style="margin-top:8px">
          <line x1="30" y1="95" x2="290" y2="95" stroke="var(--line)" />
          <line x1="30" :y1="rateChart.y100" x2="290" :y2="rateChart.y100" stroke="var(--cream-2)" stroke-dasharray="3 3" />
          <text x="2" :y="Number(rateChart.y100) + 4" fill="var(--mute)" font-size="10" font-weight="700">100%</text>
          <polyline fill="none" stroke="var(--income)" stroke-width="3" stroke-linejoin="round"
                    stroke-linecap="round" :points="rateChart.pts" />
          <g fill="var(--income)">
            <circle v-for="(d, i) in rateChart.dots" :key="i" :cx="d.cx" :cy="d.cy" r="4" />
          </g>
          <g fill="var(--mute)" font-size="9" font-weight="700" text-anchor="middle">
            <text v-for="(l, i) in rateChart.labels" :key="i" :x="l.x" y="113">{{ l.t }}</text>
          </g>
        </svg>
      </div>
    </template>
  </div>
</template>

<style scoped>
.stats-view { padding: 16px 20px 24px; }
.title { font-size: 18px; margin-bottom: 14px; }

.seg { display: flex; background: var(--cream-2); border-radius: 14px; padding: 4px; gap: 4px; margin-bottom: 10px; }
.seg .s { flex: 1; text-align: center; padding: 9px 0; font-size: 13px; font-weight: 700; color: var(--ink-2); border-radius: 11px; cursor: pointer; }
.seg .s.on { background: #fff; color: var(--gold-deep); box-shadow: 0 2px 6px rgba(120,90,30,.1); }

.card { background: var(--card); border-radius: 22px; padding: 18px; box-shadow: 0 6px 18px rgba(120,90,30,.06); border: 1px solid var(--line); margin-top: 4px; margin-bottom: 12px; }
.row { display: flex; align-items: center; }
.between { justify-content: space-between; }
.muted { color: var(--mute); }
.sub { font-size: 12px; font-weight: 700; margin: 4px 0 14px; }
.lbl { font-size: 13px; font-weight: 700; }
.amt-big { font-size: 17px; }
.chart { width: 100%; height: auto; }

.donut-wrap { display: flex; justify-content: center; margin: 8px 0 14px; }
.donut { width: 150px; height: 150px; transform: rotate(-90deg); }

.legend { display: flex; align-items: center; gap: 8px; padding: 9px 2px; border-top: 1px solid var(--line); font-size: 14px; font-weight: 700; }
.legend:first-of-type { border-top: none; }
.legend .lg { display: flex; align-items: center; gap: 9px; }
.legend .lg i { width: 12px; height: 12px; border-radius: 4px; display: inline-block; }
.legend b { margin-left: auto; }
.legend .lp { color: var(--mute); font-size: 13px; min-width: 38px; text-align: right; }

.bar-legend { display: flex; gap: 14px; font-size: 11px; font-weight: 700; color: var(--mute); margin: 6px 0 10px; }
.bar-legend span { display: flex; align-items: center; gap: 5px; }
.bar-legend i { width: 10px; height: 10px; border-radius: 3px; display: inline-block; }

.empty { padding: 24px; text-align: center; color: var(--mute); font-weight: 600; }
</style>
