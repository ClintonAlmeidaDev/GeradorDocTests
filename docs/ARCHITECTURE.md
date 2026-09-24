# Arquitetura

A aplicação é Java puro; não inicia servidor e não depende de banco. O fluxo público começa em `br.com.clinton.runner.AuditCli`, que converte argumentos em `CliOptions` e delega para `AuditService`. O wrapper deprecated `BrunoAuditRunner` mantém o entrypoint antigo, agora genérico.

| Camada | Responsabilidade |
|---|---|
| config/CliOptions | Defaults, properties, env, CLI e validação |
| runner/CollectionDetector | Reconhecer diretório Bruno/arquivo Postman |
| executor/CollectionExecutor | Contrato de execução; resultado com exit code e existência de reporter |
| executor/ExternalProcess | ProcessBuilder sem shell, PATH, timeout e descarte de stdout não confiável |
| parser/ResultParser | Contrato de normalização; parsers independentes por formato |
| model | AuditReport, RequestExecution, AssertionResult, ExecutionSummary, ReportMetadata |
| sanitizer | Cópia sanitizada; domínio original permanece intacto |
| context | Proveniência local, Git e providers CI; falha de Git é tolerada |
| report / auditor | Thymeleaf comum e Playwright Chromium com paginação |
| history | Manifest versionado e comparação sem banco |
| openapi | Bootstrap offline de endpoints básicos |

`ParserSupport` centraliza body JSON/texto e contagens. Bruno lê todas as iterações e combina `assertionResults`/`testResults`. Newman decodifica Buffer UTF-8 e considera falhas globais de scripts em `run.failures`. O status da request depende da execução, ausência de erro e aprovação das assertions, não apenas do HTTP.

Skipped é armazenado explicitamente; conta em total e falhas/não aprovadas, e torna a request FAIL. Uma collection vazia é erro técnico, evitando evidência PASS sem execução.

A camada de apresentação usa Object nos bodies, sanitiza e então formata. Thymeleaf escapa texto (não há `th:utext`). O navegador bloqueia solicitações externas durante a renderização. O manifest não copia payloads.

Pontos de extensão: adicionar executor/parser implementando as interfaces; publicar o manifest/PDF após o comando na automação corporativa; estender o coletor de contexto para outro provider. As classes de configuração antigas permanecem por compatibilidade de fonte, mas a CLI nova usa exclusivamente `CliOptions`.

Não há upload remoto ou credencial embutida. A versão vem de resource filtrado pelo Maven. Git é coletado a partir do diretório da collection: se ela estiver fora do checkout, isso será refletido como Git indisponível.
