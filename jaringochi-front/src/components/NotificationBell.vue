<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listNotifications, getUnreadCount, markRead, markAllRead } from '@/api/notification'
import { useTheme } from '@/composables/useTheme'

const route = useRoute()
const router = useRouter()
const { theme } = useTheme()

const open = ref(false)     // 드롭다운 열림 여부
const count = ref(0)        // 안 읽은 개수 (배지)
const items = ref([])       // 알림 목록
const loading = ref(false)

const hasToken = () => !!localStorage.getItem('token')
// 로그인/가입 화면에선 종을 숨김 (비로그인 호출 -> 401 -> 로그인 리다이렉트 방지)
const visible = computed(() => hasToken() && !['/login', '/signup'].includes(route.path))

// 안 읽은 개수 로드 (토큰 있을 때만)
async function loadCount() {
  if (!hasToken()) { count.value = 0; return }
  try {
    const res = await getUnreadCount()
    count.value = res.data.data.count
  } catch (e) {
    // 401 등은 http.js 응답 인터셉터가 처리
  }
}

async function toggle() {
  open.value = !open.value
  if (open.value) await loadList()
}

async function loadList() {
  loading.value = true
  try {
    const res = await listNotifications()   // 전체(안읽음+읽음)
    items.value = res.data.data
  } finally {
    loading.value = false
  }
}

// 한 건 클릭 -> 안 읽음이면 읽음 처리 + 배지 감소 -> 유형별 화면 이동
async function onItemClick(n) {
  if (n.isRead === 0) {
    await markRead(n.id)
    n.isRead = 1
    count.value = Math.max(0, count.value - 1)
  }
  goByType(n)
}

// 알림 유형별 이동: DRAW -> 굴비 옷 뽑기, REPORT -> 월 레포트
function goByType(n) {
  if (n.type === 'DRAW' && n.weeklyBudgetId != null) {
    open.value = false
    router.push({ name: 'gulbi-reward', params: { weeklyBudgetId: n.weeklyBudgetId } })
  } else if (n.type === 'REPORT') {
    open.value = false
    router.push({ name: 'report' })
  }
}

async function onReadAll() {
  await markAllRead()
  items.value.forEach((n) => { n.isRead = 1 })
  count.value = 0
}

// 유형/threshold -> 문구 조립 (DB엔 문구 없음, 프론트가 조립 - 관심사 분리)
// 끝에 붙는 아이콘은 분리: classic=이모지 / paint=Tabler 라인 아이콘
function message(n) {
  if (n.type === 'DRAW') return '절약 성공! 굴비 옷 뽑기 기회가 왔어요'
  if (n.type === 'REPORT') return `${n.reportMonth}월 가계부 레포트가 준비됐어요`
  // BUDGET: 실시간(현재 주)·지난주 결산 양쪽에 쓰이므로 주차 표현 없이 중립적으로
  const t = n.threshold
  if (t >= 125) return `주간 예산을 ${t - 100}% 초과했어요`
  if (t >= 100) return `주간 예산을 다 썼어요!`
  return `주간 예산의 ${t}%를 썼어요`
}
function messageEmoji(n) {
  if (n.type === 'DRAW') return '🎁'
  if (n.type === 'REPORT') return '📑'
  const t = n.threshold
  if (t >= 125) return '😱'
  if (t >= 100) return ''
  return '🐟'
}
function messageTi(n) {
  if (n.type === 'DRAW') return 'ti-gift'
  if (n.type === 'REPORT') return 'ti-report'
  const t = n.threshold
  if (t >= 125) return 'ti-alert-triangle'
  if (t >= 100) return ''
  return 'ti-fish'
}

// "2026-06-14T19:00:00" -> "6/14 19:00"
function fmtDate(s) {
  const d = new Date(s)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getMonth() + 1}/${d.getDate()} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

// BUDGET/DRAW 알림: 그 주 기간 "6/15~6/21" (어느 주인지 표시). TZ 영향 없게 문자열 파싱
function fmtWeek(n) {
  const md = (d) => {
    const [, m, day] = String(d).split('-')
    return `${Number(m)}/${Number(day)}`
  }
  return `${md(n.weekStartDate)}~${md(n.weekEndDate)}`
}

// REPORT 알림: 레포트가 준비된 날 = 그 달 다음 달 1일 (예: 5월 레포트 -> 6/1)
function fmtReportDate(n) {
  let m = n.reportMonth + 1
  if (m > 12) m = 1   // 12월 레포트 -> 1/1
  return `${m}/1`
}

onMounted(loadCount)
watch(() => route.path, loadCount)   // 로그인 후 화면 이동 등에서 배지 갱신
</script>

