# Java / Maven 工具链指南

本指南用于从仓库源码配置开发环境、运行测试并启动应用。命令示例使用 Windows PowerShell；所有“项目根目录”均指克隆得到的 `personal-blog-system` 目录，无需约定其盘符或父目录。

## 环境与配置职责

| 项目 | 约定 |
| --- | --- |
| 运行 JDK | JDK 21，Maven、测试和 Tomcat 使用同一主版本 |
| 编译目标 | Java 11，POM 的 `source/target/release=11` |
| Maven | Wrapper 3.9.16 |
| Servlet 容器 | 外部 Tomcat 9.x，使用 `javax.servlet` |
| 测试 | JUnit 4、Mockito 5.14.2、Surefire 3.5.4 |
| JDBC 驱动 | `com.mysql:mysql-connector-j:8.0.33` |

Java 11 编译目标不表示项目测试应在 JDK 11 上运行，也不等于已经对整个应用做过 Java 11 运行验收。Maven Wrapper 固定 Maven 版本，但不会安装或切换 JDK。Tomcat 10/11 不能直接替换当前使用的 Tomcat 9。

| 文件 | 作用 |
| --- | --- |
| `backend/pom.xml` | 编译目标、依赖和 Surefire 测试 JVM 的 Mockito agent、UTF-8 参数 |
| `backend/.mvn/jvm.config` | Maven 主 JVM 的 UTF-8 参数 |
| `backend/src/main/resources/log4j2.xml` | 控制台日志布局编码 |
| `scripts/dev-backend.ps1` | 检查环境和端口、配置独立 Tomcat 实例并前台启动 |
| `scripts/test-dev-backend.ps1` | 启动脚本的隔离回归检查 |
| `ToolchainEnvironmentTest.java` | 实际测试 JVM、编码和 inline mock 验证 |

这些配置、脚本和测试属于项目源码，应纳入版本控制。生成的 `backend/target/`、日志和工具安装包不提交。

## 设置当前终端

先安装完整 JDK 21、外部 Tomcat 9、Node.js 和项目 `frontend/package.json` 指定的 pnpm。使用自己的安装目录，无需更改系统默认 Java。

从项目根目录执行以下命令，提示时输入目录，不加外层引号：

```powershell
$env:JAVA_HOME = Read-Host '请输入 JDK 21 安装目录'
$env:BLOG_TOMCAT_HOME = Read-Host '请输入 Tomcat 9 安装目录'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

& "$env:JAVA_HOME\bin\java.exe" -version
Push-Location backend
try {
    .\mvnw.cmd -version
    if ($LASTEXITCODE -ne 0) { throw 'Maven 环境检查失败。' }
} finally {
    Pop-Location
}
```

输出应显示 Java 21 和 Maven 3.9.16。环境变量仅作用于当前终端；新终端需重新设置。数据库连接按 [README](../README.md) 配置。

## 构建

以下命令从项目根目录执行。首次使用先安装前端依赖；后续构建仍需先生成前端资源，Maven 不会代替 pnpm 构建 Vue。

```powershell
Push-Location frontend
try {
    pnpm install --frozen-lockfile
    if ($LASTEXITCODE -ne 0) { throw '前端依赖安装失败。' }
    pnpm build
    if ($LASTEXITCODE -ne 0) { throw '前端构建失败。' }
} finally {
    Pop-Location
}

Push-Location backend
try {
    .\mvnw.cmd package
    if ($LASTEXITCODE -ne 0) { throw '后端测试或打包失败。' }
} finally {
    Pop-Location
}
```

产物：

- `backend/target/personal_blog_system_war_exploded.war`：包含前端资源的 WAR。
- `backend/target/personal_blog_system_war_exploded/`：供开发脚本部署的 exploded WAR。

日常打包不必附带 `clean`。确需清理时先停止使用这些产物的实例，确认仅清理构建目录；不得删除上传数据。

## 命令行启动

从已设置上述环境变量的项目根目录执行：

```powershell
# 只检查环境、产物和端口，不创建实例或启动服务。
.\scripts\dev-backend.ps1 -Check
```

检查通过后启动：

```powershell
.\scripts\dev-backend.ps1
```

也可显式传入 `-JavaHome`、`-TomcatHome`，其优先级高于 `JAVA_HOME` 和 `BLOG_TOMCAT_HOME`。脚本不自动构建、安装工具或结束其他进程；源码更新后须重新构建产物。

