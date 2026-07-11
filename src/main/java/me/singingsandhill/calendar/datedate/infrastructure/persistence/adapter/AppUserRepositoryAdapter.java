package me.singingsandhill.calendar.datedate.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import me.singingsandhill.calendar.datedate.domain.user.AppUser;
import me.singingsandhill.calendar.datedate.domain.user.AppUserRepository;
import me.singingsandhill.calendar.datedate.infrastructure.persistence.entity.AppUserJpaEntity;
import me.singingsandhill.calendar.datedate.infrastructure.persistence.repository.AppUserJpaRepository;

@Repository
public class AppUserRepositoryAdapter implements AppUserRepository {

    private final AppUserJpaRepository jpaRepository;

    public AppUserRepositoryAdapter(AppUserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<AppUser> findByKakaoId(Long kakaoId) {
        return jpaRepository.findByKakaoId(kakaoId).map(this::toDomain);
    }

    @Override
    public Optional<AppUser> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public AppUser save(AppUser user) {
        AppUserJpaEntity saved = jpaRepository.save(toEntity(user));
        return toDomain(saved);
    }

    private AppUser toDomain(AppUserJpaEntity entity) {
        return new AppUser(
                entity.getId(),
                entity.getKakaoId(),
                entity.getNickname(),
                entity.getProfileImageUrl(),
                entity.getCreatedAt(),
                entity.getLastLoginAt()
        );
    }

    private AppUserJpaEntity toEntity(AppUser user) {
        return new AppUserJpaEntity(
                user.getId(),
                user.getKakaoId(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getCreatedAt(),
                user.getLastLoginAt()
        );
    }
}
