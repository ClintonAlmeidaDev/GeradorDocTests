#!/usr/bin/env python3
"""Real Bruno/Newman + packaged Java + Chromium, against a disposable local API.
Run after mvn package. Requires Python 3, installed runners and Playwright Chromium.
No remote API calls; collections and fake secrets are generated under the output path.
"""
import argparse
import http.server
import json
import os
import re
from pathlib import Path
import shutil
import subprocess
import threading
import uuid

parser = argparse.ArgumentParser()
parser.add_argument('--jar', default='target/gerador-docs-tests-3.0.0.jar')
parser.add_argument('--output', default='output/evidence')
args = parser.parse_args()
jar = Path(args.jar).resolve()
java = str(Path(os.environ['JAVA_HOME']) / 'bin' / 'java') if os.environ.get('JAVA_HOME') else 'java'
root = Path(args.output).resolve() / ('Coleção & Empresa 100% ' + str(uuid.uuid4())[:8])
root.mkdir(parents=True)
secret = 'FICTIONAL_SENSITIVE_VALUE_7934'

class Handler(http.server.BaseHTTPRequestHandler):
    def log_message(self, *_):
        pass
    def answer(self):
        data = json.dumps({'id': 1, 'title': 'Teste local', 'clientSecret': secret,
                           'items': [{'index': i, 'text': 'Payload longo para verificar paginação e footer.'} for i in range(0 if self.path.startswith('/short') else 60)]}).encode()
        self.send_response(201 if self.command == 'POST' else 200)
        self.send_header('Content-Type', 'application/json; charset=utf-8')
        self.send_header('Set-Cookie', 'session=' + secret + '; Path=/; HttpOnly')
        self.send_header('Content-Length', str(len(data)))
        self.end_headers()
        self.wfile.write(data)
    do_GET = do_POST = do_PUT = do_DELETE = answer

server = http.server.ThreadingHTTPServer(('127.0.0.1', 0), Handler)
threading.Thread(target=server.serve_forever, daemon=True).start()
base = 'http://127.0.0.1:' + str(server.server_port)
env = dict(os.environ)
for key in ('HTTP_PROXY', 'HTTPS_PROXY', 'ALL_PROXY', 'http_proxy', 'https_proxy', 'all_proxy'):
    env.pop(key, None)
