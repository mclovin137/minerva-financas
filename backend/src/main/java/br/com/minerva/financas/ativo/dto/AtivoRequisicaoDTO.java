package br.com.minerva.financas.ativo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * {@code precoMercado} não faz mais parte do contrato do ativo a partir do nível 2 (D-B2). Ele
 * continua declarado aqui de propósito: tolerar e ignorar o campo esconderia um bug do cliente,
 * então quem o enviar recebe 400 em vez de um sucesso enganoso.
 */
public record AtivoRequisicaoDTO(String ativo, String nome, String tipo, LocalDate dataEmissao,
                              LocalDate dataVencimento, BigDecimal precoMercado) {
}
