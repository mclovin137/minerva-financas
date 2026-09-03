package br.com.minerva.financas.posicao.dominio;

import br.com.minerva.financas.comum.dominio.Dinheiro;
import br.com.minerva.financas.comum.dominio.PrecoUnitario;
import br.com.minerva.financas.comum.dominio.Quantidade;
import br.com.minerva.financas.movimentacao.dominio.Movimentacao;
import br.com.minerva.financas.movimentacao.dominio.TipoMovimentacao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Fórmulas da posição, isoladas de HTTP e persistência. */
public final class CalculoPosicao {

    private CalculoPosicao() {
    }

    public record Resultado(Quantidade quantidade, PrecoUnitario precoMedio, PrecoUnitario rendimento,
                            Dinheiro valorMercadoTotal, Dinheiro lucro) {
    }

    public static Resultado calcular(List<Movimentacao> movimentos, PrecoUnitario precoMercado) {
        return calcular(new Acumulador(movimentos), precoMercado);
    }

    public static Resultado calcular(Acumulador acumulado, PrecoUnitario precoMercado) {
        if (acumulado.quantidadeE2 < 0) {
            throw new IllegalArgumentException("Quantidade negativa: histórico de movimentações inconsistente.");
        }
        PrecoUnitario precoMedio = precoMedio(acumulado);
        PrecoUnitario rendimento = rendimento(precoMercado, precoMedio);
        Dinheiro valorMercado = valorMercado(acumulado.quantidadeE2, precoMercado);
        long lucro = Math.subtractExact(acumulado.vendasCentavos, acumulado.comprasCentavos);
        return new Resultado(new Quantidade(acumulado.quantidadeE2), precoMedio, rendimento,
                valorMercado, Dinheiro.deCentavos(lucro));
    }

    private static PrecoUnitario precoMedio(Acumulador acumulado) {
        if (acumulado.comprasQuantidadeE2 <= 0) {
            return null;
        }
        BigDecimal medio = BigDecimal.valueOf(acumulado.comprasCentavos)
                .divide(BigDecimal.valueOf(acumulado.comprasQuantidadeE2), 8, RoundingMode.FLOOR);
        return PrecoUnitario.de(medio);
    }

    private static PrecoUnitario rendimento(PrecoUnitario precoMercado, PrecoUnitario precoMedio) {
        if (precoMercado == null || precoMedio == null || precoMedio.unidadesE8() == 0) {
            return null;
        }
        BigDecimal valor = precoMercado.decimal().divide(precoMedio.decimal(), 8, RoundingMode.FLOOR);
        return PrecoUnitario.de(valor);
    }

    private static Dinheiro valorMercado(long quantidadeE2, PrecoUnitario precoMercado) {
        if (precoMercado == null) {
            return null;
        }
        BigDecimal valor = BigDecimal.valueOf(quantidadeE2).movePointLeft(2)
                .multiply(precoMercado.decimal()).setScale(2, RoundingMode.FLOOR);
        return Dinheiro.deCentavos(valor.movePointRight(2).longValueExact());
    }

    /** Acumula quatro inteiros por ativo, sem materializar o histórico. */
    public static final class Acumulador {
        private long quantidadeE2;
        private long comprasQuantidadeE2;
        private long comprasCentavos;
        private long vendasCentavos;

        public Acumulador() {
        }

        private Acumulador(List<Movimentacao> movimentos) {
            for (Movimentacao movimento : movimentos) {
                acumular(movimento.tipo(), movimento.quantidade().unidadesE2(), movimento.valor().centavos());
            }
        }

        public void acumular(TipoMovimentacao tipo, long quantidadeE2, long valorCentavos) {
            if (tipo == TipoMovimentacao.COMPRA) {
                this.quantidadeE2 = Math.addExact(this.quantidadeE2, quantidadeE2);
                this.comprasQuantidadeE2 = Math.addExact(this.comprasQuantidadeE2, quantidadeE2);
                this.comprasCentavos = Math.addExact(this.comprasCentavos, valorCentavos);
            } else {
                this.quantidadeE2 = Math.subtractExact(this.quantidadeE2, quantidadeE2);
                this.vendasCentavos = Math.addExact(this.vendasCentavos, valorCentavos);
            }
        }
    }
}
