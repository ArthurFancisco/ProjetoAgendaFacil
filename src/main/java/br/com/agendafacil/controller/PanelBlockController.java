package br.com.agendafacil.controller;

import br.com.agendafacil.dto.BlockForm;
import br.com.agendafacil.entity.AppUser;
import br.com.agendafacil.entity.BlockedTime;
import br.com.agendafacil.entity.Professional;
import br.com.agendafacil.exception.BusinessException;
import br.com.agendafacil.repository.BlockedTimeRepository;
import br.com.agendafacil.repository.ProfessionalRepository;
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
@RequestMapping("/painel/bloqueios")
public class PanelBlockController {
    private final CurrentUserService currentUserService;
    private final BlockedTimeRepository blockedTimeRepository;
    private final ProfessionalRepository professionalRepository;
    private final AuditService auditService;

    public PanelBlockController(CurrentUserService currentUserService, BlockedTimeRepository blockedTimeRepository, ProfessionalRepository professionalRepository, AuditService auditService) {
        this.currentUserService = currentUserService;
        this.blockedTimeRepository = blockedTimeRepository;
        this.professionalRepository = professionalRepository;
        this.auditService = auditService;
    }

    @GetMapping
    public String list(Model model) {
        UUID establishmentId = currentUserService.currentEstablishmentId();
        model.addAttribute("blocks", blockedTimeRepository.findTop20ByEstablishmentIdOrderByDateDescStartTimeAsc(establishmentId));
        model.addAttribute("professionals", professionalRepository.findByEstablishmentIdAndActiveTrueOrderByName(establishmentId));
        model.addAttribute("form", new BlockForm());
        return "panel/blocks";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") BlockForm form, BindingResult result, Model model, RedirectAttributes redirectAttributes, HttpServletRequest request) {
        AppUser user = currentUserService.currentUser();
        UUID establishmentId = user.getEstablishment().getId();
        if (result.hasErrors()) {
            model.addAttribute("blocks", blockedTimeRepository.findTop20ByEstablishmentIdOrderByDateDescStartTimeAsc(establishmentId));
            model.addAttribute("professionals", professionalRepository.findByEstablishmentIdAndActiveTrueOrderByName(establishmentId));
            return "panel/blocks";
        }
        if (!form.getStartTime().isBefore(form.getEndTime())) {
            throw new BusinessException("O início deve ser antes do fim do bloqueio.");
        }
        BlockedTime block = new BlockedTime();
        block.setEstablishment(user.getEstablishment());
        block.setDate(form.getDate());
        block.setStartTime(form.getStartTime());
        block.setEndTime(form.getEndTime());
        block.setReason(form.getReason().trim());
        block.setCreatedBy(user);
        if (form.getProfessionalId() != null) {
            Professional professional = professionalRepository.findByIdAndEstablishmentId(form.getProfessionalId(), establishmentId)
                    .orElseThrow(() -> new BusinessException("Profissional não encontrado."));
            block.setProfessional(professional);
        }
        blockedTimeRepository.save(block);
        auditService.record(user, user.getEstablishment(), "CRIAR_BLOQUEIO", "BlockedTime", block.getId().toString(), block.getReason(), request);
        redirectAttributes.addFlashAttribute("successMessage", "Horário bloqueado.");
        return "redirect:/painel/bloqueios";
    }
}
