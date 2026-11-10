$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'Start-Community.ps1')

function Assert-Launcher {
    param([bool]$Condition, [string]$Message)
    if (-not $Condition) { throw $Message }
}

$temporary = Join-Path ([IO.Path]::GetTempPath()) ('community-launcher-' + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $temporary | Out-Null
$listener = $null
$child = $null
$frontend = $null
$previousValue = [Environment]::GetEnvironmentVariable('COMMUNITY_LAUNCHER_TEST_VALUE', 'Process')
$previousLiteral = [Environment]::GetEnvironmentVariable('COMMUNITY_LAUNCHER_TEST_LITERAL', 'Process')
try {
    Assert-Launcher ((Find-CommunityJava) -match 'java.exe$') 'JDK 17 detection failed.'
    $listener = New-Object Net.Sockets.TcpListener([Net.IPAddress]::Loopback, 0)
    $listener.Start()
    $port = $listener.LocalEndpoint.Port
    Assert-Launcher (Test-CommunityTcp '127.0.0.1' $port) 'Listening TCP port was not detected.'
    $listener.Stop()
    Assert-Launcher (-not (Test-CommunityTcp '127.0.0.1' $port)) 'Closed TCP port was reported available.'
    Assert-Launcher (-not (Test-CommunityBackend "http://127.0.0.1:$port")) 'Unreachable API was marked healthy.'
    Assert-Launcher (-not (Test-CommunityFrontend "http://127.0.0.1:$port")) 'Unreachable frontend was marked healthy.'

    $missingJar = Join-Path $temporary 'missing.jar'
    Assert-Launcher (Test-CommunityBuildRequired $temporary $missingJar) 'Missing JAR was not marked for build.'
    foreach ($module in @('smart-community-parent', 'smart-community-common', 'smart-community-api', 'smart-community-admin')) {
        $modulePath = Join-Path $temporary $module
        New-Item -ItemType Directory -Path $modulePath | Out-Null
        New-Item -ItemType File -Path (Join-Path $modulePath 'pom.xml') | Out-Null
    }
    $jar = Join-Path $temporary 'test.jar'
    New-Item -ItemType File -Path $jar | Out-Null
    (Get-Item -LiteralPath $jar).LastWriteTimeUtc = [DateTime]::UtcNow.AddMinutes(1)
    Assert-Launcher (-not (Test-CommunityBuildRequired $temporary $jar)) 'Current build was incorrectly marked stale.'
    (Get-Item -LiteralPath (Join-Path $temporary 'smart-community-common/pom.xml')).LastWriteTimeUtc = [DateTime]::UtcNow.AddMinutes(2)
    Assert-Launcher (Test-CommunityBuildRequired $temporary $jar) 'Changed POM was not detected.'

    [Environment]::SetEnvironmentVariable('COMMUNITY_LAUNCHER_TEST_VALUE', 'inherited', 'Process')
    [Environment]::SetEnvironmentVariable('COMMUNITY_LAUNCHER_TEST_LITERAL', $null, 'Process')
    Import-CommunityEnv (Join-Path $PSScriptRoot 'fixtures/launcher.env.example')
    Assert-Launcher ($env:COMMUNITY_LAUNCHER_TEST_VALUE -eq 'inherited') 'Inherited environment was overwritten.'
    Assert-Launcher ($env:COMMUNITY_LAUNCHER_TEST_LITERAL -eq '$(1+1)') 'Environment text was executed instead of read literally.'
    [Environment]::SetEnvironmentVariable('COMMUNITY_LAUNCHER_TEST_VALUE', $null, 'Process')
    Import-CommunityEnv (Join-Path $PSScriptRoot 'fixtures/launcher.env.example')
    Assert-Launcher ($env:COMMUNITY_LAUNCHER_TEST_VALUE -eq 'value with spaces') 'Quoted environment value was parsed incorrectly.'

    $node = (Get-Command node.exe).Source
    $prefix = Join-Path $temporary 'process with spaces'
    $child = Start-CommunityProcess $node @('-p', 'process.versions.node') $temporary $prefix
    $child.WaitForExit()
    Assert-Launcher ($child.ExitCode -eq 0) "Background process launch failed: $($child.ExitCode)."
    Assert-Launcher ((Get-Content -LiteralPath "$prefix.out.log" -Raw) -match '^\d+\.') 'Background stdout was not captured.'
    $failed = $false
    try { Wait-CommunityService 'Fixture' 'http://127.0.0.1' { $false } $child 1 $prefix } catch { $failed = $true }
    Assert-Launcher $failed 'Early process exit was not reported.'
    $frontendDirectory = Join-Path (Split-Path $PSScriptRoot -Parent) 'smart-community-frontend'
    $vite = Join-Path $frontendDirectory 'node_modules/vite/bin/vite.js'
    $prefix = Join-Path $temporary 'vite with spaces'
    $frontend = Start-CommunityProcess $node @($vite, '--host', '127.0.0.1', '--port', "$port", '--strictPort') $frontendDirectory $prefix
    Wait-CommunityService 'Test frontend' "http://127.0.0.1:$port" ${function:Test-CommunityFrontend} $frontend 45 $prefix
    Assert-Launcher (Test-CommunityFrontend "http://127.0.0.1:$port") 'Real Vite deep link did not load.'
    if (Test-CommunityBackend 'http://127.0.0.1:8001') {
        Assert-Launcher (Test-CommunityBackend "http://127.0.0.1:$port") 'Real Vite API proxy failed.'
    }
    Write-Host 'PASS: JDK, TCP, HTTP failures, stale JAR, literal .env, background logs, early exit and real Vite startup/deep link/proxy.' -ForegroundColor Green
} finally {
    if ($listener) { $listener.Stop() }
    if ($child -and -not $child.HasExited) { $child.Kill(); $child.WaitForExit() }
    if ($frontend -and -not $frontend.HasExited) { $frontend.Kill(); $frontend.WaitForExit() }
    [Environment]::SetEnvironmentVariable('COMMUNITY_LAUNCHER_TEST_VALUE', $previousValue, 'Process')
    [Environment]::SetEnvironmentVariable('COMMUNITY_LAUNCHER_TEST_LITERAL', $previousLiteral, 'Process')
    # Only remove the exact per-test temporary directory created above.
    if ((Split-Path $temporary -Parent) -eq [IO.Path]::GetTempPath().TrimEnd('\')) {
        Remove-Item -LiteralPath $temporary -Recurse -Force
    }
}
