# 주식 트레이딩 봇 로직 리뷰 — 수익 가능성·타당성 (2026-07-24)

> 대상: `stock` 모듈 — 한국주식 Gap & Pullback 자동매매 (한국투자증권 KIS API). Spring Boot 4 / Java 21, 헥사고날 구조.
> 방법: 4개 관점(경제성·상태머신·주문실행·스크리닝) 멀티에이전트 정적 분석 → 발견 37건 → 발견별 적대적 검증
> (완료 13건 **반박 0건**, 세션 한도로 중단된 치명·상위 항목은 코드 1차 재확인으로 대체 검증).
> 성격: **분석·권고 문서.** 코드는 변경하지 않음. 실제 수정은 §9 우선순위에 따라 단계적 PR 로 진행.
> 관련: 선행 진단 (2026-07-24 로그 분석 — 유니버스 rank=0·cttr=0, [ADR stock/algorithm/0006](../adr/stock/algorithm/0006-universe-rank-retry-at-screening.md)),
> 코인 봇 선례 [`coin-trading-profit-audit-2026-05-30.md`](coin-trading-profit-audit-2026-05-30.md) ·
> [`coin-trading-operational-review-2026-07-06.md`](coin-trading-operational-review-2026-07-06.md), 결정 기록 [`docs/adr/stock/`](../adr/stock/).

핵심 파일: `GapPullbackBotService.java`(388행), `ScreeningService.java`(546행), `PullbackDetectionService.java`(317행),
`StockRiskService.java`(262행), `StockPosition.java`(428행), `StockPositionService.java`(300행), `Stock.java`(327행),
`StockRepositoryAdapter.java`, `KisRestClient.java`(758행), `KoreaInvestmentApiClient.java`, `StockProperties.java`,
`common/.../SecurityConfig.java`, `application.yaml:205-356`.

---

## 1. 한 줄 결론

**현재 코드는 "수익이 나는가"를 논하기 전 단계다.** 진입 파이프라인이 상태 영속화 버그로 기술적으로
작동 불가하고(그래서 지금까지 손실도 없었음), 그 버그를 고쳐 설계대로 작동시켜도 현재 파라미터 조합은
손익분기 승률 ~83% 를 요구하는 역스큐 구조라 기대값이 음수일 개연성이 높다. 여기에 무인증 봇 제어
API + 기본 LIVE 모드라는 운영 사고 위험이 겹쳐 있다.

## 2. 왜 지금까지 한 번도 매수가 없었나 — 3중 독립 차단

| # | 차단 지점 | 상태 |
|---|---|---|
| 1 | 유니버스: 08:30 거래량순위 매일 0건 → 정적 대형주 폴백 70종목 | 2026-07-24 수정 (스크리닝 시점 재시도, ADR-0006) |
| 2 | 스크리닝: 체결강도(cttr)=0 대량 → dataInsufficient 스킵 | 계측 로깅 배포, 원인 확인 중 |
| 3 | **상태머신: DB 재로딩 시 상태 필드 유실 → HIGH_FORMED 에서 조용히 영구 정체 (§3-①)** | **본 리뷰에서 신규 발견, 미수정** |

①·② 를 고쳐도 ③ 때문에 매수는 발생하지 않는다. 역설적으로 이 3중 차단이 아래 §3 의
주문·익절 결함들이 실계좌에서 터지는 것을 지금까지 막아 왔다.

## 3. 치명 결함 (P0) — 매매가 시작되는 순간 문제가 되는 것들

### ① 상태머신이 DB 왕복마다 기억을 잃음 → 진입 자체가 불가능 [코드 1차 확인]

`StockRepositoryAdapter.toDomain()`(149-166행)이 `highAfterOpen`·`highFormedAt`·`pullbackLow`·
`pullbackStartAt`·`entryPrice` 5개 필드를 복원하지 않는다 (저장 측 `updateEntity` 142-146행은 모두 기록).
트레이딩 루프는 5초마다 `findActiveStocks()` 로 재로딩하므로 HIGH_FORMED 전이 때 기록한 고가가
다음 틱에 null 이 되고, `Stock.calculateDropFromHigh()`(139-141행)는 null 이면 **예외 없이 0% 를 반환**
→ 풀백 판정(`isInPullbackRange`)이 영원히 false → 로그도 없이 HIGH_FORMED 정체. 다음 save 는
null 을 DB 에 되써서 이전 기록도 지운다.

### ② `/api/stock/bot/**` 무인증 공개 + CSRF 면제 + 기본 LIVE [적대적 검증 유지 — critical 상향]

