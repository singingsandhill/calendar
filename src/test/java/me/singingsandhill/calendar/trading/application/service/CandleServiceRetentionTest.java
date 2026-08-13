package me.singingsandhill.calendar.trading.application.service;

import me.singingsandhill.calendar.trading.domain.candle.CandleRepository;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * 캔들 보관기간이 설정에서 오는지 고정.
 *
 * <p>2026-08-06 이전에는 {@code CandleService.cleanupOldCandles()} 가
 * {@code LocalDateTime.now().minusDays(7)} 을 코드에 박고 있었다. 자정 정리 잡에
 * {@code bot.enabled} 가드도 없어 봇을 꺼둔 기간에도 7일 초과분이 삭제됐고, 그 결과
 * 지표 재계산·백테스트 지평이 영구히 1주로 고정됐다 — 신호 품질 분석에 필요한 과거
 * 구간을 매일 되돌릴 수 없이 버리고 있었다는 뜻이다.
 *
 * <p>여기서 단정하는 것은 "값이 90이다" 가 아니라 <b>값이 설정에서 온다</b>는 사실이다.
 * 임의의 두 보관기간에 대해 컷오프가 그만큼 움직여야 한다 (stock 모듈의
 * {@code StockRiskServiceTimeDecayTest.decayEndpointsComeFromConfigNotConstants} 와 같은 기법).
 */
class CandleServiceRetentionTest {

    /**
     * cleanup 을 1회 실행하고, 리포지토리에 전달된 컷오프 시각을 호출 전후 시각과 함께 돌려준다.
     * 컷오프는 반드시 {@code [before - N일, after - N일]} 구간 안에 있어야 한다.
     */
    private void assertCutoffIsDaysAgo(int retentionDays) {
        CandleRepository repository = mock(CandleRepository.class);
        TradingProperties properties = new TradingProperties();
        properties.getBot().setCandleRetentionDays(retentionDays);
        CandleService service = new CandleService(repository, null, properties);

        LocalDateTime before = LocalDateTime.now();
        service.cleanupOldCandles();
        LocalDateTime after = LocalDateTime.now();

        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repository).deleteByDateTimeBefore(cutoff.capture());

        assertThat(cutoff.getValue())
                .isBetween(before.minusDays(retentionDays), after.minusDays(retentionDays));
    }

    @Test
    void cutoffComesFromConfigNotHardcodedSevenDays() {
        // 하드코딩(7) 상태에서는 두 케이스 모두 실패한다.
        assertCutoffIsDaysAgo(1);
        assertCutoffIsDaysAgo(90);
    }

    @Test
    void javaDefaultMatchesYamlOperationalValue() {
        // P1-9 의 교훈: yaml 키가 누락되면 Java 기본값으로 폴백하므로 둘이 어긋나면 안 된다.
        assertThat(new TradingProperties().getBot().getCandleRetentionDays()).isEqualTo(90);
    }
}
