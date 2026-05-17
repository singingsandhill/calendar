# ADR-0001: Cookie → Accept-Language → KO fallback 로케일 해석

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-04 (i18n 인프라) ~ 2026-05-01 (SameSite 보강) |
| 도메인 | common |
| 관심사 | i18n |
| 관련 커밋 | `cdb552e`(SeoService 초기) → 후속 i18n PR 들 |
| 관련 이슈 | — |

## Context — 무엇이 문제였나

i18n 도입 시 로케일 결정 우선순위를 어떻게 정할지 결정 필요했다.

- **사용자 명시 선택** (헤더 토글 → cookie) 이 *가장 신뢰 가능* — 실제 의도.
- **Accept-Language 헤더** 는 브라우저 시스템 언어. 한국 사용자가 영어 OS 를 쓰는
  경우 의도와 충돌.
- **KO 기본값** — 시스템 fallback 은 ko (한국 사용자가 주력 시장).

Spring 의 `AcceptHeaderLocaleResolver` 만 사용하면 토글 결과가 페이지 새로고침 한
번에 사라짐 — 사용자 의도 무시.

## Decision — 무엇을 골랐나

`CookieThenAcceptLanguageLocaleResolver` 신설 — 쿠키 우선, 헤더 보조, KO fallback.

- **해석 순서:** cookie `lang` → Accept-Language → `ko`.
- **쿠키 정책:** 1년 유지, `SameSite=Lax` (CSRF 회피), `Path=/` (전 도메인).
- **시스템 로케일 폴백 차단** — `MessageSource` 의 `defaultLocale` 을 `ko` 로 명시,
  JVM 시스템 로케일 무시.
- **메시지 파일** — `messages.properties` (한국어, 기본) + `messages_en.properties`
  (영어).

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| Accept-Header 만 | 표준 | 토글 결과 미저장 → 사용자 매번 재선택 |
| 경로 기반 (`/en/`) | URL 정규화 | 라우팅 전반 재설계, 기존 외부 링크 깨짐 |
| **(선택) 쿠키 우선** | 사용자 의도 보존, 헤더 보조 | — |

쿠키 1년 유지의 근거: 사용자가 한 번 토글하면 그 의도가 *영속* — 매번 다시 묻지
않는다. 짧은 TTL 은 모순.

## Consequences — 영향

- **긍정:**
  - 토글 한 번으로 영구 적용.
  - 첫 방문자는 브라우저 언어로 추정.
  - KO 기본값으로 한국 시장 우선.
- **부정:**
  - 쿠키 정책으로 EU GDPR 동의 필요 (현재 분석 도구 쿠키와 함께 처리).
- **후속:**
  - ADR-0002 (즉시 적용) 이 토글 직후 새로고침 한 번에 적용 안 되는 잔여 버그 수정.
  - ADR-0003 (NumberFormat 그룹화 차단) 이 이 시스템 위에서 SEO 메시지 포맷 정합성
    유지.

## References

- 관련 코드:
  - `src/main/java/me/singingsandhill/calendar/common/infrastructure/config/CookieThenAcceptLanguageLocaleResolver.java`
  - `src/main/java/me/singingsandhill/calendar/common/infrastructure/config/WebConfig.java`
  - `src/main/resources/messages.properties`
  - `src/main/resources/messages_en.properties`
- 관련 docs: `CLAUDE.md` (i18n 섹션), `docs/seo-evolution-playbook.md` (i18n 의사결정 트리)
