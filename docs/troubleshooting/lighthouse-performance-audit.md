# Lighthouse 성능 / Best Practices 감사 (datedate.site)

`https://datedate.site` 1차 Lighthouse 측정 결과 분석과 적용한 수정, 그리고
보류한 후속 항목을 정리한다.

---

## 측정 결과 요약

### Performance Metrics

| 메트릭 | 값 | 평가 |
|--------|-----|------|
| First Contentful Paint | 1.0 s | 양호 |
| Largest Contentful Paint | 1.2 s | 양호 |
| Total Blocking Time | **200 ms** | 개선 필요 (<100 ms 권장) |
| Cumulative Layout Shift | 0.001 | 우수 |
| Speed Index | 1.6 s | 양호 |

Core Web Vitals 자체는 통과 수준이지만 페이로드와 TBT 가 병목.

### 주요 진단 항목 (Performance)

| 항목 | 수치 | 영향 |
|------|------|------|
| 페이로드 합계 | 2,987 KiB | LCP / 모바일 데이터 |
| `PretendardVariable.woff2` (jsdelivr) | **2,009 KiB** | 단일 자산이 전체의 67% |
| Google Fonts (Noto Serif KR) | 227.9 KiB (4 개 woff2) | 영어 사용자에게도 로드 |
| Google Fonts unused CSS | 56 KiB | 사용하지 않는 unicode-range 청크 |
| `manifest-*.json` critical path | **2,343 ms** | 0.75 KiB 파일이 LCP 직전까지 점유 |
| Long main-thread task (datedate.site) | 595 ms | TBT 가산 |
| 1st-party JS/CSS 캐시 TTL | 7 d | 해시 파일명임에도 짧음 |

### Best Practices

- **3rd-party cookies: 31 개**
  - `pagead2.googlesyndication.com` (AdSense): `__mggpc__`, `IDE`, `DSID`, `ar_debug`
  - `www.google.com/api2/aframe` (SODAR2 광고 사기 방지): `__Secure-OSID`, `__Secure-3PSID*` 다수
  - GTM/GA4
- **Browser console error**: `js?id=G-ERBDZ6V6VN — net::ERR_CONNECTION_CLOSED` (광고 차단기에 의한 차단, 아래 B 항목 참조)
- DevTools Issues 패널에 cookie / sodar / gen_204 관련 경고 다수

---

## 적용한 수정

### 1. Pretendard Variable 동적 서브셋 전환

| 파일 | 변경 |
|------|------|
| `templates/fragments/head.html` | `pretendardvariable.min.css` → `pretendardvariable-dynamic-subset.min.css` |
| `templates/error/4xx.html` | 동일 |
| `templates/error/5xx.html` | 동일 |

**원리.** 동적 서브셋 CSS 는 `unicode-range` 로 글리프 청크를 잘라서 발급한다.
브라우저는 페이지에 실제로 등장한 코드포인트를 포함한 청크만 다운로드 → 한국어
사용자는 KR + Latin 슬라이스만, 영어 사용자는 Latin 만 받는다.

**효과.** 단일 2 MB woff2 → 페이지당 200~400 KiB. 전체 페이로드의 약 -1.6 MB.

### 2. Noto Serif KR 로케일 분기

`#locale.language == 'ko'` 일 때만 Google Fonts 로 로드. 영어 사용자는
`--font-serif: 'Noto Serif KR', Georgia, serif;` 폴백 체인의 `Georgia` 가 적용된다.

```html
<th:block th:if="${#locale.language == 'ko'}">
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link rel="preload"
          href="https://fonts.googleapis.com/css2?family=Noto+Serif+KR:wght@300;400;600&display=swap"
          as="style" onload="this.onload=null;this.rel='stylesheet'">
    <noscript>...</noscript>
</th:block>
```

**효과.** 영어 사용자에 한해 추가 -228 KiB.

> `wght@300;400;600` 유지: `style.css:411-412` 의 `var(--font-serif)` + `font-weight: 300`
> 사용처가 있어 dropping 시 fallback 렌더 발생.

### 3. manifest.json 동적 생성 + 캐시 + i18n

`StaticResourceController.manifestJson()` 변경:

- 정적 파일 반환 → `MessageSource` 기반 동적 JSON 생성
- 로케일에 따라 `name`/`short_name`/`description`/`lang` 결정
  - `ko` → `약속 잡기` / `lang: "ko-KR"`
  - `en` → `Group Scheduling` / `lang: "en-US"`
- `Cache-Control: public, max-age=7d` + `Vary: Accept-Language, Cookie` 추가
- 키 재사용: `seo.home.appName`, `seo.home.appAlternateName`, `seo.home.description`

**효과.** 첫 요청 후 manifest 가 서버를 거치지 않음 (브라우저 캐시) → critical
path 에서 manifest 행 사라짐. 또한 영어 사용자가 PWA 설치 시 영문 메타데이터.

### 4. 정적 `static/manifest.json` 파일 삭제

컨트롤러가 `/manifest.json` 매핑을 점유하므로 정적 파일은 dead. 혼동 방지를 위해 제거.
SecurityConfig 의 `permitAll` 매처는 컨트롤러 경로에도 동일하게 적용되므로 변경 불필요.

