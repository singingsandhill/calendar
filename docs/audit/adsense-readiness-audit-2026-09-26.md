# AdSense 심사·게재 준비 상태 재분석 — 기술 요인·도메인 유효성·게재 경로 (2026-09-26)

> 목표: **AdSense 재검토 통과 → 실제 광고 게재.** 두 단계 각각의 미비점을 찾는다.
> 방법:
> ① 라이브 실측 (2026-09-26 14:26~ KST) — RDAP·DNS(DoH)·TLS·Safe Browsing·크롤러 UA 3종·`ads.txt` 4변형·
>   사이트맵 38 URL 헤더/본문 스캔·TTFB
> ② 코드·배포 설정 대조 (`AdsenseProperties`·`application.yaml`·`deploy/compose.yaml`·`ad-slot.html`·CSS)
> ③ **실측 프로브** — 프로퍼티 바인딩 임시 테스트, 확인 후 삭제
> ④ Google 공식 문서 대조 (2026-09-26 조회):
>   - AdSense 사이트 연결 방법 — support.google.com/adsense/answer/12169212
>   - 특수 크롤러 robots 처리 — developers.google.com/search/docs/crawling-indexing/google-special-case-crawlers
>   - CMP 요건 — support.google.com/adsense/answer/13554116
>
> 기준: HEAD `132562b` + 미커밋 작업 트리. 커밋 로그 대기 178~184. 라이브는 09-19 빌드.
> 선행 문서:
> - [SEO 종합 점검 2026-09-24](seo-comprehensive-audit-2026-09-24.md) (§11 재검증·§12 수정 계획)
> - [AdSense 3차 진단 2026-08-17](adsense-low-value-content-diagnosis-2026-08-17.md)
> - [ADR common/seo/0010](../adr/common/seo/0010-home-ads-script-off-and-cls-diagnosis.md)

---

## 1. 결론

1. **기술·도메인 측면에서 심사를 막는 요인은 없다.**
   - 도메인·DNS·TLS·Safe Browsing·크롤러 접근·`ads.txt`·사이트맵·보안 헤더·개인정보처리방침 고지가 모두 정상이다(§3).
   - 사이트 소유권 연결은 `ads.txt` 방식으로 충족된다.
2. **심사 측면의 핵심 미비점은 "개선분이 라이브에 없다" 는 것이다(G2).**
   - 09-19 이후의 개선(기사 12편, 내비·breadcrumb, 퍼블리셔 신원, 제목, `ads.txt` LF)이 전부 미배포다.
   - 지금 재검토를 요청하면 08-17 과 같은 콘텐츠로 심사받는다.
3. **게재 측면에는 실측으로 확인한 차단 결함이 하나 있다(G1).**
   - 문서화된 절차("승인 후 `ADSENSE_CLIENT` 설정")로는 광고가 켜지지 않는다.
   - `adsense.client` 에 환경 변수 플레이스홀더가 없고, 운영 비밀값은 `.env` 파일 임포트로 들어오기 때문이다.

## 2. 현재 위치

| 항목 | 상태 |
|---|---|
| AdSense | 08-17 3차 통지 — "승인됨 + 주의 필요(낮은 가치 콘텐츠)". 이후 재검토 요청 여부는 저장소 기록 없음 |
| 라이브 | 09-19 빌드 — 사이트맵 19페이지(38 URL), 기사 4편, 광고 스크립트 0 |
| 작업 트리 | 26페이지(52 URL), 기사 12편, 고아 0·breadcrumb 3단계·연락처·제목 예산 — 대기 커밋 178~184, 전체 스위트 124/790 GREEN |

## 3. 기술 요인 점검 — 실측 (전부 정상 또는 정보)

