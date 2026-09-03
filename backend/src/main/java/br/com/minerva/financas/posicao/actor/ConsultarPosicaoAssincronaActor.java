package br.com.minerva.financas.posicao.actor;

import br.com.minerva.financas.posicao.builder.PosicaoBuilder;
import br.com.minerva.financas.posicao.dto.PosicaoResposta;
import br.com.minerva.financas.posicao.service.IPosicaoAssincronaService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ConsultarPosicaoAssincronaActor {
    private final IPosicaoAssincronaService service;
    public ConsultarPosicaoAssincronaActor(IPosicaoAssincronaService service) { this.service = service; }
    public IPosicaoAssincronaService.Estado executar(long id) {
        return service.consultar(id).orElseThrow(() -> new br.com.minerva.financas.comum.dominio.ErroAplicacao(
                "RECURSO_NAO_ENCONTRADO", 404,
                "Execução inexistente, já entregue ou expirada."));
    }
    public List<PosicaoResposta> respostas(List<br.com.minerva.financas.posicao.dominio.Posicao> posicoes) {
        return posicoes.stream().map(PosicaoBuilder::resposta).toList();
    }
}
