# 升级与兼容说明

本指南面向从旧版本升级的使用者。新建开发环境请从 [README](../README.md) 开始；数据库是否需要迁移取决于目标库结构，不能根据文档推断已经执行过迁移。

## 数据库升级

先停止应用、关闭媒体清理器，并备份数据库与上传目录。不要对已有数据库重新运行 `init-database.sql`。

| 目标结构 | 迁移文件 |
| --- | --- |
| 用户个人寄语 `user.bio` | [新增 bio 字段](../database/migrations/2026-08-25-add-user-bio.sql) |
| 媒体资产与引用表 | [媒体生命周期结构](../database/migrations/2026-08-31-add-media-lifecycle.sql) |

先核对已有结构，再选择所需迁移；不要盲目重复执行。媒体历史回填、人工核对和启用清理的顺序见[媒体运行手册](media-lifecycle-runbook.md)。数据库备份和迁移结果应保存在自己的维护记录中，不提交真实数据。

## 页面与接口兼容

- 页面统一由 Vue 3 提供，原 JSP 和页面型 Servlet 已移除。旧页面 GET 地址按路由策略重定向；旧写地址返回 `410`，旧 GET 注销也被禁用。
- `FrontendRoutePolicy` 与 `SpaRoutingFilter` 维护兼容和 SPA fallback；API、上传、静态资源、下载及导出不会回落到 Vue 页面。
- 前后端使用同域 Session、`JSESSIONID` 与 CSRF Token。调用方应使用当前 [JSON API](api.md)，不要继续提交旧页面表单。
- 生产应用上下文固定为 `/personal_blog_system_war_exploded`，前端构建与 WAR 部署需保持一致。
- 开发运行环境为 JDK 21 和外部 Tomcat 9，配置见[工具链指南](java-maven-toolchain.md)。

## 历史上传文件

持久化目录默认是项目的 `docs/uploads/`，可通过 `BLOG_UPLOAD_DIR` 或 JVM 参数 `blog.upload.dir` 覆盖；后者优先。离开源码目录部署时必须显式配置。不要把上传文件继续放在 Maven 或 Tomcat 的构建展开目录中。

若旧文件仍有备份，在停止应用、备份数据库和文件后，按类型复制到当前上传根目录：

```text
旧 uploads/avatars/<name>              → <upload-root>/image/avatar_<name>
旧 uploads/images/<name>               → <upload-root>/image/image_<name>
旧 WEB-INF/private-uploads/files/<name> → <upload-root>/file/file_<name>
旧 uploads/files/<name>                → <upload-root>/file/file_<name>
```

只有物理文件增加类型前缀，数据库 URL 仍使用原稳定文件名。迁移后核对数量、大小及哈希；若目标文件已存在，先核对内容，不能直接覆盖。仅有数据库 URL 无法恢复已经丢失的二进制文件。

附件原始名称来自 `media_asset.original_name`，只影响下载建议名，不改变 UUID URL、物理路径或登录权限。缺少有效原名时使用稳定文件名；查询失败返回错误。验证方法见[附件下载验证指南](attachment-download-verification.md)。

## 升级后验证

先运行前后端测试并构建 WAR，再检查页面、会话、旧地址跳转和已有媒体读取。写入型 E2E、迁移及清理验收应在独立测试环境进行；单元测试不能替代真实数据库外键、并发认领和物理文件验证。