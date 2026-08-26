<script setup lang="ts">
import { AlertTriangle, X } from 'lucide-vue-next'

withDefaults(
  defineProps<{
    open: boolean
    title: string
    message: string
    confirmLabel?: string
    busy?: boolean
  }>(),
  { confirmLabel: '确认删除', busy: false },
)
defineEmits<{ confirm: []; cancel: [] }>()
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="dialog-backdrop" @click.self="$emit('cancel')">
      <section class="dialog" role="alertdialog" aria-modal="true" :aria-label="title">
        <button class="dialog-close" type="button" aria-label="关闭" @click="$emit('cancel')">
          <X :size="20" />
        </button>
        <AlertTriangle class="dialog-icon" :size="30" />
        <h2>{{ title }}</h2>
        <p>{{ message }}</p>
        <div class="dialog-actions">
          <button class="button" type="button" :disabled="busy" @click="$emit('cancel')">
            取消
          </button>
          <button
            class="button button--primary"
            type="button"
            :disabled="busy"
            @click="$emit('confirm')"
          >
            {{ busy ? '处理中…' : confirmLabel }}
          </button>
        </div>
      </section>
    </div>
  </Teleport>
</template>

<style scoped>
.dialog-backdrop {
  position: fixed;
  z-index: 100;
  inset: 0;
  display: grid;
  place-items: center;
  padding: 1rem;
  background: rgb(21 21 20 / 48%);
}

.dialog {
  position: relative;
  width: min(100%, 440px);
  padding: 2rem;
  border: 1px solid var(--line);
  background: var(--paper-light);
  box-shadow: var(--shadow-paper);
  text-align: center;
}

.dialog-close {
  position: absolute;
  top: 0.75rem;
  right: 0.75rem;
  padding: 0.35rem;
  border: 0;
  background: transparent;
  cursor: pointer;
}

.dialog-icon {
  color: var(--vermilion);
}

h2 {
  margin: 0.8rem 0 0.45rem;
  font-family: var(--serif);
}

p {
  margin: 0;
  color: var(--ink-soft);
}

.dialog-actions {
  display: flex;
  justify-content: center;
  gap: 0.75rem;
  margin-top: 1.5rem;
}
</style>
