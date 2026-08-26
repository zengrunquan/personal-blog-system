<script setup lang="ts">
import { ArrowLeft } from 'lucide-vue-next'
</script>

<template>
  <section class="page-container not-found">
    <span>404</span>
    <h1>这一页暂时没有文字</h1>
    <p>地址可能已经变化，也可能只是一次误入。</p>
    <RouterLink class="button button--primary" :to="{ name: 'home' }"
      ><ArrowLeft :size="17" />回到首页</RouterLink
    >
  </section>
</template>

<style scoped>
.not-found {
  min-height: 60vh;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 0.8rem;
  text-align: center;
}

span {
  color: var(--vermilion);
  font-family: var(--serif);
  font-size: clamp(4rem, 15vw, 9rem);
  line-height: 1;
}

h1,
p {
  margin: 0;
}

h1 {
  font-family: var(--serif);
}

p {
  color: var(--ink-soft);
}

a {
  margin-top: 1rem;
}
</style>
