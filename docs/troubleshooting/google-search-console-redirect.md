# Google Search Console 리디렉션 이슈

## 증상

Google Search Console에서 다음과 같은 경고가 표시됨:

```
리디렉션이 포함된 페이지
이러한 페이지는 색인이 생성되지 않거나 Google에 게시되지 않습니다
http://www.example.site/
유효성 검사 상태: 실패함
```

### 확인 방법

1. **Google Search Console** → 색인 생성 → 페이지 → "리디렉션이 포함된 페이지" 확인
2. **URL 검사 도구**에서 해당 URL 입력 후 상태 확인

---

## 원인

### 1. 프로토콜 불일치 (HTTP vs HTTPS)

애플리케이션의 여러 위치에서 URL 프로토콜이 일관되지 않음:

| 위치 | 설정값 | 문제 |
|------|--------|------|
| `robots.txt` | `http://example.site/sitemap.xml` | HTTP 사용 |
| `sitemap.xml` | `https://example.site/` | HTTPS 사용 |
| `application.yaml` | `http://example.site` | HTTP 사용 |
| `SeoService.java` | `http://example.site` (기본값) | HTTP 사용 |

### 2. www 서브도메인 리디렉션 체인

서버/DNS 레벨에서 www 서브도메인 리디렉션이 발생:

```
http://www.example.site/
    ↓ 301 Redirect (www 제거)
http://example.site/
    ↓ 301 Redirect (HTTPS 전환)
https://example.site/  ← 최종 URL
```

**문제점**: 2번의 리디렉션이 발생하여 Google이 이를 "리디렉션이 포함된 페이지"로 분류

### 3. 근본 원인

- 개발 시 HTTP로 시작하여 프로덕션에서 HTTPS로 전환할 때 설정 누락
- www와 non-www 도메인 간의 정식 URL(canonical) 미설정
- 인프라(DNS, 웹서버)와 애플리케이션 코드 간의 URL 불일치

---

## 해결방법

### Step 1: robots.txt 수정

**파일**: `src/main/resources/static/robots.txt`

```diff
- Sitemap: http://example.site/sitemap.xml
+ Sitemap: https://example.site/sitemap.xml
```

### Step 2: SeoService 기본값 수정

**파일**: `src/main/java/.../application/service/SeoService.java`

```diff
- @Value("${app.base-url:http://example.site}")
+ @Value("${app.base-url:https://example.site}")
  private String baseUrl;
```

### Step 3: application.yaml 수정

**파일**: `src/main/resources/application.yaml`

```diff
  app:
-   base-url: http://example.site
+   base-url: https://example.site
```

### Step 4: sitemap.xml 확인

**파일**: `src/main/resources/static/sitemap.xml`

모든 `<loc>` 태그가 `https://`를 사용하는지 확인:

```xml
<url>
  <loc>https://example.site/</loc>  <!-- http가 아닌 https -->
</url>
```

---

## Google Search Console 설정

### Step 5: 도메인 속성 등록 (권장)

1. [Google Search Console](https://search.google.com/search-console) 접속
2. 좌측 상단 속성 선택 → "속성 추가"
3. **"도메인" 유형 선택** → `example.site` 입력
4. DNS TXT 레코드로 소유권 확인

> 도메인 속성은 모든 서브도메인(www 포함)과 프로토콜(http/https)을 통합 관리

### Step 6: 유효성 검사 요청

1. Search Console → 해당 속성 선택
2. 좌측 메뉴: "색인 생성" → "페이지"
3. "리디렉션이 포함된 페이지" 클릭
4. 문제 URL 선택 → **"유효성 검사 요청"**

### Step 7: 정식 URL 및 Sitemap 제출

1. 상단 검색창에 `https://example.site/` 입력
2. "색인 생성 요청" 클릭
3. 좌측 메뉴: "Sitemap" → `https://example.site/sitemap.xml` 제출

---

## 디버깅

### curl로 리디렉션 체인 확인

```bash
# 리디렉션 따라가며 확인
curl -I -L http://www.example.site/

# 예상 출력:
# HTTP/1.1 301 Moved Permanently
# Location: https://example.site/
#
# HTTP/2 200
```

**이상적인 결과**: 1번의 301 리디렉션 후 200 OK

### 문제 있는 출력 예시

```bash
# 2번 이상의 리디렉션 (문제)
HTTP/1.1 301 Moved Permanently
Location: http://example.site/

HTTP/1.1 301 Moved Permanently
Location: https://example.site/

HTTP/2 200
```

---

## 서버 설정 (인프라)

### Nginx 권장 설정

모든 변형을 단일 리디렉션으로 처리:

```nginx
# HTTP → HTTPS (www 포함)
server {
    listen 80;
    server_name www.example.site example.site;
    return 301 https://example.site$request_uri;
}

# HTTPS www → HTTPS non-www
server {
    listen 443 ssl;
    server_name www.example.site;
    return 301 https://example.site$request_uri;
}

# 메인 서버 (HTTPS non-www)
server {
    listen 443 ssl;
    server_name example.site;
    # ... 앱 설정
}
```

### Apache .htaccess

```apache
RewriteEngine On

# HTTP/HTTPS + www 모두 https://non-www로 리디렉션
RewriteCond %{HTTPS} off [OR]
RewriteCond %{HTTP_HOST} ^www\. [NC]
RewriteRule ^ https://example.site%{REQUEST_URI} [L,R=301]
```

---

## 예방 체크리스트

배포 전 확인사항:

- [ ] `robots.txt`의 Sitemap URL이 `https://`인가?
- [ ] `sitemap.xml`의 모든 URL이 `https://`인가?
- [ ] `application.yaml`의 `app.base-url`이 `https://`인가?
- [ ] 코드의 기본 URL 값이 `https://`인가?
- [ ] 웹서버에서 www → non-www 리디렉션이 1회로 완료되는가?
- [ ] HTTP → HTTPS 리디렉션이 1회로 완료되는가?
- [ ] canonical URL이 올바르게 설정되어 있는가?

---

## 영향받는 파일

| 파일 | 수정 내용 |
|------|-----------|
| `src/main/resources/static/robots.txt` | Sitemap URL 프로토콜 |
| `src/main/resources/static/sitemap.xml` | 모든 URL 프로토콜 |
| `src/main/resources/application.yaml` | `app.base-url` |
| `src/main/java/.../SeoService.java` | 기본값 프로토콜 |

---

## 예상 결과

코드 배포 + GSC 재검증 후:
- Google이 `https://example.site/`를 정식 URL로 인식
- www 버전은 정상 리디렉션으로 처리됨
- **색인 경고 해결까지 약 1-2주 소요**

---

## 관련 자료

- [Google Search Console 도움말 - 리디렉션](https://support.google.com/webmasters/answer/7440203)
- [Google URL 검사 도구](https://support.google.com/webmasters/answer/9012289)
- [Canonical URL 설정](https://developers.google.com/search/docs/crawling-indexing/consolidate-duplicate-urls)
