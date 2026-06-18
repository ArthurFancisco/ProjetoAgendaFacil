package br.com.agendafacil.service;

import br.com.agendafacil.dto.PublicAppointmentForm;
import br.com.agendafacil.dto.SlotOption;
import br.com.agendafacil.entity.*;
import br.com.agendafacil.enums.AppointmentStatus;
import br.com.agendafacil.enums.ClientStatus;
import br.com.agendafacil.exception.BusinessException;
import br.com.agendafacil.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AppointmentService {
    private static final List<AppointmentStatus> BLOCKING_STATUSES = List.of(
            AppointmentStatus.CONFIRMED,
            AppointmentStatus.PENDING_APPROVAL
    );

    private final EstablishmentRepository establishmentRepository;
    private final ServiceOptionRepository serviceOptionRepository;
    private final ProfessionalRepository professionalRepository;
    private final BusinessHourRepository businessHourRepository;
    private final AppointmentRepository appointmentRepository;
    private final BlockedTimeRepository blockedTimeRepository;
    private final ClientRepository clientRepository;
    private final PhoneNormalizer phoneNormalizer;
    private final BookingGuardService bookingGuardService;
    private final int temporaryHoldMinutes;
    private final int slotStepMinutes;

    public AppointmentService(EstablishmentRepository establishmentRepository,
                              ServiceOptionRepository serviceOptionRepository,
                              ProfessionalRepository professionalRepository,
                              BusinessHourRepository businessHourRepository,
                              AppointmentRepository appointmentRepository,
                              BlockedTimeRepository blockedTimeRepository,
                              ClientRepository clientRepository,
                              PhoneNormalizer phoneNormalizer,
                              BookingGuardService bookingGuardService,
                              @Value("${app.booking.temporary-hold-minutes}") int temporaryHoldMinutes,
                              @Value("${app.booking.slot-step-minutes}") int slotStepMinutes) {
        this.establishmentRepository = establishmentRepository;
        this.serviceOptionRepository = serviceOptionRepository;
        this.professionalRepository = professionalRepository;
        this.businessHourRepository = businessHourRepository;
        this.appointmentRepository = appointmentRepository;
        this.blockedTimeRepository = blockedTimeRepository;
        this.clientRepository = clientRepository;
        this.phoneNormalizer = phoneNormalizer;
        this.bookingGuardService = bookingGuardService;
        this.temporaryHoldMinutes = temporaryHoldMinutes;
        this.slotStepMinutes = slotStepMinutes;
    }

    @Transactional(readOnly = true)
    public List<SlotOption> listAvailableSlots(UUID establishmentId, UUID serviceId, UUID professionalId, LocalDate date) {
        if (date == null || date.isBefore(LocalDate.now())) {
            return List.of();
        }

        Establishment establishment = establishmentRepository.findById(establishmentId)
                .orElseThrow(() -> new BusinessException("Estabelecimento não encontrado."));
        ServiceOption service = serviceOptionRepository.findByIdAndEstablishmentIdAndActiveTrue(serviceId, establishmentId)
                .orElseThrow(() -> new BusinessException("Serviço não encontrado."));
        Professional professional = professionalRepository.findByIdAndEstablishmentId(professionalId, establishmentId)
                .orElseThrow(() -> new BusinessException("Profissional não encontrado."));

        BusinessHour hour = findBusinessHour(establishment, professional, date);
        if (hour == null) {
            return List.of();
        }

        List<SlotOption> slots = new ArrayList<>();
        LocalTime cursor = hour.getOpenTime();
        LocalTime lastStart = hour.getCloseTime().minusMinutes(service.totalMinutes());
        while (!cursor.isAfter(lastStart)) {
            LocalTime end = cursor.plusMinutes(service.totalMinutes());
            boolean available = isSlotAvailable(establishmentId, professionalId, date, cursor, end);
            slots.add(new SlotOption(date, cursor, cursor.toString(), available));
            cursor = cursor.plusMinutes(slotStepMinutes);
        }
        return slots;
    }

    @Transactional
    public Appointment createPublicAppointment(String slug, PublicAppointmentForm form, HttpServletRequest request) {
        Establishment establishment = establishmentRepository.findBySlugAndActiveTrue(slug)
                .orElseThrow(() -> new BusinessException("Estabelecimento não encontrado."));
        ServiceOption service = serviceOptionRepository.findByIdAndEstablishmentIdAndActiveTrue(form.getServiceId(), establishment.getId())
                .orElseThrow(() -> new BusinessException("Serviço indisponível."));
        Professional professional = professionalRepository.findByIdAndEstablishmentId(form.getProfessionalId(), establishment.getId())
                .orElseThrow(() -> new BusinessException("Profissional indisponível."));

        if (professional.getServices().stream().noneMatch(s -> s.getId().equals(service.getId()))) {
            throw new BusinessException("Esse profissional não realiza o serviço escolhido.");
        }

        String phoneNormalized = phoneNormalizer.normalize(form.getClientPhone());
        bookingGuardService.validatePublicAttempt(establishment, phoneNormalized, form.getWebsite(), request);

        Client client = clientRepository.findByEstablishmentIdAndPhoneNormalized(establishment.getId(), phoneNormalized)
                .orElseGet(() -> newClient(establishment, form.getClientName(), form.getClientPhone(), phoneNormalized));
        client.setName(clean(form.getClientName(), 80));
        client.setPhoneOriginal(clean(form.getClientPhone(), 40));

        if (client.getStatus() == ClientStatus.BLOCKED) {
            throw new BusinessException("Para marcar um novo horário, fale diretamente com o estabelecimento.");
        }

        LocalTime endTime = form.getStartTime().plusMinutes(service.totalMinutes());
        validateInsideBusinessHours(establishment, professional, form.getDate(), form.getStartTime(), endTime);
        if (!isSlotAvailable(establishment.getId(), professional.getId(), form.getDate(), form.getStartTime(), endTime)) {
            throw new BusinessException("Esse horário acabou de ficar indisponível. Escolha outro horário.");
        }

        Appointment appointment = new Appointment();
        appointment.setEstablishment(establishment);
        appointment.setClient(client);
        appointment.setProfessional(professional);
        appointment.setService(service);
        appointment.setDate(form.getDate());
        appointment.setStartTime(form.getStartTime());
        appointment.setEndTime(endTime);
        appointment.setExpectedValue(service.getPrice());
        appointment.setObservation(clean(form.getObservation(), 255));
        appointment.setConfirmToken(UUID.randomUUID().toString());
        appointment.setCancelToken(UUID.randomUUID().toString());

        boolean trustedClient = client.isPhoneVerified() && client.getStatus() == ClientStatus.NORMAL && client.getNoShowCount() < 2 && !service.isRequiresManualApproval();
        if (trustedClient) {
            appointment.setStatus(AppointmentStatus.CONFIRMED);
            appointment.setConfirmedAt(LocalDateTime.now());
        } else {
            appointment.setStatus(AppointmentStatus.PENDING_APPROVAL);
            appointment.setExpiresAt(LocalDateTime.now().plusMinutes(temporaryHoldMinutes));
        }
        clientRepository.save(client);
        return appointmentRepository.save(appointment);
    }

    @Transactional
    public void approve(UUID appointmentId, UUID establishmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new BusinessException("Agendamento não encontrado."));
        ensureSameEstablishment(appointment, establishmentId);
        if (appointment.getStatus() != AppointmentStatus.PENDING_APPROVAL) {
            throw new BusinessException("Esse agendamento não está pendente.");
        }
        if (appointment.getExpiresAt() != null && appointment.getExpiresAt().isBefore(LocalDateTime.now())) {
            appointment.setStatus(AppointmentStatus.EXPIRED);
            throw new BusinessException("Essa reserva expirou. Peça para o cliente escolher outro horário.");
        }
        if (!isSlotAvailable(appointment.getEstablishment().getId(), appointment.getProfessional().getId(), appointment.getDate(), appointment.getStartTime(), appointment.getEndTime(), appointment.getId())) {
            throw new BusinessException("Conflito detectado. O horário não pode ser aprovado.");
        }
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointment.setConfirmedAt(LocalDateTime.now());
        appointment.setExpiresAt(null);
        appointment.getClient().setPhoneVerified(true);
        if (appointment.getClient().getStatus() == ClientStatus.MANUAL_APPROVAL) {
            appointment.getClient().setStatus(ClientStatus.WATCHLIST);
        }
    }

    @Transactional
    public void cancel(UUID appointmentId, UUID establishmentId, String reason) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new BusinessException("Agendamento não encontrado."));
        ensureSameEstablishment(appointment, establishmentId);
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelledAt(LocalDateTime.now());
        appointment.setCancellationReason(clean(reason, 255));
    }

    @Transactional
    public void complete(UUID appointmentId, UUID establishmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new BusinessException("Agendamento não encontrado."));
        ensureSameEstablishment(appointment, establishmentId);
        appointment.setStatus(AppointmentStatus.COMPLETED);
    }

    @Transactional
    public void markNoShow(UUID appointmentId, UUID establishmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new BusinessException("Agendamento não encontrado."));
        ensureSameEstablishment(appointment, establishmentId);
        appointment.setStatus(AppointmentStatus.NO_SHOW);
        Client client = appointment.getClient();
        client.setNoShowCount(client.getNoShowCount() + 1);
        if (client.getNoShowCount() >= 3) {
            client.setStatus(ClientStatus.BLOCKED);
        } else if (client.getNoShowCount() >= 2) {
            client.setStatus(ClientStatus.MANUAL_APPROVAL);
        } else {
            client.setStatus(ClientStatus.WATCHLIST);
        }
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void expireOldPendingAppointments() {
        appointmentRepository.expireOldPending(LocalDateTime.now(), AppointmentStatus.PENDING_APPROVAL, AppointmentStatus.EXPIRED);
    }

    private Client newClient(Establishment establishment, String name, String phoneOriginal, String phoneNormalized) {
        Client client = new Client();
        client.setEstablishment(establishment);
        client.setName(clean(name, 80));
        client.setPhoneOriginal(clean(phoneOriginal, 40));
        client.setPhoneNormalized(phoneNormalized);
        client.setStatus(ClientStatus.NORMAL);
        return client;
    }

    private BusinessHour findBusinessHour(Establishment establishment, Professional professional, LocalDate date) {
        int day = date.getDayOfWeek().getValue();
        return businessHourRepository.findFirstByEstablishmentIdAndProfessionalIdAndDayOfWeekAndActiveTrue(establishment.getId(), professional.getId(), day)
                .or(() -> businessHourRepository.findFirstByEstablishmentIdAndProfessionalIsNullAndDayOfWeekAndActiveTrue(establishment.getId(), day))
                .orElse(null);
    }

    private void validateInsideBusinessHours(Establishment establishment, Professional professional, LocalDate date, LocalTime start, LocalTime end) {
        if (date.isBefore(LocalDate.now())) {
            throw new BusinessException("Não é possível agendar em data passada.");
        }
        BusinessHour hour = findBusinessHour(establishment, professional, date);
        if (hour == null || start.isBefore(hour.getOpenTime()) || end.isAfter(hour.getCloseTime())) {
            throw new BusinessException("Horário fora do expediente do estabelecimento.");
        }
    }

    private boolean isSlotAvailable(UUID establishmentId, UUID professionalId, LocalDate date, LocalTime start, LocalTime end) {
        return isSlotAvailable(establishmentId, professionalId, date, start, end, null);
    }

    private boolean isSlotAvailable(UUID establishmentId, UUID professionalId, LocalDate date, LocalTime start, LocalTime end, UUID ignoreAppointmentId) {
        if (blockedTimeRepository.existsConflict(establishmentId, professionalId, date, start, end)) {
            return false;
        }
        boolean conflict = ignoreAppointmentId == null
                ? appointmentRepository.existsConflict(professionalId, date, start, end, BLOCKING_STATUSES, LocalDateTime.now())
                : appointmentRepository.existsConflictExcluding(professionalId, ignoreAppointmentId, date, start, end, BLOCKING_STATUSES, LocalDateTime.now());
        return !conflict;
    }

    private void ensureSameEstablishment(Appointment appointment, UUID establishmentId) {
        if (!appointment.getEstablishment().getId().equals(establishmentId)) {
            throw new BusinessException("Acesso negado para este estabelecimento.");
        }
    }

    private String clean(String value, int max) {
        if (value == null) return null;
        String cleaned = value.trim().replaceAll("[<>]", "");
        return cleaned.length() > max ? cleaned.substring(0, max) : cleaned;
    }
}
