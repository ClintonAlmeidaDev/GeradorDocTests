#!/usr/bin/env python3
"""Build an end-user ZIP from an already packaged JAR; no Maven needed by consumers."""
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parent.parent
version = ET.parse(root / 'pom.xml').getroot().find('{*}version').text
name = f'gerador-docs-tests-{version}'
jar = root / 'target' / f'{name}.jar'
if not jar.is_file():
    raise SystemExit('Execute mvn package antes de empacotar a distribuicao.')
files = [jar, root / 'LICENSE', root / 'README.md', root / 'examples/maven-settings.xml']
files += sorted((root / 'docs').glob('*.md'))
files += [root / 'scripts' / f for f in ('audit.ps1', 'setup.ps1', 'run_ci.ps1', 'run_ci.sh')]
with ZipFile(root / 'target' / f'{name}-distribution.zip', 'w', ZIP_DEFLATED) as archive:
    for file in files:
        archive.write(file, str(Path(name) / file.relative_to(root)))
print(root / 'target' / f'{name}-distribution.zip')
