<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Search, X } from 'lucide-vue-next'
import { useRoute, useRouter } from 'vue-router'
import { publicApi } from '@/api/blog'
import type { Article, Category, PageResult } from '@/api/types'
import { AppError } from '@/api/types'
import ArticleListItem from '@/components/article/ArticleListItem.vue'
import AppPagination from '@/components/common/AppPagination.vue'
import PageState from '@/components/common/PageState.vue'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const error = ref('')
const articles = ref<PageResult<Article> | null>(null)
const categories = ref<Category[]>([])
const searchText = ref(typeof route.query.q === 'string' ? route.query.q : '')

const page = computed(() => Math.max(Number(route.query.page) || 1, 1))
const category = computed(() => (route.query.category ? Number(route.query.category) : undefined))
const keyword = computed(() => (typeof route.query.q === 'string' ? route.query.q : undefined))

async function load(): Promise<void> {
  loading.value = true
  error.value = ''
  try {
    const data = await publicApi.articles({
      page: page.value,
      pageSize: 8,
      q: keyword.value,
      category: category.value,
    })
    articles.value = data.articles
    categories.value = data.categories
  } catch (caught) {
    error.value = caught instanceof AppError ? caught.message : '文章列表加载失败'
    console.error(`[DEBUG] ${new Date().toISOString()} [ArticlesView.load] 加载失败`, caught)
  } finally {
    loading.value = false
  }
}

async function submitSearch(): Promise<void> {
  const q = searchText.value.trim()
  await router.push({
    name: 'articles',
    query: { ...(category.value ? { category: category.value } : {}), ...(q ? { q } : {}) },
  })
}

async function clearSearch(): Promise<void> {
  searchText.value = ''
  await submitSearch()
}

async function selectCategory(id?: number): Promise<void> {
  await router.push({
    name: 'articles',
    query: { ...(keyword.value ? { q: keyword.value } : {}), ...(id ? { category: id } : {}) },
  })
}

async function changePage(next: number): Promise<void> {
  await router.push({ query: { ...route.query, page: next === 1 ? undefined : String(next) } })
}

watch(
  () => route.fullPath,
  () => {
    searchText.value = keyword.value ?? ''
    void load()
  },
  { immediate: true },
)
</script>

<template>
  <section class="page-container page-section">
    <header class="article-index-header">
      <div>
        <p class="page-kicker">Archive</p>
        <h1 class="page-title">文章</h1>
        <p class="page-lead">按主题慢慢翻阅技术记录、读书笔记与生活随笔。</p>
      </div>
      <form class="index-search" role="search" @submit.prevent="submitSearch">
        <Search :size="19" />
        <input
          v-model="searchText"
          type="search"
          placeholder="标题关键词"
          aria-label="文章关键词"
        />
        <button v-if="searchText" type="button" aria-label="清空搜索" @click="clearSearch">
          <X :size="17" />
        </button>
      </form>
    </header>

    <nav class="category-filter" aria-label="分类筛选">
      <button type="button" :class="{ active: !category }" @click="selectCategory()">全部</button>
      <button
        v-for="item in categories"
        :key="item.id"
        type="button"
        :class="{ active: category === item.id }"
        @click="selectCategory(item.id)"
      >
        {{ item.name }} <span>{{ item.articleCount ?? 0 }}</span>
      </button>
    </nav>

    <PageState v-if="loading" type="loading" />
    <PageState v-else-if="error" type="error" :message="error" @retry="load" />
    <PageState
      v-else-if="!articles?.items.length"
      type="empty"
      message="换一个关键词或分类试试。"
    />
    <div v-else class="article-index-list">
      <ArticleListItem v-for="article in articles.items" :key="article.id" :article="article" />
      <AppPagination
        :page="articles.page"
        :total-pages="articles.totalPages"
        @change="changePage"
      />
    </div>
  </section>
</template>

<style scoped>
.article-index-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--space-6);
  padding-bottom: var(--space-6);
  border-bottom: 1px solid var(--line);
}

.index-search {
  width: min(100%, 330px);
  display: flex;
  align-items: center;
  gap: 0.65rem;
  padding: 0.65rem 0;
  border-bottom: 1px solid var(--ink-soft);
}

.index-search input {
  min-width: 0;
  flex: 1;
  border: 0;
  outline: 0;
  background: transparent;
}

.index-search button {
  display: grid;
  padding: 0.25rem;
  border: 0;
  background: transparent;
  cursor: pointer;
}

.category-filter {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem 1.6rem;
  padding: var(--space-5) 0;
  border-bottom: 1px solid var(--line-soft);
}

.category-filter button {
  padding: 0.25rem 0;
  border: 0;
  border-bottom: 1px solid transparent;
  color: var(--ink-soft);
  background: transparent;
  cursor: pointer;
}

.category-filter button.active,
.category-filter button:hover {
  color: var(--vermilion);
  border-bottom-color: var(--vermilion);
}

.category-filter span {
  margin-left: 0.2rem;
  color: var(--ink-faint);
  font-size: 0.72rem;
}

.article-index-list {
  max-width: 980px;
}

@media (max-width: 700px) {
  .article-index-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .index-search {
    width: 100%;
  }

  .category-filter {
    flex-wrap: nowrap;
    overflow-x: auto;
  }

  .category-filter button {
    flex: 0 0 auto;
  }
}
</style>
