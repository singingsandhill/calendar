# ADR-0003: 재시작 시 보호 전용 자동 재개 + 최종청산 재시도·알림

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-07-24 |
| 도메인 | stock |
| 관심사 | 모드 / 운영 안전 |
| 관련 ADR | [stock/modes/0002](0002-paper-default-mode.md), [common/security/0005](../../common/security/0005-admin-only-stock-bot-control-api.md) |
| 관련 이슈 | 주식 봇 로직 리뷰 §6 (P1-5, P1-6) |

## Context — 무엇이 문제였나

- **재시작 = 보호 상실**: `running` 이 인메모리 `AtomicBoolean(false)` 이고 자동 기동이 없어,
  오픈 포지션을 들고 있는 상태에서 재배포하면 손절·트레일링·11:20 강제청산이 **전부 멈춘다**.
  관리자가 수동 start 할 때까지 포지션은 무방비다.
- **최종청산 원샷**: 11:20 청산은 cron 1회뿐이고 루프 가드도 11:20 에 닫힌다. 시세 조회 실패는
  로그조차 없이 스킵됐고, 매도 거부 시 재시도·알림이 없어 **의도치 않은 오버나이트 홀드**가
  조용히 발생할 수 있었다.

## Decision — 무엇을 골랐나

**(1) 보호 전용 복구 모드** (사용자 선택):
`@EventListener(ApplicationReadyEvent)` 로 기동 시 `bot.enabled` 이고 당일 오픈 포지션이
있으면 `running=true, recoveryMode=true` 로 자동 재개한다.

| 동작 | 복구 모드 |
|---|---|
| 리스크 체크(손절·익절·트레일링) | 실행 |
| 상태머신 갱신 · 미확인 주문 스윕 | 실행 |
| 11:20 강제청산 | 실행 |
| **신규 진입(`executeEntries`)** | **차단** |

관리자가 명시적으로 `start()` 하면 `recoveryMode` 가 해제되어 완전 재개된다.
`BotStatus.recoveryMode` 로 대시보드/API 에 노출하고 기동 시 WARN + `RECOVERY_RESUMED` 이벤트.

부수 효과로 `GapPullbackBotService` 가 `Clock` 을 주입받아(기존 `stockClock` 빈 재사용)
거래창 가드가 `Clock.fixed` 로 결정성 테스트 가능해졌다.

**(2) 최종청산 재시도 + 알림**: 종목당 최대 3회 시도(시세 조회 실패·매도 미완료 모두 재시도),
최종 실패 종목은 ERROR 로그 + `TIME_EXIT_FAILED` 이벤트 + **메일 알림**
(`StockMailService.sendTimeExitFailureAlert`) 으로 수동 청산을 요청한다.
이를 위해 `StockPositionService.closePosition` 이 `void` → `boolean`(전량 청산 성공 여부)로
바뀌었다 — 매도 주문 거부 시 조용히 성공처럼 보이던 문제를 없앤다.

## Rationale

| 대안 | 기각 이유 |
|---|---|
| 완전 자동 재개(진입 포함) | 재배포가 매매 판단에 무개입이어야 한다는 원칙에 어긋남 — 사용자 선택도 보호 전용 |
| 봇 상태를 DB 영속화 후 복원 | 더 정확하나 스키마·수명주기 복잡도 증가. 오픈 포지션 유무로 충분 |
| 청산 실패를 다음 틱 루프에 위임 | 루프 가드가 11:20 에 닫혀 재시도 창이 없음 |

## Consequences

- 재배포 후에도 포지션 보호가 유지된다. 단 **신규 진입은 관리자가 start 해야 재개**되므로,
  장중 재배포 시 진입 기회를 놓칠 수 있다(의도된 트레이드오프).
- 메일 미설정 환경에서는 알림이 WARN 로그로만 남는다.
- 회귀 가드: `GapPullbackBotRecoveryTest`(5), `StockRiskServiceTimeExitTest`(4).

## References

- `stock/application/service/GapPullbackBotService.java` (`resumeProtectionOnStartup`, `recoveryMode`)
- `stock/application/service/StockRiskService.java` (`closeWithRetry`)
- `stock/application/service/StockMailService.java` (`sendTimeExitFailureAlert`)
