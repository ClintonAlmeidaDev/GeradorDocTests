# Quero testar minha collection e gerar o PDF

O GeradorDocsTests executa sua collection Bruno ou Postman e cria um PDF com os resultados. Se algum teste falhar, o PDF mostra a falha. Você não precisa saber Java, Maven ou programação para usar a distribuição ZIP.

## 1. Prepare uma vez

Você precisa de **Java 25 ou superior** e **Node 22 ou superior** instalados. A versão Node validada nesta entrega é a 22. Se não tiver esses programas, peça a instalação à sua TI. **Não é necessário instalar Maven.**

Extraia o ZIP inteiro. Não mova somente o arquivo JAR. Dentro da pasta extraída, clique na barra de endereço do Explorador de Arquivos, digite `powershell` e pressione Enter. Isso abre o PowerShell nessa pasta.

Se usa **Bruno**, copie e execute:

```powershell
.\scripts\setup.ps1 -SkipBuild -Runner bruno
```

Se usa **Postman**, execute:

```powershell
.\scripts\setup.ps1 -SkipBuild -Runner postman
```

Aguarde a mensagem `Instalacao verificada`. Esse preparo instala o executor dos testes e o navegador usado para criar PDFs. Precisa de acesso à rede para os downloads iniciais. Não abre nem executa sua collection. Nas próximas utilizações, vá direto ao passo 2.

Se o Windows bloquear scripts ou a empresa bloquear downloads, peça ajuda à TI. Não é necessário desativar proteções ou certificados.

## 2. Gere o relatório

Antes de executar, confira que o ambiente e as operações da sua collection estão autorizados. O programa executa os mesmos requests e scripts da collection, incluindo gravações e exclusões que ela contenha.

### Uso com Bruno

Copie o comando e troque o caminho e o nome do ambiente pelos seus:

```powershell
.\scripts\audit.ps1 --collection "C:\Collections\Minha API" --bruno-env "HML" --environment "HML" --output-dir ".\meus-relatorios"
```

| Trecho | O que colocar |
|---|---|
| `C:\Collections\Minha API` | A pasta principal da collection: aquela que contém `opencollection.yml` ou `bruno.json` |
| `--bruno-env "HML"` | O nome exato do ambiente salvo no Bruno. Se a collection não usa ambiente, remova esse trecho |
| `--environment "HML"` | O nome que você quer mostrar no PDF. Esse campo **não seleciona** as variáveis do Bruno |
| `.\meus-relatorios` | A pasta onde os resultados serão guardados. Pode manter como está |

**Quer executar somente uma pasta da collection?** Acrescente `--folder`:

```powershell
.\scripts\audit.ps1 --collection "C:\Collections\Minha API" --folder "HOMOLOGACAO" --bruno-env "HML" --environment "HML" --output-dir ".\meus-relatorios"
```

Esse exemplo inclui as subpastas de `HOMOLOGACAO`. Sem `--folder`, todas as pastas da collection são executadas. `HOMOLOGACAO` e `HML` são exemplos: use os nomes da sua collection.

Se você recebeu um **arquivo de ambiente**, substitua `--bruno-env "HML"` por `--environment-file "C:\Collections\Minha API\environments\HML.yml"`.

### Uso com Postman

Exporte sua collection e, se necessário, seu ambiente pelo Postman. Depois execute:

```powershell
.\scripts\audit.ps1 --collection "C:\Collections\Minha API.postman_collection.json" --environment-file "C:\Collections\HML.postman_environment.json" --environment "HML" --output-dir ".\meus-relatorios"
```

Substitua os dois caminhos pelos arquivos exportados. Se sua collection não usa arquivo de ambiente, remova o trecho `--environment-file "..."`.

## 3. Abra o resultado

Abra a pasta **meus-relatorios** dentro da pasta extraída e dê dois cliques no arquivo **PDF** mais recente.

- **PASS:** os testes foram aprovados e o relatório foi criado.
- **FAIL:** um ou mais testes falharam; o relatório foi criado com os detalhes para análise.
- **Erro técnico / código 2:** a execução não foi concluída normalmente. Leia a mensagem; pode faltar um programa, arquivo ou permissão. Um arquivo parcial não comprova sucesso.

Além do PDF, são criados dois arquivos auxiliares: `.sanitized.json`, com os dados tratados, e `.audit-run.json`, com um resumo da execução. Para ler a evidência, abra o PDF. Revise o conteúdo antes de compartilhá-lo: o mascaramento automático não garante remover todo dado sensível.

**Um status HTTP 200 ou 201 não garante PASS.** As verificações da collection também precisam passar. Uma resposta 401 ou 422 pode ser correta quando o teste espera a rejeição de uma operação inválida.

## Se precisar de ajuda

Para conferir a instalação Bruno sem executar sua API:

```powershell
.\scripts\audit.ps1 --doctor --runner bruno
```

Para Postman, troque `bruno` por `postman`. Esse diagnóstico não valida VPN, credenciais nem acesso à sua API.

Se o Bruno funciona no terminal, mas o gerador não o encontra, peça ajuda à TI com [o guia Windows](docs/WINDOWS.md). Para suporte, informe a mensagem e a versão dos programas; não envie tokens, senhas nem arquivos de ambiente com credenciais.

Se sua TI preferir executar **diretamente pelo Java**, o equivalente ao comando Bruno é:

```powershell
java -jar .\target\gerador-docs-tests-3.0.0.jar --collection "C:\Collections\Minha API" --bruno-env "HML" --environment "HML" --output-dir ".\meus-relatorios"
```

O preparo do passo 1 continua necessário. O Java direto e o script PowerShell usam o mesmo gerador.

Para experimentar sem acessar APIs reais, use a [demonstração corporativa local](examples/bruno-corporate/README.md). Ela usa apenas dados fictícios e exige Python 3 para iniciar a API de exemplo; Python não é necessário para documentar sua própria collection.

Esta entrega foi testada no **Windows 10 Pro**, com **Windows PowerShell 5.1** e **PowerShell 7**. A compatibilidade com Windows 11 é buscada, mas ainda precisa ser validada nesse sistema. Os detalhes técnicos e limites estão em [VALIDATION.md](docs/VALIDATION.md).
