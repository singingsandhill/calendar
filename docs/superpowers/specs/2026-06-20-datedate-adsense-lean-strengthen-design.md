# DateDate — AdSense "Low Value Content" Remediation (Lean Strengthen) — Design

- **Date:** 2026-06-20
- **Status:** Draft (awaiting user review)
- **Module:** `datedate` (+ small `common` touch)
- **Goal:** Clear Google AdSense **"가치가 별로 없는 콘텐츠 (Low value content)"** on `datedate.site` (flagged 2026-06-11) by strengthening the editorial content layer — without re-architecting anything.

---

## 1. Problem & Diagnosis (summary)

A multi-agent audit + current-guidance research (2026) concluded:

- **Technical SEO is already correct.** Zero critical indexation bugs: robots/canonical/hreflang/sitemap are right, UGC (owner dashboard, schedule view) is `noindex,nofollow`, `ads.txt` present, JSON-LD on every page, and the **Privacy Policy already carries the required AdSense third-party-cookie / personalized-ads / opt-out disclosure** (KO+EN). There is **no technical toggle** that fixes this.
- **The flag is editorial.** From a reviewer's seat the site is a *mostly-UI utility app with a thin original-content footprint*: ~10 indexable pages, of which the genuinely editorial set is **4 use-case pages — all rendered through one template** (`use-cases/detail.html`). The prose inside is good and distinct, but "4 good pages on 1 template" trips both **"not enough original content"** and **"templated pages replicated within the same site."**
- **Channel correction:** This is an **AdSense Policy Center** issue, *not* a Search Console Manual Action. Re-review is requested via **AdSense → Policy Center → Fix → Start review process**. Do **not** file a Search Console reconsideration request.
- **Myths ignored:** no official post-count, word-count, traffic, or site-age threshold; FAQ schema is not a quality lever.

### Corrections found while scoping (change the plan vs. the raw audit)

1. **Home is *not* pure UI.** `index.html` already has stats, popular-picks, a **features** section (3 features), a **scenarios** section, and a 5-Q **FAQ** with real answers (~57 home keys + 25 home-FAQ keys). → We *deepen* home, not build it from scratch.
2. **`study-group` home scenario card is commented out** (`index.html` ~lines 148–154) even though `/use-cases/study-group` exists and is in the footer. → Re-enable for nav/consistency (keys `index.scenario.study.*` already exist).
3. **Footer hardcodes 4 use-case links** (`footer.html` 53–56). → Make data-driven so new pages aren't orphaned.

---

## 2. Scope

**In scope (Lean Strengthen first):**
1. Differentiate the existing 4 use-case pages so they look distinct, not slot-filled.
2. Add **1** new genuinely-distinct use-case: **club-activity (동호회 / 취미모임)**.
3. Data-driven footer nav + re-enable study-group home scenario.
4. Deepen the home page's existing sections with a "why a poll beats a group chat / how it works / limitations" block.
5. Internal linking + a pre-reapply verification pass.

**Out of scope (deferred — the "Full content layer" option):** a separate `/guides` editorial hub with its own `ContentController`/templates; `/tools` family expansion; insights-as-prose. These remain available as a follow-up if the lean pass doesn't clear the flag.

**Non-goals:** any change to the link-as-password security model, the GET-never-mutates ADRs, the day-level binary availability model, or AdSense placement strategy. No padding to hit word counts.

---

## 3. Design

Hexagonal layering is preserved. Most work is **content** (i18n `.properties`) + **template** changes; one tiny `common` presentation addition.

### 3.1 Component A — Differentiate the 4 use-case pages  *(effort: M)*

**Problem:** `use-cases/detail.html` renders identical section order for every slug; `SeoService.getUseCaseSeo` gives every slug the same `DEFAULT_OG_IMAGE`. Visual cookie-cutter.

**Design:**
- Add a per-slug **"worked example"** block in `detail.html`, switched on the already-present `${currentSlug}` (`th:switch`/`th:if`), rendered *after* `.use-case-intro` and before `.use-case-when`. Each slug shows something the others don't:
  - `friend-meetup`: a small rendered availability-grid snippet (static HTML illustration; no live data).
  - `team-meeting`: a recurring monthly example (`/team/2026/02` style).
  - `travel-planning`: a multi-day range illustration.
  - `study-group`: a fixed weekly-slot illustration.
  - `club-activity`: a roster + recurring-attendance illustration.
- The block is an optional fragment per slug: `fragments/usecase-sample.html` with a `th:fragment="sample(slug)"` that internally `th:switch`es — keeps `detail.html` clean and the seam reusable.
- **Per-slug OG image:** extend `SeoService.getUseCaseSeo(slug)` to select an OG image by slug (`/og/use-case-{slug}.svg` or a templated SVG endpoint reusing the `StaticResourceController` SVG pattern). If producing 5 SVGs is too heavy for this pass, fall back to a single shared image but **keep the per-slug seam** (method already takes `slug`). OG differentiation is **nice-to-have**, not a blocker for re-review.
- Optional light section reordering per slug (e.g., tips-before-mistakes for some) to reduce structural identicality. Keep it subtle; correctness > novelty.

