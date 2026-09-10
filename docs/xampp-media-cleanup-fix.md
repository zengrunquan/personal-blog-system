# 数据库兼容性说明：MySQL / MariaDB

XAMPP 中提供的数据库可能是 MariaDB，不能仅凭“mysql”目录名或新下载的安装包判断正在连接的服务版本。通过项目所连接的数据库执行只读查询 `SELECT VERSION();` 核实。

## 媒体清理的锁行为

MariaDB 10.4 不支持 `FOR UPDATE SKIP LOCKED`。项目认领查询使用普通 `FOR UPDATE`，保留事务、条件更新、无引用检查和 claim token；多个实例会等待行锁，不能假定自动跳过锁。

锁等待超时或认领失败会回滚并向调度器报告异常，后续轮次仍可继续。文件删除在认领事务提交之后执行。清理任务由后台 Listener 调度，与登录接口没有直接调用关系；启用时首次延迟约 1 分钟，后续默认间隔 60 分钟。

这项兼容处理不意味着 MariaDB 所有版本和全部迁移场景均已通过验证。生产使用前还应验证目标数据库的表结构、外键和并发行为。

## 兼容测试

默认后端测试不连接真实数据库。`blog.test.media.database` 是显式启用开关；测试读取本地 `db.properties`，需要已有媒体表及创建临时表权限。

如需验证，先准备独立测试数据库、备份并核对连接配置，然后按[工具链指南](java-maven-toolchain.md)设置 JDK 21。在 `backend` 目录执行：

```powershell
.\mvnw.cmd test '-Dblog.test.media.database=true'
```

兼容测试使用连接级临时表，检查实际 DAO 的语法、到期和引用筛选、同事务内不重复认领以及回滚，不运行业务清理服务。它不能替代两个独立连接的并发认领验收，后者见[媒体运行手册](media-lifecycle-runbook.md)。

代码更新后需重新打包并重启或重新部署 Tomcat，才能加载新类。仅为验证认领查询无需更换数据库安装文件；是否需要结构迁移，应按[升级说明](migration-review.md)单独核对。