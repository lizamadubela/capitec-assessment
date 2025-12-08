# ---------- build stage ----------
FROM maven:3.9.4-eclipse-temurin-21 AS build

ARG MAVEN_OPTS="-DskipTests"
WORKDIR /build

# copy maven wrapper & pom first to leverage cache for dependency download
COPY pom.xml mvnw ./
COPY .mvn .mvn

# go-offline to download dependencies (faster subsequent builds)
RUN mvn -B dependency:go-offline

# copy sources and generate
COPY src ./src
# run package; skip tests by default (controlled by ARG)
RUN mvn -B ${MAVEN_OPTS} package

# ---------- runtime stage ----------
FROM eclipse-temurin:21-jre AS runtime

# create non-root user
RUN useradd -m appuser
WORKDIR /app

# copy the built jar from the build stage
# tries a SNAPSHOT pattern first then falls back to any jar
COPY --from=build /build/target/*-SNAPSHOT.jar /app/app.jar
# fallback (in case your artifact isn't a SNAPSHOT)
COPY --from=build /build/target/*.jar /app/app.jar

# create dirs used by docker-compose mounts
RUN mkdir -p /app/data /app/logs && chown -R appuser:appuser /app

USER appuser

EXPOSE 8081

# default JVM options (overridable from docker-compose / env)
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
