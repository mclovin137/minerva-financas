package br.com.minerva.financas.movimentacao.actor;

import br.com.minerva.financas.movimentacao.builder.MovimentacaoBuilder;
import br.com.minerva.financas.movimentacao.dto.MovimentacaoRespostaDTO;
import br.com.minerva.financas.movimentacao.service.MovimentacaoService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class ListarMovimentacoesActor {

    private final MovimentacaoService service;

    public ListarMovimentacoesActor(MovimentacaoService service) {
        this.service = service;
    }

    public List<MovimentacaoRespostaDTO> executar(LocalDate inicio, LocalDate fim) {
        return service.listar(inicio, fim).stream().map(MovimentacaoBuilder::resposta).toList();
    }
}
