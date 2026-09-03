package br.com.minerva.financas.comum.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Dinheiro(long centavos) {
    public static Dinheiro de(BigDecimal valor) {
        if (valor == null || valor.scale() > 2 || valor.signum() < 0) throw new IllegalArgumentException("Valor monetário inválido.");
        try { return new Dinheiro(valor.setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact()); }
        catch (ArithmeticException e) { throw new IllegalArgumentException("Valor monetário inválido.", e); }
    }
    public static Dinheiro deCentavos(long centavos) { return new Dinheiro(centavos); }
    public Dinheiro negado() { return new Dinheiro(Math.negateExact(centavos)); }
}
