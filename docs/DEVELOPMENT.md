# Desenvolvimento e testes

Java 25 e Maven. Build:

```bash
mvn clean test
mvn package
java -jar target/gerador-docs-tests-3.0.0.jar --version
python3 scripts/integration_test.py
```

JUnit 5/Surefire executam testes sem internet/API externa. Cobertura: formatos Bruno/Newman, arrays/iterações, Buffer UTF-8, JSON/texto/body vazio, query, assertions e skipped, scripts, resumo, sanitização recursiva/headers/URLs, escaping HTML, pretty JSON, precedência de config, detecção, filenames, proveniência local/CI, bootstrap e comparação. Testes de processo usam launcher fictício em POSIX e verificam timeout/saída nova; são desabilitados em Windows.

Fixtures de reporter em `src/test/resources/results`. Collections públicas em `examples`. Smoke mains antigos permanecem por compatibilidade, mas não são testes JUnit nem parte do gate.

O script de integração usa servidor HTTP local descartável em porta livre, executa os runners reais e o JAR empacotado com Chromium. Verifica 0/1/2, PDF não vazio, JSON/manifest, ausência de secrets fictícios, retenção explícita e remoção raw. Se Poppler estiver disponível, extrai texto para confirmar paginação e sanitização. Evidências são criadas em `output/evidence/<id>/`; não precisam de acesso ao JSONPlaceholder. O script não é chamado automaticamente por `mvn test`.

Para revisão visual, use `pdfinfo`, `pdftotext` e `pdftoppm -scale-to 1200 -png arquivo.pdf /tmp/page`. Confira começo, meio e fim de um PDF multipágina: nenhum texto cortado, sem sobreposição no footer. O teste não faz comparação pixel-perfect.

O Maven Shade inclui dependências/driver no JAR; warnings de LICENSE/MANIFEST duplicados podem ocorrer por empacotamento. Recursos de serviços são mesclados. Não fazemos upgrade massivo das dependências existentes nesta entrega.

O build usa uma única propriedade `maven.compiler.release=25`, sem preview. Compiler 3.14.1 e Shade 3.6.2 foram selecionados para o toolchain/bytecode atual; Maven Enforcer valida JDK 25+ e Maven 3.8.7+. Os scripts de integração/CI usam `JAVA_HOME/bin/java` quando disponível, evitando executar acidentalmente com um Java antigo do PATH.
