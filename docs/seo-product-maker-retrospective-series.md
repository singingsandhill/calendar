# 개인 프로젝트 SEO 적용기: 검색엔진이 이해하는 제품으로 다듬기

> DateDate 라는 개인 프로젝트를 운영하며 SEO·다국어·AdSense 승인·운영 검증을
> 어떻게 한 줄기 흐름으로 묶어왔는지에 대한 회고. 단순한 기능 구현 정리가 아니라,
> "기능은 만들었지만 검색엔진과 사용자가 그것을 이해하지 못한다" 는 자각에서
> 시작해 제품을 다듬어 간 과정을 정리한 시리즈의 1편이다.

---

## 시리즈 기획 의도

처음 SEO 라는 단어를 떠올렸을 때 기대했던 건 단순히 검색 결과 상위 노출이었다.
"키워드 잘 박고 사이트맵 제출하면 끝나는 거 아니야?" — 솔직히 말하면 그 정도
인식이었다. 그러나 DateDate 라는 사이드 프로젝트를 약 6개월간 운영하면서, SEO 가
가장 강하게 추궁한 질문은 *"이 사이트가 도대체 무엇이고, 누구를 위해 존재하며,
사용자가 머무를 만한 가치가 있는가"* 였다.

이 시리즈는 그 질문에 답해 가는 동안 마주친 결정들의 기록이다. AdSense 거절 통보,
GSC(Google Search Console)의 *"리디렉션이 포함된 페이지"* 경고, *"robots.txt 에 의해
차단됨"* 경고, hreflang return-tag 누락, 그리고 한 번에 안 먹는 언어 토글 같은
크고 작은 사고를 하나씩 풀어가다 보니 어느새 글이 한 편으로는 부족해졌다. 그래서
시리즈로 나눠 정리한다.

이 시리즈의 독자는 *주니어 백엔드 개발자* 또는 *개인 프로젝트를 제품처럼 운영해
보고 싶은 개발자* 다. 무겁지 않게, 그러나 실제 코드와 커밋 근거 위에서 — 만약
다음 사이드 프로젝트를 출시한다면 같은 시행착오를 반복하지 않도록 도움이 되는
글이 되길 바란다.

### DateDate 프로젝트 맥락 짧게 정리

DateDate(`datedate.site`) 는 그룹 일정 조율 서비스다. 오너가 일정을 만들고, 참가자가
가능한 시간대를 표시하면 모임이 잡힌다. 한국어가 기본 언어이고 영어를 추가
지원한다. Spring Boot 4 / Java 21 / Thymeleaf 모놀리스이며, 같은 코드베이스
안에서 그룹 일정(`datedate`) 외에 러닝 크루(`runner`), 트레이딩 봇(`trading`),
주식 봇(`stock`) 모듈이 함께 산다. 이 멀티 도메인 구조가 SEO 의사결정에 의외의
영향을 미쳤다는 점이 이번 회고의 한 축이다.

기능을 만든 뒤 마주한 문제는 다음 다섯 가지였다.

1. 검색엔진이 이 페이지가 무엇인지 모른다.
2. 같은 콘텐츠가 여러 URL 로 보인다 — 다국어 추가 후 더 심해졌다.
3. AdSense 가 *"가치가 별로 없는 콘텐츠"* 라며 거절했다.
4. UGC(사용자 일정) 페이지가 색인되어선 안 되는데 차단 정책이 자꾸 흔들렸다.
5. 한 번 SEO 작업을 해도 다음 기능 변경에서 쉽게 깨졌다.

---

## 시리즈 목차

이 시리즈는 총 4편으로 기획했다. 각 편의 키워드는 다음과 같다.

1. **검색엔진이 이해하는 구조 만들기 — Sitemap, Robots, Canonical** *(이번 글)*
2. **다국어 서비스의 SEO 설계 — hreflang, locale resolver, OG 메타**
3. **Low Value Content 를 넘어서 — AdSense 거절을 제품 가치 점검의 기회로**
4. **개인 프로젝트를 운영형 제품으로 다듬기 — 회귀 테스트, GSC/Naver/Bing 검증, IndexNow**

각 편은 독립적으로 읽을 수 있도록 쓰지만, 전체를 관통하는 메시지는 하나다.
**SEO 는 마케팅 작업이 아니라 제품 구조 설계의 작업이다.**

