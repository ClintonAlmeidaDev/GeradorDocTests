# Configuração

Precedência: **defaults < arquivo .properties UTF-8 < variáveis AUDIT_* < CLI**. `--config PATH` ou `AUDIT_CONFIG` seleciona o arquivo. As chaves do arquivo são as mesmas opções, sem `--`. Não há Spring/framework de configuração.

| Opção | Default / semântica |
|---|---|
| collection | Obrigatória para execução; caminho local |
| runner | auto; override bruno ou postman |
| environment | LOCAL; rótulo, não altera o servidor alvo |
| company | SUA_EMPRESA |
| executor | Usuário do SO, separado do autor Git |
| collection-name | Nome do diretório/arquivo sem sufixo Postman |
| output-dir | audit-output |
| pdf-output | Nome automático PASS/FAIL |
| json-output | JSON sanitizado ao lado do PDF |
| keep-raw-results | false; CLI aceita flag isolada ou `=true/false` |
| mask-key | Repetível na CLI; lista separada por vírgulas em properties/env |
| environment-file | Arquivo do ambiente do runner; convertido em caminho absoluto |
| bruno-env | Nome do ambiente Bruno, passado com --env |
| timeout-seconds | 1800; inteiro positivo, termina processo e descendentes |
| tool-arg | Repetível; cada valor é um argumento literal |
| openapi / generate | Bootstrap offline; generate padrão postman |

Exemplo: `AUDIT_OUTPUT_DIR=/artifacts`, `AUDIT_ENVIRONMENT=HML`, `AUDIT_COMPANY=Empresa`. Executáveis usam `BRU_EXECUTABLE` e `NEWMAN_EXECUTABLE` (defaults `bru`, `newman`). Nenhum valor de secret deve ser passado na linha de comando, pois o SO pode expor argumentos de processos. Prefira ambientes/secret providers do runner.

```properties
company=MINHA_EMPRESA
environment=HML
runner=auto
output-dir=./audit-output
keep-raw-results=false
mask-key=cpf,accountNumber
```

CLI aceita `--opção=valor` e `--opção valor`. Para pass-through: `--tool-arg=--folder --tool-arg=Payments` ou `-- --folder Payments`. `--tool-args` também inicia a lista literal final. Não se faz divisão por espaços nem avaliação de shell. Opções de reporter/export são reservadas: saídas adicionais do runner contornariam a política de temporários.

Chaves desconhecidas geram erro 2. Caminhos relativos de arquivos de ambiente são resolvidos antes de mudar o diretório do runner. Outros caminhos em pass-through devem ser absolutos; o cwd é o diretório da collection (ou pai do arquivo Postman). Opções antigas de `AuditConfigurationLoader` não participam da nova CLI.

## Diagnóstico e pasta

`--folder` seleciona uma pasta do runner (no Bruno, dentro da raiz da collection, com recursão). `--doctor --runner bruno|postman` verifica somente a instalação local. `--diagnostics` acrescenta informações de execução sem imprimir stdout/stderr bruto nem valores dos argumentos adicionais. Veja [Windows](WINDOWS.md) para exemplos completos.
