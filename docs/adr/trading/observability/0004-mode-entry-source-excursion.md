# ADR-0004: 체결·포지션 운영 모드(mode) + 포지션 진입 출처(entrySource) + 최대 유리·불리 폭(MFE/MAE)

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-10-22 |
| 도메인 | trading |
| 관심사 | 관측성 |
| 관련 ADR | [trading/observability/0002](0002-decision-input-persistence.md), [trading/risk/0002](../risk/0002-rebalance-position-accounting.md), [trading/modes/0002](../modes/0002-paper-default-mode.md), [stock/observability/0003](../../stock/observability/0003-signal-restore-and-mode.md) |
| 관련 이슈 | 2026-10-20 운영 데이터 진단 P1 (`docs/prompts/trading-fix-2026-10-20.md` 작업 3) |

## Context — 무엇이 문제였나

- **PAPER/LIVE 혼재:** 코인 체결 1,512건 중 `PAPER-` 44건(마지막 8/2)이 LIVE 와 같은 테이블에 섞여 있다. 손익 요약·일별 요약·
  승률·신호 품질 분석이 둘을 합산한다. 구분은 거래 `uuid` 접두사뿐이고 포지션에는 그마저 없다.
- **진입 출처 부재:** 10/11~10/20 리밸런싱 진입 17건과 신호 진입 1건을 가르려면 이벤트 시각 매칭으로 추정해야 했다.
  `trading_trades.signal_id`(ADR observability/0002)는 신호 매수만 표시하고, 리밸런싱·수동·스윕 복구 진입은 모두 null 이라
  null 의 의미가 하나가 아니다.
- **보유 중 폭 부재:** 포지션에 최고가(HWM)만 있고 최저가가 없어, "손절에 걸린 포지션이 그 전에 얼마나 올랐었나 / 익절한
  포지션이 얼마나 빠졌었나" 를 포지션 데이터만으로 볼 수 없었다 — 출구 규칙 대안(작업 5·6) 평가의 기본 재료다.

## Decision — 무엇을 골랐나

- `mode` 컬럼(nullable, 값 `LIVE`/`PAPER`/`BACKTEST`): `trading_trades.mode`, `trading_positions.mode`. 도메인 `TradeMode`.
  **insert 시 어댑터가 현재 봇 모드(`trading.bot.mode`)를 찍는다**(주식 ADR observability/0003 과 같은 방식 — 생성 지점
  누락 불가). 집계 필터 기본 **LIVE**: `ProfitService` 의 손익 요약·오늘 요약·일별 요약 생성, `TradingAnalyticsService`.
  `GET /api/trading/profit/summary`·`/today` 는 `?mode=PAPER` 로 다른 모드를 볼 수 있다. NULL(백필 전 레거시) 은 LIVE 로
  취급한다. 거래·포지션 목록 API 는 필터하지 않고 행마다 `mode` 를 내려준다.
- `trading_positions.entry_source`(nullable, `EntrySource`): 생성 지점마다 설정 — 신호 매수 `executeBuy` = `SIGNAL`,
  리밸런싱 매수 = `REBALANCE`, 수동 매수·검증용 test-order = `MANUAL`, §8-B 스윕이 체결을 확인해 만든 포지션 = `RECOVERY`.
  **과거분은 null 유지 — 이벤트 시각 매칭으로 추정 백필하지 않는다**(추정이 사실처럼 굳는다).
- `trading_positions.low_water_mark`(nullable): `Position.open` 이 진입가로 초기화, 리스크 루프가 매 체크 **손절 판정 전에**
  현재가로 갱신(HWM 은 종전 위치 그대로). 포지션 응답 DTO 에 `mfePct = (HWM/진입가 − 1)×100`, `maePct = (LWM/진입가 − 1)×100`
  (수수료 미차감 가격 폭, 소수 4자리, LWM 없는 과거분은 `maePct` null).
- 백필(운영 수동 실행, 멱등 — `WHERE mode IS NULL`). 실행 프롬프트의 `order_id` 는 코인 테이블에 없다 — 코인 거래의 주문
  식별자 컬럼은 `uuid` 다:

