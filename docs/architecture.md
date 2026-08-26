# 前后端分离架构

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

## 安全边界

- Session 中只保存登录用户；客户端不保存密码或哈希。
- API 只返回 DTO，`UserDto` 没有密码字段。
- 普通用户接口与管理员接口由 `ApiSecurityFilter` 分层校验。
- 文章更新/删除在服务端检查作者或管理员身份，不能只依赖路由守卫。
- 富文本写入前使用服务端白名单清洗，展示前再经 DOMPurify。
- 图片允许 JPEG、PNG、GIF、WebP，最大 5 MB；附件最大 10 MB。
- 附件保存在 `WEB-INF/private-uploads`，只能经鉴权下载接口以 attachment 响应读取；旧公开附件地址会重定向到该接口。
- 登录成功会销毁匿名 Session 并创建新的认证 Session，同时生成新的 CSRF Token。
- 真实数据库配置、数据库备份、环境变量、公开上传文件和 `WEB-INF/private-uploads` 私有附件均只保留在本机并由 Git 忽略；仓库只提供脱敏的数据库配置示例。

## 设计系统

- 暖白纸张背景、墨黑正文、朱红强调与细分隔线。
- 中文衬线字体用于标题，系统无衬线字体用于正文和高密度后台。
- 公共端使用编辑排版布局；后台使用侧栏、表格、筛选与明确操作层级。
- 不使用渐变、玻璃拟态、CSS 假插画或重复卡片墙。
