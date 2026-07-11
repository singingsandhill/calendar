package me.singingsandhill.calendar.datedate.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import me.singingsandhill.calendar.datedate.domain.recap.RecapShare;
import me.singingsandhill.calendar.datedate.domain.recap.RecapShareRepository;
import me.singingsandhill.calendar.datedate.infrastructure.persistence.entity.RecapShareJpaEntity;
import me.singingsandhill.calendar.datedate.infrastructure.persistence.repository.RecapShareJpaRepository;

@Repository
public class RecapShareRepositoryAdapter implements RecapShareRepository {

    private final RecapShareJpaRepository jpaRepository;

    public RecapShareRepositoryAdapter(RecapShareJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<RecapShare> findByToken(String token) {
        return jpaRepository.findByToken(token).map(this::toDomain);
    }

    @Override
    public Optional<RecapShare> findByUserIdAndYear(Long userId, int year) {
        return jpaRepository.findByUserIdAndYear(userId, year).map(this::toDomain);
    }

    @Override
    public RecapShare save(RecapShare share) {
        RecapShareJpaEntity saved = jpaRepository.save(new RecapShareJpaEntity(
                share.getId(), share.getUserId(), share.getYear(), share.getToken(), share.getCreatedAt()));
        return toDomain(saved);
    }

    private RecapShare toDomain(RecapShareJpaEntity entity) {
        return new RecapShare(entity.getId(), entity.getUserId(), entity.getYear(),
                entity.getToken(), entity.getCreatedAt());
    }
}