---

## 1편 본문 — 검색엔진이 이해하는 구조 만들기

### 1. 왜 SEO 를 고민하게 되었는가

처음 DateDate 의 MVP 를 배포한 직후, 가장 먼저 한 일은 도메인을 사고 친구
몇 명에게 링크를 보낸 것이었다. 며칠 뒤 *"이거 구글에 검색하면 나와?"* 라는 질문을
받았는데, 이름으로 검색해도 결과는 비어 있었다. 새 사이트가 한두 주 만에 색인되지
않는 건 자연스러운 일이었지만, 한 달이 지나도 GSC 의 *"색인 생성됨"* 페이지 수가
0 에서 1 로 올라가지 않는 걸 보면서 **검색엔진이 이 사이트를 발견조차 못하고
있다** 는 사실을 받아들여야 했다.

이 시점에서 깨달은 건 두 가지였다. 첫째, *발견 가능성(discoverability)* 은 기능을
만든다고 자동으로 따라오는 무언가가 아니라 별도로 설계해야 하는 영역이라는 것.
둘째, 개인 프로젝트라도 일단 외부에 배포한 순간부터는 운영·신뢰·발견·접근성 같은
제품 차원의 고민을 피할 수 없다는 것. 사이드 프로젝트라는 명칭은 어디까지나 만든
사람의 자기 정의일 뿐, 사용자나 검색엔진은 그것을 *제품* 으로 본다.

그래서 SEO 를 단순히 *검색 유입을 늘리는 작업* 이 아니라, *서비스가 외부 세계에
어떻게 자기소개를 할 것인가* 라는 질문으로 다시 정의했다. 메타 태그를 다는 것은
명함을 만드는 것에 가깝고, sitemap 을 제공하는 것은 *"우리 사이트는 이런 페이지로
이뤄져 있어요"* 라는 안내문을 두는 일에 가깝다는 관점.

### 2. 처음 마주한 문제 — 기능과 의미의 분리

코드는 동작했지만, 검색엔진 입장에서 보면 페이지들의 *목적과 관계* 가 보이지
않았다. 예를 들어 처음의 인덱스 페이지는 거대한 폼과 *"일정 만들기"* CTA
버튼만으로 이뤄져 있었고, 그 페이지가 *어떤 서비스이며 누구에게 유용한가* 를
설명하는 텍스트가 없었다. AdSense 봇과 검색엔진 입장에서는 *"기능은 있지만 의미가
빈약한 페이지"* 였던 셈이다.

그뿐이 아니었다. UGC 일정 공유 페이지(`/{ownerId}/{year}/{month}` 같은 동적 URL)는
오너 ID 와 연-월 단위로 무한히 늘어날 수 있는 구조였다. 이런 페이지가 그대로
색인되면 사용자 이름·약속 정보 같은 사적 정보가 검색 결과에 노출될 위험이 있었고,
동시에 검색엔진 입장에서는 *비슷비슷한 본문* 이 수만 개 인식되어 사이트 전체의
콘텐츠 품질이 깎이는 부작용이 있었다.

다국어 지원을 추가하니 문제가 더 복잡해졌다. 같은 `/guide` 페이지가
`?lang=en` 쿼리에 따라 두 가지 언어로 보였고, 검색엔진 입장에서는 *동일 URL 인지
서로 다른 언어 변종인지* 알 도리가 없었다. canonical 을 잘못 걸면 한쪽 언어가
완전히 무시되고, 안 걸면 *중복 콘텐츠* 신호가 발생한다.

마지막으로 AdSense 첫 신청에서 *"가치가 별로 없는 콘텐츠(Low value content)"* 사유로
거절을 받았다. 처음에는 단순히 *"콘텐츠를 좀 더 넣으면 되겠지"* 정도로 받아들였지만,
거절 통보를 정책 문서와 1:1 로 매핑해 보니 그것은 콘텐츠 분량의 문제가 아니라
**페이지의 목적성·고유성·신뢰성을 묻는 신호** 였다. 이 인식이 이후 모든 SEO
의사결정의 기준선이 됐다.

### 3. Sitemap 과 Robots 를 정리한 이유 — 지도와 출입 정책

