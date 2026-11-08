# ADR-0007 (trading/risk): 일봉 레짐 — 신호 매수 필터로 전환, 리밸런싱 엔진은 기본 OFF + 구멍 3개 봉인

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-10-22 |
| 도메인 | trading (코인) |
| 관심사 | 리스크 / 알고리즘 |
| 관련 ADR | [risk/0002 리밸런싱 회계 정합](0002-rebalance-position-accounting.md), [risk/0003 진입·시간 가드](0003-entry-and-time-risk-guards.md), [strategy/0003 적자 매매 방지](../strategy/0003-loss-prevention-guards.md) 를 정련, [risk/0005 fail-safe 진입 가드](0005-fail-safe-entry-guard-on-price-unavailable.md), [strategy/0009 형성봉 제외](../strategy/0009-exclude-forming-candle.md), [observability/0004 entrySource](../observability/0004-mode-entry-source-excursion.md) |
| 관련 이슈 | 2026-10-20 운영 데이터 진단 P1 (`docs/prompts/trading-fix-2026-10-20.md` 작업 4) |

## Context — 무엇이 문제였나

- **수익 원천은 신호 매수 → 신호 매도뿐이다.** SIGNAL 출구 209건 +49,539원(평균 +1.29%). 누적 청산 540건은 -71,303원이고
  손절 218건이 -133,333원.
- **리밸런싱 레짐 = 현재가 vs 1분봉 MA60(= 60분 평균).** 10/11~10/20 목표 비중이 70% ↔ 30% 로 매번 뒤집혔고 리밸런싱 매수 17회
  (신호 매수 1회) 중 손절 10회, 진입 17건 -4,407원(자산 약 5.8%). 매수 크기는 총자산의 30~70% — 단일 알트에 한 번에 건다.
- 6/5 포지션화(risk/0002) 이후 리밸런싱 포지션도 장중 -1.5% 손절을 받으면서 **손절 루프**가 됐다: 약세 전환 → '수익일 때만 매도'
  가드(strategy/0003·risk/0002)가 손실 중인 70% 를 붙잡음(10/11 밤 `REBALANCE_SKIPPED` 162회) → 손절 → 비중 0 → 편차 초과 →
  쿨다운 뒤 재매수.
- 레짐 정보 자체는 쓸 가치가 있다 — 다만 독립 매수 엔진이 아니라 **신호 매수의 필터**로.

## Decision — 무엇을 골랐나

**4-A. 일봉 레짐 서비스 `DailyRegimeService`**

- 소스: 빗썸 `GET /v1/candles/days` 200개(봉 경계 = KST 자정, 첫 원소 = 형성 중인 당일봉 — 2026-10-22 실측). 당일봉은 판정에서
  뺀다(strategy/0009 정합). 조회 실패 시에만 보관 1분봉(90일)의 날짜별 마지막 종가로 대체.
- 판정(순수 함수 `RegimeCalculator`, 운영·리플레이 공용): 종가 vs SMA(`trading.regime.ma-days` 20). 첫 판정은 종가 ≥ MA 면 BULL.
  이후 MA × (1 ± `hysteresis-pct` 0.02) 밴드를 벗어나야 전환 신호, 현 상태로 지낸 날이 `min-days`(3) 미만이면 전환하지 않는다.
  데이터 부족·조회 실패는 `UNKNOWN`. 진입일 D 에 적용되는 레짐 = D-1 종가까지의 판정(미래 데이터 금지).
- 캐시: 기동 후 첫 조회 때 로드, KST 00:05 이후 하루 1회 갱신(`RegimeScheduler` 00:05 + 조회 시 지연 갱신), 실패하면 직전 판정을
  유지하고 10분 뒤 재시도, 최신 확정봉이 2일보다 오래되면 UNKNOWN. 전환 시 `REGIME_CHANGED` 이벤트 영속(이전·새 상태, 종가, MA —
  payload JSON). `GET /api/trading/bot/status` 에 `regime`.

