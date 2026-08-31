# ADR-0003: 재시작 시 보호 전용 자동 재개 (stock/modes/0003 미러)

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-08-17 |
| 도메인 | trading |
| 관심사 | 모드 / 운영 안전 |
| 관련 ADR | [stock/modes/0003](../../stock/modes/0003-protection-only-recovery-on-restart.md) (미러 원본), [trading/infrastructure/0002](../infrastructure/0002-order-pre-persistence-and-tick-sweep.md), [common/infrastructure/0001](../../common/infrastructure/0001-container-restart-deploy-pipeline.md) |
| 관련 이슈 | docs/trading/remaining-work.md P1-3 |

## Context — 무엇이 문제였나

- **재시작 = 보호 상실**: `running` 이 인메모리 `AtomicBoolean(false)` 이고 자동 기동이
  없어, 재배포·불시 재시작(OOM 등) 후 관리자가 `POST /api/trading/bot/start` 를 호출하기
  전까지 OPEN 포지션의 손절·익절·트레일링·TIME_EXIT 감시가 **전부 죽는다**
  (`checkAndExecuteRiskRules` 는 `executeTradeLoop` 안에서만 호출된다). remaining-work
  P1-3 로 문서화된 미해결 항목이었고, 크립토는 24/7 시장이라 주식(11:20 장 마감)보다
  무방비 창이 길다.
- CI/CD 도입(ADR common/infrastructure/0001)으로 재시작이 "가끔 있는 사고"에서 "배포마다
  일어나는 일"이 된다 — 이 공백을 배포 파이프라인의 전제 조건으로 막아야 했다.
- stock 모듈에는 같은 문제의 해법(보호 전용 자동 재개, ADR stock/modes/0003)이 이미
  운영 실적과 회귀 테스트를 갖고 있었다.

## Decision — 무엇을 골랐나

**stock ADR 0003 의 보호 전용 복구 모드를 trading 에 미러링한다.** 크립토는 24/7 이라
거래일·Clock 개념만 없다.

- `@EventListener(ApplicationReadyEvent)` `resumeProtectionOnStartup()`: `bot.enabled` 이고
  해당 마켓 OPEN 포지션이 있으면 `running=true, paused=false, recoveryMode=true` 자동 재개
  + WARN 로그 + `RECOVERY_RESUMED` 이벤트. 플래그를 먼저 세우고 §8-G 기동 스윕은
  best-effort — 스윕 실패가 보호 재개를 무산시키지 않는다. **캔들 초기화는 하지 않는다**
  (ApplicationReadyEvent 리스너는 동기 실행 — 느린 빗썸 백필이 stock 봇의 자동 재개까지
  지연시키고, 리스크 판정은 캔들이 아니라 실시간 현재가를 쓴다).

| 동작 | 복구 모드 |
|---|---|
| SUBMITTED 스윕(0) · 캔들 동기화(1) | 실행 |
| 리스크 체크(2): 손절·익절·트레일링·TIME_EXIT | 실행 |
| 신호 기록(3) — 1분 관측 시계열 (ADR observability/0003) | 실행 |
| **강신호 매매(4) · 리밸런싱(5) · 일반 신호 매매(6)** | **차단** |
| `manualBuy`/`manualSell`/`emergencyClose` (관리자 명시 조작) | 불변 |

- 게이트 위치는 신호 기록(3) **직후** — "리스크 체크 최우선" 불변식
  (`riskCheckRunsBeforeSignalGeneration` 가드)과 청산 틱 신호 기록(ADR observability/0003)을
  둘 다 보존한다. 리밸런싱도 차단한다 — 상승장 70% 목표는 신규 매수를 낼 수 있고 리스크
  보호가 아니다.
- **`start()` 한 번으로 완전 재개**: 자동 재개 상태는 `running=true` 라 기존
  `compareAndSet(false,true)` 로는 영원히 해제 불가(false 반환) — CAS 앞에 recovery 해제
  분기를 두어 Start 1회가 `recoveryMode` 를 풀고 true 를 반환한다. stop→start 2단계를
  강요하면 그 사이 리스크 보호가 끊긴다. stop 경유 경로의 잔존도 CAS 블록에서 해제.
