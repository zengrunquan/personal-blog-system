# 媒体文件生命周期运行手册

本手册用于头像、文章图片和附件的媒体元数据、引用、历史回填和延迟回收。二进制文件继续保存在 `docs/uploads/image` 与 `docs/uploads/file`，本功能不移动、重命名或删除历史文件。

## 状态和边界

```text
上传文件
   │
   ▼
TEMP ──被文章/头像引用──> ACTIVE ──零引用──> DELETE_PENDING
  │                                            │
  └─超过 24h ────────────────────────────────┘
                                               ▼
                                           DELETING
                                      ┌────────┴────────┐
                                      ▼                 ▼
                                  DELETED       DELETE_FAILED → 重试
```

`LEGACY_PROTECTED` 表示历史上没有可确认业务引用的文件，`MISSING_BINARY` 表示数据库引用存在但物理文件缺失。这两个状态和未知文件都必须人工处理，清理器永远不会自动删除。

文章正文只识别服务端清洗后的 `img[src]`、`a[href]`；封面单独识别。头像、正文、封面和附件引用分别记录为 `USER_AVATAR`、`ARTICLE_CONTENT`、`ARTICLE_COVER`。`ARTICLE_CONTENT` 可以引用头像、文章图片和附件，`ARTICLE_COVER` 仍只能引用文章图片；同一资产可以被多篇文章或多个用户共享。

## 清理配置

JVM 参数优先于对应环境变量。开发启动脚本强制设置 `blog.media.cleanup.enabled=false`；通过其他方式启动时，应用默认启用清理，维护前应显式关闭。

| JVM 参数 | 环境变量 | 默认值 | 允许范围 |
| --- | --- | --- | --- |
| `blog.media.cleanup.enabled` | `BLOG_MEDIA_CLEANUP_ENABLED` | `true` | `true` / `false` |
| `blog.media.retention.hours` | `BLOG_MEDIA_RETENTION_HOURS` | `24` | `1..8760` |
| `blog.media.cleanup.interval.minutes` | `BLOG_MEDIA_CLEANUP_INTERVAL_MINUTES` | `60` | `1..1440` |
| `blog.media.cleanup.batch.size` | `BLOG_MEDIA_CLEANUP_BATCH_SIZE` | `100` | `1..10000` |
| `blog.media.claim.timeout.minutes` | `BLOG_MEDIA_CLAIM_TIMEOUT_MINUTES` | `60` | `1..1440` |

非法值会使清理器拒绝启动并记录错误。备份和维护报告包含真实数据，以下示例将它们放在已被 Git 忽略的 `database/backups/`；仍应另行保存可靠备份。

## 首次上线门禁

以下步骤必须在停止应用、已获明确授权的独立维护窗口内执行。是否需要迁移取决于目标数据库的当前结构。下面的脚本以 Windows PowerShell 为准，从本仓库根目录执行；先按[工具链指南](java-maven-toolchain.md)配置 JDK 21，准备 MySQL 客户端，并核对数据库账号和实际上传根目录，不能把密码写入脚本。各阶段应分开执行并核对结果，不要整篇复制运行。

### 1. 停止应用并建立可验证备份

清理器在迁移、回填和核对完成前必须关闭。数据库备份和上传目录备份必须先成功，才允许执行迁移 SQL：

