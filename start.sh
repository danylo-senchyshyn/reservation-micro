#!/bin/sh
# Start the reservation microservices stack
# Usage: ./start.sh [--build]

set -e

echo "=== Reservation Microservices ==="
echo "Stopping any existing containers..."
docker compose down 2>/dev/null || docker-compose down 2>/dev/null || true

echo "Starting stack..."
docker compose up --build -d 2>/dev/null || docker-compose up --build -d

echo ""
echo "Waiting for services to be ready..."
sleep 20

echo ""
echo "=== Services ready ==="
echo "  Gateway (all APIs):        http://localhost:8080"
echo ""
echo "  Swagger UI:"
echo "    User Service:            http://localhost:8080/user-service/swagger-ui/index.html"
echo "    Reservation Service:     http://localhost:8080/reservation-service/swagger-ui/index.html"
echo "    Payment Service:         http://localhost:8080/payment-service/swagger-ui/index.html"
echo ""
echo "  E2E flow:"
echo "    1. POST /api/users           - create user"
echo "    2. POST /api/reservations    - create reservation (uses userId)"
echo "    3. Wait ~5s for Kafka event -> payment auto-created"
echo "    4. GET  /api/payments/by-reservation/{id} - find payment"
echo "    5. POST /api/payments/{id}/confirm or /fail"