| 영역 | 결과 | 판정 |
|---|---|---|
| 도메인 등록 (RDAP) | 등록 **2025-12-14**(약 9.5개월), 만료 **2026-12-14 (79일 남음)**, 레지스트라 Gabia, `client transfer prohibited` | 정상 — **만료 전 자동 연장 확인 필요(G4)** |
| DNS | apex·www 모두 A `35.237.128.174`. AAAA·CAA·TXT·MX 없음 | 정상 (GSC 는 메타 태그로 인증) |
| TLS | Let's Encrypt `YE1`, SAN = `datedate.site`·`www.datedate.site`, 만료 **2026-11-09 (44일)** | 정상 — certbot 타이머·nginx 재적재 훅 동작 확인 필요(G4) |
| Safe Browsing | Transparency Report 상태 1, 위협 플래그 전부 false | 정상 |
| 리다이렉트 | `http`·`www` → `https://datedate.site` 1홉 301 | 정상 |
| 크롤러 접근 | `Mediapartners-Google`·`AdsBot-Google`·Googlebot × (`/`, `/ads.txt`, `/robots.txt`, 기사, `/privacy`, `/about`) 전부 200 | 정상 |
| robots.txt | AdSense 크롤러 2종은 **`User-agent: *` 그룹을 무시**(공식 문서). 전용 그룹 없음 → 차단 없음 | 정상 |
| ads.txt | `google.com, pub-7334667748813914, DIRECT, f08c47fec0942fa0` — 형식·인증 ID 정상. 4변형 전부 최종 200 `text/plain` 60B | 정상. 단 **라이브는 아직 CRLF**(LF 정규화 커밋 176 미배포, G3) |
| 사이트 소유권 연결 | 3방식(AdSense 코드 / ads.txt / `google-adsense-account` 메타) 중 **ads.txt 로 충족**. 코드·메타 없음 | 정상 — 메타 태그는 선택 |
| 사이트맵 | 라이브 38 URL 전부 200·index·self-canonical (09-24 점검). 코드 52 URL | 정상 — 코드분 미배포 |
| 광고 차단 헤더 | 38 URL 에 CSP·COEP·`X-Robots-Tag`·Permissions-Policy 없음. `X-Frame-Options: SAMEORIGIN` 은 광고 iframe 과 무관 | 정상 |
| 미완성 신호 | 38 URL 가시 텍스트에 "준비 중"·coming soon·lorem·TODO·`??key` 0건 | 정상 |
| 개인정보처리방침 | ko/en 모두 AdSense 쿠키·제3자 광고·맞춤 광고 해제(`google.com/settings/ads`)·Google 광고 정책 링크 | 정상 |
| 배포 중단 | 재시작 배포 1~3분 동안 전 경로 503 + `Retry-After: 120` | 정상 — 다만 이 창에 `ads.txt` 를 긁히면 "찾을 수 없음" 이 최대 5일 유지(Commit 176 조사) |
| 응답 속도 | TTFB 0.6~0.9s(`/ads.txt` 도 0.65s — 한국↔GCP 미국 동부 왕복 지배) | 정보 — 심사 무관 |

## 4. 미비점 (심각도 순)

### G1 (게재 차단) — `ADSENSE_CLIENT` 를 설정해도 광고가 켜지지 않는다

- **코드:** `application.yaml:26` `adsense.client:` 가 빈 값 고정이다.
  - 2026-05 에 `${ADSENSE_CLIENT:ca-pub-…}` 를 비우면서 플레이스홀더는 주석(:24)으로만 남았다.
  - 슬롯 3개는 `${ADSENSE_SLOT_*:}` 플레이스홀더를 유지한다.
- **배포:** 운영 앱은 비밀값을 OS 환경 변수가 아니라 `./.env` 마운트 + `spring.config.import: optional:file:.env[.properties]` 로 받는다
  (`deploy/compose.yaml` 의 `environment` 는 `TZ`·`JAVA_TOOL_OPTIONS` 뿐).
  - 프로퍼티 소스의 `ADSENSE_CLIENT` 키는 relaxed binding 으로 `adsense.client` 에 매핑되지 않는다.
  - 대문자·밑줄 → 점 변환은 OS 환경 변수 소스에만 적용된다.
- **실측 프로브:** `@SpringBootTest(properties = {"ADSENSE_CLIENT=ca-pub-probe", "ADSENSE_SLOT_INFEED=999"})` 결과
  **`client=[] slotInfeed=[999] enabled=false`**. 임시 테스트는 확인 후 삭제했다.
- **영향:**
  - 08-17 진단 §6-4·`docs/prompts/adsense-approval.md`·ADR-0010 이 적어 둔 게재 절차("통과 후 `ADSENSE_CLIENT` 재설정")가
    **조용히 실패**한다. 슬롯 ID 를 넣어도 `hasXxxSlot()` 이 `isEnabled()` 에 막혀 광고가 전혀 나오지 않는다.
  - `head.html` 의 주석 "gated by env-driven publisher ID" 도 사실과 다르다.
