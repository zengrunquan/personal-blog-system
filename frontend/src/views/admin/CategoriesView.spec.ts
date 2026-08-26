import { render, screen } from '@testing-library/vue'
import { createMemoryHistory, createRouter } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { adminApi } from '@/api/blog'
import CategoriesView from './CategoriesView.vue'

vi.mock('@/api/blog', () => ({
  adminApi: {
    categories: vi.fn(),
    createCategory: vi.fn(),
    updateCategory: vi.fn(),
    deleteCategory: vi.fn(),
  },
}))

const category = {
  id: 8,
  name: '技术随笔',
  description: '开发与架构',
  sortOrder: 3,
  articleCount: 2,
}

function routerFor(path: string) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/admin', name: 'admin', component: { template: '<div />' } },
      { path: '/admin/users', name: 'admin-users', component: { template: '<div />' } },
      { path: '/admin/articles', name: 'admin-articles', component: { template: '<div />' } },
      { path: '/admin/categories', name: 'admin-categories', component: { template: '<div />' } },
    ],
  })
  return router.push(path).then(() => router)
}

function inputValue(label: string): string {
  const element = screen.getByLabelText(label)
  if (!(element instanceof HTMLInputElement)) throw new Error(`${label} 不是输入框`)
  return element.value
}

describe('CategoriesView legacy route intent', () => {
  beforeEach(() => {
    vi.mocked(adminApi.categories).mockResolvedValue([category])
  })

  it('通过 create 查询参数打开新增表单', async () => {
    const router = await routerFor('/admin/categories?create=1')

    render(CategoriesView, { global: { plugins: [router] } })

    expect(await screen.findByRole('button', { name: '创建分类' })).toBeTruthy()
    expect(inputValue('分类名称')).toBe('')
  })

  it('通过 edit 查询参数打开并填充指定分类', async () => {
    const router = await routerFor('/admin/categories?edit=8')

    render(CategoriesView, { global: { plugins: [router] } })

    expect(await screen.findByRole('button', { name: '更新分类' })).toBeTruthy()
    expect(inputValue('分类名称')).toBe('技术随笔')
    expect(inputValue('说明')).toBe('开发与架构')
  })
})
