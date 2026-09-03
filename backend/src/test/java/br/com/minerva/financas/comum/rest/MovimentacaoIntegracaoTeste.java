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
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Compra, venda e consulta (TC-017 a TC-024) e as regras temporais TC-042 e TC-046 a TC-048. */
class MovimentacaoIntegracaoTeste extends TesteIntegracao {

    /** 2020-02-28 é uma sexta-feira; 2020-02-29 é sábado. */
    private static final String SEXTA = "2020-02-28";
    private static final String SABADO = "2020-02-29";

    /** Ativo com saldo e preço prontos, para os casos que não estão testando o preparo. */
    private String ativoComSaldo(String caso) {
        String codigo = codigoDeAtivo(caso);
        criarAtivo(codigo);
        definirPreco(codigo, "2020-01-02", "10.00");
        credito("1000.00", "2020-01-02");
        return codigo;
    }

    @Test
    @DisplayName("TC-017 compra válida retorna 201 sem corpo")
    void tc017() {
        String codigo = ativoComSaldo("TC-017");

        Resposta resposta = comprar(codigo, SEXTA, "2.50", "105.53");

        assertEquals(201, resposta.status());
        assertTrue(resposta.corpo().isEmpty());
    }

    @Test
    @DisplayName("TC-018 venda válida retorna 201 sem corpo")
    void tc018() {
        String codigo = ativoComSaldo("TC-018");
        comprar(codigo, SEXTA, "2.50", "105.53");

        Resposta resposta = vender(codigo, SEXTA, "1.00", "50.00");

        assertEquals(201, resposta.status());
        assertTrue(resposta.corpo().isEmpty());
    }

    @Test
    @DisplayName("TC-019 compra debita e venda credita a conta, atomicamente")
    void tc019() {
        String codigo = ativoComSaldo("TC-019");

        comprar(codigo, SEXTA, "2.00", "100.00");
        assertEquals("{\"saldo\":900.00}", get("/contacorrente/saldo?data=" + SEXTA).corpo());

        vender(codigo, SEXTA, "1.00", "70.00");
        assertEquals("{\"saldo\":970.00}", get("/contacorrente/saldo?data=" + SEXTA).corpo());

        Resposta lancamentos = get("/contacorrente/lancamentos?dataInicio=2020-01-01&dataFim=" + SEXTA);
        assertEquals(3, lancamentos.json().size(), "crédito inicial e os dois lançamentos espelho");
        assertEquals("Compra " + codigo, lancamentos.item(1).get("descricao").asString());
        assertEquals("Venda " + codigo, lancamentos.item(2).get("descricao").asString());
    }

    @Test
    @DisplayName("TC-020 compra sem saldo retorna 409 e não gera lançamento")
    void tc020() {
        String codigo = codigoDeAtivo("TC-020");
        criarAtivo(codigo);
        credito("10.00", "2020-01-02");

        Resposta resposta = comprar(codigo, SEXTA, "1.00", "10.01");

        assertEquals(409, resposta.status());
        assertEquals("SALDO_INSUFICIENTE", resposta.codigoDeErro());
        assertEquals("{\"saldo\":10.00}", get("/contacorrente/saldo?data=" + SEXTA).corpo());
        assertEquals(1, get("/contacorrente/lancamentos?dataInicio=2020-01-01&dataFim=" + SEXTA).json().size());
    }

    @Test
    @DisplayName("TC-021 venda acima da quantidade retorna 409 sem efeito parcial")
    void tc021() {
        String codigo = ativoComSaldo("TC-021");
        comprar(codigo, SEXTA, "1.00", "10.00");

        Resposta resposta = vender(codigo, SEXTA, "1.01", "10.00");

        assertEquals(409, resposta.status());
        assertEquals("QUANTIDADE_INSUFICIENTE", resposta.codigoDeErro());
        assertEquals(1, get("/movimentacao?dataInicio=2020-01-01&dataFim=" + SEXTA).json().size(),
                "a movimentação recusada não foi gravada");
    }

    @Test
    @DisplayName("TC-022 movimentar ativo inexistente retorna 404")
    void tc022() {
        Resposta resposta = comprar("NAO-EXISTE-TC022", SEXTA, "1.00", "10.00");

        assertEquals(404, resposta.status());
        assertEquals("ATIVO_NAO_ENCONTRADO", resposta.codigoDeErro());
    }

