# Relatório de entrega e validação

## Validação nativa Windows 10 — 24/09/2026

Esta seção é o registro atual. As seções seguintes preservam o histórico de Linux e da migração Java; suas limitações antigas não substituem os resultados abaixo. Para o usuário final, a entrada recomendada é [INICIAR_AQUI.md](../INICIAR_AQUI.md).

### Ambiente realmente utilizado

| Componente | Versão |
|---|---|
| Sistema | Windows 10 Pro 22H2, build 19045.3448, x64 |
| Windows PowerShell | 5.1.19041.3031 |
| PowerShell | 7.6.6 |
| Java | Eclipse Temurin 25.0.4.1+1 LTS |
| Maven | 3.9.6 |
| Node / npm | 22.23.3 / 10.9.9 |
| Bruno CLI / Newman | 4.0.0 / 6.2.2 |
| Playwright / Chromium | Java 1.40.0 / 120.0.6099.28 (build 1091) |
| Python | 3.12.14 |

O Java 18 e Node 24 encontrados inicialmente não foram substituídos globalmente. JDK 25, Node 22, PowerShell 7, npm, runners e navegador foram provisionados em diretórios isolados da validação. Não foram alterados settings corporativos, TLS ou registries globais.

### Defeitos reproduzidos e ajustes

- O `ProcessBuilder` Windows, no modo legado do JDK, alterava um argumento com aspas internas antes de entregá-lo ao Node. A serialização nativa foi corrigida e testada com aspas, barras finais, valores vazios, espaços, acentos, `&` e `%PATH%` literal, sem shell. O caminho Linux permanece inalterado.
- Newman levou aproximadamente 23 segundos para responder a `--version`; o limite anterior de 15 segundos produziu um diagnóstico falso de falha. O limite agora é 60 segundos.
- A geração de PDF tentava provisionar Firefox mesmo com Chromium instalado. Auditorias agora usam apenas o navegador previamente provisionado e não fazem downloads automáticos.
- Os wrappers de CI acrescentavam `--output-dir` depois do pass-through `--`, enviando uma opção interna ao runner. PowerShell e Bash agora inserem a opção antes desse delimitador; a integração com pass-through verificou os códigos 0/1/2 e o conteúdo publicado.
- `audit.ps1` preserva argumentos literais no PowerShell 5.1/7 e normaliza falhas de inicialização para código 2. Uma regressão reproduzida após `Set-Location`/`cd` foi corrigida explicitando o diretório de trabalho do Java; collections e saídas relativas passaram a acompanhar a pasta atual nos dois PowerShells. `setup.ps1` verifica versões antes da instalação e reaproveita runners na versão requerida. Um JDK 18 foi rejeitado antes de instalar pacotes.
- A mensagem de saída sem escrita orienta verificar `--output-dir` e permissões, sem imprimir a exceção bruta. A fixture confirmou tanto caminho ocupado por arquivo quanto negação real de escrita por ACL; a ACL temporária foi restaurada.
- `.gitattributes` conserva LF nos scripts Bash/Python ao trabalhar no Windows. O empacotador também normaliza o Bash no ZIP, pois o checkout anterior ainda continha CRLF, e recusa um JAR sem a CLI/dependências obrigatórias. O workflow Linux foi alinhado ao Ubuntu 22.04, presente nas definições de dependências do Playwright 1.40; esse ajuste ainda exige execução remota.

### Testes concluídos