- **수정안:** `client: ${ADSENSE_CLIENT:}` + `.env.example` 에 `ADSENSE_*` 4키 문서화 + 바인딩 회귀 테스트(위 프로브의 정식판).
- **주의 — 적용 순서:** 이 수정이 배포되는 순간 서버 `.env` 에 `ADSENSE_CLIENT` 가 남아 있으면
  **승인 전에 광고 스크립트가 켜진다**(08-17 §6-4 "승인 전 OFF" 위반, 홈 제외 광고 허용 페이지 전부).
  배포 전에 서버 `.env` 를 먼저 확인한다.

### G2 (심사 효과 차단) — 개선분이 전부 미배포

- 라이브는 09-19 빌드다. 기사 8편(404), 헤더 "모임 노하우", 푸터 FAQ·연락처, breadcrumb 3단계, 제목 예산, FAQ 구조화 데이터 정합,
  `robots` `/oauth2/`, `ads.txt` LF 가 전부 대기 커밋(178~184)과 미배포 상태다.
- 08-17 진단 §6-1: 콘텐츠 레이어 배포·색인 확인 전에는 재검토를 요청하지 않는다. **지금 요청하면 4라운드째 같은 판정 위험.**

### G3 (낮음) — 라이브 `ads.txt` 가 CRLF

- IAB 스펙상 파서는 CR/LF/CRLF 를 모두 허용해야 하므로 직접 원인일 가능성은 낮다(Commit 176 판단 유지).
- 커밋 176 은 이미 HEAD 에 있으므로 다음 배포로 해소된다.

### G4 (운영 확인) — 만료 일정 두 개

| 대상 | 만료 | 확인할 것 |
|---|---|---|
| 도메인 | 2026-12-14 (79일) | Gabia 자동 연장·결제 수단. 만료 시 `ads.txt`·사이트 전체가 사라진다 |
| TLS 인증서 | 2026-11-09 (44일) | `systemctl list-timers certbot.timer` 활성, `sudo certbot renew --dry-run` 성공, 갱신 훅 `/etc/letsencrypt/renewal-hooks/deploy/reload-nginx.sh` 존재 ([런북](../operations/server-migration-runbook.md) 절차) |

### G5 (게재 품질) — 광고 단위·예약 높이·자동 광고 미결정

- 수동 슬롯 18곳은 전부 본문 뒤·CTA 앞이다. `.ad-slot + [class*="-cta"]` 간격 CSS 가 있다.
  - 예약 높이: 인피드 `min-height: 200px`, 리더보드 90px.
  - in-article(fluid) 광고는 모바일에서 200px 를 넘기 쉬워 CLS 가 발생할 수 있다. ADR-0010 이 "광고 재개 전 예약 높이 재설계" 를 요구한다.
- 슬롯 ID 는 승인 후 콘솔에서 발급한다.
- **자동 광고(Auto ads)** 를 콘솔에서 켜면 스크립트가 로드되는 광고 허용 페이지에 앵커·비네트·페이지 내 광고가 예약 공간 없이 주입된다.
  - CLS 와 "콘텐츠보다 광고가 많은" 인상을 만들 수 있다.
  - 수동 단위만 쓸지 결정이 필요하다.

### G6 (지역 한정) — EEA·영국·스위스 동의 관리

- Google 인증 CMP(IAB TCF)가 없으면 해당 지역 트래픽은 **비맞춤·제한 광고만** 받는다(EEA·영국 2024-01-16, 스위스 2024-07-31 시행).
- 트래픽이 주로 한국이라 수익 영향은 작다.
- AdSense 콘솔 "개인 정보 보호 및 메시지" 의 유럽 규정 메시지(Google 인증 CMP)를 켜는 것이 가장 작은 해법이다(코드 변경 불필요, AdSense 태그 필요 → G1 이후).

### G7 (콘텐츠 잔여 신호 — 이미 기록된 것)

| 신호 | 출처 | 비고 |
|---|---|---|
| 새 기사 8편 템플릿 골격 유사도 0.96 (본문 중복 0) | SEO 점검 §11 M1 | R10(선택) |
| 기사 게시일이 실제 공개일보다 앞섬 | N3 | 사용자 결정으로 수정 제외 — 수용한 위험 |
| 연락처 이메일 로컬파트가 다른 서비스명 | M2, ADR-0013 | 사용자 확인 대기 |
| `sameAs` 는 화면에 없음 | ADR-0013 | 사람 리뷰어에게는 효과 없음 |

### G8 (낮음) — 같은 도메인의 무관 섹션

