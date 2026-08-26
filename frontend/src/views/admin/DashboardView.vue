<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { FileText, FolderTree, Radio, Users } from 'lucide-vue-next'
import { adminApi, type DashboardData } from '@/api/blog'
import { AppError } from '@/api/types'
import PageState from '@/components/common/PageState.vue'
import AdminLayout from '@/components/layout/AdminLayout.vue'

const data = ref<DashboardData | null>(null)
const loading = ref(true)
const error = ref('')

async function load(): Promise<void> {
  loading.value = true
  error.value = ''
  try {
    data.value = await adminApi.dashboard()
  } catch (caught) {
    error.value = caught instanceof AppError ? caught.message : '后台概览加载失败'
    console.error(`[DEBUG] ${new Date().toISOString()} [DashboardView.load] 加载失败`, caught)
  } finally {
    loading.value = false
  }
}
onMounted(() => void load())
</script>

<template>
  <AdminLayout title="后台概览" description="站点内容、用户与在线状态的即时快照。">
    <PageState v-if="loading" type="loading" />
    <PageState v-else-if="error" type="error" :message="error" @retry="load" />
    <template v-else-if="data">
      <div class="metric-grid">
        <article>
          <Users :size="22" /><span>用户</span><strong>{{ data.stats.users }}</strong>
        </article>
        <article>
          <FileText :size="22" /><span>文章</span><strong>{{ data.stats.articles }}</strong>
        </article>
        <article>
          <FolderTree :size="22" /><span>分类</span><strong>{{ data.stats.categories }}</strong>
        </article>
        <article>
          <Radio :size="22" /><span>在线</span><strong>{{ data.stats.online }}</strong>
        </article>
      </div>
      <section class="recent-panel">
        <div class="section-heading">
          <h2>最近文章</h2>
          <RouterLink class="text-link" :to="{ name: 'admin-articles' }">管理全部</RouterLink>
        </div>
        <div class="recent-list">
          <RouterLink
            v-for="article in data.recentArticles"
            :key="article.id"
            :to="{ name: 'article-detail', params: { id: article.id } }"
          >
            <span
              class="status-badge"
              :class="article.status === 1 ? 'status-badge--live' : 'status-badge--draft'"
              >{{ article.status === 1 ? '发布' : '草稿' }}</span
            >
            <strong>{{ article.title }}</strong
            ><span>{{ article.authorNickname || article.authorName || '—' }}</span
            ><time>{{ new Date(article.createTime || '').toLocaleDateString('zh-CN') }}</time>
          </RouterLink>
        </div>
      </section>
    </template>
  </AdminLayout>
</template>

<style scoped>
.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-3);
}
.metric-grid article {
  position: relative;
  min-height: 145px;
  display: grid;
  align-content: space-between;
  padding: var(--space-4);
  border: 1px solid var(--line);
  background: var(--paper-light);
}
.metric-grid svg {
  color: var(--vermilion);
}
.metric-grid span {
  color: var(--ink-soft);
  font-size: 0.8rem;
}
.metric-grid strong {
  font-family: var(--serif);
  font-size: 2.4rem;
  font-weight: 400;
  line-height: 1;
}
.recent-panel {
  margin-top: var(--space-6);
}
.recent-list a {
  display: grid;
  grid-template-columns: 70px 1fr 150px 110px;
  align-items: center;
  gap: var(--space-3);
  padding: 0.9rem 0;
  border-bottom: 1px solid var(--line-soft);
  font-size: 0.86rem;
}
.recent-list a:hover strong {
  color: var(--vermilion);
}
.recent-list strong {
  font-family: var(--serif);
  font-size: 1rem;
}
.recent-list a > span:not(.status-badge),
.recent-list time {
  color: var(--ink-faint);
}
@media (max-width: 950px) {
  .metric-grid {
    grid-template-columns: 1fr 1fr;
  }
  .recent-list a {
    grid-template-columns: 65px 1fr;
  }
  .recent-list a > span:not(.status-badge),
  .recent-list time {
    display: none;
  }
}
</style>
