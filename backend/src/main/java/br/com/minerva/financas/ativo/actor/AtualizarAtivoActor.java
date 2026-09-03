package br.com.minerva.financas.ativo.actor;

import br.com.minerva.financas.ativo.builder.AtivoBuilder;
import br.com.minerva.financas.ativo.dto.AtivoRequisicao;
import br.com.minerva.financas.ativo.dto.AtivoResposta;
import br.com.minerva.financas.ativo.service.AtivoService;
import org.springframework.stereotype.Component;

@Component
public class AtualizarAtivoActor {

    private final AtivoService service;

    public AtualizarAtivoActor(AtivoService service) {
        this.service = service;
    }

    public AtivoResposta executar(String codigo, AtivoRequisicao requisicao) {
        return AtivoBuilder.resposta(service.atualizarAtivo(codigo, AtivoBuilder.paraDominio(requisicao)));
    }
}
