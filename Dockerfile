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

# Respeita limite do container, heap 512MB→1.5GB, G1GC, heap dump em OOM
ENV JAVA_OPTS="-XX:+UseContainerSupport \
  -Xms512m -Xmx1536m \
  -XX:+UseG1GC -XX:MaxGCPauseMillis=200 \
  -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/heapdump.hprof \
  -Duser.timezone=America/Sao_Paulo"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
