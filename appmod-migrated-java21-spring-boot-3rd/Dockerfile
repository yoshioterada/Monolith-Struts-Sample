FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -q
COPY src src
RUN mvn package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine

# 非ルートユーザーで実行（コンテナセキュリティ対策必須）
RUN addgroup -S skishop && adduser -S skishop -G skishop

WORKDIR /app
COPY --from=build --chown=skishop:skishop /app/target/skishop-app-*.jar app.jar
USER skishop

# Java 21 コンテナ対応 JVM フラグ（-Xmx ハードコードよりも %指定準拠）
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "/app/app.jar"]

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1
