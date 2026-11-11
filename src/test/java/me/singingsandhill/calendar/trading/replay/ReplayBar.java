package me.singingsandhill.calendar.trading.replay;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 1분봉 — time 은 KST 분 시작 시각. 빗썸 공개 API 는 체결이 없는 분을 건너뛴다. */
record ReplayBar(LocalDateTime time, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close) {

    static ReplayBar flat(LocalDateTime time, BigDecimal price) {
        return new ReplayBar(time, price, price, price, price);
    }
}
