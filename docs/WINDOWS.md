# Windows 10/11 e rede corporativa

Validação nativa desta rodada: **Windows 10 Pro 22H2, build 19045.3448**. O código busca compatibilidade com Windows 11, mas os resultados desta máquina não substituem a homologação nesse sistema. Consulte [os resultados e limites](VALIDATION.md).

## Usar sem Maven

Instale JDK 25 e Node 22 pelos canais aprovados pela empresa. Extraia o ZIP de distribuição inteiro em uma pasta local. O ZIP contém JAR, scripts, licença e documentação; Maven e Git são necessários somente para quem compila o código.

Abra PowerShell na pasta extraída:

```powershell
.\scripts\setup.ps1 -SkipBuild -Runner bruno
.\scripts\audit.ps1 --doctor --runner bruno
.\scripts\audit.ps1 --collection "C:\Collections\Minha coleção" --folder "HOMOLOGACAO" --bruno-env "HML" --environment "HML" --output-dir "C:\Evidencias\Minha API" --diagnostics
$LASTEXITCODE
```

Use o nome real do ambiente salvo no Bruno em `--bruno-env`. Alternativamente, use `--environment-file "C:\Collections\Minha coleção\environments\HML.yml"`. `--environment` é somente o rótulo do relatório e não seleciona variáveis do runner. Não é necessário informar ambos os seletores de ambiente.

A raiz passada em `--collection` deve conter `opencollection.yml` ou `bruno.json`. O diretório de execução continua sendo essa raiz, mesmo com `--folder HOMOLOGACAO`. A execução é recursiva: inclui as subpastas da pasta selecionada. Sem `--folder`, todas as pastas da collection são executadas; isso pode incluir LOCAL e GATEWAY. Caminhos com espaços devem ser colocados entre aspas. No PowerShell, não use a barra `\` de continuação de linha do Bash.

Os scripts suportam Windows PowerShell 5.1 e PowerShell 7. O setup verifica Java 25+ e Node 22+, instala somente os runners escolhidos que ainda não estão na versão requerida, instala Chromium e faz o diagnóstico local. Se um pacote constar como instalado mas estiver incompleto, o diagnóstico falha; reinstale esse pacote no mesmo prefixo npm. `--doctor` não executa a collection: verifica a versão do runner, a escrita na saída e a geração de um PDF temporário (removido ao terminar). O diagnóstico aguarda até 60 segundos pela versão do runner, pois a inicialização nativa do Newman excedeu 15 segundos nesta máquina. Ele não comprova conectividade com sua API nem validade das credenciais.

A geração de evidências não baixa navegadores: se Chromium estiver ausente, retorna 2 com orientação de instalação. O ZIP inclui a [collection corporativa fictícia](../examples/bruno-corporate/README.md) e sua API de loopback. Python é necessário somente para iniciar essa API de demonstração; o uso normal do gerador continua exigindo Java, Node, runner e Chromium.

Se a política do PowerShell bloquear scripts, siga a orientação da TI. A execução Java direta também está disponível:

```powershell
java -jar .\target\gerador-docs-tests-3.0.0.jar --doctor --runner bruno
java -jar .\target\gerador-docs-tests-3.0.0.jar --collection "C:\Collections\Minha coleção" --folder HOMOLOGACAO --bruno-env HML --environment HML --diagnostics
```

## Compilar na empresa

Com Maven 3.8.7+ instalado, confira `java -version` e `mvn -version`: ambos precisam usar JDK 25 ou superior. Na cópia do repositório:

```powershell
.\scripts\setup.ps1 -Runner bruno
```

Se o Maven tentar acessar Central diretamente e receber 403, utilize o arquivo de settings aprovado pela empresa:

```powershell
.\scripts\setup.ps1 -Runner bruno -SettingsPath "C:\Configuracoes\settings.xml"
```

Há um modelo em `examples/maven-settings.xml`, com URL e credenciais por variáveis de ambiente. O mirror tem `mirrorOf=*`; seu Artifactory/Nexus precisa disponibilizar dependências **e plugins Maven**. O `server.id` precisa corresponder ao `mirror.id`. Não é necessário alterar o `pom.xml` para um endereço de uma empresa específica nem substituir o settings global. Veja a [documentação oficial de mirrors do Maven](https://maven.apache.org/guides/mini/guide-mirror-settings.html).

Maven, npm e Chromium usam mecanismos de download distintos. Resolver o mirror Maven não configura automaticamente o registry npm, proxy ou certificados do Node/Playwright. Use o registry e a CA aprovados pela TI (`NODE_EXTRA_CA_CERTS` para Node, quando aplicável); o Chromium pode usar `HTTPS_PROXY` e `PLAYWRIGHT_DOWNLOAD_HOST`. Não desative TLS para contornar erros de certificado.

## Bruno funciona no terminal, mas não no Java/IntelliJ

O executor procura `bru` no PATH e, no Windows, também em `%APPDATA%\npm`. Para os launchers npm `.cmd`/`.ps1`, resolve o `bin` do pacote e executa diretamente com Node, sem shell. Isso evita interpretar espaços, `&` e `%` como comandos. A instalação npm precisa estar completa: não copie somente o arquivo `bru.cmd`. A estrutura do prefixo está documentada pelo [npm](https://docs.npmjs.com/cli/v8/configuring-npm/folders).

Se necessário, configure os caminhos explicitamente (inclusive na Run Configuration do IntelliJ):

```powershell
$env:BRU_EXECUTABLE = "$env:APPDATA\npm\bru.cmd"
$env:NODE_EXECUTABLE = "C:\Program Files\nodejs\node.exe"
.\scripts\audit.ps1 --doctor --runner bruno
```

Para Newman, use `NEWMAN_EXECUTABLE`. Reabra o terminal/IDE após instalar Node. `--diagnostics` registra launcher, diretório de execução, reporter esperado e estrutura do comando, ocultando valores de argumentos adicionais. Caminhos locais podem aparecer: revise o log antes de compartilhá-lo.

## Resultado e falhas

- **0:** execução aprovada; PDF, JSON sanitizado e manifesto gerados.
- **1:** execução com falhas funcionais; as evidências também são geradas quando há reporter válido.
- **2:** erro técnico (instalação, arquivo, runner sem reporter, PDF etc.); leia a mensagem e execute o diagnóstico.

A saída bruta de scripts do Bruno não é publicada, pois pode conter tokens. Erros conhecidos são traduzidos em mensagens fixas: argumento desconhecido, módulo ausente, acesso negado, ambiente, certificado ou parsing. Um erro não reconhecido pode exigir investigação local pelo comando direto do Bruno; revise dados sensíveis antes de compartilhar qualquer saída. Um exit code 1 do Bruno sozinho não prova que o reporter foi criado.

No Windows 10 foram executados runners reais, Chromium, resolução de launchers npm e scripts PowerShell. Linux possui regressões e workflow próprios; não foi executado nativamente nesta rodada. Windows 11 e a collection corporativa real continuam exigindo homologação específica. Veja [VALIDATION.md](VALIDATION.md).

Argumentos do Java e do wrapper PowerShell são enviados ao executável nativo sem shell, preservando espaços, acentos, `&`, `%`, aspas internas e valores vazios no pass-through. A configuração opcional do JDK `jdk.lang.Process.allowAmbiguousCommands=false` tem uma restrição adicional: valores cercados por aspas literais são rejeitados com orientação para arquivo de ambiente, evitando alterá-los silenciosamente.
