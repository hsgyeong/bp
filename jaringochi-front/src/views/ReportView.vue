<script setup>
import { ref, computed, onMounted } from 'vue'
import { getMonthlyReport, talkToGulbi } from '@/api/report'
import GulbiMascot from '@/components/GulbiMascot.vue'

// ===== 대상 연/월 =====
// 레포트는 완료된 달(지난달)까지만 생성 가능 → 기본값·상한을 "지난달"로 둔다.
const today = new Date()
const latest = new Date(today.getFullYear(), today.getMonth() - 1, 1) // 지난달 1일
const year = ref(latest.getFullYear())
const month = ref(latest.getMonth() + 1)

const report = ref(null)
const loading = ref(false)
const errorMsg = ref('')

// 더 최신(다음) 달로는 못 감 — 지난달이 상한
const isLatest = computed(
  () => year.value === latest.getFullYear() && month.value === latest.getMonth() + 1,
)

const won = (n) => Number(n || 0).toLocaleString()

// 전월 대비 표시 (감소=초록, 증가=코랄)
const diff = computed(() => {
  const d = report.value?.diffRatio
  if (d === null || d === undefined) return null
  const v = Number(d)
  return { abs: Math.abs(v).toFixed(0), down: v < 0, flat: v === 0 }
})

// ===== 카테고리 도넛 =====
const DONUT_COLORS = ['#E8623D', '#F2A33C', '#5B8DEF', '#2FA98C', '#B0A595']
const donut = computed(() => {
  const items = report.value?.categories || []
  if (!items.length) return null
  const r = 54
  const circ = 2 * Math.PI * r
  let acc = 0
  const segs = items.map((it, i) => {
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
  (report.value?.categories || []).map((it, i) => ({
    ...it,
    color: DONUT_COLORS[i % DONUT_COLORS.length],
  })),
)

// ===== 로드 =====
async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await getMonthlyReport(year.value, month.value)
    report.value = res.data.data
  } catch (e) {
    errorMsg.value = e.response?.data?.message || '레포트를 불러오지 못했어요.'
    report.value = null
  } finally {
    loading.value = false
  }
}
function prevMonth() {
  if (month.value === 1) { month.value = 12; year.value-- }
  else month.value--
  load()
}
function nextMonth() {
  if (isLatest.value) return
  if (month.value === 12) { month.value = 1; year.value++ }
  else month.value++
  load()
}
onMounted(load)

// ===== 굴비에게 한 마디 =====
const draft = ref('')
const sending = ref(false)
const talkError = ref('')
const canTalk = computed(() => report.value && !report.value.gulbiReply)

async function send() {
  const msg = draft.value.trim()
  if (!msg || sending.value) return
  sending.value = true
  talkError.value = ''
  try {
    const res = await talkToGulbi(year.value, month.value, msg)
    report.value = res.data.data
    draft.value = ''
  } catch (e) {
    talkError.value = e.response?.data?.message || '굴비가 답하지 못했어요. 잠시 후 다시 시도해 주세요.'
  } finally {
    sending.value = false
  }
}
</script>

<template>
  <div class="report-view">
    <h1 class="title">이달의 굴비 레포트</h1>

    <!-- 월 네비게이션 -->
    <div class="month-nav">
      <button class="nav-btn" @click="prevMonth" aria-label="이전 달">‹</button>
      <span class="month-label">{{ year }}년 {{ month }}월</span>
      <button class="nav-btn" :disabled="isLatest" @click="nextMonth" aria-label="다음 달">›</button>
    </div>

    <div v-if="loading" class="empty">굴비가 한 달을 정산하는 중...</div>
    <div v-else-if="errorMsg" class="empty">{{ errorMsg }}</div>

    <template v-else-if="report">
      <!-- 굴비 한줄평 -->
      <div class="card gulbi-card">
        <GulbiMascot :mood="report.mood || 'hello'" :size="96" />
        <div class="bubble">{{ report.oneLiner }}</div>
      </div>

      <!-- 핵심 숫자 카드 -->
      <div class="num-grid">
        <div class="card num">
          <div class="num-lbl">총 지출</div>
          <div class="num-val expense">{{ won(report.totalExpense) }}원</div>
        </div>
        <div class="card num">
          <div class="num-lbl">전월 대비</div>
          <div v-if="diff" class="num-val" :style="{ color: diff.down ? 'var(--income)' : 'var(--expense)' }">
            <template v-if="diff.flat">변동 없음</template>
            <template v-else>{{ diff.down ? '▼' : '▲' }} {{ diff.abs }}%</template>
          </div>
          <div v-else class="num-val muted">-</div>
        </div>
        <div class="card num">
          <div class="num-lbl">예산 성공</div>
          <div class="num-val">
            <span class="income">{{ report.successWeeks ?? 0 }}</span
            ><span class="muted"> / {{ report.totalWeeks ?? 0 }}주</span>
          </div>
        </div>
      </div>

      <!-- 카테고리 분석 + AI 코멘트 -->
      <div class="card">
        <div class="row between" style="margin-bottom:6px">
          <b style="font-size:15px">카테고리 분석</b>
          <b v-if="report.topCategory" class="muted" style="font-size:13px">최다: {{ report.topCategory }}</b>
        </div>

        <div class="donut-wrap" v-if="donut">
          <svg viewBox="0 0 160 160" class="donut">
            <circle v-for="(s, i) in donut.segs" :key="i" cx="80" cy="80" :r="donut.r"
                    fill="none" :stroke="s.color" stroke-width="26"
                    :stroke-dasharray="s.dash" :stroke-dashoffset="s.offset" />
          </svg>
        </div>

        <div class="legend" v-for="it in legend" :key="it.categoryName">
          <span class="lg"><i :style="{ background: it.color }"></i>{{ it.categoryName }}</span>
          <b>{{ won(it.amount) }}</b>
          <span class="lp">{{ Number(it.ratio).toFixed(0) }}%</span>
          <span class="ld" :class="Number(it.diffAmount) > 0 ? 'up' : Number(it.diffAmount) < 0 ? 'dn' : 'fl'">
            <template v-if="Number(it.diffAmount) > 0">▲{{ won(it.diffAmount) }}</template>
            <template v-else-if="Number(it.diffAmount) < 0">▼{{ won(Math.abs(Number(it.diffAmount))) }}</template>
            <template v-else>-</template>
          </span>
        </div>

        <div v-if="!donut" class="empty">지출 기록이 없어요</div>

        <p class="ai-comment" v-if="report.categoryComment">🐟 {{ report.categoryComment }}</p>
      </div>

      <!-- 굴비의 조언 -->
      <div class="card advice" v-if="report.advice">
        <div class="advice-head">굴비의 한 수</div>
        <p>{{ report.advice }}</p>
      </div>

      <!-- 굴비에게 한 마디 (월 1회) -->
      <div class="card talk">
        <div class="advice-head">굴비에게 한 마디 <span class="once">한 달에 한 번</span></div>

        <!-- 이미 주고받음 -->
        <template v-if="report.gulbiReply">
          <div class="chat me">{{ report.userMessage }}</div>
          <div class="chat gulbi"><span class="g-ico">🐟</span>{{ report.gulbiReply }}</div>
        </template>

        <!-- 아직 안 함: 입력 -->
        <template v-else-if="canTalk">
          <p class="talk-guide">이번 달 살림을 본 굴비에게 하고 싶은 말을 건네보세요. (이번 달 1회)</p>
          <div class="talk-input">
            <input v-model="draft" type="text" maxlength="200" placeholder="예) 다음 달엔 외식 좀 줄여볼게!"
                   @keyup.enter="send" :disabled="sending" />
            <button class="send-btn" :disabled="sending || !draft.trim()" @click="send">
              {{ sending ? '...' : '보내기' }}
            </button>
          </div>
          <p class="talk-error" v-if="talkError">{{ talkError }}</p>
        </template>
      </div>
    </template>
  </div>
</template>

<style scoped>
.report-view { padding: 34px 20px 24px; }
.title { font-size: 18px; margin-bottom: 14px; }

.month-nav { display: flex; align-items: center; justify-content: center; gap: 18px; margin-bottom: 14px; }
.month-label { font-size: 15px; font-weight: 800; color: var(--ink); }
.nav-btn { width: 32px; height: 32px; border-radius: 10px; border: 1px solid var(--line);
  background: var(--card); color: var(--ink); font-size: 18px; font-weight: 700; cursor: pointer; }
.nav-btn:disabled { color: var(--mute); opacity: .4; cursor: default; }

.card { background: var(--card); border-radius: 22px; padding: 18px; border: 1px solid var(--line);
  box-shadow: 0 6px 18px rgba(120,90,30,.06); margin-bottom: 12px; }
.row { display: flex; align-items: center; }
.between { justify-content: space-between; }
.muted { color: var(--mute); }
.income { color: var(--income); }
.expense { color: var(--expense); }

/* 굴비 한줄평 */
.gulbi-card { display: flex; align-items: center; gap: 14px; }
.bubble { position: relative; flex: 1; background: var(--cream-2); border-radius: 16px;
  padding: 14px 16px; font-size: 14px; font-weight: 700; line-height: 1.5; color: var(--ink); }
.bubble::before { content: ''; position: absolute; left: -8px; top: 50%; transform: translateY(-50%);
  border: 8px solid transparent; border-right-color: var(--cream-2); }

/* 핵심 숫자 3카드 */
.num-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; margin-bottom: 12px; }
.num { padding: 14px 10px; text-align: center; margin-bottom: 0; }
.num-lbl { font-size: 12px; font-weight: 700; color: var(--mute); margin-bottom: 6px; }
.num-val { font-size: 15px; font-weight: 800; }

