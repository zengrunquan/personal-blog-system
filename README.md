# 个人博客系统

一个前后端分离的个人博客：读者端、用户中心和管理后台均使用 Vue 3，后端保留 Java 11、Servlet、Service、DAO、JDBC、MySQL 与 `JSESSIONID` Session。

## 架构

```text
浏览器（Vue 3 SPA）
       │ JSON / X-CSRF-Token / JSESSIONID
       ▼
Servlet JSON API → Service → DAO → MySQL
       │
       └─ 生产环境把 frontend/dist 打入同一个 WAR
```

```text
personal-blog-system/
├── backend/               # Java 11、Servlet、Service、DAO、JSON API
├── frontend/              # Vue 3、TypeScript、Vite、Router、Pinia
├── docs/                  # 架构、接口与迁移审查记录
├── init-database.sql
└── README.md
```

详细设计见 [docs/architecture.md](docs/architecture.md)，API 见 [docs/api.md](docs/api.md)。

## 技术栈

- 前端：Vue 3、TypeScript strict、Vite、Vue Router、Pinia、Axios、TipTap、DOMPurify、Lucide、Vitest、Playwright
- 后端：Java 11、Servlet 4、JDBC、Druid、Log4j2、Gson、OWASP Java HTML Sanitizer
- 数据库：MySQL 8，保留原有表关系，并通过迁移脚本新增可空的 `user.bio` 字段
- 会话：同域 `JSESSIONID`，写请求使用 `X-CSRF-Token`

## 主要功能

- 公共端：首页、文章分页、查询参数搜索/分类、文章详情与评论
- 用户端：登录注册、资料/头像/密码、个人文章、富文本新建与编辑
- 管理端：仪表盘、用户、文章、批量删除、CSV 导出、分类管理
- 安全：DTO 脱敏、Session 权限、管理员边界、文章所有权、CSRF、前后端双重 HTML 清洗、上传 MIME 与大小限制
- 附件：私有目录保存并只允许鉴权下载，避免 HTML 等主动内容被同源直接执行
- 兼容：旧 JSP 地址通过 `301` 跳转到对应 Vue 路由；前端 GET 路由使用 SPA fallback

## 本地开发

### 1. 数据库

如需新建开发库，可使用：

```bash
mysql -u root -p < init-database.sql
```

```powershell
Copy-Item backend/src/main/resources/db.properties.example `
  backend/src/main/resources/db.properties
