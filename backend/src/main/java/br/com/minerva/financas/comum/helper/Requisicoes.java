package br.com.minerva.financas.comum.helper;

import br.com.minerva.financas.comum.dominio.ErroAplicacao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Validação e conversão da fronteira HTTP.
 * <p>
 * Fica aqui, e não no domínio, porque trata de <em>forma</em> da requisição — escala decimal, sinal,
 * presença de campo e parse de data. As regras financeiras propriamente ditas ficam no domínio e nos
 * casos de uso.
 */
public final class Requisicoes {

    private Requisicoes() {
    }

    /**
     * Converte um decimal monetário ou de quantidade em inteiro escalado, rejeitando escala excedente,
     * sinal inválido e valor não representável. A conversão é exata: nunca há arredondamento silencioso.
     */
    public static long escalado(BigDecimal valor, String campo, int casas, boolean estritamentePositivo) {
        if (valor == null) {
            throw ErroAplicacao.campoInvalido(campo, "O campo " + campo + " é obrigatório.");
        }
        if (valor.scale() > casas) {
            throw ErroAplicacao.campoInvalido(campo,
                    "O campo " + campo + " admite no máximo " + casas + " casas decimais.");
        }
        if (estritamentePositivo ? valor.signum() <= 0 : valor.signum() < 0) {
            throw ErroAplicacao.campoInvalido(campo, "O campo " + campo
                    + (estritamentePositivo ? " deve ser positivo." : " não pode ser negativo."));
        }
        try {
            return valor.movePointRight(casas).longValueExact();
        } catch (ArithmeticException e) {
            throw ErroAplicacao.campoInvalido(campo, "O campo " + campo + " está fora da faixa representável.");
        }
    }

    public static String texto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw ErroAplicacao.campoInvalido(campo, "O campo " + campo + " é obrigatório.");
        }
        return valor;
    }

    public static LocalDate dataObrigatoria(LocalDate data, String campo) {
        if (data == null) {
            throw ErroAplicacao.campoInvalido(campo, "O campo " + campo + " é obrigatório.");
        }
        return data;
    }

    /** Filtro de data vindo da query string ou do caminho, parseado aqui para responder 400, nunca 500. */
    public static LocalDate filtroDeData(String bruto, String campo) {
        if (bruto == null || bruto.isBlank()) {
            throw ErroAplicacao.filtroInvalido("O filtro " + campo + " é obrigatório.");
        }
        try {
            return LocalDate.parse(bruto);
        } catch (DateTimeParseException e) {
            throw ErroAplicacao.filtroInvalido(
                    "O filtro " + campo + " deve estar no formato YYYY-MM-DD.");
        }
    }

    public static Intervalo intervalo(String inicioBruto, String fimBruto) {
        LocalDate inicio = filtroDeData(inicioBruto, "dataInicio");
        LocalDate fim = filtroDeData(fimBruto, "dataFim");
        if (inicio.isAfter(fim)) {
            throw ErroAplicacao.filtroInvalido("O intervalo está invertido: dataInicio é posterior a dataFim.");
        }
        return new Intervalo(inicio, fim);
    }

    public record Intervalo(LocalDate inicio, LocalDate fim) {
    }
}
