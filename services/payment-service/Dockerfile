# ===== Build stage =====
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# 1) кэш зависимостей
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

# 2) исходники
COPY src ./src
RUN mvn -q -DskipTests package

# ===== Run stage =====
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

ENV JAVA_OPTS=""
EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]