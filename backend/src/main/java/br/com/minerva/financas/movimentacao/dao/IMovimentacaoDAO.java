package br.com.minerva.financas.movimentacao.dao;

import br.com.minerva.financas.movimentacao.dominio.Movimentacao;
import br.com.minerva.financas.movimentacao.dominio.TipoMovimentacao;

import java.time.LocalDate;
import java.util.List;

public interface IMovimentacaoDAO {

    void inserir(long usuario, String codigo, LocalDate data, TipoMovimentacao tipo,
                 long quantidadeE2, long valorCentavos);

    boolean quantidadeFuturaValida(long usuario, String codigo, LocalDate data, long deltaE2);

    List<Movimentacao> listar(long usuario, LocalDate inicio, LocalDate fim);
}
