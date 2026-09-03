package br.com.minerva.financas.contacorrente.actor;

import br.com.minerva.financas.contacorrente.service.ContaCorrenteService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
class DebitoActor implements ILancamentoActor {

    private final ContaCorrenteService service;

    DebitoActor(ContaCorrenteService service) {
        this.service = service;
    }

    @Override
    public void executar(long centavos, String descricao, LocalDate data) {
        service.debito(centavos, descricao, data);
    }
}
