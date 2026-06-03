# ADR-0001 (trading/modes): PAPER 기본 모드 + 주문 게이트 + 인메모리 체결 시뮬레이션

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-05-30 |
| 도메인 | trading (코인) |
| 관심사 | 모드 / 실행 안전 |
| 관련 | 수익성 감사 `docs/audit/coin-trading-profit-audit-2026-05-30.md` (P0-1) |

## Context — 무엇이 문제였나

코인 봇은 stock 봇과 달리 **운영 모드 구분이 없어** `bot.enabled=true` 이고 API 키만
있으면 모든 주문이 즉시 실거래로 나갔다. 결과:

1. 파라미터·로직 변경을 **검증할 수단이 실거래뿐** — 오프라인 백테스트/페이퍼 불가.
2. 수익성 감사에서 드러난 구조적 문제(출구 비대칭, 회계 드리프트)를 고치려면
   파라미터 재보정이 필요한데, 검증 없이 실거래에 반영하는 것은 risk-of-ruin.

stock 봇은 이미 [`stock/modes/0001`](../../stock/modes/0001-paper-backtest-mode-and-clock-bean.md)
로 `Mode {LIVE, PAPER, BACKTEST}` 를 가지고 있어 동일 패턴을 코인에 이식한다.

## Decision — 무엇을 골랐나

`TradingProperties.Bot.Mode {LIVE, PAPER, BACKTEST}` 도입.

- **기본값 LIVE** — 운영자 결정(2026-05-30). 감사는 PAPER-기본을 권고했으나, 기존 운영
  봇의 동작 연속성을 위해 LIVE 를 기본으로 두고 PAPER/BACKTEST 를 *옵트인 검증 모드* 로
  사용한다. **파라미터/로직 변경은 LIVE 반영 전 PAPER 로 검증할 것.**
- `BithumbApiClient.placeMarketBuyOrder/placeMarketSellOrder` 는 `isLive()` 가 아니면
  실주문(`BithumbPrivateApi`)을 **호출하지 않고** 현재가 기반 인메모리 체결을 반환한다
  (`simulateOrder`).
- 시뮬레이션 체결가 = 현재가 ± 슬리피지(`risk.slippageBuffer`), 수수료 = 체결대금 ×
  `risk.takerFeeRate`. 반환 `BithumbOrderResponse` 는 `trades` 리스트를 채워
  기존 체결가 추출 로직(`extractExecutedPrice`)과 호환된다.
- 검증 전환: `TRADING_BOT_MODE=PAPER`(또는 BACKTEST). env 미설정 시 LIVE.

## Rationale — 왜 이 선택인가

| 대안 | 기각 이유 |
|---|---|
| 모드 없이 `bot.enabled` 만 | 켜면 곧 실거래 — 검증 불가, 사고 위험 |
| 별도 시뮬레이터 프로젝트 | 신호/리스크 로직 중복 → 드리프트 |
| **(선택) API 클라이언트 레벨 게이트** | 신호/리스크 로직은 그대로, 주문 경계만 가드 → 최소 침습 |

감사는 "실거래는 명시적 옵트인"(PAPER 기본)을 권고했으나, 운영자는 기존 LIVE 봇의
동작 연속성을 우선해 LIVE 를 기본으로 유지하기로 결정. 대신 PAPER/BACKTEST 를 언제든
옵트인할 수 있어, 파라미터/로직 변경 검증 경로는 확보된다.

## Consequences — 영향

- **긍정:** 오프라인/페이퍼 검증 가능 → 파라미터·로직 변경을 *측정된 변경* 으로 전환.
  주문 경계 게이트로 시뮬레이션과 실거래가 단일 코드 경로 공유.
- **부정:** 기본 LIVE 이므로, 검증 없이 변경을 LIVE 에 올리는 실수를 *프로세스* 로
  막아야 한다(코드가 강제하지 않음). PR/배포 체크리스트에 "PAPER 검증 완료" 항목 권장.
- **후속:** BACKTEST 모드는 현재 PAPER 와 동일하게 실주문만 차단. 저장 캔들 리플레이
  엔진(과거 구간 전체 시뮬레이션)은 별도 작업으로 분리.
- 지정가(`placeLimit*`) 게이팅은 봇이 limit 주문을 쓰기 시작할 때(쿠폰/슬리피지
  개선, 감사 P1-9) 테스트와 함께 추가.

## References

- 코드: `infrastructure/api/BithumbApiClient.java` (`isLive`/`simulateBuy`/`simulateSell`),
  `infrastructure/config/TradingProperties.java` (`Bot.Mode`)
- 테스트: `BithumbApiClientModeTest`
- 미러: [`stock/modes/0001`](../../stock/modes/0001-paper-backtest-mode-and-clock-bean.md)
