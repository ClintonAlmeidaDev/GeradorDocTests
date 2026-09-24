# Segurança e tratamento de evidência

PDF sanitizado **não significa** reporter bruto seguro. Bruno/Newman podem incluir headers, cookies, tokens de auth, variáveis e bodies nos resultados. O fluxo padrão cria diretório temporário privado (0700 em POSIX), executa o reporter ali e remove arquivo/diretório no `finally`, também após falhas do parser/Chromium. Em Windows as permissões seguem as ACLs da conta; essa plataforma não foi validada.

`--keep-raw-results` copia o reporter para `<uuid>.raw.json`, aplica 0600 em POSIX e registra aviso. É opção de diagnóstico explícita, não deve ser usada nos templates de CI. `.gitignore` exclui esses outputs. Remoção normal não é apagamento criptográfico; kill -9, queda da máquina ou falta de permissão podem deixar temporários no diretório temporário do SO. Use agentes efêmeros, criptografia de disco e política de retenção.

## Cobertura

Map/List/JsonNode recursivos; JSON textual é interpretado; chaves normalizadas ignoram maiúsculas, `_` e `-`. Defaults: password, passwd, secret, clientSecret, token, accessToken, refreshToken, apiKey, authorization, proxy-authorization, x-api-key, cookie e set-cookie. A máscara é `••••••••`.

Headers em mapa e listas `{key|name,value}` são cobertos. Query params sensíveis e userinfo em URLs são ocultados. Form key=value, linhas de headers e tags XML sensíveis simples são tratados. Valores identificados em campos estruturados são removidos também de mensagens de assertion e outros textos da mesma evidência. Campos normais como accountId não são ocultados automaticamente. Acrescente `--mask-key cpf --mask-key accountNumber` conforme a classificação da empresa.

HTML, JSON normalizado e manifest recebem o domínio sanitizado. Thymeleaf escapa texto; o navegador não faz requisições de rede durante a impressão. O JSON de `--json-output` não é raw. O manifest omite bodies/headers, mas URLs, nomes, usuário e contexto ainda podem ser informação interna.

## Limites e operação

Não se pode garantir detecção universal de secrets em texto livre, XML arbitrário, conteúdo binário/base64, valores ofuscados ou identificadores pessoais sem classificação. Segredos em path de URL não têm chave confiável e exigem revisão. Valores conhecidos curtos podem causar mascaramento excessivo. Revise evidências antes de compartilhamento externo.

Scripts de collection são código executável com permissões do runner. Execute somente collections confiáveis, em conta/ambiente restritos e sem acesso desnecessário à produção. A sanitização de saída não torna seguro executar scripts não confiáveis. Não escreva tokens em argumentos: a lista de processos pode expô-los. Use ambientes/secret stores do runner e dados fictícios em HML/sandbox.

stdout/stderr livres do runner são descartados deliberadamente: `console.log(secret)` pode não conter nenhuma chave reconhecível. O Java registra início, código do executor, contagens e caminho do PDF; não imprime exception stack/parser input. Exports/reporters extras são bloqueados no pass-through para preservar a política de arquivos. Scripts das collections ainda podem escrever arquivos por conta própria; o orquestrador não oferece sandbox para essas ações.

Publique somente PDF e, opcionalmente, manifest/JSON sanitizado. Nunca use `audit-output/**` como seleção indiscriminada de artifact se houver retenção raw. Os templates usam seleção explícita e não possuem tokens ou chamadas autenticadas.