`SecurityConfig.java:74` `.requestMatchers("/api/stock/**").permitAll()` + `:126` CSRF `/api/**` 면제로
`StockBotApiController` 의 start/stop/pause/emergency-close(47-102행)가 인터넷에서 무인증 POST 가능.
크립토는 동일 문제를 [ADR common/security/0003](../adr/common/security/0003-admin-only-trading-control-api.md)
으로 막았으나(`/api/trading/**` ADMIN) 주식 모듈만 누락. `stock.bot.mode` 기본값도
LIVE(`StockProperties.java:70`) — 크립토는 수익성 감사 후 PAPER 기본으로 전환한 선례가 있다
([ADR trading/modes/0001](../adr/trading/modes/0001-paper-mode-default-and-order-gate.md)).

### ③ 비멱등 주문 POST 에 자동 재시도 — 중복 시장가 주문 위험 [적대적 검증 유지]

주문 실행(`KisRestClient.java:592` `executeOrder` → `executePostWithRetry` 147-177행)이 조회와 동일한
`Retry.backoff(3, 1s)` 를 타며, 재시도 대상(`isRetryableException` 82-96행)에 IOException·
"prematurely closed"·429·5xx 가 포함된다. KIS 가 주문을 접수했는데 응답만 유실되면 같은 시장가
주문이 최대 3회 재전송된다. 모든 예외를 삼키고 null 반환 → 호출측은 "주문 실패"로 오인.

### ④ 체결 확인 부재 — 주문 전 시세로 체결가·수수료 0 픽션 기록 + 주문 선영속화 없음 [적대적 검증 유지]

매수·매도 모두 주문 **직전** quote 가격을 체결가로, 수수료 0 으로 기록
(`StockPositionService.java:109, 207` `trade.markFilled(currentPrice, quantity, BigDecimal.ZERO)`).
체결조회 TR(TTTC8001R, `KisRestClient.getOrderHistory`)은 구현돼 있으나 **application 레이어 호출처 0곳**.
주문 전 영속화도 없어(96-99행 실패 시 즉시 return null) 응답 유실 시 "체결됐지만 시스템에 없는
포지션"이 생긴다 — 크립토 [ADR trading/infrastructure/0002](../adr/trading/infrastructure/0002-order-pre-persistence-and-tick-sweep.md)
(주문 선영속화 + 틱 스윕)의 대응물 부재. 진입가·손절가·TP 앵커가 전부 이 스냅샷 기준이라
슬리피지만큼 실효 SL 이 넓어지고 TP 문턱이 실질 상향된다.

### ⑤ TP1 수량 버그 — 승자 트레이드의 익절 경로 파괴 [코드 1차 확인]

`StockPosition.calculateTp1Quantity()`(249행) = `floor(entryQuantity × 0.5)` — 잔여수량 캡 없음,
`tp1-ratio` 설정 무시(하드코딩 0.5). 파라미터상 TP2(전고점 회복, 진입가 +0.8~5.0%)가 TP1(+5%)보다
거의 항상 먼저 발동해 잔여 40% 상태에서 TP1 트리거 시 50% 매도를 시도하는데, 매도 주문이 도메인
수량 검증(`executePartialExit` 275-277행 throw)보다 **먼저** 나간다(`StockPositionService.java:191`).
LIVE: 잔고부족 거부 주문 5초 반복 스팸(계좌에 동일 종목 별도 보유분이 있으면 그것을 매도).
PAPER: 매 틱 IllegalArgumentException.

## 4. 수익성 수학 — 결함을 다 고쳐 "설계대로" 작동한다고 가정해도

청산 체인의 실동작 (코드 1차 확인):

- **TP2 가 사실상 유일한 익절.** 진입가 = 풀백저가×1.002 (yaml bounce 0.2%), 풀백 깊이 1~5%
  → 전고점 회복(TP2, `shouldTp2`: 현재가 ≥ 직전 틱 당일고가) 시 이익 +0.8~5.0%(통상 ~+2%)에서
  잔여의 60% 청산. TP1(+5%)이 선행하려면 풀백 깊이가 ~4.8% 이상이어야 해 경계 사례뿐.
- **TP3 는 사문 조항.** `shouldTp3`(227-230행)는 매 틱 갱신되는 당일고가(`stck_hgpr`)의 +10% 를
  요구 — 5초 폴링 간격에 +10% 점프는 KRX VI(±3% 급변 시 2분 단일가) 하에서 수학적으로 불가능.
