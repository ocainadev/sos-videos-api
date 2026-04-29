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

# Respeita limite do container, G1GC, heap dump em OOM.
# -Xmx512m: a app é I/O-bound (streaming), não precisa de heap grande.
# -XX:MaxDirectMemorySize=128m: NIO/networking aloca fora do heap por padrão
#   até Xmx (512 MB) — sem esse cap a VPS esgota RAM mesmo com heap pequeno.
ENV JAVA_OPTS="-XX:+UseContainerSupport \
  -Xms128m -Xmx512m \
  -XX:MaxDirectMemorySize=128m \
  -XX:+UseG1GC -XX:MaxGCPauseMillis=200 \
  -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/heapdump.hprof \
  -Duser.timezone=America/Sao_Paulo"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
