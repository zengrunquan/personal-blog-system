<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Ban, CheckCircle2, Trash2 } from 'lucide-vue-next'
import { adminApi } from '@/api/blog'
import type { PageResult, User } from '@/api/types'
import { AppError } from '@/api/types'
import AdminLayout from '@/components/layout/AdminLayout.vue'
import AppPagination from '@/components/common/AppPagination.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import PageState from '@/components/common/PageState.vue'
import { useSessionStore } from '@/stores/session'

const session = useSessionStore()
const result = ref<PageResult<User> | null>(null)
const page = ref(1)
const loading = ref(true)
const error = ref('')
const busyId = ref<number | null>(null)
const deleteTarget = ref<User | null>(null)

async function load(): Promise<void> {
  loading.value = true
  error.value = ''
  try {
    result.value = await adminApi.users({ page: page.value, pageSize: 15 })
  } catch (caught) {
    error.value = caught instanceof AppError ? caught.message : '用户列表加载失败'
    console.error(`[DEBUG] ${new Date().toISOString()} [UsersView.load] 加载失败`, caught)
  } finally {
    loading.value = false
  }
}
async function changePage(next: number): Promise<void> {
  page.value = next
  await load()
}
async function toggleStatus(user: User): Promise<void> {
  busyId.value = user.id
  try {
    await adminApi.setUserStatus(user.id, user.status === 1 ? 0 : 1)
    await load()
  } catch (caught) {
    error.value = caught instanceof AppError ? caught.message : '状态更新失败'
    console.error(`[DEBUG] ${new Date().toISOString()} [UsersView.toggleStatus] 更新失败`, caught)
  } finally {
    busyId.value = null
  }
}
async function confirmDelete(): Promise<void> {
  if (!deleteTarget.value) return
  busyId.value = deleteTarget.value.id
  try {
    await adminApi.deleteUser(deleteTarget.value.id)
    deleteTarget.value = null
    await load()
  } catch (caught) {
    error.value = caught instanceof AppError ? caught.message : '用户删除失败'
    console.error(`[DEBUG] ${new Date().toISOString()} [UsersView.delete] 删除失败`, caught)
  } finally {
    busyId.value = null
  }
}
onMounted(() => void load())
</script>

<template>
  <AdminLayout title="用户管理" description="启用、禁用或移除用户账号。">
    <PageState v-if="loading" type="loading" />
    <PageState v-else-if="error && !result" type="error" :message="error" @retry="load" />
    <template v-else-if="result">
      <p v-if="error" class="form-error page-error">{{ error }}</p>
      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>用户</th>
              <th>邮箱</th>
              <th>角色</th>
              <th>状态</th>
              <th>加入时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in result.items" :key="user.id">
              <td data-label="用户">
                <strong>{{ user.nickname }}</strong
                ><small>@{{ user.username }}</small>
              </td>
              <td data-label="邮箱">{{ user.email }}</td>
              <td data-label="角色">{{ user.role === 1 ? '管理员' : '普通用户' }}</td>
              <td data-label="状态">
                <span
                  class="status-badge"
                  :class="user.status === 1 ? 'status-badge--live' : 'status-badge--draft'"
                  >{{ user.status === 1 ? '正常' : '禁用' }}</span
                >
              </td>
              <td data-label="加入时间">
                {{ user.createTime ? new Date(user.createTime).toLocaleDateString('zh-CN') : '—' }}
              </td>
              <td data-label="操作">
                <div v-if="user.id !== session.user?.id" class="table-actions">
                  <button
                    class="button button--small"
                    type="button"
                    :disabled="busyId === user.id"
                    @click="toggleStatus(user)"
                  >
                    <Ban v-if="user.status === 1" :size="15" /><CheckCircle2 v-else :size="15" />{{
                      user.status === 1 ? '禁用' : '启用'
                    }}</button
                  ><button
                    class="button button--small button--danger"
                    type="button"
                    @click="deleteTarget = user"
                  >
                    <Trash2 :size="15" />删除
                  </button>
                </div>
                <span v-else class="current-user">当前账号</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <AppPagination :page="result.page" :total-pages="result.totalPages" @change="changePage" />
    </template>
    <ConfirmDialog
      :open="deleteTarget !== null"
      title="删除这个用户？"
      :message="`账号 @${deleteTarget?.username ?? ''} 的关联内容可能同时受到影响。`"
      :busy="busyId !== null"
      @cancel="deleteTarget = null"
      @confirm="confirmDelete"
    />
  </AdminLayout>
</template>

<style scoped>
td strong,
td small {
  display: block;
}
td strong {
  font-family: var(--serif);
}
td small,
.current-user {
  color: var(--ink-faint);
  font-size: 0.76rem;
}
.page-error {
  margin-bottom: var(--space-3);
}
</style>
