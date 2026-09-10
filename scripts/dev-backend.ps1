[CmdletBinding()]
param(
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$TomcatHome = $env:BLOG_TOMCAT_HOME,
    [ValidateRange(1024, 65535)]
    [int]$Port = 8080,
    [switch]$Check
)

Set-StrictMode -Version 2.0
$ErrorActionPreference = 'Stop'
$script:IsDotSourced = $MyInvocation.InvocationName -eq '.'
$script:ManagedMarker = '由 scripts/dev-backend.ps1 生成，请勿手工编辑。'
$script:ContextPath = '/personal_blog_system_war_exploded'
$script:ArtifactName = 'personal_blog_system_war_exploded'
$script:InstanceName = 'tomcat9-dev'
$script:ManagedConfFiles = @(
    'catalina.properties',
    'context.xml',
    'jaspic-providers.xml',
    'logging.properties',
    'tomcat-users.xml',
    'web.xml'
)

function Get-ToolchainTimestamp {
    return (Get-Date).ToString('yyyy-MM-dd HH:mm:ss')
}

function Write-ToolchainMessage {
    param(
        [ValidateSet('INFO', 'WARN', 'ERROR', 'DEBUG')]
        [string]$Level,
        [string]$Context,
        [string]$Message
    )

    Write-Host ('[{0}][{1}][{2}] {3}' -f (Get-ToolchainTimestamp), $Level, $Context, $Message)
}

function Throw-ToolchainError {
    param(
        [string]$Context,
        [string]$Message
    )

    $errorMessage = '[{0}][ERROR][{1}] {2}' -f (Get-ToolchainTimestamp), $Context, $Message
    throw [InvalidOperationException]::new($errorMessage)
}

function Resolve-ToolchainPath {
    param(
        [string]$Path,
        [string]$Label
    )

    if ([string]::IsNullOrWhiteSpace($Path)) {
        Throw-ToolchainError $Label '路径为空，请通过参数或对应环境变量提供绝对路径。'
    }

    try {
        return [IO.Path]::GetFullPath($Path)
    }
    catch {
        Throw-ToolchainError $Label ("路径无法解析：{0}" -f $_.Exception.Message)
    }
}

