package br.com.minerva.financas.posicao.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record PosicaoRespostaDTO(String ativo, String nome, String tipo, BigDecimal quantidade,
                              BigDecimal precoMercado, BigDecimal valorMercadoTotal,
                              BigDecimal precoMedio, BigDecimal rendimento, BigDecimal lucro) {
}
