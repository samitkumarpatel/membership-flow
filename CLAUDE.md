# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./mvnw clean package

# Run tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=MembershipFlowApplicationTests

# Run the app locally (uses Testcontainers to start MongoDB automatically — no separate MongoDB needed)
./mvnw spring-boot:test-run

# Build OCI image
./mvnw spring-boot:build-image
```

## Architecture

Spring Boot 4.0.6 web service using Java 25. Stack:

- **Web layer**: Spring Web MVC — servlet-based REST endpoints
- **Persistence**: Spring Data MongoDB — `mongo:latest` via Docker
- **HTTP client**: `RestClient` for outbound calls
- **Observability**: Spring Boot Actuator

Root package: `com.example.membership_flow` (underscore, not hyphen — the hyphenated form is invalid Java).

### Domain packages

| Package | Responsibility |
|---|---|
| `plan` | Salon owner creates/manages `MembershipPlan` documents (price, duration, benefits, gateway) |
| `member` | Salon customers — registration and lookup |
| `membership` | A member's subscription to a plan; holds gateway subscription ID and status |
| `payment.gateway` | `PaymentGateway` interface with `StripeGatewayService` and `ShopifyGatewayService` stubs |
| `payment` | `WebhookController` — receives `POST /api/webhooks/stripe` and `POST /api/webhooks/shopify` |
| `common` | `GlobalExceptionHandler` (RFC 9457 ProblemDetail), `MongoConfig` (auditing) |

### API surface

**Public (member-facing)**
- `GET /api/plans` — active plans only
- `GET /api/plans/{id}`
- `POST /api/members` — register member
- `GET /api/members/{id}`
- `POST /api/members/{memberId}/memberships` — subscribe (initiates gateway payment)
- `GET /api/members/{memberId}/memberships`

**Admin**
- `GET/POST /api/admin/plans`, `PUT /api/admin/plans/{id}`, `PATCH /api/admin/plans/{id}/status`
- `GET /api/admin/members`
- `GET /api/admin/memberships` (optional `?status=` filter), `GET /api/admin/memberships/{id}`, `PATCH /api/admin/memberships/{id}/cancel`

**Webhooks** (called by gateway, not clients)
- `POST /api/webhooks/stripe`
- `POST /api/webhooks/shopify`

### Payment gateway design

`PaymentGateway` is an interface selected at runtime by `MembershipPlan.Gateway` enum (STRIPE / SHOPIFY). Both implementations are stubs — replace the method bodies with real SDK calls. Toggle which gateways are loaded via `spring.application.payment.stripe.enabled` / `spring.application.payment.shopify.enabled` in `application.yaml`.

Subscription flow: `POST /api/members/{id}/memberships` → gateway creates subscription → returns `clientSecret` (Stripe) or `confirmationUrl` (Shopify) → frontend confirms → gateway fires webhook → membership status updated to ACTIVE.

## Testing

Tests use Testcontainers to spin up a real MongoDB container (`mongo:latest`). `TestcontainersConfiguration` wires this via `@ServiceConnection` so no manual URI config is needed. `TestMembershipFlowApplication` bootstraps the full app with the same Testcontainers MongoDB for local development runs.

Tests require Docker to be running.
