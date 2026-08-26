<script setup lang="ts">
import { ChevronLeft, ChevronRight } from 'lucide-vue-next'

defineProps<{ page: number; totalPages: number }>()
defineEmits<{ change: [page: number] }>()
</script>

<template>
  <nav v-if="totalPages > 1" class="pagination" aria-label="分页">
    <button
      class="page-button"
      type="button"
      :disabled="page <= 1"
      aria-label="上一页"
      @click="$emit('change', page - 1)"
    >
      <ChevronLeft :size="18" />
    </button>
    <span>第 {{ page }} / {{ totalPages }} 页</span>
    <button
      class="page-button"
      type="button"
      :disabled="page >= totalPages"
      aria-label="下一页"
      @click="$emit('change', page + 1)"
    >
      <ChevronRight :size="18" />
    </button>
  </nav>
</template>

<style scoped>
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-4);
  padding-top: var(--space-6);
  color: var(--ink-soft);
  font-size: 0.9rem;
}

.page-button {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border: 1px solid var(--line);
  background: transparent;
  cursor: pointer;
}

.page-button:hover:not(:disabled) {
  color: var(--paper-light);
  border-color: var(--vermilion);
  background: var(--vermilion);
}

.page-button:disabled {
  cursor: not-allowed;
  opacity: 0.4;
}
</style>
