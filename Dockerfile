# calendar 런타임 이미지 (ADR common/infrastructure/0001)
#
# jar 는 CI(GitHub Actions)가 빌드한다 — 이미지 안에서 Gradle 을 돌리지 않는다
# (e2-micro 서버 빌드 제거 + CI 러너의 Gradle 캐시가 더 효율적).
# 레이어 추출은 Boot 4 공식 Dockerfile 예제의 jarmode=tools 방식:
# dependencies(~95MB) 레이어가 캐시되어 코드만 바뀐 배포의 pull 이 수 MB 로 줄어든다.
FROM eclipse-temurin:21-jre AS builder
WORKDIR /builder
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

FROM eclipse-temurin:21-jre
# curl: compose healthcheck 용. 비 root 실행: /app/.env(실주문 키)·H2 파일이 붙는 컨테이너다.
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd -u 10001 -m app \
    && mkdir -p /app/data /app/logs \
    && chown -R app /app
# WORKDIR /app 은 계약이다 — 앱의 상대경로 셋이 전부 여기에 걸린다:
#   spring.config.import optional:file:.env  → /app/.env (볼륨, ro)
#   DB_URL jdbc:h2:file:./data/scheduledb    → /app/data (볼륨)
#   logback LOG_DIR=logs + StockMailService  → /app/logs (볼륨)
WORKDIR /app
COPY --from=builder --chown=app /builder/extracted/dependencies/ ./
COPY --from=builder --chown=app /builder/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=app /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=app /builder/extracted/application/ ./
USER app
# TZ 는 굽지 않는다 — compose 의 TZ 환경변수로 호스트와 동일하게 주입 (무존 trading cron 시각 보존).
# JVM 플래그도 compose 의 JAVA_TOOL_OPTIONS 로 주입 → 이미지 재빌드 없이 조정 가능.
# jarmode=tools 추출 레이아웃의 application.jar 는 의존성을 참조하는 경량 jar — JarLauncher 아님.
ENTRYPOINT ["java", "-jar", "application.jar"]
