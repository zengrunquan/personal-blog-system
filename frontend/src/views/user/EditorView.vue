<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Eye, Save } from 'lucide-vue-next'
import DOMPurify from 'dompurify'
import { useRoute, useRouter } from 'vue-router'
import { articleApi, publicApi } from '@/api/blog'
import type { Article, Category } from '@/api/types'
import { AppError } from '@/api/types'
import PageState from '@/components/common/PageState.vue'
import RichTextEditor from '@/components/editor/RichTextEditor.vue'

const route = useRoute()
const router = useRouter()
const articleId = computed(() => (route.params.id ? Number(route.params.id) : null))
const form = reactive<Article>({
  title: '',
  content: '<p></p>',
  summary: '',
  coverImage: '',
  categoryId: 0,
  status: 1,
})
const categories = ref<Category[]>([])
const loading = ref(true)
const busy = ref(false)
const error = ref('')
const fieldErrors = ref<Record<string, string>>({})
const preview = ref(false)
const safePreview = computed(() => DOMPurify.sanitize(form.content))

async function load(): Promise<void> {
  loading.value = true
  error.value = ''
  try {
    const list = await publicApi.articles({ page: 1, pageSize: 1 })
    categories.value = list.categories
    if (!form.categoryId && categories.value[0]) form.categoryId = categories.value[0].id
    if (articleId.value) {
      const data = await publicApi.article(articleId.value)
      Object.assign(form, data.article)
    }
  } catch (caught) {
    error.value = caught instanceof AppError ? caught.message : '编辑器加载失败'
    console.error(`[DEBUG] ${new Date().toISOString()} [EditorView.load] 加载失败`, caught)
  } finally {
    loading.value = false
  }
}

async function submit(): Promise<void> {
  error.value = ''
  fieldErrors.value = {}
  if (!form.title.trim()) {
    fieldErrors.value.title = '请输入文章标题'
    return
  }
  if (!form.categoryId) {
    fieldErrors.value.categoryId = '请选择文章分类'
    return
  }
  if (!form.content || form.content === '<p></p>') {
    fieldErrors.value.content = '请输入文章正文'
    return
  }
  busy.value = true
  try {
    if (articleId.value) await articleApi.update(articleId.value, form)
    else await articleApi.create(form)
    await router.push({ name: 'me-articles' })
  } catch (caught) {
    if (caught instanceof AppError) {
      error.value = caught.message
      fieldErrors.value = caught.fieldErrors ?? {}
    } else error.value = '文章保存失败'
    console.error(`[DEBUG] ${new Date().toISOString()} [EditorView.submit] 保存失败`, caught)
  } finally {
    busy.value = false
  }
}

onMounted(() => void load())
</script>

<template>
  <section class="page-container editor-page page-section">
    <PageState v-if="loading" type="loading" />
    <PageState
      v-else-if="error && !categories.length"
      type="error"
      :message="error"
      @retry="load"
    />
    <form v-else class="editor-paper editor-form" @submit.prevent="submit">
      <header class="editor-header">
        <div>
          <p class="page-kicker">{{ articleId ? 'Revise' : 'New story' }}</p>
          <h1>{{ articleId ? '编辑文章' : '写一篇新文章' }}</h1>
        </div>
        <div class="editor-actions">
          <button class="button" type="button" @click="preview = !preview">
            <Eye :size="17" />{{ preview ? '返回编辑' : '预览' }}</button
          ><button class="button button--primary" type="submit" :disabled="busy">
            <Save :size="17" />{{ busy ? '保存中…' : '保存文章' }}
          </button>
        </div>
      </header>
      <template v-if="!preview">
        <div class="form-field">
          <label for="title">标题</label
          ><input
            id="title"
            v-model="form.title"
            class="form-control title-input"
            maxlength="200"
            placeholder="给这篇文章一个清楚的标题"
          />
          <p v-if="fieldErrors.title" class="field-error">{{ fieldErrors.title }}</p>
        </div>
        <div class="editor-meta-grid">
          <div class="form-field">
            <label for="category">分类</label
            ><select id="category" v-model="form.categoryId" class="form-control">
              <option v-for="category in categories" :key="category.id" :value="category.id">
                {{ category.name }}
              </option>
            </select>
            <p v-if="fieldErrors.categoryId" class="field-error">{{ fieldErrors.categoryId }}</p>
          </div>
          <div class="form-field">
            <label for="status">状态</label
            ><select id="status" v-model="form.status" class="form-control">
              <option :value="1">发布</option>
              <option :value="0">草稿</option>
            </select>
          </div>
        </div>
        <div class="form-field">
          <label for="summary">摘要</label
          ><textarea
            id="summary"
            v-model="form.summary"
            class="form-control"
            maxlength="500"
            placeholder="留空时将从正文自动生成"
          ></textarea>
        </div>
        <div class="form-field">
          <span class="field-label">正文</span><RichTextEditor v-model="form.content" />
          <p v-if="fieldErrors.content" class="field-error">{{ fieldErrors.content }}</p>
        </div>
      </template>
      <article v-else class="preview-paper">
        <p class="page-kicker">
          {{ categories.find((item) => item.id === form.categoryId)?.name || '未分类' }}
        </p>
        <h1>{{ form.title || '未命名文章' }}</h1>
        <p v-if="form.summary" class="preview-summary">{{ form.summary }}</p>
        <div class="preview-body" v-html="safePreview"></div>
      </article>
      <p v-if="error" class="form-error">{{ error }}</p>
    </form>
  </section>
</template>

<style scoped>
.editor-page {
  max-width: 1120px;
}
.editor-form {
  display: grid;
  gap: var(--space-5);
  padding: clamp(1.25rem, 4vw, 2.5rem);
}
.editor-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--space-4);
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--line);
}
.editor-header h1,
.editor-header p {
  margin: 0;
}
.editor-header h1 {
  font-family: var(--serif);
  font-size: 2.2rem;
}
.editor-actions {
  display: flex;
  gap: var(--space-3);
}
.title-input {
  height: auto;
  padding: 0.6rem 0;
  border-width: 0 0 1px;
  background: transparent;
  font-family: var(--serif);
  font-size: clamp(1.5rem, 3vw, 2.3rem);
}
.editor-meta-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-4);
}
.preview-paper {
  max-width: 760px;
  min-height: 520px;
  margin: 0 auto;
  padding: var(--space-6);
}
.preview-paper > h1 {
  margin: 0;
  font-family: var(--serif);
  font-size: 2.8rem;
  line-height: 1.3;
}
.preview-summary {
  color: var(--ink-soft);
  font-family: var(--serif);
  font-size: 1.1rem;
}
.preview-body {
  margin-top: var(--space-6);
  font-family: var(--serif);
  line-height: 2;
}
.preview-body :deep(pre) {
  overflow-x: auto;
  padding: 1rem;
  color: var(--paper-light);
  background: var(--ink);
}
.preview-body :deep(blockquote) {
  margin-left: 0;
  padding-left: 1rem;
  border-left: 3px solid var(--vermilion);
}
@media (max-width: 700px) {
  .editor-header {
    align-items: stretch;
    flex-direction: column;
  }
  .editor-actions .button {
    flex: 1;
  }
  .editor-meta-grid {
    grid-template-columns: 1fr;
  }
  .preview-paper {
    padding: var(--space-4) 0;
  }
}
</style>
