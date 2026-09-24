# Instalação

Use Java 21 (o pom compila com release 21), Maven 3.8 ou superior e Node 22. Os runners verificados foram Bruno CLI 4.0.0 e Newman 6.2.2. Playwright/Jackson/Thymeleaf foram mantidos nas versões existentes; somente YAML, logging e infraestrutura de testes/build foram acrescentados.

```bash
java -version
mvn -version
node --version
npm --version
bru --version
newman --version
git --version
npm install -g @usebruno/cli@4.0.0 newman@6.2.2
mvn clean test
mvn package
java -cp target/gerador-docs-tests-3.0.0.jar com.microsoft.playwright.CLI install chromium
```

Instale somente o runner que for usar. Git é opcional; sua ausência não impede a execução. O JAR contém as dependências Java e o driver Playwright; o navegador é instalado separadamente no cache do usuário. Para Linux sem bibliotecas nativas: execute a CLI Playwright com `install --with-deps chromium` em etapa de provisionamento autorizada. Não execute a auditoria como root apenas para contornar bibliotecas ausentes.

NVM altera PATH no terminal. IntelliJ iniciado pelo desktop/Snap pode não herdar esse PATH. Configure nas variáveis de ambiente da Run Configuration:

```bash
export BRU_EXECUTABLE="$(command -v bru)"
export NEWMAN_EXECUTABLE="$(command -v newman)"
```

Alternativamente defina o caminho absoluto da instalação. O executor acrescenta o diretório do executável ao PATH dos processos filhos, permitindo localizar o Node instalado junto dele. `NODE_OPTIONS` é herdado sem sobrescrita; se um Node antigo realmente exigir WebCrypto, configure-o externamente. Node 22 não exigiu esse ajuste.

Linux foi validado end-to-end. macOS deve usar executáveis nativos no PATH; Windows com shims `.cmd` pode exigir um launcher nativo compatível com ProcessBuilder e não foi validado. Não há dependência de bash na aplicação Java, mas templates de CI usam agentes Linux.
