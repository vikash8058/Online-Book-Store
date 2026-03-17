## UC3 — Order And User Enhancements

### What Was Added
- Stock validation and deduction on order creation
- Stock restoration on order cancellation
- Order status update with transition validation
- Duplicate email check on user registration
- Get all orders by a specific user
- Search books by author name

---

## 1. Stock Management

### How Stock Works Now
```
ORDER CREATION:
Book stock = 10, Requested quantity = 3
        ↓
Check → 10 >= 3 ✅ enough stock
        ↓
Deduct → 10 - 3 = 7
        ↓
Book stock saved = 7
        ↓
Order created successfully ✅

ORDER CANCELLATION:
Book stock = 7, Order quantity = 3
        ↓
Status updated to CANCELLED
        ↓
Restore → 7 + 3 = 10
        ↓
Book stock saved = 10 ✅
```

### Error Response — Insufficient Stock
```
POST /api/orders/1
Body: { "items": [{ "bookId": 1, "quantity": 50 }] }

Response 400 Bad Request:
{
  "message": "Insufficient stock for book: Clean Code. Available: 8, Requested: 50",
  "statusCode": 400,
  "timestamp": "2024-01-15T10:30:00"
}
```

### Why @Transactional
```
Without @Transactional:
✅ Book 1 stock deducted and saved
✅ Book 2 stock deducted and saved
❌ Book 3 — InsufficientStockException thrown
→ Book 1 and Book 2 stock already changed
→ Order never created
→ DATA INCONSISTENT ❌

With @Transactional:
❌ Book 3 — InsufficientStockException thrown
→ Entire operation rolls back automatically
→ All stock restored to original values
→ Order not saved
→ DATA CONSISTENT ✅
```

### Files Changed — Stock Management

| File | Change |
|---|---|
| `OrderService.java` | Stock check + deduction + `@Transactional` |
| `InsufficientStockException.java` | New custom exception |
| `GlobalExceptionHandler.java` | Handler for `InsufficientStockException` |

---

## 2. Order Status Update

### New Endpoint
```
PATCH /api/orders/{id}/status
```

Request Body:
```json
{
  "status": "CONFIRMED"
}
```

Response `200 OK`:
```json
{
  "id": 1,
  "orderDate": "2024-01-15T10:30:00",
  "totalAmount": 89.97,
  "status": "CONFIRMED",
  "userId": 1,
  "items": [...]
}
```

### Valid Status Transitions
```
PENDING   → CONFIRMED ✅
PENDING   → CANCELLED ✅ (stock restored)
CONFIRMED → SHIPPED   ✅
CONFIRMED → CANCELLED ✅ (stock restored)
SHIPPED   → DELIVERED ✅
SHIPPED   → CANCELLED ✅ (stock restored)

DELIVERED → anything  ❌ order already delivered
CANCELLED → anything  ❌ order already cancelled
anything  → PENDING   ❌ cannot go back to pending
```

### Error Response — Invalid Transition
```
PATCH /api/orders/1/status
Body: { "status": "PENDING" }

Response 400 Bad Request:
{
  "message": "Cannot move order back to PENDING",
  "statusCode": 400,
  "timestamp": "2024-01-15T10:30:00"
}
```
```
PATCH /api/orders/1/status  (already DELIVERED)
Body: { "status": "SHIPPED" }

Response 400 Bad Request:
{
  "message": "Cannot update status of a DELIVERED order",
  "statusCode": 400,
  "timestamp": "2024-01-15T10:30:00"
}
```
```
PATCH /api/orders/1/status  (already CANCELLED)
Body: { "status": "CONFIRMED" }

Response 400 Bad Request:
{
  "message": "Cannot update status of a CANCELLED order",
  "statusCode": 400,
  "timestamp": "2024-01-15T10:30:00"
}
```

### OrderStatus Enum
```
PENDING     → Order just placed
CONFIRMED   → Order confirmed
SHIPPED     → Order shipped
DELIVERED   → Order delivered to customer
CANCELLED   → Order cancelled (stock restored automatically)
```

### Files Changed — Order Status Update

| File | Change |
|---|---|
| `OrderService.java` | Added `updateOrderStatus()` with transition validation and stock restoration |
| `OrderController.java` | Added `PATCH /{id}/status` endpoint |
| `OrderStatusUpdateRequest.java` | New DTO for status update request |
| `InvalidOrderStatusException.java` | New custom exception |
| `GlobalExceptionHandler.java` | Handler for `InvalidOrderStatusException` |

