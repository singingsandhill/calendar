# AdSense ② 도구 중심 콘텐츠 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** About 에 운영자 신원·사실 정정, /guide 를 실제 화면 6단계로 보강, /insights/trends 에 실사용 집계 리포트(요일·월별·리드타임)를 더해 리뷰어에게 도구의 가치와 실제 사용을 보이게 한다.

**Architecture:** 세 파트를 각자 커밋 하나로 순서대로 낸다(②-1 → ②-2 → ②-3). 리포트는 읽기 전용 벌크 쿼리 2개(참여자+일정 JOIN FETCH, 일정 생성 시각)를 스프링 의존 없는 계산 클래스에 넘겨 렌더 시점에 집계한다. 스크린샷은 테스트 DB 에 예시 일정을 넣고 MockMvc 로 렌더한 HTML 을 Windows Chrome 헤드리스(CDP)로 잘라 찍는다 — `bootRun` 없이.

**Tech Stack:** Spring Boot 4 / Java 21, Thymeleaf, Spring Data JPA(H2), JUnit 5 + AssertJ + MockMvc + `@MockitoBean`, Node(Windows) CDP 스크립트, Python 3(WSL) 편집 도구.

**Spec:** `docs/superpowers/specs/2026-10-14-adsense-tool-content-design.md`

## Global Constraints

- 커밋·푸시를 직접 하지 않는다. 파트마다 `docs/guides/git-commit.md` 에 섹션을 덧붙인다 — cmd.exe 호환(큰따옴표만), `git add` 한 줄, 그 배치의 마지막 커밋이 로그 파일 자신을 포함. 덧붙이기 직전 마지막 Commit 번호를 다시 확인한다(세션 간 경합).
- Commit 197(staging 됨)·198 이 먼저 커밋돼 있어야 한다. 197 은 `git commit` 만(add 재실행 금지).
- `bootRun` 금지(LIVE 봇). 검증은 MockMvc·하네스로.
- Gradle 은 `cmd.exe /c "set JAVA_HOME=C:\\jdk-21&& .\\gradlew.bat test --tests <정확한 FQCN>"` — 와일드카드 금지. 좁혀 돌렸으면 `build/test-results/test/*.xml` 로 실제 실행 수 확인.
- 개행: `*.md` LF. Java 는 파일의 기존 개행 유지(`git ls-files --eol` 확인). `style.css` 는 한 파일 안 혼재 — 아래 `css_insert.py` 로만 삽입. `messages*.properties` 값 교체는 `set_props.py`(줄 개행·`\u` 이스케이프 스타일 유지).
- 메시지: 인자 없는 키는 `'` 1개, 인자 있는 키는 `''` 와 `{n,number,#}`.
- 문구: 광고로 비용을 충당한다는 문구는 사이트 어디에도 두지 않는다. About 은 검증 가능한 사실만. 스크린샷은 "예시 화면" 캡션.
- 리포트: 표본 하한 요일 300 날짜 / 월별 30 일정 / 리드타임 30 일정, 최근 12개월, KST(`Asia/Seoul`). 설정 키·캐시·JS·차트 라이브러리 없음.
- 연락처 이메일 변경 없음. Organization JSON-LD `founder`·`Person` 없음.

## Review Focus

- 기존 모드(`weeks != 7`) 일정: 인덱스 = 그 달의 날짜. 2026-10 인덱스 1 은 목요일(10-01)로 세야 한다(확장 규칙이면 일요일 09-27) — Task 8 `YearMonthTest`, Task 10 요일 테스트(기존 모드 입력).
- 저장된 selections 의 0·50·null·중복 인덱스: 건너뛰고 한 번만 센다, 예외 없음 — Task 8 `ParticipantAvailabilityTest`.
- 첫 일정 달과 이번 달 사이 일정이 0개인 달: 막대가 빠지지 않고 0 으로 나온다 — Task 10 월별 테스트.
- 비율 반올림 동률: 합이 정확히 100, 결과가 결정적(잉여 큰 순 → 앞 순서) — Task 10 리드타임 테스트(34/33/20/13).
- 리소스 버전 URL(해시)로 바뀐 스크린샷 경로가 ko·en 모두 실제로 서빙된다(200, `image/webp`) — Task 6 렌더 테스트.

---

## 공용 도구 (작업 전 스크래치패드에 만든다 — 저장소에 커밋하지 않음)

`$S` = 세션 스크래치패드 디렉터리.

`$S/scripts/set_props.py` — 키 단위 값 교체(줄 개행·이스케이프 유지):

```python
"""usage: set_props.py <file> <json: {key: [old_decoded_substring_or_null, new_value]}>
기존 값이 \\uXXXX 이스케이프면 새 값도 이스케이프로 쓴다. old 가 null 이면 값 전체 교체."""
import sys, json, re
path, spec_path = sys.argv[1], sys.argv[2]
spec = json.load(open(spec_path, encoding='utf-8'))
lines = open(path, 'rb').read().split(b'\n')
done = set()
dec = lambda v: re.sub(r'\\u([0-9a-fA-F]{4})', lambda m: chr(int(m.group(1), 16)), v)
enc = lambda v: ''.join(c if ord(c) < 128 else '\\u%04X' % ord(c) for c in v)
for i, raw in enumerate(lines):
    cr = raw.endswith(b'\r')
    text = (raw[:-1] if cr else raw).decode('utf-8')
    if '=' not in text or text.lstrip().startswith('#'):
        continue
    k, v = text.split('=', 1)
    if k in spec and k not in done:
        old, new = spec[k]
        if old is not None:
            assert old in dec(v), (k, 'substring not found')
            new = dec(v).replace(old, new)
        out = k + '=' + (enc(new) if re.search(r'\\u[0-9a-fA-F]{4}', v) else new)
        lines[i] = out.encode('utf-8') + (b'\r' if cr else b'')
        done.add(k)
assert not (set(spec) - done), ('keys not found', set(spec) - done)
open(path, 'wb').write(b'\n'.join(lines))
print('updated', sorted(done))
```

`$S/scripts/add_props.py` — 앵커 키 바로 뒤에 새 키 줄을 넣는다(앵커 줄의 개행을 따름):

```python
"""usage: add_props.py <file> <anchor-key> <lines-file(UTF-8, key=value 줄들)>"""
import sys
path, anchor, add_path = sys.argv[1], sys.argv[2], sys.argv[3]
lines = open(path, 'rb').read().split(b'\n')
idx = next(i for i, l in enumerate(lines) if l.startswith((anchor + '=').encode()))
cr = lines[idx].endswith(b'\r')
new = [l for l in open(add_path, encoding='utf-8').read().splitlines() if l.strip()]
assert not any(any(l2.startswith((n.split('=', 1)[0] + '=').encode()) for l2 in lines) for n in new), 'key exists'
lines[idx + 1:idx + 1] = [n.encode('utf-8') + (b'\r' if cr else b'') for n in new]
open(path, 'wb').write(b'\n'.join(lines))
print('added', len(new), 'after', anchor)
```

`$S/scripts/css_insert.py` — 앵커 규칙 블록의 닫는 `}` 다음에 빈 줄 + 블록 삽입(앵커 블록의 개행을 따름):

```python
"""usage: css_insert.py <css> <anchor-regex> <block-file>"""
import sys, re
css, anchor, block_path = sys.argv[1], sys.argv[2], sys.argv[3]
lines = open(css, 'rb').read().split(b'\n')
i = next(n for n, l in enumerate(lines) if re.search(anchor.encode(), l))
while lines[i].rstrip(b'\r').strip() != b'}':
    i += 1
cr = lines[i].endswith(b'\r')
block = open(block_path, encoding='utf-8').read().replace('\r\n', '\n').rstrip('\n').split('\n')
new = [b''] + [l.encode('utf-8') for l in block]
lines[i + 1:i + 1] = [(l + b'\r') if cr else l for l in new]
open(css, 'wb').write(b'\n'.join(lines))
print('inserted', len(block), 'lines after line', i + 1, 'CRLF' if cr else 'LF')
```

CSS 삽입 후 확인: `git diff --numstat -- src/main/resources/static/css/style.css` 와 `git diff --ignore-cr-at-eol --numstat -- …` 의 수치가 같아야 한다(다르면 개행이 바뀐 것 → `git checkout -- style.css` 후 다시).

---

## 파트 ②-1 신뢰 정보 (Commit 199)

### Task 1: About "만든 사람" + 광고 비용 충당 문구 제거 + 사실 정정

**Files:**
- Create: `src/test/java/me/singingsandhill/calendar/datedate/presentation/controller/AboutPageRenderingTest.java`
- Modify: `src/main/resources/templates/about.html` (audience `article` 다음)
- Modify: `src/main/resources/messages.properties`, `src/main/resources/messages_en.properties`

**Interfaces:**
- Consumes: 없음
- Produces: 메시지 키 `about.section.maker.title`, `about.section.maker.body`, `about.section.maker.githubLabel`

- [ ] **Step 1: 실패하는 테스트 작성**

```java
package me.singingsandhill.calendar.datedate.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.HtmlUtils;

/**
 * About 신뢰 정보 가드 (AdSense ② — ADR common/seo/0015).
 * 사람 리뷰어가 보는 운영자 신원(GitHub 링크)과, 사용자 결정으로 없앤 "광고로 운영비 충당" 문구의 재유입을 막는다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AboutPageRenderingTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest(name = "/about [{0}]")
    @ValueSource(strings = {"ko", "en"})
    @DisplayName("About 은 운영자 GitHub 링크를 새 탭(rel=me)으로 화면에 보여준다")
    void showsOperatorGithubLink(String lang) throws Exception {
        String html = render("/about", lang);
        assertThat(html).containsPattern(
                "href=\"https://github.com/singingsandhill\"[^>]*target=\"_blank\"[^>]*rel=\"me noopener noreferrer\"");
    }

    @Test
    @DisplayName("About(ko) 에 만든 사람 섹션이 있다")
    void koreanMakerSection() throws Exception {
        String text = visibleText(render("/about", "ko"));
        assertThat(text).contains("만든 사람").contains("1인 개발자가 만들고 운영하는 개인 프로젝트");
    }

    @Test
    @DisplayName("About(en) 에 만든 사람 섹션이 있다")
    void englishMakerSection() throws Exception {
        String text = visibleText(render("/about", "en"));
        assertThat(text).contains("Who's behind DateDate").contains("built and run by a single developer");
    }

    @ParameterizedTest(name = "{0} [{1}]")
    @CsvSource({"/about,ko", "/about,en", "/terms,ko", "/terms,en"})
    @DisplayName("광고로 운영비를 충당한다는 문구가 본문·메타 어디에도 없다 (사용자 결정 2026-10-14)")
    void noAdFundingCopy(String path, String lang) throws Exception {
        String html = HtmlUtils.htmlUnescape(render(path, lang));
        assertThat(html)
                .doesNotContain("광고로 충당")
                .doesNotContain("광고 운영 기준")
                .doesNotContain("서비스 운영을 위해")
                .doesNotContain("hosting costs with ads")
                .doesNotContain("To keep the service running");
    }

    private String render(String path, String lang) throws Exception {
        return mockMvc.perform(get(path).param("lang", lang))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private static String visibleText(String html) {
        int start = html.indexOf("<body");
        int end = html.indexOf("</body>");
        String body = (start >= 0 && end > start) ? html.substring(start, end) : html;
        body = body.replaceAll("(?is)<(script|style|noscript)[^>]*>.*?</\\1>", " ");
        return HtmlUtils.htmlUnescape(body.replaceAll("(?s)<[^>]+>", " ")).replaceAll("\\s+", " ");
    }
}
```

- [ ] **Step 2: 실패 확인**

Run: `cmd.exe /c "set JAVA_HOME=C:\\jdk-21&& .\\gradlew.bat test --tests me.singingsandhill.calendar.datedate.presentation.controller.AboutPageRenderingTest"`
Expected: 실행 8건 전부 FAIL — GitHub 링크 2, 만든 사람 2, 광고 문구 4(About ko "광고로 충당"·"광고 운영 기준", About en "hosting costs with ads", Terms ko/en).

- [ ] **Step 3: 템플릿 — `about.html` 의 audience `</article>` 다음, contact `<article>` 앞에 삽입**

```html
        <article class="policy-section">
            <h2 th:text="#{about.section.maker.title}">만든 사람</h2>
            <p th:text="#{about.section.maker.body}">Body</p>
            <p>
                <span th:text="#{about.section.maker.githubLabel}">운영자 GitHub</span>:
                <a href="https://github.com/singingsandhill" target="_blank" rel="me noopener noreferrer">singingsandhill</a>
            </p>
        </article>
```

- [ ] **Step 4: 새 키 추가 (`add_props.py`, 앵커 `about.section.audience.case4`)**

`$S/about_ko.txt`:
```
about.section.maker.title=만든 사람
about.section.maker.body=DateDate 는 1인 개발자가 만들고 운영하는 개인 프로젝트입니다. 2025년 12월에 공개했고, 이후 시간 투표(2026년 9월)와 일정 설명(2026년 10월) 같은 기능을 더해 왔습니다.
about.section.maker.githubLabel=운영자 GitHub
```
`$S/about_en.txt`:
```
about.section.maker.title=Who's behind DateDate
about.section.maker.body=DateDate is a personal project built and run by a single developer. It launched in December 2025 and has since added features such as time voting (September 2026) and schedule descriptions (October 2026).
about.section.maker.githubLabel=Operator's GitHub
```
Run: `python3 -I $S/scripts/add_props.py src/main/resources/messages.properties about.section.audience.case4 $S/about_ko.txt` (en 도 같은 앵커로)

사용자가 스펙 리뷰에서 만든 이유 문장을 주었다면 `about.section.maker.body` 끝에 그 문장을 붙인다(주지 않았으면 그대로).

