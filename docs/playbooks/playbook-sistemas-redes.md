# Playbook — Fundamentos de sistemas, redes e comunicação distribuída

**Autor:** Cristóvão Augusto

## Como usar

Referência seletiva para o Neo. Consulte a seção disparada pela hipótese; não execute o documento
como checklist linear. O método prescritivo está em
[`investigar-sistemas-redes`](../skills/investigar-sistemas-redes.md). Ferramentas citadas são
adapters possíveis para capacidades permanentes, não dependências do agente.

## Índice por sintoma

| Sintoma ou hipótese | Seções |
|---|---|
| processo “rodando”, porta inacessível | 1, 2, 3, 12 |
| nome não resolve ou resolve diferente | 6, 12 |
| timeout/reset/refused | 3, 4, 5, 12 |
| TCP funciona, TLS falha | 7, 12 |
| TLS funciona, HTTP falha | 8, 9, 12 |
| container/pod funciona só internamente | 2, 10, 12 |
| falha parcial por tamanho, perda ou latência | 4, 5, 12 |
| conexões acumulam ou esgotam recursos | 1, 3, 9, 12 |
| caminho até internet/cloud | 5, 10 |
| escolher adapter por plataforma | 11 |
| avaliar fraqueza/superfície defensiva | 13 |

## 1. Processo, runtime e sistema operacional

Um processo não “possui uma porta” diretamente. Ele mantém um descriptor/handle que referencia um
objeto socket do kernel. O runtime ou framework pode criar e gerenciar esse socket, mas o estado
observável continua distribuído entre processo, kernel, namespace e rede.

### Fluxo conceitual de servidor

```text
socket()  → cria endpoint no kernel
bind()    → associa protocolo + endereço local + porta
listen()  → transforma socket TCP em listener e cria filas/backlog
accept()  → retira conexão estabelecida da fila e cria novo socket conectado
recv()    → copia/expõe bytes recebidos ao processo
send()    → entrega bytes ao kernel para transporte
close()   → libera referência; o protocolo pode continuar encerramento no kernel
```

### Fluxo conceitual de cliente

```text
socket() → connect() → porta efêmera/endereço local escolhidos → handshake/associação → send/recv
```

Para UDP, `sendto`/`recvfrom` preservam o endpoint por datagrama; `connect` em UDP pode fixar peer e
facilitar entrega de erros, mas não cria handshake equivalente ao TCP.

### O que observar

- PID, parent, threads, estado, usuário/identidade e cgroup/job;
- descriptors/handles, limites soft/hard e contagem em uso;
- sockets herdados, ativação por supervisor e múltiplos workers;
- CPU, memória, bloqueio, fila de execução e pressão de I/O;
- namespace/pilha de rede realmente usados pelo processo;
- logs correlacionados com o mesmo relógio e request/connection id.

Processo existente não prova que inicialização terminou. Health check positivo não prova que o
socket externo, a rota ou dependências estão saudáveis, a menos que o próprio contrato do check as
exerça e isso esteja documentado.

### Syscalls, buffers, interrupções e scheduling

Aplicações normalmente entram no kernel por uma API/runtime que termina em system calls. O nome e
a forma exata variam, mas a transição conceitual preserva estes pontos:

1. o processo prepara endereço, buffers e flags em user space;
2. a system call valida handles/descriptors, permissões, memória e estado do socket;
3. o kernel copia ou referencia dados entre user space e buffers/filas do socket;
4. a pilha de protocolos segmenta/encapsula, escolhe rota e entrega ao driver/interface;
5. hardware e driver sinalizam transmissão/recepção por interrupções, polling ou mecanismos
   híbridos; processamento pode ser adiado/agregado;
6. o scheduler decide quando threads/processos voltam a executar; wakeups, afinidade, prioridades,
   throttling e pressão de CPU alteram latência;
7. no recebimento, bytes sobem por driver, pilha, fila/socket e chamada bloqueante ou notificação de
   prontidão até o processo consumi-los.

Buffer “disponível” em uma camada não prova capacidade nas demais. Podem saturar ring da NIC, fila
de backlog, receive/send buffer, fila do runtime, thread pool ou downstream. Drops, backpressure e
latência precisam ser localizados no ponto em que aparecem.

### Blocking, nonblocking e readiness/completion

- Em I/O bloqueante, uma thread pode dormir até progresso, timeout ou sinal; isso não significa que
  o kernel parou de receber.
- Em nonblocking, chamadas retornam quando não podem progredir; o processo usa readiness
  (`select`/`poll`/`epoll`/`kqueue`) ou completion (IOCP e modelos equivalentes) para retomar.
- `io_uring` e mecanismos assíncronos podem submeter operações em lote e receber completions; a
  semântica de socket/protocolo continua.
- Edge-triggered/one-shot e registros incorretos podem deixar bytes na fila sem novo wakeup; isso é
  diferente de pacote perdido.
- Pausa do runtime, event loop bloqueado e thread pool esgotado podem parecer falha de rede enquanto
  o kernel mantém socket e buffers ativos.

## 2. Socket, bind e portas

O identificador relevante é conceitualmente:

```text
namespace + família IP + protocolo + endereço local + porta local
```

Uma conexão TCP acrescenta endereço e porta remotos. Sockets podem coexistir conforme wildcard,
dual-stack, regras de reuse e implementação do sistema operacional.

### Endereços de bind

| Bind | Significado mínimo |
|---|---|
| `127.0.0.1` | loopback IPv4; não recebe diretamente tráfego destinado ao IP da interface |
| `0.0.0.0` | wildcard IPv4; aceita destinos locais IPv4 compatíveis com regras do SO |
| `::1` | loopback IPv6 |
| `::` | wildcard IPv6; aceitar IPv4-mapped depende de SO/configuração |
| IP específico | somente o endereço/interface correspondente, sujeito à existência e namespace |