---

## 3. User Fixes — Duplicate Email Check

### Problem Before UC3
```
POST /api/users/register (same email twice)

Before UC3:
→ Raw database constraint error (ugly, exposes internals) ❌

After UC3:
→ Clean structured JSON error response ✅
```

### Error Response — Duplicate Email
```
POST /api/users/register
Body: { "name": "John", "email": "john@example.com", "password": "pass123" }
(email already registered)

Response 409 Conflict:
{
  "message": "Email already registered: john@example.com",
  "statusCode": 409,
  "timestamp": "2024-01-15T10:30:00"
}
```

### Files Changed — User Fixes

| File | Change |
|---|---|
| `UserService.java` | Added duplicate email check before saving |
| `DuplicateEmailException.java` | New custom exception |
| `GlobalExceptionHandler.java` | Handler for `DuplicateEmailException` with `409 Conflict` |

---

## 4. Extra Query Endpoints

### Get All Orders by User
```
GET /api/orders/user/{userId}
```

Response `200 OK`:
```json
[
  {
    "id": 1,
    "orderDate": "2024-01-15T10:30:00",
    "totalAmount": 89.97,
    "status": "PENDING",
    "userId": 1,
    "items": [...]
  },
  {
    "id": 2,
    "orderDate": "2024-01-16T11:00:00",
    "totalAmount": 49.99,
    "status": "CONFIRMED",
    "userId": 1,
    "items": [...]
  }
]
```

Error — User Not Found:
```json
{
  "message": "User not found with id: 999",
  "statusCode": 404,
  "timestamp": "2024-01-15T10:30:00"
}
```

---

### Search Books by Author
```
GET /api/books/author?authorName=Robert C. Martin
```

Response `200 OK`:
```json
[
  {
    "id": 1,
    "title": "Clean Code",
    "authorName": "Robert C. Martin",
    "price": 29.99,
    "stock": 45
  },
  {
    "id": 4,
    "title": "The Clean Coder",
    "authorName": "Robert C. Martin",
    "price": 34.99,
    "stock": 20
  }
]
```

### Files Changed — Extra Query Endpoints

| File | Change |
|---|---|
| `OrderRepository.java` | Added `findByUserId(Long userId)` |
| `OrderService.java` | Added `getOrdersByUser()` method |
| `OrderController.java` | Added `GET /user/{userId}` endpoint |
| `BookRepository.java` | Added `findByAuthorName(String authorName)` |
| `BookService.java` | Added `searchBookByAuthor()` method |
| `BookController.java` | Added `GET /author` endpoint |

---

## Complete List of New Exceptions Added in UC3

| Exception | Status | Trigger |
|---|---|---|
| `InsufficientStockException` | 400 | Stock less than requested quantity |
| `InvalidOrderStatusException` | 400 | Invalid order status transition |
| `DuplicateEmailException` | 409 | Email already registered |

---

## Updated API Endpoints After UC3

### Book APIs

| Method  | Endpoint                           | Description           | UC  |
|---------|------------------------------------|-----------------------|-----|
| `POST`  | `/api/books`                       | Create book           | UC1 |
| `GET`   | `/api/books`                       | Get all books         | UC1 |
| `GET`   | `/api/books/{id}`                  | Get book by ID        | UC1 |
| `PUT`   | `/api/books/{id}`                  | Full update           | UC1 |
| `PATCH` | `/api/books/{id}`                  | Partial update        | UC1 |
| `DELETE`| `/api/books/{id}`                  | Delete book           | UC1 |
| `GET`   | `/api/books/search?title`          | Search by title       | UC1 |
| `GET`   | `/api/books/author?authorName`     | Search by author      | UC3 |

### User APIs

| Method   | Endpoint              | Description              | UC  |
|----------|-----------------------|--------------------------|-----|
| `POST`   | `/api/users/register` | Register user            | UC1 |
| `GET`    | `/api/users`          | Get all users            | UC1 |
| `GET`    | `/api/users/{id}`     | Get user by ID           | UC1 |
| `DELETE` | `/api/users/{id}`     | Delete user              | UC1 |

### Order APIs

