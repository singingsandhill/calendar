package me.singingsandhill.calendar.trading.presentation.api;

import me.singingsandhill.calendar.trading.application.service.RebalanceService;
import me.singingsandhill.calendar.trading.infrastructure.config.TradingProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/trading/rebalance")
public class RebalanceApiController {

    private final RebalanceService rebalanceService;
    private final TradingProperties tradingProperties;

    public RebalanceApiController(RebalanceService rebalanceService,
                                  TradingProperties tradingProperties) {
        this.rebalanceService = rebalanceService;
        this.tradingProperties = tradingProperties;
    }

    /**
     * 리밸런싱 상태 조회
     */
    @GetMapping("/status")
    public ResponseEntity<RebalanceStatusDto> getStatus() {
        String market = tradingProperties.getBot().getMarket();
        RebalanceService.RebalanceStatus s = rebalanceService.getStatus(market);
        return ResponseEntity.ok(new RebalanceStatusDto(
                s.enabled(),
                market,
                s.marketRegime(),
                s.currentRatio() != null ? s.currentRatio().doubleValue() : null,
                s.targetRatio() != null ? s.targetRatio().doubleValue() : null,
                s.deviation() != null ? s.deviation().doubleValue() : null,
                s.deviationTrigger() != null ? s.deviationTrigger().doubleValue() : null,
                s.krwBalance() != null ? s.krwBalance().doubleValue() : null,
                s.coinBalance() != null ? s.coinBalance().doubleValue() : null,
                s.currentPrice() != null ? s.currentPrice().doubleValue() : null,
                s.ma60() != null ? s.ma60().doubleValue() : null,
                s.cooldownRemainingSec(),
                tradingProperties.getRebalancing().getCooldownMinutes(),
                s.lastRebalanceTime() != null ? s.lastRebalanceTime().toString() : null
        ));
    }

    /**
     * 수동 리밸런싱 실행 (쿨다운/조건 충족 시)
     */
    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> execute() {
        String market = tradingProperties.getBot().getMarket();
        RebalanceService.RebalanceResult result = rebalanceService.checkAndExecute(market);

        Map<String, Object> body = new HashMap<>();
        body.put("executed", result.executed());
        body.put("currentRatio", result.currentRatio() != null ? result.currentRatio().doubleValue() : null);
        body.put("targetRatio", result.targetRatio() != null ? result.targetRatio().doubleValue() : null);
        body.put("deviation", result.deviation() != null ? result.deviation().doubleValue() : null);
        body.put("reason", result.reason());
        body.put("message", result.executed()
                ? "Rebalance executed"
                : (result.reason() != null ? result.reason() : "Rebalance not executed (cooldown, deviation, or conditions not met)"));
        return ResponseEntity.ok(body);
    }

    public record RebalanceStatusDto(
            boolean enabled,
            String market,
            String marketRegime,
            Double currentRatio,
            Double targetRatio,
            Double deviation,
            Double deviationTrigger,
            Double krwBalance,
            Double coinBalance,
            Double currentPrice,
            Double ma60,
            long cooldownRemainingSec,
            long cooldownMinutes,
            String lastRebalanceTime
    ) {}
}
