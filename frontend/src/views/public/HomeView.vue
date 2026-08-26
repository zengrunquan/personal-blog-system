<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ArrowRight, Eye, FileText, Mail, PenLine } from 'lucide-vue-next'
import { publicApi } from '@/api/blog'
import type { HomeData } from '@/api/types'
import { AppError } from '@/api/types'
import { siteConfig } from '@/config/site'
import { useSessionStore } from '@/stores/session'
import ArticleListItem from '@/components/article/ArticleListItem.vue'
import PageState from '@/components/common/PageState.vue'

const loading = ref(true)
const error = ref('')
const data = ref<HomeData | null>(null)
const session = useSessionStore()

const categories = computed(() => data.value?.categories ?? [])
const aboutAvatar = computed(() => session.user?.avatar?.trim() || siteConfig.defaultAvatar)
const aboutBiography = computed(() => session.user?.bio?.trim() || siteConfig.defaultBiography)
const aboutAvatarAlt = computed(() =>
  session.user ? `${session.user.nickname || session.user.username}的头像` : '默认头像',
)

async function load(): Promise<void> {
  loading.value = true
  error.value = ''
  try {
    data.value = await publicApi.home()
  } catch (caught) {
    error.value = caught instanceof AppError ? caught.message : '首页内容加载失败'
    console.error(`[DEBUG] ${new Date().toISOString()} [HomeView.load] 加载失败`, caught)
  } finally {
    loading.value = false
  }
}

onMounted(() => void load())
</script>

<template>
  <div>
    <section class="page-container hero" aria-labelledby="hero-title">
      <div class="hero-copy">
        <h1 id="hero-title">{{ siteConfig.title }}</h1>
        <p>{{ siteConfig.tagline }}</p>
        <span class="hero-stroke" aria-hidden="true"></span>
        <RouterLink class="button button--primary hero-button" :to="{ name: 'articles' }">
          开始阅读 <ArrowRight :size="18" />
        </RouterLink>
      </div>
      <div class="hero-art" aria-hidden="true">
        <img src="/images/hero-still-life.png" alt="" />
      </div>
    </section>

    <section class="page-container home-content">
      <div class="latest-column">
        <div class="section-heading">
          <h2>最新文章</h2>
          <RouterLink class="text-link" :to="{ name: 'articles' }"
            >查看全部文章 <ArrowRight :size="16"
          /></RouterLink>
        </div>
        <PageState v-if="loading" type="loading" />
        <PageState v-else-if="error" type="error" :message="error" @retry="load" />
        <PageState
          v-else-if="!data?.featuredArticles.length"
          type="empty"
          message="第一篇文章正在路上。"
        />
        <div v-else>
          <ArticleListItem
            v-for="article in data.featuredArticles.slice(0, 3)"
            :key="article.id"
            :article="article"
          />
          <div class="all-articles-link">
            <RouterLink class="text-link" :to="{ name: 'articles' }"
              >查看全部文章 <ArrowRight :size="16"
            /></RouterLink>
          </div>
        </div>
      </div>

      <aside class="home-rail">
        <section id="about" class="rail-section about-section">
          <h2>关于我</h2>
          <span class="rail-mark" aria-hidden="true"></span>
          <img class="author-portrait" :src="aboutAvatar" :alt="aboutAvatarAlt" />
          <p>{{ aboutBiography }}</p>
          <a v-if="session.user?.email" class="contact" :href="`mailto:${session.user.email}`"
            ><Mail :size="18" />{{ session.user.email }}</a
          >
        </section>

        <section class="rail-section">
          <h2>文章分类</h2>
          <span class="rail-mark" aria-hidden="true"></span>
          <ul class="category-list">
            <li v-for="category in categories" :key="category.id">
              <RouterLink :to="{ name: 'articles', query: { category: category.id } }">
                <span><PenLine :size="16" />{{ category.name }}</span>
                <strong>{{ category.articleCount ?? 0 }}</strong>
              </RouterLink>
            </li>
          </ul>
        </section>

        <section class="rail-section">
          <h2>站点统计</h2>
          <span class="rail-mark" aria-hidden="true"></span>
          <div class="stats-grid">
            <div>
              <FileText :size="19" /><strong>{{ data?.stats.articles ?? 0 }}</strong
              ><span>文章总数</span>
            </div>
            <div>
              <Eye :size="19" /><strong>{{ data?.stats.authors ?? 0 }}</strong
              ><span>写作者</span>
            </div>
          </div>
        </section>
      </aside>
    </section>
  </div>
</template>

<style scoped>
.hero {
  min-height: 430px;
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(410px, 0.92fr);
  align-items: center;
  gap: var(--space-5);
  padding: var(--space-6) 2rem var(--space-5);
}

