# ADR-0002: 결정 입력을 결정 시점에 영속화한다 (signal_id · ATR · 거래량 맥락 · 적용 주문비중)

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-08-06 |
| 도메인 | trading |
| 관심사 | 관측성 |
| 관련 커밋 | (배치 커밋 시 기입) |
| 관련 ADR | [trading/observability/0001](0001-signal-quality-analytics-page.md), [trading/risk/0003](../risk/0003-position-risk-guards.md) |

## Context — 무엇이 문제였나

`trading_signals` 는 결정 입력이 풍부한데, **결과 쪽에서 원인을 되짚을 방법이 없었다.**

- `trading_trades` 에 남는 신호 정보는 `signal_score` 정수 하나와 `signal_reason` 문자열뿐이다.
  Trade/Position 어디에도 `signal_id` 가 없다.
- 반대 방향도 막혀 있었다. `trading_signals.executed` 는 `Signal.markExecuted()` 가
  **저장소 전체에서 한 번도 호출되지 않아** 항상 `false` 인 죽은 컬럼이다.
- 그래서 "이 체결이 어느 신호에서 나왔는가" 는 타임스탬프 근사로만 추정할 수 있었다.

계산해놓고 버리는 값도 있었다.

- **ATR** — `calculateDynamicOrderRatio` 가 주문 크기를 정하는 데 쓰지만 어디에도 저장하지 않는다.
  **실제 적용된 주문 비중**도 마찬가지다. 즉 포지션 사이징 결정을 DB 로 재구성할 수 없었다.
- **`volumeMa` / `currentVolume`** — `IndicatorResult` 에는 있는데 `SignalJpaEntity` 에는 컬럼이 없다.
  이 둘은 MA60 하회 시 매수 확인 게이트의 거래량 스파이크 분기 입력이라,
  없으면 "임계를 올렸다면 몇 건이 통과했겠는가" 를 **판정할 수 없다**.

## Decision — 무엇을 골랐나

결정에 쓰인 값은 그 결정 시점에 남긴다.

- `trading_trades` 에 `signal_id`(nullable) 추가. **신호 기반 매수·매도에서만 채운다.**
  리스크 청산·리밸런싱·수동 매매는 신호가 원인이 아니므로 **일부러 null** 로 둔다 —
  null 자체가 "신호로 난 체결이 아니다" 라는 정보다.
- `trading_trades` 에 `order_ratio` 추가. 매수에만 채우며, 저장 전 4자리로 반올림한다
  (보간 결과가 double 이라 `0.29999999999999993` 같은 값이 그대로 들어간다).
- `trading_signals` 에 `atr` / `atr_percent` / `volume_ma` / `current_volume` 추가.
  ATR 은 `IndicatorService.calculate()` 가 **이미 로드한 캔들**로 계산한다 —
  `calculateATRPercent(market)` 를 부르면 같은 틱에 캔들을 두 번 읽는다.
- **주문 사이징이 쓰는 `calculateATRPercent(market)` 는 건드리지 않는다.** 그쪽은 자체적으로
  캔들을 다시 읽고 `excludeFormingCandle` 을 따르지 않아, 그 플래그를 켜면 두 값이 갈릴 수 있다.
  그래서 재유도하지 않고 **실제 적용된 비중을 따로 기록**한다.

## Rationale — 왜 이 선택인가

**필드 추가 형태 — 생성자가 아니라 setter.**

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| `Signal` 생성자에 4개 인자 추가 | 불변성 유지 | 이미 25인자다. 29인자 위치 호출에서 인접한 `BigDecimal` 둘이 뒤바뀌어도 **컴파일이 통과하고 그 뒤 모든 행이 조용히 오염된다.** 이 계획 전체에서 가장 위험한 실패 모드 |
| `Trade` 의 4개 정적 팩토리에 인자 추가 | 일관성 | 모든 생성 지점이 바뀐다. 선택적 값 하나 때문에 `createSubmittedBuy`/`createSellOrder` 등 전부를 넓히는 건 과하다 |
| **가변 필드 + 이름 있는 setter (선택)** | 호출부가 읽힌다. 같은 클래스의 `id`·`executed`(Signal), `positionId`·`signalScore`(Trade)가 이미 가변이라 선례가 있다 | 불변성이 약해진다 — 다섯 번째 필드가 필요해지면 그때는 `ScoreBreakdown`/`IndicatorSnapshot` 중첩 레코드로 실제 분해를 한다 |

**리스크 청산에 신호를 연결하지 않는 이유.** [observability/0003](0003-signal-series-continuity-on-risk-exit.md)
이후에는 청산이 일어난 분에도 신호 행이 생긴다. 그 신호를 청산 Trade 의 `signal_id` 로 붙이고
싶어질 수 있는데, 그건 "이 신호가 이 청산을 유발했다" 는 **거짓**을 기록하는 것이고
`signal_id` 기반 집계를 전부 오염시킨다. 청산 시점의 지표 상태는 별도 관계(타임스탬프)로만 본다.

**스키마 적용.** `ddl-auto: update` 는 JDBC 메타데이터에 없는 컬럼에 `ALTER TABLE ... ADD COLUMN`
을 발행하며 절대 drop/retype 하지 않는다. 추가 컬럼은 전부 nullable 이어야 하고(기존 행 때문에),
실제로 그렇다. 마이그레이션 스크립트는 이 저장소에 없다.

## Consequences — 영향

- **긍정:** 신호↔체결이 직접 조인된다. 포지션 사이징 결정이 사후 재구성 가능해졌다.
  거래량 맥락이 생기면서 임계 반사실이 "범위" 가 아닌 단일 값으로 수렴한다.
- **부정:** `Signal`·`Trade` 의 불변성이 조금 더 약해졌다. 새 컬럼 6개는 이 ADR 이전 행에서
  전부 null 이라, 분석 페이지는 그 구간을 "미측정" 으로 분류해야 한다(그렇게 하고 있다).
- **후속:** `signal_id` 는 인덱스를 만들지 않았다 — 분석은 포지션→신호 방향으로만 조인한다.
  반대 방향 쿼리가 생기면 그때 추가한다.

## References

- 관련 코드: `src/main/java/.../trading/domain/trade/Trade.java`, `.../domain/signal/Signal.java`,
  `.../application/service/TradingBotService.java` (executeBuy·executeSell 의 4개 생성 지점),
  `.../application/service/SignalService.java`, `.../application/dto/IndicatorResult.java`
- 관련 테스트: `src/test/java/.../trading/application/service/TradingBotServiceSignalLinkTest.java`
