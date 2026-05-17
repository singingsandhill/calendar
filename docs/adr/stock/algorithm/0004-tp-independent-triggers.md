# ADR-0004: TP1·TP2·TP3 비순차화 (선행 의존 제거)

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-05-01 |
| 도메인 | stock |
| 관심사 | 알고리즘 / 리스크 |
| 관련 커밋 | `docs/git_commit.md` Commit 4 (PR-4) |
| 관련 이슈 | — |

## Context — 무엇이 문제였나

기존 `StockPosition.shouldTp2/shouldTp3` 가 *TP1 이 발동된 이후만 TP2 검사*, *TP2 발동
이후만 TP3 검사* 식으로 선행 의존을 가졌다. 부작용:

- **장 초 급등 케이스** — TP1(+1.5%) 을 거치지 않고 즉시 +5% 로 점프하면 TP3 트리거가
  발동 자격이 없어 익절 누락.
- **분봉 갭 업** — 한 분봉 안에서 TP1 부분 익절 처리 직후 가격이 TP3 까지 갔는데
  다음 분봉으로 미뤄지면서 익절가 흘러내림.

또 `checkTakeProfitLevels` 의 분기 코드가 길고 중복 — 세 단계가 거의 같은 패턴
(threshold 체크 → 부분 매도 호출 → 이벤트 emit) 인데 인라인 반복.

## Decision — 무엇을 골랐나

TP1/TP2/TP3 를 독립 트리거로 만들고 가장 강한 신호를 즉시 발동.

- **`StockPosition.shouldTp2 / shouldTp3`** — TP1·TP2 선행 의존 제거. 각 임계값
  도달 여부만 판단.
- **`StockRiskService.checkTakeProfitLevels`** — TP3 → TP2 → TP1 순서로 *독립 평가*.
  가장 강한 트리거가 발견되면 즉시 발동. 같은 분봉에서 TP3 이 가능하면 TP1/TP2 를
  건너뛰고 TP3 부분 매도 비율 적용.
- **`tryFireTp` 헬퍼** — 세 단계의 중복 코드(threshold 체크 + 부분 매도 + 이벤트)를
  하나의 메서드로 추출.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| 선행 의존 유지 | 점진적 익절 보장 | 급등 케이스에서 TP3 누락 — 의도된 가치 손실 |
| TP3 단일 청산 | 단순 | TP1/TP2 의 *부분 익절로 평균 진입가 끌어올리기* 전략 사라짐 |
| **(선택) 독립 트리거, 강한 것 우선 발동** | 익절 누락 0 + 부분 익절 정책 보존 | — |

부분 매도 비율은 각 TP 단계별로 다르게 정의되어 있으므로 (TP1=1/3, TP2=1/3, TP3=잔여)
즉시 TP3 발동 시 잔여 100% 청산 → 최대 익절. TP3 미도달 시 점진적 매도.

## Consequences — 영향

- **긍정:**
  - 장 초 급등 종목에서 익절 누락 사라짐.
  - `tryFireTp` 추출로 향후 TP4 추가 시 호출 한 줄.
  - 독립 트리거이므로 단위 테스트가 쉬워짐 (`StockPositionTakeProfitTest` 가 각
    레벨을 격리 검증).
- **부정:**
  - TP1 부분 익절을 통한 평균 진입가 끌어올리기 효과가 *TP3 즉시 발동 케이스*에서는
    사라짐. 장 흐름에 따라 일장일단.
- **후속:**
  - 백테스트 시 분봉 시뮬레이션에서 TP3 도달률을 별도 메트릭으로 관찰 권장.

## References

- 관련 코드:
  - `src/main/java/me/singingsandhill/calendar/stock/domain/position/StockPosition.java`
  - `src/main/java/me/singingsandhill/calendar/stock/application/service/StockRiskService.java`
  - `src/test/java/me/singingsandhill/calendar/stock/domain/StockPositionTakeProfitTest.java`
- 관련 docs: `docs/stock-bot.md` (TP1/TP2/TP3 정책)
- 관련 커밋: `docs/git_commit.md` Commit 4
