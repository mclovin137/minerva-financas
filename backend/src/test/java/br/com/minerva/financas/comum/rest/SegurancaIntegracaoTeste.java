package br.com.minerva.financas.comum.rest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Opção A: autenticação, capacidades e isolamento entre usuários. TC-065 a TC-072. */
class SegurancaIntegracaoTeste extends TesteIntegracao {

    private static final String DIA = "2020-02-28";

    @Test
    @DisplayName("TC-065 Basic válido permite o usuário comum operar")
    void tc065() {
        como("usuario3", "senha3", () -> {
            assertEquals(201, credito("100.00", DIA).status());
            assertEquals("{\"saldo\":100.00}", get("/contacorrente/saldo?data=" + DIA).corpo());
        });
    }

    @Test
    @DisplayName("TC-066 Basic ausente, malformado ou com senha errada retorna 401")
    void tc066() {
        semCredencial(() -> {
            Resposta resposta = get("/contacorrente/saldo?data=" + DIA);
            assertEquals(401, resposta.status());
            assertEquals("NAO_AUTENTICADO", resposta.codigoDeErro());
        });

        como("usuario0", "senha-errada", () -> assertEquals(401, get("/ativos").status()));
        como("nao-existe", "qualquer", () -> assertEquals(401, get("/ativos").status()));
    }

    @Test
    @DisplayName("TC-067 root administra ativo e preço de mercado")
    void tc067() {
        String codigo = codigoDeAtivo("TC-067");

        como("root", "spiderman", () -> {
            assertEquals(201, post("/ativos", """
                    {"ativo":"%s","nome":"Ativo %s","tipo":"RV",
                     "dataEmissao":"2020-01-01","dataVencimento":"2030-01-01"}"""
                    .formatted(codigo, codigo)).status());
            assertEquals(201, put("/ativos/" + codigo + "/precos/2020-01-02",
                    "{\"precoMercado\":10.00}").status());
            assertEquals(200, get("/ativos/" + codigo).status());
        });
    }

    @Test
    @DisplayName("TC-068 usuário comum sem capacidade administrativa retorna 403")
    void tc068() {
        String codigo = codigoDeAtivo("TC-068");
        criarAtivo(codigo);

        como("usuario1", "senha1", () -> {
            Resposta criacao = post("/ativos", """
                    {"ativo":"OUTRO","nome":"Outro","tipo":"RV",
                     "dataEmissao":"2020-01-01","dataVencimento":"2030-01-01"}""");
            assertEquals(403, criacao.status());
            assertEquals("CAPACIDADE_NEGADA", criacao.codigoDeErro());

            assertEquals(403, delete("/ativos/" + codigo).status());
            assertEquals(403, put("/ativos/" + codigo + "/precos/2020-01-02",
                    "{\"precoMercado\":10.00}").status());
            assertEquals(200, get("/ativos").status(), "a leitura de ativos é compartilhada");
        });
    }

    @Test
    @DisplayName("TC-069 root não transaciona nem consulta dados confidenciais")
    void tc069() {
        como("root", "spiderman", () -> {
            assertEquals(403, credito("10.00", DIA).status());
            assertEquals(403, debito("10.00", DIA).status());
            assertEquals(403, get("/contacorrente/saldo?data=" + DIA).status());
            assertEquals(403, get("/contacorrente/lancamentos?dataInicio=" + DIA + "&dataFim=" + DIA).status());
            assertEquals(403, get("/posicao?data=" + DIA).status());
            assertEquals(403, get("/movimentacao?dataInicio=" + DIA + "&dataFim=" + DIA).status());
        });
    }

    @Test
    @DisplayName("TC-070 um usuário não enxerga os dados financeiros do outro")
    void tc070() {
        String codigo = codigoDeAtivo("TC-070");
        criarAtivo(codigo);
        definirPreco(codigo, "2020-01-02", "10.00");

        como("usuario4", "senha4", () -> {
            credito("500.00", DIA);
            comprar(codigo, DIA, "1.00", "10.00");
        });

        // usuario5 é reservado pelos testes de backpressure da opção B; usar o usuário padrão
        // evita que execuções retidas de outro caso contaminem este teste de isolamento.
        como("usuario0", "senha0", () -> {
            assertEquals("{\"saldo\":0.00}", get("/contacorrente/saldo?data=" + DIA).corpo(),
                    "o saldo do usuario4 não vaza para o usuario5");
            assertEquals(0, get("/contacorrente/lancamentos?dataInicio=2020-01-01&dataFim=" + DIA)
                    .json().size());
            assertEquals(0, posicao(DIA).json().size());
            assertFalse(get("/movimentacao?dataInicio=2020-01-01&dataFim=" + DIA).corpo().contains(codigo));
        });

        como("usuario4", "senha4", () ->
                assertEquals("{\"saldo\":490.00}", get("/contacorrente/saldo?data=" + DIA).corpo(),
                        "e o dono continua vendo os próprios dados"));
    }

