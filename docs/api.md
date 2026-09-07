# JSON API

应用上下文为 `/personal_blog_system_war_exploded`，下表路径均相对该上下文。

## 统一响应

```ts
type ApiResponse<T> =
  | { success: true; data: T; message?: string }
  | {
      success: false
      error: {
        code: string
        message: string
        fieldErrors?: Record<string, string>
      }
    }
```

常见错误码：`INVALID_JSON`、`VALIDATION_ERROR`、`AUTH_REQUIRED`、`ADMIN_REQUIRED`、`FORBIDDEN`、`CSRF_INVALID`、`NOT_FOUND`、`INTERNAL_ERROR`。

## 会话

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/auth/session` | 当前用户与 CSRF Token |
| POST | `/api/auth/login` | 登录并刷新 CSRF Token |
| POST | `/api/auth/logout` | 注销 Session |
| POST | `/api/auth/register` | 注册 |
| GET | `/api/auth/check-username` | 用户名可用性 |

除读取请求外，客户端必须携带 `X-CSRF-Token`。Token 来自 Session 接口或登录响应。文章写入、评论写入与删除、用户中心、上传和附件下载要求登录；`/api/admin/*` 还要求管理员角色。

## 首页、文章与评论

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/home` | 最新文章、分类、统计 |
| GET | `/api/articles` | `page`、`pageSize`、`q`、`category` 分页查询 |
| GET | `/api/articles/{id}` | 文章与评论 |
| POST | `/api/articles` | 新建文章 |
| PUT | `/api/articles/{id}` | 更新本人文章或管理员更新 |
| DELETE | `/api/articles/{id}` | 删除本人文章或管理员删除 |
| POST | `/api/articles/{id}/comments` | 发表评论 |
| DELETE | `/api/comments/{id}` | 删除本人评论或管理员删除 |

## 用户中心与上传

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET / PUT | `/api/me` | 获取资料，或更新昵称、邮箱和最多 200 字的个人寄语 |
| PUT | `/api/me/password` | 修改密码 |
| POST | `/api/me/avatar` | 上传头像 |
| GET | `/api/me/articles` | 当前用户文章分页 |
| POST | `/api/uploads/images` | 富文本图片，最大 5 MB |
| POST | `/api/uploads/files` | 附件，最大 10 MB |
| GET | `/api/files/{name}/download` | 下载附件 |

上传成功仍返回 HTTP `201`，数据字段保持兼容：

```ts
interface UploadResult {
  storedName: string       // 兼容现有物理前缀，例如 image_<uuid>.png
  originalName: string
  url: string
  contentType: string | null
  size: number
}
```

上传服务会同时登记 `media_asset` 的 `TEMP` 元数据；文章保存或头像替换成功后才登记业务引用。该生命周期登记不新增前端端点和请求字段，附件下载仍要求登录，且仍使用 UUID 文件名而不是原始文件名。

## 管理后台

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/admin/dashboard` | 仪表盘 |
| GET | `/api/admin/users` | 用户分页 |
| PUT | `/api/admin/users/{id}/status` | 启用/禁用用户 |
| DELETE | `/api/admin/users/{id}` | 删除用户 |
| GET | `/api/admin/articles` | 文章分页 |
| DELETE | `/api/admin/articles/{id}` | 删除文章 |
| POST | `/api/admin/articles/batch-delete` | 批量删除 |
| GET | `/api/admin/articles/export` | CSV 导出 |
| GET / POST | `/api/admin/categories` | 分类列表/新建 |
| PUT / DELETE | `/api/admin/categories/{id}` | 更新/删除分类 |

## 分页

```ts
interface PageResult<T> {
  items: T[]
  page: number
  pageSize: number
  total: number
  totalPages: number
}
```
