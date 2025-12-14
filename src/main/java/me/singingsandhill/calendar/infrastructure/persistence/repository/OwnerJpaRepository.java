package me.singingsandhill.calendar.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import me.singingsandhill.calendar.infrastructure.persistence.entity.OwnerJpaEntity;

public interface OwnerJpaRepository extends JpaRepository<OwnerJpaEntity, String> {
}
