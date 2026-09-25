# Publish only evidence files, preserving functional failure (exit 1).
$ErrorActionPreference = 'Stop'
$status = 2
try {
    $runDir = Join-Path (Get-Location) ('audit-output/ci-' + [guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Force $runDir, 'audit-artifacts' | Out-Null
    & (Join-Path $PSScriptRoot 'audit.ps1') @args --output-dir $runDir
    $status = $LASTEXITCODE
    Get-ChildItem -LiteralPath $runDir -File | Where-Object {
        $_.Name.EndsWith('.pdf') -or $_.Name.EndsWith('.audit-run.json')
    } | Copy-Item -Destination 'audit-artifacts'
} catch {
    Write-Error $_ -ErrorAction Continue
    $status = 2
}
New-Item -ItemType Directory -Force 'audit-artifacts' | Out-Null
Set-Content -Path 'audit-artifacts/exit-code.txt' -Value $status -Encoding ascii
exit $status
