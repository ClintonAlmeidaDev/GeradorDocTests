# Windows PowerShell 5.1 and PowerShell 7: preserve literal native arguments.
$ErrorActionPreference = 'Stop'
try {
    $java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { (Get-Command java.exe -ErrorAction Stop).Source }
    $jar = if ($env:AUDIT_JAR) { $env:AUDIT_JAR } else { Join-Path $PSScriptRoot '../target/gerador-docs-tests-3.0.0.jar' }
    if (!(Test-Path -LiteralPath $jar -PathType Leaf)) {
        Write-Host 'JAR ausente. Execute scripts/setup.ps1 ou extraia a distribuicao completa.'
        exit 2
    }
    # ProcessStartInfo avoids PowerShell 5.1 dropping empty arguments and embedded quotes.
    # Escape according to the Windows native argv convention; never invoke a shell.
    $encoded = foreach ($value in (@('-jar', $jar) + $args)) {
        $escaped = [regex]::Replace([string]$value, '(\\*)"', '$1$1\"')
        '"' + [regex]::Replace($escaped, '(\\+)$', '$1$1') + '"'
    }
    $start = New-Object System.Diagnostics.ProcessStartInfo
    $start.FileName = $java
    $start.Arguments = $encoded -join ' '
    $start.UseShellExecute = $false
    # Set-Location does not change the .NET process current directory on Windows.
    $start.WorkingDirectory = (Get-Location).ProviderPath
    $process = [System.Diagnostics.Process]::Start($start)
    try {
        $process.WaitForExit()
        $status = $process.ExitCode
    } finally {
        $process.Dispose()
    }
    if ($status -notin @(0, 1, 2)) { $status = 2 }
    exit $status
} catch {
    Write-Host 'Falha ao iniciar Java. Confira JAVA_HOME, JDK 25, caminho do JAR e permissoes.'
    exit 2
}
