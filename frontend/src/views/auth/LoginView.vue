<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ArrowRight, LockKeyhole, UserRound } from 'lucide-vue-next'
import { useRoute, useRouter } from 'vue-router'
import { AppError } from '@/api/types'
import { useSessionStore } from '@/stores/session'

const route = useRoute()
const router = useRouter()
const session = useSessionStore()
const form = reactive({ username: '', password: '' })
const error = ref('')
const busy = ref(false)
const message = computed(
  () => session.sessionMessage || (route.query.registered ? '注册成功，请使用新账号登录' : ''),
)

async function submit(): Promise<void> {
  if (!form.username.trim() || !form.password) {
    error.value = '请输入用户名和密码'
    return
  }
  busy.value = true
  error.value = ''
  try {
    const user = await session.login(form.username.trim(), form.password)
    const target =
      typeof route.query.redirect === 'string'
        ? route.query.redirect
        : user.role === 1
          ? '/admin'
          : '/'
    await router.push(target)
  } catch (caught) {
    error.value = caught instanceof AppError ? caught.message : '登录失败，请稍后重试'
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <section class="page-container auth-layout page-section">
    <div class="auth-intro">
      <p class="page-kicker">Welcome back</p>
      <h1>回来继续写，<br />也继续读。</h1>
      <p>登录后可以管理个人资料、文章草稿与评论。</p>
    </div>
    <form class="auth-paper auth-form" @submit.prevent="submit">
      <header>
        <h2>登录</h2>
        <p>使用你的博客账号</p>
      </header>
      <p v-if="message" class="form-success">{{ message }}</p>
      <div class="form-field">
        <label for="username">用户名</label>
        <div class="input-with-icon">
          <UserRound :size="18" /><input
            id="username"
            v-model="form.username"
            class="form-control"
            autocomplete="username"
          />
        </div>
      </div>
      <div class="form-field">
        <label for="password">密码</label>
        <div class="input-with-icon">
          <LockKeyhole :size="18" /><input
            id="password"
            v-model="form.password"
            class="form-control"
            type="password"
            autocomplete="current-password"
          />
        </div>
      </div>
      <p v-if="error" class="form-error">{{ error }}</p>
      <button class="button button--primary auth-submit" type="submit" :disabled="busy">
        {{ busy ? '正在登录…' : '登录' }}<ArrowRight :size="18" />
      </button>
      <p class="auth-switch">
        还没有账号？<RouterLink :to="{ name: 'register' }">注册一个</RouterLink>
      </p>
    </form>
  </section>
</template>

<style scoped>
.auth-layout {
  min-height: 620px;
  display: grid;
  grid-template-columns: 1fr minmax(360px, 480px);
  align-items: center;
  gap: clamp(3rem, 10vw, 9rem);
}

.auth-intro h1 {
  margin: 0;
  font-family: var(--serif);
  font-size: clamp(2.8rem, 6vw, 5rem);
  line-height: 1.25;
}

.auth-intro > p:last-child {
  max-width: 430px;
  color: var(--ink-soft);
  font-family: var(--serif);
}

.auth-form {
  display: grid;
  gap: var(--space-4);
  padding: clamp(1.5rem, 4vw, 2.5rem);
}

.auth-form header {
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--line-soft);
}

.auth-form h2,
.auth-form header p {
  margin: 0;
}

.auth-form h2 {
  font-family: var(--serif);
  font-size: 2rem;
}

.auth-form header p {
  color: var(--ink-faint);
  font-size: 0.85rem;
}

.input-with-icon {
  position: relative;
}

.input-with-icon svg {
  position: absolute;
  z-index: 1;
  top: 50%;
  left: 0.85rem;
  color: var(--ink-faint);
  transform: translateY(-50%);
}

.input-with-icon input {
  padding-left: 2.6rem;
}

.auth-submit {
  width: 100%;
  margin-top: 0.5rem;
}

.auth-switch {
  margin: 0;
  color: var(--ink-soft);
  font-size: 0.88rem;
  text-align: center;
}

.auth-switch a {
  color: var(--vermilion);
  font-weight: 700;
}

@media (max-width: 760px) {
  .auth-layout {
    min-height: 0;
    grid-template-columns: 1fr;
    gap: var(--space-5);
  }

  .auth-intro h1 {
    font-size: 2.75rem;
  }
}
</style>
