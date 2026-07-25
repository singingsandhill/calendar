# ADR-0002: 일일 실적 요약 리포트 (PAPER 실측 데이터원)

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-07-24 |
| 도메인 | stock |
| 관심사 | 관측성 |
| 관련 ADR | [stock/observability/0001](0001-trade-events-logger-and-bot-metrics.md), [stock/modes/0002](../modes/0002-paper-default-mode.md) (PAPER 기본), [stock/algorithm/0007](../algorithm/0007-exit-structure-recalibration.md) |
| 관련 이슈 | 주식 봇 로직 리뷰 §9 (P2-5) |

## Context — 무엇이 문제였나

리뷰의 최종 권고(P2-5)는 "**PAPER 2~4주 실측 후에만 LIVE 논의**"다. 그런데 실측을 하려면
매일의 진입·청산·손익·거절 사유가 한 곳에 집계돼야 하는데, 기존 관측 수단은
`TradeEvents`(이벤트 단위 로그)와 DB 테이블뿐이라 **사람이 매번 질의해야** 했다.
청산 구조를 크게 바꾼 뒤(ADR-0007) 손절 빈도·승률이 어떻게 변하는지 추적할 수단이 없으면
LIVE 전환 판단이 다시 감에 의존하게 된다.

## Decision — 무엇을 골랐나

`DailyPerformanceReportService` 신설 — 최종청산(11:20) 이후 **11:40 cron**(평일, 휴일 제외)에
당일 실적을 집계한다.

| 집계 항목 | 내용 |
|---|---|
| 청산/미청산 건수 | `findByTradingDate` 기준 |
| 승·패 건수 및 승률 | 실현손익 부호 기준 (수수료·세금 반영값) |
| 실현손익 합계 · 종목별 손익 | |
| 청산 사유 분포 | TP1/TP2/TP3/STOP_LOSS/TRAILING/TIME_EXIT/… |
| 진입 거절 사유 분포 | `EntryAttempt.rejectReason` — 어떤 조건이 병목인지 |

출력은 **메일 + `DAILY_REPORT` TradeEvents 이벤트 + INFO 로그** 3중.
**매매도 진입 시도도 없던 날은 메일을 생략**한다(알림 피로 방지) — 이벤트·로그는 남는다.

## Rationale

- 이벤트 로그(`stock-events.log`)만으로도 사후 집계는 가능하나, 매일 자동으로 요약이 오면
  이상 징후(승률 급락, 특정 거절 사유 폭증)를 즉시 알아챌 수 있다.
- 11:40 은 최종청산(11:20) 이후로 실현손익이 확정된 시점이며, 스크리닝 메일(09:20)과
  겹치지 않는다.
- 별도 배치/대시보드 대신 기존 `StockMailService` 를 재사용해 인프라 추가가 없다.

## Consequences

- PAPER 운영 2~4주 후 리포트를 누적하면 승률·기대값·거절 병목이 데이터로 남는다 →
  LIVE 전환(P2-5) 판단 근거.
- 메일 미설정 환경에서는 로그·이벤트로만 남는다(동작 불변).
- 회귀 가드: `DailyPerformanceReportServiceTest`(4 — 승률 집계, 사유 그룹핑, 발송/생략).

## References

- `stock/application/service/DailyPerformanceReportService.java`
- `stock/application/service/StockMailService.java` (`sendDailyPerformanceReport`)
- `stock/infrastructure/scheduler/StockTradingScheduler.java` (11:40 cron)
