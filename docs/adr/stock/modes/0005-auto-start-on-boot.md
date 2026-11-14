# ADR-0005 (stock/modes): 재시작 후 자동 시작 옵션(기본 OFF) + 코인 서킷브레이커 연속손실 복원

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-10-23 |
| 도메인 | stock, trading (코인 쪽도 이 ADR 에서 함께 다룬다) |
| 관심사 | 모드 / 운영 |
| 관련 ADR | [stock/modes/0003](0003-protection-only-recovery-on-restart.md)·[trading/modes/0003](../../trading/modes/0003-protection-only-recovery-on-restart.md) 보호 전용 복구를 정련, [trading/risk/0001 서킷브레이커](../../trading/risk/0001-circuit-breaker-daily-and-consecutive-loss.md) |
| 관련 이슈 | 2026-10-20 운영 데이터 진단 P1 (`docs/prompts/trading-fix-2026-10-20.md` 작업 8) |

## Context — 무엇이 문제였나

- 10/20 23시경 `.env` 변경 재시작 뒤 주식 봇이 `running:false/STOPPED` 로 떴다 — 10/17 부터 가동 중이었다. 기동 리스너
  `resumeProtectionOnStartup()` 은 오픈 포지션이 있을 때만 보호 전용으로 재개하고(modes/0003), 없으면 그냥 반환한다. 코인
  `TradingBotService` 도 같은 구조다. 재시작(=배포)마다 운영자가 두 봇을 손으로 Start 해야 했다.
- 코인 서킷브레이커의 연속손실 카운터는 인메모리라 재시작하면 0 이 된다. 손실 3연속으로 막혀 있던 신규 매수가 재시작 한 번에 풀린다
  — `RECOVERY_RESUMED` 이벤트 문구가 "카운터 리셋됨" 이라고 적고 있었을 뿐이다.

## Decision — 무엇을 골랐나

- **`stock.bot.auto-start`·`trading.bot.auto-start`** (기본 `false`, env `STOCK_BOT_AUTO_START`·`TRADING_BOT_AUTO_START`). 기동
  (`ApplicationReadyEvent`) 시 봇이 enabled 이고 오픈 포지션이 **없으면** `start()` 로 완전 시작한다. 오픈 포지션이 **있으면** 종전대로
  보호 전용 복구가 이긴다(신규 진입은 관리자 Start) — 충돌 시 복구 우선. 이벤트 `BOT_AUTO_STARTED`(주식 TradeEvents, 코인 이벤트 원장).
- **코인 서킷브레이커 복원:** 같은 리스너가 먼저 당일(JVM 날짜, 일일 손실 가드 `realizedPnlToday` 와 같은 창) 청산 포지션 중 **현재 봇
  모드** 것(observability/0004 의 `mode`, NULL = LIVE)을 청산 시각 순으로 `TradingCircuitBreaker.restoreFrom` 에 넘긴다 —
  `recordOutcome` 과 같은 규칙(손실 +1, 그 외 0)으로 처음부터 다시 센다. 복원 값은 `RECOVERY_RESUMED`·`BOT_AUTO_STARTED` 문구에
  "연속손실 N회 복원(당일 청산 이력)" 으로 남는다. 봇이 enabled 면 auto-start 와 무관하게 항상 복원한다(나중의 수동 Start 도 같은 카운터).

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| 항상 자동 시작 | 운영 편함 | LIVE 에서 의도치 않은 재가동 위험 — 기본은 종전 동작(OFF), 운영자가 env 로 켠다 |
| 포지션이 있어도 auto-start 면 완전 시작 | 단순 | 재시작 직후 상태(미결 주문·잔고)를 운영자가 보기 전에 신규 매매가 나간다 — modes/0003 의 판단 유지 |
| 카운터를 DB 컬럼으로 영속 | 정확 | 청산 이력에서 다시 셀 수 있는 값이라 상태를 하나 더 두지 않는다 |
| 전체 이력으로 연속손실 복원 | 자정을 넘긴 스트릭까지 | 작업 정의가 "당일" — 일일 손실 가드와 같은 창으로 맞춘다(아래 한계) |

## Consequences — 영향

- **긍정:** `*_AUTO_START=true` 면 배포 뒤 Start 클릭이 필요 없다(포지션 보유 시는 여전히 보호 전용). 재시작으로 서킷브레이커가 풀리지 않는다.
- **한계:** 인메모리 카운터 자체는 날짜로 리셋되지 않으므로, 자정을 넘긴 연속손실(예: 23시 3연속 → 00시 재시작)은 복원되지 않는다.
  모드가 하루 중 바뀌면 바뀐 모드의 청산만 센다.
- **운영:** env 미설정이면 동작 불변. 켜려면 서버 `.env` 에 `TRADING_BOT_AUTO_START=true`·`STOCK_BOT_AUTO_START=true` 후 재시작.
  주식 봇은 KIS 미설정이면 자동 시작이 실패하고(`started=false` 이벤트) 정지 상태로 남는다.

## References

- 코드: `TradingBotService.resumeProtectionOnStartup`·`restoreCircuitBreaker`, `TradingCircuitBreaker.restoreFrom`,
  `GapPullbackBotService.resumeProtectionOnStartup`, `TradingProperties.Bot.autoStart`, `StockProperties.Bot.autoStart`
- 테스트: `TradingBotAutoStartTest`(5), `TradingCircuitBreakerTest.restoreReplaysOutcomesInCloseOrder`,
  `GapPullbackBotRecoveryTest`(auto-start 4)
- 운영: `docs/operations/deployment.md` 배포 후 확인 절차
