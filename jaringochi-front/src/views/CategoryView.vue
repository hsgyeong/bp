<script setup>
import { ref, onMounted, watch } from 'vue'
import { listCategories, createCategory, deleteCategory } from '@/api/category'
import { useTheme } from '@/composables/useTheme'
import { categoryTablerIcon } from '@/utils/categoryIcon'
import AppModal from '@/components/AppModal.vue'

const { theme } = useTheme()   // paint 테마: 카테고리/삭제 이모지 → Tabler 라인 아이콘

const addOpen = ref(false)        // 분류 추가 모달
const deleteTarget = ref(null)    // 삭제 확인 모달 대상 카테고리(없으면 닫힘)

const type = ref(2)          // 1=수입, 2=지출 (기본은 지출 탭)
const categories = ref([])   // 화면에 뿌릴 목록 (처음엔 빈 배열)

// 서버에서 현재 type의 목록을 가져와 categories에 담는다
async function load() {
  const res = await listCategories(type.value)
  categories.value = res.data.data   // 봉투 두 겹! axios의 .data + 우리 봉투의 .data
}

onMounted(load)     // 화면이 처음 뜰 때 한 번 호출
watch(type, load)   // 탭(type)이 바뀔 때마다 다시 호출

// 추가: 모달에서 입력받은 이름으로 만들고 → 목록 새로고침
async function onAddConfirm(name) {
  addOpen.value = false
  await createCategory({ name, type: type.value, icon: '📦' })
  await load()
}

// 삭제(소프트): 모달 확인 후 → 목록 새로고침
async function onDeleteConfirm() {
  const target = deleteTarget.value
  deleteTarget.value = null
  if (!target) return
  await deleteCategory(target.id)
  await load()
}
</script>

<template>
  <div class="cat-view">
    <h1 class="title">분류 관리</h1>

    <!-- 수입/지출 세그먼트 탭 -->
    <div class="seg">
      <div class="s income" :class="{ on: type === 1 }" @click="type = 1">수입 분류</div>
      <div class="s expense" :class="{ on: type === 2 }" @click="type = 2">지출 분류</div>
    </div>

    <!-- 목록 -->
    <div class="card list">
      <div class="menu" :class="{ 'paint-hline-b': i < categories.length - 1 }" v-for="(c, i) in categories" :key="c.id">
        <span class="cat-ic" :style="{ background: type === 1 ? 'var(--income-soft)' : 'var(--expense-soft)' }">
          <i v-if="theme === 'paint'" class="ti" :class="categoryTablerIcon(c.name, type)" aria-hidden="true"></i>
          <template v-else>{{ c.icon }}</template>
        </span>
        {{ c.name }}
        <span class="arr" @click="deleteTarget = c">
          <i v-if="theme === 'paint'" class="ti ti-trash" aria-hidden="true"></i>
          <template v-else>🗑</template>
        </span>
      </div>
      <div v-if="categories.length === 0" class="empty">카테고리가 없어요</div>
    </div>

    <button class="btn-ghost" @click="addOpen = true">＋ 분류 추가</button>

    <!-- 분류 추가 (입력형 모달) -->
    <AppModal
      v-if="addOpen"
      title="분류 추가"
      with-input
      placeholder="카테고리명을 입력해주세요."
      confirm-text="추가"
      @confirm="onAddConfirm"
      @cancel="addOpen = false"
    />

    <!-- 분류 삭제 (확인형 모달) -->
    <AppModal
      v-if="deleteTarget"
      title="분류 삭제"
      :message="`'${deleteTarget.name}' 분류를 삭제할까요?`"
      confirm-text="삭제"
      danger
      @confirm="onDeleteConfirm"
      @cancel="deleteTarget = null"
    />
  </div>
</template>

<style scoped>
.cat-view { padding: 16px 20px 24px; }
.title { font-size: 18px; margin-bottom: 14px; }

.seg { display: flex; background: var(--cream-2); border-radius: 14px; padding: 4px; gap: 4px; margin-bottom: 14px; }
.seg .s { flex: 1; text-align: center; padding: 9px 0; font-size: 13px; font-weight: 700; color: var(--ink-2); border-radius: 11px; cursor: pointer; }
.seg .s.on { background: #fff; color: var(--gold-deep); box-shadow: 0 2px 6px rgba(120,90,30,.1); }

.list { padding: 4px 4px; margin-bottom: 14px; }
.menu { display: flex; align-items: center; gap: 13px; padding: 15px 12px; border-bottom: 1px solid var(--line); font-size: 15px; font-weight: 700; }
.menu:last-child { border-bottom: none; }
.cat-ic { width: 38px; height: 38px; border-radius: 12px; display: grid; place-items: center; font-size: 18px; flex: 0 0 38px; }
.arr { margin-left: auto; color: var(--mute); font-size: 18px; cursor: pointer; }

.btn-ghost { width: 100%; border: 1.5px solid var(--gold-soft); background: #fff; border-radius: 16px; padding: 15px; font-size: 15px; font-weight: 700; color: var(--gold-deep); font-family: inherit; cursor: pointer; }
.empty { padding: 24px; text-align: center; color: var(--mute); font-weight: 600; }

/* ── paint(그림판) 테마 보정 ── */
/* 카테고리 아이콘: 회색 배경 박스 제거하고 라인 아이콘만 */
:root[data-theme="paint"] .cat-ic { background: transparent !important; border-radius: 0; font-size: 22px; }
/* 행 구분선: .paint-hline-b 가 손그림 선을 그림 → 직선 border 숨김 */
:root[data-theme="paint"] .menu { border-bottom-color: transparent; }
/* 수입/지출 분류 토글: 거래 등록 화면과 동일한 색 규칙.
   비선택은 흰 배경 + 해당 색 글씨, 선택 시 수입=#bdcad9/글씨 #6FA3DC, 지출=#e0c1b8/글씨 #E08A72 */
:root[data-theme="paint"] .seg .s.income { color: var(--income); }
:root[data-theme="paint"] .seg .s.expense { color: var(--expense); }
:root[data-theme="paint"] .seg .s.income.on { color: #6FA3DC; }
:root[data-theme="paint"] .seg .s.income.on::before { background: #bdcad9; }
:root[data-theme="paint"] .seg .s.expense.on { color: #E08A72; }
:root[data-theme="paint"] .seg .s.expense.on::before { background: #e0c1b8; }
</style>