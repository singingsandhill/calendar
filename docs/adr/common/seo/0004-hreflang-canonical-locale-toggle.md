# ADR-0004: hreflang/canonical + 언어 토글 rel 속성 + og:locale:alternate

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-05-01 |
| 도메인 | common |
| 관심사 | SEO / i18n |
| 관련 커밋 | `docs/git_commit.md` Commit 9 |
| 관련 이슈 | — |

## Context — 무엇이 문제였나

i18n SEO 가 단계적으로 보강됐지만 두 가지 잔여 문제:

1. **og:locale:alternate 누락** — 페이지가 다른 언어로도 존재함을 OG 그래프 측에
   알리는 신호 없음. 페이스북/카카오톡 미리보기에서 한 언어만 노출.
2. **언어 토글 링크의 rel 속성 부재** — 헤더의 KO/EN 토글이 모두 평범한 `<a href="?lang=...">`.
   결과: GSC 가 `?lang=ko` 변종을 *별도 페이지*로 색인 시도 → "알트 페이지" 노이즈.

## Decision — 무엇을 골랐나

토글 링크의 rel 속성으로 SEO 시그널을 분리하고 OG 측에 alternate 추가.

- **`head.html`** — `og:locale:alternate` 추가 (hreflangEnabled=true 일 때만 다른
  언어 알림). 기존 og:locale 와 함께 출력.
- **`header.html` KO 토글** — `rel="nofollow"` → Google 이 `?lang=ko` 변종을 따라가지
  않음 → GSC 알트 페이지 노이즈 제거.
- **`header.html` EN 토글** — `rel="alternate" hreflang="en"` → 영문 색인 보조 신호.
- **`naver-site-verification` 메타** — `seo.naver-site-verification` 프로퍼티가 비어
  있으면 미출력. NAVER_SITE_VERIFICATION 환경변수 폴백.

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| 경로 기반 i18n (`/en/...`) 로 마이그레이션 | URL 정규화 강함 | 라우팅 전반 재설계, 기존 외부 링크 깨짐 |
| 양 토글 모두 `rel="alternate"` | 대칭 | KO 가 기본 페이지(canonical) 와 같으므로 alternate 부여하면 자기 자신 alternate — 의미 불일치 |
| **(선택) KO=nofollow / EN=alternate hreflang=en** | 비대칭 의미 정확 | — |

Google 의 hreflang 권장: 같은 콘텐츠의 다른 언어 변종은 `alternate hreflang=xx`. 단,
*기본 언어와 동일* 한 페이지 변종은 rel 부여 시 중복 신호. nofollow 로 토글 자체를
색인 대상에서 제외.

## Consequences — 영향

- **긍정:**
  - GSC 알트 페이지 노이즈 사라짐.
  - 페이스북/카카오톡 미리보기에서 다른 언어 변종 인식.
  - 네이버 서치어드바이저 수동 검증 진입.
- **부정:**
  - rel 비대칭 정책을 새 토글 추가 시 누락 위험. 코드 리뷰 가드.
- **후속:**
  - ADR-0006 (정적 엔드포인트) 가 naver 검증 파일을 `/naver{hash}.html` 정적 리소스로
    서빙하는 결정과 같이 묶임.

## References

- 관련 코드:
  - `src/main/resources/templates/fragments/head.html`
  - `src/main/resources/templates/fragments/header.html`
  - `src/main/resources/application.yaml`
  - `.env.example`
- 관련 docs: `docs/seo-evolution-playbook.md` (i18n SEO 의사결정 트리)
- 관련 커밋: `docs/git_commit.md` Commit 9
