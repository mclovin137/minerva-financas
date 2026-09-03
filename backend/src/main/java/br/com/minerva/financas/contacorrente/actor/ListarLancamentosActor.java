package br.com.minerva.financas.contacorrente.actor;

import br.com.minerva.financas.contacorrente.builder.LancamentoBuilder;
import br.com.minerva.financas.contacorrente.dto.LancamentoRespostaDTO;
import br.com.minerva.financas.contacorrente.service.ContaCorrenteService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class ListarLancamentosActor {

    private final ContaCorrenteService service;

    public ListarLancamentosActor(ContaCorrenteService service) {
        this.service = service;
    }

    public List<LancamentoRespostaDTO> executar(LocalDate inicio, LocalDate fim) {
        return service.lancamentos(inicio, fim).stream().map(LancamentoBuilder::resposta).toList();
    }
}
