package br.com.agendafacil.service;

import br.com.agendafacil.entity.Establishment;
import br.com.agendafacil.entity.ScheduleAttempt;
import br.com.agendafacil.enums.AppointmentStatus;
import br.com.agendafacil.exception.BusinessException;
import br.com.agendafacil.repository.AppointmentRepository;
import br.com.agendafacil.repository.ScheduleAttemptRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingGuardService {
    private final ScheduleAttemptRepository scheduleAttemptRepository;
    private final AppointmentRepository appointmentRepository;
    private final RequestInfoService requestInfoService;
    private final HashService hashService;
    private final int maxAttemptsPhoneHour;
    private final int maxAttemptsIpHour;
    private final int maxFutureAppointmentsPhone;

    public BookingGuardService(ScheduleAttemptRepository scheduleAttemptRepository,
                               AppointmentRepository appointmentRepository,
                               RequestInfoService requestInfoService,
                               HashService hashService,
                               @Value("${app.booking.max-attempts-phone-hour}") int maxAttemptsPhoneHour,
                               @Value("${app.booking.max-attempts-ip-hour}") int maxAttemptsIpHour,
                               @Value("${app.booking.max-future-appointments-phone}") int maxFutureAppointmentsPhone) {
        this.scheduleAttemptRepository = scheduleAttemptRepository;
        this.appointmentRepository = appointmentRepository;
        this.requestInfoService = requestInfoService;
        this.hashService = hashService;
        this.maxAttemptsPhoneHour = maxAttemptsPhoneHour;
        this.maxAttemptsIpHour = maxAttemptsIpHour;
        this.maxFutureAppointmentsPhone = maxFutureAppointmentsPhone;
    }

    public void validatePublicAttempt(Establishment establishment, String phoneNormalized, String honeypot, HttpServletRequest request) {
        String ipHash = hashService.hash(requestInfoService.clientIp(request));
        String userAgentHash = hashService.hash(requestInfoService.userAgent(request));
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);

        try {
            if (honeypot != null && !honeypot.isBlank()) {
                throw new BusinessException("Não foi possível continuar agora. Tente novamente mais tarde.");
            }

            long phoneAttempts = scheduleAttemptRepository.countByEstablishmentIdAndPhoneNormalizedAndCreatedAtAfter(establishment.getId(), phoneNormalized, oneHourAgo);
            if (phoneAttempts >= maxAttemptsPhoneHour) {
                throw new BusinessException("Muitas tentativas para este telefone. Fale com o estabelecimento para confirmar seu horário.");
            }

            long ipAttempts = scheduleAttemptRepository.countByEstablishmentIdAndIpHashAndCreatedAtAfter(establishment.getId(), ipHash, oneHourAgo);
            if (ipAttempts >= maxAttemptsIpHour) {
                throw new BusinessException("Muitas tentativas recentes. Aguarde alguns minutos ou fale com o estabelecimento.");
            }

            long futureAppointments = appointmentRepository.countFutureByPhone(
                    establishment.getId(),
                    phoneNormalized,
                    LocalDate.now(),
                    List.of(AppointmentStatus.CONFIRMED, AppointmentStatus.PENDING_APPROVAL)
            );
            if (futureAppointments >= maxFutureAppointmentsPhone) {
                throw new BusinessException("Esse WhatsApp já possui horários em aberto. Para marcar outro, fale com o estabelecimento.");
            }

            record(establishment, phoneNormalized, ipHash, userAgentHash, "ALLOWED", null);
        } catch (BusinessException ex) {
            record(establishment, phoneNormalized, ipHash, userAgentHash, "BLOCKED", ex.getMessage());
            throw ex;
        }
    }

    private void record(Establishment establishment, String phoneNormalized, String ipHash, String userAgentHash, String result, String reason) {
        ScheduleAttempt attempt = new ScheduleAttempt();
        attempt.setEstablishmentId(establishment.getId());
        attempt.setPhoneNormalized(phoneNormalized);
        attempt.setIpHash(ipHash);
        attempt.setUserAgentHash(userAgentHash);
        attempt.setResult(result);
        attempt.setReason(reason);
        scheduleAttemptRepository.save(attempt);
    }
}