- **트레일링(-3.8%)은 이중 결함으로 미작동.** (a) 활성화 조건이 `tp1Executed` 뿐(347행)인데 TP1 은
  TP2 에 선점 + §3-⑤ 버그로 실행 불가. (b) 활성화돼도 `trailingStopPrice` 는 신고가 갱신
  블록(357-370행) 안에서만 세팅 — 활성화 직후 하락만 하면 스탑 가격 null 로 영원히 미발동.
- 따라서 잔여 물량의 보호·익절 수단은 **-5% 손절과 11:20 시간청산뿐.**

기대값: 승리 혼합익 ≈ 0.6×(+2%) + 0.4×(시간청산 ≈ 0) ≈ **+1.1%** vs 패배 **-5.26%**(비용 포함)
→ **손익분기 승률 ~83%.** 갭업 종목이 2시간 창(09:20~11:20)에서 -5% 역행할 확률을 감안하면
비현실적 요구 승률이다. 손절 폭(5%)이 현실적 익절 폭의 4배 이상인 R:R 역전이 근본 문제.

비용 모델 평가 (코드 1차 확인):

- **잘 된 것:** 모든 TP 는 수수료 포함 수익률(`calculateUnrealizedPnlPctWithFee`)이 time-decay
  임계(11:20 기준 유효 ~0.36%) 이상일 때만 발동(`StockRiskService.tryFireTp` 146행) — 비용 미만
  명목익절 churn 은 구조적으로 차단. 트레일링에 손익분기 하한 보장(365-367행)도 있음(활성화 시).
- **문제:** 매도세율 0.23%(`StockProperties.java:240`)는 구식 — 2026 실효 코스피 농특세 0.15%
  (거래세 0%)·코스닥 0.15%. 왕복 모델 0.26% vs 실제 ~0.18% 로 장부손익 과소 기록.
  `slippageBuffer`(0.2%, :241)는 정의만 있고 **사용처 0곳** — 실슬리피지는 §3-④ 픽션 기록과
  겹쳐 P&L 에 전혀 안 잡힘. time-decay 의 15:15 하드코딩(`StockRiskService.java:174`)은 11:20
  청산과 이중 진실원 — 후반 완화(0.1%)는 설계상 도달 불가(감쇠 레인지 64% 미소진).

## 5. 스크리닝·유니버스 타당성

| 항목 | 내용 | 검증 |
|---|---|---|
| min-candidates 강제 선정 | 선정 루프가 `selected.size() < 3 \|\| score ≥ 40` — floor 만 통과하면 점수 무관 상위 3개 강제 선정. 엣지 없는 날에도 매일 3종목 진입 시도 구조 | 코드 1차 확인 |
| 갭 floor 0.5% | 갭업 전략 하한으로는 노이즈 수준. 벨커브(중심 4%, σ3) 점수와 결합 시 0.5% 갭에도 상당 점수 | 미검증(정황) |
| min-score 40 이 유동성 팩터만으로 충족 | 거래대금(20)+시총(10)+스프레드(15) 합 45 > 40 — 갭·강도 신호 없이 통과 가능한 점수 왜곡 | 미검증(정황) |
| 체결강도 floor 95 | 100=매수세 균형이므로 95 는 매도우위 종목도 통과. 진입 검증 imbalance ≥1.0 도 사실상 무필터, soft 2/3 이 추가 무력화 | 부분 확인 |
| 거래량순위 유니버스 | 주식수 기준(FID_BLNG_CLS_CODE=0) + 가격하한 0 — 저가주 편향 가능. 시총 floor 500억이 일부 필터 | 미검증(정황) |
| 09:20 단발 스크리닝 | 이후 갭 형성 종목 미포착. 09:00~09:20 실고가 미반영(`recordHighFormed` 가 KIS `stck_hgpr` 아닌 현재가를 고가로 기록 — 고가 앵커 과소) | 부분 확인 |

## 6. 운영 리스크 [적대적 검증 유지]

- **재시작 시 `running=false`** (`GapPullbackBotService.java:39` 인메모리 AtomicBoolean): 자동 기동
  없음, `start()` 호출처는 공개 API 단 한 곳. 오픈 포지션 보유 중 재배포되면 손절·트레일링·11:20
  청산까지 전부 정지.
- **11:20 최종청산 원샷** (`StockTradingScheduler.java:89` cron 1회 + 루프 가드 11:20 마감): 시세
  조회 실패 시 **로그 없이 스킵**(`StockRiskService.java:230-233`), 매도 거부 시 재시도·알림 없음
  → 오버나이트 홀드.
