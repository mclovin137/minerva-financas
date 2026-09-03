package br.com.minerva.financas.movimentacao.actor;

import br.com.minerva.financas.movimentacao.service.MovimentacaoService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
class VendaActor implements IMovimentacaoActor {

    private final MovimentacaoService service;

    VendaActor(MovimentacaoService service) {
        this.service = service;
    }

    @Override
    public void executar(String ativo, LocalDate data, long quantidadeE2, long valorCentavos) {
        service.vender(ativo, data, quantidadeE2, valorCentavos);
    }
}
