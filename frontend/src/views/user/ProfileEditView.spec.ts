import { fireEvent, render, screen } from '@testing-library/vue'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { meApi } from '@/api/blog'
import type { User } from '@/api/types'
import ProfileEditView from './ProfileEditView.vue'

vi.mock('@/api/blog', () => ({
  meApi: {
    profile: vi.fn(),
    update: vi.fn(),
    avatar: vi.fn(),
  },
}))

const profile = {
  id: 9,
  username: 'reader',
  nickname: '读者',
  email: 'reader@example.com',
  avatar: '',
  bio: '愿每次记录都有回声。',
  role: 0,
  status: 1,
} as User & { bio: string }

async function renderProfileEdit() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/me', name: 'me', component: { template: '<div />' } },
      { path: '/me/edit', name: 'me-edit', component: { template: '<div />' } },
      { path: '/me/password', name: 'me-password', component: { template: '<div />' } },
      { path: '/me/articles', name: 'me-articles', component: { template: '<div />' } },
    ],
  })
  await router.push('/me/edit')
  await router.isReady()
  return render(ProfileEditView, { global: { plugins: [router] } })
}

describe('ProfileEditView 个人寄语', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    vi.mocked(meApi.profile).mockResolvedValue(profile)
    vi.mocked(meApi.update).mockImplementation((payload) => {
      const biography = 'bio' in payload ? payload.bio : undefined
      if (biography !== '记录新的想法。') throw new Error('请求未携带修改后的个人寄语')
      return Promise.resolve({ ...profile, bio: biography })
    })
  })

  it('加载已有寄语并在保存时提交修改值', async () => {
    await renderProfileEdit()
    const biography = await screen.findByLabelText('个人寄语')

    expect((biography as HTMLTextAreaElement).value).toBe('愿每次记录都有回声。')

    await fireEvent.update(biography, '记录新的想法。')
    await fireEvent.click(screen.getByRole('button', { name: '保存修改' }))

    expect(await screen.findByText('个人资料已保存')).toBeTruthy()
  })
})
