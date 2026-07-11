package me.singingsandhill.calendar.trading.infrastructure.persistence.repository;

import me.singingsandhill.calendar.trading.infrastructure.persistence.entity.TradingEventJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface TradingEventJpaRepository extends JpaRepository<TradingEventJpaEntity, Long> {

    @Query("SELECT e FROM TradingEventJpaEntity e ORDER BY e.createdAt DESC, e.id DESC")
    List<TradingEventJpaEntity> findRecent(Pageable pageable);

    @Query("SELECT e FROM TradingEventJpaEntity e WHERE e.level IN :levels ORDER BY e.createdAt DESC, e.id DESC")
    List<TradingEventJpaEntity> findRecentByLevelIn(@Param("levels") Collection<String> levels, Pageable pageable);
}
