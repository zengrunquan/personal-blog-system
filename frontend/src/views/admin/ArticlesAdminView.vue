<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Download, Edit3, Trash2 } from 'lucide-vue-next'
import { adminApi } from '@/api/blog'
import type { Article, PageResult } from '@/api/types'
import { AppError } from '@/api/types'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import AppPagination from '@/components/common/AppPagination.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import PageState from '@/components/common/PageState.vue'

const result = ref<PageResult<Article> | null>(null)
const page = ref(1)
const loading = ref(true)
const error = ref('')
const selected = ref<number[]>([])
const deleteTarget = ref<Article | null>(null)
const batchConfirm = ref(false)
const busy = ref(false)
const exportUrl = computed(
  () =>
    `${import.meta.env.DEV ? '' : import.meta.env.BASE_URL.replace(/\/$/, '')}/api/admin/articles/export`,
)
async function load(): Promise<void> {
  loading.value = true
  error.value = ''
  try {
    result.value = await adminApi.articles({ page: page.value, pageSize: 15 })
    selected.value = []
  } catch (caught) {
    error.value = caught instanceof AppError ? caught.message : '文章列表加载失败'
    console.error(`[DEBUG] ${new Date().toISOString()} [ArticlesAdminView.load] 加载失败`, caught)
  } finally {
    loading.value = false
  }
}
async function changePage(next: number): Promise<void> {
  page.value = next
  await load()
}
function toggleAll(event: Event): void {
  const checked = (event.target as HTMLInputElement).checked
  selected.value = checked ? (result.value?.items.flatMap((a) => (a.id ? [a.id] : [])) ?? []) : []
}
async function remove(ids: number[]): Promise<void> {
  if (ids.length === 0) return
  busy.value = true
  try {
    const firstId = ids[0]
    if (ids.length === 1) await adminApi.deleteArticle(firstId)
    else await adminApi.batchDeleteArticles(ids)
    deleteTarget.value = null
    batchConfirm.value = false
    await load()
  } catch (caught) {
    error.value = caught instanceof AppError ? caught.message : '文章删除失败'
    console.error(`[DEBUG] ${new Date().toISOString()} [ArticlesAdminView.remove] 删除失败`, caught)
  } finally {
    busy.value = false
  }
}
onMounted(() => void load())
</script>

<template>
  <AdminLayout title="文章管理" description="集中检查状态、编辑内容并导出数据。">
    <template #actions
      ><div class="header-actions">
        <button
          class="button button--danger"
          type="button"
          :disabled="selected.length === 0"
          @click="batchConfirm = true"
        >
          <Trash2 :size="16" />批量删除 ({{ selected.length }})</button
        ><a class="button" :href="exportUrl"><Download :size="16" />导出 CSV</a>
      </div></template
    >
    <PageState v-if="loading" type="loading" />
    <PageState v-else-if="error && !result" type="error" :message="error" @retry="load" />
    <template v-else-if="result">
      <p v-if="error" class="form-error page-error">{{ error }}</p>
      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>
                <input
                  type="checkbox"
                  aria-label="选择本页全部文章"
                  :checked="selected.length === result.items.length && result.items.length > 0"
                  @change="toggleAll"
                />
              </th>
              <th>标题</th>
              <th>作者</th>
              <th>分类</th>
              <th>状态</th>
              <th>阅读</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="article in result.items" :key="article.id">
              <td data-label="选择">
                <input
                  v-if="article.id"
                  v-model="selected"
                  type="checkbox"
                  :value="article.id"
                  :aria-label="`选择 ${article.title}`"
                />
              </td>
              <td data-label="标题">
                <strong>{{ article.title }}</strong
                ><small>{{
                  article.createTime ? new Date(article.createTime).toLocaleDateString('zh-CN') : ''
                }}</small>
              </td>
              <td data-label="作者">{{ article.authorNickname || article.authorName || '—' }}</td>
              <td data-label="分类">{{ article.categoryName || '—' }}</td>
              <td data-label="状态">
                <span
                  class="status-badge"
                  :class="article.status === 1 ? 'status-badge--live' : 'status-badge--draft'"
                  >{{ article.status === 1 ? '发布' : '草稿' }}</span
                >
              </td>
              <td data-label="阅读">{{ article.viewCount ?? 0 }}</td>
              <td data-label="操作">
                <div class="table-actions">
                  <RouterLink
                    class="button button--small"
                    :to="{ name: 'editor-edit', params: { id: article.id } }"
                    ><Edit3 :size="15" />编辑</RouterLink
                  ><button
                    class="button button--small button--danger"
                    type="button"
                    @click="deleteTarget = article"
                  >
                    <Trash2 :size="15" />删除
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <AppPagination :page="result.page" :total-pages="result.totalPages" @change="changePage" />
    </template>
    <ConfirmDialog
      :open="deleteTarget !== null"
      title="删除这篇文章？"
      :message="`《${deleteTarget?.title ?? ''}》及其评论将被删除。`"
      :busy="busy"
      @cancel="deleteTarget = null"
      @confirm="deleteTarget?.id && remove([deleteTarget.id])"
    />
    <ConfirmDialog
      :open="batchConfirm"
      title="批量删除文章？"
      :message="`已选择 ${selected.length} 篇文章，删除后无法恢复。`"
      :busy="busy"
      @cancel="batchConfirm = false"
      @confirm="remove(selected)"
    />
  </AdminLayout>
</template>

<style scoped>
.header-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: var(--space-2);
}
td strong,
td small {
  display: block;
}
td strong {
  max-width: 340px;
  overflow: hidden;
  font-family: var(--serif);
  text-overflow: ellipsis;
  white-space: nowrap;
}
td small {
  color: var(--ink-faint);
  font-size: 0.72rem;
}
.page-error {
  margin-bottom: var(--space-3);
}
@media (max-width: 620px) {
  .header-actions {
    width: 100%;
  }
  .header-actions .button {
    flex: 1;
  }
}
</style>
