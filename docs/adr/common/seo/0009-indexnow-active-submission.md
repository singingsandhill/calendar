# ADR-0009: IndexNow 능동 색인 제출 — opt-in 일 1회 배치, 전 구간 fail-soft

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-05-18 |
| 도메인 | common |
| 관심사 | SEO / 외부 연동 |
| 관련 커밋 | `cb257f0` |
| 관련 이슈 | — |

## Context — 무엇이 문제였나

ADR-0003 (lastmod 신뢰도), ADR-0004 (hreflang), ADR-0005 (robots Disallow 좁히기) 로
*크롤 가능성* 은 정리됐지만, 변경을 **알리는** 수단은 여전히 크롤러의 자발적 재방문에만
의존했다. 사이트맵을 고쳐도 검색엔진이 다시 긁어갈 때까지 색인이 갱신되지 않는다.

IndexNow 는 이 지연을 줄이는 능동 통보 프로토콜이고 Bing / Yandex / Naver 계열이 참여한다.
**Google 은 미참여**라 주채널(Search Console + sitemap.xml)을 대체하지 못하고 보조 채널로만
쓸 수 있다.

도입 시점의 제약 두 가지:

1. **실패가 전파되면 안 된다.** 색인 보조 채널이 스케줄러 스레드에서 예외를 던지면
   같은 스케줄러에 얹힌 다른 잡이나 배포가 흔들린다. 얻는 것(색인 며칠 단축)에 비해
   잃는 것이 크다.
2. **잘못 켜지면 개발 인스턴스가 오염원이 된다.** 제출 URL 은 `app.base-url` 기준으로
   만들어지므로, 로컬에서 켜면 개발 머신이 실서비스 URL 을 제출한다.

## Decision — 무엇을 골랐나

사이트맵 URL 전량을 **매일 03:30 KST 한 번** POST 하는 opt-in 배치. 실패는 전부 로그로만
흡수한다.

- **opt-in 기본 `false`** — `indexnow.enabled` 가 `true` 일 때만
  `@ConditionalOnProperty` 로 `IndexNowScheduler` **빈 자체를 등록**한다. 플래그가 꺼져
  있으면 스케줄 등록도, 서비스 호출도 없다. `IndexNowService` 는 별도로 `enabled()` 를
  한 번 더 확인해 직접 호출 경로도 막는다.
- **일 1회 배치** — `SitemapService.getSitemapEntries()` 의 URL 전량 + `bilingual` 엔트리의
  `?lang=en` 확장을 `LinkedHashSet` 으로 중복 제거해 **단일 POST** 로 보낸다.
- **호스트 필터** — `indexnow.host` 와 호스트가 다른 URL 은 제출 전에 탈락시킨다.
  전부 탈락하면 HTTP 호출 없이 WARN 만 남기고 끝낸다.
- **전 구간 fail-soft** — `onStatus(status -> true)` 로 상태 코드 예외화를 억제하고,
  바깥을 `try/catch (Exception)` 으로 감싼다. 어떤 실패도 throw 하지 않는다. 대신
  200/202 · 400 · 403 · 422 · 429 를 분기해 원인이 로그에서 바로 읽히게 한다.
- **키 파일은 명시 엔드포인트** — 정적 리소스 핸들러가 아니라 `StaticResourceController` 의
  `@GetMapping(produces = TEXT_PLAIN)` 으로 노출한다 (ADR-0006 과 같은 이유).

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| 도입하지 않음 (GSC·sitemap 만) | 무비용, 유지보수 0 | Bing·Naver 계열 색인 지연을 방치. 키 파일 배포만으로 얻을 수 있는 채널을 버림 |
| 변경 이벤트마다 즉시 제출 | 신선도 최고 | 트리거 소스가 없다 — 사이트맵에 오르는 건 정적 페이지뿐이고(UGC 는 ADR-0001 에 따라 제외) 그 lastmod 는 빌드 시각이다. 이벤트가 발생하는 지점이 사실상 배포뿐이라 배치와 차이가 없고, 429 위험만 얹힌다 |
| **(선택) 일 1회 배치 + opt-in + fail-soft** | 신선도 충분, 실패 영향 0, 오작동 시 플래그 하나로 차단 | — |

