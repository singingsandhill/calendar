# 카카오 로그인 + 연간 Recap — 로컬 테스트 / 서버 반영 체크리스트

- 일자: 2026-07-11
- 대상 기능: 카카오 OAuth2 로그인, 오너 계정 연결, 활동 이벤트 기록, 연간 recap + 공개 공유
- 관련 문서: [설계 스펙](superpowers/specs/2026-07-11-kakao-login-recap-design.md) ·
  [ADR common/security/0004](adr/common/security/0004-kakao-oauth2-login.md) ·
  [ADR datedate/domain/0005](adr/datedate/domain/0005-user-activity-event-recap.md) ·
  커밋 절차: `docs/git_commit.md` **Commit 72~81**

> 자동 테스트는 전부 통과한 상태(413 tests, 0 failures — 더미 키). 아래는 **실제
> 카카오 키가 필요해 사람이 직접 해야 하는 작업**만 모았다.

> **자동 검증 결과 (2026-07-14, 실키 주입 후 curl 기반):** `[x]` 표시 항목은 실키로
> 기동한 로컬 앱에 대해 검증 완료. 증거 — ① `/oauth2/authorization/kakao` → kauth
> 인가 URL 302 (scope·redirect_uri·state·PKCE 정상), ② 인가 URL 추적 시 KOE 에러 없이
> 카카오 계정 로그인 페이지 도달 (앱 키·Redirect URI 등록 유효), ③ 토큰 엔드포인트
> 더미 코드 프로브가 **KOE320**(코드 무효) 반환 — KOE010 이 아니므로 **Client Secret
> 활성화 확인**, ④ 익명 플로우 API 스모크 (오너 생성 → 일정 → 참여자 → 날짜 선택 →
> 장소 투표 → 뷰 렌더) 전부 정상, 어드민 진입점(`/runners/admin`·`/trading` →
> `/runners/admin/login`) 무회귀. 실제 카카오 계정 로그인·동의가 필요한 항목만 남음.

---

## 1. 로컬 테스트 TODO

### 1-1. 카카오 디벨로퍼스 콘솔 (1회 준비)