처음에는 정적인 `sitemap.xml` 파일을 한 줄짜리로 두고 GSC 에 제출하는 것에서
시작했다. 단 한 줄이라도 *존재한다는 사실 자체* 가 색인 등록을 가능하게 했다.
그 후 콘텐츠 페이지가 늘면서, sitemap 을 *손으로 갱신* 하는 방식의 한계가 빠르게
드러났다. 페이지를 추가할 때마다 잊어버렸다.

그래서 동적 생성으로 옮겼다. `SitemapService` 가 코드 레벨에서 페이지 목록을
구성하고 XML 을 만들어 응답한다. 여기서 첫 번째 함정에 빠졌다 — `lastmod` 값에
`LocalDate.now()` 를 그냥 박아 넣은 것이다. 결과적으로 **재배포할 때마다 모든
페이지의 lastmod 이 오늘로 갱신** 됐다. Google 은 2023년부터 신뢰할 수 없는 lastmod
신호를 점진적으로 무시한다. 처음에는 *"좀 더 자주 크롤링되겠지"* 라고 좋아했는데,
실제로는 사이트맵 자체의 신뢰도를 깎아 먹는 안티패턴이었다.

그래서 lastmod 의 출처를 바꿨다. 정적 페이지는 빌드 시각(`BuildProperties#getTime()`)을,
인사이트 페이지는 *"인기 데이터가 마지막으로 추가된 시각"*(`Location/MenuRepository.findLatestActivity()`)을 쓴다.
ISO 8601 KST 오프셋 풀 정밀도로 출력하고, XML 5개 미리정의 엔티티(`& < > " '`)를
escape 하는 헬퍼를 추가했다. 결정성(deterministic) 이라는 단어가 핵심이었다 — 같은
입력에 같은 출력을 내는 사이트맵이라야 신호로서의 가치가 있다.

robots.txt 는 *지도* 라기보다 *출입 정책서* 다. 어떤 영역은 색인되어야 하고, 어떤
영역은 들어와도 무방하나 색인은 곤란하고, 어떤 영역은 봇이 들어오는 것 자체를
원치 않는다. DateDate 에서는 다음 세 가지를 구분해야 했다.

1. 공개 콘텐츠(홈, `/guide`, `/use-cases/*`, `/insights/trends`, `/privacy`, `/terms`) — *Allow + 색인*
2. UGC 일정 페이지 — *접근 가능하지만 색인 차단(noindex)*
3. 어드민·내부·트레이딩/주식 봇 모듈 — *접근 차단*

여기서 사고가 한 번 있었다. 초기에 `Disallow: /*/*` 를 넣었는데, 이 패턴은
*모든 2-depth URL* 을 차단해 버려서 `/runners/runs` 같은 정상 페이지마저 색인
대상에서 제외됐다. GSC 의 *"robots.txt 에 의해 차단됨"* 경고를 보고서야 원인을
찾았다. `/*/2024/`, `/*/2025/`, … `/*/2035/` 처럼 *연도별로 명시* 하는 방식으로
좁혔지만, robots.txt 에 character class(`[0-9]`) 가 없는 한계 때문에 결국 향후
10년 치를 미리 enumerate 하는 우회를 택했다. 더 안전한 방식은 *컨트롤러 응답에
`<meta name="robots" content="noindex">` 를 출력* 하는 페이지별 제어다. robots.txt
와일드카드는 도메인이 자랄수록 시한폭탄이 된다.

이 시점에 또 한 가지 결정을 내렸다. 같은 모놀리스 안에 있는 `runner`/`trading`/`stock`
모듈은 datedate 의 비즈니스 테마(*그룹 일정 조율*)와 다른 도메인이다. AdSense 의
*Site behaviour content* 정책은 *"사이트의 테마와 관련 없는 페이지의 텍스트"*를
부정적 신호로 본다. 그래서 trading/stock 은 robots.txt Disallow + 컨트롤러
noindex 로 이중 차단했고, runner 도 sitemap 에서 완전히 제외해 *AdSense 봇이
사이트 전반을 둘러볼 때 datedate 만 보이도록* 정리했다.

### 4. Canonical 과 hreflang — 같은 페이지가 여러 모습으로 보일 때

