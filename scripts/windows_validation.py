#!/usr/bin/env python3
"""Native Windows launcher/PowerShell diagnostics regression; no external API calls.

Requires the packaged JAR, Node, Bruno/Newman and Playwright Chromium. All
generated launchers, copies and intentionally invalid inputs stay under output/.
"""
import argparse
import json
import os
from pathlib import Path
import shutil
import subprocess
import uuid
import csv

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--jar', default='target/gerador-docs-tests-3.0.0.jar')
parser.add_argument('--output', default='output/windows')
args = parser.parse_args()
if os.name != 'nt':
    raise SystemExit('This regression requires native Windows.')
repo = Path(__file__).resolve().parent.parent
jar = Path(args.jar).resolve()
root = Path(args.output).resolve() / ('Validação & 100% ' + uuid.uuid4().hex[:8])
root.mkdir(parents=True)
java = str(Path(os.environ['JAVA_HOME']) / 'bin/java.exe') if os.environ.get('JAVA_HOME') else shutil.which('java')
env = dict(os.environ)
for key in list(env):
    if key.upper() in ('BRU_EXECUTABLE', 'NEWMAN_EXECUTABLE', 'NODE_EXECUTABLE'):
        del env[key]
path_key = next(k for k in env if k.upper() == 'PATH')
node = Path(os.environ.get('NODE_EXECUTABLE') or shutil.which('node')).resolve()
results = []

def run(name, command, expected=0, extra=None, contains=None):
    child_env = dict(env)
    if extra:
        child_env.update(extra)
    result = subprocess.run(command, env=child_env, capture_output=True, timeout=120)
    text = (result.stdout + result.stderr).decode('utf-8', errors='replace')
    assert result.returncode == expected, (name, result.returncode, text)
    assert 'PRIVATE_FIXTURE_SECRET' not in text, name
    if contains:
        assert contains in text, (name, text)
    results.append({'scenario': name, 'exitCode': result.returncode})
    print(name, 'OK', flush=True)
    return result

doctor = [java, '-jar', str(jar), '--doctor', '--output-dir', str(root / 'doctor')]
for runner in ('bruno', 'postman'):
    run('PATH discovery ' + runner, doctor + ['--runner', runner])

# Copy complete real npm installations to a path containing every reported hazard.
# Hardlinks save space; no linked package files are modified by this regression.
roaming = root / 'Roaming'
prefix = roaming / 'npm'
prefix.mkdir(parents=True)
for tool, package in (('bru', '@usebruno/cli'), ('newman', 'newman')):
    original = Path(shutil.which(tool + '.cmd')).parent
    destination = prefix / 'node_modules'
    if not destination.exists():
        shutil.copytree(original / 'node_modules', destination, copy_function=os.link)
    for extension in ('.cmd', '.ps1'):
        shutil.copy2(original / (tool + extension), prefix / (tool + extension))
node_copy = root / 'Node á & 100%' / 'node.exe'
node_copy.parent.mkdir()
shutil.copy2(node, node_copy)
minimal_path = str(node_copy.parent) + ';' + os.environ['SystemRoot'] + '\\System32'
for runner, tool, variable in (('bruno', 'bru', 'BRU_EXECUTABLE'), ('postman', 'newman', 'NEWMAN_EXECUTABLE')):
    isolated = {path_key: minimal_path, 'APPDATA': str(roaming), 'NODE_EXECUTABLE': str(node_copy)}
    run('APPDATA npm ' + runner, doctor + ['--runner', runner], extra=isolated)
    run('explicit cmd and Node ' + runner, doctor + ['--runner', runner],
        extra={**isolated, variable: str(prefix / (tool + '.cmd'))})

run('missing Node', doctor + ['--runner', 'bruno'], 2,
    {'BRU_EXECUTABLE': str(prefix / 'bru.cmd'), 'NODE_EXECUTABLE': str(root / 'missing-node.exe')}, 'NODE_EXECUTABLE')
