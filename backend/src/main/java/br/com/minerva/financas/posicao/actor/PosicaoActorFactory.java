package br.com.minerva.financas.posicao.actor;

import org.springframework.stereotype.Component;

/** Ponto único de escolha dos actors da posição assíncrona. */
@Component
public class PosicaoActorFactory {
    private final SolicitarPosicaoAssincronaActor solicitacao;
    private final ConsultarPosicaoAssincronaActor consulta;

    public PosicaoActorFactory(SolicitarPosicaoAssincronaActor solicitacao,
                               ConsultarPosicaoAssincronaActor consulta) {
        this.solicitacao = solicitacao;
        this.consulta = consulta;
    }
    public SolicitarPosicaoAssincronaActor solicitacao() { return solicitacao; }
    public ConsultarPosicaoAssincronaActor consulta() { return consulta; }
}
