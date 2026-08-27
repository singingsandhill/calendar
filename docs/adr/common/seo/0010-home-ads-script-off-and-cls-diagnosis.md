# ADR-0010: 홈 adsEnabled(false) — 광고 정책·코드 정합화, GSC 데스크톱 CLS 진단 기록

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-08-17 |
| 도메인 | common (head.html·style.css 는 datedate 를 넘는 공용 자산) |
| 관심사 | SEO, 성능(Core Web Vitals) |
| 관련 커밋 | (git-commit.md Commit 147 참고) |

## Context — 무엇이 문제였나

GSC Core Web Vitals 가 **데스크톱 CLS 0.15 (기준 0.1 초과), 영향 URL 12개**
(예시 `https://datedate.site/`, 2026-06-15 첫 감지)를 보고했다. 진단 결과 이 경고는
**색인 페이지들의 실제 문제가 아니라 GSC 그룹핑 아티팩트**였다:

- 12개 URL = `SitemapService` 인덱싱 대상과 정확히 일치 (`/`, `/guide`, `/about`,
  `/privacy`, `/terms`, `/faq`, `/tools/date-diff`, use-cases 5개).
- **CrUX URL 레벨 (홈): CLS p75 = 0.00** — 데이터 존재 전 기간(2026-05-30~08-15) 매주 0.00.
- **CrUX 오리진 레벨 (전 디바이스): 0.07~0.09.** 오리진×데스크톱 단독은 공개 API 표본 부족(404).
  GSC 는 내부의 더 낮은 문턱 데이터를 쓰며, URL 표본이 부족하면 그룹에 오리진/유사 페이지
  값을 씌운다 → 12개 색인 URL 이 오리진 값 0.15 를 상속받아 표시된 것.
- 랩 재현 (운영 사이트, Windows Chrome 헤드리스 + CDP): 폰트 1.5~3초 지연 주입 +
  스크롤 시나리오 + 9개 URL 에서 **CLS 최대 0.004**. 1000~1920px 10px 간격 93개 폭
  전수 스위프에서 폴백↔웹폰트 렌더의 **줄바꿈 변화 0건, 문서 높이 차 0** (홈·guide·use-case).
  명시적 line-height + 짧은 헤드라인 + 여유 컨테이너 덕에 폰트 스왑이 레이아웃을 못 바꾼다.
  → **폰트 폴백 메트릭 보정(size-adjust 등)은 효과 없음이 실측되어 채택하지 않았다.**
- 오리진 CLS 의 실제 발생원(추정, 코드로 메커니즘 확인): 비색인 데스크톱 페이지 —
  `trading-dashboard.js` 가 15~30초 폴링마다 포지션 목록·테이블을 `innerHTML` 전체
  재구축(높이 가변), stock 대시보드도 동일 폴링 구조. 관리자가 데스크톱에서 장시간
  열어두는 페이지라 저트래픽 오리진의 데스크톱 표본을 지배할 수 있다. 이벤트 로그는
  `max-height:360px` 로 이미 방어돼 있음. **RUM 계측 없이는 확정 불가 — 이번에는 관찰만 결정.**

이 진단 과정에서 코드·문서 불일치를 발견했다: 홈에는 광고 슬롯이 0개이고 문서 3곳
(`templates/fragments/CLAUDE.md` Ad Slot Strategy 표, `docs/audit/adsense-low-value-content-policy-mapping.md`,
`docs/seo/adsense-low-value-content-remediation.md`)이 모두 "홈 광고 없음/금지"라고 서술하는데,
`SeoService.getHomeSeo()` 는 `adsEnabled(true)` 였다. `ADSENSE_CLIENT` 가 주입되면 홈에도
`adsbygoogle.js` 가 로드되고(`head.html` 의 `seo.adsEnabled() and adsense.enabled` 게이트),
AdSense 콘솔에서 자동광고가 켜져 있으면 **예약 공간 없는 DOM 주입으로 CLS 가 실제로 발생**할
경로였다. (현재는 AdSense 미승인·`ADSENSE_CLIENT` 미주입이라 미발현 — 운영 HTML 확인 완료.)

## Decision — 무엇을 골랐나

