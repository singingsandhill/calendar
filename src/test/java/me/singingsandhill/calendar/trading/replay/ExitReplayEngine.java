package me.singingsandhill.calendar.trading.replay;

import me.singingsandhill.calendar.trading.domain.position.ExitThresholds;
import me.singingsandhill.calendar.trading.domain.position.Position;
import me.singingsandhill.calendar.trading.domain.position.PositionStatus;
import me.singingsandhill.calendar.trading.domain.trade.BithumbTickSize;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties.Bot.TimeExitMode;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Set;

import static java.time.temporal.ChronoUnit.MINUTES;

/**
 * 출구 리플레이(작업 6-A) — 실제 진입(시각·가격·수량)을 고정하고 분 단위로 출구를 다시 판정한다 (ADR trading/observability/0005).
 *
 * <p>판정은 운영과 같은 부품을 쓴다: 손익률 {@code Position.calculateUnrealizedPnlPctWithFee}(기록된 진입수수료 + 판정 수수료로 청산
 * 수수료 추정), 틱 하한·트레일링 가격 {@code ExitThresholds}, 손익분기 격자 올림 {@code BithumbTickSize}. CLOSE 모델의 순서는
 * {@code RiskManagementService.checkPositionRisk} 와 같다({@code ExitReplayParityTest}). 리스크 판정 뒤에 같은 분의 신호로
 * 신호 매도({@code TradingBotService.executeTradeBySignal} 의 SELL 분기)를, 모델 밖 실제 출구(리밸런싱·수동 등)는 그 시각에 실제
 * 가격으로 반영한다. 손익은 모델 수수료(양쪽)로 다시 계산한다 — 2026-09-27 이전 체결은 수수료가 0 으로 기록됐다.
 *
 * <p>근사: 신호 쿨다운(전역 30분)과 포지션 간 상호작용, 봇 정지 구간은 모델링하지 않는다.
 */
final class ExitReplayEngine {

    /** 리플레이가 다시 판정하는 출구. 나머지(SIGNAL·REBALANCE·MANUAL…)는 시뮬레이션 포지션이 살아 있으면 실제 시각·가격으로. */
    private static final Set<String> MODELED = Set.of("STOP_LOSS", "TAKE_PROFIT", "TRAILING_STOP", "TIME_EXIT",
            "CATASTROPHIC_STOP");
    /** 신호 매도 상수 — yaml 운영값과 같은 Java 기본값(P1-9). 강한 매도 점수 -60 은 TradingBotService.isStrongSellSignal. */
    private static final TradingProperties DEFAULTS = new TradingProperties();
    private static final int STRONG_SELL_SCORE = -60;

    private ExitReplayEngine() {
    }

