package me.singingsandhill.calendar.runner.domain;

import java.util.List;
import java.util.Optional;

public interface RunRepository {

    Optional<Run> findById(Long id);

    List<Run> findAll();

    List<Run> findAllOrderByDateDesc();

    Run save(Run run);

    void deleteById(Long id);

    boolean existsById(Long id);
}
