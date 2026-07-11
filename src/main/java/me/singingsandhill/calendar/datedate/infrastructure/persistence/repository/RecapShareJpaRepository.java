package me.singingsandhill.calendar.datedate.infrastructure.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import me.singingsandhill.calendar.datedate.infrastructure.persistence.entity.RecapShareJpaEntity;

public interface RecapShareJpaRepository extends JpaRepository<RecapShareJpaEntity, Long> {

    Optional<RecapShareJpaEntity> findByToken(String token);

    Optional<RecapShareJpaEntity> findByUserIdAndYear(Long userId, int year);
}
