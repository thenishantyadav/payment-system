# Payment System

Order-driven payment workflow built with Spring Boot, PostgreSQL, and Kafka.

## What This App Does

This project exposes an `order-service` API that:

- creates payment-backed orders for authenticated users
- prevents duplicate submissions with idempotency keys
- persists order state in PostgreSQL
- publishes `order.created` events to Kafka
- consumes payment result events and updates the order state machine

Current order lifecycle:

`CREATED -> PAYMENT_PENDING -> PAID | FAILED`

## Stack

- Java 17
- Spring Boot 3.2
- Spring Web
- Spring Data JPA
- Spring Security
- Spring Kafka
- PostgreSQL
- Docker Compose

## Architecture

```text
Client
  |
  v
order-service (REST API)
  |
  +--> PostgreSQL  (orders, status, idempotency)
  |
  +--> Kafka topic: order.created
          |
          v
     payment-service (future / external consumer)
          |
          +--> payment.success / payment.failed
                    |
                    v
              order-service consumer
```

## Project Layout

```text
payment-system/
├── docker-compose.yml
├── README.md
├── scripts/
│   └── demo.ps1
└── order-service/
    ├── pom.xml
    └── src/main/java/com/nishant/orderservice/
```

## Quick Start

### 1. Start infrastructure

```powershell
docker compose up -d
```

This starts:

- PostgreSQL on `localhost:5432`
- Redis on `localhost:6379`
- Zookeeper
- Kafka on `localhost:9092`

### 2. Run the service

```powershell
cd order-service
mvn spring-boot:run
```

The API starts on `http://localhost:8081`.

### 3. Verify health

```powershell
Invoke-WebRequest http://localhost:8081/actuator/health
```

## Live Demo

A demo script is included for local verification.

Run it after the service is up:

```powershell
.\scripts\demo.ps1
```

What it does:

- checks the health endpoint
- generates a signed development JWT using the local shared secret
- creates an order
- fetches the created order
- lists orders for the demo user

## API Summary

### Create order

`POST /api/v1/orders`

Headers:

- `Authorization: Bearer <jwt>`
- `X-Idempotency-Key: <unique-key>`

Body:

```json
{
  "amount": 499.99,
  "currency": "USD",
  "description": "Premium subscription"
}
```

### Get order

`GET /api/v1/orders/{orderId}`

### Get current user's orders

`GET /api/v1/orders`

### Health check

`GET /actuator/health`

## Local Development Notes

- The app forces JVM and JDBC time zone handling to `UTC` to avoid PostgreSQL startup issues on machines using `Asia/Calcutta`.
- The current JWT filter is intentionally lightweight for local development. It trusts tokens signed with the configured secret and uses the JWT `sub` claim as the authenticated user id.
- Kafka topics are auto-created by the local compose setup.

## Verification

The project has been verified locally with:

```powershell
cd order-service
mvn test
mvn spring-boot:run
```

and the live health check:

```text
GET http://localhost:8081/actuator/health -> 200 OK
```
