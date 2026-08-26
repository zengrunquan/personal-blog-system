import { render, screen } from '@testing-library/vue'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { publicApi } from '@/api/blog'
import type { User } from '@/api/types'
import { useSessionStore } from '@/stores/session'
import HomeView from './HomeView.vue'

vi.mock('@/api/blog', () => ({
  publicApi: {
    home: vi.fn(),
  },
}))

const homeData = {
  featuredArticles: [],
  categories: [],
  stats: { articles: 0, categories: 0, authors: 0 },
}

function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'home', component: { template: '<div />' } },
      { path: '/articles', name: 'articles', component: { template: '<div />' } },
    ],
  })
}

async function renderHome() {
  const router = createTestRouter()
  await router.push('/')
  await router.isReady()
  return render(HomeView, { global: { plugins: [router] } })
}

describe('HomeView 关于我', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    vi.mocked(publicApi.home).mockResolvedValue(homeData)
  })

  it('访客使用默认头像和默认寄语且不显示邮箱', async () => {
    await renderHome()

    expect(
      await screen.findByText('喜欢分享技术心得，也记录生活里值得慢慢回看的片段。'),
    ).toBeTruthy()
    expect(screen.getByRole('img', { name: '默认头像' }).getAttribute('src')).toBe(
      '/images/author-portrait.png',
    )
    expect(screen.queryByText('admin@example.com')).toBeNull()
  })

  it('登录后显示当前用户的头像、寄语和注册邮箱', async () => {
    const currentUser = {
      id: 9,
      username: 'reader',
      nickname: '读者',
      email: 'reader@example.com',
      avatar: '/uploads/images/reader.webp',
      bio: '愿每次记录都有回声。',
      role: 0,
      status: 1,
    } as User & { bio: string }
    useSessionStore().setUser(currentUser)

    await renderHome()

    expect(await screen.findByText('愿每次记录都有回声。')).toBeTruthy()
    expect(screen.getByRole('img', { name: '读者的头像' }).getAttribute('src')).toBe(
      '/uploads/images/reader.webp',
    )
    expect(screen.getByRole('link', { name: 'reader@example.com' }).getAttribute('href')).toBe(
      'mailto:reader@example.com',
    )
  })
})
