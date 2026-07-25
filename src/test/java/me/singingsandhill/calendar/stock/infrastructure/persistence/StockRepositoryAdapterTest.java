package me.singingsandhill.calendar.stock.infrastructure.persistence;

import me.singingsandhill.calendar.stock.domain.stock.Stock;
import me.singingsandhill.calendar.stock.domain.stock.StockState;
import me.singingsandhill.calendar.stock.infrastructure.persistence.adapter.StockRepositoryAdapter;
import me.singingsandhill.calendar.stock.infrastructure.persistence.entity.StockJpaEntity;
import me.singingsandhill.calendar.stock.infrastructure.persistence.repository.StockJpaRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 도메인 ↔ JPA 매핑 라운드트립 회귀 테스트.
 *
 * 사고 이력 (2026-07-24 리뷰 §3-①): toDomain 이 상태머신 작업 필드 5개
 * (highAfterOpen/highFormedAt/pullbackLow/pullbackStartAt/entryPrice)를 복원하지 않아,
 * 5초 틱마다 DB 재로딩되는 트레이딩 루프에서 HIGH_FORMED 이후 전이가 조용히 영구 정체했다
 * (calculateDropFromHigh 가 null 이면 예외 없이 0% 반환).
 */
class StockRepositoryAdapterTest {

    private final StockJpaRepository jpaRepository = mock(StockJpaRepository.class);
    private final StockRepositoryAdapter adapter = new StockRepositoryAdapter(jpaRepository);

    @Test
    void saveRoundTripPreservesStateMachineFields() {
        // 상태머신이 실제로 채우는 순서대로 도메인 객체 구성
        Stock stock = new Stock("005930", "삼성전자", LocalDate.of(2026, 7, 24));
        stock.setOpenPrice(new BigDecimal("70000"));
        stock.setCurrentPrice(new BigDecimal("71500"));
        stock.recordHighFormed(new BigDecimal("71500"));   // HIGH_FORMED + highAfterOpen/highFormedAt
        stock.recordPullbackStart(new BigDecimal("70700")); // PULLBACK + pullbackLow/pullbackStartAt
        stock.markEntryReady();
        stock.markEntered(new BigDecimal("70900"));         // entryPrice

        when(jpaRepository.save(any(StockJpaEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Stock reloaded = adapter.save(stock);

        assertThat(reloaded.getState()).isEqualTo(StockState.ENTERED);
        assertThat(reloaded.getHighAfterOpen()).isEqualByComparingTo("71500");
        assertThat(reloaded.getHighFormedAt()).isEqualTo(stock.getHighFormedAt());
        assertThat(reloaded.getPullbackLow()).isEqualByComparingTo("70700");
        assertThat(reloaded.getPullbackStartAt()).isEqualTo(stock.getPullbackStartAt());
        assertThat(reloaded.getEntryPrice()).isEqualByComparingTo("70900");
    }
}
