package br.com.minerva.financas.ativo.dao;

import br.com.minerva.financas.ativo.dominio.Ativo;
import br.com.minerva.financas.comum.dominio.PrecoUnitario;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Porta de saída de persistência do ativo e de seu preço de mercado por data. */
public interface IAtivoDAO {

    void inserirAtivo(Ativo ativo);

    void atualizarAtivo(String codigo, Ativo ativo);

    void removerAtivo(String codigo);

    List<Ativo> ativos();

    Optional<Ativo> ativo(String codigo);

    boolean ativoTemMovimentacao(String codigo);

    /** @return {@code true} quando o preço foi criado, {@code false} quando substituiu um preço existente */
    boolean definirPreco(String codigo, LocalDate data, long precoE8);

    /** @return {@code true} quando havia preço naquela data e ele foi removido */
    boolean removerPreco(String codigo, LocalDate data);

    /** Preço mais recente com data menor ou igual a {@code data}; vazio quando não há preço elegível (D-B3). */
    Optional<PrecoUnitario> precoVigente(String codigo, LocalDate data);
}
