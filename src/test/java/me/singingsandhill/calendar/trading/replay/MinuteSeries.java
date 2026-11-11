package me.singingsandhill.calendar.trading.replay;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import static java.time.temporal.ChronoUnit.MINUTES;

/**
 * 분 단위 시계열 — 1분봉과 신호를 분 인덱스로 둔다. 체결이 없어 봉이 없는 분은 직전 종가의 평탄 봉으로 메운다(봇은 매분
 * 현재가를 읽으므로 그 분에도 직전 가격으로 시간청산 등을 판정한다). 같은 분의 신호가 여럿이면 마지막 것. 첫 봉 이전·마지막 봉
 * 이후는 범위 밖이다.
 */
final class MinuteSeries {

    private final LocalDateTime start;
    private final ReplayBar[] bars;
    private final ReplaySignal[] signals;

    MinuteSeries(List<ReplayBar> bars, List<ReplaySignal> signals) {
        if (bars.isEmpty()) {
            this.start = null;
            this.bars = new ReplayBar[0];
            this.signals = new ReplaySignal[0];
            return;
        }
        List<ReplayBar> ordered = bars.stream().sorted(Comparator.comparing(ReplayBar::time)).toList();
        this.start = ordered.get(0).time().truncatedTo(MINUTES);
        int size = (int) MINUTES.between(start, ordered.get(ordered.size() - 1).time().truncatedTo(MINUTES)) + 1;
        this.bars = new ReplayBar[size];
        for (ReplayBar bar : ordered) {
            this.bars[indexOf(bar.time())] = bar;
        }
        for (int i = 1; i < size; i++) {
            if (this.bars[i] == null) {
                this.bars[i] = ReplayBar.flat(start.plusMinutes(i), this.bars[i - 1].close());
            }
        }
        this.signals = new ReplaySignal[size];
        for (ReplaySignal signal : signals) {
            int i = indexOf(signal.time());
            if (i >= 0 && i < size) {
                this.signals[i] = signal;
            }
        }
    }

    int size() {
        return bars.length;
    }

    /** t 가 속한 분의 인덱스 — 범위 밖이면 음수 또는 size 이상. */
    int indexOf(LocalDateTime t) {
        return start == null ? -1 : (int) MINUTES.between(start, t.truncatedTo(MINUTES));
    }

    ReplayBar bar(int i) {
        return bars[i];
    }

    ReplaySignal signal(int i) {
        return signals[i];
    }
}
