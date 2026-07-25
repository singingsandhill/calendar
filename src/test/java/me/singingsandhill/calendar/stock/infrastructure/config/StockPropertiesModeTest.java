package me.singingsandhill.calendar.stock.infrastructure.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 봇 동작 모드 기본값 회귀 테스트 (2026-07-24 리뷰 §3-② / P0-2).
 *
 * 실주문(LIVE)이 기본값이면 설정 누락만으로 실계좌 주문이 나간다. 크립토 모듈이
 * 수익성 감사 후 PAPER 기본으로 전환한 선례(ADR trading/modes/0001)를 따라,
 * LIVE 는 명시적 opt-in(STOCK_BOT_MODE=LIVE)으로만 가능해야 한다.
 */
class StockPropertiesModeTest {

    @Test
    void botMode_defaultsToPaper() {
        assertThat(new StockProperties().getBot().getMode())
            .isEqualTo(StockProperties.Bot.Mode.PAPER);
    }

    @Test
    void botMode_nullFallsBackToPaper() {
        StockProperties.Bot bot = new StockProperties().getBot();
        bot.setMode(null);
        assertThat(bot.getMode()).isEqualTo(StockProperties.Bot.Mode.PAPER);
    }
}