```sql
UPDATE trading_trades SET mode = CASE WHEN uuid LIKE 'PAPER-%' THEN 'PAPER' ELSE 'LIVE' END
 WHERE mode IS NULL;
UPDATE trading_positions SET mode = CASE WHEN EXISTS (
         SELECT 1 FROM trading_trades t WHERE t.position_id = trading_positions.id AND t.uuid LIKE 'PAPER-%')
       THEN 'PAPER' ELSE 'LIVE' END
 WHERE mode IS NULL;
```

  H2 2.4.240 MySQL 모드에서 실행·재실행(무변경)을 확인했다. 포지션은 연결된 매수·매도 거래로 판정한다 — 리스크 청산·리밸런싱
  매도·신호 매도는 모두 `position_id` 를 남기므로 PAPER 시기 포지션은 청산 거래로 잡힌다. 연결 거래가 하나도 없는 포지션
  (2026-09-27 이전 리밸런싱·수동 매수 중 수동 매도로 닫힌 것, 미청산)은 LIVE 로 분류된다.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| A. 집계 쿼리마다 `uuid` 접두사 조인 | 스키마 무변경 | 포지션은 매번 거래 조인, BACKTEST 구분 불가, 접두사 규칙이 집계 코드 전역으로 샌다 |
| B. 서비스가 생성 지점마다 mode 설정 | 명시적 | 생성 지점 8곳(신호·스윕·리스크 청산·리밸런싱·수동 2·test-order …) 중 하나만 빠져도 그 행이 LIVE 집계로 섞인다 |
| **C. insert 시 어댑터가 모드를 찍음 (선택)** | 누락 불가 | 어댑터가 `TradingProperties` 를 읽는다 |
| entrySource 추정 백필 | 과거 통계 확보 | 추정이 사실 데이터로 굳는다 — 분석에서 "출처 불명" 이 정직한 값 |

- 진입 출처는 서비스만 알 수 있어 어댑터가 찍을 수 없다 — 생성 지점에서 명시한다(5곳, 테스트로 고정).
- LWM 을 손절 판정 **전에** 갱신하는 이유: 손절이 걸리는 틱의 가격이 곧 최저가인데, 판정 뒤에 두면 손절 포지션의 MAE 가
  체계적으로 빠진다. HWM 은 손절 틱에 바뀌지 않으므로 위치를 옮기지 않았다(트레일링 동작 불변).
- MFE/MAE 는 리스크 루프 주기(1분, 호가 중간값) 표본이다 — 분 안의 꼬리는 잡지 않는다.

## Consequences — 영향

- **긍정:** 손익·승률·분석이 LIVE 만 본다(PAPER 44건 제거, 백필 후). 리밸런싱 진입과 신호 진입을 쿼리 한 줄로 가른다
  (작업 4-C 의 REBALANCE 포지션 출구 규칙이 이 컬럼을 쓴다). 출구 리플레이(작업 6)가 실제 포지션의 MFE/MAE 와 대조할 수 있다.
- **부정:** 2026-10-22 이전 포지션은 `entry_source`·`low_water_mark` 가 null — 진입 출처 분석은 이 날짜 이후분만 유효.
  백필 전에는 레거시 PAPER 행이 LIVE 로 집계된다(종전과 같음).
- **주의(범위 밖, 보고만):** LIVE 프로세스의 리스크 루프는 모드와 무관하게 OPEN 포지션 전부를 청산 대상으로 본다 — PAPER
  시기에 남은 OPEN 포지션이 있으면 LIVE 에서 실매도 주문이 나갈 수 있다. 이제 `mode` 로 식별할 수 있으니 별도 결정이 필요하다.
- 회귀 가드: `TradingPersistenceModeTest`(어댑터 mode 찍기·복원), `PositionExcursionTest`, entrySource 생성 지점 5건,
  `ProfitServiceModeFilterTest`, `TradingAnalyticsService` 모드 필터, 리스크 루프 LWM 갱신 순서.

## References

- `trading/domain/{trade/TradeMode, position/EntrySource, position/Position}.java`
- `trading/infrastructure/persistence/adapter/{Trade,Position}RepositoryAdapter.java`
- `trading/application/service/{TradingBotService, RebalanceService, RiskManagementService, ProfitService, TradingAnalyticsService}.java`
- `trading/presentation/api/{TradeApiController, TradingVerificationApiController}.java`