```powershell
Set-StrictMode -Version Latest

$repoRoot = (Get-Location).Path
if (-not (Test-Path -LiteralPath (Join-Path $repoRoot 'backend\pom.xml')) -or
    -not (Test-Path -LiteralPath (Join-Path $repoRoot 'frontend\package.json'))) {
    throw '请从 personal-blog-system 仓库根目录执行维护步骤。'
}
$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$backupRoot = Join-Path $repoRoot "database\backups\media-lifecycle\$timestamp"
$reportRoot = Join-Path $backupRoot 'reports'
New-Item -ItemType Directory -Path $backupRoot -Force | Out-Null
New-Item -ItemType Directory -Path $reportRoot -Force | Out-Null

$dbUser = 'root'
$dbName = 'personal_blog'
# 如果部署环境使用自定义目录，把下一行改成与应用 BLOG_UPLOAD_DIR 或 -Dblog.upload.dir 完全一致的绝对路径。
$uploadRoot = Join-Path $repoRoot 'docs\uploads'
if (-not [IO.Path]::IsPathRooted($uploadRoot)) {
    throw "上传根目录必须是绝对路径：$uploadRoot"
}
if (-not (Test-Path -LiteralPath $uploadRoot -PathType Container)) {
    throw "上传根目录不存在：$uploadRoot"
}
$uploadRoot = (Resolve-Path -LiteralPath $uploadRoot).Path.TrimEnd('\')

# 维护命令通过环境变量明确使用同一个根目录；JVM -Dblog.upload.dir 的优先级更高，若使用它也必须指向同一路径。
$env:BLOG_UPLOAD_DIR = $uploadRoot
# 如果应用启动参数已有 -Dblog.media.cleanup.enabled=true，必须先删除该参数或改为 false；JVM 参数优先于环境变量。
$env:BLOG_MEDIA_CLEANUP_ENABLED = 'false'
Write-Host "本次维护使用上传根目录：$uploadRoot"

$dumpFile = Join-Path $backupRoot "$dbName.sql"
& mysqldump --single-transaction --routines --triggers "-u$dbUser" '-p' $dbName "--result-file=$dumpFile"
$dumpExitCode = $LASTEXITCODE
if ($dumpExitCode -ne 0 -or -not (Test-Path -LiteralPath $dumpFile)) {
    throw "mysqldump 失败，exitCode=$dumpExitCode，文件=$dumpFile"
}
if ((Get-Item -LiteralPath $dumpFile).Length -le 0) {
    throw "mysqldump 生成了空文件：$dumpFile"
}

$uploadBackup = Join-Path $backupRoot 'uploads'
New-Item -ItemType Directory -Path $uploadBackup -Force | Out-Null
robocopy $uploadRoot $uploadBackup /E /COPY:DAT /DCOPY:DAT /R:1 /W:1 /XJ /NFL /NDL
$copyExitCode = $LASTEXITCODE
# robocopy 的 0..7 都表示成功或有可接受的复制差异，8 及以上才是失败。
if ($copyExitCode -gt 7) {
    throw "上传目录备份失败，robocopy exitCode=$copyExitCode"
}

function Get-MediaManifest {
    param([Parameter(Mandatory = $true)][string]$Root)

    $base = (Resolve-Path -LiteralPath $Root).Path.TrimEnd('\')
    @(Get-ChildItem -LiteralPath $Root -File -Recurse | ForEach-Object {
        $relative = $_.FullName.Substring($base.Length).TrimStart('\', '/')
        [PSCustomObject]@{
            RelativePath = $relative
            Length       = $_.Length
            SHA256       = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash
        }
    }) | Sort-Object RelativePath
}

$sourceManifest = @(Get-MediaManifest -Root $uploadRoot)
$backupManifest = @(Get-MediaManifest -Root $uploadBackup)
$sourceManifestFile = Join-Path $backupRoot 'uploads-source-manifest.csv'
$backupManifestFile = Join-Path $backupRoot 'uploads-backup-manifest.csv'
$sourceManifest | Export-Csv -LiteralPath $sourceManifestFile -NoTypeInformation -Encoding UTF8
$backupManifest | Export-Csv -LiteralPath $backupManifestFile -NoTypeInformation -Encoding UTF8

$sourceEntries = @($sourceManifest | ForEach-Object { "$($_.RelativePath)|$($_.Length)|$($_.SHA256)" })
$backupEntries = @($backupManifest | ForEach-Object { "$($_.RelativePath)|$($_.Length)|$($_.SHA256)" })
$manifestDiff = Compare-Object -ReferenceObject $sourceEntries -DifferenceObject $backupEntries
if ($null -ne $manifestDiff) {
    $manifestDiff | Format-Table -AutoSize | Out-String | Write-Error
    throw '上传目录源与备份的相对路径、大小或 SHA-256 不一致'
}
Write-Host "数据库备份：$dumpFile"
Write-Host "上传目录备份：$uploadBackup"
Write-Host "上传文件清单：$sourceManifestFile、$backupManifestFile"
```

`mysqldump` 的 `-p` 会交互式提示密码，命令行、PowerShell 历史和报告中都不应出现密码。备份目录应位于上传根目录之外；如果实际部署通过 JVM 参数配置根目录，应先确认该参数与脚本中的 `$uploadRoot` 相同。

### 2. 执行迁移并构造回填运行时 classpath

