package br.com.minerva.financas.movimentacao.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MovimentacaoRespostaDTO(long id, String ativo, LocalDate data, String tipo,
                                   BigDecimal quantidade, BigDecimal valor) {
}
