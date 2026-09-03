package br.com.minerva.financas.ativo.actor;

import br.com.minerva.financas.ativo.builder.AtivoBuilder;
import br.com.minerva.financas.ativo.dto.AtivoResposta;
import br.com.minerva.financas.ativo.service.AtivoService;
import org.springframework.stereotype.Component;

@Component
public class ConsultarAtivoActor {

    private final AtivoService service;

    public ConsultarAtivoActor(AtivoService service) {
        this.service = service;
    }

    public AtivoResposta executar(String codigo) {
        return AtivoBuilder.resposta(service.ativo(codigo));
    }
}
