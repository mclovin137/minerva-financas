package br.com.minerva.financas.contacorrente.dominio;

import br.com.minerva.financas.comum.dominio.Dinheiro;
import java.time.LocalDate;

public record Lancamento(long id, LocalDate data, Dinheiro valor, String descricao) {
    public Lancamento { if (data == null || valor == null || descricao == null || descricao.isBlank()) throw new IllegalArgumentException("Lançamento inválido."); }
}
