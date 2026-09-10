# 个人博客系统

基于 Vue 3 与 Java Servlet 的前后端分离博客，包含文章阅读与评论、用户中心、富文本编辑器和管理后台。

```text
Vue 3 / TypeScript
        │ JSON、Session、CSRF Token
        ▼
Servlet → Service → DAO / JDBC → MySQL
```

生产构建将前端资源合并到同一个 WAR；开发时使用 Vite 代理访问后端。

## 项目结构与环境

```text
personal-blog-system/
├── backend/          # Servlet、Service、DAO、测试与 Maven Wrapper
├── frontend/         # Vue 3、TypeScript、Vite、Pinia、Router
├── scripts/          # Windows 开发启动脚本及脚本测试
├── database/         # 数据库迁移
├── docs/             # 架构、接口、工具链与维护指南
└── init-database.sql # 新开发库初始化数据
```

| 环境 | 要求 |
| --- | --- |
| Java | JDK 21 用于开发、测试和运行；Java 11 为编译目标 |
| Servlet 容器 | 外部 Tomcat 9（Servlet 4 / `javax.servlet`） |
| Maven | 使用 `backend` 中的 Maven Wrapper |
| 前端 | Node.js、pnpm；pnpm 版本见 `frontend/package.json` 的 `packageManager` |
| 数据库 | MySQL 8；MariaDB 使用说明见[兼容性文档](docs/xampp-media-cleanup-fix.md) |
| 命令行启动脚本 | Windows PowerShell 5.1 或 PowerShell 7 |

## 开始使用

以下相对路径均以本仓库根目录为基准。

1. **准备数据库。** 仅在新建开发库时使用 [init-database.sql](init-database.sql)。在项目根目录启动 `mysql -u root -p`，进入 MySQL 客户端后执行 `SOURCE init-database.sql;`。已有数据库先阅读[升级与兼容说明](docs/migration-review.md)，不要重新初始化。
2. **配置连接。** 将 [db.properties.example](backend/src/main/resources/db.properties.example) 复制为同目录的 `db.properties`，填写自己的数据库连接。真实配置被 Git 忽略。
3. **构建并启动后端。** 按[工具链指南](docs/java-maven-toolchain.md)设置 JDK 21 和 Tomcat 9 路径，依次安装前端依赖、构建前端、运行 `mvnw.cmd package`，最后执行 `scripts/dev-backend.ps1`。脚本不会自动构建。
4. **访问应用。** 默认地址为 `http://localhost:8080/personal_blog_system_war_exploded/`。IDEA 配置、仅检查环境的 `-Check` 用法和端口排错也见工具链指南。

如需前端热更新，在另一个终端从项目根目录执行：

```powershell
cd frontend
pnpm dev
```

访问 Vite 输出的地址。开发代理固定连接后端 8080 端口，并使用上述应用上下文；后端仍需先启动。IDEA 与命令行不要同时占用 8080。

初始化数据提供以下**仅限本地学习**的账号，部署前必须修改默认密码：

| 角色 | 用户名 | 密码 |
| --- | --- | --- |
| 管理员 | `admin` | `admin123` |
| 普通用户 | `zhangsan` | `user123` |
| 普通用户 | `lisi` | `user123` |

## 数据与开发约定

- 上传文件默认存于项目内的 `docs/uploads/image/` 和 `docs/uploads/file/`，不随 WAR 重建而删除。部署到源码目录之外时，必须通过 `BLOG_UPLOAD_DIR` 或 `-Dblog.upload.dir` 指定持久化目录；JVM 参数优先。附件需要登录下载，并以安全处理后的原始文件名作为建议下载名。
- 媒体清理涉及真实数据。开发启动脚本强制关闭清理器；其他启动方式在备份、迁移及回填核对完成前也应关闭。详细配置和操作见[媒体运行手册](docs/media-lifecycle-runbook.md)。
- 密码使用 BCrypt；旧 MD5 账号登录成功后会自动升级。写接口使用 Session 权限和 CSRF 校验。
- DAO 和 DBUtil 使用统一数据库错误日志，保留操作上下文与异常堆栈；Service 构造器依赖注入用于替换依赖和测试。
- 应提交脚本源码、测试、`backend/.mvn/jvm.config` 和项目文档。不提交 `.idea/`、`target/`、`*.class`、`frontend/dist/`、真实数据库配置、备份及 `docs/uploads/`；历史 `src/main/webapp/uploads/` 也继续忽略。
- 本地 WAR 会包含本地数据库配置，不应直接作为公开下载产物分发；部署凭据需按目标环境管理。

## 测试与文档

前端在 `frontend` 目录执行 `pnpm lint`、`pnpm test`；后端在 `backend` 目录使用 JDK 21 执行 `mvnw.cmd test`。`mvnw.cmd package` 自身也会运行测试。完整 CRUD E2E 和数据库兼容测试须使用独立测试库，不能把单元测试通过视为完成部署验收。

| 文档 | 内容 |
| --- | --- |
| [工具链指南](docs/java-maven-toolchain.md) | 环境、构建、启动、IDEA、编码与测试 |
| [架构说明](docs/architecture.md) | 模块边界、会话、安全与存储设计 |
| [API 文档](docs/api.md) | 请求、响应及接口约定 |
| [升级与兼容说明](docs/migration-review.md) | 已有数据库、旧 URL 与历史上传文件升级 |
| [媒体运行手册](docs/media-lifecycle-runbook.md) | 备份、回填、清理配置与维护验收 |
| [数据库兼容性说明](docs/xampp-media-cleanup-fix.md) | MySQL / MariaDB 的清理认领与兼容测试 |
| [附件下载验证指南](docs/attachment-download-verification.md) | 下载文件名、权限与内容一致性验证 |

文档描述当前用法；后续更新应修改对应章节，避免在 README 追加临时排错过程或重复验收记录。

## 许可证

[MIT License](LICENSE)。