“Rodando em 8080” precisa ser decomposto em protocolo, família, namespace, bind e processo. Porta
TCP 8080 e UDP 8080 são espaços distintos. Porta publicada no host não é sinônimo de listener no
container, e listener no container não prova publicação no host.

### Backlog e filas

Há distinção entre tentativas ainda no handshake e conexões estabelecidas aguardando `accept`;
detalhes e limites variam por kernel. Saturação pode produzir timeout, retransmissão ou reset mesmo
com `LISTEN` presente. Meça filas e drops em vez de inferir pela configuração nominal.

### Encerramento, ownership e liberação

Em TCP, fechar um descriptor não apaga instantaneamente todo estado do protocolo:

```text
aplicação A close/shutdown(write)
→ kernel A envia FIN depois dos bytes pendentes
→ kernel B reconhece e entrega EOF à aplicação B
→ aplicação B fecha sua direção e kernel B envia FIN
→ kernel A reconhece
→ estado terminal, incluindo TIME_WAIT na ponta que encerrou ativamente quando aplicável
```

- `shutdown` pode encerrar apenas leitura ou escrita; `close` reduz referência e o comportamento
  final depende de outras referências, dados pendentes e opções do socket.
- `CLOSE_WAIT` significa que o FIN remoto já foi recebido, mas a aplicação local ainda não fechou;
  investigar ownership e caminho de erro local.
- `FIN_WAIT` indica encerramento iniciado localmente ainda não concluído.
- RST encerra abruptamente e pode descartar dados não consumidos; identifique quem o emitiu.
- `TIME_WAIT` protege contra segmentos antigos e confirmação final; não é vazamento por definição.
- Em HTTP keep-alive/pools, o código pode “terminar request” sem fechar conexão; responsabilidade de
  retorno ao pool, descarte e timeout precisa ser observada.

Exaustão pode vir de descriptors/handles, portas efêmeras, conntrack, filas ou pools, cada um com
sintoma e dono diferentes.

## 3. TCP, UDP e QUIC

### TCP

Estabelecimento básico:

```text
cliente → servidor  SYN seq=x
servidor → cliente  SYN-ACK seq=y ack=x+1
cliente → servidor  ACK ack=y+1
```

Sequence numbers ordenam bytes; ACKs confirmam o próximo byte esperado; janela anuncia capacidade
de recepção. Retransmissão não prova perda física: pode decorrer de congestionamento, reorder,
filtragem, host sobrecarregado ou captura incompleta.

Estados diagnósticos:

| Estado | Interpretação inicial | Próxima discriminação |
|---|---|---|
| `LISTEN` | kernel aceita novas conexões no endpoint | bind/namespace, fila, teste local/remoto |
| `SYN_SENT` | SYN enviado; estabelecimento ainda não completou | captura nas duas pontas, rota de retorno/filtro |
| `SYN_RECV` | servidor recebeu SYN e respondeu/aguarda ACK | retorno, backlog, perda |
| `ESTABLISHED` | handshake completou | TLS/protocolo/aplicação e fluxo de bytes |
| `TIME_WAIT` | ponta ativa preserva estado contra segmentos antigos | taxa/portas efêmeras/reuso; geralmente esperado |
| `CLOSE_WAIT` | FIN remoto recebido; processo local ainda não fechou | ownership do socket e caminho de close no processo |
| `FIN_WAIT_*` | encerramento local em andamento | resposta remota, timeout e captura |

`connection refused` normalmente indica rejeição ativa no endpoint alcançado, frequentemente RST,
mas ainda exige confirmar destino e ponto de resposta. Timeout indica ausência de conclusão no
prazo, não identifica sozinho firewall.

### UDP

Sem handshake, ausência de resposta é ambígua. Correlacione envio/recepção, ICMP errors, processo
servidor e protocolo de aplicação. NAT e conntrack mantêm estado temporário apesar de UDP ser
connectionless.

### QUIC

QUIC opera sobre UDP e integra transporte seguro; HTTP/3 usa QUIC. Diagnosticar exige separar
seleção/negociação de HTTP/3, alcance UDP, versão/handshake QUIC, TLS integrado e possível fallback
para HTTP/2/TCP. Sucesso do fallback pode esconder bloqueio de UDP.

### Modelo cliente-servidor: ida e volta completas

Exemplo de HTTPS tradicional, com os pontos que precisam ser correlacionados:

```text
CLIENTE
aplicação constrói URL/request
→ runtime pede resolução de nome
→ resolver retorna A/AAAA
→ kernel cria socket e escolhe IP/porta efêmera de origem
→ route lookup escolhe interface/next hop
→ ARP/NDP resolve vizinho
→ TCP SYN atravessa LAN/gateway/NAT/ISP/Internet
→ host servidor recebe na interface
→ filtros/conntrack aceitam
→ kernel encontra listener pelo endpoint
→ SYN-ACK volta e handshake completa
→ cliente envia ClientHello TLS com SNI/ALPN
→ servidor escolhe identidade/parâmetros e envia cadeia
→ cliente valida hostname, tempo, cadeia e trust
→ canal TLS fica disponível
→ cliente envia bytes HTTP protegidos
→ servidor accept/read ou evento de prontidão entrega ao runtime
→ parser HTTP cria request e seleciona handler
→ aplicação processa e pode chamar dependências
→ servidor serializa status/headers/body
→ write/send entrega ao kernel/TLS/TCP
→ ACKs, flow/congestion control e retransmissões transportam a resposta
→ cliente recebe, valida/decripta TLS, interpreta HTTP e entrega à aplicação
```

O retorno não é mera inversão gráfica: pode usar rota assimétrica, outra tradução/policy, filas e
buffers diferentes. Proxies dividem o fluxo em conexões independentes; um request id de aplicação
não substitui a correlação pelos endpoints/horários de cada perna.

