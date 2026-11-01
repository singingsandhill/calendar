# ADR-0004: PAPER 가상 예수금 원장 + 체결 현실화(슬리피지·KRX 호가단위)

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-10-22 |
| 도메인 | stock |
| 관심사 | 모드 / 관측성 |
| 관련 ADR | [stock/modes/0001](0001-paper-backtest-mode-and-clock-bean.md), [stock/modes/0002](0002-paper-default-mode.md), [stock/algorithm/0008](../algorithm/0008-screening-selection-and-cost-model.md), [stock/infrastructure/0006](../infrastructure/0006-order-pre-persistence-and-fill-backfill.md) |
| 관련 이슈 | 2026-10-20 운영 데이터 진단 P0 (`docs/prompts/trading-fix-2026-10-20.md` 작업 1) |

## Context — 무엇이 문제였나

- 2026-08-03 ~ 10-20, **51거래일 동안 감시 306건 · `ENTRY_READY` 139건인데 거래·포지션 0건**.
  PAPER 는 LIVE 전환 판단의 데이터원(ADR stock/modes/0002, observability/0002)인데 표본이 0 이었다.
- 원인: `StockPositionService.calculatePositionSize()` 가 모드와 무관하게 **실계좌**
  `getAvailableCash()`·`getBuyableQuantity()` 를 읽었다. PAPER 계좌 예수금은 0 → 수량 0 →
  `openPosition()` 이 기록 없이 `null` → `POSITION_OPEN_FAILED` 로그 이벤트뿐(DB 흔적 없음).
  ADR modes/0001 의 "시세·잔고 조회는 모드 무관 실 API" 원칙이 사이징에는 맞지 않았다 — 시세는 실측이
  필요하지만 예수금은 가상이어야 한다.
- 부수 문제: PAPER 체결가가 **요청가 그대로**(매도는 트리거가 그대로) 기록돼, 시장가 슬리피지가 없는
  낙관적 장부였고 호가단위에 없는 가격도 체결가가 될 수 있었다.

## Decision — 무엇을 골랐나

PAPER/BACKTEST 사이징은 **DB 에서 매번 계산하는 일 단위 가상 예수금 원장**을 쓰고, 시뮬레이션 체결가는
**요청가 ± 슬리피지를 KRX 호가단위로 반올림**한다. LIVE 경로는 그대로다.

- `stock.bot.paper-cash`(기본 `10000000`, env `STOCK_BOT_PAPER_CASH`) — 거래일 시작 가상 예수금.
- `PaperCashLedger.available(tradingDate)` = paper-cash − Σ(SIM- 매수: 체결금액 + 수수료)
  + Σ(SIM- 매도: 체결금액 − 수수료·세금). 그날 `orderedAt` 의 체결된 시뮬레이션 거래만 센다
  (`StockTrade.isSimulated()` = 주문번호 `SIM-` 접두사). 잔고를 메모리에 두지 않아 **재시작해도 같은 값**.
- `calculatePositionSize()`: `mode != LIVE` 면 원장 가용액을 쓰고 `getAvailableCash()`·
  `getBuyableQuantity()` 를 **호출하지 않는다**. 매수가능수량의 PAPER 대응은
  `floor(가용액 / (시뮬레이션 체결가 × (1 + 수수료율)))`.
- `stock.paper.slippage-percent`(기본 `0.2`) — 매수 체결가 = `KrxTickSize.roundUp(요청가 × 1.002)`,
  매도 체결가 = `KrxTickSize.roundDown(요청가 × 0.998)`. 매도 원장 수수료·포지션 손익도 이 체결가 기준.
- `KrxTickSize`(도메인 유틸) 신설 — 주식 2023-01-25 단일화 표(2,000 미만 1 · 5,000 미만 5 · 20,000 미만 10 ·
  50,000 미만 50 · 200,000 미만 100 · 500,000 미만 500 · 이상 1,000), ETF·ETN 은 2023-12-18 개정으로
  2,000 미만 1 · 이상 5. 상품 유형을 모르면 주식 표.
