# Postman / Newman

Detecção por JSON com `info.schema` v2.x e `item`, ou nome `.postman_collection.json` com `item`. Override: `--runner postman`. Execução verificada com Newman 6.2.2: `newman run <arquivo> --reporters cli,json --reporter-json-export <temporário>`.

Environment: `--environment-file /caminho/hml.postman_environment.json` vira `--environment`. `--environment HML` apenas rotula o relatório; não deve ser confundido com a flag nativa do Newman. Auth, pre-request/test scripts, folders e variáveis continuam no Newman.

`run.executions` associa assertions à request. `run.failures` indica falha global, incluindo scripts sem assertion associada. URL aceita string, raw ou protocol/host/path/port/query/hash; query disabled é excluída. Bodies raw JSON são normalizados, texto preservado; formdata/urlencoded também podem ser mostrados como estruturas. Arquivos binários não são embutidos.

O response stream Buffer é convertido de bytes UTF-8 e depois tenta JSON. Ausência de response ou requestError representa falha da API testada. Assertion com error é FAIL; skipped é SKIPPED, conta como não aprovada e falha o gate. HTTP 201 por si só não faz passar uma assertion esperando 200.

`NEWMAN_EXECUTABLE` configura a instalação quando o PATH do IntelliJ não inclui o NVM. Saída livre dos scripts é suprimida por segurança. Não use `--suppress-exit-code` para tentar aprovar a execução: falhas do domínio e `run.failures` também são verificadas.