    @Test
    @DisplayName("TC-071 todas as rotas de API exigem Basic")
    void tc071() {
        List<String[]> rotas = List.of(
                new String[]{"GET", "/ativos"},
                new String[]{"GET", "/ativos/ATIVO0"},
                new String[]{"POST", "/ativos"},
                new String[]{"POST", "/contacorrente/credito"},
                new String[]{"POST", "/contacorrente/debito"},
                new String[]{"GET", "/contacorrente/saldo?data=" + DIA},
                new String[]{"GET", "/contacorrente/lancamentos?dataInicio=" + DIA + "&dataFim=" + DIA},
                new String[]{"POST", "/movimentacao/compra"},
                new String[]{"POST", "/movimentacao/venda"},
                new String[]{"GET", "/movimentacao?dataInicio=" + DIA + "&dataFim=" + DIA},
                new String[]{"GET", "/posicao?data=" + DIA},
                new String[]{"GET", "/posicao/1"},
                // As rotas administrativas também exigem Basic. Sem elas na lista, o caso afirmaria
                // cobrir todas as rotas de API e passaria mesmo se uma delas ficasse aberta.
                new String[]{"PUT", "/ativos/ATIVO0"},
                new String[]{"DELETE", "/ativos/ATIVO0"},
                new String[]{"PUT", "/ativos/ATIVO0/precos/2020-01-02"},
                new String[]{"DELETE", "/ativos/ATIVO0/precos/2020-01-02"});

        semCredencial(() -> {
            for (String[] rota : rotas) {
                Resposta resposta = switch (rota[0]) {
                    case "GET" -> get(rota[1]);
                    case "POST" -> post(rota[1], "{}");
                    case "PUT" -> put(rota[1], "{}");
                    case "DELETE" -> delete(rota[1]);
                    default -> throw new IllegalStateException("método não previsto: " + rota[0]);
                };
                assertEquals(401, resposta.status(), rota[0] + " " + rota[1] + " deve exigir Basic");
            }
        });
    }

    @Test
    @DisplayName("arquivos estáticos da interface respondem sem exigir Basic, por exceção intencional")
    void arquivosEstaticosNaoExigemBasic() {
        List<String> caminhos = List.of("/", "/index.html", "/favicon.ico");

        semCredencial(() -> {
            for (String caminho : caminhos) {
                Resposta resposta = get(caminho);
                assertFalse(resposta.status() == 401,
                        caminho + " é arquivo estático sem dado do usuário; não deve exigir Basic");
            }
        });
    }

    @Test
    @DisplayName("TC-072 operações concorrentes de dois usuários mantêm isolamento e saldo")
    void tc072() throws Exception {
        String codigo = codigoDeAtivo("TC-072");
        criarAtivo(codigo);
        definirPreco(codigo, "2020-01-02", "10.00");
        como("usuario6", "senha6", () -> credito("100.00", DIA));
        como("usuario7", "senha7", () -> credito("100.00", DIA));

        List<Integer> status = emParalelo(List.of(
                () -> creditoDe("usuario6", "senha6", "10.00"),
                () -> creditoDe("usuario7", "senha7", "20.00"),
                () -> creditoDe("usuario6", "senha6", "10.00"),
                () -> creditoDe("usuario7", "senha7", "20.00")));

        assertTrue(status.stream().allMatch(s -> s == 201), "todos os créditos são aceitos: " + status);
        como("usuario6", "senha6", () -> assertEquals("{\"saldo\":120.00}",
                get("/contacorrente/saldo?data=" + DIA).corpo()));
        como("usuario7", "senha7", () -> assertEquals("{\"saldo\":140.00}",
                get("/contacorrente/saldo?data=" + DIA).corpo()));
    }

    private int creditoDe(String login, String senha, String valor) {
        int[] status = new int[1];
        como(login, senha, () -> status[0] = credito(valor, DIA).status());
        return status[0];
    }

    private static List<Integer> emParalelo(List<Callable<Integer>> acoes) throws Exception {
        try (ExecutorService pool = Executors.newFixedThreadPool(acoes.size())) {
            List<Future<Integer>> futuros = new ArrayList<>();
            for (Callable<Integer> acao : acoes) {
                futuros.add(pool.submit(acao));
            }
            List<Integer> resultados = new ArrayList<>();
            for (Future<Integer> futuro : futuros) {
                resultados.add(futuro.get());
            }
            return resultados;
        }
    }
}
