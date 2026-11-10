param([string]$OutputDirectory = [Environment]::GetFolderPath('Desktop'))

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$root = (Resolve-Path -LiteralPath (Split-Path $PSScriptRoot -Parent)).Path
$output = (Resolve-Path -LiteralPath $OutputDirectory).Path
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$zipPath = Join-Path $output "SmartCommunity-Full-$stamp.zip"
$prefix = 'SmartCommunity/'
$utf8 = New-Object Text.UTF8Encoding($false)

function Get-PackageFiles {
    param([IO.DirectoryInfo]$Directory)
    foreach ($item in Get-ChildItem -LiteralPath $Directory.FullName -Force) {
        if ($item.Attributes -band [IO.FileAttributes]::ReparsePoint) { continue }
        if ($item.PSIsContainer) {
            if ($item.Name -in @('.git','.idea','.vscode','.cursor','.runtime','.cache','.vite',
                'test-results','playwright-report','screenshots','coverage','.uploads')) { continue }
            Get-PackageFiles $item
        } else {
            $relative = $item.FullName.Substring($root.Length + 1).Replace('\','/')
            if ($item.Name -match '^\.env' -and $item.Name -ne '.env.example') { continue }
            if ($item.Extension -in @('.log','.lnk','.iml','.pem','.key','.pfx','.p12','.jks','.keystore','.local')) { continue }
            if ($relative -match '/target/' -and $relative -ne 'smart-community-backend/smart-community-admin/target/smart-community-admin-1.0.0-SNAPSHOT.jar') { continue }
            $item
        }
    }
}

function Get-StreamHash {
    param([IO.Stream]$Stream)
    $hasher = [Security.Cryptography.SHA256]::Create()
    try { return [BitConverter]::ToString($hasher.ComputeHash($Stream)).Replace('-','').ToLowerInvariant() }
    finally { $hasher.Dispose() }
}

function Add-PackageText {
    param([IO.Compression.ZipArchive]$Archive, [string]$Name, [string]$Text)
    $entry = $Archive.CreateEntry($Name, [IO.Compression.CompressionLevel]::Optimal)
    $stream = $entry.Open()
    try {
        $bytes = $utf8.GetBytes($Text)
        $stream.Write($bytes, 0, $bytes.Length)
    } finally { $stream.Dispose() }
}

$files = @(
    foreach ($directory in @('docs','scripts','smart-community-backend','smart-community-frontend','e2e-tests')) {
        Get-PackageFiles (Get-Item -LiteralPath (Join-Path $root $directory))
    }
    Get-ChildItem -LiteralPath $root -File -Force | Where-Object {
        $_.Name -in @('.gitignore','Start-Community.cmd','Check-Environment.cmd','README.md','功能结构图.html','代码量统计报告.md')
    }
) | Sort-Object FullName

$relativePaths = @($files | ForEach-Object { $_.FullName.Substring($root.Length + 1).Replace('\','/') })
foreach ($required in @('Start-Community.cmd','scripts/Start-Community.ps1',
    'docs/智慧社区综合管理平台_PRD_V2.0_现状重编版.md',
    'smart-community-backend/smart-community-admin/target/smart-community-admin-1.0.0-SNAPSHOT.jar',
    'smart-community-backend/sql/smart_community_init.sql',
    'smart-community-frontend/dist/index.html','smart-community-frontend/node_modules/vite/bin/vite.js')) {
    if ($required -notin $relativePaths) { throw "Required package file is missing: $required" }
}

$note = @'
# 智慧社区完整项目包

此包包含打包时的本地工作区版本（含未提交源码），不是仅导出 Git 已提交版本。

## 包含内容

- 管理端、智慧社区业主端和租户端的前端源码、前端 dist 构建文件及 node_modules。
- Java 后端源码、Maven 配置及可运行 JAR。
- 当前 PRD V2.0、历史 PRD、其他项目文档和功能结构图。
- Start-Community.cmd、一键启动脚本、自检脚本、打包脚本和使用说明。
- 数据库初始化/增量补丁/演示数据 SQL、示例图片、素材许可与来源清单。
- 端到端测试源码与现有依赖；不包含浏览器安装包及测试运行产物。

## 使用方法

1. 先完整解压，不要直接在压缩包里双击运行。
2. 本机已有环境和数据库：双击 SmartCommunity/Start-Community.cmd。
3. 新电脑：先安装 JDK 17、Node.js 18+、MySQL 8.0；源码需编译时安装 Maven 3.9+。
4. 新数据库参照 README 和 sql 脚本初始化。初始化脚本可能重建表，禁止对需要保留的已有业务库盲目执行。
5. 配置数据库连接。后端 .env.example 是示例，不含真实 AI Key；需要 AI 时在系统管理中重新配置。
6. 详见 docs/一键启动说明.md。管理端 /login，业主端 /community/login，租户端 /tenant/login，前端默认 5174，后端 8001。

## 边界与安全

- 未打包当前 MySQL 实例的数据快照，因此不是数据库全量备份；包含的是项目现有 SQL 和种子数据。
- 未包含机器级 Java、Node、MySQL、Maven 安装目录；这不是无需环境的独立 EXE。
- node_modules 是本机 Windows 环境依赖，换系统/架构时应执行 npm ci 重新安装。
- 排除真实 .env、.runtime（包含 AI Key）、Git 历史、IDE 设置、日志、截图、私钥及桌面绝对路径快捷方式。
- 本项目仍有演示密码、演示数据和模拟支付，正式使用前必须修改默认凭据并完成安全加固。
- 前后端构建文件为当前已有构建产物，本次打包不重新编译，也不声明全功能验收通过。
- PACKAGE-MANIFEST.json 记录每个打包文件的相对路径、长度及 SHA-256；生成后已逐文件从 ZIP 读取校验。
'@

$manifest = New-Object 'System.Collections.Generic.List[object]'
$fileStream = [IO.File]::Open($zipPath, [IO.FileMode]::CreateNew, [IO.FileAccess]::ReadWrite, [IO.FileShare]::None)
try {
    $archive = New-Object IO.Compression.ZipArchive($fileStream, [IO.Compression.ZipArchiveMode]::Create, $true, $utf8)
    try {
        $index = 0
        foreach ($file in $files) {
            $relative = $file.FullName.Substring($root.Length + 1).Replace('\','/')
            $entry = $archive.CreateEntry($prefix + $relative, [IO.Compression.CompressionLevel]::Optimal)
            $source = [IO.File]::Open($file.FullName, [IO.FileMode]::Open, [IO.FileAccess]::Read, [IO.FileShare]::Read)
            try {
                $hash = Get-StreamHash $source
                $length = $source.Length
                $source.Position = 0
                $destination = $entry.Open()
                try { $source.CopyTo($destination) } finally { $destination.Dispose() }
                $manifest.Add([pscustomobject]@{path=$relative; bytes=$length; sha256=$hash})
            } finally { $source.Dispose() }
            $index++
            if ($index % 2000 -eq 0) { Write-Host "Packed $index / $($files.Count) files" }
        }
        Add-PackageText $archive ($prefix + 'PACKAGE-README.md') $note
        Add-PackageText $archive ($prefix + 'PACKAGE-MANIFEST.json') (ConvertTo-Json -InputObject @($manifest.ToArray()) -Depth 3)
    } finally { $archive.Dispose() }
} finally { $fileStream.Dispose() }

Write-Host 'Verifying every ZIP entry against its SHA-256...'
$archive = [IO.Compression.ZipFile]::OpenRead($zipPath)
try {
    if ($archive.Entries.Count -ne $manifest.Count + 2) { throw 'Archive entry count mismatch.' }
    foreach ($record in $manifest) {
        $entry = $archive.GetEntry($prefix + $record.path)
        if (-not $entry -or $entry.Length -ne $record.bytes) { throw "Length mismatch: $($record.path)" }
        $stream = $entry.Open()
        try {
            if ((Get-StreamHash $stream) -ne $record.sha256) { throw "SHA-256 mismatch: $($record.path)" }
        } finally { $stream.Dispose() }
    }
} finally { $archive.Dispose() }

$result = Get-Item -LiteralPath $zipPath
[pscustomobject]@{Archive=$result.FullName; Files=$manifest.Count + 2; SizeMB=[math]::Round($result.Length/1MB,2);
    SHA256=(Get-FileHash -LiteralPath $zipPath -Algorithm SHA256).Hash; Verified=$true} | ConvertTo-Json