function Get-JdkInfo {
    param([string]$JavaHomePath)

    $requiredFiles = @('java.exe', 'javac.exe', 'jar.exe', 'jdb.exe')
    foreach ($fileName in $requiredFiles) {
        $filePath = Join-Path $JavaHomePath ('bin\' + $fileName)
        if (-not (Test-Path -LiteralPath $filePath -PathType Leaf)) {
            Throw-ToolchainError 'JDK' ("$JavaHomePath 不是完整 JDK，缺少 $filePath。")
        }
    }

    $javaPath = Join-Path $JavaHomePath 'bin\java.exe'
    $previousErrorActionPreference = $ErrorActionPreference
    try {
        # Windows PowerShell 5.1 会把原生命令的正常 stderr 视为错误记录，局部放宽才能读取版本而不改变脚本其余部分的错误策略。
        $ErrorActionPreference = 'Continue'
        $versionOutput = @(& $javaPath -version 2>&1)
        $javaExitCode = [int]$LASTEXITCODE
    }
    finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    if ($javaExitCode -ne 0) {
        $failureOutput = $versionOutput -join [Environment]::NewLine
        Throw-ToolchainError 'JDK' ("执行 $javaPath -version 失败，退出码 $javaExitCode。输出：$failureOutput")
    }
    $versionText = $versionOutput -join [Environment]::NewLine
    if ($versionText -notmatch 'version\s+"(?<version>[^"]+)"') {
        Throw-ToolchainError 'JDK' ("无法读取 $javaPath 的实际版本。")
    }

    $fullVersion = $Matches['version']
    $majorVersion = $fullVersion.Split('.')[0]
    if ($majorVersion -eq '1' -and $fullVersion.Split('.').Length -gt 1) {
        $majorVersion = $fullVersion.Split('.')[1]
    }
    if ($majorVersion -ne '21') {
        Throw-ToolchainError 'JDK' ("需要 JDK 21，实际检测到 $fullVersion：$JavaHomePath。")
    }

    Write-ToolchainMessage INFO 'JDK' ("使用 JDK $fullVersion：$JavaHomePath")
    return [pscustomobject]@{
        Home = $JavaHomePath
        Java = $javaPath
        Version = $fullVersion
        MajorVersion = $majorVersion
    }
}

function Get-TomcatVersion {
    param([string]$CatalinaJarPath)

    try {
        Add-Type -AssemblyName System.IO.Compression.FileSystem -ErrorAction SilentlyContinue
        $archive = [IO.Compression.ZipFile]::OpenRead($CatalinaJarPath)
    }
    catch {
        Throw-ToolchainError 'Tomcat' ("无法读取 $CatalinaJarPath：$($_.Exception.Message)")
    }

    $reader = $null
    $stream = $null
    try {
        $entry = $archive.GetEntry('META-INF/MANIFEST.MF')
        if ($null -eq $entry) {
            Throw-ToolchainError 'Tomcat' 'catalina.jar 缺少 META-INF/MANIFEST.MF，无法确认版本。'
        }
        $stream = $entry.Open()
        $reader = New-Object System.IO.StreamReader -ArgumentList $stream, ([Text.Encoding]::UTF8), $true
        $manifest = $reader.ReadToEnd()
        if ($manifest -notmatch '(?m)^Implementation-Version:\s*(?<version>[^\r\n]+)') {
            Throw-ToolchainError 'Tomcat' 'catalina.jar 的清单缺少 Implementation-Version。'
        }
        return $Matches['version'].Trim()
    }
    finally {
        if ($null -ne $reader) {
            $reader.Dispose()
        }
        elseif ($null -ne $stream) {
            $stream.Dispose()
        }
        $archive.Dispose()
    }
}

function Get-TomcatInfo {
    param([string]$TomcatHomePath)

    $catalinaPath = Join-Path $TomcatHomePath 'bin\catalina.bat'
    $catalinaJarPath = Join-Path $TomcatHomePath 'lib\catalina.jar'
    $confPath = Join-Path $TomcatHomePath 'conf'
    foreach ($requiredPath in @($catalinaPath, $catalinaJarPath, $confPath)) {
        if (-not (Test-Path -LiteralPath $requiredPath)) {
            Throw-ToolchainError 'Tomcat' ("Tomcat 目录不完整，缺少：$requiredPath。")
        }
    }

    $version = Get-TomcatVersion $catalinaJarPath
    if ($version -notmatch '^9\.') {
        Throw-ToolchainError 'Tomcat' ("需要 Tomcat 9.x，实际检测到 $version：$TomcatHomePath。")
    }

    Write-ToolchainMessage INFO 'Tomcat' ("使用外部 Tomcat $version：$TomcatHomePath")
    return [pscustomobject]@{
        Home = $TomcatHomePath
        Catalina = $catalinaPath
        Version = $version
        Conf = $confPath
    }
}

function Get-ArtifactInfo {
    param([string]$ProjectRootPath)

    $artifactPath = Join-Path $ProjectRootPath ('backend\target\' + $script:ArtifactName)
    if (-not (Test-Path -LiteralPath $artifactPath -PathType Container)) {
        Throw-ToolchainError '产物' ("找不到 exploded WAR：$artifactPath。请先执行 frontend\pnpm build，再执行 backend\mvnw.cmd package。")
    }

    $requiredEntries = @(
        @{ RelativePath = 'WEB-INF\web.xml'; Type = 'Leaf' },
        @{ RelativePath = 'WEB-INF\classes'; Type = 'Container' },
        @{ RelativePath = 'WEB-INF\lib'; Type = 'Container' },
        @{ RelativePath = 'index.html'; Type = 'Leaf' }
    )
    $missingEntries = @()
    foreach ($entry in $requiredEntries) {
        $entryPath = Join-Path $artifactPath $entry.RelativePath
        if (-not (Test-Path -LiteralPath $entryPath -PathType $entry.Type)) {
            $missingEntries += $entry.RelativePath
        }
    }
    if ($missingEntries.Count -gt 0) {
        Throw-ToolchainError '产物' ("exploded WAR 不完整，缺少：$($missingEntries -join ', ')。请先执行 frontend\pnpm build，再执行 backend\mvnw.cmd package。")
    }

    $lastWriteTime = (Get-Item -LiteralPath $artifactPath).LastWriteTime.ToString('yyyy-MM-dd HH:mm:ss')
    Write-ToolchainMessage INFO '产物' ("使用 exploded WAR：$artifactPath；最后更新时间：$lastWriteTime。该时间戳不能证明源码已是最新，请在修改后重新构建。")
    return [pscustomobject]@{
        Root = $artifactPath
        LastWriteTime = $lastWriteTime
    }
}

function Get-ListeningConnections {
    param([int]$PortNumber)

    if ($null -eq (Get-Command Get-NetTCPConnection -ErrorAction SilentlyContinue)) {
        Throw-ToolchainError '端口' '当前 PowerShell 没有 Get-NetTCPConnection，无法安全判断端口是否空闲。'
    }

    try {
        return @(Get-NetTCPConnection -LocalPort $PortNumber -State Listen -ErrorAction Stop)
    }
    catch {
        # Windows PowerShell 5.1 的无匹配结果会抛出本地化 CIM 异常，使用稳定的错误 ID 不能依赖系统语言。
        if ($_.FullyQualifiedErrorId -like 'CmdletizationQuery_NotFound,Get-NetTCPConnection*' -or $_.Exception.Message -match 'No matching MSFT_NetTCPConnection objects found') {
            return @()
        }
        Throw-ToolchainError '端口' ("读取端口 $PortNumber 的监听状态失败，不能当作端口空闲：$($_.Exception.Message)")
    }
}

function Assert-PortAvailable {
    param([int]$PortNumber)

    $connections = @(Get-ListeningConnections $PortNumber)
    if ($connections.Count -eq 0) {
        Write-ToolchainMessage INFO '端口' ("端口 $PortNumber 未发现监听。")
        return
    }

    foreach ($connection in $connections) {
        $processName = '<无法获取进程名>'
        try {
            $processName = (Get-Process -Id $connection.OwningProcess -ErrorAction Stop).ProcessName
        }
        catch {
            $processName = '<无法获取进程名：权限或进程已退出>'
        }
        Write-ToolchainMessage ERROR '端口' ("端口 $PortNumber 已被占用：地址=$($connection.LocalAddress)，PID=$($connection.OwningProcess)，进程=$processName。脚本不会终止该进程。")
    }
    Throw-ToolchainError '端口' ("端口 $PortNumber 已被占用，请由用户决定停止哪个实例。")
}

function Ensure-Directory {
    param(
        [string]$Path,
        [string]$Label
    )

    if (Test-Path -LiteralPath $Path) {
        if (-not (Test-Path -LiteralPath $Path -PathType Container)) {
            Throw-ToolchainError $Label ("路径已存在但不是目录：$Path。")
        }
        return
    }

    New-Item -ItemType Directory -Path $Path -Force | Out-Null
}

function Assert-OnlyNames {
    param(
        [string]$DirectoryPath,
        [string[]]$AllowedNames,
        [string]$Label
    )

    if (-not (Test-Path -LiteralPath $DirectoryPath -PathType Container)) {
        return
    }
    foreach ($item in @(Get-ChildItem -LiteralPath $DirectoryPath -Force)) {
        if ($AllowedNames -notcontains $item.Name) {
            Throw-ToolchainError $Label ("发现未知内容：$($item.FullName)。脚本不会递归删除，请先人工检查。")
        }
    }
}

function Write-InstanceMarker {
    param(
        [string]$MarkerPath,
        [string]$ProjectRootPath,
        [string]$ArtifactPath,
        [string]$TomcatHomePath
    )

    $marker = [ordered]@{
        managedBy = 'scripts/dev-backend.ps1'
        schemaVersion = 1
        projectRoot = $ProjectRootPath
        artifactPath = $ArtifactPath
        tomcatHome = $TomcatHomePath
        contextPath = $script:ContextPath
    }
    $markerText = $marker | ConvertTo-Json -Depth 3
    [IO.File]::WriteAllText($MarkerPath, $markerText, (New-Object Text.UTF8Encoding($false)))
}

function Assert-InstanceMarker {
    param(
        [string]$MarkerPath,
        [string]$ProjectRootPath
    )

    try {
        # 标记由脚本写成 UTF-8 无 BOM；显式指定编码，避免 Windows PowerShell 5.1 按系统代码页误读中文路径。
        $marker = ([IO.File]::ReadAllText($MarkerPath, [Text.Encoding]::UTF8)) | ConvertFrom-Json
        if ($marker.managedBy -ne 'scripts/dev-backend.ps1' -or [int]$marker.schemaVersion -ne 1) {
            Throw-ToolchainError '实例目录' "实例标记不是由当前启动脚本生成：$MarkerPath。"
        }
        if ([string]::Compare([string]$marker.projectRoot, $ProjectRootPath, $true) -ne 0) {
            Throw-ToolchainError '实例目录' "实例目录属于其他项目：$MarkerPath。"
        }
    }
    catch {
        if ($_.Exception.Message -match '^\[') {
            throw
        }
        Throw-ToolchainError '实例目录' ("实例标记无法读取：$($_.Exception.Message)")
    }
}

function Write-ManagedCopy {
    param(
        [string]$SourcePath,
        [string]$DestinationPath,
        [string]$CommentPrefix
    )

    if (-not (Test-Path -LiteralPath $SourcePath -PathType Leaf)) {
        Throw-ToolchainError '实例配置' ("Tomcat 默认配置不存在：$SourcePath。")
    }

    $sourceContent = [IO.File]::ReadAllText($SourcePath)
    if ($CommentPrefix.StartsWith('<!--', [StringComparison]::Ordinal)) {
        # XML 声明必须是文档首节点，因此受控标记要放在声明之后，不能简单地加在文件首行。
        $xmlDeclaration = [regex]::Match(
            $sourceContent,
            '^(?:\uFEFF)?<\?xml\b.*?\?>',
            [Text.RegularExpressions.RegexOptions]::Singleline
        )
        if ($xmlDeclaration.Success) {
            $content = $xmlDeclaration.Value + [Environment]::NewLine + $CommentPrefix + [Environment]::NewLine + $sourceContent.Substring($xmlDeclaration.Length)
        }
        else {
            $content = $CommentPrefix + [Environment]::NewLine + $sourceContent
        }
    }
    else {
        $content = $CommentPrefix + [Environment]::NewLine + $sourceContent
    }
    if (Test-Path -LiteralPath $DestinationPath -PathType Leaf) {
        $existing = [IO.File]::ReadAllText($DestinationPath)
        $isManaged = if ($CommentPrefix.StartsWith('<!--', [StringComparison]::Ordinal)) {
            $existing.IndexOf($script:ManagedMarker, [StringComparison]::Ordinal) -ge 0
        }
        else {
            $existing.StartsWith($CommentPrefix, [StringComparison]::Ordinal)
        }
        if (-not $isManaged) {
            Throw-ToolchainError '实例配置' ("配置文件不是脚本生成的，拒绝覆盖：$DestinationPath。")
        }
    }
    elseif (Test-Path -LiteralPath $DestinationPath) {
        Throw-ToolchainError '实例配置' ("配置目标不是普通文件：$DestinationPath。")
    }

    [IO.File]::WriteAllText($DestinationPath, $content, (New-Object Text.UTF8Encoding($false)))
}

function Write-XmlDocument {
    param(
        [string]$DestinationPath,
        [Xml.XmlDocument]$Document
    )

    if (Test-Path -LiteralPath $DestinationPath -PathType Leaf) {
        $existing = [IO.File]::ReadAllText($DestinationPath)
        if ($existing.IndexOf($script:ManagedMarker, [StringComparison]::Ordinal) -lt 0) {
            Throw-ToolchainError '实例配置' ("XML 文件不是脚本生成的，拒绝覆盖：$DestinationPath。")
        }
    }
    elseif (Test-Path -LiteralPath $DestinationPath) {
        Throw-ToolchainError '实例配置' ("XML 目标不是普通文件：$DestinationPath。")
    }

    $settings = New-Object Xml.XmlWriterSettings
    $settings.Encoding = New-Object Text.UTF8Encoding($false)
    $settings.Indent = $true
    $settings.NewLineChars = [Environment]::NewLine
    $writer = [Xml.XmlWriter]::Create($DestinationPath, $settings)
    try {
        $Document.Save($writer)
    }
    finally {
        $writer.Dispose()
    }
}

function New-ServerDocument {
    param([int]$PortNumber)

    $document = New-Object Xml.XmlDocument
    $document.AppendChild($document.CreateXmlDeclaration('1.0', 'UTF-8', $null)) | Out-Null
    $server = $document.CreateElement('Server')
    $server.SetAttribute('port', '-1')
    $server.SetAttribute('shutdown', 'SHUTDOWN')
    $server.AppendChild($document.CreateComment($script:ManagedMarker)) | Out-Null

    $versionListener = $document.CreateElement('Listener')
    $versionListener.SetAttribute('className', 'org.apache.catalina.startup.VersionLoggerListener')
    $server.AppendChild($versionListener) | Out-Null
    $memoryListener = $document.CreateElement('Listener')
    $memoryListener.SetAttribute('className', 'org.apache.catalina.core.JreMemoryLeakPreventionListener')
    $server.AppendChild($memoryListener) | Out-Null

    $service = $document.CreateElement('Service')
    $service.SetAttribute('name', 'Catalina')
    $connector = $document.CreateElement('Connector')
    $connector.SetAttribute('address', '127.0.0.1')
    $connector.SetAttribute('port', $PortNumber.ToString())
    $connector.SetAttribute('protocol', 'HTTP/1.1')
    $connector.SetAttribute('connectionTimeout', '20000')
    $connector.SetAttribute('redirectPort', '8443')
    $connector.SetAttribute('URIEncoding', 'UTF-8')
    $service.AppendChild($connector) | Out-Null

    $engine = $document.CreateElement('Engine')
    $engine.SetAttribute('name', 'Catalina')
    $engine.SetAttribute('defaultHost', 'localhost')
    $hostElement = $document.CreateElement('Host')
    $hostElement.SetAttribute('name', 'localhost')
    $hostElement.SetAttribute('appBase', 'webapps')
    $hostElement.SetAttribute('deployOnStartup', 'true')
    $hostElement.SetAttribute('autoDeploy', 'true')
    $hostElement.SetAttribute('unpackWARs', 'false')
    $engine.AppendChild($hostElement) | Out-Null
    $service.AppendChild($engine) | Out-Null
    $server.AppendChild($service) | Out-Null
    $document.AppendChild($server) | Out-Null
    return $document
}

function New-ContextDocument {
    param([string]$ArtifactPath)

    $document = New-Object Xml.XmlDocument
    $document.AppendChild($document.CreateXmlDeclaration('1.0', 'UTF-8', $null)) | Out-Null
    $context = $document.CreateElement('Context')
    $context.AppendChild($document.CreateComment($script:ManagedMarker)) | Out-Null
    $context.SetAttribute('path', $script:ContextPath)
    $context.SetAttribute('docBase', $ArtifactPath)
    $context.SetAttribute('reloadable', 'false')
    $document.AppendChild($context) | Out-Null
    return $document
}

function Write-SetenvFile {
    param(
        [string]$DestinationPath
    )

    $content = "@echo off`r`nif not defined BLOG_DEV_BACKEND_JAVA_HOME exit /b 1`r`nset `"JAVA_HOME=%BLOG_DEV_BACKEND_JAVA_HOME%`"`r`nset `"JRE_HOME=%BLOG_DEV_BACKEND_JAVA_HOME%`"`r`nset `"CATALINA_OPTS=%BLOG_DEV_BACKEND_CATALINA_OPTS%`"`r`n"
    if (Test-Path -LiteralPath $DestinationPath -PathType Leaf) {
        $existing = [IO.File]::ReadAllText($DestinationPath)
        if ($existing -ne $content) {
            Throw-ToolchainError '实例配置' ("setenv.bat 不是脚本生成的版本，拒绝覆盖：$DestinationPath。")
        }
        return
    }
    if (Test-Path -LiteralPath $DestinationPath) {
        Throw-ToolchainError '实例配置' ("setenv.bat 目标不是普通文件：$DestinationPath。")
    }

    [IO.File]::WriteAllText($DestinationPath, $content, [Text.Encoding]::ASCII)
}

function Prepare-Instance {
    param(
        [string]$ProjectRootPath,
        [string]$ArtifactPath,
        [string]$TomcatHomePath,
        [string]$InstancePath,
        [int]$PortNumber
    )

    Ensure-Directory $InstancePath '实例目录'
    $markerPath = Join-Path $InstancePath '.dev-backend-instance.json'
    if (Test-Path -LiteralPath $markerPath -PathType Leaf) {
        Assert-InstanceMarker $markerPath $ProjectRootPath
    }
    elseif (Test-Path -LiteralPath $markerPath) {
        Throw-ToolchainError '实例目录' ("实例标记不是普通文件：$markerPath。")
    }
    else {
        $existingItems = @(Get-ChildItem -LiteralPath $InstancePath -Force | Where-Object { $_.Name -ne '.dev-backend.lock' })
        if ($existingItems.Count -gt 0) {
            Throw-ToolchainError '实例目录' ("实例目录存在未知内容：$InstancePath。脚本不会递归删除，请先人工检查。")
        }
        Write-InstanceMarker $markerPath $ProjectRootPath $ArtifactPath $TomcatHomePath
    }

    $allowedRootNames = @('.dev-backend-instance.json', '.dev-backend.lock', 'bin', 'conf', 'logs', 'temp', 'webapps', 'work')
    Assert-OnlyNames $InstancePath $allowedRootNames '实例目录'

    $binPath = Join-Path $InstancePath 'bin'
    $confPath = Join-Path $InstancePath 'conf'
    $localhostPath = Join-Path $confPath 'Catalina\localhost'
    $logsPath = Join-Path $InstancePath 'logs'
    $tempPath = Join-Path $InstancePath 'temp'
    $webappsPath = Join-Path $InstancePath 'webapps'
    $workPath = Join-Path $InstancePath 'work'
    foreach ($directory in @(
        @{ Path = $binPath; Label = '实例 bin' },
        @{ Path = $confPath; Label = '实例 conf' },
        @{ Path = (Join-Path $confPath 'Catalina'); Label = '实例 conf/Catalina' },
        @{ Path = $localhostPath; Label = '实例 conf/Catalina/localhost' },
        @{ Path = $logsPath; Label = '实例 logs' },
        @{ Path = $tempPath; Label = '实例 temp' },
        @{ Path = $webappsPath; Label = '实例 webapps' },
        @{ Path = $workPath; Label = '实例 work' }
    )) {
        Ensure-Directory $directory.Path $directory.Label
    }

    Assert-OnlyNames $binPath @('setenv.bat') '实例 bin'
    Assert-OnlyNames $confPath @('Catalina', 'catalina.properties', 'context.xml', 'jaspic-providers.xml', 'logging.properties', 'server.xml', 'tomcat-users.xml', 'web.xml') '实例 conf'
    Assert-OnlyNames (Join-Path $confPath 'Catalina') @('localhost') '实例 conf/Catalina'
    Assert-OnlyNames $localhostPath @('personal_blog_system_war_exploded.xml') '实例 conf/Catalina/localhost'
    if (@(Get-ChildItem -LiteralPath $webappsPath -Force).Count -gt 0) {
        Throw-ToolchainError '实例 webapps' "专用 webapps 目录存在其他应用，脚本不会删除：$webappsPath。"
    }

    foreach ($fileName in $script:ManagedConfFiles) {
        $sourcePath = Join-Path $TomcatHomePath ('conf\' + $fileName)
        $destinationPath = Join-Path $confPath $fileName
        $commentPrefix = if ($fileName.EndsWith('.xml')) {
            '<!-- ' + $script:ManagedMarker + ' -->'
        }
        else {
            '# ' + $script:ManagedMarker
        }
        Write-ManagedCopy $sourcePath $destinationPath $commentPrefix
    }

    Write-XmlDocument (Join-Path $confPath 'server.xml') (New-ServerDocument $PortNumber)
    Write-XmlDocument (Join-Path $localhostPath 'personal_blog_system_war_exploded.xml') (New-ContextDocument $ArtifactPath)
    Write-SetenvFile (Join-Path $binPath 'setenv.bat')
}

function Open-InstanceLock {
    param([string]$InstancePath)

    $lockPath = Join-Path $InstancePath '.dev-backend.lock'
    try {
        return [IO.File]::Open($lockPath, [IO.FileMode]::OpenOrCreate, [IO.FileAccess]::ReadWrite, [IO.FileShare]::None)
    }
    catch [IO.IOException] {
        Throw-ToolchainError '实例锁' ("实例目录正在被另一个启动脚本使用：$InstancePath。")
    }
    catch {
        Throw-ToolchainError '实例锁' ("无法打开实例独占锁 $lockPath：$($_.Exception.Message)")
    }
}

function Get-EnvironmentSnapshot {
    $names = @(
        'JAVA_HOME',
        'JRE_HOME',
        'CATALINA_HOME',
        'CATALINA_BASE',
        'CATALINA_TMPDIR',
        'CATALINA_OPTS',
        'CATALINA_LOGGING_CONFIG',
        'BLOG_DEV_BACKEND_JAVA_HOME',
        'BLOG_DEV_BACKEND_CATALINA_OPTS',
        'BLOG_MEDIA_CLEANUP_ENABLED',
        'Path'
    )
    $values = @{}
    foreach ($name in $names) {
        $values[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
    }
    return $values
}

function Restore-Environment {
    param(
        [hashtable]$Values,
        [string]$Location,
        [object]$OutputEncoding,
        [Text.Encoding]$ConsoleOutputEncoding,
        [Text.Encoding]$ConsoleInputEncoding
    )

    foreach ($name in $Values.Keys) {
        [Environment]::SetEnvironmentVariable($name, $Values[$name], 'Process')
    }
    Set-Location -LiteralPath $Location
    Restore-ConsoleEncoding $OutputEncoding $ConsoleOutputEncoding $ConsoleInputEncoding
}

function Restore-ConsoleEncoding {
    param(
        [object]$OutputEncoding,
        [Text.Encoding]$ConsoleOutputEncoding,
        [Text.Encoding]$ConsoleInputEncoding
    )

    if ($null -eq $OutputEncoding) {
        Remove-Variable -Name OutputEncoding -Scope Global -ErrorAction SilentlyContinue
    }
    else {
        Set-Variable -Name OutputEncoding -Scope Global -Value $OutputEncoding
    }
    [Console]::OutputEncoding = $ConsoleOutputEncoding
    [Console]::InputEncoding = $ConsoleInputEncoding
}

function Invoke-DevBackend {
    [CmdletBinding()]
    param(
        [string]$JavaHome = $env:JAVA_HOME,
        [string]$TomcatHome = $env:BLOG_TOMCAT_HOME,
        [ValidateRange(1024, 65535)]
        [int]$Port = 8080,
        [switch]$Check
    )

    $exitCode = 1
    $lockStream = $null
    $oldLocation = (Get-Location).Path
    $oldEnvironment = Get-EnvironmentSnapshot
    $oldOutputEncoding = Get-Variable -Name OutputEncoding -Scope Global -ValueOnly -ErrorAction SilentlyContinue
    $oldConsoleOutputEncoding = [Console]::OutputEncoding
    $oldConsoleInputEncoding = [Console]::InputEncoding
    $utf8Encoding = New-Object Text.UTF8Encoding($false)
    $consoleRestored = $false
    try {
        Set-Variable -Name OutputEncoding -Scope Global -Value $utf8Encoding
        [Console]::OutputEncoding = $utf8Encoding
        [Console]::InputEncoding = $utf8Encoding
        $projectRootPath = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
        $javaHomePath = Resolve-ToolchainPath $JavaHome 'JDK'
        $tomcatHomePath = Resolve-ToolchainPath $TomcatHome 'Tomcat'
        $instancePath = Join-Path $projectRootPath ('backend\target\' + $script:InstanceName)

        $jdkInfo = Get-JdkInfo $javaHomePath
        $tomcatInfo = Get-TomcatInfo $tomcatHomePath
        $artifactInfo = Get-ArtifactInfo $projectRootPath
        Assert-PortAvailable $Port

        if ($Check) {
            Write-ToolchainMessage INFO '检查' '环境、产物和端口检查通过；-Check 不会创建实例目录或启动服务。'
            return 0
        }

        Ensure-Directory $instancePath '实例目录'
        $lockStream = Open-InstanceLock $instancePath
        Prepare-Instance $projectRootPath $artifactInfo.Root $tomcatHomePath $instancePath $Port

        try {
            Set-Location -LiteralPath $projectRootPath

            $existingCatalinaOpts = [string]$oldEnvironment['CATALINA_OPTS']
            $requiredCatalinaOpts = '-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -Dblog.media.cleanup.enabled=false'
            $combinedCatalinaOpts = (($existingCatalinaOpts, $requiredCatalinaOpts) | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }) -join ' '
            $env:JAVA_HOME = $javaHomePath
            $env:JRE_HOME = $javaHomePath
            $env:CATALINA_HOME = $tomcatHomePath
            $env:CATALINA_BASE = $instancePath
            $env:CATALINA_TMPDIR = Join-Path $instancePath 'temp'
            $env:CATALINA_OPTS = $combinedCatalinaOpts
            $env:CATALINA_LOGGING_CONFIG = '-Djava.util.logging.config.file="' + (Join-Path $instancePath 'conf\logging.properties') + '"'
            $env:BLOG_DEV_BACKEND_JAVA_HOME = $javaHomePath
            $env:BLOG_DEV_BACKEND_CATALINA_OPTS = $combinedCatalinaOpts
            $env:BLOG_MEDIA_CLEANUP_ENABLED = 'false'
            $env:Path = $javaHomePath + '\bin;' + [string]$oldEnvironment['Path']

            Write-ToolchainMessage INFO '启动' ("Java Home=$javaHomePath；Tomcat Home=$tomcatHomePath；CATALINA_BASE=$instancePath；端口=$Port。媒体清理器已强制禁用。")
            $previousErrorActionPreference = $ErrorActionPreference
            try {
                # Tomcat 的前台日志可能正常写入 stderr；只在该原生命令边界抑制误报，并保留实时输出。
                $ErrorActionPreference = 'Continue'
                & $tomcatInfo.Catalina run 2>&1 | ForEach-Object { Write-Host ([string]$_) }
                $tomcatExitCode = [int]$LASTEXITCODE
            }
            finally {
                $ErrorActionPreference = $previousErrorActionPreference
            }
            if ($tomcatExitCode -ne 0) {
                $exitCode = $tomcatExitCode
                Throw-ToolchainError 'Tomcat' ("catalina.bat run 返回退出码 $tomcatExitCode。")
            }
            $exitCode = 0
        }
        finally {
            Restore-Environment $oldEnvironment $oldLocation $oldOutputEncoding $oldConsoleOutputEncoding $oldConsoleInputEncoding
            $consoleRestored = $true
        }
        return $exitCode
    }
    catch {
        Write-ToolchainMessage ERROR 'dev-backend' $_.Exception.Message
        if (-not [string]::IsNullOrWhiteSpace($_.ScriptStackTrace)) {
            Write-ToolchainMessage DEBUG 'dev-backend' ("脚本位置：$($_.InvocationInfo.PositionMessage)")
        }
        return $exitCode
    }
    finally {
        if (-not $consoleRestored) {
            Restore-ConsoleEncoding $oldOutputEncoding $oldConsoleOutputEncoding $oldConsoleInputEncoding
        }
        if ($null -ne $lockStream) {
            $lockStream.Dispose()
        }
    }
}

if (-not $script:IsDotSourced) {
    $result = Invoke-DevBackend -JavaHome $JavaHome -TomcatHome $TomcatHome -Port $Port -Check:$Check
    exit ([int]$result)
}
