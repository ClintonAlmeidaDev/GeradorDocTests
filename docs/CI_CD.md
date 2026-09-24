# CI/CD e artifacts

Azure DevOps é o template principal: `examples/azure-pipelines.yml`. Também há `examples/github-actions.yml`, `examples/gitlab-ci.yml` e `examples/Jenkinsfile`. São templates executáveis a adaptar ao caminho da collection e ao agente da organização; não foram publicados em contas remotas nesta entrega.

Etapas: preparar Java 21/Node 22/Maven, instalar e validar runners, compilar o JAR, provisionar Chromium, executar auditoria, publicar artifact **mesmo quando a execução retorna 1**. Não use `continueOnError` para esconder falha funcional. A execução retorna o código Java; a publicação usa `always()`/`when: always`/`post always`. Assim PDF FAIL fica disponível e job continua FAILED.

`scripts/run_ci.sh` cria diretório de execução e copia somente PDFs/manifests para `audit-artifacts/`; escreve `exit-code.txt` e devolve 0/1/2. Não inclui raw, ambientes nem JSON completo por padrão. Execute em checkout limpo/efêmero para evitar artifacts de jobs anteriores. Não acrescente `--keep-raw-results` em CI.

## Azure DevOps

Copie o YAML para a raiz ou selecione-o na definição do pipeline. Ajuste collection/environment e ambiente do runner, por exemplo:

```yaml
- bash: bash scripts/run_ci.sh --collection ./api-tests --environment HML --bruno-env HML
  displayName: API audit
```

Credenciais vêm de variable groups/secret variables/secure files da organização e do mecanismo do runner. Não escreva segredo no YAML ou passe token literal em argumentos. Para environment file, use caminho de Secure File preparado pelo pipeline. Preserve as permissões e delete o arquivo ao terminar. Agentes precisam ter acesso à API/VPN; o Java não cria esse acesso.

A rastreabilidade lê `TF_BUILD`, `BUILD_DEFINITIONNAME`, `BUILD_BUILDNUMBER`, `BUILD_BUILDID`, `BUILD_SOURCEBRANCH`, `BUILD_SOURCEVERSION`, `BUILD_REQUESTEDFOR` e `SYSTEM_PULLREQUEST_PULLREQUESTID` quando presentes. Campos ausentes são omitidos. Identidade solicitante não é autor Git e pode representar conta de serviço em execução agendada. Fonte: [variáveis oficiais Azure Pipelines](https://learn.microsoft.com/en-us/azure/devops/pipelines/build/variables?view=azure-devops).

GitHub usa GITHUB_ACTIONS/WORKFLOW/RUN_NUMBER/RUN_ID/REF_NAME/SHA/ACTOR e ref de PR; veja [variáveis GitHub](https://docs.github.com/en/actions/reference/workflows-and-actions/variables). GitLab usa CI_PIPELINE_ID, CI_COMMIT_REF_NAME/SHA, GITLAB_USER_NAME e CI_MERGE_REQUEST_IID; veja [variáveis GitLab](https://docs.gitlab.com/ci/variables/predefined_variables/). Jenkins usa JOB_NAME/BUILD_NUMBER/GIT_BRANCH/GIT_COMMIT/CHANGE_ID e BUILD_USER_ID quando fornecido pelo agente/plugins.

## Evidência em PR

O artifact do build é a fonte de evidência. Copie seu link autenticado para o PR, ou gere um Markdown local:

```bash
python3 scripts/pr_evidence.py --artifact-url 'https://dev.azure.com/ORG/PROJECT/_build/results?buildId=123&view=artifacts'
```

O script não envia comentários nem faz chamadas autenticadas. O ponto de integração é posterior à publicação: uma etapa corporativa autorizada pode usar API/CLI do provider para comentar o conteúdo de `pr-evidence.md` com suas próprias permissões. Não há token ou URL real hardcoded. Controle acesso e retenção do artifact; um PDF pode conter informações internas mesmo após sanitização.

O template Jenkins exige agente previamente provisionado com Java/Node e bibliotecas nativas do Chromium. O template GitLab usa container Linux x64 e requer rede para instalar Node. Pinagem de imagens/actions por digest/SHA e mirrors internos devem seguir a política de supply chain da empresa.
