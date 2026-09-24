# Troubleshooting

| Sintoma | Causa / ação |
|---|---|
| Bruno CLI não encontrado | Configure BRU_EXECUTABLE ou PATH; `bru --version` no mesmo contexto Java |
| Newman não encontrado / Cannot run program | Configure NEWMAN_EXECUTABLE. NVM no terminal não implica PATH no IntelliJ/Snap |
| Executor não gerou JSON | Collection/config/ambiente inválido, flag incompatível ou runner abortado. Verifique `bru run --help` / `newman run --help`; use execução direta em terminal privado para diagnóstico |
| Código 1 com HTTP 200/201 | Assertion, skipped, erro de script ou request falhou; é o gate correto |
| Código 2 | Configuração, input, processo, parser ou geração de PDF falhou. Não interprete artefato parcial como PASS |
| Playwright executable doesn't exist | Instale Chromium usando a CLI incluída no JAR, com a mesma conta que executa a ferramenta |
| OS não oficialmente suportado / fallback ubuntu22.04-x64 | Warning pode ser não bloqueante; valide PDF. Se launch falhar, confira bibliotecas nativas e plataforma suportada pelo Playwright instalado |
| No SLF4J providers / NOP logger | O fat JAR inclui slf4j-simple. Se ocorrer ao usar IDE, reimporte Maven e evite classpath antigo |
| JSON Bruno inválido | Reporter precisa ter raiz array e results por iteração; não é export da collection |
| Newman body ausente | Reporter usa stream Buffer. Parser converte bytes UTF-8; binários não são decodificados como documentos |
| Timeout | Ajuste --timeout-seconds; processo e descendentes são encerrados. Configure também timeouts próprios do runner via pass-through |
| Arquivo de saída já existe | Escolha outro --pdf-output/--json-output; evidência anterior não é sobrescrita |
| Sem detalhes de exception no console | Mensagens de bibliotecas podem incluir secrets. Logs são deliberadamente restritos; investigue localmente com dados fictícios/raw protegido |

Ao pedir suporte, informe versão da ferramenta, Java/Node/runner, código final, opções sem secrets e fixture mínima reproduzível. Não envie reporter bruto de empresa nem copie environment com tokens. Teste primeiro `python3 scripts/integration_test.py` para separar problema de instalação de problema da collection/VPN.

## Java 25

- `GeradorDocsTests exige JDK 25 ou superior`: o Maven está usando JDK antigo. Confira `mvn -version`, `JAVA_HOME` e o JDK do runner Maven no IntelliJ.
- `UnsupportedClassVersionError` mencionando versão 69: o JAR foi compilado para Java 25, mas o runtime selecionado é anterior. Confira `java -version`. Os scripts `run_ci.sh` e `integration_test.py` respeitam `JAVA_HOME` quando definido.
- `sun.misc.Unsafe::objectFieldOffset` ao iniciar Maven 3.8.7 com Java 25: aviso observado em uma dependência interna do Maven (Guava). Não impediu o build/testes; não é motivo para reduzir o release do projeto. Use uma distribuição Maven atualizada conforme a política do ambiente.