- **VI·단일가·상한가·거래정지 방어 0건**: 관련 KIS 필드(temp_stop_yn, vi_cls_code, mrkt_warn 등)
  참조가 전 모듈에 없음. 갭 상한 15% 허용과 위험한 조합.
- **PAPER 체결 모델 과낙관** (`KoreaInvestmentApiClient.simulateOrder` 238-243행): 요청가 즉시
  전량 체결 픽션 — PAPER 검증만으로 LIVE 성과를 추정하면 슬리피지·부분체결·미체결이 전부 누락.

## 7. 문서-코드 드리프트 [코드 1차 확인]

| 문서 | 문서 값 | 실제 운영값 |
|---|---|---|
| stock/CLAUDE.md 상태머신 | PULLBACK -1.5%, FILTERED_OUT -3.0%, bounce +0.3% | yaml: -1~-5%, -5%, +0.2% |
| `PullbackDetectionService` 주석 (128·152행) | "-1.5% ~ -3.0%", "+0.3%" | 자기 설정과 불일치 (동일) |
| ADR stock/algorithm/0004 부분청산 | 1/3·1/3 비율 | 코드: 50%·60% |
| CLAUDE.md "TP3 +10% Sell remaining" | 실행 가능처럼 기술 | §4 — 도달 불가 |

## 8. 잘 되어 있는 것

수수료 포함 TP 게이트 + 손익분기 트레일링 하한(활성화 시), 주문 진입부 LIVE/PAPER 모드 가드,
Semaphore(8)+종목별 ReentrantLock 동시성 계층, EntryAttempt 영속화(거절 사유 라벨링), TradeEvents
관측성 + 스크리닝 침묵 실패 WARN, Clock 빈 기반 결정성 테스트. 골격과 관측성은 성실하다 —
문제는 도메인 로직 정합성과 파라미터 수학이다.

## 9. 우선순위 로드맵

> **진행 상태 (2026-07-24 갱신):** P0 5건 · P1 6건 · P2 5건 **모두 완료** (TDD, 전체 스위트 GREEN).
> P2-5 만 코드가 아닌 *운영 대기* — 집계 도구(일일 리포트)는 완비됐고 PAPER 2~4주 누적이 남았다.
> 관련 ADR: [common/security/0005](../adr/common/security/0005-admin-only-stock-bot-control-api.md) ·
> [stock/modes/0002](../adr/stock/modes/0002-paper-default-mode.md) ·
> [stock/modes/0003](../adr/stock/modes/0003-protection-only-recovery-on-restart.md) ·
> [stock/infrastructure/0005](../adr/stock/infrastructure/0005-non-idempotent-order-no-retry.md) ·
> [stock/infrastructure/0006](../adr/stock/infrastructure/0006-order-pre-persistence-and-fill-backfill.md) ·
> [stock/algorithm/0007](../adr/stock/algorithm/0007-exit-structure-recalibration.md) ·
> [stock/algorithm/0008](../adr/stock/algorithm/0008-screening-selection-and-cost-model.md) ·
> [stock/observability/0002](../adr/stock/observability/0002-daily-performance-report.md).
> **P2-5(PAPER 실측) 통과 전에는 LIVE 전환 금지.**