脚本使用独立 `CATALINA_BASE=backend/target/tomcat9-dev`，不改写共享 Tomcat 安装。实例锁防止重复使用同一实例，未知内容会使脚本拒绝继续。默认只监听回环地址的 8080 端口；按 Ctrl+C 停止前台实例。

访问 `http://localhost:8080/personal_blog_system_war_exploded/`，确认页面、静态资源及 `/api/auth/session` 正常返回，不能仅凭进程存在判定应用部署成功。

脚本为 Tomcat 设置：

```text
-Dfile.encoding=UTF-8
-Dstdout.encoding=UTF-8
-Dstderr.encoding=UTF-8
-Dblog.media.cleanup.enabled=false
```

开发入口强制关闭媒体清理器，避免开发启动触发后台清理；应用其他启动方式仍须自行配置。启用清理的条件见[媒体运行手册](media-lifecycle-runbook.md)。

### 端口占用

脚本报告监听地址、PID 和进程名后退出，不会自动换端口或终止进程。可只读查看：

```powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen |
    Select-Object LocalAddress, LocalPort, OwningProcess
```

根据 PID 确认所属服务后自行决定停止哪个实例。显式 `-Port 18080` 可用于独立后端检查，但不会改变 Vite 的 8080 代理。IDEA 与命令行不可同时占用同一端口。

## IDEA 配置

不同版本菜单名称可能不同，逐项核对以下设置，不要只改 Project SDK：

| 设置 | 值 |
| --- | --- |
| 项目 SDK（Project SDK） | 自己安装的 JDK 21 |
| 项目及模块语言级别、目标字节码 | Java 11 |
| Maven Importer / Runner JDK | JDK 21 |
| Maven home | 项目 Maven Wrapper |
| Tomcat Server / JRE | 自己安装的外部 Tomcat 9 / JDK 21 |
| Deployment | 项目的 exploded WAR |
| Application context | `/personal_blog_system_war_exploded` |
| HTTP port | 8080 |
| VM options | 上一节的三个 UTF-8 参数和关闭媒体清理的参数 |
| 控制台编码 | UTF-8 |

前端资源需先构建。测试建议创建 Maven `test` 运行配置，工作目录设为 `backend`。原生 JUnit 运行器不能假定自动获得 POM 动态解析的 Mockito agent 路径；若使用它，需另行确认实际 JVM 参数。不要把测试 agent 加到 Tomcat。

IDEA 的本地 SDK 和安装路径留在本机配置，不纳入仓库。旧 `tomcat7:run-war` 启动入口已移除。

## 测试与排错

在 `backend` 目录、JDK 21 环境执行：

```powershell
.\mvnw.cmd test
```

Surefire 使用 Maven 解析的 Mockito JAR 路径预加载 `-javaagent`。正常测试不需要 `net.bytebuddy.experimental=true`，也不依赖动态加载 agent。

专项检查：

```powershell
.\mvnw.cmd test '-Dtest=ToolchainEnvironmentTest' `
    '-Dtoolchain.test.jvmArgs=-XX:-EnableDynamicAgentLoading' `
    '-Dblog.test.requireNoDynamicAgentLoading=true'
```

后两项配套使用：第一项禁止动态加载，第二项要求测试断言该限制确实进入测试 JVM。它们不会代替 POM 中的启动 agent。未显式启用的真实数据库兼容测试按设计跳过；不要为了消除跳过数而连接日常数据库。

启动脚本测试从项目根目录分别执行：

```powershell
powershell.exe -NoProfile -File .\scripts\test-dev-backend.ps1
pwsh.exe -NoProfile -File .\scripts\test-dev-backend.ps1
```

该测试目前含固定工具路径。运行前须按自己的安装位置调整测试文件中的 `$validJavaHome`、`$validTomcatHome`，以及错误 JDK 版本场景使用的 JDK 23 路径。正常启动脚本无需这样修改，直接传参数即可。测试临时产物位于 `backend/target/dev-backend-script-tests/`。

中文乱码应逐层检查：文件保存编码 → Maven/测试 JVM 输出编码 → Log4j2 布局编码 → 终端或 IDEA 解码。单设 `file.encoding` 不能解决整条链路。PowerShell 5.1 下含中文的脚本应保留 UTF-8 BOM；控制台接收 UTF-8 输出时可设置：

```powershell
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false)
$OutputEncoding = [Console]::OutputEncoding
```

验收时分别核对 Maven 的 JDK、测试结果、日志中文、WAR 资源和真实 HTTP 响应。IDEA 与命令行启动应分别验证；历史测试次数不能证明当前环境通过。