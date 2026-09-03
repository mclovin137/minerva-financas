package br.com.minerva.financas.posicao.dominio;

import br.com.minerva.financas.comum.dominio.Dinheiro;
import br.com.minerva.financas.comum.dominio.PrecoUnitario;
import br.com.minerva.financas.comum.dominio.Quantidade;
import br.com.minerva.financas.movimentacao.dominio.Movimentacao;
import br.com.minerva.financas.movimentacao.dominio.TipoMovimentacaoEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** As fórmulas D1 isoladas de HTTP e de banco: onde um erro de arredondamento nasceria. */
class CalculoPosicaoTeste {

    private static final LocalDate DIA = LocalDate.parse("2020-02-28");
    private static final PrecoUnitario DEZ = preco("10.00");

    private static Movimentacao compra(String quantidade, String valor) {
        return movimento(TipoMovimentacaoEnum.COMPRA, quantidade, valor);
    }

    private static Movimentacao venda(String quantidade, String valor) {
        return movimento(TipoMovimentacaoEnum.VENDA, quantidade, valor);
    }

    private static Movimentacao movimento(TipoMovimentacaoEnum tipo, String quantidade, String valor) {
        return new Movimentacao(1L, "ATIVO1", DIA, tipo,
                Quantidade.de(new BigDecimal(quantidade)), Dinheiro.de(new BigDecimal(valor)));
    }

    private static PrecoUnitario preco(String valor) {
        return PrecoUnitario.de(new BigDecimal(valor));
    }

    @Test
    @DisplayName("quantidade total é compras menos vendas")
    void quantidadeTotal() {
        var resultado = CalculoPosicao.calcular(
                List.of(compra("5.50", "55.00"), compra("2.50", "25.00"), venda("3.00", "30.00")), DEZ);

        assertEquals(500, resultado.quantidade().unidadesE2());
    }

    @Test
    @DisplayName("preço médio pondera pela quantidade, não pela média aritmética dos preços")
    void precoMedioEhPonderado() {
        var resultado = CalculoPosicao.calcular(List.of(compra("1.00", "10.00"), compra("3.00", "60.00")),
                preco("25.00"));

        assertEquals(new BigDecimal("17.50000000"), resultado.precoMedio().decimal(),
                "(10,00 + 60,00) ÷ 4,00 = 17,50; a média dos preços 10 e 20 daria 15,00");
    }

    @Test
    @DisplayName("o floor entra somente no quociente, já na escala de oito casas")
    void floorApenasNoQuociente() {
        var resultado = CalculoPosicao.calcular(List.of(compra("3.00", "10.00")), DEZ);

        assertEquals(new BigDecimal("3.33333333"), resultado.precoMedio().decimal(),
                "10,00 ÷ 3,00 truncado para baixo; arredondar para cima daria 3,33333334");
    }

    @Test
    @DisplayName("as somas intermediárias são exatas antes de qualquer divisão")
    void somasIntermediariasSaoExatas() {
        var resultado = CalculoPosicao.calcular(
                List.of(compra("0.33", "0.10"), compra("0.33", "0.10"), compra("0.34", "0.10")), preco("1.00"));

        assertEquals(100, resultado.quantidade().unidadesE2(), "0,33 + 0,33 + 0,34 = 1,00 exato");
        assertEquals(new BigDecimal("0.30000000"), resultado.precoMedio().decimal(),
                "dividir cada compra antes de somar perderia frações a cada parcela");
    }

    @Test
    @DisplayName("rendimento é preço de mercado dividido pelo preço médio")
    void rendimento() {
        var resultado = CalculoPosicao.calcular(List.of(compra("2.00", "100.00")), preco("60.00"));

        assertEquals(new BigDecimal("1.20000000"), resultado.rendimento().decimal());
    }

    @Test
    @DisplayName("lucro é a soma das vendas menos a das compras e pode ser negativo")
    void lucro() {
        var resultado = CalculoPosicao.calcular(
                List.of(compra("2.00", "100.00"), venda("1.00", "70.00")), preco("60.00"));

        assertEquals(-3000, resultado.lucro().centavos());
    }

    @Test
    @DisplayName("valor de mercado total é quantidade × preço, truncado no centavo")
    void valorDeMercadoTotal() {
        var resultado = CalculoPosicao.calcular(List.of(compra("3.00", "30.00")), preco("1.005"));

        assertEquals(301, resultado.valorMercadoTotal().centavos(),
                "3,00 × 1,005 = 3,015, que trunca para 3,01 e nunca sobe para 3,02");
    }

    @Test
    @DisplayName("sem preço de mercado elegível, só os campos que dependem dele ficam nulos")
    void semPrecoDeMercado() {
        var resultado = CalculoPosicao.calcular(List.of(compra("2.00", "100.00")), null);

        assertNull(resultado.valorMercadoTotal());
        assertNull(resultado.rendimento());
        assertEquals(200, resultado.quantidade().unidadesE2());
        assertEquals(new BigDecimal("50.00000000"), resultado.precoMedio().decimal());
        assertEquals(-10000, resultado.lucro().centavos());
    }

    @Test
    @DisplayName("um histórico que zeraria a posição não divide por zero")
    void posicaoZeradaPreservaPrecoMedio() {
        var resultado = CalculoPosicao.calcular(
                List.of(compra("1.00", "10.00"), venda("1.00", "12.00")), DEZ);

        assertEquals(0, resultado.quantidade().unidadesE2());
        assertEquals(new BigDecimal("10.00000000"), resultado.precoMedio().decimal(),
                "o preço médio das compras não é apagado por uma venda total");
        assertEquals(200, resultado.lucro().centavos());
    }

    @Test
    @DisplayName("histórico que deixaria a quantidade negativa é rejeitado pelo domínio")
    void quantidadeNegativaEhRejeitada() {
        List<Movimentacao> inconsistente = List.of(compra("1.00", "10.00"), venda("2.00", "20.00"));

        assertThrows(IllegalArgumentException.class, () -> CalculoPosicao.calcular(inconsistente, DEZ));
    }

    @Test
    @DisplayName("o acumulador tem estado constante, independentemente do volume percorrido")
    void acumuladorNaoCresceComOVolume() {
        var acumulador = new CalculoPosicao.Acumulador();
        for (int i = 0; i < 200_000; i++) {
            acumulador.acumular(TipoMovimentacaoEnum.COMPRA, 100, 1000);
        }

        var resultado = CalculoPosicao.calcular(acumulador, DEZ);

        assertEquals(20_000_000L, resultado.quantidade().unidadesE2());
        assertEquals(new BigDecimal("10.00000000"), resultado.precoMedio().decimal());
    }
}
