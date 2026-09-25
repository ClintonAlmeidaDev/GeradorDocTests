#!/usr/bin/env python3
"""Validate the end-user ZIP on Windows without Maven on PATH."""
import argparse
import http.server
import json
import os
from pathlib import Path
import shutil
import subprocess
import threading
import uuid
from zipfile import ZipFile
from corporate_fixture import CorporateHandler

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--zip', default='target/gerador-docs-tests-3.0.0-distribution.zip')
parser.add_argument('--powershell', default='powershell')
args = parser.parse_args()
if os.name != 'nt':
    raise SystemExit('This end-user PowerShell regression requires Windows.')
repo = Path(__file__).resolve().parent.parent
root = repo / 'output' / ('Distribuição ZIP & 100% ' + uuid.uuid4().hex[:8])
root.mkdir(parents=True)
with ZipFile(args.zip) as archive:
    assert archive.testzip() is None
    shell_script = next(name for name in archive.namelist() if name.endswith('/scripts/run_ci.sh'))
    assert b'\r\n' not in archive.read(shell_script), 'Bash in the ZIP must use LF even when built on Windows'
    archive.extractall(root)
project = next(p for p in root.iterdir() if p.is_dir())
for required in ('LICENSE', 'README.md', 'INICIAR_AQUI.md', 'docs/WINDOWS.md', 'scripts/setup.ps1',
                 'scripts/audit.ps1', 'scripts/run_ci.ps1', 'scripts/corporate_fixture.py',
                 'examples/bruno-corporate/opencollection.yml', 'target/gerador-docs-tests-3.0.0.jar'):
    assert (project / required).is_file(), required
assert not (project / 'pom.xml').exists()
env = dict(os.environ)
env.pop('AUDIT_JAR', None)
path_key = next(k for k in env if k.upper() == 'PATH')
# Remove every directory exposing Maven, not only directories named "maven".
env[path_key] = ';'.join(p for p in env[path_key].split(';')
                         if not (Path(p.strip('"')) / 'mvn.cmd').is_file())
assert shutil.which('mvn', path=env[path_key]) is None
shell = shutil.which(args.powershell)
setup = subprocess.run([shell, '-NoProfile', '-File', str(project / 'scripts/setup.ps1'),
                        '-SkipBuild', '-Runner', 'bruno'], cwd=project, env=env, capture_output=True, timeout=240)
assert setup.returncode == 0, setup.stderr.decode(errors='replace')
collection = root / 'Collection corporativa'
shutil.copytree(project / 'examples/bruno-corporate', collection)
server = http.server.ThreadingHTTPServer(('127.0.0.1', 0), CorporateHandler)
threading.Thread(target=server.serve_forever, daemon=True).start()
environment = collection / 'environments/LOCAL.yml'
environment.write_text(environment.read_text(encoding='utf-8').replace(
    'http://127.0.0.1:8765', f'http://127.0.0.1:{server.server_port}'), encoding='utf-8')
try:
    run = subprocess.run([shell, '-NoProfile', '-File', str(project / 'scripts/audit.ps1'),
                          '--collection', str(collection), '--folder', 'HOMOLOGACAO', '--bruno-env', 'LOCAL',
                          '--output-dir', str(root / 'Evidências'), '--', '--noproxy'],
                         cwd=project, env=env, capture_output=True, timeout=180)
    assert run.returncode == 0, (run.stdout.decode(errors='replace'), run.stderr.decode(errors='replace'))
    pdf = next((root / 'Evidências').glob('*.pdf'))
    assert pdf.read_bytes().startswith(b'%PDF-') and pdf.stat().st_size > 1000
    summary = {'setupExit': setup.returncode, 'auditExit': run.returncode, 'mavenOnPath': False,
               'powershell': shell, 'pdf': str(pdf), 'distribution': str(project)}
    (root / 'validation.json').write_text(json.dumps(summary, indent=2), encoding='utf-8')
    print(json.dumps(summary, indent=2), flush=True)
finally:
    server.shutdown()
    server.server_close()
