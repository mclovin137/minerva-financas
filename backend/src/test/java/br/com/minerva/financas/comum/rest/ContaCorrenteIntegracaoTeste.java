package br.com.minerva.financas.comum.rest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TC-001 a TC-008 e os filtros temporais de lançamentos: TC-041 e TC-043 a TC-045. */
class ContaCorrenteIntegracaoTeste extends TesteIntegracao {

    private static final String DIA = "2020-02-28";

    @Test
    @DisplayName("TC-001 crédito válido retorna 201 sem corpo")
    void tc001() {
        Resposta resposta = credito("12.42", DIA);

        assertEquals(201, resposta.status());
        assertTrue(resposta.corpo().isEmpty(), "o contrato do POST canônico não devolve corpo");
    }

    @Test
    @DisplayName("TC-002 débito válido reduz o saldo e retorna 201")
    void tc002() {
        credito("100.00", DIA);

        assertEquals(201, debito("30.00", DIA).status());
        assertEquals("{\"saldo\":70.00}", get("/contacorrente/saldo?data=" + DIA).corpo());
    }

    @Test
    @DisplayName("TC-003 saldo atualizado retorna 200 e JSON no formato do enunciado")
    void tc003() {
        credito("1234.56", DIA);

        Resposta resposta = get("/contacorrente/saldo?data=" + DIA);

        assertEquals(200, resposta.status());
        assertEquals("{\"saldo\":1234.56}", resposta.corpo(),
                "o corpo é literalmente o do enunciado, com duas casas preservadas");
    }

    @Test
    @DisplayName("TC-004 débito que zera o saldo é aceito")
    void tc004() {
        credito("50.00", DIA);

        assertEquals(201, debito("50.00", DIA).status());
        assertEquals("{\"saldo\":0.00}", get("/contacorrente/saldo?data=" + DIA).corpo());
    }

    @Test
    @DisplayName("TC-005 saldo insuficiente retorna 409 e não altera o saldo")
    void tc005() {
        credito("10.00", DIA);

        Resposta resposta = debito("10.01", DIA);

        assertEquals(409, resposta.status());
        assertEquals("SALDO_INSUFICIENTE", resposta.codigoDeErro());
        assertEquals("{\"saldo\":10.00}", get("/contacorrente/saldo?data=" + DIA).corpo());
    }

    @Test
    @DisplayName("TC-006 valor ausente, zero, negativo ou com escala excedente retorna 400")
    void tc006() {
        List<String> corposInvalidos = List.of(
                "{\"descricao\":\"sem valor\",\"data\":\"" + DIA + "\"}",
                "{\"valor\":0,\"descricao\":\"zero\",\"data\":\"" + DIA + "\"}",
                "{\"valor\":-5.00,\"descricao\":\"negativo\",\"data\":\"" + DIA + "\"}",
                "{\"valor\":1.001,\"descricao\":\"três casas\",\"data\":\"" + DIA + "\"}");

        for (String corpo : corposInvalidos) {
            Resposta resposta = post("/contacorrente/credito", corpo);
            assertEquals(400, resposta.status(), "corpo recusado esperado: " + corpo);
            assertEquals("CAMPO_INVALIDO", resposta.codigoDeErro());
        }
    }

    @Test
    @DisplayName("TC-007 descrição ausente ou em branco retorna 400")
    void tc007() {
        Resposta ausente = post("/contacorrente/credito", "{\"valor\":10.00,\"data\":\"" + DIA + "\"}");
        Resposta branca = post("/contacorrente/credito",
                "{\"valor\":10.00,\"descricao\":\"   \",\"data\":\"" + DIA + "\"}");

        assertEquals(400, ausente.status());
        assertEquals(400, branca.status());
        assertEquals("CAMPO_INVALIDO", ausente.codigoDeErro());
    }

    @Test
    @DisplayName("TC-008 débitos concorrentes nunca deixam o saldo negativo")
    void tc008() throws Exception {
        credito("5.00", DIA);

        List<Integer> status = emParalelo(10, () -> debito("1.00", DIA).status());

        long aceitos = status.stream().filter(s -> s == 201).count();
        assertTrue(status.stream().allMatch(s -> s == 201 || s == 409),
                "toda tentativa termina aceita ou recusada por saldo, nunca em erro: " + status);
        assertEquals(5, aceitos, "exatamente cinco débitos de R$ 1,00 cabem em R$ 5,00");
        assertEquals("{\"saldo\":0.00}", get("/contacorrente/saldo?data=" + DIA).corpo());
    }

    @Test
    @DisplayName("TC-041 filtro de lançamentos é inclusivo nos dois limites")
    void tc041() {
        credito("10.00", "2020-02-10");
        credito("20.00", "2020-02-20");
        credito("30.00", "2020-02-25");

        Resposta resposta = get("/contacorrente/lancamentos?dataInicio=2020-02-10&dataFim=2020-02-20");

        assertEquals(200, resposta.status());
        assertEquals(2, resposta.json().size(), "os dois limites entram, o lançamento posterior não");
        assertEquals("2020-02-10", resposta.item(0).get("data").asString());
        assertEquals("2020-02-20", resposta.item(1).get("data").asString());
    }

    @Test
    @DisplayName("TC-043 filtro obrigatório ausente retorna 400")
    void tc043() {
        assertEquals(400, get("/contacorrente/lancamentos?dataFim=2020-02-20").status());
        assertEquals(400, get("/contacorrente/lancamentos?dataInicio=2020-02-10").status());
        assertEquals(400, get("/contacorrente/lancamentos").status());
        assertEquals("FILTRO_INVALIDO", get("/contacorrente/saldo").codigoDeErro());
        assertEquals("FILTRO_INVALIDO", get("/posicao").codigoDeErro());
    }

    @Test
    @DisplayName("TC-044 intervalo invertido retorna 400")
    void tc044() {
        Resposta resposta = get("/contacorrente/lancamentos?dataInicio=2020-02-20&dataFim=2020-02-10");

        assertEquals(400, resposta.status());
        assertEquals("FILTRO_INVALIDO", resposta.codigoDeErro());
    }

    @Test
    @DisplayName("TC-045 data não parseável retorna 400, nunca 500")
    void tc045() {
        Resposta filtro = get("/contacorrente/lancamentos?dataInicio=abc&dataFim=2020-02-10");
        Resposta saldo = get("/contacorrente/saldo?data=2020-13-45");

        assertEquals(400, filtro.status());
        assertEquals("FILTRO_INVALIDO", filtro.codigoDeErro());
        assertEquals(400, saldo.status());
        Resposta posicao = get("/posicao?data=2020-13-45");
        assertEquals(400, posicao.status());
        assertEquals("FILTRO_INVALIDO", posicao.codigoDeErro());
    }

    private static <T> List<T> emParalelo(int vezes, Callable<T> acao) throws Exception {
        try (ExecutorService pool = Executors.newFixedThreadPool(vezes)) {
            List<Future<T>> futuros = IntStream.range(0, vezes).mapToObj(i -> pool.submit(acao)).toList();
            List<T> resultados = new java.util.ArrayList<>();
            for (Future<T> futuro : futuros) {
                resultados.add(futuro.get());
            }
            return resultados;
        }
    }
}
