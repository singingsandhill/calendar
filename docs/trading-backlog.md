# 코인 트레이딩 미완 작업 백로그 (2026-07-07)

> **전체 계획 대비 "아직 안 한 것" 전부.** 완료된 것은 경계 확인용으로만 §1에 요약.
> 근거 문서: [운영 감사](audit/coin-trading-operational-review-2026-07-06.md)(발견·근거·라인),
> [v2 마이그레이션 계획](trading-bithumb-v2-migration-plan.md)(단계·게이트·안전요구),
> [세션 인계](trading-v2-migration-handoff.md)(§6 스윕 상세 설계, §7 테스트 패턴).
> 각 항목은 이 문서의 요약만으로 착수 가능하되, 상세 설계는 참조 문서 섹션을 따른다.

---

## 1. 완료 경계 (재작업 금지)

| 완료 | 커밋 |
|---|---|
| P0-1 무인증 실주문 API 차단 (+회귀 테스트, ADR 0003) | `20a9aac`, `4fa259d` |
| 감사·v2 계획·인계·트러블슈팅 문서 | `f699d33` |
| §8-A 모드게이트 확장 / P0-2 파사드 멱등 재조회(기본 OFF) / §8-C 실체결량 / v2 어댑터+라우팅(기본 V1) / MockWebServer 의존성 | **미커밋** — 아래 §2-0 |

---

## 2. 코드 작업 (Claude 실행 가능)

### 2-0. [즉시] 잔여 커밋 실행 — Section T Commit 65~67
- **상태:** 코드·테스트는 완료(전체 스위트 GREEN 확인됨)이나 커밋만 미실행. 신규 파일 5개가 untracked(`??`).
- **방법:** `docs/git_commit.md` Section T 의 Commit 65/66/67 `git add`+`git commit` 그대로 실행.
  실행 전 `[수정·선재확인]` 파일(`build.gradle`·`TradingProperties`·`BithumbApiClient`·`BithumbPrivateApi`·`TradingBotService`)은
  `git diff` 로 이번 델타만인지 확인(