같은 `/guide` 페이지가 `?lang=ko`, `?lang=en`, 또는 파라미터 없는 형태로 여러 URL 에
존재한다. 사용자는 이걸 *같은 페이지의 두 언어 변종* 으로 인지하지만 검색엔진은
URL 만 본다. canonical 과 hreflang 은 그 인지 차이를 메우는 두 개의 신호다.

핵심은 `SeoMetadata` 라는 record 를 *처음부터* 다음 형태로 둔 것이다.

- `canonical` — 현재 로케일에 맞춘 self-canonical
- `canonicalKo`, `canonicalEn` — hreflang 태그용으로 *항상 둘 다 채움*
- `hreflangEnabled` — UGC noindex 페이지에서는 false 로 두어 hreflang 발행 자체를 차단

이 분리는 처음 SEO 인프라를 깔 때 *아직 영문화 계획이 구체화되지 않은 단계* 에서
이미 도입돼 있었다. 그게 이후 KO+EN 양방향으로 전환할 때 컨트롤러 수정 없이
서비스 한 곳만 고치면 되는 결정의 결정적인 이유가 됐다. 만약 record 가
`canonical` 한 필드만 가졌다면, hreflang 추가는 모든 컨트롤러를 다시 손대는
대수술이 됐을 것이다.

UGC 페이지에 hreflang 을 *발행하지 않는다* 는 결정도 의도적이다. noindex 페이지가
hreflang alt 그룹에 들어가면 Google 이 그 그룹 전체의 인식을 포기한다. 이 함정은
공식 문서를 정독하기 전까지는 잘 안 보이는데, `hreflangEnabled` 플래그가 그
실수를 코드 레벨에서 명시적으로 분리한다.

### 5. 메타 태그와 OG 이미지 — 검색 결과와 공유 링크의 첫인상

OG 메타와 Twitter Card 는 사람이 실제로 *처음 보는 화면* 이다. 검색 결과 미리보기,
카카오톡 링크 미리보기, 슬랙 unfurl 모두 OG 태그를 읽는다. 단순한 장식이 아니라
**제품 설명문의 일부** 다.

DateDate 에서는 다음 원칙을 잡았다.

- `og:title` 은 *서비스 이름 + 페이지 의도* 가 한 줄에 보이도록 — 잘려도 의미 전달.
- `og:description` 은 검색 결과와 SNS 미리보기에서 동일하게 작동하도록 *카피라이팅* 을
  의식해 작성 — 한국어 95~115자, 영어 140~160자.
- `og:image` 는 1490×780 PNG 한 장으로 통일, 추후 KO/EN 분기 가능하도록 옵션 필드 유지.
- `og:locale` + `og:locale:alternate` 로 SNS 측에 다른 언어 변종이 있음을 알림.

신뢰감을 주는 정보 구조는 *과시* 가 아니라 *기대값 일치* 였다. 검색 결과에서 본 설명,
링크를 클릭했을 때 보이는 페이지, 실제로 사용해 본 기능 — 이 셋이 어긋나지 않으면
사용자는 *"신뢰할 수 있는 작은 서비스"* 정도의 인상을 받는다. 그게 개인 프로젝트가
받을 수 있는 최선의 신뢰 점수라고 생각한다.

### 6. 회귀 테스트와 운영 검증 — SEO 는 한 번에 끝나지 않는다

이 모든 작업의 가장 무서운 점은 *기능 한 줄 바꾸다 깨진다* 는 것이다. 메시지 키
하나의 인자 타입을 바꿨더니 페이지 타이틀에 *"2,026/5 Schedule"* 처럼 **천 단위
쉼표가 박힌 연도** 가 출력된 적이 있다. 원인은 Spring `MessageFormat` 이 `int`
인자를 자동 NumberFormat 으로 처리하면서 그룹 구분자를 적용한 것이었다. 해법은
메시지 키에서 `{1,number,#}` 처럼 그룹 비활성화 패턴을 명시하는 것. 그리고
이런 사고를 반복하지 않기 위해 회귀 테스트를 추가했다.

지금 운영하는 회귀 테스트는 다음과 같은 *불변 조건* 을 지킨다.

