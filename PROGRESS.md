# Admin Flow — Progress Summary

Shopify subscription membership admin flow built on Spring Boot 4 / Java 25 / Vue 3 CDN.

---

## Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 4.0.6, Java 25, Spring WebMVC |
| HTTP clients | Spring Framework 7 `@ImportHttpServices` declarative HTTP service registry |
| Shopify API | Admin GraphQL 2026-04 |
| Frontend | Vue 3 (CDN, Options API) — single `index.html`, no build step |
| Config | `application.yaml` + env vars |

---

## Configuration

```yaml
shopify:
  store-domain: ${SHOPIFY_STORE_DOMAIN:helinhair-2.myshopify.com}
  client-id: ${SHOPIFY_CLIENT_ID:placeholder-client-id}
  client-secret: ${SHOPIFY_CLIENT_SECRET:placeholder-client-secret}
  api-version: 2026-04
```

Run with:
```bash
# External MongoDB
./mvnw spring-boot:run

# Testcontainers-managed MongoDB (requires Docker, no external MongoDB)
./mvnw spring-boot:run -Dspring-boot.run.mainClass=com.example.membership_flow.TestMembershipFlowApplication
```

Admin UI → `http://localhost:8080`

---

## Shopify Auth — Dynamic Token

`ShopifyTokenService` obtains access tokens dynamically from `clientId` + `clientSecret` via Shopify's OAuth token endpoint. Tokens are cached with double-checked locking:

- `client_credentials` grant on first fetch
- `refresh_token` grant if token is expiring and a refresh token is available
- 30-second expiry buffer before proactive refresh
- 86400-second fallback TTL for non-expiring tokens

Two HTTP service groups are registered via `@ImportHttpServices`:

| Group | Interface | Purpose |
|---|---|---|
| `shopify` | `ShopifyAdminClient` | Admin GraphQL API — all mutations/queries |
| `shopify-auth` | `ShopifyTokenClient` | Token endpoint (`/admin/oauth/access_token`) |

`ShopifyClientConfig` uses `@Lazy @Autowired ShopifyTokenService` on the admin configurer bean to break the init-order cycle, injecting the token header dynamically at request time via `requestInterceptor`.

---

## API Endpoints (`/api/admin/`)

| Method | Path | Description |
|---|---|---|
| `GET` | `/selling-groups` | List all selling plan groups with their plans |
| `POST` | `/create-selling-group` | Create a group with one or more selling plans |
| `POST` | `/selling-groups/add-plan` | Add selling plan(s) to an existing group |
| `GET` | `/selling-groups/products?groupId=` | List products linked to a group (with variant IDs) |
| `POST` | `/link-product-to-plan` | Link products to a selling plan group |
| `POST` | `/remove-products-from-plan` | Remove products from a selling plan group |
| `GET` | `/products` | List all Shopify products (paginated, all pages) with variant IDs |
| `POST` | `/checkout-url` | Build a test cart checkout URL for a variant + selling plan |
| `GET` | `/subscription-contracts` | List subscription contracts from Shopify (most recent 20) |

### Key design notes

- Product actions operate at the **selling plan group** level — Shopify has no plan-level product attachment
- `groupId` is passed as `@RequestParam` (not `@PathVariable`) because Shopify GIDs (`gid://shopify/...`) contain slashes
- `GET /products` uses cursor-based pagination (`first: 250, after: $cursor`) to fetch all products regardless of store size
- `POST /checkout-url` constructs `https://{store-domain}/cart/add?id={variantNumericId}&quantity=1&selling_plan={planNumericId}` — the `/cart/add` format properly attaches the selling plan; the legacy `/cart/{id}:qty?selling_plan=` format does not
- `GET /subscription-contracts` is a diagnostic endpoint to confirm subscription contracts are created after checkout

---

## Shopify GraphQL Operations

| Constant | Operation | Purpose |
|---|---|---|
| `LIST_SELLING_PLAN_GROUPS` | query | Fetch all groups with plans, billing policy, pricing policy, product count |
| `GET_SELLING_PLAN_GROUP_PRODUCTS` | query | Fetch products (with first variant ID) linked to a group |
| `LIST_PRODUCTS` | query | Paginated fetch of all store products with first variant ID |
| `LIST_SUBSCRIPTION_CONTRACTS` | query | Fetch most recent 20 contracts with customer, lines, billing policy, orders |
| `CREATE_SELLING_PLAN_GROUP` | mutation | Create group + one or more selling plans in one call |
| `UPDATE_SELLING_PLAN_GROUP` | mutation | Add selling plans to an existing group |
| `ADD_PRODUCTS_TO_GROUP` | mutation | Link products to a group |
| `REMOVE_PRODUCTS_FROM_GROUP` | mutation | Unlink products from a group |

