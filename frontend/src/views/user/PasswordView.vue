<script setup lang="ts">
import { reactive, ref } from 'vue'
import { KeyRound } from 'lucide-vue-next'
import { meApi } from '@/api/blog'
import { AppError } from '@/api/types'
import UserTabs from '@/components/user/UserTabs.vue'

const form = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const error = ref('')
const success = ref('')
const busy = ref(false)

async function submit(): Promise<void> {
  error.value = ''
  success.value = ''
  if (form.newPassword.length < 6) {
    error.value = '新密码至少 6 位'
    return
  }
  if (form.newPassword !== form.confirmPassword) {
    error.value = '两次输入的新密码不一致'
    return
  }
  busy.value = true
  try {
    await meApi.password({ oldPassword: form.oldPassword, newPassword: form.newPassword })
    form.oldPassword = ''
    form.newPassword = ''
    form.confirmPassword = ''
    success.value = '密码修改成功，下次登录请使用新密码'
  } catch (caught) {
    error.value = caught instanceof AppError ? caught.message : '密码修改失败'
    console.error(`[DEBUG] ${new Date().toISOString()} [PasswordView.submit] 修改失败`, caught)
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <section class="page-container password-page page-section">
    <UserTabs />
    <form class="profile-paper password-form" @submit.prevent="submit">
      <header>
        <KeyRound :size="26" />
        <div>
          <p class="page-kicker">Security</p>
          <h1>修改密码</h1>
        </div>
      </header>
      <div class="form-field">
        <label for="oldPassword">当前密码</label
        ><input
          id="oldPassword"
          v-model="form.oldPassword"
          class="form-control"
          type="password"
          autocomplete="current-password"
          required
        />
      </div>
      <div class="form-field">
        <label for="newPassword">新密码</label
        ><input
          id="newPassword"
          v-model="form.newPassword"
          class="form-control"
          type="password"
          minlength="6"
          autocomplete="new-password"
          required
        />
      </div>
      <div class="form-field">
        <label for="confirmPassword">确认新密码</label
        ><input
          id="confirmPassword"
          v-model="form.confirmPassword"
          class="form-control"
          type="password"
          autocomplete="new-password"
          required
        />
      </div>
      <p v-if="error" class="form-error">{{ error }}</p>
      <p v-if="success" class="form-success">{{ success }}</p>
      <button class="button button--primary" type="submit" :disabled="busy">
        {{ busy ? '正在修改…' : '确认修改' }}
      </button>
    </form>
  </section>
</template>

<style scoped>
.password-page {
  max-width: 760px;
}
.password-form {
  display: grid;
  gap: var(--space-5);
  padding: clamp(1.5rem, 5vw, 3rem);
}
header {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--line-soft);
}
header svg {
  color: var(--vermilion);
}
h1,
header p {
  margin: 0;
}
h1 {
  font-family: var(--serif);
  font-size: 2rem;
}
.button {
  justify-self: end;
}
@media (max-width: 560px) {
  .button {
    width: 100%;
  }
}
</style>
