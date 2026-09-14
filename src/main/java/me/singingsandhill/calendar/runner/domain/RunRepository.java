package me.singingsandhill.calendar.runner.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RunRepository {

    Optional<Run> findById(Long id);

    List<Run> findAll();

    List<Run> findAllOrderByDateDesc();

    /**
     * 날짜 범위(경계 포함) 내 런 조회. 경계는 non-null — null 무제한 처리는 서비스 레이어 책임.
     */
    List<Run> findByDateBetweenOrderByDateDesc(LocalDate from, LocalDate to);

    Run save(Run run);

    void deleteById(Long id);

    boolean existsById(Long id);
}