No HTTP/3, substitua TCP + handshake TLS separado por QUIC sobre UDP com TLS 1.3 integrado:

```text
HTTP/3 → QUIC + TLS → UDP → IP → enlace
```

Diagnosticar “HTTPS” exige registrar qual stack foi realmente negociada; sucesso em HTTP/2/TCP por
fallback não confirma UDP/QUIC.

## 4. IP, enlace, MTU e vizinhança

IPv4 e IPv6 são caminhos independentes. Registre qual família a aplicação escolheu, não apenas que
o nome possui ambos os registros.

### Transição local

1. processo entrega dados ao socket;
2. pilha escolhe endereço de origem e rota;
3. host resolve next hop por ARP (IPv4) ou NDP (IPv6), quando on-link;
4. interface transmite frames/pacotes;
5. retorno percorre rota possivelmente diferente.

### MTU e MSS

MTU limita pacote no enlace; MSS limita payload TCP anunciado. Path MTU Discovery depende de sinais
como ICMP Packet Too Big/Fragmentation Needed. Bloqueio desses sinais ou túnel com overhead pode
gerar “small packets work, large transfers stall”. Teste tamanho de forma controlada, compare TCP
MSS, retransmissões e ICMP, e não conclua MTU apenas por uma página grande falhar.

### ICMP

ICMP carrega diagnóstico e controle, não só echo. Echo bloqueado não prova host indisponível;
resposta a echo não prova porta ou aplicação disponível. Mensagens de unreachable/too-big podem ser
evidência mais relevante que echo.

## 5. Rota, NAT e caminho até a internet

### Fluxo típico

```text
aplicação → socket/kernel → interface → LAN/Wi-Fi → gateway padrão
→ NAT/PAT/CGNAT quando aplicável → rede de acesso do ISP → backbone
→ trânsito/peering/IXP → ASN de destino → rede/host de destino
```

No destino, o fluxo sobe pela interface, driver, pilha IP, filtro/NAT local, socket e processo. A
resposta percorre a mesma sequência conceitual ao contrário, mas a rota física/política pode ser
outra:

```text
processo remoto → socket/kernel remoto → interface/rede de destino
→ peering/trânsito/Internet → ISP do cliente → CGNAT/NAT/gateway
→ LAN/Wi-Fi → interface/kernel/socket → processo cliente
```

Para conexões de entrada em rede doméstica/corporativa, múltiplas camadas de NAT, ausência de
port-forward, endereço privado, CGNAT ou filtro impedem alcançar o host mesmo que o listener local
esteja correto. Para saída, estado NAT/conntrack e rota de retorno são tão necessários quanto ida.

Tabela de rota escolhe next hop, interface e origem segundo prefixo, métrica/política e regras do
SO. Deve existir caminho de ida e de retorno; rota assimétrica é válida, mas muda captura, filtros
stateful e interpretação.

### NAT/PAT/conntrack

NAT traduz endereços; PAT também portas. A tradução cria estado e timeouts. Exaustão de portas,
colisão, retorno fora do dispositivo stateful ou regra ausente pode falhar seletivamente. CGNAT
adiciona tradução no ISP e normalmente impede entrada não solicitada sem mecanismo adicional.

### Traceroute

Revela respostas de alguns hops para TTL/hop limit expirado; não mostra necessariamente o caminho
de retorno, e silêncio de hop pode ser política de resposta. Mudança de caminho não prova falha;
correlacione com perda/latência no destino e protocolo relevante.

### BGP, peering e trânsito

BGP propaga alcançabilidade entre ASNs segundo política. Para diagnóstico de aplicação, basta saber
que anúncio, seleção e propagação podem tornar prefixos inacessíveis ou subótimos. Só escale para
essa camada depois de provar que a falha ultrapassa host, rede local e provedor imediato.

## 6. DNS

### Fluxo conceitual

```text
aplicação → API/resolver do runtime → resolver do SO → hosts/cache local
→ resolver configurado/recursivo → root → TLD → autoritativo → registro → cache → aplicação
```

Implementações podem usar cache próprio, DoH/DoT, sidecar, DNS corporativo, container ou cluster e
desviar desse fluxo. Descubra o resolver realmente usado pela aplicação.

### Registros

| Tipo | Função diagnóstica |
|---|---|
| A / AAAA | endereço IPv4 / IPv6 |
| CNAME | alias; exige resolução do alvo |
| NS | servidores autoritativos da zona |
| MX | roteamento de e-mail, com preferência |
| TXT | metadados textuais e políticas específicas |
| SRV | serviço, protocolo, prioridade, peso e porta |

TTL controla cache positivo; respostas negativas também podem ser cacheadas. Split DNS faz o
mesmo nome responder diferente conforme resolver/rede. `/etc/hosts` e equivalentes podem vencer ou
participar conforme a ordem do resolver.

### Separação obrigatória

1. consulta produziu qual resposta, por qual servidor e família?
2. a aplicação usou essa mesma resposta/cache?
3. o IP retornado é o esperado para aquele contexto?
4. há rota e transporte até esse IP?

Resolver corretamente não prova conectividade; conectar ao IP não prova que DNS, SNI ou roteamento
virtual por hostname estão corretos.

## 7. TLS

TLS é uma etapa distinta do transporte. Em TCP, o handshake TLS ocorre depois do estabelecimento;
em QUIC, TLS 1.3 é integrado ao transporte, mas hostname, trust e negociação continuam relevantes.

Observe:

- SNI enviado e virtual host/certificado selecionado;
- cadeia leaf → intermediária(s) → raiz confiável;
- validade temporal, hostname/SAN, assinatura e trust store do cliente;
- versões/ciphers negociados quando relevantes;
- ALPN (`http/1.1`, `h2`, `h3` etc.);
- exigência e validação de certificado cliente em mTLS;
- sessão retomada versus handshake completo;
- proxy que termina TLS e abre nova conexão upstream.

