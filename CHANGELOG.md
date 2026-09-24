# Changelog

## Não lançado

- Migração do build e runtime para Java 25, incluindo IDE, documentação e pipelines.
- Compiler 3.14.1 e Shade 3.6.2 para empacotamento de bytecode Java 25.
- Maven Enforcer exige JDK 25+ e Maven 3.8.7+ com mensagem de configuração explícita.

## 3.0.0 - 2026-09-24

- CLI genérica com autodetecção Bruno/Postman, configuração properties/env/args e JAR executável Java 21.
- Exit codes 0/1/2; evidência preservada em falhas funcionais; timeout e reporter fresco.
- Parsers com todas as iterações Bruno, testes de script, Buffer UTF-8 Newman, queries e skipped conservador.
- Sanitização antes dos outputs, headers/query/cookies e campos adicionais; raw temporário e retenção explícita.
- Template comum, rastreabilidade LOCAL/CI/CD, Git CLEAN/DIRTY, versão e footer multipágina.
- Bootstrap básico OpenAPI JSON/YAML, manifests sanitizados e comparação por arquivos.
- JUnit 5, integração local com runners/Chromium reais, exemplos públicos e documentação operacional.
- Templates de pipeline Azure DevOps, GitHub Actions, GitLab CI e Jenkins, sem credenciais embutidas.

## Estado anterior

Fluxo Bruno com template PDF e parser Newman inicial, smoke mains manuais, configuração parcial e ausência de empacotamento/gate de exit code.