**Acceptance:** Each `/use-cases/{slug}` page shows at least one slug-unique content block that no other slug renders; pages are visibly distinguishable in a side-by-side skim.

### 3.2 Component B — New use-case: `club-activity` (동호회 / 취미모임)  *(effort: M)*

**Seam:** add `"club-activity"` to `UseCaseSlugs.ALL` (`domain/usecase/UseCaseSlugs.java`). This auto-wires routing (`UseCaseController`), sitemap (`SitemapService` iterates the list), and — after Component C — footer/nav.

**Content to author** — mirror the full `friend-meetup` key set, in **both** `messages.properties` and `messages_en.properties`. Required keys (per the verified `friend-meetup` block at lines 429–439, 538–577):

| Key family | Keys |
|---|---|
| Head | `seo.useCase.club-activity.title`, `.description`, `.body`, `.exampleLabel`, `.exampleId` |
| HowTo steps | `.step1.name/.text`, `.step2.name/.text`, `.step3.name/.text` |
| Intro | `.section.intro` |
| When to use | `.section.whenToUse.title/.lead`, `.scenario{1,2,3}.title/.text` |
| Common mistakes | `.section.mistakes.title/.lead`, `.mistake{1,2,3}.problem/.fix` |
| Tips | `.section.tips.title`, `.tip{1,2,3,4}.title/.text` |
| FAQ | `.section.faq.q{1..5}`, `.a{1..5}` |
| Nav label (new, see C) | `.navLabel` |

