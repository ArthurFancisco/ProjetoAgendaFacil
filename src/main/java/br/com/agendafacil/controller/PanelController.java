package br.com.agendafacil.controller;

import br.com.agendafacil.entity.AppUser;
import br.com.agendafacil.entity.Appointment;
import br.com.agendafacil.enums.AppointmentStatus;
import br.com.agendafacil.repository.AppointmentRepository;
import br.com.agendafacil.service.CurrentUserService;
import br.com.agendafacil.util.AppointmentViewUtil;
import br.com.agendafacil.util.DateTimeUtil;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/painel")
public class PanelController {
    private static final List<AppointmentStatus> ACTIVE_STATUSES = List.of(AppointmentStatus.CONFIRMED, AppointmentStatus.PENDING_APPROVAL);
    private static final List<AppointmentStatus> HISTORY_STATUSES = List.of(AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW, AppointmentStatus.COMPLETED, AppointmentStatus.EXPIRED);

    private final CurrentUserService currentUserService;
    private final AppointmentRepository appointmentRepository;

    public PanelController(CurrentUserService currentUserService, AppointmentRepository appointmentRepository) {
        this.currentUserService = currentUserService;
        this.appointmentRepository = appointmentRepository;
    }

    @GetMapping
    public String dashboard(Model model) {
        AppUser user = currentUserService.currentUser();
        UUID establishmentId = currentUserService.currentEstablishmentId();
        LocalDate today = LocalDate.now();

        List<Appointment> todayAppointments = appointmentRepository.findByEstablishmentIdAndDateOrderByStartTimeAsc(establishmentId, today);
        List<Appointment> pendingAppointments = appointmentRepository.findTop20ByEstablishmentIdAndStatusOrderByCreatedAtAsc(establishmentId, AppointmentStatus.PENDING_APPROVAL);
        List<Appointment> weekAppointments = appointmentRepository.findPeriodByStatuses(establishmentId, today, today.plusDays(6), ACTIVE_STATUSES);
        List<Appointment> recentHistory = appointmentRepository.findTop20History(establishmentId, HISTORY_STATUSES, PageRequest.of(0, 20));

        Map<LocalDate, List<Appointment>> weekBoard = new LinkedHashMap<>();
        for (int i = 0; i < 7; i++) {
            weekBoard.put(today.plusDays(i), new ArrayList<>());
        }
        for (Appointment appointment : weekAppointments) {
            weekBoard.computeIfAbsent(appointment.getDate(), ignored -> new ArrayList<>()).add(appointment);
        }

        long confirmedToday = todayAppointments.stream().filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED).count();
        long noShowToday = todayAppointments.stream().filter(a -> a.getStatus() == AppointmentStatus.NO_SHOW).count();
        long completedToday = todayAppointments.stream().filter(a -> a.getStatus() == AppointmentStatus.COMPLETED).count();

        model.addAttribute("user", user);
        model.addAttribute("establishment", user.getEstablishment());
        model.addAttribute("todayAppointments", todayAppointments);
        model.addAttribute("pendingAppointments", pendingAppointments);
        model.addAttribute("weekAppointments", weekAppointments);
        model.addAttribute("recentHistory", recentHistory);
        model.addAttribute("weekBoard", weekBoard);
        model.addAttribute("confirmedToday", confirmedToday);
        model.addAttribute("noShowToday", noShowToday);
        model.addAttribute("completedToday", completedToday);
        model.addAttribute("dateUtil", DateTimeUtil.class);
        model.addAttribute("appointmentView", AppointmentViewUtil.class);
        return "panel/dashboard";
    }
}
