package br.com.agendafacil.util;

import br.com.agendafacil.entity.Appointment;
import br.com.agendafacil.enums.AppointmentStatus;

public final class AppointmentViewUtil {
    private AppointmentViewUtil() {
    }

    public static String statusLabel(AppointmentStatus status) {
        if (status == null) {
            return "Sem status";
        }
        return switch (status) {
            case PENDING_APPROVAL -> "Pendente";
            case CONFIRMED -> "Confirmado";
            case COMPLETED -> "Concluido";
            case NO_SHOW -> "Faltou";
            case CANCELLED -> "Cancelado";
            case EXPIRED -> "Expirado";
        };
    }

    public static String badgeClass(AppointmentStatus status) {
        if (status == null) {
            return "neutral";
        }
        return switch (status) {
            case PENDING_APPROVAL -> "pending";
            case CONFIRMED -> "confirmed";
            case COMPLETED -> "completed";
            case NO_SHOW -> "no-show";
            case CANCELLED -> "cancelled";
            case EXPIRED -> "expired";
        };
    }

    public static String rowClass(AppointmentStatus status) {
        if (status == null) {
            return "";
        }
        return "status-row status-row-" + badgeClass(status);
    }

    public static String chipClass(AppointmentStatus status) {
        if (status == null) {
            return "chip-neutral";
        }
        return "chip-" + badgeClass(status);
    }

    public static String historyDetail(Appointment appointment) {
        if (appointment == null || appointment.getStatus() == null) {
            return "Sem detalhe adicional.";
        }
        if (appointment.getCancellationReason() != null && !appointment.getCancellationReason().isBlank()) {
            return appointment.getCancellationReason();
        }
        return switch (appointment.getStatus()) {
            case PENDING_APPROVAL -> "Aguardando analise do estabelecimento.";
            case CONFIRMED -> "Horario confirmado e reservado para o cliente.";
            case COMPLETED -> "Atendimento finalizado com sucesso.";
            case NO_SHOW -> "Cliente nao compareceu ao horario reservado.";
            case CANCELLED -> "Horario liberado novamente para novos agendamentos.";
            case EXPIRED -> "A reserva pendente expirou sem aprovacao.";
        };
    }

    public static boolean canApprove(Appointment appointment) {
        return appointment != null && appointment.getStatus() == AppointmentStatus.PENDING_APPROVAL;
    }

    public static boolean canReject(Appointment appointment) {
        return canApprove(appointment);
    }

    public static boolean canComplete(Appointment appointment) {
        return appointment != null && appointment.getStatus() == AppointmentStatus.CONFIRMED;
    }

    public static boolean canNoShow(Appointment appointment) {
        return appointment != null && appointment.getStatus() == AppointmentStatus.CONFIRMED;
    }

    public static boolean canCancel(Appointment appointment) {
        return appointment != null && appointment.getStatus() == AppointmentStatus.CONFIRMED;
    }

    public static boolean hasPendingAction(Appointment appointment) {
        return canApprove(appointment) || canReject(appointment) || canComplete(appointment) || canNoShow(appointment) || canCancel(appointment);
    }
}
