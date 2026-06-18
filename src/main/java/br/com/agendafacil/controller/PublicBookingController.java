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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
                .orElseThrow(() -> new BusinessException("Servico indisponivel."));
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
                .orElseThrow(() -> new BusinessException("Servico indisponivel."));
        Professional professional = professionalRepository.findByIdAndEstablishmentId(professionalId, establishment.getId())
                .orElseThrow(() -> new BusinessException("Profissional indisponivel."));
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
        ServiceOption service = loadService(establishment.getId(), serviceId);
        Professional professional = loadProfessional(establishment.getId(), professionalId);

        PublicAppointmentForm form = new PublicAppointmentForm();
        form.setServiceId(serviceId);
        form.setProfessionalId(professionalId);
        form.setDate(date);
        form.setStartTime(startTime);

        fillDataPageModel(model, establishment, service, professional, form);
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
        ServiceOption service = loadService(establishment.getId(), form.getServiceId());
        Professional professional = loadProfessional(establishment.getId(), form.getProfessionalId());

        if (bindingResult.hasErrors()) {
            fillDataPageModel(model, establishment, service, professional, form);
            model.addAttribute("errorMessage", "Revise os dados informados e tente novamente.");
            return "public/data";
        }

        try {
            Appointment appointment = appointmentService.createPublicAppointment(slug, form, request);
            redirectAttributes.addFlashAttribute("appointmentId", appointment.getId());
            return "redirect:/b/" + slug + "/sucesso/" + appointment.getId();
        } catch (BusinessException ex) {
            fillDataPageModel(model, establishment, service, professional, form);
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

    private void fillDataPageModel(Model model,
                                   Establishment establishment,
                                   ServiceOption service,
                                   Professional professional,
                                   PublicAppointmentForm form) {
        model.addAttribute("establishment", establishment);
        model.addAttribute("service", service);
        model.addAttribute("professional", professional);
        model.addAttribute("form", form);
        model.addAttribute("dateUtil", DateTimeUtil.class);
    }

    private Establishment loadEstablishment(String slug) {
        return establishmentRepository.findBySlugAndActiveTrue(slug)
                .orElseThrow(() -> new BusinessException("Pagina de agendamento nao encontrada."));
    }

    private ServiceOption loadService(UUID establishmentId, UUID serviceId) {
        return serviceOptionRepository.findByIdAndEstablishmentIdAndActiveTrue(serviceId, establishmentId)
                .orElseThrow(() -> new BusinessException("Servico indisponivel."));
    }

    private Professional loadProfessional(UUID establishmentId, UUID professionalId) {
        return professionalRepository.findByIdAndEstablishmentId(professionalId, establishmentId)
                .orElseThrow(() -> new BusinessException("Profissional indisponivel."));
    }
}
