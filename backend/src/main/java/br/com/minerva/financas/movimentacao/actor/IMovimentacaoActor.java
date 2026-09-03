package br.com.minerva.financas.movimentacao.actor;

import java.time.LocalDate;

public interface IMovimentacaoActor {
    void executar(String codigo, LocalDate data, long quantidadeE2, long valorCentavos);
}
