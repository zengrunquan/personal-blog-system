import { http, unwrap } from './http'
import type {
  ApiResponse,
  Article,
  Category,
  Comment,
  HomeData,
  PageResult,
  SessionData,
  UploadResult,
  User,
} from './types'

export const authApi = {
  session: () => unwrap(http.get<ApiResponse<SessionData>>('/auth/session')),
  login: (payload: { username: string; password: string }) =>
    unwrap(http.post<ApiResponse<{ user: User; csrfToken: string }>>('/auth/login', payload)),
  logout: () => unwrap(http.post<ApiResponse<{ authenticated: boolean }>>('/auth/logout')),
  register: (payload: { username: string; password: string; nickname: string; email: string }) =>
    unwrap(http.post<ApiResponse<{ username: string }>>('/auth/register', payload)),
  checkUsername: (username: string) =>
    unwrap(
      http.get<ApiResponse<{ available: boolean }>>('/auth/check-username', {
        params: { username },
      }),
    ),
}

export const publicApi = {
  home: () => unwrap(http.get<ApiResponse<HomeData>>('/home')),
  articles: (params: { page?: number; pageSize?: number; q?: string; category?: number }) =>
    unwrap(
      http.get<ApiResponse<{ articles: PageResult<Article>; categories: Category[] }>>(
        '/articles',
        {
          params,
        },
      ),
    ),
  article: (id: number) =>
    unwrap(http.get<ApiResponse<{ article: Article; comments: Comment[] }>>(`/articles/${id}`)),
  addComment: (id: number, content: string) =>
    unwrap(http.post<ApiResponse<{ articleId: number }>>(`/articles/${id}/comments`, { content })),
  deleteComment: (id: number) =>
    unwrap(http.delete<ApiResponse<{ id: number }>>(`/comments/${id}`)),
}

export const articleApi = {
  create: (article: Article) => unwrap(http.post<ApiResponse<Article>>('/articles', article)),
  update: (id: number, article: Article) =>
    unwrap(http.put<ApiResponse<Article>>(`/articles/${id}`, article)),
  delete: (id: number) => unwrap(http.delete<ApiResponse<{ id: number }>>(`/articles/${id}`)),
}

export const meApi = {
  profile: () => unwrap(http.get<ApiResponse<User>>('/me')),
  update: (payload: { nickname: string; email: string; bio: string }) =>
    unwrap(http.put<ApiResponse<User>>('/me', payload)),
  password: (payload: { oldPassword: string; newPassword: string }) =>
    unwrap(http.put<ApiResponse<{ changed: boolean }>>('/me/password', payload)),
  articles: (params: { page?: number; pageSize?: number }) =>
    unwrap(http.get<ApiResponse<PageResult<Article>>>('/me/articles', { params })),
  avatar: (file: File) => {
    const data = new FormData()
    data.append('file', file)
    return unwrap(http.post<ApiResponse<UploadResult>>('/me/avatar', data))
  },
}

export const uploadApi = {
  image: (file: File) => {
    const data = new FormData()
    data.append('file', file)
    return unwrap(http.post<ApiResponse<UploadResult>>('/uploads/images', data))
  },
  file: (file: File) => {
    const data = new FormData()
    data.append('file', file)
    return unwrap(http.post<ApiResponse<UploadResult>>('/uploads/files', data))
  },
}

export interface DashboardData {
  stats: { users: number; articles: number; categories: number; online: number }
  recentArticles: Article[]
}

export const adminApi = {
  dashboard: () => unwrap(http.get<ApiResponse<DashboardData>>('/admin/dashboard')),
  users: (params: { page?: number; pageSize?: number }) =>
    unwrap(http.get<ApiResponse<PageResult<User>>>('/admin/users', { params })),
  setUserStatus: (id: number, status: number) =>
    unwrap(
      http.put<ApiResponse<{ id: number; status: number }>>(`/admin/users/${id}/status`, {
        status,
      }),
    ),
  deleteUser: (id: number) =>
    unwrap(http.delete<ApiResponse<{ id: number }>>(`/admin/users/${id}`)),
  articles: (params: { page?: number; pageSize?: number }) =>
    unwrap(http.get<ApiResponse<PageResult<Article>>>('/admin/articles', { params })),
  deleteArticle: (id: number) =>
    unwrap(http.delete<ApiResponse<{ id: number }>>(`/admin/articles/${id}`)),
  batchDeleteArticles: (ids: number[]) =>
    unwrap(http.post<ApiResponse<{ deleted: number }>>('/admin/articles/batch-delete', { ids })),
  categories: () => unwrap(http.get<ApiResponse<Category[]>>('/admin/categories')),
  createCategory: (payload: Pick<Category, 'name' | 'description' | 'sortOrder'>) =>
    unwrap(http.post<ApiResponse<{ created: boolean }>>('/admin/categories', payload)),
  updateCategory: (id: number, payload: Pick<Category, 'name' | 'description' | 'sortOrder'>) =>
    unwrap(http.put<ApiResponse<Category>>(`/admin/categories/${id}`, payload)),
  deleteCategory: (id: number) =>
    unwrap(http.delete<ApiResponse<{ id: number }>>(`/admin/categories/${id}`)),
}
