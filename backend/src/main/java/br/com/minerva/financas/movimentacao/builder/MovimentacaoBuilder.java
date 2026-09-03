package br.com.minerva.financas.movimentacao.builder;

import br.com.minerva.financas.movimentacao.dominio.Movimentacao;
import br.com.minerva.financas.movimentacao.dto.MovimentacaoResposta;

import java.math.BigDecimal;

public final class MovimentacaoBuilder {

    private MovimentacaoBuilder() {
    }

    public static MovimentacaoResposta resposta(Movimentacao movimento) {
        return new MovimentacaoResposta(movimento.id(), movimento.ativo(), movimento.data(),
                movimento.tipo().name(), movimento.quantidade().decimal(),
                BigDecimal.valueOf(movimento.valor().centavos(), 2));
    }
}
