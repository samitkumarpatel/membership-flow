# Shopify Subscription Membership Platform — Build Instructions

This document describes every architectural decision, pattern, and component built in this codebase.
Use it as a blueprint to build a similar system in a new repository.

---

## Tech Stack

| Concern | Choice |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 4.0.6 |
| Web | Spring WebMVC (synchronous, servlet-based) |
| HTTP client | Spring RestClient (`@ImportHttpServices`) |
| Database | MongoDB (Spring Data) |
| JSON | Jackson 3 (`tools.jackson.databind`) |
| Resilience | Spring Framework 7 core (`@Retryable`, `@EnableResilientMethods`) |
| Scheduling | Spring `@Scheduled` + `@EnableScheduling` |
| Events | Spring `ApplicationEventPublisher` |
| Shopify API | Admin GraphQL API v2026-04 |
| Frontend | Vue 3 (CDN, no build step) |

> **Jackson 3 note**: Spring Boot 4 ships `tools.jackson.core:jackson-databind:3.x`.
> `ObjectMapper` is now `tools.jackson.databind.ObjectMapper`, but annotations
> (`@JsonProperty`, `@JsonIgnoreProperties`) stay in `com.fasterxml.jackson.annotation`
> for compatibility.

---

## Application Entry Point

```java
@SpringBootApplication
@EnableResilientMethods   // Spring Framework 7 — enables @Retryable
@EnableScheduling         // enables @Scheduled
public class Application {
    public static void main(String[] args) { SpringApplication.run(Application.class, args); }
}
```

---

## Configuration (`application.yaml`)

```yaml
spring:
  application:
    name: membership-flow
  mongodb:
    database: membership_flow
    # URI not set → connects to localhost:27017 by default

shopify:
  store-domain: ${SHOPIFY_STORE_DOMAIN}
  client-id: ${SHOPIFY_CLIENT_ID}
  client-secret: ${SHOPIFY_CLIENT_SECRET}   # also used for webhook HMAC verification
  api-version: 2026-04

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

Bind to a record with `@ConfigurationProperties`:

```java
@ConfigurationProperties(prefix = "shopify")
public record ShopifyProperties(
        String storeDomain, String clientId, String clientSecret, String apiVersion) {}
