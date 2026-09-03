package br.com.minerva.financas.ativo.actor;

import br.com.minerva.financas.ativo.builder.AtivoBuilder;
import br.com.minerva.financas.ativo.dto.AtivoRequisicaoDTO;
import br.com.minerva.financas.ativo.dto.AtivoRespostaDTO;
import br.com.minerva.financas.ativo.service.AtivoService;
import org.springframework.stereotype.Component;

@Component
public class AtualizarAtivoActor {

    private final AtivoService service;

    public AtualizarAtivoActor(AtivoService service) {
        this.service = service;
    }

    public AtivoRespostaDTO executar(String codigo, AtivoRequisicaoDTO requisicao) {
        return AtivoBuilder.resposta(service.atualizarAtivo(codigo, AtivoBuilder.paraDominio(requisicao)));
    }
}
