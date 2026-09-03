package br.com.minerva.financas.ativo.builder;

import br.com.minerva.financas.ativo.dominio.Ativo;
import br.com.minerva.financas.ativo.dominio.TipoAtivo;
import br.com.minerva.financas.ativo.dto.AtivoRequisicao;
import br.com.minerva.financas.ativo.dto.AtivoResposta;
import br.com.minerva.financas.ativo.dto.PrecoResposta;
import br.com.minerva.financas.comum.dominio.ErroAplicacao;
import br.com.minerva.financas.comum.helper.Requisicoes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.stream.Collectors;

/** Monta o domínio a partir da requisição e os DTOs de resposta a partir do domínio. */
public final class AtivoBuilder {

    private AtivoBuilder() {
    }

    public static Ativo paraDominio(AtivoRequisicao r) {
        if (r == null) {
            throw new ErroAplicacao("REQUISICAO_INVALIDA", 400, "O corpo da requisição é obrigatório.");
        }
        if (r.precoMercado() != null) {
            throw ErroAplicacao.campoInvalido("precoMercado",
                    "O preço de mercado deixou de ser atributo do ativo. Use PUT /ativos/{ativo}/precos/{data}.");
        }
        try {
            return new Ativo(
                    Requisicoes.texto(r.ativo(), "ativo"),
                    Requisicoes.texto(r.nome(), "nome"),
                    tipo(r.tipo()),
                    Requisicoes.dataObrigatoria(r.dataEmissao(), "dataEmissao"),
                    Requisicoes.dataObrigatoria(r.dataVencimento(), "dataVencimento"));
        } catch (IllegalArgumentException e) {
            throw ErroAplicacao.campoInvalido("ativo", e.getMessage());
        }
    }

    public static AtivoResposta resposta(Ativo ativo) {
        return new AtivoResposta(ativo.codigo(), ativo.nome(), ativo.tipo().name(),
                ativo.dataEmissao(), ativo.dataVencimento());
    }

    public static PrecoResposta respostaDePreco(String codigo, LocalDate data, long precoE8) {
        return new PrecoResposta(codigo, data, BigDecimal.valueOf(precoE8, 8));
    }

    private static TipoAtivo tipo(String bruto) {
        Requisicoes.texto(bruto, "tipo");
        try {
            return TipoAtivo.valueOf(bruto);
        } catch (IllegalArgumentException e) {
            String validos = Arrays.stream(TipoAtivo.values()).map(Enum::name).collect(Collectors.joining(", "));
            throw ErroAplicacao.campoInvalido("tipo", "O tipo deve ser um de: " + validos + ".");
        }
    }
}