- [x] [developers.kakao.com](https://developers.kakao.com) 앱 생성 → **REST API 키** 확보
- [x] 앱 설정 > 플랫폼 > Web: `http://localhost:8081` 등록
- [x] 제품 설정 > 카카오 로그인 **활성화** + Redirect URI 등록:
      `http://localhost:8081/login/oauth2/code/kakao` (인가 요청 KOE006 없음 — 검증됨)
- [x] 카카오 로그인 > 보안: **Client Secret 생성 + "활성화" 상태로 변경**
      (미활성 시 토큰 교환이 KOE010 으로 실패 → `/login?error`) — KOE320 프로브로 활성화 검증됨
- [x] 동의항목: 닉네임(`profile_nickname`)·프로필 사진(`profile_image`) 활성화
      (이메일은 사용하지 않음 — 비즈 앱 전환 불필요) — scope 요청 수락됨(간접), 동의창에서 최종 확인

### 1-2. 로컬 환경 설정

- [x] `.env` 에 키 기입:
  ```properties
  KAKAO_CLIENT_ID=<REST API 키>
  KAKAO_CLIENT_SECRET=<Client Secret>
  ```
- [ ] (선택) 공유 링크를 로컬 도메인으로 확인하려면 `.env` 또는 실행 인자로
      `app.base-url=http://localhost:8081` 오버라이드.
      **미설정 시 공유 링크가 `https://datedate.site/...` 로 복사됨** — 기본값에 의한
      착시일 뿐 버그 아님.
- [x] 앱 기동: `cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat bootRun"`
      (WSL 에서 확인은 `cmd.exe /c curl` — 앱이 Windows 프로세스로 뜸)

### 1-3. 수동 QA 시나리오

**기본 왕복**

- [x] `http://localhost:8081/` 헤더에 "카카오 로그인" 버튼 노출 (렌더 HTML 검증)
- [x] 버튼 클릭 → kauth.kakao.com 동의 화면 → 동의 → `/me` 복귀, 닉네임·프로필 표시
      — 2026-07-14 사용자 브라우저 QA: `/me` 닉네임·프로필 이미지 표시, `/recap/2026`
      빈 상태 렌더("아직 2026의 기록이 없어요"), 헤더 로그인/비로그인 상태 전환 확인
- [ ] 카카오 동의창에서 **"취소"** → `/login?error` 로 복귀, 에러 메시지 렌더
- [ ] 미인증으로 보호 딥링크(`/recap/2026`) 접근 → `/login` → 로그인 → **원래 목적지 복귀**

**오너 연결·활동·recap**

- [ ] 로그인 상태에서 `POST /start`(홈 폼)로 오너 생성 → `/me` 오너 목록에 표시
- [ ] 기존(미연결) 오너 대시보드에서 "내 계정에 연결" 버튼 → 연결됨 배너로 전환
- [ ] 일정 생성 + 참여자 추가 + 날짜 선택 + 장소/메뉴 투표 → `/recap/2026` 수치 반영
- [ ] **미연결** 오너 페이지에서 로그인 상태로 일정 생성 → recap "만든 일정"에 합산되는지
- [ ] 공유 링크 생성 → 시크릿 창(비로그인)에서 공유 URL 열림 (닉네임만, 프로필 이미지 없음)
- [ ] 공유 페이지 소스에 `noindex` 메타 확인, 카카오톡 붙여넣기 시 OG 미리보기 카드
- [ ] `?lang=en` 으로 영어 recap 확인

**무회귀·보안**

- [x] 시크릿 창(익명)에서 일정 생성·참여·투표가 **기존과 동일하게** 동작 (로그인 강제 없음)
      — API 스모크로 검증: POST /start(CSRF 폼) → 일정 → 참여자 → 선택 → 장소 투표 → 뷰 200
- [x] 시크릿 창에서 `/me` → `/login` 리다이렉트 (무세션 curl 302 확인)
- [ ] 러너 어드민 로그인/로그아웃 기존 동작 확인 (`/runners/admin/login` → 로그아웃 → `/runners`)
      — 미인증 진입점 302 는 검증됨, 실제 로그인 왕복만 남음
- [ ] 어드민 세션 상태에서 카카오 로그인 → 어드민 인증이 **대체**됨 (역할 상호 배타 — 의도 동작)
- [ ] 카카오 로그아웃(`POST /logout`) → `/` 복귀, 헤더가 비로그인 상태로 전환

### 1-4. 커밋

- [ ] `docs/git_commit.md` 의 **Commit 72 ~ 81** 섹션을 순서대로 실행
      (검증: 커밋 전 `cmd.exe /c "set JAVA_HOME=C:\jdk-21&& .\gradlew.bat test"` GREEN)

---

## 2. 서버 반영 TODO

### 2-1. 배포 전 (카카오 콘솔·설정)

- [ ] 카카오 콘솔 > 플랫폼 > Web 에 `https://datedate.site` 추가
- [ ] Redirect URI 추가: `https://datedate.site/login/oauth2/code/kakao`
      (**등록값과 실제 redirect_uri 가 한 글자라도 다르면 KOE006** — 인가 단계 실패)
- [ ] 운영 서버 `.env` 에 `KAKAO_CLIENT_ID` / `KAKAO_CLIENT_SECRET` 설정
      (로컬과 같은 앱을 쓰면 키 동일, 앱을 분리하면 운영 키)
- [ ] `app.base-url` 이 운영 기본값(`https://datedate.site`)인지 확인 (공유 링크 도메인)

### 2-2. 프록시(nginx) 확인

`application.yaml` 에 `server.forward-headers-strategy: native` 가 이미 설정돼 있어
Spring 이 `X-Forwarded-*` 를 해석한다. nginx 쪽에서 다음이 전달되는지 확인:

- [ ] `proxy_set_header X-Forwarded-Proto $scheme;` (없으면 redirect_uri 가
      `http://...` 로 생성돼 카카오 등록값과 불일치 → KOE006)
- [ ] `proxy_set_header Host $host;` (또는 X-Forwarded-Host)
- [ ] 배포 후 실제 인가 요청 URL 의 `redirect_uri` 파라미터가
      `https://datedate.site/login/oauth2/code/kakao` 인지 브라우저 주소창에서 확인

### 2-3. 첫 기동 (DB 마이그레이션 — `ddl-auto: update` 자동)

- [ ] 기동 로그에서 신규 스키마 반영 무오류 확인:
      테이블 `app_users`(kakao_id unique) · `user_activities`(userId,occurredAt 인덱스) ·
      `recap_shares`(token unique, userId+shareYear unique) + `owners.user_id`(nullable) 컬럼
- [ ] 기존 `owners`/`schedules` 데이터 무손실 확인 (기존 오너 페이지 정상 렌더)

### 2-4. 운영 스모크 테스트

- [ ] 카카오 로그인 왕복 (동의 → `/me`) — HTTPS 환경에서
- [ ] 세션 쿠키 응답 헤더에 `SameSite=Lax` 포함 확인 (`Set-Cookie`)
- [ ] `/recap/share/{token}` 비로그인 공개 접근 + `robots: noindex`
- [ ] 익명 플로우 스모크 (일정 생성·참여·투표 기존 동작)
- [ ] 어드민 영역 회귀: `/runners/admin/**`, `/trading` 이 여전히 어드민 전용
      (카카오 ROLE_USER 로 `/trading` 접근 → 403)
- [ ] 사이트맵·SEO 무영향 확인 (`/sitemap.xml` — 신규 페이지는 전부 noindex 라 미포함이 정상)

### 2-5. 반영 후 관찰·롤백 노트

- [ ] 로그인 실패율 관찰: `/login?error` 빈도, 로그의 `OAuth2AuthenticationException`
      (카카오 에러코드 KOE___ 는 [공식 문서 문제 해결](https://developers.kakao.com/docs/latest/ko/kakaologin/trouble-shooting) 참조)
- [ ] 롤백 시: 기능은 전부 **추가형**(익명 플로우 무변경, 신규 테이블/nullable 컬럼)이라
      이전 버전 재배포만으로 안전 — DB 되돌림 불필요
- [ ] 후속 과제(문서화된 보류): `/api/me/**` CSRF 토큰화(ADR 0004), first-claim
      동시성 race 수용(ADR 0005), 카카오 unlink·탈퇴 플로우
