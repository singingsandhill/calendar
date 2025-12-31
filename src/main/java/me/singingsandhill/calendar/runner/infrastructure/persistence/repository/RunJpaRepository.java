package me.singingsandhill.calendar.runner.infrastructure.persistence.repository;

import me.singingsandhill.calendar.runner.infrastructure.persistence.entity.RunJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RunJpaRepository extends JpaRepository<RunJpaEntity, Long> {

    @Query("SELECT r FROM RunJpaEntity r ORDER BY r.date DESC, r.time DESC")
    List<RunJpaEntity> findAllOrderByDateDescTimeDesc();
}
