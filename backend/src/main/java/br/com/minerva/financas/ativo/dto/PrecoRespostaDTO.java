package br.com.minerva.financas.ativo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PrecoRespostaDTO(String ativo, LocalDate data, BigDecimal precoMercado) {
}
