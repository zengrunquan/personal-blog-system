import { describe, expect, it } from 'vitest'
import type { RouteLocationNormalized } from 'vue-router'
import { guardRoute } from './index'
import type { useSessionStore } from '@/stores/session'

function route(meta: Record<string, unknown>, fullPath = '/me'): RouteLocationNormalized {
  return { meta, fullPath } as RouteLocationNormalized
}

function session(authenticated: boolean, isAdmin: boolean) {
  return { authenticated, isAdmin } as unknown as ReturnType<typeof useSessionStore>
}

describe('guardRoute', () => {
  it('将未登录用户送到登录页并保留目标地址', () => {
    expect(guardRoute(route({ requiresAuth: true }), session(false, false))).toEqual({
      name: 'login',
      query: { redirect: '/me' },
    })
  })

  it('拒绝普通用户进入后台', () => {
    expect(guardRoute(route({ requiresAdmin: true }, '/admin'), session(true, false))).toEqual({
      name: 'home',
      query: { redirect: '/admin' },
    })
  })

  it('允许管理员进入后台', () => {
    expect(guardRoute(route({ requiresAdmin: true }, '/admin'), session(true, true))).toBe(true)
  })
})
