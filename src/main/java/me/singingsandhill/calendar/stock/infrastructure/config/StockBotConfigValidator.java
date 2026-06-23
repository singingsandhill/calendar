package me.singingsandhill.calendar.stock.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 앱 기동 시 stock 봇의 *유효 설정* 을 1줄 요약 로깅하고, 위험·부정합 조합을 WARN 으로 surface 한다.
 *
 * 배경: 봇이 수개월간 잘못된 설정(동적 랭킹 비활성 → 정적 대형주 풀, 체결강도 영구 0,
 * 기본 LIVE 모드)으로 *조용히* 매일 0건만 선정하며 돌았다. 기동 시 유효 설정과 경고를 남겨
 * "조용한 오설정" 을 운영자가 즉시 발견하게 한다.
 *
 * 진단 전용 — 어떤 동작/임계값도 바꾸지 않는다.
 */
@Component
public class StockBotConfigValidator {

    private static final Logger log = LoggerFactory.getLogger(StockBotConfigValidator.class);

    private final StockProperties props;

    public StockBotConfigValidator(StockProperties props) {
        this.props = props;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validateOnStartup() {
        log.info(effectiveConfigSummary());
        for (String warning : collectWarnings()) {
            log.warn("[StockBot] ⚠️ {}", warning);
        }
    }

    /** 유효 설정 1줄 요약 (봇 활성 여부와 무관하게 출력). */
    public String effectiveConfigSummary() {
        StockProperties.Bot bot = props.getBot();
        StockProperties.Universe uni = props.getUniverse();
        StockProperties.Screening scr = props.getScreening();

        String universeSource = uni.getRankApiTop() > 0
            ? "거래량순위 top-" + uni.getRankApiTop() + " (실패 시 fallback " + uni.getFallbackCodes().size() + ")"
            : "fallback-codes(" + uni.getFallbackCodes().size() + ") only, rank 비활성";

        return String.format(
            "[StockBot] enabled=%s, mode=%s, universe=%s, pinned=%d, gapFloor=%s~%s%%, strengthFloor=%s, entryMinStrength=%s, mail=%s",
            bot.isEnabled(), bot.getMode(), universeSource, uni.getPinned().size(),
            scr.getFloorGapPercent(), props.getScoring().getFloorMaxGap(),
            scr.getFloorTradeStrength(), props.getEntry().getEntryMinStrength(),
            props.getMail().isEnabled());
    }

    /**
     * 위험·부정합 설정 경고 목록 (순수 함수 — 테스트 대상). 봇 비활성 시 빈 목록.
     */
    public List<String> collectWarnings() {
        List<String> warnings = new ArrayList<>();
        StockProperties.Bot bot = props.getBot();
        if (!bot.isEnabled()) {
            return warnings;
        }
        StockProperties.Universe uni = props.getUniverse();
        StockProperties.Screening scr = props.getScreening();
        StockProperties.Entry entry = props.getEntry();
        StockProperties.Mail mail = props.getMail();

        // 1) 실거래 모드 — 첫 실주문 위험.
        if (bot.getMode() == StockProperties.Bot.Mode.LIVE) {
            warnings.add("LIVE 모드 — 실주문이 발생합니다. 신규 배포/설정 변경 직후엔 PAPER 로 1일 검증 권장.");
        }
        // 2) 유니버스 공급원 점검.
        boolean rankOff = uni.getRankApiTop() <= 0;
        if (rankOff && uni.getFallbackCodes().isEmpty() && uni.getPinned().isEmpty()) {
            warnings.add("유니버스가 비어있음 (rank 비활성 + fallback/pinned 없음) → 스크리닝이 항상 0건.");
        } else if (rankOff && uni.getPinned().isEmpty()) {
            warnings.add("동적 유니버스(rank-api-top) 비활성 → 정적 fallback-codes 만 사용 (시장 변화 미반영).");
        }
        // 3) 스크리닝 floor 가 진입 임계보다 엄격 — 선정돼도 진입 불가 가능.
        if (scr.getFloorTradeStrength().compareTo(entry.getEntryMinStrength()) > 0) {
            warnings.add(String.format(
                "floor-trade-strength(%s) > entry-min-strength(%s): 스크리닝이 진입보다 엄격 — 선정돼도 진입 불가 가능.",
                scr.getFloorTradeStrength(), entry.getEntryMinStrength()));
        }
        // 4) 메일 수신자 미설정.
        if (mail.isEnabled() && (mail.getTo() == null || mail.getTo().isBlank())) {
            warnings.add("mail.enabled=true 이나 mail.to 미설정 — 스크리닝 메일이 전송되지 않음.");
        }
        return warnings;
    }
}