**4-B. 신호 매수 필터** — `trading.regime.filter-mode: OFF | BLOCK_BEAR | SCALE_BEAR`, **기본 OFF**.

- `BLOCK_BEAR`: BEAR 면 `executeBuy` 의 신호 매수를 막는다. `SCALE_BEAR`: BEAR 면 `calculateDynamicOrderRatio()` × `bear-size-multiplier`(0.5),
  적용 비중이 `trading_trades.order_ratio` 에 그대로 남는다.
- 필터가 켜져 있는데 UNKNOWN 이면 `unknown-policy`(기본 BLOCK, risk/0005 의 fail-safe 원칙) — ALLOW 면 축소 없이 통과.
- **진입에만** 적용한다. 매도 신호·손절·트레일링·시간청산·수동 주문은 무관. 1분봉 MA Trend 점수는 그대로.
- 차단·축소는 `REGIME_FILTER_BLOCKED`/`REGIME_FILTER_SCALED` 이벤트 — 같은 사유는 1시간에 1회로 요약(생략 횟수 표기).

**4-C. 리밸런싱 엔진 — 기본 OFF + 켜졌을 때의 구멍 봉인** (엔진 코드는 비교 대상으로 남긴다)

- `trading.rebalancing.enabled: false`(yaml·Java 기본). 레짐은 `regime-source: DAILY`(기본) 로 4-A 를 쓰고, 1분봉 MA60 은
  `MINUTE_MA60`(레거시) 옵션으로만 남긴다. DAILY 에서 UNKNOWN 이면 `skip-when-data-insufficient` 를 따른다.
- `entrySource=REBALANCE` 포지션은 **엔진이 켜져 있을 때** 장중 SL/TP/트레일링/시간청산 대상이 아니고 재난 대비 비상 손절
  `catastrophic-stop-pct`(-0.15, 수수료차감 손익률)만 받는다 — 사유 `CATASTROPHIC_STOP`(이벤트 CRITICAL). HWM·LWM 관측은 계속.
  엔진이 꺼져 있으면 그 포지션을 관리할 주체가 없으므로 일반 출구를 받는다.
- **구멍 1 — 수익일 때만 매도 가드:** DAILY 에서 리밸런싱 매도는 REBALANCE 포지션부터 FIFO, 다른 레짐에서 산 REBALANCE 포지션
  (진입일의 레짐 ≠ 현재 레짐 = 레짐 전환 매도)은 `min-sell-pnl-pct` 가드를 **무시**한다. 가드는 같은 레짐 안의 편차 매도와 신호 포지션에만.
- **구멍 2 — 비상 손절 후 재매수:** 마지막 `CATASTROPHIC_STOP` 청산(DB) 이후 일봉 레짐이 한 번도 바뀌지 않았으면 리밸런싱 매수를
  막는다. DB 와 레짐 이력으로 판정하므로 재시작 후에도 그대로 복원된다(이력 범위 밖이면 차단하지 않음 — 확인할 수 없는 영구 차단 방지).
- **구멍 3 — 검증 표본 편향:** 재활성화 판정은 최소 2년 일봉 리플레이(작업 6-C)로만 한다 — 보관 1분봉 90일은 ADA 상승기만 담는다.
- 같은 사유의 연속 `REBALANCE_SKIPPED` 는 60분에 1회만 기록(생략 횟수 표기).

**4-D. 결정 기준** (작업 6 리플레이 결과로 운영 `.env` 값을 제안)

- 필터 켜기: 6-B 에서 BEAR 진입분 기대값 < BULL 진입분이고, 필터 적용 시 총손익과 MDD 가 **1/4~6/4 와 6/5 이후 두 구간 모두**에서
  나아질 때만 `BLOCK_BEAR`/`SCALE_BEAR` 중 나은 쪽(`TRADING_REGIME_FILTER_MODE`). 한 구간에서만 나아지면 OFF 유지.
