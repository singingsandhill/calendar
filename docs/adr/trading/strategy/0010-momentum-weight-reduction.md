# ADR-0010 (trading/strategy): 모멘텀 가중 하향 (MA Trend ±15→±8, MA State ±10→±5)

| 항목 | 값 |
|---|---|
| 상태 | Accepted (ADR-0001 스코어링 정련) |
| 날짜 | 2026-05-30 |
| 도메인 | trading (코인) |
| 관심사 | 알고리즘 / 스코어링 가중 |
| 관련 | 수익성 감사 `docs/audit/coin-trading-profit-audit-2026-05-30.md` (P2-7), [ADR-0001 8지표 컨센서스](0001-multi-indicator-consensus.md) 정련 |

## Context — 무엇이 문제였나

ADR-0001 의 ±135 컨센서스에서 **모멘텀 컴포넌트**(MA Cross event ±25, MA Trend ±15, MA
State ±10)가 **평균회귀 컴포넌트**(RSI Level ±15, Stoch Level ±15)와 ±40 매수/매도 임계
근처에서 **상쇄**되는 경향. 특히 ADA 횡보(평균회귀) 레짐에서 *과매도 + 추세 하회(price <
MA60)* 인 딥에서 모멘텀 음수 점수가 과매도 양수 점수를 깎아 매수 임계 도달을 억제 →
의도된 평균회귀 딥 매수 엣지 미실현. (감사 평가: medium/illustrative.)

## Decision — 무엇을 골랐나

모멘텀 가중을 낮춘다 (`SignalService`):

- **MA Trend ±15 → ±8**
- **MA State ±10 → ±5** (MA Cross *event* ±25 불변, MA 수렴 억제 불변)
- 매수/매도 임계 ±40 불변.

새 합산 범위: **±128**(cross event 시) = 25 + 8 + 20 + 15 + 15 + 15 + 20 + 10.
ADR-0001 은 역사적 기록으로 유지하고 본 ADR 이 가중치 부분을 정련(supersede 아님).

## Rationale — 왜 이 선택인가

| 대안 | 기각 이유 |
|---|---|
| 임계만 낮춤(±40→±30) | 모든 신호 빈도↑ — 모멘텀 상쇄 문제 자체는 그대로 |
| |price−MA60| > 1% 일 때만 모멘텀 페널티 | 더 정교하나 분기 복잡 — 우선 가중 하향으로 단순 적용 |
| **(선택) 모멘텀 가중 하향** | 평균회귀 신호가 임계에 더 쉽게 도달, 최소 변경 |

## Consequences — 영향

- **긍정:** 억눌렸던 과매도 딥 진입 회복(평균회귀 엣지 실현). below-MA60 확인 게이트
  (`SignalService` Issue #9)와 함께 딥 매수 품질 유지.
- **트레이드오프:** 추세장에서 모멘텀 신호 비중↓ → 추세 추종 약화. ADA 횡보 레짐 가정.
- **주의:** 컨센서스 스코어링 분포가 바뀌므로 **LIVE 전 PAPER 백테스트 필수**. 회귀
  테스트로 oversold-below-MA60 + 1다이버전스 조합이 BUY 도달하는지 확인 권장.
- `application/CLAUDE.md` 의 ±135 표 → ±128 갱신.

## References

- 코드: `application/service/SignalService.java` (`calculateMaTrendScore`/`calculateMaCrossScore`)
- 테스트: `SignalServiceWeightTest`
- 정련 대상: [ADR-0001](0001-multi-indicator-consensus.md)
