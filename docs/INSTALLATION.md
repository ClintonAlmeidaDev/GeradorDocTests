# Instalação

Para Windows 11, PowerShell, uso sem Maven e Artifactory/Nexus, siga [Windows e rede corporativa](WINDOWS.md).

Use Java 25 (o pom compila com release 25), Maven 3.8.7 ou superior e Node 22. Os runners verificados foram Bruno CLI 4.0.0 e Newman 6.2.2. Playwright/Jackson/Thymeleaf foram mantidos nas versões existentes; somente YAML, logging e infraestrutura de testes/build foram acrescentados.

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

Linux foi validado end-to-end. O executor Windows resolve launchers npm via Node e possui testes de resolução e um job de integração nativo no CI. macOS deve usar executáveis no PATH. Para CI Windows, use `scripts/run_ci.ps1`; para Linux, `scripts/run_ci.sh`. Confira o status do CI para a validação nativa Windows.

## Selecionar o JDK 25

Instale um JDK 25, por exemplo Eclipse Temurin, e configure o ambiente antes do build:

```bash
export JAVA_HOME=/caminho/do/jdk-25
export PATH="$JAVA_HOME/bin:$PATH"
java -version
mvn -version
```

Ambos devem indicar Java 25 (ou superior). O build produz bytecode Java 25 (`--release 25`), sem recursos preview; o JAR não executa em Java 21. O Enforcer interrompe o build cedo quando o Maven usa um JDK antigo.

No IntelliJ, registre o JDK 25 em **Project Structure → SDKs**, selecione-o em **Project SDK**, use language level 25 e ajuste também **Maven → Runner → JRE** e o JDK do importador. Reimporte o pom. A seleção do SDK no projeto não instala o JDK na máquina. Use uma versão do IntelliJ com suporte a Java 25.

A migração não altera o Java padrão do sistema; outros projetos podem continuar usando seus próprios JDKs.

## Distribuição interna

Após `mvn package`, execute `python scripts/package_distribution.py`. O ZIP em `target/` contém o JAR e os scripts necessários ao usuário final, que não precisa de Maven. Transfira o ZIP pelo canal aprovado pela empresa; ele não inclui Node nem Chromium. O código é licenciado sob MIT (veja `LICENSE`); preserve a licença na distribuição.
