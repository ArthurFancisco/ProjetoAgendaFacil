package br.com.agendafacil.controller;

import br.com.agendafacil.dto.PublicAppointmentForm;
import br.com.agendafacil.entity.Appointment;
import br.com.agendafacil.entity.Establishment;
import br.com.agendafacil.entity.Professional;
import br.com.agendafacil.entity.ServiceOption;
import br.com.agendafacil.exception.BusinessException;
import br.com.agendafacil.repository.EstablishmentRepository;
import br.com.agendafacil.repository.ProfessionalRepository;
import br.com.agendafacil.repository.ServiceOptionRepository;
import br.com.agendafacil.service.AppointmentService;
import br.com.agendafacil.util.DateTimeUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/b/{slug}")
public class PublicBookingController {
    private final EstablishmentRepository establishmentRepository;
    private final ServiceOptionRepository serviceOptionRepository;
    private final ProfessionalRepository professionalRepository;
    private final AppointmentService appointmentService;

    public PublicBookingController(EstablishmentRepository establishmentRepository,
                                   ServiceOptionRepository serviceOptionRepository,
                                   ProfessionalRepository professionalRepository,
                                   AppointmentService appointmentService) {
        this.establishmentRepository = establishmentRepository;
        this.serviceOptionRepository = serviceOptionRepository;
        this.professionalRepository = professionalRepository;
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public String establishment(@PathVariable String slug, Model model) {
        Establishment establishment = loadEstablishment(slug);
        model.addAttribute("establishment", establishment);
        model.addAttribute("services", serviceOptionRepository.findByEstablishmentIdAndActiveTrueOrderByName(establishment.getId()));
        return "public/establishment";
    }

    @GetMapping("/profissionais")
    public String professionals(@PathVariable String slug, @RequestParam UUID serviceId, Model model) {
        Establishment establishment = loadEstablishment(slug);
        ServiceOption service = serviceOptionRepository.findByIdAndEstablishmentIdAndActiveTrue(serviceId, establishment.getId())
                .orElseThrow(() -> new BusinessException("Serviço indisponível."));
        List<Professional> professionals = professionalRepository.findActiveByService(establishment.getId(), serviceId);
        model.addAttribute("establishment", establishment);
        model.addAttribute("service", service);
        model.addAttribute("professionals", professionals);
        return "public/professionals";
    }

    @GetMapping("/horarios")
    public String slots(@PathVariable String slug,
                        @RequestParam UUID serviceId,
                        @RequestParam UUID professionalId,
                        @RequestParam(required = false) LocalDate date,
                        Model model) {
        Establishment establishment = loadEstablishment(slug);
        ServiceOption service = serviceOptionRepository.findByIdAndEstablishmentIdAndActiveTrue(serviceId, establishment.getId())
                .orElseThrow(() -> new BusinessException("Serviço indisponível."));
        Professional professional = professionalRepository.findByIdAndEstablishmentId(professionalId, establishment.getId())
                .orElseThrow(() -> new BusinessException("Profissional indisponível."));
        LocalDate selectedDate = date == null ? LocalDate.now() : date;
        model.addAttribute("establishment", establishment);
        model.addAttribute("service", service);
        model.addAttribute("professional", professional);
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("nextDates", LocalDate.now().datesUntil(LocalDate.now().plusDays(10)).toList());
        model.addAttribute("slots", appointmentService.listAvailableSlots(establishment.getId(), serviceId, professionalId, selectedDate));
        model.addAttribute("dateUtil", DateTimeUtil.class);
        return "public/slots";
    }

    @GetMapping("/dados")
    public String data(@PathVariable String slug,
                       @RequestParam UUID serviceId,
                       @RequestParam UUID professionalId,
                       @RequestParam LocalDate date,
                       @RequestParam LocalTime startTime,
                       Model model) {
        Establishment establishment = loadEstablishment(slug);
        ServiceOption service = serviceOptionRepository.findByIdAndEstablishmentIdAndActiveTrue(serviceId, establishment.getId())
                .orElseThrow(() -> new BusinessException("Serviço indisponível."));
        Professional professional = professionalRepository.findByIdAndEstablishmentId(professionalId, establishment.getId())
                .orElseThrow(() -> new BusinessException("Profissional indisponível."));
        PublicAppointmentForm form = new PublicAppointmentForm();
        form.setServiceId(serviceId);
        form.setProfessionalId(professionalId);
        form.setDate(date);
        form.setStartTime(startTime);
        model.addAttribute("establishment", establishment);
        model.addAttribute("service", service);
        model.addAttribute("professional", professional);
        model.addAttribute("form", form);
        model.addAttribute("dateUtil", DateTimeUtil.class);
        return "public/data";
    }

    @PostMapping("/agendar")
    public String schedule(@PathVariable String slug,
                           @Valid @ModelAttribute("form") PublicAppointmentForm form,
                           BindingResult bindingResult,
                           HttpServletRequest request,
                           RedirectAttributes redirectAttributes,
                           Model model) {
        Establishment establishment = loadEstablishment(slug);
        if (bindingResult.hasErrors()) {
            model.addAttribute("establishment", establishment);
            model.addAttribute("errorMessage", "Revise os dados informados e tente novamente.");
            return "public/data";
        }
        try {
            Appointment appointment = appointmentService.createPublicAppointment(slug, form, request);
            redirectAttributes.addFlashAttribute("appointmentId", appointment.getId());
            return "redirect:/b/" + slug + "/sucesso/" + appointment.getId();
        } catch (BusinessException ex) {
            model.addAttribute("establishment", establishment);
            model.addAttribute("errorMessage", ex.getMessage());
            return "public/data";
        }
    }

    @GetMapping("/sucesso/{appointmentId}")
    public String success(@PathVariable String slug, @PathVariable UUID appointmentId, Model model) {
        Establishment establishment = loadEstablishment(slug);
        model.addAttribute("establishment", establishment);
        model.addAttribute("appointmentId", appointmentId);
        return "public/success";
    }

    private Establishment loadEstablishment(String slug) {
        return establishmentRepository.findBySlugAndActiveTrue(slug)
                .orElseThrow(() -> new BusinessException("Página de agendamento não encontrada."));
    }
}