确认备份脚本退出码为 0、SQL dump 非空、两个上传清单一致后，评审并执行 [database/migrations/2026-08-31-add-media-lifecycle.sql](../database/migrations/2026-08-31-add-media-lifecycle.sql)。不要用 `init-database.sql` 覆盖已有数据库。下面的 `mysql` 命令只应在上述备份完成且维护窗口已确认后执行：

```powershell
$migrationFile = Join-Path $repoRoot 'database\migrations\2026-08-31-add-media-lifecycle.sql'
Get-Content -LiteralPath $migrationFile -Raw | & mysql "-u$dbUser" '-p' $dbName
if ($LASTEXITCODE -ne 0) {
    throw "媒体生命周期迁移失败，exitCode=$LASTEXITCODE"
}

Push-Location (Join-Path $repoRoot 'backend')
try {
    & .\mvnw.cmd -DskipTests package
    if ($LASTEXITCODE -ne 0) { throw 'Maven 构建失败，停止回填' }

    & .\mvnw.cmd dependency:build-classpath "-Dmdep.outputFile=target\runtime-classpath.txt"
    if ($LASTEXITCODE -ne 0) { throw 'Maven 依赖 classpath 生成失败，停止回填' }

    $classesPath = (Resolve-Path -LiteralPath 'target\classes').Path
    $dependencyClasspath = (Get-Content -LiteralPath 'target\runtime-classpath.txt' -Raw).Trim()
    $mediaRuntimeClasspath = "$classesPath;$dependencyClasspath"
    $javaCommand = @('-cp', $mediaRuntimeClasspath, 'com.blog.media.maintenance.MediaBackfillCommand')
} finally {
    Pop-Location
}
```

### 3. dry-run、人工核对、apply 和幂等复核

下面的函数会同时保存原始控制台输出和单独 JSON 报告。它从输出中提取最后一行 JSON，因此即使 JDBC 驱动输出 INFO 日志，也不会把日志误当成报告：

```powershell
Push-Location (Join-Path $repoRoot 'backend')
try {
    function Invoke-MediaBackfill {
        param(
            [Parameter(Mandatory = $true)][string]$Mode,
            [Parameter(Mandatory = $true)][string]$RawOutputPath,
            [Parameter(Mandatory = $true)][string]$JsonReportPath
        )

        $output = @(& java @javaCommand $Mode)
        $exitCode = $LASTEXITCODE
        $output | Set-Content -LiteralPath $RawOutputPath -Encoding UTF8
        if ($exitCode -ne 0) {
            throw "历史媒体回填 $Mode 失败，exitCode=$exitCode；详见 $RawOutputPath"
        }
        $jsonLines = @($output | Where-Object { $_ -match '^\s*\{.*\}\s*$' })
        if ($jsonLines.Count -ne 1) {
            throw "回填 $Mode 未产生唯一 JSON 报告；详见 $RawOutputPath"
        }
        $jsonLines[0] | Set-Content -LiteralPath $JsonReportPath -Encoding UTF8
        return ($jsonLines[0] | ConvertFrom-Json)
    }

    $dryRunBefore = Invoke-MediaBackfill `
        -Mode '--dry-run' `
        -RawOutputPath (Join-Path $reportRoot 'dry-run-before.raw.log') `
        -JsonReportPath (Join-Path $reportRoot 'dry-run-before.json')

    if ([IO.Path]::GetFullPath([string]$dryRunBefore.storageRoot) -ne $uploadRoot) {
        throw "回填实际使用的 storageRoot 与备份根目录不一致：$($dryRunBefore.storageRoot)"
    }
    $dryRunBefore | Format-List
    Write-Host '人工审查 manualReview、duplicateKeys、hashFailures、missingBinary 以及 sourceSummary；未批准前不要执行 --apply。'

    # 仅在人工审查通过后解除下一条命令的阻断；--apply 只写媒体元数据和引用，不改动物理文件。
    $applyOutput = @(& java @javaCommand '--apply')
    $applyExitCode = $LASTEXITCODE
    $applyOutput | Set-Content -LiteralPath (Join-Path $reportRoot 'apply.raw.log') -Encoding UTF8
    if ($applyExitCode -ne 0) {
        throw "历史媒体回填 --apply 失败，exitCode=$applyExitCode"
    }

    $dryRunAfter = Invoke-MediaBackfill `
        -Mode '--dry-run' `
        -RawOutputPath (Join-Path $reportRoot 'dry-run-after.raw.log') `
        -JsonReportPath (Join-Path $reportRoot 'dry-run-after.json')

    $stableFields = @(
        'storageRoot', 'sourceSummary', 'usersScanned', 'articlesScanned', 'filesScanned',
        'activeAssets', 'protectedAssets', 'missingBinary', 'referencesCreated',
        'manualReview', 'duplicateKeys', 'hashFailures'
    )
    foreach ($field in $stableFields) {
        if ([string]$dryRunBefore.$field -ne [string]$dryRunAfter.$field) {
            throw "apply 后 dry-run 字段发生变化：$field，before=$($dryRunBefore.$field)，after=$($dryRunAfter.$field)"
        }
    }
    Write-Host '第二次 dry-run 与首次报告一致，回填结果满足本次幂等核对。'
} finally {
    Pop-Location
}
```

