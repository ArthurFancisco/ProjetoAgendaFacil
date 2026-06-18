package br.com.agendafacil.repository;

import br.com.agendafacil.entity.ScheduleAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.UUID;

public interface ScheduleAttemptRepository extends JpaRepository<ScheduleAttempt, UUID> {
    long countByEstablishmentIdAndPhoneNormalizedAndCreatedAtAfter(UUID establishmentId, String phoneNormalized, LocalDateTime after);
    long countByEstablishmentIdAndIpHashAndCreatedAtAfter(UUID establishmentId, String ipHash, LocalDateTime after);
}
