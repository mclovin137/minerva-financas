package br.com.minerva.financas.movimentacao.dao;

import br.com.minerva.financas.comum.dominio.PrecoUnitario;
import br.com.minerva.financas.movimentacao.dominio.TipoMovimentacao;

import java.time.LocalDate;
import java.util.Map;

/** Contrato de leitura incremental das movimentações usado pela posição assíncrona. */
public interface ILeitorDeMovimentacoes {

    void percorrer(long usuario, LocalDate data, int totalDeParticoes, int particao,
                   ConsumidorDeMovimento consumidor);

    Map<String, PrecoUnitario> precosVigentes(LocalDate data);

    @FunctionalInterface
    interface ConsumidorDeMovimento {
        void aceitar(String codigo, TipoMovimentacao tipo, long quantidadeE2, long valorCentavos);
    }
}
