<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { LogOut, Menu, PenLine, Search, UserRound, X } from 'lucide-vue-next'
import { useSessionStore } from '@/stores/session'

const router = useRouter()
const route = useRoute()
const session = useSessionStore()
const query = ref(typeof route.query.q === 'string' ? route.query.q : '')
const open = ref(false)

const accountLabel = computed(() => session.user?.nickname || session.user?.username || '我的主页')

async function search(): Promise<void> {
  const q = query.value.trim()
  await router.push({ name: 'articles', query: q ? { q } : {} })
  open.value = false
}

async function logout(): Promise<void> {
  try {
    await session.logout()
    await router.push({ name: 'home' })
  } catch (error) {
    console.error(`[DEBUG] ${new Date().toISOString()} [SiteHeader.logout] 退出失败`, error)
  }
}
</script>

<template>
  <header class="site-header">
    <div class="page-container header-inner">
      <RouterLink class="brand" :to="{ name: 'home' }" aria-label="个人博客首页">
        <PenLine :size="23" stroke-width="1.8" />
        <span>个人博客</span>
      </RouterLink>

      <button
        class="menu-button"
        type="button"
        :aria-expanded="open"
        aria-label="打开导航"
        @click="open = !open"
      >
        <X v-if="open" :size="23" />
        <Menu v-else :size="23" />
      </button>

      <div class="header-content" :class="{ 'header-content--open': open }">
        <nav class="main-nav" aria-label="主导航">
          <RouterLink :to="{ name: 'articles' }" @click="open = false">文章</RouterLink>
          <RouterLink
            :to="{ name: 'home', hash: '#about' }"
            active-class=""
            exact-active-class=""
            :class="{ 'hash-link--active': route.hash === '#about' }"
            @click="open = false"
            >关于</RouterLink
          >
          <RouterLink
            v-if="session.authenticated"
            :to="{ name: 'me-articles' }"
            @click="open = false"
            >我的文章</RouterLink
          >
          <RouterLink v-if="session.isAdmin" :to="{ name: 'admin' }" @click="open = false"
            >管理后台</RouterLink
          >
        </nav>

        <form class="header-search" role="search" @submit.prevent="search">
          <Search :size="18" aria-hidden="true" />
          <input v-model="query" type="search" aria-label="搜索文章" placeholder="搜索文章…" />
        </form>

        <div class="account-nav">
          <template v-if="session.authenticated">
            <RouterLink class="account-link" :to="{ name: 'me' }" @click="open = false">
              <UserRound :size="17" />{{ accountLabel }}
            </RouterLink>
            <button class="icon-action" type="button" aria-label="退出登录" @click="logout">
              <LogOut :size="17" />
            </button>
          </template>
          <template v-else>
            <RouterLink :to="{ name: 'login' }" @click="open = false">登录</RouterLink>
            <span class="nav-divider" aria-hidden="true"></span>
            <RouterLink :to="{ name: 'register' }" @click="open = false">注册</RouterLink>
          </template>
        </div>
      </div>
    </div>
  </header>
</template>

<style scoped>
.site-header {
  position: relative;
  z-index: 30;
  background: rgb(246 241 232 / 96%);
}

.header-inner {
  min-height: 86px;
  display: flex;
  align-items: center;
  gap: 3.5rem;
  border-bottom: 1px solid var(--line);
}

.brand {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 0.72rem;
  font-family: var(--serif);
  font-size: 1.25rem;
  font-weight: 700;
  letter-spacing: 0.06em;
}

.brand :deep(svg) {
  color: var(--vermilion);
}

.header-content {
  min-width: 0;
  flex: 1;
  display: flex;
  align-items: center;
  gap: 2.75rem;
}

.main-nav,
.account-nav {
  display: flex;
  align-items: center;
  gap: 2rem;
  white-space: nowrap;
}

.main-nav a,
.account-nav a {
  position: relative;
  font-family: var(--serif);
}

.main-nav a::after {
  position: absolute;
  right: 0;
  bottom: -0.45rem;
  left: 0;
  height: 1px;
  background: var(--vermilion);
  content: '';
  transform: scaleX(0);
  transition: transform 160ms ease;
}

.main-nav a:hover::after,
.main-nav a.router-link-active::after,
.main-nav a.hash-link--active::after {
  transform: scaleX(1);
}

.header-search {
  min-width: 190px;
  max-width: 280px;
  flex: 1;
  display: flex;
  align-items: center;
  gap: 0.65rem;
  margin-left: auto;
  color: var(--ink-soft);
  border-bottom: 1px solid transparent;
}

.header-search:focus-within {
  border-bottom-color: var(--vermilion);
}

.header-search input {
  width: 100%;
  padding: 0.5rem 0;
  border: 0;
  outline: 0;
  color: var(--ink);
  background: transparent;
}

.nav-divider {
  width: 1px;
  height: 1.15rem;
  background: var(--line);
}

.account-link,
.icon-action {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
}

.icon-action,
.menu-button {
  padding: 0.4rem;
  border: 0;
  background: transparent;
  cursor: pointer;
}

.menu-button {
  display: none;
  margin-left: auto;
}

@media (max-width: 1040px) {
  .header-inner {
    gap: 2rem;
  }

  .header-content {
    gap: 1.5rem;
  }

  .main-nav,
  .account-nav {
    gap: 1.2rem;
  }

  .header-search {
    min-width: 150px;
  }
}

@media (max-width: 820px) {
  .header-inner {
    min-height: 72px;
  }

  .menu-button {
    display: inline-flex;
  }

  .header-content {
    position: absolute;
    top: 72px;
    right: 0;
    left: 0;
    display: none;
    padding: 1.35rem 1rem 1.75rem;
    border-bottom: 1px solid var(--line);
    background: var(--paper-light);
    box-shadow: var(--shadow-paper);
  }

  .header-content--open {
    display: grid;
  }

  .main-nav,
  .account-nav {
    align-items: flex-start;
    flex-direction: column;
    gap: 0.95rem;
  }

  .header-search {
    width: 100%;
    max-width: none;
    margin: 0;
    padding: 0.25rem 0;
    border-bottom-color: var(--line);
  }

  .account-nav {
    padding-top: 1rem;
    border-top: 1px solid var(--line-soft);
  }

  .nav-divider {
    display: none;
  }
}
</style>
