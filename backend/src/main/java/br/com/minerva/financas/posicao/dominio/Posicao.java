package br.com.minerva.financas.posicao.dominio;

import br.com.minerva.financas.ativo.dominio.Ativo;
import br.com.minerva.financas.comum.dominio.PrecoUnitario;

public record Posicao(Ativo ativo, CalculoPosicao.Resultado resultado, PrecoUnitario precoMercado) {
}