- [ ] **Step 5: 기존 문구 교체 (`set_props.py`)**

`$S/about_fix_ko.json` (`DD` = 작업 당일 KST 날짜):
```json
{
  "about.section.principles.list1": ["페이지 ID 와 참여자 이름은 사용자가 자유롭게 입력합니다.", "페이지 ID 와 참여자 이름은 사용자가 자유롭게 입력합니다. 선택 사항인 카카오 로그인을 쓰면 카카오 회원번호·닉네임·프로필 이미지 주소를 저장합니다."],
  "about.section.principles.list2": [" 운영 비용은 콘텐츠 페이지의 광고로 충당합니다.", ""],
  "seo.about.description": ["광고 운영 기준, ", ""],
  "terms.section.ads.body": ["서비스 운영을 위해 일부 페이지에", "일부 페이지에"],
  "about.updated": [null, "최종 업데이트: 2026년 10월 DD일"],
  "terms.updated": [null, "최종 수정일: 2026년 10월 DD일"]
}
```
`$S/about_fix_en.json`:
```json
{
  "about.section.principles.list1": ["The page ID and participant names are values you choose and type in.", "The page ID and participant names are values you choose and type in. If you choose to sign in with Kakao, we store your Kakao user ID, nickname, and profile image URL."],
  "about.section.principles.list2": [" We cover hosting costs with ads on content pages only.", ""],
  "terms.section.ads.body": ["To keep the service running, ads served through Google AdSense appear on some pages.", "Ads served through Google AdSense appear on some pages."],
  "about.updated": [null, "Last updated: October DD, 2026"],
  "terms.updated": [null, "Last updated: October DD, 2026"]
}
```
Run: `python3 -I $S/scripts/set_props.py src/main/resources/messages.properties $S/about_fix_ko.json` (en 도 같이)

- [ ] **Step 6: 통과 확인**

Run: Step 2 와 같은 명령. Expected: 8 tests PASS.

### Task 2: 기사 게시일 정정 + 바이라인 "DateDate"

**Files:**
- Modify: `src/main/java/me/singingsandhill/calendar/datedate/domain/guide/GuideSlugs.java`
- Modify: `src/test/java/me/singingsandhill/calendar/common/application/service/SitemapServiceHreflangTest.java:124`
- Modify: `src/test/java/me/singingsandhill/calendar/datedate/presentation/controller/GuidesLocaleRenderingTest.java` (테스트 추가)
- Modify: `messages.properties`, `messages_en.properties` (`guides.byline.author`)

**Interfaces:**
- Consumes: 없음
- Produces: `GuideSlugs.ALL` 날짜(4편 2026-09-19, 8편 2026-10-04)

- [ ] **Step 1: 실패하는 테스트 — `GuidesLocaleRenderingTest` 에 추가**

```java
    @Test
    @DisplayName("기사 바이라인은 실제 첫 공개일을 보여주고 저자는 'DateDate' 다 (AdSense ② — 게시일 정정)")
    void bylineShowsActualPublishDate() throws Exception {
        String ko = visibleText(mockMvc.perform(get("/guides/how-to-pick-a-date"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(ko).contains("2026년 9월 19일").doesNotContain("2026년 8월 23일").doesNotContain("DateDate 팀");

        String koNew = visibleText(mockMvc.perform(get("/guides/timezone-coordination"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(koNew).contains("2026년 10월 4일").doesNotContain("2026년 9월 24일");
    }
```
(`visibleText`·`mockMvc` 는 이 클래스에 이미 있다.)

`SitemapServiceHreflangTest` 124행 기대값을 새 날짜로 바꾼다:
```java
        assertThat(block).contains("<lastmod>2026-09-19T00:00:00+09:00</lastmod>");
```

- [ ] **Step 2: 실패 확인**

Run: `cmd.exe /c "set JAVA_HOME=C:\\jdk-21&& .\\gradlew.bat test --tests me.singingsandhill.calendar.datedate.presentation.controller.GuidesLocaleRenderingTest --tests me.singingsandhill.calendar.common.application.service.SitemapServiceHreflangTest"`
Expected: `bylineShowsActualPublishDate` FAIL(8월 23일 표시), `guideArticleLastmodComesFromSsot` FAIL(08-23 lastmod).

- [ ] **Step 3: `GuideSlugs.ALL` 날짜 교체** — 앞 4편 `LocalDate.of(2026, 8, 23)` 두 곳 → `LocalDate.of(2026, 9, 19)`, 나머지 8편 `LocalDate.of(2026, 9, 24)` 두 곳 → `LocalDate.of(2026, 10, 4)`. 파일 개행 유지(`git ls-files --eol` 확인 후 Edit). 클래스 Javadoc 아래에 한 줄:

```java
 * <p>published 는 실제 첫 공개(라이브 배포)일이다 — 4편 09-19 빌드, 8편 10-04 배포 (2026-10-14 정정, AdSense ②).
```

- [ ] **Step 4: 바이라인 키** — `set_props.py` 로 `guides.byline.author` ko `[null, "DateDate"]`, en `[null, "DateDate"]`.

- [ ] **Step 5: 통과 확인** — Step 2 명령. Expected: 두 클래스 전부 PASS. `GuideSlugsTest`(게시 ≤ 수정 ≤ 오늘)도 함께: `--tests me.singingsandhill.calendar.datedate.domain.guide.GuideSlugsTest`.

### Task 3: ADR common/seo/0015 + 문서 + ②-1 전체 검증 + 커밋 로그

**Files:**
- Create: `docs/adr/common/seo/0015-operator-github-link-on-about.md`
- Modify: `docs/adr/common/seo/0013-publisher-identity-contact-and-sameas.md` (상태 행)
- Modify: `docs/adr/README.md`, `src/main/java/me/singingsandhill/calendar/datedate/application/CLAUDE.md`
- Modify: `docs/guides/git-commit.md`

- [ ] **Step 1: ADR 0015 작성**

```markdown
# ADR-0015: 운영자 GitHub 링크를 About 화면에 노출

| 항목 | 값 |
|---|---|
| 상태 | Accepted — ADR 0013 결정 3 의 "화면(UI)에는 링크를 노출하지 않는다" 를 대체 |
| 날짜 | 2026-10-14 |
| 도메인 | common (SEO) / datedate |
| 관심사 | SEO / E-E-A-T / AdSense 게시자 신원 |
| 관련 커밋 | `docs/guides/git-commit.md` Commit 199 |

## Context — 무엇이 문제였나

- AdSense "가치가 별로 없는 콘텐츠" 4번째 통지(2026-10-12). 통지문은 "신뢰할 수 있는" 정보를 기준으로 든다.
- ADR 0013 은 운영자 GitHub 를 Organization `sameAs` 로만 연결하고 화면에는 두지 않았다. 0013 스스로 "사람 리뷰어에게는 효과 없음" 을 부정적 결과로 적었다.
- About 에는 운영 주체가 누구인지 드러내는 문장이 없었다.

## Decision — 무엇을 골랐나

About 에 "만든 사람" 섹션을 두고 운영자 GitHub(`https://github.com/singingsandhill`) 링크를 화면에 노출한다. 실명은 쓰지 않는다(사용자 결정).

- 문구는 검증 가능한 사실만 — 1인 개발자의 개인 프로젝트, 2025-12 공개, 시간 투표(2026-09)·일정 설명(2026-10) 추가.
- 링크는 새 탭, `rel="me noopener noreferrer"`.
- JSON-LD `sameAs` 는 그대로. `founder`·`Person` 저자는 두지 않는다(ADR 0011 결정 6 유지).
- 기사 바이라인은 "DateDate 팀" → "DateDate" — 1인 운영과 "팀" 표기가 모순.

## Rationale — 왜 이 선택인가

| 대안 | 판정 |
|---|---|
| 0013 유지(JSON-LD 만) | 사람 리뷰어에게 신원 신호 없음 |
| 실명 + GitHub | 신호는 가장 강하지만 개인정보 공개 부담 — 사용자가 택하지 않음 |
| **GitHub 핸들 + 사실 소개 (선택)** | 실재하는 계정으로 확인 가능한 신원, 실명 비공개 |

## Consequences — 영향

- **긍정:** 사람 리뷰어가 운영 주체를 확인할 수 있다. 회귀 가드 `AboutPageRenderingTest`.
- **부정:** GitHub 계정의 공개 정보가 사이트와 연결된다.
- **후속:** 실명·`Person` 저자 도입은 별도 결정.
```

ADR 0013 상태 행 끝에 덧붙인다: ` — 결정 3 의 UI 비노출은 [ADR 0015](0015-operator-github-link-on-about.md) 로 대체 (2026-10-14)`.

- [ ] **Step 2: README 갱신** — 매트릭스 `common` SEO 14→15·합계 27→28, `합계` 행 SEO 14→15·총 99→100, 상단 "총 **99개 ADR**" → 100, 시간순 표 끝에 행 추가:

```
| 2026-10-14 | common/seo | [0015 운영자 GitHub 링크를 About 화면에 노출](common/seo/0015-operator-github-link-on-about.md) | AdSense 4번째 통지 — 사람 리뷰어가 볼 신원 신호 부재 (0013 의 UI 비노출 대체) |
```
폴더 목록 `common/seo/` 14 → 15.

- [ ] **Step 3: `datedate/application/CLAUDE.md`** — SeoService 줄의 "`sameAs`(운영자 GitHub — 화면 비노출)" 을 "`sameAs`(운영자 GitHub — About 화면에도 링크, [ADR 0015](../../../../../../../../docs/adr/common/seo/0015-operator-github-link-on-about.md))" 로.

- [ ] **Step 4: 전체 스위트**

Run: `cmd.exe /c "set JAVA_HOME=C:\\jdk-21&& .\\gradlew.bat test"` (5분 이상 — 백그라운드 실행 권장)
Expected: failures 0 errors 0. 기준(198 후) 134 클래스 867 테스트 → AboutPageRenderingTest(+1 클래스, +8) + 바이라인 테스트(+1) = 135 / 876.

- [ ] **Step 5: 커밋 로그 섹션 (Commit 199)** — 형식은 Commit 198 섹션을 따른다. `git add` 목록:

```
git add src/main/resources/templates/about.html src/main/resources/messages.properties src/main/resources/messages_en.properties src/main/java/me/singingsandhill/calendar/datedate/domain/guide/GuideSlugs.java src/test/java/me/singingsandhill/calendar/datedate/presentation/controller/AboutPageRenderingTest.java src/test/java/me/singingsandhill/calendar/datedate/presentation/controller/GuidesLocaleRenderingTest.java src/test/java/me/singingsandhill/calendar/common/application/service/SitemapServiceHreflangTest.java docs/adr/common/seo/0015-operator-github-link-on-about.md docs/adr/common/seo/0013-publisher-identity-contact-and-sameas.md docs/adr/README.md src/main/java/me/singingsandhill/calendar/datedate/application/CLAUDE.md docs/superpowers/specs/2026-10-14-adsense-tool-content-design.md docs/superpowers/plans/2026-10-14-adsense-tool-content.md docs/guides/git-commit.md
git commit -m "feat(datedate): About 운영자 소개·GitHub 링크 + 광고 비용 충당 문구 제거 + 기사 게시일 정정 (ADR common/seo/0015)" -m "…(RED→GREEN 수치·결정 근거)…"
```

---

## 파트 ②-2 /guide 보강 (Commit 200)

### Task 4: /guide 6단계 문구 + HowTo 6단계

**Files:**
- Create: `src/test/java/me/singingsandhill/calendar/datedate/presentation/controller/GuidePageRenderingTest.java`
- Modify: `src/test/java/me/singingsandhill/calendar/datedate/application/service/SeoServiceI18nTest.java` (테스트 추가)
- Modify: `src/main/resources/templates/guide.html`
- Modify: `src/main/java/me/singingsandhill/calendar/datedate/application/service/SeoService.java` (`getGuideSeo`)
- Modify: `messages.properties`, `messages_en.properties`

**Interfaces:**
- Produces: 키 `guide.step6.{title,p,li1,li2,li3}`, 새 `guide.step5.{title,p,li1,li2,li3}`(시간 투표), `guide.step2.li3`, `seo.guide.step6.{name,text}`, 화면 앵커 `#step-6`

- [ ] **Step 1: 실패하는 렌더 테스트**

```java
package me.singingsandhill.calendar.datedate.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.HtmlUtils;

/**
 * /guide 6단계 가드 (AdSense ② — 시간 투표 단계 신설, 일정 생성 안내 정정).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GuidePageRenderingTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("/guide(ko) — 6단계, 5단계는 시간 정하기, 6단계는 결과 확인, 일정 생성 안내가 실제 흐름과 맞다")
    void koreanSixSteps() throws Exception {
        String html = render("ko");
        assertThat(html).contains("id=\"step-6\"").doesNotContain("id=\"step-7\"");
        String text = visibleText(html);
        assertThat(text).contains("6단계로").contains("시간 정하기").contains("결과 확인 & 결정")
                .contains("30분 단위").contains("이 일정 만들기").contains("일정 설명(최대 500자)")
                .doesNotContain("자동으로 생성됩니다").doesNotContain("겨치는");
    }

    @Test
    @DisplayName("/guide(en) — 6단계, Pick a Time, 실제 생성 흐름")
    void englishSixSteps() throws Exception {
        String text = visibleText(render("en"));
        assertThat(text).contains("in 6 steps").contains("Pick a Time").contains("Review Results & Decide")
                .contains("30-minute steps").contains("Create this schedule").contains("up to 500 characters")
                .doesNotContain("automatically created");
    }

    private String render(String lang) throws Exception {
        return mockMvc.perform(get("/guide").param("lang", lang))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private static String visibleText(String html) {
        int start = html.indexOf("<body");
        int end = html.indexOf("</body>");
        String body = (start >= 0 && end > start) ? html.substring(start, end) : html;
        body = body.replaceAll("(?is)<(script|style|noscript)[^>]*>.*?</\\1>", " ");
        return HtmlUtils.htmlUnescape(body.replaceAll("(?s)<[^>]+>", " ")).replaceAll("\\s+", " ");
    }
}
```