| Method   | Endpoint                      | Description              | UC  |
|----------|-------------------------------|--------------------------|-----|
| `POST`   | `/api/orders/{userId}`        | Create order             | UC1 |
| `GET`    | `/api/orders`                 | Get all orders           | UC1 |
| `GET`    | `/api/orders/{id}`            | Get order by ID          | UC1 |
| `DELETE` | `/api/orders/{id}`            | Delete order             | UC1 |
| `PATCH`  | `/api/orders/{id}/status`     | Update order status      | UC3 |
| `GET`    | `/api/orders/user/{userId}`   | Get orders by user       | UC3 |

---

## Postman Testing — UC3

### Stock Management Tests
```
✅ Create order with sufficient stock
POST http://localhost:8080/api/orders/1
Body: { "items": [{ "bookId": 1, "quantity": 2 }] }
Expected: 201 Created — check book stock decreased by 2

✅ Create order with insufficient stock
POST http://localhost:8080/api/orders/1
Body: { "items": [{ "bookId": 1, "quantity": 999 }] }
Expected: 400 Bad Request — insufficient stock message
```

### Order Status Update Tests
```
✅ Valid status update
PATCH http://localhost:8080/api/orders/1/status
Body: { "status": "CONFIRMED" }
Expected: 200 OK — status updated to CONFIRMED

✅ Invalid — back to PENDING
PATCH http://localhost:8080/api/orders/1/status
Body: { "status": "PENDING" }
Expected: 400 Bad Request

✅ Invalid — update DELIVERED order
PATCH http://localhost:8080/api/orders/1/status  (already DELIVERED)
Body: { "status": "SHIPPED" }
Expected: 400 Bad Request

✅ Cancel order and check stock restored
PATCH http://localhost:8080/api/orders/1/status
Body: { "status": "CANCELLED" }
Expected: 200 OK — check book stock increased back
```

### User Fixes Tests
```
✅ Register with new email
POST http://localhost:8080/api/users/register
Body: { "name": "John", "email": "john@example.com", "password": "pass123" }
Expected: 201 Created

✅ Register with same email again
POST http://localhost:8080/api/users/register
Body: { "name": "John", "email": "john@example.com", "password": "pass123" }
Expected: 409 Conflict — email already registered
```

### Extra Query Endpoints Tests
```
✅ Get orders by user
GET http://localhost:8080/api/orders/user/1
Expected: 200 OK — list of orders for user 1

✅ Get orders by invalid user
GET http://localhost:8080/api/orders/user/999
Expected: 404 Not Found

✅ Search books by author
GET http://localhost:8080/api/books/author?authorName=Robert C. Martin
Expected: 200 OK — list of books by that author
```

---

## Complete Testing Checklist — UC3
```
STOCK MANAGEMENT
□ POST /api/orders/1 (valid stock)       → order created, stock decremented
□ POST /api/orders/1 (invalid stock)     → 400 insufficient stock

ORDER STATUS UPDATE
□ PATCH /api/orders/1/status CONFIRMED   → 200 status updated
□ PATCH /api/orders/1/status SHIPPED     → 200 status updated
□ PATCH /api/orders/1/status DELIVERED   → 200 status updated
□ PATCH /api/orders/1/status PENDING     → 400 invalid transition
□ PATCH /api/orders/1/status (DELIVERED) → 400 cannot update delivered
□ PATCH /api/orders/1/status CANCELLED   → 200 cancelled + stock restored

USER FIXES
□ POST /api/users/register (new email)   → 201 created
□ POST /api/users/register (same email)  → 409 conflict

EXTRA ENDPOINTS
□ GET /api/orders/user/1                 → 200 list of orders
□ GET /api/orders/user/999               → 404 user not found
□ GET /api/books/author?authorName=...   → 200 list of books
```

---

## Files Changed Summary — UC3

| File | Type | Change |
|---|---|---|
| `OrderService.java` | Modified | Stock check, deduction, restoration, status update |
| `OrderController.java` | Modified | Added status update + get orders by user endpoints |
| `OrderRepository.java` | Modified | Added `findByUserId()` |
| `UserService.java` | Modified | Added duplicate email check |
| `BookService.java` | Modified | Added `searchBookByAuthor()` |
| `BookController.java` | Modified | Added search by author endpoint |
| `BookRepository.java` | Modified | Added `findByAuthorName()` |
| `OrderStatusUpdateRequest.java` | New | DTO for status update |
| `InsufficientStockException.java` | New | Custom exception |
| `InvalidOrderStatusException.java` | New | Custom exception |
| `DuplicateEmailException.java` | New | Custom exception |
| `GlobalExceptionHandler.java` | Modified | Handlers for 3 new exceptions |

---
