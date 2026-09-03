package br.com.minerva.financas.movimentacao.dominio;

import br.com.minerva.financas.comum.dominio.Dinheiro;
import br.com.minerva.financas.comum.dominio.Quantidade;

import java.time.LocalDate;

public record Movimentacao(long id, String ativo, LocalDate data, TipoMovimentacao tipo,
                           Quantidade quantidade, Dinheiro valor) {
    public Movimentacao {
        if (ativo == null || data == null || tipo == null || quantidade == null || valor == null) {
            throw new IllegalArgumentException("Movimentação inválida.");
        }
    }
}
