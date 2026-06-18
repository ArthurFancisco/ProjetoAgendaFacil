package br.com.agendafacil.controller;

import br.com.agendafacil.entity.AppUser;
import br.com.agendafacil.entity.Appointment;
import br.com.agendafacil.enums.AppointmentStatus;
import br.com.agendafacil.repository.AppointmentRepository;
import br.com.agendafacil.service.AppointmentService;
import br.com.agendafacil.service.CurrentUserService;
import br.com.agendafacil.util.AppointmentViewUtil;
import br.com.agendafacil.util.DateTimeUtil;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Controller
@RequestMapping("/painel")
public class PanelController {
    private static final List<AppointmentStatus> ACTIVE_STATUSES = List.of(
            AppointmentStatus.CONFIRMED,
            AppointmentStatus.PENDING_APPROVAL
    );
    private static final List<AppointmentStatus> HISTORY_STATUSES = List.of(
            AppointmentStatus.CANCELLED,
            AppointmentStatus.NO_SHOW,
            AppointmentStatus.COMPLETED,
            AppointmentStatus.EXPIRED
    );
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private final CurrentUserService currentUserService;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentService appointmentService;

    public PanelController(CurrentUserService currentUserService,
                           AppointmentRepository appointmentRepository,
                           AppointmentService appointmentService) {
        this.currentUserService = currentUserService;
        this.appointmentRepository = appointmentRepository;
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public String dashboard(Model model) {
        appointmentService.refreshExpiredPendings();

        AppUser user = currentUserService.currentUser();
        UUID establishmentId = currentUserService.currentEstablishmentId();
        LocalDate today = LocalDate.now();

        List<Appointment> todayAppointments = appointmentRepository.findByEstablishmentIdAndDateOrderByStartTimeAsc(establishmentId, today);
        List<Appointment> pendingAppointments = appointmentRepository.findPendingDashboard(
                establishmentId,
                AppointmentStatus.PENDING_APPROVAL,
                java.time.LocalDateTime.now(),
                PageRequest.of(0, 20)
        );
        List<Appointment> weekAppointments = appointmentRepository.findPeriodByStatuses(establishmentId, today, today.plusDays(6), ACTIVE_STATUSES);
        List<Appointment> recentHistory = appointmentRepository.findTop20History(establishmentId, HISTORY_STATUSES, PageRequest.of(0, 20));

        List<WeekDayColumn> weekBoardDays = buildWeekBoard(today, weekAppointments);
        long confirmedToday = todayAppointments.stream().filter(appointment -> appointment.getStatus() == AppointmentStatus.CONFIRMED).count();
        long pendingToday = todayAppointments.stream().filter(appointment -> appointment.getStatus() == AppointmentStatus.PENDING_APPROVAL).count();
        long noShowToday = todayAppointments.stream().filter(appointment -> appointment.getStatus() == AppointmentStatus.NO_SHOW).count();
        long completedToday = todayAppointments.stream().filter(appointment -> appointment.getStatus() == AppointmentStatus.COMPLETED).count();

        model.addAttribute("user", user);
        model.addAttribute("establishment", user.getEstablishment());
        model.addAttribute("todayAppointments", todayAppointments);
        model.addAttribute("pendingAppointments", pendingAppointments);
        model.addAttribute("recentHistory", recentHistory);
        model.addAttribute("weekBoardDays", weekBoardDays);
        model.addAttribute("confirmedToday", confirmedToday);
        model.addAttribute("pendingToday", pendingToday);
        model.addAttribute("noShowToday", noShowToday);
        model.addAttribute("completedToday", completedToday);
        model.addAttribute("dateUtil", DateTimeUtil.class);
        model.addAttribute("appointmentView", AppointmentViewUtil.class);
        return "panel/dashboard";
    }

    private List<WeekDayColumn> buildWeekBoard(LocalDate startDate, List<Appointment> weekAppointments) {
        List<WeekDayColumn> days = new ArrayList<>();
        for (int index = 0; index < 7; index++) {
            LocalDate date = startDate.plusDays(index);
            List<Appointment> dayAppointments = weekAppointments.stream()
                    .filter(appointment -> appointment.getDate().equals(date))
                    .toList();
            days.add(new WeekDayColumn(
                    date,
                    date.getDayOfWeek().getDisplayName(TextStyle.FULL, PT_BR),
                    date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM")),
                    dayAppointments,
                    dayAppointments.size()
            ));
        }
        return days;
    }

    public record WeekDayColumn(
            LocalDate date,
            String weekdayLabel,
            String dateLabel,
            List<Appointment> appointments,
            int activeCount
    ) {
    }
}