env['NO_PROXY'] = '127.0.0.1,localhost'
results = []
try:
    for runner in ('bruno', 'postman'):
        for state, expected in (('pass', 0), ('fail', 1)):
            dest = root / (runner + '-' + state)
            dest.mkdir()
            if runner == 'bruno':
                collection = dest / 'collection'
                shutil.copytree('examples/bruno-' + state, collection)
                for p in collection.glob('*.yml'):
                    text = p.read_text(encoding='utf-8').replace('https://jsonplaceholder.typicode.com', '{{baseUrl}}')
                    p.write_text(re.sub(r'(?m)^(\s*url: )(.+)$', lambda m: m[1] + json.dumps(m[2]), text), encoding='utf-8')
                environment = dest / 'hml.bru'
                environment.write_text('vars {\n  baseUrl: ' + base + '\n}\n', encoding='utf-8')
                extra = ['--environment-file', str(environment), '--', '--noproxy']
            else:
                collection = dest / 'fixture.postman_collection.json'
                data = json.loads(Path('examples/' + state + '.postman_collection.json').read_text(encoding='utf-8'))
                for item in data['item']:
                    url = item['request']['url']['raw'].replace('https://jsonplaceholder.typicode.com', '{{baseUrl}}')
                    item['request']['url'] = url
                collection.write_text(json.dumps(data))
                environment = dest / 'hml.postman_environment.json'
                environment.write_text(json.dumps({'name': 'LOCAL', 'values': [{'key': 'baseUrl', 'value': base, 'enabled': True}]}))
                extra = ['--environment-file', str(environment)]
            command = [java, '-jar', str(jar), '--collection', str(collection), '--output-dir', str(dest / 'reports'),
                       '--environment', 'LOCAL', '--company', 'EMPRESA EXEMPLO', '--timeout-seconds', '120'] + extra
            run = subprocess.run(command, env=env, text=True, encoding='utf-8', errors='replace', stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=180)
            assert run.returncode == expected, (runner, state, run.returncode, run.stdout)
            assert secret not in run.stdout
            pdfs = list((dest / 'reports').glob('*.pdf'))
            assert len(pdfs) == 1 and pdfs[0].stat().st_size > 1000
            assert pdfs[0].name.endswith(state.upper() + '.pdf')
            normalized = next((dest / 'reports').glob('*.sanitized.json'))
            assert secret not in normalized.read_text(encoding='utf-8')
            manifest = next((dest / 'reports').glob('*.audit-run.json'))
            report = json.loads(manifest.read_text(encoding='utf-8'))
            assert report['result'] == state.upper()
            assert report['summary']['failedRequests'] == (0 if state == 'pass' else 1)
            assert not list((dest / 'reports').glob('*.raw.json'))
            if shutil.which('pdftotext'):
                text = subprocess.check_output(['pdftotext', str(pdfs[0]), '-'], text=True, encoding='utf-8', errors='replace')
                assert secret not in text
                assert 'Página' in text and 'Payload Recebido' in text
                assert 'Evidência' in text or 'EVIDÊNCIA' in text
            results.append({'runner': runner, 'scenario': state, 'exitCode': run.returncode, 'pdf': str(pdfs[0]), 'manifest': str(manifest)})
            print(runner, state, 'OK; PDF:', pdfs[0], flush=True)
    # Reproduce an OpenCollection tree with 45 requests, named YAML environment,
    # runtime tests, inherited root context and literal Windows-style path hazards.
    nested = root / 'nested-opencollection'
    nested.mkdir()
    (nested / 'opencollection.yml').write_text('opencollection: 1.0.0\ninfo:\n  name: Nested regression\n', encoding='utf-8')
    environments = nested / 'environments'
    environments.mkdir()
    (environments / 'HML.yml').write_text('name: HML\nvariables:\n  - name: baseUrl\n    value: ' + base + '\n', encoding='utf-8')
    for index in range(45):
        branch = 'HOMOLOGACAO/NEGOCIO/INTRODUCTION' if index < 15 else ('LOCAL' if index < 30 else 'GATEWAY')
        folder = nested / branch
        folder.mkdir(parents=True, exist_ok=True)
        expected_status = 200 if index < 10 else 418
        (folder / f'request-{index:02}.yml').write_text(f'''info:
  name: Request {index:02}
  type: http
  seq: {index + 1}
http:
  method: get
  url: "{{{{baseUrl}}}}/short/{index}"
runtime:
  scripts:
    - type: tests
      code: |-
        test("status esperado", () => {{ expect(res.getStatus()).to.equal({expected_status}); }});
''', encoding='utf-8')
    for scenario, folder_args, count, failures in (
        ('all-45', [], 45, 35),
        ('homologacao-15', ['--folder', 'HOMOLOGACAO'], 15, 5),
    ):
        reports = root / scenario
        command = [java, '-jar', str(jar), '--collection', str(nested), '--bruno-env', 'HML',
                   '--environment', 'HML', '--output-dir', str(reports), '--diagnostics'] + folder_args + ['--', '-r', '--noproxy']
        run = subprocess.run(command, env=env, text=True, encoding='utf-8', errors='replace',
                             stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=180)
        assert run.returncode == 1, (scenario, run.returncode, run.stdout)
        assert secret not in run.stdout
        manifest = next(reports.glob('*.audit-run.json'))
        report = json.loads(manifest.read_text(encoding='utf-8'))
        assert len(report['requests']) == count, report['summary']
        assert report['summary']['failedRequests'] == failures, report['summary']
        assert all(request['assertions'] for request in report['requests'])
        pdf = next(reports.glob('*.pdf'))
        assert pdf.stat().st_size > 1000
        results.append({'runner': 'bruno', 'scenario': scenario, 'exitCode': 1, 'pdf': str(pdf), 'manifest': str(manifest)})
        print('Bruno nested', scenario, 'OK;', count, 'requests;', failures, 'failures; PDF:', pdf, flush=True)
    missing = subprocess.run([java, '-jar', str(jar), '--collection', str(root / 'missing')], env=env, capture_output=True)
    assert missing.returncode == 2
    # Existing collection + missing executable must be technical failure.
    absent = dict(env, NEWMAN_EXECUTABLE=str(root / 'missing-executable'))
    bad = subprocess.run([java, '-jar', str(jar), '--collection', str(collection), '--output-dir', str(root / 'missing-runner')], env=absent, capture_output=True)
    assert bad.returncode == 2 and b'NEWMAN_EXECUTABLE' in bad.stderr
    # Explicit raw retention uses distinct restricted .raw.json; output JSON remains sanitized.
    retained = root / 'retention'
    keep = subprocess.run([java, '-jar', str(jar), '--collection', str(collection), '--keep-raw-results', '--environment-file', str(environment), '--output-dir', str(retained)], env=env, capture_output=True, timeout=180)
    assert keep.returncode == 1, keep.stderr.decode()
    assert len(list(retained.glob('*.raw.json'))) == 1
    for raw in retained.glob('*.raw.json'):
        assert secret in raw.read_text(encoding='utf-8')
        if os.name == 'posix': assert raw.stat().st_mode & 0o077 == 0
        raw.unlink()  # deliberately sensitive fixture; do not retain in delivered evidence
    print('Technical exit codes and explicit retention: OK', flush=True)
    ci = root / 'ci'
    ci.mkdir()
    ci_launcher = ['pwsh', '-NoProfile', '-File', str(Path('scripts/run_ci.ps1').resolve())] if os.name == 'nt' else ['bash', str(Path('scripts/run_ci.sh').resolve())]
    ci_run = subprocess.run(ci_launcher + [ '--collection', str(collection), '--environment-file', str(environment)],
                            cwd=ci, env=dict(env, AUDIT_JAR=str(jar)), capture_output=True, timeout=180)
    assert ci_run.returncode == 1, ci_run.stderr.decode()
    assert list((ci / 'audit-artifacts').glob('*.pdf'))
    assert list((ci / 'audit-artifacts').glob('*.audit-run.json'))
    assert not list((ci / 'audit-artifacts').glob('*.raw.json'))
    assert (ci / 'audit-artifacts/exit-code.txt').read_text(encoding='utf-8').strip() == '1'
    print('CI staging preserves functional failure and publishes evidence: OK', flush=True)
    (root / 'validation.json').write_text(json.dumps(results, indent=2), encoding='utf-8')
finally:
    server.shutdown()
print('EVIDENCE_DIR=' + str(root), flush=True)
