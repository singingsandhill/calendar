# ADR-0001 (trading/risk): 서킷브레이커 — 일일 손실 / 연속 손실 한도

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-05-30 |
| 도메인 | trading (코인) |
| 관심사 | 알고리즘 / 리스크 |
| 관련 | 수익성 감사 `docs/audit/coin-trading-profit-audit-2026-05-30.md` (P0-2) |

## Context — 무엇이 문제였나

코인 봇에는 **드로다운 차단 장치가 전혀 없었다.** 손절(-3%)·강신호 가드는 *개별
트레이드* 단위일 뿐, 하락장에서 손절→재진입→손절을 반복하며 잔잔한 승의 누적을
지우고 자본을 무한정 출혈시킬 수 있었다. 수익성 감사가 이를 P0(실거래 전 필수
안전장치)로 분류.

## Decision — 무엇을 골랐나

`TradingCircuitBreaker` 도입 — 다음 중 하나라도 충족 시 **신규 진입(BUY) 차단**
(리스크 청산·익절은 계속 허용, 자본 보호는 멈추지 않음).

- **연속 손실:** 손실 청산이 `risk.maxConsecutiveLosses`(기본 3)회 연속이면 차단.
  손실 청산마다 +1, 이익/본전 청산 시 0 으로 리셋(인메모리 스트릭).
- **일일 손실:** 당일(KST 자정 이후) 실현손익이 당일 시작 자본 대비
  `risk.maxDailyLossPct`(기본 -5%) 이하이면 차단. 시작 자본은 당일 첫 계좌 스냅샷;
  스냅샷 부재 시 일일 가드만 스킵(연속 손실 가드는 계속).
- `risk.circuitBreakerEnabled`(기본 true)로 on/off.
- 차단 시 `TradingEvent(CRITICAL, CIRCUIT_BREAKER)` 기록.

연결: 청산 결과는 `TradingBotService.executeSell` 과
`RiskManagementService.closePosition` 에서 `recordOutcome(realizedPnl)` 로 집계.
진입 차단 판정은 `executeBuy` 진입부에서 수행.

## Rationale — 왜 이 선택인가

| 대안 | 기각 이유 |
|---|---|
| 손절만 강화 | 개별 트레이드 단위 — 반복 손절 누적 막지 못함 |
| 봇 전체 정지 | 열린 포지션의 리스크 청산까지 멈춤 → 더 위험 |
| **(선택) 신규 진입만 차단** | 자본 보호(청산)는 유지, 추가 노출만 중단 |

임계 3회 / -5% 는 보수적 기본값(ADA 운영 기준 재보정 가능). 연속 손실 스트릭은
이익 청산으로만 풀리므로 데스스파이럴에서 자연히 묶이고, 일일 가드는 KST 자정에
실현손익이 리셋되며 자연 해제된다.

## Consequences — 영향

- **긍정:** 최악 일손실을 ~-5% 로 캡, 반복 손절 데스스파이럴 차단.
- **부정:** 추세 회복 국면 초입에 진입을 놓칠 수 있음(보수성의 대가).
- **한계:** 연속 손실 스트릭은 인메모리 — 프로세스 재시작 시 0 으로 초기화. 영속이
  필요하면 후속 작업.
- **상호작용:** 수동 매수(`manualBuy`)는 운영자 의도이므로 차단하지 않음.

## References

- 코드: `application/service/TradingCircuitBreaker.java`,
  `TradingBotService.executeBuy/executeSell`, `RiskManagementService.closePosition`,
  `infrastructure/config/TradingProperties.Risk`
- 테스트: `TradingCircuitBreakerTest`
