function Find-CommunityProgram {
    param([string[]]$Commands, [string[]]$Paths, [string]$ProductPattern)
    foreach ($name in $Commands) {
        $command = Get-Command $name -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($command) { return $command.Source }
    }
    foreach ($path in $Paths) {
        if ($path) {
            $item = Get-Item -Path $path -ErrorAction SilentlyContinue | Where-Object { -not $_.PSIsContainer } | Select-Object -First 1
            if ($item) { return $item.FullName }
        }
    }
    if ($ProductPattern) {
        foreach ($key in @('HKCU:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*',
            'HKLM:\Software\Microsoft\Windows\CurrentVersion\Uninstall\*',
            'HKLM:\Software\WOW6432Node\Microsoft\Windows\CurrentVersion\Uninstall\*')) {
            foreach ($app in Get-ItemProperty -Path $key -ErrorAction SilentlyContinue | Where-Object { $_.DisplayName -match $ProductPattern }) {
                $icon = ($app.DisplayIcon -replace ',\s*\d+$', '').Trim('"')
                if ($icon -and (Test-Path -LiteralPath $icon -PathType Leaf) -and [IO.Path]::GetExtension($icon) -eq '.exe') { return $icon }
                if ($app.InstallLocation) {
                    foreach ($name in $Commands) {
                        foreach ($relative in @($name, "bin/$name")) {
                            $candidate = Join-Path $app.InstallLocation $relative
                            if (Test-Path -LiteralPath $candidate -PathType Leaf) { return $candidate }
                        }
                    }
                }
            }
        }
    }
    return $null
}

