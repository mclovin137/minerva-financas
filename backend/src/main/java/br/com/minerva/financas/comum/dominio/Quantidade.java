package br.com.minerva.financas.comum.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Quantidade(long unidadesE2) {
    public static Quantidade de(BigDecimal valor) {
        if (valor == null || valor.scale() > 2 || valor.signum() <= 0) throw new IllegalArgumentException("Quantidade inválida.");
        try { return new Quantidade(valor.movePointRight(2).longValueExact()); }
        catch (ArithmeticException e) { throw new IllegalArgumentException("Quantidade inválida.", e); }
    }
    public BigDecimal decimal() { return BigDecimal.valueOf(unidadesE2, 2).setScale(2, RoundingMode.UNNECESSARY); }
}
