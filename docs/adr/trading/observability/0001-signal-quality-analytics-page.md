# ADR-0001: 신호 품질 분석은 앱 내부 온-리드 집계 + 온디맨드 관리자 페이지

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-08-06 |
| 도메인 | trading |
| 관심사 | 관측성 |
| 관련 커밋 | (배치 커밋 시 기입) |
| 관련 ADR | [trading/infrastructure/0005](../infrastructure/0005-candle-retention-for-analysis.md), [trading/observability/0002](0002-decision-input-persistence.md), [trading/strategy/0001](../strategy/0001-multi-indicator-consensus.md) |

## Context — 무엇이 문제였나

봇은 서버에서 `TRADING_BOT_MODE=LIVE` 로 실자금을 운용하는데, 파라미터를 조정할 근거가 없었다.
감사(`docs/audit/coin-trading-profit-audit-2026-05-30.md`)는 P2-1(거래량 다이버전스 ±20 과대가중)을
"백테스트 없는 prior" 로 판단 유보했고, P1-1/P1-2 로 바꾼 손절 −1.5% / TP +3% / 트레일링 −0.8%
역시 실측 검증 없이 적용된 상태였다.

그런데 판단에 필요한 데이터는 이미 쌓여 있었다. `SignalService.generateSignal()` 이 매 루프 틱마다
조건 없이 `trading_signals` 에 1행을 기록한다 — **HOLD 포함**, 하루 약 1,440행, 삭제 스케줄러 없이
영구 보관. 각 행에 8개 구성점수·원지표·그 시점 현재가가 들어 있다. HOLD 행까지 남아 있으므로
"임계를 40이 아니라 50으로 올렸다면?" 같은 반사실 분석이 원리적으로 가능했다. 읽는 수단만 없었다.

## Decision — 무엇을 골랐나

집계를 앱 안에서 수행하고, 결과를 `/trading/analytics` 관리자 페이지로 요청 시점에 렌더한다.

- `TradingAnalyticsService` 가 7개 섹션(데이터 충분성 / 점수 구간별 전방수익 / 구성요소별 조건부
  엣지 / 임계 반사실 / 청산사유별 실현 성과 / 진입 맥락→결과 / 비용 현실)을 계산한다.
- **전방수익은 `trading_candles` 가 아니라 신호 자신의 `current_price` 로 계산한다.** 신호가 매 분
  기록되므로 그 테이블 자체가 1분 가격 시계열이다.
- **스케줄러도, 스냅샷 저장도 두지 않는다.** 페이지는 보낼 것이 없어 주기 갱신 개념이 없고,
  신호·포지션이 영구 보관이라 과거 어느 구간이든 다시 계산하면 된다.
- **JSON 엔드포인트를 만들지 않는다.** 기존 `/api/trading/**` 는 전부 특정 JS 파일이 폴링하기
  때문에 존재하는데, 이 페이지에는 폴링할 이유가 없다.
- 세 가지 통계적 함정을 자료구조에 박는다: 겹치는 창을 보정한 **`effectiveN`**(= resolved /
  지평분), 왕복 수수료를 뺀 **net 수익률**, 그리고 **데이터 충분성 패널을 최상단 배치**.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| DB 쿼리 결과를 CSV 로 첨부해 메일 발송 | 앱에 해석 로직 0 | 첨부는 전송 수단이지 분석이 아니다. 매번 직접 파싱해야 하고, 90일치 원본은 20MB 로 Gmail 한계에 걸린다 |
| 주 1회 스케줄러가 리포트를 만들어 저장 | 푸시가 된다 | 소비자 없는 잡이 하나 늘 뿐이다. 원본이 영구 보관이라 과거 구간 재계산이 언제든 가능해 스냅샷을 남길 이유가 없다 |
| 외부 분석 스토어(BigQuery 등)로 미러링 | 무제한 분석 | 1인 운영에 파이프라인 하나 추가. 현재 질문은 H2 안에서 전부 답할 수 있다 |
| **온-리드 집계 + 온디맨드 페이지 (선택)** | 열 때마다 최신, 보안 설정 변경 불필요(`/trading/**` 가 이미 `ROLE_ADMIN`), 분석 로직이 서비스로 분리돼 나중에 메일이 필요하면 렌더러만 추가 | 푸시가 아니라 "안 보면 안 본다" — 실제로 그렇게 되면 그때 주간 요약 메일을 덧붙인다 |

부수 결정 세 가지도 이 선택에 딸려 온다.

1. **신호 읽기만 엔티티가 아니라 투영(`SignalSample`)을 쓴다.** 90일 = 약 130,000행이고,
   `@Transactional(readOnly = true)` 안에서 엔티티로 읽으면 전부 1차 캐시에 남아 요청이 끝날
   때까지 힙을 붙잡는다. 이 앱은 Jetson Nano 에서도 돈다. 나머지(포지션·체결)는 수십~수백 건이라
   기존 관례대로 엔티티를 읽는다.
2. **매수 게이트를 `SignalService` 에서 추출하지 않고 복제한다.** 추출하면 분석 기능을 위해
   실주문 신호 생성 경로를 수정해야 하는데, 그게 이 작업이 통제하려는 위험 그 자체다. 대신
   `TradingAnalyticsGateParityTest` 가 두 구현의 드리프트를 빌드 실패로 만든다.
3. **전방 조회에 인덱스 산술(`i + 15`)을 쓰지 않는다.** 봇 정지·일시정지·신호 생성 실패로
   시계열에 구멍이 있어서, 인덱스로 세면 조용히 엉뚱한 시각의 가격을 집는다. 목표 시각 기준
   허용오차(90초) 안의 최근접 행만 쓰고, 없으면 그 행을 결측으로 버린다.

## Consequences — 영향

- **긍정:** 감사가 남긴 판단 유보 항목(P2-1 가중치, 출구 R:R)을 실측으로 닫을 근거가 생겼다.
  `effectiveN`·net·커버리지를 강제 표기하므로 "n=10,000, 평균 +0.31%" 를 결정적 증거로
  오독하는 실수가 구조적으로 막힌다.
- **부정:** 매수 게이트 로직이 두 곳에 존재한다 — 파리티 테스트가 유일한 방어선이므로,
  그 테스트가 깨지면 리팩터링 중이라도 무시하고 넘어가면 안 된다. 90일 조회는 요청 스레드를
  수 초 잡을 수 있다(페이지 하단에 `computeMillis` 를 표시해 비용을 보이게 했다).
- **후속:** 이 페이지가 판정할 수 있는 범위는 기록된 결정 입력에 달려 있다 →
  [observability/0002](0002-decision-input-persistence.md),
  [observability/0003](0003-signal-series-continuity-on-risk-exit.md).
  리플레이 지평은 캔들 보관 기간에 달려 있다 → [infrastructure/0005](../infrastructure/0005-candle-retention-for-analysis.md).

## References

- 관련 문서: `docs/audit/coin-trading-profit-audit-2026-05-30.md`, `docs/trading/remaining-work.md`
- 관련 코드: `src/main/java/.../trading/application/service/TradingAnalyticsService.java`,
  `.../application/dto/AnalyticsReport.java`, `.../domain/signal/SignalSample.java`,
  `src/main/resources/templates/trading/analytics.html`
- 관련 테스트: `TradingAnalyticsServiceTest`, `TradingAnalyticsGateParityTest`, `TradingAnalyticsPageTest`
