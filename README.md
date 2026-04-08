# UC12 — Persistent Cart Management

## Branch
`feature/UC12-cart-management`

## Overview
Implements a server-side persistent cart stored in MySQL. The cart is tied to the logged-in user via JWT token, survives logout/login, and auto-clears after an order is placed.

---

## Why Server-Side Cart?

| Approach | Survives Logout? | Multi-Device? | Production-Ready? |
|---|---|---|---|
| React state (useState) | ❌ | ❌ | ❌ |
| localStorage | ✅ (same browser) | ❌ | ❌ |
| **Database (UC12)** | **✅** | **✅** | **✅** |

This is the same approach used by Amazon, Flipkart, and all major e-commerce platforms.

---

## Database Tables Added

### `carts`
| Column | Type | Notes |
|---|---|---|
| id | BIGINT (PK) | Auto-generated |
| user_id | BIGINT (FK) | References `users.id` — unique (one cart per user) |
| created_at | DATETIME | Auto-set on creation |

### `cart_items`
| Column | Type | Notes |
|---|---|---|
| id | BIGINT (PK) | Auto-generated |
| cart_id | BIGINT (FK) | References `carts.id` |
| book_id | BIGINT (FK) | References `books.id` |
| quantity | INT | Min: 1 |
| added_at | DATETIME | Auto-set on creation |

---

## New Files

```
src/main/java/com/bookstore/
├── model/
│   ├── Cart.java
│   └── CartItem.java
├── repository/
│   ├── CartRepository.java
│   └── CartItemRepository.java
├── dto/
│   ├── request/
│   │   ├── AddToCartRequest.java
│   │   └── UpdateCartItemRequest.java
│   └── response/
│       ├── CartItemResponse.java
│       └── CartResponse.java
├── exception/
│   └── CartItemNotFoundException.java
├── service/
│   └── CartService.java
└── controller/
    └── CartController.java
```

## Modified Files

| File | Change |
|---|---|
| `GlobalExceptionHandler.java` | Added handler for `CartItemNotFoundException` |
| `SecurityConfig.java` | Added `/api/cart/**` → `ROLE_CUSTOMER` only |
| `OrderService.java` | Added `cartService.clearCart()` after order is placed |

---

## API Endpoints

All endpoints require `Authorization: Bearer <JWT_TOKEN>` (CUSTOMER role).

| Method | URL | Description |
|---|---|---|
| POST | `/api/cart/add` | Add book to cart (creates cart if first time) |
| GET | `/api/cart` | View current user's cart |
| PATCH | `/api/cart/update/{cartItemId}` | Update item quantity |
| DELETE | `/api/cart/remove/{cartItemId}` | Remove one item from cart |
| DELETE | `/api/cart/clear` | Clear all items from cart |

---

## Request / Response Examples

### POST /api/cart/add

**Request Body:**
```json
{
  "bookId": 1,
  "quantity": 2
}
```

**Response:**
```json
{
  "cartId": 1,
  "items": [
    {
      "cartItemId": 1,
      "bookId": 1,
      "bookTitle": "Clean Code",
      "authorName": "Robert C. Martin",
      "price": 499.0,
      "quantity": 2,
      "itemTotal": 998.0
    }
  ],
  "totalItems": 1,
  "grandTotal": 998.0
}
```

### GET /api/cart

Returns the same `CartResponse` structure as above.

### PATCH /api/cart/update/{cartItemId}

**Request Body:**
```json
{
  "quantity": 5
}
```

### DELETE /api/cart/remove/{cartItemId}

No request body. Returns updated `CartResponse`.

### DELETE /api/cart/clear

**Response:**
```
Cart cleared successfully.
```

---

## Key Design Decisions

**Cart auto-created on first add** — no separate "create cart" API needed. `getOrCreateCart()` handles it internally.

**Duplicate book → quantity increase** — adding the same book twice doesn't create a duplicate row. `findByCartAndBook()` checks first.

**JWT-based ownership** — cart owner is read from the JWT token via `SecurityContextHolder`. No userId in URL.

**Security check on update/remove** — the service verifies the cart item belongs to the currently logged-in user before allowing modification.

**Auto-clear on order** — `OrderService.createOrder()` calls `cartService.clearCart()` after saving the order.

---

## How to Test

Import `UC12-Cart-Postman-Collection.json` into Postman.

Run in this order:
1. Login → copy token → set as `{{token}}` in Postman environment
2. Add to Cart
3. View Cart
4. Update Cart Item
5. Remove Cart Item
6. Clear Cart

---
