# 前后端分离架构  -- 2026年8月25日，重构项目前端

## 模块边界

```text
frontend/src/
├── api/          # Axios、统一响应解包、业务 API
├── components/   # 公共、文章、用户、后台组件
├── config/       # 类型化站点配置
├── router/       # 路由与权限守卫
├── stores/       # Pinia Session 状态
├── styles/       # 设计令牌与全局样式
└── views/        # 公共端、用户端、管理端页面

backend/src/main/java/com/blog/
├── api/
│   ├── dto/      # 对外数据模型，禁止直接暴露 Entity
│   ├── exception/# API 业务异常
│   ├── response/ # ApiResponse、ApiError、PageResult
│   ├── security/ # Session、CSRF、API 权限过滤器
│   ├── servlet/  # JSON API 入口
│   ├── support/  # DTO 映射、HTML 清洗
│   └── upload/   # 上传策略与存储
├── web/          # SPA fallback 与旧地址重定向
├── service/      # 业务逻辑
├── dao/          # JDBC 数据访问
└── entity/       # 仅后端内部使用
```

## 请求链路

```text
Vue View
  │
  ├─ Pinia Session ── GET /api/auth/session
  │
  └─ Axios
      ├─ 自动携带 JSESSIONID
      ├─ 写请求携带 X-CSRF-Token
      ▼
ApiSecurityFilter
      ▼
API Servlet → 参数校验 → Service → DAO → MySQL
      ▼
DTO Mapper → ApiResponse<T> → JSON
```

## 生产路由

- `/api/*`：JSON API，不进入 SPA fallback。
- `/uploads/*`、静态资源、下载、导出：保持资源/接口语义。
- Vue 的公共、用户与后台 GET 路由：转发到 `index.html`。
- 旧页面地址：先 `301` 到新的 Vue 路由。
- Vue 构建产物由 Maven WAR 插件从 `frontend/dist` 合并到 WAR 根目录；产物名为 `personal_blog_system_war_exploded.war`，与 Vite 生产基础路径及 Tomcat 上下文一致。

## 上传存储

```text
POST /api/me/avatar ───────┐
POST /api/uploads/images ──┼─→ MediaUploadService
POST /api/uploads/files ────┘       │
                                    ├─ 原子写入 <项目根目录>\docs\uploads
                                    ├─ 插入 media_asset
                                    └─ TEMP / 24h → 引用后 ACTIVE
                                             │
                                             ├─ image\avatar_<UUID>.jpg
                                             ├─ image\image_<UUID>.webp
                                             └─ file\file_<UUID>.pdf
```

- 上传根目录可由 JVM 参数 `blog.upload.dir` 或环境变量 `BLOG_UPLOAD_DIR` 覆盖，默认使用 `<项目根目录>\docs\uploads`。代码会从当前工作目录和类加载位置向上识别包含 `backend/pom.xml` 与 `frontend/package.json` 的项目根目录，避免 IDEA/Tomcat 工作目录变化导致写入位置漂移。
- 应用首次访问存储时调用 `Files.createDirectories` 自动创建上传根目录及 `image`、`file` 子目录，并显式检查每个目录的类型和写权限。
- 头像与文章图片存入 `image/`，附件存入 `file/`；物理文件名继续使用 `avatar_`、`image_`、`file_` 前缀，以保持现有 URL 映射并进一步明确类型边界。
- 数据库仍只保存无物理前缀的头像 URL 和文章 HTML 中的资源 URL，不保存文件二进制；公共媒体和附件接口负责把 URL 文件名映射到对应物理前缀，因此物理目录变化不影响浏览器 URL。
- `media_asset` 记录媒体类型、物理名、URL 文件名、原始名、MIME、大小、SHA-256、上传人和生命周期状态；`media_reference` 记录 `USER_AVATAR`、`ARTICLE_CONTENT`、`ARTICLE_COVER` 引用。`ARTICLE_CONTENT` 允许头像、文章图片和附件，`ARTICLE_COVER` 只允许文章图片，`USER_AVATAR` 只允许头像。文章保存会在同一 JDBC 事务内更新文章和引用，数据库失败后上传服务补偿删除新文件。
- 文章/用户/分类级联删除只移除引用，绝不在请求线程删除物理文件；零引用 `ACTIVE` 进入 `DELETE_PENDING`，清理器提交认领后在事务外删除，再以 claim token 标记 `DELETED` 或 `DELETE_FAILED`。共享文件只有最后一个引用消失并经过缓冲期才会删除。
- 历史回填默认 dry-run；`--apply` 只新增元数据、补齐引用，不移动、重命名或删除历史文件。回填必须解析到已存在且可读的上传根目录，无法确认根目录或读取用户/文章数据失败时立即停止；报告会带规范化绝对路径和安全的数据源摘要。无法识别的文件进入人工复核，`LEGACY_PROTECTED` 与 `MISSING_BINARY` 永远不自动删除。
- 上传目录位于源码项目的 `docs/uploads/`，但独立于 Maven `target`、WAR 和 Tomcat `docBase`，重新构建或部署不会清理业务文件；该目录被 Git 忽略。

## 安全边界

- Session 中只保存登录用户；客户端不保存密码或哈希。
- API 只返回 DTO，`UserDto` 没有密码字段。
- 普通用户接口与管理员接口由 `ApiSecurityFilter` 分层校验。
- 文章更新/删除在服务端检查作者或管理员身份，不能只依赖路由守卫。
- 富文本写入前使用服务端白名单清洗，展示前再经 DOMPurify。
- 图片允许 JPEG、PNG、GIF、WebP，最大 5 MB；附件最大 10 MB。
- 私有附件的物理文件使用 `file_` 前缀，只能经鉴权下载接口以 attachment 响应读取；公共媒体接口根据 URL 分类只映射 `avatar_` 或 `image_` 物理文件。
- 登录成功会销毁匿名 Session 并创建新的认证 Session，同时生成新的 CSRF Token。
- 真实数据库配置、数据库备份、环境变量和 `docs/uploads/` 运行时数据均只保留在部署环境并单独备份，不进入 Git；仓库只提供脱敏的数据库配置示例。

## 设计系统

- 暖白纸张背景、墨黑正文、朱红强调与细分隔线。
- 中文衬线字体用于标题，系统无衬线字体用于正文和高密度后台。
- 公共端使用编辑排版布局；后台使用侧栏、表格、筛选与明确操作层级。
- 不使用渐变、玻璃拟态、CSS 假插画或重复卡片墙。
