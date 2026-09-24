# Relatório de entrega e validação

Entrega 3.0.0, 24/09/2026. Alterações deixadas na working tree, sem commits. Os arquivos previamente adicionados pelo usuário foram preservados no índice.

| Escopo | Status |
|---|---|
| V1.0 | Implementado; Bruno/Newman reais, PASS/FAIL, ambientes, PDF, sanitização, JAR e exit codes validados localmente |
| V1.1 | Implementado; contexto local/Git e providers CI, versão e rastreabilidade |
| V2 | Bootstrap mínimo funcional OpenAPI 3 JSON/YAML, com testes; limitações detalhadas em OPENAPI.md |
| V3 | Manifests/comparação e templates de CI/artifact entregues; publicação remota não executada |

## Arquitetura e arquivos principais

- `pom.xml`: Java 21, JUnit 5/Surefire, YAML, logging e Shade/Main-Class.
- `runner/AuditCli`, `AuditService`, `CollectionDetector`: entrada, orquestração, detecção e códigos de saída.
- `config/CliOptions`: precedência, properties/env/CLI, pass-through e validação.
- `executor/*`: processos isolados, PATH/Node, timeout e reporter fresco.
- `parser/*`: modelo comum, iterações/scripts Bruno, Buffer/query/skipped Newman.
- `sanitizer/SensitiveDataSanitizer`: mascaramento recursivo e de valores refletidos; raw temporário.
- `context/ExecutionContextCollector`: proveniência LOCAL/Git/CI.
- `report/*`, `auditor/AuditReportGenerator`, template comum: HTML/PDF paginado.
- `openapi/OpenApiBootstrap`, `history/RunManifest`: skeleton e histórico sem banco.
- `src/test/java/br/com/clinton/tests`, fixtures, `scripts/integration_test.py`: validação automatizada.
- README, changelog, docs e exemplos de CI: operação e manutenção.

Fluxo final: collection → executor → parser → AuditReport → sanitização → Thymeleaf → Chromium → PDF. JSON normalizado sanitizado e manifest acompanham a evidência.

## Build e resultados

`mvn clean test`: **24 testes, 0 falhas, 0 erros, 0 skipped**.

`mvn package`: build aprovado, JAR executável `target/gerador-docs-tests-3.0.0.jar`. O JAR é grande (~167 MiB) porque inclui o driver bundle multiplataforma do Playwright. O navegador continua instalado separadamente.

Validações adicionais: CLI help/version, bootstrap pelo JAR para Bruno e Postman, comparação real PASS→FAIL, sintaxe dos scripts, links locais da documentação e `git diff --check`.

## Integração real

`python3 scripts/integration_test.py` inicia API local HTTP com payload longo e secret fictício. Usa Bruno 4.0.0, Newman 6.2.2 e Chromium/Playwright 1.40.0. Os testes unitários não dependem de internet; a integração depende apenas das ferramentas instaladas e acesso ao loopback.

| Runner/cenário | Requests pass/fail | Assertions pass/fail | Exit | PDF |
|---|---:|---:|---:|---|
| Bruno PASS | 5 / 0 | 1 / 0 | 0 | 16 páginas |
| Bruno FAIL | 4 / 1 | 0 / 1 | 1 | 16 páginas |
| Newman PASS | 2 / 0 | 2 / 0 | 0 | 8 páginas |
| Newman FAIL | 1 / 1 | 1 / 1 | 1 | 8 páginas |

Arquivos de ambiente reais foram passados aos dois runners. Também foram verificados: código 2 para input/executável ausente; reporter raw ausente por padrão; retenção explícita com permissão 0600; limpeza após falha de parsing; secrets ausentes de console/JSON/texto do PDF; script de CI preservando exit code 1 e preparando PDF/manifest. PDFs foram renderizados com Poppler para inspeção visual de cabeçalho, payloads longos, páginas intermediárias, finais e footer.

## Operação

Bruno: `java -jar target/gerador-docs-tests-3.0.0.jar --collection /caminho/bruno --bruno-env HML --environment HML --company Empresa`.

Postman: `java -jar target/gerador-docs-tests-3.0.0.jar --collection /caminho/api.postman_collection.json --environment-file /caminho/hml.json --environment HML`.

Saída padrão `audit-output/auditoria_api_<collection>_<environment>_<timestamp>_<PASS|FAIL>.pdf`. Código 0 aprova; 1 reprova testes e mantém PDF; 2 indica problema técnico/configuração. `--mask-key` amplia o mascaramento; `--keep-raw-results` é somente para diagnóstico privado. Logs livres de scripts são suprimidos para proteger secrets.

Pipeline: use os templates em `examples/` e `scripts/run_ci.sh`; a publicação roda mesmo com falha de testes. Consulte CI_CD.md para Secure Files, ambientes, artifact e referência em PR.

## Limitações conhecidas

- Collection corporativa, VPN, credenciais e publicação real Azure/GitHub/GitLab/Jenkins não estavam disponíveis e não foram simuladas como integrações remotas concluídas.
- OpenAPI gera somente skeleton básico; requer variáveis, auth e assertions funcionais definidos pelo time.
- Sanitização não é anonimização universal de texto livre/binário nem assinatura criptográfica da evidência.
- Linux foi validado; Windows/macOS e shims `.cmd` precisam de homologação específica.
- Comparação usa método/URL/nome/ocorrência; URLs dinâmicas ou reordenação de duplicadas podem alterar o pareamento.
- Dependências centrais existentes foram mantidas. Antes da distribuição corporativa, aplique a política de supply chain e retenção da empresa.

Evidências locais da rodada final: `output/evidence/56c44089/validation.json`, com os quatro PDFs PASS/FAIL e respectivos manifests nos subdiretórios dos runners. São outputs ignorados pelo Git; cada nova execução do script cria outro diretório. As cópias raw fictícias usadas no teste de retenção foram removidas pelo próprio teste.
