package br.com.agendafacil.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record SlotOption(LocalDate date, LocalTime startTime, String label, boolean available) {
}
