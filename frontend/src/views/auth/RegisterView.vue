<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ArrowRight } from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import { authApi } from '@/api/blog'
import { AppError } from '@/api/types'

const router = useRouter()
const form = reactive({ username: '', nickname: '', email: '', password: '', confirmPassword: '' })
const fieldErrors = ref<Record<string, string>>({})
const error = ref('')
const busy = ref(false)

async function submit(): Promise<void> {
  fieldErrors.value = {}
  error.value = ''
  if (form.password !== form.confirmPassword) {
    fieldErrors.value.confirmPassword = '两次输入的密码不一致'
    return
  }
  busy.value = true
  try {
    await authApi.register({
      username: form.username.trim(),
      nickname: form.nickname.trim(),
      email: form.email.trim(),
      password: form.password,
    })
    await router.push({ name: 'login', query: { registered: '1' } })
  } catch (caught) {
    if (caught instanceof AppError) {
      error.value = caught.message
      fieldErrors.value = caught.fieldErrors ?? {}
    } else error.value = '注册失败，请稍后重试'
    console.error(`[DEBUG] ${new Date().toISOString()} [RegisterView.submit] 注册失败`, caught)
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <section class="page-container register-page page-section">
    <header class="register-intro">
      <p class="page-kicker">Join the journal</p>
      <h1 class="page-title">创建账号</h1>
      <p class="page-lead">保留自己的阅读轨迹，也开始写下第一篇文章。</p>
    </header>
    <form class="auth-paper register-form" @submit.prevent="submit">
      <div class="form-field">
        <label for="username">用户名</label
        ><input
          id="username"
          v-model="form.username"
          class="form-control"
          minlength="3"
          maxlength="20"
          autocomplete="username"
          required
        />
        <p v-if="fieldErrors.username" class="field-error">{{ fieldErrors.username }}</p>
      </div>
      <div class="form-field">
        <label for="nickname">昵称</label
        ><input
          id="nickname"
          v-model="form.nickname"
          class="form-control"
          maxlength="40"
          required
        />
        <p v-if="fieldErrors.nickname" class="field-error">{{ fieldErrors.nickname }}</p>
      </div>
      <div class="form-field form-wide">
        <label for="email">邮箱</label
        ><input
          id="email"
          v-model="form.email"
          class="form-control"
          type="email"
          autocomplete="email"
          required
        />
        <p v-if="fieldErrors.email" class="field-error">{{ fieldErrors.email }}</p>
      </div>
      <div class="form-field">
        <label for="password">密码</label
        ><input
          id="password"
          v-model="form.password"
          class="form-control"
          type="password"
          minlength="6"
          autocomplete="new-password"
          required
        />
        <p v-if="fieldErrors.password" class="field-error">{{ fieldErrors.password }}</p>
      </div>
      <div class="form-field">
        <label for="confirmPassword">确认密码</label
        ><input
          id="confirmPassword"
          v-model="form.confirmPassword"
          class="form-control"
          type="password"
          autocomplete="new-password"
          required
        />
        <p v-if="fieldErrors.confirmPassword" class="field-error">
          {{ fieldErrors.confirmPassword }}
        </p>
      </div>
      <p v-if="error" class="form-error form-wide">{{ error }}</p>
      <div class="register-actions form-wide">
        <p>已有账号？<RouterLink :to="{ name: 'login' }">直接登录</RouterLink></p>
        <button class="button button--primary" type="submit" :disabled="busy">
          {{ busy ? '正在创建…' : '完成注册' }}<ArrowRight :size="18" />
        </button>
      </div>
    </form>
  </section>
</template>

<style scoped>
.register-page {
  max-width: 980px;
}

.register-intro {
  margin-bottom: var(--space-6);
  padding-bottom: var(--space-5);
  border-bottom: 1px solid var(--line);
}

.register-form {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-5);
  padding: clamp(1.5rem, 5vw, 3rem);
}

.form-wide {
  grid-column: 1 / -1;
}

.register-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  padding-top: var(--space-4);
  border-top: 1px solid var(--line-soft);
}

.register-actions p {
  margin: 0;
  color: var(--ink-soft);
  font-size: 0.88rem;
}

.register-actions a {
  color: var(--vermilion);
  font-weight: 700;
}

@media (max-width: 650px) {
  .register-form {
    grid-template-columns: 1fr;
  }

  .form-wide {
    grid-column: auto;
  }

  .register-actions {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
