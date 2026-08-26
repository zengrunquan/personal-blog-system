<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Pencil, Plus, Trash2, X } from 'lucide-vue-next'
import { useRoute } from 'vue-router'
import { adminApi } from '@/api/blog'
import type { Category } from '@/api/types'
import { AppError } from '@/api/types'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import PageState from '@/components/common/PageState.vue'

const route = useRoute()
const categories = ref<Category[]>([])
const loading = ref(true)
const busy = ref(false)
const error = ref('')
const formOpen = ref(false)
const editingId = ref<number | null>(null)
const deleteTarget = ref<Category | null>(null)
const routeIntentApplied = ref(false)
const form = reactive({ name: '', description: '', sortOrder: 0 })
function resetForm(): void {
  form.name = ''
  form.description = ''
  form.sortOrder = 0
  editingId.value = null
  formOpen.value = false
}
function edit(category: Category): void {
  editingId.value = category.id
  form.name = category.name
  form.description = category.description ?? ''
  form.sortOrder = category.sortOrder ?? 0
  formOpen.value = true
}
function applyLegacyRouteIntent(): void {
  if (route.query.create === '1') {
    formOpen.value = true
    return
  }
  const rawEditId = Array.isArray(route.query.edit) ? route.query.edit[0] : route.query.edit
  const editId = Number(rawEditId)
  if (!Number.isInteger(editId) || editId <= 0) return
  const category = categories.value.find((item) => item.id === editId)
  if (category) edit(category)
}
async function load(): Promise<void> {
  loading.value = true
  error.value = ''
  try {
    categories.value = await adminApi.categories()
    if (!routeIntentApplied.value) {
      // 旧分类表单地址只在首次进入时恢复意图，保存后的刷新不能再次打开已完成的表单。
      applyLegacyRouteIntent()
      routeIntentApplied.value = true
    }
  } catch (caught) {
    error.value = caught instanceof AppError ? caught.message : '分类加载失败'
    console.error(`[DEBUG] ${new Date().toISOString()} [CategoriesView.load] 加载失败`, caught)
  } finally {
    loading.value = false
  }
}
async function submit(): Promise<void> {
  if (!form.name.trim()) {
    error.value = '分类名称不能为空'
    return
  }
  busy.value = true
  error.value = ''
  try {
    const payload = {
      name: form.name.trim(),
      description: form.description.trim(),
      sortOrder: form.sortOrder || 0,
    }
    if (editingId.value) await adminApi.updateCategory(editingId.value, payload)
    else await adminApi.createCategory(payload)
    resetForm()
    await load()
  } catch (caught) {
    error.value = caught instanceof AppError ? caught.message : '分类保存失败'
    console.error(`[DEBUG] ${new Date().toISOString()} [CategoriesView.submit] 保存失败`, caught)
  } finally {
    busy.value = false
  }
}
async function confirmDelete(): Promise<void> {
  if (!deleteTarget.value) return
  busy.value = true
  try {
    await adminApi.deleteCategory(deleteTarget.value.id)
    deleteTarget.value = null
    await load()
  } catch (caught) {
    error.value = caught instanceof AppError ? caught.message : '分类删除失败'
    console.error(`[DEBUG] ${new Date().toISOString()} [CategoriesView.delete] 删除失败`, caught)
  } finally {
    busy.value = false
  }
}
onMounted(() => void load())
</script>

