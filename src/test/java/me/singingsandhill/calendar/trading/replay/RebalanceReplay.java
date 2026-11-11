package me.singingsandhill.calendar.trading.replay;

import me.singingsandhill.calendar.trading.domain.position.Position;
import me.singingsandhill.calendar.trading.domain.position.PositionStatus;
import me.singingsandhill.calendar.trading.domain.regime.DailyBar;
import me.singingsandhill.calendar.trading.domain.regime.MarketRegime;
import me.singingsandhill.calendar.trading.domain.regime.RegimeCalculator;
import me.singingsandhill.calendar.trading.domain.regime.RegimePoint;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 리밸런싱 엔진 일봉 리플레이(작업 6-C) — ADR trading/risk/0007 4-C 규칙을 엔진이 켜진 상태로 일봉 위에서 돌린다.
 *
 * <ul>
 *   <li>하루 한 번 시가에 판정·체결(운영 쿨다운 8시간의 일봉 근사). 목표 = D-1 종가까지의 일봉 레짐 BULL/BEAR 비중, UNKNOWN 스킵</li>
 *   <li>편차·최소주문·슬리피지 버퍼·비중 반올림(소수 4자리)은 {@code RebalanceService} 와 같다</li>
 *   <li>매도는 FIFO 로 포지션 통째. 다른 레짐에서 산 포지션(전환 매도)은 수익 가드 무시, 같은 레짐이면 판정 손익률 ≥ min-sell-pnl</li>
 *   <li>장중 저가로 비상 손절(판정 손익률 ≤ catastrophic) — 그날 이후 레짐이 바뀌기 전까지 매수 차단(BLOCKED)</li>
 *   <li>손익은 모델 수수료(양쪽), 판정 손익률은 기록 진입수수료 + 판정 수수료(운영 taker-fee-rate)</li>
 * </ul>
 */
final class RebalanceReplay {

    record Settings(double bullRatio, double bearRatio, double deviationTrigger, double minOrderAmount,
                    double slippageBuffer, double catastrophicStopPct, double minSellPnlPct, double decisionFeeRate,
                    double feeRate, BigDecimal initialKrw) {
    }

    /** action: BUY / SELL / CATASTROPHIC_STOP / BLOCKED(비상 손절 뒤 매수 차단). */
    record Event(LocalDate date, String action, BigDecimal price, BigDecimal volume) {
    }

    record Result(BigDecimal finalEquity, double totalReturnPct, double maxDrawdownPct, int trades,
                  int catastrophicStops, List<Event> events) {
    }

    private record Holding(Position position, MarketRegime regimeAtEntry) {
    }

    private RebalanceReplay() {
    }

    static Result run(List<DailyBar> bars, List<RegimePoint> points, LocalDate from, Settings s) {
        BigDecimal fee = BigDecimal.valueOf(s.feeRate());
        BigDecimal decisionFee = BigDecimal.valueOf(s.decisionFeeRate());
        BigDecimal stopThreshold = BigDecimal.valueOf(s.catastrophicStopPct() * 100);
        BigDecimal minSellPnl = BigDecimal.valueOf(s.minSellPnlPct() * 100);
        BigDecimal krw = s.initialKrw();
        List<Holding> holdings = new ArrayList<>();
        List<Event> events = new ArrayList<>();
        Drawdown drawdown = new Drawdown(s.initialKrw());
        int trades = 0;
        int stops = 0;
        LocalDate lastStopDay = null;
        BigDecimal equity = s.initialKrw();

        for (DailyBar bar : bars) {
            if (bar.date().isBefore(from)) {
                continue;
            }
            MarketRegime regime = RegimeCalculator.regimeInEffectOn(points, bar.date());
            BigDecimal price = bar.open();

            // 1. 시가 리밸런싱
            if (regime != MarketRegime.UNKNOWN) {
                BigDecimal coinValue = volume(holdings).multiply(price);
                BigDecimal total = krw.add(coinValue);
                BigDecimal target = BigDecimal.valueOf(regime == MarketRegime.BULL ? s.bullRatio() : s.bearRatio());
                BigDecimal ratio = total.signum() == 0 ? BigDecimal.ZERO : coinValue.divide(total, 4, RoundingMode.HALF_UP);
                BigDecimal difference = total.multiply(target).subtract(coinValue);
                if (total.signum() > 0 && ratio.subtract(target).abs().compareTo(BigDecimal.valueOf(s.deviationTrigger())) >= 0
                        && difference.abs().compareTo(BigDecimal.valueOf(s.minOrderAmount())) >= 0) {
                    BigDecimal adjusted = difference.multiply(BigDecimal.valueOf(1.0 - s.slippageBuffer()));
                    if (difference.signum() > 0) {
                        if (lastStopDay != null && !regimeChangedSince(points, lastStopDay, bar.date())) {
                            events.add(new Event(bar.date(), "BLOCKED", price, BigDecimal.ZERO));
                        } else {
                            BigDecimal volume = adjusted.divide(price, 8, RoundingMode.DOWN);
                            BigDecimal buyFee = adjusted.multiply(fee);
                            krw = krw.subtract(adjusted).subtract(buyFee);
                            holdings.add(new Holding(open(price, volume, buyFee, bar.date()), regime));
                            events.add(new Event(bar.date(), "BUY", price, volume));
                            trades++;
                        }
                    } else {
                        BigDecimal targetVolume = adjusted.abs().divide(price, 8, RoundingMode.DOWN);
                        BigDecimal sold = BigDecimal.ZERO;
                        for (Holding h : List.copyOf(holdings)) {
                            if (sold.compareTo(targetVolume) >= 0) {
                                break;
                            }
                            boolean regimeSwitchSell = h.regimeAtEntry() != MarketRegime.UNKNOWN && h.regimeAtEntry() != regime;
                            if (!regimeSwitchSell
                                    && h.position().calculateUnrealizedPnlPctWithFee(price, decisionFee).compareTo(minSellPnl) < 0) {
                                continue;
                            }
                            krw = krw.add(proceeds(h, price, fee));
                            holdings.remove(h);
                            sold = sold.add(h.position().getEntryVolume());
                            events.add(new Event(bar.date(), "SELL", price, h.position().getEntryVolume()));
                            trades++;
                        }
                    }
                }
            }

            // 2. 장중 비상 손절 (저가) — 시가에 이미 넘었으면 시가 체결
            for (Holding h : List.copyOf(holdings)) {
                Position position = h.position();
                if (position.calculateUnrealizedPnlPctWithFee(bar.low(), decisionFee).compareTo(stopThreshold) <= 0) {
                    BigDecimal fill = GridPrices.highestAtOrBelow(bar.open(),
                            p -> position.calculateUnrealizedPnlPctWithFee(p, decisionFee).compareTo(stopThreshold) <= 0);
                    krw = krw.add(proceeds(h, fill, fee));
                    holdings.remove(h);
                    events.add(new Event(bar.date(), "CATASTROPHIC_STOP", fill, position.getEntryVolume()));
                    trades++;
                    stops++;
                    lastStopDay = bar.date();
                }
            }

            // 3. 종가 평가
            equity = krw.add(volume(holdings).multiply(bar.close()));
            drawdown.mark(equity);
        }
        return new Result(equity, returnPct(equity, s.initialKrw()), drawdown.maxPct, trades, stops, events);
    }

