# ── Stage 1: build ────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copia o pom primeiro — o Docker cacheia as dependências separadamente do código.
# Só baixa tudo de novo se o pom.xml mudar.
COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn package -DskipTests -q

# ── Stage 2: runtime ───────────────────────────────────────────────────────────
# Imagem final muito menor — não inclui Maven nem JDK, só JRE
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN groupadd -r app && useradd -r -g app app
USER app

COPY --from=build /app/target/sosvideos-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