```

Enable in a `@Configuration` class: `@EnableConfigurationProperties(ShopifyProperties.class)`.

---

## Package Structure

```
com.example.{app}
├── admin/                      Admin API controllers, service, DTOs
│   ├── AdminController.java       /api/admin  (products, billing-attempts overview)
│   ├── SellingGroupController.java  /api/admin/selling-groups/**
│   ├── SubscriptionContractController.java  /api/admin/subscription-contracts/**
│   ├── AdminService.java          All Shopify GraphQL calls + MongoDB billing queries
│   └── dto/                       Request/response records
├── billing/                    Billing attempt tracking (MongoDB + scheduler)
│   ├── BillingAttemptRecord.java  MongoDB document
│   ├── BillingAttemptRepository.java
│   ├── BillingAttemptInfo.java    Shopify-side attempt DTO (for API responses)
│   ├── BillingAttemptSummary.java MongoDB-side attempt DTO (for admin dashboard)
│   └── BillingScheduler.java      Daily billing + dunning jobs
├── contract/                   Subscription contract lifecycle (MongoDB + events)
│   ├── SubscriptionContractRecord.java   MongoDB document
│   ├── SubscriptionContractRepository.java
│   ├── SubscriptionContractWebhookService.java  Upsert + event publish
│   └── event/
│       ├── SubscriptionCreatedEvent.java
│       ├── SubscriptionUpdatedEvent.java
│       └── SubscriptionContractEventListener.java  Placeholder for email/QR
├── common/
│   └── GlobalExceptionHandler.java
├── customer/                   Customer-facing API
│   ├── CustomerSubscriptionController.java  /api/customer/**
│   ├── CustomerSubscriptionService.java
│   └── dto/
├── shopify/                    Shopify API integration
│   ├── ShopifyAdminClient.java    HTTP interface (all GraphQL calls)
│   ├── ShopifyClientConfig.java   RestClient wiring + token injection
│   ├── ShopifyProperties.java
│   ├── token/                     OAuth token fetch + caching
│   └── graphql/                   One result record per GraphQL operation
├── subscription/               Public subscription product browsing
│   ├── SubscriptionController.java  /api/subscriptions/**
│   └── SubscriptionService.java
└── webhook/
    ├── ShopifyWebhookController.java  POST /webhooks/shopify
    └── ShopifyHmacVerifier.java       HMAC-SHA256 signature verification
```

---

## Shopify HTTP Client

Use Spring's declarative HTTP interface — one Java interface, one method per GraphQL operation.
All methods POST to the same `/graphql.json` endpoint; Spring deserializes each into its declared
return type.

```java
@ImportHttpServices(group = "shopify", types = ShopifyAdminClient.class)
@ImportHttpServices(group = "shopify-auth", types = ShopifyTokenClient.class)
@Configuration
@EnableConfigurationProperties(ShopifyProperties.class)
public class ShopifyClientConfig {

    @Bean
    RestClientHttpServiceGroupConfigurer shopifyAuthConfigurer(ShopifyProperties props) {
        return groups -> groups.filterByName("shopify-auth").forEachClient((_, builder) ->
                builder.baseUrl("https://" + props.storeDomain()));
    }

    @Bean
    RestClientHttpServiceGroupConfigurer shopifyAdminConfigurer(
            ShopifyProperties props,
            @Lazy @Autowired ShopifyTokenService tokenService) {
        return groups -> groups.filterByName("shopify").forEachClient((_, builder) ->
                builder.baseUrl("https://%s/admin/api/%s".formatted(props.storeDomain(), props.apiVersion()))
                       .requestInterceptor((request, body, execution) -> {
                           request.getHeaders().set("X-Shopify-Access-Token", tokenService.getAccessToken());
                           return execution.execute(request, body);
                       }));
    }
}
```

```java
public interface ShopifyAdminClient {
    @PostExchange("/graphql.json")
    SellingPlanGroupsQueryResult listSellingPlanGroups(@RequestBody GraphQLRequest request);

    @PostExchange("/graphql.json")
    SubscriptionContractsQueryResult listSubscriptionContracts(@RequestBody GraphQLRequest request);

    // ... one method per operation
}
```

GraphQL request wrapper:

```java
public record GraphQLRequest(String query, Object variables) {}
```

Each result is a plain Java record that mirrors the GraphQL response shape:

```java
public record SubscriptionContractsQueryResult(Data data) {
    public record Data(SubscriptionContractConnection subscriptionContracts) {}
    public record SubscriptionContractConnection(List<SubscriptionContractEdge> edges) {}
    public record SubscriptionContractEdge(SubscriptionContractNode node) {}
    public record SubscriptionContractNode(
            String id, String status, String nextBillingDate, String createdAt,
            Customer customer, SubscriptionLineConnection lines,
            SubscriptionBillingPolicy billingPolicy, OrderConnection orders,
            BillingAttemptConnection billingAttempts   // included in query
    ) {}
    // ... nested records follow the exact GraphQL shape
}
```

---

## Shopify OAuth Token (Client Credentials)

```java
// ShopifyTokenService — caches the token until it expires
public class ShopifyTokenService {
    private volatile CachedToken cachedToken;

    public String getAccessToken() {
        if (cachedToken == null || cachedToken.isExpired()) {
            var response = tokenClient.fetchToken(new ShopifyTokenRequest(
                    props.clientId(), props.clientSecret(), "client_credentials"));
            cachedToken = new CachedToken(response.accessToken(),
                    Instant.now().plusSeconds(response.expiresIn() - 60));
        }
        return cachedToken.token();
    }

    record CachedToken(String token, Instant expiresAt) {
        boolean isExpired() { return Instant.now().isAfter(expiresAt); }
    }
}
```

---

## Admin API — URI Design

All admin endpoints live under `/api/admin/**` so a single Spring Security matcher can protect them.

| Controller | Base path | Responsibility |
|---|---|---|
| `AdminController` | `/api/admin` | Products list, billing attempt overview |
| `SellingGroupController` | `/api/admin/selling-groups` | Selling plan group CRUD + plans + products |
| `SubscriptionContractController` | `/api/admin/subscription-contracts` | Contracts list, state transitions, billing attempts |

### Selling groups

| Method | Path | Action |
|---|---|---|
| `GET` | `/api/admin/selling-groups` | List all groups |
| `POST` | `/api/admin/selling-groups` | Create group |
| `PATCH` | `/api/admin/selling-groups` | Rename group (body: `{group_id, name}`) |
| `DELETE` | `/api/admin/selling-groups?groupId=` | Delete group |
| `GET` | `/api/admin/selling-groups/products?groupId=` | Products in group |
| `POST` | `/api/admin/selling-groups/products` | Link products |
| `DELETE` | `/api/admin/selling-groups/products` | Remove products (body with IDs) |
| `POST` | `/api/admin/selling-groups/plans` | Add selling plan |
| `PATCH` | `/api/admin/selling-groups/plans` | Update selling plan |
| `DELETE` | `/api/admin/selling-groups/plans` | Remove selling plan (body with IDs) |

> Shopify GIDs (e.g. `gid://shopify/SellingPlanGroup/123`) contain `/` characters which
> cannot be placed in URL path segments without encoded-slash issues on many servers.
> Keep GIDs in request bodies or query parameters.

### Subscription contracts

| Method | Path | Action |
|---|---|---|
| `GET` | `/api/admin/subscription-contracts` | List (includes `billingAttempts` from Shopify) |
| `PATCH` | `/api/admin/subscription-contracts/cancel` | Cancel (body: `{contract_id}`) |
| `PATCH` | `/api/admin/subscription-contracts/pause` | Pause |
| `PATCH` | `/api/admin/subscription-contracts/activate` | Activate |
| `POST` | `/api/admin/subscription-contracts/billing-attempts` | Trigger billing attempt |
| `GET` | `/api/admin/subscription-contracts/billing-attempts?attemptId=` | Poll attempt status |

### Billing attempts overview

| Method | Path | Action |
|---|---|---|
| `GET` | `/api/admin/billing-attempts` | All attempts from MongoDB (`?contractId=` `?status=`) |

---

## Customer API

| Method | Path | Action |
|---|---|---|
| `POST` | `/api/customer/subscriptions/lookup` | Find contracts by email or phone |
| `POST` | `/api/customer/subscriptions/pause?contractId=` | Pause a contract |
| `POST` | `/api/customer/subscriptions/resume?contractId=` | Resume a contract |
| `POST` | `/api/customer/subscriptions/cancel?contractId=` | Cancel a contract |

The `lookup` response embeds `billingAttempts` (live from Shopify GraphQL) inside each contract —
customers see payment history without a separate API call.

---

## Billing Attempt Lifecycle

### What a billing attempt is

When a subscription is due, you call Shopify's `subscriptionBillingAttemptCreate` mutation.
Shopify processes it asynchronously (`ready: false`), then fires a webhook with the result.
The response includes an `idempotencyKey` you control — use it to safely retry API-level failures.

### Idempotency key strategy

```
contractId (stripped) + "-" + LocalDate.now() + "-" + attemptNumber
```

- `attemptNumber = 1` for the daily job's first try
- `attemptNumber = 2, 3` for dunning retries
- The `@Retryable` wrapper retries with the **same** idempotency key (same call, safe to replay)
- Dunning retries use a different `attemptNumber` → new key → new Shopify billing attempt

### MongoDB document — `BillingAttemptRecord`

```java
@Document(collection = "billing_attempts")
public class BillingAttemptRecord {
    @Id String id;
    String contractId;        // Shopify GID
    @Indexed String attemptId; // Shopify GID — set after API call
    Status status;            // PENDING, SUCCESS, FAILED
    int attemptCount;
    LocalDate scheduledDate;  // billing cycle date
    Instant nextRetryAt;      // set on FAILED
    String errorCode;
    String errorMessage;
    Instant createdAt;
    Instant updatedAt;
    enum Status { PENDING, SUCCESS, FAILED }
}
```

### Spring `@Retryable` — transient API failures

Applied to `AdminService.createBillingAttempt`:

```java
@Retryable(
    includes = {HttpServerErrorException.class, ResourceAccessException.class},
    maxRetries = 3,
    delay = 500,
    multiplier = 2,
    maxDelay = 5000)
public BillingAttemptResponse createBillingAttempt(BillingAttemptRequest request) { ... }
```

Retries on Shopify 5xx or network failure, with exponential backoff (500 ms → 1 s → 2 s).
Does NOT retry on 4xx (user errors, bad request).

> `@Retryable` requires the method to be called through the Spring proxy — always inject the
> service bean, never call it from within the same class.

### Scheduler — two cron jobs

```java
@Component
public class BillingScheduler {

    // Midnight daily: bill contracts due today
    @Scheduled(cron = "0 0 0 * * *")
    public void processDailyBilling() {
        adminService.listSubscriptionContracts().contracts().stream()
                .filter(c -> "ACTIVE".equals(c.status()))
                .filter(c -> isDueToday(c.nextBillingDate(), LocalDate.now()))
                .filter(c -> !alreadyProcessed(c.id(), LocalDate.now()))
                .forEach(c -> executeAttempt(c.id(), LocalDate.now(), 1));
    }

    // 6am daily: dunning — retry FAILED attempts (up to 3 total)
    @Scheduled(cron = "0 0 6 * * *")
    public void processDunning() {
        billingAttemptRepository
            .findByStatusAndAttemptCountLessThanAndNextRetryAtBefore(FAILED, 3, Instant.now())
            .forEach(r -> executeAttempt(r.getContractId(), r.getScheduledDate(), r.getAttemptCount() + 1));
    }
}
```

`alreadyProcessed` checks MongoDB for an existing PENDING or SUCCESS record for that contract
on that `scheduledDate` — makes the daily job idempotent if restarted.

---

## Webhook Handler

Single endpoint `POST /webhooks/shopify` handles all topics.

### HMAC verification

Shopify signs every webhook:
```
X-Shopify-Hmac-Sha256: Base64( HMAC-SHA256( rawBody, clientSecret ) )
```

```java
@Component
public class ShopifyHmacVerifier {
    private final byte[] secretBytes;

    public boolean verify(String rawBody, String shopifyHmacHeader) {
        if (shopifyHmacHeader == null || shopifyHmacHeader.isBlank()) return false;
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretBytes, "HmacSHA256"));
            byte[] computed = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            byte[] expected = Base64.getDecoder().decode(shopifyHmacHeader);
            return MessageDigest.isEqual(computed, expected); // constant-time compare
        } catch (Exception e) { return false; }
    }
}
```

`MessageDigest.isEqual()` is mandatory — `String.equals()` short-circuits and leaks timing info.

### Controller dispatch

```java
@PostMapping
@ResponseStatus(HttpStatus.OK)
public void handleWebhook(
        @RequestHeader("X-Shopify-Topic") String topic,
        @RequestHeader(value = "X-Shopify-Hmac-Sha256", required = false) String hmacHeader,
        @RequestBody String rawBody) {          // raw String for HMAC + flexible deserialisation

    if (!hmacVerifier.verify(rawBody, hmacHeader))
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid HMAC signature");

    switch (topic) {
        case "subscription_billing_attempts/success" -> ...
        case "subscription_billing_attempts/failure" -> ...
        case "subscription_contracts/create"  -> contractWebhookService.onCreate(...)
        case "subscription_contracts/update"  -> contractWebhookService.onUpdate(...)
    }
}
```

Keep `@RequestBody String rawBody` (not a typed record) so the raw bytes are available for HMAC
and each topic can deserialise into its own strongly-typed record via `ObjectMapper`.

### Webhook topics to register in Shopify

| Topic | Action |
|---|---|
| `subscription_contracts/create` | New subscription purchased |
| `subscription_contracts/update` | Status changed (pause, cancel, etc.) |
| `subscription_billing_attempts/success` | Payment succeeded → mark SUCCESS |
| `subscription_billing_attempts/failure` | Payment failed → mark FAILED, schedule retry |

---

## Event-Driven Architecture (Subscription Lifecycle)

`SubscriptionContractWebhookService` saves to MongoDB **and** publishes a Spring event.
Downstream services listen without coupling to the webhook handler.

```
Shopify webhook
    → ShopifyWebhookController (verifies HMAC)
    → SubscriptionContractWebhookService
        → upsert SubscriptionContractRecord in MongoDB
        → ApplicationEventPublisher.publishEvent(SubscriptionCreatedEvent)
            → SubscriptionContractEventListener
                → (now) log
                → (future) send email, generate QR code, trigger workflow
```

Events:

```java
public record SubscriptionCreatedEvent(SubscriptionContractRecord contract) {}
public record SubscriptionUpdatedEvent(SubscriptionContractRecord contract, String previousStatus) {}
```

`previousStatus` lets listeners gate logic:
```java
@EventListener
public void onUpdated(SubscriptionUpdatedEvent event) {
    if ("ACTIVE".equals(event.previousStatus()) && "CANCELLED".equals(event.contract().getStatus())) {
        // send cancellation email
    }
}
```

To make handlers non-blocking (off the webhook thread):
1. Add `@EnableAsync` to the application class
2. Annotate individual `@EventListener` methods with `@Async`

---

## MongoDB Documents

### `billing_attempts` collection

Tracks every billing attempt your app fires, including dunning state.

```java
@Document("billing_attempts")
public class BillingAttemptRecord {
    @Id String id;
    String contractId;
    @Indexed String attemptId;   // unique index for webhook lookup
    Status status;
    int attemptCount;
    LocalDate scheduledDate;
    Instant nextRetryAt;
    String errorCode, errorMessage;
    Instant createdAt, updatedAt;
}
```

### `subscription_contracts` collection

Mirror of Shopify subscription contracts. Updated on every `subscription_contracts/update` webhook.

```java
@Document("subscription_contracts")
public class SubscriptionContractRecord {
    @Id String id;
    @Indexed(unique = true) String contractId;  // Shopify GID
    String customerId;                           // Shopify GID
    String status, currencyCode, nextBillingDate;
    BillingPolicy billingPolicy;
    List<ContractLine> lines;
    Instant shopifyCreatedAt, createdAt, updatedAt;
}
```

---

## GraphQL Patterns

### Query with nested `billingAttempts`

Add to the `subscriptionContracts` query to get live attempt status alongside each contract:

```graphql
billingAttempts(first: 10, reverse: true) {
  edges {
    node {
      id
      ready
      errorCode
      errorMessage
      createdAt
      order { id name }
    }
  }
}
```

### `subscriptionBillingAttemptCreate` mutation

```graphql
mutation subscriptionBillingAttemptCreate(
  $subscriptionContractId: ID!
  $subscriptionBillingAttemptInput: SubscriptionBillingAttemptInput!
) {
  subscriptionBillingAttemptCreate(
    subscriptionContractId: $subscriptionContractId
    subscriptionBillingAttemptInput: $subscriptionBillingAttemptInput
  ) {
    subscriptionBillingAttempt {
      id
      ready          # false initially — poll or use webhook
      errorCode
      errorMessage
      order { id name totalPriceSet { shopMoney { amount currencyCode } } }
    }
    userErrors { field message }
  }
}
```

Variables:
```json
{
  "subscriptionContractId": "gid://shopify/SubscriptionContract/123",
  "subscriptionBillingAttemptInput": { "idempotencyKey": "contract123-2026-05-21-1" }
}
```

### Polling a billing attempt

```graphql
query getBillingAttempt($id: ID!) {
  subscriptionBillingAttempt(id: $id) {
    id ready errorCode errorMessage
    order { id name }
  }
}
```

`ready: false` is normal — Shopify processes asynchronously. Use webhooks in production instead
of polling.

---

## Security Model

```
/api/admin/**     → require ADMIN role (Spring Security antMatcher)
/api/customer/**  → require authenticated user
/api/subscriptions/** → public (product browsing)
/webhooks/**      → public URL but protected by HMAC verification
```

---

## Frontend

Vue 3 loaded from CDN — no build step, served as static files from
`src/main/resources/static/`.

| File | Audience | APIs used |
|---|---|---|
| `membership.html` | Customer | `/api/subscriptions/products`, `/api/customer/**` |
| `index.html` | Admin | `/api/admin/**` |
| `js/api.js` | Shared | `get`, `post`, `patch`, `del` wrappers over `fetch` |

`del(path, body)` accepts an optional body for DELETE endpoints that carry a request body
(e.g. removing products or plans by ID list).

---

## Required Shopify OAuth Scopes

| Scope | Required for |
|---|---|
| `write_products` | Creating and attaching products to selling plan groups |
| `write_purchase_options` | Creating selling plan groups |
| `write_own_subscription_contracts` | Cancelling, pausing, activating contracts |
| `read_own_subscription_contracts` | Querying contracts and billing attempts |

---

## Local Development

```bash
# Run with Testcontainers-managed MongoDB (requires Docker — no external MongoDB needed)
./mvnw spring-boot:run -Dspring-boot.run.mainClass=com.example.TestApplication

# Run all tests
./mvnw test

# Build JAR
./mvnw package
```

`TestcontainersConfiguration` spins up `mongo:latest` via `@ServiceConnection`.

---

## Extending — Adding Email / QR Code

1. Create your service (e.g. `EmailService`, `QrCodeService`)
2. Inject it into `SubscriptionContractEventListener`
3. Add logic inside the existing `@EventListener` methods — no changes to the webhook handler
4. For async execution: add `@EnableAsync` to the application class, `@Async` to the listener method

```java
@EventListener
@Async          // add when ready — runs off the webhook thread
public void onCreated(SubscriptionCreatedEvent event) {
    emailService.sendWelcome(event.contract().getCustomerId());
    qrCodeService.generateMembershipCard(event.contract());
}
```
