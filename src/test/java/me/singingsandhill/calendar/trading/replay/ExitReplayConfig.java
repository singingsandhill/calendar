package me.singingsandhill.calendar.trading.replay;

import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties.Bot.TimeExitMode;

/**
 * 출구 리플레이 설정 한 점. decisionFeeRate 는 봇이 판정에 쓰는 청산 수수료 추정(taker-fee-rate), accountingFeeRate 는
 * 손익 계산용 실수수료(양쪽). minStopTicks·minTrailingTicks 0 = 틱 하한 끔.
 */
record ExitReplayConfig(double stopLoss, double takeProfit, double trailingStop, double trailingActivation,
                        int minStopTicks, int minTrailingTicks, TimeExitMode timeExitMode, long maxHoldMinutes,
                        double decisionFeeRate, double accountingFeeRate, PriceModel priceModel) {

    /** 2026-07-01 ~ 10-20 운영 설정 — 손절 -1.5%·TP +3%·트레일링 0.8%/+1.5%·maxHold 360·BREAKEVEN_CLOSE·틱 하한 없음. */
    static ExitReplayConfig historical(PriceModel priceModel, double decisionFeeRate, double accountingFeeRate) {
        return new ExitReplayConfig(-0.015, 0.03, 0.008, 0.015, 0, 0, TimeExitMode.BREAKEVEN_CLOSE, 360,
                decisionFeeRate, accountingFeeRate, priceModel);
    }

    ExitReplayConfig withExits(double trailing, double activation, TimeExitMode mode, long maxHold, int ticks) {
        return new ExitReplayConfig(stopLoss, takeProfit, trailing, activation, ticks, ticks, mode, maxHold,
                decisionFeeRate, accountingFeeRate, priceModel);
    }

    String label() {
        return String.format("trail %.1f%% · act %.1f%% · %s · hold %d · tick %s", trailingStop * 100,
                trailingActivation * 100, timeExitMode, maxHoldMinutes, minStopTicks > 0 ? "on" : "off");
    }
}
