package br.com.minerva.financas.contacorrente.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LancamentoRequisicao(BigDecimal valor, String descricao, LocalDate data) {
}
