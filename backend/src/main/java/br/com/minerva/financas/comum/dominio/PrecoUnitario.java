package br.com.minerva.financas.comum.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record PrecoUnitario(long unidadesE8) {
    public static PrecoUnitario de(BigDecimal valor) {
        if (valor == null || valor.scale() > 8 || valor.signum() < 0) throw new IllegalArgumentException("Preço inválido.");
        try { return new PrecoUnitario(valor.movePointRight(8).longValueExact()); }
        catch (ArithmeticException e) { throw new IllegalArgumentException("Preço inválido.", e); }
    }
    public BigDecimal decimal() { return BigDecimal.valueOf(unidadesE8, 8).setScale(8, RoundingMode.UNNECESSARY); }
}
