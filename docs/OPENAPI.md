# Bootstrap OpenAPI

```bash
java -jar target/gerador-docs-tests-3.0.0.jar \
  --openapi examples/minimal-openapi.yaml --generate postman --output-dir ./generated
java -jar target/gerador-docs-tests-3.0.0.jar \
  --openapi examples/minimal-openapi.yaml --generate bruno --output-dir ./generated
```

Entrada local OpenAPI **3.x JSON ou YAML**. Saídas: `openapi.postman_collection.json` ou diretório `openapi-bruno/` com OpenCollection YAML. Não executa a collection gerada. Não faz download do contrato nem resolve refs externos; forneça contrato bundled. Destino existente é recusado.

Escopo mínimo funcional: métodos HTTP comuns, paths, parâmetros de path/query/header definidos no path ou operação, requestBody JSON/texto e schemas simples object/array/string/number/boolean. `$ref` local básico é suportado com limite de profundidade/ciclos. Bodies recebem placeholders TODO/0/false. `baseUrl` Postman começa em `https://example.invalid`; Bruno requer variável `baseUrl` no ambiente. Preencha também as variáveis de path/query/header.

Não copiamos exemplos do contrato, pois podem conter dados sensíveis. Não inferimos autenticação, encadeamento, assertions de negócio ou validação completa de response schemas. Composições oneOf/allOf, serialização complexa de parâmetros, multipart, callbacks, webhooks e refs encadeados complexos não são implementados. Revise manualmente o skeleton e o contrato antes de executar.

OpenAPI é contrato intermediário. Para Spring, exporte o contrato pelo mecanismo apropriado (por exemplo springdoc-openapi); a ferramenta não interpreta `@RestController`. Gere skeleton, complemente com cenários reais na ferramenta de API e então audite essa collection. Um skeleton sem assertions verifica somente execução/transporte, não regras funcionais.