broken = root / 'incomplete npm'
broken.mkdir()
(broken / 'bru.cmd').write_text('@exit /b 99\n')
run('incomplete npm', doctor + ['--runner', 'bruno'], 2, {'BRU_EXECUTABLE': str(broken / 'bru.cmd')}, 'package.json')
run('invalid explicit runner', doctor + ['--runner', 'bruno'], 2,
    {'BRU_EXECUTABLE': str(root / 'missing-bru.cmd')}, 'BRU_EXECUTABLE')
run('missing Chromium', doctor + ['--runner', 'bruno'], 2,
    {'PLAYWRIGHT_BROWSERS_PATH': str(root / 'missing-browser')}, 'Chromium/PDF')
blocked = root / 'output-is-a-file'
blocked.write_text('fixture', encoding='utf-8')
run('unwritable output', doctor + ['--runner', 'bruno', '--output-dir', str(blocked)], 2, contains='--output-dir')
# Deny writing only inside a freshly created fixture directory, then restore its ACL.
denied = root / 'permission-denied'
denied.mkdir()
identity = subprocess.check_output(['whoami', '/user', '/fo', 'csv', '/nh'], text=True)
sid = next(csv.reader(identity.splitlines()))[1]
subprocess.run(['icacls', str(denied), '/deny', '*' + sid + ':(W)'], check=True, capture_output=True)
try:
    run('ACL write denied', doctor + ['--runner', 'bruno', '--output-dir', str(denied)], 2, contains='--output-dir')
finally:
    subprocess.run(['icacls', str(denied), '/remove:d', '*' + sid], check=True, capture_output=True)

collection = root / 'collection'
collection.mkdir()
(collection / 'opencollection.yml').write_text('opencollection: 1.0.0\ninfo:\n  name: Fixture\n')
argv_file = root / 'argv.json'
fake = root / 'no-reporter.js'
fake.write_text("require('fs').writeFileSync(" + json.dumps(str(argv_file))
                + ", JSON.stringify(process.argv.slice(2)));"
                + "console.error('unknown option PRIVATE_FIXTURE_SECRET');process.exit(1);", encoding='utf-8')
literal = ['value=ação & %PATH% ! literal', 'quote="a b"', '"outer quotes"', 'C:\\space path\\', '']
for shell in ('powershell', 'pwsh'):
    executable = shutil.which(shell)
    if not executable:
        raise AssertionError(shell + ' must be installed to validate both supported PowerShell versions')
    command = [executable, '-NoProfile', '-File', str(repo / 'scripts/audit.ps1'),
               '--collection', 'collection', '--output-dir', shell, '--diagnostics', '--']
    # -File itself loses embedded quotes under Windows PowerShell when called by another
    # native program. Encode the invocation as PowerShell source with literal array values.
    audit_args = command[4:] + literal
    script = "$ProgressPreference='SilentlyContinue'; Set-Location -LiteralPath '" + str(root).replace("'", "''") + "'; $auditArgs=@(" + ','.join(
        "'" + value.replace("'", "''") + "'" for value in audit_args) + "); & '" + str(
            repo / 'scripts/audit.ps1').replace("'", "''") + "' @auditArgs; exit $LASTEXITCODE"
    import base64
    encoded = base64.b64encode(script.encode('utf-16-le')).decode()
    run(shell + ' literal arguments and no reporter', [executable, '-NoProfile', '-EncodedCommand', encoded], 2,
        {'AUDIT_JAR': str(jar), 'BRU_EXECUTABLE': str(fake), 'NODE_EXECUTABLE': str(node)}, '--doctor')
    actual = json.loads(argv_file.read_text(encoding='utf-8'))
    assert actual[3:3 + len(literal)] == literal, (shell, actual)
    assert (root / shell).is_dir(), 'Relative output must follow PowerShell Set-Location'
    run(shell + ' missing JAR', [executable, '-NoProfile', '-File', str(repo / 'scripts/audit.ps1'), '--version'],
        2, {'AUDIT_JAR': str(root / 'missing.jar')}, 'JAR')
(root / 'validation.json').write_text(json.dumps(results, indent=2), encoding='utf-8')
print('WINDOWS_EVIDENCE_DIR=' + str(root), flush=True)
