package br.com.minerva.financas.contacorrente.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LancamentoRespostaDTO(long id, LocalDate data, BigDecimal valor, String descricao) {
}
