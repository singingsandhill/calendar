# ADR-0001: 8개 지표 컨센서스 채점 (±135점)

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-01-01 (초기 구현) ~ 2026-04-16 (고도화) |
| 도메인 | trading (코인) |
| 관심사 | 알고리즘 |
| 관련 커밋 | `cbcd9e0`, `b09a467` |
| 관련 이슈 | #17 |

## Context — 무엇이 문제였나

코인 시장(KRW-ADA) 은 단일 지표(예: RSI, MACD) 기반 매매가 *횡보장 / 추세 전환점*
에서 신호 오류가 잦다. "하나의 지표는 거짓말할 수 있다" 라는 운영 가설 아래, 다수
지표의 일치도(consensus) 를 점수로 합산하는 방식 채택 필요.

## Decision — 무엇을 골랐나

8개 지표 점수 합산으로 매수/매도 결정 — 합산 ±135점 범위, 임계값 도달 시 행동.

- **점수 구성요소 8종 (`SignalService`):** MA Cross 또는 State (±25 / ±10, 둘 중 하나만),
  MA Trend (±15), RSI Divergence (±20), RSI Level (±15), Stoch Divergence (±15),
  Stoch Level (±15), Volume Divergence (±20), RSI Trend (±10).
- **점수 범위 ±135점** = 25+15+20+15+15+15+20+10. 강한 신호일수록 절대값 ↑.
- **MA 크로스 채점 정책:** 이벤트(±25) OR 상태(±10) 중 하나만, 둘 동시 부여 X
  (이중 카운팅 회피).
- **다이버전스 검출 (`DivergenceService`):** 가격 극값 vs 지표 극값 in 20봉 윈도우,
  RSI / Stochastic / Volume 3 지표.
- **매매 임계값:** 매수 score ≥ +40, 매도 score ≤ −40. 추가 가드: RSI < 70 / StochK <
  85 (매수), RSI > 30 / StochK > 15 (매도). 최소 동의 지표 3개 이상.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| 단일 지표 기반 | 단순 | 횡보장 위신호 ↑ |
| AI 모델 (RNN, Transformer) | 강력 | 학습 데이터/하이퍼파라미터 튜닝 부담, 해석성 ↓ |
| **(선택) 가중 합산 다지표** | 해석 가능, 단위 테스트 가능 | — |

각 지표의 점수 가중치는 *과거 백테스트* 결과로 결정 (`docs/trading-bot.md` 참조). 새
지표 추가 시 가중치 조정 → 회귀 테스트 필수.

## Consequences — 영향

- **긍정:**
  - 단일 지표 위신호 흡수.
  - 점수 합산 결과를 로그로 남겨 사후 분석 가능.
- **부정:**
  - 8개 지표 계산 비용 — 매 분 캔들 수집 후 모두 갱신.
  - 가중치 튜닝이 *어느 지표가 효과적인지* 명확히 분리 어려움 → 인터프리터빌리티 한계.
- **후속:**
  - ADR-0002 (MA 수렴 시 크로스 억제) 와 ADR-0003 (적자 매매 방지 가드) 가 이 채점
    시스템 위에서 *위신호 보정* + *손실 방지* 추가.

## References

- 관련 코드:
  - `src/main/java/me/singingsandhill/calendar/trading/application/service/SignalScoringService.java`
  - `src/main/java/me/singingsandhill/calendar/trading/application/service/TradingBotService.java`
- 관련 docs: `docs/trading-bot.md`
- 관련 커밋: `git log -1 cbcd9e0`, `git log -1 b09a467`
