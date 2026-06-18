package br.com.agendafacil.controller;

import br.com.agendafacil.dto.ProfessionalForm;
import br.com.agendafacil.entity.AppUser;
import br.com.agendafacil.entity.Professional;
import br.com.agendafacil.entity.ServiceOption;
import br.com.agendafacil.repository.ProfessionalRepository;
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

import java.util.HashSet;
import java.util.UUID;

@Controller
@PreAuthorize("hasAnyRole('ADMIN_SISTEMA', 'DONO', 'GERENTE')")
@RequestMapping("/painel/profissionais")
public class PanelProfessionalController {
    private final CurrentUserService currentUserService;
    private final ProfessionalRepository professionalRepository;
    private final ServiceOptionRepository serviceOptionRepository;
    private final AuditService auditService;

    public PanelProfessionalController(CurrentUserService currentUserService, ProfessionalRepository professionalRepository, ServiceOptionRepository serviceOptionRepository, AuditService auditService) {
        this.currentUserService = currentUserService;
        this.professionalRepository = professionalRepository;
        this.serviceOptionRepository = serviceOptionRepository;
        this.auditService = auditService;
    }

    @GetMapping
    public String list(Model model) {
        UUID establishmentId = currentUserService.currentEstablishmentId();
        model.addAttribute("professionals", professionalRepository.findByEstablishmentIdAndActiveTrueOrderByName(establishmentId));
        model.addAttribute("services", serviceOptionRepository.findByEstablishmentIdAndActiveTrueOrderByName(establishmentId));
        model.addAttribute("form", new ProfessionalForm());
        return "panel/professionals";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") ProfessionalForm form, BindingResult result, RedirectAttributes redirectAttributes, HttpServletRequest request, Model model) {
        AppUser user = currentUserService.currentUser();
        UUID establishmentId = user.getEstablishment().getId();
        if (result.hasErrors()) {
            model.addAttribute("professionals", professionalRepository.findByEstablishmentIdAndActiveTrueOrderByName(establishmentId));
            model.addAttribute("services", serviceOptionRepository.findByEstablishmentIdAndActiveTrueOrderByName(establishmentId));
            return "panel/professionals";
        }
        Professional professional = new Professional();
        professional.setEstablishment(user.getEstablishment());
        professional.setName(form.getName().trim());
        professional.setPhone(form.getPhone());
        professional.setServices(new HashSet<>());
        for (UUID serviceId : form.getServiceIds()) {
            serviceOptionRepository.findByIdAndEstablishmentIdAndActiveTrue(serviceId, establishmentId).ifPresent(professional.getServices()::add);
        }
        professionalRepository.save(professional);
        auditService.record(user, user.getEstablishment(), "CRIAR_PROFISSIONAL", "Professional", professional.getId().toString(), professional.getName(), request);
        redirectAttributes.addFlashAttribute("successMessage", "Profissional cadastrado.");
        return "redirect:/painel/profissionais";
    }

    @PostMapping("/{id}/inativar")
    public String inactive(@PathVariable UUID id, RedirectAttributes redirectAttributes, HttpServletRequest request) {
        AppUser user = currentUserService.currentUser();
        professionalRepository.findByIdAndEstablishmentId(id, user.getEstablishment().getId()).ifPresent(professional -> {
            professional.setActive(false);
            professionalRepository.save(professional);
            auditService.record(user, user.getEstablishment(), "INATIVAR_PROFISSIONAL", "Professional", professional.getId().toString(), professional.getName(), request);
        });
        redirectAttributes.addFlashAttribute("successMessage", "Profissional inativado.");
        return "redirect:/painel/profissionais";
    }
}
