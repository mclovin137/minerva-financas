package br.com.minerva.financas.ativo.actor;

import br.com.minerva.financas.ativo.builder.AtivoBuilder;
import br.com.minerva.financas.ativo.dto.AtivoRespostaDTO;
import br.com.minerva.financas.ativo.service.AtivoService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ListarAtivosActor {

    private final AtivoService service;

    public ListarAtivosActor(AtivoService service) {
        this.service = service;
    }

    public List<AtivoRespostaDTO> executar() {
        return service.ativos().stream().map(AtivoBuilder::resposta).toList();
    }
}
