package br.com.minerva.financas.comum.rest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contrato canônico do nível 3: as cinco rotas que o enunciado fixa literalmente, com os payloads
 * exatos dos exemplos. TC-057 a TC-064.
 */
class ContratoN3IntegracaoTeste extends TesteIntegracao {

    /** O ativo do exemplo do enunciado existe no seed, e 2020-02-28 é uma sexta-feira. */
    private static final String DIA = "2020-02-28";

    @Autowired
    private JdbcTemplate db;

    @Test
    @DisplayName("TC-057 as cinco rotas canônicas aceitam o payload literal do enunciado")
    void tc057() {
        assertEquals(201, post("/contacorrente/credito", """
                {
                  "valor": 12.42,
                  "descricao": "alguma coisa",
                  "data": "2020-02-28"
                }""").status());

        assertEquals(201, post("/contacorrente/debito", """
                {
                  "valor": 12.42,
                  "descricao": "alguma coisa",
                  "data": "2020-02-28"
                }""").status());

        credito("1000.00", DIA);

        assertEquals(201, post("/movimentacao/compra", """
                {
                  "ativo": "ATIVO1",
                  "data": "2020-02-28",
                  "quantidade": 2.5,
                  "valor": 105.53
                }""").status());

        assertEquals(201, post("/movimentacao/venda", """
                {
                  "ativo": "ATIVO1",
                  "data": "2020-02-28",
                  "quantidade": 2.5,
                  "valor": 105.53
                }""").status());

        assertEquals(200, get("/contacorrente/saldo?data=2020-02-28").status());
    }

    @Test
    @DisplayName("TC-058 os POSTs canônicos respondem 201 sem corpo")
    void tc058() {
        credito("1000.00", DIA);

        // Os quatro POSTs canônicos, não uma amostra deles: o enunciado diz que nenhum precisa
        // responder conteúdo, e um que respondesse quebraria o contrato sem ser notado.
        Map<String, Resposta> respostas = new LinkedHashMap<>();
        respostas.put("credito", credito("10.00", DIA));
        respostas.put("debito", debito("5.00", DIA));
        respostas.put("compra", comprar("ATIVO2", DIA, "1.00", "10.00"));
        respostas.put("venda", vender("ATIVO2", DIA, "1.00", "12.00"));

        respostas.forEach((rota, resposta) -> {
            assertEquals(201, resposta.status(), rota + " responde 201");
            assertTrue(resposta.corpo().isEmpty(), rota + " responde sem corpo");
        });
    }

    @Test
    @DisplayName("TC-059 o saldo responde exatamente no formato do enunciado")
    void tc059() {
        credito("1234.56", DIA);

        Resposta resposta = get("/contacorrente/saldo?data=2020-02-28");

        assertEquals(200, resposta.status());
        assertEquals("{\"saldo\":1234.56}", resposta.corpo());
    }

    @Test
    @DisplayName("TC-060 JSON malformado ou caminho inválido não vira erro de servidor")
    void tc060() {
        assertEquals(400, post("/contacorrente/credito", "{\"valor\":").status());
        assertEquals(400, post("/contacorrente/credito", "").status());
        assertEquals(404, get("/rota/que/nao/existe").status());
    }

    @Test
    @DisplayName("TC-061 conflito financeiro retorna 409")
    void tc061() {
        credito("10.00", DIA);

        Resposta resposta = debito("10.01", DIA);

        assertEquals(409, resposta.status());
        assertEquals("SALDO_INSUFICIENTE", resposta.codigoDeErro());
    }

    @Test
    @DisplayName("TC-062 a resposta de erro não revela SQL, stack trace nem caminho de arquivo")
    void tc062() {
        List<Resposta> erros = List.of(
                post("/contacorrente/credito", "{\"valor\":"),
                debito("999999.00", DIA),
                get("/ativos/NAO-EXISTE-TC062"),
                get("/contacorrente/lancamentos?dataInicio=xx&dataFim=" + DIA));

        for (Resposta erro : erros) {
            String corpo = erro.corpo();
            assertTrue(erro.status() >= 400, "todas são respostas de erro");
            assertFalse(corpo.contains("SELECT") || corpo.contains("INSERT"), "sem SQL: " + corpo);
            assertFalse(corpo.contains("java."), "sem stack trace nem classe interna: " + corpo);
            assertFalse(corpo.contains("/mnt/") || corpo.contains("\\\\"), "sem caminho de arquivo: " + corpo);
            assertFalse(corpo.toLowerCase().contains("senha"), "sem credencial: " + corpo);
            assertTrue(corpo.contains("\"codigo\""), "e sempre no contrato de erro único: " + corpo);
        }
    }

