# sitemap.xml 점검 — 코드 · 라이브 산출물 · 커버리지 (2026-08-02)

> 대상: `SitemapService` / `StaticResourceController#sitemapXml` / `static/robots.txt` 와
> 라이브 산출물 `https://datedate.site/sitemap.xml`.
> 방법: ① 코드 정적 분석 ② 배포본에 실제 HTTP 요청(전 URL 상태코드·헤더·canonical·hreflang·title)
> ③ 공개 라우트 전수 열거 후 사이트맵 수록 범위와 대조.
> 성격: 점검 보고 + 위험 낮은 불일치 정리. 사이트맵 **출력 자체는 이 작업으로 바뀌지 않는다.**
> 관련: [ADR common/seo/0002](../adr/common/seo/0002-http-to-https-unification.md) ·
> [0003](../adr/common/seo/0003-trustworthy-sitemap-lastmod.md) ·
> [0004](../adr/common/seo/0004-hreflang-canonical-locale-toggle.md) ·
> [0005](../adr/common/seo/0005-robots-disallow-narrowing.md),
> [`docs/seo/evolution-playbook.md`](../seo/evolution-playbook.md) (성숙도 모델 L0~L5).

---

## 1. 한 줄 결론

**산출물은 건강하다.** 죽은 URL 0건, 색인 대상인데 누락된 페이지 0건, 색인 대상이 아닌데 실린
페이지 0건, hreflang·canonical 정합 100%. 남은 문제는 **신호 품질 1건(lastmod)** 과 **문서·테스트
공백**이며, 앞의 것은 ADR-0003 이 의식적으로 감수한 트레이드오프다.

## 2. 정상 확인 항목 (실측)

측정 시각 2026-08-02 12:59 KST. 명령은 §6 에 재현 가능한 형태로 있다.

| 항목 | 결과 |
|---|---|
| 수록 URL | 26개 = 13 엔트리 × ko/en. **전부 HTTP 200** — 리다이렉트·404 0건 |
| XML 유효성 | well-formed, `<urlset>` + `xmlns:xhtml` 선언, 12,606 bytes (5만 URL / 50MB 한도와 무관) |
| hreflang | `<url>` 블록당 ko·en·x-default 3개 = 총 78개. 각 블록이 **자기 자신을 포함한** 전체 집합을 발행 → reciprocal 충족 |
| x-default | 26개 전부 존재, ko URL 을 가리킴 |
| canonical 정합 | 표본 5개(`/`, `/?lang=en`, `/guide?lang=en`, `/insights/trends`, `/use-cases/friend-meetup?lang=en`) 의 `<link rel=canonical>` 이 사이트맵 `<loc>` 과 **문자 단위 일치**. en URL 은 self-canonical |
| 언어 실체 | 13개 경로 전부 ko/en 의 `<title>` 과 `<html lang>` 이 다름 — hreflang 이 빈 약속이 아님 |
| 커버리지 | `index,follow` 페이지 집합 == 사이트맵 집합. **누락 0 / 과다 0** |
| 비수록 공개 페이지 | `/runners*` `noindex,follow` · `/stock*` `noindex,nofollow,noarchive` · `/login` `noindex,nofollow` · UGC `noindex,nofollow` — 전부 의도대로 |
| 결정성 | 연속 3회 호출 lastmod 동일. playbook Case 6 이 경고하는 *요청마다 변동* 은 **없음** |
| 서빙 | `Content-Type: application/xml`, `Cache-Control: max-age=86400`, ETag 발급, nginx 200 |
| robots.txt | 배포본이 저장소 파일과 일치. `Sitemap:` 줄 존재 |

## 3. 발견 사항

### 3-1. lastmod 가 빌드 시각이라 콘텐츠 무관 배포에도 전 URL 이 갱신 신호를 낸다 — 중요도 中

`SitemapService` 는 생성자에서 `BuildProperties.getTime()` 을 한 번 잡아 정적 페이지 12개
(= 24 URL) 전부에 같은 값을 쓴다. 즉 lastmod 는 **콘텐츠 변경 시각이 아니라 배포 시각**이다.

측정된 근거 두 가지:

- 점검 도중 값이 `2026-07-26T03:26:02.799+09:00` → `2026-08-02T12:40:54.479+09:00` 로 바뀌었다
  (약 40분 간격 두 번의 요청 사이에 재배포 발생).
- 최근 30일 커밋 52건 중 사이트맵 수록 페이지의 템플릿·메시지 파일을 실제로 건드린 것은 **8건**.
  나머지 배포는 트레이딩/주식 모듈 등 이 13개 페이지와 무관한 변경이지만, 배포되는 순간 24개
  URL 이 모두 "방금 갱신됨"으로 나간다.

