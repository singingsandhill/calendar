# ADR-0002: HTTP → HTTPS 4곳 통일

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-01-06 |
| 도메인 | common |
| 관심사 | SEO |
| 관련 커밋 | `19f9286` |
| 관련 이슈 | #18 |

## Context — 무엇이 문제였나

GSC (Google Search Console) 에 "리디렉션이 포함된 페이지" 경고가 누적. 원인은
**4개 다른 위치**에서 프로토콜이 불일치:

| 위치 | 상태 |
|---|---|
| `robots.txt` | `http://datedate.site/sitemap.xml` |
| `application.yaml` | `app.base-url: http://datedate.site` |
| `SeoService.java` (default) | `http://datedate.site` |
| nginx server_name / 정적 템플릿 일부 | http 노출 |

크롤러는 http → 301 → https 체인을 *원본 URL 의 신뢰 저하* 로 해석. 한 곳만 통일
해도 다른 곳이 http 면 효과 0.

## Decision — 무엇을 골랐나

4곳을 동시에 https 로 통일.

- `robots.txt` 의 `Sitemap:` URL → `https://`.
- `application.yaml` 의 `app.base-url` → `https://`.
- `SeoService` 의 fallback / default 상수 → `https://`.
- nginx — http :80 은 http→https 단방향 리디렉션 만, 그 외 콘텐츠 응답 X.

동시 커밋 1 회로 통일 — 부분 통일 시도 시 회귀 위험.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| nginx 만 https 강제 | 단순 | robots/yaml 의 http 가 그대로 노출되어 신호 일관성 깨짐 |
| 환경변수 한 곳에서 주입 | DRY | 정적 파일(robots.txt) 은 환경변수 치환 어려움 — 빌드 타임 처리 부담 |
| **(선택) 4곳 모두 명시 https** | 중복이지만 검증 용이 | — |

## Consequences — 영향

- **긍정:**
  - GSC "리디렉션 포함된 페이지" 경고 해소.
  - hreflang/canonical(ADR-0004) 도 모두 https 전제 → 후속 SEO 결정 단순화.
- **부정:**
  - 4곳을 한 결정으로 묶어 관리. 신규 호스팅 환경 추가 시 4곳 모두 갱신해야 함 —
    회귀 테스트(ADR-0003 의 sitemap 검증 테스트 중 "https only" 단언) 가 가드.
- **후속:**
  - ADR-0003 (sitemap lastmod) 의 회귀 테스트가 *모든 loc 은 https* 단언으로 이 결정
    영구화.

## References

- 관련 코드:
  - `src/main/resources/application.yaml`
  - `src/main/resources/static/robots.txt`
  - `src/main/java/me/singingsandhill/calendar/common/application/service/SeoService.java`
- 관련 docs: `docs/seo-evolution-playbook.md#23-위기-1--httphttps-혼재`, `docs/troubleshooting/google-search-console-redirect.md`, `docs/troubleshooting/nginx-configuration.md`
- 관련 커밋: `git log -1 19f9286`
