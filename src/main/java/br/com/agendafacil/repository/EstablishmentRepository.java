package br.com.agendafacil.repository;

import br.com.agendafacil.entity.Establishment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface EstablishmentRepository extends JpaRepository<Establishment, UUID> {
    Optional<Establishment> findBySlugAndActiveTrue(String slug);
    boolean existsBySlug(String slug);
}
