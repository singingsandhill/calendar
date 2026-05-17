# ADR-0001: TradeEvents 로거 / KST 자정 회전 / BotStatus 메트릭

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-05-01 |
| 도메인 | stock |
| 관심사 | 관측성 (Observability) |
| 관련 커밋 | `9f1a960`, `d135907` |
| 관련 이슈 | — |

## Context — 무엇이 문제였나

운영 중 가장 자주 묻는 질문이 두 가지였다.

1. **"봇이 죽었나, 후보가 없나?"** — 09:20 스크리닝이 끝나도 거래가 안 일어나면
   원인이 *루프 자체가 멈췄는지* / *유니버스가 비어 있는지* / *KIS API 가 막혔는지*
   구분할 정보가 없었다.
2. **"방금 무슨 종목을 어느 시점에 어떤 이유로 진입/청산했나?"** — 거래 흐름이
   일반 애플리케이션 로그(Hibernate SQL, Spring 부트스트랩, 헬스체크) 사이에
   섞여 grep 으로 추적이 어려웠다.

KIS API 호출량 한도(분당/초당)에 근접하는지 확인할 수 있는 카운터도 없었다.

## Decision — 무엇을 골랐나

거래 이벤트 전용 로깅 채널과 핵심 운영 메트릭을 BotStatus 응답에 노출.

- **`TradeEvents`** — `stock.trade` 카테고리 전용 logger. `event=NAME key=value`
  한 줄 포맷. MDC 키로 `phase`, `tradingDate`, `stock.code`, `stock.tradeId` 헬퍼 제공.
- **logback-spring.xml** — KST 자정 회전 정책. 거래 이벤트는 `stock-events.log`,
  Hibernate SQL 은 `stock-sql.log` 로 분리하여 일반 애플리케이션 로그와 격리.
- **`StockBotMetrics`** — 슬라이딩 윈도우 카운터 `apiCallsLast5min`, 마지막 트레이딩
  틱 시각 `lastTradingTickAt`, 스크리닝 결과 스냅샷 `lastScreeningResult`.
- **`BotStatus`** 응답 — 위 세 메트릭을 노출 (`d135907` 보강 커밋에서 추가).

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| 일반 logger 에 INFO 로 그대로 두기 | 추가 작업 0 | 거래 이벤트가 SQL/health-check 로그 사이에 매몰. grep 부담. |
| 외부 APM (Datadog, NewRelic) | 풀 스택 가시성 | 단일 인스턴스 + 비용. 자체 운영 단계에선 과다. |
| **(선택) 전용 카테고리 + appender 분리 + BotStatus JSON 노출** | 파일 grep + HTTP 폴링 둘 다 가능, 비용 0 | — |

`event=NAME key=value` 한 줄 포맷은 의도적: 정규식/awk 로 손쉽게 파싱하면서도
사람이 읽기 좋다. JSON 으로 가면 파일 grep 시 줄바꿈 문제가 생긴다.

KST 자정 회전은 거래일 단위 분석을 위해 필수. UTC 회전이면 한국 거래시간(09–11시)이
두 파일에 걸쳐 잘려나간다.

## Consequences — 영향

- **긍정:**
  - `tail -f stock-events.log | grep ENTRY_FILLED` 같은 운영 동작이 가능.
  - `/api/stock/bot/status` 한 번 호출로 "봇이 살아있는지 / 후보를 보고 있는지 /
    KIS 호출이 어디까지 갔는지" 동시 판단.
  - 분당 100 회 이상이면 `apiCallsLast5min` 으로 사전 인지.
- **부정:**
  - logback 설정 복잡도 증가 (3개의 RollingFileAppender + 카테고리 별 라우팅).
  - MDC 키를 잊으면 phase/tradingDate 가 비어 출력 — `TradeEvents` 헬퍼로 강제.
- **후속:**
  - ADR-0002 (UniverseBuilder) 가 빈 유니버스 시 `SCREENING_SKIPPED` 이벤트를 emit
    하는 것이 이 ADR 의 채널을 전제로 함.
  - ADR-0001 (KIS rate-limit Semaphore) 가 `apiCallsLast5min` 카운터에 기록.

## References

- 관련 코드:
  - `src/main/java/me/singingsandhill/calendar/stock/observability/TradeEvents.java`
  - `src/main/java/me/singingsandhill/calendar/stock/observability/StockBotMetrics.java`
  - `src/main/resources/logback-spring.xml`
  - `src/main/java/me/singingsandhill/calendar/stock/presentation/dto/BotStatus.java`
- 관련 ADR: [stock/algorithm/0002 UniverseBuilder](../algorithm/0002-universe-builder-snapshot.md), [stock/infrastructure/0001 KIS Semaphore](../infrastructure/0001-kis-rate-limit-semaphore.md)
- 관련 커밋: `git log -1 9f1a960`, `git log -1 d135907`
