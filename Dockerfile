# ---- build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml .
RUN mvn -B dependency:go-offline -q || true

COPY src ./src
COPY prerequisite-knowledge ./prerequisite-knowledge
RUN mvn -B package -DskipTests -q

# ---- runtime stage ----
# JDK (not JRE) is deliberate: the app compiles learner Java at runtime
# via javax.tools.JavaCompiler (jdk.compiler is JDK-only).
FROM eclipse-temurin:21-jdk
WORKDIR /app

COPY --from=build /build/target/*.jar app.jar
COPY --from=build /build/prerequisite-knowledge ./prerequisite-knowledge
COPY docker-entrypoint.sh ./docker-entrypoint.sh
RUN chmod +x docker-entrypoint.sh

EXPOSE 8080
ENTRYPOINT ["./docker-entrypoint.sh"]