function Get-CommunityEnvironment {
    param([string]$Root, [switch]$Rebuild)
    $items = New-Object 'System.Collections.Generic.List[object]'
    function Add-Check {
        param([string]$Name, [string]$Category, [string]$Status, [string]$Detail, [string]$Action, [string]$Executable)
        $items.Add([pscustomobject]@{Name=$Name; Category=$Category; Status=$Status; Detail=$Detail; Action=$Action; Executable=$Executable})
    }
    $java = $null
    try { $java = Find-CommunityJava; Add-Check 'JDK 17' 'Runtime' 'OK' $java '' }
    catch { Add-Check 'JDK 17' 'Runtime' 'BLOCK' 'Not found or incompatible.' 'Install JDK 17; set JAVA_HOME. Other Java major versions do not replace Java 17 preview support.' }
    $node = Find-CommunityProgram @('node.exe') @("$env:ProgramFiles/nodejs/node.exe", "$env:LOCALAPPDATA/Programs/nodejs/node.exe", "$env:NVM_SYMLINK/node.exe")
    $nodeOk = $false
    if ($node) {
        try {
            $version = & $node -p 'process.versions.node' 2>$null
            $nodeOk = $LASTEXITCODE -eq 0 -and $version -match '^\d+\.' -and [int]($version.Split('.')[0]) -ge 18
        } catch { $version = 'Cannot execute Node.js' }
    } else { $version = 'Not found in PATH or standard installation locations.' }
    Add-Check 'Node.js 18+' 'Runtime' $(if ($nodeOk) { 'OK' } else { 'BLOCK' }) "$version $node" 'Install Node.js 18+ (Node 20 is the tested baseline); reopen the launcher.' $node
    $npm = Find-CommunityProgram @('npm.cmd') @($(if ($node) { Join-Path (Split-Path $node -Parent) 'npm.cmd' }))
    Add-Check 'npm' 'Development' $(if ($npm) { 'OK' } else { 'WARN' }) "$npm" 'Install npm with Node.js; it is needed for npm ci/build, not for existing Vite dependencies.'
    Add-Check 'Windows PowerShell' 'Runtime' $(if ($PSVersionTable.PSVersion.Major -ge 5) { 'OK' } else { 'BLOCK' }) ($PSVersionTable.PSVersion.ToString()) 'Use Windows PowerShell 5.1 or later.'

    $frontend = Join-Path $Root 'smart-community-frontend'
    $backend = Join-Path $Root 'smart-community-backend'
    $jar = Join-Path $backend 'smart-community-admin/target/smart-community-admin-1.0.0-SNAPSHOT.jar'
    $sourcesOk = (Test-Path -LiteralPath (Join-Path $backend 'smart-community-parent/pom.xml')) -and
        (Test-Path -LiteralPath (Join-Path $frontend 'package.json')) -and
        (Test-Path -LiteralPath (Join-Path $frontend 'src/router/index.ts')) -and
        (Test-Path -LiteralPath (Join-Path $backend 'smart-community-admin/src/main/resources/application.yml'))
    Add-Check 'Project source/configuration' 'Runtime' $(if ($sourcesOk) { 'OK' } else { 'BLOCK' }) $Root 'Extract the entire ZIP; preserve module directories. Do not run inside the ZIP preview.'
    $viteOk = $false
    if ($nodeOk -and (Test-Path -LiteralPath (Join-Path $frontend 'node_modules/vite/bin/vite.js'))) {
        Push-Location $frontend
        try {
            $null = & $node '-e' "require('esbuild').transformSync('let x=1');require.resolve('vite');require.resolve('vue');require.resolve('element-plus')" 2>$null
            $viteOk = $LASTEXITCODE -eq 0
        } catch {} finally { Pop-Location }
    }
    Add-Check 'Frontend dependencies / native module' 'Runtime' $(if ($viteOk) { 'OK' } else { 'BLOCK' }) 'Vite, Vue, Element Plus and an esbuild native transform.' 'Run npm ci in smart-community-frontend; dependencies may not match this OS/architecture.'
    $backendReady = Test-CommunityBackend 'http://127.0.0.1:8001'
    $needsBuild = $true
    if ($sourcesOk) { $needsBuild = $Rebuild -or (Test-CommunityBuildRequired $backend $jar) }
    Add-Check 'Backend JAR' 'Runtime' $(if (Test-Path -LiteralPath $jar) { 'OK' } else { 'WARN' }) $(if ($needsBuild) { 'Missing or source is newer; a build may be needed.' } else { 'Packaged JAR exists and is current by file timestamps.' }) 'The launcher builds when needed; Maven and network access for uncached dependencies are then required.'
    $maven = Find-CommunityProgram @('mvn.cmd') @($(if ($env:MAVEN_HOME) { Join-Path $env:MAVEN_HOME 'bin/mvn.cmd' }), $(if ($env:M2_HOME) { Join-Path $env:M2_HOME 'bin/mvn.cmd' }))
    $mavenOk = $false
    $mavenVersion = 'Not found or cannot execute with Java 17.'
    if ($maven -and $java) {
        $previousJava = $env:JAVA_HOME
        try {
            $env:JAVA_HOME = Split-Path (Split-Path $java -Parent) -Parent
            $versionText = (& $maven '--version' 2>$null) -join ' '
            if ($LASTEXITCODE -eq 0 -and $versionText -match 'Apache Maven (\d+\.\d+\.\d+)') {
                $mavenVersion = $Matches[1]; $mavenOk = [version]$mavenVersion -ge [version]'3.9.0'
            }
        } catch {} finally { $env:JAVA_HOME = $previousJava }
    }
    $mavenStatus = if ($mavenOk) { 'OK' } elseif ($needsBuild -and -not $backendReady) { 'BLOCK' } else { 'WARN' }
    Add-Check 'Maven 3.9+' 'Development' $mavenStatus "$mavenVersion $maven" 'Install Maven 3.9+ and set PATH or MAVEN_HOME. Required when the backend needs rebuilding.'

    $mysqlHost = if ($env:MYSQL_HOST) { $env:MYSQL_HOST } else { 'localhost' }
    $mysqlPort = 3306
    $validPort = -not $env:MYSQL_PORT -or ([int]::TryParse($env:MYSQL_PORT, [ref]$mysqlPort) -and $mysqlPort -ge 1 -and $mysqlPort -le 65535)
    $mysqlReady = $validPort -and (Test-CommunityTcp $mysqlHost $mysqlPort)
    $service = Get-Service -Name 'MySQL80' -ErrorAction SilentlyContinue
    $recoverable = $validPort -and $mysqlHost -in @('localhost','127.0.0.1','::1') -and $mysqlPort -eq 3306 -and $service -and $service.Status -ne 'Running'
    $dbStatus = if ($mysqlReady) { 'OK' } elseif ($recoverable) { 'WARN' } else { 'BLOCK' }
    Add-Check 'MySQL connection / service' 'Runtime' $dbStatus "${mysqlHost}:$mysqlPort; MySQL80: $($service.Status); TCP reachable: $mysqlReady" 'Install/start MySQL 8.0 or configure MYSQL_HOST/MYSQL_PORT. A stopped MySQL80 can be started by the launcher if permissions permit.'
    Add-Check 'Database account / schema' 'Runtime' 'VERIFY' 'TCP is not database authentication. This preflight does not validate credentials, schema completeness or MySQL server version.' 'Confirm the existing database and account; verify startup and business queries. Never reimport a valuable database just to pass this check.'
    $mysql = Find-CommunityProgram @('mysql.exe') @("$env:ProgramFiles/MySQL/MySQL Server 8.0/bin/mysql.exe")
    Add-Check 'MySQL command-line client' 'Optional' $(if ($mysql) { 'OK' } else { 'WARN' }) "$mysql" 'Useful for SQL import and diagnosis. The Java backend can connect without this client.'

    $code = Find-CommunityProgram @('Code.exe','code.cmd') @("$env:LOCALAPPDATA/Programs/Microsoft VS Code/Code.exe", "$env:ProgramFiles/Microsoft VS Code/Code.exe", "${env:ProgramFiles(x86)}/Microsoft VS Code/Code.exe") 'Microsoft Visual Studio Code'
    $idea = Find-CommunityProgram @('idea64.exe','idea.exe') @("$env:ProgramFiles/JetBrains/IntelliJ IDEA*/bin/idea64.exe", "$env:LOCALAPPDATA/Programs/IntelliJ IDEA*/bin/idea64.exe", "$env:LOCALAPPDATA/JetBrains/Toolbox/apps/*/*/bin/idea64.exe", "$env:LOCALAPPDATA/JetBrains/Toolbox/apps/*/*/*/bin/idea64.exe") 'IntelliJ IDEA'
    $git = Find-CommunityProgram @('git.exe') @("$env:ProgramFiles/Git/cmd/git.exe")
    Add-Check 'Visual Studio Code' 'Development' $(if ($code) { 'OK' } else { 'WARN' }) "$code" 'Install VS Code to view/edit frontend source, or use another editor. https://code.visualstudio.com/'
    Add-Check 'IntelliJ IDEA' 'Development' $(if ($idea) { 'OK' } else { 'WARN' }) "$idea" 'Install IDEA Community/Ultimate to import the parent pom.xml, or use another Java IDE. https://www.jetbrains.com/idea/'
    Add-Check 'Git' 'Development' $(if ($git) { 'OK' } else { 'WARN' }) "$git" 'Optional for an extracted ZIP; recommended for source version control. https://git-scm.com/'
    $browser = Find-CommunityProgram @('msedge.exe','chrome.exe','firefox.exe') @("${env:ProgramFiles(x86)}/Microsoft/Edge/Application/msedge.exe", "$env:ProgramFiles/Microsoft/Edge/Application/msedge.exe", "$env:ProgramFiles/Google/Chrome/Application/chrome.exe", "$env:LOCALAPPDATA/Google/Chrome/Application/chrome.exe", "$env:ProgramFiles/Mozilla Firefox/firefox.exe")
    Add-Check 'Web browser' 'Runtime' $(if ($browser) { 'OK' } else { 'WARN' }) "$browser" 'Install Edge/Chrome/Firefox or use an existing browser; custom/portable locations may not be detected.'
    foreach ($port in @(8001,5174)) {
        $occupied = Test-CommunityTcp '127.0.0.1' $port
        $healthy = if ($port -eq 8001) { $backendReady } else { Test-CommunityFrontend 'http://127.0.0.1:5174' }
        Add-Check "Port $port" 'Runtime' $(if ($occupied -and -not $healthy) { 'BLOCK' } else { 'OK' }) $(if ($healthy) { 'Healthy service can be reused.' } elseif ($occupied) { 'Occupied by an unknown or unhealthy service.' } else { 'Available for startup.' }) 'Do not kill unknown processes; identify the owner or inspect existing service logs.'
    }
    Add-Check 'AI / Redis / IDE plugins' 'Optional' 'VERIFY' 'AI is optional; Redis is not required by the current configuration. IDE plugins and IDE project SDK settings are not inspected.' 'Configure a valid AI API only when needed; in IDEA select JDK 17 and --enable-preview. In VS Code, Vue - Official is recommended.'
    return $items.ToArray()
}

