package br.com.minerva.financas.comum.dominio;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Dia útil de negociação, conforme decisão D-B4 do FDD-002.
 * <p>
 * Dia útil é qualquer dia de segunda a sexta-feira. <strong>Não existe calendário de feriados</strong>:
 * nem o enunciado nem o PRD definem um, e inventá-lo mudaria silenciosamente o conjunto de datas
 * aceitas pela aplicação.
 */
public final class CalendarioNegociacao {

    private CalendarioNegociacao() {
    }

    public static boolean ehDiaUtil(LocalDate data) {
        DayOfWeek dia = data.getDayOfWeek();
        return dia != DayOfWeek.SATURDAY && dia != DayOfWeek.SUNDAY;
    }
}