<template>
  <div v-if="visible" class="noti">
    <!-- 종 + 배지 -->
    <button class="bell" @click="toggle" aria-label="알림">
      <i v-if="theme === 'paint'" class="ti ti-bell" aria-hidden="true"></i>
      <template v-else>🔔</template>
      <span v-if="count > 0" class="badge">{{ count > 99 ? '99+' : count }}</span>
    </button>

    <!-- 드롭다운 -->
    <template v-if="open">
      <div class="backdrop" @click="open = false"></div>
      <div class="panel paint-box">
        <div class="panel-head paint-hline-b">
          <b>알림</b>
          <button class="read-all" @click="onReadAll" :disabled="count === 0">전체 읽음</button>
        </div>

        <div class="panel-list">
        <div v-if="loading" class="empty">불러오는 중…</div>
        <template v-else>
          <div
            v-for="(n, i) in items"
            :key="n.id"
            class="item"
            :class="{ unread: n.isRead === 0, 'paint-hline-b': i < items.length - 1 }"
            @click="onItemClick(n)"
          >
            <span class="dot" :class="{ on: n.isRead === 0 }"></span>
            <div class="item-body">
              <div class="msg">
                {{ message(n) }}
                <i v-if="theme === 'paint' && messageTi(n)" class="ti" :class="messageTi(n)" aria-hidden="true"></i>
                <template v-else-if="messageEmoji(n)">{{ messageEmoji(n) }}</template>
              </div>
              <div class="meta">
                <template v-if="(n.type === 'BUDGET' || n.type === 'DRAW') && n.weekStartDate">{{ fmtWeek(n) }}</template>
                <template v-else-if="n.type === 'REPORT'">{{ fmtReportDate(n) }}</template>
                <template v-else>{{ fmtDate(n.createdAt) }}</template>
                <span v-if="n.ratio != null"> · {{ Number(n.ratio).toFixed(0) }}%</span>
              </div>
            </div>
          </div>
          <div v-if="items.length === 0" class="empty">
            알림이 없어요
            <i v-if="theme === 'paint'" class="ti ti-fish" aria-hidden="true"></i>
            <template v-else>🐟</template>
          </div>
        </template>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
/* 앱 폭(480) 중앙 정렬 컨테이너 - 자체는 클릭 통과, 자식만 클릭 받음 */
.noti {
  position: fixed;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 480px;
  z-index: 50;
  pointer-events: none;
}
.bell, .panel, .backdrop { pointer-events: auto; }

.bell {
  position: absolute;
  top: 12px;
  right: 16px;
  width: 40px;
  height: 40px;
  border: 1px solid var(--line);
  border-radius: 50%;
  background: var(--card);
  font-size: 19px;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(120, 90, 30, .1);
}
.badge {
  position: absolute;
  top: -4px;
  right: -4px;
  min-width: 18px;
  height: 18px;
  padding: 0 4px;
  border-radius: 9px;
  background: var(--expense);
  color: #fff;
  font-size: 11px;
  font-weight: 800;
  line-height: 18px;
  text-align: center;
  box-sizing: border-box;
}

.backdrop { position: fixed; inset: 0; background: transparent; }

.panel {
  position: absolute;
  top: 58px;
  right: 12px;
  width: 300px;
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: 18px;
  box-shadow: 0 10px 30px rgba(120, 90, 30, .18);
}
.panel-list { max-height: calc(60vh - 50px); overflow-y: auto; }
.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid var(--line);
  font-size: 15px;
}
.read-all {
  border: none;
  background: none;
  color: var(--gold-deep);
  font-family: inherit;   /* 본문과 같은 글씨체(paint: 손글씨) */
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}
.read-all:disabled { color: var(--mute); cursor: default; }
/* paint 테마의 전역 버튼 손그림 테두리(::before) 제거 — 텍스트만 */
:root[data-theme="paint"] .read-all::before { content: none; }

.item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 13px 16px;
  border-bottom: 1px solid var(--line);
  cursor: pointer;
}
.item:last-child { border-bottom: none; }
.item.unread { background: #FFF9EF; }
.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  margin-top: 6px;
  flex: 0 0 7px;
  background: transparent;
}
.dot.on { background: var(--expense); }
.item-body { flex: 1; }
.msg { font-size: 14px; font-weight: 700; color: var(--ink); }
.item.unread .msg { color: var(--ink); }
.item:not(.unread) .msg { color: var(--ink-2); font-weight: 600; }
.meta { margin-top: 4px; font-size: 12px; font-weight: 600; color: var(--mute); }

.empty { padding: 28px 16px; text-align: center; color: var(--mute); font-weight: 600; font-size: 13px; }

/* ── paint(그림판) 테마 ── */
/* 종 테두리는 전역 button::before(wobble)가 그려줌 → 원형 모서리만 맞춤 */
:root[data-theme="paint"] .bell { border-radius: 50%; }
:root[data-theme="paint"] .bell::before { border-radius: 50% !important; }
/* 패널 테두리: .paint-box(::before)가 손그림 wobble 로 그림 → border-radius 만 맞춤 */
:root[data-theme="paint"] .panel { border-radius: 6px; }
/* 구분선: 직선 border 숨기고 .paint-hline-b 손그림 가로선만 보이게 */
:root[data-theme="paint"] .panel-head,
:root[data-theme="paint"] .item { border-bottom-color: transparent; --hand-line-w: 1px; }
:root[data-theme="paint"] .item.unread { background: #F2F2F2; }
</style>