- Maven `package` e `setup.ps1 -Runner both` com build no PowerShell 7: **BUILD SUCCESS**. Suíte final: **39 testes encontrados, 31 executados, 0 falhas/erros, 8 ignorados por serem exclusivos de POSIX**. Cinco testes novos executam Node nativamente no Windows, incluindo limpeza após parsing inválido e retenção explícita.
- Integração completa repetida com **Windows PowerShell 5.1 e PowerShell 7**: Bruno/Newman PASS/FAIL, PDFs, JSON sanitizado, manifests, ambientes, códigos técnicos, retenção explícita e staging de CI aprovados. O staging publicou somente PDF, manifesto e `exit-code.txt`, mantendo 0/1/2 mesmo com argumentos após `--`.
- OpenCollection aninhada: **45 requests, 10 aprovadas/35 falhas**, PDF e código 1; com `--folder HOMOLOGACAO`: **15 requests, 10 aprovadas/5 falhas**, PDF e código 1. Ambiente HML nomeado e arquivo de ambiente também foram exercitados.
- Bruno executado diretamente contra a API fictícia local: **5 requests aprovados e 11/11 testes**. O fluxo usa token, propagação de variáveis, headers de correlação/idempotência, pedido e respostas negativas esperadas 401/422. Pelo gerador, a variante de negócio FAIL mantém HTTP 201, falha a assertion, gera evidência e retorna 1.
- `windows_validation.py`: **16 cenários aprovados**, cobrindo PATH, APPDATA/npm, `BRU_EXECUTABLE`, `NEWMAN_EXECUTABLE`, `NODE_EXECUTABLE`, prefixos reais com espaços/acentos/`&`/`%`, Node ausente, npm incompleto, configuração inválida, Chromium ausente, escrita negada, argumentos literais, runner sem reporter e JAR ausente nos dois PowerShells.
- Texto dos PDFs extraído com pypdf: segredos fictícios ausentes, conteúdo e rodapé presentes. PDF corporativo aberto e primeira página renderizada para inspeção visual; isso não é inspeção visual de todas as páginas de todos os relatórios.
- ZIP extraído em outra pasta com espaços, acentos, `&` e `%`: `setup.ps1 -SkipBuild`, diagnóstico e geração de PDF concluídos tanto no PowerShell 5.1 quanto no 7, com Maven removido do PATH. O conteúdo verificado inclui licença, documentação, scripts, guia simples e fixture corporativa local.
- Sintaxe de `run_ci.sh` e contratos de pass-through, códigos 0/1/2 e seleção de artefatos verificados com Git Bash e processo fictício local. Isso não é uma execução nativa Linux.

Evidências locais, ignoradas pelo Git:

- `output/evidence/Coleção & Empresa 100% dc9f8a75/validation.json`: integração com PowerShell 5.1.
- `output/evidence/Coleção & Empresa 100% 586310cd/validation.json`: repetição com PowerShell 7.
- `output/windows/Validação & 100% 6daec076/validation.json`: regressão nativa Windows.
- `output/Distribuição ZIP & 100% */validation.json`: extrações e smoke tests da distribuição; cada resumo aponta para seu PDF.
- `target/gerador-docs-tests-3.0.0-distribution.zip`: distribuição empacotada.

### Limites desta rodada

**Windows 11 não foi executado.** Linux mantém suporte e validação histórica, mas não foi reexecutado nativamente nesta máquina; não havia distribuição WSL instalada. A execução Git Bash não substitui Linux. A collection, VPN, credenciais e APIs corporativas reais não foram acessadas: todas as chamadas desta rodada usaram loopback e dados fictícios.