    /** @return 시뮬레이션 출구. 진입 시각이 시계열 밖이면 null(커버리지 밖). */
    static ExitOutcome simulate(ReplayPosition p, MinuteSeries series, ExitReplayConfig c) {
        if (!covers(series, p)) {
            return null;
        }
        int first = series.indexOf(p.openedAt()) + 1;   // 진입 분 다음 분부터 — 진입 전 가격을 보지 않는다
        Decision d = new Decision(p, c);
        int exogenous = MODELED.contains(p.closeReason()) || p.closedAt() == null || p.exitPrice() == null
                ? -1 : series.indexOf(p.closedAt());
        BigDecimal hwm = p.entryPrice();
        boolean trailing = false;
        BigDecimal stop = null;
        ReplaySignal lastSignal = null;

        for (int i = first; i < series.size(); i++) {
            ReplayBar bar = series.bar(i);
            boolean stale = c.maxHoldMinutes() > 0
                    && MINUTES.between(p.openedAt(), bar.time().plusMinutes(1)) >= c.maxHoldMinutes();

            if (c.priceModel() == PriceModel.HIGH_LOW) {
                if (d.stopHit(bar.low())) {
                    BigDecimal fill = d.stopHit(bar.open()) ? bar.open() : GridPrices.highestAtOrBelow(bar.open(), d::stopHit);
                    return outcome(p, c, "STOP_LOSS", bar, fill);
                }
                if (trailing && bar.low().compareTo(stop) <= 0) {
                    return outcome(p, c, "TRAILING_STOP", bar, bar.open().min(stop));
                }
                if (d.takeProfitHit(bar.high())) {
                    BigDecimal fill = d.takeProfitHit(bar.open()) ? bar.open()
                            : GridPrices.lowestAtOrAbove(bar.open(), d::takeProfitHit);
                    return outcome(p, c, "TAKE_PROFIT", bar, fill);
                }
                // 이 봉의 고가로 올린 스탑은 다음 봉부터 — 봉 안에서 고가·저가 순서를 모른다
                hwm = hwm.max(bar.high());
                boolean tighten = c.timeExitMode() == TimeExitMode.TIGHTEN_TRAIL && stale && d.pnl(bar.close()).signum() >= 0;
                if (!trailing && (d.activationHit(bar.high()) || tighten)) {
                    trailing = true;
                    stop = d.trailingStop(bar.high());
                }
                if (trailing) {
                    stop = stop.max(d.trailingStop(hwm).max(d.breakEven));
                }
                if (timeExit(stale, d.pnl(bar.close()), lastSignal, c)) {
                    return outcome(p, c, "TIME_EXIT", bar, bar.close());
                }
            } else {
                BigDecimal price = bar.close();
                BigDecimal pnl = d.pnl(price);
                if (pnl.compareTo(d.stopThreshold) <= 0) {
                    return outcome(p, c, "STOP_LOSS", bar, price);
                }
                hwm = hwm.max(price);
                boolean tighten = c.timeExitMode() == TimeExitMode.TIGHTEN_TRAIL && stale && pnl.signum() >= 0;
                if (!trailing && (pnl.compareTo(d.activationThreshold) >= 0 || tighten)) {
                    trailing = true;
                    stop = d.trailingStop(price);
                }
                if (trailing) {
                    stop = stop.max(d.trailingStop(hwm).max(d.breakEven));
                    if (price.compareTo(stop) <= 0) {
                        return outcome(p, c, "TRAILING_STOP", bar, price);
                    }
                }
                if (pnl.compareTo(d.takeProfitThreshold) >= 0) {
                    return outcome(p, c, "TAKE_PROFIT", bar, price);
                }
                if (timeExit(stale, pnl, lastSignal, c)) {
                    return outcome(p, c, "TIME_EXIT", bar, price);
                }
            }

            ReplaySignal signal = series.signal(i);
            if (signal != null && d.sellsOn(signal)) {
                return outcome(p, c, "SIGNAL", bar, signal.price());
            }
            if (i == exogenous) {
                return outcome(p, c, p.closeReason(), bar, p.exitPrice());
            }
            if (signal != null) {
                lastSignal = signal;   // 리스크 판정 뒤에 생긴 신호 — 다음 분부터 보인다
            }
        }
        ReplayBar last = series.bar(series.size() - 1);
        return outcome(p, c, "END_OF_DATA", last, last.close());
    }

    /** 진입 분과 그다음 분이 시계열 안에 있어야 다시 판정할 수 있다. */
    static boolean covers(MinuteSeries series, ReplayPosition p) {
        int first = series.indexOf(p.openedAt()) + 1;
        return first > 0 && first < series.size();
    }

    /** 모델 수수료 차감 실현손익 = (청산가 - 진입가) × 수량 - 진입금액 × 수수료 - 청산금액 × 수수료. */
    static BigDecimal netPnl(BigDecimal entryPrice, BigDecimal exitPrice, BigDecimal volume, double feeRate) {
        BigDecimal fee = BigDecimal.valueOf(feeRate);
        BigDecimal entryAmount = entryPrice.multiply(volume);
        BigDecimal exitAmount = exitPrice.multiply(volume);
        return exitAmount.subtract(entryAmount).subtract(entryAmount.multiply(fee)).subtract(exitAmount.multiply(fee));
    }

