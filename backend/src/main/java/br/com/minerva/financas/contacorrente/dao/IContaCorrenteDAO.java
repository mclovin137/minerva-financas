package br.com.minerva.financas.contacorrente.dao;

import br.com.minerva.financas.contacorrente.dominio.Lancamento;

import java.time.LocalDate;
import java.util.List;

/** Porta de saída de persistência da conta corrente. */
public interface IContaCorrenteDAO {

    void inserirLancamento(long usuario, LocalDate data, long centavos, String descricao);

    long saldo(long usuario, LocalDate data);

    /**
     * Verifica, dentro da mesma transação, se aplicar {@code delta} em {@code data} preserva saldo
     * não negativo em <strong>toda</strong> data maior ou igual a {@code data} (FDD-001, D-A3).
     */
    boolean saldoFuturoValido(long usuario, LocalDate data, long delta);

    List<Lancamento> lancamentos(long usuario, LocalDate inicio, LocalDate fim);
}