`SeoServiceI18nTest` 에 추가(클래스의 `service`·`json` 필드 사용):

```java
    @Test
    @DisplayName("가이드 HowTo 는 6단계이고 5단계는 시간 정하기다 (AdSense ② — 시간 투표 단계)")
    void guideHowToHasSixSteps() throws Exception {
        LocaleContextHolder.setLocale(Locale.KOREAN);
        com.fasterxml.jackson.databind.JsonNode root = json.readTree(service.getGuideSeo().jsonLd());
        com.fasterxml.jackson.databind.JsonNode steps = root.get(0).get("step");
        assertThat(steps).hasSize(6);
        assertThat(steps.get(4).get("name").asText()).isEqualTo("시간 정하기");
        assertThat(steps.get(5).get("url").asText()).endsWith("/guide#step-6");
    }
```

- [ ] **Step 2: 실패 확인**

Run: `cmd.exe /c "set JAVA_HOME=C:\\jdk-21&& .\\gradlew.bat test --tests me.singingsandhill.calendar.datedate.presentation.controller.GuidePageRenderingTest --tests me.singingsandhill.calendar.datedate.application.service.SeoServiceI18nTest"`
Expected: `koreanSixSteps`·`englishSixSteps`·`guideHowToHasSixSteps` FAIL.

- [ ] **Step 3: `guide.html`** — `#step-5` 블록을 아래 두 블록으로 바꾼다(기존 5단계 내용은 6단계 키로).

```html
        <div class="guide-step animate-on-scroll" id="step-5">
            <div class="guide-step-number">5</div>
            <div class="guide-step-content">
                <h2 th:text="#{guide.step5.title}">시간 정하기</h2>
                <p th:utext="#{guide.step5.p}">누군가 가능하다고 고른 날에 시간 후보를 올리고 투표합니다.</p>
                <ul>
                    <li th:text="#{guide.step5.li1}">후보는 30분 단위</li>
                    <li th:text="#{guide.step5.li2}">가능하다고 고른 날짜에만 후보를 올릴 수 있음</li>
                    <li th:text="#{guide.step5.li3}">겹치는 시간 차트로 확인</li>
                </ul>
            </div>
        </div>

        <div class="guide-step animate-on-scroll" id="step-6">
            <div class="guide-step-number">6</div>
            <div class="guide-step-content">
                <h2 th:text="#{guide.step6.title}">결과 확인 & 결정</h2>
                <p th:utext="#{guide.step6.p}">가장 많은 사람이 가능한 날짜가 자동으로 하이라이트됩니다.</p>
                <ul>
                    <li th:text="#{guide.step6.li1}">겹치는 날짜가 많을수록 진한 색상</li>
                    <li th:text="#{guide.step6.li2}">인기 장소와 메뉴 순위</li>
                    <li th:text="#{guide.step6.li3}">최종 결정</li>
                </ul>
            </div>
        </div>
```
`#step-2` 의 `<ul>` 에 한 줄 추가: `<li th:text="#{guide.step2.li3}">일정 설명</li>`. 로그인 배너 위 주석 "5단계 가이드 직후" → "6단계 가이드 직후". (문제 해결 6문항·활용 사례·CTA 문구는 2026-10-14 점검에서 현재 사실과 맞아 변경하지 않는다.)

- [ ] **Step 4: 메시지** — `set_props.py` 로 교체 + `add_props.py` 로 추가.

ko 교체(`$S/guide_ko.json`):
```json
{
  "guide.hero.subtitle": [null, "6단계로 여러명의 약속을 쉽게 잡는 방법"],
  "guide.step2.p": [null, "대시보드에서 원하는 <strong>월(月)을 고르고</strong> <strong>이 일정 만들기</strong>를 누르면 그 달의 일정 페이지가 생깁니다."],
  "guide.step5.title": [null, "시간 정하기"],
  "guide.step5.p": [null, "누군가 가능하다고 고른 날에 <strong>시간 후보</strong>를 올리고, 참여자들이 되는 시간에 <strong>투표</strong>합니다."],
  "guide.step5.li1": [null, "후보는 30분 단위(예: 19:00–21:00)이고 자정을 넘길 수 없음"],
  "guide.step5.li2": [null, "참여자가 가능하다고 고른 날짜에만 후보를 올릴 수 있음"],
  "guide.step5.li3": [null, "겹치는 시간 차트로 가장 많은 사람이 되는 시간대를 한눈에 확인"],
  "seo.guide.description": ["5단계로", "6단계로"],
  "seo.guide.ogDescription": [null, "6단계로 쉽게! 링크 하나로 날짜·시간·장소·메뉴까지 한번에 정하는 방법을 알아보세요."],
  "seo.guide.howTo.description": [null, "DateDate를 사용하여 그룹 일정을 조율하는 방법을 6단계로 안내합니다."],
  "seo.guide.step2.text": [null, "대시보드에서 월을 고르고 이 일정 만들기를 누르면 그 달의 일정 페이지가 생깁니다."],
  "seo.guide.step5.name": [null, "시간 정하기"],
  "seo.guide.step5.text": [null, "누군가 가능하다고 고른 날에 30분 단위 시간 후보를 올리고 참여자들이 투표합니다. 겹치는 시간 차트로 가장 많은 사람이 되는 시간을 확인합니다."]
}
```
`seo.guide.description` 에 "장소·메뉴 투표, 최종 확정까지" 가 있으면 `["장소·메뉴 투표, 최종 확정까지", "장소·메뉴 투표, 시간 투표, 최종 확정까지"]` 를 같은 파일에 한 번 더 적용한다(키 1개당 1회 규칙이라 별도 JSON).

ko 추가(`add_props.py`, 앵커 `guide.step5.li3` — 위 교체 후 실행) `$S/guide_ko_add.txt`:
```
guide.step6.title=결과 확인 & 결정
guide.step6.p=가장 많은 사람이 가능한 날짜가 <strong>자동으로 하이라이트</strong>됩니다.
guide.step6.li1=겹치는 날짜가 많을수록 진한 색상으로 표시
guide.step6.li2=인기 장소와 메뉴 순위도 함께 확인
guide.step6.li3=최종 약속 날짜, 시간, 장소, 메뉴를 결정하세요
```
앵커 `guide.step2.li2` 에 `guide.step2.li3=일정 설명(최대 500자)에 장소 후보·회비·링크를 적어 둘 수 있음`, 앵커 `seo.guide.step5.text` 에:
```
seo.guide.step6.name=결과 확인 & 결정
seo.guide.step6.text=가장 많은 사람이 가능한 날짜, 가장 인기있는 장소와 메뉴를 확인하고 최종 약속을 결정합니다.
```

en 교체(`$S/guide_en.json`):
```json
{
  "guide.hero.subtitle": [null, "How to coordinate a group schedule in 6 steps"],
  "guide.step2.p": [null, "Pick the <strong>month</strong> on the dashboard and click <strong>Create this schedule</strong> to open that month's schedule page."],
  "guide.step5.title": [null, "Pick a Time"],
  "guide.step5.p": [null, "On a day someone marked as available, <strong>propose time options</strong> and let participants <strong>vote</strong> for the times that work."],
  "guide.step5.li1": [null, "Options use 30-minute steps (for example, 19:00–21:00) and can't cross midnight"],
  "guide.step5.li2": [null, "You can only propose times on dates someone has marked as available"],
  "guide.step5.li3": [null, "The overlap chart shows which time works for the most people"],
  "seo.guide.description": ["in 5 steps", "in 6 steps"],
  "seo.guide.ogDescription": [null, "6 easy steps! Lock in dates, times, places, and menus with one link."],
  "seo.guide.howTo.description": [null, "A 6-step guide to coordinating group schedules using DateDate."],
  "seo.guide.step2.text": [null, "Pick a month on the dashboard and click Create this schedule to open that month's schedule page."],
  "seo.guide.step5.name": [null, "Pick a Time"],
  "seo.guide.step5.text": [null, "On a day someone marked as available, propose 30-minute time options and let participants vote. The overlap chart shows which time works for the most people."]
}
```
en 추가: 앵커 `guide.step5.li3` →
```
guide.step6.title=Review Results & Decide
guide.step6.p=The date with the most availability is <strong>highlighted automatically</strong>.
guide.step6.li1=Dates with more overlap appear in darker colors
guide.step6.li2=See popular location and menu rankings too
guide.step6.li3=Finalize your date, time, location, and menu
```
앵커 `guide.step2.li2` → `guide.step2.li3=Add a description (up to 500 characters) for venue ideas, fees, or links`, 앵커 `seo.guide.step5.text` →
```
seo.guide.step6.name=Review Results & Decide
seo.guide.step6.text=Check the dates most people are available, the top-voted venues and menus, and finalize your plan.
```
(`guide.step5.*` 의 문구는 인자 없는 메시지라 `'` 1개.)

- [ ] **Step 5: `SeoService.getGuideSeo`** — `"position": 5` 단계 객체 뒤에 6단계 객체를 추가하고 `formatted(...)` 인자에 한 줄 추가:

```java
                    {
                        "@type": "HowToStep",
                        "position": 6,
                        "name": "%s",
                        "text": "%s",
                        "url": "%s/guide#step-6"
                    }
```
```java
                mJson("seo.guide.step6.name"), mJson("seo.guide.step6.text"), baseUrl,
```
(`"position": 5` 객체의 닫는 `}` 뒤에 `,` 추가. SeoService.java 개행 유지.)

- [ ] **Step 6: 통과 확인** — Step 2 명령 + `--tests me.singingsandhill.calendar.common.presentation.StructuredDataVisibilityTest`. Expected: 전부 PASS(HowTo 6단계 이름이 화면에 있음).

### Task 5: 스크린샷 만들기 (1회성 — 도구는 커밋하지 않음)

**Files:**
- Create(임시, 끝나면 삭제): `src/test/java/me/singingsandhill/calendar/datedate/presentation/controller/GuideScreenshotFixtureProbeTest.java`
- Create: `src/main/resources/static/image/guide/ko/step{1..6}.webp`, `src/main/resources/static/image/guide/en/step{1..6}.webp`

**Interfaces:**
- Produces: 이미지 12장 + 각 이미지의 픽셀 크기(Task 6 의 `width`/`height` 메시지 값)

- [ ] **Step 1: 임시 프로브 작성** — 테스트 DB 에 예시 일정 2개(ko·en)를 넣고 페이지 6종 HTML 을 `build/guide-shots/` 에 덤프.

```java
package me.singingsandhill.calendar.datedate.presentation.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import me.singingsandhill.calendar.datedate.application.service.LocationService;
import me.singingsandhill.calendar.datedate.application.service.MenuService;
import me.singingsandhill.calendar.datedate.application.service.ParticipantService;
import me.singingsandhill.calendar.datedate.application.service.ScheduleService;
import me.singingsandhill.calendar.datedate.application.service.TimeSlotService;
import me.singingsandhill.calendar.datedate.domain.location.Location;
import me.singingsandhill.calendar.datedate.domain.menu.Menu;
import me.singingsandhill.calendar.datedate.domain.participant.Participant;
import me.singingsandhill.calendar.datedate.domain.schedule.Schedule;
import me.singingsandhill.calendar.datedate.domain.timeslot.TimeSlot;

/** TEMPORARY PROBE — /guide 스크린샷용 렌더 HTML 덤프. 캡처 후 삭제. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GuideScreenshotFixtureProbeTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ScheduleService scheduleService;
    @Autowired private ParticipantService participantService;
    @Autowired private TimeSlotService timeSlotService;
    @Autowired private LocationService locationService;
    @Autowired private MenuService menuService;

    @Test
    void dumpGuidePages() throws Exception {
        Path out = Path.of("build", "guide-shots");
        Files.createDirectories(out);
        seed("college-friends", List.of("지민", "서준", "하은", "도윤", "수아"),
                "강남역", "홍대입구", "삼겹살", "파스타",
                "11월 정기 모임 — 회비 2만 원, 장소는 아래 투표로 정해요.");
        seed("team-dinner", List.of("Alex", "Jamie", "Sam", "Taylor", "Jordan"),
                "Downtown", "Riverside", "Pizza", "Tacos",
                "November team dinner — about $25 each. Vote on the venue below.");
        dump(out, "ko-home", "/", "ko");
        dump(out, "en-home", "/", "en");
        dump(out, "ko-create", "/college-friends/2026/12", "ko");
        dump(out, "en-create", "/team-dinner/2026/12", "en");
        dump(out, "ko-view", "/college-friends/2026/11", "ko");
        dump(out, "en-view", "/team-dinner/2026/11", "en");
    }

    /** 2026-11-01 은 일요일 → 7주 그리드 시작이 11-01 이라 인덱스 = 11월 날짜. 14일(토) 5명, 21일(토) 4명이 겹친다. */
    private void seed(String ownerId, List<String> names, String place1, String place2,
                      String menu1, String menu2, String description) {
        Schedule s = scheduleService.createSchedule(ownerId, 2026, 11, null);
        scheduleService.updateSchedule(ownerId, 2026, 11, null, description);
        int[][] picks = {{6, 7, 14, 21}, {7, 14, 20, 21}, {13, 14, 21}, {14, 21, 28}, {7, 14, 27}};
        for (int i = 0; i < names.size(); i++) {
            Participant p = participantService.addParticipant(s.getId(), names.get(i));
            participantService.updateSelections(p.getId(), Arrays.stream(picks[i]).boxed().toList());
        }
        TimeSlot evening = timeSlotService.addTimeSlot(s.getId(), 14, 18 * 60, 20 * 60);
        TimeSlot lunch = timeSlotService.addTimeSlot(s.getId(), 14, 12 * 60, 14 * 60);
        for (int i = 0; i < 4; i++) timeSlotService.vote(evening.getId(), names.get(i));
        for (int i = 0; i < 2; i++) timeSlotService.vote(lunch.getId(), names.get(i));
        Location l1 = locationService.addLocation(s.getId(), place1);
        Location l2 = locationService.addLocation(s.getId(), place2);
        for (int i = 0; i < 3; i++) locationService.vote(l1.getId(), names.get(i));
        for (int i = 3; i < 5; i++) locationService.vote(l2.getId(), names.get(i));
        Menu m1 = menuService.addMenu(s.getId(), menu1, null);
        Menu m2 = menuService.addMenu(s.getId(), menu2, null);
        for (int i = 0; i < 3; i++) menuService.vote(m1.getId(), names.get(i));
        for (int i = 3; i < 5; i++) menuService.vote(m2.getId(), names.get(i));
    }

    private void dump(Path out, String name, String path, String lang) throws Exception {
        byte[] html = mockMvc.perform(get(path).param("lang", lang))
                .andReturn().getResponse().getContentAsByteArray();
        Files.write(out.resolve(name + ".html"), html);
    }
}
```

