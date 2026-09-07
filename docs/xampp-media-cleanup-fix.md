# XAMPP 媒体清理兼容修复（2026-09-07）

## 环境和原因

本机实际路径为 `D:\dev_tools\mysql\XAMPP`。`mysql\bin\mysqld.exe` 为 MariaDB 10.4.32，`mysql_ma\bin\mariadbd.exe` 为 10.6.28；项目 JDBC 连接返回 `5.5.5-10.4.32-MariaDB`。下载新版本并不会让正在运行的服务自动升级。

MariaDB 10.4 不支持 `FOR UPDATE SKIP LOCKED`。清理器在应用启动 1 分钟后运行，默认后续每隔 60 分钟运行，与登录接口没有直接调用关系。

## 修复

- 认领查询改用 `FOR UPDATE`，保留事务、条件更新、无引用检查和 claim token。多实例会等待行锁，不能再假定会跳过锁；锁等待超时由事务管理器回滚。
- 准备阶段或认领失败继续向调度器抛出异常，记录“媒体清理轮次异常，后续轮次继续”，不再返回正常空结果并误报轮次完成。
- 保持文件删除在认领事务提交之后执行。

## 验证和启动

`MediaCleanupServiceTest` 的新增用例在修复前失败；真实数据库临时表用例在修复前复现 `SKIP LOCKED` 语法异常，修复后通过。临时表只对测试连接可见，不修改业务表或上传文件，也不运行业务清理服务。

在 backend 目录执行：

```powershell
$env:JAVA_HOME = 'D:/dev_tools/IDEA/IntelliJ IDEA 2024.3.7/jbr'
./mvnw.cmd '-Dblog.test.media.database=true' package
```

本机使用 Java 21 验证 149 项测试全部通过并生成 WAR。现有 Mockito/Byte Buddy 不支持默认 Java 23；未为本次修复更换测试依赖或修改系统 JAVA_HOME。

`blog.test.media.database` 默认关闭；开启时读取本机 `db.properties`，需要已存在媒体表及创建临时表权限。该用例验证实际 DAO 的语法、到期和引用筛选、同一事务内不重复认领以及回滚。两个独立连接的并发验收仍按媒体生命周期 runbook 在独立 schema 中执行，不能把临时表测试等同于多连接并发验收。

在 IDEA 重新部署或停止后重新运行 Tomcat，使 JVM 加载新类。无需替换 XAMPP 数据库文件或执行数据库迁移。

## 浏览器 startTime 异常

当前页面只加载项目构建脚本，业务源码和依赖声明中没有 `reportAllChanges`。浏览器实际捕获的匿名脚本堆栈与 [web-vitals #792](https://github.com/GoogleChrome/web-vitals/issues/792) 的行列号一致；该报告指向 Chromium DevTools 注入的性能监测脚本在单页路由切换时出错。

本轮刷新页面并在现有登录态下切换文章、我的文章页面后，未观察到新增的同类异常。此结果不是浏览器上游代码已修复的证明。

若打开 Performance 面板后再次出现，关闭 DevTools 并刷新页面再试；如当前版本提供实时指标开关，可关闭该采集功能。浏览器升级是否包含修复需核对后续版本，不能通过修改本项目登录函数修复浏览器内置脚本。不要添加全局错误屏蔽器掩盖异常。