**Content distinctiveness requirements** (so it doesn't read as a friend-meetup rephrase):
- Center on **recurring attendance for a standing group with a larger, churning roster** (동호회): regular weekly/monthly sessions, members who join/leave, "who's coming this time" rather than "what day works once."
- Example ID: something like `tennis-club` / `book-club`; exampleLabel "동호회 정기모임".
- Scenarios: (1) 매주 정기 운동/연습 출석 집계, (2) 분기 번개·외부 행사 날짜 조율, (3) 신규 회원 합류 시 일정 공유.
- Mistakes specific to clubs: relying on a 단톡 공지 nobody reads; no attendance visibility; organizer carrying it alone.
- Tips: reuse a page monthly via `/club/2026/03`; pre-seed the roster; pair date + 장소 vote for venue-dependent hobbies.
- FAQ: roster > 8 (split core vs. casual), recurring reuse, link leakage for open clubs, no-shows, carrying members month to month.

**Acceptance:** `/use-cases/club-activity` (KO default and `?lang=en`) renders all sections fully, with content materially different from the other four; appears in `sitemap.xml` (both locales) and in footer nav.

### 3.3 Component C — Data-driven nav + re-enable study-group  *(effort: S)*

**Design:**
- New `@ControllerAdvice` `UseCaseNavAdvice` in `common` (or `datedate`) `presentation`, mirroring the existing `AdsenseModelAdvice` (`@ModelAttribute("adsense")`) pattern:
  ```java
  @ControllerAdvice
  class UseCaseNavAdvice {
      @ModelAttribute("useCaseSlugs")
      List<String> useCaseSlugs() { return UseCaseSlugs.ALL; }
  }
  ```
  *(Placement note: `AdsenseModelAdvice` lives in `common`. `UseCaseSlugs` lives in `datedate.domain`. Put the advice in `datedate.presentation` to avoid `common → datedate` coupling; advices apply globally regardless of package.)*
- `footer.html` lines 53–56 → `th:each="slug : ${useCaseSlugs}"` rendering `${@localeLinks.href('/use-cases/' + slug)}` with label `#{'seo.useCase.' + slug + '.navLabel'}`.
- Add `seo.useCase.<slug>.navLabel` (short label) for **all 5 slugs** (KO+EN). Retire the now-unused `footer.usecase.*` keys (or leave them; harmless).
- Re-enable the `study-group` scenario card in `index.html` (uncomment ~148–154); keys `index.scenario.study.*` already exist. (Home scenarios stay a curated set; do not necessarily add a club-activity card unless layout allows — decide visually.)

**Acceptance:** Footer shows a link per slug in `UseCaseSlugs.ALL` (5 after Component B); adding/removing a slug changes the footer with no template edit; study-group card visible on home.

### 3.4 Component D — Deepen the home page  *(effort: S/M)*

**Design:** Add one substantial original prose section to `index.html` beneath `#start-form` (and before the first ad slot), as new `index.*` message keys (KO+EN). Cover, in genuine prose (no padding):
- **Why a shared availability poll beats a group chat** (the repeated "언제 돼?" failure mode).
- **How it works** end-to-end in 3–4 sentences referencing the real flow (create → share link → click days → consensus highlight → vote 장소/메뉴).
- **Limitations / honesty:** day-level (not time-of-day) availability, ~8-participant guidance, no accounts, link = access.

This converts the most-crawled URL from "form + cards" into "form + cards + real editorial substance" and adds an honest E-E-A-T signal.

**Acceptance:** Home renders a new multi-paragraph KO section with original copy; English equivalent under `?lang=en`.

### 3.5 Component E — Internal linking + verification  *(effort: S)*

- **Linking:** extend the existing `.use-case-others` related section in `detail.html` to also surface 1–2 topically-related pages (already iterates `allSlugs`); ensure home → `/insights/trends` link stays; add home → a use-case or two contextually. Goal: a connected graph, not orphaned leaves.
- **Pre-reapply verification (no code):**
  1. `curl -s https://datedate.site/use-cases/club-activity` (and each indexable URL) **without** `?lang=en` → confirm substantial **Korean** body text (reviewer crawls default locale; catch any English fallback / thin render).
  2. Validate JSON-LD on `index,follow` pages via Google Rich Results Test (SeoService hand-builds JSON strings).
  3. Confirm `/privacy` is linked in footer (it is) and live; `/ads.txt` + IndexNow key file served.
  4. Search Console **URL Inspection → Request Indexing** on new/changed pages; confirm re-crawl **before** requesting AdSense review.

---

## 4. Error handling & edge cases

- **Unknown slug:** `UseCaseController` already redirects unknown slugs to `/` — adding a slug needs no controller change; a slug in `ALL` *without* message keys would render blank sections (the `th:if` guards hide empty sections, risking a thin page). **Mitigation:** author all keys in the same change that adds the slug; add a test asserting key completeness (§5).
- **Missing `navLabel`:** Thymeleaf `#{...}` on a missing key renders `??key??`. **Mitigation:** the key-completeness test covers `navLabel` for every slug in `ALL`.
- **`th:each` footer + locale:** labels resolve per request locale via `MessageSource`; verify EN labels exist.
- **OG SVG endpoint (if built):** must be public and cached like existing `StaticResourceController` assets; keep it `noindex`-irrelevant (images).

---

## 5. Testing

- **Key-completeness test (new, important):** a `@SpringBootTest`/unit test iterating `UseCaseSlugs.ALL × {ko, en}` asserting every required key family resolves to a non-blank message (title, description, body, steps, intro, all section keys, all FAQ q/a, navLabel). This guards against the thin-page-from-missing-keys failure mode and future slug additions.
- **Controller test:** `GET /use-cases/club-activity` → 200 + view `use-cases/detail`; unknown slug → redirect.
- **Sitemap test:** `club-activity` (both locales) present in `/sitemap.xml`.
- **i18n number-format guard:** existing `SeoServiceI18nTest` patterns still pass.
- **Manual:** the §3.5 verification checklist; visual side-by-side of all 5 use-case pages to confirm differentiation.

---

## 6. CLAUDE.md / ADR sync (repo rule)

Per the repo's sync rule:
- **Fact change → update CLAUDE.md:** root `CLAUDE.md` DateDate section lists "친구 모임, 팀 회의, 여행 계획, 스터디 그룹" — add **동호회(club-activity)**. No numeric/path facts otherwise change.
- **Decision change → ADR:** none of these are policy/structure reversals (no new module, no security/availability-model change), so **no new ADR is required**. The data-driven footer + per-slug differentiation are implementation improvements, not decisions. *(If per-slug OG image generation introduces a new asset-serving approach, note it in the relevant frontend ADR rather than a new one.)*
- **Commit convention:** per repo workflow, do **not** run `git commit`; record the change as a section in `docs/git_commit.md`.

---

## 7. Reapply (AdSense Policy Center)

1. Ship Components A–E; verify (§3.5).
2. Confirm Google re-crawled the changed/new pages (Search Console URL Inspection).
3. AdSense → **Policy Center** → site → **Fix** → **Start review process** → "Fixed the violations" → Request review. Make **all** fixes first (the button throttles after repeated rejections).
4. No published SLA; expect days–~2 weeks. If rejected again with "not enough original content," escalate to the deferred **Full content layer** (guides hub).

---

## 8. Honest expectation

The technical groundwork is already done, so this is a **writing-heavy** pass, mostly Korean. The lean scope (differentiate 4 + 1 new distinct page + deepen home + nav) is a *meaningful* increase in original, non-templated content, but a thin utility app sometimes needs the larger guides hub before approval. Treat this as iteration 1; the `/guides` hub is the prepared fallback.
