import { render, screen } from '@testing-library/vue'
import { createMemoryHistory, createRouter } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { meApi } from '@/api/blog'
import ProfileView from './ProfileView.vue'

vi.mock('@/api/blog', () => ({
  meApi: {
    profile: vi.fn(),
  },
}))

async function renderProfile() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/me', name: 'me', component: { template: '<div />' } },
      { path: '/me/edit', name: 'me-edit', component: { template: '<div />' } },
      { path: '/me/password', name: 'me-password', component: { template: '<div />' } },
      { path: '/me/articles', name: 'me-articles', component: { template: '<div />' } },
    ],
  })
  await router.push('/me')
  await router.isReady()
  return render(ProfileView, { global: { plugins: [router] } })
}

describe('ProfileView 默认资料', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('用户未设置头像和寄语时展示系统默认内容', async () => {
    vi.mocked(meApi.profile).mockResolvedValue({
      id: 10,
      username: 'new-user',
      nickname: '新用户',
      email: 'new@example.com',
      avatar: '',
      role: 0,
      status: 1,
    })

    await renderProfile()

    expect(
      await screen.findByText('喜欢分享技术心得，也记录生活里值得慢慢回看的片段。'),
    ).toBeTruthy()
    expect(screen.getByRole('img', { name: '新用户的头像' }).getAttribute('src')).toBe(
      '/images/author-portrait.png',
    )
  })
})