- 관측: `GET /api/stock/bot/status` 에 `mode`, `paperAvailableCash`(LIVE 면 null), 기동 로그
  `[StockBot] mode=PAPER, paperCash=…, paperAvailableCash=…`.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| A. PAPER 에서 수량 0 이면 고정 수량(예: 1주)으로 진입 | 단순 | 가격대 68원~282,250원(중앙 6,730원) 종목을 같은 1주로 사면 손익 비교가 무의미 |
| B. 인메모리 가상 잔고 | 계산 비용 없음 | 재시작(배포 = 재시작)마다 리셋 — 장중 재배포 후 이미 쓴 돈을 다시 쓴다 |
| C. 누적(이월) 원장 | 장기 자산곡선 | 장중 전략이고 11:20 전량 청산이라 이월분이 없다. 오버나이트 청산 실패 시 이중계상 위험만 추가 |
| **D. 일 단위·DB 재계산 원장 (선택)** | 재시작 정합, 거래 원장(`StockTrade`) 한 곳이 진실원 | 매 진입 시도마다 당일 거래 1회 조회(수십 행) |

- paper-cash 1,000만원 근거: `position-size-ratio` 10% 면 포지션당 약 100만원 — 진입가 최고대(282,250원)도
  3주 이상 살 수 있고 `max-position-size`(500만원) 캡에 걸리지 않는다. 동시 5포지션이면 원장의 약 41%.
- 슬리피지 0.2% 는 ADR algorithm/0008 의 비용 모델(`risk.slippage-buffer` 0.002) 과 같은 가정이다.
  익절 게이트가 이미 이 비용을 차감해 판정하므로, 실제 체결에도 같은 비용을 물려야 PAPER 손익이 LIVE 와
  같은 잣대가 된다. 호가단위 반올림은 매수 올림·매도 내림 — 항상 불리한 쪽.

## Consequences — 영향

- **긍정:** PAPER 가 실제로 체결·청산 기록을 남긴다 → 11:40 일일 리포트가 표본을 갖는다.
  상태 API 만으로 "PAPER 인데 쓸 돈이 0" 같은 상태를 볼 수 있다.
- **부정:** PAPER 손익이 이전(요청가 체결)보다 왕복 약 0.4% + 호가단위만큼 나빠진다 — 의도된 보수화.
  가상 예수금은 거래일 밖으로 이월되지 않는다(최종청산 실패로 남은 포지션은 다음 날 원장에 안 잡힌다).
- **후속:** 작업 7(상품 유형 저장) 전까지 ETF·ETN 체결은 주식 호가표로 반올림된다(1틱 미만 오차).
  → 2026-10-23 [algorithm/0011](../algorithm/0011-product-type-and-deferred-etf-etn-exclusion.md) 로 해소 — 종목의 실제 유형을 쓴다.
  다음 거래일 운영 확인: `/api/stock/trades` 에 `SIM-` 주문 ≥ 1건, 11:40 리포트에 체결 집계.
- 회귀 가드: `KrxTickSizeTest`(5), `PaperCashLedgerTest`(8), `StockPositionServiceTest` PAPER 사이징·체결 6건,
  `GapPullbackBotPaperStatusTest`(2), `StockBotApiSecurityTest.statusExposesModeAndPaperAvailableCash`.

## References

- `stock/application/service/PaperCashLedger.java`
- `stock/application/service/StockPositionService.java` (`calculatePositionSize`, `resolveBuyFill`, `doExecutePartialExit`)
- `stock/domain/trade/KrxTickSize.java`, `stock/domain/stock/StockProductType.java`
- KRX 호가가격단위: 대신증권 공지(2023-01-25 시행 표), 이데일리 2023-10-05(ETF·ETN 2,000원 미만 1원, 12-18 시행)
