<script setup>
import { ref, computed, onMounted } from 'vue'
import { getMonthlyReport, talkToGulbi } from '@/api/report'
import { fetchMeApi } from '@/api/auth'
import { useTheme } from '@/composables/useTheme'
import GulbiMascot from '@/components/GulbiMascot.vue'
import MonthPicker from '@/components/MonthPicker.vue'

// paint 테마일 때 차트를 '색연필' 톤으로 (StatsView 와 동일 규칙)
const { theme } = useTheme()
const isPaint = computed(() => theme.value === 'paint')

// ===== 대상 연/월 =====
// 레포트는 완료된 달(지난달)까지만 생성 가능 → 기본값·상한을 "지난달"로 둔다.
const today = new Date()
const latest = new Date(today.getFullYear(), today.getMonth() - 1, 1) // 지난달 1일
const year = ref(latest.getFullYear())
const month = ref(latest.getMonth() + 1)

const report = ref(null)
const loading = ref(false)
const errorMsg = ref('')

const gulbiImages = ref(null)   // 현재 착용 옷 이미지 맵 (받기 한 옷)

// 더 최신(다음) 달로는 못 감 — 지난달이 상한
const isLatest = computed(
  () => year.value === latest.getFullYear() && month.value === latest.getMonth() + 1,
)

// 월 선택 모달(MonthPicker)용 'YYYY-MM' 양방향 바인딩 + 상한(지난달)
const pad2 = (n) => String(n).padStart(2, '0')
const maxYm = `${latest.getFullYear()}-${pad2(latest.getMonth() + 1)}`
const ymValue = computed({
  get: () => `${year.value}-${pad2(month.value)}`,
  set: (v) => {
    const [y, m] = v.split('-').map(Number)
    year.value = y
    month.value = m
    load()
  },
})

const won = (n) => Number(n || 0).toLocaleString()          // 원본 금액 그대로(천단위 구분만)
const md = (iso) => {                                        // 'YYYY-MM-DD' → 'M/D'
  if (!iso) return ''
  const [, m, d] = String(iso).split('-')
  return `${Number(m)}/${Number(d)}`
}

// 레포트 표정은 4종만 (hello는 로그인/회원가입 전용). 옛 값(warn 등)은 happy로 흡수
const REPORT_MOODS = ['happy', 'smirk', 'angry', 'sad']
const mascotMood = computed(() =>
  REPORT_MOODS.includes(report.value?.mood) ? report.value.mood : 'happy',
)

// 전월 대비 표시 (감소=초록, 증가=코랄)
const diff = computed(() => {
  const d = report.value?.diffRatio
  if (d === null || d === undefined) return null
  const v = Number(d)
  return { abs: Math.abs(v).toFixed(0), down: v < 0, flat: v === 0 }
})

// ===== 카테고리 도넛 (전월/당월 2개, 색은 categories 리스트 위치 기준으로 양쪽 일관) =====
const DONUT_COLORS = ['#E8623D', '#F2A33C', '#5B8DEF', '#2FA98C', '#B0A595', '#2F4B8F', '#E48AB0', '#6FB04A']  // classic
const DONUT_PAINT  = ['#E08A72', '#E7BE63', '#94B8E6', '#B197D0', '#7FBE96', '#5E72A8', '#E6A6C4', '#9FC27E']  // paint(색연필)
const palette = computed(() => (isPaint.value ? DONUT_PAINT : DONUT_COLORS))

// categories 에 색을 입힌 공용 리스트 (도넛 2개 + 범례가 같은 색 매핑 공유)
const cats = computed(() =>
  (report.value?.categories || []).map((it, i) => ({
    ...it,
    color: palette.value[i % palette.value.length],
  })),
)

function buildDonut(ratioField) {
  const items = cats.value
  if (!items.length) return null
  const r = 54
  const circ = 2 * Math.PI * r
  let acc = 0
  const segs = []
  for (const it of items) {
    const ratio = Number(it[ratioField]) || 0
    if (ratio <= 0) continue
    const seg = (ratio / 100) * circ
    segs.push({
      color: it.color,
      dash: `${seg.toFixed(1)} ${(circ - seg).toFixed(1)}`,
      offset: (-acc).toFixed(1),
    })
    acc += seg
  }
  return segs.length ? { r, segs } : null
}
const curDonut = computed(() => buildDonut('ratio'))
const prevDonut = computed(() => buildDonut('prevRatio'))

