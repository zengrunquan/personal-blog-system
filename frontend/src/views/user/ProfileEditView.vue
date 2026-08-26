<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Camera, Save } from 'lucide-vue-next'
import { meApi } from '@/api/blog'
import { AppError } from '@/api/types'
import PageState from '@/components/common/PageState.vue'
import UserTabs from '@/components/user/UserTabs.vue'
import { siteConfig } from '@/config/site'
import { useSessionStore } from '@/stores/session'

const session = useSessionStore()
const form = reactive({ nickname: '', email: '', bio: '' })
const loading = ref(true)
const busy = ref(false)
const avatarBusy = ref(false)
const error = ref('')
const success = ref('')
const avatarPreview = ref('')

async function load(): Promise<void> {
  loading.value = true
  try {
    const profile = await meApi.profile()
    form.nickname = profile.nickname
    form.email = profile.email
    form.bio = profile.bio ?? ''
    avatarPreview.value = profile.avatar ?? ''
  } catch (caught) {
    error.value = caught instanceof AppError ? caught.message : '资料加载失败'
  } finally {
    loading.value = false
  }
}

async function submit(): Promise<void> {
  busy.value = true
  error.value = ''
  success.value = ''
  try {
    const user = await meApi.update({
      nickname: form.nickname.trim(),
      email: form.email.trim(),
      bio: form.bio.trim(),
    })
    session.setUser(user)
    success.value = '个人资料已保存'
  } catch (caught) {
    error.value = caught instanceof AppError ? caught.message : '资料保存失败'
    console.error(`[DEBUG] ${new Date().toISOString()} [ProfileEditView.submit] 保存失败`, caught)
  } finally {
    busy.value = false
  }
}

async function uploadAvatar(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  avatarBusy.value = true
  error.value = ''
  try {
    const result = await meApi.avatar(file)
    avatarPreview.value = result.url
    await session.bootstrap(true)
    success.value = '头像已更新'
  } catch (caught) {
    error.value = caught instanceof AppError ? caught.message : '头像上传失败'
    console.error(
      `[DEBUG] ${new Date().toISOString()} [ProfileEditView.uploadAvatar] 上传失败`,
      caught,
    )
  } finally {
    avatarBusy.value = false
    input.value = ''
  }
}

onMounted(() => void load())
</script>

<template>
  <section class="page-container edit-page page-section">
    <UserTabs />
    <PageState v-if="loading" type="loading" />
    <form v-else class="profile-paper edit-form" @submit.prevent="submit">
      <header>
        <p class="page-kicker">Profile</p>
        <h1>编辑资料</h1>
        <p>让昵称与联系方式保持最新。</p>
      </header>
      <div class="avatar-editor">
        <div class="avatar-preview">
          <img :src="avatarPreview || siteConfig.defaultAvatar" alt="当前头像" />
        </div>
        <label class="button button--small" :class="{ disabled: avatarBusy }"
          ><Camera :size="16" />{{ avatarBusy ? '上传中…' : '更换头像'
          }}<input
            type="file"
            accept="image/jpeg,image/png,image/gif,image/webp"
            :disabled="avatarBusy"
            @change="uploadAvatar"
        /></label>
        <small>支持 JPG、PNG、GIF、WebP，最大 5MB。</small>
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
      </div>
      <div class="form-field">
        <label for="email">邮箱</label
        ><input id="email" v-model="form.email" class="form-control" type="email" required />
      </div>
      <div class="form-field bio-field">
        <label for="bio">个人寄语</label>
        <textarea
          id="bio"
          v-model="form.bio"
          class="form-control"
          maxlength="200"
          rows="4"
          aria-describedby="bio-help"
        ></textarea>
        <small id="bio-help">留空将使用系统默认寄语，最多 200 字。{{ form.bio.length }}/200</small>
      </div>
      <p v-if="error" class="form-error">{{ error }}</p>
      <p v-if="success" class="form-success">{{ success }}</p>
      <div class="form-actions">
        <button class="button button--primary" type="submit" :disabled="busy">
          <Save :size="17" />{{ busy ? '保存中…' : '保存修改' }}
        </button>
      </div>
    </form>
  </section>
</template>

<style scoped>
.edit-page {
  max-width: 850px;
}
.edit-form {
  display: grid;
  gap: var(--space-5);
  padding: clamp(1.5rem, 5vw, 3rem);
}
header {
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--line-soft);
}
h1,
header p {
  margin: 0;
}
h1 {
  font-family: var(--serif);
  font-size: 2.2rem;
}
header > p:last-child {
  color: var(--ink-soft);
}
.avatar-editor {
  display: grid;
  grid-template-columns: 100px auto 1fr;
  align-items: center;
  gap: var(--space-4);
}
.avatar-preview {
  width: 100px;
  height: 100px;
  display: grid;
  place-items: center;
  overflow: hidden;
  border: 1px solid var(--line);
  border-radius: 50%;
  background: var(--paper-deep);
  color: var(--vermilion);
  font-family: var(--serif);
  font-size: 2.5rem;
}
.avatar-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.avatar-editor label input {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
}
.avatar-editor small {
  color: var(--ink-faint);
}
.bio-field textarea {
  min-height: 7rem;
  resize: vertical;
  line-height: 1.7;
}
.bio-field small {
  color: var(--ink-faint);
}
.disabled {
  opacity: 0.5;
  pointer-events: none;
}
.form-actions {
  display: flex;
  justify-content: flex-end;
  padding-top: var(--space-4);
  border-top: 1px solid var(--line-soft);
}
@media (max-width: 560px) {
  .avatar-editor {
    grid-template-columns: 82px 1fr;
  }
  .avatar-preview {
    width: 82px;
    height: 82px;
  }
  .avatar-editor small {
    grid-column: 1 / -1;
  }
}
</style>
