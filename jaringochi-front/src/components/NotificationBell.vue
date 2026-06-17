<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { listNotifications, getUnreadCount, markRead, markAllRead } from '@/api/notification'

const route = useRoute()

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

// 한 건 클릭 -> 안 읽음이면 읽음 처리 + 배지 감소
async function onItemClick(n) {
  if (n.isRead === 0) {
    await markRead(n.id)
    n.isRead = 1
    count.value = Math.max(0, count.value - 1)
  }
}

async function onReadAll() {
  await markAllRead()
  items.value.forEach((n) => { n.isRead = 1 })
  count.value = 0
}

// threshold -> 문구 조립 (DB엔 문구 없음, 프론트가 조립 - 관심사 분리)
function message(n) {
  const t = n.threshold
  if (t >= 125) return `이번 주 예산을 ${t - 100}% 초과했어요 😱`
  if (t >= 100) return `이번 주 예산을 다 썼어요!`
  return `이번 주 예산의 ${t}%를 썼어요 🐟`
}

// "2026-06-14T19:00:00" -> "6/14 19:00"
function fmtDate(s) {
  const d = new Date(s)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getMonth() + 1}/${d.getDate()} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

onMounted(loadCount)
watch(() => route.path, loadCount)   // 로그인 후 화면 이동 등에서 배지 갱신
</script>

<template>
  <div v-if="visible" class="noti">
    <!-- 종 + 배지 -->
    <button class="bell" @click="toggle" aria-label="알림">
      🔔
      <span v-if="count > 0" class="badge">{{ count > 99 ? '99+' : count }}</span>
    </button>

    <!-- 드롭다운 -->
    <template v-if="open">
      <div class="backdrop" @click="open = false"></div>
      <div class="panel">
        <div class="panel-head">
          <b>알림</b>
          <button class="read-all" @click="onReadAll" :disabled="count === 0">전체 읽음</button>
        </div>

        <div v-if="loading" class="empty">불러오는 중…</div>
        <template v-else>
          <div
            v-for="n in items"
            :key="n.id"
            class="item"
            :class="{ unread: n.isRead === 0 }"
            @click="onItemClick(n)"
          >
            <span class="dot" :class="{ on: n.isRead === 0 }"></span>
            <div class="item-body">
              <div class="msg">{{ message(n) }}</div>
              <div class="meta">
                {{ fmtDate(n.createdAt) }}
                <span v-if="n.ratio != null"> · {{ Number(n.ratio).toFixed(0) }}%</span>
              </div>
            </div>
          </div>
          <div v-if="items.length === 0" class="empty">알림이 없어요 🐟</div>
        </template>
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
  max-height: 60vh;
  overflow-y: auto;
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: 18px;
  box-shadow: 0 10px 30px rgba(120, 90, 30, .18);
}
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
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}
.read-all:disabled { color: var(--mute); cursor: default; }

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
</style>
