package br.com.agendafacil.util;

import br.com.agendafacil.entity.Appointment;
import br.com.agendafacil.enums.AppointmentStatus;

public final class AppointmentViewUtil {
    private AppointmentViewUtil() {}

    public static String statusLabel(AppointmentStatus status) {
        if (status == null) return "Sem status";
        return switch (status) {
            case PENDING_APPROVAL -> "Pendente";
            case CONFIRMED -> "Confirmado";
            case EXPIRED -> "Expirado";
            case CANCELLED -> "Cancelado";
            case COMPLETED -> "Concluído";
            case NO_SHOW -> "Faltou";
        };
    }

    public static String badgeClass(AppointmentStatus status) {
        if (status == null) return "neutral";
        return switch (status) {
            case PENDING_APPROVAL -> "warning";
            case CONFIRMED -> "info";
            case EXPIRED -> "muted";
            case CANCELLED -> "danger-soft";
            case COMPLETED -> "success";
            case NO_SHOW -> "danger";
        };
    }

    public static String rowClass(AppointmentStatus status) {
        if (status == null) return "";
        return switch (status) {
            case PENDING_APPROVAL -> "row-pending";
            case CONFIRMED -> "row-confirmed";
            case EXPIRED -> "row-expired";
            case CANCELLED -> "row-cancelled";
            case COMPLETED -> "row-completed";
            case NO_SHOW -> "row-no-show";
        };
    }

    public static String chipClass(AppointmentStatus status) {
        if (status == null) return "chip-neutral";
        return switch (status) {
            case PENDING_APPROVAL -> "chip-warning";
            case CONFIRMED -> "chip-confirmed";
            case EXPIRED -> "chip-neutral";
            case CANCELLED -> "chip-cancelled";
            case COMPLETED -> "chip-completed";
            case NO_SHOW -> "chip-no-show";
        };
    }

    public static boolean canComplete(Appointment appointment) {
        return appointment != null && appointment.getStatus() == AppointmentStatus.CONFIRMED;
    }

    public static boolean canNoShow(Appointment appointment) {
        return appointment != null && appointment.getStatus() == AppointmentStatus.CONFIRMED;
    }

    public static boolean canCancel(Appointment appointment) {
        return appointment != null && (appointment.getStatus() == AppointmentStatus.CONFIRMED || appointment.getStatus() == AppointmentStatus.PENDING_APPROVAL);
    }
}
