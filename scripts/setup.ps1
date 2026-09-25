param(
    [ValidateSet('bruno', 'postman', 'both')][string]$Runner = 'bruno',
    [switch]$SkipBuild,
    [string]$SettingsPath
)
$ErrorActionPreference = 'Stop'
$java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { 'java' }
& $java -version
if ($LASTEXITCODE -ne 0) { throw 'Instale JDK 25 e configure JAVA_HOME.' }
& node --version
if ($LASTEXITCODE -ne 0) { throw 'Instale Node 22 e reabra o terminal.' }
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
$packages = @()
if ($Runner -in @('bruno', 'both')) { $packages += '@usebruno/cli@4.0.0' }
if ($Runner -in @('postman', 'both')) { $packages += 'newman@6.2.2' }
& $npm install -g @packages
if ($LASTEXITCODE -ne 0) { throw 'Instalacao npm falhou. Confira registry, proxy e CA corporativos.' }
& $java -cp $jar com.microsoft.playwright.CLI install chromium
if ($LASTEXITCODE -ne 0) { throw 'Instalacao Chromium falhou. Confira proxy, CA e PLAYWRIGHT_DOWNLOAD_HOST.' }
$runners = if ($Runner -eq 'both') { @('bruno', 'postman') } else { @($Runner) }
foreach ($selected in $runners) {
    & $java -jar $jar --doctor "--runner=$selected"
    if ($LASTEXITCODE -ne 0) { throw "Diagnostico de $selected falhou. Veja a mensagem acima." }
}
Write-Host 'Instalacao verificada. Use scripts/audit.ps1 para gerar evidencias.'