영향은 제한적이다 — Google 은 부정확한 lastmod 에 페널티를 주는 게 아니라 **그 값을 무시**한다.
즉 손해는 "신호 하나를 잃는 것"이지 순위 하락이 아니다. playbook Case 6(:408-413) 이 경고하는
증상(GSC 사이트맵 리포트 신뢰도 하락)에 해당하지만, 같은 문서의 안티패턴 3(`LocalDate.now()`,
요청마다 변동)보다는 한 단계 약한 형태다.

**이건 결함이 아니라 선택이다.** ADR-0003 의 Rationale 표가 "페이지별 콘텐츠 hash 변경 시각
추적"을 *가장 정확하지만 인프라 추가* 라는 이유로 기각하고 BuildProperties 를 골랐다. 이번 점검은
그 선택의 비용을 처음으로 수치화한 것이다.

**판단: 보류.** 고칠 경우의 선택지는 셋이다.

| 안 | 장점 | 비용 |
|---|---|---|
| 페이지별 실제 수정일 맵 | 가장 정확. `docs/audit/adsense-low-value-content-policy-mapping.md:91` 의 기존 권고와 일치하고 AS-Content 신선도에도 유리 | 콘텐츠 수정 시 날짜 갱신을 잊는 드리프트. 강제 수단 없음 |
| 정적 페이지 lastmod 제거 | 가장 단순·정직. 모르는 값을 주장하지 않음 | 신선도 신호를 완전히 포기 |
| git 커밋일에서 빌드 시 산출 | 드리프트 없음, 자동 | 빌드 플러밍 추가 — CLAUDE.md 단순성 우선과 상충 |

### 3-2. ADR-0003 · Javadoc 이 코드와 달랐다 — 중요도 中 → **정정 완료**

둘 다 "insights 데이터가 없으면 buildTime fallback" 이라고 적었지만, 코드는
`computeInsightsLastmodIfPresent()` 가 비면 **엔트리 자체를 싣지 않는다**. 2026-05-28 커밋
`061626f` 에서 "sitemap 이 광고하는데 페이지는 noindex" 라는 모순을 없애려고 바뀌었고 ADR 이
따라가지 않았다. ADR 은 각주로 정정하고 Javadoc 은 실제 동작으로 고쳤다.

### 3-3. robots.txt `Allow:` 목록이 반쪽 미러 — 중요도 低 → **정리 완료**

`/about`, `/faq`, `/tools/date-diff` 가 빠져 있었다. default-allow 라 크롤링 동작에는 영향이
없지만, 목록만 보면 이 세 페이지가 허용 대상이 아닌 것처럼 읽힌다. 또 UGC 차단 블록의 주석이
이미 삭제된 `Allow: /runners/runs/` 규칙을 근거로 들고 있었다. 세 줄을 채워 미러를 복원하고
주석을 현재 파일 상태에 맞게 고쳤다.

### 3-4. 회귀 가드 공백 — 중요도 低 → **테스트 4종 추가**

기존 테스트(`SitemapServiceHreflangTest` 14개, `SitemapServiceWhitelistTest` 2개)는 생성된 XML
**문자열**만 본다. 그 XML 이 실제로 서빙되는지, 광고하는 URL 이 존재하는지, robots.txt 에 막히지
않는지는 아무도 확인하지 않았다. 이 저장소의 대표적 색인 사고 두 건이 정확히 그 공백에서 났다 —
2026-01 프로토콜 불일치(ADR-0002), 2026-04 `Disallow: /*/*` 로 콘텐츠 페이지 전면 차단(ADR-0005).

`SitemapEndpointTest` 4종으로 메운다. §5 참조. playbook 미완 체크리스트(:493) "sitemap 의 모든
URL 200 응답 자동 검증"도 이걸로 닫힌다.

### 3-5. 기록만 — 조치하지 않음

