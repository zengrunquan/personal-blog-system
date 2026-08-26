<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ArrowLeft, Clock3, Eye, MessageCircle, Send, Trash2 } from 'lucide-vue-next'
import DOMPurify from 'dompurify'
import { useRoute, useRouter } from 'vue-router'
import { publicApi } from '@/api/blog'
import type { Article, Comment } from '@/api/types'
import { AppError } from '@/api/types'
import PageState from '@/components/common/PageState.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import { useSessionStore } from '@/stores/session'

const route = useRoute()
const router = useRouter()
const session = useSessionStore()
const article = ref<Article | null>(null)
const comments = ref<Comment[]>([])
const loading = ref(true)
const error = ref('')
const commentText = ref('')
const commentError = ref('')
const submitting = ref(false)
const deleteId = ref<number | null>(null)

const id = computed(() => Number(route.params.id))
const safeContent = computed(() => DOMPurify.sanitize(article.value?.content ?? ''))
const canDeleteComment = (comment: Comment) =>
  session.isAdmin || session.user?.id === comment.userId

async function load(): Promise<void> {
  loading.value = true
  error.value = ''
  try {
    const data = await publicApi.article(id.value)
    article.value = data.article
    comments.value = data.comments
  } catch (caught) {
    error.value = caught instanceof AppError ? caught.message : '文章加载失败'
    console.error(`[DEBUG] ${new Date().toISOString()} [ArticleDetailView.load] 加载失败`, caught)
  } finally {
    loading.value = false
  }
}

