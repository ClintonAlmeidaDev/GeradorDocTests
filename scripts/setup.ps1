param(
    [ValidateSet('bruno', 'postman', 'both')][string]$Runner = 'bruno',
    [switch]$SkipBuild,
    [string]$SettingsPath
)
$ErrorActionPreference = 'Stop'
$java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { 'java' }
$javaVersion = & $java --version
if ($LASTEXITCODE -ne 0 -or ($javaVersion -join ' ') -notmatch '(?:openjdk|java) (\d+)' -or [int]$Matches[1] -lt 25) {
    throw 'Instale JDK 25 ou superior e configure JAVA_HOME antes do setup.'
}
Write-Host ($javaVersion -join "`n")
$nodeVersion = & node --version
if ($LASTEXITCODE -ne 0 -or $nodeVersion -notmatch '^v(\d+)\.' -or [int]$Matches[1] -lt 22) {
    throw 'Instale Node 22 ou superior e reabra o terminal.'
}
Write-Host $nodeVersion
$npm = (Get-Command npm.cmd -ErrorAction Stop).Source
$project = Split-Path $PSScriptRoot -Parent
$jar = if ($env:AUDIT_JAR) { $env:AUDIT_JAR } else { Join-Path $project 'target/gerador-docs-tests-3.0.0.jar' }
if (!$SkipBuild) {
    $mavenArgs = @('-f', (Join-Path $project 'pom.xml'))
    if ($SettingsPath) { $mavenArgs += @('-s', (Resolve-Path -LiteralPath $SettingsPath).Path) }
    & mvn.cmd @mavenArgs clean package
    if ($LASTEXITCODE -ne 0) { throw 'Build Maven falhou. Confira JDK 25 e mirror corporativo em docs/WINDOWS.md.' }
}
if (!(Test-Path -LiteralPath $jar)) { throw 'JAR ausente. Use o ZIP completo ou execute sem -SkipBuild.' }
$required = @{}
if ($Runner -in @('bruno', 'both')) { $required['@usebruno/cli'] = '4.0.0' }
if ($Runner -in @('postman', 'both')) { $required['newman'] = '6.2.2' }
$installed = $null
try {
    $listing = & $npm list -g --depth=0 --json 2>$null
    if ($LASTEXITCODE -eq 0) { $installed = ($listing -join "`n" | ConvertFrom-Json).dependencies }
} catch { $installed = $null }
$packages = @()
foreach ($package in $required.Keys) {
    if (!$installed -or !$installed.$package -or $installed.$package.version -ne $required[$package]) {
        $packages += ($package + '@' + $required[$package])
    }
}
if ($packages.Count -gt 0) {
    & $npm install -g @packages
    if ($LASTEXITCODE -ne 0) { throw 'Instalacao npm falhou. Confira registry, proxy, CA e permissoes de subprocessos.' }
} else {
    Write-Host 'Versoes npm requeridas ja instaladas; verificando funcionamento no diagnostico.'
}
& $java -cp $jar com.microsoft.playwright.CLI install chromium
if ($LASTEXITCODE -ne 0) { throw 'Instalacao Chromium falhou. Confira proxy, CA e PLAYWRIGHT_DOWNLOAD_HOST.' }
$runners = if ($Runner -eq 'both') { @('bruno', 'postman') } else { @($Runner) }
foreach ($selected in $runners) {
    & $java -jar $jar --doctor "--runner=$selected"
    if ($LASTEXITCODE -ne 0) { throw "Diagnostico de $selected falhou. Veja a mensagem acima." }
}
Write-Host 'Instalacao verificada. Use scripts/audit.ps1 para gerar evidencias.'
