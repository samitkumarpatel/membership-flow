# Membership Flow — Progress Summary

Shopify subscription membership platform built on Spring Boot 4 / Java 25 / Vue 3 CDN.

---

## Stack

| Layer | Technology |
|---|---|
| Backend | Spring Boot 4.0.6, Java 25, Spring WebMVC |
| HTTP clients | Spring Framework 7 `@ImportHttpServices` declarative HTTP service registry |
| Shopify API | Admin GraphQL 2026-04 |
| Frontend | Vue 3 (CDN, Options API) — no build step |
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

- Admin UI → `http://localhost:8080`
- Customer portal → `http://localhost:8080/membership.html`

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

## API Endpoints

### Admin (`/api/admin/`)

| Method | Path | Description |
|---|---|---|
| `GET` | `/selling-groups` | List all selling plan groups with their plans |
| `POST` | `/create-selling-group` | Create a group with one or more selling plans |
| `POST` | `/selling-groups/add-plan` | Add selling plan(s) to an existing group |
| `POST` | `/selling-groups/remove-plan` | Remove selling plan(s) from a group |
| `POST` | `/selling-groups/update-plan` | Update an existing selling plan (interval, discount) |
| `POST` | `/selling-groups/update-name` | Rename a selling plan group |
| `DELETE` | `/selling-groups?groupId=` | Delete a selling plan group |
| `GET` | `/selling-groups/products?groupId=` | List products linked to a group (with variant IDs) |
| `POST` | `/link-product-to-plan` | Link products to a selling plan group |
| `POST` | `/remove-products-from-plan` | Remove products from a selling plan group |
| `GET` | `/products` | List all Shopify products (paginated, all pages) with variant IDs |
| `GET` | `/subscription-contracts` | List subscription contracts from Shopify (most recent 20) |
| `POST` | `/subscription-contracts/cancel` | Cancel a subscription contract (terminal) |
| `POST` | `/subscription-contracts/pause` | Pause a subscription contract (reversible) |
| `POST` | `/subscription-contracts/activate` | Resume a paused subscription contract |

### Customer (`/api/customer/`)

| Method | Path | Description |
|---|---|---|
| `POST` | `/subscriptions/lookup` | Find contracts by email or phone — body `{ email, phone }` |
| `POST` | `/subscriptions/pause?contractId=` | Pause a contract |
| `POST` | `/subscriptions/resume?contractId=` | Resume a paused contract |
| `POST` | `/subscriptions/cancel?contractId=` | Cancel a contract (terminal) |

### Subscription catalogue (`/api/subscriptions/`)

| Method | Path | Description |
|---|---|---|
| `GET` | `/products` | List products with their selling plan groups and checkout URLs |

### Key design notes

- `groupId` / `contractId` are passed as `@RequestParam` (not `@PathVariable`) because Shopify GIDs (`gid://shopify/...`) contain slashes that break path variable matching
- `GET /products` uses cursor-based pagination (`first: 250, after: $cursor`) to fetch all products regardless of store size
- `CANCELLED` is a terminal state in Shopify — no mutation can reactivate it; only `PAUSED` contracts can be resumed via `subscriptionContractActivate`
- Shopify does **not** auto-send emails for API-triggered status changes; email notifications require Shopify Flow or a custom webhook

---

## Shopify GraphQL Operations

| Constant | Operation | Purpose |
|---|---|---|
| `LIST_SELLING_PLAN_GROUPS` | query | Fetch all groups with plans, billing policy, pricing policy, product count |
| `GET_SELLING_PLAN_GROUP_PRODUCTS` | query | Fetch products (with first variant ID) linked to a group |
| `LIST_PRODUCTS` | query | Paginated fetch of all store products with first variant ID |
| `LIST_SUBSCRIPTION_CONTRACTS` | query | Fetch most recent 20 contracts with customer, lines, billing policy, orders |
| `CREATE_SELLING_PLAN_GROUP` | mutation | Create group + one or more selling plans in one call |
| `UPDATE_SELLING_PLAN_GROUP` | mutation | Add / update / delete selling plans or rename a group |
| `DELETE_SELLING_PLAN_GROUP` | mutation | Delete an entire selling plan group |
| `ADD_PRODUCTS_TO_GROUP` | mutation | Link products to a group |
| `REMOVE_PRODUCTS_FROM_GROUP` | mutation | Unlink products from a group (returns `removedProductIds`, not `sellingPlanGroup`) |
| `CANCEL_SUBSCRIPTION_CONTRACT` | mutation | Cancel a contract — terminal |
| `PAUSE_SUBSCRIPTION_CONTRACT` | mutation | Pause a contract — reversible |
| `ACTIVATE_SUBSCRIPTION_CONTRACT` | mutation | Resume a paused contract |
| `FIND_CUSTOMER_CONTRACTS` | query | Find a customer by email/phone and return their subscription contracts |