Sucesso com verificação desabilitada prova no máximo capacidade de negociar naquele teste; não
prova validade do certificado. Sucesso por IP sem SNI pode testar um virtual host diferente.

## 8. HTTP e protocolos de aplicação

```text
HTTP/1.x ou HTTP/2 sobre HTTPS → TLS → TCP → IP
HTTP/1.x ou HTTP/2 sem TLS → TCP → IP
HTTP/3 sobre HTTPS → QUIC com TLS 1.3 → UDP → IP
```

HTTP não abre porta; o processo/runtime abre um socket e interpreta bytes como HTTP.
HTTPS não é outro protocolo de aplicação que “abre a 443”: é HTTP protegido por TLS sobre um
transporte e um endpoint que, por convenção, frequentemente usa 443. Virtual hosting pode selecionar
certificado por SNI e aplicação por `Host`/`:authority`; testar um sem o outro pode alcançar destinos
diferentes.

Observe método, target, `Host`/`:authority`, headers, body, status, redirects, compressão, cache,
chunking/framing, reuso de conexão e timings. Em HTTP/2, streams compartilham conexão; em HTTP/3,
streams QUIC reduzem head-of-line entre streams, mas não eliminam perda ou bloqueio UDP.

### Códigos como localização, não causa final

- 4xx prova que alguma camada HTTP respondeu; o significado depende do emissor e contrato.
- 502/504 frequentemente vêm de proxy/gateway e apontam para upstream, mas exigem logs/timings para
  distinguir connect, TLS, timeout e resposta inválida.
- redirect prova resposta e nova localização; cliente pode falhar no próximo destino.
- CORS é política aplicada por user agent após/ao redor da troca HTTP; falha de `curl` ou TCP não é
  CORS, e resposta bloqueada pelo browser pode ter chegado pela rede.

Streaming, SSE e WebSocket exigem observar upgrade/framing, timeouts, buffering e intermediários.
Um request curto bem-sucedido não prova que conexão longa seja preservada.

## 9. Servidores e concorrência

Todo servidor web reduz-se conceitualmente a:

```text
listener = socket(família, tipo, protocolo)
bind(listener, endereço, porta)
listen(listener, backlog)

loop:
    conn, peer = accept(listener)
    bytes = read/recv(conn)
    request = parse(bytes)
    response = process(request)
    output = serialize(response)
    write/send(conn, output)
    reutilizar, shutdown ou close(conn) conforme protocolo/política
```

`accept` cria/entrega um socket conectado diferente do listener. Cada conexão tem endpoints,
buffers, estado e ciclo próprios. TLS pode ocorrer no mesmo processo ou em proxy frontal. HTTP
keep-alive permite várias requests na conexão; HTTP/2 multiplexa streams; HTTP/3 usa conexão QUIC.

### Modelos de execução

O esqueleto permanece mesmo usando:

- **thread por conexão:** simples, mas threads, stacks e scheduling podem limitar escala;
- **thread pool:** accept/dispatcher entrega trabalho a conjunto limitado; fila pode saturar;
- **process pool:** isolamento e paralelismo, com custo de IPC/memória e estratégia de listener;
- **event loop/nonblocking:** poucos executores multiplexam sockets prontos; handler bloqueante trava
  outras conexões;
- **`epoll`/`kqueue`:** notificam readiness; flags edge/level/one-shot mudam obrigação de drenar e
  registrar novamente;
- **IOCP:** entrega completions de operações assíncronas no Windows, não apenas readiness;
- **`io_uring`:** submissão/completion assíncronas em kernels compatíveis, com semântica e limites
  próprios;
- **múltiplos workers/listeners:** compartilhamento/reuse e distribuição podem criar desequilíbrio;
- **proxy frontal:** termina downstream e abre/poola upstream, criando duas pernas diagnósticas.

### Onde um servidor aparentemente “escutando” falha

- fila de handshake ou accept saturada;
- processo não chama `accept`/não drena eventos;
- thread/event loop bloqueado, pool esgotado ou scheduler/throttling atrasando execução;
- buffers cheios e backpressure não propagado;
- parser esperando framing/body adicional;
- TLS consumindo CPU ou esperando certificado/OCSP/downstream;
- handler bloqueado em lock, disco, fila, banco ou serviço remoto;
- resposta escrita parcialmente, não flushada ou descartada por close/reset;
- descriptor vazado, limite de processo/host ou portas efêmeras/conntrack esgotadas.

Diagnóstico de saturação correlaciona fila de accept, conexões, descriptors, threads/tasks, event
loop lag, buffers, CPU, memória, pauses e downstream. `CLOSE_WAIT` acumulado aponta para fechamento
local não concluído; `TIME_WAIT` elevado pode ser efeito normal de taxa/estratégia de conexão antes
de ser causa de exaustão.

## 10. Infraestrutura moderna reduzida aos fundamentos

### Containers

Pergunte sempre: em qual network namespace está o processo? Qual veth/bridge/overlay liga ao host?
Existe listener interno? Há publicação/forward no host? O retorno passa pelo mesmo conntrack? Nome e
IP vistos dentro e fora podem diferir.

### Kubernetes e orquestradores

Um Service/Ingress não é explicação suficiente. Localize:

- endereço virtual/DNS e porta do serviço;
- endpoints/backends selecionados;
- regra/tabela/proxy/eBPF que encaminha;
- rede do pod, policy, namespace e readiness;
- processo/socket que finalmente recebe;
- caminho de retorno e SNAT quando existente.

### Proxies, meshes e load balancers

Cada proxy cria duas pernas potencialmente independentes: downstream e upstream. TLS, protocolo,
timeouts, pools, retries, circuit breaker, health e DNS podem divergir entre elas. Observe ambas.