Run: `cmd.exe /c "set JAVA_HOME=C:\\jdk-21&& .\\gradlew.bat test --tests me.singingsandhill.calendar.datedate.presentation.controller.GuideScreenshotFixtureProbeTest"` → `build/guide-shots/` 에 HTML 6개. (서비스 검증 예외가 나면 프로브 데이터를 고친다 — 예: 후보 시간 범위, 이름 길이 10자.)

- [ ] **Step 2: 하네스 사이트** — `$S/guide-site/` 에 HTML 을 복사하면서 해시 제거(`sed -E 's#-[0-9a-f]{32}\.(css|js|png|svg|webp)#.\1#g'`), `css`·`js`·`image` 를 `src/main/resources/static/*` 로 심볼릭 링크. `python3 -m http.server 8766 --bind 127.0.0.1`(백그라운드).

- [ ] **Step 3: CDP 캡처 스크립트** `$S/guide-shots.mjs` — 390×844 모바일, deviceScaleFactor 2, 요소 영역만 WebP(quality 80).

```javascript
// usage: node --experimental-websocket guide-shots.mjs <port> <outDir(windows path)>
import fs from 'node:fs';
import path from 'node:path';
const [port, outDir] = process.argv.slice(2);
const JOBS = [
  ['step1', 'home', '#start-form'],
  ['step2', 'create', '#main-content'],
  ['step3', 'view', '.schedule-header'],
  ['step4', 'view', '.participant-section'],
  ['step5', 'view', '.time-section'],
  ['step6', 'view', '#locationList'],
];
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const version = await (await fetch(`http://127.0.0.1:${port}/json/version`)).json();
const ws = new WebSocket(version.webSocketDebuggerUrl);
await new Promise((r) => ws.addEventListener('open', r, { once: true }));
let seq = 0; const pending = new Map();
ws.addEventListener('message', (ev) => { const m = JSON.parse(ev.data); if (m.id && pending.has(m.id)) { pending.get(m.id)(m); pending.delete(m.id); } });
const send = (method, params = {}, sessionId) => new Promise((ok, no) => { const id = ++seq;
  pending.set(id, (m) => (m.error ? no(new Error(method + ' ' + JSON.stringify(m.error))) : ok(m.result)));
  ws.send(JSON.stringify({ id, method, params, sessionId })); });
const results = [];
for (const lang of ['ko', 'en']) {
  for (const [step, page, selector] of JOBS) {
    const { targetId } = await send('Target.createTarget', { url: 'about:blank' });
    const { sessionId } = await send('Target.attachToTarget', { targetId, flatten: true });
    await send('Network.enable', {}, sessionId);
    await send('Network.setCacheDisabled', { cacheDisabled: true }, sessionId);
    await send('Page.enable', {}, sessionId);
    await send('Emulation.setDeviceMetricsOverride', { width: 390, height: 844, deviceScaleFactor: 2, mobile: true }, sessionId);
    await send('Page.navigate', { url: `http://localhost:8766/${lang}-${page}.html` }, sessionId);
    await sleep(2500);
    const rect = (await send('Runtime.evaluate', { returnByValue: true, expression: `(() => {
      document.querySelectorAll('.animate-on-scroll').forEach(e => e.classList.add('animate-in'));
      const banner = document.getElementById('onboardingBanner'); if (banner) banner.remove();
      const cells = document.querySelectorAll('#calendarBody .calendar-day').length;
      const slots = document.querySelectorAll('#timeSlotList > *').length;
      let el = document.querySelector('${selector}');
      if ('${selector}' === '#locationList') el = el.closest('.location-section');
      const r = el.getBoundingClientRect();
      return { x: r.left + scrollX, y: r.top + scrollY, w: r.width, h: r.height, cells, slots };
    })()` }, sessionId)).result.value;
    if (page === 'view' && (rect.cells === 0 || rect.slots === 0)) throw new Error(`${lang} ${step}: 달력/시간 후보가 렌더되지 않음 — 콘솔 확인`);
    const pad = 8;
    const clip = { x: Math.max(0, rect.x - pad), y: Math.max(0, rect.y - pad), width: Math.min(390, rect.w + pad * 2), height: rect.h + pad * 2, scale: 1 };
    const shot = await send('Page.captureScreenshot', { format: 'webp', quality: 80, captureBeyondViewport: true, clip }, sessionId);
    const file = path.join(outDir, lang, `${step}.webp`);
    fs.mkdirSync(path.dirname(file), { recursive: true });
    fs.writeFileSync(file, Buffer.from(shot.data, 'base64'));
    results.push({ lang, step, width: Math.round(clip.width * 2), height: Math.round(clip.height * 2), bytes: fs.statSync(file).size });
    await send('Target.closeTarget', { targetId });
  }
}
fs.writeFileSync(path.join(outDir, 'sizes.json'), JSON.stringify(results, null, 2));
console.log(JSON.stringify(results, null, 2));
await send('Browser.close').catch(() => {});
process.exit(0);
```

- [ ] **Step 4: 캡처 실행**
  1. 전용 프로필 Chrome: `"/mnt/c/Program Files/Google/Chrome/Application/chrome.exe" --headless=new --disable-gpu --hide-scrollbars --remote-debugging-port=9222 --user-data-dir='C:\Temp\cc-guide-shots' about:blank` (백그라운드)
  2. `W=$(wslpath -w $S)` 후 `cmd.exe /c "node --experimental-websocket $W\\guide-shots.mjs 9222 D:\\projects\\calendar\\src\\main\\resources\\static\\image\\guide"`
  3. `sizes.json` 은 `static/image/guide/` 에 생기므로 `$S/` 로 옮긴다(커밋 대상 아님).
  4. 각 이미지를 Read 로 열어 눈으로 확인 — 예시 이름·겹침·시간 후보·장소 투표가 보이는지, 잘림 없는지. `bytes` 가 80KB 를 넘으면 quality 70 으로 다시.
  5. 정리: http.server 종료(PID 로 kill), `cmd.exe /c "rmdir /s /q C:\\Temp\\cc-guide-shots"`, 프로브 테스트 파일 삭제, `build/guide-shots` 삭제.

### Task 6: /guide 에 스크린샷 + 캡션

**Files:**
- Modify: `src/test/java/me/singingsandhill/calendar/datedate/presentation/controller/GuidePageRenderingTest.java` (테스트 추가)
- Modify: `src/main/resources/templates/guide.html`
- Modify: `messages.properties`, `messages_en.properties`
- Modify: `src/main/resources/static/css/style.css` (`.guide-tip` 블록 다음)

**Interfaces:**
- Consumes: Task 5 의 이미지 12장, `sizes.json` 의 width/height
- Produces: 키 `guide.screenshot.caption`, `guide.screenshot.width`, `guide.step{1..6}.imgAlt`, `guide.step{1..6}.imgHeight`

- [ ] **Step 1: 실패하는 테스트 — `GuidePageRenderingTest` 에 추가**

```java
    @org.junit.jupiter.params.ParameterizedTest(name = "/guide [{0}]")
    @org.junit.jupiter.params.provider.ValueSource(strings = {"ko", "en"})
    @DisplayName("단계마다 스크린샷이 있고 width·height·alt 를 갖추며, 버전 URL 로 실제 서빙된다")
    void everyStepHasServedScreenshot(String lang) throws Exception {
        String html = render(lang);
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("<img[^>]*class=\"guide-shot-img\"[^>]*>").matcher(html);
        int count = 0;
        while (m.find()) {
            String tag = m.group();
            assertThat(tag).containsPattern("width=\"\\d+\"").containsPattern("height=\"\\d+\"")
                    .containsPattern("alt=\"[^\"]+\"");
            String src = tag.replaceAll(".*src=\"([^\"]+)\".*", "$1");
            assertThat(src).contains("/image/guide/" + lang + "/step");
            mockMvc.perform(get(src))
                    .andExpect(status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                            .content().contentType("image/webp"));
            count++;
        }
        assertThat(count).isEqualTo(6);
        assertThat(visibleText(html)).contains("ko".equals(lang) ? "예시 화면" : "Example screen");
    }
```

- [ ] **Step 2: 실패 확인** — `--tests …GuidePageRenderingTest`. Expected: `everyStepHasServedScreenshot` 2건 FAIL(img 0개).

- [ ] **Step 3: 템플릿** — 각 `#step-N` 의 `.guide-step-content` 마지막(`</ul>` 또는 `.guide-tip` 뒤)에 N 을 바꿔 넣는다. 1단계는 `loading="eager"`, 2~6단계는 `loading="lazy"`.

```html
                <figure class="guide-shot">
                    <img class="guide-shot-img"
                         th:src="@{'/image/guide/' + ${#locale.language == 'en' ? 'en' : 'ko'} + '/step1.webp'}"
                         th:alt="#{guide.step1.imgAlt}"
                         th:width="#{guide.screenshot.width}" th:height="#{guide.step1.imgHeight}"
                         loading="eager" decoding="async">
                    <figcaption th:text="#{guide.screenshot.caption}">예시 화면 — 이름과 날짜는 예시입니다</figcaption>
                </figure>
```

- [ ] **Step 4: 메시지 추가** — ko(앵커 `guide.step6.li3`):
```
guide.screenshot.caption=예시 화면 — 이름과 날짜는 예시입니다
guide.screenshot.width=780
guide.step1.imgAlt=홈 화면의 페이지 ID 입력 폼
guide.step2.imgAlt=이 일정 만들기 버튼이 있는 새 일정 화면
guide.step3.imgAlt=일정 페이지 상단의 링크 복사 버튼
guide.step4.imgAlt=참여자 5명이 가능한 날짜를 표시한 달력
guide.step5.imgAlt=시간 후보 목록과 겹치는 시간 차트
guide.step6.imgAlt=장소 후보와 투표 수
guide.step1.imgHeight=<sizes.json 의 ko step1 height>
guide.step2.imgHeight=<ko step2 height>
guide.step3.imgHeight=<ko step3 height>
guide.step4.imgHeight=<ko step4 height>
guide.step5.imgHeight=<ko step5 height>
guide.step6.imgHeight=<ko step6 height>
```
en(앵커 `guide.step6.li3`): caption `Example screen — names and dates are samples`, width `780`, alt — `The page ID form on the home page` / `New schedule screen with the Create this schedule button` / `Copy link button at the top of a schedule page` / `Calendar with available dates marked by five participants` / `Time options list and the time overlap chart` / `Venue options with vote counts`, imgHeight 은 en 값.
(`<…>` 는 Task 5 Step 4 의 `sizes.json` 실측값을 그대로 적는 자리 — 캡처 결과가 있어야 정해지는 값이다. width 가 780 이 아닌 줄이 있으면 그 줄은 `guide.stepN.imgWidth` 키를 따로 두지 말고 캡처를 390 폭으로 다시 맞춘다.)

- [ ] **Step 5: CSS** — `$S/guide-shot.css` 를 `css_insert.py … '^\.guide-tip \{' …` 로 삽입 후 numstat 두 개 비교.

```css
/* /guide 단계 스크린샷 (예시 화면) */
.guide-shot {
    margin: 1rem 0 0;
    max-width: 390px;
}

.guide-shot-img {
    display: block;
    width: 100%;
    height: auto;
    border: 1px solid var(--border-color);
    border-radius: 12px;
}

.guide-shot figcaption {
    margin-top: 0.5rem;
    font-size: 0.8rem;
    color: var(--text-light);
}
```

- [ ] **Step 6: 통과 확인** — `--tests …GuidePageRenderingTest --tests …StructuredDataVisibilityTest --tests …HeadSeoSmokeTest`. Expected: 전부 PASS. content-type 이 `image/webp` 가 아니면 우회하지 말고 원인(정적 리소스 핸들러의 MIME 매핑)부터 조사한다.

- [ ] **Step 7: 하네스로 /guide 화면 확인** — ①과 같은 방식(MockMvc 덤프 → 해시 제거 → http.server → CDP)으로 `/guide` ko 375·1280px 캡처, 이미지 6장·캡션·가로 넘침 없음 확인. 끝나면 정리.

### Task 7: ②-2 문서·전체 검증·커밋 로그

