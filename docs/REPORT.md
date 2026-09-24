# Relatório e manifest

Um único template `report-template.html` atende ambos os runners. Cabeçalho azul-marinho em gradiente com empresa, título, coleção, data com timezone, ambiente e executor. A seção compacta de rastreabilidade mostra origem LOCAL/CI/CD, SO/usuário, versão, Git/commit/autor e CLEAN/DIRTY, além de campos CI disponíveis.

Resumo apresenta total de requests, aprovadas/falhas e assertions. Cada request mostra nome, URL sanitizada, badge de método/status, HTTP status + statusText, tempo em ms e tamanho em bytes, assertions com PASS/FAIL/SKIPPED, mensagens de erro, headers e bodies. Payload JSON é formatado com Jackson; texto não JSON mantém seu conteúdo textual.

Playwright Chromium gera A4, cores de fundo e footer `Página X de Y` com `pageNumber`/`totalPages`. Margem inferior 20 mm. Requests grandes podem quebrar entre páginas; linhas curtas das tabelas evitam quebra, títulos ficam junto do conteúdo seguinte. Não truncamos silenciosamente bodies. Volumes muito grandes consomem memória proporcional ao reporter + domínio + HTML + browser; limite o escopo/folder ou aumente o heap com critério.

Resultados brutos são temporários; saídas finais são PDF, domínio `.sanitized.json` e `.audit-run.json`. Este último tem schemaVersion=1, UUID, timestamp, collection/environment, PASS/FAIL, durationMs, summary, traceability, versão, nome do PDF e requests/tempos/assertions para comparação. Duração inclui execução, parsing e renderização até escrever o manifest. O nome do PDF é relativo ao diretório do manifest.

Não há assinatura digital nem prova criptográfica de imutabilidade. Git DIRTY indica que a evidência pode incluir código não commitado. Em CI, Branch/Commit vêm do provider, enquanto Workspace/Autor Git descrevem checkout local quando disponível. O artifact deve seguir a retenção e o controle de acesso da organização.
