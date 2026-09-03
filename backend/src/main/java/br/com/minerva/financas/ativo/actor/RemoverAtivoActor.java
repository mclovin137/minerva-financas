package br.com.minerva.financas.ativo.actor;

import br.com.minerva.financas.ativo.service.AtivoService;
import org.springframework.stereotype.Component;

@Component
public class RemoverAtivoActor {

    private final AtivoService service;

    public RemoverAtivoActor(AtivoService service) {
        this.service = service;
    }

    public void executar(String codigo) {
        service.removerAtivo(codigo);
    }
}
