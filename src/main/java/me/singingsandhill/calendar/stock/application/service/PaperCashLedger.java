package me.singingsandhill.calendar.stock.application.service;

import me.singingsandhill.calendar.stock.domain.trade.StockTrade;
import me.singingsandhill.calendar.stock.domain.trade.StockTradeRepository;
import me.singingsandhill.calendar.stock.infrastructure.config.StockProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;

/**
 * PAPER/BACKTEST 가상 예수금 원장 (ADR stock/modes/0004).
 *
 * 거래일마다 {@code stock.bot.paper-cash} 에서 시작해 그날의 시뮬레이션 체결(SIM- 주문)을 더하고 뺀다 —
 * 매수는 (체결금액 + 매수 수수료) 차감, 매도는 (체결금액 − 매도 수수료·세금) 가산. 잔고를 메모리에 들지
 * 않고 매번 DB 에서 계산하므로 재시작해도 같은 값이다. 장중 전략이고 11:20 에 전량 청산하므로 일 단위
 * 리셋이 단순하고 안전하다 (전일 미청산분은 원장에 이월되지 않는다).
 *
 * 51거래일 0체결(2026-08-03~10-20)의 원인은 PAPER 사이징이 실계좌 예수금(0원)을 읽은 것이었다.
 */
@Service
@Transactional(readOnly = true)
public class PaperCashLedger {

    private static final Logger log = LoggerFactory.getLogger(PaperCashLedger.class);

    private final StockTradeRepository tradeRepository;
    private final StockProperties stockProperties;
    private final Clock clock;

    public PaperCashLedger(StockTradeRepository tradeRepository,
                           StockProperties stockProperties,
                           Clock clock) {
        this.tradeRepository = tradeRepository;
        this.stockProperties = stockProperties;
        this.clock = clock;
    }

    /** 해당 거래일의 가상 가용 예수금. */
    public BigDecimal available(LocalDate tradingDate) {
        BigDecimal cash = stockProperties.getBot().getPaperCash();
        for (StockTrade trade : tradeRepository.findByOrderedAtBetween(
                tradingDate.atStartOfDay(), tradingDate.plusDays(1).atStartOfDay())) {
            if (!trade.isSimulated() || trade.getExecutedPrice() == null
                    || trade.getExecutedQuantity() == null || trade.getExecutedQuantity() <= 0) {
                continue;
            }
            BigDecimal amount = trade.getExecutedPrice().multiply(BigDecimal.valueOf(trade.getExecutedQuantity()));
            BigDecimal fee = trade.getFee() != null ? trade.getFee() : BigDecimal.ZERO;
            cash = trade.isBuy()
                ? cash.subtract(amount).subtract(fee)
                : cash.add(amount).subtract(fee);
        }
        return cash;
    }

    public BigDecimal availableToday() {
        return available(LocalDate.now(clock));
    }

    /** 기동 로그 — PAPER 인데 쓸 돈이 0원인 상태를 51거래일 동안 아무도 못 봤다. */
    @EventListener(ApplicationReadyEvent.class)
    public void logOnStartup() {
        StockProperties.Bot bot = stockProperties.getBot();
        if (bot.getMode() == StockProperties.Bot.Mode.LIVE) {
            return;
        }
        log.info("[StockBot] mode={}, paperCash={}, paperAvailableCash={} (거래일 {})",
            bot.getMode(), bot.getPaperCash(), availableToday(), LocalDate.now(clock));
    }
}