- [ ] **Step 1: 루트 `CLAUDE.md`** — DateDate Module 목록에 줄 추가: "- `/guide` 사용 가이드 6단계(5단계 = 시간 투표) — 단계별 예시 화면 스크린샷 `static/image/guide/{ko,en}/step{1..6}.webp`(UI 가 바뀌면 재촬영 — 절차는 커밋 로그 Commit 200). HowTo JSON-LD 단계는 화면과 같은 키(`seo.guide.step1~6`)."
- [ ] **Step 2: 전체 스위트** — Expected: failures 0. 135/876 → GuidePageRenderingTest(+1 클래스: 6단계 2 + 이미지 2) + HowTo 1 = 136 / 881.
- [ ] **Step 3: 커밋 로그 섹션 (Commit 200)** — 재촬영 절차(Task 5 의 프로브·하네스·CDP 요약)를 섹션 주석에 남긴다. `git add`:

```
git add src/main/resources/templates/guide.html src/main/resources/messages.properties src/main/resources/messages_en.properties src/main/java/me/singingsandhill/calendar/datedate/application/service/SeoService.java src/main/resources/static/css/style.css src/main/resources/static/image/guide src/test/java/me/singingsandhill/calendar/datedate/presentation/controller/GuidePageRenderingTest.java src/test/java/me/singingsandhill/calendar/datedate/application/service/SeoServiceI18nTest.java CLAUDE.md docs/guides/git-commit.md
```

---

## 파트 ②-3 모임 데이터 리포트 (Commit 201)

### Task 8: `YearMonth.dateOf` + `ParticipantAvailability`

**Files:**
- Modify: `src/main/java/me/singingsandhill/calendar/datedate/domain/schedule/YearMonth.java`
- Create: `src/main/java/me/singingsandhill/calendar/datedate/domain/participant/ParticipantAvailability.java`
- Modify: `src/test/java/me/singingsandhill/calendar/datedate/domain/schedule/YearMonthTest.java`
- Create: `src/test/java/me/singingsandhill/calendar/datedate/domain/participant/ParticipantAvailabilityTest.java`

**Interfaces:**
- Produces: `Optional<LocalDate> YearMonth.dateOf(int index, int weeks)`; `record ParticipantAvailability(Long scheduleId, int year, int month, int weeks, LocalDateTime scheduleCreatedAt, List<Integer> selections)` + `List<LocalDate> availableDates()`

- [ ] **Step 1: 실패하는 테스트 — `YearMonthTest` 에 추가**

```java
    @Test
    @DisplayName("dateOf — 7주 확장 모드는 그리드 시작(1일이 속한 주의 일요일)부터 센다")
    void dateOf_extendedMode() {
        YearMonth oct = new YearMonth(2026, 10);   // 10-01 목요일 → 그리드 시작 09-27(일)
        assertThat(oct.dateOf(1, 7)).contains(java.time.LocalDate.of(2026, 9, 27));
        assertThat(oct.dateOf(5, 7)).contains(java.time.LocalDate.of(2026, 10, 1));
        assertThat(oct.dateOf(49, 7)).contains(java.time.LocalDate.of(2026, 11, 14));
        assertThat(oct.dateOf(0, 7)).isEmpty();
        assertThat(oct.dateOf(50, 7)).isEmpty();
    }

    @Test
    @DisplayName("dateOf — 기존 모드(weeks != 7)는 인덱스 = 그 달의 날짜 (확장 규칙이면 요일이 어긋난다)")
    void dateOf_legacyMode() {
        YearMonth oct = new YearMonth(2026, 10);
        assertThat(oct.dateOf(1, 5)).contains(java.time.LocalDate.of(2026, 10, 1));
        assertThat(oct.dateOf(31, 5)).contains(java.time.LocalDate.of(2026, 10, 31));
        assertThat(oct.dateOf(32, 5)).isEmpty();
        assertThat(oct.dateOf(0, 5)).isEmpty();
    }
```

`ParticipantAvailabilityTest`:

```java
package me.singingsandhill.calendar.datedate.domain.participant;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ParticipantAvailabilityTest {

    private static final LocalDateTime CREATED = LocalDateTime.of(2026, 10, 1, 9, 0);

    @Test
    @DisplayName("고른 인덱스를 실제 날짜로 — 0·범위 밖·null 은 건너뛰고 중복은 한 번만")
    void convertsAndSkipsInvalid() {
        ParticipantAvailability a = new ParticipantAvailability(1L, 2026, 10, 5, CREATED,
                Arrays.asList(1, 0, 32, null, 3, 3));
        assertThat(a.availableDates()).containsExactly(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3));
    }

    @Test
    @DisplayName("selections 가 null·빈 목록이면 빈 결과")
    void emptySelections() {
        assertThat(new ParticipantAvailability(1L, 2026, 10, 7, CREATED, null).availableDates()).isEmpty();
        assertThat(new ParticipantAvailability(1L, 2026, 10, 7, CREATED, List.of()).availableDates()).isEmpty();
    }
}
```

- [ ] **Step 2: 실패 확인** — `--tests me.singingsandhill.calendar.datedate.domain.schedule.YearMonthTest --tests me.singingsandhill.calendar.datedate.domain.participant.ParticipantAvailabilityTest`. Expected: 컴파일 실패(`dateOf`·`ParticipantAvailability` 없음).

- [ ] **Step 3: 구현** — `YearMonth` 에 추가(파일 개행 유지, `import java.util.Optional;`):

```java
    /**
     * 참여자 선택 인덱스를 실제 날짜로 — 웹 달력(static/js/schedule/calendar.js)의 두 렌더 경로와 같은 규칙.
     * 7주 확장 모드(weeks == FIXED_WEEKS)는 그리드 시작 + (index − 1), 기존 모드는 그 달의 index 일. 범위 밖은 빈 값.
     */
    public Optional<LocalDate> dateOf(int index, int weeks) {
        if (weeks == FIXED_WEEKS) {
            return index >= 1 && index <= FIXED_TOTAL_DAYS ? Optional.of(indexToDate(index)) : Optional.empty();
        }
        return index >= 1 && index <= getDaysInMonth()
                ? Optional.of(LocalDate.of(year, month, index))
                : Optional.empty();
    }
```

`ParticipantAvailability.java`:

```java
package me.singingsandhill.calendar.datedate.domain.participant;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import me.singingsandhill.calendar.datedate.domain.schedule.YearMonth;

/**
 * 모임 데이터 리포트용 읽기 모델 — 참여자 한 명의 고른 날짜와 그 일정의 연월·주 모드·생성 시각
 * (ADR datedate/domain/0013). 이름·ID 는 담지 않는다.
 */
public record ParticipantAvailability(Long scheduleId, int year, int month, int weeks,
                                      LocalDateTime scheduleCreatedAt, List<Integer> selections) {

    /** 고른 날짜를 실제 날짜로 — null·범위 밖 인덱스는 건너뛰고 중복은 한 번만 센다. */
    public List<LocalDate> availableDates() {
        if (selections == null || selections.isEmpty()) {
            return List.of();
        }
        YearMonth ym = new YearMonth(year, month);
        return selections.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(index -> ym.dateOf(index, weeks))
                .flatMap(Optional::stream)
                .toList();
    }
}
```

- [ ] **Step 4: 통과 확인** — Step 2 명령. Expected: PASS.

### Task 9: 읽기 포트 2개 + JPA 어댑터

**Files:**
- Modify: `src/main/java/me/singingsandhill/calendar/datedate/domain/participant/ParticipantRepository.java`
- Modify: `src/main/java/me/singingsandhill/calendar/datedate/domain/schedule/ScheduleRepository.java`
- Modify: `src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/repository/ParticipantJpaRepository.java`
- Modify: `src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/repository/ScheduleJpaRepository.java`
- Modify: `src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/adapter/ParticipantRepositoryAdapter.java`
- Modify: `src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/adapter/ScheduleRepositoryAdapter.java`
- Create: `src/test/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/adapter/MeetupReportQueryTest.java`

**Interfaces:**
- Consumes: `ParticipantAvailability` (Task 8)
- Produces: `List<ParticipantAvailability> ParticipantRepository.findAllAvailability()`, `List<LocalDateTime> ScheduleRepository.findAllCreatedAt()`

- [ ] **Step 1: 실패하는 테스트**

```java
package me.singingsandhill.calendar.datedate.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import me.singingsandhill.calendar.datedate.application.service.ParticipantService;
import me.singingsandhill.calendar.datedate.application.service.ScheduleService;
import me.singingsandhill.calendar.datedate.domain.participant.Participant;
import me.singingsandhill.calendar.datedate.domain.participant.ParticipantAvailability;
import me.singingsandhill.calendar.datedate.domain.participant.ParticipantRepository;
import me.singingsandhill.calendar.datedate.domain.schedule.Schedule;
import me.singingsandhill.calendar.datedate.domain.schedule.ScheduleRepository;

/** 리포트 읽기 쿼리 — 일정 필드와 selections 변환(SelectionListConverter)이 record 에 그대로 실린다. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MeetupReportQueryTest {

    @Autowired private ScheduleService scheduleService;
    @Autowired private ParticipantService participantService;
    @Autowired private ParticipantRepository participantRepository;
    @Autowired private ScheduleRepository scheduleRepository;

    @Test
    @DisplayName("findAllAvailability — 일정 id·연월·주 모드·생성 시각·고른 인덱스를 담는다")
    void availabilityCarriesScheduleFieldsAndSelections() {
        Schedule s = scheduleService.createSchedule("report-query-x1", 2026, 11, null);
        Participant p = participantService.addParticipant(s.getId(), "민수");
        participantService.updateSelections(p.getId(), List.of(3, 14));

        List<ParticipantAvailability> rows = participantRepository.findAllAvailability().stream()
                .filter(r -> r.scheduleId().equals(s.getId())).toList();

        assertThat(rows).hasSize(1);
        ParticipantAvailability row = rows.get(0);
        assertThat(row.year()).isEqualTo(2026);
        assertThat(row.month()).isEqualTo(11);
        assertThat(row.weeks()).isEqualTo(7);
        assertThat(row.scheduleCreatedAt()).isEqualTo(s.getCreatedAt());
        assertThat(row.selections()).containsExactly(3, 14);
    }

    @Test
    @DisplayName("findAllCreatedAt — 일정 생성 시각 목록에 새 일정이 들어 있다")
    void createdAtListContainsSchedule() {
        Schedule s = scheduleService.createSchedule("report-query-x2", 2026, 11, null);
        assertThat(scheduleRepository.findAllCreatedAt()).contains(s.getCreatedAt());
    }
}
```

- [ ] **Step 2: 실패 확인** — `--tests me.singingsandhill.calendar.datedate.infrastructure.persistence.adapter.MeetupReportQueryTest`. Expected: 컴파일 실패(메서드 없음).

- [ ] **Step 3: 구현** — 포트:

```java
    // ParticipantRepository
    /** 모임 데이터 리포트용 — 전체 참여자의 고른 날짜와 일정 정보 (ADR datedate/domain/0013). */
    List<ParticipantAvailability> findAllAvailability();
```
```java
    // ScheduleRepository  (import java.time.LocalDateTime)
    /** 모임 데이터 리포트용 — 전체 일정의 생성 시각. */
    List<LocalDateTime> findAllCreatedAt();
```

JPA:
```java
    // ParticipantJpaRepository — 일정과 함께 한 번에 읽는다(일정마다 지연 로딩하면 일정 수만큼 쿼리)
    @Query("SELECT p FROM ParticipantJpaEntity p JOIN FETCH p.schedule")
    List<ParticipantJpaEntity> findAllWithSchedule();
```
```java
    // ScheduleJpaRepository  (import java.time.LocalDateTime)
    @Query("SELECT s.createdAt FROM ScheduleJpaEntity s")
    List<LocalDateTime> findAllCreatedAt();
```

어댑터:
```java
    // ParticipantRepositoryAdapter  (import ParticipantAvailability)
    @Override
    public List<ParticipantAvailability> findAllAvailability() {
        return participantJpaRepository.findAllWithSchedule().stream()
                .map(p -> new ParticipantAvailability(
                        p.getSchedule().getId(),
                        p.getSchedule().getYear(),
                        p.getSchedule().getMonth(),
                        p.getSchedule().getWeeks(),
                        p.getSchedule().getCreatedAt(),
                        p.getSelections()))
                .toList();
    }
```
```java
    // ScheduleRepositoryAdapter  (import java.time.LocalDateTime)
    @Override
    public List<LocalDateTime> findAllCreatedAt() {
        return scheduleJpaRepository.findAllCreatedAt();
    }
```
(필드 이름: `ParticipantRepositoryAdapter.participantJpaRepository`, `ScheduleRepositoryAdapter.scheduleJpaRepository`. 각 파일 개행 유지.)

- [ ] **Step 4: 통과 확인** — Step 2 명령. Expected: 2 tests PASS.

### Task 10: 리포트 계산 클래스 + DTO

**Files:**
- Create: `src/main/java/me/singingsandhill/calendar/datedate/application/dto/MeetupReportDto.java`
- Create: `src/main/java/me/singingsandhill/calendar/datedate/application/service/MeetupReportCalculator.java`
- Create: `src/test/java/me/singingsandhill/calendar/datedate/application/service/MeetupReportCalculatorTest.java`

**Interfaces:**
- Consumes: `ParticipantAvailability.availableDates()`, `scheduleId()`, `scheduleCreatedAt()` (Task 8)
- Produces: `MeetupReportDto MeetupReportCalculator.calculate(List<ParticipantAvailability>, List<LocalDateTime>, LocalDate today)`; `MeetupReportDto(LocalDate asOf, WeekdayBlock weekdays, MonthlyBlock monthly, LeadTimeBlock leadTime)` — 블록은 하한 미달이면 null. `Share(String key, int percent)`, `WeekdayBlock(int sampleSize, List<Share> shares)` — key = `DayOfWeek.name()` 일~토 순, `MonthlyBlock(int total, List<MonthCount> months)`, `MonthCount(int year, int month, int count, int barPercent, boolean inProgress)`, `LeadTimeBlock(int sampleSize, int medianDays, List<Share> buckets)` — key `D0_6`·`D7_13`·`D14_27`·`D28_PLUS`.

