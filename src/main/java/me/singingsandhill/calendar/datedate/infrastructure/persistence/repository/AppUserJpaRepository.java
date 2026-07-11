package me.singingsandhill.calendar.datedate.infrastructure.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import me.singingsandhill.calendar.datedate.infrastructure.persistence.entity.AppUserJpaEntity;

public interface AppUserJpaRepository extends JpaRepository<AppUserJpaEntity, Long> {

    Optional<AppUserJpaEntity> findByKakaoId(Long kakaoId);
}
