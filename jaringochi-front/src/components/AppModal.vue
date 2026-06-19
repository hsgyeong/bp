<!--
  공용 모달 — 브라우저 기본 prompt()/confirm() 대신 앱 톤(그림판 포함)에 맞춘 다이얼로그.
  - 확인형:  <AppModal title="삭제" message="삭제할까요?" confirm-text="삭제" danger
               @confirm="..." @cancel="..." />
  - 입력형:  <AppModal title="분류 추가" with-input placeholder="..."
               confirm-text="추가" @confirm="name => ..." @cancel="..." />
  부모가 v-if 로 열고 닫는다. confirm 이벤트는 입력형이면 입력값을 함께 전달.
-->
<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'

const props = defineProps({
  title: { type: String, default: '' },
  message: { type: String, default: '' },
  confirmText: { type: String, default: '확인' },
  cancelText: { type: String, default: '취소' },
  withInput: { type: Boolean, default: false },
  placeholder: { type: String, default: '' },
  danger: { type: Boolean, default: false },   // 삭제·탈퇴 등 파괴적 동작이면 확인 버튼 강조
  hideCancel: { type: Boolean, default: false },  // 알림(단일 버튼)용 — 취소 버튼 숨김
})

const emit = defineEmits(['confirm', 'cancel'])

const text = ref('')
const inputRef = ref(null)

function onConfirm() {
  if (props.withInput) {
    if (!text.value.trim()) return   // 빈 입력은 무시
    emit('confirm', text.value.trim())
  } else {
    emit('confirm')
  }
}

function onKey(e) {
  if (e.key === 'Escape') emit('cancel')
}

onMounted(() => {
  window.addEventListener('keydown', onKey)
  if (props.withInput) inputRef.value?.focus()
})
onBeforeUnmount(() => window.removeEventListener('keydown', onKey))
</script>

<template>
  <div class="modal-backdrop" @click.self="emit('cancel')">
    <div class="modal-card" role="dialog" aria-modal="true">
      <h3 v-if="title" class="modal-title">{{ title }}</h3>
      <p v-if="message" class="modal-message">{{ message }}</p>

      <span v-if="withInput" class="paint-field modal-field">
        <input
          ref="inputRef"
          v-model="text"
          type="text"
          :placeholder="placeholder"
          @keyup.enter="onConfirm"
        />
      </span>

      <div class="modal-actions" :class="{ single: hideCancel }">
        <button v-if="!hideCancel" type="button" class="modal-btn cancel" @click="emit('cancel')">
          {{ cancelText }}
        </button>
        <button
          type="button"
          class="modal-btn confirm"
          :class="{ danger }"
          :disabled="withInput && !text.trim()"
          @click="onConfirm"
        >
          {{ confirmText }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(0, 0, 0, 0.42);
}

.modal-card {
  width: 100%;
  max-width: 340px;
  background: var(--card);
  border-radius: 20px;
  padding: 22px 20px 18px;
  box-shadow: 0 18px 44px rgba(60, 45, 15, 0.28);
}

.modal-title {
  font-size: 17px;
  font-weight: 900;
  color: var(--ink);
  margin-bottom: 8px;
}

.modal-message {
  font-size: 14px;
  font-weight: 600;
  line-height: 1.5;
  color: var(--ink-2);
  margin-bottom: 4px;
  overflow-wrap: anywhere;
}

.modal-field {
  display: block;
  margin-top: 14px;
}

.modal-field input {
  width: 100%;
  height: 48px;
  border: 1.5px solid var(--line);
  border-radius: 14px;
  padding: 0 14px;
  background: var(--card);
  color: var(--ink);
  font: inherit;
  font-size: 15px;
  font-weight: 700;
  outline: none;
  box-sizing: border-box;
}

.modal-field input:focus {
  border-color: var(--gold);
}

.modal-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-top: 20px;
}

.modal-actions.single {
  grid-template-columns: 1fr;
}

.modal-btn {
  height: 48px;
  border-radius: 14px;
  font: inherit;
  font-size: 15px;
  font-weight: 800;
  cursor: pointer;
}

.modal-btn.cancel {
  border: 1.5px solid var(--line);
  background: var(--card);
  color: var(--ink-2);
}

.modal-btn.confirm {
  border: 0;
  background: var(--gold);
  color: #fff;
}

.modal-btn.confirm.danger {
  background: var(--expense);
}

.modal-btn:disabled {
  opacity: 0.5;
  cursor: default;
}

/* ── paint(그림판) 테마 ── */
/* 카드 테두리는 전역 [class*="-card"] wobble 규칙이 그려줌. 입력칸은 .paint-field 가 처리.
   확인 버튼은 골드 대신 검은 채움(테두리는 전역 button wobble) */
:root[data-theme="paint"] .modal-btn.confirm {
  background: var(--ink);
}
</style>
