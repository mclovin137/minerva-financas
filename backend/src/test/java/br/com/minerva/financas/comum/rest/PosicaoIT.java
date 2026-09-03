package br.com.minerva.financas.comum.rest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Consulta de posição e as fórmulas D1: TC-025 a TC-032. */
class PosicaoIT extends TesteIntegracao {

    private static final String DIA = "2020-02-28";

    private String ativoComSaldo(String caso, String preco) {
        String codigo = codigoDeAtivo(caso);
        criarAtivo(codigo);
        definirPreco(codigo, "2020-01-02", preco);
        credito("100000.00", "2020-01-02");
        return codigo;
    }

    private JsonNode posicaoDe(String codigo) {
        Resposta resposta = posicao(DIA);
        for (JsonNode linha : resposta.json()) {
            if (codigo.equals(linha.get("ativo").asString())) {
                return linha;
            }
        }
        throw new AssertionError("o ativo " + codigo + " não apareceu na posição: " + resposta.corpo());
    }

    @Test
    @DisplayName("filtro de data da posição ausente ou inválido retorna 400")
    void filtroDeDataObrigatorio() {
        Resposta ausente = get("/posicao");
        Resposta invalido = get("/posicao?data=2020-13-45");

        assertEquals(400, ausente.status());
        assertEquals("FILTRO_INVALIDO", ausente.codigoDeErro());
        assertEquals(400, invalido.status());
        assertEquals("FILTRO_INVALIDO", invalido.codigoDeErro());
    }

    @Test
    @DisplayName("TC-025 posição válida retorna 200 com um registro por ativo movimentado")
    void tc025() {
        String codigo = ativoComSaldo("TC-025", "10.00");
        comprar(codigo, DIA, "2.00", "20.00");

        JsonNode linha = posicaoDe(codigo);

        assertEquals("Ativo " + codigo, linha.get("nome").asString());
        assertEquals("RV", linha.get("tipo").asString());
    }

    @Test
    @DisplayName("TC-026 quantidade total é a soma das compras menos as vendas")
    void tc026() {
        String codigo = ativoComSaldo("TC-026", "10.00");
        comprar(codigo, DIA, "5.50", "55.00");
        comprar(codigo, DIA, "2.50", "25.00");
        vender(codigo, DIA, "3.00", "30.00");

        assertNumero("5.00", posicaoDe(codigo).get("quantidade"), "5,50 + 2,50 − 3,00");
    }

    @Test
    @DisplayName("TC-027 valor de mercado total é quantidade × preço vigente")
    void tc027() {
        String codigo = ativoComSaldo("TC-027", "7.25");
        comprar(codigo, DIA, "4.00", "20.00");

        assertNumero("29.00", posicaoDe(codigo).get("valorMercadoTotal"), "4,00 × 7,25");
    }

    @Test
    @DisplayName("TC-028 preço médio é a média ponderada pela quantidade, não a média dos preços")
    void tc028() {
        String codigo = ativoComSaldo("TC-028", "25.00");
        comprar(codigo, DIA, "1.00", "10.00");
        comprar(codigo, DIA, "3.00", "60.00");

        JsonNode linha = posicaoDe(codigo);

        assertNumero("17.50", linha.get("precoMedio"),
                "(10,00 + 60,00) ÷ (1,00 + 3,00); a média aritmética dos preços daria 15,00");
    }

    @Test
    @DisplayName("TC-029 as somas intermediárias são exatas e só então convertidas")
    void tc029() {
        String codigo = ativoComSaldo("TC-029", "1.00");
        comprar(codigo, DIA, "0.33", "0.10");
        comprar(codigo, DIA, "0.33", "0.10");
        comprar(codigo, DIA, "0.34", "0.10");

        JsonNode linha = posicaoDe(codigo);

        assertNumero("1.00", linha.get("quantidade"), "0,33 + 0,33 + 0,34 fecha exatamente em 1,00");
        assertNumero("0.30", linha.get("precoMedio"),
                "0,30 ÷ 1,00; converter cada compra antes de somar perderia centavos");
    }

    @Test
    @DisplayName("TC-030 o floor é aplicado somente no quociente, na escala do preço")
    void tc030() {
        String codigo = ativoComSaldo("TC-030", "10.00");
        comprar(codigo, DIA, "3.00", "10.00");

        assertNumero("3.33333333", posicaoDe(codigo).get("precoMedio"),
                "10,00 ÷ 3,00 truncado para baixo em oito casas, nunca arredondado para cima");
    }

    @Test
    @DisplayName("TC-031 rendimento é preço de mercado ÷ preço médio e lucro é vendas − compras")
    void tc031() {
        String codigo = ativoComSaldo("TC-031", "60.00");
        comprar(codigo, DIA, "2.00", "100.00");
        vender(codigo, DIA, "1.00", "70.00");

        JsonNode linha = posicaoDe(codigo);

        assertNumero("1.20", linha.get("rendimento"), "60,00 ÷ 50,00");
        assertNumero("-30.00", linha.get("lucro"), "70,00 − 100,00; o lucro pode ser negativo");
    }

    @Test
    @DisplayName("TC-032 ativo sem movimentação não entra na posição e não divide por zero")
    void tc032() {
        String semMovimento = ativoComSaldo("TC-032a", "10.00");
        String comMovimento = ativoComSaldo("TC-032b", "10.00");
        comprar(comMovimento, DIA, "1.00", "10.00");

        Resposta resposta = posicao(DIA);

        assertEquals(200, resposta.status());
        assertTrue(resposta.corpo().contains(comMovimento));
        assertTrue(!resposta.corpo().contains(semMovimento),
                "a posição lista apenas ativos movimentados pelo proprietário até a data");
    }

    @Test
    @DisplayName("D-B3 sem preço elegível na data, os campos de mercado vêm nulos e a consulta segue 200")
    void semPrecoElegivel() {
        String codigo = codigoDeAtivo("DB3");
        criarAtivo(codigo);
        definirPreco(codigo, "2020-03-10", "50.00");
        credito("1000.00", "2020-01-02");
        comprar(codigo, DIA, "2.00", "100.00");

        JsonNode linha = posicaoDe(codigo);

        assertNulo(linha.get("precoMercado"), "não existe preço com data menor ou igual a " + DIA);
        assertNulo(linha.get("valorMercadoTotal"), "sem preço não há valor de mercado");
        assertNulo(linha.get("rendimento"), "sem preço não há rendimento");
        assertNumero("2.00", linha.get("quantidade"), "a quantidade não depende do preço de mercado");
        assertNumero("50.00", linha.get("precoMedio"), "o preço médio vem das compras, não do mercado");
        assertNumero("-100.00", linha.get("lucro"), "o lucro não depende do preço de mercado");
    }
}
