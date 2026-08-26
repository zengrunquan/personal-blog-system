import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { authApi } from '@/api/blog'
import { setCsrfToken } from '@/api/http'
import type { User } from '@/api/types'

export const useSessionStore = defineStore('session', () => {
  const user = ref<User | null>(null)
  const initialized = ref(false)
  const loading = ref(false)
  const sessionMessage = ref('')
  let bootstrapPromise: Promise<void> | null = null

  const authenticated = computed(() => user.value !== null)
  const isAdmin = computed(() => user.value?.role === 1)

  async function bootstrap(force = false): Promise<void> {
    if (initialized.value && !force) return
    if (bootstrapPromise && !force) return bootstrapPromise
    loading.value = true
    bootstrapPromise = (async () => {
      try {
        const data = await authApi.session()
        // Gson 默认会省略 null 字段；这里统一归一化，避免 undefined 被误判为已登录用户。
        user.value = data.user ?? null
        setCsrfToken(data.csrfToken)
      } catch (error) {
        user.value = null
        console.error(`[DEBUG] ${new Date().toISOString()} [session.bootstrap] 初始化失败`, error)
      } finally {
        initialized.value = true
        loading.value = false
        bootstrapPromise = null
      }
    })()
    return bootstrapPromise
  }

  async function login(username: string, password: string): Promise<User> {
    await bootstrap()
    try {
      const data = await authApi.login({ username, password })
      user.value = data.user
      setCsrfToken(data.csrfToken)
      return data.user
    } catch (error) {
      console.error(`[DEBUG] ${new Date().toISOString()} [session.login] 登录失败`, error)
      throw error
    }
  }

  async function logout(): Promise<void> {
    try {
      await authApi.logout()
    } finally {
      user.value = null
      initialized.value = false
      setCsrfToken('')
      await bootstrap(true)
    }
  }

  function setUser(next: User): void {
    user.value = next
  }

  if (typeof window !== 'undefined') {
    window.addEventListener('session-expired', () => {
      user.value = null
      sessionMessage.value = '登录状态已过期，请重新登录'
    })
  }

  return {
    user,
    initialized,
    loading,
    sessionMessage,
    authenticated,
    isAdmin,
    bootstrap,
    login,
    logout,
    setUser,
  }
})