.hero-copy {
  position: relative;
  z-index: 1;
}

.hero h1 {
  margin: 0;
  font-family: var(--serif);
  font-size: clamp(3rem, 5.4vw, 4.25rem);
  font-weight: 700;
  letter-spacing: 0.025em;
  line-height: 1.15;
}

.hero-copy > p:not(.hero-eyebrow) {
  margin: 1.2rem 0 0;
  color: var(--ink-soft);
  font-family: var(--serif);
  font-size: clamp(1.15rem, 2vw, 1.55rem);
  letter-spacing: 0.08em;
}

.hero-stroke {
  width: 42px;
  height: 2px;
  display: block;
  margin: 2rem 0 2.25rem;
  background: var(--vermilion);
}

.hero-button {
  min-width: 176px;
  min-height: 54px;
  font-family: var(--serif);
  font-size: 1.02rem;
}

.hero-art {
  min-width: 0;
  align-self: stretch;
  display: flex;
  align-items: center;
  justify-content: center;
}

.hero-art img {
  width: min(100%, 690px);
  max-height: 475px;
  object-fit: contain;
  filter: saturate(0.8) contrast(0.96);
}

@media (min-width: 701px) {
  .hero-art img {
    transform: scale(1.1);
  }
}

.home-content {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 310px;
  gap: var(--space-7);
  padding-top: var(--space-5);
}

.latest-column {
  min-width: 0;
}

.all-articles-link {
  display: flex;
  justify-content: center;
  padding: var(--space-5) 0;
}

.home-rail {
  padding-left: var(--space-6);
  border-left: 1px solid var(--line-soft);
}

.rail-section {
  padding: 0 0 var(--space-5);
  border-top: 1px solid var(--line);
}

.rail-section + .rail-section {
  padding-top: var(--space-4);
}

.rail-section h2 {
  margin: var(--space-5) 0 0;
  font-family: var(--serif);
  font-size: 1.2rem;
}

.rail-mark {
  width: 27px;
  height: 1px;
  display: block;
  margin: 0.8rem 0 1.35rem;
  background: var(--ink-soft);
}

.author-portrait {
  width: 112px;
  height: 112px;
  margin: 0 auto 1.25rem;
  border-radius: 50%;
  object-fit: cover;
}

.about-section p {
  margin: 0;
  color: var(--ink-soft);
  font-family: var(--serif);
  font-size: 0.92rem;
}

.contact {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  margin-top: 1.2rem;
  color: var(--ink-soft);
  font-size: 0.86rem;
}

.contact:hover {
  color: var(--vermilion);
}

.category-list {
  display: grid;
  gap: 0.85rem;
  margin: 0;
  padding: 0;
  list-style: none;
}

.category-list a,
.category-list a span {
  display: flex;
  align-items: center;
}

.category-list a {
  justify-content: space-between;
  gap: 1rem;
  color: var(--ink-soft);
  font-size: 0.9rem;
}

.category-list a span {
  gap: 0.65rem;
}

.category-list a:hover {
  color: var(--vermilion);
}

.category-list strong {
  font-family: var(--serif);
  font-weight: 400;
}

.stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
}

.stats-grid div {
  display: grid;
  gap: 0.25rem;
  padding-right: 1rem;
}

.stats-grid div + div {
  padding-left: 1rem;
  border-left: 1px solid var(--line-soft);
}

.stats-grid svg {
  color: var(--ink-soft);
}

.stats-grid strong {
  color: var(--vermilion);
  font-family: var(--serif);
  font-size: 1.8rem;
  font-weight: 400;
  line-height: 1.2;
}

.stats-grid span {
  color: var(--ink-faint);
  font-size: 0.75rem;
}

@media (max-width: 950px) {
  .hero {
    min-height: auto;
    grid-template-columns: 1fr 1fr;
  }

  .home-content {
    grid-template-columns: 1fr;
  }

  .home-rail {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: var(--space-5);
    padding-left: 0;
    border-left: 0;
  }

  .rail-section + .rail-section {
    padding-top: 0;
  }
}

@media (max-width: 700px) {
  .hero {
    grid-template-columns: 1fr;
    padding: var(--space-6) 0 var(--space-5);
  }

  .hero-copy {
    padding-inline: 0.25rem;
  }

  .hero h1 {
    font-size: clamp(2.65rem, 13vw, 4rem);
  }

  .hero-art {
    order: -1;
    max-height: 270px;
    overflow: hidden;
  }

  .hero-art img {
    width: 100%;
    max-height: 300px;
  }

  .home-rail {
    grid-template-columns: 1fr;
  }

  .rail-section + .rail-section {
    padding-top: var(--space-4);
  }
}
</style>
