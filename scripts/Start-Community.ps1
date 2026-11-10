param(
    [switch]$NoBrowser,
    [switch]$CheckOnly,
    [switch]$Rebuild
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'Community-Environment.ps1')

function Test-CommunityTcp {
    param([string]$HostName, [int]$Port)
    $client = New-Object Net.Sockets.TcpClient
    try {
        $pending = $client.BeginConnect($HostName, $Port, $null, $null)
        if (-not $pending.AsyncWaitHandle.WaitOne(1500)) { return $false }
        $client.EndConnect($pending)
        return $true
    } catch { return $false } finally { $client.Dispose() }
}

function Test-CommunityBackend {
    param([string]$BaseUrl)
    try {
        $response = Invoke-RestMethod -Uri "$BaseUrl/api/v1/setup/check" -TimeoutSec 5
        return ($response.code -eq 200 -and $null -ne $response.data -and
            $response.data.needSetup -is [bool])
    } catch { return $false }
}

function Test-CommunityFrontend {
    param([string]$BaseUrl)
    try {
        $response = Invoke-WebRequest -Uri "$BaseUrl/community/login" -UseBasicParsing -TimeoutSec 5
        return ($response.StatusCode -eq 200 -and $response.Content -match '<div id="app">' -and
            $response.Content -match '<script[^>]+type="module"')
    } catch { return $false }
}

function Import-CommunityEnv {
    param([string]$Path)
    if (-not (Test-Path -LiteralPath $Path)) { return }
    foreach ($line in Get-Content -LiteralPath $Path -Encoding UTF8) {
        if ($line -match '^\s*(#|$)') { continue }
        if ($line -notmatch '^\s*([A-Z][A-Z0-9_]*)\s*=(.*)$') {
            throw 'Invalid .env line. Use NAME=value only; shell expressions are not supported.'
        }
        $name = $Matches[1]
        $value = $Matches[2].Trim()
        if ($value.Length -ge 2 -and (($value.StartsWith('"') -and $value.EndsWith('"')) -or
            ($value.StartsWith("'") -and $value.EndsWith("'")))) { $value = $value.Substring(1, $value.Length - 2) }
        if ($null -eq [Environment]::GetEnvironmentVariable($name, 'Process')) {
            [Environment]::SetEnvironmentVariable($name, $value, 'Process')
        }
    }
}

function Find-CommunityJava {
    $candidates = @()
    if ($env:JAVA_HOME) { $candidates += Join-Path $env:JAVA_HOME 'bin/java.exe' }
    $command = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($command) { $candidates += $command.Source }
    $jdkDirectory = Join-Path ([Environment]::GetFolderPath('UserProfile')) '.jdks'
    if (Test-Path -LiteralPath $jdkDirectory) {
        $candidates += @(Get-ChildItem -LiteralPath $jdkDirectory -Directory | ForEach-Object { Join-Path $_.FullName 'bin/java.exe' })
    }
    foreach ($pattern in @("$env:ProgramFiles/Microsoft/jdk-17*/bin/java.exe", "$env:ProgramFiles/Java/jdk-17*/bin/java.exe", "$env:ProgramFiles/Eclipse Adoptium/jdk-17*/bin/java.exe")) {
        $candidates += @(Get-Item -Path $pattern -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName)
    }
    foreach ($candidate in $candidates | Select-Object -Unique) {
        $jdk = Split-Path (Split-Path $candidate -Parent) -Parent
        $release = Join-Path $jdk 'release'
        if ((Test-Path -LiteralPath $candidate) -and (Test-Path -LiteralPath (Join-Path $jdk 'bin/javac.exe')) -and
            (Test-Path -LiteralPath $release) -and
            ((Get-Content -LiteralPath $release) -match '^JAVA_VERSION="17[.\-"]')) { return $candidate }
    }
    throw 'JDK 17 was not found. Install JDK 17 and set JAVA_HOME or add its bin directory to PATH.'
}

