package br.com.minerva.financas.movimentacao.actor;

import org.springframework.stereotype.Component;

@Component
public class MovimentacaoActorFactory {

    private final CompraActor compra;
    private final VendaActor venda;

    MovimentacaoActorFactory(CompraActor compra, VendaActor venda) {
        this.compra = compra;
        this.venda = venda;
    }

    public IMovimentacaoActor compra() {
        return compra;
    }

    public IMovimentacaoActor venda() {
        return venda;
    }
}
