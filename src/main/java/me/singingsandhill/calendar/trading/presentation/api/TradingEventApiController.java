package me.singingsandhill.calendar.trading.presentation.api;

import me.singingsandhill.calendar.trading.application.service.TradingEventService;
import me.singingsandhill.calendar.trading.domain.event.TradingEvent;
import me.singingsandhill.calendar.trading.domain.event.TradingEventLevel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/trading/events")
public class TradingEventApiController {

    private final TradingEventService tradingEventService;

    public TradingEventApiController(TradingEventService tradingEventService) {
        this.tradingEventService = tradingEventService;
    }

    @GetMapping
    public ResponseEntity<List<TradingEventDto>> recent(
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            @RequestParam(value = "minLevel", required = false) String minLevel) {
        int safeLimit = Math.min(Math.max(1, limit), 200);
        List<TradingEvent> events = (minLevel == null || minLevel.isBlank())
                ? tradingEventService.findRecent(safeLimit)
                : tradingEventService.findRecentByMinLevel(parseLevel(minLevel), safeLimit);
        return ResponseEntity.ok(events.stream().map(TradingEventDto::from).toList());
    }

    private TradingEventLevel parseLevel(String s) {
        try {
            return TradingEventLevel.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return TradingEventLevel.OK;
        }
    }

    public record TradingEventDto(
            Long id,
            String level,
            String eventType,
            String market,
            String message,
            String createdAt
    ) {
        static TradingEventDto from(TradingEvent e) {
            return new TradingEventDto(
                    e.getId(),
                    e.getLevel().name(),
                    e.getEventType(),
                    e.getMarket(),
                    e.getMessage(),
                    e.getCreatedAt() != null ? e.getCreatedAt().toString() : null
            );
        }
    }
}
