package me.singingsandhill.calendar.trading.application.dto;

import me.singingsandhill.calendar.trading.domain.signal.DivergenceType;

public record DivergenceResult(
    DivergenceType rsiDivergence,
    DivergenceType stochDivergence,
    DivergenceType volumeDivergence
) {
    public boolean hasBullishDivergence() {
        return rsiDivergence == DivergenceType.BULLISH ||
               stochDivergence == DivergenceType.BULLISH ||
               volumeDivergence == DivergenceType.BULLISH;
    }

    public boolean hasBearishDivergence() {
        return rsiDivergence == DivergenceType.BEARISH ||
               stochDivergence == DivergenceType.BEARISH ||
               volumeDivergence == DivergenceType.BEARISH;
    }

    public int countBullishDivergences() {
        int count = 0;
        if (rsiDivergence == DivergenceType.BULLISH) count++;
        if (stochDivergence == DivergenceType.BULLISH) count++;
        if (volumeDivergence == DivergenceType.BULLISH) count++;
        return count;
    }

    public int countBearishDivergences() {
        int count = 0;
        if (rsiDivergence == DivergenceType.BEARISH) count++;
        if (stochDivergence == DivergenceType.BEARISH) count++;
        if (volumeDivergence == DivergenceType.BEARISH) count++;
        return count;
    }

    public static DivergenceResult none() {
        return new DivergenceResult(DivergenceType.NONE, DivergenceType.NONE, DivergenceType.NONE);
    }
}