### Shopify API quirks / fixes applied

- `sellingPlanGroupRemoveProducts` payload in API 2026-04 returns `removedProductIds` (not `sellingPlanGroup { id }` — that field was removed)
- `UserError.field` is `[String!]` (JSON array), not `String` — Java record uses `List<String>`
- Deleting the last selling plan from a group is rejected by Shopify — the admin UI disables the Delete button when only one plan remains
- `sellingPlanGroupUpdate` is reused for add-plan, update-plan, remove-plan, and rename-group by varying which `input` sub-fields are populated

---

## DTOs

### `admin/dto/`

| DTO | Direction | Fields |
|---|---|---|
| `CreateSellingGroupRequest` | Request | `name`, `selling_plans: List<SellingPlanInput>` |
| `CreateSellingGroupResponse` | Response | `status`, `selling_plan_group_id`, `selling_plan_ids` |
| `AddSellingPlanRequest` | Request | `selling_plan_group_id`, `selling_plans: List<SellingPlanInput>` |
| `RemoveSellingPlanRequest` | Request | `selling_plan_group_id`, `selling_plan_ids` |
| `UpdateSellingPlanRequest` | Request | `selling_plan_group_id`, `selling_plan_id`, `interval`, `interval_count`, `discount_percentage` |
| `UpdateSellingGroupRequest` | Request | `group_id`, `name` |
| `LinkProductRequest` | Request | `selling_plan_group_id`, `product_ids` |
| `RemoveProductRequest` | Request | `selling_plan_group_id`, `product_ids` |
| `LinkProductResponse` | Response | `status`, `message` |
| `SellingGroupsResponse` | Response | list of groups — each with id, name, merchantCode, productsCount, sellingPlans |
| `GroupProductsResponse` | Response | `groupId`, `groupName`, `total`, `products` (id, title, status, variant_id) |
| `ProductsResponse` | Response | `total`, `products` (id, title, status, variant_id) |
| `SubscriptionContractsResponse` | Response | `total`, `contracts` — each with id, status, next_billing_date, customer, billing, lines, orders |
| `CancelContractRequest` | Request | `contract_id` |
| `PauseContractRequest` | Request | `contract_id` |
| `ActivateContractRequest` | Request | `contract_id` |

`SellingPlanInput` (nested): `interval`, `interval_count`, `discount_percentage`

### `customer/dto/`

| DTO | Direction | Fields |
|---|---|---|
| `CustomerLookupRequest` | Request | `email`, `phone` (either one required) |
| `CustomerSubscriptionsResponse` | Response | `customerId`, `email`, `firstName`, `lastName`, `count`, `contracts` |

`CustomerSubscriptionsResponse.ContractItem`: `id`, `status`, `nextBillingDate`, `createdAt`, `billing`, `lines`

---

## Error Handling

`GlobalExceptionHandler` maps exceptions to RFC 9457 `ProblemDetail`:

| Exception | HTTP status |
|---|---|
| `ShopifyUserErrorException` | 422 — includes Shopify `userErrors` list |
| `IllegalArgumentException` | 400 |
| `RestClientResponseException` | Proxied from Shopify (4xx/5xx) |
| `Exception` | 500 |

Frontend reads `err.detail` or `err.title` from the JSON body to display error messages.

---

## Admin UI (`static/index.html`)

Single-page Vue 3 Options API dashboard. All state is in-memory — no localStorage, no router.

### Layout

