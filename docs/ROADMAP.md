# Escopo entregue e próximos passos

- V1.0: execução comum Bruno/Newman, CLI genérica, sanitização, PDF e exit codes; exemplos PASS/FAIL e teste local real.
- V1.1: contexto SO/Git/CI, CLEAN/DIRTY, seção de rastreabilidade e versão de build.
- V2: bootstrap mínimo OpenAPI 3 JSON/YAML e pass-through para ambientes/collections reais. Não é gerador de testes funcionais nem validador integral de schemas.
- V3: manifests sem banco, comparação e templates Azure/GitHub/GitLab/Jenkins. Publicação remota depende da configuração/credenciais do CI e não foi executada nesta entrega.

Futuro: validar plataformas adicionais, ampliar formatos OpenAPI, IDs persistentes de requests para comparação entre reordenações, política corporativa de retenção/assinatura, testes de carga do gerador e revisão de dependências antes de distribuição externa. Não há banco, servidor web, dashboard, Kafka, Redis ou autenticação própria.
