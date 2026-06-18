package br.com.agendafacil.repository;

import br.com.agendafacil.entity.BlockedTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface BlockedTimeRepository extends JpaRepository<BlockedTime, UUID> {
    @EntityGraph(attributePaths = {"professional"})
    List<BlockedTime> findTop20ByEstablishmentIdOrderByDateDescStartTimeAsc(UUID establishmentId);

    @Query("select count(b) > 0 from BlockedTime b " +
            "where b.establishment.id = :establishmentId " +
            "and b.date = :date " +
            "and (b.professional is null or b.professional.id = :professionalId) " +
            "and :startTime < b.endTime and :endTime > b.startTime")
    boolean existsConflict(@Param("establishmentId") UUID establishmentId,
                           @Param("professionalId") UUID professionalId,
                           @Param("date") LocalDate date,
                           @Param("startTime") LocalTime startTime,
                           @Param("endTime") LocalTime endTime);
}
