<script setup lang="ts">
import { Bookmark, Clock3, MessageCircle } from 'lucide-vue-next'
import { computed } from 'vue'
import type { Article } from '@/api/types'

const props = defineProps<{ article: Article }>()

const date = computed(() => {
  const value = props.article.createTime ? new Date(props.article.createTime) : new Date()
  return {
    day: String(value.getDate()).padStart(2, '0'),
    monthYear: `${value.getMonth() + 1}月 ${value.getFullYear()}`,
    full: value.toLocaleDateString('zh-CN'),
  }
})
</script>

<template>
  <article class="article-row">
    <time class="article-date" :datetime="article.createTime">
      <strong>{{ date.day }}</strong>
      <span>{{ date.monthYear }}</span>
    </time>
    <div class="article-copy">
      <span class="article-category">{{ article.categoryName || '未分类' }}</span>
      <h3>
        <RouterLink :to="{ name: 'article-detail', params: { id: article.id } }">{{
          article.title
        }}</RouterLink>
      </h3>
      <p>{{ article.summary || '作者还没有为这篇文章写摘要。' }}</p>
      <div class="article-meta">
        <span><Clock3 :size="15" />{{ date.full }}</span>
        <span><Bookmark :size="15" />{{ article.categoryName || '未分类' }}</span>
        <span><MessageCircle :size="15" />{{ article.commentCount || 0 }} 条评论</span>
      </div>
    </div>
  </article>
</template>

<style scoped>
.article-row {
  display: grid;
  grid-template-columns: 108px 1fr;
  gap: var(--space-5);
  padding: var(--space-6) 0;
  border-bottom: 1px solid var(--line-soft);
}

.article-date {
  align-self: stretch;
  padding-right: var(--space-4);
  border-right: 1px solid var(--line-soft);
  font-family: var(--serif);
}

.article-date strong,
.article-date span {
  display: block;
}

.article-date strong {
  font-size: 2.5rem;
  font-weight: 400;
  line-height: 1;
}

.article-date span {
  margin-top: 0.55rem;
  color: var(--ink-soft);
  font-size: 0.85rem;
}

.article-category {
  color: var(--vermilion);
  font-size: 0.82rem;
  font-weight: 700;
}

h3 {
  margin: 0.35rem 0 0.45rem;
  font-family: var(--serif);
  font-size: clamp(1.35rem, 2.4vw, 1.85rem);
  line-height: 1.35;
}

h3 a:hover {
  color: var(--vermilion);
}

p {
  margin: 0;
  color: var(--ink-soft);
}

.article-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-4);
  margin-top: var(--space-3);
  color: var(--ink-faint);
  font-size: 0.8rem;
}

.article-meta span {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
}

@media (max-width: 560px) {
  .article-row {
    grid-template-columns: 58px 1fr;
    gap: var(--space-3);
    padding: var(--space-5) 0;
  }

  .article-date {
    padding-right: var(--space-2);
  }

  .article-date strong {
    font-size: 1.85rem;
  }

  .article-date span {
    font-size: 0.68rem;
  }

  .article-meta {
    gap: var(--space-2) var(--space-3);
  }
}
</style>
