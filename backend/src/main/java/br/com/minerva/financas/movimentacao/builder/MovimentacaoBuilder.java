package br.com.minerva.financas.movimentacao.builder;

import br.com.minerva.financas.movimentacao.dominio.Movimentacao;
import br.com.minerva.financas.movimentacao.dto.MovimentacaoRespostaDTO;

import java.math.BigDecimal;

public final class MovimentacaoBuilder {

    private MovimentacaoBuilder() {
    }

    public static MovimentacaoRespostaDTO resposta(Movimentacao movimento) {
        return new MovimentacaoRespostaDTO(movimento.id(), movimento.ativo(), movimento.data(),
                movimento.tipo().name(), movimento.quantidade().decimal(),
                BigDecimal.valueOf(movimento.valor().centavos(), 2));
    }
}