홈의 `adsEnabled` 를 `false` 로 바꿔 문서화된 광고 정책과 코드를 정합화하고, 페이지별 광고
게재 정책을 테스트로 고정한다. CLS 자체는 코드 수정 없이 관찰한다.

- `SeoService.getHomeSeo()`: `.adsEnabled(true)` → `.adsEnabled(false)`.
- `SeoServiceI18nTest.adsPolicy_perPage` 신설 — 광고 금지(홈·about·privacy·terms) /
  게재(guide·faq·date-diff·use-cases) / insights 데이터 가드까지 전 페이지 정책 고정.
- 폰트 폴백 메트릭 보정·대시보드 렌더 수정·RUM 계측은 이번 범위에서 제외 (아래 Rationale).

## Rationale — 왜 이 선택인가

| 대안 | 장단점 | 기각 이유 |
|---|---|---|
| 폰트 폴백 메트릭 보정 (`size-adjust` 등) | 표준 처방이지만 이 사이트에선 효과 없음 | 93개 폭 전수 스위프 실측으로 줄바꿈 변화 0건 — 개선할 시프트 자체가 없다 |
| 대시보드 `innerHTML` 재구축 수정 | 오리진 CLS 의 유력 발생원 제거 | RUM 확정 근거 없이 실거래 봇 제어 UI 를 추측 수정하는 리스크 > 기대 효과. 색인 페이지 무죄가 확인돼 SEO 실영향도 제한적 |
| RUM 계측 (layout-shift → dataLayer/GA4) | 발생원을 데이터로 확정 | 관찰 우선 결정 — GSC 검증 추이를 먼저 본다 |
| 홈 adsEnabled(false) (선택) | 코드 1줄 + 테스트, 향후 AdSense 재활성화 시 홈 CLS 재발 경로 제거 | — |

## Consequences — 영향

- **긍정:** AdSense 재활성화 시 홈은 스크립트 자체가 로드되지 않아 자동광고 CLS 재발
  경로가 차단된다. 광고 게재 정책이 테스트로 고정돼 회귀가 즉시 잡힌다. 폰트/광고를
  재추적하지 않도록 진단 수치가 이 문서에 남는다.
- **부정:** 향후 홈에 광고를 넣기로 결정하면 이 ADR 을 Superseded 처리하고 슬롯(예약
  높이 포함)과 함께 명시적으로 뒤집어야 한다.
- **후속 — AdSense 재활성화 시 체크리스트:** ① 콘솔 자동광고(앵커/인페이지) OFF 확인,
  ② `ad-slot` 예약 높이 재설계 — leaderboard 는 `data-ad-format="auto"` +
  `full-width-responsive` 인데 예약이 `min-height:90px` 뿐이라 데스크톱 대형 크리에이티브가
  초과 시프트를 만든다(고정 height 또는 min-height 상향, 수익 트레이드오프와 함께 결정),
  infeed(fluid) 도 200px 예약 초과 가능. ③ 게재 개시 후 광고 포함 랩 CLS 재측정.
- **후속 — CLS 관찰:** GSC 에서 수정 검증 시작. CrUX p75 는 28일 롤링이라 판정까지 최대
  28일+α. 오리진 CLS(0.07~0.09, 완만한 상승)가 0.1 을 넘거나 GSC 실패가 지속되면
  RUM 계측(전 페이지 layout-shift source → dataLayer)을 다음 수단으로 검토.

## References

- 관련 코드: `src/main/java/me/singingsandhill/calendar/datedate/application/service/SeoService.java` (getHomeSeo),
  `src/test/java/.../SeoServiceI18nTest.java` (adsPolicy_perPage),
  `src/main/resources/templates/fragments/head.html` (AdSense 게이트),
  `src/main/resources/static/js/trading-dashboard.js` (폴링 재구축 — 관찰 대상)
- 관련 문서: `docs/audit/adsense-low-value-content-policy-mapping.md`,
  `docs/seo/adsense-low-value-content-remediation.md`,
  `docs/troubleshooting/lighthouse-performance-audit.md` (랩 CLS 0.001 — 필드와의 괴리가 이번 진단의 출발점)
- 관련 ADR: [0007 콘텐츠 페이지 AdSense](0007-content-pages-for-adsense.md)