<template>
  <AdminLayout title="分类管理" description="维护文章归档结构与显示顺序。">
    <template #actions
      ><button class="button button--primary" type="button" @click="formOpen = !formOpen">
        <X v-if="formOpen" :size="16" /><Plus v-else :size="16" />{{
          formOpen ? '收起表单' : '新增分类'
        }}
      </button></template
    >
    <form v-if="formOpen" class="category-form" @submit.prevent="submit">
      <div class="form-field">
        <label for="name">分类名称</label
        ><input id="name" v-model="form.name" class="form-control" maxlength="50" required />
      </div>
      <div class="form-field">
        <label for="sortOrder">排序</label
        ><input
          id="sortOrder"
          v-model.number="form.sortOrder"
          class="form-control"
          type="number"
          min="0"
        />
      </div>
      <div class="form-field description">
        <label for="description">说明</label
        ><input id="description" v-model="form.description" class="form-control" maxlength="200" />
      </div>
      <div class="category-form-actions">
        <button class="button" type="button" @click="resetForm">取消</button
        ><button class="button button--primary" type="submit" :disabled="busy">
          {{ busy ? '保存中…' : editingId ? '更新分类' : '创建分类' }}
        </button>
      </div>
    </form>
    <p v-if="error" class="form-error page-error">{{ error }}</p>
    <PageState v-if="loading" type="loading" />
    <PageState v-else-if="!categories.length" type="empty" message="还没有分类，先创建一个。" />
    <div v-else class="category-grid">
      <article v-for="category in categories" :key="category.id">
        <span class="category-order">{{ String(category.sortOrder ?? 0).padStart(2, '0') }}</span>
        <div>
          <h2>{{ category.name }}</h2>
          <p>{{ category.description || '暂无说明' }}</p>
          <span>{{ category.articleCount ?? 0 }} 篇文章</span>
        </div>
        <div class="category-actions">
          <button type="button" aria-label="编辑分类" @click="edit(category)">
            <Pencil :size="17" /></button
          ><button type="button" aria-label="删除分类" @click="deleteTarget = category">
            <Trash2 :size="17" />
          </button>
        </div>
      </article>
    </div>
    <ConfirmDialog
      :open="deleteTarget !== null"
      title="删除这个分类？"
      :message="`“${deleteTarget?.name ?? ''}”分类下的文章也会被删除，请确认已经核对。`"
      :busy="busy"
      @cancel="deleteTarget = null"
      @confirm="confirmDelete"
    />
  </AdminLayout>
</template>

<style scoped>
.category-form {
  display: grid;
  grid-template-columns: 1.3fr 0.5fr 2fr auto;
  align-items: end;
  gap: var(--space-3);
  margin-bottom: var(--space-5);
  padding: var(--space-4);
  border: 1px solid var(--line);
  background: var(--paper-light);
}
.category-form-actions {
  display: flex;
  gap: var(--space-2);
}
.page-error {
  margin-bottom: var(--space-3);
}
.category-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-3);
}
.category-grid article {
  position: relative;
  min-height: 150px;
  display: grid;
  grid-template-columns: 50px 1fr auto;
  gap: var(--space-3);
  padding: var(--space-4);
  border: 1px solid var(--line);
  background: var(--paper-light);
}
.category-order {
  color: var(--vermilion);
  font-family: var(--serif);
  font-size: 1.7rem;
}
.category-grid h2,
.category-grid p {
  margin: 0;
}
.category-grid h2 {
  font-family: var(--serif);
  font-size: 1.25rem;
}
.category-grid p {
  color: var(--ink-soft);
  font-size: 0.85rem;
}
.category-grid div > span {
  color: var(--ink-faint);
  font-size: 0.76rem;
}
.category-actions {
  display: flex;
  gap: 0.25rem;
}
.category-actions button {
  align-self: start;
  display: grid;
  padding: 0.4rem;
  border: 0;
  color: var(--ink-faint);
  background: transparent;
  cursor: pointer;
}
.category-actions button:hover {
  color: var(--vermilion);
}
@media (max-width: 1050px) {
  .category-form {
    grid-template-columns: 1fr 1fr;
  }
  .description {
    grid-column: 1 / -1;
  }
  .category-form-actions {
    grid-column: 1 / -1;
    justify-content: flex-end;
  }
}
@media (max-width: 650px) {
  .category-grid {
    grid-template-columns: 1fr;
  }
  .category-form {
    grid-template-columns: 1fr;
  }
  .description,
  .category-form-actions {
    grid-column: auto;
  }
  .category-form-actions .button {
    flex: 1;
  }
}
</style>
