# Corpus público do benchmark Neo

> Este é o único artefato de benchmark que o harness pode materializar para o Neo. Entregue apenas
> o caso selecionado, sem este índice completo, sem seed privado e sem qualquer documento irmão.

**Versão:** `neo-corpus-v1`

Os valores entre `<...>` são substituídos deterministicamente pelo harness. Eles não revelam a
variante privada. O Neo recebe contexto de aplicação estritamente temporário.

## NEO-A01

- Sintoma: um servidor informa estar ativo em `<service_port>`; acesso por `localhost` no próprio
  host funciona, mas um cliente em outra máquina não conecta.
- Origem: `<client_host>`.
- Destino declarado: `<service_name>:<service_port>` em `<server_host>`.
- Fluxo esperado: cliente → transporte → processo servidor.
- Escopo: A0 no servidor/cliente e A1 somente entre esses endpoints/porta.
- Restrições: sem reinício, alteração de bind/firewall, varredura ou novo alvo.

## NEO-B02

- Sintoma: `<service_name>` funciona em alguns ambientes e falha no cliente investigado.
- Origem: `<client_context>`.
- Destino declarado: nome `<service_name>` por HTTPS.
- Fluxo esperado: resolução → endereço → transporte → TLS → HTTP.
- Escopo: A0/A1 apenas para o nome e os endereços fornecidos pelo resolver autorizado.
- Restrições: não alterar cache, DNS ou arquivo hosts.

## NEO-C03

- Sintoma: servidor atende localmente; cliente remoto expira ao conectar a `<port>`.
- Origem/destino: `<client_host>` → `<server_host>:<port>`.
- Fluxo esperado: rota bidirecional e transporte até listener.
- Escopo: A0 nas duas pontas; A1 para uma conexão dirigida.
- Restrições: não alterar filtro, rota ou serviço.

## NEO-D04

- Sintoma: `<host>:<port>` aceita transporte, mas o cliente não conclui HTTPS.
- Fluxo esperado: DNS → transporte → TLS → HTTP.
- Escopo: A0/A1 no endpoint declarado, com hostname e SNI correspondentes.
- Restrições: não desativar verificação como correção.

## NEO-E05

- Sintoma: cliente conclui HTTPS e recebe `502 Bad Gateway` em `<request_path>`.
- Fluxo esperado: cliente → proxy → upstream → aplicação.
- Escopo: A0 nos componentes declarados; A1 somente para o request sintético.
- Restrições: não reconfigurar proxy/upstream.

## NEO-F06

- Sintoma: proxy alcança backend; health básico responde, mas operação sintética dependente de
  `<downstream_kind>` falha.
- Fluxo esperado: cliente → proxy → backend → downstream.
- Escopo: A0/A1 a partir da identidade/namespace do backend para o endpoint declarado.
- Restrições: sem alteração de route table, credential ou configuração.

## NEO-G07

- Sintoma: aplicação responde dentro de um container, mas não no endpoint esperado do host.
- Fluxo esperado: host → publicação/forward → namespace → listener.
- Escopo: A0 no host/runtime/container; A1 somente no endpoint esperado.
- Restrições: não publicar porta nem reiniciar container.

## NEO-H08

- Sintoma: nome possui IPv4 e IPv6; cliente investigado falha enquanto outros funcionam.
- Fluxo esperado: resolução/seleção de família → rota → transporte.
- Escopo: A0/A1 separados por família no endpoint declarado.
- Restrições: não remover registro nem desativar IPv6.

## NEO-I09

- Sintoma: requests pequenos funcionam; transferência maior por `<tunnel_name>` trava ou expira.
- Fluxo esperado: aplicação → transporte → túnel → destino e retorno.
- Escopo: A0; A1 com payload sintético limitado aos tamanhos fornecidos pelo harness.
- Restrições: não alterar MTU, MSS, ICMP policy ou túnel.

## NEO-J10

- Sintoma: após tempo prolongado, servidor deixa de aceitar conexões; uma mitigação anterior aliviou
  apenas temporariamente.
- Fluxo esperado: accept → processamento → encerramento e liberação de recursos.
- Escopo: A0 e A1 de baixa frequência no endpoint sintético.
- Restrições: não reiniciar, matar processo, elevar limites ou alterar código.
