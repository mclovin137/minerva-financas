package br.com.minerva.financas.contacorrente.actor;

import br.com.minerva.financas.contacorrente.service.ContaCorrenteService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/** Orquestra a consulta de saldo por data (única rota de GET fixada pelo enunciado). */
@Component
public class ConsultarSaldoActor {

    private final ContaCorrenteService service;

    public ConsultarSaldoActor(ContaCorrenteService service) {
        this.service = service;
    }

    public long executar(LocalDate data) {
        return service.saldo(data);
    }
}
