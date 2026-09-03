package br.com.minerva.financas.posicao.dao;

import br.com.minerva.financas.comum.dominio.PrecoUnitario;
import br.com.minerva.financas.movimentacao.dominio.Movimentacao;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IPosicaoDAO {

    List<Movimentacao> movimentacoesAte(long usuario, String codigo, LocalDate data);

    Optional<PrecoUnitario> precoVigente(String codigo, LocalDate data);

}
