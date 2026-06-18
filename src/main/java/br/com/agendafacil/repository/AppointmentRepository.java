package br.com.agendafacil.repository;

import br.com.agendafacil.entity.Appointment;
import br.com.agendafacil.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
    Optional<Appointment> findByCancelToken(String cancelToken);

    @EntityGraph(attributePaths = {"client", "professional", "service", "establishment"})
    List<Appointment> findByEstablishmentIdAndDateOrderByStartTimeAsc(UUID establishmentId, LocalDate date);

    @EntityGraph(attributePaths = {"client", "professional", "service", "establishment"})
    List<Appointment> findTop20ByEstablishmentIdAndStatusOrderByCreatedAtAsc(UUID establishmentId, AppointmentStatus status);

    @EntityGraph(attributePaths = {"client", "professional", "service", "establishment"})
    @Query("select a from Appointment a where a.establishment.id = :establishmentId and a.date between :start and :end order by a.date asc, a.startTime asc")
    List<Appointment> findPeriod(@Param("establishmentId") UUID establishmentId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    @EntityGraph(attributePaths = {"client", "professional", "service", "establishment"})
    List<Appointment> findByEstablishmentIdAndDateAndStatusInOrderByStartTimeAsc(UUID establishmentId, LocalDate date, Collection<AppointmentStatus> statuses);

    @EntityGraph(attributePaths = {"client", "professional", "service", "establishment"})
    @Query("select a from Appointment a where a.establishment.id = :establishmentId and a.date between :start and :end and a.status in :statuses order by a.date asc, a.startTime asc")
    List<Appointment> findPeriodByStatuses(@Param("establishmentId") UUID establishmentId,
                                           @Param("start") LocalDate start,
                                           @Param("end") LocalDate end,
                                           @Param("statuses") Collection<AppointmentStatus> statuses);

    @EntityGraph(attributePaths = {"client", "professional", "service", "establishment"})
    @Query("select a from Appointment a where a.establishment.id = :establishmentId and a.status in :statuses order by a.date desc, a.startTime desc")
    List<Appointment> findTop20History(@Param("establishmentId") UUID establishmentId,
                                       @Param("statuses") Collection<AppointmentStatus> statuses,
                                       org.springframework.data.domain.Pageable pageable);


    @Query("select count(a) from Appointment a where a.establishment.id = :establishmentId and a.client.phoneNormalized = :phone and a.date >= :today and a.status in :statuses")
    long countFutureByPhone(@Param("establishmentId") UUID establishmentId,
                            @Param("phone") String phoneNormalized,
                            @Param("today") LocalDate today,
                            @Param("statuses") Collection<AppointmentStatus> statuses);

    @Query("select count(a) > 0 from Appointment a " +
            "where a.professional.id = :professionalId " +
            "and a.date = :date " +
            "and a.status in :statuses " +
            "and (a.expiresAt is null or a.expiresAt > :now) " +
            "and :startTime < a.endTime and :endTime > a.startTime")
    boolean existsConflict(@Param("professionalId") UUID professionalId,
                           @Param("date") LocalDate date,
                           @Param("startTime") LocalTime startTime,
                           @Param("endTime") LocalTime endTime,
                           @Param("statuses") Collection<AppointmentStatus> statuses,
                           @Param("now") LocalDateTime now);

    @Query("select count(a) > 0 from Appointment a " +
            "where a.professional.id = :professionalId " +
            "and a.id <> :ignoreId " +
            "and a.date = :date " +
            "and a.status in :statuses " +
            "and (a.expiresAt is null or a.expiresAt > :now) " +
            "and :startTime < a.endTime and :endTime > a.startTime")
    boolean existsConflictExcluding(@Param("professionalId") UUID professionalId,
                                    @Param("ignoreId") UUID ignoreId,
                                    @Param("date") LocalDate date,
                                    @Param("startTime") LocalTime startTime,
                                    @Param("endTime") LocalTime endTime,
                                    @Param("statuses") Collection<AppointmentStatus> statuses,
                                    @Param("now") LocalDateTime now);

    @Modifying
    @Query("update Appointment a set a.status = :expired, a.updatedAt = :now " +
            "where a.status = :pending " +
            "and a.expiresAt is not null and a.expiresAt < :now")
    int expireOldPending(@Param("now") LocalDateTime now,
                         @Param("pending") AppointmentStatus pending,
                         @Param("expired") AppointmentStatus expired);
}