- 모든 페이지를 KO/EN 양쪽으로 호출했을 때 `SeoMetadata` 가 null 필드 없이 빌드됨.
- 모든 JSON-LD 가 valid JSON 으로 파싱됨.
- canonical EN 은 `?lang=en` 으로 끝남.
- noindex 페이지는 `hreflangEnabled = false`.
- sitemap 의 모든 URL 이 200 응답.
- 같은 sitemap 을 두 번 호출했을 때 출력이 *결정적* 이어야 함.
- 페이지 타이틀에 천 단위 쉼표가 들어간 연도가 없음.

배포 후에는 GSC URL 검사 도구로 홈/주요 페이지의 색인 상태를 확인하고, Bing
Webmaster (GSC 에서 import) 와 네이버 서치어드바이저(한국 시장이라 필수) 의
사이트맵 제출 상태를 본다. `curl -IL https://www.datedate.site/` 로 redirect
횟수가 1회 이내인지도 매번 확인한다 — HTTP/HTTPS 혼재로 2회 이상 chain redirect 가
나면 GSC 가 *"리디렉션이 포함된 페이지"* 경고를 띄운다.

운영 검증을 자동화하기 위해 IndexNow 도 도입했다. 매일 03:30 KST 에 sitemap 의
URL 들을 `api.indexnow.org` 에 POST 하는 fail-soft 스케줄러로, 4xx/5xx/네트워크
예외는 모두 WARN 로그만 남기고 흘려보낸다. SEO 는 본질적으로 *비동기적 보상* 을
주는 작업이라, 실패가 곧바로 서비스에 영향을 주지 않게 만드는 것이 운영 안정성의
전제다.

### 7. 얻은 인사이트

SEO 를 거쳐 다시 코드를 보면 다음 다섯 가지가 또렷해진다.

1. **SEO 는 마케팅이 아니라 정보 구조 설계다.** 검색엔진이 이해할 수 있는 구조는
   사용자에게도 더 명확한 정보 구조를 의미한다. 페이지의 목적이 한 문장으로
   요약되지 않으면 메타 태그도 쓸 수 없다.
2. **SSOT(단일 진실 공급원) 가 결국 비용을 줄인다.** `SeoMetadata` record 하나가
   이후 모든 SEO 결정(hreflang, og:locale:alternate, JSON-LD 확장)을
   *컨트롤러 수정 없이* 흡수했다. 첫 커밋부터 둔 것이 가장 잘한 결정이다.
3. **기능 페이지와 콘텐츠 페이지는 다르게 다뤄야 한다.** UGC/대시보드/어드민에 광고를
   걸지 않고 noindex 처리하는 것이, 콘텐츠 페이지의 가치를 보호하는 길이다.
4. **AdSense 거절은 SEO 약점 알람이다.** AdSense 와 검색엔진은 같은 신호를 본다 —
   *"사용자가 머물 만한 콘텐츠가 있는가"*. 거절을 단순한 승인 실패로 보지 말고
   제품 가치 점검의 계기로 삼아야 한다 (이 이야기는 3편에서 자세히 한다).
5. **개인 프로젝트의 운영도 결국 *반복* 이다.** 한 번 SEO 작업을 끝내는 것이
   아니라, 회귀 테스트로 보호하고 지표를 모니터링하면서 매주 조금씩 보강해 나가는
   리듬이 필요하다.

기능을 만드는 작업과 제품으로 다듬는 작업은 같은 코드 위에서 일어나지만, 머릿속에
다른 *모자* 를 써야 한다. 개발자의 모자만 쓰고 있을 때는 *"기능이 동작하면 됐지"*
라는 결론으로 빠지기 쉽고, 프로덕트 메이커의 모자를 함께 쓸 때 비로소
*"검색엔진이 이걸 이해하나, 사용자가 이걸 신뢰하나, 봇이 이걸 안전하게
크롤링하나"* 같은 질문이 떠오른다. 그 두 모자 사이를 자유롭게 오가는 연습이
이번 6개월의 핵심이었다.

### 8. 다음 편 예고

다음 편은 다국어 SEO 의 디테일을 다룬다. URL 전략(쿼리 vs 서브패스 vs 서브도메인)을
어떻게 정했는지, hreflang return-tag 누락 사고, 언어 토글이 한 번에 안 먹는 버그,
SEO 텍스트를 `messages.properties` 로 외부화하면서 얻은 부수 효과, 그리고 OG 메타와
manifest.json 까지 다국어로 분기시키는 과정을 다룬다.

