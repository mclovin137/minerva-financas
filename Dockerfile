# Build e execução do backend Minerva Finanças.
#
# Duas etapas de propósito: a imagem final não carrega Maven, código-fonte nem cache de dependências,
# só o JRE e o jar. As dependências são resolvidas em uma camada própria, antes de copiar o código,
# para que uma mudança de código não invalide o download do Maven.

FROM node:24-alpine AS interface
WORKDIR /interface
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build

COPY backend/pom.xml ./pom.xml
RUN mvn -B -q dependency:go-offline

COPY backend/src ./src

# A interface entra em static/, para que a aplicação sirva API e telas no mesmo host e na mesma porta.
COPY --from=interface /interface/dist ./src/main/resources/static

RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app

# Usuário sem privilégios: um processo comprometido não deve ter permissão de root no contêiner.
RUN useradd --system --uid 10001 --create-home minerva
USER minerva

# O banco SQLite fica em volume próprio, senão os dados morrem junto com o contêiner.
VOLUME ["/app/data"]
ENV SPRING_DATASOURCE_URL=jdbc:sqlite:/app/data/minerva-financas.db

COPY --from=build /build/target/*.jar /app/minerva-financas.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/minerva-financas.jar"]
