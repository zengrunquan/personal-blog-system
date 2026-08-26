<script setup lang="ts">
import { FilePenLine, FileText, KeyRound, UserRound } from 'lucide-vue-next'
</script>

<template>
  <nav class="user-tabs" aria-label="用户中心导航">
    <RouterLink :to="{ name: 'me' }"><UserRound :size="17" />主页</RouterLink>
    <RouterLink :to="{ name: 'me-edit' }"><FilePenLine :size="17" />编辑资料</RouterLink>
    <RouterLink :to="{ name: 'me-password' }"><KeyRound :size="17" />修改密码</RouterLink>
    <RouterLink :to="{ name: 'me-articles' }"><FileText :size="17" />我的文章</RouterLink>
  </nav>
</template>

<style scoped>
.user-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 0 var(--space-5);
  margin-bottom: var(--space-5);
  border-bottom: 1px solid var(--line);
}

a {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  padding: 0.75rem 0;
  border-bottom: 2px solid transparent;
  color: var(--ink-soft);
  font-size: 0.88rem;
}

a:hover,
a.router-link-exact-active {
  color: var(--vermilion);
  border-bottom-color: var(--vermilion);
}

@media (max-width: 560px) {
  .user-tabs {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 0;
  }

  a {
    justify-content: center;
  }
}
</style>
