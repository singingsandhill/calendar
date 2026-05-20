package me.singingsandhill.calendar.common.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Google AdSense 설정.
 *
 * <p>{@code client} 가 비어 있으면 페이지에 {@code adsbygoogle.js} 스크립트를 로드하지 않는다.
 * 슬롯 ID 가 비어 있으면 해당 fragment 를 렌더하지 않아 placeholder DOM 이 노출되지 않는다.
 *
 * <p>슬롯 ID 는 AdSense 심사 통과 후 콘솔에서 발급받는 값이므로, 통과 전까지는 환경변수 미설정
 * 상태로 두어 fragment 자체가 렌더되지 않게 한다.
 */
@ConfigurationProperties(prefix = "adsense")
public record AdsenseProperties(
        String client,
        String slotLeaderboard,
        String slotInfeed,
        String slotRectangle
) {

    public AdsenseProperties {
        client = client == null ? "" : client.trim();
        slotLeaderboard = slotLeaderboard == null ? "" : slotLeaderboard.trim();
        slotInfeed = slotInfeed == null ? "" : slotInfeed.trim();
        slotRectangle = slotRectangle == null ? "" : slotRectangle.trim();
    }

    public boolean isEnabled() {
        return !client.isEmpty();
    }

    public boolean hasLeaderboardSlot() {
        return isEnabled() && !slotLeaderboard.isEmpty();
    }

    public boolean hasInfeedSlot() {
        return isEnabled() && !slotInfeed.isEmpty();
    }

    public boolean hasRectangleSlot() {
        return isEnabled() && !slotRectangle.isEmpty();
    }
}