일 1회로 충분한 근거: 사이트맵 엔트리는 13개뿐이고(정적 페이지 7 + use-case 5 +
`insights/trends` 1) 그중 12개의 lastmod 는 빌드 시각이라 배포 때만 움직인다.
IndexNow 는 동일 URL 재제출에도 관대하다.

fail-soft 를 예외 처리가 아니라 *정책* 으로 둔 이유: 이 채널은 실패해도 주채널(GSC)이
그대로 돌아간다. 보조 채널의 실패가 운영 신호를 시끄럽게 만들 이유가 없다. 대신 상태
코드별 메시지를 다르게 해 "조용하지만 진단 가능"하게 만들었다.

## Consequences — 영향

- **긍정:**
  - Bing / Yandex / Naver 계열에 사이트맵 변경을 능동 통보.
  - 403(키 파일 접근 불가) / 422(host 불일치) / 429(과다 제출) 가 각각 다른 메시지로
    남아 오설정이 로그 한 줄로 판별된다.
  - 플래그가 꺼져 있으면 빈 자체가 없어, 실수로 개발 환경에서 도는 일이 구조적으로 막힌다.
- **부정:**
  - **운영 플래그에 전적으로 의존한다.** 코드·키·키 파일이 모두 준비돼 있어도
    `INDEXNOW_ENABLED` 를 켜지 않으면 아무 일도 일어나지 않는다. 실제로 이 상태가
    도입 후 오래 방치됐고 `docs/data-analysis/04-todo.md` P1-4 로만 추적됐다.
  - `indexnow.host` 와 `app.base-url` 이 **독립 설정**이라 어긋나면 전량 탈락하고
    WARN 한 줄만 남는다. 둘은 항상 같이 움직여야 한다.
  - 수동 트리거 엔드포인트가 없다 → 플래그를 켠 뒤 첫 제출까지 최대 24시간.
  - `?lang=en` 확장 로직이 `SitemapService.appendLangEn` 과
    `IndexNowService.collectSitemapUrls` 두 곳에 중복 구현돼 있다.
- **후속:**
  - 키를 교체하려면 5곳을 동시에 고쳐야 한다 — `application.yaml` 의 `key`/`key-location`,
    `static/` 아래 키 파일명, `StaticResourceController` 의 `@GetMapping` 경로,
    `SecurityConfig` 의 `permitAll` 목록, `IndexNowServiceTest` 의 상수.
  - 콘솔 등록(Bing Webmaster Tools 등)은 제출의 전제조건이 아니라 결과 확인 수단이다.
    제출 자체는 `keyLocation` 의 키 파일 검증으로 성립한다.

## References

- 관련 코드:
  - `src/main/java/me/singingsandhill/calendar/common/application/service/IndexNowService.java`
  - `src/main/java/me/singingsandhill/calendar/common/infrastructure/scheduler/IndexNowScheduler.java`
  - `src/main/java/me/singingsandhill/calendar/common/infrastructure/config/IndexNowProperties.java`, `IndexNowConfig.java`
  - `src/main/java/me/singingsandhill/calendar/common/presentation/controller/StaticResourceController.java` (키 파일 엔드포인트)
  - `src/main/java/me/singingsandhill/calendar/common/infrastructure/config/SecurityConfig.java` (키 경로 permitAll)
  - `src/main/resources/application.yaml` (`indexnow.*`)
  - `src/test/java/me/singingsandhill/calendar/common/application/service/IndexNowServiceTest.java`
- 관련 ADR: [0001 SeoMetadata SSOT](0001-seo-metadata-as-ssot.md) (사이트맵 수록 범위),
  [0003 Sitemap lastmod 신뢰도](0003-trustworthy-sitemap-lastmod.md),
  [0006 명시적 정적 엔드포인트](0006-explicit-static-endpoints.md) (키 파일 노출 방식)
- 관련 docs: `docs/data-analysis/04-todo.md` P1-3 / P1-4
- 관련 커밋: `git log -1 cb257f0`
