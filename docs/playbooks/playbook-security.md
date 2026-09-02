# Playbook Security — riscos, identificação e correção

**Autor:** Cristóvão Augusto

> **Como usar**: referência de consulta seletiva do agente `security` (e do `backend-dev` ao
> implementar área sensível). NÃO ler inteiro — o índice mapeia superfície → seção. Cada seção
> traz: onde o risco costuma existir, **como identificar** (sinais em revisão, padrões de grep),
> **como resolver** e **severidade típica** (calibrar com contexto — roles.md §6.6; risco teórico
> sem vetor real é informativo, não bloqueante).
>
> **Este é o playbook-base do template.** Na descoberta, contextualize ao domínio (quais dados
> são PII, qual gateway/webhook, se há uploads, se a LGPD/GDPR se aplica) e à stack (os exemplos
> citam Go/Echo como ilustração — traduza os greps para a stack escolhida).
> Skills relacionadas: `security-review` (checklist geral), `security-scan` (config `.claude/`).

| # | Superfície | Consultar quando… |
|---|-----------|-------------------|
| 1 | [SQL injection](#1-sql-injection) | qualquer SQL dinâmico/concatenado |
| 2 | [XSS](#2-xss) | HTML/template renderizando dado do usuário |
| 3 | [CSRF](#3-csrf) | rota de escrita autenticada por cookie |
| 4 | [Armazenamento de senha](#4-armazenamento-de-senha) | login/cadastro |
| 5 | [Brute force e enumeração](#5-brute-force-e-enumeração) | login, reset de senha, erros de auth |
| 6 | [Sessão e cookies](#6-sessão-e-cookies) | emissão/validação de sessão, atributos de cookie |
| 7 | [Autorização e IDOR](#7-autorização-e-idor) | rota com ID de recurso; rotas administrativas |
| 8 | [Secrets](#8-secrets) | config, env, docker-compose, CI, logs |
| 9 | [Webhooks de terceiros](#9-webhooks-de-terceiros) | endpoint de webhook, verificação de assinatura |
| 10 | [Integridade de valores](#10-integridade-de-valores) | preço/valor/status vindo do cliente |
| 11 | [Tokens de acesso a recurso](#11-tokens-de-acesso-a-recurso) | vouchers, QR, links de download, convites |
| 12 | [Validação de entrada / mass assignment](#12-validação-de-entrada--mass-assignment) | bind de request em struct/modelo |
| 13 | [Exposição de dados e erros](#13-exposição-de-dados-e-erros) | structs de resposta, handler de erro |
| 14 | [Headers e transporte](#14-headers-e-transporte) | middleware HTTP, TLS, CORS |
| 15 | [SSRF](#15-ssrf) | servidor buscando URL fornecida por alguém |
| 16 | [Upload de arquivos](#16-upload-de-arquivos) | qualquer upload |
| 17 | [DoS e exaustão de recursos](#17-dos-e-exaustão-de-recursos) | limites de body, timeouts, rate limit |
| 18 | [Dependências e supply chain](#18-dependências-e-supply-chain) | dependência nova/atualizada, CI |
| 19 | [Logs, PII e LGPD](#19-logs-pii-e-lgpd) | qualquer log/trace com dado de usuário |
| 20 | [Timing e comparação de segredos](#20-timing-e-comparação-de-segredos) | comparação de token/assinatura/hash |
| 21 | [Checklist de auditoria](#21-checklist-de-auditoria) | passe de julgamento pré-push (roles.md §6.4) |

---

## 1. SQL injection

**Como identificar**: grep por `fmt.Sprintf`/concatenação/f-string/template-string perto de `SELECT|INSERT|UPDATE|DELETE|ORDER BY|WHERE`; chamadas de query onde o SQL não é constante; qualquer identificador (coluna de sort, direção) vindo do request. Stacks com queries geradas/parametrizadas eliminam quase tudo por construção — o risco mora nas exceções manuais.

**Como resolver**: valores → sempre placeholder/parâmetro; identificadores dinâmicos → **allowlist fechada no código** (`map` de nomes permitidos), nunca o valor do request na string; filtros opcionais → `($1 IS NULL OR col = $1)` ou queries separadas (playbook-database §17.5).

**Severidade**: crítica se explorável; SQL concatenado com input é no mínimo alta mesmo sem PoC.

## 2. XSS

**Como identificar**: bypasses explícitos de escaping — `template.HTML(` (Go), `dangerouslySetInnerHTML` (React), `v-html` (Vue), `innerHTML` — sobre dado externo; e-mails HTML montados por concatenação; endpoint devolvendo `text/html` com dado não escapado. O XSS **armazenado** via conteúdo cadastrado em área administrativa dispara no browser dos usuários finais.

**Como resolver**: nunca desativar escaping sobre dado externo (templates modernos escapam por default); `Content-Type` correto em APIs JSON; CSP como segunda camada (§14); sanitização de HTML rico só se o campo rico for inevitável — preferir não ter o campo.

**Severidade**: alta (armazenado) / média (refletido).

## 3. CSRF

**Como identificar**: rotas mutantes (POST/PUT/DELETE) autenticadas via **cookie** sem token CSRF nem `SameSite` restritivo; `GET` que muda estado (duplamente errado). Se a autenticação for por header (`Authorization`), CSRF não se aplica.

**Como resolver**: `SameSite=Lax` (ou `Strict`) no cookie de sessão como base obrigatória (§6); middleware CSRF com token por sessão nas áreas administrativas se houver qualquer dúvida (subdomínios, iframes); nenhuma mutação via GET, nunca.

**Severidade**: alta em área administrativa sem SameSite.

## 4. Armazenamento de senha

**Como identificar**: grep `md5|sha1|sha256` perto de `password|senha`; comparação direta de senha em SELECT; coluna de senha sem sufixo `_hash`; senha em log ou resposta de API.

**Como resolver**: **argon2id** (parâmetros OWASP como piso) ou bcrypt (custo ≥ 12) — registrar em `lib.md`; formato de hash versionado (`$argon2id$...`) para rehash futuro; validação pela própria lib (constant-time embutido — §20); política: mínimo 8+, sem regras de composição bizantinas.

**Severidade**: crítica (hash fraco/ausente = comprometimento total na primeira exfiltração).

## 5. Brute force e enumeração

**Como identificar**: login sem rate limit por conta+IP; mensagens distintas ("usuário não existe" vs "senha incorreta") — enumeração de contas; idem no reset ("enviado" vs "não cadastrado"); timing revelador (hash só roda se o usuário existe).

**Como resolver**: rate limit duro no login (ex.: 5/min por conta+IP, 429 — playbook-backend §17); mensagem única "credenciais inválidas"; reset sempre "se existir, enviamos"; hash dummy no caminho "usuário não existe" para igualar tempo; lockout progressivo/backoff (bloqueio permanente vira DoS contra o dono da conta); log + métrica de tentativas falhas (roles.md §6.8).

**Severidade**: alta (login administrativo sem rate limit).

## 6. Sessão e cookies

**Como identificar**: grep `SetCookie|Set-Cookie|http.Cookie` e conferir atributos; token previsível ou curto; sessão sem expiração; ID de sessão que não muda após login; `math/rand` (ou equivalente não-criptográfico) em código de auth = achado imediato.

**Como resolver**
1. Cookie de sessão: `HttpOnly`, `Secure`, `SameSite=Lax` mínimo (§3), `Path` restrito.
2. Token: 128+ bits de gerador **criptográfico**, armazenado server-side com expiração absoluta + deslizante.
3. **Rotacionar o ID de sessão no login** (session fixation) e invalidar server-side no logout.
4. JWT: só com necessidade real (múltiplos serviços); algoritmo fixo (nunca aceitar `alg` do token, rejeitar `none`), expiração curta; revogação exige lista server-side — por isso sessão server-side é o default mais simples.

**Severidade**: alta (cookie sem HttpOnly/Secure em produção); token previsível = crítica.

## 7. Autorização e IDOR

**Como identificar**: toda rota com ID de recurso — a query filtra por dono (`WHERE id = $1 AND usuario_id = $2`) ou só por ID? Rotas administrativas: middleware de papel no **grupo** de rotas (não handler a handler, onde um esquecimento passa)? IDs sequenciais expostos amplificam (enumeráveis).

**Como resolver**
1. **Posse na query, não no código**: buscar-por-id-e-dono numa query só; "não achou" = 404 (não 403, que confirma existência).
2. Autorização por **grupo de rotas**, deny-by-default — rota nova nasce protegida.
3. UUIDs em URLs públicas (defesa em profundidade, não substituto do check de posse).
4. Teste do QA obrigatório em todo PRD com recurso possuído: usuário A tenta recurso de B → 404; anônimo tenta admin → 401/302.

**Severidade**: crítica (IDOR em dado pessoal/financeiro; admin aberto = comprometimento total).

## 8. Secrets

**Como identificar**: grep no diff e na árvore por `sk_live|whsec_|api_key|password=|secret|BEGIN.*PRIVATE KEY|AKIA[0-9A-Z]{16}` e strings de alta entropia; compose com senha real em vez de env; `.env` fora do `.gitignore`; secret em log (§19), URL (vai para access log) ou mensagem de erro. **Histórico conta**: secret removido em commit posterior continua exposto — exige rotação.

**Como resolver**: env vars carregadas na borda (config no boot, falha rápida se ausente); `.env` ignorado + `.env.example` sem valores; secrets de CI no cofre da plataforma; **vazou → rotacionar imediatamente** (a chave, não só o commit); secret scanning + push protection no repositório. Chave de sandbox/teste vaza também: normaliza o hábito e o mesmo caminho carrega a chave real depois.

**Severidade**: crítica, sempre (regra dura da auditoria: REPROVADO sem exceção).

## 9. Webhooks de terceiros

**Como identificar**: handler de webhook sem verificação de **assinatura** (ex.: `Stripe-Signature` via `webhook.ConstructEvent` da lib oficial); segredo do webhook hardcoded (§8); corpo lido **depois** de middleware que o consome/transforma (a verificação exige o corpo bruto); lógica confiando em campos do evento sem revalidar contra o dado local (§10).

**Como resolver**
1. Verificação de assinatura **pela lib oficial do provedor** (HMAC constant-time + janela anti-replay) — nunca implementar na mão.
2. Rota fora de middlewares de parse de body; ler o corpo bruto com limite (§17) antes de tudo.
3. Idempotência por `event.ID` (playbook-backend §1) — replay legítimo é esperado.
4. Processar só os tipos de evento esperados; responder 2xx rápido e mover efeitos pesados para a outbox (timeout do provedor gera retry).

**Severidade**: crítica (webhook de pagamento sem assinatura = confirmação forjada).

## 10. Integridade de valores

**Regra**: o cliente escolhe **o quê**; o servidor decide **quanto**.

**Como identificar**: campo `valor|preco|total|amount` no payload de request que o servidor **usa** em vez de recalcular; cobrança criada com amount vindo do request; webhook confirmando sem conferir valor/moeda contra o registro local; transição de status sem máquina de estados (webhook atrasado revivendo pedido expirado — race legítima que precisa de resposta definida).

**Como resolver**: preço sempre do banco no momento da operação, congelado no registro (snapshot); cobrança criada server-side com o valor do registro + referência nos metadata; confirmação valida evento↔registro (id, valor, moeda) antes de transicionar; transições válidas explícitas com update condicional (`WHERE status = 'aguardando'` — playbook-database §7.3); divergência → não confirmar, logar como incidente.

**Severidade**: crítica.

## 11. Tokens de acesso a recurso

Vale para QR de ingresso, voucher, link de download, convite, link de reset — qualquer artefato que **é** a credencial de acesso a um recurso.

**Como identificar**: conteúdo do token é ID sequencial/adivinhável? A validação consulta o servidor ou "confia" no conteúdo? Existe marcação de uso para o que é single-use? O endpoint de validação é um oráculo aberto (sem auth/rate limit)?

**Como resolver**
1. **Referência opaca** — UUID aleatório ou token de 128+ bits criptográfico. Token assinado (HMAC) validável offline só se a validação precisar funcionar sem rede; senão, referência opaca + consulta é mais simples e revogável.
2. Single-use marcado **atomicamente**: `UPDATE ... SET usado_em = now() WHERE id = $1 AND usado_em IS NULL`, RowsAffected = 1 (dois validadores simultâneos = um passa).
3. Endpoint de validação autenticado e com rate limit.
4. Expiração adequada ao artefato (link de reset: minutos/horas, não dias).

**Severidade**: alta (token adivinhável/reusável); com IDOR (§7) vira crítica.

## 12. Validação de entrada / mass assignment

**Como identificar**: bind automático do request (`c.Bind`, `@RequestBody`, `Model.create(req.body)`) preenchendo **modelo de domínio/banco** — o cliente escreve `status`, `role`, `saldo`; campos sem validação de faixa/formato/tamanho após o bind.

**Como resolver**: **structs/DTOs de request dedicados por endpoint** contendo só os campos que o cliente pode enviar — mass assignment morre por construção; validação declarativa ou explícita logo após o bind (presença, faixa, comprimento, formato); rejeitar cedo com 400 por campo; limite de tamanho de body antes do parse (§17).

**Severidade**: alta quando o bind alcança campo sensível; média como higiene.

## 13. Exposição de dados e erros

**Como identificar**: modelo de domínio serializado direto na resposta (vaza hash de senha, campos internos, dados de outros usuários); `err.Error()`/stack trace dentro de resposta HTTP (revela schema, confirma e-mails cadastrados); modo debug ligado fora de dev; endpoints de diagnóstico (`/metrics`, profiler) expostos sem auth.

**Como resolver**: DTOs de resposta explícitos (espelho do §12 — nada sai sem struct de saída dedicado); handler de erro central: erro de domínio → status + mensagem controlada; erro inesperado → 500 genérico + log completo server-side com correlation id (o id vai na resposta, o detalhe não); diagnóstico em porta/grupo interno.

**Severidade**: média a alta conforme o que vaza; hash de senha em resposta = crítica.

## 14. Headers e transporte

**Como identificar**: ausência de middleware de secure headers; CORS `*` com credenciais; cookie sem `Secure`; app aceitando HTTP puro em produção; rate limit por IP confiando em `X-Forwarded-For` de qualquer origem (contornável por header forjado).

**Como resolver**: middleware de secure headers como base — `Strict-Transport-Security`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`/CSP `frame-ancestors`, `Referrer-Policy`; CSP `default-src 'self'` + exceções mínimas documentadas nas páginas sensíveis (camada 2 contra XSS §2); CORS com origem exata, nunca `*` com `AllowCredentials`; confiar no `X-Forwarded-For` **só** do proxy da plataforma (configurar o IP extractor).

**Severidade**: média (defesa em profundidade); CORS aberto com credenciais = alta.

## 15. SSRF

**Como identificar**: grep por client HTTP com URL derivada de input; qualquer feature "buscar de URL" (importar imagem por link, callback configurável). Alvos do atacante: metadata da cloud (`169.254.169.254`), serviços internos (`redis:6379`).

**Como resolver**: preferir **upload** (§16) a fetch-por-URL — elimina a classe; se inevitável: allowlist de esquema (https) e host, resolver DNS e bloquear IPs privados/link-local **na conexão** (protege contra DNS rebinding), não seguir redirects cegamente, timeout curto e limite de tamanho.

**Severidade**: alta se o vetor existir; informativo enquanto não houver fetch de URL.

## 16. Upload de arquivos

**Como identificar**: handler confiando em `Content-Type` do request ou na extensão; arquivo salvo com nome vindo do cliente (path traversal `../../`); sem limite de tamanho; servido do mesmo host com Content-Type permissivo (SVG com script = XSS §2).

**Como resolver**: validar **magic bytes** contra allowlist curta (jpeg, png, webp — **não** SVG de usuário); nome novo gerado (UUID + extensão do tipo real), nunca o original; limite de tamanho antes de ler; armazenar fora da árvore servida ou com `Content-Type` explícito + `nosniff`; dimensões máximas se houver processamento de imagem (decompression bomb).

**Severidade**: alta (upload sem validação); path traversal = crítica.

## 17. DoS e exaustão de recursos

**Como identificar**: servidor HTTP sem `ReadTimeout`/`WriteTimeout`/`IdleTimeout` (Go não tem default!); sem limite global de body; rota cara sem rate limit; **recurso reservável sem TTL** (bot reserva tudo para sempre — a variante de domínio mais comum).

**Como resolver**: limite de body global com exceção dimensionada só onde precisa; timeouts do servidor configurados; rate limiting nos públicos (playbook-backend §17); **TTL curto com liberação automática em toda reserva/trava de recurso**; back pressure interno (playbook-backend §7). DDoS volumétrico é camada de plataforma/CDN — registrar como limitação aceita, não prometer.

**Severidade**: média (hardening); reserva sem TTL = alta (nega o negócio).

## 18. Dependências e supply chain

**Como identificar**: scanner de vulnerabilidades da stack (ex.: `govulncheck` em Go — acusa só função alcançável; `npm audit`/`pip-audit`) na revisão e no CI; sanidade da lib nova: manutenção ativa, autor identificável, typosquatting no nome; lockfile sempre commitado; diff do manifesto sem entrada correspondente no `lib.md` = achado imediato (roles.md §6.9).

**Como resolver**: vulnerabilidade alcançável → atualizar; sem correção → substituir ou mitigar documentado com prazo; menor conjunto de dependências (stdlib primeiro); atualizar em cadência; CVEs por fontes atuais (Context7/advisories), nunca memória de treinamento.

**Severidade**: conforme CVE e alcançabilidade; processo violado (dep sem registro/CVE check) = REPROVADO por regra.

## 19. Logs, PII e LGPD

**Como identificar**: grep nos logs estruturados por `email|nome|cpf|telefone|senha|token|cookie|authorization|card`; middleware de dump de request body; DSN com senha em log de boot; token em query string (vai para access log de qualquer proxy). Dados de cartão: **não podem existir** em log nem banco próprio — com gateway embedado (Elements/Checkout) o número nunca toca o servidor; campo de cartão em request próprio = achado crítico + escopo PCI.

**Como resolver**: allowlist mental — IDs opacos sim, conteúdo não (`pedido_id` em vez de e-mail); **redação central por tipo** (ex.: `slog.LogValuer` em Go, serializer custom — o tipo com PII se auto-redige em qualquer log); tokens jamais em URL; retenção curta documentada; PII é dado de negócio (banco, protegido por §7), não dado de log.

**Severidade**: alta (PII em log de terceiro = incidente LGPD/GDPR); credencial/token em log = crítica (REPROVADO).

## 20. Timing e comparação de segredos

**Como identificar**: grep por `==`/`equals`/`bytes.Equal` comparando token, assinatura, hash ou chave; branch de login que retorna antes de custo constante (§5).

**Como resolver**: comparação constant-time (`crypto/subtle.ConstantTimeCompare` ou equivalente da stack); melhor: armazenar e comparar **hash** do token (SHA-256) — lookup por hash é seguro e o banco nunca guarda o token vivo; libs oficiais (webhook §9, argon2/bcrypt §4) já fazem certo — não reimplementar.

**Severidade**: média isolada (higiene barata); token vivo armazenado sem hash sobe para alta.

## 21. Checklist de auditoria

Passe de julgamento de segurança (roles.md §6.4) — percorrer o que o diff toca:

1. **Secrets**: diff e histórico limpos? Env/config sem valor real? (§8 — REPROVADO se violado)
2. **Auth**: rota nova exige autenticação correta? Grupo protegido? (§6, §7)
3. **Posse**: recurso possuído filtrado por dono na query? (§7)
4. **Entrada**: DTO dedicado + validação + limite de body? (§12, §17)
5. **Saída**: DTO de resposta, erro genérico, nada interno vazando? (§13)
6. **SQL**: algum SQL concatenado/dinâmico fora do padrão? (§1)
7. **Valores**: preço/estado sempre server-side? Webhook verificado? (§9, §10)
8. **Logs**: PII/token em algum log novo? (§19 — REPROVADO se credencial)
9. **Dependência nova**: `lib.md` + CVE verificado? (§18 — REPROVADO se ausente)
10. **Segredos comparados**: constant-time / hash armazenado? (§20)

Classificar cada achado (crítica/alta/média/baixa) com vetor concreto; teórico sem vetor = informativo.
