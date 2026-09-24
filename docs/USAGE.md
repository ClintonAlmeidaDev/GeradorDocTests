# Uso operacional

Após `mvn package`, use `java -jar target/gerador-docs-tests-3.0.0.jar --help`.

## Bruno externo

```bash
java -jar target/gerador-docs-tests-3.0.0.jar \
  --collection /caminho/collection --bruno-env HML --environment HML \
  --company Empresa --output-dir ./audit-output --mask-key cpf
```

## Postman externo

```bash
java -jar target/gerador-docs-tests-3.0.0.jar \
  --collection /caminho/api.postman_collection.json \
  --environment-file /caminho/hml.postman_environment.json --environment HML
```

Para Bruno que usa environment file: `--environment-file /caminho/hml.bru`. Para folders Newman: acrescente `-- --folder Payments`. Bruno já usa execução recursiva `-r`. Para iterações: `-- --iteration-count 3`.

Os exemplos `examples/bruno-pass`, `examples/bruno-fail`, `examples/pass.postman_collection.json` e `examples/fail.postman_collection.json` usam JSONPlaceholder. São demonstrações públicas, dependentes de internet e da disponibilidade desse serviço. Nunca apontam para dados corporativos.

Para validação determinística sem API externa:

```bash
python3 scripts/integration_test.py
```

O script inicia API em loopback, executa ambos os runners e o JAR, verifica PASS/FAIL, código 2, retenção explícita e PDF. Precisa de Python, Bruno, Newman e Chromium instalados; suas evidências ficam em `output/evidence/<id>/`.

## Histórico

```bash
java -jar target/gerador-docs-tests-3.0.0.jar compare \
  /caminho/anterior.audit-run.json /caminho/atual.audit-run.json
```

Compare a mesma collection e ambiente. O relatório lista requests novas/removidas, regressões/recuperações, delta de tempo em ms e assertions novas/removidas. Identidade: método + URL sanitizada + nome + ocorrência. Reordenar requests duplicadas ou alterar URLs dinâmicas pode afetar o pareamento; não é um motor estatístico de performance. Assertions são pareadas por expressão; sem identificadores estáveis, renomeações aparecem como remoção/adição.

## Diagnóstico

`--keep-raw-results` mantém reporter sensível com UUID no output-dir, inclusive se o processamento posterior falhar. O arquivo normalizado de `--json-output` continua sanitizado. Não use retenção em pipeline compartilhado. Não sobrescrevemos arquivos existentes. Em erro técnico, artefatos parciais podem existir e não significam execução aprovada; considere sempre o exit code.
