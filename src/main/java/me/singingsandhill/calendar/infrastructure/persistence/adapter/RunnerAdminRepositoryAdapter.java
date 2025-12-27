package me.singingsandhill.calendar.infrastructure.persistence.adapter;

import me.singingsandhill.calendar.domain.runner.Admin;
import me.singingsandhill.calendar.domain.runner.AdminRepository;
import me.singingsandhill.calendar.infrastructure.persistence.entity.RunnerAdminJpaEntity;
import me.singingsandhill.calendar.infrastructure.persistence.repository.RunnerAdminJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class RunnerAdminRepositoryAdapter implements AdminRepository {

    private final RunnerAdminJpaRepository adminJpaRepository;

    public RunnerAdminRepositoryAdapter(RunnerAdminJpaRepository adminJpaRepository) {
        this.adminJpaRepository = adminJpaRepository;
    }

    @Override
    public Optional<Admin> findByUsername(String username) {
        return adminJpaRepository.findByUsername(username)
                .map(this::toDomain);
    }

    @Override
    public Admin save(Admin admin) {
        RunnerAdminJpaEntity entity = new RunnerAdminJpaEntity(
                admin.getUsername(),
                admin.getPassword(),
                admin.getCreatedAt()
        );

        RunnerAdminJpaEntity saved = adminJpaRepository.save(entity);
        Admin result = toDomain(saved);
        result.setId(saved.getId());
        return result;
    }

    @Override
    public boolean existsByUsername(String username) {
        return adminJpaRepository.existsByUsername(username);
    }

    private Admin toDomain(RunnerAdminJpaEntity entity) {
        return new Admin(
                entity.getId(),
                entity.getUsername(),
                entity.getPassword(),
                entity.getCreatedAt()
        );
    }
}
