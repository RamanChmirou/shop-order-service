# shop-order-service

Order service of the online shop. It turns the user's cart into an order, takes the payment through a
payment gateway port, issues the invoice and tells the other services that the order was placed.

In the target architecture the service is internal: it is called only by the `user-interaction-service`
proxy, which takes the `userId` from the access token. Other services never call it directly.

## Contents

- [Architecture](#architecture)
- [Order flow](#order-flow)
- [REST API](#rest-api)
- [Errors](#errors)
- [Kafka and the outbox](#kafka-and-the-outbox)
- [Scheduled jobs](#scheduled-jobs)
- [Configuration](#configuration)
- [Running](#running)
- [Tests](#tests)

## Architecture

Hexagonal, multi-module Maven project. The domain does not depend on Spring.

| Module    | Content |
|-----------|---------|
| `pub`     | Public contract: request/response DTOs, `Page`/`Pageable`, enums, the `OrderPlacedEvent`. |
| `model`   | Domain objects (`Order`, `OrderItem`, `Invoice`, `Price`, ...) and domain exceptions. Independent of `pub`. |
| `domain`  | `OrderService`, `OrderFactory`, `CheckoutValidator`, `DeliveryCostCalculator`, and the **ports**: `OrderRepositoryPort`, `CartPort`, `InvoicePort`, `PaymentPort`, `OrderEventPublisherPort`, `TransactionPort`, `OrderMapperPort`. |
| `adapter` | Spring Boot application: REST controller, JPA entities and repositories, MapStruct mappers, Feign client to cart-service, outbox publisher and relay, simulated payment gateway, stub invoice adapter, scheduled jobs, configuration. |

Technology: Java 21, Spring Boot 4, Spring Cloud OpenFeign + Resilience4j, PostgreSQL + Liquibase,
Kafka, MapStruct, springdoc (Swagger UI), Testcontainers, WireMock.

Each port has an adapter in the `adapter` module, for example `OrderRepositoryPort` is implemented by
`OrderRepositoryAdapter` (Spring Data repository + MapStruct mapper) and `CartPort` by `CartPortAdapter`
(Feign client). Replacing an adapter (for example the invoice provider) does not touch the domain.

## Order flow

```
checkout ──► NEW ──► pay ──► PAID ──► ship ──► SHIPPED
              │
              ├─ cancel ───────────────► CANCELLED
              └─ not paid in 24 h ─────► CANCELLED (scheduled job)
```

1. **Checkout** (`POST /orders/{userId}`)
   - the service asks cart-service to reprice the cart (`POST /carts/{userId}/reprice`);
   - the cart must not be empty; the total the user saw (`expectedTotal`) must equal the repriced total,
     otherwise the response is `409` with both totals, and no order is created;
   - the order freezes the verified price snapshot and adds the delivery cost;
   - in **one database transaction** the order is saved and an `ORDER_PLACED` event is written to the outbox.
2. **Pay** (`POST /orders/{userId}/{orderId}/pay`)
   - the order must be `NEW`;
   - `PaymentPort.charge` is called; if the gateway declines the payment the response is `402` and the order
     stays `NEW`;
   - the order becomes `PAID` and the invoice is issued; if the invoice service is unavailable the order is
     still `PAID`, the invoice is `FAILED` and can be retried (`POST .../invoice`).
3. **Ship** (`POST /orders/{orderId}/ship`) works only for `PAID` orders.
4. **Cancel** (`POST /orders/{userId}/{orderId}/cancel`) works only for `NEW` orders.

Delivery cost: `COURIER` 15.00, `PARCEL_LOCKER` 9.99, `PICKUP` 0.00. An order without a billing address
uses the shipping address. A shipping address is required unless the delivery method is `PICKUP`.

Concurrent modification of one order is detected by optimistic locking (`version` column) and returns `409`.

## REST API

Swagger UI: `http://localhost:8083/swagger-ui.html`

| Method | Path | Description | Success |
|--------|------|-------------|---------|
| POST | `/orders/{userId}` | Checkout of the user's cart | 201 |
| GET | `/orders/{userId}` | Orders of the user, paged | 200 |
| GET | `/orders/{userId}/{orderId}` | One order of the user | 200 |
| POST | `/orders/{userId}/{orderId}/pay` | Pay the order | 200 |
| POST | `/orders/{userId}/{orderId}/invoice` | Retry a failed invoice | 200 |
| POST | `/orders/{userId}/{orderId}/cancel` | Cancel an unpaid order | 200 |
| POST | `/orders/{orderId}/ship` | Ship a paid order (back office) | 200 |

Pagination of `GET /orders/{userId}`: query parameters `page` (default 0), `size`, `sortBy`
(`createdAt`, `grandTotal`, `orderNumber`, `status`) and `direction` (`ASC`/`DESC`). An unsupported
`sortBy` returns `400`. The response contains `content`, `page`, `size`, `totalElements`, `totalPages`.

Checkout request:

```json
{
  "customer": { "firstName": "Jan", "lastName": "Kowalski", "email": "jan@example.com", "phone": "+48123456789" },
  "deliveryMethod": "COURIER",
  "paymentMethod": "CARD",
  "shippingAddress": { "street": "Marszalkowska 1", "city": "Warszawa", "postalCode": "00-001", "country": "PL" },
  "expectedTotal": 10398.00
}
```

`deliveryMethod`: `COURIER`, `PARCEL_LOCKER`, `PICKUP`. `paymentMethod`: `CARD`, `BLIK`, `TRANSFER`,
`CASH_ON_DELIVERY`.

## Errors

All errors use one body: `status`, `error`, `message`, `timestamp`, `details`.

| Status | When |
|--------|------|
| 400 | Invalid request body, empty cart, unsupported sort field |
| 402 | The payment gateway declined the payment |
| 404 | The order does not exist or belongs to another user |
| 409 | The total changed since the user saw it, wrong order state (for example paying twice), invoice already issued, concurrent modification |
| 503 | cart-service is unavailable |

## Kafka and the outbox

The `order.placed` event must reach the cart-service (it closes the cart), but a plain "save and then send"
can lose the event if Kafka is down at that moment. The service uses the **transactional outbox**:

1. `checkout` saves the order and a row in `outbox_events` (`status = PENDING`) in one transaction;
2. `OutboxRelay` runs every 2 seconds, sends `PENDING` rows to the topic `order.placed` (key = order id) and
   marks them `SENT`;
3. when Kafka is unavailable the rows stay `PENDING` and are sent later, in order.

Delivery is at-least-once, so consumers must be idempotent. The cart-service consumer is: closing an already
closed cart does nothing.

Event payload: `eventId`, `orderId`, `orderNumber`, `userId`, `cartId`, `grandTotal`, `currency`, `occurredAt`.

## Scheduled jobs

| Job | What it does | Configuration |
|-----|--------------|---------------|
| `OutboxRelay` | Sends pending outbox events to Kafka | `outbox.relay-delay`, `outbox.batch-size`, `outbox.topic` |
| `UnpaidOrdersCleanupJob` | Cancels `NEW` orders older than the limit; an order paid at the same moment is skipped thanks to optimistic locking | `orders.cancel-unpaid.max-age` (24h), `batch-size`, `cron` |

## Configuration

Environment variables (defaults are for running outside Docker):

| Variable | Default | Meaning |
|----------|---------|---------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/order_db` | Database URL |
| `DB_USER` / `DB_PASSWORD` | `shop` / `shop` | Database credentials |
| `CART_SERVICE_URL` | `http://localhost:8082` | cart-service address |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka address |

Properties in `application.yml`:

| Property | Default | Meaning |
|----------|---------|---------|
| `payment.gateway.declined-methods` | `TRANSFER` | Payment methods that the **simulated** gateway declines (to demonstrate a failed payment) |
| `orders.cancel-unpaid.*` | 24h, 100, every 10 min | Cancellation of unpaid orders |
| `outbox.*` | 2000 ms, 50, `order.placed` | Outbox relay |
| `spring.cloud.openfeign.client.config.cart-service.*` | connect 2 s, read 5 s | Feign timeouts |

Resilience of the cart-service client: 3 attempts with backoff on network errors and `5xx`, no retry on `4xx`,
circuit breaker (Resilience4j), and a fallback that maps failures to `503` (a missing cart becomes "empty
cart", `400`).

## Running

The whole shop (PostgreSQL, Kafka, AKHQ, product, cart and order services) starts with one command from the
`shop-infra` repository:

```
cd shop-infra
docker compose up --build
```

| What | Address |
|------|---------|
| order-service | http://localhost:8083 (Swagger: `/swagger-ui.html`, health: `/actuator/health`) |
| AKHQ (Kafka UI) | http://localhost:8090 |

The databases are created by `shop-infra/postgres/init.sql`; the schema is created by Liquibase.

Running the service alone (needs PostgreSQL with `order_db`, Kafka and cart-service):

```
mvn -pl adapter -am spring-boot:run
```

REST requests for the whole flow are in `shop-infra/bruno/order-service` (Bruno collection). Run
`cart-service/02-add-laptop` first, then `order-service/01` to `13`.

## Tests

```
mvn clean test
```

| Kind | Where | What |
|------|-------|------|
| Unit | `domain` | `OrderService`: checkout, payment, invoice, cancellation, pagination, cleanup |
| Controller | `adapter` (`@WebMvcTest`) | Status codes and error mapping |
| Client | `adapter` (WireMock) | `CartPortAdapter`: success, 404, retry, fallback, connection reset |
| Unit | `adapter` | Outbox publisher, outbox relay, cleanup job |
| Integration | `adapter` (`OrderApiIntegrationTest`) | Whole API against PostgreSQL and Kafka in Testcontainers, cart-service in WireMock: checkout, event in Kafka, payment and decline, pagination and sorting, optimistic locking |

The integration test needs a running Docker.

## Known limitations

- The invoice provider is a **stub** (`StubInvoiceAdapter`); the real provider will be a new adapter for
  `InvoicePort`.
- The payment gateway is **simulated** (`SimulatedPaymentGateway`); refunds of paid orders are not supported,
  so a paid order cannot be cancelled.
