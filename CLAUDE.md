# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run the app (requires a running MongoDB instance)
./mvnw spring-boot:run

# Run the app with Testcontainers-managed MongoDB (no external MongoDB needed, requires Docker)
./mvnw spring-boot:run -Dspring-boot.run.mainClass=com.example.membership_flow.TestMembershipFlowApplication

# Build JAR
./mvnw package

# Run all tests (requires Docker for Testcontainers)
./mvnw test

# Run a single test class
./mvnw test -Dtest=MembershipFlowApplicationTests
```

MongoDB database name: `membership_flow` (configured in `application.yaml`). No URI is set by default — Spring Boot connects to `localhost:27017`.

## Architecture

**Spring Boot 4.0.6 / Java 25 / MongoDB / Spring WebMVC / RestClient**

The app is a Shopify subscription membership platform. It wraps Shopify's Admin GraphQL API and exposes two surfaces: a customer-facing portal and an admin dashboard, both served as static Vue 3 (CDN) SPAs from `src/main/resources/static/`.

### Domain packages

| Package | Responsibility |
|---|---|
| `member` | Member entity, registration, email-based lookup — no passwords |
| `subscription` | Subscription plan templates stored in MongoDB, each containing multiple `PlanEntry` records (interval + price). Each Subscription links to a Shopify SellingPlanGroup and a Shopify Product variant. |
| `membership` | A member's subscription instance. Lifecycle: `PENDING → ACTIVE → CANCELLED / SUSPENDED`. Holds a `checkoutUrl` and `nextBillingDate`. |
| `shopify` | Shopify integration: `ShopifyAdminGraphQLClient` (Admin GraphQL mutations via RestClient), `ShopifyTokenService` (token caching via inner `CachedToken`), `ShopifyCheckoutUrlBuilder`, `ShopifyProperties` (config). |
| `webhook` | `ShopifyWebhookController` — receives Shopify subscription events to activate/update memberships. |
| `common` | `GlobalExceptionHandler`, `MongoConfig`, `WebConfig` with `SpaFallbackInterceptor` (serves `index.html` for unknown paths, enabling SPA routing). |

### Request flows

**Subscription creation (admin):**
`POST /api/admin/subscriptions` → `SubscriptionService` → `ShopifyAdminGraphQLClient` calls `sellingPlanGroupCreate` + creates and attaches a Shopify product → persists `Subscription` to MongoDB.

**Member subscribe (customer):**
`POST /api/members/{id}/memberships` → `MembershipService` → `ShopifyCheckoutUrlBuilder` generates a Shopify checkout URL for the selected selling plan → persists `Membership` with status `PENDING` + `checkoutUrl` → member is redirected to Shopify checkout.

**Payment confirmation:**
Shopify webhook → `ShopifyWebhookController` → membership status updated to `ACTIVE`, `nextBillingDate` set.

### API surface

Customer (`/api/`): `GET /api/subscriptions`, `POST /api/auth/register`, `POST /api/auth/lookup`, `POST|GET|DELETE /api/members/{id}/memberships`

Admin (`/api/admin/`): `GET /api/admin/dashboard`, `GET|POST /api/admin/subscriptions`, `GET /api/admin/members`, `GET /api/admin/memberships`, `PATCH /api/admin/memberships/{id}/cancel`

### Frontend

- `static/index.html` — customer portal (register/login by email, browse plans, checkout, view memberships)
- `static/admin.html` — admin dashboard (stats, subscription management, member/membership tables)
- `static/js/api.js` — shared `get/post/put/patch/del` wrappers over `fetch`; errors are thrown from non-OK responses using `detail` or `title` fields from the JSON body.

Member identity is stored in `localStorage` (no session/JWT). The `SpaFallbackInterceptor` ensures unknown routes fall through to `index.html`.

### Testing

`TestcontainersConfiguration` spins up a `mongo:latest` Docker container wired via `@ServiceConnection` — no manual MongoDB setup needed when running tests. `TestMembershipFlowApplication` reuses this for local development without a real MongoDB.

### Shopify API reference

GraphQL API version: **2026-04**. See `FLOW.md` for the full Admin integration flow including mutation signatures and required OAuth scopes (`write_products` + `write_own_subscription_contracts` or `write_purchase_options`).