GraphQL inline fragments (`... on SellingPlanRecurringBillingPolicy`, `... on SellingPlanFixedPricingPolicy`) are handled by plain Java records with nullable fields — Shopify merges fragment fields directly into the JSON object so no custom deserializer is needed.

---

## DTOs (`admin/dto/`)

| DTO | Direction | Fields |
|---|---|---|
| `CreateSellingGroupRequest` | Request | `name`, `selling_plans: List<SellingPlanInput>` |
| `CreateSellingGroupResponse` | Response | `status`, `selling_plan_group_id`, `selling_plan_ids` |
| `AddSellingPlanRequest` | Request | `selling_plan_group_id`, `selling_plans: List<SellingPlanInput>` |
| `LinkProductRequest` | Request | `selling_plan_group_id`, `product_ids` |
| `RemoveProductRequest` | Request | `selling_plan_group_id`, `product_ids` |
| `LinkProductResponse` | Response | `status`, `message` |
| `SellingGroupsResponse` | Response | `sellingPlanGroups` list — each with id, name, merchantCode, productsCount, sellingPlans |
| `GroupProductsResponse` | Response | `groupId`, `groupName`, `total`, `products` (id, title, status, variant_id) |
| `ProductsResponse` | Response | `total`, `products` (id, title, status, variant_id) |
| `CheckoutUrlRequest` | Request | `variant_id`, `selling_plan_id` |
| `CheckoutUrlResponse` | Response | `checkout_url`, `variant_id`, `selling_plan_id` |
| `SubscriptionContractsResponse` | Response | `total`, `contracts` list — each with id, status, next_billing_date, created_at, customer, billing, lines, orders |

`SellingPlanInput` (nested in Create/Add requests): `interval`, `interval_count`, `discount_percentage`

---

## Error Handling

`GlobalExceptionHandler` maps exceptions to RFC 9457 `ProblemDetail`:

| Exception | HTTP status |
|---|---|
| `ShopifyUserErrorException` | 422 — includes Shopify `userErrors` list |
| `RestClientResponseException` | Proxied from Shopify (4xx/5xx) |
| `Exception` | 500 |

Frontend `js/api.js` reads `err.detail` or `err.title` from the JSON body to display error messages.

---

## Frontend (`static/index.html`)

Single-page Vue 3 Options API admin dashboard. All state is in-memory — no localStorage, no router.

### Layout

```
Header
├── Selling Plan Groups section
│   ├── [↺ Refresh]  [+ New Group]
│   ├── Group accordion rows
│   │   ├── Header: name · merchantCode · products count · plan chips
│   │   └── Expanded body (on click):
│   │       ├── IDs Reference Bar (dark strip)
│   │       │   ├── Group ID                    [copy]
│   │       │   └── Plan ID (one row per plan)  [copy]
│   │       ├── Selling Plans sub-section
│   │       │   ├── Plan cards (icon · name · short ID)
│   │       │   └── [+ Add Plan] → inline form (multi-plan, interval/count/discount grid)
│   │       └── Products sub-section
│   │           ├── Product rows (title · GID · variant ID · status badge · [🔗 Test] · [✕])
│   │           │   └── Test checkout panel (per plan: URL · [copy] · [Open ↗])
│   │           └── Product search combobox → [Link]
│   └── New Group form (dashed border, collapsible)
│       ├── Group name field
│       ├── Selling Plans builder (dynamic rows, [+ Add Plan])
│       └── [Create Group] → success panel with IDs + copy buttons
└── Subscription Contracts section
    ├── [↺ Refresh]
    └── Contract rows (customer · status · billing interval · next billing date)
        ├── Line items (product title · selling plan name · price · quantity)
        └── Associated orders (order name · date)
```

### Key frontend behaviours

