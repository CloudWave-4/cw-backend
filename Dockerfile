# Stage 1: Build

FROM gradle:8.14-jdk17 AS builder

WORKDIR /app

# Gradle 캐싱을 위해 의존성 파일 먼저 복사
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# 의존성 다운로드 (캐싱 활용)
RUN gradle dependencies --no-daemon

# 소스 코드 복사
COPY src ./src

# 애플리케이션 빌드 (테스트 제외)
RUN gradle bootJar --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# wget 없을 수 있어서 안전하게
RUN apk add --no-cache wget

# OpenTelemetry Java Agent 추가 (버전 고정 권장)
RUN wget -O /opentelemetry-javaagent.jar \
    https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v1.32.0/opentelemetry-javaagent.jar

# 보안을 위한 non-root 사용자 생성
RUN addgroup -g 1001 appgroup && \
    adduser -u 1001 -G appgroup -D appuser

# 빌드된 jar 파일 복사(한 번만)
COPY --from=builder /app/build/libs/*.jar /app/app.jar

# 소유권 변경
RUN chown appuser:appgroup /app/app.jar

# non-root 사용자로 전환
USER appuser

# 포트 노출
EXPOSE 8080

# 환경변수 (런타임에 주입)
ENV JAVA_OPTS=""
ENV DB_URL=""
ENV DB_NAME=""
ENV DB_USERNAME=""
ENV DB_PW=""

# 애플리케이션 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -javaagent:/opentelemetry-javaagent.jar -jar /app/app.jar"]