    @Test
    @DisplayName("TC-023 quantidade zero, negativa ou com mais de duas casas retorna 400")
    void tc023() {
        String codigo = ativoComSaldo("TC-023");

        assertEquals(400, comprar(codigo, SEXTA, "0", "10.00").status());
        assertEquals(400, comprar(codigo, SEXTA, "-1.00", "10.00").status());
        assertEquals(400, comprar(codigo, SEXTA, "1.001", "10.00").status());
    }

    @Test
    @DisplayName("TC-024 vendas concorrentes não vendem a mesma quantidade duas vezes")
    void tc024() throws Exception {
        String codigo = ativoComSaldo("TC-024");
        comprar(codigo, SEXTA, "1.00", "10.00");

        List<Integer> status = emParalelo(4, () -> vender(codigo, SEXTA, "1.00", "10.00").status());

        assertEquals(1, status.stream().filter(s -> s == 201).count(),
                "só uma venda cabe na quantidade existente: " + status);
        assertEquals(3, status.stream().filter(s -> s == 409).count(), "as demais conflitam: " + status);
    }

    @Test
    @DisplayName("TC-042 filtro de movimentações é inclusivo nos dois limites")
    void tc042() {
        String codigo = ativoComSaldo("TC-042");
        comprar(codigo, "2020-02-10", "1.00", "10.00");
        comprar(codigo, "2020-02-20", "1.00", "10.00");
        comprar(codigo, "2020-02-25", "1.00", "10.00");

        Resposta resposta = get("/movimentacao?dataInicio=2020-02-10&dataFim=2020-02-20");

        assertEquals(200, resposta.status());
        assertEquals(2, resposta.json().size());
        assertEquals("2020-02-10", resposta.item(0).get("data").asString());
        assertEquals("2020-02-20", resposta.item(1).get("data").asString());
    }

    @Test
    @DisplayName("TC-046 a emissão é inclusiva e o vencimento é exclusivo")
    void tc046() {
        String codigo = codigoDeAtivo("TC-046");
        criarAtivo(codigo, "RV", "2020-02-03", "2020-02-14");
        definirPreco(codigo, "2020-01-02", "10.00");
        credito("1000.00", "2020-01-02");

        assertEquals(201, comprar(codigo, "2020-02-03", "1.00", "10.00").status(),
                "o dia da emissão é negociável");

        Resposta noVencimento = comprar(codigo, "2020-02-14", "1.00", "10.00");
        assertEquals(400, noVencimento.status(), "o dia do vencimento não é negociável");
        assertEquals("FORA_DA_JANELA", noVencimento.codigoDeErro());

        assertEquals(400, comprar(codigo, "2020-01-31", "1.00", "10.00").status(), "antes da emissão");
    }

    @Test
    @DisplayName("TC-047 fim de semana retorna 400 com código próprio")
    void tc047() {
        String codigo = ativoComSaldo("TC-047");

        Resposta resposta = comprar(codigo, SABADO, "1.00", "10.00");

        assertEquals(400, resposta.status());
        assertEquals("DATA_NAO_UTIL", resposta.codigoDeErro());
    }

    @Test
    @DisplayName("TC-048 inserção fora de ordem preserva a não negatividade histórica")
    void tc048() {
        String codigo = ativoComSaldo("TC-048");
        comprar(codigo, "2020-02-20", "5.00", "500.00");
        vender(codigo, "2020-02-25", "5.00", "500.00");

        Resposta retroativa = vender(codigo, "2020-02-21", "3.00", "300.00");

        assertEquals(409, retroativa.status(),
                "vender em 21/02 deixaria a quantidade negativa em 25/02, quando as 5 unidades já saíram");
        assertEquals("QUANTIDADE_INSUFICIENTE", retroativa.codigoDeErro());

        Resposta debitoRetroativo = post("/contacorrente/debito",
                "{\"valor\":900.00,\"descricao\":\"retroativo\",\"data\":\"2020-02-21\"}");
        assertEquals(409, debitoRetroativo.status(),
                "o saldo em 20/02, já descontada a compra, não comporta esse débito");
    }

    private static <T> List<T> emParalelo(int vezes, Callable<T> acao) throws Exception {
        try (ExecutorService pool = Executors.newFixedThreadPool(vezes)) {
            List<Future<T>> futuros = new ArrayList<>();
            for (int i = 0; i < vezes; i++) {
                futuros.add(pool.submit(acao));
            }
            List<T> resultados = new ArrayList<>();
            for (Future<T> futuro : futuros) {
                resultados.add(futuro.get());
            }
            return resultados;
        }
    }
}