### 검증

`./gradlew test` 통과 (1m 16s, 회귀 없음).

---

## 보류 항목

향후 별도 작업으로 분리. 우선순위 순.

### P1 — 즉시 적용 가능

#### A. AdSense lazy load (IntersectionObserver)

**현재.** `templates/fragments/head.html:90-92`

```html
<script th:if="${seo.adsEnabled()}" async
        src="https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=ca-pub-7334667748813914"
        crossorigin="anonymous"></script>
```

**제안.** 첫 광고 슬롯이 뷰포트 300 px 이내 진입할 때만 스크립트 주입.
`fragments/scripts.html` 에 IntersectionObserver 코드 추가, head 의 즉시 로드 제거.

**효과.** TBT 감소, 광고 노출 안 본 사용자 쿠키 0 개, `show_ads_impl_fy2021.js`
의 162 KiB unused JS 회피.

**트레이드오프.** 광고 매출 -5~10 % 추정 (스크롤 안 한 사용자 노출 손실).

#### B. GTM 측정 ID 구조 (정정 — 조치 불필요)

이전 초안은 `G-ERBDZ6V6VN` 을 stale GA4 측정 ID 로 오진했으나, 실제 구조는 다음과 같다.

```
GTM-PFPKQT7W (컨테이너)
  └─ Google Tag "datedate"
        ├─ 태그 ID: G-ERBDZ6V6VN (legacy 식별자), GT-5MCR2RS2 (new Google Tag 식별자)
        └─ 대상(destination) ID: G-9QTMK4CDDF  ← 실제 GA4 데이터 송신 대상

GA4 속성 516824378 "datedate"
  └─ 데이터 스트림 13159527689 (https://www.datedate.site/)
        └─ 측정 ID: G-9QTMK4CDDF  ← 위 destination 과 일치
```

`G-ERBDZ6V6VN` 은 GA4 측정 ID 가 아니라 GTM 안 Google Tag 의 자체 식별자이며,
Google Tag 부트스트랩 스크립트가 그 ID 로 `gtag/js` 를 로드하기 때문에 광고 차단기가
해당 요청을 끊어 `ERR_CONNECTION_CLOSED` 가 발생한 것이다. 차단기 OFF 상태에서는
정상 응답하며 데이터는 `G-9QTMK4CDDF` 속성으로 정상 수신 중.

조치 불필요. GTM 컨테이너에 stale 태그 없음.

### P2 — 적용 권장 (법적 / 매출 요건)

#### D. Consent Mode v2

2024.03 부터 EU/EEA/UK 사용자에게 광고 표시하려면 GDPR 동의 모달 + Consent Mode v2
신호 필수. 현재 미적용 → EU 광고 매출 점진적 0 으로 수렴.

옵션:

1. **Funding Choices** (Google 무료 CMP) — GTM 한 줄 추가로 끝남, 가장 빠름
2. CookieYes / Cookiebot 등 third-party CMP — 더 풍부한 옵션, 유료

**판단 기준.** 한국 트래픽 비중이 압도적이면 P3 로 미뤄도 됨 (한국 PIPA 는 광고
쿠키 동의 의무가 EU 만큼 엄격하지 않음). EU 트래픽이 의미 있어지면 즉시 적용.

### P3 — 선택적 (큰 변경)

#### E. AdSense 슬롯 ID 검증

`templates/fragments/ad-slot.html` 의 placeholder

```html
data-ad-client="ca-pub-XXXXXXXXXX"
data-ad-slot="XXXXXXXXXX"
```

가 head 의 실제 client `ca-pub-7334667748813914` 와 불일치. 광고가 실제로 뜨는
페이지(`/insights`, `/use-cases`, `/guide`)에서 어떻게 슬롯이 주입되는지 확인 후
실값 채우거나 슬롯을 폐기.

#### F. Server-side GTM (first-party 프록시)

`tag.datedate.site` 서브도메인으로 GTM 로드 → 쿠키가 first-party 가 되어 Safari ITP
환경에서도 추적 유지. GCP / Cloud Run 운영비 + 설정 복잡도가 비용. 트래픽 규모가
커진 시점에 재검토.

---

## 후속 측정 권장 시점

위 1~4 적용 후 24 시간 + nginx static asset 캐시 헤더 (`Cache-Control: public,
max-age=31536000, immutable`) 갱신 후 재측정. 기대 변화:

- LCP: 1.2 s 유지 또는 소폭 개선 (manifest 임계 경로 제거 효과)
- 페이로드: 2,987 KiB → 약 1,000~1,200 KiB (Pretendard 효과)
- 영어 사용자 페이로드: 추가 -228 KiB
- TBT: P1-A 적용 전까지는 큰 변화 없음 (AdSense 가 주범)

## 관련 파일

- [`templates/fragments/head.html`](../../src/main/resources/templates/fragments/head.html)
- [`templates/error/4xx.html`](../../src/main/resources/templates/error/4xx.html)
- [`templates/error/5xx.html`](../../src/main/resources/templates/error/5xx.html)
- [`StaticResourceController.java`](../../src/main/java/me/singingsandhill/calendar/common/presentation/controller/StaticResourceController.java)