/* 도넛 + 범례 */
.donut-wrap { display: flex; justify-content: center; margin: 8px 0 14px; }
.donut { width: 150px; height: 150px; transform: rotate(-90deg); }
.legend { display: flex; align-items: center; gap: 8px; padding: 9px 2px; border-top: 1px solid var(--line);
  font-size: 14px; font-weight: 700; }
.legend:first-of-type { border-top: none; }
.legend .lg { display: flex; align-items: center; gap: 9px; }
.legend .lg i { width: 12px; height: 12px; border-radius: 4px; display: inline-block; }
.legend b { margin-left: auto; }
.legend .lp { color: var(--mute); font-size: 13px; min-width: 36px; text-align: right; }
.legend .ld { font-size: 11px; font-weight: 700; min-width: 56px; text-align: right; }
.legend .ld.up { color: var(--expense); }
.legend .ld.dn { color: var(--income); }
.legend .ld.fl { color: var(--mute); }

.ai-comment { margin: 12px 0 0; padding: 12px 14px; background: var(--cream-2); border-radius: 14px;
  font-size: 13px; font-weight: 700; line-height: 1.6; color: var(--ink); }

/* 조언 / 한 마디 */
.advice-head { font-size: 14px; font-weight: 800; color: var(--gold-deep); margin-bottom: 8px; }
.advice p, .talk-guide { font-size: 14px; font-weight: 600; line-height: 1.6; color: var(--ink); margin: 0; }
.once { font-size: 11px; font-weight: 700; color: var(--mute); margin-left: 4px; }

.talk-input { display: flex; gap: 8px; margin-top: 10px; }
.talk-input input { flex: 1; border: 1px solid var(--line); border-radius: 12px; padding: 11px 13px;
  font-size: 14px; background: #fff; color: var(--ink); }
.send-btn { border: none; border-radius: 12px; padding: 0 16px; font-size: 14px; font-weight: 800;
  background: var(--gold-deep); color: #fff; cursor: pointer; }
.send-btn:disabled { opacity: .5; cursor: default; }
.talk-error { margin: 8px 0 0; font-size: 12px; font-weight: 700; color: var(--expense); }

.chat { max-width: 85%; padding: 11px 14px; border-radius: 16px; font-size: 14px; font-weight: 700;
  line-height: 1.5; margin-top: 8px; }
.chat.me { margin-left: auto; background: var(--gold-soft, #fbe4c4); color: var(--ink); border-bottom-right-radius: 4px; }
.chat.gulbi { background: var(--cream-2); color: var(--ink); border-bottom-left-radius: 4px; display: flex; gap: 6px; }
.chat .g-ico { flex-shrink: 0; }

.empty { padding: 40px 24px; text-align: center; color: var(--mute); font-weight: 700; }
</style>