- `/runners`(러닝 크루)는 noindex 로 공개되어 있다.
- `/stock`(주식 봇 대시보드)은 noindex + `permitAll` 로 **로그인 없이 열린다.**
- 둘 다 색인 페이지에서 링크되지 않아 리뷰어가 마주칠 가능성은 낮다. 다만 사이트 주제 일관성 측면에서 알려진 잔여 위험이다
  (AS-Content 관련 이전 결정: 사이트맵 제외·noindex).

### G9 (저장소 밖 — 사용자 확인 필요)

| 항목 | 왜 필요한가 |
|---|---|
| AdSense → 사이트 상태, `ads.txt` 상태("승인됨"/"찾을 수 없음"), 정책 센터 | 재검토 요청 가능 여부와 대기 중 심사가 있는지 |
| GSC → 사이트맵 제출 상태, 색인 페이지 수, 기사 URL 검사 | 08-17 §6-2 의 "색인 생성됨 확인 후 요청" 조건 |
| 서버 `.env` 의 `ADSENSE_*` 값 | G1 수정의 배포 순서 결정 |
| 도메인 자동 연장·certbot 타이머 | G4 |

## 5. 광고 게재까지 로드맵

| 단계 | 작업 | 확인 기준 |
|---|---|---|
| 0. 지금 | G9 확인. 특히 **대기 중인 AdSense 심사 여부**(있으면 판정 전 배포 금지). 대기 커밋 178~184 커밋 | 커밋 로그 순서대로, 각 커밋 후 필요 시 `gradlew test` |
| 1. 배포 | 수동 배포(평일 09:15~11:25 제외). `ads.txt` 크롤이 503 창에 걸리지 않도록 한가한 시간대 | 라이브 재실측: 사이트맵 52 URL 200·고아 0·BreadcrumbList 3단계·`ads.txt` LF·새 기사 200 (SEO 점검 R12) |
| 2. 색인 | GSC 사이트맵 재제출 + 새 기사 8편 URL 검사 → 색인 요청 | 8편 "색인 생성됨" (~7일). AdSense `ads.txt` 상태 정상 |
| 3. 재검토 | AdSense 정책 센터에서 검토 요청. 심사 중 색인 페이지 코드 변경·배포 금지 | 통지 수신 |
| 4. 게재 준비 (승인 후, 또는 1단계와 함께 — 순서 주의) | **G1 수정**(플레이스홀더 + `.env.example` + 바인딩 테스트) → 서버 `.env` 에 `ADSENSE_CLIENT` 설정. 콘솔에서 광고 단위 생성(in-article·display) → `ADSENSE_SLOT_*` 설정. 자동 광고 사용 여부 결정(G5). 유럽 규정 메시지 켜기(G6) | 광고 허용 페이지에만 `adsbygoogle.js` 로드(홈·정책·허브 0). 실제 광고 렌더 후 CLS 측정(ADR-0010 체크리스트) — 인피드 예약 높이 조정 |
| 5. 운영 | AdSense 정책 센터·무효 트래픽·`ads.txt` 상태 주기 확인. 도메인·인증서 만료 알림 | — |

G1 수정은 코드 변경이라 배포가 필요하다. 1단계 배포에 같이 실으면 승인 직후 설정만으로 게재할 수 있다.
그 경우 **서버 `.env` 에 `ADSENSE_CLIENT` 가 없음을 먼저 확인**해야 승인 전 OFF 가 유지된다.

## 6. 재현 명령

```bash
curl -sS https://rdap.org/domain/datedate.site | python3 -m json.tool | grep -A1 eventAction     # 등록·만료일
curl -sS "https://dns.google/resolve?name=datedate.site&type=A"                                 # DNS
echo | openssl s_client -connect datedate.site:443 -servername datedate.site 2>/dev/null | openssl x509 -noout -dates -ext subjectAltName
curl -sS "https://transparencyreport.google.com/transparencyreport/api/v3/safebrowsing/status?site=datedate.site"
for u in https://datedate.site/ads.txt http://datedate.site/ads.txt https://www.datedate.site/ads.txt http://www.datedate.site/ads.txt; do
  curl -sS -L -o /dev/null -w "%{http_code} hops=%{num_redirects} %{content_type} %{size_download}\n" "$u"; done
curl -sS https://datedate.site/ads.txt | od -c | tail -3                                           # 줄바꿈 (\r\n = CRLF)
curl -sS -A "Mediapartners-Google" -o /dev/null -w "%{http_code}\n" https://datedate.site/guides/how-to-pick-a-date
curl -sS https://datedate.site/ | grep -c "adsbygoogle\|google-adsense-account"                   # 광고 코드·메타 (현재 0)
```
