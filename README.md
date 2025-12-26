# Reservation Architecture Comparison

This project demonstrates a comparison between monolithic and microservice
architectures using a reservation system as an example.

The microservice version consists of multiple Spring Boot services communicating
via REST and Apache Kafka.

## Services
- Gateway Service
- User Service
- Reservation Service
- Payment Service
- Notification Service
- Common (shared event contracts)

## Communication
- REST (synchronous)
- Kafka (asynchronous, event-driven)

## Purpose
Bachelor thesis project – comparison of architectural approaches.