function Test-CommunityBuildRequired {
    param([string]$BackendDirectory, [string]$JarPath)
    if (-not (Test-Path -LiteralPath $JarPath)) { return $true }
    $builtAt = (Get-Item -LiteralPath $JarPath).LastWriteTimeUtc
    foreach ($module in @('smart-community-parent', 'smart-community-common', 'smart-community-api', 'smart-community-admin')) {
        $moduleDirectory = Join-Path $BackendDirectory $module
        if ((Get-Item -LiteralPath (Join-Path $moduleDirectory 'pom.xml')).LastWriteTimeUtc -gt $builtAt) { return $true }
        $source = Join-Path $moduleDirectory 'src'
        if ((Test-Path -LiteralPath $source) -and
            (Get-ChildItem -LiteralPath $source -File -Recurse | Where-Object { $_.LastWriteTimeUtc -gt $builtAt } | Select-Object -First 1)) { return $true }
    }
    return $false
}

function Start-CommunityProcess {
    param([string]$Executable, [string[]]$Arguments, [string]$Directory, [string]$LogPrefix)
    # Start-Process joins arguments on Windows, so quote paths containing spaces explicitly.
    $quoted = @($Arguments | ForEach-Object {
        if ($_ -match '["\r\n]') { throw 'Unsupported quote or newline in a process argument.' }
        if ($_ -match '\s') { '"' + $_ + '"' } else { $_ }
    })
    $process = Start-Process -FilePath $Executable -ArgumentList $quoted -WorkingDirectory $Directory `
        -WindowStyle Hidden -RedirectStandardOutput "$LogPrefix.out.log" `
        -RedirectStandardError "$LogPrefix.err.log" -PassThru
    # Keep the process handle open so Windows PowerShell 5.1 can read an early exit code.
    $null = $process.Handle
    return $process
}

function Wait-CommunityService {
    param([string]$Name, [string]$Url, [scriptblock]$Probe, [Diagnostics.Process]$Process,
        [int]$TimeoutSeconds, [string]$LogPrefix)
    $deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
    do {
        if (& $Probe $Url) { return }
        if ($Process.HasExited) { throw "$Name exited ($($Process.ExitCode)). See $LogPrefix.out.log and .err.log" }
        Start-Sleep -Milliseconds 1000
    } while ([DateTime]::UtcNow -lt $deadline)
    throw "$Name did not become ready within $TimeoutSeconds seconds. See $LogPrefix.out.log and .err.log"
}