- [ ] **Step 1: 실패하는 테스트** (기대값은 모두 손으로 계산 — 주석에 근거)

```java
package me.singingsandhill.calendar.datedate.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import me.singingsandhill.calendar.datedate.application.dto.MeetupReportDto;
import me.singingsandhill.calendar.datedate.application.dto.MeetupReportDto.MonthCount;
import me.singingsandhill.calendar.datedate.application.dto.MeetupReportDto.Share;
import me.singingsandhill.calendar.datedate.domain.participant.ParticipantAvailability;

/** 모임 데이터 리포트 계산 (ADR datedate/domain/0013). 입력은 손으로 만들고 기대값도 손으로 계산했다. */
class MeetupReportCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 14);
    private static final LocalDateTime OCT_1 = LocalDateTime.of(2026, 10, 1, 10, 0);

    /** 2026-10 기존 모드(weeks 5) — 인덱스 = 10월 날짜. 10-01 목, 02 금, 03 토, 04 일, 10 토. */
    private static ParticipantAvailability oct(long scheduleId, LocalDateTime created, Integer... days) {
        return new ParticipantAvailability(scheduleId, 2026, 10, 5, created, List.of(days));
    }

    private static List<ParticipantAvailability> weekdayFixture(int participants) {
        List<ParticipantAvailability> list = new ArrayList<>();
        for (int i = 0; i < participants; i++) {
            list.add(oct(i, OCT_1, 1, 2, 3, 4, 10));   // 목1 금1 토2 일1
        }
        return list;
    }

    @Test
    @DisplayName("요일 비율 — 60명 × 5날짜 = 300: 일20 목20 금20 토40 (기존 모드 인덱스 = 그 달 날짜)")
    void weekdayShares() {
        MeetupReportDto r = MeetupReportCalculator.calculate(weekdayFixture(60), List.of(), TODAY);

        assertThat(r.weekdays().sampleSize()).isEqualTo(300);
        assertThat(r.weekdays().shares()).containsExactly(
                new Share("SUNDAY", 20), new Share("MONDAY", 0), new Share("TUESDAY", 0),
                new Share("WEDNESDAY", 0), new Share("THURSDAY", 20), new Share("FRIDAY", 20),
                new Share("SATURDAY", 40));
    }

    @Test
    @DisplayName("요일 비율 — 선택 날짜 295개(< 300)면 블록 없음")
    void weekdayBelowThreshold() {
        assertThat(MeetupReportCalculator.calculate(weekdayFixture(59), List.of(), TODAY).weekdays()).isNull();
    }

    @Test
    @DisplayName("월별 — 08월 10개, 09월 0개, 10월 20개: 빈 달도 0 으로, 이번 달은 진행 중, 막대는 최댓값 대비")
    void monthlyCountsIncludeEmptyMonth() {
        List<LocalDateTime> created = new ArrayList<>();
        for (int i = 0; i < 10; i++) created.add(LocalDateTime.of(2026, 8, 5, 12, 0));
        for (int i = 0; i < 20; i++) created.add(LocalDateTime.of(2026, 10, 2, 12, 0));

        MeetupReportDto r = MeetupReportCalculator.calculate(List.of(), created, TODAY);

        assertThat(r.monthly().total()).isEqualTo(30);
        assertThat(r.monthly().months()).containsExactly(
                new MonthCount(2026, 8, 10, 50, false),
                new MonthCount(2026, 9, 0, 0, false),
                new MonthCount(2026, 10, 20, 100, true));
    }

    @Test
    @DisplayName("월별 — 최근 12개월만: 2025-01 일정이 있어도 2025-11 부터 2026-10 까지 12칸")
    void monthlyWindowIsTwelveMonths() {
        List<LocalDateTime> created = new ArrayList<>();
        created.add(LocalDateTime.of(2025, 1, 10, 9, 0));
        for (int i = 0; i < 29; i++) created.add(LocalDateTime.of(2026, 10, 3, 9, 0));

        MeetupReportDto r = MeetupReportCalculator.calculate(List.of(), created, TODAY);

        assertThat(r.monthly().months()).hasSize(12);
        assertThat(r.monthly().months().get(0)).isEqualTo(new MonthCount(2025, 11, 0, 0, false));
        assertThat(r.monthly().months().get(11)).isEqualTo(new MonthCount(2026, 10, 29, 100, true));
    }

    @Test
    @DisplayName("월별 — 일정 29개(< 30)면 블록 없음")
    void monthlyBelowThreshold() {
        List<LocalDateTime> created = new ArrayList<>();
        for (int i = 0; i < 29; i++) created.add(OCT_1);
        assertThat(MeetupReportCalculator.calculate(List.of(), created, TODAY).monthly()).isNull();
    }

    /** 일정 n 개, 각 2명이 같은 날(day) 하나를 고른다 → 최다 겹침일 = day. */
    private static void addSchedules(List<ParticipantAvailability> list, long fromId, int n, LocalDateTime created, int day) {
        for (long id = fromId; id < fromId + n; id++) {
            list.add(oct(id, created, day));
            list.add(oct(id, created, day));
        }
    }

    @Test
    @DisplayName("리드타임 — 2일×10, 9일×10, 19일×6, 30일×4: 구간 34/33/20/13(최대 잉여, 동률은 앞 구간), 아래쪽 중앙값 9")
    void leadTimeBucketsAndMedian() {
        List<ParticipantAvailability> list = new ArrayList<>();
        addSchedules(list, 0, 10, OCT_1, 3);    // 10-03 − 10-01 = 2
        addSchedules(list, 10, 10, OCT_1, 10);  // 9
        addSchedules(list, 20, 6, OCT_1, 20);   // 19
        addSchedules(list, 26, 4, OCT_1, 31);   // 30

        MeetupReportDto.LeadTimeBlock lead = MeetupReportCalculator.calculate(list, List.of(), TODAY).leadTime();

        assertThat(lead.sampleSize()).isEqualTo(30);
        assertThat(lead.medianDays()).isEqualTo(9);   // 정렬 후 인덱스 (30−1)/2 = 14
        assertThat(lead.buckets()).containsExactly(
                new Share("D0_6", 34), new Share("D7_13", 33), new Share("D14_27", 20), new Share("D28_PLUS", 13));
    }

    @Test
    @DisplayName("리드타임 — 최다 겹침이 동률이면 이른 날, 당일(0일)은 0~6일, 생성일 이전·1인·빈 선택 일정은 제외")
    void leadTimeTieAndExclusions() {
        List<ParticipantAvailability> list = new ArrayList<>();
        for (long id = 0; id < 30; id++) {
            list.add(oct(id, OCT_1, 1, 5));   // 1일·5일 동률 2명 → 이른 1일 → 0일
            list.add(oct(id, OCT_1, 5, 1));
        }
        LocalDateTime oct20 = LocalDateTime.of(2026, 10, 20, 9, 0);
        list.add(oct(100, oct20, 3));        // 03 − 20 = −17 → 제외
        list.add(oct(100, oct20, 3));
        list.add(oct(101, OCT_1, 3));        // 1명뿐 → 제외
        list.add(oct(102, OCT_1, 3));        // 한 명은 빈 선택 → 날짜를 고른 사람 1명 → 제외
        list.add(new ParticipantAvailability(102L, 2026, 10, 5, OCT_1, List.of()));

        MeetupReportDto.LeadTimeBlock lead = MeetupReportCalculator.calculate(list, List.of(), TODAY).leadTime();

        assertThat(lead.sampleSize()).isEqualTo(30);
        assertThat(lead.medianDays()).isZero();
        assertThat(lead.buckets()).containsExactly(
                new Share("D0_6", 100), new Share("D7_13", 0), new Share("D14_27", 0), new Share("D28_PLUS", 0));
    }

    @Test
    @DisplayName("리드타임 — 해당 일정 29개(< 30)면 블록 없음")
    void leadTimeBelowThreshold() {
        List<ParticipantAvailability> list = new ArrayList<>();
        addSchedules(list, 0, 29, OCT_1, 3);
        assertThat(MeetupReportCalculator.calculate(list, List.of(), TODAY).leadTime()).isNull();
    }

    @Test
    @DisplayName("기준일은 넘겨받은 오늘")
    void asOfIsToday() {
        assertThat(MeetupReportCalculator.calculate(List.of(), List.of(), TODAY).asOf()).isEqualTo(TODAY);
    }
}
```

- [ ] **Step 2: 실패 확인** — `--tests me.singingsandhill.calendar.datedate.application.service.MeetupReportCalculatorTest`. Expected: 컴파일 실패.

- [ ] **Step 3: DTO**

```java
package me.singingsandhill.calendar.datedate.application.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * /insights/trends 모임 데이터 리포트 (ADR datedate/domain/0013). 블록은 표본 하한 미달이면 null — 템플릿이 숨긴다.
 */
public record MeetupReportDto(LocalDate asOf, WeekdayBlock weekdays, MonthlyBlock monthly, LeadTimeBlock leadTime) {

    /** key 는 메시지 키 꼬리(요일은 DayOfWeek.name(), 리드타임은 D0_6 등), percent 는 정수 %(블록 합계 100). */
    public record Share(String key, int percent) {
    }

    public record WeekdayBlock(int sampleSize, List<Share> shares) {
    }

    public record MonthCount(int year, int month, int count, int barPercent, boolean inProgress) {
    }

    public record MonthlyBlock(int total, List<MonthCount> months) {
    }

    public record LeadTimeBlock(int sampleSize, int medianDays, List<Share> buckets) {
    }

    public boolean hasAnyBlock() {
        return weekdays != null || monthly != null || leadTime != null;
    }
}
```

- [ ] **Step 4: 계산 클래스**

```java
package me.singingsandhill.calendar.datedate.application.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.IntStream;

import me.singingsandhill.calendar.datedate.application.dto.MeetupReportDto;
import me.singingsandhill.calendar.datedate.application.dto.MeetupReportDto.LeadTimeBlock;
import me.singingsandhill.calendar.datedate.application.dto.MeetupReportDto.MonthCount;
import me.singingsandhill.calendar.datedate.application.dto.MeetupReportDto.MonthlyBlock;
import me.singingsandhill.calendar.datedate.application.dto.MeetupReportDto.Share;
import me.singingsandhill.calendar.datedate.application.dto.MeetupReportDto.WeekdayBlock;
import me.singingsandhill.calendar.datedate.domain.participant.ParticipantAvailability;

/**
 * 모임 데이터 리포트 계산 — 스프링 의존 없는 순수 계산 (ADR datedate/domain/0013).
 * 표본 하한 미달 블록은 null. 비율은 최대 잉여 반올림으로 합계 100.
 */
public final class MeetupReportCalculator {

    static final int WEEKDAY_MIN_DATES = 300;
    static final int MONTHLY_MIN_SCHEDULES = 30;
    static final int LEAD_TIME_MIN_SCHEDULES = 30;
    static final int MONTHS_SHOWN = 12;

    private static final List<DayOfWeek> WEEK_ORDER = List.of(
            DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY);
    private static final List<String> LEAD_BUCKETS = List.of("D0_6", "D7_13", "D14_27", "D28_PLUS");

    private MeetupReportCalculator() {
    }

    public static MeetupReportDto calculate(List<ParticipantAvailability> availabilities,
                                            List<LocalDateTime> scheduleCreatedAts,
                                            LocalDate today) {
        return new MeetupReportDto(today,
                weekdays(availabilities),
                monthly(scheduleCreatedAts, today),
                leadTime(availabilities));
    }

    private static WeekdayBlock weekdays(List<ParticipantAvailability> availabilities) {
        Map<DayOfWeek, Integer> counts = new EnumMap<>(DayOfWeek.class);
        int total = 0;
        for (ParticipantAvailability a : availabilities) {
            for (LocalDate date : a.availableDates()) {
                counts.merge(date.getDayOfWeek(), 1, Integer::sum);
                total++;
            }
        }
        if (total < WEEKDAY_MIN_DATES) {
            return null;
        }
        int[] percents = percentages(WEEK_ORDER.stream().mapToInt(d -> counts.getOrDefault(d, 0)).toArray());
        List<Share> shares = IntStream.range(0, WEEK_ORDER.size())
                .mapToObj(i -> new Share(WEEK_ORDER.get(i).name(), percents[i]))
                .toList();
        return new WeekdayBlock(total, shares);
    }

    private static MonthlyBlock monthly(List<LocalDateTime> createdAts, LocalDate today) {
        if (createdAts.size() < MONTHLY_MIN_SCHEDULES) {
            return null;
        }
        Map<java.time.YearMonth, Integer> counts = new HashMap<>();
        java.time.YearMonth first = null;
        for (LocalDateTime createdAt : createdAts) {
            java.time.YearMonth ym = java.time.YearMonth.from(createdAt);
            counts.merge(ym, 1, Integer::sum);
            if (first == null || ym.isBefore(first)) {
                first = ym;
            }
        }
        java.time.YearMonth current = java.time.YearMonth.from(today);
        java.time.YearMonth windowStart = current.minusMonths(MONTHS_SHOWN - 1);
        java.time.YearMonth start = first.isBefore(windowStart) ? windowStart : first;
        if (start.isAfter(current)) {
            start = current;
        }
        int max = 0;
        for (java.time.YearMonth ym = start; !ym.isAfter(current); ym = ym.plusMonths(1)) {
            max = Math.max(max, counts.getOrDefault(ym, 0));
        }
        List<MonthCount> months = new ArrayList<>();
        for (java.time.YearMonth ym = start; !ym.isAfter(current); ym = ym.plusMonths(1)) {
            int count = counts.getOrDefault(ym, 0);
            int bar = max == 0 ? 0 : count * 100 / max;
            months.add(new MonthCount(ym.getYear(), ym.getMonthValue(), count, bar, ym.equals(current)));
        }
        return new MonthlyBlock(createdAts.size(), months);
    }

    private static LeadTimeBlock leadTime(List<ParticipantAvailability> availabilities) {
        Map<Long, List<ParticipantAvailability>> bySchedule = new LinkedHashMap<>();
        for (ParticipantAvailability a : availabilities) {
            bySchedule.computeIfAbsent(a.scheduleId(), k -> new ArrayList<>()).add(a);
        }
        List<Integer> leads = new ArrayList<>();
        for (List<ParticipantAvailability> group : bySchedule.values()) {
            List<List<LocalDate>> picked = group.stream()
                    .map(ParticipantAvailability::availableDates)
                    .filter(dates -> !dates.isEmpty())
                    .toList();
            if (picked.size() < 2) {
                continue;
            }
            Map<LocalDate, Integer> overlap = new TreeMap<>();
            picked.forEach(dates -> dates.forEach(d -> overlap.merge(d, 1, Integer::sum)));
            LocalDate best = null;
            int bestCount = 0;
            for (Map.Entry<LocalDate, Integer> e : overlap.entrySet()) {   // 날짜 오름차순 → 동률이면 이른 날 유지
                if (e.getValue() > bestCount) {
                    best = e.getKey();
                    bestCount = e.getValue();
                }
            }
            long lead = ChronoUnit.DAYS.between(group.get(0).scheduleCreatedAt().toLocalDate(), best);
            if (lead >= 0) {
                leads.add((int) lead);
            }
        }
        if (leads.size() < LEAD_TIME_MIN_SCHEDULES) {
            return null;
        }
        int[] buckets = new int[LEAD_BUCKETS.size()];
        for (int lead : leads) {
            buckets[lead <= 6 ? 0 : lead <= 13 ? 1 : lead <= 27 ? 2 : 3]++;
        }
        int[] percents = percentages(buckets);
        List<Share> shares = IntStream.range(0, LEAD_BUCKETS.size())
                .mapToObj(i -> new Share(LEAD_BUCKETS.get(i), percents[i]))
                .toList();
        List<Integer> sorted = leads.stream().sorted().toList();
        int median = sorted.get((sorted.size() - 1) / 2);   // 짝수 개면 아래쪽 중앙값
        return new LeadTimeBlock(leads.size(), median, shares);
    }

    /** 최대 잉여 반올림 — 정수 % 합계가 정확히 100. 잉여가 같으면 앞 항목부터. */
    static int[] percentages(int[] counts) {
        int total = IntStream.of(counts).sum();
        int[] result = new int[counts.length];
        if (total == 0) {
            return result;
        }
        long[] remainders = new long[counts.length];
        int assigned = 0;
        for (int i = 0; i < counts.length; i++) {
            long scaled = (long) counts[i] * 100;
            result[i] = (int) (scaled / total);
            remainders[i] = scaled % total;
            assigned += result[i];
        }
        List<Integer> order = IntStream.range(0, counts.length).boxed()
                .sorted(Comparator.comparingLong((Integer i) -> remainders[i]).reversed()
                        .thenComparingInt(i -> i))
                .toList();
        for (int k = 0; k < 100 - assigned; k++) {
            result[order.get(k)]++;
        }
        return result;
    }
}
```