- **Groups** are loaded on mount and after every mutation (silent background refresh preserves accordion state)
- **All Shopify products** are loaded once on mount for the combobox
- **Subscription contracts** are loaded on mount; refreshable manually
- **Per-group state** (`groupState` map keyed by group GID) tracks: open/closed, products list, loading flags, combobox search text + selection, add-plan form visibility, checkout URL cache
- **Product combobox** uses `<Teleport to="body">` + `position: fixed` via `getBoundingClientRect()` to escape `overflow: hidden` on the card container; filters by title, product GID, or variant GID
- **Test checkout panel** generates `/cart/add?id=...&quantity=1&selling_plan=...` URLs lazily on first open, cached per `productId + planId` key
- **Selling plan chips** on the header row show `interval · discount%` + short numeric ID
- **Copy flash toast** shown for 1.8 s after any clipboard write

---

## Checkout / Payment Flow

For manual e2e testing (Shopify test mode):

1. Expand a selling plan group
2. Note the **Plan ID** from the IDs bar
3. Link a product — its **Variant ID** appears in the product row
4. Click **🔗 Test** on a product row → checkout URLs generated per plan
5. Click **Open ↗** — adds item to cart with selling plan, redirects to cart page showing subscription consent text
6. Proceed to checkout and complete payment using a [Shopify test card](https://shopify.dev/docs/apps/payments/test-payments)
7. After payment, hit **↺ Refresh** on the Subscription Contracts section to confirm a contract was created

The two IDs needed for checkout: `variantId` + `sellingPlanId` (the group ID is only used on the admin side).

### Checkout URL format

```
# Correct — properly attaches selling plan when adding to cart
https://{store}/cart/add?id={variantNumericId}&quantity=1&selling_plan={planNumericId}

# Wrong — selling_plan query param is silently ignored on the /cart/ permalink
https://{store}/cart/{variantNumericId}:1?selling_plan={planNumericId}
```

### Prerequisites for subscriptions to work

- App must have **"Subscription app"** capability enabled in Shopify Partners Dashboard → App → Configuration
- Access token must include scopes: `write_purchase_options`, `read_purchase_options`, `write_own_subscription_contracts`, `read_own_subscription_contracts`
- Re-install or re-authorise the app after adding scopes

After a successful subscription checkout, Shopify fires `subscription_contracts/activate` webhook → `ShopifyWebhookController` updates membership status to `ACTIVE`.

---

## File Map

```
src/main/java/.../
├── admin/
│   ├── AdminController.java               REST endpoints (9 endpoints)
│   ├── AdminService.java                  Business logic + GraphQL strings
│   ├── ShopifyUserErrorException.java
│   └── dto/
│       ├── AddSellingPlanRequest.java
│       ├── CheckoutUrlRequest.java
│       ├── CheckoutUrlResponse.java
│       ├── CreateSellingGroupRequest.java
│       ├── CreateSellingGroupResponse.java
│       ├── GroupProductsResponse.java
│       ├── LinkProductRequest.java
│       ├── LinkProductResponse.java
│       ├── ProductsResponse.java
│       ├── RemoveProductRequest.java
│       ├── SellingGroupsResponse.java
│       └── SubscriptionContractsResponse.java
├── shopify/
│   ├── ShopifyAdminClient.java            HTTP service interface (8 methods)
│   ├── ShopifyClientConfig.java           @ImportHttpServices + token interceptor
│   ├── ShopifyProperties.java             Config record
│   ├── graphql/
│   │   ├── GraphQLRequest.java
│   │   ├── ProductsQueryResult.java
│   │   ├── SellingPlanGroupAddProductsResult.java
│   │   ├── SellingPlanGroupCreateResult.java
│   │   ├── SellingPlanGroupProductsQueryResult.java
│   │   ├── SellingPlanGroupRemoveProductsResult.java
│   │   ├── SellingPlanGroupsQueryResult.java
│   │   ├── SellingPlanGroupUpdateResult.java
│   │   ├── SubscriptionContractsQueryResult.java
│   │   └── UserError.java
│   └── token/
│       ├── ShopifyTokenClient.java
│       ├── ShopifyTokenRequest.java
│       ├── ShopifyTokenResponse.java
│       └── ShopifyTokenService.java
└── common/
    └── GlobalExceptionHandler.java

src/main/resources/
├── application.yaml
└── static/
    ├── index.html                         Admin SPA
    └── js/
        └── api.js                         fetch helpers (get/post/patch/del)
```
