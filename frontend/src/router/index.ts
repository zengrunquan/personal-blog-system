import { createRouter, createWebHistory, type RouteLocationNormalized } from 'vue-router'
import { useSessionStore } from '@/stores/session'

const routes = [
  { path: '/', name: 'home', component: () => import('@/views/public/HomeView.vue') },
  {
    path: '/articles',
    name: 'articles',
    component: () => import('@/views/public/ArticlesView.vue'),
  },
  {
    path: '/articles/:id(\\d+)',
    name: 'article-detail',
    component: () => import('@/views/public/ArticleDetailView.vue'),
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { guestOnly: true },
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/auth/RegisterView.vue'),
    meta: { guestOnly: true },
  },
  {
    path: '/me',
    name: 'me',
    component: () => import('@/views/user/ProfileView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/me/edit',
    name: 'me-edit',
    component: () => import('@/views/user/ProfileEditView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/me/password',
    name: 'me-password',
    component: () => import('@/views/user/PasswordView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/me/articles',
    name: 'me-articles',
    component: () => import('@/views/user/MyArticlesView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/editor/new',
    name: 'editor-new',
    component: () => import('@/views/user/EditorView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/editor/:id(\\d+)',
    name: 'editor-edit',
    component: () => import('@/views/user/EditorView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/admin',
    name: 'admin',
    component: () => import('@/views/admin/DashboardView.vue'),
    meta: { requiresAdmin: true },
  },
  {
    path: '/admin/users',
    name: 'admin-users',
    component: () => import('@/views/admin/UsersView.vue'),
    meta: { requiresAdmin: true },
  },
  {
    path: '/admin/articles',
    name: 'admin-articles',
    component: () => import('@/views/admin/ArticlesAdminView.vue'),
    meta: { requiresAdmin: true },
  },
  {
    path: '/admin/categories',
    name: 'admin-categories',
    component: () => import('@/views/admin/CategoriesView.vue'),
    meta: { requiresAdmin: true },
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/public/NotFoundView.vue'),
  },
]

export function guardRoute(
  to: RouteLocationNormalized,
  session: ReturnType<typeof useSessionStore>,
) {
  const redirect = typeof to.fullPath === 'string' ? to.fullPath : '/'
  if (to.meta.requiresAdmin && !session.isAdmin)
    return { name: session.authenticated ? 'home' : 'login', query: { redirect } }
  if (to.meta.requiresAuth && !session.authenticated) return { name: 'login', query: { redirect } }
  if (to.meta.guestOnly && session.authenticated) return { name: 'home' }
  return true
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior: (_to, _from, savedPosition) => savedPosition ?? { top: 0 },
})

router.beforeEach(async (to) => {
  const session = useSessionStore()
  await session.bootstrap()
  return guardRoute(to, session)
})

export default router