- [ ] **Step 5: 통과 확인** — Step 2 명령. Expected: 9 tests PASS.

### Task 11: 서비스·컨트롤러·템플릿·문구·CSS·SEO 제목

**Files:**
- Modify: `src/main/java/me/singingsandhill/calendar/datedate/application/service/InsightsService.java`
- Modify: `src/main/java/me/singingsandhill/calendar/datedate/presentation/controller/InsightsController.java`
- Modify: `src/main/resources/templates/insights/trends.html`
- Modify: `messages.properties`, `messages_en.properties`, `src/main/resources/static/css/style.css`
- Create: `src/test/java/me/singingsandhill/calendar/datedate/presentation/controller/InsightsMeetupReportRenderingTest.java`

**Interfaces:**
- Consumes: `MeetupReportCalculator.calculate` (Task 10), 포트 2개 (Task 9)
- Produces: `MeetupReportDto InsightsService.getMeetupReport(LocalDate today)`, 모델 속성 `report`

- [ ] **Step 1: 실패하는 렌더 테스트** — 서비스를 목으로 바꿔 공유 테스트 DB 의 다른 데이터와 무관하게 표시·숨김을 확인한다.

```java
package me.singingsandhill.calendar.datedate.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.HtmlUtils;

import me.singingsandhill.calendar.datedate.application.dto.MeetupReportDto;
import me.singingsandhill.calendar.datedate.application.dto.MeetupReportDto.LeadTimeBlock;
import me.singingsandhill.calendar.datedate.application.dto.MeetupReportDto.MonthCount;
import me.singingsandhill.calendar.datedate.application.dto.MeetupReportDto.MonthlyBlock;
import me.singingsandhill.calendar.datedate.application.dto.MeetupReportDto.Share;
import me.singingsandhill.calendar.datedate.application.dto.MeetupReportDto.WeekdayBlock;
import me.singingsandhill.calendar.datedate.application.dto.ServiceStatsDto;
import me.singingsandhill.calendar.datedate.application.service.InsightsService;

/** /insights/trends 모임 데이터 섹션 — 블록이 있으면 그리고 null 이면 숨긴다 (ADR datedate/domain/0013). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InsightsMeetupReportRenderingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InsightsService insightsService;

    private static final MeetupReportDto FULL = new MeetupReportDto(LocalDate.of(2026, 10, 14),
            new WeekdayBlock(300, List.of(new Share("SUNDAY", 20), new Share("MONDAY", 0), new Share("TUESDAY", 0),
                    new Share("WEDNESDAY", 0), new Share("THURSDAY", 20), new Share("FRIDAY", 20), new Share("SATURDAY", 40))),
            new MonthlyBlock(30, List.of(new MonthCount(2026, 9, 10, 50, false), new MonthCount(2026, 10, 20, 100, true))),
            new LeadTimeBlock(30, 9, List.of(new Share("D0_6", 34), new Share("D7_13", 33),
                    new Share("D14_27", 20), new Share("D28_PLUS", 13))));

    @BeforeEach
    void stats() {
        when(insightsService.getServiceStats()).thenReturn(new ServiceStatsDto(0, 0, 0, 0, 0, 0, 0, 0, 0));
    }

    @Test
    @DisplayName("ko — 세 블록과 기준일·정의가 그려진다")
    void koreanBlocks() throws Exception {
        when(insightsService.getMeetupReport(any())).thenReturn(FULL);
        String text = visibleText(render("ko"));
        assertThat(text).contains("기준: 2026-10-14").contains("요일별 가능 비율").contains("토요일 40%")
                .contains("월별 일정 수").contains("2026년 10월").contains("진행 중")
                .contains("미리 잡는 기간").contains("중앙값 9일").contains("6일 이내 34%");
    }

    @Test
    @DisplayName("en — 영어로 그려진다")
    void englishBlocks() throws Exception {
        when(insightsService.getMeetupReport(any())).thenReturn(FULL);
        String text = visibleText(render("en"));
        assertThat(text).contains("As of 2026-10-14").contains("Availability by day of week").contains("Saturday 40%")
                .contains("Schedules per month").contains("in progress").contains("Median 9 days");
    }

    @Test
    @DisplayName("블록이 모두 null 이면 모임 데이터 섹션이 없다")
    void hiddenWhenNoBlocks() throws Exception {
        when(insightsService.getMeetupReport(any())).thenReturn(new MeetupReportDto(LocalDate.of(2026, 10, 14), null, null, null));
        String text = visibleText(render("ko"));
        assertThat(text).doesNotContain("요일별 가능 비율").doesNotContain("기준: 2026-10-14");
    }

    private String render(String lang) throws Exception {
        return mockMvc.perform(get("/insights/trends").param("lang", lang))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private static String visibleText(String html) {
        int start = html.indexOf("<body");
        int end = html.indexOf("</body>");
        String body = (start >= 0 && end > start) ? html.substring(start, end) : html;
        body = body.replaceAll("(?is)<(script|style|noscript)[^>]*>.*?</\\1>", " ");
        return HtmlUtils.htmlUnescape(body.replaceAll("(?s)<[^>]+>", " ")).replaceAll("\\s+", " ");
    }
}
```
("토요일 40%" 처럼 라벨과 값이 이어 보이는 것은 아래 막대 목록 마크업 기준 — 라벨·트랙·값이 공백 하나로 이어진다.)

- [ ] **Step 2: 실패 확인** — `--tests …InsightsMeetupReportRenderingTest`. Expected: 컴파일 실패(`getMeetupReport` 없음).

- [ ] **Step 3: 서비스·컨트롤러**

```java
    // InsightsService  (import java.time.LocalDate; MeetupReportDto)
    /** 모임 데이터 리포트 — 렌더 시점 집계 (ADR datedate/domain/0013). */
    public MeetupReportDto getMeetupReport(LocalDate today) {
        return MeetupReportCalculator.calculate(
                participantRepository.findAllAvailability(),
                scheduleRepository.findAllCreatedAt(),
                today);
    }
```
```java
    // InsightsController  (import java.time.LocalDate; java.time.ZoneId)
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    // trends() 안, model.addAttribute("stats", stats); 다음:
        model.addAttribute("report", insightsService.getMeetupReport(LocalDate.now(KST)));
```

- [ ] **Step 4: 템플릿 — `trends.html` 의 `<!-- 이용 현황 섹션 -->` 바로 앞에 삽입**

```html
    <!-- 모임 데이터 리포트 (ADR datedate/domain/0013) — 블록은 표본 하한 미달이면 null 이라 그리지 않는다 -->
    <section class="report-section" th:if="${report.hasAnyBlock()}" aria-labelledby="report-heading">
        <h2 id="report-heading" class="animate-on-scroll" th:text="#{insights.report.title}">모임 데이터</h2>
        <p class="report-as-of" th:text="#{insights.report.asOf(${report.asOf().toString()})}">기준: 2026-10-14</p>
        <p class="report-method" th:text="#{insights.report.method}">집계 방법</p>

        <article class="report-block" th:if="${report.weekdays() != null}">
            <h3 th:text="#{insights.report.weekday.title}">요일별 가능 비율</h3>
            <p class="report-block-desc" th:text="#{insights.report.weekday.desc(${report.weekdays().sampleSize()})}">설명</p>
            <ul class="report-bars">
                <li th:each="s : ${report.weekdays().shares()}">
                    <span class="report-bar-label" th:text="#{'insights.report.weekday.' + ${s.key()}}">일요일</span>
                    <span class="report-bar-track" aria-hidden="true"><span class="report-bar-fill" th:style="'width:' + ${s.percent()} + '%'"></span></span>
                    <span class="report-bar-value" th:text="${s.percent()} + '%'">20%</span>
                </li>
            </ul>
        </article>

        <article class="report-block" th:if="${report.monthly() != null}">
            <h3 th:text="#{insights.report.monthly.title}">월별 일정 수</h3>
            <p class="report-block-desc" th:text="#{insights.report.monthly.desc(${report.monthly().total()})}">설명</p>
            <ul class="report-bars">
                <li th:each="m : ${report.monthly().months()}">
                    <span class="report-bar-label">
                        <span th:text="#{insights.report.monthly.label(${m.year()}, ${m.month()})}">2026년 10월</span>
                        <small th:if="${m.inProgress()}" th:text="#{insights.report.monthly.inProgress}">진행 중</small>
                    </span>
                    <span class="report-bar-track" aria-hidden="true"><span class="report-bar-fill" th:style="'width:' + ${m.barPercent()} + '%'"></span></span>
                    <span class="report-bar-value" th:text="${m.count()}">20</span>
                </li>
            </ul>
        </article>

        <article class="report-block" th:if="${report.leadTime() != null}">
            <h3 th:text="#{insights.report.lead.title}">미리 잡는 기간</h3>
            <p class="report-block-desc" th:text="#{insights.report.lead.desc(${report.leadTime().sampleSize()})}">설명</p>
            <p class="report-median" th:text="#{insights.report.lead.median(${report.leadTime().medianDays()})}">중앙값 9일</p>
            <ul class="report-bars">
                <li th:each="b : ${report.leadTime().buckets()}">
                    <span class="report-bar-label" th:text="#{'insights.report.lead.' + ${b.key()}}">6일 이내</span>
                    <span class="report-bar-track" aria-hidden="true"><span class="report-bar-fill" th:style="'width:' + ${b.percent()} + '%'"></span></span>
                    <span class="report-bar-value" th:text="${b.percent()} + '%'">34%</span>
                </li>
            </ul>
        </article>
    </section>
```
(막대는 `aria-hidden`, 라벨과 값은 텍스트로 남아 스크린리더가 "토요일 40%" 로 읽는다 — 별도 표 없이 같은 정보.)

- [ ] **Step 5: 문구 추가** — ko(`add_props.py`, 앵커 `insights.cta.guide`):

