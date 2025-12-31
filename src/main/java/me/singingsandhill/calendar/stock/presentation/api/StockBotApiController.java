package me.singingsandhill.calendar.stock.presentation.api;

import me.singingsandhill.calendar.stock.application.service.GapPullbackBotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 주식 트레이딩 봇 제어 API
 */
@RestController
@RequestMapping("/api/stock/bot")
public class StockBotApiController {

    private final GapPullbackBotService botService;

    public StockBotApiController(GapPullbackBotService botService) {
        this.botService = botService;
    }

    /**
     * 봇 상태 조회
     */
    @GetMapping("/status")
    public ResponseEntity<BotStatusResponse> getStatus() {
        GapPullbackBotService.BotStatus status = botService.getStatus();
        return ResponseEntity.ok(new BotStatusResponse(
            status.running(),
            status.paused(),
            status.watchingCount(),
            status.positionCount(),
            status.tradingPhase(),
            status.startedAt()
        ));
    }

    /**
     * 봇 시작
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> start() {
        boolean success = botService.start();
        return ResponseEntity.ok(Map.of(
            "success", success,
            "message", success ? "Bot started" : "Bot is already running or API not configured"
        ));
    }

    /**
     * 봇 중지
     */
    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stop() {
        boolean success = botService.stop();
        return ResponseEntity.ok(Map.of(
            "success", success,
            "message", success ? "Bot stopped" : "Bot is not running"
        ));
    }

    /**
     * 봇 일시정지
     */
    @PostMapping("/pause")
    public ResponseEntity<Map<String, Object>> pause() {
        boolean success = botService.pause();
        return ResponseEntity.ok(Map.of(
            "success", success,
            "message", success ? "Bot paused" : "Cannot pause bot"
        ));
    }

    /**
     * 봇 재개
     */
    @PostMapping("/resume")
    public ResponseEntity<Map<String, Object>> resume() {
        boolean success = botService.resume();
        return ResponseEntity.ok(Map.of(
            "success", success,
            "message", success ? "Bot resumed" : "Cannot resume bot"
        ));
    }

    /**
     * 긴급 청산
     */
    @PostMapping("/emergency-close")
    public ResponseEntity<Map<String, Object>> emergencyClose() {
        botService.emergencyCloseAll();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Emergency close executed"
        ));
    }

    // Response DTOs
    public record BotStatusResponse(
        boolean running,
        boolean paused,
        int watchingCount,
        int positionCount,
        String tradingPhase,
        LocalDateTime startedAt
    ) {}
}
