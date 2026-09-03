package br.com.minerva.financas.comum.rest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Escalas e arredondamento na fronteira HTTP: TC-049 a TC-056.
 * <p>
 * O enunciado é explícito: valores em reais não têm fração de centavo, preços unitários podem ter
 * mais casas, e o arredondamento é sempre para baixo.
 */
class EscalasIntegracaoTeste extends TesteIntegracao {

    private static final String DIA = "2020-02-28";

    @Test
    @DisplayName("TC-049 dinheiro com até duas casas é aceito")
    void tc049() {
        assertEquals(201, credito("12.42", DIA).status());
        assertEquals(201, credito("7", DIA).status());
        assertEquals(201, credito("0.01", DIA).status());
    }

    @Test
    @DisplayName("TC-050 dinheiro com mais de duas casas retorna 400")
    void tc050() {
        Resposta resposta = credito("12.421", DIA);

        assertEquals(400, resposta.status());
        assertEquals("CAMPO_INVALIDO", resposta.codigoDeErro());
    }

    @Test
    @DisplayName("TC-051 preço com até oito casas é aceito")
    void tc051() {
        String codigo = codigoDeAtivo("TC-051");
        criarAtivo(codigo);

        Resposta resposta = definirPreco(codigo, "2020-01-02", "1.23456789");

        assertEquals(201, resposta.status());
        assertNumero("1.23456789", resposta.json().get("precoMercado"), "oito casas preservadas");
    }

    @Test
    @DisplayName("TC-052 preço com mais de oito casas retorna 400")
    void tc052() {
        String codigo = codigoDeAtivo("TC-052");
        criarAtivo(codigo);

        assertEquals(400, definirPreco(codigo, "2020-01-02", "1.234567891").status());
    }

    @Test
    @DisplayName("TC-053 quantidade com até duas casas é aceita")
    void tc053() {
        String codigo = prepararAtivo("TC-053");

        assertEquals(201, comprar(codigo, DIA, "2.50", "25.00").status());
    }

    @Test
    @DisplayName("TC-054 quantidade com mais de duas casas retorna 400")
    void tc054() {
        String codigo = prepararAtivo("TC-054");

        Resposta resposta = comprar(codigo, DIA, "2.501", "25.00");

        assertEquals(400, resposta.status());
        assertEquals("CAMPO_INVALIDO", resposta.codigoDeErro());
    }

    @Test
    @DisplayName("TC-055 sinal inválido, valor não numérico e overflow retornam 400")
    void tc055() {
        assertEquals(400, credito("-1.00", DIA).status(), "valor negativo");
        assertEquals(400, post("/contacorrente/credito",
                "{\"valor\":\"muito\",\"descricao\":\"texto no lugar do número\",\"data\":\"" + DIA + "\"}")
                .status(), "valor não numérico");
        assertEquals(400, credito("99999999999999999999.00", DIA).status(), "fora da faixa representável");
        assertEquals(400, post("/contacorrente/credito", "{").status(), "JSON malformado");
    }

    @Test
    @DisplayName("TC-056 cálculos fracionários aplicam floor determinístico e reprodutível")
    void tc056() {
        String codigo = prepararAtivo("TC-056");
        comprar(codigo, DIA, "7.00", "10.00");

        String primeira = posicao(DIA).corpo();
        String segunda = posicao(DIA).corpo();

        assertEquals(primeira, segunda, "a mesma consulta devolve exatamente o mesmo resultado");
        assertNumero("1.42857142", posicao(DIA).item(0).get("precoMedio"),
                "10,00 ÷ 7,00 = 1,428571428... truncado para baixo, não 1,42857143");
    }

    private String prepararAtivo(String caso) {
        String codigo = codigoDeAtivo(caso);
        criarAtivo(codigo);
        definirPreco(codigo, "2020-01-02", "10.00");
        credito("1000.00", "2020-01-02");
        return codigo;
    }
}