function Invoke-CommunityLauncher {
    param([string]$Root, [switch]$NoBrowser, [switch]$CheckOnly, [switch]$Rebuild)
    $backendDirectory = Join-Path $Root 'smart-community-backend'
    $adminDirectory = Join-Path $backendDirectory 'smart-community-admin'
    $frontendDirectory = Join-Path $Root 'smart-community-frontend'
    $jar = Join-Path $adminDirectory 'target/smart-community-admin-1.0.0-SNAPSHOT.jar'
    $logDirectory = Join-Path $Root '.runtime/launcher'
    $backendUrl = 'http://127.0.0.1:8001'
    $frontendUrl = 'http://127.0.0.1:5174'
    $mutex = New-Object Threading.Mutex($false, 'Local\SmartCommunityLauncher-8001-5174')
    $locked = $false
    $transcriptStarted = $false
    try {
        try { $locked = $mutex.WaitOne(0) } catch [Threading.AbandonedMutexException] { $locked = $true }
        if (-not $locked) { throw 'Another launcher is already starting this project. Please wait.' }
        if (-not $CheckOnly) {
            New-Item -ItemType Directory -Path $logDirectory -Force | Out-Null
            $session = Get-Date -Format 'yyyyMMdd-HHmmss-fff'
            Start-Transcript -Path (Join-Path $logDirectory "$session-launcher.log") | Out-Null
            $transcriptStarted = $true
        }

        Write-Host '[1/5] Checking Java, Node.js and local configuration...'
        $envError = $false
        try { Import-CommunityEnv (Join-Path $adminDirectory '.env') } catch { $envError = $true }
        $checks = @(Get-CommunityEnvironment -Root $Root -Rebuild:$Rebuild)
        if ($envError) { $checks += [pscustomobject]@{Name='.env syntax';Category='Runtime';Status='BLOCK';Detail='Invalid configuration syntax.';Action='Use NAME=value; no shell expressions. The invalid line is not displayed to protect secrets.'} }
        $report = Write-CommunityEnvironmentReport -Items $checks -Root $Root
        if (-not $NoBrowser) {
            try { Start-Process $report } catch { Write-Warning "Cannot open report automatically. Open $report manually." }
        }
        if ($checks | Where-Object Status -eq 'BLOCK') { throw 'Environment preflight found blocking issues. All missing items are listed in environment-report.html.' }
        if ($CheckOnly) { Write-Host 'Check complete. No project or database services were started.'; return }
        $java = Find-CommunityJava
        $env:JAVA_HOME = Split-Path (Split-Path $java -Parent) -Parent
        $env:Path = (Join-Path $env:JAVA_HOME 'bin') + ';' + $env:Path
        $nodePath = ($checks | Where-Object Name -eq 'Node.js 18+').Executable
        $node = Get-Item -LiteralPath $nodePath
        $env:Path = $node.DirectoryName + ';' + $env:Path
        $nodeVersion = & $node.FullName -p 'process.versions.node'
        if ($LASTEXITCODE -ne 0 -or [int]($nodeVersion.Split('.')[0]) -lt 18) { throw 'Node.js 18+ is required.' }
        $vite = Join-Path $frontendDirectory 'node_modules/vite/bin/vite.js'
        if (-not (Test-Path -LiteralPath $vite)) { throw 'Frontend dependencies are missing. Run npm ci in smart-community-frontend first.' }
        Write-Host "JDK 17: $java"
        Write-Host "Node.js: $nodeVersion"

        Write-Host '[2/5] Checking MySQL...'
        $mysqlHost = if ($env:MYSQL_HOST) { $env:MYSQL_HOST } else { 'localhost' }
        $mysqlPort = if ($env:MYSQL_PORT) { [int]$env:MYSQL_PORT } else { 3306 }
        if ($mysqlPort -lt 1 -or $mysqlPort -gt 65535) { throw 'MYSQL_PORT must be between 1 and 65535.' }
        if (-not (Test-CommunityTcp $mysqlHost $mysqlPort)) {
            if ($mysqlHost -in @('localhost', '127.0.0.1', '::1') -and $mysqlPort -eq 3306) {
                $mysqlService = Get-Service -Name 'MySQL80' -ErrorAction SilentlyContinue
                if ($mysqlService -and $mysqlService.Status -ne 'Running' -and -not $CheckOnly) {
                    try {
                        Start-Service -Name 'MySQL80'
                        $mysqlService.WaitForStatus('Running', [TimeSpan]::FromSeconds(30))
                    } catch { throw 'MySQL80 is stopped. Start it in Windows Services as administrator, then retry. No database was changed.' }
                }
            }
            if (-not (Test-CommunityTcp $mysqlHost $mysqlPort)) { throw "MySQL is not reachable at ${mysqlHost}:$mysqlPort. Check the service and MYSQL_HOST/MYSQL_PORT." }
        }

        Write-Host '[3/5] Checking backend...'
        $backendReady = Test-CommunityBackend $backendUrl
        $buildRequired = Test-CommunityBuildRequired $backendDirectory $jar
        if ($backendReady) {
            Write-Host 'Backend is healthy; reusing it without a restart.'
            if ($buildRequired -or $Rebuild) { Write-Warning 'Source/build changes detected. The running backend is preserved; stop it deliberately before rebuilding.' }
        } else {
            if (Test-CommunityTcp '127.0.0.1' 8001) { throw 'Port 8001 is occupied or the backend is unhealthy. No existing process was stopped. Check its logs.' }
            if ($buildRequired -or $Rebuild) {
                $maven = Find-CommunityProgram @('mvn.cmd') @($(if ($env:MAVEN_HOME) { Join-Path $env:MAVEN_HOME 'bin/mvn.cmd' }), $(if ($env:M2_HOME) { Join-Path $env:M2_HOME 'bin/mvn.cmd' }))
                if (-not $maven) { throw 'A backend build is needed. Install Maven 3.9+ and add it to PATH.' }
                if (-not $CheckOnly) {
                    Write-Host 'Building backend; the first build may need network access...'
                    Push-Location $backendDirectory
                    try {
                        & $maven '-f' 'smart-community-parent/pom.xml' '-DskipTests' 'package'
                        if ($LASTEXITCODE -ne 0) { throw 'Maven build failed. Read the launcher log. Existing data was not reimported.' }
                    } finally { Pop-Location }
                } else { Write-Host 'Backend would be built before startup.' }
            }
            if (-not $CheckOnly) {
                $prefix = Join-Path $logDirectory "$session-backend"
                $process = Start-CommunityProcess $java @('--enable-preview', '-jar', $jar) $adminDirectory $prefix
                Wait-CommunityService 'Backend' $backendUrl ${function:Test-CommunityBackend} $process 120 $prefix
            }
        }

        Write-Host '[4/5] Checking frontend and API proxy...'
        $frontendReady = Test-CommunityFrontend $frontendUrl
        if ($frontendReady) { Write-Host 'Frontend is available; reusing it.' }
        elseif (Test-CommunityTcp '127.0.0.1' 5174) { throw 'Port 5174 is occupied by an unknown service. No existing process was stopped.' }
        elseif (-not $CheckOnly) {
            $prefix = Join-Path $logDirectory "$session-frontend"
            $process = Start-CommunityProcess $node.FullName @($vite, '--host', '127.0.0.1', '--port', '5174', '--strictPort') $frontendDirectory $prefix
            Wait-CommunityService 'Frontend' $frontendUrl ${function:Test-CommunityFrontend} $process 45 $prefix
        }
        if (-not $CheckOnly -or ($frontendReady -and $backendReady)) {
            if (-not (Test-CommunityBackend $frontendUrl)) { throw 'Frontend API proxy failed. Check frontend logs and backend connectivity; opening the login page alone is not sufficient.' }
        }

        if ($CheckOnly) { Write-Host '[5/5] Environment check completed.' -ForegroundColor Green }
        else { Write-Host '[5/5] Ready.' -ForegroundColor Green }
        Write-Host "Management: $frontendUrl/login"
        Write-Host "Community:  $frontendUrl/community/login"
        if ($CheckOnly) { Write-Host 'Check-only mode: no services were started and no browser was opened.' }
        elseif (-not $NoBrowser) {
            Start-Process "$frontendUrl/login"
            Start-Process "$frontendUrl/community/login"
        }
        Write-Host 'Closing this launcher does not stop the project. Reopening it will reuse healthy services.'
    } catch {
        Write-Host "Launcher error: $($_.Exception.Message)" -ForegroundColor Red
        throw
    } finally {
        if ($transcriptStarted) { Stop-Transcript | Out-Null }
        if ($locked) { $mutex.ReleaseMutex() }
        $mutex.Dispose()
    }
}

if ($MyInvocation.InvocationName -ne '.') {
    try {
        Invoke-CommunityLauncher -Root (Split-Path $PSScriptRoot -Parent) -NoBrowser:$NoBrowser -CheckOnly:$CheckOnly -Rebuild:$Rebuild
        exit 0
    } catch {
        Write-Host "STARTUP FAILED: $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
}
