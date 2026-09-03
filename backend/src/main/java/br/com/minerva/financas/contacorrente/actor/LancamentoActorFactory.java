package br.com.minerva.financas.contacorrente.actor;

import br.com.minerva.financas.contacorrente.dominio.TipoLancamentoEnum;
import org.springframework.stereotype.Component;

/**
 * Elimina a duplicação entre crédito e débito atrás do controller (ADR-003, A.4). As rotas
 * {@code /contacorrente/credito} e {@code /contacorrente/debito} permanecem fixas e distintas — a
 * fábrica só escolhe qual actor concreto atende cada uma.
 */
@Component
public class LancamentoActorFactory {

    private final CreditoActor creditoActor;
    private final DebitoActor debitoActor;

    LancamentoActorFactory(CreditoActor creditoActor, DebitoActor debitoActor) {
        this.creditoActor = creditoActor;
        this.debitoActor = debitoActor;
    }

    public ILancamentoActor criar(TipoLancamentoEnum tipo) {
        return tipo == TipoLancamentoEnum.CREDITO ? creditoActor : debitoActor;
    }
}
