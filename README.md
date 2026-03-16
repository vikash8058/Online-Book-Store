## UC2 — Stock Management

### What Was Added
- Stock validation before placing an order
- Stock deduction on order creation
- Stock restoration on order cancellation
- `InsufficientStockException` for clean error response
- `@Transactional` to ensure data consistency

---

### How Stock Works Now
```
ORDER CREATION:
Book stock = 10, Requested quantity = 3
        ↓
Check → 10 >= 3 ✅
        ↓
Deduct → 10 - 3 = 7
        ↓
Book stock saved = 7
        ↓
Order created ✅

ORDER CANCELLATION:
Book stock = 7, Order quantity = 3
        ↓
Status updated to CANCELLED
        ↓
Restore → 7 + 3 = 10
        ↓
Book stock saved = 10 ✅
```

---

### Error Response — Insufficient Stock
```
POST /api/orders/1
Body: { "items": [{ "bookId": 1, "quantity": 50 }] }

Response 400 Bad Request:
{
  "message": "Insufficient stock for book: Clean Code. Available stock: 8, Requested quantity: 50",
  "statusCode": 400,
  "timestamp": "2024-01-15T10:30:00"
}
```

---

### Why @Transactional
```
Without @Transactional:
✅ Book 1 stock deducted
✅ Book 2 stock deducted
❌ Book 3 — InsufficientStockException
→ Book 1 and Book 2 stock already changed
→ Order never created
→ DATA INCONSISTENT ❌

With @Transactional:
❌ Book 3 — InsufficientStockException
→ Entire operation rolls back
→ All stock restored automatically
→ DATA CONSISTENT ✅
```

---

### Files Changed

| File | Change |
|---|---|
| `OrderService.java` | Stock check + deduction + restoration + `@Transactional` |
| `InsufficientStockException.java` | New custom exception |
| `GlobalExceptionHandler.java` | Handler for `InsufficientStockException` |

---