### Cloud, VPC/VNet, VPN e CDN

Reduza security groups/NACLs/firewalls, subnets, route tables, gateways, private endpoints, tunnels
e peering a: IP selecionado, regra aplicada, next hop, tradução, encapsulamento, estado e retorno.
Plano de controle declarando recurso “healthy” não substitui teste no plano de dados.

## 11. Mapa de capacidades e adapters

Descubra disponibilidade e semântica antes de usar. `netstat` e equivalentes variam; APIs e GUIs
podem ser melhores quando preservam o mesmo fato.

| Capacidade permanente | Linux / Unix | Windows | macOS | Containers / cluster / cloud |
|---|---|---|---|---|
| processo/thread/recurso | `ps`, `/proc`, `top`, `pidstat`, `lsof`, `strace` | Task Manager, Resource Monitor, `Get-Process`, Process Explorer, Procmon | `ps`, Activity Monitor, `lsof`, `dtruss` conforme permissão | runtime/CRI APIs, métricas, `kubectl top`, console/API do provedor |
| socket/listener/estado | `ss`, `netstat`, `lsof`, `/proc/net` | `Get-NetTCPConnection`, `Get-NetUDPEndpoint`, `netstat`, TCPView | `lsof`, `netstat` | `nsenter`, `docker exec`, `kubectl exec`, eBPF/telemetria disponível |
| interface/endereço | `ip address`, `ifconfig` | `Get-NetIPConfiguration`, `ipconfig` | `ifconfig`, `networksetup` | inspect do runtime/CNI, APIs de interface/VPC |
| vizinhança ARP/NDP | `ip neigh`, `arp`, `ndisc6` | `Get-NetNeighbor`, `arp` | `arp`, `ndp` | namespace correto, appliance/API virtual |
| rota/política | `ip route`, `ip rule`, `route` | `Get-NetRoute`, `route print` | `route`, `netstat -rn` | route tables, CNI, VPC/VNet e appliances |
| DNS | `dig`, `kdig`, `host`, `resolvectl`, `getent` | `Resolve-DnsName`, `nslookup`, cache DNS | `dig`, `scutil --dns`, `dscacheutil` | DNS do runtime/cluster, API/console autoritativa |
| conexão TCP/UDP | `nc`, `socat`, bash/socket, scripts limitados | `Test-NetConnection`, PowerShell/.NET sockets | `nc`, scripts limitados | exec/ephemeral debug container autorizado, health probes |
| HTTP e timings | `curl`, `wget`, clientes/scripts de protocolo | `curl`, `Invoke-WebRequest`, `Invoke-RestMethod` | `curl` | cliente dentro de cada hop/namespace, logs de proxy/LB |
| TLS | `openssl s_client`, `gnutls-cli`, `curl` | `openssl` quando instalado, PowerShell/.NET, ferramentas do certificado | `openssl`, `security`, `curl` | logs/config de terminadores, APIs de certificado/LB |
| pacote | `tcpdump`, `tshark`, Wireshark, eBPF | `pktmon`, `netsh trace`, Wireshark | `tcpdump`, Wireshark | captura no host/pod/appliance, flow logs; respeitar limites |
| caminho/perda | `tracepath`, `traceroute`, `mtr`, `ping` | `tracert`, `pathping`, `Test-NetConnection` | `traceroute`, `ping` | reachability analyzer/flow logs como evidência complementar |
| firewall/NAT/conntrack | `nft`, `iptables`, `ufw`, `firewall-cmd`, `conntrack` | Windows Defender Firewall/`Get-NetFirewallRule` | `pfctl` | policies, SG/NACL, LB, NAT gateway, flow logs |
| correlação | journal/logs, métricas, OpenTelemetry/APM disponíveis | Event Viewer/ETW, logs, métricas | unified log, métricas | logs/métricas/traces de runtime, proxy, LB e aplicação |

### Regras de adapter

1. Use a ferramenta já disponível e mais direta para o fato.
2. Registre versão se flags, formato ou interpretação dependerem dela.
3. Evite instalar dependência para uma observação que API nativa já fornece.
4. Não use parser frágil como única prova se a saída estruturada existe.
5. Ferramenta de cloud ou plano de controle é evidência indireta do plano de dados até teste real.
6. Captura em GUI e CLI são equivalentes somente se ponto, filtro, relógio e perda forem conhecidos.

## 12. Padrões de discriminação

| Sintoma | Teste discriminatório inicial | Interpretação mínima |
|---|---|---|
| “serviço em 8080 inacessível” | listener com namespace/bind + teste loopback e interface | separa processo/socket/bind de caminho externo |
| nome falha | consultar resolver usado e comparar acesso ao IP esperado | separa resolução de alcance |
| timeout TCP | estados/captura nas duas pontas e rota de retorno | localiza último ponto observado sem nomear filtro prematuramente |
| refused | identificar emissor do RST/erro e listener no destino | separa destino errado/sem listener/rejeição intermediária |
| TLS falha | conectar TCP e executar handshake com SNI/hostname/ALPN explícitos | separa transporte de trust/identidade/negociação |
| HTTP 502 | correlacionar proxy e upstream, connect/TLS/TTFB | separa perna cliente-proxy da proxy-upstream |
| container só interno | listener no namespace + bind + publicação/forward do host | separa socket interno de exposição |
| IPv6 falha | registrar AAAA e família escolhida; testar A e AAAA separadamente | separa seleção de família de falha geral |
| payload grande trava | variar tamanho e observar MSS/ICMP/retransmissão | sustenta hipótese MTU sem confundir com aplicação |
| `CLOSE_WAIT` cresce | mapear sockets ao PID/código de fechamento e peer FIN | localiza responsabilidade de `close` na ponta local |

## 13. Superfícies defensivas por camada

