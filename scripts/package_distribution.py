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
with ZipFile(jar) as built:
    for required in ('br/com/clinton/runner/AuditCli.class', 'com/microsoft/playwright/CLI.class',
                     'META-INF/GeradorDocsTests/LICENSE'):
        if required not in built.namelist():
            raise SystemExit('JAR incompleto. Aguarde mvn package concluir antes de criar o ZIP.')
files = [jar, root / 'LICENSE', root / 'README.md', root / 'INICIAR_AQUI.md', root / 'examples/maven-settings.xml']
files += sorted((root / 'docs').glob('*.md'))
files += sorted(p for p in (root / 'examples/bruno-corporate').rglob('*') if p.is_file())
files += [root / 'scripts' / f for f in ('audit.ps1', 'setup.ps1', 'run_ci.ps1', 'run_ci.sh', 'corporate_fixture.py')]
with ZipFile(root / 'target' / f'{name}-distribution.zip', 'w', ZIP_DEFLATED) as archive:
    for file in files:
        arcname = (Path(name) / file.relative_to(root)).as_posix()
        if file.suffix == '.sh':
            archive.writestr(arcname, file.read_bytes().replace(b'\r\n', b'\n'))
        else:
            archive.write(file, arcname)
print(root / 'target' / f'{name}-distribution.zip')
