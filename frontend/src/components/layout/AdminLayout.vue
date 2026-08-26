<script setup lang="ts">
import { FileText, FolderTree, Gauge, Users } from 'lucide-vue-next'

defineProps<{ title: string; description?: string }>()
</script>

<template>
  <div class="page-container admin-shell">
    <aside class="admin-sidebar" aria-label="后台导航">
      <div class="admin-mark">
        <span>管理台</span>
        <small>内容与用户</small>
      </div>
      <nav>
        <RouterLink :to="{ name: 'admin' }"><Gauge :size="18" />概览</RouterLink>
        <RouterLink :to="{ name: 'admin-users' }"><Users :size="18" />用户</RouterLink>
        <RouterLink :to="{ name: 'admin-articles' }"><FileText :size="18" />文章</RouterLink>
        <RouterLink :to="{ name: 'admin-categories' }"><FolderTree :size="18" />分类</RouterLink>
      </nav>
    </aside>
    <section class="admin-content">
      <header class="admin-header">
        <div>
          <p class="page-kicker">Administration</p>
          <h1>{{ title }}</h1>
          <p v-if="description">{{ description }}</p>
        </div>
        <slot name="actions" />
      </header>
      <slot />
    </section>
  </div>
</template>

<style scoped>
.admin-shell {
  display: grid;
  grid-template-columns: 210px minmax(0, 1fr);
  gap: var(--space-6);
  padding-top: var(--space-6);
}

.admin-sidebar {
  align-self: start;
  position: sticky;
  top: 1rem;
  padding: var(--space-5) var(--space-4);
  border: 1px solid var(--line);
  background: var(--ink);
  color: var(--paper-light);
}

.admin-mark span,
.admin-mark small {
  display: block;
}

.admin-mark span {
  font-family: var(--serif);
  font-size: 1.35rem;
  font-weight: 700;
}

.admin-mark small {
  color: #b9b3aa;
  font-size: 0.74rem;
  letter-spacing: 0.12em;
}

nav {
  display: grid;
  gap: 0.3rem;
  margin-top: var(--space-5);
}

nav a {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  padding: 0.7rem 0.75rem;
  border-left: 2px solid transparent;
  color: #cfcbc4;
  font-size: 0.88rem;
}

nav a:hover,
nav a.router-link-exact-active {
  color: #fff;
  border-left-color: var(--vermilion);
  background: #292d34;
}

.admin-content {
  min-width: 0;
}

.admin-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--space-4);
  margin-bottom: var(--space-5);
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--line);
}

h1,
p {
  margin: 0;
}

h1 {
  font-family: var(--serif);
  font-size: 2rem;
}

.admin-header > div > p:last-child {
  margin-top: 0.3rem;
  color: var(--ink-soft);
  font-size: 0.9rem;
}

@media (max-width: 800px) {
  .admin-shell {
    grid-template-columns: 1fr;
  }

  .admin-sidebar {
    position: static;
    padding: 0.8rem;
  }

  .admin-mark {
    display: none;
  }

  nav {
    grid-template-columns: repeat(4, 1fr);
    gap: 0;
    margin: 0;
  }

  nav a {
    justify-content: center;
    padding: 0.7rem 0.3rem;
    border-bottom: 2px solid transparent;
    border-left: 0;
    font-size: 0.74rem;
  }

  nav a:hover,
  nav a.router-link-exact-active {
    border-bottom-color: var(--vermilion);
    border-left-color: transparent;
  }
}
</style>
