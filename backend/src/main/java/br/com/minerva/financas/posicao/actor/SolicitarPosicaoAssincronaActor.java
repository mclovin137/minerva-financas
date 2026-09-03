package br.com.minerva.financas.posicao.actor;

import br.com.minerva.financas.posicao.service.IPosicaoAssincronaService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class SolicitarPosicaoAssincronaActor {
    private final IPosicaoAssincronaService service;
    public SolicitarPosicaoAssincronaActor(IPosicaoAssincronaService service) { this.service = service; }
    public long executar(LocalDate data) { return service.solicitar(data); }
}
