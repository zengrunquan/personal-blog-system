import { render, screen } from '@testing-library/vue'
import { createRouter, createMemoryHistory } from 'vue-router'
import { describe, expect, it } from 'vitest'
import ArticleListItem from './ArticleListItem.vue'

describe('ArticleListItem', () => {
  it('使用可访问链接展示文章标题、分类与摘要', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/articles/:id', name: 'article-detail', component: { template: '<div />' } },
      ],
    })
    render(ArticleListItem, {
      props: {
        article: {
          id: 18,
          title: 'MySQL 数据库优化技巧',
          content: '<p>正文</p>',
          summary: '索引优化与慢查询分析。',
          categoryId: 2,
          categoryName: '技术分享',
          status: 1,
          createTime: '2026-08-18T10:00:00+08:00',
        },
      },
      global: { plugins: [router] },
    })
    expect(screen.getByRole('link', { name: 'MySQL 数据库优化技巧' }).getAttribute('href')).toBe(
      '/articles/18',
    )
    expect(screen.getAllByText('技术分享')).toHaveLength(2)
    expect(screen.getByText('索引优化与慢查询分析。')).toBeTruthy()
  })
})