```
Header (dark navy)
└── Subscriptions section
    ├── [↺ Refresh]  [+ New Subscription]
    ├── Subscription accordion rows
    │   ├── Header: name · merchantCode · product count · frequency chips
    │   │           [✎ Edit] [Delete]
    │   ├── Inline edit form (name field, Save / Discard)
    │   └── Expanded body:
    │       ├── IDs Reference Bar (dark strip — Group ID + Plan IDs with copy buttons)
    │       ├── Frequencies sub-section
    │       │   ├── Plan cards (icon · name · short GID · [✎ Edit] [Delete])
    │       │   ├── Inline edit form (interval / count / discount, Save Changes / Discard)
    │       │   └── [+ Add Frequency] → inline multi-plan builder form
    │       ├── Products sub-section
    │       │   ├── Product rows (title · GID · variant ID · status badge · [✕ Remove])
    │       │   └── Product search combobox → [Link]
    │       └── Subscription Contracts sub-section (tabbed by frequency)
    │           ├── Tab per selling plan showing contract count
    │           └── Contract rows:
    │               customer name · email · status badge · billing interval · next billing date
    │               [Pause] (ACTIVE only) · [Resume] (PAUSED only) · [Cancel] (ACTIVE/PAUSED)
    │               Line items · Associated orders
    └── New Subscription form (collapsible, dashed border)
        ├── Subscription Name field
        ├── Frequencies builder (dynamic rows: interval / count / discount)
        └── [Create Subscription] → result panel with IDs + copy buttons

Unassigned Contracts section (only shown when contracts exist with no matching group)
```

### Key frontend behaviours

- **Per-group state** (`groupState` map keyed by group GID, each entry a `reactive()` object) tracks: open/closed, products, loading flags, combobox state, add-plan / edit-plan / edit-group-name form visibility, contract tab selection
- **Optimistic updates** — contract status (cancel/pause/resume) updates `this.contracts` immediately on API success; background `loadContracts()` syncs from Shopify to handle propagation delay
- **Product combobox** uses `<Teleport to="body">` + `position: fixed` via `getBoundingClientRect()` to escape `overflow: hidden`
- **`contractsByGroup`** computed groups contracts by matching `lines[].sellingPlanId` → selling plan → group; unmatched contracts go to `__unassigned__`
- **`contractsByPlan`** computed groups contracts by `selling_plan_id` for the tab view
- `groupId` is URL-encoded with `encodeURIComponent` before appending to `?groupId=` because GIDs contain slashes

---

## Customer Portal (`static/membership.html`)

Single-page Vue 3 Options API customer-facing portal with **two distinct views** controlled by `currentView` (`'plans'` | `'manage'`). Navigation tabs in the header switch between views; `switchToManage()` resets all lookup state on entry.

### View 1 — Browse Plans (`currentView === 'plans'`)

Loaded from `GET /api/subscriptions/products`.

```
Dark gradient hero (brand tagline)
Plan cards grid
  └── One card per selling plan (a product with 3 plans → 3 cards)
      Plan name · group tag · savings % · delivery description · price
      [Buy Now]  [Add to Cart / ✓ In Cart]
Cart button (top-right, hidden in manage view)
Cart drawer (slide-in) → multi-item checkout (opens first item's URL)
```

- **Buy Now** — navigates directly to the selling plan's Shopify checkout URL
- **Add to Cart** / **✓ In Cart — View Cart** — adds to a `localStorage` cart

### View 2 — My Subscriptions (`currentView === 'manage'`)

Completely separate screen with its own layout and no shared DOM with the plans view.

```
Dark gradient hero (#1a1a2e → #2d2b55)
  "My Subscriptions — Enter your email or phone to find your subscriptions"
Centred body (max-width: 760px)
  Lookup card
    ├── Email / Phone input + [Find My Subscriptions] button
    └── Results (after lookup)
        └── Per contract card:
            plan title · billing frequency · next billing date · status badge
            [Pause]  (ACTIVE only)
            [Resume] (PAUSED only)
            [Cancel] (ACTIVE or PAUSED)
            CANCELLED / EXPIRED: dimmed, no action buttons
```

- Lookup: `POST /api/customer/subscriptions/lookup` with `{ email, phone }`
- Pause: `POST /api/customer/subscriptions/pause?contractId=`
- Resume: `POST /api/customer/subscriptions/resume?contractId=`
- Cancel: `POST /api/customer/subscriptions/cancel?contractId=`
- Optimistic status update on action success + toast notification
- `switchToManage()` clears all lookup state so the form is always blank on entry

---

## Subscription Contract Lifecycle

```
ACTIVE  ──pause──►  PAUSED  ──resume──►  ACTIVE
ACTIVE  ──cancel──► CANCELLED  (terminal)
PAUSED  ──cancel──► CANCELLED  (terminal)
```

Email notifications for status changes are **not** triggered automatically by the Shopify API. They require Shopify Flow automation or a custom webhook + mailer (not yet implemented).