3편은 AdSense Low value content 거절 이후, 광고 정책 4개 문서를 직접 읽고 코드와
1:1 매핑하면서 *제품 가치* 라는 막연한 단어를 *측정 가능한 콘텐츠 품질 신호* 로
번역해 간 과정을 정리한다. 4편은 회귀 테스트 인프라와 GSC/Bing/Naver/IndexNow 로
이뤄진 운영 검증 자동화 — 즉 SEO 작업이 새 기능 추가에서 *깨지지 않도록* 보호하는
구조 — 를 다룬다.

---

## 참고한 프로젝트 근거

| 구분 | 파일/커밋 | 확인한 내용 | 글에 반영한 인사이트 |
|---|---|---|---|
| 코드 | `src/main/java/.../common/presentation/dto/SeoMetadata.java` | record 에 `canonical`, `canonicalKo`, `canonicalEn`, `hreflangEnabled`, `adsEnabled` 필드가 처음부터 분리되어 있음 | SEO SSOT 가 후속 결정(hreflang, AdSense 분기)을 컨트롤러 수정 없이 흡수한 결정의 근거 |
| 코드 | `src/main/java/.../common/application/service/SitemapService.java` | `BuildProperties` 와 `findLatestActivity()` 로 lastmod 산출, ISO 8601 + KST, XML escape, bilingual `<url>` 블록 + `xhtml:link` 3종(ko/en/x-default) | 신뢰 가능한 lastmod·결정적 사이트맵 출력의 핵심 근거. `LocalDate.now()` 가 안티패턴인 이유 |
| 코드 | `src/main/java/.../common/infrastructure/config/CookieThenAcceptLanguageLocaleResolver.java` | `resolveLocale` 의 0번 분기에서 request attribute 캐시 우선 반환, `setLocale` 에서 동일 요청 캐시 + 1년 쿠키 | 언어 토글이 한 번에 안 먹는 버그의 원인과 해결 (다음 편 깊이 다룸) |
| 코드 | `src/main/resources/templates/fragments/head.html` | hreflang `th:if="${seo.hreflangEnabled()}"` 로 발행, `og:locale:alternate` 조건부, naver-site-verification 환경변수 폴백 | UGC noindex 와 hreflang 발행 충돌을 코드 레벨에서 막는 가드 |
| 코드 | `src/main/resources/static/robots.txt` | `Disallow: /*/2024/` ~ `/*/2035/` 연도별 enumerate, runner/trading/stock Disallow, sitemap URL 명시 | robots.txt 와일드카드 한계, 도메인 테마 일관성을 위한 모듈별 차단 |
| 코드 | `src/main/resources/templates/fragments/ad-slot.html`, `fragments/CLAUDE.md` | leaderboard/infeed/rectangle 슬롯이 `adsEnabled` 인자에 따라 조건부 렌더링, 페이지별 슬롯 정책 표 | 기능 페이지(폼/대시보드)와 콘텐츠 페이지(`/guide`, `/use-cases`)에 광고 정책을 다르게 적용한 근거 |
| 문서 | `docs/seo-evolution-playbook.md` | Foundation → AdSense → HTTPS 통일 → 동적 sitemap → JSON-LD → 콘텐츠 폭발 → i18n SEO 까지의 전체 타임라인, 사이트맵 성숙도 모델 | 시리즈의 시간순 흐름과 단계별 트리거 사건 |
| 문서 | `docs/audit/adsense-low-value-content-policy-mapping.md` | Google 정책 4개 문서(MA-Thin, PP-Spam, AS-Content, PP-Full) 와 코드/콘텐츠의 1:1 매핑 | AdSense 거절을 *제품 가치 점검* 으로 재해석한 관점의 근거 (3편 핵심) |
| 문서 | `docs/adr/common/seo/0001-seo-metadata-as-ssot.md` | `SeoMetadata` SSOT 의사결정 기록, 대안과 기각 이유 | SEO SSOT 의 정당성 |
| 문서 | `docs/adr/common/seo/0003-trustworthy-sitemap-lastmod.md` (참조) | `BuildProperties` + `findLatestActivity()` 기반 lastmod 정책 | 신뢰 가능한 사이트맵 lastmod 의 근거 |
| 문서 | `docs/adr/common/seo/0004-hreflang-canonical-locale-toggle.md` | KO 토글 `rel="nofollow"`, EN 토글 `rel="alternate" hreflang="en"`, `og:locale:alternate` 결정 | 언어 토글의 SEO 시그널 분리 (2편에서 자세히) |
| 문서 | `docs/adr/common/seo/0005-robots-disallow-narrowing.md` (참조) | `Disallow: /*/*` → 연도별 enumerate 변천 | robots.txt 변천사와 와일드카드 함정 |
| 문서 | `docs/adr/common/seo/0007-content-pages-for-adsense.md` | `/guide`, `/use-cases/*` 4개 추가, JSON-LD HowTo/Organization/BreadcrumbList | 콘텐츠 페이지 도입의 의사결정 |
| 문서 | `docs/adr/common/i18n/0001-cookie-then-accept-language-resolver.md`, `0002-immediate-locale-application.md` | 로케일 해석 우선순위와 즉시 적용 정책 | 언어 토글 버그의 해결 근거 (2편 자세히) |
| 커밋 | `cdb552e` *(feat: SEO 적용 #9)* | `SeoMetadata` 도입, `StaticResourceController` 명시 엔드포인트 | "출시 첫 커밋부터 SSOT 를 둬라" 인사이트 |
| 커밋 | `5b65e53` *(fix: ads.txt 미식별 #10)* | 정적 리소스 핸들러로는 AdSense 가 인식 못해서 `@GetMapping(produces=TEXT_PLAIN)` 명시 | 표준 경로의 라우팅 우선순위 함정 |
| 커밋 | `7eda6e2` *(refactor: SEO 최적화 #9)* | 정적 sitemap → `SitemapService` 동적 생성으로 전환 | sitemap 진화의 전환점 |
| 커밋 | `69b9919` *(feat: SEO 인프라 + 콘텐츠 페이지 확장)* | `Disallow: /*/*` 제거, sitemap 6→11 페이지, JSON-LD 확장 | robots 사고 회복 + 콘텐츠 폭발 단계 |
| 커밋 | `6535183` *(feat: AdSense 거절 해결)* | `adsEnabled` 플래그로 콘텐츠 페이지에만 광고 로드, 정책 페이지 추가 | AdSense 거절을 페이지 종류별 정책 분기로 해소 |
| 커밋 | `65a8ff1` *(refactor: SEO 고도화)* | `LASTMOD` 상수 도입, Runner Run 상세 sitemap 자동 등록 | sitemap 신뢰도 v3 단계 |
| 커밋 | `e8dde63` *(refactor)* / 이후 `setLocale` 의 request attribute 캐시 도입 | 언어 토글 한 번에 안 먹는 버그 해결 | 2편 핵심 사고 사례 |
| 커밋 | `c8b970d` *(refactor: SEO 영문 추가)* | 1612+/873- 라인 변경, `messages_en.properties` 174 keys 도입, sitemap bilingual 전환 | i18n SEO 완성 단계 |
| 커밋 | `75fd434` *(refactor(seo): sitemap lastmod reliability)* | `LocalDate.now()` 제거, ISO 8601, XML escape | 사이트맵 신뢰도 회복 (현재 v4) |
| 커밋 | `5e1e14e` *(feat(seo): og locale alternate, lang toggle rel, naver)* | `og:locale:alternate`, KO=nofollow / EN=alternate, naver 검증 메타 | hreflang/canonical 토글 디테일 |
| 커밋 | `3f8621c` *(docs: SEO 진화 플레이북)* | 6개월 SEO 의사결정 전체 타임라인 정리 | 본 시리즈의 1차 자료 |

> 위 표의 커밋 해시는 모두 `git log --oneline` 에서 확인한 짧은 해시이며, 본문에서
> 결정의 근거로 다룬 항목만 추렸다. 더 자세한 의사결정 기록은
> [`docs/seo-evolution-playbook.md`](./seo-evolution-playbook.md) 와
> [`docs/adr/common/seo/`](./adr/common/seo/) 를 함께 참고하면 좋다.
