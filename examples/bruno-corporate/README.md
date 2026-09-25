# Fluxo corporativo local e fictício

Esta OpenCollection exercita obtenção de token, propagação de variáveis, criação e consulta de pedido, headers de correlação/idempotência e respostas negativas esperadas (401/422). Todos os identificadores e segredos são fictícios. Não aponte esta fixture para uma API real.

Na raiz do repositório, inicie `python scripts/corporate_fixture.py`. O servidor escuta apenas `127.0.0.1:8765`; encerre com Ctrl+C.

Em outro terminal, entre em `examples/bruno-corporate` e execute:

```text
bru run HOMOLOGACAO -r --env LOCAL --noproxy
```

Pelo gerador, na raiz do repositório:

```text
java -jar target/gerador-docs-tests-3.0.0.jar --collection examples/bruno-corporate --folder HOMOLOGACAO --bruno-env LOCAL --environment LOCAL --diagnostics -- --noproxy
```

`LOCAL` seleciona o endpoint de loopback. `HOMOLOGACAO` é o nome da pasta do fluxo, não uma autorização para chamar homologação real. Os cinco requests devem passar, incluindo os negativos: receber 401/422 é o comportamento esperado nesses testes. A fixture não implementa um provedor OAuth real nem persistência de produção.

`scripts/integration_test.py` inicia a API automaticamente em uma porta livre, copia a collection e executa PASS/FAIL. A variante FAIL troca somente a expectativa de negócio do pedido: HTTP 201 continua válido, mas a assertion falha e o gerador deve produzir evidência com código 1.
