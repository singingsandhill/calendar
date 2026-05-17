# ADR-0007: 콘텐츠 페이지 (`/guide`, `/use-cases/*`) 추가 — AdSense + thin-content 회피

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2025-12-19 (#14) → 2026-04-07 확장 (#69b9919) |
| 도메인 | common |
| 관심사 | SEO / 콘텐츠 |
| 관련 커밋 | `71ab65e`, `69b9919` |
| 관련 이슈 | #14 |

## Context — 무엇이 문제였나

AdSense 거절 통보의 핵심 이유는 *thin content* — 폼 + CTA 만 있는 랜딩 페이지로는
"사용자가 머물 만한 콘텐츠" 기준 미달. 같은 신호를 GSC 도 본다 — 색인 가치 없음
판정.

`71ab65e refactor: 구글 에드센스 정책 준수 #14` 시점, 인덱스에 232줄의 자체 콘텐츠
추가만으로는 부족. 다양한 시나리오 페이지 + 사용 가이드가 필요했다.

## Decision — 무엇을 골랐나

콘텐츠 페이지를 도메인의 일부로 도입하고 sitemap/내부 링크에 통합.

- **`/guide`** — 사용 가이드 페이지.
- **`/use-cases/*` 4개 페이지** — 친구 모임 / 팀 회의 / 여행 계획 / 스터디 그룹 등
  슬러그 기반 콘텐츠 마케팅 페이지.
- **JSON-LD 강화** — Organization / BreadcrumbList / HowTo 스키마 추가.
- **내부 링크 강화** — 네비게이션, 푸터 섹션화, 시나리오 카드 링크.
- **sitemap 6 → 11** (ADR-0005 의 robots Disallow 좁히기와 같은 PR).

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| 인덱스만 강화 | 변경 최소 | 단일 페이지 → 검색 엔진/AdSense 의 콘텐츠 다양성 신호 부족 |
| 외부 블로그 (medium 등) 운영 | 호스팅 부담 0 | 도메인 권위가 메인 사이트로 누적되지 않음 |
| 외부 CMS 연동 | 콘텐츠 관리 편함 | 인프라 추가, 단일 인스턴스 전제와 충돌 |
| **(선택) Thymeleaf 정적 페이지 + 슬러그 라우팅** | 자체 도메인 권위, JSON-LD 적용 가능 | — |

`/use-cases/{slug}` 는 미래 확장을 의식해 슬러그 기반으로 설계 — 새 시나리오 추가
시 라우팅 수정 없이 콘텐츠만 추가.

## Consequences — 영향

- **긍정:**
  - AdSense 거절 회복.
  - GSC 색인 실패 해소 (ADR-0005 의 robots 좁히기와 같이 작동).
  - HowTo 스키마로 리치 결과(검색 결과의 단계별 표시) 가능.
- **부정:**
  - 콘텐츠 페이지 운영 부담 — 시즌성 정보 갱신.
- **후속:**
  - ADR-0001 (SeoMetadata SSOT) 가 페이지별 메타 분기를 한 곳에서 처리.
  - ADR-0003 (sitemap lastmod) 가 콘텐츠 페이지 lastmod 를 BuildProperties 로 통일.

## References

- 관련 코드:
  - `src/main/resources/templates/guide.html`
  - `src/main/resources/templates/use-cases/detail.html`
  - `src/main/java/me/singingsandhill/calendar/datedate/presentation/controller/UseCaseController.java`
- 관련 docs: `docs/seo-evolution-playbook.md` (콘텐츠 폭발 단계)
- 관련 커밋: `git log -1 71ab65e`, `git log -1 69b9919`
