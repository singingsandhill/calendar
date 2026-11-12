package me.singingsandhill.calendar.stock.application.service;

import me.singingsandhill.calendar.stock.application.observability.StockBotMetrics;
import me.singingsandhill.calendar.stock.domain.signal.StockSignalRepository;
import me.singingsandhill.calendar.stock.domain.stock.Stock;
import me.singingsandhill.calendar.stock.domain.stock.StockProductType;
import me.singingsandhill.calendar.stock.domain.stock.StockRepository;
import me.singingsandhill.calendar.stock.infrastructure.api.KoreaInvestmentApiClient;
import me.singingsandhill.calendar.stock.infrastructure.api.dto.KisQuoteResponse;
import me.singingsandhill.calendar.stock.infrastructure.api.dto.KisStockInfo;
import me.singingsandhill.calendar.stock.infrastructure.config.StockProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 종목명 회귀 테스트 (2026-08-31 09:20 메일 종목명 칸에 종목코드 인쇄,
 * ADR stock/infrastructure/0008).
 *
 * 스크리닝이 쓰는 시세·체결·호가 TR 에는 종목명 필드가 없어 {@code Stock} 이
 * {@code new Stock(code, code, date)} 로 만들어지고 있었다(최초 커밋의 placeholder).
 * 기대: Floor 통과 종목만 {@link KoreaInvestmentApiClient#getStockInfo} 로 조회해
 * {@code stockName} 을 채우고, 조회 실패(null)는 종목코드로 대체(fail-soft, 선정 무영향).
 * 같은 응답의 상품 유형(STOCK/ETF/ETN)도 싣고, {@code universe.excluded-product-types}(기본 빈 목록)에 든
 * 유형은 여기서 뺀다 (ADR stock/algorithm/0011).
 */
class ScreeningStockNameTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 31);
    private static final BigDecimal MARKET_CAP_1000억 = new BigDecimal("100000000000");

    /** 갭 4%, 거래대금 100억 — 기본 StockProperties 로 score 경로 Floor·선정 관문을 통과하는 조합. */
    private static KisQuoteResponse passingQuote(BigDecimal marketCap) {
        return new KisQuoteResponse("005930",
            new BigDecimal("10400"), new BigDecimal("10400"), new BigDecimal("10500"),
            new BigDecimal("10300"), new BigDecimal("10000"), new BigDecimal("400"),
            new BigDecimal("4.0"), 1_000_000L, new BigDecimal("10000000000"),
            marketCap, new BigDecimal("1.0"));
    }

    private static KoreaInvestmentApiClient apiReturningName(String name, BigDecimal marketCap) {
        return apiReturning(name == null ? null : new KisStockInfo(name, StockProductType.STOCK), marketCap);
    }

    private static KoreaInvestmentApiClient apiReturning(KisStockInfo info, BigDecimal marketCap) {
        KoreaInvestmentApiClient api = mock(KoreaInvestmentApiClient.class);
        when(api.getQuote(anyString())).thenReturn(passingQuote(marketCap));
        when(api.getTradeStrength(anyString())).thenReturn(new BigDecimal("142.35"));
        when(api.getOrderbook(anyString())).thenReturn(null);
        when(api.getStockInfo(anyString())).thenReturn(info);
        return api;
    }

    private static ScreeningService service(KoreaInvestmentApiClient api, StockProperties props) {
        return new ScreeningService(mock(StockRepository.class), mock(StockSignalRepository.class),
            api, props, mock(StockBotMetrics.class));
    }

    @Test
    void selectedStock_carriesLookedUpName_notCode() {
        KoreaInvestmentApiClient api = apiReturningName("삼성전자", MARKET_CAP_1000억);

        List<Stock> selected = service(api, new StockProperties()).executeScreening(TODAY, List.of("005930"));

        assertThat(selected).hasSize(1);
        assertThat(selected.get(0).getStockCode()).isEqualTo("005930");
        assertThat(selected.get(0).getStockName()).isEqualTo("삼성전자");
        assertThat(selected.get(0).getProductType()).isEqualTo(StockProductType.STOCK);
        // 콜 예산: Floor 통과 종목당 정확히 1회 (종목명과 상품 유형이 같은 응답)
        verify(api).getStockInfo("005930");
    }

    @Test
    void nullLookup_fallsBackToCode_andStillSelects() {
        KoreaInvestmentApiClient api = apiReturningName(null, MARKET_CAP_1000억);

        List<Stock> selected = service(api, new StockProperties()).executeScreening(TODAY, List.of("005930"));

        // fail-soft: 종목명 미확보가 후보를 떨어뜨리면 안 된다 — 종전 동작(코드)으로 대체
        assertThat(selected).hasSize(1);
        assertThat(selected.get(0).getStockName()).isEqualTo("005930");
        assertThat(selected.get(0).getProductType()).isNull();
    }

    @Test
    void gapFailedStock_doesNotLookUpName() {
        KoreaInvestmentApiClient api = mock(KoreaInvestmentApiClient.class);
        // 갭 0% (시가 == 전일종가) → Floor 2 탈락
        when(api.getQuote(anyString())).thenReturn(new KisQuoteResponse("005930",
            new BigDecimal("10000"), new BigDecimal("10000"), new BigDecimal("10100"),
            new BigDecimal("9900"), new BigDecimal("10000"), BigDecimal.ZERO,
            BigDecimal.ZERO, 1_000_000L, new BigDecimal("10000000000"),
            MARKET_CAP_1000억, new BigDecimal("1.0")));

        assertThat(service(api, new StockProperties()).executeScreening(TODAY, List.of("005930"))).isEmpty();
        // 콜 예산 규약(ADR 0007 과 동일): 탈락 종목에 종목명 조회 없음
        verify(api, never()).getStockInfo(anyString());
    }

    @Test
    void legacyPath_alsoUsesLookedUpName() {
        // legacy Floor: 시총 1500억 이상 — 2000억으로 통과 (갭 2~7%, 강도 100 이상은 위 픽스처가 충족)
        KoreaInvestmentApiClient api = apiReturningName("삼성전자", new BigDecimal("200000000000"));
        StockProperties props = new StockProperties();
        props.getScoring().setEnabled(false);

        List<Stock> selected = service(api, props).executeScreening(TODAY, List.of("005930"));

        assertThat(selected).hasSize(1);
        assertThat(selected.get(0).getStockName()).isEqualTo("삼성전자");
    }

    @Test
    void excludedProductTypeIsDroppedOnBothPaths() {
        StockProperties score = new StockProperties();
        score.getUniverse().setExcludedProductTypes(List.of(StockProductType.ETF));
        StockProperties legacy = new StockProperties();
        legacy.getScoring().setEnabled(false);
        legacy.getUniverse().setExcludedProductTypes(List.of(StockProductType.ETF));
        KisStockInfo etf = new KisStockInfo("KODEX 200", StockProductType.ETF);

        assertThat(service(apiReturning(etf, MARKET_CAP_1000억), score).executeScreening(TODAY, List.of("069500")))
            .isEmpty();
        assertThat(service(apiReturning(etf, new BigDecimal("200000000000")), legacy)
            .executeScreening(TODAY, List.of("069500"))).isEmpty();
    }

    @Test
    void noProductTypeIsExcludedByDefault() {
        // 데이터가 반대 방향이라 제외는 보류 — 키만 마련(ETF·ETN n=16 +0.75% vs 개별주 n=123 -0.64%)
        assertThat(new StockProperties().getUniverse().getExcludedProductTypes()).isEmpty();
    }

    @Test
    void unknownProductTypeIsNeverExcluded() {
        StockProperties props = new StockProperties();
        props.getUniverse().setExcludedProductTypes(List.of(StockProductType.ETF, StockProductType.ETN));

        List<Stock> selected = service(apiReturning(new KisStockInfo("맥쿼리인프라", null), MARKET_CAP_1000억), props)
            .executeScreening(TODAY, List.of("088980"));

        assertThat(selected).hasSize(1);
    }
}
