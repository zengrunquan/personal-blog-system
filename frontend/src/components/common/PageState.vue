<script setup lang="ts">
import { AlertCircle, Inbox, LoaderCircle } from 'lucide-vue-next'

withDefaults(
  defineProps<{
    type: 'loading' | 'empty' | 'error' | 'forbidden'
    title?: string
    message?: string
  }>(),
  { title: '', message: '' },
)

defineEmits<{ retry: [] }>()
</script>

<template>
  <div class="page-state" role="status">
    <LoaderCircle v-if="type === 'loading'" class="spinner" :size="30" />
    <Inbox v-else-if="type === 'empty'" :size="30" />
    <AlertCircle v-else :size="30" />
    <h2>
      {{
        title ||
        (type === 'loading' ? '正在整理内容' : type === 'empty' ? '这里还没有内容' : '加载没有成功')
      }}
    </h2>
    <p v-if="message">{{ message }}</p>
    <button
      v-if="type === 'error'"
      class="button button--small"
      type="button"
      @click="$emit('retry')"
    >
      重新加载
    </button>
    <slot />
  </div>
</template>

<style scoped>
.page-state {
  min-height: 260px;
  display: grid;
  place-items: center;
  align-content: center;
  gap: var(--space-3);
  padding: var(--space-6);
  border-block: 1px solid var(--line-soft);
  color: var(--ink-soft);
  text-align: center;
}

h2,
p {
  margin: 0;
}

h2 {
  color: var(--ink);
  font-family: var(--serif);
  font-size: 1.25rem;
}

.spinner {
  animation: spin 900ms linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
