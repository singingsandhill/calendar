package me.singingsandhill.calendar.runner.infrastructure.persistence.repository;

import me.singingsandhill.calendar.runner.infrastructure.persistence.entity.RunJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RunJpaRepository extends JpaRepository<RunJpaEntity, Long> {

    @Query("SELECT r FROM RunJpaEntity r ORDER BY r.date DESC, r.time DESC")
    List<RunJpaEntity> findAllOrderByDateDescTimeDesc();

    @Query("SELECT r FROM RunJpaEntity r WHERE r.date BETWEEN :from AND :to ORDER BY r.date DESC, r.time DESC")
    List<RunJpaEntity> findByDateBetweenOrderByDateDescTimeDesc(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
