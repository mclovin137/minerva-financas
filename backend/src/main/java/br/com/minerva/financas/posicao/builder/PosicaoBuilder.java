package br.com.minerva.financas.posicao.builder;

import br.com.minerva.financas.posicao.dominio.Posicao;
import br.com.minerva.financas.posicao.dto.PosicaoRespostaDTO;

import java.math.BigDecimal;

public final class PosicaoBuilder {

    private PosicaoBuilder() {
    }

    public static PosicaoRespostaDTO resposta(Posicao posicao) {
        var resultado = posicao.resultado();
        return new PosicaoRespostaDTO(posicao.ativo().codigo(), posicao.ativo().nome(),
                posicao.ativo().tipo().name(), resultado.quantidade().decimal(),
                posicao.precoMercado() == null ? null : posicao.precoMercado().decimal(),
                resultado.valorMercadoTotal() == null ? null
                        : BigDecimal.valueOf(resultado.valorMercadoTotal().centavos(), 2),
                resultado.precoMedio() == null ? null : resultado.precoMedio().decimal(),
                resultado.rendimento() == null ? null : resultado.rendimento().decimal(),
                BigDecimal.valueOf(resultado.lucro().centavos(), 2));
    }
}
