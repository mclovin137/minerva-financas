package br.com.minerva.financas.ativo.dominio;

import br.com.minerva.financas.comum.dominio.CalendarioNegociacao;

import java.time.LocalDate;

/**
 * Ativo financeiro com sua janela de negociação.
 * <p>
 * O ativo <strong>não carrega preço de mercado</strong> (FDD-001, D-A1): o preço vive sempre em
 * {@code valor_mercado (ativo_id, data, preco)} e é selecionado pela data de referência da consulta.
 *
 * @param dataEmissao    primeiro dia em que o ativo pode ser movimentado, inclusive
 * @param dataVencimento dia a partir do qual o ativo não pode mais ser movimentado, exclusive
 */
public record Ativo(String codigo, String nome, TipoAtivo tipo, LocalDate dataEmissao, LocalDate dataVencimento) {

    public Ativo {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("O código do ativo é obrigatório.");
        }
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome do ativo é obrigatório.");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("O tipo do ativo é obrigatório.");
        }
        if (dataEmissao == null || dataVencimento == null) {
            throw new IllegalArgumentException("A data de emissão e a data de vencimento são obrigatórias.");
        }
        if (!dataEmissao.isBefore(dataVencimento)) {
            throw new IllegalArgumentException("A data de emissão deve ser anterior à data de vencimento.");
        }
    }

    /** A janela de negociação é fechada na emissão e aberta no vencimento: {@code [emissao, vencimento)}. */
    public boolean dentroDaJanela(LocalDate data) {
        return !data.isBefore(dataEmissao) && data.isBefore(dataVencimento);
    }

    public boolean negociavelEm(LocalDate data) {
        return dentroDaJanela(data) && CalendarioNegociacao.ehDiaUtil(data);
    }
}