- **가시성**: `BotStatus`/`BotStatusDto` 에 `recoveryMode` 추가(컨트롤러는 수동 매핑이라
  DTO 도 함께), 대시보드 2곳(trading-dashboard.js·trading-globals.js)이 `PROTECTION-ONLY`
  (warn) 로 표기하고 토글 버튼은 Stop 이 아니라 **Start(완전 재개)** 를 제안한다 —
  복구 상태에서 Stop 하면 리스크 감시까지 끊긴다.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| 완전 자동 재개(신규 매매 포함) | 개입 불요 | 재배포가 매매 판단에 무개입이어야 한다는 원칙 위반. 배포 직후는 검증 관찰 구간(StockBotConfigValidator 의 "PAPER 1일 검증 권장"과 같은 철학) |
| 배포 스크립트가 admin API 로 정지→상태 복원 | 앱 코드 무변경 | CSRF 파싱 로그인 의존으로 취약하고, 배포 외 불시 재시작(OOM·전원)은 못 덮는다 — 사용자 선택에서 제외됨 |
| 수동 + 알림만 | 최소 변경 | 보호 공백이 사람 반응 속도에 좌우 — 실계좌 LIVE 에 부적합 |
| running 상태 DB 영속화 | 더 정확 | 스키마·수명주기 복잡도 대비 오픈 포지션 유무 판정으로 충분 (stock 0003 과 같은 결론) |

## Consequences — 영향

- **긍정:** 배포·불시 재시작 후에도 OPEN 포지션 보호가 자동 유지된다. §8-G 기동 스윕이
  "관리자 start 대기"에서 "기동 직후"로 앞당겨져 미결 주문 정합 창도 줄었다.
- **부정:** 재시작 후 관리자가 Start 하기 전까지 신규 진입 기회를 놓친다(의도된
  트레이드오프). 운영자가 **의도적으로 stop 해 둔 상태도 재시작을 넘어 보존되지 않는다**
  — 포지션이 있으면 보호 전용으로 재무장한다(보호 방향이라 수용; 완전 정지가 필요하면
  포지션 청산 또는 `TRADING_BOT_ENABLED=false` 재기동).
- **부정:** `TradingCircuitBreaker.consecutiveLosses` 는 인메모리라 재시작마다 0 으로
  리셋된다 — 자동 재개 도입으로 "연속 손실 중 재배포 → 카운터 초기화 → Start 한 번에
  차단 해제" 경로가 생긴다. `RECOVERY_RESUMED` 이벤트 메시지에 리셋 사실을 명기해
  가시화했고, 카운터 영속화(청산 이력 재계산)는 후속 과제.
- **부정:** cid 미부착 구성(V1+OFF)에서는 선영속화가 꺼져 있어 기동 스윕이 찾을
  SUBMITTED 자체가 없다 — "주문 HTTP 후 커밋 전 kill" 무기록 체결 갭은 이 ADR 로
  해소되지 않는다 (근본 해소는 remaining-work A-3).
- 회귀 가드: `TradingBotRecoveryTest`(7) — 자동재개 3 + 게이트(happy-path 전체 스텁으로
  게이트 부재 시 실주문 도달을 증명) + start 해제 2경로 + 게이트 해제 대조.

## References

- `trading/application/service/TradingBotService.java` (`resumeProtectionOnStartup`, `recoveryMode`, `executeTradeLoop` 게이트, `start()` 해제 분기)
- `trading/presentation/api/BotControlApiController.java` (`BotStatusDto.recoveryMode`)
- `static/js/trading-dashboard.js`·`static/js/trading-globals.js` (PROTECTION-ONLY 표기·토글 분기)
- `src/test/java/.../trading/application/service/TradingBotRecoveryTest.java`
- `docs/trading/remaining-work.md` P1-3
