package br.com.agendafacil.repository;

import br.com.agendafacil.entity.BusinessHour;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface BusinessHourRepository extends JpaRepository<BusinessHour, UUID> {
    Optional<BusinessHour> findFirstByEstablishmentIdAndProfessionalIdAndDayOfWeekAndActiveTrue(UUID establishmentId, UUID professionalId, int dayOfWeek);
    Optional<BusinessHour> findFirstByEstablishmentIdAndProfessionalIsNullAndDayOfWeekAndActiveTrue(UUID establishmentId, int dayOfWeek);
}