| 순위 | 항목 | 근거 | 상태 |
|---|---|---|---|
| **P0-1** | `/api/stock/bot/**` ADMIN 전용화 (+CSRF 정책 검토) | §3-② — 크립토 ADR 0003 선례 | ✅ POST 만 ADMIN (GET status 공개 유지) |
| **P0-2** | `stock.bot.mode` 기본 PAPER 전환 | §3-② — 크립토 ADR trading/modes/0001 선례 | ✅ `STOCK_BOT_MODE=LIVE` opt-in |
| **P0-3** | `toDomain` 상태 필드 5개 복원 | §3-① — 진입 파이프라인 차단 해제 | ✅ `restorePersistedState` |
| **P0-4** | 주문 POST 재시도 제거(또는 멱등키/주문조회 확인 후 재시도) | §3-③ | ✅ `executePostNoRetry` |
| **P0-5** | TP1 수량 `min(entry×tp1Ratio, remaining)` 캡 + 주문 전 도메인 검증 선행 | §3-⑤ | ✅ 주문 전 수량 검증 포함 |
| **P1-1** | 체결조회(TTTC8001R) 기반 실체결가·수수료 backfill + 주문 선영속화 | §3-④ | ✅ + 고아 체결 스윕 |
| **P1-2** | 트레일링: 활성화 조건 `tp1||tp2`, 활성화 시점 스탑가 즉시 초기화 | §4 | ✅ `tp1\|\|tp2\|\|tp3` |
| **P1-3** | TP3 앵커 재정의(진입 시점 고정 기준) 또는 삭제 + 문서 동기화 | §4 | ✅ 진입가 +10% 고정 |
| **P1-4** | SL/TP 재설계 — R:R ≥ 1 (예: 풀백저가 하회 기반 -1.5~2% 손절) | §4 기대값 | ✅ 풀백저가 앵커 + -2% 캡 |
| **P1-5** | 재시작 시 오픈 포지션 존재하면 리스크 루프 자동 재개 | §6 | ✅ 보호 전용(신규 진입 차단) |
| **P1-6** | 11:20 청산 실패 재시도 + 메일/이벤트 알림 | §6 | ✅ 3회 재시도 + 메일 |
| **P2-1** | ~~매도세율 0.15% 갱신~~ → **0.20%** + 슬리피지 반영 | §4 | ✅ 아래 정정 참고 |
| **P2-2** | min-candidates 강제 선정 제거, 갭 floor 상향(≥2% 검토), 점수 왜곡 재점검 | §5 | ✅ 강제선정 제거 + 신호 게이트 / 갭 floor 는 실측 후 재검토 |
| **P2-3** | VI/거래정지 가드 (시세 필드 검사 + 주문 전 상태 확인) | §6 | ✅ 스크리닝 + 진입 직전 2중 |
| **P2-4** | 문서-코드 드리프트 해소 (CLAUDE.md·ADR-0004) | §7 | ✅ 상태머신·Exit Rules·ADR-0004 비율·소스 주석 |
| **P2-5** | PAPER 2~4주 실측(EntryAttempt·TradeEvents 데이터) 후에만 LIVE 논의 | §4·§6 | 🔶 **집계 도구 완비, 운영 대기 — LIVE 전환 전제조건** |

> **⚠ 본 보고서 §4 의 세율 권고 정정:** "2026 실효 0.15%" 는 **2025년 세율**이었다.
> 2026-01-01 이후 양도분부터 코스피 = 증권거래세 0.05% + 농특세 0.15%, 코스닥·K-OTC =
> 0.20%(농특세 없음) → **두 시장 모두 매도측 0.20%**. 코드는 0.0020 으로 반영했다
> ([ADR stock/algorithm/0008](../adr/stock/algorithm/0008-screening-selection-and-cost-model.md)).
> 따라서 왕복 비용은 수수료 0.03% + 세금 0.20% = **0.23%**, 슬리피지 0.2% 포함 시 **0.43%**.

정책·구조 변경(P0-1·2, P1-3·4 등)은 CLAUDE.md 동기화 규칙에 따라 구현 시 ADR 작성 대상.

**P1 이후 손익 구조 변화** (§4 대비): 손절 -5% → 약 **-1.2%**(풀백저가 앵커, 캡 -2%),
TP3·트레일링이 실제 발동 가능해져 잔여 물량 보호가 생겼다. 승리 혼합익 ~+1.1% 기준
손익분기 승률은 **~83% → ~50% 내외**로 내려간다. 다만 -1.2% 손절은 노이즈 손절 빈도를
높이므로, 실제 기대값은 P2-5 실측으로만 확인 가능하다.

## 10. 부록 — 발견 37건 검증 상태 요약

- **적대적 검증 완료 13건 (반박 0)**: 주문실행 관점 전체 — 무인증 API(critical 상향), 체결 픽션 기록,
  주문 재시도, 선영속화 부재, running 플래그, 11:20 원샷, VI 부재, PAPER 과낙관, 세율 구식 등.
- **코드 1차 확인 (본 문서 작성자 직접)**: toDomain 상태 유실, TP1 수량 버그, TP2 선발동 수학,
  TP3 도달불가, 트레일링 이중 결함, min-candidates 강제 선정, 비용 모델 존재·게이트 동작,
  time-decay 15:15, 문서 드리프트.
- **미검증(정황 근거) — 후속 확인 필요**: 갭 floor 0.5% 점수 기여, 유동성 팩터 점수 왜곡,
  거래량순위 저가주 편향, bounce 0.2% 1틱 노이즈, PULLBACK 15분 초과 시 전이 부재,
  ENTRY_READY 타임아웃 부재, 단일 트랜잭션 rollback 전파.
