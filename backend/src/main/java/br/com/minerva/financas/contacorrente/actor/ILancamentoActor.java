package br.com.minerva.financas.contacorrente.actor;

import java.time.LocalDate;

/** Contrato comum de crédito e débito, escolhido em runtime pela {@link LancamentoActorFactory}. */
public interface ILancamentoActor {

    void executar(long centavos, String descricao, LocalDate data);
}
