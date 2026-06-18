package br.com.agendafacil.repository;

import br.com.agendafacil.entity.AppUser;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<AppUser, UUID> {
    @EntityGraph(attributePaths = "establishment")
    Optional<AppUser> findByEmail(String email);

    @EntityGraph(attributePaths = "establishment")
    @Query("select u from AppUser u where u.id = :id")
    Optional<AppUser> findWithEstablishmentById(@Param("id") UUID id);

    boolean existsByEmail(String email);
}
