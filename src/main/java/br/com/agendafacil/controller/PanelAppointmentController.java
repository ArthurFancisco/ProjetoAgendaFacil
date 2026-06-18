package br.com.agendafacil.controller;

import br.com.agendafacil.entity.AppUser;
import br.com.agendafacil.exception.BusinessException;
import br.com.agendafacil.service.AppointmentService;
import br.com.agendafacil.service.AuditService;
import br.com.agendafacil.service.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/painel/agendamentos")
public class PanelAppointmentController {
    private final AppointmentService appointmentService;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    public PanelAppointmentController(AppointmentService appointmentService, CurrentUserService currentUserService, AuditService auditService) {
        this.appointmentService = appointmentService;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
    }

    @PostMapping("/{id}/aprovar")
    public String approve(@PathVariable UUID id, RedirectAttributes redirectAttributes, HttpServletRequest request) {
        AppUser user = currentUserService.currentUser();
        try {
            appointmentService.approve(id, user.getEstablishment().getId());
            auditService.record(user, user.getEstablishment(), "APROVAR_AGENDAMENTO", "Appointment", id.toString(), "Agendamento aprovado", request);
            redirectAttributes.addFlashAttribute("successMessage", "Agendamento aprovado com segurança.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/painel";
    }

    @PostMapping("/{id}/cancelar")
    public String cancel(@PathVariable UUID id, @RequestParam(defaultValue = "Cancelado pelo estabelecimento") String reason,
                         RedirectAttributes redirectAttributes, HttpServletRequest request) {
        AppUser user = currentUserService.currentUser();
        try {
            appointmentService.cancel(id, user.getEstablishment().getId(), reason);
            auditService.record(user, user.getEstablishment(), "CANCELAR_AGENDAMENTO", "Appointment", id.toString(), reason, request);
            redirectAttributes.addFlashAttribute("successMessage", "Agendamento cancelado.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/painel";
    }

    @PostMapping("/{id}/concluir")
    public String complete(@PathVariable UUID id, RedirectAttributes redirectAttributes, HttpServletRequest request) {
        AppUser user = currentUserService.currentUser();
        appointmentService.complete(id, user.getEstablishment().getId());
        auditService.record(user, user.getEstablishment(), "CONCLUIR_AGENDAMENTO", "Appointment", id.toString(), "Atendimento concluído", request);
        redirectAttributes.addFlashAttribute("successMessage", "Atendimento concluído.");
        return "redirect:/painel";
    }

    @PostMapping("/{id}/faltou")
    public String noShow(@PathVariable UUID id, RedirectAttributes redirectAttributes, HttpServletRequest request) {
        AppUser user = currentUserService.currentUser();
        appointmentService.markNoShow(id, user.getEstablishment().getId());
        auditService.record(user, user.getEstablishment(), "MARCAR_FALTA", "Appointment", id.toString(), "Cliente faltou", request);
        redirectAttributes.addFlashAttribute("successMessage", "Falta registrada no histórico do cliente.");
        return "redirect:/painel";
    }
}
