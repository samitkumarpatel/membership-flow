# Custom Subscription App: Admin Integration Flow

This document details the end-to-end (E2E) technical data flow for the Custom Admin UI. The admin dashboard manages subscription configurations by acting as a JSON orchestration API wrapper on top of Shopify's GraphQL Admin API.

---

## 1. System Architecture Overview

```shell
[ Custom Admin UI Dashboard ]
│
(Sends Form JSON)
▼
[ API Wrapper ]
│
(Executes GraphQL Mutation)
▼
[ Shopify Admin Core Engine ]
```


---

## 2. Step-by-Step Data Flow

### Step 1: Subscription Plan Creation
1. **Admin Action:** The administrator opens the custom app dashboard and enters the desired configuration (e.g., Plan Name: *Monthly Coffee Subscription*, Billing Interval: *1 Month*, Discount: *10%*). Note - If there are already some sellingGroupPlan and sellingPlan available it can pull and show to the admin.
2. **UI Execution:** The Admin UI sends a `POST` request containing a JSON payload to the backend application wrapper.
    - **Endpoint:** `/api/admin/create-selling-group`
3. **Backend Transformation:** The backend validates the fields, translates the input variables into a Shopify GraphQL mutation payload, and forwards it to the Shopify Admin API using the store access token (`X-Shopify-Access-Token`).
4. **Shopify Execution:** Shopify registers the new transaction logic. It securely generates and returns unique identifiers for the objects:
    - `SellingPlanGroupId` (e.g., `gid://shopify/SellingPlanGroup/111`)
    - `SellingPlanId` (e.g., `gid://shopify/SellingPlan/222`)
5. **UI State Update:** The backend wrapper catches Shopify's JSON success response and returns the generated IDs back to the custom Admin UI layout.

### Step 2: Product Mapping (Linking Catalog Items)
1. **Admin Action:** Within the custom dashboard, If there are already product in shopify , it can pull and allow the manager to selects the newly created subscription plan and checks the catalog items or specific product variants that should offer this subscription option.
2. **UI Execution:** The dashboard submits a list of product target IDs to the wrapper api.
    - **Endpoint:** `/api/admin/link-product-to-plan`
3. **Backend Transformation:** The backend wrapper maps the payload parameters into the `sellingPlanGroupAddProducts` GraphQL mutation.
4. **Catalog Activation:** Shopify binds the subscription group explicitly to the specified products.
5. **Shopify Status Verification:** The items are now natively marked inside the store catalog database. If you navigate to the standard native Shopify Admin page for that product, the assigned plan will be displayed on the **Purchase Options** configuration card.

---

## 3. Core API Endpoints Reference

### Endpoint 1: Create Selling Plan Group
Creates the overarching billing intervals, delivery rules, and pricing adjustments.

* **Method:** `POST`
* **Path:** `/api/admin/create-selling-group`
* **Request JSON Payload:**
```json
{
  "name": "Monthly Coffee Subscription",
  "interval": "MONTH",
  "interval_count": 1,
  "discount_percentage": 10.0
}
```
* **Response JSON (Success):**
```json
{
  "status": "success",
  "selling_plan_group_id": "gid://shopify/SellingPlanGroup/111",
  "selling_plan_id": "gid://shopify/SellingPlan/222"
}
```

### Endpoint 2: Link Products to Plan Group
Binds the generated subscription logic parameters to specific catalog products.

* **Method:** `POST`
* **Path:** `/api/admin/link-product-to-plan`
* **Request JSON Payload:**
```json
{
  "selling_plan_group_id": "gid://shopify/SellingPlanGroup/111",
  "product_ids": [
    "gid://shopify/Product/8888888888",
    "gid://shopify/Product/9999999999"
  ]
}
```
* **Response JSON (Success):**
```json
{
  "status": "success",
  "message": "Linked to 2 products"
}
```

---

## 4. Underlying Shopify GraphQL Operations

The backend automatically formats and executes these native Shopify queries.

### Mutation A: `sellingPlanGroupCreate`

**Required scopes:** `write_products` + `write_own_subscription_contracts` or `write_purchase_options`

```graphql
mutation sellingPlanGroupCreate(\$input: SellingPlanGroupInput!, \$resources: SellingPlanGroupResourceInput) {
  sellingPlanGroupCreate(input: \$input, resources: \$resources) {
    sellingPlanGroup {
      id
      sellingPlans(first: 10) {
        edges {
          node {
            id
          }
        }
      }
    }
    userErrors {
      field
      message
    }
  }
}
```

> `$resources` is optional. Pass `productIds` or `productVariantIds` inside it to link products immediately at creation time, skipping the need for a separate Step 2 call.

### Mutation B: `sellingPlanGroupAddProducts`

**Required scopes:** `write_products` + `write_own_subscription_contracts` or `write_purchase_options`

```graphql
mutation sellingPlanGroupAddProducts(\$id: ID!, \$productIds: [ID!]!) {
  sellingPlanGroupAddProducts(id: \$id, productIds: \$productIds) {
    sellingPlanGroup {
      id
    }
    userErrors {
      field
      message
    }
  }
}
```

---
## 5. Next Architectural Step
Once your Admin Flow is deployed and your plans are linked to products, the customer-facing frontend can query these options natively via the **Shopify Storefront API** or parse them inside theme layouts using the `product.selling_plan_groups` Liquid collection loop.
