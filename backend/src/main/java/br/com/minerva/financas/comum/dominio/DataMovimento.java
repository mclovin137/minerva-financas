package br.com.minerva.financas.comum.dominio;

import java.time.LocalDate;

public record DataMovimento(LocalDate valor) {
    public DataMovimento { if (valor == null) throw new IllegalArgumentException("Data inválida."); }
}
