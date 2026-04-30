package me.singingsandhill.calendar.stock.application.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.Closeable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 주식 봇 거래 이벤트 전용 로거.
 *
 * 로거 이름 me.singingsandhill.calendar.stock.trade 는 logback-spring.xml 에서
 * 별도 appender 로 분리되어 logs/stock-events.log 에 key=value 한 줄 단위로 적재된다.
 *
 * 사용 예
 *   TradeEvents.event("ENTRY_ATTEMPT")
 *       .with("stockCode", code)
 *       .with("result", "REJECTED")
 *       .with("reason", "imbalance")
 *       .log();
 */
public final class TradeEvents {

    private static final Logger EVENT_LOG = LoggerFactory.getLogger("me.singingsandhill.calendar.stock.trade");

    public static final String MDC_PHASE = "stock.phase";
    public static final String MDC_TRADING_DATE = "stock.tradingDate";
    public static final String MDC_STOCK_CODE = "stock.code";
    public static final String MDC_TRADE_ID = "stock.tradeId";

    private TradeEvents() {}

    public static Builder event(String name) {
        return new Builder(name);
    }

    public static Closeable phase(String phase) {
        return mdc(MDC_PHASE, phase);
    }

    public static Closeable tradingDate(Object tradingDate) {
        return mdc(MDC_TRADING_DATE, tradingDate);
    }

    public static Closeable stockCode(String stockCode) {
        return mdc(MDC_STOCK_CODE, stockCode);
    }

    public static Closeable tradeId(Object tradeId) {
        return mdc(MDC_TRADE_ID, tradeId);
    }

    /**
     * 여러 MDC 키를 한꺼번에 세팅하고, close 시 모두 복원.
     */
    public static Closeable mdcAll(String... pairs) {
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException("pairs must be even length");
        }
        Map<String, String> previous = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            String key = pairs[i];
            String value = pairs[i + 1];
            previous.put(key, MDC.get(key));
            if (value == null) {
                MDC.remove(key);
            } else {
                MDC.put(key, value);
            }
        }
        return () -> previous.forEach((k, v) -> {
            if (v == null) {
                MDC.remove(k);
            } else {
                MDC.put(k, v);
            }
        });
    }

    private static Closeable mdc(String key, Object value) {
        String previous = MDC.get(key);
        if (value == null) {
            MDC.remove(key);
        } else {
            MDC.put(key, String.valueOf(value));
        }
        return () -> {
            if (previous == null) {
                MDC.remove(key);
            } else {
                MDC.put(key, previous);
            }
        };
    }

    public static final class Builder {
        private final String name;
        private final LinkedHashMap<String, String> fields = new LinkedHashMap<>();

        private Builder(String name) {
            this.name = name;
        }

        public Builder with(String key, Object value) {
            fields.put(key, format(value));
            return this;
        }

        public void log() {
            if (!EVENT_LOG.isInfoEnabled()) {
                return;
            }
            StringBuilder sb = new StringBuilder("event=").append(name);
            fields.forEach((k, v) -> sb.append(' ').append(k).append('=').append(v));
            EVENT_LOG.info(sb.toString());
        }

        public void warn() {
            if (!EVENT_LOG.isWarnEnabled()) {
                return;
            }
            StringBuilder sb = new StringBuilder("event=").append(name);
            fields.forEach((k, v) -> sb.append(' ').append(k).append('=').append(v));
            EVENT_LOG.warn(sb.toString());
        }

        private static String format(Object value) {
            if (value == null) {
                return "-";
            }
            if (value instanceof BigDecimal bd) {
                return bd.setScale(Math.min(bd.scale(), 4), RoundingMode.HALF_UP).toPlainString();
            }
            String s = String.valueOf(value);
            if (s.indexOf(' ') >= 0 || s.indexOf('=') >= 0) {
                return '"' + s.replace("\"", "\\\"") + '"';
            }
            return s;
        }
    }
}
