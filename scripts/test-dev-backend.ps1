[CmdletBinding()]
param(
    [string]$PowerShellExecutable
)

Set-StrictMode -Version 2.0
$ErrorActionPreference = 'Stop'

$scriptRoot = $PSScriptRoot
$projectRoot = Split-Path -Parent $scriptRoot
$devScriptPath = Join-Path $scriptRoot 'dev-backend.ps1'
$validJavaHome = 'D:\dev_tools\jdk-21-temurin'
$validTomcatHome = 'D:\dev_tools\tomcat\apache-tomcat-9.0.85'
$testRoot = Join-Path $projectRoot ('backend\target\dev-backend-script-tests\' + [Guid]::NewGuid().ToString('N'))
$failureCount = 0

function Assert-True {
    param(
        [bool]$Condition,
        [string]$Message
    )

    if (-not $Condition) {
        throw "断言失败：$Message"
    }
}

function Assert-Equal {
    param(
        [object]$Expected,
        [object]$Actual,
        [string]$Message
    )

    if ($Expected -ne $Actual) {
        throw "断言失败：$Message；期望 [$Expected]，实际 [$Actual]"
    }
}

function Assert-Contains {
    param(
        [string]$Text,
        [string]$Expected,
        [string]$Message
    )

    if ($Text.IndexOf($Expected, [StringComparison]::OrdinalIgnoreCase) -lt 0) {
        throw "断言失败：$Message；输出中没有 [$Expected]"
    }
}

function Assert-NonZeroExit {
    param(
        [object]$Result,
        [string]$Message
    )

    Assert-True ($Result.ExitCode -ne 0) "$Message；实际退出码为 $($Result.ExitCode)"
}

function Get-CombinedOutput {
    param([object]$Result)

    return "$($Result.StdOut)`n$($Result.StdErr)"
}

function ConvertTo-CommandLineArgument {
    param([string]$Value)

    if ($null -eq $Value) {
        return '""'
    }

    return '"' + ($Value -replace '"', '\"') + '"'
}

function Get-PowerShellExecutable {
    if (-not [string]::IsNullOrWhiteSpace($PowerShellExecutable)) {
        $requestedPath = [IO.Path]::GetFullPath($PowerShellExecutable)
        if (-not (Test-Path -LiteralPath $requestedPath -PathType Leaf)) {
            throw "指定的 PowerShell 可执行文件不存在：$requestedPath"
        }
        return $requestedPath
    }

    $hostExecutableName = if ($PSVersionTable.PSEdition -eq 'Desktop') { 'powershell.exe' } else { 'pwsh.exe' }
    $hostExecutablePath = Join-Path $PSHOME $hostExecutableName
    if (Test-Path -LiteralPath $hostExecutablePath -PathType Leaf) {
        return $hostExecutablePath
    }

    throw "找不到当前 PowerShell 宿主可执行文件：$hostExecutablePath"
}

function New-ProcessStartInfo {
    param(
        [string]$ScriptPath,
        [string]$JavaHome,
        [string]$TomcatHome,
        [int]$Port,
        [switch]$Check,
        [string]$WorkingDirectory,
        [hashtable]$Environment
    )

    $argumentParts = @(
        '-NoLogo',
        '-NoProfile',
        '-NonInteractive',
        '-ExecutionPolicy',
        'Bypass',
        '-File',
        (ConvertTo-CommandLineArgument $ScriptPath),
        '-JavaHome',
        (ConvertTo-CommandLineArgument $JavaHome),
        '-TomcatHome',
        (ConvertTo-CommandLineArgument $TomcatHome),
        '-Port',
        $Port.ToString()
    )
    if ($Check) {
        $argumentParts += '-Check'
    }

    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = Get-PowerShellExecutable
    $startInfo.Arguments = ($argumentParts -join ' ')
    $startInfo.WorkingDirectory = $WorkingDirectory
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    $startInfo.StandardOutputEncoding = [Text.Encoding]::UTF8
    $startInfo.StandardErrorEncoding = [Text.Encoding]::UTF8

    if ($null -ne $Environment) {
        foreach ($name in $Environment.Keys) {
            $startInfo.EnvironmentVariables[$name] = [string]$Environment[$name]
        }
    }

    return $startInfo
}

function Invoke-DevScript {
    param(
        [string]$ScriptPath,
        [string]$JavaHome,
        [string]$TomcatHome,
        [int]$Port,
        [switch]$Check,
        [string]$WorkingDirectory = $env:TEMP,
        [hashtable]$Environment
    )

    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = New-ProcessStartInfo @PSBoundParameters
    if (-not $process.Start()) {
        throw "无法启动测试进程：$ScriptPath"
    }

    $stdout = $process.StandardOutput.ReadToEnd()
    $stderr = $process.StandardError.ReadToEnd()
    $process.WaitForExit()

    return [pscustomobject]@{
        ExitCode = $process.ExitCode
        StdOut = $stdout
        StdErr = $stderr
    }
}

function Start-DevScript {
    param(
        [string]$ScriptPath,
        [string]$JavaHome,
        [string]$TomcatHome,
        [int]$Port,
        [string]$WorkingDirectory = $env:TEMP,
        [hashtable]$Environment
    )

    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = New-ProcessStartInfo @PSBoundParameters
    if (-not $process.Start()) {
        throw "无法启动测试进程：$ScriptPath"
    }

    return $process
}

function Get-FreeTcpPort {
    $listener = New-Object System.Net.Sockets.TcpListener([Net.IPAddress]::Loopback, 0)
    try {
        $listener.Start()
        return ([Net.IPEndPoint]$listener.LocalEndpoint).Port
    }
    finally {
        $listener.Stop()
    }
}

function Wait-Until {
    param(
        [scriptblock]$Condition,
        [int]$TimeoutMilliseconds = 15000
    )

    $deadline = [DateTime]::UtcNow.AddMilliseconds($TimeoutMilliseconds)
    while ([DateTime]::UtcNow -lt $deadline) {
        if (& $Condition) {
            return
        }
        Start-Sleep -Milliseconds 100
    }

    throw "等待测试条件超时：$TimeoutMilliseconds 毫秒"
}

function Complete-StartedProcess {
    param(
        [System.Diagnostics.Process]$Process,
        [string]$StopFile
    )

    if ($null -ne $StopFile) {
        [IO.File]::WriteAllText($StopFile, 'stop', [Text.Encoding]::UTF8)
    }
    if (-not $Process.HasExited) {
        $Process.WaitForExit(15000) | Out-Null
    }
    if (-not $Process.HasExited) {
        $Process.Kill()
        $Process.WaitForExit(5000) | Out-Null
    }
}

function New-DirectoryJunction {
    param(
        [string]$LinkPath,
        [string]$TargetPath
    )

    New-Item -ItemType Directory -Path (Split-Path -Parent $LinkPath) -Force | Out-Null
    New-Item -ItemType Junction -Path $LinkPath -Target $TargetPath | Out-Null
}

function New-TestProject {
    param(
        [string]$Name,
        [bool]$WithArtifact
    )

    $fixtureRoot = Join-Path $testRoot $Name
    $fixtureScriptRoot = Join-Path $fixtureRoot 'scripts'
    $fixtureBackendRoot = Join-Path $fixtureRoot 'backend'
    $fixtureTargetRoot = Join-Path $fixtureBackendRoot 'target'
    New-Item -ItemType Directory -Path $fixtureScriptRoot, $fixtureTargetRoot -Force | Out-Null
    Copy-Item -LiteralPath $devScriptPath -Destination (Join-Path $fixtureScriptRoot 'dev-backend.ps1')

    $artifactRoot = Join-Path $fixtureTargetRoot 'personal_blog_system_war_exploded'
    if ($WithArtifact) {
        New-Item -ItemType Directory -Path (Join-Path $artifactRoot 'WEB-INF\classes'), (Join-Path $artifactRoot 'WEB-INF\lib') -Force | Out-Null
        [IO.File]::WriteAllText((Join-Path $artifactRoot 'WEB-INF\web.xml'), '<web-app/>', [Text.Encoding]::UTF8)
        [IO.File]::WriteAllText((Join-Path $artifactRoot 'index.html'), '<!doctype html>', [Text.Encoding]::UTF8)
    }

    return [pscustomobject]@{
        Root = $fixtureRoot
        Script = Join-Path $fixtureScriptRoot 'dev-backend.ps1'
        Artifact = $artifactRoot
        Instance = Join-Path $fixtureTargetRoot 'tomcat9-dev'
    }
}

function New-TestTomcat {
    param([string]$Name)

    $tomcatRoot = Join-Path $testRoot ('tomcat fixture ' + $Name + ' 中文 & space')
    $tomcatBin = Join-Path $tomcatRoot 'bin'
    $tomcatConf = Join-Path $tomcatRoot 'conf'
    $tomcatLib = Join-Path $tomcatRoot 'lib'
    New-Item -ItemType Directory -Path $tomcatBin, $tomcatConf, $tomcatLib -Force | Out-Null

    Copy-Item -LiteralPath (Join-Path $validTomcatHome 'lib\catalina.jar') -Destination $tomcatLib
    foreach ($fileName in @('catalina.properties', 'context.xml', 'jaspic-providers.xml', 'logging.properties', 'tomcat-users.xml', 'web.xml')) {
        Copy-Item -LiteralPath (Join-Path $validTomcatHome ('conf\' + $fileName)) -Destination $tomcatConf
    }

    $batchContent = @'
@echo off
if "%BLOG_TEST_TOMCAT_MODE%"=="failure" echo fake catalina stderr 1>&2
if "%BLOG_TEST_TOMCAT_MODE%"=="failure" exit /b 7
if "%BLOG_TEST_TOMCAT_MODE%"=="success" echo fake catalina stderr 1>&2
if "%BLOG_TEST_TOMCAT_MODE%"=="success" exit /b 0
echo "%CATALINA_BASE%">"%CATALINA_BASE%\fake-catalina-base.txt"
:wait_for_stop
if exist "%CATALINA_BASE%\stop-test" exit /b 0
%SystemRoot%\System32\ping.exe -n 2 127.0.0.1 >nul
goto wait_for_stop
'@
    [IO.File]::WriteAllText((Join-Path $tomcatBin 'catalina.bat'), $batchContent, [Text.Encoding]::ASCII)

    return $tomcatRoot
}

function Invoke-TestCase {
    param(
        [string]$Name,
        [scriptblock]$Action
    )

    try {
        & $Action
        Write-Host "[PASS] $Name"
    }
    catch {
        $script:failureCount++
        Write-Host "[FAIL] $Name：$($_.Exception.Message)"
    }
}

try {
    if (-not (Test-Path -LiteralPath $devScriptPath -PathType Leaf)) {
        throw "测试红灯：启动入口尚不存在：$devScriptPath"
    }
    if (-not (Test-Path -LiteralPath $validJavaHome -PathType Container)) {
        throw "测试环境缺少已授权安装的 JDK 21：$validJavaHome"
    }
    if (-not (Test-Path -LiteralPath $validTomcatHome -PathType Container)) {
        throw "测试环境缺少外部 Tomcat 9：$validTomcatHome"
    }
    Write-Host ("[INFO] 隔离子进程使用 PowerShell：{0}" -f (Get-PowerShellExecutable))

    $scriptBytes = [IO.File]::ReadAllBytes($devScriptPath)
    Assert-True ($scriptBytes.Length -ge 3 -and $scriptBytes[0] -eq 0xEF -and $scriptBytes[1] -eq 0xBB -and $scriptBytes[2] -eq 0xBF) '含中文的启动脚本必须使用 UTF-8 BOM'

    New-Item -ItemType Directory -Path $testRoot -Force | Out-Null
    $fixture = New-TestProject -Name 'project fixture 中文 & space' -WithArtifact $true
    $fixtureTomcat = New-TestTomcat -Name 'main'
    $fixtureJdk = Join-Path $testRoot 'jdk fixture 中文 & space'
    New-DirectoryJunction -LinkPath $fixtureJdk -TargetPath $validJavaHome

    Invoke-TestCase '缺失 JDK 路径返回非零并指出路径' {
        $missing = Join-Path $testRoot 'missing-jdk'
        $result = Invoke-DevScript -ScriptPath $fixture.Script -JavaHome $missing -TomcatHome $fixtureTomcat -Port (Get-FreeTcpPort) -Check
        $output = Get-CombinedOutput $result
        Assert-NonZeroExit $result '缺失 JDK 必须拒绝启动'
        Assert-Contains $output $missing '缺失 JDK 错误必须包含实际路径'
    }

    Invoke-TestCase 'JDK 23 被明确拒绝' {
        $result = Invoke-DevScript -ScriptPath $fixture.Script -JavaHome 'D:\dev_tools\jdk' -TomcatHome $fixtureTomcat -Port (Get-FreeTcpPort) -Check
        $output = Get-CombinedOutput $result
        Assert-NonZeroExit $result 'JDK 23 不应通过 JDK 检查'
        Assert-Contains $output 'JDK 21' 'JDK 版本错误必须说明要求 JDK 21'
    }

    Invoke-TestCase '缺失或非 9.x Tomcat 被拒绝' {
        $result = Invoke-DevScript -ScriptPath $fixture.Script -JavaHome $fixtureJdk -TomcatHome $validJavaHome -Port (Get-FreeTcpPort) -Check
        $output = Get-CombinedOutput $result
        Assert-NonZeroExit $result '非 Tomcat 目录必须拒绝启动'
        Assert-Contains $output 'Tomcat' 'Tomcat 错误必须说明容器要求'
        Assert-Contains $output '9' 'Tomcat 错误必须说明要求 9.x'
    }

    Invoke-TestCase '缺少构建产物列出构建步骤' {
        $missingArtifactFixture = New-TestProject -Name 'missing artifact fixture' -WithArtifact $false
        $result = Invoke-DevScript -ScriptPath $missingArtifactFixture.Script -JavaHome $fixtureJdk -TomcatHome $fixtureTomcat -Port (Get-FreeTcpPort) -Check
        $output = Get-CombinedOutput $result
        Assert-NonZeroExit $result '缺少 exploded WAR 必须拒绝启动'
        Assert-Contains $output 'pnpm build' '产物错误必须提示前端构建步骤'
        Assert-Contains $output 'mvnw.cmd package' '产物错误必须提示 Maven 打包步骤'
    }

    Invoke-TestCase 'JDK 21 的正常 stderr 不误报' {
        $result = Invoke-DevScript -ScriptPath $fixture.Script -JavaHome $fixtureJdk -TomcatHome $fixtureTomcat -Port (Get-FreeTcpPort) -Check
        $output = Get-CombinedOutput $result
        Assert-Equal 0 $result.ExitCode 'JDK 版本输出到 stderr 时 -Check 仍应返回 0'
        Assert-Contains $output '使用 JDK 21' 'JDK 检查应解析 stderr 中的版本信息'
    }

    Invoke-TestCase '端口占用报告端口和 PID 且不终止占用者' {
        $listener = New-Object System.Net.Sockets.TcpListener([Net.IPAddress]::Loopback, 0)
        try {
            $listener.Start()
            $port = ([Net.IPEndPoint]$listener.LocalEndpoint).Port
            $result = Invoke-DevScript -ScriptPath $fixture.Script -JavaHome $fixtureJdk -TomcatHome $fixtureTomcat -Port $port -Check
            $output = Get-CombinedOutput $result
            Assert-NonZeroExit $result '已占用端口必须拒绝启动'
            Assert-Contains $output $port.ToString() '端口错误必须包含端口号'
            Assert-Contains $output $PID.ToString() '端口错误必须包含监听 PID'
            Assert-True (-not $listener.Server.IsBound -eq $false) '端口占用检查不应关闭测试监听器'
        }
        finally {
            $listener.Stop()
        }
    }

    Invoke-TestCase '正常 -Check 不创建实例目录或启动进程' {
        $port = Get-FreeTcpPort
        Assert-True (-not (Test-Path -LiteralPath $fixture.Instance)) '测试前实例目录不应存在'
        $result = Invoke-DevScript -ScriptPath $fixture.Script -JavaHome $fixtureJdk -TomcatHome $fixtureTomcat -Port $port -Check -WorkingDirectory $env:TEMP
        $output = Get-CombinedOutput $result
        Assert-Equal 0 $result.ExitCode '正常 -Check 应返回 0'
        Assert-Contains $output '检查通过' '正常 -Check 应输出检查通过'
        Assert-True (-not (Test-Path -LiteralPath $fixture.Instance)) '-Check 结束后不得创建 CATALINA_BASE'
    }

    Invoke-TestCase '未知实例内容被拒绝且不删除' {
        $unknownFixture = New-TestProject -Name 'unknown instance fixture' -WithArtifact $true
        New-Item -ItemType Directory -Path $unknownFixture.Instance -Force | Out-Null
        $unknownFile = Join-Path $unknownFixture.Instance 'user-owned.txt'
        [IO.File]::WriteAllText($unknownFile, 'keep', [Text.Encoding]::UTF8)
        $result = Invoke-DevScript -ScriptPath $unknownFixture.Script -JavaHome $fixtureJdk -TomcatHome $fixtureTomcat -Port (Get-FreeTcpPort)
        $output = Get-CombinedOutput $result
        Assert-NonZeroExit $result '实例目录存在未知内容时必须拒绝启动'
        Assert-Contains $output '未知' '未知实例内容错误必须可识别'
        Assert-True (Test-Path -LiteralPath $unknownFile) '脚本不得删除未知实例内容'
    }

    Invoke-TestCase '同一实例目录的第二个启动被独占锁拒绝' {
        $lockFixture = New-TestProject -Name 'lock fixture 中文 & space' -WithArtifact $true
        $lockTomcat = New-TestTomcat -Name 'lock'
        $lockJdk = Join-Path $testRoot 'lock jdk 中文 & space'
        New-DirectoryJunction -LinkPath $lockJdk -TargetPath $validJavaHome
        $firstPort = Get-FreeTcpPort
        $secondPort = Get-FreeTcpPort
        $firstProcess = $null
        $stopFile = Join-Path $lockFixture.Instance 'stop-test'
        $firstOutput = ''
        try {
            $firstProcess = Start-DevScript -ScriptPath $lockFixture.Script -JavaHome $lockJdk -TomcatHome $lockTomcat -Port $firstPort -Environment @{ BLOG_TEST_TOMCAT_MODE = 'wait' }
            Wait-Until { (Test-Path -LiteralPath (Join-Path $lockFixture.Instance '.dev-backend-instance.json')) -and (Test-Path -LiteralPath (Join-Path $lockFixture.Instance '.dev-backend.lock')) }
            $secondResult = Invoke-DevScript -ScriptPath $lockFixture.Script -JavaHome $lockJdk -TomcatHome $lockTomcat -Port $secondPort -Environment @{ BLOG_TEST_TOMCAT_MODE = 'wait' }
            $secondOutput = Get-CombinedOutput $secondResult
            Assert-NonZeroExit $secondResult '同一实例目录的第二次启动必须失败'
            Assert-Contains $secondOutput '锁' '第二次启动必须说明实例锁冲突'
            $contextPath = Join-Path $lockFixture.Instance 'conf\Catalina\localhost\personal_blog_system_war_exploded.xml'
            $serverPath = Join-Path $lockFixture.Instance 'conf\server.xml'
            $contextRaw = [IO.File]::ReadAllText($contextPath, [Text.Encoding]::UTF8)
            $serverXml = [xml]$contextRaw
            $contextXml = [xml]$contextRaw
            Assert-True ($contextRaw.Contains('&amp;')) '上下文 XML 必须转义含 & 的绝对路径'
            Assert-Equal ([IO.Path]::GetFullPath($lockFixture.Artifact)) $contextXml.Context.docBase '上下文 docBase 必须指向当前 exploded WAR'
            foreach ($managedXmlName in @('context.xml', 'jaspic-providers.xml', 'tomcat-users.xml', 'web.xml')) {
                $managedXmlPath = Join-Path $lockFixture.Instance ('conf\' + $managedXmlName)
                $managedXmlRaw = [IO.File]::ReadAllText($managedXmlPath, [Text.Encoding]::UTF8)
                $null = [xml]$managedXmlRaw
            }
            $serverXml = [xml]([IO.File]::ReadAllText($serverPath, [Text.Encoding]::UTF8))
            Assert-Equal '-1' $serverXml.Server.port 'Tomcat shutdown 端口必须关闭'
            Assert-Equal '127.0.0.1' $serverXml.Server.Service.Connector.address 'HTTP 入口必须限制到回环地址'
            Assert-Equal $firstPort.ToString() $serverXml.Server.Service.Connector.port 'HTTP 端口必须使用脚本参数'
            Assert-True ($null -eq $serverXml.SelectSingleNode('//Connector[@protocol="AJP/1.3"]')) '专用 server.xml 不应启用 AJP'
            Assert-True ((@(Get-ChildItem -LiteralPath (Join-Path $lockFixture.Instance 'webapps') -Force)).Count -eq 0) '专用 webapps 不应包含其他应用'
        }
        finally {
            if ($null -ne $firstProcess) {
                Complete-StartedProcess -Process $firstProcess -StopFile $stopFile
                $firstOutput = "$($firstProcess.StandardOutput.ReadToEnd())`n$($firstProcess.StandardError.ReadToEnd())"
            }
        }
        if ($firstProcess.ExitCode -ne 0) {
            $baseEvidencePath = Join-Path $lockFixture.Instance 'fake-catalina-base.txt'
            $baseEvidence = if (Test-Path -LiteralPath $baseEvidencePath) { [IO.File]::ReadAllText($baseEvidencePath, [Text.Encoding]::UTF8) } else { '<fake Tomcat 未记录 CATALINA_BASE>' }
            throw "第一个锁持有进程应正常结束；实际退出码 [$($firstProcess.ExitCode)]；fake CATALINA_BASE [$baseEvidence]；输出 [$firstOutput]"
        }
    }

    Invoke-TestCase 'Tomcat 非零退出码原样返回' {
        $failureFixture = New-TestProject -Name 'tomcat failure fixture' -WithArtifact $true
        $failureTomcat = New-TestTomcat -Name 'failure'
        $failureJdk = Join-Path $testRoot 'failure jdk'
        New-DirectoryJunction -LinkPath $failureJdk -TargetPath $validJavaHome
        $result = Invoke-DevScript -ScriptPath $failureFixture.Script -JavaHome $failureJdk -TomcatHome $failureTomcat -Port (Get-FreeTcpPort) -Environment @{ BLOG_TEST_TOMCAT_MODE = 'failure' }
        $output = Get-CombinedOutput $result
        Assert-Equal 7 $result.ExitCode 'Tomcat 退出码必须保留'
        Assert-Contains $output 'fake catalina stderr' 'Tomcat stderr 必须保留在诊断输出中'
        Assert-Contains $output '7' 'Tomcat 启动失败必须输出原始退出码'
    }

    Invoke-TestCase 'Tomcat 正常 stderr 不误报且退出码为 0' {
        $successFixture = New-TestProject -Name 'tomcat stderr success fixture' -WithArtifact $true
        $successTomcat = New-TestTomcat -Name 'stderr success'
        $successJdk = Join-Path $testRoot 'stderr success jdk'
        New-DirectoryJunction -LinkPath $successJdk -TargetPath $validJavaHome
        $result = Invoke-DevScript -ScriptPath $successFixture.Script -JavaHome $successJdk -TomcatHome $successTomcat -Port (Get-FreeTcpPort) -Environment @{ BLOG_TEST_TOMCAT_MODE = 'success' }
        $output = Get-CombinedOutput $result
        Assert-Equal 0 $result.ExitCode 'Tomcat 正常 stderr 不应导致启动失败'
        Assert-Contains $output 'fake catalina stderr' 'Tomcat 正常 stderr 必须保留在启动输出中'
    }

    Invoke-TestCase '正常启动恢复环境、位置和控制台编码' {
        $restoreFixture = New-TestProject -Name 'restore fixture' -WithArtifact $true
        $restoreTomcat = New-TestTomcat -Name 'restore'
        $restoreJdk = Join-Path $testRoot 'restore jdk'
        New-DirectoryJunction -LinkPath $restoreJdk -TargetPath $validJavaHome
        $previousJavaHome = $env:JAVA_HOME
        $previousCatalinaBase = $env:CATALINA_BASE
        $previousLocation = (Get-Location).Path
        $previousOutputEncoding = $OutputEncoding
        $previousConsoleOutputEncoding = [Console]::OutputEncoding
        $previousConsoleInputEncoding = [Console]::InputEncoding
        try {
            . $restoreFixture.Script
            $ErrorActionPreference = 'Continue'
            $previousErrorActionPreference = $ErrorActionPreference
            $env:JAVA_HOME = 'restore-java-home'
            $env:CATALINA_BASE = 'restore-catalina-base'
            $env:BLOG_TEST_TOMCAT_MODE = 'success'
            Push-Location $env:TEMP
            $results = @(Invoke-DevBackend -JavaHome $restoreJdk -TomcatHome $restoreTomcat -Port (Get-FreeTcpPort))
            if ($results.Count -eq 0) {
                throw '启动函数没有返回退出码。'
            }
            $result = $results[$results.Count - 1]
            Assert-Equal 0 $result '直接调用启动函数应成功返回 0'
            Assert-Equal 'restore-java-home' $env:JAVA_HOME 'JAVA_HOME 必须恢复'
            Assert-Equal 'restore-catalina-base' $env:CATALINA_BASE 'CATALINA_BASE 必须恢复'
            Assert-Equal $env:TEMP (Get-Location).Path '工作目录必须恢复到调用前位置'
            Assert-Equal $previousOutputEncoding.WebName $OutputEncoding.WebName 'PowerShell 输出编码必须恢复'
            Assert-Equal $previousConsoleOutputEncoding.WebName ([Console]::OutputEncoding.WebName) '控制台输出编码必须恢复'
            Assert-Equal $previousConsoleInputEncoding.WebName ([Console]::InputEncoding.WebName) '控制台输入编码必须恢复'
            Assert-Equal $previousErrorActionPreference $ErrorActionPreference '原生命令边界必须恢复 PowerShell 错误策略'
        }
        finally {
            Pop-Location -ErrorAction SilentlyContinue
            if ($null -ne (Get-Variable -Name previousErrorActionPreference -ErrorAction SilentlyContinue)) {
                $ErrorActionPreference = $previousErrorActionPreference
            }
            if ($null -eq $previousJavaHome) { Remove-Item Env:JAVA_HOME -ErrorAction SilentlyContinue } else { $env:JAVA_HOME = $previousJavaHome }
            if ($null -eq $previousCatalinaBase) { Remove-Item Env:CATALINA_BASE -ErrorAction SilentlyContinue } else { $env:CATALINA_BASE = $previousCatalinaBase }
            Remove-Item Env:BLOG_TEST_TOMCAT_MODE -ErrorAction SilentlyContinue
        }
    }
}
finally {
    if (Test-Path -LiteralPath $testRoot) {
        Remove-Item -LiteralPath $testRoot -Recurse -Force
    }
}

if ($failureCount -gt 0) {
    Write-Error "启动脚本测试失败：$failureCount 项"
    exit 1
}

Write-Host '[PASS] 所有 dev-backend.ps1 隔离测试通过'
exit 0