async function submitComment(): Promise<void> {
  if (!session.authenticated) {
    await router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  if (!commentText.value.trim()) {
    commentError.value = '请写下评论内容'
    return
  }
  submitting.value = true
  commentError.value = ''
  try {
    await publicApi.addComment(id.value, commentText.value.trim())
    commentText.value = ''
    await load()
  } catch (caught) {
    commentError.value = caught instanceof AppError ? caught.message : '评论发表失败'
    console.error(
      `[DEBUG] ${new Date().toISOString()} [ArticleDetailView.submitComment] 提交失败`,
      caught,
    )
  } finally {
    submitting.value = false
  }
}

async function confirmDeleteComment(): Promise<void> {
  if (deleteId.value === null) return
  try {
    await publicApi.deleteComment(deleteId.value)
    deleteId.value = null
    await load()
  } catch (caught) {
    commentError.value = caught instanceof AppError ? caught.message : '评论删除失败'
    console.error(
      `[DEBUG] ${new Date().toISOString()} [ArticleDetailView.deleteComment] 删除失败`,
      caught,
    )
  }
}

watch(id, () => void load())
onMounted(() => void load())
</script>

<template>
  <section class="page-container article-page page-section">
    <RouterLink class="back-link" :to="{ name: 'articles' }"
      ><ArrowLeft :size="17" />返回文章列表</RouterLink
    >
    <PageState v-if="loading" type="loading" />
    <PageState v-else-if="error" type="error" :message="error" @retry="load" />
    <template v-else-if="article">
      <article>
        <header class="article-header">
          <p class="page-kicker">{{ article.categoryName || '未分类' }}</p>
          <h1>{{ article.title }}</h1>
          <p v-if="article.summary" class="article-deck">{{ article.summary }}</p>
          <div class="article-byline">
            <span
              ><Clock3 :size="16" />{{
                article.createTime ? new Date(article.createTime).toLocaleDateString('zh-CN') : ''
              }}</span
            >
            <span><Eye :size="16" />{{ article.viewCount ?? 0 }} 次阅读</span>
            <span><MessageCircle :size="16" />{{ comments.length }} 条评论</span>
          </div>
        </header>
        <img
          v-if="article.coverImage"
          class="article-cover"
          :src="article.coverImage"
          :alt="article.title"
        />
        <!-- 服务端白名单是第一道边界，DOMPurify 在渲染前再次防护已有历史内容。 -->
        <div class="article-body" v-html="safeContent"></div>
      </article>

      <section class="comments-section" aria-labelledby="comments-title">
        <div class="section-heading"><h2 id="comments-title">读者留言</h2></div>
        <form class="comment-form" @submit.prevent="submitComment">
          <label for="comment">{{
            session.authenticated ? '写下你的想法' : '登录后参与讨论'
          }}</label>
          <textarea
            id="comment"
            v-model="commentText"
            class="form-control"
            maxlength="1000"
            :disabled="submitting"
            placeholder="保持真诚，也保持友善。"
          ></textarea>
          <div class="comment-form-footer">
            <p v-if="commentError" class="field-error">{{ commentError }}</p>
            <span v-else>{{ commentText.length }} / 1000</span>
            <button class="button button--primary" type="submit" :disabled="submitting">
              <Send :size="17" />{{
                submitting ? '正在提交…' : session.authenticated ? '发表评论' : '去登录'
              }}
            </button>
          </div>
        </form>

        <PageState v-if="!comments.length" type="empty" message="还没有留言，来写第一条吧。" />
        <div v-else class="comment-list">
          <article v-for="comment in comments" :key="comment.id" class="comment-item">
            <div class="comment-avatar">
              {{ (comment.userNickname || comment.username || '读').slice(0, 1) }}
            </div>
            <div>
              <header>
                <strong>{{ comment.userNickname || comment.username || '读者' }}</strong
                ><time>{{ new Date(comment.createTime).toLocaleString('zh-CN') }}</time>
              </header>
              <p>{{ comment.content }}</p>
            </div>
            <button
              v-if="canDeleteComment(comment)"
              class="comment-delete"
              type="button"
              aria-label="删除评论"
              @click="deleteId = comment.id"
            >
              <Trash2 :size="16" />
            </button>
          </article>
        </div>
      </section>
    </template>
    <ConfirmDialog
      :open="deleteId !== null"
      title="删除这条评论？"
      message="删除后无法恢复。"
      @cancel="deleteId = null"
      @confirm="confirmDeleteComment"
    />
  </section>
</template>

<style scoped>
.article-page {
  max-width: 920px;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  margin-bottom: var(--space-5);
  color: var(--ink-soft);
  font-size: 0.86rem;
}

.back-link:hover {
  color: var(--vermilion);
}

.article-header {
  padding: var(--space-6) 0;
  border-block: 1px solid var(--line);
  text-align: center;
}

.article-header h1 {
  max-width: 820px;
  margin: 0 auto;
  font-family: var(--serif);
  font-size: clamp(2.25rem, 5vw, 4rem);
  line-height: 1.25;
}

.article-deck {
  max-width: 660px;
  margin: 1rem auto 0;
  color: var(--ink-soft);
  font-family: var(--serif);
  font-size: 1.08rem;
}

.article-byline {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: var(--space-4);
  margin-top: var(--space-4);
  color: var(--ink-faint);
  font-size: 0.8rem;
}

.article-byline span {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
}

.article-cover {
  width: 100%;
  max-height: 520px;
  margin: var(--space-6) 0;
  object-fit: cover;
}

.article-body {
  max-width: var(--reading);
  margin: var(--space-7) auto;
  font-family: var(--serif);
  font-size: 1.08rem;
  line-height: 2;
}

.article-body :deep(h2),
.article-body :deep(h3) {
  margin: 2.2em 0 0.7em;
  line-height: 1.4;
}

.article-body :deep(a) {
  color: var(--vermilion);
  border-bottom: 1px solid currentColor;
}

.article-body :deep(blockquote) {
  margin: 1.8rem 0;
  padding: 0.6rem 0 0.6rem 1.4rem;
  border-left: 3px solid var(--vermilion);
  color: var(--ink-soft);
}

.article-body :deep(pre) {
  overflow-x: auto;
  padding: 1.2rem;
  color: var(--paper-light);
  background: var(--ink);
  font-family: Consolas, monospace;
  font-size: 0.88rem;
}

.article-body :deep(img) {
  margin: 2rem auto;
}

.comments-section {
  margin-top: var(--space-8);
}

.comment-form {
  display: grid;
  gap: 0.65rem;
  margin: var(--space-5) 0;
  padding: var(--space-5);
  border: 1px solid var(--line);
  background: var(--paper-light);
}

.comment-form label {
  font-family: var(--serif);
  font-weight: 700;
}

.comment-form-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  color: var(--ink-faint);
  font-size: 0.8rem;
}

.comment-form-footer p {
  margin: 0;
}

.comment-list {
  border-top: 1px solid var(--line-soft);
}

.comment-item {
  position: relative;
  display: grid;
  grid-template-columns: 42px 1fr auto;
  gap: var(--space-3);
  padding: var(--space-5) 0;
  border-bottom: 1px solid var(--line-soft);
}

.comment-avatar {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border: 1px solid var(--line);
  border-radius: 50%;
  background: var(--paper-deep);
  font-family: var(--serif);
}

.comment-item header {
  display: flex;
  align-items: baseline;
  gap: var(--space-3);
}

.comment-item time {
  color: var(--ink-faint);
  font-size: 0.75rem;
}

.comment-item p {
  margin: 0.35rem 0 0;
  color: var(--ink-soft);
  white-space: pre-wrap;
}

.comment-delete {
  align-self: start;
  padding: 0.35rem;
  border: 0;
  color: var(--ink-faint);
  background: transparent;
  cursor: pointer;
}

.comment-delete:hover {
  color: var(--danger);
}

@media (max-width: 600px) {
  .article-header {
    text-align: left;
  }

  .article-byline {
    justify-content: flex-start;
  }

  .comment-form-footer {
    align-items: stretch;
    flex-direction: column;
  }

  .comment-item {
    grid-template-columns: 36px 1fr auto;
  }

  .comment-avatar {
    width: 36px;
    height: 36px;
  }
}
</style>