O [CI do commit base 1293e2d](https://github.com/ClintonAlmeidaDev/GeradorDocTests/actions/runs/36078270941) foi consultado e estava com falha. A API permitiu consultar etapas/anotações, mas respondeu 403 ao download dos logs detalhados; não foi confirmada a causa completa da falha remota. Os ajustes desta rodada não devem ser apresentados como CI remoto aprovado sem um novo run.

Falhas `EPERM` e bloqueios de criação de processos no sandbox da ferramenta foram separados das falhas do produto; os testes de browser/runners foram repetidos fora desse sandbox, com os mesmos caches isolados. A hipótese de ausência de recursão não foi confirmada: `-r` já existia. Os resultados locais não provam a causa do erro original da empresa nem garantem ausência de outros defeitos.

---

Registro da entrega original 3.0.0 com Java 21, 24/09/2026. A migração posterior para Java 25 está registrada ao final. Alterações deixadas na working tree, sem commits. Os arquivos previamente adicionados pelo usuário foram preservados no índice.

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

## Migração para Java 25 — 24/09/2026

Esta seção substitui o requisito Java 21 do registro histórico acima. O projeto agora compila com `maven.compiler.release=25`, sem preview, e exige JDK 25+ para build/runtime. Maven mínimo: 3.8.7. IDE, documentação operacional, pipelines Azure/GitHub/GitLab/Jenkins e scripts foram alinhados ao JDK 25. Scripts respeitam `JAVA_HOME`; o Java padrão do sistema não foi alterado.

Validação executada com Eclipse Temurin **25.0.4.1+1-LTS** (arquivo oficial Adoptium com SHA-256 conferido) e Maven **3.8.7**:

- `mvn clean test`: **24 testes, 0 falhas, 0 erros, 0 skipped**.
- `mvn package`: **BUILD SUCCESS**, usando Compiler 3.14.1 e Shade 3.6.2.
- JAR: bytecode da aplicação confirmado como **major 69 (Java 25)**; `--version` executou normalmente e a licença MIT continua incluída.
- `mvn validate` com JDK 21: falhou como esperado, com a mensagem do Maven Enforcer exigindo JDK 25+.
- Integração com JDK 25: Bruno PASS/FAIL e Newman PASS/FAIL, PDFs, arquivos de ambiente, sanitização, códigos 0/1/2, retenção raw explícita e staging de artifacts do CI aprovados.
- Evidências desta migração: `output/java25-evidence/92c0f836/validation.json` e PDFs/manifests nos subdiretórios dos runners (outputs locais não versionados).

As bibliotecas da aplicação e as regras de negócio foram mantidas. O Maven 3.8.7 emitiu aviso de depreciação de `sun.misc.Unsafe` vindo da sua própria dependência Guava; isso não impediu testes ou empacotamento. Não foram executados pipelines em provedores remotos.

## Correção Windows / OpenCollection (24/09/2026)

Validação local com JDK 25, Bruno CLI 4.0.0, Newman 6.2.2 e Chromium, em Linux:

- 34 testes unitários, sem falhas; incluem resolução Windows de Path/PATHEXT, APPDATA/npm, Node e caminhos com espaços, acentos, `%` e `&`.
- `--doctor --runner bruno`: versão do runner, escrita e PDF temporário verificados sem executar APIs.
- Integração real: Bruno e Newman em PASS e FAIL, produzindo PDF e manifesto com os códigos 0/1 esperados.
- OpenCollection YAML com 45 requests aninhadas em HOMOLOGACAO, LOCAL e GATEWAY, ambiente HML em YAML e scripts `runtime`: 10 aprovadas, 35 falhas, PDF gerado e código 1.
- A mesma collection com `--folder HOMOLOGACAO`: 15 requests, 5 falhas, PDF gerado e código 1. As demais pastas não foram incluídas.
- Erros técnicos retornam 2; retenção explícita do reporter e publicação de evidências no CI preservam os contratos anteriores. Saída bruta do runner não aparece nos logs; a regressão sem reporter verifica dica útil e ausência do segredo fictício.
- Integridade e conteúdo obrigatório do ZIP de distribuição verificados.

O workflow `.github/workflows/verify.yml` acrescenta a execução nativa em Windows e Linux. O job Windows valida também os scripts PowerShell e os launchers npm reais. Esse job não foi executado nesta máquina Linux; seu resultado deve ser consultado após publicar a branch. A rede e a collection privadas da empresa não foram acessadas.

A hipótese de ausência de recursão do relato não foi confirmada: `-r` já existia. Foram corrigidas lacunas reais no launcher Windows, no bloqueio do pass-through `-r`, na seleção de pasta e na informação disponível quando o reporter não é produzido. O novo diagnóstico ajuda a identificar eventuais particularidades ainda presentes no ambiente corporativo.