Esta seção orienta **identificação defensiva** durante A0/A1. Não autoriza exploração, payloads,
enumeração indiscriminada ou varredura fora do alvo. Ao encontrar risco, Neo compõe imediatamente
[`security-review`](../skills/security-review.md), preserva evidência mínima e faz handoff; não
“confirma explorabilidade” executando abuso.

### Processo e runtime

| Fraqueza/superfície | Consequência | Evidência observável | Controle esperado | Teste defensivo autorizado | Handoff |
|---|---|---|---|---|---|
| serviço/processo desnecessário | superfície e privilégio sem função | inventário de processo, owner, listener e origem de inicialização | allowlist de serviços e remoção/desativação governada | A0: correlacionar processo→socket→contrato; sem probing amplo | Jarvis para ambiente; Severino para manifesto versionado |
| execução como identidade excessiva | impacto maior após falha | usuário/token, grupos/roles, capabilities/entitlements e acesso efetivo | mínimo privilégio e separação de identidade | A0: comparar identidade efetiva com contrato | Jarvis/Severino; Yoda se mudar modelo de confiança |
| runtime/dependência vulnerável ou sem manutenção | execução indevida, indisponibilidade, supply-chain | versão resolvida, lock/SBOM, advisory atual e alcance | versão suportada, pin, proveniência e atualização governada | A0: `auditar-dependencias`; não explorar CVE | Severino + Neo/security-review |
| segredo em env, argv, dump, log ou trace | credencial reutilizável/exposição lateral | configuração redigida e ocorrência segura; nunca copiar valor | secret store, escopo mínimo, redação e rotação | A0: presença/fluxo do segredo sem materializar conteúdo | Jarvis para rotação/ambiente; Severino para logging/config |

### Syscall, privilégio e limites do host

| Fraqueza/superfície | Consequência | Evidência observável | Controle esperado | Teste defensivo autorizado | Handoff |
|---|---|---|---|---|---|
| syscall/capability/entitlement além da necessidade | escape, inspeção ou mutação indevida | policy efetiva, sandbox/profile, capabilities e chamadas necessárias | deny-by-default e conjunto mínimo | A0: comparar policy com operação esperada; sem tentar escapar | Jarvis/Severino; Yoda se estrutural |
| descriptors/processos/memória sem limite | exaustão e indisponibilidade | limites, uso, OOM/eventos, fila e tendência | quotas, backpressure, timeouts e alertas | A0/A1: medir sob tráfego mínimo já autorizado | Jarvis para mitigação; Severino para lifecycle/backpressure |
| acesso de diagnóstico excessivo | vazamento por attach/dump/capture | ACL/role necessária ao adapter e auditoria de uso | acesso just-in-time, escopo e registro | A0: verificar autorização; não elevar privilégio | Jarvis/usuário |

### Socket, bind e porta

| Fraqueza/superfície | Consequência | Evidência observável | Controle esperado | Teste defensivo autorizado | Handoff |
|---|---|---|---|---|---|
| bind wildcard/exposto sem necessidade | serviço alcançável por redes não pretendidas | listener com namespace/família/endereço/porta e rota de exposição | bind/interface mínimos + filtro em profundidade | A0: listener; A1: conexão só de origens autorizadas | Severino se config; Jarvis se override/filtro |
| porta/serviço legado ou de debug | bypass e informação sensível | inventário listener→PID→imagem/config→owner | nenhum listener sem dono/contrato; debug fechado | A0: mapear; não enumerar rede | Severino/Jarvis + security-review |
| reuse/backlog/timeout permissivo ou incoerente | hijack local, DoS, starvation ou conexão presa conforme plataforma | opções do socket, filas/drops, config e estados | defaults revisados, limites e lifecycle | A0/A1: observar estado e teste de carga mínima, nunca stress | Severino/Jarvis |
| autenticação/autorização ausente na fronteira | acesso indevido apesar de rede funcionar | contrato, middleware/policy e respostas negativas controladas | authn/authz server-side e deny-by-default | A1: caso negativo autorizado e idempotente, sem brute force | Severino; Patrick valida comportamento; Neo gate |

### Host, firewall e connection tracking

| Fraqueza/superfície | Consequência | Evidência observável | Controle esperado | Teste defensivo autorizado | Handoff |
|---|---|---|---|---|---|
| regra ampla por origem/porta/direção | exposição e movimento lateral | regra efetiva, contador/log e caminho do pacote | mínimo necessário, egress/ingress explícitos | A0: policy/counters; A1: uma conexão no fluxo autorizado | Jarvis; Yoda se topologia mudar |
| conflito entre firewall local e externo | bypass ou bloqueio inconsistente | regras em cada camada e flow logs correlacionados | fonte canônica e defesa em profundidade coerente | A0/A1 focado; não varrer portas | Jarvis |
| conntrack/NAT sem capacidade/timeout adequado | indisponibilidade seletiva e colisão | uso/limite, drops, traduções e estados | capacity guardrails, timeouts medidos e alertas | A0; A1 apenas conexão limitada | Jarvis |

### LAN, ARP/NDP e enlace

| Fraqueza/superfície | Consequência | Evidência observável | Controle esperado | Teste defensivo autorizado | Handoff |
|---|---|---|---|---|---|
| vizinhança ARP/NDP inesperada ou instável | desvio/interceptação/indisponibilidade | cache vizinho, MAC/port esperado, eventos de mudança | segmentação, controles de switch/router e monitoramento | A0: comparar cache e inventário; sem spoofing | Jarvis/rede |
| LAN/VLAN/SSID mais amplo que o contrato | alcance lateral indevido | associação de interface, VLAN/subnet, tabela MAC/ACL autorizada | segmentação e isolamento | A0 e conexão dirigida ao endpoint autorizado | Jarvis/Yoda |
| RA/DHCP/configuração automática indevida | gateway/DNS/endereço controlados incorretamente | leases, RA, gateway/resolver e origem | servidores autorizados, validação/guardas disponíveis | A0: conferir estado; não enviar anúncios | Jarvis/rede |

