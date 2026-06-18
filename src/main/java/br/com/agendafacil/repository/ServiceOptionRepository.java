package br.com.agendafacil.repository;

import br.com.agendafacil.entity.ServiceOption;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceOptionRepository extends JpaRepository<ServiceOption, UUID> {
    List<ServiceOption> findByEstablishmentIdAndActiveTrueOrderByName(UUID establishmentId);
    Optional<ServiceOption> findByIdAndEstablishmentIdAndActiveTrue(UUID id, UUID establishmentId);
    Optional<ServiceOption> findByIdAndEstablishmentId(UUID id, UUID establishmentId);
}
