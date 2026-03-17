## UC4 — AOP Logging

### What is AOP
```
AOP = Aspect Oriented Programming

Problem Without AOP:
BookService  → business logic + logging mixed together
UserService  → business logic + logging mixed together
OrderService → business logic + logging mixed together
Same logging code repeated in every method ❌

Solution With AOP:
BookService  → ONLY business logic ✅
UserService  → ONLY business logic ✅
OrderService → ONLY business logic ✅
LoggingAspect → logging written ONCE, applied automatically ✅
```

### What Was Added
- AOP dependency in `pom.xml`
- `LoggingAspect.java` — one aspect class for all 3 services
- Logging on `createBook` method in `BookService`
- Logging on `registerUser` method in `UserService`
- Logging on `createOrder` method in `OrderService`
- No changes in any existing file

---

## Core AOP Concepts Used
```
@Aspect         → marks class as an Aspect (contains AOP logic)
@Component      → registers it as a Spring bean
@Pointcut       → defines WHICH method to intercept
@Before         → runs BEFORE method executes
@AfterReturning → runs ONLY when method returns successfully
@AfterThrowing  → runs ONLY when method throws exception
JoinPoint       → gives method name and class name
```

---

## Pointcuts Defined
```java
// targets ONLY createBook in BookService
execution(* com.bookstore.service.BookService.createBook(..))

// targets ONLY registerUser in UserService
execution(* com.bookstore.service.UserService.registerUser(..))

// targets ONLY createOrder in OrderService
execution(* com.bookstore.service.OrderService.createOrder(..))
```

---

## How AOP Works in This Project
```
You call any API
        ↓
Controller receives request
        ↓
Controller calls Service method
        ↓
AOP intercepts BEFORE service method runs
→ prints METHOD STARTED
        ↓
actual service method runs
        ↓
if success   → prints METHOD SUCCESS + returned value
if exception → prints METHOD FAILED  + exception message
        ↓
response goes back to client
```

---

## Console Output

### BookService — createBook

#### Success
```
POST /api/books
Body: { "title": "Clean Code", "authorName": "Robert C. Martin", "price": 29.99, "stock": 50 }

==============================================
BOOK SERVICE - METHOD STARTED : createBook
==============================================
BOOK SERVICE - METHOD SUCCESS : createBook
BOOK CREATED  : BookResponse(id=1, title=Clean Code, authorName=Robert C. Martin, price=29.99, stock=50)
```

#### Failure
```
POST /api/books
Body: { "title": "", "price": -10 }

==============================================
BOOK SERVICE - METHOD STARTED : createBook
==============================================
BOOK SERVICE - METHOD FAILED : createBook
EXCEPTION     : Title must not be blank
```

---

### UserService — registerUser

#### Success
```
POST /api/users/register
Body: { "name": "John Doe", "email": "john@example.com", "password": "secure123" }

==============================================
USER SERVICE - METHOD STARTED : registerUser
==============================================
USER SERVICE - METHOD SUCCESS : registerUser
USER CREATED  : UserResponse(id=1, name=John Doe, email=john@example.com, role=CUSTOMER)
```

#### Failure — Duplicate Email
```
POST /api/users/register
Body: { "name": "John Doe", "email": "john@example.com", "password": "secure123" }
(email already registered)

==============================================
USER SERVICE - METHOD STARTED : registerUser
==============================================
USER SERVICE - METHOD FAILED : registerUser
EXCEPTION     : Email already registered: john@example.com
```

---

### OrderService — createOrder

#### Success
```
POST /api/orders/1
Body: { "items": [{ "bookId": 1, "quantity": 2 }] }

==============================================
ORDER SERVICE - METHOD STARTED : createOrder
==============================================
ORDER SERVICE - METHOD SUCCESS : createOrder
ORDER CREATED : OrderResponse(id=1, totalAmount=59.98, status=PENDING, userId=1, ...)
```

#### Failure — Insufficient Stock
```
POST /api/orders/1
Body: { "items": [{ "bookId": 1, "quantity": 999 }] }

==============================================
ORDER SERVICE - METHOD STARTED : createOrder
==============================================
ORDER SERVICE - METHOD FAILED : createOrder
EXCEPTION     : Insufficient stock for book: Clean Code. Available: 8, Requested: 999
```

#### Failure — User Not Found
```
POST /api/orders/999
Body: { "items": [{ "bookId": 1, "quantity": 1 }] }

==============================================
ORDER SERVICE - METHOD STARTED : createOrder
==============================================
ORDER SERVICE - METHOD FAILED : createOrder
EXCEPTION     : User not found with id: 999
```

---

## Difference — Logging vs AOP
```
┌──────────────────────────────────────────────────┐
│                    LOGGING                       │
│  Tool to print messages                          │
│  Example: log.info(), log.error()                │
│  Written manually inside every method            │
└──────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────┐
│                      AOP                         │
│  Programming technique                           │
│  Intercepts methods automatically                │
│  Written once, applied everywhere                │
│  Business logic stays clean                      │
└──────────────────────────────────────────────────┘

Logging = WHAT you print
AOP     = HOW and WHERE you apply it
Both combined = clean and powerful ✅
```

---

## Methods Covered
```
BookService
└── createBook()    ← AOP logs this ✅

UserService
└── registerUser()  ← AOP logs this ✅

OrderService
└── createOrder()   ← AOP logs this ✅

All other methods   ← not intercepted (simple and clean)
```

---

## Files Changed Summary

| File | Type | Change |
|---|---|---|
| `pom.xml` | Modified | Added AOP dependency |
| `LoggingAspect.java` | New | Aspect class with logging for all 3 services |
| `BookService.java` | No Change | ❌ not touched |
| `UserService.java` | No Change | ❌ not touched |
| `OrderService.java` | No Change | ❌ not touched |

---

## Postman Testing — UC4
```
BOOK SERVICE LOGGING
□ POST /api/books (valid)    → check console — METHOD STARTED + METHOD SUCCESS
□ POST /api/books (invalid)  → check console — METHOD STARTED + METHOD FAILED

USER SERVICE LOGGING
□ POST /api/users/register (new email)    → check console — METHOD STARTED + METHOD SUCCESS
□ POST /api/users/register (same email)   → check console — METHOD STARTED + METHOD FAILED

ORDER SERVICE LOGGING
□ POST /api/orders/1 (valid)              → check console — METHOD STARTED + METHOD SUCCESS
□ POST /api/orders/999 (invalid user)     → check console — METHOD STARTED + METHOD FAILED
□ POST /api/orders/1 (insufficient stock) → check console — METHOD STARTED + METHOD FAILED
```

---

