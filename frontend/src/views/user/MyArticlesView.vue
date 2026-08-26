<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Edit3, Plus, Trash2 } from 'lucide-vue-next'
import { meApi, articleApi } from '@/api/blog'
import type { Article, PageResult } from '@/api/types'
import { AppError } from '@/api/types'
import AppPagination from '@/components/common/AppPagination.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import PageState from '@/components/common/PageState.vue'
import UserTabs from '@/components/user/UserTabs.vue'

const result = ref<PageResult<Article> | null>(null)
const page = ref(1)
const loading = ref(true)
const error = ref('')
const deleteTarget = ref<Article | null>(null)
const deleting = ref(false)

async function load(): Promise<void> {
  loading.value = true
  error.value = ''
  try {
    result.value = await meApi.articles({ page: page.value, pageSize: 10 })
  } catch (caught) {
    error.value = caught instanceof AppError ? caught.message : '文章加载失败'
    console.error(`[DEBUG] ${new Date().toISOString()} [MyArticlesView.load] 加载失败`, caught)
  } finally {
    loading.value = false
  }
}

async function changePage(next: number): Promise<void> {
  page.value = next
  await load()
}

async function confirmDelete(): Promise<void> {
  if (!deleteTarget.value?.id) return
  deleting.value = true
  try {
    await articleApi.delete(deleteTarget.value.id)
    deleteTarget.value = null
    await load()
  } catch (caught) {
    error.value = caught instanceof AppError ? caught.message : '删除失败'
    console.error(`[DEBUG] ${new Date().toISOString()} [MyArticlesView.delete] 删除失败`, caught)
  } finally {
    deleting.value = false
  }
}

onMounted(() => void load())
</script>

<template>
  <section class="page-container page-section">
    <UserTabs />
    <div class="my-heading">
      <div>
        <p class="page-kicker">Writing desk</p>
        <h1 class="page-title">我的文章</h1>
      </div>
      <RouterLink class="button button--primary" :to="{ name: 'editor-new' }"
        ><Plus :size="17" />写新文章</RouterLink
      >
    </div>
    <PageState v-if="loading" type="loading" />
    <PageState v-else-if="error" type="error" :message="error" @retry="load" />
    <PageState v-else-if="!result?.items.length" type="empty" message="开始写下第一篇文章吧。"
      ><RouterLink class="button button--primary" :to="{ name: 'editor-new' }"
        >创建文章</RouterLink
      ></PageState
    >
    <template v-else>
      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>标题</th>
              <th>分类</th>
              <th>状态</th>
              <th>更新时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="article in result.items" :key="article.id">
              <td data-label="标题">
                <strong>{{ article.title }}</strong>
              </td>
              <td data-label="分类">{{ article.categoryName || '—' }}</td>
              <td data-label="状态">
                <span
                  class="status-badge"
                  :class="article.status === 1 ? 'status-badge--live' : 'status-badge--draft'"
                  >{{ article.status === 1 ? '已发布' : '草稿' }}</span
                >
              </td>
              <td data-label="更新时间">
                {{
                  new Date(article.updateTime || article.createTime || '').toLocaleDateString(
                    'zh-CN',
                  )
                }}
              </td>
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
      :message="`《${deleteTarget?.title ?? ''}》及其评论将一并删除，且无法恢复。`"
      :busy="deleting"
      @cancel="deleteTarget = null"
      @confirm="confirmDelete"
    />
  </section>
</template>

<style scoped>
.my-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--space-4);
  margin-bottom: var(--space-5);
  padding-bottom: var(--space-5);
  border-bottom: 1px solid var(--line);
}
.data-table strong {
  font-family: var(--serif);
  font-size: 1rem;
}
@media (max-width: 560px) {
  .my-heading {
    align-items: stretch;
    flex-direction: column;
  }
  .my-heading .button {
    width: 100%;
  }
}
</style>
