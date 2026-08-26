import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useSessionStore } from './session'
import { authApi } from '@/api/blog'

vi.mock('@/api/blog', () => ({
  authApi: {
    session: vi.fn(),
    login: vi.fn(),
    logout: vi.fn(),
  },
}))

describe('session store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('后端省略空 user 字段时仍保持访客状态', async () => {
    vi.mocked(authApi.session).mockResolvedValue({
      authenticated: false,
      csrfToken: 'test-token',
    } as Awaited<ReturnType<typeof authApi.session>>)

    const session = useSessionStore()
    await session.bootstrap()

    expect(session.user).toBeNull()
    expect(session.authenticated).toBe(false)
  })
})