// '기타' 범례 펼쳐보기 — 기타에 합쳐진 세부 카테고리(members)가 있을 때만
const etcOpen = ref(false)
const hasMembers = (it) => Array.isArray(it.members) && it.members.length > 0

// ===== 부가 지표 (extra_json) =====
const extra = computed(() => report.value?.extra || null)
const weekBars = computed(() =>
  (extra.value?.weeks || []).map((w) => {
    const ratio = Math.round(Number(w.ratio) || 0)
    return { label: w.label, ratio, pass: w.pass, width: Math.min(ratio, 100) }
  }),
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
onMounted(() => {
  load()
  // 현재 착용 옷(받기 한 경우)을 불러와 굴비에 입힘
  fetchMeApi()
    .then((res) => { gulbiImages.value = res.data?.data?.currentGulbiImages || null })
    .catch(() => { gulbiImages.value = null })
})

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
    <!-- 색연필 빗금(hatch) 패턴 — paint 도넛 컬러 위에 덧칠 (StatsView 와 동일 id) -->
    <svg width="0" height="0" style="position:absolute" aria-hidden="true">
      <defs>
        <pattern id="statsHatch" width="6" height="6" patternUnits="userSpaceOnUse"
                 patternTransform="rotate(45)">
          <line x1="0" y1="0" x2="0" y2="6" stroke="rgba(0,0,0,.16)" stroke-width="1.4" />
        </pattern>
      </defs>
    </svg>

    <h1 class="title">이달의 굴비 레포트</h1>

    <!-- 월 네비게이션 (가운데 라벨을 누르면 월 선택 모달) -->
    <div class="month-nav">
      <button class="nav-btn" @click="prevMonth" aria-label="이전 달">‹</button>
      <MonthPicker v-model="ymValue" :max="maxYm" align="left">
        <span class="month-label">{{ year }}년 {{ month }}월 ▾</span>
      </MonthPicker>
      <button class="nav-btn" :disabled="isLatest" @click="nextMonth" aria-label="다음 달">›</button>
    </div>

    <div v-if="loading" class="empty">굴비가 한 달을 정산하는 중...</div>
    <div v-else-if="errorMsg" class="empty">{{ errorMsg }}</div>

    <template v-else-if="report">
      <!-- 굴비 한줄평 -->
      <div class="card gulbi-card">
        <GulbiMascot :mood="mascotMood" :size="96" :images="gulbiImages" />
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

      <!-- 카테고리 분석: 전월/당월 도넛 2개 + AI 코멘트 -->
      <div class="card">
        <div class="row between" style="margin-bottom:6px">
          <b style="font-size:15px">카테고리 분석</b>
          <b class="muted" style="font-size:13px">전월과 비교</b>
        </div>

        <div class="donut-pair" v-if="curDonut || prevDonut">
          <div class="donut-col">
            <span class="donut-cap muted">전월</span>
            <span class="donut-sum muted">{{ won(report.prevExpense) }}원</span>
            <svg viewBox="0 0 160 160" class="donut sub">
              <g :filter="isPaint ? 'url(#paintWobble)' : undefined">
                <circle v-for="(s, i) in (prevDonut?.segs || [])" :key="'pc' + i" cx="80" cy="80" :r="prevDonut.r"
                        fill="none" :stroke="s.color" stroke-width="26"
                        :stroke-dasharray="s.dash" :stroke-dashoffset="s.offset" />
                <template v-if="isPaint && prevDonut">
                  <circle v-for="(s, i) in prevDonut.segs" :key="'ph' + i" cx="80" cy="80" :r="prevDonut.r"
                          fill="none" stroke="url(#statsHatch)" stroke-width="26"
                          :stroke-dasharray="s.dash" :stroke-dashoffset="s.offset" />
                  <circle cx="80" cy="80" :r="prevDonut.r + 13" fill="none" stroke="var(--ink)" stroke-width="1.6" />
                  <circle cx="80" cy="80" :r="prevDonut.r - 13" fill="none" stroke="var(--ink)" stroke-width="1.6" />
                </template>
              </g>
            </svg>
          </div>
          <div class="donut-col">
            <span class="donut-cap">이번 달</span>
            <span class="donut-sum">{{ won(report.totalExpense) }}원</span>
            <svg viewBox="0 0 160 160" class="donut">
              <g :filter="isPaint ? 'url(#paintWobble)' : undefined">
                <circle v-for="(s, i) in (curDonut?.segs || [])" :key="'cc' + i" cx="80" cy="80" :r="curDonut.r"
                        fill="none" :stroke="s.color" stroke-width="26"
                        :stroke-dasharray="s.dash" :stroke-dashoffset="s.offset" />
                <template v-if="isPaint && curDonut">
                  <circle v-for="(s, i) in curDonut.segs" :key="'ch' + i" cx="80" cy="80" :r="curDonut.r"
                          fill="none" stroke="url(#statsHatch)" stroke-width="26"
                          :stroke-dasharray="s.dash" :stroke-dashoffset="s.offset" />
                  <circle cx="80" cy="80" :r="curDonut.r + 13" fill="none" stroke="var(--ink)" stroke-width="1.6" />
                  <circle cx="80" cy="80" :r="curDonut.r - 13" fill="none" stroke="var(--ink)" stroke-width="1.6" />
                </template>
              </g>
            </svg>
          </div>
        </div>

        <template v-for="it in cats" :key="it.categoryId ?? it.categoryName">
          <div class="legend paint-hline" :class="{ 'is-etc': hasMembers(it) }"
               @click="hasMembers(it) && (etcOpen = !etcOpen)">
            <span class="lg">
              <i class="paint-sketch" :style="{ background: it.color }"></i>{{ it.categoryName }}
              <span v-if="hasMembers(it)" class="etc-caret">{{ etcOpen ? '▴' : '▾' }}</span>
            </span>
            <b>{{ won(it.amount) }}</b>
            <span class="lp">{{ Number(it.ratio).toFixed(0) }}%</span>
            <span class="ld" :class="Number(it.diffAmount) > 0 ? 'up' : Number(it.diffAmount) < 0 ? 'dn' : 'fl'">
              <template v-if="Number(it.diffAmount) > 0">▲{{ won(it.diffAmount) }}</template>
              <template v-else-if="Number(it.diffAmount) < 0">▼{{ won(Math.abs(Number(it.diffAmount))) }}</template>
              <template v-else>-</template>
            </span>
          </div>

          <!-- 기타 펼치기: 합쳐진 세부 카테고리 목록 -->
          <div v-if="hasMembers(it) && etcOpen" class="etc-members">
            <div class="etc-member" v-for="m in it.members" :key="m.categoryId ?? m.categoryName">
              <span class="etc-name">{{ m.categoryName }}</span>
              <b>{{ won(m.amount) }}</b>
              <span class="lp">{{ Number(m.ratio).toFixed(0) }}%</span>
            </div>
          </div>
        </template>

        <div v-if="!cats.length" class="empty">지출 기록이 없어요</div>

        <p class="ai-comment" v-if="report.categoryComment">🐟 {{ report.categoryComment }}</p>
      </div>

      <!-- 이번 달 한눈에 -->
      <div class="card" v-if="extra">
        <b style="font-size:15px">이번 달 한눈에</b>
        <div class="glance">
          <div class="g-cell">
            <div class="g-lbl">하루 평균</div>
            <div class="g-val">{{ won(extra.dailyAvg) }}원</div>
          </div>
          <div class="g-cell">
            <div class="g-lbl">무지출 날</div>
            <div class="g-val">{{ extra.noSpendDays ?? 0 }}일 🐟</div>
          </div>
          <div class="g-cell" v-if="extra.biggestDay">
            <div class="g-lbl">가장 큰 하루</div>
            <div class="g-val">{{ md(extra.biggestDay.date) }} · {{ won(extra.biggestDay.amount) }}원</div>
          </div>
          <div class="g-cell" v-if="extra.savedMost">
            <div class="g-lbl">가장 아낀 항목</div>
            <div class="g-val income">{{ extra.savedMost.name }} ▼{{ won(Math.abs(Number(extra.savedMost.diff))) }}</div>
          </div>
          <div class="g-cell" v-else-if="extra.spentMost">
            <div class="g-lbl">가장 늘어난 항목</div>
            <div class="g-val expense">{{ extra.spentMost.name }} ▲{{ won(extra.spentMost.diff) }}</div>
          </div>
        </div>
      </div>

      <!-- 주차별 예산 달성 -->
      <div class="card" v-if="weekBars.length">
        <b style="font-size:15px">주차별 예산 달성</b>
        <div class="weeks">
          <div class="wk" v-for="w in weekBars" :key="w.label">
            <span class="wk-lbl">{{ w.label }}</span>
            <div class="wk-bar"><div class="wk-fill" :class="w.pass ? 'ok' : 'no'" :style="{ width: w.width + '%' }"></div></div>
            <span class="wk-pct" :class="w.pass ? 'income' : 'expense'">{{ w.pass ? '✓' : '✗' }} {{ w.ratio }}%</span>
          </div>
        </div>
      </div>

      <!-- 굴비의 총평 (이번 달 긴 이야기) -->
      <div class="card advice" v-if="report.story">
        <div class="advice-head">굴비의 총평</div>
        <p>{{ report.story }}</p>
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
            <span class="paint-field" style="flex:1">
              <input v-model="draft" type="text" maxlength="200" placeholder="예) 다음 달엔 외식 좀 줄여볼게!"
                     @keyup.enter="send" :disabled="sending" />
            </span>
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

/* 도넛 2개 (전월/당월) + 범례 */
.donut-pair { display: flex; justify-content: center; align-items: flex-end; gap: 10px; margin: 10px 0 14px; }
.donut-col { display: flex; flex-direction: column; align-items: center; gap: 4px; }
.donut { width: 132px; height: 132px; transform: rotate(-90deg); }
.donut.sub { width: 132px; height: 132px; }
.donut-cap { font-size: 12px; font-weight: 800; }
.donut-sum { font-size: 12px; font-weight: 700; margin-bottom: 2px; }
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

/* '기타' 펼쳐보기 */
.legend.is-etc { cursor: pointer; }
.legend .etc-caret { margin-left: 5px; font-size: 11px; color: var(--mute); }
.etc-members { padding: 4px 2px 9px; }
.etc-member { display: flex; align-items: center; gap: 8px; padding: 6px 2px 6px 21px;
  font-size: 13px; font-weight: 700; color: var(--ink); }
.etc-member .etc-name { color: var(--mute); }
.etc-member b { margin-left: auto; font-weight: 800; }
.etc-member .lp { color: var(--mute); font-size: 12px; min-width: 36px; text-align: right; }

.ai-comment { margin: 12px 0 0; padding: 12px 14px; background: var(--cream-2); border-radius: 14px;
  font-size: 13px; font-weight: 700; line-height: 1.6; color: var(--ink); }

/* 이번 달 한눈에 */
.glance { display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px; margin-top: 10px; }
.g-cell { background: var(--cream-2); border-radius: 12px; padding: 10px 12px; }
.g-lbl { font-size: 11px; font-weight: 700; color: var(--mute); margin-bottom: 3px; }
.g-val { font-size: 15px; font-weight: 800; color: var(--ink); }

/* 주차별 막대 */
.weeks { display: flex; flex-direction: column; gap: 9px; margin-top: 10px; }
.wk { display: flex; align-items: center; gap: 8px; font-size: 13px; }
.wk-lbl { width: 48px; color: var(--mute); font-weight: 700; }
.wk-bar { flex: 1; height: 10px; background: var(--cream-2); border-radius: 6px; overflow: hidden; }
.wk-fill { height: 100%; border-radius: 6px; }
.wk-fill.ok { background: var(--income); }
.wk-fill.no { background: var(--expense); }
.wk-pct { width: 64px; text-align: right; font-weight: 800; }


/* 조언 / 한 마디 */
.advice-head { font-size: 14px; font-weight: 800; color: var(--gold-deep); margin-bottom: 8px; }
.advice p, .talk-guide { font-size: 14px; font-weight: 600; line-height: 1.6; color: var(--ink); margin: 0; }
.once { font-size: 11px; font-weight: 700; color: var(--mute); margin-left: 4px; }

.talk-input { display: flex; gap: 8px; margin-top: 10px; }
.talk-input input { width: 100%; border: 1px solid var(--line); border-radius: 12px; padding: 11px 13px;
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
