# ADR-0011 (trading/strategy): 출구 틱 하한(기본 ON) + 손절 방식·시간청산 모드 설정화(기본 = 현행)

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-10-22 |
| 도메인 | trading (코인) |
| 관심사 | 알고리즘 / 리스크 |
| 관련 ADR | [strategy/0005 출구 R:R 재보정](0005-exit-risk-reward-recalibration.md) 에 하한을 더함(값은 그대로), [risk/0003 진입·시간 가드](../risk/0003-entry-and-time-risk-guards.md) 의 시간청산을 모드로 정련, [risk/0007 일봉 레짐](../risk/0007-daily-regime-filter-and-rebalance-guards.md)(REBALANCE 포지션 출구는 그쪽) |
| 관련 이슈 | 2026-10-20 운영 데이터 진단 P1 (`docs/prompts/trading-fix-2026-10-20.md` 작업 5) |

## Context — 무엇이 문제였나

- **7월 이후(현 리스크 설정) 245건 -8,892원**, 승률 45.7%, 평균 이익 +1.40% / 평균 손실 -1.66%(비율 0.84 → 손익분기 승률 54%).
  청산 사유: 손절 124건(평균 -1.77%, 보유 4.1시간) · 시간청산 74건(+0.60%, 7.1시간) · 트레일링 24건(+0.97%) · TP 14건(+3.19%) ·
  신호 매도 8건(+5.37%). 가장 좋은 출구(신호 매도)는 다른 출구가 먼저 걸려 8건뿐이다.
- **임계값이 호가 노이즈 수준이었다.** KRW-ADA ≈ 290원에서 호가단위 1원 = 0.34%. 트레일링 0.8% = 2.4틱(정수 내림이라 실제로는
  고점 대비 3원), 손절 1.5% = 4.4틱. 호가 스프레드 자체가 최소 1틱이라 2~3틱 되돌림은 추세가 아니라 호가 깜빡임이다.
- 손절·트레일링 값은 ADR strategy/0005 가 "고정 권장값으로 시작, 백테스트 후 ATR 화 검토" 로 남겼고, 시간청산(risk/0003)은 이익
  포지션을 6시간에 +0.6% 로 끊는다. 이 값들을 정할 근거(리플레이, 작업 6-A)는 아직 없다.
- 진단 문서의 "시간청산 대칭화(손실에도 적용)"는 효과가 작다 — 손절이 평균 4.1시간에 이미 걸려 6시간 maxHold 에 닿는 손실
  포지션이 적다.

## Decision — 무엇을 골랐나

틱 하한은 최적화가 아니라 "노이즈 미만 임계값 금지" 하한이라 **기본 ON**. 결론이 안 난 전략 키는 **기본 = 현재 동작**이고 값은
리플레이(작업 6-A)로 정한다.

- **빗썸 KRW 호가단위 `BithumbTickSize`** (domain/trade) — 1 미만 0.0001 · 10 미만 0.001 · 100 미만 0.01 · 5,000 미만 1 ·
  10,000 미만 5 · 50,000 미만 10 · 100,000 미만 50 · 500,000 미만 100 · 1,000,000 미만 500 · 이상 1,000. 경계 가격은 위 구간.
  2026-10-22 공개 호가창 실측(ADA 290/289 = 1원 등)과 고객지원 원화 마켓 거래 정책을 대조했다.
- **틱 하한 `ExitThresholds`** (domain/position, 리스크 루프·리플레이 공용 순수 함수):
  - `trading.risk.min-stop-ticks: 5` — 유효 손절 = min(손절, -5틱/진입가). 손절과 같은 단위(수수료차감 손익률)로 비교한다.
  - `trading.risk.min-trailing-ticks: 5` — 트레일링 스탑 = HWM - max(HWM × trailing-stop, 5틱). 가격 공간에서 계산해 호가 격자로
    내림(비율을 곱하면 정확히 격자 위인 값이 부동소수 오차로 한 틱 아래로 떨어질 수 있다). 손익분기 floor 는 격자 올림.
  - 0 이면 끈다. 진입 시 저장하는 표시용 손절가(`calculateStopLossPrice`)도 같은 유효 손절을 쓴다.
- **손절 방식 `trading.risk.stop-mode: PERCENT | ATR`** (기본 PERCENT) + `atr-period`(14) · `atr-multiplier`(2.0) ·
  `atr-bar-minutes`(15). ATR = 손절 -배수 × N분 집계봉 ATR%(`IndicatorService.calculateAggregatedATRPercent` — 최신 1분봉부터
  N개씩 묶음, 형성 중 봉 포함). 계산 불가(캔들 부족)면 PERCENT 로 대체하고 WARN. 틱 하한은 두 방식 모두 적용.
