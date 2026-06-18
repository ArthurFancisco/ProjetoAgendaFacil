package br.com.agendafacil.controller;

import br.com.agendafacil.dto.ServiceForm;
import br.com.agendafacil.entity.AppUser;
import br.com.agendafacil.entity.ServiceOption;
import br.com.agendafacil.repository.ServiceOptionRepository;
import br.com.agendafacil.service.AuditService;
import br.com.agendafacil.service.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@PreAuthorize("hasAnyRole('ADMIN_SISTEMA', 'DONO', 'GERENTE')")
@RequestMapping("/painel/servicos")
public class PanelServiceController {
    private final CurrentUserService currentUserService;
    private final ServiceOptionRepository serviceOptionRepository;
    private final AuditService auditService;

    public PanelServiceController(CurrentUserService currentUserService, ServiceOptionRepository serviceOptionRepository, AuditService auditService) {
        this.currentUserService = currentUserService;
        this.serviceOptionRepository = serviceOptionRepository;
        this.auditService = auditService;
    }

    @GetMapping
    public String list(Model model) {
        UUID establishmentId = currentUserService.currentEstablishmentId();
        model.addAttribute("services", serviceOptionRepository.findByEstablishmentIdAndActiveTrueOrderByName(establishmentId));
        model.addAttribute("form", new ServiceForm());
        return "panel/services";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") ServiceForm form, BindingResult result, RedirectAttributes redirectAttributes, HttpServletRequest request, Model model) {
        AppUser user = currentUserService.currentUser();
        if (result.hasErrors()) {
            model.addAttribute("services", serviceOptionRepository.findByEstablishmentIdAndActiveTrueOrderByName(user.getEstablishment().getId()));
            return "panel/services";
        }
        ServiceOption service = new ServiceOption();
        service.setEstablishment(user.getEstablishment());
        service.setName(form.getName().trim());
        service.setDescription(form.getDescription());
        service.setPrice(form.getPrice());
        service.setDurationMinutes(form.getDurationMinutes());
        service.setBufferMinutes(form.getBufferMinutes());
        service.setRequiresManualApproval(form.isRequiresManualApproval());
        serviceOptionRepository.save(service);
        auditService.record(user, user.getEstablishment(), "CRIAR_SERVICO", "ServiceOption", service.getId().toString(), service.getName(), request);
        redirectAttributes.addFlashAttribute("successMessage", "Serviço cadastrado.");
        return "redirect:/painel/servicos";
    }

    @PostMapping("/{id}/inativar")
    public String inactive(@PathVariable UUID id, RedirectAttributes redirectAttributes, HttpServletRequest request) {
        AppUser user = currentUserService.currentUser();
        serviceOptionRepository.findByIdAndEstablishmentId(id, user.getEstablishment().getId()).ifPresent(service -> {
            service.setActive(false);
            serviceOptionRepository.save(service);
            auditService.record(user, user.getEstablishment(), "INATIVAR_SERVICO", "ServiceOption", service.getId().toString(), service.getName(), request);
        });
        redirectAttributes.addFlashAttribute("successMessage", "Serviço inativado.");
        return "redirect:/painel/servicos";
    }
}
