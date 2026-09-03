package br.com.minerva.financas.comum.rest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Base dos testes de integração.
 * <p>
 * O servidor sobe em porta real e as chamadas saem por {@link HttpClient} do JDK: exercitam o
 * servidor HTTP, a serialização JSON, os controllers, os casos de uso e a persistência em SQLite —
 * a pilha inteira, como o enunciado exige. Um mock de camada web provaria menos.
 * <p>
 * <h2>Isolamento entre casos</h2>
 * Cada caso cria os próprios dados, com código de ativo exclusivo, e o {@code @AfterEach} devolve o
 * banco ao estado do seed — inclusive quando o caso falha, de modo que uma falha não contamina o
 * seguinte. A suíte roda repetidamente e em qualquer ordem.
 * <p>
 * A limpeza é <strong>global</strong>, não seletiva: apaga todos os lançamentos e movimentações e os
 * ativos que não pertencem ao seed. Isso é correto porque os casos rodam em sequência, e é declarado
 * aqui porque tem uma consequência real — habilitar execução paralela de testes exigiria trocar esta
 * limpeza por uma escopada ao caso, senão um teste apagaria os dados de outro em pleno voo.
 * <p>
 * O que a limpeza <em>não</em> alcança é estado em memória do processo, como o limite de execuções
 * assíncronas retidas por usuário. Casos que exercitam esse limite usam um login exclusivo.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class TesteIntegracao {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final AtomicInteger SEQUENCIA = new AtomicInteger();

    @LocalServerPort
    private int porta;

    /** Casos em execução, extraídos do {@code @DisplayName}, para nomear as pastas de evidência. */
    private List<String> casosAtuais = List.of("sem-tc");

    @BeforeEach
    void identificarCaso(TestInfo informacao) {
        casosAtuais = identificadoresDeCaso(informacao.getDisplayName());
    }

    @Autowired
    private JdbcTemplate db;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /** Resposta HTTP crua, com o corpo já disponível como texto e como árvore JSON. */
    record Resposta(int status, String corpo) {

        JsonNode json() {
            return JSON.readTree(corpo);
        }

        JsonNode item(int indice) {
            return json().get(indice);
        }

        String codigoDeErro() {
            return json().path("codigo").asString();
        }
    }

    /**
     * Compara números de JSON pelo valor, não pelo texto. {@code 10.00} e {@code 10.0} são o mesmo
     * número JSON, e prender o teste à forma textual quebraria a suíte por uma diferença que não é
     * de comportamento.
     */
    static void assertNumero(String esperado, JsonNode no, String mensagem) {
        java.math.BigDecimal real = no.isNull() ? null : no.decimalValue();
        assertTrue(real != null && new java.math.BigDecimal(esperado).compareTo(real) == 0,
                mensagem + " — esperado " + esperado + ", obtido " + no.asString());
    }

    static void assertNulo(JsonNode no, String mensagem) {
        assertTrue(no != null && no.isNull(), mensagem + " — obtido " + no);
    }

    // ---------------------------------------------------------------- verbos HTTP

    Resposta get(String caminho) {
        return enviar(requisicao(caminho).GET());
    }

    /** Solicita a posição no contrato da opção B e aguarda a entrega consumível. */
    Resposta posicao(String data) {
        Resposta solicitacao = get("/posicao?data=" + data);
        assertTrue(solicitacao.status() == 202, "a solicitação assíncrona foi aceita");
        long id = solicitacao.json().get("id").asLong();
        Instant limite = Instant.now().plus(Duration.ofSeconds(30));
        while (Instant.now().isBefore(limite)) {
            Resposta resultado = get("/posicao/" + id);
            if (resultado.status() == 200) return resultado;
            assertEquals(425, resultado.status(), "a posição só pode estar pendente ou concluída");
            Thread.onSpinWait();
        }
        throw new AssertionError("a posição não concluiu dentro do limite");
    }

    Resposta post(String caminho, String corpo) {
        return enviar(requisicao(caminho)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(corpo)));
    }

    Resposta put(String caminho, String corpo) {
        return enviar(requisicao(caminho)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(corpo)));
    }

    Resposta delete(String caminho) {
        return enviar(requisicao(caminho).DELETE());
    }

    /**
     * Credencial usada por padrão. É {@link ThreadLocal} porque os casos de concorrência trocam de
     * usuário dentro de threads paralelas: um campo de instância seria disputado entre elas e faria
     * uma requisição sair com a credencial de outra — um falso negativo do harness, não da aplicação.
     */
    private final ThreadLocal<java.util.Optional<String>> credencial =
            ThreadLocal.withInitial(() -> java.util.Optional.of(basic("usuario0", "senha0")));

    /** Executa o trecho autenticado como outro usuário, restaurando a credencial anterior ao final. */
    void como(String login, String senha, Runnable acao) {
        comCredencial(java.util.Optional.of(basic(login, senha)), () -> {
            acao.run();
            return null;
        });
    }

    void semCredencial(Runnable acao) {
        comCredencial(java.util.Optional.empty(), () -> {
            acao.run();
            return null;
        });
    }

    /**
     * {@code Optional.empty()} representa "sem credencial" de propósito: remover o valor do
     * {@link ThreadLocal} faria o próximo acesso recair no valor inicial, e o teste de rota
     * desprotegida passaria a enviar a credencial padrão sem que ninguém percebesse.
     */
    private <T> T comCredencial(java.util.Optional<String> nova, java.util.function.Supplier<T> acao) {
        java.util.Optional<String> anterior = credencial.get();
        credencial.set(nova);
        try {
            return acao.get();
        } finally {
            credencial.set(anterior);
        }
    }

    static String basic(String login, String senha) {
        return "Basic " + java.util.Base64.getEncoder()
                .encodeToString((login + ":" + senha).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private HttpRequest.Builder requisicao(String caminho) {
        HttpRequest.Builder builder = HttpRequest
                .newBuilder(URI.create("http://localhost:" + porta + caminho))
                .timeout(Duration.ofSeconds(30));
        credencial.get().ifPresent(autorizacao -> builder.header("Authorization", autorizacao));
        return builder;
    }

    private Resposta enviar(HttpRequest.Builder builder) {
        try {
            HttpRequest requisicao = builder.build();
            HttpResponse<String> resposta = http.send(requisicao, HttpResponse.BodyHandlers.ofString());
            registrarEvidencia(requisicao, resposta);
            return new Resposta(resposta.statusCode(), resposta.body());
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Falha de rede ao chamar a API sob teste.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Chamada à API interrompida.", e);
        }
    }

    // ---------------------------------------------------------------- evidência

    private static final Pattern IDENTIFICADOR_DE_CASO = Pattern.compile("TC-\\d{3}(?:/\\d{3})*");

    static List<String> identificadoresDeCaso(String displayName) {
        Matcher achado = IDENTIFICADOR_DE_CASO.matcher(displayName);
        List<String> identificadores = new ArrayList<>();
        while (achado.find()) {
            String[] casos = achado.group().split("/");
            identificadores.add(casos[0]);
            for (int indice = 1; indice < casos.length; indice++) {
                identificadores.add("TC-" + casos[indice]);
            }
        }
        return identificadores.isEmpty() ? List.of("sem-tc") : List.copyOf(identificadores);
    }

    /** Identifica a versão exercitada, para que a evidência não fique órfã do código que a produziu. */
    private static final String COMMIT = commitAtual();

    /**
     * Publica request e response de cada chamada em
     * {@code artifacts/testes/<commit>/<tc-id>/requisicoes.jsonl}, que é o caminho que o pipeline
     * `testes-integracao` recolhe como artefato e o PR referencia.
     * <p>
     * O cabeçalho {@code Authorization} é substituído por um marcador: evidência de teste é publicada
     * como artefato de CI, e credencial em artefato é credencial vazada.
     */
    private void registrarEvidencia(HttpRequest requisicao, HttpResponse<String> resposta) {
        try {
            String linha = """
                    {"metodo":"%s","caminho":"%s","autenticado":%s,"status":%d,"corpoDaResposta":%s}
                    """.formatted(
                    requisicao.method(),
                    requisicao.uri().getPath() + (requisicao.uri().getQuery() == null ? ""
                            : "?" + requisicao.uri().getQuery()),
                    requisicao.headers().firstValue("Authorization").isPresent(),
                    resposta.statusCode(),
                    JSON.writeValueAsString(resposta.body()));
            for (String caso : casosAtuais) {
                Path pasta = Path.of("..", "artifacts", "testes", COMMIT, caso);
                Files.createDirectories(pasta);
                Files.writeString(pasta.resolve("requisicoes.jsonl"), linha,
                        StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
        } catch (java.io.IOException e) {
            // Evidência é registro, não asserção: falhar aqui esconderia o resultado real do caso.
            System.err.println("Não foi possível registrar evidência de " + casosAtuais + ": " + e.getMessage());
        }
    }

    private static String commitAtual() {
        String doCi = System.getenv("GITHUB_SHA");
        if (doCi != null && !doCi.isBlank()) {
            return doCi;
        }
        try {
            Process processo = new ProcessBuilder("git", "rev-parse", "--short", "HEAD")
                    .redirectErrorStream(true).start();
            String saida = new String(processo.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            return saida.isBlank() ? "local" : saida;
        } catch (java.io.IOException e) {
            return "local";
        }
    }

    // ---------------------------------------------------------------- fixture

    /** Código de ativo exclusivo do caso, para que dois casos nunca disputem a mesma linha. */
    String codigoDeAtivo(String caso) {
        return caso.replace("-", "") + SEQUENCIA.incrementAndGet();
    }

    /** Cria um ativo negociável em toda a década, para os casos que não testam a janela. */
    Resposta criarAtivo(String codigo) {
        return criarAtivo(codigo, "RV", "2020-01-01", "2030-01-01");
    }

    Resposta criarAtivo(String codigo, String tipo, String emissao, String vencimento) {
        return comoAdministrador(() -> post("/ativos", """
                {"ativo":"%s","nome":"Ativo %s","tipo":"%s","dataEmissao":"%s","dataVencimento":"%s"}
                """.formatted(codigo, codigo, tipo, emissao, vencimento)));
    }

    Resposta definirPreco(String codigo, String data, String preco) {
        return comoAdministrador(() -> put("/ativos/" + codigo + "/precos/" + data, """
                {"precoMercado":%s}""".formatted(preco)));
    }

    Resposta alterarAtivo(String codigo, String corpo) {
        return comoAdministrador(() -> put("/ativos/" + codigo, corpo));
    }

    Resposta removerAtivo(String codigo) {
        return comoAdministrador(() -> delete("/ativos/" + codigo));
    }

    Resposta removerPreco(String codigo, String data) {
        return comoAdministrador(() -> delete("/ativos/" + codigo + "/precos/" + data));
    }

    /**
     * Ativos e preços são acervo compartilhado e só o administrador os gerencia (D-C1). O preparo de
     * fixture usa essa credencial; o caso de teste continua rodando como usuário comum.
     */
    Resposta comoAdministrador(java.util.function.Supplier<Resposta> acao) {
        return comCredencial(java.util.Optional.of(basic("root", "spiderman")), acao);
    }

    Resposta credito(String valor, String data) {
        return post("/contacorrente/credito", """
                {"valor":%s,"descricao":"crédito de teste","data":"%s"}""".formatted(valor, data));
    }

    Resposta debito(String valor, String data) {
        return post("/contacorrente/debito", """
                {"valor":%s,"descricao":"débito de teste","data":"%s"}""".formatted(valor, data));
    }

    Resposta comprar(String codigo, String data, String quantidade, String valor) {
        return post("/movimentacao/compra", movimento(codigo, data, quantidade, valor));
    }

    Resposta vender(String codigo, String data, String quantidade, String valor) {
        return post("/movimentacao/venda", movimento(codigo, data, quantidade, valor));
    }

    static String movimento(String codigo, String data, String quantidade, String valor) {
        return """
                {"ativo":"%s","data":"%s","quantidade":%s,"valor":%s}"""
                .formatted(codigo, data, quantidade, valor);
    }

    /**
     * Limpeza própria de cada caso. Roda mesmo quando o teste falha, de modo que uma falha não
     * contamina o caso seguinte nem a reexecução da suíte.
     */
    @AfterEach
    void limparDados() {
        db.update("DELETE FROM lancamento");
        db.update("DELETE FROM movimentacao");
        // Os ativos ATIVO0..ATIVO127 e seus preços são o ambiente pré-cadastrado que o enunciado
        // exige: a limpeza remove só o que o caso criou, e a exclusão em cascata leva junto os
        // preços desses ativos.
        db.update("DELETE FROM ativo WHERE codigo NOT LIKE 'ATIVO%'");
    }
}
