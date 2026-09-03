package br.com.minerva.financas.ativo.dto;

import java.time.LocalDate;

public record AtivoResposta(String ativo, String nome, String tipo, LocalDate dataEmissao, LocalDate dataVencimento) {
}
