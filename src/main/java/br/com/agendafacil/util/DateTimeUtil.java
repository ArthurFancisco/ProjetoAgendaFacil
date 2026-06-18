package br.com.agendafacil.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class DateTimeUtil {
    private static final DateTimeFormatter DATE_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_BR = DateTimeFormatter.ofPattern("HH:mm");

    private DateTimeUtil() {}

    public static String dateBr(LocalDate date) {
        return date == null ? "" : date.format(DATE_BR);
    }

    public static String timeBr(LocalTime time) {
        return time == null ? "" : time.format(TIME_BR);
    }

    public static String weekDayPt(LocalDate date) {
        if (date == null) return "";
        return date.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, Locale.forLanguageTag("pt-BR"));
    }
}
