# Bruno

Detecção: diretório com `opencollection.yml` ou `bruno.json`. O parser trabalha com reporter JSON em array de iterações, cada uma contendo `results`. Todos os resultados são normalizados; não há acesso incorreto a `root.results`.

Comando base verificado em Bruno CLI 4.0.0: `bru run -r --reporter-json <temporário>`. `-r` cobre folders. Seleção: `--bruno-env HML` vira `--env HML`; `--environment-file` vira `--env-file`. Flags extras usam pass-through e continuam sujeitas à versão instalada do Bruno (`bru run --help`).

`request.data` JSON em string vira Object; XML/texto é preservado. `response.data` estruturado continua estruturado. `response.status/statusText/responseTime/size` alimentam os detalhes. `assertionResults` e `testResults` de scripts alimentam a lista comum. Error técnico e assertion/teste falho fazem a request FAIL, mesmo quando `status=pass` e HTTP 200.

Autenticação, cookies, redirects, scripts e resolução de variáveis são responsabilidade do Bruno. Não fazemos interpretação própria de credenciais. Configuração de proxy/CA pode ser passada ao CLI. Os testes locais usam `--noproxy`, sem desabilitar TLS nas execuções normais.

Executável: `BRU_EXECUTABLE` ou `bru`. NODE_OPTIONS existente é preservado. stdout/stderr livres são consumidos sem publicação para impedir vazamento por scripts; o progresso seguro vem da CLI Java. Para inspecionar falhas do runner, use reporter mantido com cuidado ou execute diretamente o Bruno em terminal privado.