function Write-CommunityEnvironmentReport {
    param([object[]]$Items, [string]$Root)
    $directory = Join-Path $Root '.runtime/launcher'
    New-Item -ItemType Directory -Path $directory -Force | Out-Null
    $path = Join-Path $directory 'environment-report.html'
    $template = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'environment-report.template.html') -Raw -Encoding UTF8
    $labels = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'environment-report.zh-CN.json') -Raw -Encoding UTF8 | ConvertFrom-Json
    $display = @($Items | ForEach-Object {
        $label = $labels.PSObject.Properties[$_.Name].Value
        [pscustomobject][ordered]@{Name=$label.name;Category=$_.Category;Status=$_.Status;Detail=$_.Detail;Action=$label.action}
    })
    $rows = ($display | ConvertTo-Html -Fragment) -join "`n"
    $attention = ($display | Where-Object Status -in @('BLOCK','WARN') | ConvertTo-Html -Fragment) -join "`n"
    $blocks = @($Items | Where-Object Status -eq 'BLOCK').Count
    $warnings = @($Items | Where-Object Status -eq 'WARN').Count
    $html = $template.Replace('{{ROWS}}', $rows).Replace('{{ATTENTION}}', $attention).Replace('{{BLOCKS}}', "$blocks").Replace('{{WARNINGS}}', "$warnings").Replace('{{TIME}}', (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'))
    [IO.File]::WriteAllText($path, $html, (New-Object Text.UTF8Encoding($false)))
    Write-Host "`nEnvironment preflight: $blocks blocking issue(s), $warnings recommendation(s)."
    foreach ($item in $Items) {
        Write-Host "[$($item.Status)] $($item.Name): $($item.Detail)"
        if ($item.Status -ne 'OK') { Write-Host "  -> $($item.Action)" }
    }
    Write-Host "Full report: $path"
    return $path
}