- 리밸런싱 재활성화: 6-C 에서 4-C 규칙이 "KRW 보유"와 "단순 보유"를 위험조정 기준(총수익 ÷ MDD)으로 **모두** 이길 때만.
  (6-C 첫 실행 2026-10-23: [`docs/data-analysis/coin-regime-replay-2026-10-23.md`](../../../data-analysis/coin-regime-replay-2026-10-23.md) —
  하네스는 [observability/0005](../observability/0005-offline-replay-harness.md).)
  필터와 리밸런싱을 둘 다 켜는 조합은 제안하지 않는다(같은 레짐에 이중 노출).

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| A. 리밸런싱을 고쳐 다시 켬 | 레짐 신호를 그대로 활용 | 단일 알트에 자산 30~70% 를 한 번에 거는 구조 자체가 위험 — 데이터상 이익 원천이 아니다 |
| B. 1분봉 MA60 유지 + 쿨다운 연장 | 변경 최소 | 60분 평균은 레짐이 아니라 잡음 — 매번 뒤집혔다 |
| C. 레짐을 필터로 즉시 기본 ON | 약세장 진입 감소 기대 | 효과가 데이터로 확인되지 않았다(기본값 원칙: 결론 난 변경만 기본 동작을 바꾼다) |
| **D. 일봉 레짐 필터(기본 OFF) + 리밸런싱 기본 OFF + 켜졌을 때 손실 경로 봉인 (선택)** | 결정은 리플레이로, 누가 켜도 손절 루프·재진입 루프가 다시 생기지 않음 | 설정 키 증가 |

- 히스테리시스·최소 유지: 밴드가 없으면 MA 근처에서 하루씩 뒤집힌다(1분봉 MA60 의 일봉판). 경로 의존이라 운영과 리플레이가 같은
  함수를 처음부터 순서대로 돌려야 결과가 같다 — 그래서 도메인 순수 함수로 뺐다.
- 구멍 1 에서 신호 포지션의 가드를 유지하는 이유: 신호 포지션은 자체 SL/TP 가 있고, 레짐 전환 매도의 목적은 리밸런싱 엔진이 산 노출을
  줄이는 것이다.

## Consequences — 영향

- **긍정:** 기본 동작에서 리밸런싱 손절 루프가 사라진다(운영은 이미 env 로 정지 — 이제 yaml 기본도 OFF). 누가 엔진을 켜도
  -1.5% 손절 루프·비상 손절 후 재진입 루프가 없다. 레짐이 이벤트·상태 API 로 보인다.
- **부정·위험:** 리밸런싱을 켜면 여전히 **단일 알트에 자산 최대 70% 노출**이고, 비상 손절(-15%)까지는 손실을 들고 간다 — 의도된
  설계(장기 레짐 보유)지만 위험을 명시한다. 일봉 API 하루 1회 호출이 늘었다.
- **후속:** 작업 6-B·6-C 리플레이 결과로 필터 모드·리밸런싱 재활성화를 판정한다.
- 회귀 가드: `RegimeCalculatorTest`(6), `DailyRegimeServiceTest`(6), `TradingBotServiceRegimeFilterTest`(8),
  `RiskManagementServiceRebalanceExitTest`(5), `RebalanceServiceRegimeGuardTest`(8), `TradingRegimePropertiesTest`(2).

## References

- `trading/domain/regime/{MarketRegime, DailyBar, RegimePoint, RegimeCalculator}.java`
- `trading/application/service/{DailyRegimeService, TradingBotService, RiskManagementService, RebalanceService, EventThrottle}.java`
- `trading/infrastructure/scheduler/RegimeScheduler.java`, `trading/infrastructure/api/BithumbPublicApi.java#getDayCandles`
- 진단 근거: `docs/prompts/trading-fix-2026-10-20.md` 데이터 근거 요약
