package br.com.minerva.financas.movimentacao.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MovimentacaoRequisicaoDTO(String ativo, LocalDate data, BigDecimal quantidade, BigDecimal valor) {
}
