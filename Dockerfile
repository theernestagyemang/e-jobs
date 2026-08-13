# ---- Stage 1: build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copy the pom first so the dependency layer is cached and only re-resolves
# when pom.xml itself changes.
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# ---- Stage 2: runtime ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Only the fat jar crosses the stage boundary - no Maven, no source, no ~/.m2.
COPY --from=build /build/target/*.jar app.jar

ENV JAVA_OPTS="-Xmx350m -Xms200m"
EXPOSE 8080
# Without exec, sh stays PID 1 and never forwards SIGTERM to the JVM, so the container is SIGKILLed instead of shutting down gracefully.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