只有在第二次 dry-run 与首次报告一致、人工复核项已处理、且上传根目录仍与备份一致后，才允许重新启用清理器。首次上线建议保持 24 小时缓冲并观察至少一轮：

```text
-Dblog.media.cleanup.enabled=true
-Dblog.media.retention.hours=24
-Dblog.media.cleanup.interval.minutes=60
-Dblog.media.cleanup.batch.size=100
-Dblog.media.claim.timeout.minutes=60
```

## 独立 MySQL 8 验收门禁

以下验收必须使用可丢弃的独立 MySQL 8 schema、临时上传根目录和两个独立 JDBC 连接；不能连接生产库，也不能用单元测试替代。记录目标环境的实际结果，未执行的场景标记为未验证。

1. 备份或新建测试 schema 后执行迁移脚本，核对 `CHECK`、唯一索引和全部外键。
2. 建立两个文章共享同一媒体、头像与文章正文共享同一媒体的测试数据；分别删除文章、分类和用户，确认 `media_reference` 按外键级联删除，而 `media_asset` 和物理文件不会被请求线程直接删除。
3. 使用两个连接同时执行认领流程，设置 `SET SESSION innodb_lock_wait_timeout = 5`；确认同一 `DELETE_PENDING` 资产只被一个连接拿到。认领使用普通 `FOR UPDATE` 以兼容 XAMPP MariaDB 10.4，另一个连接会等待锁；首个连接提交后应处理其他资产或返回空结果。额外验证锁等待超时会回滚并报告轮次异常，后续轮次仍能正常运行。
4. 构造 `delete_after` 为当前时间前 1 秒、当前时间后 1 秒和当前时间的资产，确认到期比较是 `<=`：未到期不删除，恰好到期可删除。
5. 构造过期 `DELETING` 且 `claimed_at` 早于认领超时的记录，模拟进程退出后确认下一轮恢复为可重试状态；同时验证 claim token 不匹配时不能标记 `DELETED` 或 `DELETE_FAILED`。
6. 验收迁移执行结果、并发锁等待、级联结果和边界结果后，只清理本次明确创建的测试 schema 与临时目录；任何删除前仍需再次确认具体路径。

## 日常观测

- 上传成功后应看到 `media_asset.status=TEMP`，`delete_after` 为当前时间后 24 小时。
- 文章保存或头像替换提交后，应看到对应 `media_reference`，资产变为 `ACTIVE`。
- 文章移除资源、删除文章、分类级联或删除用户后，只检查引用消失；物理文件不应在请求线程立即删除。
- 清理器日志应包含 `mediaId`、`storageName`、claim token 和异常堆栈。文件已不存在是幂等成功；I/O 失败进入 `DELETE_FAILED`，重试间隔为 `min(2^attempt 小时, 24 小时)`。
- `DELETING` 长时间未结束时，下一轮会按 claim 超时恢复为待清理状态。

## 故障处理和回退

- 数据库事务失败：上传服务自动补偿删除新文件；补偿失败会同时记录原事务异常和删除异常，先保留文件并人工处理。
- 清理器启动配置非法：Listener 拒绝启动并记录 ERROR，不会回退到危险默认值。修正配置后重启应用。
- 回填失败：停止清理器，保留新表和原上传目录，检查数据库日志后重试 dry-run；不要手动删除或移动历史文件。
- 应用版本回退：可以回退 WAR，但不要因应用回退删除上传目录。只有确认数据库结构已被破坏并再次获授权时，才从完整备份恢复数据库。
