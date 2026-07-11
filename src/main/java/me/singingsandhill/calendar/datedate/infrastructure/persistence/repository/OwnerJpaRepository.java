package me.singingsandhill.calendar.datedate.infrastructure.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import me.singingsandhill.calendar.datedate.infrastructure.persistence.entity.OwnerJpaEntity;

public interface OwnerJpaRepository extends JpaRepository<OwnerJpaEntity, String> {

    List<OwnerJpaEntity> findAllByUserId(Long userId);
}
