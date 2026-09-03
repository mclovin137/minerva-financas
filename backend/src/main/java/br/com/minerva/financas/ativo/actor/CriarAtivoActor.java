package br.com.minerva.financas.ativo.actor;

import br.com.minerva.financas.ativo.builder.AtivoBuilder;
import br.com.minerva.financas.ativo.dto.AtivoRequisicaoDTO;
import br.com.minerva.financas.ativo.service.AtivoService;
import org.springframework.stereotype.Component;

@Component
public class CriarAtivoActor {

    private final AtivoService service;

    public CriarAtivoActor(AtivoService service) {
        this.service = service;
    }

    public void executar(AtivoRequisicaoDTO requisicao) {
        service.criarAtivo(AtivoBuilder.paraDominio(requisicao));
    }
}
