$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'Start-Community.ps1')
function Assert-Environment {
    param([bool]$Condition, [string]$Message)
    if (-not $Condition) { throw $Message }
}
$root = Split-Path $PSScriptRoot -Parent
$temporary = Join-Path ([IO.Path]::GetTempPath()) ('community-env-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $temporary | Out-Null
try {
    $checks = @(Get-CommunityEnvironment $root)
    Assert-Environment ($checks.Count -ge 18) 'Not all environment checks were collected.'
    Assert-Environment (@($checks | Where-Object { $_.Name -in @('Visual Studio Code','IntelliJ IDEA','Git') -and $_.Status -eq 'BLOCK' }).Count -eq 0) 'An optional editor blocked runtime.'
    $fixtureChecks = & {
        function Find-CommunityJava { throw 'Missing Java fixture' }
        function Find-CommunityProgram { return $null }
        function Test-CommunityTcp { return $false }
        function Test-CommunityBackend { return $false }
        function Test-CommunityFrontend { return $false }
        Get-CommunityEnvironment $temporary
    }
    foreach ($name in @('JDK 17','Node.js 18+','Project source/configuration','Frontend dependencies / native module','Maven 3.9+')) {
        Assert-Environment (@($fixtureChecks | Where-Object { $_.Name -eq $name -and $_.Status -eq 'BLOCK' }).Count -eq 1) "Missing prerequisite was not collected: $name"
    }
    Assert-Environment (@($fixtureChecks | Where-Object { $_.Name -eq 'Visual Studio Code' -and $_.Status -eq 'WARN' }).Count -eq 1) 'Missing VS Code was not a recommendation.'
    $reportChecks = @($fixtureChecks)
    $reportChecks[0].Detail = '<script>alert("unsafe")</script>'
    $report = Write-CommunityEnvironmentReport $reportChecks $temporary
    $html = Get-Content -LiteralPath $report -Raw -Encoding UTF8
    Assert-Environment ($html -notmatch '<script>') 'Report output was not HTML-escaped.'
    Assert-Environment ($html -notmatch '\{\{\w+\}\}') 'Report contains unresolved template variables.'
    Assert-Environment ($html -match 'VS Code') 'Missing editor is absent from report.'
    Assert-Environment ($html -match 'Maven') 'Missing build tool is absent from report.'
    Write-Host 'PASS: complete detection, optional editors, multiple missing prerequisites and safe report rendering.' -ForegroundColor Green
} finally {
    if ((Split-Path $temporary -Parent) -eq [IO.Path]::GetTempPath().TrimEnd('\')) { Remove-Item -LiteralPath $temporary -Recurse -Force }
}
