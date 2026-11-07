package me.singingsandhill.calendar.trading.domain.position;

/**
 * 포지션 진입 출처 (ADR trading/observability/0004). 생성 지점에서만 정할 수 있어 서비스가 명시한다.
 * 컬럼 도입 전 포지션은 null — 이벤트 시각 매칭 같은 추정으로 백필하지 않는다.
 */
public enum EntrySource {
    /** 신호 매수 (TradingBotService.executeBuy) */
    SIGNAL,
    /** 리밸런싱 매수 (RebalanceService) */
    REBALANCE,
    /** 수동 매수·검증용 test-order */
    MANUAL,
    /** §8-B 스윕이 체결을 확인해 만든 포지션 (응답 유실 복구) */
    RECOVERY
}