---

## Checkout / Payment Flow

1. Customer browses the membership portal and clicks **Buy Now** on a plan card
2. Redirected to Shopify checkout with the variant + selling plan pre-attached
3. Customer completes payment using a [Shopify test card](https://shopify.dev/docs/apps/payments/test-payments)
4. Shopify fires `subscription_contracts/activate` webhook → membership status updated to `ACTIVE`
5. Customer returns to membership portal, enters email/phone → sees their active contract

### Checkout URL format

```
# Correct — properly attaches selling plan when adding to cart
https://{store}/cart/add?id={variantNumericId}&quantity=1&selling_plan={planNumericId}

# Wrong — selling_plan query param is silently ignored on the /cart/ permalink
https://{store}/cart/{variantNumericId}:1?selling_plan={planNumericId}
```

### Prerequisites for subscriptions to work

- App must have **"Subscription app"** capability enabled in Shopify Partners Dashboard → App → Configuration
- Access token scopes required: `write_purchase_options`, `read_purchase_options`, `write_own_subscription_contracts`, `read_own_subscription_contracts`
- Re-install or re-authorise the app after adding scopes

---

## File Map

```
src/main/java/.../
├── admin/
│   ├── AdminController.java               REST endpoints (15 endpoints)
│   ├── AdminService.java                  Business logic + GraphQL mutation strings
│   ├── ShopifyUserErrorException.java
│   └── dto/
│       ├── ActivateContractRequest.java
│       ├── AddSellingPlanRequest.java
│       ├── CancelContractRequest.java
│       ├── CreateSellingGroupRequest.java
│       ├── CreateSellingGroupResponse.java
│       ├── GroupProductsResponse.java
│       ├── LinkProductRequest.java
│       ├── LinkProductResponse.java
│       ├── PauseContractRequest.java
│       ├── ProductsResponse.java
│       ├── RemoveProductRequest.java
│       ├── RemoveSellingPlanRequest.java
│       ├── SellingGroupsResponse.java
│       ├── SubscriptionContractsResponse.java
│       ├── UpdateSellingGroupRequest.java
│       └── UpdateSellingPlanRequest.java
├── customer/
│   ├── CustomerSubscriptionController.java  REST endpoints (4 endpoints)
│   ├── CustomerSubscriptionService.java     Lookup + delegates actions to AdminService
│   └── dto/
│       ├── CustomerLookupRequest.java
│       └── CustomerSubscriptionsResponse.java
├── shopify/
│   ├── ShopifyAdminClient.java            HTTP service interface (14 methods)
│   ├── ShopifyClientConfig.java           @ImportHttpServices + token interceptor
│   ├── ShopifyProperties.java             Config record
│   ├── graphql/
│   │   ├── CustomerSubscriptionContractsQueryResult.java
│   │   ├── GraphQLRequest.java
│   │   ├── ProductsQueryResult.java
│   │   ├── SellingPlanGroupAddProductsResult.java
│   │   ├── SellingPlanGroupCreateResult.java
│   │   ├── SellingPlanGroupDeleteResult.java
│   │   ├── SellingPlanGroupProductsQueryResult.java
│   │   ├── SellingPlanGroupRemoveProductsResult.java
│   │   ├── SellingPlanGroupsQueryResult.java
│   │   ├── SellingPlanGroupUpdateResult.java
│   │   ├── SubscriptionContractActivateResult.java
│   │   ├── SubscriptionContractCancelResult.java
│   │   ├── SubscriptionContractPauseResult.java
│   │   ├── SubscriptionContractsQueryResult.java
│   │   ├── SubscriptionProductsQueryResult.java
│   │   └── UserError.java
│   └── token/
│       ├── ShopifyTokenClient.java
│       ├── ShopifyTokenRequest.java
│       ├── ShopifyTokenResponse.java
│       └── ShopifyTokenService.java
├── subscription/
│   ├── SubscriptionController.java        GET /api/subscriptions/products
│   ├── SubscriptionProductsResponse.java
│   └── SubscriptionService.java
└── common/
    └── GlobalExceptionHandler.java

src/main/resources/
├── application.yaml
└── static/
    ├── index.html          Admin SPA (subscription management dashboard)
    ├── membership.html     Customer portal (browse plans + manage subscriptions)
    └── js/
        └── api.js          fetch helpers (get / post / patch / del)
```
