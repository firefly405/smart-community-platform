param(
    [Parameter(Mandatory = $true)]
    [int]$ProcessId,
    [Parameter(Mandatory = $true)]
    [string]$JavaExecutable,
    [Parameter(Mandatory = $true)]
    [string]$JarPath,
    [Parameter(Mandatory = $true)]
    [string]$WorkingDirectory
)

$ErrorActionPreference = 'Stop'

$deadline = [DateTime]::UtcNow.AddSeconds(30)
while ((Get-Process -Id $ProcessId -ErrorAction SilentlyContinue) -and [DateTime]::UtcNow -lt $deadline) {
    Start-Sleep -Milliseconds 250
}
if (Get-Process -Id $ProcessId -ErrorAction SilentlyContinue) {
    Write-Error "The previous backend process did not stop within 30 seconds."
    exit 1
}

$logDirectory = Split-Path -Parent $JarPath
$stdout = Join-Path $logDirectory 'backend-runtime.out.log'
$stderr = Join-Path $logDirectory 'backend-runtime.err.log'
$quotedJar = '"' + $JarPath + '"'

Start-Process `
    -FilePath $JavaExecutable `
    -ArgumentList @('--enable-preview', '-jar', $quotedJar) `
    -WorkingDirectory $WorkingDirectory `
    -WindowStyle Hidden `
    -RedirectStandardOutput $stdout `
    -RedirectStandardError $stderr
