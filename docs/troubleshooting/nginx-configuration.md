# Nginx 설정 이슈

## http2 지시어 deprecated 경고

### 증상
```
[warn] the "listen ... http2" directive is deprecated, use the "http2" directive instead in /etc/nginx/sites-enabled/datedate:7
```

`sudo nginx -T` 또는 `sudo nginx -t` 실행 시 경고 메시지 출력.

### 원인
Nginx 1.25.1 이후 버전에서 `listen` 지시어의 `http2` 파라미터가 deprecated됨. 별도의 `http2` 지시어를 사용해야 함.

### 해결방법

**변경 전:**
```nginx
server {
    listen 443 ssl http2;
    server_name example.site;
    ...
}
```

**변경 후:**
```nginx
server {
    listen 443 ssl;
    http2 on;
    server_name example.site;
    ...
}
```

### 확인
```bash
sudo nginx -t
# nginx: configuration file /etc/nginx/nginx.conf test is successful
```

---

## 중복 server_name 충돌 경고

### 증상
```
[warn] conflicting server name "example.com" on 0.0.0.0:80, ignored
```

### 원인
동일한 `server_name`이 여러 설정 파일에서 같은 포트로 정의됨.

### 진단
```bash
# 모든 설정 파일에서 server_name 검색
sudo grep -r "server_name" /etc/nginx/sites-enabled/
```

### 해결방법
1. 중복 파일 확인
2. 불필요한 설정 파일 삭제 또는 비활성화

```bash
# 중복 파일 비활성화
sudo mv /etc/nginx/sites-enabled/duplicate-file /etc/nginx/sites-available/

# 또는 삭제
sudo rm /etc/nginx/sites-enabled/duplicate-file
```

---

## HTTP 서버 블록 server_name 누락 (SEO 문제)

### 증상
- 모든 HTTP 요청이 의도하지 않은 도메인으로 리디렉션됨
- Google Search Console에서 "리디렉션이 포함된 페이지" 경고

### 원인
HTTP 서버 블록에 `server_name`이 없으면 모든 도메인의 요청을 처리함.

**문제 코드:**
```nginx
server {
    listen 80;
    # server_name 누락!
    return 301 https://$host$request_uri;
}
```

### 해결방법
```nginx
server {
    listen 80;
    server_name example.site www.example.site;
    return 301 https://example.site$request_uri;
}
```

---

## www ↔ non-www 리디렉션 설정

### 요구사항
모든 URL 변형이 1회 리디렉션으로 정식 URL에 도달해야 함.

### 권장 설정 (non-www 선호)

```nginx
# 1. HTTP → HTTPS 리디렉션 (모든 변형)
server {
    listen 80;
    server_name example.site www.example.site;
    return 301 https://example.site$request_uri;
}

# 2. HTTPS www → HTTPS non-www
server {
    listen 443 ssl;
    http2 on;
    server_name www.example.site;

    ssl_certificate /etc/letsencrypt/live/example.site/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/example.site/privkey.pem;

    return 301 https://example.site$request_uri;
}

# 3. 메인 서버 (HTTPS non-www)
server {
    listen 443 ssl;
    http2 on;
    server_name example.site;

    ssl_certificate /etc/letsencrypt/live/example.site/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/example.site/privkey.pem;

    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_prefer_server_ciphers off;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### 리디렉션 흐름

| 요청 URL | 리디렉션 횟수 | 최종 URL |
|----------|--------------|----------|
| `http://example.site/` | 1 | `https://example.site/` |
| `http://www.example.site/` | 1 | `https://example.site/` |
| `https://www.example.site/` | 1 | `https://example.site/` |
| `https://example.site/` | 0 | (직접 응답) |

### 테스트
```bash
curl -I http://www.example.site/
curl -I http://example.site/
curl -I https://www.example.site/
```

---

## 보안: server_tokens 설정

### 증상
HTTP 응답 헤더에 Nginx 버전 정보 노출:
```
Server: nginx/1.24.0
```

### 해결방법
`/etc/nginx/nginx.conf`에서:
```nginx
http {
    server_tokens off;
    ...
}
```

### 확인
```bash
curl -I https://example.site/ | grep Server
# Server: nginx
```

---

## 성능: Gzip 압축 설정

### 증상
- 페이지 로딩 속도 저하
- 네트워크 전송량 과다

### 해결방법
`/etc/nginx/nginx.conf`에서 gzip 설정 활성화:

```nginx
http {
    gzip on;
    gzip_vary on;
    gzip_proxied any;
    gzip_comp_level 6;
    gzip_buffers 16 8k;
    gzip_http_version 1.1;
    gzip_types text/plain text/css application/json application/javascript
               text/xml application/xml application/xml+rss text/javascript
               image/svg+xml;
}
```

### 확인
```bash
curl -H "Accept-Encoding: gzip" -I https://example.site/
# Content-Encoding: gzip
```

---

## 설정 변경 후 적용

### 1. 문법 검사
```bash
sudo nginx -t
```

### 2. 설정 리로드 (무중단)
```bash
sudo systemctl reload nginx
```

### 3. 전체 재시작 (필요 시)
```bash
sudo systemctl restart nginx
```

---

## 디버깅 명령어

```bash
# 모든 설정 출력
sudo nginx -T

# 설정 문법 검사
sudo nginx -t

# 에러 로그 확인
sudo tail -f /var/log/nginx/error.log

# 액세스 로그 확인
sudo tail -f /var/log/nginx/access.log

# 활성화된 사이트 목록
ls -la /etc/nginx/sites-enabled/
```

---

## 영향받는 파일

| 파일 | 용도 |
|------|------|
| `/etc/nginx/nginx.conf` | 전역 설정 (gzip, server_tokens) |
| `/etc/nginx/sites-available/` | 사이트 설정 원본 |
| `/etc/nginx/sites-enabled/` | 활성화된 사이트 (심볼릭 링크) |

---

## 관련 문서

- [Nginx 공식 문서](https://nginx.org/en/docs/)
- [Google Search Console Redirect](google-search-console-redirect.md) - SEO 리디렉션 문제