- **시간청산 모드 `trading.bot.time-exit-mode`** — maxHold 경과 + 수수료차감 손익 ≥ 0 인 포지션을:
  - `BREAKEVEN_CLOSE`(기본, 현행): 즉시 `TIME_EXIT` 청산.
  - `TIGHTEN_TRAIL`: 청산 대신 트레일링을 즉시 활성화(활성화 조건 +1.5% 의 대안 트리거). 스탑 = max(손익분기, HWM - 트레일링 폭),
    청산되면 사유는 `TRAILING_STOP`. 현재가가 이미 손익분기가 아래면 같은 틱에 청산된다.
  - `SIGNAL_GATED`: 최신 신호 `totalScore` ≤ 0 일 때만 `TIME_EXIT`. 신호가 없으면 판정 근거가 없으므로 현행대로 청산.
- **손실 시간청산 `trading.bot.time-exit-loss-threshold`** (기본 null = 비활성, 음수 비율 예 -0.005) — maxHold 경과 + 손익률 ≥
  그 값인 작은 손실만 `TIME_EXIT`. 더 큰 손실은 손절에 맡긴다. 모드와 무관.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| 값 자체를 넓힘 (트레일링 1.7%, 손절 -1.75%) | 단순 | 가격대가 바뀌면 다시 틀어진다(ADA 1,000원이면 1틱 = 0.1%). 값 결정은 리플레이 몫 |
| **틱 하한 (선택)** | 가격대에 자동 적응, 설정값이 더 넓으면 무영향 | 290원 대에서는 LIVE 동작이 넓어지는 쪽으로 바뀐다(아래) |
| 하한을 가격 하락 5틱으로 (수수료 무시) | 정확히 5틱 | 기존 손절이 수수료차감 손익률이라 단위가 섞인다. 작업 정의(min(stop%, -5틱/가격)) 그대로 둠 |
| ATR 손절 기본 ON | 변동성 적응 | 데이터 결론 없음 → 옵션(기본 PERCENT) |
| 손실에도 시간청산 기본 적용 | 대칭 | 6시간에 닿는 손실 포지션이 적어 효과 작음, 손절 정책과 충돌(risk/0003) → 옵션(기본 null) |

## Consequences — 영향

- **LIVE 동작 변경 (290원 대, 운영 yaml 기준):** 손절 판정 -1.5% → -1.72%. 수수료차감 손익률이라 가격 하락으로는 4틱 → 5틱에서
  걸린다(진입 수수료 실측 0.04% + 청산 수수료 추정 `taker-fee-rate` 0.25% 기준). 트레일링은 고점 대비 3원 → 5원. 손절 1건당
  손실이 약 1틱(0.34%p) 커지고 트레일링 이익 반납이 늘 수 있다 — 승률·출구 분포 변화는 리플레이로만 확인된다. 끄려면 두 키를 0.
- **긍정:** 노이즈 손절·트레일링 감소 기대. 100원 미만 코인에서 정수 내림이 스탑을 여러 틱 끌어내리던 문제 제거(격자 내림 —
  예: 72.55원 트레일링 71.97 → 정수 내림 71, 1.3%).
  리플레이(작업 6-A)가 같은 `ExitThresholds`·`BithumbTickSize` 를 써서 운영 판정과 어긋나지 않는다.
- **부정:** 설정 키 8개(`risk.*` 6, `bot.*` 2). `RiskManagementService` 가 `IndicatorService`(ATR 모드)·`SignalRepository`
  (SIGNAL_GATED)에 의존. 설정 화면(`/trading/settings`)은 유효값이 아니라 설정값을 보여준다 — 유효 손절은 손절 로그의
  `(stop: x%)` 와 포지션 `stopLossPrice`/`trailingStopPrice` 로 본다(설정 화면 동기화는 P2 범위 밖).
- **후속:** 리플레이 6-A 그리드(트레일링 {0.8, 1.2, 1.7, 2.5}% × 활성화 {1.5, 2.0}% × 시간청산 모드 3종 × maxHold {360, 720,
  1440}분 × 틱 하한 {on, off})로 값을 정한다. 보정 게이트(실제 7~10월 재현)를 통과하지 못하면 이 기본값을 유지한다.

## References

- 코드: `trading/domain/trade/BithumbTickSize`, `trading/domain/position/ExitThresholds`,
  `RiskManagementService.checkPositionRisk`·`shouldTimeExit`·`calculateStopLossPrice`,
  `IndicatorService.calculateAggregatedATRPercent`, `TradingProperties.Risk`(min-*-ticks, stop-mode, atr-*)·`Bot`(time-exit-*)
- 테스트: `BithumbTickSizeTest`, `ExitThresholdsTest`, `RiskManagementServiceExitRulesTest`, `IndicatorServiceAggregatedAtrTest`,
  `TradingExitPropertiesTest`
- 데이터: `docs/prompts/trading-fix-2026-10-20.md` "데이터 근거 요약"(2026-10-20 운영 API 조회)
