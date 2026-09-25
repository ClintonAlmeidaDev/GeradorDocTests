# Publish only evidence files, preserving functional failure (exit 1).
$ErrorActionPreference = 'Stop'
$status = 2
try {
    $runDir = Join-Path (Get-Location) ('audit-output/ci-' + [guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Force $runDir, 'audit-artifacts' | Out-Null
    # Managed options must precede the runner's -- / --tool-args boundary.
    $auditArgs = @()
    $inserted = $false
    foreach ($arg in $args) {
        if (!$inserted -and $arg -in @('--', '--tool-args')) {
            $auditArgs += @('--output-dir', $runDir)
            $inserted = $true
        }
        $auditArgs += $arg
    }
    if (!$inserted) { $auditArgs += @('--output-dir', $runDir) }
    & (Join-Path $PSScriptRoot 'audit.ps1') @auditArgs
    $status = $LASTEXITCODE
    Get-ChildItem -LiteralPath $runDir -File | Where-Object {
        $_.Name.EndsWith('.pdf') -or $_.Name.EndsWith('.audit-run.json')
    } | Copy-Item -Destination 'audit-artifacts'
} catch {
    Write-Host 'Falha tecnica na execucao ou publicacao. Confira Java e permissoes dos diretorios de saida.'
    $status = 2
}
New-Item -ItemType Directory -Force 'audit-artifacts' | Out-Null
Set-Content -Path 'audit-artifacts/exit-code.txt' -Value $status -Encoding ascii
exit $status
