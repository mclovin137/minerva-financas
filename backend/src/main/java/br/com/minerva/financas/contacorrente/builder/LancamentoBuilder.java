package br.com.minerva.financas.contacorrente.builder;

import br.com.minerva.financas.contacorrente.dominio.Lancamento;
import br.com.minerva.financas.contacorrente.dto.LancamentoRespostaDTO;

import java.math.BigDecimal;

/** Monta o DTO de resposta a partir do domínio. Nenhuma decisão de negócio aqui. */
public final class LancamentoBuilder {

    private LancamentoBuilder() {
    }

    public static LancamentoRespostaDTO resposta(Lancamento lancamento) {
        return new LancamentoRespostaDTO(lancamento.id(), lancamento.data(),
                BigDecimal.valueOf(lancamento.valor().centavos(), 2), lancamento.descricao());
    }
}
