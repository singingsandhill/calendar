package me.singingsandhill.calendar.datedate.domain.recap;

import java.util.Optional;

public interface RecapShareRepository {

    Optional<RecapShare> findByToken(String token);

    Optional<RecapShare> findByUserIdAndYear(Long userId, int year);

    RecapShare save(RecapShare share);
}