    private static boolean timeExit(boolean stale, BigDecimal pnl, ReplaySignal lastSignal, ExitReplayConfig c) {
        if (!stale || pnl.signum() < 0) {
            return false;
        }
        return switch (c.timeExitMode()) {
            case BREAKEVEN_CLOSE -> true;
            case TIGHTEN_TRAIL -> false;
            case SIGNAL_GATED -> lastSignal == null || lastSignal.score() <= 0;
        };
    }

    private static ExitOutcome outcome(ReplayPosition p, ExitReplayConfig c, String reason, ReplayBar bar, BigDecimal price) {
        BigDecimal pnl = netPnl(p.entryPrice(), price, p.volume(), c.accountingFeeRate());
        double pct = pnl.divide(p.entryPrice().multiply(p.volume()), MathContext.DECIMAL64).doubleValue() * 100;
        return new ExitOutcome(reason, bar.time(), price, pnl, pct);
    }

    /** 한 포지션의 판정 상수 — 운영 RiskManagementService 와 같은 식. */
    private static final class Decision {
        private final ReplayPosition p;
        private final ExitReplayConfig c;
        private final Position position;
        private final BigDecimal fee;
        private final BigDecimal stopThreshold;
        private final BigDecimal takeProfitThreshold;
        private final BigDecimal activationThreshold;
        private final BigDecimal breakEven;

        Decision(ReplayPosition p, ExitReplayConfig c) {
            this.p = p;
            this.c = c;
            BigDecimal e = p.entryPrice();
            this.position = new Position(p.id(), "REPLAY", PositionStatus.OPEN, e, p.volume(), e.multiply(p.volume()),
                    null, null, null, null, null, null, null, null, e, false, null, p.openedAt(), null, p.openedAt(),
                    p.recordedEntryFee(), null, null);
            this.fee = BigDecimal.valueOf(c.decisionFeeRate());
            this.stopThreshold = BigDecimal.valueOf(ExitThresholds.effectiveStopLoss(c.stopLoss(), c.minStopTicks(), e) * 100);
            this.takeProfitThreshold = BigDecimal.valueOf(c.takeProfit() * 100);
            this.activationThreshold = BigDecimal.valueOf(c.trailingActivation() * 100);
            this.breakEven = BithumbTickSize.roundUp(e.multiply(BigDecimal.valueOf(1 + c.decisionFeeRate() * 2)));
        }

        BigDecimal pnl(BigDecimal price) {
            return position.calculateUnrealizedPnlPctWithFee(price, fee);
        }

        boolean stopHit(BigDecimal price) {
            return pnl(price).compareTo(stopThreshold) <= 0;
        }

        boolean takeProfitHit(BigDecimal price) {
            return pnl(price).compareTo(takeProfitThreshold) >= 0;
        }

        boolean activationHit(BigDecimal price) {
            return pnl(price).compareTo(activationThreshold) >= 0;
        }

        BigDecimal trailingStop(BigDecimal reference) {
            return ExitThresholds.trailingStopPrice(reference, c.trailingStop(), c.minTrailingTicks());
        }

        /** TradingBotService.executeTradeBySignal 의 SELL 분기 — 최소 보유, 슬리피지 버퍼 가격의 판정 손익률, 강한 신호 손실 한도. */
        boolean sellsOn(ReplaySignal signal) {
            if (!"SELL".equals(signal.type()) || signal.price() == null
                    || MINUTES.between(p.openedAt(), signal.time()) < DEFAULTS.getBot().getMinHoldingMinutes()) {
                return false;
            }
            BigDecimal conservative = signal.price()
                    .multiply(BigDecimal.valueOf(1.0 - DEFAULTS.getRisk().getSlippageBuffer()));
            BigDecimal pnl = pnl(conservative);
            boolean strong = signal.score() <= STRONG_SELL_SCORE || signal.bearishDivergence();
            if (strong) {
                return pnl.compareTo(BigDecimal.valueOf(DEFAULTS.getRisk().getStrongSignalMaxLoss() * 100)) >= 0;
            }
            return pnl.doubleValue() >= DEFAULTS.getRisk().getMinProfitThreshold() * 100;
        }
    }
}
