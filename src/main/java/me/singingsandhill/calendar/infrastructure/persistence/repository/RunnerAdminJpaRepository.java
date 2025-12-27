package me.singingsandhill.calendar.infrastructure.persistence.repository;

import me.singingsandhill.calendar.infrastructure.persistence.entity.RunnerAdminJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RunnerAdminJpaRepository extends JpaRepository<RunnerAdminJpaEntity, Long> {

    Optional<RunnerAdminJpaEntity> findByUsername(String username);

    boolean existsByUsername(String username);
}
