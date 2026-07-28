package me.singingsandhill.calendar.stock.application.service;

import me.singingsandhill.calendar.stock.application.observability.StockBotMetrics;
import me.singingsandhill.calendar.stock.domain.signal.StockSignalRepository;
import me.singingsandhill.calendar.stock.domain.stock.Stock;
import me.singingsandhill.calendar.stock.domain.stock.StockRepository;
import me.singingsandhill.calendar.stock.infrastructure.api.KoreaInvestmentApiClient;
import me.singingsandhill.calendar.stock.infrastructure.api.dto.KisQuoteResponse;
import me.singingsandhill.calendar.stock.infrastructure.config.StockProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Floor 3(체결강도) 데이터 소스 회귀 테스트 (2026-07-27 스크리닝 전멸,
 * ADR stock/infrastructure/0007).
 *
 * 시세 응답(inquire-price)에는 체결강도 필드가 없으므로, 스크리닝은
 * {@link KoreaInvestmentApiClient#getTradeStrength} (inquire-ccnl 의 tday_rltv)로
 * 체결강도를 얻어야 한다. 이 소스가 끊기면 갭 통과 종목 전량이
 * dataInsufficient 로 탈락한다(2026-07-27 로그: 30 = gap 11 + dataInsufficient 19).
 */
class ScreeningFloorStrengthTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 27);

    /** 실스펙 반영: 체결강도 필드가 없는 시세 응답 (갭 4%, 시총 1000억, 거래대금 100억). */
    private static KisQuoteResponse quoteWithoutStrengthFields() {
        return new KisQuoteResponse("005930",
            new BigDecimal("10400"), new BigDecimal("10400"), new BigDecimal("10500"),
            new BigDecimal("10300"), new BigDecimal("10000"), new BigDecimal("400"),
            new BigDecimal("4.0"), 1_000_000L, new BigDecimal("10000000000"),
            new BigDecimal("100000000000"), new BigDecimal("1.0"));
    }

    @Test
    void screeningUsesCcnlTradeStrength_notAbsentQuoteFields() {
        KoreaInvestmentApiClient api = mock(KoreaInvestmentApiClient.class);
        when(api.getQuote(anyString())).thenReturn(quoteWithoutStrengthFields());
        when(api.getTradeStrength(anyString())).thenReturn(new BigDecimal("142.35"));
        when(api.getOrderbook(anyString())).thenReturn(null);

        ScreeningService service = new ScreeningService(
            mock(StockRepository.class), mock(StockSignalRepository.class),
            api, new StockProperties(), mock(StockBotMetrics.class));

        List<Stock> selected = service.executeScreening(TODAY, List.of("005930"));

        // 갭 4% + 체결강도 142.35 — 시세 필드가 아니라 ccnl 소스로 Floor 3 을 통과해야 한다
        assertThat(selected).hasSize(1);
        assertThat(selected.get(0).getTradeStrength()).isEqualByComparingTo(new BigDecimal("142.35"));
    }

    @Test
    void gapFailedStock_doesNotFetchTradeStrength() {
        KoreaInvestmentApiClient api = mock(KoreaInvestmentApiClient.class);
        // 갭 0% (시가 == 전일종가) → Floor 2 탈락
        when(api.getQuote(anyString())).thenReturn(new KisQuoteResponse("005930",
            new BigDecimal("10000"), new BigDecimal("10000"), new BigDecimal("10100"),
            new BigDecimal("9900"), new BigDecimal("10000"), BigDecimal.ZERO,
            BigDecimal.ZERO, 1_000_000L, new BigDecimal("10000000000"),
            new BigDecimal("100000000000"), new BigDecimal("1.0")));

        ScreeningService service = new ScreeningService(
            mock(StockRepository.class), mock(StockSignalRepository.class),
            api, new StockProperties(), mock(StockBotMetrics.class));

        assertThat(service.executeScreening(TODAY, List.of("005930"))).isEmpty();
        // 콜 예산 규약(ADR 0007): 체결강도 조회는 갭 통과 종목만
        org.mockito.Mockito.verify(api, org.mockito.Mockito.never()).getTradeStrength(anyString());
    }

    @Test
    void skipZeroStrengthDisabled_correctsNullToFloorStrength() {
        KoreaInvestmentApiClient api = mock(KoreaInvestmentApiClient.class);
        when(api.getQuote(anyString())).thenReturn(quoteWithoutStrengthFields());
        when(api.getTradeStrength(anyString())).thenReturn(null);
        when(api.getOrderbook(anyString())).thenReturn(null);

        StockProperties props = new StockProperties();
        props.getScreening().setSkipZeroStrength(false);
        ScreeningService service = new ScreeningService(
            mock(StockRepository.class), mock(StockSignalRepository.class),
            api, props, mock(StockBotMetrics.class));

        List<Stock> selected = service.executeScreening(TODAY, List.of("005930"));

        // skipZeroStrength=false: 미집계(null→0)는 floorTradeStrength(95) 로 보정되어 통과
        // (강도점수는 0 — 갭 점수만으로 선정 관문을 넘는 케이스)
        assertThat(selected).hasSize(1);
        assertThat(selected.get(0).getTradeStrength())
            .isEqualByComparingTo(props.getScreening().getFloorTradeStrength());
    }

    @Test
    void screeningSkipsAsDataInsufficient_whenCcnlStrengthUnavailable() {
        KoreaInvestmentApiClient api = mock(KoreaInvestmentApiClient.class);
        when(api.getQuote(anyString())).thenReturn(quoteWithoutStrengthFields());
        when(api.getTradeStrength(anyString())).thenReturn(null);
        when(api.getOrderbook(anyString())).thenReturn(null);

        ScreeningService service = new ScreeningService(
            mock(StockRepository.class), mock(StockSignalRepository.class),
            api, new StockProperties(), mock(StockBotMetrics.class));

        // 체결강도 미확보(null)는 기존 skipZeroStrength 경로로 안전 탈락 — 선정 없음
        assertThat(service.executeScreening(TODAY, List.of("005930"))).isEmpty();
    }
}
