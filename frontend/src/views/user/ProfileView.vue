<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  CalendarDays,
  Edit3,
  FileText,
  Mail,
  MessageSquareQuote,
  ShieldCheck,
} from 'lucide-vue-next'
import { meApi } from '@/api/blog'
import type { User } from '@/api/types'
import { AppError } from '@/api/types'
import PageState from '@/components/common/PageState.vue'
import UserTabs from '@/components/user/UserTabs.vue'
import { siteConfig } from '@/config/site'

const profile = ref<User | null>(null)
const loading = ref(true)
const error = ref('')
const profileAvatar = computed(() => profile.value?.avatar?.trim() || siteConfig.defaultAvatar)
const profileBiography = computed(() => profile.value?.bio?.trim() || siteConfig.defaultBiography)

async function load(): Promise<void> {
  loading.value = true
  error.value = ''
  try {
    profile.value = await meApi.profile()
  } catch (caught) {
    error.value = caught instanceof AppError ? caught.message : '个人资料加载失败'
    console.error(`[DEBUG] ${new Date().toISOString()} [ProfileView.load] 加载失败`, caught)
  } finally {
    loading.value = false
  }
}

onMounted(() => void load())
</script>

<template>
  <section class="page-container profile-page page-section">
    <UserTabs />
    <PageState v-if="loading" type="loading" />
    <PageState v-else-if="error" type="error" :message="error" @retry="load" />
    <div v-else-if="profile" class="profile-paper profile-card">
      <div class="profile-avatar-wrap">
        <img :src="profileAvatar" :alt="`${profile.nickname}的头像`" />
      </div>
      <div class="profile-copy">
        <p class="page-kicker">Personal desk</p>
        <h1>{{ profile.nickname }}</h1>
        <p class="username">@{{ profile.username }}</p>
        <dl>
          <div>
            <dt><Mail :size="17" />邮箱</dt>
            <dd>{{ profile.email }}</dd>
          </div>
          <div>
            <dt><MessageSquareQuote :size="17" />个人寄语</dt>
            <dd>{{ profileBiography }}</dd>
          </div>
          <div>
            <dt><CalendarDays :size="17" />加入时间</dt>
            <dd>
              {{
                profile.createTime ? new Date(profile.createTime).toLocaleDateString('zh-CN') : '—'
              }}
            </dd>
          </div>
          <div>
            <dt><ShieldCheck :size="17" />账号角色</dt>
            <dd>{{ profile.role === 1 ? '管理员' : '普通用户' }}</dd>
          </div>
        </dl>
        <div class="profile-actions">
          <RouterLink class="button button--primary" :to="{ name: 'me-edit' }"
            ><Edit3 :size="17" />编辑资料</RouterLink
          >
          <RouterLink class="button" :to="{ name: 'me-articles' }"
            ><FileText :size="17" />我的文章</RouterLink
          >
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.profile-page {
  max-width: 980px;
}

.profile-card {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: var(--space-7);
  padding: clamp(1.5rem, 6vw, 4rem);
}

.profile-avatar-wrap {
  width: 220px;
  height: 220px;
  display: grid;
  place-items: center;
  border: 1px solid var(--line);
  border-radius: 50%;
  background: var(--paper-deep);
  overflow: hidden;
}

.profile-avatar-wrap img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-avatar-wrap span {
  color: var(--vermilion);
  font-family: var(--serif);
  font-size: 5rem;
}

h1,
.username {
  margin: 0;
}

h1 {
  font-family: var(--serif);
  font-size: 2.7rem;
}

.username {
  color: var(--ink-faint);
}

dl {
  margin: var(--space-5) 0;
}

dl div {
  display: grid;
  grid-template-columns: 130px 1fr;
  gap: 1rem;
  padding: 0.75rem 0;
  border-bottom: 1px solid var(--line-soft);
}

dt {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--ink-soft);
}

dd {
  margin: 0;
}

.profile-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
}

@media (max-width: 700px) {
  .profile-card {
    grid-template-columns: 1fr;
    gap: var(--space-5);
  }

  .profile-avatar-wrap {
    width: 140px;
    height: 140px;
  }

  dl div {
    grid-template-columns: 1fr;
    gap: 0.2rem;
  }
}
</style>
