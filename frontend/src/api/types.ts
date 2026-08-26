export type ApiResponse<T> =
  | { success: true; data: T; message?: string }
  | {
      success: false
      error: { code: string; message: string; fieldErrors?: Record<string, string> }
    }

export interface PageResult<T> {
  items: T[]
  page: number
  pageSize: number
  total: number
  totalPages: number
}

export interface User {
  id: number
  username: string
  nickname: string
  email: string
  avatar?: string
  bio?: string
  role: number
  status: number
  createTime?: string
}

export interface Article {
  id?: number
  title: string
  content: string
  summary?: string
  coverImage?: string
  userId?: number
  categoryId: number
  viewCount?: number
  status: number
  createTime?: string
  updateTime?: string
  authorName?: string
  authorNickname?: string
  authorAvatar?: string
  categoryName?: string
  commentCount?: number
}

export interface Category {
  id: number
  name: string
  description?: string
  sortOrder?: number
  articleCount?: number
}

export interface Comment {
  id: number
  content: string
  userId: number
  articleId: number
  createTime: string
  userNickname?: string
  userAvatar?: string
  username?: string
}

export interface HomeData {
  featuredArticles: Article[]
  categories: Category[]
  stats: { articles: number; categories: number; authors: number }
}

export interface SessionData {
  authenticated: boolean
  user: User | null
  csrfToken: string
}

export interface UploadResult {
  storedName: string
  originalName: string
  url: string
  contentType: string
  size: number
}

export class AppError extends Error {
  constructor(
    message: string,
    public readonly code = 'UNKNOWN_ERROR',
    public readonly fieldErrors?: Record<string, string>,
    public readonly status?: number,
  ) {
    super(message)
    this.name = 'AppError'
  }
}
