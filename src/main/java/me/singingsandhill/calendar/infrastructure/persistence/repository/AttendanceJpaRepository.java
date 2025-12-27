package me.singingsandhill.calendar.infrastructure.persistence.repository;

import me.singingsandhill.calendar.infrastructure.persistence.entity.AttendanceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AttendanceJpaRepository extends JpaRepository<AttendanceJpaEntity, Long> {

    List<AttendanceJpaEntity> findByRun_Id(Long runId);

    @Query("SELECT COUNT(a) > 0 FROM AttendanceJpaEntity a " +
           "WHERE a.run.id = :runId AND LOWER(a.participantName) = LOWER(:participantName)")
    boolean existsByRunIdAndParticipantNameIgnoreCase(
            @Param("runId") Long runId,
            @Param("participantName") String participantName);

    @Query("SELECT a.participantName, COUNT(a) as cnt " +
           "FROM AttendanceJpaEntity a " +
           "GROUP BY a.participantName " +
           "ORDER BY cnt DESC")
    List<Object[]> findAllGroupedByParticipantOrderByCount();

    @Query("SELECT a.participantName, SUM(a.distance) as total " +
           "FROM AttendanceJpaEntity a " +
           "GROUP BY a.participantName " +
           "ORDER BY total DESC")
    List<Object[]> findAllGroupedByParticipantOrderByDistance();

    @Query("SELECT a.participantName, " +
           "SUM(CASE WHEN a.run.category = me.singingsandhill.calendar.infrastructure.persistence.entity.RunCategoryJpa.REGULAR THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN a.run.category = me.singingsandhill.calendar.infrastructure.persistence.entity.RunCategoryJpa.LIGHTNING THEN 1 ELSE 0 END), " +
           "COUNT(a) " +
           "FROM AttendanceJpaEntity a " +
           "GROUP BY a.participantName " +
           "ORDER BY COUNT(a) DESC")
    List<Object[]> findAllMemberAttendanceStats();

    @Query("SELECT a FROM AttendanceJpaEntity a " +
           "JOIN FETCH a.run " +
           "WHERE a.participantName = :name " +
           "ORDER BY a.run.date DESC, a.run.time DESC")
    List<AttendanceJpaEntity> findByParticipantNameWithRun(@Param("name") String name);
}
