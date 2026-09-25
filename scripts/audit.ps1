# Forward CLI arguments unchanged, including paths containing spaces.
$ErrorActionPreference = 'Stop'
$java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { 'java' }
$jar = if ($env:AUDIT_JAR) { $env:AUDIT_JAR } else { Join-Path $PSScriptRoot '../target/gerador-docs-tests-3.0.0.jar' }
if (!(Test-Path -LiteralPath $jar)) { throw 'JAR ausente. Execute scripts/setup.ps1 ou extraia a distribuicao completa.' }
& $java -jar $jar @args
exit $LASTEXITCODE
