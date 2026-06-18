package br.com.agendafacil.repository;

import br.com.agendafacil.entity.Professional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProfessionalRepository extends JpaRepository<Professional, UUID> {
    @EntityGraph(attributePaths = "services")
    List<Professional> findByEstablishmentIdAndActiveTrueOrderByName(UUID establishmentId);
    Optional<Professional> findByIdAndEstablishmentId(UUID id, UUID establishmentId);

    @Query("select distinct p from Professional p join p.services s where p.establishment.id = :establishmentId and p.active = true and s.id = :serviceId and s.active = true order by p.name")
    List<Professional> findActiveByService(@Param("establishmentId") UUID establishmentId, @Param("serviceId") UUID serviceId);
}
