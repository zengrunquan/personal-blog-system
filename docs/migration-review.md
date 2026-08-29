# Vue 3 迁移审查记录 -- 2026年8月25日，重构项目前端

## 已完成

- 项目拆分为 `backend/` 与 `frontend/`，原有表关系、DAO 和核心 Service 保持不变；后续仅为用户个人寄语新增可空的 `user.bio` 字段。
- 公共端、用户中心、富文本编辑器和管理后台均已实现 Vue 路由。
- 新增 JSON API、DTO 脱敏、统一错误、Session/CSRF、HTML 清洗和上传策略。
- 旧 URL 兼容跳转与 SPA fallback 已覆盖，API、上传、静态资源、下载和导出被排除。
- Vue 构建结果已自动打入 WAR；Maven Wrapper 固定为 3.9.16。
- 视觉在 1024×1536 参考尺寸、1440×1024 和 390×844 下完成截图验收。

## 本次迁移中发现并修复的回归

1. Gson 省略空 `user` 时，前端曾把 `undefined` 错判为已登录；会话接口现稳定返回 `user: null`，Pinia 也做防御性归一化，并新增回归测试。
2. 首页 Hero 初版过高且标题在 1024 px 下换行；已重新校准网格、字号、插画比例与区块密度。
3. 文章列表清空搜索的模板内多语句被格式化后无法解析；已收敛为类型化方法并通过生产构建。
4. 缺少 favicon 导致浏览器控制台出现 404；已复用真实作者图作为站点图标。
5. 审查发现附件若直接作为静态资源公开可能形成同源存储型 XSS；附件下载地址强制转到鉴权接口。后续持久化改造将头像和文章图片存入项目 `docs/uploads/image/`，附件存入 `docs/uploads/file/`，并用目录与物理前缀共同阻止附件经公共图片接口读取。
6. 清理旧页面型 Servlet 前，旧 POST 曾可能绕过新 CSRF 边界；SPA 过滤器现统一以 `410 LEGACY_ENDPOINT_DISABLED` 阻断旧写地址，兼容策略不再依赖旧控制器。
7. Vite 开发代理现重写 Session Cookie Path，并代理生产上下文资源地址，开发模式可稳定复用 `JSESSIONID`。
8. SPA fallback 已改为负向排除 API、上传、静态资源、下载和导出，使未知无扩展名地址也进入 Vue 404。
9. 登录成功会轮换整个 Session 与 CSRF Token，避免 Session fixation；前端用户类型也已与 `UserDto` 的 `role` 字段对齐。
10. 旧公开附件地址现同时按原始 URI 与容器解码路径拦截整个 `/uploads/files` 命名空间，编码点号、矩阵参数和路径穿越均不能回落到静态资源 Servlet。
11. 删除旧控制器后补齐真实历史入口：分类表单转到 Vue 分类页，旧下载与导出转到受鉴权 API；旧 GET 注销因会修改 Session 而明确返回 `410 LEGACY_GET_DISABLED`。
12. 管理端现行 Vue 路由不再被旧路由表重复匹配，避免 `/admin/users`、`/admin/articles`、`/admin/categories` 自重定向；分类页会读取兼容查询参数并恢复新增或编辑表单。
13. 首页“关于我”改为读取当前用户头像、邮箱和个人寄语；访客使用默认头像与默认寄语并隐藏邮箱，未编辑资料的登录用户同样使用默认头像和寄语。个人资料编辑页新增最多 200 字的寄语字段。
14. 发布前复核发现 Maven 产物名与前端固定上下文不一致；WAR 现统一命名为 `personal_blog_system_war_exploded.war`，并补齐真实数据库配置、环境变量和私有附件目录的 Git 忽略边界。
15. 上传文件原先经 `ServletContext.getRealPath` 写入 exploded WAR，重新构建或部署会留下数据库 URL 但删除物理文件；现改为可配置的持久化目录，缺失的根目录及 `image`、`file` 子目录自动创建，默认根路径为 `<项目根目录>\docs\uploads`。项目根目录通过源码标识文件稳定定位，不依赖 IDEA/Tomcat 当前工作目录；数据库 URL 保持无前缀文件名，磁盘文件按类型进入对应子目录并增加前缀，历史备份可迁移恢复而无需修改数据库。

## 验证边界

- 后端单元测试覆盖统一响应、CSRF、权限过滤、HTML 清洗、上传策略、路由策略、密码与依赖注入。
- 前端单元测试覆盖响应解包、Session、路由守卫、文章组件、首页资料展示和个人资料查看/编辑。
- 最终验证结果：清理旧页面专用测试并补充兼容回归测试后，后端 82 个测试、前端 13 个 Vitest、TypeScript 严格检查、生产构建和 WAR 打包全部通过。前端 ESLint 的 CRLF/Prettier 基线已在后续提交 `b6e5b17` 中统一为 LF，零警告门禁现已恢复。
- Playwright 在桌面与 390 px 手机项目共执行 8 项：7 项通过，1 项按设计仅在手机项目运行而在桌面项目跳过。
- Playwright 冒烟测试只读取当前数据库；写入型注册、资料、文章、评论和后台操作需配置独立测试库后执行。
- 本次在完整备份当前数据库后，仅执行 `database/migrations/2026-08-25-add-user-bio.sql`，为 `user` 表增加可空的 `bio VARCHAR(200)` 字段；没有运行 `init-database.sql`。

## 最终清理

经用户明确授权，旧 JSP、页面型 Servlet、`AuthFilter`/`AdminFilter`、旧页面专用测试与静态资源已删除；JSP API、JSTL 及仅由旧页面控制器使用的 POI、Commons FileUpload、Commons IO 依赖也已移除。`web.xml` 只保留全局 UTF-8 编码、Session 时长和 Vue `index.html` 欢迎页配置。发布候选文件不包含真实数据库配置、数据库备份、本机验收记录或运行时上传内容。

旧 GET 地址的 `301` 兼容与旧写地址的 `410` 阻断继续由 `FrontendRoutePolicy` 和 `SpaRoutingFilter` 维护，因此历史链接仍可迁移到 Vue 页面，但仓库不再包含可执行的 JSP 页面分支。
