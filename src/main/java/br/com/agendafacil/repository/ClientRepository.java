package br.com.agendafacil.repository;

import br.com.agendafacil.entity.Client;
import br.com.agendafacil.enums.ClientStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClientRepository extends JpaRepository<Client, UUID> {
    Optional<Client> findByEstablishmentIdAndPhoneNormalized(UUID establishmentId, String phoneNormalized);
    List<Client> findTop20ByEstablishmentIdOrderByCreatedAtDesc(UUID establishmentId);
    long countByEstablishmentIdAndStatus(UUID establishmentId, ClientStatus status);
}