```

以 [backend/src/main/resources/db.properties.example](backend/src/main/resources/db.properties.example) 为模板，然后编辑本地的 `backend/src/main/resources/db.properties` 数据库连接；真实配置已被 Git 忽略。初始化脚本已包含 `bio` 字段；已有数据库需在完整备份后执行 [database/migrations/2026-08-25-add-user-bio.sql](database/migrations/2026-08-25-add-user-bio.sql)。初始化脚本和迁移脚本均不会自动执行。

### 2. 启动后端

Windows：

```powershell
cd backend
.\mvnw.cmd tomcat7:run-war
```

macOS / Linux：

```bash
cd backend
./mvnw tomcat7:run-war
```

### 3. 启动前端开发服务器

```bash
cd frontend
pnpm install
pnpm dev
```

Vite 会把 `/api` 与 `/uploads` 代理到 Tomcat 的 `/personal_blog_system_war_exploded` 上下文。

### 上传文件存储

头像、文章图片和附件不会写入 Maven 的 `target` 或 Tomcat 展开目录，而是按媒体类型存放到项目根目录下的运行时目录。默认结构为：

```text
<personal-blog-system 项目根目录>\docs\uploads
├── image\   # 头像 avatar_*、文章图片 image_*
└── file\    # 附件 file_*
```

在当前开发机上，上传根目录的绝对路径是 `D:\work\local_repository\javaweb\zrq_231124081\personal-blog-system\docs\uploads`。应用会从当前工作目录和类加载位置向上识别项目根目录，并在首次上传或读取文件时自动创建根目录及 `image`、`file` 子目录；因此 IDEA 重新构建或重新部署 exploded WAR 不会改变默认位置。物理文件通过目录和前缀双重分类：头像使用 `image\avatar_*`、文章图片使用 `image\image_*`、私有附件使用 `file\file_*`；数据库和浏览器 URL 仍使用不带物理前缀的 UUID 文件名。头像和文章图片继续通过 `/uploads/*` 读取，附件只能通过要求登录的 `/api/files/{name}/download` 下载。

其他开发机或部署环境可通过环境变量覆盖默认目录：

```powershell
$env:BLOG_UPLOAD_DIR='E:\blog-data\uploads'
```

也可以使用 JVM 参数 `-Dblog.upload.dir=E:\blog-data\uploads`；JVM 参数优先于环境变量。若部署包已离开源码目录，必须使用其中一种方式明确配置路径。`docs/uploads/` 已加入 `.gitignore`，其中的运行时业务数据不提交到 Git，生产环境仍应单独备份。

历史文件若仍有备份，可在停止应用并备份数据库后复制到新目录，同时只给物理文件添加类型前缀，数据库 URL 不需要修改：

```text
旧 uploads/avatars/<name>              → <upload-root>/image/avatar_<name>
旧 uploads/images/<name>               → <upload-root>/image/image_<name>
旧 WEB-INF/private-uploads/files/<name> → <upload-root>/file/file_<name>
旧 uploads/files/<name>                → <upload-root>/file/file_<name>
```

迁移后应按文件数量、大小或哈希核对备份与新目录。只有数据库 URL、没有原始文件或文件备份时，无法恢复已丢失的二进制内容。

## 生产构建

```powershell
cd frontend
pnpm build

cd ..\backend
.\mvnw.cmd clean test package
```

产物为 `backend/target/personal_blog_system_war_exploded.war`，其中包含预先生成的 Vue 构建结果。WAR 文件名、Vite 生产基础路径和 Tomcat Maven 插件均统一使用以下应用上下文：

```text
/personal_blog_system_war_exploded
```

## 验证

```powershell
cd frontend
pnpm lint
pnpm test
pnpm build

$env:E2E_BASE_URL='http://localhost:8080/personal_blog_system_war_exploded/'
pnpm e2e

cd ..\backend
.\mvnw.cmd clean test package
```

会写数据库的完整 CRUD E2E 必须使用独立测试库；默认冒烟测试只执行读取、搜索、旧地址跳转和响应式导航，不改动当前数据库。

## 测试账号

> **安全提示：以下账号仅限本地开发。首次运行后必须立即修改默认密码，禁止用于生产环境。**

| 角色 | 用户名 | 密码 |
| --- | --- | --- |
| 管理员 | `admin` | `admin123` |
| 普通用户 | `zhangsan` | `user123` |
| 普通用户 | `lisi` | `user123` |

## 迁移说明

- 当前运行数据库已在完整备份后新增可空的 `user.bio` 字段，其他表结构与现有数据保持不变。
- Vue 是新的页面入口，生产资源与后端同域，不使用 JWT 或跨域配置。
- 原页面型 Servlet、JSP、旧页面专用 Filter/测试及 JSP/JSTL 等遗留依赖均已移除；旧地址兼容由独立路由策略维护，复核结论见 [docs/migration-review.md](docs/migration-review.md)。
- 不要提交 `frontend/dist`、`backend/target`、本地上传文件、数据库密码或 IDE 配置。

## 后端工程约束

- 密码使用成本因子 12 的 BCrypt 哈希；旧 MD5 账号仅用于兼容，登录成功后会自动升级为 BCrypt。
- DAO 和 DBUtil 执行统一数据库错误日志策略，错误日志包含类名、方法名、操作上下文和异常堆栈。
- Service 构造器依赖注入用于替换 DAO 依赖并提高可测试性，同时保留供 Servlet 装配的无参构造器。
- 仓库卫生规则：不提交 `.idea/`、`target/`、`*.class`、真实 `db.properties`、`.env`、数据库备份和 `docs/uploads/` 运行时上传数据；历史 `src/main/webapp/uploads/`、`backend/src/main/webapp/uploads/` 及对应的 `WEB-INF/private-uploads/` 目录仍被忽略，避免旧运行文件被误提交。
- 本机打包的 WAR 会包含本地数据库配置，只能用于本机部署；不要把该 WAR 上传到 GitHub Releases 或交给其他环境。如需分发可部署产物，应先把数据库凭据改为由部署环境外部注入。

## 许可证

本项目采用 [MIT License](LICENSE)，允许在保留版权与许可声明的前提下使用、修改和分发。