```
insights.report.title=모임 데이터
insights.report.asOf=기준: {0}
insights.report.method=아래 수치는 DateDate 에서 만들어진 일정과 참여자가 고른 날짜를 그대로 센 결과입니다. 이름이나 페이지 ID 없이 합계만 보여주며, 표본이 적은 항목은 숨깁니다.
insights.report.weekday.title=요일별 가능 비율
insights.report.weekday.desc=참여자가 가능하다고 고른 날짜 {0,number,#}개를 요일별로 나눈 비율입니다.
insights.report.weekday.SUNDAY=일요일
insights.report.weekday.MONDAY=월요일
insights.report.weekday.TUESDAY=화요일
insights.report.weekday.WEDNESDAY=수요일
insights.report.weekday.THURSDAY=목요일
insights.report.weekday.FRIDAY=금요일
insights.report.weekday.SATURDAY=토요일
insights.report.monthly.title=월별 일정 수
insights.report.monthly.desc=일정이 만들어진 달을 기준으로 센 개수입니다. 전체 {0,number,#}개.
insights.report.monthly.label={0,number,#}년 {1,number,#}월
insights.report.monthly.inProgress=진행 중
insights.report.lead.title=미리 잡는 기간
insights.report.lead.desc=두 명 이상이 날짜를 고른 일정 {0,number,#}개에서, 가장 많이 겹친 날이 일정을 만든 날로부터 며칠 뒤였는지 센 분포입니다.
insights.report.lead.median=중앙값 {0,number,#}일
insights.report.lead.D0_6=6일 이내
insights.report.lead.D7_13=7~13일
insights.report.lead.D14_27=14~27일
insights.report.lead.D28_PLUS=28일 이상
```
en(앵커 `insights.cta.guide`):
```
insights.report.title=Meetup data
insights.report.asOf=As of {0}
insights.report.method=These figures count the schedules created on DateDate and the dates participants picked, as recorded. Only totals are shown, never names or page IDs, and items with small samples are hidden.
insights.report.weekday.title=Availability by day of week
insights.report.weekday.desc=Share of the {0,number,#} dates participants marked as available, by day of week.
insights.report.weekday.SUNDAY=Sunday
insights.report.weekday.MONDAY=Monday
insights.report.weekday.TUESDAY=Tuesday
insights.report.weekday.WEDNESDAY=Wednesday
insights.report.weekday.THURSDAY=Thursday
insights.report.weekday.FRIDAY=Friday
insights.report.weekday.SATURDAY=Saturday
insights.report.monthly.title=Schedules per month
insights.report.monthly.desc=Counted by the month each schedule was created. {0,number,#} in total.
insights.report.monthly.label={1,number,#}/{0,number,#}
insights.report.monthly.inProgress=in progress
insights.report.lead.title=How far ahead groups plan
insights.report.lead.desc=For {0,number,#} schedules where two or more people picked dates: days from creating the schedule to its most-overlapping date.
insights.report.lead.median=Median {0,number,#} days
insights.report.lead.D0_6=Within 6 days
insights.report.lead.D7_13=7–13 days
insights.report.lead.D14_27=14–27 days
insights.report.lead.D28_PLUS=28+ days
```

SEO·제목 교체(`set_props.py`) ko:
```json
{
  "insights.hero.title": [null, "모임 데이터 & 인기 트렌드"],
  "insights.breadcrumb.current": [null, "모임 데이터 & 인기 트렌드"],
  "seo.insights.title": [null, "모임 데이터 & 인기 트렌드 | DateDate"],
  "seo.insights.ogTitle": [null, "모임 데이터 & 인기 트렌드 | DateDate"],
  "seo.insights.webPageName": [null, "모임 데이터 & 인기 트렌드 - DateDate"],
  "seo.insights.description": [null, "DateDate 사용자들이 고른 날짜로 본 요일별 가능 비율, 월별 일정 수, 미리 잡는 기간과 인기 약속 장소·메뉴 TOP 10을 한 페이지에서 확인하세요. 이름 없이 합계만 보여주는 실사용 데이터입니다."]
}
```
en:
```json
{
  "insights.hero.title": [null, "Meetup Data & Popular Trends"],
  "insights.breadcrumb.current": [null, "Meetup Data & Popular Trends"],
  "seo.insights.title": [null, "Meetup Data & Popular Trends | DateDate"],
  "seo.insights.ogTitle": [null, "Meetup Data & Popular Trends | DateDate"],
  "seo.insights.webPageName": [null, "Meetup Data & Popular Trends - DateDate"],
  "seo.insights.description": [null, "Availability by day of week, schedules per month, and how far ahead groups plan, from real DateDate usage, plus the top 10 venues and menus. Totals only, no names."]
}
```

- [ ] **Step 6: CSS** — `css_insert.py … '^\.insights-info p \{' $S/report.css` 후 numstat 비교.

```css
/* 모임 데이터 리포트 (/insights/trends, ADR datedate/domain/0013) */
.report-section {
    max-width: 700px;
    margin: 0 auto 3rem;
}

.report-section h2 {
    text-align: center;
    margin-bottom: 0.5rem;
}

.report-as-of {
    text-align: center;
    color: var(--text-light);
    font-size: 0.9rem;
    margin-bottom: 1rem;
}

.report-method,
.report-block-desc {
    color: var(--text-light);
    font-size: 0.95rem;
    line-height: 1.7;
    margin-bottom: 1rem;
}

.report-block {
    margin-bottom: 2.5rem;
}

.report-block h3 {
    font-size: 1.1rem;
    margin-bottom: 0.5rem;
}

.report-median {
    font-weight: 600;
    margin-bottom: 0.75rem;
}

.report-bars {
    list-style: none;
    padding: 0;
    margin: 0;
}

.report-bars li {
    display: grid;
    grid-template-columns: 6.5rem 1fr 3.5rem;
    align-items: center;
    gap: 0.75rem;
    margin-bottom: 0.5rem;
    font-size: 0.9rem;
}

.report-bar-track {
    height: 0.75rem;
    background: var(--bg-light);
    border-radius: 999px;
    overflow: hidden;
}

.report-bar-fill {
    display: block;
    height: 100%;
    background: var(--primary-color);
    border-radius: 999px;
}

.report-bar-value {
    text-align: right;
    font-variant-numeric: tabular-nums;
}
```

- [ ] **Step 7: 통과 확인** — `--tests …InsightsMeetupReportRenderingTest --tests …InsightsPartialDataIndexingTest --tests …SerpTitleBudgetTest --tests …SeoServiceI18nTest --tests …InsightsServiceTest --tests …HeadSeoSmokeTest`. Expected: 전부 PASS(ko 제목 26자 ≤ 35).

### Task 12: ADR datedate/domain/0013 + 문서 + 전체 검증 + 화면 확인 + 커밋 로그

- [ ] **Step 1: ADR 작성** `docs/adr/datedate/domain/0013-meetup-data-report.md`

```markdown
# ADR-0013: 모임 데이터 리포트 — 렌더 시 집계, 표본 하한, 집계만 공개

| 항목 | 값 |
|---|---|
| 상태 | Accepted |
| 날짜 | 2026-10-14 |
| 도메인 | datedate |
| 관심사 | 도메인 모델 |
| 관련 커밋 | `docs/guides/git-commit.md` Commit 201 |

## Context — 무엇이 문제였나

- AdSense "가치가 별로 없는 콘텐츠" 4번째 통지(2026-10-12). 색인 콘텐츠는 남들이 이미 쓴 일반론이었고, 사이트만 가진 실사용 데이터(일정 2,171건)는 인기 장소·메뉴 표뿐이었다.
- 참여자 선택은 인덱스로 저장되고 모드에 따라 해석이 다르다 — 7주 확장 모드는 그리드 시작 + (인덱스 − 1), 기존 모드는 그 달의 날짜. `RecapService` 는 모드 구분 없이 확장 규칙을 써서 기존 모드 일정의 요일이 어긋난다(이 ADR 범위 밖).

## Decision — 무엇을 골랐나

`/insights/trends` 에 요일별 가능 비율·월별 일정 수·미리 잡는 기간 세 블록을 렌더 시점에 집계해 보여준다.

- 읽기: 참여자+일정 `JOIN FETCH` 1회, 일정 생성 시각 1회. 계산은 스프링 의존 없는 `MeetupReportCalculator`.
- 날짜 변환: `YearMonth.dateOf(index, weeks)` — 웹 달력의 두 렌더 경로와 같은 규칙.
- 표본 하한(요일 300 날짜 / 월별 30 일정 / 리드타임 30 일정) 미달 블록은 숨긴다. 월별은 최근 12개월.
- 비율은 최대 잉여 반올림(합계 100). 리드타임 = 최다 겹침일(동점이면 이른 날) − 생성일, 음수 제외, 아래쪽 중앙값.
- 공개 범위는 합계뿐 — 이름·페이지 ID·자유 입력 문구 없음. 캐시·설정 키·JS 없음.

## Rationale — 왜 이 선택인가

| 대안 | 판정 |
|---|---|
| 일 단위 스냅샷 테이블 + 스케줄러 | 현 규모(수천 행, 하루 몇 회 조회)에서 과설계 |
| 기존 포트로 일정마다 참여자 지연 로딩 | 일정 수만큼 쿼리(2,171+) |
| 월간 스냅샷 URL + 해설 글 | 매월 직접 써야 하고 멈추면 방치 신호 — 사용자가 라이브 페이지를 택함 |
| **렌더 시 집계 (선택)** | 항상 최신, 새 테이블·스케줄러 없음 |

## Consequences — 영향

- **긍정:** 다른 사이트가 쓸 수 없는 실측 콘텐츠가 매일 저절로 갱신된다. 회귀 가드 `MeetupReportCalculatorTest`·`YearMonthTest`·`ParticipantAvailabilityTest`·`MeetupReportQueryTest`·`InsightsMeetupReportRenderingTest`.
- **부정:** 조회마다 전체 참여자를 읽는다. 조회량이 늘면 캐시를 다시 검토한다.
- **후속:** `RecapService` 를 `YearMonth.dateOf` 로 고치는 일은 별도.
```

- [ ] **Step 2: README** — 매트릭스 `datedate` 도메인 모델 12→13·합계 20→21, `합계` 행 도메인 모델 14→15·총 100→101, 상단 "총 101개", 시간순 표 끝 행:

```
| 2026-10-14 | datedate/domain | [0013 모임 데이터 리포트 — 렌더 시 집계·표본 하한·집계만 공개](datedate/domain/0013-meetup-data-report.md) | AdSense 4번째 통지 — 사이트만 가진 실사용 데이터가 인기 장소·메뉴 표뿐 |
```
폴더 목록 `datedate/domain/` 12 → 13.

- [ ] **Step 3: CLAUDE.md** — 루트 DateDate Module 첫 줄 `InsightsService` + `InsightsController` 항목 끝에 "+ 모임 데이터 리포트(요일별 가능 비율·월별 일정 수·리드타임, 렌더 시 집계, 표본 하한 미달 블록 숨김 — [ADR datedate/domain/0013](docs/adr/datedate/domain/0013-meetup-data-report.md))". `datedate/application/CLAUDE.md` 의 InsightsService 줄도 같은 내용.

- [ ] **Step 4: 전체 스위트** — Expected: failures 0. 136/881 → 새 클래스 4개(ParticipantAvailabilityTest 2, MeetupReportQueryTest 2, MeetupReportCalculatorTest 9, InsightsMeetupReportRenderingTest 3) + YearMonthTest 2 = +18 → 140 / 899.

- [ ] **Step 5: 화면 확인** — 하네스로 `/insights/trends` ko 375·1280px: 임시 프로브가 `InsightsMeetupReportRenderingTest` 의 FULL 과 같은 DTO 를 목으로 넣은 렌더 HTML 을 덤프 → 캡처 → 막대·라벨·가로 넘침 확인 → 프로브 삭제.

- [ ] **Step 6: 커밋 로그 섹션 (Commit 201)** — `git add`:

```
git add src/main/java/me/singingsandhill/calendar/datedate/domain/schedule/YearMonth.java src/main/java/me/singingsandhill/calendar/datedate/domain/participant/ParticipantAvailability.java src/main/java/me/singingsandhill/calendar/datedate/domain/participant/ParticipantRepository.java src/main/java/me/singingsandhill/calendar/datedate/domain/schedule/ScheduleRepository.java src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/repository/ParticipantJpaRepository.java src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/repository/ScheduleJpaRepository.java src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/adapter/ParticipantRepositoryAdapter.java src/main/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/adapter/ScheduleRepositoryAdapter.java src/main/java/me/singingsandhill/calendar/datedate/application/dto/MeetupReportDto.java src/main/java/me/singingsandhill/calendar/datedate/application/service/MeetupReportCalculator.java src/main/java/me/singingsandhill/calendar/datedate/application/service/InsightsService.java src/main/java/me/singingsandhill/calendar/datedate/presentation/controller/InsightsController.java src/main/resources/templates/insights/trends.html src/main/resources/messages.properties src/main/resources/messages_en.properties src/main/resources/static/css/style.css src/test/java/me/singingsandhill/calendar/datedate/domain/schedule/YearMonthTest.java src/test/java/me/singingsandhill/calendar/datedate/domain/participant/ParticipantAvailabilityTest.java src/test/java/me/singingsandhill/calendar/datedate/infrastructure/persistence/adapter/MeetupReportQueryTest.java src/test/java/me/singingsandhill/calendar/datedate/application/service/MeetupReportCalculatorTest.java src/test/java/me/singingsandhill/calendar/datedate/presentation/controller/InsightsMeetupReportRenderingTest.java docs/adr/datedate/domain/0013-meetup-data-report.md docs/adr/README.md CLAUDE.md src/main/java/me/singingsandhill/calendar/datedate/application/CLAUDE.md docs/guides/git-commit.md
```

## 배포 후 (사용자 배포 → 에이전트 라이브 확인)

- 파트마다 배포(평일 09:15~11:25 제외) 후: `/about`·`/terms`(광고 문구 없음·GitHub 링크), `/guide`(6단계·이미지 12장 200·`image/webp`), `/insights/trends`(블록 표시 여부 — 운영 데이터가 하한을 넘는지), 사이트맵 전 URL 구조화 데이터 가시성(①의 스크립트) 재실측.
- 사용자: GSC 에서 `/about`·`/guide`·`/insights/trends`(ko·en) 색인 요청. 마지막 파트 배포 후 8주 이상 지나 AdSense 재검토 1회.
