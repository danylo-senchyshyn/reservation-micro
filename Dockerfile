# Stage 1: Build all modules
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY . .
RUN mvn clean install -DskipTests

# Stage 2: Create the user-service image
FROM eclipse-temurin:17-jre-jammy AS user-service-image
WORKDIR /app
COPY --from=builder /app/services/user-service/target/user-service-1.0.0.jar ./app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

# Stage 3: Create the reservation-service image
FROM eclipse-temurin:17-jre-jammy AS reservation-service-image
WORKDIR /app
COPY --from=builder /app/services/reservation-service/target/reservation-service-1.0.0.jar ./app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

# Stage 4: Create the payment-service image
FROM eclipse-temurin:17-jre-jammy AS payment-service-image
WORKDIR /app
COPY --from=builder /app/services/payment-service/target/payment-service-1.0.0.jar ./app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

# Stage 5: Create the notification-service image
FROM eclipse-temurin:17-jre-jammy AS notification-service-image
WORKDIR /app
COPY --from=builder /app/services/notification-service/target/notification-service-1.0.0.jar ./app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

# Stage 6: Create the gateway-service image
FROM eclipse-temurin:17-jre-jammy AS gateway-service-image
WORKDIR /app
COPY --from=builder /app/services/gateway-service/target/gateway-service-1.0.0.jar ./app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
