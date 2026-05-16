@echo off
REM Start the reservation microservices stack (Windows)
REM Usage: start.bat

echo === Reservation Microservices ===
echo Stopping any existing containers...
docker compose down 2>nul
if errorlevel 1 docker-compose down 2>nul

echo Starting stack...
docker compose up --build -d
if errorlevel 1 docker-compose up --build -d

echo.
echo Waiting for services to be ready (30 seconds)...
timeout /t 30 /nobreak >nul

echo.
echo === Services ready ===
echo   Gateway (all APIs):        http://localhost:8080
echo.
echo   Swagger UI:
echo     User Service:            http://localhost:8080/user-service/swagger-ui/index.html
echo     Reservation Service:     http://localhost:8080/reservation-service/swagger-ui/index.html
echo     Payment Service:         http://localhost:8080/payment-service/swagger-ui/index.html
echo.
echo   E2E flow:
echo     1. POST /api/users           - create user
echo     2. POST /api/reservations    - create reservation (uses userId)
echo     3. Wait ~5s for Kafka event -^> payment auto-created
echo     4. GET  /api/payments/by-reservation/{id} - find payment
echo     5. POST /api/payments/{id}/confirm or /fail