### Routing, NAT, BGP e Internet

| Fraqueza/superfície | Consequência | Evidência observável | Controle esperado | Teste defensivo autorizado | Handoff |
|---|---|---|---|---|---|
| rota default/estática/policy route permissiva ou errada | egress indevido, blackhole ou bypass | route lookup efetivo por origem/destino, next hop e retorno | rotas mínimas, ownership e revisão | A0: lookup; A1: trace dirigido ao destino autorizado | Jarvis/Yoda |
| NAT/port-forward público não intencional | exposição direta e bypass de proxy | tradução/regra efetiva e endpoint observado | publicação explícita, inventário e filtro | A0 + conexão dirigida de origem autorizada | Jarvis + Neo/security-review |
| egress irrestrito/SSRF alcança rede interna | acesso a serviços internos/metadata | policy de egress, rotas, DNS/proxy e logs de destinos | egress allowlist/proxy, validação de destino, proteção de metadata | A0: revisar controles; A1: somente caso benigno aprovado, sem tocar metadata real | Severino/Jarvis/Yoda |
| anúncio/propagação BGP anômala ou prefixo sem proteção | hijack/leak/indisponibilidade | fontes de roteamento autorizadas, visibilidade multi-ponto e mudança temporal | filtros, RPKI/ROA e política do operador quando aplicável | A0: consultar observações públicas/autorizadas; sem anunciar rota | operador/ISP/Jarvis; Yoda se arquitetura |

### DNS

| Fraqueza/superfície | Consequência | Evidência observável | Controle esperado | Teste defensivo autorizado | Handoff |
|---|---|---|---|---|---|
| registro/zone/delegação incorretos | desvio, indisponibilidade ou takeover conforme contexto | resposta por recursivo/autoritativo, NS/delegação, TTL e config/IaC | ownership, revisão, least privilege e mudança auditada | A0/A1: consultas dirigidas a servidores conhecidos | Jarvis/Severino; Yoda se ownership |
| cache poisoning/hijack ou resolver não autorizado | cliente recebe destino controlado/errado | diferenças entre resolvers/pontos, origem da config, DNSSEC quando adotado | resolver confiável, transporte/policy e validação aplicável | A1: comparar consultas; nunca injetar resposta | Jarvis + security-review |
| split DNS e search suffix mal configurados | destino interno/externo trocado | resolver efetivo da aplicação, search list e resposta por contexto | namespace/zone explícitos e teste por ambiente | A0/A1 dirigido | Jarvis/Severino |
| DNS de container/cluster permissivo ou spoofable | redirecionamento/lateral movement | config do runtime/cluster, policies e logs do resolver | policies, isolamento e identidade do serviço | A0; sem envenenamento simulado | Jarvis/Yoda |

### TLS e HTTPS

| Fraqueza/superfície | Consequência | Evidência observável | Controle esperado | Teste defensivo autorizado | Handoff |
|---|---|---|---|---|---|
| trust store/CA excessiva ou inesperada | certificados indevidos aceitos | cadeia validada, store efetivo e policy | raízes mínimas, gestão/rotação e validação ativa | A1: handshake com hostname/SNI corretos; sem desabilitar trust como aceite | Jarvis/Severino + Neo |
| hostname/SNI/certificado divergentes | impersonation warning/falha/virtual host errado | SNI enviado, SAN, cadeia e erro exato | identidade correta por hostname e renovação | A1: handshake dirigido e redigido | Jarvis/Severino |
| versão/cipher/ALPN incompatível ou legado | downgrade, falha ou protocolo inesperado | negociação efetiva por cliente autorizado | política moderna compatível com requisito | A1: enumerar somente negociações necessárias/permitidas, sem varredura ampla | Yoda/Severino/Jarvis |
| mTLS ausente, opcional ou trust/client mapping incorretos | cliente indevido ou bloqueio legítimo | exigência, cadeia cliente, mapping de identidade e decisão | mTLS deny-by-default quando requerido, identidade/autorização separadas | A1 com credencial de teste autorizada, nunca material real em output | Severino/Jarvis + security-review |
| terminação TLS cria upstream sem proteção/validação | trecho interno interceptável ou destino errado | configuração e handshake da perna upstream | TLS/identidade conforme modelo de ameaça | A0/A1 na perna autorizada | Yoda/Jarvis/Severino |

### HTTP, proxy, load balancer e CDN

| Fraqueza/superfície | Consequência | Evidência observável | Controle esperado | Teste defensivo autorizado | Handoff |
|---|---|---|---|---|---|
| confiança indevida em `Host`, forwarded headers ou IP do cliente | auth/routing/log/policy bypass | cadeia de proxies, headers reescritos e lista de proxies confiáveis | sobrescrever/normalizar na borda e confiar só em hops definidos | A1: requests benignos com variações controladas no ambiente de teste | Severino + Neo/security-review |
| acesso direto ao upstream/proxy bypass | contorna TLS, WAF, auth ou rate limit | rotas/listeners/DNS e policy que permitem perna direta | upstream privado/autenticado e filtros | A0/A1 dirigido apenas ao endpoint autorizado | Jarvis/Yoda/Severino |
| request smuggling/desync por parsing/framing divergente | troca de fronteira entre requests, cache/route indevidos | versões/config de parsers, normalização, logs de framing/anomalias | cadeia atualizada, um contrato de framing e rejeição ambígua | revisão conceitual/config + testes oficiais seguros em laboratório; sem payload de exploração | Severino/Yoda + Neo; bloqueante |
| cache/CDN key ou conteúdo sensível incorretos | vazamento entre usuários/contexts | headers de cache, key policy e resposta de casos autorizados | cache explícito, `private/no-store` e vary/key corretos | A1: usuários/dados sintéticos em teste idempotente | Severino/Jarvis/Patrick |
| authn/authz apenas no proxy ou apenas na aplicação sem contrato | bypass por rota alternativa | policies e testes negativos em cada fronteira | responsabilidade explícita e defesa em profundidade | A1: teste negativo autorizado por caminho | Severino/Yoda + Neo |
| logs/capturas contêm headers, cookies ou bodies sensíveis | exposição operacional | schema/redaction e amostra segura sem copiar segredo | allowlist/redaction, retenção e acesso mínimos | A0: verificar metadados/presença com valores mascarados | Severino/Jarvis + security-review |

