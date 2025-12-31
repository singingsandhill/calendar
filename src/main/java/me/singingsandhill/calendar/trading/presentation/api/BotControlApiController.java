package me.singingsandhill.calendar.trading.presentation.api;

import me.singingsandhill.calendar.trading.application.service.TradingBotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/trading/bot")
public class BotControlApiController {

    private final TradingBotService tradingBotService;

    public BotControlApiController(TradingBotService tradingBotService) {
        this.tradingBotService = tradingBotService;
    }

    /**
     * 봇 상태 조회
     */
    @GetMapping("/status")
    public ResponseEntity<BotStatusDto> getStatus() {
        TradingBotService.BotStatus status = tradingBotService.getStatus();
        return ResponseEntity.ok(new BotStatusDto(
                status.running(),
                status.paused(),
                status.market()
        ));
    }

    /**
     * 봇 시작
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> start() {
        boolean success = tradingBotService.start();
        return ResponseEntity.ok(Map.of(
                "success", success,
                "message", success ? "Bot started" : "Bot is already running"
        ));
    }

    /**
     * 봇 중지
     */
    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stop() {
        boolean success = tradingBotService.stop();
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
        boolean success = tradingBotService.pause();
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
        boolean success = tradingBotService.resume();
        return ResponseEntity.ok(Map.of(
                "success", success,
                "message", success ? "Bot resumed" : "Cannot resume bot"
        ));
    }

    /**
     * 수동 매수
     */
    @PostMapping("/manual/buy")
    public ResponseEntity<Map<String, Object>> manualBuy(@RequestBody ManualOrderRequest request) {
        boolean success = tradingBotService.manualBuy(BigDecimal.valueOf(request.amount()));
        return ResponseEntity.ok(Map.of(
                "success", success,
                "message", success ? "Manual buy order placed" : "Failed to place buy order"
        ));
    }

    /**
     * 수동 매도
     */
    @PostMapping("/manual/sell")
    public ResponseEntity<Map<String, Object>> manualSell(@RequestBody ManualOrderRequest request) {
        boolean success = tradingBotService.manualSell(BigDecimal.valueOf(request.volume()));
        return ResponseEntity.ok(Map.of(
                "success", success,
                "message", success ? "Manual sell order placed" : "Failed to place sell order"
        ));
    }

    /**
     * 긴급 청산
     */
    @PostMapping("/emergency-close")
    public ResponseEntity<Map<String, Object>> emergencyClose() {
        tradingBotService.emergencyClose();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Emergency close executed"
        ));
    }

    // Request/Response DTOs
    public record BotStatusDto(boolean running, boolean paused, String market) {}

    public record ManualOrderRequest(Double amount, Double volume) {}
}