    @Test
    @DisplayName("TC-063 requisições simultâneas preservam as invariantes financeiras")
    void tc063() throws Exception {
        credito("100.00", DIA);

        List<Integer> status = emParalelo(20, () -> debito("10.00", DIA).status());

        assertEquals(10, status.stream().filter(s -> s == 201).count(),
                "exatamente dez débitos de R$ 10,00 cabem em R$ 100,00: " + status);
        assertTrue(status.stream().allMatch(s -> s == 201 || s == 409),
                "nenhuma requisição termina em erro de servidor: " + status);
        assertEquals("{\"saldo\":0.00}", get("/contacorrente/saldo?data=" + DIA).corpo());
    }

    @Test
    @DisplayName("TC-064 o seed contém ATIVO0 a ATIVO127 com preço de mercado em 2020-01-02")
    void tc064() {
        Integer ativos = db.queryForObject(
                "SELECT COUNT(*) FROM ativo WHERE codigo LIKE 'ATIVO%'", Integer.class);
        Integer precos = db.queryForObject("""
                SELECT COUNT(*) FROM valor_mercado vm
                  JOIN ativo a ON a.id = vm.ativo_id
                 WHERE a.codigo LIKE 'ATIVO%' AND vm.data = '2020-01-02'
                """, Integer.class);

        assertEquals(128, ativos, "ATIVO0 até ATIVO127");
        assertEquals(128, precos, "todos com valor de mercado para o dia 2020-01-02");
        assertEquals(404, get("/ativos/ATIVO128").status(), "o seed para em 127");

        // Os valores seguem a regra determinística documentada no README, porque o enunciado fixa
        // apenas os nomes e a data. Verificá-los aqui é o que impede a regra de mudar em silêncio.
        conferirAtivoDoSeed("ATIVO0", "Ativo 0", "RV", 1000L);
        conferirAtivoDoSeed("ATIVO1", "Ativo 1", "RF", 1037L);
        conferirAtivoDoSeed("ATIVO2", "Ativo 2", "FUNDO", 1074L);
        conferirAtivoDoSeed("ATIVO127", "Ativo 127", "RF", 5699L);
    }

    /** @param precoEmCentavos preço esperado em 2020-01-02, pela regra 10,00 + índice × 0,37 */
    private void conferirAtivoDoSeed(String codigo, String nome, String tipo, long precoEmCentavos) {
        Resposta ativo = get("/ativos/" + codigo);
        assertEquals(200, ativo.status(), codigo + " existe");
        assertEquals(nome, ativo.json().get("nome").asString(), codigo + ": nome");
        assertEquals(tipo, ativo.json().get("tipo").asString(), codigo + ": tipo cicla RV, RF e FUNDO");
        assertEquals("2020-01-01", ativo.json().get("dataEmissao").asString(), codigo + ": emissão");
        assertEquals("2030-01-01", ativo.json().get("dataVencimento").asString(), codigo + ": vencimento");

        Long armazenado = db.queryForObject("""
                SELECT vm.preco_mercado_e8 FROM valor_mercado vm
                  JOIN ativo a ON a.id = vm.ativo_id
                 WHERE a.codigo = ? AND vm.data = '2020-01-02'
                """, Long.class, codigo);
        assertEquals(precoEmCentavos * 1_000_000L, armazenado, codigo + ": preço em 2020-01-02");
    }

    private static List<Integer> emParalelo(int vezes, Callable<Integer> acao) throws Exception {
        try (ExecutorService pool = Executors.newFixedThreadPool(vezes)) {
            List<Future<Integer>> futuros = new ArrayList<>();
            for (int i = 0; i < vezes; i++) {
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
