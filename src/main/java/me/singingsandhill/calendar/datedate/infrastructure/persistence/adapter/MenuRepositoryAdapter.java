package me.singingsandhill.calendar.datedate.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import me.singingsandhill.calendar.datedate.domain.menu.Menu;
import me.singingsandhill.calendar.datedate.domain.menu.MenuRepository;
import me.singingsandhill.calendar.datedate.infrastructure.persistence.entity.MenuJpaEntity;
import me.singingsandhill.calendar.datedate.infrastructure.persistence.entity.MenuVoteJpaEntity;
import me.singingsandhill.calendar.datedate.infrastructure.persistence.entity.ScheduleJpaEntity;
import me.singingsandhill.calendar.datedate.infrastructure.persistence.repository.MenuJpaRepository;
import me.singingsandhill.calendar.datedate.infrastructure.persistence.repository.ScheduleJpaRepository;

@Repository
public class MenuRepositoryAdapter implements MenuRepository {

    private final MenuJpaRepository menuJpaRepository;
    private final ScheduleJpaRepository scheduleJpaRepository;

    public MenuRepositoryAdapter(MenuJpaRepository menuJpaRepository,
                                  ScheduleJpaRepository scheduleJpaRepository) {
        this.menuJpaRepository = menuJpaRepository;
        this.scheduleJpaRepository = scheduleJpaRepository;
    }

    @Override
    public Optional<Menu> findById(Long id) {
        return menuJpaRepository.findById(id)
                .map(this::toDomain);
    }

    @Override
    public List<Menu> findAllByScheduleId(Long scheduleId) {
        return menuJpaRepository.findAllByScheduleId(scheduleId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Menu save(Menu menu) {
        MenuJpaEntity entity;

        if (menu.getId() != null) {
            entity = menuJpaRepository.findById(menu.getId())
                    .orElseThrow(() -> new IllegalStateException("Menu not found: " + menu.getId()));

            entity.getVotes().clear();
            for (String voter : menu.getVoters()) {
                entity.addVote(new MenuVoteJpaEntity(entity, voter));
            }
        } else {
            ScheduleJpaEntity schedule = scheduleJpaRepository.findById(menu.getScheduleId())
                    .orElseThrow(() -> new IllegalStateException("Schedule not found: " + menu.getScheduleId()));

            entity = new MenuJpaEntity(schedule, menu.getName(), menu.getUrl(), menu.getCreatedAt());

            for (String voter : menu.getVoters()) {
                entity.addVote(new MenuVoteJpaEntity(entity, voter));
            }
        }

        MenuJpaEntity saved = menuJpaRepository.save(entity);
        Menu result = toDomain(saved);
        result.setId(saved.getId());
        return result;
    }

    @Override
    public void delete(Menu menu) {
        menuJpaRepository.deleteById(menu.getId());
    }

    @Override
    public boolean existsByScheduleIdAndName(Long scheduleId, String name) {
        return menuJpaRepository.existsByScheduleIdAndName(scheduleId, name);
    }

    @Override
    public List<Menu> findAllOrderByPopularity() {
        return menuJpaRepository.findAllOrderByPopularity().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private Menu toDomain(MenuJpaEntity entity) {
        List<String> voters = entity.getVotes().stream()
                .map(MenuVoteJpaEntity::getVoterName)
                .collect(Collectors.toList());

        return new Menu(
                entity.getId(),
                entity.getScheduleId(),
                entity.getName(),
                entity.getUrl(),
                voters,
                entity.getCreatedAt()
        );
    }
}