### Containers, Kubernetes e service mesh

| Fraqueza/superfície | Consequência | Evidência observável | Controle esperado | Teste defensivo autorizado | Handoff |
|---|---|---|---|---|---|
| namespace compartilhado/host network desnecessário | isolamento reduzido e portas colidindo/expostas | spec/runtime e namespace efetivo | namespace isolado salvo justificativa | A0: mapear processo/socket/namespace | Yoda/Jarvis/Severino |
| privileged/capabilities/mount/socket do runtime excessivos | controle de host/escape com impacto alto | spec efetiva, capabilities, mounts e device access | rootless/minimum capabilities, readonly e policy | A0: comparar com contrato; não tentar escape | Jarvis/Severino + security-review |
| Service/Ingress/NetworkPolicy amplo ou selector errado | exposição/lateral movement/tráfego ao backend errado | endpoints selecionados, policy efetiva e flow logs | selectors e policies deny-by-default | A0/A1 dirigido, sem descoberta ampla de cluster | Jarvis/Yoda |
| mesh permite bypass do sidecar/gateway ou identidade frouxa | contorna mTLS/policy/telemetria | rotas, listeners, policy e identidade da perna direta | captura/redirect robusto, mTLS e policy explícita | A0/A1 somente em fixture/autorizado | Jarvis/Yoda + Neo |
| secret em env, volume, annotation ou log | exposição entre workloads/operadores | presença e ACL redigidas | secret store, montagem mínima, rotação e redação | A0 sem ler valor | Jarvis/Severino |

### Cloud, VPC/VNet, VPN e metadata service

| Fraqueza/superfície | Consequência | Evidência observável | Controle esperado | Teste defensivo autorizado | Handoff |
|---|---|---|---|---|---|
| SG/NACL/firewall/route permissivo | exposição ou egress/lateral movement | policy efetiva + flow/reachability + plano de dados | mínimo por origem/destino/porta/direção | A0 e uma conexão A1 no fluxo autorizado | Jarvis/Yoda + Neo |
| subnet pública/IP público sem necessidade | aumento de superfície/bypass | interface, endereço, route table, gateway e DNS | private-by-default e proxy/LB explícito | A0: mapear; A1 dirigido | Yoda/Jarvis |
| VPN/túnel com routes amplas, split incorreto ou trust indevido | leak/bypass/alcance lateral | selectors/routes/SAs e origem efetiva | prefixos/identidades mínimos e kill/fail-closed conforme contrato | A0/A1 limitado; sem testar redes fora do escopo | Jarvis/Yoda |
| metadata service alcançável por workload vulnerável/SSRF | credenciais temporárias e identidade cloud | route/policy/token mode e proteção documentada; nunca coletar credencial | mecanismo endurecido, identidade mínima e egress/host controls | A0: verificar configuração; A1 somente simulação segura sem acessar credencial real | Jarvis/Severino/Yoda + Neo bloqueante |
| flow log/reachability analyzer tratado como prova final | falso negativo/positivo de plano de dados | diferença entre modelo e conexão/captura real | usar como evidência complementar | A0 + A1 real controlado | Neo/Jarvis |

### Supply-chain de agentes, skills e ferramentas

| Fraqueza/superfície | Consequência | Evidência observável | Controle esperado | Teste defensivo autorizado | Handoff |
|---|---|---|---|---|---|
| adapter/tool não pinado ou origem não verificada | comportamento alterado/comando malicioso | versão/hash/origem/licença e caminho resolvido | pin, checksum/proveniência, mínimo conjunto e atualização revisada | A0: inventário + `security-scan`; não executar binário desconhecido | Severino/Yoda + Neo |
| output de tool tratado como instrução | prompt/tool injection e desvio de escopo | fluxo de dados e prompt/adapter | output como dado não confiável, schema e guardrails | revisão A0 com conteúdo sintético | Yoda/Severino + Neo |
| agente/tool com permissão maior que A0/A1 | mutação ou exfiltração indevida | grants, sandbox, cwd, network e secrets expostos | capability e sandbox mínimos, aprovação por ação | A0: comparar grants; não testar abuso | Oráculo/usuário/Jarvis |
| trace/handoff preserva segredo ou contexto irrelevante | vazamento entre agentes/artefatos | schema/redaction e amostra segura | filtro de contexto, allowlist e retenção | A0: lint/review redigido | Severino + Neo/security-scan |

## Critério de granularidade deste playbook

SO, sockets, transporte, DNS, TLS, HTTP, infraestrutura e superfícies defensivas permanecem em um
único playbook nesta primeira versão porque formam uma cadeia causal e a leitura é seletiva pelo
índice. Dividir só será proposto se uma avaliação de qualidade demonstrar degradação mensurável de
seleção, latência/contexto ou precisão, ou se uma seção adquirir owner/autoridade diferente.
Número de linhas sozinho não justifica split.

## Relações

- Agente: [`../agentes/neo.md`](../agentes/neo.md)
- Skill: [`../skills/investigar-sistemas-redes.md`](../skills/investigar-sistemas-redes.md)
- Contrato: [`../contratos/neo-evidencia-handoff.md`](../contratos/neo-evidencia-handoff.md)
