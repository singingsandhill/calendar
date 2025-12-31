package me.singingsandhill.calendar.stock.domain.candle;

/**
 * 캔들 간격
 */
public enum CandleInterval {
    MINUTE_1("1", 60),
    MINUTE_5("5", 300),
    MINUTE_15("15", 900),
    MINUTE_30("30", 1800),
    MINUTE_60("60", 3600),
    DAILY("D", 86400);

    private final String code;
    private final int seconds;

    CandleInterval(String code, int seconds) {
        this.code = code;
        this.seconds = seconds;
    }

    public String getCode() {
        return code;
    }

    public int getSeconds() {
        return seconds;
    }
}
