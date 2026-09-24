#!/usr/bin/env python3
"""Produce Markdown to reference an existing CI artifact. Does not post to PRs."""
import argparse
from pathlib import Path
p = argparse.ArgumentParser()
p.add_argument('--artifact-url', required=True)
p.add_argument('--output', default='audit-artifacts/pr-evidence.md')
a = p.parse_args()
if not a.artifact_url.startswith('https://') or any(c in a.artifact_url for c in '\r\n<>'):
    p.error('Use a valid HTTPS artifact URL')
out = Path(a.output)
out.parent.mkdir(parents=True, exist_ok=True)
out.write_text('## Evidência de testes de API\n\nRelatório e manifest disponíveis no artifact do pipeline:\n\n<' + a.artifact_url + '>\n\nConsulte o exit code do job (0 PASS, 1 FAIL, 2 erro técnico).\n')
print(out)