| 항목 | 내용 |
|---|---|
| `escapeXml` 적용 범위 | `<loc>` 과 `href` 에만 적용. `changefreq`·`priority`·`lastmod` 는 미적용. 현재 전부 코드 내 리터럴이라 실제 위험 없음 |
| `hreflangEntryCountReasonable` | `12*2*3` 하드코딩. 화이트리스트 테스트가 이미 정확 집합을 고정하므로 중복이고, 페이지 추가 시 두 테스트를 함께 고쳐야 한다. 의도된 가드라 유지 |
| SQL 2회/요청 | `findLatestActivity()` 2건이 사이트맵 응답마다 실행, 캐시 없음. 24시간 캐시 + 저트래픽이라 무시 가능. IndexNow(일 1회)도 같은 경로를 쓴다 |
| Content-Type charset | `application/xml` 에 charset 파라미터 없음. 출력이 전부 ASCII 라 실제 문제 없음 |
| **범위 밖** `/stock*` | noindex 가 `SeoMetadata` 가 아니라 `stock/fragments/header.html` 하드코딩에만 있고, `Disallow: /stock/` 은 맨 URL `/stock` 을 매칭하지 못한다. 공개 경로 3개가 템플릿 한 줄에 의존 — 사이트맵이 아니라 색인 노출 경계 문제라 별건 |
| **범위 밖** 허브 페이지 | `/use-cases`·`/tools` 는 404 (예약 ownerId → 4xx). ADR-0008 이 보류로 남긴 항목 |
| 문서 간 숫자 불일치 | 수록 규모가 문서마다 6→11 / 14 / 24 / 26 / 66 / 72 로 제각각. 단위(엔트리 vs URL vs hreflang 링크)와 시점이 달라 생긴 것이나 어디에도 정합 설명이 없다 |

## 4. 이번에 바꾼 것

| 파일 | 변경 |
|---|---|
| `docs/adr/common/seo/0003-...md` | insights fallback 문장에 정정 각주 + References 에 이 보고서 |
| `common/application/service/SitemapService.java` | 클래스 Javadoc lastmod 정책 문장 정정 (주석만) |
| `static/robots.txt` | `Allow:` 3줄 추가, 스테일 주석 1줄 수정 |
| `common/presentation/controller/SitemapEndpointTest.java` | 신설 (4 테스트) |

## 5. 추가된 회귀 가드

| 테스트 | 막는 사고 |
|---|---|
| `GET /sitemap.xml` → 200 / `application/xml` / `max-age=86400` | 엔드포인트 자체가 깨지거나 Content-Type 이 바뀌는 회귀 (ADR-0006 의 ads.txt 사고와 같은 유형) |
| 모든 `<loc>` 이 2xx | 라우트를 지우거나 경로를 바꾸면서 사이트맵을 안 고치는 경우 — 404 를 광고하게 됨 |
| robots.txt 가 사이트맵 URL 을 막지 않음 | `Disallow: /*/*` 재발 (ADR-0005). Allow/Disallow **최장 패턴 우선** 규칙을 그대로 구현해 판정 |
| `Sitemap:` 줄 == `app.base-url` | 2026-01 http/https·www 불일치 재발 (ADR-0002) |

앞의 두 개는 도입 전 RED 를 확인했다: 존재하지 않는 경로를 사이트맵에 넣으면 "302, 200~299 기대"
로 실패하고, robots.txt 에 해당 경로 `Disallow` 를 넣으면 "차단한다 → true" 로 실패한다.

## 6. 재현 방법

```bash
# 라이브 산출물
curl -sS -D- -o sitemap.xml https://datedate.site/sitemap.xml     # 헤더 + 본문
grep -o '<loc>[^<]*' sitemap.xml | sed 's/<loc>//' | while read u; do
  printf "%s %s\n" "$(curl -sS -o /dev/null -w '%{http_code}' "$u")" "$u"; done   # 전 URL 상태코드
grep -o '<lastmod>[^<]*' sitemap.xml | sort -u                    # lastmod 종류
curl -sS https://datedate.site/guide?lang=en | grep -i canonical   # 페이지 ↔ 사이트맵 정합

# 저장소
cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test --tests me.singingsandhill.calendar.common.presentation.controller.SitemapEndpointTest --tests me.singingsandhill.calendar.common.application.service.SitemapServiceHreflangTest --tests me.singingsandhill.calendar.common.application.service.SitemapServiceWhitelistTest"
```

측정 결과(2026-08-02): 20 테스트 통과 (신규 4 + hreflang 14 + whitelist 2), 라이브 26 URL 전부 200.

## 7. 남은 과제

| 항목 | 상태 |
|---|---|
| lastmod 설계 (§3-1) | **보류** — 위 3안 중 선택 필요 |
| `/stock*` noindex 를 `SeoMetadata` 로 이관 (§3-5) | 미착수, 별건 |
| `/use-cases`·`/tools` 허브 신설 | ADR-0008 보류 항목. 만들면 breadcrumb 3단계 복원 가능 |
| robots.txt 연도 열거가 2035 에 만료 | ADR-0005 가 인지, 알람 없음 |
| GSC 사이트맵 리포트 상태 | 저장소 어느 문서에도 기록 없음 — 제출 성공/색인 수치를 한 번 남겨둘 것 |
