package me.singingsandhill.calendar.runner.infrastructure.persistence.adapter;

import me.singingsandhill.calendar.runner.domain.Run;
import me.singingsandhill.calendar.runner.domain.RunCategory;
import me.singingsandhill.calendar.runner.domain.RunRepository;
import me.singingsandhill.calendar.runner.infrastructure.persistence.entity.RunCategoryJpa;
import me.singingsandhill.calendar.runner.infrastructure.persistence.entity.RunJpaEntity;
import me.singingsandhill.calendar.runner.infrastructure.persistence.repository.RunJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class RunRepositoryAdapter implements RunRepository {

    private final RunJpaRepository runJpaRepository;

    public RunRepositoryAdapter(RunJpaRepository runJpaRepository) {
        this.runJpaRepository = runJpaRepository;
    }

    @Override
    public Optional<Run> findById(Long id) {
        return runJpaRepository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public List<Run> findAll() {
        return runJpaRepository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Run> findAllOrderByDateDesc() {
        return runJpaRepository.findAllOrderByDateDescTimeDesc().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Run save(Run run) {
        RunJpaEntity entity;

        if (run.getId() != null) {
            entity = runJpaRepository.findById(run.getId())
                    .orElseThrow(() -> new IllegalStateException("Run not found: " + run.getId()));
            entity.setDate(run.getDate());
            entity.setTime(run.getTime());
            entity.setLocation(run.getLocation());
            entity.setCategory(RunCategoryJpa.valueOf(run.getCategory().name()));
        } else {
            entity = new RunJpaEntity(
                    run.getDate(),
                    run.getTime(),
                    run.getLocation(),
                    RunCategoryJpa.valueOf(run.getCategory().name()),
                    run.getCreatedAt()
            );
        }

        RunJpaEntity saved = runJpaRepository.save(entity);
        Run result = toDomain(saved);
        result.setId(saved.getId());
        return result;
    }

    @Override
    public void deleteById(Long id) {
        runJpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return runJpaRepository.existsById(id);
    }

    private Run toDomain(RunJpaEntity entity) {
        return new Run(
                entity.getId(),
                entity.getDate(),
                entity.getTime(),
                entity.getLocation(),
                RunCategory.valueOf(entity.getCategory().name()),
                entity.getCreatedAt()
        );
    }
}
