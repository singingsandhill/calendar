# 코인 트레이딩 — 전체 계획 잔여 작업 백로그 (2026-07-08 기준)

> **목적:** 운영 감사(P0~P3) + Bithumb v2 마이그레이션 계획 전체에서 **아직 안 된 부분만** 한 곳에 모은 인덱스.
> 각 항목의 상세 근거·영향·개선방향은 원문서에 있고, 여기서는 **상태·담당·난이도·포인터**만 관리한다.
> 원문서: [운영 리뷰](audit/coin-trading-operational-review-2026-07-06.md) ·
> [v2 마이그레이션 계획](trading-bithumb-v2-migration-plan.md) · [세션 인계](trading-v2-migration-handoff.md).
> 담당 표기: **[dev]** 코드 작업(다음 세션) / **[live]** 실계좌 접근 필요(사용자 실행).

---

## 0. 완료된 것 (참고 — 여기 목록에서 제외)

| 항목 | 닫은 작업 |
|---|---|
| **P0-1** 무인증 실주문·봇제어 API | ROLE_ADMIN 분리 (commit 20a9aac, ADR common/security/0003) |
| **P0-2** 타임아웃→중복주문 | v1 멱등 재조회 + v2 어댑터 + §8-B 선영속화·틱스윕·Position 생성 (코드 완료, **활성화만 대기** — 아래 A 참고) |
| **P1-1** 체결기록 실패 상태 불일치 | §8-B 선영속화+스윕이 미추적 체결 수습(매수·매도) |
| **P1-9(일부)** 체결수량 유도값 | §8-C executed_volume 실측화 (defaults 정합은 아래 B 잔존) |
| **P2-1** 지정가 모드게이트 우회 | §8-A 모드게이트를 지정가·취소·미결조회까지 확장 |

---

## A. v2 마이그레이션 잔여 (계획 §5)

P0-2 코드는 완료됐으나 **기본 구성(order-api-version=v1 + clientOrderIdEnabled=OFF)에서 운영 동작 불변**이다.
실제 이득(멱등키·v2)을 켜려면 아래 라이브 게이트를 통과해야 한다.

| # | 작업 | 담당 | 상태 | 포인터 |
|---|---|---|---|---|
| A-1 | **Phase 0a** — 봇 계정으로 `GET /v1/orders/chance` 실측(maker/taker 4필드·min_total) + 라이브 v1 주문 JSON fixture 캡처 | **[live]** | 미착수 | 계획 §5 Phase 0a |
| A-2 | **Phase 2** — `order-api-version=v2` 소액 라이브 왕복(생성→정규화→체결확인→취소) 검증. 실패 시 v1 원복 | **[live]** | 미착수 | 계획 §5 Phase 2, §8 게이트 |
| A-3 | **Phase 3** — 정량 게이트 통과 후 기본 `order-api-version=v2` + `clientOrderIdEnabled=ON` 전환 + CLAUDE.md·ADR Status 확정 | **[live]** | 미착수 | 계획 §5 Phase 3 (게이트: 왕복 각 10건+·중복 0·현재가폴백 0·재조회성공률 ≥95%) |
| A-4 | **Phase 4** — Private WS v2 `myOrder`/`myAsset` 관측·경보 채널(REST가 유일 진실원, WS 승격 금지). 장애 격리, 재연결 백오프, myAsset 드리프트 경보 | **[dev]** | 미착수 (**Phase 3 이후**) | 계획 §5 Phase 4 (게이트가 LIVE 관찰이라 선행 구현 보류) |
| A-5 | (검증 확정 후) 정규화 시 v2 `order_id`가 `GET /v1/order?uuid=` 로 조회되는지, 동일 client_order_id 중복 에러코드 실측 | **[live]** | 미확인 | 핸드오프 §4 "불확실" |

> **불확실(라이브에서 확정):** v1 생성의 client_order_id 지원 여부, 중복키 에러코드, Bithumb 실 maker/taker 차등,
> v2 order_id↔v1 uuid 조회 호환.

---

## B. 감사 P0/P1 미해결

