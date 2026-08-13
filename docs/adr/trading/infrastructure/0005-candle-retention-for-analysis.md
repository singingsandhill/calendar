# ADR-0005: 캔들 보관 기간을 설정으로 빼고 7일 → 90일

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-08-06 |
| 도메인 | trading |
| 관심사 | 인프라 |
| 관련 커밋 | (배치 커밋 시 기입) |
| 관련 ADR | [trading/observability/0001](../observability/0001-signal-quality-analytics-page.md) |

## Context — 무엇이 문제였나

`docs/audit/coin-trading-profit-audit-2026-05-30.md` 는 P0/P1 안전장치와 출구 R:R 재보정을
끝냈지만, 전략 가중치 관련 지적 여러 건을 **"백테스트 없는 prior"** 라며 판단 유보했다. 대표적으로
P2-1(거래량 다이버전스 ±20 과대가중)은 medium → **low 로 강등**된 채 미해결이고,
`docs/trading/remaining-work.md:100` 은 "나머지 P2(전략·구조)·P3는 라이브 전환 이후
**데이터 기반으로**" 라고 적어뒀다.

그런데 그 "데이터 기반" 판단에 필요한 재료가 매일 삭제되고 있었다.

- `CandleService.cleanupOldCandles()` 가 `LocalDateTime.now().minusDays(7)` 를 코드에 박고 있었다.
- 호출자 `CandleScheduler.cleanupOldCandles()` 는 자정 크론이며, 다른 잡과 달리
  `bot.enabled` 가드가 없어 봇을 꺼둔 기간에도 실행된다.
- 결과적으로 1분봉 보관 지평이 **영구히 1주**로 고정됐다. 지표 파라미터를 바꿔 과거에 다시
  돌려보는 것(예: "RSI 기간을 21로 하면?")이 구조적으로 불가능했다.

Bithumb 캔들 API 는 과거 구간을 무한정 주지 않으므로, **삭제된 구간은 복구할 수 없다.**
이 항목만은 결정을 미루는 것 자체가 손실이었다.

## Decision — 무엇을 골랐나

보관 기간을 설정 키로 빼고 기본값을 90일로 올린다.

- `TradingProperties.Bot` 에 `candleRetentionDays = 90` 추가. 새 중첩 클래스를 만들지 않고
  `Bot` 에 둔다 — 이미 market·maxPositions·쿨다운 등 운영 파라미터를 담고 있는 묶음이다.
- `application.yaml` 의 `trading.bot.candle-retention-days: 90` 이 운영값. Java 기본값과
  일치시킨다 (P1-9 의 교훈 — 키가 누락되면 Java 기본값으로 폴백하므로 둘이 어긋나면 안 된다).
- `CandleService.cleanupOldCandles()` 는 이 값을 읽는다.
- `CandleScheduler` 의 `bot.enabled` 가드 부재는 **이번 범위에서 손대지 않는다** (아래 Consequences).

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| 7일 유지 | 저장소 최소 | 지표 재계산·파라미터 리플레이가 불가능. 감사가 남긴 판단 유보를 영원히 못 닫는다 |
| 상수를 90으로만 바꾸기 | 1줄 | 다음에 조정할 때 또 코드를 고쳐야 하고, 값이 코드에만 있어 운영자가 볼 수 없다 |
| 365일 | 리플레이 재료 최대 | 1분봉 × 1시장 ≈ 525,600행(약 105MB). H2 파일이 다루기 번거로워지고, 인덱스가 서지 않는 자정 삭제 풀스캔이 눈에 띄기 시작한다 |
| **90일 + 설정화 (선택)** | 분기 단위 리플레이 재료. 1,440행/일 × 90 ≈ **129,600행**, 행당 약 200B 로 **26MB 미만** — 현재 DB 파일이 643KB 라 지배적 테이블이 되지만 여전히 사소하다 | — |
| 별도 아카이브 스토어로 내보내기 | 무제한 보관 | 1인 운영에 파이프라인 하나를 새로 만드는 비용. 90일로 답할 수 있는 질문이 아직 많이 남아 있다 |

## Consequences — 영향

- **긍정:** 지표 재계산·파라미터 리플레이 지평이 1주 → 1분기. 감사의 P2 항목들을 데이터로
  닫을 수 있는 최소 조건이 생겼다. 보관 기간이 운영자에게 보이는 설정값이 됐다.
- **부정:** H2 파일이 26MB 가량 커진다. `deleteByDateTimeBefore` 는 `candle_date_time` 단독
  조건인데 기존 유니크 인덱스가 `market` 으로 시작해 이 조건에 쓰이지 않으므로 자정 삭제는
  풀스캔이다 — 13만 행에서는 밀리초 단위라 인덱스를 따로 추가하지 않는다.
- **후속:** `CandleScheduler.cleanupOldCandles()` 에 `bot.enabled` 가드가 없다는 점은 그대로
  남는다. 봇을 90일 넘게 꺼두면 캔들 테이블이 서서히 비고, 재가동 시
  `initializeCandles()` 는 200봉만(그것도 `count < 100` 일 때만) 다시 채우므로 그 구간은
  영구 손실이다. 이번 변경으로 실질 위험은 크게 줄었지만 원인은 남아 있다 — 별도 결정으로 다룬다.

## References

- 관련 문서: `docs/audit/coin-trading-profit-audit-2026-05-30.md`, `docs/trading/remaining-work.md:100`
- 관련 코드: `src/main/java/me/singingsandhill/calendar/trading/application/service/CandleService.java:112`,
  `.../infrastructure/config/TradingProperties.java` (`Bot.candleRetentionDays`),
  `src/main/resources/application.yaml` (`trading.bot.candle-retention-days`)
- 관련 테스트: `src/test/java/.../trading/application/service/CandleServiceRetentionTest.java`
