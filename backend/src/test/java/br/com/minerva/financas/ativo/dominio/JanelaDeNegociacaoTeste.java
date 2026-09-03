package br.com.minerva.financas.ativo.dominio;

import br.com.minerva.financas.comum.dominio.CalendarioNegociacao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Janela de negociação: emissão inclusiva, vencimento exclusivo e apenas dias de semana. */
class JanelaDeNegociacaoTeste {

    private static Ativo ativo(String emissao, String vencimento) {
        return new Ativo("ATIVO1", "Ativo 1", TipoAtivoEnum.RV, LocalDate.parse(emissao), LocalDate.parse(vencimento));
    }

    @Test
    @DisplayName("a emissão entra na janela e o vencimento fica de fora")
    void janelaEhFechadaNaEmissaoEAbertaNoVencimento() {
        Ativo ativo = ativo("2020-02-03", "2020-02-14");

        assertTrue(ativo.dentroDaJanela(LocalDate.parse("2020-02-03")), "o dia da emissão é negociável");
        assertTrue(ativo.dentroDaJanela(LocalDate.parse("2020-02-13")), "a véspera do vencimento é negociável");
        assertFalse(ativo.dentroDaJanela(LocalDate.parse("2020-02-14")), "o dia do vencimento não é");
        assertFalse(ativo.dentroDaJanela(LocalDate.parse("2020-02-02")), "antes da emissão não é");
    }

    @Test
    @DisplayName("emissão igual ou posterior ao vencimento é ativo inválido")
    void emissaoDeveSerAnteriorAoVencimento() {
        assertThrows(IllegalArgumentException.class, () -> ativo("2020-02-14", "2020-02-14"));
        assertThrows(IllegalArgumentException.class, () -> ativo("2020-02-15", "2020-02-14"));
    }

    @ParameterizedTest(name = "{0} é dia útil")
    @ValueSource(strings = {"2020-02-24", "2020-02-25", "2020-02-26", "2020-02-27", "2020-02-28"})
    void segundaASextaSaoDiasUteis(String data) {
        assertTrue(CalendarioNegociacao.ehDiaUtil(LocalDate.parse(data)));
    }

    @ParameterizedTest(name = "{0} não é dia útil")
    @ValueSource(strings = {"2020-02-29", "2020-03-01"})
    void sabadoEDomingoNaoSaoDiasUteis(String data) {
        assertFalse(CalendarioNegociacao.ehDiaUtil(LocalDate.parse(data)));
    }

    @Test
    @DisplayName("não existe calendário de feriados: um feriado em dia de semana continua negociável")
    void feriadoNaoEhTratadoComoNaoUtil() {
        assertTrue(CalendarioNegociacao.ehDiaUtil(LocalDate.parse("2020-12-25")),
                "inventar um calendário mudaria em silêncio o conjunto de datas aceitas (D-B4)");
    }

    @ParameterizedTest(name = "negociável em {0}: {1}")
    @CsvSource({
            "2020-02-03, true",
            "2020-02-08, false",
            "2020-02-14, false",
            "2020-01-31, false"
    })
    void negociavelCombinaJanelaEDiaUtil(String data, boolean esperado) {
        assertEquals(esperado, ativo("2020-02-03", "2020-02-14").negociavelEm(LocalDate.parse(data)));
    }
}