| ID | 문제 | 담당 | 난이도 | 상태 | 개선 방향 (상세: 운영리뷰 §3) |
|---|---|---|---|---|---|
| **P0-3** | `enabled=false` 킬스위치가 수동 매수/매도 API를 못 막음 | [dev] | 하 | **구현됨·미검증** (2026-07-09, interop 다운으로 테스트 미실행) — manualBuy/manualSell 을 `bot.enabled` 로 게이트(emergencyClose 는 안전청산이라 예외) | — |
| **P1-2** | 캔들 동결 — 미완성 봉이 확정봉으로 영구 고정 가능 | [dev] | 중 | 미해결 | 최신 1~2봉 upsert 갱신 또는 직전 확정봉까지만 저장. 형성봉 여부 로그 검증 선행 |
| **P1-3** | 재시작 시 봇 정지 복귀 → OPEN 포지션 손절 보호 공백 | [dev] | 중 | **부분** (§8-G 기동 스윕은 in-flight SUBMITTED만 수습) | `running` 상태 영속화+부팅 복원, 또는 OPEN 포지션 있으면 리스크 루프 항상 실행 |
| **P1-4** | 수동/검증(test-order) 주문이 서킷브레이커·노출상한·물타기 가드 우회 | [dev] | 중 | **부분 구현됨·미검증** (2026-07-09) — `manualBuy` 에 `entryRiskGuardsBlock`(서킷·물타기·노출) 적용. **`test-order`(TradingVerificationApiController)는 별개 컨트롤러라 아직 미적용** | test-order 는 운영 프로파일 비활성 or 동일 가드 추가 (후속) |
| **P1-5** | 손절/익절/트레일링 트리거(`checkPositionRisk`) 테스트 0건 + Clock 미주입 | [dev] | 중 | 미해결 (TimeExit만 테스트) | `Clock` 빈 주입(stock 모듈 선례) → 경계값 결정성 테스트 |
| **P1-6** | 리밸런스 쿨다운 check-then-act 비원자 → 이중 리밸런스 | [dev] | 하 | 미해결 | 종목별 `ReentrantLock`/CAS 로 진입 직렬화 |
| **P1-7** | 서킷브레이커 연속손실 스트릭 인메모리 유실(일일손실 가드는 DB 유지) | [dev] | 중 | 미해결 | 부팅 시 당일 CLOSED 포지션으로 스트릭 재계산 |
| **P1-8** | 운영 모드 기본값 LIVE + 설정 누락/오타 시 실주문 폴백 | [dev] | 하 | 미해결 (ADR 수용 트레이드오프) | 안전측 기본(PAPER) 또는 이중 플래그(mode=LIVE AND armed). ADR 재검토 수반 |
| **P1-9** | `TradingProperties` 자바 기본값 ↔ yaml 대폭 불일치(키 누락 시 구식 고위험값 폴백) | [dev] | 하 | **부분** (executed_volume은 §8-C 완료) | 자바 기본값을 유효값과 일치 또는 필수키 미설정 시 부팅 실패 |

---

## C. 감사 P2 미해결 (전략 신뢰도·유지보수성)

상세: 운영리뷰 §3 🟡. (P2-1 지정가 모드게이트는 §8-A로 닫힘.)

| ID | 문제 | 난이도 |
|---|---|---|
| P2-2 | 약한 SELL 신호가 수익률 게이트에 종속(강한 SELL은 -2%까지 가능) | 중 |
| P2-3 | 급락장 선제 중단 장치 없음(손실 확정 후에만 후행 차단) | 중 |
| P2-4 | 청산 실패 백오프(5/30분) 동안 해당 포지션 손절 판정 자체 정지 | 하 |
| P2-5 | 운영 알림 채널 부재(CRITICAL도 DB+로그만) | 중 |
| P2-6 | `@Transactional` 안 HTTP 잔존(캔들 수집·계좌 스냅샷) | 하 |
| P2-7 | `TradingCircuitBreaker.consecutiveLosses++` 비원자(volatile int) | 하 |
| P2-8 | 캔들 동기화 vs 트레이딩 루프 동시 insert race → 유니크 위반이 틱 중단 | 하 |
| P2-9 | 판단 지점별 기준가 혼용(신호=DB캔들, 주문·리스크=호가중간값) | 중 |
| P2-10 | 거래소 포트 부재 / 단일 심볼 고정(멀티심볼 확장 불가) | 상 |
| P2-11 | `TradingBotService` 부분 god class + 체결가/수수료 파서 3중 복제 | 중 |
| P2-기타 | 하드코딩 매직넘버(±60·5000 등) 설정 미분리 / 사전 rate-limit 스로틀 부재 / 스토캐스틱 %D=%K 표준 불일치 / dev·prod 프로파일 미분리(운영 SQL로깅·h2-console) / 거래량 다이버전스 오실레이터 프레임+±20 가중 | 중 |

