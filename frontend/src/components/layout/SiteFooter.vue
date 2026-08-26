<script setup lang="ts">
import { PenLine } from 'lucide-vue-next'
import { siteConfig } from '@/config/site'
</script>

<template>
  <footer class="site-footer">
    <div class="page-container footer-grid">
      <div>
        <div class="footer-brand"><PenLine :size="21" />{{ siteConfig.name }}</div>
        <p>{{ siteConfig.tagline }}</p>
      </div>
      <div>
        <h2>快速链接</h2>
        <RouterLink :to="{ name: 'home' }">首页</RouterLink>
        <RouterLink :to="{ name: 'articles' }">文章列表</RouterLink>
      </div>
      <div>
        <h2>技术栈</h2>
        <span v-for="item in siteConfig.techStack" :key="item">{{ item }}</span>
      </div>
    </div>
    <div class="page-container copyright">© 2026 个人博客系统 · 认真记录，慢慢生长</div>
  </footer>
</template>

<style scoped>
.site-footer {
  margin-top: var(--space-6);
  padding: var(--space-5) 0 var(--space-3);
  border-top: 1px solid var(--line);
}

.footer-grid {
  display: grid;
  grid-template-columns: 1.6fr 1fr 1fr;
  gap: var(--space-7);
  padding-bottom: var(--space-5);
}

.footer-brand {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  font-family: var(--serif);
  font-size: 1.2rem;
  font-weight: 700;
}

.footer-brand :deep(svg) {
  color: var(--vermilion);
}

p,
a,
span {
  display: block;
  margin: 0.5rem 0 0;
  color: var(--ink-soft);
  font-size: 0.9rem;
}

a:hover {
  color: var(--vermilion);
}

h2 {
  margin: 0;
  font-family: var(--serif);
  font-size: 1rem;
}

.copyright {
  padding-top: var(--space-3);
  border-top: 1px solid var(--line-soft);
  color: var(--ink-faint);
  font-size: 0.82rem;
  text-align: center;
}

@media (max-width: 700px) {
  .footer-grid {
    grid-template-columns: 1fr;
    gap: var(--space-5);
  }
}
</style>