    /** 단순 보유 — 첫 거래일 시가에 전액(수수료 포함) 매수, 매일 종가 평가. 마지막에 팔지 않는다(엔진과 같은 시가평가). */
    static Result buyAndHold(List<DailyBar> bars, LocalDate from, double feeRate, BigDecimal initialKrw) {
        List<DailyBar> window = bars.stream().filter(b -> !b.date().isBefore(from)).toList();
        if (window.isEmpty()) {
            return new Result(initialKrw, 0, 0, 0, 0, List.of());
        }
        DailyBar first = window.get(0);
        BigDecimal amount = initialKrw.divide(BigDecimal.ONE.add(BigDecimal.valueOf(feeRate)), 8, RoundingMode.DOWN);
        BigDecimal volume = amount.divide(first.open(), 8, RoundingMode.DOWN);
        BigDecimal rest = initialKrw.subtract(amount).subtract(amount.multiply(BigDecimal.valueOf(feeRate)));
        Drawdown drawdown = new Drawdown(initialKrw);
        BigDecimal equity = initialKrw;
        for (DailyBar bar : window) {
            equity = rest.add(volume.multiply(bar.close()));
            drawdown.mark(equity);
        }
        return new Result(equity, returnPct(equity, initialKrw), drawdown.maxPct, 1, 0,
                List.of(new Event(first.date(), "BUY", first.open(), volume)));
    }

    /** 운영 DailyRegimeService.hasRegimeChangedSince 와 같은 판정 — 단 today 이전 판정만 본다(미래 데이터 금지). */
    static boolean regimeChangedSince(List<RegimePoint> points, LocalDate stopDay, LocalDate today) {
        MarketRegime atStop = RegimeCalculator.regimeInEffectOn(points, stopDay);
        if (atStop == MarketRegime.UNKNOWN) {
            return true;
        }
        LocalDate from = stopDay.minusDays(1);
        return points.stream()
                .filter(p -> !p.date().isBefore(from) && p.date().isBefore(today))
                .anyMatch(p -> p.regime() != MarketRegime.UNKNOWN && p.regime() != atStop);
    }

    private static Position open(BigDecimal price, BigDecimal volume, BigDecimal entryFee, LocalDate day) {
        return new Position(null, "REPLAY", PositionStatus.OPEN, price, volume, price.multiply(volume), null, null, null,
                null, null, null, null, null, price, false, null, day.atStartOfDay(), null, day.atStartOfDay(), entryFee,
                null, null);
    }

    private static BigDecimal proceeds(Holding h, BigDecimal price, BigDecimal fee) {
        BigDecimal amount = h.position().getEntryVolume().multiply(price);
        return amount.subtract(amount.multiply(fee));
    }

    private static BigDecimal volume(List<Holding> holdings) {
        return holdings.stream().map(h -> h.position().getEntryVolume()).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static double returnPct(BigDecimal equity, BigDecimal initial) {
        return equity.divide(initial, MathContext.DECIMAL64).subtract(BigDecimal.ONE).doubleValue() * 100;
    }

    /** 시작자본을 첫 고점으로 둔 일별 평가액의 최대 낙폭(%). */
    private static final class Drawdown {
        private BigDecimal peak;
        private double maxPct;

        Drawdown(BigDecimal initial) {
            this.peak = initial;
        }

        void mark(BigDecimal equity) {
            peak = peak.max(equity);
            if (peak.signum() > 0) {
                maxPct = Math.max(maxPct, peak.subtract(equity).divide(peak, MathContext.DECIMAL64).doubleValue() * 100);
            }
        }
    }
}