---

## D. 감사 P3 미해결 (개선하면 좋은 것)

상세: 운영리뷰 §3 🟢.

- 주석·문서 수치 드리프트 (하)
- 스케줄러 계층(CandleScheduler/DailySummaryScheduler) 테스트 0건 (하)
- 거래 이벤트 전용 로그 격리 부재(stock은 `stock-events.log` 분리) (중)
- 테스트가 리플렉션·null 대량주입 의존(리팩터링 취약) (하)
- 리밸런스 매수 체결가 폴백이 재시도 없이 사전조회가 사용 → SL/TP 어긋난 포지션 (하)

---

## E. 권고 우선순위 (다음 [dev] 세션)

1. **P0-3 + P1-4 (묶어서)** — 수동/검증 주문에 킬스위치 + 리스크 가드 통과. 무인증은 P0-1로 막혔지만, 인증된
   관리자 오조작·검증 경로의 실거래 안전판. 난이도 하~중, 즉효.
2. **P1-9 잔여** — TradingProperties 자바 기본값 정합(또는 필수키 미설정 부팅 실패). 난이도 하, 설정 누락 사고 예방.
3. **P1-5** — `Clock` 주입 + 손절/익절/트레일링 트리거 테스트. 자본 보호 핵심 로직 회귀 안전망.
4. **P2-4 / P2-7 / P2-8 / P2-6** — 작고 안전한 안정성 수리 묶음(백오프 중 손절정지, 비원자 카운터, 캔들 race, tx 내 HTTP).
5. **P1-2 / P1-3** — 캔들 동결 + 재시작 보호(중간 규모).
6. **P2-5 알림 채널** — CRITICAL 이벤트 푸시(stock `StockMailService` 패턴 재사용).
7. 나머지 P2(전략·구조)·P3는 라이브 전환(A) 이후 데이터 기반으로.

> **A(라이브 전환)와 B~D는 독립적.** A는 [live] 사용자 실행, B~D는 [dev]가 언제든 착수 가능.
> 단 P1-8(모드 기본값)은 A-3(v2 전환) 결정과 함께 재검토하는 것이 자연스럽다.

---

## F. 커밋 상태 (미커밋 정리 위치)

- 2026-07-06 분(P0-1·문서·P0-2 v1·§8-A/C·Phase1 착수): `docs/git_commit.md` **Section T** (commit 62~67).
  이 중 62~64(P0-1·문서)는 이미 커밋됨(git log 20a9aac/4fa259d/f699d33), 65~67(주문신뢰성)은 미커밋.
- 2026-07-08 분(§8-B 본체): **Section U**. 잔여 마감(매도 확장·422·지정가 cid·기동 스윕·프리픽스): **Section V**.
- 워킹트리에 선재 미커밋 변경이 광범위하게 섞여 있으니, 커밋 전 파일별 `git diff` 로 이번 델타만인지 확인.
  이 환경은 `git add -p` 미지원 → 파일 단위 커밋만 가능.

---

## G. 참조

- 감사 원문(P0~P3 상세): [`audit/coin-trading-operational-review-2026-07-06.md`](audit/coin-trading-operational-review-2026-07-06.md)
- v2 계획(단계·게이트·안전요구): [`trading-bithumb-v2-migration-plan.md`](trading-bithumb-v2-migration-plan.md)
- 다음 세션 인계(빌드·테스트·설계 상세): [`trading-v2-migration-handoff.md`](trading-v2-migration-handoff.md)
- ADR: common/security/0003(P0-1), trading/infrastructure/0002(§8-B 선영속화·스윕)·0003(v2 마이그레이션)
- 커밋 큐: `docs/git_commit.md` Section T/U/V
