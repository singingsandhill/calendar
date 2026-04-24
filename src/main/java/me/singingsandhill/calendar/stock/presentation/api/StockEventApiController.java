package me.singingsandhill.calendar.stock.presentation.api;

import me.singingsandhill.calendar.stock.application.service.StockPositionService;
import me.singingsandhill.calendar.stock.domain.position.StockPosition;
import me.singingsandhill.calendar.stock.domain.signal.StockSignal;
import me.singingsandhill.calendar.stock.domain.signal.StockSignalRepository;
import me.singingsandhill.calendar.stock.domain.signal.StockSignalType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 이벤트 스트림 API — 포지션 진입/청산 + 주요 시그널을 시간 역순으로 merge하여 반환.
 * 대시보드 Recent Events 섹션 전용.
 */
@RestController
@RequestMapping("/api/stock/events")
public class StockEventApiController {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final StockPositionService positionService;
    private final StockSignalRepository signalRepository;

    public StockEventApiController(StockPositionService positionService,
                                    StockSignalRepository signalRepository) {
        this.positionService = positionService;
        this.signalRepository = signalRepository;
    }

    @GetMapping
    public ResponseEntity<List<EventResponse>> getEvents(
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "10") int limit) {

        LocalDate tradingDate = date != null ? LocalDate.parse(date) : LocalDate.now(KST);
        List<EventResponse> events = new ArrayList<>();

        for (StockPosition p : positionService.getAllPositions(tradingDate)) {
            if (p.getEnteredAt() != null) {
                events.add(new EventResponse(
                    p.getEnteredAt(),
                    "ENTRY",
                    p.getStockCode(),
                    entryMessage(p),
                    p.getEntryAmount(),
                    null
                ));
            }
            if (p.getClosedAt() != null && p.getCloseReason() != null) {
                events.add(new EventResponse(
                    p.getClosedAt(),
                    p.getCloseReason().name(),
                    p.getStockCode(),
                    closeMessage(p),
                    p.getRealizedPnl(),
                    p.getRealizedPnlPercent()
                ));
            }
        }

        LocalDateTime from = tradingDate.atStartOfDay();
        LocalDateTime to = tradingDate.plusDays(1).atStartOfDay();
        for (StockSignal s : signalRepository.findBySignalTimeBetween(from, to)) {
            if (s.getSignalType() == StockSignalType.PULLBACK_ENTRY
                || s.getSignalType() == StockSignalType.HIGH_FORMED) {
                events.add(new EventResponse(
                    s.getSignalTime(),
                    "SIGNAL_" + s.getSignalType().name(),
                    s.getStockCode(),
                    s.getSignalType().getDisplayName(),
                    s.getCurrentPrice(),
                    s.getPullbackPercent()
                ));
            }
        }

        List<EventResponse> sorted = events.stream()
            .sorted(Comparator.comparing(EventResponse::time).reversed())
            .limit(Math.max(1, Math.min(limit, 100)))
            .toList();

        return ResponseEntity.ok(sorted);
    }

    private String entryMessage(StockPosition p) {
        if (p.getEntryPrice() == null || p.getEntryQuantity() == null) return "진입";
        return String.format("진입 %,d원 × %d주",
            p.getEntryPrice().intValue(), p.getEntryQuantity());
    }

    private String closeMessage(StockPosition p) {
        String reason = p.getCloseReason() != null ? p.getCloseReason().name() : "청산";
        if (p.getAverageExitPrice() == null) return reason;
        return String.format("%s (청산가 %,d원)", reason, p.getAverageExitPrice().intValue());
    }

    public record EventResponse(
        LocalDateTime time,
        String kind,
        String stockCode,
        String message,
        BigDecimal amount,
        BigDecimal pnlPercent
    ) {}
}
